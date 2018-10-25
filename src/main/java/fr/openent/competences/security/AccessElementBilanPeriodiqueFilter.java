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
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class AccessElementBilanPeriodiqueFilter implements ResourcesProvider {
    protected static final Logger log = LoggerFactory.getLogger(AccessElementProgrammeFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, final UserInfos user,
                          final Handler<Boolean> handler) {
        FilterUserUtils userUtils = new FilterUserUtils(user, null);

        if(new WorkflowActionUtils().hasRight(user, WorkflowActions.CREATE_ELEMENT_BILAN_PERIODIQUE.toString())) {
            resourceRequest.resume();
            handler.handle(true);
        } else {
            switch (user.getType()) {
                case "Teacher": {
                    resourceRequest.pause();
                    MultiMap params = resourceRequest.params();
                    if(params.contains("idElement") && (params.contains("idClasse"))) {
                        userUtils.validateElement(params.getAll("idElement"), params.get("idClasse"),
                                new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean isValid) {
                                resourceRequest.resume();
                                handler.handle(isValid);
                                /*if(isValid){
                                    resourceRequest.resume();
                                    handler.handle(true);
                                }
                                else {
                                    resourceRequest.resume();
                                    handler.handle(false);
                                }*/
                            }
                        });
                    }
                    else if(params.contains("idClasse")
                            && params.contains("idEtablissement")
                            && params.contains("idEnseignant")) {

                        resourceRequest.resume();
                        handler.handle( userUtils.validateStructure(params.get("idEtablissement"))
                                && userUtils.validateUser(params.get("idEnseignant")));
                    }
                    else {
                        resourceRequest.resume();
                        handler.handle(true);
                    }
                }
                break;
                default: {
                    resourceRequest.resume();
                    handler.handle(false);
                }
            }
        }
    }
}
