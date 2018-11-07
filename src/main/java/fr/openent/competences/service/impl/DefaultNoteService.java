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
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.utils.UtilsConvert;
import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

import static fr.openent.competences.Competences.TRANSITION_CONFIG;
import static org.entcore.common.sql.SqlResult.validResultHandler;
import static org.entcore.common.sql.SqlResult.validResultsHandler;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultNoteService extends SqlCrudService implements NoteService {

    private EventBus eb;
    private UtilsService utilsService;
    public DefaultNoteService(String schema, String table) {
        super(schema, table);
    }
    public DefaultNoteService(String schema, String table, EventBus eb) {
        super(schema, table);
        this.eb = eb;
        utilsService = new DefaultUtilsService(eb);
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
    public void getNoteElevePeriode(String userId, String etablissementId, JsonArray idsClass, String matiereId,
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
                .append(" ON rel_devoirs_groupes.id_devoir = devoirs.id AND rel_devoirs_groupes.id_groupe IN " + Sql.listPrepared( idsClass.getList()))
                .append(" WHERE devoirs.id_etablissement = ? ")
                .append(" AND devoirs.id_matiere = ? ")
                .append((null != periodeId)? " AND devoirs.id_periode = ? ": " ")
                .append(" ORDER BY devoirs.date ASC, devoirs.id ASC;");

        if(null != userId) {
            values.add(userId);
        }

        for(int i = 0; i < idsClass.size(); i++){
            values.add(idsClass.getString(i));
        }

        values.add(etablissementId).add(matiereId);
        if(null != periodeId) {
            values.add(periodeId);
        }

        Sql.getInstance().prepared(query.toString(), values,
                new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L),
                validResultHandler(handler));
    }

    @Override
    public void getNotesReleve(String etablissementId, String classeId, String matiereId, Long periodeId,
                               Integer typeClasse, Boolean withMoyenneFinale,
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
                                    " devoirs.is_evaluated, null as annotation, devoirs.id_matiere " +
                                    ((withMoyenneFinale)? ", moyenne_finale.moyenne " : " ") +
                                    " FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs " +
                                    " LEFT JOIN " + Competences.COMPETENCES_SCHEMA + ".notes " +
                                    " ON (devoirs.id = notes.id_devoir AND " +
                                    " notes.id_eleve IN " + Sql.listPrepared(idEleves) + ")" +
                                    " INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_devoirs_groupes ON " +
                                    " (rel_devoirs_groupes.id_devoir = devoirs.id AND rel_devoirs_groupes.id_groupe = ?)" +
                                       ((withMoyenneFinale)?("LEFT JOIN notes.moyenne_finale " +
                                                    " ON (devoirs.id_periode = moyenne_finale.id_periode " +
                                               " AND  notes.id_eleve = moyenne_finale.id_eleve " +
                                               " AND devoirs.id_matiere = moyenne_finale.id_matiere " +
                                               " AND moyenne_finale.id_classe = ?) ") : " " )+
                                    " WHERE devoirs.id_etablissement = ? " +
                                    ((matiereId != null)?" AND devoirs.id_matiere = ? ": " "));
                            for (String eleve : idEleves) {
                                values.add(eleve);
                            }

                            values.add(classeId);
                            if (withMoyenneFinale) {
                                values.add(classeId);
                            }
                            values.add(etablissementId);

                            if (matiereId != null) {
                                    values.add(matiereId);
                            }

                            if (periodeId != null) {
                                query.append("AND devoirs.id_periode = ? ");
                                values.add(periodeId);
                            }
                            query.append(" UNION ");
                            query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, " +
                                    " devoirs.diviseur, devoirs.ramener_sur,null as valeur, null as id, " +
                                    " rel_annotations_devoirs.id_eleve, devoirs.is_evaluated, " +
                                    " rel_annotations_devoirs.id_annotation as annotation, devoirs.id_matiere " +
                                    ((withMoyenneFinale)? ", null as moyenne" : " " )+
                                    " FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs " +
                                    " LEFT JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_annotations_devoirs " +
                                    " ON (devoirs.id = rel_annotations_devoirs.id_devoir  AND " +
                                    " rel_annotations_devoirs.id_eleve IN " + Sql.listPrepared(idEleves) + ")" +
                                    " INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_devoirs_groupes " +
                                    " ON (rel_devoirs_groupes.id_devoir = devoirs.id AND rel_devoirs_groupes.id_groupe = ?) " +
                                    " WHERE devoirs.id_etablissement = ? " +
                                    ((matiereId != null)?" AND devoirs.id_matiere = ? ": ""));
                            for (String eleve : idEleves) {
                                values.add(eleve);
                            }
                            values.add(classeId).add(etablissementId);

                            if (matiereId != null) {
                                values.add(matiereId);
                            }
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


    public void getCompetencesNotesReleve(String etablissementId, String classeId, JsonArray groupIds, String matiereId,
                                          Long periodeId,  String eleveId, Integer typeClasse, Boolean withDomaineInfo,
                                          Handler<Either<String, JsonArray>> handler) {
        if(typeClasse == null){
            runGetCompetencesNotesReleve(etablissementId,classeId, groupIds, matiereId,periodeId,eleveId,
                    typeClasse, new ArrayList<String>(), withDomaineInfo, handler);
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
                                runGetCompetencesNotesReleve(etablissementId, classeId, groupIds,  matiereId, periodeId, eleveId,
                                        typeClasse, idEleves, withDomaineInfo, handler);

                            } else {
                                handler.handle(new Either.Left<>("Error While getting Available student "));
                            }
                        }
                    });
        }
    }

    private void runGetCompetencesNotesReleve(String etablissementId, String classeId, JsonArray groupIds, String matiereId,
                                              Long periodeId,  String eleveId, Integer typeClasse,
                                              List<String> idEleves,
                                              Boolean withDomaineInfo,
                                              Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT ")
                .append( (null != eleveId)? "DISTINCT": "")
                .append(" devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, ")
                .append(" devoirs.diviseur, devoirs.ramener_sur, competences_notes.evaluation ,")
                .append(" competences_notes.id_competence , devoirs.id_matiere, ")
                .append( (null != eleveId)? "": " competences_notes.id, ")
                .append((withDomaineInfo)? "compDom.id_domaine, " : "")
                .append(" devoirs.id_periode, competences_notes.id_eleve, devoirs.is_evaluated, ")
                .append("null as annotation, ")
                .append("competence_niveau_final.niveau_final AS niveau_final")
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

        if(classeId != null ){
            if(groupIds == null) {
                query.append("(rel_devoirs_groupes.id_devoir = devoirs.id  AND rel_devoirs_groupes.id_groupe = ? )");
                values.add(classeId);
            } else {
                groupIds.add(classeId);
                query.append("(rel_devoirs_groupes.id_devoir = devoirs.id  AND rel_devoirs_groupes.id_groupe IN " + Sql.listPrepared( UtilsConvert.jsonArrayToStringArr(groupIds)) + " )");
                for (Object groupeId : groupIds) {
                    values.add(groupeId);
                }
            }
        }else{
            query.append("rel_devoirs_groupes.id_devoir = devoirs.id");
        }

        if (withDomaineInfo) {
            query.append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_competences_domaines " + " AS compDom")
                    .append(" ON competences_notes.id_competence = compDom.id_competence ");
        }
        query.append(" LEFT JOIN "+ Competences.COMPETENCES_SCHEMA + ".competence_niveau_final ON " +
                "( competence_niveau_final.id_periode = devoirs.id_periode AND competence_niveau_final.id_eleve = competences_notes.id_eleve" +
               " AND competence_niveau_final.id_competence = competences_notes.id_competence AND competence_niveau_final.id_matiere = devoirs.id_matiere )");
        query.append(" WHERE devoirs.id_etablissement = ? ")
        .append((matiereId != null)? " AND devoirs.id_matiere = ? ": " ");

        values.add(etablissementId);

        if(matiereId != null){
          values.add(matiereId);
        }

        if(periodeId != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(periodeId);
        }

        Sql.getInstance().prepared(query.toString(), values,
                new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L),
                validResultHandler(handler));
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
        Sql.getInstance().prepared(query.toString(), values,
                new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L),
                validResultHandler(handler));
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
            if("moyenne".equals(colonne)) {
                values.add(field.getValue(colonne)).add(idClasse).add(idMatiere)
                        .add(field.getValue(colonne));
            }
            else values.add(field.getLong(colonne)).add(idClasse).add(idMatiere)
                    .add(field.getLong(colonne));
        }
        else if ("appreciation_matiere_periode".equals(colonne)) {
            values.add(field.getString(colonne)).add(idClasse).add(idMatiere)
                    .add(field.getString(colonne));
        }

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    public HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> calculMoyennesEleveByPeriode (JsonArray listNotes, final JsonObject result, String idEleve, JsonArray idEleves) {

        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriode = new HashMap<>();
        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse = new HashMap<>();

        notesByDevoirByPeriode.put(null,
                new HashMap<Long, ArrayList<NoteDevoir>>());
        notesByDevoirByPeriodeClasse.put(null,
                new HashMap<Long, ArrayList<NoteDevoir>>());


        //pour toutes les notes existantes dans la classe
        for (int i = 0; i < listNotes.size(); i++) {
            JsonObject note =
                    listNotes.getJsonObject(i);
            if (note.getString("valeur") == null || !note.getBoolean("is_evaluated")) {
                continue; //Si la note fait partie d'un devoir qui n'est pas évalué,
                // elle n'est pas prise en compte dans le calcul de la moyenne
            }
            else {
                Long id_periode = note.getLong("id_periode");
                String id_eleve = note.getString("id_eleve");

                if(!notesByDevoirByPeriode.containsKey(id_periode)) {

                    notesByDevoirByPeriode.put(id_periode,
                            new HashMap<Long, ArrayList<NoteDevoir>>());
                    notesByDevoirByPeriodeClasse.put(id_periode,
                            new HashMap<Long, ArrayList<NoteDevoir>>());

                }
                if(!idEleves.contains(id_eleve)) {
                    idEleves.add(id_eleve);
                }

                NoteDevoir noteDevoir = new NoteDevoir(Double.valueOf(
                        note.getString("valeur")),
                        Double.valueOf(note.getLong("diviseur")),
                        note.getBoolean("ramener_sur"),
                        Double.valueOf(note.getString("coefficient")),
                        note.getString("id_eleve"));

                //ajouter la note à la période correspondante et à l'année pour l'élève
                if(note.getString("id_eleve").equals(idEleve)) {
                    utilsService.addToMap(id_periode,
                            notesByDevoirByPeriode.get(id_periode), noteDevoir);
                    utilsService.addToMap(null,
                            notesByDevoirByPeriode.get(null), noteDevoir);
                }

                //ajouter la note à la période correspondante et à l'année pour toute la classe
                utilsService.addToMap(id_periode,
                        notesByDevoirByPeriodeClasse.get(id_periode), noteDevoir);
                utilsService.addToMap(null,
                        notesByDevoirByPeriodeClasse.get(null), noteDevoir);
            }
        }
        result.put("moyennes",
                new fr.wseduc.webutils.collections.JsonArray());

        HashMap<Long,JsonArray> listMoyDevoirs = new HashMap<>();

        // Calcul des moyennes par période pour l'élève
        for(Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> entryPeriode :
                notesByDevoirByPeriode.entrySet()) {
            Long idPeriode = entryPeriode.getKey();

            //entryPeriode contient les notes de l'élève pour une période
            listMoyDevoirs.put(idPeriode,
                    new fr.wseduc.webutils.collections.JsonArray());

            for (Map.Entry<Long, ArrayList<NoteDevoir>> entry :
                    entryPeriode.getValue().entrySet()) {
                JsonObject moyenne = utilsService.calculMoyenne(entry.getValue(),
                        false, 20);
                moyenne.put("id", idPeriode);
                listMoyDevoirs.get(idPeriode).add(moyenne);
            }

            //ajout des moyennes de l'élève sur chaque période au résultat final
            if (listMoyDevoirs.get(idPeriode).size() > 0) {
                result.getJsonArray("moyennes").add(listMoyDevoirs.
                        get(idPeriode).getJsonObject(0));
            }
        }

        return notesByDevoirByPeriodeClasse;
    }

    @Override
    public void getAppreciationMoyFinalePositionnement(String idEleve, String idMatiere, Long idPeriode, Handler<Either<String, JsonArray>> handler) {
        String query = new String();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query = "SELECT positionnement.positionnement AS positionnement_final, positionnement.id_classe AS id_classe_posi," +
                " positionnement.id_periode AS id_periode_positionnement,"+
                " moyenne_finale.moyenne AS moyenne_finale, moyenne_finale.id_classe AS id_classe_moyfinale, moyenne_finale.id_periode AS id_periode_moyenne_finale," +
                " appreciation_matiere_periode.appreciation_matiere_periode,  appreciation_matiere_periode.id_classe AS id_classe_appreciation," +
                " appreciation_matiere_periode.id_periode AS id_periode_appreciation"+
                " FROM notes.positionnement" +
                " FULL JOIN notes.moyenne_finale ON (positionnement.id_periode = moyenne_finale.id_periode" +
                " AND positionnement.id_eleve = moyenne_finale.id_eleve AND positionnement.id_matiere = moyenne_finale.id_matiere)" +
                " FULL JOIN notes.appreciation_matiere_periode ON (positionnement.id_periode = appreciation_matiere_periode.id_periode" +
                " AND positionnement.id_eleve = appreciation_matiere_periode.id_eleve AND" +
                " positionnement.id_matiere = appreciation_matiere_periode.id_matiere )" +
                " WHERE ";
        if(idPeriode != null){
            query += "(positionnement.id_periode = ? OR moyenne_finale.id_periode = ? OR appreciation_matiere_periode.id_periode = ? ) AND";
        }
        query += " (positionnement.id_eleve = ? OR moyenne_finale.id_eleve = ? OR appreciation_matiere_periode.id_eleve = ? )" +
                 " AND (positionnement.id_matiere = ? OR moyenne_finale.id_matiere = ? OR appreciation_matiere_periode.id_matiere = ? )";
       if(idPeriode !=null){
           values.add(idPeriode).add(idPeriode).add(idPeriode);
       }
        values.add(idEleve).add(idEleve).add(idEleve);
        values.add(idMatiere).add(idMatiere).add(idMatiere);

        Sql.getInstance().prepared( query, values,
                new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L),
                validResultHandler(handler));
    }

    public Double calculMoyenneClasseByPeriode(ArrayList<NoteDevoir> allNotes,
                                                JsonArray moyFinalesEleves,
                                                Long idPeriode){

        //classer les moyennes finales par période et par élèves dans une map
        Map<Long, Map<String, Double>> moyFinales =
                new HashMap<Long, Map<String, Double>>();
        for(Object o : moyFinalesEleves) {
            JsonObject moyFinale = (JsonObject) o;
            Long periode = moyFinale.getLong("id_periode");

            if(!moyFinales.containsKey(periode)) {
                moyFinales.put(periode, new HashMap<String, Double>());
            }
            moyFinales.get(periode).put(moyFinale.getString("id_eleve"),
                    Double.parseDouble(moyFinale.getString("moyenne")));
        }

        HashMap<String, ArrayList<NoteDevoir>> notesPeriodeByEleves = new HashMap<>();
        //mettre dans notesPeriodeByEleves idEleve -> notes de l'élève pour la période
        for(NoteDevoir note : allNotes){
            String id_eleve = note.getIdEleve();
            if(!notesPeriodeByEleves.containsKey(id_eleve)) {
                notesPeriodeByEleves.put(id_eleve, new ArrayList<NoteDevoir>());
            }
            notesPeriodeByEleves.get(id_eleve).add(note);
        }
        Double sumMoyClasse = 0.0;


        Map<String, Double> moyFinalesPeriode = moyFinales.get(idPeriode);

        //pour tous les élèves mettre leur moyenne finale ou atuo dans sumMoyClasse
        for(Map.Entry<String, ArrayList<NoteDevoir>> notesPeriodeByEleve : notesPeriodeByEleves.entrySet()){

            String idEleve = notesPeriodeByEleve.getKey();

            //si l'éleve en cours a une moyenne finale sur la période l'ajouter à sumMoyClasse
            //sinon calculer la moyenne de l'eleve et l'ajouter à sumMoyClasse

            if(moyFinalesPeriode != null && moyFinalesPeriode.containsKey(idEleve)){
                sumMoyClasse = sumMoyClasse + moyFinalesPeriode.get(idEleve);
            } else {
                sumMoyClasse = sumMoyClasse + utilsService.calculMoyenne(
                        notesPeriodeByEleve.getValue(),
                        false, 20)
                        .getDouble("moyenne");
            }
        }
        return (double) Math.round((sumMoyClasse/notesPeriodeByEleves.size()) * 100) / 100;
    }

    public void calculAndSetMoyenneClasseByPeriode(final JsonArray moyFinalesEleves,
                                                    final HashMap<Long,HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse,
                                                    final JsonObject result ) {

        JsonArray moyennesClasses = new fr.wseduc.webutils.collections.JsonArray();
        //pour chaque période,
        for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByPeriode :
                notesByDevoirByPeriodeClasse.entrySet()) {

            Long idPeriode = notesByPeriode.getKey();
            JsonObject moyennePeriodeClasse = new JsonObject();
            moyennePeriodeClasse.put("id", idPeriode);

            // je suis pas dans trimestre
            if (idPeriode != null) {

                ArrayList<NoteDevoir> allNotes =
                        notesByPeriode.getValue().get(notesByPeriode.getKey());
                moyennePeriodeClasse.put("moyenne",
                        calculMoyenneClasseByPeriode(
                                allNotes,
                                moyFinalesEleves,
                                idPeriode));
                moyennesClasses.add(moyennePeriodeClasse);

            } else { // additionne les moyennes pour de chaque periode et diviser le tout par le nombre de périodes
                Double sumMoyPeriode = 0.0;
                for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesPeriode :
                        notesByDevoirByPeriodeClasse.entrySet()) { //pour toutes les périodes existantes

                    ArrayList<NoteDevoir> allNotes =
                            notesPeriode.getValue().get(notesPeriode.getKey());
                    Long periode = notesPeriode.getKey();
                    if (periode != null) {
                        sumMoyPeriode = sumMoyPeriode +
                                calculMoyenneClasseByPeriode(
                                        allNotes,
                                        moyFinalesEleves,
                                        periode);
                    }
                }
                moyennePeriodeClasse.put("moyenne",
                        (double) Math.round((sumMoyPeriode / (notesByDevoirByPeriodeClasse.size() - 1)) * 100) / 100);
                moyennesClasses.add(moyennePeriodeClasse);
            }
        }

        result.put("moyennesClasse", moyennesClasses);

    }
    public void calculPositionnementAutoByEleveByMatiere(JsonArray listNotes, JsonObject result) {

        HashMap<Long, JsonArray> listMoyDevoirs = new HashMap<>();

        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>
                notesByDevoirByPeriode = new HashMap<>();

        notesByDevoirByPeriode.put(null,
                new HashMap<Long, ArrayList<NoteDevoir>>());

        Set<Long> idsCompetence = new HashSet<Long>();

        for (int i = 0; i < listNotes.size(); i++) {


            JsonObject note = listNotes.getJsonObject(i);
            Long id_periode = note.getLong("id_periode");
            if (!notesByDevoirByPeriode.containsKey(id_periode)) {
                notesByDevoirByPeriode.put(id_periode,
                        new HashMap<Long, ArrayList<NoteDevoir>>());

            }

            if (note.getLong("evaluation") == null
                    || note.getLong("evaluation") < 0) {
                continue; //Si pas de compétence Note
            }
            NoteDevoir noteDevoir;
            if (note.getLong("niveau_final") != null && !idsCompetence.contains(note.getLong("id_competence"))) {

                idsCompetence.add(note.getLong("id_competence"));
                noteDevoir = new NoteDevoir(
                        Double.valueOf(note.getLong("niveau_final")),
                        1.0,
                        false,
                        1.0);

            } else {
                noteDevoir = new NoteDevoir(
                        Double.valueOf(note.getLong("evaluation")),
                        1.0,
                        false,
                        1.0);
            }


            utilsService.addToMap(id_periode,
                    notesByDevoirByPeriode.get(id_periode),
                    noteDevoir);
            // positionnement sur l'année
            utilsService.addToMap(null,
                    notesByDevoirByPeriode.get(null),
                    noteDevoir);
        }
        result.put("positionnements_auto", new fr.wseduc.webutils.collections.JsonArray());
        // Calcul des moyennes des max de compétencesNotes par période pour L'élève
        for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> entryPeriode
                : notesByDevoirByPeriode.entrySet()) {
            listMoyDevoirs.put(entryPeriode.getKey(), new fr.wseduc.webutils.collections.JsonArray());
            for (Map.Entry<Long, ArrayList<NoteDevoir>> entry :
                    entryPeriode.getValue().entrySet()) {
                JsonObject moyenne = utilsService.calculMoyenne(
                        entry.getValue(),
                        false, 1);
                moyenne.put("id_periode", entry.getKey());
                listMoyDevoirs.get(entryPeriode.getKey()).add(moyenne);
            }
            if (listMoyDevoirs.get(entryPeriode.getKey()).size() > 0) {
                result.getJsonArray("positionnements_auto").add(
                        listMoyDevoirs.get(entryPeriode.getKey()).getJsonObject(0));
            } else {
                result.getJsonArray("positionnements_auto").add(new JsonObject()
                        .put("moyenne", -1)
                        .put("id_periode", entryPeriode.getKey()));
            }
        }
    }

}
