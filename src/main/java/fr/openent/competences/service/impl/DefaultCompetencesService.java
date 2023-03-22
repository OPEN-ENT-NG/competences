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
import fr.openent.competences.service.CompetencesService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static fr.openent.competences.Competences.COMPETENCES_SCHEMA;
import static fr.openent.competences.Competences.DELIVERY_OPTIONS;
import static fr.openent.competences.Competences.PERSO_COMPETENCES_TABLE;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultCompetencesService extends SqlCrudService implements CompetencesService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultCompetencesService.class);

    private static final String COMPETENCES_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.COMPETENCES_TABLE;
    private static final String COMPETENCES_DOMAINES_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.REL_COMPETENCES_DOMAINES_TABLE;
    private static final String COMPETENCES_ENSEIGNEMENTS_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.REL_COMPETENCES_ENSEIGNEMENTS_TABLE;
    private static final String COMPETENCES_PERSO_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.PERSO_COMPETENCES_TABLE;
    private static final String COMPETENCES_PERSO_ORDRE_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.PERSO_COMPETENCES_ORDRE_TABLE;
    private static final String COMPETENCES_DEVOIRS_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.COMPETENCES_DEVOIRS;
    private static final String DEVOIRS_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.DEVOIR_TABLE;

    private final EventBus eb;

    public DefaultCompetencesService(EventBus eb) {
        super(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_TABLE);
        this.eb = eb;
    }

    @Override
    public void getCompetencesItem(final String idEtablissement, final String idClasse, final Long idCycle,
                                   final Handler<Either<String, JsonArray>> handler) {

        if (idEtablissement != null) {
            getCompetencesItem(idEtablissement, (Long) null, handler);
        } else {
            JsonObject action = new JsonObject()
                    .put("action", "classe.getEtabClasses")
                    .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(new String[]{idClasse})));

            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {

                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject body = message.body();

                    if ("ok".equals(body.getString("status"))) {
                        final String idEtablissement = ((JsonObject) body.getJsonArray("results").getJsonObject(0)).getString("idStructure");

                        if (idCycle != null) {
                            getCompetencesItem(idEtablissement, idCycle, handler);
                        } else {
                            JsonObject action = new JsonObject()
                                    .put("action", "eleve.getCycle")
                                    .put("idClasse", idClasse);

                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> message) {
                                    JsonObject body = message.body();

                                    if ("ok".equals(body.getString("status")) && body.getJsonArray("results").size() > 0) {
                                        final Number idCycle = ((JsonObject) body.getJsonArray("results").getJsonObject(0)).getInteger("id_cycle");
                                        getCompetencesItem(idEtablissement, idCycle, handler);
                                    } else {
                                        log.error(body.getString("message"));
                                        handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                                    }
                                }
                            }));
                        }
                    } else {
                        log.error(body.getString("message"));
                        handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                    }
                }
            }));
        }
    }

    @Override
    public void getCompetencesItem(String idEtablissement, Number idCycle, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT comp.id, COALESCE(compPerso.nom, comp.nom) AS nom," +
                " comp.id_type, comp.id_parent, comp.id_cycle, compDom.id_domaine," +
                " CASE WHEN comp.id_etablissement IS NULL THEN FALSE ELSE TRUE END AS isManuelle," +
                " CASE WHEN compPerso.masque IS TRUE THEN TRUE ELSE FALSE END AS masque" +
                " FROM " + COMPETENCES_TABLE + " AS comp " +
                " LEFT JOIN " + COMPETENCES_DOMAINES_TABLE + " AS compDom" +
                " ON comp.id = compDom.id_competence " +
                " LEFT JOIN (SELECT nom, id_competence, masque FROM " + COMPETENCES_PERSO_TABLE +
                " WHERE id_etablissement = ?) AS compPerso" +
                " ON comp.id = compPerso.id_competence " +
                " WHERE comp.id_type = 2 ";

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray().add(idEtablissement);
        if (idCycle != null) {
            query += "AND comp.id_cycle = ?";
            values.add(idCycle);
        }

        query += " AND comp.id_etablissement IS NULL OR comp.id_etablissement = ? " +
                " ORDER BY nom ASC";

        values.add(idEtablissement);
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void setDevoirCompetences(Long devoirId, JsonArray values, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray data = new fr.wseduc.webutils.collections.JsonArray();
        query.append("INSERT INTO " + COMPETENCES_SCHEMA + ".competences_devoirs (id_devoir, id_competence) VALUES ");
        for(int i = 0; i < values.size(); i++){
            query.append("(?, ?)");
            data.add(devoirId);
            data.add((Number) values.getLong(i));
            if(i != values.size()-1){
                query.append(",");
            }else{
                query.append(";");
            }
        }

        Sql.getInstance().prepared(query.toString(), data, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void remDevoirCompetences(Long devoirId, JsonArray values, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray data = new fr.wseduc.webutils.collections.JsonArray();
        query.append("DELETE FROM " + COMPETENCES_SCHEMA + ".competences_devoirs WHERE ");
        for(int i = 0; i < values.size(); i++){
            query.append("(id_devoir = ? AND  id_competence = ?)");
            data.add(devoirId);
            data.add((Number) values.getLong(i));
            if(i != values.size()-1){
                query.append(" OR ");
            }else{
                query.append(";");
            }
        }

        Sql.getInstance().prepared(query.toString(), data, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getDevoirCompetences(Long devoirId, Long idCycle, final Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT string_agg(domaines.codification, ', ') as code_domaine," +
                " string_agg( cast (domaines.id as text), ',') as ids_domaine, comp.id as id_competence," +
                " compDevoir.*, COALESCE(compPerso.nom, comp.nom) AS nom, comp.id_type as id_type," +
                " comp.id_parent as id_parent, compDevoir.index as index, comp.id_cycle" +
                " FROM " + COMPETENCES_TABLE + " AS comp" +
                " INNER JOIN " + COMPETENCES_DEVOIRS_TABLE + " AS compDevoir ON (comp.id = compDevoir.id_competence )" +
                " LEFT JOIN " + COMPETENCES_DOMAINES_TABLE + " AS compDom ON (comp.id = compDom.id_competence)" +
                " LEFT JOIN " + COMPETENCES_SCHEMA + ".domaines ON (domaines.id = compDom.id_domaine)" +
                " LEFT JOIN (SELECT nom, id_competence FROM " + COMPETENCES_PERSO_TABLE + " WHERE id_etablissement = " +
                "(SELECT id_etablissement FROM " + DEVOIRS_TABLE + " WHERE id = ?)) AS compPerso" +
                " ON comp.id = compPerso.id_competence" +
                " WHERE compDevoir.id_devoir = ?" +
                " GROUP BY compDevoir.id, COALESCE(compPerso.nom, comp.nom), comp.id_type, comp.id_parent, comp.id" +
                " ORDER BY (compDevoir.index ,compDevoir.id);";

        Sql.getInstance().prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(devoirId).add(devoirId),
                DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }
    public void getDevoirCompetences(JsonArray devoirIds, String idEtablissement, Long idCycle,
                                     final Handler<Either<String, JsonArray>> handler) {
        if (null == devoirIds || devoirIds.isEmpty()){
            handler.handle(new Either.Right<>(new JsonArray()));
            return;
        }

        String query = "SELECT string_agg(domaines.codification, ', ') as code_domaine," +
                " string_agg( cast (domaines.id as text), ',') as ids_domaine, comp.id as id_competence," +
                " compDevoir.*, COALESCE(compPerso.nom, comp.nom) AS nom, comp.id_type as id_type," +
                " comp.id_parent as id_parent, compDevoir.index as index, devoir.id_matiere as id_matiere" +
                " FROM " + COMPETENCES_TABLE + " AS comp" +
                " INNER JOIN " + COMPETENCES_DEVOIRS_TABLE + " AS compDevoir ON (comp.id = compDevoir.id_competence )" +
                " LEFT JOIN " + COMPETENCES_DOMAINES_TABLE + " AS compDom ON (comp.id = compDom.id_competence)" +
                " LEFT JOIN " + COMPETENCES_SCHEMA + ".domaines ON (domaines.id = compDom.id_domaine)" +
                " LEFT JOIN (SELECT nom, id_competence FROM " + COMPETENCES_PERSO_TABLE + " WHERE id_etablissement = ?"+
                " ) AS compPerso" +
                " ON comp.id = compPerso.id_competence" +
                " LEFT JOIN " + DEVOIRS_TABLE + " AS devoir ON (compDevoir.id_devoir = devoir.id)" +
                " WHERE compDevoir.id_devoir IN " + Sql.listPrepared(devoirIds.getList());
        JsonArray values = new JsonArray().add(idEtablissement).addAll(devoirIds);
        if (idCycle != null) {
            query += " AND comp.id_cycle = ?";
            values.add(idCycle);
        }
        query += " GROUP BY compDevoir.id, COALESCE(compPerso.nom, comp.nom), comp.id_type, comp.id_parent, comp.id, devoir.id_matiere" +
                 " ORDER BY (compDevoir.index ,compDevoir.id);";

        Sql.getInstance().prepared(query, values,
                DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getDevoirCompetencesByEnseignement(Long devoirId, Long idCycle, final Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT comp.id as id_competence," +
                " compDevoir.id AS id, compDevoir.id_devoir, COALESCE(compPerso.nom, comp.nom) AS nom, comp.id_type as id_type," +
                " comp.id_parent as id_parent, compDevoir.index as index, compEns.id_enseignement AS id_enseignement " +
                " FROM " + COMPETENCES_TABLE + " AS comp" +
                " INNER JOIN " + COMPETENCES_DEVOIRS_TABLE + " AS compDevoir ON (comp.id = compDevoir.id_competence )" +
                " INNER JOIN " + COMPETENCES_SCHEMA + ".rel_competences_enseignements AS compEns ON (comp.id = compEns.id_competence)" +
                " LEFT JOIN " + COMPETENCES_SCHEMA + ".perso_competences AS compPerso ON comp.id = compPerso.id_competence" +
                " WHERE compDevoir.id_devoir = ?";

        JsonArray values = new JsonArray().add(devoirId);
        if (idCycle != null) {
            query += " AND comp.id_cycle = ?";
            values.add(idCycle);
        }
        query += " ORDER BY (compDevoir.index ,compDevoir.id);";

        Sql.getInstance().prepared(query, values,
                DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

    public void getDevoirCompetencesByEnseignement(JsonArray devoirIds, Long idCycle, String idEtablissement, final Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT comp.id as id_competence," +
                " compDevoir.id AS id, compDevoir.id_devoir, COALESCE(compPerso.nom, comp.nom) AS nom, comp.id_type as id_type," +
                " comp.id_parent as id_parent, compDevoir.index as index, compEns.id_enseignement AS id_enseignement " +
                " FROM " + COMPETENCES_TABLE + " AS comp" +
                " INNER JOIN " + COMPETENCES_DEVOIRS_TABLE + " AS compDevoir ON (comp.id = compDevoir.id_competence )" +
                " INNER JOIN " + COMPETENCES_SCHEMA + ".rel_competences_enseignements AS compEns ON (comp.id = compEns.id_competence)" +
                " LEFT JOIN (SELECT nom, id_competence FROM " + COMPETENCES_SCHEMA +
                ".perso_competences WHERE id_etablissement = ?) AS compPerso ON comp.id = compPerso.id_competence " +
                " WHERE compDevoir.id_devoir IN " + Sql.listPrepared(devoirIds.getList()) ;

        JsonArray values = new JsonArray().add(idEtablissement).addAll(devoirIds);
        if (idCycle != null) {
            query += " AND comp.id_cycle = ?";
            values.add(idCycle);
        }
        query += " ORDER BY (compDevoir.index ,compDevoir.id);";

        Sql.getInstance().prepared(query, values,
                DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getLastCompetencesDevoir(String idEtablissement, String userId, Handler<Either<String, JsonArray>> handler) {

        String query = "WITH lastDevoir AS (SELECT * FROM " + DEVOIRS_TABLE + " AS devoirs" +
                " WHERE devoirs.owner = ? ORDER BY devoirs.created DESC LIMIT 1)" +
                " SELECT compDevoir.*, COALESCE(compPerso.nom, comp.nom) AS nom" +
                " FROM " + COMPETENCES_DEVOIRS_TABLE + " AS compDevoir" +
                " LEFT JOIN " + COMPETENCES_TABLE + " AS comp ON comp.id = compDevoir.id_competence" +
                " LEFT JOIN" +
                "(SELECT nom, id_competence" +
                " FROM " + COMPETENCES_PERSO_TABLE +
                " WHERE id_etablissement = (SELECT id_etablissement FROM lastDevoir)) AS compPerso " +
                " ON comp.id = compPerso.id_competence" +
                " WHERE compDevoir.id_devoir = (SELECT id FROM lastDevoir)";

        Sql.getInstance().prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(userId), SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCompetencesByLevel(final String filter, final String valueToFilter, final String idClasse, final String idCycle, final Handler<Either<String, JsonArray>> handler) {
        final JsonObject action = new JsonObject()
                .put("action", "classe.getEtabClasses")
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(new String[]{idClasse})));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {

                    JsonArray results =  body.getJsonArray("results");
                    String idEtablissement = null;
                    if (results.size() > 0 ){
                        idEtablissement = ((JsonObject)results.getJsonObject(0)).getString("idStructure");
                    }
                    getCompetencesByLevel(idEtablissement, filter, valueToFilter, idClasse, idCycle, handler);
                } else {
                    log.error(body.getString("message"));
                    handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                }
            }
        }));
    }

    @Override
    public void getCompetencesByLevel(final String idEtablissement, final String filter, final String valueToFilter, final String idClasse,
                                      final String idCycle, final Handler<Either<String, JsonArray>> handler) {

        if (idEtablissement == null) {
            getCompetencesByLevel(filter, valueToFilter, idClasse, idCycle, handler);
        } else {

            JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

            String filterQuery = "";
            if(Objects.equals(filter, "id_type") && valueToFilter != null){
                filterQuery = "comp." + filter + " = ? AND";
            }

            String query = "SELECT DISTINCT string_agg(domaines.codification, ', ') as code_domaine," +
                    " string_agg( cast (domaines.id as text), ',') as ids_domaine, comp.id," +
                    " COALESCE(compPerso.nom, comp.nom) AS nom, comp.id_parent, comp.id_type," +
                    " compEns.id_enseignement, comp.id_cycle, perso_ordre.index as index,  " +
                    " comp.id_etablissement IS NOT NULL AS isManuelle," +
                    " compPerso.nom IS NOT NULL  AS hasNamePerso," +
                    " CASE WHEN compPerso.masque IS TRUE THEN TRUE ELSE FALSE END AS masque" +
                    " FROM " + COMPETENCES_TABLE + " AS comp" +
                    " INNER JOIN " + COMPETENCES_ENSEIGNEMENTS_TABLE + " AS compEns" +
                    " ON (comp.id = compEns.id_competence) " +
                    " LEFT OUTER JOIN " + COMPETENCES_SCHEMA + ".perso_order_item_enseignement AS perso_ordre" +
                    " ON ( perso_ordre.id_competence = compEns.id_competence AND " +
                    " perso_ordre.id_enseignement = compEns.id_enseignement AND perso_ordre.id_etablissement = ?) ";
            params.add(idEtablissement);
            if (idClasse != null && idCycle == null) {
                query += "INNER JOIN " + COMPETENCES_SCHEMA + ".rel_groupe_cycle" +
                        " ON (rel_groupe_cycle.id_cycle = comp.id_cycle) ";
            }

            query += " LEFT JOIN " + COMPETENCES_DOMAINES_TABLE + " AS compDom" +
                    " ON (comp.id = compDom.id_competence)" +
                    " LEFT JOIN " + COMPETENCES_SCHEMA + ".domaines" +
                    " ON (domaines.id = compDom.id_domaine) " +
                    " LEFT JOIN (SELECT nom, id_competence, masque  FROM " + COMPETENCES_PERSO_TABLE +
                    "  WHERE id_etablissement = ? ) AS compPerso" +
                    " ON comp.id = compPerso.id_competence " +
                    " WHERE " + filterQuery + " (comp.id_etablissement = ? OR comp.id_etablissement IS NULL ) ";
            params.add(idEtablissement);
            if(!filterQuery.equals("")){
                params.add(valueToFilter);
            }
            params.add(idEtablissement);
            if(idCycle != null){
                query += "AND comp.id_cycle = ?";
                params.add(idCycle);
            }

            if (idClasse != null && idCycle == null) {
                query += " AND rel_groupe_cycle.id_groupe = ?";
                params.add(idClasse);
            }

            query += " GROUP BY comp.id, COALESCE(compPerso.nom, comp.nom), comp.id_parent, comp.id_type," +
                    " compEns.id_enseignement, comp.id_cycle, compPerso.masque, perso_ordre.index, compPerso.nom  " +
                    " ORDER BY index, nom ASC";

            sql.prepared(query, params, SqlResult.validResultHandler(handler));
        }
    }

    @Override
    public void getCompetencesDomaines(String idClasse, final Long[] idDomaines, final Handler<Either<String, JsonArray>> handler) {

        final JsonObject action = new JsonObject()
                .put("action", "classe.getEtabClasses")
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(new String[]{idClasse})));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    String idEtablissement = ((JsonObject) body.getJsonArray("results").getJsonObject(0)).getString("idStructure");
                    JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

                    String query = "SELECT * FROM " + COMPETENCES_DOMAINES_TABLE + " AS compEns" +
                            " WHERE id_domaine IN " + Sql.listPrepared(idDomaines) + " AND id_competence IN (" +
                            "SELECT id FROM " + COMPETENCES_TABLE + " WHERE id_etablissement IS NULL OR id_etablissement = ?)";

                    for(Long l : idDomaines) {
                        params.add(l);
                    }

                    Sql.getInstance().prepared(query, params.add(idEtablissement), SqlResult.validResultHandler(handler));
                } else {
                    log.error(body.getString("message"));
                    handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                }
            }
        }));
    }

    @Override
    public void create(JsonObject competence, Handler<Either<String, JsonObject>> handler) {

        String query = "WITH new_competence AS (" +
                " INSERT INTO " + COMPETENCES_TABLE + " (nom, id_parent, id_type, id_cycle, id_etablissement)" +
                " VALUES (?, ?, ?, ?, ?)" +
                " RETURNING id)," +
                " ens_insert AS (" +
                " INSERT INTO " + COMPETENCES_ENSEIGNEMENTS_TABLE + " (id_competence, id_enseignement)" +
                " SELECT id, ? FROM new_competence )," +
                " dom_insert AS (" +
                " INSERT INTO " + COMPETENCES_DOMAINES_TABLE + " (id_competence, id_domaine)" +
                " SELECT id, unnest(" + Sql.arrayPrepared(competence.getJsonArray("ids_domaine").getList().toArray()) +
                ") FROM new_competence) SELECT id FROM new_competence;";

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray().add(competence.getString("nom"))
                .add(competence.getInteger("id_parent"))
                .add(competence.getInteger("id_type"))
                .add(competence.getInteger("id_cycle"))
                .add(competence.getString("id_etablissement"))
                .add(competence.getInteger("id_enseignement"));

        for(Object n : competence.getJsonArray("ids_domaine")) {
            values.add((Number) n);
        }

        sql.prepared(query, values, validUniqueResultHandler(handler));
    }

    private void isCompManuelle(Number id, final Handler<Either<String, Boolean>> handler) {
        String query = "SELECT CASE WHEN id_etablissement IS NULL THEN FALSE ELSE TRUE END AS isManuelle"
                + " FROM " + COMPETENCES_TABLE + " WHERE id = ?";

        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(id), validUniqueResultHandler(
                new Handler<Either<String, JsonObject>>() {
                    @Override
                    public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                        if(stringJsonObjectEither.isRight()) {
                            handler.handle(new Either.Right<String, Boolean>(
                                    stringJsonObjectEither.right().getValue().getBoolean("ismanuelle")));
                        } else {
                            handler.handle(new Either.Left<String, Boolean>(stringJsonObjectEither.left().getValue()));
                        }
                    }
                }));
    }

    private void updateDomain(Number idComp, String idEtablissement, Number idDomaine,
                              Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT notes.function_updateDomaineCompetence(?, ?, ?);";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray().add(idComp).add(idEtablissement).add(idDomaine);

        sql.prepared(query, values, validUniqueResultHandler(handler));
    }

    private void updateMasqueComp(Number id, String idEtablissement, Boolean masque, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT notes.function_masqueCompetence(?, ?, ?);";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray().add(id).add(idEtablissement).add(masque);

        sql.prepared(query, values, validUniqueResultHandler(handler));
    }

    private void changeNameComp(final Number idComp, final String idEtablissement, final String name, final Handler<Either<String, JsonObject>> handler) {
        isCompManuelle(idComp, new Handler<Either<String, Boolean>>() {
            @Override
            public void handle(Either<String, Boolean> stringBooleanEither) {
                if(stringBooleanEither.isLeft()) {
                    handler.handle(new Either.Left<String, JsonObject>(stringBooleanEither.left().getValue()));
                } else if (!stringBooleanEither.right().getValue()) {
                    String query = "INSERT INTO " + COMPETENCES_PERSO_TABLE + " (id_competence, id_etablissement, nom)" +
                            " VALUES (?, ?, ?)" +
                            " ON CONFLICT (id_competence, id_etablissement) DO UPDATE" +
                            " SET nom = EXCLUDED.nom RETURNING *";
                    JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

                    values.add(idComp)
                            .add(idEtablissement)
                            .add(name);

                    sql.prepared(query, values, validUniqueResultHandler(handler));

                } else {
                    String query = "UPDATE " + COMPETENCES_TABLE + " SET nom = ?" +
                            " WHERE id = ? AND id_etablissement = ? RETURNING *";
                    JsonArray values = new fr.wseduc.webutils.collections.JsonArray().add(name).add(idComp).add(idEtablissement);
                    sql.prepared(query, values, validUniqueResultHandler(handler));
                }

            }
        });
    }

    private void changeIndexComp(final Number idComp, final Number idEnseignement,final String idEtablissement,
                                 final JsonArray index , final Handler<Either<String, JsonObject>> handler) {

        StringBuilder query = new StringBuilder("INSERT INTO " + COMPETENCES_PERSO_ORDRE_TABLE +
                " (id_competence, id_etablissement, id_enseignement, index)  VALUES ") ;
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        for(int i = 0; i < index.size(); i++){
            query.append("(?, ?, ? ,?) ");
            JsonObject rel_comp_ens = (JsonObject) index.getJsonObject(i);
            values.add(rel_comp_ens.getLong("id"))
                    .add(rel_comp_ens.getString("id_etablissement"))
                    .add(rel_comp_ens.getLong("id_enseignement"))
                    .add(rel_comp_ens.getLong("index"));
            if(i != index.size()-1){
                query.append(",");
            }else{
                query.append(" ON CONFLICT (id_competence, id_etablissement, id_enseignement) DO UPDATE" +
                        " SET index = EXCLUDED.index RETURNING *");
            }
        }

        sql.prepared(query.toString(), values, SqlResult.validRowsResultHandler(handler));

    }

    @Override
    public void update(Number idComp, String idEtab, String fieldToUpdate, Object valueToUpdate,
                       Handler<Either<String, JsonObject>> handler, Number idEns) {
        switch (fieldToUpdate) {
            case "masque":
                updateMasqueComp(idComp, idEtab, (Boolean) valueToUpdate, handler);
                break;
            case "id_domaine":
                updateDomain(idComp, idEtab, (Number) valueToUpdate, handler);
                break;
            case "nom":
                changeNameComp(idComp, idEtab, (String) valueToUpdate, handler);
                break;
            case "index":
                changeIndexComp(idComp, idEns, idEtab, (JsonArray) valueToUpdate, handler);
                break;
            default:
                break;
        }
    }

    @Override
    public void delete(final Number id, final String idEtablissement, final Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT notes.function_deleteCompetence(?, ?);";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray().add(id).add(idEtablissement);

        sql.prepared(query, values, validUniqueResultHandler(handler));
    }

    @Override
    public void deleteCustom(String idEtablissement, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();

        // SUPPRESSION DE COMPETENCES MANUELLES NON UTILISEES
        StringBuilder query = new StringBuilder().append(" DELETE FROM " + COMPETENCES_TABLE)
                .append(" WHERE id_etablissement = ? AND id NOT IN (SELECT  DISTINCT id_competence FROM ")
                .append(COMPETENCES_DEVOIRS_TABLE + " WHERE id_competence IS NOT NULL )")
                .append(" AND id_etablissement IS NOT NULL ");
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(idEtablissement);
        statements.add(new JsonObject()
                .put("statement", query.toString())
                .put("values", params)
                .put("action", "prepared"));

        // SUPPRESSION D'INFO PERSONNALISATION
        StringBuilder queryPerso = new StringBuilder().append("DELETE FROM " + COMPETENCES_PERSO_TABLE)
                .append(" WHERE id_etablissement = ? ");
        JsonArray paramsPerso = new fr.wseduc.webutils.collections.JsonArray();

        paramsPerso.add(idEtablissement);
        statements.add(new JsonObject()
                .put("statement", queryPerso.toString())
                .put("values", paramsPerso)
                .put("action", "prepared"));

        // CREER PERSO DE MASQUAGE
        StringBuilder queryMask = new StringBuilder()
                .append("INSERT INTO "+ COMPETENCES_PERSO_TABLE + " (id_competence, masque, id_etablissement) ( ")
                .append(" SELECT DISTINCT competences_devoirs.id_competence, true, id_etablissement ")
                .append(" FROM " + COMPETENCES_DEVOIRS_TABLE)
                .append(" INNER JOIN " + COMPETENCES_TABLE)
                .append(" ON competences_devoirs.id_competence = competences.id AND id_etablissement = ? )")
                .append(" ON CONFLICT (id_competence, id_etablissement) DO UPDATE SET masque = true ");
        JsonArray paramsMask = new fr.wseduc.webutils.collections.JsonArray();

        paramsMask.add(idEtablissement);
        statements.add(new JsonObject()
                .put("statement", queryMask.toString())
                .put("values", paramsMask)
                .put("action", "prepared"));

        // SUPPRESSION D'INFO PERSONNALISATION D'ORDRE
        StringBuilder queryPersoOrdre = new StringBuilder().append("DELETE FROM " + COMPETENCES_PERSO_ORDRE_TABLE)
                .append(" WHERE id_etablissement = ? ");
        JsonArray paramsPersoOrdre = new fr.wseduc.webutils.collections.JsonArray();

        paramsPersoOrdre.add(idEtablissement);
        statements.add(new JsonObject()
                .put("statement", queryPersoOrdre.toString())
                .put("values", paramsPersoOrdre)
                .put("action", "prepared"));

        Sql.getInstance().transaction(statements, SqlResult.validRowsResultHandler(handler));
    }
}
