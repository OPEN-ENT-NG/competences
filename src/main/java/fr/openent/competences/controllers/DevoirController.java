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
import fr.openent.competences.constants.Field;
import fr.openent.competences.enums.EventStoresCompetences;
import fr.openent.competences.helpers.DevoirControllerHelper;
import fr.openent.competences.security.*;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.CompetencesService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultCompetencesService;
import fr.openent.competences.service.impl.DefaultDevoirService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.openent.competences.utils.HomeworkUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static fr.openent.competences.Competences.NN;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

/**
 * Created by ledunoiss on 04/08/2016.
 */
public class DevoirController extends ControllerHelper {

    /**
     * Déclaration des services
     */
    private final DefaultDevoirService devoirsService;
    private final CompetencesService competencesService;
    private final UtilsService utilsService;
    private EventStore eventStore;

    public DevoirController(EventBus eb, EventStore eventStore) {
        this.eb = eb;
        this.eventStore = eventStore;
        this.utilsService = new DefaultUtilsService(eb);
        devoirsService = new DefaultDevoirService(eb);
        competencesService = new DefaultCompetencesService(eb);
    }

    @Get("/devoirs")
    @ApiDoc("Récupère les devoirs d'un utilisateur")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    public void getDevoirs(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, user -> {
            if(user != null){
                String forStudentReleveString = request.params().get("forStudentReleve");
                boolean forStudentReleve = false;
                if(forStudentReleveString != null)
                    forStudentReleve = forStudentReleveString.equals("true");

                String idEtablissement = request.params().get("idEtablissement");

                String limit = request.params().get("limit");
                Integer iLimit = limit == null ? null : Integer.valueOf(limit);

                // si l'utilisateur a la fonction d'admin
                if(WorkflowActionUtils.hasRight(user, WorkflowActions.ADMIN_RIGHT.toString()) && !forStudentReleve) {
                    devoirsService.listDevoirsChefEtab(user, idEtablissement, iLimit, getDevoirHandler(request));
                } else {
                    String idClasse = request.params().get("idClasse");
                    String idMatiere = request.params().get("idMatiere");

                    final String _STUDENT = "Student";
                    final String _RELATIVE = "Relative";
                    if (idClasse == null && !_STUDENT.equals(user.getType())
                            && !_RELATIVE.equals(user.getType()) && !forStudentReleve) {
                        devoirsService.listDevoirs(user, idEtablissement, iLimit, getDevoirHandler(request));
                    } else {
                        boolean historise = false;
                        if (request.params().get("historise") != null) {
                            historise = Boolean.parseBoolean(request.params().get("historise"));
                        }
                        Long idPeriode = null;
                        if (request.params().get("idPeriode") != null) {
                            idPeriode = testLongFormatParameter("idPeriode", request);
                        }

                        if(_STUDENT.equals(user.getType()) || _RELATIVE.equals(user.getType()) || forStudentReleve){
                            String idEleve = request.params().get("idEleve");
                            devoirsService.listDevoirs(idEleve, idEtablissement, idClasse, null,
                                    idPeriode, historise, getDevoirHandler(request));
                        } else if (!idEtablissement.equals("undefined") && !idClasse.equals("undefined")
                                && !idMatiere.equals("undefined") && !request.params().get("idPeriode").equals("undefined")) {
                            devoirsService.listDevoirs(null, idEtablissement, idClasse, idMatiere,
                                    idPeriode, historise, getDevoirHandler(request));
                        } else {
                            Renders.badRequest(request, "Invalid parameters");
                        }
                    }
                }
            } else {
                unauthorized(request);
            }
        });
    }

    private Handler<Either<String, JsonArray>> getDevoirHandler(HttpServerRequest request) {
        return event -> {
            event.right().getValue().stream().forEach(obj -> {
                JsonObject result = (JsonObject) obj;
                if (result.containsKey(Field.DIVISEUR)) {
                    result.put(Field.DIVISEUR, HomeworkUtils.safeGetDouble(result, Field.DIVISEUR));
                }
            });
            if (event.isRight()) {
                Renders.renderJson(request, event.right().getValue());
            } else {
                JsonObject error = (new JsonObject()).put(Field.ERROR, event.left().getValue());
                Renders.renderJson(request, error, 400);
            }
        };
    }

    /**
     * Créer un devoir avec les paramètres passés en post.
     * @param request
     */
    @Post("/devoir")
    @ApiDoc("Créer un devoir")
    @SecuredAction("competences.create.evaluation")
    public void createDevoir(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                        @Override
                        public void handle(final JsonObject resource) {
                            if(null != resource.getLong("type_groupe")
                                    && resource.getLong("type_groupe") > -1){
                                DevoirControllerHelper.creationDevoir(request, user, resource, pathPrefix,
                                        devoirsService, eb);
                                eventStore.createAndStoreEvent(EventStoresCompetences.CREATE_HOMEWORK.name(), request);
                            } else {
                                checkEleveEvaluable(resource, request, user);
                            }
                        }
                    });
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    private void checkEleveEvaluable(JsonObject resource, HttpServerRequest request, UserInfos user) {
        JsonObject action = new JsonObject()
                .put("action", "eleve.isEvaluableOnPeriode")
                .put("idEleve", resource.getJsonObject("competenceEvaluee").getString("id_eleve"))
                .put("idPeriode", new Long(resource.getInteger("id_periode")))
                .put(Competences.ID_ETABLISSEMENT_KEY, resource.getString("id_etablissement"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status"))) {
                    if(body.getJsonArray("results").size() > 0){
                        DevoirControllerHelper.creationDevoir(request, user, resource, pathPrefix,
                                devoirsService, eb);
                    } else {
                        log.debug("Student not evaluable on this period");
                        Renders.unauthorized(request);
                    }
                } else {
                    log.debug("Student not evaluable on this period");
                    Renders.unauthorized(request);
                }
            }
        }));
    }

    /**
     * Liste des devoirs publiés par l'utilisateur pour un établissement et une période donnée.
     * La liste est ordonnée selon la date du devoir (du plus ancien au plus récent).
     *
     * @param request
     */
    @Get("/devoirs/periode/:idPeriode")
    @ApiDoc("Liste des devoirs publiés par l'utilisateur pour un établissement et une période donnée.")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AccessPeriodeFilter.class)
    public void listDevoirsPeriode (final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    MultiMap params = request.params();

                    Long idPeriode = testLongFormatParameter("idPeriode", request);

                    String idEtablissement = params.get("idEtablissement");
                    String idUser = params.get("idUser");
                    Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                    devoirsService.listDevoirs(idEtablissement, idPeriode, idUser, handler);
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    @Put("/devoirs/:idDevoir/visibility")
    @ApiDoc("Permet de switcher l'etat de visibiliter des appreciations du devoir")
    @ResourceFilter(AccessVisibilityAppreciation.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void switchVisibilityApprec(final HttpServerRequest request) {
        try {
            Long idDevoir = Long.parseLong(request.params().get("idDevoir"));

            devoirsService.switchVisibilityApprec(idDevoir, arrayResponseHandler(request));
        } catch (NumberFormatException err) {
            leftToResponse(request, new Either.Left<>(err.toString()));
        }
    }

    @Get("/devoirs/evaluations/information")
    @ApiDoc("Recupère la liste des compétences pour un devoir donné")
    @ResourceFilter(AccessEvaluationFilter.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void isEvaluatedDevoir(final HttpServerRequest request){
        Long idDevoir  = testLongFormatParameter("idDevoir", request);

        Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
        devoirsService.getevaluatedDevoir(idDevoir,handler);

    }

    @Get("/devoirs/evaluations/informations")
    @ApiDoc("Recupère pour une liste de devoirs ne nombre de competences evaluer et de notes saisie")
    @ResourceFilter(AccessEvaluationFilter.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void areEvaluatedDevoirs(final HttpServerRequest request){
        List<String> idDevoirsList = request.params().getAll("idDevoir");

        if (idDevoirsList == null || idDevoirsList.size() == 0) {
            log.error("Error : one id must be present");
            badRequest(request);
            return;
        }

        Long[] idDevoirsArray = new Long[idDevoirsList.size()] ;
        try {
            for (int i = 0; i < idDevoirsList.size(); i++) {
                idDevoirsArray[i] = Long.parseLong(idDevoirsList.get(i));
            }
        } catch(NumberFormatException e) {
            log.error("Error : id must be a long object", e);
            badRequest(request);
        }

        Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
        devoirsService.getevaluatedDevoirs(idDevoirsArray,handler);
    }

    /**
     * Met à jour un devoir
     * @param request
     */
    @Put("/devoir")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessEvaluationFilter.class)
    @ApiDoc("Met à jour un devoir")
    public void updateDevoir (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix +
                Competences.SCHEMA_DEVOIRS_UPDATE, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject devoir) {
                final List<String> idDevoirsList = request.params().getAll("idDevoir");
                final HashMap<Long, Integer> nbCompetencesByDevoir = new HashMap<>();
                final String idGroupe = devoir.getString("id_groupe");
                final String idEtablissement = devoir.getString("id_etablissement");

                JsonObject jsonRequest = new JsonObject()
                        .put("headers", new JsonObject()
                                .put("Accept-Language", request.headers().get("Accept-Language")))
                        .put("Host", getHost(request));
                JsonObject action = new JsonObject()
                        .put("action", "periode.getPeriodes")
                        .put("idEtablissement", idEtablissement)
                        .put("idGroupes", new fr.wseduc.webutils.collections.JsonArray().add(idGroupe))
                        .put("request", jsonRequest);

                eb.send(Competences.VIESCO_BUS_ADDRESS, action,handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        JsonArray periodes = body.getJsonArray("result");
                        boolean isUpdatable = true;

                        if ("ok".equals(body.getString("status"))) {
                            // On vérifie que la date de fin de saisie n'est pas dépassée
                            final Number idPeriode =  devoir.getLong("id_periode");
                            JsonObject periode = null;
                            for(int i =0; i< periodes.size(); i++) {
                                if(idPeriode.intValue()
                                        == ((JsonObject)periodes.getJsonObject(i)).getLong("id_type").intValue()) {
                                    periode = (JsonObject)periodes.getJsonObject(i);
                                    break;
                                }
                            }
                            if (periode != null) {
                                String dateFinSaisieStr = periode.getString("date_fin_saisie")
                                        .split("T")[0];
                                DateFormat formatter = new SimpleDateFormat("yy-MM-dd");
                                try {
                                    Date dateFinSaisie = formatter.parse(dateFinSaisieStr);
                                    Date dateActuelle = new Date();
                                    dateActuelle.setTime(0);
                                    if(dateActuelle.after(dateFinSaisie)){
                                        isUpdatable = false;
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                            else {
                                isUpdatable = false;
                            }

                            if (!isUpdatable) {
                                leftToResponse(request, new Either.Left<String, String>("END OF SAISIE"));
                            } else {
                                Long[] idDevoirsArray = new Long[idDevoirsList.size()];

                                for (int i = 0; i < idDevoirsList.size(); i++) {
                                    idDevoirsArray[i] = Long.valueOf(idDevoirsList.get(i));
                                }
                                // On recherche le Nombre de compétences sur le devoir à mettre à jour
                                devoirsService.getNbCompetencesDevoirs(idDevoirsArray, new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            if (event.right().getValue() != null) {
                                                JsonArray resultNbCompetencesDevoirs = event.right().getValue();

                                                for (int i = 0; i < resultNbCompetencesDevoirs.size(); i++) {
                                                    JsonObject o = resultNbCompetencesDevoirs.getJsonObject(i);

                                                    if (o != null) {
                                                        nbCompetencesByDevoir.put(o.getLong("id"),
                                                                o.getInteger("nb_competences"));
                                                    }
                                                }

                                                // On limite le nbre de compétence d' un devoir
                                                if ((devoir.containsKey("competencesAdd")
                                                        && devoir.containsKey("competencesRem"))

                                                        && ((nbCompetencesByDevoir.get(Long.valueOf(request
                                                        .params().get("idDevoir")))
                                                        + devoir.getJsonArray("competencesAdd").size()
                                                        - devoir.getJsonArray("competencesRem").size())
                                                        <= Competences.MAX_NBR_COMPETENCE)) {
                                                    devoirsService.updateDevoir(request.params().get("idDevoir"),
                                                            devoir, arrayResponseHandler(request));
                                                } else {
                                                    leftToResponse(request, event.left());
                                                }
                                            } else {
                                                leftToResponse(request, event.left());
                                            }
                                        } else {
                                            leftToResponse(request, event.left());
                                        }
                                    }
                                });
                            }
                        }
                    }
                }));
            }
        });
    }


    /**
     * @param request
     * @queryParam {idEtablissement} mandatory
     */
    @Get("/devoirs/done")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    @ApiDoc("Calcul le pourcentage réalisé pour un devoir")
    public void getPercentDone(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if(user != null) {
                final Long idDevoir = testLongFormatParameter("idDevoir", request);
                final Integer nbStudents = Integer.parseInt(request.params().get("nbStudents"));

                new DefaultDevoirService(eb).getDevoirInfo(idDevoir, devoirInfo -> {
                    if (devoirInfo.isRight()) {
                        final JsonObject devoirInfos = devoirInfo.right().getValue();

                        final boolean is_evaluated = devoirInfos.getBoolean("is_evaluated");
                        final boolean has_competence = devoirInfos.getLong("nbrcompetence") > 0;
                        final String idGroupe = devoirInfos.getString("id_groupe");

                        final HashMap<Long, Float> nbNotesByDevoir = new HashMap<>();
                        if (is_evaluated) {
                            updatePercentageWithNotes(idDevoir, user, idGroupe,
                                    nbNotesByDevoir, is_evaluated, request, has_competence, nbStudents);
                        } else {
                            updatePercentWithAnnotationsAndCompetences(idDevoir, idGroupe,
                                    nbNotesByDevoir, is_evaluated, request, has_competence, nbStudents);
                        }
                    } else {
                        leftToResponse(request, devoirInfo.left());
                    }

                });
            }
        });
    }

    /**
     *  Supprimer un devoir
     */
    @Delete("/devoir")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessEvaluationFilter.class)
    @ApiDoc("Supprime un devoir")
    public void remove(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    devoirsService.getDevoirInfo(Long.parseLong(request.params().get("idDevoir")), new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(final Either<String, JsonObject> devoirInfo) {
                            if (devoirInfo.isRight()) {
                                final JsonObject devoirInfos = (JsonObject) ((Either.Right) devoirInfo).getValue();
                                final String idGroupe = devoirInfos.getString("id_groupe");
                                final Long idPeriode = devoirInfos.getLong("id_periode");
                                final String idMatiere = devoirInfos.getString("id_matiere");
                                devoirsService.delete(request.params().get("idDevoir"), user, new Handler<Either<String, JsonObject>>() {
                                    @Override
                                    public void handle(Either<String, JsonObject> event) {
                                        if (event.isRight()) {
                                            devoirsService.autoCleanSQLTable(new Handler<Either<String, JsonObject>>() {
                                                @Override
                                                public void handle(Either<String, JsonObject> event) {
                                                    if (event.isRight()) {
                                                        updateSQLTablesAfterDelete(idGroupe,idPeriode,idMatiere, request);
                                                    } else {
                                                        badRequest(request);
                                                    }
                                                }
                                            });
                                        } else {
                                            badRequest(request);
                                        }
                                    }
                                });
                            } else {
                                badRequest(request);
                            }
                        }
                    });
                } else {
                    unauthorized(request);
                }
            }
        });
    }

    @Get("/devoir/:idDevoir/moyenne")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    @ApiDoc("Retourne la moyenne du devoir dont l'id est passé en paramètre")
    public void getMoyenneDevoir(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if(user != null) {
                Long idDevoir = Long.parseLong(request.params().get("idDevoir"));

                devoirsService.getMoyenne(idDevoir,null, event -> {
                    if(event.isRight()) {
                        Renders.renderJson(request, event.right().getValue());
                    } else {
                        leftToResponse(request, event.left());
                    }
                });
            }
        });
    }

    @Post("/devoir/:idDevoir/duplicate")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessEvaluationFilter.class)
    @ApiDoc("Duplique un devoir pour une liste de classes donnée")
    public void duplicateDevoir(final HttpServerRequest request) {
        if (!request.params().contains("idDevoir")) {
            badRequest(request);
        } else {
            UserUtils.getUserInfos(eb, request, user -> RequestUtils.bodyToJson(request, body -> {
                devoirsService.duplicateDevoirs(request, user, body, competencesService, shareService);
                eventStore.createAndStoreEvent(EventStoresCompetences.CREATE_HOMEWORK.name(), request);
            }));
        }
    }

    @Get("/devoirs/service")
    @ApiDoc("Récupère la liste des devoirs liés à un service")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getDevoirsService(HttpServerRequest request) {
        if(request.params().contains("id_groups") && request.params().contains("id_matiere")
                && request.params().contains("id_enseignant")) {
            devoirsService.listDevoirsService(request.params().get("id_enseignant"), request.params().get("id_matiere"),
                    Arrays.asList(request.params().get("id_groups").split(",")), arrayResponseHandler(request));
        } else {
            badRequest(request);
        }
    }

    /**
     * @param request
     * @queryParam {idEtablissement} mandatory
     */
    @Put("/devoirs/service")
    @ApiDoc("Mets à jour les devoirs liés à un service")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessViscoParamServiceStructure.class)
    public void updateDevoirsService(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject entries) {
                if(entries.containsKey("id_devoirs") && entries.containsKey("id_matiere")) {
                    devoirsService.updateDevoirsService(entries.getJsonArray("id_devoirs"), entries.getString("id_matiere"), arrayResponseHandler(request));
                } else {
                    badRequest(request);
                }
            }
        });
    }

    @Put("/devoirs/delete")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessEvaluationFilter.class)
    @ApiDoc("Supprime des devoir")
    public void removeMultiple(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, entries -> {
            if (!entries.containsKey("id_devoirs")) {
                badRequest(request);
            } else {
                Long[] idDevoirs = new Long[entries.getJsonArray("id_devoirs").getList().size()];
                for (int i = 0; i < entries.getJsonArray("id_devoirs").getList().size(); i++) {
                    idDevoirs[i] = Long.parseLong(entries.getJsonArray("id_devoirs").getList().get(i).toString());
                }
                devoirsService.getDevoirsInfos(idDevoirs, new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(final Either<String, JsonArray> devoirsInfos) {
                        if (devoirsInfos.isRight()) {
                            final JsonArray devoirsInfosList = devoirsInfos.right().getValue();
                            devoirsService.delete(entries.getJsonArray("id_devoirs"), new Handler<Either<String, JsonObject>>() {
                                @Override
                                public void handle(Either<String, JsonObject> event) {
                                    if (event.isRight()) {
                                        devoirsService.autoCleanSQLTable(new Handler<Either<String, JsonObject>>() {
                                            @Override
                                            public void handle(Either<String, JsonObject> event) {
                                                if (event.isRight()) {
                                                    updateSQLTablesAfterDelete(devoirsInfosList.getJsonObject(0).getString("id_groupe"),Long.parseLong(devoirsInfosList.getJsonObject(0).getValue("id_periode").toString()),devoirsInfosList.getJsonObject(0).getString("id_matiere"), request);
                                                } else {
                                                    badRequest(request);
                                                }
                                            }
                                        });
                                    } else {
                                        badRequest(request);
                                    }
                                }
                            });
                        } else {
                            badRequest(request);
                        }
                    }
                });
            }
        });
    }

    private void updateSQLTablesAfterDelete(final String idGroupe,final Long idPeriode,final String idMatiere,final HttpServerRequest request){
        devoirsService.getEleveGroups(idGroupe, new Handler<Either<String, JsonArray>>(){
            @Override
            public void handle(final Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray listElevesGroups = event.right().getValue();
                    List<ArrayList<String>> listOfListGroups = new ArrayList<ArrayList<String>>();
                    List<ArrayList<String>> listOfListEleves = new ArrayList<ArrayList<String>>();
                    for (int i = 0; i < listElevesGroups.size(); i++) {
                        JsonObject jsonObjectEleve = listElevesGroups.getJsonObject(i);
                        ArrayList<String> groupes = new ArrayList<String>();
                        ArrayList<String> eleve = new ArrayList<String>();
                        eleve.add(jsonObjectEleve.getString("idEleve"));
                        groupes.add(idGroupe);
                        for(Object groupe : jsonObjectEleve.getJsonArray("id_groupes")){
                            groupes.add(((String)groupe));
                        }
                        if (!listOfListGroups.contains(groupes)) {
                            listOfListGroups.add(groupes);
                            listOfListEleves.add(eleve);
                        }else{
                            listOfListEleves.get(listOfListGroups.indexOf(groupes)).add(jsonObjectEleve.getString("idEleve"));
                        }
                    }
                    List<Future> futures = new ArrayList<>();
                    for (int i = 0; i < listOfListGroups.size(); i++) {
                        List<String> listEleves = listOfListEleves.get(i);
                        List<String> listGroups = listOfListGroups.get(i);
                        Future<JsonArray> future1 = Future.future();
                        devoirsService.updatePositionnementTableAfterDelete(listEleves, listGroups, idMatiere, idPeriode, eventFutur -> {
                            if (eventFutur.isRight()) {
                                future1.complete(eventFutur.right().getValue());
                            } else {
                                log.error(eventFutur.left());
                                future1.complete(new JsonArray());
                            }
                        });
                        futures.add(future1);
                        Future<JsonArray> future2 = Future.future();
                        devoirsService.updateCompetenceNiveauFinalTableAfterDelete(listEleves, listGroups, idMatiere, idPeriode, eventFutur -> {
                            if (eventFutur.isRight()) {
                                future2.complete(eventFutur.right().getValue());
                            } else {
                                log.error(eventFutur.left());
                                future2.complete(new JsonArray());
                            }
                        });
                        futures.add(future2);
                    }
                    CompositeFuture.all(futures).setHandler(eventFutur -> {
                        if (eventFutur.succeeded()) {
                            Renders.ok(request);
                        } else {
                            leftToResponse(request, new Either.Left<String,JsonObject>(eventFutur.cause().getMessage()));
                        }
                    });
                } else {
                    leftToResponse(request, event.left());
                }
            }
        });
    }

    private void updatePercentageWithNotes(final long idDevoir, final UserInfos user, final String idGroupe,
                                           final HashMap<Long, Float> nbNotesByDevoir, final boolean is_evaluated,
                                           final HttpServerRequest request, final boolean has_competence,
                                           final Integer nbStudents){
        devoirsService.getNbNotesDevoirs(user, idDevoir, event -> {
            if (event.isRight()) {
                JsonArray resultNbNotesDevoir = event.right().getValue();

                if (resultNbNotesDevoir != null && resultNbNotesDevoir.size() > 0) {
                    JsonObject o = resultNbNotesDevoir.getJsonObject(0);
                    if (o != null) {
                        if (has_competence) {
                            nbNotesByDevoir.put(o.getLong("id"),
                                    Float.valueOf(o.getInteger("nb_notes")) / 2);
                        } else {
                            nbNotesByDevoir.put(o.getLong("id"),
                                    Float.valueOf(o.getInteger("nb_notes")));
                        }
                    }
                }
                updatePercentWithAnnotationsAndCompetences(idDevoir, idGroupe, nbNotesByDevoir,
                        is_evaluated, request, has_competence, nbStudents);
            } else {
                leftToResponse(request, event.left());
            }
        });
    }

    void updatePercentWithAnnotationsAndCompetences(final long idDevoir, final String idGroupe,
                                                    final HashMap<Long, Float> nbNotesByDevoir,
                                                    final boolean is_evaluated, final HttpServerRequest request,
                                                    final boolean devoirHasCompetence, final Integer nbStudents){
        // On récupère le nombre d'annotations
        Future<JsonArray> nbAnnotationsDevoirsFuture = Future.future();
        devoirsService.getNbAnnotationsDevoirs(idDevoir,
                nbsAnnotations -> formate(nbAnnotationsDevoirsFuture, nbsAnnotations));

        // On récupère le nombre de compétences par élèves pour le devoir courant
        Future<JsonArray> nbCompetenceNotesDevoirsFuture = Future.future();
        devoirsService.getNbCompetencesDevoirsByEleve(idDevoir,
                nbCompetenceNotes -> formate(nbCompetenceNotesDevoirsFuture, nbCompetenceNotes));

        CompositeFuture.all(nbAnnotationsDevoirsFuture, nbCompetenceNotesDevoirsFuture).setHandler(event -> {
            if(event.failed()){
                String error = event.cause().getMessage();
                log.info(error);
                leftToResponse(request, new Either.Left<>(error));
            } else {
                JsonArray resultNbAnnotationsDevoir = nbAnnotationsDevoirsFuture.result();
                if (resultNbAnnotationsDevoir.size() > 0) {
                    for (int i = 0; i < resultNbAnnotationsDevoir.size(); i++) {
                        JsonObject o = resultNbAnnotationsDevoir.getJsonObject(i);
                        boolean isNN = o.getString("type") != null && NN.equals(o.getString("type"));
                        // On ajoute le nombre d'annotations au nombre de notes pour déterminer le taux d'avancement
                        Float nbAnnotations = Float.valueOf(o.getInteger("nb_annotations"));
                        if (isNN && is_evaluated && devoirHasCompetence) {
                            nbAnnotations = nbAnnotations / 2;
                        }
                        if (!nbNotesByDevoir.containsKey(o.getLong("id"))) {
                            nbNotesByDevoir.put(o.getLong("id"), nbAnnotations);
                        } else {
                            Float nbNotesEtAnnotations = nbNotesByDevoir.get(o.getLong("id")) + nbAnnotations;
                            nbNotesByDevoir.put(o.getLong("id"), nbNotesEtAnnotations);
                        }
                    }
                }

                JsonArray resultNbCompetencesByStudents = nbCompetenceNotesDevoirsFuture.result();
                Float nbCompetences = Float.valueOf(0);
                for (int i = 0; i < resultNbCompetencesByStudents.size(); i++) {
                    JsonObject ob = resultNbCompetencesByStudents.getJsonObject(i);
                    /*
                    - Un élève est considéré comme évalué lorsqu'il a au moins une compétence d'évalué.
                    - Pour l'avancement les compétences comptent à moitié pour les devoirs contenant
                      une évaluation numérique
                    */
                    if (ob != null) {
                        if (ob.getInteger("nb_competences") >= 1 && is_evaluated) {
                            nbCompetences += Float.valueOf(1) / 2;
                        } else if (ob.getInteger("nb_competences") >= 1 && !is_evaluated) {
                            nbCompetences += 1;
                        }
                    }

                }

                // On ajoute le nombre de compétences au nombre d'annotations et de note pour déterminer le taux d'avancement
                if (!nbNotesByDevoir.containsKey(idDevoir)) {
                    nbNotesByDevoir.put(idDevoir, nbCompetences);
                } else {
                    Float nbNotesAnnotionsCompetences = nbNotesByDevoir.get(idDevoir) + nbCompetences;
                    nbNotesByDevoir.put(idDevoir, nbNotesAnnotionsCompetences);
                }

                final HashMap<String, Integer> nbElevesByGroupe = new HashMap<>();

                // Calcul du taux d'avancement en fonction du nombre d'élèves
                nbElevesByGroupe.put(idGroupe, nbStudents);

                final float nbElevesEvalues = nbNotesByDevoir.get(idDevoir);
                final Integer nbEleves = nbElevesByGroupe.get(idGroupe);

                int percent = 0;
                if (nbEleves != Integer.valueOf(0) && nbEleves != null) {
                    percent = (int) (nbElevesEvalues * 100 / nbEleves);
                }

                JsonObject result = new JsonObject();
                result.put("id", idDevoir);
                result.put("percent", percent);

                devoirsService.updatePercent(idDevoir, percent, updated -> {
                    if (updated.isRight()) {
                        Renders.renderJson(request, result);
                    } else {
                        String error = "[updatePercentWithAnnotationsAndCompetences] Update not done for devoirs.";
                        log.error(error);
                        leftToResponse(request, new Either.Left<>(error));
                    }
                });
            }
        });
    }

    Long testLongFormatParameter(String name, final HttpServerRequest request) {
        Long param = null;
        try {
            param = Long.parseLong(request.params().get(name));
        } catch(NumberFormatException e) {
            log.error("Error :" +  name + " must be a long object", e);
            badRequest(request, e.getMessage());
            return null;
        }
        return param;
    }

    @Put("/devoir/finish")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateEvaluationWorkflow.class)
    @ApiDoc("Permet de positionner une évaluation à 100% terminée même si des compétences ou des notes n'ont pas toutes été saisies")
    public void finishDevoir(final HttpServerRequest request) {
        try {
            Long idDevoir = Long.parseLong(request.params().get(Field.IDDEVOIR));
            devoirsService.updatePercent(idDevoir, 100, arrayResponseHandler(request));
        } catch (NumberFormatException err) {
            leftToResponse(request, new Either.Left<>(err.toString()));
        }
    }

    @Get("/devoirs/eleve")
    @ApiDoc("Pour l'appli mobile, permet de récupérer les derniers devoirs d'un élève")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessStudentParentTeacherPersonnelFilter.class)
    public void getDevoirsEleve(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                final String idEtablissement = request.params().get("idEtablissement");
                final String idEleve = request.params().get("idEleve");
                final String idMatiere = request.params().get("idMatiere");
                Long idPeriode = null;
                if (request.params().get("idPeriode") != null) {
                    idPeriode = testLongFormatParameter("idPeriode", request);
                }

                devoirsService.getDevoirsEleve(idEtablissement, idEleve, idMatiere, idPeriode, event -> {
                    if(event.isLeft()){
                        leftToResponse(request, new Either.Left<>(event.left().getValue()));
                    } else {
                        JsonObject result = event.right().getValue();
                        Renders.renderJson(request, result);
                    }
                });
            } else {
                unauthorized(request);
            }
        });
    }

    @Get("/devoirs/notes")
    @ApiDoc("Pour l'appli mobile, permet de récupérer l'ensemble des notes d'un élève pour une période")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessStudentParentTeacherPersonnelFilter.class)
    public void getDevoirsNotes(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if(user != null){
                final String idEtablissement = request.params().get("idEtablissement");
                final String idEleve = request.params().get("idEleve");

                if (idEtablissement != null && idEleve != null) {
                    Long idPeriode = null;
                    if (request.params().get("idPeriode") != null) {
                        idPeriode = testLongFormatParameter("idPeriode", request);
                    }
                    devoirsService.getDevoirsNotes(idEtablissement, idEleve, idPeriode, event -> {
                        if(event.isLeft()){
                            leftToResponse(request, new Either.Left<>(event.left().getValue()));
                        } else {
                            JsonObject result = event.right().getValue();
                            Renders.renderJson(request, Utils.sortJsonObjectIntValue("matiere_rank", result));
                        }
                    });
                }
            } else {
                unauthorized(request);
            }
        });
    }
}