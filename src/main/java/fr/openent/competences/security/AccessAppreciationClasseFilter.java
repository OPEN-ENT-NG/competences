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

import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AccessAppreciationClasseFilter implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(AccessAppreciationClasseFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user, final Handler<Boolean> handler) {
        FilterUserUtils userUtils = new FilterUserUtils(user, null);

        boolean isAdmin = new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());
        if(isAdmin) {
            handler.handle(true);
            return;
        }

        if (user.getType().equals("Teacher")) {
            resourceRequest.pause();
            MultiMap params = resourceRequest.params();

            if (!(params.contains("id_classe") &&
                    params.contains("id_periode") &&
                    params.contains("id_matiere")) ) {
                resourceRequest.resume();
                handler.handle(false);
                return;
            }

            WorkflowActionUtils.hasHeadTeacherRight(user, new JsonArray().add(params.get("id_classe")),
                    null, null, null, null, new Handler<Either<String, Boolean>>() {
                        @Override
                        public void handle(Either<String, Boolean> event) {
                            Boolean isHeadTeacher;
                            if (event.isLeft()) {
                                isHeadTeacher = false;
                            }
                            else {
                                isHeadTeacher = event.right().getValue();
                            }

                            if (isHeadTeacher) {
                                resourceRequest.resume();
                                handler.handle(true);
                                return;
                            }
                            else {
                                //On check que la classe passée en paramètre soit bien ceux de l'utilisateur
                                if (!userUtils.validateClasse(params.get("id_classe"))) {
                                    resourceRequest.resume();
                                    handler.handle(false);
                                    return;
                                }


                                Integer idPeriode;
                                if (params.get("id_periode") != null) {
                                    try {
                                        idPeriode = Integer.parseInt(params.get("id_periode"));
                                    } catch (NumberFormatException e) {
                                        log.error("Error : id_periode must be an Integer", e);
                                        resourceRequest.resume();
                                        handler.handle(false);
                                        return;
                                    }
                                }

                                resourceRequest.resume();
                                handler.handle(true);
                            }
                        }
                    }
            );
        }
        else {
            handler.handle(false);
        }
    }
}
