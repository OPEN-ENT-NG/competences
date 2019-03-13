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
import fr.openent.competences.bean.StatClass;
import fr.openent.competences.bean.StatMat;
import fr.openent.competences.security.AccessEvaluationFilter;
import fr.openent.competences.security.AccessNoteFilter;
import fr.openent.competences.security.AccessReleveFilter;
import fr.openent.competences.security.CreateEvaluationWorkflow;
import fr.openent.competences.security.utils.FilterUser;
import fr.openent.competences.security.utils.FilterPeriodeUtils;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.service.ElementProgramme;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.*;
import fr.openent.competences.utils.FormateFutureEvent;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.getLibellePeriode;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

import java.util.*;
import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.*;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class NoteController extends ControllerHelper {

    /**
     * Déclaration des services
     */
    private final NoteService notesService;
    private final UtilsService utilsService;
    private final ElementProgramme elementProgramme;

    public NoteController(EventBus eb) {
        this.eb = eb;
        notesService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE,eb);
        utilsService = new DefaultUtilsService();
        elementProgramme = new DefaultElementProgramme();
    }

    /**
     * Recupère les notes d'un devoir donné
     *
     * @param request
     */
    @Get("/devoir/:idDevoir/notes")
    @ApiDoc("Recupère les notes pour un devoir donné")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessEvaluationFilter.class)
    public void view(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if (user != null) {
                    Handler<Either<String, JsonArray>> handler = arrayResponseHandler(request);

                    Long idDevoir;
                    try {
                        idDevoir = Long.parseLong(request.params().get("idDevoir"));
                    } catch (NumberFormatException e) {
                        log.error(" Error : idDevoir must be a long object", e);
                        badRequest(request, e.getMessage());
                        return;
                    }

                    notesService.listNotesParDevoir(idDevoir, handler);
                } else {
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Créer une note avec les données passées en POST
     *
     * @param request
     */
    @Post("/note")
    @ApiDoc("Créer une note")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateEvaluationWorkflow.class)
    public void create(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_NOTES_CREATE, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            notesService.createNote(resource, user, notEmptyResponseHandler(request));
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
     * Modifie une note avec les données passées en PUT
     *
     * @param request
     */
    @Put("/note")
    @ApiDoc("Modifie une note")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessNoteFilter.class)
    public void update(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_NOTES_UPDATE, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            notesService.updateNote(resource, user, defaultResponseHandler(request));
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
     * Supprime la note passé en paramètre
     *
     * @param request
     */
    @Delete("/note")
    @ApiDoc("Supprimer une note donnée")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessNoteFilter.class)
    public void deleteNoteDevoir(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {

                    Long idNote;
                    try {
                        idNote = Long.parseLong(request.params().get("idNote"));
                    } catch (NumberFormatException e) {
                        log.error("Error : idNote must be a long object", e);
                        badRequest(request, e.getMessage());
                        return;
                    }

                    notesService.deleteNote(idNote, user, defaultResponseHandler(request));
                } else {
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Récupère les notes pour le widget
     *
     * @param request
     */
    @Get("/widget")
    @ApiDoc("Récupère les notes pour le widget")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getWidgetNotes(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if (user != null) {
                    notesService.getWidgetNotes(request.params().get("userId"), arrayResponseHandler(request));
                }
            }
        });
    }

    /**
     * Récupère les notes pour le relevé de notes
     *
     * @param request
     */
    @Get("/releve")
    @ApiDoc("Récupère les notes, les moyennes finales pour le relevé de notes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void getNoteElevePeriode(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

            @Override
            public void handle(UserInfos user) {

                final String idEleve = request.params().get(ID_ELEVE_KEY);
                final String idEtablissement = request.params().get(ID_ETABLISSEMENT_KEY);
                final String idClasse = request.params().get(ID_CLASSE_KEY);
                final String idMatiere = request.params().get(ID_MATIERE_KEY);
                final String idPeriodeString = request.params().get(ID_PERIODE_KEY);
                final Integer typeClasse = Integer.valueOf(request.params().get(TYPE_CLASSE_KEY));


                new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere, false,
                        new Handler<Boolean>() {
                            @Override
                            public void handle(final Boolean hasAccessToMatiere) {

                                Long idPeriode = null;
                                if (!hasAccessToMatiere) {
                                    unauthorized(request);
                                } else {
                                    if (idPeriodeString != null) {
                                        try {
                                            idPeriode = Long.parseLong(idPeriodeString);
                                        } catch (NumberFormatException e) {
                                            log.error("Error : idPeriode must be a long object", e);
                                            badRequest(request, e.getMessage());
                                            return;
                                        }
                                    }
                                        JsonObject params = new JsonObject().put(ID_ELEVE_KEY, idEleve)
                                                .put(ID_CLASSE_KEY, idClasse)
                                                .put(ID_PERIODE_KEY, idPeriode)
                                                .put(ID_ETABLISSEMENT_KEY, idEtablissement)
                                                .put(ID_MATIERE_KEY, idMatiere)
                                                .put(TYPE_CLASSE_KEY, typeClasse);

                                        notesService.getDatasReleve(params, notEmptyResponseHandler(request));

                                }
                            }
                        });
            }
        });
    }

    @Post("/releve/export")
    @ApiDoc("Exporte un relevé périodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void exportRelevePeriodique(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, param -> {
            if(param.getString("fileType").equals("pdf")) {

               notesService.exportPDFRelevePeriodique(param,  request, vertx, config);
            }
            else {
                notesService.getDatasReleve(param, notEmptyResponseHandler(request));
            }
        });
    }

    @Get("/eleve/:idEleve/moyenne")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @ApiDoc("Retourne la moyenne de l'élève dont l'id est passé en paramètre, sur les devoirs passés en paramètre")
    public void getMoyenneEleve(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    List<String> idDevoirsList = request.params().getAll("devoirs");
                    String idEleve = request.params().get("idEleve");

                    Long[] idDevoirsArray = new Long[idDevoirsList.size()];

                    for (int i = 0; i < idDevoirsList.size(); i++) {
                        idDevoirsArray[i] = Long.parseLong(idDevoirsList.get(i));
                    }

                    notesService.getNotesParElevesParDevoirs(new String[]{idEleve}, idDevoirsArray, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                JsonArray notesEleve = event.right().getValue();
                                List<NoteDevoir> notes = new ArrayList<>();

                                for (int i = 0; i < notesEleve.size(); i++) {
                                    JsonObject note = notesEleve.getJsonObject(i);
                                    notes.add(new NoteDevoir(Double.parseDouble(note.getString("valeur")),
                                            Double.parseDouble(note.getInteger("diviseur").toString()),
                                            note.getBoolean("ramener_sur"),
                                            Double.parseDouble(note.getString("coefficient"))));
                                }
                                Renders.renderJson(request, utilsService.calculMoyenne(notes, false, 20));
                            } else {
                                renderError(request, new JsonObject().put("error", event.left().getValue()));
                            }
                        }
                    });
                }
            }
        });
    }

    // méthode permettant de récupérer les éléments du programme, les moyennes finales et les appréciations pour:
    // - une classe
    // - une matière
    // - une période
    // le résultat de la récupération est greffé à la réponse de la request
    private void addMoyenneFinalAndAppreciation(final HttpServerRequest request, final JsonObject res) {
        final String idClasse = request.params().get("idClasse");
        final String idMatiere = request.params().get("idMatiere");
        final String idPeriodeString = request.params().get("idPeriode");

        final JsonObject action = new JsonObject()
                .put("action", "classe.getElevesClasses")
                .put("idPeriode", Long.valueOf(idPeriodeString))
                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray().add(idClasse));

        if (idPeriodeString != null) {
            try {
                final long idPeriode = Long.parseLong(idPeriodeString);
                elementProgramme.getElementProgramme(idPeriode,idMatiere,idClasse,new Handler<Either<String, JsonObject>>() {
                    @Override
                    public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                        if (stringJsonObjectEither.isRight()) {

                            res.put("elementProgramme",stringJsonObjectEither.right().getValue());

                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> message) {
                                    JsonObject body = message.body();

                                    if ("ok".equals(body.getString("status"))) {
                                        Map<String, List<String>> result = new LinkedHashMap<>();
                                        final JsonArray idEleves = new fr.wseduc.webutils.collections.JsonArray();
                                        JsonArray queryResult = body.getJsonArray("results");

                                        for (int i = 0; i < queryResult.size(); i++) {
                                            JsonObject eleve = queryResult.getJsonObject(i);
                                            idEleves.add(eleve.getString("idEleve"));
                                        }
                                        notesService.getColonneReleve(
                                                idEleves,
                                                idPeriode,
                                                idMatiere,
                                                new JsonArray().add(idClasse),
                                                "moyenne",
                                                new Handler<Either<String, JsonArray>>() {
                                                    @Override
                                                    public void handle(Either<String, JsonArray> event) {
                                                        if (event.isRight()) {
                                                            res.put("moyennes",
                                                                    event.right().getValue());
                                                            addAppreciationsElevesPeriodeMatiere(request, idEleves,
                                                                    idPeriodeString, idMatiere,
                                                                    idClasse, res);

                                                        } else {
                                                            JsonObject error = new JsonObject()
                                                                    .put("error",
                                                                            event.left().getValue());
                                                            Renders.renderJson(request, error, 400);
                                                        }
                                                    }
                                                });
                                    } else {
                                        log.error("getRelevePeriodique " + body.getString("message"));
                                        JsonObject error = (new JsonObject()).put("error",
                                                "failed get Moyenne Finale");
                                        Renders.renderJson(request, error, 400);
                                    }
                                }
                            }));
                        }
                    }
                });
            } catch (NumberFormatException e) {
                log.error("Error : idPeriode  must be a long object ", e);
                badRequest(request, e.getMessage());
                return;
            }
        }
    }

    // Méthode permettant de récupérer les appréciation d'un ensemble d'élève et de greffer le résultat
    // à la réponse de la request
    private void addAppreciationsElevesPeriodeMatiere(final HttpServerRequest request, JsonArray idEleves,
                                                      String idPeriodeString,
                                                      String idMatiere, String idClasse, final JsonObject res) {
        Long idPeriode = Long.parseLong(idPeriodeString);
        notesService.getColonneReleve(
                idEleves,
                idPeriode,
                idMatiere,
                new JsonArray().add(idClasse),
                "appreciation_matiere_periode",
                new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            Renders.renderJson(request, res.put("appreciations",
                                    event.right().getValue()));
                        } else {
                            JsonObject error = new JsonObject()
                                    .put("error", event.left().getValue());
                            Renders.renderJson(request, error, 400);
                        }
                    }
                });
    }




    @Post("/releve/element/programme")
    @ApiDoc("Ajoute ou modifie un élément du programme")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void setElementProgramme(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(final JsonObject resource) {
                        final String idClasse = resource.getString("idClasse");
                        final String idMatiere = resource.getString("idMatiere");
                        final Long idPeriode = resource.getLong("idPeriode");
                        final String texte = resource.getString("texte");
                        final String idEtablissement = resource.getString("idEtablissement");
                        // Vérification de l'accès à la matière
                        new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere,false,
                                new Handler<Boolean>() {
                                    @Override
                                    public void handle(final Boolean hasAccessToMatiere) {
                                        if (hasAccessToMatiere) {
                                            // Vérification de la date de fin de saisie
                                            new FilterPeriodeUtils(eb, user).validateEndSaisie(request,
                                                    idClasse, idPeriode.intValue(),
                                                    new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean isUpdatable) {
                                                            //verif date fin de saisie
                                                            if (isUpdatable) {
                                                                elementProgramme.setElementProgramme(
                                                                        user.getUserId(),
                                                                        idPeriode,
                                                                        idMatiere,
                                                                        idClasse,
                                                                        texte,
                                                                        arrayResponseHandler(request));
                                                            }
                                                            else {
                                                                log.error("Not access to API because of end of saisie");
                                                                unauthorized(request);
                                                            }
                                                        }
                                                    });
                                        } else {
                                            log.error("Not access to Matiere");
                                            unauthorized(request);
                                        }
                                    }
                                });
                    }
                });

            }
        });
    }

    @Post("/releve/periodique")
    @ApiDoc("Créé, met à jour ou supprime une donnée du relevé périodique pour un élève. Les données traitées ici sont:"
            +" - moyenne finale, - appréciation, -positionnement ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void setColonneRelevePeriode(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {

                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(final JsonObject resource) {
                        final String idClasse = resource.getString("idClasse");

                        saveColonneRelevePeriode(request, user, resource);
                    }
                });
            }
        });
    }

    @Post("/bilan/periodique")
    @ApiDoc("Créé, met à jour ou supprime une donnée du relevé périodique pour un élève. Les données traitées ici sont:"
            +" - moyenne finale, - appréciation, -positionnement ")
    @SecuredAction(value="bilan.periodique.save.appMatiere.positionnement",type = ActionType.WORKFLOW)
    public void saveAppreciationMatiereAndPositionnement(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {

                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(final JsonObject resource) {
                        final String idClasse = resource.getString("idClasse");
                        FilterUser.isChefEtabAndHeadTeacher(user, new fr.wseduc.webutils.collections.JsonArray().add(idClasse), new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean isChefEtabAndHeadTeacher) {
                                if(isChefEtabAndHeadTeacher){
                                    saveColonneRelevePeriode(request, user, resource);
                                }else{
                                    Renders.unauthorized(request);
                                }

                            }
                        });

                    }
                });
            }
        });
    }




    private void saveColonneRelevePeriode (final HttpServerRequest request, final UserInfos user, final JsonObject resource){

        final String idClasse = resource.getString("idClasse");
        final String idMatiere = resource.getString("idMatiere");
        final String idEleve = resource.getString("idEleve");
        final String table = resource.getString("colonne");
        final Long idPeriode = resource.getLong("idPeriode");
        final String idEtablissement = resource.getString("idEtablissement");
        final Boolean isBilanPeriodique = (resource.getBoolean("isBilanPeriodique")!=null)?
                resource.getBoolean("isBilanPeriodique") : false;

        // Vérification de l'accès à la matière
        new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere,isBilanPeriodique,
                new Handler<Boolean>() {
                    @Override
                    public void handle(final Boolean hasAccessToMatiere) {
                        if (hasAccessToMatiere) {
                            // Vérification de la date de fin de saisie
                            new FilterPeriodeUtils(eb, user).validateEndSaisie(request,
                                    idClasse, idPeriode.intValue(),
                                    new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean isUpdatable) {
                                            //verif date fin de saisie
                                            if (isUpdatable) {

                                                if (resource.getBoolean("delete")) {
                                                    notesService.deleteColonneReleve(
                                                            idEleve,
                                                            idPeriode,
                                                            idMatiere,
                                                            idClasse,
                                                            table,
                                                            arrayResponseHandler(request));
                                                } else {
                                                    notesService.setColonneReleve(
                                                            idEleve,
                                                            idPeriode,
                                                            idMatiere,
                                                            idClasse,
                                                            resource,
                                                            table,
                                                            arrayResponseHandler(request));
                                                }
                                            } else {
                                                log.error("Not access to API because of end of saisie");
                                                unauthorized(request);
                                            }
                                        }
                                    });
                        } else {
                            log.error("Not access to Matiere");
                            unauthorized(request);
                        }
                    }
                });
    }


    @Get("/releve/informations/eleve/:idEleve")
    @ApiDoc("Renvoit  les moyennes , les moyennes finales pour le relevé de notes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void getInfosEleve(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

            @Override
            public void handle(UserInfos user) {
                final String idEtablissement = request.params().get("idEtablissement");
                final String idClasse = request.params().get("idClasse");
                final String idMatiere = request.params().get("idMatiere");
                final String idEleve = request.params().get("idEleve");
                final Integer typeClasse = null;

                new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere, false,
                        new Handler<Boolean>() {
                            @Override
                            public void handle(final Boolean hasAccessToMatiere) {

                                Handler<Either<String, JsonArray>> handler = new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {

                                            final JsonObject result = new JsonObject();
                                            JsonArray idEleves = new fr.wseduc.webutils.collections.JsonArray();
                                            HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse =
                                                    notesService.calculMoyennesEleveByPeriode(event.right().getValue(), result, idEleve, idEleves);

                                            addMoyenneFinalAndAppreciationPositionnementEleve(
                                                    idEleve, idClasse,
                                                    idMatiere, idEtablissement,
                                                    request,
                                                    result, idEleves, notesByDevoirByPeriodeClasse);
                                        } else {
                                            JsonObject error = (new JsonObject()).put("error",
                                                    (String) event.left().getValue());
                                            Renders.renderJson(request, error, 400);
                                        }
                                    }
                                };
                                if (!hasAccessToMatiere) {
                                    unauthorized(request);
                                } else {
                                    notesService.getNoteElevePeriode(null,
                                            idEtablissement,
                                            new fr.wseduc.webutils.collections.JsonArray().add(idClasse),
                                            idMatiere,
                                            null, handler);
                                }
                            }
                        });
            }
        });
    }


    @Get("/releve/annee/classe")
    @ApiDoc("Renvoit  les moyennes , les moyennes finales pour le relevé de notes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void getReleveAnne(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

            @Override
            public void handle(UserInfos user) {
                final String idEtablissement = request.params().get("idEtablissement");
                final List<String> classes = request.params().getAll("idClasse");
                final String idClasse = classes.toArray(new String[0])[0];
                final String idMatiere = request.params().get("idMatiere");

                final JsonObject action = new JsonObject()
                        .put("action", "classe.getElevesClasses")
                        .put("idClasses", new fr.wseduc.webutils.collections.JsonArray().add(idClasse));

                new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere, false,
                        new Handler<Boolean>() {
                            @Override
                            public void handle(final Boolean hasAccessToMatiere) {

                                Handler<Either<String, JsonArray>> handler = new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            final JsonObject result = new JsonObject();
                                            final JsonArray listNotes = event.right().getValue();

                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> message) {
                                                    JsonObject body = message.body();

                                                    if ("ok".equals(body.getString("status"))) {
                                                        final JsonObject result = new JsonObject();
                                                        final JsonArray idEleves = new fr.wseduc.webutils.collections.JsonArray();
                                                        JsonArray queryResult = body.getJsonArray("results");
                                                        result.put("moyennes",new fr.wseduc.webutils.collections.JsonArray());

                                                        for (int i = 0; i < queryResult.size(); i++) {

                                                            HashMap<Long,JsonArray> listMoyDevoirs = new HashMap<>();
                                                            HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>
                                                                    notesByDevoirByPeriode = new HashMap<>();

                                                            notesByDevoirByPeriode.put(null,
                                                                    new HashMap<Long, ArrayList<NoteDevoir>>());

                                                            JsonObject eleve = queryResult.getJsonObject(i);
                                                            String idEleve = eleve.getString("idEleve");
                                                            idEleves.add(idEleve);


                                                            for (int j = 0; j < listNotes.size(); j++) {

                                                                JsonObject note = listNotes.getJsonObject(j);

                                                                if (note.getString("valeur") == null ||
                                                                        !note.getBoolean("is_evaluated")) {
                                                                    continue; //Si la note fait partie d'un devoir qui n'est pas évalué,
                                                                    // elle n'est pas prise en compte dans le calcul de la moyenne
                                                                }
                                                                else {
                                                                    Long id_periode = note.getLong("id_periode");
                                                                    if(!notesByDevoirByPeriode.containsKey(id_periode)) {
                                                                        notesByDevoirByPeriode.put(id_periode,
                                                                                new HashMap<Long, ArrayList<NoteDevoir>>());

                                                                    }
                                                                    NoteDevoir noteDevoir = new NoteDevoir(
                                                                            Double.valueOf(note.getString("valeur")),
                                                                            Double.valueOf(note.getLong("diviseur")),
                                                                            note.getBoolean("ramener_sur"),
                                                                            Double.valueOf(note.getString("coefficient")));
                                                                    if(note.getString("id_eleve").equals(idEleve)) {
                                                                        utilsService.addToMap(id_periode,
                                                                                notesByDevoirByPeriode.get(id_periode),
                                                                                noteDevoir);
                                                                        utilsService.addToMap(null,
                                                                                notesByDevoirByPeriode.get(null),
                                                                                noteDevoir);
                                                                    }
                                                                }
                                                            }

                                                            // Calcul des moyennes par période pour L'élève
                                                            for(Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> entryPeriode
                                                                    : notesByDevoirByPeriode.entrySet()) {
                                                                listMoyDevoirs.put(entryPeriode.getKey(), new fr.wseduc.webutils.collections.JsonArray());
                                                                for (Map.Entry<Long, ArrayList<NoteDevoir>> entry :
                                                                        entryPeriode.getValue().entrySet()) {
                                                                    JsonObject moyenne = utilsService.calculMoyenne(
                                                                            entry.getValue(),
                                                                            false, 20);
                                                                    moyenne.put("id_periode", entry.getKey());
                                                                    moyenne.put("id_eleve", idEleve);
                                                                    listMoyDevoirs.get(entryPeriode.getKey()).add(moyenne);
                                                                }
                                                                if (listMoyDevoirs.get(entryPeriode.getKey()).size() > 0) {
                                                                    result.getJsonArray("moyennes").add(
                                                                            listMoyDevoirs.get(entryPeriode.getKey()).getJsonObject(0));
                                                                }
                                                            }
                                                        }



                                                        // On récupère les moyennes finales
                                                        notesService.getColonneReleve(
                                                                idEleves,
                                                                null,
                                                                idMatiere,
                                                                new JsonArray().add(idClasse),
                                                                "moyenne",
                                                                new Handler<Either<String, JsonArray>>() {
                                                                    @Override
                                                                    public void handle(Either<String, JsonArray> event) {
                                                                        if (event.isRight()) {
                                                                            result.put("moyennes_finales",event.right().getValue());
                                                                            Renders.renderJson(request, result);
                                                                        } else {
                                                                            JsonObject error = new JsonObject()
                                                                                    .put("error",
                                                                                            event.left().getValue());
                                                                            Renders.renderJson(request, error, 400);
                                                                        }
                                                                    }
                                                                });
                                                    }

                                                }
                                            }));
                                        } else {
                                            JsonObject error = (new JsonObject()).put("error",
                                                    (String) event.left().getValue());
                                            Renders.renderJson(request, error, 400);
                                        }
                                    }
                                };
                                if (!hasAccessToMatiere) {
                                    unauthorized(request);
                                } else {


                                    notesService.getNoteElevePeriode(null,
                                            idEtablissement,
                                            new fr.wseduc.webutils.collections.JsonArray().add(idClasse),
                                            idMatiere,
                                            null, handler);
                                }
                            }
                        });
            }
        });
    }

    private void addMoyenneFinalAndAppreciationPositionnementEleve(final String idEleve, final String idClasse,
                                                                   final String idMatiere, final String idEtablissement,
                                                                   final HttpServerRequest request,
                                                                   final  JsonObject result, final JsonArray IdEleves,
                                                                   final HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse) {
        notesService.getColonneReleve(
                new fr.wseduc.webutils.collections.JsonArray().add(idEleve),
                null,
                idMatiere,
                new JsonArray().add(idClasse),
                "appreciation_matiere_periode",
                new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            result.put("appreciations",
                                    event.right().getValue());
                            notesService.getColonneReleve(
                                    new fr.wseduc.webutils.collections.JsonArray().add(idEleve),
                                    null,
                                    idMatiere,
                                    new JsonArray().add(idClasse),
                                    "moyenne",
                                    new Handler<Either<String, JsonArray>>() {
                                        @Override
                                        public void handle(Either<String, JsonArray> event) {
                                            if (event.isRight()) {
                                                result.put("moyennes_finales",event.right().getValue());
                                                notesService.getColonneReleve(
                                                        new fr.wseduc.webutils.collections.JsonArray().add(idEleve),
                                                        null,
                                                        idMatiere,
                                                        new JsonArray().add(idClasse),
                                                        "positionnement", new Handler<Either<String, JsonArray>>() {
                                                            @Override
                                                            public void handle(Either<String, JsonArray> event) {
                                                                if (event.isRight()) {
                                                                    result
                                                                            .put("positionnements",
                                                                                    event.right().getValue());

                                                                    // idClasse et typeClass à null car on récupère le positionnement quelque soit sa classe
                                                                    //On récupère le positionnement seulement par rapport à la matière
                                                                    //idClass sera mis à null dans le service qu'appelle cette méthode car on a besoin de idClasse
                                                                    addPositionnementAutoEleve(idEleve, idClasse,
                                                                            idMatiere, idEtablissement,
                                                                            request,result, IdEleves,notesByDevoirByPeriodeClasse);
                                                                } else {
                                                                    JsonObject error = new JsonObject()
                                                                            .put("error",
                                                                                    event.left().getValue());
                                                                    Renders.renderJson(request, error, 400);
                                                                }
                                                            }
                                                        });
                                            } else {
                                                JsonObject error = new JsonObject()
                                                        .put("error",
                                                                event.left().getValue());
                                                Renders.renderJson(request, error, 400);
                                            }
                                        }
                                    });
                        } else {
                            JsonObject error = new JsonObject()
                                    .put("error",
                                            event.left().getValue());
                            Renders.renderJson(request, error, 400);
                        }
                    }
                });
    }
    private void addPositionnementAutoEleve(final String idEleve, final String idClasse, final String idMatiere,
                                            final String idEtablissement,
                                            final HttpServerRequest request,final  JsonObject result, final JsonArray idEleves,
                                            final HashMap<Long,
                                                    HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse) {
        // idClasse et typeClass à null car on récupère le positionnement quelque soit sa classe
        //On récupère le positionnement seulement par rapport à la matière
        notesService.getCompetencesNotesReleve( idEtablissement,null,null, idMatiere,null,idEleve,
                null, false,
                new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            JsonArray listNotes = event.right().getValue();
                            notesService.calculPositionnementAutoByEleveByMatiere(listNotes,result);
//si idEleves.size()=0 alors aucune note sur les élèves => il faut voir s'il y a des moyennes finales => dans tous les cas on récupère les moyennes finales
                            notesService.getColonneReleve(null, null, idMatiere,
                                    new JsonArray().add(idClasse), "moyenne",
                                    new Handler<Either<String, JsonArray>>() {
                                        @Override
                                        public void handle(Either<String, JsonArray> event) {
                                            if (event.isRight()) {
                                                JsonArray moyFinalesEleves = event.right().getValue();
                                                // Calcul des moyennes par période pour la classe
                                                notesService.calculAndSetMoyenneClasseByPeriode(idEleves,moyFinalesEleves, notesByDevoirByPeriodeClasse, result);
                                                Renders.renderJson(request, result);

                                            } else {
                                                JsonObject error = (new JsonObject()).put("error",
                                                        (String) event.left().getValue());
                                                Renders.renderJson(request, error, 400);
                                            }
                                        }
                                    });

                           /* if(idEleves.size() != 0) {

                                notesService.getColonneReleve(idEleves, null, idMatiere, idClasse, "moyenne",
                                        new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(Either<String, JsonArray> event) {
                                                if (event.isRight()) {
                                                    JsonArray moyFinalesEleves = event.right().getValue();
                                                    notesService.calculAndSetMoyenneClasseByPeriode(moyFinalesEleves, notesByDevoirByPeriodeClasse, result);
                                                    Renders.renderJson(request, result);

                                                } else {
                                                    JsonObject error = (new JsonObject()).put("error",
                                                            (String) event.left().getValue());
                                                    Renders.renderJson(request, error, 400);
                                                }
                                            }
                                        });

                            }else{
                                result.put("moyennesClasse",new fr.wseduc.webutils.collections.JsonArray());
                                Renders.renderJson(request, result);
                            }*/
                        } else {
                            JsonObject error = (new JsonObject()).put("error",
                                    (String) event.left().getValue());
                            Renders.renderJson(request, error, 400);
                        }

                    }
                });
    }

    private void getDataGraph(final HttpServerRequest request, JsonArray groupIds) {
        final String idEleve = request.params().get("idEleve");
        final String idEtablissement = request.params().get("idEtablissement");
        final String idClasse = request.params().get("idClasse");
        final Integer typeClasse = Integer.valueOf(request.params().get("typeClasse"));
        final String idPeriodeString = request.params().get("idPeriode");
        final Long idPeriode = (idPeriodeString != null)? Long.parseLong(idPeriodeString): null;
        // 1. On récupère les CompétencesNotes de toutes les matières et de tous les élèves
        notesService.getCompetencesNotesReleve(idEtablissement,idClasse,
                groupIds,
                null,
                idPeriode,
                null,
                typeClasse,
                false,
                new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if(event.isLeft()) {
                            String message = "[getReleveDataForGraph] error while getCompetencesNotesReleve";
                            badRequest(request, message);
                            log.error(message);
                        }
                        else {
                            final JsonArray listCompNotes = event.right().getValue();
                            // 2. On récupère les Notes de toutes les matières et de tous les élèves
                            notesService.getNotesReleve(idEtablissement, idClasse, null,
                                    idPeriode, typeClasse, true, groupIds,
                                    new Handler<Either<String, JsonArray>>() {
                                        @Override
                                        public void handle(Either<String, JsonArray> event) {
                                            if(event.isLeft()) {
                                                String message = "[getReleveDataForGraph] " +
                                                        "error while getNotesReleve";
                                                badRequest(request, message);
                                                log.error(message);
                                            }
                                            else {
                                                final JsonArray listNotes = event.right().getValue();
                                                Map<String,JsonArray> matieresCompNotes = new HashMap<>();
                                                Map<String,JsonArray> matieresCompNotesEleve = new HashMap<>();
                                                JsonArray idMatieres;

                                                // 3. On regroupe  les compétences notes par idMatière
                                                idMatieres = groupDataByMatiere(listCompNotes,
                                                        matieresCompNotes,
                                                        matieresCompNotesEleve, idEleve, true);

                                                // 4. On regroupe les notes par idMatière
                                                Map<String,JsonArray> matieresNotes = new HashMap<>();
                                                Map<String,JsonArray> matieresNotesEleve = new HashMap<>();
                                                idMatieres = utilsService.saUnion(groupDataByMatiere(listNotes,
                                                        matieresNotes,
                                                        matieresNotesEleve, idEleve, false), idMatieres);

                                                StatMat statMat = new StatMat();
                                                statMat.setMapIdMatStatclass(listNotes);
                                                Map<String, StatClass> mapMatieresStatClasseAndEleve =
                                                        statMat.getMapIdMatStatclass();
                                                // 5. On récupère tous les libelles des matières de
                                                // l'établissement et on fait correspondre aux résultats par
                                                // idMatière
                                                linkIdSubjectToLibelle(idEleve, getMaxByItem(matieresCompNotes),
                                                        getMaxByItem(matieresCompNotesEleve),
                                                        matieresNotes,
                                                        matieresNotesEleve, mapMatieresStatClasseAndEleve, idMatieres, request);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }


    private void getDataGraphDomaine(final HttpServerRequest request, JsonArray groupIds) {
        final String idEleve = request.params().get("idEleve");
        final String idEtablissement = request.params().get("idEtablissement");
        final String idClasse = request.params().get("idClasse");
        final Integer typeClasse = Integer.valueOf(request.params().get("typeClasse"));
        final String idPeriodeString = request.params().get("idPeriode");


        // 1. On récupère les Compétences-Notes de tous les domaines et de tous les élèves
        Future<JsonArray> compNotesFuture = Future.future();
        notesService.getCompetencesNotesReleve(idEtablissement, idClasse, groupIds, null,
                (idPeriodeString != null)? Long.parseLong(idPeriodeString) : null,
                null,
                typeClasse,
                true, event -> {
                    if (event.isLeft()) {
                        String message = "[DomaineDataForGraph] error while getCompetencesNotesReleve";
                        badRequest(request, message);
                    }
                    else {
                        compNotesFuture.complete(event.right().getValue());
                    }
                });

        // 2. En parallèle, On récupère les domaines du cycle auquel la classe est rattachée
        Future<JsonArray> domainesCycleFuture = Future.future();
        new DefaultDomaineService().getDomaines(idClasse, event -> {
            if (event.isLeft()) {
                String message = "[DomaineDataForGraph] error while getting domaines";
                domainesCycleFuture.fail(message);
            }
            else {
                domainesCycleFuture.complete(event.right().getValue());
            }
        });

        // 3. On Lie les compétences-Notes à leur libellé
        CompositeFuture.all(compNotesFuture, domainesCycleFuture).setHandler(event -> {
            if (event.succeeded()) {
                Renders.renderJson(request, linkCompNoteToLibelle(domainesCycleFuture.result(),
                        compNotesFuture.result(), idEleve));
            } else {
                String message = event.cause().getMessage();
                badRequest(request, message);
                log.error(message);
            }
        });

    }

    /**
     * Récupère les notes pour le bilan periodique
     *
     * @param request
     */
    @Get("/bilan/periodique/datas/graph")
    @ApiDoc("Récupère les données pour construire les graphs du bilan periodique")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    //@ResourceFilter(AccessReleveFilter.class)
    public void getBilanPeriodiqueDataForGraph(final HttpServerRequest request) {
        final String idEleve = request.params().get("idEleve");
        final String idEtablissement = request.params().get(Competences.ID_ETABLISSEMENT_KEY);
        Utils.getGroupsEleve(eb, idEleve, idEtablissement, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle( Either<String, JsonArray> responseQuerry) {
                if (!responseQuerry.isRight()) {
                    String error = responseQuerry.left().getValue();
                    log.error(error);
                    badRequest(request, error);
                } else {
                    JsonArray idGroups = responseQuerry.right().getValue();
                    //idGroups null si l'eleve n'est pas dans un groupe
                    getDataGraph(request, idGroups);
                }
            }
        });
    }


    @Get("/bilan/periodique/datas/graph/domaine")
    @ApiDoc("Récupère les données pour construire les graphs du bilan periodique")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    //@ResourceFilter(AccessReleveFilter.class)
    public void getBilanPeriodiqueDomaineForGraph(final HttpServerRequest request) {
        final String idEleve = request.params().get("idEleve");
        final String idEtablissement = request.params().get(Competences.ID_ETABLISSEMENT_KEY);
        Utils.getGroupsEleve(eb, idEleve, idEtablissement, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle( Either<String, JsonArray> responseQuerry) {
                if (!responseQuerry.isRight()) {
                    String error = responseQuerry.left().getValue();
                    log.error(error);
                    badRequest(request, error);
                } else {
                    JsonArray idGroups = responseQuerry.right().getValue();
                    //idGroups null si l'eleve n'est pas dans un groupe
                    getDataGraphDomaine(request, idGroups);
                }
            }
        });
    }


    /**
     * Récupère les notes pour le relevé de notes
     *
     * @param request
     */
    @Get("/releve/datas/graph")
    @ApiDoc("Récupère les données pour construire les graphs du relevé de notes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void getReleveDataForGraph(final HttpServerRequest request) {
        getDataGraph(request, null);
    }

    private JsonArray groupDataByMatiere(JsonArray datas, Map<String,JsonArray> mapDataClasse,
                                         Map<String,JsonArray> mapDataEleve, String idEleve, boolean checkFormative){
        JsonArray result = new JsonArray();
        for (int i=0; i< datas.size(); i++ ) {
            JsonObject data = datas.getJsonObject(i);
            if ( (checkFormative && data != null && data.getBoolean("formative") != null && !data.getBoolean("formative").booleanValue())
                    || !checkFormative) {
                String idMatiere = data.getString("id_matiere");
                idMatiere = (idMatiere!= null)? idMatiere : "no_id_matiere";
                if (!mapDataClasse.containsKey(idMatiere)) {
                    mapDataClasse.put(idMatiere, new JsonArray());
                    mapDataEleve.put(idMatiere, new JsonArray());
                    result.add(idMatiere);
                }
                mapDataClasse.get(idMatiere).add(data);
                if(idEleve.equals(data.getString("id_eleve")) || idEleve.equals(data.getString("id_eleve_moyenne_finale")) ) {
                    mapDataEleve.get(idMatiere).add(data);
                }
            }

        }
        return result;
    }

    // Permet de Calculer le Max des Niveaux Atteints par Items regroupés par Matière
    private Map<String,JsonArray> getMaxByItem(Map<String,JsonArray> mapData){
        Map<String,JsonArray> result = new HashMap<>();

        for (Map.Entry<String,JsonArray> entry: mapData.entrySet()) {
            String idEntry = entry.getKey();
            JsonArray currentEntry = entry.getValue();
            Map<String, JsonObject> maxComp = calculMaxCompNoteItem(currentEntry);
            result.put(idEntry, new JsonArray());
            for (Map.Entry<String,JsonObject> max: maxComp.entrySet()) {
                result.get(idEntry).add(max.getValue());
            }


        }
        return result;
    }

    // Permet de Calculer le Max des Niveaux Atteints par Items regroupés par Domaine
    private JsonArray getMaxByItemDomaine(JsonArray compNotes){
        JsonArray result = new JsonArray();

        Map<String, JsonObject> maxCompNote = calculMaxCompNoteItem(compNotes);
        for (Map.Entry<String,JsonObject> max: maxCompNote.entrySet()) {
            result.add(max.getValue());
        }

        return result;
    }

    private Map<String, JsonObject> calculMaxCompNoteItem (JsonArray compNotes) {
        Map<String, JsonObject> maxComp = new HashMap<>();
        for (int i = 0; i < compNotes.size(); i++) {
            JsonObject compNote = compNotes.getJsonObject(i);
            Long idCompetence = compNote.getLong("id_competence");
            String idEleve = compNote.getString("id_eleve");

            idEleve = (idEleve!= null)? idEleve : "null";

            String idItem = (idCompetence != null)? idCompetence.toString() : "null";
            idItem += idEleve;

            if (!maxComp.containsKey(idItem)) {
                maxComp.put(idItem, compNote);
            } else {

                Long evaluation = compNote.getLong("evaluation");
                Long niveauFinal = compNote.getLong("niveau_final");
                Long valueToTake = (niveauFinal != null) ? niveauFinal : evaluation;


                JsonObject maxCompetence = maxComp.get(idItem);
                Long maxEvaluation = maxCompetence.getLong("evaluation");
                Long maxNiveauFinal = maxCompetence.getLong("niveau_final");
                Long maxToTake = (maxNiveauFinal != null) ? maxNiveauFinal : maxEvaluation;

                if(maxToTake == null) {
                    maxComp.replace(idItem, compNote);
                }
                else if ((valueToTake != null && maxToTake != null) && (valueToTake > maxToTake)) {
                    maxComp.replace(idItem, compNote);
                }
            }

        }
        return maxComp;
    }

    private void linkIdSubjectToLibelle(String idEleve, Map<String, JsonArray> matieresCompNotes,
                                        Map<String, JsonArray> matieresCompNotesEleve,
                                        Map<String, JsonArray> matieresNotes,
                                        Map<String, JsonArray> matieresNotesEleve,
                                        Map<String, StatClass> mapMatieresStatClasseAndEleve,
                                        JsonArray idMatieres, final HttpServerRequest request) {

        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatieres")
                .put("idMatieres", idMatieres);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if (!"ok".equals(body.getString("status"))) {

                } else {
                    final JsonArray matieres = body.getJsonArray("results");
                    for (int i = 0 ; i < matieres.size(); i++) {
                        JsonObject matiere = matieres.getJsonObject(i);
                        Double classAverage = null;
                        Double averageStudent = null;
                        StatClass statClasse = mapMatieresStatClasseAndEleve.get(matiere.getString("id"));
                        if(statClasse != null) {
                            classAverage = statClasse.getAverageClass();
                            averageStudent = statClasse.getMoyenneEleve(idEleve);
                        }
                        matiere.put("competencesNotes",  matieresCompNotes
                                .get(matiere.getString("id")))
                                .put("competencesNotesEleve", matieresCompNotesEleve
                                        .get(matiere.getString("id")))
                                .put("notes", matieresNotes.get(matiere.getString("id")))
                                .put("notesEleve", matieresNotesEleve.get(matiere.getString("id")))
                                .put("studentAverage",averageStudent)
                                .put("classAverage", classAverage);

                    }
                    matieres.add(
                            new JsonObject().put("name", "null")
                                    .put("competencesNotes", matieresCompNotes.get("no_id_matiere"))
                                    .put("competencesNotesEleve", matieresCompNotesEleve.get("no_id_matiere"))
                                    .put("notes", matieresNotes.get("no_id_matiere"))
                                    .put("notesEleve", matieresNotesEleve.get("no_id_matiere"))
                    );

                    Renders.renderJson(request, matieres);

                }
            }
        }));
    }

    /**
     * Récupère les notes pour le relevé de notes
     *
     * @param request
     */
    @Get("/releve/datas/graph/domaine")
    @ApiDoc("Récupère les données pour construire les graphs du relevé de notes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void getReleveDataDomaineForGraph(final HttpServerRequest request) {
        getDataGraphDomaine(request, null);
    }



    private JsonArray linkCompNoteToLibelle(JsonArray domaines,
                                            JsonArray compNotes,
                                            String idEleve ) {
        Map<Long,JsonObject> domainesMap = new HashMap<>();
        JsonArray res = new JsonArray();

        // On groupe Competences-notes par domaine
        for (int i=0; i<compNotes.size(); i++) {
            JsonObject compNote = compNotes.getJsonObject(i);
            if(!compNote.getBoolean("formative")) {
                Long idDomaine = compNote.getLong("id_domaine");
                if (!domainesMap.containsKey(idDomaine)) {
                    domainesMap.put(idDomaine, new JsonObject()
                            .put("competencesNotes", new JsonArray())
                            .put("competencesNotesEleve", new JsonArray()));
                }
                JsonObject domaine = domainesMap.get(idDomaine);
                if (domaine != null) {
                    if (idEleve.equals(compNote.getString("id_eleve"))) {
                        domaine.getJsonArray("competencesNotesEleve").add(compNote);
                    }
                    domaine.getJsonArray("competencesNotes").add(compNote);
                }
            }
        }
        // On Lie les competences-notes groupées aux domaines
        for (int i=0; i<domaines.size(); i++) {
            JsonObject domaine = domaines.getJsonObject(i);
            JsonObject data = domainesMap.get(domaine.getLong("id"));
            JsonArray competencesNotesEleve =
                    (data!=null)? data.getJsonArray("competencesNotesEleve"): new JsonArray();
            JsonArray competencesNotes =
                    (data!=null)?data.getJsonArray("competencesNotes"): new JsonArray();
            if (data!= null) {
                res.add(domaine.put("competencesNotes",   getMaxByItemDomaine(competencesNotes))
                        .put("competencesNotesEleve", getMaxByItemDomaine(competencesNotesEleve)));
            }
        }
        return res;
    }

}



