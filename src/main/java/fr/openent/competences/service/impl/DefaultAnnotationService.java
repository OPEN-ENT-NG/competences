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
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

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
        JsonArray values = new JsonArray();

        query.append("SELECT * ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".annotations ")
                .append("WHERE "+ Competences.COMPETENCES_SCHEMA +".annotations.id_etablissement = ?");

        values.addString(idEtab);

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
                                    new Handler<Either<String, JsonArray>>() {
                                        @Override
                                        public void handle(Either<String, JsonArray> eventCompetencesDevoir) {
                                            JsonArray statements = new JsonArray();

                                            //Si on une compétence note existe sur le devoir, pour un élève donné, on le supprime
                                            if(eventCompetencesDevoir.right() != null){
                                                if(eventCompetencesDevoir.right().getValue() != null
                                                        && eventCompetencesDevoir.right().getValue().size()>0){
                                                    // Suppression compétence note
                                                    StringBuilder queryDeleteCompetenceNote = new StringBuilder().append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".competences_notes WHERE id_devoir = ? AND id_eleve = ? ;");
                                                    JsonArray paramsDeleteCompetenceNote = new JsonArray();
                                                    paramsDeleteCompetenceNote.addNumber(idDevoir).addString(idEleve);
                                                    statements.add(new JsonObject()
                                                            .putString("statement", queryDeleteCompetenceNote.toString())
                                                            .putArray("values", paramsDeleteCompetenceNote)
                                                            .putString("action", "prepared"));
                                                }
                                            }

                                            //Si on une note existe sur le devoir, pour un élève donné, on le supprime
                                            if(eventNotesDevoirs.right() != null){
                                                if(eventNotesDevoirs.right().getValue() != null
                                                        && eventNotesDevoirs.right().getValue().size()>0){
                                                    // Suppression Note
                                                    StringBuilder queryDeleteNote = new StringBuilder().append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".notes WHERE id_devoir = ? AND id_eleve = ? ;");
                                                    JsonArray paramsDeleteNote = new JsonArray();
                                                    paramsDeleteNote.addNumber(idDevoir).addString(idEleve);
                                                    statements.add(new JsonObject()
                                                            .putString("statement", queryDeleteNote.toString())
                                                            .putArray("values", paramsDeleteNote)
                                                            .putString("action", "prepared"));
                                                }
                                            }

                                            // Ajout de l'annotation sur le devoir, pour un élève donné
                                            StringBuilder query = new StringBuilder().append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".rel_annotations_devoirs (id_devoir, id_annotation, id_eleve) VALUES (?, ?, ?);");
                                            JsonArray params = new JsonArray();
                                            params.addNumber(idDevoir).addNumber(idAnnotation).addString(idEleve);
                                            statements.add(new JsonObject()
                                                    .putString("statement", query.toString())
                                                    .putArray("values", params)
                                                    .putString("action", "prepared"));

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
        JsonArray values = new JsonArray();

        query.append("UPDATE "+ Competences.COMPETENCES_SCHEMA +".rel_annotations_devoirs SET id_annotation = ? WHERE id_devoir = ? AND id_eleve = ?;");
        values.addNumber(idAnnotation).addNumber(idDevoir).addString(idEleve);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void deleteAnnotation(Long idDevoir, String idEleve, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new JsonArray();

        query.append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".rel_annotations_devoirs WHERE id_devoir = ? AND id_eleve = ?;");
        values.addNumber(idDevoir).addString(idEleve);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validRowsResultHandler(handler));
    }
}
