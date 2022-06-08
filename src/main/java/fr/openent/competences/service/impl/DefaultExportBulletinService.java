package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.ImgLevel;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.enums.TypePDF;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.model.*;
import fr.openent.competences.service.*;
import fr.openent.competences.utils.BulletinUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.*;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.openent.competences.helpers.NodePdfGeneratorClientHelper.*;
import static fr.openent.competences.service.impl.BulletinWorker.SAVE_BULLETIN;
import static fr.openent.competences.service.impl.DefaultExportService.COEFFICIENT;
import static fr.openent.competences.service.impl.DefaultNoteService.*;
import static fr.openent.competences.utils.ArchiveUtils.getFileNameForStudent;
import static fr.openent.competences.utils.BulletinUtils.getIdParentForStudent;
import static fr.openent.competences.utils.HomeworkUtils.safeGetDouble;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultExportBulletinService implements ExportBulletinService{

    // Constantes statiques
    private static final Logger log = LoggerFactory.getLogger(DefaultExportBulletinService.class);
    private static final int MAX_SIZE_LIBELLE = 300;
    private static final int MAX_SIZE_LIBELLE_PROJECT = 600;
    private static final int MAX_SIZE_APPRECIATION_CPE = 600;
    private static final int MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE = 600;
    private static final String GET_ANNEE_SCOLAIRE_METHOD = "getAnneeScolaire";
    private static final String GET_EVENEMENT_METHOD = "getEvenements";
    private static final String PUT_LIBELLE_FOR_EXPORT_METHOD = "putLibelleForExport";
    private static final String GET_RESPONSABLE_METHOD = "getResponsables";
    private static final String GET_SUIVI_ACQUIS_METHOD = "getSuiviAcquis";
    private static final String GET_PROJECTS_METHOD = "getProjets";
    private static final String GET_SYNTHESE_BILAN_PERIO_METHOD = "getSyntheseBilanPeriodique";
    private static final String GET_STRUCTURE_METHOD = "getStructure";
    private static final String GET_HEAD_TEACHERS_METHOD = "getHeadTeachers";
    private static final String GET_CYCLE_METHOD = "getCycle";
    private static final String GET_LIBELLE_PERIOD_METHOD = "getLibellePeriode";
    private static final String GET_APPRECIATION_CPE_METHOD = "getAppreciationCPE";
    private static final String EXPORT_BULLETIN_METHOD = "export Bulletin";
    private static final String GET_AVIS_CONSEIL_METHOD = "getAvisConseil";
    private static final String GET_AVIS_ORIENTATION_METHOD = "getAvisOrientation";
    private static final String GET_IMAGE_GRAPH_METHOD = "getImageGraph";
    private static final String GET_ARBRE_DOMAINE_METHOD = "getArbreDomaines";
    private static final String GET_DATA_FOR_GRAPH_DOMAINE_METHOD = "getBilanPeriodiqueDomaineForGraph";
    private static final String PRINT_SOUS_MATIERES = "printSousMatieres";
    private static final String PRINT_COEFFICIENT = "printCoefficient";
    private static final String PRINT_MOYENNE_GENERALE = "printMoyenneGenerale";
    private static final String PRINT_MOYENNE_ANNUELLE = "printMoyenneAnnuelle";
    private static final String BACKGROUND_COLOR = "backgroundColor";
    // Keys Utils
    private static final String APPRECIATION_KEY = "appreciation";
    private static final String PRINT_MATIERE_KEY = "printMatiere";
    public static final String ID = "id";
    private static final String ID_PARENT = "id_parent";
    private static final String ID_PERIODE ="id_periode";
    private static final String ID_CLASSE = "idClasse";
    private static final String ID_ELEVE = "id_eleve";
    public static final String ID_ETABLISSEMENT = "id_etablissement";
    private static final String AGRICULTURE_LOGO = "agricultureLogo";
    private static final String LOGO_PATH = "pathLogoImg";
    private static final String HIDE_HEADTEACHER = "hideHeadTeacher";
    private static final String ADD_OTHER_TEACHER = "addOtherTeacher";
    private static final String FUNCTION_OTHER_TEACHER = "functionOtherTeacher";
    private static final String OTHER_TEACHER_NAME = "otherTeacherName";
    private static final String GET_RESPONSABLE = "getResponsable";
    private static final String MOYENNE = "moyenne";
    private static final String MOYENNE_CLASSE = "moyenneClasse";
    private static final String MOYENNE_ELEVE = "moyenneEleve";
    private static final String MOYENNE_GENERALE = "moyenneGenerale";
    private static final String MOYENNE_ANNUELLE = "moyenneAnnuelle";
    private static final String STUDENT_RANK = "studentRank";
    private static final String CLASS_AVERAGE_MINMAX = "classAverageMinMax";
    private static final String POSITIONNEMENT = "positionnement";
    private static final String MOYENNE_CLASSE_SOUS_MAT = "moyenneClasseSousMat";
    private static final String MOYENNE_ELEVE_SOUS_MAT = "moyenneEleveSousMat";
    private static final String POSITIONNEMENT_SOUS_MAT = "positionnementSousMat";
    private static final String ELEMENTS_PROGRAMME = "elementsProgramme";
    private static final String EXTERNAL_ID_KEY = "externalId";
    private static final String NIVEAU_COMPETENCE = "niveauCompetences";
    private static final String EVALUATED = "evaluated";
    public static final String ACTION = "action";
    private static final String NAME = Competences.NAME;
    private static final String CLASSE_NAME = CLASSE_NAME_KEY;
    public static final String DISPLAY_NAME = "displayName";
    private static final String ADDRESSE_POSTALE = "addressePostale";
    private static final String GRAPH_PER_DOMAINE = "graphPerDomaine";
    private static final String LIBELLE = "libelle";
    public static final String ERROR = "errors";
    public static final String TIME = "Time";
    private static final String HAS_PROJECT = "hasProject";
    private static final String ID_IMAGES_FILES = "idImagesFiles";
    private static final String IS_DOMAINE_PARENT = "isDomaineParent";
    public static final String CLASSE_NAME_TO_SHOW = "classeNameToShow";
    private static final String NAME_STRUCTURE = "nameStructure";
    private static final String BEFORE_SYNTHESE_BP = "Synthèse de l'évolution des acquis : ";

    public static final String PERIODE = "periode";
    private static final String STRUCTURE_LIBELLE = "structureLibelle";
    public static final String STRUCTURE = "structure";
    private static final String NEUTRE = "neutre";

    public static final String USE_MODEL_KEY = "useModel";
    public static final String TYPE_PERIODE = "typePeriode";
    public static final String TIMED_OUT = "Timed out";
    public static final String ACCEPT_LANGUAGE = "accept-language";
    public static final String HOST = "host";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String SCHEME = "scheme";
    public static final String PATH = "path";
    public static final String FIRST_NAME_KEY = "firstName";
    public static final String LAST_NAME_KEY = "lastName";

    // Parameter Key
    private static final String GET_MOYENNE_CLASSE = "getMoyenneClasse";
    private static final String GET_MOYENNE_ELEVE = "getMoyenneEleve";
    private static final String GET_STUDENT_RANK = "getStudentRank";
    private static final String GET_CLASS_AVERAGE_MINMAX = "getClassAverageMinMax";
    private static final String GET_POSITIONNEMENT = "getPositionnement";
    private static final String GET_PROGRAM_ELEMENT = "getProgramElements";
    private static final String HAS_IMG_STRUCTURE = "hasImgStructure";
    private static final String HAS_IMG_SIGNATURE = "hasImgSignature";
    private static final String IMG_STRUCTURE = "imgStructure";
    private static final String IMG_SIGNATURE = "imgSignature";
    private static final String NAME_CE = "nameCE";
    private static final String SHOW_PROJECTS = "showProjects";
    private static final String SHOW_BILAN_PER_DOMAINE = "showBilanPerDomaines";
    private static final String SHOW_FAMILY = "showFamily";


    // Données-membres privées
    private EventBus eb;
    private BilanPeriodiqueService bilanPeriodiqueService;
    private ElementBilanPeriodiqueService  elementBilanPeriodiqueService;
    private final DefaultAppreciationCPEService appreciationCPEService;
    private final DefaultSyntheseBilanPeriodiqueService syntheseBilanPeriodiqueService;
    private AvisConseilService avisConseilService;
    private AvisOrientationService avisOrientationService;
    private DomainesService domainesService;
    private ExportService exportService;
    private final Storage storage;
    private UtilsService utilsService;
    private final DefaultCompetenceNoteService competenceNoteService;
    private final DefaultNiveauDeMaitriseService defaultNiveauDeMaitriseService;
    private HttpClient httpClient;
    private DefaultNoteService noteService;
    private SubTopicService subTopicService;
    private WorkspaceHelper workspaceHelper;
    private MongoExportService mongoExportService;

    public DefaultExportBulletinService(EventBus eb, Storage storage) {
        this.eb = eb;
        bilanPeriodiqueService = new DefaultBilanPerioqueService(eb);
        elementBilanPeriodiqueService = new DefaultElementBilanPeriodiqueService(eb);
        appreciationCPEService = new DefaultAppreciationCPEService();
        syntheseBilanPeriodiqueService = new DefaultSyntheseBilanPeriodiqueService();
        avisConseilService = new DefaultAvisConseilService();
        avisOrientationService = new DefaultAvisOrientationService();
        domainesService = new DefaultDomaineService(Competences.COMPETENCES_SCHEMA, Competences.DOMAINES_TABLE);
        exportService = new DefaultExportService(eb, storage);
        utilsService = new DefaultUtilsService(eb);
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA,
                Competences.COMPETENCES_NOTES_TABLE);
        this.storage = storage;
        this.mongoExportService = new DefaultMongoExportService();
        defaultNiveauDeMaitriseService = new DefaultNiveauDeMaitriseService();
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE,eb);
        workspaceHelper = new WorkspaceHelper(eb,storage);
        subTopicService = new DefaultSubTopicService(Competences.COMPETENCES_SCHEMA, "services_subtopic");

    }

    public DefaultExportBulletinService(EventBus eb, Storage storage, Vertx vertx) {
        this.eb = eb;
        bilanPeriodiqueService = new DefaultBilanPerioqueService(eb);
        elementBilanPeriodiqueService = new DefaultElementBilanPeriodiqueService(eb);
        appreciationCPEService = new DefaultAppreciationCPEService();
        syntheseBilanPeriodiqueService = new DefaultSyntheseBilanPeriodiqueService();
        avisConseilService = new DefaultAvisConseilService();
        avisOrientationService = new DefaultAvisOrientationService();
        domainesService = new DefaultDomaineService(Competences.COMPETENCES_SCHEMA, Competences.DOMAINES_TABLE);
        exportService = new DefaultExportService(eb, storage);
        utilsService = new DefaultUtilsService(eb);
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA,
                Competences.COMPETENCES_NOTES_TABLE);
        this.storage = storage;
        this.mongoExportService = new DefaultMongoExportService();
        defaultNiveauDeMaitriseService = new DefaultNiveauDeMaitriseService();
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE,eb);
        this.httpClient =  createHttpClient(vertx);
        workspaceHelper = new WorkspaceHelper(eb,storage);
        subTopicService = new DefaultSubTopicService(Competences.COMPETENCES_SCHEMA, "services_subtopic");

    }

    private  Handler<AsyncResult<Message<JsonObject>>>
    getInfoEleveHandler(Future<JsonArray> elevesFuture, JsonArray idEleves, String idEtablissement) {
        return handlerToAsyncHandler(
                message -> {
                    if ("ok".equals(message.body().getString(STATUS))) {
                        elevesFuture.complete(message.body().getJsonArray(RESULTS));

                    }
                    else
                    {
                        String error = message.body().getString(MESSAGE);
                        log.error("[runExport | getInfoEleve ] : " + error);
                        if(error.contains(TIMED_OUT)){
                            JsonObject action = new JsonObject()
                                    .put(ACTION, "eleve.getInfoEleve")
                                    .put(ID_ETABLISSEMENT_KEY, idEtablissement)
                                    .put("idEleves", idEleves);
                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS,
                                    getInfoEleveHandler(elevesFuture, idEleves, idEtablissement));
                        }
                        else {
                            elevesFuture.fail(error);
                        }
                    }
                });
    }

    private void stopExportBulletin(final Handler<Either<String, JsonObject>> finalHandler,
                                    String message){
        finalHandler.handle(new Either.Left(message));
    }

    public void runExportBulletin(String idEtablissement, String idClasse, JsonArray idStudents, Long idPeriode,
                                  JsonObject params, Future<JsonArray> idElevesFuture,
                                  final Map<String, JsonObject> elevesMap, final AtomicBoolean answered, String host,
                                  String acceptLanguage, final Handler<Either<String, JsonObject>> finalHandler,
                                  Future<JsonObject> future, Vertx vertx){
        // On récupère les informations basic des élèves (nom, Prenom, niveau, date de naissance,  ...)
        Future<JsonArray> elevesFuture = Future.future();
        if (idStudents != null) {
            idElevesFuture.complete(idStudents);
        }

        CompositeFuture.all(idElevesFuture, Future.succeededFuture()).setHandler(
                getHandlerGetInfosEleves(idEtablissement, idClasse, idStudents, idPeriode, idElevesFuture, future, elevesFuture));

        // On récupère le tableau de conversion des compétences notes pour Lire le positionnement
        Future<JsonArray> tableauDeConversionFuture = Future.future();
        getConversionNoteCompetence(idEtablissement, idClasse, tableauDeConversionFuture);

        // Si on doit utiliser un model de libelle, On le récupère
        Future<JsonArray> modelsLibelleFuture = Future.future();
        Boolean useModel = params.getBoolean(USE_MODEL_KEY);
        if (useModel) {
            Long idModel = params.getLong("idModel");
            new DefaultMatiereService(eb).getModels(idEtablissement, idModel, models -> {
                formate(modelsLibelleFuture, models);
            });
        } else {
            modelsLibelleFuture.complete(new JsonArray());
        }
        // Lorsqu'on a le suivi des Acquis et le tableau de conversion, on lance la récupération
        // complète des données de l'export
        CompositeFuture.all(tableauDeConversionFuture, elevesFuture, modelsLibelleFuture).setHandler(
                initClassObjectInfo(idEtablissement, idClasse, idPeriode, params, elevesMap, answered, host, acceptLanguage, finalHandler,
                        elevesFuture, tableauDeConversionFuture, modelsLibelleFuture, useModel, vertx));
    }

    private Handler<AsyncResult<CompositeFuture>> initClassObjectInfo(String  idEtablissement, String idClasse,
                                                                      Long idPeriode, JsonObject params, Map<String, JsonObject> elevesMap,
                                                                      AtomicBoolean answered, String host, String acceptLanguage,
                                                                      Handler<Either<String, JsonObject>> finalHandler,
                                                                      Future<JsonArray> elevesFuture,
                                                                      Future<JsonArray> tableauDeConversionFuture,
                                                                      Future<JsonArray> modelsLibelleFuture,
                                                                      Boolean useModel, Vertx vertx) {
        return event -> {
            if (event.succeeded()) {
                JsonArray eleves = elevesFuture.result();
                eleves.stream().forEach( eleve -> {
                    JsonObject o_eleve = (JsonObject) eleve;
                    // if student is deleted and moved to another structure, his idStructure is different with front idStructure
                    if(o_eleve.containsKey("deleteDate") && !idEtablissement.equals(o_eleve.getString("idEtablissement")))
                        o_eleve.put("idEtablissement", idEtablissement );
                });
                eleves = Utils.sortElevesByDisplayName(eleves);
                final JsonObject classe = new JsonObject().put("tableauDeConversion", tableauDeConversionFuture.result());
                if (useModel) {
                    JsonArray models = modelsLibelleFuture.result();
                    if (!models.isEmpty()) {
                        models = models.getJsonObject(0).getJsonArray(SUBJECTS);
                    }
                    classe.put("models", models);
                }

                Boolean showBilanPerDomaines = params.getBoolean("showBilanPerDomaines");

                classe.put("idClasse", idClasse);
                if(params.containsKey("classeName")){
                    classe.put("classeName", params.getString("classeName"));
                    buildDataForStudent(answered, eleves, elevesMap, idPeriode, params, classe,
                            showBilanPerDomaines, host, acceptLanguage, finalHandler, vertx);
                } else {
                    JsonArray finalEleves = eleves;
                    getClasseInfo(idClasse, classeInfoEvent -> {
                        if(classeInfoEvent.isRight()){
                            classe.put("classeName", classeInfoEvent.right().getValue());

                            buildDataForStudent(answered, finalEleves, elevesMap, idPeriode, params, classe,
                                    showBilanPerDomaines, host, acceptLanguage, finalHandler, vertx);
                        } else {
                            String error = "[Viescolaire] @ DefaultExportBulletinService error when getting class";
                            finalHandler.handle(new Either.Left(error));
                        }
                    });
                }
            } else {
                // S'il y a un problème lors d'une récupération , on stoppe tout
                String error = "[runExportBulletin] : " + event.cause().getMessage();
                finalHandler.handle(new Either.Left(error));
            }
        };
    }

    private Handler<AsyncResult<CompositeFuture>> getHandlerGetInfosEleves(String idEtablissement, String idClasse,
                                                                           JsonArray idStudents, Long idPeriode,
                                                                           Future<JsonArray> idElevesFuture, Future<JsonObject> future,
                                                                           Future<JsonArray> elevesFuture) {
        return idElevesEvent -> {
            if (idElevesEvent.failed()) {
                log.error("getHandlerGetInfosEleves :" + idElevesFuture.cause().getMessage());
                future.complete(null);
            } else {
                JsonArray idEleves = idStudents;
                if (idEleves == null) {
                    idEleves = new JsonArray(idElevesFuture.result().stream()
                            .map(e -> ((JsonObject) e).getString(ID_ELEVE_KEY))
                            .collect(Collectors.toList()));
                }
                // si on a aucun élève, pas la peine de faire un export, on stoppe tout
                if (idEleves.isEmpty()) {
                    String message = "No student for classe " + idClasse + " and periode " + idPeriode;
                    elevesFuture.fail(message);
                    return;
                }
                JsonObject action = new JsonObject()
                        .put(ACTION, "eleve.getInfoEleve")
                        .put(ID_ETABLISSEMENT_KEY, idEtablissement)
                        .put("idEleves", idEleves);
                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                        getInfoEleveHandler(elevesFuture, idEleves, idEtablissement));
            }
        };
    }

    private void getClasseInfo(String idClasse, Handler<Either<String,String>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getClasseInfo")
                .put(ID_CLASSE_KEY, idClasse);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action,  DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                String classe = body.getJsonObject(RESULT).getJsonObject("c").getJsonObject("data").getString(NAME);

                handler.handle(new Either.Right<>(classe));
            }else {
                handler.handle(new Either.Left<>("getClasseInfo failed"));
            }
        }));
    }

    private void logBegin(String method, String idEleve) {
        log.debug("------- [" + method + "]: " + idEleve + " DEBUT " );
    }

    @Override
    public void putLibelleForExport(String idEleve, Map<String, JsonObject> elevesMap, JsonObject params,
                                    Handler<Either<String, JsonObject>> finalHandler){
        JsonObject eleve = elevesMap.get(idEleve);
        logBegin(PUT_LIBELLE_FOR_EXPORT_METHOD, idEleve);
        if(eleve == null) {
            logStudentNotFound(idEleve, PUT_LIBELLE_FOR_EXPORT_METHOD);
        }
        else {
            eleve.put("suiviAcquisLibelle", getLibelle("evaluation.bilan.periodique.suivi.acquis")
                    + " " + getLibelle("of.student"))
                    .put("communicationLibelle", getLibelle("viescolaire.communication.with.familly"))
                    .put("communicationHeaderRightFirst",
                            getLibelle("evaluations.export.bulletin.communication.header.right.first"))
                    .put("communicationHeaderRightSecond",
                            getLibelle("evaluations.export.bulletin.communication.header.right.second"))
                    .put("moyenneClasseLibelle", getLibelle("average.min.classe"))
                    .put("suiviElementLibelle",
                            getLibelle("evaluations.export.bulletin.element.programme.libelle"))
                    .put("suiviAcquisitionLibelle",
                            getLibelle("evaluations.export.bulletin.element.appreciation.libelle"))
                    .put("positionementLibelle", getLibelle("evaluations.releve.positionnement.min") + '*')
                    .put("studentRankLibelle", getLibelle("sudent.rank"))
                    .put("classAverageMinLibelle", getLibelle("classaverage.min"))
                    .put("classAverageMaxLibelle", getLibelle("classaverage.max"))
                    .put("moyenneStudentLibelle", getLibelle("average.min.eleve"))
                    .put("bilanAcquisitionLibelle", getLibelle("viescolaire.suivi.des.acquis.libelle.export"))
                    .put("viescolaireLibelle", getLibelle("evaluations.export.bulletin.viescolaireLibelle"))
                    .put("familyVisa", getLibelle("evaluations.export.bulletin.visa.libelle"))
                    .put("signature", getLibelle("evaluations.export.bulletin.date.name.visa.responsable"))
                    .put("bornAt", getLibelle("born.on"))
                    .put("classeOf", getLibelle("classe.of"))
                    .put("numberINE", getLibelle("number.INE"))
                    .put("bilanPerDomainesLibelle", getLibelle("evaluations.bilan.by.domaine"))
                    .put("levelStudent", getLibelle("level.student"))
                    .put("levelClass", getLibelle("level.class"))
                    .put("averageStudent", getLibelle("average.student"))
                    .put("averageClass", getLibelle("average.class"))
                    .put("levelItems", getLibelle("level.items"))
                    .put("averages", getLibelle("average"))
                    .put("evaluationByDomaine", getLibelle("evaluation.by.domaine"))
                    .put("coefficientLibelle", getLibelle("viescolaire.utils.coef"))
                    .put("moyenneAnnuelleLibelle", getLibelle("average.annual"))
                    .put("moyenneGeneraleLibelle", getLibelle("average.general"))

                    // positionnement des options d'impression
                    .put(GET_RESPONSABLE, params.getBoolean(GET_RESPONSABLE))
                    .put(GET_MOYENNE_CLASSE, params.getBoolean(MOYENNE_CLASSE))
                    .put(GET_MOYENNE_ELEVE, params.getBoolean(MOYENNE_ELEVE))
                    .put(GET_STUDENT_RANK, params.getBoolean(STUDENT_RANK))
                    .put(GET_CLASS_AVERAGE_MINMAX, params.getBoolean(CLASS_AVERAGE_MINMAX))
                    .put(GET_POSITIONNEMENT, params.getBoolean(POSITIONNEMENT))
                    .put(SHOW_PROJECTS, params.getBoolean(SHOW_PROJECTS))
                    .put(SHOW_FAMILY, params.getBoolean(SHOW_FAMILY))
                    .put(GET_PROGRAM_ELEMENT, params.getBoolean(GET_PROGRAM_ELEMENT))
                    .put(SHOW_BILAN_PER_DOMAINE, params.getBoolean(SHOW_BILAN_PER_DOMAINE))
                    .put(IMG_STRUCTURE, params.getString(IMG_STRUCTURE))
                    .put(HAS_IMG_STRUCTURE, params.getBoolean(HAS_IMG_STRUCTURE))
                    .put(IMG_SIGNATURE, params.getString(IMG_SIGNATURE))
                    .put(HAS_IMG_SIGNATURE, params.getBoolean(HAS_IMG_SIGNATURE))
                    .put(NAME_CE, params.getString(NAME_CE))
                    .put(PRINT_COEFFICIENT, params.getBoolean(COEFFICIENT))
                    .put(PRINT_SOUS_MATIERES, params.getBoolean(PRINT_SOUS_MATIERES))
                    .put(PRINT_MOYENNE_ANNUELLE, params.getBoolean(MOYENNE_ANNUELLE))
                    .put(NEUTRE, params.getBoolean(NEUTRE, false))
                    .put(HIDE_HEADTEACHER, params.getBoolean(HIDE_HEADTEACHER,false))
                    .put(ADD_OTHER_TEACHER, params.getBoolean(ADD_OTHER_TEACHER,false))
                    .put(FUNCTION_OTHER_TEACHER, params.getString(FUNCTION_OTHER_TEACHER,""))
                    .put(OTHER_TEACHER_NAME, params.getString(OTHER_TEACHER_NAME,""))
                    .put(AGRICULTURE_LOGO, params.getBoolean(AGRICULTURE_LOGO,false));

            JsonArray niveauCompetences;
            try{
                niveauCompetences = (JsonArray) params.getValue(NIVEAU_COMPETENCE);

            }catch (java.lang.ClassCastException e){
                niveauCompetences = new JsonArray(params.getString(NIVEAU_COMPETENCE));
            }
            JsonArray footerArray = new JsonArray();
            if(niveauCompetences != null && !niveauCompetences.isEmpty()){
                for (int i = niveauCompetences.size() - 1; i >= 0; i--) { //reverse Array
                    footerArray.add(niveauCompetences.getJsonObject(i));
                }
            }

            String caption = "";
            if(!footerArray.isEmpty()){
                for (int i = 0; i < footerArray.size(); i++) {
                    JsonObject niv = footerArray.getJsonObject(i);

                    String lib = niv.getString(LIBELLE);
                    Integer id_niv;
                    Integer id_cycle = niv.getInteger("id_cycle");
                    try{
                        id_niv = niv.getInteger("id_niveau");
                        if(id_cycle == 2){
                            id_niv -= 4;
                        }
                    }catch (NullPointerException e){
                        id_niv = id_cycle;
                    }

                    caption += id_niv + " : " + lib + " - ";
                }
                caption = caption.substring(0, caption.length() - 2);
            }

            eleve.put(NIVEAU_COMPETENCE, niveauCompetences).put("caption", "* " + caption);

            if(isNotNull(params.getValue(AGRICULTURE_LOGO)) && params.getBoolean(AGRICULTURE_LOGO)){
                eleve.put(LOGO_PATH, "img/ministere_agriculture.png");
            } else {
                eleve.put(LOGO_PATH, "img/education_nationale.png");
            }

        }
        log.debug(" -------[" + PUT_LIBELLE_FOR_EXPORT_METHOD +" ]: " + idEleve + " FIN " );
        finalHandler.handle(new Either.Right<>(null));
    }

    protected Handler<Either<String, JsonObject>> futureGetHandler(Future<JsonObject> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                future.fail(event.left().getValue());
            }
        };
    }

    @Override
    public void getExportBulletin(final AtomicBoolean answered, String idEleve,
                                  Map<String, JsonObject> elevesMap, Student student, JsonArray idEleves, Long idPeriode, JsonObject params,
                                  final JsonObject classe, String host, String acceptLanguage,
                                  Vertx vertx, Handler<Either<String, JsonObject>> finalHandler){
        try {
            if (!answered.get()) {
                Boolean isBulletinLycee = (isNotNull(params.getValue("simple"))) ? params.getBoolean("simple") : false ;
                String beforeAvisConseil = params.getString("mentionOpinion") + " : ";
                String beforeAvisOrientation = params.getString("orientationOpinion");
                List<Future> futures = new ArrayList<>();

                //FAIRE DES PROMISES
                Promise<List<StudentEvenement>> getEvenementsPromise = Promise.promise();
                Promise<JsonObject> getSyntheseBilanPeriodiquePromise = Promise.promise();
                Promise<JsonObject> getCyclePromise = Promise.promise();
                Promise<JsonObject> getAppreciationCPEPromise  = Promise.promise();
                Promise<JsonObject> getAvisConseilPromise = Promise.promise();
                Promise<JsonObject> getAvisOrientationPromise = Promise.promise();
                Promise<JsonObject>  getSuiviAcquisPromise = Promise.promise();

                futures.add(getEvenementsPromise.future());
                futures.add(getSyntheseBilanPeriodiquePromise.future());
                futures.add(getCyclePromise.future());
                futures.add(getAppreciationCPEPromise.future());
                futures.add(getAvisConseilPromise.future());
                futures.add(getAvisOrientationPromise.future());
                futures.add(getSuiviAcquisPromise.future());

                int nbOptions = 0;


                if(params.getBoolean(GET_RESPONSABLE)) {
                    Promise<Object> getResponsablesPromise = Promise.promise();
                    futures.add(getResponsablesPromise.future());
                    getResponsables(student,getResponsablesPromise);
                    nbOptions++;
                }

                if(params.getBoolean(SHOW_BILAN_PER_DOMAINE)) {
                    Promise<Object> getImageGraphPromise = Promise.promise();
                    Promise<Object> getArbreDomainesPromise = Promise.promise();
                    futures.add(getImageGraphPromise.future());
                    futures.add(getArbreDomainesPromise.future());
                    getImageGraph(student, getImageGraphPromise);
                    getArbreDomaines(student, getArbreDomainesPromise);
                    student.getParamBulletins().addParams(new JsonObject().put("hasGraphPerDomaine", true));
                    nbOptions += 2;
                }

                if (params.getBoolean(SHOW_PROJECTS)) {
                    Promise<Object> getProjetsPromise = Promise.promise();
                    futures.add(getProjetsPromise.future());
                    getProjets(student , getProjetsPromise);
                    nbOptions++;

                }


                //utiles?
                if(params.getValue(GET_DATA_FOR_GRAPH_DOMAINE_METHOD) != null){
                    if(params.getBoolean(GET_DATA_FOR_GRAPH_DOMAINE_METHOD)){
                        Promise<Object> getBilanPeriodiqueDomaineForGraphPromise = Promise.promise();
                        futures.add(getBilanPeriodiqueDomaineForGraphPromise.future());
                        getBilanPeriodiqueDomaineForGraph(student , getBilanPeriodiqueDomaineForGraphPromise);
                        nbOptions++;
                    }
                }

                getEvenements(student, getEvenementsPromise);
                getSyntheseBilanPeriodique(student,isBulletinLycee,
                        getSyntheseBilanPeriodiquePromise);
                getCycle(student,getCyclePromise);
                getAppreciationCPE(student,getAppreciationCPEPromise);
                getAvisConseil(student ,getAvisConseilPromise, beforeAvisConseil);
                getAvisOrientation(student,
                        getAvisOrientationPromise, beforeAvisOrientation);
                getSuiviAcquis(student,idEleves, classe, params, getSuiviAcquisPromise);

                int finalNbOptions = nbOptions;
                CompositeFuture.all(futures).setHandler(event -> {
                    if (event.succeeded()) {
                        try {
                            List<StudentEvenement> studentEvenements = getEvenementsPromise.future().result();
                            student.getParamBulletins().addParams(getSyntheseBilanPeriodiquePromise.future().result());
                            student.getParamBulletins().addParams(getCyclePromise.future().result());
                            student.getParamBulletins().addParams(getAppreciationCPEPromise.future().result());
                            student.getParamBulletins().addParams(getAvisConseilPromise.future().result());
                            student.getParamBulletins().addParams(getAvisOrientationPromise.future().result());
                            student.getParamBulletins().addParams(getSuiviAcquisPromise.future().result());
                            for(StudentEvenement studentEvenement : studentEvenements){
                                student.addEvenement(studentEvenement);
                            }

                            //OPTIONS
                            for (int i = 1; i <= finalNbOptions ; i++ ) {
                                student.getParamBulletins().addParams((JsonObject) event.result().list().get(6 + i));
                            }
//                            elevesMap.put(student.getId(),student.toJsonObject());
                            log.info("[Competences:@getExportBulletin]------------------"+ idEleve + " end get datas for export bulletin  ---------------------");
                            finalHandler.handle(new Either.Right<>(student.toJsonObject()));
                        }catch (Exception e){
                            finalHandler.handle(new Either.Left<>(e.getMessage()));

                        }

                    }else {
                        log.error("[Competences] at getExportBulletin error when getting datas for export bulletins : student :" + idEleve);
                    }
                });
            } else {
                finalHandler.handle(new Either.Left<>("[Viescolaire]getExportBulletin :  Problème de parallelisation Lors de l'export des bulletin "));
            }
        }
        catch (Exception e) {
            log.error(EXPORT_BULLETIN_METHOD, e);
            finalHandler.handle(new Either.Left<>("[Viescolaire]getExportBulletin : " + e.getMessage()));
        }
    }

    private AtomicInteger nbServices (JsonObject params) {
    /*
        - Récupération des retards et absences
        - Récupération du suivi des acquis
        - Récupération du libelle de l'établissement
        - Récupération des professeurs principaux
        - Récupération de la synthèse du relevé périodique
        - Récupération du libelle de la période
        - Récupération de l'année scolaire
        - Récupère le cycle de la classe de l'élève
        - Rajoute tous les libelles i18n nécessaires pour le bulletin
        - Récupère l'appréciation CPE
        - Récupération de l'avis du conseil de l'élève
        - Récupération de l'avis d'orientation de l'élève
        - Récupération des Responsable légaux de l'élève (si GET_RESPONSABLE)

        - Récupération des EPI/AP/PARCOURS (si SHOW_PROJECTS)

        - Récupération de l'arbre des domaines de l'élève (si SHOW_BILAN_PER_DOMAINE)
        - Récupération du Graphe du Bilan par domaine de l'élève (si SHOW_BILAN_PER_DOMAINE)


     */
        int nbServices = 12;
        if (params.getBoolean(GET_RESPONSABLE)) {
            ++nbServices;
        }
        if (params.getBoolean(SHOW_PROJECTS)) {
            ++nbServices;
        }
        if (params.getBoolean(SHOW_BILAN_PER_DOMAINE)) {
            nbServices += 2;
        }
        if(params.getValue(GET_DATA_FOR_GRAPH_DOMAINE_METHOD)!= null) {
            if (params.getBoolean(GET_DATA_FOR_GRAPH_DOMAINE_METHOD)) {
                ++nbServices;
            }
        }
        if(params.getBoolean(HIDE_HEADTEACHER,false))
            --nbServices;
        return new AtomicInteger(nbServices);

    }

    private Handler<Either<String, JsonObject>> createFinalHandler(final HttpServerRequest request,
                                                                   Map<String, JsonObject> elevesMap,
                                                                   Vertx vertx, JsonObject config,
                                                                   Future<JsonArray> elevesFuture,
                                                                   JsonObject params, Boolean forArchive,
                                                                   Future<JsonObject> future){
        return event -> {
            if (event.isRight()) {
                CompositeFuture.all(elevesFuture, Future.succeededFuture()).setHandler(elevesEvent -> {
                    if(elevesEvent.failed()){
                        log.error("getFinalBulletinHandler | error ");
                        if(future != null){
                            future.fail(elevesFuture.cause());
                        }
                    }
                    String title = params.getString(CLASSE_NAME) + "_" + I18n.getInstance()
                            .translate("evaluations.bulletin",
                                    I18n.DEFAULT_DOMAIN, Locale.FRANCE);
                    JsonObject resultFinal = new JsonObject()
                            .put(GET_PROGRAM_ELEMENT, params.getBoolean(GET_PROGRAM_ELEMENT))
                            .put("title", title)
                            .put(NEUTRE, params.getBoolean(NEUTRE));
                    if(params.getBoolean(COEFFICIENT) && isNotNull(params.getValue(ERROR + COEFFICIENT))){
                        renderError(request, new JsonObject().put(ELEVES,
                                sortResultByClasseNameAndNameForBulletin(elevesMap)));
                        return;
                    }
                    resultFinal.put(ELEVES, sortResultByClasseNameAndNameForBulletin(elevesMap));
                    if(!forArchive) {
                        resultFinal.put(ID_IMAGES_FILES, params.getJsonArray(ID_IMAGES_FILES));
                        String template = "bulletin.pdf.xhtml";
                        if(isNotNull(params.getValue("simple")) && params.getBoolean("simple")) {
                            template = "bulletin_lycee.pdf.xhtml";
                        }

                        JsonObject jsonRequest = new JsonObject()
                                .put("headers", new JsonObject()
                                        .put("Accept-Language", request.headers().get("Accept-Language")))
                                .put("Host", getHost(request));
                        JsonArray students = resultFinal.getJsonArray("eleves");
                        List<Future<String>> futureArray =  mongoExportService.insertDataInMongo(students,resultFinal,jsonRequest,title,template,SAVE_BULLETIN);
                        FutureHelper.all(futureArray).onSuccess(success ->{
                            log.info("[Competences DefaultExportBulletinService ] insert bulletins data in Mongo done");
                            eb.send(BulletinWorker.class.getSimpleName(), new JsonObject(), Competences.DELIVERY_OPTIONS);
                        }).onFailure(error ->{
                            log.info(error.getMessage());
                        });
                        exportService.generateSchoolReportPdf(request, resultFinal, template, title, vertx, config);
                    }
                });
            } else {
                log.error("createFinalHandler : " + event.left().getValue());
                badRequest(request);
                return;
            }
        };
    }

    private List<Future<String>> insertDataInMongo(JsonObject resultFinal) {
        JsonObject common  = resultFinal.copy();
        common.remove("eleves");
        JsonArray students = resultFinal.getJsonArray("eleves");
        List<Future<String>> futureArray= new ArrayList<>();
        for(Object studentJO : students){
            JsonObject student = (JsonObject) studentJO;
            student.remove("u.deleteDate");
            common.put("eleve",student);
            Promise<String> promise = Promise.promise();
            mongoExportService.createWhenStart("pdf", common,
                    SAVE_BULLETIN,promise);
            futureArray.add(promise.future());
        }
        return futureArray;
    }

    @Override
    public Handler<Either<String, JsonObject>> getFinalBulletinHandler(final HttpServerRequest request,
                                                                       Map<String, JsonObject> elevesMap,
                                                                       Vertx vertx, JsonObject config,
                                                                       Future<JsonArray> elevesFuture,
                                                                       JsonObject params) {
        return createFinalHandler(request, elevesMap, vertx, config, elevesFuture, params,false,null);
    }

    private void getBilanPeriodiqueDomaineForGraph(Student student,
                                                   Promise<Object> promise){


        String idEtablissement = student.getStructure().getId();
        String idEleve = student.getId();
        String idClasse = student.getClasse().getId();
        JsonObject result = new JsonObject();
        final Integer typeClasse = 0;
        final String idPeriodeString = student.getClasse().getPeriode().getId();

        bilanPeriodiqueService.getBilanPeriodiqueDomaineForGraph(idEleve, idEtablissement, idClasse, typeClasse,
                idPeriodeString,
                new Handler<Either<String, JsonArray>>() {
                    private int count = 1;
                    private AtomicBoolean answer = new AtomicBoolean(false);

                    @Override
                    public void handle(Either<String, JsonArray> datasEvent) {
                        if(datasEvent.isLeft()) {
                            String error = datasEvent.left().getValue();
                            log.error("[Competences:@" + GET_DATA_FOR_GRAPH_DOMAINE_METHOD + "] : " + error + " " + count);
                            if(error.contains(TIME)) {
                                count++;
                                bilanPeriodiqueService.getBilanPeriodiqueDomaineForGraph(idEleve,idEtablissement,
                                        idClasse, typeClasse, idPeriodeString, this);
                            }
                            else {
                                promise.fail("[Competences:@" + GET_DATA_FOR_GRAPH_DOMAINE_METHOD + "] : " + error + " " + count);
                            }
                        }
                        else {
                            JsonArray domainsDatas = datasEvent.right().getValue();
                            String datas = "[";
                            for(int i = 0; i < domainsDatas.size(); i++) {
                                datas += domainsDatas.getJsonObject(i).encode() + ",";
                            }
                            if(datas.length() > 1 ) datas = datas.substring(0, datas.length() - 1);
                            datas += "]";
                            result.put("_data", datas);
                            log.info( "[Competences:@"+ GET_DATA_FOR_GRAPH_DOMAINE_METHOD + " data put on jsobjectEleve " +datas );
                            promise.complete(result);
                        }
                    }
                });
    }

    @Override
    public void getCycle (Student student, Promise<JsonObject> promise) {
        String idEleve = student.getId();
        String idClasse = student.getClasse().getId();
        Long typePeriode = student.getClasse().getPeriode().getType();
        logBegin(GET_CYCLE_METHOD, idEleve);
        if (idClasse == null) {
            promise.fail("[getCycle]| Object eleve doesn't contains field idClasse ");
        }
        else {
            JsonObject action = new JsonObject()
                    .put(ACTION, "eleve.getCycle")
                    .put(ID_CLASSE, idClasse);
            eb.request(Competences.VIESCO_BUS_ADDRESS, action,Competences.DELIVERY_OPTIONS,
                    handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                        private int count = 1;
                        private AtomicBoolean answer = new AtomicBoolean(false);
                        @Override
                        public void handle(Message<JsonObject> result) {
                            JsonObject body = result.body();
                            if (!"ok".equals(body.getString(STATUS))) {
                                String message =  body.getString(MESSAGE);
                                promise.fail("[Competences:@getCycle] : " + idEleve + " " + message + count);
//                                buildErrorReponseForEb (idEleve, message, answer, count, action,
//                                        this, finalHandler, eleve, GET_LIBELLE_PERIOD_METHOD);
                            } else {
                                JsonArray results = body.getJsonArray(RESULTS);
                                if(results.size() > 0) {
                                    final String libelle = results.getJsonObject(0)
                                            .getString(LIBELLE);
                                    promise.complete(new JsonObject().put("bilanCycle",
                                            getLibelle("evaluations.bilan.periodique.of." + typePeriode)
                                                    + libelle));
                                } else {
                                    promise.fail("[Competences:@"+ GET_CYCLE_METHOD + "]" + idEleve + "| no link to cycle for object " +
                                            idClasse);
                                }
                            }
                        }
                    }));
        }
    }

    @Override
    public void getLibellePeriode(String idEleve, Map<String, JsonObject> elevesMap, Long idPeriode,
                                  String host, String acceptLanguage,
                                  Handler<Either<String, JsonObject>> finalHandler){
        JsonObject eleve = elevesMap.get(idEleve);
        logBegin(GET_LIBELLE_PERIOD_METHOD, idEleve);
        if(eleve == null) {
            logStudentNotFound(idEleve, GET_LIBELLE_PERIOD_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            JsonObject jsonRequest = new JsonObject()
                    .put("headers", new JsonObject().put("Accept-Language", acceptLanguage))
                    .put("Host",host);

            JsonObject action = new JsonObject()
                    .put(ACTION, "periode.getLibellePeriode")
                    .put("idType", idPeriode)
                    .put("request", jsonRequest);
            eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                    handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                        private int count = 1;
                        private AtomicBoolean answer = new AtomicBoolean(false);
                        @Override
                        public void handle(Message<JsonObject> message) {
                            JsonObject body = message.body();

                            if (!"ok".equals(body.getString(STATUS))) {
                                String mess =  body.getString(MESSAGE);
                                log.error("[ getLibellePeriode ] : " + idEleve + " " + mess + " " + count);
                                buildErrorReponseForEb (idEleve, mess, answer, count, action,
                                        this, finalHandler, eleve, GET_LIBELLE_PERIOD_METHOD);

                            } else {
                                String periodeName = body.getString(RESULT);
                                eleve.put(PERIODE, periodeName);
                                serviceResponseOK(answer, finalHandler, count, idEleve, GET_LIBELLE_PERIOD_METHOD);
                            }
                        }
                    }));
        }
    }


    public void getLibellePeriode(Long idPeriode,
                                  String host, String acceptLanguage,
                                  Promise<Periode> promise) {
        JsonObject jsonRequest = new JsonObject()
                .put("headers", new JsonObject().put("Accept-Language", acceptLanguage))
                .put("Host",host);
        JsonObject action = new JsonObject()
                .put(ACTION, "periode.getLibellePeriode")
                .put("idType", idPeriode)
                .put("request", jsonRequest);
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();

                        String periodeName = body.getString(RESULT);
                        Periode periode = new Periode();
                        periode.setId(idPeriode.toString());
                        periode.setName(periodeName);
                        promise.complete(periode);
                    }
                }));
    }

    @Override
    public void getAnneeScolaire(String idEleve, String idClasse, JsonObject eleve, Handler<Either<String, JsonObject>> finalHandler) {
        logBegin(GET_ANNEE_SCOLAIRE_METHOD, idEleve);
        if (eleve == null) {
            logStudentNotFound(idEleve, GET_ANNEE_SCOLAIRE_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            if (idClasse == null) {
                logidClasseNotFound(idEleve, GET_ANNEE_SCOLAIRE_METHOD);
            }
            else {
                JsonObject action = new JsonObject();
                action.put(ACTION, "periode.getPeriodes")
                        .put("idGroupes", new fr.wseduc.webutils.collections.JsonArray().add(idClasse));

                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                        handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                            private int count = 1;
                            private AtomicBoolean answer = new AtomicBoolean(false);
                            @Override
                            public void handle(Message<JsonObject> message) {
                                JsonObject body = message.body();
                                JsonArray periodes = body.getJsonArray(RESULT);
                                String mess = body.getString(MESSAGE);
                                if (!"ok".equals(body.getString(STATUS))) {
                                    log.error("[" + GET_ANNEE_SCOLAIRE_METHOD + "] : " + idEleve + " " + mess + " "
                                            + count);

                                    buildErrorReponseForEb (idEleve, mess, answer, count, action,
                                            this, finalHandler, eleve, GET_ANNEE_SCOLAIRE_METHOD);
                                }
                                else {
                                    Long debut = null;
                                    Long fin = null;
                                    for (int i = 0; i < periodes.size(); i++) {
                                        JsonObject periode = periodes.getJsonObject(i);
                                        String debutPeriode = periode.getString("timestamp_dt")
                                                .split("T")[0];
                                        String finPeriode = periode.getString("timestamp_fn")
                                                .split("T")[0];

                                        Long _debut =    Long.valueOf(debutPeriode.split("-")[0]);
                                        Long _fin =    Long.valueOf(finPeriode.split("-")[0]);
                                        if (debut == null || _debut < debut){
                                            debut = _debut;
                                        }

                                        if(fin == null || _fin > fin){
                                            fin = _fin;
                                        }
                                    }
                                    if (debut != null && fin != null) {
                                        eleve.put("schoolYear", getLibelle("school.year")
                                                + debut + '-' + fin);
                                    }
                                    serviceResponseOK(answer, finalHandler, count, idEleve, GET_ANNEE_SCOLAIRE_METHOD);
                                }
                            }
                        }));
            }
        }
    }

    public void getAnneeScolaire(String idClasse, Promise<Periode> promise) {

        JsonObject action = new JsonObject();
        action.put(ACTION, "periode.getPeriodes")
                .put("idGroupes", new JsonArray().add(idClasse));

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();
                    JsonArray periodes = body.getJsonArray(RESULT);
                    if (!"ok".equals(body.getString(STATUS))) {
                        promise.fail(body.getString(STATUS));
                    }
                    else {
                        Long start = null;
                        Long end = null;
                        for (int i = 0; i < periodes.size(); i++) {
                            JsonObject periode = periodes.getJsonObject(i);
                            //YYYY-MM-DD
                            String debutPeriode = periode.getString("timestamp_dt")
                                    .split("T")[0];
                            //YYYY-MM-DD
                            String finPeriode = periode.getString("timestamp_fn")
                                    .split("T")[0];

                            Long _debut = Long.valueOf(debutPeriode.split("-")[0]);
                            Long _fin = Long.valueOf(finPeriode.split("-")[0]);
                            if (start == null || _debut < start) {
                                start = _debut;
                            }

                            if(end == null || _fin > end) {
                                end = _fin;
                            }
                        }
                        if (start != null && end != null) {
                            Periode periode =  new Periode();
                            periode.setStartDate(start.toString());
                            periode.setEndDate(end.toString());
                            promise.complete(periode);
                        }
                    }
                }));
    }
    private void getImageGraph(Student student, Promise<Object> promise) {
        String idFile = student.getParamBulletins().getImGraph();
        JsonObject result = new JsonObject();
        if(idFile != null) {
            storage.readFile(idFile, new Handler<Buffer>() {
                private int count = 1;
                private AtomicBoolean answer = new AtomicBoolean(false);

                @Override
                public void handle(Buffer eventBuffer) {
                    if (eventBuffer != null) {
                        String graphPerDomaine = eventBuffer.getString(0, eventBuffer.length());

                        result.put(GRAPH_PER_DOMAINE, graphPerDomaine);
                        promise.complete(result);
                    } else {
                        promise.fail("["+ GET_IMAGE_GRAPH_METHOD + "] : " + student.getId() + " fail " + count);
                    }

                }
            });
        }
        else {
            promise.complete(result);
        }
    }

    @Override
    public void getAvisConseil(Student student,
                               Promise<JsonObject> promise, String beforeAvisConseil) {
        String idEleve = student.getId();
        String idStructure = student.getStructure().getId();
        Long idPeriode = student.getClasse().getPeriode().getIdPeriode();
        avisConseilService.getAvisConseil(idEleve, idPeriode, idStructure, new Handler<Either<String, JsonArray>>() {
            private int count = 1;
            @Override
            public void handle(Either<String, JsonArray> event) {
                if(event.isLeft()){
                    String message = event.left().getValue();
                    if (message.contains(TIME)) {
                        count++;
                        avisConseilService.getAvisConseil(idEleve, idPeriode, idStructure, this);
                    } else {
                        promise.fail("[getAvisConseil ] : " + idEleve  + " " + message + " " + count);
                    }
                } else {
                    JsonArray result = event.right().getValue();
                    JsonObject resultData = new JsonObject().put("avisConseil", "").put("beforeAvisConseil", "").put("hasAvisConseil", false);
                    JsonObject avisConseil = new JsonObject();
                    if(!result.isEmpty())
                        avisConseil = result.getJsonObject(0);
                    if(avisConseil != null && !avisConseil.isEmpty()) {
                        resultData.put("beforeAvisConseil", beforeAvisConseil);

                        resultData.put("avisConseil", avisConseil.getString(LIBELLE))
                                .put("hasAvisConseil", true);
                    }
                    promise.complete(resultData);
                }
            }
        });
    }

    @Override
    public void getAvisOrientation(Student student,
                                   Promise<JsonObject> promise, String beforeAvisOrientation) {
        String idEleve = student.getId();
        String idStructure = student.getStructure().getId();
        Long idPeriode = student.getClasse().getPeriode().getIdPeriode();
        avisOrientationService.getAvisOrientation(idEleve, idPeriode, idStructure, new Handler<Either<String, JsonArray>>() {
            private int count = 1;
            @Override
            public void handle(Either<String, JsonArray> event) {
                if(event.isLeft()){
                    String message = event.left().getValue();
                    if (message.contains(TIME)) {
                        count++;
                        avisOrientationService.getAvisOrientation(idEleve, idPeriode, idStructure,this);
                    }
                    else {
                        promise.fail("[getAvisOrientation ] : " + idEleve  + " " + message + " " + count);
                    }
                }else{
                    JsonArray result = event.right().getValue();
                    JsonObject avisOrientation = new JsonObject();
                    JsonObject resultsJsonObject = new JsonObject().put("avisOrientation","").put("hasAvisOrientation",false).put("beforeAvisOrientation","");
                    if(!result.isEmpty())
                        avisOrientation = result.getJsonObject(0);
                    if(avisOrientation != null && !avisOrientation.isEmpty() ) {
                        //faire un objet?
                        resultsJsonObject.put("avisOrientation",avisOrientation.getString(LIBELLE))
                                .put("hasAvisOrientation",true);

                        resultsJsonObject.put("beforeAvisOrientation", beforeAvisOrientation);
                    }
                    promise.complete(resultsJsonObject);
                }
            }
        });
    }

    private void sethasProject( JsonObject project, boolean value) {

        if(project.getBoolean(HAS_PROJECT) == value) {
            return;
        }
        else if (project.getBoolean(HAS_PROJECT) != null) {
            project.remove(HAS_PROJECT);
        }
        project.put(HAS_PROJECT, value);
    }


    //A decouper
    @Override
    public void getProjets (Student student, Promise<Object> promise) {
        // gets Projects
        String idEleve = student.getId();
        String idClasse = student.getClasse().getId();
        Long idPeriode = student.getClasse().getPeriode().getIdPeriode();
        String idEtablissement = student.getStructure().getId();
        JsonObject result = new JsonObject();
        elementBilanPeriodiqueService.getElementsBilanPeriodique(null,  Arrays.asList(idClasse),
                idEtablissement, new Handler<Either<String, JsonArray>>() {
                    private int count = 1;
                    private AtomicBoolean answer = new AtomicBoolean(false);
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isLeft()) {
                            String message = event.left().getValue();
                            log.error("["+ GET_PROJECTS_METHOD +"] :" + idEleve + " " + message + " " + count);
                            if (message.contains(TIME) && !answer.get()) {
                                count++;
                                elementBilanPeriodiqueService.getElementsBilanPeriodique(null,
                                        Collections.singletonList(idClasse), idEtablissement, this);
                            }
                            else {
                                promise.fail("["+ GET_PROJECTS_METHOD +"] :" + idEleve + " " + message + " " + count);
                            }

                        }
                        else {
                            if (count > 1 ) {
                                log.debug("[getProjets] : " + idEleve + " success " + count);
                            }
                            List<String> idClasses = new ArrayList<>();
                            idClasses.add(idClasse);
                            JsonArray elementBilanPeriodique = event.right().getValue();
                            List<String> idElements = new ArrayList<>();
                            Map<Long, JsonObject> mapElement = new HashMap<>();
                            JsonObject epi = new JsonObject().put(LIBELLE,
                                    getLibelle("enseignements.pratiques.interdisciplinaires"))
                                    .put(HAS_PROJECT, false);
                            JsonObject ap = new JsonObject().put(LIBELLE,
                                    getLibelle("accompagnements.personnalises"))
                                    .put(HAS_PROJECT, false);
                            JsonObject parcours = new JsonObject().put(LIBELLE,
                                    getLibelle("parcours.educatifs"))
                                    .put(HAS_PROJECT, false);

                            if (elementBilanPeriodique == null) {
                                promise.complete(result);
                            }
                            else {
                                generateElementBilanPeriodique(elementBilanPeriodique, idElements, mapElement, epi, ap, parcours, result);
                                if(!idElements.isEmpty()) {
                                    elementBilanPeriodiqueService.getAppreciations(idClasses,
                                            idPeriode.toString(), idElements, idEleve,
                                            new Handler<Either<String, JsonArray>>() {
                                                private int count = 1;
                                                @Override
                                                public void handle(Either<String, JsonArray> event) {
                                                    if (event.isLeft()) {
                                                        String message = event.left().getValue();
                                                        log.error("[getProjets | getAppreciations ] : " +
                                                                idEleve + " " + message + " " + count);
                                                        if (message.contains(TIME) && !answer.get()) {
                                                            count++;
                                                            elementBilanPeriodiqueService
                                                                    .getAppreciations(idClasses,
                                                                            idPeriode.toString(),
                                                                            idElements, idEleve, this);
                                                        }
                                                        else {
                                                            promise.fail("[getProjets | getAppreciations ] : " + idEleve + " " + message + " " + count);
                                                        }
                                                    }
                                                    else {
                                                        JsonArray appreciations = event.right().getValue();
                                                        for(int i=0; i< appreciations.size(); i++) {
                                                            JsonObject app = appreciations.getJsonObject(i);
                                                            Long periodeId = app.getLong(ID_PERIODE);
                                                            if(periodeId.equals(idPeriode)) {
                                                                String com = app.getString("commentaire");

                                                                Long idElem = app.getLong(
                                                                        "id_elt_bilan_periodique");
                                                                JsonObject elem = mapElement.get(idElem);
                                                                elem.remove("hasCommentaire");

                                                                elem.put("hasCommentaire", true)
                                                                        .put("commentaire",
                                                                                troncateLibelle(com,
                                                                                        MAX_SIZE_LIBELLE_PROJECT))
                                                                        .put("commentaireStyle",
                                                                                fontSizeProject(com,
                                                                                        MAX_SIZE_LIBELLE_PROJECT));
                                                                Long typeElem = elem.getLong("type");
                                                                if (3L == typeElem) {
                                                                    sethasProject(parcours,true);
                                                                }
                                                                else if (2L == typeElem) {
                                                                    sethasProject(ap,true);
                                                                }
                                                                else if (1L == typeElem) {
                                                                    sethasProject(epi,true);
                                                                }
                                                            }
                                                        }
                                                        promise.complete(result);
                                                    }
                                                }
                                            });
                                }
                                else {
                                    log.debug("[Competences:@getProjets] | NO elements founds for classe " + idClasse);
                                    promise.complete(result);
                                }
                            }

                        }
                    }
                });
    }

    private void generateElementBilanPeriodique(JsonArray elementBilanPeriodique, List<String> idElements, Map<Long, JsonObject> mapElement,
                                                JsonObject epi, JsonObject ap, JsonObject parcours, JsonObject result) {
        for(int i = 0; i< elementBilanPeriodique.size(); i++) {
            JsonObject element = elementBilanPeriodique.getJsonObject(i);
            if (element != null) {
                Long idElement = element.getLong("id");
                Long typeElement = element.getLong("type");
                idElements.add(idElement.toString());
                element.put("hasCommentaire", false);
                mapElement.put(idElement, element);
                if (3L == typeElement) {
                    element.put("hasLibelle", false);
                    if(parcours.getJsonArray("elements") == null) {
                        parcours.put("elements", new JsonArray().add(element));
                    }
                    else {
                        parcours.getJsonArray("elements").add(element);
                    }
                } else if (2L == typeElement) {
                    element.put("hasLibelle", true);
                    if(ap.getJsonArray("elements") == null) {
                        ap.put("elements", new JsonArray().add(element));
                    }
                    else {
                        ap.getJsonArray("elements").add(element);
                    }
                } else if (1L == typeElement) {
                    element.put("hasLibelle", true);
                    if(epi.getJsonArray("elements") == null) {
                        epi.put("elements", new JsonArray().add(element));
                    }
                    else {
                        epi.getJsonArray("elements").add(element);
                    }
                }
            }
        }
        result.put("projects", new JsonArray().add(epi).add(ap).add(parcours));
    }

    @Override
    public void getSyntheseBilanPeriodique (Student student,
                                            Boolean isBulletinLycee, Promise<JsonObject> promise) {
        String idEleve = student.getId();
        Long idPeriode = student.getClasse().getPeriode().getIdPeriode();
        String idStructure = student.getStructure().getId();

        syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(idPeriode, idEleve, idStructure,
                new Handler<Either<String, JsonArray>>() {
                    private int count = 1;
                    private AtomicBoolean answer = new AtomicBoolean(false);
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if(event.isLeft()){
                            String message = event.left().getValue();
                            log.error("[getSyntheseBilanPeriodique ] : " + idEleve  + " " + message + " " + count);
                            if (message.contains(TIME) && !answer.get()) {
                                count++;
                                syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(idPeriode, idEleve, idStructure,
                                        this);
                            }
                            else {
                                promise.fail(GET_SYNTHESE_BILAN_PERIO_METHOD + " " + event.left().getValue());
                            }
                        }
                        else {
                            JsonObject results = new JsonObject();
                            JsonArray result = event.right().getValue();
                            JsonObject synthese = new JsonObject();
                            if(!result.isEmpty())
                                synthese = result.getJsonObject(0);
                            if (synthese != null && !synthese.isEmpty()) {
                                String syntheseStr = synthese.getString("synthese");
                                results.put("syntheseBilanPeriodque", troncateLibelle(syntheseStr,
                                        MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE));
                                results.put("syntheseBilanPeriodqueStyle", fontSize(syntheseStr,
                                        MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE));
                                if(Boolean.TRUE.equals(isBulletinLycee)){
                                    results.put("beforeSyntheseBP", BEFORE_SYNTHESE_BP);
                                }
                            }
                            promise.complete(results);
                        }
                    }
                });

    }

    public void getAppreciationCPE(Student student,
                                   Promise<JsonObject> promise){
        Long idPeriode = student.getClasse().getPeriode().getIdPeriode();
        String idEleve = student.getId();

        appreciationCPEService.getAppreciationCPE(idPeriode, idEleve, new Handler<Either<String, JsonObject>>() {
            private int count = 1;
            private AtomicBoolean answer = new AtomicBoolean(false);
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isLeft()) {
                    String message = " " + event.left().getValue();
                    if (message.contains(TIME) && !answer.get()) {
                        count++;
                        appreciationCPEService.getAppreciationCPE(idPeriode, idEleve, this);
                    } else {
                        promise.fail("[" + GET_APPRECIATION_CPE_METHOD + "] : " + idEleve + " " + message + " " + count);
                    }
                } else {
                    JsonObject appreciationCPE = event.right().getValue();
                    JsonObject results = new JsonObject();
                    if (appreciationCPE != null) {
                        String app = troncateLibelle(appreciationCPE.getString(APPRECIATION_KEY),
                                MAX_SIZE_APPRECIATION_CPE);

                        results.put("appreciationCPE", app)
                                .put("appreciationCPEStyle", fontSizeAppreciationCPE(app, MAX_SIZE_APPRECIATION_CPE));
                    }
                    promise.complete(results);
                }
            }
        });
    }
    @Override
    public void getStructure( String idEleve, JsonObject eleveObject,
                              Handler<Either<String, JsonObject>> finalHandler) {
        if (eleveObject == null) {
            logStudentNotFound(idEleve,"getStructure");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            JsonObject action = new JsonObject();
            String idStructure = eleveObject.getString(ID_ETABLISSEMENT_KEY);
            action.put(ACTION, "structure.getStructure").put("idStructure", idStructure);
            eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                    handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                        private int count = 1;
                        private AtomicBoolean answer = new AtomicBoolean(false);
                        @Override
                        public void handle(Message<JsonObject> message) {
                            JsonObject body = message.body();
                            if (!"ok".equals(body.getString(STATUS))) {
                                String mess = body.getString(MESSAGE);
                                log.error("["+ GET_STRUCTURE_METHOD + "] : " + idEleve + " " + mess + " " + count);

                                if (mess.contains(TIME) && !answer.get()) {
                                    count++;
                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                            Competences.DELIVERY_OPTIONS,
                                            handlerToAsyncHandler(this));
                                }
                                else {
                                    if (eleveObject.getJsonArray(ERROR) == null) {
                                        eleveObject.put(ERROR, new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray(ERROR);
                                    errors.add(GET_STRUCTURE_METHOD);
                                    serviceResponseOK(answer, finalHandler, count, idEleve, GET_STRUCTURE_METHOD);
                                }
                            } else {
                                JsonObject structure = body.getJsonObject(RESULT);
                                JsonArray structureLibelle = new JsonArray();
                                if(structure != null){
                                    structure = structure.getJsonObject("s");
                                    if(structure != null) {
                                        structure = structure.getJsonObject("data");
                                        String academy = structure.getString("academy");
                                        String type = structure.getString("type");
                                        String name = structure.getString(NAME);
                                        String address = structure.getString("address");
                                        String codePostal = structure.getString("zipCode");
                                        String phone = structure.getString("phone");
                                        String email = structure.getString("email");
                                        String city =  structure.getString("city");
                                        if(academy != null) {
                                            structureLibelle.add(new JsonObject().put("academy", academy));}
                                        if(type != null) {
                                            structureLibelle.add(new JsonObject().put("type", type));}
                                        if(name != null) {
                                            structureLibelle.add(new JsonObject().put(NAME_STRUCTURE,name));
                                            eleveObject.put(STRUCTURE, name);
                                        }
                                        if(address != null) {
                                            structureLibelle.add(new JsonObject().put("address", address));}
                                        String town = null;
                                        if(codePostal != null) {
                                            town = codePostal;
                                        }
                                        if(city != null) {
                                            town = (town!=null)? (town + ' ' + city) : city;
                                        }
                                        if (town!= null) {
                                            structureLibelle.add(new JsonObject().put("town",town));
                                        }
                                        if(phone != null) {structureLibelle.add(new JsonObject().put("phone", phone));}
                                        if(email != null) {
                                            structureLibelle.add(new JsonObject().put("couriel", email));}
                                    }
                                }
                                eleveObject.put(STRUCTURE_LIBELLE, structureLibelle);
                                serviceResponseOK(answer, finalHandler, count, idEleve, GET_STRUCTURE_METHOD);
                            }
                        }
                    }));
        }
    }

    public void getStructure( String idStructure,
                              Handler<Either<String, Structure>> finalHandler) {

        JsonObject action = new JsonObject();
        Structure structure = new Structure();
        action.put(ACTION, "structure.getStructure").put("idStructure", idStructure);
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();
                    JsonObject structureJO = body.getJsonObject(RESULT);
                    if(structureJO != null){
                        structureJO = structureJO.getJsonObject("s");
                        if(structureJO != null) {
                            structureJO = structureJO.getJsonObject("data");

                            structure.setAcademy( structureJO.getString("academy"));
                            structure.setType(structureJO.getString("type"));
                            structure.setName( structureJO.getString(NAME));
                            structure.setAddress(structureJO.getString("address"));
                            structure.setZipCode(structureJO.getString("zipCode"));
                            structure.setPhone(structureJO.getString("phone"));
                            structure.setMail( structureJO.getString("email"));
                            structure.setCity( structureJO.getString("city"));

                        }
                    }
                    finalHandler.handle(new Either.Right<>(structure));
                }));
    }


    public void getStructure(String idStructure,
                             Promise<Structure> promise) {

        JsonObject action = new JsonObject();
        Structure structure = new Structure();
        action.put(ACTION, "structure.getStructure").put("idStructure", idStructure);
        //TODO FAIRE LE FAIL
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();
                    JsonObject structureJO = body.getJsonObject(RESULT);
                    if(structureJO != null){
                        structureJO = structureJO.getJsonObject("s");
                        if(structureJO != null) {
                            structureJO = structureJO.getJsonObject("data");
                            structure.setAcademy( structureJO.getString("academy"));
                            structure.setType(structureJO.getString("type"));
                            structure.setName( structureJO.getString(NAME));
                            structure.setAddress(structureJO.getString("address"));
                            structure.setZipCode(structureJO.getString("zipCode"));
                            structure.setPhone(structureJO.getString("phone"));
                            structure.setMail( structureJO.getString("email"));
                            structure.setCity( structureJO.getString("city"));

                        }
                    }
                    promise.complete(structure);
                }));
    }
    @Override
    public void getHeadTeachers(String idEleve, String idClasse, JsonObject eleveObject,
                                Handler<Either<String, JsonObject>> finalHandler) {
        logBegin(GET_HEAD_TEACHERS_METHOD, idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_HEAD_TEACHERS_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        } else {
            JsonObject action = new JsonObject();
            if (idClasse == null) {
                logidClasseNotFound(idEleve, GET_HEAD_TEACHERS_METHOD);
                finalHandler.handle(new Either.Right<>(null));
            } else {
                action.put(ACTION, "classe.getHeadTeachersClasse")
                        .put(ID_CLASSE, idClasse);

                eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                        handlerToAsyncHandler( new Handler<Message<JsonObject>>() {
                            private int count = 1;
                            private AtomicBoolean answer = new AtomicBoolean(false);
                            @Override
                            public void handle(Message<JsonObject> message) {
                                JsonObject body = message.body();
                                if (!"ok".equals(body.getString(STATUS))) {
                                    String mess = body.getString(MESSAGE);
                                    log.error("[" + GET_HEAD_TEACHERS_METHOD + "] : " + idEleve + " "
                                            + mess + " " + count);

                                    buildErrorReponseForEb(idEleve, mess, answer, count, action,
                                            this, finalHandler, eleveObject,
                                            GET_HEAD_TEACHERS_METHOD);
                                } else {
                                    JsonArray res = body.getJsonArray(RESULTS);

                                    if (res != null) {
                                        JsonArray headTeachers = new JsonArray();
                                        for(int i=0; i < res.size(); i++){
                                            JsonObject headTeacher = res.getJsonObject(i);
                                            String firstName = headTeacher.getString(FIRST_NAME_KEY);
                                            String initial = "";

                                            if(firstName != null && firstName.length() > 0) {
                                                initial = firstName.charAt(0) + ". ";
                                            }
                                            headTeacher.put("initial", initial);

                                            if(!headTeachers.contains(headTeacher)){
                                                headTeachers.add(headTeacher);
                                            }
                                        }
                                        String headTeachersLibelle = getLibelle(
                                                (headTeachers.size() > 1) ? "headTeachers" : "headTeacher");
                                        eleveObject.put("headTeacherLibelle", headTeachersLibelle + " : ")
                                                .put("headTeachers", headTeachers);
                                    }
                                    serviceResponseOK(answer, finalHandler, count, idEleve, GET_HEAD_TEACHERS_METHOD);
                                }
                            }
                        }));
            }
        }
    }


    public void getHeadTeachers(String idClasse,
                                Promise<Object> promise) {

        JsonObject action = new JsonObject();

        action.put(ACTION, "classe.getHeadTeachersClasse")
                .put(ID_CLASSE, idClasse);

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler( new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        JsonArray res = body.getJsonArray(RESULTS);
                        JsonObject result = new JsonObject();
                        if (res != null) {
                            JsonArray headTeachers = new JsonArray();
                            for(int i=0; i < res.size(); i++) {
                                JsonObject headTeacher = res.getJsonObject(i);
                                String firstName = headTeacher.getString(FIRST_NAME_KEY);
                                String initial = "";

                                if(firstName != null && firstName.length() > 0) {
                                    initial = firstName.charAt(0) + ". ";
                                }
                                headTeacher.put("initial", initial);

                                if(!headTeachers.contains(headTeacher)) {
                                    headTeachers.add(headTeacher);
                                }
                            }
                            String headTeachersLibelle = getLibelle(
                                    (headTeachers.size() > 1) ? "headTeachers" : "headTeacher");
                            result.put("headTeacherLibelle", headTeachersLibelle + " : ")
                                    .put("headTeachers", headTeachers);
                        }
                        promise.complete(result);
                    }
                }));
    }

    @Override
    public void getResponsables( Student student, Promise promise) {
        String idEleve = student.getId();
        JsonObject action = new JsonObject();
        action.put(ACTION, "eleve.getResponsables")
                .put(ID_ELEVE_KEY, idEleve);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    private int count = 1;
                    private AtomicBoolean answer = new AtomicBoolean(false);
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        if (!"ok".equals(body.getString(STATUS))) {
                            String mess = body.getString(MESSAGE);
                            promise.fail("[" + GET_RESPONSABLE_METHOD + "] : " + idEleve + " " + mess + " " + count);
                        } else {
                            JsonObject result = new JsonObject();
                            JsonArray responsables = body.getJsonArray(RESULTS);
                            result.put("responsables", responsables);
                            promise.complete(result);
                        }
                    }
                }));
    }

    @Override
    public void getEvenements(Student student, Promise<List<StudentEvenement>> promise) {
        logBegin(GET_EVENEMENT_METHOD, student.getId());
        String idEleve = student.getId();
        String idStructure = student.getStructure().getId();
        String idClasse = student.getClasse().getId();
        Long idPeriode = student.getClasse().getPeriode().getIdPeriode();
        bilanPeriodiqueService.getRetardsAndAbsences(idStructure, Collections.singletonList(idEleve),
                Collections.singletonList(idClasse), new Handler<Either<String, JsonArray>>() {
                    private AtomicBoolean answer = new AtomicBoolean(false);

                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isLeft()) {
                            String message = event.left().getValue();

                            if (message.contains(TIME) && !answer.get()) {
                                bilanPeriodiqueService.getRetardsAndAbsences(idStructure, Collections.singletonList(idEleve),
                                        Collections.singletonList(idClasse), this);
                            } else {
                                promise.fail(GET_EVENEMENT_METHOD + " " + event.left().getValue());
                            }
                        } else {
                            JsonArray evenements = event.right().getValue();

                            Long absTotaleHeure = 0L;
                            Long absNonJust = 0L;
                            Long absJust = 0L;
                            Long retard = 0L;

                            for (int i = 0; i < evenements.size(); i++) {
                                JsonObject ev = evenements.getJsonObject(i);
                                Long evAbsTotH = ev.getLong("abs_totale_heure");
                                Long evAbsNonJust = ev.getLong("abs_non_just");
                                Long evAbsJust = ev.getLong("abs_just");
                                Long evRetard = ev.getLong("retard");

                                if (ev.getLong(ID_PERIODE) == idPeriode || idPeriode == null) {
                                    absTotaleHeure += ((evAbsTotH != null) ? evAbsTotH : 0L);
                                    absNonJust += ((evAbsNonJust != null) ? evAbsNonJust : 0L);
                                    absJust += ((evAbsJust != null) ? evAbsJust : 0L);
                                    retard += ((evRetard != null) ? evRetard : 0L);
                                }
                            }
                            StudentEvenement retardEvenement = new StudentEvenement(getLibelle("viescolaire.retards"),retard);
                            StudentEvenement absJustEvenement = new StudentEvenement(getLibelle("evaluations.export.bulletin.asbence.just"),absJust,getLibelle("half.days"));
                            StudentEvenement absNotJustEvenement = new StudentEvenement(getLibelle("evaluations.export.bulletin.asbence.not.just"), absNonJust,getLibelle("half.days"));
                            StudentEvenement absTotalEvenement = new StudentEvenement(getLibelle("evaluations.export.bulletin.asbence.nb.heures"),absTotaleHeure,getLibelle("hours"));
                            List<StudentEvenement> listEvenements = new ArrayList<>();
                            listEvenements.add(retardEvenement);
                            listEvenements.add(absJustEvenement);
                            listEvenements.add(absNotJustEvenement);
                            listEvenements.add(absTotalEvenement);
                            promise.complete(listEvenements);
                        }
                    }
                });

    }

    private String troncateLibelle(String libelle, int max) {
        if(libelle == null) {
            libelle = "";
        } else if (libelle.length() > max) {
            libelle = libelle.substring(0, max);
            libelle += "...";
        }
        return libelle;
    }

    private String fontSize(String libelle, int max) {
        if (libelle == null) {
            return "";
        } else if (libelle.length() < max / 2) {
            return "font-size: 10px !important;";
        } else if (libelle.length() <= max) {
            return "font-size: 8.5px !important;";
        }
        return "";
    }

    private String fontSizeAppreciationCPE(String libelle, int max) {
        String size = "";
        if (libelle != null) {
            if (libelle.length() < max / 3) {
                size = "font-size: small !important;";
            } else if (libelle.length() <= max) {
                size = "font-size: x-small !important;";
            }
        }
        return size;
    }

    private String fontSizeProject(String libelle, int max) {
        if(libelle == null) {
            return  "";
        } else if (libelle.length() < max/2) {
            return "font-size: 10px !important;";
        } else if (libelle.length() <= max) {
            return "font-size: 7.5px !important;";
        }
        return "";
    }

    // La taille de la police varie en fonction du nombre de matières affichées, du fait qu'on affiche la colonne des
    // éléments du programme et aussi du nombre de caractères de l'appréciation ou du libellé des éléments du programme
    private void setFontSizeOfSuivi(JsonArray subjects, boolean withProgramElement) {
        String value;
        JsonObject fontstyle = new JsonObject();

        int nbSubjectOnLimit = 0;
        int nbSubject = subjects.size();
        for (int i=0; i < nbSubject; i++) {
            JsonObject subject = subjects.getJsonObject(i);
            subject.put("display", fontstyle);

            int maxCaractere;
            String elementsProgramme = troncateLibelle(subject.getString(ELEMENTS_PROGRAMME), MAX_SIZE_LIBELLE);
            String appreciation =  troncateLibelle(subject.getString(APPRECIATION_KEY), MAX_SIZE_LIBELLE);

            if (withProgramElement) {
                maxCaractere = Math.max(elementsProgramme.length(), appreciation.length());
            } else {
                maxCaractere = appreciation.length();
            }
            if (maxCaractere > (MAX_SIZE_LIBELLE /2 + 24)) {
                nbSubjectOnLimit++;
            }
        }

        int nbSubjectUnderLimit = nbSubject - nbSubjectOnLimit;

        if ((nbSubjectOnLimit <= 6 && nbSubject <= 6)
                || (nbSubject <= 6 && withProgramElement)
                || (!withProgramElement && nbSubjectOnLimit <= 10)) {
            value = "font-size: auto !important;";
        } else {
            if (7 == nbSubject
                    || (nbSubject <= 11 && nbSubjectUnderLimit >= nbSubject -1)) {
                value = "font-size: 11px !important;";
            } else {
                if (8 == nbSubject) {
                    value = "font-size: 10.124px !important;";
                } else {
                    if (9 == nbSubject
                            || nbSubject <= 13 && nbSubjectOnLimit <= 1) {
                        value = "font-size: 10px !important;";
                    } else {
                        if (nbSubject <= 13 && nbSubjectOnLimit <= 2) {
                            value = "font-size: 9.5px !important;";
                        } else {
                            if (10 == nbSubject) {
                                value = "font-size: 8.7px !important;";
                            } else {
                                value = "font-size: 8.5px !important;";
                            }
                        }
                    }
                }
            }
        }
        fontstyle.put("style", value);
    }

    private void setHeightByRow(JsonArray subjects) {
        int nbSubject = subjects.size();
        int sizeOfTable = 580; // Taille en pixel du tableau de suivi des acquis
        if(nbSubject > 0 && subjects.getJsonObject(0).getBoolean(GET_POSITIONNEMENT)) {
            sizeOfTable -= 20;
        }
        for (int i = 0; i < nbSubject; i++) {
            JsonObject subject = subjects.getJsonObject(i);
            subject.put("heightByRow", (sizeOfTable / nbSubject) + "px");
        }
    }

    private void setMoyenneAnnuelle(JsonObject eleveObject, JsonArray matieres, JsonObject params){
        if(isNull(matieres) || isNull(eleveObject) || isNull(params)){
            log.error("setMoyenneAnnuelle call with null Object ");
            return;
        }
        Double moy = new Double(0);
        int sumCoef = 0;
        for(int i=0; i < matieres.size(); i++){
            JsonObject matiere = matieres.getJsonObject(i);
            Object coefMatiere = matiere.getValue("coef", "1");

            final Double[] moyMatiere = {0.0};
            JsonArray moyennes = matiere.getJsonArray(MOYENNES);
            JsonArray moyennesFinales = matiere.getJsonArray("moyennesFinales");
            ArrayList<Long> periodes = new ArrayList<>();
            moyennesFinales.forEach(moyenneFinale -> {
                JsonObject moyenneFinaleJson = (JsonObject) moyenneFinale;
                Long periode = moyenneFinaleJson.getLong("id_periode");
                if(periode != null && !periodes.contains(periode)){
                    if(!moyenneFinaleJson.getValue("moyenneFinale").equals(NN)){
                        moyMatiere[0] += moyenneFinaleJson.getDouble("moyenneFinale");
                        periodes.add(periode);
                    }
                }
            });
            moyennes.forEach(moyenne -> {
                JsonObject moyenneJson = (JsonObject) moyenne;
                Long periode = moyenneJson.getLong("id");
                if(periode != null && !periodes.contains(periode)){
                    if(!moyenneJson.getValue("moyenne").equals(NN)){
                        moyMatiere[0] += moyenneJson.getDouble("moyenne");
                        periodes.add(periode);
                    }
                }
            });

            moyMatiere[0] = moyMatiere[0] / periodes.size();

            if(isNotNull(moyMatiere[0]) && isNotNull(coefMatiere) && periodes.size() > 0){
                coefMatiere = Integer.valueOf(coefMatiere.toString());
                sumCoef += (int) coefMatiere;
                moy += ((int)coefMatiere * moyMatiere[0]);
            }
        }
        DecimalFormat decimalFormat = new DecimalFormat("#.0");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);//with this mode 2.125 -> 2.13 without 2.125 -> 2.12
        Object moyenAnnuelle = (sumCoef > 0) ? decimalFormat.format(moy / sumCoef) : NN;

        eleveObject.put(PRINT_MOYENNE_ANNUELLE, params.getBoolean(MOYENNE_ANNUELLE));
        eleveObject.put(MOYENNE_ANNUELLE, moyenAnnuelle);
    }

    private void setMoyenneGenerale(JsonObject eleveObject, JsonArray matieres, JsonObject params,
                                    Long idPeriode, String idEl, JsonArray idEleves){
        Map<String, Double> moyMap = new HashMap<>();
        Map<String, Integer> sumCoefMap = new HashMap<>();

        Double moy = new Double(0);
        int sumCoef = 0;

        Double moyClass = new Double(0);
        int nbEleveClass = 0;

        for(int i=0; i < matieres.size(); i++){
            JsonObject matiere = matieres.getJsonObject(i);

            Object coefMatiere = matiere.getValue("coef", "1");
            JsonObject moyenneMapPeriode = matiere.getJsonObject(NOTES_BY_PERIODE_BY_STUDENT);
            // Calcul de la moyenne générale de chacun des élèves évalué sur la matière
            if(isNotNull(moyenneMapPeriode) && isNotNull(idPeriode) && moyenneMapPeriode.containsKey(idPeriode.toString())){
                JsonObject matiereMoyenne = moyenneMapPeriode.getJsonObject(idPeriode.toString());
                for(Map.Entry<String, Object> moyenneEleve : matiereMoyenne.getMap().entrySet()){
                    String idEleve = moyenneEleve.getKey();
                    Double moyEleve = (Double) moyenneEleve.getValue();
                    if(!moyMap.containsKey(idEleve)){
                        moyMap.put(idEleve, new Double(0));
                        sumCoefMap.put(idEleve, 0);
                    }
                    int coefInt = Integer.valueOf(coefMatiere.toString()).intValue();
                    moyMap.put(idEleve, moyMap.get(idEleve) + (moyEleve * coefInt));
                    sumCoefMap.put(idEleve, sumCoefMap.get(idEleve) + coefInt);
                }
            }

            // Calcul de la moyenne générale de l'élève
            Object moyMatiere = matiere.getValue(MOYENNE_ELEVE);
            if(isNotNull(moyMatiere) && isNotNull(coefMatiere) && !moyMatiere.equals(NN)){
                coefMatiere = Integer.valueOf(coefMatiere.toString());
                sumCoef += (int) coefMatiere;
                moy += ((int)coefMatiere * Double.valueOf(moyMatiere.toString()));
            }
        }

        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse = new HashMap<>();
        notesByDevoirByPeriodeClasse.put(idPeriode, new HashMap<>());
        notesByDevoirByPeriodeClasse.get(idPeriode).put(idPeriode, new ArrayList<>());
        for(Map.Entry<String, Double> moyenneEleve : moyMap.entrySet()){
            String idEleve = moyenneEleve.getKey();
            int sumCo = sumCoefMap.get(idEleve);
            Double moyen = moyenneEleve.getValue();
            if(sumCo > 0 && idEleves.contains(idEleve)) {
                Double moyCuEl = moyen / sumCo;
                NoteDevoir noteEleve = new NoteDevoir(moyCuEl, 20.0, false, 1.0, idEleve);
                notesByDevoirByPeriodeClasse.get(idPeriode).get(idPeriode).add(noteEleve);

                // Calcul de la moyenne Génerale de la classe
                moyClass += moyCuEl;
                nbEleveClass++;
            }
        }

        eleveObject.put(MOYENNE_GENERALE + "Obj", new JsonObject());
        noteService.setRankAndMinMaxInClasseByPeriode(idPeriode, idEl, notesByDevoirByPeriodeClasse, new JsonArray(),
                eleveObject.getJsonObject(MOYENNE_GENERALE + "Obj"));
        eleveObject.put(PRINT_MOYENNE_GENERALE, params.getBoolean(MOYENNE_GENERALE));

        DecimalFormat decimalFormat = new DecimalFormat("#.0");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);//with this mode 2.125 -> 2.13 without 2.125 -> 2.12
        Object moyenGeneral = (sumCoef > 0) ? decimalFormat.format(moy / sumCoef) : NN;
        Object moyenGeneralClass = (nbEleveClass > 0) ? decimalFormat.format(moyClass / nbEleveClass) : NN;

        eleveObject.put(MOYENNE_GENERALE, moyenGeneral);
        eleveObject.put(MOYENNE_GENERALE + "Class", moyenGeneralClass);

        if(params.getBoolean(NEUTRE, false)){
            eleveObject.put(BACKGROUND_COLOR, "#ffffff");
        } else{
            eleveObject.put(BACKGROUND_COLOR, (matieres.size()%2 == 0) ? "#E2F0FA" : "#EFF7FC");
        }
    }

    @Override
    public void getSuiviAcquis(Student student, JsonArray idEleves, JsonObject classe,
                               JsonObject params, Promise<JsonObject> promise) {
        boolean getProgrammeElement = params.getBoolean(GET_PROGRAM_ELEMENT);
        String idEleve = student.getId();
        Long idPeriode = student.getClasse().getPeriode().getIdPeriode();
        String idEtablissement = student.getStructure().getId();
        String idClasse = student.getClasse().getId();
        if (idClasse == null || idEtablissement == null) {
            if(idClasse == null) {
                logidClasseNotFound(idEleve, GET_SUIVI_ACQUIS_METHOD);
                promise.fail("[getSuiviAcquis] Classe Not found");
            }
            if (idEtablissement == null) {
                promise.fail("[getSuiviAcquis] Structure Not found");
                logidEtabNotFound(idEleve, GET_SUIVI_ACQUIS_METHOD);
            }
        } else {
            Utils.getGroupesClasse(eb, new JsonArray().add(idClasse), responseGroupsClass -> {
                if(responseGroupsClass.isLeft()) {
                    String error = responseGroupsClass.left().getValue();
                    promise.fail("[Competence] DefaultExportBulletinService at getSuiviAcquis : getGroupesClasse " + error);
                } else {
                    JsonArray groupsClassResult = responseGroupsClass.right().getValue();
                    JsonArray idGroupClasse = new JsonArray()
                            .add(idClasse);
                    if(groupsClassResult != null && !groupsClassResult.isEmpty()){
                        idGroupClasse.addAll(groupsClassResult.getJsonObject(0).getJsonArray("id_groupes"));
                    }
                    JsonArray servicesJson = new JsonArray(student.getClasse().getServices().stream().map(Service::toJson).collect(Collectors.toList()));
                    JsonArray multiTeachers = new JsonArray(student.getClasse().getMultiTeachers().stream().map(MultiTeaching::toJsonObject).collect(Collectors.toList()));
                    List<Service> services = student.getClasse().getServices();

                    bilanPeriodiqueService.getSuiviAcquis(idEtablissement, idPeriode, idEleve,
                            idGroupClasse, servicesJson, multiTeachers,
                            getSuiviAcquisHandler(student, params, promise,classe,idEleves,
                                    getProgrammeElement,  idGroupClasse, services, multiTeachers));
                }
            });
        }
    }

    //passer en param la map de students + fonction vieille
    private Handler<Either<String, JsonArray>>
    getSuiviAcquisHandler(Student student, JsonObject params,
                          Promise<JsonObject> promise, JsonObject classe, JsonArray idEleves, boolean getProgrammeElement,
                          JsonArray idGroupClasse,
                          List<Service> services, JsonArray multiTeachers) {
        return new Handler<Either<String, JsonArray>>() {
            private int count = 1;
            private AtomicBoolean answer = new AtomicBoolean(false);
            final String idEtablissement = student.getStructure().getId();
            final String idEleve = student.getId();
            final Long idPeriode = student.getClasse().getPeriode().getIdPeriode();
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isLeft()) { //si erreur
                    String message = event.left().getValue();

                    if (message.contains(TIME) && !answer.get())
                    {
                        count ++;
                        JsonArray servicesJson = new JsonArray(services.stream().map(Service::toJson).collect(Collectors.toList()));
                        bilanPeriodiqueService.getSuiviAcquis(idEtablissement, idPeriode, idEleve, idGroupClasse,
                                servicesJson, multiTeachers,this);
                    } else {
                        promise.fail("["+ GET_SUIVI_ACQUIS_METHOD + "] :" + idEleve + " " + message + count);
                    }
                } else {
                    JsonArray suiviAcquis = event.right().getValue();
                    JsonArray res = new JsonArray();
                    JsonObject result = new JsonObject();

                    if(suiviAcquis != null){
                        // On considèrera qu'on a un suivi des acquis si on affiche au moins une matière
                        for (int i = 0; i < suiviAcquis.size() ; i++) {
                            final JsonObject matiereJO = suiviAcquis.getJsonObject(i);

                            Matiere matiere = new Matiere();
                            if(Boolean.TRUE.equals(params.getBoolean(NEUTRE, false))){
                                result.put(BACKGROUND_COLOR, "#ffffff");
                                matiere.setBackgroundColor("#ffffff");
                                matiereJO.put(BACKGROUND_COLOR, "#ffffff");
                            } else{
                                matiere.setBackgroundColor( (res.size()%2 == 0) ? "#E2F0FA" : "#EFF7FC");
                                matiereJO.put(BACKGROUND_COLOR, (res.size()%2 == 0) ? "#E2F0FA" : "#EFF7FC");
                            }
                            // Une matière sera affichée si on a au moins un élement sur la période

                            buildMatiereForSuiviAcquis(matiereJO,matiere, idPeriode, classe, params,services);
                            checkCoefficientConflict(result, matiereJO.getJsonObject(COEFFICIENT), params);
                            if(Boolean.TRUE.equals(matiereJO.getBoolean(PRINT_MATIERE_KEY))) {
                                res.add(matiereJO);
                            }
                        }
                        setFontSizeOfSuivi(res, getProgrammeElement);
                        setHeightByRow(res);

                        setMoyenneGenerale(result, suiviAcquis, params, idPeriode, idEleve, idEleves);
                        setMoyenneAnnuelle(result, suiviAcquis, params);
                    }

                    result.put("suiviAcquis", res).put("hasSuiviAcquis", res.size() > 0);
                    promise.complete(result);
                }
            }
        };
    }

    @Override
    public void getArbreDomaines(Student student, Promise<Object> promise){
        String idClasse = student.getClasse().getId();
        String idEleve = student.getId();
        JsonObject result = new JsonObject();

        domainesService.getArbreDomaines(idClasse, idEleve, null,
                new Handler<Either<String, JsonArray>>() {
                    private AtomicBoolean answer = new AtomicBoolean(false);
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isLeft()) {
                            String message = event.left().getValue();
                            if (message.contains(TIME) && !answer.get()) {
                                domainesService.getArbreDomaines(idClasse, idEleve, null, this);
                            } else {
                                promise.fail("Error when handling getArbreDomaine  ");
                            }
                        } else {
                            JsonArray domaines = event.right().getValue();
                            JsonArray domainesToDisplay = new JsonArray();
                            Map<Long,Boolean> idDomaineParent = new HashMap<>();
                            for (int i = 0; i < domaines.size(); i++) {
                                JsonObject domaine = domaines.getJsonObject(i);
                                Boolean isEvaluable = domaine.getBoolean(EVALUATED);
                                Long idParent = domaine.getLong(ID_PARENT);
                                boolean isDomaineParent = Boolean.TRUE.equals(idParent == 0L);
                                domaine.put(IS_DOMAINE_PARENT, isDomaineParent);

                                if (isDomaineParent) {
                                    idDomaineParent.put(domaine.getLong(ID), isEvaluable);
                                }
                                if (isEvaluable) {
                                    if (isDomaineParent) {
                                        domainesToDisplay.add(domaine);
                                    }
                                    else if (Boolean.FALSE.equals(idDomaineParent.get(idParent))) {
                                        domainesToDisplay.add(domaine);
                                    }
                                }
                            }
                            result.put("domaines", domainesToDisplay);
                            promise.complete(result);
                        }
                    }
                });
    }

    private JsonObject getObjectForPeriode(JsonArray array, Long idPeriode, String key) {
        return utilsService.getObjectForPeriode(array, idPeriode, key);
    }

    private void setLibelleMatiere(JsonObject matiereJO, Matiere matiere, JsonArray models){
        String idMatiere = matiereJO.getString(ID_MATIERE);
        matiere.setId(idMatiere);

        for (int i=0; i<models.size(); i++) {
            JsonObject libelleSubject = models.getJsonObject(i);
            String id = libelleSubject.getString("id");
            if (idMatiere.equals(id)){
                String libelleMatiere = libelleSubject.getString(LIBELLE);
                matiereJO.remove(LIBELLE_MATIERE);
                matiere.setLibelle(libelleMatiere);
                matiereJO.put(LIBELLE_MATIERE, libelleMatiere);
            }
        }
    }

    private Float getMoyenneForSousMat(Object object, Long idPeriode, Long idSousMat){
        Float res = null;
        if (isNotNull(object) && isNotNull(idPeriode)) {
            object = ((JsonObject)object).getJsonObject(idPeriode.toString());
            if (isNotNull(object) && isNotNull(idSousMat)) {
                object  = ((JsonObject)object).getValue(idSousMat.toString());
                if (isNotNull(object)) {
                    if(object instanceof Double){
                        res = ((Double) object).floatValue();
                    }
                    else {
                        Boolean hasNote = ((JsonObject)object).getBoolean("hasNote");
                        if(isNotNull(hasNote) && hasNote) {
                            res = ((JsonObject) object).getFloat(MOYENNE);
                        }
                    }

                }
            }
        }
        return res;
    }

    private void buildSousMatieres(JsonObject matiere, JsonArray tableauDeConversion, Long idPeriode, JsonObject params, List<Service> services){
        JsonArray sousMatiere = matiere.getJsonArray(SOUS_MATIERES);
        Boolean printPosi = params.getBoolean(POSITIONNEMENT_SOUS_MAT);
        Boolean printMoyEl = params.getBoolean(MOYENNE_ELEVE_SOUS_MAT);
        Boolean printMoyCl = params.getBoolean(MOYENNE_CLASSE_SOUS_MAT);
        matiere.put(POSITIONNEMENT_SOUS_MAT, printPosi).put(MOYENNE_CLASSE_SOUS_MAT, printMoyCl)
                .put(MOYENNE_ELEVE_SOUS_MAT, printMoyEl).put(GET_MOYENNE_CLASSE, params.getBoolean(MOYENNE_CLASSE))
                .put(GET_MOYENNE_ELEVE, params.getBoolean(MOYENNE_ELEVE))
                .put(GET_POSITIONNEMENT, params.getBoolean(POSITIONNEMENT));


        //TODO modifcation a été nécessaire à check
        int rowSpan;
        try {
            rowSpan = (isNull(sousMatiere) || !params.getBoolean(PRINT_SOUS_MATIERES)) ? 1 : sousMatiere.size() + 1;
        }catch (NullPointerException e){
            rowSpan = 1;
        }


        String backgroundColor = matiere.getString(BACKGROUND_COLOR);
        matiere.put("rowSpan", rowSpan)
                .put("hasSousMatiere", isNull(sousMatiere)? false : sousMatiere.size()>0);
        JsonArray sousMatiereWithoutFirst = new JsonArray();
        if(isNotNull(sousMatiere)) {
            for (int i = 0; i < sousMatiere.size(); i++){
                JsonObject sousMat = sousMatiere.getJsonObject(i);
                sousMat.put(BACKGROUND_COLOR, backgroundColor);
                Long idSousMat = sousMat.getLong(ID_TYPE_SOUS_MATIERE);

                if(isNotNull(idSousMat)) {
                    // mise en forme du positionnement
                    JsonObject posSous = matiere.getJsonObject("_" + POSITIONNEMENTS_AUTO);
                    Float pos = getMoyenneForSousMat(posSous, idPeriode, idSousMat);
                    String val = NN;
                    if(isNotNull(pos)){
                        val = utilsService.convertPositionnement(pos, tableauDeConversion,false);
                    }
                    sousMat.put(POSITIONNEMENT, val);

                    // Mise en forme de la moyenne Elève des sousMatières
                    JsonObject moyenSous = matiere.getJsonObject("_" + MOYENNE);
                    Float moyElv = getMoyenneForSousMat(moyenSous, idPeriode, idSousMat);
                    sousMat.put(MOYENNE_ELEVE, isNull(moyElv)? NN : moyElv);

                    // Mise en forme de la moyenne Elève des sousMatières
                    JsonObject moyenClasseSous = matiere.getJsonObject("_moyennesClasse");
                    Float moyCl = getMoyenneForSousMat(moyenClasseSous, idPeriode, idSousMat);
                    sousMat.put(MOYENNE_CLASSE, isNull(moyCl)? NN : moyCl);
                    sousMat.put("subCoef",1);
                    for(Service service : services){
                        for(SubTopic subTopic : service.getSubtopics()){
                            if(subTopic.getId().equals(sousMat.getInteger("id_type_sousmatiere"))){
                                sousMat.put("subCoef",subTopic.getCoefficient());
                            }
                        }
                    }
                    if(i!=0){
                        sousMatiereWithoutFirst.add(sousMat);
                    }
                    if(i==0){
                        matiere.put("firstSousMatiere", sousMat);
                    }
                }
            }
            matiere.remove(SOUS_MATIERES);
            matiere.put(SOUS_MATIERES, sousMatiereWithoutFirst);
        }
    }
    private void setPrintCoefficient(JsonObject matiere, JsonObject params){
        matiere.put(PRINT_COEFFICIENT, params.getBoolean(COEFFICIENT));
        matiere.put("coef", matiere.getValue("coef", "1"));
    }

    private void checkCoefficientConflict(JsonObject elevesObject, JsonObject coefficient, JsonObject params){
        JsonArray subjectConflict = new JsonArray();
        for(Map.Entry<String, Object> coefEntry : coefficient.getMap().entrySet()){
            subjectConflict.add( ((JsonObject)coefEntry.getValue()).put(COEFFICIENT, coefEntry.getKey()));
        }
        if(subjectConflict.size()>1){
            elevesObject.put("conflict_"+ COEFFICIENT, subjectConflict);
            elevesObject.put("has_conflict_"+ COEFFICIENT, true);
            if(!params.containsKey(ERROR + COEFFICIENT)){
                params.put(ERROR + COEFFICIENT, true);
            }
        }
    }

    /**
     *  Calcule et met en forme les colonnes de la matière passée en paramètre
     * @param matiereJO matière à traiter
     * @param matiere
     * @param idPeriode période sélectionnée
     * @param classe JsonObject contenant les informations de la classe de l'élève dont on contruit le bulletin
     * @param services
     */
    private void buildMatiereForSuiviAcquis(final JsonObject matiereJO, Matiere matiere, Long idPeriode, final JsonObject classe,
                                            JsonObject params, List<Service> services) {
        boolean printMatiere = false;

        JsonArray models = classe.getJsonArray("models");

        if (models != null && !models.isEmpty()){
            setLibelleMatiere(matiereJO,matiere, models);
        }

        matiereJO.put(PRINT_SOUS_MATIERES, params.getBoolean(PRINT_SOUS_MATIERES));
        setPrintCoefficient(matiereJO, params);
        JsonObject moyenneEleve = getObjectForPeriode(matiereJO.getJsonArray(MOYENNES), idPeriode, ID_KEY);
        JsonObject moyenneClasse = getObjectForPeriode(matiereJO.getJsonArray("moyennesClasse"), idPeriode, ID_KEY);
        JsonObject positionnement = getObjectForPeriode(matiereJO.getJsonArray(POSITIONNEMENTS_AUTO), idPeriode,
                ID_PERIODE);
        JsonObject positionnementFinal = getObjectForPeriode(matiereJO.getJsonArray("positionnementsFinaux"),
                idPeriode, ID_PERIODE);
        JsonObject res = getObjectForPeriode(matiereJO.getJsonArray("appreciations"), idPeriode, ID_PERIODE);
        JsonObject moyenneFinale = getObjectForPeriode(matiereJO.getJsonArray("moyennesFinales"), idPeriode,
                ID_PERIODE);
        JsonObject appreciation = null;
        JsonArray appreciationByClasse = null;

        if (res != null) {
            appreciationByClasse = res.getJsonArray("appreciationByClasse");
        }
        if (appreciationByClasse != null && appreciationByClasse.size() > 0) {
            printMatiere = true;
            appreciation = appreciationByClasse.getJsonObject(0);
        }

        if (moyenneFinale != null) {
            printMatiere = true;
            matiereJO.put(MOYENNE_ELEVE, moyenneFinale.getValue("moyenneFinale"));
        } else {
            if (isNotNull(moyenneEleve)) {
                printMatiere = true;
                matiereJO.put(MOYENNE_ELEVE, moyenneEleve.getValue(MOYENNE));
            } else{
                matiereJO.put(MOYENNE_ELEVE, NN);
            }
        }

        JsonArray tableauDeconversion = classe.getJsonArray("tableauDeConversion");
        if (positionnementFinal != null) {
            printMatiere = true;
            int posFinal = positionnementFinal.getInteger("positionnementFinal");
            if(posFinal == 0)
                matiereJO.put(POSITIONNEMENT, NN);
            else
                matiereJO.put(POSITIONNEMENT, posFinal);
        } else {
            // On récupère la moyenne des positionements et on la convertie
            // Grâce à l'échelle de conversion du cycle de la classe de l'élève
            if(positionnement != null) {
                Float pos = positionnement.getFloat(MOYENNE);
                Boolean hasCompNote = positionnement.getBoolean("hasNote");
                String val = NN;
                if(isNotNull(hasCompNote) && hasCompNote){
                    printMatiere = true;
                    val = utilsService.convertPositionnement(pos, tableauDeconversion, false);
                }

                matiereJO.put(POSITIONNEMENT, val);
            }
        }

        // Mise Remplissage des données des sousMatières
        buildSousMatieres(matiereJO, tableauDeconversion, idPeriode , params,services);

        String elementsProgramme = troncateLibelle(matiereJO.getString(ELEMENTS_PROGRAMME), MAX_SIZE_LIBELLE);

        String app = "";

        if(appreciation != null) {
            app = troncateLibelle(appreciation.getString(APPRECIATION_KEY), MAX_SIZE_LIBELLE);
            printMatiere = true;
        }

        // Construction des libelles et de leur style.
        matiereJO.put(ELEMENTS_PROGRAMME, elementsProgramme)
                .put(MOYENNE_CLASSE, (moyenneClasse != null) ? moyenneClasse.getValue(MOYENNE) : NN)
                .put(APPRECIATION_KEY, app)
                .put(PRINT_MATIERE_KEY, printMatiere);

        JsonArray teachers = matiereJO.getJsonArray("teachers");

        // Rajout de la première lettre du prenom des enseignants
        if (teachers != null && teachers.size() > 0) {
            for(int j=0; j < teachers.size(); j++) {
                JsonObject teacher = teachers.getJsonObject(j);
                String initial = teacher.getString(FIRST_NAME_KEY);
                if(initial == null) {
                    initial = "";
                } else {
                    initial =  String.valueOf(initial.charAt(0));
                }
                teacher.put("initial", initial);
                String name = teacher.getString(NAME);
                if(name != null) {
                    if(j == teachers.size() - 1) {
                        name += "";
                    } else {
                        name += ",";
                    }
                    teacher.remove(NAME);
                    teacher.put(NAME, name);
                }
            }
        }
    }

    @Override
    public JsonArray sortResultByClasseNameAndNameForBulletin(Map<String, JsonObject> mapEleves) {
        List<JsonObject> eleves = new ArrayList<>(mapEleves.values());
        Collections.sort(eleves, new Comparator<JsonObject>() {
            private static final String KEY_DISPLAY_NAME = DISPLAY_NAME;
            private static final String KEY_CLASSE_NAME = CLASSE_NAME;

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                try {
                    valA = a.getString(KEY_CLASSE_NAME) + "-" + a.getString(KEY_DISPLAY_NAME);
                    valB = b.getString(KEY_CLASSE_NAME) + "-" + b.getString(KEY_DISPLAY_NAME);
                } catch (Exception e) {
                    //do something
                }
                valA = Utils.removeAccent(valA);
                valB = Utils.removeAccent(valB);
                return valA.compareTo(valB);
            }
        });

        JsonArray sortedJsonArray = new JsonArray();
        for (JsonObject o : eleves) {
            JsonArray responsables = o.getJsonArray("responsables");
            if(responsables == null || responsables.size() == 0) {
                String keyResponsable = GET_RESPONSABLE;
                o.remove(keyResponsable);
                o.put(keyResponsable, false);
                sortedJsonArray.add(o);
            } else {
                for (int i = 0; i < responsables.size(); i++) {
                    if (i == 0) {
                        sortedJsonArray.add(setResponsablesLibelle(JsonObject.mapFrom(o),
                                responsables.getJsonObject(i)));
                    } else {
                        JsonObject responsable = setResponsablesLibelle(JsonObject.mapFrom(o),
                                responsables.getJsonObject(i));
                        Boolean isDifferentAddress = false;
                        if(sortedJsonArray.isEmpty()) continue;
                        try {
                            for (int j = sortedJsonArray.size() - 1; j > (sortedJsonArray.size() - 1 - i); j--) {
                                JsonObject responsableToCheck = sortedJsonArray.getJsonObject(j);
                                String addressResponsaleToCheck = responsableToCheck.getString(ADDRESSE_POSTALE);
                                String addressResponsale = responsable.getString(ADDRESSE_POSTALE);
                                String lastNameResponsableToCheck = responsableToCheck.getString("responsableLastName","");
                                String lastNameResponsable = responsable.getString("responsableLastName","");
                                String civiliteResponsableToCheck = responsableToCheck.getString("civilite");
                                String civiliteResponsable = responsable.getString("civilite");
                                String newLastNameResponsableToCheck;

                                if (!addressResponsale.equals(addressResponsaleToCheck)) {
                                    isDifferentAddress = true;
                                    responsable.put("externalIdRelative", responsables.getJsonObject(i).getString("externalIdRelative")  );
                                    responsableToCheck.put("externalIdRelative", responsables.getJsonObject(i-1).getString("externalIdRelative"));
                                } else { //if same adress
                                    //with same lastName
                                    JsonArray responsableNewLibelle = new fr.wseduc.webutils.collections.JsonArray();
                                    JsonArray responsableOldLibelle = responsableToCheck.getJsonArray("responsableLibelle");
                                    if (lastNameResponsable.equals(lastNameResponsableToCheck)) {
                                        if ("M.".equals(civiliteResponsableToCheck)) {
                                            newLastNameResponsableToCheck = civiliteResponsableToCheck + " et Mme " +
                                                    lastNameResponsableToCheck + " " +
                                                    responsableToCheck.getString("responsableFirstName", "");
                                        } else {
                                            newLastNameResponsableToCheck = civiliteResponsable + " et Mme " +
                                                    lastNameResponsable + " " +
                                                    responsable.getString("responsableFirstName", "");
                                        }

                                        responsableNewLibelle.add(newLastNameResponsableToCheck);
                                        responsableNewLibelle.add(responsableOldLibelle.getValue(1))
                                                .add(responsableOldLibelle.getValue(2));
                                    } else { //if same adress with different lastName
                                        JsonObject responsableWithDifferentName = new JsonObject();
                                        if ("M.".equals(civiliteResponsableToCheck)) {
                                            responsableWithDifferentName.put("firstLastName", civiliteResponsableToCheck + " " + lastNameResponsableToCheck + " et")
                                                    .put("secondLastName", civiliteResponsable + " " + lastNameResponsable)
                                                    .put("adresseResponsable", responsableOldLibelle.getValue(1))
                                                    .put("codePostalRelative", responsableOldLibelle.getValue(2));
                                        } else {
                                            responsableWithDifferentName.put("firstLastName", civiliteResponsable + " " + lastNameResponsable + " et")
                                                    .put("secondLastName", civiliteResponsableToCheck + " " + lastNameResponsableToCheck);
                                            if (responsableOldLibelle.size() > 1){
                                                responsableWithDifferentName.put("adresseResponsable", responsableOldLibelle.getValue(1))
                                                        .put("codePostalRelative", responsableOldLibelle.getValue(2));
                                            }else{
                                                responsableWithDifferentName.put("adresseResponsable", "")
                                                        .put("codePostalRelative", "");
                                            }
                                        }

                                        responsableNewLibelle.add(responsableWithDifferentName);
                                        responsableToCheck.put("relativesHaveTwoNames", true);
                                    }
                                    responsableToCheck.put("responsableLibelle", responsableNewLibelle);
                                }
                            }
                        } catch (ArrayIndexOutOfBoundsException e) {
                            continue;
                        }
                        if (isDifferentAddress) {
                            sortedJsonArray.add(responsable);
                        }
                    }
                }
            }
        }
        return sortedJsonArray;
    }


    private JsonObject setResponsablesLibelle(JsonObject o, JsonObject responsable) {
        Boolean relativesHaveTwoNames = false;
        JsonObject res = new JsonObject(o.getMap());
        String civilite = responsable.getString("civilite");
        String lastName = responsable.getString("lastNameRelative");
        String firstName = responsable.getString("firstNameRelative");
        String address = responsable.getString("address");
        String city = responsable.getString("city");
        try{
            String zipCode = responsable.getString("zipCode");
            if (zipCode == null) {
                zipCode = " ";
            }

            if (city == null) {
                city = zipCode;
            } else {
                city = zipCode + " " + city;
            }

        } catch(ClassCastException e){
            String zipCode = String.valueOf(responsable.getInteger("zipcode"));
            if (zipCode == null) {
                zipCode = " ";
            }

            if (city == null) {
                city = zipCode;
            } else {
                city = zipCode + " " + city;
            }
        }

        if (civilite == null) {
            civilite = "M.";
        }

        JsonArray responsableLibelle = new JsonArray().add(civilite + " " + lastName  + " " + firstName);
        if (address != null){
            responsableLibelle.add(address);
        }
        else {
            address = " ";
            responsableLibelle.add(address);
        }

        responsableLibelle.add(city);

        res.put("responsableLibelle", responsableLibelle);
        res.put(ADDRESSE_POSTALE, address + city);
        res.put("responsableLastName", lastName);
        res.put("civilite", civilite);
        if("M.".equals(civilite)){
            res.put("responsableFirstName", firstName);
        }
        res.put("relativesHaveTwoNames", relativesHaveTwoNames);
        return res;
    }

    private void logStudentNotFound(String idEleve, String service) {
        log.error("[ " + EXPORT_BULLETIN_METHOD + "| " + service + "] : elevesMap doesn't contains idEleve " + idEleve);
    }

    private void logidClasseNotFound(String idEleve, String service) {
        log.error("[ " + EXPORT_BULLETIN_METHOD + "| " + service + "] : eleveObject doesn't contains field idClasse "
                + idEleve);
    }

    private  void logidEtabNotFound(String idEleve, String service) {
        log.error("[ " + EXPORT_BULLETIN_METHOD + "| "+ service + "] : " +
                "eleveObject doesn't contains field idEtablissement "  + idEleve);
    }

    // Rappelle l'évent Bus si l'erreur est un timeout sinon renvoit l'erreur
    private void buildErrorReponseForEb (String idEleve,
                                         String mess, AtomicBoolean answer, int count, JsonObject action,
                                         Handler currentHandler, Handler finalHandler, JsonObject eleve,
                                         String method) {

        if (mess.contains(TIME) && !answer.get()) {
            count++;
            eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                    handlerToAsyncHandler(currentHandler));
        }
        else {
            if (eleve.getJsonArray(ERROR) == null) {
                eleve.put(ERROR, new JsonArray());
            }
            JsonArray errors = eleve.getJsonArray(ERROR);
            errors.add(method);
            serviceResponseOK(answer, finalHandler, count, idEleve, method);
        }
    }


    /*
      Method assurant la réponse de chaque service lancé
     */
    //TODO FAIRE UNE FUTURE BOOL PLUTOT
    private void serviceResponseOK (AtomicBoolean answer, Handler<Either<String, JsonObject>> finalHandler,
                                    int count, String idEleve, String method) {
        if (count > 1 ) {

            log.debug("[ "+ method + " ] : "
                    + idEleve + " success " + count);
        }
        if(!answer.get()) {
            answer.set(true);
            log.debug(" -------[" + method + "]: " + idEleve + " FIN " );
            finalHandler.handle(new Either.Right<>(null));
        }
    }

    public void setBirthDate(JsonObject eleve){
        String birthDate = eleve.getString("birthDate");
        if(birthDate != null) {
            String [] be = birthDate.split("-");
            eleve.put("birthDateLibelle",  be[2] + '/' + be[1] + '/' + be[0]);
        }
    }

    public void setIdGraphPerDomaine(JsonObject eleve, JsonObject images){
        String idEleve = eleve.getString(ID_ELEVE_KEY);
        if (images != null) {
            String img = images.getString(idEleve);
            Boolean hasGraphPerDomaine = (img != null);
            eleve.put("hasGraphPerDomaine", hasGraphPerDomaine);
            if(Boolean.TRUE.equals(hasGraphPerDomaine)) {
                eleve.put(GRAPH_PER_DOMAINE, img);
            }
        }
    }

    public void setIdGraphPerDomaine(Student student, JsonObject images){
        String idEleve = student.getId();
        if (images != null) {
            String img = images.getString(idEleve);
            Boolean hasGraphPerDomaine = (img != null);
            student.getParamBulletins().setHasGraphPerDomaine(hasGraphPerDomaine);
            if(hasGraphPerDomaine) {
                //GRAPH_PER_DOMAINE
                student.getParamBulletins().setImGraph(img);
            }
        }
    }

    public void setLevel(JsonObject eleve) {
        String level = eleve.getString(LEVEL);
        if(level == null) {
            level = eleve.getString(CLASSE_NAME_TO_SHOW);
        }
        if(level != null) {
            level = String.valueOf(level.charAt(0));
            try {
                int levelInt = Integer.parseInt(level);
                if(levelInt >= 3 && levelInt <= 6) {
                    eleve.put("level", level);
                    eleve.put("hasLevel", true);
                    eleve.put("imgLevel", ImgLevel.getImgLevel(levelInt));
                }
            }
            catch (NumberFormatException e) {
                eleve.put("hasLevel", false);
            }
        }
    }


    public void setLevel(Student student,JsonObject eleve) {
        String level = eleve.getString(LEVEL);
        Level level1 = new Level();

        student.setLevel(level1);
        if(level == null) {
            level = eleve.getString(CLASSE_NAME_TO_SHOW);
            level1.setName(level);
        }
        if(level != null) {
            level = String.valueOf(level.charAt(0));
            try {
                int levelInt = Integer.parseInt(level);
                if(levelInt >= 3 && levelInt <= 6) {
                    level1.setName(level);
                    level1.setImage(ImgLevel.getImgLevel(levelInt));

                    eleve.put("level", level);
                    eleve.put("hasLevel", true);
                    eleve.put("imgLevel", ImgLevel.getImgLevel(levelInt));
                }
            }
            catch (NumberFormatException e) {
                eleve.put("hasLevel", false);
            }
        }
    }

    public void buildDataForStudent(final AtomicBoolean answered, JsonArray eleves,
                                    Map<String, JsonObject> elevesMap, Long idPeriode, JsonObject params,
                                    final JsonObject classe, Boolean showBilanPerDomaines, String host,
                                    String acceptLanguage, Handler<Either<String, JsonObject>> finalHandler, Vertx vertx) {
        JsonObject images = params.getJsonObject("images");
        Long typePeriode = params.getLong(TYPE_PERIODE);

        List<Future> futures = new ArrayList<>();
        if (eleves.size() > 0) {
            JsonObject firstStudent = eleves.getJsonObject(0);
            String idClasse = firstStudent.getString("idClasse");
            Utils.getGroupesClasse(eb, new JsonArray().add(idClasse), new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if(event.isRight() ) {
                        List<String> groupIds = new ArrayList<>(event.right().getValue().getJsonObject(0).getJsonArray("id_groupes").getList());
                        groupIds.add(idClasse);
                        Promise<Structure> structurePromise = Promise.promise();
                        Promise<Periode> periodeLibellePromise = Promise.promise();
                        Promise<Periode> periodeYearPromise = Promise.promise();
                        Promise<Object> imgPromise = Promise.promise();
                        Promise<Object> listStudentsPromise = Promise.promise();
                        Promise<Object> servicesPromise = Promise.promise();
                        Promise<Object> multiTeachingPromise = Promise.promise();
                        Promise<List<SubTopic>> subTopicCoefPromise = Promise.promise();
                        List<Future> promises = new ArrayList<>();
                        promises.add(structurePromise.future());
                        promises.add(periodeLibellePromise.future());
                        promises.add(periodeYearPromise.future());
                        promises.add(imgPromise.future());
                        promises.add(listStudentsPromise.future());
                        promises.add(servicesPromise.future());
                        promises.add(multiTeachingPromise.future());
                        promises.add(subTopicCoefPromise.future());
                        int nbOptions= 0;
                        if(!params.getBoolean(HIDE_HEADTEACHER, false)) {
                            Promise<Object> getHeadTeachersPromise = Promise.promise();
                            promises.add(getHeadTeachersPromise.future());
                            getHeadTeachers(firstStudent.getString("idClasse"),getHeadTeachersPromise);
                            nbOptions++;
                        }

                        getSubTopicCoeff(firstStudent.getString("idEtablissement"),firstStudent.getString("idClasse"),subTopicCoefPromise);
                        getStructure(firstStudent.getString("idEtablissement"),structurePromise);
                        getLibellePeriode(idPeriode,host,acceptLanguage,periodeLibellePromise);
                        getAnneeScolaire(idClasse,periodeYearPromise);
                        generateImagesFromPathForBulletin(params,vertx,imgPromise);
                        Utils.getElevesClasse(eb, idClasse, idPeriode, listStudentsPromise);
                        utilsService.getServices(firstStudent.getString("idEtablissement"),
                                new JsonArray(groupIds),FutureHelper.handlerJsonArray(servicesPromise));
                        utilsService.getMultiTeachers(firstStudent.getString("idEtablissement"),
                                new JsonArray(groupIds),idPeriode.intValue() ,FutureHelper.handlerJsonArray(multiTeachingPromise));

                        int finalNbOptions = nbOptions;
                        CompositeFuture.all(promises).onSuccess(success -> {

                            List<SubTopic> subTopics = subTopicCoefPromise.future().result();
                            Structure structure = structurePromise.future().result();
                            Periode periode = periodeLibellePromise.future().result();
                            periode.setEndDate( periodeYearPromise.future().result().getEndDate());
                            periode.setStartDate( periodeYearPromise.future().result().getStartDate());
                            ParamsBulletins paramBulletins = new ParamsBulletins();
                            paramBulletins.setParams(params);
                            paramBulletins.setHasImgLoaded(true);
                            paramBulletins.addParams((JsonObject) success.result().list().get(3));
                            JsonArray idEleves = new JsonArray(((JsonArray) success.result().list().get(4)).stream()
                                    .map(e -> ((JsonObject) e).getString(ID_ELEVE_KEY)).collect(Collectors.toList()));
                            Map<String, Student> students = new HashMap<>();

                            JsonArray servicesJson = (JsonArray) success.result().list().get(5);
                            JsonArray multiTeachinJsonArray = (JsonArray) success.result().list().get(6);
                            for(int i = 1; i <= finalNbOptions; i++) {
                                paramBulletins.addParams((JsonObject) success.result().list().get(6 + i));
                            }
                            List<Service> services = new ArrayList<>();
                            List<MultiTeaching> multiTeachings = new ArrayList<>();

                            setMultiTeaching(structure, multiTeachinJsonArray, multiTeachings, idClasse);
                            setServices(structure, servicesJson, services,subTopics);

                            for (int i = 0; i < eleves.size(); i++) {
                                futures.add(Future.future());
                                JsonObject eleve = eleves.getJsonObject(i);
                                String idEleve = eleve.getString(ID_ELEVE_KEY);
                                Student student = initStudent(structure, periode, paramBulletins, services, multiTeachings,
                                        eleve, typePeriode, idPeriode, classe, showBilanPerDomaines, images, params);
                                students.put(idEleve, student);
                                getExportBulletin(answered, idEleve, elevesMap, student, idEleves,idPeriode, params, classe, host, acceptLanguage, vertx,
                                        futureGetHandler(futures.get(i)));
                            }
                            CompositeFuture.all(futures).setHandler(compositeEvent -> {
                                if (compositeEvent.succeeded()) {
                                    //ici créer le Json
                                    eleves.clear();
                                    eleves.addAll(new JsonArray(compositeEvent.result().list()));
                                    for(Object eleve : eleves){
                                        JsonObject ob = (JsonObject) eleve;
                                        elevesMap.put(ob.getString("idEleve"),ob);
                                    }
                                    log.info("[Competences DefaultExportBulletinService ]end students" );
                                    finalHandler.handle(new Either.Right<>(null));
                                }
                            });

                        });
                    }
                }
            });
        }
    }

    private void getSubTopicCoeff(String idEtablissement, String idClasse, Promise<List<SubTopic>> promise) {
        subTopicService.getSubtopicServices(idEtablissement,idClasse,event -> {
            if(event.isRight()){
                List<SubTopic> subTopics= new ArrayList<>();
                for(Object subTopicobj : event.right().getValue()){
                    SubTopic subTopic = new SubTopic();
                    JsonObject subTopicJo = (JsonObject) subTopicobj;
                    Service service = new Service();
                    Matiere matiere = new Matiere();
                    Group group = new Group();
                    Teacher teacher = new Teacher();
                    group.setId(subTopicJo.getString("id_group"));
                    matiere.setId(subTopicJo.getString("id_topic"));
                    teacher.setId(subTopicJo.getString("id_teacher"));
                    service.setMatiere(matiere);
                    service.setGroup(group);
                    service.setTeacher(teacher);
                    subTopic.setService(service);
                    subTopic.setId(subTopicJo.getInteger("id_subtopic"));
                    subTopic.setCoefficient(safeGetDouble(subTopicJo,"coefficient"));
                    subTopics.add(subTopic);
                }
                promise.complete(subTopics);
            }else{
                promise.fail(event.left().getValue());
            }
        });
    }

    private Student initStudent(Structure structure, Periode periode, ParamsBulletins paramBulletins, List<Service> services, List<MultiTeaching> multiTeachings, JsonObject eleve, Long typePeriode, Long idPeriode, JsonObject classe, Boolean showBilanPerDomaines, JsonObject images, JsonObject params) {
        Student student = new Student();
        student.setFirstName(eleve.getString("firstName"));
        student.setLastName(eleve.getString("lastName"));
        student.setINE(eleve.getString("ine"));
        student.setId(eleve.getString("idEleve"));
        student.setDeleteDate(eleve.getString("u.deleteDate"));
        student.setExternalId(eleve.getString("externalId"));
        student.setParamBulletins(paramBulletins);


        structure.setId(eleve.getString("idEtablissement"));
        student.setStructure(structure);

        Classe classeStudent = new Classe();
        classeStudent.addMultiTeaching(multiTeachings);
        classeStudent.addServices(services);
        classeStudent.setName(eleve.getString("classeName"));
        classeStudent.setDisplayName(eleve.getString("classeNameToShow"));
        classeStudent.setId(eleve.getString("idClasse"));
        student.setClasse(classeStudent);

        periode.setType(typePeriode);
        periode.setIdPeriode(idPeriode);
        //Faire le libelle
        classeStudent.setPeriode(periode);
        JsonArray groupes, manualGroupes = new JsonArray();
          try {
            groupes = eleve.getJsonArray("idGroupes");
              } catch (ClassCastException e) {
             String groupesStr = eleve.getString("idGroupes");
             String[] array = groupesStr.split(",");
              groupes = new JsonArray();
            for(String s : array){
              groupes.add(s);
            }
          }
        manualGroupes.addAll(eleve.getJsonArray("idManualGroupes", new JsonArray()));
        for (int j = 0; j < groupes.size(); j++) {
            Group group = new Group();
            group.setId(groupes.getString(j));
            student.addGroupe(group);
        }

        for (int j = 0; j < manualGroupes.size(); j++) {
            Group group = new Group();
            group.setId(manualGroupes.getString(j));
            student.addManualGroupe(group);
        }


        eleve.put(TYPE_PERIODE, typePeriode);
        eleve.put(ID_PERIODE_KEY, idPeriode);

        // Mise en forme de la date de naissance
        student.setBirthDate(eleve.getString("birthDate"));
        student.formatBirthDate();


        setBirthDate(eleve);

        // Classe à afficher
        setStudentClasseToPrint(eleve, classe);

        // Ajout de l'image du graphe par domaine
        if (showBilanPerDomaines) {
            //TODO supprimer des que student est pris en compte
            setIdGraphPerDomaine(student, images);
        }

        // Ajout du niveau de l'élève
        setLevel(eleve);
        setLevel(student, eleve);

        // Ajout de l'idEtablissement pour l'archive
        if (isNotNull(params.getString("idStructure")) && (isNull(eleve.getString(ID_ETABLISSEMENT_KEY))
                || !eleve.getString(ID_ETABLISSEMENT_KEY).equals(params.getString(ID_ETABLISSEMENT_KEY)))) {
            eleve.put(ID_ETABLISSEMENT_KEY, params.getString("idStructure"));
        }

        eleve.put("hasINENumber", eleve.containsKey("ine") && eleve.getString("ine") != null);
        return student;
    }

    private void setMultiTeaching(Structure structure, JsonArray multiTeachinJsonArray, List<MultiTeaching> multiTeachings, String idClasse) {
        for(int i = 0 ; i < multiTeachinJsonArray.size(); i++){
            JsonObject multiTeachinJo = multiTeachinJsonArray.getJsonObject(i);
            MultiTeaching multiTeaching = new MultiTeaching();
            multiTeaching.setStructure(structure);

            Teacher mainTeacher = new Teacher();
            Teacher secondTeacher = new Teacher();
            mainTeacher.setId(multiTeachinJo.getString("main_teacher_id"));
            secondTeacher.setId(multiTeachinJo.getString("second_teacher_id"));

            multiTeaching.setMainTeacher(mainTeacher);
            multiTeaching.setSecondTeacher(secondTeacher);

            Subject subject = new Subject();
            subject.setId(multiTeachinJo.getString("subject_id"));
            Classe classe = new Classe();
            classe.setId(idClasse);

            multiTeaching.setSubject(subject);

            multiTeaching.setIdInteger(multiTeachinJo.getInteger("id"));
            multiTeaching.setGroupOrClassId(multiTeachinJo.getString("class_or_group_id"));
            multiTeaching.setStartDate(multiTeachinJo.getString("start_date",""));
            multiTeaching.setEndDate(multiTeachinJo.getString("end_date",""));
            multiTeaching.setEnteredEndDate(multiTeachinJo.getString("entered_end_date"));
            multiTeaching.setCoTeaching(multiTeachinJo.getBoolean("is_coteaching"));
            multiTeaching.setVisible(multiTeachinJo.getBoolean("is_visible"));
            multiTeaching.setLibelle(multiTeachinJo.getString("libelle",""));
            multiTeaching.setTimestampDt(multiTeachinJo.getString("timestamp_dt",""));
            multiTeaching.setTimestampFn(multiTeachinJo.getString("timestamp_Fn",""));
            multiTeaching.setEndDateSaisie(multiTeachinJo.getString("date_fin_saisie"));
            multiTeaching.setDateConseilClass(multiTeachinJo.getString("date_conseil_classe"));
            multiTeaching.setClasse(classe);
            multiTeaching.setType(multiTeachinJo.getInteger("id_type"));
            multiTeaching.setPublicationBulletin(multiTeachinJo.getBoolean("publication_bulletin"));
            multiTeachings.add(multiTeaching);
        }
    }

    private void setServices(Structure structure, JsonArray servicesJson, List<Service> services, List<SubTopic> subTopics) {
        for (int i = 0 ; i < servicesJson.size();i++){
            JsonObject serviceJo = servicesJson.getJsonObject(i);
            Service service = new Service();
            service.setStructure(structure);
            Group group = new Group();
            group.setId(serviceJo.getString("id_groupe"));
            service.setGroup(group);
            Matiere matiere = new Matiere();
            matiere.setId(serviceJo.getString("id_matiere"));
            service.setMatiere(matiere);
            Teacher teacher =  new Teacher();
            teacher.setId(serviceJo.getString("id_enseignant"));
            service.setTeacher(teacher);
            service.setEvaluable(serviceJo.getBoolean("evaluable"));
            service.setVisible(serviceJo.getBoolean("is_visible"));
            service.setModalite(serviceJo.getString("modalite",""));
            service.setCoefficient(serviceJo.getLong("coefficient"));
            for(SubTopic subTopic : subTopics){
                if(subTopic.getService().equals(service)){
                    service.addSubtopics(subTopic);
                }
            }
            services.add(service);

        }
    }

    private void setStudentClasseToPrint(JsonObject student, JsonObject classe){
        student.put(CLASSE_NAME_TO_SHOW, classe.getString("classeName"));
    }

    private void getElevesClasse( String idClasse, Long idPeriode, Future<JsonArray> elevesFuture){
        Utils.getElevesClasse(eb, idClasse, idPeriode, elevesEvent -> {
            if(elevesEvent.isRight()){
                elevesFuture.complete(elevesEvent.right().getValue());
            }
            else {
                String error = elevesEvent.left().getValue();
                if(error.contains(TIME)){
                    log.error("[Competences getElevesClasse] : "+ error);
                    getElevesClasse(idClasse, idPeriode, elevesFuture);
                    return;
                }
                elevesFuture.complete(new JsonArray());
            }
        });
    }

    private void getConversionNoteCompetence(String idEtablissement, String idClasse, Future<JsonArray> tab){
        competenceNoteService.getConversionNoteCompetence(idEtablissement, idClasse,
                tableau -> {
                    if (tableau.isRight()) {
                        tab.complete(tableau.right().getValue());
                    } else {
                        String error = tableau.left().getValue();
                        if(error.contains(TIME)){ //boucle infini
                            log.error("[getConversionNoteCompetence] : "+ error);
                            getConversionNoteCompetence(idEtablissement, idClasse,tab);
                            return;
                        }
                        tab.complete(new JsonArray());
                    }
                });
    }

    public static void getExternalIdClasse(String idClasse,  Handler<Either<String, JsonObject>> handler) {
        try {
            String query = "MATCH (c:Class {id:{idClasse}}) return c.externalId as externalId ";
            JsonObject params = new JsonObject().put(ID_CLASSE_KEY, idClasse);
            Neo4j.getInstance().execute(query.toString(), params, Neo4jResult.validUniqueResultHandler(handler));
        }catch (Exception e){
            handler.handle(new Either.Left<>("[DefaultExportBulletinService | getExternalIDClassse] : Exception on savePdfInStorage "
                    + e.getMessage() + " "
                    + idClasse));
        }
    }

    // BULLETIN WORKER
    @Override
    public void savePdfInStorage(JsonObject eleve, Buffer file, Handler<Either<String, JsonObject>> handler){
        try{
            String name = getFileNameForStudent(eleve);
            String idEleve = eleve.getString("idEleve");
            String idClasse = eleve.getString("idClasse");
            Integer idCycle = eleve.getInteger("idCycle");
            String externalIdClasse = eleve.getString("externalId");
            String idEtablissement = eleve.getString("idEtablissement");
            Long idPeriode = eleve.getLong("idPeriode");
            String idParent = getIdParentForStudent(eleve);
            String type = eleve.getString("typeExport");

            this.storage.writeBuffer(file, "application/pdf", name, uploaded -> {
                try {
                    String idFile = uploaded.getString("_id");
                    if (!OK.equals(uploaded.getString(STATUS)) || idFile == null) {
                        String error = "save pdf  : " + uploaded.getString(MESSAGE);
                        if (error.contains(TIME)) {
                            savePdfInStorage(eleve, file, handler);
                        } else {
                            log.error(error);
                            handler.handle(new Either.Right<>(new JsonObject()));
                        }
                        return;
                    }

                    Date date = new Date();
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    int month = cal.get(Calendar.MONTH);
                    int year = cal.get(Calendar.YEAR);
                    String idYear;
                    if (month >= 7) {
                        idYear = String.valueOf(year);
                    } else {
                        idYear = String.valueOf(year - 1);
                    }
                    if (type.equals(TypePDF.BULLETINS.toString())) {
                        handleSaveBulletinInSql(eleve, file, handler, name, idEleve, idClasse, externalIdClasse,
                                idEtablissement, idPeriode, idParent, idFile, idYear);
                    } else {
                        handleSaveBFCinSQL(eleve, file, handler, name, idEleve, idClasse, idCycle, idYear,
                                externalIdClasse, idEtablissement, idFile);
                    }
                }catch (Exception e){
                    handler.handle(new Either.Left<>("[DefaultExportBulletinService | savePdfInStorage | writeBuffer] : Exception on savePdfInStorage "
                            + e.getClass().toString() + " "
                            + eleve.getString("idEleve") + " " + eleve.getString("lastName")));
                }

            });
        }catch (Exception e){
            handler.handle(new Either.Left<>("[DefaultExportBulletinService | savePdfInStorage] : Exception on savePdfInStorage "
                    + e.getMessage() + " "
                    + eleve.getString("idEleve") + " " + eleve.getString("lastName")));
        }
    }

    private void handleSaveBFCinSQL(JsonObject eleve, Buffer file, Handler<Either<String, JsonObject>> handler,
                                    String name, String idEleve, String idClasse, Integer idCycle, String idYear,
                                    String externalIdClasse, String idEtablissement, String idFile) {
        try{
            if (isNotNull(externalIdClasse)) {
                saveBFCfile(idEleve, idClasse, externalIdClasse, idEtablissement, idCycle, idYear, name, idFile, handler);
            }else{
                getExternalIdClasse(idClasse, event -> {
                    try{
                        if (event.isLeft()) {
                            String error = "save bfc pdf  : " + event.left().getValue();
                            log.error(error);
                            if (error.contains(TIME)) {
                                getExternalIdClasse(idClasse, handler);
                            } else {
                                log.error(error);
                                handler.handle(new Either.Right<>(new JsonObject()));
                            }
                            return;
                        } else {
                            JsonObject result = event.right().getValue();
                            if (result == null) {
                                log.error("Null externalId");
                                handler.handle(new Either.Right<>(new JsonObject()));
                            } else {
                                String externalId = result.getString(EXTERNAL_ID_KEY);
                                saveBFCfile(idEleve, idClasse, externalId, idEtablissement, idCycle, idYear, name, idFile, handler);
                            }
                        }
                    }catch (Exception e){
                        handler.handle(new Either.Left<>("[DefaultExportBulletinService | handleBFCinSQL event id classe] : Exception on savePdfInStorage "
                                + e.getMessage() + " "
                                + eleve.getString("idEleve") + " " + eleve.getString("lastName")));
                    }
                });
            }
        }catch (Exception e){
            handler.handle(new Either.Left<>("[DefaultExportBulletinService | handleBFCinSQL] : Exception on savePdfInStorage "
                    + e.getMessage() + " "
                    + eleve.getString("idEleve") + " " + eleve.getString("lastName")));
        }

    }

    private void handleSaveBulletinInSql(JsonObject eleve, Buffer file, Handler<Either<String, JsonObject>> handler,
                                         String name, String idEleve, String idClasse, String externalIdClasse,
                                         String idEtablissement, Long idPeriode, String idParent, String idFile,
                                         String idYear) {
        try {
            if (!(isNotNull(idEleve) && isNotNull(idClasse) && isNotNull(idEtablissement) && isNotNull(idPeriode))) {
                log.error("save bulletin pdf : null parameter ");
                handler.handle(new Either.Right<>(new JsonObject()));
            } else {
                Handler<Either<String, JsonObject>> saveBulletinHandler = BulletinUtils.saveBulletinHandler(idFile,idEleve,
                        idClasse, externalIdClasse, idEtablissement, idPeriode, handler);

                if (isNotNull(externalIdClasse)) {
                    BulletinUtils.saveIdBulletin(storage, idEleve, idClasse, externalIdClasse, idEtablissement, idPeriode,
                            idFile, name, idParent, idYear, saveBulletinHandler);
                } else {
                    getExternalIdClasse(idClasse, event -> {
                        try {
                            if (event.isLeft()) {
                                String error = "save bulletin pdf  : " + event.left().getValue();
                                log.error(error);
                                if (error.contains(TIME)) {
                                    savePdfInStorage(eleve, file, handler);
                                } else {
                                    log.error(error);
                                    handler.handle(new Either.Right<>(new JsonObject()));
                                }
                            } else {
                                JsonObject result = event.right().getValue();
                                if (result == null) {
                                    log.error("Null externalId");
                                    handler.handle(new Either.Right<>(new JsonObject()));
                                } else {
                                    String externalId = result.getString(EXTERNAL_ID_KEY);
                                    BulletinUtils.saveIdBulletin(storage, idEleve, idClasse, externalId, idEtablissement,
                                            idPeriode, idFile, name, idParent, idYear, saveBulletinHandler);
                                }
                            }
                        }catch (Exception e){
                            handler.handle(new Either.Left<>("[DefaultExportBulletinService | handleSaveBulletinInsQL | externalIdClasse] : Exception on savePdfInStorage "
                                    + e.getMessage() + " "
                                    + eleve.getString("idEleve") + " " + eleve.getString("lastName")));
                        }
                    });
                }
            }
        }catch (Exception e){
            handler.handle(new Either.Left<>("[DefaultExportBulletinService | handleSaveBulletinInSQL] : Exception on savePdfInStorage "
                    + e.getMessage() + " "
                    + eleve.getString("idEleve") + " " + eleve.getString("lastName")));
        }

    }

    private void saveBFCfile(String idEleve, String idClasse, String externalIdClasse, String idEtablissement,
                             Integer idCycle, String idYear, String filename, String idFile,
                             Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Competences.EVAL_SCHEMA +
                ".archive_bfc (id_eleve, external_id_classe, id_classe, id_etablissement, id_cycle, id_annee, id_file, file_name, modified ) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, Now()) " +
                "ON CONFLICT (id_classe, id_etablissement, id_cycle, id_eleve, id_annee) " +
                "DO UPDATE SET id_eleve = ?, external_id_classe = ?, id_classe = ?, id_etablissement = ?, id_cycle = ?, " +
                "id_annee = ?, id_file = ?, file_name = ? , modified = Now() " +
                "RETURNING (SELECT id_file from notes.archive_bfc " +
                "WHERE file_name = ? AND external_id_classe = ? AND id_classe = ? AND id_etablissement = ? " +
                "AND id_cycle = ? AND id_annee = ? AND id_eleve = ?);";

        JsonArray params = new JsonArray()
                .add(idEleve).add(externalIdClasse).add(idClasse).add(idEtablissement).add(idCycle).add(idYear).add(idFile).add(filename)
                .add(idEleve).add(externalIdClasse).add(idClasse).add(idEtablissement).add(idCycle).add(idYear).add(idFile).add(filename)
                .add(filename).add(externalIdClasse).add(idClasse).add(idEtablissement).add(idCycle).add(idYear).add(idEleve);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if(event.isRight()){
                String idToDelete = event.right().getValue().getJsonObject(0).getString("id_file");
                if(idToDelete != null){
                    storage.removeFile(idToDelete, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject event) {
                            handler.handle(new Either.Right<>(new JsonObject().put("idFile",idFile)));
                        }
                    });
                }else{
                    handler.handle(new Either.Right<>(new JsonObject().put("idFile",idFile)));
                }
            }else{
                handler.handle(new Either.Left<>("error when putting data in sql bfc_archive"));
            }
        }));
    }

    @Override
    public void runSavePdf(JsonObject bulletinEleve, final JsonObject bulletin, Vertx vertx, JsonObject config,
                           Handler<Either<String, String>> bulletinHandlerWork){
        try {
            final HttpServerRequest request = new JsonHttpServerRequest(bulletin.getJsonObject("request"));
            final JsonObject templateProps = bulletin;
            final String templateName = bulletin.getString("template");
            final String prefixPdfName = bulletin.getString("title");

            generateAndSavePdf(request, templateProps, templateName, prefixPdfName, bulletinEleve,
                    vertx, config, bulletinHandlerWork);
        }catch (Exception e){
            bulletinHandlerWork.handle(new Either.Left<>("runSavePdf " + e.getMessage()  + " "+
                    bulletinEleve.getString("idEleve") + " " + bulletinEleve.getString("lastName")));
        }
    }

    @Override
    public void generateAndSavePdf(final HttpServerRequest request, JsonObject resultFinal, final String templateName,
                                   final String prefixPdfName, JsonObject eleve, Vertx vertx, JsonObject config,
                                   Handler<Either<String, String>> finalHandler) {
        try {
            final String dateDebut = new SimpleDateFormat("dd.MM.yyyy").format(new Date().getTime());
            final String templatePath = FileResolver.absolutePath(config.getJsonObject("exports")
                    .getString("template-path"));
            final String baseUrl = getScheme(request) + "://" + Renders.getHost(request) +
                    config.getString("app-address") + "/public/";
            String node = (String) vertx.sharedData().getLocalMap("server").get("node");
            if (node == null) {
                node = "";
            }
            final String _node = node;
            processTemplate(request, resultFinal, templateName, prefixPdfName, eleve, vertx, config, finalHandler,
                    dateDebut, templatePath, baseUrl, _node);
        }catch (Exception e){
            finalHandler.handle(new Either.Left<>("generateAndSavePdf " + e.getMessage() + " "+
                    eleve.getString("idEleve") + " " + eleve.getString("lastName")));
        }
    }

    private void processTemplate (HttpServerRequest request, JsonObject resultFinal, String templateName,
                                  String prefixPdfName, JsonObject eleve, Vertx vertx, JsonObject config,
                                  Handler<Either<String, String>> finalHandler, String dateDebut, String templatePath,
                                  String baseUrl, String _node) {
        try {
            vertx.fileSystem().readFile(templatePath + templateName, new Handler<AsyncResult<Buffer>>() {
                @Override
                public void handle(AsyncResult<Buffer> result) {
                    if (!result.succeeded()) {
                        log.error("[DefaultExportBulletinService | processTemplate] Error while reading template : " + templatePath + templateName);
                        finalHandler.handle(new Either.Left("[DefaultExportBulletinService | processTemplate] Error while reading template : " + templatePath + templateName));
                        badRequest(request, "Error while reading template : " + templatePath + templateName);
                        return;
                    }
                    try {
                        StringReader reader = new StringReader(result.result().toString("UTF-8"));
                        Renders render = new Renders(vertx, config);
                        JsonObject templateProps = resultFinal;
                        if(eleve.containsKey("typeExport") && TypePDF.BFC.toString().equals(eleve.getString("typeExport"))){
                            templateProps.getJsonArray("classes").getJsonObject(0).put("eleves", new JsonArray().add(eleve));
                        } else {
                            templateProps.put("eleves", new JsonArray().add(eleve)  );
                        }
                        render.processTemplate(request, templateProps, templateName, reader,
                                getRenderProcessHandler(templateProps, baseUrl, _node, request, prefixPdfName, dateDebut, eleve, finalHandler));
                    }
                    catch (Exception e){
                        finalHandler.handle(new Either.Left<>(" processTemplate readFile Handle "+ e.getClass().getName() + " "+
                                eleve.getString("idEleve") + " " + eleve.getString("lastName")));
                    }
                }
            });
        }catch (Exception e){
            finalHandler.handle(new Either.Left<>("processTemplate " + e.getMessage()  + " "+
                    eleve.getString("idEleve") + " " + eleve.getString("lastName")));
        }

    }

    private Handler<Writer> getRenderProcessHandler(JsonObject templateProps, String baseUrl, String _node,
                                                    HttpServerRequest request, String prefixPdfName, String dateDebut,
                                                    JsonObject eleve, Handler<Either<String, String>> finalHandler) {
        return new Handler<Writer>() {
            @Override
            public void handle(Writer writer) {
                try{
                    String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                    JsonObject actionObject = new JsonObject();
                    byte[] bytes;
                    try {
                        bytes = processedTemplate.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        bytes = processedTemplate.getBytes();
                        log.error("[DefaultExportBulletinService | getRenderProcessHandler] " + e.getMessage() + " "+
                                eleve.getString("idEleve") + " " + eleve.getString("lastName"));
                        finalHandler.handle(new Either.Left<>("[DefaultExportBulletinService | getRenderProcessHandler] " + e.getMessage() + " "+
                                eleve.getString("idEleve") + " " + eleve.getString("lastName")));
                    }
                    actionObject.put("content", bytes).put("baseUrl", baseUrl);
                    eb.send(_node + "entcore.pdf.generator", actionObject,
                            new DeliveryOptions().setSendTimeout(
                                    TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L),
                            handlerToAsyncHandler(getPdfRenderHandler(request, templateProps, prefixPdfName, dateDebut, eleve, finalHandler)));
                }catch (Exception e){
                    finalHandler.handle(new Either.Left<>("getRenderProcessHandler " + e.getMessage() + " "+
                            eleve.getString("idEleve") + " " + eleve.getString("lastName")));
                }
            }
        };
    }

    private Handler<Message<JsonObject>> getPdfRenderHandler(HttpServerRequest request, JsonObject templateProps,
                                                             String prefixPdfName, String dateDebut, JsonObject eleve,
                                                             Handler<Either<String, String>> finalHandler) {
        return new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                JsonObject pdfResponse = reply.body();
                try {
                    if (!"ok".equals(pdfResponse.getString("status"))) {
                        badRequest(request, pdfResponse.getString("message"));
                        finalHandler.handle(new Either.Left("getPdfRenderHandler pdfResponse status " + pdfResponse.getString("message")
                                + " "
                                + eleve.getString("idEleve") + " " + eleve.getString("lastName")));
                        return;
                    }
                    byte[] pdf = pdfResponse.getBinary("content");

                    if (templateProps.containsKey("image") && templateProps.getBoolean("image")) {
                        File pdfFile = new File(prefixPdfName + "_" + dateDebut + ".pdf");
                        OutputStream outStream = null;
                        try {
                            outStream = new FileOutputStream(pdfFile);
                        } catch (FileNotFoundException e) {
                            log.error("[DefaultExportBulletinService | getPdfRenderHandler 1 ]" + e.getMessage() + " "
                                    + eleve.getString("idEleve") + " " + eleve.getString("lastName"));
                            e.printStackTrace();
                            finalHandler.handle(new Either.Left("[DefaultExportBulletinService | getPdfRenderHandler 1 ]" + e.getMessage() + " "
                                    + eleve.getString("idEleve") + " " + eleve.getString("lastName")));
                            return;
                        }
                        try {
                            outStream.write(pdf);
                            handleCreateFile(pdfFile, outStream, templateProps, prefixPdfName, dateDebut, eleve, finalHandler);
                        } catch (IOException e) {
                            log.error("[DefaultExportBulletinService | getPdfRenderHandler 2] " + e.getMessage() + " " +
                                    eleve.getString("idEleve") + " " + eleve.getString("lastName"));
                            e.printStackTrace();
                            finalHandler.handle(new Either.Left("[DefaultExportBulletinService | IOException  ]" + e.getMessage() + " "
                                    + eleve.getString("idEleve") + " " + eleve.getString("lastName")));
                        }
                    } else {
                        Buffer buffer = Buffer.buffer(pdf);
                        savePdfDefault(buffer, eleve, finalHandler);
                    }
                }catch(Exception e){
                    finalHandler.handle(new Either.Left("[DefaultExportBulletinService | Exception  ]" + e.getMessage() + " "
                            + eleve.getString("idEleve") + " " + eleve.getString("lastName")));
                }
            }
        };
    }

    private void handleCreateFile(File pdfFile, OutputStream outStream, JsonObject templateProps, String prefixPdfName,
                                  String dateDebut, JsonObject eleve, Handler<Either<String, String>> finalHandler) {
        try {
            String sourceDir = pdfFile.getAbsolutePath();
            File sourceFile = new File(sourceDir);
            while (!sourceFile.exists()) {
                System.err.println(sourceFile.getName() + " File does not exist");
            }
            if (sourceFile.exists()) {
                PDDocument document = PDDocument.load(sourceDir);
                @SuppressWarnings("unchecked")
                List<PDPage> list = document.getDocumentCatalog().getAllPages();
                File imageFile = null;
                for (PDPage page : list) {
                    BufferedImage image = page.convertToImage();
                    int height = 150 + Integer.parseInt(templateProps.getString("nbrCompetences")) * 50;
                    BufferedImage SubImage = image.getSubimage(0, 0, 1684, height);
                    imageFile = new File(prefixPdfName + "_" + dateDebut + ".jpg");
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
                Buffer buffer = Buffer.buffer(bytes);
                savePdfDefault(buffer, eleve, finalHandler);
                bos.close();
                fis.close();

                Files.deleteIfExists(Paths.get(pdfFile.getAbsolutePath()));
                Files.deleteIfExists(Paths.get(imageFile.getAbsolutePath()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[DefaultExportBulletinService | handleCreateFile] : " + e.getMessage() + " "
                    + eleve.getString("idEleve") + " " + eleve.getString("lastName"));
            finalHandler.handle(new Either.Left<>(e.getMessage()));
        }
        finally {
            try {
                outStream.close();
            } catch (IOException e) {
                finalHandler.handle(new Either.Left<>(e.getMessage()));
            }
        }
    }

    private void getBase64File(String id, Future<String> future) {
        workspaceHelper.readDocument(id, document -> {
            if (document == null){
                log.error("Cannot load image in getBase64File for id : " + id);
                future.complete("");
            } else {
                String base64 = Base64.getEncoder().encodeToString(document.getData().getBytes());
                future.complete(base64);
            }
        });
    }
    @Override
    public void generateImagesFromPathForBulletin (JsonObject eleve, Vertx vertx, Handler<Either<String, JsonObject>> handler) {
        List<Future> futureList = new ArrayList<>();
        Future<String> imgStructureFuture = Future.future();
        Future<String> imgSignatureFuture = Future.future();
        Future<String> logoFuture = Future.future();

        futureList.add(imgStructureFuture);
        futureList.add(imgSignatureFuture);
        futureList.add(logoFuture);

        CompositeFuture.all(futureList).setHandler(new Handler<AsyncResult<CompositeFuture>>() {
            @Override
            public void handle(AsyncResult<CompositeFuture> event) {

                if (event.succeeded()) {
                    String imgStructureEncoded = imgStructureFuture.result();
                    String imgSignatureEncoded = imgSignatureFuture.result();
                    String logoEncoded = logoFuture.result();

                    eleve.put(IMG_SIGNATURE, imgSignatureEncoded);
                    eleve.put(IMG_STRUCTURE, imgStructureEncoded);
                    eleve.put("logoData", logoEncoded);
                    handler.handle(new Either.Right<>(new JsonObject()));
                } else {
                    handler.handle(new Either.Left<>("Error to get Image : " + event.cause().getMessage()));
                }
            }
        });
        if (eleve.getString(IMG_STRUCTURE) != null) {
            String[] structureLogoString =  eleve.getString(IMG_STRUCTURE).split("/");
            String structureLogoId = structureLogoString[structureLogoString.length - 1];
            getBase64File(structureLogoId, imgStructureFuture);
        } else {
            imgStructureFuture.complete("");
        }

        if (eleve.getString("imgSignature") != null) {
            String[] signatureSplit = eleve.getString("imgSignature").split("/");
            String signatureLogoId = signatureSplit[signatureSplit.length - 1];
            getBase64File(signatureLogoId, imgSignatureFuture);
        } else {
            imgSignatureFuture.complete("");
        }

        generatesImage(eleve, vertx, LOGO_PATH, logoFuture);
        eleve.put("hasImgLoaded", true);
    }

    public void generateImagesFromPathForBulletin (JsonObject params, Vertx vertx,Promise<Object> promise) {
        List<Future> futureList = new ArrayList<>();
        Future<String> imgStructureFuture = Future.future();
        Future<String> imgSignatureFuture = Future.future();
        Future<String> logoFuture = Future.future();

        futureList.add(imgStructureFuture);
        futureList.add(imgSignatureFuture);
        futureList.add(logoFuture);

        if(isNotNull(params.getValue(AGRICULTURE_LOGO)) && params.getBoolean(AGRICULTURE_LOGO)){
            params.put(LOGO_PATH, "img/ministere_agriculture.png");
        } else {
            params.put(LOGO_PATH, "img/education_nationale.png");
        }

        CompositeFuture.all(futureList).onSuccess( event -> {

            if (event.succeeded()) {
                String imgStructureEncoded = imgStructureFuture.result();
                String imgSignatureEncoded = imgSignatureFuture.result();
                String logoEncoded = logoFuture.result();

                JsonObject results = new JsonObject() ;
                results.put(IMG_SIGNATURE, imgSignatureEncoded);
                results.put(IMG_STRUCTURE, imgStructureEncoded);
                results.put("logoData", logoEncoded);
                promise.complete(results);
            } else {
                promise.fail("Error to get Image : " + event.cause().getMessage());
            }
        }).onFailure(event -> promise.fail("[Competences:generateImagesFromPathForBulletin] error when generate image " + event.getMessage() ));

        if (params.getString(IMG_STRUCTURE) != null) {
            String[] structureLogoString =  params.getString(IMG_STRUCTURE).split("/");
            String structureLogoId = structureLogoString[structureLogoString.length - 1];
            getBase64File(structureLogoId, imgStructureFuture);
        } else {
            imgStructureFuture.complete("");
        }

        if (params.getString("imgSignature") != null) {
            String[] signatureSplit = params.getString("imgSignature").split("/");
            String signatureLogoId = signatureSplit[signatureSplit.length - 1];
            getBase64File(signatureLogoId, imgSignatureFuture);
        } else {
            imgSignatureFuture.complete("");
        }
        generatesImage(params, vertx, LOGO_PATH, logoFuture);
    }

    @Override
    public void checkBulletinsExist(JsonArray students, Integer idPeriode, String idStructure, Handler<Either<String, Boolean>> handler) {

        JsonArray idsStudent = new JsonArray();
        JsonArray idsClasses = new JsonArray();
        for (int i = 0; i < students.size(); i++) {
            JsonObject student = students.getJsonObject(i);
            idsStudent.add(student.getString("id"));
            if (student.getString("idClasse") != null && !idsClasses.contains(student.getString("idClasse")))
                idsClasses.add(student.getString("idClasse"));
        }
        ;
        utilsService.getYearsAndPeriodes(idStructure, true, yearEvent -> {
            String idYear = yearEvent.right().getValue().getString("start_date").substring(0, 4);
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM notes.archive_bulletins WHERE id_eleve IN ").append(Sql.listPrepared(idsStudent.getList()))
                    .append(" AND id_classe IN ").append(Sql.listPrepared(idsClasses.getList())).append(" AND id_periode = ? ")
                    .append(" AND id_etablissement = ? AND id_annee = ? ;");
            JsonArray values = new JsonArray().addAll(idsStudent).addAll(idsClasses).add(idPeriode).add(idStructure).add(idYear);

            Sql.getInstance().prepared(query.toString(), values, event -> {
                JsonObject result = event.body();
                if (result.getString("status").equals("ok")) {
                    Integer response =
                            result.getInteger("rows");
                    handler.handle(new Either.Right<>(response != null && response > 0));

                } else {
                    handler.handle(new Either.Left<>(result.getString("status")));
                }
            });
        });
    }


    private JsonObject checkStatements(String idStudent, String idClasse, Integer idPeriode, String idYear) {
        String query = "SELECT 1 from " + Competences.EVAL_SCHEMA + ".archive_bulletins " +
                " WHERE id_classe = ? AND id_eleve = ? AND id_periode = ? AND id_annee = ? ; ";
        JsonArray params = new JsonArray().add(idClasse).add(idStudent).add(idPeriode).add(idYear);
        return  new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private void generatesImage(JsonObject eleve, Vertx vertx, String path, Future<String> logoFuture) {
        String image = "public/" + eleve.getString(path);
        try {
            final String imagePath = FileResolver.absolutePath(image);
            Buffer imageBuffer = vertx.fileSystem().readFileBlocking(imagePath);
            String encodedImage = "";
            try {
                encodedImage = new String(Base64.getMimeEncoder().encode(imageBuffer.getBytes()), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                log.error("[DefaultExportPDFService@generatePDF] An error occurred while encoding logo to base 64");
            }
            logoFuture.complete(encodedImage);
        }catch (FileSystemException ee){
            log.error("IMG FILE NOT FOUND");
            logoFuture.fail("IMG FILE NOT FOUND");
        }
    }

    private void savePdfDefault(Buffer buffer, JsonObject eleve, Handler<Either<String, String>> finalHandler) {
        savePdfInStorage(eleve, buffer, event -> {
            try {
                if (event.isLeft()) {
                    log.error("[DefaultExportBulletinService | savePdfDefault] : Error on savePdfInStorage "
                            + event.left().getValue() + " "
                            + eleve.getString("idEleve") + " " + eleve.getString("lastName"));
                    finalHandler.handle(new Either.Left<>("[DefaultExportBulletinService | savePdfDefault] : Error on savePdfInStorage "
                            + event.left().getValue() + " "
                            + eleve.getString("idEleve") + " " + eleve.getString("lastName")));
                } else {
                    finalHandler.handle(new Either.Right<>(event.right().getValue().getString("idFile")));
                }
            }catch (Exception e){
                finalHandler.handle(new Either.Left<>("[DefaultExportBulletinService | savePdfDefault] : Exception on savePdfInStorage "
                        + e.getMessage() + " "
                        + eleve.getString("idEleve") + " " + eleve.getString("lastName")));

            }
        });
    }
}
