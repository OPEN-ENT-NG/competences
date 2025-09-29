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
import fr.openent.competences.constants.Field;
import fr.openent.competences.enums.LevelCycle;
import fr.openent.competences.model.MoyenneFinale;
import fr.openent.competences.model.Service;
import fr.openent.competences.model.SubTopic;
import fr.openent.competences.repository.RepositoryFactory;
import fr.openent.competences.security.HasExportLSURight;
import fr.openent.competences.service.*;
import fr.openent.competences.service.digitalSkills.ClassAppreciationDigitalSkillsService;
import fr.openent.competences.service.digitalSkills.DigitalSkillsService;
import fr.openent.competences.service.digitalSkills.impl.DefaultClassAppreciationDigitalSkills;
import fr.openent.competences.service.digitalSkills.impl.DefaultDigitalSkillsService;
import fr.openent.competences.service.impl.*;
import fr.openent.competences.utils.UtilsConvert;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.neo4j.Neo4j;
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.*;
import static fr.openent.competences.bean.lsun.TypeEnseignant.fromValue;
import static fr.openent.competences.constants.Field.DI;
import static fr.openent.competences.constants.Field.EA;
import static fr.openent.competences.constants.LSUConstants.DEFAULT_SCHEMA_VERSION_VALUE;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.openent.competences.service.impl.DefaultLSUService.DISCIPLINE_KEY;
import static fr.openent.competences.service.impl.DefaultUtilsService.setServices;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static java.lang.Long.*;
import static org.entcore.common.http.response.DefaultResponseHandler.*;


/**
 * Created by agnes.lapeyronnie on 30/06/2017.
 */
public class LSUController extends ControllerHelper {

    protected static final Logger log = LoggerFactory.getLogger(LSUController.class);
    private final ObjectFactory objectFactory = new ObjectFactory();
    private final UtilsService utilsService;
    private final BFCService bfcService;
    private final BfcSyntheseService bfcSynthseService;
    private final EleveEnseignementComplementService eleveEnsCpl;
    private JsonObject errorsExport;
    private final DispenseDomaineEleveService dispenseDomaineEleveService;
    private final STSFileService stsFileService;
    private final BilanPeriodiqueService bilanPeriodiqueService;
    private final ElementBilanPeriodiqueService elementBilanPeriodiqueService;
    private final DefaultSyntheseBilanPeriodiqueService syntheseBilanPeriodiqueService;
    private final DefaultCompetenceNoteService competenceNoteService;
    private final DigitalSkillsService digitalSkillsService;
    private final ClassAppreciationDigitalSkillsService classAppreciationDigitalSkillsService;
    private final LSUService lsuService;
    private final UserService userService;
    private int fakeCode = 10;

    private static final String TIME = "Time";
    private static final String MESSAGE = "message";

    //ID
    private static final String EPI_GROUPE = "EPI_GROUPE_";
    private static final String ACC_GROUPE = "ACC_GROUPE_";



