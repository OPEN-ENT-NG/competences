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

package fr.openent.competences;

import fr.openent.competences.bean.Eleve;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import static fr.openent.competences.Competences.TRANSITION_CONFIG;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.Renders.getHost;

import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    protected static final Logger log = LoggerFactory.getLogger(Utils.class);
    protected static  UtilsService utilsService = new DefaultUtilsService();

    /**
     * Recupere l'identifiant de la structure a laquelle appartiennent les classes dont l'identifiant est passe en
     * parametre.
     *
     * @param idClasses Tableau contenant l'identifiant des classes dont on souhaite connaitre la structure.
     * @param handler   Handler contenant l'identifiant de la structure.
     */
    public static void getStructClasses(EventBus eb, String[] idClasses, final Handler<Either<String, String>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "classe.getEtabClasses")
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(idClasses)));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    JsonArray queryResult = body.getJsonArray("results");
                    if (queryResult.size() == 0) {
                        handler.handle(new Either.Left<String, String>("Aucune classe n'a ete trouvee."));
                        log.error("getStructClasses : No classes found with these ids");
                    } else if (queryResult.size() > 1) {
                        // Il est impossible de demander un BFC pour des classes n'appartenant pas au meme etablissement.
                        handler.handle(new Either.Left<String, String>("Les classes n'appartiennent pas au meme etablissement."));
                        log.error("getStructClasses : provided classes are not from the same structure.");
                    } else {
                        JsonObject structure = queryResult.getJsonObject(0);
                        handler.handle(new Either.Right<String, String>(structure.getString("idStructure")));
                    }
                } else {
                    handler.handle(new Either.Left<String, String>(body.getString("message")));
                    log.error("getStructClasses : " + body.getString("message"));
                }
            }
        }));
    }

    /**
     * retourne une classe avec ses groups (ids)
     * @param eb eventbus
     * @param idsClasses array une clase
     * @param handler response l'id de classe avec ses groups s'ils existent sinon retourne que id de la classe
     */
    public static void getGroupesClasse(EventBus eb, final JsonArray idsClasses, final Handler<Either<String, JsonArray>> handler){
        JsonObject action = new JsonObject()
                .put("action", "classe.getGroupesClasse")
                .put("idClasses", idsClasses);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action,Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();

                        if ("ok".equals(body.getString("status"))) {
                            JsonArray queryResult = body.getJsonArray("results");
                            handler.handle(new Either.Right<String, JsonArray>(queryResult));
                        } else {
                            handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                            log.error("getGroupesClasse : " + body.getString("message"));
                        }
                    }
                }));
    }

    public static void getInfosGroupes(EventBus eb, final JsonArray idsClasses, final Handler<Either<String, Map<String, String>>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "classe.getClassesInfo")
                .put("idClasses", idsClasses);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if ("ok".equals(body.getString("status"))) {
                JsonArray queryResult = body.getJsonArray("results");

                if (queryResult != null) {
                    Map<String, String> result = queryResult.stream()
                            .collect(Collectors.toMap(val -> ((JsonObject) val).getString("id") , val -> ((JsonObject) val).getString("name")));

                    handler.handle(new Either.Right<>(result));
                } else {
                    handler.handle(new Either.Left<>("getInfosGroupes : empty result"));
                    log.error("getInfosGroupes : empty result");
                }
            } else {
                handler.handle(new Either.Left<>(body.getString("message")));
                log.error("getInfosGroupes : " + body.getString("message"));
            }
        }));
    }

    public static void getIdElevesClassesGroupes(EventBus eb, final String  idGroupe,
                                                 final Integer idPeriode,
                                                 final int typeClasse,
                                                 final Handler<Either<String, List<String>>> handler) {

        JsonObject action = new JsonObject();
        if(typeClasse == 0) {
            action.put("action", "classe.getEleveClasse")
                    .put("idClasse", idGroupe);

            if(idPeriode != null)  action.put("idPeriode", idPeriode);

        }
        else if(typeClasse == 1 || typeClasse == 2){
            action.put("action", "groupe.listUsersByGroupeEnseignementId")
                    .put("groupEnseignementId", idGroupe)
                    .put("profile", "Student");

            if (idPeriode != null) action.put("idPeriode", idPeriode);
        }


        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                List<String> idEleves = new ArrayList<String>();
                if ("ok".equals(body.getString("status"))) {
                    JsonArray queryResult = body.getJsonArray("results");
                    if(queryResult != null) {
                        for (int i =0; i< queryResult.size(); i++) {
                            idEleves.add(((JsonObject)queryResult.getJsonObject(i)).getString("id"));
                        }
                    }
                    handler.handle(new Either.Right<String, List<String>>(idEleves));

                } else {
                    handler.handle(new Either.Left<String, List<String>>(body.getString("message")));
                    log.error("Error :can not get students of groupe : " + idGroupe);
                }
            }
        }));

    }

    /**
     * Get eleve group
     * @param eb EventBus
     * @param idEleve id Eleve
     * @param handler response eleve groups
     */

    public static void getGroupsEleve (EventBus eb, String idEleve, Handler<Either<String, JsonArray>> handler){
        JsonObject action = new JsonObject()
                .put("action","eleve.getGroups")
                .put("idEleve", idEleve);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler (new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonObject body = message.body();
                if(!"ok".equals(body.getString("status"))){
                    handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                    log.error("getGroupsEleve : "+ body.getString("message"));
                }else{
                    JsonArray idGroupsObjects = body.getJsonArray("results");
                    JsonArray idGroupsResult = new fr.wseduc.webutils.collections.JsonArray();
                    if(idGroupsObjects != null ){
                        if(idGroupsObjects.isEmpty()){
                            idGroupsResult = null;
                        }else{
                            for(int i = 0; i < idGroupsObjects.size(); i++){
                                JsonObject objectGroup = idGroupsObjects.getJsonObject(i);
                                idGroupsResult.add(objectGroup.getString("id_groupe"));
                            }
                        }
                    }
                    handler.handle(new Either.Right<String, JsonArray>(idGroupsResult));
                }


            }
        }));

    }


    /**
     * Recupere l'identifiant de l'ensemble des eleves de la classe dont l'identifiant est passe en parametre.
     *
     * @param idClasses Identifiant de la classe dont on souhaite recuperer les eleves.
     * @param handler   Handler contenant la liste des identifiants des eleves recuperees.
     */
    public static void getElevesClasses(EventBus eb, String[] idClasses,
                                        Long idPeriode,
                                        final Handler<Either<String, Map<String, List<String>>>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "classe.getElevesClasses")
                .put("idPeriode", idPeriode)
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(idClasses)));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    Map<String, List<String>> result = new LinkedHashMap<>();
                    JsonArray queryResult = body.getJsonArray("results");
                    for (int i = 0; i < queryResult.size(); i++) {
                        JsonObject eleve = queryResult.getJsonObject(i);
                        if (!result.containsKey(eleve.getString("idClasse"))) {
                            result.put(eleve.getString("idClasse"), new ArrayList<String>());
                        }
                        result.get(eleve.getString("idClasse")).add(eleve.getString("idEleve"));
                    }
                    handler.handle(new Either.Right<String, Map<String, List<String>>>(result));
                } else {
                    handler.handle(new Either.Left<String, Map<String, List<String>>>(body.getString("message")));
                    log.error("getElevesClasses : " + body.getString("message"));
                }
            }
        }));
    }

    /**
     * Recupere les informations relatives a chaque eleve dont l'identifiant est passe en parametre, et cree un objet
     * Eleve correspondant a cet eleve.
     *
     * @param idEleves Tableau contenant les identifiants des eleves dont on souhaite recuperer les informations.
     * @param handler  Handler contenant la liste des objets Eleve ainsi construit,
     *                 ou un erreur potentiellement survenue.
     * @see Eleve
     */
    public static void getInfoEleve(EventBus eb, String[] idEleves, final Handler<Either<String, List<Eleve>>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "eleve.getInfoEleve")
                .put("idEleves", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(idEleves)));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    final Set<String> classes = new HashSet<>();
                    final List<Eleve> result = new ArrayList<>();
                    JsonArray queryResult = body.getJsonArray("results");

                    // récupération des noms des classes des élèves supprimés et stockés dans postgres
                    JsonArray idClasses = new JsonArray();
                    for (int i = 0; i < queryResult.size(); i++) {
                        if(null == queryResult.getJsonObject(i).getString("classeName") ) {
                            idClasses.add(queryResult.getJsonObject(i).getString("idClasse"));
                        }
                    }
                    if (idClasses.size() == 0) {
                        for (int i = 0; i < queryResult.size(); i++) {
                            JsonObject eleveBase = queryResult.getJsonObject(i);
                            Eleve eleveObj = new Eleve(eleveBase.getString("idEleve"),
                                    eleveBase.getString("lastName"),
                                    eleveBase.getString("firstName"),
                                    eleveBase.getString("idClasse"),
                                    eleveBase.getString("classeName"));
                            classes.add(eleveObj.getIdClasse());
                            result.add(eleveObj);
                        }
                        getCycleElevesForBfcCycle(classes,result, handler);
                    }
                    // S'il existe des élèves stockés dans postgres, on va récupérer le nom des classes dans Neo
                    else{
                        StringBuilder query = new StringBuilder();
                        JsonObject params = new JsonObject();
                        query.append("MATCH (c:Class) WHERE c.id IN {idClasses} return c.id as id, c.name as name");
                        params.put("idClasses", idClasses);

                        Neo4j.getInstance().execute(query.toString(), params,new Handler<Message<JsonObject>>() {
                            public void handle(Message<JsonObject> event) {
                                JsonObject body = event.body();

                                if (!"ok".equals(body.getString("status"))) {
                                    String message = "PB while getting classeName for BFc export";
                                    log.error(message);
                                    handler.handle(new Either.Left<>(message));
                                }
                                else {
                                    final HashMap<String, String> classeName = new HashMap<>();
                                    JsonArray classesGetFromNeo = body.getJsonArray("result");
                                    for(int i=0; i<classesGetFromNeo.size(); i++) {
                                        classeName.put(classesGetFromNeo.getJsonObject(i).getString("id"),
                                                classesGetFromNeo.getJsonObject(i).getString("name"));
                                    }
                                    for (int i = 0; i < queryResult.size(); i++) {
                                        JsonObject eleveBase = queryResult.getJsonObject(i);
                                        String eleveBaseClasseName = eleveBase.getString("classeName");
                                        if (null == eleveBaseClasseName) {
                                            eleveBaseClasseName = classeName.get(eleveBase.getString("idClasse"));
                                        }
                                        Eleve eleveObj = new Eleve(eleveBase.getString("idEleve"),
                                                eleveBase.getString("lastName"),
                                                eleveBase.getString("firstName"),
                                                eleveBase.getString("idClasse"),
                                                eleveBaseClasseName);
                                        classes.add(eleveObj.getIdClasse());
                                        result.add(eleveObj);
                                    }
                                    getCycleElevesForBfcCycle(classes,result, handler);
                                }
                            }
                        });

                    }


                } else {
                    handler.handle(new Either.Left<String, List<Eleve>>(body.getString("message")));
                    log.error("getInfoEleve : getInfoEleve : " + body.getString("message"));
                }
            }
        }));

    }

    private static void getCycleElevesForBfcCycle(final Set<String> classes, final List<Eleve> result,
                                                  final Handler<Either<String, List<Eleve>>> handler) {
        utilsService.getCycle(new ArrayList<>(classes), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray queryResult = event.right().getValue();
                    for (int i = 0; i < queryResult.size(); i++) {
                        JsonObject cycle = queryResult.getJsonObject(i);
                        for (Eleve eleve : result) {
                            if (Objects.equals(eleve.getIdClasse(), cycle.getString("id_groupe"))) {
                                eleve.setCycle(cycle.getString("libelle"));
                            }
                        }
                    }
                    handler.handle(new Either.Right<String, List<Eleve>>(result));
                } else {
                    handler.handle(new Either.Left<String, List<Eleve>>(event.left().getValue()));
                    log.error("getInfoEleve : getCycle : " + event.left().getValue());
                }
            }
        });

    }
    /**
     * Recupere l'identifiant de l'ensemble des classes de la structure dont l'identifiant est passe en parametre.
     *
     * @param idStructure Identifiant de la structure dont on souhaite recuperer les classes.
     * @param handler     Handler contenant la liste des identifiants des classes recuperees.
     */
    public static void getClassesStruct(EventBus eb, final String idStructure, final Handler<Either<String, List<String>>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "classe.getClasseEtablissement")
                .put("idEtablissement", idStructure);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    List<String> result = new ArrayList<>();
                    JsonArray queryResult = body.getJsonArray("results");
                    for (int i = 0; i < queryResult.size(); i++) {
                        JsonObject classe = queryResult.getJsonObject(i);
                        result.add(classe.getString("idClasse"));
                    }
                    handler.handle(new Either.Right<String, List<String>>(result));
                } else {
                    handler.handle(new Either.Left<String, List<String>>(body.getString("message")));
                    log.error("getClassesStruct : " + body.getString("message"));
                }
            }
        }));
    }

    /**
     *
     * @param eb
     * @param idStructure Identifiant de la structure dont on souhaite recuperer les périodes
     * @param idClasses Identifiants des classes dont on souhaite recuperer les periodes
     * @param handler Handler contenant pour chaque classe
     *                la date de creation du BFC = date de début la première periode de l'annee scolaire en cours
     *                la date verrou du BFC = date de la fin de la dernière periode de l'année scolaire en cours
     *                ces dates sont necessaires pour export XML du LSU
     */
    public static void getDatesCreationVerrouByClasses(EventBus eb, final String idStructure, final List<String> idClasses, final Handler<Either<String,Map<String,JsonObject>>> handler){
        JsonObject action = new JsonObject()
                .put("action","periode.getDatesDtFnAnneeByClasse")
                .put("idEtablissement", idStructure)
                .put("idClasses",new fr.wseduc.webutils.collections.JsonArray(idClasses));
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                Map<String,JsonObject> datesCreationVerrouByClasse = new HashMap<String,JsonObject>();
                if("ok".equals(body.getString("status"))) {
                    JsonArray requestDateByClasse = body.getJsonArray("results");
                    if(requestDateByClasse.size() > 0 && requestDateByClasse != null){
                        for(int i = 0; i < requestDateByClasse.size(); i++){
                            JsonObject jsonObjectRep = requestDateByClasse.getJsonObject(i);
                            if(jsonObjectRep.containsKey("id_classe")&&
                                    jsonObjectRep.containsKey("date_debut") && jsonObjectRep.containsKey("date_fin")) {
                                String idClasse = jsonObjectRep.getString("id_classe");
                                JsonObject dates = new JsonObject();
                                dates.put("date_creation", jsonObjectRep.getString("date_debut"));
                                dates.put("date_verrou", jsonObjectRep.getString("date_fin"));
                                datesCreationVerrouByClasse.put(idClasse, dates);
                            }else{
                                handler.handle(new Either.Left<String,Map<String,JsonObject>>("Cette classe n'a pas de dates de debut et/ou de fin de periode : "+ jsonObjectRep.getString("id_classe")));
                                log.error("getDatesCreationVerrouByClasses : cette classe n'a pas de dates de debut et/ou de fin de periode : "+jsonObjectRep.getString("id_classe"));
                            }
                        }
                    }else{
                        handler.handle(new Either.Left<>("Les classes de cet établissement n'ont pas de dates de début et de fin de periode : "+idStructure));
                        log.error("getDatesCreationVerrouByClasses : " +
                                "Les classes de cet établissement n'ont pas de dates de début et de fin de periode : "+idStructure);
                    }
                    handler.handle(new Either.Right<String,Map<String,JsonObject>>(datesCreationVerrouByClasse));
                } else {
                    handler.handle(new Either.Left<String, Map<String,JsonObject>>(body.getString("message")));
                    log.error("getDatesCreationVerrouByClasses : " + body.getString("message"));
                }
            }
        }) );

    }
    /* public static void getLibelleMatiere(EventBus eb, final JsonArray idsMatieres, final Handler<Either<String,Map<String,String>>> handler){
         JsonObject action = new JsonObject()
                 .put("action","matiere.getMatieres")
                 .put("idMatieres", idsMatieres);
         eb.send(Competences.VIESCO_BUS_ADDRESS,action,Competences.DELIVERY_OPTIONS,
                 handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
             @Override
             public void handle(Message<JsonObject> message) {
                 JsonObject body = message.body();
                 Map<String,String> idsMatLibelle = new HashMap<>();

                 if("ok".equals(body.getString("status"))) {

                     JsonArray requestMats = body.getJsonArray("results");
                     if( requestMats != null && requestMats.size() > 0 ){
                         for( int i = 0; i < requestMats.size(); i++){
                             JsonObject requestMat = requestMats.getJsonObject(i);

                             if(!idsMatLibelle.containsKey(requestMat.getString("id"))){
                                idsMatLibelle.put(requestMat.getString("id"),requestMat.getString("name"));
                             }
                         }
                     }else {
                         handler.handle(new Either.Left<>(" no subject "));
                         log.error("getMatieres : no subject");
                     }

                 handler.handle(new Either.Right<String,Map<String,String>>(idsMatLibelle));
                 } else {
                     handler.handle(new Either.Left<String, Map<String,String>>(body.getString("message")));
                     log.error("getMatieres : " + body.getString("message"));
                 }
             }
         }));


     }*/
    public static void getLibelleMatiere(EventBus eb, final JsonArray idsMatieres, final Handler<Either<String,Map<String,JsonObject>>> handler){
        JsonObject action = new JsonObject()
                .put("action","matiere.getMatieres")
                .put("idMatieres", idsMatieres);
        eb.send(Competences.VIESCO_BUS_ADDRESS,action,handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                Map<String,JsonObject> idsMatLibelle = new HashMap<>();

                if("ok".equals(body.getString("status"))) {

                    JsonArray requestMats = body.getJsonArray("results");
                    if( requestMats != null && requestMats.size() > 0 ){
                        for( int i = 0; i < requestMats.size(); i++){
                            JsonObject requestMat = requestMats.getJsonObject(i);

                            if(!idsMatLibelle.containsKey(requestMat.getString("id"))){
                                idsMatLibelle.put(requestMat.getString("id"), requestMat);
                            }
                        }
                        handler.handle(new Either.Right<>(idsMatLibelle));

                    }else {
                        handler.handle(new Either.Left<>(" no subject "));
                        log.error("getMatieres : no subject");
                    }
                } else {
                    handler.handle(new Either.Left<>(body.getString("message")));
                    log.error("getMatieres : " + body.getString("message"));
                }
            }
        }));


    }


    public static void getLastNameFirstNameUser(EventBus eb, final JsonArray idsUsers,
                                                final Handler<Either<String,Map<String,JsonObject>>> handler){
        JsonObject action = new JsonObject()
                .put("action","eleve.getUsers")
                .put("idUsers", idsUsers);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action,Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {

                        JsonObject body = message.body();
                        Map<String, JsonObject> idsUserNamePrenom = new HashMap<>();

                        if ("ok".equals(body.getString("status"))) {
                            JsonArray requestUsers = body.getJsonArray("results");
                            if(requestUsers != null && requestUsers.size() > 0 ){

                                for(int i = 0; i < requestUsers.size(); i++){
                                    JsonObject requestUser = requestUsers.getJsonObject(i);
                                    if(!idsUserNamePrenom.containsKey(requestUser.getString("id"))){
                                        idsUserNamePrenom.put(requestUser.getString("id"),new JsonObject()
                                                .put("firstName",requestUser.getString("firstName")).put("name",requestUser.getString("name")));
                                    }

                                }
                            } else {
                                handler.handle(new Either.Left<>("no User "));
                                log.error("getUsers : no User");
                            }
                            handler.handle(new Either.Right<String,Map<String,JsonObject>>(idsUserNamePrenom));
                        }else {
                            handler.handle(new Either.Left<String, Map<String,JsonObject>>(body.getString("message")));
                            log.error("getUsers : " + body.getString("message"));
                        }

                    }
                }));
    }

    /**
     * get infoEleve of one class order by lastName
     * @param eb eventBus
     * @param idClass idClass
     * @param handler elveList with idEleve,lastName, firstName, level, classes, className
     */
    public static void getEleveClasse(EventBus eb, List<String> idsEleve, final String idClass, final Handler<Either<String, List<Eleve>>> handler){
        JsonObject action = new JsonObject()
                .put("action","eleve.getEleveClasse")
                .put("idClass", idClass);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                List<Eleve> eleves = new ArrayList<Eleve>();
                if(!"ok".equals(body.getString("status"))){
                    handler.handle(new Either.Left<>(body.getString("message")));
                    log.error("getEleveClasse :  " + body.getString("message") );
                }else{
                    JsonArray requesteleves = body.getJsonArray("results");
                    if( requesteleves != null && requesteleves.size() > 0){
                        for(int i=0 ; i < requesteleves.size(); i++ ){
                            JsonObject requestEleve = requesteleves.getJsonObject(i);
                            Eleve eleve = new Eleve(requestEleve.getString("id"),requestEleve.getString("lastName"),
                                    requestEleve.getString("firstName"),idClass,requestEleve.getString("className"));
                            eleves.add(eleve);
                            if(idsEleve != null){
                                idsEleve.add(requestEleve.getString("id"));
                            }
                        }
                    }else{
                        handler.handle(new Either.Left<>("no Student in this Class " + idClass));
                        log.error("getEleveClasse : no Student in this Class idClass: " + idClass );
                    }
                    handler.handle(new Either.Right<>(eleves));
                }
            }
        }));
    }

    public static void getLibellePeriode(EventBus eb, HttpServerRequest request,Integer idPeriode, Handler<Either<String,String>> handler){


        JsonObject jsonRequest = new JsonObject()
                .put("headers", new JsonObject()
                        .put("Accept-Language", request.headers().get("Accept-Language")))
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
                    handler.handle(new Either.Left<>("periode not found " + idPeriode));
                    log.error("getLibellePeriode : periode not found: " + idPeriode);
                } else {
                    handler.handle(new Either.Right<>(body.getString("result")));
                }
            }
        }));
    }
}
