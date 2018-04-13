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
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.security.AccessEvaluationFilter;
import fr.openent.competences.security.AccessNoteFilter;
import fr.openent.competences.security.AccessReleveFilter;
import fr.openent.competences.security.CreateEvaluationWorkflow;
import fr.openent.competences.security.utils.FilterPeriodeUtils;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.service.ElementProgramme;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultElementProgramme;
import fr.openent.competences.service.impl.DefaultNoteService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.*;

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
        notesService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE);
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
//    /:idEleve/:idEtablissement/:idClasse/:idMatiere/:idPeriode
    @Get("/releve")
    @ApiDoc("Récupère les notes, les moyennes finales pour le relevé de notes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void getNoteElevePeriode(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

            @Override
            public void handle(UserInfos user) {
                final String idEleve = request.params().get("idEleve");
                final String idEtablissement = request.params().get("idEtablissement");
                final String idClasse = request.params().get("idClasse");
                final String idMatiere = request.params().get("idMatiere");
                final String idPeriodeString = request.params().get("idPeriode");


                new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere,
                        new Handler<Boolean>() {
                            @Override
                            public void handle(final Boolean hasAccessToMatiere) {

                                Handler<Either<String, JsonArray>> handler = new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            final JsonObject result = new JsonObject();
                                            JsonArray listNotes = event.right().getValue();
                                            JsonArray listMoyDevoirs = new JsonArray();
                                            JsonArray listMoyEleves = new JsonArray();
                                            HashMap<Long, ArrayList<NoteDevoir>> notesByDevoir = new HashMap<>();
                                            HashMap<String, ArrayList<NoteDevoir>> notesByEleve = new HashMap<>();

                                            for (int i = 0; i < listNotes.size(); i++) {

                                                JsonObject note = listNotes.get(i);

                                                if (note.getString("valeur") == null ||
                                                        !note.getBoolean("is_evaluated")) {
                                                    continue; //Si la note fait partie d'un devoir qui n'est pas évalué,
                                                    // elle n'est pas prise en compte dans le calcul de la moyenne
                                                }

                                                NoteDevoir noteDevoir = new NoteDevoir(
                                                        Double.valueOf(note.getString("valeur")),
                                                        Double.valueOf(note.getLong("diviseur")),
                                                        note.getBoolean("ramener_sur"),
                                                        Double.valueOf(note.getString("coefficient")));

                                                Long idDevoir = note.getLong("id_devoir");
                                                utilsService.addToMap(idDevoir, notesByDevoir, noteDevoir);

                                                String idEleve = note.getString("id_eleve");
                                                utilsService.addToMap(idEleve, notesByEleve, noteDevoir);
                                            }

                                            for (Map.Entry<Long, ArrayList<NoteDevoir>> entry : notesByDevoir.entrySet()) {
                                                JsonObject moyenne = utilsService.calculMoyenneParDiviseur(entry.getValue(),
                                                        true);
                                                moyenne.putValue("id", entry.getKey());
                                                listMoyDevoirs.add(moyenne);
                                            }
                                            result.putArray("devoirs", listMoyDevoirs);

                                            for (Map.Entry<String, ArrayList<NoteDevoir>> entry : notesByEleve
                                                    .entrySet()) {
                                                JsonObject moyenne = utilsService.calculMoyenne(entry.getValue(),
                                                        false, 20);
                                                moyenne.putValue("id", entry.getKey());
                                                listMoyEleves.add(moyenne);
                                            }
                                            result.putArray("eleves", listMoyEleves);

                                            result.putArray("notes", listNotes);

                                            notesService.getCompetencesNotesReleve(
                                                    idEtablissement,
                                                    idClasse,
                                                    idMatiere,
                                                    (null != idPeriodeString)? Long.parseLong(idPeriodeString): null,
                                                    new Handler<Either<String, JsonArray>>() {
                                                        @Override
                                                        public void handle(Either<String, JsonArray> event) {
                                                            if (event.isRight()) {
                                                                JsonArray listCompNotes = event.right().getValue();
                                                                result.putArray("competencesNotes",
                                                                        listCompNotes);
                                                                if (null != idPeriodeString) {
                                                                    addMoyenneFinalAndAppreciation(request, result);
                                                                } else {
                                                                    Renders.renderJson(request, result);
                                                                }
                                                            } else {
                                                                Renders.renderJson(request, new JsonObject()
                                                                                .putString("error",
                                                                                        (String) event.left()
                                                                                                .getValue()),
                                                                        400);
                                                            }
                                                        }
                                                    });

                                        } else {
                                            JsonObject error = (new JsonObject()).putString("error",
                                                    (String) event.left().getValue());
                                            Renders.renderJson(request, error, 400);
                                        }
                                    }
                                };
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

                                    if (idEleve != null) {

                                        notesService.getNoteElevePeriode(idEleve,
                                                idEtablissement,
                                                idClasse,
                                                idMatiere,
                                                idPeriode,
                                                handler);
                                    } else if (idPeriode != null) {

                                        notesService.getNotesReleve(idEtablissement,
                                                idClasse,
                                                idMatiere,
                                                idPeriode,
                                                handler);
                                    } else {
                                        notesService.getNotesReleve(idEtablissement,
                                                idClasse,
                                                idMatiere,
                                                null,
                                                handler);
                                    }
                                }
                            }
                        });
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
                                    JsonObject note = notesEleve.get(i);
                                    notes.add(new NoteDevoir(Double.parseDouble(note.getString("valeur")),
                                            Double.parseDouble(note.getNumber("diviseur").toString()),
                                            note.getBoolean("ramener_sur"),
                                            Double.parseDouble(note.getString("coefficient"))));
                                }
                                Renders.renderJson(request, utilsService.calculMoyenne(notes, false, 20));
                            } else {
                                renderError(request, new JsonObject().putString("error", event.left().getValue()));
                            }
                        }
                    });
                }
            }
        });
    }

    void addMoyenneFinalAndAppreciation(final HttpServerRequest request, final JsonObject res) {
        final String idClasse = request.params().get("idClasse");
        final String idMatiere = request.params().get("idMatiere");
        final String table = request.params().get("colonne");
        final String idPeriodeString = request.params().get("idPeriode");

        final JsonObject action = new JsonObject()
                .putString("action", "classe.getElevesClasses")
                .putArray("idClasses", new JsonArray().addString(idClasse));

        if (idPeriodeString != null) {
            try {
                final long idPeriode = Long.parseLong(idPeriodeString);
                elementProgramme.getElementProgramme(idPeriode,idMatiere,idClasse,new Handler<Either<String, JsonObject>>() {
                    @Override
                    public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                        if (stringJsonObjectEither.isRight()) {

                            res.putObject("elementProgramme",stringJsonObjectEither.right().getValue());

                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> message) {
                                    JsonObject body = message.body();

                                    if ("ok".equals(body.getString("status"))) {
                                        Map<String, List<String>> result = new LinkedHashMap<>();
                                        final JsonArray idEleves = new JsonArray();
                                        JsonArray queryResult = body.getArray("results");

                                        for (int i = 0; i < queryResult.size(); i++) {
                                            JsonObject eleve = queryResult.get(i);
                                            idEleves.addString(eleve.getString("idEleve"));
                                        }
                                        notesService.getColonneReleve(
                                                idEleves,
                                                idPeriode,
                                                idMatiere,
                                                idClasse,
                                                "moyenne",
                                                new Handler<Either<String, JsonArray>>() {
                                                    @Override
                                                    public void handle(Either<String, JsonArray> event) {
                                                        if (event.isRight()) {
                                                            res.putArray("moyennes",
                                                                    event.right().getValue());
                                                            addAppreciationsEleve(request, idEleves, idPeriodeString, idMatiere,
                                                                    idClasse, res);

                                                        } else {
                                                            JsonObject error = new JsonObject()
                                                                    .putString("error", event.left().getValue());
                                                            Renders.renderJson(request, error, 400);
                                                        }
                                                    }
                                                });
                                    } else {
                                        log.error("getRelevePeriodique " + table + body.getString("message"));
                                        JsonObject error = (new JsonObject()).putString("error",
                                                "failed get Moyenne Finale");
                                        Renders.renderJson(request, error, 400);
                                    }
                                }
                            });
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


    private void addAppreciationsEleve(final HttpServerRequest request, JsonArray idEleves, String idPeriodeString,
                                       String idMatiere, String idClasse, final JsonObject res) {
        Long idPeriode = Long.parseLong(idPeriodeString);
        notesService.getColonneReleve(
                idEleves,
                idPeriode,
                idMatiere,
                idClasse,
                "appreciation_matiere_periode",
                new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            Renders.renderJson(request, res.putArray("appreciations",
                                    event.right().getValue()));
                        } else {
                            JsonObject error = new JsonObject()
                                    .putString("error", event.left().getValue());
                            Renders.renderJson(request, error, 400);
                        }
                    }
                });
    }


    @Get("/releve/periodique")
    @ApiDoc("Récupère les moyennes finales pour le relevé périodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void getColonneRelevePeriode(final HttpServerRequest request) {
        final String idClasse = request.params().get("idClasse");
        final String idMatiere = request.params().get("idMatiere");
        final String table = request.params().get("colonne");

        JsonObject action = new JsonObject()
                .putString("action", "classe.getElevesClasses")
                .putArray("idClasses", new JsonArray().addString(idClasse));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    Map<String, List<String>> result = new LinkedHashMap<>();
                    final JsonArray idEleves = new JsonArray();
                    JsonArray queryResult = body.getArray("results");

                    for (int i = 0; i < queryResult.size(); i++) {
                        JsonObject eleve = queryResult.get(i);
                        idEleves.addString(eleve.getString("idEleve"));
                    }
                    String idPeriodeString = request.params().get("idPeriode");
                    Long idPeriode = null;
                    if (idPeriodeString != null) {
                        try {
                            idPeriode = Long.parseLong(idPeriodeString);
                        } catch (NumberFormatException e) {
                            log.error("Error : idPeriode must be a long object ", e);
                            badRequest(request, e.getMessage());
                            return;
                        }
                    }
                    notesService.getColonneReleve(
                            idEleves,
                            idPeriode,
                            idMatiere,
                            idClasse,
                            table,
                            arrayResponseHandler(request));
                } else {
                    log.error("getRelevePeriodique " + table + body.getString("message"));
                    JsonObject error = (new JsonObject()).putString("error",
                            "failed get Moyenne Finale");
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
                        new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere,
                                new Handler<Boolean>() {
                                    @Override
                                    public void handle(final Boolean hasAccessToMatiere) {
                                        if (hasAccessToMatiere) {
                                            elementProgramme.setElementProgramme(
                                                    user.getUserId(),
                                                    idPeriode,
                                                    idMatiere,
                                                    idClasse,
                                                    texte,
                                                    arrayResponseHandler(request));
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
    @ApiDoc("Récupère les moyennes finales pour le relevé périodique")
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
                        final String idMatiere = resource.getString("idMatiere");
                        final String idEleve = resource.getString("idEleve");
                        final String table = resource.getString("colonne");
                        final Long idPeriode = resource.getLong("idPeriode");
                        final String idEtablissement = resource.getString("idEtablissement");

                        // Vérification de l'accès à la matière
                        new FilterUserUtils(user, eb).validateMatiere(request, idEtablissement, idMatiere,
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
                });

            }
        });
    }

}
