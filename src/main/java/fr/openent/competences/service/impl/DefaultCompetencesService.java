/*
 * Copyright (c) Région Hauts-de-France, Département 77, CGI, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.CompetencesService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.List;
import java.util.Map;

import static fr.openent.competences.Competences.COMPETENCES_SCHEMA;
import static org.entcore.common.sql.SqlResult.validResultHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultCompetencesService extends SqlCrudService implements CompetencesService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultCompetencesService.class);

    private EventBus eb;

    private static final String COMPETENCES_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.COMPETENCES_TABLE;
    private static final String COMPETENCES_DOMAINES_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.REL_COMPETENCES_DOMAINES_TABLE;
    private static final String COMPETENCES_ENSEIGNEMENTS_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.REL_COMPETENCES_ENSEIGNEMENTS_TABLE;
    private static final String COMPETENCES_PERSO_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.PERSO_COMPETENCES_TABLE;
    private static final String COMPETENCES_DEVOIRS_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.COMPETENCES_DEVOIRS;
    private static final String DEVOIRS_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.DEVOIR_TABLE;

    public DefaultCompetencesService(EventBus eb) {
        super(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_TABLE);
        this.eb = eb;
    }

    @Override
    public void getCompetencesItem(final String idClasse, final Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .putString("action", "classe.getEtabClasses")
                .putArray("idClasses", new JsonArray(new String[]{idClasse}));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    final String idEtablissement = body.getObject("result").getString("id_etablissement");

                    JsonObject action = new JsonObject()
                            .putString("action", "eleve.getCycle")
                            .putString("idClasse", idClasse);

                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            JsonObject body = message.body();

                            if ("ok".equals(body.getString("status"))) {
                                final Number cycle = body.getObject("result").getNumber("id_cycle");

                                String query = "SELECT comp.id, COALESCE(compPerso.nom, comp.nom) AS nom," +
                                        " comp.description, comp.id_type, comp.id_parent, competences.id_cycle," +
                                        " compDom.id_domaine" +
                                        " FROM " + COMPETENCES_TABLE + " AS comp " +
                                        " LEFT JOIN " + COMPETENCES_DOMAINES_TABLE + " AS compDom" +
                                        " ON comp.id = compDom.id_competence " +
                                        " LEFT JOIN (SELECT nom FROM " + COMPETENCES_PERSO_TABLE +
                                        " WHERE id_etablissement = ? AND masque = false) AS compPerso" +
                                        " ON comp.id = compPerso.id_competence " +
                                        " WHERE competences.id_type = 2 ";

                                if (cycle != null) {
                                    query += "AND comp.id_cycle = ?";
                                }
                                query += " AND comp.id_etablissement IS NULL OR comp.id_etablissement = ? " +
                                        " ORDER BY comp.id ASC";

                                JsonArray values = new JsonArray()
                                        .addString(idEtablissement)
                                        .addNumber(cycle)
                                        .addString(idEtablissement);
                                Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));

                            } else {
                                log.error(body.getString("message"));
                                handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                            }
                        }
                    });
                } else {
                    log.error(body.getString("message"));
                    handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                }
            }
        });
    }

    @Override
    public void getCompetences(List<Map.Entry<String, Object>> params, Handler<Either<String, JsonArray>> handler) {
        String idEtablissement = null;
        for (Map.Entry<String, Object> param : params) {
            if ("id_etablissement".equals(param.getKey())) {
                idEtablissement = (String) param.getValue();
            }
        }
        getCompetences(idEtablissement, params, handler);
    }

    @Override
    public void getCompetences(String idEtablissement, List<Map.Entry<String, Object>> params, Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT id, nom, id_type, string_agg(cast(query.id_domaine as text), ',') AS ids_domaine," +
                " query.id_enseignement, masque, isManuelle, id_parent, id_cycle, " +
                " FROM (" +
                "SELECT comp.id, COALESCE(compPerso.nom, comp.nom) AS nom, comp.id_type, comp.id_parent, " +
                "comp.id_cycle, compEns.id_enseignement, compDom.id_domaine, compPerso.masque," +
                " CASE WHEN comp.id_etablissement IS NULL THEN TRUE ELSE FALSE END AS isManuelle " +
                "FROM " + COMPETENCES_TABLE + " AS comp " +
                "LEFT JOIN " + COMPETENCES_ENSEIGNEMENTS_TABLE + " AS compEns ON comp.id = compEns.id_competence " +
                "LEFT JOIN " + COMPETENCES_DOMAINES_TABLE + " AS compDom ON comp.id = compDom.id_competence " +
                "LEFT JOIN " + COMPETENCES_PERSO_TABLE + " AS compPerso ON comp.id = compPerso.id_competence " +
                "WHERE (comp.id_etablissement IS NULL OR comp.id_etablissement = ?)";

        JsonArray values = new JsonArray().addString(idEtablissement);

        if (!params.isEmpty()) {
            for (Map.Entry<String, Object> param : params) {
                query += " AND ? = ?";
                values.addString(param.getKey()).add(param.getValue());
            }
        }

        query += " GROUP BY id, nom, id_parent, id_type, id_enseignement, id_cycle) AS query ORDER BY nom ASC";

        sql.prepared(query, values, validResultHandler(handler));
    }

//    private boolean contains(String key, List<Map.Entry<String, String>> list) {
//        for(Map.Entry<String, String> prop : list) {
//            if(key.equals(prop.getKey())) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private String get(String key, List<Map.Entry<String, String>> list) {
//        for (Map.Entry<String, String> prop : list) {
//            if (key.equals(prop.getKey())) {
//                return prop.getValue();
//            }
//        }
//        return null;
//    }

    @Override
    public void setDevoirCompetences(Long devoirId, JsonArray values, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray data = new JsonArray();
        query.append("INSERT INTO " + COMPETENCES_SCHEMA + ".competences_devoirs (id_devoir, id_competence) VALUES ");
        for (int i = 0; i < values.size(); i++) {
            query.append("(?, ?)");
            data.addNumber(devoirId);
            data.addNumber((Number) values.get(i));
            if (i != values.size() - 1) {
                query.append(",");
            } else {
                query.append(";");
            }
        }

        Sql.getInstance().prepared(query.toString(), data, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void remDevoirCompetences(Long devoirId, JsonArray values, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray data = new JsonArray();
        query.append("DELETE FROM " + COMPETENCES_SCHEMA + ".competences_devoirs WHERE ");
        for (int i = 0; i < values.size(); i++) {
            query.append("(id_devoir = ? AND  id_competence = ?)");
            data.addNumber(devoirId);
            data.addNumber((Number) values.get(i));
            if (i != values.size() - 1) {
                query.append(" OR ");
            } else {
                query.append(";");
            }
        }

        Sql.getInstance().prepared(query.toString(), data, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getDevoirCompetences(Long devoirId, final Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT string_agg(domaines.codification, ', ') as code_domaine," +
                " string_agg( cast (domaines.id as text), ',') as ids_domaine, comp.id as id_competence," +
                " compDevoir.*, COALESCE(compPerso.nom, comp.nom) AS nom, comp.id_type as id_type," +
                " comp.id_parent as id_parent, compDevoir.index as index" +
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

        Sql.getInstance().prepared(query, new JsonArray().addNumber(devoirId).addNumber(devoirId),
                SqlResult.validResultHandler(handler));
    }

    @Override
    public void getLastCompetencesDevoir(String idEtablissement, String userId, Handler<Either<String, JsonArray>> handler) {

        String query = "WITH lastDevoir AS (SELECT * FROM " + DEVOIRS_TABLE + " AS devoirs" +
                " WHERE devoirs.owner = ? ORDER BY devoirs.created DESC LIMIT 1)" +
                " SELECT compDevoir.*, COALESCE(compPerso.nom, comp.nom) AS nom" +
                " FROM " + COMPETENCES_DEVOIRS_TABLE + " AS compDevoir" +
                " LEFT JOIN " + COMPETENCES_TABLE + " AS comp ON comp.id = compDevoir.id_competence" +
                " LEFT JOIN (SELECT nom FROM " + COMPETENCES_PERSO_TABLE +
                " WHERE id_etablissement = lastDevoir.id_etablissement ) AS compPerso" +
                " ON comp.id = compPerso.id_competence" +
                " WHERE competences_devoirs.id_devoir = lastDevoir.id ";

        Sql.getInstance().prepared(query, new JsonArray().addString(userId), SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCompetencesByLevel(final String filter, final String idClasse, final Handler<Either<String, JsonArray>> handler) {
        final JsonObject action = new JsonObject()
                .putString("action", "classe.getEtabClasses")
                .putArray("idClasses", new JsonArray(new String[]{idClasse}));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    String idEtablissement = body.getObject("result").getString("id_etablissement");

                    JsonArray params = new JsonArray();

                    String query = "SELECT DISTINCT string_agg(domaines.codification, ', ') as code_domaine," +
                            " string_agg( cast (domaines.id as text), ',') as ids_domaine, comp.id," +
                            " COALESCE(compPerso.nom, comp.nom) AS nom, comp.id_parent, comp.id_type," +
                            " compDom.id_enseignement, comp.id_cycle " +

                            "FROM " + COMPETENCES_TABLE + " AS comp " +
                            "INNER JOIN " + COMPETENCES_ENSEIGNEMENTS_TABLE + " AS compEns" +
                            " ON (competences.id = rel_competences_enseignements.id_competence) ";

                    if (idClasse != null) {
                        query += "INNER JOIN " + COMPETENCES_SCHEMA + ".rel_groupe_cycle" +
                                " ON (rel_groupe_cycle.id_cycle = competences.id_cycle) ";
                    }

                    query += " LEFT JOIN " + COMPETENCES_DOMAINES_TABLE + " AS compDom" +
                            " ON (comp.id = compDom.id_competence)" +
                            " LEFT JOIN " + COMPETENCES_SCHEMA + ".domaines" +
                            " ON (domaines.id = compDom.id_domaine) " +
                            " LEFT JOIN (SELECT nom FROM " + COMPETENCES_PERSO_TABLE +
                            " WHERE id_etablissement = ?) AS compPerso" +
                            " ON comp.id = compPerso.id_competence" +
                            "WHERE comp." + filter;

                    params.addString(idEtablissement);

                    if (idClasse != null) {
                        query += " AND rel_groupe_cycle.id_groupe = ?";
                        params.addString(idClasse);
                    }

                    query += " GROUP BY comp.id, nom, comp.id_parent, comp.id_type, compEns.id_enseignement, comp.id_cycle" +
                            " ORDER BY nom ASC";

                    Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
                } else {
                    log.error(body.getString("message"));
                    handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                }
            }
        });
    }

    @Override
    public void getCompetencesDomaines(String idClasse, final Long[] idDomaines, final Handler<Either<String, JsonArray>> handler) {

        final JsonObject action = new JsonObject()
                .putString("action", "classe.getEtabClasses")
                .putArray("idClasses", new JsonArray(new String[]{idClasse}));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    String idEtablissement = body.getObject("result").getString("id_etablissement");
                    JsonArray params = new JsonArray();

                    String query = "SELECT * FROM " + COMPETENCES_DOMAINES_TABLE + " AS compEns" +
                            " WHERE id_domaine IN " + Sql.listPrepared(idDomaines) + " AND id_competence IN (" +
                            "SELECT id FROM " + COMPETENCES_TABLE + " WHERE id_etablissement IS NULL OR id_etablissement = ?)";

                    for (Long l : idDomaines) {
                        params.addNumber(l);
                    }

                    Sql.getInstance().prepared(query, params.addString(idEtablissement), SqlResult.validResultHandler(handler));
                } else {
                    log.error(body.getString("message"));
                    handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                }
            }
        });
    }

    @Override
    public void create(JsonObject competence, Handler<Either<String, JsonObject>> handler) {

        String query = "WITH new_competence AS(" +
                "INSERT INTO " + COMPETENCES_SCHEMA + ".competences(nom, id_parent, id_type, id_cycle) " +
                "VALUES (?, ?, ?, ?) " +
                "RETURNING id" +
                ") INSERT INTO rel_competences_enseignements VALUES (new_competence.id, ?)";

        JsonArray values = new JsonArray().addString(competence.getString("nom"))
                .addNumber(competence.getInteger("id_parent"))
                .addNumber(competence.getInteger("id_type"))
                .addNumber(competence.getInteger("id_cycle"))
                .addNumber(competence.getInteger("id_enseignement"));

        sql.prepared(query, values, validUniqueResultHandler(handler));
    }

    @Override
    public void isCompManuelle(Number id, final Handler<Either<String, Boolean>> handler) {
        String query = "SELECT CASE WHEN id_etablissement IS NULL THEN TRUE ELSE FALSE END AS isManuelle"
                + "FROM " + COMPETENCES_TABLE + " WHERE id = ?";

        sql.prepared(query, new JsonArray().addNumber(id), validUniqueResultHandler(
                new Handler<Either<String, JsonObject>>() {
                    @Override
                    public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                        if (stringJsonObjectEither.isRight()) {
                            handler.handle(new Either.Right<String, Boolean>(
                                    stringJsonObjectEither.right().getValue().getBoolean("isManuelle")));
                        } else {
                            handler.handle(new Either.Left<String, Boolean>(stringJsonObjectEither.left().getValue()));
                        }
                    }
                }));
    }

    private void updateDomain(final Number id, final Number[] idDomains, final Handler<Either<String, JsonObject>> handler) {

        JsonArray transaction = new JsonArray();

        String queryDelete = "DELETE FROM " + COMPETENCES_DOMAINES_TABLE
                + " WHERE id_competence = ? AND id_domaine NOT IN " + Sql.listPrepared(idDomains);
        String queryCreate = "INSERT INTO " + COMPETENCES_DOMAINES_TABLE
                + " VALUES (?, unnest( " + Sql.arrayPrepared(idDomains) + "))"
                + " ON CONFLICT DO NOTHING RETURNING id_competence, array_agg(id_domaine) as ids_domaine";
        JsonArray values = new JsonArray().addNumber(id);


        for (Number n : idDomains) {
            values.addNumber(n);
        }

        transaction.add(new JsonObject()
                .putString("statement", queryCreate)
                .putArray("values", values)
                .putString("action", "prepared"));

        transaction.add(new JsonObject()
                .putString("statement", queryDelete)
                .putArray("values", values)
                .putString("action", "prepared"));

        sql.transaction(transaction, validUniqueResultHandler(handler));
    }

    private void hideComp(final Number id, final String idEtablissement, final Boolean masque,
                          final Handler<Either<String, JsonObject>> handler) {
        lastOfDomain(id, idEtablissement, new Handler<Either<String, Boolean>>() {
            @Override
            public void handle(Either<String, Boolean> stringBooleanEither) {
                if (stringBooleanEither.isLeft()) {
                    handler.handle(new Either.Left<String, JsonObject>(stringBooleanEither.left().getValue()));
                } else if (stringBooleanEither.right().getValue()) {
                    handler.handle(new Either.Right<String, JsonObject>(
                            new JsonObject().putString("error", "Competence is the last of its domain")));
                } else {
                    sql.prepared("UPDATE " + COMPETENCES_PERSO_TABLE
                                    + " SET masque = ? WHERE id_competence = ? AND id_etablissement = ?"
                                    + " RETURNING id_competence, masque",
                            new JsonArray().addBoolean(masque).addNumber(id).addString(idEtablissement),
                            validUniqueResultHandler(handler));
                }
            }
        });
    }

    private void changeNameComp(Number id, String idEtablissment, String name, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + COMPETENCES_PERSO_TABLE + " SET nom = ? WHERE id_competence = ? "
                + "AND id_etablissement = ? RETURNING id_competence, nom";
        JsonArray values = new JsonArray().addString(name).addNumber(id).addString(idEtablissment);
        sql.prepared(query, values, validUniqueResultHandler(handler));
    }

    private void lastOfDomain(Number id, String idEtablissement, final Handler<Either<String, Boolean>> handler) {
        String query = "SELECT count(id), id_domaine FROM " + COMPETENCES_TABLE + " RIGHT JOIN"
                + " (SELECT id_competence, comp1.id_domaine FROM " + COMPETENCES_DOMAINES_TABLE + " AS compDom1 INNER JOIN"
                + " (SELECT id_domaine FROM " + COMPETENCES_DOMAINES_TABLE + " WHERE id_competence = ?) AS compDom2"
                + " ON compDom1.id_domaine = compDom2.id_domaine) AS compSameDomain"
                + " ON competences.id = compSameDomain.id_competence"
                + " WHERE id_etablissement IS NULL OR id_etablissement = ? GROUP BY id_domaine";

        JsonArray values = new JsonArray().addNumber(id).addString(idEtablissement);

        sql.prepared(query, values, validResultHandler(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isLeft()) {
                    handler.handle(new Either.Left<String, Boolean>(stringJsonArrayEither.left().getValue()));
                } else if (stringJsonArrayEither.right().getValue().size() == 0) {
                    handler.handle(new Either.Right<String, Boolean>(false));
                } else {
                    for (Object o : stringJsonArrayEither.right().getValue()) {
                        JsonObject domaine = (JsonObject) o;
                        if (domaine.getNumber("count") == 1) {
                            handler.handle(new Either.Right<String, Boolean>(true));
                            return;
                        }
                    }
                    handler.handle(new Either.Right<String, Boolean>(false));
                }
            }
        }));
    }

    @Override
    public void update(final JsonObject competence, final Handler<Either<String, JsonObject>> handler) {
        final Number idComp = competence.getNumber("id");
        final String idEtab = competence.getString("id_etablissement");

        initPerso(competence, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                if (stringJsonObjectEither.isRight()) {
                    for (Map.Entry<String, Object> property : competence.toMap().entrySet()) {
                        if ("masque".equals(property.getKey())) {
                            hideComp(idComp, idEtab, (Boolean) property.getValue(), handler);
                        } else if ("ids_domaine".equals(property.getKey())) {
                            updateDomain(idComp, (Number[]) property.getValue(), handler);
                        } else if ("nom".equals(property.getKey())) {
                            changeNameComp(idComp, idEtab, (String) property.getValue(), handler);
                        }
                    }
                } else {
                    handler.handle(stringJsonObjectEither.left());
                }
            }
        });
    }

    private void getPerso(JsonObject competence, final Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + COMPETENCES_PERSO_TABLE + " WHERE id_competence = ? AND id_etablissement = ?";
        JsonArray values = new JsonArray().addNumber(competence.getNumber("id_competence"))
                .addString(competence.getString("id_etablissement"));

        sql.prepared(query, values, validUniqueResultHandler(handler));
    }

    private void initPerso(final JsonObject competence, final Handler<Either<String, JsonObject>> handler) {

        getPerso(competence, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                if (stringJsonObjectEither.isLeft()) {
                    handler.handle(stringJsonObjectEither.left());
                } else if (stringJsonObjectEither.right().getValue().size() == 0) {
                    String query = "INSERT INTO " + COMPETENCES_PERSO_TABLE + " "
                            + Sql.listPrepared(competence.toMap().keySet().toArray())
                            + "VALUES " + Sql.listPrepared(competence.toMap().keySet().toArray());
                    JsonArray values = new JsonArray();
                    for (String propertyName : competence.toMap().keySet()) {
                        values.addString(propertyName);
                    }
                    for (Object propertyValue : competence.toMap().values()) {
                        values.add(propertyValue);
                    }

                    sql.prepared(query, values, validUniqueResultHandler(handler));
                } else {
                    handler.handle(stringJsonObjectEither.right());
                }
            }
        });

    }

    @Override
    public void delete(final Number id, final String idEtablissement, final Handler<Either<String, JsonObject>> handler) {
        isCompManuelle(id, new Handler<Either<String, Boolean>>() {
            @Override
            public void handle(final Either<String, Boolean> isManuelle) {
                if (isManuelle.isRight()) {
                    hasCompetenceDevoir(id, new Handler<Either<String, Boolean>>() {
                        @Override
                        public void handle(Either<String, Boolean> hasDevoir) {
                            if (hasDevoir.isRight()) {
                                if (hasDevoir.right().getValue() || !isManuelle.right().getValue()) {
                                    hideComp(id, idEtablissement, true, handler);
                                } else {
                                    delete(String.valueOf(id), handler);
                                }
                            } else {
                                handler.handle(new Either.Left<String, JsonObject>(hasDevoir.left().getValue()));
                            }
                        }
                    });
                } else {
                    handler.handle(new Either.Left<String, JsonObject>(isManuelle.left().getValue()));
                }
            }
        });
    }


    @Override
    public void deleteCustom(String idEtablissement, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();

        // SUPPRESSION DE COMPETENCES MANUELLES
        StringBuilder query = new StringBuilder().append(" DELETE FROM " + Competences.COMPETENCES_SCHEMA)
                .append(".competences WHERE id_etablissement = ? ");
        JsonArray params = new JsonArray();
        params.addString(idEtablissement);
        statements.add(new JsonObject()
                .putString("statement", query.toString())
                .putArray("values", params)
                .putString("action", "prepared"));

        // SSUPPRESSION D'INFO PERSO
        StringBuilder query_perso = new StringBuilder().append("DELETE FROM " + Competences.COMPETENCES_SCHEMA)
                .append(".perso_competences WHERE id_etablissement = ? ");
        JsonArray params_perso = new JsonArray();

        params_perso.addString(idEtablissement);
        statements.add(new JsonObject()
                .putString("statement", query_perso.toString())
                .putArray("values", params_perso)
                .putString("action", "prepared"));
        Sql.getInstance().transaction(statements, SqlResult.validRowsResultHandler(handler));
    }


    private void hasCompetenceDevoir(Number id, final Handler<Either<String, Boolean>> handler) {
        String query = "SELECT devoir.id FROM notes.devoirs " +
                "LEFT JOIN rel_comp_devoir ON rel_comp_devoir.id_devoir = devoir.id " +
                "WHERE rel_comp_devoir.id_com = ?";

        sql.prepared(query, new JsonArray().addNumber(id), validResultHandler(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isRight() && stringJsonArrayEither.right().getValue().size() == 0) {
                    handler.handle(new Either.Right<String, Boolean>(false));
                } else if (stringJsonArrayEither.isRight() && stringJsonArrayEither.right().getValue().size() != 0) {
                    handler.handle(new Either.Right<String, Boolean>(true));
                } else {
                    handler.handle(new Either.Left<String, Boolean>(stringJsonArrayEither.left().getValue()));
                }
            }
        }));
    }

}