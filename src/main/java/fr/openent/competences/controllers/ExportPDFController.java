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
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.*;
import fr.openent.competences.service.impl.*;
import fr.openent.competences.utils.UtilsConvert;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.text.DecimalFormat;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.utils.FormateFutureEvent.formate;
import static fr.openent.competences.utils.UtilsConvert.strIdGroupesToJsonArray;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static java.util.Objects.isNull;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class ExportPDFController extends ControllerHelper {
    private String assetsPath = "../..";
    private Map<String, String> skins = new HashMap<String, String>();
    protected static final Logger log = LoggerFactory.getLogger(ExportPDFController.class);
    private final static String  COEFFICIENT = "coefficient";

    /**
     * Déclaration des services
     */
    private DevoirService devoirService;
    private UtilsService utilsService;
    private BFCService bfcService;
    private DomainesService domaineService;
    private CompetenceNoteService competenceNoteService;
    private NoteService noteService;
    private CompetencesService competencesService;
    private NiveauDeMaitriseService niveauDeMaitriseService;
    private ExportService exportService;
    private BfcSyntheseService bfcSynthseService;
    private EleveEnseignementComplementService eleveEnseignementComplementService;
    private DispenseDomaineEleveService dispenseDomaineEleveService;
    private AppreciationService appreciationService;
    private ExportBulletinService exportBulletinService;
    private final Storage storage;

    public ExportPDFController(EventBus eb, EmailSender notification, Storage storage) {
        devoirService = new DefaultDevoirService(eb);
        utilsService = new DefaultUtilsService(eb);
        bfcService = new DefaultBFCService(eb, storage);
        domaineService = new DefaultDomaineService(Competences.COMPETENCES_SCHEMA, Competences.DOMAINES_TABLE);
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_NOTES_TABLE);
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE, eb);
        competencesService = new DefaultCompetencesService(eb);
        niveauDeMaitriseService = new DefaultNiveauDeMaitriseService();
        exportService = new DefaultExportService(eb, storage);
        exportBulletinService = new DefaultExportBulletinService(eb, storage);
        bfcSynthseService = new DefaultBfcSyntheseService(Competences.COMPETENCES_SCHEMA, Competences.BFC_SYNTHESE_TABLE, eb);
        eleveEnseignementComplementService = new DefaultEleveEnseignementComplementService(Competences.COMPETENCES_SCHEMA, Competences.ELEVE_ENSEIGNEMENT_COMPLEMENT);
        dispenseDomaineEleveService = new DefaultDispenseDomaineEleveService(Competences.COMPETENCES_SCHEMA,Competences.DISPENSE_DOMAINE_ELEVE);
        appreciationService = new DefaultAppreciationService(Competences.COMPETENCES_SCHEMA, Competences.APPRECIATIONS_TABLE);
        this.storage = storage;
    }



    /**
     * Genere le releve d'un eleve sous forme de PDF
     */
    @Get("/releve/pdf")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getReleveEleve(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                unauthorized(request);
                return;
            }
            // parametres de l'url
            final MultiMap params = request.params();

            final Long idPeriode;
            if (params.get("idPeriode") == null) {
                idPeriode = null;
            }
            else {
                try {
                    idPeriode = Long.parseLong(params.get("idPeriode"));
                } catch (NumberFormatException e) {
                    log.error("Error : idPeriode must be a long object", e);
                    badRequest(request, e.getMessage());
                    return;
                }
            }

            final String idEtablissement = params.get("idEtablissement");
            final String idUser = params.get("idUser");
            exportService.getDataForExportReleveEleve(idUser, idEtablissement, idPeriode, params,
                    event -> {
                        if(event.isLeft()){
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
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getReleve(final HttpServerRequest request){
        RequestUtils.bodyToJson(request,  params -> {
            final String idClasse = params.getString(ID_CLASSE_KEY);
            final Long idPeriode = params.getLong(ID_PERIODE_KEY);
            final String idEtablissement = params.getString(ID_STRUCTURE_KEY);
            final Long idTypePeriode = params.getLong("idTypePeriode");
            final Long ordre = params.getLong(ORDRE);
            final String classeName = params.getString(CLASSE_NAME_KEY);
            exportService.getDataForExportReleveClasse(idClasse, idEtablissement, idPeriode, idTypePeriode, ordre,
                    event -> {
                        if(event.isLeft()){
                            leftToResponse(request, event.left());
                            return;
                        }
                        JsonObject templateProps = event.right().getValue();
                        String templateName = "releve-classe.pdf.xhtml";
                        String prefixPdfName = "releve-classe_" +  classeName;
                        exportService.genererPdf(request, templateProps, templateName, prefixPdfName, vertx, config);
                    } );

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
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getBFCEleve(final HttpServerRequest request) {
        final String idStructure = request.params().get("idStructure");
        final List<String> idClasses = request.params().getAll("idClasse");
        final List<String> idEleves = request.params().getAll(ID_ELEVE_KEY);
        final Long idCycle = (request.params().get("idCycle") != null) ?
                Long.valueOf(request.params().get("idCycle")) : null;
        final Long idPeriode = isNull(request.params().get(ID_PERIODE_KEY)) ? null :
                Long.valueOf(request.params().get(ID_PERIODE_KEY));

        // paramètre pour l'export des élèves
        final String idEtablissement = isNull(idStructure) ? request.params().get(ID_ETABLISSEMENT_KEY) : idStructure;

        // Ou exclusif sur la presence des parametres, de facon a s'assurer qu'un seul soit renseigne.
        if (idStructure != null ^ !idClasses.isEmpty() ^ !idEleves.isEmpty()) {
            Future<JsonObject> exportResult = Future.future();
            Future<String> periodeNameFuture = Future.future();
            bfcService.generateBFCExport(idPeriode, idEtablissement, new JsonArray(idClasses), new JsonArray(idEleves),
                    idCycle, getHost(request), I18n.acceptLanguage(request), vertx, config, exportResult,
                    periodeNameFuture);
            CompositeFuture.all(exportResult, periodeNameFuture).setHandler(event -> {
                if(event.failed()){
                    leftToResponse(request, new Either.Left<>(event.cause().getMessage()));
                    return;
                }
                JsonObject result = exportResult.result();
                String periodeName = periodeNameFuture.result();
                String fileNamePrefix = result.getString(NAME);
                String prefixPdfName = "BFC_" + fileNamePrefix + periodeName;
                String templateName = "BFC.pdf.xhtml";
                exportService.genererPdf(request, result, templateName, prefixPdfName, vertx, config);
            });
        } else {
            leftToResponse(request, new Either.Left<>("Un seul parametre autre que la periode doit être specifie."));
            log.error("getBFCEleve : call with more than 1 parameter type (among idEleve, idClasse and idStructure).");
        }
    }



    @Get("/devoirs/print/:idDevoir/formsaisie")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
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
                if(event.isLeft()){
                    badRequest(request, event.left().getValue());
                    return;
                }

                exportService.genererPdf(request, event.right().getValue() ,"Devoir.saisie.xhtml",
                        "Formulaire_saisie", vertx, config);
            });

        } else {
            log.error("Error : idDevoir must be a long object");
            badRequest(request, "Error : idDevoir must be a long object");
        }
    }

    @Get("/devoirs/print/:idDevoir/cartouche")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getCartouche(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {

            if (user != null) {
                MultiMap params = request.params();
                final boolean json = Boolean.parseBoolean(request.params().get("json"));

                exportService.getExportCartouche(params,  event-> {
                    if(event.isRight()){
                        JsonObject result = event.right().getValue();

                        if (json) {
                            Renders.renderJson(request, result);
                        } else {
                            exportService.genererPdf(request, result,"cartouche.pdf.xhtml",
                                    "Cartouche",vertx, config);
                        }
                    }else{
                        leftToResponse(request, event.left());
                    }

                });

            } else {
                unauthorized(request);
            }

        });
    }

    @Get("/devoirs/print/:idDevoir/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
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
                        request,  event -> {
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
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExportReleveComp(final HttpServerRequest request) {
        final Boolean text = Boolean.parseBoolean(request.params().get("text"));
        final Boolean usePerso = Boolean.parseBoolean(request.params().get("usePerso"));
        final Boolean byEnseignement = Boolean.parseBoolean(request.params().get("byEnseignement"));
        final Boolean json = Boolean.parseBoolean(request.params().get("json"));
        final Boolean isCycle = Boolean.parseBoolean(request.params().get("isCycle"));
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
        Future<String> matieresFuture = Future.future();
        exportService.getMatiereExportReleveComp(idMatieres, event -> formate(matieresFuture, event));

        // Récupération du libelle des périodes
        Future<String> periodeFuture = Future.future();
        exportService.getLibellePeriodeExportReleveComp(request, finalIdPeriode, isCycle, event ->
                formate(periodeFuture, event));

        // Récupération des élèves
        Future<Object> elevesFuture = Future.future();
        final Map<String, String> elevesMap = new LinkedHashMap<>();
        exportService.getElevesExportReleveComp(finalIdClasse, idStructure, finalIdEleve, finalIdPeriode,
                elevesMap, event -> formate(elevesFuture, event));

        // Une fois la récupération effectuée, lancement de l'export
        CompositeFuture.all(matieresFuture, periodeFuture, elevesFuture).setHandler(event -> {
            if (event.failed()) {
                String error = event.cause().getMessage();
                log.error(error);
                leftToResponse(request, new Either.Left<>(error));
                return;
            }
            final String matieres = matieresFuture.result();
            final String libellePeriode = periodeFuture.result();

            if (finalIdClasse == null) {
                JsonObject eleve = (JsonObject) elevesFuture.result();
                final String nomClasse = eleve.getString("classeName");
                final String idEtablissementEl = eleve.getString(ID_ETABLISSEMENT_KEY);
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
                exportService.getExportReleveComp(text, usePerso, byEnseignement, idEleves[0], idGroupes.toArray(new String[0]),
                        _iGroupesdArr, idEtablissementEl, listIdMatieres, finalIdPeriode, isCycle, finalHandler);
            } else {
                JsonArray eleves = (JsonArray) elevesFuture.result();
                eleves = Utils.sortElevesByDisplayName(eleves);
                final AtomicBoolean answered = new AtomicBoolean();
                JsonArray resultFinal = new fr.wseduc.webutils.collections.JsonArray();
                final Handler<Either<String, JsonObject>> finalHandler = getReleveCompetences(request, elevesMap,
                        nomGroupes, matieres, libellePeriode, json, answered, resultFinal);
                for (int i = 0; i < eleves.size(); i++) {
                    JsonObject eleve = eleves.getJsonObject(i);
                    String idEleveEl = eleve.getString(ID_ELEVE_KEY);
                    String idEtablissementEl = eleve.getString(ID_ETABLISSEMENT_KEY);
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
                    exportService.getExportReleveComp(text, usePerso, byEnseignement, idEleveEl, _idGroupes, idGroupesArr,
                            idEtablissement.get(i), listIdMatieres, finalIdPeriode, isCycle, finalHandler);
                }
            }
        });

    }


    @Get("/recapAppreciations/print/:idClasse/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExportRecapAppreciations(final HttpServerRequest request) {
        final String idClasse = request.params().get("idClasse");
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

        Integer finalIdPeriode = idPeriode;

        Set<JsonObject> MatGrp = new HashSet<>();
        Map<JsonObject, String> teachers = new HashMap<>();

        Future<Map<JsonObject, String>> apprFuture = Future.future();
        Future<Map<JsonObject, Map<String, List<NoteDevoir>>>> notesFuture = Future.future();
        Future<Map<JsonObject, Map<String, NoteDevoir>>> moyennesFinalFuture = Future.future();

        Future<Map<String, String>> libMatFuture = Future.future();
        Future<Map<String, String>> libGrpFuture = Future.future();
        Future<Map<String, String>> libTeachFuture = Future.future();

        Future<Map<JsonObject, JsonObject>> moyObjectFuture = Future.future();

        JsonObject result = new JsonObject();

        Future<JsonArray> idGroupesFuture = Future.future();
        Utils.getGroupesClasse(eb, new JsonArray().add(idClasse), eventResultGroups -> {
            if (eventResultGroups.isRight()) {
                idGroupesFuture.complete(eventResultGroups.right().getValue());
            } else {
                idGroupesFuture.fail(eventResultGroups.left().getValue());
            }
        });

        Future<List<String>> idElevesFuture = Future.future();
        Utils.getIdElevesClassesGroupes(eb, idClasse, finalIdPeriode, 0, eventResultEleves -> {
            if (eventResultEleves.isRight()) {
                idElevesFuture.complete(eventResultEleves.right().getValue());
            } else {
                idElevesFuture.fail(eventResultEleves.left().getValue());
            }
        });

        CompositeFuture.all(idElevesFuture, idGroupesFuture).setHandler(event -> {
            if(event.succeeded()) {
                Set<String> idGroups = new HashSet<>(Collections.singleton(idClasse));
                idGroupesFuture.result().stream().forEach(line -> {
                    idGroups.add(((JsonObject) line).getString("id_classe"));
                    ((JsonObject) line).getJsonArray("id_groupes").getList().forEach(idGroup -> idGroups.add((String) idGroup));
                });

                appreciationService.getAppreciationClasse(idGroups.toArray(new String[0]), finalIdPeriode, null, resultAppr -> {
                    if (resultAppr.isRight()) {
                        Map<JsonObject, String> appr = new HashMap<>();

                        resultAppr.right().getValue().stream().forEach(line -> {
                            JsonObject lineObject = (JsonObject) line;

                            JsonObject key = new JsonObject();
                            key.put("id_matiere", lineObject.getString("id_matiere"));
                            key.put("id_groupe", lineObject.getString("id_classe"));

                            MatGrp.add(key);
                            appr.put(key, lineObject.getString("appreciation"));
                        });

                        apprFuture.complete(appr);
                    } else {
                        apprFuture.fail(resultAppr.left().getValue());
                    }
                });

                noteService.getNotesParElevesParDevoirs(idElevesFuture.result().toArray(new String[0]), idGroups.toArray(new String[0]), null, finalIdPeriode,
                        resultNotesEleves -> {
                            if (resultNotesEleves.isRight()) {
                                Map<JsonObject, Map<String, List<NoteDevoir>>> notes = new HashMap<>();

                                resultNotesEleves.right().getValue().stream().forEach(line -> {
                                    JsonObject lineObject = (JsonObject) line;

                                    JsonObject key = new JsonObject();
                                    key.put("id_matiere", lineObject.getString("id_matiere"));
                                    key.put("id_groupe", lineObject.getString("id_groupe"));

                                    MatGrp.add(key);

                                    if (!teachers.containsKey(key)) {
                                        teachers.put(key, lineObject.getString("owner"));
                                    }

                                    NoteDevoir note = new NoteDevoir(Double.parseDouble(lineObject.getString("valeur")),
                                            lineObject.getLong("diviseur").doubleValue(),
                                            lineObject.getBoolean("ramener_sur"),
                                            Double.parseDouble(lineObject.getString(COEFFICIENT)));

                                    if (!notes.containsKey(key)) {
                                        notes.put(key, new HashMap<>());
                                    }

                                    if (!notes.get(key).containsKey(lineObject.getString("id_eleve"))) {
                                        notes.get(key).put(lineObject.getString("id_eleve"), new ArrayList<>());
                                    }
                                    notes.get(key).get(lineObject.getString("id_eleve")).add(note);
                                });

                                notesFuture.complete(notes);

                            } else {
                                notesFuture.fail(resultNotesEleves.left().getValue());
                            }
                        });

                noteService.getMoyennesFinal(idElevesFuture.result().toArray(new String[0]), finalIdPeriode, null, idGroups.toArray(new String[0]), stringJsonArrayEither -> {
                    if (stringJsonArrayEither.isRight()) {
                        Map<JsonObject, Map<String, NoteDevoir>> moyFinal = new HashMap<>();

                        stringJsonArrayEither.right().getValue().stream().forEach(line -> {
                            JsonObject lineObject = (JsonObject) line;

                            JsonObject key = new JsonObject()
                                    .put("id_groupe", lineObject.getString("id_classe"))
                                    .put("id_matiere", lineObject.getString("id_matiere"));

                            MatGrp.add(key);

                            if (!moyFinal.containsKey(key)) {
                                moyFinal.put(key, new HashMap<>());
                            }

                            moyFinal.get(key).put(lineObject.getString("id_eleve"), new NoteDevoir(Double.parseDouble(lineObject.getString(MOYENNE)), false, new Double(1)));
                        });

                        moyennesFinalFuture.complete(moyFinal);
                    } else {
                        moyennesFinalFuture.fail(stringJsonArrayEither.left().getValue());
                    }
                });
            } else {
                apprFuture.fail(event.cause());
                notesFuture.fail(event.cause());
                moyennesFinalFuture.fail(event.cause());
            }
        });

        CompositeFuture.all(apprFuture, notesFuture, moyennesFinalFuture).setHandler(compositeFutureAsyncResult -> {
            if (compositeFutureAsyncResult.succeeded()) {
                Map<JsonObject, String> appr = compositeFutureAsyncResult.result().resultAt(0);
                Map<JsonObject, Map<String, List<NoteDevoir>>> notes = compositeFutureAsyncResult.result().resultAt(1);
                Map<JsonObject, Map<String, NoteDevoir>> moyennesFinales = compositeFutureAsyncResult.result().resultAt(2);

                Map<JsonObject, JsonObject> moyObjects = MatGrp.stream().collect(Collectors.toMap(val -> val, val -> new JsonObject()));

                MatGrp.stream().forEach(matGrp -> {
                    List<NoteDevoir> matGrpNotes = new ArrayList<>();

                    JsonObject moyObject = new JsonObject();

                    idElevesFuture.result().stream().forEach(idEleve -> {
                        if (moyennesFinales.containsKey(matGrp) && moyennesFinales.get(matGrp).containsKey(idEleve)) {
                            matGrpNotes.add(moyennesFinales.get(matGrp).get(idEleve));
                        } else if (notes.containsKey(matGrp) && notes.get(matGrp).containsKey(idEleve)) {
                            matGrpNotes.add(new NoteDevoir(utilsService.calculMoyenne(notes.get(matGrp).get(idEleve), false, null,false).getDouble(MOYENNE), false, new Double(1)));
                        }
                    });

                    JsonObject resultCalc = utilsService.calculMoyenne(matGrpNotes, true, null,false);
                    if (resultCalc.getDouble("noteMin") > resultCalc.getDouble(MOYENNE)) {
                        moyObject.put("min", "");
                        moyObject.put("max", "");
                        moyObject.put("moy", "");
                    } else {
                        moyObject.put("min", resultCalc.getDouble("noteMin"));
                        moyObject.put("max", resultCalc.getDouble("noteMax"));
                        moyObject.put("moy", resultCalc.getDouble(MOYENNE));
                    }
                    moyObject.put("appr", appr.get(matGrp));

                    moyObjects.put(matGrp, moyObject);
                });

                moyObjectFuture.complete(moyObjects);

                if (teachers.values().size() == 0) {
                    libTeachFuture.complete(new HashMap<>());
                } else {
                    Utils.getLastNameFirstNameUser(eb, new JsonArray(new ArrayList(teachers.values())), libTeachersEvent -> {
                        if (libTeachersEvent.isRight()) {
                            libTeachFuture.complete(libTeachersEvent.right().getValue().entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(val -> val.getKey(), val -> val.getValue().getString("firstName") + " " + val.getValue().getString("name"))));
                        } else {
                            log.error(libTeachersEvent.left().getValue());
                            libTeachFuture.fail(new Throwable(libTeachersEvent.left().getValue()));
                        }
                    });
                }

                if (MatGrp.size() == 0) {
                    libMatFuture.complete(new HashMap<>());
                } else {
                    Utils.getLibelleMatiere(eb, new JsonArray(MatGrp.stream().map(matGrp -> matGrp.getString("id_matiere")).collect(Collectors.toList())), libMatEvent -> {
                        if (libMatEvent.isRight()) {
                            libMatFuture.complete(libMatEvent.right().getValue().entrySet().stream().collect(Collectors.toMap(val -> val.getKey(), val -> val.getValue().getString("name"))));
                        } else {
                            log.error(libMatEvent.left().getValue());
                            libMatFuture.fail(new Throwable(libMatEvent.left().getValue()));
                        }
                    });
                }

                if (MatGrp.size() == 0) {
                    libGrpFuture.complete(new HashMap<>());
                } else {
                    Utils.getInfosGroupes(eb, new JsonArray(MatGrp.stream().map(matGrp -> matGrp.getString("id_groupe")).collect(Collectors.toList())), libGrpEvent -> {
                        if (libGrpEvent.isRight()) {
                            libGrpFuture.complete(libGrpEvent.right().getValue());
                        } else {
                            log.error(libGrpEvent.left().getValue());
                            libGrpFuture.fail(new Throwable(libGrpEvent.left().getValue()));
                        }
                    });
                }
            } else {
                moyObjectFuture.fail(compositeFutureAsyncResult.cause());
                libTeachFuture.fail(compositeFutureAsyncResult.cause());
                libMatFuture.fail(compositeFutureAsyncResult.cause());
                libGrpFuture.fail(compositeFutureAsyncResult.cause());
            }
        });

        Future<String> libellePeriodeFuture = Future.future();
        if(finalIdPeriode == null) {
            libellePeriodeFuture.complete("Année");
        } else {
            Utils.getLibellePeriode(eb, request, finalIdPeriode, stringStringEither -> {
                if (stringStringEither.isRight()) {
                    libellePeriodeFuture.complete(stringStringEither.right().getValue());
                } else {
                    libellePeriodeFuture.fail(stringStringEither.left().getValue());
                }
            });
        }

        Future<String> libelleClasseFuture = Future.future();
        Utils.getInfosGroupes(eb, new JsonArray().add(idClasse), stringMapEither -> {
            if (stringMapEither.isRight()) {
                libelleClasseFuture.complete(stringMapEither.right().getValue().get(idClasse));
            } else {
                libelleClasseFuture.fail(stringMapEither.left().getValue());
            }
        });

        CompositeFuture.all(libellePeriodeFuture, libelleClasseFuture, libTeachFuture, libGrpFuture, libMatFuture, moyObjectFuture).setHandler(allData -> {
            if (allData.succeeded()) {
                String libellePeriode = allData.result().resultAt(0);
                String libelleClasse = allData.result().resultAt(1);
                Map<String, String> libTeachers = allData.result().resultAt(2);
                Map<String, String> libGrp = allData.result().resultAt(3);
                Map<String, String> libMatieres = allData.result().resultAt(4);
                Map<JsonObject, JsonObject> moyObject = allData.result().resultAt(5);

                JsonArray data = new JsonArray(
                        moyObject.entrySet().stream().map(entry -> {
                            JsonObject newMoy = new JsonObject();
                            newMoy.put("mat", libMatieres.get(entry.getKey().getString("id_matiere")));
                            newMoy.put("prof", libTeachers.get(teachers.get(entry.getKey())));
                            newMoy.put("grp", libGrp.get(entry.getKey().getString("id_groupe")));
                            newMoy.mergeIn(entry.getValue());

                            return newMoy;
                        }).collect(Collectors.toList()));

                result.put("data", data);
                result.put("periode", libellePeriode);
                result.put("classe", libelleClasse);

                if(json) {
                    Renders.renderJson(request, result);
                } else {
                    String fileName = result.getString("classe") + "_export_synthese";
                    exportService.genererPdf(request, result,
                            "export_syntheses-classe.pdf.xhtml", fileName, vertx, config);
                }
            } else {
                leftToResponse(request, new Either.Left<>(allData.cause().getMessage()));
            }
        });
    }

    @Get("/recapEval/print/:idClasse/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExportRecapEval(final HttpServerRequest request) {
        final Boolean text = Boolean.parseBoolean(request.params().get("text"));
        final Boolean json = Boolean.parseBoolean(request.params().get("json"));
        final String idClasse = request.params().get("idClasse");
        final Boolean usePerso = Boolean.parseBoolean(request.params().get("usePerso"));

        Long idPeriode = null;

        try {
            if (request.params().contains("idPeriode")) {
                idPeriode = Long.parseLong(request.params().get("idPeriode"));
            }
        } catch (NumberFormatException err) {
            badRequest(request, err.getMessage());
            log.error(err);
            return;
        }

        final Long finalIdPeriode = idPeriode;

        utilsService.getCycle(Arrays.asList(idClasse), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isRight()) {
                    final Long idCycle = ((JsonObject) stringJsonArrayEither.right().getValue()
                            .getJsonObject(0)).getLong("id_cycle");
                    JsonObject cycleObj = stringJsonArrayEither.right().getValue().getJsonObject(0);

                    if (!idCycle.equals(cycleObj.getLong("id_cycle"))) {
                        leftToResponse(request, new Either.Left<>("different cycle"));
                    } else {
                        final JsonObject action = new JsonObject()
                                .put("action", "classe.getEtabClasses")
                                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(Arrays
                                        .asList(new String[]{idClasse})));

                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(
                                new Handler<Message<JsonObject>>() {
                                    @Override
                                    public void handle(Message<JsonObject> message) {
                                        JsonObject body = message.body();
                                        if ("ok".equals(body.getString("status"))) {
                                            final String idEtablissement = ((JsonObject) body.getJsonArray("results")
                                                    .getJsonObject(0)).getString("idStructure");
                                            UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                                                @Override
                                                public void handle(final UserInfos user) {
                                                    final boolean isChefEtab;
                                                    if(user != null) {
                                                        isChefEtab =  new WorkflowActionUtils().hasRight(user,
                                                                WorkflowActions.ADMIN_RIGHT.toString());
                                                    }
                                                    else {
                                                        isChefEtab = false;
                                                    }
                                                    WorkflowActionUtils.hasHeadTeacherRight(user,
                                                            new JsonArray().add(idClasse), null, null,
                                                            null, null, null,
                                                            new Handler<Either<String, Boolean>>() {
                                                                @Override
                                                                public void handle(Either<String, Boolean> event) {
                                                                    Boolean isHeadTeacher;
                                                                    if(event.isLeft()) {
                                                                        isHeadTeacher = false;
                                                                    }
                                                                    else {
                                                                        isHeadTeacher = event.right().getValue();
                                                                    }
                                                                    getExportRecapUtils(user, idEtablissement, idCycle,
                                                                            text, json, usePerso, idClasse,
                                                                            finalIdPeriode, request,
                                                                            isChefEtab || isHeadTeacher);
                                                                }
                                                            });
                                                }
                                            });
                                        } else {
                                            leftToResponse(request, new Either.Left<>("etab not found"));
                                        }
                                    }
                                }));
                    }
                } else {
                    leftToResponse(request, stringJsonArrayEither.left());
                }
            }
        });
    }

    private void getExportRecapUtils (final UserInfos user, final String idEtablissement, final Long idCycle,
                                      final Boolean text, final Boolean json, final Boolean usePerso,
                                      final String idClasse, final Long finalIdPeriode, final HttpServerRequest request,
                                      final Boolean isChefEtab) {

        if ((user != null) || isChefEtab) {
            //idVisibility = 1 pour la visibilité de la moyBFC
            bfcService.getVisibility(idEtablissement,1,
                    user, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                            if (stringJsonArrayEither.isRight() || isChefEtab) {
                                boolean moy = false;
                                Number state = null;
                                if (stringJsonArrayEither.isRight()) {
                                    JsonArray result = stringJsonArrayEither.right().getValue();
                                    JsonObject obj = (JsonObject) result.getJsonObject(0);
                                    state = (obj).getInteger("visible");
                                }
                                if (state != null
                                        && Competences.BFC_AVERAGE_VISIBILITY_NONE.equals(state)) {
                                    moy = false;
                                } else if (state != null && isChefEtab &&
                                        Competences.BFC_AVERAGE_VISIBILITY_FOR_ADMIN_ONLY
                                                .equals(state)) {
                                    moy = true;
                                } else if (state != null &&
                                        Competences.BFC_AVERAGE_VISIBILITY_FOR_ALL
                                                .equals(state)) {
                                    moy = true;
                                }
                                final boolean isHabilite = moy;

                                exportService.getExportRecapEval(text, usePerso, idCycle, idEtablissement,
                                        new Handler<Either<String, JsonArray>>() {

                                            @Override
                                            public void handle(final Either<String, JsonArray> stringJsonObjectEither) {
                                                if (stringJsonObjectEither.isRight()) {
                                                    try {
                                                        final JsonObject result = new JsonObject();

                                                        final JsonArray legende = stringJsonObjectEither.right().getValue();
                                                        result.put("legende", legende);
                                                        String atteint_calcule =
                                                                new String(("Valeurs affichées par domaine : niveau atteint " +
                                                                        "+ niveau calculé").getBytes(), StandardCharsets.UTF_8);
                                                        String atteint =
                                                                new String("Valeurs affichées par domaine : niveau atteint"
                                                                        .getBytes(), StandardCharsets.UTF_8);
                                                        result.put("displayMoy", isHabilite ? atteint_calcule : atteint);

                                                        final JsonObject action = new JsonObject()
                                                                .put("action", "classe.getEleveClasse")
                                                                .put("idClasse", idClasse)
                                                                .put("idPeriode", finalIdPeriode);

                                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(
                                                                new Handler<Message<JsonObject>>() {
                                                                    @Override
                                                                    public void handle(Message<JsonObject> message) {
                                                                        JsonObject body = message.body();

                                                                        if ("ok".equals(body.getString("status"))) {
                                                                            JsonArray eleves = body.getJsonArray("results");
                                                                            eleves = Utils.sortElevesByDisplayName(eleves);
                                                                            final String[] idEleves = new String[eleves.size()];
                                                                            final String[] nameEleves = new String[eleves.size()];
                                                                            for (int i = 0; i < eleves.size(); i++) {
                                                                                idEleves[i] = ((JsonObject) eleves.getJsonObject(i))
                                                                                        .getString("id");
                                                                                nameEleves[i] = ((JsonObject) eleves.getJsonObject(i))
                                                                                        .getString("lastName") + " "
                                                                                        + ((JsonObject) eleves.getJsonObject(i))
                                                                                        .getString("firstName");
                                                                            }
                                                                            boolean recapEval = true;
                                                                            bfcService.buildBFC(recapEval, idEleves, idClasse,
                                                                                    idEtablissement, finalIdPeriode, idCycle,
                                                                                    new Handler<Either<String, JsonObject>>() {
                                                                                        @Override
                                                                                        public void handle(Either<String, JsonObject> stringMapEither) {
                                                                                            if (stringMapEither.isRight()) {
                                                                                                final JsonArray eleves = new fr.wseduc.webutils.collections.JsonArray();
                                                                                                JsonObject bfc = stringMapEither.right().getValue();
                                                                                                if (bfc.size() > 0) {
                                                                                                    final int[] idDomaines = new int[bfc.getJsonArray("domainesRacine").size()];
                                                                                                    for (int l = 0; l < bfc.getJsonArray("domainesRacine").size(); l++) {
                                                                                                        Long idDomaine = bfc.getJsonArray("domainesRacine").getLong(l);
                                                                                                        idDomaines[l] = idDomaine.intValue();
                                                                                                    }

                                                                                                    for (int i = 0; i < idEleves.length; i++) {
                                                                                                        JsonObject eleve = new JsonObject();
                                                                                                        JsonArray notesEleve = new fr.wseduc.webutils.collections.JsonArray();
                                                                                                        List domainesEvalues = new ArrayList();
                                                                                                        if (bfc.containsKey(idEleves[i])) {
                                                                                                            for (Object resultNote : bfc.getJsonArray(idEleves[i])) {
                                                                                                                for (Object niveau : legende) {
                                                                                                                    JsonObject note = new JsonObject();

                                                                                                                    if (((JsonObject) resultNote).getValue("niveau").toString()
                                                                                                                            .equals(((JsonObject) niveau).getLong("ordre").toString())) {
                                                                                                                        note.put("id", ((JsonObject) resultNote).getInteger("idDomaine"));
                                                                                                                        note.put("visu", ((JsonObject) niveau).getString("visu"));
                                                                                                                        note.put("persoColor", ((JsonObject) niveau).getString("persoColor"));
                                                                                                                        note.put("nonEvalue", false);
                                                                                                                        String moyCalcule = new DecimalFormat("#0.00").format(((JsonObject) resultNote).getDouble(MOYENNE).doubleValue());
                                                                                                                        if (isHabilite)
                                                                                                                            note.put(MOYENNE, text ? "- " + moyCalcule
                                                                                                                                    : "" + moyCalcule);

                                                                                                                        domainesEvalues.add(((JsonObject) note).getInteger("id").intValue());
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

                                                                                                    domaineService.getDomainesLibCod(idDomaines, new Handler<Either<String, JsonArray>>() {
                                                                                                        @Override
                                                                                                        public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                                                                                                            if (stringJsonArrayEither.isRight()) {
                                                                                                                JsonArray domaines = stringJsonArrayEither.right().getValue();
                                                                                                                result.put("domaines", isDomaineParent(sortJsonArrayById(domaines)));
                                                                                                                result.put("eleves", eleves);
                                                                                                                JsonObject action = new JsonObject()
                                                                                                                        .put("action", "classe.getClasseInfo")
                                                                                                                        .put("idClasse", idClasse);
                                                                                                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                                                                                    @Override
                                                                                                                    public void handle(Message<JsonObject> message) {
                                                                                                                        JsonObject body = message.body();

                                                                                                                        if ("ok".equals(body.getString("status"))) {
                                                                                                                            final String classeName = body.getJsonObject("result").getJsonObject("c").getJsonObject("data").getString("name");
                                                                                                                            result.put("classe", classeName);

                                                                                                                            JsonObject jsonRequest = new JsonObject()
                                                                                                                                    .put("headers", new JsonObject().put("Accept-Language", request.headers().get("Accept-Language")))
                                                                                                                                    .put("Host", getHost(request));
                                                                                                                            JsonObject action = new JsonObject()
                                                                                                                                    .put("action", "periode.getLibellePeriode")
                                                                                                                                    .put("idType", finalIdPeriode)
                                                                                                                                    .put("request", jsonRequest);
                                                                                                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                                                                                                @Override
                                                                                                                                public void handle(Message<JsonObject> message) {
                                                                                                                                    JsonObject body = message.body();

                                                                                                                                    if ("ok".equals(body.getString("status"))) {
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
                                                                                                                                            exportService.genererPdf(request, result,
                                                                                                                                                    "recapitulatif-evaluations.pdf.xhtml", fileName, vertx, config);
                                                                                                                                        }
                                                                                                                                    } else {
                                                                                                                                        leftToResponse(request, new Either.Left<>("periode not found")); //leftToResponse(request, new Either.Left<>(body.getString("message")));
                                                                                                                                    }
                                                                                                                                }
                                                                                                                            }));
                                                                                                                        } else {
                                                                                                                            leftToResponse(request, new Either.Left<>("classe not found"));
                                                                                                                        }
                                                                                                                    }
                                                                                                                }));
                                                                                                            } else {
                                                                                                                leftToResponse(request, stringJsonArrayEither.left());
                                                                                                            }
                                                                                                        }
                                                                                                    });
                                                                                                } else {
                                                                                                    leftToResponse(request, new Either.Left<>("eval not found"));
                                                                                                }
                                                                                            } else {
                                                                                                leftToResponse(request, new Either.Left<>("bfc not found"));
                                                                                            }
                                                                                        }
                                                                                    });
                                                                        } else {
                                                                            leftToResponse(request, new Either.Left<>("eleves not found"));
                                                                        }
                                                                    }
                                                                }));
                                                    } catch (Error err) {
                                                        leftToResponse(request, new Either.Left<>("An error occured while rendering pdf export : " + err.getMessage()));
                                                    }
                                                } else {
                                                    leftToResponse(request, stringJsonObjectEither.left());
                                                }
                                            }
                                        });
                            } else {
                                leftToResponse(request, stringJsonArrayEither.left());
                            }
                        }
                    });
        } else {
            badRequest(request);
        }
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
                            Boolean noDevoir =  res.getBoolean("noDevoir");
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
                                if ( 0 == result.size()){
                                    leftToResponse(request,
                                            new Either.Left<>("getExportReleveComp : No exams " +
                                                    "on given period and/or material."));
                                }
                                else if (json) {
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
            if ("0".equals(domaine.getInteger("id_parent").toString()) || k == 0) {
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

    @Post("/export/bulletins")
    @SecuredAction(value = "export.bulletins.periodique", type = ActionType.WORKFLOW)
    public void exportBulletins(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, params -> {
            Long idPeriode = params.getLong(ID_PERIODE_KEY);
            JsonArray idStudents = params.getJsonArray(ID_STUDENTS_KEY);
            String idClasse = params.getString(ID_CLASSE_KEY);
            String idEtablissement = params.getString(ID_STRUCTURE_KEY);
            Future<JsonArray> elevesFuture = Future.future();
            final Map<String, JsonObject> elevesMap = new LinkedHashMap<>();
            final AtomicBoolean answered = new AtomicBoolean();

            final Handler<Either<String, JsonObject>> finalHandler = exportBulletinService
                    .getFinalBulletinHandler(request, elevesMap, vertx, config, elevesFuture, answered, params);

            exportBulletinService.runExportBulletin(idEtablissement, idClasse, idStudents, idPeriode, params,
                    elevesFuture, elevesMap, answered, getHost(request), I18n.acceptLanguage(request),
                    finalHandler, null);

        });
    }

    @Post("/see/bulletins")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void seeBulletins(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request,  params -> {
            Long idPeriode = params.getLong(ID_PERIODE_KEY);
            JsonArray idStudents = params.getJsonArray(ID_STUDENTS_KEY);
            String idClasse = params.getString(ID_CLASSE_KEY);
            String idEtablissement = params.getString(ID_STRUCTURE_KEY);
            Future<JsonArray> elevesFuture = Future.future();
            final Map<String, JsonObject> elevesMap = new LinkedHashMap<>();
            final AtomicBoolean answered = new AtomicBoolean();

            final Handler<Either<String, JsonObject>> finalHandler = exportBulletinService
                    .getFinalBulletinHandler(request, elevesMap, vertx, config, elevesFuture, answered, params);

            exportBulletinService.runExportBulletin(idEtablissement, idClasse, idStudents, idPeriode, params,
                    elevesFuture, elevesMap, answered, getHost(request), I18n.acceptLanguage(request),
                    finalHandler, null);

        });
    }

    @Get("/student/bulletin/parameters")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getBulletinParameters(final HttpServerRequest request) {
        Long idPeriode = Long.valueOf(request.params().get(ID_PERIODE_KEY));
        String idStudent = request.params().get(ID_ELEVE_KEY);
        exportBulletinService.getParameters(idStudent, idPeriode, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isLeft()){
                    leftToResponse(request, event.left());
                    log.error(event.left().getValue());
                } else{
                    JsonObject parameters = event.right().getValue();
                    if(!parameters.isEmpty()){
                        JsonObject paramsJson = new JsonObject(parameters.getString("params"));
                        JsonArray idStudents = new JsonArray().add(idStudent);
                        paramsJson.put(ID_PERIODE_KEY,idPeriode);
                        paramsJson.put(ID_STUDENTS_KEY,idStudents);
                        Renders.renderJson(request, paramsJson);
                    }else{
                        noContent(request);
                    }
                }
            }
        });
    }

    @Post("/save/bulletin/parameters")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void saveParameters(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                final boolean isChefEtab;
                if (user != null) {
                    isChefEtab = new WorkflowActionUtils().hasRight(user,
                            WorkflowActions.ADMIN_RIGHT.toString());
                } else {
                    isChefEtab = false;
                }
                RequestUtils.bodyToJson(request, params -> {
                    Long idPeriode = params.getLong(ID_PERIODE_KEY);
                    JsonArray idStudents = params.getJsonArray(ID_STUDENTS_KEY);
                    String idClasse = params.getString(ID_CLASSE_KEY);

                    WorkflowActionUtils.hasHeadTeacherRight(user,
                            new JsonArray().add(idClasse), null, null,
                            null, null, null,
                            new Handler<Either<String, Boolean>>() {
                                @Override
                                public void handle(Either<String, Boolean> event) {
                                    Boolean isHeadTeacher;
                                    if (event.isLeft()) {
                                        isHeadTeacher = false;
                                    } else {
                                        isHeadTeacher = event.right().getValue();
                                    }
                                    if (isHeadTeacher || isChefEtab)
                                        exportBulletinService.saveParameters(idStudents, idPeriode, params.toString(), defaultResponseHandler(request));
                                    else
                                        unauthorized(request);
                                }
                            });
                });

            }
        });
    }

    @Get("/suiviClasse/tableau/moyenne/:idClasse/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void exportBulletinMoyennneOnly(HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos eventUser) {
                if (eventUser == null) {
                    unauthorized(request);
                } else {
                    String idClasse = request.params().get("idClasse");

                    Boolean withMoyGeneraleByEleve = Boolean.valueOf(request.params().get("withMoyGeneraleByEleve"));
                    Boolean withMoyMinMaxByMat = Boolean.valueOf(request.params().get("withMoyMinMaxByMat"));
                    Boolean text = Boolean.parseBoolean(request.params().get("text"));

                    Integer idPeriode = null;
                    try {
                        if (request.params().contains("idPeriode") && request.params().get("idPeriode")!= null ) {
                            idPeriode = Integer.parseInt(request.params().get("idPeriode"));
                        }
                    } catch (NumberFormatException err) {
                        badRequest(request, err.getMessage());
                        log.error(err);
                        return;
                    }
                    final Integer idPeriodeFinal = idPeriode;

                    SortedMap<String, Set<String>> mapAllidMatAndidTeachers = new TreeMap<>();
                    Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve = new LinkedHashMap<>();
                    final JsonObject[] resultElevesTab = new JsonObject[1];

                    Handler<Either<String, JsonObject>> getMatEvaluatedAndStatHandler = event -> {
                        if(!event.isRight()) {
                            leftToResponse(request, event.left());
                        } else {
                            JsonObject resultMatieres = event.right().getValue();
                            JsonObject resultEleves = resultElevesTab[0];
                            resultEleves.getMap().putAll(resultMatieres.getMap());

                            JsonObject result = new JsonObject(resultEleves.getMap());

                            if(idPeriodeFinal != null) {
                                Utils.getLibellePeriode(eb, request, idPeriodeFinal, new Handler<Either<String, String>>() {
                                    @Override
                                    public void handle(Either<String, String> event) {
                                        if (!event.isRight()) {
                                            leftToResponse(request, event.left());
                                        } else {
                                            String libellePeriode = event.right().getValue();
                                            result.put("periode", libellePeriode);
                                            String prefix = result.getJsonArray("eleves").getJsonObject(0).getString("nameClasse");
                                            result.put("nameClass", prefix);
                                            prefix += "_" + libellePeriode;
                                            prefix = prefix.replaceAll(" ", "_");
                                            setParamsExportMoys(result, withMoyGeneraleByEleve, withMoyMinMaxByMat,
                                                    text, request, prefix);
                                        }
                                    }
                                });
                            } else {
                                result.put("periode", "Année");
                                String prefix = result.getJsonArray("eleves").getJsonObject(0).getString("nameClasse");
                                result.put("nameClass", prefix);
                                prefix = prefix.replaceAll(" ", "_") + "_" + "Année";
                                setParamsExportMoys(result, withMoyGeneraleByEleve, withMoyMinMaxByMat,
                                        text, request, prefix);
                            }
                        }
                    };


                    Handler<Either<String, JsonObject>> getMoysEleveByMatHandler = event -> {
                        if(!event.isRight()) {
                            leftToResponse(request, event.left());
                            log.error(event.left());
                        } else {
                            resultElevesTab[0] = event.right().getValue();
                            noteService.getMatEvaluatedAndStat(mapAllidMatAndidTeachers, mapIdMatListMoyByEleve, getMatEvaluatedAndStatHandler);
                        }
                    };


                    if(idPeriode != null){
                        //in this case, in mapIdMatListMoyByEleve, this average is the average of the periode
                        noteService.getMoysEleveByMatByPeriode(idClasse, idPeriode, mapAllidMatAndidTeachers,
                                mapIdMatListMoyByEleve, getMoysEleveByMatHandler);
                    } else {
                        List<String> listIdClasse = new ArrayList<>();
                        listIdClasse.add(idClasse);
                        utilsService.getPeriodes(listIdClasse, null, new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle(Either<String, JsonArray> event) {
                                if(event.isLeft()){
                                    leftToResponse(request, event.left());
                                    log.error(event.left().getValue());
                                } else{
                                    JsonArray periodes = event.right().getValue();
                                    //in this case, in mapIdMatListMoyByEleve, this average is the average of the year
                                    noteService.getMoysEleveByMatByYear(periodes, mapAllidMatAndidTeachers, mapIdMatListMoyByEleve, getMoysEleveByMatHandler);
                                }
                            }
                        });
                    }
                }
            }

        });
    }

    void setParamsExportMoys (JsonObject result, Boolean withMoyGeneraleByEleve, Boolean withMoyMinMaxByMat,
                              Boolean text, HttpServerRequest request, String prefix){
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