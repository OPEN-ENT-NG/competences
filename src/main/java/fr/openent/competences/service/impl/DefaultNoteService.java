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
import fr.openent.competences.bean.*;
import fr.openent.competences.service.AnnotationService;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.utils.UtilsConvert;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
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
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.getLibellePeriode;
import static fr.openent.competences.Utils.isNotNull;
import static fr.openent.competences.Utils.isNull;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.ERROR;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.Renders.badRequest;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;
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
    public final String ID_SOUS_MATIERE = "id_sousmatiere";
    public final String APPRECIATION_MATIERE_PERIODE = "appreciation_matiere_periode";
    public final String SYNTHESE_BILAN_PERIODIQUE = "synthese_bilan_periodique";
    public final String AVIS_CONSEIL_DE_CLASSE = "avis_conseil_de_classe";
    public final String AVIS_CONSEIL_ORIENTATION = "avis_conseil_orientation";
    public final String AVIS_CONSEIL_BILAN_PERIODIQUE = "avis_conseil_bilan_periodique";
    public final String COMPETENCES_NOTES_KEY = "competencesNotes";
    public final String TABLE_CONVERSION_KEY = "tableConversions";
    public static final String SOUS_MATIERES = "sousMatieres";
    public static final String ID_TYPE_SOUS_MATIERE = "id_type_sousmatiere";
    public static final String COLSPAN = "colspan";
    public static final String MOYSPAN = "moyspan";
    public static final String MOYENNES_CLASSE = "moyennesClasse";
    public static final String STUDENT_RANK = "studentRank";
    public static final String CLASS_AVERAGE_MINMAX = "classAverageMinMax";
    public static final String NOTES_BY_PERIODE_BY_STUDENT = "notes_by_periode_by_student";

    private EventBus eb;
    private UtilsService utilsService;
    private AnnotationService annotationService;
    public DefaultNoteService(String schema, String table) {
        super(schema, table);
    }
    protected static final Logger log = LoggerFactory.getLogger(DefaultNoteService.class);

    public DefaultNoteService(String schema, String table, EventBus eb) {
        super(schema, table);
        this.eb = eb;
        utilsService = new DefaultUtilsService(eb);
        annotationService = new DefaultAnnotationService(COMPETENCES_SCHEMA, REL_ANNOTATIONS_DEVOIRS_TABLE);
    }



    @Override
    public void createNote(JsonObject note, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.create(note, user, handler);
    }

    @Override
    public void createNoteDevoir(JsonObject jo, String id_user, final Handler<Either<String, JsonObject>> handler) {
        Long id_devoir = jo.getLong("id_devoir");
        String id_eleve = jo.getString("id_eleve");
        Object valeur = jo.getValue("valeur");

        annotationService.getAnnotationByEleveByDevoir(new Long[]{id_devoir}, new String[]{id_eleve}, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {

                if(event.isLeft()){
                    handler.handle(new Either.Left<>(event.left().getValue()));
                    log.error(event.left().getValue());
                }else{
                    JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
                    JsonArray resultAnnotation = event.right().getValue();

                    if( resultAnnotation != null && !resultAnnotation.isEmpty()){
                        addStatmentDeleteAnnotation(statements, id_devoir, id_eleve);
                    }

                    addStatmentNote(statements, id_devoir, id_eleve, valeur, id_user);
                    Sql.getInstance().transaction(statements, SqlResult.validRowsResultHandler(handler));
                }
            }
        });

    }
    private void addStatmentDeleteAnnotation(JsonArray statements, Long id_devoir, String id_eleve ){
        String query = "DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".rel_annotations_devoirs " +
                "WHERE id_devoir = ? AND id_eleve = ? ;";
        JsonArray paramsDeleteAnnotation = new fr.wseduc.webutils.collections.JsonArray();
        paramsDeleteAnnotation.add(id_devoir).add(id_eleve);
        statements.add(new JsonObject()
                .put("statement", query)
                .put("values",paramsDeleteAnnotation)
                .put("action", "prepared"));
    }

    private void addStatmentNote(JsonArray statements, Long id_devoir, String id_eleve, Object valeur, String id_user){

        String query = "INSERT INTO " + COMPETENCES_SCHEMA + ".notes " +
                "( id_eleve, id_devoir, valeur, owner ) VALUES ( ?, ?, ?, ? ) ON CONFLICT ( id_devoir, id_eleve ) " +
                "DO UPDATE SET valeur = ?, owner = ?";
        JsonArray paramsNote = new JsonArray();
        paramsNote.add( id_eleve ).add( id_devoir ).add( valeur ).add( id_user).add( valeur ).add( id_user );

        statements.add( new JsonObject()
                .put("statement", query)
                .put("values", paramsNote)
                .put("action", "prepared"));
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

        Sql.getInstance().prepared(query.toString(), values, DELIVERY_OPTIONS, validResultHandler(handler));
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
                .append(" devoirs.ramener_sur, devoirs.is_evaluated, devoirs.id_periode,  devoirs.id_sousmatiere,")
                .append(" notes.valeur, notes.id, notes.id_eleve, services.coefficient as coef ")
                .append(" FROM "+ COMPETENCES_SCHEMA +".devoirs ")
                .append(" LEFT JOIN "+ COMPETENCES_SCHEMA +".notes ")
                .append(" ON devoirs.id = notes.id_devoir ")
                .append( (null!= userId)? " AND notes.id_eleve = ? ": "")
                .append(" INNER JOIN "+ COMPETENCES_SCHEMA +".rel_devoirs_groupes ")
                .append(" ON rel_devoirs_groupes.id_devoir = devoirs.id AND rel_devoirs_groupes.id_groupe IN " + Sql.listPrepared( idsClass.getList()))
                .append(" LEFT JOIN "+ Competences.COMPETENCES_SCHEMA + ".services ")
                .append(" ON (rel_devoirs_groupes.id_groupe = services.id_groupe ")
                .append(" AND devoirs.owner = services.id_enseignant   AND devoirs.id_matiere = services.id_matiere) ")
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
                                periodeId, withMoyenneFinale, idsGroup,null, handler);
                    } else {
                        handler.handle(new Either.Left<>("Error While getting Available student "));
                    }
                });

    }

    private void getNotesReleveEleves(JsonArray ids,String etablissementId, String classeId,String matiereId,
                                      Long periodeId, Boolean withMoyenneFinale, JsonArray idsGroup, JsonArray matiereIds,
                                      Handler<Either<String, JsonArray>> handler) {
        List<String> idEleves = new ArrayList<String>();

        if (ids != null) {
            for (int i = 0; i < ids.size(); i++) {
                idEleves.add(ids.getString(i));
            }
        }

        List<String> idMatieres = new ArrayList<String>();

        if(matiereIds != null) {
            for (int i = 0; i < matiereIds.size(); i++) {
                idMatieres.add(matiereIds.getString(i));
            }
        }else{
            idMatieres=null;
        }

        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        //Construction de la requête
        query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient," +
                " devoirs.diviseur, devoirs.ramener_sur,notes.valeur, notes.id, devoirs.id_periode , notes.id_eleve," +
                " devoirs.is_evaluated, null as annotation, devoirs.id_matiere, devoirs.id_sousmatiere " +
                " FROM " + COMPETENCES_SCHEMA + ".devoirs" +
                " LEFT JOIN " + COMPETENCES_SCHEMA + ".notes" +
                " ON ( devoirs.id = notes.id_devoir  " +
                (( null != idsGroup)? ")" : "AND notes.id_eleve IN " + Sql.listPrepared(idEleves) + ")") +
                " INNER JOIN " + COMPETENCES_SCHEMA + ".rel_devoirs_groupes ON " +
                "(rel_devoirs_groupes.id_devoir = devoirs.id AND "+
                ((null != idsGroup)? "rel_devoirs_groupes.id_groupe IN "+Sql.listPrepared(idsGroup.getList())+")" : "rel_devoirs_groupes.id_groupe = ?)") +
                " WHERE devoirs.id_etablissement = ? " +
                ((matiereIds != null || matiereId != null)?"AND devoirs.id_matiere IN "+((matiereIds != null)?Sql.listPrepared(idMatieres):"(?)")+" ": " "));

        setParamGetNotesReleve(idsGroup,idEleves,classeId, idMatieres, matiereId, etablissementId, values);
        if (periodeId != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(periodeId);
        }
        query.append(" UNION ");
        query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient," +
                " devoirs.diviseur, devoirs.ramener_sur,null as valeur, null as id, devoirs.id_periode, " +
                " rel_annotations_devoirs.id_eleve, devoirs.is_evaluated," +
                " rel_annotations_devoirs.id_annotation as annotation, devoirs.id_matiere, devoirs.id_sousmatiere " +
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
                ((matiereIds != null || matiereId != null)?"AND devoirs.id_matiere IN "+((matiereIds != null)?Sql.listPrepared(idMatieres):"(?)")+" ": " "));
        setParamGetNotesReleve(idsGroup,idEleves,classeId, idMatieres, matiereId, etablissementId, values);
        if (periodeId != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(periodeId);
        }
        query.append("ORDER BY date ASC ");

        String queryWithMoyF = "";
        if (withMoyenneFinale) {
            queryWithMoyF = ("SELECT * FROM ( " + query + ") AS devoirs_notes_annotation " +
                    "FULL JOIN ( SELECT moyenne_finale.id_matiere AS id_matiere_moyf, " +
                    "moyenne_finale.id_eleve AS id_eleve_moyenne_finale, COALESCE(moyenne_finale.moyenne, -100) AS moyenne, " +
                    "moyenne_finale.id_periode AS id_periode_moyenne_finale " +
                    "FROM notes.moyenne_finale WHERE "+
                    ((null != idsGroup)? "moyenne_finale.id_classe IN "+ Sql.listPrepared(idsGroup.getList()):
                            " moyenne_finale.id_eleve IN " +  Sql.listPrepared(idEleves) +
                                    " AND moyenne_finale.id_classe = ? " )+
                    ((matiereIds != null || matiereId != null)?"AND devoirs.id_matiere IN "+((matiereIds != null)?Sql.listPrepared(idMatieres):"(?)")+" ":"") +
                    ((null != periodeId)? "AND moyenne_finale.id_periode = ? " :"") +
                    ") AS moyf ON ( moyf.id_eleve_moyenne_finale = devoirs_notes_annotation.id_eleve "+
                    " AND moyf.id_matiere_moyf = devoirs_notes_annotation.id_matiere " +
                    " AND moyf.id_periode_moyenne_finale = devoirs_notes_annotation.id_periode )");

            setParamGetNotesReleve(idsGroup,idEleves,classeId, idMatieres, matiereId, null, values);
            if (periodeId != null) {
                values.add(periodeId);
            }
        }

        Sql.getInstance().prepared((withMoyenneFinale)? queryWithMoyF : query.toString(), values,
                Competences.DELIVERY_OPTIONS, validResultHandler(handler));
    }

    private void setParamGetNotesReleve(JsonArray idsGroup,List<String> idEleves,String classeId, List<String> matiereIds, String matiereId,
                                        String etablissementId, JsonArray values){
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
        if(etablissementId != null) {
            values.add(etablissementId);
        }
        if(null == matiereIds && matiereId != null){
            values.add(matiereId);
        }else if(null != matiereIds){
            for (String matiereIdToAdd : matiereIds) {
                values.add(matiereIdToAdd);
            }
        }
    }

    private void getCompetencesNotesReleveEleves(JsonArray ids, String etablissementId, String classeId,
                                                 JsonArray groupIds, String matiereId, JsonArray matiereIds,
                                                 Long periodeId,  String eleveId, Boolean withDomaineInfo,
                                                 Boolean isYear, Handler<Either<String, JsonArray>> handler) {
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
        else
            idMatieres = null;
        runGetCompetencesNotesReleve(etablissementId, classeId, groupIds,  matiereId, idMatieres, periodeId,
                eleveId, idEleves, withDomaineInfo, isYear, handler);
    }

    public void getCompetencesNotesReleve(String etablissementId, String classeId, JsonArray groupIds, String matiereId,
                                          Long periodeId,  String eleveId, Integer typeClasse, Boolean withDomaineInfo,
                                          Boolean isYear, Handler<Either<String, JsonArray>> handler) {
        if(typeClasse == null){
            runGetCompetencesNotesReleve(etablissementId, classeId, groupIds, matiereId, null,
                    periodeId, eleveId, new ArrayList<>(), withDomaineInfo, isYear, handler);
        }
        else {
            new DefaultUtilsService(this.eb).studentIdAvailableForPeriode(classeId, periodeId, typeClasse,
                    event -> {
                        if (event.isRight()) {
                            JsonArray ids = event.right().getValue();
                            getCompetencesNotesReleveEleves( ids, etablissementId, classeId,
                                    groupIds, matiereId, null, periodeId, eleveId, withDomaineInfo,
                                    isYear, handler);

                        } else {
                            handler.handle(new Either.Left<>("Error While getting Available student "));
                        }
                    });
        }
    }

    private void runGetCompetencesNotesReleve(String etablissementId, String classeId, JsonArray groupIds,
                                              String matiereId, List<String> matiereIds, Long periodeId,
                                              String eleveId, List<String> idEleves, Boolean withDomaineInfo,
                                              Boolean isYear,Handler<Either<String, JsonArray>> handler) {

        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT ")
                .append( (null != eleveId)? "DISTINCT": "")
                .append(" devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, ")
                .append(" devoirs.diviseur, devoirs.ramener_sur, competences_notes.evaluation ,")
                .append(" competences_notes.id_competence , devoirs.id_matiere, devoirs.id_sousmatiere, ")
                .append( (null != eleveId)? "": " competences_notes.id, ")
                .append((withDomaineInfo)? "compDom.id_domaine, " : "")
                .append(" devoirs.id_periode, competences_notes.id_eleve, devoirs.is_evaluated, ")
                .append("null as annotation, ")
                .append("competence_niveau_final.niveau_final AS niveau_final, type.formative")
                .append((!isYear)? "" : ", competence_niveau_final_annuel.niveau_final AS niveau_final_annuel")

                .append(" FROM "+ COMPETENCES_SCHEMA +".devoirs ")
                .append(" INNER JOIN "+ COMPETENCES_SCHEMA +".type ON (devoirs.id_type = type.id) ");

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
                query.append("(rel_devoirs_groupes.id_devoir = devoirs.id  AND rel_devoirs_groupes.id_groupe IN " +
                        Sql.listPrepared( UtilsConvert.jsonArrayToStringArr(groupIds)) + " )");
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
                "( competence_niveau_final.id_periode = devoirs.id_periode " +
                "AND competence_niveau_final.id_eleve = competences_notes.id_eleve " +
                "AND competence_niveau_final.id_competence = competences_notes.id_competence " +
                "AND competence_niveau_final.id_matiere = devoirs.id_matiere )");

        if(isYear) {
            query.append(" LEFT JOIN " + COMPETENCES_SCHEMA + ".competence_niveau_final_annuel ON " +
                    "( competence_niveau_final_annuel.id_eleve = competences_notes.id_eleve " +
                    "AND competence_niveau_final_annuel.id_competence = competences_notes.id_competence " +
                    "AND competence_niveau_final_annuel.id_matiere = devoirs.id_matiere )");
        }

        query.append(" WHERE devoirs.id_etablissement = ? ")
                .append((matiereIds != null || matiereId != null)?" AND devoirs.id_matiere IN "+((matiereIds != null)?Sql.listPrepared(matiereIds):"(?)")+" ":" ");

        values.add(etablissementId);

        if(null == matiereIds && matiereId != null){
            values.add(matiereId);
        }else if(null != matiereIds){
            for (String matiereIdToAdd : matiereIds) {
                values.add(matiereIdToAdd);
            }
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
                .append(" WHERE ");

        if (null != idMatiere) {
            query.append("id_matiere = ? AND");
            values.add(idMatiere);
        }
        if (!colonne.equals(POSITIONNEMENT) && idsClasse != null) {
            query.append(" id_classe IN " + Sql.listPrepared(idsClasse.getList()) + " AND");
            for (Object idClasse : idsClasse.getList()) {
                values.add(idClasse);
            }

        }
        if (null != idEleves) {
            query.append(" id_eleve IN " + Sql.listPrepared(idEleves.getList().toArray())+ " AND");
            for (int i = 0; i < idEleves.size(); i++) {
                values.add(idEleves.getString(i));
            }
        }
        if (null != idPeriode) {
            query.append(" id_periode = ? ");
            values.add(idPeriode);
        }
        if(query.toString().substring(query.length()-3,query.length()).equals("AND")){
            query.delete(query.length() - 3, query.length());
        }
        Sql.getInstance().prepared(query.toString(), values,
                new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L),
                validResultHandler(handler));
    }

    public void getColonneReleveTotale(JsonArray idEleves, Long idPeriode, JsonArray idsMatiere, JsonArray idsClasse, String idStructure,
                                       Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT moy.id_eleve, moy.id_periode, null as avis_conseil_orientation, null as avis_conseil_de_classe, null as synthese_bilan_periodique, null as positionnement, " +
                "moy.id_matiere, COALESCE(moy.moyenne, -100) AS moyenne, moy.id_classe FROM "+COMPETENCES_SCHEMA + ".moyenne_finale AS moy WHERE " );

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
        if (null != idStructure) {
            query.append(" AND IdTableAvisOrientation.id_etablissement = ? ");
            values.add(idStructure);
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
        if (null != idStructure) {
            query.append(" AND IdTableAvisConseil.id_etablissement = ? ");
            values.add(idStructure);
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
        if (null != idStructure) {
            query.append(" AND syntheseBP.id_etablissement = ? ");
            values.add(idStructure);
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

    public void getMoyennesMatieresByCoefficient(JsonArray moyFinalesEleves, JsonArray listNotes,
                                                 final JsonObject result, String idEleve, JsonArray idEleves){
        Map<Long, JsonArray> notesByCoef = new HashMap<>();
        if(isNull(result.getJsonObject(COEFFICIENT))) {
            result.put(COEFFICIENT, new JsonObject());
        }
        //pour toutes les notes existantes dans la classe
        for (int i = 0; i < listNotes.size(); i++) {
            JsonObject note = listNotes.getJsonObject(i);

            if (note.getString("valeur") == null
                    || !note.getBoolean("is_evaluated") || note.getString("coefficient") == null) {
                continue; //Si la note fait partie d'un devoir qui n'est pas évalué,
                // elle n'est pas prise en compte dans le calcul de la moyenne
            } else {
                Long coefMatiere = note.getLong("coef", 1L);

                if (!notesByCoef.containsKey(coefMatiere)) {
                    notesByCoef.put(coefMatiere, new JsonArray());
                }
                notesByCoef.get(coefMatiere).add(note);
            }
        }

        for(Map.Entry<Long, JsonArray> notesByCoefEntry : notesByCoef.entrySet()){
            Long coef = notesByCoefEntry.getKey();
            JsonArray notes = notesByCoefEntry.getValue();
            JsonObject resultCoef = new JsonObject();
            if(isNotNull(coef)) {
                final HashMap<Long,HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse =
                        calculMoyennesEleveByPeriode(notes, resultCoef, idEleve, idEleves);
                calculAndSetMoyenneClasseByPeriode(moyFinalesEleves, notesByDevoirByPeriodeClasse, resultCoef);
                if(isNull( result.getJsonObject(COEFFICIENT).getJsonObject(coef.toString()))){
                    result.getJsonObject(COEFFICIENT).put(coef.toString(), new JsonObject());
                }

                result.getJsonObject(COEFFICIENT).getJsonObject(coef.toString()).getMap().putAll(resultCoef.getMap());
                result.put("coef", coef.toString());
            }
        }


    }
    public HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>
    calculMoyennesEleveByPeriode (JsonArray listNotes, final JsonObject result, String idEleve, JsonArray idEleves) {

        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriode = new HashMap<>();
        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse = new HashMap<>();
        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesClasseBySousMat = new HashMap<>();
        HashMap<String, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeBySousMat = new HashMap<>();

        notesByDevoirByPeriode.put(null, new HashMap<>());
        notesByDevoirByPeriodeClasse.put(null, new HashMap<>());
        notesByDevoirByPeriodeBySousMat.put(null, new HashMap<>());
        notesClasseBySousMat.put(null, new HashMap<>());

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
                Long id_sousMatiere = note.getLong(ID_SOUS_MATIERE);

                if(!notesByDevoirByPeriode.containsKey(id_periode)) {
                    notesByDevoirByPeriode.put(id_periode, new HashMap<>());
                    notesByDevoirByPeriodeClasse.put(id_periode, new HashMap<>());
                    notesByDevoirByPeriodeBySousMat.put(id_periode.toString(), new HashMap<>());
                    notesClasseBySousMat.put(id_periode, new HashMap<>());
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
                    utilsService.addToMap(id_periode, notesByDevoirByPeriode.get(id_periode), noteDevoir);
                    utilsService.addToMap(null, notesByDevoirByPeriode.get(null), noteDevoir);
                    utilsService.addToMap(id_periode.toString(), id_sousMatiere, notesByDevoirByPeriodeBySousMat,
                            noteDevoir);
                }

                //ajouter la note à la période correspondante et à l'année pour toute la classe
                utilsService.addToMap(id_periode, notesByDevoirByPeriodeClasse.get(id_periode), noteDevoir);
                utilsService.addToMap(null, notesByDevoirByPeriodeClasse.get(null), noteDevoir);
                if(isNotNull(id_sousMatiere)){
                    utilsService.addToMap(id_sousMatiere, notesClasseBySousMat.get(id_periode), noteDevoir);
                    utilsService.addToMap(id_sousMatiere, notesClasseBySousMat.get(null), noteDevoir);
                }

            }
        }
        // permettra de stocker les moyennes des sousMatières par période
        result.put(MOYENNES, new JsonArray());
        result.put("_"+ MOYENNE, new JsonObject());
        HashMap<Long,JsonArray> listMoyDevoirs = new HashMap<>();

        // Calcul des moyennes par période pour l'élève
        for(Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> entryPeriode : notesByDevoirByPeriode.entrySet()) {

            //entryPeriode contient les notes de l'élève pour une période
            Long idPeriode = entryPeriode.getKey();
            String periodeKey = isNull(idPeriode)? "null" : idPeriode.toString();
            listMoyDevoirs.put(idPeriode, new JsonArray());
            result.getJsonObject("_"+ MOYENNE).put(periodeKey, new JsonObject());

            // Paramètres pour le calcul des moyennes
            final Boolean withStat = false;
            final Integer diviseur = 20;
            final Boolean annual = false;

            // calcul des moyennes des notes de la matière de l'élève
            for (Map.Entry<Long, ArrayList<NoteDevoir>> entry : entryPeriode.getValue().entrySet()) {
                JsonObject moyenne = utilsService.calculMoyenne(entry.getValue(), withStat, diviseur, annual);
                moyenne.put("id", idPeriode);
                listMoyDevoirs.get(idPeriode).add(moyenne);
            }

            // Calcul des moyennes des notes par sous-matières pour la période courante
            HashMap<Long, ArrayList<NoteDevoir>> notesSubMatForPeriode = notesByDevoirByPeriodeBySousMat.get(periodeKey);
            if(isNotNull(notesSubMatForPeriode)) {
                for (Map.Entry<Long, ArrayList<NoteDevoir>> smEntry : notesSubMatForPeriode.entrySet()) {
                    Long idSousMatiere = smEntry.getKey();
                    if (isNotNull(idSousMatiere)) {
                        JsonObject moyenne = utilsService.calculMoyenne(smEntry.getValue(), withStat, diviseur, annual);
                        result.getJsonObject("_" + MOYENNE).getJsonObject(periodeKey).put(idSousMatiere.toString(),
                                moyenne);
                    }
                }
            }

            //ajout des moyennes de l'élève sur chaque période au résultat final
            if (listMoyDevoirs.get(idPeriode).size() > 0) {
                result.getJsonArray(MOYENNES).add(listMoyDevoirs.get(idPeriode).getJsonObject(0));
            }
        }

        // calcul des moyenne classe des sousMatieres par période
        result.put("_" + MOYENNES_CLASSE, new JsonObject());
        for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> nClassSousMat : notesClasseBySousMat.entrySet()) {

            Long idPeriode = nClassSousMat.getKey();
            if (idPeriode != null) {
                result.getJsonObject("_" + MOYENNES_CLASSE).put(idPeriode.toString(), new JsonObject());
                for (Map.Entry<Long, ArrayList<NoteDevoir>> noteSousMat : nClassSousMat.getValue().entrySet()) {
                    Long idSousMat = noteSousMat.getKey();
                    ArrayList<NoteDevoir> notes = noteSousMat.getValue();
                    Double moyClasse = calculMoyenneClasseByPeriode(notes, new HashMap<>(), new HashMap<>(), idPeriode);
                    result.getJsonObject("_" + MOYENNES_CLASSE).getJsonObject(idPeriode.toString())
                            .put(idSousMat.toString(), moyClasse);
                }
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

    private void getMoyenneFinale(String idEleve, String idMatiere, Long idPeriode, JsonArray idsGroups,
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
                "     AND ( moyenne_finale.id_matiere = ?) " +
                "AND id_classe IN " + Sql.listPrepared(idsGroups.getList());
        JsonArray params = new JsonArray();
        if(idPeriode!=null){
            params.add(idPeriode);
        }
        params.add(idEleve).add(idMatiere);
        for(Object idGroup : idsGroups){
            params.add(idGroup);
        }
        Sql.getInstance().prepared( query, params,Competences.DELIVERY_OPTIONS, validResultHandler(handler));

    }

    private String nullAppreciation() {
        return " null AS appreciation_matiere_periode,  null AS id_classe_appreciation, " +
                " null AS id_periode_appreciation, " ;
    }
    private void getAppreciationMatierePeriode(String idEleve, String idMatiere, Long idPeriode, JsonArray idGroups,
                                               Handler<Either<String, JsonArray>> handler){
        String query = "SELECT appreciation_matiere_periode.appreciation_matiere_periode, " +
                " appreciation_matiere_periode.id_classe AS id_classe_appreciation, " +
                nullMoyenneFinale() + nullPositionnemetFinal() +
                " appreciation_matiere_periode.id_periode AS id_periode_appreciation " +
                " FROM notes.appreciation_matiere_periode " +
                " WHERE " +
                ((idPeriode!=null)?"(appreciation_matiere_periode.id_periode = ? ) AND ": "") +
                "    (appreciation_matiere_periode.id_eleve = ?) " +
                "     AND (appreciation_matiere_periode.id_matiere = ?) " +
                "AND id_classe IN "+ Sql.listPrepared(idGroups.getList());

        JsonArray params = new JsonArray();
        if(idPeriode!=null){
            params.add(idPeriode);
        }
        params.add(idEleve).add(idMatiere);
        for(Object idGroup : idGroups){
            params.add(idGroup);
        }

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
    public void getAppreciationMoyFinalePositionnement(String idEleve, String idMatiere, Long idPeriode, JsonArray idGroups,
                                                       Handler<Either<String, JsonArray>> handler) {


        Future<JsonArray> appreciationFuture = Future.future();
        getAppreciationMatierePeriode(idEleve, idMatiere, idPeriode, idGroups, event-> {
            formate(appreciationFuture, event);
        });

        Future<JsonArray> moyenneFinaleFuture = Future.future();
        getMoyenneFinale(idEleve, idMatiere, idPeriode, idGroups, event -> {
            formate(moyenneFinaleFuture, event);
        });

        Future<JsonArray> positionnementFinalFuture = Future.future();
        getPositionnementFinal(idEleve, idMatiere, idPeriode, event -> {
            formate(positionnementFinalFuture, event);
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

    public Double calculMoyenneByElevesByPeriode(JsonObject result, ArrayList<NoteDevoir> allNotes,
                                                Map<Long, Map<String, Double>> moyFinalesElevesByPeriode,
                                                 Map<Long, List<String>> moyFinalesNNElevesByPeriode,
                                                Long idPeriode ){

        if(!result.containsKey(NOTES_BY_PERIODE_BY_STUDENT)) {
            result.put(NOTES_BY_PERIODE_BY_STUDENT, new JsonObject());
        }
        String periodeKey = isNull(idPeriode)? "null": idPeriode.toString();
        if(!result.getJsonObject(NOTES_BY_PERIODE_BY_STUDENT).containsKey(periodeKey)){
            result.getJsonObject(NOTES_BY_PERIODE_BY_STUDENT).put(periodeKey, new JsonObject());
        }

        HashMap<String, ArrayList<NoteDevoir>> notesPeriodeByEleves = new HashMap<>();
        //mettre dans notesPeriodeByEleves idEleve -> notes de l'élève pour la période
        for(NoteDevoir note : allNotes){
            String id_eleve = note.getIdEleve();
            if(!(moyFinalesNNElevesByPeriode != null && moyFinalesNNElevesByPeriode.containsKey(idPeriode) && moyFinalesNNElevesByPeriode.get(idPeriode).contains(id_eleve))) {
                if (!notesPeriodeByEleves.containsKey(id_eleve)) {
                    notesPeriodeByEleves.put(id_eleve, new ArrayList<NoteDevoir>());
                }
                notesPeriodeByEleves.get(id_eleve).add(note);
            }
        }

        Integer nbEleve = notesPeriodeByEleves.size();
        Double sumMoyClasse = 0.0;
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
            Double moyEleve;
            if(moyFinalesPeriode != null && moyFinalesPeriode.containsKey(idEleve)){
                moyEleve = moyFinalesPeriode.get(idEleve);
            } else {
                moyEleve = utilsService.calculMoyenne(notesPeriodeByEleve.getValue(),
                        false, 20,false).getDouble("moyenne");
            }
            sumMoyClasse = sumMoyClasse + moyEleve;
            result.getJsonObject(NOTES_BY_PERIODE_BY_STUDENT).getJsonObject(periodeKey).put(idEleve, moyEleve);
        }
        return (double) Math.round((sumMoyClasse/nbEleve) * 100) / 100;
    }
    public Double calculMoyenneClasseByPeriode(ArrayList<NoteDevoir> allNotes,
                                               Map<Long, Map<String, Double>> moyFinalesElevesByPeriode,
                                               Map<Long, List<String>> moyFinalesNNElevesByPeriode,
                                               Long idPeriode){


        return calculMoyenneByElevesByPeriode(new JsonObject(), allNotes, moyFinalesElevesByPeriode, moyFinalesNNElevesByPeriode, idPeriode);
    }

    public void calculAndSetMoyenneClasseByPeriode(final JsonArray moyFinalesEleves,
                                                   final HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse,
                                                   final JsonObject result) {

        JsonArray moyennesClasses = new fr.wseduc.webutils.collections.JsonArray();

        Map<Long, Map<String, Double>> moyFinales = null;
        Map<Long, List<String>> moyFinalesNN = null;

        if (moyFinalesEleves != null && moyFinalesEleves.size() > 0) {
            moyFinales = new HashMap<>();
            moyFinalesNN = new HashMap<>();
            for (Object o : moyFinalesEleves) {
                JsonObject moyFinale = (JsonObject) o;
                if(isNotNull(moyFinale.getValue("moyenne")) && isNotNull(moyFinale.getValue("id_periode"))) {
                    Long periode = moyFinale.getLong("id_periode");

                    if (!moyFinales.containsKey(periode)) {
                        moyFinales.put(periode, new HashMap<>());
                    }
                    moyFinales.get(periode).put(moyFinale.getString("id_eleve"),
                            Double.parseDouble(moyFinale.getString("moyenne").replace(",",".")));
                }else if(isNull(moyFinale.getValue("moyenne")) && isNotNull(moyFinale.getValue("id_periode"))) {
                    Long periode = moyFinale.getLong("id_periode");
                    if (!moyFinalesNN.containsKey(periode)) {
                        moyFinalesNN.put(periode, new ArrayList<String>());
                    }
                    moyFinalesNN.get(periode).add(moyFinale.getString("id_eleve"));
                }
            }
            //cas où il n'y a que des compétences et avec des moyennes finales pour une période <=> notesByDevoirByPeriodeClasse n'a pas cette période
            //Pour cette periode, il faut calculer la moyenne de la classe à partir des moyennes finales
            //Donc on vérifie que pour chaque période où il y a des moyennes finale, il y a aussi des notes
            //sinon il faut calculer la moyenne de classe à partir des moyennes finales
            for (Map.Entry<Long, Map<String, Double>> mapMoysFinales : moyFinales.entrySet()) {
                if (!notesByDevoirByPeriodeClasse.containsKey(mapMoysFinales.getKey())) {
                    StatClass statClass = new StatClass();
                    for (Map.Entry<String, Double> moyFinale : mapMoysFinales.getValue().entrySet()) {
                        statClass.putMapEleveStat(moyFinale.getKey(), moyFinale.getValue(), null);
                    }
                    moyennesClasses.add(new JsonObject().put("id", mapMoysFinales.getKey())
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
                        calculMoyenneByElevesByPeriode(result, allNotes, moyFinales, moyFinalesNN, idPeriode));
                moyennesClasses.add(moyennePeriodeClasse);
            }
        }

        //si moyennesClasses.size()> 0 c'est qu'il y a eu soit des moyennees finales soit des notes sur au moins un trimestre
        // alors on peut calculer la moyenne de la classe pour l'année
        if (moyennesClasses.size() > 0) {
            Double sumMoyPeriode = 0.0;
            for (int i = 0; i < moyennesClasses.size(); i++) {
                sumMoyPeriode += moyennesClasses.getJsonObject(i).getDouble("moyenne");
            }
            JsonObject moyennePeriodeClasse = new JsonObject();
            moyennePeriodeClasse.put("id", (JsonObject) null).put("moyenne",
                    (double) Math.round((sumMoyPeriode / moyennesClasses.size()) * 100) / 100);
            moyennesClasses.add(moyennePeriodeClasse);
        }
        result.put(MOYENNES_CLASSE, moyennesClasses);

    }


    public void setRankAndMinMaxInClasseByPeriode(final Long idPeriodAsked, final String idEleve, final HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse,
                                                  final JsonArray moyFinalesEleves, final JsonObject result) {

        JsonArray ranks = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray allMinMax = new fr.wseduc.webutils.collections.JsonArray();

        for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByPeriode :notesByDevoirByPeriodeClasse.entrySet()) {

            Long idPeriode = notesByPeriode.getKey();

            if (idPeriode != null) {
                //get all scores on the periode
                ArrayList<NoteDevoir> allNotes = notesByPeriode.getValue().get(notesByPeriode.getKey());

                //group all scores by student
                Map<String, List<NoteDevoir>> allNotesByEleve = allNotes.stream().collect(Collectors.groupingBy(NoteDevoir::getIdEleve));

                //Group note final by eleves
                Map<String, Double> allMoyennesFinales = new HashMap<>();

                //Group Non Noted Final average by eleves
                ArrayList<String> allMoyennesFinalesNN = new ArrayList<String>();

                for (int i = 0; i < moyFinalesEleves.size(); i++) {
                   JsonObject mf = moyFinalesEleves.getJsonObject(i);
                   if(isNotNull(mf) && isNotNull(mf.getValue("id_periode")) && isNotNull(mf.getValue("id_eleve"))
                           && isNotNull(mf.getValue("moyenne"))) {
                       Long periodeMF = mf.getLong("id_periode");
                       String idEleveMF = mf.getString("id_eleve");
                       if (idPeriode == periodeMF && idEleve.equals(idEleveMF)) {
                           allMoyennesFinales.put(mf.getString("id_eleve"),
                                   Double.parseDouble(mf.getString("moyenne").replace(",",".")));
                       }
                   } else if (isNotNull(mf) && isNotNull(mf.getValue("id_periode")) && isNotNull(mf.getValue("id_eleve"))
                           && isNull(mf.getValue("moyenne"))){
                       Long periodeMF = mf.getLong("id_periode");
                       if (idPeriode == periodeMF) {
                           allMoyennesFinalesNN.add(mf.getString("id_eleve"));
                       }
                   }
                }


                //calculate the average score with coef by student
                Map<String, Double> allMoyennes = new HashMap<>();
                final Double[] min = {null};
                final Double[] max = {null};
                allNotesByEleve.forEach((key, value) -> {
                    if(!allMoyennesFinalesNN.contains(key)) {
                        JsonObject moyenne = utilsService.calculMoyenne(value, false, 20, false);
                        Double moyenneTmp = moyenne.getDouble("moyenne");

                        //manage min value
                        if (min[0] == null || min[0] > moyenneTmp) {
                            min[0] = moyenneTmp;
                        }
                        //manage max value
                        if (max[0] == null || max[0] < moyenneTmp) {
                            max[0] = moyenneTmp;
                        }

                        if (allMoyennesFinales.containsKey(key)) {
                            moyenneTmp = allMoyennesFinales.get(key);
                        }
                        allMoyennes.put(key, moyenneTmp);
                    }
                });

                if (min[0] != null && max[0] != null) {
                    JsonObject minMaxObj = new JsonObject();
                    minMaxObj.put("id_periode", idPeriode)
                            .put("min", min[0])
                            .put("max", max[0]);
                    if(idPeriodAsked.equals(idPeriode)){
                        allMinMax.add(minMaxObj);
                    }
                }

                //order average scores in classes
                Map<String, Double> allMoyennesSorted = allMoyennes.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (oldValue, newValue) -> oldValue, LinkedHashMap::new));

                //find the target student's rank
                int rank = -1;
                List keys = new ArrayList(allMoyennesSorted.keySet());
                for (int i = 0; i < keys.size(); i++) {
                    Object obj = keys.get(i);
                    if (obj != null){
                        //manage rank
                        if (obj.equals(idEleve)) {
                            rank = i + 1;
                            i = allNotes.size() -1;
                        }
                    }
                }

                //if student have rank we put it in the final object
                if(rank >= 0 ){
                    JsonObject rankObj = new JsonObject();
                    rankObj.put("id_periode", idPeriode)
                            .put("rank", rank)
                            .put("rank_size", allMoyennesSorted.size());
                    if (idPeriodAsked.equals(idPeriode)) {
                        ranks.add(rankObj);
                    }
                }
            }
        }
        result.put(STUDENT_RANK, ranks);
        result.put(CLASS_AVERAGE_MINMAX, allMinMax);
    }

    private Map<String, JsonArray> groupeNotesByStudent(JsonArray allNotes) {
        Map<String, JsonArray> notesByStudent = new HashMap<>();
        for (int i = 0; i < allNotes.size(); i++) {
            JsonObject note = allNotes.getJsonObject(i);
            String idStudent = note.getString(ID_ELEVE);

            if (!notesByStudent.containsKey(idStudent)) {
                notesByStudent.put(idStudent, new JsonArray().add(note));
            } else {
                notesByStudent.get(idStudent).add(note);
            }
        }
        return notesByStudent;
    }
    private void getMaxCompNoteByPeriode(JsonArray listCompNotes,
                                         HashMap<Long, ArrayList<NoteDevoir>> notesByDevoirByPeriode,
                                         HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByPeriodeBySousMatiere,
                                         JsonArray tableauConversion){
        notesByDevoirByPeriode.put(null, new ArrayList<>());

        // 1- parcourir la listCompNotes pour en extraire les map map<idP, JAcompNote> et map<idP,map<idssM,JAcompNote>>
        HashMap<Long, JsonArray> compNotesByPeriode = new HashMap<>();
        HashMap<Long,HashMap<Long,JsonArray>> compNotesBySousMatByPeriode = new HashMap<>();

        for(int i = 0; i < listCompNotes.size(); i++){
            JsonObject compNote = listCompNotes.getJsonObject(i);
            Long id_periode = compNote.getLong(ID_PERIODE);
            Long idSousMatiere = compNote.getLong(ID_SOUS_MATIERE);

            Boolean isFormative = compNote.getBoolean("formative");
            if (isFormative) {
                continue;
            }

            if (compNote.getLong("evaluation") == null || compNote.getLong("evaluation") < 0) {
                continue; //Si pas de compétence Note
            }
            if (!compNotesByPeriode.containsKey(id_periode)) {
                compNotesByPeriode.put(id_periode, new JsonArray());
                compNotesBySousMatByPeriode.put(id_periode, new HashMap<>());
            }
            if(isNotNull(idSousMatiere)){
                utilsService.addToMapWithJsonArray(idSousMatiere,compNotesBySousMatByPeriode.get(id_periode),compNote);
            }
            utilsService.addToMapWithJsonArray(id_periode,compNotesByPeriode,compNote);
        }
        //2- loop map<idP, JAcompNote> -> JAcompNoteMaxByPeriode -> map<idP,list<NoteDevoirMax>> = notesByDevoirByPeriode
        compNotesByPeriode.forEach((id_period,compNotes)->{
            Map<String, JsonObject> MaxCompNotesByCompetence = calculMaxCompNoteItem(compNotes, tableauConversion,true);

            MaxCompNotesByCompetence.forEach((id_comp,maxCompNote)->{
                NoteDevoir noteDevoir;

                Long niveauFinal = maxCompNote.getLong("niveau_final");
                Long niveauFinalAnnuel = maxCompNote.getLong("niveau_final_annuel");
                if (isNotNull(niveauFinalAnnuel) ) {
                    noteDevoir = new NoteDevoir(Double.valueOf(niveauFinalAnnuel)+1,1.0,false,1.0);
                } else if (isNotNull(niveauFinal) ) {
                    noteDevoir = new NoteDevoir(Double.valueOf(niveauFinal)+1,1.0,false,1.0);
                } else {
                    noteDevoir = new NoteDevoir(Double.valueOf(maxCompNote.getLong("evaluation"))+1, 1.0,
                            false, 1.0);
                }
                utilsService.addToMap(id_period, notesByDevoirByPeriode, noteDevoir);
                // positionnement sur l'année
                utilsService.addToMap(null, notesByDevoirByPeriode, noteDevoir);
            });

        });

        //3-loop map<idP,map<idssM,JAcompNote>> ->JAcompNoteMaxBySousMatByPerode -> map<idP,map<idssM,list<NoteDevoirMax>> = notesByPeriodeBySousMatiere
        compNotesBySousMatByPeriode.forEach((id_periode,mapSousMatCompNotes)->{
            mapSousMatCompNotes.forEach((id_sousMat, compNotesSousMat)-> {
                Map<String,JsonObject> MaxCompNoteSousMatByComp = calculMaxCompNoteItem(compNotesSousMat, tableauConversion,
                        false);

                MaxCompNoteSousMatByComp.forEach((id_comp,maxCompNoteSousMat) -> {
                    NoteDevoir noteDevoir = new NoteDevoir(Double.valueOf(maxCompNoteSousMat.getLong("evaluation"))+1,
                            1.0, false, 1.0);

                    if(!notesByPeriodeBySousMatiere.containsKey(id_periode)) notesByPeriodeBySousMatiere.put(id_periode,
                            new HashMap());
                    utilsService.addToMap(id_sousMat,notesByPeriodeBySousMatiere.get(id_periode),noteDevoir);

                });

            });
        });


    }

    public void calculPositionnementAutoByEleveByMatiere(JsonArray listCompNotes, JsonObject result, Boolean annual, JsonArray tableauConversion) {

        HashMap<Long, JsonArray> listMoyDevoirs = new HashMap<>();
        HashMap<Long, ArrayList<NoteDevoir>> notesByDevoirByPeriode = new HashMap<>();
        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByPeriodeBySousMatiere = new HashMap<>();

        result.put(POSITIONNEMENTS_AUTO, new JsonArray());
        result.put("_" + POSITIONNEMENTS_AUTO, new JsonObject());

        getMaxCompNoteByPeriode(listCompNotes, notesByDevoirByPeriode,notesByPeriodeBySousMatiere, tableauConversion);

        // Paramètre de calcul des moyennes de compétencesNotes
        Boolean withStat = false;
        Integer diviseur = 1;

        // Calcul des moyennes des compétencesNotes par période pour L'élève
        for (Map.Entry<Long, ArrayList<NoteDevoir>> entryPeriode: notesByDevoirByPeriode.entrySet()) {
            listMoyDevoirs.put(entryPeriode.getKey(), new JsonArray());
            Long idPeriode = entryPeriode.getKey();
            ArrayList<NoteDevoir> compNotesPeriode = entryPeriode.getValue();
            JsonObject moyenneMatiere = utilsService.calculMoyenne(compNotesPeriode, withStat, diviseur, annual);
            moyenneMatiere.put("id_periode", idPeriode);
            listMoyDevoirs.get(idPeriode).add(moyenneMatiere);

            // Calcul des moyennes des compétencesNotes par sousMatiere
            if(isNotNull(idPeriode)) {
                HashMap<Long, ArrayList<NoteDevoir>> moyCompNoteBySousMat = notesByPeriodeBySousMatiere.get(idPeriode);
                if (isNotNull(moyCompNoteBySousMat)) {
                    JsonObject sousMatMoy = new JsonObject();
                    for (Map.Entry<Long, ArrayList<NoteDevoir>> entry : moyCompNoteBySousMat.entrySet()) {
                        JsonObject moyenne = utilsService.calculMoyenne(entry.getValue(), withStat, diviseur, annual);
                        Long idSousMatiere = entry.getKey();
                        sousMatMoy.put(idSousMatiere.toString(), moyenne);
                    }
                    result.getJsonObject("_" + POSITIONNEMENTS_AUTO).put(idPeriode.toString(), sousMatMoy);
                }
            }

            if (listMoyDevoirs.get(idPeriode).size() > 0) {
                result.getJsonArray(POSITIONNEMENTS_AUTO).add(listMoyDevoirs.get(idPeriode).getJsonObject(0));
            } else {
                result.getJsonArray(POSITIONNEMENTS_AUTO).add(new JsonObject()
                        .put("moyenne", -1)
                        .put("id_periode", idPeriode));
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
                "COALESCE(moyenne_finale.moyenne,-100) AS moyenne_finale, moyenne_finale.id_matiere AS id_mat_moyf " +
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
    public void getMoysEleveByMatByPeriode(String idClasse, Integer idPeriode,
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
                            List<Eleve> eleves = new ArrayList<>(responseListEleve.right().getValue());

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
                                                final String idClass;
                                                if (responseQuerry.isLeft()) {
                                                    String error = responseQuerry.left().getValue();
                                                    log.error(error);
                                                    handler.handle(new Either.Left<>(error));
                                                } else {
                                                    JsonArray idClasseGroups = responseQuerry.right().getValue();

                                                    if (!(idClasseGroups != null && !idClasseGroups.isEmpty())) {
                                                        idsGroups.add(idClasse);
                                                        handler.handle(new Either.Left<>("idClasseGroups null"));
                                                    } else {
                                                        idsGroups.add(idClasseGroups.getJsonObject(0)
                                                                .getString("id_classe") );
                                                        idsGroups.addAll(idClasseGroups.getJsonObject(0)
                                                                .getJsonArray("id_groupes"));
                                                        nameClasse = idClasseGroups.getJsonObject(0)
                                                                .getString("name_classe");
                                                        idClass = idClasseGroups.getJsonObject(0)
                                                                .getString("id_classe");
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
                                                                                    if (respNoteMoyFinale.getString("moyenne_finale") != null && !respNoteMoyFinale.getString("moyenne_finale").equals("-100")) {

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

                                                                                    } else if(!(respNoteMoyFinale.getString("moyenne_finale") != null && respNoteMoyFinale.getString("moyenne_finale").equals("-100"))) {//pas de moyFinale => set mapIdEleveIdMatListNotes
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
                                                                                        Double moy = utilsService.calculMoyenne(noteDevoirList, false, 20,false).getDouble("moyenne");
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
                                                                                            .put("id_class", idClass)
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
                                                                                                        1.0));

                                                                                                if (mapIdMatListMoyByEleve.containsKey(idMatOfAllMat)) {
                                                                                                    mapIdMatListMoyByEleve.get(idMatOfAllMat)
                                                                                                            .add(new NoteDevoir(
                                                                                                                    mapIdMatMoy.get(idMatOfAllMat),
                                                                                                                    new Double(20),
                                                                                                                    false,
                                                                                                                    1.0));
                                                                                                } else {
                                                                                                    List<NoteDevoir> listMoyEleve = new ArrayList<>();
                                                                                                    listMoyEleve.add(new NoteDevoir(
                                                                                                            mapIdMatMoy.get(idMatOfAllMat),
                                                                                                            new Double(20),
                                                                                                            false,
                                                                                                            1.0));
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
                                                                                                1.0));
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
                                                                                setJOResultWithList(resultMoysElevesByMat, listMoyGeneraleEleve);
                                                                                elevesResult = Utils.sortElevesByDisplayName(elevesResult);
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
    public void getMoysEleveByMatByYear(JsonArray periodes, SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
                                        Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve,
                                        Handler<Either<String, JsonObject>> handler) {


        Eleves eleves = new Eleves();
        List<Future> futureListPeriodes = new ArrayList<>();
        for(int i = 0; i < periodes.size(); i++){
            Future<JsonObject> futurePeriode = Future.future();
            futureListPeriodes.add(futurePeriode);
            JsonObject periode = periodes.getJsonObject(i);
            Map<String, List<NoteDevoir>> mapIdMatListMoyByEleveByPeriode = new LinkedHashMap<>();
            getMoysEleveByMatByPeriode(periode.getString("id_classe"), periode.getInteger("id_type"),
                    mapAllidMatAndidTeachers, mapIdMatListMoyByEleveByPeriode, new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> event) {
                            if(event.isLeft()){
                                futurePeriode.fail(event.left().getValue());
                                handler.handle(new Either.Left<>( event.left().getValue()));
                                log.error(event.left().getValue());

                            }else{

                                JsonObject resultElevePeriode = event.right().getValue();
                                Object moyClass = resultElevePeriode.getValue("moyClassAllEleves");
                                JsonArray elevesJsonArray = resultElevePeriode.getJsonArray("eleves");

                                eleves.setEleves(elevesJsonArray,"id_matiere", "moyenneByMat",
                                        "eleveMoyByMat", "moyGeneraleEleve");
                                futurePeriode.complete();
                            }
                        }
                    });
        }

        CompositeFuture.all(futureListPeriodes).setHandler( event -> {
            if(event.failed()){
                handler.handle(new Either.Left<>( event.cause().getMessage()));
                log.error(event.cause().getMessage());
            }else{

                JsonObject result = new JsonObject();
                List<NoteDevoir> moysElevesByYear = new ArrayList<NoteDevoir>();
                JsonArray elevesJA =  eleves.buildJsonArrayEleves(mapIdMatListMoyByEleve,
                        mapAllidMatAndidTeachers,moysElevesByYear);
                setJOResultWithList(result, moysElevesByYear);
                result.put("eleves",elevesJA);
                result.put("nbEleves", elevesJA.size());
                handler.handle( new Either.Right<>(result));
            }

        });
    }

    private void setJOResultWithList(JsonObject resultJO, List<NoteDevoir> notesList){
        if(notesList.isEmpty()){
            resultJO.put("moyClassAllEleves", "");
            resultJO.put("moyMinClass", "");
            resultJO.put("moyMaxClass", "");
        }else {
            JsonObject resultCalculMoy =  utilsService.calculMoyenne(notesList, true,null,false);
            resultJO.put("moyClassAllEleves", resultCalculMoy.getDouble("moyenne"));
            resultJO.put("moyMinClass", resultCalculMoy.getDouble("noteMin"));
            resultJO.put("moyMaxClass", resultCalculMoy.getDouble("noteMax"));
        }
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
                        } else{
                            matiereJson.put("moyClass", "");
                            matiereJson.put("moyMinClass", "");
                            matiereJson.put("moyMaxClass", "");
                        }
                        matieresResult.add(matiereJson);
                    }
                    JsonObject resultMatieres = new JsonObject();
                    resultMatieres.put("matieres", matieresResult);
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
        resultFinal.put(MOYSPAN, params.getValue(MOYSPAN));

        for(int i = 0; i <students.size(); i++) {
            JsonObject student = students.getJsonObject(i);
            if(i==0) {
                Boolean hasLevel = student.getString(LEVEL) != null;
                resultFinal.put("hasLevel", hasLevel);
                if(hasLevel) {
                    resultFinal.put(LEVEL, student.getString(LEVEL).charAt(0));
                }
                resultFinal.put(SOUS_MATIERES, student.getValue(SOUS_MATIERES));
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
                event ->  formate(elementProgrammeFuture, event));

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
        getNbEvaluatedHomeWork(idClasse, idMatiere, idPeriode, null, event -> {
            formate(nbEvaluatedHomeWork, event);
        });

        // Récupération du tableau de conversion
        Future<JsonArray> tableauDeConversionFuture = Future.future();
        // On récupère le tableau de conversion des compétences notes pour Lire le positionnement
        new DefaultCompetenceNoteService(COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE)
                .getConversionNoteCompetence(idEtablissement, idClasse,
                        tableauEvent -> formate(tableauDeConversionFuture, tableauEvent));

        // Récupération de l'appréciation de la classe
        Future<JsonArray> appreciationClassFuture = Future.future();
        if (idPeriode != null) {
            new DefaultAppreciationService(Competences.COMPETENCES_SCHEMA,
                    Competences.APPRECIATIONS_TABLE).getAppreciationClasse(
                    new String[]{idClasse}, Integer.valueOf(idPeriode.intValue()),
                    new String[]{idMatiere}, appreciationsEither -> {
                        formate(appreciationClassFuture, appreciationsEither);
                    });
        }
        else {
            appreciationClassFuture.complete(new JsonArray());
        }

        // Récupération des sousMatieres
        Future<JsonArray> sousMatiereFuture = Future.future();
        new DefaultMatiereService().getSousMatieres(idMatiere, event -> formate(sousMatiereFuture, event));

        // Avec les ids des élèves de la classe, récupération des moyennes Finales , des Notes, des Competences Notes
        // et des Appreciations et des Positionnements finaux
        CompositeFuture.all(studentsClassFuture, appreciationClassFuture, tableauDeConversionFuture,
                nbEvaluatedHomeWork, sousMatiereFuture)
                .setHandler( idElevesEvent -> {
                    if(idElevesEvent.succeeded()) {
                        putParamSousMatiere(sousMatiereFuture.result(), params);
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
                                    formate(moyennesFinalesFutures, moyennesFinalesEvent);
                                });

                        Future<JsonArray> appreciationsFutures = Future.future();
                        if (idPeriode != null) {
                            getColonneReleve(idEleves, idPeriode, idMatiere, new JsonArray().add(idClasse),
                                    APPRECIATION_MATIERE_PERIODE,
                                    appreciationEvent -> formate(appreciationsFutures, appreciationEvent));
                        }
                        else {
                            appreciationsFutures.complete(new JsonArray());
                        }
                        // Récupération des positionnements finaux
                        Future<JsonArray> positionnementsFinauxFutures = Future.future();
                        if(idPeriode != null) {
                            getColonneReleve(idEleves, idPeriode, idMatiere, new JsonArray().add(idClasse),
                                    POSITIONNEMENT, positionnementsFinauxEvent ->
                                            formate(positionnementsFinauxFutures, positionnementsFinauxEvent));
                        }
                        else {
                            positionnementsFinauxFutures.complete(new JsonArray());
                        }
                        // Récupération des Notes du Relevé
                        Future<JsonArray> notesFuture = Future.future();
                        Boolean hasEvaluatedHomeWork = (nbEvaluatedHomeWork.result().getLong("nb") > 0);
                        if(idEleve == null) {
                            getNotesReleveEleves(idEleves, idEtablissement, idClasse, idMatiere, idPeriode,
                                    false, null, null,
                                    (Either<String, JsonArray> notesEvent) -> formate(notesFuture, notesEvent));
                        }
                        else {
                            if (!hasEvaluatedHomeWork) {
                                notesFuture.complete(new JsonArray());
                            }
                            else {
                                if (idPeriode != null) {
                                    getNoteElevePeriode(idEleve, idEtablissement, new JsonArray().add(idClasse),
                                            idMatiere, idPeriode, notesEvent -> formate(notesFuture, notesEvent));
                                }
                                else {
                                    getNotesReleve(idEtablissement, idClasse, idMatiere,null, typeClasse,
                                            false,null, notesEvent ->
                                                    formate(notesFuture, notesEvent));
                                }
                            }
                        }

                        // Récupération des Compétences-Notes du Relevé
                        Future<JsonArray> compNotesFuture = Future.future();
                        getCompetencesNotesReleveEleves(idEleves, idEtablissement, idClasse,null, idMatiere,
                                null, idPeriode,null, true, false, compNotesEvent ->
                                        formate(compNotesFuture, compNotesEvent));

                        List<Future> listFutures = new ArrayList<>(Arrays.asList(compNotesFuture, notesFuture,
                                moyennesFinalesFutures, appreciationsFutures, positionnementsFinauxFutures));

                        CompositeFuture.all(listFutures).setHandler( event -> {
                            if(event.succeeded()) {
                                // Rajout des moyennes finales
                                FormateColonneFinaleReleve(moyennesFinalesFutures.result(), elevesMapObject,
                                        MOYENNE, idPeriode, hasEvaluatedHomeWork);

                                // Rajout des notes par devoir et Calcul des moyennes auto
                                resultHandler.put(NOTES, notesFuture.result());
                                calculMoyennesNotesFOrReleve(notesFuture.result(), resultHandler, idPeriode,
                                        elevesMapObject, hasEvaluatedHomeWork, isExport, false, null);

                                resultHandler.put(COMPETENCES_NOTES_KEY, compNotesFuture.result());
                                if (idPeriode != null) {
                                    // Cacul du positionnement auto
                                    calculMoyennesCompetencesNotesForReleve(compNotesFuture.result(), resultHandler,
                                            idPeriode, tableauDeConversionFuture.result(), elevesMapObject);

                                    // Format sous matières moyennes
                                    addSousMatieres(elevesMapObject, sousMatiereFuture.result(),
                                            resultHandler, idPeriode);

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

    public void getTotaleDatasReleve(final JsonObject params, final Long idPeriode, final boolean annual,
                                     final Handler<Either<String, JsonObject>> handler){
        try{
            final String idEtablissement = params.getString(Competences.ID_ETABLISSEMENT_KEY);
            final String idClasse = params.getString(Competences.ID_CLASSE_KEY);
            final JsonArray idMatieres = params.getJsonArray("idMatieres");
            final Integer typeClasse = params.getInteger(Competences.TYPE_CLASSE_KEY);
            final JsonArray idEleves = new fr.wseduc.webutils.collections.JsonArray();
            final JsonObject resultHandler = new JsonObject();
            final JsonObject data = new JsonObject();
            final JsonArray idGroups = ((params.getJsonArray("idGroups").size() == 0)? null : params.getJsonArray("idGroups"));
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
                                formate(tableauDeConversionFuture, tableauEvent);
                            });
            List<Future> listFuturesFirst = new ArrayList<>(
                    Arrays.asList(studentsClassFuture,tableauDeConversionFuture));
            for (Object idMatiere : idMatieres){
                // Récupération du  nombre de devoirs avec évaluation numérique
                Future<JsonObject> nbEvaluatedHomeWork = Future.future();
                getNbEvaluatedHomeWork(idClasse, idMatiere.toString(), idPeriode, idGroups, event -> {
                    formate(nbEvaluatedHomeWork, event);
                });
                listFuturesFirst.add(nbEvaluatedHomeWork);
            }
            // Avec les ids des élèves de la classe, récupération des moyennes Finales , des Notes, des Competences Notes
            // et des Appreciations et des Positionnements finaux
            CompositeFuture.all(listFuturesFirst)
                    .setHandler( idElevesEvent -> {
                        try{
                            if(idElevesEvent.succeeded()) {

                                // Récupération des moyennes, positionnement Finales, appréciations, avis conseil de classe et orientation
                                Future<JsonArray> bigRequestFuture = Future.future();
                                getColonneReleveTotale(idEleves, idPeriode, idMatieres, new JsonArray().add(idClasse), idEtablissement,
                                        event -> formate(bigRequestFuture, event));

                                // Récupération des Notes du Relevé
                                Future<JsonArray> notesFuture = Future.future();
                                getNotesReleveEleves(idEleves, idEtablissement, idClasse, null, idPeriode,
                                        false, idGroups, idMatieres,
                                        notesEvent -> formate(notesFuture, notesEvent));

                                // Récupération des Compétences-Notes du Relevé
                                Future<JsonArray> compNotesFuture = Future.future();
                                getCompetencesNotesReleveEleves(idEleves, idEtablissement, idClasse, idGroups, null,
                                        idMatieres, idPeriode, null, true, annual, compNotesEvent -> {
                                            formate(compNotesFuture, compNotesEvent);
                                        });

                                List<Future> listFutures = new ArrayList<>(
                                        Arrays.asList(bigRequestFuture, compNotesFuture,notesFuture)
                                );
                                CompositeFuture.all(listFutures).setHandler( event -> {
                                    try{
                                        if(event.succeeded()) {
                                            // Rajout des moyennes finales
                                            for (int i=2; i<idMatieres.size()+2;i++){
                                                // Récupération du  nombre de devoirs avec évaluation numérique
                                                Boolean hasEvaluatedHomeWork = (((JsonObject)listFuturesFirst.get(i).result())
                                                        .getLong("nb") > 0);

                                                JsonArray notesMatiere = new JsonArray();

                                                for(Object note : notesFuture.result()){
                                                    if(((JsonObject)note).getString("id_matiere").equals(idMatieres.getString(i-2))){
                                                        notesMatiere.add(note);
                                                    }
                                                }

                                                if(data.containsKey(NOTES)){
                                                    data.getJsonObject(NOTES).put(idMatieres.getString(i-2),notesMatiere);
                                                }else{
                                                    JsonObject jsonNotesToAdd = new JsonObject();
                                                    jsonNotesToAdd.put(idMatieres.getString(i-2),notesMatiere);
                                                    data.put(NOTES, jsonNotesToAdd);
                                                }

                                                JsonObject resultNotes = new JsonObject();

                                                calculMoyennesNotesFOrReleve(notesMatiere, resultNotes, idPeriode,
                                                        elevesMapObject, hasEvaluatedHomeWork,false, annual, idMatieres.getString(i-2));
                                                if( data.containsKey(MOYENNE)){
                                                    data.getJsonObject(MOYENNE).put(idMatieres.getString(i-2),resultNotes);
                                                }else{
                                                    JsonObject jsonToAdd = new JsonObject();
                                                    jsonToAdd.put(idMatieres.getString(i-2),resultNotes);
                                                    data.put(MOYENNE, jsonToAdd);
                                                }

                                            }
                                            for (Object idMatiere : idMatieres){
                                                JsonArray notesMatiere = new JsonArray();

                                                for(Object note : compNotesFuture.result()){
                                                    if(((JsonObject)note).getString("id_matiere").equals(idMatiere.toString())){
                                                        notesMatiere.add(note);
                                                    }
                                                }
                                                if( data.containsKey(COMPETENCES_NOTES_KEY)){
                                                    data.getJsonObject(COMPETENCES_NOTES_KEY).put(idMatiere.toString(),notesMatiere);
                                                }else{
                                                    JsonObject jsonToAdd = new JsonObject();
                                                    jsonToAdd.put(idMatiere.toString(),notesMatiere);
                                                    data.put(COMPETENCES_NOTES_KEY, jsonToAdd);
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
                                                        calculPositionnementAutoByEleveByMatiere(compNotesEleve, resultNotes,annual, tableauDeConversionFuture.result());
                                                        if( eleveObject.containsKey(POSITIONNEMENT_AUTO)){
                                                            eleveObject.getJsonObject(POSITIONNEMENT_AUTO).put(idMatiere.toString(),
                                                                    resultNotes.getJsonArray(POSITIONNEMENTS_AUTO));
                                                        }else{
                                                            JsonObject jsonToAdd = new JsonObject();
                                                            jsonToAdd.put(idMatiere.toString(),resultNotes.getJsonArray(POSITIONNEMENTS_AUTO));
                                                            eleveObject.put(POSITIONNEMENT_AUTO, jsonToAdd);
                                                        }
                                                        JsonObject positionnement = utilsService.getObjectForPeriode(
                                                                eleveObject.getJsonObject(POSITIONNEMENT_AUTO).getJsonArray(idMatiere.toString()),
                                                                idPeriode, ID_PERIODE);
                                                        String positionnement_auto  = "";
                                                        if (positionnement != null) {
                                                            positionnement_auto = positionnement.getFloat(MOYENNE).toString();
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
                                                getMoyenneMinMaxByMatiere(elevesMapObject,idPeriode,idMatieres.getString(i-2),annual,resultHandler);
                                            }
                                            getMoyenneGeneraleMinMax(elevesMapObject,idPeriode,idMatieres, annual, resultHandler);
                                            //Rajout des appreciations par élèves
                                            FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                                    SYNTHESE_BILAN_PERIODIQUE, idPeriode, false, "");
                                            FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                                    AVIS_CONSEIL_DE_CLASSE, idPeriode, false, "");
                                            FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                                    AVIS_CONSEIL_ORIENTATION, idPeriode, false, "");

                                            handler.handle(new Either.Right<>(resultHandler.put(ELEVES, new DefaultExportBulletinService(eb, null)
                                                    .sortResultByClasseNameAndNameForBulletin(elevesMapObject))));
                                        } else {
                                            handler.handle(new Either.Left<>(event.cause().getMessage()));
                                            if(idElevesEvent.failed()) {
                                                log.error("getTotaleDatasReleve (idElevesEvent.failed()): " +
                                                        idElevesEvent.cause());
                                            }
                                        }
                                    } catch (Exception error) {
                                        log.error("listFuturesFirst failed : " + error);
                                        handler.handle(new Either.Left<>("listFuturesFirst failed : " + error));
                                    }
                                });
                            }
                            else {
                                handler.handle(new Either.Left<>(idElevesEvent.cause().getMessage()));
                                log.error("getTotaleDatasReleve (idElevesEvent.failed()): " +
                                        idElevesEvent.cause());
                            }
                        } catch (Exception error) {
                            log.error("getTotaleDatasReleve (prepare data) failed: " + error);
                            handler.handle(new Either.Left<>("getTotaleDatasReleve (prepare data) failed : " + error));
                        }
                    });
        } catch (Exception error) {
            log.error("getTotaleDatasReleve (prepare data) failed: " + error);
            handler.handle(new Either.Left<>("getTotaleDatasReleve (prepare data) failed : " + error));
        }
    }

    private <T> void calculMoyennesNotesFOrReleve (JsonArray listNotes, JsonObject result, Long idPeriode,
                                                   Map<String, JsonObject> eleveMapObject,  Boolean hasEvaluatedHomeWork,
                                                   Boolean isExport, Boolean annual,
                                                   String idMatiere) {

        // Si pour il n'y a pas de devoirs avec évaluation numérique, la moyenne auto est NN
        if (!hasEvaluatedHomeWork) {
            if(idMatiere == null){
                for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                    student.getValue().put(MOYENNE, NN);
                }
            }
            else {
                for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                    if (student.getValue().containsKey(MOYENNE)) {
                        if (student.getValue().getValue(MOYENNE).getClass() == Double.class) {
                            student.getValue().remove(MOYENNE);
                            JsonObject jsonToAdd = new JsonObject();
                            jsonToAdd.put(idMatiere, NN);
                            student.getValue().put(MOYENNE, jsonToAdd);
                        } else {
                            student.getValue().getJsonObject(MOYENNE).put(idMatiere, NN);
                        }
                    } else {
                        JsonObject jsonToAdd = new JsonObject();
                        jsonToAdd.put(idMatiere, NN);
                        student.getValue().put(MOYENNE, jsonToAdd);
                    }
                }
            }
        }
        else {

            JsonArray listMoyDevoirs = new fr.wseduc.webutils.collections.JsonArray();
            JsonArray listMoyEleves = new fr.wseduc.webutils.collections.JsonArray();
            HashMap<Long, ArrayList<NoteDevoir>> notesByDevoir = new HashMap<>();
            HashMap<String, T> notesByEleve = new HashMap<>();
            HashMap<String,HashMap<Long,ArrayList<NoteDevoir>>> notesByEleveBySousMatiere = new HashMap<>();

            Map<String, HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>>
                    notesByDevoirByPeriodeByEleve = new HashMap<>();
            Map<String, HashMap<Long, HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>>>
                    notesByDevoirByPeriodeByEleveBySousMatiere = new HashMap<>();

            if (idPeriode == null) {
                result.put(MOYENNES, new JsonArray());
                result.put("_"+ MOYENNES, new JsonArray());
            }

            String matiereId = idMatiere;
            for (int i = 0; i < listNotes.size(); i++) {
                JsonObject note = listNotes.getJsonObject(i);
                if (note.getString(VALEUR) == null || note.getString(COEFFICIENT) == null ||
                        !note.getBoolean(IS_EVALUATED)) {
                    continue; //Si la note fait partie d'un devoir qui n'est pas évalué,
                    // elle n'est pas prise en compte dans le calcul de la moyenne
                }

                if(isNull(matiereId)){
                    matiereId = listNotes.getJsonObject(i).getString(ID_MATIERE);
                }
                Long sousMatiereId = listNotes.getJsonObject(i).getLong(ID_SOUS_MATIERE);
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
                        notesByDevoirByPeriodeByEleveBySousMatiere.put(idEleve, new HashMap<>());
                        notesByDevoirByPeriodeByEleveBySousMatiere.get(idEleve).put(null, new HashMap<>());
                    }

                    if (!notesByDevoirByPeriodeByEleve.get(idEleve).containsKey(id_periode)) {
                        notesByDevoirByPeriodeByEleve.get(idEleve).put(id_periode, new HashMap<>());
                        notesByDevoirByPeriodeByEleveBySousMatiere.get(idEleve).put(id_periode, new HashMap<>());
                    }

                    if (note.getString(ID_ELEVE).equals(idEleve)) {
                        utilsService.addToMap(id_periode,
                                notesByDevoirByPeriodeByEleve.get(idEleve).get(id_periode), noteDevoir);
                        utilsService.addToMap(null, notesByDevoirByPeriodeByEleve.get(idEleve).get(null),
                                noteDevoir);
                        Map<Long, HashMap<Long, ArrayList<NoteDevoir>>> m =
                                notesByDevoirByPeriodeByEleveBySousMatiere.get(idEleve).get(id_periode);
                        Map<Long, HashMap<Long, ArrayList<NoteDevoir>>> mNull =
                                notesByDevoirByPeriodeByEleveBySousMatiere.get(idEleve).get(null);
                        if(isNotNull(sousMatiereId)) {
                            if (!m.containsKey(sousMatiereId)) {
                                m.put(sousMatiereId, new HashMap<>());
                                mNull.put(sousMatiereId, new HashMap<>());
                            }
                            utilsService.addToMap(id_periode, m.get(sousMatiereId), noteDevoir);
                            utilsService.addToMap(null, mNull.get(sousMatiereId), noteDevoir);
                        }
                    }
                }
                utilsService.addToMap(idDevoir, notesByDevoir, noteDevoir);
                utilsService.addToMap(idEleve, (HashMap<String, ArrayList<NoteDevoir>>) notesByEleve, noteDevoir);
                if(isNotNull(sousMatiereId)) {
                    utilsService.addToMap(idEleve, sousMatiereId, notesByEleveBySousMatiere, noteDevoir);
                }
                utilsService.addToMap(idEleve, null, notesByEleveBySousMatiere, noteDevoir);
            }

            // Calcul des Moyennes de la classe par devoir
            for (Map.Entry<Long, ArrayList<NoteDevoir>> entry : notesByDevoir.entrySet()) {
                JsonObject moyenne = utilsService.calculMoyenneParDiviseur(entry.getValue(), true);
                moyenne.put("id", entry.getKey());
                listMoyDevoirs.add(moyenne);
            }
            result.put("devoirs", listMoyDevoirs);



            Double sumMoyClasse = 0.0;
            int nbMoyenneClasse = 0;
            Double min = null;
            Double max = null;

            Map<Long, Double>  mapSumMoyClasse = new HashMap<>();
            Map<Long, Integer> mapNbMoyenneClasse = new HashMap<>();
            Map<Long, Double> mapMin = new HashMap<>();
            Map<Long, Double> mapMax = new HashMap<>();
            // Calcul des moyennes par élève
            for (Map.Entry<String, T> entry : notesByEleve.entrySet()) {
                JsonObject moyenne;
                String idEleve = entry.getKey();

                moyenne = utilsService.calculMoyenne((ArrayList<NoteDevoir>) entry.getValue(),
                        false, 20, annual);
                JsonObject submoyenne = new JsonObject().put(matiereId, new JsonObject());
                if(isNotNull(notesByEleveBySousMatiere.get(idEleve))) {
                    for (Map.Entry<Long, ArrayList<NoteDevoir>> subEntry :
                            notesByEleveBySousMatiere.get(idEleve).entrySet()) {
                        Long idSousMat = subEntry.getKey();
                        JsonObject moyenSousMat = utilsService.calculMoyenne(subEntry.getValue(),
                                false, 20, annual);
                        String key = (isNull(idSousMat)? "null" : idSousMat.toString());
                        submoyenne.getJsonObject(matiereId).put(key, moyenSousMat);

                        if(isNotNull(moyenSousMat)) {
                            if (!mapNbMoyenneClasse.containsKey(idSousMat)) {
                                mapNbMoyenneClasse.put(idSousMat, 0);
                                mapSumMoyClasse.put(idSousMat, 0.0);
                            }
                            Double moySousMat = moyenSousMat.getDouble(MOYENNE);
                            int nbSousMoyClass = mapNbMoyenneClasse.get(idSousMat);
                            Double sumMoySous = mapSumMoyClasse.get(idSousMat);
                            mapNbMoyenneClasse.put(idSousMat, nbSousMoyClass + 1);
                            mapSumMoyClasse.put(idSousMat, sumMoySous + moySousMat);
                            if(isNull(mapMax.get(idSousMat)) || mapMax.get(idSousMat).compareTo(moySousMat) < 0) {
                                mapMax.put(idSousMat, moySousMat);
                            }
                            if(isNull(mapMin.get(idSousMat)) || mapMin.get(idSousMat).compareTo(moySousMat) > 0) {
                                mapMin.put(idSousMat, moySousMat);
                            }
                        }
                    }

                }
                if (eleveMapObject.containsKey(idEleve)) {
                    Double moy = moyenne.getDouble(MOYENNE);
                    JsonObject el = eleveMapObject.get(idEleve);
                    Double moyEl = null;
                    if (idMatiere != null) {

                        if (!el.containsKey(MOYENNE)) {
                            el.put(MOYENNE, new JsonObject());
                        }
                        el.getJsonObject(MOYENNE).put(idMatiere, moy);

                        if (el.containsKey(HAS_NOTE)) {
                            el.getJsonObject(HAS_NOTE).put(idMatiere, moyenne.getBoolean(HAS_NOTE));
                        } else {
                            JsonObject moyMat = new JsonObject();
                            moyMat.put(idMatiere, moyenne.getBoolean(HAS_NOTE));
                            el.put(HAS_NOTE, moyMat);
                        }

                        if (!el.containsKey(MOYENNEFINALE)) {
                            JsonObject moyMat = new JsonObject();
                            moyMat.put(idMatiere, moy);
                            el.put(MOYENNEFINALE, moyMat);
                        } else if (!el.getJsonObject(MOYENNEFINALE).containsKey(idMatiere)) {
                            el.getJsonObject(MOYENNEFINALE).put(idMatiere, moy);
                        }

                        if (el.getJsonObject(MOYENNEFINALE).containsKey(idMatiere)) {
                            if(isNotNull(el.getJsonObject(MOYENNEFINALE).getValue(idMatiere))) {
                                try {
                                    moyEl = Double.valueOf(el.getJsonObject(MOYENNEFINALE).getString(idMatiere));
                                    sumMoyClasse += moyEl;

                                } catch (ClassCastException c) {
                                    moyEl = el.getJsonObject(MOYENNEFINALE).getDouble(idMatiere);
                                    sumMoyClasse += moyEl;
                                }
                            }else{
                                el.put("hasMoyenneFinaleNN",true);
                            }
                        } else {
                            moyEl = moy;
                            sumMoyClasse += moyEl;
                        }
                    } else {
                        el.put(MOYENNE, moy).put(HAS_NOTE, moyenne.getBoolean(HAS_NOTE));

                        if (isExport && !el.containsKey(MOYENNEFINALE)) {
                            el.put(MOYENNEFINALE, moy);
                        }
                        if (el.containsKey(MOYENNEFINALE)) {
                            if(isNotNull(el.getValue(MOYENNEFINALE))) {
                                try {
                                    moyEl = Double.valueOf(el.getString(MOYENNEFINALE));
                                    sumMoyClasse += moyEl;
                                } catch (ClassCastException c) {
                                    moyEl = el.getDouble(MOYENNEFINALE);
                                    sumMoyClasse += moyEl;
                                }
                            }else{
                                el.put("hasMoyenneFinaleNN",true);
                            }
                        } else {
                            moyEl = moy;
                            sumMoyClasse += moyEl;
                        }
                    }
                    if(isNotNull(moyEl)) {
                        if (isNull(min) || isNull(max)) {
                            min = moyEl;
                            max = moyEl;
                        }
                        if (moyEl < min) {
                            min = moyEl;
                        }
                        if(moyEl > max){
                            max = moyEl;
                        }
                        ++nbMoyenneClasse;
                    }
                    if (!el.containsKey("_" +MOYENNE)) {
                        el.put("_"+ MOYENNE, new JsonObject());
                    }
                    if(!el.getJsonObject("_"+MOYENNE).containsKey(matiereId)){
                        el.getJsonObject("_"+MOYENNE).put(matiereId, new JsonObject());
                    }
                    el.getJsonObject("_"+ MOYENNE).getJsonObject(matiereId).getMap()
                            .putAll(submoyenne.getJsonObject(matiereId).getMap());
                }

                moyenne.put("id", idEleve);
                listMoyEleves.add(moyenne);
            }

            Object moyClasse;
            DecimalFormat decimalFormat = new DecimalFormat("#.00");
            if(!annual) {
                moyClasse = (nbMoyenneClasse > 0) ? decimalFormat.format((sumMoyClasse / nbMoyenneClasse)) : " ";
                result.put("moyenne_classe", moyClasse);

            }else{
                moyClasse = (nbMoyenneClasse > 0) ? (sumMoyClasse / nbMoyenneClasse) : " ";
                result.put("moyenne_classe", moyClasse);
            }
            result.put("_moyenne_classe", new JsonObject());
            JsonObject moyClasseObj = new JsonObject().put("min", min).put("max", max).put(MOYENNE, moyClasse);
            result.getJsonObject("_moyenne_classe").put("nullFinal", moyClasseObj);
            for(Map.Entry<Long, Double> sousMatMoyClasse : mapSumMoyClasse.entrySet()){
                Double moySousMat = sousMatMoyClasse.getValue();
                Long idSousMat = sousMatMoyClasse.getKey();
                String key = (isNull(idSousMat)? "null" : idSousMat.toString());
                int nbSousMoyClass = mapNbMoyenneClasse.get(idSousMat);
                Object moySous = (nbSousMoyClass > 0) ? (moySousMat / nbSousMoyClass) : " ";
                JsonObject moySousMatCl = new JsonObject().put("min", mapMin.get(idSousMat))
                        .put("max", mapMax.get(idSousMat)).put(MOYENNE, decimalFormat.format(moySous));
                result.getJsonObject("_moyenne_classe").put(key, moySousMatCl);
            }
            if (idPeriode == null) {
                HashMap<Long, JsonArray> listMoy = new HashMap<>();

                for (Map.Entry<String, HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>> entryEleve
                        : notesByDevoirByPeriodeByEleve.entrySet()) {
                    for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> entryPeriode
                            : notesByDevoirByPeriodeByEleve.get(entryEleve.getKey()).entrySet()) {
                        listMoy.put(entryPeriode.getKey(), new fr.wseduc.webutils.collections.JsonArray());
                        JsonArray moyennesFinales;
                        if (idMatiere != null) {
                            if (!eleveMapObject.get(entryEleve.getKey()).containsKey(MOYENNES)) {
                                JsonObject matMoy = new JsonObject();
                                matMoy.put(idMatiere, new JsonArray());
                                eleveMapObject.get(entryEleve.getKey()).put(MOYENNES, matMoy);
                            } else if (!eleveMapObject.get(entryEleve.getKey()).getJsonObject(MOYENNES).containsKey(idMatiere)) {
                                eleveMapObject.get(entryEleve.getKey()).getJsonObject(MOYENNES).put(idMatiere, new JsonArray());
                            }
                            moyennesFinales = eleveMapObject.get(entryEleve.getKey()).getJsonObject(MOYENNESFINALES).getJsonArray(idMatiere);
                        } else {
                            if (!eleveMapObject.get(entryEleve.getKey()).containsKey(MOYENNES)) {
                                eleveMapObject.get(entryEleve.getKey()).put(MOYENNES, new JsonArray());
                            }
                            moyennesFinales = eleveMapObject.get(entryEleve.getKey()).getJsonArray(MOYENNESFINALES);
                        }
                        for (Map.Entry<Long, ArrayList<NoteDevoir>> entry :
                                entryPeriode.getValue().entrySet()) {
                            JsonObject moyenne = utilsService.calculMoyenne(entry.getValue(), false,
                                    20, annual);

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
                                        moyenne = utilsService.calculMoyenne(notePeriode, false, 20,
                                                annual);
                                        sumMoy += moyenne.getDouble(MOYENNE);
                                    }
                                    moyenne.remove(MOYENNE);
                                    moyenne.put(MOYENNE, sumMoy / nbMoy);
                                } else {
                                    JsonObject moyObj = utilsService.getObjectForPeriode(moyennesFinales,
                                            entry.getKey(), ID_PERIODE);
                                    if (moyObj != null) {
                                        Double moy = Double.valueOf(moyObj.getString(MOYENNE));
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
                            if (idMatiere != null) {
                                eleveMapObject.get(entryEleve.getKey()).getJsonObject(MOYENNES).getJsonArray(idMatiere).add(moyenne);
                            } else {
                                eleveMapObject.get(entryEleve.getKey()).getJsonArray(MOYENNES).add(moyenne);
                            }
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

    private <T> void calculMoyennesCompetencesNotesForReleve (JsonArray listCompNotes, JsonObject result, Long idPeriode,
                                                              JsonArray tableauDeconversion,
                                                              Map<String, JsonObject> eleveMapObject) {

        Map<String, JsonArray> notesByEleve = groupeNotesByStudent(listCompNotes);

        for (Map.Entry<String, JsonArray> entry : notesByEleve.entrySet()) {
            String idEleve = entry.getKey();
            JsonArray compNotesEleve = entry.getValue();

            if(eleveMapObject.containsKey(idEleve)) {
                JsonObject eleveObject = eleveMapObject.get(idEleve);
                if (eleveObject.getJsonArray(COMPETENCES_NOTES_KEY) == null) {
                    eleveObject.put(COMPETENCES_NOTES_KEY, compNotesEleve);
                }
                if(!eleveObject.getJsonArray(COMPETENCES_NOTES_KEY).equals(compNotesEleve)){
                    log.info("calculMoyennesCompetencesNotesForReleve get difference");
                    log.warn(compNotesEleve.encode() + " _\n "  +
                            eleveObject.getJsonArray(COMPETENCES_NOTES_KEY).encode());
                }
                calculPositionnementAutoByEleveByMatiere(compNotesEleve, eleveObject,false, tableauDeconversion);
                JsonObject positionnement = utilsService.getObjectForPeriode(
                        eleveObject.getJsonArray(POSITIONNEMENTS_AUTO), idPeriode, ID_PERIODE);

                JsonObject posiSousMatiere = eleveObject.getJsonObject("_" + POSITIONNEMENTS_AUTO);

                if(isNotNull(posiSousMatiere) && isNotNull(idPeriode)){
                    JsonObject positionnementSousMatiere =  posiSousMatiere.getJsonObject(idPeriode.toString());
                    if(isNotNull(positionnementSousMatiere)){
                        Map<String, Object> posiMap = positionnementSousMatiere.getMap();
                        for (Map.Entry<String, Object> o : posiMap.entrySet()){
                            JsonObject posi_sous_matiere = ((JsonObject)o.getValue());
                            Float moyennePositionnement =  posi_sous_matiere.getFloat(MOYENNE);
                            String pos = utilsService.convertPositionnement(moyennePositionnement,
                                    tableauDeconversion,false);
                            posi_sous_matiere.put(MOYENNE, pos);
                            posi_sous_matiere.put(POSITIONNEMENT, pos);
                        }
                    }
                }

                String positionnement_auto  = "";
                if (positionnement != null) {
                    Float moyennePositionnement =  positionnement.getFloat(MOYENNE);

                    positionnement_auto = utilsService.convertPositionnement(moyennePositionnement,
                            tableauDeconversion,false);
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
                        JsonArray eleves = body.getJsonArray("results");
                        eleves = Utils.sortElevesByDisplayName(eleves);
                        for (int i = 0; i < eleves.size(); i++) {
                            JsonObject eleve = eleves.getJsonObject(i);
                            String idEleve = eleve.getString("id");
                            idEleves.add(idEleve);
                            eleve.put(CLASSE_NAME_KEY, eleve.getString("level"));
                            eleve.put(NAME, eleve.getString(LAST_NAME_KEY));
                            eleve.put(DISPLAY_NAME_KEY, eleve.getString(LAST_NAME_KEY) + " "
                                    + eleve.getString(FIRST_NAME_KEY));
                            eleveMapObject.put(idEleve, eleve);
                        }
                        studentsClassFuture.complete(eleves);
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

    private void getMoyenneMinMaxByMatiere(Map<String, JsonObject> eleveMapObject, Long idPeriode, String idMatiere, Boolean annual, JsonObject resulHandler) {
        String moyenneLabel = MOYENNE;
        moyenneLabel += (idPeriode!=null)? "Finale" : "sFinales";
        Double moyenne = 0.0;
        int nbElevesMoyenne = 0;
        Double moyennePos = 0.0;
        int nbElevesPositionnement = 0;
        Double moyenneMin = 0.0;
        Double moyenneMax = 0.0;
        double positionnementMin = 0.0;
        double positionnementMax = 0.0;
        boolean initialisationMoyenne = false;
        boolean initialisationPositionnement = false;


        for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
            if (student.getValue().containsKey(moyenneLabel) &&
                    student.getValue().getJsonObject(moyenneLabel).containsKey(idMatiere)){
                if(student.getValue().getJsonObject(moyenneLabel).getValue(idMatiere) != NN &&
                        student.getValue().getJsonObject(moyenneLabel).getValue(idMatiere) != "" &&
                        student.getValue().getJsonObject(moyenneLabel).getValue(idMatiere) != null){
                    Double number;
                    try {
                        number = Double.valueOf(student.getValue().getJsonObject(moyenneLabel).getString(idMatiere));
                    } catch (ClassCastException c) {
                        number = student.getValue().getJsonObject(moyenneLabel).getDouble(idMatiere);
                    }
                    moyenne += number;
                    nbElevesMoyenne++;
                    if(!initialisationMoyenne){
                        moyenneMin = number;
                        moyenneMax = number;
                        initialisationMoyenne = true;
                    }else{
                        if(moyenneMin > number)
                            moyenneMin = number;
                        if(moyenneMax < number)
                            moyenneMax = number;
                    }
                }
            }
            if (student.getValue().containsKey(POSITIONNEMENT) &&
                    student.getValue().getJsonObject(POSITIONNEMENT).containsKey(idMatiere)){
                if(student.getValue().getJsonObject(POSITIONNEMENT).getValue(idMatiere) != NN &&
                        student.getValue().getJsonObject(POSITIONNEMENT).getValue(idMatiere) != "" &&
                        student.getValue().getJsonObject(POSITIONNEMENT).getValue(idMatiere) != null){
                    Double number;
                    try {
                        number = Double.parseDouble(student.getValue().getJsonObject(POSITIONNEMENT).getString(idMatiere).replace(",","."));
                    } catch (ClassCastException c) {
                        number = student.getValue().getJsonObject(POSITIONNEMENT).getDouble(idMatiere);
                    }
                    moyennePos += number;
                    nbElevesPositionnement++;
                    if(!initialisationPositionnement){
                        positionnementMin = number;
                        positionnementMax = number;
                        initialisationPositionnement = true;
                    }else{
                        if(positionnementMin >  number)
                            positionnementMin =  number;
                        if(positionnementMax <  number)
                            positionnementMax =  number;
                    }
                }
            }
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.00");

        JsonObject statsToAdd = new JsonObject();
        if(!initialisationPositionnement){
            JsonObject minPos = new JsonObject().put("minimum", NN);
            statsToAdd.put("positionnement",minPos);
            statsToAdd.getJsonObject("positionnement").put("maximum", NN);
            statsToAdd.getJsonObject("positionnement").put("moyenne", NN);
        }else{
            JsonObject minPos = new JsonObject().put("minimum", positionnementMin);
            statsToAdd.put("positionnement",minPos);
            statsToAdd.getJsonObject("positionnement").put("maximum", positionnementMax);
            if(!annual) {
                if (moyennePos.compareTo((double) 0) != 0) {
                    statsToAdd.getJsonObject("positionnement").put("moyenne", decimalFormat.format((moyennePos / nbElevesPositionnement)));
                }else {
                    statsToAdd.getJsonObject("positionnement").put("moyenne", Double.valueOf(0));
                }
            }else
                statsToAdd.getJsonObject("positionnement").put("moyenne", (moyennePos/nbElevesPositionnement));

        }
        if(!initialisationMoyenne){
            JsonObject minPos = new JsonObject().put("minimum", NN);
            statsToAdd.put("moyenne",minPos);
            statsToAdd.getJsonObject("moyenne").put("maximum", NN);
            statsToAdd.getJsonObject("moyenne").put("moyenne", NN);
        }else{
            JsonObject minPos = new JsonObject().put("minimum", moyenneMin);
            statsToAdd.put("moyenne",minPos);
            statsToAdd.getJsonObject("moyenne").put("maximum", moyenneMax);
            if(!annual)
                statsToAdd.getJsonObject("moyenne").put("moyenne",  decimalFormat.format((moyenne/nbElevesMoyenne)));
            else
                statsToAdd.getJsonObject("moyenne").put("moyenne",  (moyenne/nbElevesMoyenne));

        }

        if (resulHandler.containsKey("statistiques"))
            resulHandler.getJsonObject("statistiques").put(idMatiere, statsToAdd);
        else {
            JsonObject statsOfMat = new JsonObject();
            statsOfMat.put(idMatiere, statsToAdd);
            resulHandler.put("statistiques", statsOfMat);
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
                                    if(!data.getValue(colonne).equals("-100"))
                                        eleve.getJsonObject(resultLabel).put(idMatiere, data.getValue(colonne));
                                    else
                                        eleve.getJsonObject(resultLabel).put(idMatiere, NN);
                                }else if(!data.getValue(colonne).equals("-100")){
                                    JsonObject jsonToAdd = new JsonObject();
                                    jsonToAdd.put(idMatiere, data.getValue(colonne));
                                    eleve.put(resultLabel, jsonToAdd);
                                }else{
                                    JsonObject jsonToAdd = new JsonObject();
                                    jsonToAdd.put(idMatiere, NN);
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

    private void getMoyenneGeneraleMinMax(Map<String, JsonObject> eleveMapObject, Long idPeriode, JsonArray idMatieres, Boolean annual, JsonObject resulHandler) {
        String moyenneLabel = MOYENNE;
        moyenneLabel += (idPeriode!=null)? "Finale" : "sFinales";
        double moyenneDeMoyenne = 0.0;
        int nbElevesMoyenne = 0;
        double moyenneMin = 0.0;
        double moyenneMax = 0.0;
        boolean initialisationMoyenne = false;
        DecimalFormat decimalFormat = new DecimalFormat("#.00");

        for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
            Double moyenne = 0.0;
            int nbMatieres = 0;
            for (Object idMatiere : idMatieres) {
                String idMat = (String)idMatiere;
                if (student.getValue().containsKey(moyenneLabel) && student.getValue().getJsonObject(moyenneLabel).containsKey(idMat)) {
                    if (student.getValue().getJsonObject(moyenneLabel).getValue(idMat) != NN && student.getValue().getJsonObject(moyenneLabel).getValue(idMat) != "" && student.getValue().getJsonObject(moyenneLabel).getValue(idMat) != null) {
                        try {
                            moyenne += Double.parseDouble(student.getValue().getJsonObject(moyenneLabel).getString(idMat).replace(",","."));
                        } catch (ClassCastException c) {
                            moyenne += student.getValue().getJsonObject(moyenneLabel).getDouble(idMat);
                        }
                        nbMatieres++;
                    }
                }
            }
            if(nbMatieres == 0){
                student.getValue().put("moyenne_generale",NN);
            }else{
                if(!annual)
                    student.getValue().put("moyenne_generale",decimalFormat.format((moyenne/nbMatieres)));
                else
                    student.getValue().put("moyenne_generale",(moyenne/nbMatieres));
                moyenneDeMoyenne += moyenne/nbMatieres;
                nbElevesMoyenne++;
                if (!initialisationMoyenne) {
                    moyenneMin = moyenne/nbMatieres;
                    moyenneMax = moyenne/nbMatieres;
                    initialisationMoyenne = true;
                } else {
                    if (moyenneMin > moyenne/nbMatieres)
                        moyenneMin = moyenne/nbMatieres;
                    if (moyenneMax < moyenne/nbMatieres)
                        moyenneMax = moyenne/nbMatieres;
                }
            }
        }
        if(nbElevesMoyenne == 0){
            JsonObject minMoy = new JsonObject().put("minimum", NN);
            resulHandler.getJsonObject("statistiques").put("moyenne_generale",minMoy);
            resulHandler.getJsonObject("statistiques").getJsonObject("moyenne_generale").put("maximum", NN);
            resulHandler.getJsonObject("statistiques").getJsonObject("moyenne_generale").put("moyenne", NN);
        }else{
            JsonObject minMoy = new JsonObject().put("minimum", decimalFormat.format(moyenneMin));
            resulHandler.getJsonObject("statistiques").put("moyenne_generale",minMoy);
            resulHandler.getJsonObject("statistiques").getJsonObject("moyenne_generale").put("maximum", decimalFormat.format(moyenneMax));
            resulHandler.getJsonObject("statistiques").getJsonObject("moyenne_generale").put("moyenne", decimalFormat.format((moyenneDeMoyenne / nbElevesMoyenne)));
        }
    }

    private void getNbEvaluatedHomeWork(String idClasse, String idMatiere, Long idPeriode, JsonArray idsGroup,
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
                .append(table_rel_devoir_groupes).append(" ON id_devoir = id   AND ")
                .append((null != idsGroup) ? "id_groupe IN " + Sql.listPrepared(idsGroup.getList()) : "id_groupe = ?")
                .append(" AND id_matiere = ? AND is_evaluated = true ")
                .append((idPeriode!=null)? " AND id_periode = ? " : " ");

        if(idsGroup != null)
            for (int i = 0; i < idsGroup.size(); i++) {
                values.add(idsGroup.getString(i));
            }
        else
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
        getDatasReleve(param, event -> formate(exportResult, event));

        String key = ELEVES;
        String idStructure = param.getString(ID_ETABLISSEMENT_KEY);
        Map<String, JsonObject> mapEleve = new HashMap<>();
        Future<JsonObject> structureFuture = Future.future();
        mapEleve.put(key, new JsonObject().put(ID_ETABLISSEMENT_KEY, idStructure));

        // Récupération des informations sur l'établissment
        new DefaultExportBulletinService(eb, null).getStructure(key,mapEleve.get(key), event ->
                formate(structureFuture, event));

        // Récupération du logo de l'établissment
        Future<JsonObject> imgStructureFuture = Future.future();
        utilsService.getParametersForExport(idStructure, event -> formate(imgStructureFuture, event));

        // Récupération du libellé de la période
        Future<String> periodeLibelleFuture = Future.future();
        getLibellePeriode(eb, request, param.getInteger(ID_PERIODE_KEY),  periodeLibelleEvent -> {
            formate(periodeLibelleFuture, periodeLibelleEvent );
        });

        CompositeFuture.all(exportResult, structureFuture, imgStructureFuture, periodeLibelleFuture)
                .setHandler((event -> {
                    if (event.failed()) {
                        log.info(event.cause().getMessage());
                        badRequest(request);
                    }
                    else{
                        JsonObject exportJson = exportResult.result();
                        putLibelleAndParamsForExportReleve(exportJson, param);

                        JsonObject imgStructure =  imgStructureFuture.result();
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
                }));
    }
    public void getDataGraph(final String idEleve, JsonArray groupIds, final String idEtablissement ,
                             final String idClasse,final Integer typeClasse, final String idPeriodeString,
                             final Boolean isYear, final Handler<Either<String, JsonArray>> handler) {

        final Long idPeriode = (idPeriodeString != null)? Long.parseLong(idPeriodeString): null;

        // 1. On récupère les CompétencesNotes de toutes les matières et de tous les élèves
        Future<JsonArray> compNoteF = Future.future();
        getCompetencesNotesReleve(idEtablissement,idClasse, groupIds,null, idPeriode,null,
                typeClasse,true, isYear, eventReleve ->  formate(compNoteF, eventReleve));

        // 2. On récupère les Notes de toutes les matières et de tous les élèves
        Future<JsonArray> noteF = Future.future();
        getNotesReleve(idEtablissement, idClasse, null, idPeriode, typeClasse, true,
                groupIds, event -> formate(noteF, event));

        // 3. On récupère toutes les matières de l'établissement
        Future<JsonArray> subjectF = Future.future();
        JsonObject action = new JsonObject().put("action", "matiere.getMatieresForUser").put("userType", "Personnel")
                .put("idUser", "null").put("idStructure", idEtablissement).put("onlyId", false);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                subjectF.complete(body.getJsonArray(RESULTS));
            } else {
                subjectF.fail(body.getString(MESSAGE));
            }
        }));

        // Récupération du tableau de conversion
        Future<JsonArray> tableauDeConversionFuture = Future.future();
        // On récupère le tableau de conversion des compétences notes pour Lire le positionnement
        new DefaultCompetenceNoteService(COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE)
                .getConversionNoteCompetence(idEtablissement, idClasse,
                        tableauEvent -> formate(tableauDeConversionFuture, tableauEvent));

        CompositeFuture.all(compNoteF, noteF, subjectF, tableauDeConversionFuture).setHandler(event -> {
            if(event.failed()){
                String message = "[getReleveDataForGraph] " + event.cause().getMessage();
                log.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }

            final JsonArray listCompNotes = compNoteF.result();
            final JsonArray listNotes = noteF.result();
            Map<String,JsonArray> matieresCompNotes = new HashMap<>();
            Map<String,JsonArray> matieresCompNotesEleve = new HashMap<>();
            JsonArray idMatieres;

            // 4. On regroupe  les compétences notes par idMatière
            idMatieres = groupDataByMatiere(listCompNotes, matieresCompNotes, matieresCompNotesEleve, idEleve,
                    true);

            // 5. On regroupe les notes par idMatière
            Map<String,JsonArray> matieresNotes = new HashMap<>();
            Map<String,JsonArray> matieresNotesEleve = new HashMap<>();
            idMatieres = utilsService.saUnion(groupDataByMatiere(listNotes, matieresNotes, matieresNotesEleve, idEleve,
                    false), idMatieres);
            StatMat statMat = new StatMat();
            statMat.setMapIdMatStatclass(listNotes);
            Map<String, StatClass> mapMatieresStatClasseAndEleve = statMat.getMapIdMatStatclass();

            // 6. On récupère tous les libelles des matières de l'établissement et on fait correspondre
            // aux résultats par idMatière
            linkIdSubjectToLibelle(idEleve, getMaxByItem(matieresCompNotes, tableauDeConversionFuture.result()),
                    getMaxByItem(matieresCompNotesEleve, tableauDeConversionFuture.result()), matieresNotes, matieresNotesEleve,
                    mapMatieresStatClasseAndEleve, idMatieres, subjectF.result(), handler);
        });
    }
    public void getDataGraphDomaine(final String idEleve, JsonArray groupIds, final String idEtablissement ,
                                    final String idClasse,final Integer typeClasse, final String idPeriodeString,
                                    final Boolean isYear, final Handler<Either<String, JsonArray>> handler) {


        // 1. On récupère les Compétences-Notes de tous les domaines et de tous les élèves
        Future<JsonArray> compNotesFuture = Future.future();
        getCompetencesNotesReleve(idEtablissement, idClasse, groupIds, null,
                (idPeriodeString != null)? Long.parseLong(idPeriodeString) : null,
                null, typeClasse,true, isYear, event -> {
                    if (event.isLeft()) {
                        String message = "[DomaineDataForGraph] error while getCompetencesNotesReleve";
                        handler.handle(new Either.Left<>(message));
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

        //3. En parallèle, On va chercher la table de conversion des notes-compétences
        Future<JsonArray> tableauDeConversionFuture = Future.future();
        new DefaultCompetenceNoteService(COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE)
                .getConversionNoteCompetence(idEtablissement, idClasse,
                        tableauEvent -> formate(tableauDeConversionFuture, tableauEvent));

        // 4. On Lie les compétences-Notes à leur libellé
        CompositeFuture.all(compNotesFuture, domainesCycleFuture,tableauDeConversionFuture).setHandler(event -> {
            if (event.succeeded()) {
                handler.handle(new Either.Right<>(linkCompNoteToLibelle(domainesCycleFuture.result(),
                        compNotesFuture.result(), tableauDeConversionFuture.result(), idEleve)));
            } else {
                String message = event.cause().getMessage();
                handler.handle(new Either.Left<>(message));
                log.error(message);
            }
        });

    }

    private JsonArray linkCompNoteToLibelle(JsonArray domaines, JsonArray compNotes, JsonArray tableauConversion, String idEleve ) {

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
                    (data!=null)? data.getJsonArray("competencesNotesEleve") : new JsonArray();
            JsonArray competencesNotes =
                    (data!=null)?data.getJsonArray("competencesNotes") : new JsonArray();
            if (data!= null) {
                res.add(domaine.put("competencesNotes",   getMaxByItemDomaine(competencesNotes,tableauConversion))
                        .put("competencesNotesEleve", getMaxByItemDomaine(competencesNotesEleve,tableauConversion)));
            }
        }
        return res;
    }

    private JsonArray groupDataByMatiere(JsonArray datas, Map<String,JsonArray> mapDataClasse,
                                         Map<String,JsonArray> mapDataEleve, String idEleve, boolean checkFormative){
        JsonArray result = new JsonArray();
        for (int i=0; i< datas.size(); i++ ) {
            JsonObject data = datas.getJsonObject(i);
            if (!checkFormative || (data != null && data.getBoolean("formative") != null && !data.getBoolean("formative"))) {
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
    private Map<String,JsonArray> getMaxByItem(Map<String,JsonArray> mapData, JsonArray tableauConversion){
        Map<String,JsonArray> result = new HashMap<>();

        for (Map.Entry<String,JsonArray> entry: mapData.entrySet()) {
            String idEntry = entry.getKey();
            JsonArray currentEntry = entry.getValue();
            Map<String, JsonObject> maxComp = calculMaxCompNoteItem(currentEntry, tableauConversion,true);
            result.put(idEntry, new JsonArray());
            for (Map.Entry<String,JsonObject> max: maxComp.entrySet()) {
                result.get(idEntry).add(max.getValue());
            }


        }
        return result;
    }

    // Permet de Calculer le Max des Niveaux Atteints par Items regroupés par Domaine
    private JsonArray getMaxByItemDomaine(JsonArray compNotes,JsonArray tableauConversion){
        JsonArray result = new JsonArray();

        Map<String, JsonObject> maxCompNote = calculMaxCompNoteItem(compNotes, tableauConversion,true);
        for (Map.Entry<String,JsonObject> max: maxCompNote.entrySet()) {
            result.add(max.getValue());
        }

        return result;
    }

    private Map<String, JsonObject> calculMaxCompNoteItem (JsonArray compNotes, JsonArray tableauConversion, Boolean takeNivFinal) {
        Map<String, JsonObject> maxComp = new HashMap<>();
        Map<String, List<JsonObject>> groupByStudent = new HashMap<>();
        JsonArray clone = compNotes.copy();
        //On groupe les items de compétences par élève
        for (int i = 0; i < clone.size(); i++) {
            JsonObject compNote = clone.getJsonObject(i);
            if(!groupByStudent.containsKey(compNote.getString("id_eleve"))){
                groupByStudent.put(compNote.getString("id_eleve"), new ArrayList<>());
            }
            groupByStudent.get(compNote.getString("id_eleve")).add(compNote);
        }
        for (Map.Entry<String,List<JsonObject>> listCompOfStudent: groupByStudent.entrySet()) {
            List<JsonObject> listSameCompStudent = listCompOfStudent.getValue();
            Map<Long, List<JsonObject>> groupByComp = new HashMap<>();
            for (JsonObject compNote : listSameCompStudent) {
                if (!groupByComp.containsKey(compNote.getLong("id_competence"))) {
                    groupByComp.put(compNote.getLong("id_competence"), new ArrayList<>());
                }
                groupByComp.get(compNote.getLong("id_competence")).add(compNote);
            }
            for (Map.Entry<Long, List<JsonObject>> listComp : groupByComp.entrySet()) {
                List<JsonObject> listSameComp = listComp.getValue();
                Map<String, List<JsonObject>> groupByMat = new HashMap<>();
                //On groupe chaque notes d'item de compétences par matières
                for (JsonObject compNote : listSameComp) {
                    if (!groupByMat.containsKey(compNote.getString("id_matiere"))) {
                        groupByMat.put(compNote.getString("id_matiere"), new ArrayList<>());
                    }
                    groupByMat.get(compNote.getString("id_matiere")).add(compNote);
                }
                List<JsonObject> listSameCompMaxMat = new ArrayList<>();
                //On récupère les maxs de chaque matière
                for (Map.Entry<String, List<JsonObject>> listMat : groupByMat.entrySet()) {
                    List<JsonObject> listSameMatComp = listMat.getValue();
                    Long max = listSameMatComp.get(0).getLong("evaluation");
                    JsonObject JsonObjectToAdd = listSameMatComp.get(0);
                    for (JsonObject compNoteTemp : listSameMatComp) {
                        Long evaluation = compNoteTemp.getLong("evaluation");
                        Long niveauFinal = compNoteTemp.getLong("niveau_final");
                        Long niveauFinalAnnuel = compNoteTemp.getLong("niveau_final_annuel");
                        Long valueToTake;
                        if (takeNivFinal) {
                            if(niveauFinalAnnuel != null)
                                valueToTake = niveauFinalAnnuel;
                            else
                                valueToTake = (niveauFinal != null) ? niveauFinal : evaluation;
                        }else{
                            valueToTake = evaluation;
                        }
                        if (valueToTake != null && valueToTake > max) {
                            max = valueToTake;
                            JsonObjectToAdd = compNoteTemp;
                        }
                    }
                    listSameCompMaxMat.add(JsonObjectToAdd);
                }
                //Et on fait la moyenne des maxs de chaque matières dans l'item de compétences
                JsonObject compNote = listSameComp.get(0);
                Long idCompetence = compNote.getLong("id_competence");
                String idEleve = compNote.getString("id_eleve");

                idEleve = (idEleve != null) ? idEleve : "null";

                String idItem = (idCompetence != null) ? idCompetence.toString() : "null";
                idItem += idEleve;
                float sum = 0L;
                float nbrofMat = 0L;
                for (JsonObject compNoteTemp : listSameCompMaxMat) {
                    Long evaluation = compNoteTemp.getLong("evaluation");
                    Long niveauFinal = compNoteTemp.getLong("niveau_final");
                    Long niveauFinalAnnuel = compNoteTemp.getLong("niveau_final_annuel");
                    Long valueToTake;
                    if (takeNivFinal) {
                        if(niveauFinalAnnuel != null)
                            valueToTake = niveauFinalAnnuel;
                        else
                            valueToTake = (niveauFinal != null) ? niveauFinal : evaluation;
                    }else{
                        valueToTake = evaluation;
                    }
                    if (valueToTake != null) {
                        sum += valueToTake;
                        nbrofMat++;
                    }
                }
                float moyenneToSend;
                if (nbrofMat != 0)
                    moyenneToSend = sum / nbrofMat;
                else
                    moyenneToSend = 1f;
                int moyenneConverted = utilsService.getPositionnementValue(moyenneToSend+1, tableauConversion)-1;
                compNote.put("evaluation", moyenneConverted).put("niveau_final", moyenneConverted);
                maxComp.put(idItem, compNote);
            }
        }
        return maxComp;
    }

    private void linkIdSubjectToLibelle(String idEleve, Map<String, JsonArray> matieresCompNotes,
                                        Map<String, JsonArray> matieresCompNotesEleve,
                                        Map<String, JsonArray> matieresNotes,
                                        Map<String, JsonArray> matieresNotesEleve,
                                        Map<String, StatClass> mapMatieresStatClasseAndEleve,
                                        JsonArray idMatieres, JsonArray matieresEtab,
                                        Handler<Either<String, JsonArray>> handler) {


        JsonArray matieres = new JsonArray();
        for (int i = 0; i < matieresEtab.size(); i++) {
            JsonObject matiere = matieresEtab.getJsonObject(i);
            String idMatiere = matiere.getString("id");
            if (!idMatieres.contains(idMatiere)) {
                continue;
            }
            Double classAverage = null;
            Double classMin = null;
            Double classMax = null;
            Double averageStudent = null;
            StatClass statClasse = mapMatieresStatClasseAndEleve.get(matiere.getString("id"));
            if (statClasse != null) {
                classAverage = statClasse.getAverageClass();
                classMin = statClasse.getMinMaxClass(true);
                classMax = statClasse.getMinMaxClass(false);
                averageStudent = statClasse.getMoyenneEleve(idEleve);
            }
            matiere.put("competencesNotes", matieresCompNotes
                    .get(matiere.getString("id")))
                    .put("competencesNotesEleve", matieresCompNotesEleve
                            .get(matiere.getString("id")))
                    .put("notes", matieresNotes.get(matiere.getString("id")))
                    .put("notesEleve", matieresNotesEleve.get(matiere.getString("id")))
                    .put("studentAverage", averageStudent)
                    .put("classAverage", classAverage)
                    .put("classMin", classMin)
                    .put("classMax", classMax);
            matieres.add(matiere);
        }

        matieres.add(
                new JsonObject().put("name", "null")
                        .put("competencesNotes", matieresCompNotes.get("no_id_matiere"))
                        .put("competencesNotesEleve", matieresCompNotesEleve.get("no_id_matiere"))
                        .put("notes", matieresNotes.get("no_id_matiere"))
                        .put("notesEleve", matieresNotesEleve.get("no_id_matiere"))
        );

        handler.handle(new Either.Right<>(matieres));
    }

    private JsonObject cloneSousMatiere( Map<String, Object> stringObjectMap){
        fr.wseduc.webutils.collections.JsonObject res = new fr.wseduc.webutils.collections.JsonObject();
        for(Map.Entry<String, Object> entry : stringObjectMap.entrySet()){
            res.put(entry.getKey(), entry.getValue());
        }
        return res;
    }

    private void addSousMatieres(Map<String, JsonObject> elevesMapObject, JsonArray sousMatieres,
                                 JsonObject result,Long idPeriode){
        for(Map.Entry<String, JsonObject> elMap : elevesMapObject.entrySet()){
            JsonObject eleve = elMap.getValue();
            eleve.put(SOUS_MATIERES, new JsonObject().put(MOYENNES, new JsonArray())
                    .put(POSITIONNEMENTS_AUTO, new JsonArray()));
            if (isNotNull(sousMatieres)) {
                for (int i = 0; i < sousMatieres.size(); i++) {

                    JsonObject sousMatiere = cloneSousMatiere(sousMatieres.getJsonObject(i).getMap());
                    Long idSousMatiere = sousMatiere.getLong(ID_SOUS_MATIERE);
                    String idMatiere = sousMatiere.getString(ID_MATIERE);

                    // Get moyenne sous matiere
                    Object moyenne = eleve.getJsonObject("_" + MOYENNE);
                    if (isNotNull(moyenne)) {
                        moyenne = ((JsonObject) moyenne).getJsonObject(idMatiere);
                        if (isNotNull(moyenne)) {
                            moyenne = ((JsonObject) moyenne).getJsonObject(idSousMatiere.toString());
                            if (isNotNull(moyenne)) {
                                moyenne = ((JsonObject) moyenne).getFloat(MOYENNE);
                            }
                        }
                    }
                    // Get positionnement sous matiere
                    Object positionnement = eleve.getJsonObject("_" + POSITIONNEMENTS_AUTO);
                    if (isNotNull(positionnement) && isNotNull(idPeriode)) {
                        positionnement = ((JsonObject) positionnement).getJsonObject(idPeriode.toString());
                        if (isNotNull(positionnement)) {
                            positionnement = ((JsonObject) positionnement).getJsonObject(idSousMatiere.toString());
                            if (isNotNull(positionnement)) {
                                positionnement = ((JsonObject) positionnement).getValue(POSITIONNEMENT);
                            }
                        }
                    }

                    String moyLibelle = getLibelle("average.min") + " " + sousMatiere.getString(LIBELLE);
                    String posLibelle =  getLibelle("evaluations.releve.positionnement.min")  + " " +
                            sousMatiere.getString(LIBELLE);
                    sousMatiere.put(MOYENNE, isNull(moyenne) ? " " : moyenne)
                            .put(POSITIONNEMENT, isNull(positionnement)? 0 : positionnement )
                            .put("moyLibelle", moyLibelle).put("posLibelle", posLibelle);

                    eleve.getJsonObject(SOUS_MATIERES).getJsonArray(MOYENNES).add(sousMatiere);
                    eleve.getJsonObject(SOUS_MATIERES).getJsonArray(POSITIONNEMENTS_AUTO).add(sousMatiere);
                }
            }
        }
        result.put("moyenneClasseSousMat", new JsonArray());
        for (int i = 0; i < sousMatieres.size(); i++) {
            JsonObject sousMatiere = sousMatieres.getJsonObject(i);
            String moyLibelle = getLibelle("average.class") + " " + sousMatiere.getString(LIBELLE);
            Long idSousMatiere = sousMatiere.getLong(ID_SOUS_MATIERE);
            sousMatiere.put("_"+ LIBELLE, moyLibelle);
            Object moy = result.getJsonObject("_moyenne_classe");
            if(isNotNull(moy) && isNotNull(idSousMatiere)){
                moy = ((JsonObject) moy).getJsonObject(idSousMatiere.toString());
                if(isNotNull(moy)){
                    moy = ((JsonObject) moy).getValue(MOYENNE);
                }
            }
            moy = isNull(moy)? "" : moy;
            sousMatiere.put("_" + MOYENNE, moy);
            result.getJsonArray("moyenneClasseSousMat").add(sousMatiere);
        }
    }

    private void putParamSousMatiere(JsonArray sousMatieres, JsonObject params) {
        if (isNotNull(sousMatieres)) {
            for (int i = 0; i < sousMatieres.size(); i++) {

                JsonObject sousMatiere = sousMatieres.getJsonObject(i);
                Long idSousMatiere = sousMatiere.getLong(ID_SOUS_MATIERE);
                Object print = params.getJsonObject(SOUS_MATIERES);
                Object printPosi = params.getJsonObject(SOUS_MATIERES);
                sousMatiere.put(COLSPAN, params.getValue(COLSPAN));
                if(isNotNull(print)){
                    print = ((JsonObject)print).getJsonObject(MOYENNES);
                    printPosi = ((JsonObject)printPosi).getJsonObject(POSITIONNEMENTS_AUTO);
                    if(isNotNull(print)) {
                        print = ((JsonObject) print).getValue(idSousMatiere.toString());
                        printPosi = ((JsonObject)printPosi).getValue(idSousMatiere.toString());
                    }
                }
                sousMatiere.put("print", (isNull(print))? false : print)
                        .put("printPosi", (isNull(printPosi))? false : printPosi);
            }
        }
    }

    public void getDetailsReleve(final String idEleve, final String idClasse, final String idMatiere,
                                 final String idEtablissement, final HttpServerRequest request){

        JsonObject result = new JsonObject();
        final JsonArray idEleves = new JsonArray();
        List<Future> detailsFuture = new ArrayList<>();

        // Récupération des compétences Note de l'élève
        Future<JsonArray> listCompNotesF = Future.future();
        getCompetencesNotesReleve( idEtablissement, idClasse,null, idMatiere, null,
                idEleve,null, true, true, event -> formate(listCompNotesF, event));
        detailsFuture.add(listCompNotesF);

        // Récupération des notes de l'élève
        Future<JsonArray> listNoteF = Future.future();
        getNoteElevePeriode(null, idEtablissement, new JsonArray().add(idClasse), idMatiere, null,
                event -> formate(listNoteF, event));
        detailsFuture.add(listNoteF);

        // Récupération des moyennes finales de tous les élèves de la classe
        Future<JsonArray> moyFinalesElevesF = Future.future();
        getColonneReleve(null, null, idMatiere,
                new JsonArray().add(idClasse), "moyenne", event -> formate(moyFinalesElevesF, event));
        detailsFuture.add(moyFinalesElevesF);

        // Récupération des appréciations matières, des moyennesFinales et  positionnements finaux
        // de l'élève sur toutes les périodes de la classe
        Future<JsonArray> appreciationMatierePeriode = Future.future();
        getColonneReleve(new JsonArray().add(idEleve), null, idMatiere, new JsonArray().add(idClasse),
                "appreciation_matiere_periode", event -> formate(appreciationMatierePeriode, event));
        detailsFuture.add(appreciationMatierePeriode);
        Future<JsonArray> moyenneFinalesF = Future.future();
        getColonneReleve( new JsonArray().add(idEleve), null, idMatiere, new JsonArray().add(idClasse),
                "moyenne", event -> formate(moyenneFinalesF, event));
        detailsFuture.add(moyenneFinalesF);
        Future<JsonArray> positionnementF = Future.future();
        getColonneReleve( new JsonArray().add(idEleve), null, idMatiere, new JsonArray().add(idClasse),
                "positionnement", event -> formate(positionnementF, event));
        detailsFuture.add(positionnementF);

        // Récupération du tableau de conversion
        // On récupère le tableau de conversion des compétences notes pour Lire le positionnement
        Future<JsonArray> tableauDeConversionFuture = Future.future();
        new DefaultCompetenceNoteService(COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE)
                .getConversionNoteCompetence(idEtablissement, idClasse,
                        tableauEvent -> formate(tableauDeConversionFuture, tableauEvent));
        detailsFuture.add(tableauDeConversionFuture);

        CompositeFuture.all(detailsFuture).setHandler(event -> {
            if(event.failed()){
                String error = event.cause().getMessage();
                log.error(error);
                Renders.renderError(request, new JsonObject().put(ERROR, error));
                return;
            }

            result.put("appreciations", appreciationMatierePeriode.result());
            result.put("moyennes_finales", moyenneFinalesF.result());
            result.put("positionnements", positionnementF.result());

            // Calcul des positionements
            JsonArray listCompNotes = listCompNotesF.result();
            calculPositionnementAutoByEleveByMatiere(listCompNotes, result,false, tableauDeConversionFuture.result());

            // Calcul des moyennes par période pour la classe
            JsonArray moyFinalesEleves = moyFinalesElevesF.result();
            HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse =
                    calculMoyennesEleveByPeriode(listNoteF.result(), result, idEleve, idEleves);
            calculAndSetMoyenneClasseByPeriode(moyFinalesEleves, notesByDevoirByPeriodeClasse, result);
            Renders.renderJson(request, result);

        });
    }
}
