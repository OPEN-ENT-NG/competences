package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.BilanPeriodiqueService;
import fr.openent.competences.service.ElementBilanPeriodiqueService;
import fr.openent.competences.service.ExportBulletinService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
    private final int MAX_SIZE_LIBELLE = 80;
    private final int MAX_SIZE_LIBELLE_PROJECT = 220;
    private final int MAX_SIZE_APPRECIATION_CPE = 230;
    private final int MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE = 560;

    public DefaultExportBulletinService(EventBus eb) {
        this.eb = eb;
        bilanPeriodiqueService = new DefaultBilanPerioqueService(eb);
        elementBilanPeriodiqueService = new DefaultElementBilanPeriodiqueService(eb);
        appreciationCPEService = new DefaultAppreciationCPEService();
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
                    .put("familyVisa", getLibelle("evaluations.export.bulletin.visa.libelle"))
                    .put("signature", getLibelle("evaluations.export.bulletin.date.name.visa.responsable"))
                    .put("bornAt", getLibelle("born.on"))
                    .put("classeOf", getLibelle("classe.of"))
                    .put("footer", "*: " + getLibelle("evaluations.export.bulletin.legendPositionnement"))

                    // positionnement des options d'impression
                    .put("getResponsable", params.getBoolean("getResponsable"))
                    .put("getMoyenneClasse", params.getBoolean("moyenneClasse"))
                    .put("getMoyenneEleve", params.getBoolean("moyenneEleve"))
                    .put("getPositionnement", params.getBoolean("positionnement"))
                    .put("showProjects", params.getBoolean("showProjects"))
                    .put("showFamily", params.getBoolean("showFamily"));

        }
        finalHandler.handle(new Either.Right<>(null));
    }

    @Override
    public void getExportBulletin(final HttpServerRequest request, final AtomicBoolean answered, String idEleve,
                                  Map<String, JsonObject> elevesMap, Long idPeriode, JsonObject params,
                                  Handler<Either<String, JsonObject>> finalHandler){
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

                eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                        handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {
                                JsonObject body = message.body();

                                if (!"ok".equals(body.getString("status"))) {
                                    if (eleve.getJsonArray("errors") == null) {
                                        eleve.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleve.getJsonArray("errors");
                                    errors.add("getCycle");
                                    log.error(body.getString("message"));
                                }
                                else{
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
                                finalHandler.handle(new Either.Right<>(null));
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
            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject body = message.body();

                    if (!"ok".equals(body.getString("status"))) {
                        if (eleve.getJsonArray("errors") == null) {
                            eleve.put("errors", new JsonArray());
                        }
                        JsonArray errors = eleve.getJsonArray("errors");
                        errors.add("getLibellePeriode");
                        log.error(body.getString("message"));
                    } else {
                        String periodeName = body.getString("result");
                        eleve.put("periode", periodeName);
                    }
                    finalHandler.handle(new Either.Right<>(null));
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

                eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                        handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {
                                JsonObject body = message.body();
                                JsonArray periodes = body.getJsonArray("result");
                                JsonArray idAvailableEleve = new JsonArray();

                                if (!"ok".equals(body.getString("status"))) {
                                    if (eleve.getJsonArray("errors") == null) {
                                        eleve.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleve.getJsonArray("errors");
                                    errors.add("getLibellePeriode");
                                    log.error(body.getString("message"));
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
                                finalHandler.handle(new Either.Right<>(null));
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
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isLeft()) {
                                if (eleveObject.getJsonArray("errors") == null) {
                                    eleveObject.put("errors", new JsonArray());
                                }
                                JsonArray errors = eleveObject.getJsonArray("errors");
                                errors.add("getProjets");
                                log.error(event.left().getValue());
                                finalHandler.handle(new Either.Right<>(null));
                            }
                            else {

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
                                                    @Override
                                                    public void handle(Either<String, JsonArray> event) {
                                                        if (event.isLeft()) {
                                                            if (eleveObject.getJsonArray("errors") == null) {
                                                                eleveObject.put("errors", new JsonArray());
                                                            }
                                                            JsonArray errors = eleveObject.getJsonArray("errors");
                                                            errors.add("getProjets");
                                                            log.error(event.left().getValue());
                                                        }
                                                        else {
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
                                                                                    MAX_SIZE_LIBELLE_PROJECT) );
                                                                }
                                                            }
                                                        }
                                                        finalHandler.handle(new Either.Right<>(null));
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
            new DefaultSyntheseBilanPeriodiqueService().getSyntheseBilanPeriodique(idPeriode, idEleve,
                    new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> event) {
                            if(event.isLeft()){
                                if (eleveObject.getJsonArray("errors") == null) {
                                    eleveObject.put("errors", new JsonArray());
                                }
                                JsonArray errors = eleveObject.getJsonArray("errors");
                                errors.add("getSyntheseBilanPeriodique");
                            }
                            else {
                                JsonObject synthese = event.right().getValue();
                                if (synthese != null) {
                                    eleveObject.put("syntheseBilanPeriodque",
                                            troncateLibelle(synthese.getString("synthese"),
                                                    MAX_SIZE_SYNTHESE_BILAN_PERIODIQUE));
                                }
                            }
                            finalHandler.handle(new Either.Right<>(null));
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
                @Override
                public void handle(Either<String, JsonObject> event) {
                    if (event.isLeft()) {
                        if (eleveObject.getJsonArray("errors") == null) {
                            eleveObject.put("errors", new JsonArray());
                        }
                        JsonArray errors = eleveObject.getJsonArray("errors");
                        errors.add("getAppreciationCPE");
                    } else {
                        JsonObject appreciationCPE = event.right().getValue();
                        if (appreciationCPE != null) {
                            eleveObject.put("appreciationCPE",
                                    troncateLibelle(appreciationCPE.getString("appreciation"),
                                            MAX_SIZE_APPRECIATION_CPE));
                        }
                    }
                    finalHandler.handle(new Either.Right<>(null));
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

            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject body = message.body();


                    if (!"ok".equals(body.getString("status"))) {
                        log.error("[getStructure ] : " + eleveObject.getString("nom") + "failed");
                        log.error(body.getString("message"));

                        if (eleveObject.getJsonArray("errors") == null) {
                            eleveObject.put("errors", new JsonArray());
                        }
                        JsonArray errors = eleveObject.getJsonArray("errors");
                        errors.add("getStructure");

                    } else {
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
                                if(academy != null) {structureLibelle.add(new JsonObject().put("academy", academy));}
                                if(type != null) {structureLibelle.add(new JsonObject().put("type", type));}
                                if(name != null) {structureLibelle.add(new JsonObject().put("name",name));}
                                if(address != null) {structureLibelle.add(new JsonObject().put("address", address));}
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
                                if(email != null) {structureLibelle.add(new JsonObject().put("couriel", email));}
                            }
                        }
                        eleveObject.put("structureLibelle", structureLibelle);
                    }
                    finalHandler.handle(new Either.Right<>(null));
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

                eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(
                        new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {
                                JsonObject body = message.body();


                                if (!"ok".equals(body.getString("status"))) {
                                    log.error("[getHeadTeachers ] : " + eleveObject.getString("nom") + "failed");
                                    log.error(body.getString("message"));

                                    if (eleveObject.getJsonArray("errors") == null) {
                                        eleveObject.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray("errors");
                                    errors.add("getHeadTeachers");

                                } else {
                                    JsonArray headTeachers = body.getJsonArray("results");
                                    if (headTeachers != null) {
                                        String headTeachersLibelle = getLibelle(
                                                (headTeachers.size() > 1) ? "headTeachers" : "headTeacher");
                                        eleveObject.put("headTeacherLibelle", headTeachersLibelle + " : ")
                                                .put("headTeachers", headTeachers);
                                    }
                                }
                                finalHandler.handle(new Either.Right<>(null));
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
            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject body = message.body();

                    if (!"ok".equals(body.getString("status"))) {
                        log.error("[getResponsables ] : " + eleveObject.getString("nom") + "failed");
                        log.error(body.getString("message"));

                        if (eleveObject.getJsonArray("errors") == null) {
                            eleveObject.put("errors", new JsonArray());
                        }
                        JsonArray errors = eleveObject.getJsonArray("errors");
                        errors.add("getResponsables");

                    } else {
                        JsonArray responsables = body.getJsonArray("results");
                        eleveObject.put("responsables", responsables);
                    }
                    finalHandler.handle(new Either.Right<>(null));
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
                @Override
                public void handle(Either<String, JsonArray> event) {


                    if (event.isLeft()) {

                        log.error("[getEvenements ] : " + eleveObject.getString("idEleve") + "failed");
                        log.error(event.left().getValue());

                        if (eleveObject.getJsonArray("errors") == null) {
                            eleveObject.put("errors", new JsonArray());
                        }
                        JsonArray errors = eleveObject.getJsonArray("errors");
                        errors.add("getEvenements");
                    } else {
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
                    finalHandler.handle(new Either.Right<>(null));
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
                            @Override
                            public void handle(Either<String, JsonArray> event) {

                                if (event.isLeft()) {

                                    log.error("[getSuiviAcquis ] : " + eleveObject.getString("idEleve")
                                            + "failed");
                                    log.error(event.left().getValue());

                                    if (eleveObject.getJsonArray("errors") == null) {
                                        eleveObject.put("errors", new JsonArray());
                                    }
                                    JsonArray errors = eleveObject.getJsonArray("errors");
                                    errors.add("getSuiviAcquis");
                                } else {
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
                                        JsonObject appreciation = getObjectForPeriode(
                                                matiere.getJsonArray("appreciations"), idPeriode,
                                                "id_periode");

                                        JsonObject moyenneFinale = getObjectForPeriode(
                                                matiere.getJsonArray("moyennesFinales"), idPeriode,
                                                "id_periode");

                                        if (moyenneFinale != null) {
                                            matiere.put("moyenneEleve", (moyenneFinale != null) ?
                                                    moyenneFinale.getValue("moyenne") : "");
                                        }
                                        else {
                                            matiere.put("moyenneEleve", (moyenneEleve != null) ?
                                                    moyenneEleve.getValue("moyenne") : "");
                                        }

                                        if (positionnementFinal != null) {
                                            matiere.put("positionnement", (positionnementFinal != null) ?
                                                    (positionnementFinal.getInteger("positionnementFinal")
                                                            + new Integer(1)) : "");
                                        }
                                        else {
                                            Integer pos = positionnement.getInteger("moyenne");
                                            String val = "";

                                            if (pos != null && pos != -1) {
                                                pos += new Integer(1);
                                                val = pos.toString();
                                            }
                                            matiere.put("positionnement", val);
                                        }
                                        String elementsProgramme = troncateLibelle(
                                                matiere.getString("elementsProgramme"), MAX_SIZE_LIBELLE);

                                        String app = "";

                                        if(appreciation != null) {
                                        app = troncateLibelle(
                                                appreciation.getString("appreciation"), MAX_SIZE_LIBELLE);
                                        }
                                        matiere.put("elementsProgramme", elementsProgramme);
                                        matiere.put("moyenneClasse", (moyenneClasse != null) ?
                                                        moyenneClasse.getValue("moyenne") : "")
                                                .put("appreciation",app);

                                        }
                                    eleveObject.put("suiviAcquis", suiviAcquis).put("hasSuiviAcquis",
                                            (suiviAcquis.size() > 0));


                                }
                                finalHandler.handle(new Either.Right<>(null));
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
