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

package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.service.*;
import fr.openent.competences.helpers.ExportEvaluationHelper;
import fr.openent.competences.helpers.FormateFutureEvent;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Either.*;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.entcore.common.storage.Storage;

import javax.imageio.ImageIO;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Competences.RESULTS;
import static fr.openent.competences.Utils.*;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.openent.competences.helpers.ExportEvaluationHelper.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static fr.wseduc.webutils.http.Renders.badRequest;
import static fr.wseduc.webutils.http.Renders.getHost;
import static fr.wseduc.webutils.http.Renders.getScheme;

import java.text.DecimalFormat;

public class DefaultExportService implements ExportService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultExportService.class);
    public final static String  COEFFICIENT = "coefficient";
    private final static String DEVOIRS = "devoirs";
    private final static String PRINT_SOUS_MATIERE = "printSousMatiere";
    /**
     * Déclaration des services
     */
    private DevoirService devoirService;
    private UtilsService utilsService;
    private DomainesService domaineService;
    private CompetenceNoteService competenceNoteService;
    private NoteService noteService;
    private CompetencesService competencesService;
    private EnseignementService enseignementService;
    private NiveauDeMaitriseService niveauDeMaitriseService;
    private AnnotationService annotationsService;
    private EventBus eb;
    private final Storage storage;

    public DefaultExportService(EventBus eb, Storage storage) {
        this.eb = eb;
        devoirService = new DefaultDevoirService(eb);
        utilsService = new DefaultUtilsService(eb);
        domaineService = new DefaultDomaineService(Competences.COMPETENCES_SCHEMA, Competences.DOMAINES_TABLE);
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_NOTES_TABLE);
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE, eb);
        competencesService = new DefaultCompetencesService(eb);
        niveauDeMaitriseService = new DefaultNiveauDeMaitriseService();
        enseignementService = new DefaultEnseignementService(Competences.COMPETENCES_SCHEMA, Competences.ENSEIGNEMENTS_TABLE);
        annotationsService = new DefaultAnnotationService(Competences.COMPETENCES_SCHEMA, Competences.REL_ANNOTATIONS_DEVOIRS_TABLE);
        this.storage = storage;
    }

    @Override
    public void getExportCartouche (final MultiMap params , Handler<Either<String, JsonObject>> handler) {
        final String byEleves = params.get("eleve");
        final Boolean withResult = "true".equals(params.get("withResult"));
        JsonObject result = new JsonObject();
        int nbrCartouche;
        try {
            nbrCartouche = Integer.parseInt(params.get("nbr"));
        } catch (NumberFormatException e) {
            log.error("Error : to parse int nbrCartouche ", e.getMessage());
            handler.handle(new Either.Left<>("can't parse nbrCartouche " + e.getMessage()));
            return;
        }
        if (nbrCartouche > 0) {
            JsonArray nbr = new fr.wseduc.webutils.collections.JsonArray();
            for (int j = 0; j < nbrCartouche; j++) {
                nbr.add(j);
            }
            result.put("number", nbr);
        } else {
            result.put("number", new fr.wseduc.webutils.collections.JsonArray().add("cartouche"));
        }

        final Long idDevoir ;
        if (params.get("idDevoir") == null) {
            log.error("Error : idDevoir must be a long object");
            handler.handle(new Either.Left<>("Error : idDevoir must be a long object" ));
        }else{
            try {
                idDevoir = Long.parseLong(params.get("idDevoir"));
            } catch (NumberFormatException e) {
                log.error("Error : idDevoir must be a long object", e.getMessage());
                handler.handle(new Either.Left<>("can't parse idDevoir" + e.getMessage()));
                return;
            }

            devoirService.getDevoirInfo(idDevoir, devoirInfo -> {

                if (devoirInfo.isLeft()) {
                    handler.handle(new Either.Left<>("error to get devoir " + devoirInfo.left().getValue()));
                    log.error("error to get devoir id: "+idDevoir + " Message : "+devoirInfo.left().getValue());
                    return;
                }
                final JsonObject devoir = (JsonObject) ((Either.Right) devoirInfo).getValue();
                String idStructure = devoir.getString("id_etablissement");
                final String idClass = devoir.getString("id_groupe");

                Future<JsonObject> classInfoFuture = Future.future();
                utilsService.getClassInfo(idClass, event ->
                        FormateFutureEvent.formate(classInfoFuture,event));

                Future<JsonArray> competencesFuture = Future.future();
                if(devoir.getInteger("nbrcompetence")> 0){
                    competencesService.getDevoirCompetences(idDevoir, event ->
                            FormateFutureEvent.formate(competencesFuture, event));
                }else{
                    competencesFuture.complete(new JsonArray());
                }

                CompositeFuture.all(classInfoFuture,competencesFuture).setHandler( event -> {
                    if(event.failed()){
                        handler.handle(new Either.Left<>(event.cause().getMessage()));
                        log.error("error to get niveau de maitrise, classInfo and competence : "
                                + event.cause().getMessage());
                    }else {

                        JsonObject classInfo = classInfoFuture.result();
                        JsonArray competences = competencesFuture.result();
                        Map<String, JsonArray> mapResult = new HashMap<>();
                        if( classInfo.isEmpty() || competences.isEmpty() ){
                            handler.handle(new Either.Left<>("no classInfo or no competences"));
                            log.error("error : no classInfo or no competences");
                        }else {
                            mapResult.put("competences", competences);
                            Long idCycle = competences.getJsonObject(0).getLong("id_cycle");

                            niveauDeMaitriseService.getNiveauDeMaitrise(idStructure, idCycle,
                                    eventNivMaitrise -> {

                                        if(eventNivMaitrise.isLeft() || eventNivMaitrise.right().getValue().isEmpty()){
                                            handler.handle(new Either.Left<>("export : no level."));
                                            log.error("error : no level " + eventNivMaitrise.left().getValue());
                                        }else {
                                            mapResult.put("maitrises", eventNivMaitrise.right().getValue());

                                            if (!byEleves.equals("true")) {
                                                handler.handle(new Either.Right<>(buildExportCartouche(devoir,
                                                        params, result, byEleves,
                                                        withResult, classInfo, mapResult)));
                                            } else {
                                                JsonObject action = new JsonObject()
                                                        .put("action", "classe.getEleveClasse")
                                                        .put("idClasse", devoir.getString("id_groupe"))
                                                        .put("idPeriode", devoir.getInteger("id_periode"));

                                                eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                                        handlerToAsyncHandler( message -> {

                                                            JsonObject body = message.body();

                                                            if ("ok".equals(body.getString("status")) &&
                                                                    !body.getJsonArray("results").isEmpty()) {

                                                                JsonArray eleves = body.getJsonArray("results");
                                                                mapResult.put("eleves", eleves);
                                                                if (!withResult) {
                                                                    handler.handle(new Either.Right<>(
                                                                            buildExportCartouche(devoir, params, result, byEleves,
                                                                                    withResult, classInfo, mapResult)));

                                                                } else {

                                                                    getResultsEleves(idDevoir,
                                                                            idStructure, mapResult,
                                                                            eventResultsEleve -> {

                                                                                if (eventResultsEleve.isRight()) {
                                                                                    if (eventResultsEleve.right().getValue()) {
                                                                                        handler.handle(new Either.Right<>(
                                                                                                buildExportCartouche(
                                                                                                        devoir, params,
                                                                                                        result,byEleves,
                                                                                                        withResult, classInfo,
                                                                                                        mapResult)));
                                                                                    } else {
                                                                                        handler.handle(new Either.Left<>(
                                                                                                "exportCartouche : empty result."));
                                                                                    }
                                                                                } else {
                                                                                    handler.handle(new Either.Left<>(
                                                                                            "error to get competencesNotes" +
                                                                                                    " or notes or annotations "));
                                                                                    log.error("error to get competencesNotes"+
                                                                                            " or notes or annotations "
                                                                                            + eventResultsEleve.left().getValue());
                                                                                }

                                                                            });
                                                                }
                                                            } else {
                                                                handler.handle(new Either.Left<>(
                                                                        "errorCartouche : can not get students"));
                                                                log.error("errorCartouche : can not get students "
                                                                        + body.getString(MESSAGE));
                                                            }
                                                        }));
                                            }
                                        }
                                    });
                        }
                    }

                });
            });

        }
    }

    private void getResultsEleves(Long idDevoir, String idStructure, Map mapResult, Handler<Either<String,Boolean>> handler){

        Future<JsonArray> competencesNotesFuture = Future.future();
        competenceNoteService.getCompetencesNotesDevoir(idDevoir, event -> {
            FormateFutureEvent.formate(competencesNotesFuture, event);
        });

        Future<JsonArray> annotationsFuture = Future.future();
        annotationsService.listAnnotations(idStructure, event -> {
            FormateFutureEvent.formate(annotationsFuture, event);
        });

        Future<JsonArray> notesFuture = Future.future();
        noteService.listNotesParDevoir(idDevoir, event -> {
            FormateFutureEvent.formate(notesFuture, event);
        });

        CompositeFuture.all(competencesNotesFuture, annotationsFuture, notesFuture)
                .setHandler(new Handler<AsyncResult<CompositeFuture>>() {
                    @Override
                    public void handle (AsyncResult<CompositeFuture> event) {
                        if(event.succeeded()){
                            //notes = note, annotations et appreciation
                            JsonArray notes = notesFuture.result();
                            JsonArray competencesNotes = competencesNotesFuture.result();
                            JsonArray annotations = annotationsFuture.result();

                            if( competencesNotes.isEmpty() && notes.isEmpty() || annotations.isEmpty() && !notes.isEmpty()) {
                                handler.handle( new Either.Right<>(false));
                            }else {

                                mapResult.put("notes",notes);
                                mapResult.put("competencesNotes",competencesNotes);
                                mapResult.put("annotations", annotations);
                                handler.handle(new Either.Right<>(true));
                            }

                        } else {
                            handler.handle(new Either.Left<>("Error : to get notes , annotations and competencesNotes : "+
                                    event.cause().getMessage()));
                            log.error("Error : to get notes , annotations and competencesNotes : "+
                                    event.cause().getMessage());
                        }
                    }
                });

    };
    private JsonObject buildExportCartouche(JsonObject devoir, MultiMap params,
                                            JsonObject result, String byEleves,
                                            Boolean withResult, JsonObject classInfo,
                                            Map<String,JsonArray> mapResult ){

        JsonArray maitrises = mapResult.get("maitrises");
        JsonArray competences = mapResult.get("competences");
        final String color = params.get("color");
        final boolean withAppreciations = "true".equals(params.get("withAppreciations"));

        //commun à tous les types export cartouche sans/avec eleves et sans/avec resultat

        result.put("byEleves", "true".equals(byEleves));
        result.put("byColor", "true".equals(color));

        result.put("evaluation", devoir.getBoolean("is_evaluated"));
        result.put("nameClass", classInfo.getString("name"));
        result.put("niveaux", maitrises);
        result.put("withResult", withResult);
        result.put("devoirName", devoir.getString("name"));


        if(devoir.getInteger("nbrcompetence") > 0 && !competences.isEmpty()){
            JsonArray CompetencesNew = new fr.wseduc.webutils.collections.JsonArray();
            for (int i = 0; i < competences.size(); i++) {
                JsonObject Comp = competences.getJsonObject(i);
                Comp.put("i", i + 1);
                if (i == 0) {
                    Comp.put("first", true);
                } else {
                    Comp.put("first", false);
                }
                CompetencesNew.add(Comp);
            }
            result.put("competences", CompetencesNew);
        }
        result.put("nbrCompetences", String.valueOf(competences.size()));
        result.put("hasCompetences", devoir.getInteger("nbrcompetence") > 0);

        if( result.getBoolean("byEleves")){
            JsonArray eleves = mapResult.get("eleves");
            eleves = Utils.sortElevesByDisplayName(eleves);
            if(devoir.getInteger("nbrcompetence") > 0 && !competences.isEmpty() && withResult) {
                JsonArray evaluatedCompetences = result.getJsonArray("competences");
                result.remove("competences");
                Map<String, JsonObject> mapAnnotations = extractData(mapResult.get("annotations"),ID_KEY);
                Map<String, JsonObject> mapNotesEleves = extractData(mapResult.get("notes"),"id_eleve");
                Map<String, Map<String, JsonObject>> competencesNotesElevesMap = new HashMap<>();
                if (!(mapResult.get("competencesNotes")).isEmpty()) {
                    for (int j = 0; j < mapResult.get("competencesNotes").size(); j++) {
                        JsonObject competenceNote = mapResult.get("competencesNotes").getJsonObject(j);
                        if (!competencesNotesElevesMap.containsKey(competenceNote.getString("id_eleve"))) {
                            competencesNotesElevesMap.put(
                                    competenceNote.getString("id_eleve"),
                                    new HashMap<String, JsonObject>());
                        }
                        competencesNotesElevesMap.get(competenceNote.getString("id_eleve"))
                                .put(String.valueOf(competenceNote.getLong("id_competence")), competenceNote);
                    }
                }

                eleves.stream().forEach(eleve -> {
                    JsonObject eleve_jo = (JsonObject)eleve;
                    String note = "";
                    boolean hasAnnotation = false;
                    boolean hasAppreciation = false;
                    if (mapNotesEleves.containsKey((eleve_jo.getString(ID_KEY)))) {
                        JsonObject noteEleve = mapNotesEleves.get(eleve_jo.getString(ID_KEY));
                        if (isNotNull(noteEleve.getLong("id_annotation"))) {
                            note = mapAnnotations.get(String.valueOf(
                                    noteEleve.getLong("id_annotation"))).getString("libelle_court");
                            hasAnnotation = true;

                        } else {
                            note = noteEleve.getString("valeur");
                        }
                        eleve_jo.put("note", note);

                        if (isNotNull(noteEleve.getString("appreciation")) && withAppreciations) {
                            result.put("colspanAppreciation", devoir.getBoolean("is_evaluated") ?
                                    result.getJsonArray("niveaux").size() + 3 :
                                    result.getJsonArray("niveaux").size()+ 2);
                            hasAppreciation = true;

                            eleve_jo.put("appreciation", noteEleve.getString("appreciation"));
                        }
                    }
                    eleve_jo.put("hasAnnotation", hasAnnotation);
                    eleve_jo.put("showAppreciation", hasAppreciation);

                    JsonArray competencesNotesElevesArray = new JsonArray();
                    evaluatedCompetences.stream().forEach(evaluatedCompetence -> {
                        String id_competenceEvaluated = String.
                                valueOf(((JsonObject)evaluatedCompetence).getLong("id_competence"));
                        JsonObject competenceNoteEleveResult = new JsonObject();
                        competenceNoteEleveResult.put("code_domaine",
                                ((JsonObject)evaluatedCompetence).getString("code_domaine"));
                        competenceNoteEleveResult.put("nom", ((JsonObject)evaluatedCompetence).getString("nom"));
                        competenceNoteEleveResult.put("first", ((JsonObject)evaluatedCompetence).getBoolean("first"));
                        JsonArray niveauxEleve = new fr.wseduc.webutils.collections.JsonArray();
                        String idEleve = eleve_jo.getString(ID_KEY);
                        if (!competencesNotesElevesMap.containsKey(idEleve)
                                || competencesNotesElevesMap.containsKey(idEleve)
                                && !competencesNotesElevesMap.get(idEleve).containsKey(id_competenceEvaluated)
                                || competencesNotesElevesMap.containsKey(idEleve)
                                && competencesNotesElevesMap.get(idEleve).containsKey(id_competenceEvaluated)
                                && competencesNotesElevesMap.get(idEleve).get(id_competenceEvaluated).getInteger("evaluation") == -1) {
                            niveauxEleve.add(true);
                            result.getJsonArray("niveaux").stream().forEach(niveau ->{
                                niveauxEleve.add(false);
                            });
                            competenceNoteEleveResult.put("niveauxEleve", niveauxEleve);

                        }else {
                            Map<String, JsonObject> mapCompetencesNotesEleve = competencesNotesElevesMap.get(idEleve);

                            if (mapCompetencesNotesEleve.containsKey(id_competenceEvaluated)) {

                                JsonObject competenceNoteEleveMap =
                                        mapCompetencesNotesEleve.get(String.
                                                valueOf(((JsonObject)evaluatedCompetence).getLong("id_competence")));
                                niveauxEleve.add(false);
                                Integer niveauEleve = competenceNoteEleveMap.getInteger("evaluation");

                                for (int k = 0; k < result.getJsonArray("niveaux").size(); k++) {

                                    JsonObject niveau = result.getJsonArray("niveaux").getJsonObject(k);
                                    niveauxEleve.add(niveauEleve + 1 == niveau.getInteger("ordre"));
                                }
                                competenceNoteEleveResult.put("niveauxEleve", niveauxEleve);
                            }
                        }
                        competencesNotesElevesArray.add(competenceNoteEleveResult);
                    });
                    eleve_jo.put("competences", competencesNotesElevesArray);
                });
                result.put("eleves", eleves);
            }else{
                result.put("eleves", eleves);
            }
        }else{
            result.put("image", Boolean.parseBoolean(params.get("image")));
        }
        return result;

    }

    @Override
    public void getExportEval(final Boolean text, final Boolean usePerso, final Boolean onlyEvaluation, final JsonObject devoir,
                              String idGroupe, final String idEtablissement, HttpServerRequest request,
                              final Handler<Either<String, JsonObject>> handler) {

        List<Future> exportDatas = new ArrayList<>();
        Long idDevoir = devoir.getLong(ID_KEY);
        HashMap devoirMap = new HashMap();
        ExportEvaluationHelper.formatDevoir(devoir, getHost(request), I18n.acceptLanguage(request),devoirMap);
        exportDatas.add(getClasseDevoir(devoir , devoirMap, eb));
        exportDatas.add(getMatiereDevoir(devoir , devoirMap, eb));

        JsonArray maitrises = new JsonArray();
        Future  maitriseFuture = getNiveauDeMaitriseDevoir(devoir, onlyEvaluation, maitrises);
        exportDatas.add(maitriseFuture);

        final JsonArray elevesArray = new JsonArray();
        Future eleveFuture = getStudents(devoir, eb, idGroupe, elevesArray);
        exportDatas.add(eleveFuture);

        JsonArray notesArray = new JsonArray();
        Future noteFuture = listNotes(idDevoir, eb, notesArray);
        exportDatas.add(noteFuture);

        JsonArray annotationsArray = new JsonArray();
        Future annotationF = listAnnotations(idEtablissement, annotationsArray);
        exportDatas.add(annotationF);

        JsonArray competencesArray = new JsonArray();
        Future competenceFuture = getDevoirCompetences(idDevoir, onlyEvaluation, eb, competencesArray);
        exportDatas.add(competenceFuture);

        JsonArray cmpNotesAr = new JsonArray();
        Future compNotesFuture = getCompetencesNotesDevoir(idDevoir, onlyEvaluation, cmpNotesAr);
        exportDatas.add(compNotesFuture);

        CompositeFuture.all(exportDatas).setHandler(event -> {
            if(event.failed()){
                returnFailure("getExportEval", event, handler);
            }
            else {
                final Map<String, Map<String, JsonObject>> compNoteElevesMap  = new HashMap<>();
                for (int i = 0; i < cmpNotesAr.size(); i++) {
                    JsonObject compNote = cmpNotesAr.getJsonObject(i);
                    String idEleve = compNote.getString(ID_ELEVE);
                    if (!compNoteElevesMap.containsKey(idEleve)) {
                        compNoteElevesMap.put(idEleve, new HashMap<>());
                    }
                    compNoteElevesMap.get(idEleve).put(String.valueOf(compNote.getLong("id_competence")), compNote);
                }

                handler.handle(new Either.Right<>(ExportEvaluationHelper.formatJsonObjectExportDevoir(text, usePerso,
                        new JsonObject(devoirMap),
                        extractData(Utils.sortElevesByDisplayName(elevesArray), ID_ELEVE_KEY),
                        extractData(orderBy(addMaitriseNE(maitrises), ORDRE, true), ORDRE),
                        extractData(competencesArray, "id_competence"),
                        extractData(notesArray, ID_ELEVE),
                        extractData(annotationsArray, ID_KEY),
                        compNoteElevesMap)));
            }
        });
    }

    private  void buildDevoirExport(final Boolean pByEnseignement, final String idEleve, final String[] idGroupes,
                                    String[] idFunctionalGroupes, final String idEtablissement,
                                    final List<String> idMatieres, Long idPeriodeType, Boolean isCycle,
                                    final JsonArray enseignementArray, final JsonArray devoirsArray,
                                    final JsonArray competencesArray, final JsonArray domainesArray,
                                    final JsonArray competencesNotesArray, String[] idMatieresTab,
                                    final Handler<Either<String, JsonArray>> finalHandler){


        //on recupere la liste des devoirs des classes mais aussi des groupes de l'eleve
        final List<String> idClasseAndFunctionnalGroups = new ArrayList<>();
        Collections.addAll(idClasseAndFunctionnalGroups, idGroupes);
        Collections.addAll(idClasseAndFunctionnalGroups, idFunctionalGroupes);

        devoirService.listDevoirs(idEleve, idClasseAndFunctionnalGroups.toArray(new String[0]), null,
                idPeriodeType != null ? new Long[]{idPeriodeType} : null,
                idEtablissement != null ? new String[]{idEtablissement} : null,
                idMatieres != null ? idMatieresTab : null, null, isCycle,
                getIntermediateHandler(devoirsArray,  stringJsonArrayEither -> {
                    if (stringJsonArrayEither.isRight() && isNotNull(stringJsonArrayEither.right().getValue()) &&
                            !stringJsonArrayEither.right().getValue().isEmpty() &&
                            !(stringJsonArrayEither.right().getValue().getValue(0) instanceof String)) {
                        JsonArray idDevoirs = new JsonArray();
                        for (int i = 0; i < stringJsonArrayEither.right().getValue().size(); i++) {
                            Long idDevoir = stringJsonArrayEither.right().getValue().getJsonObject(i).getLong(ID_KEY);
                            idDevoirs.add(idDevoir);
                        }

                        if (pByEnseignement) {
                            competencesService.getDevoirCompetencesByEnseignement(idDevoirs,
                                    getIntermediateHandler(idDevoirs, competencesArray, finalHandler));
                        } else {
                            competencesService.getDevoirCompetences(idDevoirs, idEtablissement,
                                    getIntermediateHandler(idDevoirs, competencesArray, finalHandler));
                        }
                        competenceNoteService.getCompetencesNotes(idDevoirs, idEleve,
                                true,
                                getIntermediateHandler(idDevoirs, competencesNotesArray, finalHandler));
                        domaineService.getDomainesRacines(idGroupes[0], null,
                                getIntermediateHandler(domainesArray, finalHandler));
                        enseignementService.getEnseignementsOrdered(
                                getIntermediateHandler(enseignementArray, finalHandler));
                    } else if (isNotNull(stringJsonArrayEither.right()) &&
                            isNotNull(stringJsonArrayEither.right().getValue()) &&
                            !stringJsonArrayEither.right().getValue().isEmpty() &&
                            stringJsonArrayEither.right().getValue().getValue(0) instanceof String) {
                        if (pByEnseignement) {
                            competencesService.getDevoirCompetencesByEnseignement((Long) null,
                                    getIntermediateHandler((Long)null, competencesArray, finalHandler));
                        } else {
                            competencesService.getDevoirCompetences(null,
                                    getIntermediateHandler((Long)null, competencesArray, finalHandler));
                        }
                        competenceNoteService.getCompetencesNotes((Long)null, idEleve,true,
                                getIntermediateHandler((Long)null, competencesNotesArray, finalHandler));
                        domaineService.getDomainesRacines(idGroupes[0], null,
                                getIntermediateHandler(domainesArray, finalHandler));
                        enseignementService.getEnseignementsOrdered(
                                getIntermediateHandler(enseignementArray, finalHandler));
                    } else {
                        String error = stringJsonArrayEither.left().getValue();
                        log.error("getExportReleveComp | buildDevoirExport " + error);
                        if(error.contains("Timeout") || error.contains("Timed out")){
                            log.info(" reset buildDevoirExport");
                            buildDevoirExport(pByEnseignement, idEleve, idGroupes, idFunctionalGroupes, idEtablissement,
                                    idMatieres, idPeriodeType, isCycle, enseignementArray, devoirsArray,
                                    competencesArray, domainesArray, competencesNotesArray, idMatieresTab, finalHandler);
                        }
                        else {
                            finalHandler.handle(stringJsonArrayEither.left());
                        }
                    }
                }));
    }

    private void buildNiveauReleveComp(final String[] idGroupes, final String idEtablissement, JsonArray maitriseArray,
                                       final Handler<Either<String, JsonArray>> finalHandler){
        utilsService.getCycle(Arrays.asList(idGroupes),  stringJsonArrayEither -> {
            if (stringJsonArrayEither.isRight() && isNotNull(stringJsonArrayEither.right().getValue()) &&
                    !stringJsonArrayEither.right().getValue().isEmpty()) {
                Long idCycle = new Long(stringJsonArrayEither.right().getValue().getJsonObject(0)
                        .getLong("id_cycle"));

                for (int i = 0; i < stringJsonArrayEither.right().getValue().size(); i++) {
                    JsonObject cycleObj = stringJsonArrayEither.right().getValue().getJsonObject(i);
                    if (!idCycle.equals(cycleObj.getLong("id_cycle"))) {
                        finalHandler.handle(new Either.Left<String, JsonArray>(
                                "getExportReleveComp : Given groups belong to different cycle."));
                    }
                }
                niveauDeMaitriseService.getNiveauDeMaitrise(idEtablissement, idCycle,
                        getIntermediateHandler(maitriseArray, finalHandler));
            } else {
                String error = stringJsonArrayEither.left().getValue();
                log.error("getExportReleveComp | getCycle " + error);
                if(error.contains("Timeout") || error.contains("Timed out")){
                    log.info(" reset getCycle");
                    buildNiveauReleveComp(idGroupes, idEtablissement, maitriseArray, finalHandler);
                }
                else {
                    finalHandler.handle(new Either.Left<>(error));
                }
            }
        });
    }

    @Override
    public void getExportReleveComp(final Boolean text, final Boolean usePerso, final Boolean pByEnseignement,
                                    final String idEleve, final String[] idGroupes, String[] idFunctionalGroupes,
                                    final String idEtablissement, final List<String> idMatieres,
                                    Long idPeriodeType, Boolean isCycle, final Handler<Either<String, JsonObject>> handler) {
        final JsonArray maitriseArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray enseignementArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray devoirsArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray competencesArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray domainesArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray competencesNotesArray = new fr.wseduc.webutils.collections.JsonArray();
        String[] idMatieresTab = idMatieres.toArray(new String[0]);
        final AtomicBoolean answered = new AtomicBoolean();
        final AtomicBoolean byEnseignement = new AtomicBoolean(pByEnseignement);
        final Handler<Either<String, JsonArray>> finalHandler = getReleveCompFinalHandler(text, usePerso, idEleve, devoirsArray,
                maitriseArray, competencesArray, domainesArray, competencesNotesArray, enseignementArray, answered,
                byEnseignement, handler);

        buildDevoirExport(pByEnseignement, idEleve, idGroupes, idFunctionalGroupes, idEtablissement,
                idMatieres, idPeriodeType, isCycle, enseignementArray, devoirsArray,
                competencesArray, domainesArray, competencesNotesArray, idMatieresTab, finalHandler);

        buildNiveauReleveComp(idGroupes, idEtablissement, maitriseArray, finalHandler);
    }

    @Override
    public void getExportRecapEval(final Boolean text, final Boolean usePerso, final Long idCycle,
                                   final String idEtablissement, final Handler<Either<String, JsonArray>> handler) {

        niveauDeMaitriseService.getNiveauDeMaitrise(idEtablissement, idCycle, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isRight()) {
                    JsonArray legende = new fr.wseduc.webutils.collections.JsonArray();
                    JsonArray result = stringJsonArrayEither.right().getValue();
                    for (int i = result.size() - 1; i >= 0; i--) {
                        JsonObject niveau = new JsonObject();
                        JsonObject o = result.getJsonObject(i);

                        niveau.put("libelle",  o.getString("libelle") != null
                                ? o.getString("libelle") : o.getString("default_lib"));

                        niveau.put("visu", text ? getMaitrise(o.getString("lettre"),
                                o.getInteger(ORDRE).toString()) : o.getString("default"));
                        niveau.put(ORDRE, o.getInteger(ORDRE));

                        if(usePerso && !text)
                            niveau.put("persoColor", o.getString("couleur"));

                        legende.add(niveau);
                    }
                    handler.handle(new Either.Right<>(legende));
                } else {
                    handler.handle(new Either.Left<>("exportRecapEval : empty result."));
                }
            }
        });
    }

    private Handler<Either<String, JsonArray>>
    getIntermediateHandler(final JsonArray collection, final Handler<Either<String, JsonArray>> finalHandler) {
        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isRight()) {
                    JsonArray result = stringJsonArrayEither.right().getValue();
                    if(isNull(result)){
                        result = new JsonArray();
                    }
                    if (result.size() == 0) {
                        result.add("empty");
                    }
                    utilsService.saUnion(collection, result);
                }
                finalHandler.handle(stringJsonArrayEither);
            }
        };
    }

    private Handler<Either<String, JsonArray>>
    getIntermediateHandler(final Long idDevoir, final JsonArray collection,
                           final Handler<Either<String, JsonArray>> finalHandler) {
        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isRight()) {
                    JsonArray result = stringJsonArrayEither.right().getValue();
                    if (result.size() == 0 && idDevoir != null) {
                        JsonObject obj = new JsonObject();
                        obj.put("id_devoir", idDevoir);
                        obj.put("empty", true);
                        result.add(obj);
                    }
                }
                getIntermediateHandler(collection, finalHandler).handle(stringJsonArrayEither);
            }
        };
    }
    private Handler<Either<String, JsonArray>>
    getIntermediateHandler(final JsonArray idDevoirs, final JsonArray collection,
                           final Handler<Either<String, JsonArray>> finalHandler) {
        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isRight()) {
                    for(int i=0; i<idDevoirs.size(); i++) {
                        long idDevoir = idDevoirs.getLong(i);
                        JsonArray res = stringJsonArrayEither.right().getValue();
                        if (res.size() == 0 && isNotNull(idDevoir)) {
                            JsonObject obj = new JsonObject();
                            obj.put("id_devoir", idDevoir);
                            obj.put("empty", true);
                            res.add(obj);
                        }
                    }
                }
                getIntermediateHandler(collection, finalHandler).handle(stringJsonArrayEither);
            }
        };
    }
    private int getNbDiffKey(JsonArray collection, String key) {
        Set<String> keyShown = new HashSet<>();
        int jsonShown = 0;
        for (int i = 0; i < collection.size(); i++) {
            if (collection.getValue(i) instanceof JsonObject) {
                JsonObject row = collection.getJsonObject(i);
                String keyValue = String.valueOf(row.getValue(key));
                if (!keyShown.contains(keyValue)) {
                    keyShown.add(keyValue);
                }
            } else {
                jsonShown++;
            }
        }
        return keyShown.size() + jsonShown;
    }


    private Handler<Either<String, JsonArray>>
    getReleveCompFinalHandler(final Boolean text, final Boolean usePerso, final String idEleve, final JsonArray devoirs,
                              final JsonArray maitrises, final JsonArray competences,
                              final JsonArray domaines, final JsonArray competencesNotes, final JsonArray enseignements,
                              final AtomicBoolean answered, final AtomicBoolean byEnseignement,
                              final Handler<Either<String, JsonObject>> responseHandler) {
        final AtomicBoolean devoirsDone = new AtomicBoolean();
        final AtomicBoolean maitriseDone = new AtomicBoolean();
        final AtomicBoolean competencesDone = new AtomicBoolean();
        final AtomicBoolean domainesDone = new AtomicBoolean();
        final AtomicBoolean enseignementsDone = new AtomicBoolean();
        final AtomicBoolean competencesNotesDone = new AtomicBoolean();

        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (!answered.get()) {
                    if (stringJsonArrayEither.isRight()) {

                        devoirsDone.set(devoirs.size() > 0);
                        maitriseDone.set(maitrises.size() > 0);
                        domainesDone.set(domaines.size() > 0);
                        enseignementsDone.set(enseignements.size() > 0);
                        competencesDone.set(competences.size() > 0);
                        competencesNotesDone.set(competencesNotes.size() > 0);

                        if (devoirsDone.get() && maitriseDone.get() && ((domainesDone.get() && !byEnseignement.get())
                                || (byEnseignement.get() && enseignementsDone.get()))
                                && competencesDone.get() && competencesNotesDone.get()
                        ) {
                            answered.set(true);

                            if (maitrises.contains("empty")) {
                                responseHandler.handle(new Either.Left<>("devoirs not found"));
                            } else if (domaines.contains("empty") && !byEnseignement.get()) {
                                responseHandler.handle(new Either.Left<>("domaines not found"));
                            } else if (enseignements.contains("empty") && byEnseignement.get()) {
                                responseHandler.handle(new Either.Left<>("enseignements not found"));
                            } else {
                                if (!devoirs.contains("empty")) {
                                    Map<String, Map<String, Long>> competenceNotesMap = new HashMap<>();

                                    for (int i = 0; i < competencesNotes.size(); i++) {
                                        if (competencesNotes.getJsonObject(i) instanceof JsonObject) {
                                            JsonObject row = competencesNotes.getJsonObject(i);
                                            if (row.containsKey("empty")) {
                                                continue;
                                            }
                                            String compKey = String.valueOf(row.getLong("id_competence"));
                                            String devoirKey = String.valueOf(row.getLong("id_devoir"));
                                            Long eval = row.getLong("evaluation");
                                            Long niv_final = row.getLong("niveau_final");
                                            if (!competenceNotesMap.containsKey(devoirKey)) {
                                                competenceNotesMap.put(devoirKey, new HashMap<>());
                                            }
                                            if (!competenceNotesMap.get(devoirKey).containsKey(compKey)) {
                                                if (eval != null || niv_final != null) {
                                                    competenceNotesMap.get(devoirKey).put(compKey,
                                                            (niv_final != null) ? niv_final : eval);
                                                }
                                            }
                                        }
                                    }

                                    List<String> devoirsList = new ArrayList<>(extractData(devoirs, ID_KEY).keySet());
                                    Map<String, JsonObject> maitrisesMap = extractData(
                                            orderBy(addMaitriseNE(maitrises), ORDRE, true), ORDRE);
                                    Map<String, JsonObject> competencesMap = extractData(competences, ID_KEY);
                                    Map<String, JsonObject> domainesMap = extractData(domaines, ID_KEY);
                                    Map<String, JsonObject> enseignementsMap = extractData(enseignements, ID_KEY);

                                    JsonObject resToAdd = formatJsonObjectExportReleveComp(
                                            text, usePerso, Boolean.valueOf(byEnseignement.get()), idEleve, devoirsList,
                                            maitrisesMap, competencesMap, domainesMap,
                                            enseignementsMap,
                                            competenceNotesMap)
                                            .put("noDevoir", false);

                                    responseHandler.handle(new Either.Right<>(resToAdd));
                                } else {
                                    responseHandler.handle(new Either.Right<>(
                                            new JsonObject().put("text", text)
                                                    .getJsonObject("header", new JsonObject())
                                                    .put("noDevoir", true)
                                                    .put("idEleve", idEleve)

                                    ));
                                }
                            }
                        }
                    } else {
                        answered.set(true);
                        responseHandler.handle(
                                new Either.Left<String, JsonObject>(stringJsonArrayEither.left().getValue()));
                    }
                }
            }
        };
    }


    private JsonObject formatJsonObjectExportReleveComp(Boolean text, Boolean usePerso, Boolean byEnseignement,
                                                        String idEleve, List<String> devoirs,
                                                        Map<String, JsonObject> maitrises,
                                                        Map<String, JsonObject> competences,
                                                        Map<String, JsonObject> domaines,
                                                        Map<String, JsonObject> enseignements,
                                                        Map<String, Map<String, Long>> competenceNotesByDevoir) {

        JsonObject result = new JsonObject();
        result.put("text", text);
        result.put("idEleve", idEleve);

        JsonObject header = new JsonObject();
        JsonObject body = new JsonObject();

        //Maitrise
        JsonArray headerMiddle = new fr.wseduc.webutils.collections.JsonArray();
        for (JsonObject maitrise : maitrises.values()) {
            JsonObject _maitrise = new JsonObject()
                    .put("libelle", maitrise.getString("libelle") != null
                            ? maitrise.getString("libelle") : maitrise.getString("default_lib"))
                    .put("visu", text ? getMaitrise(maitrise
                            .getString("lettre"), String.valueOf(maitrise
                            .getLong(ORDRE))) : maitrise.getString("default"));
            if(usePerso && !text)
                _maitrise.put("persoColor", maitrise.getString("couleur"));

            headerMiddle.add(_maitrise);
        }
        header.put("right", headerMiddle);
        result.put("header", header);

        final Map<String, JsonObject> competencesObjByIdComp = new HashMap<>();

        Map<String, Set<String>> competencesByDomainOrEnsei = new LinkedHashMap<>();
        for (String idEntity : (byEnseignement) ? enseignements.keySet() : domaines.keySet()) {
            competencesByDomainOrEnsei.put(idEntity, new TreeSet<String>(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    String s1 = competencesObjByIdComp.get(o1).getString("nom");
                    String s2 = competencesObjByIdComp.get(o2).getString("nom");
                    return s1.compareTo(s2);
                }
            }));
        }

        Map<String, List<String>> devoirByCompetences = new HashMap<>();
        for (JsonObject competence : competences.values()) {
            competencesObjByIdComp.put(String.valueOf(competence.getLong("id_competence")), competence);

            if (competence.containsKey("empty")) {
                continue;
            }
            String idDevoir = String.valueOf(competence.getLong("id_devoir"));
            String idComp = String.valueOf(competence.getLong("id_competence"));
            if (!devoirByCompetences.containsKey(idComp)) {
                devoirByCompetences.put(idComp, new ArrayList<String>());
            }
            devoirByCompetences.get(idComp).add(idDevoir);
            if (byEnseignement) {
                if (null != competence.getLong("id_enseignement")
                        && null != competencesByDomainOrEnsei.get(competence.getLong("id_enseignement").toString())) {
                    competencesByDomainOrEnsei.get(competence.getLong("id_enseignement").toString()).add(idComp);
                }
            } else {
                if (null != competence.getString("ids_domaine")) {
                    String[] idsDomain = competence.getString("ids_domaine").split(",");
                    for (String idDomain : idsDomain) {
                        if (null != competencesByDomainOrEnsei.get(idDomain)) {
                            competencesByDomainOrEnsei.get(idDomain).add(idComp);
                        }

                    }
                }
            }
        }

        JsonObject bodyHeader = new JsonObject();
        if (byEnseignement) {
            bodyHeader.put("left", "Enseignements / items");
        } else {
            bodyHeader.put("left", "Domaines / items");
        }
        String right = getLibelle("evaluations.competence.level.and.number");
        bodyHeader.put("right", right);
        body.put("header", bodyHeader);

        JsonArray bodyBody = new fr.wseduc.webutils.collections.JsonArray();
        for (Map.Entry<String, Set<String>> competencesInDomain : competencesByDomainOrEnsei.entrySet()) {
            JsonObject domainObj = new JsonObject();
            if (byEnseignement) {
                if (enseignements.get(competencesInDomain.getKey()) != null) {
                    domainObj.put("domainHeader", enseignements.get(competencesInDomain.getKey())
                            .getString("nom"));
                }
            } else {
                domainObj.put("domainHeader", domaines.get(competencesInDomain.getKey())
                        .getString("codification") + " " + domaines.get(competencesInDomain.getKey())
                        .getString("libelle"));
            }
            JsonArray competencesInDomainArray = new fr.wseduc.webutils.collections.JsonArray();
            for (String competence : competencesInDomain.getValue()) {
                List<Long> valuesByComp = new ArrayList<>();
                for (String devoir : devoirByCompetences.get(competence)) {
                    if (competenceNotesByDevoir.containsKey(devoir) && competenceNotesByDevoir.get(devoir)
                            .containsKey(competence)) {
                        valuesByComp.add(competenceNotesByDevoir.get(devoir).get(competence) + 1);
                    } else {
                        valuesByComp.add(0L);
                    }
                }
                JsonObject competenceNote = new JsonObject();
                competenceNote.put("header", competencesObjByIdComp.get(competence).getString("nom"));
                competenceNote.put("competenceNotes", calcWidthNote(text, usePerso, maitrises, valuesByComp, devoirs.size()));
                competencesInDomainArray.add(competenceNote);
            }
            domainObj.put("domainBody", competencesInDomainArray);
            bodyBody.add(domainObj);
        }

        body.put("body", bodyBody);

        result.put("body", body);
        return result;
    }

    private void getDevoirInfos(JsonObject devoir, final HttpServerRequest request,
                                final Handler<Either<String, JsonObject>> handler) {

        final AtomicBoolean handled = new AtomicBoolean();
        final Map<String, Object> devoirMap = new HashMap<>();

        final Handler<Either<String, Map<String, Object>>> finalHandler = stringMapEither -> {
            if (!handled.get()) {
                if (stringMapEither.isRight()) {
                    Map<String, Object> devoirObj = stringMapEither.right().getValue();
                    int checkDevoirInfos = checkDevoirInfos(devoirObj);
                    if (checkDevoirInfos == 0) {
                        handled.set(true);
                        handler.handle(new Either.Right<>(new JsonObject(devoirObj)));
                    } else if (checkDevoirInfos == 1) {
                        handled.set(true);
                        handler.handle(new Either.Left<>("getDevoirsInfos : Devoir doesn't respect format."));
                    }
                } else {
                    handled.set(true);
                    handler.handle(new Either.Left<>("getDevoirsInfos : Error handled"));
                }
            }
        };
        String[] date = devoir.getString("date")
                .substring(0, devoir.getString("date").indexOf(" ")).split("-");
        devoirMap.put("date", date[2] + '/' + date[1] + '/' + date[0]);

        devoirMap.put(ID_KEY, devoir.getLong(ID_KEY));
        devoirMap.put("nom", devoir.getString("name"));
        devoirMap.put("coeff", devoir.getString("coefficient"));
        devoirMap.put("sur", devoir.getLong("diviseur"));
        devoirMap.put("periode", I18n.getInstance().translate(
                "viescolaire.periode." + String.valueOf(devoir.getLong("periodetype")),
                getHost(request),
                I18n.acceptLanguage(request)) + " " + String.valueOf(devoir.getLong("periodeordre")));
        devoirMap.put("sousMatiere", devoir.getString("libelle", ""));

        JsonObject action = new JsonObject()
                .put("action", "classe.getClasseInfo")
                .put("idClasse", devoir.getString("id_groupe"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if ("ok".equals(body.getString("status"))) {
                devoirMap.put("classe", body.getJsonObject("result").getJsonObject("c").getJsonObject("data")
                        .getString("name"));
                finalHandler.handle(new Right<>(devoirMap));

            } else {
                String error = "getDevoirsInfos : devoir '" + devoirMap.get(ID_KEY) + "), couldn't get class name.";
                log.error(error);
                finalHandler.handle(new Left<>(error));
            }
        }));


        JsonObject matiereAction = new JsonObject()
                .put("action", "matiere.getMatiere")
                .put("idMatiere", devoir.getString("id_matiere"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, matiereAction, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if ("ok".equals(body.getString("status"))) {
                devoirMap.put("matiere", body.getJsonObject("result").getJsonObject("n").getJsonObject("data")
                        .getString("label"));
                finalHandler.handle(new Right<>(devoirMap));
            } else {
                String error = "getDevoirsInfos : devoir '" +devoirMap.get(ID_KEY)+ "), couldn't get matiere name.";
                log.error(error);
                finalHandler.handle(new Left<>(error));
            }
        }));
    }

    private int checkDevoirInfos(Map<String, Object> devoir) {
        List<String> params = new ArrayList<>(Arrays.asList("classe", "nom", "matiere", "periode", "date", "coeff", "sur"));

        if (devoir.containsValue(null)) {
            return 1;
        } else {
            for (String param : params) {
                if (!devoir.keySet().contains(param)) {
                    return 2;
                }
            }
        }
        return 0;
    }

    private JsonArray calcWidthNote(Boolean text, Boolean usePerso, Map<String, JsonObject> maitrises,
                                    List<Long> competenceNotes, Integer nbDevoir) {
        Map<Long, Integer> occNote = new HashMap<>();
        for (Long competenceNote : competenceNotes) {
            if (!occNote.containsKey(competenceNote)) {
                occNote.put(competenceNote, 0);
            }
            occNote.put(competenceNote, occNote.get(competenceNote) + 1);
        }

        JsonArray resultList = new fr.wseduc.webutils.collections.JsonArray();
        for (Map.Entry<Long, Integer> notesMaitrises : occNote.entrySet()) {
            JsonObject competenceNotesObj = new JsonObject();
            String number = (text ? getMaitrise(maitrises.get(String.valueOf(notesMaitrises.getKey())).getString("lettre"), String.valueOf(notesMaitrises.getKey())) + " - " : "") + String.valueOf(notesMaitrises.getValue());
            competenceNotesObj.put("number", number);
            String color = text ? "white" : maitrises.get(String.valueOf(notesMaitrises.getKey())).getString("default");
            competenceNotesObj.put("color", color);

            if(usePerso && !text)
                competenceNotesObj.put("persoColor", maitrises.get(String.valueOf(notesMaitrises.getKey())).getString("couleur"));


            String width = "100"; // gestion cas 0 devoir
            if (nbDevoir > 0) {
                double tempWidth = notesMaitrises.getValue() / (double) nbDevoir * 100D;
                if (tempWidth < 1) {
                    tempWidth = 1;
                }
                width = String.valueOf(tempWidth);
            }
            competenceNotesObj.put("width", width);

            resultList.add(competenceNotesObj);
        }
        return resultList;
    }


    /**
     * Generation d'un PDF à partir d'un template xhtml
     *
     * @param request
     * @param templateProps objet JSON contenant l'ensemble des valeurs à remplir dans le template
     * @param templateName  nom du template
     * @param prefixPdfName prefixe du nom du pdf (qui sera complété de la date de génération)
     */
    @Override
    public void genererPdf(final HttpServerRequest request, final JsonObject templateProps, final String templateName,
                           final String prefixPdfName, Vertx vertx, JsonObject config) {
        final String dateDebut = new SimpleDateFormat("dd.MM.yyyy").format(new Date().getTime());
        if (templateProps.containsKey("image") && templateProps.getBoolean("image")) {
            log.info(new SimpleDateFormat("HH:mm:ss:S").format(new Date().getTime())
                    + " -> Debut Generation Image du template " + templateName);
        } else {
            log.info(new SimpleDateFormat("HH:mm:ss:S").format(new Date().getTime())
                    + " -> Debut Generation PDF du template " + templateName);
        }
        if (null != templateProps) {
            log.debug(new SimpleDateFormat("HH:mm:ss:S").format(new Date().getTime())
                    + " -> Detail Generation du template templateProps : " + templateProps.toString());
        } else {
            log.error(new SimpleDateFormat("HH:mm:ss:S").format(new Date().getTime())
                    + " -> Detail Generation du template templateProps : vide");
        }

        final String templatePath = FileResolver.absolutePath(config.getJsonObject("exports")
                .getString("template-path")).toString();
        final String baseUrl = getScheme(request) + "://"
                + Renders.getHost(request) + config.getString("app-address") + "/public/";

        String node = (String) vertx.sharedData().getLocalMap("server").get("node");
        if (node == null) {
            node = "";
        }
        final String _node = node;
        vertx.fileSystem().readFile(templatePath + templateName, new Handler<AsyncResult<Buffer>>() {
            @Override
            public void handle(AsyncResult<Buffer> result) {
                if (!result.succeeded()) {
                    badRequest(request, "Error while reading template : " + templatePath
                            + templateName);
                    log.error("Error while reading template : " + templatePath + templateName);
                    return;
                }
                StringReader reader = new StringReader(result.result().toString("UTF-8"));
                Renders render = new Renders(vertx, config);
                render.processTemplate(request, templateProps, templateName, reader, new Handler<Writer>() {
                    @Override
                    public void handle(Writer writer) {
                        String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                        if (processedTemplate == null) {
                            badRequest(request, "Error while processing.");
                            if (templateProps != null) {
                                log.error("processing error : \ntemplateProps : " + templateProps.toString()
                                        + "\ntemplateName : " + templateName);
                            }
                            return;
                        }
                        JsonObject actionObject = new JsonObject();
                        byte[] bytes;
                        try {
                            bytes = processedTemplate.getBytes("UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            bytes = processedTemplate.getBytes();
                            log.error(e.getMessage(), e);
                        }

                        actionObject.put("content", bytes)
                                .put("baseUrl", baseUrl);
                        eb.send(_node + "entcore.pdf.generator", actionObject,
                                new DeliveryOptions().setSendTimeout(
                                        TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L),
                                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                    @Override
                                    public void handle(Message<JsonObject> reply) {
                                        JsonObject pdfResponse = reply.body();
                                        if (!"ok".equals(pdfResponse.getString("status"))) {
                                            badRequest(request, pdfResponse.getString("message"));
                                            return;
                                        }
                                        byte[] pdf = pdfResponse.getBinary("content");

                                        if (templateProps.containsKey("image") && templateProps
                                                .getBoolean("image")) {
                                            File pdfFile = new File(prefixPdfName + "_"
                                                    + dateDebut + ".pdf");
                                            OutputStream outStream = null;
                                            try {
                                                outStream = new FileOutputStream(pdfFile);
                                            } catch (FileNotFoundException e) {
                                                log.error(e.getMessage());
                                                e.printStackTrace();
                                            }
                                            try {
                                                outStream.write(pdf);
                                            } catch (IOException e) {
                                                log.error(e.getMessage());
                                                e.printStackTrace();
                                            }

                                            try {
                                                String sourceDir = pdfFile.getAbsolutePath();
                                                File sourceFile = new File(sourceDir);
                                                while (!sourceFile.exists()) {
                                                    System.err.println(sourceFile.getName()
                                                            + " File does not exist");
                                                }
                                                if (sourceFile.exists()) {
                                                    PDDocument document = PDDocument.load(sourceDir);
                                                    @SuppressWarnings("unchecked")
                                                    List<PDPage> list = document.getDocumentCatalog()
                                                            .getAllPages();
                                                    File imageFile = null;
                                                    for (PDPage page : list) {
                                                        BufferedImage image = page.convertToImage();
                                                        int height = 150
                                                                + Integer.parseInt(templateProps
                                                                .getString("nbrCompetences")) * 50;
                                                        BufferedImage SubImage =
                                                                image.getSubimage(0, 0, 1684, height);
                                                        imageFile = new File(prefixPdfName
                                                                + "_" + dateDebut + ".jpg");
                                                        ImageIO.write(SubImage, "jpg", imageFile);
                                                    }
                                                    document.close();
                                                    FileInputStream fis = new FileInputStream(imageFile);
                                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                                    byte[] buf = new byte[(int) imageFile.length()];
                                                    for (int readNum; (readNum = fis.read(buf)) != -1; ) {
                                                        bos.write(buf, 0, readNum);
                                                    }
                                                    byte[] bytes = bos.toByteArray();

                                                    request.response()
                                                            .putHeader("Content-Type", "image/jpg");
                                                    request.response()
                                                            .putHeader("Content-Disposition",
                                                                    "attachment; filename="
                                                                            + prefixPdfName + "_"
                                                                            + dateDebut + ".jpg");
                                                    request.response().end(Buffer.buffer(bytes));
                                                    outStream.close();
                                                    bos.close();
                                                    fis.close();

                                                    Files.deleteIfExists(Paths.get(pdfFile.getAbsolutePath()));
                                                    Files.deleteIfExists(
                                                            Paths.get(imageFile.getAbsolutePath()));
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            log.info(new SimpleDateFormat("HH:mm:ss:S")
                                                    .format(new Date().getTime())
                                                    + " -> Fin Generation Image du template " + templateName);
                                        } else {
                                            request.response()
                                                    .putHeader("Content-Type", "application/pdf");
                                            request.response().putHeader("Content-Disposition",
                                                    "attachment; filename=" + prefixPdfName + "_"
                                                            + dateDebut + ".pdf");
                                            request.response().end(Buffer.buffer(pdf));
                                            JsonArray removesFiles = templateProps.getJsonArray(
                                                    "idImagesFiles");
                                            if (removesFiles != null) {
                                                storage.removeFiles(removesFiles, event -> {
                                                    log.info(" [Remove graph Images] " + event.encode());
                                                });
                                            }
                                            log.info(new SimpleDateFormat("HH:mm:ss:S")
                                                    .format(new Date().getTime())
                                                    + " -> Fin Generation PDF du template " + templateName);
                                        }
                                    }
                                }));
                    }
                });

            }
        });

    }

    public void getMatiereExportReleveComp(final JsonArray idMatieres, Handler<Either<String, String>> handler) {
        JsonObject action = new JsonObject().put(ACTION, "matiere.getMatieres").put("idMatieres", idMatieres);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();

                    if (OK.equals(body.getString(STATUS))) {
                        final JsonArray results = body.getJsonArray(RESULTS);
                        String mat = "";
                        if (isNotNull(results) && !results.isEmpty()) {
                            mat = results.getJsonObject(0).getString(NAME);
                            for (int i = 1; i < results.size(); i++) {
                                mat = mat + ", " + results.getJsonObject(i).getString(NAME);
                            }
                        }
                        handler.handle(new Either.Right<>(mat));
                    } else {
                        handler.handle(new Either.Left<>(body.getString(MESSAGE)));
                    }
                }));
    }

    public void getLibellePeriodeExportReleveComp(final HttpServerRequest request, final Long finalIdPeriode,
                                                  Boolean isCycle, Handler<Either<String, String>> handler) {

        if (isCycle) {
            handler.handle(new Either.Right(getLibelle("viescolaire.utils.cycle")));
            return;
        }

        JsonObject jsonRequest = new JsonObject()
                .put("headers", new JsonObject().put("Accept-Language",
                        request.headers().get("Accept-Language")))
                .put("Host", getHost(request));
        JsonObject action = new JsonObject()
                .put(ACTION, "periode.getLibellePeriode")
                .put("request", jsonRequest);
        if (!"undefined".equals(finalIdPeriode)) {
            action.put("idType", finalIdPeriode);
        }
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            final JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                String libellePeriode = body.getString(RESULT)
                        .replace("é", "e")
                        .replace("è", "e");
                handler.handle(new Either.Right<>(libellePeriode));
            } else {
                handler.handle(new Either.Left<>(body.getString(MESSAGE)));
            }
        }));
    }

    public void getElevesExportReleveComp(final String finalIdClasse, String idStructure, String finalIdEleve,
                                          final Long finalIdPeriode, final Map<String, String> elevesMap,
                                          Handler<Either<String, Object>> handler) {
        if (finalIdClasse == null) {
            JsonObject action = new JsonObject()
                    .put("action", "eleve.getInfoEleve")
                    .put(Competences.ID_ETABLISSEMENT_KEY, idStructure)
                    .put("idEleves", new fr.wseduc.webutils.collections.JsonArray(
                            Arrays.asList(new String[]{finalIdEleve})));

            eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                    handlerToAsyncHandler(message -> {
                        JsonObject body = message.body();

                        if (OK.equals(body.getString(STATUS))) {
                            JsonArray results = body.getJsonArray(RESULTS);
                            if (isNull(results) || results.isEmpty()) {
                                handler.handle(new Either.Left<>(" No student found "));
                                return;
                            }
                            JsonObject eleve = body.getJsonArray(RESULTS).getJsonObject(0);
                            handler.handle(new Either.Right<>(eleve));
                        } else {
                            handler.handle(new Either.Left<>(body.getString(MESSAGE)));
                        }
                    }));
        } else {
            final JsonObject action = new JsonObject()
                    .put(ACTION, "classe.getEleveClasse")
                    .put(ID_CLASSE_KEY, finalIdClasse)
                    .put(ID_PERIODE_KEY, finalIdPeriode);
            eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS, handlerToAsyncHandler(
                    message -> {
                        if (OK.equals(message.body().getString(STATUS))) {
                            JsonArray eleves = message.body().getJsonArray(RESULTS);
                            eleves = Utils.sortElevesByDisplayName(eleves);
                            final String[] idEleves = new String[eleves.size()];

                            for (int i = 0; i < eleves.size(); i++) {
                                elevesMap.put(eleves.getJsonObject(i).getString(ID_KEY),
                                        eleves.getJsonObject(i).getString("lastName")
                                                + " " + eleves.getJsonObject(i).getString("firstName"));
                                idEleves[i] = eleves.getJsonObject(i).getString(ID_KEY);
                            }

                            JsonObject infosAction = new JsonObject()
                                    .put("action", "eleve.getInfoEleve")
                                    .put(Competences.ID_ETABLISSEMENT_KEY, idStructure)
                                    .put("idEleves", new JsonArray(Arrays.asList(idEleves)));
                            eb.send(Competences.VIESCO_BUS_ADDRESS, infosAction, Competences.DELIVERY_OPTIONS,
                                    handlerToAsyncHandler(event -> {
                                        JsonObject body = event.body();

                                        if (OK.equals(body.getString(STATUS))) {
                                            JsonArray results = body.getJsonArray(RESULTS);
                                            if (isNull(results) || results.isEmpty()) {
                                                handler.handle(new Either.Left<>(" No student's info found"));
                                                return;
                                            }
                                            handler.handle(new Either.Right<>(results));
                                        } else {
                                            handler.handle(new Either.Left<>(body.getString(MESSAGE)));
                                        }
                                    }));
                        }
                    }));
        }
    }

    public void getDataForExportReleveEleve(String idUser, String idEtablissement, Long idPeriode,
                                            final MultiMap params, Handler<Either<String, JsonObject>> handler) {
        // TODO verifier que l'utilisateur connecte est bien l'eleve dont essaie d'acceder au releve ou que
        // le parent connecte essaie bien d'acceder au releve d'un de ses enfants
        List<Future> futures = new ArrayList<>();

        // récupération de l'élève
        Future<JsonObject> infoEleve = Future.future();
        utilsService.getInfoEleve(idUser, event -> formate(infoEleve, event));
        futures.add(infoEleve);

        // récupération du Backup de l'élève s'il est supprimé
        Future<JsonObject> infoEleveBackup = Future.future();
        utilsService.getInfoEleveBackup(idUser, event -> formate(infoEleveBackup, event));
        futures.add(infoEleveBackup);

        // Récupération de la liste des devoirs de la personne avec ses notes associées
        Future<JsonArray> devoirsFuture = Future.future();
        devoirService.listDevoirs(idUser, idEtablissement, null, null, idPeriode,
                false, event -> formate(devoirsFuture, event));
        futures.add(devoirsFuture);

        // Récupération de la liste des devoirs de la personne avec ses notes associées
        Future<JsonArray> annotationsFuture = Future.future();
        devoirService.listDevoirsWithAnnotations(idUser, idPeriode, event -> formate(annotationsFuture, event));
        futures.add(annotationsFuture);

        // Récupération des moyennes finales
        Future<JsonArray> moyenneFinaleFuture = Future.future();
        noteService.getColonneReleve(new JsonArray().add(idUser), idPeriode, null, null, "moyenne",
                moyenneFinaleEvent -> formate(moyenneFinaleFuture, moyenneFinaleEvent));
        futures.add(moyenneFinaleFuture);

        //Récupération de la structure
        Future<JsonObject> structureFuture = Future.future();
        utilsService.getStructure(idEtablissement, event -> formate(structureFuture, event));
        futures.add(structureFuture);

        // Récupération des matières de l'établissement
        Future<JsonArray> subjectF = Future.future();
        new DefaultMatiereService(eb).getMatieresEtab(idEtablissement, event -> formate(subjectF, event));
        futures.add(subjectF);

        CompositeFuture.all(futures).setHandler(event -> {
            if (event.failed()) {
                Utils.returnFailure("getExportReleveEleve : event failed", event, handler);
                return;
            }
            if(infoEleve.result().isEmpty() && infoEleveBackup.result().isEmpty()){
                Utils.returnFailure("getExportReleveEleve : No informations about the student", event, handler);
                return;
            }

            final JsonObject userJSON;
            boolean isBackup = false;

            if(!infoEleve.result().isEmpty()){
                userJSON = infoEleve.result();
            } else {
                userJSON = infoEleveBackup.result();
                isBackup = true;
            }

            Future<JsonArray> multiTeachersFuture = Future.future();
            utilsService.getMultiTeachersByClass(idEtablissement,
                    userJSON.getJsonObject("c").getJsonObject("data").getString("id"), idPeriode != null ? idPeriode.intValue() : null,
                    multiTeacherEvent -> {
                        formate(multiTeachersFuture, multiTeacherEvent);
                    });

            Future<JsonArray> servicesFuture = Future.future();
            utilsService.getServices(idEtablissement,
                    new JsonArray().add(userJSON.getJsonObject("c").getJsonObject("data").getString("id")),
                    servicesEvent -> {
                        formate(servicesFuture, servicesEvent);
                    });

            // devoirs de l'eleve (avec ses notes) sous forme d'objet JSON
            final JsonArray devoirsJSON = devoirsFuture.result();
            final JsonArray annotationsJSON = annotationsFuture.result();
            final JsonArray idMatieres = new fr.wseduc.webutils.collections.JsonArray();
            final JsonArray idEnseignants = new fr.wseduc.webutils.collections.JsonArray();

            final JsonArray moyennesFinales = moyenneFinaleFuture.result();


            annotationsJSON.stream().forEach(annotation -> {
                JsonObject annotationJson = (JsonObject) annotation;
                annotationJson.put("is_annotation", true);
                annotationJson.put("id", annotationJson.getInteger("id_devoir"));
                annotationJson.put("note", annotationJson.getString("libelle_court"));
                annotationJson.put("hasDiviseur", false);
                devoirsJSON.add(annotationJson);
            });

            for (int i = 0; i < devoirsJSON.size(); i++) {
                JsonObject devoir = devoirsJSON.getJsonObject(i);
                String idMatiere = devoir.getString(ID_MATIERE);
                idMatieres.add(idMatiere);
                idEnseignants.add(devoir.getValue("owner"));
            }
            // récupération de l'ensemble des matières de l'élève

            JsonArray matieres = new JsonArray();

            for (int i = 0; i < subjectF.result().size(); i++) {
                JsonObject o = subjectF.result().getJsonObject(i);
                String idMatiere = o.getString(ID_KEY);
                if (idMatieres.contains(idMatiere)) {
                    matieres.add(o);
                }
            }

            final JsonObject etabJSON = structureFuture.result().getJsonObject("s")
                    .getJsonObject("data");
            final JsonObject periodeJSON = new JsonObject();

            if (null != params.get("idTypePeriode") && null != params.get("ordrePeriode")) {
                final long idTypePeriode = Long.parseLong(params.get("idTypePeriode"));
                final long ordrePeriode = Long.parseLong(params.get("ordrePeriode"));
                String libellePeriode = getLibelle("viescolaire.periode." + idTypePeriode);
                libellePeriode += (" " + ordrePeriode);
                periodeJSON.put("libelle", libellePeriode);
            } else {
                // Construction de la période année
                periodeJSON.put("libelle", "Ann\u00E9e");
            }
            final Boolean finalBackUp = isBackup;
            CompositeFuture.all(multiTeachersFuture, servicesFuture).setHandler(futuresEvent -> {
                final JsonArray multiTeachers = multiTeachersFuture.result();
                final JsonArray services = servicesFuture.result();

                for (int i = 0; i < multiTeachers.size(); i++) {
                    JsonObject multiTeacher = multiTeachers.getJsonObject(i);
                    idEnseignants.add(multiTeacher.getString("second_teacher_id"));
                }

                getEnseignantsMatieres(matieres, idEnseignants, devoirsJSON, periodeJSON, userJSON, etabJSON,
                        finalBackUp, moyennesFinales, multiTeachers, services, handler);
            });
        });
    }

    /**
     * Récupère le nom des enseignants de chacune des matières puis positionne
     * les devoirs de l'élève sur les bonnes matières et enfin génère le PDF associé
     * formant le relevé de notes de l'élève.
     *
     * @param matieres    la liste des matières de l'élève.
     * @param idUsers
     * @param devoirsJson la liste des devoirs et notes de l'élève.
     * @param periodeJson la periode
     * @param userJson    l'élève
     * @param etabJson    l'établissement
     * @param handler
     */
    public void getEnseignantsMatieres(final JsonArray matieres, JsonArray idUsers, final JsonArray devoirsJson,
                                       final JsonObject periodeJson, final JsonObject userJson,
                                       final JsonObject etabJson, final boolean isBackup, final JsonArray moyennesFinales,
                                       final JsonArray multiTeachers, final JsonArray services,
                                       Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getUsers")
                .put("idUsers", idUsers);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                JsonArray users = body.getJsonArray(RESULTS);
                boolean printSousMatiere = false;

                for (int i = 0; i < devoirsJson.size(); i++) {
                    JsonObject devoir = devoirsJson.getJsonObject(i);
                    // Récupération de l'enseignant du devoir
                    JsonObject enseignantDevoir = null;
                    for (int j = 0; j < users.size(); j++) {
                        enseignantDevoir = users.getJsonObject(j);
                        if (enseignantDevoir.getString("id").equals(devoir.getString("owner"))) {
                            break;
                        }
                    }
                    if (enseignantDevoir != null) {
                        // Récupération de la matière
                        for (int k = 0; k < matieres.size(); k++) {
                            JsonObject matiereDevoir = matieres.getJsonObject(k);
                            int rowspan = matiereDevoir.getJsonArray("sous_matieres").size();
                            matiereDevoir.put("rowspan", rowspan);
                            if(rowspan > 0 && !printSousMatiere){
                                printSousMatiere = true;
                            }
                            JsonObject moyenneFinale = new JsonObject();
                            for(int n = 0; n < moyennesFinales.size(); n++) {
                                if (moyennesFinales.getJsonObject(n).getString("id_matiere").equals(matiereDevoir.getString("id"))) {
                                    moyenneFinale = moyennesFinales.getJsonObject(n);
                                    break;
                                }
                            }
                            getDevoirsByMatiere(devoirsJson, matiereDevoir, moyenneFinale);

                            if (matiereDevoir.getString("id").equals(devoir.getString("id_matiere"))) {
                                String firstNameEnsiegnant = enseignantDevoir.getString("firstName");
                                String displayName = firstNameEnsiegnant.substring(0, 1) + ".";
                                displayName = displayName + enseignantDevoir.getString("name");

                                String idMatiere = matiereDevoir.getString("id");
                                String idOwner = enseignantDevoir.getString("id");

                                Boolean isVisible = true;
                                for(int j = 0; j < services.size(); j++){
                                    JsonObject service = (JsonObject) services.getJsonObject(j);

                                    String serviceIdMatiere = service.getString("id_matiere");
                                    if(serviceIdMatiere.equals(idMatiere)) {
                                        isVisible = service.getBoolean("is_visible");
                                        break;
                                    }
                                }

                                JsonArray _enseignantMatiere = matiereDevoir.getJsonArray("displayNameEnseignant");
                                if (_enseignantMatiere == null) {
                                    _enseignantMatiere = new JsonArray();
                                    if(isVisible){
                                        _enseignantMatiere.add(displayName);
                                        matiereDevoir.put("displayNameEnseignant", _enseignantMatiere);
                                    }
                                } else {
                                    if (!_enseignantMatiere.contains(displayName)) {
                                        if(isVisible){
                                            _enseignantMatiere.add(displayName);
                                            matiereDevoir.put("displayNameEnseignant", _enseignantMatiere);
                                        }
                                    }
                                }

                                for(int j = 0; j < multiTeachers.size(); j ++){
                                    JsonObject multiTeacher = multiTeachers.getJsonObject(j);

                                    String subjectId = multiTeacher.getString("subject_id");
                                    String mainTeacherId = multiTeacher.getString("main_teacher_id");
                                    String coTeacherId = multiTeacher.getString("second_teacher_id");

                                    if (subjectId.equals(idMatiere) && mainTeacherId.equals(idOwner)){
                                        JsonObject coTeacher = null;
                                        for(int l = 0; l < users.size(); l++){
                                            JsonObject user = users.getJsonObject(l);
                                            if(user.getString("id").equals(coTeacherId))
                                                coTeacher = user;
                                        }
                                        if(coTeacher != null){
                                            String multiTeacherName = coTeacher.getString("firstName").substring(0, 1) + "." + coTeacher.getString("name");
                                            if(!_enseignantMatiere.contains(multiTeacherName)) {
                                                _enseignantMatiere.add(multiTeacherName);
                                            }
                                        }

                                    }
                                }
                                matiereDevoir.put("displayNameEnseignant", _enseignantMatiere);
                            }
                        }
                    }
                }
                final JsonObject templateProps = new JsonObject();

                templateProps.put("matieres", matieres);
                templateProps.put("periode", periodeJson);
                String prefixPdfName = "releve-eleve";
                if(isBackup) {
                    templateProps.put("user", userJson);
                    prefixPdfName += "-" + userJson.getString("displayName");
                } else {
                    templateProps.put("user", userJson.getJsonObject("u").getJsonObject("data"));
                    templateProps.put("classe", userJson.getJsonObject("c").getJsonObject("data"));
                    prefixPdfName += "-" + userJson.getJsonObject("u").getJsonObject("data").getString("displayName");
                    prefixPdfName += "-" + userJson.getJsonObject("c").getJsonObject("data").getString("name");
                }
                templateProps.put("etablissement", etabJson);
                templateProps.put(PRINT_SOUS_MATIERE, printSousMatiere);

                String etablissementName = etabJson.getString("name");
                etablissementName = etablissementName.trim().replaceAll(" ", "-");
                prefixPdfName += "-" + etablissementName;
                templateProps.put("prefixPdfName", prefixPdfName);

                handler.handle(new Either.Right<>(templateProps));
            }
            else {
                String error = "getUsers : event bus get Users failed ";
                log.error(error + "\n" + body.encode() + "\n");
                handler.handle(new Either.Left<>(body.getString("message")));
            }
        }));
    }

    /**
     * Récupère les devoirs de la matière et les positionnent sur celle ci.
     *
     * @param devoirsJson  la liste de tous les devoirs de l'élève.
     * @param matiereInter la matière dont on cherche les devoirs.
     */
    private void getDevoirsByMatiere(JsonArray devoirsJson, JsonObject matiereInter, JsonObject moyenneFinale) {
        JsonArray devoirsMatiereJson = new fr.wseduc.webutils.collections.JsonArray();

        List<NoteDevoir> listeNoteDevoirs = new ArrayList<NoteDevoir>();

        Map<Long, List<NoteDevoir>> listNotesSousMatiere = new HashMap<>();
        Map<String, Map<Long, JsonArray>> devoirsSousMat = new HashMap<>();
        final String idMatiere = matiereInter.getString("id");

        // parcours des devoirs
        for (int i = 0; i < devoirsJson.size(); i++) {
            JsonObject devoirJson = devoirsJson.getJsonObject(i);
            double sumNotes = 0.0;
            if(!devoirJson.containsKey("is_annotation")) {
                boolean hasCoeff = devoirJson.getString(COEFFICIENT) != null;
                Double coefficient = null;
                if (hasCoeff) {
                    hasCoeff = !Double.valueOf(devoirJson.getString(COEFFICIENT)).equals(1d);
                    coefficient = Double.valueOf(devoirJson.getString(COEFFICIENT));
                }
                // boolean permettant de savoir s'il y a un coefficient différent de 1 sur la note
                devoirJson.put("hasCoeff", hasCoeff);

                // ajout du devoir sur la matiere, si son identifiant de matière correspond bien
                if (isNotNull(coefficient) && idMatiere.equals(devoirJson.getString("id_matiere"))) {
                    devoirsMatiereJson.add(devoirJson);
                    Double note = Double.valueOf(devoirJson.getString("note"));
                    Double diviseur = Double.valueOf(devoirJson.getInteger("diviseur"));
                    Boolean ramenerSur = devoirJson.getBoolean("ramener_sur");
                    NoteDevoir noteDevoir = new NoteDevoir(note, diviseur, ramenerSur, coefficient);
                    Long idSousMatiere = devoirJson.getLong("id_sousmatiere");
                    Long nbrEleves = devoirJson.getLong("nbr_eleves");

                    try {
                        sumNotes = Double.parseDouble(devoirJson.getString("sum_notes"));
                    } catch (ClassCastException exc) {
                        log.error("[ getDevoirsByMatiere ] : sum_notes of devoirJson cannot be transform to double");
                    }

                    if (isNotNull(nbrEleves) && isNotNull(sumNotes)) {
                        DecimalFormat df = new DecimalFormat("0.##");
                        df.setRoundingMode(RoundingMode.HALF_UP);//with this mode 2.125 -> 2.13 without 2.125 -> 2.12
                        Double moyenneClasse = sumNotes / nbrEleves;
                        devoirJson.put("moyenneClasse", df.format(moyenneClasse));
                        devoirJson.put("hasMoyenneClasse", true);
                        devoirJson.put("hasDiviseurClasse", true);
                    }

                    if (isNotNull(idSousMatiere)) {
                        if (!listNotesSousMatiere.containsKey(idSousMatiere)) {
                            listNotesSousMatiere.put(idSousMatiere, new ArrayList<>());
                        }
                        listNotesSousMatiere.get(idSousMatiere).add(noteDevoir);
                        if (!devoirsSousMat.containsKey(idMatiere)) {
                            devoirsSousMat.put(idMatiere, new HashMap<>());
                        }
                        if (!devoirsSousMat.get(idMatiere).containsKey(idSousMatiere)) {
                            devoirsSousMat.get(idMatiere).put(idSousMatiere, new JsonArray());
                        }
                        devoirsSousMat.get(idMatiere).get(idSousMatiere).add(devoirJson);
                    }
                    listeNoteDevoirs.add(noteDevoir);
                }
                devoirJson.put("hasDiviseur", true);
            }
            else {
                if(idMatiere.equals(devoirJson.getString("id_matiere"))){
                    Long nbrEleves = devoirJson.getLong("nbr_eleves");

                    try {
                        if(devoirJson.getString("sum_notes") != null){
                            sumNotes = Double.parseDouble(devoirJson.getString("sum_notes"));
                        }
                    } catch (ClassCastException exc) {
                        log.error("[ getDevoirsByMatiere ] : sum_notes of devoirJson cannot be transform to double");
                    }

                    if (isNotNull(nbrEleves) && isNotNull(sumNotes)) {
                        DecimalFormat df = new DecimalFormat("0.##");
                        df.setRoundingMode(RoundingMode.HALF_UP);//with this mode 2.125 -> 2.13 without 2.125 -> 2.12
                        Double moyenneClasse = sumNotes / nbrEleves;
                        devoirJson.put("moyenneClasse", df.format(moyenneClasse));
                        devoirJson.put("hasMoyenneClasse", true);
                        devoirJson.put("hasDiviseurClasse", true);
                    }
                    else {
                        devoirJson.put("moyenneClasse", "NN");
                        devoirJson.put("hasMoyenneClasse", true);
                        devoirJson.put("hasDiviseurClasse", false);
                    }
                    devoirsMatiereJson.add(devoirJson);
                }
            }
        }
        matiereInter.put(DEVOIRS, devoirsMatiereJson);

        boolean hasDevoirs = !listeNoteDevoirs.isEmpty();
        matiereInter.put("hasDevoirs", hasDevoirs);

        if (hasDevoirs) {
            // param du calcul des moyennes
            final Boolean withStat = false;
            final int diviseur = 20;
            final Boolean annual = false;
            // calcul de la moyenne de l'eleve pour la matiere
            JsonObject moyenneMatiere = utilsService.calculMoyenne(listeNoteDevoirs, withStat, diviseur, annual);// TODO recuper le diviseur de la matiere
            // ajout sur l'objet json
            if(!moyenneFinale.isEmpty()){
                if(isNotNull(moyenneFinale.getValue("moyenne")))
                    matiereInter.put(MOYENNE, moyenneFinale.getValue("moyenne").toString());
                else
                    matiereInter.put(MOYENNE, "NN");
            } else if(moyenneMatiere.getValue(MOYENNE) != null){
                matiereInter.put(MOYENNE, moyenneMatiere.getValue(MOYENNE).toString());
            }
            matiereInter.put("hasDiviseurMatiere", true);
            matiereInter.put(MOYENNE_NON_NOTE, matiereInter.getValue(MOYENNE).equals("NN"));
            String keySousMatiere = "sous_matieres";
            JsonArray sousMatieres = matiereInter.getJsonArray(keySousMatiere);
            matiereInter.put("hasSousMatiere", sousMatieres.size()>0);
            JsonArray sousMatieresWithoutFirst = new JsonArray();

            for(int i = 0; i < sousMatieres.size(); i++) {
                JsonObject sousMatiere = sousMatieres.getJsonObject(i);
                Long idSousMatiere = sousMatiere.getLong("id_type_sousmatiere");
                List<NoteDevoir> notesSousMat = listNotesSousMatiere.get(idSousMatiere);
                String moy =  "NN";
                if(isNotNull(notesSousMat)) {
                    JsonObject moySousMatiere = utilsService.calculMoyenne(notesSousMat, withStat, diviseur, annual);
                    moy = moySousMatiere.getValue(MOYENNE).toString();
                    if(!moy.equals("NN"))
                        moy += "/20";
                }
                sousMatiere.put(MOYENNE, moy).put("isLast", i == sousMatieres.size() - 1);
                sousMatiere.put(MOYENNE_NON_NOTE, sousMatiere.getValue(MOYENNE).equals("NN"));
                JsonArray devoirsSousMatieres = new JsonArray();
                if(isNotNull(devoirsSousMat.get(idMatiere))){
                    if(isNotNull(devoirsSousMat.get(idMatiere).get(idSousMatiere))){
                        devoirsSousMatieres = devoirsSousMat.get(idMatiere).get(idSousMatiere);
                    }
                }
                sousMatiere.put(DEVOIRS, devoirsSousMatieres);
                if(i == 0){
                    matiereInter.put("first_sous_matieres", sousMatiere);
                }
                else {
                    sousMatieresWithoutFirst.add(sousMatiere);
                }
            }
            matiereInter.put(keySousMatiere + "_tail", sousMatieresWithoutFirst);
        }
        else {
            matiereInter.put(MOYENNE, "NN");
            matiereInter.put("hasDiviseurMatiere", false);
        }
    }

    public void getDataForExportReleveClasse(String idClasse, String idEtablissement, Long idPeriode,
                                             Long idTypePeriode, final Long ordre,
                                             Handler<Either<String, JsonObject>> handler){
        Utils.getElevesClasse(eb, idClasse, idPeriode, elevesEvent -> {
            if (elevesEvent.isLeft()) {
                String error = elevesEvent.left().getValue();
                log.error("[ getDataForExportReleveClasse ]" + error);
                handler.handle(new Either.Left<>(getLibelle("evaluations.get.students.classe.error")));
                return;
            }
            JsonArray elevesClasse = elevesEvent.right().getValue();
            if(isNull(elevesClasse)){
                log.error("[ getDataForExportReleveClasse ] : NO student in classe");
                handler.handle(new Either.Left<>(getLibelle("evaluations.export.releve.no.student")));
                return;
            }

            JsonArray exportResultClasse = new JsonArray();
            List<Future> classeFuture = new ArrayList<>();
            MultiMap params = MultiMap.caseInsensitiveMultiMap();
            params.add("idTypePeriode", isNotNull(idTypePeriode)? idTypePeriode.toString() : null)
                    .add("ordrePeriode", isNotNull(ordre)? ordre.toString() : null);
            getDataForClasse(elevesClasse, idEtablissement, idPeriode, params, exportResultClasse, classeFuture);

            CompositeFuture.all(classeFuture).setHandler(event -> {
                if(event.failed()){
                    returnFailure("getDataForExportReleveClasse", event, handler);
                    return;
                }
                handler.handle(new Either.Right<>(new JsonObject().put(ELEVES,
                        Utils.sortElevesByDisplayName(exportResultClasse))));
            });
        });
    }

    private void getDataForClasse(JsonArray elevesClasse, String idEtablissement, Long idPeriode, MultiMap params,
                                  JsonArray exportResultClasse, List<Future> classeFuture) {
        for(int i=0; i < elevesClasse.size(); i++){
            JsonObject eleve = elevesClasse.getJsonObject(i);
            String idEleve = eleve.getString(ID_KEY);
            if(isNull(idEleve)){
                idEleve = eleve.getString(ID_ELEVE_KEY);
            }
            Future eleveFuture = Future.future();

            classeFuture.add(eleveFuture);
            getDataForEleve(elevesClasse, idEleve, idEtablissement, idPeriode, params, eleveFuture, exportResultClasse);
        }
    }

    private void getDataForEleve(JsonArray elevesClasse, String idEleve, String idEtablissement, Long idPeriode, MultiMap params,
                                 Future eleveFuture, JsonArray exportResultClasse) {
        getDataForExportReleveEleve(idEleve, idEtablissement, idPeriode, params, event -> {
            if(event.isLeft()){
                String error = event.left().getValue();
                log.error("[getDataForEleve] " + error);
                eleveFuture.fail(getLibelle("evaluations.get.data.student.classe.error"));
                return;
            }
            exportResultClasse.add(event.right().getValue());
            eleveFuture.complete();
        });

    }
}