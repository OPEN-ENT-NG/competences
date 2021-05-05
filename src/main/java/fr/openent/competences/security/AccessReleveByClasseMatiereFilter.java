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

package fr.openent.competences.security;

import fr.openent.competences.AccessEventBus;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by ledunoiss on 20/10/2016.
 */
public class AccessReleveByClasseMatiereFilter implements ResourcesProvider {
    protected static final Logger log = LoggerFactory.getLogger(AccessReleveByClasseMatiereFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, final UserInfos user,
                          final Handler<Boolean> handler) {
        if ("GET".equals(resourceRequest.method().toString())) {
            String idClasse = resourceRequest.params().get("idClasse");
            String idEtablissement = resourceRequest.params().get("idEtablissement");
            String idMatiere = resourceRequest.params().get("idMatiere");
            String idPeriodeString = resourceRequest.params().get("idPeriode");
            Long idPeriode = null;
            if (idPeriodeString != null) {
                try {
                    idPeriode = Long.parseLong(idPeriodeString);
                } catch (NumberFormatException e) {
                    log.error(" Error :idPeriode must be a long object ", e);
                    resourceRequest.resume();
                    handler.handle(false);
                    return;
                }
            }
            authorizeAccess(resourceRequest, user, idEtablissement, idClasse, idMatiere, idPeriode, handler);
        } else {
            RequestUtils.bodyToJson(resourceRequest, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject resource) {
                    String idClasse = resource.getString("idClasse");
                    String idEtablissement = resource.getString("idEtablissement");
                    String idMatiere = resource.getString("idMatiere");
                    Long idPeriode = resource.getLong("idPeriode");
                    authorizeAccess(resourceRequest, user, idEtablissement, idClasse, idMatiere, idPeriode, handler);
                }
            });
        }
    }

    private void authorizeAccess(final HttpServerRequest resourceRequest, UserInfos user, String idEtablissement,
                                 String idClasse, String idMatiere, Long idPeriode, final Handler<Boolean> handler) {
        boolean isAdmin = WorkflowActionUtils.hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());

        if(isAdmin) {
            resourceRequest.resume();
            handler.handle(true);
        } else if (user.getType().equals("Teacher")) {
            resourceRequest.pause();

            if (null == idClasse || null == idMatiere || null == idEtablissement) {
                resourceRequest.resume();
                handler.handle(false);
            } else {
                JsonArray idClasses = new JsonArray().add(idClasse);
                WorkflowActionUtils.hasHeadTeacherRight(user, idClasses,null, null,null,
                        null, null, event -> {
                            boolean isHeadTeacher = event.isRight() ? event.right().getValue() : false;

                            // Si on est professeur principal de la classe, on a le droit
                            if(isHeadTeacher) {
                                resourceRequest.resume();
                                handler.handle(true);
                            } else {
                                //On check que la classe et l'établissement passé en paramètre soit bien ceux de l'utilisateur
                                DefaultUtilsService utilsService = new DefaultUtilsService(AccessEventBus.getInstance().getEventBus());
                                utilsService.hasService(idEtablissement, idClasses, idMatiere, idPeriode, user, isValid -> {
                                    resourceRequest.resume();
                                    handler.handle(isValid);
                                });
                            }
                        });
            }
        } else {
            resourceRequest.resume();
            handler.handle(false);
        }
    }
}
