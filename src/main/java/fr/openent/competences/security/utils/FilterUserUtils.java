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
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by ledunoiss on 20/10/2016.
 */
public class FilterUserUtils{

    private UserInfos user;
    private EventBus eb;

    public FilterUserUtils (UserInfos user, EventBus eb) {
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
        return user.getClasses().contains(idClasse);
    }

    public void validateMatiere (final HttpServerRequest request, final String idEtablissement, final String idMatiere, final Handler<Boolean> handler) {

        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

            @Override
            public void handle(UserInfos user) {

                JsonObject action = new JsonObject()
                        .putString("action", "matiere.getMatieresForUser")
                        .putString("userType", user.getType())
                        .putString("idUser", user.getUserId())
                        .putString("idStructure", idEtablissement)
                        .putBoolean("onlyId", true);

                if(null == eb) {
                    handler.handle(false);
                }
                else {
                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {

                            JsonObject body = message.body();
                            JsonArray listIdsMatieres = body.getArray("results");
                            JsonArray listReswithIdMatieres;
                            if (null != listIdsMatieres) {
                                if (listIdsMatieres.get(0) instanceof String) {
                                    listReswithIdMatieres = null;
                                } else {
                                    listReswithIdMatieres = ((JsonObject) listIdsMatieres.get(0))
                                            .getArray("res");
                                }
                                if (!(listIdsMatieres != null &&
                                        (listIdsMatieres.contains(idMatiere)
                                                || (listReswithIdMatieres != null
                                                && listReswithIdMatieres.contains(idMatiere))))) {
                                    handler.handle(false);
                                } else {
                                    handler.handle(true);
                                }
                            }
                            else {
                                handler.handle(false);
                            }
                        }
                    });

                }
            }
        });
    }
}
