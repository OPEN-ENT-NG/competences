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
import fr.openent.competences.security.AccessEvaluationFilter;
import fr.openent.competences.security.ParamCompetenceRight;
import fr.openent.competences.service.CompetencesService;
import fr.openent.competences.service.impl.DefaultCompetencesService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
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
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.http.response.DefaultResponseHandler.*;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class CompetenceController extends ControllerHelper {

    /**
     * Déclaration des services
     */
    private final CompetencesService competencesService;

    public CompetenceController(EventBus eb) {
        competencesService = new DefaultCompetencesService(eb);
    }

    /**
     * Regarde si la compétence a des enfants
     * @param competence
     * @param values
     * @return True si la compétence a des enfants, sinon False
     */
    public Boolean isParent(JsonObject competence, JsonArray values){
        Integer id = competence.getInteger("id");
        JsonObject o;
        for(int i = 0 ; i < values.size(); i++){
            o = values.get(i);
            if(o.getInteger("id_parent") == id){
                return true;
            }
        }
        return false;
    }

    /**
     * Cherche les enfants de la compétences
     * @param competence
     * @param values
     * @return Liste des enfants de la compétence
     */
    public JsonArray findChildren(JsonObject competence, JsonArray values){
        JsonArray children = new JsonArray();
        Integer id = competence.getInteger("id");
        JsonObject o;
        for(int i = 0; i < values.size(); i++){
            o = values.get(i);
            if(o.getInteger("id_parent") == id){
                children.addObject(o);
            }
        }
        return children;
    }

    /**
     * Ordonne les compétences pour retourner un arbre
     * @param values
     * @return Liste des compétences ordonnées
     */
    public JsonArray orderCompetences(JsonArray values){
        JsonArray resultat = new JsonArray();
        JsonObject o;
        for(int i = 0; i < values.size(); i++){
            o = values.get(i);
            o.putBoolean("selected", false);
            if(isParent(o, values)){
                o.putArray("children", findChildren(o, values));
            }
            if(o.getInteger("id_parent") == 0){
                resultat.addObject(o);
            }
        }
        return resultat;
    }

    /**
     * Recupère la liste des compétences pour un devoir donné
     * @param request
     */
    @Get("/competences/devoir/:idDevoir")
    @ApiDoc("Recupère la liste des compétences pour un devoir donné")
    @ResourceFilter(AccessEvaluationFilter.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getCompetencesDevoir(final HttpServerRequest request){

        Long lIdDevoir;

        try {
            lIdDevoir = Long.parseLong(request.params().get("idDevoir"));

        } catch(NumberFormatException e) {
            log.error("Error : idDevoir must be a long object", e);
            badRequest(request, e.getMessage());
            return;
        }

        competencesService.getDevoirCompetences(lIdDevoir, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if(event.isRight()){
                    Renders.renderJson(request, event.right().getValue());
                }else{
                    leftToResponse(request, event.left());
                }
            }
        });
    }

    @Delete("/items/:idEtablissement")
    @ApiDoc("Supprimer toutes les données personnalisées sur les items d'un étbalissement donné")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ParamCompetenceRight.class)
    public void delete(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                String idEtablissement = request.params().get("idEtablissement");
                if(user != null && user.getStructures().contains(idEtablissement)){
                    competencesService.deleteCustom(idEtablissement, defaultResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Recupère les dernière compétences utilisée lors de la création d'un devoir
     * @param request
     */
    @Get("/competences/last/devoir/")
    @ApiDoc("Recupère les dernière compétences utilisée lors de la création d'un devoir")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getLastCompetencesDevoir(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    String idEtablissement = request.params().get("idStructure");
                    competencesService.getLastCompetencesDevoir(idEtablissement, user.getUserId(), new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if(event.isRight()){
                                Renders.renderJson(request, event.right().getValue());
                            }else{
                                leftToResponse(request, event.left());
                            }
                        }
                    });
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    @Post("/competence")
    @ApiDoc("Crée une nouvelle compétence")
    @SecuredAction(Competences.PARAM_COMPETENCE_RIGHT)
    public void createCompetence(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_COMPETENCE_CREATE, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject competence) {
                competencesService.create(competence, defaultResponseHandler(request));
            }
        });
    }

    @Put("/competence")
    @ApiDoc("Met à jour une compétence")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ParamCompetenceRight.class)
    public void updateCompetence(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_COMPETENCE_UPDATE, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject competence) {
                Number idComp = Long.valueOf(request.params().get("id"));
                String idEtablissement = request.params().get("idEtablissement");
                competencesService.update(idComp, idEtablissement, competence, defaultResponseHandler(request));
            }
        });
    }

    @Delete("/competence")
    @ApiDoc("Supprime une compétence")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ParamCompetenceRight.class)
    public void deleteCompetence(HttpServerRequest request) {
        try {
            Number idComp = Long.valueOf(request.params().get("id"));
            String idEtablissement = request.params().get("id_etablissement");
            competencesService.delete(idComp, idEtablissement, defaultResponseHandler(request));
        } catch (Exception e) {
            leftToResponse(request, new Either.Left<>(e.toString()));
        }
    }
}
