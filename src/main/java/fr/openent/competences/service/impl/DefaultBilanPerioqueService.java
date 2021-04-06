package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.service.*;
import fr.openent.competences.helpers.FormateFutureEvent;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import fr.openent.competences.enums.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.isNotNull;
import static fr.openent.competences.Utils.isNull;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.TIME;
import static fr.openent.competences.service.impl.DefaultExportService.COEFFICIENT;
import static fr.openent.competences.service.impl.DefaultNoteService.SOUS_MATIERES;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;

import fr.openent.competences.message.MessageResponseHandler;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.sql.SqlResult.validResultHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

public class DefaultBilanPerioqueService implements BilanPeriodiqueService{
    private static final Logger log = LoggerFactory.getLogger(DefaultBilanPerioqueService.class);
    private final NoteService noteService;
    private final UtilsService utilsService;
    private final DevoirService devoirService;
    private final ElementProgramme elementProgramme;
    private final EventBus eb;
    private final Sql sql;
    private final MatiereService defautlMatiereService;

    public DefaultBilanPerioqueService (EventBus eb){
        this.eb = eb;
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE, eb);
        utilsService = new DefaultUtilsService(eb);
        devoirService = new DefaultDevoirService(eb);
        elementProgramme = new DefaultElementProgramme() ;
        defautlMatiereService = new DefaultMatiereService(eb);
        sql = Sql.getInstance();
    }

    @Override
    public void getRetardsAndAbsences(String structureId, String idClasse, String idEleve, Handler<Either<String, JsonArray>> eitherHandler){
        // Récupération de l'état d'activation du module présences de l'établissement
        Future<JsonObject> activationFuture = Future.future();
        utilsService.getActiveStatePresences(structureId, event -> formate(activationFuture, event));

        // Récupération de l'état de la récupération des données du modules présences
        Future<JsonObject> syncFuture = Future.future();
        utilsService.getSyncStatePresences(structureId, event -> formate(syncFuture, event));

        CompositeFuture.all(syncFuture, activationFuture).setHandler(event -> {
            if(event.failed()){
                String error = event.cause().getMessage();
                log.error("[initRecuperationAbsencesRetardsFromPresences] : " + error);
                eitherHandler.handle(new Either.Left<>("[getRetardsAndAbsences-config] Failed"));
            } else{
                JsonObject activationState = activationFuture.result();
                JsonObject syncState = syncFuture.result();
                if(activationState.getBoolean("installed") && activationState.getBoolean("activate") &&
                        syncState.containsKey("presences_sync") && syncState.getBoolean("presences_sync")){
                    getRetardsAndAbsencesFromPresences(structureId, idClasse, idEleve, eitherHandler);
                }else{
                    getRetardsAndAbsencesFromCompetences(idEleve, eitherHandler);
                }
            }
        });
    }

    private void getRetardsAndAbsencesFromCompetences(String idEleve, Handler<Either<String, JsonArray>> eitherHandler){
        JsonArray params = new JsonArray().add(idEleve);

        String query = "SELECT * " +
                "FROM viesco.absences_et_retards " +
                "WHERE id_eleve = ?";

        sql.prepared(query, params, Competences.DELIVERY_OPTIONS, validResultHandler(eitherHandler));
    }

    private void getRetardsAndAbsencesFromPresences(String structureId, String idClasse, String idEleve,
                                                    Handler<Either<String, JsonArray>> eitherHandler) {
        Future<JsonArray> periodesFuture = Future.future();
        utilsService.getPeriodes(Collections.singletonList(idClasse), structureId, event -> formate(periodesFuture, event));

        Future<JsonArray> reasonsFuture = Future.future();
        utilsService.getPresencesReasonsId(structureId, event -> formate(reasonsFuture, event));

        CompositeFuture.all(periodesFuture, reasonsFuture).setHandler(eventParams -> {
            if (eventParams.failed()) {
                String error = eventParams.cause().getMessage();
                log.error("[getRetardsAndAbsencesFromPresences] : " + error);
                eitherHandler.handle(new Either.Left<>("[getRetardsAndAbsencesFromPresences] Failed"));
            } else {
                JsonArray periodes = periodesFuture.result();
                JsonArray reasons = reasonsFuture.result();
                List<Integer> reasonIds = ((List<JsonObject>) reasons.getList())
                        .stream()
                        .map(reason -> reason.getLong("id").intValue())
                        .collect(Collectors.toList());

                String beginningDateYear = periodes.getJsonObject(0).getString("timestamp_dt").substring(0, 10);
                String endDateYear = periodes.getJsonObject(periodes.size() - 1).getString("timestamp_fn").substring(0, 10);

                Future<JsonArray> absencesRegularizedFuture = Future.future();
                sendEventBusGetEvent(EventType.ABSENCE.getType(), Collections.singletonList(idEleve), structureId,
                        beginningDateYear, endDateYear, "HALF_DAY", true, false,
                        null, true, event -> formate(absencesRegularizedFuture, event));

                Future<JsonArray> absencesUnregularizedFuture = Future.future();
                sendEventBusGetEvent(EventType.ABSENCE.getType(), Collections.singletonList(idEleve), structureId,
                        beginningDateYear, endDateYear, "HALF_DAY", false, true,
                        reasonIds, true, event -> formate(absencesUnregularizedFuture, event));

                Future<JsonArray> retardsFuture = Future.future();
                sendEventBusGetEvent(EventType.LATENESS.getType(), Collections.singletonList(idEleve), structureId,
                        beginningDateYear, endDateYear, "HOUR", null, true,
                        null, null, event -> formate(retardsFuture, event));

                CompositeFuture.all(absencesRegularizedFuture, absencesUnregularizedFuture, retardsFuture).setHandler(event -> {
                    if (event.failed()) {
                        String message = event.cause().getMessage();
                        log.error("[getRetardsAndAbsencesFromPresences-getEventsStudent] : " + idEleve + " " + message);
                        eitherHandler.handle(new Either.Left<>("[getRetardsAndAbsences-getEventsStudent] Future Failed"));
                    } else {
                        JsonArray absencesRegularizedArray = absencesRegularizedFuture.result();
                        JsonArray absencesNotRegularizedArray = absencesUnregularizedFuture.result();
                        JsonArray retardsArray = retardsFuture.result();
                        JsonArray result = new JsonArray();
                        for (Object periode : periodes) {
                            JsonObject periodeJson = (JsonObject) periode;

                            LocalDateTime beginningDatePeriode = LocalDateTime.parse(periodeJson.getString("timestamp_dt"));
                            LocalDateTime endDatePeriode = LocalDateTime.parse(periodeJson.getString("timestamp_fn"));
                            Integer idPeriode = periodeJson.getInteger("id_type");

                            int nbrRetards = 0;
                            int nbrAbsenceJustificated = 0;
                            int minutesAbsenceJustificated = 0;
                            int nbrAbsenceUnjustificated = 0;
                            int minutesAbsenceUnjustificated = 0;

                            for (Object eventType : absencesRegularizedArray) {
                                JsonObject eventTypeJson = (JsonObject) eventType;

                                LocalDateTime eventStartDate = LocalDateTime.parse(eventTypeJson.getString("start_date").replace(" ", "T").substring(0, 19));
                                if (eventStartDate.isAfter(beginningDatePeriode) && eventStartDate.isBefore(endDatePeriode)) {
                                    for (Object eventAbsences : eventTypeJson.getJsonArray("events")) {
                                        JsonObject eventJson = (JsonObject) eventAbsences;

                                        eventStartDate = LocalDateTime.parse(eventJson.getString("start_date"));
                                        LocalDateTime eventEndDate = LocalDateTime.parse(eventJson.getString("end_date"));
                                        minutesAbsenceJustificated += ChronoUnit.MINUTES.between(eventStartDate, eventEndDate);
                                    }
                                    nbrAbsenceJustificated++;
                                }
                            }

                            for (Object eventType : absencesNotRegularizedArray) {
                                JsonObject eventTypeJson = (JsonObject) eventType;

                                LocalDateTime eventStartDate = LocalDateTime.parse(eventTypeJson.getString("start_date").replace(" ", "T").substring(0, 19));
                                if (eventStartDate.isAfter(beginningDatePeriode) && eventStartDate.isBefore(endDatePeriode)) {
                                    for (Object eventAbsences : eventTypeJson.getJsonArray("events")) {
                                        JsonObject eventJson = (JsonObject) eventAbsences;

                                        eventStartDate = LocalDateTime.parse(eventJson.getString("start_date"));
                                        LocalDateTime eventEndDate = LocalDateTime.parse(eventJson.getString("end_date"));
                                        minutesAbsenceUnjustificated += ChronoUnit.MINUTES.between(eventStartDate, eventEndDate);
                                    }
                                    nbrAbsenceUnjustificated++;
                                }
                            }

                            for (Object eventType : retardsArray) {
                                JsonObject eventTypeJson = (JsonObject) eventType;

                                LocalDateTime eventStartDate = LocalDateTime.parse(eventTypeJson.getString("start_date").replace(" ", "T").substring(0, 19));
                                if (eventStartDate.isAfter(beginningDatePeriode) && eventStartDate.isBefore(endDatePeriode)) {
                                    nbrRetards++;
                                }
                            }

                            int hoursAbsenceJustificated = (int) Math.round((long) minutesAbsenceJustificated / 60.0);
                            int hoursAbsenceUnjustificated = (int) Math.round((long) minutesAbsenceUnjustificated / 60.0);

                            JsonObject dataForPeriode = new JsonObject().put("id_periode", idPeriode).put("id_eleve", idEleve);
                            dataForPeriode.put("abs_just", nbrAbsenceJustificated);
                            dataForPeriode.put("abs_just_heure", hoursAbsenceJustificated);
                            dataForPeriode.put("abs_non_just", nbrAbsenceUnjustificated);
                            dataForPeriode.put("abs_non_just_heure", hoursAbsenceUnjustificated);
                            dataForPeriode.put("abs_totale", nbrAbsenceUnjustificated + nbrAbsenceJustificated);
                            dataForPeriode.put("abs_totale_heure", hoursAbsenceJustificated + hoursAbsenceUnjustificated);
                            dataForPeriode.put("retard", nbrRetards);
                            dataForPeriode.put("from_presences", true);

                            result.add(dataForPeriode);
                        }
                        eitherHandler.handle(new Either.Right<>(result));
                    }
                });
            }
        });
    }

    private void sendEventBusGetEvent(Integer eventType, List<String> students, String structure,
                                      String startDate, String endDate, String recoveryMethod, Boolean regularized,
                                      Boolean noReasons, List<Integer> reasonsId, Boolean compliance,
                                      Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("eventType", eventType)
                .put("students", new JsonArray(students))
                .put("structure", structure)
                .put("startDate", startDate)
                .put("endDate", endDate)
                .put("noReasons", noReasons)
                .put("recoveryMethod", recoveryMethod)
                .put("action", "get-events-by-student");

        if(regularized != null){
            action.put("regularized", regularized);
        }

        if(reasonsId != null){
            action.put("reasonsId", reasonsId);
        }

        if(compliance != null){
            action.put("compliance", compliance);
        }

        eb.send("fr.openent.presences", action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    //TODO A APPELER QU UNE FOIS -> Voir comment précharger
    public void getSubjectLibelleForSuivi(final String idEtablissement,
                                          Future<Map<String,JsonObject>> libelleMatiereFuture){

        // Récupération des matières de l'établissement
        Future<JsonArray> subjectF = Future.future();
        defautlMatiereService.getMatieresEtab(idEtablissement, event -> formate(subjectF, event));
        // Récupération des libellé court des matières
        Future<Map<String, String>> libelleCourtsFuture = Future.future();
        defautlMatiereService.getLibellesCourtsMatieres(true,
                event -> formate(libelleCourtsFuture, event));

        CompositeFuture.all(subjectF, libelleCourtsFuture).setHandler(
                event -> {
                    if(event.failed()){
                        String error = event.cause().getMessage();
                        log.error("[getSubjectLibelleForSuivi] : " + error);
                        if(error.contains(TIME)){
                            getSubjectLibelleForSuivi(idEtablissement, libelleMatiereFuture);
                        }
                        else {
                            libelleMatiereFuture.fail(error);
                        }
                        return;
                    }
                    Map mapCodeLibelleCourt = libelleCourtsFuture.result();
                    JsonArray subjects = subjectF.result();
                    Map<String,JsonObject> mapSubjects = new HashMap<>();

                    Utils.buildMapSubject(subjects, mapSubjects, mapCodeLibelleCourt);
                    libelleMatiereFuture.complete(mapSubjects);
                });

    }

    public void getSuiviAcquis(final String idEtablissement, final Long idPeriode, final String idEleve,
                               final String idClasse, Handler<Either<String, JsonArray>> handler) {
        // Récupération des groupes de l'élève
        Utils.getGroupesClasse(eb, new JsonArray().add(idClasse), responseGroupsClass -> {
            if(responseGroupsClass.isLeft()){
                String error = responseGroupsClass.left().getValue();
                log.error("[Competence] DefaultBilanPeriodique at getSuiviAcquis : getGroupesClasse " + error);
                handler.handle(new Either.Left<>(error));
            } else {
                JsonArray idGroups = responseGroupsClass.right().getValue();
                JsonArray idClasseGroups = new JsonArray();
                if (!(idGroups != null && !idGroups.isEmpty())) {
                    idClasseGroups.add(idClasse);
                } else {
                    idClasseGroups.add(idClasse);
                    idClasseGroups.addAll(idGroups.getJsonObject(0).getJsonArray("id_groupes"));
                }

                List<Future> listOfFutures = new ArrayList<>();

                Future<JsonArray> subjectEleveFuture = Future.future();
                // Récupération des matières et des professeurs
                devoirService.getMatiereTeacherForOneEleve(idEleve, idEtablissement, idClasseGroups, event -> {
                    formate("[Competences] DefaultBilanPeriodique at getSuivi : getMatiereTeacherOneEleve",
                            subjectEleveFuture, event);
                });
                listOfFutures.add(subjectEleveFuture);

                Future<JsonArray> allSubjectFuture = Future.future();
                // Récupération des matières et des professeurs
                devoirService.getMatiereTeacherForOneEleve(null, idEtablissement, idClasseGroups, event -> {
                    formate("[Competences] DefaultBilanPeriodique at getSuivi : getMatiereTeacherOneEleve",
                            allSubjectFuture, event);
                });
                listOfFutures.add(allSubjectFuture);

                Future<JsonArray> multiTeachersFuture = Future.future();
                utilsService.getMultiTeachers(idEtablissement, idClasseGroups, idPeriode != null ? idPeriode.intValue() : null, event -> {
                    formate("[Competences] DefaultBilanPeriodique at getSuiviAcqui : getMultiTeachers",
                            multiTeachersFuture, event);
                });
                listOfFutures.add(multiTeachersFuture);

                Future<JsonArray> servicesFuture = Future.future();
                utilsService.getServices(idEtablissement, idClasseGroups, event -> {
                    formate("[Competences] DefaultBilanPeriodique at getSuiviAcqui : getServices",
                            servicesFuture, event);
                });
                listOfFutures.add(servicesFuture);

                CompositeFuture.all(listOfFutures).setHandler(event -> {
                    if (event.succeeded()) {
                        Map<String, JsonObject> idsMatieresIdsTeachers = new HashMap<>();
                        JsonArray idsTeachers = new fr.wseduc.webutils.collections.JsonArray();
                        JsonArray responseArray = subjectEleveFuture.result();
                        JsonArray allSubjectsArray = allSubjectFuture.result();
                        JsonArray multiTeachersArray = multiTeachersFuture.result();
                        JsonArray services = servicesFuture.result();

                        if (responseArray == null || responseArray.isEmpty()) {
                            handler.handle(new Either.Right<>(new JsonArray()));
                        } else {
                            buildSubjectForSuivi(idsMatieresIdsTeachers, idsTeachers, responseArray, allSubjectsArray,
                                    idPeriode, multiTeachersArray, services);

                            List<Future> futures = new ArrayList<>();

                            // Récupération du libelle des matières et sous Matières
                            Future<Map<String, JsonObject>> libelleMatiereFuture = Future.future();
                            getSubjectLibelleForSuivi(idEtablissement, libelleMatiereFuture);
                            futures.add(libelleMatiereFuture);

                            // Récupération des noms et prénoms des professeurs
                            Future<Map<String, JsonObject>> lastNameAndFirstNameFuture = Future.future();
                            Utils.getLastNameFirstNameUser(eb, idsTeachers, lastNameAndFirstNameEvent -> {
                                FormateFutureEvent.formate(lastNameAndFirstNameFuture, lastNameAndFirstNameEvent);
                            });
                            futures.add(lastNameAndFirstNameFuture);

                            Future<JsonArray> groupsStudentFuture = Future.future();
                            Utils.getGroupsEleve(eb,idEleve,idEtablissement, responseGroupsStudent-> {
                                FormateFutureEvent.formate("[Competences] DefaultBilanPeriodique at getSuiviAcqui :",
                                        groupsStudentFuture, responseGroupsStudent);
                            });
                            futures.add(groupsStudentFuture);
                            CompositeFuture.all(futures).setHandler(
                                    setSubjectLibelleAndTeachersHandler(idEtablissement, idPeriode, idEleve, idClasse,
                                            handler, idsMatieresIdsTeachers, idClasseGroups, futures)
                            );
                        }
                    } else {
                        String error = event.cause().getMessage();
                        log.error("[Competences] getSuiviAcquisWithFuture " + error);
                        handler.handle(new Either.Left<>(error));
                    }
                });
            }
        });
    }

    private Handler<AsyncResult<CompositeFuture>>
    setSubjectLibelleAndTeachersHandler(String idEtablissement, Long idPeriode, String idEleve, String idClasse,
                                        Handler<Either<String, JsonArray>> handler,
                                        Map<String, JsonObject> idsMatieresIdsTeachers,
                                        JsonArray idClasseGroups, List<Future> futures) {
        return event1 -> {
            if(event1.succeeded()) {
                Map<String, JsonObject> idsMatLibelle = (Map<String, JsonObject>) futures.get(0).result();
                Map<String, JsonObject> teachersInfos = (Map<String, JsonObject>) futures.get(1).result();
                JsonArray groupsStudent = (JsonArray) futures.get(2).result();
                setSubjectLibelleAndTeachers(idEleve, idClasseGroups, idClasse, idEtablissement, groupsStudent,
                        idsMatieresIdsTeachers, idsMatLibelle, teachersInfos, idPeriode, handler);
            } else{
                handler.handle(new Either.Right<>(new JsonArray()));
            }
        };
    }


    private void setSubjectLibelle(String idMatiere, JsonObject result, Map<String, JsonObject> idsMatLibelle){
        if (idsMatLibelle != null && !idsMatLibelle.isEmpty() && idsMatLibelle.containsKey(idMatiere)) {
            result.put("id_matiere", idMatiere)
                    .put("libelleMatiere", idsMatLibelle.get(idMatiere).getString(NAME))
                    .put(SOUS_MATIERES, idsMatLibelle.get(idMatiere).getJsonArray("sous_matieres"))
                    .put("rank", idsMatLibelle.get(idMatiere).getInteger("rank"));
        } else {
            result.put("id_matiere", idMatiere)
                    .put("libelleMatiere", "no libelle")
                    .put("rank", 0);
            log.error("matiere non retrouve sans libelle idMatiere : " + idMatiere);
        }
    }

    private void setTeacherInfo(JsonObject result, JsonArray idsTeachers, Map<String, JsonObject> teachersInfos){
        if (idsTeachers != null) {
            JsonArray teachers = new fr.wseduc.webutils.collections.JsonArray();
            for (Object idTeacher : idsTeachers) {
                if (teachersInfos != null && !teachersInfos.isEmpty() && teachersInfos.containsKey(idTeacher)) {
                    teachers.add(teachersInfos.get(idTeacher));
                } else {
                    teachers.add(new JsonObject().put("id", idTeacher)
                            .put("firstName", "no first name").put("name", "no name"));
                    log.error("enseignant non retrouve idTeacher : " + idTeacher);
                }
            }
            result.put("teachers", teachers);
        }
    }

    private void setSubjectByCoeficient(String idMatiere, JsonObject result, JsonObject coefObject,
                                        Map<String, JsonObject> idsMatLibelle, Map<String, JsonObject> teachersInfos){
        if(isNotNull(coefObject)) {
            for(Map.Entry<String, Object> coefEntry : coefObject.getMap().entrySet()) {
                JsonArray coefIdTeachers = (JsonArray)coefEntry.getValue();
                String coefKey = coefEntry.getKey();
                if(!result.getJsonObject(COEFFICIENT).containsKey(coefKey)){
                    result.getJsonObject(COEFFICIENT).put(coefKey, new JsonObject());
                }
                JsonObject resultCoef = result.getJsonObject(COEFFICIENT).getJsonObject(coefKey);
                setSubjectLibelle(idMatiere, resultCoef, idsMatLibelle);
                setTeacherInfo(resultCoef, coefIdTeachers, teachersInfos);
            }
        }
    }

    private void setSubjectLibelleAndTeachers(String idEleve, JsonArray idClasseGroups, final String idClasse,
                                              String idEtablissement,JsonArray groupsStudent,
                                              Map<String,JsonObject> idsMatieresIdsTeachers,
                                              Map<String, JsonObject> idsMatLibelle,
                                              Map<String, JsonObject> teachersInfos, Long idPeriod,
                                              Handler<Either<String, JsonArray>> handler) {
        ArrayList<Future> subjectsFuture = new ArrayList<>();
        JsonArray results = new JsonArray();

        // For each subject build the result
        for (Map.Entry<String, JsonObject> idMAtTeachersGroups : idsMatieresIdsTeachers.entrySet()) {
            String idMatiere = idMAtTeachersGroups.getKey();
            JsonObject teachersObject = idMAtTeachersGroups.getValue();
            JsonArray idsTeachers = teachersObject.getJsonArray("teachers");
            final JsonObject result = new JsonObject().put(COEFFICIENT, new JsonObject());
            JsonObject coefObject = teachersObject.getJsonObject("_" +  COEFFICIENT);

            //Ajout des libellés des matières
            setSubjectLibelle(idMatiere, result, idsMatLibelle);
            // Ajout des enseignants des matières
            setTeacherInfo(result, idsTeachers, teachersInfos);
            // Mise Mise en forme des matières par coefficient
            setSubjectByCoeficient(idMatiere, result, coefObject, idsMatLibelle, teachersInfos);
            // Récupération des élements du Programme
            Future<JsonArray> elementsProgFuture = Future.future();
            elementProgramme.getElementProgrammeClasses(idPeriod, idMatiere, groupsStudent,
                    elementsProgEvent -> formate("[Competeces] DefaulteBilanPeriodique at setSubjectLibelleAndTeachers : getElementProgrammeClasses",
                            elementsProgFuture, elementsProgEvent));

            // Récupération des appreciation Moyenne Finale et positionnement Finale
            Future<JsonArray> appreciationMoyFinalePosFuture = Future.future();
            noteService.getAppreciationMoyFinalePositionnement(idEleve, idMatiere, null, idClasseGroups,
                    event -> formate(appreciationMoyFinalePosFuture, event));

            // Récupération des notes
            Future<JsonArray> notesFuture = Future.future();
            noteService.getNoteElevePeriode(null, idEtablissement, idClasseGroups, idMatiere, null,
                    notesEvent -> formate(notesFuture, notesEvent));

            // Récupération des compétences-notes
            Future<JsonArray> compNotesFuture =  Future.future();
            //USAGE DU IN NORMALEMENT
            noteService.getCompetencesNotesReleve(idEtablissement, null, null, idMatiere,
                    null, idEleve, null, false, false,
                    compNotesEvent -> formate(compNotesFuture, compNotesEvent));

            // Récupération de la moyenne finale
            Future<JsonArray> moyenneFinaleFuture = Future.future();
            noteService.getColonneReleve(null, null, idMatiere, idClasseGroups, "moyenne",
                    moyenneFinaleEvent -> formate(moyenneFinaleFuture, moyenneFinaleEvent));

            // Récupération du tableau de conversion
            Future<JsonArray> tableauDeConversionFuture = Future.future();
            // On récupère le tableau de conversion des compétences notes
            new DefaultCompetenceNoteService(COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE)
                    .getConversionNoteCompetence(idEtablissement, idClasse,
                            tableauEvent -> formate(tableauDeConversionFuture, tableauEvent));

            Future<String> subjectFuture = Future.future();
            subjectsFuture.add(subjectFuture);
            CompositeFuture.all(elementsProgFuture, appreciationMoyFinalePosFuture, notesFuture, compNotesFuture,
                    moyenneFinaleFuture, tableauDeConversionFuture).setHandler(event -> {
                if(event.succeeded()){
                    setElementProgramme(result, elementsProgFuture.result());
                    setAppreciationMoyFinalePositionnementEleve(result, appreciationMoyFinalePosFuture.result());
                    setMoyAndPosForSuivi(notesFuture.result(), compNotesFuture.result(), moyenneFinaleFuture.result(),
                            result, idEleve, idPeriod, tableauDeConversionFuture.result());
                    results.add(result);
                    subjectFuture.complete();
                } else {
                    subjectFuture.fail(event.cause().getMessage());
                }
            });
        }

        CompositeFuture.all(subjectsFuture).setHandler(event -> {
            if (event.succeeded()) {
                handler.handle(new Either.Right<>(Utils.sortJsonArrayIntValue("rank", results)));
            } else {
                String error = event.cause().getMessage();
                log.error(error);
                handler.handle(new Either.Left<>(error));
            }
        });
    }

    private void buildSubjectForSuivi(Map<String,JsonObject> idsMatieresIdsTeachers, JsonArray idsTeachers,
                                      JsonArray responseArray, JsonArray allSubjects, final Long idPeriode,
                                      JsonArray multiTeachers, JsonArray services) {
        List<String> subjectsMissingTeachers = new ArrayList<>();

        for (int i = 0; i < responseArray.size(); i++) {
            JsonObject responseObject = responseArray.getJsonObject(i);
            final String idMatiere = responseObject.getString(ID_MATIERE);
            String owner = responseObject.getString("owner");
            Long id_periode = responseObject.getLong("id_periode");
            Long coefficient = isNull(responseObject.getLong(COEFFICIENT)) ? 1L : responseObject.getLong(COEFFICIENT);

            if (!idsMatieresIdsTeachers.containsKey(idMatiere)) {
                idsMatieresIdsTeachers.put(idMatiere, new JsonObject()
                        .put("teachers", new JsonArray()).put("_" + COEFFICIENT, new JsonObject()));
            }
            JsonObject matiere = idsMatieresIdsTeachers.get(idMatiere);
            JsonArray teachers = matiere.getJsonArray("teachers");

            if (idPeriode.equals(id_periode)) {
                checkVisibilityAndAddTeachers(services,  owner, idMatiere, coefficient,
                        multiTeachers, teachers, idsTeachers,  matiere, subjectsMissingTeachers);
            }
        }

        // Ajoute les matieres manquantes pour un calcul correct de la moyenne général
        for (int i = 0; i < allSubjects.size(); i++) {
            final String idMatiere = allSubjects.getJsonObject(i).getString(ID_MATIERE);

            if (!idsMatieresIdsTeachers.containsKey(idMatiere)) {
                idsMatieresIdsTeachers.put(idMatiere, new JsonObject()
                        .put("teachers", new JsonArray()).put("_" + COEFFICIENT, new JsonObject()));
            }
        }

        if (!subjectsMissingTeachers.isEmpty()) {
            getMissingTeachers(idsTeachers, subjectsMissingTeachers, idsMatieresIdsTeachers, services);
        }
    }

    private void getMissingTeachers (JsonArray idsTeachers, List<String> subjectsMissingTeachers,
                                     Map<String,JsonObject> idsMatieresIdsTeachers, JsonArray services ) {

        subjectsMissingTeachers.forEach( idSubject -> {
            services.stream().forEach(service -> {

                JsonObject serviceObj = (JsonObject) service;
                String idServiceSubject = serviceObj.getString("id_matiere");

                if (idServiceSubject.equals(idSubject) && serviceObj.getBoolean("is_visible")) {
                    String owner = serviceObj.getString("id_enseignant");
                    Long coefficient = serviceObj.getLong(COEFFICIENT);;
                    coefficient = isNull(coefficient) ? 1L : coefficient;
                    JsonObject matiere = idsMatieresIdsTeachers.get(idSubject);
                    JsonArray teachers = matiere.getJsonArray("teachers");

                    if ( !teachers.contains(owner) && teachers.isEmpty() ) {
                        addTeachers (teachers, idsTeachers, owner, matiere, coefficient );
                    }
                }
            });
        });
    }

    private void addMultiTeachers (JsonArray multiTeachers, String idMatiere, JsonArray teachers, JsonArray idsTeachers){
        multiTeachers.forEach(item -> {
            JsonObject teacher = (JsonObject) item;

            String subjectId = teacher.getString("subject_id");
            String coTeacherId = teacher.getString("second_teacher_id");

            if (subjectId.equals(idMatiere)) {
                if (isNotNull(coTeacherId) && !teachers.contains(coTeacherId)) {
                    teachers.add(coTeacherId);
                }
                if (isNotNull(coTeacherId) && !idsTeachers.contains(coTeacherId))
                    idsTeachers.add(coTeacherId);
            }

        });
    }

    private void checkVisibilityAndAddTeachers(JsonArray services, String owner, final String idMatiere,
                                               Long coefficient, JsonArray multiTeachers, JsonArray teachers,
                                               JsonArray idsTeachers, JsonObject matiere,
                                               List<String> subjectsMissingTeachers){
        Boolean isVisible = false;
        for (int j = 0; j < services.size(); j++) {
            JsonObject service = services.getJsonObject(j);
            String serviceIdMatiere = service.getString("id_matiere");
            String serviceIdTeacher = service.getString("id_enseignant");

            if (isNotNull(owner)) {
                if (serviceIdMatiere.equals(idMatiere) && serviceIdTeacher.equals(owner)) {
                    isVisible = service.getBoolean("is_visible");
                    coefficient = service.getLong(COEFFICIENT);
                    break;
                }
            } else {
                if (serviceIdMatiere.equals(idMatiere) && service.getBoolean("is_visible")) {
                    owner = serviceIdTeacher;
                    isVisible = service.getBoolean("is_visible");
                    coefficient = service.getLong(COEFFICIENT);
                    break;
                }
            }
        }

        addMultiTeachers(multiTeachers, idMatiere, teachers, idsTeachers);

        if (isVisible && !teachers.contains(owner)) {
            addTeachers(teachers, idsTeachers, owner, matiere, coefficient);
        }
        if (teachers.isEmpty() && !subjectsMissingTeachers.contains(idMatiere))
            subjectsMissingTeachers.add(idMatiere);
        else if (!teachers.isEmpty())
            subjectsMissingTeachers.remove(idMatiere);
    }

    private void addTeachers (JsonArray teachers, JsonArray idsTeachers, String owner,
                              JsonObject matiere, Long coefficient) {

        teachers.add(owner);

        if ( !idsTeachers.contains(owner)) {
            idsTeachers.add(owner);
        }

        JsonObject coeffObject = matiere.getJsonObject("_" + COEFFICIENT);
        if (!coeffObject.containsKey(coefficient.toString())) {
            coeffObject.put(coefficient.toString(), new JsonArray());
        }
        if ( !coeffObject.getJsonArray(coefficient.toString()).contains(owner)) {
            coeffObject.getJsonArray(coefficient.toString()).add(owner);
        }
    }

    private void setElementProgramme(final JsonObject result, final JsonArray eltsProg) {
        String elementsProg = "";
        if (isNotNull(eltsProg) && eltsProg.size() > 0) {
            for (int i = 0; i < eltsProg.size(); i++) {
                JsonObject element;
                element = eltsProg.getJsonObject(i);
                if (isNull(element.getString("texte"))) {
                    element.put("texte", "");
                }
                if (elementsProg.isEmpty()) {
                    elementsProg = element.getString("texte");
                } else {
                    elementsProg += " " + element.getString("texte");
                }
            }
        }
        result.put("elementsProgramme", elementsProg);
    }


    private void  setAppreciationMoyFinalePositionnementEleve(final JsonObject result,
                                                              final JsonArray allAppMoyPosi){
        JsonArray appreciations = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray moyennesFinales = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray positionnements = new fr.wseduc.webutils.collections.JsonArray();
        if( allAppMoyPosi != null){

            Map<Integer, JsonArray> mapIdPeriodeAppreciations = new HashMap<>();
            for(int i = 0; i < allAppMoyPosi.size(); i++){
                JsonObject appMoyPosi = allAppMoyPosi.getJsonObject(i);
                if(appMoyPosi.getString("appreciation_matiere_periode") != null ) {

                    if(!mapIdPeriodeAppreciations.containsKey(
                            appMoyPosi.getInteger("id_periode_appreciation"))){

                        mapIdPeriodeAppreciations.put(appMoyPosi.getInteger("id_periode_appreciation"),
                                new fr.wseduc.webutils.collections.JsonArray().add(
                                        new JsonObject().put("idClasse",
                                                appMoyPosi.getString("id_classe_appreciation"))
                                                .put("appreciation",
                                                        appMoyPosi.getString("appreciation_matiere_periode"))));

                    }else {
                        Integer idPeriode = appMoyPosi.getInteger("id_periode_appreciation");
                        JsonArray appreciationsByIdPeriode = mapIdPeriodeAppreciations.get(idPeriode);
                        JsonObject appResponse = new JsonObject().put("idClasse",
                                appMoyPosi.getString("id_classe_appreciation"))
                                .put("appreciation",appMoyPosi.getString("appreciation_matiere_periode"));

                        if(!appreciationsByIdPeriode.contains(appResponse)){

                            mapIdPeriodeAppreciations.put(idPeriode, appreciationsByIdPeriode.add(appResponse) );
                        }
                    }
                }
                //on récupère la moyenne finale de l'élève pour sa classe principale = idClasse passé en
                // paramètre
                //moyennesFinales
                JsonObject moyenne_finale = new JsonObject();
                //if(appMoyPosi.getString("id_classe_moyfinale").equals(idClasse)) {
                // dans le contexte d'un matiere on est sensé n'avoir qu'une moyenne finale
                // qui est soit sur un groupe soit sur une classe
                if( isNotNull(appMoyPosi.getValue("moyenne_finale")) && isNotNull(appMoyPosi.getValue("id_periode_moyenne_finale"))) {
                    moyenne_finale.put("id_periode",
                            appMoyPosi.getInteger("id_periode_moyenne_finale"))

                            .put("moyenneFinale",
                                    Double.valueOf(appMoyPosi.getString("moyenne_finale")));
                }else if(isNotNull(appMoyPosi.getValue("id_periode_moyenne_finale"))){
                    moyenne_finale.put("id_periode",
                            appMoyPosi.getInteger("id_periode_moyenne_finale"))
                            .put("moyenneFinale", "NN");
                }
                if(!moyennesFinales.contains(moyenne_finale)){
                    moyennesFinales.add(moyenne_finale);
                }
                //}
                //Pour le positionnement on ne peut en avoir qu'un par matière
                //le positionnement n'est pas enregistré par classe
                if(appMoyPosi.getInteger("positionnement_final") != null){
                    JsonObject positionnement = new JsonObject();
                    positionnement.put("id_periode",
                            appMoyPosi.getInteger("id_periode_positionnement"))

                            .put("positionnementFinal",
                                    appMoyPosi.getInteger("positionnement_final"));

                    if(!positionnements.contains(positionnement)){
                        positionnements.add(positionnement);
                    }
                }
            }
            if(!mapIdPeriodeAppreciations.isEmpty()) {
                for (Map.Entry<Integer, JsonArray> idPeriodeApp : mapIdPeriodeAppreciations.entrySet()) {

                    appreciations.add(new JsonObject().put("id_periode",
                            idPeriodeApp.getKey()).put("appreciationByClasse", idPeriodeApp.getValue()));
                }
            }
        }

        result.put("appreciations",appreciations);
        result.put("positionnementsFinaux", positionnements);
        result.put("moyennesFinales",moyennesFinales);
    }

    private void setMoyAndPosForSuivi(JsonArray notes, JsonArray compNotes, JsonArray moyFinalesEleves,
                                      JsonObject result, String idEleve, Long idPeriodAsked,
                                      JsonArray tableauConversion) {
        JsonArray idsEleves = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse =
                noteService.calculMoyennesEleveByPeriode(notes, result, idEleve, idsEleves);
        noteService.getMoyennesMatieresByCoefficient(moyFinalesEleves, notes, result, idEleve, idsEleves);
        noteService.calculPositionnementAutoByEleveByMatiere(compNotes, result,false, tableauConversion);
        noteService.calculAndSetMoyenneClasseByPeriode(moyFinalesEleves, notesByDevoirByPeriodeClasse, result);
        noteService.setRankAndMinMaxInClasseByPeriode(idPeriodAsked, idEleve, notesByDevoirByPeriodeClasse, moyFinalesEleves, result);
    }

    public void getBilanPeriodiqueDomaineForGraph(final String idEleve,String idEtablissement,
                                                  final String idClasse,final Integer typeClasse, final String idPeriodeString,
                                                  final Handler<Either<String, JsonArray>> handler){
        Utils.getGroupsEleve(eb, idEleve, idEtablissement,  responseQuerry -> {
            if (!responseQuerry.isRight()) {
                String error = responseQuerry.left().getValue();
                log.error(error);
                handler.handle(new Either.Left<>(error));
            } else {
                JsonArray idGroups = responseQuerry.right().getValue();
                //idGroups null si l'eleve n'est pas dans un groupe
                new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE,eb)
                        .getDataGraphDomaine(idEleve, idGroups, idEtablissement, idClasse,
                                typeClasse, idPeriodeString, isNull(idPeriodeString), handler);
            }
        });
    }
}
