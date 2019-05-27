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
import fr.openent.competences.Utils;
import fr.openent.competences.bean.Eleve;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.bean.StatClass;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.utils.FormateFutureEvent;
import fr.openent.competences.utils.UtilsConvert;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.getLibellePeriode;
import static fr.wseduc.webutils.http.Renders.badRequest;
import static org.entcore.common.sql.SqlResult.validResultHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;


/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultNoteService extends SqlCrudService implements NoteService {


    public final String MOYENNES = "moyennes";
    public final String MOYENNESFINALES = "moyennesFinales";
    public final String MOYENNEFINALE = "moyenneFinale";
    public final String VALEUR = "valeur";
    public final String DIVISEUR = "diviseur";
    public final String RAMENER_SUR = "ramener_sur";
    public final String COEFFICIENT = "coefficient";
    public final String IS_EVALUATED = "is_evaluated";
    public final String ID_DEVOIR = "id_devoir";
    public final String APPRECIATION_MATIERE_PERIODE = "appreciation_matiere_periode";
    public final String SYNTHESE_BILAN_PERIODIQUE = "synthese_bilan_periodique";
    public final String AVIS_CONSEIL_DE_CLASSE = "avis_conseil_de_classe";
    public final String AVIS_CONSEIL_ORIENTATION = "avis_conseil_orientation";
    public final String AVIS_CONSEIL_BILAN_PERIODIQUE = "avis_conseil_bilan_periodique";
    public final String COMPETENCES_NOTES_KEY = "competencesNotes";
    public final String TABLE_CONVERSION_KEY = "tableConversions";
    public static final String COLSPAN = "colspan";

    private EventBus eb;
    private UtilsService utilsService;
    public DefaultNoteService(String schema, String table) {
        super(schema, table);
    }
    protected static final Logger log = LoggerFactory.getLogger(DefaultNoteService.class);

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
        String table_appreciation = COMPETENCES_SCHEMA + "." +Competences.APPRECIATIONS_TABLE;
        String table_note         = COMPETENCES_SCHEMA + "." +Competences.NOTES_TABLE;
        String table_annotations  = COMPETENCES_SCHEMA + "." +Competences.REL_ANNOTATIONS_DEVOIRS_TABLE;

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
        getNotesParElevesParDevoirs(idEleves, idDevoirs, null, handler);
    }

    @Override
    public void getNotesParElevesParDevoirs(String[] idEleves, Long[] idDevoirs, Integer idPeriode, Handler<Either<String, JsonArray>> handler) {
        getNotesParElevesParDevoirs (idEleves, null, idDevoirs, idPeriode, handler);
    }

    @Override
    public void getNotesParElevesParDevoirs(String[] idEleves, String[] idGroupes, Long[] idDevoirs, Integer idPeriode, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        boolean eleves = idEleves != null && idEleves.length != 0;
        boolean groupes = idGroupes != null && idGroupes.length != 0;
        boolean devoirs = idDevoirs != null && idDevoirs.length != 0;
        boolean periode = idPeriode != null;

        query.append("SELECT notes.id_devoir, notes.id_eleve, notes.valeur, devoirs.coefficient, devoirs.diviseur, devoirs.ramener_sur, devoirs.owner, devoirs.id_matiere, grp.id_groupe " +
                "FROM " + COMPETENCES_SCHEMA + ".notes " +
                "LEFT JOIN " + COMPETENCES_SCHEMA + ".devoirs ON devoirs.id = notes.id_devoir " +
                "LEFT JOIN " + COMPETENCES_SCHEMA + "." + Competences.REL_DEVOIRS_GROUPES + " AS grp ON devoirs.id = grp.id_devoir WHERE devoirs.is_evaluated = true AND ");

        if(eleves) {
            query.append("notes.id_eleve IN " + Sql.listPrepared(idEleves) + " AND ");
            for(String s : idEleves) {
                values.add(s);
            }
        }
        if(groupes) {
            query.append("grp.id_groupe IN " + Sql.listPrepared(idGroupes) + " AND ");
            for(String s : idGroupes) {
                values.add(s);
            }
        }
        if(devoirs) {
            query.append("notes.id_devoir IN " + Sql.listPrepared(idDevoirs) + " AND ");
            for(Long l : idDevoirs) {
                values.add(l);
            }
        }
        if(periode) {
            query.append("devoirs.id_periode = ? AND ");
            values.add(idPeriode);
        }

        Sql.getInstance().prepared(query.toString().substring(0, query.length() - 5), values, validResultHandler(handler));
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
                .append("FROM "+ COMPETENCES_SCHEMA +".notes, "+ COMPETENCES_SCHEMA +".devoirs ")
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
                .append(" FROM "+ COMPETENCES_SCHEMA +".devoirs ")
                .append(" LEFT JOIN "+ COMPETENCES_SCHEMA +".notes ")
                .append(" ON devoirs.id = notes.id_devoir ")
                .append( (null!= userId)? " AND notes.id_eleve = ? ": "")
                .append(" INNER JOIN "+ COMPETENCES_SCHEMA +".rel_devoirs_groupes ")
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

        Sql.getInstance().prepared(query.toString(), values,Competences.DELIVERY_OPTIONS,
                validResultHandler(handler));
    }

    @Override
    public void getNotesReleve(String etablissementId, String classeId, String matiereId, Long periodeId,
                               Integer typeClasse, Boolean withMoyenneFinale, JsonArray idsGroup,
                               Handler<Either<String, JsonArray>> handler) {

        new DefaultUtilsService(this.eb).studentIdAvailableForPeriode(classeId,periodeId, typeClasse,
                event -> {
                    if (event.isRight()) {
                        JsonArray queryResult = event.right().getValue();
                        getNotesReleveEleves(queryResult, etablissementId, classeId, matiereId,
                                periodeId, typeClasse, withMoyenneFinale, idsGroup, handler);
                    } else {
                        handler.handle(new Either.Left<>("Error While getting Available student "));
                    }
                });

    }

    public void getNotesReleveEleves(JsonArray ids,String etablissementId, String classeId,String matiereId,
                                     Long periodeId,
                                     Integer typeClasse, Boolean withMoyenneFinale, JsonArray idsGroup,
                                     Handler<Either<String, JsonArray>> handler) {

        List<String> idEleves = new ArrayList<String>();

        if (ids != null) {
            for (int i = 0; i < ids.size(); i++) {
                idEleves.add(ids.getString(i));
            }
        }
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        //Construction de la requête
        query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient," +
                " devoirs.diviseur, devoirs.ramener_sur,notes.valeur, notes.id, devoirs.id_periode , notes.id_eleve," +
                " devoirs.is_evaluated, null as annotation, devoirs.id_matiere" +
                " FROM " + COMPETENCES_SCHEMA + ".devoirs" +
                " LEFT JOIN " + COMPETENCES_SCHEMA + ".notes" +
                " ON ( devoirs.id = notes.id_devoir  " +
                (( null != idsGroup)? ")" : "AND notes.id_eleve IN " + Sql.listPrepared(idEleves) + ")") +
                " INNER JOIN " + COMPETENCES_SCHEMA + ".rel_devoirs_groupes ON " +
                "(rel_devoirs_groupes.id_devoir = devoirs.id AND "+
                ((null != idsGroup)? "rel_devoirs_groupes.id_groupe IN "+Sql.listPrepared(idsGroup.getList())+")" : "rel_devoirs_groupes.id_groupe = ?)") +
                " WHERE devoirs.id_etablissement = ? " +
                ((matiereId != null)?" AND devoirs.id_matiere = ? ": " "));

        setParamGetNotesReleve(idsGroup,idEleves,classeId, values);
        values.add(etablissementId);

        if (matiereId != null) {
            values.add(matiereId);
        }
        if (periodeId != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(periodeId);
        }
        query.append(" UNION ");
        query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient," +
                " devoirs.diviseur, devoirs.ramener_sur,null as valeur, null as id, devoirs.id_periode, " +
                " rel_annotations_devoirs.id_eleve, devoirs.is_evaluated," +
                " rel_annotations_devoirs.id_annotation as annotation, devoirs.id_matiere" +
                " FROM " + COMPETENCES_SCHEMA + ".devoirs" +
                " LEFT JOIN " + COMPETENCES_SCHEMA + ".rel_annotations_devoirs" +
                " ON (devoirs.id = rel_annotations_devoirs.id_devoir " +
                ((null != idsGroup)? ")": " AND rel_annotations_devoirs.id_eleve IN " +
                        Sql.listPrepared(idEleves) + ")")+
                " INNER JOIN " + COMPETENCES_SCHEMA + ".rel_devoirs_groupes" +
                " ON (rel_devoirs_groupes.id_devoir = devoirs.id " +
                ((null != idsGroup)? "AND rel_devoirs_groupes.id_groupe IN "
                        +Sql.listPrepared(idsGroup.getList())+")": "AND rel_devoirs_groupes.id_groupe = ?) ")+
                " WHERE devoirs.id_etablissement = ? " +
                ((matiereId != null)?" AND devoirs.id_matiere = ? ": ""));
        setParamGetNotesReleve(idsGroup,idEleves,classeId, values);
        values.add(etablissementId);
        if (matiereId != null) {
            values.add(matiereId);
        }
        if (periodeId != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(periodeId);
        }
        query.append("ORDER BY date ASC ");

        String queryWithMoyF = new String();
        if (withMoyenneFinale) {

            queryWithMoyF = ("SELECT * FROM ( " + query + ") AS devoirs_notes_annotation " +
                    "FULL JOIN ( SELECT moyenne_finale.id_matiere AS id_matiere_moyf, " +
                    "moyenne_finale.id_eleve AS id_eleve_moyenne_finale, moyenne_finale.moyenne, " +
                    "moyenne_finale.id_periode AS id_periode_moyenne_finale " +
                    "FROM notes.moyenne_finale WHERE "+
                    ((null != idsGroup)? "moyenne_finale.id_classe IN "+ Sql.listPrepared(idsGroup.getList()):
                            " moyenne_finale.id_eleve IN " +  Sql.listPrepared(idEleves) +
                                    " AND moyenne_finale.id_classe = ? " )+
                    ((null != periodeId)? "AND moyenne_finale.id_periode = ? " :"") +
                    ((null != matiereId)? "AND moyenne_finale.id_matiere = ?" : "") +
                    ") AS moyf ON ( moyf.id_eleve_moyenne_finale = devoirs_notes_annotation.id_eleve "+
                    " AND moyf.id_matiere_moyf = devoirs_notes_annotation.id_matiere " +
                    " AND moyf.id_periode_moyenne_finale = devoirs_notes_annotation.id_periode )");

            setParamGetNotesReleve(idsGroup,idEleves,classeId, values);
            if (periodeId != null) {
                values.add(periodeId);
            }
            if (matiereId != null) {
                values.add(matiereId);
            }
        }

        Sql.getInstance().prepared((withMoyenneFinale)? queryWithMoyF : query.toString(), values,
                Competences.DELIVERY_OPTIONS, validResultHandler(handler));
    }

    public void getNotesReleveTotalesEleves(JsonArray ids,String etablissementId, String classeId,JsonArray matiereIds,
                                     Long periodeId, Handler<Either<String, JsonArray>> handler) {

        List<String> idEleves = new ArrayList<String>();

        if (ids != null) {
            for (int i = 0; i < ids.size(); i++) {
                idEleves.add(ids.getString(i));
            }
        }

        List<String> idMatieres = new ArrayList<String>();

        if (matiereIds != null) {
            for (int i = 0; i < matiereIds.size(); i++) {
                idMatieres.add(matiereIds.getString(i));
            }
        }

        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        //Construction de la requête
        query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient," +
                " devoirs.diviseur, devoirs.ramener_sur,notes.valeur, notes.id, devoirs.id_periode , notes.id_eleve," +
                " devoirs.is_evaluated, null as annotation, devoirs.id_matiere" +
                " FROM " + COMPETENCES_SCHEMA + ".devoirs" +
                " LEFT JOIN " + COMPETENCES_SCHEMA + ".notes" +
                " ON ( devoirs.id = notes.id_devoir  " +
                "AND notes.id_eleve IN " + Sql.listPrepared(idEleves) + ")"+
                " INNER JOIN " + COMPETENCES_SCHEMA + ".rel_devoirs_groupes ON " +
                "(rel_devoirs_groupes.id_devoir = devoirs.id AND "+
                "rel_devoirs_groupes.id_groupe = ?)"+
                " WHERE devoirs.id_etablissement = ? AND devoirs.id_matiere IN "+Sql.listPrepared(idMatieres)+" ");

        for (String eleve : idEleves) {
            values.add(eleve);
        }
        values.add(classeId);
        values.add(etablissementId);
        for (String matiere : idMatieres) {
            values.add(matiere);
        }

        if (periodeId != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(periodeId);
        }
        query.append(" UNION ");
        query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient," +
                " devoirs.diviseur, devoirs.ramener_sur,null as valeur, null as id, devoirs.id_periode, " +
                " rel_annotations_devoirs.id_eleve, devoirs.is_evaluated," +
                " rel_annotations_devoirs.id_annotation as annotation, devoirs.id_matiere" +
                " FROM " + COMPETENCES_SCHEMA + ".devoirs" +
                " LEFT JOIN " + COMPETENCES_SCHEMA + ".rel_annotations_devoirs" +
                " ON (devoirs.id = rel_annotations_devoirs.id_devoir " +
                " AND rel_annotations_devoirs.id_eleve IN " +Sql.listPrepared(idEleves) + ")"+
                " INNER JOIN " + COMPETENCES_SCHEMA + ".rel_devoirs_groupes" +
                " ON (rel_devoirs_groupes.id_devoir = devoirs.id " +
                "AND rel_devoirs_groupes.id_groupe = ?) "+
                " WHERE devoirs.id_etablissement = ? " +
                "AND devoirs.id_matiere IN "+Sql.listPrepared(idMatieres)+" ");

        for (String eleve : idEleves) {
            values.add(eleve);
        }
        values.add(classeId);
        values.add(etablissementId);
        for (String matiere : idMatieres) {
            values.add(matiere);
        }
        if (periodeId != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(periodeId);
        }
        query.append("ORDER BY date ASC ");

        Sql.getInstance().prepared(query.toString(), values,
                Competences.DELIVERY_OPTIONS, validResultHandler(handler));
    }



    private void setParamGetNotesReleve(JsonArray idsGroup,List<String> idEleves,String classeId,JsonArray values){
        if(null == idsGroup){
            for (String eleve : idEleves) {
                values.add(eleve);
            }
            values.add(classeId);
        }else {
            for (int i = 0; i < idsGroup.size(); i++) {
                values.add(idsGroup.getString(i));
            }
        }
    }

    public void getCompetencesNotesReleveEleves(JsonArray ids, String etablissementId, String classeId,
                                                JsonArray groupIds, String matiereId,
                                                Long periodeId,  String eleveId, Integer typeClasse,
                                                Boolean withDomaineInfo,
                                                Handler<Either<String, JsonArray>> handler) {
        List<String> idEleves = new ArrayList<String>();

        if (ids != null) {
            for (int i = 0; i < ids.size(); i++) {
                idEleves.add(ids.getString(i));
            }
        }
        runGetCompetencesNotesReleve(etablissementId, classeId, groupIds,  matiereId, periodeId,
                eleveId,
                typeClasse, idEleves, withDomaineInfo, handler);
    }

    public void getCompetencesNotesReleveTotaleEleves(JsonArray ids, String etablissementId, String classeId,
                                                JsonArray matiereIds, Long periodeId,
                                                Handler<Either<String, JsonArray>> handler) {
        List<String> idEleves = new ArrayList<String>();

        if (ids != null) {
            for (int i = 0; i < ids.size(); i++) {
                idEleves.add(ids.getString(i));
            }
        }
        List<String> idMatieres = new ArrayList<String>();

        if (matiereIds != null) {
            for (int i = 0; i < matiereIds.size(); i++) {
                idMatieres.add(matiereIds.getString(i));
            }
        }
        runGetCompetencesNotesReleveTotale(etablissementId, classeId,  idMatieres, periodeId,
                idEleves, handler);
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
                    event -> {
                        if (event.isRight()) {
                            JsonArray ids = event.right().getValue();
                            getCompetencesNotesReleveEleves( ids, etablissementId, classeId,
                                    groupIds, matiereId, periodeId, eleveId, typeClasse, withDomaineInfo, handler);

                        } else {
                            handler.handle(new Either.Left<>("Error While getting Available student "));
                        }
                    });
        }
    }

    private void runGetCompetencesNotesReleve(String etablissementId, String classeId, JsonArray groupIds,
                                              String matiereId,
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
                .append("competence_niveau_final.niveau_final AS niveau_final, type.formative")
                .append(" FROM "+ COMPETENCES_SCHEMA +".devoirs ");


        query.append(" INNER JOIN "+ COMPETENCES_SCHEMA +".type ON (devoirs.id_type = type.id) ");

        if (null != eleveId) {
            query.append(" LEFT JOIN (SELECT id, id_devoir, id_competence, max(evaluation) ")
                    .append("as evaluation, id_eleve " )
                    .append(" FROM "+ COMPETENCES_SCHEMA +".competences_notes ")
                    .append(" WHERE id_eleve = ? ")
                    .append(" GROUP BY (id, id_devoir, id_eleve) ) AS competences_notes  ")
                    .append(" ON devoirs.id ")
                    .append(" = competences_notes.id_devoir " );
            values.add(eleveId);
        }
        else {
            query.append(" LEFT JOIN "+ COMPETENCES_SCHEMA +".competences_notes " +
                    "ON (devoirs.id  = competences_notes.id_devoir " +
                    "AND  competences_notes.id_eleve IN "+ Sql.listPrepared(idEleves)+ ")" );
            for (String idEleve : idEleves) {
                values.add(idEleve);
            }
        }

        query.append(" INNER JOIN "+ COMPETENCES_SCHEMA +".rel_devoirs_groupes ON ");

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
            query.append(" INNER JOIN " + COMPETENCES_SCHEMA + ".rel_competences_domaines " + " AS compDom")
                    .append(" ON competences_notes.id_competence = compDom.id_competence ");
        }
        query.append(" LEFT JOIN "+ COMPETENCES_SCHEMA + ".competence_niveau_final ON " +
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

        Sql.getInstance().prepared(query.toString(), values,Competences.DELIVERY_OPTIONS,
                validResultHandler(handler));
    }

    private void runGetCompetencesNotesReleveTotale(String etablissementId, String classeId, List<String> matiereIds,
                                              Long periodeId,List<String> idEleves,
                                              Handler<Either<String, JsonArray>> handler) {

        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT ")
                .append(" devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, ")
                .append(" devoirs.diviseur, devoirs.ramener_sur, competences_notes.evaluation ,")
                .append(" competences_notes.id_competence , devoirs.id_matiere, competences_notes.id,")
                .append(" devoirs.id_periode, competences_notes.id_eleve, devoirs.is_evaluated, ")
                .append("null as annotation, ")
                .append("competence_niveau_final.niveau_final AS niveau_final, type.formative")
                .append(" FROM "+ COMPETENCES_SCHEMA +".devoirs ");

        query.append(" INNER JOIN "+ COMPETENCES_SCHEMA +".type ON (devoirs.id_type = type.id) ");

        query.append(" LEFT JOIN "+ COMPETENCES_SCHEMA +".competences_notes " +
                "ON (devoirs.id  = competences_notes.id_devoir " +
                "AND  competences_notes.id_eleve IN "+ Sql.listPrepared(idEleves)+ ")" );
        for (String idEleve : idEleves) {
            values.add(idEleve);
        }

        query.append(" INNER JOIN "+ COMPETENCES_SCHEMA +".rel_devoirs_groupes ON ");

        if(classeId != null ){
            JsonArray groupIds = new JsonArray();
            groupIds.add(classeId);
            query.append("(rel_devoirs_groupes.id_devoir = devoirs.id  AND rel_devoirs_groupes.id_groupe IN " + Sql.listPrepared( UtilsConvert.jsonArrayToStringArr(groupIds)) + " )");
            for (Object groupeId : groupIds) {
                values.add(groupeId);
            }

        }else{
            query.append("rel_devoirs_groupes.id_devoir = devoirs.id");
        }

        query.append(" LEFT JOIN "+ COMPETENCES_SCHEMA + ".competence_niveau_final ON " +
                "( competence_niveau_final.id_periode = devoirs.id_periode AND competence_niveau_final.id_eleve = competences_notes.id_eleve" +
                " AND competence_niveau_final.id_competence = competences_notes.id_competence AND competence_niveau_final.id_matiere = devoirs.id_matiere )");
        query.append(" WHERE devoirs.id_etablissement = ? ")
                .append(" AND devoirs.id_matiere IN "+Sql.listPrepared(matiereIds)+" ");

        values.add(etablissementId);

        for (String matiere : matiereIds) {
            values.add(matiere);
        }

        if(periodeId != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(periodeId);
        }

        Sql.getInstance().prepared(query.toString(), values,Competences.DELIVERY_OPTIONS,
                validResultHandler(handler));
    }

    @Override
    public void deleteColonneReleve(String idEleve, Long idPeriode, String idMatiere, String idClasse,
                                    String colonne,   Handler<Either<String, JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder()
                .append("DELETE FROM "+ COMPETENCES_SCHEMA +"."+ colonne )
                .append("moyenne".equals(colonne)? "_finale": " ")
                .append(" WHERE id_periode = ? AND id_eleve=?  AND id_classe=? AND id_matiere=? ");

        values.add(idPeriode).add(idEleve).add(idClasse).add(idMatiere);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }


    @Override
    public void getColonneReleve(JsonArray idEleves, Long idPeriode, String idMatiere, JsonArray idsClasse,
                                 String colonne, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        // le positionnement est enregistré pour un élève indépendament de sa classe
        // ou de ses groupes (positionnement global)
        // on le recupere donc sans filtre sur la classe
        if (colonne.equals(POSITIONNEMENT)) {
                query.append("SELECT id_periode, id_eleve," + colonne + ", id_matiere ");
            } else {
                query.append("SELECT id_periode, id_eleve," + colonne + ", id_classe, id_matiere ");
            }

            query.append(" FROM ")
                    .append(COMPETENCES_SCHEMA + "." + colonne + (MOYENNE.equals(colonne) ? "_finale" : " "))
                    .append(" WHERE   id_matiere = ? ");
            values.add(idMatiere);
            if (!colonne.equals(POSITIONNEMENT)) {
                query.append(" AND id_classe IN " + Sql.listPrepared(idsClasse.getList()));
                for (Object idClasse : idsClasse.getList()) {
                    values.add(idClasse);
                }

            }
            if (null != idEleves) {
                query.append(" AND id_eleve IN " + Sql.listPrepared(idEleves.getList().toArray()));
                for (int i = 0; i < idEleves.size(); i++) {
                    values.add(idEleves.getString(i));
                }
            }
            if (null != idPeriode) {
                query.append(" AND id_periode = ? ");
                values.add(idPeriode);
            }
        Sql.getInstance().prepared(query.toString(), values,
                new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L),
                validResultHandler(handler));
    }

    public void getColonneReleveTotale(JsonArray idEleves, Long idPeriode, JsonArray idsMatiere, JsonArray idsClasse,
                                        Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT moy.id_eleve, moy.id_periode, null as avis_conseil_orientation, null as avis_conseil_de_classe, null as synthese_bilan_periodique, null as positionnement, " +
                "moy.id_matiere, moy.moyenne, moy.id_classe FROM "+COMPETENCES_SCHEMA + ".moyenne_finale AS moy WHERE " );

        if (null != idEleves) {
            query.append("moy.id_eleve IN " + Sql.listPrepared(idEleves.getList().toArray()));
            for (int i = 0; i < idEleves.size(); i++) {
                values.add(idEleves.getString(i));
            }
        }
        if (null != idPeriode) {
            query.append(" AND moy.id_periode = ? ");
            values.add(idPeriode);
        }

        if (null != idsMatiere) {
            query.append(" AND moy.id_matiere IN " + Sql.listPrepared(idsMatiere.getList().toArray()));
            for (int i = 0; i < idsMatiere.size(); i++) {
                values.add(idsMatiere.getString(i));
            }
        }

        if (null != idsClasse) {
            query.append(" AND moy.id_classe IN " + Sql.listPrepared(idsClasse.getList()));
            for (Object idClasse : idsClasse.getList()) {
                values.add(idClasse);
            }
        }

        query.append(" UNION " +
                "SELECT pos.id_eleve, pos.id_periode, null as avis_conseil_orientation, null as avis_conseil_de_classe, " +
                "null as synthese_bilan_periodique, pos.positionnement, pos.id_matiere, null as moyenne, null as id_classe " +
                "FROM "+COMPETENCES_SCHEMA + ".positionnement AS pos WHERE " );

        if (null != idEleves) {
            query.append("pos.id_eleve IN " + Sql.listPrepared(idEleves.getList().toArray()));
            for (int i = 0; i < idEleves.size(); i++) {
                values.add(idEleves.getString(i));
            }
        }
        if (null != idPeriode) {
            query.append(" AND pos.id_periode = ? ");
            values.add(idPeriode);
        }

        if (null != idsMatiere) {
            query.append(" AND pos.id_matiere IN " + Sql.listPrepared(idsMatiere.getList().toArray()));
            for (int i = 0; i < idsMatiere.size(); i++) {
                values.add(idsMatiere.getString(i));
            }
        }

        query.append(" UNION " +
                "SELECT IdTableAvisOrientation.id_eleve, IdTableAvisOrientation.id_periode, libelleTableAvisOrientation.libelle as avis_conseil_orientation, " +
                "null as avis_conseil_de_classe, null as synthese_bilan_periodique, null as positionnement, null as id_matiere, null as moyenne, null as id_classe " +
                "FROM "+COMPETENCES_SCHEMA + ".avis_conseil_orientation AS IdTableAvisOrientation " +
                "JOIN "+COMPETENCES_SCHEMA +".avis_conseil_bilan_periodique AS libelleTableAvisOrientation ON " +
                "IdTableAvisOrientation.id_avis_conseil_bilan = libelleTableAvisOrientation.id WHERE ");

        if (null != idEleves) {
            query.append("IdTableAvisOrientation.id_eleve IN " + Sql.listPrepared(idEleves.getList().toArray()));
            for (int i = 0; i < idEleves.size(); i++) {
                values.add(idEleves.getString(i));
            }
        }
        if (null != idPeriode) {
            query.append(" AND IdTableAvisOrientation.id_periode = ? ");
            values.add(idPeriode);
        }

        query.append(" UNION " +
                "SELECT IdTableAvisConseil.id_eleve, IdTableAvisConseil.id_periode, null as avis_conseil_orientation," +
                "libelleTableAvisConseil.libelle as avis_conseil_de_classe, null as synthese_bilan_periodique, null as positionnement, null as id_matiere, " +
                "null as moyenne, null as id_classe FROM "+COMPETENCES_SCHEMA + ".avis_conseil_de_classe AS IdTableAvisConseil " +
                "JOIN "+COMPETENCES_SCHEMA +".avis_conseil_bilan_periodique AS libelleTableAvisConseil ON " +
                "IdTableAvisConseil.id_avis_conseil_bilan = libelleTableAvisConseil.id WHERE ");

        if (null != idEleves) {
            query.append("IdTableAvisConseil.id_eleve IN " + Sql.listPrepared(idEleves.getList().toArray()));
            for (int i = 0; i < idEleves.size(); i++) {
                values.add(idEleves.getString(i));
            }
        }
        if (null != idPeriode) {
            query.append(" AND IdTableAvisConseil.id_periode = ? ");
            values.add(idPeriode);
        }

        query.append("UNION " +
                "SELECT syntheseBP.id_eleve, syntheseBP.id_typeperiode as id_periode, null as avis_conseil_orientation, null as avis_conseil_de_classe," +
                "syntheseBP.synthese as synthese_bilan_periodique, null as positionnement, null as id_matiere, null as moyenne, null as id_classe " +
                "FROM "+COMPETENCES_SCHEMA + ".synthese_bilan_periodique AS syntheseBP WHERE " );

        if (null != idEleves) {
            query.append("syntheseBP.id_eleve IN " + Sql.listPrepared(idEleves.getList().toArray()));
            for (int i = 0; i < idEleves.size(); i++) {
                values.add(idEleves.getString(i));
            }
        }
        if (null != idPeriode) {
            query.append(" AND syntheseBP.id_typeperiode = ? ");
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
                .append("INSERT INTO "+ COMPETENCES_SCHEMA +"."+ colonne )
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
            JsonObject note = listNotes.getJsonObject(i);

            if (note.getString("valeur") == null
                    || !note.getBoolean("is_evaluated") || note.getString("coefficient") == null) {
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

    private String nullPositionnemetFinal() {
        return  " null AS positionnement_final,  null AS id_classe_posi,  null AS id_periode_positionnement, ";
    }

    private void getPositionnementFinal(String idEleve, String idMatiere, Long idPeriode,
                                        Handler<Either<String, JsonArray>> handler){
        String query = "SELECT positionnement.positionnement AS positionnement_final, " +
                " positionnement.id_classe AS id_classe_posi, " +
                nullMoyenneFinale() + nullAppreciation() +
                " positionnement.id_periode AS id_periode_positionnement " +
                " FROM notes.positionnement " +
                " WHERE " +
                ((idPeriode!=null)?" (positionnement.id_periode = ?) AND ":"") +
                "    (positionnement.id_eleve = ?) " +
                "     AND " +
                "     (positionnement.id_matiere = ?)";
        JsonArray params = new JsonArray();
        if(idPeriode!=null){
            params.add(idPeriode);
        }
        params.add(idEleve).add(idMatiere);
        Sql.getInstance().prepared( query, params,Competences.DELIVERY_OPTIONS, validResultHandler(handler));
    }

    private String nullMoyenneFinale() {
        return " null AS moyenne_finale,  null AS id_classe_moyfinale, null AS id_periode_moyenne_finale, ";
    }

    private void getMoyenneFinale(String idEleve, String idMatiere, Long idPeriode,
                                  Handler<Either<String, JsonArray>> handler){
        String query = "SELECT moyenne_finale.moyenne AS moyenne_finale, " +
                " moyenne_finale.id_classe AS id_classe_moyfinale, moyenne_finale.id_periode " +
                " AS id_periode_moyenne_finale, " +
                nullAppreciation() + nullPositionnemetFinal() +
                " moyenne_finale.id_eleve " +

                " FROM notes.moyenne_finale" +
                " WHERE " +
                ((idPeriode!=null)?"( moyenne_finale.id_periode = ?) AND ": "") +
                "    (moyenne_finale.id_eleve = ?) " +
                "     AND ( moyenne_finale.id_matiere = ?)";
        JsonArray params = new JsonArray();
        if(idPeriode!=null){
            params.add(idPeriode);
        }
        params.add(idEleve).add(idMatiere);
        Sql.getInstance().prepared( query, params,Competences.DELIVERY_OPTIONS, validResultHandler(handler));

    }

    private String nullAppreciation() {
        return " null AS appreciation_matiere_periode,  null AS id_classe_appreciation, " +
                " null AS id_periode_appreciation, " ;
    }
    private void getAppreciationMatierePeriode(String idEleve, String idMatiere, Long idPeriode,
                                               Handler<Either<String, JsonArray>> handler){
        String query = "SELECT appreciation_matiere_periode.appreciation_matiere_periode, " +
                " appreciation_matiere_periode.id_classe AS id_classe_appreciation, " +
                nullMoyenneFinale() + nullPositionnemetFinal() +
                " appreciation_matiere_periode.id_periode AS id_periode_appreciation " +
                " FROM notes.appreciation_matiere_periode " +
                " WHERE " +
                ((idPeriode!=null)?"(appreciation_matiere_periode.id_periode = ? ) AND ": "") +
                "    (appreciation_matiere_periode.id_eleve = ?) " +
                "     AND (appreciation_matiere_periode.id_matiere = ?) ";

        JsonArray params = new JsonArray();
        if(idPeriode!=null){
            params.add(idPeriode);
        }
        params.add(idEleve).add(idMatiere);
        Sql.getInstance().prepared( query, params,Competences.DELIVERY_OPTIONS, validResultHandler(handler));
    }
    private JsonObject leftJoin(JsonObject left, JsonObject right){
        JsonObject res = new JsonObject(left.getMap());
        for ( Map.Entry<String, Object> entry : right.getMap().entrySet()){
            if(entry.getValue() != null) {
                res.put(entry.getKey(), entry.getValue());
            }
        }
        return res;
    }


    private JsonArray fullJoinAppMoyPosiFinale( JsonArray appreciation ,JsonArray moyenneFinale,
                                                JsonArray positionnement ){
        JsonArray result = new JsonArray();
        Map<String, JsonObject> joinMap = new HashMap<>();
        // SELECT FROM appreciation
        for(int i =0; i<appreciation.size(); i++){
            JsonObject appreciationObj = appreciation.getJsonObject(i);
            String idClasse = appreciationObj.getString("id_classe_appreciation");
            Integer idPeriode = appreciationObj.getInteger("id_periode_appreciation");
            String key = idClasse+idPeriode;
            joinMap.put(key, appreciationObj);
        }
        // FULL JOIN  moyenneFinale
        for(int i =0; i<moyenneFinale.size(); i++){
            JsonObject moyenneFinaleObj = moyenneFinale.getJsonObject(i);
            String idClasse = moyenneFinaleObj.getString("id_classe_moyfinale");
            Integer idPeriode = moyenneFinaleObj.getInteger("id_periode_moyenne_finale");
            String key = idClasse+idPeriode;
            if(!joinMap.containsKey(key)) {
                joinMap.put(key, moyenneFinaleObj);
            }
            else {
                joinMap.replace(key, leftJoin(joinMap.get(key), moyenneFinaleObj));
            }
        }
        // FULL JOIN POSITIONNEMENT
        for(int i =0; i<positionnement.size(); i++){
            JsonObject positionnementObj = positionnement.getJsonObject(i);
            String idClasse = positionnementObj.getString("id_classe_posi");
            Integer idPeriode = positionnementObj.getInteger("id_periode_positionnement");
            String key = idClasse+idPeriode;
            if(!joinMap.containsKey(key)) {
                joinMap.put(key, positionnementObj);
            }
            else {
                joinMap.replace(key, leftJoin(joinMap.get(key), positionnementObj));
            }
        }

        // RETURNING
        for (Map.Entry<String, JsonObject> o : joinMap.entrySet()) {
            result.add(o.getValue());
        }

        return result;
    }

    @Override
    public void getAppreciationMoyFinalePositionnement(String idEleve, String idMatiere, Long idPeriode,
                                                       Handler<Either<String, JsonArray>> handler) {


        Future<JsonArray> appreciationFuture = Future.future();
        getAppreciationMatierePeriode(idEleve, idMatiere, idPeriode, event-> {
            FormateFutureEvent.formate(appreciationFuture, event);
        });

        Future<JsonArray> moyenneFinaleFuture = Future.future();
        getMoyenneFinale(idEleve, idMatiere, idPeriode, event -> {
            FormateFutureEvent.formate(moyenneFinaleFuture, event);
        });

        Future<JsonArray> positionnementFinalFuture = Future.future();
        getPositionnementFinal(idEleve, idMatiere, idPeriode, event -> {
            FormateFutureEvent.formate(positionnementFinalFuture, event);
        });
        CompositeFuture.all(appreciationFuture, moyenneFinaleFuture, positionnementFinalFuture).setHandler(event -> {
            if(event.succeeded()){
                JsonArray appreciation = appreciationFuture.result();
                JsonArray moyenneFinale = moyenneFinaleFuture.result();
                JsonArray positionnement = positionnementFinalFuture.result();
                JsonArray result = fullJoinAppMoyPosiFinale(appreciation, moyenneFinale, positionnement);
                handler.handle(new Either.Right<>(result));
            }
            else{
                handler.handle(new Either.Left<>(event.cause().getMessage()));
            }
        });
    }

    public Double calculMoyenneClasseByPeriode(ArrayList<NoteDevoir> allNotes,
                                               Map<Long, Map<String, Double>> moyFinalesElevesByPeriode,
                                               Long idPeriode){

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
        Integer nbEleve = notesPeriodeByEleves.size();
        Map<String, Double> moyFinalesPeriode = null;
        if( moyFinalesElevesByPeriode != null && moyFinalesElevesByPeriode.containsKey(idPeriode)) {
            moyFinalesPeriode = moyFinalesElevesByPeriode.get(idPeriode);
            //un élève peut ne pas être noté et avoir une moyenne finale
            //il faut donc ajouter sa moyenne finale à  sumMoyClasse
            for(Map.Entry<String,Double> moyFinale : moyFinalesPeriode.entrySet() ){

                if(!notesPeriodeByEleves.containsKey(moyFinale.getKey())){
                    sumMoyClasse += moyFinale.getValue();
                    nbEleve ++;
                }
            }
        }

        //pour tous les élèves qui ont une note mettre leur moyenne finale ou auto dans sumMoyClasse
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
        return (double) Math.round((sumMoyClasse/nbEleve) * 100) / 100;
    }

    public void calculAndSetMoyenneClasseByPeriode(final JsonArray idEleves, final JsonArray moyFinalesEleves,
                                                   final HashMap<Long,HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse,
                                                   final JsonObject result ) {
        JsonArray moyennesClasses = new fr.wseduc.webutils.collections.JsonArray();

        Map<Long, Map<String, Double>> moyFinales = null;

        if(moyFinalesEleves != null && moyFinalesEleves.size()>0) {
            moyFinales = new HashMap<Long, Map<String, Double>>();
            for(Object o : moyFinalesEleves) {
                JsonObject moyFinale = (JsonObject) o;
                Long periode = moyFinale.getLong("id_periode");

                if(!moyFinales.containsKey(periode)) {
                    moyFinales.put(periode, new HashMap<String, Double>());
                }
                moyFinales.get(periode).put(moyFinale.getString("id_eleve"),
                        Double.parseDouble(moyFinale.getString("moyenne")));
            }
            //cas où il n'y a que des compétences et avec des moyennes finales pour une période <=> notesByDevoirByPeriodeClasse n'a pas cette période
            //Pour cette periode, il faut calculer la moyenne de la classe à partir des moyennes finales
            //Donc on vérifie que pour chaque période où il y a des moyennes finale, il y a aussi des notes
            //sinon il faut calculer la moyenne de classe à partir des moyennes finales
            for(Map.Entry<Long,Map<String,Double>> mapMoysFinales : moyFinales.entrySet()){
                if(!notesByDevoirByPeriodeClasse.containsKey(mapMoysFinales.getKey())){
                    StatClass statClass = new StatClass();
                    for(Map.Entry<String,Double> moyFinale : mapMoysFinales.getValue().entrySet()){
                        statClass.putMapEleveStat(moyFinale.getKey(),moyFinale.getValue(),null);
                    }
                    moyennesClasses.add(new JsonObject().put("id",mapMoysFinales.getKey())
                            .put("moyenne", statClass.getAverageClass()));
                }
            }

        }
        //notesByDevoirByPeriodeClasse contient au moins la clé null
        //pour chaque période où il y a des notes on calcul la moyenne de la classe
        //dans ce calcul on tient compte qu'un élève peut avoir une moyenne finale et pas de note
        //il suffit donc d'une note sur un eleve et une période pour que la moyenne de la classe
        // soit calculée par la méthode calculMoyenneClasseByPeriode
        for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByPeriode :
                notesByDevoirByPeriodeClasse.entrySet()) {

            Long idPeriode = notesByPeriode.getKey();
            JsonObject moyennePeriodeClasse = new JsonObject();
            // calcul de la moyenne de la classe si on a des notes pour 1 trimestre
            if (idPeriode != null) {
                moyennePeriodeClasse.put("id", idPeriode);
                ArrayList<NoteDevoir> allNotes =
                        notesByPeriode.getValue().get(notesByPeriode.getKey());

                moyennePeriodeClasse.put("moyenne",
                        calculMoyenneClasseByPeriode(
                                allNotes,
                                moyFinales,
                                idPeriode));
                moyennesClasses.add(moyennePeriodeClasse);
            }
        }

        //si moyennesClasses.size()> 0 c'est qu'il y a eu soit des moyennees finales soit des notes sur au moins un trimestre
        // alors on peut calculer la moyenne de la classe pour l'année
        if(moyennesClasses.size()>0){
            Double sumMoyPeriode = 0.0;
            for(int i = 0; i < moyennesClasses.size(); i++){
                sumMoyPeriode += moyennesClasses.getJsonObject(i).getDouble("moyenne");
            }
            JsonObject moyennePeriodeClasse = new JsonObject();
            moyennePeriodeClasse.put("id", (JsonObject) null).put("moyenne",
                    (double) Math.round((sumMoyPeriode / moyennesClasses.size()) * 100) / 100);
            moyennesClasses.add(moyennePeriodeClasse);
        }
        result.put("moyennesClasse", moyennesClasses);

    }

    private Map<String, JsonArray> groupeNotesByStudent(JsonArray allNotes) {
        Map<String, JsonArray> notesByStudent = new HashMap<>();
        for (int i = 0; i < allNotes.size(); i++) {


            JsonObject note = allNotes.getJsonObject(i);
            String id_student = note.getString(ID_ELEVE);

            if (!notesByStudent.containsKey(id_student)) {
                notesByStudent.put(id_student, new JsonArray().add(note));
            } else {
                notesByStudent.get(id_student).add(note);
            }
        }
        return notesByStudent;
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
            Long id_periode = note.getLong(ID_PERIODE);
            if (!notesByDevoirByPeriode.containsKey(id_periode)) {
                notesByDevoirByPeriode.put(id_periode,
                        new HashMap<Long, ArrayList<NoteDevoir>>());

            }

            // si competence provenant d'un devoir de type formative, alors,
            // on l'exclue du calcul
            Boolean isFormative = note.getBoolean("formative");
            if (isFormative) {
                continue;
            }

            if (note.getLong("evaluation") == null
                    || note.getLong("evaluation") < 0) {
                continue; //Si pas de compétence Note
            }
            NoteDevoir noteDevoir;
            if (note.getLong("niveau_final") != null
                    && !idsCompetence.contains(note.getLong("id_competence"))) {

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
        result.put(POSITIONNEMENTS_AUTO, new fr.wseduc.webutils.collections.JsonArray());
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
                result.getJsonArray(POSITIONNEMENTS_AUTO).add(
                        listMoyDevoirs.get(entryPeriode.getKey()).getJsonObject(0));
            } else {
                result.getJsonArray(POSITIONNEMENTS_AUTO).add(new JsonObject()
                        .put("moyenne", -1)
                        .put("id_periode", entryPeriode.getKey()));
            }
        }
    }


    @Override
    public void getMoyennesFinal(String[] idEleves, Integer idPeriode,
                                 String[] idMatieres, String[] idClasses, Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT * FROM " + COMPETENCES_SCHEMA + ".moyenne_finale";
        String condition = "";
        JsonArray values = new JsonArray();

        if ((idEleves != null && idEleves.length > 0)
                || (idMatieres != null && idMatieres.length > 0)
                || (idClasses != null && idClasses.length > 0)
                || idPeriode != null) {
            condition += " WHERE ";
            if (idEleves != null && idEleves.length > 0) {
                condition += "id_eleve IN " + Sql.listPrepared(idEleves) + " AND ";
                Arrays.stream(idEleves).forEach(values::add);
            }

            if (idMatieres != null && idMatieres.length > 0) {
                condition += "id_matiere IN " + Sql.listPrepared(idMatieres) + " AND ";
                Arrays.stream(idMatieres).forEach(values::add);
            }

            if (idClasses != null && idClasses.length > 0) {
                condition += "id_classe IN " + Sql.listPrepared(idClasses) + " AND ";
                Arrays.stream(idClasses).forEach(values::add);
            }

            if (idPeriode != null) {
                condition += "id_periode = ?";
                values.add(idPeriode);
            }

            if (condition.endsWith(" AND ")) {
                condition = condition.substring(0, condition.length() - 5);
            }
        }

        Sql.getInstance().prepared(query + condition, values, validResultHandler(handler));
    }

    @Override
    public void getNotesAndMoyFinaleByClasseAndPeriode(List<String> idsEleve, JsonArray idsGroups, Integer idPeriode, Handler<Either<String, JsonArray>> handler) {
        JsonArray values =new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT * FROM "+
                "(SELECT notes.id_eleve AS id_eleve_notes, devoirs.id, devoirs.id_periode, devoirs.id_matiere, devoirs.owner, rel_devoirs_groupes.id_groupe, "+
                "rel_devoirs_groupes.type_groupe, devoirs.coefficient, devoirs.diviseur, devoirs.ramener_sur, notes.valeur " +
                "FROM notes.devoirs LEFT JOIN notes.notes ON (devoirs.id = notes.id_devoir AND "+
                "notes.id_eleve IN  "+Sql.listPrepared(idsEleve)+" ) " +
                "INNER JOIN notes.rel_devoirs_groupes ON (devoirs.id = rel_devoirs_groupes.id_devoir AND "+
                "rel_devoirs_groupes.id_groupe IN "+Sql.listPrepared(idsGroups.getList())+" ) " ;

        for (String idEleve: idsEleve ) {
            values.add(idEleve);
        }
        for (Object idGroupe: idsGroups.getList()) {
            values.add(idGroupe);
        }
        if(idPeriode != null){
            query += "WHERE devoirs.id_periode = ?";
            values.add(idPeriode);
        }
        query += " ORDER BY notes.id_eleve , devoirs.id_matiere ) AS devoirs_notes " +
                "FULL JOIN ( SELECT moyenne_finale.id_periode AS id_periode_moyf, moyenne_finale.id_eleve AS id_eleve_moyf, "+
                "moyenne_finale.moyenne AS moyenne_finale, moyenne_finale.id_matiere AS id_mat_moyf " +
                "FROM notes.moyenne_finale WHERE moyenne_finale.id_eleve IN "+ Sql.listPrepared(idsEleve)+
                ((idPeriode != null)? " AND moyenne_finale.id_periode = ? )": ")")+" AS moyf "+
                "ON ( devoirs_notes.id_matiere = moyf.id_mat_moyf AND devoirs_notes.id_eleve_notes = moyf.id_eleve_moyf "+
                "AND devoirs_notes.id_periode = moyf.id_periode_moyf )";
        for (String idEleve: idsEleve ) {
            values.add(idEleve);
        }
        if(idPeriode != null){
            values.add(idPeriode);
        }
        Sql.getInstance().prepared(query, values,validResultHandler(handler));
    }

    @Override
    public void getMoysEleveByMat(String idClasse, Integer idPeriode,
                                  SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
                                  Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve,
                                  Handler<Either<String,JsonObject>> handler) {


        List<String> idsEleve = new ArrayList();

        Utils.getEleveClasse(eb, idsEleve, idClasse, idPeriode, new Handler<Either<String, List<Eleve>>>() {
                    @Override
                    public void handle(Either<String, List<Eleve>> responseListEleve) {

                        if (responseListEleve.isLeft()) {
                            handler.handle(new Either.Left<>("eleves not found"));
                            log.error(responseListEleve.left().getValue());

                        } else {
                            List<Eleve> eleves = responseListEleve.right().getValue();

                            if (idsEleve.size() == 0) {
                                handler.handle(new Either.Left<>("eleves not found"));
                            } else {
                                //on récupère les groups de la classe
                                Utils.getGroupesClasse(eb, new fr.wseduc.webutils.collections
                                                .JsonArray()
                                                .add(idClasse),
                                        new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(
                                                    Either<String, JsonArray> responseQuerry) {
                                                //List qui contient la idClasse + tous les ids groupes
                                                // de la classe
                                                JsonArray idsGroups = new fr.wseduc.webutils.collections.JsonArray();
                                                final String nameClasse;

                                                if (responseQuerry.isLeft()) {
                                                    String error = responseQuerry.left().getValue();
                                                    log.error(error);
                                                    handler.handle(new Either.Left<>(error));
                                                } else {
                                                    JsonArray idClasseGroups = responseQuerry.right().getValue();

                                                    if (!(idClasseGroups != null && !idClasseGroups.isEmpty())) {
                                                        idsGroups.add(idClasse);
                                                    } else {
                                                        idsGroups.add( idClasseGroups.getJsonObject(0)
                                                                .getString("id_classe") );
                                                        idsGroups.addAll(idClasseGroups.getJsonObject(0)
                                                                .getJsonArray("id_groupes"));
                                                        nameClasse = idClasseGroups.getJsonObject(0)
                                                                .getString("name_classe");

                                                        // 2- On récupère les notes des eleves
                                                        getNotesAndMoyFinaleByClasseAndPeriode(idsEleve, idsGroups, idPeriode, new Handler<Either<String, JsonArray>>() {
                                                                    @Override
                                                                    public void handle(Either<String, JsonArray> response) {
                                                                        if (response.isLeft()) {
                                                                            handler.handle(new Either.Left<>("eval not found"));
                                                                            log.error(response.left().getValue());

                                                                        } else {
                                                                            JsonArray respNotesMoysFinales = response.right().getValue();
                                                                            if (respNotesMoysFinales.size() == 0) {
                                                                                handler.handle(new Either.Left<>("eval not found"));
                                                                            } else {
                                                                                Map<String, Map<String, Double>> mapIdEleveIdMatMoy = new HashMap<>();
                                                                                Map<String, Map<String, List<NoteDevoir>>> mapIdEleveIdMatListNotes = new HashMap<>();

                                                                                for (int i = 0; i < respNotesMoysFinales.size(); i++) {
                                                                                    JsonObject respNoteMoyFinale = respNotesMoysFinales.getJsonObject(i);
                                                                                    //récupérer les moysFinales => set mapIdEleveIdMatMoy
                                                                                    if (respNoteMoyFinale.getString("moyenne_finale") != null) {

                                                                                        if (mapIdEleveIdMatMoy.containsKey(respNoteMoyFinale.getString("id_eleve_moyf"))) {
                                                                                            Map<String, Double> mapIdMatMoy = mapIdEleveIdMatMoy.get(respNoteMoyFinale.getString("id_eleve_moyf"));
                                                                                            // meme eleve changement de matiere
                                                                                            if (!mapIdMatMoy.containsKey(respNoteMoyFinale.getString("id_mat_moyf"))) {
                                                                                                mapIdMatMoy.put(respNoteMoyFinale.getString("id_mat_moyf"),
                                                                                                        Double.valueOf(respNoteMoyFinale.getString("moyenne_finale")));
                                                                                            }
                                                                                        } else {//nouvel eleve
                                                                                            Map<String, Double> newMapIdMatMoy = new HashMap<>();
                                                                                            newMapIdMatMoy.put(respNoteMoyFinale.getString("id_mat_moyf"),
                                                                                                    Double.valueOf(respNoteMoyFinale.getString("moyenne_finale")));
                                                                                            mapIdEleveIdMatMoy.put(respNoteMoyFinale.getString("id_eleve_moyf"), newMapIdMatMoy);
                                                                                        }

                                                                                    } else {//pas de moyFinale => set mapIdEleveIdMatListNotes
                                                                                        if (respNoteMoyFinale.getString("coefficient") == null){
                                                                                            continue;
                                                                                        }
                                                                                        if(respNoteMoyFinale.getString("id_eleve_notes")!= null){
                                                                                            if (mapIdEleveIdMatListNotes.containsKey(respNoteMoyFinale.getString("id_eleve_notes"))) {

                                                                                                Map<String, List<NoteDevoir>> mapIdMatListNotes =
                                                                                                        mapIdEleveIdMatListNotes.get(respNoteMoyFinale.getString("id_eleve_notes"));
                                                                                                if (mapIdMatListNotes.containsKey(respNoteMoyFinale.getString("id_matiere"))) {

                                                                                                    mapIdMatListNotes.get(respNoteMoyFinale.getString("id_matiere"))
                                                                                                            .add(new NoteDevoir(
                                                                                                                    Double.valueOf(respNoteMoyFinale.getString("valeur")),
                                                                                                                    Double.valueOf(respNoteMoyFinale.getInteger("diviseur")),
                                                                                                                    respNoteMoyFinale.getBoolean("ramener_sur"),
                                                                                                                    Double.valueOf(respNoteMoyFinale.getString("coefficient"))));

                                                                                                } else {//nouvelle matière dc nouvelle liste de notes
                                                                                                    List<NoteDevoir> newListNotes = new ArrayList<>();
                                                                                                    newListNotes.add(new NoteDevoir(
                                                                                                            Double.valueOf(respNoteMoyFinale.getString("valeur")),
                                                                                                            Double.valueOf(respNoteMoyFinale.getInteger("diviseur")),
                                                                                                            respNoteMoyFinale.getBoolean("ramener_sur"),
                                                                                                            Double.valueOf(respNoteMoyFinale.getString("coefficient"))));
                                                                                                    mapIdMatListNotes.put(
                                                                                                            respNoteMoyFinale.getString("id_matiere"),
                                                                                                            newListNotes);
                                                                                                }
                                                                                            } else {//nouvel élève dc nelle map idMat-listnotes
                                                                                                Map<String, List<NoteDevoir>> newMapIdMatListNotes = new HashMap<>();
                                                                                                List<NoteDevoir> newListNotes = new ArrayList<>();
                                                                                                newListNotes.add(new NoteDevoir(
                                                                                                        Double.valueOf(respNoteMoyFinale.getString("valeur")),
                                                                                                        Double.valueOf(respNoteMoyFinale.getInteger("diviseur")),
                                                                                                        respNoteMoyFinale.getBoolean("ramener_sur"),
                                                                                                        Double.valueOf(respNoteMoyFinale.getString("coefficient"))));
                                                                                                newMapIdMatListNotes.put(
                                                                                                        respNoteMoyFinale.getString("id_matiere"),
                                                                                                        newListNotes);
                                                                                                mapIdEleveIdMatListNotes.put(respNoteMoyFinale.getString("id_eleve_notes"),
                                                                                                        newMapIdMatListNotes);

                                                                                            }
                                                                                        }

                                                                                    }
                                                                                    if(respNoteMoyFinale.getString("id_matiere") != null){
                                                                                        if (mapAllidMatAndidTeachers.containsKey(respNoteMoyFinale.getString("id_matiere"))) {
                                                                                            if (!mapAllidMatAndidTeachers.get(respNoteMoyFinale.getString("id_matiere"))
                                                                                                    .contains(respNoteMoyFinale.getString("owner"))) {
                                                                                                mapAllidMatAndidTeachers.get(respNoteMoyFinale.getString("id_matiere"))
                                                                                                        .add(respNoteMoyFinale.getString("owner"));
                                                                                            }
                                                                                        } else {
                                                                                            Set<String> listIdsTeacher = new HashSet();
                                                                                            listIdsTeacher.add(respNoteMoyFinale.getString("owner"));
                                                                                            mapAllidMatAndidTeachers.put(respNoteMoyFinale.getString("id_matiere"),
                                                                                                    listIdsTeacher);
                                                                                        }
                                                                                    }
                                                                                }

                                                                                //3 - calculate average by eleve by mat with mapIdEleveIdMatListNotes and set result in mapIdEleveIdMatMoy
                                                                                for (Map.Entry<String, Map<String, List<NoteDevoir>>> stringMapEntry : mapIdEleveIdMatListNotes.entrySet()) {

                                                                                    for (Map.Entry<String, List<NoteDevoir>> stringListEntry : stringMapEntry.getValue().entrySet()) {

                                                                                        List<NoteDevoir> noteDevoirList = stringListEntry.getValue();
                                                                                        Double moy = utilsService.calculMoyenne(noteDevoirList, false, 20).getDouble("moyenne");
                                                                                        if (mapIdEleveIdMatMoy.containsKey(stringMapEntry.getKey())) {
                                                                                            mapIdEleveIdMatMoy.get(stringMapEntry.getKey())
                                                                                                    .put(stringListEntry.getKey(), moy);
                                                                                        } else {
                                                                                            Map<String, Double> mapIdMatMoy = new HashMap<>();
                                                                                            mapIdMatMoy.put(stringListEntry.getKey(), moy);
                                                                                            mapIdEleveIdMatMoy.put(stringMapEntry.getKey(), mapIdMatMoy);
                                                                                        }
                                                                                    }
                                                                                }

                                                                                //4- il faut parcourir la mapIdMatIdsTeacher pour garder l'ordre des matieres pour tester qu l'élève à bien ttes les matières
                                                                                JsonArray elevesResult = new fr.wseduc.webutils.collections.JsonArray();
                                                                                List<NoteDevoir> listMoyGeneraleEleve = new ArrayList<>();

                                                                                // mapIdEleveListMoyByMat /  eleves = Liste<eleve>
                                                                                for (Eleve eleve : eleves) {
                                                                                    JsonObject eleveJsonO = new JsonObject();
                                                                                    JsonArray eleveMoyByMat = new fr.wseduc.webutils.collections.JsonArray();

                                                                                    eleveJsonO.put("id_eleve", eleve.getIdEleve())
                                                                                            .put("lastName", eleve.getLastName())
                                                                                            .put("firstName", eleve.getFirstName())
                                                                                            .put("nameClasse", nameClasse);

                                                                                    if (mapIdEleveIdMatMoy.containsKey(eleve.getIdEleve())) {
                                                                                        Map<String, Double> mapIdMatMoy = mapIdEleveIdMatMoy.get(eleve.getIdEleve());
                                                                                        List<NoteDevoir> listMoysEleve = new ArrayList<>();
                                                                                        // on parcours les matieres evaluees
                                                                                        for (Map.Entry<String, Set<String>> setEntry : mapAllidMatAndidTeachers.entrySet()) {
                                                                                            String idMatOfAllMat = setEntry.getKey();
                                                                                            //si la mat en cours est ds la map de eleve alors eleve a ete evalue pour cette mat
                                                                                            if (mapIdMatMoy.containsKey(idMatOfAllMat)) {
                                                                                                //on récupère la moy de l'élève pour idmat en cours de toutes les matieres
                                                                                                eleveMoyByMat.add(
                                                                                                        new JsonObject()
                                                                                                                .put("id_matiere", idMatOfAllMat)
                                                                                                                .put("moyenneByMat", mapIdMatMoy.get(idMatOfAllMat)));

                                                                                                listMoysEleve.add(new NoteDevoir(
                                                                                                        mapIdMatMoy.get(idMatOfAllMat),
                                                                                                        new Double(20),
                                                                                                        false,
                                                                                                        null));

                                                                                                if (mapIdMatListMoyByEleve.containsKey(idMatOfAllMat)) {
                                                                                                    mapIdMatListMoyByEleve.get(idMatOfAllMat)
                                                                                                            .add(new NoteDevoir(
                                                                                                                    mapIdMatMoy.get(idMatOfAllMat),
                                                                                                                    new Double(20),
                                                                                                                    false,
                                                                                                                    null));
                                                                                                } else {
                                                                                                    List<NoteDevoir> listMoyEleve = new ArrayList<>();
                                                                                                    listMoyEleve.add(new NoteDevoir(
                                                                                                            mapIdMatMoy.get(idMatOfAllMat),
                                                                                                            new Double(20),
                                                                                                            false,
                                                                                                            null));
                                                                                                    mapIdMatListMoyByEleve.put(idMatOfAllMat, listMoyEleve);
                                                                                                }
                                                                                            } else {//sinon l'eleve n'a pas ete evalue pour cette matiere
                                                                                                eleveMoyByMat.add(
                                                                                                        new JsonObject()
                                                                                                                .put("id_matiere", idMatOfAllMat)
                                                                                                                .put("moyenneByMat", "NN"));
                                                                                            }
                                                                                        }
                                                                                        eleveJsonO.put("eleveMoyByMat", eleveMoyByMat);
                                                                                        Double moyGeneraleEleve = utilsService.calculMoyenneParDiviseur(
                                                                                                listMoysEleve,
                                                                                                false)
                                                                                                .getDouble("moyenne");
                                                                                        eleveJsonO.put("moyGeneraleEleve", moyGeneraleEleve);
                                                                                        //ajouter cette moyG a une liste de moyGeleve pour le calcul moyGClasse
                                                                                        listMoyGeneraleEleve.add(new NoteDevoir(
                                                                                                moyGeneraleEleve,
                                                                                                new Double(20),
                                                                                                false,
                                                                                                null));
                                                                                    } else {//eleve n'a eu aucune evaluation sur aucune matiere dc pour toutes les matieres evaluees il aura NN

                                                                                        for (Map.Entry<String, Set<String>> setEntry : mapAllidMatAndidTeachers.entrySet()) {
                                                                                            String idMatOfAllMat = setEntry.getKey();

                                                                                            eleveMoyByMat.add(
                                                                                                    new JsonObject()
                                                                                                            .put("id_matiere", idMatOfAllMat)
                                                                                                            .put("moyenneByMat", "NN"));
                                                                                            eleveJsonO.put("eleveMoyByMat", eleveMoyByMat);
                                                                                            eleveJsonO.put("moyGeneraleEleve", "NN");
                                                                                        }
                                                                                    }

                                                                                    elevesResult.add(eleveJsonO);
                                                                                }
                                                                                JsonObject resultMoysElevesByMat = new JsonObject();
                                                                                if(listMoyGeneraleEleve.size() > 0){
                                                                                    resultMoysElevesByMat.put("moyClasAllEleves",
                                                                                            utilsService.calculMoyenneParDiviseur(
                                                                                                    listMoyGeneraleEleve,
                                                                                                    false).getDouble("moyenne"));
                                                                                }else{
                                                                                    resultMoysElevesByMat.put("moyClasAllEleves","");
                                                                                }
                                                                                resultMoysElevesByMat.put("eleves", elevesResult);
                                                                                resultMoysElevesByMat.put("nbEleves", elevesResult.size());
                                                                                handler.handle(new Either.Right<>(resultMoysElevesByMat));
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                        );


                                                    }
                                                }
                                            }
                                        });




                            }
                        }
                    }
                }
        );
    }

    @Override
    public void getMatEvaluatedAndStat( SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
                                        Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve,
                                        Handler<Either<String, JsonObject>> handler) {
        utilsService.getLibelleMatAndTeacher(mapAllidMatAndidTeachers, new Handler<Either<String, SortedMap<String, JsonObject>>>() {
            @Override
            public void handle(Either<String, SortedMap<String, JsonObject>> event) {

                if (!event.isRight()) {
                    log.error(event.left().getValue());
                    handler.handle(new Either.Left(event.left()));
                } else {

                    SortedMap<String, JsonObject> mapRespMatTeacher = event.right().getValue();
                    JsonArray matieresResult = new fr.wseduc.webutils.collections.JsonArray();
                    List<NoteDevoir> listMoyClass = new ArrayList<>();
                    List<NoteDevoir> listMoyMinClass = new ArrayList<>();
                    List<NoteDevoir> listMoyMaxClass = new ArrayList<>();

                    for (Map.Entry<String, Set<String>> mapEntry : mapAllidMatAndidTeachers.entrySet()) {

                        String idMatAllMats = mapEntry.getKey();
                        JsonObject matiereJson = (JsonObject) mapRespMatTeacher.get(idMatAllMats);
                        JsonObject statClass = new JsonObject();
                        if(mapIdMatListMoyByEleve.containsKey(idMatAllMats)) {
                            statClass = utilsService.calculMoyenneParDiviseur(
                                    mapIdMatListMoyByEleve.get(idMatAllMats),
                                    true);
                            matiereJson.put("moyClass", statClass.getDouble("moyenne"));
                            matiereJson.put("moyMinClass", statClass.getDouble("noteMin"));
                            matiereJson.put("moyMaxClass", statClass.getDouble("noteMax"));
                            listMoyClass.add(new NoteDevoir(
                                    statClass.getDouble("moyenne"),
                                    new Double(20),
                                    false, null));
                            listMoyMinClass.add(new NoteDevoir(
                                    statClass.getDouble("noteMin"),
                                    new Double(20),
                                    false, null));
                            listMoyMaxClass.add(new NoteDevoir(
                                    statClass.getDouble("noteMax"),
                                    new Double(20),
                                    false, null));

                        }else{
                            matiereJson.put("moyClass", "");
                            matiereJson.put("moyMinClass", "");
                            matiereJson.put("moyMaxClass", "");
                        }


                        matieresResult.add(matiereJson);

                    }
                    JsonObject resultMatieres = new JsonObject();
                    resultMatieres.put("matieres", matieresResult);
                    if(!listMoyClass.isEmpty()){
                        resultMatieres.put("moyClassAllMat",
                                utilsService.calculMoyenneParDiviseur(
                                        listMoyClass,
                                        false).getDouble("moyenne"));
                        resultMatieres.put("moyMinClassAllMat",
                                utilsService.calculMoyenneParDiviseur(
                                        listMoyMinClass,
                                        false).getDouble("moyenne"));
                        resultMatieres.put("moyMaxClassAllMat",
                                utilsService.calculMoyenneParDiviseur(
                                        listMoyMaxClass,
                                        false).getDouble("moyenne"));
                    }else{
                        resultMatieres.put("moyClassAllMat","");
                        resultMatieres.put("moyMinClassAllMat","");
                        resultMatieres.put("moyMaxClassAllMat", "");
                    }
                    resultMatieres.put("nbDeMatieres", matieresResult.size());

                    handler.handle(new Either.Right<>(resultMatieres));
                }
            }
        });
    }

    private String getLibelle(String key) {
        return I18n.getInstance().translate(key,
                I18n.DEFAULT_DOMAIN, Locale.FRANCE);
    }

    private void putLibelleForExport(JsonObject object) {
        object.put("appreciationClasseLibelle", getLibelle("evaluations.releve.appreciation.classe"));
        object.put("moyenneClasseLibelle", getLibelle("average.class"));
        object.put("exportReleveLibelle", getLibelle("evaluations.releve.title"));
        object.put("displayNameLibelle", getLibelle("students"));
        object.put("moyenneLibelle", getLibelle("average.auto.min"));
        object.put("moyenneFinaleLibelle", getLibelle("average.final.min"));
        object.put("positionnementFinaleLibelle", getLibelle("evaluations.releve.positionnement.min"));
        object.put("appreciationLibelle", getLibelle("viescolaire.utils.appreciations"));
        object.put("title",getLibelle("evaluations.export.releve"));
        object.put("appreciationClasseLibelle", getLibelle("evaluations.releve.appreciation.classe"));

    }

    private void putParamsForExport(JsonObject object, JsonObject params) {
        for (Map.Entry<String, Object> entry : params.getMap().entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                object.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void putLibelleAndParamsForExportReleve(JsonObject resultFinal, JsonObject params) {
        JsonArray students = resultFinal.getJsonArray(ELEVES);
        putLibelleForExport(resultFinal);
        putParamsForExport(resultFinal, params);
        resultFinal.put(CLASSE_NAME_KEY, params.getString(CLASSE_NAME_KEY));
        resultFinal.put(MATIERE_TABLE, params.getString(MATIERE_TABLE));
        resultFinal.put(COLSPAN, params.getValue(COLSPAN));
        for(int i = 0; i <students.size(); i++) {
            JsonObject student = students.getJsonObject(i);
            if(i==0) {
                Boolean hasLevel = student.getString(LEVEL) != null;
                resultFinal.put("hasLevel", hasLevel);
                if(hasLevel) {
                    resultFinal.put(LEVEL, student.getString(LEVEL).charAt(0));
                }
            }
            putParamsForExport(student, params);
        }
    }

    public void getDatasReleve(final JsonObject params, final Handler<Either<String, JsonObject>> handler){
        final String idEtablissement = params.getString(Competences.ID_ETABLISSEMENT_KEY);
        final String idClasse = params.getString(Competences.ID_CLASSE_KEY);
        final String idMatiere = params.getString(Competences.ID_MATIERE_KEY);
        String idEleve = params.getString(ID_ELEVE_KEY);
        final Long idPeriode = params.getLong(Competences.ID_PERIODE_KEY);
        final Integer typeClasse = params.getInteger(Competences.TYPE_CLASSE_KEY);
        final JsonArray idEleves = new fr.wseduc.webutils.collections.JsonArray();
        final JsonObject resultHandler = new JsonObject();
        Map<String, JsonObject> elevesMapObject = new HashMap<>();
        Boolean isExport = (params.getString("fileType") != null);

        // Récupération des éléments du programme
        Future<JsonObject> elementProgrammeFuture = Future.future();
        new DefaultElementProgramme().getElementProgramme(idPeriode,idMatiere,idClasse,
                event ->  {
                    FormateFutureEvent.formate(elementProgrammeFuture, event);
                });

        // Récupération des élèves de la classe
        Future<JsonArray> studentsClassFuture =  Future.future();
        if (idEleve == null) {
            getStudentClassForExportReleve(idClasse, idPeriode, idEleves, typeClasse,
                    elevesMapObject, studentsClassFuture);
        }
        else {
            studentsClassFuture.complete(new JsonArray().add(idEleve));
        }

        // Récupération du  nombre de devoirs avec évaluation numérique
        Future<JsonObject> nbEvaluatedHomeWork = Future.future();
        getNbEvaluatedHomeWork(idClasse, idMatiere, idPeriode, event -> {
            FormateFutureEvent.formate(nbEvaluatedHomeWork, event);
        });

        // Récupération du tableau de conversion
        Future<JsonArray> tableauDeConversionFuture = Future.future();
        // On récupère le tableau de conversion des compétences notes pour Lire le positionnement
        new DefaultCompetenceNoteService(COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE)
                .getConversionNoteCompetence(idEtablissement, idClasse,
                        tableauEvent -> {
                            FormateFutureEvent.formate(tableauDeConversionFuture, tableauEvent);
                        });


        // Récupération de l'appréciation de la classe
        Future<JsonArray> appreciationClassFuture = Future.future();
        if (idPeriode != null) {
            new DefaultAppreciationService(Competences.COMPETENCES_SCHEMA,
                    Competences.APPRECIATIONS_TABLE).getAppreciationClasse(
                    new String[]{idClasse},
                    Integer.valueOf(idPeriode.intValue()),
                    new String[]{idMatiere}, appreciationsEither -> {
                        FormateFutureEvent.formate(appreciationClassFuture, appreciationsEither);
                    });
        }
        else {
            appreciationClassFuture.complete(new JsonArray());
        }

        // Avec les ids des élèves de la classe, récupération des moyennes Finales , des Notes, des Competences Notes
        // et des Appreciations et des Positionnements finaux
        CompositeFuture.all(studentsClassFuture, appreciationClassFuture, tableauDeConversionFuture,
                nbEvaluatedHomeWork)
                .setHandler( idElevesEvent -> {
                    if(idElevesEvent.succeeded()) {
                        resultHandler.put(TABLE_CONVERSION_KEY, tableauDeConversionFuture.result());

                        resultHandler.put(ELEMENT_PROGRAMME_KEY, elementProgrammeFuture.result());
                        JsonObject appClasse = utilsService.getObjectForPeriode(
                                appreciationClassFuture.result(), idPeriode, ID_PERIODE);
                        if (appClasse == null) {
                            appClasse = new JsonObject().put("appreciation", " ");
                        }
                        resultHandler.put(APPRECIATION_CLASSE, appClasse);

                        // Récupération des moyennes Finales
                        Future<JsonArray> moyennesFinalesFutures = Future.future();
                        getColonneReleve(idEleves, idPeriode, idMatiere, new JsonArray().add(idClasse), MOYENNE,
                                moyennesFinalesEvent -> {
                                    FormateFutureEvent.formate(moyennesFinalesFutures, moyennesFinalesEvent);
                                });

                        Future<JsonArray> appreciationsFutures = Future.future();
                        if (idPeriode != null) {
                            getColonneReleve(idEleves, idPeriode, idMatiere, new JsonArray().add(idClasse),
                                    APPRECIATION_MATIERE_PERIODE,
                                    appreciationEvent -> {
                                        FormateFutureEvent.formate(appreciationsFutures, appreciationEvent);
                                    });
                        }
                        else {
                            appreciationsFutures.complete(new JsonArray());
                        }
                        // Récupération des positionnements finaux
                        Future<JsonArray> positionnementsFinauxFutures = Future.future();
                        if(idPeriode != null) {
                            getColonneReleve(idEleves, idPeriode, idMatiere, new JsonArray().add(idClasse),
                                    POSITIONNEMENT,
                                    positionnementsFinauxEvent -> {
                                        FormateFutureEvent.formate(positionnementsFinauxFutures,
                                                positionnementsFinauxEvent);
                                    });

                        }
                        else {
                            positionnementsFinauxFutures.complete(new JsonArray());
                        }
                        // Récupération des Notes du Relevé
                        Future<JsonArray> notesFuture = Future.future();
                        Boolean hasEvaluatedHomeWork = (nbEvaluatedHomeWork.result().getLong("nb") > 0);
                        if(idEleve == null) {
                            getNotesReleveEleves(idEleves, idEtablissement, idClasse, idMatiere, idPeriode, typeClasse,
                                    false, null,
                                    (Either<String, JsonArray> notesEvent) -> {
                                        FormateFutureEvent.formate(notesFuture, notesEvent);
                                    });
                        }
                        else {
                            if (!hasEvaluatedHomeWork) {
                                notesFuture.complete(new JsonArray());
                            }
                            else {
                                if (idPeriode != null) {
                                    getNoteElevePeriode(idEleve, idEtablissement,
                                            new fr.wseduc.webutils.collections.JsonArray().add(idClasse), idMatiere,
                                            idPeriode,
                                            notesEvent -> {
                                                FormateFutureEvent.formate(notesFuture, notesEvent);
                                            });
                                }
                                else {
                                    getNotesReleve(idEtablissement, idClasse,idMatiere,
                                            null, typeClasse,false,
                                            null, notesEvent -> {
                                                FormateFutureEvent.formate(notesFuture, notesEvent);
                                            });
                                }
                            }
                        }

                        // Récupération des Compétences-Notes du Relevé
                        Future<JsonArray> compNotesFuture = Future.future();
                        getCompetencesNotesReleveEleves(idEleves, idEtablissement, idClasse,
                                null, idMatiere, idPeriode,null,
                                typeClasse,false, compNotesEvent -> {
                                    FormateFutureEvent.formate(compNotesFuture, compNotesEvent);
                                });

                        List<Future> listFutures = new ArrayList<>(
                                Arrays.asList(compNotesFuture,notesFuture, moyennesFinalesFutures, appreciationsFutures,
                                        positionnementsFinauxFutures)
                        );
                        CompositeFuture.all(listFutures).setHandler( event -> {
                            if(event.succeeded()) {
                                // Rajout des moyennes finales
                                FormateColonneFinaleReleve(moyennesFinalesFutures.result(), elevesMapObject,
                                        MOYENNE, idPeriode, hasEvaluatedHomeWork);

                                // Rajout des notes par devoir et Calcul des moyennes auto
                                resultHandler.put(NOTES, notesFuture.result());
                                calculMoyennesNotesFOrReleve(notesFuture.result(), resultHandler,idPeriode,
                                        elevesMapObject, hasEvaluatedHomeWork, isExport);

                                // positionne
                                resultHandler.put(COMPETENCES_NOTES_KEY, compNotesFuture.result());
                                if (idPeriode != null) {
                                    // Cacul du positionnement auto
                                    calculMoyennesCompetencesNotesFOrReleve(compNotesFuture.result(), resultHandler,
                                            idPeriode, tableauDeConversionFuture.result(), elevesMapObject);


                                    // Rajout des positionnements finaux
                                    FormateColonneFinaleReleve(positionnementsFinauxFutures.result(), elevesMapObject,
                                            POSITIONNEMENT, idPeriode, hasEvaluatedHomeWork);

                                    resultHandler.put(APPRECIATIONS, appreciationsFutures.result());
                                    FormateColonneFinaleReleve(appreciationsFutures.result(), elevesMapObject,
                                            APPRECIATION_MATIERE_PERIODE, idPeriode, hasEvaluatedHomeWork);

                                }
                                handler.handle(new Either.Right<>(resultHandler.put(ELEVES,
                                        new DefaultExportBulletinService(eb, null)
                                                .sortResultByClasseNameAndNameForBulletin(elevesMapObject))));
                            }
                            else {
                                handler.handle(new Either.Left<>(event.cause().getMessage()));
                            }
                        });
                    }
                    else {

                        handler.handle(new Either.Left<>(idElevesEvent.cause().getMessage()));
                    }
                });

    }

    public void getTotaleDatasReleve(final JsonObject params, final Handler<Either<String, JsonObject>> handler){
        final String idEtablissement = params.getString(Competences.ID_ETABLISSEMENT_KEY);
        final String idClasse = params.getString(Competences.ID_CLASSE_KEY);
        final JsonArray idMatieres = params.getJsonArray("idMatieres");
        final Long idPeriode = params.getLong(Competences.ID_PERIODE_KEY);
        final Integer typeClasse = params.getInteger(Competences.TYPE_CLASSE_KEY);
        final JsonArray idEleves = new fr.wseduc.webutils.collections.JsonArray();
        final JsonObject resultHandler = new JsonObject();
        Map<String, JsonObject> elevesMapObject = new HashMap<>();

        // Récupération des élèves de la classe
        Future<JsonArray> studentsClassFuture =  Future.future();
        getStudentClassForExportReleve(idClasse, idPeriode, idEleves, typeClasse,
                elevesMapObject, studentsClassFuture);

        // Récupération du tableau de conversion
        Future<JsonArray> tableauDeConversionFuture = Future.future();
        // On récupère le tableau de conversion des compétences notes pour Lire le positionnement
        new DefaultCompetenceNoteService(COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE)
                .getConversionNoteCompetence(idEtablissement, idClasse,
                        tableauEvent -> {
                            FormateFutureEvent.formate(tableauDeConversionFuture, tableauEvent);
                        });

        List<Future> listFuturesFirst = new ArrayList<>(
                Arrays.asList(studentsClassFuture,tableauDeConversionFuture)
        );

        for (Object idMatiere : idMatieres){
            // Récupération du  nombre de devoirs avec évaluation numérique
            Future<JsonObject> nbEvaluatedHomeWork = Future.future();
            getNbEvaluatedHomeWork(idClasse, idMatiere.toString(), idPeriode, event -> {
                FormateFutureEvent.formate(nbEvaluatedHomeWork, event);
            });
            listFuturesFirst.add(nbEvaluatedHomeWork);
        }

        // Avec les ids des élèves de la classe, récupération des moyennes Finales , des Notes, des Competences Notes
        // et des Appreciations et des Positionnements finaux
        CompositeFuture.all(listFuturesFirst)
                .setHandler( idElevesEvent -> {
                    if(idElevesEvent.succeeded()) {

                        // Récupération des moyennes, positionnement Finales, appréciations, avis conseil de classe et orientation
                        Future<JsonArray> bigRequestFuture = Future.future();
                        getColonneReleveTotale(idEleves, idPeriode, idMatieres, new JsonArray().add(idClasse), (Either<String, JsonArray> Event) -> {
                            FormateFutureEvent.formate(bigRequestFuture, Event);
                        });

                        // Récupération des Notes du Relevé
                        Future<JsonArray> notesFuture = Future.future();
                        getNotesReleveTotalesEleves(idEleves, idEtablissement, idClasse, idMatieres, idPeriode,
                                (Either<String, JsonArray> notesEvent) -> {
                                    FormateFutureEvent.formate(notesFuture, notesEvent);
                                });

                        // Récupération des Compétences-Notes du Relevé
                        Future<JsonArray> compNotesFuture = Future.future();
                            getCompetencesNotesReleveTotaleEleves(idEleves, idEtablissement, idClasse,
                                    idMatieres, idPeriode, compNotesEvent -> {
                                    FormateFutureEvent.formate(compNotesFuture, compNotesEvent);
                                });

                        List<Future> listFutures = new ArrayList<>(
                                Arrays.asList(bigRequestFuture, compNotesFuture,notesFuture)
                        );
                        CompositeFuture.all(listFutures).setHandler( event -> {
                            if(event.succeeded()) {
                                // Rajout des moyennes finales

                                for (int i=2; i<idMatieres.size()+2;i++){
                                    // Récupération du  nombre de devoirs avec évaluation numérique
                                    Boolean hasEvaluatedHomeWork = (((JsonObject)listFuturesFirst.get(i).result()).getLong("nb") > 0);

                                    JsonArray notesMatiere = new JsonArray();

                                    for(Object note : notesFuture.result()){
                                        if(((JsonObject)note).getString("id_matiere").equals(idMatieres.getString(i-2))){
                                            notesMatiere.add(note);
                                        }
                                    }

                                    if(resultHandler.containsKey(NOTES)){
                                        resultHandler.getJsonObject(NOTES).put(idMatieres.getString(i-2),notesMatiere);
                                    }else{
                                        JsonObject jsonNotesToAdd = new JsonObject();
                                        jsonNotesToAdd.put(idMatieres.getString(i-2),notesMatiere);
                                        resultHandler.put(NOTES, jsonNotesToAdd);
                                    }

                                    JsonObject resultNotes = new JsonObject();

                                    if (!hasEvaluatedHomeWork) {
                                        for (Map.Entry<String, JsonObject> student : elevesMapObject.entrySet()) {
                                            if( student.getValue().containsKey(MOYENNE)){
                                                if(student.getValue().getValue(MOYENNE).getClass() == Double.class){
                                                    student.getValue().remove(MOYENNE);
                                                    JsonObject jsonToAdd = new JsonObject();
                                                    jsonToAdd.put(idMatieres.getString(i-2),NN);
                                                    student.getValue().put(MOYENNE, jsonToAdd);
                                                }else {
                                                    student.getValue().getJsonObject(MOYENNE).put(idMatieres.getString(i - 2), NN);
                                                }
                                            }else{
                                                JsonObject jsonToAdd = new JsonObject();
                                                jsonToAdd.put(idMatieres.getString(i-2),NN);
                                                student.getValue().put(MOYENNE, jsonToAdd);
                                            }
                                        }
                                    }else{
                                        calculMoyennesNotesFOrReleveTotale(notesMatiere, resultNotes,idPeriode,
                                                elevesMapObject, idMatieres.getString(i-2));
                                        if( resultHandler.containsKey(MOYENNE)){
                                            resultHandler.getJsonObject(MOYENNE).put(idMatieres.getString(i-2),resultNotes);
                                        }else{
                                            JsonObject jsonToAdd = new JsonObject();
                                            jsonToAdd.put(idMatieres.getString(i-2),resultNotes);
                                            resultHandler.put(MOYENNE, jsonToAdd);
                                        }
                                    }

                                }

                                if (idPeriode != null) {
                                    for (Object idMatiere : idMatieres){
                                        JsonArray notesMatiere = new JsonArray();

                                        for(Object note : compNotesFuture.result()){
                                            if(((JsonObject)note).getString("id_matiere").equals(idMatiere.toString())){
                                                notesMatiere.add(note);
                                            }
                                        }
                                        if( resultHandler.containsKey(COMPETENCES_NOTES_KEY)){
                                            resultHandler.getJsonObject(COMPETENCES_NOTES_KEY).put(idMatiere.toString(),notesMatiere);
                                        }else{
                                            JsonObject jsonToAdd = new JsonObject();
                                            jsonToAdd.put(idMatiere.toString(),notesMatiere);
                                            resultHandler.put(COMPETENCES_NOTES_KEY, jsonToAdd);
                                        }

                                        Map<String, JsonArray> notesByEleve = groupeNotesByStudent(notesMatiere);

                                        for (Map.Entry<String, JsonArray> entry : notesByEleve.entrySet()) {
                                            String idEleve = entry.getKey();
                                            JsonArray compNotesEleve = entry.getValue();

                                            if(elevesMapObject.containsKey(idEleve) && idEleve != null) {
                                                JsonObject eleveObject = elevesMapObject.get(idEleve);
                                                if( eleveObject.containsKey(COMPETENCES_NOTES_KEY)){
                                                    if (eleveObject.getJsonObject(COMPETENCES_NOTES_KEY).getJsonObject(idMatiere.toString()) == null) {
                                                        eleveObject.getJsonObject(COMPETENCES_NOTES_KEY).put(idMatiere.toString(), compNotesEleve);
                                                    }
                                                }else{
                                                    JsonObject jsonToAdd = new JsonObject();
                                                    jsonToAdd.put(idMatiere.toString(),compNotesEleve);
                                                    eleveObject.put(COMPETENCES_NOTES_KEY, jsonToAdd);
                                                }
                                                JsonObject resultNotes = new JsonObject();
                                                calculPositionnementAutoByEleveByMatiere(compNotesEleve, resultNotes);
                                                if( eleveObject.containsKey(POSITIONNEMENT_AUTO)){
                                                    eleveObject.getJsonObject(POSITIONNEMENT_AUTO).put(idMatiere.toString(),resultNotes.getJsonArray(POSITIONNEMENTS_AUTO));
                                                }else{
                                                    JsonObject jsonToAdd = new JsonObject();
                                                    jsonToAdd.put(idMatiere.toString(),resultNotes.getJsonArray(POSITIONNEMENTS_AUTO));
                                                    eleveObject.put(POSITIONNEMENT_AUTO, jsonToAdd);
                                                }
                                                JsonObject positionnement = utilsService.getObjectForPeriode(
                                                        eleveObject.getJsonObject(POSITIONNEMENT_AUTO).getJsonArray(idMatiere.toString()), idPeriode, ID_PERIODE);
                                                String positionnement_auto  = "";
                                                if (positionnement != null) {
                                                    Float moyennePositionnement =  positionnement.getFloat(MOYENNE);

                                                    positionnement_auto = utilsService.convertPositionnement(moyennePositionnement,
                                                            tableauDeConversionFuture.result(), null);
                                                }
                                                if( eleveObject.containsKey(POSITIONNEMENT)){
                                                    eleveObject.getJsonObject(POSITIONNEMENT).put(idMatiere.toString(),positionnement_auto);
                                                }else{
                                                    JsonObject jsonToAdd = new JsonObject();
                                                    jsonToAdd.put(idMatiere.toString(),positionnement_auto);
                                                    eleveObject.put(POSITIONNEMENT, jsonToAdd);
                                                }
                                            }
                                        }
                                    }

                                    for (int i=2; i<idMatieres.size()+2;i++){
                                        // Récupération du  nombre de devoirs avec évaluation numérique
                                        Boolean hasEvaluatedHomeWork = (((JsonObject)listFuturesFirst.get(i).result()).getLong("nb") > 0);

                                        FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject, MOYENNE, idPeriode, hasEvaluatedHomeWork, idMatieres.getString(i-2));
                                        //Rajout des notes par devoir et Calcul des moyennes auto

                                        //Rajout des positionnements finaux
                                        FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                                POSITIONNEMENT, idPeriode, hasEvaluatedHomeWork, idMatieres.getString(i-2));

                                    }
                                    JsonArray appreciationsEleves = new JsonArray();
                                    JsonArray avisConseil = new JsonArray();
                                    JsonArray avisOrientation = new JsonArray();
                                    for(Object data : bigRequestFuture.result()){
                                        if(((JsonObject)data).getString(SYNTHESE_BILAN_PERIODIQUE) != null){
                                            JsonObject jsonToAdd = new JsonObject();
                                            jsonToAdd.put(((JsonObject)data).getString("id_eleve"),((JsonObject)data).getString(SYNTHESE_BILAN_PERIODIQUE));
                                            appreciationsEleves.add(jsonToAdd);
                                        }
                                        if(((JsonObject)data).getString(AVIS_CONSEIL_DE_CLASSE) != null){
                                            JsonObject jsonToAdd = new JsonObject();
                                            jsonToAdd.put(((JsonObject)data).getString("id_eleve"),((JsonObject)data).getString(AVIS_CONSEIL_DE_CLASSE));
                                            avisConseil.add(jsonToAdd);
                                        }
                                        if(((JsonObject)data).getString(AVIS_CONSEIL_ORIENTATION) != null){
                                            JsonObject jsonToAdd = new JsonObject();
                                            jsonToAdd.put(((JsonObject)data).getString("id_eleve"),((JsonObject)data).getString(AVIS_CONSEIL_ORIENTATION));
                                            avisOrientation.add(jsonToAdd);
                                        }
                                    }
                                    //Rajout des appreciations par élèves
                                    resultHandler.put(APPRECIATIONS_ELEVE, appreciationsEleves);
                                    FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                            SYNTHESE_BILAN_PERIODIQUE, idPeriode, false, "");
                                    resultHandler.put(AVIS_CONSEIL_DE_CLASSE, avisConseil);
                                    FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                            AVIS_CONSEIL_DE_CLASSE, idPeriode, false, "");
                                    resultHandler.put(AVIS_CONSEIL_ORIENTATION, avisOrientation);
                                    FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                            AVIS_CONSEIL_ORIENTATION, idPeriode, false, "");
                                }
                                handler.handle(new Either.Right<>(resultHandler.put(ELEVES, new DefaultExportBulletinService(eb, null)
                                        .sortResultByClasseNameAndNameForBulletin(elevesMapObject))));
                            }
                            else {
                                handler.handle(new Either.Left<>(event.cause().getMessage()));
                            }
                        });
                    }
                    else {

                        handler.handle(new Either.Left<>(idElevesEvent.cause().getMessage()));
                    }
                });

    }

    private <T> void calculMoyennesNotesFOrReleve (JsonArray listNotes, JsonObject result, Long idPeriode,
                                                   Map<String, JsonObject> eleveMapObject,
                                                   Boolean hasEvaluatedHomeWork, Boolean isExport) {

        // Si pour il n'y a pas de devoirs avec évaluation numérique, la moyenne auto est NN
        if (!hasEvaluatedHomeWork) {
            for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                student.getValue().put(MOYENNE, NN);
            }
            return;
        }

        else {

            JsonArray listMoyDevoirs = new fr.wseduc.webutils.collections.JsonArray();
            JsonArray listMoyEleves = new fr.wseduc.webutils.collections.JsonArray();
            HashMap<Long, ArrayList<NoteDevoir>> notesByDevoir = new HashMap<>();
            HashMap<String, T> notesByEleve = new HashMap<>();

            Double sumMoyClasse = 0.0;
            int nbMoyenneClasse = 0;

            Map<String, HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>>
                    notesByDevoirByPeriodeByEleve = new HashMap<>();

            if (idPeriode == null) {
                result.put(MOYENNES, new JsonArray());
            }

            for (int i = 0; i < listNotes.size(); i++) {
                JsonObject note = listNotes.getJsonObject(i);
                if (note.getString(VALEUR) == null || note.getString(COEFFICIENT) == null ||
                        !note.getBoolean(IS_EVALUATED)) {
                    continue; //Si la note fait partie d'un devoir qui n'est pas évalué,
                    // elle n'est pas prise en compte dans le calcul de la moyenne
                }

                Long idDevoir = note.getLong(ID_DEVOIR);
                String idEleve = note.getString(ID_ELEVE);
                Long id_periode = note.getLong(ID_PERIODE);
                NoteDevoir noteDevoir;
                noteDevoir = new NoteDevoir(
                        Double.valueOf(note.getString(VALEUR)),
                        Double.valueOf(note.getLong(DIVISEUR)),
                        note.getBoolean(RAMENER_SUR),
                        Double.valueOf(note.getString(COEFFICIENT)), idEleve, id_periode);


                if (idPeriode == null) {
                    if (!notesByDevoirByPeriodeByEleve.containsKey(idEleve)) {
                        notesByDevoirByPeriodeByEleve.put(idEleve, new HashMap<>());
                        notesByDevoirByPeriodeByEleve.get(idEleve).put(null, new HashMap<>());
                    }

                    if (!notesByDevoirByPeriodeByEleve.get(idEleve).containsKey(id_periode)) {
                        notesByDevoirByPeriodeByEleve.get(idEleve).put(id_periode, new HashMap<>());
                    }

                    if (note.getString(ID_ELEVE).equals(idEleve)) {
                        utilsService.addToMap(id_periode,
                                notesByDevoirByPeriodeByEleve.get(idEleve).get(id_periode), noteDevoir);
                        utilsService.addToMap(null, notesByDevoirByPeriodeByEleve.get(idEleve).get(null),
                                noteDevoir);
                    }
                }
                utilsService.addToMap(idDevoir, notesByDevoir, noteDevoir);
                utilsService.addToMap(idEleve, (HashMap<String, ArrayList<NoteDevoir>>) notesByEleve, noteDevoir);
            }

            for (Map.Entry<Long, ArrayList<NoteDevoir>> entry : notesByDevoir.entrySet()) {
                JsonObject moyenne = utilsService.calculMoyenneParDiviseur(entry.getValue(), true);
                moyenne.put("id", entry.getKey());
                listMoyDevoirs.add(moyenne);
            }
            result.put("devoirs", listMoyDevoirs);

            // Calcul des moyennes par élève
            for (Map.Entry<String, T> entry : notesByEleve.entrySet()) {
                JsonObject moyenne;
                String idEleve = entry.getKey();

                moyenne = utilsService.calculMoyenne((ArrayList<NoteDevoir>) entry.getValue(),
                        false, 20);
                if (eleveMapObject.containsKey(idEleve)) {
                    Double moy = moyenne.getDouble(MOYENNE);
                    JsonObject el = eleveMapObject.get(idEleve);
                    el.put(MOYENNE, moy).put(HAS_NOTE, moyenne.getBoolean(HAS_NOTE));
                    if(isExport && !el.containsKey(MOYENNEFINALE)){
                        el.put(MOYENNEFINALE, moy);
                    }

                    if(el.containsKey(MOYENNEFINALE)){
                        try {
                            sumMoyClasse += Double.valueOf(el.getString(MOYENNEFINALE));
                        }
                        catch (ClassCastException c) {
                            sumMoyClasse += el.getDouble(MOYENNEFINALE);
                        }
                    }
                    else {
                        sumMoyClasse += moy;
                    }
                    ++nbMoyenneClasse;
                }

                moyenne.put("id", idEleve);
                listMoyEleves.add(moyenne);
            }
            DecimalFormat decimalFormat = new DecimalFormat("#.00");
            result.put("moyenne_classe",(nbMoyenneClasse>0)? decimalFormat.format((sumMoyClasse/nbMoyenneClasse)): " ");

            if (idPeriode == null) {
                HashMap<Long, JsonArray> listMoy = new HashMap<>();

                for (Map.Entry<String, HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>> entryEleve
                        : notesByDevoirByPeriodeByEleve.entrySet()) {
                    for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> entryPeriode
                            : notesByDevoirByPeriodeByEleve.get(entryEleve.getKey()).entrySet()) {
                        listMoy.put(entryPeriode.getKey(), new fr.wseduc.webutils.collections.JsonArray());
                        if(!eleveMapObject.get(entryEleve.getKey()).containsKey(MOYENNES)) {
                            eleveMapObject.get(entryEleve.getKey()).put(MOYENNES, new JsonArray());
                        }
                        JsonArray moyennesFinales = eleveMapObject.get(entryEleve.getKey())
                                .getJsonArray(MOYENNESFINALES);
                        for (Map.Entry<Long, ArrayList<NoteDevoir>> entry :
                                entryPeriode.getValue().entrySet()) {
                            JsonObject moyenne = utilsService.calculMoyenne(
                                    entry.getValue(),
                                    false, 20);

                            Double moy = moyenne.getDouble(MOYENNE);
                            Boolean isFinale = false;
                            if (moyennesFinales != null) {

                                if (entry.getKey() == null) {
                                    List<NoteDevoir> notePeriode = entry.getValue();
                                    int nbMoy = moyennesFinales.size();
                                    double sumMoy = 0.0;
                                    for (int i = 0; i < nbMoy; i++) {
                                        JsonObject moyFin = moyennesFinales.getJsonObject(i);
                                        Long periode = moyFin.getLong(ID_PERIODE);
                                        notePeriode = notePeriode.stream()
                                                .filter(line -> !(line.getIdPeriode().equals(periode)))
                                                .collect(Collectors.toList());
                                        sumMoy += Double.valueOf(moyFin.getString(MOYENNE));
                                    }
                                    if (!notePeriode.isEmpty()) {
                                        ++nbMoy;
                                        moyenne = utilsService.calculMoyenne(
                                                notePeriode,
                                                false, 20);
                                        sumMoy += moyenne.getDouble(MOYENNE);
                                    }
                                    moyenne.remove(MOYENNE);
                                    moyenne.put(MOYENNE,sumMoy / nbMoy );
                                } else {
                                    JsonObject moyObj = utilsService.getObjectForPeriode(moyennesFinales,
                                            entry.getKey(), ID_PERIODE);
                                    if (moyObj != null) {
                                        moy = Double.valueOf(moyObj.getString(MOYENNE));
                                        moyenne.remove(MOYENNE);
                                        moyenne.put(MOYENNE, moy);
                                        isFinale = true;
                                    }
                                }
                            }
                            moyenne.put(ID_PERIODE, entry.getKey());
                            moyenne.put(ID_ELEVE, entryEleve.getKey());
                            moyenne.put("isFinale", isFinale);
                            listMoy.get(entryPeriode.getKey()).add(moyenne);
                            eleveMapObject.get(entryEleve.getKey()).getJsonArray(MOYENNES).add(moyenne);
                        }
                        if (listMoy.get(entryPeriode.getKey()).size() > 0) {
                            result.getJsonArray(MOYENNES).add(
                                    listMoy.get(entryPeriode.getKey()).getJsonObject(0));
                        }
                    }
                }
            }
        }
    }

    private <T> void calculMoyennesNotesFOrReleveTotale (JsonArray listNotes, JsonObject result, Long idPeriode,
                                                   Map<String, JsonObject> eleveMapObject,String idMatiere) {

            JsonArray listMoyDevoirs = new fr.wseduc.webutils.collections.JsonArray();
            JsonArray listMoyEleves = new fr.wseduc.webutils.collections.JsonArray();
            HashMap<Long, ArrayList<NoteDevoir>> notesByDevoir = new HashMap<>();
            HashMap<String, T> notesByEleve = new HashMap<>();

            Double sumMoyClasse = 0.0;
            int nbMoyenneClasse = 0;

            Map<String, HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>>
                    notesByDevoirByPeriodeByEleve = new HashMap<>();

            if (idPeriode == null) {
                result.put(MOYENNES, new JsonArray());
            }

            for (int i = 0; i < listNotes.size(); i++) {
                JsonObject note = listNotes.getJsonObject(i);
                if (note.getString(VALEUR) == null || note.getString(COEFFICIENT) == null ||
                        !note.getBoolean(IS_EVALUATED)) {
                    continue; //Si la note fait partie d'un devoir qui n'est pas évalué,
                    // elle n'est pas prise en compte dans le calcul de la moyenne
                }

                Long idDevoir = note.getLong(ID_DEVOIR);
                String idEleve = note.getString(ID_ELEVE);
                Long id_periode = note.getLong(ID_PERIODE);
                NoteDevoir noteDevoir;
                noteDevoir = new NoteDevoir(
                        Double.valueOf(note.getString(VALEUR)),
                        Double.valueOf(note.getLong(DIVISEUR)),
                        note.getBoolean(RAMENER_SUR),
                        Double.valueOf(note.getString(COEFFICIENT)), idEleve, id_periode);


                if (idPeriode == null) {
                    if (!notesByDevoirByPeriodeByEleve.containsKey(idEleve)) {
                        notesByDevoirByPeriodeByEleve.put(idEleve, new HashMap<>());
                        notesByDevoirByPeriodeByEleve.get(idEleve).put(null, new HashMap<>());
                    }

                    if (!notesByDevoirByPeriodeByEleve.get(idEleve).containsKey(id_periode)) {
                        notesByDevoirByPeriodeByEleve.get(idEleve).put(id_periode, new HashMap<>());
                    }

                    if (note.getString(ID_ELEVE).equals(idEleve)) {
                        utilsService.addToMap(id_periode,
                                notesByDevoirByPeriodeByEleve.get(idEleve).get(id_periode), noteDevoir);
                        utilsService.addToMap(null, notesByDevoirByPeriodeByEleve.get(idEleve).get(null),
                                noteDevoir);
                    }
                }
                utilsService.addToMap(idDevoir, notesByDevoir, noteDevoir);
                utilsService.addToMap(idEleve, (HashMap<String, ArrayList<NoteDevoir>>) notesByEleve, noteDevoir);
            }

            for (Map.Entry<Long, ArrayList<NoteDevoir>> entry : notesByDevoir.entrySet()) {
                JsonObject moyenne = utilsService.calculMoyenneParDiviseur(entry.getValue(), true);
                moyenne.put("id", entry.getKey());
                listMoyDevoirs.add(moyenne);
            }
            result.put("devoirs", listMoyDevoirs);

            // Calcul des moyennes par élève
            for (Map.Entry<String, T> entry : notesByEleve.entrySet()) {
                JsonObject moyenne;
                String idEleve = entry.getKey();

                moyenne = utilsService.calculMoyenne((ArrayList<NoteDevoir>) entry.getValue(),
                        false, 20);
                if (eleveMapObject.containsKey(idEleve)) {
                    Double moy = moyenne.getDouble(MOYENNE);
                    JsonObject el = eleveMapObject.get(idEleve);

                    if(el.containsKey(MOYENNE)){
                        el.getJsonObject(MOYENNE).put(idMatiere,moy);
                    }else{
                        JsonObject moyMat = new JsonObject();
                        moyMat.put(idMatiere,moy);
                        el.put(MOYENNE, moyMat);
                    }

                    if(el.containsKey(HAS_NOTE)){
                        el.getJsonObject(HAS_NOTE).put(idMatiere,moyenne.getBoolean(HAS_NOTE));
                    }else{
                        JsonObject moyMat = new JsonObject();
                        moyMat.put(idMatiere,moyenne.getBoolean(HAS_NOTE));
                        el.put(HAS_NOTE, moyMat);
                    }

                    if(!el.containsKey(MOYENNEFINALE)){
                        JsonObject moyMat = new JsonObject();
                        moyMat.put(idMatiere,moy);
                        el.put(MOYENNEFINALE, moyMat);
                    }else if(!el.getJsonObject(MOYENNEFINALE).containsKey(idMatiere)){
                        el.getJsonObject(MOYENNEFINALE).put(idMatiere,moy);
                    }

                    if(el.getJsonObject(MOYENNEFINALE).containsKey(idMatiere)) {
                        try {
                            sumMoyClasse += Double.valueOf(el.getJsonObject(MOYENNEFINALE).getString(idMatiere));
                        } catch (ClassCastException c) {
                            sumMoyClasse += el.getJsonObject(MOYENNEFINALE).getDouble(idMatiere);
                        }
                    }else{
                        sumMoyClasse += moy;
                    }
                    ++nbMoyenneClasse;
                }

                moyenne.put("id", idEleve);
                listMoyEleves.add(moyenne);
            }
            DecimalFormat decimalFormat = new DecimalFormat("#.00");
            result.put("moyenne_classe",(nbMoyenneClasse>0)? decimalFormat.format((sumMoyClasse/nbMoyenneClasse)): " ");

            if (idPeriode == null) {
                HashMap<Long, JsonArray> listMoy = new HashMap<>();

                for (Map.Entry<String, HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>> entryEleve
                        : notesByDevoirByPeriodeByEleve.entrySet()) {
                    for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> entryPeriode
                            : notesByDevoirByPeriodeByEleve.get(entryEleve.getKey()).entrySet()) {
                        listMoy.put(entryPeriode.getKey(), new fr.wseduc.webutils.collections.JsonArray());
                        if(!eleveMapObject.get(entryEleve.getKey()).containsKey(MOYENNES)) {
                            JsonObject matMoy = new JsonObject();
                            matMoy.put(idMatiere,new JsonArray());
                            eleveMapObject.get(entryEleve.getKey()).put(MOYENNES, matMoy);
                        }else if(!eleveMapObject.get(entryEleve.getKey()).getJsonObject(MOYENNES).containsKey(idMatiere)){
                            eleveMapObject.get(entryEleve.getKey()).getJsonObject(MOYENNES).put(idMatiere,new JsonArray());
                        }
                        JsonArray moyennesFinales = eleveMapObject.get(entryEleve.getKey())
                                .getJsonObject(MOYENNESFINALES).getJsonArray(idMatiere);
                        for (Map.Entry<Long, ArrayList<NoteDevoir>> entry :
                                entryPeriode.getValue().entrySet()) {
                            JsonObject moyenne = utilsService.calculMoyenne(
                                    entry.getValue(),
                                    false, 20);

                            Double moy = moyenne.getDouble(MOYENNE);
                            Boolean isFinale = false;
                            if (moyennesFinales != null) {

                                if (entry.getKey() == null) {
                                    List<NoteDevoir> notePeriode = entry.getValue();
                                    int nbMoy = moyennesFinales.size();
                                    double sumMoy = 0.0;
                                    for (int i = 0; i < nbMoy; i++) {
                                        JsonObject moyFin = moyennesFinales.getJsonObject(i);
                                        Long periode = moyFin.getLong(ID_PERIODE);
                                        notePeriode = notePeriode.stream()
                                                .filter(line -> !(line.getIdPeriode().equals(periode)))
                                                .collect(Collectors.toList());
                                        sumMoy += Double.valueOf(moyFin.getString(MOYENNE));
                                    }
                                    if (!notePeriode.isEmpty()) {
                                        ++nbMoy;
                                        moyenne = utilsService.calculMoyenne(
                                                notePeriode,
                                                false, 20);
                                        sumMoy += moyenne.getDouble(MOYENNE);
                                    }
                                    moyenne.remove(MOYENNE);
                                    moyenne.put(MOYENNE,sumMoy / nbMoy );
                                } else {
                                    JsonObject moyObj = utilsService.getObjectForPeriode(moyennesFinales,
                                            entry.getKey(), ID_PERIODE);
                                    if (moyObj != null) {
                                        moy = Double.valueOf(moyObj.getString(MOYENNE));
                                        moyenne.remove(MOYENNE);
                                        moyenne.put(MOYENNE, moy);
                                        isFinale = true;
                                    }
                                }
                            }
                            moyenne.put(ID_PERIODE, entry.getKey());
                            moyenne.put(ID_ELEVE, entryEleve.getKey());
                            moyenne.put("isFinale", isFinale);
                            listMoy.get(entryPeriode.getKey()).add(moyenne);
                            eleveMapObject.get(entryEleve.getKey()).getJsonObject(MOYENNES).getJsonArray(idMatiere).add(moyenne);
                        }
                        if (listMoy.get(entryPeriode.getKey()).size() > 0) {
                            result.getJsonArray(MOYENNES).add(
                                    listMoy.get(entryPeriode.getKey()).getJsonObject(0));
                        }
                    }
                }
            }
    }

    private <T> void calculMoyennesCompetencesNotesFOrReleve (JsonArray listNotes, JsonObject result,
                                                              Long idPeriode, JsonArray tableauDeconversion,
                                                              Map<String, JsonObject> eleveMapObject) {

        Map<String, JsonArray> notesByEleve = groupeNotesByStudent(listNotes);

        for (Map.Entry<String, JsonArray> entry : notesByEleve.entrySet()) {
            String idEleve = entry.getKey();
            JsonArray compNotesEleve = entry.getValue();

            if(eleveMapObject.containsKey(idEleve)) {
                JsonObject eleveObject = eleveMapObject.get(idEleve);
                if (eleveObject.getJsonArray(COMPETENCES_NOTES_KEY) == null) {
                    eleveObject.put(COMPETENCES_NOTES_KEY, compNotesEleve);
                }
                calculPositionnementAutoByEleveByMatiere(compNotesEleve, eleveObject);
                JsonObject positionnement = utilsService.getObjectForPeriode(
                        eleveObject.getJsonArray(POSITIONNEMENTS_AUTO), idPeriode, ID_PERIODE);
                String positionnement_auto  = "";
                if (positionnement != null) {
                    Float moyennePositionnement =  positionnement.getFloat(MOYENNE);

                    positionnement_auto = utilsService.convertPositionnement(moyennePositionnement,
                            tableauDeconversion, null);
                }
                eleveObject.put(POSITIONNEMENT, positionnement_auto);
            }
        }

    }


    private void getStudentClassForExportReleve(String idClasse, Long idPeriode, JsonArray idEleves, Integer typeClasse,
                                                Map<String, JsonObject> eleveMapObject,
                                                Future<JsonArray> studentsClassFuture ){
        new DefaultUtilsService(this.eb).studentAvailableForPeriode(idClasse, idPeriode, typeClasse,
                message -> {

                    JsonObject body = message.body();
                    if ("ok".equals(body.getString("status"))) {
                        JsonArray queryResult =body.getJsonArray("results");
                        for (int i = 0; i < queryResult.size(); i++) {
                            JsonObject eleve = queryResult.getJsonObject(i);
                            String idEleve = eleve.getString("id");
                            idEleves.add(idEleve);
                            eleve.put(CLASSE_NAME_KEY, eleve.getString("level"));
                            eleve.put(NAME, eleve.getString(LAST_NAME_KEY));
                            eleve.put(DISPLAY_NAME_KEY, eleve.getString(LAST_NAME_KEY) + " "
                                    + eleve.getString(FIRST_NAME_KEY));
                            eleveMapObject.put(idEleve, eleve);
                        }
                        studentsClassFuture.complete(queryResult);
                    }
                    else {
                        studentsClassFuture.fail("[getStudentClassForExportReleve] " +
                                ": Error while getting students ");
                    }
                });
    }

    private void FormateColonneFinaleReleve(JsonArray datas, Map<String, JsonObject> eleveMapObject,
                                            String colonne, Long idPeriode, Boolean hasEvaluatedHommeWork) {
        String resultLabel = colonne;
        if (MOYENNE.equals(colonne)) {
            resultLabel += (idPeriode!=null)? "Finale" : "sFinales";
        }
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            String idEleve = data.getString(ID_ELEVE);
            JsonObject eleve = eleveMapObject.get(idEleve);

            if(eleve != null) {
                if(idPeriode != null) {
                    if(eleve.containsKey(resultLabel)){
                        eleve.remove(resultLabel);
                    }
                    eleve.put(resultLabel, data.getValue(colonne));
                }
                else {
                    if(!eleve.containsKey(resultLabel)){
                        eleve.put(resultLabel, new JsonArray());
                    }
                    eleve.getJsonArray(resultLabel).add(data);
                }
            }
            else {
                log.error(" Student No found : " + idEleve);
            }
        }
        if (!hasEvaluatedHommeWork && MOYENNE.equals(colonne)) {
            for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                if (!student.getValue().containsKey(resultLabel)){
                    student.getValue().put(resultLabel, NN);
                }
            }
        }

    }

    private void FormateColonneFinaleReleveTotale(JsonArray datas, Map<String, JsonObject> eleveMapObject,
                                            String colonne, Long idPeriode, Boolean hasEvaluatedHommeWork, String idMatiere) {
        String resultLabel = colonne;
        if (MOYENNE.equals(colonne)) {
            resultLabel += (idPeriode!=null)? "Finale" : "sFinales";
        }
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            String idEleve = data.getString(ID_ELEVE);
            JsonObject eleve = eleveMapObject.get(idEleve);

            if(data.getValue(colonne) != null) {
                if (eleve != null) {
                    if (idPeriode != null) {
                        if(MOYENNE.equals(colonne) || POSITIONNEMENT.equals(colonne)) {
                            if(data.getString("id_matiere").equals(idMatiere)) {
                                if(eleve.containsKey(resultLabel)) {
                                    if (eleve.getJsonObject(resultLabel).containsKey(idMatiere)) {
                                        eleve.getJsonObject(resultLabel).remove(idMatiere);
                                    }
                                    eleve.getJsonObject(resultLabel).put(idMatiere, data.getValue(colonne));
                                }else{
                                    JsonObject jsonToAdd = new JsonObject();
                                    jsonToAdd.put(idMatiere, data.getValue(colonne));
                                    eleve.put(resultLabel, jsonToAdd);
                                }
                            }
                        }else{
                            if (eleve.containsKey(resultLabel)) {
                                eleve.remove(resultLabel);
                            }
                            eleve.put(resultLabel, data.getValue(colonne));
                        }
                    } else {
                        if (!eleve.containsKey(resultLabel)) {
                            eleve.put(resultLabel, new JsonArray());
                        }
                        eleve.getJsonArray(resultLabel).add(data);
                    }
                } else {
                    log.error(" Student No found : " + idEleve);
                }
            }
        }
        if (!hasEvaluatedHommeWork && MOYENNE.equals(colonne)) {
            for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                if(student.getValue().containsKey(resultLabel)){
                    if (!student.getValue().getJsonObject(resultLabel).containsKey(idMatiere)) {
                        student.getValue().getJsonObject(resultLabel).put(idMatiere, NN);
                    }
                }else{
                    JsonObject jsonToAdd = new JsonObject();
                    jsonToAdd.put(idMatiere, NN);
                    student.getValue().put(resultLabel, jsonToAdd);
                }
            }
        }
        if (MOYENNE.equals(colonne)) {
            for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                if(!student.getValue().containsKey(resultLabel)){
                    JsonObject jsonToAdd = new JsonObject();
                    jsonToAdd.put(idMatiere, NN);
                    student.getValue().put(resultLabel, jsonToAdd);
                }
            }
        }
        if (POSITIONNEMENT.equals(colonne)) {
            for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                if(!student.getValue().containsKey(resultLabel)) {
                    JsonObject jsonToAdd = new JsonObject();
                    jsonToAdd.putNull(idMatiere);
                    student.getValue().put(resultLabel, jsonToAdd);
                }
            }
        }

    }

    private void getNbEvaluatedHomeWork(String idClasse, String idMatiere, Long idPeriode,
                                        Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        //tables
        String table_devoirs         = COMPETENCES_SCHEMA + "." +Competences.DEVOIR_TABLE;
        String table_rel_devoir_groupes  = COMPETENCES_SCHEMA + "." +Competences.REL_DEVOIRS_GROUPES;


        query.append("SELECT COUNT(*) AS nb  ")
                .append(" FROM  ")
                .append(table_devoirs)
                .append(" INNER JOIN ")
                .append(table_rel_devoir_groupes)
                .append(" ON id_devoir = id   AND id_groupe = ? AND id_matiere = ? AND is_evaluated = true ")
                .append((idPeriode!=null)? " AND id_periode = ? " : " ");


        values.add(idClasse);
        values.add(idMatiere);
        if( idPeriode != null) {
            values.add(idPeriode);
        }

        Sql.getInstance().prepared(query.toString(), values, validUniqueResultHandler(handler));
    }

    public void exportPDFRelevePeriodique(JsonObject param, final HttpServerRequest request, Vertx vertx,
                                          JsonObject config ){
        // Récupération des données de l'export
        Future<JsonObject> exportResult = Future.future();
        getDatasReleve(param, event -> {
            FormateFutureEvent.formate(exportResult, event);
        });

        String key = ELEVES;
        String idStructure = param.getString(ID_ETABLISSEMENT_KEY);
        Map<String, JsonObject> mapEleve = new HashMap<>();
        Future<JsonObject> structureFuture = Future.future();
        mapEleve.put(key, new JsonObject().put(ID_ETABLISSEMENT_KEY, idStructure));

        // Récupération des informations sur l'établissment
        new DefaultExportBulletinService(eb, null).getStructure(key,mapEleve, event -> {
            FormateFutureEvent.formate(structureFuture, event);
        });

        // Récupération du logo de l'établissment
        Future<JsonObject> imageStructureFuture = Future.future();
        utilsService.getParametersForExport(idStructure, event -> {
            FormateFutureEvent.formate(imageStructureFuture, event);
        });

        // Récupération du libellé de la période
        Future<String> periodeLibelleFuture = Future.future();
        getLibellePeriode(eb, request, param.getInteger(ID_PERIODE_KEY),  periodeLibelleEvent -> {
            FormateFutureEvent.formate(periodeLibelleFuture,periodeLibelleEvent );
        });

        CompositeFuture.all(exportResult, structureFuture, imageStructureFuture, periodeLibelleFuture)
                .setHandler((event -> {
                    if (event.succeeded()) {
                        JsonObject exportJson = exportResult.result();

                        putLibelleAndParamsForExportReleve(exportJson, param);
                        JsonObject imgStructure =  imageStructureFuture.result();
                        if (imgStructure != null && imgStructure.containsKey("imgStructure")) {
                            exportJson.put("imgStructure", imgStructure.getJsonObject("imgStructure")
                                    .getValue("path"));
                        }
                        exportJson.put("structureLibelle", mapEleve.get(key).getValue("structureLibelle"));
                        exportJson.put("periodeLibelle", periodeLibelleFuture.result());
                        new DefaultExportService(eb, null).genererPdf(request, exportJson,
                                "releve-periodique.pdf.xhtml", exportJson.getString("title"),
                                vertx, config);
                    }
                    else {
                        log.info(event.cause().getMessage());
                        badRequest(request);
                    }
                }));
    }
}
