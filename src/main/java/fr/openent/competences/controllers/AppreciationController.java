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
import fr.openent.competences.security.AccessAppreciationClasseFilter;
import fr.openent.competences.security.AccessAppreciationFilter;
import fr.openent.competences.security.CreateEvaluationWorkflow;
import fr.openent.competences.security.CreateOrUpdateAppreciationClasseFilter;
import fr.openent.competences.security.utils.FilterPeriodeUtils;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.AppreciationService;
import fr.openent.competences.service.impl.DefaultAppreciationService;
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
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

/**
 * Created by anabah on 01/03/2017.
 */
public class AppreciationController extends ControllerHelper {




    /**
     * Déclaration des services
     */
    private final AppreciationService appreciationService;

    public AppreciationController() {
        appreciationService = new DefaultAppreciationService(Competences.COMPETENCES_SCHEMA,
                Competences.APPRECIATIONS_TABLE);
    }




    /**
     * Créer une appreciation avec les données passées en POST
     * @param request
     */
    @Post("/appreciation")
    @ApiDoc("Créer une appreciation")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(CreateEvaluationWorkflow.class)
    public void create(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    String validator = pathPrefix + Competences.SCHEMA_APPRECIATIONS_CREATE;
                    RequestUtils.bodyToJson(request, validator,
                            new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            appreciationService.createAppreciation(resource, user, notEmptyResponseHandler(request));
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
     * Modifie une appreciation avec les données passées en PUT
     * @param request
     */
    @Put("/appreciation")
    @ApiDoc("Modifie une appreciation")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AccessAppreciationFilter.class)
    public void update(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    String validator = pathPrefix + Competences.SCHEMA_APPRECIATIONS_UPDATE;
                    RequestUtils.bodyToJson(request,validator , new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            appreciationService.updateAppreciation(resource, user, defaultResponseHandler(request));
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
     * Supprime l'appreciation passée en paramètre
     * @param request
     */
    @Delete("/appreciation")
    @ApiDoc("Supprimer une appréciation donnée")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AccessAppreciationFilter.class)
    public void deleteAppreciationDevoir(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){

                    Long idAppreciation;
                    try {
                        idAppreciation = Long.parseLong(request.params().get("idAppreciation"));
                    } catch(NumberFormatException e) {
                        log.error("Error : idAppreciation must be a long object", e);
                        badRequest(request, e.getMessage());
                        return;
                    }

                    appreciationService.deleteAppreciation(idAppreciation, user, defaultResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }


    /**
     * Créer/metttre à jour une appreciation avec les données passées en POST
     * @param request
     */
    @Post("/appreciation/classe")
    @ApiDoc("Créer ou mettre à jour une appreciation d'une classe pour une période et matière donnée")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(CreateOrUpdateAppreciationClasseFilter.class)
    public void createOrUpdateAppreciationClasse (final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    String validator = pathPrefix + Competences.SCHEMA_APPRECIATIONS_CLASSE;
                    RequestUtils.bodyToJson(request, validator,
                            new Handler<JsonObject>() {
                                @Override
                                public void handle(final JsonObject appreciation) {

                                    final Integer idPeriode = appreciation.getInteger("id_periode");
                                    final String idMatiere = appreciation.getString("id_matiere");
                                    final String idClasse = appreciation.getString("id_classe");
                                    final String idEtablissement = appreciation.getString("idEtablissement");

                                    WorkflowActionUtils.hasHeadTeacherRight(user, new JsonArray().add(idClasse),
                                            null,null, null, null, null,
                                            new Handler<Either<String, Boolean>>() {
                                                @Override
                                                public void handle(Either<String, Boolean> event) {
                                                    Boolean isHeadTeacher;
                                                    if(event.isLeft()) {
                                                        isHeadTeacher = false;
                                                    }
                                                    else {
                                                         isHeadTeacher = event.right().getValue();
                                                    }
                                                    createOrUpdateAppreciationClasseUtils(request,
                                                    idPeriode,idMatiere, idClasse,idEtablissement, user, appreciation,
                                                     isHeadTeacher);
                                                }
                                            });

                                }
                            });
                }else {
                    log.error("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }
    private void createOrUpdateAppreciationClasseUtils(final HttpServerRequest request,
                                    final Integer idPeriode, final String idMatiere, final String idClasse,
                                    final String idEtablissement, final UserInfos user, final JsonObject appreciation,
                                    final Boolean isHeadTeacher) {
        // si chef etab ou prof principal sur la classe, on ne fait pas plus de controles (date fin de saisie, matiere)
        if(new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString())
                || isHeadTeacher) {
            appreciationService.createOrUpdateAppreciationClasse(appreciation.getString("appreciation"),
                    idClasse,
                    idPeriode,
                    idMatiere
                    , defaultResponseHandler(request));
        } else {
            // sinon on vérifier la date de fin de saisie et la présence de la matière sur l'utilisateur
            FilterPeriodeUtils filterPeriodeUtils = new FilterPeriodeUtils(eb,user);
            filterPeriodeUtils.validateEndSaisie(request, idClasse, idPeriode, new Handler<Boolean>() {
                @Override
                public void handle(Boolean isUpdatable) {
                    //verif date fin de saisie
                    if(isUpdatable) {
                        new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere, false,
                                new Handler<Boolean>() {
                            @Override
                            public void handle(final Boolean hasAccessToMatiere) {
                                // verif possesion matière
                                if(hasAccessToMatiere) {
                                    appreciationService.createOrUpdateAppreciationClasse(appreciation
                                                    .getString("appreciation"),
                                            idClasse,
                                            idPeriode,
                                            idMatiere
                                            , defaultResponseHandler(request));
                                } else {
                                    log.error("hasAccessToMatiere = " + hasAccessToMatiere);
                                    Renders.unauthorized(request);
                                }
                            }
                        });
                    } else {
                        log.error("Date de fin de saisie dépassée : isUpdatable = " + isUpdatable);
                        Renders.unauthorized(request);
                    }
                }
            });
        }

    }
    /**
     * Récupère les annotations de l'établissement
     * @param request
     */
    @Get("/appreciation/classe")
    @ApiDoc("Récupère l'appreciation d'une classe pour une période et matière donnée")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AccessAppreciationClasseFilter.class)
    public void getAppreciationClasse(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>(){
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    appreciationService.getAppreciationClasse(
                            new String[]{request.params().get("id_classe")},
                            Integer.parseInt(request.params().get("id_periode")),
                            new String[]{request.params().get("id_matiere")}, stringJsonObjectEither -> {
                                if (stringJsonObjectEither.isRight()) {
                                    if(stringJsonObjectEither.right().getValue().size() > 0) {
                                        Renders.renderJson(request, stringJsonObjectEither.right().getValue().getJsonObject(0), 200);
                                    }else{
                                        Renders.renderJson(request, new JsonObject(), 200);
                                    }
                                } else {
                                    JsonObject error = (new JsonObject()).put("error", stringJsonObjectEither.left().getValue());
                                    Renders.renderJson(request, error, 400);
                                }
                            });
                } else {
                    badRequest(request);
                }
            }
        });
    }
}

