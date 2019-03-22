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
import fr.openent.competences.utils.FormateFutureEvent;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.apache.fontbox.afm.Composite;
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

    private void addStatementAnnotation(JsonArray statements,final Long idDevoir,final Long idAnnotation,
                                        final String idEleve ){
        StringBuilder query = new StringBuilder()
                .append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".rel_annotations_devoirs " +
                        " (id_devoir, id_annotation, id_eleve)  VALUES (?, ?, ?) " +
                        " ON CONFLICT  (id_devoir, id_eleve) " +
                        "DO UPDATE SET  id_annotation = ? ");
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(idDevoir).add(idAnnotation).add(idEleve).add(idAnnotation);
        statements.add(new JsonObject()
                .put("statement", query.toString())
                .put("values", params)
                .put("action", "prepared"));
    }

    private void addStatementdeleteCompetenceNote(JsonArray statements,final Long idDevoir, final String idEleve ) {
        StringBuilder queryDeleteCompetenceNote = new StringBuilder()
                .append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".competences_notes " +
                        "WHERE id_devoir = ? AND id_eleve = ? ;");
        JsonArray paramsDeleteCompetenceNote = new fr.wseduc.webutils.collections.JsonArray();
        paramsDeleteCompetenceNote.add(idDevoir).add(idEleve);
        statements.add(new JsonObject()
                .put("statement", queryDeleteCompetenceNote.toString())
                .put("values", paramsDeleteCompetenceNote)
                .put("action", "prepared"));
    }

    private void addStatementdeleteNote(JsonArray statements,final Long idDevoir, final String idEleve ) {
        StringBuilder queryDeleteNote = new StringBuilder()
                .append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".notes " +
                        "WHERE id_devoir = ? AND id_eleve = ? ;");
        JsonArray paramsDeleteNote = new fr.wseduc.webutils.collections.JsonArray();
        paramsDeleteNote.add(idDevoir).add(idEleve);
        statements.add(new JsonObject()
                .put("statement", queryDeleteNote.toString())
                .put("values", paramsDeleteNote)
                .put("action", "prepared"));
    }
    @Override
    public void createAnnotationDevoir(final Long idDevoir,final Long idAnnotation,
                                       final String idEleve,final  Handler<Either<String, JsonObject>> handler) {

        // Récupération des notes de l'élèves sur le devoir
        Future<JsonArray> notesFuture = Future.future();
        noteService.getNotesParElevesParDevoirs(new String[]{idEleve}, new Long[]{idDevoir}, event -> {
            FormateFutureEvent.formate(notesFuture, event);
        });


        // Récupération des competences notes sur le devoir
        Future<JsonArray> competencesNotesFuture = Future.future();
        competenceNoteService.getCompetencesNotes(idDevoir,idEleve,false, event -> {
            FormateFutureEvent.formate(competencesNotesFuture, event);
        });

        CompositeFuture.all(notesFuture, competencesNotesFuture).setHandler( event -> {
            if(event.failed()){
                handler.handle(new Either.Left<>(event.cause().getMessage()));
            }
            else {
                JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();

                //Si on une compétence note existe sur le devoir, pour un élève donné, on le supprime
                JsonArray competencesDevoir = competencesNotesFuture.result();
                if(competencesDevoir != null && competencesDevoir.size()>0 ){
                    // Suppression compétence note
                    addStatementdeleteCompetenceNote(statements, idDevoir, idEleve);
                }

                //Si on une note existe sur le devoir, pour un élève donné, on le supprime
                JsonArray notesDevoir =  notesFuture.result();
                    if(notesDevoir!= null && notesDevoir.size()>0){
                        // Suppression Note
                        addStatementdeleteNote(statements, idDevoir, idEleve);
                    }

                // Ajout de l'annotation sur le devoir, pour un élève donné
                addStatementAnnotation(statements, idDevoir, idAnnotation, idEleve);
                Sql.getInstance().transaction(statements,SqlResult.validRowsResultHandler(handler));
            }
        });

    }

    @Override
    public void updateAnnotationDevoir(Long idDevoir, Long idAnnotation, String idEleve, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("UPDATE "+ Competences.COMPETENCES_SCHEMA +".rel_annotations_devoirs " +
                "SET id_annotation = ? WHERE id_devoir = ? AND id_eleve = ?;");
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
