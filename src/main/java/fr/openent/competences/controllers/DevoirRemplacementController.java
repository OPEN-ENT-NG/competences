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

package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.service.DevoirRemplacementService;
import fr.openent.competences.service.impl.DefaultDevoirService;
import fr.openent.competences.service.impl.DefaultDevoirRemplacementService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.http.response.DefaultResponseHandler.*;
@Deprecated
public class DevoirRemplacementController extends ControllerHelper {
    /**
     * Déclaration des services
     */
    private final DevoirRemplacementService remplacementService;

    public DevoirRemplacementController() {
        remplacementService = new DefaultDevoirRemplacementService(Competences.COMPETENCES_SCHEMA, Field.REL_PROFESSEURS_REMPLACANTS_TABLE);
    }


    /**
     * Récupère les remplacments en cours
     * @param request
     */
    @Get("/remplacements/list")
    @ApiDoc("Récupère les remplacments en cours")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void list (final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                    remplacementService.listRemplacements(user.getStructures(), handler);
                }else{
                    unauthorized(request);
                }
            }
        });
    }




    @Get("/remplacements/classes")
    @ApiDoc("Récupère la liste des classes qui font ou ont fait l'objet de remplacement")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getRemplacementClasses (final HttpServerRequest request) {
        if (request.params().contains("idEtablissement")) {
            UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                @Override
                public void handle(final UserInfos user) {
                    remplacementService.getRemplacementClasse(user.getUserId(),request.params().get("idEtablissement"),arrayResponseHandler(request));
                }
            });
        } else {
            badRequest(request);
        }
    }

}
