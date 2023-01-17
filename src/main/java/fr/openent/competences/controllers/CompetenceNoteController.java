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
import fr.openent.competences.constants.Field;
import fr.openent.competences.security.*;
import fr.openent.competences.service.BfcSyntheseService;
import fr.openent.competences.service.CompetenceNiveauFinalService;
import fr.openent.competences.service.CompetenceNoteService;
import fr.openent.competences.service.StructureOptionsService;
import fr.openent.competences.service.impl.*;
import fr.openent.competences.utils.HomeworkUtils;
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
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.*;

/**
 * Created by ledunoiss on 19/10/2016.
 */
public class CompetenceNoteController extends ControllerHelper {

    private final CompetenceNoteService competencesNotesService;
    private final BfcSyntheseService syntheseService;
    private final CompetenceNiveauFinalService competenceNiveauFinalService;
    private final DefaultDevoirService devoirsService;
    private final StructureOptionsService structureOptionsService;
    private EventBus eb;


    public CompetenceNoteController(EventBus eb) {
        this.eb = eb;
        competencesNotesService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_NOTES_TABLE);
        syntheseService = new DefaultBfcSyntheseService(Competences.COMPETENCES_SCHEMA, Competences.BFC_SYNTHESE_TABLE, eb);
        competenceNiveauFinalService = new DefaultCompetenceNiveauFinalService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCE_NIVEAU_FINAL);
        structureOptionsService = new DefaultStructureOptions();
        devoirsService = new DefaultDevoirService(eb);
    }


    /**
     * Créé une note correspondante à une compétence pour un utilisateur donné
     *
     * @param request
     */
    @Post("/competence/note")
    @ApiDoc("Créé une note correspondante à une compétence pour un utilisateur donné")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateEvaluationWorkflow.class)
    public void create(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_COMPETENCE_NOTE_CREATE, resource -> {
                    if ("Personnel".equals(user.getType())) {
                        Long id_devoir = resource.getLong("id_devoir");
                        devoirsService.getDevoir(id_devoir, handlerDevoir -> {
                            if (handlerDevoir.isLeft()) {
                                log.debug("devoir not found id : " + id_devoir);
                                Renders.badRequest(request, handlerDevoir.left().getValue());
                            } else {
                                JsonObject devoir = handlerDevoir.right().getValue();
                                String id_owner = devoir.getString("owner");
                                competencesNotesService.createCompetenceNote(resource, id_owner, notEmptyResponseHandler(request));
                            }
                        });
                    } else {
                        competencesNotesService.createCompetenceNote(resource, user.getUserId(), notEmptyResponseHandler(request));
                    }
                });
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    /**
     * Met à jour une note relative à une compétence
     *
     * @param request
     */
    @Put("/competence/note")
    @ApiDoc("Met à jour une note relative à une compétence")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessCompetenceNoteFilter.class)
    public void update(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_COMPETENCE_NOTE_UPDATE, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            String id = String.valueOf(resource.getInteger("id"));
                            if (resource.getInteger("evaluation") == -1) {
                                competencesNotesService.delete(id, defaultResponseHandler(request));
                                log.warn("Cette route ne devrait pas etre utilisee avec la valeur -1. Veulliez utiliser la methode de suppression.");
                            } else {
                                competencesNotesService.updateCompetenceNote(id, resource, user, notEmptyResponseHandler(request));
                            }
                        }
                    });
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    /**
     * Supprime une note relative à une compétence
     *
     * @param request
     */
    @Delete("/competence/note")
    @ApiDoc("Supprime une note relative à une compétence")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessCompetenceNoteFilter.class)
    public void delete(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    String id = request.params().get("id");
                    competencesNotesService.delete(id, defaultResponseHandler(request));
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    @Get("/competence/notes/devoir/:idDevoir")
    @ApiDoc("Retourne les compétences notes pour un devoir donné")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessEvaluationFilter.class)
    public void getCompetenceNotesDevoir(final HttpServerRequest request) {
        if (request.params().contains(Field.IDDEVOIR)) {
            Long idDevoir;
            try {
                idDevoir = Long.parseLong(request.params().get(Field.IDDEVOIR));
            } catch (NumberFormatException e) {
                log.error("Error : devoirId must be a long object", e);
                badRequest(request, e.getMessage());
                return;
            }

            competencesNotesService.getCompetencesNotesDevoir(idDevoir, arrayResponseHandler(request));
        } else {
            Renders.badRequest(request, "Invalid parameters");
        }
    }

    @Get("/competence/notes/eleve/:idEleve")
    @ApiDoc("Retourne les compétences notes pour un élève. Filtre possible sur la période avec l'ajout du paramètre idPeriode")
    @SecuredAction(value = "access.suivi.eleve", type = ActionType.WORKFLOW)
    public void getCompetenceNoteEleve(final HttpServerRequest request) {
        if (request.params().contains("idEleve")) {
            String idEleve = request.params().get("idEleve");
            Long idPeriode = null;
            if (request.params().contains("idPeriode")) {
                try {
                    idPeriode = Long.parseLong(request.params().get("idPeriode"));
                } catch (NumberFormatException e) {
                    log.error("Error : idPeriode must be a long object ", e);
                    badRequest(request, e.getMessage());
                    return;
                }
            }

            Long idCycle = null;
            if (request.params().contains("idCycle") && Utils.isCycleNotNull(request.params().get("idCycle"))) {
                try {
                    idCycle = Long.parseLong(request.params().get("idCycle"));
                } catch (NumberFormatException e) {
                    log.error("Error : idCycle must be a long object ", e);
                    badRequest(request, e.getMessage());
                    return;
                }
            }

            boolean isCycle = false;
            if (request.params().contains("isCycle")) {
                isCycle = Boolean.parseBoolean(request.params().get("isCycle"));
            }

            competencesNotesService.getCompetencesNotesEleve(idEleve, idPeriode, idCycle, isCycle, arrayResponseHandler(request));
        } else {
            Renders.badRequest(request, "Invalid parameters");
        }
    }

    @Get("/cycles/eleve/:idEleve")
    @ApiDoc("Récupère les cycles des groupes sur lequels un élève a des devoirs avec compétences notées")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessChildrenParentFilter.class)
    public void getCyclesEleve(final HttpServerRequest request) {
        if (request.params().contains("idEleve")) {
            String idEleve = request.params().get("idEleve");
            competencesNotesService.getCyclesEleve(idEleve, arrayResponseHandler(request));
        } else {
            Renders.badRequest(request, "Invalid parameters");
        }
    }

    @Get("/cycle/eleve/:idEleve")
    @ApiDoc("Retourne l'id du cycle courant à partir d'un idEleve.")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessChildrenParentFilter.class)
    public void getCycleEleve(final HttpServerRequest request) {
        if (request.params().contains("idEleve")) {
            String idEleve = request.params().get("idEleve");
            syntheseService.getIdCycleWithIdEleve(idEleve, new Handler<Either<String, Integer>>() {
                @Override
                public void handle(Either<String, Integer> idCycleObject) {
                    if (idCycleObject.isRight()) {
                        JsonObject idCycle = new JsonObject();
                        idCycle.put("id_cycle", idCycleObject.right().getValue());
                        renderJson(request, idCycle);
                    } else {
                        log.info("idCycle not found");
                        Renders.badRequest(request);
                    }
                }
            });
        } else {
            Renders.badRequest(request, "Invalid parameters");
        }
    }

    @Get("/competence/notes/bilan/conversion")
    @ApiDoc("Retourne les valeurs de converssion entre (Moyenne Note - Evaluation competence) d'un cycle et etablissment donné")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getCompetenceNoteConverssion(final HttpServerRequest request) {
        if (request.params().contains("idEtab") && request.params().contains("idClasse")) {
            String idEtab = request.params().get("idEtab");
            String idClasse = request.params().get("idClasse");

            competencesNotesService.getConversionNoteCompetence(idEtab, idClasse, arrayResponseHandler(request));
        } else {
            Renders.badRequest(request, "Invalid parameters");
        }
    }

    @Get("/competence/notes/classe/:idClasse/:typeClasse")
    @ApiDoc("Retourne les compétences notes pour une classee. Filtre possible sur la période avec l'ajout du paramètre idPeriode")
    @SecuredAction(value = "access.suivi.classe", type = ActionType.WORKFLOW)
    public void getCompetenceNoteClasse(final HttpServerRequest request) {
        Long idPeriode = null;
        if (request.params().contains(Field.IDCLASSE) && request.params().contains(Field.TYPECLASSE)
            && request.params().contains(Field.STRUCTUREID)) {
            final String idClasse = request.params().get(Field.IDCLASSE);
            Integer typeClasse = Integer.valueOf(request.params().get(Field.TYPECLASSE));
            final String idStructure = request.params().get(Field.STRUCTUREID);
            if (request.params().contains(Field.IDPERIODE)) {
                try {
                    idPeriode = Long.parseLong(request.params().get(Field.IDPERIODE));
                } catch (NumberFormatException e) {
                    log.error(" Error : idPeriode must be a long object ", e);
                    badRequest(request, e.getMessage());
                    return;
                }
            }
            callGetCompetenceNote(idClasse, idPeriode, typeClasse, idStructure, request);
        } else {
            Renders.badRequest(request, "Invalid parameters");
        }
    }



    /**
     * Appel de la méthode competencesNotesService.getCompetencesNotesClasse
     * à partir des éléments en paramètre
     *
     * @param idEleves
     * @param idPeriode
     * @param request
     */
    private void callCompetencesNotesService(List<String> idEleves, Long idPeriode, Boolean isSkillAverage, HttpServerRequest request) {
        if (null != idEleves
                && !idEleves.isEmpty()) {
            competencesNotesService.getMaxOrAverageCompetencesNotesClasse(idEleves, idPeriode, isSkillAverage,
                    event ->{
                if(event .isLeft()) {
                    JsonObject error = new JsonObject().put(Field.ERROR, event.left().getValue());
                    log.error("[CompetenceNoteController] : callCompetencesNotesService " + event.left().getValue());
                    Renders.renderError(request, error);
                    return;
                }
                if(!Boolean.TRUE.equals(isSkillAverage)) Renders.renderJson(request, event.right().getValue());
                else {
                    JsonArray competencesNoteWithDoubleEvaluation = new JsonArray();
                   event.right().getValue().stream().forEach(comp -> {
                       JsonObject competenceNoteEleve = (JsonObject)comp;
                       Double evaluation = HomeworkUtils.safeGetDouble(competenceNoteEleve,Field.EVALUATION);
                       competenceNoteEleve.put(Field.EVALUATION, evaluation);
                       competencesNoteWithDoubleEvaluation.add(competenceNoteEleve);
                   });
                   Renders.renderJson(request,competencesNoteWithDoubleEvaluation);

                }
            });
        }
    }

    @Post("/competence/notes")
    @ApiDoc("Créer une liste de compétences notes pour un devoir donné")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateEvaluationWorkflow.class)
    public void createCompetencesNotesDevoir(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                RequestUtils.bodyToJson(request, resource -> {
                    if ("Personnel".equals(user.getType())) {
                        JsonArray datas = resource.getJsonArray("data");
                        if (!datas.isEmpty()) {
                            Long id_devoir = datas.getJsonObject(0).getLong("id_devoir");
                            devoirsService.getDevoir(id_devoir, handlerDevoir -> {
                                if (handlerDevoir.isLeft()) {
                                    log.debug("devoir not found id : " + id_devoir);
                                    Renders.badRequest(request, handlerDevoir.left().getValue());
                                } else {
                                    JsonObject devoir = handlerDevoir.right().getValue();
                                    String id_owner = devoir.getString("owner");
                                    competencesNotesService.createCompetencesNotesDevoir(resource.getJsonArray("data"),
                                            id_owner, arrayResponseHandler(request));
                                }
                            });
                        }

                    } else {
                        competencesNotesService.createCompetencesNotesDevoir(resource.getJsonArray("data"),
                                user.getUserId(), arrayResponseHandler(request));
                    }
                });
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }

        });
    }

    @Put("/competence/notes")
    @ApiDoc("Met à jour une liste de compétences notes pour un devoir donné")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessCompetenceNoteFilter.class)
    public void updateCompetencesNotesDevoir(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject resource) {
                competencesNotesService.updateCompetencesNotesDevoir(resource.getJsonArray("data"), arrayResponseHandler(request));
            }
        });
    }

    @Delete("/competence/notes")
    @ApiDoc("Supprime une liste de compétences notes pour un devoir donné")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessCompetenceNoteFilter.class)
    public void deleteCompetencesNotesDevoir(final HttpServerRequest request) {
        List<String> ids = request.params().getAll("id");

        if (ids == null || ids.size() == 0) {
            log.error("Error : one id must be present");
            badRequest(request);
            return;
        }

        JsonArray oIdsJsonArray = new fr.wseduc.webutils.collections.JsonArray();
        try {
            for (int i = 0; i < ids.size(); i++) {
                oIdsJsonArray.add(Long.parseLong(ids.get(i)));
            }
        } catch (NumberFormatException e) {
            log.error("Error : id must be a long object", e);
            badRequest(request);
        }

        competencesNotesService.dropCompetencesNotesDevoir(oIdsJsonArray, arrayResponseHandler(request));
    }

    private void callGetCompetenceNote(String idClasse, Long idPeriode, Integer typeClasse, String idStructure,
                                       final HttpServerRequest request) {
        new DefaultUtilsService(this.eb).studentIdAvailableForPeriode(idClasse, idPeriode, typeClasse, event -> {
            if (event.isRight()) {
                JsonArray queryResult = event.right().getValue();
                List<String> idEleves = new ArrayList<>();

                if (queryResult != null) {
                    for (int i = 0; i < queryResult.size(); i++) {
                        idEleves.add(queryResult.getString(i));
                    }
                }
                structureOptionsService.getIsAverageSkills(idStructure, responseOption -> {

                    if(responseOption.isLeft()) {
                        Renders.notFound(request,"Impossible de recuperer le type de calcul des competences notes \n"
                                + responseOption.left().getValue());
                        log.error("buildBFC : getIsAverageSkills : " + responseOption.left().getValue());
                        return;
                    }
                    Boolean isSkillAverage = responseOption.right().getValue().getBoolean(Field.IS_AVERAGE_SKILLS);

                    callCompetencesNotesService(idEleves, idPeriode, isSkillAverage, request);

                });
            } else {
                Renders.notFound(request,
                        "Error :can not get CompNotes of groupe : " + idClasse);
                log.error("Error :can not get compNotes of groupe : " + idClasse);
            }
        });

    }

    @Post("competence/note/niveaufinal")
    @ApiDoc("Crée ou met à jour le niveau final pour une compétence, un élève, une matière et une classe")
    @SecuredAction("save.competence.niveau.final")
    public void saveCompetenceNiveauFinal(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos userInfos) {
                if (userInfos != null) {
                    RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_CREATE_COMPETENCE_NIVEAU_FINAL,
                            new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject competenceNiveauFinal) {
                                    if (competenceNiveauFinal.getInteger("id_periode") != null)
                                        competenceNiveauFinalService.setNiveauFinal(competenceNiveauFinal,
                                                defaultResponseHandler(request));
                                    else
                                        competenceNiveauFinalService.setNiveauFinalAnnuel(competenceNiveauFinal,
                                                defaultResponseHandler(request));
                                }
                            });
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }
}
