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

package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.NoteService;
import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.sql.SqlResult.validResultHandler;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultNoteService extends SqlCrudService implements NoteService {

    private EventBus eb;
    public DefaultNoteService(String schema, String table) {
        super(schema, table);
    }
    public DefaultNoteService(String schema, String table, EventBus eb) {
        super(schema, table);
        this.eb = eb;
    }



    @Override
    public void createNote(JsonObject note, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.create(note, user, handler);
    }

    @Override
    public void listNotesParDevoir(Long devoirId, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        //tables
        String table_appreciation = Competences.COMPETENCES_SCHEMA + "." +Competences.APPRECIATIONS_TABLE;
        String table_note         = Competences.COMPETENCES_SCHEMA + "." +Competences.NOTES_TABLE;
        String table_annotations  = Competences.COMPETENCES_SCHEMA + "." +Competences.REL_ANNOTATIONS_DEVOIRS_TABLE;

        //colonne note
        String note_id        = table_note + ".id";
        String note_id_eleve  = table_note + ".id_eleve";
        String note_id_devoir = table_note + ".id_devoir";
        String note_valeur    = table_note + ".valeur";

        //colonne appreciation
        String appreciation_id        = table_appreciation + ".id";
        String appreciation_valeur    = table_appreciation +".valeur";
        String appreciation_id_eleve  = table_appreciation + ".id_eleve";
        String appreciation_id_devoir = table_appreciation + ".id_devoir";

        //colonne annotations
        String annotations_id_devoir = table_annotations + ".id_devoir";
        String annotations_id_eleve = table_annotations + ".id_eleve";
        String annotations_id_annotation = table_annotations + ".id_annotation";

        query.append("SELECT res.*,devoirs.date, devoirs.coefficient, devoirs.ramener_sur  ")
                .append(" FROM ( ")

                // Récupération des appréciations avec/ou sans notes avec/ou sans annotation
                .append("SELECT "+ appreciation_id_devoir +" as id_devoir, " + appreciation_id_eleve+", " + note_id + " as id, ")
                .append(note_valeur + " as valeur, " + appreciation_id +" as id_appreciation, " + appreciation_valeur)
                .append(" as appreciation , " + annotations_id_annotation +" FROM " + table_appreciation)
                .append(" LEFT JOIN " + table_note + " ON ( " + appreciation_id_devoir + " = " + note_id_devoir)
                .append(" AND " + appreciation_id_eleve + " = " + note_id_eleve + " )")
                .append(" LEFT JOIN " + table_annotations + " ON ( " + appreciation_id_devoir + " = "+ annotations_id_devoir)
                .append(" AND " + appreciation_id_eleve + " = " + annotations_id_eleve +")")
                .append(" WHERE " + appreciation_id_devoir + " = ? ")

                .append(" UNION ")

                // Récupération des annotations sans appréciation
                .append("SELECT " + annotations_id_devoir + " AS id_devoir ," + annotations_id_eleve + " ,NULL ,NULL ,NULL ,NULL ," + annotations_id_annotation)
                .append(" FROM " + table_annotations)
                .append("  WHERE " + annotations_id_devoir + " = ? ")
                .append("   AND NOT EXISTS ( ")
                .append("    SELECT 1 ")
                .append("    FROM " + table_appreciation)
                .append("    WHERE " + annotations_id_devoir + " = " +  appreciation_id_devoir)
                .append("     AND " + annotations_id_eleve + " = " + appreciation_id_eleve + ")" )

                .append(" UNION ")

                // Récupération des notes sans appréciation sans annotation
                .append(" SELECT " +  note_id_devoir +" as id_devoir, " +note_id_eleve + ", " + note_id + " as id, ")
                .append(note_valeur + " as valeur, null, null, null FROM " + table_note + " WHERE " + note_id_devoir + " = ? AND NOT EXISTS ( ")
                .append(" SELECT 1 FROM " + table_appreciation + " WHERE ")
                .append(note_id_devoir +" = " + appreciation_id_devoir)
                .append(" AND " + note_id_eleve + " = " + appreciation_id_eleve + " ) " +
                        "ORDER BY 2")
                .append(") AS res, notes.devoirs WHERE res.id_devoir = devoirs.id");

        values.add(devoirId);
        values.add(devoirId);
        values.add(devoirId);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void getNotesParElevesParDevoirs(String[] idEleves, Long[] idDevoirs, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        boolean eleves = idEleves.length != 0;
        boolean devoirs = idDevoirs.length != 0;

        query.append("SELECT notes.id_devoir, notes.id_eleve, notes.valeur, devoirs.coefficient, devoirs.diviseur, devoirs.ramener_sur " +
                "FROM " + Competences.COMPETENCES_SCHEMA + ".notes " +
                "LEFT JOIN " + Competences.COMPETENCES_SCHEMA + ".devoirs ON devoirs.id = notes.id_devoir WHERE devoirs.is_evaluated = true");

        if(eleves || devoirs) {
            query.append(" AND ");
            if(eleves) {
                query.append("notes.id_eleve IN " + Sql.listPrepared(idEleves));
                for(String s : idEleves) {
                    values.add(s);
                }
            }
            if(eleves && devoirs) {
                query.append(" AND ");
            }
            if(devoirs) {
                query.append("notes.id_devoir IN " + Sql.listPrepared(idDevoirs));
                for(Long l : idDevoirs) {
                    values.add(l);
                }
            }
        }

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void updateNote(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.update(data.getValue("id").toString(), data, user, handler);
    }

    @Override
    public void deleteNote(Long idNote, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.delete(idNote.toString(), user, handler);
    }

    @Override
    public void getWidgetNotes(String userId, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT notes.valeur, devoirs.id, devoirs.date, devoirs.id_matiere, devoirs.diviseur, devoirs.libelle, devoirs.name ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".notes, "+ Competences.COMPETENCES_SCHEMA +".devoirs ")
                .append("WHERE notes.id_eleve = ? ")
                .append("AND notes.id_devoir = devoirs.id ")
                .append("AND devoirs.date_publication <= current_date ")
                .append("ORDER BY notes.id DESC ")
                .append("LIMIT 5;");
        values.add(userId);
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void getNoteElevePeriode(String userId, String etablissementId, String classeId, String matiereId,
                                    Long periodeId, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, devoirs.diviseur, ")
                .append(" devoirs.ramener_sur, devoirs.is_evaluated, devoirs.id_periode, ")
                .append(" notes.valeur, notes.id, notes.id_eleve ")
                .append(" FROM "+ Competences.COMPETENCES_SCHEMA +".devoirs ")
                .append(" LEFT JOIN "+ Competences.COMPETENCES_SCHEMA +".notes ")
                .append(" ON devoirs.id = notes.id_devoir ")
                .append( (null!= userId)? " AND notes.id_eleve = ? ": "")
                .append(" INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_devoirs_groupes ")
                .append(" ON rel_devoirs_groupes.id_devoir = devoirs.id AND rel_devoirs_groupes.id_groupe = ? ")
                .append(" WHERE devoirs.id_etablissement = ? ")
                .append(" AND devoirs.id_matiere = ? ")
                .append((null != periodeId)? " AND devoirs.id_periode = ? ": " ")
                .append(" ORDER BY devoirs.date ASC, devoirs.id ASC;");

        if(null != userId) {
            values.add(userId);
        }

        values.add(classeId).add(etablissementId).add(matiereId);
        if(null != periodeId) {
            values.add(periodeId);
        }

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void getNotesReleve(String etablissementId, String classeId, String matiereId, Long periodeId,
                               Integer typeClasse,
                               Handler<Either<String, JsonArray>> handler) {

        new DefaultUtilsService(this.eb).studentIdAvailableForPeriode(classeId,periodeId, typeClasse,
                new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            JsonArray queryResult = event.right().getValue();
                            List<String> idEleves = new ArrayList<String>();

                            if (queryResult != null) {
                                for (int i = 0; i < queryResult.size(); i++) {
                                    idEleves.add(queryResult.getString(i));
                                }
                            }
                            StringBuilder query = new StringBuilder();
                            JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

                            //Construction de la requête
                            query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, " +
                                    " devoirs.diviseur, devoirs.ramener_sur,notes.valeur, notes.id, notes.id_eleve, " +
                                    " devoirs.is_evaluated, null as annotation" +
                                    " FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs " +
                                    " LEFT JOIN " + Competences.COMPETENCES_SCHEMA + ".notes " +
                                    " ON (devoirs.id = notes.id_devoir AND " +
                                    " notes.id_eleve IN " + Sql.listPrepared(idEleves) + ")" +
                                    " INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_devoirs_groupes ON " +
                                    " (rel_devoirs_groupes.id_devoir = devoirs.id AND rel_devoirs_groupes.id_groupe = ?)" +
                                    " WHERE devoirs.id_etablissement = ? " +
                                    " AND devoirs.id_matiere = ? ");
                            for (String eleve : idEleves) {
                                values.add(eleve);
                            }

                            values.add(classeId).add(etablissementId).add(matiereId);
                            if (periodeId != null) {
                                query.append("AND devoirs.id_periode = ? ");
                                values.add(periodeId);
                            }
                            query.append(" UNION ");
                            query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, " +
                                    " devoirs.diviseur, devoirs.ramener_sur,null as valeur, null as id, " +
                                    " rel_annotations_devoirs.id_eleve, devoirs.is_evaluated, " +
                                    " rel_annotations_devoirs.id_annotation as annotation " +
                                    " FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs " +
                                    " LEFT JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_annotations_devoirs " +
                                    " ON (devoirs.id = rel_annotations_devoirs.id_devoir  AND " +
                                    " rel_annotations_devoirs.id_eleve IN " + Sql.listPrepared(idEleves) + ")" +
                                    " INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_devoirs_groupes " +
                                    " ON (rel_devoirs_groupes.id_devoir = devoirs.id AND rel_devoirs_groupes.id_groupe = ?) " +
                                    " WHERE devoirs.id_etablissement = ? " +
                                    " AND devoirs.id_matiere = ? ");
                            for (String eleve : idEleves) {
                                values.add(eleve);
                            }
                            values.add(classeId).add(etablissementId).add(matiereId);
                            if (periodeId != null) {
                                query.append("AND devoirs.id_periode = ? ");
                                values.add(periodeId);
                            }

                            query.append("ORDER BY date ASC ;");
                            Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
                        } else {
                            handler.handle(new Either.Left<>("Error While getting Available student "));
                        }
                    }
                });

    }


    public void getCompetencesNotesReleve(String etablissementId, String classeId, String matiereId,
                                          Long periodeId,  String eleveId, Integer typeClasse,
                                          Handler<Either<String, JsonArray>> handler) {
        if(typeClasse == null){
            runGetCompetencesNotesReleve(etablissementId,classeId,matiereId,periodeId,eleveId,
                    typeClasse, new ArrayList<String>(),handler);
            return;

        }
        else {
            new DefaultUtilsService(this.eb).studentIdAvailableForPeriode(classeId, periodeId, typeClasse,
                    new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                JsonArray queryResult = event.right().getValue();
                                List<String> idEleves = new ArrayList<String>();

                                if (queryResult != null) {
                                    for (int i = 0; i < queryResult.size(); i++) {
                                        idEleves.add(queryResult.getString(i));
                                    }
                                }
                                runGetCompetencesNotesReleve(etablissementId, classeId, matiereId, periodeId, eleveId,
                                        typeClasse, idEleves, handler);

                            } else {
                                handler.handle(new Either.Left<>("Error While getting Available student "));
                            }
                        }
                    });
        }
    }

    private void runGetCompetencesNotesReleve(String etablissementId, String classeId, String matiereId,
                                              Long periodeId,  String eleveId, Integer typeClasse,
                                              List<String> idEleves,
                                              Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT ")
                .append( (null != eleveId)? "DISTINCT": "")
                .append(" devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, ")
                .append(" devoirs.diviseur, devoirs.ramener_sur, competences_notes.evaluation ,")
                .append( (null != eleveId)? "": " competences_notes.id, ")
                .append(" devoirs.id_periode, competences_notes.id_eleve, devoirs.is_evaluated, ")
                .append("null as annotation ")
                .append(" FROM "+ Competences.COMPETENCES_SCHEMA +".devoirs ");

        if (null != eleveId) {
            query.append(" LEFT JOIN (SELECT id, id_devoir, id_competence, max(evaluation) ")
                    .append("as evaluation, id_eleve " )
                    .append(" FROM "+ Competences.COMPETENCES_SCHEMA +".competences_notes ")
                    .append(" WHERE id_eleve = ? ")
                    .append(" GROUP BY (id, id_devoir, id_eleve) ) AS competences_notes  ")
                    .append(" ON devoirs.id ")
                    .append(" = competences_notes.id_devoir " );
            values.add(eleveId);
        }
        else {
            query.append(" LEFT JOIN "+ Competences.COMPETENCES_SCHEMA +".competences_notes " +
                    "ON (devoirs.id  = competences_notes.id_devoir " +
                    "AND  competences_notes.id_eleve IN "+ Sql.listPrepared(idEleves)+ ")" );
            for (String idEleve : idEleves) {
                values.add(idEleve);
            }
        }

        query.append(" INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_devoirs_groupes ON ");

        if(classeId != null){
            query.append("(rel_devoirs_groupes.id_devoir = devoirs.id  AND rel_devoirs_groupes.id_groupe = ? )");
            values.add(classeId);
        }else{
            query.append("rel_devoirs_groupes.id_devoir = devoirs.id");
        }
        query.append(" WHERE devoirs.id_etablissement = ?  AND devoirs.id_matiere = ? ");

        values.add(etablissementId).add(matiereId);
        if(periodeId != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(periodeId);
        }
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void deleteColonneReleve(String idEleve, Long idPeriode, String idMatiere, String idClasse,
                                    String colonne,   Handler<Either<String, JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder()
                .append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +"."+ colonne )
                .append("moyenne".equals(colonne)? "_finale": " ")
                .append(" WHERE id_periode = ? AND id_eleve=?  AND id_classe=? AND id_matiere=? ");

        values.add(idPeriode).add(idEleve).add(idClasse).add(idMatiere);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }


    @Override
    public void getColonneReleve(JsonArray idEleves, Long idPeriode, String idMatiere, String idClasse,
                                 String colonne, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        // le positionnement est enregistré pour un élève indépendament de sa classe
        // ou de ses groupes (positionnement global)
        // on le recupere donc sans filtre sur la classe
        if(colonne.equals("positionnement")) {
            query.append("SELECT id_periode, id_eleve," + colonne + ", id_matiere ");
        } else {
            query.append("SELECT id_periode, id_eleve," + colonne + ", id_classe, id_matiere ");
        }

            query.append(" FROM ")
            .append(Competences.COMPETENCES_SCHEMA +"."+  colonne + ("moyenne".equals(colonne)? "_finale": " "))
            .append(" WHERE   id_matiere = ? ");
            values.add(idMatiere);
            if(!colonne.equals("positionnement")) {
                query.append(" AND id_classe = ? ");
                values.add(idClasse);
            }
            query.append(" AND id_eleve IN " + Sql.listPrepared(idEleves.getList().toArray()))
            .append( (null != idPeriode)? " AND id_periode = ? ": "" );


        for(int i=0; i < idEleves.size(); i++) {
            values.add(idEleves.getString(i).toString());
        }
        if(null != idPeriode) {
            values.add(idPeriode);
        }
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void setColonneReleve(String idEleve, Long idPeriode, String idMatiere, String idClasse, JsonObject field,
                                 String colonne,Handler<Either<String, JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder()
                .append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +"."+ colonne )
                .append("moyenne".equals(colonne)? "_finale": " ")
                .append(" (id_periode, id_eleve," + colonne + ", id_classe, id_matiere) VALUES ")
                .append(" (?, ?, ?, ?, ?) ")
                .append(" ON CONFLICT (id_periode, id_eleve, id_classe, id_matiere) ")
                .append(" DO UPDATE SET " + colonne + " = ? ");

        values.add(idPeriode).add(idEleve);
        if ("moyenne".equals(colonne) || "positionnement".equals(colonne)) {

            // le positionnement est enregistré pour un élève indépendament de sa classe
            // ou de ses groupes (positionnement global)
            if("positionnement".equals(colonne)) {
                idClasse = "";
            }
            values.add(field.getLong(colonne)).add(idClasse).add(idMatiere)
                    .add(field.getLong(colonne));
        }
        else if ("appreciation_matiere_periode".equals(colonne)) {
            values.add(field.getString(colonne)).add(idClasse).add(idMatiere)
                    .add(field.getString(colonne));
        }

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }
}
