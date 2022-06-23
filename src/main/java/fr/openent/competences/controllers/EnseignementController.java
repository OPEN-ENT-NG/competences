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
import fr.openent.competences.service.CompetencesService;
import fr.openent.competences.service.EnseignementService;
import fr.openent.competences.service.impl.DefaultCompetencesService;
import fr.openent.competences.service.impl.DefaultEnseignementService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.controller.ControllerHelper;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.Collator;
import java.util.*;

import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class EnseignementController extends ControllerHelper {


    /**
     * Déclaration des services
     */
    private final EnseignementService enseignementService;
    private final CompetencesService competencesService;

    public EnseignementController(EventBus eb) {
        enseignementService = new DefaultEnseignementService(Competences.COMPETENCES_SCHEMA, Field.ENSEIGNEMENTS_TABLE);
        competencesService = new DefaultCompetencesService(eb);
    }

    /**
     * Récupère la liste des enseignements et ses sous-compétences
     * Trie les enseignements par nom
     * @param request
     */
    @Get("/enseignements")
    @ApiDoc("Recupère la liste des enseignements")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getEnseignements(final HttpServerRequest request){
        final JsonObject _datas = new JsonObject();
        enseignementService.getEnseignements(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.right().isRight()) {
                    String idClasse = null;
                    if (request.params().contains("idClasse")) {
                        idClasse = request.params().get("idClasse");
                    }

                    final String idCycle = request.params().get("idCycle");
                    final String idStructure = request.params().get("idStructure");
                    if (null != idStructure) {
                        idClasse = null;
                    }

                    JsonArray sortedJsonArray = new fr.wseduc.webutils.collections.JsonArray();
                    List<JsonObject> jsonList = new ArrayList<JsonObject>();
                    for (int i = 0; i < event.right().getValue().size(); i++) {
                        jsonList.add((JsonObject) event.right().getValue().getJsonObject(i));
                    }

                    Collections.sort(jsonList, new Comparator<JsonObject>() {
                        private static final String KEY_NAME = "nom";

                        @Override
                        public int compare(JsonObject a, JsonObject b) {
                            Collator frCollator = Collator.getInstance(Locale.FRENCH);
                            return frCollator.compare(a.getString(KEY_NAME), b.getString(KEY_NAME));
                        }
                    });

                    for (JsonObject obj : jsonList) {
                        sortedJsonArray.add(obj);
                    }

                    _datas.put("enseignements", sortedJsonArray);
                    final String finalIdClasse = idClasse;


                    competencesService.getCompetencesByLevel(idStructure, "id_type", "1", finalIdClasse, idCycle, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> eventCompetences_1) {
                            if (eventCompetences_1.right().isRight()) {
                                _datas.put("_competences_1", eventCompetences_1.right().getValue());
                                competencesService.getCompetencesByLevel(idStructure, "id_type", "2", finalIdClasse, idCycle, new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> eventCompetences_2) {
                                        if (eventCompetences_2.right().isRight()) {
                                            _datas.put("_competences_2", eventCompetences_2.right().getValue());
                                            JsonArray result = new fr.wseduc.webutils.collections.JsonArray();
                                            JsonArray enseignements = _datas.getJsonArray("enseignements");
                                            JsonArray _competences_1 =  _datas.getJsonArray("_competences_1");
                                            JsonArray _competences_2 =  _datas.getJsonArray("_competences_2");
                                            // Je boucle sur mes enseignements
                                            for (int i = 0; i < enseignements.size(); i++) {
                                                JsonObject enseignement = enseignements.getJsonObject(i);
                                                Integer idEnseignement = enseignement.getInteger("id");
                                                JsonArray enseignement_competences_l1 = new fr.wseduc.webutils.collections.JsonArray();
                                                // Je boucle sur les competences de niveau 1
                                                for (int j = 0; j < _competences_1.size(); j++) {
                                                    JsonObject _competence_1 = _competences_1.getJsonObject(j);
                                                    // Si la compétence est dans l'enseignement
                                                    if (_competence_1.getInteger("id_enseignement").equals(idEnseignement)) {
                                                        Integer _competence_1_id = _competence_1.getInteger("id");
                                                        JsonArray _competence_1_competences_l2 = new fr.wseduc.webutils.collections.JsonArray();
                                                        // Je boucle dans les competences de niveau 2
                                                        for (int g = 0; g < _competences_2.size(); g++) {
                                                            JsonObject _competence_2 = _competences_2.getJsonObject(g);
                                                            // Si la competence de niveau 2 est dans la competence de niveau 1
                                                            // ET qu'elle fait parti du même enseignement alors on la récupère
                                                            if ( (_competence_2.getInteger("id_parent").equals(_competence_1_id)) &&
                                                                    _competence_2.getInteger("id_enseignement").equals(idEnseignement)) {
                                                                // J'ajoute la compétence de niveau 2 dans la liste de compétences de la compétence de niveau 1
                                                                _competence_1_competences_l2.add(_competence_2);
                                                                setIdsDomaineFromCompetence(_competence_2,_competence_1,enseignement);
                                                            }
                                                        }
                                                        // Dans la compétence de niveau 1, j'ajoute la liste de compétences de niveau 2
                                                        _competence_1.put("competences_2", _competence_1_competences_l2);
                                                        enseignement_competences_l1.add(_competence_1);
                                                    }
                                                }
                                                // Dans l'enseignement, j'ajoute la liste de ses compétences de niveau 1
                                                enseignement.put("competences_1", enseignement_competences_l1);
                                                result.add(enseignement);
                                            }
                                            Renders.renderJson(request, result);
                                        } else {
                                            leftToResponse(request, eventCompetences_2.left());
                                        }
                                    }
                                });
                            } else {
                                leftToResponse(request, eventCompetences_1.left());
                            }
                        }
                    });
                } else {
                    leftToResponse(request, event.left());
                }
            }
        });
    }

    private static final String mField_Ids_Domaine = "ids_domaine";
    private static final String mField_Ids_Domaine_Int = "ids_domaine_int";

    /**
     * Si la compétence est liée à des ids domaine on transforme cet objet en une liste de d'identifiants
     * @param pCompetence
     */
    private void setIdsDomaineFromCompetence(JsonObject pCompetence,JsonObject pConnaissance, JsonObject pEnseignement) {
        if(pCompetence.getString(mField_Ids_Domaine) != null
                && !pCompetence.getString(mField_Ids_Domaine).isEmpty()){
            String[] vIdsDomaine = pCompetence.getString(mField_Ids_Domaine).split(",");
            JsonArray vJsonArrayIdsDomaine = new fr.wseduc.webutils.collections.JsonArray();
            for (int h = 0; h< vIdsDomaine.length; h++){
                vJsonArrayIdsDomaine.add(Long.valueOf(vIdsDomaine[h]));
            }
            if(vJsonArrayIdsDomaine.size()>0){
                pCompetence.put(mField_Ids_Domaine_Int,vJsonArrayIdsDomaine);

                JsonArray tempArrayConnaissances;
                JsonArray tempArrayEnseignement;
                if(pConnaissance.getJsonArray(mField_Ids_Domaine_Int) != null){
                    tempArrayConnaissances = pConnaissance.getJsonArray(mField_Ids_Domaine_Int);
                }else{
                    tempArrayConnaissances = new fr.wseduc.webutils.collections.JsonArray();
                }
                if(pEnseignement.getJsonArray(mField_Ids_Domaine_Int) != null){
                    tempArrayEnseignement = pEnseignement.getJsonArray(mField_Ids_Domaine_Int);
                }else{
                    tempArrayEnseignement = new fr.wseduc.webutils.collections.JsonArray();
                }
                for (int i = 0 ; i < vJsonArrayIdsDomaine.size();i++){
                    Long idDomaine = (Long) vJsonArrayIdsDomaine.getLong(i);
                    if(!tempArrayConnaissances.contains(idDomaine)){
                        tempArrayConnaissances.add(idDomaine);
                    }
                    if(!tempArrayEnseignement.contains(idDomaine)){
                        tempArrayEnseignement.add(idDomaine);
                    }
                }

                pConnaissance.put(mField_Ids_Domaine_Int,tempArrayConnaissances);
                pEnseignement.put(mField_Ids_Domaine_Int,tempArrayEnseignement);
            }
        }
    }
}