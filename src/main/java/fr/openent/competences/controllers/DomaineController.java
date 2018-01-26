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
import fr.openent.competences.service.CompetencesService;
import fr.openent.competences.service.DomainesService;
import fr.openent.competences.service.EnseignementService;
import fr.openent.competences.service.impl.DefaultCompetencesService;
import fr.openent.competences.service.impl.DefaultDomaineService;
import fr.openent.competences.service.impl.DefaultEnseignementService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.controller.ControllerHelper;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

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

    public DomaineController() {
        enseignementService = new DefaultEnseignementService(Competences.COMPETENCES_SCHEMA, Competences.ENSEIGNEMENTS_TABLE);
        competencesService = new DefaultCompetencesService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_TABLE);
        domainesService = new DefaultDomaineService(Competences.COMPETENCES_SCHEMA, Competences.DOMAINES_TABLE);
    }

    /**
     * Récupère la liste des enseignements
     * @param request
     */
    @Get("/domaines")
    @ApiDoc("Recupère l'arbre des domaines pour un cycle donné.")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getArbreDomaines(final HttpServerRequest request){
        final JsonArray oArbreDomainesArray = new JsonArray();
        final String idClasse = request.params().get("idClasse");

        // 1 - Chargement des domaines ordonnés selon l'arbre recursif
        domainesService.getArbreDomaines(idClasse, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.right().isRight()) {

                    // La liste des domaines ordonnés
                    final JsonArray oDomainesArray = event.right().getValue();

                    // 2 - Chargement de toutes les competences evaluables du cycle
                    competencesService.getCompetencesItem(idClasse, new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> event) {
                                    if (event.right().isRight()) {

                                        // La liste des competences evaluables du cycle (feuilles)
                                        JsonArray oCompetencesItemArray = event.right().getValue();


                                        // 3 - Positionnement des compétences sur les domaines
                                        for (int i = 0; i < oCompetencesItemArray.size(); i++) {

                                            JsonObject oCompetenceItem = oCompetencesItemArray.get(i);

                                            for (int j = 0; j < oDomainesArray.size(); j++) {
                                                JsonObject oDomaine = oDomainesArray.get(j);

                                                if(oDomaine.getLong("id")
                                                        .equals(oCompetenceItem.getLong("id_domaine"))
                                                        && oDomaine.getLong("id_cycle")
                                                        .equals(oCompetenceItem.getLong("id_cycle"))) {
                                                    // initialisation de la liste des competences du
                                                    // domaine si cela n'a pas encore été fait
                                                    if (oDomaine.getArray("competences") == null) {
                                                        oDomaine.putArray("competences", new JsonArray());
                                                    }

                                                    oDomaine.getArray("competences")
                                                            .addObject(oCompetenceItem);
                                                }

                                            }

                                        }


                                        // 4 - Construction de l'arbre des domaines à partir de la liste ordonnée
                                        for (int i = 0; i < oDomainesArray.size(); i++) {
                                            JsonObject oDomaineAinserer = oDomainesArray.get(i);

                                            // si c'est un domaine racine on l'ajoute à la suite dans notre arbe
                                            if(oDomaineAinserer.getInteger("niveau").intValue() == 1) {
                                                oArbreDomainesArray.addObject(oDomaineAinserer);
                                            } else {

                                                // sinon cela veut dire que le domaine en cours
                                                // de parcous est un sous domaine du
                                                // dernier domaine racine ajouté
                                                JsonObject oDomaineRacine = oArbreDomainesArray
                                                        .get(oArbreDomainesArray.size() - 1);
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
            if (poDomaineRacine.getArray("domaines") == null) {
                poDomaineRacine.putArray("domaines", new JsonArray());
            }

            poDomaineRacine.getArray("domaines").addObject(poDomaineAinserer);
            return true;
        } else {
            // sinon on doit rechercher dans les sous niveaux de l'arborescence
            if (poDomaineRacine.getArray("domaines") == null) {
                log.error("Aucun point d'insertion trouve pour le domaine : "+ poDomaineAinserer.getString("id"));
                return false;
            } else {

                for (Object oSousDomaine : poDomaineRacine.getArray("domaines")) {
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

}
