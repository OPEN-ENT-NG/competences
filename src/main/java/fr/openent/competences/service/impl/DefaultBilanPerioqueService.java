package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.constants.Field;
import fr.openent.competences.enums.EventType;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.message.MessageResponseHandler;
import fr.openent.competences.model.Service;
import fr.openent.competences.model.achievements.AchievementsProgress;
import fr.openent.competences.service.*;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.isNotNull;
import static fr.openent.competences.Utils.isNull;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.TIME;
import static fr.openent.competences.service.impl.DefaultExportService.COEFFICIENT;
import static fr.openent.competences.service.impl.DefaultNoteService.SOUS_MATIERES;
import static org.entcore.common.sql.SqlResult.validResultHandler;

public class DefaultBilanPerioqueService implements BilanPeriodiqueService {
    private static final Logger log = LoggerFactory.getLogger(DefaultBilanPerioqueService.class);
    private final NoteService noteService;
    private final UtilsService utilsService;
    private final DevoirService devoirService;
    private final ElementProgramme elementProgramme;
    private final EventBus eb;
    private final Sql sql;
    private final MatiereService defautlMatiereService;
    private final StructureOptionsService structureOptionsService;


    public DefaultBilanPerioqueService(Sql sql, EventBus eb) {
        this.eb = eb;
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE, eb);
        utilsService = new DefaultUtilsService(eb);
        devoirService = new DefaultDevoirService(eb);
        elementProgramme = new DefaultElementProgramme();
        defautlMatiereService = new DefaultMatiereService(eb);
        structureOptionsService = new DefaultStructureOptions(eb);
        this.sql = sql;
    }

    @Override
    public void getRetardsAndAbsences(String structureId, List<String> idEleves, List<String> idClasses,
                                      Handler<Either<String, JsonArray>> eitherHandler) {
        // Récupération de l'état d'activation du module présences de l'établissement
        Future<JsonObject> activationFuture = Future.future();
        structureOptionsService.getActiveStatePresences(structureId, event -> formate(activationFuture, event));

        // Récupération de l'état de la récupération des données du modules présences
        Future<JsonObject> syncFuture = Future.future();
        structureOptionsService.getSyncStatePresences(structureId, event -> formate(syncFuture, event));

        CompositeFuture.all(syncFuture, activationFuture).setHandler(event -> {
            if (event.failed()) {
                String error = event.cause().getMessage();
                log.error("[initRecuperationAbsencesRetardsFromPresences] : " + error);
                eitherHandler.handle(new Either.Left<>("[getRetardsAndAbsences-config] Failed"));
            } else {
                JsonObject activationState = activationFuture.result();
                JsonObject syncState = syncFuture.result();
                if (activationState.getBoolean("installed") && activationState.getBoolean("activate") &&
                        syncState.containsKey("presences_sync") && syncState.getBoolean("presences_sync")) {
                    getRetardsAndAbsencesFromPresences(structureId, idEleves, idClasses, eitherHandler);
                } else {
                    getRetardsAndAbsencesFromCompetences(idEleves, eitherHandler);
                }
            }
        });
    }

    private void getRetardsAndAbsencesFromCompetences(List<String> idEleves,
                                                      Handler<Either<String, JsonArray>> eitherHandler) {
        String query = "SELECT id_periode, id_eleve, coalesce(abs_just, 0) as abs_just, coalesce(abs_just_heure, 0) as abs_just_heure, " +
                "coalesce(abs_non_just, 0) as abs_non_just, coalesce(abs_non_just_heure, 0) as abs_non_just_heure, " +
                "coalesce(abs_totale, 0) as abs_totale, coalesce(abs_totale_heure, 0) as abs_totale_heure, " +
                "coalesce(retard, 0) as retard " +
                "FROM " + VSCO_SCHEMA + ".absences_et_retards WHERE id_eleve IN " + Sql.listPrepared(idEleves);

        JsonArray params = new JsonArray();
        for (String idEleve : idEleves) {
            params.add(idEleve);
        }

        sql.prepared(query, params, Competences.DELIVERY_OPTIONS, validResultHandler(eitherHandler));
    }

    private void getRetardsAndAbsencesFromPresences(String structureId, List<String> idEleves, List<String> idClasses,
                                                    Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> periodesFuture = Future.future();

        utilsService.getPeriodes(idClasses, structureId, event -> formate(periodesFuture, event));

        Future<JsonArray> reasonsFuture = Future.future();
        utilsService.getPresencesReasonsId(structureId, event -> formate(reasonsFuture, event));

        CompositeFuture.all(periodesFuture, reasonsFuture).setHandler(eventParams -> {
            if (eventParams.failed()) {
                String error = eventParams.cause().getMessage();
                log.error("[getRetardsAndAbsencesFromPresences] : " + error);
                handler.handle(new Either.Left<>("[getRetardsAndAbsencesFromPresences] Failed"));
            } else {
                JsonArray periodes = periodesFuture.result();
                JsonArray reasons = reasonsFuture.result();
                List<Integer> reasonIds = ((List<JsonObject>) reasons.getList()).stream()
                        .map(reason -> reason.getLong("id").intValue())
                        .collect(Collectors.toList());
                String beginningDateYear = periodes.getJsonObject(0).getString("timestamp_dt").substring(0, 10);
                String endDateYear = periodes.getJsonObject(periodes.size() - 1).getString("timestamp_fn").substring(0, 10);

                Future<JsonArray> absencesRegularizedFuture = Future.future();
                sendEventBusGetEvent(EventType.ABSENCE.getType(), idEleves, structureId,
                        beginningDateYear, endDateYear, "HALF_DAY", true, false,
                        null, true, event -> formate(absencesRegularizedFuture, event));

                Future<JsonArray> absencesUnregularizedFuture = Future.future();
                sendEventBusGetEvent(EventType.ABSENCE.getType(), idEleves, structureId,
                        beginningDateYear, endDateYear, "HALF_DAY", false, true,
                        reasonIds, true, event -> formate(absencesUnregularizedFuture, event));

                Future<JsonArray> retardsFuture = Future.future();
                sendEventBusGetEvent(EventType.LATENESS.getType(), idEleves, structureId,
                        beginningDateYear, endDateYear, "HOUR", null, true,
                        reasonIds, true, event -> formate(retardsFuture, event));

                CompositeFuture.all(absencesRegularizedFuture, absencesUnregularizedFuture, retardsFuture).setHandler(event -> {
                    if (event.failed()) {
                        String message = event.cause().getMessage();
                        log.error("[getRetardsAndAbsencesFromPresences-getEventsStudent] : " + message);
                        handler.handle(new Either.Left<>("[getRetardsAndAbsences-getEventsStudent] Future Failed"));
                    } else {
                        JsonArray result = new JsonArray();

                        JsonArray absencesRegularizedArray = absencesRegularizedFuture.result();
                        JsonArray absencesNotRegularizedArray = absencesUnregularizedFuture.result();
                        JsonArray retardsArray = retardsFuture.result();

                        for (String idEleve : idEleves) {
                            for (Object periode : periodes) {
                                JsonObject periodeJson = (JsonObject) periode;

                                LocalDateTime beginningDatePeriode = LocalDateTime.parse(periodeJson.getString("timestamp_dt"));
                                LocalDateTime endDatePeriode = LocalDateTime.parse(periodeJson.getString("timestamp_fn"));
                                Integer idPeriode = periodeJson.getInteger("id_type");

                                int nbrRetards = 0;
                                int nbrAbsenceJustificated = 0;
                                int hoursAbsenceJustificated = 0;
                                int nbrAbsenceUnjustificated = 0;
                                int hoursAbsenceUnjustificated = 0;

                                JsonArray absencesRegularizedEleve = new JsonArray(absencesRegularizedArray.stream()
                                        .filter(el -> idEleve.equals(((JsonObject) el).getString("student_id")))
                                        .collect(Collectors.toList()));

                                JsonArray absencesNotRegularizedEleve = new JsonArray(absencesNotRegularizedArray.stream()
                                        .filter(el -> idEleve.equals(((JsonObject) el).getString("student_id")))
                                        .collect(Collectors.toList()));

                                JsonArray retardsEleve = new JsonArray(retardsArray.stream()
                                        .filter(el -> idEleve.equals(((JsonObject) el).getString("student_id")))
                                        .collect(Collectors.toList()));

                                for (Object eventType : absencesRegularizedEleve) {
                                    JsonObject eventTypeJson = (JsonObject) eventType;

                                    LocalDateTime eventStartDate = LocalDateTime.parse(eventTypeJson.getString("start_date").replace(" ", "T").substring(0, 19));
                                    if (eventStartDate.isAfter(beginningDatePeriode) && eventStartDate.isBefore(endDatePeriode)) {
                                        hoursAbsenceJustificated += eventTypeJson.getJsonArray("events").size();
                                        nbrAbsenceJustificated++;
                                    }
                                }

                                for (Object eventType : absencesNotRegularizedEleve) {
                                    JsonObject eventTypeJson = (JsonObject) eventType;

                                    LocalDateTime eventStartDate = LocalDateTime.parse(eventTypeJson.getString("start_date").replace(" ", "T").substring(0, 19));
                                    if (eventStartDate.isAfter(beginningDatePeriode) && eventStartDate.isBefore(endDatePeriode)) {
                                        hoursAbsenceUnjustificated += eventTypeJson.getJsonArray("events").size();
                                        nbrAbsenceUnjustificated++;
                                    }
                                }

                                for (Object eventType : retardsEleve) {
                                    JsonObject eventTypeJson = (JsonObject) eventType;

                                    LocalDateTime eventStartDate = LocalDateTime.parse(eventTypeJson.getString("start_date").replace(" ", "T").substring(0, 19));
                                    if (eventStartDate.isAfter(beginningDatePeriode) && eventStartDate.isBefore(endDatePeriode)) {
                                        nbrRetards++;
                                    }
                                }

                                JsonObject dataForPeriode = new JsonObject()
                                        .put("id_periode", idPeriode)
                                        .put("id_eleve", idEleve)
                                        .put("abs_just", nbrAbsenceJustificated)
                                        .put("abs_just_heure", hoursAbsenceJustificated)
                                        .put("abs_non_just", nbrAbsenceUnjustificated)
                                        .put("abs_non_just_heure", hoursAbsenceUnjustificated)
                                        .put("abs_totale", nbrAbsenceUnjustificated + nbrAbsenceJustificated)
                                        .put("abs_totale_heure", hoursAbsenceJustificated + hoursAbsenceUnjustificated)
                                        .put("retard", nbrRetards)
                                        .put("from_presences", true);

                                result.add(dataForPeriode);
                            }
                        }

                        handler.handle(new Either.Right<>(result));
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

        if (regularized != null) {
            action.put("regularized", regularized);
        }

        if (reasonsId != null) {
            action.put("reasonsId", reasonsId);
        }

        if (compliance != null) {
            action.put("compliance", compliance);
        }

        eb.send("fr.openent.presences", action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    //TODO A APPELER QU UNE FOIS -> Voir comment précharger
    public void getSubjectLibelleForSuivi(final String idEtablissement,
                                          Future<Map<String, JsonObject>> libelleMatiereFuture) {
        // Récupération des matières de l'établissement
        Future<JsonArray> subjectF = Future.future();
        defautlMatiereService.getMatieresEtab(idEtablissement, event -> formate(subjectF, event));

        // Récupération des libellé court des matières
        Future<Map<String, String>> libelleCourtsFuture = Future.future();
        defautlMatiereService.getLibellesCourtsMatieres(true,
                event -> formate(libelleCourtsFuture, event));

        CompositeFuture.all(subjectF, libelleCourtsFuture).setHandler(event -> {
            if (event.failed()) {
                String error = event.cause().getMessage();
                log.error("[getSubjectLibelleForSuivi] : " + error);
                if (error.contains(TIME)) {
                    getSubjectLibelleForSuivi(idEtablissement, libelleMatiereFuture);
                } else {
                    libelleMatiereFuture.fail(error);
                }
                return;
            }
            Map<String, String> mapCodeLibelleCourt = libelleCourtsFuture.result();
            JsonArray subjects = subjectF.result();
            Map<String, JsonObject> mapSubjects = new HashMap<>();

            Utils.buildMapSubject(subjects, mapSubjects, mapCodeLibelleCourt);
            libelleMatiereFuture.complete(mapSubjects);
        });
    }

    public void getSuiviAcquis(final String idEtablissement, final Long idPeriode, final String idEleve,
                               final JsonArray idClasseGroups, final List<Service> services, final JsonArray multiTeachers,
                               Handler<Either<String, JsonArray>> handler) {
        List<Future> listOfFutures = new ArrayList<>();

        Future<JsonArray> subjectEleveFuture = Future.future();
        devoirService.getMatiereTeacherForOneEleve(idEleve, idEtablissement, idClasseGroups, event -> {
            formate("[Competences] DefaultBilanPeriodique at getSuiviAcquis : getMatiereTeacherForOneEleve ",
                    subjectEleveFuture, event);
        });
        listOfFutures.add(subjectEleveFuture);

        Future<JsonArray> groupsStudentFuture = Future.future();
        Utils.getGroupsEleve(eb, idEleve, idEtablissement, responseGroupsStudent -> {
            formate("[Competences] DefaultBilanPeriodique at getSuiviAcquis : getGroupsEleve",
                    groupsStudentFuture, responseGroupsStudent);
        });
        listOfFutures.add(groupsStudentFuture);

        CompositeFuture.all(listOfFutures).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray subjects = subjectEleveFuture.result();
                JsonArray groupsStudent = groupsStudentFuture.result();

                if (subjects == null || subjects.isEmpty()) {
                    handler.handle(new Either.Right<>(new JsonArray()));
                } else {
                    Map<String, JsonObject> idsMatieresIdsTeachers = new HashMap<>();
                    JsonArray idsTeachers = new fr.wseduc.webutils.collections.JsonArray();

                    buildSubjectForSuivi(idsMatieresIdsTeachers, idsTeachers, subjects,
                            idPeriode, multiTeachers, services, groupsStudent);

                    List<Future> futures = new ArrayList<>();

                    // Récupération du libelle des matières et sous Matières
                    Future<Map<String, JsonObject>> libelleMatiereFuture = Future.future();
                    getSubjectLibelleForSuivi(idEtablissement, libelleMatiereFuture);
                    futures.add(libelleMatiereFuture);

                    // Récupération des noms et prénoms des professeurs
                    Future<Map<String, JsonObject>> lastNameAndFirstNameFuture = Future.future();
                    Utils.getLastNameFirstNameUser(eb, idsTeachers, lastNameAndFirstNameEvent -> {
                        formate(lastNameAndFirstNameFuture, lastNameAndFirstNameEvent);
                    });
                    futures.add(lastNameAndFirstNameFuture);

                    CompositeFuture.all(futures).setHandler(
                            setSubjectLibelleAndTeachersHandler(idEtablissement, idPeriode, idEleve,
                                    handler, idsMatieresIdsTeachers, idClasseGroups, groupsStudent, futures, multiTeachers, services)
                    );
                }
            } else {
                String error = event.cause().getMessage();
                log.error("[Competences] getSuiviAcquisWithFuture " + error);
                handler.handle(new Either.Left<>(error));
            }
        });
    }

    private Handler<AsyncResult<CompositeFuture>>
    setSubjectLibelleAndTeachersHandler(String idEtablissement, Long idPeriode, String idEleve,
                                        Handler<Either<String, JsonArray>> handler,
                                        Map<String, JsonObject> idsMatieresIdsTeachers,
                                        JsonArray idClasseGroups, JsonArray groupsStudent, List<Future> futures, JsonArray multiTeachers, List<Service> services) {
        return event -> {
            if (event.succeeded()) {
                Map<String, JsonObject> idsMatLibelle = (Map<String, JsonObject>) futures.get(0).result();
                Map<String, JsonObject> teachersInfos = (Map<String, JsonObject>) futures.get(1).result();
                setSubjectLibelleAndTeachers(idEleve, idClasseGroups, idEtablissement, groupsStudent,
                        idsMatieresIdsTeachers, idsMatLibelle, teachersInfos, idPeriode, multiTeachers, services, handler);
            } else {
                handler.handle(new Either.Right<>(new JsonArray()));
            }
        };
    }

    private void setSubjectLibelle(String idMatiere, JsonObject result, Map<String, JsonObject> idsMatLibelle) {
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

    private void setTeacherInfo(JsonObject result, JsonArray idsTeachers, Map<String, JsonObject> teachersInfos) {
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
                                        Map<String, JsonObject> idsMatLibelle, Map<String, JsonObject> teachersInfos) {
        if (isNotNull(coefObject)) {
            for (Map.Entry<String, Object> coefEntry : coefObject.getMap().entrySet()) {
                JsonArray coefIdTeachers = (JsonArray) coefEntry.getValue();
                String coefKey = coefEntry.getKey();
                if (!result.getJsonObject(COEFFICIENT).containsKey(coefKey)) {
                    result.getJsonObject(COEFFICIENT).put(coefKey, new JsonObject());
                }
                JsonObject resultCoef = result.getJsonObject(COEFFICIENT).getJsonObject(coefKey);
                setSubjectLibelle(idMatiere, resultCoef, idsMatLibelle);
                setTeacherInfo(resultCoef, coefIdTeachers, teachersInfos);
            }
        }
    }

    private void setSubjectLibelleAndTeachers(String idEleve, JsonArray idClasseGroups, String idEtablissement,
                                              JsonArray groupsStudent, Map<String, JsonObject> idsMatieresIdsTeachers,
                                              Map<String, JsonObject> idsMatLibelle,
                                              Map<String, JsonObject> teachersInfos, Long idPeriod,
                                              JsonArray multiTeachers, List<Service> services, Handler<Either<String, JsonArray>> handler) {
        ArrayList<Future> subjectsFuture = new ArrayList<>();
        JsonArray results = new JsonArray();

        String idClasse = idClasseGroups.getString(0);

        // For each subject build the result
        for (Map.Entry<String, JsonObject> idMatTeachersGroups : idsMatieresIdsTeachers.entrySet()) {
            String idMatiere = idMatTeachersGroups.getKey();
            JsonObject teachersObject = idMatTeachersGroups.getValue();
            JsonArray idsTeachers = teachersObject.getJsonArray("teachers");
            final JsonObject result = new JsonObject().put(COEFFICIENT, new JsonObject());
            JsonObject coefObject = teachersObject.getJsonObject("_" + COEFFICIENT);
            String idGroupe = teachersObject.getString("id_groupe");

            //Ajout des libellés des matières
            setSubjectLibelle(idMatiere, result, idsMatLibelle);
            // Ajout des enseignants des matières
            setTeacherInfo(result, idsTeachers, teachersInfos);
            // Mise en forme des matières par coefficient
            setSubjectByCoeficient(idMatiere, result, coefObject, idsMatLibelle, teachersInfos);

            result.put("idClasse", idGroupe);

            // Récupération des élements du Programme
            Future<JsonArray> elementsProgFuture = Future.future();
            elementProgramme.getElementProgrammeClasses(idPeriod, idMatiere, groupsStudent,
                    elementsProgEvent -> formate(elementsProgFuture, elementsProgEvent));

            // Récupération des appreciations, des moyenne finales et positionnements finales
            Future<JsonArray> appreciationMoyFinalePosFuture = Future.future();
            noteService.getAppreciationMoyFinalePositionnement(idEleve, idMatiere, null, idClasseGroups,
                    appreciationMoyPosEvent -> formate(appreciationMoyFinalePosFuture, appreciationMoyPosEvent));

            // Récupération des notes
            Future<JsonArray> notesFuture = Future.future();
            noteService.getNoteElevePeriode(null, idEtablissement, idClasseGroups, idMatiere, null,
                    notesEvent -> formate(notesFuture, notesEvent));

            // Récupération des compétences-notes
            Future<JsonArray> compNotesFuture = Future.future();
            //USAGE DU IN NORMALEMENT
            noteService.getCompetencesNotesReleve(idEtablissement, null, null, idMatiere,
                    null, idEleve, null, false, false,
                    compNotesEvent -> formate(compNotesFuture, compNotesEvent));

            // Récupération de la moyenne finale de la classe
            Future<JsonArray> moyenneFinaleFuture = Future.future();
            noteService.getColonneReleve(null, null, idMatiere, idClasseGroups, "moyenne", Boolean.FALSE,
                    moyenneFinaleEvent -> formate(moyenneFinaleFuture, moyenneFinaleEvent));

            // Récupération du tableau de conversion des compétences notes
            Future<JsonArray> tableauDeConversionFuture = Future.future();
            new DefaultCompetenceNoteService(COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE)
                    .getConversionNoteCompetence(idEtablissement, idClasse,  // note : Est ce que c'est pas l'idGroupeClasse qu'on doit passé ici ?
                            tableauEvent -> formate(tableauDeConversionFuture, tableauEvent));

            Future<Boolean> isAvgSkillFuture = structureOptionsService.isAverageSkills(idEtablissement);

            Future<String> subjectFuture = Future.future();
            subjectsFuture.add(subjectFuture);

            isAvgSkillFuture
                    .onSuccess(isAvgSkillResult -> CompositeFuture.all(elementsProgFuture, appreciationMoyFinalePosFuture, notesFuture, compNotesFuture,
                            moyenneFinaleFuture, tableauDeConversionFuture).setHandler(event -> {
                        if (event.succeeded()) {
                            List<String> idsClassWithNoteAppCompNoteStudent = new ArrayList<>();
                            setAppreciationMoyFinalePositionnementEleve(result, appreciationMoyFinalePosFuture.result(),
                                    idsClassWithNoteAppCompNoteStudent);
                            setMoyAndPosForSuivi(notesFuture.result(), compNotesFuture.result(), moyenneFinaleFuture.result(),
                                    result, idEleve, idPeriod, tableauDeConversionFuture.result(), idsClassWithNoteAppCompNoteStudent, isAvgSkillResult, services, multiTeachers);
                            setElementProgramme(result, elementsProgFuture.result(), idsClassWithNoteAppCompNoteStudent);
                            results.add(result);
                            subjectFuture.complete();
                        } else {
                            subjectFuture.fail(event.cause().getMessage());
                            log.error("[DefaultBilanPeriodique] : setSubjectLibelleAndTeachers subjectFuture " + event.cause().getMessage());
                        }
                    }))
                    .onFailure(err -> {
                        handler.handle(new Either.Left<>(err.getMessage()));
                        log.error("[DefaultBilanPeriodique] : setSubjectLibelleAndTeachers isAvgSkillFuture " + err.getMessage());
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

    private void buildSubjectForSuivi(Map<String, JsonObject> idsMatieresIdsTeachers, JsonArray idsTeachers,
                                      JsonArray subjects, final Long idPeriode,
                                      JsonArray multiTeachers, List<Service> services, JsonArray groupsStudent) {
        List<String> subjectsMissingTeachers = new ArrayList<>();

        for (int i = 0; i < subjects.size(); i++) {
            JsonObject subject = subjects.getJsonObject(i);
            final String idMatiere = subject.getString(ID_MATIERE);
            Long id_periode = subject.getLong("id_periode");
            String id_groupe = subject.getString("id_groupe");

            if (!idsMatieresIdsTeachers.containsKey(idMatiere)) {
                idsMatieresIdsTeachers.put(idMatiere, new JsonObject()
                        .put("teachers", new JsonArray())
                        .put("id_groupe", id_groupe)
                        .put("_" + COEFFICIENT, new JsonObject()));
            }

            if (idPeriode.equals(id_periode)) {
                JsonObject matiere = idsMatieresIdsTeachers.get(idMatiere);

                checkVisibilityAndAddTeachers(services, matiere, idMatiere, subject,
                        multiTeachers, idsTeachers, subjectsMissingTeachers, groupsStudent);
            }
        }

        // Ajoute les matieres manquantes pour un calcul correct de la moyenne général
        for (int i = 0; i < services.size(); i++) {
            final String idMatiere = services.get(i).getMatiere().getId();

            if (!idsMatieresIdsTeachers.containsKey(idMatiere)) {
                idsMatieresIdsTeachers.put(idMatiere, new JsonObject()
                        .put("teachers", new JsonArray())
                        .put("id_groupe", "")
                        .put("_" + COEFFICIENT, new JsonObject()));
            }
        }

        if (!subjectsMissingTeachers.isEmpty()) {
            getMissingTeachers(idsTeachers, subjectsMissingTeachers, idsMatieresIdsTeachers, services);
        }
    }

    private void checkVisibilityAndAddTeachers(List<Service> services, JsonObject matiere, final String idMatiere,
                                               JsonObject subject, JsonArray multiTeachers, JsonArray idsTeachers,
                                               List<String> subjectsMissingTeachers, JsonArray groupsStudent) {
        JsonArray teachers = matiere.getJsonArray("teachers");
        Long coefficient = isNull(subject.getLong(COEFFICIENT)) ? 1L : subject.getLong(COEFFICIENT);
        String id_groupe = subject.getString("id_groupe");
        String owner = subject.getString("owner");

        Boolean isVisible = false;
        for (int j = 0; j < services.size(); j++) {
            Service service = services.get(j);
            String serviceIdMatiere = service.getMatiere().getId();
            String serviceIdTeacher = service.getTeacher().getId();
            String serviceIdGroup = service.getGroup().getId();

            if (isNotNull(owner)) {
                if (serviceIdMatiere.equals(idMatiere) && serviceIdTeacher.equals(owner)
                        && serviceIdGroup.equals(id_groupe)) {
                    isVisible = service.isVisible();
                    coefficient = service.getCoefficient();
                    break;
                }
            } else {
                if (serviceIdMatiere.equals(idMatiere) && service.isVisible() && serviceIdGroup.equals(id_groupe)) {
                    owner = serviceIdTeacher;
                    isVisible = service.isVisible();
                    coefficient = service.getCoefficient();
                    break;
                }
            }
        }

        addMultiTeachers(multiTeachers, idMatiere, teachers, idsTeachers, groupsStudent);

        if (isVisible && !teachers.contains(owner)) {
            addTeachers(teachers, idsTeachers, owner, matiere, coefficient);
        }

        if (teachers.isEmpty() && !subjectsMissingTeachers.contains(idMatiere))
            subjectsMissingTeachers.add(idMatiere);
        else if (!teachers.isEmpty())
            subjectsMissingTeachers.remove(idMatiere);
    }

    private void getMissingTeachers(JsonArray idsTeachers, List<String> subjectsMissingTeachers,
                                    Map<String, JsonObject> idsMatieresIdsTeachers, List<Service> services) {
        subjectsMissingTeachers.forEach(idSubject -> {
            services.stream().forEach(service -> {
                String idServiceSubject = service.getMatiere().getId();

                if (idServiceSubject.equals(idSubject) && service.isVisible()) {
                    String owner = service.getTeacher().getId();
                    Long coefficient = service.getCoefficient();
                    coefficient = isNull(coefficient) ? 1L : coefficient;
                    JsonObject matiere = idsMatieresIdsTeachers.get(idSubject);
                    JsonArray teachers = matiere.getJsonArray("teachers");

                    if (!teachers.contains(owner) && teachers.isEmpty()) {
                        addTeachers(teachers, idsTeachers, owner, matiere, coefficient);
                    }
                }
            });
        });
    }

    private void addMultiTeachers(JsonArray multiTeachers, String idMatiere, JsonArray teachers, JsonArray idsTeachers,
                                  JsonArray groupsStudent) {
        multiTeachers.forEach(item -> {
            JsonObject multiTeacher = (JsonObject) item;

            String subjectId = multiTeacher.getString(Field.SUBJECT_ID);
            String coTeacherId = multiTeacher.getString(Field.SECOND_TEACHER_ID);
            String class_or_group_id = multiTeacher.getString(Field.CLASS_OR_GROUP_ID);

            if (subjectId.equals(idMatiere) && multiTeacher.getBoolean(Field.IS_VISIBLE) &&
                    groupsStudent.contains(class_or_group_id) && (multiTeacher.getString("deleted_date") == null)) {
                if (isNotNull(coTeacherId) && !teachers.contains(coTeacherId)) {
                    teachers.add(coTeacherId);
                }
                if (isNotNull(coTeacherId) && !idsTeachers.contains(coTeacherId))
                    idsTeachers.add(coTeacherId);
            }
        });
    }

    private void addTeachers(JsonArray teachers, JsonArray idsTeachers, String owner,
                             JsonObject matiere, Long coefficient) {
        teachers.add(owner);

        if (!idsTeachers.contains(owner)) {
            idsTeachers.add(owner);
        }

        JsonObject coeffObject = matiere.getJsonObject("_" + COEFFICIENT);
        if (!coeffObject.containsKey(coefficient.toString())) {
            coeffObject.put(coefficient.toString(), new JsonArray());
        }
        if (!coeffObject.getJsonArray(coefficient.toString()).contains(owner)) {
            coeffObject.getJsonArray(coefficient.toString()).add(owner);
        }
    }

    private void setElementProgramme(final JsonObject result, final JsonArray eltsProg,
                                     List<String> IdsClassWithNoteAppCompNoteStudent) {
        String elementsProg = "";
        JsonArray elementsProgByClasse = new JsonArray();

        IdsClassWithNoteAppCompNoteStudent.forEach(idClasse -> {
            elementsProgByClasse.add(new JsonObject()
                    .put("id_classe", idClasse)
                    .put("texte", ""));
        });

        if (isNotNull(eltsProg) && eltsProg.size() > 0) {
            for (int i = 0; i < eltsProg.size(); i++) {
                JsonObject element = eltsProg.getJsonObject(i);
                String texte = element.getString("texte", "");
                String idClasse = element.getString("id_classe");

                if (idClasse != null && IdsClassWithNoteAppCompNoteStudent.contains(idClasse)) {
                    if (elementsProg.isEmpty()) {
                        elementsProg = texte;
                    } else {
                        elementsProg += " " + texte;
                    }

                    JsonObject elementFounded = (JsonObject) elementsProgByClasse.stream()
                            .filter(e -> ((JsonObject) e).getString("id_classe").equals(idClasse))
                            .findFirst().orElse(null);
                    if (elementFounded != null) {
                        elementFounded.put("texte", texte);
                    }
                }
            }
        }

        result.put("elementsProgramme", elementsProg);
        result.put("elementsProgrammeByClasse", elementsProgByClasse);
    }

    private void setAppreciationMoyFinalePositionnementEleve(final JsonObject result, final JsonArray allAppMoyPosi,
                                                             List<String> idsClassWithNoteAppCompNoteStudent) {
        JsonArray appreciations = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray moyennesFinales = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray positionnements = new fr.wseduc.webutils.collections.JsonArray();

        if (allAppMoyPosi != null) {
            Map<Integer, JsonArray> mapIdPeriodeAppreciations = new HashMap<>();
            for (int i = 0; i < allAppMoyPosi.size(); i++) {
                JsonObject appMoyPosi = allAppMoyPosi.getJsonObject(i);
                String appMatPer = appMoyPosi.getString("appreciation_matiere_periode");
                Integer idPerApp = appMoyPosi.getInteger("id_periode_appreciation");
                String idClasseApp = appMoyPosi.getString("id_classe_appreciation");

                if (appMatPer != null) {
                    if (!mapIdPeriodeAppreciations.containsKey(idPerApp)) {
                        mapIdPeriodeAppreciations.put(idPerApp, new JsonArray().add(new JsonObject()
                                .put("idClasse", idClasseApp)
                                .put("appreciation", appMatPer)));
                    } else {
                        JsonArray appreciationsByIdPeriode = mapIdPeriodeAppreciations.get(idPerApp);
                        JsonObject appResponse = new JsonObject()
                                .put("idClasse", idClasseApp)
                                .put("appreciation", appMatPer);

                        if (!appreciationsByIdPeriode.contains(appResponse)) {
                            mapIdPeriodeAppreciations.put(idPerApp, appreciationsByIdPeriode.add(appResponse));
                        }
                    }

                    if (!idsClassWithNoteAppCompNoteStudent.contains(idClasseApp))
                        idsClassWithNoteAppCompNoteStudent.add(idClasseApp);
                }

                //on récupère la moyenne finale de l'élève pour sa classe principale = idClasse passé en paramètre
                // dans le contexte d'un matiere on est sensé n'avoir qu'une moyenne finale qui est soit sur un groupe soit sur une classe
                JsonObject moyenne_finale = new JsonObject();
                if (isNotNull(appMoyPosi.getValue("moyenne_finale")) && isNotNull(appMoyPosi.getValue("id_periode_moyenne_finale"))) {
                    moyenne_finale.put("id_periode", appMoyPosi.getInteger("id_periode_moyenne_finale"))
                            .put("moyenneFinale", Double.valueOf(appMoyPosi.getString("moyenne_finale")));
                    if (!idsClassWithNoteAppCompNoteStudent.contains(appMoyPosi.getString("id_classe_moyfinale")))
                        idsClassWithNoteAppCompNoteStudent.add(appMoyPosi.getString("id_classe_moyfinale"));
                } else if (isNotNull(appMoyPosi.getValue("id_periode_moyenne_finale"))) {
                    moyenne_finale.put("id_periode", appMoyPosi.getInteger("id_periode_moyenne_finale"))
                            .put("moyenneFinale", "NN");
                }

                if (!moyennesFinales.contains(moyenne_finale)) {
                    moyennesFinales.add(moyenne_finale);
                }

                //Pour le positionnement on ne peut en avoir qu'un par matière
                //le positionnement n'est pas enregistré par classe
                if (appMoyPosi.getInteger("positionnement_final") != null) {
                    JsonObject positionnement = new JsonObject()
                            .put("id_periode", appMoyPosi.getInteger("id_periode_positionnement"))
                            .put("positionnementFinal", appMoyPosi.getInteger("positionnement_final"));

                    if (!positionnements.contains(positionnement)) {
                        positionnements.add(positionnement);
                    }
                }
            }

            if (!mapIdPeriodeAppreciations.isEmpty()) {
                for (Map.Entry<Integer, JsonArray> idPeriodeApp : mapIdPeriodeAppreciations.entrySet()) {
                    appreciations.add(new JsonObject()
                            .put("id_periode", idPeriodeApp.getKey())
                            .put("appreciationByClasse", idPeriodeApp.getValue()));
                }
            }
        }

        result.put("appreciations", appreciations);
        result.put("positionnementsFinaux", positionnements);
        result.put("moyennesFinales", moyennesFinales);
    }

    private void setMoyAndPosForSuivi(JsonArray notes, JsonArray compNotes, JsonArray moyFinalesEleves,
                                      JsonObject result, String idEleve, Long idPeriodAsked,
                                      JsonArray tableauConversion, List<String> idsClassWithNoteAppCompNoteStudent, boolean isAvgSkill,
                                      List<Service> services, JsonArray multiTeachers) {
        JsonArray idsEleves = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse =
                noteService.calculMoyennesEleveByPeriode(notes, result, idEleve, idsEleves,
                        idsClassWithNoteAppCompNoteStudent, idPeriodAsked, services, multiTeachers);
        noteService.getMoyennesMatieresByCoefficient(moyFinalesEleves, notes, result, idEleve, idsEleves, services, multiTeachers);

        noteService.calculPositionnementAutoByEleveByMatiere(compNotes, result, false, tableauConversion,
                idsClassWithNoteAppCompNoteStudent, idPeriodAsked, isAvgSkill);
        noteService.calculAndSetMoyenneClasseByPeriode(moyFinalesEleves, notesByDevoirByPeriodeClasse, result);
        noteService.setRankAndMinMaxInClasseByPeriode(idPeriodAsked, idEleve, notesByDevoirByPeriodeClasse,
                moyFinalesEleves, result);
    }

    public void getBilanPeriodiqueDomaineForGraph(final String idEleve, String idEtablissement,
                                                  final String idClasse, final Integer typeClasse, final String idPeriodeString,
                                                  final Handler<Either<String, JsonArray>> handler) {
        Utils.getGroupsEleve(eb, idEleve, idEtablissement, responseQuerry -> {
            if (!responseQuerry.isRight()) {
                String error = responseQuerry.left().getValue();
                log.error(error);
                handler.handle(new Either.Left<>(error));
            } else {
                JsonArray idGroups = responseQuerry.right().getValue();
                //idGroups null si l'eleve n'est pas dans un groupe
                new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE, eb)
                        .getDataGraphDomaine(idEleve, idGroups, idEtablissement, idClasse,
                                typeClasse, idPeriodeString, isNull(idPeriodeString), handler);
            }
        });
    }
}
