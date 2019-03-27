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
import fr.openent.competences.bean.lsun.ElementProgramme;
import fr.openent.competences.bean.lsun.*;
import fr.openent.competences.service.*;
import fr.openent.competences.service.impl.*;
import fr.openent.competences.utils.FormateFutureEvent;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.openent.competences.Utils.getLibelle;
import static fr.openent.competences.bean.lsun.TypeEnseignant.fromValue;
import static fr.openent.competences.service.impl.DefaultLSUService.DISCIPLINE_KEY;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;


/**
 * Created by agnes.lapeyronnie on 30/06/2017.
 */
public class LSUController extends ControllerHelper {

    protected static final Logger log = LoggerFactory.getLogger(LSUController.class);
    private ObjectFactory objectFactory = new ObjectFactory();
    private UtilsService utilsService;
    private BFCService bfcService;
    private BfcSyntheseService bfcSynthseService;
    private EleveEnseignementComplementService eleveEnsCpl;
    private JsonObject errorsExport;
    private EventBus ebController;
    private DispenseDomaineEleveService dispenseDomaineEleveService;
    private final BilanPeriodiqueService bilanPeriodiqueService;
    private final ElementBilanPeriodiqueService elementBilanPeriodiqueService;
    private final DefaultSyntheseBilanPeriodiqueService syntheseBilanPeriodiqueService;
    private final DefaultCompetenceNoteService competenceNoteService;
    private LSUService lsuService;
    private int fakeCode = 10;

    private static final String TIME = "Time";
    private static final String MESSAGE = "message";

    //ID
    private static final String EPI_GROUPE = "EPI_GROUPE_";
    private static final String ACC_GROUPE = "ACC_GROUPE_";



