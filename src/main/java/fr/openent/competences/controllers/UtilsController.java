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
import fr.openent.competences.Utils;
import fr.openent.competences.security.*;
import fr.openent.competences.security.modelbulletinrights.AccessExportModelBulletin;
import fr.openent.competences.security.AccessParamLinkGroupCycleStructure;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class UtilsController extends ControllerHelper {

    private final UtilsService utilsService;
    private final Storage storage;

    public UtilsController(Storage storage, EventBus eb) {
        utilsService = new DefaultUtilsService(eb);
        this.storage = storage;
    }

    /**
     * Retourne tous les types de devoir par etablissement
     * @param request
     */
    @Get("/types")
    @ApiDoc("Retourne tous les types de devoir par etablissement")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    public void view(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if (user != null) {
                    Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                    utilsService.listTypesDevoirsParEtablissement(request.params().get("idEtablissement"), handler);
                } else {
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Retourne la liste des enfants pour un utilisateur donné
     * @param request
     */
    @Get("/enfants")
    @ApiDoc("Retourne la liste des enfants pour un utilisateur donné")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(IsRelative.class)
    public void getEnfants(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if (user != null) {
                    Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                    utilsService.getEnfants(user.getUserId(), new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                final JsonArray resultJsonArrayEnfants = new JsonArray();
                                JsonArray jsonArrayEnfants = event.right().getValue();
                                List<String> vIdClasseList = new ArrayList<String>();
                                for (int i = 0; i < jsonArrayEnfants.size(); i++) {
                                    JsonObject jsonObjectEnfant = jsonArrayEnfants.getJsonObject(i);
                                    if (null != jsonObjectEnfant && jsonObjectEnfant.containsKey("idClasse")) {
                                        String idClasse = jsonObjectEnfant.getString("idClasse");
                                        vIdClasseList.add(idClasse);
                                    }
                                }
                                utilsService.getCycle(vIdClasseList, new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            JsonArray queryResult = event.right().getValue();
                                            for (int i = 0; i < jsonArrayEnfants.size(); i++) {
                                                JsonObject jsonObjectEnfant = jsonArrayEnfants.getJsonObject(i);
                                                if (null != jsonObjectEnfant && jsonObjectEnfant.containsKey("idClasse")) {
                                                    String idClasse = jsonObjectEnfant.getString("idClasse");
                                                    for (int j = 0; j < queryResult.size(); j++) {
                                                        JsonObject jsonObjectClassCycle = queryResult.getJsonObject(j);
                                                        if (null != jsonObjectClassCycle && jsonObjectClassCycle.containsKey("id_groupe")
                                                                && jsonObjectClassCycle.getString("id_groupe").equalsIgnoreCase(idClasse)) {
                                                            jsonObjectEnfant.put("id_cycle", jsonObjectClassCycle.getInteger("id_cycle"));
                                                            break;
                                                        }
                                                    }
                                                    resultJsonArrayEnfants.add(jsonObjectEnfant);
                                                }
                                            }
                                            handler.handle(new Either.Right<String, JsonArray>(resultJsonArrayEnfants));
                                        } else {
                                            handler.handle(new Either.Left<String, JsonArray>(event.left().getValue()));
                                            log.error("getInfoEleve : getCycle : " + event.left().getValue());
                                        }
                                    }
                                });
                            }
                        }
                    });
                } else {
                    unauthorized(request);
                }
            }
        });
    }

    @Get("/user/list")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    public void list(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if (user != null) {
                    final String structureId = request.params().get("structureId");
                    final JsonArray types = new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("profile"));

                    utilsService.list(structureId, types, arrayResponseHandler(request));
                } else {
                    unauthorized(request);
                }
            }
        });
    }

    @Get("/classe/groupes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    @ApiDoc("Retourne une classe avec ses groupes (s'il y en a)")
    public void getGroupesClasse(final HttpServerRequest request){
        final String idStructure = request.params().get("idStructure");
        Utils.getClassesStruct(eb, idStructure, eventClasses -> {
            if (eventClasses.isRight()) {
                List<String> idClasses = eventClasses.right().getValue();
                Utils.getGroupesClasse(eb, new JsonArray(idClasses), eventGroups -> {
                    if (eventGroups.isRight()) {
                        Renders.renderJson(request, eventGroups.right().getValue());
                    } else {
                        badRequest(request);
                    }
                });
            } else {
                badRequest(request);
            }
        });
    }

    @Put("/link/groupes/cycles")
    @ApiDoc("Met à jour les cycles des classes de l'établissement")
    @SecuredAction(value = Competences.PARAM_LINK_GROUP_CYCLE_RIGHT)
    public void updateLinkGroupesCycles(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject ressource) {
                Number id_cycle = ressource.getInteger("id_cycle");
                final String[] idClasses = (String[]) ressource.getJsonArray("idClasses").getList().toArray(new String[0]);
                final Number[] typesGroupes = (Number[]) ressource.getJsonArray("typesGroupes").getList().toArray(new Number[0]);

                utilsService.linkGroupesCycles(idClasses, id_cycle, typesGroupes,
                        new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle(Either<String, JsonArray> event) {
                                if (event.isRight()) {
                                    Renders.renderJson(request, event.right().getValue());
                                } else {
                                    leftToResponse(request, event.left());
                                }
                            }
                        });
            }
        });
    }

    @Post("/link/check/data/classes")
    @ApiDoc("Changement d'affectation de cycle sur une classe")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessParamLinkGroupCycleStructure.class)
    public void checkDataOnClasses(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject ressource) {
                final String[] idClasses = (String[]) ressource.getJsonArray("idClasses")
                        .getList().toArray(new String[0]);

                utilsService.checkDataOnClasses(idClasses, new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            Renders.renderJson(request, event.right().getValue());
                        } else {
                            leftToResponse(request, event.left());
                        }
                    }
                });
            }
        });
    }

    @Post("/eleve/evenements")
    @SecuredAction(Competences.CAN_UPDATE_RETARDS_AND_ABSENCES)
    public void insertRetardOrAbscence(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject ressource) {
                String idEleve = ressource.getString("idEleve");
                String colonne = ressource.getString("colonne");
                Long idPeriode = ressource.getLong("idPeriode");
                Long value = ressource.getLong("value");
                if(value < 0)
                    value = 0L;

                utilsService.insertEvenement(idEleve, colonne, idPeriode, value, arrayResponseHandler(request));
            }
        });
    }

    @Post("/graph/img")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessExportModelBulletin.class)
    public void postImgForBulletins(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, user -> {
            this.storage.writeUploadFile(request, uploaded -> {
                if (!"ok".equals(uploaded.getString("status"))) {
                    log.error(uploaded.encode());
                    badRequest(request, uploaded.getString("message"));
                    return;
                }

                // Vérification du format qui doit-être une image
                JsonObject metadata = uploaded.getJsonObject("metadata");
                String contentType = metadata.getString("content-type");

                if (contentType.contains("image")) {
                    Renders.renderJson(request, uploaded);
                } else {
                    badRequest(request, "Format de fichier incorrect");
                }
            });
        });
    }
}
