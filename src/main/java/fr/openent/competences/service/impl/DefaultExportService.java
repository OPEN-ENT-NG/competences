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
import fr.openent.competences.constants.Field;
import fr.openent.competences.helper.DateHelper;
import fr.openent.competences.helper.NumberHelper;
import fr.openent.competences.helpers.ExportEvaluationHelper;
import fr.openent.competences.helpers.FormateFutureEvent;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.model.*;
import fr.openent.competences.service.*;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.template.TemplateProcessor;
import fr.wseduc.webutils.template.lambdas.I18nLambda;
import fr.wseduc.webutils.template.lambdas.LocaleDateLambda;
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
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.entcore.common.storage.Storage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.*;
import static fr.openent.competences.helpers.ExportEvaluationHelper.*;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.openent.competences.service.impl.DefaultUtilsService.setServices;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.Renders.*;

public class DefaultExportService implements ExportService {

    public final static String COEFFICIENT = "coefficient";
    protected static final Logger log = LoggerFactory.getLogger(DefaultExportService.class);
    private final static String DEVOIRS = "devoirs";
    private final static String PRINT_SOUS_MATIERE = "printSousMatiere";
    private final static int DEBUT_CYCLE_3 = 6;
    private final static int FIN_CYCLE_3 = 6;
    private final static int DEBUT_CYCLE_4 = 5;
    private final static int FIN_CYCLE_4 = 3;
    private final DefaultMatiereService defaultMatiereService;
    private final Storage storage;
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
    private StructureOptionsService structureOptionsService;
    private EventBus eb;

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
        defaultMatiereService = new DefaultMatiereService(eb);
        structureOptionsService = new DefaultStructureOptions();
        this.storage = storage;
    }

    @Override
    public void getExportCartouche(final MultiMap params, Handler<Either<String, JsonObject>> handler) {
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

        final Long idDevoir;
        if (params.get("idDevoir") == null) {
            log.error("Error : idDevoir must be a long object");
            handler.handle(new Either.Left<>("Error : idDevoir must be a long object"));
        } else {
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
                    log.error("error to get devoir id: " + idDevoir + " Message : " + devoirInfo.left().getValue());
                    return;
                }
                final JsonObject devoir = (JsonObject) ((Either.Right) devoirInfo).getValue();
                String idStructure = devoir.getString("id_etablissement");
                final String idClass = devoir.getString("id_groupe");

                Future<JsonObject> classInfoFuture = Future.future();
                utilsService.getClassInfo(idClass, event ->
                        FormateFutureEvent.formate(classInfoFuture, event));

                Future<JsonArray> competencesFuture = Future.future();
                if (devoir.getInteger("nbrcompetence") > 0) {
                    competencesService.getDevoirCompetences(idDevoir, null, event ->
                            FormateFutureEvent.formate(competencesFuture, event));
                } else {
                    competencesFuture.complete(new JsonArray());
                }

                CompositeFuture.all(classInfoFuture, competencesFuture).setHandler(event -> {
                    if (event.failed()) {
                        handler.handle(new Either.Left<>(event.cause().getMessage()));
                        log.error("error to get niveau de maitrise, classInfo and competence : "
                                + event.cause().getMessage());
                    } else {

                        JsonObject classInfo = classInfoFuture.result();
                        JsonArray competences = competencesFuture.result();
                        Map<String, JsonArray> mapResult = new HashMap<>();
                        if (classInfo.isEmpty() || competences.isEmpty()) {
                            handler.handle(new Either.Left<>("no classInfo or no competences"));
                            log.error("error : no classInfo or no competences");
                        } else {
                            mapResult.put("competences", competences);
                            Long idCycle = competences.getJsonObject(0).getLong("id_cycle");

                            niveauDeMaitriseService.getNiveauDeMaitrise(idStructure, idCycle,
                                    eventNivMaitrise -> {

                                        if (eventNivMaitrise.isLeft() || eventNivMaitrise.right().getValue().isEmpty()) {
                                            handler.handle(new Either.Left<>("export : no level."));
                                            log.error("error : no level " + eventNivMaitrise.left().getValue());
                                        } else {
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
                                                        handlerToAsyncHandler(message -> {

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
                                                                                                        result, byEleves,
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
                                                                                    log.error("error to get competencesNotes" +
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

    private void getResultsEleves(Long idDevoir, String idStructure, Map mapResult, Handler<Either<String, Boolean>> handler) {

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
                    public void handle(AsyncResult<CompositeFuture> event) {
                        if (event.succeeded()) {
                            //notes = note, annotations et appreciation
                            JsonArray notes = notesFuture.result();
                            JsonArray competencesNotes = competencesNotesFuture.result();
                            JsonArray annotations = annotationsFuture.result();

                            if (competencesNotes.isEmpty() && notes.isEmpty() || annotations.isEmpty() && !notes.isEmpty()) {
                                handler.handle(new Either.Right<>(false));
                            } else {

                                mapResult.put("notes", notes);
                                mapResult.put("competencesNotes", competencesNotes);
                                mapResult.put("annotations", annotations);
                                handler.handle(new Either.Right<>(true));
                            }

                        } else {
                            handler.handle(new Either.Left<>("Error : to get notes , annotations and competencesNotes : " +
                                    event.cause().getMessage()));
                            log.error("Error : to get notes , annotations and competencesNotes : " +
                                    event.cause().getMessage());
                        }
                    }
                });

    }

    ;

    private JsonObject buildExportCartouche(JsonObject devoir, MultiMap params,
                                            JsonObject result, String byEleves,
                                            Boolean withResult, JsonObject classInfo,
                                            Map<String, JsonArray> mapResult) {

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


        if (devoir.getInteger("nbrcompetence") > 0 && !competences.isEmpty()) {
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

        if (result.getBoolean("byEleves")) {
            JsonArray eleves = mapResult.get("eleves");
            eleves = Utils.sortElevesByDisplayName(eleves);
            if (devoir.getInteger("nbrcompetence") > 0 && !competences.isEmpty() && withResult) {
                JsonArray evaluatedCompetences = result.getJsonArray("competences");
                result.remove("competences");
                Map<String, JsonObject> mapAnnotations = extractData(mapResult.get("annotations"), ID_KEY);
                Map<String, JsonObject> mapNotesEleves = extractData(mapResult.get("notes"), "id_eleve");
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
                    JsonObject eleve_jo = (JsonObject) eleve;
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
                                    result.getJsonArray("niveaux").size() + 2);
                            hasAppreciation = true;

                            eleve_jo.put("appreciation", noteEleve.getString("appreciation"));
                        }
                    }
                    eleve_jo.put("hasAnnotation", hasAnnotation);
                    eleve_jo.put("showAppreciation", hasAppreciation);

                    JsonArray competencesNotesElevesArray = new JsonArray();
                    evaluatedCompetences.stream().forEach(evaluatedCompetence -> {
                        String id_competenceEvaluated = String.
                                valueOf(((JsonObject) evaluatedCompetence).getLong("id_competence"));
                        JsonObject competenceNoteEleveResult = new JsonObject();
                        competenceNoteEleveResult.put("code_domaine",
                                ((JsonObject) evaluatedCompetence).getString("code_domaine"));
                        competenceNoteEleveResult.put("nom", ((JsonObject) evaluatedCompetence).getString("nom"));
                        competenceNoteEleveResult.put("first", ((JsonObject) evaluatedCompetence).getBoolean("first"));
                        JsonArray niveauxEleve = new fr.wseduc.webutils.collections.JsonArray();
                        String idEleve = eleve_jo.getString(ID_KEY);
                        if (!competencesNotesElevesMap.containsKey(idEleve)
                                || competencesNotesElevesMap.containsKey(idEleve)
                                && !competencesNotesElevesMap.get(idEleve).containsKey(id_competenceEvaluated)
                                || competencesNotesElevesMap.containsKey(idEleve)
                                && competencesNotesElevesMap.get(idEleve).containsKey(id_competenceEvaluated)
                                && competencesNotesElevesMap.get(idEleve).get(id_competenceEvaluated).getInteger("evaluation") == -1) {
                            niveauxEleve.add(true);
                            result.getJsonArray("niveaux").stream().forEach(niveau -> {
                                niveauxEleve.add(false);
                            });
                            competenceNoteEleveResult.put("niveauxEleve", niveauxEleve);

                        } else {
                            Map<String, JsonObject> mapCompetencesNotesEleve = competencesNotesElevesMap.get(idEleve);

                            if (mapCompetencesNotesEleve.containsKey(id_competenceEvaluated)) {

                                JsonObject competenceNoteEleveMap =
                                        mapCompetencesNotesEleve.get(String.
                                                valueOf(((JsonObject) evaluatedCompetence).getLong("id_competence")));
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
            } else {
                result.put("eleves", eleves);
            }
        } else {
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
        ExportEvaluationHelper.formatDevoir(devoir, getHost(request), I18n.acceptLanguage(request), devoirMap);
        exportDatas.add(getClasseDevoir(devoir, devoirMap, eb));
        exportDatas.add(getMatiereDevoir(devoir, devoirMap, eb));

        JsonArray maitrises = new JsonArray();
        Future maitriseFuture = getNiveauDeMaitriseDevoir(devoir, onlyEvaluation, maitrises);
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
            if (event.failed()) {
                returnFailure("getExportEval", event, handler);
            } else {
                final Map<String, Map<String, JsonObject>> compNoteElevesMap = new HashMap<>();
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

    private void buildDevoirExport(final Boolean pByEnseignement, final String idEleve, final String[] idGroupes,
                                   String[] idFunctionalGroupes, final String idEtablissement,
                                   final List<String> idMatieres, Long idPeriodeType, Boolean isCycle, final Long idCycle,
                                   final JsonArray enseignementArray, final JsonArray devoirsArray,
                                   final JsonArray competencesArray, final JsonArray domainesArray,
                                   final JsonArray competencesNotesArray, String[] idMatieresTab,
                                   final Handler<Either<String, JsonArray>> finalHandler) {


        //on recupere la liste des devoirs des classes mais aussi des groupes de l'eleve
        final List<String> idClasseAndFunctionnalGroups = new ArrayList<>();
        Collections.addAll(idClasseAndFunctionnalGroups, idGroupes);
        Collections.addAll(idClasseAndFunctionnalGroups, idFunctionalGroupes);

        devoirService.listDevoirs(idEleve, idClasseAndFunctionnalGroups.toArray(new String[0]), null,
                idPeriodeType != null ? new Long[]{idPeriodeType} : null,
                idEtablissement != null ? new String[]{idEtablissement} : null,
                idMatieres != null ? idMatieresTab : null, null, isCycle,
                getIntermediateHandler(devoirsArray, stringJsonArrayEither -> {
                    if (stringJsonArrayEither.isRight() && isNotNull(stringJsonArrayEither.right().getValue()) &&
                            !stringJsonArrayEither.right().getValue().isEmpty() &&
                            !(stringJsonArrayEither.right().getValue().getValue(0) instanceof String)) {
                        JsonArray idDevoirs = new JsonArray();
                        for (int i = 0; i < stringJsonArrayEither.right().getValue().size(); i++) {
                            Long idDevoir = stringJsonArrayEither.right().getValue().getJsonObject(i).getLong(ID_KEY);
                            idDevoirs.add(idDevoir);
                        }

                        if (pByEnseignement) {
                            competencesService.getDevoirCompetencesByEnseignement(idDevoirs, idCycle, idEtablissement,
                                    getIntermediateHandler(idDevoirs, competencesArray, finalHandler));
                        } else {
                            competencesService.getDevoirCompetences(idDevoirs, idEtablissement, idCycle,
                                    getIntermediateHandler(idDevoirs, competencesArray, finalHandler));
                        }
                        competenceNoteService.getCompetencesNotes(idDevoirs, idEleve,
                                true, idCycle,
                                getIntermediateHandler(idDevoirs, competencesNotesArray, finalHandler));
                        domaineService.getDomainesRacines(idGroupes[0], idCycle,
                                getIntermediateHandler(domainesArray, finalHandler));
                        enseignementService.getEnseignementsOrdered(
                                getIntermediateHandler(enseignementArray, finalHandler));
                    } else if (isNotNull(stringJsonArrayEither.right()) &&
                            isNotNull(stringJsonArrayEither.right().getValue()) &&
                            !stringJsonArrayEither.right().getValue().isEmpty() &&
                            stringJsonArrayEither.right().getValue().getValue(0) instanceof String) {
                        if (pByEnseignement) {
                            competencesService.getDevoirCompetencesByEnseignement((Long) null, idCycle,
                                    getIntermediateHandler((Long) null, competencesArray, finalHandler));
                        } else {
                            competencesService.getDevoirCompetences(null, idCycle,
                                    getIntermediateHandler((Long) null, competencesArray, finalHandler));
                        }
                        competenceNoteService.getCompetencesNotes((Long) null, idEleve, true, idCycle,
                                getIntermediateHandler((Long) null, competencesNotesArray, finalHandler));

                        domaineService.getDomainesRacines(idGroupes[0], idCycle,
                                getIntermediateHandler(domainesArray, finalHandler));
                        enseignementService.getEnseignementsOrdered(
                                getIntermediateHandler(enseignementArray, finalHandler));
                    } else {
                        String error = stringJsonArrayEither.left().getValue();
                        log.error("getExportReleveComp | buildDevoirExport " + error);
                        if (error.contains("Timeout") || error.contains("Timed out")) {
                            log.info(" reset buildDevoirExport");
                            buildDevoirExport(pByEnseignement, idEleve, idGroupes, idFunctionalGroupes, idEtablissement,
                                    idMatieres, idPeriodeType, isCycle, idCycle, enseignementArray, devoirsArray,
                                    competencesArray, domainesArray, competencesNotesArray, idMatieresTab, finalHandler);
                        } else {
                            finalHandler.handle(stringJsonArrayEither.left());
                        }
                    }
                }));
    }

    private void buildNiveauReleveComp(final String[] idGroupes, final String idEtablissement, JsonArray maitriseArray,
                                       final Long idCycle, final Handler<Either<String, JsonArray>> finalHandler) {
        if (idCycle == null) {
            utilsService.getCycle(Arrays.asList(idGroupes), stringJsonArrayEither -> {
                if (stringJsonArrayEither.isRight() && isNotNull(stringJsonArrayEither.right().getValue()) &&
                        !stringJsonArrayEither.right().getValue().isEmpty()) {
                    Long idCycleResult = new Long(stringJsonArrayEither.right().getValue().getJsonObject(0)
                            .getLong("id_cycle"));

                    for (int i = 0; i < stringJsonArrayEither.right().getValue().size(); i++) {
                        JsonObject cycleObj = stringJsonArrayEither.right().getValue().getJsonObject(i);
                        if (!idCycleResult.equals(cycleObj.getLong("id_cycle"))) {
                            finalHandler.handle(new Either.Left<String, JsonArray>(
                                    "getExportReleveComp : Given groups belong to different cycle."));
                        }
                    }
                    niveauDeMaitriseService.getNiveauDeMaitrise(idEtablissement, idCycleResult,
                            getIntermediateHandler(maitriseArray, finalHandler));
                } else {
                    String error = stringJsonArrayEither.left().getValue();
                    log.error("getExportReleveComp | getCycle " + error);
                    if (error.contains("Timeout") || error.contains("Timed out")) {
                        log.info(" reset getCycle");
                        buildNiveauReleveComp(idGroupes, idEtablissement, maitriseArray, idCycle, finalHandler);
                    } else {
                        finalHandler.handle(new Either.Left<>(error));
                    }
                }
            });
        } else {
            niveauDeMaitriseService.getNiveauDeMaitrise(idEtablissement, idCycle,
                    getIntermediateHandler(maitriseArray, finalHandler));
        }
    }

    @Override
    public void getExportReleveComp(final Boolean text, final Boolean usePerso, final Boolean pByEnseignement,
                                    final String idEleve, final int eleveLevel, final String[] idGroupes,
                                    String[] idFunctionalGroupes, final String idEtablissement, final List<String> idMatieres,
                                    Long idPeriodeType, Boolean isCycle, final Long idCycle, final Handler<Either<String, JsonObject>> handler) {
        final JsonArray maitriseArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray enseignementArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray devoirsArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray competencesArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray domainesArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray competencesNotesArray = new fr.wseduc.webutils.collections.JsonArray();
        String[] idMatieresTab = idMatieres.toArray(new String[0]);
        final AtomicBoolean answered = new AtomicBoolean();
        final AtomicBoolean byEnseignement = new AtomicBoolean(pByEnseignement);

        final Handler<Either<String, JsonArray>> finalHandler = getReleveCompFinalHandler(text, usePerso, idEleve, eleveLevel, devoirsArray,
                maitriseArray, competencesArray, domainesArray, competencesNotesArray, enseignementArray, answered,
                byEnseignement, idCycle, handler);

        buildDevoirExport(pByEnseignement, idEleve, idGroupes, idFunctionalGroupes, idEtablissement,
                idMatieres, idPeriodeType, isCycle, idCycle, enseignementArray, devoirsArray,
                competencesArray, domainesArray, competencesNotesArray, idMatieresTab, finalHandler);

        buildNiveauReleveComp(idGroupes, idEtablissement, maitriseArray, idCycle, finalHandler);

    }

    @Override
    public void getLegendeRecapEval(final Boolean text, final Boolean usePerso, final Long idCycle,
                                    final String idEtablissement, final Handler<Either<String, JsonArray>> handler) {
        niveauDeMaitriseService.getNiveauDeMaitrise(idEtablissement, idCycle, niveauEither -> {
            if (niveauEither.isRight()) {
                JsonArray legende = new fr.wseduc.webutils.collections.JsonArray();
                JsonArray result = niveauEither.right().getValue();
                for (int i = result.size() - 1; i >= 0; i--) {
                    JsonObject niveau = new JsonObject();
                    JsonObject o = result.getJsonObject(i);

                    niveau.put("libelle", o.getString("libelle") != null
                            ? o.getString("libelle") : o.getString("default_lib"));

                    niveau.put("visu", text ? getMaitrise(o.getString("lettre"),
                            o.getInteger(ORDRE).toString()) : o.getString("default"));
                    niveau.put(ORDRE, o.getInteger(ORDRE));

                    if (usePerso && !text)
                        niveau.put("persoColor", o.getString("couleur"));

                    legende.add(niveau);
                }
                handler.handle(new Either.Right<>(legende));
            } else {
                handler.handle(new Either.Left<>("exportRecapEval : empty result."));
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
                    if (isNull(result)) {
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
                    for (int i = 0; i < idDevoirs.size(); i++) {
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
    getReleveCompFinalHandler(final Boolean text, final Boolean usePerso, final String idEleve, final int eleveLevel,
                              final JsonArray devoirs, final JsonArray maitrises, final JsonArray competences,
                              final JsonArray domaines, final JsonArray competencesNotes, final JsonArray enseignements,
                              final AtomicBoolean answered, final AtomicBoolean byEnseignement, final Long idCycle,
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
                                    Map<String, JsonObject> devoirsMap = extractData(devoirs, ID_KEY);
                                    Map<String, JsonObject> maitrisesMap = extractData(
                                            orderBy(addMaitriseNE(maitrises), ORDRE, true), ORDRE);
                                    Map<String, JsonObject> competencesMap = extractData(competences, ID_KEY);
                                    Map<String, JsonObject> domainesMap = extractData(domaines, ID_KEY);
                                    Map<String, JsonObject> enseignementsMap = extractData(enseignements, ID_KEY);

                                    JsonObject resToAdd = formatJsonObjectExportReleveComp(
                                            text, usePerso, Boolean.valueOf(byEnseignement.get()), idCycle, idEleve, eleveLevel,
                                            devoirsMap, maitrisesMap, competencesMap, domainesMap,
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


    private TreeMap<String, HashMap<Date, Date>> calculPeriodesAnnees(int eleveLevel, Long idCycle) {
        if (eleveLevel != -1) {
            Calendar date = Calendar.getInstance();
            int actualMonth = date.get(Calendar.MONTH);
            int actualYear = date.get(Calendar.YEAR);
            Calendar periodeBeginning = Calendar.getInstance();
            Calendar periodeEnding = Calendar.getInstance();
            if (actualMonth < 9) {
                int pastYear = actualYear - 1;
                periodeBeginning.set(pastYear, Calendar.SEPTEMBER, 1);
                periodeEnding.set(actualYear, Calendar.AUGUST, 31);
            } else {
                int afterYear = actualYear + 1;
                periodeBeginning.set(actualYear, Calendar.SEPTEMBER, 1);
                periodeEnding.set(afterYear, Calendar.AUGUST, 31);
            }

            TreeMap<String, HashMap<Date, Date>> periodes = new TreeMap<>(Collections.reverseOrder());

            int debut = 0, fin = 0;
            if (idCycle == 1) {
                debut = DEBUT_CYCLE_4;
                fin = FIN_CYCLE_4;
            } else if (idCycle == 2) {
                debut = fin = DEBUT_CYCLE_3;
            }
            for (int i = eleveLevel; i <= DEBUT_CYCLE_3; i++) {
                if (i != eleveLevel) {
                    periodeBeginning.set(periodeBeginning.get(Calendar.YEAR) - 1,
                            periodeBeginning.get(Calendar.MONTH), periodeBeginning.get(Calendar.DATE));
                    periodeEnding.set(periodeEnding.get(Calendar.YEAR) - 1,
                            periodeEnding.get(Calendar.MONTH), periodeEnding.get(Calendar.DATE));
                }
                if (i <= debut && i >= fin) {
                    String label = i + "EME";
                    HashMap<Date, Date> periode = new HashMap<>();
                    periode.put(periodeBeginning.getTime(), periodeEnding.getTime());
                    periodes.put(label, periode);
                }

            }
            return periodes;
        }
        return null;
    }


    private boolean isInPeriode(String dateCompetence, Date beginningYear, Date endingYear) {
        String[] dateParts = dateCompetence.split(" ");
        Date date = null;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd").parse(dateParts[0]);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return (date.after(beginningYear) && date.before(endingYear));
    }


    private JsonObject buildPeriode(String classe, String width) {
        JsonObject periode = new JsonObject();
        periode.put("classe", classe);
        periode.put("width", width);
        return periode;
    }


    private JsonObject formatJsonObjectExportReleveComp(Boolean text, Boolean usePerso, Boolean byEnseignement, Long idCycle,
                                                        String idEleve, int eleveLevel, Map<String, JsonObject> devoirs,
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
            if (usePerso && !text)
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
        if (idCycle != null) {
            TreeMap<String, HashMap<Date, Date>> periodes = calculPeriodesAnnees(eleveLevel, idCycle);
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

                for (String competenceId : competencesInDomain.getValue()) {
                    JsonObject competenceNote = new JsonObject();
                    LinkedHashMap<String, Long> valuesByComp = new LinkedHashMap<>();
                    List<Long> valuesByCompActualClasse = new ArrayList<>();
                    assert periodes != null;
                    for (Map.Entry<String, HashMap<Date, Date>> entry : periodes.entrySet()) {
                        String classe = entry.getKey();
                        HashMap<Date, Date> periode = entry.getValue();
                        Date beginningYear = new Date();
                        Date endingYear = new Date();

                        for (Map.Entry<Date, Date> date : periode.entrySet()) {
                            beginningYear = date.getKey();
                            endingYear = date.getValue();
                        }

                        if (Objects.equals(Integer.parseInt(classe.substring(0, 1)), eleveLevel)) {
                            for (String devoir : devoirByCompetences.get(competenceId)) {
                                if (competenceNotesByDevoir.containsKey(devoir) && competenceNotesByDevoir.get(devoir)
                                        .containsKey(competenceId)
                                        && isInPeriode(devoirs.get(devoir).getString("date"), beginningYear, endingYear)) {
                                    valuesByCompActualClasse.add(competenceNotesByDevoir.get(devoir).get(competenceId) + 1);
                                }
                            }
                        } else {
                            for (String devoir : devoirByCompetences.get(competenceId)) {
                                if (competenceNotesByDevoir.containsKey(devoir) && competenceNotesByDevoir.get(devoir)
                                        .containsKey(competenceId) && devoirs.get(devoir).getBoolean("eval_lib_historise")
                                        && isInPeriode(devoirs.get(devoir).getString("date"), beginningYear, endingYear)) {
                                    valuesByComp.put(classe, competenceNotesByDevoir.get(devoir).get(competenceId) + 1);
                                }
                            }
                        }
                    }
                    if (!valuesByComp.isEmpty() || !valuesByCompActualClasse.isEmpty()) {
                        JsonArray competencesNotes = calcWidthNotePeriode(text, usePerso, maitrises, valuesByComp,
                                valuesByCompActualClasse, devoirByCompetences.get(competenceId).size());
                        competenceNote.put("header", competencesObjByIdComp.get(competenceId).getString("nom"));
                        competenceNote.put("competenceNotes", competencesNotes);
                        competencesInDomainArray.add(competenceNote);
                    }

                }
                domainObj.put("domainBody", competencesInDomainArray);
                bodyBody.add(domainObj);
            }
        } else {
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
                    competenceNote.put("competenceNotes", calcWidthNote(text, usePerso, maitrises, valuesByComp,
                            devoirByCompetences.get(competence).size()));
                    competencesInDomainArray.add(competenceNote);
                }
                domainObj.put("domainBody", competencesInDomainArray);
                bodyBody.add(domainObj);
            }
        }

        body.put("body", bodyBody);

        result.put("body", body);
        return result;
    }


    private JsonObject formatJsonObjectExportReleve(Boolean text, Boolean usePerso,
                                                    String idEleve, Map<String, JsonObject> devoirs,
                                                    Map<String, JsonObject> maitrises,
                                                    Map<String, JsonObject> competences,
                                                    Map<String, JsonObject> matieres,
                                                    Map<String, Map<String, Long>> competenceNotesByDevoir) {

        JsonObject result = new JsonObject();
        result.put(Field.TEXT, text);
        result.put(Field.IDELEVE, idEleve);

        JsonObject header = new JsonObject();
        JsonObject body = new JsonObject();
        JsonArray competencesArray = new JsonArray();

        //Maitrise
        JsonArray headerMiddle = new JsonArray(
                maitrises.values().stream().map(maitrise -> {
                    JsonObject _maitrise = new JsonObject()
                            .put(Field.LIBELLE, maitrise.getString(Field.LIBELLE) != null
                                    ? maitrise.getString(Field.LIBELLE) : maitrise.getString(Field.DEFAULT_LIB))
                            .put(Field.VISU, text ? getMaitrise(maitrise
                                    .getString(Field.LETTRE), String.valueOf(maitrise
                                    .getLong(ORDRE))) : maitrise.getString(Field.DEFAULT));
                    if (usePerso && !text)
                        _maitrise.put(Field.PERSOCOLOR, maitrise.getString(Field.COULEUR));

                    return _maitrise;
                }).collect(Collectors.toList()));

        header.put(Field.RIGHT, headerMiddle);
        result.put(Field.HEADER, header);

        final Map<String, JsonObject> competencesObjByIdComp = new HashMap<>();

        Map<String, Set<String>> competencesByMatiere = new LinkedHashMap<>();
        for (String idEntity : matieres.keySet()) {
            competencesByMatiere.put(idEntity, new TreeSet<String>(new Comparator<String>() {
                @Override
                public int compare(String competenceId1, String competenceId2) {
                    String s1 = competencesObjByIdComp.get(competenceId1).getString(Field.NOM);
                    String s2 = competencesObjByIdComp.get(competenceId2).getString(Field.NOM);
                    return s1.compareTo(s2);
                }
            }));
        }

        Map<String, List<String>> devoirByCompetences = new HashMap<>();
        setSkillsArrays(competences, competencesObjByIdComp, competencesByMatiere, devoirByCompetences);

        JsonObject bodyHeader = new JsonObject();
        bodyHeader.put(Field.LEFT, "Matieres / items");
        String right = getLibelle("evaluations.competence.level.and.number");
        bodyHeader.put(Field.RIGHT, right);
        body.put(Field.HEADER, bodyHeader);

        setCompetenceNoteItem(text, usePerso, maitrises, matieres, competenceNotesByDevoir, competencesArray, devoirs,
                competencesByMatiere, devoirByCompetences);

        body.put(Field.BODY, competencesArray);

        result.put(Field.BODY, body);
        return result;
    }

    private void setCompetenceNoteItem(Boolean text, Boolean usePerso, Map<String, JsonObject> maitrises,
                                       Map<String, JsonObject> matieres, Map<String, Map<String, Long>> competenceNotesByDevoir,
                                       JsonArray competencesArray, Map<String, JsonObject> devoirs,
                                       Map<String, Set<String>> competencesBySubject,
                                       Map<String, List<String>> devoirByCompetences) {
        for (Map.Entry<String, Set<String>> competencesInSubject : competencesBySubject.entrySet()) {
            JsonArray competencesInSubjectArray = new JsonArray();
            List<Long> valuesByComp = new ArrayList<>();
            List<Long> valuesByCompFormative = new ArrayList<>();
            for (String competenceId : competencesInSubject.getValue()) {
                for (String devoirId : devoirByCompetences.get(competenceId)) {
                    if (competenceNotesByDevoir.containsKey(devoirId) && competenceNotesByDevoir.get(devoirId)
                            .containsKey(competenceId)) {
                        if (devoirs.containsKey(devoirId) && devoirs.get(devoirId).getBoolean(Field.FORMATIVE)) {
                            valuesByCompFormative.add(competenceNotesByDevoir.get(devoirId).get(competenceId) + 1);
                        } else {
                            valuesByComp.add(competenceNotesByDevoir.get(devoirId).get(competenceId) + 1);
                        }
                    } else {
                        valuesByComp.add(0L);
                    }
                }
            }
            JsonObject competenceNote = new JsonObject();
            competenceNote.put(Field.HEADER, competencesInSubject.getKey());
            competenceNote.put(Field.COMPETENCENOTES, calcWidthNote(text, usePerso, maitrises, valuesByComp,
                    valuesByCompFormative, valuesByComp.size() + valuesByCompFormative.size()));
            competencesInSubjectArray.add(competenceNote);
            competencesArray.add(competenceNote);
        }
    }

    private void setSkillsArrays(Map<String, JsonObject> competences, Map<String, JsonObject> competencesObjByIdComp, Map<String, Set<String>> competencesByMatiere, Map<String, List<String>> devoirByCompetences) {
        for (JsonObject competence : competences.values()) {
            competencesObjByIdComp.put(String.valueOf(competence.getLong(Field.ID_COMPETENCE)), competence);

            if (competence.containsKey(Field.EMPTY)) {
                continue;
            }
            String idDevoir = String.valueOf(competence.getLong(Field.ID_DEVOIR));
            String idComp = String.valueOf(competence.getLong(Field.ID_COMPETENCE));
            if (!devoirByCompetences.containsKey(idComp)) {
                devoirByCompetences.put(idComp, new ArrayList<String>());
            }
            devoirByCompetences.get(idComp).add(idDevoir);
            if (null != competence.getString(Field.ID_MATIERE)
                    && null != competencesByMatiere.get(competence.getString(Field.ID_MATIERE).toString())) {
                competencesByMatiere.get(competence.getString(Field.ID_MATIERE).toString()).add(idComp);
            }
        }
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

    private JsonArray calcWidthNotePeriode(Boolean text, Boolean usePerso, Map<String, JsonObject> maitrises,
                                           Map<String, Long> competenceNotes, List<Long> competenceNotesActualClasse, Integer nbDevoir) {

        JsonArray resultList = new fr.wseduc.webutils.collections.JsonArray();
        for (Map.Entry<String, Long> notesMaitrises : competenceNotes.entrySet()) {
            JsonObject competenceNotesObj = new JsonObject();
            competenceNotesObj.put("number", notesMaitrises.getKey());
            String color = text ? "white" : maitrises.get(String.valueOf(notesMaitrises.getValue())).getString("default");
            competenceNotesObj.put("color", color);

            if (usePerso && !text)
                competenceNotesObj.put("persoColor", maitrises.get(String.valueOf(notesMaitrises.getValue())).getString("couleur"));

            competenceNotesObj.put("width", "1");

            resultList.add(competenceNotesObj);
        }

        JsonArray resultListActualClasse = calcWidthNote(text, usePerso, maitrises, competenceNotesActualClasse, nbDevoir);
        resultList.addAll(resultListActualClasse);
        return resultList;
    }

    private JsonArray calcWidthNote(Boolean text, Boolean usePerso, Map<String, JsonObject> maitrises,
                                    List<Long> competenceNotes, Integer nbDevoir) {
        return calcWidthNote(text, usePerso, maitrises, competenceNotes, new ArrayList<>(), nbDevoir);
    }

    private Map<String, Integer> setMapValueNumberCompetencesNotes(List<Long> listValueCompetenceNotes) {
        Map<String, Integer> occNote = listValueCompetenceNotes.stream()
                .collect(Collectors.groupingBy(Object::toString, Collectors.summingInt(e -> 1)));
        return occNote;
    }

    private JsonArray calcWidthNote(Boolean text, Boolean usePerso, Map<String, JsonObject> maitrises,
                                    List<Long> competenceNotes, List<Long> competenceNotesFormative, Integer nbDevoir) {

        Map<String, Integer> occNote = setMapValueNumberCompetencesNotes(competenceNotes);
        Map<String, Integer> occNoteFormative = setMapValueNumberCompetencesNotes(competenceNotesFormative);

        JsonArray resultList = new fr.wseduc.webutils.collections.JsonArray();
        for (Map.Entry<String, JsonObject> niveauMaitrise : maitrises.entrySet()) {

            if (occNote.containsKey(niveauMaitrise.getKey()) || occNoteFormative.containsKey(niveauMaitrise.getKey())) {
                JsonObject competenceNotesObj = new JsonObject();
                String number = new String();
                double notesMaitrisesValue = 0;
                if (occNoteFormative.containsKey(niveauMaitrise.getKey()) && occNoteFormative.get(niveauMaitrise.getKey()) != null) {
                    number += occNoteFormative.get(niveauMaitrise.getKey()) + "(F) ";
                    notesMaitrisesValue += occNoteFormative.get(niveauMaitrise.getKey());
                }
                if (occNote.containsKey(niveauMaitrise.getKey()) && occNote.get(niveauMaitrise.getKey()) != null) {
                    number += occNote.get(niveauMaitrise.getKey()) + " ";
                    notesMaitrisesValue += occNote.get(niveauMaitrise.getKey());
                }
                number += getMaitrise(niveauMaitrise.getValue().getString(Field.LETTRE), niveauMaitrise.getKey());
                competenceNotesObj.put(Field.NUMBER, number);
                competenceNotesObj.put(Field.WIDTH, Math.floor(notesMaitrisesValue * 100 / nbDevoir));
                String color = text ? Field.WHITE : niveauMaitrise.getValue().getString(Field.DEFAULT);
                competenceNotesObj.put(Field.COLOR, color);
                if (usePerso && !text)
                    competenceNotesObj.put(Field.PERSOCOLOR, niveauMaitrise.getValue().getString(Field.COULEUR));
                resultList.add(competenceNotesObj);
            }
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
                log.info(" result vertx.fileSystem");
                if (!result.succeeded()) {
                    log.info("Error while reading template : " + templatePath + templateName);
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
                        log.info("write render.processTemplate");
                        String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                        if (processedTemplate == null) {
                            log.info("processing error : \ntemplateProps : " + templateProps.toString()
                                    + "\ntemplateName : " + templateName);
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
                                        log.info("response entcore.pdf.generator");
                                        JsonObject pdfResponse = reply.body();
                                        if (!"ok".equals(pdfResponse.getString("status"))) {
                                            log.info(" response entcore.pdf.generator status ko : " + pdfResponse.getString("message"));
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
                                            log.info(" response final ");
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

    @Override
    public void generateSchoolReportPdf(HttpServerRequest request, JsonObject templateProps, String templateName,
                                        String prefixPdfName, Vertx vertx, JsonObject config) {
        final String templatePath = FileResolver.absolutePath(config.getJsonObject("exports").getString("template-path"));
        final String baseUrl = getScheme(request) + "://" + Renders.getHost(request) + config.getString("app-address") + "/public/";

        TemplateProcessor templateProcessor = new TemplateProcessor(vertx, templatePath).escapeHTML(true);
        templateProcessor.setLambda("i18n", new I18nLambda("fr"));
        templateProcessor.setLambda("datetime", new LocaleDateLambda("fr"));
        JsonObject localTemplateProps = templateProps.copy();
        JsonArray students = templateProps.getJsonArray("eleves");
        String startDate = new SimpleDateFormat("dd.MM.yyyy").format(new Date().getTime());
        String fileName = prefixPdfName + "_" + startDate + ".pdf";
        List<Future<byte[]>> studentsBufferFutures = new ArrayList<>();
        Promise<byte[]> init = Promise.promise();
        Future<byte[]> current = init.future();
        for (int i = 0; i < students.size(); i++) {
            int indice = i;
            current = current.compose(e -> {
                JsonObject student = students.getJsonObject(indice);
                localTemplateProps.remove("eleves");
                localTemplateProps.put("eleves", new JsonArray().add(student));
                Future<byte[]> next = renderTemplateAndGeneratePdf(templateProcessor, vertx, baseUrl, localTemplateProps,
                        templateName, student);
                studentsBufferFutures.add(next);
                return next;
            });
        }
        current
                .onSuccess(res -> {
                    try {
                        PDFMergerUtility mergedPdf = new PDFMergerUtility();
                        List<File> pdfFiles = new ArrayList<>();
                        String tmp = System.getProperty("java.io.tmpdir");
                        for (int i = 0; i < studentsBufferFutures.size(); i++) {
                            byte[] studentByte = studentsBufferFutures.get(i).result();
                            File outputFile = new File(tmp + "/" + i + fileName);
                            FileOutputStream outputStream = new FileOutputStream(outputFile);
                            outputStream.write(studentByte);
                            pdfFiles.add(outputFile);
                        }
                        pdfFiles.forEach(mergedPdf::addSource);
                        ByteArrayOutputStream pdfDocOutputstream = new ByteArrayOutputStream();
                        mergedPdf.setDestinationFileName(fileName);
                        mergedPdf.setDestinationStream(pdfDocOutputstream);
                        mergedPdf.mergeDocuments();
                        ByteArrayOutputStream bos = (ByteArrayOutputStream) mergedPdf.getDestinationStream();

                        request.response().putHeader("Content-Type", "application/pdf");
                        request.response().putHeader("Content-Disposition", "attachment; filename=" + fileName);
                        request.response().end(Buffer.buffer(bos.toByteArray()));
                        for (File pdfFile : pdfFiles) {
                            Files.deleteIfExists(Paths.get(pdfFile.getAbsolutePath()));
                        }
                        JsonArray removesFiles = templateProps.getJsonArray("idImagesFiles");
                        if (removesFiles != null) {
                            storage.removeFiles(removesFiles, event -> log.info(String.format("[Competences@%s::generateSchoolReportPdf] - " +
                                    "Remove graph Images: %s", this.getClass().getSimpleName(), event.encode())));
                        }
                    } catch (IOException | COSVisitorException e) {
                        String error = String.format("[Competences@%s::generateSchoolReportPdf] An exception has occured during process: %s",
                                this.getClass().getSimpleName(), e.getMessage());
                        log.error(error);
                        badRequest(request, error);
                    }
                })
                .onFailure(err -> {
                    String error = String.format("[Competences@%s::generateSchoolReportPdf] An exception has occured " +
                                    "during renderTemplateAndGeneratePdf: %s",
                            this.getClass().getSimpleName(), err.getMessage());
                    log.error(error);
                    badRequest(request, error);
                });
        init.complete();
    }

    /**
     * Method that will process template to fetch its buffer and generate PDF within
     *
     * @param templateProcessor Template configuration to process template {@link TemplateProcessor}
     * @param vertx             Vertx instance
     * @param baseUrl           BaseUrl
     * @param templateProps     Props object to send for proceeding template {@link JsonObject}
     * @param templateName      Name of the template used for proceeding template {@link String} (e.g bulletin.pdf.xhtml...)
     * @param student           student data info object
     * @return Future {@link Future of {byte[]} } containing student's file pdf in byte
     */
    private Future<byte[]> renderTemplateAndGeneratePdf(TemplateProcessor templateProcessor, Vertx vertx, String baseUrl,
                                                        JsonObject templateProps, String templateName, JsonObject student) {
        Promise<byte[]> promise = Promise.promise();
        templateProcessor.processTemplate(templateName, templateProps, writer -> {
            if (writer == null || writer.isEmpty()) {
                String error = String.format("[Competences@%s::renderTemplateAndGeneratePdf] Failed to process template for student : %s",
                        this.getClass().getSimpleName(), student.getString("idEleve"));
                log.error(error);
                promise.fail(error);
            } else {
                String node = (String) vertx.sharedData().getLocalMap("server").get("node");
                if (node == null) {
                    node = "";
                }
                JsonObject actionObject = new JsonObject()
                        .put("content", writer.getBytes(StandardCharsets.UTF_8))
                        .put("baseUrl", baseUrl);

                eb.request(node + "entcore.pdf.generator", actionObject, new DeliveryOptions()
                        .setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L), handlerToAsyncHandler(reply -> {
                    JsonObject pdfResponse = reply.body();
                    if (!"ok".equals(pdfResponse.getString("status"))) {
                        String error = String.format("[Competences@%s::renderTemplateAndGeneratePdf] Failed to generate PDF" +
                                        " after processTemplate for student : %s",
                                this.getClass().getSimpleName(), student.getString("idEleve"));
                        log.error(error);
                        promise.fail(error);
                    } else {
                        promise.complete(pdfResponse.getBinary("content"));
                    }
                }));
            }
        });
        return promise.future();
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
                                                  Boolean isCycle, Long idCycle, Handler<Either<String, String>> handler) {

        if (isCycle) {
            if (idCycle == 1)
                handler.handle(new Either.Right(getLibelle("viescolaire.utils.cycle4")));
            else if (idCycle == 2)
                handler.handle(new Either.Right(getLibelle("viescolaire.utils.cycle3")));
            else
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
                                JsonObject eleve = eleves.getJsonObject(i);
                                elevesMap.put(eleve.getString(ID_KEY),
                                        eleve.getString("lastName") + " " + eleve.getString("firstName"));
                                idEleves[i] = eleve.getString(ID_KEY);
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

    public void getDataForExportReleveEleve(String userId, String structureId, Long periodId,
                                            final MultiMap params, Handler<Either<String, JsonObject>> handler) {
        List<Future> futures = new ArrayList<>();
        final JsonArray compNotes = new JsonArray();
        HashMap<String, Boolean> isBackup = new HashMap<>();
        isBackup.put(Field.ISBACKUP, false);
        StringBuilder idClass = new StringBuilder();
        final JsonObject userJSON = new JsonObject();
        JsonArray idGroupClasse = new JsonArray();
        JsonArray subjects = new JsonArray();
        Map<String, JsonArray> competencesNotesBySubject = new HashMap<>();
        final JsonArray devoirsJSON = new JsonArray();
        final JsonObject periodeJSON = new JsonObject();
        final JsonArray finalAverages = new JsonArray();

        // récupération de l'élève
        Future<JsonObject> infoEleve = Future.future();
        utilsService.getInfoEleve(userId, event -> formate(infoEleve, event));
        futures.add(infoEleve);

        // récupération du Backup de l'élève s'il est supprimé
        Future<JsonObject> infoEleveBackup = Future.future();
        utilsService.getInfoEleveBackup(userId, event -> formate(infoEleveBackup, event));
        futures.add(infoEleveBackup);

        // Récupération de la liste des devoirs de la personne avec ses notes associées
        Future<JsonArray> devoirsFuture = Future.future();
        devoirService.listDevoirs(userId, structureId, null, null, periodId,
                false, event -> formate(devoirsFuture, event));
        futures.add(devoirsFuture);

        // Récupération de la liste des devoirs de la personne avec ses notes associées
        Future<JsonArray> annotationsFuture = Future.future();
        devoirService.listDevoirsWithAnnotations(userId, periodId, null, event -> formate(annotationsFuture, event));
        futures.add(annotationsFuture);

        // Récupération des moyennes finales
        Future<JsonArray> moyenneFinaleFuture = Future.future();
        noteService.getColonneReleve(new JsonArray().add(userId), periodId, null, null, "moyenne",
                Boolean.FALSE,
                moyenneFinaleEvent -> formate(moyenneFinaleFuture, moyenneFinaleEvent));
        futures.add(moyenneFinaleFuture);

        //Récupération de la structure
        Future<JsonObject> structureFuture = Future.future();
        utilsService.getStructure(structureId, event -> formate(structureFuture, event));
        futures.add(structureFuture);

        // Récupération des matières de l'établissement
        Future<JsonArray> subjectF = Future.future();
        defaultMatiereService.getMatieresEtab(structureId, event -> formate(subjectF, event));
        futures.add(subjectF);

        Future<JsonArray> compNotesFuture = Future.future();
        noteService.getCompetencesNotesReleveEleves(new JsonArray().add(userId), structureId, null, null, periodId,
                userId, true, false,
                compNotesEvent -> formate(compNotesFuture, compNotesEvent));
        futures.add(compNotesFuture);

        // promises for next iterations
        Promise<JsonArray> groupsClassPromise = Promise.promise();
        Promise<JsonArray> periodsPromise = Promise.promise();
        Promise<JsonArray> multiTeachingPromise = Promise.promise();
        Promise<JsonArray> servicesPromise = Promise.promise();
        Promise<List<SubTopic>> subTopicCoefPromise = Promise.promise();
        Promise<JsonArray> maitrisePromise = Promise.promise();
        Promise<JsonArray> competencesPromise = Promise.promise();
        Promise<JsonArray> tableauDeConversionPromise = Promise.promise();
        Promise<Boolean> isAvgSkillPromise = Promise.promise();


        CompositeFuture.all(futures)
                .compose(event -> {
                    if (infoEleve.result().isEmpty() && infoEleveBackup.result().isEmpty()) {
                        String errorMessage = String.format("[Competences@%s::getExportReleveEleve] No informations about the student",
                                this.getClass().getSimpleName());
                        log.error(String.format("[Competences@%s::getExportReleveEleve] No informations about the student",
                                this.getClass().getSimpleName()));
                        return Future.failedFuture(errorMessage);
                    }

                    compNotes.addAll(compNotesFuture.result());

                    if (!infoEleve.result().isEmpty()) {
                        userJSON.mergeIn(infoEleve.result());
                    } else {
                        userJSON.mergeIn(infoEleveBackup.result());
                        isBackup.put(Field.ISBACKUP, true);
                    }

                    idClass.append(
                            !userJSON.containsKey(Field.C) ? userJSON.getString(Field.IDCLASSE, "") :
                                    userJSON.getJsonObject(Field.C).getJsonObject(Field.DATA).getString(Field.ID)
                    );
                    String idClassString = idClass.toString();

                    if (!"".equals(idClassString))
                        utilsService.getPeriodes(Collections.singletonList(idClassString), structureId).onComplete(periodsPromise);
                    else periodsPromise.handle(Future.succeededFuture(new JsonArray()));

                    Utils.getGroupesClasse(eb, new JsonArray().add(idClassString), FutureHelper.handler(groupsClassPromise,
                            String.format("[Competences@%s::getMultiTeachers] DefaultExportService at getDataForExportReleveEleve.",
                                    this.getClass().getSimpleName())));
                    return CompositeFuture.all(groupsClassPromise.future(), periodsPromise.future());
                })
                .compose(results -> {
                    JsonArray groupsClassResult = groupsClassPromise.future().result();
                    idGroupClasse.add(idClass.toString());

                    if (groupsClassResult != null && !groupsClassResult.isEmpty()) {
                        idGroupClasse.addAll(groupsClassResult.getJsonObject(0).getJsonArray(Field.ID_GROUPES));
                    }
                    return utilsService.getCycle(idClass.toString());
                })
                .compose(cycle -> {
                    List<Future> futuresList = new ArrayList<>();

                    utilsService.getAllMultiTeachers(structureId,
                            idGroupClasse, FutureHelper.handlerJsonArray(multiTeachingPromise.future()));
                    futuresList.add(multiTeachingPromise.future());

                    utilsService.getSubTopicCoeff(structureId, idGroupClasse).onComplete(subTopicCoefPromise);
                    futuresList.add(subTopicCoefPromise.future());

                    utilsService.getServices(structureId, idGroupClasse, FutureHelper.handler(servicesPromise));
                    futuresList.add(servicesPromise.future());

                    // devoirs de l'eleve (avec ses notes) sous forme d'objet JSON
                    devoirsJSON.addAll(devoirsFuture.result());
                    final JsonArray annotationsJSON = annotationsFuture.result();
                    finalAverages.addAll(moyenneFinaleFuture.result());

                    annotationsJSON.stream().forEach(annotation -> {
                        JsonObject annotationJson = (JsonObject) annotation;
                        annotationJson.put(Field.IS_ANNOTATION, true);
                        annotationJson.put(Field.ID, annotationJson.getInteger(Field.ID_DEVOIR));
                        annotationJson.put(Field.NOTE, annotationJson.getString(Field.LIBELLE_COURT));
                        annotationJson.put(Field.HASDIVISEUR, false);
                        devoirsJSON.add(annotationJson);
                    });

                    final JsonArray subjectIds = new JsonArray();
                    final JsonArray idDevoirsCompetences = new JsonArray();

                    for (int i = 0; i < devoirsJSON.size(); i++) {
                        JsonObject devoir = devoirsJSON.getJsonObject(i);
                        String subjectId = devoir.getString(ID_MATIERE);
                        subjectIds.add(subjectId);
                    }

                    for (int i = 0; i < compNotes.size(); i++) {
                        JsonObject devoir = compNotes.getJsonObject(i);
                        String subjectId = devoir.getString(ID_MATIERE);
                        Long idDevoir = devoir.getLong(Field.ID_DEVOIR);
                        subjectIds.add(subjectId);
                        idDevoirsCompetences.add(idDevoir);
                        if (!competencesNotesBySubject.containsKey(subjectId)) {
                            competencesNotesBySubject.put(subjectId, new JsonArray());
                        }
                        competencesNotesBySubject.get(subjectId).add(devoir);
                    }

                    for (int i = 0; i < finalAverages.size(); i++) {
                        JsonObject finalAverage = finalAverages.getJsonObject(i);
                        String idSubject = finalAverage.getString(ID_MATIERE);
                        subjectIds.add(idSubject);
                    }

                    for (int i = 0; i < subjectF.result().size(); i++) {
                        JsonObject o = subjectF.result().getJsonObject(i);
                        String idSubject = o.getString(ID_KEY);
                        if (subjectIds.contains(idSubject)) {
                            subjects.add(o);
                        }
                    }

                    niveauDeMaitriseService.getNiveauDeMaitrise(structureId, cycle.getLong(Field.ID_CYCLE),
                            FutureHelper.handlerJsonArray(maitrisePromise.future()));
                    futuresList.add(maitrisePromise.future());

                    competencesService.getDevoirCompetences(idDevoirsCompetences, structureId, cycle.getLong(Field.ID_CYCLE),
                            FutureHelper.handlerJsonArray(competencesPromise.future()));
                    futuresList.add(competencesPromise.future());

                    competenceNoteService.getConversionNoteCompetence(structureId, idClass.toString(),
                            FutureHelper.handler(tableauDeConversionPromise));
                    futuresList.add(tableauDeConversionPromise.future());

                    structureOptionsService.isAverageSkills(structureId).onComplete(isAvgSkillPromise);
                    futuresList.add(isAvgSkillPromise.future());

                    if (null != params.get(Field.IDTYPEPERIODE) && null != params.get(Field.ORDREPERIODE)) {
                        final long idTypePeriode = Long.parseLong(params.get(Field.IDTYPEPERIODE));
                        final long ordrePeriode = Long.parseLong(params.get(Field.ORDREPERIODE));
                        String libellePeriode = getLibelle(VIESCO_BUS_ADDRESS + "." + Field.VIESCO_PERIODE_TABLE +
                                "." + idTypePeriode) + " " + ordrePeriode;
                        periodeJSON.put(Field.LIBELLE, libellePeriode);
                        addPeriodDates(periodeJSON, periodsPromise.future().result(), periodId.toString());
                    } else {
                        // Construction de la période année
                        periodeJSON.put(Field.LIBELLE, Field.ANNEE);
                    }

                    return CompositeFuture.all(futuresList);
                })
                .onFailure(err -> handler.handle(new Either.Left<>(err.getMessage())))
                .onSuccess(futuresEvent -> {
                    final JsonArray multiTeachers = multiTeachingPromise.future().result();
                    final JsonArray servicesJson = servicesPromise.future().result();
                    final List<SubTopic> subTopics = subTopicCoefPromise.future().result();
                    final JsonArray maitrisesJson = maitrisePromise.future().result();
                    final JsonArray competencesJson = competencesPromise.future().result();
                    final JsonArray idEnseignants = new JsonArray();
                    List<Service> services = new ArrayList<>();
                    Structure structure = new Structure();
                    structure.setId(structureId);

                    for (int i = 0; i < servicesJson.size(); i++) {
                        JsonObject service = servicesJson.getJsonObject(i);
                        idEnseignants.add(service.getString(Field.ID_ENSEIGNANT));
                    }
                    setServices(structure, servicesJson, services, subTopics);

                    for (int i = 0; i < multiTeachers.size(); i++) {
                        JsonObject multiTeacher = multiTeachers.getJsonObject(i);
                        idEnseignants.add(multiTeacher.getString(Field.SECOND_TEACHER_ID));
                    }

                    Map<String, Map<String, Long>> competenceNotesMap = new HashMap<>();
                    Map<String, JsonObject> devoirsMap = extractData(compNotes, Field.ID_DEVOIR);
                    Map<String, JsonObject> maitrisesMap = extractData(
                            orderBy(addMaitriseNE(maitrisesJson), ORDRE, true), ORDRE);
                    Map<String, JsonObject> competencesMap = extractData(competencesJson, ID_KEY);
                    Map<String, JsonObject> matieresMap = extractData(subjects, ID_KEY);

                    final JsonObject etabJSON = structureFuture.result().getJsonObject("s").getJsonObject(Field.DATA);
                    buildCompetenceNotesMap(compNotes, competenceNotesMap);
                    boolean showSkills = null != params.get(Field.SHOWSKILLS) && Boolean.parseBoolean(params.get(Field.SHOWSKILLS));
                    if (showSkills) {
                        JsonObject compNotesByMatiere = formatJsonObjectExportReleve(false, true, userId, devoirsMap,
                                maitrisesMap, competencesMap, matieresMap, competenceNotesMap);

                        setSkillsAttributesToSubjects(periodId, competencesNotesBySubject, subjects,
                                tableauDeConversionPromise.future(), isAvgSkillPromise.future(), compNotesByMatiere);
                    }
                    boolean showScores = null != params.get(Field.SHOWSCORES) && Boolean.parseBoolean(params.get(Field.SHOWSCORES));
                    getEnseignantsMatieres(subjects, idEnseignants, devoirsJSON, periodeJSON, userJSON, etabJSON,
                            isBackup.get(Field.ISBACKUP), finalAverages, multiTeachers, services, showScores, showSkills, handler);
                });
    }

    private void addPeriodDates(JsonObject period, JsonArray periodDates, String periodId) {
        periodDates.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .filter(periodDate -> periodId
                        .equals(periodDate.getInteger(Field.ID_TYPE).toString()))
                .findFirst()
                .ifPresent(periodDate -> {
                    String periodStart = periodDate.getString(Field.TIMESTAMP_DT);
                    String periodEnd = periodDate.getString(Field.TIMESTAMP_FN);
                    if (periodStart != null) period.put(Field.START_DATE, periodStart);
                    if (periodEnd != null) period.put(Field.END_DATE, periodEnd);
                });
    }

    private void setSkillsAttributesToSubjects(Long periodId, Map<String, JsonArray> competencesNotesBySubject, JsonArray subjects, Future<JsonArray> tableauDeConversionFuture, Future<Boolean> isAvgSkillFuture, JsonObject compNotesByMatiere) {
        subjects.stream().forEach(matiere -> {
            JsonObject m = (JsonObject) matiere;
            compNotesByMatiere.getJsonObject(Field.BODY).getJsonArray(Field.BODY).stream().forEach(compMatiere -> {
                JsonObject c = (JsonObject) compMatiere;
                if (c.getString(Field.HEADER).equals(m.getString(Field.ID))) {
                    JsonArray competenceNotes = c.getJsonArray(Field.COMPETENCENOTES);
                    m.put(Field.COMPETENCESNOTES, competenceNotes);
                    m.put(Field.HASCOMPETENCESNOTES, !competenceNotes.isEmpty());
                    if (!competenceNotes.isEmpty()) {
                        JsonObject result = new JsonObject();
                        noteService.calculPositionnementAutoByEleveByMatiere(competencesNotesBySubject.get(m.getString(Field.ID)), result, false, tableauDeConversionFuture.result(),
                                null, null, isAvgSkillFuture.result());
                        JsonObject positionnement = (JsonObject) result.getJsonArray(POSITIONNEMENTS_AUTO).stream()
                                .filter(pos -> periodId.equals(((JsonObject) pos).getLong(Field.ID_PERIODE)))
                                .findFirst().orElse(null);
                        if (null != positionnement)
                            m.put(Field.POSITIONNEMENT, positionnement.getLong(Field.MOYENNE));
                    }
                }
            });
        });
    }

    private void buildCompetenceNotesMap(JsonArray compNotes, Map<String, Map<String, Long>> competenceNotesMap) {
        for (int i = 0; i < compNotes.size(); i++) {
            if (compNotes.getJsonObject(i) instanceof JsonObject) {
                JsonObject row = compNotes.getJsonObject(i);
                if (row.containsKey(Field.EMPTY)) {
                    continue;
                }
                String compKey = String.valueOf(row.getLong(Field.ID_COMPETENCE));
                String devoirKey = String.valueOf(row.getLong(Field.ID_DEVOIR));
                Long eval = row.getLong(Field.EVALUATION);
                Long niv_final = row.getLong(Field.NIVEAU_FINAL);
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
    }

    /**
     * Récupère le nom des enseignants de chacune des matières puis positionne
     * les devoirs de l'élève sur les bonnes matières et enfin génère le PDF associé
     * formant le relevé de notes de l'élève.
     *
     * @param matieres      la liste des matières de l'élève.
     * @param idEnseignants
     * @param devoirsJson   la liste des devoirs et notes de l'élève.
     * @param periodeJson   la periode
     * @param userJson      l'élève
     * @param etabJson      l'établissement
     * @param showScores    if we want to show scores area.
     * @param showSkills    if we want to show skills area.
     * @param handler
     */
    public void getEnseignantsMatieres(final JsonArray matieres, JsonArray idEnseignants, final JsonArray devoirsJson,
                                       final JsonObject periodeJson, final JsonObject userJson,
                                       final JsonObject etabJson, final boolean isBackup, final JsonArray moyennesFinales,
                                       final JsonArray multiTeachers, final List<Service> services, boolean showScores,
                                       boolean showSkills, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getUsers")
                .put(Field.IDUSERS, idEnseignants);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                JsonArray users = body.getJsonArray(RESULTS);
                JsonArray subjects;

                if (null != matieres && !matieres.isEmpty() && showSkills && !showScores) {
                    subjects = new JsonArray(matieres.stream()
                            .filter(matiere -> ((JsonObject) matiere).getBoolean(Field.HASCOMPETENCESNOTES))
                            .collect(Collectors.toList()));
                } else {
                    subjects = matieres;
                }

                HashMap<String, Boolean> printSousMatiere = new HashMap<>();
                printSousMatiere.put(Field.SHOWSUBSUBJECTS, false);
                subjects.stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .forEach(subject -> {
                            String subjectId = subject.getString(Field.ID);
                            subject.put(Field.SHOWSCORES, showScores)
                                    .put(Field.SHOWSKILLS, showSkills);

                            int rowspan = subject.getJsonArray(Field.SOUS_MATIERES).size();
                            subject.put(Field.ROWSPAN, rowspan);

                            if (rowspan > 0 && Boolean.FALSE.equals(printSousMatiere.get(Field.SHOWSUBSUBJECTS)) && showScores)
                                printSousMatiere.put(Field.SHOWSUBSUBJECTS, true);


                            JsonObject moyenneFinale = moyennesFinales.stream()
                                    .filter(JsonObject.class::isInstance)
                                    .map(JsonObject.class::cast)
                                    .filter(moyennesFinale -> subjectId.equals(moyennesFinale.getString(Field.ID_MATIERE)))
                                    .findFirst().orElse(new JsonObject());

                            getDevoirsByMatiere(devoirsJson, subject, services, moyenneFinale, multiTeachers);
                            addSubjectsTeacherNames(subject, services, users, multiTeachers, periodeJson.getString(Field.START_DATE),
                                    periodeJson.getString(Field.END_DATE));
                        });

                final JsonObject templateProps = new JsonObject();

                templateProps.put("matieres", subjects);
                templateProps.put("periode", periodeJson);
                String prefixPdfName = "releve-eleve";
                if (isBackup) {
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

                String etablissementName = etabJson.getString("name").trim().replaceAll(" ", "-");
                prefixPdfName += "-" + etablissementName;
                templateProps.put("prefixPdfName", prefixPdfName);

                handler.handle(new Either.Right<>(templateProps));
            } else {
                String error = "getUsers : event bus get Users failed ";
                log.error(error + "\n" + body.encode() + "\n");
                handler.handle(new Either.Left<>(body.getString("message")));
            }
        }));
    }

    private void addSubjectsTeacherNames(JsonObject subject, List<Service> services, JsonArray users, JsonArray multiTeachers,
                                         String periodStart, String periodEnd) {
        JsonArray subjectTeacher = subject.getJsonArray(Field.DISPLAYNAMEENSEIGNANT, new JsonArray());
        String subjectId = subject.getString(Field.ID);

        services.stream()
                .filter(service -> subjectId.equals(service.getMatiere().getId()) && service.isVisible())
                .findFirst()
                .flatMap(service -> users.stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .filter(user -> service.getTeacher().getId().equals(user.getString(Field.ID)))
                        .findFirst())
                .ifPresent(user -> addSubjectTeacherNames(user, subject, subjectTeacher));

        multiTeachers.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .filter(multiTeacher -> {
                    String teacherStart = multiTeacher.getString(Field.START_DATE);
                    String teacherEnd = multiTeacher.getString(Field.END_DATE);
                    teacherStart = "".equals(teacherStart) ? null : teacherStart;
                    teacherEnd = "".equals(teacherEnd) ? null : teacherEnd;
                    return subjectId.equals(multiTeacher.getString(Field.SUBJECT_ID)) &&
                            multiTeacher.getString(Field.DELETED_DATE) == null &&
                            multiTeacher.getBoolean(Field.IS_VISIBLE, false) && (

                            (multiTeacher.getBoolean(Field.IS_COTEACHING, false) &&
                                    subject.getJsonArray(Field.DEVOIRS).stream()
                                            .filter(JsonObject.class::isInstance)
                                            .map(JsonObject.class::cast)
                                            .anyMatch(assessment -> assessment.getString(Field.OWNER, "")
                                                    .equals(multiTeacher.getString(Field.SECOND_TEACHER_ID)))) ||

                                    (!multiTeacher.getBoolean(Field.IS_COTEACHING, false) &&
                                            (periodStart == null || periodEnd == null ||
                                                    teacherStart == null || teacherEnd == null ||
                                                    DateHelper.isPeriodContainedWithinAnother(periodStart, periodEnd,
                                                            teacherStart, teacherEnd))));
                })
                .forEach(multiTeacher ->
                        users.stream()
                                .filter(JsonObject.class::isInstance)
                                .map(JsonObject.class::cast)
                                .filter(user -> multiTeacher.getString(Field.SECOND_TEACHER_ID)
                                        .equals(user.getString(Field.ID)))
                                .findFirst()
                                .ifPresent(user -> addSubjectTeacherNames(user, subject, subjectTeacher))
                );
    }

    private void addSubjectTeacherNames(JsonObject user, JsonObject subject, JsonArray subjectTeachers) {
        String displayName = String.format("%s.%s", user.getString(Field.FIRSTNAME, "")
                .charAt(0), user.getString(Field.NAME));
        if (!subjectTeachers.contains(displayName)) {
            subjectTeachers.add(displayName);
            subject.put(Field.DISPLAYNAMEENSEIGNANT, subjectTeachers);
        }
    }

    /**
     * Récupère les devoirs de la matière et les positionnent sur celle ci.
     *
     * @param devoirsJson   la liste de tous les devoirs de l'élève.
     * @param matiere       la matière dont on cherche les devoirs.
     * @param services
     * @param moyenneFinale
     */
    private void getDevoirsByMatiere(JsonArray devoirsJson, JsonObject matiere, List<Service> services, JsonObject moyenneFinale,
                                     final JsonArray multiTeachers) {
        DecimalFormat df = new DecimalFormat("0.#");
        df.setRoundingMode(RoundingMode.HALF_UP);//with this mode 2.125 -> 2.13 without 2.125 -> 2.12

        JsonArray devoirsMatiereJson = new JsonArray();
        List<NoteDevoir> listeNoteDevoirs = new ArrayList<>();
        Map<Long, List<NoteDevoir>> listNotesSousMatiere = new HashMap<>();
        Map<String, Map<Long, JsonArray>> devoirsSousMat = new HashMap<>();

        final String idMatiere = matiere.getString(Field.ID);

        for (int i = 0; i < devoirsJson.size(); i++) {
            JsonObject devoirJson = devoirsJson.getJsonObject(i);
            double sumNotes = 0.0;
            if (idMatiere.equals(devoirJson.getString(Field.ID_MATIERE))) {
                Long nbrEleves = devoirJson.getLong(Field.NBR_ELEVES);
                Long idSousMatiere = devoirJson.getLong(Field.ID_SOUSMATIERE);
                if (isNotNull(idSousMatiere)) {
                    if (!devoirsSousMat.containsKey(idMatiere)) {
                        devoirsSousMat.put(idMatiere, new HashMap<>());
                    }
                    if (!devoirsSousMat.get(idMatiere).containsKey(idSousMatiere)) {
                        devoirsSousMat.get(idMatiere).put(idSousMatiere, new JsonArray());
                    }
                    devoirsSousMat.get(idMatiere).get(idSousMatiere).add(devoirJson);
                }

                if (!devoirJson.containsKey(Field.IS_ANNOTATION)) {
                    boolean hasCoeff = devoirJson.getString(COEFFICIENT) != null;
                    Double coefficient = null;
                    if (hasCoeff) {
                        hasCoeff = !Double.valueOf(devoirJson.getString(COEFFICIENT)).equals(1d);
                        coefficient = Double.valueOf(devoirJson.getString(COEFFICIENT));
                    }
                    // boolean permettant de savoir s'il y a un coefficient différent de 1 sur la note
                    devoirJson.put(Field.HASCOEFF, hasCoeff);

                    // ajout du devoir sur la matiere, si son identifiant de matière correspond bien
                    if (isNotNull(coefficient)) {
                        devoirsMatiereJson.add(devoirJson);
                        Boolean formative = devoirJson.getBoolean(Field.FORMATIVE);

                        try {
                            sumNotes = Double.parseDouble(devoirJson.getString(Field.SUM_NOTES));
                        } catch (ClassCastException exc) {
                            log.error("[ getDevoirsByMatiere ] : sum_notes of devoirJson cannot be transform to double");
                        }

                        if (isNotNull(nbrEleves) && isNotNull(sumNotes)) {
                            devoirJson.put(Field.MOYENNECLASSE, df.format(sumNotes / nbrEleves));
                            devoirJson.put(Field.HASMOYENNECLASSE, true);
                            devoirJson.put(Field.HASDIVISEURCLASSE, true);
                        }

                        if (!formative) {
                            Double note = Double.valueOf(devoirJson.getString(Field.NOTE));
                            Double diviseur = Double.valueOf(devoirJson.getString(Field.DIVISEUR));
                            Boolean ramenerSur = devoirJson.getBoolean(Field.RAMENER_SUR);
                            NoteDevoir noteDevoir = new NoteDevoir(note, diviseur, ramenerSur, coefficient);

                            if (isNotNull(idSousMatiere)) {
                                if (!listNotesSousMatiere.containsKey(idSousMatiere)) {
                                    listNotesSousMatiere.put(idSousMatiere, new ArrayList<>());
                                }
                                listNotesSousMatiere.get(idSousMatiere).add(noteDevoir);

                            }
                            listeNoteDevoirs.add(noteDevoir);
                        }
                    }
                    devoirJson.put(Field.HASDIVISEUR, true);
                } else {

                    try {
                        if (devoirJson.getString(Field.SUM_NOTES) != null) {
                            sumNotes = Double.parseDouble(devoirJson.getString(Field.SUM_NOTES));
                        }
                    } catch (ClassCastException exc) {
                        log.error("[ getDevoirsByMatiere ] : sum_notes of devoirJson cannot be transform to double");
                    }

                    if (isNotNull(nbrEleves) && isNotNull(sumNotes)) {
                        devoirJson.put(Field.MOYENNECLASSE, df.format(sumNotes / nbrEleves));
                        devoirJson.put(Field.HASDIVISEURCLASSE, true);
                    } else {
                        devoirJson.put(Field.MOYENNECLASSE, Field.NN);
                        devoirJson.put(Field.HASDIVISEURCLASSE, false);
                    }
                    devoirJson.put(Field.HASMOYENNECLASSE, true);
                    devoirsMatiereJson.add(devoirJson);
                }
            }
        }
        matiere.put(DEVOIRS, devoirsMatiereJson);

        boolean hasDevoirs = !listeNoteDevoirs.isEmpty();
        matiere.put(Field.HASDEVOIRS, hasDevoirs);

        matiere.put(MOYENNE, Field.NN);
        if (!moyenneFinale.isEmpty()) {
            if (isNotNull(moyenneFinale.getValue(Field.MOYENNE))) {
                matiere.put(MOYENNE, moyenneFinale.getValue(Field.MOYENNE).toString());
                matiere.put(Field.HASDIVISEURMATIERE, true);
            }
        }

        handleHasDevoirMatiere(matiere, services, listeNoteDevoirs, listNotesSousMatiere, devoirsSousMat, idMatiere, hasDevoirs, multiTeachers);
    }

    private void handleHasDevoirMatiere(JsonObject matiere, List<Service> services, List<NoteDevoir> listeNoteDevoirs,
                                        Map<Long, List<NoteDevoir>> listNotesSousMatiere, Map<String,
            Map<Long, JsonArray>> devoirsSousMat, String idMatiere, boolean hasDevoirs, final JsonArray multiTeachers) {
        if (hasDevoirs) {
            Boolean statistiques = false;
            Boolean annual = false;
            int diviseur = 20;
            JsonArray sousMatieres = matiere.getJsonArray(Field.SOUS_MATIERES);
            matiere.put(Field.HASSOUSMATIERE, sousMatieres.size() > 0);
            JsonArray sousMatieresWithoutFirst = new JsonArray();
            // calcul de la moyenne de l'eleve pour la matiere
            if (matiere.getString(MOYENNE).equals(Field.NN)) {
                JsonObject moyenneMatiere = utilsService.calculMoyenne(listeNoteDevoirs, statistiques, diviseur, annual);
                if (moyenneMatiere.getValue(MOYENNE) != null && sousMatieres.size() == 0) {
                    matiere.put(MOYENNE, moyenneMatiere.getValue(MOYENNE).toString());
                }
            }
            matiere.put("hasDiviseurMatiere", !matiere.getValue(MOYENNE).equals("NN"));
            matiere.put(MOYENNE_NON_NOTE, matiere.getValue(MOYENNE).equals("NN"));
            double coefficient = 0.d;
            double moyenneTotal = 0.d;
            for (int i = 0; i < sousMatieres.size(); i++) {
                JsonObject sousMatiere = sousMatieres.getJsonObject(i);
                Long idSousMatiere = sousMatiere.getLong("id_type_sousmatiere");
                List<NoteDevoir> notesSousMat = listNotesSousMatiere.get(idSousMatiere);
                String moy = "NN";
                //Si aucun coeff de présent -> le coeff sera de 1.d
                double currentCoeff = 1.d;
                JsonObject moySousMatiere = null;
                if (isNotNull(notesSousMat)) {
                    moySousMatiere = utilsService.calculMoyenne(notesSousMat, statistiques, diviseur, annual);
                    moy = moySousMatiere.getValue(MOYENNE).toString();
                    if (!moy.equals(Field.NN))
                        moy += "/20";
                }
                sousMatiere.put(MOYENNE, moy).put("isLast", i == sousMatieres.size() - 1);
                sousMatiere.put(MOYENNE_NON_NOTE, sousMatiere.getValue(MOYENNE).equals("NN"));
                JsonArray devoirsSousMatieres = new JsonArray();
                if (isNotNull(devoirsSousMat.get(idMatiere))) {
                    if (isNotNull(devoirsSousMat.get(idMatiere).get(idSousMatiere))) {
                        devoirsSousMatieres = devoirsSousMat.get(idMatiere).get(idSousMatiere);
                    }
                }
                if (devoirsSousMatieres.size() > 0) {
                    JsonArray finalDevoirsSousMatieres = devoirsSousMatieres;

                    Teacher teacher = new Teacher(finalDevoirsSousMatieres.getJsonObject(0).getString(Field.OWNER));
                    Group group = new Group(finalDevoirsSousMatieres.getJsonObject(0).getString(Field.ID_GROUPE));

                    //Récupération du coeff de sous-matière en fonction du Service
                    Service service = services.stream()
                            .filter(ser -> idMatiere.equals(ser.getMatiere().getId()) &&
                                    ser.getTeacher().getId().equals(finalDevoirsSousMatieres.getJsonObject(0).getString(Field.OWNER)))
                            .findFirst().orElse(null);

                    if (service == null) {
                        //On regarde les multiTeacher
                        for (Object mutliTeachO : multiTeachers) {
                            JsonObject multiTeaching = (JsonObject) mutliTeachO;
                            if (multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(teacher.getId())
                                    && multiTeaching.getString(Field.ID_CLASSE).equals(group.getId())
                                    && multiTeaching.getString(Field.SUBJECT_ID).equals(idMatiere)) {
                                service = services.stream()
                                        .filter(el -> el.getTeacher().getId().equals(multiTeaching.getString(Field.SECOND_TEACHER_ID))
                                                && idMatiere.equals(el.getMatiere().getId())
                                                && group.getId().equals(el.getGroup().getId()))
                                        .findFirst().orElse(null);
                            }

                            if (multiTeaching.getString(Field.SECOND_TEACHER_ID).equals(teacher.getId())
                                    && multiTeaching.getString(Field.CLASS_OR_GROUP_ID).equals(group.getId())
                                    && multiTeaching.getString(Field.SUBJECT_ID).equals(idMatiere)) {

                                service = services.stream()
                                        .filter(el -> multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(el.getTeacher().getId())
                                                && idMatiere.equals(el.getMatiere().getId())
                                                && group.getId().equals(el.getGroup().getId()))
                                        .findFirst().orElse(null);
                            }
                        }
                    }

                    if (service != null) {
                        SubTopic subTopic = service.getSubtopics().stream()
                                .filter(subTopic1 -> idSousMatiere.equals(subTopic1.getId())
                                )
                                .findFirst().orElse(null);
                        if (subTopic != null) {
                            currentCoeff = subTopic.getCoefficient();
                        }
                    }
                }
                if (currentCoeff != 1)
                    sousMatiere.put(Field.COEFF, currentCoeff);
                if (moySousMatiere != null && !Field.NN.equals(moySousMatiere.getValue(Field.UNROUND_AVERAGE).toString())) {
                    coefficient += currentCoeff;
                    moyenneTotal += moySousMatiere.getDouble(Field.UNROUND_AVERAGE) * currentCoeff;
                }
                sousMatiere.put(DEVOIRS, devoirsSousMatieres);
                if (i == 0) {
                    matiere.put("first_sous_matieres", sousMatiere);
                } else {
                    sousMatieresWithoutFirst.add(sousMatiere);
                }
            }
            if (coefficient != 0)
                matiere.put(MOYENNE, NumberHelper.roundUpTenth(moyenneTotal / coefficient) + "/20");

            matiere.put(Field.SOUS_MATIERES + Field._TAIL, sousMatieresWithoutFirst);
        }
    }

    public void getDataForExportReleveClasse(String idClasse, String idEtablissement, Long idPeriode,
                                             Long idTypePeriode, final Long ordre, Boolean showScores, Boolean showSkills,
                                             Handler<Either<String, JsonObject>> handler) {
        Utils.getElevesClasse(eb, idClasse, idPeriode, elevesEvent -> {
            if (elevesEvent.isLeft()) {
                String error = elevesEvent.left().getValue();
                log.error("[getDataForExportReleveClasse]" + error);
                handler.handle(new Either.Left<>(getLibelle("evaluations.get.students.classe.error")));
                return;
            }
            JsonArray elevesClasse = elevesEvent.right().getValue();
            if (isNull(elevesClasse)) {
                log.error("[getDataForExportReleveClasse] : NO student in classe");
                handler.handle(new Either.Left<>(getLibelle("evaluations.export.releve.no.student")));
                return;
            }

            JsonArray exportResultClasse = new JsonArray();
            List<Future> classeFuture = new ArrayList<>();
            MultiMap params = MultiMap.caseInsensitiveMultiMap();
            params.add(Field.IDTYPEPERIODE, isNotNull(idTypePeriode) ? idTypePeriode.toString() : null)
                    .add(Field.ORDREPERIODE, isNotNull(ordre) ? ordre.toString() : null).add(Field.SHOWSCORES, String.valueOf(showScores))
                    .add(Field.SHOWSKILLS, String.valueOf(showSkills));
            getDataForClasse(elevesClasse, idEtablissement, idPeriode, params, exportResultClasse, classeFuture);

            CompositeFuture.all(classeFuture).setHandler(event -> {
                if (event.failed()) {
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
        for (int i = 0; i < elevesClasse.size(); i++) {
            JsonObject eleve = elevesClasse.getJsonObject(i);
            String idEleve = eleve.getString(ID_KEY);
            if (isNull(idEleve)) {
                idEleve = eleve.getString(ID_ELEVE_KEY);
            }
            Future eleveFuture = Future.future();

            classeFuture.add(eleveFuture);
            getDataForEleve(idEleve, idEtablissement, idPeriode, params, eleveFuture, exportResultClasse);
        }
    }

    private void getDataForEleve(String idEleve, String idEtablissement, Long idPeriode, MultiMap params,
                                 Future eleveFuture, JsonArray exportResultClasse) {
        getDataForExportReleveEleve(idEleve, idEtablissement, idPeriode, params, event -> {
            if (event.isLeft()) {
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