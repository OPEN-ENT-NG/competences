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

package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.constants.Field;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.helpers.UtilsHelper;
import fr.openent.competences.model.*;
import fr.openent.competences.security.AccessChildrenParentFilter;
import fr.openent.competences.security.AccessEvaluationFilter;
import fr.openent.competences.security.AccessSuiviClasse;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.*;
import fr.openent.competences.service.impl.*;
import fr.openent.competences.utils.BulletinUtils;
import fr.openent.competences.utils.UtilsConvert;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.openent.competences.service.impl.DefaultExportService.COEFFICIENT;
import static fr.openent.competences.service.impl.DefaultUtilsService.setServices;
import static fr.openent.competences.utils.UtilsConvert.strIdGroupesToJsonArray;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static java.util.Objects.isNull;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class ExportPDFController extends ControllerHelper {
    protected static final Logger log = LoggerFactory.getLogger(ExportPDFController.class);
    private final String assetsPath = "../..";
    private final Map<String, String> skins = new HashMap<String, String>();
    /**
     * Déclaration des services
     */
    private final DevoirService devoirService;
    private final UtilsService utilsService;
    private final BFCService bfcService;
    private final DomainesService domaineService;
    private final NoteService noteService;
    private final ExportService exportService;
    private final AppreciationService appreciationService;
    private final ExportBulletinService exportBulletinService;
    private final AverageService averageService;
    private final MongoExportService mongoExportService;
    private final Storage storage;
    private final ClassAppreciationService classAppreciationService;

    public ExportPDFController(ServiceFactory serviceFactory) {
        devoirService = serviceFactory.devoirService();
        utilsService = serviceFactory.utilsService();
        bfcService = serviceFactory.BFCService();
        domaineService = serviceFactory.domainService();
        noteService = serviceFactory.noteService();
        exportService = serviceFactory.exportService();
        exportBulletinService = serviceFactory.exportBulletinService();
        appreciationService = serviceFactory.appreciationService();
        averageService = serviceFactory.averageService();
        this.mongoExportService = serviceFactory.mongoExportService();
        this.storage = serviceFactory.storage();
        classAppreciationService = serviceFactory.classAppreciationService();
    }

    /**
     * Genere le releve d'un eleve sous forme de PDF
     */
    @Get("/releve/pdf")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessChildrenParentFilter.class)
    public void getReleveEleve(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                unauthorized(request);
                return;
            }

            final Long idPeriode;
            if (request.params().get("idPeriode") == null) {
                idPeriode = null;
            } else {
                try {
                    idPeriode = Long.parseLong(request.params().get("idPeriode"));
                } catch (NumberFormatException e) {
                    log.error("Error : idPeriode must be a long object", e);
                    badRequest(request, e.getMessage());
                    return;
                }
            }

            final String idEtablissement = request.params().get("idEtablissement");
            final String idEleve = request.params().get("idEleve");
            exportService.getDataForExportReleveEleve(idEleve, idEtablissement, idPeriode, request.params(), event -> {
                if (event.isLeft()) {
                    leftToResponse(request, event.left());
                    return;
                }
                JsonObject templateProps = event.right().getValue();
                String templateName = "releve-eleve.pdf.xhtml";
                String prefixPdfName = templateProps.getString("prefixPdfName");
                exportService.genererPdf(request, templateProps, templateName, prefixPdfName, vertx, config);
            });
        });
    }

    @Post("/releve/classe/pdf")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessSuiviClasse.class)
    public void getReleve(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, params -> {
            final String idClasse = params.getString(ID_CLASSE_KEY);
            final Long idPeriode = params.getLong(ID_PERIODE_KEY);
            final String idEtablissement = params.getString(ID_STRUCTURE_KEY);
            final Long idTypePeriode = params.getLong("idTypePeriode");
            final Long ordre = params.getLong(ORDRE);
            final String classeName = params.getString(CLASSE_NAME_KEY);
            final boolean showSkills = request.params().contains(Field.SKILLS) && Boolean.parseBoolean(request.params().get(Field.SKILLS));
            final boolean showScores = request.params().contains(Field.SCORES) && Boolean.parseBoolean(request.params().get(Field.SCORES));
            exportService.getDataForExportReleveClasse(idClasse, idEtablissement, idPeriode, idTypePeriode, ordre, showScores,
                    showSkills, event -> {
                        if (event.isLeft()) {
                            leftToResponse(request, event.left());
                            return;
                        }
                        JsonObject templateProps = event.right().getValue()
                                .put(Field.SHOWSKILLS, showSkills)
                                .put(Field.SHOWSCORES, showScores);
                        String templateName = "releve-classe.pdf.xhtml";
                        String prefixPdfName = "releve-classe_" + classeName;
                        exportService.genererPdf(request, templateProps, templateName, prefixPdfName, vertx, config);
                    });
        });
    }

    /**
     * Genere le BFC des entites passees en parametre au format PDF via la fonction
     * Ces entites peuvent etre au choix un etablissement, un ou plusieurs classes, un ou plusieurs eleves.
     * Afin de prefixer le fichier PDF cree, appelle {@link DefaultUtilsService#/getNameEntity(String[], Handler)} afin
     * de recuperer le nom de l'entite fournie.
     *
     * @param request
     */
    @Get("/BFC/pdf")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessChildrenParentFilter.class)
    public void getBFCEleve(final HttpServerRequest request) {
        final String idStructure = request.params().get("idStructure");
        final List<String> idClasses = request.params().getAll("idClasse");
        final List<String> idEleves = request.params().getAll(ID_ELEVE_KEY);
        final Long idCycle = Utils.isCycleNotNull(request.params().get("idCycle")) ?
                Long.valueOf(request.params().get("idCycle")) : null;
        final Long idPeriode = isNull(request.params().get(ID_PERIODE_KEY)) ? null :
                Long.valueOf(request.params().get(ID_PERIODE_KEY));
        // paramètre pour l'export des élèves
        final String idEtablissement = isNull(idStructure) ? request.params().get(ID_ETABLISSEMENT_KEY) : idStructure;

        // Ou exclusif sur la presence des parametres, de facon a s'assurer qu'un seul soit renseigne.
        if (idStructure != null ^ !idClasses.isEmpty() ^ !idEleves.isEmpty()) {
            Promise<JsonObject> exportResultPromise = Promise.promise();
            Promise<String> periodeNamePromise = Promise.promise();
            bfcService.generateBFCExport(idPeriode, idEtablissement, new JsonArray(idClasses), new JsonArray(idEleves),
                    idCycle, getHost(request), I18n.acceptLanguage(request), vertx, config, exportResultPromise,
                    periodeNamePromise);
            Future.all(exportResultPromise.future(), periodeNamePromise.future()).onComplete(event -> {
                if (event.failed()) {
                    leftToResponse(request, new Either.Left<>(event.cause().getMessage()));
                    return;
                }

                JsonObject result = exportResultPromise.future().result();
                String periodeName = periodeNamePromise.future().result();
                String fileNamePrefix = result.getString(NAME);
                String prefixPdfName = "BFC_" + fileNamePrefix + periodeName;
                String templateName = "BFC.pdf.xhtml";
                exportService.genererPdf(request, result, templateName, prefixPdfName, vertx, config);
                //appel le worker
                JsonObject jsonRequest = new JsonObject()
                        .put("headers", new JsonObject()
                                .put("Accept-Language", request.headers().get("Accept-Language")))
                        .put("Host", getHost(request));

                List<String> idClassesCheckCurrentCycle = new ArrayList<>();
                if (!idClasses.isEmpty()) {
                    idClassesCheckCurrentCycle = idClasses;
                } else {
                    idClassesCheckCurrentCycle.add(result.getJsonArray("classes").getJsonObject(0)
                            .getJsonArray("eleves").getJsonObject(0).getString("idClasse"));
                }
                utilsService.getCycle(idClassesCheckCurrentCycle, cycleEvent -> {
                    Long currentIdCycle = null;

                    if (cycleEvent.isRight()) {
                        JsonArray queryResult = cycleEvent.right().getValue();
                        for (int i = 0; i < queryResult.size(); i++) {
                            JsonObject cycle = queryResult.getJsonObject(i);
                            Long cycleId = cycle.getLong("id_cycle");
                            if (currentIdCycle == null) {
                                currentIdCycle = cycleId;
                            } else if (!currentIdCycle.equals(cycleId)) {
                                log.error("getBFCEleve : cycles are not sames");
                            }
                        }

                        if (idCycle != null && idCycle.equals(currentIdCycle)) {
                            saveBfcWorker(idClasses, idCycle, result, prefixPdfName, templateName, jsonRequest);
                        }
                    } else {
                        String error = cycleEvent.left().getValue();
                        log.error("getBFCEleve : getCycle : " + error);
                    }
                });
            });
        } else {
            leftToResponse(request, new Either.Left<>("Un seul parametre autre que la periode doit être specifie."));
            log.error("getBFCEleve : call with more than 1 parameter type (among idEleve, idClasse and idStructure).");
        }
    }


    private void saveBfcWorker(List<String> idClasses, Long idCycle, JsonObject result, String prefixPdfName,
                               String templateName, JsonObject jsonRequest) {
        if (idCycle != null) {
            result.put("idCycle", idCycle);
            if (idClasses.size() > 0) {
                JsonObject actionClass = new JsonObject()
                        .put(ACTION, "classe.getClasseInfo")
                        .put(ID_CLASSE_KEY, idClasses.get(0));

                eb.request(Competences.VIESCO_BUS_ADDRESS, actionClass, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();
                    if (body.getJsonObject("result").getJsonObject("c").
                            getJsonObject("metadata").getJsonArray("labels").contains("Class")) {
                        result.put("idClasse", idClasses.get(0));

                        setFuturesToInsertMongo(result, prefixPdfName, templateName, jsonRequest);
                    }
                }));
            } else {
                setFuturesToInsertMongo(result, prefixPdfName, templateName, jsonRequest);
            }

        }
    }

    private void setFuturesToInsertMongo(JsonObject result, String prefixPdfName, String templateName, JsonObject jsonRequest) {
        try {
            JsonArray students = result.getJsonArray("classes").getJsonObject(0).getJsonArray("eleves");
            List<Future<String>> futureArray = mongoExportService.insertDataInMongo(students, result, jsonRequest, prefixPdfName, templateName, Field.SAVE_BFC);
            FutureHelper.all(futureArray).onSuccess(success -> {
                log.info(String.format("[Competences@%s::setFuturesToInsertMongo] insert BFC data in Mongo done.", this.getClass().getSimpleName()));
                eb.send(BulletinWorker.class.getSimpleName(), new JsonObject(), Competences.DELIVERY_OPTIONS);
            }).onFailure(error -> {
                log.info(String.format("[Competences@%s::setFuturesToInsertMongo] an error has occurred during insert data in mongo: %s.", this.getClass().getSimpleName(), error.getMessage()));
            });
        } catch (Exception e) {
            log.error(String.format("[Competnces@%s::setFuturesToInsertMongo] %s ", this.getClass().getSimpleName(), e.getMessage()));
        }
    }

    @Get("/devoirs/print/:idDevoir/formsaisie")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessEvaluationFilter.class)
    public void getFormsaisi(final HttpServerRequest request) {
        MultiMap params = request.params();
        final Long idDevoir;
        if (params.get("idDevoir") != null) {
            try {
                idDevoir = Long.parseLong(params.get("idDevoir"));
            } catch (NumberFormatException e) {
                log.error("Error : idDevoir must be a long object", e);
                badRequest(request, e.getMessage());
                return;
            }

            String acceptLanguage = request.headers().get("Accept-Language");
            String host = getHost(request);

            devoirService.getFormSaisieDevoir(idDevoir, acceptLanguage, host, event -> {
                if (event.isLeft()) {
                    badRequest(request, event.left().getValue());
                    return;
                }

                exportService.genererPdf(request, event.right().getValue(), "Devoir.saisie.xhtml",
                        "Formulaire_saisie", vertx, config);
            });
        } else {
            log.error("Error : idDevoir must be a long object");
            badRequest(request, "Error : idDevoir must be a long object");
        }
    }

    @Get("/devoirs/print/:idDevoir/cartouche")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessEvaluationFilter.class)
    public void getCartouche(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                MultiMap params = request.params();
                final boolean json = Boolean.parseBoolean(request.params().get("json"));

                exportService.getExportCartouche(params, event -> {
                    if (event.isRight()) {
                        JsonObject result = event.right().getValue();

                        if (json) {
                            Renders.renderJson(request, result);
                        } else {
                            exportService.genererPdf(request, result, "cartouche.pdf.xhtml",
                                    "Cartouche", vertx, config);
                        }
                    } else {
                        leftToResponse(request, event.left());
                    }
                });
            } else {
                unauthorized(request);
            }
        });
    }

    @Get("/devoirs/print/:idDevoir/export")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessEvaluationFilter.class)
    public void getExportDevoir(final HttpServerRequest request) {
        Long idDevoir = 0L;
        final Boolean text = Boolean.parseBoolean(request.params().get("text"));
        final Boolean json = Boolean.parseBoolean(request.params().get("json"));
        final Boolean usePerso = Boolean.parseBoolean(request.params().get("usePerso"));

        try {
            idDevoir = Long.parseLong(request.params().get("idDevoir"));
        } catch (NumberFormatException err) {
            badRequest(request, err.getMessage());
            log.error(err);
        }

        devoirService.getDevoirInfo(idDevoir, getDevoirInfoEither -> {
            if (getDevoirInfoEither.isRight()) {
                JsonObject devoir = getDevoirInfoEither.right().getValue();
                final Boolean only_evaluation = devoir.getLong("nbrcompetence").equals(0L);
                String idGroupe = devoir.getString("id_groupe");
                String idEtablissement = devoir.getString("id_etablissement");

                exportService.getExportEval(text, usePerso, only_evaluation, devoir, idGroupe, idEtablissement,
                        request, event -> {
                            if (event.isRight()) {
                                try {
                                    JsonObject result = event.right().getValue();
                                    result.put("notOnlyEvaluation", !only_evaluation);
                                    if (json) {
                                        Renders.renderJson(request, result);
                                    } else {
                                        String fileName = result.getJsonObject("devoir")
                                                .getString("classe") + "_" +
                                                result.getJsonObject("devoir").getString("nom")
                                                        .replace(' ', '_');
                                        exportService.genererPdf(request, result,
                                                "evaluation.pdf.xhtml", fileName, vertx, config);
                                    }
                                } catch (Error err) {
                                    String error = "An error occured while rendering pdf export : " +
                                            err.getMessage();
                                    log.error(error);
                                    leftToResponse(request, new Either.Left<>(error));
                                }
                            } else {
                                leftToResponse(request, event.left());
                            }
                        });
            } else {
                leftToResponse(request, getDevoirInfoEither.left());
            }
        });
    }

    @Get("/releveComp/print/export")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessChildrenParentFilter.class)
    public void getExportReleveComp(final HttpServerRequest request) {
        final Boolean text = Boolean.parseBoolean(request.params().get("text"));
        final Boolean usePerso = Boolean.parseBoolean(request.params().get("usePerso"));
        final Boolean byEnseignement = Boolean.parseBoolean(request.params().get("byEnseignement"));
        final Boolean json = Boolean.parseBoolean(request.params().get("json"));
        final Boolean isCycle = Boolean.parseBoolean(request.params().get("isCycle"));
        final Long idCycle;
        if (request.params().contains("idCycle")) {
            idCycle = Long.parseLong(request.params().get("idCycle"));
        } else {
            idCycle = null;
        }
        final List<String> listIdMatieres = request.params().getAll("idMatiere");
        final String idStructure = request.params().get(Competences.ID_ETABLISSEMENT_KEY);

        final JsonArray idMatieres = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < listIdMatieres.size(); i++) {
            idMatieres.add(listIdMatieres.get(i));
        }

        Long idPeriode = null;
        String idClasse = null;
        String idEleve = null;

        try {
            if (request.params().contains("idPeriode")) {
                idPeriode = Long.parseLong(request.params().get("idPeriode"));
            }
            if (request.params().contains("idClasse")) {
                idClasse = request.params().get("idClasse");
            }
            if (request.params().contains("idEleve")) {
                idEleve = request.params().get("idEleve");
            }
        } catch (NumberFormatException err) {
            badRequest(request, err.getMessage());
            log.error(err);
            return;
        }

        final Long finalIdPeriode = idPeriode;
        final String finalIdClasse = idClasse;
        final String finalIdEleve = idEleve;

        final List<String> idGroupes = new ArrayList<>();
        final Map<String, String> nomGroupes = new LinkedHashMap<>();
        final List<String> idEtablissement = new ArrayList<>();

        // Récupération des matières
        Promise<String> matieresPromise = Promise.promise();
        exportService.getMatiereExportReleveComp(idMatieres, event -> formate(matieresPromise, event));

        // Récupération du libelle des périodes
        Promise<String> periodePromise = Promise.promise();
        exportService.getLibellePeriodeExportReleveComp(request, finalIdPeriode, isCycle, idCycle, event ->
                formate(periodePromise, event));

        // Récupération des élèves
        Promise<Object> elevesPromise = Promise.promise();
        final Map<String, String> elevesMap = new LinkedHashMap<>();
        exportService.getElevesExportReleveComp(finalIdClasse, idStructure, finalIdEleve, finalIdPeriode,
                elevesMap, event -> formate(elevesPromise, event));

        // Une fois la récupération effectuée, lancement de l'export
        Future.all(matieresPromise.future(), periodePromise.future(), elevesPromise.future()).onComplete(event -> {
            if (event.failed()) {
                String error = event.cause().getMessage();
                log.error(error);
                leftToResponse(request, new Either.Left<>(error));
                return;
            }
            final String matieres = matieresPromise.future().result();
            final String libellePeriode = periodePromise.future().result();

            if (finalIdClasse == null) {
                JsonObject eleve = (JsonObject) elevesPromise.future().result();
                final String nomClasse = eleve.getString("classeName");
                final String idEtablissementEl = eleve.getString(ID_ETABLISSEMENT_KEY);
                final int eleveLevel = getEleveLevel(eleve);
                JsonArray idManualGroupes = strIdGroupesToJsonArray(eleve.getValue("idManualGroupes"));
                JsonArray idFunctionalGroupes = strIdGroupesToJsonArray(eleve.getValue("idGroupes"));

                JsonArray _idGroupes = utilsService.saUnion(idFunctionalGroupes, idManualGroupes);
                String[] _iGroupesdArr = UtilsConvert.jsonArrayToStringArr(_idGroupes);

                final String[] idEleves = new String[1];
                idEleves[0] = finalIdEleve;
                idGroupes.add(eleve.getString(ID_CLASSE_KEY));
                nomGroupes.put(eleve.getString(ID_ELEVE_KEY), nomClasse);
                elevesMap.put(finalIdEleve, eleve.getString("lastName") + " " + eleve.getString("firstName"));
                final AtomicBoolean answered = new AtomicBoolean();
                JsonArray resultFinal = new fr.wseduc.webutils.collections.JsonArray();
                final Handler<Either<String, JsonObject>> finalHandler = getReleveCompetences(request, elevesMap,
                        nomGroupes, matieres, libellePeriode, json, answered, resultFinal);
                exportService.getExportReleveComp(text, usePerso, byEnseignement, idEleves[0], eleveLevel, idGroupes.toArray(new String[0]),
                        _iGroupesdArr, idEtablissementEl, listIdMatieres, finalIdPeriode, isCycle, idCycle, finalHandler);
            } else {
                JsonArray eleves = (JsonArray) elevesPromise.future().result();
                if (eleves.size() != elevesMap.size()) {
                    leftToResponse(request, new Either.Left<>("one or more students are in several classes"));
                } else {
                    final AtomicBoolean answered = new AtomicBoolean();
                    JsonArray resultFinal = new fr.wseduc.webutils.collections.JsonArray();

                    final Handler<Either<String, JsonObject>> finalHandler = getReleveCompetences(request, elevesMap,
                            nomGroupes, matieres, libellePeriode, json, answered, resultFinal);

                    for (int i = 0; i < eleves.size(); i++) {
                        JsonObject eleve = eleves.getJsonObject(i);
                        String idEleveEl = eleve.getString(ID_ELEVE_KEY);
                        String idEtablissementEl = eleve.getString(ID_ETABLISSEMENT_KEY);
                        int eleveLevel = getEleveLevel(eleve);
                        idEtablissement.add(idEtablissementEl);
                        idGroupes.add(eleve.getString(ID_CLASSE_KEY));
                        final String nomClasse = eleve.getString("classeName");
                        nomGroupes.put(idEleveEl, nomClasse);
                        String[] _idGroupes = new String[1];
                        _idGroupes[0] = idGroupes.get(i);
                        JsonArray idManualGroupes = strIdGroupesToJsonArray(eleve.getValue("idManualGroupes"));
                        JsonArray idFunctionalGroupes = strIdGroupesToJsonArray(eleve.getValue("idGroupes"));
                        JsonArray idGroupesJsArr = utilsService.saUnion(idFunctionalGroupes, idManualGroupes);
                        String[] idGroupesArr = UtilsConvert.jsonArrayToStringArr(idGroupesJsArr);
                        exportService.getExportReleveComp(text, usePerso, byEnseignement, idEleveEl, eleveLevel, _idGroupes, idGroupesArr,
                                idEtablissement.get(i), listIdMatieres, finalIdPeriode, isCycle, idCycle, finalHandler);
                    }
                }
            }
        });
    }

    private int getEleveLevel(JsonObject eleve) {
        int eleveLevel = -1;
        String eleveLevelString = eleve.getString("level");
        if (eleveLevelString != null) {
            eleveLevel = Integer.parseInt(eleveLevelString.split(" ")[0].replaceAll("[^\\d.]", ""));
        } else {
            eleveLevelString = eleve.getString("classeName");
            eleveLevel = Integer.parseInt(eleveLevelString.substring(0, 1));
        }
        return eleveLevel;
    }

    @Get("/recapAppreciations/print/:idClasse/export")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessSuiviClasse.class)
    public void getExportRecapAppreciations(final HttpServerRequest request) {
        final String idClasse = request.params().get("idClasse");
        final String idEtablissement = request.params().get("idStructure");
        final Boolean json = Boolean.parseBoolean(request.params().get("json"));

        Integer idPeriode = null;

        try {
            if (request.params().contains("idPeriode")) {
                idPeriode = Integer.parseInt(request.params().get("idPeriode"));
            }
        } catch (NumberFormatException err) {
            badRequest(request, err.getMessage());
            log.error(err);
            return;
        }

        final Integer finalIdPeriode = idPeriode;


         classAppreciationService.getTeacherAppreciationPDF(idClasse, finalIdPeriode,
                     request.headers().get(Field.ACCEPT_LANGUAGE), getHost(request), idEtablissement, request)
                     .onSuccess(result -> {
                         if (json) {
                             Renders.renderJson(request, result);
                         } else {
                             String fileName = result.getString("classe") + "_export_synthese";
                             exportService.genererPdf(request, result,
                                     "export_syntheses-classe.pdf.xhtml", fileName, vertx, config);
                         }
                     })
                     .onFailure(fail -> {
                         String message = String.format("[Competences@%s::getExportRecapAppreciations] Failed to get pdf class appreciation : %s",
                                 this.getClass().getSimpleName(), fail.getMessage());
                         log.error(message);
                         renderError(request);
                     });
}

    @Post("/export/bulletins")
    @SecuredAction(value = "export.bulletins.periodique", type = ActionType.WORKFLOW)
    public void exportBulletins(final HttpServerRequest request) {
    }

    @Get("/recapEval/print/:idClasse/export")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessSuiviClasse.class)
    public void getExportRecapEval(final HttpServerRequest request) {
        final Boolean text = Boolean.parseBoolean(request.params().get("text"));
        final Boolean json = Boolean.parseBoolean(request.params().get("json"));
        final String idClasse = request.params().get("idClasse");
        final Boolean usePerso = Boolean.parseBoolean(request.params().get("usePerso"));
        boolean year = true;
        Long idPeriode = null;
        try {
            if (request.params().contains("idPeriode")) {
                idPeriode = Long.parseLong(request.params().get("idPeriode"));
                year = false;
            }
        } catch (NumberFormatException err) {
            badRequest(request, err.getMessage());
            log.error(err);
            return;
        }

        final Long finalIdPeriode = idPeriode;
        final Boolean finalYear = year;
        List<String> idClasses = Collections.singletonList(idClasse);

        utilsService.getCycle(idClasses, cycleEither -> {
            if (cycleEither.isRight()) {
                JsonArray cycles = cycleEither.right().getValue();
                final Long idCycle = cycles.getJsonObject(0).getLong("id_cycle");

                final JsonObject action = new JsonObject()
                        .put("action", "classe.getEtabClasses")
                        .put("idClasses", new JsonArray(idClasses));

                eb.request(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();
                    if ("ok" .equals(body.getString("status"))) {
                        final String idEtablissement = body.getJsonArray("results")
                                .getJsonObject(0).getString("idStructure");
                        UserUtils.getUserInfos(eb, request, user -> {
                            final boolean isChefEtab;
                            if (user != null) {
                                isChefEtab = WorkflowActionUtils.hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());
                            } else {
                                isChefEtab = false;
                            }
                            WorkflowActionUtils.hasHeadTeacherRight(user, new JsonArray().add(idClasse),
                                    null, null, null, null, null, event -> {
                                        Boolean isHeadTeacher = false;
                                        if (event.isRight()) {
                                            isHeadTeacher = event.right().getValue();
                                        }
                                        getExportRecapUtils(user, idEtablissement, idCycle, text, json, usePerso,
                                                idClasse, finalIdPeriode, request, isChefEtab || isHeadTeacher,
                                                finalYear);
                                    });
                        });
                    } else {
                        leftToResponse(request, new Either.Left<>("etab not found"));
                    }
                }));
            } else {
                leftToResponse(request, cycleEither.left());
            }
        });
    }

    private void getExportRecapUtils(final UserInfos user, final String idEtablissement, final Long idCycle,
                                     final Boolean text, final Boolean json, final Boolean usePerso,
                                     final String idClasse, final Long idPeriode, final HttpServerRequest request,
                                     final Boolean isChefEtab, final Boolean isYear) {
        if (user != null || isChefEtab) {
            bfcService.getVisibility(idEtablissement, 1, user, visibilityEither -> {
                if (visibilityEither.isRight() || isChefEtab) {
                    boolean moy = false;
                    if (visibilityEither.isRight()) {
                        JsonArray result = visibilityEither.right().getValue();
                        Integer state = result.getJsonObject(0).getInteger("visible");
                        if ((isChefEtab && Competences.BFC_AVERAGE_VISIBILITY_FOR_ADMIN_ONLY.equals(state)) ||
                                Competences.BFC_AVERAGE_VISIBILITY_FOR_ALL.equals(state)) {
                            moy = true;
                        }
                    }
                    final boolean isHabilite = moy;

                    exportService.getLegendeRecapEval(text, usePerso, idCycle, idEtablissement, exportRecapEither -> {
                        if (exportRecapEither.isRight()) {
                            try {
                                final JsonObject result = new JsonObject();

                                final JsonArray legende = exportRecapEither.right().getValue();
                                result.put("legende", legende);
                                String atteint_calcule = new String(("Valeurs affichées par domaine : niveau atteint " +
                                        "+ niveau calculé").getBytes(), StandardCharsets.UTF_8);
                                String atteint = new String("Valeurs affichées par domaine : niveau atteint"
                                        .getBytes(), StandardCharsets.UTF_8);
                                result.put("displayMoy", isHabilite ? atteint_calcule : atteint);

                                final JsonObject action = new JsonObject()
                                        .put("action", "classe.getEleveClasse")
                                        .put("idClasse", idClasse)
                                        .put("idPeriode", idPeriode);

                                eb.request(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
                                    JsonObject body = message.body();

                                    if ("ok" .equals(body.getString("status"))) {
                                        JsonArray results = body.getJsonArray("results");
                                        results = Utils.sortElevesByDisplayName(results);

                                        final String[] idEleves = new String[results.size()];
                                        final String[] nameEleves = new String[results.size()];
                                        for (int i = 0; i < results.size(); i++) {
                                            JsonObject eleve = results.getJsonObject(i);
                                            idEleves[i] = eleve.getString("id");
                                            nameEleves[i] = eleve.getString("lastName") + " "
                                                    + eleve.getString("firstName");
                                        }

                                        buildBfcExportRecap(idEleves, nameEleves, idClasse, idEtablissement, idPeriode,
                                                idCycle, isYear, legende, isHabilite, text, json, request, result);
                                    } else {
                                        leftToResponse(request, new Either.Left<>("eleves not found"));
                                    }
                                }));
                            } catch (Error err) {
                                leftToResponse(request, new Either.Left<>("An error occured while rendering pdf export : " + err.getMessage()));
                            }
                        } else {
                            leftToResponse(request, exportRecapEither.left());
                        }
                    });
                } else {
                    leftToResponse(request, visibilityEither.left());
                }
            });
        } else {
            badRequest(request);
        }
    }

    private void buildBfcExportRecap(String[] idEleves, String[] nameEleves, String idClasse, String idEtablissement,
                                     Long idPeriode, Long idCycle, Boolean isYear, JsonArray legende, boolean isHabilite,
                                     Boolean text, Boolean json, HttpServerRequest request, JsonObject result) {
        bfcService.buildBFC(true, idEleves, idClasse, idEtablissement, idPeriode, idCycle, isYear, bfcEither -> {
            if (bfcEither.isRight()) {
                final JsonArray eleves = new JsonArray();
                JsonObject bfc = bfcEither.right().getValue();

                if (bfc.size() > 0) {
                    JsonArray domainesRacine = bfc.getJsonArray("domainesRacine");
                    final int[] idDomaines = new int[domainesRacine.size()];
                    for (int l = 0; l < domainesRacine.size(); l++) {
                        Long idDomaine = domainesRacine.getLong(l);
                        idDomaines[l] = idDomaine.intValue();
                    }

                    for (int i = 0; i < idEleves.length; i++) {
                        JsonObject eleve = new JsonObject();
                        JsonArray notesEleve = new JsonArray();
                        ArrayList<Integer> domainesEvalues = new ArrayList();
                        if (bfc.containsKey(idEleves[i])) {
                            for (Object resultNoteObject : bfc.getJsonArray(idEleves[i])) {
                                JsonObject resultNote = (JsonObject) resultNoteObject;
                                for (Object niveauObject : legende) {
                                    JsonObject niveau = (JsonObject) niveauObject;
                                    JsonObject note = new JsonObject();

                                    if (resultNote.getValue("niveau").toString()
                                            .equals(niveau.getLong("ordre").toString())) {
                                        note.put("id", resultNote.getInteger("idDomaine"));
                                        note.put("visu", niveau.getString("visu"));
                                        note.put("persoColor", niveau.getString("persoColor"));
                                        note.put("nonEvalue", false);

                                        DecimalFormat decimalFormat = new DecimalFormat("#.0");
                                        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);

                                        String moyCalcule = decimalFormat.format(resultNote.getDouble(MOYENNE).doubleValue());
                                        if (isHabilite)
                                            note.put(MOYENNE, text ? "- " + moyCalcule : "" + moyCalcule);

                                        domainesEvalues.add(note.getInteger("id"));
                                        notesEleve.add(note);
                                    }
                                }
                            }
                        }
                        addMaitriseNE(domainesEvalues, notesEleve, idDomaines, text);
                        eleve.put("id", idEleves[i]);
                        eleve.put("nom", nameEleves[i]);
                        eleve.put("notes", sortJsonArrayById(notesEleve));
                        eleves.add(eleve);
                    }

                    domaineService.getDomainesLibCod(idDomaines, domainesLibEither -> {
                        if (domainesLibEither.isRight()) {
                            JsonArray domaines = domainesLibEither.right().getValue();
                            result.put("domaines", isDomaineParent(sortJsonArrayById(domaines)));
                            result.put("eleves", eleves);

                            JsonObject action = new JsonObject()
                                    .put("action", "classe.getClasseInfo")
                                    .put("idClasse", idClasse);
                            eb.request(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
                                JsonObject body = message.body();

                                if ("ok" .equals(body.getString("status"))) {
                                    final String classeName = body.getJsonObject("result").getJsonObject("c")
                                            .getJsonObject("data").getString("name");
                                    result.put("classe", classeName);

                                    getLibellePeriodeAndGenerateExport(idPeriode, classeName, isHabilite, text, json,
                                            request, result);
                                } else {
                                    leftToResponse(request, new Either.Left<>("classe not found : " + body.getString("message")));
                                }
                            }));
                        } else {
                            leftToResponse(request, domainesLibEither.left());
                        }
                    });
                } else {
                    leftToResponse(request, new Either.Left<>("eval not found"));
                }
            } else {
                leftToResponse(request, new Either.Left<>("bfc not found"));
            }
        });
    }

    private void getLibellePeriodeAndGenerateExport(Long idPeriode, String classeName, boolean isHabilite, Boolean text,
                                                    Boolean json, HttpServerRequest request, JsonObject result) {
        JsonObject jsonRequest = new JsonObject()
                .put("headers", new JsonObject().put("Accept-Language", request.headers().get("Accept-Language")))
                .put("Host", getHost(request));

        JsonObject action = new JsonObject()
                .put("action", "periode.getLibellePeriode")
                .put("idType", idPeriode)
                .put("request", jsonRequest);
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if ("ok" .equals(body.getString("status"))) {
                String libellePeriode = body.getString("result")
                        .replace("é", "e")
                        .replace("è", "e");
                result.put("periode", libellePeriode);
                result.put("text", text);
                result.put("isHabilite", isHabilite);
                if (json) {
                    Renders.renderJson(request, result);
                } else {
                    String fileName = classeName.replace(' ', '_') + "_export_recapitulatif";
                    exportService.genererPdf(request, result, "recapitulatif-evaluations.pdf.xhtml",
                            fileName, vertx, config);
                }
            } else {
                leftToResponse(request, new Either.Left<>("periode not found : " + body.getString("message")));
            }
        }));
    }

    private Handler<Either<String, JsonObject>> getReleveCompetences(final HttpServerRequest request,
                                                                     final Map<String, String> elevesMap,
                                                                     final Map<String, String> nomGroupes,
                                                                     final String matieres,
                                                                     final String libellePeriode, final Boolean json,
                                                                     final AtomicBoolean answered,
                                                                     final JsonArray result) {

        final AtomicInteger elevesDone = new AtomicInteger();
        final AtomicInteger elevesAdd = new AtomicInteger();

        return new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(final Either<String, JsonObject> stringJsonArrayEither) {
                if (!answered.get()) {
                    if (stringJsonArrayEither.isRight()) {
                        try {
                            JsonObject res = stringJsonArrayEither.right().getValue();
                            Boolean noDevoir = res.getBoolean("noDevoir");
                            if (!noDevoir) {
                                final JsonObject headerEleve = new JsonObject();
                                final JsonObject _headerEleve = stringJsonArrayEither.right().getValue();
                                final String idEleve = stringJsonArrayEither.right().getValue()
                                        .getString("idEleve");
                                if (elevesMap.containsKey(idEleve)) {
                                    stringJsonArrayEither.right().getValue()
                                            .put("nom", elevesMap.get(idEleve));
                                    headerEleve.put("nom", elevesMap.get(idEleve));
                                }
                                headerEleve.put("classe", nomGroupes.get(idEleve));
                                headerEleve.put("matiere", matieres);
                                headerEleve.put("periode", libellePeriode);
                                JsonObject header = _headerEleve.getJsonObject("header");
                                if (header != null) {
                                    header.put("left", headerEleve);
                                }
                                result.add(_headerEleve);
                                elevesAdd.addAndGet(1);
                            }

                            if (elevesDone.addAndGet(1) == elevesMap.size()) {
                                answered.set(true);
                                JsonObject resultFinal = new JsonObject();
                                resultFinal.put("eleves", sortJsonArrayByName(result));
                                if (0 == result.size()) {
                                    leftToResponse(request,
                                            new Either.Left<>("getExportReleveComp : No exams " +
                                                    "on given period and/or material."));
                                } else if (json) {
                                    Renders.renderJson(request, result);
                                } else {
                                    final String idEleve = stringJsonArrayEither.right().getValue()
                                            .getString("idEleve");
                                    final String _nomGroupe = nomGroupes.get(idEleve);
                                    String fileName = elevesDone.get() == 1 ? elevesMap.get(idEleve)
                                            .replace(' ', '_') + "_export_competences"
                                            : _nomGroupe.replace(' ', '_') + "_export_competences";
                                    exportService.genererPdf(request, resultFinal,
                                            "releve-competences.pdf.xhtml", fileName, vertx, config);
                                }
                            }
                        } catch (Error err) {
                            leftToResponse(request,
                                    new Either.Left<>("An error occured while rendering pdf export : "
                                            + err.getMessage()));
                        }
                    } else {
                        leftToResponse(request, stringJsonArrayEither.left());
                    }
                } else {
                    answered.set(true);
                    leftToResponse(request, stringJsonArrayEither.left());
                }
            }
        };
    }

    private JsonArray sortJsonArrayById(JsonArray jsonArray) {
        List<JsonObject> jsonValues = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject p = jsonArray.getJsonObject(i);
            jsonValues.add(p);
        }
        Collections.sort(jsonValues, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "id";

            @Override
            public int compare(JsonObject a, JsonObject b) {
                Long valA = 0L;
                Long valB = 0L;
                try {
                    valA = (Long) a.getLong(KEY_NAME);
                    valB = (Long) b.getLong(KEY_NAME);
                } catch (Exception e) {
                    //do something
                }
                return valA.compareTo(valB);
            }
        });

        JsonArray sortedJsonArray = new fr.wseduc.webutils.collections.JsonArray();
        for (JsonObject o : jsonValues) {
            sortedJsonArray.add(o);
        }
        return sortedJsonArray;
    }

    private JsonArray sortJsonArrayByName(JsonArray jsonArray) {
        List<JsonObject> jsonValues = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject p = jsonArray.getJsonObject(i);
            if (!p.getBoolean("noDevoir")) {
                jsonValues.add(p);
            }
        }
        Collections.sort(jsonValues, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "nom";

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                try {
                    valA = a.getString(KEY_NAME);
                    valB = b.getString(KEY_NAME);
                } catch (Exception e) {
                    //do something
                }
                valA = Utils.removeAccent(valA);
                valB = Utils.removeAccent(valB);
                return valA.compareTo(valB);
            }
        });

        JsonArray sortedJsonArray = new fr.wseduc.webutils.collections.JsonArray();
        for (JsonObject o : jsonValues) {
            sortedJsonArray.add(o);
        }
        return sortedJsonArray;
    }

    private JsonArray isDomaineParent(JsonArray domaines) {
        JsonArray newDomaines = new fr.wseduc.webutils.collections.JsonArray();
        for (int k = 0; k < domaines.size(); k++) {
            JsonObject domaine = domaines.getJsonObject(k);
            if ("0" .equals(domaine.getInteger("id_parent").toString()) || k == 0) {
                domaine.put("isDomaineParent", true);
//                if(!"0".equals(domaine.getNumber("id_parent").toString()))
//                    domaine.put("nomDomaine", "Domaine D" + domaine.getNumber("id_parent").toString());
//                else
//                    domaine.put("nomDomaine", "Domaine " + domaine.getString("codification"));

            } else {
                domaine.put("isDomaineParent", false);
            }
            newDomaines.add(domaine);
        }
        return newDomaines;
    }

    private void addMaitriseNE(List domainesEvalues, JsonArray notesEleve, int[] idDomaines, boolean text) {
        for (int idDomaine : idDomaines) {
            if (!domainesEvalues.contains(idDomaine)) {
                JsonObject note = new JsonObject();
                note.put("id", new Long(idDomaine));
                note.put("visu", text ? "NE" : "white");
                note.put("nonEvalue", true);
                notesEleve.add(note);
            }
        }
    }


    @Post("/see/bulletins")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessChildrenParentFilter.class)
    public void seeBulletins(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, params -> {
            Long idPeriode = params.getLong(ID_PERIODE_KEY);
            String idEleve = params.getString(ID_ELEVE_KEY);
            String idClasse = params.getString(ID_CLASSE_KEY);
            String idEtablissement = params.getString(ID_STRUCTURE_KEY);
            String idParent = params.getString(ID_PARENT_KEY);

            UserUtils.getUserInfos(eb, request, user -> {
                if (user != null && user.getType().equals("Student") || user.getType().equals("Relative")) {
                    utilsService.getPeriodesClasses(idEtablissement, new JsonArray().add(idClasse), idPeriode, eventvisibility -> {
                        if (eventvisibility.isRight()) {
                            Boolean visibility = eventvisibility.right().getValue().getJsonObject(0).getBoolean("publication_bulletin");
                            if (visibility) {
                                utilsService.getYearsAndPeriodes(idEtablissement, true, event -> {
                                    if (event.isRight()) {
                                        String idYear = event.right().getValue().getString("start_date").substring(0, 4);
                                        BulletinUtils.getBulletin(idEleve, idClasse, idPeriode, idEtablissement, idParent,
                                                idYear, storage, request);
                                    } else {
                                        log.info("[seeBulletins] : Can't get year " + event.left().getValue());
                                        leftToResponse(request, event.left());
                                    }
                                });
                            } else {
                                log.info("[ExportPDFController] :  No rights for visibility ");
                                forbidden(request, "No rights for visibility");
                            }
                        } else {
                            log.error("[ExportPDFController] :  No Periode classe " + idClasse + " " + eventvisibility.left().getValue());
                            badRequest(request);
                        }
                    });
                } else if (user != null && user.getType().equals("Teacher") || user.getType().equals("Personnel")) {
                    utilsService.getYearsAndPeriodes(idEtablissement, true, event -> {
                        if (event.isRight()) {
                            String idYear = event.right().getValue().getString("start_date").substring(0, 4);
                            BulletinUtils.getBulletin(idEleve, idClasse, idPeriode, idEtablissement, idParent,
                                    idYear, storage, request);
                        } else {
                            log.info("[ExportPDFController] :  No bulletin in Storage " + event.left().getValue());
                            leftToResponse(request, event.left());
                        }
                    });
                } else {
                    unauthorized(request);
                }
            });
        });
    }

    @Get("/suiviClasse/tableau/moyenne/:idClasse/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void exportBulletinMoyennneOnly(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, eventUser -> {
            if (eventUser == null) {
                unauthorized(request);
                return;
            }
            String idClasse = request.params().get(Field.IDCLASSE);
            String idEtablissement = request.params().get(Field.IDETABLISSEMENT);
            String name = request.params().get(Field.NAME);

            Boolean withMoyGeneraleByEleve = Boolean.valueOf(request.params().get(Field.WITHMOYGENERALEBYELEVE));
            Boolean withMoyMinMaxByMat = Boolean.valueOf(request.params().get(Field.WITHMOYMINMAXBYMAT));
            boolean isWithSummaries = Boolean.parseBoolean(request.params().get(Field.WITHAPPRECIATIONS));
            Boolean text = Boolean.parseBoolean(request.params().get(Field.TEXT));

            int typeGroup;
            Integer idPeriode = null;
            try {
                typeGroup = Integer.parseInt(request.params().get(Field.TYPEGROUPE));
                if (request.params().contains(Field.IDPERIODE) && request.params().get(Field.IDPERIODE) != null) {
                    idPeriode = Integer.parseInt(request.params().get(Field.IDPERIODE));
                }
            } catch (NumberFormatException err) {
                badRequest(request, err.getMessage());
                log.error(err);
                return;
            }

            Future<String> futureLabelPeriode = getLabelPeriod(request, idPeriode);
            Future<JsonObject> futureStudentsAverages = averageService.getStudentsAverageForExportPdf(idEtablissement,
                    idPeriode, idClasse, typeGroup, name, isWithSummaries, withMoyGeneraleByEleve,
                    getHost(request), request.headers().get(Field.ACCEPT_LANGUAGE));

            CompositeFuture.all(futureLabelPeriode, futureStudentsAverages)
                    .onFailure(err -> leftToResponse(request, new Either.Left<>(err.getMessage())))
                    .onSuccess(result -> {
                        JsonObject studentsAverages = futureStudentsAverages.result();
                        exportStudentsAverages(studentsAverages, withMoyGeneraleByEleve, withMoyMinMaxByMat,
                                text, request, setPrefixExportMoysOnResult(studentsAverages, futureLabelPeriode.result()));
                    });
        });
    }

    private Future<String> getLabelPeriod(HttpServerRequest request, Integer idPeriod) {
        Promise<String> promise = Promise.promise();
        if (idPeriod != null) Utils.getPeriodLibelle(eb, request.headers().get(Field.ACCEPT_LANGUAGE), getHost(request), idPeriod)
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
        else promise.complete(Field.ANNEE);

        return promise.future();
    }

    private String setPrefixExportMoysOnResult(JsonObject result, String periodLabel) {
        result.put(Field.PERIODE, periodLabel);
        String prefix = result.getJsonArray(Field.ELEVES).getJsonObject(0).getString(Field.NAMECLASSE);
        result.put(Field.NAMECLASS, prefix);
        return String.format("%s_%s", prefix, periodLabel).replace(" ", "_");
    }

    void exportStudentsAverages(JsonObject result, Boolean withMoyGeneraleByEleve, Boolean withMoyMinMaxByMat,
                                Boolean text, HttpServerRequest request, String prefix) {
        result.put("withMoyGeneraleByEleve", withMoyGeneraleByEleve);
        result.put("withMoyMinMaxByMat", withMoyMinMaxByMat);
        result.put("text", text);

        exportService.genererPdf(request,
                result,
                "recap_moys_eleves_par_matiere_classe.pdf.xhtml",
                prefix,
                vertx,
                config);

    }

}