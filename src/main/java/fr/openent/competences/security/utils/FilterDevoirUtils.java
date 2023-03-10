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
import fr.openent.competences.constants.Field;
import fr.wseduc.webutils.Either;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


/**
 * Created by ledunoiss on 20/10/2016.
 */
public class FilterDevoirUtils  extends ControllerHelper {

    public void validateOwnerDevoir(Integer idDevoir, String owner, final Handler<Boolean> handler) {
        StringBuilder query = new StringBuilder()
                .append("SELECT count(" + Field.DEVOIRS_TABLE + ".*) " +
                        "FROM " + Competences.COMPETENCES_SCHEMA + "." + Field.DEVOIRS_TABLE +
                        "WHERE " + Field.DEVOIRS_TABLE + "." + Field.ID + " = ? " +
                        "AND " + Field.DEVOIRS_TABLE + "." + Field.OWNER + " = ?;");

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(idDevoir).add(owner);

        Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                Long count = SqlResult.countResult(message);
                handler.handle(count != null && count > 0);
            }
        });
    }

    public void validateDevoirFinSaisie(Long idDevoir, UserInfos user, final Handler<Boolean> handler) {
        StringBuilder query = new StringBuilder()
                .append("SELECT count(" + Field.DEVOIRS_TABLE + "." + Field.ID + ") " +
                        "FROM " + Competences.COMPETENCES_SCHEMA + "." + Field.NOTES_TABLE + ", " + Competences.VSCO_SCHEMA + ".periode "+
                        "WHERE " + Field.DEVOIRS_TABLE + "." + Field.ID + " = ? " +
                        "AND " + Field.DEVOIRS_TABLE + "." + Field.OWNER + " = ?  " +
                        "AND now() < " + Field.VIESCO_PERIODE_TABLE + ".date_fin_saisie;" );

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(idDevoir).add(user.getUserId());

        Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                Long count = SqlResult.countResult(message);
                handler.handle(count != null && count > 0);
            }
        });
    }
    public void validateAccessDevoir(final Long idDevoir,
                                     final UserInfos user, final Handler<Boolean> handler) {

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder()
                .append("SELECT count(*) FROM " + Competences.COMPETENCES_SCHEMA + "." + Field.DEVOIRS_TABLE);
        query.append(" WHERE " + Field.DEVOIRS_TABLE + "." + Field.ID + " = ? ")
                .append("AND (" + Field.DEVOIRS_TABLE + "." + Field.OWNER + " = ? OR ")
                .append("? IN (SELECT member_id ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + "." + Field.DEVOIR_SHARE_TABLE)
                .append(" WHERE resource_id = ? ")
                .append("AND action = '" + Competences.DEVOIR_ACTION_UPDATE + "')")
                .append(")");

        // Ajout des params pour la partie de la requête où on vérifie si on est le propriétaire
        params.add(idDevoir);
        params.add(user.getUserId());

        // Ajout des params pour la partie de la requête où on vérifie si on a des droits
        // de partage provenant d'un remplaçant
        params.add(user.getUserId());
        params.add(idDevoir);


        Sql.getInstance().prepared(query.toString(), params,
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        Long count = SqlResult.countResult(message);
                        handler.handle(count != null && count > 0);
                    }
                });


    }
    public void validateAccesDevoirWithHeadTeacher(final Long idDevoir,
                                    final UserInfos user, final Handler<Boolean> handler,
                                    final HttpServerRequest resourceRequest) {
        WorkflowActionUtils.hasHeadTeacherRight(user, null,
                new JsonArray().add(idDevoir),
                Competences.DEVOIR_TABLE, null, null, null,
                new Handler<Either<String, Boolean>>() {
                    @Override
                    public void handle(Either<String, Boolean> event) {
                        if(event.isLeft()) {
                            validateAccessDevoirUtils(idDevoir, resourceRequest,user,handler);
                        }
                        else {
                            Boolean isHeadTeacher = event.right().getValue();
                            if(isHeadTeacher) {
                                handler.handle(true);
                            }
                            else {
                                validateAccessDevoirUtils(idDevoir, resourceRequest,user,handler);
                            }
                        }

                    }
                });
    }

    private void validateAccessDevoirUtils (Long idDevoir, final HttpServerRequest resourceRequest,
                                            UserInfos user, final Handler<Boolean> handler) {
        new FilterDevoirUtils().validateAccessDevoir(idDevoir, user, new Handler<Boolean>() {
            @Override
            public void handle(Boolean isValid) {
                resourceRequest.resume();
                handler.handle(isValid);
            }
        });
    }
}
