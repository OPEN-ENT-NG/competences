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
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AccessControleByClassFilter implements ResourcesProvider{
    protected static final Logger log = LoggerFactory.getLogger(AccessControleByClassFilter.class);

    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
        boolean isAdmin = WorkflowActionUtils.hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());

        if(isAdmin || "Personnel".equals(user.getType())){
            resourceRequest.resume();
            handler.handle(true);
        } else if("Teacher".equals(user.getType())){
            resourceRequest.pause();
            MultiMap params = resourceRequest.params();
            if(!params.contains("idClasse") && !params.contains("idStructure")){
                resourceRequest.resume();
                handler.handle(false);
            } else {
                JsonArray idClasses = new JsonArray().add(params.get("idClasse"));
                String idEtablissement = params.get("idStructure");
                String idPeriodeString = params.get("idPeriode");
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
                Long finalIdPeriode = idPeriode;
                WorkflowActionUtils.hasHeadTeacherRight(user, idClasses,null,null, null,
                        null, null, new Handler<Either<String, Boolean>>() {
                            @Override
                            public void handle(Either<String, Boolean> event) {
                                Boolean isHeadTeacher = event.isRight() ? event.right().getValue() : false;

                                if(isHeadTeacher) {
                                    handler.handle(true);
                                } else {
                                    //On check que la classe et l'établissement passé en paramètre soit bien ceux de l'utilisateur
                                    DefaultUtilsService utilsService = new DefaultUtilsService(AccessEventBus.getInstance().getEventBus());
                                    utilsService.hasService(idEtablissement, idClasses, null, finalIdPeriode, user, isValid -> {
                                        resourceRequest.resume();
                                        handler.handle(isValid);
                                    });
                                }
                            }
                        });
            }
        } else {
            handler.handle(false);
        }
    }
}