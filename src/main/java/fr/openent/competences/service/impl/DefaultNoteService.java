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
import fr.openent.competences.constants.Field;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.model.*;
import fr.openent.competences.service.*;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.*;
import static fr.openent.competences.constants.Field.*;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.ERROR;
import static fr.openent.competences.service.impl.DefaultUtilsService.setServices;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.Renders.badRequest;
import static org.entcore.common.sql.SqlResult.validResultHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultNoteService extends SqlCrudService implements NoteService {

    public static final int NB_FUTURES = 6;

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
    public final String SYNTHESE_BILAN_PERIODIQUE = "synthese_bilan_periodique";
    public final String AVIS_CONSEIL_DE_CLASSE = "avis_conseil_de_classe";
    public final String AVIS_CONSEIL_ORIENTATION = "avis_conseil_orientation";
    public final String COMPETENCES_NOTES_KEY = "competencesNotes";
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
    private CompetenceNoteService competenceNoteService;
    private SubTopicService subTopicService;
    private StructureOptionsService structureOptionsService;
    protected static final Logger log = LoggerFactory.getLogger(DefaultNoteService.class);

    public DefaultNoteService(String schema, String table) {
        super(schema, table);
    }

    public DefaultNoteService(String schema, String table, EventBus eb) {
        super(schema, table);
        this.eb = eb;
        utilsService = new DefaultUtilsService(eb);
        annotationService = new DefaultAnnotationService(COMPETENCES_SCHEMA, Field.REL_ANNOTATIONS_DEVOIRS_TABLE);
        competenceNoteService = new DefaultCompetenceNoteService(COMPETENCES_SCHEMA, Field.COMPETENCES_NOTES_TABLE);
        subTopicService = new DefaultSubTopicService(Competences.COMPETENCES_SCHEMA, Field.SUBTOPIC_TABLE);
        structureOptionsService = new DefaultStructureOptions();
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

        // Récupération des appréciations avec/ou sans notes avec/ou sans annotation
        // Récupération des annotations sans appréciation
        // Récupération des notes sans appréciation sans annotation
        // Récupération des appréciations avec/ou sans notes avec/ou sans annotation
        // Récupération des annotations sans appréciation
        // Récupération des appréciations avec/ou sans notes avec/ou sans annotation
        // Récupération des annotations sans appréciation
        query.append("SELECT res.*,devoirs.date, devoirs.coefficient, devoirs.ramener_sur  ")
                .append(" FROM ( ").append("SELECT ").append(appreciation_id_devoir)
                .append(" as id_devoir, ").append(appreciation_id_eleve).append(", ").append(note_id).append(" as id, ")
                .append(note_valeur).append(" as valeur, ").append(appreciation_id).append(" as id_appreciation, ")
                .append(appreciation_valeur).append(" as appreciation , ").append(annotations_id_annotation)
                .append(" FROM ").append(table_appreciation).append(" LEFT JOIN ")
                .append(table_note).append(" ON ( ").append(appreciation_id_devoir).append(" = ").append(note_id_devoir)
                .append(" AND ").append(appreciation_id_eleve).append(" = ").append(note_id_eleve).append(" )")
                .append(" LEFT JOIN ").append(table_annotations).append(" ON ( ").append(appreciation_id_devoir)
                .append(" = ").append(annotations_id_devoir).append(" AND ").append(appreciation_id_eleve)
                .append(" = ").append(annotations_id_eleve).append(")").append(" WHERE ").append(appreciation_id_devoir).append(" = ? ")

                .append(" UNION ").append("SELECT ").append(annotations_id_devoir).append(" AS id_devoir ,")
                .append(annotations_id_eleve).append(" ,NULL ,NULL ,NULL ,NULL ,").append(annotations_id_annotation)
                .append(" FROM ").append(table_annotations).append("  WHERE ").append(annotations_id_devoir).append(" = ? ")
                .append("   AND NOT EXISTS ( ")
                .append("    SELECT 1 ").append("    FROM ").append(table_appreciation).
                append("    WHERE ").append(annotations_id_devoir).append(" = ").append(appreciation_id_devoir)
                .append("     AND ").append(annotations_id_eleve).append(" = ").append(appreciation_id_eleve).append(")")

                .append(" UNION ").append(" SELECT ").append(note_id_devoir).append(" as id_devoir, ")
                .append(note_id_eleve).append(", ").append(note_id).append(" as id, ").append(note_valeur)
                .append(" as valeur, null, null, null FROM ").append(table_note).append(" WHERE ")
                .append(note_id_devoir).append(" = ? AND NOT EXISTS ( ").append(" SELECT 1 FROM ")
                .append(table_appreciation).append(" WHERE ").append(note_id_devoir).append(" = ")
                .append(appreciation_id_devoir).append(" AND ").append(note_id_eleve).append(" = ")
                .append(appreciation_id_eleve).append(" ) ").append("ORDER BY 2")
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

        query.append("SELECT notes.id_devoir, notes.id_eleve, notes.valeur, devoirs.coefficient, devoirs.diviseur, " +
                "devoirs.ramener_sur, devoirs.owner, devoirs.id_matiere, devoirs.id_sousmatiere, grp.id_groupe " +
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

        query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, devoirs.diviseur, devoirs.owner, ")
                .append(" devoirs.ramener_sur, devoirs.is_evaluated, devoirs.id_periode, devoirs.id_sousmatiere,")
                .append(" devoirs.id_matiere, devoirs.id_etablissement, rel_devoirs_groupes.id_groupe, notes.valeur, ")
                .append(" notes.id, notes.id_eleve, services.coefficient as coef , type.formative")
                .append(" FROM ").append(this.schema).append(Field.DEVOIRS_TABLE)
                .append(" INNER JOIN ").append(this.schema).append(Field.TYPE)
                .append(" ON ").append(Field.TYPE).append(".id = ").append(DEVOIRS_TABLE).append(".id_type")
                .append(" LEFT JOIN ")
                .append(this.schema).append(Field.NOTES_TABLE)
                .append(" ON devoirs.id = notes.id_devoir ")
                .append((null != userId) ? " AND notes.id_eleve = ? " : "").append(" INNER JOIN ")
                .append(this.schema).append(REL_DEVOIRS_GROUPES_TABLE)
                .append(" ON rel_devoirs_groupes.id_devoir = devoirs.id AND rel_devoirs_groupes.id_groupe IN ")
                .append(Sql.listPrepared(idsClass.getList())).append(" LEFT JOIN ")
                .append(Competences.VSCO_SCHEMA).append(".services ")
                .append(" ON (rel_devoirs_groupes.id_groupe = services.id_groupe ")
                .append(" AND devoirs.owner = services.id_enseignant AND devoirs.id_matiere = services.id_matiere) ")
                .append(" WHERE devoirs.id_etablissement = ? ")
                .append(" AND devoirs.id_matiere = ? ")
                .append((null != periodeId) ? " AND devoirs.id_periode = ? " : "")
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

        Sql.getInstance().prepared(query.toString(), values,Competences.DELIVERY_OPTIONS, validResultHandler(handler));
    }

    public Future<JsonArray> getNoteStudentPeriod(String userId, String structureId, JsonArray classIds, String subjectId,
                                    Long periodId) {
        Promise<JsonArray> noteStudentPeriodPromise = Promise.promise();
        getNoteElevePeriode(userId, structureId, classIds, subjectId, periodId,
                FutureHelper.handlerJsonArray(noteStudentPeriodPromise,
                        String.format("[Competences@%s:: getNoteStudentPeriod] : Error during sql resquest : %s.",
                                this.getClass().getSimpleName())));
        return noteStudentPeriodPromise.future();
    }

    @Override
    public void getNotesReleve(String etablissementId, String classeId, String matiereId, Long periodeId,
                               Integer typeClasse, Boolean withMoyenneFinale, JsonArray idsGroup,
                               Handler<Either<String, JsonArray>> handler) {

        new DefaultUtilsService(this.eb).studentIdAvailableForPeriode(classeId, periodeId, typeClasse, event -> {
            if (event.isRight()) {
                JsonArray queryResult = event.right().getValue();
                getNotesReleveEleves(queryResult, etablissementId, classeId, periodeId, withMoyenneFinale,
                        idsGroup, matiereId != null ? new JsonArray().add(matiereId) : null, handler);
            } else {
                handler.handle(new Either.Left<>("Error While getting Available student "));
            }
        });

    }

    public Future<JsonArray> getNotesReleve(String structureId, String classId, String subjectId, Long periodId,
          Integer typeClass, Boolean withFinaleAverage, JsonArray groupIds) {
        Promise<JsonArray> promiseStudentAvailable = Promise.promise();
        getNotesReleve(structureId,classId, subjectId, periodId, typeClass, withFinaleAverage, groupIds,
                FutureHelper.handlerJsonArray(promiseStudentAvailable,
                        String.format("[Competences@%s::getNotesReleve] : error sql request ",
                                this.getClass().getSimpleName())));

        return promiseStudentAvailable.future();

    }

    private void getNotesReleveEleves(JsonArray idsEleve, String etablissementId, String classeId,
                                      Long periodeId, Boolean withMoyenneFinale, JsonArray idsGroup,
                                      JsonArray idsMatiere, Handler<Either<String, JsonArray>> handler) {
        List<String> idEleves = idsEleve.getList();

        List<String> idMatieres = null;
        if(idsMatiere != null) {
            idMatieres = idsMatiere.getList();
        }

        List<String> idGroupes = null;
        if(idsGroup != null) {
            idGroupes = idsGroup.getList();
        }

        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, devoirs.owner,")
                .append(" devoirs.diviseur, devoirs.ramener_sur,notes.valeur, notes.id, devoirs.id_periode , notes.id_eleve,")
                .append(" devoirs.is_evaluated, null as annotation, devoirs.id_matiere, devoirs.id_sousmatiere, ")
                .append(" rel_devoirs_groupes.id_groupe as id_groupe, type.formative")
                .append(" FROM ").append(this.schema).append(Field.DEVOIRS_TABLE)
                .append(" INNER JOIN ").append(this.schema).append(Field.TYPE)
                .append(" ON ").append(Field.TYPE).append(".id = ").append(DEVOIRS_TABLE).append(".id_type")
                .append(" LEFT JOIN ").append(this.schema).append(Field.NOTES_TABLE)
                .append(" ON ( ").append(Field.DEVOIRS_TABLE).append(".id = ").append(Field.NOTES_TABLE).append(".id_devoir ")
                .append((null != idGroupes) ? ")" : ("AND " + Field.NOTES_TABLE + ".id_eleve IN " + Sql.listPrepared(idEleves) + ")"))
                .append(" INNER JOIN ").append(this.schema).append(REL_DEVOIRS_GROUPES_TABLE + " ON ")
                .append("(rel_devoirs_groupes.id_devoir = devoirs.id AND ")
                .append((null != idGroupes) ? ("rel_devoirs_groupes.id_groupe IN " +
                        Sql.listPrepared(idGroupes) + ")") : "rel_devoirs_groupes.id_groupe = ?)")
                .append(" WHERE devoirs.id_etablissement = ? ")
                .append((null != idMatieres) ? ("AND devoirs.id_matiere IN " + Sql.listPrepared(idMatieres)) : " ")
                .append((null != periodeId) ? "AND devoirs.id_periode = ? " : "");

        setParamGetNotesReleve(idGroupes, idEleves, classeId, idMatieres, etablissementId, periodeId, values);

        query.append(" UNION ")
                .append("SELECT devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, devoirs.owner,")
                .append(" devoirs.diviseur, devoirs.ramener_sur,null as valeur, null as id, devoirs.id_periode, ")
                .append("rel_annotations_devoirs.id_eleve, devoirs.is_evaluated,")
                .append(" rel_annotations_devoirs.id_annotation as annotation, devoirs.id_matiere, devoirs.id_sousmatiere,")
                .append(" rel_devoirs_groupes.id_groupe as id_groupe, type.formative")
                .append(" FROM ").append(this.schema).append(DEVOIRS_TABLE)
                .append(" INNER JOIN ").append(this.schema).append(Field.TYPE)
                .append(" ON ").append(Field.TYPE).append(".id = ").append(DEVOIRS_TABLE).append(".id_type")
                .append(" LEFT JOIN ").append(this.schema).append(Field.REL_ANNOTATIONS_DEVOIRS_TABLE)
                .append(" ON (devoirs.id = rel_annotations_devoirs.id_devoir ")
                .append((null != idGroupes) ? ")" : (" AND rel_annotations_devoirs.id_eleve IN " + Sql.listPrepared(idEleves) + ")"))
                .append(" INNER JOIN ").append(this.schema).append(REL_DEVOIRS_GROUPES_TABLE)
                .append(" ON (rel_devoirs_groupes.id_devoir = devoirs.id ")
                .append((null != idGroupes) ? ("AND rel_devoirs_groupes.id_groupe IN " +
                        Sql.listPrepared(idGroupes) + ")") : "AND rel_devoirs_groupes.id_groupe = ?) ")
                .append(" WHERE devoirs.id_etablissement = ? ")
                .append((null != idMatieres) ? ("AND devoirs.id_matiere IN " + Sql.listPrepared(idMatieres)) : " ")
                .append((null != periodeId) ? "AND devoirs.id_periode = ? " : "")
                .append("ORDER BY date ASC ");

        setParamGetNotesReleve(idGroupes, idEleves, classeId, idMatieres, etablissementId, periodeId, values);

        if (withMoyenneFinale) {
            query = new StringBuilder().append("SELECT * FROM ( ").append(query).append(") AS devoirs_notes_annotation ")
                    .append("FULL JOIN ( SELECT moyenne_finale.id_matiere AS id_matiere_moyf, ")
                    .append("moyenne_finale.id_eleve AS id_eleve_moyenne_finale, COALESCE(moyenne_finale.moyenne, -100) AS moyenne, ")
                    .append("moyenne_finale.id_periode AS id_periode_moyenne_finale ")
                    .append("FROM notes.moyenne_finale WHERE ")
                    .append((null != idGroupes) ? ("moyenne_finale.id_classe IN " + Sql.listPrepared(idGroupes)) :
                            (" moyenne_finale.id_eleve IN " + Sql.listPrepared(idEleves) + " AND moyenne_finale.id_classe = ? "))
                    .append((null != idMatieres) ? ("AND devoirs.id_matiere IN " + Sql.listPrepared(idMatieres)) : " ")
                    .append((null != periodeId) ? "AND moyenne_finale.id_periode = ? " : "")
                    .append(") AS moyf ON ( moyf.id_eleve_moyenne_finale = devoirs_notes_annotation.id_eleve ")
                    .append(" AND moyf.id_matiere_moyf = devoirs_notes_annotation.id_matiere ")
                    .append(" AND moyf.id_periode_moyenne_finale = devoirs_notes_annotation.id_periode )");

            setParamGetNotesReleve(idGroupes, idEleves, classeId, idMatieres,null, periodeId, values);
        }

        Sql.getInstance().prepared(query.toString(), values, Competences.DELIVERY_OPTIONS, validResultHandler(handler));
    }

    private Future<JsonArray> getNotesReleveStudents (JsonArray studentIds, String structureId, String classId,
                                                    Long periodId, Boolean withFinalAverage, JsonArray idsGroup, JsonArray subjectIds) {

        Promise<JsonArray> notesReleveElevesPromise = Promise.promise();
        getNotesReleveEleves(studentIds, structureId, classId,periodId, withFinalAverage, idsGroup, subjectIds,
                FutureHelper.handlerJsonArray(notesReleveElevesPromise,
                        String.format("[Competences@%s::getNotesReleveEleves] : ",
                                this.getClass().getSimpleName())));

        return notesReleveElevesPromise.future();
    }

    private void setParamGetNotesReleve(List<String> idGroupes, List<String> idEleves, String classeId,
                                        List<String> matiereIds, String etablissementId, Long periodeId,
                                        JsonArray values){
        if(null == idGroupes){
            for (String eleve : idEleves) {
                values.add(eleve);
            }
            values.add(classeId);
        } else {
            for (String groupe : idGroupes) {
                values.add(groupe);
            }
        }

        if(etablissementId != null) {
            values.add(etablissementId);
        }

        if(null != matiereIds) {
            for (String matiere : matiereIds) {
                values.add(matiere);
            }
        }

        if(periodeId != null) {
            values.add(periodeId);
        }
    }

    public void getCompetencesNotesReleveEleves(JsonArray studentIds, String structureIds, String subjectId,
                                                JsonArray subjectIds,
                                                Long periodId, String studentId, Boolean withDomainInfo,
                                                Boolean isYear, Handler<Either<String, JsonArray>> handler) {
        List<String> idEleves = new ArrayList<String>();

        if (studentIds != null) {
            for (int i = 0; i < studentIds.size(); i++) {
                idEleves.add(studentIds.getString(i));
            }
        }

        List<String> idMatieres = new ArrayList<String>();

        if (subjectIds != null) {
            for (int i = 0; i < subjectIds.size(); i++) {
                idMatieres.add(subjectIds.getString(i));
            }
        } else{
            idMatieres = null;
        }
        runGetCompetencesNotesReleve(structureIds, subjectId, idMatieres, periodId,
                studentId, idEleves, withDomainInfo, isYear, handler);
    }

    private Future<JsonArray> getCompetencesNotesReleveStudents(JsonArray ids, String structureId, String subjectId,
                                                 JsonArray subjectIds,
                                                 Long periodId,  String studentId, Boolean withDomaineInfo,
                                                 Boolean isYear) {
        Promise<JsonArray> promiseCompNotesReleveEleves = Promise.promise();

        getCompetencesNotesReleveEleves(ids, structureId, subjectId, subjectIds,periodId,
                studentId, withDomaineInfo, isYear,
                FutureHelper.handlerJsonArray(promiseCompNotesReleveEleves,
                        String.format("[Competences%s::getCompetencesNotesReleveStudents]: error sql request ",
                                this.getClass().getSimpleName())));
        return promiseCompNotesReleveEleves.future();
    }

    public void getCompetencesNotesReleve(String etablissementId, String classeId, JsonArray groupIds, String matiereId,
                                          Long periodeId, String eleveId, Integer typeClasse, Boolean withDomaineInfo,
                                          Boolean isYear, Handler<Either<String, JsonArray>> handler) {
        if(typeClasse == null){
            runGetCompetencesNotesReleve(etablissementId, matiereId, null,
                    periodeId, eleveId, new ArrayList<>(), withDomaineInfo, isYear, handler);
        } else {
            new DefaultUtilsService(this.eb).studentIdAvailableForPeriode(classeId, periodeId, typeClasse, event -> {
                if (event.isRight()) {
                    JsonArray ids = event.right().getValue();
                    getCompetencesNotesReleveEleves(ids, etablissementId, matiereId, null, periodeId,
                            eleveId, withDomaineInfo, isYear, handler);
                } else {
                    handler.handle(new Either.Left<>("Error While getting Available student "));
                }
            });
        }
    }

    private void runGetCompetencesNotesReleve(String etablissementId,
                                              String matiereId, List<String> matiereIds, Long periodeId,
                                              String eleveId, List<String> idEleves, Boolean withDomaineInfo,
                                              Boolean isYear, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT ")
                .append( (null != eleveId) ? "DISTINCT": "")
                .append(" devoirs.id as id_devoir, devoirs.date, devoirs.coefficient, ")
                .append(" devoirs.diviseur, devoirs.ramener_sur, competences_notes.evaluation ,")
                .append(" competences_notes.id_competence , devoirs.id_matiere, devoirs.id_sousmatiere, ")
                .append( (null != eleveId) ? "rel_devoirs_groupes.id_groupe," : " competences_notes.id, ")
                .append( (withDomaineInfo) ? "compDom.id_domaine, " : "")
                .append(" devoirs.id_periode, competences_notes.id_eleve, devoirs.is_evaluated, ")
                .append(" null as annotation, ")
                .append(" competence_niveau_final.niveau_final AS niveau_final, type.formative ")
                .append( (!isYear) ? "" : ", competence_niveau_final_annuel.niveau_final AS niveau_final_annuel ")

                .append(" FROM ").append(COMPETENCES_SCHEMA).append(".devoirs ")
                .append(" INNER JOIN ").append(COMPETENCES_SCHEMA).append(".type ON (devoirs.id_type = type.id) ");

        if (null != eleveId) {
            query.append("INNER JOIN notes.rel_devoirs_groupes ON devoirs.id = rel_devoirs_groupes.id_devoir")
                    .append(" LEFT JOIN (SELECT id, id_devoir, id_competence, max(evaluation) ")
                    .append(" as evaluation, id_eleve " )
                    .append(" FROM " ).append(COMPETENCES_SCHEMA).append( ".competences_notes ")
                    .append(" WHERE id_eleve = ? ")
                    .append(" GROUP BY (id, id_devoir, id_eleve) ) AS competences_notes  ")
                    .append(" ON devoirs.id ")
                    .append(" = competences_notes.id_devoir " );
            values.add(eleveId);
        } else {
            query.append(" LEFT JOIN ").append(COMPETENCES_SCHEMA).append(".competences_notes " +
                    "ON (devoirs.id  = competences_notes.id_devoir " +
                    "AND competences_notes.id_eleve IN " + Sql.listPrepared(idEleves)+ ")" );
            for (String idEleve : idEleves) {
                values.add(idEleve);
            }
        }

        if (withDomaineInfo) {
            query.append(" INNER JOIN ").append(COMPETENCES_SCHEMA).append(".rel_competences_domaines " + " AS compDom")
                    .append(" ON competences_notes.id_competence = compDom.id_competence ");
        }
        query.append(" LEFT JOIN ").append(COMPETENCES_SCHEMA).append(".competence_niveau_final ON " +
                "( competence_niveau_final.id_periode = devoirs.id_periode " +
                "AND competence_niveau_final.id_eleve = competences_notes.id_eleve " +
                "AND competence_niveau_final.id_competence = competences_notes.id_competence " +
                "AND competence_niveau_final.id_matiere = devoirs.id_matiere )");

        if(isYear) {
            query.append(" LEFT JOIN ").append(COMPETENCES_SCHEMA).append(".competence_niveau_final_annuel ON " +
                    "( competence_niveau_final_annuel.id_eleve = competences_notes.id_eleve " +
                    "AND competence_niveau_final_annuel.id_competence = competences_notes.id_competence " +
                    "AND competence_niveau_final_annuel.id_matiere = devoirs.id_matiere )");
        }

        query.append(" WHERE devoirs.id_etablissement = ? ")
                .append((matiereIds != null || matiereId != null) ? " AND devoirs.id_matiere IN "
                        + ((matiereIds != null) ? Sql.listPrepared(matiereIds) : "(?)") + " " : " ");

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

        query.append("AND devoirs.eval_lib_historise = false ");

        Sql.getInstance().prepared(query.toString(), values,Competences.DELIVERY_OPTIONS, validResultHandler(handler));
    }

    @Override
    public void deleteColonneReleve(String idEleve, Long idPeriode, String idMatiere, String idClasse,
                                    String colonne,   Handler<Either<String, JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder()
                .append("DELETE FROM " + COMPETENCES_SCHEMA + "." + colonne)
                .append("moyenne".equals(colonne) ? "_finale": "")
                .append(" WHERE id_periode = ? AND id_eleve = ? AND id_classe = ? AND id_matiere = ? ");

        values.add(idPeriode).add(idEleve).add(idClasse).add(idMatiere);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }


    @Override
    public void getColonneReleve(JsonArray idEleves, Long idPeriode, String idMatiere, JsonArray idsClasse,
                                 String colonne, Boolean withPreviousAppreciations, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        // le positionnement est enregistré pour un élève indépendament de sa classe
        // ou de ses groupes (positionnement global)
        // on le recupere donc sans filtre sur la classe
        if (colonne.equals(Field.POSITIONNEMENT)) {
            query.append("SELECT id_periode, id_eleve, " + Field.POSITIONNEMENT + ", id_matiere ");
            query.append(" FROM " + COMPETENCES_SCHEMA + "." + Field.POSITIONNEMENT);
        } else if(colonne.equals(APPRECIATION_MATIERE_PERIODE)){
            query.append("SELECT id_periode, id_eleve, " + APPRECIATION_MATIERE_PERIODE + ", id_classe, id_matiere, appreciation_matiere_periode.id AS id_appreciation_matiere_periode ");
            query.append(" FROM " + COMPETENCES_SCHEMA + "." + APPRECIATION_MATIERE_PERIODE);
        } else if(colonne.equals(Field.MOYENNE)){
            query.append("SELECT id_periode, id_eleve, " + Field.MOYENNE + ", id_classe, id_matiere ");
            query.append(" FROM " + COMPETENCES_SCHEMA + "." + Field.MOYENNE_FINALE_TABLE);
        } else{
            String textError = "Error when trying to get data, selected column is not supported.";
            log.error(textError);
            handler.handle(new Either.Left<>(textError));
        }

        if(colonne.equals("appreciation_matiere_periode")){
            query.append(" LEFT JOIN notes.rel_appreciations_users_neo AS ao ON appreciation_matiere_periode.id = ao.appreciation_matiere_periode_id ");
        }

        query.append(" WHERE ");

        if (null != idMatiere) {
            query.append("id_matiere = ? AND");
            values.add(idMatiere);
        }
        if (!colonne.equals(Field.POSITIONNEMENT) && idsClasse != null) {
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
            query.append(" id_periode ")
                    .append((Boolean.TRUE.equals(withPreviousAppreciations)) ?  "<= ? " : "= ? ");
            values.add(idPeriode);
        }
        if(query.toString().substring(query.length() - 3, query.length()).equals("AND")){
            query.delete(query.length() - 3, query.length());
        }
        Sql.getInstance().prepared(query.toString(), values,
                new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L),
                validResultHandler(handler));
    }

    public Future<JsonArray> getColumnReleve(JsonArray studentsIds, Long periodId, String subjectId, JsonArray classesIds,
                                 String column, Boolean withPreviousAppreciation) {

        Promise<JsonArray> columnRelevePromise = Promise.promise();
        getColonneReleve(studentsIds,periodId, subjectId, classesIds, column, withPreviousAppreciation,
                FutureHelper.handlerJsonArray(columnRelevePromise,
                        String.format("[Competences%s::getColumnReleve] : error sql request ",
                                this.getClass().getSimpleName())));
        return columnRelevePromise.future();
    }

    public void getColonneReleveTotale(JsonArray idEleves, Long idPeriode, JsonArray idsMatiere, JsonArray idsClasse,
                                       String idStructure, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT moy.id_eleve, moy.id_periode, null as avis_conseil_orientation, ")
                .append("null as avis_conseil_de_classe, null as synthese_bilan_periodique, null as positionnement, ")
                .append("moy.id_matiere, COALESCE(moy.moyenne, -100) AS moyenne, moy.id_classe ")
                .append("FROM ").append(COMPETENCES_SCHEMA).append(".moyenne_finale AS moy WHERE ");

        if (null != idEleves) {
            query.append("moy.id_eleve IN ").append(Sql.listPrepared(idEleves.getList().toArray()));
            for (int i = 0; i < idEleves.size(); i++) {
                values.add(idEleves.getString(i));
            }
        }

        if (null != idPeriode) {
            query.append(" AND moy.id_periode = ? ");
            values.add(idPeriode);
        }

        if (null != idsMatiere) {
            query.append(" AND moy.id_matiere IN ").append(Sql.listPrepared(idsMatiere.getList().toArray()));
            for (int i = 0; i < idsMatiere.size(); i++) {
                values.add(idsMatiere.getString(i));
            }
        }

        if (null != idsClasse) {
            query.append(" AND moy.id_classe IN ").append(Sql.listPrepared(idsClasse.getList()));
            for (Object idClasse : idsClasse.getList()) {
                values.add(idClasse);
            }
        }

        query.append(" UNION ")
                .append("SELECT pos.id_eleve, pos.id_periode, null as avis_conseil_orientation, null as avis_conseil_de_classe, ")
                .append("null as synthese_bilan_periodique, pos.positionnement, pos.id_matiere, null as moyenne, null as id_classe ")
                .append("FROM ").append(COMPETENCES_SCHEMA).append(".positionnement AS pos WHERE ");

        if (null != idEleves) {
            query.append("pos.id_eleve IN ").append(Sql.listPrepared(idEleves.getList().toArray()));
            for (int i = 0; i < idEleves.size(); i++) {
                values.add(idEleves.getString(i));
            }
        }

        if (null != idPeriode) {
            query.append(" AND pos.id_periode = ? ");
            values.add(idPeriode);
        }

        if (null != idsMatiere) {
            query.append(" AND pos.id_matiere IN ").append(Sql.listPrepared(idsMatiere.getList().toArray()));
            for (int i = 0; i < idsMatiere.size(); i++) {
                values.add(idsMatiere.getString(i));
            }
        }

        query.append(" UNION ")
                .append("SELECT IdTableAvisOrientation.id_eleve, IdTableAvisOrientation.id_periode, libelleTableAvisOrientation.libelle as avis_conseil_orientation, ")
                .append("null as avis_conseil_de_classe, null as synthese_bilan_periodique, null as positionnement, null as id_matiere, null as moyenne, null as id_classe ")
                .append("FROM ").append(COMPETENCES_SCHEMA).append(".avis_conseil_orientation AS IdTableAvisOrientation ")
                .append("JOIN ").append(COMPETENCES_SCHEMA).append(".avis_conseil_bilan_periodique AS libelleTableAvisOrientation ON ")
                .append("IdTableAvisOrientation.id_avis_conseil_bilan = libelleTableAvisOrientation.id WHERE ");

        if (null != idEleves) {
            query.append("IdTableAvisOrientation.id_eleve IN ").append(Sql.listPrepared(idEleves.getList().toArray()));
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

        query.append(" UNION ")
                .append("SELECT IdTableAvisConseil.id_eleve, IdTableAvisConseil.id_periode, null as avis_conseil_orientation,")
                .append("libelleTableAvisConseil.libelle as avis_conseil_de_classe, null as synthese_bilan_periodique, null as positionnement, null as id_matiere, ")
                .append("null as moyenne, null as id_classe FROM ").append(COMPETENCES_SCHEMA).append(".avis_conseil_de_classe AS IdTableAvisConseil ")
                .append("JOIN ").append(COMPETENCES_SCHEMA).append(".avis_conseil_bilan_periodique AS libelleTableAvisConseil ON ")
                .append("IdTableAvisConseil.id_avis_conseil_bilan = libelleTableAvisConseil.id WHERE ");

        if (null != idEleves) {
            query.append("IdTableAvisConseil.id_eleve IN ").append(Sql.listPrepared(idEleves.getList().toArray()));
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

        query.append("UNION ")
                .append("SELECT syntheseBP.id_eleve, syntheseBP.id_typeperiode as id_periode, null as avis_conseil_orientation, null as avis_conseil_de_classe,")
                .append("syntheseBP.synthese as synthese_bilan_periodique, null as positionnement, null as id_matiere, null as moyenne, null as id_classe ")
                .append("FROM ").append(COMPETENCES_SCHEMA).append(".synthese_bilan_periodique AS syntheseBP WHERE ");

        if (null != idEleves) {
            query.append("syntheseBP.id_eleve IN ").append(Sql.listPrepared(idEleves.getList().toArray()));
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
    public void setColonneReleve(String idStudent, Long idPeriod, String idSubject, String idClassSchool,
                                 JsonObject field, String column, String idUser,
                                 Handler<Either<String, JsonArray>> handler) {
        if (APPRECIATION_MATIERE_PERIODE.equals(column)) {
            String textError = "Error when update or insert, new API for update, insert appreciation_subject_period";
            log.error(textError);
            handler.handle(new Either.Left<>(textError));
        } else {
            /*
                Le positionnement est enregistré pour un élève indépendament de sa classe
                ou de ses groupes (positionnement global)
            */
            JsonArray valuesAverageOrPositioning = new JsonArray();
            valuesAverageOrPositioning.add(idPeriod).add(idStudent);
            if (Field.POSITIONNEMENT.equals(column)) idClassSchool = "";

            valuesAverageOrPositioning.add(field.getValue(column))
                    .add(idClassSchool)
                    .add(idSubject)
                    .add(field.getValue(column));

            updateOrInsertAverageOrPositioning(column, valuesAverageOrPositioning, handler);
        }
    }

    /**
     * @param column
     * @param values =[field.getValue(column)?, idClassSchool, idSubject, field.getValue(column)? ]
     * @param handler
     */
    private void updateOrInsertAverageOrPositioning(String column, JsonArray values, Handler<Either<String, JsonArray>> handler) {
        String query;
        query = "" +
                "INSERT INTO " + COMPETENCES_SCHEMA + "." + column +
                ("moyenne".equals(column) ? "_finale" : " ") +
                " (id_periode, id_eleve," + column + ", id_classe, id_matiere) VALUES " +
                " ( ? , ? , ? , ? , ? ) " +
                " ON CONFLICT (id_periode, id_eleve, id_classe, id_matiere) " +
                " DO UPDATE SET " + column + " = ? ";


        Sql.getInstance().prepared(query, values, validResultHandler(handler));
    }

    public void getMoyennesMatieresByCoefficient(JsonArray moyFinalesEleves, JsonArray listNotes,
                                                 final JsonObject result, String idEleve, JsonArray idEleves, List<Service> services,
                                                 JsonArray multiTeachers){
        Map<Long, JsonArray> notesByCoef = new HashMap<>();
        if(isNull(result.getJsonObject(COEFFICIENT))) {
            result.put(COEFFICIENT, new JsonObject());
        }
        if(result.getJsonObject(COEFFICIENT).size() > 0){
            result.put("coef", result.getJsonObject(COEFFICIENT).iterator().next().getKey());
        }

        //pour toutes les notes existantes dans la classe
        for (int i = 0; i < listNotes.size(); i++) {
            JsonObject note = listNotes.getJsonObject(i);

            if(note.getString("valeur") != null && note.getBoolean("is_evaluated")
                    && note.getString(COEFFICIENT) != null && !"0".equals(note.getString(COEFFICIENT))) {
                //Si la note fait partie d'un devoir qui n'est pas évalué, elle n'est pas prise en compte dans le calcul de la moyenne
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
                        calculMoyennesEleveByPeriode(notes, resultCoef, idEleve, idEleves,
                                null , null, services, multiTeachers);
                calculAndSetMoyenneClasseByPeriode(moyFinalesEleves, notesByDevoirByPeriodeClasse, resultCoef);
                if(isNull(result.getJsonObject(COEFFICIENT).getJsonObject(coef.toString()))){
                    result.getJsonObject(COEFFICIENT).put(coef.toString(), new JsonObject());
                }

                result.getJsonObject(COEFFICIENT).getJsonObject(coef.toString()).getMap().putAll(resultCoef.getMap());
                result.put("coef", coef.toString());
            }
        }
    }

    public HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>
    calculMoyennesEleveByPeriode(JsonArray listNotes, final JsonObject result, String idEleve, JsonArray idEleves,
                                 List<String> idsClassWithNoteAppCompNoteStudent, Long idPeriodeAsked, List<Service> services,
                                 JsonArray multiTeachers) {
        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriode = new HashMap<>();
        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse = new HashMap<>();
        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesClasseBySousMat = new HashMap<>();
        HashMap<String, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeBySousMat = new HashMap<>();

        notesByDevoirByPeriode.put(null, new HashMap<>());
        notesByDevoirByPeriodeClasse.put(null, new HashMap<>());
        notesByDevoirByPeriodeBySousMat.put(null, new HashMap<>());
        notesClasseBySousMat.put(null, new HashMap<>());
        //pour toutes les notes existantes dans la classe
        initMoyenneElevesArrays(listNotes, idEleve, idEleves, idsClassWithNoteAppCompNoteStudent, idPeriodeAsked, notesByDevoirByPeriode,
                notesByDevoirByPeriodeClasse, notesClasseBySousMat, notesByDevoirByPeriodeBySousMat, services , multiTeachers);

        // permettra de stocker les moyennes des sousMatières par période
        result.put(MOYENNES, new JsonArray());
        result.put("_"+ Field.MOYENNE, new JsonObject());
        HashMap<Long,JsonArray> listMoyDevoirs = new HashMap<>();

        // Calcul des moyennes par période pour l'élève
        for(Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> entryPeriode : notesByDevoirByPeriode.entrySet()) {
            //entryPeriode contient les notes de l'élève pour une période
            Long idPeriode = entryPeriode.getKey();
            String periodeKey = isNull(idPeriode) ? "null" : idPeriode.toString();
            listMoyDevoirs.put(idPeriode, new JsonArray());
            result.getJsonObject("_" + Field.MOYENNE).put(periodeKey, new JsonObject());

            // Paramètres pour le calcul des moyennes
            final Boolean withStat = false;
            final Integer diviseur = 20;
            final Boolean annual = false;

            // Calcul des moyennes des notes par sous-matières pour la période courante
            HashMap<Long, ArrayList<NoteDevoir>> notesSubMatForPeriode = notesByDevoirByPeriodeBySousMat.get(periodeKey);
            if(isNotNull(notesSubMatForPeriode) && notesSubMatForPeriode.size() > 0 ) {
                double total = 0;
                double totalCoeff = 0;
                boolean hasNote = false;

                for (Map.Entry<Long, ArrayList<NoteDevoir>> smEntry : notesSubMatForPeriode.entrySet()) {
                    Long idSousMatiere = smEntry.getKey();
                    Service serv = smEntry.getValue().get(0).getService();
                    double coeff = 1.d;
                    if(serv != null && serv.getSubtopics() != null && serv.getSubtopics().size() > 0 ) {
                        SubTopic subTopic =  serv.getSubtopics().stream()
                                .filter(el ->
                                        el.getId().equals(idSousMatiere)
                                ).findFirst().orElse(null);
                        if(subTopic != null)
                            coeff = subTopic.getCoefficient();
                    }
                    if (isNotNull(idSousMatiere)) {
                        JsonObject moyenne = utilsService.calculMoyenne(smEntry.getValue(), withStat, diviseur, annual);
                        result.getJsonObject("_" + Field.MOYENNE).getJsonObject(periodeKey).put(idSousMatiere.toString(),
                                moyenne);
                        total += coeff * moyenne.getDouble(Field.MOYENNE);
                        totalCoeff += coeff;
                        hasNote = true;
                    }
                }
                if (totalCoeff == 0) {
                    log.error("Found a 0 or negative coefficient in calculMoyennesEleveByPeriode, please check your subtopics " +
                            "coefficients (value of totalCoeff : " + totalCoeff + ")");
                    return null;
                }
                Double moyenne = Math.round((total / totalCoeff) * Field.ROUNDER) / Field.ROUNDER;

                if (hasNote)
                    result.put(Field.MOYENNE, moyenne);
                else
                    result.put(Field.MOYENNE, "NN");

                JsonObject moyenneTotale = new JsonObject().put(Field.MOYENNE,  moyenne)
                        .put(Field.HASNOTE, true);
                moyenneTotale.put(Field.ID, idPeriode);
                listMoyDevoirs.get(idPeriode).add(moyenneTotale);
            }
            else {
                // calcul des moyennes des notes de la matière de l'élève
                for (Map.Entry<Long, ArrayList<NoteDevoir>> entry : entryPeriode.getValue().entrySet()) {
                    JsonObject moyenne = utilsService.calculMoyenne(entry.getValue(), withStat, diviseur, annual);
                    moyenne.put(Field.ID, idPeriode);
                    listMoyDevoirs.get(idPeriode).add(moyenne);
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
                    Double moyClasse = calculMoyenneByElevesByPeriode(new JsonObject(), notes, new HashMap<>(), new HashMap<>(), idPeriode);
                    result.getJsonObject("_" + MOYENNES_CLASSE).getJsonObject(idPeriode.toString())
                            .put(idSousMat.toString(), moyClasse);
                }
            }
        }

        return notesByDevoirByPeriodeClasse;
    }

    private void  initMoyenneElevesArrays(JsonArray listNotes, String idEleve, JsonArray idEleves, List<String> idsClassWithNoteAppCompNoteStudent, Long idPeriodeAsked,
                                          HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriode,
                                          HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse,
                                          HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesClasseBySousMat,
                                          HashMap<String, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeBySousMat,
                                          List<Service> services, JsonArray multiTeachers) {
        for (int i = 0; i < listNotes.size(); i++) {
            JsonObject note = listNotes.getJsonObject(i);
            if((note.getBoolean("formative") != null && note.getBoolean("formative")) ||
            note.getString(Field.VALEUR) == null || !note.getBoolean(Field.IS_EVALUATED)
                    || note.getString(Field.COEFFICIENT) == null || "0".equals(note.getString(Field.COEFFICIENT))) {
                continue;
            }
            Long id_periode = note.getLong(Field.ID_PERIODE);
            String id_eleve = note.getString(Field.ID_ELEVE);
            Long id_sousMatiere = note.getLong(ID_SOUS_MATIERE);
            String id_groupe = note.getString(Field.ID_GROUPE);
            NoteDevoir noteDevoir = setNoteDevoir(services, multiTeachers, note, id_periode, id_sousMatiere);
            if(isNotNull(noteDevoir)) {
                if(idsClassWithNoteAppCompNoteStudent != null && idPeriodeAsked != null){
                    if(idEleve.equals(id_eleve) && idPeriodeAsked.equals(id_periode) && id_groupe != null &&
                            !idsClassWithNoteAppCompNoteStudent.contains(id_groupe))
                        idsClassWithNoteAppCompNoteStudent.add(id_groupe);
                }

                if (!notesByDevoirByPeriode.containsKey(id_periode)) {
                    notesByDevoirByPeriode.put(id_periode, new HashMap<>());
                    notesByDevoirByPeriodeClasse.put(id_periode, new HashMap<>());
                    notesByDevoirByPeriodeBySousMat.put(id_periode.toString(), new HashMap<>());
                    notesClasseBySousMat.put(id_periode, new HashMap<>());
                }

                if (!idEleves.contains(id_eleve)) {
                    idEleves.add(id_eleve);
                }



                //ajouter la note à la période correspondante et à l'année pour l'élève
                if (note.getString(Field.ID_ELEVE).equals(idEleve)) {
                    utilsService.addToMap(id_periode, notesByDevoirByPeriode.get(id_periode), noteDevoir);
                    utilsService.addToMap(null, notesByDevoirByPeriode.get(null), noteDevoir);
                    if (id_sousMatiere != null)
                        utilsService.addToMap(id_periode.toString(), id_sousMatiere, notesByDevoirByPeriodeBySousMat,
                                noteDevoir);
                }

                //ajouter la note à la période correspondante et à l'année pour toute la classe
                utilsService.addToMap(id_periode, notesByDevoirByPeriodeClasse.get(id_periode), noteDevoir);
                utilsService.addToMap(null, notesByDevoirByPeriodeClasse.get(null), noteDevoir);
                if (isNotNull(id_sousMatiere)) {
                    utilsService.addToMap(id_sousMatiere, notesClasseBySousMat.get(id_periode), noteDevoir);
                    utilsService.addToMap(id_sousMatiere, notesClasseBySousMat.get(null), noteDevoir);
                }
            }

        }
    }

    private NoteDevoir setNoteDevoir(List<Service> services, JsonArray multiTeachers, JsonObject note, Long id_periode, Long id_sousMatiere) {
        Matiere matiere = new Matiere(note.getString(Field.ID_MATIERE));
        Teacher teacher = new Teacher(note.getString(Field.OWNER));
        Group group = new Group(note.getString(Field.ID_GROUPE));

        Service service = services.stream()
                .filter(el -> teacher.getId().equals(el.getTeacher().getId())
                        && matiere.getId().equals(el.getMatiere().getId())
                        && group.getId().equals(el.getGroup().getId()))
                .findFirst().orElse(null);

        if (service == null){
            //On regarde les multiTeacher
            for(Object mutliTeachO: multiTeachers){
                //multiTeaching.getString(Field.SECOND_TEACHER_ID).equals(teacher.getId()
                JsonObject multiTeaching  =(JsonObject) mutliTeachO;

                if(multiTeaching.getString(Field.SECOND_TEACHER_ID).equals(teacher.getId())
                        && multiTeaching.getString(Field.CLASS_OR_GROUP_ID).equals(group.getId())
                        && multiTeaching.getString(Field.SUBJECT_ID).equals(matiere.getId())){

                    service = services.stream()
                            .filter(el -> multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(el.getTeacher().getId())
                                    && matiere.getId().equals(el.getMatiere().getId())
                                    && group.getId().equals(el.getGroup().getId()))
                            .findFirst().orElse(null);
                }
            }
        }

        NoteDevoir noteDevoir = (service != null) ? new NoteDevoir(Double.valueOf(note.getString(Field.VALEUR)),
                Double.valueOf(note.getString(Field.DIVISEUR)), note.getBoolean(Field.RAMENER_SUR),
                Double.valueOf(note.getString(Field.COEFFICIENT)), note.getString(Field.ID_ELEVE),
                id_periode, service, id_sousMatiere) : null;
        return noteDevoir;
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


    private JsonArray fullJoinAppMoyPosiFinale(JsonArray appreciation, JsonArray moyenneFinale,
                                               JsonArray positionnement){
        JsonArray result = new JsonArray();
        Map<String, JsonObject> joinMap = new HashMap<>();

        // SELECT FROM appreciation
        for(int i = 0; i < appreciation.size(); i++){
            JsonObject appreciationObj = appreciation.getJsonObject(i);
            String idClasse = appreciationObj.getString("id_classe_appreciation");
            Integer idPeriode = appreciationObj.getInteger("id_periode_appreciation");
            String key = idClasse + idPeriode;
            joinMap.put(key, appreciationObj);
        }

        // FULL JOIN  moyenneFinale
        for(int i = 0; i < moyenneFinale.size(); i++){
            JsonObject moyenneFinaleObj = moyenneFinale.getJsonObject(i);
            String idClasse = moyenneFinaleObj.getString("id_classe_moyfinale");
            Integer idPeriode = moyenneFinaleObj.getInteger("id_periode_moyenne_finale");
            String key = idClasse + idPeriode;
            if(!joinMap.containsKey(key)) {
                joinMap.put(key, moyenneFinaleObj);
            } else {
                joinMap.replace(key, leftJoin(joinMap.get(key), moyenneFinaleObj));
            }
        }

        // FULL JOIN POSITIONNEMENT
        for(int i = 0; i < positionnement.size(); i++){
            JsonObject positionnementObj = positionnement.getJsonObject(i);
            String idClasse = positionnementObj.getString("id_classe_posi");
            Integer idPeriode = positionnementObj.getInteger("id_periode_positionnement");
            String key = idClasse + idPeriode;
            if(!joinMap.containsKey(key)) {
                joinMap.put(key, positionnementObj);
            } else {
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
                                                       JsonArray idGroups, Handler<Either<String, JsonArray>> handler){
        Future<JsonArray> appreciationFuture = Future.future();
        getAppreciationMatierePeriode(idEleve, idMatiere, idPeriode, idGroups, event -> {
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
            } else{
                handler.handle(new Either.Left<>(event.cause().getMessage()));
            }
        });
    }

    public Double calculMoyenneByElevesByPeriode(JsonObject result, ArrayList<NoteDevoir> allNotes,
                                                 Map<Long, Map<String, Double>> moyFinalesElevesByPeriode,
                                                 Map<Long, List<String>> moyFinalesNNElevesByPeriode, Long idPeriode){
        if(!result.containsKey(NOTES_BY_PERIODE_BY_STUDENT)) {
            result.put(NOTES_BY_PERIODE_BY_STUDENT, new JsonObject());
        }
        String periodeKey = isNull(idPeriode) ? "null" : idPeriode.toString();
        if(!result.getJsonObject(NOTES_BY_PERIODE_BY_STUDENT).containsKey(periodeKey)){
            result.getJsonObject(NOTES_BY_PERIODE_BY_STUDENT).put(periodeKey, new JsonObject());
        }

        HashMap<String, ArrayList<NoteDevoir>> notesPeriodeByEleves = new HashMap<>();
        HashMap<String, HashMap<Long, ArrayList<NoteDevoir>>> notesByElevesBySousMat = new HashMap<>();

        //mettre dans notesPeriodeByEleves idEleve -> notes de l'élève pour la période

        for (NoteDevoir note : allNotes) {
            String id_eleve = note.getIdEleve();
            if (!(moyFinalesNNElevesByPeriode != null && moyFinalesNNElevesByPeriode.containsKey(idPeriode)
                    && moyFinalesNNElevesByPeriode.get(idPeriode).contains(id_eleve))) {
                if (!notesPeriodeByEleves.containsKey(id_eleve)) {
                    notesPeriodeByEleves.put(id_eleve, new ArrayList<>());
                }
                if (!notesByElevesBySousMat.containsKey(id_eleve)) {
                    notesByElevesBySousMat.put(id_eleve, new HashMap<Long, ArrayList<NoteDevoir>>());
                    if (isNotNull(note.getService())) {
                        note.getService().getSubtopics().stream().filter(subtopic -> !notesByElevesBySousMat.get(id_eleve).containsKey(subtopic.getId()))
                                .forEach(subtopic -> notesByElevesBySousMat.get(id_eleve).put(subtopic.getId(), new ArrayList<>()));
                    }
                }
                notesPeriodeByEleves.get(id_eleve).add(note);
            }
        }


        Integer nbEleve = 0;
        Double sumMoyClasse = 0.0;
        Map<String, Double> moyFinalesPeriode = null;
        if(moyFinalesElevesByPeriode != null && moyFinalesElevesByPeriode.containsKey(idPeriode)) {
            moyFinalesPeriode = moyFinalesElevesByPeriode.get(idPeriode);
            //un élève peut ne pas être noté et avoir une moyenne finale
            //il faut donc ajouter sa moyenne finale à sumMoyClasse
            for(Map.Entry<String,Double> moyFinale : moyFinalesPeriode.entrySet() ){
                String idEleve = moyFinale.getKey();
                Double moyEleve = moyFinale.getValue();
                sumMoyClasse += moyEleve;
                nbEleve ++;
                result.getJsonObject(NOTES_BY_PERIODE_BY_STUDENT).getJsonObject(periodeKey).put(idEleve, moyEleve);
            }
        }

        //pour tous les élèves qui ont une note mettre leur moyenne finale ou auto dans sumMoyClasse
        for(Map.Entry<String, ArrayList<NoteDevoir>> notesPeriodeByEleve : notesPeriodeByEleves.entrySet()){
            //TODO : Séparer les moyennes des sous matières puis faire leur moyenne afin d'avoir la bonne moyenne par élève,
            // par exemple à l'aide d'un tableau qui sépareraient les moyennes des différentes sous matières
            String idEleve = notesPeriodeByEleve.getKey();
            //si l'éleve en cours a une moyenne finale sur la période l'ajouter à sumMoyClasse
            //sinon calculer la moyenne de l'eleve et l'ajouter à sumMoyClasse
            if (moyFinalesPeriode == null || !moyFinalesPeriode.containsKey(idEleve)){
                if ("NN".equals(utilsService.calculMoyenne(notesPeriodeByEleve.getValue(),
                        false, 20,false).getValue("moyenne"))){
                    nbEleve--;
                } else {
                    try{
                        if( notesPeriodeByEleve.getValue().size() > 0) {
                            List<SubTopic> subTopics = notesPeriodeByEleve.getValue().get(0).getService().getSubtopics();
                            Map<Long, ArrayList<NoteDevoir>> notesBySubTopic = new HashMap<>();
                            for (NoteDevoir note : notesPeriodeByEleve.getValue()) {
                                if (notesBySubTopic.containsKey(note.getIdSousMatiere())) {
                                    ArrayList<NoteDevoir> noteDevoirList = notesBySubTopic.get(note.getIdSousMatiere());
                                    noteDevoirList.add(note);
                                    notesBySubTopic.put(note.getIdSousMatiere(), noteDevoirList);
                                } else {
                                    ArrayList<NoteDevoir> noteDevoirList = new ArrayList<>();
                                    noteDevoirList.add(note);
                                    notesBySubTopic.put(note.getIdSousMatiere(), noteDevoirList);

                                }
                            }
                            AtomicReference<Double> moyenne = new AtomicReference<>(0.d);
                            AtomicReference<Double> coeff = new AtomicReference<>(0.d);
                            Boolean withStat = false;
                            Boolean annual = false;
                            int diviseur = 20;

                            notesBySubTopic.forEach((subTopicId, noteDevoirList) -> {
                                SubTopic subTopic = subTopics.stream().filter(s -> subTopicId.equals(s.getId()))
                                        .findFirst().orElse(null);
                                if (subTopic != null) {
                                    if (!utilsService.calculMoyenne(noteDevoirList,
                                            withStat, diviseur, annual).getValue(Field.MOYENNE).equals("NN"))
                                        moyenne.updateAndGet(v -> v + utilsService.calculMoyenne(noteDevoirList,
                                                withStat, diviseur, annual).getDouble(Field.MOYENNE) * subTopic.getCoefficient());
                                    coeff.updateAndGet(v -> v + subTopic.getCoefficient());

                                } else {
                                    if (!utilsService.calculMoyenne(noteDevoirList,
                                            withStat, diviseur, annual).getValue(Field.MOYENNE).equals("NN"))
                                        moyenne.updateAndGet(v -> v + utilsService.calculMoyenne(noteDevoirList,
                                                withStat, diviseur, annual).getDouble(Field.MOYENNE));
                                    coeff.updateAndGet(v -> v + 1.d);
                                }
                            });

                            Double moyEleve;
                            if (coeff.get() != 0.d) {
                                moyEleve = moyenne.get() / coeff.get();
                            } else {
                                moyEleve = utilsService.calculMoyenne(notesPeriodeByEleve.getValue(),
                                        withStat, diviseur, annual).getDouble(Field.MOYENNE);
                            }
                            sumMoyClasse += moyEleve;
                            nbEleve++;
                            result.getJsonObject(NOTES_BY_PERIODE_BY_STUDENT).getJsonObject(periodeKey).put(idEleve, moyEleve);
                        }
                    }catch (NullPointerException ignored){
                    }
                }
            }
        }
        DecimalFormat decimalFormat = new DecimalFormat("#.0");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);

        if(nbEleve!=0)
            return Double.valueOf(decimalFormat.format((sumMoyClasse / nbEleve)).replaceAll(",", "."));
        else
            return Double.valueOf(decimalFormat.format((sumMoyClasse / 1)).replaceAll(",", "."));
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
                } else if(isNull(moyFinale.getValue("moyenne")) && isNotNull(moyFinale.getValue("id_periode"))) {
                    Long periode = moyFinale.getLong("id_periode");
                    if (!moyFinalesNN.containsKey(periode)) {
                        moyFinalesNN.put(periode, new ArrayList<>());
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

        // Afin de gérer le cas où aucun notes est présente mais avec des moyennes finales
        if(notesByDevoirByPeriodeClasse.entrySet().iterator().next().getValue().size() == 0
                && moyFinalesEleves != null && moyFinalesEleves.size() > 0){
            moyFinalesEleves.forEach(moyFinale -> {
                JsonObject moyFinaleJson = (JsonObject) moyFinale;
                Long idP = moyFinaleJson.getLong("id_periode");
                if(moyFinaleJson.getString("moyenne") != null){
                    NoteDevoir noteEleve = new NoteDevoir(Double.valueOf(moyFinaleJson.getString("moyenne")),
                            20.0, false, 1.0, moyFinaleJson.getString("id_eleve"));
                    if(!notesByDevoirByPeriodeClasse.containsKey(idP))
                        notesByDevoirByPeriodeClasse.put(idP, new HashMap<>());
                    if(!notesByDevoirByPeriodeClasse.get(idP).containsKey(idP))
                        notesByDevoirByPeriodeClasse.get(idP).put(idP, new ArrayList<>());
                    notesByDevoirByPeriodeClasse.get(idP).get(idP).add(noteEleve);
                }
            });
        }

        //notesByDevoirByPeriodeClasse contient au moins la clé null
        //pour chaque période où il y a des notes on calcul la moyenne de la classe
        //dans ce calcul on tient compte qu'un élève peut avoir une moyenne finale et pas de note
        //il suffit donc d'une note sur un eleve et une période pour que la moyenne de la classe
        // soit calculée par la méthode calculMoyenneClasseByPeriode
        for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByPeriode : notesByDevoirByPeriodeClasse.entrySet()) {
            Long idPeriode = notesByPeriode.getKey();
            JsonObject moyennePeriodeClasse = new JsonObject();
            // calcul de la moyenne de la classe si on a des notes pour 1 trimestre
            if (idPeriode != null) {
                moyennePeriodeClasse.put("id", idPeriode);
                ArrayList<NoteDevoir> allNotes = notesByPeriode.getValue().get(idPeriode);
                Double classAverage = calculMoyenneByElevesByPeriode(result, allNotes, moyFinales, moyFinalesNN, idPeriode);
                JsonObject averageStudentCurrentPeriode = result.getJsonObject(NOTES_BY_PERIODE_BY_STUDENT).getJsonObject(idPeriode.toString());
                //cas : tous les élèves notés ont une moyenne finale NN
                if (classAverage == 0 && averageStudentCurrentPeriode != null && averageStudentCurrentPeriode.isEmpty()) {
                    moyennePeriodeClasse.put("moyenne", "NN");
                } else {
                    moyennePeriodeClasse.put("moyenne", classAverage);
                }
                moyennesClasses.add(moyennePeriodeClasse);
            }
        }

        //si moyennesClasses.size()> 0 c'est qu'il y a eu soit des moyennees finales (NN ou number) soit des notes sur au moins un trimestre
        // alors on peut calculer la moyenne de la classe pour l'année
        if (moyennesClasses.size() > 0) {
            int nbPeriode = moyennesClasses.size();
            Double sumMoyPeriode = 0.0;
            for (int i = 0; i < moyennesClasses.size(); i++) {
                try {
                    sumMoyPeriode += moyennesClasses.getJsonObject(i).getDouble("moyenne");
                } catch (ClassCastException c) {
                    nbPeriode --;
                }
            }
            JsonObject moyennePeriodeClasse = new JsonObject();
            if(nbPeriode != 0) {
                DecimalFormat decimalFormat = new DecimalFormat("#.0");
                decimalFormat.setRoundingMode(RoundingMode.HALF_UP);

                moyennePeriodeClasse.put("id", (JsonObject) null).put("moyenne",
                        Double.valueOf(decimalFormat.format(sumMoyPeriode / moyennesClasses.size()).replaceAll(",", ".")));
            } else {
                moyennePeriodeClasse.put("id", (JsonObject) null).put("moyenne", "NN");
            }
            moyennesClasses.add(moyennePeriodeClasse);
        }
        result.put(MOYENNES_CLASSE, moyennesClasses);
    }

    public void setRankAndMinMaxInClasseByPeriode(final Long idPeriodAsked, final String idEleve,
                                                  final HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse,
                                                  final JsonArray moyFinalesEleves, final JsonObject result) {
        JsonArray ranks = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray allMinMax = new fr.wseduc.webutils.collections.JsonArray();

        // Afin de gérer le cas où aucun notes est présente mais avec des moyennes finales
        if(notesByDevoirByPeriodeClasse.entrySet().iterator().next().getValue().size() == 0 && moyFinalesEleves.size() > 0){
            moyFinalesEleves.forEach(moyFinale -> {
                JsonObject moyFinaleJson = (JsonObject) moyFinale;
                Long idP = moyFinaleJson.getLong("id_periode");
                NoteDevoir noteEleve = (moyFinaleJson.getString("moyenne") != null) ?
                        new NoteDevoir(Double.valueOf(moyFinaleJson.getString("moyenne")),20.0, false, 1.0, moyFinaleJson.getString("id_eleve")) :
                        new NoteDevoir(null,20.0, false, 1.0, moyFinaleJson.getString("id_eleve"));
                if(!notesByDevoirByPeriodeClasse.containsKey(idP))
                    notesByDevoirByPeriodeClasse.put(idP, new HashMap<>());
                if(!notesByDevoirByPeriodeClasse.get(idP).containsKey(idP))
                    notesByDevoirByPeriodeClasse.get(idP).put(idP, new ArrayList<>());
                notesByDevoirByPeriodeClasse.get(idP).get(idP).add(noteEleve);
            });
        }

        for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByPeriode : notesByDevoirByPeriodeClasse.entrySet()) {
            Long idPeriode = notesByPeriode.getKey();

            if (idPeriode != null) {
                //get all scores on the periode
                ArrayList<NoteDevoir> allNotes = notesByPeriode.getValue().get(idPeriode);

                //group all scores by student
                Map<String, List<NoteDevoir>> allNotesByEleve = allNotes.stream().collect(Collectors.groupingBy(NoteDevoir::getIdEleve));

                //Group note final by eleves
                Map<String, Double> moyennesFinales = new HashMap<>();

                //Group Non Noted Final average by eleves
                ArrayList<String> moyennesFinalesNN = new ArrayList<>();

                for (int i = 0; i < moyFinalesEleves.size(); i++) {
                    JsonObject mf = moyFinalesEleves.getJsonObject(i);
                    if(isNotNull(mf) && isNotNull(mf.getValue("id_periode")) && isNotNull(mf.getValue("id_eleve"))) {
                        Long periodeMF = mf.getLong("id_periode");
                        if (idPeriode.equals(periodeMF)) {
                            if(isNotNull(mf.getValue("moyenne"))) {
                                moyennesFinales.put(mf.getString("id_eleve"),
                                        Double.parseDouble(mf.getString("moyenne").replace(",",".")));
                            } else {
                                moyennesFinalesNN.add(mf.getString("id_eleve"));
                            }
                        }
                    }
                }

                //calculate the average score with coef by student
                Map<String, Double> allMoyennes = new HashMap<>();
                final Double[] min = {null};
                final Double[] max = {null};

                allNotesByEleve.forEach((key, value) -> {
                    Map<Long, ArrayList<NoteDevoir>> notesBySubTopic = new HashMap<>();
                    List<SubTopic> subTopics;
                    try {
                        subTopics = value.get(0).getService().getSubtopics();
                    }catch (NullPointerException e){
                        subTopics = new ArrayList<>();
                    }
                    if(!moyennesFinalesNN.contains(key)) {
                        value.forEach(note ->{
                            if(notesBySubTopic.containsKey(note.getIdSousMatiere())){
                                ArrayList<NoteDevoir> noteDevoirList = notesBySubTopic.get(note.getIdSousMatiere());
                                noteDevoirList.add(note);
                                notesBySubTopic.put(note.getIdSousMatiere(),noteDevoirList);
                            }else{
                                ArrayList<NoteDevoir> noteDevoirList = new ArrayList<>();
                                noteDevoirList.add(note);
                                notesBySubTopic.put(note.getIdSousMatiere(),noteDevoirList);
                            }
                        });

                        AtomicReference<Double> moyenneAtomic = new AtomicReference<>(0.d);
                        AtomicReference<Double> coeff = new AtomicReference<>(0.d);
                        Boolean withStat = false;
                        Boolean annual = false;
                        int diviseur = 20;


                        List<SubTopic> finalSubTopics = subTopics;
                        notesBySubTopic.forEach((subTopicId, noteDevoirList) ->{
                            SubTopic subTopic = finalSubTopics.stream().filter(s -> subTopicId.equals(s.getId()))
                                    .findFirst().orElse(null);
                            if(subTopic != null){
                                if(!utilsService.calculMoyenne(noteDevoirList,
                                        withStat, diviseur, annual).getValue(Field.MOYENNE).equals("NN"))
                                    moyenneAtomic.updateAndGet(v -> v + utilsService.calculMoyenne(noteDevoirList,
                                            withStat, diviseur, annual).getDouble(Field.MOYENNE) * subTopic.getCoefficient());
                                coeff.updateAndGet(v -> v + subTopic.getCoefficient());

                            }else{
                                if(!utilsService.calculMoyenne(noteDevoirList,
                                        withStat, diviseur, annual).getValue(Field.MOYENNE).equals("NN"))
                                    moyenneAtomic.updateAndGet(v -> v + utilsService.calculMoyenne(noteDevoirList,
                                            withStat, diviseur, annual).getDouble(Field.MOYENNE));
                                coeff.updateAndGet(v -> v + 1.d);
                            }
                        });
                        if(!coeff.get().equals(0.d)){
                            Double moyenneTmp = moyenneAtomic.get() / coeff.get();
                            if (moyennesFinales.containsKey(key)) {
                                moyenneTmp = moyennesFinales.get(key);
                            }
                            //manage min value
                            if (min[0] == null || min[0] > moyenneTmp) {
                                min[0] = moyenneTmp;
                            }
                            //manage max value
                            if (max[0] == null || max[0] < moyenneTmp) {
                                max[0] = moyenneTmp;
                            }

                            allMoyennes.put(key, moyenneTmp);
                        }
                    }
                });


                if (min[0] != null && max[0] != null) {
                    JsonObject minMaxObj = new JsonObject();
                    DecimalFormat decimalFormat = new DecimalFormat("#.0");
                    decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
                    minMaxObj.put("id_periode", idPeriode)
                            .put("min",decimalFormat.format(min[0]))
                            .put("max", decimalFormat.format(max[0]));
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
                            break;
                        }
                    }
                }

                //if student have rank we put it in the final object
                if(rank >= 0){
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
            String idStudent = note.getString(Field.ID_ELEVE);

            if (!notesByStudent.containsKey(idStudent)) {
                notesByStudent.put(idStudent, new JsonArray().add(note));
            } else {
                notesByStudent.get(idStudent).add(note);
            }
        }
        return notesByStudent;
    }
    private void getMaxOrAvgCompNoteByPeriode(JsonArray listCompNotes,
                                              HashMap<Long, ArrayList<NoteDevoir>> notesByDevoirByPeriode,
                                              HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByPeriodeBySousMatiere,
                                              JsonArray tableauConversion,
                                              List<String> idsClassWithNoteAppCompNoteStudent, Long idPeriodAsked, boolean isAvg){
        notesByDevoirByPeriode.put(null, new ArrayList<>());

        // 1- parcourir la listCompNotes pour en extraire les map map<idP, JAcompNote> et map<idP,map<idssM,JAcompNote>>
        HashMap<Long, JsonArray> compNotesByPeriode = new HashMap<>();
        HashMap<Long,HashMap<Long,JsonArray>> compNotesBySousMatByPeriode = new HashMap<>();

        for(int i = 0; i < listCompNotes.size(); i++){
            JsonObject compNote = listCompNotes.getJsonObject(i);
            Long id_periode = compNote.getLong(Field.ID_PERIODE);
            Long idSousMatiere = compNote.getLong(ID_SOUS_MATIERE);

            String group_id = compNote.getString("id_groupe");
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
            if(idPeriodAsked != null && idsClassWithNoteAppCompNoteStudent != null ) {
                if (idPeriodAsked.equals(id_periode) && group_id != null && !idsClassWithNoteAppCompNoteStudent.contains(group_id)) {
                    idsClassWithNoteAppCompNoteStudent.add(group_id);
                }
            }
            if(isNotNull(idSousMatiere)){
                utilsService.addToMapWithJsonArray(idSousMatiere,compNotesBySousMatByPeriode.get(id_periode),compNote);
            }
            utilsService.addToMapWithJsonArray(id_periode,compNotesByPeriode,compNote);
        }
        //2- loop map<idP, JAcompNote> -> JAcompNoteMaxByPeriode -> map<idP,list<NoteDevoirMax>> = notesByDevoirByPeriode
        compNotesByPeriode.forEach((id_period,compNotes) -> {

            Map<String, JsonObject> MaxOrAvgCompNotesByCompetence = calculMaxOrAvgCompNoteItem(compNotes, tableauConversion,true, isAvg);
            MaxOrAvgCompNotesByCompetence.forEach((id_comp,maxCompNote) -> {
                NoteDevoir noteDevoir;
                Double niveauFinal = maxCompNote.getDouble("niveau_final");
                Long niveauFinalAnnuel = maxCompNote.getLong("niveau_final_annuel");
                if (isNotNull(niveauFinalAnnuel) ) {
                    noteDevoir = new NoteDevoir(Double.valueOf(niveauFinalAnnuel)+1,1.0,false,1.0);
                }/* else if (isNotNull(niveauFinal) ) {
                    noteDevoir = new NoteDevoir(Double.valueOf(niveauFinal)+1,1.0,false,1.0);
                }*/ else {
                    noteDevoir = new NoteDevoir(maxCompNote.getDouble("evaluation") +1, 1.0,
                            false, 1.0);
                }
                utilsService.addToMap(id_period, notesByDevoirByPeriode, noteDevoir);
                // positionnement sur l'année
                utilsService.addToMap(null, notesByDevoirByPeriode, noteDevoir);
            });
        });

        //3-loop map<idP,map<idssM,JAcompNote>> ->JAcompNoteMaxBySousMatByPerode -> map<idP,map<idssM,list<NoteDevoirMax>> = notesByPeriodeBySousMatiere
        compNotesBySousMatByPeriode.forEach((id_periode,mapSousMatCompNotes) ->{
            mapSousMatCompNotes.forEach((id_sousMat, compNotesSousMat) -> {
                Map<String,JsonObject> MaxOrAvgCompNoteSousMatByComp = calculMaxOrAvgCompNoteItem(compNotesSousMat, tableauConversion,
                        false, isAvg);

                MaxOrAvgCompNoteSousMatByComp.forEach((id_comp,maxCompNoteSousMat) -> {
                    NoteDevoir noteDevoir = new NoteDevoir(maxCompNoteSousMat.getDouble("evaluation") +1.d,
                            1.0, false, 1.0);

                    if(!notesByPeriodeBySousMatiere.containsKey(id_periode)) notesByPeriodeBySousMatiere.put(id_periode,
                            new HashMap());
                    utilsService.addToMap(id_sousMat,notesByPeriodeBySousMatiere.get(id_periode),noteDevoir);
                });
            });
        });
    }

    public void calculPositionnementAutoByEleveByMatiere(JsonArray listCompNotes, JsonObject result, Boolean annual,
                                                         JsonArray tableauConversion,
                                                         List<String> idsClassWithNoteAppCompNoteStudent,
                                                         Long idPeriodAsked, boolean isAvgSkill) {
        HashMap<Long, JsonArray> listMoyDevoirs = new HashMap<>();
        HashMap<Long, ArrayList<NoteDevoir>> notesByDevoirByPeriode = new HashMap<>();
        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByPeriodeBySousMatiere = new HashMap<>();

        result.put(POSITIONNEMENTS_AUTO, new JsonArray());
        result.put("_" + POSITIONNEMENTS_AUTO, new JsonObject());

        getMaxOrAvgCompNoteByPeriode(listCompNotes, notesByDevoirByPeriode,notesByPeriodeBySousMatiere, tableauConversion,
                idsClassWithNoteAppCompNoteStudent, idPeriodAsked ,isAvgSkill);

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
    public void getNotesAndMoyFinaleByClasseAndPeriode(List<String> idsEleve, JsonArray idsGroups, Integer idPeriode,
                                                       String idEtablissement, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT * FROM " +
                "(SELECT notes.id_eleve AS id_eleve_notes, devoirs.id, devoirs.id_periode, devoirs.id_matiere, devoirs.owner, rel_devoirs_groupes.id_groupe, " +
                "rel_devoirs_groupes.type_groupe, devoirs.coefficient, devoirs.diviseur, devoirs.ramener_sur, devoirs.is_evaluated, notes.valeur, devoirs.id_sousmatiere " +
                "FROM notes.devoirs LEFT JOIN notes.notes ON (devoirs.id = notes.id_devoir AND " +
                "notes.id_eleve IN " + Sql.listPrepared(idsEleve) + " ) " +
                "INNER JOIN notes.rel_devoirs_groupes ON (devoirs.id = rel_devoirs_groupes.id_devoir AND " +
                "rel_devoirs_groupes.id_groupe IN " + Sql.listPrepared(idsGroups.getList()) + " ) ";

        for (String idEleve: idsEleve) {
            values.add(idEleve);
        }
        for (Object idGroupe: idsGroups.getList()) {
            values.add(idGroupe);
        }

        if(idPeriode != null){
            query += "WHERE devoirs.id_periode = ?";
            values.add(idPeriode);
        }
        if(idEtablissement != null){
            if(idPeriode != null) {
                query += " AND ";
            } else {
                query += " WHERE ";
            }
            query += "devoirs.id_etablissement = ?";
            values.add(idEtablissement);
        }

        query += " ORDER BY notes.id_eleve , devoirs.id_matiere ) AS devoirs_notes " +
                "FULL JOIN ( SELECT moyenne_finale.id_periode AS id_periode_moyf, moyenne_finale.id_eleve AS id_eleve_moyf, " +
                "moyenne_finale.moyenne AS moyenne_finale, moyenne_finale.id_matiere AS id_mat_moyf " +
                "FROM notes.moyenne_finale WHERE moyenne_finale.id_eleve IN " + Sql.listPrepared(idsEleve) +
                ((idPeriode != null) ? " AND moyenne_finale.id_periode = ? )" : ")") + " AS moyf " +
                "ON (devoirs_notes.id_matiere = moyf.id_mat_moyf AND devoirs_notes.id_eleve_notes = moyf.id_eleve_moyf " +
                "AND devoirs_notes.id_periode = moyf.id_periode_moyf)";

        for (String idEleve: idsEleve) {
            values.add(idEleve);
        }
        if(idPeriode != null){
            values.add(idPeriode);
        }
        Sql.getInstance().prepared(query, values, validResultHandler(handler));
    }

    @Override
    public void getMoysEleveByMatByPeriode(String idClasse, Integer idPeriode, String idEtablissement, Integer typeGroupe,
                                           String name, SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
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
                                Utils.getGroupesClasse(eb, new fr.wseduc.webutils.collections.JsonArray().add(idClasse),
                                        getGroupesClasseHandler(eleves, handler, idsEleve, idPeriode,
                                                idEtablissement, idClasse, typeGroupe, name, mapAllidMatAndidTeachers, mapIdMatListMoyByEleve));
                            }
                        }
                    }
                }
        );
    }

    private Handler<Either<String, JsonArray>> getGroupesClasseHandler(List<Eleve> eleves,
                                                                       Handler<Either<String, JsonObject>> handler,
                                                                       List<String> idsEleve, Integer idPeriode,
                                                                       String idEtablissement, String idClasse, Integer typeGroupe,
                                                                       String name, SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
                                                                       Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve) {
        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> responseQuerry) {
                if (responseQuerry.isLeft()) {
                    String error = responseQuerry.left().getValue();
                    log.error(error);
                    handler.handle(new Either.Left<>(error));
                } else {
                    JsonArray idClasseGroups = responseQuerry.right().getValue();
                    JsonArray idsGroups = new fr.wseduc.webutils.collections.JsonArray();
                    final String nameClasse, idClass;

                    if(typeGroupe != 0 || (idClasseGroups == null || idClasseGroups.isEmpty())) {
                        idsGroups.add(idClasse);
                        nameClasse = name;
                        idClass = idClasse;
                    } else {
                        idsGroups.add(idClasseGroups.getJsonObject(0).getString(Field.ID_CLASSE));
                        idsGroups.addAll(idClasseGroups.getJsonObject(0).getJsonArray("id_groupes"));
                        nameClasse = idClasseGroups.getJsonObject(0).getString("name_classe");
                        idClass = idClasseGroups.getJsonObject(0).getString(Field.ID_CLASSE);
                    }

                    //Récupération des Services
                    Promise<JsonArray> servicesPromise = Promise.promise();
                    utilsService.getServices(idEtablissement,
                            idsGroups, FutureHelper.handlerJsonArray(servicesPromise.future()));

                    //Récupération des Multi-teachers
                    Promise<JsonArray> multiTeachingPromise = Promise.promise();
                    utilsService.getMultiTeachers(idEtablissement,
                            idsGroups, idPeriode.intValue(), FutureHelper.handlerJsonArray(multiTeachingPromise.future()));

                    //Récupération des Sous-Matières
                    Future<List<SubTopic>> subTopicCoefFuture = utilsService.getSubTopicCoeff(idEtablissement, idsGroups);

                    CompositeFuture.all(servicesPromise.future(), multiTeachingPromise.future(), subTopicCoefFuture)
                            .setHandler(event -> {
                                Structure structure = new Structure();
                                structure.setId(idEtablissement);
                                JsonArray servicesJson = servicesPromise.future().result();
                                JsonArray multiTeachers = multiTeachingPromise.future().result();
                                List<SubTopic> subTopics = subTopicCoefFuture.result();

                                List<Service> services = new ArrayList<>();
                                List<MultiTeaching> multiTeachings = new ArrayList<>();
                                new DefaultExportBulletinService(eb, null).setMultiTeaching(structure, multiTeachers, multiTeachings, idClass);
                                setServices(structure, servicesJson, services, subTopics);


                                // 2- On récupère les notes des eleves
                                getNotesAndMoyFinaleByClasseAndPeriode(idsEleve, idsGroups, idPeriode, idEtablissement,
                                        getNotesAndMoyFinaleByClasseAndPeriodeHandler(nameClasse, idClass, handler, servicesJson,
                                                multiTeachers, mapAllidMatAndidTeachers, eleves, mapIdMatListMoyByEleve, services));
                            });
                }
            }
        };
    }

    //à tes souhaits
    private Handler<Either<String, JsonArray>> getNotesAndMoyFinaleByClasseAndPeriodeHandler(String nameClasse, String idClass,
                                                                                             Handler<Either<String, JsonObject>> handler,
                                                                                             JsonArray servicesJSON, JsonArray multiTeachers,
                                                                                             SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
                                                                                             List<Eleve> eleves,
                                                                                             Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve,
                                                                                             List<Service> services) {
        return response -> {
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
                    HashMap<String, HashMap<String, HashMap<Long, List<NoteDevoir>>>> mapIdEleveIdMatIdSousMatListNotes = new HashMap<>();

                    for (int i = 0; i < respNotesMoysFinales.size(); i++) {
                        JsonObject respNoteMoyFinale = respNotesMoysFinales.getJsonObject(i);
                        Double moyenneFinale = (respNoteMoyFinale.getString("moyenne_finale") != null ) ?
                                Double.valueOf(respNoteMoyFinale.getString("moyenne_finale")) : null;
                        String idEleveMoyF = respNoteMoyFinale.getString("id_eleve_moyf");
                        String idMatMoyF = respNoteMoyFinale.getString("id_mat_moyf");
                        String idEleveNotes = respNoteMoyFinale.getString("id_eleve_notes");
                        String idMatiere = respNoteMoyFinale.getString("id_matiere");

                        //récupérer les moysFinales => set mapIdEleveIdMatMoy
                        if (moyenneFinale != null ) {
                            if(!mapAllidMatAndidTeachers.containsKey(idMatMoyF))
                                setListTeachers(servicesJSON, multiTeachers,mapAllidMatAndidTeachers,idMatMoyF);
                            if(mapAllidMatAndidTeachers.containsKey(idMatMoyF)){//idMatMoyF is on service
                                setMapIdEleveMatMoy(mapIdEleveIdMatMoy, moyenneFinale, idEleveMoyF, idMatMoyF);
                            } else {
                                continue;//idMatMoyF is not on service
                            }
                        } else if(moyenneFinale == null) {//pas de moyFinale => set mapIdEleveIdMatListNotes
                            if (respNoteMoyFinale.getString("coefficient") == null || !respNoteMoyFinale.getBoolean("is_evaluated")){
                                continue;
                            }
                            if(idEleveNotes != null){
                                if(respNoteMoyFinale.getLong(Field.ID_SOUSMATIERE) != null){
                                    setMapIdEleveIdMatIdSousMatListNotes(mapIdEleveIdMatIdSousMatListNotes, respNoteMoyFinale, idEleveNotes, idMatiere, services, multiTeachers, idClass);
                                }
                                else {
                                    setMapIdEleveIdMatListNotes(mapIdEleveIdMatListNotes, respNoteMoyFinale, idEleveNotes, idMatiere);
                                }
                            }
                            if(!mapAllidMatAndidTeachers.containsKey(idMatiere)){
                                setListTeachers(servicesJSON, multiTeachers, mapAllidMatAndidTeachers, idMatiere);
                            }
                        }
                    }

                    //3 - calculate average by eleve by mat by sous mat with mapIdEleveIdMatIdSousMatListNotes and set result in mapIdEleveIdMatMoy
                    Boolean stats = false;
                    Boolean annual = false;
                    for (Map.Entry<String, HashMap<String, HashMap<Long, List<NoteDevoir>>>> eleveMapEntry : mapIdEleveIdMatIdSousMatListNotes.entrySet()) {
                        for (Map.Entry<String, HashMap<Long, List<NoteDevoir>>> matMapEntry : eleveMapEntry.getValue().entrySet()) {
                            double total = 0;
                            double totalCoeff = 0;
                            String idEleve = eleveMapEntry.getKey();
                            String idMat = matMapEntry.getKey();
                            for (Map.Entry<Long, List<NoteDevoir>> sousMatMapEntry : matMapEntry.getValue().entrySet()) {
                                Long idSousMat = sousMatMapEntry.getKey();
                                Service serv = sousMatMapEntry.getValue().get(0).getService();
                                double coeff = 1.d;

                                if (serv != null && serv.getSubtopics() != null && serv.getSubtopics().size() > 0) {
                                    SubTopic subTopic = serv.getSubtopics().stream()
                                            .filter(el ->
                                                    el.getId().equals(idSousMat)
                                            ).findFirst().orElse(null);
                                    if (subTopic != null)
                                        coeff = subTopic.getCoefficient();
                                }

                                Double moyenSousMat = utilsService.calculMoyenne(sousMatMapEntry.getValue(), stats, Field.DIVISEUR_NOTE, annual)
                                        .getDouble(Field.MOYENNE);

                                total += coeff * moyenSousMat;
                                totalCoeff += coeff;
                            }
                            if (totalCoeff != 0) {
                                Double moy = Math.round((total / totalCoeff) * Field.ROUNDER) / Field.ROUNDER;
                                setMapIdEleveMatMoy(mapIdEleveIdMatMoy, moy, idEleve, idMat);
                            }
                        }
                    }

                    //4 - calculate average by eleve by mat with mapIdEleveIdMatListNotes and set result in mapIdEleveIdMatMoy
                    for (Map.Entry<String, Map<String, List<NoteDevoir>>> stringMapEntry : mapIdEleveIdMatListNotes.entrySet()) {
                        for (Map.Entry<String, List<NoteDevoir>> stringListEntry : stringMapEntry.getValue().entrySet()) {
                            String idEleve = stringMapEntry.getKey();
                            String idMat = stringListEntry.getKey();
                            List<NoteDevoir> noteDevoirList = stringListEntry.getValue();
                            if (!Field.NN.equals(utilsService.calculMoyenne(noteDevoirList, stats, Field.DIVISEUR_NOTE, annual).getValue(Field.MOYENNE))) {
                                Double moy = utilsService.calculMoyenne(noteDevoirList, stats, Field.DIVISEUR_NOTE, annual).getDouble(Field.MOYENNE);
                                setMapIdEleveMatMoy(mapIdEleveIdMatMoy, moy, idEleve, idMat);
                            }
                        }
                    }

                    //5- il faut parcourir la mapIdMatIdsTeacher pour garder l'ordre des matieres pour tester qu l'élève à bien ttes les matières
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
                                    eleveMoyByMat.add(new JsonObject()
                                            .put("id_matiere", idMatOfAllMat)
                                            .put("moyenneByMat", mapIdMatMoy.get(idMatOfAllMat)));

                                    JsonObject service = (JsonObject) servicesJSON.stream()
                                            .filter(el -> idMatOfAllMat.equals(((JsonObject) el).getString("id_matiere")))
                                            .findFirst().orElse(null);
                                    Double coefficient = 1.0;
                                    if(service != null) {
                                        coefficient = service.getDouble("coefficient");
                                    }

                                    NoteDevoir noteDevoir = new NoteDevoir(mapIdMatMoy.get(idMatOfAllMat),
                                            20.0,false, coefficient);

                                    listMoysEleve.add(noteDevoir);

                                    if (mapIdMatListMoyByEleve.containsKey(idMatOfAllMat)) {
                                        mapIdMatListMoyByEleve.get(idMatOfAllMat).add(noteDevoir);
                                    } else {
                                        List<NoteDevoir> listMoyEleve = new ArrayList<>();
                                        listMoyEleve.add(noteDevoir);
                                        mapIdMatListMoyByEleve.put(idMatOfAllMat, listMoyEleve);
                                    }
                                } else {//sinon l'eleve n'a pas ete evalue pour cette matiere
                                    eleveMoyByMat.add(new JsonObject()
                                            .put("id_matiere", idMatOfAllMat)
                                            .put("moyenneByMat", "NN"));
                                }
                            }
                            eleveJsonO.put("eleveMoyByMat", eleveMoyByMat);

                            if(!"NN".equals(utilsService.calculMoyenne(listMoysEleve, false, 20, false).getValue("moyenne"))){
                                Double moyGeneraleEleve = utilsService.calculMoyenne(listMoysEleve,false, 20, false).getDouble("moyenne");
                                eleveJsonO.put("moyGeneraleEleve", moyGeneraleEleve);
                                //ajouter cette moyG a une liste de moyGeleve pour le calcul moyGClasse
                                listMoyGeneraleEleve.add(new NoteDevoir(moyGeneraleEleve, 20.0,false,1.0));
                            } else {
                                eleveJsonO.put("moyGeneraleEleve", "NN");
                            }

                        } else {//eleve n'a eu aucune evaluation sur aucune matiere dc pour toutes les matieres evaluees il aura NN
                            for (Map.Entry<String, Set<String>> setEntry : mapAllidMatAndidTeachers.entrySet()) {
                                String idMatOfAllMat = setEntry.getKey();

                                eleveMoyByMat.add(new JsonObject()
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
        };
    }

    private void setMapIdEleveMatMoy (Map<String, Map<String, Double>> mapIdEleveIdMatMoy, Double moyenneFinale,
                                      String idEleveMoyF, String idMatMoyF) {
        if (mapIdEleveIdMatMoy.containsKey(idEleveMoyF)) {
            Map<String, Double> mapIdMatMoy = mapIdEleveIdMatMoy.get(idEleveMoyF);
            // meme eleve changement de matiere
            if (!mapIdMatMoy.containsKey(idMatMoyF)) {
                mapIdMatMoy.put(idMatMoyF, moyenneFinale);
            }
        } else {//nouvel eleve
            Map<String, Double> newMapIdMatMoy = new HashMap<>();
            newMapIdMatMoy.put(idMatMoyF, moyenneFinale);
            mapIdEleveIdMatMoy.put(idEleveMoyF, newMapIdMatMoy);
        }
    }

    private void setMapIdEleveIdMatListNotes (Map<String, Map<String, List<NoteDevoir>>> mapIdEleveIdMatListNotes,
                                              JsonObject respNoteMoyFinale, String idEleveNotes, String idMatiere) {
        NoteDevoir noteDevoir = new NoteDevoir(
                Double.valueOf(respNoteMoyFinale.getString("valeur")),
                Double.valueOf(respNoteMoyFinale.getString(Field.DIVISEUR)),
                respNoteMoyFinale.getBoolean("ramener_sur"),
                Double.valueOf(respNoteMoyFinale.getString("coefficient")));

        if (mapIdEleveIdMatListNotes.containsKey(idEleveNotes)) {
            Map<String, List<NoteDevoir>> mapIdMatListNotes = mapIdEleveIdMatListNotes.get(idEleveNotes);
            if (mapIdMatListNotes.containsKey(idMatiere)) {
                mapIdMatListNotes.get(idMatiere).add(noteDevoir);
            } else {//nouvelle matière dc nouvelle liste de notes
                List<NoteDevoir> newListNotes = new ArrayList<>();
                newListNotes.add(noteDevoir);
                mapIdMatListNotes.put(idMatiere, newListNotes);
            }
        } else {//nouvel élève dc nelle map idMat-listnotes
            Map<String, List<NoteDevoir>> newMapIdMatListNotes = new HashMap<>();
            List<NoteDevoir> newListNotes = new ArrayList<>();
            newListNotes.add(noteDevoir);
            newMapIdMatListNotes.put(idMatiere, newListNotes);
            mapIdEleveIdMatListNotes.put(idEleveNotes, newMapIdMatListNotes);
        }
    }

    private void setMapIdEleveIdMatIdSousMatListNotes (HashMap<String, HashMap<String, HashMap<Long, List<NoteDevoir>>>> mapIdEleveIdMatIdSousMatListNotes,
                                                       JsonObject respNoteMoyFinale, String idEleveNotes, String idMatiere,
                                                       List<Service> services, JsonArray multiTeachers, String idClasse) {

        Matiere matiere = new Matiere(respNoteMoyFinale.getString(Field.ID_MATIERE));
        Teacher teacher = new Teacher(respNoteMoyFinale.getString(Field.OWNER));
        Group group = new Group(respNoteMoyFinale.getString(Field.ID_GROUPE));

        Service service = services.stream()
                .filter(el -> teacher.getId().equals(el.getTeacher().getId())
                        && matiere.getId().equals(el.getMatiere().getId())
                        && group.getId().equals(el.getGroup().getId()))
                .findFirst().orElse(null);

        if (service == null){
            //On regarde les multiTeacher
            for(Object mutliTeachO: multiTeachers){
                //multiTeaching.getString("second_teacher_id").equals(teacher.getId()
                JsonObject multiTeaching  =(JsonObject) mutliTeachO;
                if(multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(teacher.getId())
                        && multiTeaching.getString(Field.ID_CLASSE).equals(group.getId())
                        && multiTeaching.getString(Field.SUBJECT_ID).equals(matiere.getId())){
                    service = services.stream()
                            .filter(el -> el.getTeacher().getId().equals(multiTeaching.getString(Field.SECOND_TEACHER_ID))
                                    && matiere.getId().equals(el.getMatiere().getId())
                                    && group.getId().equals(el.getGroup().getId()))
                            .findFirst().orElse(null);
                }

                if(multiTeaching.getString(Field.SECOND_TEACHER_ID).equals(teacher.getId())
                        && multiTeaching.getString(Field.CLASS_OR_GROUP_ID).equals(group.getId())
                        && multiTeaching.getString(Field.SUBJECT_ID).equals(matiere.getId())){

                    service = services.stream()
                            .filter(el -> multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(el.getTeacher().getId())
                                    && matiere.getId().equals(el.getMatiere().getId())
                                    && group.getId().equals(el.getGroup().getId()))
                            .findFirst().orElse(null);
                }
            }
        }

        Long sousMatiereId = respNoteMoyFinale.getLong(Field.ID_SOUSMATIERE);
        Long id_periode = respNoteMoyFinale.getLong(Field.ID_PERIODE);

        NoteDevoir noteDevoir = new NoteDevoir(
                Double.valueOf(respNoteMoyFinale.getString(Field.VALEUR)),
                Double.valueOf(respNoteMoyFinale.getString(Field.DIVISEUR)),
                respNoteMoyFinale.getBoolean(Field.RAMENER_SUR),
                Double.valueOf(respNoteMoyFinale.getString(Field.COEFFICIENT)),
                respNoteMoyFinale.getString(Field.ID_ELEVE), id_periode, service, sousMatiereId);

        if (mapIdEleveIdMatIdSousMatListNotes.containsKey(idEleveNotes)) {
            Map<String, HashMap<Long, List<NoteDevoir>>> mapIdMatIdSousMatListNotes = mapIdEleveIdMatIdSousMatListNotes.get(idEleveNotes);
            if (mapIdMatIdSousMatListNotes.containsKey(idMatiere)) {
                HashMap<Long, List<NoteDevoir>> mapIdSousMatListNotes = mapIdMatIdSousMatListNotes.get(idMatiere);
                if (mapIdSousMatListNotes.containsKey(sousMatiereId)){
                    mapIdSousMatListNotes.get(sousMatiereId).add(noteDevoir);
                } else {//nouvelle sous-matière dc nouvelle liste de notes
                    List<NoteDevoir> newListNotes = new ArrayList<>();
                    newListNotes.add(noteDevoir);
                    mapIdSousMatListNotes.put(sousMatiereId, newListNotes);
                }
            } else {//nouvelle matière dc nouvelle liste de notes
                HashMap<Long, List<NoteDevoir>> newListSousMatiereNotes = new HashMap<>();
                List<NoteDevoir> newListNotes = new ArrayList<>();
                newListNotes.add(noteDevoir);
                newListSousMatiereNotes.put(sousMatiereId, newListNotes);
                mapIdMatIdSousMatListNotes.put(idMatiere, newListSousMatiereNotes);
            }
        } else {//nouvel élève dc nelle map idMat-listnotes
            HashMap<String, HashMap<Long, List<NoteDevoir>>> newMapIdMatIdSousMatListNotes = new HashMap<>();
            HashMap<Long, List<NoteDevoir>> newListSousMatiereNotes = new HashMap<>();
            List<NoteDevoir> newListNotes = new ArrayList<>();
            newListNotes.add(noteDevoir);
            newListSousMatiereNotes.put(sousMatiereId, newListNotes);
            newMapIdMatIdSousMatListNotes.put(idMatiere, newListSousMatiereNotes);
            mapIdEleveIdMatIdSousMatListNotes.put(idEleveNotes, newMapIdMatIdSousMatListNotes);
        }
    }

    private void setListTeachers (JsonArray services, JsonArray multiTeachers,
                                  SortedMap<String, Set<String>> mapAllidMatAndidTeachers, String idMatiere) {
        Set<String> listIdsTeacher = new HashSet();

        JsonObject service = (JsonObject) services.stream()
                .filter(el -> idMatiere.equals(((JsonObject) el).getString("id_matiere")))
                .findFirst().orElse(null);

        if(service != null) {
            if(service.getBoolean("is_visible") )listIdsTeacher.add(service.getString("id_enseignant"));
            multiTeachers.forEach(item -> {
                JsonObject teacher = (JsonObject) item;

                String subjectId = teacher.getString(Field.SUBJECT_ID);
                String coTeacherId = teacher.getString(Field.SECOND_TEACHER_ID);

                if (subjectId.equals(idMatiere)) {
                    listIdsTeacher.add(coTeacherId);
                }
            });
            mapAllidMatAndidTeachers.put(idMatiere, listIdsTeacher);
        }
    }

    @Override
    public void getMoysEleveByMatByYear(String idEtablissement, JsonArray periodes, Integer typeGroupe, String name,
                                        SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
                                        Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve,
                                        Handler<Either<String, JsonObject>> handler) {
        Eleves eleves = new Eleves();
        List<Future> futureListPeriodes = new ArrayList<>();
        for(int i = 0; i < periodes.size(); i++){
            Future<JsonObject> futurePeriode = Future.future();
            futureListPeriodes.add(futurePeriode);
            JsonObject periode = periodes.getJsonObject(i);
            Map<String, List<NoteDevoir>> mapIdMatListMoyByEleveByPeriode = new LinkedHashMap<>();
            getMoysEleveByMatByPeriode(periode.getString(Field.ID_CLASSE), periode.getInteger("id_type"), idEtablissement,
                    typeGroupe, name, mapAllidMatAndidTeachers, mapIdMatListMoyByEleveByPeriode,
                    new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> event) {
                            if(event.isLeft()){
                                futurePeriode.fail(event.left().getValue());
                                handler.handle(new Either.Left<>( event.left().getValue()));
                                log.error(event.left().getValue());

                            }else{
                                JsonObject resultElevePeriode = event.right().getValue();
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
            } else {
                JsonObject result = new JsonObject();
                List<NoteDevoir> moysElevesByYear = new ArrayList<NoteDevoir>();

                JsonArray elevesJA = eleves.buildJsonArrayEleves(mapIdMatListMoyByEleve, mapAllidMatAndidTeachers,
                        moysElevesByYear);
                setJOResultWithList(result, moysElevesByYear);

                result.put("eleves", elevesJA);
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
        } else {
            JsonObject resultCalculMoy =  utilsService.calculMoyenne(notesList, true,null,false);
            resultJO.put("moyClassAllEleves", resultCalculMoy.getDouble("moyenne"));
            resultJO.put("moyMinClass", resultCalculMoy.getDouble("noteMin"));
            resultJO.put("moyMaxClass", resultCalculMoy.getDouble("noteMax"));
        }
    }

    @Override
    public void getMatEvaluatedAndStat(SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
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
                        if(mapIdMatListMoyByEleve.containsKey(idMatAllMats)) {
                            JsonObject statClass = utilsService.calculMoyenneParDiviseur(
                                    mapIdMatListMoyByEleve.get(idMatAllMats),true);
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
                    resultMatieres.put("matieres", Utils.sortJsonArrayIntValue("rank", matieresResult));
                    resultMatieres.put("nbDeMatieres", matieresResult.size());

                    handler.handle(new Either.Right<>(resultMatieres));
                }
            }
        });
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
        JsonArray students = resultFinal.getJsonArray(Field.ELEVES);
        putLibelleForExport(resultFinal);
        putParamsForExport(resultFinal, params);
        resultFinal.put(CLASSE_NAME_KEY, params.getString(CLASSE_NAME_KEY));
        resultFinal.put(Field.MATIERE_TABLE, params.getString(Field.MATIERE_TABLE));
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

        final String idEtablissement = params.getString(ID_ETABLISSEMENT_KEY);
        final String idClasse = params.getString(ID_CLASSE_KEY);
        final String idMatiere = params.getString(ID_MATIERE_KEY);
        String idEleve = params.getString(ID_ELEVE_KEY);
        final Long idPeriode = params.getLong(ID_PERIODE_KEY);
        final Integer typeClasse = params.getInteger(TYPE_CLASSE_KEY);
        final JsonArray idEleves = new fr.wseduc.webutils.collections.JsonArray();
        final JsonObject resultHandler = new JsonObject();
        final Boolean withPreviousAppreciations = (params.containsKey(PREVIOUSAPPRECIATIONS)) ?
                params.getBoolean(PREVIOUSAPPRECIATIONS) : Boolean.FALSE;
        Map<String, JsonObject> elevesMapObject = new HashMap<>();
        Boolean isExport = (params.getString("fileType") != null);
        List<Future> futures = new ArrayList<>();
        // Récupération des éléments du programme
        Future<JsonObject> elementProgrammeFuture =
                new DefaultElementProgramme().getElementProgramme(idPeriode, idMatiere, idClasse);
        futures.add(elementProgrammeFuture);
        // Récupération des élèves de la classe
        Future<JsonArray> studentsClassFuture;
        if (idEleve == null) {
            studentsClassFuture = getStudentClassForExportReleve(idClasse, idPeriode, idEleves, typeClasse, elevesMapObject);
        } else {
            studentsClassFuture = Future.succeededFuture(new JsonArray().add(idEleve));
        }
        futures.add(studentsClassFuture);

        // Récupération du  nombre de devoirs avec évaluation numérique
        Future<JsonObject> nbEvaluatedHomeWork = getNbEvaluatedHomeWork(idClasse, idMatiere, idPeriode, null);
        futures.add(nbEvaluatedHomeWork);

        // Récupération du tableau de conversion
        // On récupère le tableau de conversion des compétences notes pour Lire le positionnement
        Future<JsonArray> tableauDeConversionFuture = competenceNoteService.getConversionNoteCompetence(idEtablissement, idClasse);
        futures.add(tableauDeConversionFuture);

        // Récupération de l'appréciation de la classe
        Future<JsonArray> appreciationClassFuture;
        if (idPeriode != null) {
            appreciationClassFuture = new DefaultAppreciationService(COMPETENCES_SCHEMA, Field.APPRECIATIONS_TABLE)
                    .getAppreciationClass(new String[]{idClasse}, idPeriode.intValue(), new String[]{idMatiere});
        } else {
            appreciationClassFuture = Future.succeededFuture(new JsonArray());
        }
        futures.add(appreciationClassFuture);

        // Récupération des sousMatieres
        Future<JsonArray> sousMatiereFuture = new DefaultMatiereService().getUnderSubjects(idMatiere, idEtablissement);
        futures.add(sousMatiereFuture);

        // Avec les ids des élèves de la classe, récupération des moyennes Finales , des Notes, des Competences Notes
        // et des Appreciations et des Positionnements finaux//
        CompositeFuture.all(futures).onComplete( idElevesEvent -> {
            if(idElevesEvent.succeeded()) {
                putParamSousMatiere(sousMatiereFuture.result(), params);
                resultHandler.put(Field.TABLECONVERSIONS, tableauDeConversionFuture.result());

                resultHandler.put(ELEMENTPROGRAMME, elementProgrammeFuture.result());
                JsonObject appClasse = utilsService.getObjectForPeriode( appreciationClassFuture.result(), idPeriode, Field.ID_PERIODE);
                if(appClasse == null) {
                    appClasse = new JsonObject().put("appreciation", " ");
                }
                resultHandler.put(APPRECIATION_CLASSE, appClasse);

                // Récupération des moyennes Finales
                Future<JsonArray> moyennesFinalesFutures =  getColumnReleve(idEleves, idPeriode, idMatiere,
                        new JsonArray().add(idClasse), Field.MOYENNE, withPreviousAppreciations);
                Future<JsonArray> appreciationsFutures;
                Future<JsonArray> positionnementsFinauxFutures;
                if (idPeriode != null) {

                    appreciationsFutures =getColumnReleve(idEleves, idPeriode , idMatiere,
                            new JsonArray().add(idClasse), APPRECIATION_MATIERE_PERIODE, withPreviousAppreciations);

                    positionnementsFinauxFutures =getColumnReleve(idEleves, idPeriode, idMatiere,
                            new JsonArray().add(idClasse), Field.POSITIONNEMENT, withPreviousAppreciations);
                } else {
                    appreciationsFutures = Future.succeededFuture(new JsonArray());
                    positionnementsFinauxFutures = Future.succeededFuture(new JsonArray());
                }

                // Récupération des Notes du Relevé
                Future<JsonArray> notesFuture;
                Boolean hasEvaluatedHomeWork = (nbEvaluatedHomeWork.result().getLong("nb") > 0);
                if(idEleve == null && hasEvaluatedHomeWork) {
                    notesFuture = getNotesReleveStudents(idEleves, idEtablissement, idClasse, idPeriode, false,
                            null, idMatiere != null ? new JsonArray().add(idMatiere) : null);
                } else {
                    if (!hasEvaluatedHomeWork) {
                        notesFuture = Future.succeededFuture(new JsonArray());
                    } else {
                        if (idPeriode != null) {
                            notesFuture = getNoteStudentPeriod(idEleve, idEtablissement, new JsonArray().add(idClasse),
                                    idMatiere, idPeriode);
                        } else {
                            notesFuture = getNotesReleve(idEtablissement, idClasse, idMatiere,null, typeClasse,
                                    false,null);
                        }
                    }
                }

                // Récupération des Compétences-Notes du Relevé
                Future<JsonArray> compNotesFuture = getCompetencesNotesReleveStudents(idEleves, idEtablissement, idMatiere,
                        null, idPeriode,null, true, false);
                //Récupération des Multi-teachers
                Future<JsonArray> multiTeachingFuture =
                        utilsService.getAllMultiTeachers(idEtablissement,new JsonArray().add(idClasse));
                //Récupération des Services
                Promise<Object> servicesPromise = Promise.promise();
                utilsService.getServices(idEtablissement,
                        new JsonArray().add(idClasse), FutureHelper.handlerJsonArray(servicesPromise));

                Future<List<SubTopic>> subTopicCoefFuture = utilsService.getSubTopicCoeff(idEtablissement, idClasse);

                List<Future> listFutures = new ArrayList<>(Arrays.asList(compNotesFuture, notesFuture,
                        moyennesFinalesFutures, appreciationsFutures, positionnementsFinauxFutures,
                        servicesPromise.future(), multiTeachingFuture, subTopicCoefFuture));

                Future<Boolean> isAvgSkillFuture = structureOptionsService.isAverageSkills(idEtablissement);
                listFutures.add(isAvgSkillFuture);
                CompositeFuture.all(listFutures).onComplete( event -> {
                    if(event.succeeded()) {
                        // Rajout des moyennes finales
                        FormateColonneFinaleReleve(moyennesFinalesFutures.result(), elevesMapObject,
                                Field.MOYENNE, idPeriode, hasEvaluatedHomeWork, withPreviousAppreciations);

                        Structure structure = new Structure();
                        structure.setId(idEtablissement);
                        JsonArray servicesJson = (JsonArray) servicesPromise.future().result();
                        JsonArray multiTeachers = multiTeachingFuture.result();
                        List<SubTopic> subTopics = subTopicCoefFuture.result();

                        List<Service> services = new ArrayList<>();
                        List<MultiTeaching> multiTeachings = new ArrayList<>();
                        new DefaultExportBulletinService(eb, null).setMultiTeaching(structure, multiTeachers, multiTeachings, idClasse);
                        setServices(structure, servicesJson, services, subTopics);

                        // Rajout des notes par devoir et Calcul des moyennes auto
                        resultHandler.put(NOTES, notesFuture.result());

                        calculMoyennesNotesForReleve(notesFuture.result(), resultHandler, idPeriode,
                                elevesMapObject, hasEvaluatedHomeWork, isExport, false, null, idClasse,
                                services, multiTeachers);

                        resultHandler.put(COMPETENCES_NOTES_KEY, compNotesFuture.result());
                        if (idPeriode != null) {
                            // Cacul du positionnement auto
                            calculMoyennesCompetencesNotesForReleve(compNotesFuture.result(), resultHandler,
                                    idPeriode, tableauDeConversionFuture.result(), elevesMapObject, isAvgSkillFuture.result());

                            // Format sous matières moyennes
                            addSousMatieres(elevesMapObject, sousMatiereFuture.result(), resultHandler, idPeriode);

                            // Rajout des positionnements finaux
                            FormateColonneFinaleReleve(positionnementsFinauxFutures.result(), elevesMapObject,
                                    Field.POSITIONNEMENT, idPeriode, hasEvaluatedHomeWork, withPreviousAppreciations);

                           if (withPreviousAppreciations) {
                               JsonArray appreciationsSelectedPeriod =
                                       getAppreciationSelectedPeriod(appreciationsFutures.result(),idPeriode);
                               resultHandler.put(APPRECIATIONS, appreciationsSelectedPeriod);
                           } else {
                               resultHandler.put(APPRECIATIONS, appreciationsFutures.result());
                           }
                            FormateColonneFinaleReleve(appreciationsFutures.result(), elevesMapObject,
                                    APPRECIATION_MATIERE_PERIODE, idPeriode, hasEvaluatedHomeWork, withPreviousAppreciations);

                        }
                        handler.handle(new Either.Right<>(resultHandler.put(Field.ELEVES,
                                new DefaultExportBulletinService(eb, null).sortResultByClasseNameAndNameForBulletin(elevesMapObject))));
                    } else {
                        handler.handle(new Either.Left<>(event.cause().getMessage()));
                    }
                });
            } else {
                handler.handle(new Either.Left<>(idElevesEvent.cause().getMessage()));
            }
        });
    }

    private JsonArray getAppreciationSelectedPeriod (JsonArray appreciations, Long idPeriod) {
        List<JsonObject> fileredAppreciations = new ArrayList<>();
        if (appreciations != null) {
            List<JsonObject> appreciationsList = appreciations.getList();
            fileredAppreciations = appreciationsList.stream().filter( app ->
                  idPeriod.equals(app.getLong(Field.ID_PERIODE))
            ).collect(Collectors.toList());
        }
        return new JsonArray(fileredAppreciations);
    }

    public Future<JsonObject> getDatasReleve (final JsonObject param) {
        Promise<JsonObject> promiseDataReleve = Promise.promise();
        getDatasReleve(param, FutureHelper.handlerJsonObject(promiseDataReleve,
                String.format ("[Competences@%s::getDatasReleve] error to get data for periodic transcript", this.getClass().getSimpleName())));
        return promiseDataReleve.future();
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
            //final JsonArray idGroups = {params.getJsonArray("idGroups").size() == 0 ? null : params.getJsonArray("idGroups")};

            new DefaultDevoirService(eb).getEleveGroups(idClasse, groupsEvent -> {
                if(groupsEvent.isLeft()){
                    handler.handle(new Either.Left<>(groupsEvent.left().getValue()));
                } else {
                    JsonArray idGroups = new JsonArray().add(idClasse);
                    JsonArray listElevesGroups = groupsEvent.right().getValue();
                    for(Object eleve : listElevesGroups) {
                        for(Object group : ((JsonObject) eleve).getJsonArray("id_groupes")) {
                            if(!idGroups.contains(group))
                                idGroups.add(group);
                        }
                    }

                    Map<String, JsonObject> elevesMapObject = new HashMap<>();

                    // Récupération des élèves de la classe
                    Future<JsonArray> studentsClassFuture =
                    getStudentClassForExportReleve(idClasse, idPeriode, idEleves, typeClasse, elevesMapObject);

                    // Récupération du tableau de conversion
                    Future<JsonArray> tableauDeConversionFuture = Future.future();
                    competenceNoteService.getConversionNoteCompetence(idEtablissement, idClasse, tableauEvent ->
                            formate(tableauDeConversionFuture, tableauEvent));

                    Future<JsonArray> servicesFuture = Future.future();
                    utilsService.getServices(idEtablissement, idGroups, event -> {
                        formate(servicesFuture, event);
                    });

                    //Récupération des Services
                    Promise<JsonArray> servicesPromise = Promise.promise();
                    utilsService.getServices(idEtablissement,
                            idGroups, FutureHelper.handlerJsonArray(servicesPromise.future()));

                    //Récupération des Multi-teachers
                    Promise<JsonArray> multiTeachingPromise = Promise.promise();
                    utilsService.getMultiTeachers(idEtablissement,
                            idGroups, idPeriode.intValue(), FutureHelper.handlerJsonArray(multiTeachingPromise.future()));

                    //Récupération des Sous-Matières
                    Future<List<SubTopic>> subTopicCoefFuture = utilsService.getSubTopicCoeff(idEtablissement, idGroups);

                    List<Future> listFuturesFirst = new ArrayList<>(
                            Arrays.asList(studentsClassFuture, tableauDeConversionFuture, servicesFuture,
                                    servicesPromise.future(), multiTeachingPromise.future(), subTopicCoefFuture));
                    for (Object idMatiere : idMatieres){
                        // Récupération du nombre de devoirs avec évaluation numérique
                        Future<JsonObject> nbEvaluatedHomeWork = Future.future();
                        getNbEvaluatedHomeWork(idClasse, idMatiere.toString(), idPeriode, idGroups, event ->
                                formate(nbEvaluatedHomeWork, event)
                        );
                        listFuturesFirst.add(nbEvaluatedHomeWork);
                    }

                    // Avec les ids des élèves de la classe, récupération des moyennes Finales , des Notes, des Competences Notes
                    // et des Appreciations et des Positionnements finaux
                    CompositeFuture.all(listFuturesFirst).setHandler(idElevesEvent -> {
//                        try{
                        if(idElevesEvent.succeeded()) {
                            //Récupération des services, multiteachers et subtopics afin de calculer la moyenne en prenant en compte le coefficient des sous-matières
                            Structure structure = new Structure();
                            structure.setId(idEtablissement);
                            JsonArray servicesJson = servicesPromise.future().result();
                            JsonArray multiTeachers = multiTeachingPromise.future().result();
                            List<SubTopic> subTopics = subTopicCoefFuture.result();

                            List<Service> services = new ArrayList<>();
                            List<MultiTeaching> multiTeachings = new ArrayList<>();
                            new DefaultExportBulletinService(eb, null).setMultiTeaching(structure, multiTeachers, multiTeachings, idClasse);
                            setServices(structure, servicesJson, services, subTopics);

                            // Récupération des moyennes, positionnement Finales, appréciations, avis conseil de classe et orientation
                            Future<JsonArray> bigRequestFuture = Future.future();
                            getColonneReleveTotale(idEleves, idPeriode, idMatieres, new JsonArray().add(idClasse),
                                    idEtablissement, event -> formate(bigRequestFuture, event));

                            // Récupération des Notes du Relevé
                            Future<JsonArray> notesFuture = Future.future();
                            getNotesReleveEleves(idEleves, idEtablissement, idClasse, idPeriode, false,
                                    idGroups, idMatieres, notesEvent -> formate(notesFuture, notesEvent));

                            // Récupération des Compétences-Notes du Relevé
                            Future<JsonArray> compNotesFuture = Future.future();
                            getCompetencesNotesReleveEleves(idEleves, idEtablissement, null, idMatieres, idPeriode,
                                    null, true, annual,
                                    compNotesEvent -> formate(compNotesFuture, compNotesEvent));


                            Future<Boolean> isAvgSkillFuture = structureOptionsService.isAverageSkills(idEtablissement);
                            List<Future> listFutures = new ArrayList<>(Arrays.asList(bigRequestFuture, compNotesFuture, notesFuture));
                            listFutures.add(isAvgSkillFuture);
                            CompositeFuture.all(listFutures).setHandler(event -> {
                                try{
                                    if(event.succeeded()) {
                                        // Rajout des moyennes finales
                                        for (int i = NB_FUTURES; i < idMatieres.size() + NB_FUTURES; i++){
                                            String idMatiere = idMatieres.getString(i - NB_FUTURES);
                                            // Récupération du  nombre de devoirs avec évaluation numérique
                                            Boolean hasEvaluatedHomeWork = (((JsonObject) listFuturesFirst.get(i).result())
                                                    .getLong("nb") > 0);

                                            JsonArray notesMatiere = new JsonArray();

                                            for(Object note : notesFuture.result()){
                                                if(((JsonObject) note).getString("id_matiere").equals(idMatiere)){
                                                    notesMatiere.add(note);
                                                }
                                            }

                                            if(data.containsKey(NOTES)){
                                                data.getJsonObject(NOTES).put(idMatiere, notesMatiere);
                                            }else{
                                                JsonObject jsonNotesToAdd = new JsonObject();
                                                jsonNotesToAdd.put(idMatiere, notesMatiere);
                                                data.put(NOTES, jsonNotesToAdd);
                                            }

                                            JsonObject resultNotes = new JsonObject();

                                            calculMoyennesNotesForReleve(notesMatiere, resultNotes, idPeriode,
                                                    elevesMapObject, hasEvaluatedHomeWork,false, annual, idMatiere, idClasse, services, multiTeachers);
                                            if(data.containsKey(Field.MOYENNE)){
                                                data.getJsonObject(Field.MOYENNE).put(idMatiere, resultNotes);
                                            }else{
                                                JsonObject jsonToAdd = new JsonObject();
                                                jsonToAdd.put(idMatiere, resultNotes);
                                                data.put(Field.MOYENNE, jsonToAdd);
                                            }
                                        }

                                        for (Object idMatiere : idMatieres){
                                            JsonArray notesMatiere = new JsonArray();

                                            for(Object note : compNotesFuture.result()){
                                                if(((JsonObject)note).getString("id_matiere").equals(idMatiere.toString())){
                                                    notesMatiere.add(note);
                                                }
                                            }
                                            if(data.containsKey(COMPETENCES_NOTES_KEY)){
                                                data.getJsonObject(COMPETENCES_NOTES_KEY).put(idMatiere.toString(), notesMatiere);
                                            }else{
                                                JsonObject jsonToAdd = new JsonObject();
                                                jsonToAdd.put(idMatiere.toString(), notesMatiere);
                                                data.put(COMPETENCES_NOTES_KEY, jsonToAdd);
                                            }

                                            Map<String, JsonArray> notesByEleve = groupeNotesByStudent(notesMatiere);

                                            for (Map.Entry<String, JsonArray> entry : notesByEleve.entrySet()) {
                                                String idEleve = entry.getKey();
                                                JsonArray compNotesEleve = entry.getValue();

                                                if(elevesMapObject.containsKey(idEleve) && idEleve != null) {
                                                    JsonObject eleveObject = elevesMapObject.get(idEleve);
                                                    if(eleveObject.containsKey(COMPETENCES_NOTES_KEY)){
                                                        if(eleveObject.getJsonObject(COMPETENCES_NOTES_KEY).getValue(idMatiere.toString()) == null) {
                                                            eleveObject.getJsonObject(COMPETENCES_NOTES_KEY).put(idMatiere.toString(), compNotesEleve);
                                                        }
                                                    }else{
                                                        JsonObject jsonToAdd = new JsonObject();
                                                        jsonToAdd.put(idMatiere.toString(),compNotesEleve);
                                                        eleveObject.put(COMPETENCES_NOTES_KEY, jsonToAdd);
                                                    }
                                                    JsonObject resultNotes = new JsonObject();
                                                    calculPositionnementAutoByEleveByMatiere(compNotesEleve, resultNotes,
                                                            annual, tableauDeConversionFuture.result(),
                                                            null , null, isAvgSkillFuture.result());
                                                    if(eleveObject.containsKey(POSITIONNEMENT_AUTO)){
                                                        eleveObject.getJsonObject(POSITIONNEMENT_AUTO).put(idMatiere.toString(),
                                                                resultNotes.getJsonArray(POSITIONNEMENTS_AUTO));
                                                    }else{
                                                        JsonObject jsonToAdd = new JsonObject();
                                                        jsonToAdd.put(idMatiere.toString(), resultNotes.getJsonArray(POSITIONNEMENTS_AUTO));
                                                        eleveObject.put(POSITIONNEMENT_AUTO, jsonToAdd);
                                                    }
                                                    JsonObject positionnement = utilsService.getObjectForPeriode(
                                                            eleveObject.getJsonObject(POSITIONNEMENT_AUTO).getJsonArray(idMatiere.toString()),
                                                            idPeriode, Field.ID_PERIODE);
                                                    String positionnement_auto  = "";
                                                    if (positionnement != null) {
                                                        positionnement_auto = positionnement.getFloat(Field.MOYENNE).toString();
                                                    }
                                                    if( eleveObject.containsKey(Field.POSITIONNEMENT)){
                                                        eleveObject.getJsonObject(Field.POSITIONNEMENT).put(idMatiere.toString(),positionnement_auto);
                                                    }else{
                                                        JsonObject jsonToAdd = new JsonObject();
                                                        jsonToAdd.put(idMatiere.toString(),positionnement_auto);
                                                        eleveObject.put(Field.POSITIONNEMENT, jsonToAdd);
                                                    }
                                                }
                                            }
                                        }

                                        for (int i = 6; i < idMatieres.size() + 6; i++){
                                            String idMatiere = idMatieres.getString(i - 6);
                                            // Récupération du  nombre de devoirs avec évaluation numérique
                                            Boolean hasEvaluatedHomeWork = (((JsonObject)listFuturesFirst.get(i).result()).getLong("nb") > 0);
                                            FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                                    Field.MOYENNE, idPeriode, hasEvaluatedHomeWork, idMatiere);
                                            //Rajout des notes par devoir et Calcul des moyennes auto
                                            //Rajout des positionnements finaux
                                            FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                                    Field.POSITIONNEMENT, idPeriode, hasEvaluatedHomeWork, idMatiere);

                                            getMoyenneMinMaxByMatiere(elevesMapObject, idPeriode, idMatiere, annual, resultHandler);
                                        }

                                        getMoyenneGeneraleMinMax(elevesMapObject, idPeriode, idMatieres,
                                                servicesFuture.result(), annual, resultHandler);
                                        //Rajout des appreciations par élèves
                                        FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                                SYNTHESE_BILAN_PERIODIQUE, idPeriode, false, "");
                                        FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                                AVIS_CONSEIL_DE_CLASSE, idPeriode, false, "");
                                        FormateColonneFinaleReleveTotale(bigRequestFuture.result(), elevesMapObject,
                                                AVIS_CONSEIL_ORIENTATION, idPeriode, false, "");

                                        handler.handle(new Either.Right<>(resultHandler.put(Field.ELEVES,
                                                new DefaultExportBulletinService(eb, null).sortResultByClasseNameAndNameForBulletin(elevesMapObject))));
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
                        } else {
                            handler.handle(new Either.Left<>(idElevesEvent.cause().getMessage()));
                            log.error("getTotaleDatasReleve (idElevesEvent.failed()): " +
                                    idElevesEvent.cause());
                        }
                    });
                }
            });
        } catch (Exception error) {
            log.error("getTotaleDatasReleve (prepare data) failed: " + error);
            handler.handle(new Either.Left<>("getTotaleDatasReleve (prepare data) failed : " + error));
        }
    }

    private <T> void calculMoyennesNotesForReleve(JsonArray listNotes, JsonObject result, Long idPeriode,
                                                  Map<String, JsonObject> eleveMapObject,  Boolean hasEvaluatedHomeWork,
                                                  Boolean isExport, Boolean annual, String idMatiere, String idClasse,
                                                  List<Service> services, JsonArray multiTeachers) {
        // Si pour il n'y a pas de devoirs avec évaluation numérique, la moyenne auto est NN
        if (!hasEvaluatedHomeWork) {
            if (idMatiere == null) {
                List<NoteDevoir> listMoyF = new ArrayList<>();
                for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                    if (idPeriode != null) {
                        student.getValue().put(Field.MOYENNE, Field.NN);
                        if (student.getValue().getString("moyenneFinale") != null && !"NN".equals(student.getValue().getString("moyenneFinale"))) {

                            NoteDevoir moyF = new NoteDevoir(Double.valueOf(student.getValue().getString("moyenneFinale")),
                                    false, 1.0);
                            listMoyF.add(moyF);
                        }
                    }
                }

                if (idPeriode != null) {
                    JsonObject o_statClasseF = new JsonObject();
                    if (listMoyF.isEmpty()) {
                        o_statClasseF.put("min", Field.NN)
                                .put("max", Field.NN)
                                .put("moyenne", Field.NN);
                    } else {
                        JsonObject statMoyF = utilsService.calculMoyenneParDiviseur(listMoyF, true);
                        o_statClasseF.put("min", statMoyF.getValue("noteMin"))
                                .put("max", statMoyF.getValue("noteMax"))
                                .put("moyenne", statMoyF.getValue("moyenne"));
                    }
                    result.put("_moyenne_classe", new JsonObject().put("nullFinal", o_statClasseF));
                    JsonObject o_statClasse = new JsonObject()
                            .put("min", Field.NN)
                            .put("max", Field.NN)
                            .put("moyenne", Field.NN);
                    result.getJsonObject("_moyenne_classe").put("null", o_statClasse);
                }
            } else {
                for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                    if (student.getValue().containsKey(Field.MOYENNE)) {
                        if (student.getValue().getValue(Field.MOYENNE).getClass() == Double.class) {
                            student.getValue().remove(Field.MOYENNE);
                            JsonObject jsonToAdd = new JsonObject();
                            jsonToAdd.put(idMatiere, Field.NN);
                            student.getValue().put(Field.MOYENNE, jsonToAdd);
                        } else {
                            student.getValue().getJsonObject(Field.MOYENNE).put(idMatiere, Field.NN);
                        }
                    } else {
                        JsonObject jsonToAdd = new JsonObject();
                        jsonToAdd.put(idMatiere, Field.NN);
                        student.getValue().put(Field.MOYENNE, jsonToAdd);
                    }
                }
            }
        } else {
            JsonArray listMoyDevoirs = new fr.wseduc.webutils.collections.JsonArray();
            JsonArray listMoyEleves = new fr.wseduc.webutils.collections.JsonArray();
            HashMap<Long, ArrayList<NoteDevoir>> notesByDevoir = new HashMap<>();
            HashMap<String, T> notesByEleve = new HashMap<>();
            HashMap<String, HashMap<Long, ArrayList<NoteDevoir>>> notesByEleveBySousMatiere = new HashMap<>();

            Map<String, HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>>
                    notesByDevoirByPeriodeByEleve = new HashMap<>();
            Map<String, HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>>
                    notesByDevoirByPeriodeByEleveBySousMatiere = new HashMap<>();

            if (idPeriode == null) {
                result.put(MOYENNES, new JsonArray());
                result.put("_" + MOYENNES, new JsonArray());
            }

            String matiereId = idMatiere;
            Boolean hasSousMatieres = false;
            for (int i = 0; i < listNotes.size(); i++) {
                JsonObject note = listNotes.getJsonObject(i);
                if (note.getString(VALEUR) == null || note.getString(COEFFICIENT) == null ||
                        !note.getBoolean(IS_EVALUATED) || "0".equals(note.getString(COEFFICIENT)) ||
                        (note.getBoolean(Field.FORMATIVE) != null && note.getBoolean(Field.FORMATIVE))) {
                    continue;
                    //Si la note fait partie d'un devoir qui n'est pas évalué,
                    // elle n'est pas prise en compte dans le calcul de la moyenne
                }

                if (isNull(matiereId)) {
                    matiereId = listNotes.getJsonObject(i).getString(Field.ID_MATIERE);
                }

                Matiere matiere = new Matiere(matiereId);
                Teacher teacher = new Teacher(note.getString(Field.OWNER));
                Group group = new Group(note.getString(Field.ID_GROUPE));

                Service service = services.stream()
                        .filter(el -> teacher.getId().equals(el.getTeacher().getId())
                                && matiere.getId().equals(el.getMatiere().getId())
                                && group.getId().equals(el.getGroup().getId()))
                        .findFirst().orElse(null);

                if (service == null) {
                    //On regarde les multiTeacher
                    for (Object mutliTeachO : multiTeachers) {
                        JsonObject multiTeaching = (JsonObject) mutliTeachO;
                        if (multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(teacher.getId())
                                && multiTeaching.getString(Field.CLASS_OR_GROUP_ID).equals(group.getId())
                                && multiTeaching.getString(Field.SUBJECT_ID).equals(matiere.getId())) {
                            service = services.stream()
                                    .filter(el -> el.getTeacher().getId().equals(multiTeaching.getString(Field.SECOND_TEACHER_ID))
                                            && matiere.getId().equals(el.getMatiere().getId())
                                            && group.getId().equals(el.getGroup().getId()))
                                    .findFirst().orElse(null);
                        }

                        if (multiTeaching.getString(Field.SECOND_TEACHER_ID).equals(teacher.getId())
                                && multiTeaching.getString(Field.CLASS_OR_GROUP_ID).equals(group.getId())
                                && multiTeaching.getString(Field.SUBJECT_ID).equals(matiere.getId())) {

                            service = services.stream()
                                    .filter(el -> multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(el.getTeacher().getId())
                                            && matiere.getId().equals(el.getMatiere().getId())
                                            && group.getId().equals(el.getGroup().getId()))
                                    .findFirst().orElse(null);
                        }
                    }
                }

                Long sousMatiereId = listNotes.getJsonObject(i).getLong(ID_SOUS_MATIERE);
                Long idDevoir = note.getLong(ID_DEVOIR);
                String idEleve = note.getString(Field.ID_ELEVE);
                Long id_periode = note.getLong(Field.ID_PERIODE);
                NoteDevoir noteDevoir;
                noteDevoir = new NoteDevoir(
                        Double.valueOf(note.getString(VALEUR)),
                        Double.valueOf(note.getString(DIVISEUR)),
                        note.getBoolean(RAMENER_SUR),
                        Double.valueOf(note.getString(COEFFICIENT)), idEleve, id_periode, service, sousMatiereId);

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

                    if (note.getString(Field.ID_ELEVE).equals(idEleve)) {
                        utilsService.addToMap(id_periode,
                                notesByDevoirByPeriodeByEleve.get(idEleve).get(id_periode), noteDevoir);
                        utilsService.addToMap(null, notesByDevoirByPeriodeByEleve.get(idEleve).get(null),
                                noteDevoir);
                        if (isNotNull(sousMatiereId)) {
                            utilsService.addToMap(sousMatiereId,
                                    notesByDevoirByPeriodeByEleveBySousMatiere.get(idEleve).get(id_periode), noteDevoir);
                        }
                    }
                }
                utilsService.addToMap(idDevoir, notesByDevoir, noteDevoir);
                utilsService.addToMap(idEleve, (HashMap<String, ArrayList<NoteDevoir>>) notesByEleve, noteDevoir);
                if (isNotNull(sousMatiereId)) {
                    utilsService.addToMap(idEleve, sousMatiereId, notesByEleveBySousMatiere, noteDevoir);
                    hasSousMatieres = true;
                }
                utilsService.addToMap(idEleve, null, notesByEleveBySousMatiere, noteDevoir);
            }

            // Calcul des Moyennes de la classe par devoir
            for (Map.Entry<Long, ArrayList<NoteDevoir>> entry : notesByDevoir.entrySet()) {
                JsonObject moyenne = utilsService.calculMoyenneParDiviseur(entry.getValue(), true);
                moyenne.put(Field.ID, entry.getKey());
                listMoyDevoirs.add(moyenne);
            }
            result.put(Field.DEVOIRS, listMoyDevoirs);

            Double sumMoyClasse = 0.0;
            int nbMoyenneClasse = 0;
            Double min = null;
            Double max = null;
            //  Double globalCoefficientState = new Double(0);

            Map<Long, Double> mapSumMoyClasse = new HashMap<>();
            Map<Long, Integer> mapNbMoyenneClasse = new HashMap<>();
            Map<Long, Double> mapMin = new HashMap<>();
            Map<Long, Double> mapMax = new HashMap<>();
            // Calcul des moyennes par élève
            for (Map.Entry<String, T> entry : notesByEleve.entrySet()) {
                String idEleve = entry.getKey();

                JsonObject submoyenne = new JsonObject().put(matiereId, new JsonObject());

                final Boolean withStat = false;
                final Integer diviseur = 20;
                Double moyenneComputed = 0.0;
                boolean hasNote = false;

                if (isNotNull(notesByEleveBySousMatiere.get(idEleve))) {
                    //Si on a des sous-matières, on calcule la moyenne par sous-matière, puis la moyenne de la matière.
                    double total = 0;
                    double totalCoeff = 0;

                    for (Map.Entry<Long, ArrayList<NoteDevoir>> subEntry :
                            notesByEleveBySousMatiere.get(idEleve).entrySet()) {
                        Long idSousMat = subEntry.getKey();
                        Service serv = subEntry.getValue().get(0).getService();
                        double coeff = 1.d;
                        if (serv != null && serv.getSubtopics() != null && serv.getSubtopics().size() > 0) {
                            SubTopic subTopic = serv.getSubtopics().stream()
                                    .filter(el ->
                                            el.getId().equals(idSousMat)
                                    ).findFirst().orElse(null);
                            if (subTopic != null)
                                coeff = subTopic.getCoefficient();
                        }

                        JsonObject moyenSousMat;

                        if (isNotNull(idSousMat)) {
                            moyenSousMat = utilsService.calculMoyenne(subEntry.getValue(), withStat, diviseur, annual);

                            String key = (isNull(idSousMat) ? "null" : idSousMat.toString());
                            submoyenne.getJsonObject(matiereId).put(key, moyenSousMat);

                            total += coeff * moyenSousMat.getDouble(Field.MOYENNE);
                            totalCoeff += coeff;
                            hasNote = true;
                        } else {
                            moyenSousMat = utilsService.calculMoyenne(subEntry.getValue(),
                                    withStat, diviseur, annual);
                            submoyenne.getJsonObject(matiereId).put("null", moyenSousMat);

                            if (!hasSousMatieres) {
                                total += coeff * moyenSousMat.getDouble(Field.MOYENNE);
                                totalCoeff += coeff;
                                hasNote = true;
                            }
                        }

                        if (isNotNull(moyenSousMat) && isNotNull(idSousMat)) {
                            if (!mapNbMoyenneClasse.containsKey(idSousMat)) {
                                mapNbMoyenneClasse.put(idSousMat, 0);
                                mapSumMoyClasse.put(idSousMat, 0.0);
                            }
                            Double moySousMat = 0.0;
                            if (!"NN".equals(moyenSousMat.getValue(Field.MOYENNE))) {
                                moySousMat = moyenSousMat.getDouble(Field.MOYENNE);
                            }
                            int nbSousMoyClass = mapNbMoyenneClasse.get(idSousMat);
                            Double sumMoySous = mapSumMoyClasse.get(idSousMat);
                            mapNbMoyenneClasse.put(idSousMat, nbSousMoyClass + 1);
                            mapSumMoyClasse.put(idSousMat, sumMoySous + moySousMat);
                            if (isNull(mapMax.get(idSousMat)) || mapMax.get(idSousMat).compareTo(moySousMat) < 0) {
                                mapMax.put(idSousMat, moySousMat);
                            }
                            if (isNull(mapMin.get(idSousMat)) || mapMin.get(idSousMat).compareTo(moySousMat) > 0) {
                                mapMin.put(idSousMat, moySousMat);
                            }
                        }
                    }
                    moyenneComputed = Math.round((total / totalCoeff) * Field.ROUNDER) / Field.ROUNDER;
                    if (!mapNbMoyenneClasse.containsKey(null)) {
                        mapNbMoyenneClasse.put(null, 0);
                        mapSumMoyClasse.put(null, 0.0);
                    }
                    int nbSousMoyClass = mapNbMoyenneClasse.get(null);
                    Double sumMoySous = mapSumMoyClasse.get(null);
                    mapNbMoyenneClasse.put(null, nbSousMoyClass + 1);
                    mapSumMoyClasse.put(null, sumMoySous + moyenneComputed);
                    if (isNull(mapMax.get(null)) || mapMax.get(null).compareTo(moyenneComputed) < 0) {
                        mapMax.put(null, moyenneComputed);
                    }
                    if (isNull(mapMin.get(null)) || mapMin.get(null).compareTo(moyenneComputed) > 0) {
                        mapMin.put(null, moyenneComputed);
                    }
                } else {
                    //Sinon, on calcule la moyenne avec toutes les notes, sans distinction de sous-matières.
                    moyenneComputed = utilsService.calculMoyenne((ArrayList<NoteDevoir>) entry.getValue(),
                            false, 20, annual).getDouble(Field.MOYENNE);
                    hasNote = true;
                }

                JsonObject moyenne = new JsonObject().put(Field.MOYENNE, moyenneComputed)
                        .put(Field.HASNOTE, hasNote);
                if (!hasNote) {
                    moyenne.put(Field.MOYENNE, "NN");
                }
                if (withStat) {
                    moyenne.put("noteMax", moyenneComputed).put("noteMin", moyenneComputed);
                }

                if (eleveMapObject.containsKey(idEleve)) {
                    Double moy = 0.0;
                    if (!Field.NN.equals(moyenneComputed)) {
                        moy = moyenneComputed;
                    }
                    JsonObject el = eleveMapObject.get(idEleve);
                    Double moyEl = null;
                    if (idMatiere != null) {
                        if (!el.containsKey(Field.MOYENNE)) {
                            el.put(Field.MOYENNE, new JsonObject());
                        }
                        el.getJsonObject(Field.MOYENNE).put(idMatiere, Field.NN.equals(moyenne.getValue(Field.MOYENNE)) ? Field.NN : moy);

                        if (el.containsKey(HAS_NOTE)) {
                            el.getJsonObject(HAS_NOTE).put(idMatiere, hasNote);
                        } else {
                            JsonObject moyMat = new JsonObject();
                            moyMat.put(idMatiere, hasNote);
                            el.put(HAS_NOTE, moyMat);
                        }

                        if (!el.containsKey(MOYENNEFINALE)) {
                            JsonObject moyMat = new JsonObject();
                            moyMat.put(idMatiere, Field.NN.equals(moyenne.getValue(Field.MOYENNE)) ? Field.NN : moy);
                            el.put(MOYENNEFINALE, moyMat);
                        } else if (!el.getJsonObject(MOYENNEFINALE).containsKey(idMatiere)) {
                            el.getJsonObject(MOYENNEFINALE).put(idMatiere, Field.NN.equals(moyenne.getValue(Field.MOYENNE)) ? Field.NN : moy);
                        }

                        if (el.getJsonObject(MOYENNEFINALE).containsKey(idMatiere)) {
                            if (isNotNull(el.getJsonObject(MOYENNEFINALE).getValue(idMatiere))) {
                                try {
                                    moyEl = Double.valueOf(el.getJsonObject(MOYENNEFINALE).getString(idMatiere));
                                    sumMoyClasse += moyEl;
                                } catch (ClassCastException c) {
                                    moyEl = el.getJsonObject(MOYENNEFINALE).getDouble(idMatiere);
                                    sumMoyClasse += moyEl;
                                } catch (Exception error) {
                                    log.info("NN in average note" + error);
                                }
                            } else {
                                el.put("hasMoyenneFinaleNN", true);
                            }
                        } else {
                            moyEl = moy;
                            sumMoyClasse += moyEl;
                        }
                    } else {
                        el.put(Field.MOYENNE, moy).put(HAS_NOTE, hasNote);
                        if (isExport && !el.containsKey(MOYENNEFINALE)) {
                            el.put(MOYENNEFINALE, moy);
                        }
                        if (el.containsKey(MOYENNEFINALE)) {
                            if (isNotNull(el.getValue(MOYENNEFINALE))) {
                                try {
                                    moyEl = Double.valueOf(el.getString(MOYENNEFINALE));
                                    sumMoyClasse += moyEl;
                                } catch (ClassCastException c) {
                                    moyEl = el.getDouble(MOYENNEFINALE);
                                    sumMoyClasse += moyEl;
                                }
                            } else {
                                el.put("hasMoyenneFinaleNN", true);
                            }
                        } else {
                            moyEl = moy;
                            sumMoyClasse += moyEl;
                        }
                    }
                    if (isNotNull(moyEl)) {
                        if (isNull(min) || isNull(max)) {
                            min = moyEl;
                            max = moyEl;
                        }
                        if (moyEl < min) {
                            min = moyEl;
                        }
                        if (moyEl > max) {
                            max = moyEl;
                        }
                        ++nbMoyenneClasse;
                    }
                    if (!el.containsKey("_" + Field.MOYENNE)) {
                        el.put("_" + Field.MOYENNE, new JsonObject());
                    }
                    if (!el.getJsonObject("_" + Field.MOYENNE).containsKey(matiereId)) {
                        el.getJsonObject("_" + Field.MOYENNE).put(matiereId, new JsonObject());
                    }
                    el.getJsonObject("_" + Field.MOYENNE).getJsonObject(matiereId).getMap()
                            .putAll(submoyenne.getJsonObject(matiereId).getMap());
                }

                moyenne.put("id", idEleve);
                listMoyEleves.add(moyenne);
            }

            Object moyClasse;
            DecimalFormat decimalFormat = new DecimalFormat("#.0");
            decimalFormat.setRoundingMode(RoundingMode.HALF_UP);//with this mode 2.125 -> 2.13 without 2.125 -> 2.12
            if (!annual) {
                moyClasse = (nbMoyenneClasse > 0) ? decimalFormat.format((sumMoyClasse / nbMoyenneClasse)) : " ";
                result.put("moyenne_classe", moyClasse);
            } else {
                moyClasse = (nbMoyenneClasse > 0) ? (sumMoyClasse / nbMoyenneClasse) : " ";
                result.put("moyenne_classe", moyClasse);
            }
            result.put("_moyenne_classe", new JsonObject());
            JsonObject moyClasseObj = new JsonObject().put(Field.MIN, min).put(Field.MAX, max).put(Field.MOYENNE, moyClasse);
            result.getJsonObject("_moyenne_classe").put("nullFinal", moyClasseObj);
            for (Map.Entry<Long, Double> sousMatMoyClasse : mapSumMoyClasse.entrySet()) {
                Double moySousMat = sousMatMoyClasse.getValue();
                Long idSousMat = sousMatMoyClasse.getKey();
                String key = (isNull(idSousMat) ? "null" : idSousMat.toString());
                int nbSousMoyClass = mapNbMoyenneClasse.get(idSousMat);
                Object moySous = (nbSousMoyClass > 0) ? (moySousMat / nbSousMoyClass) : " ";
                JsonObject moySousMatCl = new JsonObject().put(MIN, mapMin.get(idSousMat))
                        .put(MAX, mapMax.get(idSousMat)).put(Field.MOYENNE, decimalFormat.format(moySous));
                result.getJsonObject("_moyenne_classe").put(key, moySousMatCl);
            }

            HashMap<Long, JsonArray> listMoy = new HashMap<>();
            if (idPeriode == null) {
                if (hasSousMatieres) {
                    for (Map.Entry<String, HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>>> entryEleve
                            : notesByDevoirByPeriodeByEleveBySousMatiere.entrySet()) {
                        for (Map.Entry<Long, HashMap<Long, ArrayList<NoteDevoir>>> entryPeriode
                                : notesByDevoirByPeriodeByEleveBySousMatiere.get(entryEleve.getKey()).entrySet()) {
                            double total = 0;
                            double totalCoeff = 0;
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
                            for (Map.Entry<Long, ArrayList<NoteDevoir>> entrySousMatieres
                                    : entryPeriode.getValue().entrySet()) {
                                Long idSousMatiere = entrySousMatieres.getKey();
                                Service serv = entrySousMatieres.getValue().get(0).getService();
                                double coeff = 1.d;
                                if(serv != null && serv.getSubtopics() != null && serv.getSubtopics().size() > 0 ) {
                                    SubTopic subTopic =  serv.getSubtopics().stream()
                                            .filter(el ->
                                                    el.getId().equals(idSousMatiere)
                                            ).findFirst().orElse(null);
                                    if(subTopic != null)
                                        coeff = subTopic.getCoefficient();
                                }
                                if (isNotNull(idSousMatiere)) {
                                    JsonObject moyenne = utilsService.calculMoyenne(entrySousMatieres.getValue(), false, 20, annual);
                                    total += coeff * moyenne.getDouble(Field.MOYENNE);
                                    totalCoeff += coeff;
                                }
                            }

                            JsonObject moyenne = new JsonObject().put(Field.MOYENNE, Math.round((total / totalCoeff) * Field.ROUNDER) / Field.ROUNDER)
                                    .put("hasNote", true);
                            if(totalCoeff == 0)
                                moyenne.put(Field.MOYENNE,Field.NN);

                            Boolean isFinale = false;
                            moyenne.put(Field.ID_PERIODE, entryPeriode.getKey());
                            moyenne.put(Field.ID_ELEVE, entryEleve.getKey());
                            moyenne.put("isFinale", isFinale);
                            listMoy.get(entryPeriode.getKey()).add(moyenne);
                            if (idMatiere != null) {
                                eleveMapObject.get(entryEleve.getKey()).getJsonObject(MOYENNES).getJsonArray(idMatiere).add(moyenne);
                            } else {
                                eleveMapObject.get(entryEleve.getKey()).getJsonArray(MOYENNES).add(moyenne);
                            }

                            if (listMoy.get(entryPeriode.getKey()).size() > 0) {
                                result.getJsonArray(MOYENNES).add(
                                        listMoy.get(entryPeriode.getKey()).getJsonObject(0));
                            }
                        }
                    }
                }
                else {
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
                                            Long periode = moyFin.getLong(Field.ID_PERIODE);
                                            notePeriode = notePeriode.stream()
                                                    .filter(line -> !(line.getIdPeriode().equals(periode)))
                                                    .collect(Collectors.toList());
                                            if (moyFin.getString(Field.MOYENNE) != null) sumMoy += Double.valueOf(moyFin.getString(Field.MOYENNE));
                                        }
                                        if (!notePeriode.isEmpty()) {
                                            ++nbMoy;
                                            moyenne = utilsService.calculMoyenne(notePeriode, false, 20,
                                                    annual);
                                            sumMoy += moyenne.getDouble(Field.MOYENNE);
                                        }
                                        moyenne.remove(Field.MOYENNE);
                                        moyenne.put(Field.MOYENNE, sumMoy / nbMoy);
                                    } else {
                                        JsonObject moyObj = utilsService.getObjectForPeriode(moyennesFinales,
                                                entry.getKey(), Field.ID_PERIODE);
                                        if (moyObj != null) {
                                            moyenne.remove(Field.MOYENNE);
                                            if ((moyObj.getString(Field.MOYENNE) != null)) {
                                                moyenne.put(Field.MOYENNE, moyObj.getString(Field.MOYENNE));
                                            } else {
                                                moyenne.put(Field.MOYENNE, Field.NN);
                                            }
                                            isFinale = true;
                                        }
                                    }
                                }
                                moyenne.put(Field.ID_PERIODE, entry.getKey());
                                moyenne.put(Field.ID_ELEVE, entryEleve.getKey());
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
    }

    private <T> void calculMoyennesCompetencesNotesForReleve (JsonArray listCompNotes, JsonObject result, Long idPeriode,
                                                              JsonArray tableauDeconversion,
                                                              Map<String, JsonObject> eleveMapObject, Boolean isAvgSkill) {

        Map<String, JsonArray> notesByEleve = groupeNotesByStudent(listCompNotes);

        for (Map.Entry<String, JsonArray> entry : notesByEleve.entrySet()) {
            String idEleve = entry.getKey();
            JsonArray compNotesEleve = entry.getValue();

            if (eleveMapObject.containsKey(idEleve)) {
                JsonObject eleveObject = eleveMapObject.get(idEleve);
                if (eleveObject.getJsonArray(COMPETENCES_NOTES_KEY) == null) {
                    eleveObject.put(COMPETENCES_NOTES_KEY, compNotesEleve);
                }
                if (!eleveObject.getJsonArray(COMPETENCES_NOTES_KEY).equals(compNotesEleve)) {
                    log.info("calculMoyennesCompetencesNotesForReleve get difference");
                    log.warn(compNotesEleve.encode() + " _\n " +
                            eleveObject.getJsonArray(COMPETENCES_NOTES_KEY).encode());
                }
                calculPositionnementAutoByEleveByMatiere(compNotesEleve, eleveObject, false, tableauDeconversion,
                        null, null, isAvgSkill);
                JsonObject positionnement = utilsService.getObjectForPeriode(
                        eleveObject.getJsonArray(POSITIONNEMENTS_AUTO), idPeriode, Field.ID_PERIODE);

                JsonObject posiSousMatiere = eleveObject.getJsonObject("_" + POSITIONNEMENTS_AUTO);

                if (isNotNull(posiSousMatiere) && isNotNull(idPeriode)) {
                    JsonObject positionnementSousMatiere = posiSousMatiere.getJsonObject(idPeriode.toString());
                    if (isNotNull(positionnementSousMatiere)) {
                        Map<String, Object> posiMap = positionnementSousMatiere.getMap();
                        for (Map.Entry<String, Object> o : posiMap.entrySet()) {
                            JsonObject posi_sous_matiere = ((JsonObject) o.getValue());
                            Float moyennePositionnement = posi_sous_matiere.getFloat(Field.MOYENNE);
                            String pos = utilsService.convertPositionnement(moyennePositionnement,
                                    tableauDeconversion, false);
                            posi_sous_matiere.put(Field.MOYENNE, pos);
                            posi_sous_matiere.put(Field.POSITIONNEMENT, pos);
                        }
                    }
                }

                String positionnement_auto = "";
                if (positionnement != null) {
                    Float moyennePositionnement = positionnement.getFloat(Field.MOYENNE);

                    positionnement_auto = utilsService.convertPositionnement(moyennePositionnement,
                            tableauDeconversion, false);
                }

                eleveObject.put(Field.POSITIONNEMENT, positionnement_auto);
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
                            eleve.put(Field.NAME, eleve.getString(LAST_NAME_KEY));
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

    private Future <JsonArray> getStudentClassForExportReleve(String classId, Long periodId, JsonArray studentsIds, Integer typeClass,
                                                Map<String, JsonObject> studentMapObject){
        Promise<JsonArray> promiseStudents = Promise.promise();
        Promise<JsonArray> promiseStudentsClass = Promise.promise();
        getStudentClassForExportReleve(classId, periodId, studentsIds, typeClass,studentMapObject, promiseStudentsClass.future());

        promiseStudentsClass.future()
                .onSuccess(promiseStudents::complete)
                .onFailure( error -> {
                    log.error(String.format("[Competences@%s::getStudentClassForExportReleve] Error during request : %s.",
                            this.getClass().getSimpleName(), error.getMessage()));

                    promiseStudents.fail(error.getMessage());
                });

        return promiseStudents.future();
    }


    private void FormateColonneFinaleReleve(JsonArray datas, Map<String, JsonObject> eleveMapObject,
                                            String colonne, Long idPeriode, Boolean hasEvaluatedHommeWork,
                                            Boolean withPreviousAppreciation) {
        String resultLabel = colonne;
        if (Field.MOYENNE.equals(colonne)) {
            resultLabel += (idPeriode!=null)? "Finale" : "sFinales";
        }
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            String idEleve = data.getString(Field.ID_ELEVE);
            JsonObject eleve = eleveMapObject.get(idEleve);

            if(eleve != null) {
                if(idPeriode != null) {
                    if (withPreviousAppreciation) {
                        if( !eleve.containsKey(PREVIOUSAPPRECIATIONS)) {
                            eleve.put(PREVIOUSAPPRECIATIONS, new JsonArray());
                        }
                        if (idPeriode.equals(data.getLong(Field.ID_PERIODE))) {
                            eleve.put(resultLabel, data.getValue(colonne));
                        }
                        if (data.getLong(Field.ID_PERIODE) < idPeriode && APPRECIATION_MATIERE_PERIODE.equals(resultLabel)) {
                            eleve.getJsonArray(PREVIOUSAPPRECIATIONS).add(data);
                        }
                    } else {
                        if(eleve.containsKey(resultLabel)){
                            eleve.remove(resultLabel);
                        }
                        eleve.put(resultLabel, data.getValue(colonne));
                    }
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
        if (!hasEvaluatedHommeWork && Field.MOYENNE.equals(colonne)) {
            for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                if (!student.getValue().containsKey(resultLabel)){
                    student.getValue().put(resultLabel, Field.NN);
                }

            }
        }

    }

    private void getMoyenneMinMaxByMatiere(Map<String, JsonObject> eleveMapObject, Long idPeriode, String idMatiere, Boolean annual, JsonObject resulHandler) {
        String moyenneLabel = Field.MOYENNE;
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
                if(student.getValue().getJsonObject(moyenneLabel).getValue(idMatiere) != Field.NN &&
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
            if (student.getValue().containsKey(Field.POSITIONNEMENT) &&
                    student.getValue().getJsonObject(Field.POSITIONNEMENT).containsKey(idMatiere)){
                if(student.getValue().getJsonObject(Field.POSITIONNEMENT).getValue(idMatiere) != Field.NN &&
                        student.getValue().getJsonObject(Field.POSITIONNEMENT).getValue(idMatiere) != "" &&
                        student.getValue().getJsonObject(Field.POSITIONNEMENT).getValue(idMatiere) != null){
                    Double number;
                    try {
                        number = Double.parseDouble(student.getValue().getJsonObject(Field.POSITIONNEMENT).getString(idMatiere).replace(",","."));
                    } catch (ClassCastException c) {
                        number = student.getValue().getJsonObject(Field.POSITIONNEMENT).getDouble(idMatiere);
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

        DecimalFormat decimalFormat = new DecimalFormat("#.0");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);//with this mode 2.125 -> 2.13 without 2.125 -> 2.12

        JsonObject statsToAdd = new JsonObject();
        if(!initialisationPositionnement){
            JsonObject minPos = new JsonObject().put(Field.MINIMUM, Field.NN);
            statsToAdd.put(Field.POSITIONNEMENT, minPos);
            statsToAdd.getJsonObject(Field.POSITIONNEMENT).put(Field.MAXIMUM, Field.NN);
            statsToAdd.getJsonObject(Field.POSITIONNEMENT).put(Field.MOYENNE, Field.NN);
        }else{
            JsonObject minPos = new JsonObject().put(Field.MINIMUM, positionnementMin);
            statsToAdd.put(Field.POSITIONNEMENT,minPos);
            statsToAdd.getJsonObject(Field.POSITIONNEMENT).put(MAXIMUM, positionnementMax);
            if(!annual) {
                if (moyennePos.compareTo((double) 0) != 0) {
                    statsToAdd.getJsonObject(Field.POSITIONNEMENT).put(Field.MOYENNE, decimalFormat.format((moyennePos / nbElevesPositionnement)));
                }else {
                    statsToAdd.getJsonObject(Field.POSITIONNEMENT).put(Field.MOYENNE, Double.valueOf(0));
                }
            }else
                statsToAdd.getJsonObject(Field.POSITIONNEMENT).put(Field.MOYENNE, moyennePos/nbElevesPositionnement);

        }
        if(!initialisationMoyenne){
            JsonObject minPos = new JsonObject().put(MINIMUM, Field.NN);
            statsToAdd.put(Field.MOYENNE,minPos);
            statsToAdd.getJsonObject(Field.MOYENNE).put(MAXIMUM, Field.NN);
            statsToAdd.getJsonObject(Field.MOYENNE).put(Field.MOYENNE,Field.NN);
        }else{
            JsonObject minPos = new JsonObject().put(MINIMUM, moyenneMin);
            statsToAdd.put(Field.MOYENNE,minPos);
            statsToAdd.getJsonObject(Field.MOYENNE).put(MAXIMUM, moyenneMax);
            if(!annual)
                statsToAdd.getJsonObject(Field.MOYENNE).put(Field.MOYENNE,  decimalFormat.format((moyenne/nbElevesMoyenne)));
            else
                statsToAdd.getJsonObject(Field.MOYENNE).put(Field.MOYENNE,  (moyenne/nbElevesMoyenne));

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
        if (Field.MOYENNE.equals(colonne)) {
            resultLabel += (idPeriode!=null)? "Finale" : "sFinales";
        }
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            String idEleve = data.getString(Field.ID_ELEVE);
            JsonObject eleve = eleveMapObject.get(idEleve);

            if(data.getValue(colonne) != null) {
                if (eleve != null) {
                    if (idPeriode != null) {
                        if(Field.MOYENNE.equals(colonne) || Field.POSITIONNEMENT.equals(colonne)) {
                            if(data.getString("id_matiere").equals(idMatiere)) {
                                if(eleve.containsKey(resultLabel)) {
                                    if (eleve.getJsonObject(resultLabel).containsKey(idMatiere)) {
                                        eleve.getJsonObject(resultLabel).remove(idMatiere);
                                    }
                                    if(!data.getValue(colonne).equals("-100"))
                                        eleve.getJsonObject(resultLabel).put(idMatiere, data.getValue(colonne));
                                    else
                                        eleve.getJsonObject(resultLabel).put(idMatiere, Field.NN);
                                }else if(!data.getValue(colonne).equals("-100")){
                                    JsonObject jsonToAdd = new JsonObject();
                                    jsonToAdd.put(idMatiere, data.getValue(colonne));
                                    eleve.put(resultLabel, jsonToAdd);
                                }else{
                                    JsonObject jsonToAdd = new JsonObject();
                                    jsonToAdd.put(idMatiere, Field.NN);
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
        if (!hasEvaluatedHommeWork && Field.MOYENNE.equals(colonne)) {
            for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                if(student.getValue().containsKey(resultLabel)){
                    if (!student.getValue().getJsonObject(resultLabel).containsKey(idMatiere)) {
                        student.getValue().getJsonObject(resultLabel).put(idMatiere, Field.NN);
                    }
                }else{
                    JsonObject jsonToAdd = new JsonObject();
                    jsonToAdd.put(idMatiere, Field.NN);
                    student.getValue().put(resultLabel, jsonToAdd);
                }
            }
        }
        if (Field.MOYENNE.equals(colonne)) {
            for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                if(!student.getValue().containsKey(resultLabel)){
                    JsonObject jsonToAdd = new JsonObject();
                    jsonToAdd.put(idMatiere, Field.NN);
                    student.getValue().put(resultLabel, jsonToAdd);
                }
            }
        }
        if (Field.POSITIONNEMENT.equals(colonne)) {
            for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
                if(!student.getValue().containsKey(resultLabel)) {
                    JsonObject jsonToAdd = new JsonObject();
                    jsonToAdd.putNull(idMatiere);
                    student.getValue().put(resultLabel, jsonToAdd);
                }
            }
        }

    }

    private void getMoyenneGeneraleMinMax(Map<String, JsonObject> eleveMapObject, Long idPeriode, JsonArray idMatieres,
                                          JsonArray services, Boolean annual, JsonObject resulHandler) {
        String moyenneLabel = Field.MOYENNE;
        moyenneLabel += (idPeriode != null) ? "Finale" : "sFinales";
        double moyenneDeMoyenne = 0.0;
        int nbElevesMoyenne = 0;
        double moyenneMin = 0.0;
        double moyenneMax = 0.0;
        boolean initialisationMoyenne = false;
        DecimalFormat decimalFormat = new DecimalFormat("#.0");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);

        for (Map.Entry<String, JsonObject> student : eleveMapObject.entrySet()) {
            Double moyenne = 0.0;
            int nbMatieres = 0;

            for (Object idMatiere : idMatieres) {
                String idMat = (String) idMatiere;
                if (student.getValue().containsKey(moyenneLabel) && student.getValue().getJsonObject(moyenneLabel).containsKey(idMat)) {
                    JsonObject moyenneJson = student.getValue().getJsonObject(moyenneLabel);
                    if (moyenneJson.getValue(idMat) != Field.NN && moyenneJson.getValue(idMat) != "" &&
                            moyenneJson.getValue(idMat) != null) {

                        Long coefficient = 1L;
                        JsonObject service = (JsonObject) services.stream()
                                .filter(el -> idMatiere.equals(((JsonObject) el).getString("id_matiere")))
                                .findFirst().orElse(null);
                        if(service != null){
                            coefficient = service.getLong("coefficient");
                        }
                        try {
                            moyenne += Double.parseDouble(moyenneJson.getString(idMat).replace(",",".")) * coefficient;
                        } catch (ClassCastException c) {
                            moyenne += moyenneJson.getDouble(idMat) * coefficient;
                        }
                        nbMatieres += coefficient;
                    }
                }
            }

            if(nbMatieres == 0){
                student.getValue().put(MOYENNE_GENERALE, Field.NN);
            } else {
                Double moyenneEleve = moyenne / nbMatieres;
                if(!annual){
                    student.getValue().put(MOYENNE_GENERALE, decimalFormat.format(moyenneEleve));
                } else {
                    student.getValue().put(MOYENNE_GENERALE, moyenneEleve);
                }
                moyenneDeMoyenne += moyenneEleve;
                nbElevesMoyenne++;
                if (!initialisationMoyenne) {
                    moyenneMin = moyenneEleve;
                    moyenneMax = moyenneEleve;
                    initialisationMoyenne = true;
                } else {
                    if (moyenneMin > moyenneEleve)
                        moyenneMin = moyenneEleve;
                    if (moyenneMax < moyenneEleve)
                        moyenneMax = moyenneEleve;
                }
            }
        }

        if(nbElevesMoyenne == 0){
            JsonObject minMoy = new JsonObject().put(MINIMUM, Field.NN);
            resulHandler.getJsonObject(STATISTIQUES).put(MOYENNE_GENERALE,minMoy);
            resulHandler.getJsonObject(STATISTIQUES).getJsonObject(MOYENNE_GENERALE).put(MAXIMUM, Field.NN);
            resulHandler.getJsonObject(STATISTIQUES).getJsonObject(MOYENNE_GENERALE).put(Field.MOYENNE, Field.NN);
        } else {
            JsonObject minMoy = new JsonObject().put(MINIMUM, decimalFormat.format(moyenneMin));
            resulHandler.getJsonObject(STATISTIQUES).put(MOYENNE_GENERALE,minMoy);
            resulHandler.getJsonObject(STATISTIQUES).getJsonObject(MOYENNE_GENERALE).put(MAXIMUM,
                    decimalFormat.format(moyenneMax));
            resulHandler.getJsonObject(STATISTIQUES).getJsonObject(MOYENNE_GENERALE).put(Field.MOYENNE,
                    decimalFormat.format((moyenneDeMoyenne / nbElevesMoyenne)));
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

    private Future<JsonObject> getNbEvaluatedHomeWork(String classId, String subjectId, Long periodId, JsonArray groupId) {
        Promise<JsonObject> nbDevoisPromise = Promise.promise();
        getNbEvaluatedHomeWork(classId,subjectId,periodId,groupId, FutureHelper.handlerJsonObject(nbDevoisPromise,
                String.format("[Competences@%s::getNbEvaluatedHomeWork] Error during sql request: ",
                        this.getClass().getSimpleName())));

        return nbDevoisPromise.future();
    }

    public void exportPDFRelevePeriodique(JsonObject param, final HttpServerRequest request, Vertx vertx,
                                          JsonObject config ){
        // Récupération des données de l'export
        Future<JsonObject> exportResult = Future.future();
        getDatasReleve(param, event -> formate(exportResult, event));

        String key = Field.ELEVES;
        String idStructure = param.getString(ID_ETABLISSEMENT_KEY);
        Map<String, JsonObject> mapEleve = new HashMap<>();
        Future<JsonObject> structureFuture = Future.future();
        mapEleve.put(key, new JsonObject().put(ID_ETABLISSEMENT_KEY, idStructure));

        // Récupération des informations sur l'établissment
        new DefaultExportBulletinService(eb, null).getStructure(key, mapEleve.get(key),
                event -> formate(structureFuture, event));

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

        final Long idPeriode = (idPeriodeString != null) ? Long.parseLong(idPeriodeString) : null;

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
            if (Field.OK.equals(body.getString(Field.STATUS))) {
                subjectF.complete(body.getJsonArray(Field.RESULTS));
            } else {
                subjectF.fail(body.getString(Field.MESSAGE));
            }
        }));

        // Récupération du tableau de conversion
        Future<JsonArray> tableauDeConversionFuture = Future.future();
        // On récupère le tableau de conversion des compétences notes pour Lire le positionnement
        competenceNoteService.getConversionNoteCompetence(idEtablissement, idClasse, tableauEvent ->
                formate(tableauDeConversionFuture, tableauEvent));

        Future<Boolean> isAvgSkillFuture = structureOptionsService.isAverageSkills(idEtablissement);

        isAvgSkillFuture
                .onSuccess(isAvgSkillpromiseResult -> CompositeFuture.all(compNoteF, noteF, subjectF, tableauDeConversionFuture).setHandler(event -> {
                    if(event.failed()){
                        String message = "[getReleveDataForGraph] " + event.cause().getMessage();
                        log.error(message);
                        handler.handle(new Either.Left<>(message));
                        return;
                    }

                    //Récupération des Services
                    Promise<Object> servicesPromise = Promise.promise();
                    utilsService.getServices(idEtablissement,
                            new JsonArray().add(idClasse), FutureHelper.handlerJsonArray(servicesPromise));

                    //Récupération des Multi-teachers
                    Promise<Object> multiTeachingPromise = Promise.promise();
                    utilsService.getMultiTeachers(idEtablissement,
                            new JsonArray().add(idClasse), (idPeriode != null ? idPeriode.intValue() : null), FutureHelper.handlerJsonArray(multiTeachingPromise));

                    //Récupération des Sous-Matières
                    Future<List<SubTopic>> subTopicCoefFuture = utilsService.getSubTopicCoeff(idEtablissement, idClasse);

                    CompositeFuture.all(servicesPromise.future(), multiTeachingPromise.future(), subTopicCoefFuture).setHandler(e -> {
                        if (e.failed()) {
                            String message = "[getReleveDataForGraph] " + event.cause().getMessage();
                            log.error(message);
                            handler.handle(new Either.Left<>(message));
                            return;
                        }
                        else {

                            Structure structure = new Structure();
                            structure.setId(idEtablissement);
                            JsonArray servicesJson = (JsonArray) servicesPromise.future().result();
                            JsonArray multiTeachers = (JsonArray) multiTeachingPromise.future().result();
                            List<SubTopic> subTopics = subTopicCoefFuture.result();

                            List<Service> services = new ArrayList<>();
                            List<MultiTeaching> multiTeachings = new ArrayList<>();
                            new DefaultExportBulletinService(eb, null).setMultiTeaching(structure, multiTeachers, multiTeachings, idClasse);
                            setServices(structure, servicesJson, services, subTopics);

                            final JsonArray listCompNotes = compNoteF.result();
                            final JsonArray listNotes = noteF.result();
                            Map<String,JsonArray> matieresCompNotes = new HashMap<>();
                            Map<String,JsonArray> matieresCompNotesEleve = new HashMap<>();

                            // 4. On regroupe  les compétences notes par idMatière
                            JsonArray idMatieres = groupDataByMatiere(listCompNotes, matieresCompNotes, matieresCompNotesEleve, idEleve,
                                    true);

                            // 5. On regroupe les notes par idMatière

                            Map<String,JsonArray> matieresNotes = new HashMap<>();
                            Map<String,JsonArray> matieresNotesEleve = new HashMap<>();
                            idMatieres = utilsService.saUnion(groupDataByMatiere(listNotes, matieresNotes, matieresNotesEleve, idEleve,
                                    false), idMatieres);

                            Map<String, StatClass> mapMatieresStatClasseAndEleve = new HashMap<>();

                            StatMat statMat = new StatMat();
                            if(idPeriode != null) {
                                statMat.setMapIdMatStatclass(listNotes, services, multiTeachers, idClasse);
                                mapMatieresStatClasseAndEleve = statMat.getMapIdMatStatclass();
                            } else { // notes order by periode
                                Map<Integer, JsonArray> mapIdPeriodeNotes = getListNotesByPeriode(listNotes, false);
                                if(!mapIdPeriodeNotes.isEmpty()) {
                                    for (int i = 0; i < idMatieres.size(); i++) {
                                        String idMatiere = idMatieres.getString(i);
                                        List<NoteDevoir> listAverageClass = new ArrayList<>();
                                        List<NoteDevoir> listAverageStudent = new ArrayList<>();
                                        Double annualClassAverage;
                                        Double annualClassMin = null;
                                        Double annualClassMax = null;
                                        Double annualAverageStudent;
                                        for(Map.Entry<Integer, JsonArray> IdPeriodeNotesEntry : mapIdPeriodeNotes.entrySet()){
                                            StatMat statMatP = new StatMat();
                                            statMatP.setMapIdMatStatclass(IdPeriodeNotesEntry.getValue());
                                            Map<String, StatClass> mapMatieresStatClasseAndEleveP = statMatP.getMapIdMatStatclass();
                                            StatClass statClasseP = mapMatieresStatClasseAndEleveP.get(idMatiere);

                                            if (statClasseP != null && !statClasseP.getMapEleveStat().isEmpty()) {
                                                listAverageClass.add(new NoteDevoir
                                                        (statClasseP.getAverageClass(), new Double(20), false, 1.0));
                                                if (annualClassMin == null) {
                                                    annualClassMin = statClasseP.getMinMaxClass(true);
                                                } else {
                                                    if (statClasseP.getMinMaxClass(true) < annualClassMin)
                                                        annualClassMin = statClasseP.getMinMaxClass(true);
                                                }
                                                if (annualClassMax == null) {
                                                    annualClassMax = statClasseP.getMinMaxClass(false);
                                                } else {
                                                    if (statClasseP.getMinMaxClass(false) > annualClassMax)
                                                        annualClassMax = statClasseP.getMinMaxClass(false);
                                                }

                                                if (statClasseP.getMoyenneEleve(idEleve) != null) {
                                                    listAverageStudent.add(new NoteDevoir
                                                            (statClasseP.getMoyenneEleve(idEleve), new Double(20), false, 1.0));
                                                }
                                            }
                                        }

                                        StatEleve statEleveAnnual = new StatEleve();
                                        StatClass statClassAnnual = new StatClass();
                                        if( !listAverageClass.isEmpty() ){
                                            if( !listAverageStudent.isEmpty() ) {
                                                annualAverageStudent = utilsService.calculMoyenneParDiviseur(listAverageStudent,
                                                        false).getDouble("moyenne");
                                                statEleveAnnual.setMoyenneAuto(annualAverageStudent);
                                                statClassAnnual.getMapEleveStat().put(idEleve, statEleveAnnual);
                                            }

                                            annualClassAverage = utilsService.calculMoyenneParDiviseur(listAverageClass,
                                                    false).getDouble("moyenne");

                                            statClassAnnual.setAverageClass(annualClassAverage);
                                            statClassAnnual.setMax(annualClassMax);
                                            statClassAnnual.setMin(annualClassMin);
                                            mapMatieresStatClasseAndEleve.put(idMatiere, statClassAnnual);
                                        }
                                    }

                                    statMat.setMapIdMatStatclass(mapMatieresStatClasseAndEleve);
                                } else {
                                    statMat.setMapIdMatStatclass(listNotes);
                                    mapMatieresStatClasseAndEleve = statMat.getMapIdMatStatclass();
                                }
                            }

                            // 6. On récupère tous les libelles des matières de l'établissement et on fait correspondre
                            // aux résultats par idMatière
                            linkIdSubjectToLibelle(idEleve, idPeriode, getMaxOrAvgByItem(matieresCompNotes, tableauDeConversionFuture.result(),isAvgSkillpromiseResult ),
                                    getMaxOrAvgByItem(matieresCompNotesEleve, tableauDeConversionFuture.result(),isAvgSkillpromiseResult ), matieresNotes, matieresNotesEleve,
                                    mapMatieresStatClasseAndEleve, idMatieres, subjectF.result(), handler);
                        }
                    });
                }))
                .onFailure(err ->{
                    String message = "[getReleveDataForGraph] " + err.getMessage();
                    log.error(message);
                    handler.handle(new Either.Left<>(message));
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
        competenceNoteService.getConversionNoteCompetence(idEtablissement, idClasse,tableauEvent ->
                formate(tableauDeConversionFuture, tableauEvent));

        Future<Boolean> isAvgSkillFuture = structureOptionsService.isAverageSkills(idEtablissement);

        isAvgSkillFuture
                .onSuccess(isAvgSkillpromiseResult -> {
                    CompositeFuture.all(compNotesFuture, domainesCycleFuture,tableauDeConversionFuture).setHandler(event -> {
                        if (event.succeeded()) {
                            handler.handle(new Either.Right<>(linkCompNoteToLibelle(domainesCycleFuture.result(),
                                    compNotesFuture.result(), tableauDeConversionFuture.result(), idEleve, isAvgSkillpromiseResult)));
                        } else {
                            String message = event.cause().getMessage();
                            handler.handle(new Either.Left<>(message));
                            log.error(message);
                        }
                    });
                })
                .onFailure(err ->{
                    handler.handle(new Either.Left<>(err.getMessage()));
                    log.error(err.getMessage());
                });
        // 4. On Lie les compétences-Notes à leur libellé


    }

    private JsonArray linkCompNoteToLibelle(JsonArray domaines, JsonArray compNotes, JsonArray tableauConversion, String idEleve, boolean isAvgSkill) {

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
                res.add(domaine.put("competencesNotes",   getMaxOrAvgByItemDomaine(competencesNotes,tableauConversion, isAvgSkill))
                        .put("competencesNotesEleve", getMaxOrAvgByItemDomaine(competencesNotesEleve,tableauConversion, isAvgSkill)));
            }
        }
        return res;
    }

    private JsonArray groupDataByMatiere(JsonArray datas, Map<String,JsonArray> mapDataClasse,
                                         Map<String,JsonArray> mapDataEleve, String idEleve, boolean checkFormative){
        JsonArray result = new JsonArray();
        for (int i=0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            Double coefficient;
            try {
                coefficient = (data.getString(Field.COEFFICIENT) != null ) ?
                        Double.parseDouble(data.getString(Field.COEFFICIENT)) : 0.0;
            } catch (NumberFormatException nfe) {
               continue;
            }

            if (!checkFormative || (data != null && data.getBoolean(Field.FORMATIVE) != null &&
                    !data.getBoolean(Field.FORMATIVE)) || (data != null && data.getLong(Field.ID_DEVOIR) == null) ||
                    (data != null && data.getLong(ID_COMPETENCE) == null &&  coefficient != 0.0)) {
                String idMatiere = (data.getString(Field.ID_MATIERE) != null) ? data.getString(Field.ID_MATIERE) : data.getString(ID_MATIERE_MOYF);
                idMatiere = (idMatiere != null) ? idMatiere : "no_id_matiere";
                if (!mapDataClasse.containsKey(idMatiere)) {
                    mapDataClasse.put(idMatiere, new JsonArray());
                    mapDataEleve.put(idMatiere, new JsonArray());
                    result.add(idMatiere);
                }
                mapDataClasse.get(idMatiere).add(data);
                if(idEleve.equals(data.getString("id_eleve")) || idEleve.equals(data.getString("id_eleve_moyenne_finale"))) {
                    mapDataEleve.get(idMatiere).add(data);
                }
            }
        }
        return result;
    }

    private Map<Integer, JsonArray> getListNotesByPeriode(JsonArray listNotes, boolean checkFormative) {
        Map< Integer, JsonArray> mapIdPeriodeListeNotes = new HashMap<>();
        if( listNotes != null && !listNotes.isEmpty()) {
            for( int i = 0 ; i < listNotes.size(); i++){
                JsonObject note = listNotes.getJsonObject(i);

                if(!checkFormative || (note != null && note.getBoolean("formative") != null &&
                        !note.getBoolean("formative")) || note.getString("id_devoir") == null) {
                    Integer idPeriode = (note.getInteger("id_periode") != null) ?
                            note.getInteger("id_periode") : note.getInteger("id_periode_moyenne_finale");

                    if(!mapIdPeriodeListeNotes.containsKey(idPeriode)) {
                        mapIdPeriodeListeNotes.put( idPeriode, new JsonArray());
                    }
                    mapIdPeriodeListeNotes.get(idPeriode).add(note);
                }
            }
        }

        return mapIdPeriodeListeNotes;
    }
    // Permet de Calculer le Max des Niveaux Atteints par Items regroupés par Matière
    private Map<String,JsonArray> getMaxOrAvgByItem(Map<String, JsonArray> mapData, JsonArray tableauConversion, boolean isAvgSkill){
        Map<String,JsonArray> result = new HashMap<>();

        for (Map.Entry<String,JsonArray> entry: mapData.entrySet()) {
            String idEntry = entry.getKey();
            JsonArray currentEntry = entry.getValue();
            Map<String, JsonObject> maxOrAvgComp = calculMaxOrAvgCompNoteItem(currentEntry, tableauConversion,true, isAvgSkill);
            result.put(idEntry, new JsonArray());
            for (Map.Entry<String,JsonObject> max: maxOrAvgComp.entrySet()) {
                result.get(idEntry).add(max.getValue());
            }
        }
        return result;
    }

    // Permet de Calculer le Max des Niveaux Atteints par Items regroupés par Domaine
    private JsonArray getMaxOrAvgByItemDomaine(JsonArray compNotes, JsonArray tableauConversion, boolean isAvgSkill){
        JsonArray result = new JsonArray();

        Map<String, JsonObject> maxOrAvgCompNote = calculMaxOrAvgCompNoteItem(compNotes, tableauConversion,true, isAvgSkill);
        for (Map.Entry<String,JsonObject> max: maxOrAvgCompNote.entrySet()) {
            result.add(max.getValue());
        }

        return result;
    }

    private Map<String, JsonObject> calculMaxOrAvgCompNoteItem(JsonArray compNotes, JsonArray tableauConversion, Boolean takeNivFinal, boolean isAvg) {
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
                List<JsonObject> listSameCompMaxOrAvgMat = new ArrayList<>();

                if(isAvg){ // moyenne de chaque competence par matiere
                    for (Map.Entry<String, List<JsonObject>> listMat : groupByMat.entrySet()) {
                        List<JsonObject> listSameMatComp = listMat.getValue();
                        JsonObject compMat = listSameMatComp.get(0);
                        float moyenneComp = calculMoyCompetence(listSameMatComp, takeNivFinal);
                        compMat.put("evaluation", moyenneComp);
                        listSameCompMaxOrAvgMat.add(compMat);
                    }

                }else {  //On récupère les maxs de chaque matière
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
                                if (niveauFinalAnnuel != null)
                                    valueToTake = niveauFinalAnnuel;
                                else
                                    valueToTake = (niveauFinal != null) ? niveauFinal : evaluation;
                            } else {
                                valueToTake = evaluation;
                            }
                            if (valueToTake != null && valueToTake > max) {
                                max = valueToTake;
                                JsonObjectToAdd = compNoteTemp;
                            }
                        }
                        listSameCompMaxOrAvgMat.add(JsonObjectToAdd);
                    }
                }
                //Et on fait la moyenne des maxs de chaque matières dans l'item de compétences
                JsonObject compNote = listSameComp.get(0);
                Long idCompetence = compNote.getLong("id_competence");
                String idEleve = compNote.getString("id_eleve");

                idEleve = (idEleve != null) ? idEleve : "null";

                String idItem = (idCompetence != null) ? idCompetence.toString() : "null";
                idItem += idEleve;
                float moyenneToSend = calculMoyCompetence(listSameCompMaxOrAvgMat,takeNivFinal);
                int moyenneConverted = utilsService.getPositionnementValue(moyenneToSend+1, tableauConversion)-1;
                //evaluations pour le calcul du positionnement sans la convertion et evaluationGraph converti pour les proportions du graphe
                compNote.put("evaluation", moyenneToSend).put("niveau_final", moyenneToSend).put("evaluationGraph", moyenneConverted);
                maxComp.put(idItem, compNote);
            }
        }
        return maxComp;
    }

    private float calculMoyCompetence(List<JsonObject> listSameComp, Boolean  takeNivFinal){
        float avg;
        float sum = 0L;
        float nbrofElt = 0L;
        for (JsonObject compNoteTemp : listSameComp) {
            Float evaluation = compNoteTemp.getFloat("evaluation");
            Float niveauFinal = compNoteTemp.getFloat("niveau_final");
            Float niveauFinalAnnuel = compNoteTemp.getFloat("niveau_final_annuel");
            Float valueToTake;
            if (Boolean.TRUE.equals(takeNivFinal)){
                if(niveauFinalAnnuel != null)
                    valueToTake = niveauFinalAnnuel;
                else
                    valueToTake = (niveauFinal != null) ? niveauFinal : evaluation;
            }else{
                valueToTake = evaluation;
            }
            if (valueToTake != null) {
                sum += valueToTake;
                nbrofElt++;
            }
        }
        if (nbrofElt != 0)
            avg = sum / nbrofElt ;
        else
            avg = -2f;
        return avg;
    }

    private void linkIdSubjectToLibelle(String idEleve, Long idPeriode, Map<String, JsonArray> matieresCompNotes,
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
            StatClass statClasse = mapMatieresStatClasseAndEleve.get(idMatiere);
            if (statClasse != null) {
                classAverage = statClasse.getAverageClass();
                if ( idPeriode != null ) {
                    classMin = statClasse.getMinMaxClass(true);
                    classMax = statClasse.getMinMaxClass(false);
                } else {
                    classMin = statClasse.getMin();
                    classMax = statClasse.getMax();
                }
                averageStudent = statClasse.getMoyenneEleve(idEleve);
            }
            matiere.put("competencesNotes", matieresCompNotes.get(idMatiere))
                    .put("competencesNotesEleve", matieresCompNotesEleve.get(idMatiere))
                    .put("notes", matieresNotes.get(idMatiere))
                    .put("notesEleve", matieresNotesEleve.get(idMatiere))
                    .put("studentAverage", averageStudent)
                    .put("classAverage", classAverage)
                    .put("classMin", classMin)
                    .put("classMax", classMax);
            matieres.add(matiere);
        }

        matieres.add(new JsonObject().put("name", "null")
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
                    String idMatiere = sousMatiere.getString(Field.ID_MATIERE);

                    // Get moyenne sous matiere
                    Object moyenne = eleve.getJsonObject("_" + Field.MOYENNE);
                    if (isNotNull(moyenne)) {
                        moyenne = ((JsonObject) moyenne).getJsonObject(idMatiere);
                        if (isNotNull(moyenne)) {
                            moyenne = ((JsonObject) moyenne).getJsonObject(idSousMatiere.toString());
                            if (isNotNull(moyenne)) {
                                moyenne = ((JsonObject) moyenne).getFloat(Field.MOYENNE);
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
                                positionnement = ((JsonObject) positionnement).getValue(Field.POSITIONNEMENT);
                            }
                        }
                    }

                    String moyLibelle = getLibelle("average.min") + " " + sousMatiere.getString(Field.LIBELLE);
                    String posLibelle =  getLibelle("evaluations.releve.positionnement.min")  + " " +
                            sousMatiere.getString(Field.LIBELLE);
                    sousMatiere.put(Field.MOYENNE, isNull(moyenne) ? " " : moyenne)
                            .put(Field.POSITIONNEMENT, isNull(positionnement)? 0 : positionnement )
                            .put("moyLibelle", moyLibelle).put("posLibelle", posLibelle);

                    eleve.getJsonObject(SOUS_MATIERES).getJsonArray(MOYENNES).add(sousMatiere);
                    eleve.getJsonObject(SOUS_MATIERES).getJsonArray(POSITIONNEMENTS_AUTO).add(sousMatiere);
                }
            }
        }
        result.put("moyenneClasseSousMat", new JsonArray());
        for (int i = 0; i < sousMatieres.size(); i++) {
            JsonObject sousMatiere = sousMatieres.getJsonObject(i);
            String moyLibelle = getLibelle("average.class") + " " + sousMatiere.getString(Field.LIBELLE);
            Long idSousMatiere = sousMatiere.getLong(ID_SOUS_MATIERE);
            sousMatiere.put("_"+ Field.LIBELLE, moyLibelle);
            Object moy = result.getJsonObject("_moyenne_classe");
            if(isNotNull(moy) && isNotNull(idSousMatiere)){
                moy = ((JsonObject) moy).getJsonObject(idSousMatiere.toString());
                if(isNotNull(moy)){
                    moy = ((JsonObject) moy).getValue(Field.MOYENNE);
                }
            }
            moy = isNull(moy)? "" : moy;
            sousMatiere.put("_" + Field.MOYENNE, moy);
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
        getCompetencesNotesReleve(idEtablissement, idClasse,null, idMatiere, null,
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
                new JsonArray().add(idClasse), Field.MOYENNE, Boolean.FALSE, event -> formate(moyFinalesElevesF, event));
        detailsFuture.add(moyFinalesElevesF);

        // Récupération des appréciations matières, des moyennesFinales et  positionnements finaux
        // de l'élève sur toutes les périodes de la classe
        Future<JsonArray> appreciationMatierePeriode = Future.future();
        getColonneReleve(new JsonArray().add(idEleve), null, idMatiere, new JsonArray().add(idClasse),
                APPRECIATION_MATIERE_PERIODE, Boolean.FALSE, event -> formate(appreciationMatierePeriode, event));
        detailsFuture.add(appreciationMatierePeriode);

        Future<JsonArray> moyenneFinalesF = Future.future();
        getColonneReleve(new JsonArray().add(idEleve), null, idMatiere, new JsonArray().add(idClasse),
                Field.MOYENNE, Boolean.FALSE, event -> formate(moyenneFinalesF, event));
        detailsFuture.add(moyenneFinalesF);

        Future<JsonArray> positionnementF = Future.future();
        getColonneReleve(new JsonArray().add(idEleve), null, idMatiere, new JsonArray().add(idClasse),
                Field.POSITIONNEMENT, Boolean.FALSE, event -> formate(positionnementF, event));
        detailsFuture.add(positionnementF);

        // Récupération du tableau de conversion
        // On récupère le tableau de conversion des compétences notes pour Lire le positionnement
        Future<JsonArray> tableauDeConversionFuture = Future.future();
        competenceNoteService.getConversionNoteCompetence(idEtablissement, idClasse, tableauEvent ->
                formate(tableauDeConversionFuture, tableauEvent));
        detailsFuture.add(tableauDeConversionFuture);

        Promise<Object> servicesPromise = Promise.promise();
        detailsFuture.add(servicesPromise.future());

        Promise<Object> multiTeachingPromise = Promise.promise();
        detailsFuture.add(multiTeachingPromise.future());

        utilsService.getMultiTeachers(idEtablissement,
                new JsonArray().add(idClasse), null ,FutureHelper.handlerJsonArray(multiTeachingPromise));

        Future<List<SubTopic>> subTopicCoefFuture = utilsService.getSubTopicCoeff(idEtablissement, idClasse);
        detailsFuture.add(subTopicCoefFuture);

        utilsService.getServices(idEtablissement,
                new JsonArray().add(idClasse), FutureHelper.handlerJsonArray(servicesPromise));


        Future<Boolean> isAvgSkillFuture = structureOptionsService.isAverageSkills(idEtablissement);
        detailsFuture.add(isAvgSkillFuture);

        CompositeFuture.all(detailsFuture).setHandler(event -> {
            if(event.failed()){
                String error = event.cause().getMessage();
                log.error(error);
                Renders.renderError(request, new JsonObject().put(ERROR, error));
                return;
            }

            List<SubTopic> subTopics = subTopicCoefFuture.result();
            JsonArray servicesJson =(JsonArray) servicesPromise.future().result();

            Structure structure = new Structure();
            structure.setId(idEtablissement);
            List<Service> services = new ArrayList<>();

            JsonArray multiTeachers =(JsonArray) multiTeachingPromise.future().result();
            setServices(structure, servicesJson, services,subTopics);


            result.put("appreciations", appreciationMatierePeriode.result());
            result.put("moyennes_finales", moyenneFinalesF.result());
            result.put("positionnements", positionnementF.result());

            // Calcul des positionements
            JsonArray listCompNotes = listCompNotesF.result();
            calculPositionnementAutoByEleveByMatiere(listCompNotes, result,false, tableauDeConversionFuture.result(),
                    null ,null, isAvgSkillFuture.result());

            // Calcul des moyennes par période pour la classe
            JsonArray moyFinalesEleves = moyFinalesElevesF.result();

            HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse =
                    calculMoyennesEleveByPeriode(listNoteF.result(), result, idEleve, idEleves,
                            null , null,services,multiTeachers );
            calculAndSetMoyenneClasseByPeriode(moyFinalesEleves, notesByDevoirByPeriodeClasse, result);
            Renders.renderJson(request, result);
        });
    }

    @Override
    public Future<Void> insertOrUpdateDevoirNote(String idDevoir, String idEleve, Double valeur) {
        Promise<Void> promise = Promise.promise();

        String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + "." + Field.NOTES_TABLE +
                " (id_devoir, id_eleve, valeur) VALUES (?, ?, ?)" +
                " ON CONFLICT (id_devoir, id_eleve) DO UPDATE SET valeur = ? ";
        JsonArray values = new JsonArray();
        values.add(idDevoir);
        values.add(idEleve);
        values.add(valeur);
        values.add(valeur);
        Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(event -> {
            if (event.isLeft()) {
                promise.fail(event.left().getValue());
            } else {
                promise.complete();
            }
        }));
        return promise.future();
    }

    @Override
    public Future<Void> insertOrUpdateAnnotation(String idDevoir, String idEleve, String annotation) {
        Promise<Void> promise = Promise.promise();

        String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + "." + Field.APPRECIATIONS_TABLE +
                " (id_devoir, id_eleve, valeur) VALUES (?, ?, ?)" +
                " ON CONFLICT (id_devoir, id_eleve) DO UPDATE SET valeur = ? ";
        JsonArray values = new JsonArray();
        values.add(idDevoir);
        values.add(idEleve);
        values.add(annotation);
        values.add(annotation);
        Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(event -> {
            if (event.isLeft()) {
                promise.fail(event.left().getValue());
            } else {
                promise.complete();
            }
        }));
        return promise.future();
    }
}
