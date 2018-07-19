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

import java.util.*;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

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

    public void validateMatiere(final HttpServerRequest request, final String idEtablissement, final String idMatiere, final Handler<Boolean> handler) {

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


    public static void validateHeadTeacherWithEleves(UserInfos user, JsonArray idsEleve,
                                                     EventBus eb,
                                                     Handler<Either<String, Boolean>> handler) {
        if (eb == null || idsEleve == null) {
            log.error("[validateHeadTeacherWithEleves | idNull] : user " + user.getUsername());
            handler.handle(new Either.Right<>(false));
        } else {
            JsonObject action = new JsonObject()
                    .put("action", "eleve.getInfoEleve")
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
        }
        else {
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
            Sql.getInstance().prepared(query.toString(), param,
                    new Handler<Message<JsonObject>>() {
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