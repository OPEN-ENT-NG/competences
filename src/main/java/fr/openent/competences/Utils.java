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
import fr.openent.competences.constants.Field;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.service.MatiereService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultMatiereService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.openent.competences.utils.UtilsConvert;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.TIME;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.Renders.getHost;

public class Utils {

    protected static final Logger log = LoggerFactory.getLogger(Utils.class);
    protected static UtilsService utilsService = new DefaultUtilsService();
    protected static MatiereService matiereService = new DefaultMatiereService();
    private static String domain;
    private static String locale;
    private EventBus eb;
    private String idGroup;
    private Integer idPeriod;
    private int typeClass;

    /**
     * Recupere l'identifiant de la structure a laquelle appartiennent les classes dont l'identifiant est passe en
     * parametre.
     *
     * @param idClasses Tableau contenant l'identifiant des classes dont on souhaite connaitre la structure.
     * @param handler   Handler contenant l'identifiant de la structure.
     */
    public static void getStructClasses(EventBus eb, String[] idClasses,
                                        final Handler<Either<String, String>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getEtabClasses")
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(idClasses)));

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if (OK.equals(body.getString(STATUS))) {
                JsonArray queryResult = body.getJsonArray(RESULTS);
                if (queryResult.size() == 0) {
                    handler.handle(new Either.Left<>("Aucune classe n'a ete trouvee."));
                    log.error("getStructClasses : No classes found with these ids");
                } else if (queryResult.size() > 1) {
                    // Il est impossible de demander un BFC pour des classes n'appartenant pas au meme etablissement.
                    handler.handle(new Either.Left<>("Les classes n'appartiennent pas au meme etablissement."));
                    log.error("getStructClasses : provided classes are not from the same structure.");
                } else {
                    JsonObject structure = queryResult.getJsonObject(0);
                    String idStrucutre = structure.getString("idStructure");
                    handler.handle(new Either.Right<>(idStrucutre));
                }
            } else {
                String error = body.getString(MESSAGE);
                log.error("getStructClasses : " + error);
                if (error.contains(TIME)) {
                    getStructClasses(eb, idClasses, handler);
                    return;
                }
                handler.handle(new Either.Left<>(error));
            }
        }));
    }


    /**
     * @Deprecated Use @link{#getGroupesClasse(EventBus eb, final JsonArray classesIds)}
     * retourne une classe avec ses groups (ids)
     *
     * @param eb         eventbus
     * @param classesIds array une clase
     * @param handler    response l'id de classe avec ses groups s'ils existent sinon retourne que id de la classe
     */
    @Deprecated
    public static void getGroupesClasse(EventBus eb, final JsonArray classesIds,
                                        final Handler<Either<String, JsonArray>> handler) {

        getGroupesClasse(eb,classesIds)
                .onFailure( failGpsClass ->
                {
                    log.error(String.format("[Competences@%s::getEvaluableGroupsClass] error neo resquest %s",
                            Utils.class.getSimpleName(),
                            failGpsClass.getMessage()));
                    handler.handle(new Either.Left<>(failGpsClass.getMessage()));
                })
                .onSuccess( responseGpsClass -> {
                    handler.handle(new Either.Right<>(responseGpsClass));
                });
    }

    /**
     * retourne une classe avec ses groups (ids)
     *
     * @param eb         eventbus
     * @param classesIds array une clase
     * return future    response l'id de classe avec ses groups s'ils existent sinon retourne que id de la classe
     */
    public static Future<JsonArray> getGroupesClasse(EventBus eb, final JsonArray classesIds) {
        Promise<JsonArray> gpsClassPromise = Promise.promise();
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getEvaluableGroupsClasses")
                .put("id_classes", classesIds);

        eb.request(VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(
                FutureHelper.handlerToAsyncHandler(gpsClassPromise,
                        String.format("[Competences@%s::getEvaluableGroupsClass] error neo resquest",
                                Utils.class.getSimpleName()))));
        return gpsClassPromise.future();
    }

    /**
     * retourne les groupes de l'élève
     *
     * @param eb         eventbus
     * @param idEleve string id de l'élève
     * @param handler    response les groupes de l'élève
     */
    public static void getGroupesEleve(EventBus eb, final String idEleve, final Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getGroups")
                .put("idEleve", idEleve);
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        if (OK.equals(body.getString(STATUS))) {
                            JsonArray queryResult = body.getJsonArray(RESULTS);
                            handler.handle(new Either.Right<String, JsonArray>(queryResult));
                        } else {
                            handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                            log.error("getGroupesEleve : " + body.getString("message"));
                        }
                    }
                }));
    }


    public static void getInfosGroupes(EventBus eb, final JsonArray idsClasses, final Handler<Either<String, Map<String, String>>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getClassesInfo")
                .put("idClasses", idsClasses);
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if (OK.equals(body.getString(STATUS))) {
                JsonArray queryResult = body.getJsonArray(RESULTS);

                if (queryResult != null) {
                    Map<String, String> result = queryResult.stream()
                            .collect(Collectors.toMap(val -> ((JsonObject) val).getString("id"), val -> ((JsonObject) val).getString("name")));

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

    /**
     *
     * @param eb event bus
     * @param idGroup id Group
     * @param idPeriod id period
     * @param typeClass type classe
     * @return future List<String> idsStudent
     */

    public static Future<JsonArray> getElevesClassesGroupes(EventBus eb, final String idGroup, final Integer idPeriod,
                                                 final int typeClass) {
        Promise<JsonArray> studentPromise = Promise.promise();

        JsonObject action = new JsonObject();
        if (typeClass == 0) {
            action.put(ACTION, "classe.getEleveClasse").put(ID_CLASSE_KEY, idGroup);

            if (idPeriod != null)
                action.put("idPeriode", idPeriod);
        } else if (typeClass == 1 || typeClass == 2) {
            action.put(ACTION, "groupe.listUsersByGroupeEnseignementId")
                    .put("groupEnseignementId", idGroup).put("profile", "Student");

            if (idPeriod != null)
                action.put("idPeriode", idPeriod);
        }

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(
                FutureHelper.handlerToAsyncHandler( studentPromise,
                        "Error :can not get students of groupe : " + idGroup )));
        return studentPromise.future();

    }

    public static Future<List<String>> getIdElevesClassesGroupes(EventBus eb, final String idGroup, final Integer idPeriod,
                                                          final int typeClass){
        Promise<List<String>> idsStudentPromise = Promise.promise();

        getElevesClassesGroupes(eb, idGroup, idPeriod, typeClass)
                .onSuccess(studentsClass -> {
                            List<String> idEleves = new ArrayList<>();
                            for (int i = 0; i < studentsClass.size(); i++) {
                                idEleves.add((studentsClass.getJsonObject(i)).getString("id"));
                            }
                            idsStudentPromise.complete(idEleves);
                })
                .onFailure(idsStudentPromise::fail);

    return idsStudentPromise.future();
    }

    /**
     * @Deprecated Use @link{#getIdElevesClassesGroupes(EventBus eb, final String idGroup, final Integer idPeriod,
     *                                                  final int typeClass)}
     * @param eb event bus
     * @param idGroup id group
     * @param idPeriod id period
     * @param typeClass type class
     * @param handler response List<String> idsStudent
     */
    @Deprecated
    public static void getIdElevesClassesGroupes(EventBus eb, final String idGroup, final Integer idPeriod,
                                                 final int typeClass,
                                                 final Handler<Either<String, List<String>>> handler) {

        Future<List<String>> idElevesClassesGroupes = getIdElevesClassesGroupes(eb, idGroup, idPeriod, typeClass);
        idElevesClassesGroupes.onSuccess(idsStudent -> {
            handler.handle(new Either.Right<>(idsStudent));
        });
        idElevesClassesGroupes.onFailure(failIdsStudent -> {
            handler.handle(new Either.Left<>(failIdsStudent.getMessage()));
            log.error("Error :can not get students of groupe : " + idGroup + " error : " + failIdsStudent.getMessage());
        });

    }


    public static Future<JsonArray> getGroupsEleve(EventBus eb, String studentId, String structureId) {
        Promise<JsonArray> promise = Promise.promise();
        getGroupsEleve(eb, studentId, structureId, FutureHelper.handler(promise,
               String.format("[Competences@%s::getGroupsEleve] error during get sql request", Utils.class.getSimpleName())));
        return promise.future();
    }
    /**
     * @deprecated Use @link{#getGroupsEleve(EventBus eb, String studentId, String structureId)}
     * Get eleve group
     *
     * @param eb      EventBus
     * @param idEleve id Eleve
     * @param handler response eleve groups
     */
    @Deprecated
    public static void getGroupsEleve(EventBus eb, String idEleve, String idEtablissement,
                                      Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getInfoEleve")
                .put(Field.IDETABLISSEMENT, idEtablissement)
                .put(Field.IDELEVES, new fr.wseduc.webutils.collections.JsonArray().add(idEleve));

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        if (!OK.equals(body.getString(STATUS))) {
                            handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                            log.error("getGroupsEleve : " + body.getString("message"));
                        } else {
                            JsonArray result = body.getJsonArray(RESULTS);
                            JsonArray idGroupsResult = new JsonArray();

                            if (OK.equals(body.getString(STATUS))
                                    && result.size() > 0) {
                                for (int i = 0; i < result.size(); i++) {
                                    JsonObject eleve = body.getJsonArray(RESULTS)
                                            .getJsonObject(i);

                                    final String idClasse =
                                            eleve.getString(ID_CLASSE_KEY);


                                    JsonObject o = result.getJsonObject(i);

                                    JsonArray idManualGroupes = UtilsConvert
                                            .strIdGroupesToJsonArray(o.getValue("idManualGroupes"));
                                    JsonArray idFunctionalGroupes = UtilsConvert
                                            .strIdGroupesToJsonArray(o.getValue("idGroupes"));

                                    idGroupsResult = utilsService.saUnion(idFunctionalGroupes,
                                            idManualGroupes);
                                    idGroupsResult = utilsService.saUnion(idGroupsResult,
                                            new JsonArray().add(idClasse));

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
    public static void getElevesClasses(EventBus eb, String[] idClasses, Long idPeriode,
                                        final Handler<Either<String, Map<String, List<String>>>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getElevesClasses")
                .put(ID_PERIODE_KEY, idPeriode)
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(idClasses)));

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS,
                handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();

                    if (OK.equals(body.getString(STATUS))) {
                        Map<String, List<String>> result = new LinkedHashMap<>();
                        JsonArray queryResult = body.getJsonArray(RESULTS);
                        for (int i = 0; i < queryResult.size(); i++) {
                            JsonObject eleve = queryResult.getJsonObject(i);
                            if (!result.containsKey(eleve.getString(ID_CLASSE_KEY))) {
                                result.put(eleve.getString(ID_CLASSE_KEY), new ArrayList<>());
                            }
                            result.get(eleve.getString(ID_CLASSE_KEY)).add(eleve.getString(ID_ELEVE_KEY));
                        }
                        handler.handle(new Either.Right<>(result));
                    } else {
                        String error = body.getString(MESSAGE);
                        log.error("getElevesClasses : " + error);
                        if (error.contains(TIME)) {
                            getElevesClasses(eb, idClasses, idPeriode, handler);
                            return;
                        }
                        handler.handle(new Either.Left<>(error));
                    }
                }));
    }

    // Méthode réalisé pour que les élèves ayant changé d'établissement est bien le nom de leur classe correspondant
    // au nom de l'export.
    public static void getClassesEleves(EventBus eb, String[] idsClasse, Long idPeriode,
                                        final Handler<Either<String, Map<String, JsonArray>>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getElevesClasses")
                .put(ID_PERIODE_KEY, idPeriode)
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(idsClasse)));

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS,
                handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();

                    if (OK.equals(body.getString(STATUS))) {
                        JsonArray queryResult = body.getJsonArray(RESULTS);
                        Map<String, JsonArray> result = new LinkedHashMap<>();
                        for (int i = 0; i < queryResult.size(); i++) {
                            JsonObject eleve = queryResult.getJsonObject(i);
                            if (!result.containsKey(eleve.getString(ID_CLASSE_KEY))) {
                                result.put(eleve.getString(ID_CLASSE_KEY), new JsonArray());
                            }
                            result.get(eleve.getString(ID_CLASSE_KEY)).add(eleve);
                        }
                        handler.handle(new Either.Right<>(result));
                    } else {
                        String error = body.getString(MESSAGE);
                        log.error("getElevesClasses : " + error);
                        if (error.contains(TIME)) {
                            getClassesEleves(eb, idsClasse, idPeriode, handler);
                            return;
                        }
                        handler.handle(new Either.Left<>(error));
                    }
                }));
    }

    public static void getElevesClasse(EventBus eb, String idClasse, Long idPeriode,
                                       final Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getElevesClasses")
                .put(ID_PERIODE_KEY, idPeriode)
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray().add(idClasse));

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if (OK.equals(body.getString(STATUS))) {
                JsonArray queryResult = body.getJsonArray(RESULTS);
                handler.handle(new Either.Right<>(queryResult));
            } else {
                handler.handle(new Either.Left<>(body.getString(MESSAGE)));
                log.error("getElevesClasses : " + body.getString(MESSAGE));
            }
        }));
    }



    public static void getElevesClasse(EventBus eb, String idClasse, Long idPeriode,
                                       final Promise<Object> promise) {
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getElevesClasses")
                .put(ID_PERIODE_KEY, idPeriode)
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray().add(idClasse));

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if (OK.equals(body.getString(STATUS))) {
                JsonArray queryResult = body.getJsonArray(RESULTS);
               promise.complete(queryResult);
            } else {
              promise.fail(body.getString(MESSAGE));

            }
        }));
    }

    public static List<Eleve> toListEleve(JsonArray queryResult) {
        final List<Eleve> result = new ArrayList<>();
        for (int i = 0; i < queryResult.size(); i++) {
            JsonObject eleveBase = queryResult.getJsonObject(i);
            Eleve eleveObj = new Eleve(eleveBase.getString(ID_ELEVE_KEY),
                    eleveBase.getString(LAST_NAME_KEY),
                    eleveBase.getString(FIRST_NAME_KEY),
                    eleveBase.getString(ID_CLASSE_KEY),
                    eleveBase.getString(NAME),
                    eleveBase.getString(LEVEL),
                    eleveBase.getString("birthDate"));
            result.add(eleveObj);
        }
        return result;
    }

    /**
     * Recupere les informations relatives a chaque eleve dont l'identifiant est passe en parametre, et cree un objet
     * Eleve correspondant a cet eleve.
     *
     * @param idEleves Tableau contenant les identifiants des eleves dont on souhaite recuperer les informations.
     * @param idCycle
     * @param handler  Handler contenant la liste des objets Eleve ainsi construit,
     *                 ou un erreur potentiellement survenue.
     * @see Eleve
     */
    public static void getInfoEleve(EventBus eb, String[] idEleves, Long idCycle, String idEtablissment,
                                    final Handler<Either<String, List<Eleve>>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getInfoEleve")
                .put(Competences.ID_ETABLISSEMENT_KEY, idEtablissment)
                .put("idEleves", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(idEleves)));

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(response -> {
            JsonObject bodyResponse = response.body();

            if (OK.equals(bodyResponse.getString(STATUS))) {
                final Set<String> classes = new HashSet<>();
                final List<Eleve> result = new ArrayList<>();
                JsonArray queryResult = bodyResponse.getJsonArray(RESULTS);

                // récupération des noms des classes des élèves supprimés et stockés dans postgres
                JsonArray idClasses = new JsonArray();
                for (int i = 0; i < queryResult.size(); i++) {
                    if (null == queryResult.getJsonObject(i).getString("classeName")) {
                        idClasses.add(queryResult.getJsonObject(i).getString(ID_CLASSE_KEY));
                    }
                }

                if (idClasses.size() == 0) {
                    for (int i = 0; i < queryResult.size(); i++) {
                        JsonObject eleveBase = queryResult.getJsonObject(i);
                        Eleve eleveObj = new Eleve(eleveBase.getString(ID_ELEVE_KEY),
                                eleveBase.getString("lastName"), eleveBase.getString("firstName"),
                                eleveBase.getString(ID_CLASSE_KEY), eleveBase.getString("classeName"),
                                eleveBase.getString(LEVEL), eleveBase.getString("birthDate"));

                        classes.add(eleveObj.getIdClasse());
                        result.add(eleveObj);
                    }
                    getCycleElevesForBfcCycle(classes, idCycle, result, handler);
                } else {
                    // S'il existe des élèves stockés dans postgres, on va récupérer le nom des classes dans Neo
                    StringBuilder query = new StringBuilder();
                    JsonObject params = new JsonObject();
                    query.append("MATCH (c:Class) WHERE c.id IN {idClasses} return c.id as id, c.name as name");
                    params.put("idClasses", idClasses);

                    Neo4j.getInstance().execute(query.toString(), params, event -> {
                        JsonObject body = event.body();

                        if (!OK.equals(body.getString(STATUS))) {
                            String message = "PB while getting classeName for BFc export";
                            log.error(message);
                            handler.handle(new Either.Left<>(message));
                        } else {
                            final HashMap<String, String> classeName = new HashMap<>();
                            JsonArray classesGetFromNeo = body.getJsonArray("result");
                            for (int i = 0; i < classesGetFromNeo.size(); i++) {
                                classeName.put(classesGetFromNeo.getJsonObject(i).getString("id"),
                                        classesGetFromNeo.getJsonObject(i).getString("name"));
                            }

                            for (int i = 0; i < queryResult.size(); i++) {
                                JsonObject eleveBase = queryResult.getJsonObject(i);
                                String eleveBaseClasseName = eleveBase.getString("classeName");
                                if (null == eleveBaseClasseName) {
                                    eleveBaseClasseName = classeName.get(eleveBase.getString(ID_CLASSE_KEY));
                                }

                                Eleve eleveObj = new Eleve(eleveBase.getString(ID_ELEVE_KEY),
                                        eleveBase.getString("lastName"), eleveBase.getString("firstName"),
                                        eleveBase.getString(ID_CLASSE_KEY), eleveBaseClasseName,
                                        eleveBase.getString("level"), eleveBase.getString("birthDate"));
                                classes.add(eleveObj.getIdClasse());
                                result.add(eleveObj);
                            }
                            getCycleElevesForBfcCycle(classes, idCycle, result, handler);
                        }
                    });
                }
            } else {
                handler.handle(new Either.Left<>(bodyResponse.getString(MESSAGE)));
                log.error("getInfoEleve : getInfoEleve : " + bodyResponse.getString(MESSAGE));
            }
        }));

    }

    public static void getCycleElevesForBfcCycle(final Set<String> classes, Long idCycle, final List<Eleve> result,
                                                 final Handler<Either<String, List<Eleve>>> handler) {
        if(idCycle != null){
            getCycleInfosFromIdCycle(idCycle, event -> {
                if (event.isRight()) {
                    JsonObject cycle = event.right().getValue();
                    for (Eleve eleve : result) {
                        eleve.setCycle(cycle.getString(LIBELLE));
                    }
                    handler.handle(new Either.Right<>(result));
                } else {
                    String error = event.left().getValue();
                    log.error("getInfoEleve : getCycle : " + event);
                    if (error.contains(TIME)) {
                        getCycleElevesForBfcCycle(classes, idCycle, result, handler);
                        return;
                    }
                    handler.handle(new Either.Left<>(event.left().getValue()));
                }
            });
        } else {
            utilsService.getCycle(new ArrayList<>(classes), event -> {
                if (event.isRight()) {
                    JsonArray queryResult = event.right().getValue();
                    for (int i = 0; i < queryResult.size(); i++) {
                        JsonObject cycle = queryResult.getJsonObject(i);
                        for (Eleve eleve : result) {
                            if (Objects.equals(eleve.getIdClasse(), cycle.getString("id_groupe"))) {
                                eleve.setCycle(cycle.getString(LIBELLE));
                            }
                        }
                    }
                    handler.handle(new Either.Right<>(result));
                } else {
                    String error = event.left().getValue();
                    log.error("getInfoEleve : getCycle : " + event);
                    if (error.contains(TIME)) {
                        getCycleElevesForBfcCycle(classes, idCycle, result, handler);
                        return;
                    }
                    handler.handle(new Either.Left<>(event.left().getValue()));
                }
            });
        }
    }

    public static void getCycleInfosFromIdCycle(Long idCycle, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder()
                .append("SELECT * ")
                .append("FROM ").append(Competences.COMPETENCES_SCHEMA).append(".cycle ")
                .append("WHERE id = ?;");

        params.add(idCycle);
        Sql.getInstance().prepared(query.toString(), params, DELIVERY_OPTIONS,
                SqlResult.validUniqueResultHandler(handler));
    }

    /**
     * Recupere l'identifiant de l'ensemble des classes de la structure dont l'identifiant est passe en parametre.
     *
     * @param idStructure Identifiant de la structure dont on souhaite recuperer les classes.
     * @param handler     Handler contenant la liste des identifiants des classes recuperees.
     */
    public static void getClassesStruct(EventBus eb, final String idStructure,
                                        final Handler<Either<String, List<String>>> handler) {

        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getClasseEtablissement")
                .put(ID_ETABLISSEMENT_KEY, idStructure);

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS,
                handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();
                    if (OK.equals(body.getString(STATUS))) {
                        List<String> result = new ArrayList<>();
                        JsonArray queryResult = body.getJsonArray(RESULTS);
                        for (int i = 0; i < queryResult.size(); i++) {
                            JsonObject classe = queryResult.getJsonObject(i);
                            result.add(classe.getString(ID_CLASSE_KEY));
                        }
                        handler.handle(new Either.Right<>(result));
                    } else {
                        handler.handle(new Either.Left<>(body.getString(MESSAGE)));
                        log.error("getClassesStruct : " + body.getString(MESSAGE));
                    }
                }));
    }

    /**
     * @param eb
     * @param idStructure Identifiant de la structure dont on souhaite recuperer les périodes
     * @param idClasses   Identifiants des classes dont on souhaite recuperer les periodes
     * @param handler     Handler contenant pour chaque classe
     *                    la date de creation du BFC = date de début la première periode de l'annee scolaire en cours
     *                    la date verrou du BFC = date de la fin de la dernière periode de l'année scolaire en cours
     *                    ces dates sont necessaires pour export XML du LSU
     */
    public static void getDatesCreationVerrouByClasses(EventBus eb, final String idStructure, final List<String> idClasses, final Handler<Either<String, Map<String, JsonObject>>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "periode.getDatesDtFnAnneeByClasse")
                .put("idEtablissement", idStructure)
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(idClasses));
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                Map<String, JsonObject> datesCreationVerrouByClasse = new HashMap<String, JsonObject>();
                if (OK.equals(body.getString(STATUS))) {
                    JsonArray requestDateByClasse = body.getJsonArray(RESULTS);
                    if (requestDateByClasse != null && requestDateByClasse.size() > 0) {
                        for (int i = 0; i < requestDateByClasse.size(); i++) {
                            JsonObject jsonObjectRep = requestDateByClasse.getJsonObject(i);
                            if (jsonObjectRep.containsKey("id_classe") &&
                                    jsonObjectRep.containsKey("date_debut") && jsonObjectRep.containsKey("date_fin")) {
                                String idClasse = jsonObjectRep.getString("id_classe");
                                JsonObject dates = new JsonObject();
                                dates.put("date_creation", jsonObjectRep.getString("date_debut"));
                                dates.put("date_verrou", jsonObjectRep.getString("date_fin"));
                                datesCreationVerrouByClasse.put(idClasse, dates);
                            } else {
                                handler.handle(new Either.Left<String, Map<String, JsonObject>>("Cette classe n'a pas de dates de debut et/ou de fin de periode : " + jsonObjectRep.getString("id_classe")));
                                log.error("getDatesCreationVerrouByClasses : cette classe n'a pas de dates de debut et/ou de fin de periode : " + jsonObjectRep.getString("id_classe"));
                            }
                        }
                    } else {
                        handler.handle(new Either.Left<>("Les classes de cet établissement n'ont pas de dates de début et de fin de periode : " + idStructure));
                        log.error("getDatesCreationVerrouByClasses : " +
                                "Les classes de cet établissement n'ont pas de dates de début et de fin de periode : " + idStructure);
                    }
                    handler.handle(new Either.Right<String, Map<String, JsonObject>>(datesCreationVerrouByClasse));
                } else {
                    handler.handle(new Either.Left<String, Map<String, JsonObject>>(body.getString("message")));
                    log.error("getDatesCreationVerrouByClasses : " + body.getString("message"));
                }
            }
        }));

    }

    public static void getLibelleMatiere(EventBus eb, final JsonArray idsMatieres,
                                         final Handler<Either<String, Map<String, JsonObject>>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "matiere.getMatieres")
                .put("idMatieres", idsMatieres);
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                Map<String, JsonObject> idsMatLibelle = new HashMap<>();

                if (OK.equals(body.getString(STATUS))) {
                    JsonArray requestMats = body.getJsonArray(RESULTS);

                    if (requestMats != null && requestMats.size() > 0) {
                        matiereService.getLibellesCourtsMatieres(true, new Handler<Either<String, Map<String, String>>>() {
                            @Override
                            public void handle(Either<String, Map<String, String>> event) {
                                Map mapCodeLibelleCourt = event.right().getValue();
                                buildMapSubject(requestMats, idsMatLibelle, mapCodeLibelleCourt);
                                handler.handle(new Either.Right<>(idsMatLibelle));
                            }
                        });
                    } else {
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

    public static void buildMapSubject(JsonArray subjects, Map<String, JsonObject> mapSubjects, Map mapCodeLibelleCourt) {
        for (int i = 0; i < subjects.size(); i++) {
            JsonObject subject = subjects.getJsonObject(i);

            if (!mapSubjects.containsKey(subject.getString("id"))) {

                // String source = requestMat.getJsonObject("data").getJsonObject("data").getString("source");
                String codeMatiere;
                if (isNotNull(subject.getJsonObject("data"))) {
                    codeMatiere = subject.getJsonObject("data").getJsonObject("data").getString("code");
                } else {
                    codeMatiere = subject.getString(EXTERNAL_ID_KEY);
                }

                try {
                    Integer.valueOf(codeMatiere);
                    if (!mapCodeLibelleCourt.isEmpty() && mapCodeLibelleCourt.containsKey(codeMatiere)) {
                        subject.put("libelle_court", mapCodeLibelleCourt.get(codeMatiere));

                    } else {//si le codeMatiere n'est pas dans la table matiere prendre
                        // les 5 premiers caracteres du libelle de la matiere
                        String nameSubject = subject.getString("name").trim();
                        if(nameSubject.length() < 5){
                            subject.put("libelle_court", nameSubject);
                        }else{
                            subject.put("libelle_court", nameSubject.substring(0, 4));
                        }
                    }
                } catch (NumberFormatException e) {
                    subject.put("libelle_court", codeMatiere);
                }

            }
            mapSubjects.put(subject.getString("id"), subject);
        }
    }

    public static Future< Map<String, JsonObject>> getLastNameFirstNameUser(EventBus eb, final JsonArray usersIds) {
        Promise< Map<String, JsonObject>> promise = Promise.promise();
        getLastNameFirstNameUser(eb, usersIds, FutureHelper.handler(promise,
                String.format("[Competences@%s :: getLastNameFirstNameUser] error to get sql request by eventBus viesco",
                        Utils.class.getSimpleName())));
        return promise.future();
    }
    @Deprecated
    public static void getLastNameFirstNameUser(EventBus eb, final JsonArray idsUsers,
                                                final Handler<Either<String, Map<String, JsonObject>>> handler) {
        List<String> idsTeacherNotInNeo = idsUsers.stream().map(Object::toString).collect(Collectors.toList());
        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getUsers")
                .put("idUsers", idsUsers);
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(message -> {

                    JsonObject body = message.body();
                    Map<String, JsonObject> idsUserNamePrenom = new HashMap<>();

                    if (OK.equals(body.getString(STATUS))) {
                        JsonArray requestUsers = body.getJsonArray(RESULTS);
                        if (requestUsers != null && requestUsers.size() > 0) {
                            for (int i = 0; i < requestUsers.size(); i++) {
                                JsonObject requestUser = requestUsers.getJsonObject(i);
                                if (!idsUserNamePrenom.containsKey(requestUser.getString("id"))) {
                                    idsUserNamePrenom.put(requestUser.getString("id"), new JsonObject()
                                            .put("firstName", requestUser.getString("firstName"))
                                            .put("name", requestUser.getString("name"))
                                            .put("id", requestUser.getString("id"))
                                            .put("birthDate", requestUser.getString("birthDate")));

                                    idsTeacherNotInNeo.remove(requestUser.getString("id"));
                                }
                            }
                        }
                        if(!idsTeacherNotInNeo.isEmpty()) {
                            getDeletedTeacherInfos(eb, handler, idsTeacherNotInNeo, idsUserNamePrenom);
                        } else {
                            handler.handle(new Either.Right<>(idsUserNamePrenom));
                        }
                    } else {
                        handler.handle(new Either.Left<>(body.getString("message")));
                        log.error("getUsers : " + body.getString("message"));
                    }

                }));
    }

    private static void getDeletedTeacherInfos(EventBus eb, Handler<Either<String, Map<String, JsonObject>>> handler, List<String> idsTeacherNotInNeo, Map<String, JsonObject> idsUserNamePrenom) {
        JsonObject action2 = new JsonObject()
                .put(ACTION,"user.getDeletedTeachers")
                .put("idsTeacher", idsTeacherNotInNeo);
        eb.request(VIESCO_BUS_ADDRESS, action2, DELIVERY_OPTIONS,
                handlerToAsyncHandler(event -> {
                    JsonObject bodyDeletedUsers = event.body();
                    if(OK.equals(bodyDeletedUsers.getString(STATUS)) &&
                            bodyDeletedUsers.getJsonArray(RESULTS) != null &&
                            !bodyDeletedUsers.isEmpty()){
                        JsonArray deletedUsers = bodyDeletedUsers.getJsonArray(RESULTS);

                        deletedUsers.stream().forEach(   deletedUser -> {
                            JsonObject o_deletedUser = (JsonObject) deletedUser;
                            if (!idsUserNamePrenom.containsKey( o_deletedUser.getString("id_user"))) {
                                idsUserNamePrenom.put(o_deletedUser.getString("id_user"), new JsonObject()
                                        .put("firstName", o_deletedUser.getString("first_name"))
                                        .put("name", o_deletedUser.getString("last_name"))
                                        .put("id", o_deletedUser.getString("id_user"))
                                        .put("birthDate",o_deletedUser.getString("birth_date")));
                            }
                        });
                        handler.handle(new Either.Right<>(idsUserNamePrenom));

                    }else if(idsUserNamePrenom.isEmpty()){
                        handler.handle(new Either.Left<>("no User "));
                        log.error("getUsers : no User");
                    }else{
                        handler.handle(new Either.Right<>(idsUserNamePrenom));
                    }

                }));
    }

    /**
     * get infoEleve of one class order by lastName
     *
     * @param eb       eventBus
     * @param idClasse idClass
     * @param handler  elveList with idEleve,lastName, firstName, level, classes, className
     */
    public static void getEleveClasse(EventBus eb, List<String> idsEleve, final String idClasse, final Integer idPeriode,
                                      final Handler<Either<String, List<Eleve>>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getEleveClasse")
                .put(ID_CLASSE_KEY, idClasse)
                .put("idPeriode", idPeriode);

        eb.request(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                List<Eleve> eleves = new ArrayList<Eleve>();
                if (!OK.equals(body.getString(STATUS))) {
                    handler.handle(new Either.Left<>(body.getString("message")));
                    log.error("getEleveClasse :  " + body.getString("message"));
                } else {
                    JsonArray requesteleves = body.getJsonArray(RESULTS);
                    if (requesteleves != null && requesteleves.size() > 0) {
                        for (int i = 0; i < requesteleves.size(); i++) {
                            JsonObject requestEleve = requesteleves.getJsonObject(i);
                            Eleve eleve = new Eleve(requestEleve.getString("id"), requestEleve.getString("lastName"),
                                    requestEleve.getString("firstName"), idClasse, requestEleve.getString("className"));
                            eleves.add(eleve);
                            if (idsEleve != null) {
                                idsEleve.add(requestEleve.getString("id"));
                            }
                        }
                    } else {
                        handler.handle(new Either.Left<>("no Student in this Class " + idClasse));
                        log.error("getEleveClasse : no Student in this Class idClass: " + idClasse);
                    }
                    handler.handle(new Either.Right<>(eleves));
                }
            }
        }));
    }

    /**
     * @deprecated Use {@link #getPeriodLibelle(EventBus, String, String, Integer)}
     */
    @Deprecated
    public static void getLibellePeriode(EventBus eb, HttpServerRequest request, Integer idPeriode,
                                         Handler<Either<String, String>> handler) {
        getPeriodLibelle(eb, request.headers().get(Field.ACCEPT_LANGUAGE), getHost(request), idPeriode)
                .onSuccess(res -> handler.handle(new Either.Right<>(res)))
                .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }

    public static Future<String> getPeriodLibelle(EventBus eb, String language, String host, Integer periodId) {
        Promise<String> promise = Promise.promise();

        JsonObject jsonRequest = new JsonObject()
                .put(Field.HEADERS, new JsonObject()
                        .put(Field.ACCEPT_LANGUAGE, language))
                .put(Field.HOST, host);

        JsonObject action = new JsonObject()
                .put(Field.ACTION, "periode.getLibellePeriode")
                .put(Field.IDTYPE, periodId)
                .put(Field.REQUEST, jsonRequest);
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if (!OK.equals(body.getString(STATUS))) {
                String error = String.format("[Competences@%s::getEvaluableGroupsClass] periode not found",
                        Utils.class.getSimpleName());
                promise.fail(String.format("%s: %s. %s", error, periodId,
                        body.getString(Field.MESSAGE)));
                log.error(error);
            } else {
                promise.complete(body.getString(Field.RESULT));
            }
        }));
        return promise.future();
    }

    public static String getLibelle(String key) {
        return I18n.getInstance().translate(key,
                domain, locale);
    }

    public static void setLocale(String locale) {
        Utils.locale = locale;
    }

    public static void setDomain(String domain) {
        Utils.domain = domain;
    }

    public static String getPeriode(JsonArray periodesByClass, Boolean wantedBegenningPeriode) {
        String periode = null;
        Integer smallestTypeOfPeriode = periodesByClass.getJsonObject(0).getInteger("id_type");
        Integer biggestTypePeriode = periodesByClass.getJsonObject(0).getInteger("id_type");


        for (int i = 0; i < periodesByClass.size(); i++) {
            //get the begining periode
            if (wantedBegenningPeriode) {
                if (periodesByClass.getJsonObject(i).getInteger("id_type") <= smallestTypeOfPeriode) {
                    smallestTypeOfPeriode = periodesByClass.getJsonObject(i).getInteger("id_type");
                    periode = periodesByClass.getJsonObject(i).getString("timestamp_dt");
                }
            } else {
                if (periodesByClass.getJsonObject(i).getInteger("id_type") >= biggestTypePeriode) {
                    biggestTypePeriode = periodesByClass.getJsonObject(i).getInteger("id_type");
                    periode = periodesByClass.getJsonObject(i).getString("timestamp_fn");
                }
            }
        }

        return periode;
    }

    public static Boolean isNotNull(Object o) {
        return o != null;
    }

    public static Boolean isNull(Object o) {
        return o == null;
    }

    public static Boolean isCycleNotNull(String cycle) {
        return cycle != null && !cycle.equals("null");
    }

    public static <T> void returnFailure(String method, AsyncResult<CompositeFuture> event,
                                         Handler<Either<String, T>> handler) {
        String cause = event.cause().getMessage();
        log.error(method + cause);
        handler.handle(new Either.Left<>(cause));
    }

    public static String removeAccent(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    public static JsonArray sortElevesByDisplayName(JsonArray array) {
        List<JsonObject> eleves = array.getList();
        Collections.sort(eleves, new Comparator<JsonObject>() {
            private static final String KEY_DISPLAY_NAME = "displayName";
            private static final String KEY_LAST_NAME = "lastName";
            private static final String KEY_FIRST_NAME = "firstName";
            private static final String USER_KEY = "user";
            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                try {
                    if(!isNull(a.getJsonObject(USER_KEY))){
                        valA = a.getJsonObject(USER_KEY).getString(KEY_DISPLAY_NAME);
                        valB = b.getJsonObject(USER_KEY).getString(KEY_DISPLAY_NAME);
                        if(isNull(valA))
                            valA = a.getJsonObject(USER_KEY).getString(KEY_LAST_NAME) + " "
                                    + a.getJsonObject(USER_KEY).getString(KEY_FIRST_NAME);
                        if(isNull(valB))
                            valB = b.getJsonObject(USER_KEY).getString(KEY_LAST_NAME) + " "
                                    + b.getJsonObject(USER_KEY).getString(KEY_FIRST_NAME);
                    }
                    else {
                        valA = a.getString(KEY_DISPLAY_NAME);
                        valB = b.getString(KEY_DISPLAY_NAME);
                        if(isNull(valA))
                            valA = a.getString(KEY_LAST_NAME) + " " + a.getString(KEY_FIRST_NAME);
                        if(isNull(valB))
                            valB = b.getString(KEY_LAST_NAME) + " " + b.getString(KEY_FIRST_NAME);
                    }
                    valA = removeAccent(valA);
                    valB = removeAccent(valB);
                } catch (Exception e) {
                    //do something
                }
                return valA.compareTo(valB);
            }
        });
        return new JsonArray(eleves);
    }

    public static String getMaitrise(String maitrise, String key) {
        if (maitrise == null) {
            return getMaitrise(key);
        } else if (maitrise.equals("  ")) {
            return getMaitrise(key);
        } else {
            return maitrise;
        }
    }

    public static String getMaitrise(String key) {
        switch (key) {
            case "1":
                return "MI";
            case "2":
                return "MF";
            case "3":
                return "MS";
            default:
                return "TB";
        }
    }
    public static Map<String, JsonObject> extractData(JsonArray collection, String key) {

        Map<String, JsonObject> result = new LinkedHashMap<>();

        for (int i = 0; i < collection.size(); i++) {
            if (collection.getValue(i) instanceof String) {
                continue;
            }
            JsonObject item = collection.getJsonObject(i);
            String itemKey = String.valueOf(item.getValue(key));
            if (!result.containsKey(itemKey)) {
                result.put(itemKey, item);
            }
        }

        return result;
    }

    public static JsonArray orderBy(JsonArray collection, String key, Boolean inverted) {
        Set<String> sortedSet = inverted ? new TreeSet<String>(Collections.reverseOrder()) : new TreeSet<String>();
        Map<String, JsonArray> unsortedMap = new HashMap<>();
        JsonArray result = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < collection.size(); i++) {
            JsonObject item = collection.getJsonObject(i);
            String itemKey = String.valueOf(item.getValue(key));
            if (!unsortedMap.containsKey(itemKey)) {
                unsortedMap.put(itemKey, new fr.wseduc.webutils.collections.JsonArray());
            }
            unsortedMap.get(itemKey).add(item);
            sortedSet.add(itemKey);
        }

        for (String aSortedSet : sortedSet) {
            utilsService.saUnion(result, unsortedMap.get(aSortedSet));
        }
        return result;
    }

    public static JsonArray orderBy(JsonArray collection, String key) {
        return orderBy(collection, key, false);
    }

    public static  JsonArray addMaitriseNE(JsonArray maitrises) {
        JsonObject nonEvalue = new JsonObject();
        String libelle = getLibelle("evaluations.competence.unevaluated");
        nonEvalue.put("libelle", libelle);
        nonEvalue.put(ORDRE, 0);
        nonEvalue.put("default", "grey");
        nonEvalue.put("lettre", "NE");
        maitrises.add(nonEvalue);

        return maitrises;
    }

    public static JsonArray sortJsonArrayIntValue(final String KEY_NAME, final JsonArray arrayNoSort) {
        final List<JsonObject> listOfJsonObjects = new ArrayList<>();
        for (int i = 0; i < arrayNoSort.size(); i++) {
            final JsonObject objectNoSorted = arrayNoSort.getJsonObject(i);
            listOfJsonObjects.add(objectNoSorted);
        }
        listOfJsonObjects.sort((firstObject, secondObject) -> {
            int firstValue = 0;
            int secondValue = 0;

            if (firstObject != null && secondObject != null &&
                    firstObject.containsKey(KEY_NAME) && secondObject.containsKey(KEY_NAME)) {
                firstValue = firstObject.getInteger(KEY_NAME);
                secondValue = secondObject.getInteger(KEY_NAME);
            }

            return Integer.compare(firstValue, secondValue);
        });

        final JsonArray sortedJsonArray = new JsonArray();
        for (final JsonObject objectSorted : listOfJsonObjects) {
            sortedJsonArray.add(objectSorted);
        }
        return sortedJsonArray;
    }

    public static JsonObject sortJsonObjectIntValue(final String KEY_NAME, final JsonObject objectNoSort) {
        final JsonObject sortedJsonObject = new JsonObject();

        JsonArray arrayToSort = new JsonArray();

        for (Map.Entry<String, Object> stringObjectEntry : objectNoSort) {
            String key = stringObjectEntry.getKey();
            JsonObject jsonObject = objectNoSort.getJsonObject(key);
            jsonObject.put("key", key);
            arrayToSort.add(jsonObject);
        }

        arrayToSort = Utils.sortJsonArrayIntValue(KEY_NAME, arrayToSort);

        for(Object object : arrayToSort) {
            JsonObject jsonObject = (JsonObject) object;
            String key = jsonObject.getString("key");
            jsonObject.remove("key");
            sortedJsonObject.put(key, jsonObject);
        }

        return sortedJsonObject;
    }

    public static JsonArray sortJsonArrayDate(final String KEY_NAME, final JsonArray arrayNoSort) {
        final List<JsonObject> listOfJsonObjects = new ArrayList<>();
        for (int i = 0; i < arrayNoSort.size(); i++) {
            final JsonObject objectNoSorted = arrayNoSort.getJsonObject(i);
            listOfJsonObjects.add(objectNoSorted);
        }
        Collections.sort(listOfJsonObjects, (firstObject, secondeObject) -> {
            Date firstValue = new Date();
            Date secondValue = new Date();
            try {
                firstValue = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                        .parse(firstObject.getString(KEY_NAME).split(" ")[0]);
                secondValue = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                        .parse(secondeObject.getString(KEY_NAME).split(" ")[0]);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return firstValue.getTime() > secondValue.getTime() ? -1 : 1;     //descending
        });

        final JsonArray sortedJsonArray = new JsonArray();
        for (final JsonObject objectSorted : listOfJsonObjects) {
            sortedJsonArray.add(objectSorted);
        }
        return sortedJsonArray;
    }
}
