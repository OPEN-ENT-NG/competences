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
import fr.openent.competences.service.MatiereService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultMatiereService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.openent.competences.utils.UtilsConvert;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
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

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.TIME;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.Renders.getHost;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Utils {

    protected static final Logger log = LoggerFactory.getLogger(Utils.class);
    protected static UtilsService utilsService = new DefaultUtilsService();
    protected static MatiereService matiereService = new DefaultMatiereService();

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

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
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
     * retourne une classe avec ses groups (ids)
     *
     * @param eb         eventbus
     * @param idsClasses array une clase
     * @param handler    response l'id de classe avec ses groups s'ils existent sinon retourne que id de la classe
     */
    public static void getGroupesClasse(EventBus eb, final JsonArray idsClasses, final Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getGroupesClasse")
                .put("idClasses", idsClasses);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        if (OK.equals(body.getString(STATUS))) {
                            JsonArray queryResult = body.getJsonArray(RESULTS);
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
                .put(ACTION, "classe.getClassesInfo")
                .put("idClasses", idsClasses);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
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

    public static void getIdElevesClassesGroupes(EventBus eb, final String idGroupe,
                                                 final Integer idPeriode,
                                                 final int typeClasse,
                                                 final Handler<Either<String, List<String>>> handler) {

        JsonObject action = new JsonObject();
        if (typeClasse == 0) {
            action.put(ACTION, "classe.getEleveClasse")
                    .put(ID_CLASSE_KEY, idGroupe);

            if (idPeriode != null) action.put("idPeriode", idPeriode);

        } else if (typeClasse == 1 || typeClasse == 2) {
            action.put(ACTION, "groupe.listUsersByGroupeEnseignementId")
                    .put("groupEnseignementId", idGroupe)
                    .put("profile", "Student");

            if (idPeriode != null) action.put("idPeriode", idPeriode);
        }


        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                List<String> idEleves = new ArrayList<String>();
                if (OK.equals(body.getString(STATUS))) {
                    JsonArray queryResult = body.getJsonArray(RESULTS);
                    if (queryResult != null) {
                        for (int i = 0; i < queryResult.size(); i++) {
                            idEleves.add(((JsonObject) queryResult.getJsonObject(i)).getString("id"));
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
     *
     * @param eb      EventBus
     * @param idEleve id Eleve
     * @param handler response eleve groups
     */

    public static void getGroupsEleve(EventBus eb, String idEleve, String idEtablissement,
                                      Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getInfoEleve")
                .put(Competences.ID_ETABLISSEMENT_KEY, idEtablissement)
                .put("idEleves", new fr.wseduc.webutils.collections.JsonArray().add(idEleve));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS,
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

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS,
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
        log.info("");
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getElevesClasses")
                .put(ID_PERIODE_KEY, idPeriode)
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(idsClasse)));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS,
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

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
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

    public static List<Eleve> toListEleve(JsonArray queryResult, Map<String, String> mapCycleLibelle) {
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
            eleveObj.setCycle(mapCycleLibelle.get(eleveObj.getIdClasse()));
            result.add(eleveObj);
        }
        return result;
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
    public static void getInfoEleve(EventBus eb, String[] idEleves, String idEtablissment,
                                    final Handler<Either<String, List<Eleve>>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getInfoEleve")
                .put(Competences.ID_ETABLISSEMENT_KEY, idEtablissment)
                .put("idEleves", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(idEleves)));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS,
                handlerToAsyncHandler(response -> {
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
                                        eleveBase.getString("lastName"),
                                        eleveBase.getString("firstName"),
                                        eleveBase.getString(ID_CLASSE_KEY),
                                        eleveBase.getString("classeName"),
                                        eleveBase.getString(LEVEL),
                                        eleveBase.getString("birthDate"));
                                classes.add(eleveObj.getIdClasse());
                                result.add(eleveObj);
                            }
                            getCycleElevesForBfcCycle(classes, result, handler);
                        }
                        // S'il existe des élèves stockés dans postgres, on va récupérer le nom des classes dans Neo
                        else {
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
                                                eleveBase.getString("lastName"),
                                                eleveBase.getString("firstName"),
                                                eleveBase.getString(ID_CLASSE_KEY),
                                                eleveBaseClasseName,
                                                eleveBase.getString("level"),
                                                eleveBase.getString("birthDate"));
                                        classes.add(eleveObj.getIdClasse());
                                        result.add(eleveObj);
                                    }
                                    getCycleElevesForBfcCycle(classes, result, handler);
                                }
                            });

                        }

                    } else {
                        handler.handle(new Either.Left<>(bodyResponse.getString(MESSAGE)));
                        log.error("getInfoEleve : getInfoEleve : " + bodyResponse.getString(MESSAGE));
                    }
                }));

    }

    private static void getCycleElevesForBfcCycle(final Set<String> classes, final List<Eleve> result,
                                                  final Handler<Either<String, List<Eleve>>> handler) {
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
                    getCycleElevesForBfcCycle(classes, result, handler);
                    return;
                }
                handler.handle(new Either.Left<>(event.left().getValue()));

            }
        });

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

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS,
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
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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

    public static void getLastNameFirstNameUser(EventBus eb, final JsonArray idsUsers,
                                                final Handler<Either<String, Map<String, JsonObject>>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getUsers")
                .put("idUsers", idsUsers);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {

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
                                                .put("id", requestUser.getString("id")));
                                    }

                                }
                            } else {
                                handler.handle(new Either.Left<>("no User "));
                                log.error("getUsers : no User");
                            }
                            handler.handle(new Either.Right<String, Map<String, JsonObject>>(idsUserNamePrenom));
                        } else {
                            handler.handle(new Either.Left<String, Map<String, JsonObject>>(body.getString("message")));
                            log.error("getUsers : " + body.getString("message"));
                        }

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

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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

    public static void getLibellePeriode(EventBus eb, HttpServerRequest request, Integer idPeriode,
                                         Handler<Either<String, String>> handler) {


        JsonObject jsonRequest = new JsonObject()
                .put("headers", new JsonObject()
                        .put("Accept-Language", request.headers().get("Accept-Language")))
                .put("Host", getHost(request));

        JsonObject action = new JsonObject()
                .put(ACTION, "periode.getLibellePeriode")
                .put("idType", idPeriode)
                .put("request", jsonRequest);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if (!OK.equals(body.getString(STATUS))) {
                    handler.handle(new Either.Left<>("periode not found " + idPeriode));
                    log.error("getLibellePeriode : periode not found: " + idPeriode);
                } else {
                    handler.handle(new Either.Right<>(body.getString("result")));
                }
            }
        }));
    }

    public static String getLibelle(String key) {
        return I18n.getInstance().translate(key,
                I18n.DEFAULT_DOMAIN, Locale.FRANCE);
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

    public static <T> void returnFailure(String method, AsyncResult<CompositeFuture> event,
                                         Handler<Either<String, T>> handler) {
        String cause = event.cause().getMessage();
        log.error(method + cause);
        handler.handle(new Either.Left<>(cause));
    }

    public static JsonArray sortUsersByDisplayNameAndFirstName(JsonArray users) {

        List<JsonObject> eleves = users.getList();
        Collections.sort(eleves, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "displayName";
            private static final String USER_KEY = "user";
            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                try {
                    valA = a.getString(KEY_NAME);
                    valB = b.getString(KEY_NAME);
                    if(isNull(valA) || isNull(valB)){
                        valA = a.getJsonObject(USER_KEY).getString(KEY_NAME);
                        valB = b.getJsonObject(USER_KEY).getString(KEY_NAME);
                    }
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

}
