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

package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.service.DevoirService;
import fr.openent.competences.service.RemplacementService;
import fr.openent.competences.service.impl.DefaultDevoirService;
import fr.openent.competences.service.impl.DefaultRemplacementService;
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

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class RemplacementController extends ControllerHelper {


    /**
     * Déclaration des services
     */
    private final RemplacementService remplacementService;
    private final DevoirService devoirService;

    public RemplacementController() {
        remplacementService = new DefaultRemplacementService(Competences.COMPETENCES_SCHEMA, Competences.REL_PROFESSEURS_REMPLACANTS_TABLE);
        devoirService = new DefaultDevoirService();
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

    /**
     * Ajout d'un remplacement
     * @param request
     */
    @Post("/remplacement/create")
    @ApiDoc("Ajout d'un remplacement")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void create (final HttpServerRequest request){
        // TODO Sécuriser la méthode
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){

                    RequestUtils.bodyToJson(request, pathPrefix +
                            Competences.SCHEMA_REL_PROFESSEURS_REMPLACANTS_CREATE, new Handler<JsonObject>() {
                                @Override
                                public void handle(final JsonObject poRemplacement) {
                                    remplacementService.createRemplacement(poRemplacement, notEmptyResponseHandler(request));
                                }
                            });

                }else{
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    /**
     * Suppression d'un remplacement
     * @param request
     */
    @Delete("/remplacement/delete")
    @ApiDoc("Ajout d'un remplacement")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void delete (final HttpServerRequest request){
        // TODO Sécuriser la méthode
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){

                    MultiMap params = request.params();
                    params.get("id_titulaire");
                    params.get("id_remplacant");
                    params.get("date_debut");
                    params.get("date_fin");
                    params.get("id_titulaire");


                    remplacementService.deleteRemplacement(params.get("id_titulaire"),
                            params.get("id_remplacant"),
                            params.get("date_debut"),
                            params.get("date_fin"),
                            params.get("id_etablissement"),
                            notEmptyResponseHandler(request));

                }else{
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
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
                    devoirService.getClassesIdsDevoir(user, request.params().get("idEtablissement"), new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                final JsonArray classeIds = event.right().getValue();
                                remplacementService.getRemplacementClasse(classeIds, user, request.params().get("idEtablissement"), new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            JsonArray values = event.right().getValue();
                                            renderJson(request, values);
                                        } else {
                                            renderError(request);
                                        }
                                    }
                                });
                            } else {
                                renderError(request);
                            }
                        }
                    });
                }
            });
        } else {
            badRequest(request);
        }
    }

}
