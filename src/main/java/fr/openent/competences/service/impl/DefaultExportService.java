package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.*;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static fr.wseduc.webutils.http.Renders.getHost;

public class DefaultExportService implements ExportService {

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

    public DefaultExportService(EventBus eb) {
        this.eb = eb;
        devoirService = new DefaultDevoirService();
        utilsService = new DefaultUtilsService();
        bfcService = new DefaultBFCService(eb);
        domaineService = new DefaultDomaineService(Competences.COMPETENCES_SCHEMA, Competences.DOMAINES_TABLE);
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_NOTES_TABLE);
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE);
        competencesService = new DefaultCompetencesService(eb);
        niveauDeMaitriseService = new DefaultNiveauDeMaitriseService();
        enseignementService = new DefaultEnseignementService(Competences.COMPETENCES_SCHEMA, Competences.ENSEIGNEMENTS_TABLE);
        annotationsService = new DefaultAnnotationService(Competences.COMPETENCES_SCHEMA, Competences.REL_ANNOTATIONS_DEVOIRS_TABLE);
    }

    @Override
    public void getExportEval(final Boolean text, final Boolean only_evaluation, final JsonObject devoir, String idGroupe, final String idEtablissement,
                              HttpServerRequest request, final Handler<Either<String, JsonObject>> handler) {

        Long idDevoir = devoir.getLong("id");
        final AtomicBoolean answered = new AtomicBoolean();
        final JsonArray elevesArray = new JsonArray();
        JsonArray notesArray = new JsonArray();
        JsonArray annotationsArray = new JsonArray();
        final JsonArray maitriseArray = new JsonArray();
        JsonArray competencesArray = new JsonArray();
        JsonArray competencesNotesArray = new JsonArray();

        final Handler<Either<String, JsonArray>> finalHandler = getDevoirFinalHandler(text, only_evaluation, devoir, request, elevesArray,
                maitriseArray, competencesArray, notesArray, competencesNotesArray, annotationsArray, answered, handler);

        JsonObject action = new JsonObject()
                .putString("action", "classe.getElevesGroupesClasses")
                .putArray("idClasses", new JsonArray().addString(idGroupe));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonArray eleves = message.body().getArray("results");
                getIntermediateHandler(elevesArray, finalHandler).handle(new Either.Right<String, JsonArray>(eleves));
            }
        });
        if(!only_evaluation){
            competencesService.getDevoirCompetences(idDevoir,
                    getIntermediateHandler(competencesArray, finalHandler));
            competenceNoteService.getCompetencesNotesDevoir(idDevoir,
                    getIntermediateHandler(competencesNotesArray, finalHandler));
            utilsService.getCycle(Arrays.asList(idGroupe), new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                    if (stringJsonArrayEither.isRight()) {
                        Long idCycle = ((JsonObject) stringJsonArrayEither.right().getValue().get(0)).getLong("id_cycle");
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
                                                for (int i = 0; i < competencesNotes.size(); i++) {
                                                    JsonObject competenceNote = competencesNotes.get(i);
                                                    if (!competenceNoteElevesMap.containsKey(competenceNote.getString("id_eleve"))) {
                                                        competenceNoteElevesMap.put(
                                                                competenceNote.getString("id_eleve"),
                                                                new HashMap<String, JsonObject>());
                                                    }
                                                    competenceNoteElevesMap.get(competenceNote.getString("id_eleve"))
                                                            .put(String.valueOf(competenceNote.getLong("id_competence")), competenceNote);
                                                }
                                                responseHandler.handle(new Either.Right<String, JsonObject>(
                                                        formatJsonObjectExportDevoir(text,
                                                                stringJsonObjectEither.right().getValue(),
                                                                extractData(orderBy(eleves, "lastName"), "id"),
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
                                                                extractData(orderBy(eleves, "lastName"), "id"),
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
        result.putBoolean("text", text);

        Map<String, String> competenceIndice = new LinkedHashMap<>();
        int i = 1;
        for (Map.Entry<String, JsonObject> competence : competences.entrySet()) {
            competenceIndice.put("[C" + String.valueOf(i) + "]", String.valueOf(competence.getValue().getLong("id_competence")));
            i++;
        }

        //Devoir
        devoir.removeField("id");
        result.putObject("devoir", devoir);

        //Maitrise
        JsonArray maitrisesArray = new JsonArray();
        for (JsonObject maitrise : maitrises.values()) {
            JsonObject _maitrise = new JsonObject();
            _maitrise.putString("libelle", maitrise.getString("libelle"));
            _maitrise.putString("visu", text ? getMaitrise(maitrise.getString("lettre"), String.valueOf(maitrise.getLong("ordre"))) : String.valueOf(maitrise.getLong("ordre")));
            maitrisesArray.add(_maitrise);
        }
        result.putArray("maitrise", maitrisesArray);

        //Competences
        JsonArray competencesArray = new JsonArray();
        for (Map.Entry<String, String> competence : competenceIndice.entrySet()) {
            competencesArray.addString(competence.getKey() + " " + competences.get(competence.getValue()).getString("code_domaine") + " " + competences.get(competence.getValue()).getString("nom"));
        }
        result.putArray("competence", competencesArray);

        //Eleves
        JsonArray elevesArray = new JsonArray();

        //Header
        JsonObject headerEleves = new JsonObject();
        headerEleves.putString("header", "");
        headerEleves.putString("note", "Note");
        headerEleves.putArray("competenceNotes", new JsonArray());
        for (String indice : competenceIndice.keySet()) {
            headerEleves.getArray("competenceNotes").addString(indice);
        }
        result.putObject("elevesHeader", headerEleves);

        //Body
        for (Map.Entry<String, JsonObject> eleve : eleves.entrySet()) {
            JsonObject eleveObject = new JsonObject();
            eleveObject.putString("header", eleve.getValue().getString("lastName").toUpperCase() + " " + eleve.getValue().getString("firstName"));

            String note = "";
            Boolean hasAnnotation = false;
            if (notes.containsKey(eleve.getKey())) {
                if (notes.get(eleve.getKey()).getString("appreciation") != null && !notes.get(eleve.getKey()).getString("appreciation").equals("")) {
                    eleveObject.putString("appreciation", notes.get(eleve.getKey()).getString("appreciation"));
                    eleveObject.putNumber("appreciationColspan", competences.size() + 1);
                }
                if (notes.get(eleve.getKey()).getLong("id_annotation") != null) {
                    note = annotations.get(String.valueOf(notes.get(eleve.getKey()).getLong("id_annotation"))).getString("libelle_court");
                    hasAnnotation = true;
                } else {
                    note = notes.get(eleve.getKey()).getString("valeur");
                }
            }
            eleveObject.putString("note", note);


            JsonArray comptenceNotesEleves = new JsonArray();
            for (String competence : competenceIndice.values()) {
                if (hasAnnotation) {
                    comptenceNotesEleves.addString("");
                } else if (competenceNotes.containsKey(eleve.getKey()) && competenceNotes.get(eleve.getKey()).containsKey(competence)) {
                    Map<String, JsonObject> competenceNotesEleve = competenceNotes.get(eleve.getKey());
                    String evaluation = String.valueOf(competenceNotesEleve.get(competence).getLong("evaluation"));
                    comptenceNotesEleves.addString(text ? getMaitrise(maitrises.get(String.valueOf(Integer.valueOf(evaluation) + 1)).getString("lettre"), String.valueOf(Integer.valueOf(evaluation) + 1))
                            : String.valueOf(Integer.valueOf(evaluation) + 1));
                } else {
                    comptenceNotesEleves.addString(text ? getMaitrise(maitrises.get("0").getString("lettre"), "0")
                            : "0");
                }
                eleveObject.putArray("competenceNotes", comptenceNotesEleves);
            }
            elevesArray.addObject(eleveObject);
        }
        result.putArray("eleves", elevesArray);

//        result.putString("height", String.valueOf(calcNumbLine(result)) + "%");

        return result;
    }
    @Override
    public void getExportReleveComp(final Boolean text, final Boolean pByEnseignement, final String idEleve, final String[] idGroupes,
                                    String[] idFunctionalGroupes, final String idEtablissement, final List<String> idMatieres,
                                    Long idPeriodeType, final Handler<Either<String, JsonObject>> handler) {

        final AtomicBoolean answered = new AtomicBoolean();
        final AtomicBoolean byEnseignement = new AtomicBoolean(pByEnseignement);
        final JsonArray maitriseArray = new JsonArray();
        final JsonArray enseignementArray = new JsonArray();
        final JsonArray devoirsArray = new JsonArray();
        final JsonArray competencesArray = new JsonArray();
        final JsonArray domainesArray = new JsonArray();
        final JsonArray competencesNotesArray = new JsonArray();
        String[] idMatieresTab = idMatieres.toArray(new String[0]);

        final Handler<Either<String, JsonArray>> finalHandler = getReleveCompFinalHandler(text, idEleve, devoirsArray,
                maitriseArray, competencesArray, domainesArray, competencesNotesArray,enseignementArray,  answered,byEnseignement, handler);

        //on recupere la liste des devoirs des classes mais aussi des groupes de l'eleve
        final List<String> idClasseAndFunctionnalGroups = new ArrayList<>();
        Collections.addAll(idClasseAndFunctionnalGroups, idGroupes);
        Collections.addAll(idClasseAndFunctionnalGroups, idFunctionalGroupes);


        devoirService.listDevoirs(idClasseAndFunctionnalGroups.toArray(new String[0]), null,
                idPeriodeType != null ? new Long[]{idPeriodeType} : null,
                idEtablissement != null ? new String[]{idEtablissement} : null,
                idMatieres != null ? idMatieresTab : null, null,
                getIntermediateHandler(devoirsArray, new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                        if(stringJsonArrayEither.isRight() &&
                                !(stringJsonArrayEither.right().getValue().get(0) instanceof String)) {
                            for (int i = 0; i < stringJsonArrayEither.right().getValue().size(); i++) {
                                Long idDevoir = ((JsonObject) stringJsonArrayEither.right().getValue().get(i))
                                        .getLong("id");
                                if (pByEnseignement) {
                                    competencesService.getDevoirCompetencesByEnseignement(idDevoir,
                                            getIntermediateHandler(idDevoir, competencesArray, finalHandler));
                                } else {
                                    competencesService.getDevoirCompetences(idDevoir,
                                            getIntermediateHandler(idDevoir, competencesArray, finalHandler));
                                }
                                competenceNoteService.getCompetencesNotes(idDevoir, idEleve,
                                        getIntermediateHandler(idDevoir, competencesNotesArray, finalHandler));
                            }
                            domaineService.getDomainesRacines(idGroupes[0],
                                    getIntermediateHandler(domainesArray, finalHandler));
                            enseignementService.getEnseignementsOrdered(
                                    getIntermediateHandler(enseignementArray, finalHandler));
                        } else if (stringJsonArrayEither.right().getValue().get(0) instanceof String){
                            if (pByEnseignement){
                                competencesService.getDevoirCompetencesByEnseignement(null,
                                        getIntermediateHandler(null, competencesArray, finalHandler));
                            } else {
                                competencesService.getDevoirCompetences(null,
                                        getIntermediateHandler(null, competencesArray, finalHandler));
                            }
                            competenceNoteService.getCompetencesNotes(null, idEleve,
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
                    Long idCycle = new Long( ((JsonObject) stringJsonArrayEither.right().getValue().get(0))
                            .getLong("id_cycle"));

                    for (int i = 0; i < stringJsonArrayEither.right().getValue().size(); i++) {
                        JsonObject cycleObj = stringJsonArrayEither.right().getValue().get(i);
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
                    JsonArray legende = new JsonArray();
                    JsonArray result = stringJsonArrayEither.right().getValue();
                    for (int i = result.size() - 1; i >= 0 ; i--){
                        JsonObject niveau = new JsonObject();
                        JsonObject o = result.get(i);
                        niveau.putString("libelle", o.getString("libelle"));
                        niveau.putString("visu", text ? getMaitrise(o.getString("lettre"),
                                o.getNumber("ordre").toString()) : o.getString("default"));
                        niveau.putNumber("ordre", o.getNumber("ordre"));
                        legende.add(niveau);
                    }
                    boolean b = true;
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
                        result.addString("empty");
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
                        obj.putNumber("id_devoir", idDevoir);
                        obj.putBoolean("empty", true);
                        result.addObject(obj);
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
            if (collection.get(i) instanceof JsonObject) {
                JsonObject row = collection.get(i);
                String keyValue = String.valueOf(row.getField(key));
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
            if(collection.get(i) instanceof String) {
                continue;
            }
            JsonObject item = collection.get(i);
            String itemKey = String.valueOf(item.getField(key));
            if(!result.containsKey(itemKey)) {
                result.put(itemKey, item);
            }
        }

        return result;
    }

    private JsonArray orderBy(JsonArray collection, String key, Boolean inverted) {
        Set<String> sortedSet = inverted ? new TreeSet<String>(Collections.reverseOrder()) : new TreeSet<String>();
        Map<String, JsonArray> unsortedMap = new HashMap<>();
        JsonArray result = new JsonArray();

        for (int i = 0; i < collection.size(); i++) {
            JsonObject item = collection.get(i);
            String itemKey = String.valueOf(item.getField(key));
            if(!unsortedMap.containsKey(itemKey)) {
                unsortedMap.put(itemKey, new JsonArray());
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
                                && (domainesDone.get() || enseignementsDone.get())
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
                                        if(competencesNotes.get(i) instanceof JsonObject) {
                                            JsonObject row = competencesNotes.get(i);
                                            if (row.containsField("empty")) {
                                                continue;
                                            }
                                            String compKey = String.valueOf(row.getLong("id_competence"));
                                            String devoirKey = String.valueOf(row.getLong("id_devoir"));
                                            Long eval = row.getLong("evaluation");
                                            if (!competenceNotesMap.containsKey(devoirKey)) {
                                                competenceNotesMap.put(devoirKey, new HashMap<String, Long>());
                                            }
                                            if (!competenceNotesMap.get(devoirKey).containsKey(compKey)) {
                                                competenceNotesMap.get(devoirKey).put(compKey, eval);
                                            }
                                        }
                                    }


                                    responseHandler.handle(new Either.Right<String, JsonObject>(
                                            formatJsonObjectExportReleveComp(
                                                    text,Boolean.valueOf(byEnseignement.get()), idEleve,
                                                    new ArrayList<>(extractData(devoirs, "id").keySet()),
                                                    extractData(orderBy(addMaitriseNE(maitrises),
                                                            "ordre", true), "ordre"),
                                                    extractData(competences, "id"),
                                                    extractData(domaines, "id"),
                                                    extractData(enseignements, "id"),
                                                    competenceNotesMap)
                                                    .putBoolean("noDevoir",false)));
                                }
                                else {
                                    responseHandler.handle(new Either.Right<String, JsonObject>(
                                            new JsonObject().putBoolean("text", text)
                                                    .putString("idEleve", idEleve)
                                                    .getObject("header", new JsonObject())
                                                    .putBoolean("noDevoir", true)

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
        nonEvalue.putString("libelle", libelle);
        nonEvalue.putNumber("ordre", 0);
        nonEvalue.putString("default", "grey");
        nonEvalue.putString("lettre", "NE");
        maitrises.addObject(nonEvalue);

        return maitrises;
    }

    private JsonObject formatJsonObjectExportReleveComp(Boolean text,Boolean byEnseignement, String idEleve, List<String> devoirs,
                                                        Map<String, JsonObject> maitrises,
                                                        Map<String, JsonObject> competences,
                                                        Map<String, JsonObject> domaines,
                                                        Map<String, JsonObject> enseignements,
                                                        Map<String, Map<String, Long>> competenceNotesByDevoir) {

        JsonObject result = new JsonObject();
        result.putBoolean("text", text);
        result.putString("idEleve", idEleve);

        JsonObject header = new JsonObject();
        JsonObject body = new JsonObject();

        //Maitrise
        JsonArray headerMiddle = new JsonArray();
        for (JsonObject maitrise : maitrises.values()) {
            JsonObject _maitrise = new JsonObject()
                    .putString("libelle", maitrise.getString("libelle"))
                    .putString("visu", text ? getMaitrise(maitrise
                            .getString("lettre"), String.valueOf(maitrise
                            .getLong("ordre"))) : String.valueOf(maitrise.getLong("ordre")));
            headerMiddle.add(_maitrise);
        }
        header.putArray("right", headerMiddle);
        result.putObject("header", header);

        final Map<String, JsonObject> competencesObjByIdComp = new HashMap<>();

        Map<String, Set<String>> competencesByDomainOrEnsei = new LinkedHashMap<>();
        if (byEnseignement) {
            for (String idEnseignement : enseignements.keySet()) {
                competencesByDomainOrEnsei.put(idEnseignement, new TreeSet<String>(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        String s1 = competencesObjByIdComp.get(o1).getString("nom");
                        String s2 = competencesObjByIdComp.get(o2).getString("nom");
                        return s1.compareTo(s2);
                    }
                }));
            }
        } else {
            for (String idDomain : domaines.keySet()) {
                competencesByDomainOrEnsei.put(idDomain, new TreeSet<String>(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        String s1 = competencesObjByIdComp.get(o1).getString("nom");
                        String s2 = competencesObjByIdComp.get(o2).getString("nom");
                        return s1.compareTo(s2);
                    }
                }));
            }
        }

        Map<String, List<String>> devoirByCompetences = new HashMap<>();
        for(JsonObject competence : competences.values()) {
            competencesObjByIdComp.put(String.valueOf(competence.getLong("id_competence")), competence);

            if(competence.containsField("empty")) {
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
            bodyHeader.putString("left", "Enseignements / items");
        } else  {
            bodyHeader.putString("left", "Domaines / items");
        }
        String right = new String("Niveau des compétences et Nombre d'évaluations".getBytes(), StandardCharsets.UTF_8);
        bodyHeader.putString("right", right);
        body.putObject("header", bodyHeader);

        JsonArray bodyBody = new JsonArray();
        for(Map.Entry<String, Set<String>> competencesInDomain : competencesByDomainOrEnsei.entrySet()) {
            JsonObject domainObj = new JsonObject();
            if (byEnseignement) {
                if(enseignements.get(competencesInDomain.getKey()) != null){
                    domainObj.putString("domainHeader", enseignements.get(competencesInDomain.getKey())
                            .getString("nom"));
                }
            } else {
                domainObj.putString("domainHeader", domaines.get(competencesInDomain.getKey())
                        .getString("codification") + " " + domaines.get(competencesInDomain.getKey())
                        .getString("libelle"));
            }
            JsonArray competencesInDomainArray = new JsonArray();
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
                competenceNote.putString("header", competencesObjByIdComp.get(competence).getString("nom"));
                competenceNote.putArray("competenceNotes", calcWidthNote(text, maitrises, valuesByComp, devoirs.size()));
                competencesInDomainArray.addObject(competenceNote);
            }
            domainObj.putArray("domainBody", competencesInDomainArray);
            bodyBody.addObject(domainObj);
        }

        body.putArray("body", bodyBody);

        result.putObject("body", body);
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
                .putString("action", "classe.getClasseInfo")
                .putString("idClasse", devoir.getString("id_groupe"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    devoirMap.put("classe", body.getObject("result").getObject("c").getObject("data").getString("name"));
                    finalHandler.handle(new Either.Right<String, Map<String, Object>>(devoirMap));
                } else {
                    finalHandler.handle(
                            new Either.Left<String, Map<String, Object>>(
                                    "getDevoirsInfos : devoir '" + devoirMap.get("id") + "), couldn't get class name."));
                }
            }
        });


        JsonObject matiereAction = new JsonObject()
                .putString("action", "matiere.getMatiere")
                .putString("idMatiere", devoir.getString("id_matiere"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, matiereAction, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    devoirMap.put("matiere", body.getObject("result").getObject("n").getObject("data").getString("label"));
                    finalHandler.handle(new Either.Right<String, Map<String, Object>>(devoirMap));
                } else {
                    finalHandler.handle(
                            new Either.Left<String, Map<String, Object>>(
                                    "getDevoirsInfos : devoir '" + devoirMap.get("id") + "), couldn't get matiere name."));
                }
            }
        });
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

        JsonArray resultList = new JsonArray();
        for(Map.Entry<Long, Integer> notesMaitrises : occNote.entrySet()) {
            JsonObject competenceNotesObj = new JsonObject();
            String number = (text ? getMaitrise(maitrises.get(String.valueOf(notesMaitrises.getKey())).getString("lettre"), String.valueOf(notesMaitrises.getKey())) + " - " : "") + String.valueOf(notesMaitrises.getValue());
            competenceNotesObj.putString("number", number);
            String color = text ? "white" : maitrises.get(String.valueOf(notesMaitrises.getKey())).getString("default");
            competenceNotesObj.putString("color", color);
            competenceNotesObj.putString("width", String.valueOf(notesMaitrises.getValue() / (double) nbDevoir * 100D));
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
}
