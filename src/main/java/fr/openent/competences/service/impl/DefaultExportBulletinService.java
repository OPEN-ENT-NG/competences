package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.ImgLevel;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.service.*;
import fr.openent.competences.helpers.MustachHelper;
import fr.openent.competences.helpers.NodePdfGeneratorClientHelper;
import fr.openent.competences.utils.BulletinUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
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

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.getLibelle;
import static fr.openent.competences.Utils.isNotNull;
import static fr.openent.competences.Utils.isNull;
import static fr.openent.competences.service.impl.DefaultExportService.COEFFICIENT;
import static fr.openent.competences.service.impl.DefaultNoteService.*;
import static fr.openent.competences.utils.ArchiveUtils.getFileNameForStudent;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.openent.competences.helpers.NodePdfGeneratorClientHelper.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLTimeoutException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.openent.competences.utils.BulletinUtils.getIdParentForStudent;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

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
    private WorkspaceHelper workspaceHelper;

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
        defaultNiveauDeMaitriseService = new DefaultNiveauDeMaitriseService();
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE,eb);
        workspaceHelper = new WorkspaceHelper(eb,storage);
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
        defaultNiveauDeMaitriseService = new DefaultNiveauDeMaitriseService();
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE,eb);
        this.httpClient =  createHttpClient(vertx);
        workspaceHelper = new WorkspaceHelper(eb,storage);

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
                initClassObjectInfo(idClasse, idPeriode, params, elevesMap, answered, host, acceptLanguage, finalHandler,
                        elevesFuture, tableauDeConversionFuture, modelsLibelleFuture, useModel, vertx));
    }

    private Handler<AsyncResult<CompositeFuture>> initClassObjectInfo(String idClasse,
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
                }else{
                    JsonArray finalEleves = eleves;
                    getClasseInfo(idClasse, classeInfoEvent -> {
                        if(classeInfoEvent.isRight()){
                            log.info(classeInfoEvent.right().getValue());
                            classe.put("classeName", classeInfoEvent.right().getValue());

                            buildDataForStudent(answered, finalEleves, elevesMap, idPeriode, params, classe,
                                    showBilanPerDomaines, host, acceptLanguage, finalHandler,vertx);
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
                log.error(" :" + idElevesFuture.cause().getMessage());
                future.complete(null);
                return;
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

    public void saveParameters(JsonArray idStudents, Long idPeriode, String idStructure,
                               String paramsString, final Handler<Either<String, JsonObject>> finalHandler){
        JsonArray params = new JsonArray();
        StringBuilder query = new StringBuilder("INSERT INTO " + COMPETENCES_SCHEMA + ".bulletin_parameters " +
                "(id_student, id_periode, params, id_structure) VALUES ");
        for(Object student : idStudents){
            query.append(" ( ?, ?, ?, ?) ,");
            params.add((String)student).add(idPeriode).add(paramsString).add(idStructure);
        }
        query = new StringBuilder(query.substring(0, query.length() - 1));
        query.append(" ON CONFLICT (id_student, id_periode, id_structure) DO UPDATE SET params = ?");
        params.add(paramsString);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(finalHandler));
    }

    public void getParameters(String idStudent, Long idPeriode, String idStructure,
                              final Handler<Either<String, JsonObject>> finalHandler){
        String query = "SELECT params FROM " + COMPETENCES_SCHEMA + ".bulletin_parameters WHERE id_student = ? " +
                "AND id_periode = ? AND id_structure = ?";
        JsonArray params = new JsonArray().add(idStudent).add(idPeriode).add(idStructure);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(finalHandler));
    }

    private void logBegin(String method, String idEleve) {
        log.debug("------- [" + method + "]: " + idEleve + " DEBUT " );
    }

    @Override
    public void putLibelleForExport(String idEleve, Map<String , JsonObject> elevesMap, JsonObject params,
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
                    .put(HIDE_HEADTEACHER,params.getBoolean(HIDE_HEADTEACHER,false))
                    .put(ADD_OTHER_TEACHER,params.getBoolean(ADD_OTHER_TEACHER,false))
                    .put(FUNCTION_OTHER_TEACHER,params.getString(FUNCTION_OTHER_TEACHER,""))
                    .put(OTHER_TEACHER_NAME,params.getString(OTHER_TEACHER_NAME,""))
                    .put(AGRICULTURE_LOGO,params.getBoolean(AGRICULTURE_LOGO,false));

            JsonArray niveauCompetences;
            try{
                niveauCompetences   = (JsonArray) params.getValue(NIVEAU_COMPETENCE);

            }catch (java.lang.ClassCastException e){
                niveauCompetences = new JsonArray(params.getString(NIVEAU_COMPETENCE));
            }
            JsonArray footerArray = new JsonArray();
            if(niveauCompetences != null && !niveauCompetences.isEmpty()){
                for (int i = niveauCompetences.size() - 1; i >= 0; i--) { //reverse Array
                    footerArray.add(niveauCompetences.getJsonObject(i));
                }
            }


            String footer = "";
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


                    footer += id_niv + " : " + lib + " - ";
                }
                footer = footer.substring(0, footer.length() - 2);
            }

            eleve.put(NIVEAU_COMPETENCE, niveauCompetences).put("footer", "* " + footer);

            if(isNotNull(params.getValue(AGRICULTURE_LOGO)) && params.getBoolean(AGRICULTURE_LOGO)){
                eleve.put(LOGO_PATH,"img/ministere_agriculture.png");
            }else{
                eleve.put(LOGO_PATH,"img/education_nationale.png");
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
                                  Map<String, JsonObject> elevesMap, Long idPeriode, JsonObject params,
                                  final JsonObject classe, String host, String acceptLanguage,
                                  Vertx vertx, Handler<Either<String, JsonObject>> finalHandler){
        try {
            if (!answered.get()) {
                Boolean isBulletinLycee = (isNotNull(params.getValue("simple"))) ? params.getBoolean("simple") : false ;
                String beforeAvisConseil = params.getString("mentionOpinion") + " : ";
                String beforeAvisOrientation = params.getString("orientationOpinion");
                List<Future> futures = new ArrayList<>();

                Future<JsonObject> getSuiviAcquisFuture = Future.future();
                Future<JsonObject> putLibelleForExportFuture = Future.future();
                Future<JsonObject> getEvenementsFuture = Future.future();
                Future<JsonObject> getSyntheseBilanPeriodiqueFuture = Future.future();
                Future<JsonObject> getStructureFuture = Future.future();
                Future<JsonObject> getLibellePeriodeFuture = Future.future();
                Future<JsonObject> getAnneeScolaireFuture = Future.future();
                Future<JsonObject> getCycleFuture = Future.future();
                Future<JsonObject> getAppreciationCPEFuture = Future.future();
                Future<JsonObject> getAvisConseilFuture = Future.future();
                Future<JsonObject> getAvisOrientationFuture = Future.future();
                Future<JsonObject> getImagesBase64Future = Future.future();

                futures.add(getImagesBase64Future);
                futures.add(getSuiviAcquisFuture);
                futures.add(putLibelleForExportFuture);
                futures.add(getEvenementsFuture);
                futures.add(getSyntheseBilanPeriodiqueFuture);
                futures.add(getStructureFuture);
                futures.add(getLibellePeriodeFuture);
                futures.add(getAnneeScolaireFuture);
                futures.add(getCycleFuture);
                futures.add(getAppreciationCPEFuture);
                futures.add(getAvisConseilFuture);
                futures.add(getAvisOrientationFuture);


                if(!params.getBoolean(HIDE_HEADTEACHER, false)) {
                    Future<JsonObject> getHeadTeachersFuture = Future.future();
                    futures.add(getHeadTeachersFuture);
                    getHeadTeachers(idEleve, classe.getString(ID_CLASSE), elevesMap.get(idEleve), futureGetHandler(getHeadTeachersFuture));
                }

                if(params.getBoolean(GET_RESPONSABLE)) {
                    Future<JsonObject> getResponsablesFuture = Future.future();
                    futures.add(getResponsablesFuture);
                    getResponsables(idEleve, elevesMap, futureGetHandler(getResponsablesFuture));
                }

                if(params.getBoolean(SHOW_BILAN_PER_DOMAINE)) {
                    Future<JsonObject> getImageGraphFuture = Future.future();
                    Future<JsonObject> getArbreDomainesFuture = Future.future();
                    futures.add(getImageGraphFuture);
                    futures.add(getArbreDomainesFuture);
                    getImageGraph(idEleve, elevesMap, futureGetHandler(getImageGraphFuture));
                    getArbreDomaines(idEleve, classe.getString(ID_CLASSE), elevesMap, futureGetHandler(getArbreDomainesFuture));
                }

                if (params.getBoolean(SHOW_PROJECTS)) {
                    Future<JsonObject> getProjetsFuture = Future.future();
                    futures.add(getProjetsFuture);
                    getProjets(idEleve, classe.getString(ID_CLASSE), elevesMap, idPeriode, futureGetHandler(getProjetsFuture));
                }

                if(params.getValue(GET_DATA_FOR_GRAPH_DOMAINE_METHOD) != null){
                    if(params.getBoolean(GET_DATA_FOR_GRAPH_DOMAINE_METHOD)){
                        Future<JsonObject> getBilanPeriodiqueDomaineForGraphFuture = Future.future();
                        futures.add(getBilanPeriodiqueDomaineForGraphFuture);
                        getBilanPeriodiqueDomaineForGraph(idEleve, classe.getString(ID_CLASSE), idPeriode,
                                elevesMap, futureGetHandler(getBilanPeriodiqueDomaineForGraphFuture));
                    }
                }

                getSuiviAcquis(idEleve, elevesMap, idPeriode, classe, params, futureGetHandler(getSuiviAcquisFuture));
                putLibelleForExport(idEleve, elevesMap, params, futureGetHandler(putLibelleForExportFuture));
                getEvenements(params.getString("idStructure"), classe.getString(ID_CLASSE),
                        idEleve, elevesMap, idPeriode, futureGetHandler(getEvenementsFuture));
                getSyntheseBilanPeriodique(idEleve, elevesMap, idPeriode, params.getString("idStructure"),isBulletinLycee,
                        futureGetHandler(getSyntheseBilanPeriodiqueFuture));
                getStructure(idEleve, elevesMap.get(idEleve),   futureGetHandler(getStructureFuture));
                getLibellePeriode(idEleve, elevesMap, idPeriode, host, acceptLanguage, futureGetHandler(getLibellePeriodeFuture));
                getAnneeScolaire(idEleve, classe.getString(ID_CLASSE), elevesMap.get(idEleve), futureGetHandler(getAnneeScolaireFuture));
                getCycle(idEleve, classe.getString(ID_CLASSE), elevesMap,idPeriode, params.getLong(TYPE_PERIODE),
                        futureGetHandler(getCycleFuture));
                getAppreciationCPE(idEleve, elevesMap, idPeriode, futureGetHandler(getAppreciationCPEFuture));
                getAvisConseil(idEleve, elevesMap, idPeriode, params.getString("idStructure"),
                        futureGetHandler(getAvisConseilFuture), beforeAvisConseil);
                getAvisOrientation(idEleve, elevesMap, idPeriode, params.getString("idStructure"),
                        futureGetHandler(getAvisOrientationFuture), beforeAvisOrientation);
                generateImagesFromPath(elevesMap.get(idEleve), vertx,futureGetHandler(getImagesBase64Future) );

                CompositeFuture.all(futures).setHandler(event -> {
                    if (event.succeeded()) {
                        log.info("------------------"+idEleve + " end get datas for export bulletin  ---------------------");
                        finalHandler.handle(new Either.Right<>(null));
                    }else {
                        log.error("[Competences] at getExportBulletin error when getting datas for export bulletins : stuedent :" + idEleve);
                    }
                });
            }
            else {
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
                                                                   final AtomicBoolean answered,
                                                                   JsonObject params, Boolean forArchive,
                                                                   Future<JsonObject> future){
        return new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    CompositeFuture.all(elevesFuture, Future.succeededFuture()).setHandler( elevesEvent -> {
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
                            exportService.genererPdf(request, resultFinal, template, title, vertx, config);

                            JsonObject jsonRequest = new JsonObject()
                                    .put("headers", new JsonObject()
                                            .put("Accept-Language", request.headers().get("Accept-Language")))
                                    .put("Host", getHost(request));

                            JsonObject action = new JsonObject().put(ACTION, BulletinWorker.SAVE_BULLETIN)
                                    .put("request", jsonRequest)
                                    .put("resultFinal", resultFinal)
                                    .put("template", template)
                                    .put("title", title);

                            eb.send(BulletinWorker.class.getSimpleName(), action, Competences.DELIVERY_OPTIONS);
                        }
                    });
                }else{
                    log.error(event.left().getValue());
                    badRequest(request);
                    return;
                }
            }
        };
    }

    @Override
    public Handler<Either<String, JsonObject>> getFinalBulletinHandler(final HttpServerRequest request,
                                                                       Map<String, JsonObject> elevesMap,
                                                                       Vertx vertx, JsonObject config,
                                                                       Future<JsonArray> elevesFuture,
                                                                       final AtomicBoolean answered,
                                                                       JsonObject params) {
        return createFinalHandler(request, elevesMap, vertx, config, elevesFuture, answered, params,
                false,null);
    }

    @Deprecated
    public Handler<Either<String, JsonObject>> getFinalArchiveBulletinHandler(Map<String, JsonObject> elevesMap,
                                                                              Vertx vertx, JsonObject config,
                                                                              Future<JsonArray> elevesFuture,
                                                                              final AtomicBoolean answered,
                                                                              JsonObject params,
                                                                              Future<JsonObject> future) {
        return createFinalHandler(null, elevesMap, vertx, config, elevesFuture, answered, params,
                true, future);
    }

    private void getBilanPeriodiqueDomaineForGraph(final String idEleve,
                                                   final String idClasse,
                                                   final Long idPeriode,
                                                   Map<String, JsonObject> elevesMap,
                                                   Handler<Either<String, JsonObject>> finalHandler){

        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_DATA_FOR_GRAPH_DOMAINE_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        } else {
            String idEtablissement = eleveObject.getString(ID_ETABLISSEMENT_KEY);
            final Integer typeClasse = 0;
            final String idPeriodeString = idPeriode.toString();

            bilanPeriodiqueService.getBilanPeriodiqueDomaineForGraph(idEleve, idEtablissement, idClasse, typeClasse,
                    idPeriodeString,
                    new Handler<Either<String, JsonArray>>(){
                        private int count = 1;
                        private AtomicBoolean answer = new AtomicBoolean(false);

                        @Override
                        public void handle(Either<String, JsonArray> datasEvent){
                            if(datasEvent.isLeft()){
                                String error = datasEvent.left().getValue();
                                log.error("[" + GET_DATA_FOR_GRAPH_DOMAINE_METHOD + "] : " + error + " " + count);
                                if(error.contains(TIME)){
                                    count++;
                                    bilanPeriodiqueService.getBilanPeriodiqueDomaineForGraph(idEleve,idEtablissement,
                                            idClasse, typeClasse, idPeriodeString, this);
                                }
                                else{
                                    if (eleveObject.getJsonArray(ERROR) == null) {
                                        eleveObject.put(ERROR, new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray(ERROR);
                                    errors.add(GET_DATA_FOR_GRAPH_DOMAINE_METHOD);
                                    serviceResponseOK(answer, finalHandler, count, idEleve,
                                            GET_DATA_FOR_GRAPH_DOMAINE_METHOD);
                                }
                            }
                            else{
                                JsonArray domainsDatas = datasEvent.right().getValue();
                                String datas = "[";
                                for(int i=0; i< domainsDatas.size(); i++){
                                    datas += domainsDatas.getJsonObject(i).encode() + ",";
                                }
                                if(datas.length() > 1 ) datas = datas.substring(0, datas.length()-1);
                                datas += "]";
                                eleveObject.put("_data", datas);
                                log.info( "data put on jsobjectEleve " +datas );
                                serviceResponseOK(answer, finalHandler, count, idEleve,
                                        GET_DATA_FOR_GRAPH_DOMAINE_METHOD);
                            }
                        }
                    });
        }
    }

    @Override
    public void getCycle ( String idEleve, String idClasse, Map<String,JsonObject> elevesMap,Long idPeriode, Long typePeriode,
                           Handler<Either<String, JsonObject>> finalHandler) {
        JsonObject eleve = elevesMap.get(idEleve);
        logBegin(GET_CYCLE_METHOD, idEleve);
        if (eleve == null) {
            logStudentNotFound(idEleve, GET_CYCLE_METHOD);
            finalHandler.handle(new Either.Right<>(null));

        }
        else {

            if (idClasse == null) {
                log.error("[getCycle]| Object eleve doesn't contains field idClasse ");
                finalHandler.handle(new Either.Right<>(null));
            }
            else {
                JsonObject action = new JsonObject()
                        .put(ACTION, "eleve.getCycle")
                        .put(ID_CLASSE, idClasse);
                eb.send(Competences.VIESCO_BUS_ADDRESS, action,Competences.DELIVERY_OPTIONS,
                        handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                            private int count = 1;
                            private AtomicBoolean answer = new AtomicBoolean(false);
                            @Override
                            public void handle(Message<JsonObject> result) {
                                JsonObject body = result.body();
                                if (!"ok".equals(body.getString(STATUS))) {
                                    String message =  body.getString(MESSAGE);
                                    log.error("[getCycle] : " + idEleve + " " + message + count);

                                    buildErrorReponseForEb (idEleve, message, answer, count, action,
                                            this, finalHandler, eleve, GET_LIBELLE_PERIOD_METHOD);
                                }
                                else{
                                    JsonArray results = body.getJsonArray(RESULTS);
                                    if(results.size() > 0) {
                                        final String libelle = results.getJsonObject(0)
                                                .getString(LIBELLE);
                                        eleve.put("bilanCycle",
                                                getLibelle("evaluations.bilan.periodique.of." + typePeriode)
                                                        + libelle);
                                    }
                                    else {
                                        log.error(GET_CYCLE_METHOD + "  " + idEleve + "| no link to cycle for object " +
                                                idClasse);
                                    }
                                    serviceResponseOK(answer, finalHandler, count, idEleve, GET_CYCLE_METHOD);
                                }
                            }
                        }));
            }

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


    @Override
    public void getAnneeScolaire(String idEleve, String idClasse,
                                 JsonObject eleve,
                                 Handler<Either<String, JsonObject>> finalHandler) {
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

    private void getImageGraph(String idEleve, Map<String, JsonObject> elevesMap,
                               Handler<Either<String, JsonObject>> finalHandler) {
        JsonObject eleve = elevesMap.get(idEleve);
        if (eleve == null) {
            logStudentNotFound(idEleve, GET_IMAGE_GRAPH_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        } else {
            String idFile = eleve.getString(GRAPH_PER_DOMAINE);

            if(idFile != null) {
                storage.readFile(idFile, new Handler<Buffer>() {
                    private int count = 1;
                    private AtomicBoolean answer = new AtomicBoolean(false);

                    @Override
                    public void handle(Buffer eventBuffer) {
                        if (eventBuffer != null) {
                            String graphPerDomaine = eventBuffer.getString(0, eventBuffer.length());

                            eleve.remove(GRAPH_PER_DOMAINE);
                            eleve.put(GRAPH_PER_DOMAINE, graphPerDomaine);

                            serviceResponseOK(answer, finalHandler, count, idEleve, GET_IMAGE_GRAPH_METHOD);
                        } else {
                            log.error("["+ GET_IMAGE_GRAPH_METHOD + "] : " + idEleve + " fail " + count);
                        }

                    }
                });
            }
            else {
                log.debug("["+ GET_IMAGE_GRAPH_METHOD + "] : File not found ");
                finalHandler.handle(new Either.Right<>(null));
            }
        }
    }

    @Override
    public void getAvisConseil(String idEleve, Map<String, JsonObject> elevesMap, Long idPeriode, String idStructure,
                               Handler<Either<String, JsonObject>> finalHandler, String beforeAvisConseil) {
        logBegin(GET_AVIS_CONSEIL_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_AVIS_CONSEIL_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }else{
            avisConseilService.getAvisConseil(idEleve, idPeriode, idStructure, new Handler<Either<String, JsonArray>>() {
                private int count = 1;
                private AtomicBoolean answer = new AtomicBoolean(false);

                @Override
                public void handle(Either<String, JsonArray> event) {
                    if(event.isLeft()){
                        String message = event.left().getValue();
                        log.error("[getAvisConseil ] : " + idEleve  + " " + message + " " + count);
                        if (message.contains(TIME)) {
                            count++;
                            avisConseilService.getAvisConseil(idEleve, idPeriode, idStructure, this);
                        }
                        else {
                            if (eleveObject.getJsonArray(ERROR) == null) {
                                eleveObject.put(ERROR, new JsonArray());
                            }
                            JsonArray errors = eleveObject.getJsonArray(ERROR);
                            errors.add(GET_AVIS_CONSEIL_METHOD);
                            serviceResponseOK(answer, finalHandler, count, idEleve, GET_AVIS_CONSEIL_METHOD);
                        }
                    } else {
                        JsonArray result = event.right().getValue();
                        JsonObject avisConseil = new JsonObject();
                        if(!result.isEmpty())
                            avisConseil = result.getJsonObject(0);
                        if(avisConseil != null && !avisConseil.isEmpty()) {
                            eleveObject.put("beforeAvisConseil", beforeAvisConseil);

                            eleveObject.put("avisConseil", avisConseil.getString(LIBELLE))
                                    .put("hasAvisConseil",true);
                        }
                        serviceResponseOK(answer, finalHandler, count, idEleve, GET_AVIS_CONSEIL_METHOD);
                    }
                }
            });
        }
    }

    @Override
    public void getAvisOrientation(String idEleve, Map<String, JsonObject> elevesMap, Long idPeriode, String idStructure,
                                   Handler<Either<String, JsonObject>> finalHandler, String beforeAvisOrientation) {
        logBegin(GET_AVIS_ORIENTATION_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_AVIS_ORIENTATION_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }else{
            avisOrientationService.getAvisOrientation(idEleve, idPeriode, idStructure, new Handler<Either<String, JsonArray>>() {
                private int count = 1;
                private AtomicBoolean answer = new AtomicBoolean(false);

                @Override
                public void handle(Either<String, JsonArray> event) {
                    if(event.isLeft()){
                        String message = event.left().getValue();
                        log.error("[getAvisOrientation ] : " + idEleve  + " " + message + " " + count);
                        if (message.contains(TIME)) {
                            count++;
                            avisOrientationService.getAvisOrientation(idEleve, idPeriode, idStructure,this);
                        }
                        else {
                            if (eleveObject.getJsonArray(ERROR) == null) {
                                eleveObject.put(ERROR, new JsonArray());
                            }
                            JsonArray errors = eleveObject.getJsonArray(ERROR);
                            errors.add(GET_AVIS_ORIENTATION_METHOD);
                            serviceResponseOK(answer, finalHandler, count, idEleve, GET_AVIS_ORIENTATION_METHOD);
                        }
                    }else{
                        JsonArray result = event.right().getValue();
                        JsonObject avisOrientation = new JsonObject();
                        if(!result.isEmpty())
                            avisOrientation = result.getJsonObject(0);
                        if(avisOrientation != null && !avisOrientation.isEmpty() ) {
                            eleveObject.put("avisOrientation",avisOrientation.getString(LIBELLE))
                                    .put("hasAvisOrientation",true);

                            eleveObject.put("beforeAvisOrientation", beforeAvisOrientation);
                        }
                        serviceResponseOK(answer, finalHandler, count, idEleve, GET_AVIS_ORIENTATION_METHOD);
                    }
                }
            });
        }
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
    public void getProjets (String idEleve, String idClasse, Map<String,JsonObject> elevesMap,Long idPeriode,
                            Handler<Either<String, JsonObject>> finalHandler) {
        JsonObject eleveObject = elevesMap.get(idEleve);
        logBegin(GET_PROJECTS_METHOD, idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_PROJECTS_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            // gets Projects
            String idEtablissement = eleveObject.getString(ID_ETABLISSEMENT_KEY);

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
                                            Arrays.asList(idClasse), idEtablissement, this);
                                }
                                else {
                                    if (eleveObject.getJsonArray(ERROR) == null) {
                                        eleveObject.put(ERROR, new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray(ERROR);
                                    errors.add(GET_PROJECTS_METHOD);
                                    serviceResponseOK(answer, finalHandler, count, idEleve, GET_PROJECTS_METHOD);
                                }

                            }
                            else {
                                if (count > 1 ) {
                                    log.debug("[getProjets] : " + idEleve + " success " + count);
                                }
                                List<String> idClasses = new ArrayList<String>();
                                idClasses.add(idClasse);
                                JsonArray elementBilanPeriodique = event.right().getValue();
                                List<String> idElements = new ArrayList<String>();
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
                                    serviceResponseOK(answer, finalHandler, count, idEleve, GET_PROJECTS_METHOD);
                                }
                                else {
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
                                            }
                                            else if (2L == typeElement) {
                                                element.put("hasLibelle", true);
                                                if(ap.getJsonArray("elements") == null) {
                                                    ap.put("elements", new JsonArray().add(element));
                                                }
                                                else {
                                                    ap.getJsonArray("elements").add(element);
                                                }
                                            }
                                            else if (1L == typeElement) {
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
                                    eleveObject.put("projects", new JsonArray().add(epi).add(ap).add(parcours));
                                    if(idElements.size() > 0) {
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
                                                                if (eleveObject.getJsonArray(ERROR) == null) {
                                                                    eleveObject.put(ERROR, new JsonArray());
                                                                }
                                                                JsonArray errors = eleveObject
                                                                        .getJsonArray(ERROR);
                                                                errors.add(GET_PROJECTS_METHOD);
                                                                serviceResponseOK(answer,finalHandler,
                                                                        count, idEleve, GET_PROJECTS_METHOD);
                                                            }
                                                        }
                                                        else {
                                                            JsonArray appreciations = event.right().getValue();
                                                            for(int i=0; i< appreciations.size(); i++) {
                                                                JsonObject app = appreciations.getJsonObject(i);
                                                                Long periodeId = app.getLong(ID_PERIODE);
                                                                if(periodeId == idPeriode) {
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
                                                            serviceResponseOK(answer, finalHandler,
                                                                    count, idEleve, GET_PROJECTS_METHOD);
                                                        }
                                                    }
                                                });
                                    }
                                    else {
                                        log.debug(" [getProjets] | NO elements founds for classe " + idClasse);
                                        serviceResponseOK(answer,finalHandler, count, idEleve, GET_PROJECTS_METHOD);
                                    }
                                }

                            }
                        }
                    });
        }
    }

    @Override
    public void getSyntheseBilanPeriodique ( String idEleve,  Map<String,JsonObject> elevesMap, Long idPeriode,
                                             String idStructure, Boolean isBulletinLycee, Handler<Either<String, JsonObject>> finalHandler) {
        JsonObject eleveObject = elevesMap.get(idEleve);
        logBegin(GET_SYNTHESE_BILAN_PERIO_METHOD, idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_SYNTHESE_BILAN_PERIO_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
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
                                    if (eleveObject.getJsonArray(ERROR) == null) {
                                        eleveObject.put(ERROR, new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray(ERROR);
                                    errors.add(GET_SYNTHESE_BILAN_PERIO_METHOD);
                                    serviceResponseOK(answer, finalHandler, count, idEleve,
                                            GET_SYNTHESE_BILAN_PERIO_METHOD);
                                }
                            }
                            else {
                                JsonArray result = event.right().getValue();
                                JsonObject synthese = new JsonObject();
                                if(!result.isEmpty())
                                    synthese = result.getJsonObject(0);
                                if (synthese != null && !synthese.isEmpty()) {
                                    String syntheseStr = synthese.getString("synthese");
                                    eleveObject.put("syntheseBilanPeriodque",troncateLibelle(syntheseStr,
                                            MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE));
                                    eleveObject.put("syntheseBilanPeriodqueStyle",fontSize(syntheseStr,
                                            MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE));
                                    if(isBulletinLycee) eleveObject.put("beforeSyntheseBP", BEFORE_SYNTHESE_BP);
                                }
                                serviceResponseOK(answer, finalHandler, count, idEleve,
                                        GET_SYNTHESE_BILAN_PERIO_METHOD);
                            }
                        }
                    });
        }

    }

    public void getAppreciationCPE (String idEleve,  Map<String,JsonObject> elevesMap, Long idPeriode,
                                    Handler<Either<String, JsonObject>> finalHandler){
        logBegin(GET_APPRECIATION_CPE_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_APPRECIATION_CPE_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            appreciationCPEService.getAppreciationCPE(idPeriode, idEleve, new Handler<Either<String, JsonObject>>() {
                private int count = 1;
                private AtomicBoolean answer = new AtomicBoolean(false);
                @Override
                public void handle(Either<String, JsonObject> event) {
                    if (event.isLeft()) {
                        String message = " " + event.left().getValue();
                        log.error("[" + GET_APPRECIATION_CPE_METHOD + "] : " + idEleve + " " + message + " " + count);
                        if (message.contains(TIME) && !answer.get()) {
                            count++;
                            appreciationCPEService.getAppreciationCPE(idPeriode, idEleve, this);
                        }
                        else {
                            if (eleveObject.getJsonArray(ERROR) == null) {
                                eleveObject.put(ERROR, new JsonArray());
                            }
                            JsonArray errors = eleveObject.getJsonArray(ERROR);
                            errors.add(GET_APPRECIATION_CPE_METHOD);
                            serviceResponseOK(answer, finalHandler, count, idEleve, GET_APPRECIATION_CPE_METHOD);
                        }
                    } else {
                        JsonObject appreciationCPE = event.right().getValue();

                        if (appreciationCPE != null) {
                            String app = troncateLibelle(appreciationCPE.getString(APPRECIATION_KEY),
                                    MAX_SIZE_APPRECIATION_CPE);
                            eleveObject.put("appreciationCPE",app)
                                    .put("appreciationCPEStyle",fontSize(app,  MAX_SIZE_APPRECIATION_CPE));
                        }
                        serviceResponseOK(answer, finalHandler, count, idEleve, GET_APPRECIATION_CPE_METHOD);
                    }
                }
            });
        }
    }
    @Override
    public void getStructure( String idEleve, JsonObject eleveObject,
                              Handler<Either<String, JsonObject>> finalHandler) {
        logBegin(GET_STRUCTURE_METHOD, idEleve);
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

                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
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

    @Override
    public void getResponsables( String idEleve, Map<String,JsonObject> elevesMap,
                                 Handler<Either<String, JsonObject>> finalHandler) {

        logBegin(GET_RESPONSABLE_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_RESPONSABLE_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
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
                                log.error("[" + GET_RESPONSABLE_METHOD + "] : " + idEleve + " " + mess + " " + count);

                                buildErrorReponseForEb(idEleve, mess, answer, count, action,
                                        this, finalHandler, eleveObject,
                                        GET_RESPONSABLE_METHOD);
                            } else {
                                JsonArray responsables = body.getJsonArray(RESULTS);
                                eleveObject.put("responsables", responsables);
                                serviceResponseOK(answer, finalHandler, count, idEleve, GET_RESPONSABLE_METHOD);
                            }
                        }
                    }));
        }
    }

    @Override
    public void getEvenements(String idStructure, String idClasse, String idEleve,Map<String, JsonObject> elevesMap, Long idPeriode,
                              Handler<Either<String, JsonObject>> finalHandler ) {

        logBegin(GET_EVENEMENT_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null || idStructure == null || idClasse == null) {
            if(eleveObject == null)
                logStudentNotFound(idEleve, GET_EVENEMENT_METHOD);
            else if (idStructure == null)
                logidEtabNotFound(idEleve,GET_EVENEMENT_METHOD);
            else
                logidClasseNotFound(idEleve,GET_EVENEMENT_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            bilanPeriodiqueService.getRetardsAndAbsences(idStructure, idClasse, idEleve, new Handler<Either<String, JsonArray>>() {
                private int count = 1;
                private AtomicBoolean answer = new AtomicBoolean(false);

                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isLeft()) {
                        String message = event.left().getValue();

                        if (message.contains(TIME) && !answer.get()) {
                            count++;
                            bilanPeriodiqueService.getRetardsAndAbsences(idStructure, idClasse, idEleve, this);

                        }
                        else {
                            if (eleveObject.getJsonArray(ERROR) == null) {
                                eleveObject.put(ERROR, new JsonArray());
                            }
                            JsonArray errors = eleveObject.getJsonArray(ERROR);
                            errors.add(GET_EVENEMENT_METHOD);
                            serviceResponseOK(answer,finalHandler, count, idEleve, GET_EVENEMENT_METHOD);
                        }
                    }
                    else {
                        JsonArray evenements = event.right().getValue();
                        if (eleveObject != null) {

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

                            JsonArray evenementsArray = new JsonArray()
                                    .add(getLibelle("viescolaire.retards") + ": [" + retard + "]")

                                    .add(getLibelle("evaluations.export.bulletin.asbence.just") + ": [" +
                                            absJust + "]" + getLibelle("half.days"))


                                    .add(getLibelle("evaluations.export.bulletin.asbence.not.just") + ": [" +
                                            absNonJust + "] " + getLibelle("half.days"))


                                    .add(getLibelle("evaluations.export.bulletin.asbence.nb.heures") + ": [" +
                                            absTotaleHeure + "]" + getLibelle("hours"));

                            eleveObject.put("evenements", evenementsArray);
                        }
                        serviceResponseOK(answer, finalHandler, count, idEleve, GET_EVENEMENT_METHOD);
                    }
                }
            });

        }
    }

    private String troncateLibelle(String libelle, int max ) {

        if(libelle == null) {
            libelle = "";
        }
        else if (libelle.length() > max) {
            libelle = libelle.substring(0, max);
            libelle += "...";
        }
        return libelle;
    }
    private String fontSize(String libelle, int max ) {

        if(libelle == null) {
            return  "";
        }
        else if (libelle.length() < max/2) {
            return "font-size: 10px !important;";
        }
        else if (libelle.length() <= max) {
            return "font-size: 8.5px !important;";
        }
        return "";
    }
    private String fontSizeProject(String libelle, int max ) {

        if(libelle == null) {
            return  "";
        }
        else if (libelle.length() < max/2) {
            return "font-size: 10px !important;";
        }
        else if (libelle.length() <= max) {
            return "font-size: 7.5px !important;";
        }
        return "";
    }

    // La taille de la police varie en fonction du nombre de matières affichées, du fait qu'on affiche la colonne des
    // éléments du programme et aussi du nombre de caractères de l'appréciation ou du libellé des éléments du programme
    private void setFontSizeOfSuivi (JsonArray subjects, boolean withProgramElement) {
        String defaultValue = "font-size: auto !important;";
        String value = defaultValue;
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
            }
            else {
                maxCaractere = appreciation.length();
            }
            if (maxCaractere > (MAX_SIZE_LIBELLE /2 + 24)) {
                nbSubjectOnLimit ++;
            }
        }

        int nbSubjectUnderLimit = nbSubject - nbSubjectOnLimit;

        if ((nbSubjectOnLimit <= 6 && nbSubject <= 6)
                || (nbSubject <= 6 && withProgramElement)
                || (!withProgramElement && nbSubjectOnLimit <= 10)) {
            value = defaultValue;
        }
        else {
            if (7 == nbSubject
                    || (nbSubject <= 11 && nbSubjectUnderLimit >= nbSubject -1 )) {
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
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
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

        DecimalFormat decimalFormat = new DecimalFormat("#.00");
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
    public void getSuiviAcquis(String idEleve,Map<String, JsonObject> elevesMap, Long idPeriode, JsonObject classe,
                               JsonObject params, Handler<Either<String, JsonObject>> finalHandler) {
        boolean getProgrammeElement = params.getBoolean(GET_PROGRAM_ELEMENT);

        logBegin(GET_SUIVI_ACQUIS_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_SUIVI_ACQUIS_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        } else {
            String idEtablissement = eleveObject.getString(ID_ETABLISSEMENT_KEY);
            String idClasse = classe.getString(ID_CLASSE);
            if (idClasse == null || idEtablissement == null) {
                if(idClasse == null) {
                    logidClasseNotFound(idEleve, GET_SUIVI_ACQUIS_METHOD);
                }
                if (idEtablissement == null) {
                    logidEtabNotFound(idEleve, GET_SUIVI_ACQUIS_METHOD);
                }
                finalHandler.handle(new Either.Right<>(null));
            } else {
                bilanPeriodiqueService.getSuiviAcquis(idEtablissement, idPeriode, idEleve,
                        idClasse, getSuiviAcquisHandler(idEleve, idPeriode, classe, params, finalHandler,
                                getProgrammeElement, eleveObject, idEtablissement, idClasse));
            }
        }
    }

    private Handler<Either<String, JsonArray>> getSuiviAcquisHandler(String idEleve, Long idPeriode, JsonObject classe, JsonObject params,
                                                                     Handler<Either<String, JsonObject>> finalHandler, boolean getProgrammeElement,
                                                                     JsonObject eleveObject, String idEtablissement, String idClasse) {
        return new Handler<Either<String, JsonArray>>() {
            private int count = 1;
            private AtomicBoolean answer = new AtomicBoolean(false);

            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isLeft()) {
                    String message = event.left().getValue();

                    if (message.contains(TIME) && !answer.get()) {
                        count ++;
                        bilanPeriodiqueService.getSuiviAcquis(idEtablissement, idPeriode, idEleve, idClasse,this);
                    } else {
                        log.error("["+ GET_SUIVI_ACQUIS_METHOD + "] :" + idEleve + " " + message + count);
                        if (eleveObject.getJsonArray(ERROR) == null) {
                            eleveObject.put(ERROR, new JsonArray());
                        }
                        JsonArray errors = eleveObject.getJsonArray(ERROR);
                        errors.add(GET_SUIVI_ACQUIS_METHOD);
                        serviceResponseOK(answer, finalHandler, count, idEleve, GET_SUIVI_ACQUIS_METHOD);
                        try {
                            finalize();
                        } catch (Throwable throwable) {
                            log.error(GET_SUIVI_ACQUIS_METHOD + " :: " + throwable.getMessage());
                        }
                    }
                } else {
                    JsonArray suiviAcquis = event.right().getValue();
                    JsonArray res = new JsonArray();

                    Utils.getElevesClasse(eb, idClasse, idPeriode, elevesEvent -> {
                        if(elevesEvent.isLeft()) {
                            log.error(GET_SUIVI_ACQUIS_METHOD + " :: " + elevesEvent.left().getValue());
                            serviceResponseOK(answer, finalHandler, count, idEleve, GET_SUIVI_ACQUIS_METHOD);
                        } else {
                            JsonArray idEleves = new JsonArray(elevesEvent.right().getValue().stream()
                                    .map(e -> ((JsonObject) e).getString(ID_ELEVE_KEY)).collect(Collectors.toList()));
                            if(suiviAcquis != null){
                                // On considèrera qu'on a un suivi des acquis si on affiche au moins une matière
                                for (int i = 0; i < suiviAcquis.size() ; i++) {
                                    final JsonObject matiere = suiviAcquis.getJsonObject(i);

                                    if(params.getBoolean(NEUTRE, false)){
                                        eleveObject.put(BACKGROUND_COLOR, "#ffffff");
                                        matiere.put(BACKGROUND_COLOR, "#ffffff");
                                    } else{
                                        matiere.put(BACKGROUND_COLOR, (res.size()%2 == 0) ? "#E2F0FA" : "#EFF7FC");
                                    }
                                    // Une matière sera affichée si on a au moins un élement sur la période
                                    buildMatiereForSuiviAcquis(matiere, idPeriode, classe, params);
                                    checkCoefficientConflict(eleveObject, matiere.getJsonObject(COEFFICIENT), params);
                                    if(matiere.getBoolean(PRINT_MATIERE_KEY)) {
                                        res.add(matiere);
                                    }
                                }
                                setFontSizeOfSuivi(res, getProgrammeElement);

                                setMoyenneGenerale(eleveObject, suiviAcquis, params, idPeriode, idEleve, idEleves);
                                setMoyenneAnnuelle(eleveObject, suiviAcquis, params);
                            }

                            eleveObject.put("suiviAcquis", res).put("hasSuiviAcquis", res.size() > 0);

                            serviceResponseOK(answer, finalHandler, count, idEleve, GET_SUIVI_ACQUIS_METHOD);
                            try {
                                finalize();
                            } catch (Throwable throwable) {
                                log.error(GET_SUIVI_ACQUIS_METHOD + " :: " + throwable.getMessage());
                            }
                        }
                    });
                }
            }
        };
    }

    @Override
    public void getArbreDomaines(String idEleve, String idClasse, Map<String, JsonObject> elevesMap,
                                 Handler<Either<String, JsonObject>> finalHandler){

        logBegin(GET_ARBRE_DOMAINE_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_ARBRE_DOMAINE_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            if (isNull(idClasse)) {
                logidClasseNotFound(idEleve, GET_ARBRE_DOMAINE_METHOD);
                finalHandler.handle(new Either.Right<>(null));
            } else {

                domainesService.getArbreDomaines(idClasse, idEleve, null,
                        new Handler<Either<String, JsonArray>>() {
                            private int count = 1;
                            private AtomicBoolean answer = new AtomicBoolean(false);

                            @Override
                            public void handle(Either<String, JsonArray> event) {
                                if (event.isLeft()) {
                                    String message = event.left().getValue();

                                    if (message.contains(TIME) && !answer.get()) {
                                        count++;
                                        domainesService.getArbreDomaines(idClasse, idEleve, null, this);

                                    } else {
                                        if (eleveObject.getJsonArray(ERROR) == null) {
                                            eleveObject.put(ERROR, new JsonArray());
                                        }
                                        JsonArray errors = eleveObject.getJsonArray(ERROR);
                                        errors.add(GET_ARBRE_DOMAINE_METHOD);
                                        serviceResponseOK(answer, finalHandler, count, idEleve,
                                                GET_ARBRE_DOMAINE_METHOD);
                                    }
                                } else {
                                    JsonArray domaines = event.right().getValue();
                                    JsonArray domainesToDisplay = new JsonArray();

                                    Map<Long,Boolean> idDomaineParent = new HashMap<>();
                                    for (int i=0; i<domaines.size(); i++) {
                                        JsonObject domaine = domaines.getJsonObject(i);
                                        Boolean isEvaluable = domaine.getBoolean(EVALUATED);
                                        Long idParent = domaine.getLong(ID_PARENT);
                                        Boolean isDomaineParent = (idParent == 0L);
                                        domaine.put(IS_DOMAINE_PARENT, isDomaineParent);

                                        if (isDomaineParent == true){
                                            idDomaineParent.put(domaine.getLong(ID), isEvaluable);
                                        }
                                        if (isEvaluable == true) {
                                            if (isDomaineParent == true) {
                                                domainesToDisplay.add(domaine);
                                            }
                                            else if (idDomaineParent.get(idParent) == false){
                                                domainesToDisplay.add(domaine);
                                            }
                                        }
                                    }
                                    eleveObject.put("domaines", domainesToDisplay);
                                    serviceResponseOK(answer, finalHandler, count, idEleve, GET_ARBRE_DOMAINE_METHOD);
                                }
                            }
                        });
            }
        }
    }

    private JsonObject getObjectForPeriode(JsonArray array, Long idPeriode, String key) {
        return utilsService.getObjectForPeriode(array, idPeriode, key);
    }

    private void setLibelleMatiere(JsonObject matiere, JsonArray models){
        String idMatiere =  matiere.getString(ID_MATIERE);

        for (int i=0; i<models.size(); i++) {
            JsonObject libelleSubject = models.getJsonObject(i);
            String id = libelleSubject.getString("id");
            if (id.equals(idMatiere)){
                String libelleMatiere = libelleSubject.getString(LIBELLE);
                matiere.remove(LIBELLE_MATIERE);
                matiere.put(LIBELLE_MATIERE, libelleMatiere);
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

    private void buildSousMatieres(JsonObject matiere, JsonArray tableauDeConversion, Long idPeriode, JsonObject params){
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
     * @param matiere matière à traiter
     * @param idPeriode période sélectionnée
     * @param classe JsonObject contenant les informations de la classe de l'élève dont on contruit le bulletin
     */
    private void buildMatiereForSuiviAcquis(final JsonObject matiere, Long idPeriode, final JsonObject classe,
                                            JsonObject params) {
        boolean printMatiere = false;

        JsonArray models = classe.getJsonArray("models");
        if (models != null && !models.isEmpty()){
            setLibelleMatiere(matiere, models);
        }

        matiere.put(PRINT_SOUS_MATIERES, params.getBoolean(PRINT_SOUS_MATIERES));
        setPrintCoefficient(matiere, params);
        JsonObject moyenneEleve = getObjectForPeriode(matiere.getJsonArray(MOYENNES), idPeriode, ID_KEY);
        JsonObject moyenneClasse = getObjectForPeriode(matiere.getJsonArray("moyennesClasse"), idPeriode, ID_KEY);
        JsonObject positionnement = getObjectForPeriode(matiere.getJsonArray(POSITIONNEMENTS_AUTO), idPeriode,
                ID_PERIODE);
        JsonObject positionnementFinal = getObjectForPeriode(matiere.getJsonArray("positionnementsFinaux"),
                idPeriode, ID_PERIODE);
        JsonObject res = getObjectForPeriode(matiere.getJsonArray("appreciations"), idPeriode, ID_PERIODE);
        JsonObject moyenneFinale = getObjectForPeriode(matiere.getJsonArray("moyennesFinales"), idPeriode,
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
            matiere.put(MOYENNE_ELEVE, moyenneFinale.getValue("moyenneFinale"));
        } else {
            if (isNotNull(moyenneEleve)) {
                printMatiere = true;
                matiere.put(MOYENNE_ELEVE, moyenneEleve.getValue(MOYENNE));
            } else{
                matiere.put(MOYENNE_ELEVE, NN);
            }
        }

        JsonArray tableauDeconversion = classe.getJsonArray("tableauDeConversion");
        if (positionnementFinal != null) {
            printMatiere = true;
            int posFinal = positionnementFinal.getInteger("positionnementFinal");
            if(posFinal == 0)
                matiere.put(POSITIONNEMENT, NN);
            else
                matiere.put(POSITIONNEMENT, posFinal);
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

                matiere.put(POSITIONNEMENT, val);
            }
        }

        // Mise Remplissage des données des sousMatières
        buildSousMatieres(matiere, tableauDeconversion, idPeriode , params);

        String elementsProgramme = troncateLibelle(matiere.getString(ELEMENTS_PROGRAMME), MAX_SIZE_LIBELLE);

        String app = "";

        if(appreciation != null) {
            app = troncateLibelle(appreciation.getString(APPRECIATION_KEY), MAX_SIZE_LIBELLE);
            printMatiere = true;
        }

        // Construction des libelles et de leur style.
        matiere.put(ELEMENTS_PROGRAMME, elementsProgramme)
                .put(MOYENNE_CLASSE, (moyenneClasse != null) ? moyenneClasse.getValue(MOYENNE) : NN)
                .put(APPRECIATION_KEY, app)
                .put(PRINT_MATIERE_KEY, printMatiere);

        JsonArray teachers = matiere.getJsonArray("teachers");

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
            }
            else {
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
                                String addressResponsaleToCheck =
                                        responsableToCheck.getString(ADDRESSE_POSTALE);
                                String addressResponsale =
                                        responsable.getString(ADDRESSE_POSTALE);
                                String lastNameResponsableToCheck = responsableToCheck.getString("responsableLastName",
                                        "");
                                String lastNameResponsable = responsable.getString("responsableLastName",
                                        "");
                                String civiliteResponsableToCheck = responsableToCheck.getString("civilite");
                                String civiliteResponsable = responsable.getString("civilite");
                                String newLastNameResponsableToCheck = new String();

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


                                    } else {//if same adress with different lastName
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
            }
            else {
                city = zipCode + " " + city;
            }

        } catch(ClassCastException e){
            String zipCode = String.valueOf(responsable.getInteger("zipcode"));
            if (zipCode == null) {
                zipCode = " ";
            }

            if (city == null) {
                city = zipCode;
            }
            else {
                city = zipCode + " " + city;
            }
        }

        if (civilite == null) {
            civilite = "M.";
        }

        JsonArray responsableLibelle = new JsonArray().add( civilite + " " + lastName  + " " + firstName );
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
        res.put("civilite",civilite);
        if("M.".equals(civilite)){
            res.put("responsableFirstName",firstName);
        }
        res.put("relativesHaveTwoNames",relativesHaveTwoNames);
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
        if(birthDate!= null) {

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
            if(hasGraphPerDomaine) {
                eleve.put(GRAPH_PER_DOMAINE, img);
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

    public void buildDataForStudent(final AtomicBoolean answered, JsonArray eleves,
                                    Map<String, JsonObject> elevesMap, Long idPeriode, JsonObject params,
                                    final JsonObject classe, Boolean showBilanPerDomaines, String host,
                                    String acceptLanguage, Handler<Either<String, JsonObject>> finalHandler, Vertx vertx){

        JsonObject images = params.getJsonObject("images");
        Long typePeriode = params.getLong(TYPE_PERIODE);
        List<Future> futures = new ArrayList<>();

        for (int i = 0; i < eleves.size(); i++) {
            futures.add(Future.future());
        }
        for (int i = 0; i < eleves.size(); i++) {
            JsonObject eleve = eleves.getJsonObject(i);
            eleve.put(TYPE_PERIODE, typePeriode);
            eleve.put(ID_PERIODE_KEY, idPeriode);

            // Mise en forme de la date de naissance
            String idEleve = eleve.getString(ID_ELEVE_KEY);
            setBirthDate(eleve);

            // Classe à afficher
            setStudentClasseToPrint(eleve, classe);

            // Ajout de l'image du graphe par domaine
            if (showBilanPerDomaines) {
                setIdGraphPerDomaine(eleve, images);
            }

            // Ajout du niveau de l'élève
            setLevel(eleve);

            // Ajout de l'idEtablissement pour l'archive
            if(isNotNull(params.getString("idStructure")) && (isNull(eleve.getString(ID_ETABLISSEMENT_KEY))
                    || !eleve.getString(ID_ETABLISSEMENT_KEY).equals(params.getString(ID_ETABLISSEMENT_KEY)))){
                eleve.put(ID_ETABLISSEMENT_KEY, params.getString("idStructure"));
            }

            elevesMap.put(idEleve, eleve);

            //METTRE FUTURE pour handle final -> suppr l ancienne méthode d appel finalHandler

            getExportBulletin(answered, idEleve, elevesMap,idPeriode, params, classe, host, acceptLanguage,vertx,
                    futureGetHandler(futures.get(i)));
        }
        CompositeFuture.all(futures).setHandler(compositeEvent ->{
            if(compositeEvent.succeeded()){
                log.info("end students");
                finalHandler.handle(new Either.Right<>(null));
            }
        });

    }




    // ARCHIVE DES BULLETINS  =>  DEPRECATED
    @Deprecated
    private void getPeriodes(String idClasse, Future<JsonArray> periodesFuture){
        utilsService.getPeriodes(new JsonArray().add(idClasse), null, periodeEvent ->
                formate(periodesFuture, periodeEvent));
    }

    @Deprecated
    private void initialiseStrucutureData(JsonObject params) {
        params.put(HAS_IMG_SIGNATURE, false)
                .put(HAS_IMG_STRUCTURE, false)
                .put(IMG_SIGNATURE, "")
                .put(IMG_STRUCTURE, "")
                .put(NAME_CE, "");
    }

    @Deprecated
    private JsonObject initParamsForArchive(String idEtablissement, String idClasse, Long idPeriode,
                                            JsonObject imgsStructureObj, JsonArray niveauDeMatrise){

        String  niveauCompetences = "[";
        for(int i =0; i< niveauDeMatrise.size(); i++){
            niveauCompetences += niveauDeMatrise.getJsonObject(i).encode() + ",";
        }

        if(niveauDeMatrise.size()>0)
            niveauCompetences = niveauCompetences.substring(0, niveauCompetences.length()-1);
        else
            log.info("niveauMaitrise is equal to 0 ");
        niveauCompetences += "]";

        //PARAMS PAR DEFAUT
        //TODO CHECK SI PARAMS ATTENDUS PAR PO
        JsonObject res = new JsonObject()
                .put(GET_PROGRAM_ELEMENT, true)
                .put(GET_RESPONSABLE,true)
                .put(POSITIONNEMENT, true)
                .put(MOYENNE_CLASSE,true)
                .put(MOYENNE_ELEVE,true)
                .put(ID_CLASSE_KEY,idClasse)
                .put(ID_STRUCTURE_KEY,idEtablissement)
                .put(ID_ETABLISSEMENT_KEY,idEtablissement)
                .put(POSITIONNEMENT_AUTO,true)
                .put(SHOW_BILAN_PER_DOMAINE,true)
                .put(SHOW_FAMILY,false)
                .put(SHOW_PROJECTS,true)
                .put("threeLevel",false)
                .put("threeMoyenneClasse",false)
                .put("threeMoyenneEleve",false)
                .put("threePage",false)
                .put(TYPE_PERIODE,idPeriode)
                .put(NIVEAU_COMPETENCE, niveauCompetences)
                .put(COEFFICIENT,false)
                .put(GET_DATA_FOR_GRAPH_DOMAINE_METHOD, true)
                .put(USE_MODEL_KEY, false);
        //HDF
        if(idEtablissement.equals("faca3c3b-d29f-4ac1-a37b-e8e5da664f48")||idEtablissement.equals("26124cd3-f53d-45c9-a1a3-d81a0c401e4a")){
            res.put(MOYENNE_GENERALE,true);
            if(idEtablissement.equals("faca3c3b-d29f-4ac1-a37b-e8e5da664f48")){
                res.put(AGRICULTURE_LOGO,true);
            }
        }
        try {
            if (isNotNull(imgsStructureObj)) {
                res.put(HAS_IMG_SIGNATURE, imgsStructureObj.getValue(HAS_IMG_SIGNATURE))
                        .put(HAS_IMG_STRUCTURE, imgsStructureObj.getValue(HAS_IMG_STRUCTURE))
                        .put(IMG_SIGNATURE, imgsStructureObj.getValue(IMG_SIGNATURE))
                        .put(IMG_STRUCTURE, imgsStructureObj.getValue(IMG_STRUCTURE))
                        .put(NAME_CE, imgsStructureObj.getValue(NAME_CE));
            }
            else {
                initialiseStrucutureData(res);
            }
        }
        catch (NullPointerException e){
            initialiseStrucutureData(res);
            return res;
        }

        return res;
    }

    @Deprecated
    private void getClasseStructure(final String idStructure, Handler<Either<String, JsonArray>> handler){
        String query = " MATCH (c:Class)-[:BELONGS]->(s:Structure {id:{idStructure}}) return c.id as id ";
        Neo4j.getInstance().execute(query, new JsonObject().put(ID_STRUCTURE_KEY, idStructure),
                Neo4jResult.validResultHandler(handler));
    }

    @Deprecated
    private void markAsComplete(final String idStructure) {
        String query = "INSERT INTO " + EVAL_SCHEMA + ".arhive_bulletins_complet (id_etablissement, date_archive) "
                + " VALUES (?, NOW())  ON CONFLICT (id_etablissement) DO UPDATE SET date_archive = NOW() ";
        JsonArray values = new JsonArray().add(idStructure);
        Sql.getInstance().prepared(query, values, DELIVERY_OPTIONS,
                event -> {
                    JsonObject body = event.body();
                    if(!body.getString(STATUS).equals(OK)){
                        String message = body.getString(MESSAGE);
                        log.error("[markAsComplete] :: " + message);
                        if(message.contains(TIME)){
                            markAsComplete(idStructure);
                        }
                    }
                    else {
                        log.info("[markAsComplete] :: " + idStructure + " is completed ");
                    }
                });
    }

    @Deprecated
    private void runArchiveForStructure(JsonArray structures, AtomicInteger nbStructure,String path,String host,
                                        String acceptLanguage, Boolean forwardedFor, Vertx vertx, JsonObject config,
                                        Future structureFuture){

        // Si toutes les structures sont archivées on complete la future
        if(nbStructure.get() == 0) {
            structureFuture.complete();
            return ;
        }

        // Sinon on va traiter l'archivage classe par classe de l' établissement en cours
        Object structure =  structures.getValue(nbStructure.getAndDecrement()-1);
        String  idStructure;
        if(structure instanceof  JsonObject) {
            idStructure = ((JsonObject) structure).getString(ID_ETABLISSEMENT);
        }
        else{
            idStructure = (String) structure;
        }
        int index = structures.size() - nbStructure.get();
        log.info(" \n\n");
        log.info("                          :-------------------------------: ");
        log.info("                          :    BULLETIN STRUCTURE " + index + "/" + structures.size()+ "   " +
                ((index<=9)?" :":":"));
        log.info("                          :-------------------------------: \n(structure : " +  idStructure + ")\n");


        // On récupère les classes de l'établissement en cours
        Future<JsonArray> structureidClassesFuture = Future.future();
        getClasseStructure(idStructure, classeEvent ->
                formate(structureidClassesFuture, classeEvent));

        // On récupère récupère le nom et l'emplacement de la  signature du CE, l'emplacement du logo de l'etab
        Future<JsonObject> imgsStructureFuture = Future.future();
        utilsService.getParametersForExport(idStructure,
                eventImgsStructure -> formate(imgsStructureFuture, eventImgsStructure));
        Future srcStructure = Future.future();
        Future srcSignature = Future.future();
        final JsonObject _imgsStructureObj = new JsonObject();
        String imgsStructureObjLibelle = "imgsStructureObj";

        CompositeFuture.all(imgsStructureFuture, Future.succeededFuture()).setHandler(event -> {
            if (event.failed()) {
                log.error("[runArchiveForStructure] :: " + event.cause());
            } else {
                JsonObject imgsStructureObj = imgsStructureFuture.result();
                JsonObject imgStructure = imgsStructureObj.getJsonObject(IMG_STRUCTURE);
                JsonObject nameAndBrad = imgsStructureObj.getJsonObject("nameAndBrad");
                String pathImgStructure = isNotNull(imgStructure)? imgStructure.getString("path") : "";
                String pathImgSignature = isNotNull(nameAndBrad)? nameAndBrad.getString("path") : "";
                String nameCE = nameAndBrad.getString(NAME);
                imgsStructureObj.put(HAS_IMG_SIGNATURE, isNull(pathImgSignature)?false:!pathImgSignature.equals(""))
                        .put(HAS_IMG_STRUCTURE,isNull(pathImgStructure)?false:!pathImgStructure.equals(""))
                        .put(NAME_CE, isNotNull(nameCE)? nameCE : "");

                // avec les emplacements de la signature et du logo de l'établissement, on va chercher les images
                getSrcImg(srcSignature, vertx, imgsStructureObj, pathImgSignature, HAS_IMG_SIGNATURE, IMG_SIGNATURE);
                getSrcImg(srcStructure, vertx, imgsStructureObj, pathImgStructure, HAS_IMG_STRUCTURE, IMG_STRUCTURE);
                _imgsStructureObj.put(imgsStructureObjLibelle, imgsStructureObj);
            }
        });

        // Une fois les images du logo et de la signature récupérés, on lance l'archivage des classes
        CompositeFuture.all(structureidClassesFuture, srcStructure, srcSignature)
                .setHandler(event -> {
                    JsonArray idClasses = structureidClassesFuture.result();
                    if(event.failed() || isNull(idClasses)){
                        String errorStructures = event.cause().getMessage();
                        log.error(" Problem with Structure " + errorStructures);

                        // Si j'ai un problème avec la structure en cours,  je passe à la suivante
                        runArchiveForStructure(structures, nbStructure, path, host,
                                acceptLanguage, forwardedFor, vertx, config,
                                structureFuture);
                        return;
                    }

                    // Sinon l'archivage des classes peut commencer
                    AtomicInteger nbClasses = new AtomicInteger(idClasses.size());
                    Future classeFuture = Future.future();
                    runArchiveForClasse(idStructure, idClasses, nbClasses, path, host, acceptLanguage, forwardedFor,
                            vertx, config, _imgsStructureObj.getJsonObject(imgsStructureObjLibelle),  classeFuture);
                    CompositeFuture.all(classeFuture, Future.succeededFuture() )
                            .setHandler(eventClasse -> {
                                if(eventClasse.failed()){
                                    String errorClasses = event.cause().getMessage();
                                    log.error(" FAIL TO GET CL<ASSES " + errorClasses);
                                }
                                // Lorsque toutes les classes sont archivées, on marque la structure courante comme
                                // terminée et on passe à la structure suivante
                                markAsComplete(idStructure);
                                runArchiveForStructure(structures, nbStructure, path, host, acceptLanguage,
                                        forwardedFor, vertx, config, structureFuture);
                            });

                });

    }

    @Deprecated
    private void runArchiveForClasse(String idEtablissement, JsonArray idClasses, AtomicInteger nbClasses,
                                     String path,String host,
                                     String acceptLanguage, Boolean forwardedFor, Vertx vertx, JsonObject config,
                                     JsonObject imageStructureObj, Future classeFuture){

        // Si toutes les classes sont archivées, on complete la future
        if(nbClasses.get() == 0){
            classeFuture.complete();
            return;
        }

        // Sinon on lance l'archivage de la classe courante (l'archivage ce fait classe/classe)
        String idClasse = idClasses.getJsonObject(nbClasses.getAndDecrement()-1).getString(ID);
        int index = idClasses.size()  - nbClasses.get();
        log.info("\n START ARCHIVE OF CLASSE : " + idClasse + " " + index + "/" + idClasses.size() +" \n");

        // On récupère les périodes de la classe courante
        Future<JsonArray> periodesFuture = Future.future();
        getPeriodes(idClasse, periodesFuture);

        // On récupère le niveau de maitrise de la classe courante
        Future<JsonArray> niveauDeMaitriseFuture = Future.future();
        defaultNiveauDeMaitriseService.getNiveauDeMaitriseofClasse(idClasse, event ->
                formate(niveauDeMaitriseFuture, event));

        // Avec les périodes et le niveau de maitrise, l'archivage de la classe courante peut commencer
        CompositeFuture.all(periodesFuture,  niveauDeMaitriseFuture)
                .setHandler(periodesEnvent -> {
                    if (periodesEnvent.failed()) {
                        log.error("|runArchiveForClasse | :" + periodesEnvent.cause());

                        // Si j'ai un problème dans la récupération des données d'une classe, je passe à la suivante
                        runArchiveForClasse(idEtablissement, idClasses, nbClasses, path, host, acceptLanguage,
                                forwardedFor, vertx, config, imageStructureObj, classeFuture);
                        return;
                    }

                    List<Future> periodeClasseFuture = new ArrayList<>();
                    JsonArray periodes = periodesFuture.result();
                    JsonArray niveauDeMatrise = niveauDeMaitriseFuture.result();

                    // Je lance en parallèle l'archivage des bulletins de toutes les périodes de la classe
                    for(int i=0; i< periodes.size(); i++){
                        JsonObject periode = periodes.getJsonObject(i);
                        Future<JsonObject> periodeFuture = Future.future();
                        Long idPeriode = periode.getLong("id_type");
                        periodeClasseFuture.add(periodeFuture);
                        generateArchiveForClassePeriode(vertx, config, idEtablissement,
                                idClasse, idPeriode, periodeFuture,
                                imageStructureObj, niveauDeMatrise, path, host, acceptLanguage, forwardedFor);

                    }

                    // Lorsque tous les bulletins de toutes les périodes d'une classe sont archivés je passe à la svte
                    CompositeFuture.all(periodeClasseFuture).setHandler(event -> {
                        if(event.failed()){
                            log.error("|runArchiveForClasse | :" + event.cause().getMessage());
                        }
                        runArchiveForClasse(idEtablissement, idClasses, nbClasses, path, host, acceptLanguage,
                                forwardedFor, vertx, config, imageStructureObj, classeFuture);
                    });
                });
    }

    @Deprecated
    private void runArchiveBulletin(JsonArray structures, Vertx vertx, JsonObject config, String path, String host,
                                    String acceptLanguage, Boolean forwardedFor ){
        AtomicInteger nbStructure = new AtomicInteger(structures.size());
        Future structuresFuture = Future.future();

        //  On lance  l'archivage des bulletins etab par etab
        runArchiveForStructure(structures, nbStructure, path, host, acceptLanguage,
                forwardedFor, vertx, config, structuresFuture);

        // Lorsque le traitement est terminé, pour tous les etabs, on log la fin de l'archivage
        CompositeFuture.all(structuresFuture, Future.succeededFuture()).setHandler(archiveStructure -> {
            if(archiveStructure.failed()){
                String error = archiveStructure.cause().getMessage();
                log.error("[ARCHIVE | structuresFuture] :: " + error);
            }
            log.info("*************** END ARCHIVE BULLETIN ***************");
        });
    }




    @Deprecated
    public void archiveBulletin(JsonArray idStructures, Vertx vertx, JsonObject config, String path, String host,
                                String acceptLanguage, Boolean forwardedFor ){
        log.info(" ***************   START ARCHIVE BULLETIN  ***************");
        if(isNotNull(idStructures)) {
            runArchiveBulletin(idStructures, vertx, config, path, host, acceptLanguage, forwardedFor);
        }
        else if(isNull(idStructures) || idStructures.isEmpty()){
            log.info("*************** ALL STRUCTURES ARE COMPLETED ***************");
            return;
        }
    }

    @Deprecated
    private void endWithNOsrcImg(Future srcSignature, JsonObject imgsStructureObj,String hasImg, String imgStr){
        imgsStructureObj.put(imgStr, "");
        imgsStructureObj.put(hasImg, false);
        srcSignature.complete();
    }
    @Deprecated
    private void getSrcImg(Future srcSignature, Vertx vertx, JsonObject imgsStructureObj,
                           String pathImg, String hasImg, String imgStr) {
        if(isNull(pathImg) || pathImg.equals("")){
            endWithNOsrcImg(srcSignature, imgsStructureObj, hasImg, imgStr);
            return;
        }

        String [] idFile = pathImg.split("/");
        if(imgsStructureObj.getBoolean(hasImg) && isNotNull(idFile) && idFile.length > 1) {
            JsonObject action = new JsonObject()
                    .put(ACTION, "getDocument")
                    .put("id", idFile[idFile.length-1]);
            eb.send(   "org.entcore.workspace",  action,
                    DELIVERY_OPTIONS, handlerToAsyncHandler( img -> {
                        JsonObject body = img.body();

                        if (!"ok".equals(body.getString(STATUS))) {
                            log.error("[getSrcImg] :: " + body.encode());
                            endWithNOsrcImg(srcSignature, imgsStructureObj, hasImg, imgStr);
                        } else {
                            String status = body.getString(STATUS);
                            final JsonObject result = body.getJsonObject(RESULT);
                            if (! ("ok".equals(status) && isNotNull(result))) {
                                endWithNOsrcImg(srcSignature, imgsStructureObj, hasImg, imgStr);
                            }
                            else {
                                String file = result.getString("file");
                                String contentType = result.getJsonObject("metadata").getString("content-type");
                                storage.readFile(file, eventBuffer -> {
                                    if(isNotNull(eventBuffer)) {
                                        imgsStructureObj.put(imgStr,"data:" + contentType +";base64, \n" +
                                                Base64.getEncoder().encodeToString(eventBuffer.getBytes()));
                                        srcSignature.complete();
                                    }
                                    else{
                                        endWithNOsrcImg(srcSignature, imgsStructureObj, hasImg, imgStr);
                                    }

                                });
                            }
                        }
                    }));
        }
        else{
            imgsStructureObj.put(imgStr, "");
            imgsStructureObj.put(hasImg, false);
            srcSignature.complete();
        }
    }

    private void setStudentClasseToPrint(JsonObject student, JsonObject classe){
        student.put(CLASSE_NAME_TO_SHOW, classe.getString("classeName"));
      /*  if(!student.getString("idClasse").equals(idClasseExporte) &&
                student.getJsonObject("oldClasses") != null &&
                student.getJsonObject("oldClasses").getString(idClasseExporte) != null) {
            student.put(CLASSE_NAME_TO_SHOW, student.getJsonObject("oldClasses").getString(idClasseExporte));
        } else {
            student.put(CLASSE_NAME_TO_SHOW, student.getString("classeName"));
        }*/
    }


    @Deprecated
    private void generateArchiveForStudent(JsonArray students,final  int index, AsyncResult<Buffer> fileResult,
                                           Vertx vertx, JsonObject config,
                                           final String idEtablissement, final String idClasse, final Long idPeriode,
                                           Future<JsonObject> periodeFuture,final JsonObject imgsStructureObj,
                                           JsonArray niveauDeMatrise, String path,String host, String acceptLanguage,
                                           Boolean forwardedFor, Future elevesFuture){

        MustachHelper mustachHelper = new MustachHelper(vertx, config);
        final String templateName = "archiveBulletin.xhtml";
        JsonObject student =  students.getJsonObject(index);
        StringReader reader = new StringReader(fileResult.result().toString("UTF-8"));
        student.put(NIVEAU_COMPETENCE, student.getValue(NIVEAU_COMPETENCE).toString());
        // On génère le template avec MUSTACHE
        mustachHelper.processTemplate(student, templateName, reader, path, host, acceptLanguage, forwardedFor,
                writer -> {
                    String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                    if (processedTemplate == null) {
                        if (student != null) {
                            log.error("processing error : \nresultFinal : " + student.toString()
                                    + "\ntemplateName : " + templateName);
                        }
                        generateArchiveForStudent(students,  index, fileResult, vertx, config, idEtablissement,
                                idClasse,idPeriode, periodeFuture, imgsStructureObj, niveauDeMatrise, path, host,
                                acceptLanguage, forwardedFor,elevesFuture);
                        return;
                    }
                    byte[] bytes;
                    try {
                        bytes = processedTemplate.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        bytes = processedTemplate.getBytes();
                        log.error(e.getMessage(), e);
                    }
                    String fileName = getFileNameForStudent(student);
                    String idEleve = student.getString(ID_ELEVE_KEY);
                    String externalIdClasse = student.getString(EXTERNAL_ID_KEY);
                    Future eleveFuture = Future.future();

                    // On Appel node-pdf-generatore pour la parite front
                    callNodePdfGenerator(bytes, fileName, idEleve, idClasse, externalIdClasse, idEtablissement, idPeriode,
                            eleveFuture);
                    CompositeFuture.all(eleveFuture, Future.succeededFuture()).setHandler( event -> {
                        if(event.failed()){
                            log.error("[generateArchiveForStudent | eleveFuture] :: " + event.cause().getMessage());
                        }
                        elevesFuture.complete();
                    });
                });
    }

    private void getElevesClasse( String idClasse, Long idPeriode, Future<JsonArray> elevesFuture){
        Utils.getElevesClasse(eb, idClasse, idPeriode, elevesEvent -> {
            if(elevesEvent.isRight()){
                elevesFuture.complete(elevesEvent.right().getValue());
            }
            else {
                String error = elevesEvent.left().getValue();
                if(error.contains(TIME)){
                    log.error("[getElevesClasse] : "+ error);
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

    @Deprecated
    private void generateArchiveForClassePeriode(Vertx vertx, JsonObject config,
                                                 String idEtablissement, String idClasse, final Long idPeriode,
                                                 Future<JsonObject> periodeFuture, JsonObject imgsStructureObj,
                                                 JsonArray niveauDeMatrise,  String path,String host,
                                                 String acceptLanguage, Boolean forwardedFor){

        // Récupération des données pour l'export d'un des bulletins de la classe et de la période courantes
        Future<JsonObject> exportFuture = Future.future();
        try {
            getDataToGenerateArchiveForClassePeriode(vertx, config, idEtablissement, idClasse, idPeriode, exportFuture,
                    imgsStructureObj, niveauDeMatrise, path, host, acceptLanguage, forwardedFor);
        }
        catch (SQLTimeoutException e){
            log.error("[generateArchiveForClassePeriode] : Got a timeout "+ idPeriode + " (CLASSE : "+  idClasse + ")");
            generateArchiveForClassePeriode(vertx, config, idEtablissement,idClasse, idPeriode, periodeFuture,
                    imgsStructureObj, niveauDeMatrise, path, host, acceptLanguage, forwardedFor);
        }

        // Une fois tous les données récupérées, on va générer tous les exports et sauvegarder les fichiers dans l'ent
        CompositeFuture.all(exportFuture, Future.succeededFuture())
                .setHandler(event -> {
                    if(periodeFuture.isComplete()){
                        return;
                    }
                    if(event.failed()) {
                        log.error("generateArchiveForClassePeriode :" + event.cause());
                        periodeFuture.complete();
                        return;
                    }
                    JsonObject exportResult = exportFuture.result();
                    if(exportResult == null) {
                        log.error("NO ARCHIVE FOR  : " + idPeriode + " (CLASSE : "+  idClasse + ") ");
                        periodeFuture.complete();
                        return;
                    }

                    final String templateName = "archiveBulletin.xhtml";
                    vertx.fileSystem().readFile(TEMPLATE_PATH + templateName, result -> {

                        if (!result.succeeded()) {
                            periodeFuture.complete();
                            log.error("Error while reading template : " + TEMPLATE_PATH + templateName);
                            return;
                        }
                        log.info(">>>> BEGIN GENERATION WITH MUSTACHE AND POST TO NODE PDF GENERATOR "+ idPeriode);
                        JsonArray students = exportResult.getJsonArray(ELEVES);
                        List<Future> allElevesFuture = new ArrayList<>();
                        for(int i=0; i<students.size(); i++) {
                            Future elevePeriodeFuture = Future.future();
                            allElevesFuture.add(elevePeriodeFuture);
                            generateArchiveForStudent(students, i, result, vertx, config,
                                    idEtablissement, idClasse, idPeriode, periodeFuture, imgsStructureObj,
                                    niveauDeMatrise, path, host, acceptLanguage, forwardedFor,
                                    elevePeriodeFuture);
                        }
                        CompositeFuture.all(allElevesFuture).setHandler(allStudentsEvent -> {
                            if (allStudentsEvent.failed()) {
                                log.error( "[generateArchiveForClassePeriode] ::" + allStudentsEvent.cause());
                            }
                            log.info(">>>> END GENERATION WITH MUSTACHE AND POST TO NODE PDF GENERATOR "
                                    + idPeriode + " (CLASSE : " + idClasse + ") ");
                            periodeFuture.complete();
                        });
                    });
                });
    }

    @Deprecated
    private void getDataToGenerateArchiveForClassePeriode(Vertx vertx, JsonObject config,
                                                          String idEtablissement, String idClasse, final Long idPeriode,
                                                          Future<JsonObject> exportFuture,
                                                          JsonObject imgsStructureObj,
                                                          JsonArray niveauDeMatrise,  String path,String host,
                                                          String acceptLanguage, Boolean forwardedFor)
            throws SQLTimeoutException {

        log.info("RUN ARCHIVE OF PERIODE  : " + idPeriode + " (CLASSE : "+  idClasse + ") ");
        final Map<String, JsonObject> elevesMap = new LinkedHashMap<>();
        final AtomicBoolean answered = new AtomicBoolean();

        // On récupère les élèves de la classe courante présents sur la période
        Future<JsonArray> elevesFuture = Future.future();
        getElevesClasse(idClasse, idPeriode, elevesFuture);

        // On lance la récupération des données de tous les élèves  nécessaires sans générer  d'export bulletins
        JsonObject params = initParamsForArchive(idEtablissement, idClasse, idPeriode,imgsStructureObj,niveauDeMatrise);
        final Handler<Either<String, JsonObject>> finalHandler =
                getFinalArchiveBulletinHandler(elevesMap, vertx, config, elevesFuture, answered,
                        params, exportFuture);
        runExportBulletin( idEtablissement,  idClasse,  null,  idPeriode,  params, elevesFuture , elevesMap,
                answered,  host, acceptLanguage, finalHandler, exportFuture, vertx);
    }

    @Deprecated
    private Boolean serverIsOverLoad(String error){
        return error.contains(CONNECTION_RESET_BY_PEER) || error.contains(FAILED_TO_CREATE_SSL_CONNECTION) ||
                error.contains(BAD_GATEWAY) || error.contains(CONNECTION_WAS_CLOSED) ||
                error.contains(SERVICE_UNAVAILABLE);
    }

    @Deprecated
    private void callNodePdfGenerator(byte[] bytes, String fileName,
                                      final String idEleve, final String idClasse,final String externalIdClasse,
                                      String idEtablissement, Long idPeriode, Future<JsonObject> future){
        log.debug(" -> Begin post node-pdf-generator : (eleve: " + idEleve + ", classe: " + idClasse + ", periode: "
                +idPeriode + ") ");
        NodePdfGeneratorClientHelper.webServiceNodePdfGeneratorPost(this.httpClient,
                Buffer.buffer(bytes).toString(), (Either<String, Buffer> bufferEither) -> {
                    if (bufferEither.isLeft()) {
                        String error = bufferEither.left().getValue();
                        log.error("[callNodePdfGenerator] " + error +
                                "(eleve: " + idEleve + ", classe: " + idClasse + ", periode: "+ idPeriode + ") " );

                        if (serverIsOverLoad(error)) {
                            callNodePdfGenerator(bytes, fileName, idEleve, idClasse, externalIdClasse,
                                    idEtablissement, idPeriode, future);
                        } else {
                            future.complete();
                        }
                    } else {
                        Buffer file = bufferEither.right().getValue();
                        saveArchivePdf(fileName, file, idEleve, idClasse, externalIdClasse, idEtablissement, idPeriode,
                                event -> formate(future, event));

                        log.debug(" -> End post node-pdf-generator : (eleve: " + idEleve + ", classe: " + idClasse +
                                ", periode: " + idPeriode + ") ");
                    }
                });
    }

    public static void getExternalIdClasse(String idClasse,  Handler<Either<String, JsonObject>> handler) {
        String query = "MATCH (c:Class {id:{idClasse}}) return c.externalId as externalId ";
        JsonObject params = new JsonObject().put(ID_CLASSE_KEY, idClasse);
        Neo4j.getInstance().execute(query.toString(), params, Neo4jResult.validUniqueResultHandler(handler));
    }

    @Deprecated
    private void saveArchivePdf(String name, Buffer file, final String idEleve, final String idClasse,
                                final String externalIdClasse, final String idEtablissement, final Long idPeriode,
                                Handler<Either<String, JsonObject>> handler){
        this.storage.writeBuffer(file, "application/pdf", name,  uploaded -> {
            String idFile = uploaded.getString("_id");
            if (!OK.equals(uploaded.getString(STATUS)) || idFile ==  null) {
                String error = uploaded.getString(MESSAGE);
                log.error("saveArchivePdf : " + error);
                handler.handle(new Either.Right<>(uploaded));
                return;
            }
            String noFileStored = "No file stored: (eleve: " + idEleve + ", classe: " + idClasse + ", periode: "
                    + idPeriode + ", externalIdClasse: "+ externalIdClasse + ", idEtablissement: " + idEtablissement +
                    ") ";

            if(isNotNull(idEleve) && isNotNull(idClasse) && isNotNull(idEtablissement) && isNotNull(idPeriode)) {
                Handler<Either<String, JsonObject>> saveHandler = savaEvent -> {
                    if (savaEvent.isRight()) {
                        //log.info("file stored: (eleve: " + idEleve + ", classe: " + idClasse + ", periode: " + idPeriode + ") ");
                    }
                    else{
                        log.error(noFileStored);
                    }
                };

                if(isNull(externalIdClasse)){
                    getExternalIdClasse(idClasse, event -> {
                        if(event.isLeft()){
                            log.error(noFileStored);
                        }
                        else {
                            JsonObject result = event.right().getValue();
                            if(result == null){
                                log.error(noFileStored);
                            }
                            else {
                                String externalId = result.getString(EXTERNAL_ID_KEY);
                                saveIdArchive(idEleve, idClasse, externalId, idEtablissement, idPeriode, idFile,
                                        name, saveHandler);
                            }
                        }
                    });
                }
                else {
                    saveIdArchive(idEleve, idClasse, externalIdClasse, idEtablissement, idPeriode, idFile, name,
                            saveHandler);
                }
            }
            else {
                log.error(noFileStored);
            }
            handler.handle(new Either.Right<>(uploaded));

        });
    }

    @Deprecated
    private void saveIdArchive(String idEleve, String idClasse, String externalIdClasse,
                               String idEtablissement, Long idPeriode, String idFile, String name,
                               Handler<Either<String, JsonObject>> handler){
        utilsService.getYearsAndPeriodes(idEtablissement, true, event -> {
            if(event.isRight()){
                String idYear = event.right().getValue().getString("start_date").substring(0,4);
                String query = " INSERT INTO " + COMPETENCES_SCHEMA + ".archive_bulletins " +
                        " (id_classe, id_eleve, id_etablissement, external_id_classe, id_periode, id_file, file_name, id_annee) " +
                        " VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
                JsonArray values = new JsonArray().add(idClasse).add(idEleve).add(idEtablissement)
                        .add(externalIdClasse).add(idPeriode).add(idFile).add(name).add(idYear);
                Sql.getInstance().prepared(query, values, Competences.DELIVERY_OPTIONS, result -> {
                    JsonObject body = result.body();
                    if (!"ok".equals(body.getString(STATUS))) {
                        handler.handle(new Either.Left<>(body.getString(MESSAGE)));
                    }
                    else{
                        handler.handle(new Either.Right<>(body));
                    }
                });
            }
            else {
                handler.handle(new Either.Left<>(event.left().getValue()));
            }
        });
    }


    // BULLETIN WORKER
    @Override
    public void savePdfInStorage(String name, Buffer file, final String idEleve, final String idClasse,
                                 final String externalIdClasse, final String idEtablissement, final Long idPeriode,
                                 final String idParent, Handler<Either<String, JsonObject>> handler){
        this.storage.writeBuffer(file, "application/pdf", name, uploaded -> {
            String idFile = uploaded.getString("_id");
            if (!OK.equals(uploaded.getString(STATUS)) || idFile ==  null) {
                String error = "save pdf  : " + uploaded.getString(MESSAGE);
                if(error.contains(TIME)){
                    savePdfInStorage(name, file, idEleve, idClasse, externalIdClasse, idEtablissement,
                            idPeriode, idParent, handler);
                }
                else {
                    log.error(error);
                    handler.handle(new Either.Right<>(new JsonObject()));
                }
                return;
            }

            if(!(isNotNull(idEleve) && isNotNull(idClasse) && isNotNull(idEtablissement) && isNotNull(idPeriode))) {
                log.error("save bulletin pdf : null parameter");
                handler.handle(new Either.Right<>(new JsonObject()));
            }
            else{
                Handler<Either<String, JsonObject>> saveBulletinHandler = BulletinUtils.saveBulletinHandler(idEleve,
                        idClasse, externalIdClasse, idEtablissement, idPeriode, handler);

                utilsService.getYearsAndPeriodes(idEtablissement, true, yearEvent -> {
                    if (yearEvent.isRight()) {
                        String idYear = yearEvent.right().getValue().getString("start_date").substring(0,4);
                        if(isNotNull(externalIdClasse)){
                            BulletinUtils.saveIdBulletin(idEleve, idClasse, externalIdClasse, idEtablissement, idPeriode,
                                    idFile, name, idParent, idYear,  saveBulletinHandler);
                        }
                        else {
                            getExternalIdClasse(idClasse, event -> {
                                if(event.isLeft()){
                                    String error = "save bulletin pdf  : " + event.left().getValue();
                                    log.error(error);
                                    if(error.contains(TIME)){
                                        savePdfInStorage(name, file, idEleve, idClasse, externalIdClasse, idEtablissement,
                                                idPeriode, idParent, saveBulletinHandler);
                                    }
                                    else {
                                        log.error(error);
                                        handler.handle(new Either.Right<>(new JsonObject()));
                                    }
                                }
                                else {
                                    JsonObject result = event.right().getValue();
                                    if(result == null){
                                        log.error("Null externalId");
                                        handler.handle(new Either.Right<>(new JsonObject()));
                                    }
                                    else {
                                        String externalId = result.getString(EXTERNAL_ID_KEY);
                                        BulletinUtils.saveIdBulletin(idEleve, idClasse, externalId, idEtablissement,
                                                idPeriode, idFile, name, idParent, idYear, saveBulletinHandler);
                                    }
                                }
                            });
                        }
                    }
                    else {
                        handler.handle(new Either.Left<>(yearEvent.left().getValue()));
                    }
                });
            }
        });
    }

    @Override
    public void runSavePdf( JsonObject bulletinEleve, final JsonObject bulletin, Vertx vertx, JsonObject config,
                            Handler<Either<String, Boolean>> bulletinHandlerWork){
        final HttpServerRequest request = new JsonHttpServerRequest(bulletin.getJsonObject("request"));
        final JsonObject templateProps =  bulletin.getJsonObject("resultFinal");
        final String templateName = bulletin.getString("template");
        final String prefixPdfName = bulletin.getString("title");

        generateAndSavePdf(request, templateProps, templateName, prefixPdfName, bulletinEleve,
                vertx, config, bulletinHandlerWork);
    }

    public void generateAndSavePdf(final HttpServerRequest request, JsonObject resultFinal, final String templateName,
                                   final String prefixPdfName, JsonObject eleve, Vertx vertx, JsonObject config,
                                   Handler<Either<String, Boolean>> finalHandler){
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
        vertx.fileSystem().readFile(templatePath + templateName, new Handler<AsyncResult<Buffer>>() {
            @Override
            public void handle(AsyncResult<Buffer> result) {
                if (!result.succeeded()) {
                    badRequest(request, "Error while reading template : " + templatePath + templateName);
                    log.error("[DefaultExportBulletinService] Error while reading template : " + templatePath + templateName);
                    return;
                }
                StringReader reader = new StringReader(result.result().toString("UTF-8"));
                Renders render = new Renders(vertx, config);

                JsonObject templateProps = resultFinal;

                templateProps.put("eleves", new JsonArray().add(eleve));
                render.processTemplate(request, templateProps, templateName, reader,
                        getRenderProcessHandler(templateProps, baseUrl, _node, request, prefixPdfName, dateDebut, eleve, finalHandler));

            }
        });
    }

    private Handler<Writer> getRenderProcessHandler(JsonObject templateProps, String baseUrl, String _node, HttpServerRequest request, String prefixPdfName, String dateDebut, JsonObject eleve, Handler<Either<String, Boolean>> finalHandler) {
        return new Handler<Writer>() {
            @Override
            public void handle(Writer writer) {
                String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                JsonObject actionObject = new JsonObject();
                byte[] bytes;
                try {
                    bytes = processedTemplate.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    bytes = processedTemplate.getBytes();
                    log.error("[DefaultExportBulletinService] " + e.getMessage() + " "+
                            eleve.getString("idEleve") + " " + eleve.getString("lastName"));
                }

                actionObject.put("content", bytes).put("baseUrl", baseUrl);
                eb.send(_node + "entcore.pdf.generator", actionObject,
                        new DeliveryOptions().setSendTimeout(
                                TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L),
                        handlerToAsyncHandler(getPdfRenderHandler(request, templateProps, prefixPdfName, dateDebut, eleve, finalHandler)));
            }
        };
    }

    private Handler<Message<JsonObject>> getPdfRenderHandler(HttpServerRequest request, JsonObject templateProps, String prefixPdfName, String dateDebut, JsonObject eleve, Handler<Either<String, Boolean>> finalHandler) {
        return new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                JsonObject pdfResponse = reply.body();
                if (!"ok".equals(pdfResponse.getString("status"))) {
                    badRequest(request, pdfResponse.getString("message"));
                    return;
                }
                byte[] pdf = pdfResponse.getBinary("content");

                Buffer buffer;
                if (templateProps.containsKey("image") && templateProps.getBoolean("image")) {
                    File pdfFile = new File(prefixPdfName + "_" + dateDebut + ".pdf");
                    OutputStream outStream = null;
                    try {
                        outStream = new FileOutputStream(pdfFile);
                    } catch (FileNotFoundException e) {
                        log.error("[DefaultExportBulletinService]" + e.getMessage() + " "
                                + eleve.getString("idEleve") + " " + eleve.getString("lastName"));
                        e.printStackTrace();
                    }
                    try {
                        outStream.write(pdf);
                    } catch (IOException e) {
                        log.error("[DefaultExportBulletinService] " + e.getMessage() + " " +
                                eleve.getString("idEleve") + " " + eleve.getString("lastName"));
                        e.printStackTrace();
                    }

                    handleCreateFile(pdfFile, outStream, templateProps, prefixPdfName, dateDebut, eleve, finalHandler);
                } else {
                    buffer = Buffer.buffer(pdf);
                    savePdfDefault(buffer, eleve, finalHandler);
                }
            }
        };
    }

    private void handleCreateFile(File pdfFile, OutputStream outStream, JsonObject templateProps, String prefixPdfName,
                                  String dateDebut, JsonObject eleve, Handler<Either<String, Boolean>> finalHandler) {
        Buffer buffer;
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
                buffer = Buffer.buffer(bytes);
                savePdfDefault(buffer,eleve,finalHandler);
                outStream.close();
                bos.close();
                fis.close();

                Files.deleteIfExists(Paths.get(pdfFile.getAbsolutePath()));
                Files.deleteIfExists(Paths.get(imageFile.getAbsolutePath()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[DefaultExportBulletinService] : " + e.getMessage() + " "
                    + eleve.getString("idEleve") + " " + eleve.getString("lastName"));
            finalHandler.handle(new Either.Left<>(e.getMessage()));
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

    private void generateImagesFromPath(JsonObject eleve, Vertx vertx, Handler<Either<String, JsonObject>> handler) {
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
                String imgStructureEncoded = imgStructureFuture.result();
                String imgSignatureEncoded = imgSignatureFuture.result();
                String logoEncoded = logoFuture.result();

                eleve.put(IMG_SIGNATURE, imgSignatureEncoded);
                eleve.put(IMG_STRUCTURE, imgStructureEncoded);
                eleve.put("logoData",logoEncoded);
                handler.handle(new Either.Right<>(new JsonObject()));
            }
        });
        String[] structureLogoString =  eleve.getString(IMG_STRUCTURE).split("/");
        String structureLogoId = structureLogoString[structureLogoString.length - 1];
        getBase64File(structureLogoId, imgStructureFuture);

        String[] signatureSplit = eleve.getString("imgSignature").split("/");
        String signatureLogoId = signatureSplit[signatureSplit.length - 1];
        getBase64File(signatureLogoId, imgSignatureFuture);

        generatesImage(eleve, vertx, LOGO_PATH, logoFuture);
        eleve.put("hasImgLoaded", true);
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
        }
    }

    private void savePdfDefault(Buffer buffer, JsonObject eleve, Handler<Either<String, Boolean>> finalHandler) {
        savePdfInStorage(getFileNameForStudent(eleve), buffer,
                eleve.getString("idEleve"),
                eleve.getString("idClasse"),
                eleve.getString("externalId"),
                eleve.getString("idEtablissement"),
                eleve.getLong("idPeriode"),
                getIdParentForStudent(eleve),
                new Handler<Either<String, JsonObject>>() {
                    @Override
                    public void handle(Either<String, JsonObject> event) {
                        if (event.isLeft()) {
                            log.error("[DefaultExportBulletinService] : Error on savePdfInStorage "
                                    + event.left().getValue() + " "
                                    + eleve.getString("idEleve") + " " + eleve.getString("lastName" ));
                            finalHandler.handle(new Either.Left<>(event.left().getValue()));
                        } else {
                            finalHandler.handle(new Either.Right<>(true));
                        }
                    }
                });
    }
}
