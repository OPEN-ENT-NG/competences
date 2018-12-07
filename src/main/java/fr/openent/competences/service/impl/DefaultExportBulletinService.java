package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.AvisConseilService;
import fr.openent.competences.service.BilanPeriodiqueService;
import fr.openent.competences.service.ElementBilanPeriodiqueService;
import fr.openent.competences.service.ExportBulletinService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;

import static fr.wseduc.webutils.http.Renders.getHost;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final String GET_IMAGE_GRAPH_METHOD= "getImageGraph";


    // Keys For JsonObject
    private static final String PRINT_MATIERE_KEY = "printMatiere";
    private static final String ID_PERIODE ="id_periode";
    private static final String ID_CLASSE = "idClasse";
    private static final String ID_ELEVE = "idEleve";
    private static final String GET_RESPONSABLE = "getResponsable";
    private static final String MOYENNE = "moyenne";
    private static final String MOYENNE_CLASSE = "moyenneClasse";
    private static final String MOYENNE_ELEVE = "moyenneEleve";
    private static final String POSITIONNEMENT = "positionnement";
    private static final String ELEMENTS_PROGRAMME = "elementsProgramme";
    private static final String ACTION = "action";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String RESULT = "result";
    private static final String RESULTS = "results";
    private static final String NAME = "name";
    private static final String ADDRESSE_POSTALE = "addressePostale";
    private static final String GRAPH_PER_DOMAINE = "graphPerDomaine";
    private static final String LIBELLE = "libelle";

    // Parameter Key 
    private static final String GET_MOYENNE_CLASSE = "getMoyenneClasse";
    private static final String GET_MOYENNE_ELEVE = "getMoyenneEleve";
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
    private final Storage storage;



    public DefaultExportBulletinService(EventBus eb, Storage storage) {
        this.eb = eb;
        bilanPeriodiqueService = new DefaultBilanPerioqueService(eb);
        elementBilanPeriodiqueService = new DefaultElementBilanPeriodiqueService(eb);
        appreciationCPEService = new DefaultAppreciationCPEService();
        syntheseBilanPeriodiqueService = new DefaultSyntheseBilanPeriodiqueService();
        avisConseilService = new DefaultAvisConseilService();
        this.storage = storage;
    }


    private String getLibelle(String key) {
        return I18n.getInstance().translate(key,
                I18n.DEFAULT_DOMAIN, Locale.FRANCE);
    }

    private void logBegin(String method, String idEleve) {
        log.info("------- [" + method + "]: " + idEleve + " DEBUT " );
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
                    .put("moyenneStudentLibelle", getLibelle("average.min.eleve"))
                    .put("bilanAcquisitionLibelle", getLibelle("viescolaire.suivi.des.acquis.libelle.export"))
                    .put("viescolaireLibelle", getLibelle("evaluations.export.bulletin.viescolaireLibelle"))
                    .put("familyVisa", getLibelle("evaluations.export.bulletin.visa.libelle"))
                    .put("signature", getLibelle("evaluations.export.bulletin.date.name.visa.responsable"))
                    .put("bornAt", getLibelle("born.on"))
                    .put("classeOf", getLibelle("classe.of"))
                    .put("footer", "*: " + getLibelle("evaluations.export.bulletin.legendPositionnement"))
                    .put("bilanPerDomainesLibelle", getLibelle("evaluations.bilan.by.domaine"))


                    // positionnement des options d'impression
                    .put(GET_RESPONSABLE, params.getBoolean(GET_RESPONSABLE))
                    .put(GET_MOYENNE_CLASSE, params.getBoolean(MOYENNE_CLASSE))
                    .put(GET_MOYENNE_ELEVE, params.getBoolean(MOYENNE_ELEVE))
                    .put(GET_POSITIONNEMENT, params.getBoolean(POSITIONNEMENT))
                    .put(SHOW_PROJECTS, params.getBoolean(SHOW_PROJECTS))
                    .put(SHOW_FAMILY, params.getBoolean(SHOW_FAMILY))
                    .put(GET_PROGRAM_ELEMENT, params.getBoolean(GET_PROGRAM_ELEMENT))
                    .put(SHOW_BILAN_PER_DOMAINE, params.getBoolean(SHOW_BILAN_PER_DOMAINE))
                    .put(IMG_STRUCTURE, params.getString(IMG_STRUCTURE))
                    .put(HAS_IMG_STRUCTURE, params.getBoolean(HAS_IMG_STRUCTURE))
                    .put(IMG_SIGNATURE, params.getString(IMG_SIGNATURE))
                    .put(HAS_IMG_SIGNATURE, params.getBoolean(HAS_IMG_SIGNATURE))
                    .put(NAME_CE, params.getString(NAME_CE));

        }
        log.info(" -------[" + PUT_LIBELLE_FOR_EXPORT_METHOD +" ]: " + idEleve + " FIN " );
        finalHandler.handle(new Either.Right<>(null));
    }

    @Override
    public void getExportBulletin(final HttpServerRequest request, final AtomicBoolean answered, String idEleve,
                                  Map<String, JsonObject> elevesMap, Long idPeriode, JsonObject params,
                                  final JsonObject classe,
                                  Handler<Either<String, JsonObject>> finalHandler){
        try {

            if (!answered.get()) {
                putLibelleForExport(idEleve, elevesMap, params, finalHandler);
                getEvenements(idEleve, elevesMap, idPeriode, finalHandler);
                getSyntheseBilanPeriodique(idEleve, elevesMap, idPeriode, finalHandler);
                getStructure(idEleve, elevesMap, finalHandler);
                getHeadTeachers(idEleve, elevesMap, finalHandler);
                getLibellePeriode(request, idEleve, elevesMap, idPeriode, finalHandler);
                getAnneeScolaire(idEleve, elevesMap, finalHandler);
                getCycle(idEleve,elevesMap,idPeriode,finalHandler);
                getAppreciationCPE(idEleve, elevesMap, idPeriode, finalHandler);
                getAvisConseil(idEleve, elevesMap, idPeriode, finalHandler);
                if(params.getBoolean(GET_RESPONSABLE)) {
                    getResponsables(idEleve, elevesMap, finalHandler);
                }
                if(params.getBoolean(SHOW_BILAN_PER_DOMAINE)) {
                    getImageGraph(idEleve, elevesMap, idPeriode, finalHandler);
                }

                if (params.getBoolean(SHOW_PROJECTS)) {
                    getProjets(idEleve, elevesMap, idPeriode, finalHandler);
                }
                getSuiviAcquis(idEleve, elevesMap, idPeriode, classe, finalHandler);
            }
            else {
                log.error("[getExportBulletin] : Problème de parallelisation Lors de l'export des bulletin ");
            }
        }
        catch (Exception e) {
            log.error(EXPORT_BULLETIN_METHOD, e);
        }
    }

    @Override
    public void getCycle ( String idEleve,  Map<String,JsonObject> elevesMap,Long idPeriode,
                           Handler<Either<String, JsonObject>> finalHandler) {
        JsonObject eleve = elevesMap.get(idEleve);
        logBegin(GET_CYCLE_METHOD, idEleve);
        if (eleve == null) {
            logStudentNotFound(idEleve, GET_CYCLE_METHOD);
            finalHandler.handle(new Either.Right<>(null));

        }
        else {

            String idClasse = eleve.getString(ID_CLASSE);

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
                                        eleve.put("bilanCycle", getLibelle("evaluations.bilan.trimestriel.of")
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
    public void getLibellePeriode(final HttpServerRequest request, String idEleve,
                                  Map<String, JsonObject> elevesMap, Long idPeriode,
                                  Handler<Either<String, JsonObject>> finalHandler){
        JsonObject eleve = elevesMap.get(idEleve);
        logBegin(GET_LIBELLE_PERIOD_METHOD, idEleve);
        if(eleve == null) {
            logStudentNotFound(idEleve, GET_LIBELLE_PERIOD_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            JsonObject jsonRequest = new JsonObject()
                    .put("headers", new JsonObject().put("Accept-Language",
                            request.headers().get("Accept-Language")))
                    .put("Host", getHost(request));

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
                                eleve.put("periode", periodeName);
                                serviceResponseOK(answer, finalHandler, count, idEleve, GET_LIBELLE_PERIOD_METHOD);
                            }
                        }
                    }));
        }
    }


    @Override
    public void getAnneeScolaire(String idEleve,
                                 Map<String, JsonObject> elevesMap,
                                 Handler<Either<String, JsonObject>> finalHandler) {
        JsonObject eleve = elevesMap.get(idEleve);
        logBegin(GET_ANNEE_SCOLAIRE_METHOD, idEleve);
        if (eleve == null) {
            logStudentNotFound(idEleve, GET_ANNEE_SCOLAIRE_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            String idClasse = eleve.getString(ID_CLASSE);

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

    private void getImageGraph (String idEleve,
                                Map<String, JsonObject> elevesMap, Long idPeriode,
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
                log.info("["+ GET_IMAGE_GRAPH_METHOD + "] : File not found ");
                finalHandler.handle(new Either.Right<>(null));
            }
        }
    }

    @Override
    public void getAvisConseil(String idEleve, Map<String, JsonObject> elevesMap, Long idPeriode,
                               Handler<Either<String, JsonObject>> finalHandler) {
        logBegin(GET_AVIS_CONSEIL_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_AVIS_CONSEIL_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }else{
            avisConseilService.getAvisConseil(idEleve, idPeriode, new Handler<Either<String, JsonObject>>() {
                private int count = 1;
                private AtomicBoolean answer = new AtomicBoolean(false);

                @Override
                public void handle(Either<String, JsonObject> event) {
                    if(event.isLeft()){
                        String message = event.left().getValue();
                        log.error("[getAvisConseil ] : " + idEleve  + " " + message + " " + count);
                        if (message.contains("Time")) {
                            count++;
                            avisConseilService.getAvisConseil(idEleve, idPeriode,this);
                        }
                        else {
                            if (eleveObject.getJsonArray("errors") == null) {
                                eleveObject.put("errors", new JsonArray());
                            }
                            JsonArray errors = eleveObject.getJsonArray("errors");
                            errors.add(GET_AVIS_CONSEIL_METHOD);
                            serviceResponseOK(answer, finalHandler, count, idEleve, GET_AVIS_CONSEIL_METHOD);
                        }
                    }else{
                        JsonObject avisConseil = event.right().getValue();
                        if(avisConseil != null ) {
                            eleveObject.put("avisConseil",avisConseil.getString(LIBELLE))
                                    .put("hasAvisConseil",true);
                        }
                        serviceResponseOK(answer, finalHandler, count, idEleve, GET_AVIS_CONSEIL_METHOD);
                    }
                }
            });
        }
    }

    private void sethasProject( JsonObject project, boolean value) {

        if(project.getBoolean("hasProject") == value) {
            return;
        }
        else if (project.getBoolean("hasProject") != null) {
            project.remove("hasProject");
        }
        project.put("hasProject", value);
    }


    @Override
    public void getProjets ( String idEleve,  Map<String,JsonObject> elevesMap,Long idPeriode,
                             Handler<Either<String, JsonObject>> finalHandler) {
        JsonObject eleveObject = elevesMap.get(idEleve);
        logBegin(GET_PROJECTS_METHOD, idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_PROJECTS_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            // gets Projects
            String idClasse = eleveObject.getString(ID_CLASSE);
            String idEtablissement = eleveObject.getString("idEtablissement");

            elementBilanPeriodiqueService.getElementsBilanPeriodique(null, idClasse,
                    idEtablissement, new Handler<Either<String, JsonArray>>() {
                        private int count = 1;
                        private AtomicBoolean answer = new AtomicBoolean(false);
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isLeft()) {
                                String message = event.left().getValue();
                                log.error("["+ GET_PROJECTS_METHOD +"] :" + idEleve + " " + message + " " + count);
                                if (message.contains("Time") && !answer.get()) {
                                    count++;
                                    elementBilanPeriodiqueService.getElementsBilanPeriodique(null,
                                            idClasse, idEtablissement, this);
                                }
                                else {
                                    if (eleveObject.getJsonArray("errors") == null) {
                                        eleveObject.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray("errors");
                                    errors.add(GET_PROJECTS_METHOD);
                                    serviceResponseOK(answer, finalHandler, count, idEleve, GET_PROJECTS_METHOD);
                                }

                            }
                            else {
                                if (count > 1 ) {
                                    log.info("[getProjets] : " + idEleve + " success " + count);
                                }
                                List<String> idClasses = new ArrayList<String>();
                                idClasses.add(idClasse);
                                JsonArray elementBilanPeriodique = event.right().getValue();
                                List<String> idElements = new ArrayList<String>();
                                Map<Long, JsonObject> mapElement = new HashMap<>();
                                JsonObject epi = new JsonObject().put(LIBELLE,
                                        getLibelle("enseignements.pratiques.interdisciplinaires"))
                                        .put("hasProject", false);
                                JsonObject ap = new JsonObject().put(LIBELLE,
                                        getLibelle("accompagnements.personnalises"))
                                        .put("hasProject", false);
                                JsonObject parcours = new JsonObject().put(LIBELLE,
                                        getLibelle("parcours.educatifs"))
                                        .put("hasProject", false);

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
                                                            if (message.contains("Time") && !answer.get()) {
                                                                count++;
                                                                elementBilanPeriodiqueService
                                                                        .getAppreciations(idClasses,
                                                                                idPeriode.toString(),
                                                                                idElements, idEleve, this);
                                                            }
                                                            else {
                                                                if (eleveObject.getJsonArray("errors") == null) {
                                                                    eleveObject.put("errors", new JsonArray());
                                                                }
                                                                JsonArray errors = eleveObject
                                                                        .getJsonArray("errors");
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
                                        log.info(" [getProjets] | NO elements founds for classe " + idClasse);
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
                                             Handler<Either<String, JsonObject>> finalHandler) {
        JsonObject eleveObject = elevesMap.get(idEleve);
        logBegin(GET_SYNTHESE_BILAN_PERIO_METHOD, idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_SYNTHESE_BILAN_PERIO_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(idPeriode, idEleve,
                    new Handler<Either<String, JsonObject>>() {
                        private int count = 1;
                        private AtomicBoolean answer = new AtomicBoolean(false);
                        @Override
                        public void handle(Either<String, JsonObject> event) {
                            if(event.isLeft()){
                                String message = event.left().getValue();
                                log.error("[getSyntheseBilanPeriodique ] : " + idEleve  + " " + message + " " + count);
                                if (message.contains("Time") && !answer.get()) {
                                    count++;
                                    syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(idPeriode, idEleve,
                                            this);
                                }
                                else {
                                    if (eleveObject.getJsonArray("errors") == null) {
                                        eleveObject.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray("errors");
                                    errors.add(GET_SYNTHESE_BILAN_PERIO_METHOD);
                                    serviceResponseOK(answer, finalHandler, count, idEleve,
                                            GET_SYNTHESE_BILAN_PERIO_METHOD);
                                }
                            }
                            else {
                                JsonObject synthese = event.right().getValue();
                                if (synthese != null) {
                                    String syntheseStr = synthese.getString("synthese");
                                    eleveObject.put("syntheseBilanPeriodque",troncateLibelle(syntheseStr,
                                             MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE));
                                    eleveObject.put("syntheseBilanPeriodqueStyle",fontSize(syntheseStr,
                                             MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE));
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
                        if (message.contains("Time") && !answer.get()) {
                            count++;
                            appreciationCPEService.getAppreciationCPE(idPeriode, idEleve, this);
                        }
                        else {
                            if (eleveObject.getJsonArray("errors") == null) {
                                eleveObject.put("errors", new JsonArray());
                            }
                            JsonArray errors = eleveObject.getJsonArray("errors");
                            errors.add(GET_APPRECIATION_CPE_METHOD);
                            serviceResponseOK(answer, finalHandler, count, idEleve, GET_APPRECIATION_CPE_METHOD);
                        }
                    } else {
                        JsonObject appreciationCPE = event.right().getValue();

                        if (appreciationCPE != null) {
                            String app = troncateLibelle(appreciationCPE.getString("appreciation"),
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
    public void getStructure( String idEleve, Map<String,JsonObject> elevesMap,
                              Handler<Either<String, JsonObject>> finalHandler) {
        logBegin(GET_STRUCTURE_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve,"getStructure");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            JsonObject action = new JsonObject();
            String idStructure = eleveObject.getString("idEtablissement");
            action.put(ACTION, "structure.getStructure")
                    .put("idStructure", idStructure);

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
                                log.error("["+ GET_STRUCTURE_METHOD + "] : " + idEleve + " " + mess + " " + count);

                                if (mess.contains("Time") && !answer.get()) {
                                    count++;
                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                            Competences.DELIVERY_OPTIONS,
                                            handlerToAsyncHandler(this));
                                }
                                else {
                                    if (eleveObject.getJsonArray("errors") == null) {
                                        eleveObject.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray("errors");
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
                                        if(name != null) {structureLibelle.add(
                                                new JsonObject().put("nameStructure",name));}
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
                                eleveObject.put("structureLibelle", structureLibelle);
                                serviceResponseOK(answer, finalHandler, count, idEleve, GET_STRUCTURE_METHOD);
                            }
                        }
                    }));
        }
    }

    @Override
    public void getHeadTeachers( String idEleve, Map<String,JsonObject> elevesMap,
                                 Handler<Either<String, JsonObject>> finalHandler) {
        logBegin(GET_HEAD_TEACHERS_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_HEAD_TEACHERS_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            JsonObject action = new JsonObject();
            String idClasse = eleveObject.getString(ID_CLASSE);
            if (idClasse == null) {
                logidClasseNotFound(idEleve, GET_HEAD_TEACHERS_METHOD);
                finalHandler.handle(new Either.Right<>(null));
            }
            else {
                action.put(ACTION, "classe.getHeadTeachersClasse")
                        .put(ID_CLASSE, idClasse);

                eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                        Competences.DELIVERY_OPTIONS,
                        handlerToAsyncHandler(
                                new Handler<Message<JsonObject>>() {
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
                                            JsonArray headTeachers = body.getJsonArray(RESULTS);
                                            if (headTeachers != null) {
                                                for(int i=0; i< headTeachers.size(); i++){
                                                    JsonObject headTeacher =  headTeachers.getJsonObject(i);
                                                    String firstName = headTeacher.getString("firstName");
                                                    String initial = "";

                                                    if(firstName != null && firstName.length() > 0) {
                                                        initial =   String.valueOf(firstName.charAt(0)) + ". ";
                                                    }
                                                    headTeacher.put("initial", initial);
                                                }
                                                String headTeachersLibelle = getLibelle(
                                                        (headTeachers.size() > 1) ? "headTeachers" : "headTeacher");
                                                eleveObject.put("headTeacherLibelle", headTeachersLibelle + " : ")
                                                        .put("headTeachers", headTeachers);
                                            }
                                            serviceResponseOK(answer, finalHandler, count, idEleve,
                                                    GET_HEAD_TEACHERS_METHOD);
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
                    .put(ID_ELEVE, idEleve);
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
    public void getEvenements(String idEleve,Map<String, JsonObject> elevesMap, Long idPeriode,
                              Handler<Either<String, JsonObject>> finalHandler ) {

        logBegin(GET_EVENEMENT_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_EVENEMENT_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            bilanPeriodiqueService.getRetardsAndAbsences(idEleve, new Handler<Either<String, JsonArray>>() {
                private int count = 1;
                private AtomicBoolean answer = new AtomicBoolean(false);

                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isLeft()) {
                        String message = event.left().getValue();

                        if (message.contains("Time") && !answer.get()) {
                            count++;
                            bilanPeriodiqueService.getRetardsAndAbsences(idEleve, this);

                        }
                        else {
                            if (eleveObject.getJsonArray("errors") == null) {
                                eleveObject.put("errors", new JsonArray());
                            }
                            JsonArray errors = eleveObject.getJsonArray("errors");
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

    @Override
    public void getSuiviAcquis(String idEleve,Map<String, JsonObject> elevesMap, Long idPeriode,
                               final JsonObject classe,
                               Handler<Either<String, JsonObject>> finalHandler ) {

        logBegin(GET_SUIVI_ACQUIS_METHOD, idEleve);
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, GET_SUIVI_ACQUIS_METHOD);
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            String idEtablissement = eleveObject.getString("idEtablissement");
            String idClasse = eleveObject.getString(ID_CLASSE);
            if (idClasse == null || idEtablissement == null) {
                if(idClasse == null) {
                    logidClasseNotFound(idEleve, GET_SUIVI_ACQUIS_METHOD);
                }
                if (idEtablissement == null) {
                    logidEtabNotFound(idEleve, GET_SUIVI_ACQUIS_METHOD);
                }
                finalHandler.handle(new Either.Right<>(null));
            }
            else {
                bilanPeriodiqueService.getSuiviAcquis(idEtablissement, idPeriode, idEleve,
                        idClasse, new Handler<Either<String, JsonArray>>() {
                            private int count = 1;
                            private AtomicBoolean answer = new AtomicBoolean(false);

                            @Override
                            public void handle(Either<String, JsonArray> event) {
                                if (event.isLeft()) {
                                    String message =  event.left().getValue();
                                    log.error("["+ GET_SUIVI_ACQUIS_METHOD + "] :" + idEleve + " " + message + " " + count);
                                    if (message.contains("Time") && !answer.get()) {
                                        count ++;
                                        bilanPeriodiqueService.getSuiviAcquis(idEtablissement, idPeriode, idEleve,
                                                idClasse,this);
                                    }
                                    else {
                                        if (eleveObject.getJsonArray("errors") == null) {
                                            eleveObject.put("errors", new JsonArray());
                                        }
                                        JsonArray errors = eleveObject.getJsonArray("errors");
                                        errors.add(GET_SUIVI_ACQUIS_METHOD);
                                        serviceResponseOK(answer, finalHandler, count, idEleve, GET_SUIVI_ACQUIS_METHOD);
                                        try {
                                            finalize();
                                        } catch (Throwable throwable) {
                                            log.error(throwable.getMessage());
                                        }

                                    }
                                }
                                else {
                                    JsonArray suiviAcquis = event.right().getValue();
                                    JsonArray res = new JsonArray();
                                    // On considèrera qu'on a un suivi des acquis si on affiche au moins une
                                    // matière
                                    for (int i = 0; suiviAcquis != null && i < suiviAcquis.size() ; i++) {
                                        final JsonObject matiere = suiviAcquis.getJsonObject(i);
                                        // Une matière sera affichée si on a au moins un élement sur la période
                                        final boolean printMatiere = false;
                                        buildMatiereForSuiviAcquis (matiere, printMatiere, idPeriode, classe);
                                        if(matiere.getBoolean(PRINT_MATIERE_KEY)) {
                                            res.add(matiere);
                                        }

                                    }
                                    eleveObject.put("suiviAcquis", res).put("hasSuiviAcquis", res.size() > 0);

                                    serviceResponseOK(answer, finalHandler, count, idEleve, GET_SUIVI_ACQUIS_METHOD);
                                    try {
                                        finalize();
                                    } catch (Throwable throwable) {
                                        log.error(throwable.getMessage());
                                    }
                                }
                            }
                        });
            }
        }
    }

    /**
     *  Calcule et met en forme les colonnes de la matière passée en paramètre
     * @param matiere matière à traiter
     * @param printMatiere utilisé pour savoir si on affiche la matière ou pas
     * @param idPeriode période sélectionnée
     * @param classe JsonObject contenant les informations de la classe de l'élève dont on contruit le bulletin
     */
    private  JsonObject buildMatiereForSuiviAcquis (final JsonObject matiere,
                                                    boolean printMatiere, Long idPeriode, final JsonObject classe) {

        JsonObject moyenneEleve = getObjectForPeriode(
                matiere.getJsonArray("moyennes"), idPeriode, "id");
        JsonObject moyenneClasse = getObjectForPeriode(
                matiere.getJsonArray("moyennesClasse"), idPeriode, "id");
        JsonObject positionnement = getObjectForPeriode(
                matiere.getJsonArray("positionnements_auto"), idPeriode,
                ID_PERIODE);
        JsonObject positionnementFinal = getObjectForPeriode(
                matiere.getJsonArray("positionnementsFinaux"), idPeriode,
                ID_PERIODE);
        JsonObject appreciation = null;
        JsonObject res = getObjectForPeriode(
                matiere.getJsonArray("appreciations"), idPeriode,
                ID_PERIODE);
        JsonArray appreciationByClasse = null;
        if (res != null) {
            appreciationByClasse = res.getJsonArray("appreciationByClasse");
        }
        if (appreciationByClasse != null && appreciationByClasse.size()> 0) {
            printMatiere = true;
            appreciation = appreciationByClasse.getJsonObject(0);
        }
        JsonObject moyenneFinale = getObjectForPeriode(
                matiere.getJsonArray("moyennesFinales"), idPeriode,
                ID_PERIODE);

        if (moyenneFinale != null) {
            printMatiere = true;
            matiere.put(MOYENNE_ELEVE, (moyenneFinale != null) ?
                    moyenneFinale.getValue("moyenneFinale") : "");
        }
        else if (moyenneEleve != null) {
            printMatiere = true;
            matiere.put(MOYENNE_ELEVE, moyenneEleve.getValue(MOYENNE));

        }

        if (positionnementFinal != null) {
            printMatiere = true;
            matiere.put(POSITIONNEMENT,
                    positionnementFinal.getInteger("positionnementFinal"));

        }
        else {
            // On récupère la moyenne des positionements et on la convertie
            // Grâce à l'échelle de conversion du cycle de la classe de l'élève
            if(positionnement != null) {
                Float pos = positionnement.getFloat(MOYENNE);
                String val = "";
                JsonArray tableauDeconversion = classe
                        .getJsonArray("tableauDeConversion");

                if (pos != null && pos != -1 && tableauDeconversion != null) {
                    int posConverti = getPositionnementValue(pos + 1 ,
                            tableauDeconversion);
                    if (posConverti != -1) {
                        val = String.valueOf(posConverti);
                    }
                }
                printMatiere = true;
                matiere.put(POSITIONNEMENT, val);
            }
        }
        String elementsProgramme = troncateLibelle(
                matiere.getString(ELEMENTS_PROGRAMME), MAX_SIZE_LIBELLE);

        String app = "";

        if(appreciation != null) {
            app = troncateLibelle(
                    appreciation.getString("appreciation"), MAX_SIZE_LIBELLE);
            printMatiere = true;
        }

        // Construction des libelles et de leur style.
        matiere.put(ELEMENTS_PROGRAMME, elementsProgramme)
                .put("elementsProgrammeStyle", fontSize(elementsProgramme,
                        MAX_SIZE_LIBELLE))

                .put(MOYENNE_CLASSE, (moyenneClasse != null) ?
                        moyenneClasse.getValue(MOYENNE) : "")

                .put("appreciation",app)
                .put("appreciationStyle", fontSize(app, MAX_SIZE_LIBELLE))
                .put(PRINT_MATIERE_KEY, printMatiere);

        JsonArray teachers = matiere.getJsonArray("teachers");

        // Rajout de la première lettre du prenom des enseignants
        if (teachers != null && teachers.size() > 0) {
            for(int j=0; j< teachers.size(); j++) {
                JsonObject teacher = teachers.getJsonObject(j);
                String initial = teacher.getString("firstName");
                if( initial == null ) {
                    initial = "";
                }
                else {
                    initial =  String.valueOf(initial.charAt(0));
                }
                teacher.put("initial", initial);
                String name = teacher.getString(NAME);
                if( name != null ) {
                    if(j == teachers.size() -1) {
                        name += "";
                    }
                    else {
                        name += ",";
                    }
                    teacher.remove(NAME);
                    teacher.put(NAME, name);
                }

            }
        }
        return matiere;
    }
    @Override
    public JsonArray sortResultByClasseNameAndNameForBulletin(Map<String, JsonObject> mapEleves) {

        List<JsonObject> eleves = new ArrayList<>(mapEleves.values());
        Collections.sort(eleves, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "nom";
            private static final String KEY_CLASSE_NAME = "classeName";

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                try {
                    valA = a.getString(KEY_CLASSE_NAME) + a.getString(KEY_NAME);
                    valB = b.getString(KEY_CLASSE_NAME) + b.getString(KEY_NAME);
                } catch (Exception e) {
                    //do something
                }
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
                        for (int j = sortedJsonArray.size() - 1; j > (sortedJsonArray.size() - 1 - i); j--) {
                            JsonObject responsableToCheck = sortedJsonArray.getJsonObject(j);
                            java.lang.String addressResponsaleToCheck =
                                    responsableToCheck.getString(ADDRESSE_POSTALE);
                            java.lang.String addressResponsale =
                                    responsable.getString(ADDRESSE_POSTALE);

                            if (!addressResponsale.equals(addressResponsaleToCheck)) {
                                isDifferentAddress = true;
                            }
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

    private JsonObject getObjectForPeriode (JsonArray array, Long idPeriode, String key) {
        JsonObject res = null;
        if(array != null) {
            for (int i = 0; i < array.size(); i++) {
                JsonObject o = array.getJsonObject(i);
                if (o != null &&  o.getLong(key) != null && o.getLong(key).equals(idPeriode)) {
                    res = o;
                }
            }
        }
        return res;
    }

    private JsonObject setResponsablesLibelle(JsonObject o, JsonObject responsable) {
        JsonObject res = new JsonObject(o.getMap());
        String civilite = responsable.getString("civilite");
        String lastName = responsable.getString("lastNameRelative");
        String firstName = responsable.getString("firstNameRelative");
        String address = responsable.getString("address");
        String city = responsable.getString("city");
        String zipCode = responsable.getString("zipCode");

        if (civilite == null) {
            civilite = " ";
        }

        JsonArray responsableLibelle = new JsonArray().add( civilite + " " +  firstName + " " + lastName );
        if (address != null){
            responsableLibelle.add(address);
        }
        else {
            address = " ";
        }


        if (zipCode == null) {
            zipCode = " ";
        }

        if (city == null) {
            city = zipCode;
        }
        else {
            city = zipCode + " " + city;
        }

        responsableLibelle.add(city);


        res.put("responsableLibelle", responsableLibelle);
        res.put(ADDRESSE_POSTALE, address + city);
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

    /**
     * A partir d'un positionnement calculé pos, retourne  le positionnement réel avec l'échelle de conversion
     * @param moyenne moyenne calculée du positionnement
     * @param tableauDeconversion tableau de conversion des niveaux du cycle de la classe
     * @return la valeur convertie grâce à l'échelle
     */
    private int getPositionnementValue(Float moyenne, JsonArray tableauDeconversion) {
        int value =-1;

        for (int i = 0; i< tableauDeconversion.size(); i++) {
            JsonObject ligne = tableauDeconversion.getJsonObject(i);
            Float valmin = ligne.getFloat("valmin");
            Float valmax = ligne.getFloat("valmax");
            int ordre = ligne.getInteger("ordre");

            if((valmin <= moyenne && valmax > moyenne) && ordre != tableauDeconversion.size()){
                value = ordre;
            }else if((valmin <= moyenne && valmax >= moyenne) && ordre == tableauDeconversion.size() ){
                value = ordre;
            }
        }
        return  value;
    }


    // Rappelle l'évent Bus si l'erreur est un timeout sinon renvoit l'erreur
    private void buildErrorReponseForEb (String idEleve,
                                         String mess, AtomicBoolean answer, int count, JsonObject action,
                                         Handler currentHandler, Handler finalHandler, JsonObject eleve,
                                         String method) {

        if (mess.contains("Time") && !answer.get()) {
            count++;
            eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                    Competences.DELIVERY_OPTIONS,
                    handlerToAsyncHandler(currentHandler));
        }
        else {
            if (eleve.getJsonArray("errors") == null) {
                eleve.put("errors", new JsonArray());
            }
            JsonArray errors = eleve.getJsonArray("errors");
            errors.add(method);
            serviceResponseOK(answer, finalHandler, count, idEleve, method);
        }
    }


    /*
      Method assurant la réponse de chaque service lancé
     */
    private void serviceResponseOK (AtomicBoolean answer, Handler<Either<String, JsonObject>> finalHandler,
                                    int count, String idEleve, String method) {
        if (count > 1 ) {

            log.info("[ "+ method + " ] : "
                    + idEleve + " success " + count);
        }
        if(!answer.get()) {
            answer.set(true);
            log.info(" -------[" + method + "]: " + idEleve + " FIN " );
            finalHandler.handle(new Either.Right<>(null));
        }
    }
}
