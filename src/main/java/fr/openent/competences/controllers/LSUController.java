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

import fr.openent.competences.service.ElementBilanPeriodiqueService;
import fr.openent.competences.service.BilanPeriodiqueService;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.lsun.*;
import fr.openent.competences.bean.lsun.ElementProgramme;
import fr.openent.competences.service.*;
import fr.openent.competences.service.impl.*;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
import java.io.*;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.openent.competences.bean.lsun.TypeEnseignant.fromValue;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;


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
    private JsonArray listErreursEleves;
    private EventBus ebController;
    private DispenseDomaineEleveService dispenseDomaineEleveService;
    private final BilanPeriodiqueService bilanPeriodiqueService;
    private final ElementBilanPeriodiqueService elementBilanPeriodiqueService;
    private final DefaultSyntheseBilanPeriodiqueService syntheseBilanPeriodiqueService;

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
                                        .put("error", body.getString("message"));
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
                || !entries.containsKey("responsables")) {
            badRequest(request, "bilanFinCycleExport - No valid params");
            return;
        }
        //instancier le lsunBilans qui sera composé de entete,donnees et version
        final LsunBilans lsunBilans = objectFactory.createLsunBilans();
        //donnees composée de responsables-etab, eleves et bilans-cycle
        final Donnees donnees = objectFactory.createDonnees();
        final String idStructure = entries.getString("idStructure");
        log.info("idStructure = " + idStructure);
        final List<String> idsClasse = getIdsList(entries.getJsonArray("classes"));
        final List<String> idsResponsable = getIdsList(entries.getJsonArray("responsables"));

        lsunBilans.setSchemaVersion("3.0");
        log.info("DEBUT  get exportLSU : export Classe : " + idsClasse);
        if (!idsClasse.isEmpty() && !idsResponsable.isEmpty()) {
            getBaliseEntete(lsunBilans, idStructure, new Handler<String>() {
                @Override
                public void handle(String event) {
                    if (event.equals("success")) {

                        getBaliseResponsables(donnees, idsResponsable, new Handler<String>() {
                            @Override
                            public void handle(String event) {
                                if (event.equals("success")) {

                                    getBaliseEleves(donnees, idsClasse, new Handler<String>() {
                                        @Override
                                        public void handle(final String event) {
                                            if (event.equals("success")) {
                                                Utils.getDatesCreationVerrouByClasses(eb, idStructure, idsClasse, new Handler<Either<String, Map<String, JsonObject>>>() {
                                                    @Override
                                                    public void handle(Either<String, Map<String, JsonObject>> resultsQuery) {
                                                        if (resultsQuery.isRight()) {
                                                            Map<String, JsonObject> dateCreationVerrouByClasse = resultsQuery.right().getValue();

                                                            getBaliseBilansCycle(donnees, idsClasse, idStructure, dateCreationVerrouByClasse, new Handler<Either<String, JsonArray>>() {
                                                                @Override
                                                                public void handle(Either<String, JsonArray> reponseErreursJsonArray) {
                                                                    if (reponseErreursJsonArray.isRight()) {

                                                                        if (donnees.getBilansCycle() != null && donnees.getBilansCycle().getBilanCycle().size() != 0) {
                                                                            lsunBilans.setDonnees(donnees);
                                                                            returnResponse(request, lsunBilans);
                                                                        } else {
                                                                            JsonArray listErreurs = reponseErreursJsonArray.right().getValue();
                                                                            if (listErreurs.size() > 0) {
                                                                                JsonArray affichageDesELeves = new fr.wseduc.webutils.collections.JsonArray();
                                                                                for (int i = 0; i < listErreurs.size(); i++) {
                                                                                    JsonObject affichageEleve = new JsonObject();
                                                                                    JsonObject erreurOneEleve = listErreurs.getJsonObject(i);
                                                                                    affichageEleve.put("nom", erreurOneEleve.getString("nom"))
                                                                                            .put("prenom", erreurOneEleve.getString("prenom"))
                                                                                            .put("classe", erreurOneEleve.getString("classe"));
                                                                                    affichageDesELeves.add(affichageEleve);
                                                                                }
                                                                                Renders.renderJson(request, affichageDesELeves);
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            });
                                                        } else {
                                                            leftToResponse(request, new Either.Left<String, JsonArray>("getXML : getDatesCreationVerrouByClasses " + resultsQuery.left().getValue()));
                                                            log.error("getXML : getDatesCreationVerrouByClasses " + resultsQuery);
                                                        }
                                                    }
                                                });

                                            } else {
                                                leftToResponse(request, new Either.Left<>(event));
                                                log.error("getXML : getBaliseEleves " + event);
                                            }
                                        }
                                    });
                                } else {
                                    leftToResponse(request, new Either.Left<>(event));
                                    log.error("getXML : getBaliseResponsable " + event);
                                }
                            }
                        });
                    } else {
                        leftToResponse(request, new Either.Left<>(event));
                        log.error("getXML : getBaliseEntete " + event);

                    }
                }
            });
        } else {
            badRequest(request);
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
                || !entries.containsKey("periodes_type")) {
            badRequest(request, "bilanPeriodiqueExport - No valid params");
            return;
        }

        final String idStructure = entries.getString("idStructure");//"a1b2a3fb-35b3-4e3e-8c2e-6991b5ec2887";
        final List<String> idsClasse = getIdsList(entries.getJsonArray("classes"));//Arrays.asList("63af9677-280b-4853-b7cd-c61440fa61cc", "12dd23cb-b32f-4b28-842f-04d08e372ccf");//3Aeme 5Ceme
        final List<String> idsResponsable = getIdsList(entries.getJsonArray("responsables"));//Arrays.asList("92ecac5a-c563-4c28-8a9f-1c5d8ad89bd2");//ce.fouquet
        final List<Integer> idsTypePeriodes = new ArrayList<Integer>();
        JsonArray dataPeriodesType = entries.getJsonArray("periodes_type");
        for (int i = 0; i < dataPeriodesType.size(); i++) {
            idsTypePeriodes.add(dataPeriodesType.getJsonObject(i).getInteger("id_type"));
        }

        final LsunBilans lsunBilans = objectFactory.createLsunBilans();//instancier le lsunBilans qui sera composé de entete,donnees et version
        //donnees composée de responsables-etab, eleves et bilans-cycle
        final Donnees donnees = objectFactory.createDonnees();
        final JsonObject periodesAdded = new JsonObject();
        final JsonObject epiGroupAdded = new JsonObject();
        final JsonObject accGroupAdded = new JsonObject();
        donnees.setEnseignants(objectFactory.createDonneesEnseignants());


        Handler<Either.Right<String, JsonObject>> getBilansPeriodiquesHandler = backresponse -> {

            JsonObject data = backresponse.right().getValue();
            if (data.getInteger("status") == 200) {
                log.info("FIN exportLSU : export ");
                lsunBilans.setDonnees(donnees);
                returnResponse(request, lsunBilans);
            } else {
                log.error("getXML : getBaliseBilansPeriodiques " + data.getString("error"));
                badRequest(request, "getXML : getBaliseBilansPeriodiques " + backresponse);
            }
        };

        Handler<Either<String, Map<String, JsonObject>>> getDatesCreationVerrouByClassesHandler = resultsQuery -> {
            if (resultsQuery.isRight()) {
                Map<String, JsonObject> dateCreationVerrouByClasse = resultsQuery.right().getValue();
                this.getBaliseBilansPeriodiques(donnees, idStructure, periodesAdded, dateCreationVerrouByClasse, epiGroupAdded, accGroupAdded, getBilansPeriodiquesHandler);


            } else {
                log.error("getXML : getDatesCreationVerrouByClasses " + resultsQuery);
                badRequest(request, "getXML : getDatesCreationVerrouByClasses " + resultsQuery);
            }
        };

        Handler<String> getApEpiParcoursHandler = event -> {
            if (event.equals("success")) {
                Utils.getDatesCreationVerrouByClasses(eb, idStructure, idsClasse, getDatesCreationVerrouByClassesHandler);

            } else {
                log.error("getXML : getApEpiParcoursBalises " + event);
                badRequest(request, "getXML : getApEpiParcoursBalises "+ event);
            }
        };

        Future<JsonObject> getEnseignantsFuture = Future.future();
        Handler<String> getEnseignantsHandler = event -> {
            if (event.equals("success")) {
                getEnseignantsFuture.complete();
            } else {
                getEnseignantsFuture.complete();
                leftToResponse(request, new Either.Left<>(event));
                log.error("getXML : getBaliseEnseignants " + event);
            }
        };
        getBaliseEnseignants(donnees, idStructure, idsClasse, getEnseignantsHandler);


        Future<JsonObject> getPeriodesFuture = Future.future();
        Handler<String> getPeriodesHandler = event -> {
            if (event.equals("success")) {
                getPeriodesFuture.complete();
            } else {
                getPeriodesFuture.fail("can't generate Balises Periodes Future");
                log.error("getXML : getBalisePeriode " + event);
            }
        };
        getBalisePeriodes(donnees, idsTypePeriodes, periodesAdded, idStructure, idsClasse, getPeriodesHandler);

        Future<JsonObject> getElevesFuture = Future.future();
        Handler<String> getElevesHandler = event -> {
            if (event.equals("success")) {
                getElevesFuture.complete();
            } else {
                getElevesFuture.fail("can't generate Balises Eleves Future");
                log.error("getXML : getBaliseEleves " + event);
            }
        };
        getBaliseEleves(donnees, idsClasse, getElevesHandler);


        Future<JsonObject> getDisciplineFuture = Future.future();
        Handler<String> getDisciplineHandler = event -> {
            if (event.equals("success")) {
                getDisciplineFuture.complete();
            } else {
                getDisciplineFuture.fail("can't generate Balises Disciplines Future");
                log.error("getXML : getBaliseDiscipline " + event);
            }
        };
        getBaliseDisciplines(donnees, idStructure, getDisciplineHandler);


        Future<JsonObject> getResponsableFuture = Future.future();
        Handler<String> getResponsableHandler = event -> {
            if (event.equals("success")) {
                getResponsableFuture.complete();
            } else {
                getResponsableFuture.fail("can't generate Balises Responsables Future");
                log.error("getXML : getBaliseResponsable " + event);
            }
        };
        getBaliseResponsables(donnees, idsResponsable, getResponsableHandler);

        Future<JsonObject> getEnteteFuture = Future.future();
        Handler<String> getEnteteHandler = event -> {
            if (event.equals("success")) {
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
            CompositeFuture.all(getEnteteFuture, getResponsableFuture, getDisciplineFuture, getElevesFuture, getPeriodesFuture, getEnseignantsFuture).setHandler(
                    event -> {
                        if (event.succeeded()) {
                            getApEpiParcoursBalises(donnees, idsClasse, idStructure, epiGroupAdded, accGroupAdded, getApEpiParcoursHandler);
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

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status")) && !body.getJsonObject("result").isEmpty()) {
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
                    handler.handle("method getBaliseEntete : error when collecting UAI  " + body.getString("message"));
                    log.error("An error occured when collecting UAI for " + idStructure + " structure");
                }
            }
        }));
    }

    //récupère chaque responsable d'établissement et les ajouter à la balise responsables-etab puis à la balise donnees
    private void getBaliseResponsables(final Donnees donnees, final List<String> idsResponsable, final Handler<String> handler) {
        JsonObject action = new JsonObject()
                .put("action", "user.getUsers")
                .put("idUsers", new fr.wseduc.webutils.collections.JsonArray(idsResponsable));
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
                        handler.handle("success");
                    }catch (Throwable e){
                        handler.handle("method getBaliseResponsable : " +e.getMessage());
                        log.error("method getBaliseResponsable : " +e.getMessage());
                    }
                } else {
                    handler.handle("getBaliseResponsable : error when collecting Responsable " + body.getString("message"));
                    log.error("method getBaliseResponsable an error occured when collecting Responsable " + idsResponsable);
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
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
                        if(o.getString("address")!= null
                                && o.getString("zipCode")!=null && o.getString("city")!= null ){
                            String adress = o.getString("address");
                            String codePostal =  o.getString("zipCode");
                            String commune = o.getString("city");
                            if(codePostal.length() > 10){
                                codePostal = o.getString("zipCode").substring(0,10);
                            }
                            if(commune.length() > 100){
                                commune = o.getString("city").substring(0,100);
                            }
                            adresse = objectFactory.createAdresse(adress, codePostal, commune);
                        }
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
                    handler.handle("success");
                    log.info("FIN method getBaliseEleves : nombre d'eleve ajoutes :"+eleves.getEleve().size());

                }else{
                    handler.handle("getBaliseEleves : error when collecting Eleves " + body.getString("message"));
                    log.error("method getBaliseEleves an error occured when collecting Eleves " + body.getString("message"));
                }
            }
        }));
    }

    /**
     *  M
     * @param classIds liste des idsClass dont on recherche le cycle auquel elles appartiennent
     * @param handler retourne une liste de 2 map : map<idClass,idCycle> et map<idCycle,value_cycle>
     */

    private void getIdClassIdCycleValue(List<String> classIds, final Handler<Either<String, List<Map>>> handler) {
        utilsService.getCycle(classIds, new Handler<Either<String, JsonArray>>() {
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
                    handler.handle(new Either.Right<String, List<Map>>(mapArrayList));
                } else {
                    handler.handle(new Either.Left<String, List<Map>>(" getValueCycle : error when collecting Cycles " + response.left().getValue()));
                    log.error("method getIdClassIdCycleValue an error occured when collecting Cycles " + response.left().getValue());
                }
            }
        });
    }

    /**
     * méthode qui permet de construire une Map avec id_domaine et son code_domaine (domaine de hérarchie la plus haute)
     * @param IdClass liste des idsClass
     * @param handler contient la map<IdDomaine,Code_domaine> les codes domaines : codes des socles communs au cycle
     */
    private void getMapCodeDomaineById(String IdClass, final Handler<Either<String, Map<Long, String>>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "user.getCodeDomaine")
                .put("idClass", IdClass);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
                    }
                } else {
                    handler.handle(new Either.Left<String, Map<Long, String>>("getMapCodeDomaineById : error when collecting codeDomaineById : " + body.getString("message")));
                    log.error("method getMapCodeDomaineById an error occured when collecting CodeDomaineById " + body.getString("message"));
                }
            }
        }));
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


    private void setSocleSyntheseEnsCpl(Map<Long, String> mapIdDomaineCodeDomaine, String[] idsEleve, AtomicInteger nbEleveCompteur, Donnees.Eleves eleves, JsonArray ensCplsEleves, JsonArray synthesesEleves,
                                        Map<String, Map<Long, Integer>> mapIdEleveIdDomainePosition, List<ResponsableEtab> responsablesEtab, Long valueCycle,
                                        String millesime, Donnees.BilansCycle bilansCycle, JsonObject datesCreationVerrou) {

        //on récupère id du codeDomaine CPD_ETR
        Long idDomaineCPD_ETR = giveIdDomaine(mapIdDomaineCodeDomaine, "CPD_ETR");

        for (String idEleve : idsEleve) {
            nbEleveCompteur.incrementAndGet();//compteur
            Eleve eleve = eleves.getEleveById(idEleve);
            if (eleve != null) {
                //on récupère le JsonObject de l'enseignement de complément et de la synthèse
                JsonObject ensCplEleve = getJsonObject(ensCplsEleves,idEleve);
                JsonObject syntheseEleve = getJsonObject(synthesesEleves, idEleve);
                JsonObject erreursEleve = new JsonObject();
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
                                // ajouter l'élève dans liste des erreurs
                                // erreursEleve.put("idEleve", idEleve).put("prenom", eleve.getPrenom()).put("nom", eleve.getNom()).put("classe", eleve.getCodeDivision());
                                // JsonArray erreurs = new fr.wseduc.webutils.collections.JsonArray();
                                // erreurs.add(new JsonObject().put("ensCpl", "L'enseignement de complement n'est pas renseigne"));
                                // erreursEleve.put("typeErreur", erreurs);
                                // listErreursEleves.add(erreursEleve);

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

                        XMLGregorianCalendar dateCreation = getDateFormatGregorian(datesCreationVerrou.getString("date_creation"));
                        bilanCycle.setDateCreation(dateCreation);
                        bilanCycle.setDateVerrou(datesCreationVerrou.getString("date_verrou").substring(0,19));

                        //on ajoute les différents attributs de la balise BilanCycle de l'élève
                        ResponsableEtab responsableEtabRef = responsablesEtab.get(0);
                        //on ajoute les responsables de l'élève (attribut de clui-ci) aux responsables et au bilanCycle
                        if(eleve.getResponsableList() != null && eleve.getResponsableList().size()> 0) {
                            BilanCycle.Responsables responsablesEleve = objectFactory.createBilanCycleResponsables();
                            responsablesEleve.getResponsable().addAll(eleve.getResponsableList());
                            bilanCycle.setResponsables(responsablesEleve);
                        }

                        bilanCycle.setResponsableEtabRef(responsableEtabRef);
                        bilanCycle.setEleveRef(eleve);
                        try {
                            bilanCycle.setCycle(new BigInteger(String.valueOf(valueCycle)));
                        }catch (Exception e){
                            log.error("method setSocleSyntheseEnsCpl new BigInteger valueCycle : " + valueCycle + " " + e.getMessage());

                        }
                        bilanCycle.setMillesime(millesime);
                        bilansCycle.getBilanCycle().add(bilanCycle);

                    } else {
                        //supprimer l'élève de la liste de la Balise ELEVES
                        eleves.getEleve().remove(eleve);
                        //affecter les différentes erreurs en fonction des conditions non respectées
                        erreursEleve.put("idEleve", idEleve).put("prenom", eleve.getPrenom()).put("nom", eleve.getNom()).put("classe", eleve.getCodeDivision());
                        JsonArray erreurs = new fr.wseduc.webutils.collections.JsonArray();
                        if ( mapIdDomainePosition.size() != mapIdDomaineCodeDomaine.size() || !bmapSansIdDomaineCPDETR ) {
                            erreurs.add(new JsonObject().put("socleDomaine", "Il manque des domaines du socle commun a cet eleve"));
                            erreursEleve.put("typeErreur", erreurs);
                            if ( syntheseEleve.size() == 0 ) {
                                erreurs.add(new JsonObject().put("synthese", "La synthese du bilan de fin de cycle n'est pas completee "));
                                erreursEleve.put("typeErreur", erreurs);
                            }
                        } else if ( syntheseEleve.size() == 0 ) {
                            erreurs.add(new JsonObject().put("synthese", "La synthese du bilan de fin de cycle n'est pas completee "));
                            erreursEleve.put("typeErreur", erreurs);
                        }
                        if(!ensCplEleve.containsKey("id_eleve")){
                            erreurs.add(new JsonObject().put("ensCpl", "L'enseignement de complement n'est pas renseigne"));
                            erreursEleve.put("typeErreur", erreurs);
                        }
                    }
                } else {//si l'élève n'est pas dans la map alors il n'a aucune évaluation et
                    // il faut le supprimer du xml donc de la list des élèves de la balise ELEVES
                    eleves.getEleve().remove(eleve);
                    erreursEleve.put("idEleve", idEleve).put("prenom", eleve.getPrenom()).put("nom", eleve.getNom()).put("classe", eleve.getCodeDivision());
                    JsonArray erreurs = new fr.wseduc.webutils.collections.JsonArray();
                    erreurs.add(new JsonObject().put("socleDomaine", "Aucun domaine du socle commun "));
                    erreursEleve.put("typeErreur", erreurs);
                    if(!ensCplEleve.containsKey("id_eleve")){
                        erreurs.add(new JsonObject().put("ensCpl", "L'enseignement de complement n'est pas renseigne"));
                        erreursEleve.put("typeErreur", erreurs);
                    }
                    if(!syntheseEleve.containsKey("id_eleve")){
                        erreurs.add(new JsonObject().put("synthese", "La synthese du bilan de fin de cycle n'est pas completee "));
                        erreursEleve.put("typeErreur", erreurs);
                    }
                }
                if (erreursEleve.size() > 0) {
                    listErreursEleves.add(erreursEleve);
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
    private void getResultsElevesByDomaine( List<String> listIdsEleve, String idClass, String idStructure, Long idCycle, Handler <Either<String,  Map<String, Map<Long, Integer>>>> handler){
        final Map<String, Map<Long, Integer>> resultatsEleves = new HashMap<>();
        final String[] idsEleve = listIdsEleve.toArray(new String[listIdsEleve.size()]);
        bfcService.buildBFC(false, idsEleve, idClass, idStructure, null, idCycle, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(final Either<String, JsonObject> repBuildBFC) {
                if (repBuildBFC.isRight()) {
                    // On récupère la map des domaine dispensé par élève
                    dispenseDomaineEleveService.mapOfDispenseDomaineByIdEleve(listIdsEleve, new Handler<Either<String, Map<String, Map<Long, Boolean>>>>() {
                        @Override
                        public void handle(Either<String, Map<String, Map<Long, Boolean>>> respDispenseDomaine) {
                            if (respDispenseDomaine.isRight()) {
                                Map<String, Map<Long, Boolean>> mapIdEleveIdDomainedispense = respDispenseDomaine.right().getValue();
                                for (String idEleve : idsEleve) {
                                    JsonArray resultats = repBuildBFC.right().getValue().getJsonArray(idEleve);
                                    Map<Long, Integer> resultEleves = new HashMap<>();

                                    // si pas de resultats, on passe a l'élève suivant
                                    if (resultats == null) {
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
                                    handler.handle(new Either.Right<String,Map<String, Map<Long, Integer>>>(resultatsEleves));
                                }
                            }
                        }
                    });
                } else {
                    handler.handle(new Either.Left<String, Map<String, Map<Long, Integer>>>("getResultsElevesByDomaine : bfcService.buidBFC : " + repBuildBFC.left().getValue()));
                    log.error("getBaliseBilansCycle XML buildBFC map<idEleve,map<idDomaine,positionnement>> : " + repBuildBFC.left().getValue());
                }
            }
        });
    }

    /**
     * permet de completer tous les attributs de la balise BilanCycle et de la setter à donnees
     * sauf les attributs de date, synthese et enseignements de complement
     * @param donnees permet de recuperer les eleves
     * @param idsClass
     * @param idStructure
     * @param handler
     */

    private void getBaliseBilansCycle(final Donnees donnees,final List<String> idsClass, final String idStructure,
                                      final Map<String,JsonObject> dateCreationVerrouByClasse, final Handler<Either<String,JsonArray>> handler) {
        final Donnees.BilansCycle bilansCycle = objectFactory.createDonneesBilansCycle();
        final List<ResponsableEtab> responsablesEtab = donnees.getResponsablesEtab().getResponsableEtab();
        final Donnees.Eleves eleves = donnees.getEleves();
        final Integer nbElevesTotal = eleves.getEleve().size();
        final String millesime = getMillesime();
        final AtomicInteger nbEleveCompteur = new AtomicInteger(0);
        final Map<String, List<String>> mapIdClassIdsEleve = eleves.getMapIdClassIdsEleve();
        log.info("DEBUT : method getBaliseBilansCycle : nombreEleve : "+eleves.getEleve().size());
        listErreursEleves = new fr.wseduc.webutils.collections.JsonArray();

        getIdClassIdCycleValue(idsClass, new Handler<Either<String, List<Map>>>() {
            @Override
            public void handle(Either<String, List<Map>> repIdClassIdCycleValue) {

                if (repIdClassIdCycleValue.isRight()) {
                    final List<Map> mapIdClassIdCycleValue = repIdClassIdCycleValue.right().getValue();
                    final Map mapIdClassIdCycle = mapIdClassIdCycleValue.get(0);//map<IdClass,IdCycle>
                    final Map mapIdCycleValue = mapIdClassIdCycleValue.get(1);//map<IdCycle,ValueCycle>
                    //on parcourt les classes
                    for (final Map.Entry<String, List<String>> listIdsEleve : mapIdClassIdsEleve.entrySet()) {
                        //récupère un tableau de sting idEleve nécessaire pour la méthode buildBFC de bfcService
                        final String[] idsEleve = listIdsEleve.getValue().toArray(new String[listIdsEleve.getValue().size()]);
                        final String idClass = listIdsEleve.getKey();
                        final JsonObject datesCreationVerrou = dateCreationVerrouByClasse.get(idClass);
                        final List<String> listIdEleves = listIdsEleve.getValue();

                        getResultsElevesByDomaine(listIdEleves, idClass, idStructure,(Long) mapIdClassIdCycle.get(idClass), new Handler<Either<String, Map<String, Map<Long, Integer>>>>() {
                            @Override
                            public void handle(Either<String, Map<String, Map<Long, Integer>>> resultatsEleves) {
                                if(resultatsEleves.isRight()) {
                                    final Map<String, Map<Long, Integer>> mapIdEleveIdDomainePosition = resultatsEleves.right().getValue();
                                    getMapCodeDomaineById(idClass, new Handler<Either<String, Map<Long, String>>>() {
                                        @Override
                                        public void handle(Either<String, Map<Long, String>> repMapCodeDomaineId) {
                                            if (repMapCodeDomaineId.isRight()) {
                                                final Map<Long, String> mapIdDomaineCodeDomaine = repMapCodeDomaineId.right().getValue();
                                                final Long idCycle = (Long) mapIdClassIdCycle.get(idClass);
                                                final Long valueCycle = (Long) mapIdCycleValue.get(idCycle);
                                                if (idCycle != null) {
                                                    bfcSynthseService.getBfcSyntheseByIdsEleve(idsEleve, idCycle, new Handler<Either<String, JsonArray>>() {
                                                        @Override
                                                        public void handle(Either<String, JsonArray> repSynthese) {
                                                            if (repSynthese.isRight()) {
                                                                final JsonArray synthesesEleves = repSynthese.right().getValue();
                                                                eleveEnsCpl.listNiveauCplByEleves(idsEleve, new Handler<Either<String, JsonArray>>() {
                                                                    @Override
                                                                    public void handle(Either<String, JsonArray> repEleveEnsCpl) {
                                                                        if (repEleveEnsCpl.isRight()) {
                                                                            final JsonArray ensCplsEleves = repEleveEnsCpl.right().getValue();
                                                                            setSocleSyntheseEnsCpl(mapIdDomaineCodeDomaine, idsEleve, nbEleveCompteur, eleves, ensCplsEleves, synthesesEleves,
                                                                                    mapIdEleveIdDomainePosition, responsablesEtab, valueCycle, millesime, bilansCycle, datesCreationVerrou);

                                                                            log.info("FIN method getBaliseBilansCycle nombre d'eleve dans la classe en cours : " + idsEleve.length);
                                                                            log.info("FIN method getBaliseBilansCycle nombre de bilans de cycle complets : " + bilansCycle.getBilanCycle().size());
                                                                            log.info("nb d'eleves au depart : " + nbElevesTotal);
                                                                            log.info("nb d'eleve parcouru : " + nbEleveCompteur.intValue());
                                                                            if (nbEleveCompteur.intValue() == nbElevesTotal.intValue()) {
                                                                                if (bilansCycle.getBilanCycle().size() != 0) {
                                                                                    donnees.setBilansCycle(bilansCycle);
                                                                                    log.info("FIN method getBaliseBilansCycle nombre d'eleve avec un bilan cycle complet " + eleves.getEleve().size());
                                                                                }
                                                                                log.info("FIN method getBaliseBilansCycle nombre d'eleve avec un bilan cycle incomplet " + listErreursEleves.size());
                                                                                handler.handle(new Either.Right<String, JsonArray>(listErreursEleves));
                                                                            }

                                                                        } else {
                                                                            handler.handle(new Either.Left<String, JsonArray>("getBaliseBilansCycle XML requete enseignement complement: " + repEleveEnsCpl.left().getValue()));
                                                                            log.error("getBaliseBilansCycle requete enseignementComplement: " + repEleveEnsCpl.left().getValue());
                                                                        }
                                                                    }
                                                                });

                                                            } else {
                                                                handler.handle(new Either.Left<String, JsonArray>("getBaliseBilansCycle XML requete synthese du BFC: " + repSynthese.left().getValue()));
                                                                log.error("getBaliseBilansCycle requete synthese du BFC: " + repSynthese.left().getValue());
                                                            }
                                                        }
                                                    });
                                                } else {
                                                    handler.handle(new Either.Left<String, JsonArray>("getBaliseBilansCycle XML idCycle  :  " + idCycle));
                                                    log.error("getBaliseBilansCycle idCycle :  " + idCycle);
                                                }
                                            } else {
                                                handler.handle(new Either.Left<String, JsonArray>("getBaliseBilansCycle : Map<String, Map<Long, Integer>>> resultatsEleves : " + resultatsEleves.left().getValue()));
                                                log.error("getBaliseBilansCycle Map<String, Map<Long, Integer>>> resultatsEleves :  " + resultatsEleves.left().getValue());
                                            }

                                        }
                                    });
                                }else{
                                    handler.handle(new Either.Left<String,JsonArray>("getBaliseBilansEleve :  : " + repIdClassIdCycleValue.left().getValue()));
                                    log.error("getBaliseBilansCycle XML:list (map<idclasse,idCycle>,map<idCycle,cycle>) " + repIdClassIdCycleValue.left().getValue());
                                }
                            }
                        });
                    }
                } else {
                    handler.handle(new Either.Left<String,JsonArray>("getBaliseBilansEleve : list (map<idclasse,idCycle>,map<idCycle,cycle>) : " + repIdClassIdCycleValue.left().getValue()));
                    log.error("getBaliseBilansCycle XML:list (map<idclasse,idCycle>,map<idCycle,cycle>) " + repIdClassIdCycleValue.left().getValue());
                }
            }

        });
    }

    private void getBaliseDisciplines(final Donnees donnees, final String idStructure, final Handler<String> handler) {
        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatieresForUser")
                .put("userType", "Personnel")
                .put("idUser", "null")
                .put("idStructure", idStructure)
                .put("onlyId", false);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                try {
                    JsonArray listSubject = body.getJsonArray("results");
                    donnees.setDisciplines(objectFactory.createDonneesDisciplines());
                    if (listSubject != null && !listSubject.isEmpty()) {
                        listSubject.forEach(item -> {
                            JsonObject currentSubject = (JsonObject) item;
                            Discipline discipline = objectFactory.createDiscipline();
                            discipline.setCode(currentSubject.getString("externalId"));
                            discipline.setId("DIS_" + currentSubject.getString("id"));
                            discipline.setLibelle(currentSubject.getString("name"));
                            discipline.setModaliteElection(ModaliteElection.fromValue("O"));
                            donnees.getDisciplines().getDiscipline().add(discipline);
                        });
                        JsonObject response = new JsonObject();
                        response.put("status", "success");
                        response.put("data", "success");
                        handler.handle("success");
                    }
                }catch(Throwable e){
                    handler.handle("method getBaliseResponsable : " + e.getMessage());
                    log.error("method getBaliseResponsable : " + e.getMessage());
                }
            } else {
                log.error("method getBaliseDisciplines discipline : error eb matiere.getMatieresForUser ko, idStructure: "+ idStructure);
                handler.handle("getBaliseDisciplines discipline : error eb matiere.getMatieresForUser ko");
            }
        }));
    }

    private void getBalisePeriodes(final Donnees donnees, final List<Integer> wantedPeriodes, final JsonObject periodesAdded, final String idStructure, final List<String> idClasse, final Handler<String> handler) {
        JsonObject action = new JsonObject()
                .put("action", "periode.getPeriodes")
                .put("idGroupes", idClasse)
                .put("idEtablissement", idStructure);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                try {
                    JsonArray periodeList = body.getJsonArray("result");
                    donnees.setPeriodes(objectFactory.createDonneesPeriodes());
                    periodeList.forEach(item -> {
                        JsonObject currentPeriode = (JsonObject) item;

                        Integer targetPeriode = wantedPeriodes.stream()
                                .filter(el -> el.equals(currentPeriode.getInteger("id_type")))
                                .findFirst()
                                .orElse(null);
                        if(targetPeriode != null){
                            periodesAdded.put("P_"+currentPeriode.getInteger("id").toString(), currentPeriode);
                            Periode periode = objectFactory.createPeriode();
                            periode.setId("P_"+currentPeriode.getInteger("id").toString());
                            periode.setMillesime(currentPeriode.getString("timestamp_dt").substring(0, 4));
                            periode.setIndice(currentPeriode.getInteger("id_type"));
                            periode.setNbPeriodes(periodeList.size());
                            donnees.getPeriodes().getPeriode().add(periode);
                        }
                    });
                    handler.handle("success");
                }
                catch(Throwable e){
                    handler.handle("method getBalisePeriodes : " + e.getMessage());
                    log.error("method getBalisePeriodes : " + e.getMessage());
                }
            }
            else {
                log.error("method getBalisePeriodes : error eb periode.getPeriodes ko");
                handler.handle("getBalisePeriodes : error eb periode.getPeriodes ko");
            }
        }));
    }

    private void getBaliseEnseignants(final Donnees donnees, final String structureId, List<String> idsClasse, final Handler<String> handler) {
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
            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject body = message.body();
                    if ("ok".equals(body.getString("status"))) {
                        JsonArray teachersList = body.getJsonArray("results");
                        for (Integer k = 0; k < teachersList.size(); k++) {
                            addorFindTeacherBalise(donnees, teachersList.getJsonObject(k));
                        }
                        resp1FutureComposite.complete();
                    }
                }
            }));
        }
        CompositeFuture.all(futureMyResponse1Lst).setHandler(
                event -> handler.handle("success"));
    }


    private void getBaliseEnseignantFromId(final Donnees donnees, String idEnseignant, final Handler<String> handler) {
        JsonArray ids = new JsonArray().add(idEnseignant);
        JsonObject action = new JsonObject()
                .put("action", "user.getUsers")
                .put("idUsers", ids);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status")) && body.getJsonArray("results").size() == 1 ) {
                    JsonArray teachersList = body.getJsonArray("results");
                    for (Integer k = 0; k < teachersList.size(); k++) {
                        addorFindTeacherBalise(donnees, teachersList.getJsonObject(k));
                    }
                    handler.handle("success");
                }
            }
        }));
    }

    private Enseignant addorFindTeacherBalise(Donnees donnees, JsonObject enseignant){
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
            existing.setType(fromValue("epp"));
            existing.setIdSts(new BigInteger(20, new Random()));
            log.info(existing.getIdSts() + "  enseignant Id sts");
            donnees.getEnseignants().getEnseignant().add(existing);
        }
        return existing;
    };

    private void getApEpiParcoursBalises(final Donnees donnees, final List<String> idsClass, final String idStructure, JsonObject epiGroupAdded, JsonObject accGroupAdded, final Handler<String> handler) {
        List<String> idTeachers = new ArrayList<>();

        donnees.setEpisGroupes(objectFactory.createDonneesEpisGroupes());
        donnees.setEpis(objectFactory.createDonneesEpis());

        donnees.setAccPersosGroupes(objectFactory.createDonneesAccPersosGroupes());
        donnees.setAccPersos(objectFactory.createDonneesAccPersos());

        donnees.setParcoursCommuns(objectFactory.createDonneesParcoursCommuns());

        elementBilanPeriodiqueService.getElementsBilanPeriodique(null, null, idStructure, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray elementBilanPeriodique = event.right().getValue();
                    if (elementBilanPeriodique == null || elementBilanPeriodique.isEmpty()) {
                        handler.handle("getApEpiParcoursBalises no data available ");
                    }
                    int imax = elementBilanPeriodique.size();
                    for (int i = 0; i < imax; i++) {
                        JsonObject element = elementBilanPeriodique.getJsonObject(i);
                        if (element != null) {
                            Long typeElement = element.getLong("type");
                            if (3L == typeElement) { //parcours group
                                addParcoursGroup(element);
                            } else if (2L == typeElement) { //ap class/group
                                addAccGroup(element, accGroupAdded);
                            } else if (1L == typeElement) { //epi group
                                addEpiGroup(element, epiGroupAdded);
                            }
                        }
                    }
                    handler.handle("success");
                }
                else {
                    handler.handle("getApEpiParcoursBalises no data available ");
                }
            }

            private void addParcoursGroup(JsonObject element) {
                List<Periode> periodes = donnees.getPeriodes().getPeriode();
                int kmax = periodes.size();
                for (int k = 0; k < kmax; k++) {
                    JsonObject theme = element.getJsonObject("theme");
                    Periode currentPeriode = periodes.get(k);
                    Donnees.ParcoursCommuns.ParcoursCommun parcoursCommun = objectFactory.createDonneesParcoursCommunsParcoursCommun();
                    Parcours parcours = objectFactory.createParcours();
                    parcoursCommun.setPeriodeRef(currentPeriode);
                    parcoursCommun.setCodeDivision(theme.getString("code"));
                    parcours.setCode(CodeParcours.fromValue(theme.getString("code")));
                    parcours.setValue(theme.getString("libelle"));
                    parcoursCommun.getParcours().add(parcours);
                    donnees.getParcoursCommuns().getParcoursCommun().add(parcoursCommun);
                }
            }

            private void addEpiGroup(JsonObject element, JsonObject epiGroupAdded) {
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
                    EpiGroupe epiGroupe = objectFactory.createEpiGroupe();
                    JsonObject theme = element.getJsonObject("theme");

                    epi.setId("EPI_" + theme.getInteger("id"));
                    epi.setIntitule(theme.getString("libelle"));
                    epi.setDescription(element.getString("description"));
                    epi.setThematique(ThematiqueEpi.fromValue(theme.getString("code")));

                    EpiGroupe.EnseignantsDisciplines enseignantsDisciplinesEpi = objectFactory.createEpiGroupeEnseignantsDisciplines();

                    JsonArray intervenantsMatieres = element.getJsonArray("intervenantsMatieres");
                    int jmax = intervenantsMatieres.size();
                    final List<Future> futureMyResponse1Lst = new ArrayList<>();

                    for (int j = 0; j < jmax; j++) {
                        final Future<JsonObject> resp1FutureComposite = Future.future();
                        futureMyResponse1Lst.add(resp1FutureComposite);
                        addEnseignantDiscipline(intervenantsMatieres.getJsonObject(j), enseignantsDisciplinesEpi.getEnseignantDiscipline(), epi.getDisciplineRefs(), donnees, resp1FutureComposite);
                    }
                    CompositeFuture.all(futureMyResponse1Lst).setHandler(event -> {
                        epiGroupe.setId("EPI_GROUPE_" + element.getInteger("id"));
                        epiGroupe.setEpiRef(epi);
                        epiGroupe.setEnseignantsDisciplines(enseignantsDisciplinesEpi);
                        epiGroupe.setIntitule(element.getString("libelle"));

                        for (int i = 0; i < element.getJsonArray("groupes").size(); i++) {
                            JsonObject currentClass = element.getJsonArray("groupes").getJsonObject(i);
                            epiGroupAdded.put(currentClass.getString("id"), epiGroupe.getId());
                        }
                        donnees.getEpis().getEpi().add(epi);
                        donnees.getEpisGroupes().getEpiGroupe().add(epiGroupe);
                    });



                }
            }


            private void addAccGroup(JsonObject element, JsonObject accGroupAdded) {
                if (element != null
                        && !element.isEmpty()
                        && element.containsKey("id")
                        && element.containsKey("libelle")
                        && element.containsKey("description")
                        && element.containsKey("groupes")
                        && element.containsKey("intervenantsMatieres")
                        && element.getJsonArray("intervenantsMatieres").size() > 0
                        && element.getJsonArray("groupes").size() > 0) {


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
                        addEnseignantDiscipline(intervenantsMatieres.getJsonObject(i), enseignantsDisciplinesAcc.getEnseignantDiscipline(), accPerso.getDisciplineRefs(), donnees, resp1FutureComposite);
                    }

                    CompositeFuture.all(futureMyResponse1Lst).setHandler(event -> {
                        accPersoGroupe.setId("ACC_GROUPE_" + element.getInteger("id"));
                        accPersoGroupe.setAccPersoRef(accPerso);
                        accPersoGroupe.setEnseignantsDisciplines(enseignantsDisciplinesAcc);
                        accPersoGroupe.setIntitule(element.getString("libelle"));

                        for (int i = 0; i < element.getJsonArray("groupes").size(); i++) {
                            JsonObject currentClass = element.getJsonArray("groupes").getJsonObject(i);
                            accGroupAdded.put(currentClass.getString("id"), accPersoGroupe.getId());
                        }

                        donnees.getAccPersos().getAccPerso().add(accPerso);
                        donnees.getAccPersosGroupes().getAccPersoGroupe().add(accPersoGroupe);
                    });
                }
            }

        });
    }

    private void addEnseignantDiscipline(JsonObject currentIntervenantMatiere, List<EnseignantDiscipline> enseignantDiscipline, List<Object> disciplineRefs, final Donnees donnees, final Future<JsonObject> resp1FutureComposite) {
        Discipline currentSubj = getDisciplineInXML(currentIntervenantMatiere.getJsonObject("matiere").getString("id"), donnees);
        if (currentSubj != null) {
            Enseignant currentEnseignant = getEnseignantInXML(currentIntervenantMatiere.getJsonObject("intervenant").getString("id"), donnees);
            if (currentEnseignant == null) {
                getBaliseEnseignantFromId(donnees, currentIntervenantMatiere.getJsonObject("intervenant").getString("id"),
                        (String event) -> {
                            if (event.equals("success")) {
                                Enseignant newEnseignant = getEnseignantInXML(currentIntervenantMatiere.getJsonObject("intervenant").getString("id"), donnees);
                                finalInsertAddEnseignantDiscipline(enseignantDiscipline, disciplineRefs, resp1FutureComposite, currentSubj, newEnseignant);
                            }
                        });
            }else {
                finalInsertAddEnseignantDiscipline(enseignantDiscipline, disciplineRefs, resp1FutureComposite, currentSubj, currentEnseignant);
            }
        } else {
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
        }
        else {
            resp1FutureComposite.complete();
        }
    }


    /**
     * permet de completer tous les attributs de la balise BilansPeriodiques et de la setter à donnees
     * @param donnees     permet de recuperer les eleves
     *
     * @param periodesAdded
     * @param epiGroupAdded
     * @param accGroupAdded
     * @param handler
     */

    private void getBaliseBilansPeriodiques(final Donnees donnees, final String idStructure,
                                            JsonObject periodesAdded, final Map<String, JsonObject> dateCreationVerrouByClasse, final JsonObject epiGroupAdded, final JsonObject accGroupAdded, final Handler<Either.Right<String, JsonObject>> handler) {

        final Donnees.BilansPeriodiques bilansPeriodiques = objectFactory.createDonneesBilansPeriodiques();
        final List<ResponsableEtab> responsablesEtab = donnees.getResponsablesEtab().getResponsableEtab();
        final Donnees.Eleves eleves = donnees.getEleves();
        final Donnees.Periodes periodes = donnees.getPeriodes();
        final Integer nbElevesTotal = eleves.getEleve().size();
        final String millesime = getMillesime();
        final AtomicInteger nbEleveCompteur = new AtomicInteger(0);
        final Map<String, List<String>> mapIdClassIdsEleve = eleves.getMapIdClassIdsEleve();
        listErreursEleves = new fr.wseduc.webutils.collections.JsonArray();
        final AtomicInteger originalSize = new AtomicInteger();



        Handler getOut = new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> suiviAcquisResponse) {
                originalSize.getAndDecrement();
                if (originalSize.get() == 0) {
                    log.info("Get OUTTTTT " + bilansPeriodiques.getBilanPeriodique().size() + "  ==  " + eleves.getEleve().size());
                    donnees.setBilansPeriodiques(bilansPeriodiques);
                    handler.handle(new Either.Right<String, JsonObject>(suiviAcquisResponse.right().getValue()));
                } else {
                    log.info("waiting all child done");
                }
            }
        };

        if( !(eleves.getEleve().size() > 0) || !(periodes.getPeriode().size() > 0)){
            handler.handle(new Either.Right<String, JsonObject>(new JsonObject().put("error", "getBaliseBilansPeriodiques : Eleves or Periodes are empty")));
            return;
        }
        //For each eleve create his periodic bilan
        for (Integer i = 0; i < eleves.getEleve().size(); i++) {
            Eleve currentEleve = eleves.getEleve().get(i);
            for (Integer i2 = 0; i2 < periodes.getPeriode().size(); i2++) {
                originalSize.getAndIncrement();
                JsonObject response = new JsonObject();
                Periode currentPeriode = periodes.getPeriode().get(i2);
                if(periodesAdded.containsKey(currentPeriode.getId())
                        && periodesAdded.getJsonObject(currentPeriode.getId()).containsKey("id_classe")
                        && currentEleve.getId_Class().equals(periodesAdded.getJsonObject(currentPeriode.getId()).getString("id_classe"))) {
                    JsonObject periodeJSon = periodesAdded.getJsonObject(currentPeriode.getId());
                    Future<JsonObject> getRetardsAndAbsencesFuture = Future.future();
                    Future<JsonObject> getAppreciationsFuture = Future.future();
                    Future<JsonObject> getSuiviAcquisFuture = Future.future();
                    Future<JsonObject> getSyntheseFuture = Future.future();
                    CompositeFuture.all(getRetardsAndAbsencesFuture, getAppreciationsFuture, getSuiviAcquisFuture, getSyntheseFuture).setHandler(
                            event -> {
                                response.put("status", 200);
                                getOut.handle(new Either.Right<String, JsonObject>(response));
                            });

                    final BilanPeriodique bilanPeriodique = objectFactory.createBilanPeriodique();

                    addDateConseilDateScolarite(bilanPeriodique);

                    if(!addresponsableEtabRef(donnees, response, bilanPeriodique)){
                        response.put("status", 400);
                        response.put("message", "Responsable do not exist on Bilan periodique balise : eleve : "+ currentEleve.getIdNeo4j() + " " +currentEleve.getNom() + " periode : "+ currentPeriode.getId() );
                        getOut.handle(new Either.Right<String, JsonObject>(response));
                        return;
                    }

                    syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(periodeJSon.getLong("id_type"), currentEleve.getIdNeo4j(),
                            eventSynthese -> {
                                log.info("syntheseBilanPeriodiqueService");
                                getSyntheseFuture.complete();
                            });


                    bilanPeriodiqueService.getRetardsAndAbsences(currentEleve.getIdNeo4j(), new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> eventViesco) {
                            addVieScolairePerso(eventViesco, bilanPeriodique);
                            bilansPeriodiques.getBilanPeriodique().add(bilanPeriodique);
                            getRetardsAndAbsencesFuture.complete();
                        }

                        private void addVieScolairePerso(Either<String, JsonArray> eventViesco, BilanPeriodique bilanPeriodique) {
                            if (eventViesco.isRight()) {
                                final JsonArray vieScoRightValue = eventViesco.right().getValue();

                                JsonObject viesco = (JsonObject) vieScoRightValue.stream()
                                        .filter(el -> periodeJSon.getInteger("id_type") == ((JsonObject) el).getInteger("id_periode"))
                                        .findFirst()
                                        .orElse(null);

                                if (viesco != null && viesco.containsKey("id_eleve") && viesco.containsKey("id_eleve")) {
                                    VieScolaire viescolare = objectFactory.createVieScolaire();
                                    viescolare.setNbAbsInjustifiees(BigInteger.valueOf(viesco.getLong("abs_non_just") != null ? viesco.getLong("abs_non_just") : 0L));
                                    viescolare.setNbAbsJustifiees(BigInteger.valueOf(viesco.getLong("abs_just") != null ? viesco.getLong("abs_just") : 0L));
                                    viescolare.setNbRetards(BigInteger.valueOf(viesco.getLong("retard") != null ? viesco.getLong("retard") : 0L));
                                    viescolare.setNbHeuresManquees(BigInteger.valueOf(viesco.getLong("abs_totale_heure") != null ? viesco.getLong("abs_totale_heure") : 0L));
                                    bilanPeriodique.setVieScolaire(viescolare);
                                }
                                else {
                                    response.put("status", 400);
                                    response.put("message", "VieScolaire do not exist on Bilan periodique balise : eleve : " + currentEleve.getIdNeo4j() + " " + currentEleve.getNom() + " periode : " + currentPeriode.getId());
                                }
                            }
                            else {
                                response.put("status", 400);
                                response.put("message", "VieScolaire, leftResponse bilanPeriodiqueService.getRetardsAndAbsences  : " + currentEleve.getIdNeo4j() + " " + currentEleve.getNom() + " periode : " + currentPeriode.getId());
                                log.error("getBaliseBilansPeriodiques : bilanPeriodiqueService.getRetardsAndAbsences  " + eventViesco);
                            }
                        }
                    });

                    elementBilanPeriodiqueService.getApprecBilanPerEleve(Collections.singletonList(periodeJSon.getString("id_classe")),
                            periodeJSon.getInteger("id_type").toString(), null, currentEleve.getIdNeo4j(),
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> eventApp) {

                                    if (eventApp.isRight()) {
                                        final JsonArray appreciations = eventApp.right().getValue();

                                        for (int i = 0; i < appreciations.size(); i++) {
                                            JsonObject element = appreciations.getJsonObject(i);
                                            if (element.getInteger("id_periode") == periodeJSon.getInteger("id_type")) {
                                                Long typeElem = element.getLong("id_elt_bilan_periodique");
                                                if (3L == typeElem) {//parcours
                                                    addParcoursEleve(element);
                                                } else if (2L == typeElem) {
                                                    addEpiEleve(element);
                                                } else if (1L == typeElem) {//ap
                                                    addApEleve(element);
                                                }
                                            }
                                        }
                                    }
                                    getAppreciationsFuture.complete();
                                }

                                private void addApEleve(JsonObject element) {
                                    String extGroup = element.getString("id_groupe");
                                    if (accGroupAdded.size() > 0 && extGroup != null && accGroupAdded.containsKey(extGroup)) {

                                        AccPersoGroupe accGroupe = donnees.getAccPersosGroupes().getAccPersoGroupe().stream()
                                                .filter(el -> el.getId().equals(accGroupAdded.getString(extGroup)))
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
                                    String extGroup = element.getString("id_groupe");
                                    if (epiGroupAdded.size() > 0 && extGroup != null && epiGroupAdded.containsKey(extGroup)) {

                                        EpiGroupe epiGroupe = donnees.getEpisGroupes().getEpiGroupe().stream()
                                                .filter(el -> el.getId().equals(epiGroupAdded.getString(extGroup)))
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

                    bilanPeriodiqueService.getSuiviAcquis(idStructure, new Long(currentPeriode.getIndice()), currentEleve.getIdNeo4j(), currentEleve.getId_Class(),
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> suiviAcquisResponse) {
                                    if (suiviAcquisResponse.isRight()) {
                                        final JsonArray suiviAcquis = suiviAcquisResponse.right().getValue();
                                        //init Bilan Periodique
                                        donnees.setElementsProgramme(objectFactory.createDonneesElementsProgramme());
                                        bilanPeriodique.setEleveRef(currentEleve);
                                        bilanPeriodique.setPeriodeRef(currentPeriode);
                                        addDateVerrou(bilanPeriodique);
                                        addResponsable(bilanPeriodique);
                                        addListeAcquis(suiviAcquis, bilanPeriodique);
                                    } else {
                                        bilansPeriodiques.getBilanPeriodique().remove(currentEleve.getIdNeo4j());
                                    }
                                    getSuiviAcquisFuture.complete();
                                }


                                private void addListeAcquis(JsonArray suiviAcquis, BilanPeriodique bilanPeriodique) {
                                    BilanPeriodique.ListeAcquis aquisEleveList = objectFactory.createBilanPeriodiqueListeAcquis();
                                    for (Integer i = 0; i < suiviAcquis.size(); i++) {
                                        final JsonObject currentAcquis = suiviAcquis.getJsonObject(i);
                                        Acquis aquisEleve = addAcquis(currentAcquis);
                                        addMissingTeacherToXml(aquisEleveList, currentAcquis, aquisEleve);
                                    }
                                    bilanPeriodique.setListeAcquis(aquisEleveList);
                                }

                                private void addResponsable(BilanPeriodique bilanPeriodique) {
                                    if (currentEleve.getResponsableList() != null && currentEleve.getResponsableList().size() > 0) {
                                        BilanPeriodique.Responsables responsablesEleve = objectFactory.createBilanPeriodiqueResponsables();
                                        responsablesEleve.getResponsable().addAll(currentEleve.getResponsableList());
                                        bilanPeriodique.setResponsables(responsablesEleve);
                                    }
                                }

                                private void addDateVerrou(BilanPeriodique bilanPeriodique) {
                                    final JsonObject datesCreationVerrou = dateCreationVerrouByClasse.get(currentEleve.getId_Class());
                                    bilanPeriodique.setDateVerrou(datesCreationVerrou.getString("date_verrou").substring(0, 19));
                                }

                                private Acquis addAcquis(JsonObject currentAcquis) {
                                    Acquis aquisEleve = objectFactory.createAcquis();

                                    addAppreciation(currentAcquis, aquisEleve, currentPeriode);
                                    addDiscipline(currentAcquis, aquisEleve);
                                    addElementProgramme(currentAcquis, aquisEleve);
                                    return aquisEleve;
                                }

                                private void addAppreciation(JsonObject currentAcquis, Acquis aquisEleve, Periode currentPeriode) {
                                    JsonObject app = getObjectForPeriode(currentAcquis.getJsonArray("appreciations"), (long) currentPeriode.getIndice(), "id_periode");
                                    if(app != null && app.containsKey("appreciationByClasse") && app.getJsonArray("appreciationByClasse").size() > 0) {
                                        int imax = app.getJsonArray("appreciationByClasse").size();
                                        for(int i = 0; i < imax; i++ ) {
                                            app = app.getJsonArray("appreciationByClasse").getJsonObject(i);
                                            if (app.containsKey("appreciation")) {
                                                String appTmp = app.getString("appreciation");
                                                if(appTmp != null && !appTmp.isEmpty()){
                                                    aquisEleve.setAppreciation(appTmp);
                                                }
                                            }
                                        }
                                    }
                                }

                                private JsonObject getObjectForPeriode(JsonArray array, Long idPeriode, String key) {
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

                                private void addDiscipline(JsonObject currentAcquis, Acquis aquisEleve) {
                                    Discipline currentSubj = getDisciplineInXML(currentAcquis.getString("id_matiere"), donnees);
                                    if (currentSubj != null) {
                                        aquisEleve.setDisciplineRef(currentSubj);
                                    }
                                }

                                private void addElementProgramme(JsonObject currentAcquis, Acquis aquisEleve) {
                                    String epLabel = currentAcquis.getString("elementsProgramme");
                                    if(epLabel != null && !epLabel.isEmpty()){
                                        String epId = generateElementProgrammeId(epLabel);

                                        ElementProgramme ep = donnees.getElementsProgramme().getElementProgramme().stream()
                                                .filter(cep -> cep.getId().equals(epId))
                                                .findFirst()
                                                .orElse(null);

                                        if(ep == null){
                                            ep = objectFactory.createElementProgramme();
                                            ep.setId(epId);
                                            ep.setLibelle(epLabel);
                                            donnees.getElementsProgramme().getElementProgramme().add(ep);
                                        }
                                        aquisEleve.getElementProgrammeRefs().add(ep);
                                    }
                                }

                                private void addMissingTeacherToXml(BilanPeriodique.ListeAcquis aquisEleveList, JsonObject currentAcquis, Acquis aquisEleve) {
                                    if(currentAcquis.containsKey("teachers") && !currentAcquis.getJsonArray("teachers").isEmpty()){
                                        JsonArray teachersList = currentAcquis.getJsonArray("teachers");
                                        for (Integer k = 0; k < teachersList.size(); k++) {
                                            Enseignant enseignant = addorFindTeacherBalise(donnees, teachersList.getJsonObject(k));
                                            aquisEleve.getEnseignantRefs().add(enseignant);
                                        }
                                    }
                                    aquisEleveList.getAcquis().add(aquisEleve);
                                }
                            });
                }
                else {
                    response.put("status", 400);
                    response.put("errorMessage", "");
                    getOut.handle(new Either.Right<String, JsonObject>(response));
                }
            }
        }
    }

    private Boolean addresponsableEtabRef(Donnees donnees, JsonObject response, BilanPeriodique bilanPeriodique) {
        if ( donnees.getResponsablesEtab() != null && donnees.getResponsablesEtab().getResponsableEtab().size() > 0) {
            bilanPeriodique.setResponsableEtabRef(donnees.getResponsablesEtab().getResponsableEtab().get(0));
            return true;
        }
        return false;
    }

    private void addDateConseilDateScolarite(BilanPeriodique bilanPeriodique) {
        try {
            XMLGregorianCalendar gregFmt = DatatypeFactory.newInstance().newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            bilanPeriodique.setDateConseilClasse(gregFmt);
            bilanPeriodique.setDateScolarite(gregFmt);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
    }

    private Discipline getDisciplineInXML(String id, Donnees donnees) {
        if (donnees.getDisciplines() == null || donnees.getDisciplines().getDiscipline() == null || donnees.getDisciplines().getDiscipline().size() < 0) {
            return null;
        }
        return donnees.getDisciplines().getDiscipline().stream()
                .filter(dis -> dis.getId().equals("DIS_"+id))
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

    private String generateElementProgrammeId (String elementsProgramme){
        return "EP_"+String.valueOf(elementsProgramme.hashCode());
    }

    private String getMillesime(){
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
                        //validator.validate(xmlFile);
                    } catch (SAXException | IOException e) {
                        log.error("Validation : Export LSU en erreur",e);
                        badRequest(request);
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


