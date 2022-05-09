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
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.model.Devoir;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.utils.HomeworkUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.share.ShareService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.joda.time.DateTime;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.returnFailure;
import static fr.openent.competences.helpers.DevoirControllerHelper.getDuplicationDevoirHandler;
import static fr.openent.competences.helpers.FormSaisieHelper.*;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.openent.competences.service.impl.DefaultExportService.COEFFICIENT;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.Renders.badRequest;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;
import static org.entcore.common.sql.SqlResult.validResultHandler;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultDevoirService extends SqlCrudService implements fr.openent.competences.service.DevoirService {

    private DefaultUtilsService utilsService;
    private DefaultNoteService noteService;
    private DefaultMatiereService matiereService;
    private DefaultCompetenceNoteService competenceNoteService;
    private final Neo4j neo4j = Neo4j.getInstance();
    private EventBus eb;
    protected static final Logger log = LoggerFactory.getLogger(DefaultDevoirService.class);

    public DefaultDevoirService(EventBus eb) {
        super(Competences.COMPETENCES_SCHEMA, Competences.DEVOIR_TABLE);
        utilsService = new DefaultUtilsService(eb);
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE,eb);
        matiereService = new DefaultMatiereService(eb);
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_NOTES_TABLE);
        this.eb = eb;
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
        final String queryNewDevoirId = "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".devoirs_id_seq') as id";

        sql.raw(queryNewDevoirId, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    final Long devoirId = event.right().getValue().getLong("id");
                    // Limitation du nombre de compétences
                    if(devoir.getJsonArray("competences").size() > Competences.MAX_NBR_COMPETENCE) {
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

        query.append("SELECT devoir.id, devoir.name, devoir.created, devoir.date, devoir.id_etablissement,")
                .append(" devoir.coefficient, devoir.id_matiere, devoir.diviseur, devoir.is_evaluated,")
                .append(" devoir.id_periode, devoir.percent,")
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

    /**
     * Get only devoir
     *
     * @param idDevoir id devoir
     * @param handler  response
     */
    @Override
    public void getDevoir(Long idDevoir, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + this.resourceTable +" WHERE id = ? ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(idDevoir);

        Sql.getInstance().prepared(query,params,SqlResult.validUniqueResultHandler(handler));
    }


    @Override
    public JsonArray createStatement(Long idDevoir, JsonObject devoir, UserInfos user) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray competences = devoir.getJsonArray("competences");

        //Merge_user dans la transaction

        JsonArray paramsForMerge = new fr.wseduc.webutils.collections.JsonArray();
        if(!user.getUserId().equals(devoir.getString("owner")) && null != devoir.getString("owner_name")) {
            paramsForMerge.add(devoir.getString("owner")).add(devoir.getString("owner_name"));
        } else {
            paramsForMerge.add(user.getUserId()).add(user.getUsername());
        }

        if(devoir.containsKey("owner_name")){
            devoir.remove("owner_name");
        }

        StringBuilder queryForMerge = new StringBuilder()
                .append("SELECT " + schema + "merge_users(?,?)");
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
                params.add(HomeworkUtils.formatDate(devoir.getString(attr)).toString());
            } else{
                Boolean isCompetencesAtt = "competencesAdd".equals(attr)
                        ||  "competencesRem".equals(attr)
                        ||  "competenceEvaluee".equals(attr)
                        ||  "competences".equals(attr)
                        || "competencesUpdate".equals(attr);
                if(!(isCompetencesAtt ||  attr.equals(attributeTypeGroupe) ||  attr.equals(attributeIdGroupe))) {
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
        if (devoir.containsKey("competences") && devoir.getJsonArray("competences").size() > 0) {
            JsonArray paramsComp = new fr.wseduc.webutils.collections.JsonArray();
            StringBuilder queryComp = new StringBuilder()
                    .append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA
                            +".competences_devoirs (id_devoir, id_competence, index) VALUES ");
            for(int i = 0; i < competences.size(); i++){
                queryComp.append("(?, ?,").append(i).append(")");
                paramsComp.add(idDevoir);
                paramsComp.add((Number) competences.getLong(i));
                if(i != competences.size() - 1){
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
            oCompetenceNote.put("owner", devoir.getString("owner"));
            StringBuilder queryCompLibre = new StringBuilder()
                    .append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".competences_notes ");
            queryCompLibre.append("( id_devoir ");
            valueParamsLibre.append("( ?");
            paramsCompLibre.add(idDevoir);
            for (String attr : oCompetenceNote.fieldNames()) {
                if(attr.contains("date")){
                    queryCompLibre.append(" , ").append(attr);
                    valueParamsLibre.append(" , to_timestamp(?,'YYYY-MM-DD') ");
                    paramsCompLibre.add(HomeworkUtils.formatDate(oCompetenceNote.getString(attr)).toString());
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
        if(null != devoir.getLong(attributeTypeGroupe) && devoir.getLong(attributeTypeGroupe) > -1){
            JsonArray paramsAddRelDevoirsGroupes = new fr.wseduc.webutils.collections.JsonArray();
            String queryAddRelDevoirsGroupes = new String("INSERT INTO " + Competences.COMPETENCES_SCHEMA +
                    ".rel_devoirs_groupes(id_groupe, id_devoir,type_groupe) VALUES (?, ?, ?)");
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
    public void duplicateDevoir(final JsonObject devoir, final JsonArray classes, final UserInfos user,
                                ShareService shareService, HttpServerRequest request, EventBus eb) {
        final JsonArray ids = new JsonArray();
        JsonArray statements = new JsonArray();
        for (int i = 0; i < classes.size(); i++) {
            String statement = "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".devoirs_id_seq') as id";
            JsonObject statementJO = new JsonObject()
                    .put("statement", statement)
                    .put("values", new JsonArray())
                    .put("action", "prepared");
            statements.add(statementJO);
        }
        Sql.getInstance().transaction(statements, event -> {
            //TODO if/else gérer les erreurs (badRequest) et récupérer les ids
            JsonObject result = event.body();
            if(result.containsKey("status") && "ok".equals(result.getString("status"))){
                JsonArray resultSql = result.getJsonArray("results");
                for(int j = 0; j < resultSql.size(); j++){
                    ids.add(resultSql.getJsonObject(j).getJsonArray("results").getJsonArray(0).getInteger(0));
                }
                //TODO insertDuplication();
                insertDuplication(ids, devoir, classes, user, getDuplicationDevoirHandler(user, shareService, request, eb));
            } else {
                badRequest(request);
            }

        });
//                    counter[0]++;
//                    if (event.isRight()) {
//                        JsonObject o = event.right().getValue();
//                        ids.add(o.getLong("id"));
//                        if (counter[0] == classes.size()) {
//                            insertDuplication(ids, devoir, classes, user, errors[0], getDuplicationDevoirHandler(user,shareService, request,eb));
//                        }
//                    } else {
//                        errors[0]++;
//                    }

    }


    private void insertDuplication(JsonArray ids, JsonObject devoir, JsonArray classes, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        if (ids.size() == classes.size()) {
            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
            JsonArray devoirs = new JsonArray();
            List<String> listClasses = classes.stream()
                    .filter(classe -> classe instanceof JsonObject)
                    .map(classe -> ((JsonObject)classe).getString("id"))
                    .collect(Collectors.toList());

            utilsService.getPeriodes(listClasses, devoir.getString("id_etablissement")).onSuccess(periodes -> {
                JsonObject actualPeriode = new JsonObject();
                for(int i = 0; i < periodes.size(); i++) {
                    JsonObject periode = periodes.getJsonObject(i);
                    String timestamp_begin = periode.getString("timestamp_dt");
                    String timestamp_end = periode.getString("timestamp_fn");
                    DateTime begin = new DateTime(timestamp_begin);
                    DateTime end = new DateTime(timestamp_end);
                    if (!begin.isAfterNow() && !end.isBeforeNow()) {
                        actualPeriode = periode;
                    }
                }
                JsonObject o, g;
                for (int i = 0; i < ids.size(); i++) {
                    try {
                        g = classes.getJsonObject(i);
                        o = HomeworkUtils.formatDevoirForDuplication(devoir);
                        o.put("id_groupe", g.getString("id"));
                        o.put("type_groupe", g.getInteger("type_groupe"));
                        o.put("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                        o.put("date_publication", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                        o.put("id_periode", actualPeriode.getInteger("id_type"));
                        JsonArray tempStatements = this.createStatement(ids.getLong(i), o, user);
                        for (int j = 0; j < tempStatements.size(); j++) {
                            statements.add(tempStatements.getValue(j));
                        }
                        JsonObject devoirtoAdd = new JsonObject().put("id",ids.getLong(i)).put("devoir",o);
                        devoirs.add(devoirtoAdd);
                    } catch (ClassCastException e) {
                        log.error("Next id devoir must be a long Object.");
                        log.error(e);
                    }

                }
                Sql.getInstance().transaction(statements, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> event) {
                        JsonObject result = event.body();
                        if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
                            handler.handle(new Either.Right<String, JsonArray>(devoirs));
                        } else {
                            handler.handle(new Either.Left<String, JsonArray>(result.getString("status")));
                        }
                    }
                });
            });
        } else {
            log.error("An error occured when collecting ids in duplication sequence.");
            handler.handle(new Either.Left<String, JsonArray>("An error occured when collecting ids in duplication sequence."));
        }
    }


    @Override
    public void updateDevoir(String id, JsonObject devoir, Handler<Either<String, JsonArray>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        String old_id_groupe = "";
        if(devoir.containsKey("old_id_groupe")
                && !devoir.getString("old_id_groupe").isEmpty()){
            old_id_groupe = devoir.getString("old_id_groupe");
            devoir.remove("old_id_groupe");
        }
        if(devoir.containsKey("owner_name")){
            devoir.remove("owner_name");
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
                    params.add(HomeworkUtils.formatDate(devoir.getString(attr)).toString());

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
    public void listDevoirs(UserInfos user, String idEtablissement, Integer limit,
                            Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new JsonArray();

        query.append("SELECT devoirs.id, devoirs.name, devoirs.owner, devoirs.created, devoirs.libelle, rel_devoirs_groupes.id_groupe, rel_devoirs_groupes.type_groupe, devoirs.is_evaluated, ")
                .append("devoirs.id_sousmatiere, devoirs.id_periode, devoirs.id_type, devoirs.id_etablissement, devoirs.diviseur, ")
                .append("devoirs.id_etat, devoirs.date_publication, devoirs.id_matiere, devoirs.coefficient, devoirs.ramener_sur, devoirs.percent, ")
                .append("type_sousmatiere.libelle as _sousmatiere_libelle, devoirs.date, devoirs.apprec_visible, ")
                .append("type.nom as _type_libelle, COUNT(competences_devoirs.id) as nbcompetences, users.username as teacher ")
                .append("FROM ").append(COMPETENCES_SCHEMA).append(".devoirs ")
                .append("INNER JOIN ").append(COMPETENCES_SCHEMA).append(".type ON devoirs.id_type = type.id ")
                .append("LEFT JOIN ").append(COMPETENCES_SCHEMA).append(".competences_devoirs ON devoirs.id = competences_devoirs.id_devoir ")
                .append("LEFT JOIN ").append(VSCO_SCHEMA).append(".sousmatiere ON devoirs.id_sousmatiere = sousmatiere.id ")
                .append("LEFT JOIN ").append(VSCO_SCHEMA).append(".type_sousmatiere ON sousmatiere.id_type_sousmatiere = type_sousmatiere.id ")
                .append("LEFT JOIN ").append(COMPETENCES_SCHEMA).append(".rel_devoirs_groupes ON rel_devoirs_groupes.id_devoir = devoirs.id ")
                .append("INNER JOIN ").append(COMPETENCES_SCHEMA).append(".users ON users.id = devoirs.owner ")
                .append("WHERE rel_devoirs_groupes.id_devoir = devoirs.id AND devoirs.id_etablissement = ? ")
                .append("AND devoirs.eval_lib_historise = false AND (devoirs.owner = ? OR ") // devoirs dont on est le propriétaire
                .append("devoirs.owner IN (SELECT DISTINCT main_teacher_id ") // ou dont l'un de mes titulaires le sont (de l'établissement passé en paramètre)
                .append("FROM ").append(VSCO_SCHEMA).append(".multi_teaching ")
                .append("INNER JOIN ").append(COMPETENCES_SCHEMA).append(".devoirs ON devoirs.id_matiere = multi_teaching.subject_id ")
                .append("INNER JOIN ").append(COMPETENCES_SCHEMA).append(".rel_devoirs_groupes ON devoirs.id = rel_devoirs_groupes.id_devoir ")
                .append("AND multi_teaching.class_or_group_id = rel_devoirs_groupes.id_groupe ")
                .append("WHERE second_teacher_id = ? AND multi_teaching.structure_id = ? ")
                .append("AND ((start_date <= current_date AND current_date <= entered_end_date AND NOT is_coteaching) OR is_coteaching)) ")
                .append("OR ? IN (SELECT member_id ") // ou devoirs que l'on m'a partagés (lorsqu'un remplaçant a créé un devoir pour un titulaire par exemple)
                .append("FROM ").append(COMPETENCES_SCHEMA).append(".devoirs_shares ")
                .append("WHERE resource_id = devoirs.id ")
                .append("AND action = '").append(DEVOIR_ACTION_UPDATE).append("')) ")
                .append("GROUP BY devoirs.id, devoirs.name, devoirs.created, devoirs.libelle, rel_devoirs_groupes.id_groupe, devoirs.is_evaluated, users.username, ")
                .append("devoirs.id_sousmatiere, devoirs.id_periode, devoirs.id_type, devoirs.id_etablissement, devoirs.diviseur, ")
                .append("devoirs.id_etat, devoirs.date_publication, devoirs.date, devoirs.id_matiere, rel_devoirs_groupes.type_groupe, ")
                .append("devoirs.coefficient, devoirs.ramener_sur, type_sousmatiere.libelle, type.nom ")
                .append("ORDER BY devoirs.date DESC ");

        // Ajout des params pour les devoirs dont on est le propriétaire sur l'établissement
        values.add(idEtablissement);
        values.add(user.getUserId());

        // Ajout des params pour la récupération des devoirs de mes titulaires
        values.add(user.getUserId());
        values.add(idEtablissement);

        // Ajout des params pour les devoirs que l'on m'a partagés (lorsqu'un remplaçant a créé un devoir pour un titulaire par exemple)
        values.add(user.getUserId());

        if(limit != null && limit > 0) {
            query.append("LIMIT ?");
            values.add(limit);
        }

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void listDevoirsChefEtab(UserInfos user, String idEtablissement, Integer limit,
                                    Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new JsonArray();

        query.append("SELECT devoirs.id, devoirs.name, devoirs.owner, devoirs.created, devoirs.libelle, rel_devoirs_groupes.id_groupe, rel_devoirs_groupes.type_groupe, devoirs.is_evaluated, ")
                .append("devoirs.id_sousmatiere, devoirs.id_periode, devoirs.id_type, devoirs.id_etablissement, devoirs.diviseur, ")
                .append("devoirs.id_etat, devoirs.date_publication, devoirs.id_matiere, devoirs.coefficient, devoirs.ramener_sur, devoirs.percent, ")
                .append("type_sousmatiere.libelle as _sousmatiere_libelle, devoirs.date, devoirs.apprec_visible,")
                .append("type.nom as _type_libelle, COUNT(competences_devoirs.id) as nbcompetences, users.username as teacher ")
                .append("FROM notes.devoirs ")
                .append("INNER JOIN notes.type on devoirs.id_type = type.id ")
                .append("LEFT JOIN notes.competences_devoirs on devoirs.id = competences_devoirs.id_devoir ")
                .append("LEFT JOIN viesco.sousmatiere  on devoirs.id_sousmatiere = sousmatiere.id ")
                .append("LEFT JOIN viesco.type_sousmatiere on sousmatiere.id_type_sousmatiere = type_sousmatiere.id  ")
                .append("LEFT JOIN notes.rel_devoirs_groupes ON rel_devoirs_groupes.id_devoir = devoirs.id ")
                .append("INNER JOIN notes.users on users.id = devoirs.owner ")
                .append("WHERE devoirs.id_etablissement = ? ")
                .append("AND devoirs.eval_lib_historise = false AND id_groupe IS NOT NULL ")
                .append("GROUP BY devoirs.id, devoirs.name, devoirs.created, devoirs.libelle, rel_devoirs_groupes.id_groupe, devoirs.is_evaluated, users.username, ")
                .append("devoirs.id_sousmatiere, devoirs.id_periode, devoirs.id_type, devoirs.id_etablissement, devoirs.diviseur, ")
                .append("devoirs.id_etat, devoirs.date_publication, devoirs.date, devoirs.id_matiere, rel_devoirs_groupes.type_groupe, ")
                .append("devoirs.coefficient, devoirs.ramener_sur, type_sousmatiere.libelle, type.nom ")
                .append("ORDER BY devoirs.date DESC ");

        values.add(idEtablissement);

        if(limit != null && limit > 0) {
            query.append("LIMIT ?");
            values.add(limit);
        }

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }


    @Override
    public void listDevoirs(String idEleve, String idEtablissement, String idClasse, String idMatiere, Long idPeriode,
                            boolean historise, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT devoirs.*, type.nom as _type_libelle, rel_type_periode.type as _periode_type, ")
                .append("rel_type_periode.ordre as _periode_ordre, users.username as teacher, id_groupe ");

        if (idEleve != null) {
            query.append(", notes.valeur as note, COUNT(competences_devoirs.id) as nbcompetences, sum.sum_notes, sum.nbr_eleves ");
        }

        query.append("FROM ").append(Competences.COMPETENCES_SCHEMA).append(".devoirs ")
                .append("LEFT JOIN ").append(Competences.VSCO_SCHEMA).append(".rel_type_periode on devoirs.id_periode = rel_type_periode.id ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".type on devoirs.id_type = type.id ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".users on users.id = devoirs.owner ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".rel_devoirs_groupes ON rel_devoirs_groupes.id_devoir = devoirs.id ");

        if(idClasse != null) {
            query.append("AND rel_devoirs_groupes.id_groupe = ? ");
            values.add(idClasse);
        }

        if (idEleve != null) {
            query.append(" LEFT JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".competences_devoirs ")
                    .append(" ON devoirs.id = competences_devoirs.id_devoir ")
                    .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".notes ON devoirs.id = notes.id_devoir ")
                    .append("INNER JOIN ( SELECT devoirs.id, SUM(notes.valeur) as sum_notes, COUNT(notes.valeur) as nbr_eleves ")
                    .append("FROM notes.devoirs INNER JOIN notes.notes on devoirs.id = notes.id_devoir ")
                    .append("WHERE devoirs.id_etablissement = ? AND date_publication <= Now() ");
            values.add(idEtablissement);
            if (idPeriode != null) {
                query.append("AND devoirs.id_periode = ? ");
                values.add(idPeriode);
            }
            query.append("GROUP BY devoirs.id) sum ON sum.id = devoirs.id ");
        }

        query.append("WHERE devoirs.id_etablissement = ? AND devoirs.eval_lib_historise = ? ");
        values.add(idEtablissement);
        values.add(historise);

        if(idMatiere != null) {
            query.append("AND devoirs.id_matiere = ? ");
            values.add(idMatiere);
        }

        if(idEleve != null){
            query.append(" AND notes.id_eleve = ? AND date_publication <= Now() ");
            values.add(idEleve);
        }

        if(idPeriode != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(idPeriode);
        }

        if (idEleve != null) {
            query.append(" GROUP BY devoirs.id, rel_type_periode.type , rel_type_periode.ordre, type.nom, notes.valeur, sum_notes, nbr_eleves, users.username, id_groupe ")
                    .append(" ORDER BY devoirs.date ASC, devoirs.id ASC");
        } else {
            query.append("ORDER BY devoirs.date ASC, devoirs.id ASC");
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

        query.append("SELECT devoirs.*, rel.id_groupe, users.username as teacher")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + "." + Competences.DEVOIR_TABLE + " AS devoirs")
                .append(" INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".users on users.id = devoirs.owner ")
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
        }
        if (historise) {
            query.append("( devoirs.eval_lib_historise = ? )");
            params.add(historise);
        }
        if (idMatieres.length != 0 || historise) {
            query.append(")  AND");
        }

        query.delete(query.length() - 3, query.length());

        query.append(" UNION ");

        query.append("SELECT devoirs.*, rel.id_groupe, users.username as teacher")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + "." + Competences.DEVOIR_TABLE + " AS devoirs")
                .append(" INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".users on users.id = devoirs.owner ")
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

        if(idGroupes.length != 0) {
            if(historise) {
                query.append(" (");
            }
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

            for (String idMatiere : idMatieres) {
                params.add(idMatiere);
            }
            if (historise) {
                query.append(" (devoirs.id_matiere = '' OR devoirs.id_matiere IN " + Sql.listPrepared(idMatieres) + ") OR");
            } else {
                query.append(" ((devoirs.id_matiere = '' OR devoirs.id_matiere IN " + Sql.listPrepared(idMatieres) + ") ");
            }
        }
        if (historise) {
            query.append("( devoirs.eval_lib_historise = ? )");
            params.add(historise);
        }
        if (idMatieres.length != 0 || historise) {
            query.append(")  AND");
        }

        query.delete(query.length() - 3, query.length());

        Sql.getInstance().prepared(query.toString(), params, DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listDevoirsWithAnnotations(String idEleve, Long idPeriode, String idMatiere,
                                           Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getAnnotations")
                .put("idEleve", idEleve)
                .put("idPeriode", idPeriode)
                .put("idMatiere", idMatiere);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                JsonArray result = body.getJsonArray(RESULTS);
                handler.handle(new Either.Right<>(result));
            } else {
                handler.handle(new Either.Left<>(body.getString("message")));
                log.error("listDevoirsWithAnnotations : " + body.getString("message"));
            }
        }));
    }

    @Override
    public void listDevoirsWithCompetences(String idEleve, Long idPeriode, String idMatiere, JsonArray groups,
                                           Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getCompetences")
                .put("idEleve", idEleve)
                .put("idPeriode", idPeriode)
                .put("idMatiere", idMatiere)
                .put("idGroups", groups);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                JsonArray result = body.getJsonArray(RESULTS);
                handler.handle(new Either.Right<String, JsonArray>(result));
            } else {
                handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                log.error("listDevoirsWithCompetences : " + body.getString("message"));
            }
        }));
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
    public void getNbNotesDevoirs(UserInfos user, Long idDevoir, Handler<Either<String, JsonArray>> handler) {
        boolean isChefEtab = WorkflowActionUtils.hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());

        WorkflowActionUtils.hasHeadTeacherRight(user, null, new JsonArray().add(idDevoir),
                Competences.DEVOIR_TABLE, null, null, null, event -> {
                    Boolean isHeadTeacher = false;
                    if(event.isRight()){
                        isHeadTeacher = event.right().getValue();
                    }
                    HomeworkUtils.getNbNotesDevoirs(user, idDevoir, handler,isChefEtab || isHeadTeacher);
                });
    }

    private StringBuilder buildAnnotationNotNNQuery(JsonArray values, Long idDevoir) {
        StringBuilder res = new StringBuilder()
                .append("SELECT count(rel_annotations_devoirs.id_annotation) AS nb_annotations, 'notNN' as type,")
                .append(" devoirs.id, rel_devoirs_groupes.id_groupe")
                .append(" FROM ").append(Competences.COMPETENCES_SCHEMA).append(".rel_annotations_devoirs")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".devoirs")
                .append(" ON rel_annotations_devoirs.id_devoir = devoirs.id AND devoirs.id = ?")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".rel_devoirs_groupes")
                .append(" ON rel_devoirs_groupes.id_devoir = devoirs.id AND devoirs.id = ?")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".annotations")
                .append(" ON annotations.id = rel_annotations_devoirs.id_annotation")
                .append(" AND NOT annotations.libelle_court = 'NN'")
                .append(" GROUP by devoirs.id, rel_devoirs_groupes.id_groupe");

        values.add(idDevoir);
        values.add(idDevoir);
        return res;
    }

    private StringBuilder buildAnnotationNNQuery(JsonArray values, Long idDevoir) {
        StringBuilder res = new StringBuilder()
                .append("SELECT count(rel_annotations_devoirs.id_annotation) AS nb_annotations, 'NN' as type,")
                .append(" devoirs.id, rel_devoirs_groupes.id_groupe")
                .append(" FROM ").append(Competences.COMPETENCES_SCHEMA).append(".rel_annotations_devoirs")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".devoirs")
                .append(" ON rel_annotations_devoirs.id_devoir = devoirs.id AND devoirs.id = ?")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".rel_devoirs_groupes")
                .append(" ON rel_devoirs_groupes.id_devoir = devoirs.id AND devoirs.id = ?")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".annotations")
                .append(" ON annotations.id = rel_annotations_devoirs.id_annotation")
                .append(" AND annotations.libelle_court = 'NN'")
                .append(" GROUP by devoirs.id, rel_devoirs_groupes.id_groupe");

        values.add(idDevoir);
        values.add(idDevoir);

        return res;
    }

    @Override
    public void getNbAnnotationsDevoirs(Long idDevoir, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new JsonArray();
        StringBuilder queryAnnotationNotNN = buildAnnotationNotNNQuery(values, idDevoir);
        StringBuilder queryAnnotationNN = buildAnnotationNNQuery(values, idDevoir);

        StringBuilder query = new StringBuilder().append(queryAnnotationNotNN)
                .append(" UNION ").append(queryAnnotationNN);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
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
    public void getMoyenne(Long idDevoir, String[] idEleves, final Handler<Either<String, JsonObject>> handler) {
        noteService.getNotesParElevesParDevoirs(idEleves, new Long[]{idDevoir}, event -> {
            if (event.isRight()) {
                ArrayList<NoteDevoir> notes = new ArrayList<>();

                JsonArray listNotes = event.right().getValue();

                for (int i = 0; i < listNotes.size(); i++) {
                    JsonObject note = listNotes.getJsonObject(i);
                    String coef = note.getString("coefficient");
                    if(coef != null) {
                        NoteDevoir noteDevoir = new NoteDevoir(Double.valueOf(note.getString("valeur")),
                                note.getBoolean("ramener_sur"), Double.valueOf(coef));

                        notes.add(noteDevoir);
                    }
                }

                if (!notes.isEmpty()) {
                    handler.handle(new Either.Right<>(utilsService.calculMoyenneParDiviseur(notes, true)));
                } else {
                    handler.handle(new Either.Right<>(new JsonObject()));
                }
            } else {
                log.error("[get Moyenne]: cannot get Eleves class");
                handler.handle(new Either.Left<>(event.left().getValue()));
            }

        });
    }

    @Override
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

    @Override
    public void getNbCompetencesDevoirsByEleve(Long idDevoir, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT count(competences_notes.id_competence) AS nb_competences, id_eleve, id_devoir as id")
                .append(" FROM ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.COMPETENCES_NOTES_TABLE)
                .append(" WHERE id_devoir = ? AND competences_notes.evaluation >= 0 ")
                .append(" GROUP BY (id_eleve, id_devoir)");

        JsonArray values = new JsonArray();
        values.add(idDevoir);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void updatePercent(Long idDevoir, Integer percent, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("UPDATE ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.DEVOIR_TABLE)
                .append(" SET percent = ?")
                .append(" WHERE id = ?");

        JsonArray values = new JsonArray();
        values.add(percent);
        values.add(idDevoir);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
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

    @Override
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
    public void getMatiereTeacherForOneEleve(String idEleve, String idEtablissement, JsonArray idsClass,
                                             Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder()
                .append("WITH res AS ( ")
                .append("SELECT devoirs.id, devoirs.id_matiere, devoirs.owner, services.is_visible, services.coefficient, ")
                .append("devoirs.id_periode, rel_devoirs_groupes.id_groupe ")
                .append("FROM notes.devoirs ")
                .append("INNER JOIN notes.rel_devoirs_groupes ON (devoirs.id = rel_devoirs_groupes.id_devoir) ")
                .append("LEFT JOIN viesco.services ON (rel_devoirs_groupes.id_groupe = services.id_groupe ")
                .append("AND devoirs.owner = services.id_enseignant AND devoirs.id_matiere = services.id_matiere ")
                .append("AND services.id_etablissement = ?) ")
                .append("WHERE devoirs.eval_lib_historise = FALSE AND devoirs.id_etablissement = ?) ");
        values.add(idEtablissement).add(idEtablissement);

        query.append("SELECT res.id_matiere, res.owner, res.is_visible, res.coefficient, res.id_periode, res.id_groupe ")
                .append("FROM res ")
                .append("INNER JOIN notes.notes ON (notes.id_devoir = res.id) ")
                .append("WHERE notes.id_eleve = ? ");
        values.add(idEleve);

        query.append("UNION ");

        query.append("SELECT res.id_matiere, res.owner, res.is_visible, res.coefficient, res.id_periode, res.id_groupe ")
                .append("FROM res ")
                .append("INNER JOIN notes.competences_notes ON (competences_notes.id_devoir = res.id) ")
                .append("WHERE competences_notes.id_eleve = ? ");
        values.add(idEleve);

        query.append("UNION ");

        query.append("SELECT res.id_matiere, res.owner, res.is_visible, res.coefficient, res.id_periode, res.id_groupe ")
                .append("FROM res ")
                .append("INNER JOIN notes.appreciation_matiere_periode ON (appreciation_matiere_periode.id_matiere = res.id_matiere ")
                .append("AND res.id_groupe = appreciation_matiere_periode.id_classe) ")
                .append("WHERE appreciation_matiere_periode.id_eleve = ? ");
        values.add(idEleve);

        query.append("UNION ");

        query.append("SELECT DISTINCT moyenne_finale.id_matiere, services.id_enseignant, services.is_visible, services.coefficient, ")
                .append("moyenne_finale.id_periode, services.id_groupe ")
                .append("FROM notes.moyenne_finale ")
                .append("LEFT JOIN viesco.services ON (moyenne_finale.id_classe = services.id_groupe ")
                .append("AND moyenne_finale.id_matiere = services.id_matiere ")
                .append("AND services.id_etablissement = ?) ")
                .append("WHERE moyenne_finale.id_eleve = ? ");
        values.add(idEtablissement);
        values.add(idEleve);

        query.append("UNION ");

        query.append("SELECT DISTINCT appreciation.id_matiere, raun.user_id_neo AS OWNER, NULL ::boolean AS is_visible, ")
                .append("NULL ::integer AS coefficient, appreciation.id_periode, appreciation.id_classe AS id_groupe ")
                .append("FROM notes.appreciation_matiere_periode AS appreciation ")
                .append("LEFT JOIN notes.rel_appreciations_users_neo AS raun ON appreciation.id = raun.appreciation_matiere_periode_id ")
                .append("WHERE appreciation.id_classe IN ").append(Sql.listPrepared(idsClass));
        for(int i= 0; i < idsClass.size(); i++) values.add(idsClass.getString(i));

        query.append(" AND appreciation.id_eleve = ? ");
        values.add(idEleve);

        query.append("ORDER BY id_periode, id_matiere, coefficient ");

        sql.prepared(query.toString(), values, Competences.DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listDevoirsService(String idEnseignant, String idMatiere, List<String> idGroups, Handler<Either<String,JsonArray>> handler) {
        String query = "SELECT devoirs.id, devoirs.id_matiere, devoirs.owner, rel_devoirs_groupes.id_groupe" +
                " FROM " + Competences.COMPETENCES_SCHEMA + "." + Competences.DEVOIR_TABLE + " AS devoirs" +
                " LEFT JOIN " + Competences.COMPETENCES_SCHEMA + "." + Competences.REL_DEVOIRS_GROUPES + " AS rel_devoirs_groupes" +
                " ON devoirs.id = rel_devoirs_groupes.id_devoir" +
                " WHERE owner=? AND id_matiere=? AND id_groupe IN " + Sql.listPrepared(idGroups) ;

        JsonArray values = new JsonArray().add(idEnseignant).add(idMatiere);
        for(String i : idGroups) {
            values.add(i);
        }
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
                " " /*+
                SUITE à la demande de la MN-1126 on garde les informations ci-après, à voir si cela n'entraine pas de problème
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
                "AND elPro.id_classe = relDevGr.id_groupe);"*/;
        JsonArray values = new JsonArray();
        Sql.getInstance().prepared(query, values, SqlResult.validRowsResultHandler(result));
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

    @Override
    public void getHomeworksFromSubjectAndTeacher(String idSubject, String idTeacher,
                                                  String groupId, Handler<Either<String, JsonArray>> handler){
        String query = "SELECT distinct devoirs.id  " +
                " FROM "+Competences.COMPETENCES_SCHEMA + ".devoirs " +
                " INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_devoirs_groupes rdg " +
                " ON rdg.id_devoir = devoirs.id "+
                " WHERE owner = ? AND id_matiere = ? AND rdg.id_groupe = ?";
        JsonArray params = new JsonArray().add(idTeacher).add(idSubject).add(groupId);
        Sql.getInstance().prepared(query,params,SqlResult.validResultHandler(handler));
    }

    @Override
    public void getDevoirsNotes(String idEtablissement, String idEleve, Long idPeriode,
                                Handler<Either<String, JsonObject>> handler) {
        List<Future> futures = new ArrayList<>();

        Future<JsonArray> devoirsFuture = Future.future();
        listDevoirs(idEleve, idEtablissement, null, null, idPeriode,
                false, devoirsEvent -> formate(devoirsFuture, devoirsEvent));
        futures.add(devoirsFuture);

        Future<JsonArray> annotationsFuture = Future.future();
        listDevoirsWithAnnotations(idEleve, idPeriode, null,
                annotationsEvent -> formate(annotationsFuture, annotationsEvent));
        futures.add(annotationsFuture);

        Future<JsonArray> moyenneFinaleFuture = Future.future();
        noteService.getColonneReleve(new JsonArray().add(idEleve), idPeriode, null, null,
                "moyenne", moyenneFinaleEvent -> formate(moyenneFinaleFuture, moyenneFinaleEvent));
        futures.add(moyenneFinaleFuture);

        Future<JsonArray> matieresFuture = Future.future();
        matiereService.getMatieresEtab(idEtablissement,
                matieresEvent -> formate(matieresFuture, matieresEvent));
        futures.add(matieresFuture);

        Future<JsonArray> groupsFuture = Future.future();
        Utils.getGroupsEleve(eb, idEleve, idEtablissement,
                groupsEvent -> formate(groupsFuture, groupsEvent));
        futures.add(groupsFuture);

        CompositeFuture.all(futures).setHandler(futuresEvent -> {
            if (futuresEvent.failed()) {
                handler.handle(new Either.Left<>(futuresEvent.cause().getMessage()));
            } else {
                JsonObject result = new JsonObject();

                final JsonArray devoirs = devoirsFuture.result();
                final JsonArray annotations = annotationsFuture.result();
                final JsonArray moyennesFinales = moyenneFinaleFuture.result();
                final JsonArray matieres = matieresFuture.result();
                final JsonArray groups = groupsFuture.result();

                Future<JsonArray> servicesFuture = Future.future();
                utilsService.getServices(idEtablissement, groups,
                        servicesEvent -> formate(servicesFuture, servicesEvent)
                );

                Future<JsonArray> multiTeachersFuture = Future.future();
                utilsService.getMultiTeachers(idEtablissement, groups, idPeriode != null ? idPeriode.intValue() : null,
                        multiTeachersEvent -> formate(multiTeachersFuture, multiTeachersEvent)
                );

                CompositeFuture.all(servicesFuture, multiTeachersFuture).setHandler(teachersEvent -> {
                    if (teachersEvent.failed()) {
                        handler.handle(new Either.Left<>(teachersEvent.cause().getMessage()));
                    } else {
                        final JsonArray services = servicesFuture.result();
                        final JsonArray allMultiTeachers = multiTeachersFuture.result();

                        ArrayList<Future> resultsFuture = new ArrayList<>();

                        buildArrayFromHomeworks(result, devoirs, annotations, moyennesFinales, matieres,
                                services, allMultiTeachers, resultsFuture, handler);

                        CompositeFuture.all(resultsFuture).setHandler(resultEvent -> {
                            if (resultEvent.failed()) {
                                handler.handle(new Either.Left<>(resultEvent.cause().getMessage()));
                            } else {
                                setAverageOfSubjects(result, idPeriode);
                                handler.handle(new Either.Right<>(result));
                            }
                        });
                    }
                });
            }
        });
    }

    private void buildArrayFromHomeworks(JsonObject result, JsonArray devoirs, JsonArray annotations,
                                         JsonArray moyennesFinales, JsonArray matieres, JsonArray services,
                                         JsonArray allMultiTeachers, ArrayList<Future> resultsFuture,
                                         Handler<Either<String, JsonObject>> handler) {
        devoirs.addAll(annotations);
        devoirs.addAll(moyennesFinales);
        devoirs.forEach(devoir -> {
            JsonObject devoirJson = (JsonObject) devoir;
            String idMatiere = devoirJson.getString("id_matiere");
            String idClass = devoirJson.containsKey("id_groupe") ? devoirJson.getString("id_groupe") : devoirJson.getString("id_classe");
            String moyenneFinale = devoirJson.containsKey("moyenne")? devoirJson.getString("moyenne") : NN;
            Long idPeriodeDevoir = devoirJson.getLong("id_periode");

            JsonObject matiere = (JsonObject) matieres.stream()
                    .filter(el -> idMatiere.equals(((JsonObject) el).getString("id")))
                    .findFirst().orElse(null);

            JsonObject service = (JsonObject) services.stream()
                    .filter(el -> idMatiere.equals(((JsonObject) el).getString("id_matiere")) &&
                            idClass.equals(((JsonObject) el).getString("id_groupe")))
                    .findFirst().orElse(null);

            JsonArray idsTeachers = new JsonArray();
            Long coefficient = 1L;
            if (service != null) {
                idsTeachers.add(service.getString("id_enseignant"));
                List<Object> multiTeachers = allMultiTeachers.stream()
                        .filter(el -> idMatiere.equals(((JsonObject) el).getString("subject_id")) &&
                                idClass.equals(((JsonObject) el).getString("class_or_group_id")))
                        .collect(Collectors.toList());
                multiTeachers.forEach(multiTeacher -> {
                    JsonObject multiTeacherJson = (JsonObject) multiTeacher;
                    if (multiTeacherJson.getBoolean("is_visible")) {
                        idsTeachers.add(multiTeacherJson.getString("second_teacher_id"));
                    }
                });

                coefficient = service.getLong("coefficient");
            }

            Future<String> resultFuture = Future.future();
            resultsFuture.add(resultFuture);
            buildObjectForSubject(result, idsTeachers, idClass, matiere, coefficient, moyenneFinale,
                    idPeriodeDevoir, idMatiere, devoirJson, resultFuture, handler);
        });
    }

    private void buildObjectForSubject(JsonObject result, JsonArray idsTeachers, String idClass, JsonObject matiere,
                                       Long coefficient, String moyenneFinale, Long idPeriodeDevoir, String idMatiere,
                                       JsonObject devoirJson, Future resultFuture,
                                       Handler<Either<String, JsonObject>> handler) {
        Utils.getLastNameFirstNameUser(eb, idsTeachers, teacherNameEvent -> {
            if (teacherNameEvent.isLeft()) {
                handler.handle(new Either.Left<>(teacherNameEvent.left().getValue()));
            } else {
                String teacherLibelle = teacherNameEvent.right().getValue().values()
                        .stream().map(t -> t.getString("name") + " " + t.getString("firstName"))
                        .collect(Collectors.joining(", "));

                JsonObject resultMatiere = new JsonObject();
                if (!result.containsKey(idMatiere)) {
                    resultMatiere.put("id_groupe", idClass);
                    resultMatiere.put("matiere", matiere != null ? matiere.getString("name") : "");
                    resultMatiere.put("matiere_rank", matiere != null ? matiere.getInteger("rank") : 0);
                    resultMatiere.put("matiere_coeff", coefficient);
                    resultMatiere.put("teacher", teacherLibelle);

                    result.put(idMatiere, resultMatiere);
                } else {
                    resultMatiere = result.getJsonObject(idMatiere);
                }

                if(!NN.equals(moyenneFinale)){
                    JsonObject moyenne = new JsonObject()
                            .put("id_periode", idPeriodeDevoir)
                            .put("moyenne", moyenneFinale);
                    if(resultMatiere.containsKey("moyennes")) {
                        resultMatiere.getJsonArray("moyennes").add(moyenne);
                    } else {
                        resultMatiere.put("moyennes", new JsonArray().add(moyenne));
                    }
                } else {
                    if(resultMatiere.containsKey("devoirs")) {
                        resultMatiere.getJsonArray("devoirs").add(devoirJson);
                    } else {
                        resultMatiere.put("devoirs", new JsonArray().add(devoirJson));
                    }
                }

                resultFuture.complete();
            }
        });
    }

    @Override
    public void getDevoirsEleve(String idEtablissement, String idEleve, String idMatiere, Long idPeriode,
                                Handler<Either<String, JsonObject>> handler){
        List<Future> futures = new ArrayList<>();

        Future<JsonArray> devoirsFuture = Future.future();
        Future<JsonArray> annotationsFuture = Future.future();
        if (idEtablissement != null && idEleve != null) {
            listDevoirs(idEleve, idEtablissement, null, idMatiere,
                    idPeriode, false, devoirsEvent -> formate(devoirsFuture, devoirsEvent)
            );
            futures.add(devoirsFuture);

            listDevoirsWithAnnotations(idEleve, idPeriode, idMatiere,
                    annotationsEvent -> formate(annotationsFuture, annotationsEvent)
            );
            futures.add(annotationsFuture);
        } else {
            handler.handle(new Either.Left<>("getLastDevoirs : missing idEleve or idEtablissement"));
        }

        Future<JsonArray> matieresFuture = Future.future();
        matiereService.getMatieresEtab(idEtablissement,
                matieresEvent -> formate(matieresFuture, matieresEvent)
        );
        futures.add(matieresFuture);

        Future<JsonArray> groupsFuture = Future.future();
        Utils.getGroupsEleve(eb, idEleve, idEtablissement,
                groupsEvent -> formate(groupsFuture, groupsEvent)
        );
        futures.add(groupsFuture);

        CompositeFuture.all(futures).setHandler(futuresEvent -> {
            if (futuresEvent.failed()) {
                handler.handle(new Either.Left<>(futuresEvent.cause().getMessage()));
            } else {
                JsonArray devoirs = devoirsFuture.result();
                JsonArray annotations = annotationsFuture.result();

                JsonArray matieres = matieresFuture.result();
                JsonArray groups = groupsFuture.result();

                Future<JsonArray> servicesFuture = Future.future();
                utilsService.getServices(idEtablissement, groups,
                        servicesEvent -> formate(servicesFuture, servicesEvent)
                );

                Future<JsonArray> multiTeachersFuture = Future.future();
                utilsService.getMultiTeachers(idEtablissement, groups, idPeriode != null ? idPeriode.intValue() : null,
                        multiTeachersEvent -> formate(multiTeachersFuture, multiTeachersEvent)
                );

                Future<JsonArray> devoirWithCompetencesFuture = Future.future();
                listDevoirsWithCompetences(idEleve, idPeriode, idMatiere, groups,
                        devoirsCompetencesEvent -> formate(devoirWithCompetencesFuture, devoirsCompetencesEvent)
                );

                CompositeFuture.all(servicesFuture, multiTeachersFuture, devoirWithCompetencesFuture).setHandler(teachersEvent -> {
                    if (teachersEvent.failed()) {
                        handler.handle(new Either.Left<>(teachersEvent.cause().getMessage()));
                    } else {
                        final JsonArray services = servicesFuture.result();
                        final JsonArray allMultiTeachers = multiTeachersFuture.result();
                        final JsonArray devoirsWithCompetences = devoirWithCompetencesFuture.result();

                        devoirsWithCompetences.addAll(annotations).forEach(devoirCompetences -> {
                            JsonObject devoirCompetencesJson = (JsonObject) devoirCompetences;

                            JsonObject devoir = (JsonObject) devoirs.stream()
                                    .filter(el -> devoirCompetencesJson.getLong("id_devoir").equals(((JsonObject) el).getLong("id")) ||
                                            devoirCompetencesJson.getLong("id_devoir").equals(((JsonObject) el).getLong("id_devoir")))
                                    .findFirst().orElse(null);
                            if(devoir == null)
                                devoirs.add(devoirCompetencesJson);
                        });

                        JsonArray orderedDevoirs = Utils.sortJsonArrayDate("date", devoirs);
                        if(idMatiere == null){
                            while(orderedDevoirs.size() > 10) {
                                orderedDevoirs.remove(orderedDevoirs.size() - 1);
                            }
                        }

                        JsonArray resultsDevoirs = new JsonArray();

                        ArrayList<Future> resultsFuture = new ArrayList<>();
                        buildArrayOfHomeworks(orderedDevoirs, matieres, services, allMultiTeachers, idEleve,
                                resultsDevoirs, resultsFuture, handler);

                        CompositeFuture.all(resultsFuture).setHandler(resultEvent -> {
                            if (resultEvent.failed()) {
                                handler.handle(new Either.Left<>(resultEvent.cause().getMessage()));
                            } else {
                                JsonObject result = new JsonObject();
                                result.put("devoirs", resultsDevoirs);

                                if(idMatiere == null && idPeriode == null){
                                    List<String> idsMatieresDevoirs = devoirs.stream()
                                            .map(devoir -> ((JsonObject) devoir).getString("id_matiere"))
                                            .collect(Collectors.toList());

                                    JsonArray resultMatieres = new JsonArray(matieres.stream()
                                            .filter(matiere -> idsMatieresDevoirs.contains(((JsonObject) matiere).getString("id")))
                                            .collect(Collectors.toList()));

                                    result.put("matieres", resultMatieres);
                                }

                                handler.handle(new Either.Right<>(result));
                            }
                        });
                    }
                });
            }
        });
    }

    private void buildArrayOfHomeworks(JsonArray orderedDevoirs, JsonArray matieres, JsonArray services,
                                       JsonArray allMultiTeachers, String idEleve, JsonArray results,
                                       ArrayList<Future> resultsFuture, Handler<Either<String, JsonObject>> handler) {
        orderedDevoirs.forEach(devoir -> {
            JsonObject devoirJson = (JsonObject) devoir;
            String idMat = devoirJson.getString("id_matiere");
            String idClasse = devoirJson.getString("id_groupe");
            Long idDevoir = devoirJson.containsKey("id_devoir") ? devoirJson.getLong("id_devoir") : devoirJson.getLong("id");

            JsonObject matiere = (JsonObject) matieres.stream()
                    .filter(el -> idMat.equals(((JsonObject) el).getString("id")))
                    .findFirst().orElse(null);

            JsonObject service = (JsonObject) services.stream()
                    .filter(el -> idMat.equals(((JsonObject) el).getString("id_matiere")) &&
                            idClasse.equals(((JsonObject) el).getString("id_groupe")))
                    .findFirst().orElse(null);

            JsonArray idsTeachers = new JsonArray();
            if (service != null) {
                idsTeachers.add(service.getString("id_enseignant"));
                List<Object> multiTeachers = allMultiTeachers.stream()
                        .filter(el -> idMat.equals(((JsonObject) el).getString("subject_id")) &&
                                idClasse.equals(((JsonObject) el).getString("class_or_group_id")))
                        .collect(Collectors.toList());
                multiTeachers.forEach(multiTeacher -> {
                    JsonObject multiTeacherJson = (JsonObject) multiTeacher;
                    if (multiTeacherJson.getBoolean("is_visible")) {
                        idsTeachers.add(multiTeacherJson.getString("second_teacher_id"));
                    }
                });
            }

            Future<Map<String, JsonObject>> teacherNamesFuture = Future.future();
            Utils.getLastNameFirstNameUser(eb, idsTeachers, teacherNameEvent ->
                    formate(teacherNamesFuture, teacherNameEvent)
            );

            Future<JsonArray> competencesFuture = Future.future();
            competenceNoteService.getCompetencesNotesDevoir(idDevoir,
                    competencesEvent -> formate(competencesFuture, competencesEvent)
            );

            Future<String> resultFuture = Future.future();
            resultsFuture.add(resultFuture);
            CompositeFuture.all(teacherNamesFuture, competencesFuture).setHandler(event -> {
                if (event.failed()) {
                    handler.handle(new Either.Left<>(event.cause().getMessage()));
                } else {
                    final Map<String, JsonObject> teachersName = teacherNamesFuture.result();
                    final JsonArray competences = competencesFuture.result();

                    buildObjectForHomework(devoirJson, matiere, idDevoir, idEleve,
                            teachersName, competences, results, resultFuture);
                }
            });
        });
    }

    private void buildObjectForHomework(JsonObject devoirJson, JsonObject matiere, Long idDevoir, String idEleve,
                                        Map<String, JsonObject> teachersName, JsonArray competences, JsonArray results,
                                        Future<String> resultFuture) {
        JsonObject result = new JsonObject();

        String teacherLibelle = teachersName.values().stream()
                .map(t -> t.getString("name") + " " + t.getString("firstName"))
                .collect(Collectors.joining(", "));
        result.put("teacher", teacherLibelle);

        result.put("date", devoirJson.getString("date"));
        result.put("title", devoirJson.getString("name"));
        result.put("matiere", matiere != null ? matiere.getString("name") : "");
        result.put("diviseur", devoirJson.getLong("diviseur"));
        result.put("coefficient", devoirJson.getString("coefficient"));

        String note = devoirJson.getString("note");
        String libelle_court = devoirJson.getString("libelle_court");
        result.put("note", note != null ? note : (libelle_court != null ? libelle_court : "NN"));

        String sum_notes = devoirJson.getString("sum_notes");
        Long nbr_eleves = devoirJson.getLong("nbr_eleves");
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        String moyenne = sum_notes != null && nbr_eleves != null ?
                decimalFormat.format(Double.parseDouble(sum_notes) / nbr_eleves) : "NN";
        result.put("moyenne", moyenne);

        JsonArray competencesDevoirs = new JsonArray(competences.stream()
                .filter(el -> idDevoir.equals(((JsonObject) el).getLong("id_devoir"))
                        && idEleve.equals((((JsonObject) el).getString("id_eleve"))))
                .collect(Collectors.toList()));
        result.put("competences", competencesDevoirs);

        results.add(result);
        resultFuture.complete();
    }

    private void setAverageOfSubjects(JsonObject result, Long idPeriod) {
        for(Map.Entry<String, Object> resultEntry : result.getMap().entrySet()) {
            JsonObject matiere = (JsonObject) resultEntry.getValue();
            List<NoteDevoir> notes = new ArrayList<>();
            JsonArray moyennesFinales = matiere.containsKey("moyennes") ? matiere.getJsonArray("moyennes") : new JsonArray();

            if(matiere.containsKey("devoirs")){
                JsonArray matiereDevoirs = matiere.getJsonArray("devoirs");
                matiereDevoirs.forEach(matiereDevoir -> {
                    JsonObject matiereDevoirJson = (JsonObject) matiereDevoir;
                    if(matiereDevoirJson.containsKey("note")){
                        NoteDevoir note = new NoteDevoir(Double.parseDouble(matiereDevoirJson.getString("note")),
                                matiereDevoirJson.getLong("diviseur").doubleValue(),
                                matiereDevoirJson.getBoolean("ramener_sur"),
                                Double.parseDouble(matiereDevoirJson.getString(COEFFICIENT)));
                        notes.add(note);
                    }
                });
            }

            JsonObject moyenneFinale = null;
            if(idPeriod != null) {
                moyenneFinale = (JsonObject) moyennesFinales.stream()
                        .filter(el -> idPeriod.equals(((JsonObject) el).getLong("id_periode")))
                        .findFirst().orElse(null);
            }

            if(moyenneFinale != null) {
                matiere.put("moyenne", (moyenneFinale.getString("moyenne") != null)? moyenneFinale.getString("moyenne"): NN);
            } else if (!notes.isEmpty()) {
                Double moy = utilsService.calculMoyenne(notes,false, 20, false).getDouble("moyenne");
                matiere.put("moyenne", moy.toString());
            } else {
                matiere.put("moyenne", NN);
            }
            matiere.remove("moyennes");
        }
    }
}
