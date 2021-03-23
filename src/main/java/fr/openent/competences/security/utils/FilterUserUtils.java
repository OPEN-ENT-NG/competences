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

package fr.openent.competences.security.utils;

import fr.openent.competences.Competences;
import fr.openent.competences.bean.Eleve;
import fr.openent.competences.service.impl.CompetenceRepositoryEvents;
import fr.wseduc.webutils.Either;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Array;
import java.util.*;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

/**
 * Created by ledunoiss on 20/10/2016.
 */
public class FilterUserUtils {
    private UserInfos user;
    private EventBus eb;
    private static final Logger log = LoggerFactory.getLogger(FilterUserUtils.class);

    public FilterUserUtils(UserInfos user, EventBus eb) {
        this.user = user;
        this.eb = eb;
    }

    public boolean validateUser(String idUser) {
        return user.getUserId().equals(idUser);
    }

    public boolean validateStructure(String idEtablissement) {
        return user.getStructures().contains(idEtablissement);
    }

    public boolean validateClasse(String idClasse) {
        return user.getClasses().contains(idClasse) || user.getGroupsIds().contains(idClasse);
    }

    public void validateElement(List<String> idsElements, String idClasse, Handler<Boolean> handler) {
        StringBuilder query = new StringBuilder()
                .append("SELECT id, type_elt_bilan_periodique ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique ")
                .append("WHERE id IN " + Sql.listPrepared(idsElements));

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (String id : idsElements) {
            params.add(id);
        }

        Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    JsonArray elements = body.getJsonArray("results");
                    List<Integer> idsEPI_AP = new ArrayList<Integer>();
                    List<Integer> idsParcours = new ArrayList<Integer>();

                    for(Object o : elements){
                        JsonArray element = (JsonArray)o;

                        if (element.getInteger(1) != 3) {
                            idsEPI_AP.add(element.getInteger(0));
                        } else {
                            idsParcours.add(element.getInteger(0));
                        }
                    }
                    if(idsEPI_AP.size() > 0){
                        // sinon on vérifie si le prof est dans la liste des intervenants affectés à l'élèment
                        StringBuilder query = new StringBuilder()
                                .append("SELECT id_intervenant ")
                                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_intervenant_matiere ")
                                .append("WHERE id_elt_bilan_periodique IN " + Sql.listPrepared(idsEPI_AP));

                        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
                        for (int id : idsEPI_AP) {
                            params.add(id);
                        }

                        Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {
                                JsonObject body = message.body();

                                if ("ok".equals(body.getString("status"))) {
                                    JsonArray enseignants = body.getJsonArray("results");
                                    JsonArray idsEnseignants = new fr.wseduc.webutils.collections.JsonArray();
                                    for(Object o : enseignants){
                                        JsonArray enseignant = (JsonArray)o;
                                        idsEnseignants.add(enseignant.getString(0));
                                    }
                                    handler.handle(idsEnseignants.contains(user.getUserId()));
                                } else {
                                    handler.handle(false);
                                }
                            }
                        });
                    } else {
                        handler.handle(idsParcours.size() > 0);
                    }
                } else {
                    handler.handle(false);
                }
            }
        });
    }

    public void validateEleve(String idEleve, String idClasse, String idEtablissement, Handler<Boolean> handler) {
        if(idEleve == null){
            handler.handle(true);
        } else {
            JsonArray idsEleves = new fr.wseduc.webutils.collections.JsonArray()
                    .add(idEleve);
            JsonObject action = new JsonObject()
                    .put("action", "eleve.getInfoEleve")
                    .put(Competences.ID_ETABLISSEMENT_KEY, idEtablissement)
                    .put("idEleves", idsEleves);
            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject body = message.body();

                    if ("ok".equals(body.getString("status"))) {
                        JsonObject infosEleve = body.getJsonArray("results").getJsonObject(0);

                        JsonArray idsGroupes = infosEleve.getJsonArray("idGroupes")
                                .add(infosEleve.getString("idClasse"))
                                .addAll(infosEleve.getJsonArray("idManualGroupes"));

                        handler.handle(idsGroupes.contains(idClasse));
                    } else {
                        handler.handle(false);
                    }
                }

            }));
        }
    }

    public void validateMatiere(final HttpServerRequest request, final String idEtablissement, final String idMatiere,
                                final Boolean isBilanPeriodique, final Handler<Boolean> handler) {
        //dans le bilanPériodique le PP peut mettre une appréciation ou un positionnement sur une matière qui n'est pas la sienne
        if(isBilanPeriodique){
            handler.handle(true);
        } else {
            UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                @Override
                public void handle(UserInfos user) {
                    JsonObject action = new JsonObject()
                            .put("action", "matiere.getMatieresForUser")
                            .put("userType", user.getType())
                            .put("idUser", user.getUserId())
                            .put("idStructure", idEtablissement)
                            .put("onlyId", true);

                    if (null == eb) {
                        handler.handle(false);
                    } else {
                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {
                                JsonObject body = message.body();
                                JsonArray listIdsMatieres = body.getJsonArray("results");
                                JsonArray listReswithIdMatieres;
                                if (null != listIdsMatieres) {
                                    if (listIdsMatieres.getValue(0) instanceof String) {
                                        listReswithIdMatieres = null;
                                    } else {
                                        listReswithIdMatieres = ((JsonObject) listIdsMatieres.getJsonObject(0))
                                                .getJsonArray("res");
                                    }
                                    if (!(listIdsMatieres != null &&
                                            (listIdsMatieres.contains(idMatiere)
                                                    || (listReswithIdMatieres != null
                                                    && listReswithIdMatieres.contains(idMatiere))))) {
                                        handler.handle(false);
                                    } else {
                                        handler.handle(true);
                                    }
                                } else {
                                    handler.handle(false);
                                }
                            }
                        }));

                    }
                }
            });
        }
    }

    public static void validateHeadTeacherWithClasses(UserInfos user, JsonArray idsClasse,
                                                      Handler<Either<String, Boolean>> handler) {
        StringBuilder query = new StringBuilder();
        JsonObject value = new JsonObject().put("idUser", user.getUserId())
                .put("idsClasse", idsClasse);
        query.append(" MATCH (u:User {id : {idUser}}) ")
                .append(" OPTIONAL MATCH (c:Class) ")
                .append(" WHERE c.id IN {idsClasse} ")
                .append(" AND (c.externalId IN u.headTeacher OR  c.externalId IN u.headTeacherManual) ")
                .append(" RETURN CASE WHEN c IS NULL THEN [] ELSE collect(c.id) END as idsClasse ");

        Neo4j.getInstance().execute(query.toString(), value, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if (!"ok".equals(body.getString("status"))) {
                    log.error("[validateHeadTeacherWithClasses] : user " + user.getUsername());
                    handler.handle(new Either.Right(false));
                } else {
                    JsonArray result = body.getJsonArray("result");
                    if (result == null || result.size() == 0) {
                        log.debug("[validateHeadTeacherWithClasses] : user " + user.getUsername()
                                + " is not HeadTeacher ");
                        handler.handle(new Either.Right(false));
                    } else {
                        JsonArray headTeacherIdsClass = result.getJsonObject(0).getJsonArray("idsClasse");
                        Map<String, String> distinctidsClasse = new HashMap<>();
                        for (int i = 0; i < idsClasse.size(); i++) {
                            if (!distinctidsClasse.containsKey(idsClasse.getString(i))) {
                                distinctidsClasse.put(idsClasse.getString(i), idsClasse.getString(i));
                            }
                        }
                        Boolean res = (distinctidsClasse.size() == headTeacherIdsClass.size());
                        String res_str = (res)? "OK": "FAIL";
                        log.debug("[HeadTeacherAccess] : user " + user.getUsername() + " ----> " + res_str);
                        handler.handle(new Either.Right<>(res));
                    }
                }
            }
        });
    }

    public static void validateHeadTeacherWithEleves(UserInfos user, JsonArray idsEleve, EventBus eb,
                                                     String idEtablissement, Handler<Either<String, Boolean>> handler) {
        if (eb == null || idsEleve == null) {
            log.error("[validateHeadTeacherWithEleves | idNull] : user " + user.getUsername());
            handler.handle(new Either.Right<>(false));
        } else {
            JsonObject action = new JsonObject()
                    .put("action", "eleve.getInfoEleve")
                    .put(Competences.ID_ETABLISSEMENT_KEY, idEtablissement)
                    .put("idEleves", new fr.wseduc.webutils.collections.JsonArray(idsEleve.getList()));
            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject body = message.body();

                    if (!"ok".equals(body.getString("status"))) {
                        log.error("[validateHeadTeacherWithEleves] : user " + user.getUsername());
                        handler.handle(new Either.Right(false));
                    } else {

                        JsonArray queryResult = body.getJsonArray("results");
                        JsonArray idClasses = new JsonArray();
                        for (int i = 0; i < queryResult.size(); i++) {
                            idClasses.add(queryResult.getJsonObject(i).getString("idClasse"));
                        }
                        validateHeadTeacherWithClasses(user, idClasses, handler);
                    }
                }
            }));
        }
    }

    public static void validateHeadTeacherWithRessources(UserInfos user, JsonArray idsRessources, String table,
                                                         Handler<Either<String, Boolean>> handler) {
        if(idsRessources == null || table == null) {
            log.error("[validateHeadTeacherWithRessources | idNull] : user " + user.getUsername());
            handler.handle(new Either.Right<>(false));
        } else {
            StringBuilder query = new StringBuilder();
            JsonArray param = new JsonArray();
            query.append(" SELECT DISTINCT id_groupe ")
                    .append(" FROM "+ Competences.COMPETENCES_SCHEMA + ".rel_devoirs_groupes ")
                    .append(" INNER JOIN "+ Competences.COMPETENCES_SCHEMA + "." + table)
                    .append(" ON rel_devoirs_groupes.id_devoir = " + table )
                    .append(Competences.DEVOIR_TABLE.equals(table)? ".id":".id_devoir ")
                    .append((idsRessources.size() > 0) ? " AND id IN "
                            + Sql.listPrepared(idsRessources.getList()) : "");

            for (int i = 0; i < idsRessources.size(); i++) {
                param.add(idsRessources.getValue(i));
            }
            Sql.getInstance().prepared(query.toString(), param, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject body = message.body();

                    if (!"ok".equals(body.getString("status"))) {
                        log.error("[validateHeadTeacherWithRessources] : user " + user.getUsername());
                        handler.handle(new Either.Right(false));
                    } else {
                        JsonArray result = body.getJsonArray("results");
                        if (result == null || result.size() == 0) {
                            handler.handle(new Either.Right(false));
                        } else {
                            validateHeadTeacherWithClasses(user, result.getJsonArray(0), handler);
                        }
                    }
                }
            });
        }
    }
}