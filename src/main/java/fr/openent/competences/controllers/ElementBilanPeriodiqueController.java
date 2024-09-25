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
import fr.openent.competences.security.AccessElementBilanPeriodiqueFilter;
import fr.openent.competences.security.AccessIfMyStructure;
import fr.openent.competences.security.AccessStructureAdminTeacherFilter;
import fr.openent.competences.security.CreateElementBilanPeriodique;
import fr.openent.competences.security.utils.AccessThematiqueBilanPeriodique;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.impl.*;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


import java.util.*;

import static fr.openent.competences.Utils.isNotNull;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

public class ElementBilanPeriodiqueController extends ControllerHelper {

    private final DefaultElementBilanPeriodiqueService defaultElementBilanPeriodiqueService;

    public ElementBilanPeriodiqueController(EventBus eb) {
        defaultElementBilanPeriodiqueService = new DefaultElementBilanPeriodiqueService(eb);
    }

    /**
     * Créer une thématique avec les données passées en POST
     * @param request
     */
    @Post("/thematique")
    @ApiDoc("Créer une thématique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void createThematique(final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix +
                Competences.SCHEMA_THEMATIQUE_BILAN_PERIODIQUE, resource -> defaultElementBilanPeriodiqueService.insertThematiqueBilanPeriodique(resource,
                defaultResponseHandler(request)));
    }

    /**
     * Retourne les thématiques correspondant au type passé en paramètre
     * @param request
     */
    @Get("/thematique")
    @ApiDoc("Retourne les thématiques correspondant au type passé en paramètre")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessThematiqueBilanPeriodique.class)
    public void getThematiques(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                final String idEtablissement = request.params().get("idEtablissement");
                if(user != null && user.getStructures().contains(idEtablissement)){
                    defaultElementBilanPeriodiqueService.getThematiqueBilanPeriodique(
                            Long.parseLong(request.params().get("type")),
                            idEtablissement,
                            arrayResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Retourne les éléments correspondant à la thématique passée en paramètre
     * @param request
     */
    @Get("/elements/thematique")
    @ApiDoc("Retourne les éléments correspondant à la thématique passée en paramètre")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessThematiqueBilanPeriodique.class)
    public void getElementsOnThematique(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.getElementsOnThematique(
                            request.params().get("idThematique"),
                            arrayResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Créer les élèments du bilan périodique avec les données passées en paramètre
     * @param request
     */
    @Post("/elementBilanPeriodique")
    @ApiDoc("Créer une élément bilan périodique")
    @SecuredAction("create.element.bilan.periodique")
    public void createElementBilanPeriodique(final HttpServerRequest request){
        String schema = defaultElementBilanPeriodiqueService.getElementSchema(request.params().get("type"));

        if(schema != null){
            RequestUtils.bodyToJson(request, pathPrefix + schema, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject resource) {
                    defaultElementBilanPeriodiqueService.insertElementBilanPeriodique(resource,
                            defaultResponseHandler(request));
                }
            });
        } else {
            Renders.renderJson(request, new JsonObject()
                    .put("error", "element type not found"), 400);
        }
    }

    /**
     * Mettre à jour l'élèment du bilan périodique avec les données passées en paramètre
     * @param request
     */
    @Put("/elementBilanPeriodique")
    @ApiDoc("Mettre à jour l'élèment bilan périodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void updateElementBilanPeriodique(final HttpServerRequest request){

        String schema = defaultElementBilanPeriodiqueService.getElementSchema(request.params().get("type"));

        if(schema != null){
            RequestUtils.bodyToJson(request, pathPrefix + schema, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject resource) {
//                    if(Boolean.parseBoolean(request.params().get("hasAppreciations")))
                    defaultElementBilanPeriodiqueService.getGroupesElementBilanPeriodique(
                            request.params().get("idElement"),
                            new Handler<Either<String, JsonArray>> () {
                                @Override
                                public void handle(Either<String, JsonArray> event){
                                    if(event.isRight()){
                                        JsonArray classes = event.right().getValue();

                                        JsonArray newClass = resource.getJsonArray("classes");
                                        List<String> newClasses = new ArrayList<String>();
                                        for(Object c : newClass){
                                            JsonObject classe = (JsonObject) c;
                                            newClasses.add(classe.getString("id"));
                                        }
                                        //pour toutes les classes présentes sur l'élèment actuellement
                                        List<String> deletedClasses = new ArrayList<String>();
                                        for(Object c : classes){
                                            JsonObject classe = (JsonObject) c;
                                            //si la classe présente sur l'élèment actuellement n'est pas dans la liste des nouvelles classes alors c'est une classe à supprimer
                                            if(!newClasses.contains(classe.getString("id_groupe"))){
                                                deletedClasses.add(classe.getString("id_groupe"));
                                            }
                                        }
                                        // on récupère les appréciations sur l'élément liées aux classes supprimées
                                        defaultElementBilanPeriodiqueService.getApprecBilanPerClasse(
                                                deletedClasses, null,
                                                request.params().getAll("idElement"),
                                                new Handler<Either<String, JsonArray>> () {
                                                    @Override
                                                    public void handle(Either<String, JsonArray> event) {
                                                        if (event.isRight()) {
                                                            //je ferai un service qui supprimera l'appreciation
                                                            JsonArray apprecClasseOnDeletedClasses = event.right().getValue();

                                                            defaultElementBilanPeriodiqueService.getApprecBilanPerEleve(
                                                                    deletedClasses, null,
                                                                    request.params().getAll("idElement"), null,
                                                                    new Handler<Either<String, JsonArray>> () {
                                                                        @Override
                                                                        public void handle(Either<String, JsonArray> event) {
                                                                            if (event.isRight()) {
                                                                                JsonArray apprecEleveOnDeletedClasses = event.right().getValue();
                                                                                List<String> idsEleves = new ArrayList<>();

                                                                                if(apprecEleveOnDeletedClasses.size() > 0){// Si j'ai des appréciations sur élèves à supprimer
                                                                                    //pour chaque appréciation, je récupère l'élève propriétaire
                                                                                    for(Object a : apprecEleveOnDeletedClasses){
                                                                                        JsonObject appreciation = (JsonObject) a;
                                                                                        idsEleves.add(appreciation.getString("id_eleve"));
                                                                                    }
                                                                                    // je cherche la liste des classes/groupes des elèves propriétaires
                                                                                    JsonObject action = new JsonObject()
                                                                                            .put("action", "user.getUsers")
                                                                                            .put("idUsers", idsEleves);

                                                                                    eb.request(Competences.VIESCO_BUS_ADDRESS, action,
                                                                                            handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                                                                @Override
                                                                                                public void handle(Message<JsonObject> message) {
                                                                                                    JsonObject body = message.body();

                                                                                                    if ("ok".equals(body.getString("status"))) {
                                                                                                        JsonArray users = body.getJsonArray("results");

                                                                                                        // map qui à un idUser associe une map de idClass -> externalIdClass de toutes les classes/groupes de l'élève
                                                                                                        Map<String, Map<String, String>> usersMap = new HashMap<String, Map<String, String>>();
                                                                                                        for(Object u : users) {
                                                                                                            JsonObject user = (JsonObject) u;
                                                                                                            Map<String, String> classesMap = new HashMap<String, String>();

                                                                                                            JsonArray idClasses = user.getJsonArray("currentClassIds");
                                                                                                            idClasses.addAll(user.getJsonArray("currentGroupIds"));

                                                                                                            JsonArray externalIdClasses = user.getJsonArray("currentClassExternalIds");
                                                                                                            externalIdClasses.addAll(user.getJsonArray("currentGroupExternalIds"));

                                                                                                            for(int i = 0; i < idClasses.size(); i++) {
                                                                                                                classesMap.put(idClasses.getString(i), externalIdClasses.getString(i));
                                                                                                            }

                                                                                                            usersMap.put(user.getString("id"), classesMap);
                                                                                                        }

                                                                                                        JsonArray apprecDeletedConcurrent = new JsonArray();
                                                                                                        JsonArray apprecDeletedAlone = new JsonArray();
                                                                                                        for(Object a : apprecEleveOnDeletedClasses){
                                                                                                            JsonObject apprec = (JsonObject) a;

                                                                                                            //si l'élève sur l'appreciation appartient à une des nouvelles classes alors j'ajoute l'appreciation à
                                                                                                            //apprecDeletedConcurrent pour supprimer les relations entre l'appreciation et toutes les deleted classes
                                                                                                            //
                                                                                                            //si l'élève sur l'appreciation n'appartient à aucune des nouvelles classes alors j'ajoute l'appreciation à
                                                                                                            //apprecDeletedAlone pour supprimer l'appréciation et les relations entre l'appreciation et toutes les deleted classes
                                                                                                            Map<String, String> studentClasses = usersMap.get(apprec.getString("id_eleve"));
                                                                                                            try {
                                                                                                                if(Collections.disjoint(studentClasses.keySet(), newClasses)){
                                                                                                                    apprecDeletedAlone.add(apprec);
                                                                                                                } else {
                                                                                                                    apprecDeletedConcurrent.add(apprec);
                                                                                                                }
                                                                                                            } catch (NullPointerException err) {
                                                                                                                badRequest(request, err.getMessage());
                                                                                                                log.error(err);
                                                                                                            }
                                                                                                        }
                                                                                                        JsonObject apprecDeleted = new JsonObject()
                                                                                                                .put("apprecDeletedAlone", apprecDeletedAlone)
                                                                                                                .put("apprecDeletedConcurrent", apprecDeletedConcurrent);
                                                                                                        defaultElementBilanPeriodiqueService.updateElementBilanPeriodique(
                                                                                                                Long.parseLong(request.params().get("idElement")), resource,
                                                                                                                apprecClasseOnDeletedClasses, apprecDeleted,
                                                                                                                deletedClasses, defaultResponseHandler(request));
                                                                                                    } else{
                                                                                                        leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                                                                                    }
                                                                                                }
                                                                                            }));
                                                                                } else { // Si je n'ai pas d'appréciations sur élèves à supprimer
                                                                                    defaultElementBilanPeriodiqueService.updateElementBilanPeriodique(
                                                                                            Long.parseLong(request.params().get("idElement")), resource,
                                                                                            apprecClasseOnDeletedClasses, null,
                                                                                            deletedClasses,defaultResponseHandler(request));
                                                                                }
                                                                            } else{
                                                                                Renders.renderJson(request, new JsonObject()
                                                                                        .put("error", "error while retreiving students appreciations"), 400);
                                                                            }
                                                                        }
                                                                    });
                                                        } else{
                                                            Renders.renderJson(request, new JsonObject()
                                                                    .put("error", "error while retreiving classes appreciations"), 400);
                                                        }
                                                    }
                                                });
                                    } else{
                                        leftToResponse(request, event.left());
                                    }
                                }
                            });
                }
            });
        } else {
            JsonObject error = (new JsonObject()).put("error", "element type not found");
            Renders.renderJson(request, error, 400);
        }
    }

    /**
     * Retourne les éléments du bilan périodique
     * @param request
     */
    @Get("/elementsBilanPeriodique")
    @ApiDoc("Retourne les élèments du bilan périodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    public void getElementBilanPeriodique(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request,  user -> {
            if(user != null){
                String idUser = null;
                List<String> idClasses = null;
                if(!new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString()) &&
                        isNotNull(Boolean.parseBoolean(request.params().get("visu")))){
                    idUser = request.params().get("idEnseignant");
                }
                if(isNotNull(request.params().get("idClasse"))){
                    idClasses = Arrays.asList(request.params().get("idClasse"));
                }
                defaultElementBilanPeriodiqueService.getElementsBilanPeriodique( idUser, idClasses,
                        request.params().get("idEtablissement"), arrayResponseHandler(request));
            }else{
                unauthorized(request);
            }
        });
    }

    /**
     * Retourne les thématiques correspondant au type passé en paramètre
     * @param request
     */
    @Get("/elementsBilanPeriodique/enseignants")
    @ApiDoc("Retourne les thématiques correspondant au type passé en paramètre")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    public void getEnseignantsElements(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.getEnseignantsElementsBilanPeriodique(
                            request.params().getAll("idElement"),
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> event) {
                                    if (event.isRight()) {
                                        JsonArray result = event.right().getValue();

                                        List<String> idUsers = new ArrayList<>();
                                        Map<Long, List<String>> ensElemMap = new HashMap<Long, List<String>>();

                                        for (Object o : result) {
                                            JsonObject ensMat = (JsonObject) o;
                                            if (!idUsers.contains(ensMat.getString("id_intervenant"))) {
                                                idUsers.add(ensMat.getString("id_intervenant"));
                                            }
                                            if (!ensElemMap.containsKey(ensMat.getLong("id_elt_bilan_periodique"))) {
                                                ensElemMap.put(ensMat.getLong("id_elt_bilan_periodique"), new ArrayList<>());
                                            }
                                            ensElemMap.get(ensMat.getLong("id_elt_bilan_periodique")).add(ensMat.getString("id_intervenant"));
                                        }

                                        // récupération des noms des intervenants
                                        JsonObject action = new JsonObject()
                                                .put("action", "user.getUsers")
                                                .put("idUsers", idUsers);

                                        eb.request(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                            @Override
                                            public void handle(Message<JsonObject> message) {
                                                JsonObject body = message.body();

                                                if ("ok".equals(body.getString("status"))) {
                                                    JsonArray users = body.getJsonArray("results");
                                                    Map<String, String> usersMap = new HashMap<String, String>();
                                                    for(Object o : users){
                                                        JsonObject user = (JsonObject)o;
                                                        usersMap.put(user.getString("id"), user.getString("displayName"));
                                                    }
                                                    JsonArray resultat = new JsonArray();

                                                    for(Map.Entry<Long, List<String>> entry : ensElemMap.entrySet()) {
                                                        JsonObject enseignantsElem = new JsonObject();
                                                        enseignantsElem.put("idElement", entry.getKey());
                                                        JsonArray ens = new fr.wseduc.webutils.collections.JsonArray();
                                                        for(Object o : entry.getValue()){
                                                            String idEns = (String)o;
                                                            ens.add(usersMap.get(idEns));
                                                        }
                                                        enseignantsElem.put("idsEnseignants", ens);
                                                        resultat.add(enseignantsElem);
                                                    }
                                                    Renders.renderJson(request, resultat);
                                                } else {
                                                    leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                                }
                                            }
                                        }));
                                    } else {
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

    /**
     * Retourne les classes correspondant à l'enseignant en paramètre
     * @param request
     */
    @Get("/elementsBilanPeriodique/classes")
    @ApiDoc("Retourne les classes correspondant à l'enseignant")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessElementBilanPeriodiqueFilter.class)
    public void getClassesElementBilanPeriodique(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request,  user -> {
            if(user != null){
                final String idStructure = request.params().get("idStructure");
                defaultElementBilanPeriodiqueService.getClasseProjets(idStructure, user, event -> {
                    if (event.isRight()) {
                        JsonArray jsonArrayResultat = event.right().getValue();
                        Renders.renderJson(request, jsonArrayResultat);
                    } else {
                        leftToResponse(request, event.left());
                    }
                });
            }else{
                unauthorized(request);
            }
        });
    }

    /**
     * Supprimer des éléments du bilan périodique dont les ids sont passés en paramètre
     * @param request
     */
    @Delete("/elementsBilanPeriodique")
    @ApiDoc("Supprimer des éléments du bilan périodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void deleteElementBilanPeriodique(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.deleteElementBilanPeriodique(
                            request.params().getAll("idElement"),
                            arrayResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Mettre à jour une thématique
     * @param request
     */
    @Put("/thematique")
    @ApiDoc("Mettre à jour une thématique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void updateThematique(final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix +
                Competences.SCHEMA_THEMATIQUE_BILAN_PERIODIQUE, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject resource) {
                defaultElementBilanPeriodiqueService.updateThematique(
                        request.params().get("idThematique"),resource,
                        defaultResponseHandler(request));
            }
        });
    }

    /**
     * Supprimer une thématique
     * @param request
     */
    @Delete("/thematique")
    @ApiDoc("Supprimer une thématique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void deleteThematique(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.deleteThematique(
                            request.params().get("idThematique"),
                            arrayResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Retourne les appreciations liées aux éléments du bilan périodique
     * (et à la classe) passés en paramètre
     * @param request
     */
    @Get("/elementsAppreciations")
    @ApiDoc("Retourne les appreciations liées au élèments du bilan périodiques passés en paramètre")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessStructureAdminTeacherFilter.class)
    public void getAppreciations(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    List<String> idsClasses =  request.params().getAll("idClasse");
                    String idPeriode = request.params().get("idPeriode");
                    List<String> idElements =  request.params().getAll("idElement");
                    String idEleve = request.params().get("idEleve");
                    defaultElementBilanPeriodiqueService.getAppreciations(idsClasses, idPeriode,idElements,idEleve,
                            arrayResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Créer une appréciation avec les données passées en POST depuis l'écran de la saisie de projet
     * @param request
     */
    @Post("/elementsAppreciationsSaisieProjet")
    @ApiDoc("Créer une appréciation")
    @SecuredAction("create.appreciation.saisie.projets")
    public void createAppreciationSaisieProjet(final HttpServerRequest request){
        defaultElementBilanPeriodiqueService.createApprec(request);
    }

    /**
     * Créer une appréciation avec les données passées en POST depuis l'écran du bilan périodique
     * @param request
     */
    @Post("/elementsAppreciationBilanPeriodique")
    @ApiDoc("Créer une appréciation")
    @SecuredAction("create.appreciation.bilan.periodique")
    public void createAppreciation(final HttpServerRequest request){
        defaultElementBilanPeriodiqueService.createApprec(request);
    }


}
