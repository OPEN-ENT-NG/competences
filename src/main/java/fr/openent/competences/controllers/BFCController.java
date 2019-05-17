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
import fr.openent.competences.security.AccessBFCFilter;
import fr.openent.competences.security.AccessControleContinuFilter;
import fr.openent.competences.security.CanUpdateBFCSyntheseRight;
import fr.openent.competences.service.*;
import fr.openent.competences.service.impl.*;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.*;

/**
 * Created by vogelmt on 29/03/2017.
 */
public class BFCController extends ControllerHelper {
    /**
     * Déclaration des services
     */
    private final BFCService bfcService;
    private final BfcSyntheseService syntheseService;
    private final EnseignementComplementService enseignementComplement;
    private final LanguesCultureRegionaleService languesCultureRegionaleService;
    private final EleveEnseignementComplementService eleveEnseignementComplement;
    private final NiveauEnsComplementService niveauEnsComplementService;

    public BFCController(EventBus eb) {
        this.eb = eb;
        bfcService = new DefaultBFCService(eb);
        syntheseService = new DefaultBfcSyntheseService(Competences.COMPETENCES_SCHEMA, Competences.BFC_SYNTHESE_TABLE, eb);
        enseignementComplement = new DefaultEnseignementComplementService(Competences.COMPETENCES_SCHEMA, Competences.ENSEIGNEMENT_COMPLEMENT);
        languesCultureRegionaleService = new DefaultLanguesCultureRegionaleService(Competences.COMPETENCES_SCHEMA, Competences.LANGUES_CULTURE_REGIONALE);
        eleveEnseignementComplement = new DefaultEleveEnseignementComplementService(Competences.COMPETENCES_SCHEMA, Competences.ELEVE_ENSEIGNEMENT_COMPLEMENT);
        niveauEnsComplementService = new DefaultNiveauEnsComplement(Competences.COMPETENCES_SCHEMA,Competences.NIVEAU_ENS_COMPLEMENT);
    }


