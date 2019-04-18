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
import fr.openent.competences.service.ElementBilanPeriodiqueService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.openent.competences.Competences.TRANSITION_CONFIG;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;
import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;


public class DefaultElementBilanPeriodiqueService extends SqlCrudService implements ElementBilanPeriodiqueService {

    private static final Logger log = LoggerFactory.getLogger(DefaultElementBilanPeriodiqueService.class);
    private EventBus eb;

    public DefaultElementBilanPeriodiqueService(EventBus eb) {
        super(Competences.COMPETENCES_SCHEMA, null);
        this.eb = eb;
    }



    @Override
    public void insertThematiqueBilanPeriodique (JsonObject thematique, Handler<Either<String, JsonObject>> handler){


        // Insert user in the right table
        final String queryIdSeq =
                "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique_id_seq') as id";

        sql.raw(queryIdSeq, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    Long idThematique = event.right().getValue().getLong("id");
                    String code = thematique.getString("code");
                    // dans le cas des themes manuels, on genere un code à partir de l'id
                    // de la thematique (necessaire pour l'export LSU ensuite)
                    if(code == null) {
                        code = "THE_"+idThematique;
                    }

                    SqlStatementsBuilder statements = new SqlStatementsBuilder();
                    String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique" +
                            "(id, libelle, code, type_elt_bilan_periodique, id_etablissement , personnalise) VALUES (?, ?, ?, ?, ?,?);";

                    JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                            .add(idThematique)
                            .add(thematique.getString("libelle"))
                            .add(code)
                            .add(thematique.getInteger("type"))
                            .add(thematique.getString("idEtablissement"))
                            .add(true);

                    statements.prepared(query, params);
                    Sql.getInstance().prepared(query.toString(), params, validResultHandler(handler));
                }
            }
        }));
    }

    @Override
    public void insertElementBilanPeriodique (JsonObject element, Handler<Either<String, JsonObject>> handler){

        SqlStatementsBuilder statements = new SqlStatementsBuilder();

        final String queryElement =
                "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique_id_seq') as id";

        sql.raw(queryElement, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    Long idElement = event.right().getValue().getLong("id");

                    String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique(" +
                            "id, type_elt_bilan_periodique, id_etablissement ";

                    if(element.getInteger("type") == 1 || element.getInteger("type") == 2){
                        query += ", intitule, description";
                    }
                    if(element.getInteger("type") == 1 || element.getInteger("type") == 3){
                        query += ", id_thematique";
                    }

                    query += ") VALUES (?, ?, ?";
                    JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                            .add(idElement)
                            .add(element.getInteger("type"))
                            .add(element.getString("idEtablissement"));

                    if(element.getInteger("type") == 1 || element.getInteger("type") == 2){
                        query += ", ?, ?";
                        params.add(element.getString("libelle"))
                                .add(element.getString("description"));
                    }
                    if(element.getInteger("type") == 1 || element.getInteger("type") == 3){
                        query += ", ?";
                        params.add(element.getInteger("id_theme"));
                    }

                    query += ") RETURNING *;";

                    statements.prepared(query, params);
                    int type = element.getInteger("type");
                    if(type == 1 || type == 2){
                        insertRelEltIntervenantMatiere(element.getJsonArray("ens_mat"), idElement, statements);
                    }
                    insertRelEltgroupe(element.getJsonArray("classes"), idElement, statements);
                }
                Sql.getInstance().transaction(statements.build(), SqlResult.validRowsResultHandler(handler));
            }
        }));
    }

    /**
     * Enregistremet de la relation élément-intervenants-matieres du nouvel élément
     * (EPI, AP ou parcours) en cours d'insertion.
     * @param intervenantsMatieres association intervenant - matière de élément en cours d'insertion
     * @param elementId id de l'élément
     * @param statements Sql statement builder
     */
    private void insertRelEltIntervenantMatiere(JsonArray intervenantsMatieres, Long elementId, SqlStatementsBuilder statements){

        for (Object o : intervenantsMatieres) {
            JsonObject intervenantMatiere = (JsonObject) o;
            String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA +
                    ".rel_elt_bilan_periodique_intervenant_matiere(id_elt_bilan_periodique, id_intervenant, id_matiere) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT ON CONSTRAINT elt_bilan_period_interv_mat_unique DO NOTHING;";
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                    .add(elementId)
                    .add(intervenantMatiere.getJsonObject("intervenant").getString("id"))
                    .add(intervenantMatiere.getJsonObject("matiere").getString("id"));
            statements.prepared(query, params);
        }
    }

    /**
     * Enregistremet de la relation élément-intervenants-matieres du nouvel élément
     * (EPI, AP ou parcours) en cours d'insertion.
     * @param groupes association intervenant - matière de élément en cours d'insertion
     * @param elementId id de l'élément
     * @param statements Sql statement builder
     */
    private void insertRelEltgroupe(JsonArray groupes, Long elementId, SqlStatementsBuilder statements){

        for (Object o : groupes) {
            JsonObject group = (JsonObject) o;
            String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA +
                    ".rel_elt_bilan_periodique_groupe(id_elt_bilan_periodique, id_groupe, externalid_groupe) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT ON CONSTRAINT elt_bilan_period_groupe_unique DO NOTHING;";
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                    .add(elementId)
                    .add(group.getString("id"))
                    .add(group.getString("externalId"));
            statements.prepared(query, params);
        }
    }

    @Override
    public void getThematiqueBilanPeriodique (Long typeElement, String idEtablissement, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id, libelle, code, personnalise ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique ")
                .append("WHERE type_elt_bilan_periodique = ? AND (id_etablissement = ? OR id_etablissement IS NULL) ");

        params.add(typeElement);
        params.add(idEtablissement);
        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getElementBilanPeriodique (String idEnseignant, List<String> listIdClasses, String idEtablissement, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("(SELECT elt_bilan_periodique.*, thematique_bilan_periodique.libelle, thematique_bilan_periodique.code, string_agg(DISTINCT id_groupe, ',') AS groupes,")
                .append(" array_agg(distinct CONCAT(id_intervenant, ',', id_matiere)) AS intervenants_matieres")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique")
                .append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_groupe ")
                .append(" ON rel_elt_bilan_periodique_groupe.id_elt_bilan_periodique = elt_bilan_periodique.id ");

        if(listIdClasses != null){
            query.append(" AND rel_elt_bilan_periodique_groupe.id_groupe IN " + Sql.listPrepared(listIdClasses));
            for(String c : listIdClasses){
                params.add(c);
            }
        }

        query.append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_intervenant_matiere ")
                .append(" ON rel_elt_bilan_periodique_intervenant_matiere.id_elt_bilan_periodique = elt_bilan_periodique.id ");

        if(idEnseignant != null){
            query.append(" AND rel_elt_bilan_periodique_intervenant_matiere.id_intervenant = ? ");
            params.add(idEnseignant);
        }
        query.append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique ")
                .append(" ON elt_bilan_periodique.id_thematique = thematique_bilan_periodique.id ")
                .append(" WHERE elt_bilan_periodique.id_etablissement = ? ")
                .append(" GROUP BY elt_bilan_periodique.id, thematique_bilan_periodique.libelle, thematique_bilan_periodique.code) ");
        params.add(idEtablissement);
        query.append(" UNION ")
                .append(" (SELECT elt_bilan_periodique.*, thematique_bilan_periodique.libelle, thematique_bilan_periodique.code, string_agg(DISTINCT id_groupe, ',') AS groupes, null ")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique ")
                .append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_groupe ")
                .append(" ON rel_elt_bilan_periodique_groupe.id_elt_bilan_periodique = elt_bilan_periodique.id ");

        if(listIdClasses != null){
            query.append(" AND rel_elt_bilan_periodique_groupe.id_groupe IN " + Sql.listPrepared(listIdClasses));
            for(String c : listIdClasses){
                params.add(c);
            }
        }
        query.append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique ")
                .append(" ON elt_bilan_periodique.id_thematique = thematique_bilan_periodique.id ")
                .append(" WHERE elt_bilan_periodique.id_etablissement = ? ")
                .append(" AND intitule is null ")
                .append(" GROUP BY elt_bilan_periodique.id, thematique_bilan_periodique.libelle, thematique_bilan_periodique.code) ");
        params.add(idEtablissement);

        query.append(" UNION ")
                .append(" (SELECT elt_bilan_periodique.*, null, null, string_agg(DISTINCT id_groupe, ',') AS groupes, ")
                .append(" array_agg(distinct CONCAT(id_intervenant, ',', id_matiere)) AS intervenants_matieres")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique ")
                .append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_groupe ")
                .append(" ON rel_elt_bilan_periodique_groupe.id_elt_bilan_periodique = elt_bilan_periodique.id ");

        if(listIdClasses != null){
            query.append(" AND rel_elt_bilan_periodique_groupe.id_groupe IN "  + Sql.listPrepared(listIdClasses));
            for(String c : listIdClasses){
                params.add(c);
            }
        }

        query.append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_intervenant_matiere ")
                .append(" ON rel_elt_bilan_periodique_intervenant_matiere.id_elt_bilan_periodique = elt_bilan_periodique.id ");

        if(idEnseignant != null){
            query.append(" AND rel_elt_bilan_periodique_intervenant_matiere.id_intervenant = ? ");
            params.add(idEnseignant);
        }
        query.append(" WHERE id_etablissement = ? ")
                .append(" AND id_thematique is null ")
                .append(" GROUP BY elt_bilan_periodique.id) ");
        params.add(idEtablissement);

        Sql.getInstance().prepared(query.toString(), params,
                new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG
                        .getInteger("timeout-transaction") * 1000L),
                SqlResult.validResultHandler(handler));
    }


    @Override
    public void getElementsBilanPeriodique (String idEnseignant, List<String> idClasse, String idEtablissement,
                                            Handler<Either<String, JsonArray>> handler) {

        getElementBilanPeriodique(idEnseignant,
                idClasse,
                idEtablissement,
                new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            JsonArray result = event.right().getValue();

                            List<String> idMatieres = new ArrayList<>();
                            List<String> idClasses = new ArrayList<>();
                            List<String> idUsers = new ArrayList<>();

                            for(Object r : result){
                                JsonObject element = (JsonObject)r;

                                String[] arrayIdClasses = element.getString("groupes").split(",");
                                JsonArray jsonArrayIntsMats = element.getJsonArray("intervenants_matieres");

                                for(int i = 0; i < arrayIdClasses.length; i++){
                                    if(!idClasses.contains(arrayIdClasses[i])){
                                        idClasses.add(arrayIdClasses[i]);
                                    }
                                }

                                if(jsonArrayIntsMats != null){
                                    for(Object o : jsonArrayIntsMats){
                                        JsonArray jsonArrayIntMat = (JsonArray) o;
                                        String[] arrayIntMat = jsonArrayIntMat.getString(1).split(",");
                                        if(!idUsers.contains(arrayIntMat[0])){
                                            idUsers.add(arrayIntMat[0]);
                                        }
                                        if(arrayIntMat.length > 1 && !idMatieres.contains(arrayIntMat[1])){
                                            idMatieres.add(arrayIntMat[1]);
                                        }
                                    }
                                }
                            }

                            // récupération des noms des matières
                            getSubjectNames(idMatieres,idClasses,idUsers, result, handler);

                        } else{
                            handler.handle(event.left());
                        }
                    }
                });
    }

    private void  getSubjectNames(List<String> idMatieres ,
                                  List<String> idClasses ,
                                  List<String> idUsers ,
                                  JsonArray result,
                                  Handler<Either<String, JsonArray>> handler){
        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatieres")
                .put("idMatieres", new fr.wseduc.webutils.collections.JsonArray(idMatieres));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();

                        if ("ok".equals(body.getString("status"))) {
                            JsonArray matieres = body.getJsonArray("results");
                            Map<String, String> matieresMap = new HashMap<String, String>();

                            for(Object o : matieres){
                                JsonObject matiere = (JsonObject)o;
                                matieresMap.put(matiere.getString("id"),
                                        matiere.getString("name"));
                            }


                            // récupération des noms des classes/groupes
                            getClassesGroupesName(idMatieres, idClasses, idUsers, matieres, matieresMap, result,
                                    handler);
                        } else{
                            String _message = body.getString("message");
                            log.error(_message);
                            handler.handle(new Either.Left<>(_message));
                        }
                    }
                }));
    }

    private void getClassesGroupesName(List<String> idMatieres, List<String> idClasses, List<String> idUsers,
                                       JsonArray matieres, Map<String, String> matieresMap,
                                       JsonArray result,Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "classe.getClassesInfo")
                .put("idClasses",
                        new fr.wseduc.webutils.collections.JsonArray(idClasses));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();

                        if ("ok".equals(body.getString("status"))) {
                            JsonArray classes = body.getJsonArray("results");
                            Map<String, String> classesNameMap =
                                    new HashMap<String, String>();
                            Map<String, String> classesExternalIdMap =
                                    new HashMap<String, String>();
                            for(Object o : classes){
                                JsonObject classe = (JsonObject)o;
                                classesNameMap.put(classe.getString("id"),
                                        classe.getString("name"));
                                classesExternalIdMap.put(
                                        classe.getString("id"),
                                        classe.getString("externalId"));
                            }

                            // récupération des noms des intervenants
                            getTeacherName(idMatieres, idClasses, idUsers, matieres, matieresMap, result,
                                    classes, classesNameMap, classesExternalIdMap, handler);
                        } else{
                            String _message = body.getString("message");
                            log.error(_message);
                            handler.handle(new Either.Left<>(_message));
                        }
                    }
                }));

    }

    private void getTeacherName(List<String> idMatieres, List<String> idClasses, List<String> idUsers,
                                JsonArray matieres, Map<String, String> matieresMap,
                                JsonArray result, JsonArray classes,
                                Map<String, String> classesNameMap ,
                                Map<String, String> classesExternalIdMap, Handler<Either<String, JsonArray>> handler) {

        JsonObject action = new JsonObject()
                .put("action", "user.getUsers")
                .put("idUsers", idUsers);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                handlerToAsyncHandler(
                        new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(
                                    Message<JsonObject> message) {
                                JsonObject body = message.body();

                                if ("ok".equals(body
                                        .getString("status"))) {
                                    JsonArray users = body.getJsonArray("results");
                                    Map<String, String> usersMap = new HashMap<String, String>();
                                    for(Object o : users){
                                        JsonObject user = (JsonObject)o;
                                        usersMap.put(user.getString("id"),user.getString("displayName"));
                                    }

                                    JsonArray parsedElems = new fr.wseduc.webutils.collections.JsonArray();
                                    for(Object o  : result){
                                        JsonObject element = (JsonObject) o;
                                        JsonObject parsedElem = new JsonObject();

                                        parsedElem.put("id", element.getInteger("id"));
                                        parsedElem.put("type",element.getInteger("type_elt_bilan_periodique"));

                                        if(element
                                                .getString("intitule")
                                                != null){
                                            parsedElem.put("libelle",element.getString("intitule"));
                                            parsedElem.put("description",element.getString("description"));
                                        }

                                        if(element.getInteger("id_thematique") != null){
                                            JsonObject theme = new JsonObject();
                                            theme.put("id", element.getInteger("id_thematique"));
                                            theme.put("libelle", element.getString("libelle"));
                                            theme.put("code", element.getString("code"));
                                            parsedElem.put("theme", theme);
                                        }

                                        String[] arrayIdGroupes = element.getString("groupes").split(",");
                                        JsonArray groupes = new fr.wseduc.webutils.collections.JsonArray();

                                        for(int i = 0; i < arrayIdGroupes.length; i++){
                                            JsonObject groupe = new JsonObject();
                                            groupe.put("id", arrayIdGroupes[i]);
                                            groupe.put("name", classesNameMap.get(arrayIdGroupes[i]));
                                            groupe.put("externalId", classesExternalIdMap.get(arrayIdGroupes[i]));
                                            groupes.add(groupe);
                                        }
                                        parsedElem.put("groupes", groupes);

                                        if(element.getJsonArray("intervenants_matieres") != null){

                                            JsonArray intMat = element.getJsonArray("intervenants_matieres");
                                            JsonArray intervenantsMatieres =
                                                    new fr.wseduc.webutils.collections.JsonArray();

                                            for(int i = 0; i < intMat.size(); i++){
                                                String[] intMatArray = intMat.getJsonArray(i)
                                                        .getString(1).split(",");
                                                JsonObject intervenantMatiere = new JsonObject();

                                                JsonObject intervenant = new JsonObject();
                                                intervenant.put("id", intMatArray[0]);
                                                intervenant.put("displayName", usersMap.get(intMatArray[0]));
                                                intervenantMatiere.put("intervenant", intervenant);
                                                if(intMatArray.length > 1 ){
                                                    JsonObject matiere = new JsonObject();
                                                    matiere.put("id", intMatArray[1]);
                                                    matiere.put("name", matieresMap.get(intMatArray[1]));
                                                    intervenantMatiere.put("matiere", matiere);
                                                }

                                                intervenantsMatieres.add(intervenantMatiere);
                                            }
                                            parsedElem.put("intervenantsMatieres", intervenantsMatieres);
                                        }

                                        parsedElems.add(parsedElem);
                                    }
                                    handler.handle(new Either.Right<>(parsedElems));
                                } else{
                                    String _message = body.getString("message");
                                    log.error(_message);
                                    handler.handle(new Either.Left<>(_message));
                                }
                            }
                        }));
    }
    @Override
    public void getEnseignantsElementsBilanPeriodique (List<String> idElements, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id_elt_bilan_periodique, id_intervenant, id_matiere ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_intervenant_matiere ")
                .append("WHERE id_elt_bilan_periodique IN " + Sql.listPrepared(idElements));

        for (int i = 0; i < idElements.size(); i++) {
            params.add(idElements.get(i));
        }

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    private static final String _elementBilanPerdiodiqueParcours = "3";

    @Override
    public void getClassesElementsBilanPeriodique (String idEtablissement, String idEnseignant,Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query
                .append("SELECT id_groupe ")
                .append(" FROM notes.rel_elt_bilan_periodique_intervenant_matiere ")
                .append("  INNER JOIN notes.rel_elt_bilan_periodique_groupe ")
                .append("  ON rel_elt_bilan_periodique_intervenant_matiere.id_elt_bilan_periodique = rel_elt_bilan_periodique_groupe.id_elt_bilan_periodique ")
                .append(" WHERE id_intervenant = ? ")
                .append(" UNION ")
                .append("SELECT id_groupe ")
                .append(" FROM notes.rel_elt_bilan_periodique_groupe ")
                .append("  INNER JOIN notes.elt_bilan_periodique ")
                .append("  ON rel_elt_bilan_periodique_groupe.id_elt_bilan_periodique = elt_bilan_periodique.id ")
                .append(" WHERE type_elt_bilan_periodique = ? ")
                .append("  AND id_etablissement = ? ");
        params.add(idEnseignant);
        params.add(_elementBilanPerdiodiqueParcours);
        params.add(idEtablissement);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getGroupesElementBilanPeriodique (String idElement, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id_groupe, externalid_groupe ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_groupe ")
                .append("WHERE id_elt_bilan_periodique = ? ");
        params.add(idElement);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getIntervenantMatiereElementBilanPeriodique (String idElement, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id_intervenant, id_matiere ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_intervenant_matiere ")
                .append("WHERE id_elt_bilan_periodique = ? ");
        params.add(idElement);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getApprecBilanPerClasse (List<String> idsClasses, String idPeriode, List<String> idElements, Handler<Either<String, JsonArray>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe.* " +
                "FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe ";

        query += " WHERE " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe.id_elt_bilan_periodique IN " + Sql.listPrepared(idElements);

        for (int i = 0; i < idElements.size(); i++) {
            params.add(idElements.get(i));
        }

        if(idPeriode != null){
            query += " AND " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe.id_periode = ? ";
            params.add(idPeriode);
        }

        if(idsClasses != null && idsClasses.size() > 0){
            query += " AND " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe.id_groupe IN " + Sql.listPrepared(idsClasses);
            for (int i = 0; i < idsClasses.size(); i++) {
                params.add(idsClasses.get(i));
            }
        }

        Sql.getInstance().prepared(query, params,
                new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG
                        .getInteger("timeout-transaction") * 1000L),
                SqlResult.validResultHandler(handler));
    }

    /**
     *
     * @param idsClasses
     * @param idPeriode
     * @param idElements id des élèments du bilan périodique, si null on ramène tous les ID et on considère que le code thematique est necessaire
     * @param idEleve
     * @param handler Handler de retour
     */
    @Override
    public void getApprecBilanPerEleve (List<String> idsClasses, String idPeriode, List<String> idElements, String idEleve, Handler<Either<String, JsonArray>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT DISTINCT " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve.* ";

        if (idElements == null) {
            query += ", rel_groupe_appreciation_elt_eleve.id_groupe , thematiqueBP.code, elBP.type_elt_bilan_periodique ";
        }
                query += "FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve ";

        if(idsClasses != null && idsClasses.size() > 0){
            query += " INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_appreciation_elt_eleve " +
                    " ON rel_groupe_appreciation_elt_eleve.id_groupe IN " + Sql.listPrepared(idsClasses);
            for (int i = 0; i < idsClasses.size(); i++) {
                params.add(idsClasses.get(i));
            }
        }

        if(idElements != null) {
            query += " WHERE " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve.id_elt_bilan_periodique IN " + Sql.listPrepared(idElements);
            for (int i = 0; i < idElements.size(); i++) {
                params.add(idElements.get(i));
            }
        }
        else {
            query += " LEFT JOIN " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique AS elBP ON elBP.id =  "+ Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve.id_elt_bilan_periodique ";
            query += " LEFT JOIN " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique AS thematiqueBP ON thematiqueBP.id = elBP.id_thematique ";
        }

        if(idPeriode != null){
            query += idElements == null ? " WHERE " : " AND ";
            query += Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve.id_periode = ? ";
            params.add(idPeriode);
        }

        if(idEleve != null){
            query += idElements == null && idPeriode == null ? " WHERE " : " AND ";
            query += Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve.id_eleve = ? ";
            params.add(idEleve);
        }

        Sql.getInstance().prepared(query, params,
                new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG
                        .getInteger("timeout-transaction") * 1000L),
                SqlResult.validResultHandler(handler));
    }

    @Override
    public void insertOrUpdateAppreciationElement (String idEleve, String idClasse, String externalidClasse, Long idPeriode, Long idEltBilanPeriodique,
                                                   String commentaire, JsonArray groupes, Handler<Either<String, JsonObject>> handler){
        if(commentaire.length() == 0){
            List<String> idGroupes = new ArrayList<String>();
            for(Object o : groupes){
                JsonObject groupe = (JsonObject) o;
                idGroupes.add(groupe.getString("id_groupe"));
            }
            deleteAppreciationElement(idEleve, idPeriode, idEltBilanPeriodique, idClasse, idGroupes, handler);
        }
        else {
            SqlStatementsBuilder statements = new SqlStatementsBuilder();

            String queryAppreciation = "";
            if(idEleve != null){
                queryAppreciation = "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve_id_seq') as id";
            }
            else {
                queryAppreciation = "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe_id_seq') as id";
            }

            sql.raw(queryAppreciation, SqlResult.validUniqueResultHandler(
                    new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> event) {
                            if (event.isRight()) {
                                Long idAppreciation = event.right().getValue().getLong("id");
                                String query = "";
                                JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

                                params.add(idAppreciation);
                                if(idEleve != null){
                                    query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve" +
                                            "(id, id_eleve, id_elt_bilan_periodique, id_periode, commentaire) VALUES (?, ?, ?, ?, ?)" +
                                            " ON CONFLICT ON CONSTRAINT appreciation_elt_bilan_period_eleve_unique DO UPDATE SET commentaire = ?";
                                    params.add(idEleve);
                                } else {
                                    query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe" +
                                            "(id, id_groupe, externalid_groupe, id_elt_bilan_periodique, id_periode, commentaire) VALUES (?, ?, ?, ?, ?, ?)" +
                                            " ON CONFLICT ON CONSTRAINT appreciation_elt_bilan_period_classe_unique DO UPDATE SET commentaire = ?";
                                    params.add(idClasse)
                                            .add(externalidClasse);
                                }
                                params.add(idEltBilanPeriodique)
                                        .add(idPeriode)
                                        .add(commentaire)
                                        .add(commentaire);

                                statements.prepared(query, params);

                                if(idEleve != null) {
                                    insertRelGroupeApprecEleve(groupes, idEleve, idPeriode, idEltBilanPeriodique, statements);
                                }

                            }
                            Sql.getInstance().transaction(statements.build(), SqlResult.validRowsResultHandler(handler));
                        }
                    }));
        }
    }

    private void insertRelGroupeApprecEleve(JsonArray groupes,
                                            String idEleve, Long idPeriode, Long idEltBilanPeriodique,
                                            SqlStatementsBuilder statements){
        for (Object o : groupes) {
            JsonObject group = (JsonObject) o;
            String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA +
                    ".rel_groupe_appreciation_elt_eleve(id_groupe, externalid_groupe, id_eleve, id_periode, id_elt_bilan_periodique) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT ON CONSTRAINT groupe_appreciation_elt_eleve_unique DO NOTHING;";
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                    .add(group.getString("id_groupe"))
                    .add(group.getString("externalid_groupe"))
                    .add(idEleve)
                    .add(idPeriode)
                    .add(idEltBilanPeriodique);
            statements.prepared(query, params);
        }
    }

    private void insertRelGroupeApprecGroupe(String idClasse, String externalidClasse, Long idPeriode,
                                             Long idEltBilanPeriodique, SqlStatementsBuilder statements){
        String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA +
                ".rel_groupe_appreciation_elt_classe(id_groupe, externalid_groupe, id_periode, id_elt_bilan_periodique) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT ON CONSTRAINT groupe_appreciation_elt_classe_unique DO NOTHING;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idClasse)
                .add(externalidClasse)
                .add(idPeriode)
                .add(idEltBilanPeriodique);
        statements.prepared(query, params);
    }

    @Override
    public void deleteAppreciationElement (String idEleve, Long idPeriode, Long idEltBilanPeriodique, String idClasse,
                                           List<String> groupes, Handler<Either<String, JsonObject>> handler){

        SqlStatementsBuilder statements = new SqlStatementsBuilder();
        deleteAppreciation(idEleve, idPeriode, idEltBilanPeriodique, idClasse, groupes,statements);
        Sql.getInstance().transaction(statements.build(), SqlResult.validRowsResultHandler(handler));
    }

    private void deleteAppreciation (String idEleve, Long idPeriode, Long idEltBilanPeriodique,
                                     String idClasse, List<String> groupes, SqlStatementsBuilder statements){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "";

        if(idEleve != null){
            query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve " +
                    " WHERE id_eleve = ?" +
                    " AND id_elt_bilan_periodique = ? " +
                    " AND id_periode = ? ";
            params.add(idEleve);
        }
        else {
            query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe" +
                    " WHERE id_groupe = ?" +
                    " AND id_elt_bilan_periodique = ? " +
                    " AND id_periode = ? ";
            params.add(idClasse);
        }
        params.add(idEltBilanPeriodique)
                .add(idPeriode);

        statements.prepared(query, params);

        if(idEleve != null) {
            deleteRelGroupeApprecEleve(idEleve, idPeriode, idEltBilanPeriodique, groupes, statements);
        }
    }

    private void deleteRelGroupeApprecEleve(String idEleve, Long idPeriode, Long idEltBilanPeriodique,
                                            List<String> groupes,SqlStatementsBuilder statements){
        for (Object o : groupes) {
            String groupe = (String) o;
            String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_appreciation_elt_eleve " +
                    "WHERE id_eleve = ?" +
                    " AND id_periode = ? " +
                    " AND id_elt_bilan_periodique = ? " +
                    " AND id_groupe = ? ";

            JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                    .add(idEleve)
                    .add(idPeriode)
                    .add(idEltBilanPeriodique)
                    .add(groupe);
            statements.prepared(query, params);
        }
    }

    @Override
    public void updateElementBilanPeriodique (Long idElement, JsonObject element,
                                              JsonArray apprecClasseOnDeletedClasses,
                                              JsonObject apprecEleveOnDeletedClasses,
                                              List<String> deletedClasses,
                                              Handler<Either<String, JsonObject>> handler){

        SqlStatementsBuilder statements = new SqlStatementsBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "UPDATE " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique SET ";

        if(element.getInteger("type") == 1 || element.getInteger("type") == 2){
            query += "intitule = ?, description = ?, ";
        }
        if(element.getInteger("type") == 1 || element.getInteger("type") == 3){
            query += "id_thematique = ?, ";
        }

        query = query.substring(0, query.length() - 2);

        query += " WHERE id = ?;";

        if(element.getInteger("type") == 1 || element.getInteger("type") == 2){
            params.add(element.getString("libelle"))
                    .add(element.getString("description"));
        }
        if(element.getInteger("type") == 1 || element.getInteger("type") == 3){
            params.add(element.getInteger("id_theme"));
        }

        params.add(idElement);

        statements.prepared(query, params);

        int type = element.getInteger("type");
        if(type == 1 || type == 2){
            //suppression des relations element - enseignant/matiere
            deleteRelEltIntervenantMatiere(idElement, statements);
            insertRelEltIntervenantMatiere(element.getJsonArray("ens_mat"), idElement, statements);
        }

        //suppression des relations entre l'élément et les anciennes classes
        deleteRelEltgroupe(idElement, statements);

        //insertion des relations entre l'élément et les nouvelles classes
        insertRelEltgroupe(element.getJsonArray("classes"), idElement, statements);

        //suppression des appréciations classe sur les anciennes classes
        for(Object a : apprecClasseOnDeletedClasses){
            JsonObject apprec = (JsonObject) a;
            deleteAppreciation(null, apprec.getLong("id_periode"),
                    apprec.getLong("id_elt_bilan_periodique"),
                    apprec.getString("id_groupe"), null, statements);
        }


        if(apprecEleveOnDeletedClasses != null && apprecEleveOnDeletedClasses.size() > 0){

            //suppression des appréciations élève sur les anciennes classes et des relations apprec-classe
            JsonArray apprecDeletedAlone = apprecEleveOnDeletedClasses.getJsonArray("apprecDeletedAlone");
            for(Object a : apprecDeletedAlone){
                JsonObject apprec = (JsonObject) a;
                deleteAppreciation(apprec.getString("id_eleve"),
                        apprec.getLong("id_periode"),
                        apprec.getLong("id_elt_bilan_periodique"),
                        null, deletedClasses, statements);
            }

            //suppression des relations entre les appréciations élève sur les anciennes classes
            // pour les appréciations élève existant sur les nouvelles classes aussi
            JsonArray apprecDeletedConcurrent = apprecEleveOnDeletedClasses.getJsonArray("apprecDeletedConcurrent");
            for(Object a : apprecDeletedConcurrent){
                JsonObject apprec = (JsonObject) a;
                deleteRelGroupeApprecEleve(apprec.getString("id_eleve"),
                        apprec.getLong("id_periode"),
                        apprec.getLong("id_elt_bilan_periodique"),
                        deletedClasses, statements);
            }
        }

        Sql.getInstance().transaction(statements.build(), SqlResult.validRowsResultHandler(handler));
    }

    private void deleteRelEltgroupe (Long idEltBilanPeriodique, SqlStatementsBuilder statements){

        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_groupe " +
                "WHERE id_elt_bilan_periodique = ? ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idEltBilanPeriodique);

        statements.prepared(query, params);
    }

    private void deleteRelEltIntervenantMatiere (Long idEltBilanPeriodique, SqlStatementsBuilder statements){
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_intervenant_matiere " +
                "WHERE id_elt_bilan_periodique = ? ";
        params.add(idEltBilanPeriodique);

        statements.prepared(query, params);
    }

    @Override
    public void deleteElementBilanPeriodique (List<String> idsEltBilanPeriodique, Handler<Either<String, JsonArray>> handler){

        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (String id : idsEltBilanPeriodique) {
            params.add(id);
        }

        String queryDelAppEleve = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve WHERE id_elt_bilan_periodique IN "
                + Sql.listPrepared(idsEltBilanPeriodique);

        String queryDelRelAppEleveGroup = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_appreciation_elt_eleve WHERE id_elt_bilan_periodique IN "
                + Sql.listPrepared(idsEltBilanPeriodique);

        String queryDelAppClasse = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe WHERE id_elt_bilan_periodique IN "
                + Sql.listPrepared(idsEltBilanPeriodique);

        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique WHERE id IN " + Sql.listPrepared(idsEltBilanPeriodique);

        statements.add(new JsonObject()
                .put("statement", queryDelAppEleve.toString())
                .put("values", params)
                .put("action", "prepared"));
        statements.add(new JsonObject()
                .put("statement", queryDelRelAppEleveGroup.toString())
                .put("values", params)
                .put("action", "prepared"));
        statements.add(new JsonObject()
                .put("statement", queryDelAppClasse.toString())
                .put("values", params)
                .put("action", "prepared"));
        statements.add(new JsonObject()
                .put("statement", query.toString())
                .put("values", params)
                .put("action", "prepared"));
        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteThematique (String idThematique, Handler<Either<String, JsonArray>> handler){
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique " +
                "WHERE id = ? ";
        params.add(idThematique);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void updateThematique (String idThematique, JsonObject thematique,
                                  Handler<Either<String, JsonObject>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query ="UPDATE notes.thematique_bilan_periodique " +
                "SET libelle = ?, code = ? WHERE id = ?";
        params.add(thematique.getString("libelle"))
                .add(thematique.getString("code"))
                .add(idThematique);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getElementsOnThematique (String idThematique, Handler<Either<String, JsonArray>> handler){
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT * FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique " +
                "WHERE id_thematique = ? ";
        params.add(idThematique);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void updateAppreciationBilanPeriodique (Long idAppreciation, String commentaire, String type,
                                                   Handler<Either<String, JsonObject>> handler){

        String query = "";
        if("eleve".equals(type)){
            query = "UPDATE notes.appreciation_elt_bilan_periodique_eleve " +
                    "SET commentaire = ? WHERE id = ?";
        } else {
            query = "UPDATE notes.appreciation_elt_bilan_periodique_classe " +
                    "SET commentaire = ? WHERE id = ?";
        }
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(commentaire)
                .add(idAppreciation);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getAppreciations (List<String> idsClasses, String idPeriode, List<String> idElements, String idEleve,
                                  Handler<Either<String, JsonArray>> handler) {

        getApprecBilanPerClasse(idsClasses, idPeriode, idElements,
                new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if(event.isRight()){
                            JsonArray apprecClasses = event.right().getValue();
                            getApprecBilanPerEleve(idsClasses, idPeriode,idElements,idEleve,
                                    new Handler<Either<String, JsonArray>>() {
                                        @Override
                                        public void handle(Either<String, JsonArray> event) {
                                            if(event.isRight()){
                                                JsonArray apprecEleves = event.right().getValue();
                                                handler.handle(new Either.Right<>(apprecClasses.addAll(apprecEleves)));
                                            } else {
                                                String error = "error while retreiving students appreciations";
                                                log.error(error);
                                                handler.handle(new Either.Left<>(error));
                                            }
                                        }
                                    });
                        } else {
                            String error = "error while retreiving classes appreciations";
                            log.error(error);
                            handler.handle(new Either.Left<>(error));
                        }

                    }
                });
    }
}

