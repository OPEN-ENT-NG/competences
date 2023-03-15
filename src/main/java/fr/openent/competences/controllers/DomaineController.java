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
import fr.openent.competences.security.AccessChildrenParentFilter;
import fr.openent.competences.security.AccessIfMyStructure;
import fr.openent.competences.security.CreateDispenseDomaineEleveFilter;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.CompetencesService;
import fr.openent.competences.service.DispenseDomaineEleveService;
import fr.openent.competences.service.DomainesService;
import fr.openent.competences.service.EnseignementService;
import fr.openent.competences.service.impl.DefaultCompetencesService;
import fr.openent.competences.service.impl.DefaultDispenseDomaineEleveService;
import fr.openent.competences.service.impl.DefaultDomaineService;
import fr.openent.competences.service.impl.DefaultEnseignementService;
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
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DomaineController extends ControllerHelper {


    private static final Logger log = LoggerFactory.getLogger(DomaineController.class);

    /**
     * Déclaration des services
     */
    private final EnseignementService enseignementService;
    private final CompetencesService competencesService;
    private final DomainesService domainesService;
    private final DispenseDomaineEleveService dispenseDomaineEleveService;

    public DomaineController(EventBus eb) {
        enseignementService = new DefaultEnseignementService(Competences.COMPETENCES_SCHEMA, Competences.ENSEIGNEMENTS_TABLE);
        competencesService = new DefaultCompetencesService(eb);
        domainesService = new DefaultDomaineService(Competences.COMPETENCES_SCHEMA, Competences.DOMAINES_TABLE);
        dispenseDomaineEleveService = new DefaultDispenseDomaineEleveService(Competences.COMPETENCES_SCHEMA,Competences.DISPENSE_DOMAINE_ELEVE);
    }

    /**
     * Récupère la liste des enseignements
     * @param request
     */
    @Get("/domaines")
    @ApiDoc("Recupère l'arbre des domaines pour un cycle donné.")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    public void getArbreDomaines(final HttpServerRequest request){
        final JsonArray oArbreDomainesArray = new fr.wseduc.webutils.collections.JsonArray();
        final String idClasse = request.params().get("idClasse");
        final String idEleve = request.params().contains("idEleve") ? request.params().get("idEleve") : null;
        final Long idCycle = Utils.isCycleNotNull(request.params().get("idCycle")) ?
                Long.parseLong(request.params().get("idCycle")) : null;
        final String idStructure = request.params().get("idStructure");

        // 1 - Chargement des domaines ordonnés selon l'arbre recursif
        domainesService.getArbreDomaines(idClasse, idEleve, idCycle, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.right().isRight()) {
                    // La liste des domaines ordonnés
                    final JsonArray oDomainesArray = event.right().getValue();

                    // 2 - Chargement de toutes les competences evaluables du cycle
                    competencesService.getCompetencesItem(idStructure, idClasse, idCycle,
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> event) {
                                    if (event.isRight()) {
                                        // La liste des competences evaluables du cycle (feuilles)
                                        JsonArray oCompetencesItemArray = event.right().getValue();

                                        // 3 - Positionnement des compétences sur les domaines
                                        for (int i = 0; i < oCompetencesItemArray.size(); i++) {
                                            JsonObject oCompetenceItem = oCompetencesItemArray.getJsonObject(i);

                                            for (int j = 0; j < oDomainesArray.size(); j++) {
                                                JsonObject oDomaine = oDomainesArray.getJsonObject(j);

                                                if(oDomaine.getLong("id")
                                                        .equals(oCompetenceItem.getLong("id_domaine"))
                                                        && oDomaine.getLong("id_cycle")
                                                        .equals(oCompetenceItem.getLong("id_cycle"))) {
                                                    // initialisation de la liste des competences du
                                                    // domaine si cela n'a pas encore été fait
                                                    if (oDomaine.getJsonArray("competences") == null) {
                                                        oDomaine.put("competences", new fr.wseduc.webutils.collections.JsonArray());
                                                    }

                                                    oDomaine.getJsonArray("competences")
                                                            .add(oCompetenceItem);
                                                }
                                            }
                                        }

                                        // 4 - Construction de l'arbre des domaines à partir de la liste ordonnée
                                        for (int i = 0; i < oDomainesArray.size(); i++) {
                                            JsonObject oDomaineAinserer = oDomainesArray.getJsonObject(i);

                                            // si c'est un domaine racine on l'ajoute à la suite dans notre arbe
                                            if(oDomaineAinserer.getInteger("niveau").intValue() == 1) {
                                                oArbreDomainesArray.add(oDomaineAinserer);
                                            } else {
                                                // sinon cela veut dire que le domaine en cours
                                                // de parcous est un sous domaine du
                                                // dernier domaine racine ajouté
                                                JsonObject oDomaineRacine = oArbreDomainesArray
                                                        .getJsonObject(oArbreDomainesArray.size() - 1);
                                                ajouterDomaineSousArbre(oDomaineRacine, oDomaineAinserer);
                                            }
                                        }
                                        Renders.renderJson(request, oArbreDomainesArray);
                                    } else {
                                        leftToResponse(request, event.left());
                                    }
                                }
                            });
                } else {
                    leftToResponse(request, event.left());
                }
            }
        });
    }

    /**
     * Ajoute un domaine au bon endroit dans l'arborescence d'un autre domaine
     *
     * @param poDomaineRacine le domaine racine de référence
     * @param poDomaineAinserer le domaine à insérer dans l'arbre du domaine racine
     *
     * @return true en cas d'insertion réussie, false sinon
     */
    private boolean ajouterDomaineSousArbre(JsonObject poDomaineRacine, JsonObject poDomaineAinserer) {
        // si l'identifiant du domaine racine correspond à l'id_parent du domaine à insérer
        // c'est qu'on est au bon endroit pour effectuer l'insertion
        if(poDomaineRacine.getLong("id").equals(poDomaineAinserer.getLong("id_parent"))) {

            // initialisation de la liste des sous-domaine si cela n'a pas encore été fait
            if (poDomaineRacine.getJsonArray("domaines") == null) {
                poDomaineRacine.put("domaines", new fr.wseduc.webutils.collections.JsonArray());
            }

            poDomaineRacine.getJsonArray("domaines").add(poDomaineAinserer);
            return true;
        } else {
            // sinon on doit rechercher dans les sous niveaux de l'arborescence
            if (poDomaineRacine.getJsonArray("domaines") == null) {
                log.error("Aucun point d'insertion trouve pour le domaine : "+ poDomaineAinserer.getString("id"));
                return false;
            } else {

                for (Object oSousDomaine : poDomaineRacine.getJsonArray("domaines")) {
                    boolean bIsInsere = ajouterDomaineSousArbre((JsonObject) oSousDomaine, poDomaineAinserer);

                    // Si insertion effectuee, on stop le traitement
                    if(bIsInsere) {
                        return true;
                    }
                }
            }

        }
        return false;
    }

    @Delete("/domaine/dispense/eleve/:idDomaine/:idEleve/:idEtablissement")
    @ApiDoc("Delete the domaine's exemption for a student")
    @ResourceFilter(CreateDispenseDomaineEleveFilter.class)
    public void deleteDispenseDomaineEleve(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user!=null) {
                    try {
                        String idEleve = request.params().get("idEleve");
                        Integer idDomaine = Integer.parseInt(request.params().get("idDomaine"));
                        String idEtablissement = request.params().get(Competences.ID_ETABLISSEMENT_KEY);
                        WorkflowActionUtils.hasHeadTeacherRight(user, null, null,
                                null, new JsonArray().add(idEleve), eb, idEtablissement,
                                new Handler<Either<String, Boolean>>() {
                                    @Override
                                    public void handle(Either<String, Boolean> event) {
                                        Boolean isHeadTeacher;
                                        if(event.isLeft()) {
                                            log.error("[deleteDispenseDomaineEleve]: failed To calcul HeadTeacher");
                                            isHeadTeacher = false;
                                        }
                                        else {
                                            isHeadTeacher = event.right().getValue();
                                        }


                                        if(isHeadTeacher ||
                                                new WorkflowActionUtils()
                                                .hasRight(user,
                                                        WorkflowActions.CREATE_DISPENSE_DOMAINE_ELEVE.toString())) {
                                            dispenseDomaineEleveService.deleteDispenseDomaineEleve(idEleve, idDomaine,
                                                    defaultResponseHandler(request));
                                        }
                                        else {
                                            Renders.unauthorized(request);
                                        }
                                    }
                                });

                    } catch (ClassCastException e) {
                        log.error("An Error occured when casting domaine id");
                    }
                }else {
                    log.error("User not found in session.");
                    Renders.unauthorized(request);
                    badRequest(request);
                }
            }
        });
    }

    @Post("/domaine/dispense/eleve")
    @ApiDoc("Create an exemption for a domain and a student")
    @SecuredAction(value="create.dispense.domaine.eleve", type=ActionType.WORKFLOW)
    public void createDispenseDomaineEleve(final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_DISPENSEDOMAINE_ELEVE_CREATE, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject dispenseDomaineEleve) {
                dispenseDomaineEleveService.createDispenseDomaineEleve(dispenseDomaineEleve, defaultResponseHandler(request));
            }
        });
    }
}