    /**
     * Créer un BFC avec les données passées en POST
     *
     * @param request
     */
    @Post("/bfc")
    @ApiDoc("Créer un BFC")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessBFCFilter.class)
    public void create(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_BFC_CREATE, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            bfcService.checkHeadTeacherForBFC(user, resource.getString("id_eleve"),
                                    resource.getString("id_etablissement"),
                                    new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean event) {
                                            if(event) {
                                                bfcService.createBFC(resource, user, notEmptyResponseHandler(request));
                                            }
                                            else {
                                                Renders.unauthorized(request);
                                            }
                                        }
                                    });
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
     * Modifie un BFC avec les données passées en PUT
     *
     * @param request
     */
    @Put("/bfc")
    @ApiDoc("Modifie un BFC")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessBFCFilter.class)
    public void update(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_BFC_UPDATE,
                            new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            bfcService.checkHeadTeacherForBFC(user, resource.getString("id_eleve"),
                                    resource.getString("id_etablissement"),
                                    new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean event) {
                                            if(event) {
                                                bfcService.updateBFC(resource, user, defaultResponseHandler(request));
                                            }
                                            else {
                                                Renders.unauthorized(request);
                                            }
                                        }
                                    });
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
     * Supprime l'appreciation passée en paramètre
     *
     * @param request
     */
    @Delete("/bfc")
    @ApiDoc("Supprimer un bfc donnée")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessBFCFilter.class)
    public void delete(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {

                    Long idBFC;
                    try {
                        idBFC = Long.parseLong(request.params().get("idDomaine"));
                    } catch (NumberFormatException e) {
                        log.error("Error : idDomaine  must be a long object", e);
                        badRequest(request, e.getMessage());
                        return;
                    }

                    String idEleve = request.params().get("idEleve");
                    String idEtablissement = request.params().get(Competences.ID_ETABLISSEMENT_KEY);

                    bfcService.checkHeadTeacherForBFC(user, idEleve, idEtablissement,
                            new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean event) {
                                    if(event) {
                                        bfcService.deleteBFC(idBFC, idEleve, user, defaultResponseHandler(request));
                                    }
                                    else {
                                        Renders.unauthorized(request);
                                    }
                                }
                            });


                } else {
                    unauthorized(request);
                }
            }
        });
    }

    @Get("/bfc/eleve/:idEleve")
    @ApiDoc("Retourne les bfcs notes pour un élève.")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getBFCsEleve(final HttpServerRequest request) {
        if (request.params().contains("idEleve")
                && request.params().contains("idEtablissement")) {
            String idEleve = request.params().get("idEleve");
            String idEtablissement = request.params().get("idEtablissement");
//            final Integer idCycle = Integer.parseInt(request.params().get("idCycle"));
//            bfcService.getBFCsByEleve(new String[]{idEleve}, idEtablissement, new Long(idCycle), arrayResponseHandler(request));
            if (request.params().contains("idCycle")) {
                bfcService.getBFCsByEleve(new String[]{idEleve}, idEtablissement, Long.parseLong(request.params().get("idCycle")), arrayResponseHandler(request));
            } else {
                syntheseService.getIdCycleWithIdEleve(idEleve, new Handler<Either<String, Integer>>() {
                    @Override
                    public void handle(Either<String, Integer> idCycle) {
                        log.debug("id_cycle : "+idCycle.right().getValue());
                        if (idCycle.isRight()) {
                            bfcService.getBFCsByEleve(new String[]{idEleve}, idEtablissement, new Long(idCycle.right().getValue()), arrayResponseHandler(request));
                        } else {
                            log.info("idCycle not found");
                            Renders.badRequest(request);
                        }
                    }
                });
            }
        } else {
            Renders.badRequest(request, "Invalid parameters");
        }
    }

    //La Synthèse du Bilan de fin de cycle

    /**
     * Créer une Synthese avec les données passées en POST
     *
     * @param request
     */
    @Post("/BfcSynthese")
    @ApiDoc("Créer une Synthese du BFC")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CanUpdateBFCSyntheseRight.class)
    public void createSynthese(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_BFCSYNTHESE_CREATE, new Handler<JsonObject>() {
                        @Override
                        public void handle(final JsonObject synthese) {
                            syntheseService.getIdCycleWithIdEleve(synthese.getString("id_eleve"), new Handler<Either<String, Integer>>() {
                                @Override
                                public void handle(Either<String, Integer> idCycle) {
                                    if (idCycle.isRight()) {
                                        JsonObject syntheseCycle = new JsonObject()
                                                .put("id_eleve", synthese.getString("id_eleve"))
                                                .put("owner", user.getUserId())
                                                .put("id_cycle", idCycle.right().getValue())
                                                .put("texte", synthese.getString("texte"));
                                        syntheseService.createBfcSynthese(syntheseCycle, user, notEmptyResponseHandler(request));
                                    } else {
                                        log.debug("idCycle not found");
                                        Renders.badRequest(request);
                                    }
                                }
                            } );
                        }
                    });
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }


    @Get("/BfcSynthese")
    @ApiDoc("récupére une Synthese du BFC pour un élève")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getSynthese(final HttpServerRequest request) {

        if (request.params().contains("idEleve")) {
            final String idEleve = request.params().get("idEleve");
//            final Integer idCycle = Integer.parseInt(request.params().get("idCycle"));
//            syntheseService.getBfcSyntheseByEleve(idEleve, idCycle, defaultResponseHandler(request));
            if (request.params().contains("idCycle")) {
                syntheseService.getBfcSyntheseByEleve(idEleve, Integer.parseInt(request.params().get("idCycle")), defaultResponseHandler(request));
            } else {
                syntheseService.getIdCycleWithIdEleve(idEleve, new Handler<Either<String, Integer>>() {
                    @Override
                    public void handle(Either<String, Integer> idCycle) {
                        log.debug("id_cycle : "+idCycle.right().getValue());
                        if (idCycle.isRight()) {
                            syntheseService.getBfcSyntheseByEleve(idEleve, idCycle.right().getValue(), defaultResponseHandler(request));
                        } else {
                            log.info("idCycle not found");
                            Renders.badRequest(request);
                        }
                    }
                });
            }
        } else {
            log.debug("idEleve not found");
            Renders.badRequest(request);
        }
    }

    @Put("/BfcSynthese")
    @ApiDoc("Met à jour la synthèse du bilan de compétence pour un élève")
    @SecuredAction(value = Competences.CAN_UPDATE_BFC_SYNTHESE_RIGHT, type = ActionType.WORKFLOW)
    public void updateSynthese(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos userInfos) {
                if(userInfos != null){
                    if(request.params().contains("id")){
                        RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_BFCSYNTHESE_CREATE, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject synthese) {
                                if(synthese.getString("texte").isEmpty()) {
                                    syntheseService.deleteBfcSynthese(request.params().get("id"), notEmptyResponseHandler(request));
                                } else {
                                    syntheseService.updateBfcSynthese(request.params().get("id"), synthese, notEmptyResponseHandler(request));
                                }
                            }
                        });
                    }else{
                        log.debug("idbfcSynthese not found");
                        Renders.badRequest(request);
                    }

                }
            }
        });
    }
    //Les enseignements de complément pour le cycle 4 seulement

    @Get("/ListEnseignementComplement")
    @ApiDoc("Récupère la liste des enseignements ")
    @SecuredAction(value="",type = ActionType.AUTHENTICATED)
    public void getEnseignementsDeComplement(final  HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos userInfos) {
                if(userInfos!=null){
                    Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                    enseignementComplement.getEnseignementsComplement(handler);
                }else{
                    Renders.unauthorized(request);
                }
            }
        });
    }

    @Get("/langues/culture/regionale/list")
    @ApiDoc("Récupère la liste des enseignements ")
    @SecuredAction(value="",type = ActionType.AUTHENTICATED)
    public void getLanguesCultureRegionale(final  HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos userInfos) {
                if(userInfos!=null){
                    Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                    languesCultureRegionaleService.getLanguesCultureRegionaleService(handler);
                }else{
                    Renders.unauthorized(request);
                }
            }
        });
    }

    @Get("/niveaux/enseignement/complement/list")
    @ApiDoc("Récupère la liste des enseignements ")
    @SecuredAction(value="",type = ActionType.AUTHENTICATED)
    public void getNiveauxEnsComplement(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos userInfos) {
                if(userInfos != null){
                    niveauEnsComplementService.getNiveauEnsComplement(arrayResponseHandler(request));

                }else{
                    Renders.unauthorized(request);
                }
            }
        });
    }



    @Post("/CreateNiveauEnsCpl")
    @ApiDoc("crée l'enseignement de complement pour un élève")
    @SecuredAction(value="",type=ActionType.AUTHENTICATED)
    public void createNiveauEnsCpl(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos userInfos) {
//                if(userInfos!=null && userInfos.getFunctions().containsKey("ENS")){
                RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_NIVEAUENSCPL_CREATE, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject data) {
                        syntheseService.getIdCycleWithIdEleve(data.getString("id_eleve"), new Handler<Either<String, Integer>>() {
                            @Override
                            public void handle(Either<String, Integer> idCycle) {
                                if (idCycle.isRight()) {
                                    data.put("id_cycle", idCycle.right().getValue());
                                    eleveEnseignementComplement.createEnsCplByELeve(data, userInfos, notEmptyResponseHandler(request));
                                } else {
                                    log.info("idCycle not found");
                                    Renders.badRequest(request);
                                }
                            }
                        });
                    }
                });
