package fr.openent.competences.service.impl;

import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.constants.Field;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.helpers.UtilsHelper;
import fr.openent.competences.model.*;
import fr.openent.competences.service.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.storage.Storage;

import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.HAS_NOTE;
import static fr.openent.competences.Competences.MOYENNE;
import static fr.openent.competences.service.impl.DefaultExportService.COEFFICIENT;
import static fr.openent.competences.service.impl.DefaultUtilsService.setServices;

public class DefaultClassAppreciation extends SqlCrudService implements ClassAppreciationService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultCompetencesService.class);
    private final EventBus eb;
    private UtilsService utilsService;
    private ExportBulletinService exportBulletinService;
    private AppreciationService appreciationService;
    private NoteService noteService;
    private Storage storage;

    public DefaultClassAppreciation (String schema, String table, EventBus eb, Storage storage) {
        super(schema, table);
        this.eb = eb;
        this.exportBulletinService = new DefaultExportBulletinService(eb, storage);
        utilsService = new DefaultUtilsService(eb);
        appreciationService = new DefaultAppreciationService(Field.SCHEMA_COMPETENCES, Field.APPRECIATIONS_TABLE);
        noteService = new DefaultNoteService(Field.SCHEMA_COMPETENCES, Field.NOTES_TABLE);

    }

    public Future<JsonObject> getTeacherAppreciationPDF(String idClasse, Integer finalIdPeriode, String language ,
                                                         String host, String idEtablissement, HttpServerRequest request){
        Promise<JsonObject> promiseGetTeacherAllPDF = Promise.promise();
        Set<JsonObject> MatGrp = new HashSet<>();
        Map<JsonObject, String> teachers = new HashMap<>();
        Map<String, ArrayList<String>> coTeachers = new HashMap<>();
        Set<String> idGroups = new HashSet<>(Collections.singleton(idClasse));
        JsonArray servicesJSON = new JsonArray();
        JsonArray multiTeachers = new JsonArray();
        List<Service> services = new ArrayList<>();
        List<MultiTeaching> multiTeachings = new ArrayList<>();
        List<SubTopic> subTopics = new ArrayList<>();
        Map<JsonObject, String> appr = new HashMap<>();
        Map<JsonObject, Map<String, List<NoteDevoir>>> notes = new HashMap<>();
        Map<JsonObject, Map<String, Map<Long, List<NoteDevoir>>>> notesBySousMatiere = new HashMap<>();
        Map<JsonObject, Map<String, NoteDevoir>> moyennesFinales = new HashMap<>();
        Map<String, String> libTeachers = new HashMap<>();
        Map<String, JsonObject> libMatieres = new HashMap<>();
        Map<String, String> libGrp = new HashMap<>();
        Map<JsonObject, JsonObject> moyObjects = new HashMap<>();
        JsonObject result = new JsonObject();

        Future<JsonArray> idGroupesFuture = Utils.getGroupesClasse(eb, new JsonArray().add(idClasse));
        Future<List<String>> idElevesFuture = Utils.getIdElevesClassesGroupes(eb, idClasse, finalIdPeriode, 0);

        CompositeFuture.all(idElevesFuture, idGroupesFuture)
               .compose(compose1 -> {

                   idGroupesFuture.result().stream().forEach(line -> {
                               idGroups.add(((JsonObject) line).getString(Field.ID_CLASSE));
                               ((JsonObject) line).getJsonArray(Field.ID_GROUPES).getList().forEach(idGroup -> idGroups.add((String) idGroup));
                           });
                   Future<JsonArray> multiTeacherFuture = utilsService.getAllMultiTeachers(idEtablissement,
                           new JsonArray(Arrays.asList(idGroups.toArray())));
                   Future<JsonArray> servicesFuture = utilsService.getServices(idEtablissement,
                           new JsonArray(Arrays.asList(idGroups.toArray())));
                   Future<List<SubTopic>> subTopicCoefFuture = utilsService.getSubTopicCoeff(idEtablissement,
                           new JsonArray(Arrays.asList(idGroups.toArray())));
                   return UtilsHelper.completeVariablesForServices(servicesFuture, multiTeacherFuture,
                           subTopicCoefFuture,servicesJSON, multiTeachers, subTopics);

               })
                .compose(compose2 -> {
                    Structure structure = new Structure();
                    structure.setId(idEtablissement);
                    exportBulletinService.setMultiTeaching(structure, multiTeachers, multiTeachings, idClasse);
                    setServices(structure, servicesJSON, services, subTopics);

                    Future<Map<JsonObject, String>> apprFuture = setClassAppreciation (MatGrp, appr,
                            idGroups.toArray(new String[0]), finalIdPeriode );

                    Future<Void> listFuture = setMapNotesTeachersAndSubject (MatGrp, teachers, servicesJSON,
                            multiTeachers, services, notes, notesBySousMatiere, idElevesFuture, idGroups, finalIdPeriode);

                    Future<Map<JsonObject, Map<String, NoteDevoir>>> averageFinalFuture = setFinaleAverage (
                             MatGrp,  moyennesFinales,
                             idElevesFuture,  finalIdPeriode,  idGroups);

                    multiTeachers.stream().forEach(mulT -> {
                        JsonObject multiTeacher = (JsonObject) mulT;
                        String key = multiTeacher.getString(Field.SUBJECT_ID);
                        if (!coTeachers.containsKey(key)) {
                            ArrayList _coTeachers = new ArrayList();
                            _coTeachers.add(multiTeacher.getString(Field.SECOND_TEACHER_ID));
                            coTeachers.put(key, _coTeachers);
                        } else {
                            ArrayList _coTeachers = coTeachers.get(key);
                            String second_teacher_id = multiTeacher.getString(Field.SECOND_TEACHER_ID);
                            if (!_coTeachers.contains(second_teacher_id)) {
                                _coTeachers.add(second_teacher_id);
                                coTeachers.put(key, _coTeachers);
                            }
                        }
                    });
                    return CompositeFuture.all(apprFuture, listFuture, averageFinalFuture);
                })
                .compose(eventCompose3 -> {
                   setMapObject(MatGrp, appr, notes, notesBySousMatiere, moyennesFinales, idElevesFuture, moyObjects);

                    ArrayList<String> idTeachers = new ArrayList(teachers.values());
                    coTeachers.values().forEach(item -> {
                        idTeachers.addAll(item);
                    });
                    Promise<Map<String,String>> mapPromise = Promise.promise();
                    mapPromise.complete(new HashMap());
                    Future<Map<String,String>> libTeachfuture =
                            (teachers.values().size() == 0 && coTeachers.values().size() == 0) ?
                                    mapPromise.future():
                                    getNameTeacher(idTeachers, libTeachers) ;

                    Future<String> libellePeriodeFuture = setWordingPeriod(finalIdPeriode, language, host, result);
                    Future<Map<String, JsonObject>> libMatFuture = setWordingSubject(libMatieres, MatGrp);
                    Future<String> libelleClasseFuture = setInfoGroupe(idClasse, result);
                    Future<Map<String,String>> libGrpFuture = setMapInfoClass(MatGrp, libGrp);
                    return CompositeFuture.all(libMatFuture, libTeachfuture, libGrpFuture,
                            libellePeriodeFuture, libelleClasseFuture);
                })
                .onSuccess(lastCompose -> {
                    JsonArray data = setData(teachers, coTeachers, libTeachers, libGrp, libMatieres, moyObjects);
                    result.put(Field.DATA, Utils.sortJsonArrayIntValue(Field.RANK, data));
                        promiseGetTeacherAllPDF.complete(result);
                    })
                .onFailure( failLastCompose -> {
                    log.error(failLastCompose.getMessage());
                    promiseGetTeacherAllPDF.fail(failLastCompose.getMessage());
                });

       return promiseGetTeacherAllPDF.future();
    }

    private void setMapObject (Set<JsonObject> MatGrp, Map<JsonObject, String> appr,
                               Map<JsonObject, Map<String, List<NoteDevoir>>> notes,
                               Map<JsonObject, Map<String, Map<Long, List<NoteDevoir>>>> notesBySousMatiere,
                               Map<JsonObject, Map<String, NoteDevoir>> moyennesFinales,
                               Future<List<String>> idElevesFuture, Map<JsonObject,JsonObject> moyObjects) {

        MatGrp.stream().forEach(matGrp -> {
            List<NoteDevoir> matGrpNotes = new ArrayList<>();
            JsonObject moyObject = new JsonObject();
            idElevesFuture.result().stream().forEach(idEleve -> {
                if (moyennesFinales.containsKey(matGrp) && moyennesFinales.get(matGrp).containsKey(idEleve)
                        && moyennesFinales.get(matGrp).get(idEleve).getNote() != null) {
                    matGrpNotes.add(moyennesFinales.get(matGrp).get(idEleve));
                } else {
                    if (notesBySousMatiere.containsKey(matGrp) && notesBySousMatiere.get(matGrp).containsKey(idEleve)) {
                        if (!(moyennesFinales.containsKey(matGrp) && moyennesFinales.get(matGrp).containsKey(idEleve)
                                && moyennesFinales.get(matGrp).get(idEleve).getNote() == null)) {
                            double total = 0;
                            double totalCoeff = 0;
                            Boolean statistiques = false;
                            Boolean annual = false;
                            for (Map.Entry<Long, List<NoteDevoir>> sousMatMapEntry : notesBySousMatiere.get(matGrp).get(idEleve).entrySet()) {
                                Long idSousMat = sousMatMapEntry.getKey();
                                Service serv = sousMatMapEntry.getValue().get(0).getService();
                                double coeff = 1.d;

                                if (serv != null && serv.getSubtopics() != null && serv.getSubtopics().size() > 0) {
                                    SubTopic subTopic = serv.getSubtopics().stream()
                                            .filter(el ->
                                                    el.getId().equals(idSousMat)
                                            ).findFirst().orElse(null);
                                    if (subTopic != null)
                                        coeff = subTopic.getCoefficient();
                                }

                                Double moyenSousMat = (utilsService.calculMoyenne(sousMatMapEntry.getValue(),
                                        statistiques, Field.DIVISEUR_NOTE, annual)).getValue(Field.MOYENNE).equals(Field.NN) ? null :
                                        utilsService.calculMoyenne(sousMatMapEntry.getValue(),
                                                statistiques, Field.DIVISEUR_NOTE, annual).getDouble(Field.MOYENNE);
                                if(moyenSousMat != null) {
                                    total += coeff * moyenSousMat;
                                    totalCoeff += coeff;
                                }
                            }
                            if (totalCoeff == 0) {
                                log.error("Found a 0 or negative coefficient in getExportRecapAppreciations, please check your subtopics " +
                                        "coefficients (value of totalCoeff : " + totalCoeff + ")");
                            } else {
                                Double moy = Math.round((total / totalCoeff) * Field.ROUNDER) / Field.ROUNDER;
                                matGrpNotes.add(new NoteDevoir(moy, false, 1.0));
                            }
                        }
                    } else {
                        if (notes.containsKey(matGrp) && notes.get(matGrp).containsKey(idEleve) &&
                                !(moyennesFinales.containsKey(matGrp) && moyennesFinales.get(matGrp).containsKey(idEleve)
                                        && moyennesFinales.get(matGrp).get(idEleve).getNote() == null)) {
                            if (!Field.NN.equals(utilsService.calculMoyenne(notes.get(matGrp).get(idEleve),
                                    false, null, false).getValue(MOYENNE))) {
                                matGrpNotes.add(new NoteDevoir(utilsService.calculMoyenne(notes.get(matGrp).get(idEleve),
                                        false, null, false).getDouble(MOYENNE), false,
                                        new Double(1)));
                            }
                        }
                    }
                }
            });
            JsonObject resultCalc = utilsService.calculMoyenne(matGrpNotes, true, null, false);
            if (!resultCalc.getBoolean(HAS_NOTE)) {
                moyObject.put(Field.MIN, "");
                moyObject.put(Field.MAX, "");
                moyObject.put(Field.MOY, Field.NN);
            } else if (resultCalc.getDouble(Field.NOTEMIN) > resultCalc.getDouble(MOYENNE)) {

                moyObject.put(Field.MIN, "");
                moyObject.put(Field.MAX, "");
                moyObject.put(Field.MOY, "");
            } else {
                moyObject.put(Field.MIN, resultCalc.getDouble(Field.NOTEMIN));
                moyObject.put(Field.MAX, resultCalc.getDouble(Field.NOTEMAX));
                moyObject.put(Field.MOY, resultCalc.getDouble(MOYENNE));
            }

            moyObject.put(Field.APPR, appr.get(matGrp));
            moyObjects.put(matGrp, moyObject);
        });
    }

    private Future<Map<String, String>> getMapInfoClass (Set<JsonObject> MatGrp) {
        Promise<Map<String, String>> libGrpPromise = Promise.promise();

        Utils.getInfosGroupes(eb, new JsonArray(MatGrp.stream().map(matGrp ->
                        matGrp.getString(Field.ID_GROUPE)).collect(Collectors.toList())),
                FutureHelper.handler(libGrpPromise,
                        String.format("[Competences@%s::getTeacherAppreciationPDF] error to getInfosGroupes groups: ",
                                getClass().getSimpleName())));

        return libGrpPromise.future();
    }
    private Future<Map<String, String>> setMapInfoClass (Set<JsonObject> MatGrp, Map<String, String> libGrp) {
        Promise<Map<String, String>> mapPromise = Promise.promise();
        getMapInfoClass(MatGrp).onSuccess(
                mapInfoclass -> {
                    libGrp.putAll(mapInfoclass);
                    mapPromise.complete(mapInfoclass);
                }
        ).onFailure(mapPromise::fail);
        return mapPromise.future();
    }


    private Future<String> setInfoGroupe (String idClasse, JsonObject result) {
        Promise<String> libelleClassePromise = Promise.promise();
        Utils.getInfosGroupes(eb, new JsonArray().add(idClasse),
                stringMapEither -> {
            if (stringMapEither.isRight()) {
                String libelleClasse = stringMapEither.right().getValue().get(idClasse);
                result.put(Field.CLASSE, libelleClasse);
                libelleClassePromise.complete(libelleClasse);
            } else {
                libelleClassePromise.fail(stringMapEither.left().getValue());
                log.error(String.format("[Competences@%s::getTeacherAppreciationPDF] error to getInfosGroupes Class: %s",
                        getClass().getSimpleName(), stringMapEither.left().getValue()));
            }
        });
        return libelleClassePromise.future();
    }

    private Future<Map<String, JsonObject>> setWordingSubject(Map<String, JsonObject> libMatieres, Set<JsonObject> MatGrp) {

        Promise<Map<String, JsonObject>> subjectPromise = Promise.promise();
        getWordingSubject (MatGrp).onSuccess(
                subjects -> {
                    libMatieres.putAll(subjects);
                    subjectPromise.complete(libMatieres);
                }
        ).onFailure(subjectPromise::fail);
        return subjectPromise.future();
    }


    private Future<Map<String, JsonObject>> getWordingSubject (Set<JsonObject> MatGrp ) {
        Promise<Map<String, JsonObject>> matPromise = Promise.promise();
        if (MatGrp.size() == 0) {
            matPromise.complete(new HashMap<>());
        } else {
            Utils.getLibelleMatiere(eb, new JsonArray(MatGrp.stream().map(matGrp ->
                            matGrp.getString(Field.ID_MATIERE)).collect(Collectors.toList())),
                    FutureHelper.handler(matPromise,
                            String.format("[Competences@%s::getTeacherAppreciationPDF] error to getLibelleMatiere : ",
                                    getClass().getSimpleName())));
        }
        return matPromise.future();
    }

    private Future<String> setWordingPeriod (Integer finalIdPeriode, String language, String host, JsonObject result) {
        Promise<String> libellePeriodePromise = Promise.promise();
        Utils.getPeriodLibelle(eb, language, host, finalIdPeriode)
        .onSuccess( responseLibellePeriode -> {
            result.put(Field.PERIODE, responseLibellePeriode);
            libellePeriodePromise.complete();
        })
        .onFailure( periodFail -> {
                    libellePeriodePromise.fail(periodFail.getMessage());
            log.error(String.format("[Competences@%s::getTeacherAppreciationPDF] error to getPeriodLibelle : %s",
                    getClass().getSimpleName(), periodFail.getMessage()));
                });
        return libellePeriodePromise.future();
    }

    private Future<Map<String, String>> getNameTeacher (ArrayList<String> idTeachers, Map<String, String> libTeachers) {

        Promise<Map<String, String>> libTeachPromise = Promise.promise();
        Utils.getLastNameFirstNameUser(eb, new JsonArray(idTeachers))
                .onSuccess(responseTeacher -> {
                    libTeachers.putAll(responseTeacher.entrySet()
                            .stream()
                            .collect(Collectors.toMap(val -> val.getKey(), val ->
                                    val.getValue().getString(Field.FIRSTNAME) + " " + val.getValue().getString(Field.NAME))));
                    libTeachPromise.complete(libTeachers);
                })
                .onFailure(errorTeacher -> {
                    log.error(String.format("[Competences@%s::getTeacherAppreciationPDF] error to getLastNameFirstNameUser : %s",
                            getClass().getSimpleName(), errorTeacher.getMessage()));
                    libTeachPromise.fail(new Throwable(errorTeacher.getMessage()));
                });
        return libTeachPromise.future();
    }

    private JsonArray setData (Map<JsonObject, String> teachers, Map<String, ArrayList<String>> coTeachers,
                               Map<String, String> libTeachers, Map<String, String> libGrp,
                               Map<String, JsonObject> libMatieres, Map<JsonObject, JsonObject> moyObject) {
        JsonArray data = new JsonArray(
                moyObject.entrySet().stream().map(entry -> {
                    JsonObject newMoy = new JsonObject();
                    if (libMatieres.get(entry.getKey().getString(Field.ID_MATIERE)) == null) {
                        return newMoy;
                    }
                    String prof = libTeachers.get(teachers.get(entry.getKey()));
                    newMoy.put(Field.MAT, libMatieres.get(entry.getKey().getString(Field.ID_MATIERE)).getString(Field.NAME));
                    newMoy.put(Field.RANK, libMatieres.get(entry.getKey().getString(Field.ID_MATIERE)).getInteger(Field.RANK));
                    newMoy.put(Field.PROF, prof);
                    newMoy.put(Field.GRP, libGrp.get(entry.getKey().getString(Field.ID_GROUPE)));

                    if (coTeachers.size() > 0 && coTeachers.get(entry.getKey().getString(Field.ID_MATIERE)) != null) {
                        ArrayList _coTeachers = new ArrayList();
                        coTeachers.get(entry.getKey().getString(Field.ID_MATIERE)).forEach(coTeacher -> {
                            String coTeacherName = libTeachers.get(coTeacher);
                            if (!_coTeachers.contains(coTeacherName)) {
                                if (prof != null) {
                                    if (!prof.equals(coTeacherName)) {
                                        _coTeachers.add(coTeacherName);
                                    }
                                } else {
                                    _coTeachers.add(coTeacherName);
                                }
                            }
                        });
                        newMoy.put(Field.COT, _coTeachers);
                    }

                    newMoy.mergeIn(entry.getValue());

                    return newMoy;
                }).collect(Collectors.toList()));
        return data;
    }

    private Future<Map<JsonObject, Map<String, NoteDevoir>>> setFinaleAverage (Set<JsonObject> MatGrp, Map<JsonObject,
            Map<String, NoteDevoir>> moyennesFinales,
                Future<List<String>> idElevesFuture, Integer finalIdPeriode, Set<String> idGroups) {

        Promise<Map<JsonObject, Map<String, NoteDevoir>>> moyennesFinalPromise = Promise.promise();
        noteService.getFinalAverage(idElevesFuture.result().toArray(new String[0]), finalIdPeriode,
                        null, idGroups.toArray(new String[0]))
                .onSuccess( responseFinalAverage -> {
                    responseFinalAverage.stream().forEach(line -> {
                        JsonObject lineObject = (JsonObject) line;

                        JsonObject key = new JsonObject()
                                .put(Field.ID_GROUPE, lineObject.getString(Field.ID_CLASSE))
                                .put(Field.ID_MATIERE, lineObject.getString(Field.ID_MATIERE));

                        MatGrp.add(key);

                        if (!moyennesFinales.containsKey(key)) {
                            moyennesFinales.put(key, new HashMap<>());
                        }
                        if (lineObject.getValue(MOYENNE) != null)
                            moyennesFinales.get(key).put(lineObject.getString(Field.ID_ELEVE),
                                    new NoteDevoir(Double.parseDouble(lineObject.getString(MOYENNE)), false,
                                            new Double(1)));
                        else // cas moyenne finale NN
                            moyennesFinales.get(key).put(lineObject.getString(Field.ID_ELEVE),
                                    new NoteDevoir(null, false, new Double(1)));
                    });
                    moyennesFinalPromise.complete(moyennesFinales);
                })
                .onFailure( moyFail -> {
                    moyennesFinalPromise.fail(moyFail.getMessage());
                    log.error(String.format("[Competences@%s::getTeacherAppreciationPDF] error to getFinalAverage : %s",
                            getClass().getSimpleName(), moyFail.getMessage()));
                });
        return moyennesFinalPromise.future();
    }

    private Future<Void> setMapNotesTeachersAndSubject (Set<JsonObject> MatGrp, Map<JsonObject, String> teachers,
                                                JsonArray servicesJSON, JsonArray multiTeachers,
                                                List<Service> services, Map<JsonObject, Map<String, List<NoteDevoir>>> notes,
                                                Map<JsonObject, Map<String, Map<Long, List<NoteDevoir>>>> notesBySousMatiere,
                                                Future<List<String>> idElevesFuture,
                                                Set<String> idGroups, Integer finalIdPeriode ) {
        Promise<Void> setMApPromise = Promise.promise();
        noteService.getAssessmentScores(idElevesFuture.result(), new ArrayList<>(idGroups), finalIdPeriode)
                .onSuccess(
                        responseScoresStudents -> {

                            responseScoresStudents.stream().forEach(line -> {
                                JsonObject lineObject = (JsonObject) line;

                                JsonObject key = new JsonObject();
                                key.put(Field.ID_MATIERE, lineObject.getString(Field.ID_MATIERE));
                                key.put(Field.ID_GROUPE, lineObject.getString(Field.ID_GROUPE));

                                MatGrp.add(key);
                                Boolean isVisible = true;
                                for (int i = 0; i < servicesJSON.size(); i++) {
                                    JsonObject service = servicesJSON.getJsonObject(i);

                                    String serviceIdMatiere = service.getString(Field.ID_MATIERE);
                                    String lineIdMatiere = lineObject.getString(Field.ID_MATIERE);
                                    if (serviceIdMatiere.equals(lineIdMatiere)) {
                                        isVisible = service.getBoolean(Field.IS_VISIBLE);
                                        break;
                                    }
                                }
                                if (!teachers.containsKey(key) && isVisible) {
                                    teachers.put(key, lineObject.getString(Field.OWNER));
                                }

                                Matiere matiere = new Matiere(lineObject.getString(Field.ID_MATIERE));
                                Teacher teacher = new Teacher(lineObject.getString(Field.OWNER));
                                Group group = new Group(lineObject.getString(Field.ID_GROUPE));

                                Service service = services.stream()
                                        .filter(el -> teacher.getId().equals(el.getTeacher().getId())
                                                && matiere.getId().equals(el.getMatiere().getId())
                                                && group.getId().equals(el.getGroup().getId()))
                                        .findFirst().orElse(null);

                                if (service == null) {
                                    //On regarde les multiTeacher
                                    for (Object mutliTeachO : multiTeachers) {
                                        JsonObject multiTeaching = (JsonObject) mutliTeachO;
                                        if (multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(teacher.getId())
                                                && multiTeaching.getString(Field.CLASS_OR_GROUP_ID).equals(group.getId())
                                                && multiTeaching.getString(Field.SUBJECT_ID).equals(matiere.getId())) {
                                            service = services.stream()
                                                    .filter(el -> el.getTeacher().getId().equals(multiTeaching.getString(Field.SECOND_TEACHER_ID))
                                                            && matiere.getId().equals(el.getMatiere().getId())
                                                            && group.getId().equals(el.getGroup().getId()))
                                                    .findFirst().orElse(null);
                                        }

                                        if (multiTeaching.getString(Field.SECOND_TEACHER_ID).equals(teacher.getId())
                                                && multiTeaching.getString(Field.CLASS_OR_GROUP_ID).equals(group.getId())
                                                && multiTeaching.getString(Field.SUBJECT_ID).equals(matiere.getId())) {

                                            service = services.stream()
                                                    .filter(el -> multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(el.getTeacher().getId())
                                                            && matiere.getId().equals(el.getMatiere().getId())
                                                            && group.getId().equals(el.getGroup().getId()))
                                                    .findFirst().orElse(null);
                                        }
                                    }
                                }

                                Long sousMatiereId = lineObject.getLong(Field.ID_SOUSMATIERE);
                                Long id_periode = lineObject.getLong(Field.ID_PERIODE);

                                NoteDevoir note = new NoteDevoir(Double.parseDouble(lineObject.getString(Field.VALEUR)),
                                        Double.valueOf(lineObject.getString(Field.DIVISEUR)),
                                        lineObject.getBoolean(Field.RAMENER_SUR),
                                        Double.parseDouble(lineObject.getString(COEFFICIENT)),
                                        lineObject.getString(Field.ID_ELEVE), id_periode, service, sousMatiereId);

                                if (sousMatiereId != null) {
                                    if (!notesBySousMatiere.containsKey(key)) {
                                        notesBySousMatiere.put(key, new HashMap<>());
                                    }

                                    if (!notesBySousMatiere.get(key).containsKey(lineObject.getString(Field.ID_ELEVE))) {
                                        notesBySousMatiere.get(key).put(lineObject.getString(Field.ID_ELEVE), new HashMap<>());
                                    }

                                    if (!notesBySousMatiere.get(key).get(lineObject.getString(Field.ID_ELEVE)).containsKey(sousMatiereId)) {
                                        notesBySousMatiere.get(key).get(lineObject.getString(Field.ID_ELEVE)).put(sousMatiereId, new ArrayList<>());
                                    }

                                    notesBySousMatiere.get(key).get(lineObject.getString(Field.ID_ELEVE)).get(sousMatiereId).add(note);
                                } else {
                                    if (!notes.containsKey(key)) {
                                        notes.put(key, new HashMap<>());
                                    }

                                    if (!notes.get(key).containsKey(lineObject.getString(Field.ID_ELEVE))) {
                                        notes.get(key).put(lineObject.getString(Field.ID_ELEVE), new ArrayList<>());
                                    }
                                    notes.get(key).get(lineObject.getString(Field.ID_ELEVE)).add(note);
                                }
                            });
                            setMApPromise.complete();
                        })
                .onFailure(err -> {
                    setMApPromise.fail(err.getMessage());
                    log.error(String.format("[Competences@%s::getTeacherAppreciationPDF] error to getAssessmentScores : %s",
                            getClass().getSimpleName(), err.getMessage()));
                });
        return setMApPromise.future();
    }

    private Future<Map<JsonObject, String>> setClassAppreciation (Set<JsonObject> MatGrp, Map<JsonObject, String> appr,
                                      String[] classIds, Integer finalIdPeriode) {

        Promise<Map<JsonObject, String>> apprPromise = Promise.promise();
        appreciationService.getAppreciationClass(classIds, finalIdPeriode, null)
                .onSuccess(
                        resultAppr -> {
                            resultAppr.stream().forEach(line -> {
                                JsonObject lineObject = (JsonObject) line;

                                JsonObject key = new JsonObject();
                                key.put(Field.ID_MATIERE, lineObject.getString(Field.ID_MATIERE));
                                key.put(Field.ID_GROUPE, lineObject.getString(Field.ID_CLASSE));

                                MatGrp.add(key);
                                appr.put(key, lineObject.getString(Field.APPRECIATION));
                            });
                            apprPromise.complete(appr);
                        })
                .onFailure(apprFail -> {
                    apprPromise.fail(apprFail.getMessage());
                    log.error(String.format("[Competences@%s::getTeacherAppreciationPDF] error to getAppreciationClass : %s",
                            getClass().getSimpleName(), apprFail.getMessage()));
                });
        return apprPromise.future();
    }
}
