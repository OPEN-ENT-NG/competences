package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.BilanPeriodiqueService;
import fr.openent.competences.service.ElementBilanPeriodiqueService;
import fr.openent.competences.service.ExportBulletinService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import static fr.openent.competences.Competences.TRANSITION_CONFIG;
import static fr.wseduc.webutils.http.Renders.getHost;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultExportBulletinService implements ExportBulletinService{
    private static final Logger log = LoggerFactory.getLogger(DefaultExportBulletinService.class);
    private EventBus eb;
    private BilanPeriodiqueService bilanPeriodiqueService;
    private ElementBilanPeriodiqueService  elementBilanPeriodiqueService;
    private final DefaultAppreciationCPEService appreciationCPEService;
    private final DefaultSyntheseBilanPeriodiqueService syntheseBilanPeriodiqueService;
    private final int MAX_SIZE_LIBELLE = 300;
    private final int MAX_SIZE_LIBELLE_PROJECT = 600;
    private final int MAX_SIZE_APPRECIATION_CPE = 600;
    private final int MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE = 600;

    public DefaultExportBulletinService(EventBus eb) {
        this.eb = eb;
        bilanPeriodiqueService = new DefaultBilanPerioqueService(eb);
        elementBilanPeriodiqueService = new DefaultElementBilanPeriodiqueService(eb);
        appreciationCPEService = new DefaultAppreciationCPEService();
        syntheseBilanPeriodiqueService = new DefaultSyntheseBilanPeriodiqueService();
    }


    private String getLibelle(String key) {
        return I18n.getInstance().translate(key,
                I18n.DEFAULT_DOMAIN, Locale.FRANCE);
    }


    @Override
    public void putLibelleForExport(String idEleve, Map<String , JsonObject> elevesMap, JsonObject params,
                                    Handler<Either<String, JsonObject>> finalHandler){

        JsonObject eleve = elevesMap.get(idEleve);
        if(eleve == null) {
            logStudentNotFound(idEleve,"putLibelleForExport");
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
                    .put("getResponsable", params.getBoolean("getResponsable"))
                    .put("getMoyenneClasse", params.getBoolean("moyenneClasse"))
                    .put("getMoyenneEleve", params.getBoolean("moyenneEleve"))
                    .put("getPositionnement", params.getBoolean("positionnement"))
                    .put("showProjects", params.getBoolean("showProjects"))
                    .put("showFamily", params.getBoolean("showFamily"))
                    .put("getProgramElements", params.getBoolean("getProgramElements"))
                    .put("showBilanPerDomaines", params.getBoolean("showBilanPerDomaines"));

        }
        finalHandler.handle(new Either.Right<>(null));
    }

    @Override
    public void getExportBulletin(final HttpServerRequest request, final AtomicBoolean answered, String idEleve,
                                  Map<String, JsonObject> elevesMap, Long idPeriode, JsonObject params,
                                  Handler<Either<String, JsonObject>> finalHandler){
        try {

            if (!answered.get()) {
                putLibelleForExport(idEleve, elevesMap, params, finalHandler);
                getEvenements(idEleve, elevesMap, idPeriode, finalHandler);
                getSuiviAcquis(idEleve, elevesMap, idPeriode, finalHandler);
                getSyntheseBilanPeriodique(idEleve, elevesMap, idPeriode, finalHandler);
                getStructure(idEleve, elevesMap, finalHandler);
                getHeadTeachers(idEleve, elevesMap, finalHandler);
                getLibellePeriode(request, idEleve, elevesMap, idPeriode, finalHandler);
                getAnneeScolaire(idEleve, elevesMap, idPeriode, finalHandler);
                getCycle(idEleve,elevesMap,idPeriode,finalHandler);
                getAppreciationCPE(idEleve, elevesMap, idPeriode, finalHandler);
                if(params.getBoolean("getResponsable")) {
                    getResponsables(idEleve, elevesMap, finalHandler);
                }
                if (params.getBoolean("showProjects")) {
                    getProjets(idEleve, elevesMap, idPeriode, finalHandler);
                }
            }
            else {
                log.error("[getExportBulletin] : Problème de parallelisation Lors de l'export des bulletin ");
            }
        }
        catch (Exception e) {
            log.error("getBulletin ", e);
        }
    }

    @Override
    public void getCycle ( String idEleve,  Map<String,JsonObject> elevesMap,Long idPeriode,
                           Handler<Either<String, JsonObject>> finalHandler) {
        JsonObject eleve = elevesMap.get(idEleve);
        if (eleve == null) {
            logStudentNotFound(idEleve, "getCycle");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {

            String idClasse = eleve.getString("idClasse");

            if (idClasse == null) {
                log.error("[getCycle]| Object eleve doesn't contains field idClasse ");
                finalHandler.handle(new Either.Right<>(null));
            }
            else {
                JsonObject action = new JsonObject()
                        .put("action", "eleve.getCycle")
                        .put("idClasse", idClasse);

                eb.send(Competences.VIESCO_BUS_ADDRESS, action,Competences.DELIVERY_OPTIONS,
                        handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                            private int count = 1;
                            private boolean answer = false;
                            @Override
                            public void handle(Message<JsonObject> result) {
                                JsonObject body = result.body();

                                if (!"ok".equals(body.getString("status"))) {
                                    String message =  body.getString("message");
                                    log.error("[getCycle] : " + idEleve + " " + message + count);

                                    if (message.contains("Time")) {
                                        count++;
                                        if (count < elevesMap.size() * 8) {

                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                                    Competences.DELIVERY_OPTIONS,
                                                    handlerToAsyncHandler(this));
                                        }
                                        else {
                                            answer = true;
                                        }
                                    }
                                    else {
                                        answer = true;
                                        if (eleve.getJsonArray("errors") == null) {
                                            eleve.put("errors", new JsonArray());
                                        }
                                        JsonArray errors = eleve.getJsonArray("errors");
                                        errors.add("getCycle");
                                    }
                                }
                                else{
                                    answer = true;
                                    JsonArray results = body.getJsonArray("results");
                                    if(results.size() > 0) {
                                        final String libelle = results.getJsonObject(0)
                                                .getString("libelle");
                                        eleve.put("bilanCycle", getLibelle("evaluations.bilan.trimestriel.of")
                                                + libelle);
                                    }
                                    else {
                                        log.error("[getCycle] | no link to cycle for object  " + idClasse);
                                    }
                                }
                                if(answer) {
                                    if(count > 1) {
                                        log.info("[getCycle] : " + idEleve + " success " + count);
                                    }
                                    finalHandler.handle(new Either.Right<>(null));
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
        if(eleve == null) {
            logStudentNotFound(idEleve, "getLibellePeriode");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            JsonObject jsonRequest = new JsonObject()
                    .put("headers", new JsonObject().put("Accept-Language",
                            request.headers().get("Accept-Language")))
                    .put("Host", getHost(request));

            JsonObject action = new JsonObject()
                    .put("action", "periode.getLibellePeriode")
                    .put("idType", idPeriode)
                    .put("request", jsonRequest);

            eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                    handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                        private int count = 1;
                        private boolean answer = false;
                        @Override
                        public void handle(Message<JsonObject> message) {
                            JsonObject body = message.body();

                            if (!"ok".equals(body.getString("status"))) {
                                String mess =  body.getString("message");
                                log.error("[ getLibellePeriode ] : " + idEleve + " " + mess + " " + count);
                                if (mess.contains("Time")) {
                                    count++;
                                    if (count < elevesMap.size() * 8) {

                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                                Competences.DELIVERY_OPTIONS,
                                                handlerToAsyncHandler(this));
                                    }
                                    else {
                                        answer = true;
                                    }
                                }
                                else {
                                    answer = true;
                                    if (eleve.getJsonArray("errors") == null) {
                                        eleve.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleve.getJsonArray("errors");
                                    errors.add("getLibellePeriode");
                                }

                            } else {
                                answer = true;
                                String periodeName = body.getString("result");
                                eleve.put("periode", periodeName);
                            }
                            if(answer) {
                                if (count > 1) {
                                    log.info("[ getLibellePeriode ] : " + idEleve + " success " + count);
                                }
                                finalHandler.handle(new Either.Right<>(null));
                            }
                        }
                    }));
        }
    }


    @Override
    public void getAnneeScolaire(String idEleve,
                                 Map<String, JsonObject> elevesMap, Long idPeriode,
                                 Handler<Either<String, JsonObject>> finalHandler) {
        JsonObject eleve = elevesMap.get(idEleve);
        if (eleve == null) {
            logStudentNotFound(idEleve, "getAnneeScolaire");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            String idClasse = eleve.getString("idClasse");

            if (idClasse == null) {
                logidClasseNotFound(idEleve, "getAnneeScolaire");
            }
            else {
                JsonObject action = new JsonObject();
                action.put("action", "periode.getPeriodes")
                        .put("idGroupes", new fr.wseduc.webutils.collections.JsonArray().add(idClasse));

                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                        handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                            private int count = 1;
                            private boolean answer = false;
                            @Override
                            public void handle(Message<JsonObject> message) {
                                JsonObject body = message.body();
                                JsonArray periodes = body.getJsonArray("result");
                                String mess = body.getString("message");

                                if (!"ok".equals(body.getString("status"))) {
                                    log.error("[getAnneeScolaire] : " + idEleve + " " + mess + " " + count);

                                    if (mess.contains("Time")) {
                                        count++;
                                        if (count < elevesMap.size() * 8) {

                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                                    Competences.DELIVERY_OPTIONS,
                                                    handlerToAsyncHandler(this));
                                        }
                                        else {
                                            answer = true;
                                        }
                                    }
                                    else {
                                        answer = true;
                                        if (eleve.getJsonArray("errors") == null) {
                                            eleve.put("errors", new JsonArray());
                                        }
                                        JsonArray errors = eleve.getJsonArray("errors");
                                        errors.add("getAnneeScolaire");
                                    }
                                }
                                else {
                                    answer = true;
                                    Long debut = null;
                                    Long fin = null;
                                    for (int i = 0; i < periodes.size(); i++) {
                                        JsonObject periode = periodes.getJsonObject(i);
                                        String debutPeriode = periode.getString("timestamp_dt")
                                                .split("T")[0];
                                        String finPeriode = periode.getString("timestamp_fn")
                                                .split("T")[0];

                                        DateFormat formatter = new SimpleDateFormat("yy-MM-dd");
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
                                }
                                if (answer) {
                                    if(count > 1){
                                        log.info("[getAnneeScolaire] : " + idEleve + " success " + count);
                                    }
                                    finalHandler.handle(new Either.Right<>(null));
                                }
                            }
                        }));
            }
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
        if (eleveObject == null) {
            logStudentNotFound(idEleve, "getProjets");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            // gets Projects
            String idClasse = eleveObject.getString("idClasse");
            String idEtablissement = eleveObject.getString("idEtablissement");

            elementBilanPeriodiqueService.getElementsBilanPeriodique(null, idClasse,
                    idEtablissement, new Handler<Either<String, JsonArray>>() {
                        private int count = 1;
                        private boolean answer = false;
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isLeft()) {
                                String message = event.left().getValue();
                                log.error("[getProjets] :" + idEleve + " " + message + " " + count);
                                if (message.contains("Time")) {
                                    count++;
                                    if (count < elevesMap.size() * 8) {
                                        elementBilanPeriodiqueService.getElementsBilanPeriodique(null,
                                                idClasse,
                                                idEtablissement, this);
                                    }
                                    else {
                                        answer = true;
                                    }
                                }
                                else {
                                    answer = true;
                                    if (eleveObject.getJsonArray("errors") == null) {
                                        eleveObject.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray("errors");
                                    errors.add("getProjets");
                                }
                                if(answer){
                                    finalHandler.handle(new Either.Right<>(null));
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
                                JsonObject epi = new JsonObject().put("libelle",
                                        getLibelle("enseignements.pratiques.interdisciplinaires"))
                                        .put("hasProject", false);
                                JsonObject ap = new JsonObject().put("libelle",
                                        getLibelle("accompagnements.personnalises"))
                                        .put("hasProject", false);
                                JsonObject parcours = new JsonObject().put("libelle",
                                        getLibelle("parcours.educatifs"))
                                        .put("hasProject", false);

                                if (elementBilanPeriodique == null) {
                                    finalHandler.handle(new Either.Right<>(null));
                                }
                                else {
                                    for(int i = 0; i< elementBilanPeriodique.size(); i++) {
                                        JsonObject element = elementBilanPeriodique.getJsonObject(i);
                                        if (element != null) {
                                            Long idElement = element.getLong("id");
                                            Long typeElement = element.getLong("type");
                                            idElements.add(idElement.toString());
                                            mapElement.put(idElement, element);
                                            if (3L == typeElement) {
                                                element.put("hasLibelle", false);
                                                if(parcours.getJsonArray("elements") == null) {
                                                    parcours.put("elements", new JsonArray().add(element));
                                                }
                                                else {
                                                    parcours.getJsonArray("elements").add(element);
                                                }
                                                sethasProject(parcours,true);
                                            }
                                            else if (2L == typeElement) {
                                                element.put("hasLibelle", true);
                                                if(ap.getJsonArray("elements") == null) {
                                                    ap.put("elements", new JsonArray().add(element));
                                                }
                                                else {
                                                    ap.getJsonArray("elements").add(element);
                                                }
                                                sethasProject(ap,true);
                                            }
                                            else if (1L == typeElement) {
                                                element.put("hasLibelle", true);
                                                if(epi.getJsonArray("elements") == null) {
                                                    epi.put("elements", new JsonArray().add(element));
                                                }
                                                else {
                                                    epi.getJsonArray("elements").add(element);
                                                }
                                                sethasProject(epi,true);
                                            }
                                        }
                                    }
                                    eleveObject.put("projects", new JsonArray().add(epi).add(ap).add(parcours));
                                    if(idElements.size() > 0) {
                                        elementBilanPeriodiqueService.getAppreciations(idClasses,
                                                idPeriode.toString(), idElements, idEleve,
                                                new Handler<Either<String, JsonArray>>() {
                                                    private int count = 1;
                                                    private boolean answer = false;
                                                    @Override
                                                    public void handle(Either<String, JsonArray> event) {
                                                        if (event.isLeft()) {
                                                            String message = event.left().getValue();
                                                            log.error("[getProjets | getAppreciations ] : " +
                                                                    idEleve + " " + message + " " + count);
                                                            if (message.contains("Time")) {
                                                                count++;
                                                                if (count < elevesMap.size() * 8) {
                                                                    elementBilanPeriodiqueService
                                                                            .getAppreciations(idClasses,
                                                                                    idPeriode.toString(),
                                                                                    idElements, idEleve, this);
                                                                }
                                                                else {
                                                                    answer = true;
                                                                }
                                                            }
                                                            else {
                                                                answer = true;
                                                                if (eleveObject.getJsonArray("errors") == null) {
                                                                    eleveObject.put("errors", new JsonArray());
                                                                }
                                                                JsonArray errors = eleveObject
                                                                        .getJsonArray("errors");
                                                                errors.add("getProjets");
                                                            }
                                                        }
                                                        else {
                                                            answer = true;
                                                            JsonArray appreciations = event.right().getValue();
                                                            for(int i=0; i< appreciations.size(); i++) {
                                                                JsonObject app = appreciations.getJsonObject(i);
                                                                Long periodeId = app.getLong("id_periode");
                                                                if(periodeId == idPeriode) {
                                                                    String com = app.getString("commentaire");

                                                                    Long idElem = app.getLong(
                                                                            "id_elt_bilan_periodique");
                                                                    mapElement.get(idElem).put("commentaire",
                                                                            troncateLibelle(com,
                                                                                    MAX_SIZE_LIBELLE_PROJECT))
                                                                            .put("commentaireStyle",
                                                                                    fontSizeProject(com,
                                                                                            MAX_SIZE_LIBELLE_PROJECT));
                                                                }
                                                            }
                                                        }
                                                        if(answer) {
                                                            if (count > 1 ) {
                                                                log.info("[getProjets | getAppreciations ] : "
                                                                        + idEleve + " success " + count);
                                                            }
                                                            finalHandler.handle(new Either.Right<>(null));
                                                        }
                                                    }
                                                });
                                    }
                                    else {
                                        log.info(" [getProjets] | NO elements founds for classe " + idClasse);
                                        finalHandler.handle(new Either.Right<>(null));
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
        if (eleveObject == null) {
            logStudentNotFound(idEleve, "getSyntheseBilanPeriodique");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(idPeriode, idEleve,
                    new Handler<Either<String, JsonObject>>() {
                        private int count = 1;
                        private boolean answer = false;
                        @Override
                        public void handle(Either<String, JsonObject> event) {
                            if(event.isLeft()){
                                String message = event.left().getValue();
                                log.error("[getSyntheseBilanPeriodique ] : " + idEleve  + " " + message + " " + count);
                                if (message.contains("Time")) {
                                    count++;
                                    if (count < elevesMap.size() * 8) {
                                        syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(idPeriode, idEleve,
                                                this);
                                    }
                                    else {
                                        answer = true;
                                    }
                                }
                                else {
                                    answer = true;
                                    if (eleveObject.getJsonArray("errors") == null) {
                                        eleveObject.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray("errors");
                                    errors.add("getSyntheseBilanPeriodique");
                                }
                            }
                            else {
                                answer = true;
                                JsonObject synthese = event.right().getValue();
                                if (synthese != null) {
                                    String syntheseStr = synthese.getString("synthese");
                                    eleveObject.put("syntheseBilanPeriodque",troncateLibelle(syntheseStr,
                                            MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE));
                                    eleveObject.put("syntheseBilanPeriodqueStyle",fontSize(syntheseStr,
                                            MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE));
                                }
                            }
                            if(answer) {
                                if (count > 1 ) {
                                    log.info("[getSyntheseBilanPeriodique ] : " + idEleve + " success " + count);
                                }
                                finalHandler.handle(new Either.Right<>(null));
                            }
                        }
                    });
        }

    }

    public void getAppreciationCPE (String idEleve,  Map<String,JsonObject> elevesMap, Long idPeriode,
                                    Handler<Either<String, JsonObject>> finalHandler){
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, "getAppreciationCPE");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            appreciationCPEService.getAppreciationCPE(idPeriode, idEleve, new Handler<Either<String, JsonObject>>() {
                private int count = 1;
                private boolean answer = false;
                @Override
                public void handle(Either<String, JsonObject> event) {
                    if (event.isLeft()) {
                        String message = " " + event.left().getValue();
                        log.error("[getAppreciationCPE ] : " + idEleve + " " + message + " " + count);
                        if (message.contains("Time")) {
                            count++;
                            if (count < elevesMap.size() * 8) {
                                appreciationCPEService.getAppreciationCPE(idPeriode, idEleve, this);
                            }
                            else {
                                answer = true;
                            }
                        }
                        else {
                            answer = true;
                            if (eleveObject.getJsonArray("errors") == null) {
                                eleveObject.put("errors", new JsonArray());
                            }
                            JsonArray errors = eleveObject.getJsonArray("errors");
                            errors.add("getAppreciationCPE");
                        }
                    } else {
                        answer = true;
                        JsonObject appreciationCPE = event.right().getValue();

                        if (appreciationCPE != null) {
                            String app = troncateLibelle(appreciationCPE.getString("appreciation"),
                                    MAX_SIZE_APPRECIATION_CPE);
                            eleveObject.put("appreciationCPE",app)
                                    .put("appreciationCPEStyle",fontSize(app, MAX_SIZE_APPRECIATION_CPE));
                        }
                    }
                    if (answer) {
                        if (count > 1 ) {
                            log.info("[getAppreciationCPE ] : " + idEleve + " success " + count);
                        }
                        finalHandler.handle(new Either.Right<>(null));
                    }
                }
            });
        }
    }
    @Override
    public void getStructure( String idEleve, Map<String,JsonObject> elevesMap,
                              Handler<Either<String, JsonObject>> finalHandler) {

        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve,"getStructure");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            JsonObject action = new JsonObject();
            String idStructure = eleveObject.getString("idEtablissement");
            action.put("action", "structure.getStructure")
                    .put("idStructure", idStructure);

            eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                    Competences.DELIVERY_OPTIONS,
                    handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                        private int count = 1;
                        private boolean answer = false;
                        @Override
                        public void handle(Message<JsonObject> message) {
                            JsonObject body = message.body();

                            if (!"ok".equals(body.getString("status"))) {
                                String mess = body.getString("message");
                                log.error("[getStructure ] : " + idEleve + " " + mess + " " + count);

                                if (mess.contains("Time")) {
                                    count++;
                                    if (count < elevesMap.size() * 8) {

                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                                Competences.DELIVERY_OPTIONS,
                                                handlerToAsyncHandler(this));
                                    }
                                    else {
                                        answer = true;
                                    }
                                }
                                else {
                                    answer = true;
                                    if (eleveObject.getJsonArray("errors") == null) {
                                        eleveObject.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray("errors");
                                    errors.add("getStructure");
                                }
                            } else {
                                answer = true;
                                JsonObject structure = body.getJsonObject("result");
                                JsonArray structureLibelle = new JsonArray();
                                if(structure != null){
                                    structure = structure.getJsonObject("s");
                                    if(structure != null) {
                                        structure = structure.getJsonObject("data");
                                        String academy = structure.getString("academy");
                                        String type = structure.getString("type");
                                        String name = structure.getString("name");
                                        String address = structure.getString("address");
                                        String codePostal = structure.getString("zipCode");
                                        String phone = structure.getString("phone");
                                        String email = structure.getString("email");
                                        String city =  structure.getString("city");
                                        if(academy != null) {
                                            structureLibelle.add(new JsonObject().put("academy", academy));}
                                        if(type != null) {
                                            structureLibelle.add(new JsonObject().put("type", type));}
                                        if(name != null) {structureLibelle.add(new JsonObject().put("name",name));}
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
                            }
                            if (answer) {
                                if (count > 1 ) {
                                    log.info("[getStructure ] : " + idEleve + " success " + count);
                                }
                                finalHandler.handle(new Either.Right<>(null));
                            }
                        }
                    }));
        }
    }

    @Override
    public void getHeadTeachers( String idEleve, Map<String,JsonObject> elevesMap,
                                 Handler<Either<String, JsonObject>> finalHandler) {

        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve,"getHeadTeachers");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            JsonObject action = new JsonObject();
            String idClasse = eleveObject.getString("idClasse");
            if (idClasse == null) {
                logidClasseNotFound(idEleve, "getHeadTeachers");
                finalHandler.handle(new Either.Right<>(null));
            }
            else {
                action.put("action", "classe.getHeadTeachersClasse")
                        .put("idClasse", idClasse);

                eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                        Competences.DELIVERY_OPTIONS,
                        handlerToAsyncHandler(
                                new Handler<Message<JsonObject>>() {
                                    private int count = 1;
                                    private boolean answer = false;
                                    @Override
                                    public void handle(Message<JsonObject> message) {
                                        JsonObject body = message.body();


                                        if (!"ok".equals(body.getString("status"))) {
                                            String mess = body.getString("message");
                                            log.error("[getHeadTeachers ] : " + idEleve + " " + mess + " " + count);

                                            if (mess.contains("Time")) {
                                                count++;
                                                if (count < elevesMap.size() * 8) {

                                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                                            Competences.DELIVERY_OPTIONS,
                                                            handlerToAsyncHandler(this));
                                                }
                                                else {
                                                    answer = true;
                                                }
                                            }
                                            else {
                                                answer = true;

                                                if (eleveObject.getJsonArray("errors") == null) {
                                                    eleveObject.put("errors", new JsonArray());
                                                }
                                                JsonArray errors = eleveObject.getJsonArray("errors");
                                                errors.add("getHeadTeachers");
                                            }
                                        } else {
                                            answer = true;
                                            JsonArray headTeachers = body.getJsonArray("results");
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
                                        }
                                        if (answer) {
                                            if (count > 1 ) {
                                                log.info("[getHeadTeachers] : " + idEleve + " success " + count);
                                            }
                                            finalHandler.handle(new Either.Right<>(null));
                                        }
                                    }
                                }));
            }
        }
    }

    @Override
    public void getResponsables( String idEleve, Map<String,JsonObject> elevesMap,
                                 Handler<Either<String, JsonObject>> finalHandler) {


        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, "getResponsables");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            JsonObject action = new JsonObject();
            action.put("action", "eleve.getResponsables")
                    .put("idEleve", idEleve);
            eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                    Competences.DELIVERY_OPTIONS,
                    handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                        private int count = 1;
                        private boolean answer = false;
                        @Override
                        public void handle(Message<JsonObject> message) {
                            JsonObject body = message.body();

                            if (!"ok".equals(body.getString("status"))) {
                                String mess = body.getString("message");
                                log.error("[getResponsables ] : " + idEleve + " " + mess + " " + count);

                                if (mess.contains("Time")) {
                                    count++;
                                    if (count < elevesMap.size() * 8) {

                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                                Competences.DELIVERY_OPTIONS,
                                                handlerToAsyncHandler(this));
                                    }
                                    else {
                                        answer = true;
                                    }
                                }
                                else {
                                    answer = true;
                                    if (eleveObject.getJsonArray("errors") == null) {
                                        eleveObject.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray("errors");
                                    errors.add("getResponsables");
                                }
                            } else {
                                answer = true;
                                JsonArray responsables = body.getJsonArray("results");
                                eleveObject.put("responsables", responsables);
                            }
                            if (answer) {
                                if (count > 1 ) {
                                    log.info("[getHeadTeachers] : " + idEleve + " success " + count);
                                }
                                finalHandler.handle(new Either.Right<>(null));
                            }
                        }
                    }));
        }
    }

    @Override
    public void getEvenements(String idEleve,Map<String, JsonObject> elevesMap, Long idPeriode,
                              Handler<Either<String, JsonObject>> finalHandler ) {
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, "getEvenements");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            bilanPeriodiqueService.getRetardsAndAbsences(idEleve, new Handler<Either<String, JsonArray>>() {
                private int count = 1;
                private boolean answer = false;

                @Override
                public void handle(Either<String, JsonArray> event) {


                    if (event.isLeft()) {
                        String message = event.left().getValue();
                        log.error("[getEvenements ] : " + idEleve + " " + message + count);

                        if (message.contains("Time")) {
                            count++;
                            if (count < elevesMap.size() * 8) {
                                bilanPeriodiqueService.getRetardsAndAbsences(idEleve, this);
                            }
                            else {
                                answer = true;
                            }

                        }
                        else {
                            answer = true;
                            if (eleveObject.getJsonArray("errors") == null) {
                                eleveObject.put("errors", new JsonArray());
                            }
                            JsonArray errors = eleveObject.getJsonArray("errors");
                            errors.add("getEvenements");
                        }
                    }
                    else {
                        answer = true;
                        if (count > 1 ) {
                            log.info("[getEvenements ] : "
                                    + eleveObject.getString("idEleve") + " success " + count);
                        }
                        JsonArray evenements = event.right().getValue();
                        if (eleveObject != null) {

                            Long absTotale = 0L;
                            Long absTotaleHeure = 0L;
                            Long absNonJust = 0L;
                            Long absNonJustHeure = 0L;
                            Long absJust = 0L;
                            Long absJustHeure = 0L;
                            Long retard = 0L;

                            for (int i = 0; i < evenements.size(); i++) {
                                JsonObject ev = evenements.getJsonObject(i);
                                Long evAbsTot = ev.getLong("abs_totale");
                                Long evAbsTotH = ev.getLong("abs_totale_heure");
                                Long evAbsNonJust = ev.getLong("abs_non_just");
                                Long evAbsNonJustH = ev.getLong(" abs_non_just_heure");
                                Long evAbsJust = ev.getLong("abs_just");
                                Long evAbsJustH = ev.getLong("abs_just_heure");
                                Long evRetard = ev.getLong("retard");

                                if (ev.getLong("id_periode") == idPeriode || idPeriode == null) {
                                    absTotale += ((evAbsTot != null) ? evAbsTot : 0L);
                                    absTotaleHeure += ((evAbsTotH != null) ? evAbsTotH : 0L);
                                    absNonJust += ((evAbsNonJust != null) ? evAbsNonJust : 0L);
                                    absNonJustHeure += ((evAbsNonJustH != null) ? evAbsNonJustH : 0L);
                                    absJust += ((evAbsJust != null) ? evAbsJust : 0L);
                                    absJustHeure += ((evAbsJustH != null) ? evAbsJustH : 0L);
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

                    }
                    if (answer) {
                        finalHandler.handle(new Either.Right<>(null));
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
                               Handler<Either<String, JsonObject>> finalHandler ) {
        JsonObject eleveObject = elevesMap.get(idEleve);
        if (eleveObject == null) {
            logStudentNotFound(idEleve, "getSuiviAcquis");
            finalHandler.handle(new Either.Right<>(null));
        }
        else {
            String idEtablissement = eleveObject.getString("idEtablissement");
            String idClasse = eleveObject.getString("idClasse");
            if (idClasse == null || idEtablissement == null) {
                if(idClasse == null) {
                    logidClasseNotFound(idEleve, "getSuiviAcquis");
                }
                if (idEtablissement == null) {
                    logidEtabNotFound(idEleve, "getSuiviAcquis");
                }
                finalHandler.handle(new Either.Right<>(null));
            }
            else {
                bilanPeriodiqueService.getSuiviAcquis(idEtablissement, idPeriode, idEleve,
                        idClasse, new Handler<Either<String, JsonArray>>() {
                            private int count = 1;
                            private boolean answer = false;

                            @Override
                            public void handle(Either<String, JsonArray> event) {

                                if (event.isLeft()) {
                                    String message =  event.left().getValue();
                                    log.error("[getSuiviAcquis ] : " + idEleve + " " + message + " " + count);
                                    if (message.contains("Time")) {
                                        count ++;
                                        if (count < elevesMap.size() * 8) {
                                            bilanPeriodiqueService.getSuiviAcquis(idEtablissement, idPeriode, idEleve,
                                                    idClasse,this);
                                        }
                                        else {
                                            answer = true;
                                        }

                                    }
                                    else {
                                        answer = true;
                                        if (eleveObject.getJsonArray("errors") == null) {
                                            eleveObject.put("errors", new JsonArray());
                                        }
                                        JsonArray errors = eleveObject.getJsonArray("errors");
                                        errors.add("getSuiviAcquis");
                                    }
                                }
                                else {
                                    answer = true;

                                    JsonArray suiviAcquis = event.right().getValue();

                                    for (int i = 0; suiviAcquis != null && i < suiviAcquis.size() ; i++) {
                                        JsonObject matiere = suiviAcquis.getJsonObject(i);
                                        JsonObject moyenneEleve = getObjectForPeriode(
                                                matiere.getJsonArray("moyennes"), idPeriode, "id");
                                        JsonObject moyenneClasse = getObjectForPeriode(
                                                matiere.getJsonArray("moyennesClasse"), idPeriode, "id");
                                        JsonObject positionnement = getObjectForPeriode(
                                                matiere.getJsonArray("positionnements_auto"), idPeriode,
                                                "id_periode");
                                        JsonObject positionnementFinal = getObjectForPeriode(
                                                matiere.getJsonArray("positionnementsFinaux"), idPeriode,
                                                "id_periode");
                                        JsonObject appreciation = null;
                                        JsonObject res = getObjectForPeriode(
                                                matiere.getJsonArray("appreciations"), idPeriode,
                                                "id_periode");
                                        JsonArray appreciationByClasse = null;
                                        if (res != null) {
                                            appreciationByClasse = res.getJsonArray("appreciationByClasse");
                                        }
                                        if (appreciationByClasse != null && appreciationByClasse.size()> 0) {
                                            appreciation = appreciationByClasse.getJsonObject(0);
                                        }
                                        JsonObject moyenneFinale = getObjectForPeriode(
                                                matiere.getJsonArray("moyennesFinales"), idPeriode,
                                                "id_periode");

                                        if (moyenneFinale != null) {
                                            matiere.put("moyenneEleve", (moyenneFinale != null) ?
                                                    moyenneFinale.getValue("moyenneFinale") : "");
                                        }
                                        else {
                                            matiere.put("moyenneEleve", (moyenneEleve != null) ?
                                                    moyenneEleve.getValue("moyenne") : "");
                                        }

                                        if (positionnementFinal != null) {
                                            matiere.put("positionnement", (positionnementFinal != null) ?
                                                    positionnementFinal.getInteger("positionnementFinal") : "");

                                        }
                                        else {
                                            if(positionnement != null) {
                                                Integer pos = positionnement.getInteger("moyenne");
                                                String val = "";

                                                if (pos != null && pos != -1) {
                                                    pos += new Integer(1);
                                                    val = pos.toString();
                                                }
                                                matiere.put("positionnement", val);
                                            }
                                        }
                                        String elementsProgramme = troncateLibelle(
                                                matiere.getString("elementsProgramme"), MAX_SIZE_LIBELLE);

                                        String app = "";

                                        if(appreciation != null) {
                                            app = troncateLibelle(
                                                    appreciation.getString("appreciation"), MAX_SIZE_LIBELLE);
                                        }
                                        matiere.put("elementsProgramme", elementsProgramme)
                                                .put("elementsProgrammeStyle", fontSize(elementsProgramme,
                                                        MAX_SIZE_LIBELLE))

                                                .put("moyenneClasse", (moyenneClasse != null) ?
                                                        moyenneClasse.getValue("moyenne") : "")

                                                .put("appreciation",app)
                                                .put("appreciationStyle", fontSize(app, MAX_SIZE_LIBELLE));

                                        JsonArray teachers = matiere.getJsonArray("teachers");
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
                                                String name = teacher.getString("name");
                                                if( name != null ) {
                                                    if(j == teachers.size() -1) {
                                                        name += "";
                                                    }
                                                    else {
                                                        name += ",";
                                                    }
                                                    teacher.remove("name");
                                                    teacher.put("name", name);
                                                }

                                            }
                                        }

                                    }
                                    eleveObject.put("suiviAcquis", suiviAcquis).put("hasSuiviAcquis",
                                            (suiviAcquis.size() > 0));


                                }
                                if(answer) {
                                    if (count > 1 ) {
                                        log.info("[suiviAcquis] : "
                                                + eleveObject.getString("idEleve") + " success " + count);
                                    }
                                    finalHandler.handle(new Either.Right<>(null));
                                }
                            }
                        });
            }
        }
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
                String keyResponsable = "getResponsable";
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
                                    responsableToCheck.getString("addressePostale");
                            java.lang.String addressResponsale =
                                    responsable.getString("addressePostale");

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
                if (o.getLong(key) == idPeriode) {
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
        res.put("addressePostale", address + city);
        return res;
    }

    private void logStudentNotFound(String idEleve, String service) {
        log.error("[ export Bulletin | " + service + "] : elevesMap doesn't contains idEleve " + idEleve);
    }

    private void logidClasseNotFound(String idEleve, String service) {
        log.error("[ export Bulletin | " + service + "] : eleveObject doesn't contains field idClasse " + idEleve);
    }

    private  void logidEtabNotFound(String idEleve, String service) {
        log.error("[ export Bulletin | " + service + "] : eleveObject doesn't contains field idEtablissement " +
                idEleve);
    }
}