//                }else{
//                    Renders.unauthorized(request);
//                }
            }
        });
    }

    @Put("/UpdateNiveauEnsCpl")
    @ApiDoc("met à jour niveau d'enseignement complément")
    @SecuredAction(value="",type=ActionType.AUTHENTICATED)
    public void updateNiveauEnsCpl(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos userInfos) {
//                if(userInfos!=null && userInfos.getFunctions().containsKey("ENS")){
                RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_NIVEAUENSCPL_CREATE, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject data) {
                        final Integer id = Integer.parseInt(request.params().get("id"));

                        eleveEnseignementComplement.updateEnsCpl(id,data,defaultResponseHandler(request));
                    }
                });
//                }else{
//                    Renders.unauthorized(request);
//                }
            }
        });
    }

    @Get("/GetNiveauEnsCpl")
    @ApiDoc("Récupère l'enseignement de complément pour un élève")
    @SecuredAction(value="", type=ActionType.AUTHENTICATED)
    public void getNiveauEnsCplByEleve(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos userInfos) {
                if(userInfos!=null)  {
                    final String idEleve = request.params().get("idEleve");
//                    final Long idCycle = Long.parseLong(request.params().get("idCycle"));
//                    eleveEnseignementComplement.getNiveauEnsCplByEleve(idEleve, idCycle, defaultResponseHandler(request));
                    if(request.params().contains("idCycle")){
                        eleveEnseignementComplement.getNiveauEnsCplByEleve(idEleve, Long.parseLong(request.params().get("idCycle")), defaultResponseHandler(request));
                    } else {
                        syntheseService.getIdCycleWithIdEleve(idEleve, new Handler<Either<String, Integer>>() {
                            @Override
                            public void handle(Either<String, Integer> idCycle) {
                                if (idCycle.isRight()) {
                                    eleveEnseignementComplement.getNiveauEnsCplByEleve(idEleve, new Long(idCycle.right().getValue()), defaultResponseHandler(request));
                                } else {
                                    log.info("idCycle not found");
                                    Renders.badRequest(request);
                                }
                            }
                        });
                    }
                }
            }
        });
    }


    @Put("/bfc/visibility/structures/:structureId/:idVisibility/:visible")
    @ApiDoc("Défini la visibilité pour un établissement donné de la moyenne calculée sur le BFC")
    @SecuredAction(value="competences.set.visibility.bfc.average", type = ActionType.WORKFLOW)
    public void setVisibility(final HttpServerRequest request) {
        // visibility values
        // 0 : caché pour tout le monde
        // 1 : caché pour les enseignants
        // 2 : visible pour tous
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {

                if(null != user  && request.params().contains("structureId")) {
                    final String structureId = request.params().get("structureId");
                    final Integer visible = Integer.valueOf(request.params().get("visible"));
                    final Integer idVisibility = Integer.valueOf(request.params().get("idVisibility"));
                    if(user.getStructures().contains(structureId)) {
                        Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                        bfcService.setVisibility(structureId, idVisibility,user, visible, handler);
                    }
                    else {
                        unauthorized(request);
                    }
                }else{
                    badRequest(request);
                }
            }
        });
    }


    @Get("/bfc/visibility/structures/:structureId/:idVisibility")
    @ApiDoc("Recupere la visibilité un établissement donné de la moyenne calculée sur le BFC")
    @SecuredAction(value="", type=ActionType.AUTHENTICATED)
    public void getVisibility(final HttpServerRequest request) {
        // visibility values
        // 0 : caché pour tout le monde
        // 1 : caché pour les enseignants
        // 2 : visible pour tous
        //idVisibility = 1 moyBFC
        //idVisibility = 2 baremeDNB
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null && request.params().contains("structureId")) {
                    final String structureId = request.params().get("structureId");
                    final Integer idVisibility = (request.params().contains("idVisibility"))?
                            Integer.valueOf(request.params().get("idVisibility")): null;
                    Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                    bfcService.getVisibility(structureId, idVisibility, user,handler);
                }else{
                    badRequest(request);
                }
            }
        });
    }

    @Get("/bfc/bareme/brevet/eleves")
    @ApiDoc("Récupère la moyenne des contrôles continus(obtenue à partir des niveaux du bfc) en fct de la dispense des domaines")
    @SecuredAction(value="",type= ActionType.RESOURCE)
    @ResourceFilter(AccessControleContinuFilter.class)
    public void getMaxBaremeMoyBaremeBrevet(final HttpServerRequest request){
        final List<String> idsClasses = request.params().contains("idClasse")?request.params().getAll("idClasse"):null;
        final Long idTypePeriode = (!request.params().get("idTypePeriode").equals("null")) ? Long.valueOf(request.params().get("idTypePeriode")) : null;
        final Boolean isCycle = Boolean.valueOf(request.params().get("isCycle"));
        final Long idCycle = (isCycle)? Long.valueOf(request.params().get("idCycle")):null;
        if(idsClasses != null) {
            bfcService.getMoyenneControlesContinusBrevet(eb, idsClasses, idTypePeriode, isCycle, idCycle,
                    arrayResponseHandler(request));
        }else{
            log.debug("eleves bareme brevet  not found");
            Renders.badRequest(request);
        }

    }

}
