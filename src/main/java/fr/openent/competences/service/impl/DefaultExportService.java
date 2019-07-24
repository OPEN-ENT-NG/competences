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
import fr.openent.competences.service.*;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
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
import org.entcore.common.user.UserInfos;

import javax.imageio.ImageIO;

import static fr.openent.competences.Competences.TRANSITION_CONFIG;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.wseduc.webutils.http.Renders.badRequest;
import static fr.wseduc.webutils.http.Renders.getHost;
import static fr.wseduc.webutils.http.Renders.getScheme;

public class DefaultExportService implements ExportService  {

    protected static final Logger log = LoggerFactory.getLogger(DefaultExportService.class);

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
    private BFCService bfcService;
    private EventBus eb;
    private final Storage storage;

    public DefaultExportService(EventBus eb, Storage storage) {
        this.eb = eb;
        devoirService = new DefaultDevoirService(eb);
        utilsService = new DefaultUtilsService();
        bfcService = new DefaultBFCService(eb);
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
    public void getExportEval(final Boolean text, final Boolean only_evaluation, final JsonObject devoir,
                              String idGroupe,
                              final String idEtablissement,
                              HttpServerRequest request, final Handler<Either<String, JsonObject>> handler) {

        Long idDevoir = devoir.getLong("id");
        final AtomicBoolean answered = new AtomicBoolean();
        final JsonArray elevesArray = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray notesArray = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray annotationsArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray maitriseArray = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray competencesArray = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray competencesNotesArray = new fr.wseduc.webutils.collections.JsonArray();

        final Handler<Either<String, JsonArray>> finalHandler = getDevoirFinalHandler(text, only_evaluation, devoir, request, elevesArray,
                maitriseArray, competencesArray, notesArray, competencesNotesArray, annotationsArray, answered, handler);

        JsonObject action = new JsonObject()
                .put("action", "classe.getElevesClasses")
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray().add(idGroupe))
                .put("idPeriode", devoir.getLong("id_periode"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonArray eleves = message.body().getJsonArray("results");
                getIntermediateHandler(elevesArray, finalHandler).handle(new Either.Right<String, JsonArray>(eleves));
            }
        }));
        if(!only_evaluation){
            competencesService.getDevoirCompetences(idDevoir,
                    getIntermediateHandler(competencesArray, finalHandler));
            competenceNoteService.getCompetencesNotesDevoir(idDevoir,
                    getIntermediateHandler(competencesNotesArray, finalHandler));
            utilsService.getCycle(Arrays.asList(idGroupe), new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                    if (stringJsonArrayEither.isRight()) {
                        Long idCycle = ((JsonObject) stringJsonArrayEither.right().getValue().getJsonObject(0)).getLong("id_cycle");
                        niveauDeMaitriseService.getNiveauDeMaitrise(idEtablissement, idCycle,
                                getIntermediateHandler(maitriseArray, finalHandler));
                    } else {
                        finalHandler.handle(new Either.Left<String, JsonArray>(stringJsonArrayEither.left().getValue()));
                    }
                }
            });
        }
        noteService.listNotesParDevoir(idDevoir,
                getIntermediateHandler(notesArray, finalHandler));
        annotationsService.listAnnotations(idEtablissement,
                getIntermediateHandler(annotationsArray, finalHandler));
    }

    private Handler<Either<String, JsonArray>> getDevoirFinalHandler(final Boolean text, final Boolean only_evaluation,
                                                                     final JsonObject devoir, final HttpServerRequest request,
                                                                     final JsonArray eleves, final JsonArray maitrises,
                                                                     final JsonArray competences, final JsonArray notes,
                                                                     final JsonArray competencesNotes, final JsonArray annotations,
                                                                     final AtomicBoolean answered,
                                                                     final Handler<Either<String, JsonObject>> responseHandler) {

        final AtomicBoolean elevesDone = new AtomicBoolean();
        final AtomicBoolean maitriseDone = new AtomicBoolean();
        final AtomicBoolean competencesDone = new AtomicBoolean();
        final AtomicBoolean notesDone = new AtomicBoolean();
        final AtomicBoolean competencesNotesDone = new AtomicBoolean();
        final AtomicBoolean annotationsDone = new AtomicBoolean();

        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (!answered.get()) {
                    if (stringJsonArrayEither.isRight()) {

                        elevesDone.set(eleves.size() > 0);
                        maitriseDone.set(maitrises.size() > 0);
                        competencesDone.set(competences.size() > 0);
                        notesDone.set(notes.size() > 0);
                        competencesNotesDone.set(competencesNotes.size() > 0);
                        annotationsDone.set(annotations.size() > 0);

                        if(!only_evaluation){
                            if (elevesDone.get()
                                    && maitriseDone.get()
                                    && competencesDone.get()
                                    && notesDone.get()
                                    && competencesNotesDone.get()
                                    && annotationsDone.get()) {
                                answered.set(true);

                                if (eleves.contains("empty")
                                        || maitrises.contains("empty")
                                        || (competencesNotes.contains("empty") && notes.contains("empty"))
                                        || annotations.contains("empty")) {
                                    answered.set(true);
                                    responseHandler.handle(new Either.Left<String, JsonObject>("exportDevoir : empty result."));
                                } else {

                                    getDevoirInfos(devoir, request, new Handler<Either<String, JsonObject>>() {
                                        @Override
                                        public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                                            if (stringJsonObjectEither.isRight()) {
                                                Map<String, Map<String, JsonObject>> competenceNoteElevesMap = new HashMap<>();
                                                if(!competencesNotes.contains("empty")) {
                                                    for (int i = 0; i < competencesNotes.size(); i++) {
                                                        JsonObject competenceNote = competencesNotes.getJsonObject(i);
                                                        if (!competenceNoteElevesMap.containsKey(competenceNote.getString("id_eleve"))) {
                                                            competenceNoteElevesMap.put(
                                                                    competenceNote.getString("id_eleve"),
                                                                    new HashMap<String, JsonObject>());
                                                        }
                                                        competenceNoteElevesMap.get(competenceNote.getString("id_eleve"))
                                                                .put(String.valueOf(competenceNote.getLong("id_competence")), competenceNote);
                                                    }
                                                }
                                                responseHandler.handle(new Either.Right<String, JsonObject>(
                                                        formatJsonObjectExportDevoir(text,
                                                                stringJsonObjectEither.right().getValue(),
                                                                extractData(orderBy(eleves, "lastName"), "idEleve"),
                                                                extractData(orderBy(addMaitriseNE(maitrises), "ordre", true), "ordre"),
                                                                extractData(competences,"id_competence"),
                                                                extractData(notes, "id_eleve"),
                                                                extractData(annotations, "id"),
                                                                competenceNoteElevesMap)));

                                            } else {
                                                responseHandler.handle(new Either.Left<String, JsonObject>("formatJsonObjectExportDevoir : an error occured."));
                                            }
                                        }
                                    });
                                }
                            }
                        } else {
                            if (elevesDone.get() && notesDone.get() && annotationsDone.get()) {
                                answered.set(true);

                                if (eleves.contains("empty") ||  notes.contains("empty") || annotations.contains("empty")) {
                                    answered.set(true);
                                    responseHandler.handle(new Either.Left<String, JsonObject>("exportDevoir : empty result."));
                                } else {

                                    getDevoirInfos(devoir, request, new Handler<Either<String, JsonObject>>() {
                                        @Override
                                        public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                                            if (stringJsonObjectEither.isRight()) {
                                                Map<String, Map<String, JsonObject>> competenceNoteElevesMap = new HashMap<>();

                                                responseHandler.handle(new Either.Right<String, JsonObject>(
                                                        formatJsonObjectExportDevoir(text,
                                                                stringJsonObjectEither.right().getValue(),
                                                                extractData(orderBy(eleves, "lastName"), "idEleve"),
                                                                extractData(orderBy(addMaitriseNE(maitrises), "ordre", true), "ordre"),
                                                                extractData(competences,"id_competence"),
                                                                extractData(notes, "id_eleve"),
                                                                extractData(annotations, "id"),
                                                                competenceNoteElevesMap)));

                                            } else {
                                                responseHandler.handle(new Either.Left<String, JsonObject>("formatJsonObjectExportDevoir : an error occured."));
                                            }
                                        }
                                    });
                                }
                            }
                        }

                    } else {
                        answered.set(true);
                        responseHandler.handle(new Either.Left<String, JsonObject>("exportDevoir : empty result."));
                    }
                }
            }
        };
    }

    private JsonObject formatJsonObjectExportDevoir(final Boolean text, final JsonObject devoir,
                                                    final Map<String, JsonObject> eleves,
                                                    final Map<String, JsonObject> maitrises,
                                                    final Map<String, JsonObject> competences,
                                                    final Map<String, JsonObject> notes,
                                                    final Map<String, JsonObject> annotations,
                                                    final Map<String, Map<String, JsonObject>> competenceNotes) {

        JsonObject result = new JsonObject();
        result.put("text", text);

        Map<String, String> competenceIndice = new LinkedHashMap<>();
        int i = 1;
        for (Map.Entry<String, JsonObject> competence : competences.entrySet()) {
            competenceIndice.put("[C" + String.valueOf(i) + "]", String.valueOf(competence.getValue().getLong("id_competence")));
            i++;
        }

        //Devoir
        devoir.remove("id");
        result.put("devoir", devoir);

        //Maitrise
        JsonArray maitrisesArray = new fr.wseduc.webutils.collections.JsonArray();
        for (JsonObject maitrise : maitrises.values()) {
            JsonObject _maitrise = new JsonObject();
            _maitrise.put("libelle", maitrise.getString("libelle"));
            _maitrise.put("visu", text ? getMaitrise(maitrise.getString("lettre"), String.valueOf(maitrise.getLong("ordre"))) : String.valueOf(maitrise.getLong("ordre")));
            maitrisesArray.add(_maitrise);
        }
        result.put("maitrise", maitrisesArray);

        //Competences
        JsonArray competencesArray = new fr.wseduc.webutils.collections.JsonArray();
        for (Map.Entry<String, String> competence : competenceIndice.entrySet()) {
            competencesArray.add(competence.getKey() + " " + competences.get(competence.getValue()).getString("code_domaine") + " " + competences.get(competence.getValue()).getString("nom"));
        }
        result.put("competence", competencesArray);

        //Eleves
        JsonArray elevesArray = new fr.wseduc.webutils.collections.JsonArray();

        //Header
        JsonObject headerEleves = new JsonObject();
        headerEleves.put("header", "");
        headerEleves.put("note", "Note");
        headerEleves.put("competenceNotes", new fr.wseduc.webutils.collections.JsonArray());
        for (String indice : competenceIndice.keySet()) {
            headerEleves.getJsonArray("competenceNotes").add(indice);
        }
        result.put("elevesHeader", headerEleves);

        //Body
        for (Map.Entry<String, JsonObject> eleve : eleves.entrySet()) {
            JsonObject eleveObject = new JsonObject();
            eleveObject.put("header", eleve.getValue().getString("lastName").toUpperCase() + " " + eleve.getValue().getString("firstName"));

            String note = "";
            Boolean hasAnnotation = false;
            if (notes.containsKey(eleve.getKey())) {
                if (notes.get(eleve.getKey()).getString("appreciation") != null && !notes.get(eleve.getKey()).getString("appreciation").equals("")) {
                    eleveObject.put("appreciation", notes.get(eleve.getKey()).getString("appreciation"));
                    eleveObject.put("appreciationColspan", competences.size() + 1);
                }
                if (notes.get(eleve.getKey()).getLong("id_annotation") != null) {
                    note = annotations.get(String.valueOf(notes.get(eleve.getKey()).getLong("id_annotation"))).getString("libelle_court");
                    hasAnnotation = true;
                } else {
                    note = notes.get(eleve.getKey()).getString("valeur");
                }
            }
            eleveObject.put("note", note);


            JsonArray comptenceNotesEleves = new fr.wseduc.webutils.collections.JsonArray();
            for (String competence : competenceIndice.values()) {
                if (hasAnnotation) {
                    comptenceNotesEleves.add("");
                } else if (competenceNotes.containsKey(eleve.getKey()) && competenceNotes.get(eleve.getKey()).containsKey(competence)) {
                    Map<String, JsonObject> competenceNotesEleve = competenceNotes.get(eleve.getKey());
                    String evaluation = String.valueOf(competenceNotesEleve.get(competence).getLong("evaluation"));
                    comptenceNotesEleves.add(text ? getMaitrise(maitrises.get(String.valueOf(Integer.valueOf(evaluation) + 1)).getString("lettre"), String.valueOf(Integer.valueOf(evaluation) + 1))
                            : String.valueOf(Integer.valueOf(evaluation) + 1));
                } else {
                    comptenceNotesEleves.add(text ? getMaitrise(maitrises.get("0").getString("lettre"), "0")
                            : "0");
                }
                eleveObject.put("competenceNotes", comptenceNotesEleves);
            }
            elevesArray.add(eleveObject);
        }
        result.put("eleves", elevesArray);

//        result.put("height", String.valueOf(calcNumbLine(result)) + "%");

        return result;
    }
    @Override
    public void getExportReleveComp(final Boolean text, final Boolean pByEnseignement, final String idEleve, final String[] idGroupes,
                                    String[] idFunctionalGroupes, final String idEtablissement, final List<String> idMatieres,
                                    Long idPeriodeType, Boolean isCycle, final Handler<Either<String, JsonObject>> handler) {

        final AtomicBoolean answered = new AtomicBoolean();
        final AtomicBoolean byEnseignement = new AtomicBoolean(pByEnseignement);
        final JsonArray maitriseArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray enseignementArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray devoirsArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray competencesArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray domainesArray = new fr.wseduc.webutils.collections.JsonArray();
        final JsonArray competencesNotesArray = new fr.wseduc.webutils.collections.JsonArray();
        String[] idMatieresTab = idMatieres.toArray(new String[0]);

        final Handler<Either<String, JsonArray>> finalHandler = getReleveCompFinalHandler(text, idEleve, devoirsArray,
                maitriseArray, competencesArray, domainesArray, competencesNotesArray,enseignementArray,  answered,byEnseignement, handler);

        //on recupere la liste des devoirs des classes mais aussi des groupes de l'eleve
        final List<String> idClasseAndFunctionnalGroups = new ArrayList<>();
        Collections.addAll(idClasseAndFunctionnalGroups, idGroupes);
        Collections.addAll(idClasseAndFunctionnalGroups, idFunctionalGroupes);


        devoirService.listDevoirs(idEleve, idClasseAndFunctionnalGroups.toArray(new String[0]), null,
                idPeriodeType != null ? new Long[]{idPeriodeType} : null,
                idEtablissement != null ? new String[]{idEtablissement} : null,
                idMatieres != null ? idMatieresTab : null, null ,isCycle,
                getIntermediateHandler(devoirsArray, new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                        if(stringJsonArrayEither.isRight() &&
                                !(stringJsonArrayEither.right().getValue().getValue(0) instanceof String)) {
                            for (int i = 0; i < stringJsonArrayEither.right().getValue().size(); i++) {
                                Long idDevoir = ((JsonObject) stringJsonArrayEither.right().getValue().getJsonObject(i))
                                        .getLong("id");
                                if (pByEnseignement) {
                                    competencesService.getDevoirCompetencesByEnseignement(idDevoir,
                                            getIntermediateHandler(idDevoir, competencesArray, finalHandler));
                                } else {
                                    competencesService.getDevoirCompetences(idDevoir,
                                            getIntermediateHandler(idDevoir, competencesArray, finalHandler));
                                }
                                competenceNoteService.getCompetencesNotes(idDevoir, idEleve,
                                        true,
                                        getIntermediateHandler(idDevoir, competencesNotesArray, finalHandler));
                            }
                            domaineService.getDomainesRacines(idGroupes[0],
                                    getIntermediateHandler(domainesArray, finalHandler));
                            enseignementService.getEnseignementsOrdered(
                                    getIntermediateHandler(enseignementArray, finalHandler));
                        } else if (stringJsonArrayEither.isRight() && stringJsonArrayEither.right().getValue().getValue(0) instanceof String){
                            if (pByEnseignement){
                                competencesService.getDevoirCompetencesByEnseignement(null,
                                        getIntermediateHandler(null, competencesArray, finalHandler));
                            } else {
                                competencesService.getDevoirCompetences(null,
                                        getIntermediateHandler(null, competencesArray, finalHandler));
                            }
                            competenceNoteService.getCompetencesNotes(null, idEleve,
                                    true,
                                    getIntermediateHandler(null, competencesNotesArray, finalHandler));
                            domaineService.getDomainesRacines(idGroupes[0],
                                    getIntermediateHandler(domainesArray, finalHandler));
                            enseignementService.getEnseignementsOrdered(
                                    getIntermediateHandler(enseignementArray, finalHandler));
                        } else {
                            finalHandler.handle(stringJsonArrayEither.left());
                        }
                    }
                }));
        utilsService.getCycle(Arrays.asList(idGroupes), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isRight()) {
                    Long idCycle = new Long( ((JsonObject) stringJsonArrayEither.right().getValue().getJsonObject(0))
                            .getLong("id_cycle"));

                    for (int i = 0; i < stringJsonArrayEither.right().getValue().size(); i++) {
                        JsonObject cycleObj = stringJsonArrayEither.right().getValue().getJsonObject(i);
                        if(!idCycle.equals(cycleObj.getLong("id_cycle"))) {
                            finalHandler.handle(new Either.Left<String, JsonArray>(
                                    "getExportReleveComp : Given groups belong to different cycle."));
                        }
                    }
                    niveauDeMaitriseService.getNiveauDeMaitrise(idEtablissement, idCycle,
                            getIntermediateHandler(maitriseArray, finalHandler));
                } else {
                    finalHandler.handle(new Either.Left<String, JsonArray>(stringJsonArrayEither.left().getValue()));
                }
            }
        });
    }

    @Override
    public void getExportRecapEval(final Boolean text, final Long idCycle, final String idEtablissement,
                                   final Handler<Either<String, JsonArray>> handler){

        niveauDeMaitriseService.getNiveauDeMaitrise(idEtablissement, idCycle, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isRight()) {
                    JsonArray legende = new fr.wseduc.webutils.collections.JsonArray();
                    JsonArray result = stringJsonArrayEither.right().getValue();
                    for (int i = result.size() - 1; i >= 0 ; i--){
                        JsonObject niveau = new JsonObject();
                        JsonObject o = result.getJsonObject(i);
                        niveau.put("libelle", o.getString("libelle"));
                        niveau.put("visu", text ? getMaitrise(o.getString("lettre"),
                                o.getInteger("ordre").toString()) : o.getString("default"));
                        niveau.put("ordre", o.getInteger("ordre"));
                        legende.add(niveau);
                    }
                    handler.handle(new Either.Right<String, JsonArray>(legende));
                } else {
                    handler.handle(new Either.Left<String, JsonArray>("exportRecapEval : empty result."));
                }
            }
        });
    }

    private Handler<Either<String, JsonArray>>
    getIntermediateHandler(final JsonArray collection,
                           final Handler<Either<String, JsonArray>> finalHandler) {
        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isRight()) {
                    JsonArray result = stringJsonArrayEither.right().getValue();
                    if(result.size() == 0) {
                        result.add("empty");
                    }
                    utilsService.saUnion(collection, result);
                }
                finalHandler.handle(stringJsonArrayEither);
            }
        };
    }

    private Handler<Either<String, JsonArray>>
    getIntermediateHandler(final Long idDevoir,final JsonArray collection,
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
            }
            else {
                jsonShown ++;
            }
        }
        return keyShown.size() + jsonShown;
    }

    private Map<String, JsonObject> extractData(JsonArray collection, String key) {

        Map<String, JsonObject> result = new LinkedHashMap<>();

        for (int i = 0; i < collection.size(); i++) {
            if(collection.getValue(i) instanceof String) {
                continue;
            }
            JsonObject item = collection.getJsonObject(i);
            String itemKey = String.valueOf(item.getValue(key));
            if(!result.containsKey(itemKey)) {
                result.put(itemKey, item);
            }
        }

        return result;
    }

    private JsonArray orderBy(JsonArray collection, String key, Boolean inverted) {
        Set<String> sortedSet = inverted ? new TreeSet<String>(Collections.reverseOrder()) : new TreeSet<String>();
        Map<String, JsonArray> unsortedMap = new HashMap<>();
        JsonArray result = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < collection.size(); i++) {
            JsonObject item = collection.getJsonObject(i);
            String itemKey = String.valueOf(item.getValue(key));
            if(!unsortedMap.containsKey(itemKey)) {
                unsortedMap.put(itemKey, new fr.wseduc.webutils.collections.JsonArray());
            }
            unsortedMap.get(itemKey).add(item);
            sortedSet.add(itemKey);
        }

        for (String aSortedSet : sortedSet) {
            utilsService.saUnion(result, unsortedMap.get(aSortedSet));
        }
        return result;
    }

    private JsonArray orderBy(JsonArray collection, String key) {
        return orderBy (collection, key, false);
    }

    private Handler<Either<String, JsonArray>>
    getReleveCompFinalHandler(final Boolean text, final String idEleve, final JsonArray devoirs,
                              final JsonArray maitrises, final JsonArray competences,
                              final JsonArray domaines, final JsonArray competencesNotes,final JsonArray enseignements ,
                              final AtomicBoolean answered,final AtomicBoolean byEnseignement,
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
                        competencesDone.set(competences.size() > 0
                                && getNbDiffKey(competences, "id_devoir") == devoirs.size());
                        competencesNotesDone.set(competencesNotes.size() > 0
                                && getNbDiffKey(competencesNotes, "id_devoir") == devoirs.size());

                        if (devoirsDone.get()
                                && maitriseDone.get()
                                && ((domainesDone.get() && !byEnseignement.get())
                                || (byEnseignement.get() && enseignementsDone.get()))
                                && competencesDone.get()
                                && competencesNotesDone.get()
                                ) {
                            answered.set(true);

                            if (maitrises.contains("empty")) {
                                responseHandler.handle(new Either.Left<String, JsonObject>("devoirs not found"));
                            } else if (domaines.contains("empty") && !byEnseignement.get()) {
                                responseHandler.handle(new Either.Left<String, JsonObject>("domaines not found"));
                            } else if (enseignements.contains("empty") && byEnseignement.get()) {
                                responseHandler.handle(new Either.Left<String, JsonObject>("enseignements not found"));
                            } else {
                                if (!devoirs.contains("empty")) {
                                    Map<String, Map<String, Long>> competenceNotesMap = new HashMap<>();

                                    for (int i = 0; i < competencesNotes.size(); i++) {
                                        if(competencesNotes.getJsonObject(i) instanceof JsonObject) {
                                            JsonObject row = competencesNotes.getJsonObject(i);
                                            if (row.containsKey("empty")) {
                                                continue;
                                            }
                                            String compKey = String.valueOf(row.getLong("id_competence"));
                                            String devoirKey = String.valueOf(row.getLong("id_devoir"));
                                            Long eval = row.getLong("evaluation");
                                            Long niv_final = row.getLong("niveau_final");
                                            if (!competenceNotesMap.containsKey(devoirKey)) {
                                                competenceNotesMap.put(devoirKey, new HashMap<String, Long>());
                                            }
                                            if (!competenceNotesMap.get(devoirKey).containsKey(compKey)) {
                                                if (eval != null || niv_final != null) {
                                                    competenceNotesMap.get(devoirKey).put(compKey,
                                                            (niv_final != null) ? niv_final : eval);
                                                }
                                            }
                                        }
                                    }

                                    List<String> devoirsList =  new ArrayList<>(extractData(devoirs, "id").keySet());
                                    Map<String, JsonObject> maitrisesMap = extractData(
                                            orderBy(addMaitriseNE(maitrises),"ordre", true), "ordre");
                                    Map<String, JsonObject> competencesMap = extractData(competences, "id");
                                    Map<String, JsonObject> domainesMap = extractData(domaines, "id");
                                    Map<String, JsonObject> enseignementsMap = extractData(enseignements, "id");
                                    Map<String, Map<String, Long>> competenceNotesByDevoir = competenceNotesMap;

                                    JsonObject resToAdd = formatJsonObjectExportReleveComp(
                                            text,Boolean.valueOf(byEnseignement.get()), idEleve,devoirsList,
                                            maitrisesMap,competencesMap,domainesMap,
                                            enseignementsMap,
                                            competenceNotesMap)
                                            .put("noDevoir",false);

                                    responseHandler.handle(new Either.Right<String, JsonObject>(resToAdd));
                                }
                                else {
                                    responseHandler.handle(new Either.Right<String, JsonObject>(
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

    private JsonArray addMaitriseNE(JsonArray maitrises) {
        JsonObject nonEvalue = new JsonObject();
        String libelle = new String("Compétence non évaluée".getBytes(), StandardCharsets.UTF_8);
        nonEvalue.put("libelle", libelle);
        nonEvalue.put("ordre", 0);
        nonEvalue.put("default", "grey");
        nonEvalue.put("lettre", "NE");
        maitrises.add(nonEvalue);

        return maitrises;
    }

    private JsonObject formatJsonObjectExportReleveComp(Boolean text,Boolean byEnseignement, String idEleve,
                                                        List<String> devoirs,
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
                    .put("libelle", maitrise.getString("libelle"))
                    .put("visu", text ? getMaitrise(maitrise
                            .getString("lettre"), String.valueOf(maitrise
                            .getLong("ordre"))) : String.valueOf(maitrise.getLong("ordre")));
            headerMiddle.add(_maitrise);
        }
        header.put("right", headerMiddle);
        result.put("header", header);

        final Map<String, JsonObject> competencesObjByIdComp = new HashMap<>();

        Map<String, Set<String>> competencesByDomainOrEnsei = new LinkedHashMap<>();
        for (String idEntity : (byEnseignement)? enseignements.keySet(): domaines.keySet()) {
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
        for(JsonObject competence : competences.values()) {
            competencesObjByIdComp.put(String.valueOf(competence.getLong("id_competence")), competence);

            if(competence.containsKey("empty")) {
                continue;
            }
            String idDevoir = String.valueOf(competence.getLong("id_devoir"));
            String idComp = String.valueOf(competence.getLong("id_competence"));
            if(!devoirByCompetences.containsKey(idComp)) {
                devoirByCompetences.put(idComp, new ArrayList<String>());
            }
            devoirByCompetences.get(idComp).add(idDevoir);
            if (byEnseignement) {
                if (null != competence.getLong("id_enseignement")
                        && null != competencesByDomainOrEnsei.get(competence.getLong("id_enseignement").toString())) {
                    competencesByDomainOrEnsei.get(competence.getLong("id_enseignement").toString()).add(idComp);
                }
            } else {
                String[] idsDomain = competence.getString("ids_domaine").split(",");
                for(String idDomain : idsDomain) {
                    if (null != competencesByDomainOrEnsei.get(idDomain)) {
                        competencesByDomainOrEnsei.get(idDomain).add(idComp);
                    }
                }
            }
        }

        JsonObject bodyHeader = new JsonObject();
        if (byEnseignement) {
            bodyHeader.put("left", "Enseignements / items");
        } else  {
            bodyHeader.put("left", "Domaines / items");
        }
        String right = new String("Niveau des compétences et Nombre d'évaluations".getBytes(), StandardCharsets.UTF_8);
        bodyHeader.put("right", right);
        body.put("header", bodyHeader);

        JsonArray bodyBody = new fr.wseduc.webutils.collections.JsonArray();
        for(Map.Entry<String, Set<String>> competencesInDomain : competencesByDomainOrEnsei.entrySet()) {
            JsonObject domainObj = new JsonObject();
            if (byEnseignement) {
                if(enseignements.get(competencesInDomain.getKey()) != null){
                    domainObj.put("domainHeader", enseignements.get(competencesInDomain.getKey())
                            .getString("nom"));
                }
            } else {
                domainObj.put("domainHeader", domaines.get(competencesInDomain.getKey())
                        .getString("codification") + " " + domaines.get(competencesInDomain.getKey())
                        .getString("libelle"));
            }
            JsonArray competencesInDomainArray = new fr.wseduc.webutils.collections.JsonArray();
            for(String competence : competencesInDomain.getValue()) {
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
                competenceNote.put("competenceNotes", calcWidthNote(text, maitrises, valuesByComp, devoirs.size()));
                competencesInDomainArray.add(competenceNote);
            }
            domainObj.put("domainBody", competencesInDomainArray);
            bodyBody.add(domainObj);
        }

        body.put("body", bodyBody);

        result.put("body", body);
        return result;
    }

    private void getDevoirInfos(JsonObject devoir, final HttpServerRequest request, final Handler<Either<String, JsonObject>> handler) {

        final AtomicBoolean handled = new AtomicBoolean();
        final Map<String, Object> devoirMap = new HashMap<>();

        final Handler<Either<String, Map<String, Object>>> finalHandler = new Handler<Either<String, Map<String, Object>>>() {
            @Override
            public void handle(Either<String, Map<String, Object>> stringMapEither) {
                if (!handled.get()) {
                    if (stringMapEither.isRight()) {
                        Map<String, Object> devoir = stringMapEither.right().getValue();
                        int checkDevoirInfos = checkDevoirInfos(devoir);
                        if (checkDevoirInfos == 0) {
                            handled.set(true);
                            handler.handle(new Either.Right<String, JsonObject>(new JsonObject(devoir)));
                        } else if (checkDevoirInfos == 1) {
                            handled.set(true);
                            handler.handle(new Either.Left<String, JsonObject>("getDevoirsInfos : Devoir doesn't respect format."));
                        }
                    } else {
                        handled.set(true);
                        handler.handle(new Either.Left<String, JsonObject>("getDevoirsInfos : Error handled"));
                    }
                }
            }
        };
        String[] date = devoir.getString("date").substring(0, devoir.getString("date").indexOf(" ")).split("-");
        devoirMap.put("date", date[2] + '/' + date[1] + '/' + date[0]);

        devoirMap.put("id", devoir.getLong("id"));
        devoirMap.put("nom", devoir.getString("name"));
        devoirMap.put("coeff", devoir.getString("coefficient"));
        devoirMap.put("sur", devoir.getLong("diviseur"));
        devoirMap.put("periode", I18n.getInstance().translate(
                "viescolaire.periode." + String.valueOf(devoir.getLong("periodetype")),
                getHost(request),
                I18n.acceptLanguage(request)) + " " + String.valueOf(devoir.getLong("periodeordre")));

        JsonObject action = new JsonObject()
                .put("action", "classe.getClasseInfo")
                .put("idClasse", devoir.getString("id_groupe"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    devoirMap.put("classe", body.getJsonObject("result").getJsonObject("c").getJsonObject("data").getString("name"));
                    finalHandler.handle(new Either.Right<String, Map<String, Object>>(devoirMap));
                } else {
                    finalHandler.handle(
                            new Either.Left<String, Map<String, Object>>(
                                    "getDevoirsInfos : devoir '" + devoirMap.get("id") + "), couldn't get class name."));
                }
            }
        }));


        JsonObject matiereAction = new JsonObject()
                .put("action", "matiere.getMatiere")
                .put("idMatiere", devoir.getString("id_matiere"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, matiereAction, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    devoirMap.put("matiere", body.getJsonObject("result").getJsonObject("n").getJsonObject("data").getString("label"));
                    finalHandler.handle(new Either.Right<String, Map<String, Object>>(devoirMap));
                } else {
                    finalHandler.handle(
                            new Either.Left<String, Map<String, Object>>(
                                    "getDevoirsInfos : devoir '" + devoirMap.get("id") + "), couldn't get matiere name."));
                }
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

    private JsonArray calcWidthNote(Boolean text, Map<String, JsonObject> maitrises, List<Long> competenceNotes, Integer nbDevoir) {
        Map<Long, Integer> occNote = new HashMap<>();
        for(Long competenceNote : competenceNotes) {
            if(!occNote.containsKey(competenceNote)) {
                occNote.put(competenceNote, 0);
            }
            occNote.put(competenceNote, occNote.get(competenceNote) + 1);
        }

        JsonArray resultList = new fr.wseduc.webutils.collections.JsonArray();
        for(Map.Entry<Long, Integer> notesMaitrises : occNote.entrySet()) {
            JsonObject competenceNotesObj = new JsonObject();
            String number = (text ? getMaitrise(maitrises.get(String.valueOf(notesMaitrises.getKey())).getString("lettre"), String.valueOf(notesMaitrises.getKey())) + " - " : "") + String.valueOf(notesMaitrises.getValue());
            competenceNotesObj.put("number", number);
            String color = text ? "white" : maitrises.get(String.valueOf(notesMaitrises.getKey())).getString("default");
            competenceNotesObj.put("color", color);

            String width = "100"; // gestion cas 0 devoir
            if(nbDevoir > 0) {
                double tempWidth = notesMaitrises.getValue() / (double) nbDevoir * 100D;
                if(tempWidth < 1){
                    tempWidth = 1;
                }
                width =  String.valueOf(tempWidth);
            }
            competenceNotesObj.put("width", width);

            resultList.add(competenceNotesObj);
        }
        return resultList;
    }

    private String getMaitrise(String maitrise, String key){
        if(maitrise == null){
            return getMaitrise(key);
        } else if(maitrise.equals("  ")) {
            return getMaitrise(key);
        } else {
            return maitrise;
        }
    }

    private String getMaitrise(String key){
        switch (key) {
            case "1":
                return "MI";
            case "2":
                return "MF";
            case "3":
                return "MS";
            default:
                return "TB";
        }
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
    public void  genererPdf(final HttpServerRequest request, final JsonObject templateProps, final String templateName,
                            final String prefixPdfName, Vertx vertx, JsonObject config) {

        final String dateDebut = new SimpleDateFormat("dd.MM.yyyy").format(new Date().getTime());
        if (templateProps.containsKey("image") && templateProps.getBoolean("image")) {
            log.info(new SimpleDateFormat("HH:mm:ss:S").format(new Date().getTime())

                    + " -> Debut Generation Image du template " + templateName);
        } else {
            log.info(new SimpleDateFormat("HH:mm:ss:S").format(new Date().getTime())
                    + " -> Debut Generation PDF du template " + templateName);
        }
        if (null != templateProps){
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
        vertx.fileSystem().readFile(templatePath + templateName,
                new Handler<AsyncResult<Buffer>>() {

                    @Override
                    public void handle(AsyncResult<Buffer> result) {
                        if (!result.succeeded()) {
                            badRequest(request, "Error while reading template : " + templatePath
                                    + templateName);
                            log.error("Error while reading template : " + templatePath + templateName);
                            return;
                        }
                        StringReader reader = new StringReader(result.result().toString("UTF-8"));
                        Renders render = new Renders(vertx,config);
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

                                actionObject
                                        .put("content", bytes)
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
}
