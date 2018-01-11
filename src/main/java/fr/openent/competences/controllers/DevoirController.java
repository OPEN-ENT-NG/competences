/*
 * Copyright (c) Région Hauts-de-France, Département 77, CGI, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.security.AccessEvaluationFilter;
import fr.openent.competences.security.AccessPeriodeFilter;
import fr.openent.competences.security.AccessVisibilityAppreciation;
import fr.openent.competences.service.CompetencesService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultCompetencesService;
import fr.openent.competences.service.impl.DefaultDevoirService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.*;

/**
 * Created by ledunoiss on 04/08/2016.
 */
public class DevoirController extends ControllerHelper {

    /**
     * Déclaration des services
     */
    private final DefaultDevoirService devoirsService;
    private final UtilsService utilsService;
    private final CompetencesService competencesService;

    public DevoirController() {
        devoirsService = new DefaultDevoirService(Competences.COMPETENCES_SCHEMA, Competences.DEVOIR_TABLE);
        utilsService = new DefaultUtilsService();
        competencesService = new DefaultCompetencesService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_TABLE);
    }

    @Get("/devoirs")
    @ApiDoc("Récupère les devoirs d'un utilisateurs")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void view(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    final String _PERSONNEL = "Personnel";
                    if(_PERSONNEL.equals(user.getType()) && user.getFunctions().containsKey("DIR")){
                        final Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                        devoirsService.listDevoirsEtab(user, handler);
                    }
                    else{
                        final Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);
                        if (request.params().size() == 2) {
                            String idEtablissement = request.params().get("idEtablissement");
                            devoirsService.listDevoirs(user,idEtablissement, handler);
                        } else {
                            String idEtablissement = request.params().get("idEtablissement");
                            String idClasse = request.params().get("idClasse");
                            String idMatiere = request.params().get("idMatiere");

                            final String _STUDENT = "Student";
                            final String _RELATIVE = "Relative";
                            Long idPeriode = null;
                            if (request.params().get("idPeriode") != null) {
                               idPeriode = testLongFormatParameter("idPeriode", request);
                            }

                            if( _STUDENT.equals(user.getType()) || _RELATIVE.equals(user.getType())){
                                String idEleve = request.params().get("idEleve");
                                devoirsService.listDevoirs(idEleve,idEtablissement, idClasse, null, idPeriode, handler);

                            } else if (idEtablissement != "undefined" && idClasse != "undefined"
                                    && idMatiere != "undefined" && request.params().get("idPeriode") != "undefined") {
                                devoirsService.listDevoirs(null,idEtablissement, idClasse, idMatiere, idPeriode, handler);
                            } else {
                                Renders.badRequest(request, "Invalid parameters");
                            }
                        }
                    }

                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Créer un devoir avec les paramètres passés en post.
     * @param request
     */
    @Post("/devoir")
    @ApiDoc("Créer un devoir")
    @SecuredAction("viescolaire.evaluations.createEvaluation")
    public void create(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                        @Override
                        public void handle(final JsonObject resource) {
                            resource.removeField("competences");
                            resource.removeField("competencesAdd");
                            resource.removeField("competencesRem");
                            resource.removeField("competenceEvaluee");

                            RequestUtils.bodyToJson(request, pathPrefix +
                                    Competences.SCHEMA_DEVOIRS_CREATE, new Handler<JsonObject>() {
                                @Override
                                public void handle(final JsonObject devoir) {

                                    devoirsService.createDevoir(devoir, user, new Handler<Either<String, JsonObject>>() {
                                        @Override
                                        public void handle(Either<String, JsonObject> event) {
                                            if (event.isRight()) {
                                                final JsonObject devoirWithId = event.right().getValue();
                                                // recuperation des professeurs que l'utilisateur connecté remplacent
                                                utilsService.getTitulaires(user.getUserId(), devoir.getString("id_etablissement"), new Handler<Either<String, JsonArray>>() {
                                                    @Override
                                                    public void handle(Either<String, JsonArray> event) {
                                                        if (event.isRight()) {
                                                            // si l'utilisateur connecté remplace bien un professeur
                                                            // on partage à ce professeur (le titulaire) le devoir
                                                            JsonArray values = event.right().getValue();

                                                            if(values.size() > 0) {

                                                                // TODO potentielement il peut y avoir plusieurs titulaires pour un remplaçant sur le même établissement
                                                                String userIdTitulaire = ((JsonObject)values.get(0)).getString("id_titulaire");
                                                                List<String> actions = new ArrayList<String>();
                                                                actions.add(Competences.DEVOIR_ACTION_UPDATE);

                                                                // TODO ne partager le devoir seulement si le titulaire enseigne sur la classe du remplaçant
                                                                shareService.userShare(user.getUserId(), userIdTitulaire, devoirWithId.getLong("id").toString(), actions, new Handler<Either<String, JsonObject>>() {
                                                                    @Override
                                                                    public void handle(Either<String, JsonObject> event) {
                                                                        if (event.isRight()) {
                                                                            renderJson(request, devoirWithId);
                                                                        } else {
                                                                            leftToResponse(request, event.left());
                                                                        }

                                                                    }
                                                                });
                                                            } else {
                                                                // sinon on renvoie la réponse, pas besoin de partage
                                                                renderJson(request, devoirWithId);
                                                            }
                                                        }else {
                                                            leftToResponse(request, event.left());
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
                    });
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
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
                List<String> idDevoirsList = request.params().getAll("idDevoir");
                final HashMap<Long, Integer> nbCompetencesByDevoir = new HashMap<>();
                Long[] idDevoirsArray = new Long[idDevoirsList.size()];

                for (int i = 0; i < idDevoirsList.size(); i++) {
                    idDevoirsArray[i] = Long.valueOf(idDevoirsList.get(i));
                }
                // On recherche le Nonbre de compétences sur le devoir à mettre à jour
                devoirsService.getNbCompetencesDevoirs(idDevoirsArray, new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            if (event.right().getValue() != null) {
                                JsonArray resultNbCompetencesDevoirs = event.right().getValue();

                                for (int i = 0; i < resultNbCompetencesDevoirs.size(); i++) {
                                    JsonObject o = resultNbCompetencesDevoirs.get(i);

                                    if (o != null) {
                                        nbCompetencesByDevoir.put(o.getLong("id"),
                                                o.getInteger("nb_competences"));
                                    }
                                }

                                // On limite le nbre de compétence d' un devoir
                                if ((devoir.containsField("competencesAdd")
                                        && devoir.containsField("competencesRem"))

                                        && ((nbCompetencesByDevoir.get(Long.valueOf(request.params().get("idDevoir")))
                                        + devoir.getArray("competencesAdd").size()
                                        - devoir.getArray("competencesRem").size())
                                        <= Competences.MAX_NBR_COMPETENCE)) {
                                    devoirsService.updateDevoir(request.params().get("idDevoir"),
                                            devoir, arrayResponseHandler(request));

                                }
                                else{
                                    leftToResponse(request, event.left());
                                }
                            }
                            else {
                                leftToResponse(request, event.left());
                            }
                        }
                        else {
                            leftToResponse(request, event.left());
                        }
                    }
                });
            }
        });
    }

    @Get("/devoirs/done")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @ApiDoc("Calcul le pourcentage réalisé pour un devoir")
    public void getPercentDone (final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null) {

                    final HashMap<Long, Float> nbNotesByDevoir = new HashMap<>();

                    // Paramètres d'entrée
                    final Long idDevoir = testLongFormatParameter("idDevoir",request);
                    final String is_evaluated = request.params().get("is_evaluated");
                    final String has_competence = request.params().get("has_competence");
                    final String idGroupe = request.params().get("idGroupe");


                    /*
                     - Si le devoir contient une evaluation numérique, on regarde le nombre de note en base
                     - Sinon, on regarde directement le nombre d'annotation(s) et de compétence(s)
                     */
                    if (String.valueOf(true).equals(is_evaluated)){
                        updatePercentageWithNotes (idDevoir, user,idGroupe,nbNotesByDevoir,is_evaluated,
                                request, has_competence, true, 0 ,0);

                    }
                    else {
                        updatePercentWithAnnotationsAndCompetences(idDevoir, user, idGroupe,
                                nbNotesByDevoir, is_evaluated, request,true ,0 ,0);
                    }
                }
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
                    devoirsService.delete(request.params().get("idDevoir"),user, notEmptyResponseHandler(request));
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
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null) {
                    Long idDevoir = Long.parseLong(request.params().get("idDevoir"));
                    boolean stats = Boolean.parseBoolean(request.params().get("stats"));
                    devoirsService.getMoyenne(idDevoir, stats, new Handler<Either<String, JsonObject>>() {

                        @Override
                        public void handle(Either<String, JsonObject> event) {
                            if(event.isRight()) {
                                Renders.renderJson(request, event.right().getValue());
                            } else {
                                leftToResponse(request, event.left());
                            }
                        }
                    });
                }
            }
        });
    }

    @Post("/devoir/:idDevoir/duplicate")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessEvaluationFilter.class)
    @ApiDoc("Duplique un devoir pour une liste de classes donnée")
    public void duplicateDevoir (final HttpServerRequest request) {
        if (!request.params().contains("idDevoir")) {
            badRequest(request);
        } else {
            UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                @Override
                public void handle(final UserInfos user) {
                    RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                        @Override
                        public void handle(final JsonObject body) {
                            try {
                                final Long idDevoir = Long.parseLong(request.params().get("idDevoir"));
                                devoirsService.retrieve(idDevoir.toString(), new Handler<Either<String, JsonObject>>() {
                                    @Override
                                    public void handle(Either<String, JsonObject> result) {
                                        if (result.isRight()) {
                                            final JsonObject devoir = result.right().getValue();
                                            competencesService.getDevoirCompetences(idDevoir,new Handler<Either<String, JsonArray>>() {
                                                @Override
                                                public void handle(Either<String, JsonArray> result) {
                                                    if (result.isRight()) {
                                                        JsonArray competences = result.right().getValue();
                                                        if (competences.size() > 0) {
                                                            JsonArray idCompetences = new JsonArray();
                                                            JsonObject o;
                                                            for (int i = 0; i < competences.size(); i++) {
                                                                o = competences.get(i);
                                                                if (o.containsField("id")) {
                                                                    idCompetences.addNumber(o.getNumber("id_competence"));
                                                                }
                                                            }
                                                            devoir.putArray("competences", idCompetences);
                                                        }
                                                        devoirsService.duplicateDevoir(idDevoir, devoir, body.getArray("classes"), user, arrayResponseHandler(request));
                                                    } else {
                                                        log.error("An error occured when collecting competences for devoir id " + idDevoir);
                                                        renderError(request);
                                                    }
                                                }
                                            });
                                        } else {
                                            log.error("An error occured when collecting devoir data for id " + idDevoir);
                                            renderError(request);
                                        }
                                    }
                                });
                            } catch (ClassCastException e) {
                                log.error("idDevoir parameter must be a long object.");
                                log.error(e);
                                renderError(request);
                            }
                        }
                    });
                }
            });
        }
    }

    // Methode permettant de calculer le nombre de note(s)
    private void updatePercentageWithNotes (final long idDevoir, final UserInfos user,
                                            final String  idGroupe,
                                            final HashMap<Long, Float> nbNotesByDevoir,
                                            final String is_evaluated, final HttpServerRequest request,
                                            final String has_competence, final boolean returning,
                                            final int currentThread, final int number){
        devoirsService.getNbNotesDevoirs(user, idDevoir , new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    if (event.right().getValue() != null) {
                        JsonArray resultNbNotesDevoir = event.right().getValue();

                        if (resultNbNotesDevoir.size() > 0) {
                            JsonObject o = resultNbNotesDevoir.get(0);
                            if (o != null) {
                                if (String.valueOf(true).equals(has_competence)) {
                                    nbNotesByDevoir.put(o.getLong("id"),
                                            Float.valueOf(o.getInteger("nb_notes")) / 2);
                                } else {
                                    nbNotesByDevoir.put(o.getLong("id"),
                                            Float.valueOf(o.getInteger("nb_notes")));
                                }
                            }
                        }
                        updatePercentWithAnnotationsAndCompetences(idDevoir, user, idGroupe,
                                nbNotesByDevoir, is_evaluated, request, returning, currentThread,number);
                    }
                } else {
                    leftToResponse(request, event.left());
                }
            }

        });
    }

    // Methode permettant de calculer le nombre d'annotation(s) et de compétence(s)
    void updatePercentWithAnnotationsAndCompetences(final long idDevoir, final UserInfos user,
                                                    final String  idGroupe,
                                                    final HashMap<Long, Float> nbNotesByDevoir,
                                                    final String is_evaluated, final HttpServerRequest request ,
                                                    final boolean returning, final int currentThread, final int number){

        final JsonArray idGroupes = new JsonArray().add(idGroupe);
        final JsonArray result = new JsonArray();
        final HashMap<String, Integer> nbElevesByGroupe = new HashMap<>();

        // On récupère le nombre d'annotations
        devoirsService.getNbAnnotationsDevoirs(user, idDevoir , new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray resultNbAnnotationsDevoir = event.right().getValue();

                    if( resultNbAnnotationsDevoir.size() > 0) {
                        JsonObject o = resultNbAnnotationsDevoir.get(0);

                        if (o != null) {
                            // On ajoute le nombre d'annotations au nombre de notes pour déterminer le taux d'avancement
                            if (!nbNotesByDevoir.containsKey(o.getLong("id"))) {
                                nbNotesByDevoir.put(o.getLong("id"), Float.valueOf(o.getInteger("nb_annotations")));
                            } else {
                                Float nbNotesEtAnnotations = nbNotesByDevoir.get(o.getLong("id"))
                                        + o.getInteger("nb_annotations");
                                nbNotesByDevoir.put(o.getLong("id"), nbNotesEtAnnotations);
                            }
                        }
                    }
                    // On récupère le nombre de compétences par élèves pour le devoir courant
                    devoirsService.getNbCompetencesDevoirsByEleve(idDevoir, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                JsonArray resultNbCompetencesByStudents = event.right().getValue();
                                Float nbCompetences = Float.valueOf(0);
                                for (int i = 0; i < resultNbCompetencesByStudents.size(); i++) {
                                    JsonObject o = resultNbCompetencesByStudents.get(i);
                                    /*
                                    - Un élève est considéré comme évalué lorsqu'il a au moins une compétence d'évalué.
                                    - Pour l'avancement les compétences comptent à moitié pour les devoirs contenant
                                      une évaluation numérique
                                    */
                                    if (o != null) {
                                        if (o.getInteger("nb_competences") >= 1
                                                && String.valueOf(true).equals(is_evaluated)) {
                                            nbCompetences += Float.valueOf(1)/2;
                                        }
                                        else if (o.getInteger("nb_competences") >= 1
                                                && String.valueOf(false).equals(is_evaluated)) {
                                            nbCompetences += 1;
                                        }
                                    }

                                }
                                // On ajoute le nombre de compétences au nombre d'annotations et de notes
                                // pour déterminer le taux d'avancement
                                if (!nbNotesByDevoir.containsKey(idDevoir)) {
                                    nbNotesByDevoir.put(idDevoir, nbCompetences);
                                } else {

                                    Float nbNotesAnnotionsCompetences = nbNotesByDevoir.get(idDevoir)
                                            + nbCompetences;
                                    nbNotesByDevoir.put(idDevoir, nbNotesAnnotionsCompetences);
                                }
                                // Calcul du taux d'avancement en fonction du nombre d'élève(s)
                                JsonObject action = new JsonObject()
                                        .putString("action", "classe.getNbElevesGroupe")
                                        .putArray("idGroups", idGroupes);
                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                    @Override
                                    public void handle(Message<JsonObject> res) {
                                        JsonObject body = res.body();
                                        if ("ok".equals(body.getString("status"))) {
                                            JsonArray resultNbElevesGroupes = body.getArray("results");
                                            if( resultNbElevesGroupes.size() > 0) {
                                                JsonObject nbEleves = resultNbElevesGroupes.get(0);
                                                nbElevesByGroupe.put(
                                                        nbEleves.getString("id_groupe"),
                                                        nbEleves.getInteger("nb"));
                                            }

                                            JsonObject o = new JsonObject();

                                            if( nbElevesByGroupe.get(idGroupe) != Integer.valueOf(0)) {
                                                final float nbElevesEvalues = nbNotesByDevoir.get(idDevoir);
                                                final Integer nbEleves = nbElevesByGroupe.get(idGroupe);

                                                int _percent = 0;
                                                if(nbEleves != Integer.valueOf(0) && nbEleves != null){
                                                    _percent = (int) (nbElevesEvalues*100/ nbEleves);
                                                }

                                                final Integer percent = _percent;

                                                o.putNumber("id", idDevoir);
                                                o.putNumber("percent", percent);
                                                result.add(o);

                                                devoirsService.updatePercent(idDevoir, percent, new
                                                        Handler<Either<String, JsonArray>>() {
                                                            @Override
                                                            public void handle(Either<String, JsonArray> event){
                                                                if (event.isRight()) {
                                                                    if (returning) {
                                                                        Renders.renderJson(request, result);
                                                                    } else if (number == currentThread) {
                                                                        JsonObject res = new JsonObject();
                                                                        int nbrs = number + 1;
                                                                        res.putNumber("nbUpdatedDevoirs",nbrs);
                                                                        Renders.renderJson(request, res);
                                                                        log.info(" FIN : " + nbrs +
                                                                                " devoir(s) mis à jour");
                                                                    }
                                                                }
                                                            }
                                                        });
                                            }
                                            else {
                                                log.info(" No students for class or group: " + idGroupe);

                                            }
                                        } else {
                                            leftToResponse(request,new Either.Left<String, JsonArray>(body.getString("message")));
                                        }
                                    }
                                });
                            } else {
                                leftToResponse(request, event.left());
                            }
                        }
                    });
                } else {
                    leftToResponse(request, event.left());
                }
            }
        });
    }

    Long testLongFormatParameter(String name,final HttpServerRequest request) {
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


    @Get("/devoirs/updatedone")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Met à jour le pourcentage réalisé pour chaque devoir")
    public void updatePercentDone (final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    final HashMap<Long, Float> nbNotesByDevoir = new HashMap<>();

                    List<String> idDevoirsList = request.params().getAll("id");


                    Long[] idDevoirsArray =  null;

                    if(!idDevoirsList.isEmpty()) {
                        idDevoirsArray =  new Long[idDevoirsList.size()];
                        for (int i = 0; i < idDevoirsList.size(); i++) {
                            idDevoirsArray[i] = Long.valueOf(idDevoirsList.get(i));

                        }
                        log.info(" MAJ du taux de complétude pour "+ idDevoirsList.size() + " devoir(s).");

                    }
                    else {
                        log.info("MAJ du taux de complétude pour tous les devoirs.");
                    }

                    devoirsService.getDevoirsInfos(idDevoirsArray, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                JsonArray devoirsInfos = event.right().getValue();
                                log.info(" Récupération de  "+ devoirsInfos.size()+ " devoir(s).");
                                log.info("Devoir  |  Completude ");
                                for (int i = 0; i < devoirsInfos.size(); i++) {
                                    JsonObject o = devoirsInfos.get(i);
                                    if (o != null) {
                                        updatePercentageWithNotes (Long.valueOf(o.getInteger("id")),
                                                null,o.getString("id_groupe"),
                                                nbNotesByDevoir,String.valueOf(o.getBoolean("is_evaluated")),request,
                                                String.valueOf(o.getBoolean("has_competence")), false, i,
                                                devoirsInfos.size()-1);
                                    }

                                }

                            }

                        }
                    });

                }
            }
        });
    }

}