    public LSUController(ServiceFactory serviceFactory) {
        bilanPeriodiqueService = serviceFactory.bilanPeriodiqueService();
        elementBilanPeriodiqueService = new DefaultElementBilanPeriodiqueService(serviceFactory.eventBus());
        utilsService = new DefaultUtilsService(serviceFactory.eventBus());
        bfcService = new DefaultBFCService(serviceFactory.eventBus());
        syntheseBilanPeriodiqueService = new DefaultSyntheseBilanPeriodiqueService();
        bfcSynthseService = new DefaultBfcSyntheseService(Competences.COMPETENCES_SCHEMA, Competences.BFC_SYNTHESE_TABLE, serviceFactory.eventBus());
        eleveEnsCpl = new DefaultEleveEnseignementComplementService(Competences.COMPETENCES_SCHEMA,
                Competences.ELEVE_ENSEIGNEMENT_COMPLEMENT);
        dispenseDomaineEleveService = new DefaultDispenseDomaineEleveService(Competences.COMPETENCES_SCHEMA,
                Competences.DISPENSE_DOMAINE_ELEVE);
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA,
                Competences.COMPETENCES_NOTES_TABLE);
        lsuService = new DefaultLSUService(serviceFactory.eventBus());
        stsFileService = new DefaultSTSFileService(Competences.STSFILE_TABLE);
        digitalSkillsService = new DefaultDigitalSkillsService();
        classAppreciationDigitalSkillsService = new DefaultClassAppreciationDigitalSkills(COMPETENCES_SCHEMA,
                CLASS_APPRECIATION_DIGITAL_SKILLS);
        Neo4j neo4j = Neo4j.getInstance();
        RepositoryFactory repositoryFactory = new RepositoryFactory(neo4j);
        this.userService = new DefaultUserService(repositoryFactory);
    }

    /**
     * save sts file with id_structure, name_file and content
     *
     */
    @Post("/lsu/data/sts")
    @ApiDoc("Save sts data")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(HasExportLSURight.class)
    public void saveDataSts ( final HttpServerRequest request) {
        RequestUtils.bodyToJson(request,pathPrefix + Competences.SCHEMA_CREATE_STSFILE, oSTSFile ->
                stsFileService.create(oSTSFile, defaultResponseHandler(request)));
    }

    /**
     * get all sts files for a structure
     *
     */
    @Get("/lsu/sts/files/:idStructure")
    @ApiDoc("Get sts data")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(HasExportLSURight.class)
    public void getDataSts ( final HttpServerRequest request) {
        String id_etablissement = request.getParam("idStructure");
        stsFileService.getSTSFile(id_etablissement, arrayResponseHandler(request));
    }

    @Post("/lsu/unheeded/students")
    @ApiDoc("Ajoute / Supprime / Recupere les élèves cochés ignorés")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(HasExportLSURight.class)
    public void postUnheededStudents( final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            String action = body.getString("action");
            switch (action) {
                case "add" : {
                    JsonArray idsStudents =  body.getJsonArray("idsStudents");
                    Long idPeriode = body.getLong(ID_PERIODE_KEY);
                    String idClasse = body.getString(ID_CLASSE_KEY);
                    lsuService.addUnheededStudents(idsStudents, idPeriode, idClasse, arrayResponseHandler(request));
                    break;
                }

                case "rem": {
                    JsonArray idsStudents =  body.getJsonArray("idsStudents");
                    Long idPeriode = body.getLong(ID_PERIODE_KEY);
                    String idClasse = body.getString(ID_CLASSE_KEY);
                    lsuService.remUnheededStudents(idsStudents, idPeriode, idClasse, arrayResponseHandler(request));
                    break;
                }

                case "get": {
                    JsonArray idPeriodes = body.getJsonArray("idPeriodes");
                    JsonArray idClasses = body.getJsonArray("idClasses");
                    String idStructure = body.getString(ID_STRUCTURE_KEY);
                    lsuService.getUnheededStudents(idPeriodes, idClasses, idStructure, arrayResponseHandler(request));
                    break;
                }

                default: {
                    badRequest(request);
                    break;
                }
            }
        });
    }
    /**
     * Methode qui contruit le xml pour le LSU
     *
     * @param request contient la list des idsClasse et des idsResponsable ainsi que idStructure
     *                sur laquelle sont les responsables
     */
    @Post("/exportLSU/lsu")
    @ApiDoc("Export data to LSUN xml format")
    @SecuredAction("competences.lsun.export")
    public void getXML(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject entries) {
                if (entries.containsKey("type")) {
                    errorsExport = new JsonObject();
                    if ("1".equals(entries.getString("type"))) {
                        bilanFinCycleExport(request, entries);
                    } else {
                        bilanPeriodiqueExport(request, entries);
                    }
                } else {
                    badRequest(request, "No valid params");
                }
            }
        });
    }

    /**
     * méthode qui récupère les responsables de direction à partir de idStructure
     *
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
                    eb.request(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
            } else {
                if(!errorsExport.containsKey("error")){
                    errorsExport.put("error", event);
                }
                renderJson(request,errorsExport, 400);
                log.info("getXML : BaliseBilansCycle");
            }
        };

        List<Future<?>> listFutureGetMethodsBFC = new ArrayList<>();

        Promise<Void> getEntetePromise = Promise.promise();
        listFutureGetMethodsBFC.add(getEntetePromise.future());
        Handler<String> getEnteteHandler = event -> {
            if (event.equals("success")) {
                getEntetePromise.complete();
            } else {
                getEntetePromise.fail("can't generate Balises Entete Future");
                log.error("getXML : getBaliseEntete " + event);
            }
        };
        getBaliseEntete(lsunBilans, idStructure, getEnteteHandler);

        Promise<Void> getResponsablePromise = Promise.promise();
        listFutureGetMethodsBFC.add(getResponsablePromise.future());
        Handler<String> getResponsableHandler = event -> {
            if (event.equals("success")) {
                getResponsablePromise.complete();
            } else {
                getResponsablePromise.fail("can't generate Balises Responsables Future");
                log.error("getXML : getBaliseResponsable " + event);
            }
        };
        getBaliseResponsables(donnees, idsResponsable, getResponsableHandler);

        Promise<Void> getElevesPromise = Promise.promise();
        listFutureGetMethodsBFC.add(getElevesPromise.future());
        Handler<String> getElevesHandler = event -> {
            if (event.equals("success")) {
                getElevesPromise.complete();
            } else {
                getElevesPromise.fail(event);
                log.error("getXML : getBaliseEleves " + event);
            }
        };
        getBaliseEleves(donnees, idsClasse, getElevesHandler);

        Promise<Map<String,List<Enseignant>>> getHeadTeachersPromise = Promise.promise();
        listFutureGetMethodsBFC.add(getHeadTeachersPromise.future());
        Handler<String> getHeadTeachersHandler = event -> {
            Map<String,List<Enseignant>> mapIdClassListHeadTeacher = new HashMap<>();
            if(event.equals("success")){
                if( mapIdClassHeadTeacher.size() > 0){
                    for(Map.Entry<String,JsonArray> jsonArrayEntry : mapIdClassHeadTeacher.entrySet() ){
                        JsonArray arrayHeadTeachers = jsonArrayEntry.getValue();
                        List<Enseignant> listHeadTeacher = new ArrayList<>();
                        if(arrayHeadTeachers != null && arrayHeadTeachers.size() > 0){
                            for(int i = 0; i < arrayHeadTeachers.size(); i++){
                                Enseignant headTeacherEnseignant = addorFindTeacherBalise(donnees,enseignantFromSts,
                                        arrayHeadTeachers.getJsonObject(i));
                                listHeadTeacher.add(headTeacherEnseignant);
                            }
                        }
                        mapIdClassListHeadTeacher.put(jsonArrayEntry.getKey(),listHeadTeacher);
                    }
                }
                getHeadTeachersPromise.complete(mapIdClassListHeadTeacher);
            }else{
                getHeadTeachersPromise.complete(mapIdClassListHeadTeacher);
                log.error("getXML LSU : getHeadteachers "+ event);
            }
        };
        getHeadTeachers( idsClasse, mapIdClassHeadTeacher,getHeadTeachersHandler);

        Promise<Map<String, JsonObject>> getDatesCreationVerrouByClassesPromise = Promise.promise();
        listFutureGetMethodsBFC.add(getDatesCreationVerrouByClassesPromise.future());
        Handler<Either<String, Map<String, JsonObject>>> getDatesCreationVerrouHandler = event -> {
            if(event.isRight()){
                getDatesCreationVerrouByClassesPromise.complete(event.right().getValue());
            }else{
                log.error("getXML LSU : getDatesCreationVerrouByClasses " + event);
                getDatesCreationVerrouByClassesPromise.fail("getXML LSU : getDatesCreationVerrouByClasses " +
                        event.left().getValue());
            }
        };
        Utils.getDatesCreationVerrouByClasses(eb, idStructure, idsClasse, getDatesCreationVerrouHandler);

        Promise<List<Map>> getIdClassIdCycleValuePromise = Promise.promise();
        listFutureGetMethodsBFC.add(getIdClassIdCycleValuePromise.future());
        Handler<Either<String, List<Map>>> getIdClassIdCycleValueHandler = event -> {
            if(event.isRight()){
                getIdClassIdCycleValuePromise.complete(event.right().getValue());
            }else{
                log.error("getXML LSU : getIdClassIdCycleValue : list (map<idclasse,idCycle>,map<idCycle,cycle>) " +
                        event.left().getValue());
                getIdClassIdCycleValuePromise.fail(
                        "getXML LSU : getIdClassIdCycleValue : list (map<idclasse,idCycle>,map<idCycle,cycle>) "
                                + event.left().getValue());
            }
        };
        lsuService.getIdClassIdCycleValue(idsClasse, getIdClassIdCycleValueHandler );

        Promise<Map<String,Map<Long, String>>> getMapIdClassCodeDomaineByIdPromise = Promise.promise();
        listFutureGetMethodsBFC.add(getMapIdClassCodeDomaineByIdPromise.future());
        Handler<Either<String, Map<String,Map<Long, String>>>> getMapCodeDomaineByIdHandler = event -> {
            if(event.isRight()){
                getMapIdClassCodeDomaineByIdPromise.complete(event.right().getValue());
            }else{
                log.error("getXML LSU : getMapCodeDomaineById error when collecting codeDomaineById " + event.left().getValue());
                getMapIdClassCodeDomaineByIdPromise.fail("getMapCodeDomaineById : map<");
            }
        };
        lsuService.getMapIdClassCodeDomaineById(idsClasse,getMapCodeDomaineByIdHandler);

        lsunBilans.setSchemaVersion(DEFAULT_SCHEMA_VERSION_VALUE);
        log.info("DEBUT  get exportLSU : export Classe : " + idsClasse);
        if (!idsClasse.isEmpty() && !idsResponsable.isEmpty()) {
            Future.all(listFutureGetMethodsBFC).onComplete(event -> {
                if(event.succeeded()){
                    Map<String, JsonObject> dateCreationVerrouByClasse = getDatesCreationVerrouByClassesPromise.future().result();
                    Map<String,List<Enseignant>> mapIdClassListHeadTeacher = getHeadTeachersPromise.future().result();
                    List<Map> listMapClassCycle = getIdClassIdCycleValuePromise.future().result();
                    Map<String,Map<Long,String>> mapIdClasseCodesDomaines = getMapIdClassCodeDomaineByIdPromise.future().result();
                    getBaliseBilansCycle(mapIdClasseCodesDomaines, listMapClassCycle, donnees, idsClasse, idStructure,
                            dateCreationVerrouByClasse, mapIdClassListHeadTeacher, getBilanfinCycleHandler);
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
     * @param entries idstructure idsClass idsPersonResponsible period_type stsFile
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

        final String idStructure = entries.getString("idStructure");
        final List<String> idsClasse = getIdsList(entries.getJsonArray("classes"));
        final List<String> idsResponsable = getIdsList(entries.getJsonArray("responsables"));
        final List<Integer> idsTypePeriodes = new ArrayList<Integer>();
        JsonArray dataPeriodesType = entries.getJsonArray("periodes_type");
        final JsonArray enseignantFromSts = entries.getJsonArray("stsFile");
        for (int i = 0; i < dataPeriodesType.size(); i++) {
            idsTypePeriodes.add(dataPeriodesType.getJsonObject(i).getInteger("id_type"));
        }

        final LsunBilans lsunBilans = objectFactory.createLsunBilans();
        final Donnees donnees = objectFactory.createDonnees();
        final JsonObject epiGroupAdded = new JsonObject();
        final Map<String, JsonArray> tableConversionByClass = new HashMap<>();
        final Map<String, JsonArray> periodesByClass = new HashMap<>();
        final Map<String, Integer> cyclesByClass = new HashMap<>();
        final Map<String, JsonArray> mapIdClassHeadTeachers = new HashMap<>();
        final Map<String, JsonArray> mapIdsGroupsClasses = new HashMap<>();
        Map<Long, JsonObject> periodeUnheededStudents = new HashMap<>();
        List<String> idsGroupsClasses = new ArrayList<>();
        lsuService.initIdsEvaluatedDiscipline();
        fakeCode = 10;
        donnees.setEnseignants(objectFactory.createDonneesEnseignants());

        Handler<Either.Right<String, JsonObject>> getBilansPeriodiquesHandler = backResponse -> {
            JsonObject data = backResponse.right().getValue();
            lsuService.validateDisciplines(lsuService.getIdsEvaluatedDiscipline(), donnees, errorsExport);
            if (data.getInteger("status") == 200 && errorsExport.isEmpty()) {
                log.info("FIN exportLSU : export ");
                lsunBilans.setDonnees(donnees);
                returnResponse(request, lsunBilans);
            } else {
                renderJson(request, errorsExport, 400);//406
                log.error("getXML : getBaliseBilansPeriodiques " + data.getString("error"));
                //badRequest(request, "getXML : getBaliseBilansPeriodiques " + backresponse);
            }
        };

        List<Future<Void>> listGetFuture = new ArrayList<>();

        Promise<Void> getClassGroupsPromise = Promise.promise();
        listGetFuture.add(getClassGroupsPromise.future());
        Handler<String> getGroupsClassHandler = event -> {
            if (event.equals("success")) {
                log.info("getGroupsClass");
                getClassGroupsPromise.complete();
            } else {
                badRequest(request,"getXML : getGroupsClass " + event);
                log.error("getXML : getGroupsClass " + event);
            }
        };
        getGroupsClass(idsClasse, idsGroupsClasses, mapIdsGroupsClasses, getGroupsClassHandler);

        Promise<Void> getCyclePromise = Promise.promise();
        listGetFuture.add(getCyclePromise.future());
        Handler<String> getCycleHandler = event -> {
            if (event.equals("success")) {
                log.info("getCycle");
                getCyclePromise.complete();
            } else {
                badRequest(request,"getXML : getCycle " + event);
                log.error("getXML : getCycle " + event);
            }
        };
        getCycle(idsClasse, cyclesByClass, getCycleHandler);

        Promise<Void> getHeadTeachersPromise = Promise.promise();
        listGetFuture.add(getHeadTeachersPromise.future());
        Handler<String> getHeadTeachersHandler = event -> {
            if (event.equals("success")) {
                log.info("getHeadTeachers");
                getHeadTeachersPromise.complete();
            } else {
                badRequest(request,"getXML : getBaliseEnseignants " + event);
                log.error("getXML : getBaliseEnseignants " + event);
            }
        };
        getHeadTeachers(idsClasse, mapIdClassHeadTeachers, getHeadTeachersHandler);

        Promise<Void> getTableConversionPromise = Promise.promise();
        listGetFuture.add(getTableConversionPromise.future());
        Handler<String> getTableConversionHandler = event -> {
            if(event.equals("success")){
                log.info("getConversionTableFuture");
                getTableConversionPromise.complete();
            }else{
                log.error("getXML : getConversionTable ");
                getTableConversionPromise.fail("can't get the conversion table Future");
            }
        };
        getTableConversion(idStructure, idsClasse, tableConversionByClass, getTableConversionHandler);

        Promise<Void> getDisciplinePromise = Promise.promise();
        listGetFuture.add(getDisciplinePromise.future());
        Handler<String> getDisciplineHandler = event -> {
            if (event.equals("success")) {
                log.info("getDisciplineFuture");
                getDisciplinePromise.complete();
            } else {
                getDisciplinePromise.fail("can't generate Balises Disciplines Future");
                log.error("getXML : getBaliseDiscipline " + event);
            }
        };
        getBaliseDisciplines(donnees, idStructure, getDisciplineHandler);

        Promise<Void> getResponsablePromise = Promise.promise();
        listGetFuture.add(getResponsablePromise.future());
        Handler<String> getResponsableHandler = event -> {
            if (event.equals("success")) {
                log.info("getResponsableFututre");
                getResponsablePromise.complete();
            } else {
                getResponsablePromise.fail("can't generate Balises Responsables Future");
                log.error("getXML : getBaliseResponsable " + event);
            }
        };
        getBaliseResponsables(donnees, idsResponsable, getResponsableHandler);

        Promise<Void> getEntetePromise = Promise.promise();
        listGetFuture.add(getEntetePromise.future());
        Handler<String> getEnteteHandler = event -> {
            if (event.equals("success")) {
                log.info("getEnteteFuture");
                getEntetePromise.complete();
            } else {
                getEntetePromise.fail("can't generate Balises Entete Future");
                log.error("getXML : getBaliseEntete " + event);
            }
        };
        getBaliseEntete(lsunBilans, idStructure, getEnteteHandler);

        // Récupération des élèves à ignorer pour l'export
        Promise<JsonArray> ignoredStudentPromise = Promise.promise();
        lsuService.getUnheededStudents(new JsonArray(idsTypePeriodes), new JsonArray(idsClasse),
                unheededStudents -> formate(ignoredStudentPromise, unheededStudents));

        lsunBilans.setSchemaVersion(DEFAULT_SCHEMA_VERSION_VALUE);
        log.info("DEBUT  get exportLSU : export Classe : " + idsClasse);
        if (!idsClasse.isEmpty() && !idsResponsable.isEmpty()) {
            Handler<String> getElevesHandler = event -> {
                if (event.equals("success")) {
                    Future.all(listGetFuture).onComplete(eventFuture -> {
                        if (eventFuture.succeeded()) {
                            List<Future<?>> listGetProjectAndCompNum = new ArrayList();

                            Promise<Void> projectPromise = Promise.promise();
                            listGetProjectAndCompNum.add(projectPromise.future());
                            Handler<String> getProjectHandler = project -> {
                                if(project.equals("success")){
                                    projectPromise.complete();
                                } else {
                                    projectPromise.fail("getXML : getApEpiParcoursBalises");
                                    log.error("getXML : getApEpiParcoursBalises " + project);
                                }
                            };
                            getApEpiParcoursBalises(donnees, idsGroupsClasses, idStructure, epiGroupAdded,
                                    enseignantFromSts, getProjectHandler);

                            Promise<Void> compNumCommunPromise = Promise.promise();
                            listGetProjectAndCompNum.add(compNumCommunPromise.future());
                            List<String> idsClasesCycle3 = new ArrayList<>();
                            if(hasClassesWithCycle3(donnees, idsClasse, idsClasesCycle3, cyclesByClass)) {
                                Handler<String> getBaliseCompNumCommuneHandler = compNumCommun -> {
                                    if(compNumCommun.equals("success")){
                                        compNumCommunPromise.complete();
                                    } else {
                                        compNumCommunPromise.fail("getXML : getBaliseCompetencesNumeriqueCommun");
                                        log.error("getXML : log.error(\"getXML : getApEpiParcoursBalises \" + project); " + compNumCommun);
                                    }
                                };
                                getBaliseCompetencesNumeriqueCommun(donnees, idsClasesCycle3, getBaliseCompNumCommuneHandler);
                            } else {
                                compNumCommunPromise.complete();
                            }

                            List<String> idEleves = donnees.getEleves().getEleve().stream()
                                    .map(Eleve::getIdNeo4j).collect(Collectors.toList());

                            Future<List<SubTopic>> subTopicCoefFuture = utilsService.getSubTopicCoeff(idStructure);
                            listGetProjectAndCompNum.add(subTopicCoefFuture);

                            Promise<JsonArray> getAbsencesAndRetardsPromise = Promise.promise();
                            listGetProjectAndCompNum.add(getAbsencesAndRetardsPromise.future());
                            bilanPeriodiqueService.getRetardsAndAbsences(idStructure, idEleves, idsClasse,
                                    absencesEvent -> formate(getAbsencesAndRetardsPromise, absencesEvent));

                            Promise<JsonArray> servicesPromise = Promise.promise();
                            listGetProjectAndCompNum.add(servicesPromise.future());
                            utilsService.getServices(idStructure, new JsonArray(idsGroupsClasses),
                                    servicesEvent -> formate(servicesPromise, servicesEvent));

                            Promise<JsonArray> multiTeachersPromise = Promise.promise();
                            listGetProjectAndCompNum.add(multiTeachersPromise.future());
                            utilsService.getAllMultiTeachers(idStructure, new JsonArray(idsGroupsClasses),
                                    multiTeachersEvent -> formate(multiTeachersPromise, multiTeachersEvent));
                            Future.all(listGetProjectAndCompNum).onComplete(eventProjectCompNum -> {
                                if(eventProjectCompNum.succeeded()){
                                    final JsonArray absencesAndRetards = getAbsencesAndRetardsPromise.future().result();
                                    final JsonArray servicesJsonArray = servicesPromise.future().result();
                                    final JsonArray multiTeachers = multiTeachersPromise.future().result();
                                    List<SubTopic> subTopics = subTopicCoefFuture.result();

                                    fr.openent.competences.model.Structure structure = new fr.openent.competences.model.Structure();
                                    structure.setId(idStructure);
                                    List<Service> services = new ArrayList<>();
                                    setServices(structure, servicesJsonArray, services,subTopics);

                                    this.getBaliseBilansPeriodiques(donnees, idStructure, idsGroupsClasses, periodesByClass, cyclesByClass,
                                            tableConversionByClass, enseignantFromSts, mapIdClassHeadTeachers, periodeUnheededStudents,
                                            absencesAndRetards, services, multiTeachers, mapIdsGroupsClasses, getBilansPeriodiquesHandler);
                                } else {
                                    log.error("getXML : listGetProjectAndCompNum " + eventProjectCompNum);
                                    badRequest(request, "getXML : listGetProjectAndCompNum "+ eventProjectCompNum);
                                }
                            });
                        } else{
                            badRequest(request, eventFuture.cause().getMessage());
                            log.error("getXML : listGetFuture " + eventFuture);
                        }
                    });
                } else {
                    badRequest(request, "getEleves : "+ event);
                    log.error("getXML : getBaliseEleves " + event);
                }
            };

            Handler<String> getPeriodesHandler = event -> {
                if (event.equals("success")) {
                    Future.all(ignoredStudentPromise.future(), Future.succeededFuture()).onComplete(eventignoredStudent -> {
                        if (eventignoredStudent.succeeded()) {
                            log.info("getAllStudentAndBaliseEleve");
                            lsuService.setLsuUnheededStudents(ignoredStudentPromise.future(), periodeUnheededStudents);
                            getAllStudentAndBaliseEleve(request, donnees, idsClasse, periodesByClass, idStructure,
                                    periodeUnheededStudents, getElevesHandler);
                        } else{
                            badRequest(request, eventignoredStudent.cause().getMessage());
                        }
                    });
                } else {
                    badRequest(request, "getXML : getBalisePeriode " + event);
                    log.error("getXML : getBalisePeriode " + event);
                }
            };
            getBalisePeriodes(donnees, idsTypePeriodes, periodesByClass, idStructure, idsClasse, getPeriodesHandler);
            log.info("getPeriodesFuture");
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

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
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
                                eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
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

    private void getBaliseResponsables(final Donnees donnees, final List<String> idsResponsable,
                                       final Handler<String> handler) {

        JsonObject action = new JsonObject()
                .put("action", "user.getUsers")
                .put("idUsers", new fr.wseduc.webutils.collections.JsonArray(idsResponsable));
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
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
                                    if (!responsableJson.getString("externalId").isEmpty()  &&
                                            !responsableJson.getString("displayName").isEmpty()) {
                                        ResponsableEtab responsableEtab = objectFactory.createResponsableEtab(
                                                responsableJson.getString("externalId"),
                                                responsableJson.getString("displayName"));
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
                                eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
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
     * Get all students ( have changed Class or being deleted) with their responsables
     * @param donnees balise donnees
     * @param idsClass list of  requested classes
     * @param periodesByClass map idClass periodes
     * @param handler response success
     */
    private void getAllStudentAndBaliseEleve(HttpServerRequest request, Donnees donnees, List<String> idsClass,
                                             Map<String,JsonArray> periodesByClass, String idStructure,
                                             Map<Long, JsonObject> periodeUnheededStudents, Handler<String> handler){
        Map<String, JsonObject> mapDeleteStudent = new HashMap<>();

        Handler<Either<String,JsonArray>> handlerGetAllStudents = event -> {
            if(event.isLeft()){
                badRequest(request,"getAllStudentWithRelatives : "+ event.left().getValue());
            }else{
                JsonArray allStudents = event.right().getValue();
                try {
                    getBaliseEleveBP(donnees, idsClass, periodesByClass, allStudents, mapDeleteStudent,
                            periodeUnheededStudents, handler);
                } catch (Exception e) {
                    e.printStackTrace();
                    if(e instanceof ParseException){
                        badRequest(request, "getBaliseEleveBP : error to convert date "+ e.getMessage());
                        log.error("getBaliseEleveBP : error to convert date "+ e.getMessage());
                    }else{
                        badRequest(request, "getBaliseEleveBP : error to setEleve "+ e.getMessage());
                        log.error("getBaliseEleveBP : error to setEleve "+ e.getMessage());
                    }
                }
            }
        };

        final Handler<Either<String,Map<String,JsonObject>>> handlerDeletedStudentPostgre = event -> {
            if(event.isLeft()){
                String error = event.left().getValue();
                log.error("error to get deleted Student in Postgres : " + error);
                badRequest(request, "getDeletedStudentsPostgres : " + error);
            }else{
                List<String> idsEleve = new ArrayList<String>(mapDeleteStudent.keySet());
                lsuService.getAllStudentWithRelatives(idStructure, idsClass, idsEleve, handlerGetAllStudents);
            }
        };

        lsuService.getDeletedStudentsPostgres(periodesByClass,mapDeleteStudent, handlerDeletedStudentPostgre);
    }

    private void getBaliseEleveBP(Donnees donnees, List<String> idsClass,Map<String,JsonArray> periodesByClass,
                                  JsonArray allStudentsWithRelatives, Map<String,JsonObject> deletedStudentPostgres,
                                  Map<Long, JsonObject> periodeUnheededStudents,Handler<String> handler)
            throws ParseException {
        Map<String,String> mapIdClassCodeDivision = new HashMap<>();
        //errorsExport = new JsonObject();
        if(allStudentsWithRelatives.isEmpty()){
            handler.handle("no student");
        }else {
            Donnees.Eleves eleves = objectFactory.createDonneesEleves();

            for (int i = 0; i < allStudentsWithRelatives.size(); i++) {
                JsonObject student = allStudentsWithRelatives.getJsonObject(i);
                String created_date = student.getString("createdDate");
                Date createdDate = UtilsConvert.convertStringToDate(created_date, "yyyy-MM-dd");
                String idEleve = student.getString("idEleve");
                String idClasse = student.getString("idClass");
                JsonArray periodes = periodesByClass.get(idClasse);
                int nbIgnoredTimes = lsuService.nbIgnoredTimes(idEleve, idClasse, periodesByClass,
                        periodeUnheededStudents);
                // Si l'élève est ignoré sur toutes les périodes de l'export pour sa classe,
                if(periodes != null && periodes.size() == nbIgnoredTimes) {
                    continue;
                }
                //cas élève non supprimé qui est dans la classe (Neo4j) => élève qui n'a pas changé de classe
                //or student being deleted
                if (idsClass.size() == 1 && idsClass.contains(student.getString("idClass")) || !deletedStudentPostgres.containsKey(idEleve)  ){
                    String biggestPeriode = Utils.getPeriode(periodesByClass.get(idClasse), false);
                    Date biggestPeriodeDate = UtilsConvert.convertStringToDate(biggestPeriode, "yyyy-MM-dd");
                    if (createdDate == null || createdDate.before(biggestPeriodeDate)) {
                        Eleve eleve = setBaliseEleve(eleves, mapIdClassCodeDivision, null, null, student, handler);
                        setBaliseResponsableAndAdress(student, eleve);
                    }
                }else{//cas de l'élève qui a été dans la ou les classes demandées => élève qui a changé de classe

                    JsonObject studentPostgres = deletedStudentPostgres.get(student.getString("idEleve"));
                    JsonArray oldClasses = new JsonArray(studentPostgres.getString("delete_date_id_class"));
                    //élève qui a changé de classe et dont la nouvelle classe n'est pas demandée pour l'export
                    //dans la rep de la requête Neo on aura id de la nouvelle classe et non de l'ancienne
                    // si on demande l'export de l'ancienne et/ou de la nouvelle
                    if(!idsClass.contains(student.getString("idClass"))) { //cas élève supprimé ds une autre classe
                        if (oldClasses.size() == 1) {
                            String deleteDateString = oldClasses.getJsonObject(0).getString("deleteDate");
                            Date deleteDatePostgre = UtilsConvert.convertStringToDate(deleteDateString.split("T")[0], "yyyy-MM-dd");
                            String idClassPostgres = oldClasses.getJsonObject(0).getString("oldIdClass");
                            String biggestPeriode = Utils.getPeriode(periodesByClass.get(idClassPostgres), false);
                            Date biggestPeriodeDate = UtilsConvert.convertStringToDate(biggestPeriode, "yyyy-MM-dd");

                            if (createdDate == null || createdDate.before(biggestPeriodeDate)) {
                                Eleve eleve = setBaliseEleve(eleves, mapIdClassCodeDivision, idClassPostgres,
                                        deleteDatePostgre, student, handler);
                                setBaliseResponsableAndAdress(student, eleve);
                            }
                        } else {
                            addErrorClass(studentPostgres, student);
                        }
                    } else {//cas où l'export est demandé sur plusieurs classes et que l'élève appartient et a appartenu à celles-ci
                        addErrorClass(studentPostgres, student);
                    }
                }
            }

            if(eleves.getEleve().isEmpty()) {
                handler.handle("no student");
                log.info("FIN method getBaliseEleves : aucun eleve ajoute ");
            } else {
                donnees.setEleves(eleves);
                handler.handle("success");
                log.info("FIN method getBaliseEleves : nombre d'eleve ajoutes :" + eleves.getEleve().size());
            }
        }
    }

    private void addErrorClass(JsonObject studentPostgres, JsonObject student){
        JsonObject eleveError = new JsonObject()
                .put("idEleve", studentPostgres.getString("id_user"))
                .put("firstName", studentPostgres.getString("first_name"))
                .put("lastName", studentPostgres.getString("last_name"))
                .put("idClasse",student.getString("idClass"))
                .put("nameClass",student.getString("nameClass"));

        String messageError = getLibelle("evaluation.lsu.error.eleve.in.several.classes");
        setError(errorsExport, eleveError, messageError, null);
    }

    private Eleve setBaliseEleve(Donnees.Eleves eleves,
                                 Map<String,String> mapIdClassCodeDivision,
                                 String idClassePostgres,
                                 Date deleteDatePostgres,
                                 JsonObject student,
                                 Handler<String> handler){

        Eleve eleve = null;
        String idEleve = student.getString("idEleve");
        if (!eleves.containIdEleve(idEleve)) {
            String[] externalIdClass ;
            String className;

            if (student.getString("externalIdClass") != null) {
                externalIdClass = student.getString("externalIdClass").split("\\$");
                className = externalIdClass[(externalIdClass.length - 1)];
                try {
                    if(idClassePostgres != null){//cas élève supprimé => deleteDate vient de Postgré
                        eleve = objectFactory.createEleve(student.getString("externalId"),
                                student.getString("attachmentId"), student.getString("firstName"),
                                student.getString("lastName"), mapIdClassCodeDivision.get(idClassePostgres),
                                student.getString("idEleve"),
                                idClassePostgres, student.getString("level"));
                        eleve.setDeleteDate(deleteDatePostgres);

                    }else{//élève appartenant à la classe => neo
                        eleve = objectFactory.createEleve(student.getString("externalId"),
                                student.getString("attachmentId"), student.getString("firstName"),
                                student.getString("lastName"), className ,
                                student.getString("idEleve"), student.getString("idClass"),
                                student.getString("level"));
                        Date deleteDate = null;
                        if(student.getLong("deleteDate") != null){
                            Long deleteDateLongNeo = student.getLong("deleteDate");
                            deleteDate = new Date(deleteDateLongNeo);
                            eleve.setDeleteDate(deleteDate);
                        }else{
                            eleve.setDeleteDate(deleteDate);
                        }

                        if(!mapIdClassCodeDivision.containsKey(student.getString("idClass"))){
                            mapIdClassCodeDivision.put(student.getString("idClass"),className);
                        }
                    }

                    String createDateString = student.getString("createdDate").split("T")[0];
                    Date createDate =  UtilsConvert.convertStringToDate(createDateString,"yyyy-MM-dd");
                    eleve.setCreatedDate(createDate);

                    eleves.getEleve().add(eleve);

                } catch (Exception e) {
                    if(e instanceof NumberFormatException){
                        handler.handle(e.getMessage());
                        log.error(" method getBaliseEleve : creationEleve " +student.getString("lastName") +
                                " id " +student.getString("idEleve")+" "+ e.getMessage() +
                                "new BigInteger(attachmentId) is impossible attachmentId : "+
                                student.getString("attachmentId"));
                    }else {
                        log.error(" method getBaliseEleve : creationEleve " +student.getString("lastName") +
                                " id " +student.getString("idEleve")+" "+ e.getMessage());
                    }
                }
            }else {

                log.info("[EXPORT LSU]: remove " + student.getString("name")
                        + student.getString("firstName"));

            }
        } else {
            eleve = eleves.getEleveById(idEleve);
        }
        return eleve;
    }

    private void setBaliseResponsableAndAdress (JsonObject student, Eleve eleve){
        Adresse adresse = null;
        Responsable responsable = null;
        String adress = student.getString("address");
        String commune = student.getString("city");
        String codePostal ;
        try {
            adress = (adress == null || adress.isEmpty()) ? "inconnue" : adress;
            commune = (commune == null || commune.isEmpty()) ? "inconnue" : commune;
            if(commune.length() > 100){
                commune = commune.substring(0,100);
            }
            codePostal =  student.getString("zipCode");

            codePostal = (codePostal == null || codePostal.isEmpty()) ? "inconnu" : codePostal;

            if(codePostal.length() > 10){
                codePostal = codePostal.substring(0,10);
            }

        }catch (ClassCastException e) {
            codePostal = String.valueOf(student.getInteger("zipCode"));
            if (codePostal == null) {
                codePostal = "inconnu";
            }
        }

        adresse = objectFactory.createAdresse(adress, codePostal, commune);

        if (student.getString("externalIdRelative")!= null && student.getString("lastNameRelative") != null &&
                student.getString("firstNameRelative")!= null && student.getJsonArray("relative").size() > 0 ) {
            JsonArray relatives = student.getJsonArray("relative");

            String civilite = student.getString("civilite");

            for (int j = 0; j < relatives.size(); j++) {
                String relative = relatives.getString(j);
                String[] paramRelative = relative.toString().split("\\$");
                //création d'un responsable Eleve avec la civilite si MERE ou PERE

                if (student.getString("externalIdRelative").equals(paramRelative[0])) {
                    if (adresse != null) {
                        responsable = objectFactory.createResponsable(student.getString("externalIdRelative"),
                                student.getString("lastNameRelative"),
                                student.getString("firstNameRelative"), relative, adresse);
                    } else {
                        responsable = objectFactory.createResponsable(student.getString("externalIdRelative"),
                                student.getString("lastNameRelative"),
                                student.getString("firstNameRelative"), relative);
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


    /**
     * pour une liste de classe mise a jour des attributs de l'eleve et de son responsable.
     *
     * @param donnees la liste des eleves est ajoutee a la balise donnees
     * @param classids liste des classes pour lesquelles le fichier xml doit etre genere
     * @param handler  renvoie  "success" si tout c'est bien passe
     */

    private void getBaliseEleves(final Donnees donnees, final List<String> classids, final Handler<String> handler) {
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean answer = new AtomicBoolean(false);
        final String thread = "idsResponsable -> " + classids.toString();
        final String method = "getBaliseEleves";

        // Récupération des élèves de la classe
        Promise<Message<JsonObject>> studentsPromise = Promise.promise() ;
        lsuService.getStudents(classids, studentsPromise, count, answer, thread, method);

        // Récupération des élèves à ignorer pour l'export
        Promise<JsonArray> ignoredStudentPromise = Promise.promise();
        lsuService.getUnheededStudents(new JsonArray(), new JsonArray(classids),
                unheededStudents -> formate(ignoredStudentPromise, unheededStudents));

        Future.all(studentsPromise.future(), ignoredStudentPromise.future()).onComplete( event -> {
            if(event.failed()) {
                handler. handle(event.cause().getMessage());
            }
            else {
                JsonArray allStudents = studentsPromise.future().result().body().getJsonArray("results");
                JsonArray ignoratedStudents = ignoredStudentPromise.future().result();

                // Suppression des élèves ignorés de la liste des élèves récupérés
                JsonArray jsonElevesRelatives = lsuService.filterUnheededStrudentsForBfc(allStudents,
                        ignoratedStudents);
                Eleve eleve = null;
                Donnees.Eleves eleves = objectFactory.createDonneesEleves();
                for (int i = 0; i < jsonElevesRelatives.size(); i++) {
                    JsonObject o = jsonElevesRelatives.getJsonObject(i);
                    Responsable responsable = null;
                    String idEleve = o.getString("idNeo4j");
                    if (!eleves.containIdEleve(idEleve)) {
                        String[] externalIdClass;
                        String className;
                        if (o.getString("externalIdClass") != null) {
                            externalIdClass = o.getString("externalIdClass").split("\\$");
                            className = externalIdClass[(externalIdClass.length - 1)];
                            try {
                                eleve = objectFactory.createEleve(o.getString("externalId"), o.getString("attachmentId"), o.getString("firstName"),
                                        o.getString("lastName"), className, o.getString("idNeo4j"), o.getString("idClass"), o.getString("level"));
                                eleves.getEleve().add(eleve);
                            } catch (Exception e) {
                                if(e instanceof NumberFormatException){
                                    log.error(" method getBaliseEleve : creationEleve " + e.getMessage() +
                                            "new BigInteger(attachmentId) is impossible attachmentId : " + o.getString("attachmentId"));
                                } else {
                                    // log for time-out
                                    answer.set(true);
                                    lsuService.serviceResponseOK(answer, count.get(), thread, method);
                                    handler.handle(e.getMessage());
                                    log.error(" method getBaliseEleve : creationEleve " + e.getMessage());
                                }
                            }
                        } else {
                            log.info("[EXPORT LSU]: remove " + o.getString("name")
                                    + o.getString("firstName"));
                        }
                    } else {
                        eleve = eleves.getEleveById(idEleve);
                    }

                    String codePostal = null, adress = null, commune = null;
                    try{
                        adress = o.getString("address");
                        commune = o.getString("city");
                        codePostal = o.getString("zipCode");
                    } catch (ClassCastException e) {
                        codePostal = String.valueOf(o.getInteger("zipCode"));
                    }

                    adress = (adress == null || adress.isEmpty()) ? "inconnue" : adress;
                    commune = (commune == null || commune.isEmpty()) ? "inconnue" : commune;
                    codePostal = (codePostal == null || codePostal.isEmpty()) ? "inconnu" : codePostal;

                    if(codePostal.length() > 10){
                        codePostal = codePostal.substring(0, 10);
                    }

                    if(commune.length() > 100){
                        commune = commune.substring(0, 100);
                    }

                    Adresse adresse = objectFactory.createAdresse(adress, codePostal, commune);

                    if (o.getString("externalIdRelative")!= null && o.getString("lastNameRelative") !=null &&
                            o.getString("firstNameRelative")!= null && o.getJsonArray("relative").size() > 0) {
                        JsonArray relatives = o.getJsonArray("relative");

                        String civilite = o.getString("civilite");

                        for (int j = 0; j < relatives.size(); j++) {
                            String relative = relatives.getString(j);
                            String[] paramRelative = relative.toString().split("\\$");
                            //création d'un responsable Eleve avec la civilite si MERE ou PERE

                            if (o.getString("externalIdRelative").equals(paramRelative[0])) {
                                if (adresse != null) {
                                    responsable = objectFactory.createResponsable(o.getString("externalIdRelative"),
                                            o.getString("lastNameRelative"),
                                            o.getString("firstNameRelative"), relative, adresse);
                                } else {
                                    responsable = objectFactory.createResponsable(o.getString("externalIdRelative"),
                                            o.getString("lastNameRelative"),
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
                lsuService.serviceResponseOK(answer, count.get(), thread, method);
                handler.handle("success");
                log.info("FIN method getBaliseEleves : nombre d'eleve ajoutes :"+eleves.getEleve().size());

            }
        });
    }

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

    private XMLGregorianCalendar getDateFormatGregorian(Object dateOrString) {
        XMLGregorianCalendar dateGregorian = null;
        try {
            SimpleDateFormat dfYYYYMMdd = new SimpleDateFormat("yyyy-MM-dd");
            Date date = null;
            if(dateOrString instanceof String) {
                date = dfYYYYMMdd.parse((String) dateOrString);
            }else{
                date = (Date)dateOrString;
            }
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            dateGregorian = DatatypeFactory.newInstance().newXMLGregorianCalendar();
            dateGregorian.setYear(cal.get(Calendar.YEAR));
            dateGregorian.setMonth(cal.get(Calendar.MONTH) + 1);
            dateGregorian.setDay(cal.get(Calendar.DATE));
        } catch (DatatypeConfigurationException | ParseException e) {
            log.error("getDateFormatGregorian : " + e.getMessage());
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
                                         final Map<String, Map<Long, Integer>> mapIdEleveIdDomainePosition,
                                         final Long valueCycle,
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
                    if (syntheseEleve.size() > 0 && (mapIdDomainePosition.size() == mapIdDomaineCodeDomaine.size() ||
                            bmapSansIdDomaineCPDETR)) {

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
                                EnseignementComplement enseignementComplement = new EnseignementComplement(ensCplEleve.getString("code"),
                                        ensCplEleve.getInteger("niveau"));
                                bilanCycle.setEnseignementComplement(enseignementComplement);

                                CodeLangueCultureRegionale codeLangueCultureRegionale = null;
                                Integer niveauLcr = null;
                                try {
                                    String codeLCR = ensCplEleve.getString("code_lcr");
                                    if(codeLCR != null) {
                                        codeLangueCultureRegionale = CodeLangueCultureRegionale.fromValue(codeLCR);
                                        niveauLcr = ensCplEleve.getInteger("niveau_lcr");

                                        if (codeLangueCultureRegionale != null
                                                && !codeLangueCultureRegionale.equals(CodeLangueCultureRegionale.AUC)
                                                && niveauLcr != null) {
                                            LangueCultureRegionale langueCultureRegionale = new LangueCultureRegionale();
                                            langueCultureRegionale.setCode(codeLangueCultureRegionale);
                                            langueCultureRegionale.setPositionnement(BigInteger.valueOf(niveauLcr));
                                            bilanCycle.setLangueCultureRegionale(langueCultureRegionale);
                                        }
                                    }
                                } catch (Exception e) {
                                    log.error("error setting langueCultureRegionale fo user " + idEleve, e);
                                    log.error("codeLangueCultureRegionale : "+ codeLangueCultureRegionale);
                                    log.error("niveauLcr : " + niveauLcr);
                                }
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
                        String messageError = "";
                        if (mapIdDomainePosition.size() != mapIdDomaineCodeDomaine.size() && !bmapSansIdDomaineCPDETR ) {
                            messageError = getLibelle("evaluation.lsu.error.no.domain");
                            setError(errorsExport, eleve, messageError, null);
                            if (syntheseEleve.size() == 0) {
                                messageError = getLibelle("evaluation.lsu.error.no.syntheseBFC");
                                setError(errorsExport, eleve, messageError, null);
                            }
                        } else if (syntheseEleve.size() == 0) {
                            messageError = getLibelle("evaluation.lsu.error.no.syntheseBFC");
                            setError(errorsExport, eleve,messageError, null);
                        }
                    }
                } else {//si l'élève n'est pas dans la map alors il n'a aucune évaluation et
                    // il faut le supprimer du xml donc de la list des élèves de la balise ELEVES
                    eleves.getEleve().remove(eleve);
                    String messageError = getLibelle("evaluation.lsu.error.no.common.domain");
                    setError(errorsExport, eleve, messageError, null);
                    if(!syntheseEleve.containsKey("id_eleve")){
                        messageError = getLibelle("evaluation.lsu.error.no.syntheseBFC");
                        setError(errorsExport, eleve, messageError, null);
                    }
                }
            }else {
                log.info("eleve qui n'est pas dans la list des eleves " + idEleve);
            }
        }
    }



    /**
     * Construit la map des résultats des élèves en tenant compte de la dispense des domaines
     * Map<idEleve,Map<idDomaine,positionnement>>
     * @param listIdsEleve listIdsEleve
     * @param idClass idClass
     * @param idStructure idStructure
     * @param idCycle idCycle
     * @param handler handler
     */
    private void getResultsElevesByDomaine( List<String> listIdsEleve, String idClass, String idStructure, Long idCycle,
                                            Handler <Either<String,  Map<String, Map<Long, Integer>>>> handler){
        Map<String, Map<Long, Integer>> resultatsEleves = new HashMap<>();
        final String[] idsEleve = listIdsEleve.toArray(new String[listIdsEleve.size()]);
        AtomicBoolean answer = new AtomicBoolean(false);
        AtomicInteger count = new AtomicInteger(0);
        final String thread = "(" + idClass + ", " + idStructure + ", " + idCycle + ")";
        final String method = "getResultsElevesByDomaine";

        bfcService.buildBFC(false, idsEleve, idClass, idStructure, null, idCycle, false,
                new Handler<Either<String, JsonObject>>() {
                    @Override
                    public void handle(final Either<String, JsonObject> repBuildBFC) {
                        if (repBuildBFC.isLeft()) {
                            String error = repBuildBFC.left().getValue();
                            lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                            if (error!=null && error.contains(TIME)){
                                bfcService.buildBFC(false, idsEleve, idClass, idStructure, null,
                                        idCycle, false,this);
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
                            dispenseDomaineEleveService.mapOfDispenseDomaineByIdEleve(listIdsEleve,
                                    new Handler<Either<String, Map<String, Map<Long, Boolean>>>>() {
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
                                                final List<Future<JsonObject>> futureDispensesElevesList = new ArrayList<>();
                                                for (String idEleve : idsEleve) {
                                                    Promise<JsonObject> dispenseElevePromise = Promise.promise();
                                                    futureDispensesElevesList.add(dispenseElevePromise.future());
                                                    JsonArray resultats = repBuildBFC.right().getValue().getJsonArray(idEleve);
                                                    Map<Long, Integer> resultEleves = new HashMap<>();

                                                    // si pas de resultats, on passe a l'élève suivant
                                                    if (resultats == null) {
                                                        dispenseElevePromise.complete();
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
                                                    dispenseElevePromise.complete();
                                                }
                                                Future.all(futureDispensesElevesList).onComplete(
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

        final Donnees.Eleves eleves = donnees.getEleves();
        Integer nbElevesTotal = eleves.getEleve().size();
        final String millesime = getMillesimeBFC();
        final AtomicInteger nbEleveCompteur = new AtomicInteger(0);
        final Map<String, List<String>> mapIdClassIdsEleve = eleves.getMapIdClassIdsEleve();
        final Map<String, Long> mapIdClassIdCycle = listMapClassCycle.get(0);//map<IdClass,IdCycle>
        final Map<Long, Long> mapIdCycleValue = listMapClassCycle.get(1);//map<IdCycle,ValueCycle>

        log.info("DEBUT : method getBaliseBilansCycle : nombreEleve : "+eleves.getEleve().size());
        AtomicBoolean answer = new AtomicBoolean(false);
        AtomicInteger count = new AtomicInteger(0);
        final String thread = "("  + idStructure +  ")";
        final String method = "getBaliseBilansCycle";
        if(eleves.getEleve().isEmpty()){
            handler.handle("getEleves : no student");
            log.info("FIN method getBaliseBilansCycle Aucun eleve");
            return;
        }
        donnees.setBilansCycle(objectFactory.createDonneesBilansCycle());
        //on parcourt les classes
        for (final Map.Entry<String, List<String>> listIdsEleve : mapIdClassIdsEleve.entrySet()) {
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
                                                            if (nbEleveCompteur.intValue() == nbElevesTotal) {
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
            valueOf(externalId);
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
        Promise<Map<String, String>> libelleCourtPromise = Promise.promise();
        new DefaultMatiereService().getLibellesCourtsMatieres(false, event -> {
            formate(libelleCourtPromise, event);
        });
        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatieresForUser")
                .put("userType", "Personnel")
                .put("idUser", "null")
                .put("idStructure", idStructure)
                .put("onlyId", false);
        Promise<JsonArray> disciplinesPromise = Promise.promise();
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
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
                                disciplinesPromise.complete(listSubject);
                                answer.set(true);
                                lsuService.serviceResponseOK(answer, count.get(), thread, method);
                            } catch (Throwable e) {
                                disciplinesPromise.fail("method getBaliseResponsable : " + e.getMessage());
                            }
                        } else {
                            String error = body.getString(MESSAGE);
                            lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                            if (error!=null && error.contains(TIME)) {
                                eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                        handlerToAsyncHandler(this));
                            }
                            else {
                                String failureMessage = "getBaliseDisciplines discipline :" +
                                        " error eb matiere.getMatieresForUser ko";
                                disciplinesPromise.fail(failureMessage);
                            }
                        }
                    }}));

        Future.all(libelleCourtPromise.future(), disciplinesPromise.future()).onComplete(event -> {
            if(event.failed()){
                handler.handle(event.cause().getMessage());
            }
            else{
                setDisciplineForStructure(donnees, disciplinesPromise.future().result(), libelleCourtPromise.future().result(), handler);
            }

        });
    }

    private void getBalisePeriodes(final Donnees donnees, final List<Integer> wantedPeriodes, final Map<String, JsonArray> periodesByClass,
                                   final String idStructure, final List<String> idClasse, final Handler<String> handler) {
        utilsService.getPeriodes(idClasse, idStructure, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> responsePeriodes) {
                AtomicBoolean answer = new AtomicBoolean(false);
                AtomicInteger count = new AtomicInteger(0);
                final String thread = "("  + idStructure + ", "+ idClasse.toString()+ " )";
                final String method = "getBalisePeriodes";

                if(responsePeriodes.isLeft()){
                    String error = responsePeriodes.left().getValue();
                    lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                    if (error!=null && error.contains(TIME)) {
                        utilsService.getPeriodes(idClasse, idStructure,this);
                    }
                    else {
                        log.error("method getBalisePeriodes : error eb periode.getPeriodes ko");
                        handler.handle("getBalisePeriodes : error eb periode.getPeriodes ko");
                    }
                }else{
                    try {
                        JsonArray periodeList = responsePeriodes.right().getValue();
                        donnees.setPeriodes(objectFactory.createDonneesPeriodes());
                        periodeList.forEach(item -> {
                            JsonObject currentPeriode = (JsonObject) item;
                            Integer targetPeriode = wantedPeriodes.stream()
                                    .filter(el -> el == currentPeriode.getInteger("id_type"))
                                    .findFirst()
                                    .orElse(null);
                            if (targetPeriode != null) {
                                String millesime = getMillesimeBFC();
                                int indice = 0;
                                int nbPeriode = 0;
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
                }
            }
        });
    }

    private void getGroupsClass(List<String> idsClasses, List<String> idsGroupsClasses,
                                Map<String, JsonArray> mapIdsGroupsClasses, final Handler<String> handler){
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getGroupesClasse")
                .put("idClasses", idsClasses);

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    int count = 0;
                    AtomicBoolean answer = new AtomicBoolean(false);
                    final String thread = "idsClasses -> " + idsClasses;
                    final String method = "getGroupsClass";
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        if ("ok".equals(body.getString("status")) && !body.getJsonArray("results").isEmpty()) {
                            // log for time-out
                            answer.set(true);
                            lsuService.serviceResponseOK(answer, count, thread, method);
                            JsonArray groupsClassResult = body.getJsonArray(RESULTS);

                            if (groupsClassResult != null && !groupsClassResult.isEmpty()) {
                                for(int i = 0; i < groupsClassResult.size() ; i++){
                                    String idClass = groupsClassResult.getJsonObject(i).getString("id_classe");
                                    JsonArray idsGroup = groupsClassResult.getJsonObject(i).getJsonArray("id_groupes");

                                    idsGroupsClasses.add(idClass);
                                    if (idsGroup != null && !idsGroup.isEmpty()) {
                                        idsGroupsClasses.addAll(idsGroup.getList());
                                    }

                                    mapIdsGroupsClasses.put(idClass, new JsonArray(idsGroupsClasses));
                                }

                                handler.handle("success");
                            } else {
                                idsGroupsClasses.addAll(idsClasses);
                            }
                        } else {
                            String error = body.getString(MESSAGE);
                            count ++;
                            if(error !=null && error.contains(TIME)){
                                eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                        handlerToAsyncHandler(this));
                            } else {
                                handler.handle("method getGroupsClass : error when collecting Groups  " + error);
                                log.error("An error occured when collecting Groups for " + idsClasses + " Classes");
                            }
                            lsuService.serviceResponseOK(answer, count, thread, method);
                        }
                    }
                }));
    }

    private void getCycle(List<String> idsClasses, final Map<String, Integer> cyclesByClasse, final Handler<String> handler){
        utilsService.getCycle(idsClasses, event -> {
            if(event.isLeft()) {
                log.error("method getCycle : error when collecting cycles  " + event.left().getValue());
                handler.handle("failed");
            } else {
                JsonArray cycleResult = event.right().getValue();
                for(Object cycle : cycleResult) {
                    JsonObject cycleJson = (JsonObject) cycle;
                    cyclesByClasse.put(cycleJson.getString("id_groupe"), cycleJson.getInteger("value_cycle"));
                }
                handler.handle("success");
            }
        });
    }

    private void getHeadTeachers( List<String> idsClasse,
                                  Map<String, JsonArray> mapIdClasseHeadTeachers,
                                  final Handler<String> handler){
        List<Future<Void>> listFutureClasses = new ArrayList<>();
        for(String idClass : idsClasse){
            Promise<Void> classPromise = Promise.promise();
            listFutureClasses.add(classPromise.future());
            JsonObject action = new JsonObject();
            action.put("action","classe.getHeadTeachersClasse").put("idClasse", idClass);
            eb.request(Competences.VIESCO_BUS_ADDRESS,action,Competences.DELIVERY_OPTIONS, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
                            eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                    handlerToAsyncHandler(this));
                        }
                        else {
                            // log for time-out
                            answer.set(true);
                            classPromise.complete();
                        }
                        lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);

                    }else{
                        JsonArray headTeachers = body.getJsonArray("results");
                        mapIdClasseHeadTeachers.put(idClass,headTeachers);
                        // log for time-out
                        answer.set(true);
                        lsuService.serviceResponseOK(answer, count.get(), thread, method);
                        classPromise.complete();

                    }

                }
            }));


        }
        Future.all(listFutureClasses).onComplete(
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
                            List<Future<JsonObject>> listFutureTable = new ArrayList<>();


                            for (int i = 0; i < allTablesConversion.size(); i++) {

                                Promise<JsonObject> tablePromise = Promise.promise();
                                listFutureTable.add(tablePromise.future());

                                JsonObject tableConversion = allTablesConversion.getJsonObject(i);
                                if (tableConversionByClass != null && !tableConversion.isEmpty()
                                        && tableConversion.containsKey("id_groupe")
                                        && tableConversion.getString("id_groupe") != null) {
                                    if( !tableConversionByClass.containsKey(tableConversion.getString("id_groupe"))){
                                        tableConversionByClass.put(tableConversion.getString("id_groupe"),
                                                new JsonArray(tableConversion.getString("table_conversion")));
                                    }
                                }
                                tablePromise.complete();
                            }
                            lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                            Future.all(listFutureTable).onComplete(event -> {
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


    private void getBaliseEnseignantFromId(final Donnees donnees, String idEnseignant,final JsonArray enseignantFromSts,
                                           final Handler<String> handler) {
        final JsonArray ids = new JsonArray().add(idEnseignant);

        Utils.getLastNameFirstNameUser(eb, ids, mapResponseTeacher -> {
            AtomicBoolean answer = new AtomicBoolean(false);
            AtomicInteger count = new AtomicInteger(0);
            String method = "getLastNameFirestNameUser" ;
            String thread = " id_teacher " + ids;
            if(mapResponseTeacher.isLeft()){
                String error = mapResponseTeacher.left().getValue();
                if(error != null && error.contains(TIME)){
                    Utils.getLastNameFirstNameUser(eb, ids, (Handler<Either<String, Map<String, JsonObject>>>) this);
                }else{
                    handler.handle(error);
                }
            }else{
                Map<String, JsonObject> mapIdJoTeacher = mapResponseTeacher.right().getValue();
                mapIdJoTeacher.forEach((k,jsonObjectTeacher)-> {
                    addorFindTeacherBalise(donnees, enseignantFromSts, jsonObjectTeacher);
                });
                handler.handle("success");
            }
            lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
        });
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
                JsonObject enseigantFromSts = (JsonObject) enseignantsFromSts.stream().filter(el ->
                        (((JsonObject) el).getString("NOM_USAGE") != null &&
                                ((JsonObject) el).getString("NOM_USAGE").equals(enseignant.getString("lastName")) ||
                                ((JsonObject) el).getString("NOM_USAGE").equals(enseignant.getString("name"))) &&
                                ((JsonObject) el).getString("PRENOM") != null &&
                                ((JsonObject) el).getString("PRENOM").equals(enseignant.getString("firstName")) &&
                                (((JsonObject) el).getString("DATE_NAISSANCE")== null ||
                                        ((JsonObject) el).getString("DATE_NAISSANCE") != null &&
                                                ((JsonObject) el).getString("DATE_NAISSANCE").equals(enseignant.getString("birthDate")))
                ).findFirst().orElse(null);
                if (enseigantFromSts != null && enseigantFromSts.containsKey("ID") && enseigantFromSts.containsKey("TYPE")) {
                    existing.setIdSts(new BigInteger(enseigantFromSts.getString("ID")));
                    existing.setType(TypeEnseignant.fromValue(enseigantFromSts.getString("TYPE")));
                }
            }

            if(existing.getId() != null && existing.getIdSts() != null ){
                if(donnees.getEnseignants() == null){
                    donnees.setEnseignants(objectFactory.createDonneesEnseignants());
                }
                donnees.getEnseignants().getEnseignant().add(existing);
            }
        }
        return existing;
    };

    private void getApEpiParcoursBalises(final Donnees donnees, final List<String> groupsClass, final String idStructure,
                                         JsonObject epiGroupAdded, final JsonArray enseignantFromSts,
                                         final Handler<String> handler) {
        elementBilanPeriodiqueService.getElementsBilanPeriodique(null, groupsClass, idStructure,
                new Handler<Either<String, JsonArray>>() {
                    AtomicBoolean answer = new AtomicBoolean(false);
                    AtomicInteger count = new AtomicInteger(0);
                    final String thread = "(" + idStructure + ", " + groupsClass.toString() + ")";
                    final String method = "getApEpiParcoursBalises";
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            JsonArray elementBilanPeriodique = event.right().getValue();
                            if (elementBilanPeriodique == null || elementBilanPeriodique.isEmpty()) {
                                answer.set(true);
                                handler.handle("success");
                                log.info(" getElementsBilanPeriodique in getApEpiParcoursBalises");
                            } else {
                                final List<Future<JsonObject>> futuresListApEpiParcours = new ArrayList<>();

                                for (int i = 0; i < elementBilanPeriodique.size(); i++) {
                                    final Promise<JsonObject> eltBilanPeriodiquePromise = Promise.promise();
                                    futuresListApEpiParcours.add(eltBilanPeriodiquePromise.future());
                                    JsonObject element = elementBilanPeriodique.getJsonObject(i);
                                    if (element != null) {
                                        Long typeElement = element.getLong("type");
                                        if (3L == typeElement) { //parcours group
                                            addParcoursGroup(element, eltBilanPeriodiquePromise);
                                        } else if (2L == typeElement) { //ap class/group
                                            addAccGroup(element, eltBilanPeriodiquePromise);
                                        } else if (1L == typeElement) { //epi group
                                            addEpiGroup(element, epiGroupAdded, eltBilanPeriodiquePromise);
                                        }
                                    }
                                }
                                Future.all(futuresListApEpiParcours).onComplete(eventFutureApEpiParcours -> {
                                    handler.handle("success");
                                });
                            }
                        } else {
                            String error = event.left().getValue();
                            if(error != null && error.contains(TIME)){
                                elementBilanPeriodiqueService.getElementsBilanPeriodique(null, groupsClass,
                                        idStructure, this);
                            } else {
                                answer.set(true);
                                handler.handle("getApEpiParcoursBalises no data available ");
                                log.info("event is not Right getElementsBilanPeriodique in getApEpiParcoursBalises");
                            }
                            lsuService.serviceResponseOK(answer,count.incrementAndGet(), thread, method);
                        }
                    }

                    private void addParcoursGroup(JsonObject element, Promise eltBilanPeriodiquePromise) {
                        if(donnees.getParcoursCommuns() == null){
                            donnees.setParcoursCommuns(objectFactory.createDonneesParcoursCommuns());
                        }
                        List<Periode> periodes = donnees.getPeriodes().getPeriode();

                        for (int k = 0; k < periodes.size(); k++) {
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
                        eltBilanPeriodiquePromise.complete();
                    }

                    private void addEpiGroup(JsonObject element, JsonObject epiGroupAdded, Promise eltBilanPeriodiquePromise) {
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
                            EpiThematique epiThematique = objectFactory.createEpiThematique();
                            EpiGroupe epiGroupe = objectFactory.createEpiGroupe();
                            JsonObject theme = element.getJsonObject(Field.THEME);

                            String description = element.getString(Field.DESCRIPTION);
                            epi.setId(String.format("%s_%s", Field.EPI, element.getInteger(Field.ID)));
                            epi.setIntitule(theme.getString(Field.LIBELLE));
                            epi.setDescription(description != null && !"".equals(description.trim()) ? description : null);

                            if (ThematiqueEpi.contains(theme.getString("code"))) {
                                epi.setThematique(theme.getString("code"));
                            } else {
                                epiThematique.setCode(theme.getString("code"));
                                epiThematique.setLibelle(theme.getString("libelle"));
                                epi.setThematique(epiThematique.getCode());
                            }

                            EpiGroupe.EnseignantsDisciplines enseignantsDisciplinesEpi = objectFactory.createEpiGroupeEnseignantsDisciplines();

                            JsonArray intervenantsMatieres = element.getJsonArray("intervenantsMatieres");
                            final List<Future<JsonObject>> futureMyResponse1Lst = new ArrayList<>();

                            for (int j = 0; j < intervenantsMatieres.size(); j++) {
                                final Promise<JsonObject> resp1Promise = Promise.promise();
                                futureMyResponse1Lst.add(resp1Promise.future());
                                addEnseignantDiscipline(intervenantsMatieres.getJsonObject(j),
                                        enseignantsDisciplinesEpi.getEnseignantDiscipline(), epi.getDisciplineRefs(),
                                        donnees, enseignantFromSts, resp1Promise);
                            }

                            Future.all(futureMyResponse1Lst).onComplete(event -> {
                                epiGroupe.setId(EPI_GROUPE + element.getInteger("id"));
                                epiGroupe.setEpiRef(epi);
                                String libelle = element.getString("libelle");
                                epiGroupe.setEnseignantsDisciplines(enseignantsDisciplinesEpi);
                                epiGroupe.setIntitule(libelle);
                                JsonArray groupes = element.getJsonArray("groupes");

                                // Si l'EPI a moins de deux (intervenant-disciplines)
                                if(enseignantsDisciplinesEpi != null &&
                                        enseignantsDisciplinesEpi.getEnseignantDiscipline().size() < 2){
                                    String errorEPITeachersKey = "errorEPITeachers";
                                    if(!errorsExport.containsKey(errorEPITeachersKey)){
                                        errorsExport.put(errorEPITeachersKey, new JsonArray());
                                    }
                                    errorsExport.getJsonArray(errorEPITeachersKey).add(new JsonObject()
                                            .put(NAME,libelle)
                                            .put("groupes", groupes)
                                            .put("intervenantsMatieres", intervenantsMatieres));
                                    eltBilanPeriodiquePromise.complete();
                                    return;
                                }

                                for (int i = 0; i < groupes.size(); i++) {
                                    JsonObject currentClass = element.getJsonArray("groupes").getJsonObject(i);
                                    epiGroupAdded.put(currentClass.getString("id"), epiGroupe.getId());
                                }

                                if(epiThematique.getCode() != null){
                                    if(donnees.getEpisThematiques() == null){
                                        donnees.setEpisThematiques(objectFactory.createEpisThematiques());
                                    }
                                    donnees.getEpisThematiques().getEpiThematique().add(epiThematique);
                                }

                                if(donnees.getEpis() == null){
                                    donnees.setEpis(objectFactory.createDonneesEpis());
                                }
                                if(!donnees.getEpis().contains(epi)){
                                    donnees.getEpis().getEpi().add(epi);
                                }

                                if(donnees.getEpisGroupes() == null){
                                    donnees.setEpisGroupes(objectFactory.createDonneesEpisGroupes());
                                }
                                donnees.getEpisGroupes().getEpiGroupe().add(epiGroupe);

                                eltBilanPeriodiquePromise.complete();
                            });
                        }
                    }

                    private void addEnseignantDiscipline(JsonObject currentIntervenantMatiere,
                                                         List<EnseignantDiscipline> enseignantDiscipline,
                                                         List<Object> disciplineRefs, final Donnees donnees,
                                                         final JsonArray enseignantFromSts, final Promise<JsonObject> resp1Promise) {
                        Discipline currentSubj = getDisciplineInXML(currentIntervenantMatiere.getJsonObject("matiere").getString("id"), donnees);
                        if (currentSubj != null) {
                            Enseignant currentEnseignant = getEnseignantInXML(
                                    currentIntervenantMatiere.getJsonObject("intervenant").getString("id"),
                                    donnees);
                            if (currentEnseignant == null) {
                                getBaliseEnseignantFromId(donnees,
                                        currentIntervenantMatiere.getJsonObject("intervenant").getString("id"), enseignantFromSts,
                                        (String event) -> {
                                            if (event.equals("success")) {
                                                Enseignant newEnseignant = getEnseignantInXML(
                                                        currentIntervenantMatiere.getJsonObject("intervenant").getString("id"), donnees);
                                                finalInsertAddEnseignantDiscipline(enseignantDiscipline, disciplineRefs,
                                                        resp1Promise, currentSubj, newEnseignant);
                                            }
                                        });
                            }else {
                                finalInsertAddEnseignantDiscipline(enseignantDiscipline, disciplineRefs, resp1Promise, currentSubj, currentEnseignant);
                            }
                            lsuService.addIdsEvaluatedDiscipline(currentSubj.getId().replaceAll(DISCIPLINE_KEY, ""));
                        } else {
                            log.info("addEnseignantDiscipline no completed " + currentIntervenantMatiere.getJsonObject("intervenant").getString("displayName"));
                            resp1Promise.complete();
                        }
                    }

                    private void finalInsertAddEnseignantDiscipline(List<EnseignantDiscipline> enseignantDiscipline, List<Object> disciplineRefs,
                                                                    final Promise<JsonObject> resp1Promise, Discipline currentSubj,
                                                                    Enseignant currentEnseignant) {
                        if (currentEnseignant != null) {
                            EnseignantDiscipline currentEnseignantDiscipline = objectFactory.createEnseignantDiscipline();
                            currentEnseignantDiscipline.setDisciplineRef(currentSubj);
                            currentEnseignantDiscipline.setEnseignantRef(currentEnseignant);
                            boolean hasDiscipline = enseignantDiscipline.stream().anyMatch((teacher) ->
                                    ((Discipline)teacher.getDisciplineRef()).getId().equals(currentSubj.getId()));
                            if(!hasDiscipline) {
                                enseignantDiscipline.add(currentEnseignantDiscipline);
                            }
                            // ajout sans doublon sinon rejet de LSU
                            if(!disciplineRefs.contains(currentSubj)) {
                                disciplineRefs.add(currentSubj);
                            }
                        }
                        resp1Promise.complete();
                    }

                    private void addAccGroup(JsonObject element, Promise eltBilanPeriodiquePromise) {
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

                            AccPersoGroupe.EnseignantsDisciplines enseignantsDisciplinesAcc =
                                    objectFactory.createAccPersoGroupeEnseignantsDisciplines();

                            JsonArray intervenantsMatieres = element.getJsonArray("intervenantsMatieres");
                            final List<Future<JsonObject>> futureMyResponse1Lst = new ArrayList<>();

                            for (int i = 0; i < intervenantsMatieres.size(); i++) {
                                final Promise<JsonObject> resp1Promise = Promise.promise();
                                futureMyResponse1Lst.add(resp1Promise.future());
                                addEnseignantDiscipline(intervenantsMatieres.getJsonObject(i),
                                        enseignantsDisciplinesAcc.getEnseignantDiscipline(), accPerso.getDisciplineRefs(),
                                        donnees, enseignantFromSts, resp1Promise);
                            }

                            Future.all(futureMyResponse1Lst).onComplete(event -> {
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
                                eltBilanPeriodiquePromise.complete();
                            });
                        }
                    }
                });
    }

    private void getBaliseCompetencesNumeriqueCommun(final Donnees donnees, final List<String> idsClassCycle3,
                                                     final Handler<String> handler){
        classAppreciationDigitalSkillsService.getAppreciationsClasses(idsClassCycle3,
                new Handler<Either<String, JsonArray>>() {
                    final AtomicBoolean answer = new AtomicBoolean(false);
                    final AtomicInteger count = new AtomicInteger(0);
                    final String thread = "( classeCycle3 " + idsClassCycle3.toString() + ")";
                    final String method = "getBaliseCompetencesNumeriqueCommun";
                    @Override
                    public void handle (Either<String, JsonArray> eventAppsClasses) {
                        if(eventAppsClasses.isLeft()){
                            String error = eventAppsClasses.left().getValue();
                            if(error != null && error.contains(TIME)){
                                classAppreciationDigitalSkillsService.getAppreciationsClasses(idsClassCycle3,this );
                            } else {
                                answer.set(true);
                                handler.handle("getBaliseCompetencesNumeriqueCommun no data available ");
                                log.info("event is not Right getAppreciationsClasses in getBaliseCompetencesNumeriqueCommun");
                            }
                            lsuService.serviceResponseOK(answer, count.get(), thread, method);

                        } else {

                            JsonArray appreciationsClasses = eventAppsClasses.right().getValue();
                            if( appreciationsClasses != null && appreciationsClasses.isEmpty()){
                                answer.set(true);
                                handler.handle("success");
                                log.info(" no appNumeriqueClass getAppreciationsClasses in getBaliseCompetencesNumeriqueCommun");
                            } else {
                                donnees.setCompetencesNumeriquesCommuns(objectFactory.createDonneesCompetencesNumeriquesCommuns());
                                appreciationsClasses.forEach(  appClass -> {
                                            JsonObject o_appClass = (JsonObject) appClass;
                                            CompetencesNumeriquesCommun competencesNumeriquesCommun = objectFactory.createCompetencesNumeriquesCommun();
                                            competencesNumeriquesCommun.setId("CNC_" + o_appClass.getLong("id").toString());
                                            competencesNumeriquesCommun.setValue(o_appClass.getString("appreciation"));
                                            competencesNumeriquesCommun.setCodeStructure(getCodeDivision(donnees,
                                                    o_appClass.getString("class_or_group_id")));
                                            competencesNumeriquesCommun.setTypeStructure(TypeStructure.fromValue(o_appClass.getString("type_structure")));
                                            donnees.getCompetencesNumeriquesCommuns().getCompetencesNumeriquesCommun().add(competencesNumeriquesCommun);
                                        }
                                );
                                handler.handle("success");
                            }
                        }
                    }

                    private String getCodeDivision(Donnees donnees, String appClass) {
                        Eleve studentFind = donnees.getEleves().getEleve().stream().filter( student -> student.getId_Class().equals(appClass))
                                .findFirst().get();
                        return studentFind.getCodeDivision();
                    }
                });

    }

    private boolean hasClassesWithCycle3 (final Donnees donnees, final List<String> idsClass,
                                          List<String> idsClassCycle3, final Map<String, Integer> cyclesByClass){

        Periode findLastPeriod = donnees.getPeriodes().getPeriode().stream().filter(period ->
                period.getIndice() == period.getNbPeriodes()).findFirst().orElse(null);

        idsClass.forEach( idClass -> {
            Integer cycleClass = cyclesByClass.get(idClass);
            if( cycleClass.equals(LevelCycle.CYCLE3.getValue())) idsClassCycle3.add(idClass);
        });

        if(findLastPeriod != null && !idsClassCycle3.isEmpty() ){
            return true;
        }

        return false;
    }

    private void addVieScolairePerso(JsonArray viescoResult, Periode currentPeriode, BilanPeriodique bilanPeriodique) {
        VieScolaire viescolaire = objectFactory.createVieScolaire();
        viescolaire.setNbAbsInjustifiees(BigInteger.valueOf(0L));
        viescolaire.setNbAbsJustifiees(BigInteger.valueOf(0L));
        viescolaire.setNbRetards(BigInteger.valueOf(0L));
        viescolaire.setNbHeuresManquees(BigInteger.valueOf(0L));

        JsonObject viesco = (JsonObject) viescoResult.stream()
                .filter(el -> currentPeriode.getTypePeriode() == ((JsonObject) el).getInteger("id_periode"))
                .findFirst().orElse(null);

        if (viesco != null && viesco.containsKey("id_eleve")) {
            if (viesco.getLong("abs_non_just") != null) {
                viescolaire.setNbAbsInjustifiees(BigInteger.valueOf(viesco.getLong("abs_non_just")));
            }
            if (viesco.getLong("abs_just") != null) {
                viescolaire.setNbAbsJustifiees(BigInteger.valueOf(viesco.getLong("abs_just")));
            }
            if (viesco.getLong("retard") != null) {
                viescolaire.setNbRetards(BigInteger.valueOf(viesco.getLong("retard")));
            }
            if (viesco.getLong("abs_totale_heure") != null) {
                viescolaire.setNbHeuresManquees(BigInteger.valueOf(viesco.getLong("abs_totale_heure")));
            }
        }
        bilanPeriodique.setVieScolaire(viescolaire);
    }

    /**
     * permet de completer tous les attributs de la balise BilansPeriodiques et de la setter à donnees
     * @param donnees     permet de recuperer les eleves
     * @param periodesByClasse map class with her periode list
     * @param tableConversionByClasse map class with conversion table
     * @param handler
     */
    private void getBaliseBilansPeriodiques(final Donnees donnees, final String idStructure, final List<String> idsGroupsClass,
                                            final Map<String, JsonArray> periodesByClasse, final Map<String, Integer> cyclesByClasse,
                                            final Map<String, JsonArray> tableConversionByClasse, final JsonArray enseignantFromSts,
                                            final Map<String, JsonArray> mapIdClassHeadTeachers, Map<Long, JsonObject> periodeUnheededStudents,
                                            final JsonArray retardsAndAbsences, final List<Service> services, final JsonArray multiTeachers,
                                            final Map<String, JsonArray> mapIdsGroupsClasses, final Handler<Either.Right<String, JsonObject>> handler) {
        final Donnees.BilansPeriodiques bilansPeriodiques = objectFactory.createDonneesBilansPeriodiques();
        final List<Eleve> eleves = donnees.getEleves().getEleve();
        final List<Periode> periodes = donnees.getPeriodes().getPeriode();
        final AtomicInteger nbIgnoratedStudents = new AtomicInteger(0);
        final AtomicInteger originalSize = new AtomicInteger();
        final AtomicInteger idElementProgramme = new AtomicInteger();
        boolean withDigitalSkillsError = Boolean.TRUE.equals(Competences.LSUN_CONFIG.getBoolean("withDigitalSkillsError"));
        Handler getOut = (Handler<Either<String, JsonObject>>) suiviAcquisResponse -> {
            originalSize.getAndDecrement();
            if (originalSize.get() == 0) {
                log.info("Get OUTTTTT (nb of BP) " + bilansPeriodiques.getBilanPeriodique().size()
                        + " + " + nbIgnoratedStudents.get() + " ignorated  ==  "
                        + eleves.size() + " (nf of student) * "
                        + periodes.size() + " periodes");
                donnees.setBilansPeriodiques(bilansPeriodiques);
                handler.handle(new Either.Right<>(suiviAcquisResponse.right().getValue()));
            }
        };

        if(!(eleves.size() > 0) || !(periodes.size() > 0)){
            handler.handle(new Either.Right<>(new JsonObject().put("error",
                    "getBaliseBilansPeriodiques : Eleves or Periodes are empty")));
            return;
        }

        donnees.setElementsProgramme(objectFactory.createDonneesElementsProgramme());
        //For each eleve create his periodic bilan
        originalSize.addAndGet(eleves.size() * periodes.size());
        for (int i = 0; i < eleves.size(); i++) {
            Eleve currentEleve = eleves.get(i);
            String idEleve = currentEleve.getIdNeo4j();
            String idClasse = currentEleve.getId_Class();
            Date createDateEleve = currentEleve.getCreatedDate();
            Date deletedDate = currentEleve.getDeleteDate();
            JsonArray headTeachers = mapIdClassHeadTeachers.get(idClasse);
            Integer currentCycle = cyclesByClasse.get(idClasse);
            JsonArray idClasseGroups = mapIdsGroupsClasses.get(idClasse);

            List<Service> servicesClasse = services.stream()
                    .filter(el -> idClasse.equals(el.getGroup().getId()) ||
                            (idClasseGroups.getList()).contains(el.getGroup().getId()))
                    .collect(Collectors.toList());

            for (int j = 0; j < periodes.size(); j++) {
                JsonObject response = new JsonObject();
                Periode currentPeriode = periodes.get(j);
                Long idPeriode = (long) currentPeriode.getTypePeriode();

                JsonArray periodesOfClass = periodesByClasse.get(idClasse);
                JsonObject periodeOfClass = utilsService.getObjectForPeriode(periodesOfClass,
                        idPeriode, "id_type");

                String dateStringDtPeriode = periodeOfClass.getString("timestamp_dt");
                String dateStringFnPeriode = periodeOfClass.getString("timestamp_fn");

                Date dateDtPeriode = UtilsConvert.convertStringToDate(dateStringDtPeriode, "yyyy-MM-dd");
                Date dateFnPeriode = UtilsConvert.convertStringToDate(dateStringFnPeriode, "yyyy-MM-dd");

                Boolean isgnorated = lsuService.isIgnorated(idEleve, idClasse, idPeriode, periodeUnheededStudents);

                JsonArray multiTeachersClasse = multiTeachers;

                if(isgnorated || !(createDateEleve == null || createDateEleve.before(dateFnPeriode)) &&
                        (deletedDate == null || deletedDate.after(dateDtPeriode))) {
                    nbIgnoratedStudents.incrementAndGet();
                    response.put("status", 200);
                    getOut.handle(new Either.Right<String, JsonObject>(response));
                } else{
                    Promise<JsonObject> getAppreciationsPromise = Promise.promise();
                    Promise<JsonObject> getSuiviAcquisPromise = Promise.promise();
                    Promise<JsonObject> getSynthesePromise = Promise.promise();
                    Promise<JsonObject> getDigitalSkillsPromise = Promise.promise();
                    final BilanPeriodique bilanPeriodique = objectFactory.createBilanPeriodique();

                    Future.all(getAppreciationsPromise.future(), getSuiviAcquisPromise.future(),
                            getSynthesePromise.future(), getDigitalSkillsPromise.future()).onComplete(event -> {
                        if (event.succeeded()) {
                            response.put("status", 200);
                            getOut.handle(new Either.Right<String, JsonObject>(response));
                        }
                    });

                    if (headTeachers != null && headTeachers.size() > 0) {
                        for (int k = 0; k < headTeachers.size(); k++) {
                            Enseignant headTeacher = addorFindTeacherBalise(donnees, enseignantFromSts, headTeachers.getJsonObject(k));
                            bilanPeriodique.getProfPrincRefs().add(headTeacher);
                        }
                    }

                    if(!addresponsableEtabRef(donnees, response, bilanPeriodique)){
                        response.put("status", 400);
                        response.put(MESSAGE, "Responsable do not exist on Bilan periodique balise : eleve : " +
                                idEleve + " " + currentEleve.getNom() + " periode : " + idPeriode);
                        getOut.handle(new Either.Right<String, JsonObject>(response));
                        return;
                    }

                    if(!addDatesBilanPeriodique(bilanPeriodique, currentEleve, currentPeriode, periodeOfClass)){
                        String messageError = getLibelle("evaluation.lsu.error.no.dates");
                        setError(errorsExport, currentEleve, messageError, null);
                    }

                    syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(idPeriode, idEleve, idStructure, new Handler<Either<String, JsonArray>>() {
                        AtomicBoolean answer = new AtomicBoolean(false);
                        AtomicInteger count = new AtomicInteger(0);
                        final String thread = "(" + currentEleve.getNom() + " " + currentEleve.getPrenom() + " )";
                        final String method = "getBaliseBilansPeriodiques |getSyntheseBilanPeriodique ";

                        @Override
                        public void handle(Either<String, JsonArray> eventSynthese) {
                            String synthese = "null";
                            if (eventSynthese.isRight()) {
                                JsonArray result = eventSynthese.right().getValue();
                                JsonObject rightValue = new JsonObject();
                                if(!result.isEmpty())
                                    rightValue = result.getJsonObject(0);
                                if ((rightValue != null) && rightValue.containsKey("synthese")
                                        && !rightValue.getString("synthese").isEmpty()) {
                                    synthese = rightValue.getString("synthese");
                                }
                                answer.set(true);
                            } else {
                                String messageError = getLibelle("evaluation.lsu.error.no.synthese") +
                                        currentPeriode.getLabel();
                                setError(errorsExport, currentEleve, messageError, null);
                                String error = eventSynthese.left().getValue();
                                if (error != null && error.contains(TIME)) {
                                    if (getSynthesePromise.future().isComplete()) {
                                        return;
                                    }
                                    syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(idPeriode, idEleve, idStructure, this);
                                } else {
                                    answer.set(true);
                                }
                            }
                            lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                            if (answer.get()) {
                                if (!"null".equals(synthese)) {
                                    bilanPeriodique.setAcquisConseils(synthese);
                                } else {
                                    String messageError = getLibelle("evaluation.lsu.error.no.synthese") +
                                            currentPeriode.getLabel();
                                    setError(errorsExport, currentEleve, messageError, null);
                                }
                                getSynthesePromise.complete();
                            }
                        }
                    });

                    JsonArray retardsAndAbsencesEleve = new JsonArray(retardsAndAbsences.stream()
                            .filter(el -> idEleve.equals(((JsonObject) el).getString("id_eleve")))
                            .collect(Collectors.toList()));
                    addVieScolairePerso(retardsAndAbsencesEleve, currentPeriode, bilanPeriodique);

                    elementBilanPeriodiqueService.getApprecBilanPerEleve(idsGroupsClass,
                            Integer.toString(currentPeriode.getTypePeriode()), null, idEleve,
                            new Handler<Either<String, JsonArray>>() {
                                AtomicBoolean answer = new AtomicBoolean(false);
                                AtomicInteger count = new AtomicInteger(0);
                                final String thread = "(" + currentEleve.getNom() + " " + currentEleve.getPrenom() + " )";
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
                                    } else {
                                        String error = eventApp.left().getValue();
                                        if (error != null && error.contains(TIME)) {
                                            elementBilanPeriodiqueService.getApprecBilanPerEleve(
                                                    Collections.singletonList(idClasse),
                                                    Integer.toString(currentPeriode.getTypePeriode()),
                                                    null, idEleve, this);
                                        } else {
                                            answer.set(true);
                                        }
                                    }
                                    lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                                    if (answer.get()) {
                                        getAppreciationsPromise.complete();
                                    }
                                }

                                private void addApEleve(JsonObject element) {
                                    if(donnees.getAccPersosGroupes() != null){
                                        List<AccPersoGroupe> listAccPersoGroupe = donnees.getAccPersosGroupes().getAccPersoGroupe();
                                        if (listAccPersoGroupe != null && listAccPersoGroupe.size() > 0) {
                                            AccPersoGroupe accGroupe = listAccPersoGroupe.stream()
                                                    .filter(accG -> accG.getId().equals(ACC_GROUPE + element.getInteger("id_elt_bilan_periodique").toString()))
                                                    .findFirst().orElse(null);

                                            if (accGroupe != null) {

                                                if (bilanPeriodique.getAccPersosEleve() == null ||
                                                        bilanPeriodique.getAccPersosEleve().getAccPersoEleve() == null) {
                                                    bilanPeriodique.setAccPersosEleve(objectFactory.createBilanPeriodiqueAccPersosEleve());
                                                }
                                                if (!containsAccPersoEleve(bilanPeriodique.getAccPersosEleve().getAccPersoEleve(),
                                                        accGroupe)) {
                                                    AccPersoEleve accEleve = objectFactory.createAccPersoEleve();
                                                    accEleve.setCommentaire(element.getString("commentaire"));
                                                    accEleve.setAccPersoGroupeRef(accGroupe);
                                                    bilanPeriodique.getAccPersosEleve().getAccPersoEleve().add(accEleve);
                                                }
                                            }
                                        }
                                    }

                                }
                                private boolean containsAccPersoEleve (List<AccPersoEleve> listAccPersoEleve, Object accPersoGroupeRef){
                                    if (listAccPersoEleve != null && !listAccPersoEleve.isEmpty()){
                                        for(AccPersoEleve ape : listAccPersoEleve){
                                            if(ape.getAccPersoGroupeRef().equals(accPersoGroupeRef) ) return true;
                                        }
                                    }
                                    return false;
                                }

                                private void addEpiEleve(JsonObject element) {
                                    if (donnees.getEpisGroupes() != null) {
                                        List<EpiGroupe> listEpiGroupe = donnees.getEpisGroupes().getEpiGroupe();
                                        if (listEpiGroupe != null && listEpiGroupe.size() > 0) {
                                            EpiGroupe epiGroupe = listEpiGroupe.stream()
                                                    .filter(epiG -> epiG.getId().equals(EPI_GROUPE +
                                                            element.getInteger("id_elt_bilan_periodique").toString()))
                                                    .findFirst()
                                                    .orElse(null);
                                            if (epiGroupe != null) {//epi

                                                if (bilanPeriodique.getEpisEleve() == null ||
                                                        bilanPeriodique.getEpisEleve().getEpiEleve() == null) {
                                                    bilanPeriodique.setEpisEleve(objectFactory.createBilanPeriodiqueEpisEleve());
                                                }
                                                if (!containsEpiEleve(bilanPeriodique.getEpisEleve().getEpiEleve(),epiGroupe)){
                                                    EpiEleve epiEleve = objectFactory.createEpiEleve();
                                                    epiEleve.setCommentaire(element.getString("commentaire"));
                                                    epiEleve.setEpiGroupeRef(epiGroupe);
                                                    bilanPeriodique.getEpisEleve().getEpiEleve().add(epiEleve);
                                                }
                                            }
                                        }
                                    }
                                }

                                private boolean containsEpiEleve (List<EpiEleve> listEpiElve, Object epiGroupe){
                                    if (listEpiElve != null && !listEpiElve.isEmpty()){
                                        for(EpiEleve ee: listEpiElve){
                                            if( ee.getEpiGroupeRef().equals(epiGroupe)) return true;
                                        }
                                    }
                                    return false;
                                }

                                private void addParcoursEleve(JsonObject app) {
                                    if (bilanPeriodique.getListeParcours() == null || bilanPeriodique.getListeParcours().getParcours() == null) {
                                        bilanPeriodique.setListeParcours(objectFactory.createBilanPeriodiqueListeParcours());
                                    }
                                    if(!containsParcours(bilanPeriodique.getListeParcours().getParcours(),
                                            app.getString("code"))){
                                        Parcours parcoursEleve = objectFactory.createParcours();
                                        parcoursEleve.setValue(app.getString("commentaire"));
                                        parcoursEleve.setCode(CodeParcours.fromValue(app.getString("code")));
                                        bilanPeriodique.getListeParcours().getParcours().add(parcoursEleve);
                                    }
                                }

                                private boolean containsParcours (List<Parcours> listParcours, String codeValue){
                                    if(listParcours != null && !listParcours.isEmpty()){
                                        for(Parcours p  : listParcours){
                                            if (p.getCode().value().equals(codeValue)) {
                                                return true;
                                            }
                                        }
                                    }
                                    return false;
                                }
                            });

                    bilanPeriodiqueService.getSuiviAcquis(idStructure, idPeriode, idEleve, idClasseGroups,
                            servicesClasse, multiTeachersClasse, new Handler<Either<String, JsonArray>>() {
                                AtomicBoolean answer = new AtomicBoolean(false);
                                AtomicInteger count = new AtomicInteger(0);
                                final String thread = "(" + currentEleve.getNom() + " " + currentEleve.getPrenom() + ")";
                                final String method = "getBaliseBilansPeriodiques | getSuiviAcquis";

                                @Override
                                public void handle(Either<String, JsonArray> suiviAcquisResponse) {
                                    if (suiviAcquisResponse.isLeft()) {
                                        String error = suiviAcquisResponse.left().getValue();
                                        if (error != null && error.contains(TIME) && !getSuiviAcquisPromise.future().isComplete()) {
                                            bilanPeriodiqueService.getSuiviAcquis(idStructure, idPeriode, idEleve,
                                                    idClasseGroups, servicesClasse, multiTeachersClasse,this);
                                        } else {
                                            getSuiviAcquisPromise.complete();
                                        }
                                    } else {
                                        if (!suiviAcquisResponse.right().getValue().isEmpty()) {
                                            final JsonArray suiviAcquis = suiviAcquisResponse.right().getValue();

                                            addListeAcquis(suiviAcquis, bilanPeriodique);
                                            // add Only if suiviAcquis is not Empty
                                            bilansPeriodiques.getBilanPeriodique().add(bilanPeriodique);
                                        } else {
                                            if (suiviAcquisResponse.isRight()) {
                                                String messageError = getLibelle("evaluation.lsu.error.no.suivi.acquis") +
                                                        currentPeriode.getLabel();
                                                setError(errorsExport, currentEleve, messageError, null);
                                                log.info(idEleve + " NO ");
                                            }
                                        }
                                        if (!getSuiviAcquisPromise.future().isComplete()) {
                                            getSuiviAcquisPromise.complete();
                                        }
                                    }
                                    lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                                }

                                private void addListeAcquis(JsonArray suiviAcquis, BilanPeriodique bilanPeriodique) {
                                    BilanPeriodique.ListeAcquis acquisEleveList = objectFactory.createBilanPeriodiqueListeAcquis();
                                    for (int i = 0; i < suiviAcquis.size(); i++) {
                                        final JsonObject currentAcquis = suiviAcquis.getJsonObject(i);
                                        Acquis acquisEleve = addListeAcquis_addAcquis(currentAcquis);
                                        if(currentAcquis.getBoolean("toAdd")) {
                                            addAcquis_addDiscipline(currentAcquis, acquisEleve);
                                            addAcquis_addElementProgramme(currentAcquis, acquisEleve);
                                            addAcquis_addMissingTeacher(acquisEleveList, currentAcquis, acquisEleve);
                                        }
                                    }
                                    if(!acquisEleveList.getAcquis().isEmpty()) {
                                        bilanPeriodique.setListeAcquis(acquisEleveList);
                                    } else {
                                        String messageError = getLibelle("evaluation.lsu.error.no.suivi.acquis") +
                                                currentPeriode.getLabel();
                                        setError(errorsExport, currentEleve, messageError, null);
                                    }
                                }

                                private Acquis addListeAcquis_addAcquis(JsonObject currentAcquis) {
                                    Acquis acquisEleve = objectFactory.createAcquis();
                                    JsonArray tableConversion = tableConversionByClasse.get(idClasse);
                                    addAcquis_addMoyennes(currentAcquis, acquisEleve);
                                    addAcquis_addPositionnement(currentAcquis, tableConversion, acquisEleve);
                                    addAcquis_addAppreciation(currentAcquis, acquisEleve, currentPeriode);
                                    return acquisEleve;
                                }

                                private void addAcquis_addMoyennes(JsonObject currentAcquis, Acquis acquisEleve) {
                                    JsonArray moyennesEleves = currentAcquis.getJsonArray("moyennes");
                                    JsonArray moyennesFinales = currentAcquis.getJsonArray("moyennesFinales");
                                    JsonArray moyennesClasse = currentAcquis.getJsonArray("moyennesClasse");

                                    JsonObject moyEleve = utilsService.getObjectForPeriode(moyennesEleves, idPeriode, "id");
                                    JsonObject moyFinale = utilsService.getObjectForPeriode(moyennesFinales, idPeriode, "id_periode");
                                    JsonObject moyClasse = utilsService.getObjectForPeriode(moyennesClasse, idPeriode, "id");

                                    String idMatiere = currentAcquis.getString(ID_MATIERE);

                                    DefaultNoteService.getMoyenneFinaleByIdEleveAndIdMatiereAndIdPeriod(idEleve, idMatiere, idPeriode)
                                            .compose(optMoyenneFinale -> {
                                                if (optMoyenneFinale.isPresent()) {
                                                    MoyenneFinale moyenneFinale = optMoyenneFinale.get();

                                                    if (moyenneFinale.getMoyenne() != null) {
                                                        acquisEleve.setMoyenneEleve(new BigDecimal(moyenneFinale.getMoyenne().toString()));
                                                    }

                                                    if (moyenneFinale.getStatut() != null) {
                                                        String statut = moyenneFinale.getStatut();

                                                        if (Objects.equals(statut, NN)) {
                                                            acquisEleve.setStatutEvaluationEleve(BigInteger.valueOf(1L));
                                                        } else if (Objects.equals(statut, EA)) {
                                                            acquisEleve.setStatutEvaluationEleve(BigInteger.valueOf(2L));
                                                        } else if (Objects.equals(statut, DI)) {
                                                            acquisEleve.setStatutEvaluationEleve(BigInteger.valueOf(3L));
                                                        }
                                                    }

                                                } else {
                                                    // Si pas de moyenneFinale en base, on tente avec les moyennes JSON
                                                    log.info("niko1: " + moyEleve);
                                                    if (moyEleve != null && moyEleve.containsKey(MOYENNE)) {
                                                        log.info("niko3: " + new BigDecimal(moyEleve.getValue(MOYENNE).toString()));
                                                        acquisEleve.setMoyenneEleve(new BigDecimal(moyEleve.getValue(MOYENNE).toString()));
                                                    }
                                                }

                                                // Traitement moyenne de classe
                                                if (moyClasse != null && moyClasse.containsKey(MOYENNE)) {
                                                    Object valClasse = moyClasse.getValue(MOYENNE);
                                                    if (valClasse instanceof Number) {
                                                        acquisEleve.setMoyenneStructure(new BigDecimal(valClasse.toString()));
                                                    }
                                                }

                                                // Si la moyenneStructure est toujours null, on vérifie si l’élève est en 3e
                                                if (acquisEleve.getMoyenneStructure() == null) {
                                                    return userService.isUserInThirdClassLevel(idEleve)
                                                            .map(isInThirdClassLevel -> {
                                                                if (isInThirdClassLevel) {
                                                                    acquisEleve.setStatutEvaluationStructure(BigInteger.valueOf(2L));
                                                                } else {
                                                                    acquisEleve.setStatutEvaluationStructure(BigInteger.valueOf(1L));
                                                                }
                                                                return null;
                                                            });
                                                } else {
                                                    // Rien d'autre à faire, on retourne une Future vide
                                                    return Future.succeededFuture();
                                                }

                                            }).onFailure(err -> {
                                                log.error("Erreur lors de l'ajout des moyennes pour l'acquis", err);
                                            });
                                }


                                private void addAcquis_addPositionnement(JsonObject currentAcquis,
                                                                         JsonArray tableConversion, Acquis acquisEleve){
                                    JsonArray positionnements_auto = currentAcquis.getJsonArray("positionnements_auto");
                                    JsonArray positionnementsFinaux = currentAcquis.getJsonArray("positionnementsFinaux");

                                    JsonObject positionnementAuto = utilsService.getObjectForPeriode(positionnements_auto,
                                            idPeriode, "id_periode");
                                    JsonObject positionnementFinal = utilsService.getObjectForPeriode(positionnementsFinaux,
                                            idPeriode, "id_periode");

                                    Integer valuePositionnementFinal = null;
                                    if (positionnementFinal != null)
                                        valuePositionnementFinal = positionnementFinal.getInteger("positionnementFinal");

                                    BigInteger positionnementToSet = BigInteger.valueOf(0);
                                    // "hasNote" exit if  moyenne != -1
                                    if (positionnementAuto != null && positionnementAuto.containsKey("hasNote") && positionnementAuto.getBoolean("hasNote") ) {
                                        // BigInteger positionnementToSet;
                                        String valuePositionnementAuto = utilsService.convertPositionnement(
                                                positionnementAuto.getFloat("moyenne"), tableConversion,false);

                                        positionnementToSet = isNotNull(valuePositionnementFinal) ? BigInteger.valueOf(valuePositionnementFinal) :
                                                new BigInteger(valuePositionnementAuto);
                                    } else if (isNotNull(valuePositionnementFinal)) {
                                        positionnementToSet = BigInteger.valueOf(valuePositionnementFinal);
                                    }
                                    if (positionnementToSet.intValue() != 0) {
                                        acquisEleve.setPositionnement(positionnementToSet);
                                    }
                                }

                                private void addAcquis_addAppreciation(JsonObject currentAcquis, Acquis acquisEleve,
                                                                       Periode currentPeriode) {
                                    JsonArray appreciations = currentAcquis.getJsonArray("appreciations");
                                    boolean hasAppreciation = false;
                                    JsonObject app = addAppreciation_getObjectForPeriode(appreciations, idPeriode);
                                    if (app != null) {
                                        JsonArray appreciationByClasse = app.getJsonArray("appreciationByClasse");
                                        if (appreciationByClasse != null) {
                                            for (int i = 0; i < appreciationByClasse.size(); i++) {
                                                app = appreciationByClasse.getJsonObject(i);
                                                if (app.containsKey("appreciation")) {
                                                    String appTmp = app.getString("appreciation");
                                                    if (appTmp != null && !appTmp.isEmpty()) {
                                                        acquisEleve.setAppreciation(appTmp);
                                                        hasAppreciation = true;
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (!hasAppreciation) {
                                        String messageError = getLibelle("evaluation.lsu.error.no.appreciation") +
                                                currentPeriode.getLabel() +
                                                getLibelle("evaluation.lsu.error.on.subject");
                                        String libelleMatiere = currentAcquis.getString("libelleMatiere");
                                        setError(errorsExport, currentEleve, messageError, libelleMatiere);
                                    } else if(!hasAppreciation){
                                        acquisEleve.setAppreciation(getLibelle("evaluation.lsu.no.appreciation.message"));
                                    }

                                    boolean toAdd = false;
                                    if (hasAppreciation) {
                                        bilanPeriodique.setEleveRef(currentEleve);
                                        bilanPeriodique.setPeriodeRef(currentPeriode);
                                        addResponsable(bilanPeriodique);
                                        toAdd = true;
                                    }

                                    currentAcquis.put("toAdd", toAdd);
                                }

                                private JsonObject addAppreciation_getObjectForPeriode(JsonArray array, Long idPeriode) {
                                    JsonObject res = null;
                                    if (array != null) {
                                        for (int i = 0; i < array.size(); i++) {
                                            JsonObject o = array.getJsonObject(i);
                                            if (o.getLong("id_periode") == idPeriode) {
                                                res = o;
                                            }
                                        }
                                    }
                                    return res;
                                }

                                private void addResponsable(BilanPeriodique bilanPeriodique) {
                                    if (currentEleve.getResponsableList() != null && currentEleve.getResponsableList().size() > 0) {
                                        BilanPeriodique.Responsables responsablesEleve = objectFactory.createBilanPeriodiqueResponsables();
                                        responsablesEleve.getResponsable().addAll(currentEleve.getResponsableList());
                                        bilanPeriodique.setResponsables(responsablesEleve);
                                    }
                                }

                                private void addAcquis_addDiscipline(JsonObject currentAcquis, Acquis acquisEleve) {
                                    String idMatiere = currentAcquis.getString("id_matiere");
                                    lsuService.addIdsEvaluatedDiscipline(idMatiere);
                                    Discipline currentSubj = getDisciplineInXML(idMatiere, donnees);
                                    if (currentSubj != null) {
                                        acquisEleve.setDisciplineRef(currentSubj);
                                    }
                                }

                                private void addAcquis_addElementProgramme(JsonObject currentAcquis, Acquis acquisEleve) {
                                    String epLabel = currentAcquis.getString("elementsProgramme");
                                    final ElementProgramme newEP = objectFactory.createElementProgramme();
                                    newEP.setLibelle(epLabel);

                                    ElementProgramme ep = donnees.getElementsProgramme().getElementProgramme().stream()
                                            .filter(cep -> cep.getLibelle().replaceAll("[\\s]", "")
                                                    .equals(newEP.getLibelle().replaceAll("[\\s]", "")))
                                            .findFirst().orElse(null);
                                    if (ep == null) {
                                        String epId = generateElementProgrammeId(idElementProgramme);
                                        newEP.setId(epId);
                                        newEP.setLibelle(epLabel);
                                        donnees.getElementsProgramme().getElementProgramme().add(newEP);
                                        acquisEleve.getElementProgrammeRefs().add(newEP);
                                    } else {
                                        acquisEleve.getElementProgrammeRefs().add(ep);
                                    }
                                }

                                private void addAcquis_addMissingTeacher(BilanPeriodique.ListeAcquis acquisEleveList,
                                                                         JsonObject currentAcquis, Acquis acquisEleve) {
                                    if (currentAcquis.containsKey("teachers")) {
                                        JsonArray teachersList = currentAcquis.getJsonArray("teachers");
                                        for (int k = 0; k < teachersList.size(); k++) {
                                            Enseignant enseignant = addorFindTeacherBalise(donnees, enseignantFromSts,
                                                    teachersList.getJsonObject(k));
                                            acquisEleve.getEnseignantRefs().add(enseignant);
                                        }
                                    }
                                    if (acquisEleve.getElementProgrammeRefs().size() > 0
                                            && acquisEleve.getEnseignantRefs().size() > 0
                                            && acquisEleve.getDisciplineRef() != null) {
                                        acquisEleveList.getAcquis().add(acquisEleve);
                                    }
                                }
                            }
                    );

                    if(currentPeriode.getIndice() == currentPeriode.getNbPeriodes() && currentCycle.equals(LevelCycle.CYCLE3.getValue())) {
                        digitalSkillsService.getDigitalSkillsByStudent(idEleve, idStructure, new Handler<Either<String, JsonObject>>() {
                            AtomicBoolean answer = new AtomicBoolean(false);
                            AtomicInteger count = new AtomicInteger(0);
                            final String thread = "(" + currentEleve.getNom() + " " + currentEleve.getPrenom() + ")";
                            final String method = "getBaliseBilansPeriodiques | getAllDigitalSkills ";

                            @Override
                            public void handle(Either<String, JsonObject> digitalSkillsEvent) {
                                if (digitalSkillsEvent.isRight()) {
                                    final JsonObject digitalSkills = digitalSkillsEvent.right().getValue();
                                    JsonObject studentAppreciation = digitalSkills.getJsonObject("studentAppreciation");
                                    JsonArray evaluatedDigitalSkills = digitalSkills.getJsonArray("evaluatedDigitalSkills");
                                    CompetencesNumeriquesCommun appClassCompNum = getAppClassCompNum(donnees, currentEleve.getCodeDivision());
                                    if( appClassCompNum == null && studentAppreciation.isEmpty() &&
                                            evaluatedDigitalSkills.isEmpty() && withDigitalSkillsError) {
                                        String messageError = getLibelle("evaluation.lsu.error.no.digital.skills") + currentPeriode.getLabel();
                                        setError(errorsExport, currentEleve, messageError, null);
                                    }
                                    if(!studentAppreciation.isEmpty() || !evaluatedDigitalSkills.isEmpty() || appClassCompNum != null )
                                    bilanPeriodique.setCompetencesNumeriques(objectFactory.createBilanPeriodiqueCompetencesNumeriques());

                                    if(appClassCompNum != null) bilanPeriodique.getCompetencesNumeriques()
                                            .getCompNumCommunRefs().add(appClassCompNum);
                                    if(!studentAppreciation.isEmpty()) {
                                        bilanPeriodique.getCompetencesNumeriques().setAppreciationNum(studentAppreciation.getString("appreciation"));
                                    }
                                    if(!evaluatedDigitalSkills.isEmpty()) {
                                        evaluatedDigitalSkills.forEach(evaluatedDigitalSkill -> {
                                            JsonObject evaluatedDigitalSkillJson = (JsonObject) evaluatedDigitalSkill;
                                            CompetenceNumeriqueNiveau competenceNumeriqueNiveau = objectFactory.createCompetenceNumeriqueNiveau();
                                            competenceNumeriqueNiveau.setNiveau(BigInteger.valueOf(evaluatedDigitalSkillJson.getLong("level")));
                                            competenceNumeriqueNiveau.setCode(CodeCompetenceNumerique.fromValue(evaluatedDigitalSkillJson.getString("code")));
                                            bilanPeriodique.getCompetencesNumeriques().getCompetenceNum().add(competenceNumeriqueNiveau);
                                        });
                                    }
                                    answer.set(true);
                                } else {
                                    String error = digitalSkillsEvent.left().getValue();
                                    if (error != null && error.contains(TIME)) {
                                        digitalSkillsService.getDigitalSkillsByStudent(idEleve, idStructure, this);
                                    } else {
                                        answer.set(true);
                                    }
                                }
                                lsuService.serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                                if (answer.get()) {
                                    getDigitalSkillsPromise.complete();
                                }

                            }
                            private CompetencesNumeriquesCommun getAppClassCompNum(Donnees donnees, String codeDivision){
                                if(donnees.getCompetencesNumeriquesCommuns() != null &&
                                        !donnees.getCompetencesNumeriquesCommuns().getCompetencesNumeriquesCommun().isEmpty()){
                                    CompetencesNumeriquesCommun compNumClass = donnees.getCompetencesNumeriquesCommuns().getCompetencesNumeriquesCommun().stream()
                                            .filter( compNumCommon -> compNumCommon.getCodeStructure().equals(codeDivision))
                                            .findFirst().orElse(null);
                                    if(compNumClass != null) {
                                        return compNumClass;
                                    }
                                    return null;
                                }
                                return null;
                            }
                        });
                    } else {
                        getDigitalSkillsPromise.complete();
                    }

                }
            }
        }
    }

    private String getSimilarError(JsonArray errorsMessage, String message) {
        String similarError = null;

        for(Object errorObject : errorsMessage) {
            String error = errorObject.toString();
            if(error.contains(message))
                similarError = error;
        }

        return similarError;
    }

    private void setError(JsonObject errorsExport, Object currentEleve, String message, String matiere){
        String finalMessage = matiere != null ? message + matiere : message;
        if(currentEleve instanceof Eleve){
            Eleve eleve = (Eleve) currentEleve;
            if(errorsExport.containsKey(eleve.getIdNeo4j())){
                JsonObject errorEleve = errorsExport.getJsonObject(eleve.getIdNeo4j());
                if(errorEleve.containsKey("errorsMessages")){
                    JsonArray errorsMessages = errorEleve.getJsonArray("errorsMessages");
                    String similarError = getSimilarError(errorsMessages, message);
                    if(!errorsMessages.contains(message) && isNull(similarError)){
                        errorsMessages.add(finalMessage);
                    } else if(similarError != null) {
                        errorsMessages.remove(similarError);
                        similarError += ", " + matiere;
                        errorsMessages.add(similarError);
                    }
                } else {
                    errorEleve.put("error", new JsonArray().add(finalMessage));
                }
            } else {
                errorsExport.put(eleve.getIdNeo4j(), new JsonObject()
                        .put("idEleve", eleve.getIdNeo4j())
                        .put("lastName", eleve.getNom())
                        .put("firstName", eleve.getPrenom())
                        .put("idClass", eleve.getId_Class())
                        .put("nameClass", eleve.getCodeDivision())
                        .put("id_classe", eleve.getId_Class())
                        .put("errorsMessages", new JsonArray().add(finalMessage)));
            }
        } else {
            ((JsonObject) currentEleve).put("errorsMessages", new JsonArray().add(finalMessage));
            errorsExport.put(((JsonObject) currentEleve).getString("idEleve"), currentEleve);
        }
    }

    private Boolean addDatesBilanPeriodique( BilanPeriodique bilanPeriodique,
                                             Eleve currentEleve, Periode currentPeriode, JsonObject periode ){

        if(periode != null ){

            if(currentPeriode.getLabel() == null) {
                String labelPeriode = getLibelle("viescolaire.periode." + periode.getValue("type"));
                labelPeriode += (" " + periode.getValue("ordre"));
                currentPeriode.setLabel(labelPeriode);
            }
            XMLGregorianCalendar dateScolarite = null;

            Date dateFnPeriode = UtilsConvert.convertStringToDate(periode.getString("timestamp_fn"), "yyyy-MM-dd");
            if(currentEleve.getDeleteDate() != null && currentEleve.getDeleteDate().before(dateFnPeriode)){
                log.info("addDatesBilanPeriodique : deleteDate != null "+ currentEleve.getNom() +" "+currentEleve.getPrenom());
                dateScolarite = getDateFormatGregorian(currentEleve.getDeleteDate());
            }else{
                dateScolarite = getDateFormatGregorian(periode.getString("timestamp_fn"));
            }

            XMLGregorianCalendar dateConseil = getDateFormatGregorian(periode.getString("date_conseil_classe"));
            bilanPeriodique.setDateScolarite(dateScolarite);
            bilanPeriodique.setDateConseilClasse(dateConseil);
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
        if (donnees.getDisciplines() == null || donnees.getDisciplines().getDiscipline() == null ||
                donnees.getDisciplines().getDiscipline().isEmpty()) {
            return null;
        }
        return donnees.getDisciplines().getDiscipline().stream()
                .filter(dis -> dis.getId().equals(DISCIPLINE_KEY + id))
                .findFirst()
                .orElse(null);
    }

    private Enseignant getEnseignantInXML(String id, Donnees donnees) {
        if (donnees.getEnseignants() == null || donnees.getEnseignants().getEnseignant() == null ||
                donnees.getEnseignants().getEnseignant().isEmpty()) {
            return null;
        }
        return donnees.getEnseignants().getEnseignant().stream()
                .filter(ens -> ens.getId().equals("ENS_" +id))
                .findFirst()
                .orElse(null);
    }

    private String generateElementProgrammeId(AtomicInteger idElementProgramme){
        return "EP_" + idElementProgramme.incrementAndGet();
    }

    private String getMillesimeBFC(){
        Integer millesime;
        Calendar today = Calendar.getInstance();
        Calendar janvier = Calendar.getInstance();
        janvier.set(Calendar.DAY_OF_MONTH,1);
        janvier.set(Calendar.MONTH,Calendar.JANUARY);

        Calendar juillet = Calendar.getInstance();
        juillet.set(Calendar.DAY_OF_MONTH, 31);
        juillet.set(Calendar.MONTH, Calendar.JULY);

        millesime = today.get(Calendar.YEAR);
        // Si on est entre le 01 janvier et le 31 juillet on enleve une année au millésime
        // ex: si anne scolaire 2018/2019 le millesime est 2018
        if(today.after(janvier) && today.before(juillet)){
            millesime--;}
        return millesime.toString();
    }

    /**
     * génère le fichier xml et le valide
     *
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
                    request.response().putHeader("Content-Disposition", "attachment; filename=import_lsun_"
                            + new Date().getTime() + ".xml");
                    request.response().end(Buffer.buffer(response.toString()));
                    log.info("FIN method returnResponse");
                }
            });
        } catch (IOException | JAXBException e) {
            log.error("xml non valide : "+ e.toString());
            badRequest(request);
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


