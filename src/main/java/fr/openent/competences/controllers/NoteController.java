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
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.security.*;
import fr.openent.competences.security.utils.FilterPeriodeUtils;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.service.DevoirService;
import fr.openent.competences.service.ElementProgramme;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.*;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.isNotNull;
import static fr.openent.competences.Utils.isNull;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class NoteController extends ControllerHelper {

    /**
     * Déclaration des services
     */
    private final NoteService notesService;
    private final DevoirService devoirsService;
    private final UtilsService utilsService;
    private final ElementProgramme elementProgramme;

    public NoteController(EventBus eb) {
        this.eb = eb;
        notesService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE,eb);
        devoirsService = new DefaultDevoirService(this.eb);
        utilsService = new DefaultUtilsService(this.eb);
        elementProgramme = new DefaultElementProgramme();
    }

    /**
     * Recupère les notes d'un devoir donné
     *
     * @param request
     */
    @Get("/devoir/:idDevoir/notes")
    @ApiDoc("Recupère les notes pour un devoir donné")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessEvaluationFilter.class)
    public void view(final HttpServerRequest request) {
        Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);

        Long idDevoir;
        try {
            idDevoir = Long.parseLong(request.params().get("idDevoir"));
        } catch (NumberFormatException e) {
            log.error(" Error : idDevoir must be a long object", e);
            badRequest(request, e.getMessage());
            return;
        }

        notesService.listNotesParDevoir(idDevoir, handler);
    }

    /**
     * Créer une note avec les données passées en POST
     *
     * @param request
     */
    @Post("/note")
    @ApiDoc("Créer une note")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateEvaluationWorkflow.class)
    public void create(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_NOTES_CREATE, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            notesService.createNote(resource, user, notEmptyResponseHandler(request));
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
     * Créer une note avec les données passées en POST
     *
     * @param request
     */
    @Post("/note")
    @ApiDoc("Créer une note")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateEvaluationWorkflow.class)
    public void createAndUpdateNote(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_NOTES_CREATE, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jo) {
                            notesService.createNoteDevoir(jo, user.getUserId(), defaultResponseHandler(request));
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
     * Modifie une note avec les données passées en PUT
     *
     * @param request
     */
    @Put("/note")
    @ApiDoc("Modifie une note")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessNoteFilter.class)
    public void update(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_NOTES_UPDATE, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            notesService.updateNote(resource, user, defaultResponseHandler(request));
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
     * Supprime la note passé en paramètre
     *
     * @param request
     */
    @Delete("/note")
    @ApiDoc("Supprimer une note donnée")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessNoteFilter.class)
    public void deleteNoteDevoir(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {

                    Long idNote;
                    try {
                        idNote = Long.parseLong(request.params().get("idNote"));
                    } catch (NumberFormatException e) {
                        log.error("Error : idNote must be a long object", e);
                        badRequest(request, e.getMessage());
                        return;
                    }

                    notesService.deleteNote(idNote, user, defaultResponseHandler(request));
                } else {
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Récupère les notes pour le widget
     *
     * @param request
     */
    @Get("/widget")
    @ApiDoc("Récupère les notes pour le widget")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getWidgetNotes(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if (user != null) {
                    notesService.getWidgetNotes(request.params().get("userId"), arrayResponseHandler(request));
                }
            }
        });
    }

    /**
     * Récupère les notes pour le relevé de notes
     *
     * @param request
     */
    @Get("/releve")
    @ApiDoc("Récupère les notes, les moyennes finales pour le relevé de notes")
    @SecuredAction(value="access.releve", type=ActionType.WORKFLOW)
    public void getNoteElevePeriode(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                final String idEleve = request.params().get(ID_ELEVE_KEY);
                final String idEtablissement = request.params().get(ID_ETABLISSEMENT_KEY);
                final String idClasse = request.params().get(ID_CLASSE_KEY);
                final String idMatiere = request.params().get(ID_MATIERE_KEY);
                final String idPeriodeString = request.params().get(ID_PERIODE_KEY);
                final Integer typeClasse = Integer.valueOf(request.params().get(TYPE_CLASSE_KEY));

                new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere, false,
                        hasAccessToMatiere -> {
                            Long idPeriode = null;
                            if (!hasAccessToMatiere) {
                                unauthorized(request);
                            } else {
                                if (idPeriodeString != null) {
                                    try {
                                        idPeriode = Long.parseLong(idPeriodeString);
                                    } catch (NumberFormatException e) {
                                        log.error("Error : idPeriode must be a long object", e);
                                        badRequest(request, e.getMessage());
                                        return;
                                    }
                                }
                                JsonObject params = new JsonObject().put(ID_ELEVE_KEY, idEleve)
                                        .put(ID_CLASSE_KEY, idClasse)
                                        .put(ID_PERIODE_KEY, idPeriode)
                                        .put(ID_ETABLISSEMENT_KEY, idEtablissement)
                                        .put(ID_MATIERE_KEY, idMatiere)
                                        .put(TYPE_CLASSE_KEY, typeClasse);

                                notesService.getDatasReleve(params, notEmptyResponseHandler(request));
                            }
                        });
            }
        });
    }

    @Post("/releve/export")
    @ApiDoc("Exporte un relevé périodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveByClasseMatiereFilter.class)
    public void exportRelevePeriodique(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, param -> {
            if(param.getString("fileType").equals("pdf")) {
                notesService.exportPDFRelevePeriodique(param,  request, vertx, config);
            }
            else {
                notesService.getDatasReleve(param, notEmptyResponseHandler(request));
            }
        });
    }

    @Post("/releve/exportTotale")
    @ApiDoc("Exporte un relevé périodique")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void exportTotaleRelevePeriodique(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, param -> {
            JsonArray idPeriodes = param.getJsonArray("idPeriodes");
            JsonArray idMatieres = param.getJsonArray("idMatieres");
            boolean annual = idPeriodes.size() > 0;
            if(!annual) {
                final Long idPeriode = param.getLong(Competences.ID_PERIODE_KEY);
                notesService.getTotaleDatasReleve(param, idPeriode, annual, notEmptyResponseHandler(request));
            } else {
                try{
                    final JsonObject resultHandler = new JsonObject();
                    List<Future> listFuturesEachPeriode = new ArrayList<>();
                    for (Object idPeriode : idPeriodes){
                        // Récupération du  nombre de devoirs avec évaluation numérique
                        Long periode = ((Integer)idPeriode).longValue();
                        Future<JsonObject> exportPeriode = Future.future();
                        notesService.getTotaleDatasReleve(param, periode, annual, event -> {
                            formate(exportPeriode, event);
                        });
                        listFuturesEachPeriode.add(exportPeriode);
                    }
                    CompositeFuture.all(listFuturesEachPeriode).setHandler(exportPeriodeEvent -> {
                        try{
                            if (exportPeriodeEvent.succeeded()) {
                                DecimalFormat decimalFormat = new DecimalFormat("#.00");
                                decimalFormat.setRoundingMode(RoundingMode.HALF_UP);//with this mode 2.125 -> 2.13 without 2.125 -> 2.12

                                for (int i=0; i < idPeriodes.size(); i++) {
                                    resultHandler.put(idPeriodes.getLong(i).toString(), ((JsonObject) listFuturesEachPeriode.get(i).result()));
                                }

                                JsonObject eleves = new JsonObject()
                                        .put("eleves",((JsonObject) listFuturesEachPeriode.get(0).result()).getJsonArray("eleves").copy());
                                resultHandler.put("annual", eleves);

                                for (int i=1; i < idPeriodes.size(); i++) {
                                    JsonArray elevesAutresPeriodes = ((JsonObject) listFuturesEachPeriode.get(i).result()).getJsonArray("eleves");
                                    for(Object el : resultHandler.getJsonObject("annual").getJsonArray("eleves")){
                                        JsonObject eleve = (JsonObject) el;
                                        for (Object elAutrePeriode : elevesAutresPeriodes){
                                            JsonObject eleveAutrePeriode = (JsonObject) elAutrePeriode;
                                            if(eleve.getString("id").equals(eleveAutrePeriode.getString("id"))){
                                                for(Object idM : idMatieres){
                                                    String idMatiere = idM.toString();
                                                    if(eleve.getJsonObject("moyenneFinale").containsKey(idMatiere)
                                                            && eleve.getJsonObject("moyenneFinale").getValue(idMatiere) != null &&
                                                            eleveAutrePeriode.getJsonObject("moyenneFinale").containsKey(idMatiere) &&
                                                            eleveAutrePeriode.getJsonObject("moyenneFinale").getValue(idMatiere) != null &&
                                                            !(eleveAutrePeriode.getJsonObject("moyenneFinale").getValue(idMatiere).equals(NN) ||
                                                                    eleveAutrePeriode.getJsonObject("moyenneFinale").getValue(idMatiere).equals(""))) {
                                                        if (eleve.getJsonObject("moyenneFinale").getValue(idMatiere).equals(NN) ||
                                                                eleve.getJsonObject("moyenneFinale").getValue(idMatiere).equals("")) {
                                                            eleve.getJsonObject("moyenneFinale")
                                                                    .put(idMatiere, decimalFormat.format(Double.valueOf(eleveAutrePeriode.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",","."))));
                                                        }else {
                                                            eleve.getJsonObject("moyenneFinale")
                                                                    .put(idMatiere, decimalFormat.format(Double.parseDouble(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",",".")) +
                                                                            Double.parseDouble(eleveAutrePeriode.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",","."))));
                                                            if (eleve.containsKey("nbPeriodeMoyenne")) {
                                                                if (eleve.getJsonObject("nbPeriodeMoyenne").containsKey(idMatiere)) {
                                                                    eleve.getJsonObject("nbPeriodeMoyenne").put(idMatiere,
                                                                            eleve.getJsonObject("nbPeriodeMoyenne").getLong(idMatiere) + 1);
                                                                } else {
                                                                    eleve.getJsonObject("nbPeriodeMoyenne").put(idMatiere, 2);
                                                                }
                                                            } else {
                                                                JsonObject jsonToAdd = new JsonObject().put(idMatiere, 2);
                                                                eleve.put("nbPeriodeMoyenne", jsonToAdd);
                                                            }
                                                        }
                                                    }else if(eleveAutrePeriode.getJsonObject("moyenneFinale").containsKey(idMatiere) &&
                                                            eleveAutrePeriode.getJsonObject("moyenneFinale").getValue(idMatiere) != null &&
                                                            !(eleveAutrePeriode.getJsonObject("moyenneFinale").getValue(idMatiere).equals(NN) || eleveAutrePeriode.getJsonObject("moyenneFinale").getValue(idMatiere).equals(""))){
                                                        eleve.getJsonObject("moyenneFinale")
                                                                .put(idMatiere, decimalFormat.format(Double.valueOf(eleveAutrePeriode.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",","."))));
                                                    }

                                                    if(eleve.getJsonObject("positionnement").containsKey(idMatiere) && eleve.getJsonObject("positionnement").getValue(idMatiere) != null &&
                                                            eleveAutrePeriode.getJsonObject("positionnement").containsKey(idMatiere) && eleveAutrePeriode.getJsonObject("positionnement").getValue(idMatiere) != null &&
                                                            !(eleveAutrePeriode.getJsonObject("positionnement").getValue(idMatiere).equals(NN) || eleveAutrePeriode.getJsonObject("positionnement").getValue(idMatiere).equals(""))) {
                                                        if (eleve.getJsonObject("positionnement").getValue(idMatiere).equals(NN) || eleve.getJsonObject("positionnement").getValue(idMatiere).equals("")) {
                                                            eleve.getJsonObject("positionnement")
                                                                    .put(idMatiere,Double.valueOf(eleveAutrePeriode.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",",".")));
                                                        }else {
                                                            eleve.getJsonObject("positionnement")
                                                                    .put(idMatiere, Float.parseFloat(eleve.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",",".")) +
                                                                            Float.parseFloat(eleveAutrePeriode.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",",".")));
                                                            if (eleve.containsKey("nbPeriodeMoyennePos")) {
                                                                if (eleve.getJsonObject("nbPeriodeMoyennePos").containsKey(idMatiere)) {
                                                                    eleve.getJsonObject("nbPeriodeMoyennePos").put(idMatiere,
                                                                            eleve.getJsonObject("nbPeriodeMoyennePos").getLong(idMatiere) + 1);
                                                                } else {
                                                                    eleve.getJsonObject("nbPeriodeMoyennePos").put(idMatiere, 2);
                                                                }
                                                            } else {
                                                                JsonObject jsonToAdd = new JsonObject().put(idMatiere, 2);
                                                                eleve.put("nbPeriodeMoyennePos", jsonToAdd);
                                                            }
                                                        }
                                                    }else if(eleveAutrePeriode.getJsonObject("positionnement").containsKey(idMatiere) && eleveAutrePeriode.getJsonObject("positionnement").getValue(idMatiere) != null &&
                                                            !(eleveAutrePeriode.getJsonObject("positionnement").getValue(idMatiere).equals(NN) || eleveAutrePeriode.getJsonObject("positionnement").getValue(idMatiere).equals(""))){
                                                        eleve.getJsonObject("positionnement")
                                                                .put(idMatiere,Float.valueOf(eleveAutrePeriode.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",",".")));
                                                    }
                                                }

                                                if(eleve.containsKey("moyenne_generale") && eleve.getValue("moyenne_generale") != null &&
                                                        eleveAutrePeriode.containsKey("moyenne_generale") && eleveAutrePeriode.getValue("moyenne_generale") != null &&
                                                        !(eleveAutrePeriode.getValue("moyenne_generale").equals(NN) || eleveAutrePeriode.getValue("moyenne_generale").equals(""))) {
                                                    if (eleve.getValue("moyenne_generale").equals(NN) || eleve.getValue("moyenne_generale").equals("")) {
                                                        eleve.put("moyenne_generale", decimalFormat.format(Double.valueOf(eleveAutrePeriode.getValue("moyenne_generale").toString().replace(",","."))));
                                                        if (!eleve.containsKey("nbPeriodesMoyenne"))
                                                            eleve.put("nbPeriodesMoyenne", 1);
                                                    }else {
                                                        eleve.put("moyenne_generale", decimalFormat.format(Double.parseDouble(eleve.getValue("moyenne_generale").toString().replace(",",".")) +
                                                                Double.parseDouble(eleveAutrePeriode.getValue("moyenne_generale").toString().replace(",","."))));
                                                        if (eleve.containsKey("nbPeriodesMoyenne")) {
                                                            eleve.put("nbPeriodesMoyenne", eleve.getLong("nbPeriodesMoyenne") + 1);
                                                        } else {
                                                            eleve.put("nbPeriodesMoyenne", 2);
                                                        }
                                                    }
                                                }else if(eleveAutrePeriode.containsKey("moyenne_generale") && eleveAutrePeriode.getValue("moyenne_generale") != null &&
                                                        !(eleveAutrePeriode.getValue("moyenne_generale").equals(NN) || eleveAutrePeriode.getValue("moyenne_generale").equals(""))){
                                                    eleve.put("moyenne_generale", decimalFormat.format(Double.valueOf(eleveAutrePeriode.getValue("moyenne_generale").toString().replace(",","."))));
                                                    if (!eleve.containsKey("nbPeriodesMoyenne"))
                                                        eleve.put("nbPeriodesMoyenne", 1);
                                                }else if(eleve.containsKey("moyenne_generale") && eleve.getValue("moyenne_generale") != null &&
                                                        !(eleve.getValue("moyenne_generale").equals(NN) || eleve.getValue("moyenne_generale").equals(""))){
                                                    eleve.put("moyenne_generale", decimalFormat.format(Double.valueOf(eleve.getValue("moyenne_generale").toString().replace(",","."))));
                                                    if (!eleve.containsKey("nbPeriodesMoyenne"))
                                                        eleve.put("nbPeriodesMoyenne", 1);
                                                }
                                                eleveAutrePeriode.put("addAnnual",true);
                                            }
                                        }
                                    }
                                    for (Object elAutrePeriode : elevesAutresPeriodes){
                                        JsonObject eleveAutrePeriode = (JsonObject) elAutrePeriode;
                                        if(!eleveAutrePeriode.containsKey("addAnnual")){
                                            resultHandler.getJsonObject("annual").getJsonArray("eleves").add(eleveAutrePeriode);
                                        }
                                    }
                                }

                                for (int i=0; i < idPeriodes.size();i++) {
                                    JsonArray elevesAutresPeriodes = ((JsonObject) listFuturesEachPeriode.get(i).result()).getJsonArray("eleves");
                                    for (Object elAutrePeriode : elevesAutresPeriodes) {
                                        JsonObject eleveAutrePeriode = (JsonObject) elAutrePeriode;
                                        if(eleveAutrePeriode.containsKey("moyenne_generale") && eleveAutrePeriode.getValue("moyenne_generale") != null &&
                                                !(eleveAutrePeriode.getValue("moyenne_generale").equals(NN) || eleveAutrePeriode.getValue("moyenne_generale").equals(""))) {
                                            eleveAutrePeriode.put("moyenne_generale", decimalFormat.format(Double.valueOf(eleveAutrePeriode.getValue("moyenne_generale").toString())));
                                        }
                                    }
                                }

                                boolean init = false;
                                Double min = 0.0;
                                Double max = 0.0;
                                int nbElevesMoyenne = 0;
                                double moyenneDeMoyenne = 0.0;
                                for(Object el : resultHandler.getJsonObject("annual").getJsonArray("eleves")) {
                                    JsonObject eleve = ((JsonObject) el);

                                    for (Object idM : idMatieres) {
                                        String idMatiere = idM.toString();
                                        if (eleve.getJsonObject("moyenneFinale").containsKey(idMatiere) && eleve.getJsonObject("moyenneFinale").getValue(idMatiere) != null) {
                                            if (!(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).equals(NN) || eleve.getJsonObject("moyenneFinale").getValue(idMatiere).equals(""))) {
                                                if (eleve.containsKey("nbPeriodeMoyenne") && eleve.getJsonObject("nbPeriodeMoyenne").containsKey(idMatiere)) {
                                                    eleve.getJsonObject("moyenneFinale").put(idMatiere, decimalFormat.format(Double.valueOf(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",",".")) / eleve.getJsonObject("nbPeriodeMoyenne").getLong(idMatiere)));
                                                } else {
                                                    eleve.getJsonObject("moyenneFinale")
                                                            .put(idMatiere, decimalFormat.format(Double.valueOf(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",","."))));
                                                }
                                            }
                                        }
                                        if (eleve.getJsonObject("positionnement").containsKey(idMatiere) && eleve.getJsonObject("positionnement").getValue(idMatiere) != null) {
                                            if (!(eleve.getJsonObject("positionnement").getValue(idMatiere).equals(NN) || eleve.getJsonObject("positionnement").getValue(idMatiere).equals(""))) {
                                                if (eleve.containsKey("nbPeriodeMoyennePos") && eleve.getJsonObject("nbPeriodeMoyennePos").containsKey(idMatiere)) {
                                                    eleve.getJsonObject("positionnement").put(idMatiere, decimalFormat.format(Float.parseFloat(eleve.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",",".")) / eleve.getJsonObject("nbPeriodeMoyennePos").getLong(idMatiere)));
                                                } else {
                                                    eleve.getJsonObject("positionnement")
                                                            .put(idMatiere, decimalFormat.format((Double.valueOf(eleve.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",",".")))));
                                                }
                                            }
                                        }
                                    }
                                    if (eleve.containsKey("nbPeriodesMoyenne") && eleve.containsKey("moyenne_generale") && eleve.getValue("moyenne_generale") != null
                                            && !(eleve.getValue("moyenne_generale").equals(NN) || eleve.getValue("moyenne_generale").equals(""))) {
                                        moyenneDeMoyenne += Double.parseDouble(eleve.getValue("moyenne_generale").toString().replace(",",".")) / eleve.getLong("nbPeriodesMoyenne");
                                        nbElevesMoyenne++;
                                        if (!init) {
                                            min = Double.parseDouble(eleve.getValue("moyenne_generale").toString().replace(",",".")) / eleve.getLong("nbPeriodesMoyenne");
                                            max = Double.parseDouble(eleve.getValue("moyenne_generale").toString().replace(",",".")) / eleve.getLong("nbPeriodesMoyenne");
                                            init = true;
                                        } else {
                                            if (min > Double.parseDouble(eleve.getValue("moyenne_generale").toString().replace(",",".")) / eleve.getLong("nbPeriodesMoyenne"))
                                                min = Double.parseDouble(eleve.getValue("moyenne_generale").toString().replace(",",".")) / eleve.getLong("nbPeriodesMoyenne");
                                            if (max < Double.parseDouble(eleve.getValue("moyenne_generale").toString().replace(",",".")) / eleve.getLong("nbPeriodesMoyenne"))
                                                max = Double.parseDouble(eleve.getValue("moyenne_generale").toString().replace(",",".")) / eleve.getLong("nbPeriodesMoyenne");
                                        }
                                        eleve.put("moyenne_generale",
                                                decimalFormat.format(Double.parseDouble(eleve.getValue("moyenne_generale").toString().replace(",",".")) / eleve.getLong("nbPeriodesMoyenne")));
                                    } else if (!eleve.containsKey("moyenne_generale") || eleve.getValue("moyenne_generale") == null
                                            || (eleve.getValue("moyenne_generale").equals(NN) || eleve.getValue("moyenne_generale").equals(""))) {
                                        eleve.put("moyenne_generale", NN);
                                    }
                                }
                                if(nbElevesMoyenne == 0){
                                    JsonObject minMoy = new JsonObject().put("minimum", NN);
                                    JsonObject moyenneJson = new JsonObject().put("moyenne_generale", minMoy);
                                    resultHandler.getJsonObject("annual").put("statistiques", moyenneJson);
                                    resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject("moyenne_generale").put("maximum", NN);
                                    resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject("moyenne_generale").put("moyenne", NN);
                                } else {
                                    JsonObject minMoy = new JsonObject().put("minimum", decimalFormat.format(min));
                                    JsonObject moyenneJson = new JsonObject().put("moyenne_generale", minMoy);
                                    resultHandler.getJsonObject("annual").put("statistiques", moyenneJson);
                                    resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject("moyenne_generale").put("maximum", decimalFormat.format(max));
                                    resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject("moyenne_generale").put("moyenne", decimalFormat.format(moyenneDeMoyenne / nbElevesMoyenne));
                                }
                                for(Object idM : idMatieres){
                                    String idMatiere = idM.toString();
                                    Double moyenne = 0.0;
                                    float moyennePos = (float) 0;
                                    nbElevesMoyenne = 0;
                                    int nbElevesMoyennePos = 0;
                                    min = 0.0;
                                    max = 0.0;
                                    float minPos = (float) 0;
                                    float maxPos = (float) 0;
                                    init = false;
                                    boolean initPos = false;
                                    for(Object el : resultHandler.getJsonObject("annual").getJsonArray("eleves")){
                                        JsonObject eleve = ((JsonObject) el);
                                        if(eleve.getJsonObject("moyenneFinale").containsKey(idMatiere) && eleve.getJsonObject("moyenneFinale").getValue(idMatiere) != null
                                                && !(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).equals(NN) || eleve.getJsonObject("moyenneFinale").getValue(idMatiere).equals(""))) {
                                            moyenne += Double.parseDouble(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",","."));
                                            nbElevesMoyenne++;
                                            if(!init){
                                                min = Double.parseDouble(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",","."));
                                                max =  Double.parseDouble(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",","."));
                                                init = true;
                                            }else{
                                                if(min >  Double.parseDouble(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",",".")))
                                                    min = Double.parseDouble(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",","."));
                                                if(max <  Double.parseDouble(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",",".")))
                                                    max = Double.parseDouble(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",","."));
                                            }
                                            eleve.getJsonObject("moyenneFinale").put(idMatiere, decimalFormat.format(Double.parseDouble(eleve.getJsonObject("moyenneFinale").getValue(idMatiere).toString().replace(",","."))));
                                        }
                                        if(eleve.getJsonObject("positionnement").containsKey(idMatiere) && eleve.getJsonObject("positionnement").getValue(idMatiere) != null
                                                && !(eleve.getJsonObject("positionnement").getValue(idMatiere).equals(NN) || eleve.getJsonObject("positionnement").getValue(idMatiere).equals(""))) {
                                            moyennePos += Float.parseFloat(eleve.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",","."));
                                            nbElevesMoyennePos++;
                                            if(!initPos){
                                                minPos = Float.parseFloat(eleve.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",","."));
                                                maxPos = Float.parseFloat(eleve.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",","."));
                                                initPos = true;
                                            }else{
                                                if(minPos >  Float.parseFloat(eleve.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",",".")))
                                                    minPos = Float.parseFloat(eleve.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",","."));
                                                if(maxPos <  Float.parseFloat(eleve.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",",".")))
                                                    maxPos = Float.parseFloat(eleve.getJsonObject("positionnement").getValue(idMatiere).toString().replace(",","."));
                                            }
                                        }
                                    }
                                    if(nbElevesMoyenne == 0){
                                        JsonObject minMoy = new JsonObject().put("minimum", NN);
                                        JsonObject moyenneMatJson = new JsonObject().put("moyenne", minMoy);
                                        if(resultHandler.getJsonObject("annual").getJsonObject("statistiques").containsKey(idMatiere))
                                            resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).put("moyenne", minMoy);
                                        else
                                            resultHandler.getJsonObject("annual").getJsonObject("statistiques").put(idMatiere, moyenneMatJson);
                                        resultHandler.getJsonObject("annual").getJsonObject("statistiques").put(idMatiere, moyenneMatJson);
                                        resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).getJsonObject("moyenne").put("maximum", NN);
                                        resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).getJsonObject("moyenne").put("moyenne", NN);
                                    }else{
                                        JsonObject minMoy = new JsonObject().put("minimum", decimalFormat.format(min));
                                        JsonObject moyenneMatJson = new JsonObject().put("moyenne", minMoy);
                                        if(resultHandler.getJsonObject("annual").getJsonObject("statistiques").containsKey(idMatiere))
                                            resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).put("moyenne", minMoy);
                                        else
                                            resultHandler.getJsonObject("annual").getJsonObject("statistiques").put(idMatiere, moyenneMatJson);
                                        resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).getJsonObject("moyenne").put("maximum", decimalFormat.format(max));
                                        resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).getJsonObject("moyenne").put("moyenne", decimalFormat.format(moyenne / nbElevesMoyenne));
                                    }
                                    if(nbElevesMoyennePos == 0){
                                        JsonObject minMoy = new JsonObject().put("minimum", NN);
                                        JsonObject moyenneMatJson = new JsonObject().put("positionnement", minMoy);
                                        if(resultHandler.getJsonObject("annual").getJsonObject("statistiques").containsKey(idMatiere))
                                            resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).put("positionnement", minMoy);
                                        else
                                            resultHandler.getJsonObject("annual").getJsonObject("statistiques").put(idMatiere, moyenneMatJson);
                                        resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).getJsonObject("positionnement").put("maximum", NN);
                                        resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).getJsonObject("positionnement").put("moyenne", NN);
                                    }else{
                                        JsonObject minMoy = new JsonObject().put("minimum", minPos);
                                        JsonObject moyenneMatJson = new JsonObject().put("positionnement",minMoy);
                                        if(resultHandler.getJsonObject("annual").getJsonObject("statistiques").containsKey(idMatiere))
                                            resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).put("positionnement", minMoy);
                                        else
                                            resultHandler.getJsonObject("annual").getJsonObject("statistiques").put(idMatiere, moyenneMatJson);
                                        resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).getJsonObject("positionnement").put("maximum", maxPos);
                                        resultHandler.getJsonObject("annual").getJsonObject("statistiques").getJsonObject(idMatiere).getJsonObject("positionnement")
                                                .put("moyenne", decimalFormat.format((moyennePos / nbElevesMoyennePos)));
                                    }
                                }

                                Renders.renderJson(request, resultHandler);
                            } else {
                                Renders.badRequest(request, "export of each periodes doesn't work");
                                if(exportPeriodeEvent.failed()) {
                                    log.error("ExportTotal annual (exportPeriodeEvent.failed()) : " + exportPeriodeEvent.cause());
                                }
                            }
                        } catch (Exception error) {
                            log.error("ExportTotal annual (create response): " + error);
                            Renders.badRequest(request, "ExportTotal annual failed : " + error);
                        }
                    });
                } catch (Exception error) {
                    log.error("ExportTotal annual (prepare data): " + error);
                    Renders.badRequest(request, "ExportTotal annual failed : " + error);
                }
            }
        });
    }

    @Get("/releve/export/checkDevoirs")
    @ApiDoc("Vérifie s'il y a des devoirs dans la matière")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void exportCheckDevoirs(final HttpServerRequest request) {
        final Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
        String idEtablissement = request.params().get("idEtablissement");
        String idClasse = request.params().get("idClasse");
        Long[] idPeriode = null;
        if(isNotNull(request.params().get("idPeriode")))
            idPeriode =new Long[]{Long.parseLong(request.params().get("idPeriode"))};
        if (!idEtablissement.equals("undefined") && !idClasse.equals("undefined")) {
            Long[] finalIdPeriode = idPeriode;
            Utils.getGroupesClasse(eb, new fr.wseduc.webutils.collections.JsonArray().add(idClasse), new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> responseQuerry) {
                    if (responseQuerry.isLeft()) {
                        String error = responseQuerry.left().getValue();
                        log.error(error);
                        handler.handle(new Either.Left<>(error));
                    } else {
                        JsonArray idClasseGroups = responseQuerry.right().getValue();
                        //List qui contient la idClasse + tous les ids groupes de la classe
                        if (idClasseGroups.isEmpty()) {
                            String[] idsGroups = new String[1];
                            idsGroups[0] = idClasse;
                            devoirsService.listDevoirs(null, idsGroups, null,
                                    finalIdPeriode, new String[]{idEtablissement}, null,
                                    null, false, handler);
                        } else {
                            String[] idsGroups = new String[idClasseGroups.getJsonObject(0)
                                    .getJsonArray("id_groupes").size() + 1];
                            idsGroups[0] = idClasseGroups.getJsonObject(0).getString("id_classe");
                            for (int i = 0; i < idClasseGroups.getJsonObject(0)
                                    .getJsonArray("id_groupes").size(); i++) {
                                idsGroups[i + 1] = idClasseGroups.getJsonObject(0)
                                        .getJsonArray("id_groupes").getString(i);
                            }
                            devoirsService.listDevoirs(null, idsGroups, null,
                                    finalIdPeriode, new String[]{idEtablissement}, null,
                                    null, false, handler);
                        }
                    }
                }
            });
        } else {
            Renders.badRequest(request, "Invalid parameters");
        }
    }

    @Get("/eleve/:idEleve/moyenne")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessChildrenParentFilter.class)
    @ApiDoc("Retourne la moyenne de l'élève dont l'id est passé en paramètre, sur les devoirs passés en paramètre")
    public void getMoyenneEleve(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    List<String> idDevoirsList = request.params().getAll("devoirs");
                    String idEleve = request.params().get("idEleve");
                    String idMatiere = request.params().get("idMatiere");
                    Long idPeriode = null;
                    if(request.params().get("idPeriode") != null)
                        idPeriode = Long.valueOf(request.params().get("idPeriode"));

                    Long[] idDevoirsArray = new Long[idDevoirsList.size()];

                    for (int i = 0; i < idDevoirsList.size(); i++) {
                        idDevoirsArray[i] = Long.parseLong(idDevoirsList.get(i));
                    }

                    // Récupération des moyennes finales
                    Future<JsonArray> moyenneFinaleFuture = Future.future();
                    notesService.getColonneReleve(new JsonArray().add(idEleve), idPeriode, idMatiere, null, "moyenne",
                            moyenneFinaleEvent -> formate(moyenneFinaleFuture, moyenneFinaleEvent));

                    // Récupération des notes des devoirs
                    Future<JsonArray> notesFuture = Future.future();
                    notesService.getNotesParElevesParDevoirs(new String[]{idEleve}, idDevoirsArray,
                            notesEvent -> formate(notesFuture, notesEvent));

                    Long finalIdPeriode = idPeriode;
                    CompositeFuture.all(notesFuture, moyenneFinaleFuture)
                            .setHandler(event -> {
                                if (event.failed()) {
                                    renderError(request, new JsonObject().put("error",request.params()));
                                }else {
                                    JsonArray notesEleve = notesFuture.result();
                                    JsonArray moyenneFinaleArray = moyenneFinaleFuture.result();
                                    List<NoteDevoir> notes = new ArrayList<>();

                                    if(!moyenneFinaleArray.isEmpty() && finalIdPeriode != null){
                                        JsonObject moyenneFinale = moyenneFinaleArray.getJsonObject(0);
                                        if(isNull(moyenneFinale.getValue("moyenne")))
                                            Renders.renderJson(request,new JsonObject().put("moyenne", "NN").put("hasNote", false));
                                        else
                                            Renders.renderJson(request, new JsonObject().put("moyenne", moyenneFinale.getValue("moyenne"))
                                                    .put("hasNote", true));

                                    }else{
                                        for (int i = 0; i < notesEleve.size(); i++) {
                                            JsonObject note = notesEleve.getJsonObject(i);
                                            if(note.getString("coefficient") != null) {
                                                notes.add(new NoteDevoir(Double.parseDouble(note.getString("valeur").replace(",",".")),
                                                        Double.parseDouble(note.getInteger("diviseur").toString().replace(",",".")),
                                                        note.getBoolean("ramener_sur"),
                                                        Double.parseDouble(note.getString("coefficient").replace(",","."))));
                                            }
                                        }
                                        Renders.renderJson(request, utilsService.calculMoyenne(notes, false, 20,false));
                                    }
                                }
                            });
                }
            }
        });
    }

    @Get("/eleve/:idEleve/moyenneFinale")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessChildrenParentFilter.class)
    @ApiDoc("Retourne les moyennes finales de l'élève sur la période")
    public void getMoyenneFinaleEleve(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    String idEleve = request.params().get("idEleve");
                    Long idPeriode = null;
                    if(request.params().get("idPeriode") != null)
                        idPeriode = Long.valueOf(request.params().get("idPeriode"));

                    notesService.getColonneReleve(new JsonArray().add(idEleve), idPeriode, null, null,
                            "moyenne", arrayResponseHandler(request));
                } else{
                    unauthorized(request);
                }
            }
        });
    }

    @Post("/bilanPeriodiqueWorkflow")
    @ApiDoc("Méthode crée uniquement pour gérer le droit workflow pour la méthode suivante : setElementProgramme / createAppreciationSubjectPeriod")
    @SecuredAction(value = "bilan.periodique.save.appMatiere.positionnement", type = ActionType.WORKFLOW)
    public void saveAppreciationMatiereAndPositionnementWorfklow(final HttpServerRequest request) {
        badRequest(request);
    }

    @Post("/releve/element/programme")
    @ApiDoc("Ajoute ou modifie un élément du programme")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SaveAppreciationBilanPeriodiqueFilter.class)
    public void setElementProgramme(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(final JsonObject resource) {
                        final String idClasse = resource.getString("idClasse");
                        final String idMatiere = resource.getString("idMatiere");
                        final Long idPeriode = resource.getLong("idPeriode");
                        final String texte = resource.getString("texte");

                        // Vérification de la date de fin de saisie
                        new FilterPeriodeUtils(eb, user).validateEndSaisie(request, idClasse, idPeriode.intValue(), isUpdatable -> {
                            //verif date fin de saisie
                            if (isUpdatable) {
                                elementProgramme.setElementProgramme(user.getUserId(), idPeriode, idMatiere,
                                        idClasse, texte, arrayResponseHandler(request));
                            } else {
                                log.error("Not access to API because of end of saisie");
                                unauthorized(request);
                            }
                        });
                    }
                });
            }
        });
    }

    @Post("/releve/periodique")
    @ApiDoc("Créé, met à jour ou supprime une donnée du relevé périodique pour un élève. Les données traitées ici sont:"
            +" - moyenne finale, -positionnement ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveByClasseMatiereFilter.class)
    public void setColonneRelevePeriode(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            RequestUtils.bodyToJson(request, resource -> {
                saveColonneRelevePeriode(request, user, resource);
            });
        });
    }

    private void saveColonneRelevePeriode(final HttpServerRequest request, final UserInfos user, final JsonObject resource){
        final String idClasse = resource.getString("idClasse");
        final String idMatiere = resource.getString("idMatiere");
        final String idEleve = resource.getString("idEleve");
        final String table = resource.getString("colonne");
        final Long idPeriode = resource.getLong("idPeriode");

        new FilterPeriodeUtils(eb, user).validateEndSaisie(request, idClasse, idPeriode.intValue(), isUpdatable -> {
            //verif date fin de saisie
            if (isUpdatable) {
                if (resource.getBoolean("delete")) {
                    notesService.deleteColonneReleve(idEleve, idPeriode, idMatiere, idClasse, table,
                            arrayResponseHandler(request));
                } else {
                    notesService.setColonneReleve(idEleve, idPeriode, idMatiere, idClasse, resource, table,
                            user.getUserId(), arrayResponseHandler(request));
                }
            } else {
                log.error("Not access to API because of end of saisie");
                unauthorized(request);
            }
        });
    }

    @Get("/releve/informations/eleve/:idEleve")
    @ApiDoc("Renvoit  les moyennes , les moyennes finales pour le relevé de notes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveByClasseMatiereFilter.class)
    public void getInfosEleve(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request,  user -> {
            final String idEtablissement = request.params().get("idEtablissement");
            final String idClasse = request.params().get("idClasse");
            final String idMatiere = request.params().get("idMatiere");
            final String idEleve = request.params().get("idEleve");

            new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere,false,
                    hasAccessToMatiere -> {
                        if (!hasAccessToMatiere) {
                            unauthorized(request);
                            return;
                        }
                        notesService.getDetailsReleve(idEleve, idClasse, idMatiere, idEtablissement, request);
                    });
        });
    }

    @Get("/releve/annee/classe")
    @ApiDoc("Renvoit  les moyennes , les moyennes finales pour le relevé de notes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveByClasseMatiereFilter.class)
    public void getReleveAnne(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                final String idEtablissement = request.params().get("idEtablissement");
                final List<String> classes = request.params().getAll("idClasse");
                final String idClasse = classes.toArray(new String[0])[0];
                final String idMatiere = request.params().get("idMatiere");

                final JsonObject action = new JsonObject()
                        .put("action", "classe.getElevesClasses")
                        .put("idClasses", new fr.wseduc.webutils.collections.JsonArray().add(idClasse));

                new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere, false,
                        new Handler<Boolean>() {
                            @Override
                            public void handle(final Boolean hasAccessToMatiere) {
                                Handler<Either<String, JsonArray>> handler = new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            final JsonObject result = new JsonObject();
                                            final JsonArray listNotes = event.right().getValue();

                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> message) {
                                                    JsonObject body = message.body();

                                                    if ("ok".equals(body.getString("status"))) {
                                                        final JsonObject result = new JsonObject();
                                                        final JsonArray idEleves = new fr.wseduc.webutils.collections.JsonArray();
                                                        JsonArray queryResult = body.getJsonArray("results");
                                                        result.put("moyennes", new fr.wseduc.webutils.collections.JsonArray());

                                                        for (int i = 0; i < queryResult.size(); i++) {
                                                            HashMap<Long,JsonArray> listMoyDevoirs = new HashMap<>();
                                                            HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>
                                                                    notesByDevoirByPeriode = new HashMap<>();

                                                            notesByDevoirByPeriode.put(null,
                                                                    new HashMap<Long, ArrayList<NoteDevoir>>());

                                                            JsonObject eleve = queryResult.getJsonObject(i);
                                                            String idEleve = eleve.getString("idEleve");
                                                            idEleves.add(idEleve);

                                                            for (int j = 0; j < listNotes.size(); j++) {
                                                                JsonObject note = listNotes.getJsonObject(j);

                                                                if (note.getString("valeur") == null ||
                                                                        note.getString("coefficient") == null ||
                                                                        !note.getBoolean("is_evaluated")) {

                                                                    continue; //Si la note fait partie d'un devoir qui n'est pas évalué,
                                                                    // elle n'est pas prise en compte dans le calcul de la moyenne
                                                                }
                                                                else {
                                                                    Long id_periode = note.getLong("id_periode");
                                                                    if(!notesByDevoirByPeriode.containsKey(id_periode)) {
                                                                        notesByDevoirByPeriode.put(id_periode,
                                                                                new HashMap<Long, ArrayList<NoteDevoir>>());

                                                                    }
                                                                    NoteDevoir noteDevoir = new NoteDevoir(
                                                                            Double.valueOf(note.getString("valeur")),
                                                                            Double.valueOf(note.getLong("diviseur")),
                                                                            note.getBoolean("ramener_sur"),
                                                                            Double.valueOf(note.getString("coefficient")));
                                                                    if(note.getString("id_eleve").equals(idEleve)) {
                                                                        utilsService.addToMap(id_periode,
                                                                                notesByDevoirByPeriode.get(id_periode),
                                                                                noteDevoir);
                                                                        utilsService.addToMap(null,
                                                                                notesByDevoirByPeriode.get(null),
                                                                                noteDevoir);
                                                                    }
                                                                }
                                                            }

                                                            // Calcul des moyennes par période pour L'élève
                                                            for(Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> entryPeriode
                                                                    : notesByDevoirByPeriode.entrySet()) {
                                                                listMoyDevoirs.put(entryPeriode.getKey(), new fr.wseduc.webutils.collections.JsonArray());
                                                                for (Map.Entry<Long, ArrayList<NoteDevoir>> entry :
                                                                        entryPeriode.getValue().entrySet()) {
                                                                    JsonObject moyenne = utilsService.calculMoyenne(
                                                                            entry.getValue(),
                                                                            false, 20, false);
                                                                    moyenne.put("id_periode", entry.getKey());
                                                                    moyenne.put("id_eleve", idEleve);
                                                                    listMoyDevoirs.get(entryPeriode.getKey()).add(moyenne);
                                                                }
                                                                if (listMoyDevoirs.get(entryPeriode.getKey()).size() > 0) {
                                                                    result.getJsonArray("moyennes").add(
                                                                            listMoyDevoirs.get(entryPeriode.getKey()).getJsonObject(0));
                                                                }
                                                            }
                                                        }

                                                        // On récupère les moyennes finales
                                                        notesService.getColonneReleve(
                                                                idEleves,
                                                                null,
                                                                idMatiere,
                                                                new JsonArray().add(idClasse),
                                                                "moyenne",
                                                                new Handler<Either<String, JsonArray>>() {
                                                                    @Override
                                                                    public void handle(Either<String, JsonArray> event) {
                                                                        if (event.isRight()) {
                                                                            result.put("moyennes_finales",event.right().getValue());
                                                                            Renders.renderJson(request, result);
                                                                        } else {
                                                                            JsonObject error = new JsonObject()
                                                                                    .put("error",
                                                                                            event.left().getValue());
                                                                            Renders.renderJson(request, error, 400);
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                }
                                            }));
                                        } else {
                                            JsonObject error = (new JsonObject()).put("error",
                                                    (String) event.left().getValue());
                                            Renders.renderJson(request, error, 400);
                                        }
                                    }
                                };
                                if (!hasAccessToMatiere) {
                                    unauthorized(request);
                                } else {
                                    notesService.getNoteElevePeriode(null,
                                            idEtablissement,
                                            new fr.wseduc.webutils.collections.JsonArray().add(idClasse),
                                            idMatiere,
                                            null, handler);
                                }
                            }
                        });
            }
        });
    }





    /**
     * Récupère les notes pour le bilan periodique
     *
     * @param request
     */
    @Get("/bilan/periodique/datas/graph")
    @ApiDoc("Récupère les données pour construire les graphs du bilan periodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessChildrenParentFilter.class)
    public void getBilanPeriodiqueDataForGraph(final HttpServerRequest request) {
        final String idEleve = request.params().get("idEleve");
        final String idEtablissement = request.params().get(Competences.ID_ETABLISSEMENT_KEY);
        final String idClasse = request.params().get("idClasse");
        final Integer typeClasse = Integer.valueOf(request.params().get("typeClasse"));
        final String idPeriodeString = request.params().get("idPeriode");
        Utils.getGroupsEleve(eb, idEleve, idEtablissement, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle( Either<String, JsonArray> responseQuerry) {
                if (!responseQuerry.isRight()) {
                    String error = responseQuerry.left().getValue();
                    log.error(error);
                    badRequest(request, error);
                } else {
                    JsonArray idGroups = responseQuerry.right().getValue();
                    //idGroups null si l'eleve n'est pas dans un groupe
                    notesService.getDataGraph(idEleve, idGroups, idEtablissement, idClasse, typeClasse,
                            idPeriodeString, isNull(idPeriodeString), arrayResponseHandler(request));
                }
            }
        });
    }

    @Get("/bilan/periodique/datas/graph/domaine")
    @ApiDoc("Récupère les données pour construire les graphs du bilan periodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessChildrenParentFilter.class)
    public void getBilanPeriodiqueDomaineForGraph(final HttpServerRequest request) {
        final String idEleve = request.params().get("idEleve");
        final String idEtablissement = request.params().get(Competences.ID_ETABLISSEMENT_KEY);
        final String idClasse = request.params().get("idClasse");
        final Integer typeClasse = Integer.valueOf(request.params().get("typeClasse"));
        final String idPeriodeString = request.params().get("idPeriode");
        new DefaultBilanPerioqueService(eb).getBilanPeriodiqueDomaineForGraph(idEleve, idEtablissement, idClasse,
                typeClasse, idPeriodeString, arrayResponseHandler(request));
    }

    /**
     * Récupère les notes pour le relevé de notes
     *
     * @param request
     */
    @Get("/releve/datas/graph")
    @ApiDoc("Récupère les données pour construire les graphs du relevé de notes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveByClasseMatiereFilter.class)
    public void getReleveDataForGraph(final HttpServerRequest request) {
        final String idEleve = request.params().get("idEleve");
        final String idEtablissement = request.params().get("idEtablissement");
        final String idClasse = request.params().get("idClasse");
        final Integer typeClasse = Integer.valueOf(request.params().get("typeClasse"));
        final String idPeriodeString = request.params().get("idPeriode");
        notesService.getDataGraph(idEleve, null, idEtablissement, idClasse, typeClasse, idPeriodeString,
                isNull(idPeriodeString),arrayResponseHandler(request));
    }

    /**
     * Récupère les notes pour le relevé de notes
     *
     * @param request
     */
    @Get("/releve/datas/graph/domaine")
    @ApiDoc("Récupère les données pour construire les graphs du relevé de notes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveByClasseMatiereFilter.class)
    public void getReleveDataDomaineForGraph(final HttpServerRequest request) {
        final String idEleve = request.params().get("idEleve");
        final String idEtablissement = request.params().get("idEtablissement");
        final String idClasse = request.params().get("idClasse");
        final Integer typeClasse = Integer.valueOf(request.params().get("typeClasse"));
        final String idPeriodeString = request.params().get("idPeriode");
        notesService.getDataGraphDomaine(idEleve, null, idEtablissement, idClasse, typeClasse, idPeriodeString,
                isNull(idPeriodeString),arrayResponseHandler(request));
    }

}



