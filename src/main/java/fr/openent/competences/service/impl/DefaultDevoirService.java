/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.openent.competences.Competences.DELIVERY_OPTIONS;
import static fr.openent.competences.Utils.returnFailure;
import static fr.openent.competences.utils.FormSaisieHelper.*;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.sql.SqlResult.validResultHandler;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultDevoirService extends SqlCrudService implements fr.openent.competences.service.DevoirService {

    private DefaultUtilsService utilsService;
    private DefaultNoteService noteService;
    private final Neo4j neo4j = Neo4j.getInstance();
    private EventBus eb;

    public DefaultDevoirService(EventBus eb) {
        super(Competences.COMPETENCES_SCHEMA, Competences.DEVOIR_TABLE);
        utilsService = new DefaultUtilsService(eb);
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE,eb);
        this.eb = eb;
    }

    public StringBuilder formatDate (String date) {
        Pattern p = Pattern.compile("[0-9]*-[0-9]*-[0-9]*.*");
        Matcher m = p.matcher(date);
        if (!m.matches()) {
            StringBuilder dateFormated = new StringBuilder();
            String[] splitedDate = date.split("/");
            if(splitedDate.length < 3) {
                log.error("Date " + date + " cannot be formated");
                return new StringBuilder(date);
            }

            dateFormated.append(date.split("/")[2]).append('-');
            dateFormated.append(date.split("/")[1]).append('-');
            dateFormated.append(date.split("/")[0]);
            return dateFormated;
        } else {
            return new StringBuilder(date);
        }


    }

    private static final String attributeTypeGroupe = "type_groupe";
    //private static final String attributeCodeTypeClasse = "code_type_classe";
    //private static final int typeClasse_Classe = 0;
    private static final int typeClasse_GroupeEnseignement = 1;
    // private static final String typeClasse_Grp_Ens = "groupeEnseignement";
    private static final String attributeIdGroupe = "id_groupe";

    @Override

    public void createDevoir(final JsonObject devoir, final UserInfos user, final Handler<Either<String, JsonObject>> handler) {
        // Requête de recupération de l'id du devoir à créer
        final String queryNewDevoirId =
                "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".devoirs_id_seq') as id";

        sql.raw(queryNewDevoirId, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {

                if (event.isRight()) {
                    final Long devoirId = event.right().getValue().getLong("id");
                    // Limitation du nombre de compétences
                    if( devoir.getJsonArray("competences").size() > Competences.MAX_NBR_COMPETENCE) {
                        handler.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
                    }
                    else {
                        // Récupération de l'id du devoir à créer
                        JsonArray statements = createStatement(devoirId, devoir, user);

                        // Exécution de la transaction avec roleBack
                        Sql.getInstance().transaction(statements, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> event) {
                                JsonObject result = event.body();
                                if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
                                    handler.handle(new Either.Right<String, JsonObject>(new JsonObject().put("id", devoirId)));
                                } else {
                                    handler.handle(new Either.Left<String, JsonObject>(result.getString("status")));
                                }
                            }
                        });
                    }
                } else {
                    handler.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
                }
            }
        }));
    }


    @Override
    public void getDevoirInfo(final Long idDevoir, final Handler<Either<String, JsonObject>> handler){
        StringBuilder query = new StringBuilder();

        query.append( "SELECT devoir.id, devoir.name, devoir.created, devoir.date, devoir.id_etablissement,")
                .append(" devoir.coefficient,devoir.id_matiere,devoir.diviseur, devoir.is_evaluated,devoir.id_periode,")
                .append(" rel_periode.type AS periodeType,rel_periode.ordre AS periodeOrdre, Gdevoir.id_groupe, comp.*")
                .append(" , Gdevoir.type_groupe, devoir.id_sousmatiere, type_sousmatiere.libelle, id_cycle ")
                .append(" FROM notes.devoirs devoir")
                .append(" INNER JOIN viesco.rel_type_periode rel_periode on rel_periode.id = devoir.id_periode")
                .append(" NATURAL  JOIN (SELECT COALESCE(count(*), 0) NbrCompetence" )
                .append(" FROM notes.competences_devoirs c" )
                .append(" WHERE c.id_devoir =?) comp")
                .append(" INNER JOIN  notes.rel_devoirs_groupes Gdevoir ON Gdevoir.id_devoir = devoir.id")
                .append(" LEFT JOIN "+ Competences.VSCO_SCHEMA +".sousmatiere")
                .append("            ON devoir.id_sousmatiere = sousmatiere.id ")
                .append(" LEFT JOIN "+ Competences.VSCO_SCHEMA +".type_sousmatiere ")
                .append("            ON sousmatiere.id_type_sousmatiere = type_sousmatiere.id ")
                .append(" LEFT JOIN "+ Competences.EVAL_SCHEMA +".rel_devoirs_groupes ")
                .append("            ON rel_devoirs_groupes.id_devoir = devoir.id ")
                .append(" LEFT JOIN "+ Competences.EVAL_SCHEMA +".rel_groupe_cycle ")
                .append("            ON rel_groupe_cycle.id_groupe = rel_devoirs_groupes.id_groupe ")
                .append(" WHERE devoir.id = ? ;");

        JsonArray values =  new fr.wseduc.webutils.collections.JsonArray();
        values.add(idDevoir).add(idDevoir);
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public JsonArray createStatement(Long idDevoir, JsonObject devoir, UserInfos user) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray competences = devoir.getJsonArray("competences");

        //Merge_user dans la transaction

        JsonArray paramsForMerge = new fr.wseduc.webutils.collections.JsonArray();
        paramsForMerge.add(user.getUserId()).add(user.getUsername());

        StringBuilder queryForMerge = new StringBuilder()
                .append("SELECT " + schema + "merge_users(?,?)" );
        statements.add(new JsonObject()
                .put("statement", queryForMerge.toString())
                .put("values", paramsForMerge)
                .put("action", "prepared"));


        //Ajout de la creation du devoir dans la pile de transaction
        StringBuilder queryParams = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder valueParams = new StringBuilder();
        queryParams.append("( id ");
        valueParams.append("( ?");
        params.add(idDevoir);
        for (String attr : devoir.fieldNames()) {
            if(attr.contains("date") && !"competencesUpdate".equals(attr)){
                queryParams.append(" , ").append(attr);
                valueParams.append(" , to_date(?,'YYYY-MM-DD') ");
                params.add(formatDate(devoir.getString(attr)).toString());
            }
            else{
                Boolean isCompetencesAtt = "competencesAdd".equals(attr)
                        ||  "competencesRem".equals(attr)
                        ||  "competenceEvaluee".equals(attr)
                        ||  "competences".equals(attr)
                        || "competencesUpdate".equals(attr);
                if(!( isCompetencesAtt ||  attr.equals(attributeTypeGroupe)
                        ||  attr.equals(attributeIdGroupe))) {
                    queryParams.append(" , ").append(attr);
                    valueParams.append(" , ? ");
                    params.add(devoir.getValue(attr));
                }
            }
        }
        queryParams.append(" )");
        valueParams.append(" ) ");
        queryParams.append(" VALUES ").append(valueParams.toString());
        StringBuilder query = new StringBuilder()
                .append("INSERT INTO " + resourceTable + queryParams.toString());
        statements.add(new JsonObject()
                .put("statement", query.toString())
                .put("values", params)
                .put("action", "prepared"));


        //Ajout de chaque compétence dans la pile de transaction
        if (devoir.containsKey("competences") &&
                devoir.getJsonArray("competences").size() > 0) {

            JsonArray paramsComp = new fr.wseduc.webutils.collections.JsonArray();
            StringBuilder queryComp = new StringBuilder()
                    .append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA
                            +".competences_devoirs (id_devoir, id_competence, index) VALUES ");
            for(int i = 0; i < competences.size(); i++){
                queryComp.append("(?, ?,").append(i).append(")");
                paramsComp.add(idDevoir);
                paramsComp.add((Number) competences.getLong(i));
                if(i != competences.size()-1){
                    queryComp.append(",");
                }else{
                    queryComp.append(";");
                }
            }
            statements.add(new JsonObject()
                    .put("statement", queryComp.toString())
                    .put("values", paramsComp)
                    .put("action", "prepared"));
        }

        // ajoute de l'évaluation de la compéténce (cas évaluation libre)
        if(devoir.containsKey("competenceEvaluee")) {
            final JsonObject oCompetenceNote = devoir.getJsonObject("competenceEvaluee");
            JsonArray paramsCompLibre = new fr.wseduc.webutils.collections.JsonArray();
            StringBuilder valueParamsLibre = new StringBuilder();
            oCompetenceNote.put("owner", user.getUserId());
            StringBuilder queryCompLibre = new StringBuilder()
                    .append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".competences_notes ");
            queryCompLibre.append("( id_devoir ");
            valueParamsLibre.append("( ?");
            paramsCompLibre.add(idDevoir);
            for (String attr : oCompetenceNote.fieldNames()) {
                if(attr.contains("date")){
                    queryCompLibre.append(" , ").append(attr);
                    valueParamsLibre.append(" , to_timestamp(?,'YYYY-MM-DD') ");
                    paramsCompLibre.add(formatDate(oCompetenceNote.getString(attr)).toString());
                }
                else{
                    queryCompLibre.append(" , ").append(attr);
                    valueParamsLibre.append(" , ? ");
                    paramsCompLibre.add(oCompetenceNote.getValue(attr));
                }
            }
            queryCompLibre.append(" )");
            valueParamsLibre.append(" ) ");
            queryCompLibre.append(" VALUES ").append(valueParamsLibre.toString());
            statements.add(new JsonObject()
                    .put("statement", queryCompLibre.toString())
                    .put("values", paramsCompLibre)
                    .put("action", "prepared"));

        }

        // Ajoute une relation notes.rel_devoirs_groupes
        if(null != devoir.getLong(attributeTypeGroupe)
                && devoir.getLong(attributeTypeGroupe)>-1){
            JsonArray paramsAddRelDevoirsGroupes = new fr.wseduc.webutils.collections.JsonArray();
            String queryAddRelDevoirsGroupes = new String("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".rel_devoirs_groupes(id_groupe, id_devoir,type_groupe) VALUES (?, ?, ?)");
            paramsAddRelDevoirsGroupes.add(devoir.getValue(attributeIdGroupe));
            paramsAddRelDevoirsGroupes.add(idDevoir);
            paramsAddRelDevoirsGroupes.add(devoir.getInteger(attributeTypeGroupe).intValue());
            statements.add(new JsonObject()
                    .put("statement", queryAddRelDevoirsGroupes)
                    .put("values", paramsAddRelDevoirsGroupes)
                    .put("action", "prepared"));
        }else{
            log.info("Attribut type_groupe non renseigné pour le devoir relation avec la classe inexistante : Evaluation Libre:  " + idDevoir);
        }
        return statements;
    }

    @Override
    public void duplicateDevoir(Long idDevoir, final JsonObject devoir, final JsonArray classes, final UserInfos user, final Handler<Either<String, JsonArray>> handler) {
        final JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
        String queryNewDevoirId;
        final Integer[] counter = {0};
        final Integer[] errors = {0};
        for (int i = 0; i < classes.size(); i++) {
            queryNewDevoirId = "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".devoirs_id_seq') as id";
            sql.raw(queryNewDevoirId, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> event) {
                    counter[0]++;
                    if (event.isRight()) {
                        JsonObject o = event.right().getValue();
                        ids.add(o.getLong("id"));
                        if (counter[0] == classes.size()) {
                            insertDuplication(ids, devoir, classes, user, errors[0], handler);
                        }
                    } else {
                        errors[0]++;
                    }
                }
            }));
        }
    }

    private JsonObject formatDevoirForDuplication (JsonObject devoir) {
        JsonObject o = new JsonObject(devoir.getMap());
        o.remove("owner");
        o.remove("created");
        o.remove("modified");
        o.remove("id");
        // le pourcentage d'avancement n'est pas conservé lors de la duplication d'un devoir
        o.put("percent", 0);
        try {
            o.put("coefficient", Double.valueOf(o.getString("coefficient")));
        } catch (ClassCastException e) {
            log.error("An error occured when casting devoir object to duplication format.");
            log.error(e);
        }
        if (o.getString("libelle") == null) {
            o.remove("libelle");
        }
        if (o.getLong("id_sousmatiere") == null) {
            o.remove("id_sousmatiere");
        }
        return o;
    }

    private void insertDuplication(JsonArray ids, JsonObject devoir, JsonArray classes, UserInfos user, Integer errors, Handler<Either<String, JsonArray>> handler) {
        if (errors == 0 && ids.size() == classes.size()) {
            JsonObject o, g;
            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
            for (int i = 0; i < ids.size(); i++) {
                try {
                    g = classes.getJsonObject(i);
                    o = formatDevoirForDuplication(devoir);
                    o.put("id_groupe", g.getString("id"));
                    o.put("type_groupe", g.getInteger("type_groupe"));
                    o.put("owner", user.getUserId());
                    JsonArray tempStatements = this.createStatement(ids.getLong(i), o, user);
                    for (int j = 0; j < tempStatements.size(); j++) {
                        statements.add(tempStatements.getValue(j));
                    }
                } catch (ClassCastException e) {
                    log.error("Next id devoir must be a long Object.");
                    log.error(e);
                }

            }
            Sql.getInstance().transaction(statements, SqlResult.validResultHandler(handler));
        } else {
            log.error("An error occured when collecting ids in duplication sequence.");
            handler.handle(new Either.Left<String, JsonArray>("An error occured when collecting ids in duplication sequence."));
        }
    }

    protected static final Logger log = LoggerFactory.getLogger(DefaultDevoirService.class);
    @Override
    public void updateDevoir(String id, JsonObject devoir, Handler<Either<String, JsonArray>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        String old_id_groupe = "";
        if(devoir.containsKey("old_id_groupe")
                && !devoir.getString("old_id_groupe").isEmpty()){
            old_id_groupe = devoir.getString("old_id_groupe");
            devoir.remove("old_id_groupe");
        }
        if (devoir.containsKey("competencesAdd") &&
                devoir.getJsonArray("competencesAdd").size() > 0) {
            JsonArray competenceAdd = devoir.getJsonArray("competencesAdd");
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
            StringBuilder query = new StringBuilder()
                    .append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".competences_devoirs")
                    .append(" (id_devoir, id_competence, index) VALUES ");
            for(int i = 0; i < competenceAdd.size(); i++){
                query.append("(?, ?, ?)");
                params.add(Integer.parseInt(id));
                params.add(((JsonObject)competenceAdd.getJsonObject(i)).getLong("id"));
                params.add(((JsonObject)competenceAdd.getJsonObject(i)).getLong("index"));
                if(i != competenceAdd.size()-1){
                    query.append(",");
                }else{
                    query.append(";");
                }
            }
            statements.add(new JsonObject()
                    .put("statement", query.toString())
                    .put("values", params)
                    .put("action", "prepared"));
        }
        if (devoir.containsKey("competencesRem") &&
                devoir.getJsonArray("competencesRem").size() > 0) {
            JsonArray competenceRem = devoir.getJsonArray("competencesRem");
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
            StringBuilder query = new StringBuilder()
                    .append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".competences_devoirs WHERE ");
            StringBuilder queryDelNote = new StringBuilder()
                    .append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".competences_notes WHERE ");
            for(int i = 0; i < competenceRem.size(); i++){
                query.append("(id_devoir = ? AND  id_competence = ?)");
                queryDelNote.append("(id_devoir = ? AND  id_competence = ?)");
                params.add(Integer.parseInt(id));
                params.add((Number) competenceRem.getLong(i));
                if(i != competenceRem.size()-1){
                    query.append(" OR ");
                    queryDelNote.append(" OR ");
                }else{
                    query.append(";");
                    queryDelNote.append(";");
                }
            }
            statements.add(new JsonObject()
                    .put("statement", query.toString())
                    .put("values", params)
                    .put("action", "prepared"));
            statements.add(new JsonObject()
                    .put("statement", queryDelNote.toString())
                    .put("values", params)
                    .put("action", "prepared"));

        }

        if (devoir.containsKey("competencesUpdate") &&
                devoir.getJsonArray("competencesUpdate").size() > 0) {
            JsonArray competencesUpdate = devoir.getJsonArray("competencesUpdate");
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
            StringBuilder query = new StringBuilder()
                    .append("UPDATE " + Competences.COMPETENCES_SCHEMA +".competences_devoirs ")
                    .append(" SET index = CASE ");


            for(int i = 0; i < competencesUpdate.size(); i++){
                query.append(" WHEN id_competence = ? AND id_devoir = ? THEN ? ");
                params.add(((JsonObject)competencesUpdate.getJsonObject(i)).getLong("id"));
                params.add(Integer.parseInt(id));
                params.add(((JsonObject)competencesUpdate.getJsonObject(i)).getLong("index"));
            }
            query.append(" ELSE index END ")
                    .append(" WHERE id_devoir = ? ");
            params.add(Integer.parseInt(id));

            statements.add(new JsonObject()
                    .put("statement", query.toString())
                    .put("values", params)
                    .put("action", "prepared"));
        }

        StringBuilder queryParams = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        devoir.remove("competencesRem");
        devoir.remove("competencesAdd");
        devoir.remove("competencesUpdate");
        devoir.remove("competences");

        for (String attr : devoir.fieldNames()) {
            if(!(attr.equals(attributeTypeGroupe)
                    || attr.equals(attributeIdGroupe))) {
                if (attr.contains("date")) {
                    queryParams.append(attr).append(" =to_date(?,'YYYY-MM-DD'), ");
                    params.add(formatDate(devoir.getString(attr)).toString());

                } else {
                    queryParams.append(attr).append(" = ?, ");
                    params.add(devoir.getValue(attr));
                }
            }
        }
        //FIXME : A modifier lorsqu'on pourra rattacher un devoir à plusieurs groupes
        // Modifie une relation notes.rel_devoirs_groupes
        if(null != devoir.getString(attributeIdGroupe)
                && null != devoir.getLong(attributeTypeGroupe)
                && devoir.getLong(attributeTypeGroupe)>-1){
            String queryUpdateRelDevoirGroupe ="UPDATE "+ Competences.COMPETENCES_SCHEMA + ".rel_devoirs_groupes " +
                    "SET id_groupe = ? " +
                    "WHERE id_devoir = ? ";
            JsonArray paramsUpdateRelDevoirGroupe = new fr.wseduc.webutils.collections.JsonArray();
            paramsUpdateRelDevoirGroupe.add(devoir.getString(attributeIdGroupe));
            paramsUpdateRelDevoirGroupe.add(Integer.parseInt(id));
            statements.add(new JsonObject()
                    .put("statement", queryUpdateRelDevoirGroupe)
                    .put("values", paramsUpdateRelDevoirGroupe)
                    .put("action", "prepared"));
        }else{
            log.info("Attribut type_groupe non renseigné pour le devoir relation avec la classe inexistante : Evaluation Libre :  " + id);
        }

        // Lors du changement de classe, on supprimes : annotations, notes et appréciations du devoir
        if(!old_id_groupe.isEmpty()
                && !devoir.getString(attributeIdGroupe).equalsIgnoreCase(old_id_groupe)){

            JsonArray paramsDelete = new fr.wseduc.webutils.collections.JsonArray();
            paramsDelete.add(Integer.parseInt(id));

            StringBuilder queryDeleteNote = new StringBuilder()
                    .append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".notes WHERE id_devoir = ? ");
            statements.add(new JsonObject()
                    .put("statement", queryDeleteNote.toString())
                    .put("values", paramsDelete)
                    .put("action", "prepared"));

            StringBuilder queryDeleteAnnotations = new StringBuilder()
                    .append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".rel_annotations_devoirs WHERE id_devoir = ? ");
            statements.add(new JsonObject()
                    .put("statement", queryDeleteAnnotations.toString())
                    .put("values", paramsDelete)
                    .put("action", "prepared"));

            StringBuilder queryDeleteAppreciations = new StringBuilder()
                    .append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".appreciations WHERE id_devoir = ? ");
            statements.add(new JsonObject()
                    .put("statement", queryDeleteAppreciations.toString())
                    .put("values", paramsDelete)
                    .put("action", "prepared"));

            StringBuilder queryDeleteCompetences = new StringBuilder()
                    .append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".competences_notes WHERE id_devoir = ? ");
            statements.add(new JsonObject()
                    .put("statement", queryDeleteCompetences.toString())
                    .put("values", paramsDelete)
                    .put("action", "prepared"));
        }

        StringBuilder query = new StringBuilder()
                .append("UPDATE " + resourceTable +" SET " + queryParams.toString() + "modified = NOW() WHERE id = ? ");
        statements.add(new JsonObject()
                .put("statement", query.toString())
                .put("values", params.add(Integer.parseInt(id)))
                .put("action", "prepared"));



        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(handler));
    }

    @Override
    /**
     * Liste des devoirs de l'utilisateur
     * @param user utilisateur l'utilisateur connecté
     * @param handler handler portant le résultat de la requête
     */
    public void listDevoirs(UserInfos user, String idEtablissement, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT devoirs.id, devoirs.name, devoirs.owner, devoirs.created, devoirs.libelle, rel_devoirs_groupes.id_groupe, rel_devoirs_groupes.type_groupe , devoirs.is_evaluated,")
                .append("devoirs.id_sousmatiere,devoirs.id_periode, devoirs.id_type, devoirs.id_etablissement, devoirs.diviseur, ")
                .append("devoirs.id_etat, devoirs.date_publication, devoirs.id_matiere, devoirs.coefficient, devoirs.ramener_sur, devoirs.percent, ")
                .append("type_sousmatiere.libelle as _sousmatiere_libelle, devoirs.date, devoirs.apprec_visible, ")
                .append("type.nom as _type_libelle, COUNT(competences_devoirs.id) as nbcompetences, users.username as teacher ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".devoirs ")
                .append("inner join "+ Competences.COMPETENCES_SCHEMA +".type on devoirs.id_type = type.id ")
                .append("left join "+ Competences.COMPETENCES_SCHEMA +".competences_devoirs on devoirs.id = competences_devoirs.id_devoir ")
                .append("left join "+ Competences.VSCO_SCHEMA +".sousmatiere  on devoirs.id_sousmatiere = sousmatiere.id ")
                .append("left join "+ Competences.VSCO_SCHEMA +".type_sousmatiere on sousmatiere.id_type_sousmatiere = type_sousmatiere.id ")
                .append("left join "+ Competences.COMPETENCES_SCHEMA +".rel_devoirs_groupes ON rel_devoirs_groupes.id_devoir = devoirs.id ")
                .append("inner join "+ Competences.COMPETENCES_SCHEMA + ".users ON users.id = devoirs.owner ")
                .append("WHERE (rel_devoirs_groupes.id_devoir = devoirs.id) ")
                .append("AND (devoirs.id_etablissement = ? ) ")
                .append("AND (devoirs.eval_lib_historise = false ) ")
                .append("AND (devoirs.owner = ? OR ") // devoirs dont on est le propriétaire
                .append("devoirs.owner IN (SELECT DISTINCT id_titulaire ") // ou dont l'un de mes tiulaires le sont (de l'établissement passé en paramètre)
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_professeurs_remplacants ")
                .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".devoirs ON devoirs.id_etablissement = rel_professeurs_remplacants.id_etablissement  ")
                .append("WHERE id_remplacant = ? ")
                .append("AND rel_professeurs_remplacants.id_etablissement = ? ")
                .append(") OR ")
                .append("? IN (SELECT member_id ") // ou devoirs que l'on m'a partagés (lorsqu'un remplaçant a créé un devoir pour un titulaire par exemple)
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs_shares ")
                .append("WHERE resource_id = devoirs.id ")
                .append("AND action = '" + Competences.DEVOIR_ACTION_UPDATE+"')")
                .append(") ")
                .append("GROUP BY devoirs.id, devoirs.name, devoirs.created, devoirs.libelle, rel_devoirs_groupes.id_groupe, devoirs.is_evaluated, users.username, ")
                .append("devoirs.id_sousmatiere,devoirs.id_periode, devoirs.id_type, devoirs.id_etablissement, devoirs.diviseur, ")
                .append("devoirs.id_etat, devoirs.date_publication, devoirs.date, devoirs.id_matiere, rel_devoirs_groupes.type_groupe , devoirs.coefficient, devoirs.ramener_sur, type_sousmatiere.libelle , type.nom ")
                .append("ORDER BY devoirs.date DESC;");


        // Ajout des params pour les devoirs dont on est le propriétaire sur l'établissement
        values.add(idEtablissement);
        values.add(user.getUserId());

        // Ajout des params pour la récupération des devoirs de mes tiulaires
        values.add(user.getUserId());
        values.add(idEtablissement);

        // Ajout des params pour les devoirs que l'on m'a partagés (lorsqu'un remplaçant a créé un devoir pour un titulaire par exemple)
        values.add(user.getUserId());

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void listDevoirsEtab(UserInfos user, Integer limit, Handler<Either<String, JsonArray>> handler){
        boolean limitResult = limit != null && limit.intValue() > 0;

        StringBuilder mainQuery = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        mainQuery.append(" SELECT devoirs.id, devoirs.name, devoirs.owner, devoirs.created, devoirs.libelle, rel_devoirs_groupes.id_groupe , rel_devoirs_groupes.type_groupe , devoirs.is_evaluated, " )
                .append("   devoirs.id_sousmatiere,devoirs.id_periode, devoirs.id_type, devoirs.id_etablissement, devoirs.diviseur, devoirs.percent, ")
                .append("   devoirs.id_etat, devoirs.date_publication, devoirs.id_matiere, devoirs.coefficient, devoirs.ramener_sur,  ")
                .append("   type_sousmatiere.libelle as _sousmatiere_libelle, devoirs.date,  ")
                .append("   type.nom as _type_libelle, COUNT(competences_devoirs.id) as nbcompetences, users.username as teacher ")
                .append("   FROM notes.devoirs  ")
                .append("   inner join notes.type on devoirs.id_type = type.id  ")
                .append("   left join notes.competences_devoirs on devoirs.id = competences_devoirs.id_devoir  ")
                .append("   left join viesco.sousmatiere  on devoirs.id_sousmatiere = sousmatiere.id  ")
                .append("   left join viesco.type_sousmatiere on sousmatiere.id_type_sousmatiere = type_sousmatiere.id  ")
                .append("   left join notes.rel_devoirs_groupes ON rel_devoirs_groupes.id_devoir = devoirs.id  ")
                .append("   inner join notes.users on users.id = devoirs.owner")
                .append("   where devoirs.id_etablissement IN "+ Sql.listPrepared(user.getStructures().toArray()) +" ")
                .append("   and devoirs.eval_lib_historise = false ")
                .append("   and id_groupe is not null ");

        StringBuilder endMainQuery = new StringBuilder();
        endMainQuery.append("   GROUP BY devoirs.id, devoirs.name, devoirs.created, devoirs.libelle, rel_devoirs_groupes.id_groupe, devoirs.is_evaluated, users.username,  ")
                .append("   devoirs.id_sousmatiere,devoirs.id_periode, devoirs.id_type, devoirs.id_etablissement, devoirs.diviseur,  ")
                .append("   devoirs.id_etat, devoirs.date_publication, devoirs.date, devoirs.id_matiere, rel_devoirs_groupes.type_groupe , devoirs.coefficient, devoirs.ramener_sur, type_sousmatiere.libelle, type.nom  ");


        StringBuilder orderByQuery = new StringBuilder();
        orderByQuery.append("   ORDER BY date DESC; ");



        StringBuilder globalQuery = new StringBuilder();
        if(limitResult) {
            StringBuilder limitQuery = new StringBuilder();
            limitQuery.append("LIMIT ?");

            globalQuery.append("(")
                    .append(mainQuery)
                    .append("AND devoirs.percent < 100 ")
                    .append(endMainQuery)
                    .append(limitQuery)
                    .append(") UNION ( ")
                    .append(mainQuery)
                    .append("AND devoirs.percent = 100 ")
                    .append(endMainQuery)
                    .append(limitQuery)
                    .append(")")
                    .append(orderByQuery);

            // params requete 1
            for (int i = 0; i < user.getStructures().size(); i++) {
                values.add(user.getStructures().get(i));
            }
            values.add(limit);

            // params requete 2
            for (int i = 0; i < user.getStructures().size(); i++) {
                values.add(user.getStructures().get(i));
            }
            values.add(limit);

        } else {
            globalQuery.append(mainQuery).append(endMainQuery).append(orderByQuery);

            for (int i = 0; i < user.getStructures().size(); i++) {
                values.add(user.getStructures().get(i));
            }
        }


        Sql.getInstance().prepared(globalQuery.toString(), values, validResultHandler(handler));

    }

    @Override
    public void getClassesIdsDevoir(UserInfos user, String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT distinct(rel_devoirs_groupes.id_groupe) " +
                "FROM notes.devoirs " +
                "inner join notes.rel_devoirs_groupes ON (rel_devoirs_groupes.id_devoir = devoirs.id) " +
                "AND (devoirs.id_etablissement = ? ) " +
                "AND (devoirs.owner = ? " +
                "OR devoirs.owner IN (SELECT DISTINCT id_titulaire " +
                "FROM notes.rel_professeurs_remplacants " +
                "INNER JOIN notes.devoirs ON devoirs.id_etablissement = rel_professeurs_remplacants.id_etablissement " +
                "WHERE id_remplacant = ? " +
                "AND rel_professeurs_remplacants.id_etablissement = ?) " +
                "OR ? IN (SELECT member_id " +
                "FROM notes.devoirs_shares " +
                "WHERE resource_id = devoirs.id " +
                "AND action = '"+ Competences.DEVOIR_ACTION_UPDATE +"'))";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(structureId)
                .add(user.getUserId())
                .add(user.getUserId())
                .add(structureId)
                .add(user.getUserId());
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listDevoirs(String idEleve, String idEtablissement, String idClasse, String idMatiere, Long idPeriode,boolean historise, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT devoirs.*, ")
                .append("type.nom as _type_libelle, rel_type_periode.type as _periode_type, rel_type_periode.ordre as _periode_ordre, users.username as teacher ");
        if (idEleve != null) {
            query.append(", notes.valeur as note, COUNT(competences_devoirs.id) as nbcompetences ");
        }
        query.append("FROM ")
                .append(Competences.COMPETENCES_SCHEMA +".devoirs ")
                .append("left join "+ Competences.VSCO_SCHEMA +".rel_type_periode on devoirs.id_periode = rel_type_periode.id ")
                .append("inner join "+ Competences.COMPETENCES_SCHEMA +".type on devoirs.id_type = type.id ")
                .append("inner join "+ Competences.COMPETENCES_SCHEMA +".users on users.id = devoirs.owner ");
        if(idClasse != null) {
            query.append("inner join " + Competences.COMPETENCES_SCHEMA + ".rel_devoirs_groupes on rel_devoirs_groupes.id_devoir = devoirs.id AND rel_devoirs_groupes.id_groupe =? ");
            values.add(idClasse);
        }
        if (idEleve != null) {
            query.append(" left join "+ Competences.COMPETENCES_SCHEMA +".competences_devoirs ")
                    .append(" on devoirs.id = competences_devoirs.id_devoir ")
                    .append("inner join "+ Competences.COMPETENCES_SCHEMA +".notes on devoirs.id = notes.id_devoir ");
        }
        query.append("WHERE ")
                .append("devoirs.id_etablissement = ? ");
        values.add(idEtablissement);
        if( idMatiere != null ) {
            query.append("AND ")
                    .append("devoirs.id_matiere = ? ");
            values.add(idMatiere);
        }
        if (idEleve !=  null){
            query.append(" AND  notes.id_eleve = ? AND date_publication <= Now() ");
            values.add(idEleve);
        }
        if (idPeriode != null) {
            query.append("AND ")
                    .append("devoirs.id_periode = ? ");
            values.add(idPeriode);
        }
        if (historise) {
            query.append(" AND ")
                    .append("devoirs.eval_lib_historise = ? ");
            values.add(historise);
        }
        if (idEleve != null) {
            query.append(" GROUP BY devoirs.id,rel_type_periode.type , rel_type_periode.ordre, type.nom, notes.valeur, users.username ")
                    .append(" ORDER BY devoirs.date ASC, devoirs.id ASC ");
        } else {
            query.append("ORDER BY devoirs.date ASC, devoirs.id ASC ");
        }
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void listDevoirs(String idEleve, String[] idGroupes, Long[] idDevoirs, Long[] idPeriodes,
                            String[] idEtablissements, String[] idMatieres, Boolean hasCompetences,
                            Boolean historise, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        if(idGroupes == null) {
            idGroupes = new String[0];
        }
        if (idDevoirs == null) {
            idDevoirs = new Long[0];
        }
        if (idPeriodes == null) {
            idPeriodes = new Long[0];
        }
        if (idEtablissements == null) {
            idEtablissements = new String[0];
        }
        if (idMatieres == null) {
            idMatieres = new String[0];
        }

        if(idGroupes.length == 0 && idDevoirs.length == 0 && idPeriodes.length == 0 && idEtablissements.length == 0 && idMatieres.length == 0) {
            handler.handle(new Either.Left<String, JsonArray>("listDevoirs : All parameters are empty."));
        }

        query.append("SELECT devoirs.*, rel.id_groupe")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + "." + Competences.DEVOIR_TABLE + " AS devoirs")
                .append(" LEFT JOIN " + Competences.COMPETENCES_SCHEMA + "." + Competences.REL_DEVOIRS_GROUPES + " AS rel")
                .append(" ON devoirs.id = rel.id_devoir");

        if(hasCompetences == null || !hasCompetences) {
            query.append(" WHERE");
        } else {
            query.append(" WHERE EXISTS (SELECT 1 FROM " + Competences.COMPETENCES_SCHEMA + "." + Competences.COMPETENCES_DEVOIRS + " AS comp WHERE comp.id_devoir = devoirs.id) AND");
        }

        if(idGroupes.length != 0) {
            query.append(" rel.id_groupe IN " + Sql.listPrepared(idGroupes) + " AND");
            for(String idGroupe : idGroupes) {
                params.add(idGroupe);
            }
        }

        if (idDevoirs.length != 0) {
            query.append(" devoirs.id IN " + Sql.listPrepared(idDevoirs) + " AND");
            for (Long idDevoir : idDevoirs) {
                params.add(idDevoir);
            }
        }

        if (idPeriodes.length != 0) {
            query.append(" devoirs.id_periode IN " + Sql.listPrepared(idPeriodes) + " AND");
            for (Long idPeriode : idPeriodes) {
                params.add(idPeriode);
            }
        }

        if (idEtablissements.length != 0) {
            query.append(" devoirs.id_etablissement IN " + Sql.listPrepared(idEtablissements) + " AND");
            for (String idEtablissement : idEtablissements) {
                params.add(idEtablissement);
            }
        }

        if (idMatieres.length != 0) {
            query.append(" (devoirs.id_matiere IN " + Sql.listPrepared(idMatieres));
            for (String idMatiere : idMatieres) {
                params.add(idMatiere);
            }
            if (historise) {
                query.append(" OR ");
            }
        } else {
            query.append(" (");
        }
        if (historise) {
            query.append(" devoirs.eval_lib_historise = ? ");
            params.add(historise);
        }
        if (idMatieres.length != 0 || historise) {
            query.append(")  AND");
        }

        query.delete(query.length() - 3, query.length());

        query.append(" UNION ");

        query.append("SELECT devoirs.*, rel.id_groupe")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + "." + Competences.DEVOIR_TABLE + " AS devoirs")
                .append(" LEFT JOIN " + Competences.COMPETENCES_SCHEMA + "." + Competences.REL_DEVOIRS_GROUPES + " AS rel").append(" ON devoirs.id = rel.id_devoir")
                .append(" LEFT JOIN " + Competences.COMPETENCES_SCHEMA + "." + Competences.COMPETENCES_NOTES_TABLE + " AS comp").append(" ON devoirs.id = comp.id_devoir");


        if(hasCompetences == null || !hasCompetences) {
            query.append(" WHERE");
        } else {
            query.append(" WHERE EXISTS (SELECT 1 FROM " + Competences.COMPETENCES_SCHEMA + "." + Competences.COMPETENCES_DEVOIRS + " AS comp WHERE comp.id_devoir = devoirs.id) AND");
        }

        // recuperation des evaluations libres de l'élève
        if(idEleve != null) {
            query.append(" comp.id_eleve = ? AND rel.id_groupe IS NULL AND ");
            params.add(idEleve);
        }

        if (idDevoirs.length != 0) {
            query.append(" devoirs.id IN " + Sql.listPrepared(idDevoirs) + " AND");
            for (Long idDevoir : idDevoirs) {
                params.add(idDevoir);
            }
        }

        if (idPeriodes.length != 0) {
            query.append(" devoirs.id_periode IN " + Sql.listPrepared(idPeriodes) + " AND");
            for (Long idPeriode : idPeriodes) {
                params.add(idPeriode);
            }
        }

        if (idEtablissements.length != 0) {
            query.append(" devoirs.id_etablissement IN " + Sql.listPrepared(idEtablissements) + " AND");
            for (String idEtablissement : idEtablissements) {
                params.add(idEtablissement);
            }
        }

        if (idMatieres.length != 0) {
            query.append(" ((devoirs.id_matiere = '' OR devoirs.id_matiere IN " + Sql.listPrepared(idMatieres) + ") ");
            for (String idMatiere : idMatieres) {
                params.add(idMatiere);
            }
            if (historise) {
                query.append(" OR ");
            }
        } else {
            query.append(" (");
        }
        if (historise) {
            query.append(" devoirs.eval_lib_historise = ? ");
            params.add(historise);
        }
        if (idMatieres.length != 0 || historise) {
            query.append(")  AND");
        }

        query.delete(query.length() - 3, query.length());

        Sql.getInstance().prepared(query.toString(), params, DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

    @Override
    @Deprecated // FIXME GERER LES DROITS ET PERMISSIONS COMME FAIT POUR LES ENSEIGNANTS
    public void listDevoirs(String idEtablissement, Long idPeriode, String idUser, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT devoirs.*,type_sousmatiere.libelle as _sousmatiere_libelle,sousmatiere.id as _sousmatiere_id " +
                "FROM "+ Competences.COMPETENCES_SCHEMA +".devoirs " +
                "LEFT JOIN "+ Competences.VSCO_SCHEMA +".sousmatiere ON devoirs.id_sousmatiere = sousmatiere.id " +
                "LEFT JOIN "+ Competences.VSCO_SCHEMA +".type_sousmatiere ON sousmatiere.id_type_sousmatiere = type_sousmatiere.id " +
                "WHERE devoirs.id_etablissement = ?" +
                "AND devoirs.id_periode = ? " +
                "AND devoirs.owner = ? " +
                "AND devoirs.date_publication <= current_date " +
                "ORDER BY devoirs.date ASC;");

        values.add(idEtablissement);
        values.add(idPeriode);
        values.add(idUser);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }


    @Override
    public void getNbNotesDevoirs(UserInfos user, List<String> idEleves, Long idDevoir,
                                  Handler<Either<String, JsonArray>> handler) {
        // Si l'utilisateur est null c'est qu'on essait de mettre à jour le taux de completude des devoirs
        boolean isChefEtab = (user!= null)?(new WorkflowActionUtils().hasRight(user,
                WorkflowActions.ADMIN_RIGHT.toString())):true;

        WorkflowActionUtils.hasHeadTeacherRight(user, null, new JsonArray().add(idDevoir),
                Competences.DEVOIR_TABLE, null, null, null, new Handler<Either<String, Boolean>>() {
                    @Override
                    public void handle(Either<String, Boolean> event) {
                        Boolean isHeadTeacher;
                        if(event.isLeft()){
                            isHeadTeacher = false;
                        }
                        else {
                            isHeadTeacher = event.right().getValue();
                        }
                        getNbNotesDevoirs(user,idEleves,idDevoir,handler, isChefEtab || isHeadTeacher);
                    }
                });


    }
    private void getNbNotesDevoirs(UserInfos user, List<String> idEleves, Long idDevoir,
                                   Handler<Either<String, JsonArray>> handler, Boolean isChefEtab) {
        StringBuilder query = new StringBuilder();


        query.append("SELECT count(notes.id) as nb_notes , devoirs.id, rel_devoirs_groupes.id_groupe ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".notes,"+ Competences.COMPETENCES_SCHEMA +".devoirs, "+ Competences.COMPETENCES_SCHEMA +".rel_devoirs_groupes " )
                .append("WHERE notes.id_devoir = devoirs.id ")
                .append("AND rel_devoirs_groupes.id_devoir = devoirs.id ")
                .append("AND devoirs.id = ? ");

        // filtre sur les élèves de la classe à l'instant T
        if(idEleves != null && idEleves.size() > 0) {
            query.append(" AND "+ Competences.NOTES_TABLE + ".id_eleve IN ").append(Sql.listPrepared(idEleves.toArray()));
        }

        if(!isChefEtab) {
            query.append(" AND (devoirs.owner = ? OR ") // devoirs dont on est le propriétaire
                    .append("devoirs.owner IN (SELECT DISTINCT id_titulaire ") // ou dont l'un de mes tiulaires le sont (on regarde sur tous mes établissments)
                    .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_professeurs_remplacants ")
                    .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".devoirs ON devoirs.id_etablissement = rel_professeurs_remplacants.id_etablissement  ")
                    .append("WHERE id_remplacant = ? ")
                    .append("AND rel_professeurs_remplacants.id_etablissement IN " + Sql.listPrepared(user.getStructures().toArray()) + " ")
                    .append(") OR ")
                    .append("? IN (SELECT member_id ") // ou devoirs que l'on m'a partagés (lorsqu'un remplaçant a créé un devoir pour un titulaire par exemple)
                    .append("FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs_shares ")
                    .append("WHERE resource_id = devoirs.id ")
                    .append("AND action = '" + Competences.DEVOIR_ACTION_UPDATE + "')")
                    .append(") ");
        }
        query.append("GROUP by devoirs.id, rel_devoirs_groupes.id_groupe");

        JsonArray values =  new fr.wseduc.webutils.collections.JsonArray();

        //Ajout des id désirés
        values.add(idDevoir);

        // Ajout des id pour le filtre sur les élèves de la classe à l'instant T
        if(idEleves != null && idEleves.size() > 0) {
            for (String idEleve: idEleves) {
                values.add(idEleve);
            }
        }
        addValueForRequest(values,user,isChefEtab);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getNbAnnotationsDevoirs(UserInfos user, List<String> idEleves, Long idDevoir,
                                        Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        // Si l'utilisateur est null c'est qu'on essait de mettre à jour le taux de completude des devoirs
        boolean isChefEtab = (user!= null)?(new WorkflowActionUtils().hasRight(user,
                WorkflowActions.ADMIN_RIGHT.toString())):true;

        WorkflowActionUtils.hasHeadTeacherRight(user, null, new JsonArray().add(idDevoir),
                Competences.DEVOIR_TABLE, null, null, null,
                new Handler<Either<String, Boolean>>() {
                    @Override
                    public void handle(Either<String, Boolean> event) {
                        Boolean isHeadTeacher;
                        if(event.isLeft()){
                            isHeadTeacher = false;
                        }
                        else {
                            isHeadTeacher = event.right().getValue();
                        }
                        getNbAnnotationsDevoirs(user,idEleves,idDevoir,handler, isChefEtab || isHeadTeacher);
                    }
                });

    }

    private StringBuilder buildAnnotationNotNNQuery(List<String> idsStudents, JsonArray values, Long idDevoir) {
        StringBuilder res = new StringBuilder()
                .append("SELECT count(rel_annotations_devoirs.id_annotation) AS nb_annotations ,'notNN' as type, ")
                .append(" devoirs.id, rel_devoirs_groupes.id_groupe ")
                .append(" FROM ").append(Competences.COMPETENCES_SCHEMA).append(".rel_annotations_devoirs \n")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".devoirs ")
                .append("      ON rel_annotations_devoirs.id_devoir = devoirs.id AND devoirs.id = ? \n");
        values.add(idDevoir);

        if(idsStudents != null && idsStudents.size() > 0) {
            res.append(" AND rel_annotations_devoirs.id_eleve IN ")
                    .append(Sql.listPrepared(idsStudents.toArray()));
            for (String idEleve: idsStudents) {
                values.add(idEleve);
            }
        }
        res.append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".rel_devoirs_groupes ")
                .append("      ON rel_devoirs_groupes.id_devoir = devoirs.id AND devoirs.id =? \n")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".annotations ")
                .append("      ON annotations.id = rel_annotations_devoirs.id_annotation ")
                .append("         AND NOT annotations.libelle_court = 'NN' \n")
                .append(" GROUP by devoirs.id, rel_devoirs_groupes.id_groupe");
        values.add(idDevoir);
        return res;
    }

    private StringBuilder buildAnnotationNNQuery(List<String> idsStudents, JsonArray values, Long idDevoir) {
        StringBuilder res = new StringBuilder()
                .append("SELECT count(rel_annotations_devoirs.id_annotation) AS nb_annotations ,'NN' as type, ")
                .append(" devoirs.id, rel_devoirs_groupes.id_groupe ")
                .append(" FROM ").append(Competences.COMPETENCES_SCHEMA).append(".rel_annotations_devoirs \n")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".devoirs ")
                .append("      ON rel_annotations_devoirs.id_devoir = devoirs.id AND devoirs.id = ? \n");
        values.add(idDevoir);
        if(idsStudents != null && idsStudents.size() > 0) {
            res.append(" AND rel_annotations_devoirs.id_eleve IN ")
                    .append(Sql.listPrepared(idsStudents.toArray()));
            for (String idEleve: idsStudents) {
                values.add(idEleve);
            }
        }
        res.append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".rel_devoirs_groupes ")
                .append("      ON rel_devoirs_groupes.id_devoir = devoirs.id AND devoirs.id =? \n")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".annotations ")
                .append("      ON annotations.id = rel_annotations_devoirs.id_annotation ")
                .append("         AND annotations.libelle_court = 'NN' \n")
                .append(" GROUP by devoirs.id, rel_devoirs_groupes.id_groupe");
        values.add(idDevoir);

        return res;
    }
    private void getNbAnnotationsDevoirs(UserInfos user, List<String> idsStudents, Long idDevoir,
                                         Handler<Either<String, JsonArray>> handler, Boolean isChefEtab) {

        JsonArray values =  new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder queryAnnotationNotNN = buildAnnotationNotNNQuery(idsStudents,values, idDevoir);
        StringBuilder queryAnnotationNN = buildAnnotationNNQuery(idsStudents,values,idDevoir);

        StringBuilder query = new StringBuilder().append(queryAnnotationNotNN)
                .append(" UNION ").append(queryAnnotationNN);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    private void addValueForRequest (JsonArray values, UserInfos user, Boolean isChefEtab) {
        if(!isChefEtab) {
            // Ajout des params pour les devoirs dont on est le propriétaire
            values.add( user.getUserId());

            // Ajout des params pour la récupération des devoirs de mes tiulaires
            values.add(user.getUserId());
            for (int i = 0; i < user.getStructures().size(); i++) {
                values.add( user.getStructures().get(i));
            }

            // Ajout des params pour les devoirs que l'on m'a partagés (lorsqu'un remplaçant a créé un devoir
            // pour un titulaire par exemple)
            values.add( user.getUserId());
        }

    }

    @Override
    public void getevaluatedDevoir(Long idDevoir, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String TypeEvalNum = "TypeEvalNum";
        String TypeEvalSkill = "TypeEvalSkill";
        query.append("select count(n.id_eleve) NbrEval, n.id_eleve ID, n.valeur Evaluation, '"+TypeEvalNum+"' TypeEval " );
        query.append("FROM "+ Competences.COMPETENCES_SCHEMA +".notes n, "+ Competences.COMPETENCES_SCHEMA +".devoirs d ");
        query.append("WHERE n.id_devoir = d.id ");
        query.append("AND d.id = ? ");
        query.append("Group BY (n.id_eleve, n.valeur) ");
        query.append("UNION ");
        query.append("select count(c.id_competence) NbrEval, concat(c.id_competence,'') ID, c.evaluation Evaluation,  '"+TypeEvalSkill+"' TypeEval ");
        query.append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences_notes c, "+ Competences.COMPETENCES_SCHEMA +".devoirs d ");
        query.append("WHERE c.id_devoir = d.id ");
        query.append("AND d.id = ? ");
        query.append("and c.evaluation != -1 ");
        query.append("Group BY(id_competence,evaluation) ");
        query.append("order by (TypeEval) ");

        values.add(idDevoir);
        values.add(idDevoir);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }


    @Override
    public void getevaluatedDevoirs(Long[] idDevoir, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();



        query.append("SELECT case ");
        query.append("when SkillEval.id is null then NumEval.id ");
        query.append("when NumEval.id is null then SkillEval.id ");
        query.append("else SkillEval.id ");
        query.append("END id, ");
        query.append("NbEvalSkill, NbEvalNum  FROM " );
        query.append("(SELECT d.id, count(d.id) NbEvalSkill FROM notes.devoirs d " );
        query.append("INNER  JOIN notes.competences_notes c ON d.id = c.id_devoir " );
        query.append("AND d.id in ");
        query.append("(");
        for (int i=0; i<idDevoir.length-1 ; i++){
            query.append("?,");
        }
        query.append("?) ");
        query.append("Group by (d.id)  ) SkillEval ");
        query.append("FULL JOIN (SELECT  d.id, count(d.id) NbEvalNum FROM notes.devoirs d ");
        query.append("INNER  JOIN notes.notes n ON d.id = n.id_devoir ");
        query.append("AND  d.id in ");
        query.append("(");
        for (int i=0; i<idDevoir.length-1 ; i++){
            query.append("?,");
        }
        query.append("?) ");
        query.append("Group by (d.id)  ) NumEval ON  SkillEval.id = NumEval.id ");




        for (int i=0; i<idDevoir.length ; i++){
            values.add(idDevoir[i]);
        }

        for (int i=0; i<idDevoir.length ; i++){
            values.add(idDevoir[i]);
        }

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void getMoyenne(Long idDevoir, final boolean stats, final Handler<Either<String, JsonObject>> handler) {

        getDevoirInfo(idDevoir, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(final Either<String, JsonObject> devoirInfo) {
                if (devoirInfo.isRight()) {

                    final JsonObject devoirInfos = (JsonObject) ((Either.Right) devoirInfo).getValue();
                    Long idPeriode = devoirInfos.getLong("id_periode");

                    JsonObject action = new JsonObject()
                            .put("action", "classe.getEleveClasse")
                            .put("idClasse", devoirInfos.getString("id_groupe"))
                            .put("idPeriode", devoirInfos.getInteger("id_periode"));

                    eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                            handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> message) {
                                    JsonObject body = message.body();

                                    if ("ok".equals(body.getString("status"))) {
                                        JsonArray eleves = body.getJsonArray("results");
                                        String[] idEleves = new String[eleves.size()];
                                        for (int i = 0; i < eleves.size(); i++) {
                                            idEleves[i] = eleves.getJsonObject(i).getString("id");
                                        }

                                        noteService.getNotesParElevesParDevoirs(idEleves, new Long[]{idDevoir},
                                                new Handler<Either<String, JsonArray>>() {

                                                    @Override
                                                    public void handle(Either<String, JsonArray> event) {
                                                        if (event.isRight()) {
                                                            ArrayList<NoteDevoir> notes = new ArrayList<>();

                                                            JsonArray listNotes = event.right().getValue();

                                                            for (int i = 0; i < listNotes.size(); i++) {

                                                                JsonObject note = listNotes.getJsonObject(i);
                                                                String  coef = note.getString("coefficient");
                                                                if(coef != null) {
                                                                    NoteDevoir noteDevoir = new NoteDevoir(
                                                                            Double.valueOf(note
                                                                                    .getString("valeur")),
                                                                            note.getBoolean("ramener_sur"),
                                                                            Double.valueOf(coef));

                                                                    notes.add(noteDevoir);
                                                                }
                                                            }

                                                            Either<String, JsonObject> result;

                                                            if (!notes.isEmpty()) {
                                                                result = new Either.Right<>(utilsService
                                                                        .calculMoyenneParDiviseur(notes, stats));
                                                            } else {
                                                                result = new Either.Right<>(new JsonObject());
                                                            }

                                                            handler.handle(result);

                                                        } else {
                                                            log.error("[get Moyenne]: cannot get Eleves class");
                                                            handler.handle(new Either.Left<String, JsonObject>(
                                                                    event.left().getValue()));
                                                        }
                                                    }
                                                });
                                    }
                                    else {
                                        log.error("[get Moyenne]: cannot get Eleves class");
                                        handler.handle(new Either.Left<>("[get Moyenne]: cannot get Eleves class"));
                                    }
                                }
                            }));
                } else {
                    log.error("[get Moyenne]: cannot get Eleves class");
                    handler.handle(devoirInfo.left());
                }
            }
        });
    }


    public void getNbCompetencesDevoirs(Long[] idDevoirs, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT d.id id, count(id_competence) as nb_competences ")
                .append("FROM  "+ Competences.COMPETENCES_SCHEMA +".devoirs d ")
                .append("LEFT JOIN "+ Competences.COMPETENCES_SCHEMA +".competences_devoirs cd  ON d.id = cd.id_devoir ")
                .append("where d.id IN "+ Sql.listPrepared(idDevoirs) + " ")
                .append("GROUP by d.id ");

        JsonArray values =  new fr.wseduc.webutils.collections.JsonArray();
        //Ajout des id désirés
        for (Long idDevoir : idDevoirs) {
            values.add(idDevoir);
        }

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    public void getNbCompetencesDevoirsByEleve(List<String> idEleves, Long idDevoir,
                                               Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT count(competences_notes.id_competence) AS nb_competences, id_eleve, id_devoir as id" )
                .append(" FROM  "+ Competences.COMPETENCES_SCHEMA +'.'+ Competences.COMPETENCES_NOTES_TABLE)
                .append(" WHERE id_devoir = ?  AND "+ Competences.COMPETENCES_NOTES_TABLE + ".evaluation >= 0 ");

        // filtre sur les élèves de la classe à l'instant T
        if(idEleves != null && idEleves.size() > 0) {
            query.append(" AND "+ Competences.COMPETENCES_NOTES_TABLE + ".id_eleve IN ")
                    .append(Sql.listPrepared(idEleves.toArray()));
        }


        query.append(" GROUP BY (id_eleve, id_devoir)");

        JsonArray values =  new fr.wseduc.webutils.collections.JsonArray();
        values.add(idDevoir);

        // Ajout des id pour le filtre sur les élèves de la classe à l'instant T
        if(idEleves != null && idEleves.size() > 0) {
            for (String idEleve: idEleves) {
                values.add(idEleve);
            }
        }

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    public void updatePercent(Long idDevoir, Integer percent, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        query.append(" UPDATE " + Competences.COMPETENCES_SCHEMA + "." + Competences.DEVOIR_TABLE )
                .append(" SET percent = ? ")
                .append(" WHERE id = ? ");

        JsonArray values =  new fr.wseduc.webutils.collections.JsonArray();
        values.add(percent);
        values.add(idDevoir);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));

    }

    public void getDevoirsInfosCompetencesCondition(Long[] idDevoirs, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values =  new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id, is_evaluated, CASE WHEN nb_competences > 0 THEN TRUE ELSE FALSE END AS ")
                .append("has_competences, id_groupe FROM notes.rel_devoirs_groupes,")
                .append(" (SELECT count(competences_devoirs.id_devoir) AS nb_competences,")
                .append(" devoirs.id,devoirs.is_evaluated FROM  notes.devoirs LEFT OUTER JOIN notes.competences_devoirs")
                .append(" ON devoirs.id = competences_devoirs.id_devoir  GROUP by (devoirs.id) ) AS res ")
                .append(" WHERE id = id_devoir");

        if (idDevoirs != null) {
            query.append(" AND id IN " + Sql.listPrepared(idDevoirs) + " ");
            //Ajout des id désirés
            for (Long l : idDevoirs) {
                values.add(l);
            }
        }

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    public void getDevoirsInfos(Long[] idDevoirs, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values =  new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT devoir.id, devoir.id_matiere, devoir.id_periode, Gdevoir.id_groupe FROM notes.devoirs devoir ")
                .append("INNER Join notes.rel_devoirs_groupes Gdevoir ON Gdevoir.id_devoir = devoir.id ")
                .append(" WHERE devoir.id IN ");

        if (idDevoirs != null) {
            query.append(Sql.listPrepared(idDevoirs) + " ;");
            //Ajout des id désirés
            for (Long l : idDevoirs) {
                values.add(l);
            }
        }

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void switchVisibilityApprec(Long idDevoir, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("UPDATE "+ Competences.COMPETENCES_SCHEMA + ".devoirs ")
                .append("SET apprec_visible = NOT apprec_visible WHERE id = ? ");

        values.add(idDevoir);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getMatiereTeacherForOneEleveByPeriode(String id_eleve, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        String headQuery = " SELECT DISTINCT devoirs.id_matiere, devoirs.owner, services.coefficient " +
                " FROM "+ Competences.COMPETENCES_SCHEMA + ".devoirs " +
                " INNER JOIN "+ Competences.COMPETENCES_SCHEMA + ".rel_devoirs_groupes " +
                " ON (devoirs.id = rel_devoirs_groupes.id_devoir) "+
                " LEFT JOIN "+ Competences.COMPETENCES_SCHEMA + ".services " +
                " ON (rel_devoirs_groupes.id_groupe = services.id_groupe AND devoirs.owner = services.id_enseignant " +
                "                           AND devoirs.id_matiere = services.id_matiere) ";


                String footerQuery =   " WHERE devoirs.eval_lib_historise = false ";

        String query = "SELECT * " +
                " FROM (" +
                headQuery +
                " INNER JOIN "+ Competences.COMPETENCES_SCHEMA + ".rel_annotations_devoirs " +
                " ON (devoirs.id = rel_annotations_devoirs.id_devoir AND rel_annotations_devoirs.id_eleve = ?) " +
                footerQuery +

                " UNION " + headQuery +
                " INNER JOIN "+ Competences.COMPETENCES_SCHEMA + ".notes " +
                " ON (notes.id_devoir = devoirs.id AND notes.id_eleve = ? ) " +
                footerQuery +

                " UNION " + headQuery +
                " INNER JOIN "+ Competences.COMPETENCES_SCHEMA + ".competences_notes " +
                " ON (competences_notes.id_devoir = devoirs.id AND competences_notes.id_eleve = ?) "+
                footerQuery +

                " UNION " + headQuery +
                " INNER JOIN "+ Competences.COMPETENCES_SCHEMA + ".appreciation_matiere_periode " +
                " ON (appreciation_matiere_periode.id_matiere = devoirs.id_matiere " +
                "           AND appreciation_matiere_periode.id_eleve = ? " +
                "           AND rel_devoirs_groupes.id_groupe = appreciation_matiere_periode.id_classe ) "+

                footerQuery +

                " ) AS res " +
                " ORDER BY res.id_matiere ";

        values.add(id_eleve).add(id_eleve).add(id_eleve).add(id_eleve);

        sql.prepared(query,values,Competences.DELIVERY_OPTIONS,SqlResult.validResultHandler(handler));
    }

    @Override
    public void listDevoirsService(String idEnseignant, String idMatiere, String idGroupe, Handler<Either<String,JsonArray>> handler) {
        String query = "SELECT devoirs.id, devoirs.id_matiere, devoirs.owner, rel_devoirs_groupes.id_groupe" +
                " FROM " + Competences.COMPETENCES_SCHEMA + "." + Competences.DEVOIR_TABLE + " AS devoirs" +
                " LEFT JOIN " + Competences.COMPETENCES_SCHEMA + "." + Competences.REL_DEVOIRS_GROUPES + " AS rel_devoirs_groupes" +
                " ON devoirs.id = rel_devoirs_groupes.id_devoir" +
                " WHERE owner=? AND id_matiere=? AND id_groupe = ?";

        JsonArray values = new JsonArray().add(idEnseignant).add(idMatiere).add(idGroupe);

        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void updateDevoirsService(JsonArray ids, String idMatiere, Handler<Either<String, JsonArray>> handler) {
        String query = "UPDATE " + Competences.COMPETENCES_SCHEMA + "." + Competences.DEVOIR_TABLE
                + " SET id_matiere = ? WHERE id IN " + Sql.listPrepared(ids.getList());

        JsonArray values = new JsonArray().add(idMatiere);
        for(Object o : ids) {
            values.add(o);
        }

        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void delete(JsonArray ids, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + this.resourceTable + " WHERE id IN " + Sql.listPrepared(ids.getList());
        JsonArray values = new JsonArray();
        for(Object o: ids) {
            values.add(o);
        }

        Sql.getInstance().prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getEleveGroups(String id_classe, Handler<Either<String, JsonArray>> result) {
        StringBuilder query = new StringBuilder();

        query.append("MATCH (u:User {profiles: ['Student']})-[:IN]-(:ProfileGroup)-[:DEPENDS]-(c:Class) ")
                .append("WHERE c.id = {idClasse} ")
                .append("WITH u, c MATCH (u)--(g) WHERE g:FunctionalGroup OR g:ManualGroup OR g:ProfileGroup ")
                .append("RETURN u.id as idEleve, COLLECT(DISTINCT g.id) AS id_groupes");

        neo4j.execute(query.toString(), new JsonObject().put("idClasse", id_classe), Neo4jResult.validResultHandler(result));
    }

    @Override
    public void autoCleanSQLTable(Handler<Either<String, JsonObject>> result) {
        String query = "DELETE FROM notes.moyenne_finale AS moy WHERE NOT EXISTS " +
                "(SELECT * FROM notes.devoirs AS dev INNER JOIN notes.rel_devoirs_groupes AS relDevGr ON relDevGr.id_devoir = dev.id" +
                " WHERE moy.id_matiere = dev.id_matiere AND moy.id_periode = dev.id_periode AND moy.id_classe = relDevGr.id_groupe);" +
                " " +
                "DELETE FROM notes.appreciation_classe AS appClass WHERE NOT EXISTS " +
                "(SELECT * FROM notes.devoirs AS dev INNER JOIN notes.rel_devoirs_groupes AS relDevGr ON relDevGr.id_devoir = dev.id" +
                " WHERE appClass.id_matiere = dev.id_matiere AND appClass.id_periode = dev.id_periode" +
                " AND appClass.id_classe = relDevGr.id_groupe);" +
                " " +
                "DELETE FROM notes.appreciation_matiere_periode AS appMatPer WHERE NOT EXISTS " +
                "(SELECT * FROM notes.devoirs AS dev INNER JOIN notes.rel_devoirs_groupes AS relDevGr ON relDevGr.id_devoir = dev.id" +
                "  WHERE appMatPer.id_matiere = dev.id_matiere AND appMatPer.id_periode = dev.id_periode" +
                " AND appMatPer.id_classe = relDevGr.id_groupe);" +
                " " +
                "DELETE FROM notes.element_programme AS elPro WHERE NOT EXISTS " +
                "(SELECT * FROM notes.devoirs AS dev INNER JOIN notes.rel_devoirs_groupes AS relDevGr ON relDevGr.id_devoir = dev.id" +
                " WHERE elPro.id_matiere = dev.id_matiere AND elPro.id_periode = dev.id_periode " +
                "AND elPro.id_classe = relDevGr.id_groupe);";

        Sql.getInstance().raw(query, SqlResult.validRowsResultHandler(result));
    }

    @Override
    public void updateCompetenceNiveauFinalTableAfterDelete(List<String> listEleves, List<String> listGroups, String idMatiere, Long idPeriode, Handler<Either<String, JsonArray>> result) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new JsonArray();
            query.append("DELETE FROM notes.competence_niveau_final AS compNivFin WHERE compNivFin.id_periode = ? AND " +
                    "compNivFin.id_matiere = ? AND compNivFin.id_eleve IN " + Sql.listPrepared(listEleves.toArray()) + " AND NOT EXISTS " +
                    "(SELECT * FROM notes.devoirs AS dev INNER JOIN notes.rel_devoirs_groupes AS relDevGr ON relDevGr.id_devoir = dev.id" +
                    " WHERE compNivFin.id_matiere = dev.id_matiere AND compNivFin.id_periode = dev.id_periode " +
                    "AND relDevGr.id_groupe IN " + Sql.listPrepared(listGroups.toArray()) + ") ");

                values.add(idPeriode);
                values.add(idMatiere);
                for (String eleve : listEleves)
                    values.add(eleve);
                for (String group : listGroups)
                    values.add(group);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(result));
    }

    @Override
    public void updatePositionnementTableAfterDelete(List<String> listEleves, List<String> listGroups, String idMatiere, Long idPeriode, Handler<Either<String, JsonArray>> result) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new JsonArray();
            query.append("DELETE FROM notes.positionnement AS pos WHERE pos.id_periode = ? AND " +
                    "pos.id_matiere = ? AND pos.id_eleve IN " + Sql.listPrepared(listEleves.toArray()) + " AND NOT EXISTS " +
                    "(SELECT * FROM notes.devoirs AS dev INNER JOIN notes.rel_devoirs_groupes AS relDevGr ON relDevGr.id_devoir = dev.id" +
                    " WHERE pos.id_matiere = dev.id_matiere AND pos.id_periode = dev.id_periode " +
                    "AND relDevGr.id_groupe IN " + Sql.listPrepared(listGroups.toArray()) + ") ");

                values.add(idPeriode);
                values.add(idMatiere);
                for (String eleve : listEleves)
                    values.add(eleve);
                for (String group : listGroups)
                    values.add(group);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(result));
    }



    @Override
    public void getFormSaisieDevoir(Long idDevoir, String acceptLanguage, String host,
                                    Handler<Either<String, JsonObject>> handler){
      JsonObject result = new JsonObject();
        getDevoirInfo(idDevoir,  (Either<String, JsonObject> devoirInfo) -> {
          if (devoirInfo.isRight()) {

              final JsonObject devoirInfos = (JsonObject) ((Either.Right) devoirInfo).getValue();

              formatDevoirsInfos(devoirInfos, result);

              // Récupération de la période pour l'export
              Future periodeFuture = getPeriodeForFormaSaisie(devoirInfos, acceptLanguage, host, result, eb);

              // Récupération des élèves de la classe
              Future studentsFuture = getStudentsForFormSaisie(devoirInfos, result, eb);

              // Récupération du libellé de la matière du devoir
              Future subjectFuture = getSubjectsFuture(devoirInfos, result, eb);

              // Récupération du nom de la classe
              Future classeFuture = getClasseFuture(devoirInfos, result, eb);

              // Récupération des compétences du devoir
              Future compFuture = getCompFuture(idDevoir, devoirInfos, result, eb);

              CompositeFuture.all(periodeFuture, studentsFuture, subjectFuture, compFuture, classeFuture)
                      .setHandler(event -> {
                          if(event.failed()){
                              returnFailure("[getFormSaisieDevoir] ", event, handler);
                          }
                          else{
                              handler.handle(new Either.Right<>(result));
                          }
                      });


          } else {
              String error = "Error :can not get informations from postgres tables ";
              log.error(error);
              handler.handle(new Either.Left<>(error));
          }
      });
  }
}
