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

package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.AnnotationService;
import fr.openent.competences.service.CompetenceNoteService;
import fr.openent.competences.service.NoteService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DefaultAnnotationService extends SqlCrudService implements AnnotationService {
    private final NoteService noteService;
    private final CompetenceNoteService competenceNoteService;
    public DefaultAnnotationService(String schema, String table) {
        super(schema, table);
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE);
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_NOTES_TABLE);
    }

    public void listAnnotations(String idEtab, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT * ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".annotations ")
                .append("WHERE "+ Competences.COMPETENCES_SCHEMA +".annotations.id_etablissement = ?");

        values.add(idEtab);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void createAppreciation(JsonObject appreciation, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.create(appreciation, user, handler);
    }

    @Override
    public void updateAppreciation(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.update(data.getValue("id").toString(), data, user, handler);
    }

    @Override
    public void deleteAppreciation(Long idAppreciation, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.delete(idAppreciation.toString(), user, handler);
    }

    @Override
    public void createAnnotationDevoir(final Long idDevoir,final Long idAnnotation,final String idEleve,final  Handler<Either<String, JsonObject>> handler) {
        noteService.getNotesParElevesParDevoirs(new String[]{idEleve}, new Long[]{idDevoir},
                new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(final Either<String, JsonArray> eventNotesDevoirs) {
                        if (eventNotesDevoirs.isRight()) {
                            competenceNoteService.getCompetencesNotes(idDevoir,idEleve,
                                    false,
                                    new Handler<Either<String, JsonArray>>() {
                                        @Override
                                        public void handle(Either<String, JsonArray> eventCompetencesDevoir) {
                                            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();

                                            //Si on une compétence note existe sur le devoir, pour un élève donné, on le supprime
                                            if(eventCompetencesDevoir.right() != null){
                                                if(eventCompetencesDevoir.right().getValue() != null
                                                        && eventCompetencesDevoir.right().getValue().size()>0){
                                                    // Suppression compétence note
                                                    StringBuilder queryDeleteCompetenceNote = new StringBuilder().append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".competences_notes WHERE id_devoir = ? AND id_eleve = ? ;");
                                                    JsonArray paramsDeleteCompetenceNote = new fr.wseduc.webutils.collections.JsonArray();
                                                    paramsDeleteCompetenceNote.add(idDevoir).add(idEleve);
                                                    statements.add(new JsonObject()
                                                            .put("statement", queryDeleteCompetenceNote.toString())
                                                            .put("values", paramsDeleteCompetenceNote)
                                                            .put("action", "prepared"));
                                                }
                                            }

                                            //Si on une note existe sur le devoir, pour un élève donné, on le supprime
                                            if(eventNotesDevoirs.right() != null){
                                                if(eventNotesDevoirs.right().getValue() != null
                                                        && eventNotesDevoirs.right().getValue().size()>0){
                                                    // Suppression Note
                                                    StringBuilder queryDeleteNote = new StringBuilder().append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".notes WHERE id_devoir = ? AND id_eleve = ? ;");
                                                    JsonArray paramsDeleteNote = new fr.wseduc.webutils.collections.JsonArray();
                                                    paramsDeleteNote.add(idDevoir).add(idEleve);
                                                    statements.add(new JsonObject()
                                                            .put("statement", queryDeleteNote.toString())
                                                            .put("values", paramsDeleteNote)
                                                            .put("action", "prepared"));
                                                }
                                            }

                                            // Ajout de l'annotation sur le devoir, pour un élève donné
                                            StringBuilder query = new StringBuilder().append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".rel_annotations_devoirs (id_devoir, id_annotation, id_eleve) VALUES (?, ?, ?);");
                                            JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
                                            params.add(idDevoir).add(idAnnotation).add(idEleve);
                                            statements.add(new JsonObject()
                                                    .put("statement", query.toString())
                                                    .put("values", params)
                                                    .put("action", "prepared"));

                                            Sql.getInstance().transaction(statements, SqlResult.validRowsResultHandler(handler));
                                        }
                                    });


                        } else {
                            handler.handle(new Either.Left<String, JsonObject>(eventNotesDevoirs.left().getValue()));
                        }
                    }
                });
    }

    @Override
    public void updateAnnotationDevoir(Long idDevoir, Long idAnnotation, String idEleve, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("UPDATE "+ Competences.COMPETENCES_SCHEMA +".rel_annotations_devoirs SET id_annotation = ? WHERE id_devoir = ? AND id_eleve = ?;");
        values.add(idAnnotation).add(idDevoir).add(idEleve);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void deleteAnnotation(Long idDevoir, String idEleve, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".rel_annotations_devoirs WHERE id_devoir = ? AND id_eleve = ?;");
        values.add(idDevoir).add(idEleve);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validRowsResultHandler(handler));
    }
}
