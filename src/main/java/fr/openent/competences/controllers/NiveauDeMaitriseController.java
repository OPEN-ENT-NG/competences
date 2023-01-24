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
import fr.openent.competences.security.*;
import fr.openent.competences.service.impl.DefaultNiveauDeMaitriseService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;


/**
 * Created by anabah on 30/08/2017.
 */
public class NiveauDeMaitriseController extends ControllerHelper {

    DefaultNiveauDeMaitriseService niveauDeMaitriseService;

    public NiveauDeMaitriseController() {
        niveauDeMaitriseService = new DefaultNiveauDeMaitriseService();

    }

    @Get("/maitrise/level/:idEtablissement")
    @ApiDoc("Recupere tous les niveaux de maitrise d'un établissement")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    public void getMaitriseLevel(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if (user != null) {
                    if (request.params().isEmpty()) {
                        badRequest(request);
                    } else {
                        final Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                        String idEtablissement = request.params().get("idEtablissement");
                        niveauDeMaitriseService.getNiveauDeMaitrise(idEtablissement, null, handler);
                    }
                }
            }
        });
    }

    @Get("/maitrise/perso/use/:idUser")
    @ApiDoc("Vérifie si un utilisateur utilise la personnification des couleurs de compétence de son établissement")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    public void getPersoNiveauMaitrise(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    if (request.params().isEmpty()) {
                        badRequest(request);
                    } else {
                        final Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                        String idUser = request.params().get("idUser");
                        niveauDeMaitriseService.getPersoNiveauMaitrise(idUser, handler);
                    }
                }
            }
        });
    }

    /**
     * Créer un niveau de maitrise avec les données passées en POST
     * @param request
     */
    @Post("/maitrise/level")
    @ApiDoc("Créer un niveau de maitrise ")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void create(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    RequestUtils.bodyToJson(request, pathPrefix +
                            Competences.SCHEMA_MAITRISE_CREATE, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject niveauDeMaitrise) {
                            niveauDeMaitriseService.createMaitrise(niveauDeMaitrise,user,arrayResponseHandler(request));
                        }

                    });
                }else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    /**
     * Marquer l'utilisateur comme utilisant la personnification du niveau de maitrise de son établissement
     */
    @Post("/maitrise/perso/use")
    @ApiDoc("Marquer l'utilisateur comme utilisant la personnification du niveau de maitrise de son établissement")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void markUserInUsePerso(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    RequestUtils.bodyToJson(request, pathPrefix +
                            Competences.SCHEMA_USE_PERSO_NIVEAU_COMPETENCE, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject idUser) {
                            niveauDeMaitriseService.markUsePerso(idUser, arrayResponseHandler(request));
                        }

                    });
                }else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }


    /**
     * Modifie un niveau de maitrise avec les données passées en PUT
     * @param request
     */
    @Put("/maitrise/level/:idNiveau")
    @ApiDoc("Modifie un niveau de maitrise")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void update(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    RequestUtils.bodyToJson(request, pathPrefix +
                            Competences.SCHEMA_MAITRISE_UPDATE, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            niveauDeMaitriseService.update(resource, user, defaultResponseHandler(request));
                        }
                    });
                }else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }
    /**
     * Supprime tous les niveaux de maitrise d'un étbalissement donné
     * @param request
     */
    @Delete("/maitrise/level/:idEtablissement")
    @ApiDoc("Supprimer tous les niveaux de maitrise d'un étbalissement donné")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void delete(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    String idEtablissement = request.params().get("idEtablissement");
                    niveauDeMaitriseService.delete(idEtablissement, user, defaultResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Permet à un utilisateur de ne plus  utiliser la personnalisation des niveaux de compétences
     * @param request
     */
    @Delete("/maitrise/perso/use/:idUser")
    @ApiDoc("Permet à un utilisateur de ne plus  utiliser la personnalisation des niveaux de compétences")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void deleteUserFromPerso(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    String idUser = request.params().get("idUser");
                    niveauDeMaitriseService.deleteUserFromPerso(idUser, defaultResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }


}