    public LSUController(EventBus eb) {
        this.ebController = eb;
        bilanPeriodiqueService = new DefaultBilanPerioqueService(eb);
        elementBilanPeriodiqueService = new DefaultElementBilanPeriodiqueService(eb);
        utilsService = new DefaultUtilsService();
        bfcService = new DefaultBFCService(eb);
        syntheseBilanPeriodiqueService = new DefaultSyntheseBilanPeriodiqueService();
        bfcSynthseService = new DefaultBfcSyntheseService(Competences.COMPETENCES_SCHEMA, Competences.BFC_SYNTHESE_TABLE, eb);
        eleveEnsCpl = new DefaultEleveEnseignementComplementService(Competences.COMPETENCES_SCHEMA,Competences.ELEVE_ENSEIGNEMENT_COMPLEMENT);
        dispenseDomaineEleveService = new DefaultDispenseDomaineEleveService(Competences.COMPETENCES_SCHEMA,Competences.DISPENSE_DOMAINE_ELEVE);
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA,Competences.COMPETENCES_NOTES_TABLE);
        lsuService = new DefaultLSUService(eb);
    }


    /**
     * Methode qui contruit le xml pour le LSU
     *
     * @param request contient la list des idsClasse et des idsResponsable ainsi que idStructure sur laquelle sont les responsables
     */
    @Post("/exportLSU/lsu")
    @ApiDoc("Export data to LSUN xml format")
    @SecuredAction("competences.lsun.export")
    public void getXML(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject entries) {
                if (entries.containsKey("type")) {
                    if ("1".equals(entries.getString("type"))) {
                        bilanFinCycleExport(request, entries);
                    } else {
                        bilanPeriodiqueExport(request, entries);
                    }
                } else {
                    badRequest(request, "No valid params");
                    return;
                }
            }
        });
    }

    /**
     * méthode qui récupère les responsables de direction à partir de idStructure
     *
     * @param request
     */
    @Get("/responsablesDirection")
    @ApiDoc("Retourne les responsables de direction de l'établissement passé en paramètre")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getResponsablesDirection(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if (user != null && request.params().contains("idStructure")) {
                    JsonObject action = new JsonObject()
                            .put("action", "user.getResponsablesDirection")
                            .put("idStructure", request.params().get("idStructure"));
                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            JsonObject body = message.body();
                            if ("ok".equals(body.getString("status"))) {
                                Renders.renderJson(request, body.getJsonArray("results"));
                            } else {
                                JsonObject error = new JsonObject()
                                        .put("error", body.getString(MESSAGE));
                                Renders.renderJson(request, error, 400);
                            }
                        }
                    }));
                } else {
                    badRequest(request);
                }
            }
        });
    }


    private void bilanFinCycleExport(HttpServerRequest request, JsonObject entries) {
        if (!entries.containsKey("idStructure")
                || !entries.containsKey("classes")
                || !entries.containsKey("responsables")
                || !entries.containsKey("stsFile")) {
            badRequest(request, "bilanFinCycleExport - No valid params");
            return;
        }

        final String idStructure = entries.getString("idStructure");
        log.info("idStructure = " + idStructure);
        final List<String> idsClasse = getIdsList(entries.getJsonArray("classes"));
        final List<String> idsResponsable = getIdsList(entries.getJsonArray("responsables"));
        final JsonArray enseignantFromSts = entries.getJsonArray("stsFile");

        //instancier le lsunBilans qui sera composé de entete,donnees et version
        final LsunBilans lsunBilans = objectFactory.createLsunBilans();
        //donnees composée de responsables-etab, eleves et bilans-cycle
        final Donnees donnees = objectFactory.createDonnees();
        final Map<String,JsonArray> mapIdClassHeadTeacher = new HashMap<>();

        Handler<String> getBilanfinCycleHandler = event -> {

            if(event.equals("success") && errorsExport.isEmpty()){
                log.info("FIN exportLSU : export ");
                lsunBilans.setDonnees(donnees);
                returnResponse(request, lsunBilans);
            }else{
                renderJson(request,errorsExport, 400);
                log.info("getXML : BaliseBilansCycle");
            }

        };

        List<Future> listFutureGetMethodsBFC = new ArrayList<>();

        Future<JsonObject> getEnteteFuture = Future.future();
        listFutureGetMethodsBFC.add(getEnteteFuture);
        Handler<String> getEnteteHandler = event -> {
            if (event.equals("success")) {
                getEnteteFuture.complete();
            } else {
                getEnteteFuture.fail("can't generate Balises Entete Future");
                log.error("getXML : getBaliseEntete " + event);
            }
        };
        getBaliseEntete(lsunBilans, idStructure, getEnteteHandler);

        Future<JsonObject> getResponsableFuture = Future.future();
        listFutureGetMethodsBFC.add(getResponsableFuture);
        Handler<String> getResponsableHandler = event -> {
            if (event.equals("success")) {
                getResponsableFuture.complete();
            } else {
                getResponsableFuture.fail("can't generate Balises Responsables Future");
                log.error("getXML : getBaliseResponsable " + event);
            }
        };
        getBaliseResponsables(donnees, idsResponsable, getResponsableHandler);

        Future<JsonObject> getElevesFuture = Future.future();
        listFutureGetMethodsBFC.add(getElevesFuture);
        Handler<String> getElevesHandler = event -> {
            if (event.equals("success")) {
                getElevesFuture.complete();
            } else {
                getElevesFuture.fail("can't generate Balises Eleves Future");
                log.error("getXML : getBaliseEleves " + event);
            }
        };
        getBaliseEleves(donnees, idsClasse, getElevesHandler);

        Future<Map<String,List<Enseignant>>> getHeadTeachersFuture = Future.future();
        listFutureGetMethodsBFC.add(getHeadTeachersFuture);
        Handler<String> getHeadTeachersHandler = event -> {
            Map<String,List<Enseignant>> mapIdClassListHeadTeacher = new HashMap<>();
            if(event.equals("success")){

                if(mapIdClassHeadTeacher != null && mapIdClassHeadTeacher.size() > 0){
                    for(Map.Entry<String,JsonArray> jsonArrayEntry : mapIdClassHeadTeacher.entrySet() ){
                        JsonArray arrayHeadTeachers = jsonArrayEntry.getValue();
                        List<Enseignant> listHeadTeacher = new ArrayList<>();
                        if(arrayHeadTeachers != null && arrayHeadTeachers.size() > 0){
                            for(int i = 0; i < arrayHeadTeachers.size(); i++){
                                Enseignant headTeacherEnseignant = addorFindTeacherBalise(donnees,enseignantFromSts,arrayHeadTeachers.getJsonObject(i));
                                 if(headTeacherEnseignant != null){
                                    listHeadTeacher.add(headTeacherEnseignant);
                                }
                            }
                        }
                        mapIdClassListHeadTeacher.put(jsonArrayEntry.getKey(),listHeadTeacher);
                    }
                }
                getHeadTeachersFuture.complete(mapIdClassListHeadTeacher);

            }else{
                getHeadTeachersFuture.complete(mapIdClassListHeadTeacher);
                log.error("getXML LSU : getHeadteachers "+ event);
            }
        };
        getHeadTeachers( idsClasse, mapIdClassHeadTeacher,getHeadTeachersHandler);


        Future<Map<String, JsonObject>> getDatesCreationVerrouByClassesFuture = Future.future();
        listFutureGetMethodsBFC.add(getDatesCreationVerrouByClassesFuture);
        Handler<Either<String, Map<String, JsonObject>>> getDatesCreationVerrouHandler = event -> {
            if(event.isRight()){
                getDatesCreationVerrouByClassesFuture.complete(event.right().getValue());
            }else{
                log.error("getXML LSU : getDatesCreationVerrouByClasses " + event);
                getDatesCreationVerrouByClassesFuture.fail("getXML LSU : getDatesCreationVerrouByClasses " +
                        event.left().getValue());
            }
        };
        Utils.getDatesCreationVerrouByClasses(eb,idStructure,idsClasse,getDatesCreationVerrouHandler);

        Future<List<Map>> getIdClassIdCycleValueFuture = Future.future();
        listFutureGetMethodsBFC.add(getIdClassIdCycleValueFuture);
        Handler<Either<String, List<Map>>> getIdClassIdCycleValueHandler = event -> {
            if(event.isRight()){
                getIdClassIdCycleValueFuture.complete(event.right().getValue());

            }else{
                log.error("getXML LSU : getIdClassIdCycleValue : list (map<idclasse,idCycle>,map<idCycle,cycle>) " + event.left().getValue());
                getIdClassIdCycleValueFuture.fail("getXML LSU : getIdClassIdCycleValue : list (map<idclasse,idCycle>,map<idCycle,cycle>) "
                + event.left().getValue());
            }
        };
        lsuService.getIdClassIdCycleValue(idsClasse, getIdClassIdCycleValueHandler );

        Future<Map<String,Map<Long, String>>> getMapIdClassCodeDomaineByIdFuture = Future.future();
        listFutureGetMethodsBFC.add(getMapIdClassCodeDomaineByIdFuture);
        Handler<Either<String, Map<String,Map<Long, String>>>> getMapCodeDomaineByIdHandler = event -> {
            if(event.isRight()){
                getMapIdClassCodeDomaineByIdFuture.complete(event.right().getValue());
            }else{
                log.error("getXML LSU : getMapCodeDomaineById error when collecting codeDomaineById " + event.left().getValue());
                getMapIdClassCodeDomaineByIdFuture.fail("getMapCodeDomaineById : map<");

            }
        };
        lsuService.getMapIdClassCodeDomaineById(idsClasse,getMapCodeDomaineByIdHandler);


        lsunBilans.setSchemaVersion("3.0");
        log.info("DEBUT  get exportLSU : export Classe : " + idsClasse);
        if (!idsClasse.isEmpty() && !idsResponsable.isEmpty()) {

            CompositeFuture.all(listFutureGetMethodsBFC).setHandler(event -> {
                if(event.succeeded()){
                    Map<String, JsonObject> dateCreationVerrouByClasse = getDatesCreationVerrouByClassesFuture.result();
                    Map<String,List<Enseignant>> mapIdClassListHeadTeacher = getHeadTeachersFuture.result();
                    List<Map> listMapClassCycle = getIdClassIdCycleValueFuture.result();
                    Map<String,Map<Long,String>> mapIdClasseCodesDomaines = getMapIdClassCodeDomaineByIdFuture.result();
                    getBaliseBilansCycle(mapIdClasseCodesDomaines, listMapClassCycle, donnees, idsClasse, idStructure, dateCreationVerrouByClasse, mapIdClassListHeadTeacher, getBilanfinCycleHandler);
                }else{
                    badRequest(request, event.cause().getMessage());
                }
            });
        }else {
            badRequest(request, "Classes or Responsable are empty.");
        }
        log.info("FIN exportLSU : export ");
    }


    /**
     * Methode qui contruit le xml pour le LSU
     *
     * @param request contient la list des idsClasse et des idsResponsable ainsi que idStructure sur laquelle sont les responsables
     * @param entries
     */
    private void bilanPeriodiqueExport(final HttpServerRequest request, JsonObject entries) {

        if (!entries.containsKey("idStructure")
                || !entries.containsKey("classes")
                || !entries.containsKey("responsables")
                || !entries.containsKey("periodes_type")
                || !entries.containsKey("stsFile")) {
            badRequest(request, "bilanPeriodiqueExport - No valid params");
            return;
        }

        final String idStructure = entries.getString("idStructure");//"a1b2a3fb-35b3-4e3e-8c2e-6991b5ec2887";
        final List<String> idsClasse = getIdsList(entries.getJsonArray("classes"));//Arrays.asList("63af9677-280b-4853-b7cd-c61440fa61cc", "12dd23cb-b32f-4b28-842f-04d08e372ccf");//3Aeme 5Ceme
        final List<String> idsResponsable = getIdsList(entries.getJsonArray("responsables"));//Arrays.asList("92ecac5a-c563-4c28-8a9f-1c5d8ad89bd2");//ce.fouquet
        final List<Integer> idsTypePeriodes = new ArrayList<Integer>();
        JsonArray dataPeriodesType = entries.getJsonArray("periodes_type");
        final JsonArray enseignantFromSts = entries.getJsonArray("stsFile");
        for (int i = 0; i < dataPeriodesType.size(); i++) {
            idsTypePeriodes.add(dataPeriodesType.getJsonObject(i).getInteger("id_type"));
        }

        final LsunBilans lsunBilans = objectFactory.createLsunBilans();//instancier le lsunBilans qui sera composé de entete,donnees et version
        //donnees composée de responsables-etab, eleves et bilans-cycle
        final Donnees donnees = objectFactory.createDonnees();
        final JsonObject epiGroupAdded = new JsonObject();
        final Map<String, JsonArray> tableConversionByClass= new HashMap<>();
        final Map<String, JsonArray> periodesByClass = new HashMap<>();
        final Map<String, JsonArray> mapIdClassHeadTeachers = new HashMap<>();
        lsuService.initIdsEvaluatedDiscipline();
        fakeCode = 10;
        donnees.setEnseignants(objectFactory.createDonneesEnseignants());

        Handler<Either.Right<String, JsonObject>> getBilansPeriodiquesHandler = backresponse -> {

            JsonObject data = backresponse.right().getValue();
            // récupération des disciplines évaluées
            lsuService.validateDisciplines(lsuService.getIdsEvaluatedDiscipline(), donnees, errorsExport);
            if (data.getInteger("status") == 200 && errorsExport.isEmpty()) {
                log.info("FIN exportLSU : export ");
                lsunBilans.setDonnees(donnees);
                returnResponse(request, lsunBilans);
            } else {
                renderJson(request,errorsExport, 400);
                log.error("getXML : getBaliseBilansPeriodiques " + data.getString("error"));
               //badRequest(request, "getXML : getBaliseBilansPeriodiques " + backresponse);
            }
        };

        Handler<String> getApEpiParcoursHandler = event -> {
            if (event.equals("success")) {
                this.getBaliseBilansPeriodiques(donnees, idStructure, periodesByClass,
                        epiGroupAdded, tableConversionByClass,enseignantFromSts,mapIdClassHeadTeachers, getBilansPeriodiquesHandler);

            } else {
            log.error("getXML : getApEpiParcoursBalises " + event);
            badRequest(request, "getXML : getApEpiParcoursBalises "+ event);
            }
        };

        List<Future> listGetFuture = new ArrayList<Future>();

        Future getHeadTeachersFuture = Future.future();
        listGetFuture.add(getHeadTeachersFuture);
        Handler<String> getHeadTeachersHandler = event -> {
            if (event.equals("success")) {
                log.info("getHeadTeachers");
                getHeadTeachersFuture.complete();

            } else {

                leftToResponse(request, new Either.Left<>(event));
                log.error("getXML : getBaliseEnseignants " + event);
            }
        };
        getHeadTeachers(idsClasse,mapIdClassHeadTeachers,getHeadTeachersHandler);


        Future<JsonObject> getTableConversionFuture = Future.future();
        listGetFuture.add(getTableConversionFuture);

        Handler<String> getTableConversionHandler = event -> {
            if(event.equals("success")){
                log.info("getConversionTableFuture");
                getTableConversionFuture.complete();
            }else{
                log.error("getXML : getConversionTable ");
                getTableConversionFuture.fail("can't get the conversion table Future");
            }
        };

        getTableConversion(idStructure,idsClasse, tableConversionByClass, getTableConversionHandler);

        Future<JsonObject> getEnseignantsFuture = Future.future();
        listGetFuture.add(getEnseignantsFuture);
        Handler<String> getEnseignantsHandler = event -> {
            if (event.equals("success")) {
                log.info("getEnseignantsFuture");
                getEnseignantsFuture.complete();
            } else {
                getEnseignantsFuture.complete();
                leftToResponse(request, new Either.Left<>(event));
                log.error("getXML : getBaliseEnseignants " + event);
            }
        };
        getBaliseEnseignants(donnees, idStructure, idsClasse, enseignantFromSts, getEnseignantsHandler);

        Future<JsonObject> getPeriodesFuture = Future.future();
        listGetFuture.add(getPeriodesFuture);
        Handler<String> getPeriodesHandler = event -> {
            if (event.equals("success")) {
                log.info("getPeriodesFuture");
                getPeriodesFuture.complete();
            } else {
                getPeriodesFuture.fail("can't generate Balises Periodes Future");
                log.error("getXML : getBalisePeriode " + event);
            }
        };
        getBalisePeriodes(donnees, idsTypePeriodes, periodesByClass, idStructure, idsClasse, getPeriodesHandler);

        Future<JsonObject> getElevesFuture = Future.future();
        listGetFuture.add(getElevesFuture);
        Handler<String> getElevesHandler = event -> {
            if (event.equals("success")) {
                log.info("getElevesFuture");
                getElevesFuture.complete();
            } else {
                getElevesFuture.fail("can't generate Balises Eleves Future");
                log.error("getXML : getBaliseEleves " + event);
            }
        };
        getBaliseEleves(donnees, idsClasse, getElevesHandler);


        Future<JsonObject> getDisciplineFuture = Future.future();
        listGetFuture.add(getDisciplineFuture);
        Handler<String> getDisciplineHandler = event -> {
            if (event.equals("success")) {
                log.info("getDisciplineFuture");
                getDisciplineFuture.complete();
            } else {
                getDisciplineFuture.fail("can't generate Balises Disciplines Future");
                log.error("getXML : getBaliseDiscipline " + event);
            }
        };
        getBaliseDisciplines(donnees, idStructure, getDisciplineHandler);

        Future<JsonObject> getResponsableFuture = Future.future();
        listGetFuture.add(getResponsableFuture);
        Handler<String> getResponsableHandler = event -> {
            if (event.equals("success")) {
                log.info("getResponsableFututre");
                getResponsableFuture.complete();
            } else {
                getResponsableFuture.fail("can't generate Balises Responsables Future");
                log.error("getXML : getBaliseResponsable " + event);
            }
        };
        getBaliseResponsables(donnees, idsResponsable, getResponsableHandler);

        Future<JsonObject> getEnteteFuture = Future.future();
        listGetFuture.add(getEnteteFuture);
        Handler<String> getEnteteHandler = event -> {
            if (event.equals("success")) {
                log.info("getEnteteFuture");
                getEnteteFuture.complete();
            } else {
                getEnteteFuture.fail("can't generate Balises Entete Future");
                log.error("getXML : getBaliseEntete " + event);
            }
        };
        getBaliseEntete(lsunBilans, idStructure, getEnteteHandler);

        lsunBilans.setSchemaVersion("3.0");
        log.info("DEBUT  get exportLSU : export Classe : " + idsClasse);
        if (!idsClasse.isEmpty() && !idsResponsable.isEmpty()) {
            log.info("before CompositeFuture bilanPeriodiqueExport");
            CompositeFuture.all(listGetFuture).setHandler(
                    event -> {
                        log.info("out future 1 ");
                        if (event.succeeded()) {
                            log.info("getApEpiParcoursBalises");
                            getApEpiParcoursBalises(donnees, idsClasse, idStructure, epiGroupAdded, enseignantFromSts, getApEpiParcoursHandler);
                        }
                        else{
                            badRequest(request, event.cause().getMessage());

                        }
                    });
        } else {
            badRequest(request, "Classes or Responsable are empty.");
        }
    }


    /**
     * complete la balise entete et la set a lsunBilans
     * @param lsunBilans
     * @param idStructure
     * @param handler
     */
    private void getBaliseEntete(final LsunBilans lsunBilans, final String idStructure, final Handler<String> handler) {
        JsonObject action = new JsonObject()
                .put("action", "user.getUAI")
                .put("idEtabl", idStructure);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    int count = 0;
                    AtomicBoolean answer = new AtomicBoolean(false);
                    final String thread = "idStructure -> " + idStructure;
                    final String method = "getBaliseEntete";
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        if ("ok".equals(body.getString("status")) && !body.getJsonObject("result").isEmpty()) {
                            // log for time-out
                            answer.set(true);
                            lsuService.serviceResponseOK(answer, count, thread, method);

                            JsonObject valueUAI = body.getJsonObject("result");
                            if (valueUAI != null) {
                                Entete entete = objectFactory.createEntete("CGI","OpenENT", valueUAI.getString("uai"));
                                lsunBilans.setEntete(entete);
                                handler.handle("success");
                            } else {
                                handler.handle("UAI de l'établissement null");
                                log.error("UAI etablissement null");
                            }
                        } else {
                            String error = body.getString(MESSAGE);
                            count ++;
                            if(error!=null && error.contains(TIME)){
                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                        handlerToAsyncHandler(this));
                            }
                            else {
                                handler.handle("method getBaliseEntete : error when collecting UAI  " + error);
                                log.error("An error occured when collecting UAI for " + idStructure + " structure");
                            }
                            lsuService.serviceResponseOK(answer, count, thread, method);
                        }
                    }
                }));
    }

    //récupère chaque responsable d'établissement et les ajouter à la balise responsables-etab puis à la balise donnees
    private void getBaliseResponsables(final Donnees donnees, final List<String> idsResponsable,
                                       final Handler<String> handler) {

        JsonObject action = new JsonObject()
                .put("action", "user.getUsers")
                .put("idUsers", new fr.wseduc.webutils.collections.JsonArray(idsResponsable));
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    int count = 0;
                    AtomicBoolean answer = new AtomicBoolean(false);
                    final String thread = "idsResponsable -> " + action.encode();
                    final String method = "getBaliseResponsables";
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        if ("ok".equals(body.getString("status")) && body.getJsonArray("results").size()!= 0 ) {
                            JsonArray value = body.getJsonArray("results");
                            Donnees.ResponsablesEtab responsablesEtab = objectFactory.createDonneesResponsablesEtab();
                            try {
                                for (int i = 0; i < value.size(); i++) {
                                    JsonObject responsableJson = value.getJsonObject(i);
                                    if (!responsableJson.getString("externalId").isEmpty()  && !responsableJson.getString("displayName").isEmpty()) {
                                        ResponsableEtab responsableEtab = objectFactory.createResponsableEtab(responsableJson.getString("externalId"),responsableJson.getString("displayName"));
                                        responsablesEtab.getResponsableEtab().add(responsableEtab);
                                    } else {
                                        throw new Exception("attributs responsableEtab null");
                                    }
                                }
                                donnees.setResponsablesEtab(responsablesEtab);
                                // log for time-out
                                answer.set(true);
                                lsuService.serviceResponseOK(answer, count, thread, method);

                                handler.handle("success");
                            }catch (Throwable e){
                                handler.handle("method getBaliseResponsable : " +e.getMessage());
                                log.error("method getBaliseResponsable : " +e.getMessage());
                            }
                        } else {
                            String error =  body.getString(MESSAGE);
                            count ++;
                            if (error!=null && error.contains(TIME)){
                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                        handlerToAsyncHandler(this));
                            }
                            else {
                                handler.handle("getBaliseResponsable : error when collecting Responsable " + error);
                                log.error("method getBaliseResponsable an error occured when collecting Responsable "
                                        + idsResponsable);
                            }
                        }
                    }
                }));
    }

    /**
     * pour une liste de classe mise a jour des attributs de l'eleve et de son responsable.
     *
     * @param donnees la liste des eleves est ajoutee a la balise donnees
     * @param Classids liste des classes pour lesquelles le fichier xml doit etre genere
     * @param handler  renvoie  "success" si tout c'est bien passe
     */

    private void getBaliseEleves(final Donnees donnees, final List<String> Classids, final Handler<String> handler) {
        JsonObject action = new JsonObject()
                .put("action", "user.getElevesRelatives")
                .put("idsClass", new fr.wseduc.webutils.collections.JsonArray(Classids));
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    int count = 0;
                    AtomicBoolean answer = new AtomicBoolean(false);
                    final String thread = "idsResponsable -> " + action.encode();
                    final String method = "getBaliseEleves";
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        if ("ok".equals(body.getString("status")) && body.getJsonArray("results").size() != 0) {
                            JsonArray jsonElevesRelatives = body.getJsonArray("results");
                            Eleve eleve = null;
                            //Responsable responsable = null;
                            Adresse adresse = null;
                            Donnees.Eleves eleves = objectFactory.createDonneesEleves();
                            for (int i = 0; i < jsonElevesRelatives.size(); i++) {
                                JsonObject o = jsonElevesRelatives.getJsonObject(i);
                                Responsable responsable = null;
                                String idEleve = o.getString("idNeo4j");
                                if(idEleve == null) {
                                    idEleve = o.getString("id");
                                }
                                if (!eleves.containIdEleve(idEleve)) {
                                    String[] externalIdClass ;
                                    String className;
                                    if (o.getString("externalIdClass") != null) {
                                        externalIdClass = o.getString("externalIdClass").split("\\$");
                                        className = externalIdClass[(externalIdClass.length - 1)];
                                        try {
                                            eleve = objectFactory.createEleve(o.getString("externalId"), o.getString("attachmentId"), o.getString("firstName"),
                                                    o.getString("lastName"), className, o.getString("idNeo4j"), o.getString("idClass"), o.getString("level"));
                                            eleves.add(eleve);
                                        } catch (Exception e) {
                                            if(e instanceof NumberFormatException){
                                                log.error(" method getBaliseEleve : creationEleve " + e.getMessage() +"new BigInteger(attachmentId) is impossible attachmentId : "+o.getString("attachmentId"));
                                            }else {
                                                // log for time-out
                                                answer.set(true);
                                                lsuService.serviceResponseOK(answer, count, thread, method);
                                                handler.handle(e.getMessage());
                                                log.error(" method getBaliseEleve : creationEleve " + e.getMessage());
                                            }
                                        }
                                    }else {

                                        log.info("[EXPORT LSU]: remove " + o.getString("name")
                                                + o.getString("firstName"));

                                    }
                                } else {
                                    eleve = eleves.getEleveById(idEleve);
                                }


                                String adress = o.getString("address");
                                String codePostal =  o.getString("zipCode");
                                String commune = o.getString("city");

                                // gestion données non renseignées
                                adress = (adress == null || adress.isEmpty()) ? "inconnue" : adress;
                                codePostal = (codePostal == null || codePostal.isEmpty()) ? "inconnu" : codePostal;
                                commune = (commune == null || commune.isEmpty()) ? "inconnue" : commune;

                                if(codePostal.length() > 10){
                                    codePostal = codePostal.substring(0,10);
                                }
                                if(commune.length() > 100){
                                    commune = commune.substring(0,100);
                                }
                                adresse = objectFactory.createAdresse(adress, codePostal, commune);


                                if (o.getString("externalIdRelative")!= null && o.getString("lastNameRelative") !=null &&
                                        o.getString("firstNameRelative")!= null && o.getJsonArray("relative").size() > 0 ) {
                                    JsonArray relatives = o.getJsonArray("relative");

                                    String civilite = o.getString("civilite");

                                    for (int j = 0; j < relatives.size(); j++) {
                                        String relative = relatives.getString(j);
                                        String[] paramRelative = relative.toString().split("\\$");
                                        //création d'un responsable Eleve avec la civilite si MERE ou PERE

                                        if (o.getString("externalIdRelative").equals(paramRelative[0])) {
                                            if (adresse != null) {
                                                responsable = objectFactory.createResponsable(o.getString("externalIdRelative"), o.getString("lastNameRelative"),
                                                        o.getString("firstNameRelative"), relative, adresse);
                                            } else {
                                                responsable = objectFactory.createResponsable(o.getString("externalIdRelative"), o.getString("lastNameRelative"),
                                                        o.getString("firstNameRelative"), relative);
                                            }
                                            responsable.setCivilite(civilite);
                                        }
                                    }
                                    //le xml ne peut-être édité si le responsable n'a pas la civilité
                                    if (responsable != null && responsable.getCivilite() != null
                                            && eleve != null) {
                                        eleve.getResponsableList().add(responsable);
                                    }
                                }
                            }
                            donnees.setEleves(eleves);
                            // log for time-out
                            answer.set(true);
                            lsuService.serviceResponseOK(answer, count, thread, method);
                            handler.handle("success");
                            log.info("FIN method getBaliseEleves : nombre d'eleve ajoutes :"+eleves.getEleve().size());

                        }else{
                            String error =  body.getString(MESSAGE);
                            count ++;
                            lsuService.serviceResponseOK(answer, count, thread, method);
                            if (error!=null && error.contains(TIME)){
                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                        handlerToAsyncHandler(this));
                            }
                            else {
                                handler.handle("getBaliseEleves : error when collecting Eleves " + error);
                                log.error("method getBaliseEleves an error occured when collecting Eleves " + error);
                            }

                        }
                    }
                }));
    }

    /**
     *  M
     * @param //classIds liste des idsClass dont on recherche le cycle auquel elles appartiennent
     * @param //handler retourne une liste de 2 map : map<idClass,idCycle> et map<idCycle,value_cycle>
     */

   /* private void getIdClassIdCycleValue(List<String> classIds, final Handler<Either<String, List<Map>>> handler) {
        utilsService.getCycle(classIds, new Handler<Either<String, JsonArray>>() {
            int count = 0;
            AtomicBoolean answer = new AtomicBoolean(false);
            final String thread = "classIds -> " + classIds.toString();
            final String method = "getIdClassIdCycleValue";
            @Override
            public void handle(Either<String, JsonArray> response) {
                if (response.isRight()) {
                    JsonArray cycles = response.right().getValue();
                    Map mapIclassIdCycle = new HashMap<>();
                    Map mapIdCycleValue_cycle = new HashMap<>();
                    List<Map> mapArrayList = new ArrayList<>();
                    try {
                        for (int i = 0; i < cycles.size(); i++) {
                            JsonObject o = cycles.getJsonObject(i);
                            if(o.getString("id_groupe")!=null &&o.getLong("id_cycle")!=null && o.getLong("value_cycle")!=null) {
                                mapIclassIdCycle.put(o.getString("id_groupe"), o.getLong("id_cycle"));
                                mapIdCycleValue_cycle.put(o.getLong("id_cycle"), o.getLong("value_cycle"));
                            }else {
                                throw new Exception ("Erreur idGroupe, idCycle et ValueCycle null");
                            }
                        }
                        mapArrayList.add(mapIclassIdCycle);
                        mapArrayList.add(mapIdCycleValue_cycle);
                    }catch(Exception e){
                        handler.handle(new Either.Left<String, List<Map>>(" Exception " + e.getMessage()));
                        log.error("catch Exception in getCycle" + e.getMessage());
                    }
                    answer.set(true);
                    lsuService.serviceResponseOK(answer, count, thread, method);
                    handler.handle(new Either.Right<String, List<Map>>(mapArrayList));
                } else {
                    String error =  response.left().getValue();
                    count ++;
                    lsuService.serviceResponseOK(answer, count, thread, method);
                    if (error!=null && error.contains(TIME)){
                        utilsService.getCycle(classIds, this);
                    }
                    else {
                        handler.handle(new Either.Left<String, List<Map>>(
                                " getValueCycle : error when collecting Cycles " + error));
                        log.error("method getIdClassIdCycleValue an error occured when collecting Cycles " + error);
                    }
                }
            }
        });
    }*/

  /*  private void getMapCodeDomaineById(String idClass, final Handler<Either<String, Map<Long, String>>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "user.getCodeDomaine")
                .put("idClass", idClass);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            int count = 0;
            AtomicBoolean answer = new AtomicBoolean(false);
            final String thread = "classIds -> " + idClass;
            final String method = "getMapCodeDomaineById";
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status"))) {
                    JsonArray domainesJson = message.body().getJsonArray("results");
                    Map<Long, String> mapDomaines = new HashMap<>();

                    try {
                        for (int i = 0; i < domainesJson.size(); i++) {
                            JsonObject o = domainesJson.getJsonObject(i);
                            if (CodeDomaineSocle.valueOf(o.getString("code_domaine")) != null) {
                                mapDomaines.put(o.getLong("id_domaine"), o.getString("code_domaine"));
                            }
                        }
                        //la mapDomaines n'est renvoyee que si elle contient les 8 codes domaine du socle commun
                        if (mapDomaines.size() == CodeDomaineSocle.values().length) {
                            // log for time-out
                            answer.set(true);
                            lsuService.serviceResponseOK(answer, count, thread, method);
                            handler.handle(new Either.Right<String, Map<Long, String>>(mapDomaines));
                        }
                        else{
                            throw new Exception("getMapCodeDomaine : map incomplete" );
                        }
                    }catch (Exception e) {

                        if(e instanceof IllegalArgumentException){
                            handler.handle(new Either.Left<String,Map<Long,String>>("code_domaine en base de données non valide"));
                        }else{
                            handler.handle(new Either.Left<String, Map<Long, String>>("getMapCodeDomaineById : "));
                            log.error("getMapCodeDomaineById : "+e.getMessage());
                        }
                        // log for time-out
                        answer.set(true);
                        lsuService.serviceResponseOK(answer, count, thread, method);
                    }
                } else {
                    String error =  body.getString(MESSAGE);
                    count ++;
                    lsuService.serviceResponseOK(answer, count, thread, method);
                    if (error!=null && error.contains(TIME)){
                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                handlerToAsyncHandler(this));
                    }
                    else {
                        handler.handle(new Either.Left<String, Map<Long, String>>(
                                "getMapCodeDomaineById : error when collecting codeDomaineById : " + error));
                        log.error("method getMapCodeDomaineById an error occured when collecting CodeDomaineById " +
                                error);
                    }
                }
            }
        }));
    }*/


    private JsonObject  getJsonObject(JsonArray rep ,String idEleve){
        JsonObject repSyntheseIdEleve = new JsonObject();
        for (int i=0; i<rep.size();i++){
            JsonObject o = rep.getJsonObject(i);
            if((o.getString("id_eleve")).equals(idEleve)){
                repSyntheseIdEleve = o;
            }
        }
        return repSyntheseIdEleve;
    }

    private XMLGregorianCalendar getDateFormatGregorian(String dateString) {
        XMLGregorianCalendar dateGregorian = null;
        try {
            SimpleDateFormat dfYYYYMMdd = new SimpleDateFormat("yyyy-MM-dd");
            Date date = dfYYYYMMdd.parse(dateString);
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            dateGregorian = DatatypeFactory.newInstance().newXMLGregorianCalendar();
            dateGregorian.setYear(cal.get(Calendar.YEAR));
            dateGregorian.setMonth(cal.get(Calendar.MONTH) + 1);
            dateGregorian.setDay(cal.get(Calendar.DATE));
        } catch (DatatypeConfigurationException | ParseException e) {
            e.printStackTrace();
        }
        return dateGregorian;
    }


    private Long giveIdDomaine(Map<Long,String> mapIdDomaineCodeDomaine, String value){
        Long idDomaine = null;
        for(Map.Entry<Long, String> idDomaineCode : mapIdDomaineCodeDomaine.entrySet()){
            if(idDomaineCode.getValue().equals(value)){
                idDomaine = idDomaineCode.getKey();
            }
        }
        return idDomaine;
    }


    private void setSocleSyntheseEnsCpl( final Map<String,List<Enseignant>> mapIdClassListHeadTeacher,
                                        final Map<Long, String> mapIdDomaineCodeDomaine, String[] idsEleve,
                                        final AtomicInteger nbEleveCompteur, Donnees donnees,
                                        final JsonArray ensCplsEleves, final JsonArray synthesesEleves,
                                        final Map<String, Map<Long, Integer>> mapIdEleveIdDomainePosition, final Long valueCycle,
                                        final String millesime, JsonObject datesCreationVerrou) {
        Donnees.Eleves eleves = donnees.getEleves();
        List<ResponsableEtab> responsablesEtab = donnees.getResponsablesEtab().getResponsableEtab();
        //on récupère id du codeDomaine CPD_ETR
        Long idDomaineCPD_ETR = giveIdDomaine(mapIdDomaineCodeDomaine, "CPD_ETR");

        for (String idEleve : idsEleve) {
            nbEleveCompteur.incrementAndGet();//compteur
            Eleve eleve = eleves.getEleveById(idEleve);
            if (eleve != null) {
                //on récupère le JsonObject de l'enseignement de complément et de la synthèse
                JsonObject ensCplEleve = getJsonObject(ensCplsEleves,idEleve);
                JsonObject syntheseEleve = getJsonObject(synthesesEleves, idEleve);
                //JsonObject erreursEleve = new JsonObject();
                //si l'élève est dans la mapIdEleveIdDomainePosition
                if (mapIdEleveIdDomainePosition.containsKey(eleve.getIdNeo4j())) {
                    //alors on récupère la map<Iddomaine,positionnement> de l'élève en cours
                    Map<Long, Integer> mapIdDomainePosition = mapIdEleveIdDomainePosition.get(eleve.getIdNeo4j());
                    //variable a true quand  la taille de map<Iddomaine,positionnement> est à 7 et qu'elle ne contient pas idDomaine
                    //correspondant au codeDomaineCPD_ETR
                    Boolean bmapSansIdDomaineCPDETR = (mapIdDomainePosition.size() == (mapIdDomaineCodeDomaine.size() - 1)
                            && !mapIdDomainePosition.containsKey(idDomaineCPD_ETR));
                    if (syntheseEleve.size() > 0 && (mapIdDomainePosition.size() == mapIdDomaineCodeDomaine.size() || bmapSansIdDomaineCPDETR)) {

                        final BilanCycle bilanCycle = objectFactory.createBilanCycle();
                        BilanCycle.Socle socle = objectFactory.createBilanCycleSocle();
                        if(valueCycle == 4) {
                            if (!ensCplEleve.containsKey("id_eleve")) {
                                //si l'élève n'a pas d'enseignement de complément par défault on met le code AUC et niv 0
                                EnseignementComplement enseignementComplement = new EnseignementComplement("AUC", 0);
                                bilanCycle.setEnseignementComplement(enseignementComplement);
                            }
                            //enseignement Complément s'il existe pour l'élève en cours
                            if (ensCplEleve.containsKey("id_eleve")) {
                                EnseignementComplement enseignementComplement = new EnseignementComplement(ensCplEleve.getString("code"), ensCplEleve.getInteger("niveau"));
                                bilanCycle.setEnseignementComplement(enseignementComplement);
                            }
                        }

                        // alors on peut ajouter le bilanCycle à l'élève avec la synthèse, les ensCpl et les codesDomaines et positionnement au socle
                        //Ajouter les CodesDomaines et positionnement
                        //on teste si la map<Iddomaine,positionnement> contient idDomaine correspondant à CPD_ETR
                        // alors on ajoute domaineSocleCycle manuellement avec le positionnement à zéro
                        if (bmapSansIdDomaineCPDETR) {
                            DomaineSocleCycle domaineSocleCycle = new DomaineSocleCycle(mapIdDomaineCodeDomaine.get(idDomaineCPD_ETR), 0);
                            socle.getDomaine().add(domaineSocleCycle);
                        }
                        // on parcours la map  idDomainePositionnement et on ajoute le code domaine qui a pour clé idDomaine en cours
                        for (Map.Entry<Long, Integer> idDomainePosition : mapIdDomainePosition.entrySet()) {
                            DomaineSocleCycle domaineSocleCycle = new DomaineSocleCycle(mapIdDomaineCodeDomaine.get(idDomainePosition.getKey()), idDomainePosition.getValue());
                            socle.getDomaine().add(domaineSocleCycle);
                        }
                        bilanCycle.setSocle(socle);

                        //la synthèse
                        bilanCycle.setSynthese(syntheseEleve.getString("texte"));
                        //dates
                        XMLGregorianCalendar dateCreation = getDateFormatGregorian(datesCreationVerrou.getString("date_creation"));
                        bilanCycle.setDateCreation(dateCreation);
                        bilanCycle.setDateVerrou(datesCreationVerrou.getString("date_verrou").substring(0,19));
                        bilanCycle.setMillesime(millesime);

                        //on ajoute les différents attributs de la balise BilanCycle de l'élève
                        ResponsableEtab responsableEtabRef = responsablesEtab.get(0);
                        bilanCycle.setResponsableEtabRef(responsableEtabRef);
                        //on ajoute les responsables de l'élève (attribut de clui-ci) aux responsables et au bilanCycle
                        if(eleve.getResponsableList() != null && eleve.getResponsableList().size()> 0) {
                            BilanCycle.Responsables responsablesEleve = objectFactory.createBilanCycleResponsables();
                            responsablesEleve.getResponsable().addAll(eleve.getResponsableList());
                            bilanCycle.setResponsables(responsablesEleve);
                        }

                        bilanCycle.setEleveRef(eleve);
                        try {
                            bilanCycle.setCycle(new BigInteger(String.valueOf(valueCycle)));
                        }catch (Exception e){
                            log.error("method setSocleSyntheseEnsCpl new BigInteger valueCycle : " + valueCycle + " " + e.getMessage());

                        }
                        if(mapIdClassListHeadTeacher != null && mapIdClassListHeadTeacher.containsKey(eleve.getId_Class())){
                            List<Enseignant> listHeadTeachers = mapIdClassListHeadTeacher.get(eleve.getId_Class());
                            if(listHeadTeachers != null && listHeadTeachers.size() > 0){
                                bilanCycle.getProfPrincRefs().addAll(listHeadTeachers);
                            }
                        }
                        donnees.getBilansCycle().getBilanCycle().add(bilanCycle);

                    } else {

                        //supprimer l'élève de la liste de la Balise ELEVES
                        eleves.getEleve().remove(eleve);
                        //affecter les différentes erreurs en fonction des conditions non respectées
                        if ( mapIdDomainePosition.size() != mapIdDomaineCodeDomaine.size() && !bmapSansIdDomaineCPDETR ) {
                            setError(errorsExport,eleve,"Des domaines n'ont pas été evalués");
                           if ( syntheseEleve.size() == 0 ) {
                                setError(errorsExport,eleve,"Pas de synthèse du BFC");

                            }
                        } else if ( syntheseEleve.size() == 0 ) {
                            setError(errorsExport,eleve,"Pas de synthèse du BFC");
                        }
                    }
                } else {//si l'élève n'est pas dans la map alors il n'a aucune évaluation et
                    // il faut le supprimer du xml donc de la list des élèves de la balise ELEVES
                    eleves.getEleve().remove(eleve);
                    setError(errorsExport,eleve,"Aucun domaine du socle commun ");
                    if(!syntheseEleve.containsKey("id_eleve")){
                        setError(errorsExport,eleve, "Pas de synthèse du BFC");
                    }
                }
            }else
                log.info("eleve qui n'est pas dans la list des eleves " + idEleve);
        }
    }



    /**
     * Construit la map des résultats des élèves en tenant compte de la dispense des domaines
     * Map<idEleve,Map<idDomaine,positionnement>>
     * @param listIdsEleve
     * @param idClass
     * @param idStructure
     * @param idCycle
     * @param handler
     */
    private void getResultsElevesByDomaine( List<String> listIdsEleve, String idClass, String idStructure, Long idCycle,
                                            Handler <Either<String,  Map<String, Map<Long, Integer>>>> handler){
        final Map<String, Map<Long, Integer>> resultatsEleves = new HashMap<>();
        final String[] idsEleve = listIdsEleve.toArray(new String[listIdsEleve.size()]);
        AtomicBoolean answer = new AtomicBoolean(false);
        AtomicInteger count = new AtomicInteger(0);
        final String thread = "(" + idClass + ", " + idStructure + ", " + idCycle + ")";
        final String method = "getResultsElevesByDomaine";

        bfcService.buildBFC(false, idsEleve, idClass, idStructure, null, idCycle,
                new Handler<Either<String, JsonObject>>() {
                    @Override
                    public void handle(final Either<String, JsonObject> repBuildBFC) {
                        if (repBuildBFC.isLeft()) {
                            String error = repBuildBFC.left().getValue();
                            lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                            if (error!=null && error.contains(TIME)){
                                bfcService.buildBFC(false, idsEleve, idClass, idStructure, null,
                                        idCycle,this);
                            }
                            else {
                                handler.handle(new Either.Left<>("getResultsElevesByDomaine : bfcService.buidBFC : " +
                                        error));
                                log.error("getBaliseBilansCycle XML buildBFC map<idEleve,map<idDomaine,positionnement>> : " +
                                        error);
                            }
                        }
                        else {
                            // On récupère la map des domaine dispensé par élève
                            dispenseDomaineEleveService.mapOfDispenseDomaineByIdEleve(listIdsEleve, new Handler<Either<String, Map<String, Map<Long, Boolean>>>>() {
                                @Override
                                public void handle(Either<String, Map<String, Map<Long, Boolean>>> respDispenseDomaine) {
                                    if (respDispenseDomaine.isLeft()) {
                                        String error = respDispenseDomaine.left().getValue();
                                        lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                                        if (error!=null && error.contains(TIME)){
                                            dispenseDomaineEleveService.mapOfDispenseDomaineByIdEleve(listIdsEleve,
                                                    this);
                                        }
                                        else {
                                            // Map dispense Error
                                        }
                                    }
                                    else{
                                        Map<String, Map<Long, Boolean>> mapIdEleveIdDomainedispense = respDispenseDomaine.right().getValue();
                                        final List<Future> futureDispensesElevesList = new ArrayList<Future>();
                                        for (String idEleve : idsEleve) {
                                            Future<JsonObject> futureDispenseEleve = Future.future();
                                            futureDispensesElevesList.add(futureDispenseEleve);
                                            JsonArray resultats = repBuildBFC.right().getValue().getJsonArray(idEleve);
                                            Map<Long, Integer> resultEleves = new HashMap<>();

                                            // si pas de resultats, on passe a l'élève suivant
                                            if (resultats == null) {
                                                futureDispenseEleve.complete();
                                                continue;
                                            }

                                            //variable qui permet de tester si pour un élève qui a une dispense sur un domaine a bien eu son positionnement à zéro
                                            //cas de l'élève qui a une dispense sur un domaine mais aucune évaluation("niveau")
                                            Boolean eleveHasDispenseDomaine = false;
                                            Map<Long, Boolean> idsDomainesDispense = new HashMap<>();
                                            for (Object resultat : resultats) {
                                                //si l'idEleve de l'élève en cours et l'iddomaine de result sont dans la mapIdEleveIdDomainedispense alors l'élève est dispensé pour ce domaine
                                                //et son niveau est zéro
                                                if (mapIdEleveIdDomainedispense.containsKey(idEleve)) {
                                                    eleveHasDispenseDomaine = mapIdEleveIdDomainedispense.containsKey(idEleve);
                                                    idsDomainesDispense = mapIdEleveIdDomainedispense.get(idEleve);
                                                    if (mapIdEleveIdDomainedispense.get(idEleve).containsKey(((JsonObject) resultat).getLong("idDomaine"))) {
                                                        if (idsDomainesDispense.get(((JsonObject) resultat).getLong("idDomaine"))) {
                                                            resultEleves.put(((JsonObject) resultat).getLong("idDomaine"), Competences.POSITIONNEMENT_ZERO);
                                                            idsDomainesDispense.remove(((JsonObject) resultat).getLong("idDomaine"));

                                                        } else {
                                                            resultEleves.put(((JsonObject) resultat).getLong("idDomaine"), ((JsonObject) resultat).getInteger("niveau"));
                                                        }
                                                    } else {
                                                        resultEleves.put(((JsonObject) resultat).getLong("idDomaine"), ((JsonObject) resultat).getInteger("niveau"));

                                                    }
                                                } else {
                                                    resultEleves.put(((JsonObject) resultat).getLong("idDomaine"), ((JsonObject) resultat).getInteger("niveau"));
                                                }
                                            }
                                            //si l'élève a des domaines dispensés non évalués
                                            if (eleveHasDispenseDomaine && !(idsDomainesDispense.size() == 0)) {
                                                for (Map.Entry<Long, Boolean> idDomaineDispense : idsDomainesDispense.entrySet()) {
                                                    if (idDomaineDispense.getValue()) {
                                                        resultEleves.put(idDomaineDispense.getKey(), Competences.POSITIONNEMENT_ZERO);
                                                    }
                                                }
                                            }

                                            resultatsEleves.put(idEleve, resultEleves);
                                            futureDispenseEleve.complete();
                                        }
                                        CompositeFuture.all(futureDispensesElevesList).setHandler(
                                                event ->  {
                                                    // log for time-out
                                                    answer.set(true);
                                                    lsuService.serviceResponseOK(answer, count.get(), thread, method);
                                                    handler.handle(new Either.Right<>(resultatsEleves));
                                                });

                                    }
                                }
                            });
                        }
                    }
                });
    }

    /**
     * permet de completer tous les attributs de la balise BilanCycle et de la setter à donnees
     * sauf les attributs de date, synthese et enseignements de complement
     * @param listMapClassCycle = Map<IdClass,idCycle> and Map<idCycle, libelle>
     * @param donnees permet de recuperer les eleves
     * @param idsClass classes list
     * @param idStructure id Structure
     * @param handler response
     */

    private void getBaliseBilansCycle( final Map<String,Map<Long,String>> mapIdClasseCodesDomaines,
                                       final List<Map> listMapClassCycle,
                                       final Donnees donnees,final List<String> idsClass, final String idStructure,
                                       final Map<String,JsonObject> dateCreationVerrouByClasse,
                                       final Map<String,List<Enseignant>> mapIdClassListHeadTeachers,
                                       final Handler<String> handler) {

        //final List<ResponsableEtab> responsablesEtab = donnees.getResponsablesEtab().getResponsableEtab();
        final Donnees.Eleves eleves = donnees.getEleves();
        final Integer nbElevesTotal = eleves.getEleve().size();
        final String millesime = getMillesimeBFC();
        final AtomicInteger nbEleveCompteur = new AtomicInteger(0);
        final Map<String, List<String>> mapIdClassIdsEleve = eleves.getMapIdClassIdsEleve();
        final Map mapIdClassIdCycle = listMapClassCycle.get(0);//map<IdClass,IdCycle>
        final Map mapIdCycleValue = listMapClassCycle.get(1);//map<IdCycle,ValueCycle>

        log.info("DEBUT : method getBaliseBilansCycle : nombreEleve : "+eleves.getEleve().size());
        errorsExport = new JsonObject();
        AtomicBoolean answer = new AtomicBoolean(false);
        AtomicInteger count = new AtomicInteger(0);
        final String thread = "("  + idStructure +  ")";
        final String method = "getBaliseBilansCycle";
        donnees.setBilansCycle(objectFactory.createDonneesBilansCycle());
        //on parcourt les classes
        for (final Map.Entry<String, List<String>> listIdsEleve : mapIdClassIdsEleve.entrySet()) {
            //récupère un tableau de sting idEleve nécessaire pour la méthode buildBFC de bfcService
            final String[] idsEleve = listIdsEleve.getValue().toArray(
                    new String[listIdsEleve.getValue().size()]);
            final String idClass = listIdsEleve.getKey();
            final JsonObject datesCreationVerrou = dateCreationVerrouByClasse.get(idClass);
            final List<String> listIdEleves = listIdsEleve.getValue();
            final Map<Long, String> mapIdDomaineCodeDomaine = mapIdClasseCodesDomaines.get(idClass);
            final Long idCycle = (Long) mapIdClassIdCycle.get(idClass);
            final Long valueCycle = (Long) mapIdCycleValue.get(idCycle);

            getResultsElevesByDomaine(listIdEleves, idClass, idStructure,(Long) mapIdClassIdCycle.get(idClass),
                    new Handler<Either<String, Map<String, Map<Long, Integer>>>>() {
                        @Override
                        public void handle(Either<String, Map<String, Map<Long, Integer>>> resultatsEleves) {

                            if(resultatsEleves.isLeft()) {

                                String error =  resultatsEleves.left().getValue();

                                lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                                if (error!=null && error.contains(TIME)) {
                                    getResultsElevesByDomaine(listIdEleves, idClass, idStructure,
                                            (Long) mapIdClassIdCycle.get(idClass),  this);
                                }
                                else {
                                    handler.handle("getBaliseBilansEleve :  : " + error);
                                    log.error("getBaliseBilansCycle XML:list (map<idclasse,idCycle>, " +
                                            " map<idCycle,cycle>) " + error);
                                }
                            }
                            else{

                                final Map<String, Map<Long, Integer>> mapIdEleveIdDomainePosition = resultatsEleves.right().getValue();

                                if (idCycle != null) {
                                    bfcSynthseService.getBfcSyntheseByIdsEleve(idsEleve, idCycle, new Handler<Either<String, JsonArray>>() {
                                        @Override
                                        public void handle(Either<String, JsonArray> repSynthese) {
                                            if (repSynthese.isLeft()) {
                                                String error =  repSynthese.left().getValue();
                                                lsuService.serviceResponseOK(answer,
                                                        count.incrementAndGet(), thread, method);
                                                if (error!=null && error.contains(TIME)) {
                                                    bfcSynthseService.getBfcSyntheseByIdsEleve(idsEleve,
                                                            idCycle, this);
                                                }
                                                else {
                                                    handler.handle("getBaliseBilansCycle XML requete synthese du BFC: " + error);
                                                    log.error("getBaliseBilansCycle requete synthese du BFC: " + error);
                                                }
                                            }
                                            else {
                                                final JsonArray synthesesEleves = repSynthese.right().getValue();
                                                eleveEnsCpl.listNiveauCplByEleves(idsEleve, new Handler<Either<String, JsonArray>>() {
                                                    @Override
                                                    public void handle(Either<String, JsonArray> repEleveEnsCpl) {
                                                        if (repEleveEnsCpl.isLeft()) {
                                                            String error =  repEleveEnsCpl.left().getValue();
                                                            lsuService.serviceResponseOK(answer,
                                                                    count.incrementAndGet(), thread, method);
                                                            if (error!=null && error.contains(TIME)) {
                                                                eleveEnsCpl.listNiveauCplByEleves(idsEleve,this);
                                                            }
                                                            else {
                                                                handler.handle("getBaliseBilansCycle XML requete enseignement complement: " + error);
                                                                log.error("getBaliseBilansCycle requete enseignementComplement: " + error);
                                                            }
                                                        }
                                                        else {
                                                            final JsonArray ensCplsEleves = repEleveEnsCpl.right().getValue();
                                                            setSocleSyntheseEnsCpl(mapIdClassListHeadTeachers, mapIdDomaineCodeDomaine,
                                                                    idsEleve, nbEleveCompteur, donnees, ensCplsEleves, synthesesEleves,
                                                                    mapIdEleveIdDomainePosition, valueCycle, millesime, datesCreationVerrou);

                                                            log.info("FIN method getBaliseBilansCycle nombre d'eleve dans la classe en cours : " + idsEleve.length);
                                                            log.info("FIN method getBaliseBilansCycle nombre de bilans de cycle complets : " + donnees.getBilansCycle().getBilanCycle().size());
                                                            log.info("nb d'eleves au depart : " + nbElevesTotal);
                                                            log.info("nb d'eleve parcouru : " + nbEleveCompteur.intValue());
                                                            if (nbEleveCompteur.intValue() == nbElevesTotal.intValue()) {
                                                                if (donnees.getBilansCycle().getBilanCycle().size() != 0) {
                                                                    donnees.setBilansCycle(donnees.getBilansCycle());
                                                                    log.info("FIN method getBaliseBilansCycle nombre d'eleve avec un bilan cycle complet " + eleves.getEleve().size());
                                                                }
                                                                log.info("FIN method getBaliseBilansCycle nombre d'eleve avec un bilan cycle incomplet " + errorsExport.getMap().size());
                                                                handler.handle("success");
                                                                // log for time-out
                                                                answer.set(true);
                                                                lsuService.serviceResponseOK(answer, count.get(), thread, method);
                                                            }

                                                        }
                                                    }
                                                });

                                            }
                                        }
                                    });
                                } else {
                                   handler.handle("getBaliseBilansCycle XML idCycle  :  " + idCycle);
                                    log.error("getBaliseBilansCycle idCycle :  " + idCycle);
                                }
                            }
                        }
                    });
        }
    }

    private String getCode(String externalId, Map<String, String> lisCode){
        Object result = externalId;
        try {
            Long.valueOf(externalId);
        }
        catch (NumberFormatException e){
            result = lisCode.get(externalId);
            if(result == null){
                log.info(" No Code Found " + externalId);
                result = "0000" + String.valueOf(++fakeCode);
            }
        }
        return result.toString();
    }
    private void setDisciplineForStructure(final Donnees donnees, JsonArray listSubject, Map<String, String> listCode,
                                           final Handler<String> handler) {
        donnees.setDisciplines(objectFactory.createDonneesDisciplines());
        if (listSubject != null && !listSubject.isEmpty()) {
            listSubject.forEach(item -> {
                JsonObject currentSubject = (JsonObject) item;
                Discipline discipline = objectFactory.createDiscipline();
                String externalId =  currentSubject.getString("externalId");
                discipline.setCode(getCode(externalId, listCode));
                discipline.setId(DISCIPLINE_KEY + currentSubject.getString("id"));
                discipline.setLibelle(currentSubject.getString("name"));
                discipline.setModaliteElection(ModaliteElection.fromValue("S"));
                donnees.getDisciplines().getDiscipline().add(discipline);
            });
            JsonObject response = new JsonObject();
            response.put("status", "success");
            response.put("data", "success");
            // log for time-out
            handler.handle("success");
        }
    }
    private void getBaliseDisciplines(final Donnees donnees, final String idStructure, final Handler<String> handler) {

        Future<Map<String, String>> libelleCourtFuture = Future.future();
        new DefaultMatiereService().getLibellesCourtsMatieres(false, event -> {
            FormateFutureEvent.formate(libelleCourtFuture, event);
        });
        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatieresForUser")
                .put("userType", "Personnel")
                .put("idUser", "null")
                .put("idStructure", idStructure)
                .put("onlyId", false);
        Future<JsonArray> disciplines = Future.future();
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    AtomicBoolean answer = new AtomicBoolean(false);
                    AtomicInteger count = new AtomicInteger(0);
                    final String thread = "("  + idStructure +  ")";
                    final String method = "getBaliseDisciplines";
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        if ("ok".equals(body.getString("status"))) {
                            try {
                                JsonArray listSubject = body.getJsonArray("results");
                                disciplines.complete(listSubject);
                                answer.set(true);
                                lsuService.serviceResponseOK(answer, count.get(), thread, method);
                            } catch (Throwable e) {
                                disciplines.fail("method getBaliseResponsable : " + e.getMessage());
                            }
                        } else {
                            String error = body.getString(MESSAGE);
                            lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                            if (error!=null && error.contains(TIME)) {
                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                        handlerToAsyncHandler(this));
                            }
                            else {
                                String failureMessage = "getBaliseDisciplines discipline :" +
                                        " error eb matiere.getMatieresForUser ko";
                                disciplines.fail(failureMessage);
                            }
                        }
                    }}));

        CompositeFuture.all(libelleCourtFuture, disciplines).setHandler(event -> {
            if(event.failed()){
               handler.handle(event.cause().getMessage());
            }
            else{
                setDisciplineForStructure(donnees, disciplines.result(), libelleCourtFuture.result(), handler);
            }

        });
    }

    private void getBalisePeriodes(final Donnees donnees, final List<Integer> wantedPeriodes, final Map<String, JsonArray> periodesByClass,
                                   final String idStructure, final List<String> idClasse, final Handler<String> handler) {
        JsonObject action = new JsonObject()
                .put("action", "periode.getPeriodes")
                .put("idGroupes", idClasse)
                .put("idEtablissement", idStructure);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    AtomicBoolean answer = new AtomicBoolean(false);
                    AtomicInteger count = new AtomicInteger(0);
                    final String thread = "("  + idStructure + ", "+ idClasse.toString()+ " )";
                    final String method = "getBalisePeriodes";

                    @Override
                    public void handle(Message<JsonObject> message) {

                        JsonObject body = message.body();
                        if ("ok".equals(body.getString("status"))) {
                            try {
                                JsonArray periodeList = body.getJsonArray("result");
                                donnees.setPeriodes(objectFactory.createDonneesPeriodes());
                                periodeList.forEach(item -> {
                                    JsonObject currentPeriode = (JsonObject) item;
                                    Integer targetPeriode = wantedPeriodes.stream()
                                            .filter(el -> el == currentPeriode.getInteger("id_type"))
                                            .findFirst()
                                            .orElse(null);
                                    if (targetPeriode != null) {
                           /* periodesAdded.put("P_"+currentPeriode.getInteger("id").toString(), currentPeriode);
                            Periode periode = objectFactory.createPeriode();
                            periode.setId("P_"+currentPeriode.getInteger("id").toString());
                            periode.setMillesime(currentPeriode.getString("timestamp_dt").substring(0, 4));
                            periode.setIndice(currentPeriode.getInteger("id_type"));
                            periode.setNbPeriodes(periodeList.size());
                            donnees.getPeriodes().getPeriode().add(periode);*/

                                        String millesime = currentPeriode.getString("timestamp_dt").substring(0, 4);
                                        Integer indice = new Integer(0);
                                        Integer nbPeriode = new Integer(0);
                                        if (currentPeriode.getInteger("id_type") == 3 ||
                                                currentPeriode.getInteger("id_type") == 4 ||
                                                currentPeriode.getInteger("id_type") == 5) {
                                            indice = currentPeriode.getInteger("id_type") - 2;
                                            nbPeriode = 3;
                                        } else {
                                            indice = currentPeriode.getInteger("id_type");
                                            nbPeriode = 2;
                                        }

                                        Periode periode = donnees.getPeriodes().getOnePeriode(millesime, indice, nbPeriode);

                                        if (periode == null) {
                                            periode = objectFactory.createPeriode();
                                            periode.setId("P_" + currentPeriode.getInteger("id").toString());
                                            periode.setMillesime(millesime);
                                            periode.setTypePeriode(currentPeriode.getInteger("id_type"));
                                            periode.setNbPeriodes(nbPeriode);
                                            periode.setIndice(indice);
                                            donnees.getPeriodes().getPeriode().add(periode);
                                        }

                                        //set map periodesByClasse
                                        if (periodesByClass != null && periodesByClass.containsKey(currentPeriode.getString("id_classe"))) {
                                            JsonArray periodes = periodesByClass.get(currentPeriode.getString("id_classe"));
                                            periodes.add(currentPeriode);
                                        } else {
                                            periodesByClass.put(currentPeriode.getString("id_classe"), new JsonArray().add(currentPeriode));
                                        }
                                    }
                                });
                                // log for time-out
                                answer.set(true);
                                lsuService.serviceResponseOK(answer, count.get(), thread, method);
                                handler.handle("success");
                            } catch (Throwable e) {
                                handler.handle("method getBalisePeriodes : " + e.getMessage());
                                log.error("method getBalisePeriodes : " + e.getMessage());
                            }
                        } else {
                            String error = body.getString(MESSAGE);
                            lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                            if (error!=null && error.contains(TIME)) {
                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                        handlerToAsyncHandler(this));
                            }
                            else {
                                log.error("method getBalisePeriodes : error eb periode.getPeriodes ko");
                                handler.handle("getBalisePeriodes : error eb periode.getPeriodes ko");
                            }
                        }
                    }}));
    }

    private void getBaliseEnseignants(final Donnees donnees, final String structureId, List<String> idsClasse,
                                      JsonArray enseignantFromSts, final Handler<String> handler) {
        final List<Future> futureMyResponse1Lst = new ArrayList<>();
        for (int i = 0; i < idsClasse.size(); i++) {
            Future<JsonObject> resp1FutureComposite = Future.future();
            futureMyResponse1Lst.add(resp1FutureComposite);
            JsonArray types = new JsonArray().add("Teacher");
            JsonObject action = new JsonObject()
                    .put("action", "user.getUsersByTypeClassAndStructure")
                    .put("structureId", structureId)
                    .put("classId", idsClasse.get(i))
                    .put("types", types);
            eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                    handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                        AtomicBoolean answer = new AtomicBoolean(false);
                        AtomicInteger count = new AtomicInteger(0);
                        final String thread = "("  + structureId + ", "+ idsClasse.toString()+ " )";
                        final String method = "getBaliseEnseignants";
                        @Override
                        public void handle(Message<JsonObject> message) {
                            JsonObject body = message.body();
                            if ("ok".equals(body.getString("status"))) {
                                JsonArray teachersList = body.getJsonArray("results");
                                for (Integer k = 0; k < teachersList.size(); k++) {
                                    addorFindTeacherBalise(donnees, enseignantFromSts, teachersList.getJsonObject(k));
                                }
                                // log for time-out
                                answer.set(true);
                                lsuService.serviceResponseOK(answer, count.get(), thread, method);
                                resp1FutureComposite.complete();
                            }else{
                                String error = body.getString(MESSAGE);
                                if (error!=null && error.contains(TIME)) {
                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                            handlerToAsyncHandler(this));
                                }
                                else {
                                    // log for time-out
                                    answer.set(true);
                                    resp1FutureComposite.complete();
                                }
                                lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                            }
                        }
                    }));
        }
        CompositeFuture.all(futureMyResponse1Lst).setHandler(
                event -> handler.handle("success"));
    }

    private void getHeadTeachers( List<String> idsClasse,
                                  Map<String, JsonArray> mapIdClasseHeadTeachers,
                                  final Handler<String> handler){
        List<Future> listFutureClasses = new ArrayList<Future>();
        for(String idClass : idsClasse){
            Future futureClass = Future.future();
            listFutureClasses.add(futureClass);
            JsonObject action = new JsonObject();
            action.put("action","classe.getHeadTeachersClasse").put("idClasse", idClass);
            eb.send(Competences.VIESCO_BUS_ADDRESS,action,Competences.DELIVERY_OPTIONS, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                AtomicBoolean answer = new AtomicBoolean(false);
                AtomicInteger count = new AtomicInteger(0);
                final String thread = "( "  + idClass + " )";
                final String method = "getHeadTeachers";

                @Override
                public void handle(Message<JsonObject> message) {

                    JsonObject body = message.body();
                    if(!"ok".equals(body.getString("status"))){
                        String error = body.getString(MESSAGE);
                        if (error!=null && error.contains(TIME)) {
                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                    handlerToAsyncHandler(this));
                        }
                        else {
                            // log for time-out
                            answer.set(true);
                            futureClass.complete();
                        }
                        lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);

                    }else{
                        JsonArray headTeachers = body.getJsonArray("results");
                        mapIdClasseHeadTeachers.put(idClass,headTeachers);
                        // log for time-out
                        answer.set(true);
                        lsuService.serviceResponseOK(answer, count.get(), thread, method);
                        futureClass.complete();

                    }

                }
            }));


        }
        CompositeFuture.all(listFutureClasses).setHandler(
                event -> handler.handle("success"));

    }

    private void getTableConversion(String idStructure, List<String> idsClasses,
                                    final Map<String, JsonArray> tableConversionByClass,final Handler<String> handler){
        competenceNoteService.getConversionTableByClass(idStructure, idsClasses, true,
                new Handler<Either<String, JsonArray>>() {
                    AtomicBoolean answer = new AtomicBoolean(false);
                    AtomicInteger count = new AtomicInteger(0);
                    final String thread = "(" + idStructure + ", " + idsClasses.toString() + " )";
                    final String method = "getTableConversion";

                    @Override
                    public void handle(Either<String, JsonArray> responseTableConversion) {
                        if (responseTableConversion.isRight()) {
                            answer.set(true);
                            JsonArray allTablesConversion = responseTableConversion.right().getValue();
                            List<Future> listFutureTable = new ArrayList<Future>();


                            for (int i = 0; i < allTablesConversion.size(); i++) {

                                Future<JsonObject> futureTable = Future.future();
                                listFutureTable.add(futureTable);

                                JsonObject tableConversion = allTablesConversion.getJsonObject(i);
                                if (tableConversionByClass != null && !tableConversionByClass.isEmpty() &&
                                        !tableConversionByClass.containsKey(tableConversion.getString("id_groupe"))) {
                                    tableConversionByClass.put(tableConversion.getString("id_groupe"),
                                            new JsonArray(tableConversion.getString("table_conversion")));

                                } else {
                                    tableConversionByClass.put(tableConversion.getString("id_groupe"),
                                            new JsonArray(tableConversion.getString("table_conversion")));
                                }
                                futureTable.complete();
                            }
                            lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                            CompositeFuture.all(listFutureTable).setHandler(event -> {
                                handler.handle("success");
                            });

                        } else {
                            String error = responseTableConversion.left().getValue();
                            if (error != null && error.contains(TIME)) {
                                competenceNoteService.getConversionTableByClass(idStructure, idsClasses,
                                        true, this);
                            } else {
                                answer.set(true);
                                log.info("event is not Right getTableConversion ");
                                handler.handle("getTableConversion no data available ");

                            }
                            lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                        }
                    }
                });
    }


    private void getBaliseEnseignantFromId(final Donnees donnees, String idEnseignant,final JsonArray enseignantFromSts, final Handler<String> handler) {
        JsonArray ids = new JsonArray().add(idEnseignant);
        JsonObject action = new JsonObject()
                .put("action", "user.getUsers")
                .put("idUsers", ids);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    AtomicBoolean answer = new AtomicBoolean(false);
                    AtomicInteger count = new AtomicInteger(0);
                    final String thread = action.encode();
                    final String method = "getBaliseEnseignantFromId";
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        if ("ok".equals(body.getString("status")) && body.getJsonArray("results").size() == 1 ) {
                            JsonArray teachersList = body.getJsonArray("results");
                            for (Integer k = 0; k < teachersList.size(); k++) {
                                addorFindTeacherBalise(donnees, enseignantFromSts, teachersList.getJsonObject(k));
                            }
                            handler.handle("success");
                        }
                        else{
                            String error = message.body().getString(MESSAGE);
                            if(error != null && error.contains(TIME)){
                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                        handlerToAsyncHandler(this));
                            }
                            else{
                                answer.set(true);
                                handler.handle(error);
                            }
                            lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                        }
                    }
                }));
    }

    private Enseignant addorFindTeacherBalise(Donnees donnees, final JsonArray enseignantsFromSts, JsonObject enseignant){
        Enseignant existing = getEnseignantInXML(enseignant.getString("id"), donnees);
        if (existing == null) {
            existing = objectFactory.createEnseignant();
            existing.setId("ENS_" + enseignant.getString("id"));
            existing.setPrenom(enseignant.getString("firstName"));
            if(enseignant.getString("lastName") != null){
                existing.setNom(enseignant.getString("lastName"));
            }
            else if(enseignant.getString("name") != null) {
                existing.setNom(enseignant.getString("name"));
            }
            existing.setType(fromValue("autre"));
            existing.setIdSts(new BigInteger(20, new Random()));

            if(enseignantsFromSts != null && enseignantsFromSts.size() > 0) {
                JsonObject enseigantFromSts = (JsonObject) enseignantsFromSts.stream()
                        .filter(
                                el -> (((JsonObject) el).getString("NOM_USAGE") != null &&
                                        ((JsonObject) el).getString("NOM_USAGE").equals(enseignant.getString("lastName")) ||
                                        ((JsonObject) el).getString("NOM_USAGE").equals(enseignant.getString("name")))
                                        && ((JsonObject) el).getString("PRENOM")!=null &&
                                        ((JsonObject) el).getString("PRENOM").equals(enseignant.getString("firstName"))
                                        && ( ((JsonObject) el).getString("DATE_NAISSANCE")== null ||
                                        ((JsonObject) el).getString("DATE_NAISSANCE")!=null &&
                                                ((JsonObject) el).getString("DATE_NAISSANCE").equals(enseignant.getString("birthDate")))
                        )
                        .findFirst()
                        .orElse(null);
                if (enseigantFromSts != null && enseigantFromSts.containsKey("ID") && enseigantFromSts.containsKey("TYPE")) {
                    existing.setIdSts(new BigInteger(enseigantFromSts.getString("ID")));
                    existing.setType(TypeEnseignant.fromValue(enseigantFromSts.getString("TYPE")));
                }
            }

            if(existing.getId()!= null && existing.getIdSts() != null && existing.getIdSts() != null){
                if(donnees.getEnseignants() == null){
                  donnees.setEnseignants(objectFactory.createDonneesEnseignants());
                }
                donnees.getEnseignants().getEnseignant().add(existing);
            }
        }
        return existing;
    };
    private void getApEpiParcoursBalises(final Donnees donnees, final List<String> idsClass, final String idStructure,
                                         JsonObject epiGroupAdded, final JsonArray enseignantFromSts,
                                         final Handler<String> handler) {
        elementBilanPeriodiqueService.getElementsBilanPeriodique(null, idsClass, idStructure,
                new Handler<Either<String, JsonArray>>() {
                    AtomicBoolean answer = new AtomicBoolean(false);
                    AtomicInteger count = new AtomicInteger(0);
                    final String thread = "(" + idStructure + ", " + idsClass.toString() + ")";
                    final String method = "getApEpiParcoursBalises";
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            JsonArray elementBilanPeriodique = event.right().getValue();
                            if (elementBilanPeriodique == null || elementBilanPeriodique.isEmpty()) {
                                answer.set(true);
                                handler.handle("success");
                                log.info(" getElementsBilanPeriodique in getApEpiParcoursBalises");
                            }
                            else {
                                int imax = elementBilanPeriodique.size();
                                final List<Future> futuresListApEpiParcours = new ArrayList<>();

                                for (int i = 0; i < imax; i++) {
                                    final Future<JsonObject> futureEltBilanPeriodique = Future.future();
                                    futuresListApEpiParcours.add(futureEltBilanPeriodique);
                                    JsonObject element = elementBilanPeriodique.getJsonObject(i);
                                    if (element != null) {
                                        Long typeElement = element.getLong("type");
                                        if (3L == typeElement) { //parcours group
                                            addParcoursGroup(element, futureEltBilanPeriodique);
                                        } else if (2L == typeElement) { //ap class/group
                                            addAccGroup(element, futureEltBilanPeriodique);
                                        } else if (1L == typeElement) { //epi group
                                            addEpiGroup(element, epiGroupAdded, futureEltBilanPeriodique);
                                        }
                            }
                        }
                        CompositeFuture.all(futuresListApEpiParcours).setHandler(eventFutureApEpiParcours -> {
                            handler.handle("success");
                        });
                    }
                }
                else {
                    String error = event.left().getValue();
                            if(error!=null && error.contains(TIME)){
                                elementBilanPeriodiqueService.getElementsBilanPeriodique(null, idsClass,
                                        idStructure, this);
                            }
                            else {
                                answer.set(true);
                                handler.handle("getApEpiParcoursBalises no data available ");
                                log.info("event is not Right getElementsBilanPeriodique in getApEpiParcoursBalises");
                            }
                            lsuService.serviceResponseOK(answer,count.incrementAndGet(), thread, method);
                        }
                    }

                    private void addParcoursGroup(JsonObject element,Future futureEltBilanPeriodique) {
                        if(donnees.getParcoursCommuns() == null){
                            donnees.setParcoursCommuns(objectFactory.createDonneesParcoursCommuns());
                        }
                        List<Periode> periodes = donnees.getPeriodes().getPeriode();

                        int kmax = periodes.size();
                        for (int k = 0; k < kmax; k++) {
                            JsonObject theme = element.getJsonObject("theme");
                            JsonArray groups = element.getJsonArray("groupes");
                            Periode currentPeriode = periodes.get(k);
                            for( int i= 0; i < groups.size(); i++ ) {
                                JsonObject group = groups.getJsonObject(i);
                                Donnees.ParcoursCommuns.ParcoursCommun parcoursCommun ;
                                if(donnees.getParcoursCommuns().getParcoursCommunInList(currentPeriode, group.getString("name")) != null){
                                    parcoursCommun = donnees.getParcoursCommuns().getParcoursCommunInList(currentPeriode, group.getString("name"));
                                }else{
                                    parcoursCommun = objectFactory.createDonneesParcoursCommunsParcoursCommun();
                                    parcoursCommun.setPeriodeRef(currentPeriode);
                                    parcoursCommun.setCodeDivision(group.getString("name"));
                                    donnees.getParcoursCommuns().getParcoursCommun().add(parcoursCommun);
                                }
                                Parcours parcours = objectFactory.createParcours();
                                parcours.setCode(CodeParcours.fromValue(theme.getString("code")));
                                parcours.setValue(theme.getString("libelle"));
                                parcoursCommun.getParcours().add(parcours);
                            }
                        }
                        futureEltBilanPeriodique.complete();
                    }
                    private void addEpiGroup(JsonObject element, JsonObject epiGroupAdded, Future futureEltBilanPeriodique) {
                        if (element != null
                                && !element.isEmpty()
                                && element.containsKey("id")
                                && element.containsKey("libelle")
                                && element.containsKey("description")
                                && element.containsKey("theme")
                                && element.containsKey("groupes")
                                && element.containsKey("intervenantsMatieres")
                                && element.getJsonArray("intervenantsMatieres").size() > 0
                                && element.getJsonArray("groupes").size() > 0) {


                            Epi epi = objectFactory.createEpi();
                            EpiThematique epiThematique = objectFactory.createEpiThematique();;
                            EpiGroupe epiGroupe = objectFactory.createEpiGroupe();
                            JsonObject theme = element.getJsonObject("theme");

                            epi.setId("EPI_" + theme.getInteger("id"));
                            epi.setIntitule(theme.getString("libelle"));
                            epi.setDescription(element.getString("description"));

                            if (ThematiqueEpi.contains(theme.getString("code"))) {
                                epi.setThematique(theme.getString("code"));
                            } else {
                                epiThematique.setCode(theme.getString("code"));
                                epiThematique.setLibelle(theme.getString("libelle"));
                                epi.setThematique(epiThematique.getCode());
                            }

                            EpiGroupe.EnseignantsDisciplines enseignantsDisciplinesEpi = objectFactory.createEpiGroupeEnseignantsDisciplines();

                            JsonArray intervenantsMatieres = element.getJsonArray("intervenantsMatieres");
                            int jmax = intervenantsMatieres.size();
                            final List<Future> futureMyResponse1Lst = new ArrayList<>();

                            for (int j = 0; j < jmax; j++) {
                                final Future<JsonObject> resp1FutureComposite = Future.future();
                                futureMyResponse1Lst.add(resp1FutureComposite);
                                addEnseignantDiscipline(intervenantsMatieres.getJsonObject(j),
                                        enseignantsDisciplinesEpi.getEnseignantDiscipline(), epi.getDisciplineRefs(),
                                        donnees,enseignantFromSts, resp1FutureComposite);
                            }

                            CompositeFuture.all(futureMyResponse1Lst).setHandler(event -> {
                                epiGroupe.setId(EPI_GROUPE + element.getInteger("id"));
                                epiGroupe.setEpiRef(epi);
                                epiGroupe.setEnseignantsDisciplines(enseignantsDisciplinesEpi);
                                epiGroupe.setIntitule(element.getString("libelle"));

                                for (int i = 0; i < element.getJsonArray("groupes").size(); i++) {
                                    JsonObject currentClass = element.getJsonArray("groupes").getJsonObject(i);
                                    epiGroupAdded.put(currentClass.getString("id"), epiGroupe.getId());
                                }
                                if(epiThematique.getCode() != null){
                                    if(donnees.getEpisThematiques() == null){
                                        donnees.setEpisThematiques(objectFactory.createEpisThematiques());
                                    }
                                    donnees.getEpisThematiques().getEpiThematique().add(epiThematique);
                                }
                                if(donnees.getEpis() == null ){
                                    donnees.setEpis(objectFactory.createDonneesEpis());
                                }
                                if(donnees.getEpisGroupes() == null ){
                                    donnees.setEpisGroupes(objectFactory.createDonneesEpisGroupes());
                                }

                                donnees.getEpis().getEpi().add(epi);
                                donnees.getEpisGroupes().getEpiGroupe().add(epiGroupe);
                                futureEltBilanPeriodique.complete();

                            });
                        }
                    }

                    // private void addAccGroup(JsonObject element, JsonObject accGroupAdded, Future futureEltBilanPeriodique)
                    private void addAccGroup(JsonObject element, Future futureEltBilanPeriodique) {
                        if (element != null
                                && !element.isEmpty()
                                && element.containsKey("id")
                                && element.containsKey("libelle")
                                && element.containsKey("description")
                                && element.containsKey("groupes")
                                && element.containsKey("intervenantsMatieres")
                                && element.getJsonArray("intervenantsMatieres").size() > 0
                                && element.getJsonArray("groupes").size() > 0) {
                            donnees.setAccPersosGroupes(objectFactory.createDonneesAccPersosGroupes());
                            donnees.setAccPersos(objectFactory.createDonneesAccPersos());
                            AccPerso accPerso = objectFactory.createAccPerso();
                            AccPersoGroupe accPersoGroupe = objectFactory.createAccPersoGroupe();
                            JsonObject theme = element.getJsonObject("theme");

                            accPerso.setId("ACC_PERSO_" + element.getInteger("id"));
                            accPerso.setIntitule(element.getString("libelle"));
                            accPerso.setDescription(element.getString("description"));

                            AccPersoGroupe.EnseignantsDisciplines enseignantsDisciplinesAcc = objectFactory.createAccPersoGroupeEnseignantsDisciplines();

                            JsonArray intervenantsMatieres = element.getJsonArray("intervenantsMatieres");
                            int imax = intervenantsMatieres.size();
                            final List<Future> futureMyResponse1Lst = new ArrayList<>();

                            for (int i = 0; i < imax; i++) {
                                final Future<JsonObject> resp1FutureComposite = Future.future();
                                futureMyResponse1Lst.add(resp1FutureComposite);
                                addEnseignantDiscipline(intervenantsMatieres.getJsonObject(i),
                                        enseignantsDisciplinesAcc.getEnseignantDiscipline(), accPerso.getDisciplineRefs(),
                                        donnees,enseignantFromSts, resp1FutureComposite);
                            }

                            CompositeFuture.all(futureMyResponse1Lst).setHandler(event -> {
                                accPersoGroupe.setId(ACC_GROUPE + element.getInteger("id"));
                                accPersoGroupe.setAccPersoRef(accPerso);
                                accPersoGroupe.setEnseignantsDisciplines(enseignantsDisciplinesAcc);
                                accPersoGroupe.setIntitule(element.getString("libelle"));

                                for (int i = 0; i < element.getJsonArray("groupes").size(); i++) {
                                    JsonObject currentClass = element.getJsonArray("groupes").getJsonObject(i);
                                    // accGroupAdded.put(currentClass.getString("id"), accPersoGroupe.getId());
                                }

                                donnees.getAccPersos().getAccPerso().add(accPerso);
                                donnees.getAccPersosGroupes().getAccPersoGroupe().add(accPersoGroupe);
                                futureEltBilanPeriodique.complete();
                            });
                        }
                    }

                });
    }

    private void addEnseignantDiscipline(JsonObject currentIntervenantMatiere, List<EnseignantDiscipline> enseignantDiscipline, List<Object> disciplineRefs, final Donnees donnees,final JsonArray enseignantFromSts, final Future<JsonObject> resp1FutureComposite) {
        Discipline currentSubj = getDisciplineInXML(currentIntervenantMatiere.getJsonObject("matiere").getString("id"), donnees);
        if (currentSubj != null) {
            Enseignant currentEnseignant = getEnseignantInXML(currentIntervenantMatiere.getJsonObject("intervenant").getString("id"), donnees);
            if (currentEnseignant == null) {
                getBaliseEnseignantFromId(donnees, currentIntervenantMatiere.getJsonObject("intervenant").getString("id"), enseignantFromSts,
                        (String event) -> {
                            if (event.equals("success")) {
                                Enseignant newEnseignant = getEnseignantInXML(currentIntervenantMatiere.getJsonObject("intervenant").getString("id"), donnees);
                                finalInsertAddEnseignantDiscipline(enseignantDiscipline, disciplineRefs, resp1FutureComposite, currentSubj, newEnseignant);
                            }
                        });
            }else {
                finalInsertAddEnseignantDiscipline(enseignantDiscipline, disciplineRefs, resp1FutureComposite, currentSubj, currentEnseignant);
            }
            lsuService.addIdsEvaluatedDiscipline(currentSubj.getId().replaceAll(DISCIPLINE_KEY, ""));
        } else {
            log.info("addEnseignantDiscipline no completed");
            resp1FutureComposite.complete();
        }
    }

    private void finalInsertAddEnseignantDiscipline(List<EnseignantDiscipline> enseignantDiscipline, List<Object> disciplineRefs, final Future<JsonObject> resp1FutureComposite, Discipline currentSubj, Enseignant currentEnseignant) {
        if (currentEnseignant != null) {
            EnseignantDiscipline currentEnseignantDiscipline = objectFactory.createEnseignantDiscipline();
            currentEnseignantDiscipline.setDisciplineRef(currentSubj);
            currentEnseignantDiscipline.setEnseignantRef(currentEnseignant);
            enseignantDiscipline.add(currentEnseignantDiscipline);
            disciplineRefs.add(currentSubj);
            resp1FutureComposite.complete();
        } else {
            resp1FutureComposite.complete();
        }
    }


    /**
     * permet de completer tous les attributs de la balise BilansPeriodiques et de la setter à donnees
     * @param donnees     permet de recuperer les eleves
     * @param periodesByClasse
     * @param epiGroupAdded
     * @param tableConversionByClasse
     * @param handler
     */

    private void getBaliseBilansPeriodiques(final Donnees donnees, final String idStructure,
                                            final Map<String, JsonArray> periodesByClasse,
                                            final JsonObject epiGroupAdded, final Map<String, JsonArray> tableConversionByClasse,
                                            final JsonArray enseignantFromSts, final  Map<String, JsonArray> mapIdClassHeadTeachers,
                                            final Handler<Either.Right<String, JsonObject>> handler) {
        final Donnees.BilansPeriodiques bilansPeriodiques = objectFactory.createDonneesBilansPeriodiques();
        final List<ResponsableEtab> responsablesEtab = donnees.getResponsablesEtab().getResponsableEtab();
        final Donnees.Eleves eleves = donnees.getEleves();
        final Donnees.Periodes periodes = donnees.getPeriodes();
        final Integer nbElevesTotal = eleves.getEleve().size();
        final AtomicInteger nbEleveCompteur = new AtomicInteger(0);
        final Map<String, List<String>> mapIdClassIdsEleve = eleves.getMapIdClassIdsEleve();
        errorsExport = new JsonObject();
        final AtomicInteger originalSize = new AtomicInteger();
        final AtomicInteger idElementProgramme = new AtomicInteger();



        Handler getOut = new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> suiviAcquisResponse) {
                originalSize.getAndDecrement();
                if (originalSize.get() == 0) {
                    log.info("Get OUTTTTT " + bilansPeriodiques.getBilanPeriodique().size() + "  ==  "
                            + eleves.getEleve().size());
                    donnees.setBilansPeriodiques(bilansPeriodiques);
                    handler.handle(new Either.Right<String, JsonObject>(suiviAcquisResponse.right().getValue()));
                } else {
                    //log.info("waiting all child done");
                }
            }
        };

        if( !(eleves.getEleve().size() > 0) || !(periodes.getPeriode().size() > 0)){
            handler.handle(new Either.Right<String, JsonObject>(new JsonObject().put("error",  "getBaliseBilansPeriodiques : Eleves or Periodes are empty")));
            return;
        }

        donnees.setElementsProgramme(objectFactory.createDonneesElementsProgramme());
        //For each eleve create his periodic bilan
        for (Integer i = 0; i < eleves.getEleve().size(); i++) {
            Eleve currentEleve = eleves.getEleve().get(i);
           JsonArray headTeachers = mapIdClassHeadTeachers.get(currentEleve.getId_Class());

            for (Integer i2 = 0; i2 < periodes.getPeriode().size(); i2++) {
                originalSize.getAndIncrement();
                JsonObject response = new JsonObject();
                Periode currentPeriode = periodes.getPeriode().get(i2);
                /*if(periodesAdded.containsKey(currentPeriode.getId())
                        && periodesAdded.getJsonObject(currentPeriode.getId()).containsKey("id_classe")
                        && currentEleve.getId_Class().equals(periodesAdded.getJsonObject(currentPeriode.getId()).getString("id_classe"))) {
                    JsonObject periodeJSon = periodesAdded.getJsonObject(currentPeriode.getId());*/
                Future<JsonObject> getRetardsAndAbsencesFuture = Future.future();
                Future<JsonObject> getAppreciationsFuture = Future.future();
                Future<JsonObject> getSuiviAcquisFuture = Future.future();
                Future<JsonObject> getSyntheseFuture = Future.future();
                final BilanPeriodique bilanPeriodique = objectFactory.createBilanPeriodique();;

                CompositeFuture.all(getRetardsAndAbsencesFuture, getAppreciationsFuture,
                        getSuiviAcquisFuture, getSyntheseFuture).setHandler(
                        event -> {
                            if(event.succeeded()){
                                response.put("status", 200);
                                getOut.handle(new Either.Right<String, JsonObject>(response));
                            }

                        });


                if (headTeachers != null && headTeachers.size() > 0) {
                    for(int k = 0; k < headTeachers.size(); k++){
                        Enseignant headTeacher = addorFindTeacherBalise(donnees,enseignantFromSts,headTeachers.getJsonObject(k));
                        bilanPeriodique.getProfPrincRefs().add(headTeacher);
                    }
                }


               if(!addresponsableEtabRef(donnees, response, bilanPeriodique)){
                    response.put("status", 400);
                    response.put(MESSAGE, "Responsable do not exist on Bilan periodique balise : eleve : "+
                            currentEleve.getIdNeo4j() + " " +currentEleve.getNom() + " periode : "+ currentPeriode.getId() );
                    getOut.handle(new Either.Right<String, JsonObject>(response));
                    return;
                }

               if(!addDatesBilanPeriodique(bilanPeriodique, periodesByClasse, currentEleve, currentPeriode)){
                   /*response.put("status", 400);
                    response.put(MESSAGE, "Periode do not exit for this class "+ currentEleve.getCodeDivision() +
                            "id : "+currentEleve.getId_Class());
                    getOut.handle(new Either.Right<String, JsonObject>(response));
                    return;*/
                   setError(errorsExport,currentEleve,"Pas de dates pour cette classe");
               }

                syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique((long) currentPeriode.getTypePeriode(),
                        currentEleve.getIdNeo4j(), new Handler<Either<String, JsonObject>>() {
                            AtomicBoolean answer = new AtomicBoolean(false);
                            AtomicInteger count = new AtomicInteger(0);
                            final String thread = "(" + currentEleve.getNom() + " " + currentEleve.getPrenom() +" )";
                            final String method = "getBaliseBilansPeriodiques |getSyntheseBilanPeriodique ";
                            @Override
                            public void handle(Either<String, JsonObject> eventSynthese) {

                                String synthese = "null";
                                if (eventSynthese.isRight()) {
                                    final JsonObject rightValue = eventSynthese.right().getValue();
                                    if ((rightValue != null) && rightValue.containsKey("synthese")
                                            && !rightValue.getString("synthese").isEmpty()) {
                                        synthese = rightValue.getString("synthese");
                                    }
                                    answer.set(true);
                                } else{
                                    setError(errorsExport, currentEleve,
                                            getLibelle("evaluation.lsu.error.no.synthese") +
                                                    currentPeriode.getLabel());
                                    String error = eventSynthese.left().getValue();
                                    if(error != null && error.contains(TIME)){

                                        if(getSyntheseFuture.isComplete()){
                                            return;
                                        }
                                        syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(
                                                (long) currentPeriode.getTypePeriode(),
                                                currentEleve.getIdNeo4j(),this);
                                    }
                                    else {
                                        answer.set(true);
                                    }
                                }
                                lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                                if (answer.get()) {
                                    if(!"null".equals(synthese)){
                                        bilanPeriodique.setAcquisConseils(synthese);
                                    }else{
                                        setError(errorsExport, currentEleve,
                                                getLibelle("evaluation.lsu.error.no.synthese") +
                                                currentPeriode.getLabel());
                                    }
                                    getSyntheseFuture.complete();
                                }
                            }});


                bilanPeriodiqueService.getRetardsAndAbsences(currentEleve.getIdNeo4j(),
                        new Handler<Either<String, JsonArray>>() {
                            AtomicBoolean answer = new AtomicBoolean(false);
                            AtomicInteger count = new AtomicInteger(0);
                            final String thread = "(" + currentEleve.getNom() + " " + currentEleve.getPrenom() +" )";
                            final String method = "getBaliseBilansPeriodiques | getRetardsAndAbsences ";
                    @Override
                    public void handle(Either<String, JsonArray> eventViesco) {
                        if(eventViesco.isLeft()) {
                            String error = eventViesco.left().getValue();
                            if(error != null && error.contains(TIME)){
                                if(!getRetardsAndAbsencesFuture.isComplete()) {
                                    bilanPeriodiqueService.getRetardsAndAbsences(currentEleve.getIdNeo4j(),
                                            this);
                                }
                                else {
                                    return;
                                }
                            }
                            else {
                                answer.set(true);
                            }
                        }
                        else {
                            answer.set(true);
                            if(!getRetardsAndAbsencesFuture.isComplete()) {
                                addVieScolairePerso(eventViesco, bilanPeriodique);
                                getRetardsAndAbsencesFuture.complete();
                            }
                            else {
                                return;
                            }
                        }
                        lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                    }

                    private void addVieScolairePerso(Either<String, JsonArray> eventViesco, BilanPeriodique bilanPeriodique) {
                        if (eventViesco.isRight()) {
                            VieScolaire viescolare = objectFactory.createVieScolaire();
                            viescolare.setNbAbsInjustifiees(BigInteger.valueOf(0L));
                            viescolare.setNbAbsJustifiees(BigInteger.valueOf(0L));
                            viescolare.setNbRetards(BigInteger.valueOf(0L));
                            viescolare.setNbHeuresManquees(BigInteger.valueOf(0L));

                            final JsonArray vieScoRightValue = eventViesco.right().getValue();
                                /*JsonObject viesco = (JsonObject) vieScoRightValue.stream()
                                        .filter(el -> periodeJSon.getInteger("id_type").equals(((JsonObject) el).getInteger("id_periode")))
                                        .findFirst()
                                        .orElse(null);*/
                            JsonObject viesco = (JsonObject) vieScoRightValue.stream()
                                    //.filter(el -> periodeJSon.getInteger("id_type").equals(((JsonObject) el).getInteger("id_periode")))
                                    .filter(el -> currentPeriode.getTypePeriode()==((JsonObject) el).getInteger("id_periode"))
                                    .findFirst()
                                    .orElse(null);

                                if (viesco != null && viesco.containsKey("id_eleve")) {
                                    if (viesco.getLong("abs_non_just") != null) {
                                        viescolare.setNbAbsInjustifiees(BigInteger.valueOf(viesco.getLong("abs_non_just")));
                                    }
                                    if (viesco.getLong("abs_just") != null) {
                                        viescolare.setNbAbsJustifiees(BigInteger.valueOf(viesco.getLong("abs_just")));
                                    }
                                    if (viesco.getLong("retard") != null) {
                                        viescolare.setNbRetards(BigInteger.valueOf(viesco.getLong("retard")));
                                    }
                                    if (viesco.getLong("abs_totale_heure") != null) {
                                        viescolare.setNbHeuresManquees(BigInteger.valueOf(viesco.getLong("abs_totale_heure")));
                                    }
                                }
                            bilanPeriodique.setVieScolaire(viescolare);
                        }
                        else {
                                response.put("status", 400);
                                response.put(MESSAGE, "VieScolaire, leftResponse bilanPeriodiqueService.getRetardsAndAbsences  : " +
                                    currentEleve.getIdNeo4j() + " " + currentEleve.getNom() + " periode : " + currentPeriode.getId());
                            log.error("getBaliseBilansPeriodiques : bilanPeriodiqueService.getRetardsAndAbsences  " + eventViesco);
                        }
                    }
                });

                    elementBilanPeriodiqueService.getApprecBilanPerEleve(Collections.singletonList(currentEleve.getId_Class()),
                            new Integer (currentPeriode.getTypePeriode()).toString(), null, currentEleve.getIdNeo4j(),
                            new Handler<Either<String, JsonArray>>() {
                            AtomicBoolean answer = new AtomicBoolean(false);
                            AtomicInteger count = new AtomicInteger(0);
                            final String thread = "(" + currentEleve.getNom() + " " + currentEleve.getPrenom() +" )";
                            final String method = "getBaliseBilansPeriodiques | getApprecBilanPerEleve ";
                            @Override
                            public void handle(Either<String, JsonArray> eventApp) {

                                    if (eventApp.isRight()) {
                                        final JsonArray appreciations = eventApp.right().getValue();

                                    for (int i = 0; i < appreciations.size(); i++) {
                                        JsonObject element = appreciations.getJsonObject(i);
                                        // if (element.getInteger("id_periode") == periodeJSon.getInteger("id_type")) {
                                        if (element.getInteger("id_periode") == currentPeriode.getTypePeriode()) {
                                            Long typeElem = element.getLong("type_elt_bilan_periodique");
                                            if (3L == typeElem) {//parcours
                                                addParcoursEleve(element);
                                            } else if (1L == typeElem) {
                                                addEpiEleve(element);
                                            } else if (2L == typeElem) {//ap
                                                addApEleve(element);
                                            }
                                        }
                                    }
                                    answer.set(true);
                                }
                                else {
                                    String error = eventApp.left().getValue();
                                    if(error != null && error.contains(TIME)){
                                        elementBilanPeriodiqueService.getApprecBilanPerEleve(
                                                Collections.singletonList(currentEleve.getId_Class()),
                                                new Integer (currentPeriode.getTypePeriode()).toString(),
                                                null, currentEleve.getIdNeo4j(), this);
                                    }
                                    else {
                                        answer.set(true);
                                    }
                                }
                                lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                                if(answer.get()) {
                                    getAppreciationsFuture.complete();
                                }
                            }

                            private void addApEleve(JsonObject element) {
                                   /* String extGroup = element.getString("id_groupe");
                                    if (accGroupAdded.size() > 0 && extGroup != null && accGroupAdded.containsKey(extGroup)) {
                                        AccPersoGroupe accGroupe = donnees.getAccPersosGroupes().getAccPersoGroupe().stream()
                                                .filter(el -> el.getId().equals(accGroupAdded.getString(extGroup)))
                                                .findFirst()
                                                .orElse(null);*/
                                List<AccPersoGroupe> listAccPersoGroupe = donnees.getAccPersosGroupes().getAccPersoGroupe();
                                if(listAccPersoGroupe != null && listAccPersoGroupe.size() > 0){
                                    AccPersoGroupe accGroupe = listAccPersoGroupe.stream()
                                            .filter( accG -> accG.getId().equals(ACC_GROUPE + element.getInteger("id_elt_bilan_periodique").toString()))
                                            .findFirst()
                                            .orElse(null);

                                    if (accGroupe != null) {
                                        AccPersoEleve accEleve = objectFactory.createAccPersoEleve();
                                        accEleve.setCommentaire(element.getString("commentaire"));
                                        accEleve.setAccPersoGroupeRef(accGroupe);
                                        if (bilanPeriodique.getAccPersosEleve() == null || bilanPeriodique.getAccPersosEleve().getAccPersoEleve() == null) {
                                            bilanPeriodique.setAccPersosEleve(objectFactory.createBilanPeriodiqueAccPersosEleve());
                                        }
                                        bilanPeriodique.getAccPersosEleve().getAccPersoEleve().add(accEleve);
                                    }
                                }
                            }

                            private void addEpiEleve(JsonObject element) {
                                   /* String extGroup = element.getString("id_groupe");
                                    if (epiGroupAdded.size() > 0 && extGroup != null && epiGroupAdded.containsKey(extGroup)) {
                                    EpiGroupe epiGroupe = donnees.getEpisGroupes().getEpiGroupe().stream()
                                                .filter(el -> el.getId().equals(epiGroupAdded.getString(extGroup)))
                                                .findFirst()
                                                .orElse(null);*/
                                if(donnees.getEpisGroupes() != null) {
                                    List<EpiGroupe> listEpiGroupe = donnees.getEpisGroupes().getEpiGroupe();
                                    if (listEpiGroupe != null && listEpiGroupe.size() > 0) {
                                        EpiGroupe epiGroupe = listEpiGroupe.stream()
                                                .filter(epiG -> epiG.getId().equals(EPI_GROUPE + element.getInteger("id_elt_bilan_periodique").toString()))
                                                .findFirst()
                                                .orElse(null);
                                        if (epiGroupe != null) {//epi
                                            EpiEleve epiEleve = objectFactory.createEpiEleve();
                                            epiEleve.setCommentaire(element.getString("commentaire"));
                                            epiEleve.setEpiGroupeRef(epiGroupe);
                                            if (bilanPeriodique.getEpisEleve() == null || bilanPeriodique.getEpisEleve().getEpiEleve() == null) {
                                                bilanPeriodique.setEpisEleve(objectFactory.createBilanPeriodiqueEpisEleve());
                                            }
                                            bilanPeriodique.getEpisEleve().getEpiEleve().add(epiEleve);
                                        }
                                    }
                                }
                            }

                            private void addParcoursEleve(JsonObject app) {
                                Parcours parcoursEleve = objectFactory.createParcours();
                                parcoursEleve.setValue(app.getString("commentaire"));
                                parcoursEleve.setCode(CodeParcours.fromValue(app.getString("code")));
                                if (bilanPeriodique.getListeParcours() == null || bilanPeriodique.getListeParcours().getParcours() == null) {
                                    bilanPeriodique.setListeParcours(objectFactory.createBilanPeriodiqueListeParcours());
                                }
                                bilanPeriodique.getListeParcours().getParcours().add(parcoursEleve);
                            }
                        });
                bilanPeriodiqueService.getSuiviAcquis(idStructure, new Long(currentPeriode.getTypePeriode()),
                        currentEleve.getIdNeo4j(), currentEleve.getId_Class(),
                        new Handler<Either<String, JsonArray>>() {
                            AtomicBoolean answer = new AtomicBoolean(false);
                            AtomicInteger count = new AtomicInteger(0);
                            final String thread = "(" + currentEleve.getNom() + " " + currentEleve.getPrenom() +" )";
                            final String method = "getBaliseBilansPeriodiques | getSuiviAcquis ";
                            @Override
                            public void handle(Either<String, JsonArray> suiviAcquisResponse) {
                                if (suiviAcquisResponse.isLeft()) {
                                    String error = suiviAcquisResponse.left().getValue();
                                    if (error != null && error.contains(TIME) && !getSuiviAcquisFuture.isComplete()) {
                                        bilanPeriodiqueService.getSuiviAcquis(idStructure,
                                                new Long(currentPeriode.getTypePeriode()),
                                                currentEleve.getIdNeo4j(), currentEleve.getId_Class(), this);
                                    } else {
                                        getSuiviAcquisFuture.complete();
                                    }
                                } else {
                                    if (!suiviAcquisResponse.right().getValue().isEmpty()) {
                                        final JsonArray suiviAcquis = suiviAcquisResponse.right().getValue();

                                        addListeAcquis(suiviAcquis, bilanPeriodique);
                                        // add Only if suiviAcquis is not Empty
                                        bilansPeriodiques.getBilanPeriodique().add(bilanPeriodique);
                                    } else {
                                        if (suiviAcquisResponse.isRight()) {
                                            setError(errorsExport, currentEleve,
                                                    getLibelle("evaluation.lsu.error.no.suivi.acquis")+
                                                            currentPeriode.getLabel());
                                            log.info(currentEleve.getIdNeo4j() + " NO ");
                                        }
                                    }
                                    if(!getSuiviAcquisFuture.isComplete()) {
                                        getSuiviAcquisFuture.complete();
                                    }
                                }
                                lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                            }

                            private void addResponsable(BilanPeriodique bilanPeriodique) {
                                if (currentEleve.getResponsableList() != null && currentEleve.getResponsableList().size() > 0) {
                                    BilanPeriodique.Responsables responsablesEleve = objectFactory.createBilanPeriodiqueResponsables();
                                    responsablesEleve.getResponsable().addAll(currentEleve.getResponsableList());
                                    bilanPeriodique.setResponsables(responsablesEleve);
                                }
                            }

                                private void addListeAcquis(JsonArray suiviAcquis, BilanPeriodique bilanPeriodique) {
                                    BilanPeriodique.ListeAcquis aquisEleveList = objectFactory.createBilanPeriodiqueListeAcquis();
                                    for (Integer i = 0; i < suiviAcquis.size(); i++) {
                                        final JsonObject currentAcquis = suiviAcquis.getJsonObject(i);
                                        Boolean toAdd = false;
                                        Acquis aquisEleve = addListeAcquis_addAcquis(currentAcquis, toAdd);
                                        if(currentAcquis.getBoolean("toAdd")) {
                                            addAcquis_addDiscipline(currentAcquis, aquisEleve);
                                            addAcquis_addElementProgramme(currentAcquis, aquisEleve);
                                            addListeAcquis_addMissingTeacherToXml(aquisEleveList, currentAcquis,
                                                    aquisEleve);
                                        }
                                    }
                                    if(!aquisEleveList.getAcquis().isEmpty()) {
                                        bilanPeriodique.setListeAcquis(aquisEleveList);
                                    }
                                    else{
                                        setError(errorsExport, currentEleve,
                                                getLibelle("evaluation.lsu.error.no.suivi.acquis")+
                                                        currentPeriode.getLabel());
                                    }
                                }

                            private Acquis addListeAcquis_addAcquis(JsonObject currentAcquis, Boolean toAdd) {
                                Acquis aquisEleve = objectFactory.createAcquis();
                                JsonArray tableConversion = tableConversionByClasse.get(currentEleve.getId_Class());
                                addAcquis_addMoyennes(currentAcquis,aquisEleve,currentPeriode);
                                addAcquis_addPositionnement(currentAcquis, tableConversion, aquisEleve, currentPeriode);
                                addAcquis_setEleveNonNote(aquisEleve);
                                addAcquis_addAppreciation(currentAcquis, aquisEleve, currentPeriode , toAdd);
                                return aquisEleve;
                            }

                            private void addAcquis_addMoyennes( JsonObject currentAcquis,
                                                                Acquis acquisEleve,
                                                                Periode currentPeriode){
                                JsonArray moyennesEleves = currentAcquis.getJsonArray("moyennes");
                                JsonArray moyennesFinales = currentAcquis.getJsonArray("moyennesFinales");
                                JsonArray moyennesClasse = currentAcquis.getJsonArray("moyennesClasse");

                                JsonObject moyEleve = utilsService.getObjectForPeriode(moyennesEleves,
                                        (long)currentPeriode.getTypePeriode(), "id");
                                JsonObject moyFinale = utilsService.getObjectForPeriode(moyennesFinales,
                                        (long) currentPeriode.getTypePeriode(), "id_periode");
                                JsonObject moyClasse = utilsService.getObjectForPeriode(moyennesClasse,
                                        (long)currentPeriode.getTypePeriode(),"id");
                                //Moyenne Eleve
                                String valueMoyEleve = new String();
                                if(moyEleve != null){
                                    valueMoyEleve = (moyFinale != null)? moyFinale.getValue("moyenneFinale") + "/20" :
                                            moyEleve.getValue("moyenne") + "/20";

                                }else{
                                    valueMoyEleve = (moyFinale != null)? moyFinale.getValue("moyenneFinale") + "/20" :
                                            "NN";
                                }
                                acquisEleve.setMoyenneEleve(valueMoyEleve);
                                //MoyenneClasse
                                String valueMoyClasse = new String();
                                valueMoyClasse = (moyClasse != null)? moyClasse.getValue("moyenne") + "/20" :
                                        "NN";
                                acquisEleve.setMoyenneStructure(valueMoyClasse);
                            }
                            private void addAcquis_addPositionnement( JsonObject currentAcquis, JsonArray tableConversion,
                                                                      Acquis acquisEleve, Periode currentPeriode){

                                JsonArray positionnements_auto = currentAcquis.getJsonArray("positionnements_auto");
                                JsonArray positionnementsFinaux = currentAcquis.getJsonArray("positionnementsFinaux");

                                JsonObject positionnementAuto = utilsService.getObjectForPeriode(positionnements_auto,
                                        (long) currentPeriode.getTypePeriode(),"id_periode");

                                JsonObject positionnementFinal = utilsService.getObjectForPeriode(positionnementsFinaux,
                                        (long) currentPeriode.getTypePeriode(),"id_periode");

                                Integer valuePositionnementFinal = new Integer(0);
                                if(positionnementFinal != null)  valuePositionnementFinal = positionnementFinal.getInteger("positionnementFinal");

                                BigInteger positionnementToSet = BigInteger.valueOf(0);
                                // "hasNote" exit if  moyenne != -1
                                if(positionnementAuto != null && positionnementAuto.containsKey("hasNote")){
                                    // BigInteger positionnementToSet;
                                    String valuePositionnementAuto = utilsService.convertPositionnement(
                                            positionnementAuto.getFloat("moyenne"), tableConversion, null);

                                    positionnementToSet = (valuePositionnementFinal.intValue() != 0)? BigInteger.valueOf(valuePositionnementFinal):
                                            new BigInteger( valuePositionnementAuto);

                                }else if(valuePositionnementFinal.intValue() != 0){

                                    positionnementToSet = BigInteger.valueOf(valuePositionnementFinal);

                                    }
                                    if(positionnementToSet.intValue() != 0){
                                        acquisEleve.setPositionnement(positionnementToSet);
                                    }
                            }

                            private void addAcquis_setEleveNonNote(Acquis acquisEleve){
                                if(acquisEleve.getPositionnement() != null || !"NN".equals(acquisEleve.getMoyenneEleve())){
                                    acquisEleve.setEleveNonNote(false);
                                }else{
                                    acquisEleve.setEleveNonNote("NN".equals(acquisEleve.getMoyenneEleve()) && acquisEleve.getPositionnement() == null);
                                }
                                acquisEleve.setStructureNonNotee(false);
                            }

                            private void addListeAcquis_addMissingTeacherToXml(BilanPeriodique.ListeAcquis aquisEleveList,
                                                                               JsonObject currentAcquis,
                                                                               Acquis aquisEleve) {
                                if (currentAcquis.containsKey("teachers") && !currentAcquis.getJsonArray("teachers").isEmpty()) {
                                    JsonArray teachersList = currentAcquis.getJsonArray("teachers");
                                    for (Integer k = 0; k < teachersList.size(); k++) {
                                        Enseignant enseignant = addorFindTeacherBalise(donnees, enseignantFromSts, teachersList.getJsonObject(k));
                                        aquisEleve.getEnseignantRefs().add(enseignant);
                                    }
                                }
                                if (aquisEleve.getElementProgrammeRefs().size() > 0
                                        && aquisEleve.getEnseignantRefs().size() > 0
                                        && aquisEleve.getDisciplineRef() != null) {
                                    aquisEleveList.getAcquis().add(aquisEleve);
                                }
                            }

                                private void addAcquis_addAppreciation(JsonObject currentAcquis,
                                                                       Acquis aquisEleve,
                                                                       Periode currentPeriode, boolean toAdd) {
                                    boolean hasAppreciation = false;
                                    boolean studentIsNN = aquisEleve.isEleveNonNote();
                                    JsonObject app = addAppreciation_getObjectForPeriode(currentAcquis.getJsonArray("appreciations"),
                                            (long) currentPeriode.getTypePeriode(),
                                            "id_periode");
                                    if(app != null){
                                        JsonArray appreciationByClasse = app.getJsonArray("appreciationByClasse");
                                        if(appreciationByClasse != null && appreciationByClasse.size() > 0 ){
                                            int imax = appreciationByClasse.size();
                                            for(int i = 0; i < imax; i++ ) {
                                                app = appreciationByClasse.getJsonObject(i);
                                                if (app.containsKey("appreciation")) {
                                                    String appTmp = app.getString("appreciation");
                                                    if(appTmp != null && !appTmp.isEmpty()){
                                                        aquisEleve.setAppreciation(appTmp);
                                                        hasAppreciation = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if(!hasAppreciation && !studentIsNN){

                                        setError(errorsExport, currentEleve,
                                                getLibelle("evaluation.lsu.error.no.appreciation") +
                                        currentPeriode.getLabel());
                                    }
                                    else if(!hasAppreciation && studentIsNN){
                                        aquisEleve.setAppreciation(
                                                getLibelle("evaluation.lsu.no.appreciation.message"));
                                    }
                                    if (hasAppreciation || !studentIsNN) {
                                        bilanPeriodique.setEleveRef(currentEleve);
                                        bilanPeriodique.setPeriodeRef(currentPeriode);
                                        addResponsable(bilanPeriodique);
                                        toAdd = true;
                                    }

                                    currentAcquis.put("toAdd", toAdd);
                                }


                            private JsonObject addAppreciation_getObjectForPeriode(JsonArray array, Long idPeriode, String key) {
                                JsonObject res = null;
                                if (array != null) {
                                    for (int i = 0; i < array.size(); i++) {
                                        JsonObject o = array.getJsonObject(i);
                                        if (o.getLong(key) == idPeriode) {
                                            res = o;
                                        }
                                    }
                                }
                                return res;
                            }

                            private void addAcquis_addDiscipline(JsonObject currentAcquis, Acquis aquisEleve) {
                                String idMatiere = currentAcquis.getString("id_matiere");
                                lsuService.addIdsEvaluatedDiscipline(idMatiere);
                                Discipline currentSubj = getDisciplineInXML(idMatiere, donnees);
                                if (currentSubj != null) {
                                    aquisEleve.setDisciplineRef(currentSubj);
                                }
                            }

                            private void addAcquis_addElementProgramme(JsonObject currentAcquis, Acquis aquisEleve) {
                                String epLabel = currentAcquis.getString("elementsProgramme");
                                final ElementProgramme newEP = objectFactory.createElementProgramme();
                                newEP.setLibelle(epLabel);

                                ElementProgramme ep = donnees.getElementsProgramme().getElementProgramme().stream()
                                        .filter(cep -> cep.getLibelle().replaceAll("[\\s]","")
                                                .equals(newEP.getLibelle().replaceAll("[\\s]","")))
                                        .findFirst()
                                        .orElse(null);
                                if(ep == null){
                                    String epId = generateElementProgrammeId(idElementProgramme);
                                    newEP.setId(epId);
                                    newEP.setLibelle(epLabel);
                                    donnees.getElementsProgramme().getElementProgramme().add(newEP);
                                    aquisEleve.getElementProgrammeRefs().add(newEP);
                                }else{
                                    aquisEleve.getElementProgrammeRefs().add(ep);
                                }
                            }
                        });
               /* }
                else {
                    response.put("status", 400);
                    response.put("errorMessage", "");
                    getOut.handle(new Either.Right<String, JsonObject>(response));
                }*/
            }
        }
    }

    private void setError(JsonObject errorsExport,Eleve currentEleve, String message){
        if(errorsExport.containsKey(currentEleve.getIdNeo4j())){
            JsonObject errorEleve = errorsExport.getJsonObject(currentEleve.getIdNeo4j());
            if(errorEleve.containsKey("errorsMessages")){
                JsonArray errorsMessages = errorEleve.getJsonArray("errorsMessages");
                if(!errorsMessages.contains(message)){
                    errorsMessages.add(message);
                }
            }else{
                errorEleve.put("error",new JsonArray().add(message));
            }
        }else{
            errorsExport.put(currentEleve.getIdNeo4j(),
                    new JsonObject().put("idEleve", currentEleve.getIdNeo4j())
                            .put("lastName",currentEleve.getNom())
                            .put("firstName", currentEleve.getPrenom())
                            .put("nameClass",currentEleve.getCodeDivision())
                            .put("errorsMessages",new JsonArray().add(message)));
        }

    }

    private Boolean addDatesBilanPeriodique(BilanPeriodique bilanPeriodique,final Map<String,JsonArray> periodesByClass,
                                            Eleve currentEleve, Periode currentPeriode){

        if(periodesByClass != null  && periodesByClass.containsKey(currentEleve.getId_Class()) &&
                utilsService.getObjectForPeriode(periodesByClass.get(currentEleve.getId_Class()),
                        (long) currentPeriode.getTypePeriode(), "id_type") != null){

            JsonArray periodes = periodesByClass.get(currentEleve.getId_Class());
            JsonObject periode = utilsService.getObjectForPeriode(periodes, (long) currentPeriode.getTypePeriode(),
                    "id_type");
            if(currentPeriode.getLabel() == null) {
                String labelPeriode = getLibelle("viescolaire.periode." + periode.getValue("type"));
                labelPeriode += (" " + periode.getValue("ordre"));
                currentPeriode.setLabel(labelPeriode);
            }
            XMLGregorianCalendar dateScolarite = getDateFormatGregorian(periode.getString("timestamp_fn"));
            XMLGregorianCalendar dateConseil = getDateFormatGregorian(periode.getString("date_conseil_classe"));
            String dateVerrou = periode.getString("date_fin_saisie").substring(0,19);
            bilanPeriodique.setDateScolarite(dateScolarite);
            bilanPeriodique.setDateConseilClasse(dateConseil);
            bilanPeriodique.setDateVerrou(dateVerrou);
            return true;

        }
        return false;
    }
    private Boolean addresponsableEtabRef(Donnees donnees, JsonObject response, BilanPeriodique bilanPeriodique) {
        if ( donnees.getResponsablesEtab() != null && donnees.getResponsablesEtab().getResponsableEtab().size() > 0) {
            bilanPeriodique.setResponsableEtabRef(donnees.getResponsablesEtab().getResponsableEtab().get(0));
            return true;
        }
        return false;
    }
    private Discipline getDisciplineInXML(String id, Donnees donnees) {
        if (donnees.getDisciplines() == null || donnees.getDisciplines().getDiscipline() == null || donnees.getDisciplines().getDiscipline().size() < 0) {
            return null;
        }
        return donnees.getDisciplines().getDiscipline().stream()
                .filter(dis -> dis.getId().equals(DISCIPLINE_KEY + id))
                .findFirst()
                .orElse(null);
    }

    private Enseignant getEnseignantInXML(String id, Donnees donnees) {
        if (donnees.getEnseignants() == null || donnees.getEnseignants().getEnseignant() == null || donnees.getEnseignants().getEnseignant().size() < 0) {
            return null;
        }
        return donnees.getEnseignants().getEnseignant().stream()
                .filter(ens -> ens.getId().equals("ENS_" +id))
                .findFirst()
                .orElse(null);
    }

    private String generateElementProgrammeId ( AtomicInteger idElementProgramme){
        return "EP_"+String.valueOf(idElementProgramme.incrementAndGet());
    }

    private String getMillesimeBFC(){
        Integer millesime;
        Calendar today = Calendar.getInstance();
        Calendar janvier = Calendar.getInstance();
        janvier.set(Calendar.DAY_OF_MONTH,1);
        janvier.set(Calendar.MONTH,1);

        Calendar juillet = Calendar.getInstance();
        juillet.set(Calendar.DAY_OF_MONTH, 31);
        juillet.set(Calendar.MONTH, 7);

        millesime = today.get(Calendar.YEAR);
        // Si on est entre le 01 janvier et le 31 juillet on enleve une année au millésime
        // ex: si anne scolaire 2018/2019 le millesime est 2018
        if(today.after(janvier) && today.before(juillet)){
            millesime--;}
        return millesime.toString();
    }

    /**
     * génère le fichier xml et le valide
     * @param request
     * @param lsunBilans
     */

    private void returnResponse(final HttpServerRequest request, LsunBilans lsunBilans) {
        log.info("DEBUT method returnResponse ");
        try (StringWriter response = new StringWriter()) {

            JAXBContext jc = JAXBContext.newInstance(LsunBilans.class);
            Marshaller marshaller = jc.createMarshaller();
            // écriture de la réponse
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "urn:fr:edu:scolarite:lsun:bilans:import import-bilan-complet.xsd");
            marshaller.marshal(lsunBilans, response);

            /* Vérification du fichier xml généré par rapport au xsd */
            final String templatePath =  FileResolver.absolutePath(Competences.LSUN_CONFIG.getString("xsd_path")).toString();
            vertx.fileSystem().readFile(templatePath, new Handler<AsyncResult<Buffer>>() {
                @Override
                public void handle(AsyncResult<Buffer> result) {
                    if (!result.succeeded()) {
                        log.info("readFile ko");
                        badRequest(request);
                        return;
                    }

                    try {
                        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                        Schema schema = schemaFactory.newSchema(new StreamSource(new ByteArrayInputStream(result.result().getBytes())));
                        log.info("method returnResponse avant la validation");
                        Validator validator = schema.newValidator();
                        Source xmlFile = new StreamSource(new ByteArrayInputStream(response.toString().getBytes("UTF-8")));
                        log.info("validator");
                        validator.validate(xmlFile);
                    } catch (SAXException | IOException e) {
                        log.error("Validation : Export LSU en erreur",e);
                        request.response().setStatusCode(400).setStatusMessage(e.getMessage()).end();
                        return;
                    }
                    //préparation de la requêteDefaultCompetenceNoteService
                    request.response().putHeader("content-type", "text/xml");
                    request.response().putHeader("charset", "utf-8");
                    //request.response().putHeader("standalone", "yes");
                    request.response().putHeader("Content-Disposition", "attachment; filename=import_lsun_" + new Date().getTime() + ".xml");
                    request.response().end(Buffer.buffer(response.toString()));
                    log.info("FIN method returnResponse");
                }
            });
        } catch (IOException | JAXBException e) {
            log.error("xml non valide : "+ e.toString());
            badRequest(request);
            return;
        }
    }

    private List<String> getIdsList(JsonArray arr){
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < arr.size(); i++) {
            list.add(arr.getJsonObject(i).getString("id"));
        }
        return list;
    }
}


