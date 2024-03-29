
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
import fr.openent.competences.constants.Field;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.model.achievements.AchievementsProgress;
import fr.openent.competences.service.StructureOptionsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultCompetenceNoteService extends SqlCrudService implements fr.openent.competences.service.CompetenceNoteService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultCompetenceNoteService.class);
    private final StructureOptionsService structureOptionsService;
    private final Sql sql;

    @Deprecated
    public DefaultCompetenceNoteService(String schema, String table) {
        super(schema, table);
        this.structureOptionsService = new DefaultStructureOptions();
        this.sql = Sql.getInstance();
    }

    public DefaultCompetenceNoteService(Sql sql, StructureOptionsService structureOptionsService) {
        super(Field.NOTES_TABLE, Field.COMPETENCES_NOTES_TABLE);
        this.structureOptionsService = structureOptionsService;
        this.sql = sql;
    }

    @Override
    public void createCompetenceNote(final JsonObject competenceNote, final String idUser, final Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " +  resourceTable +
                "( id_devoir, id_competence, evaluation, owner, id_eleve ) " +
                "SELECT id AS id_devoir, ? AS id_competence, ? AS evaluation, owner, ? AS id_eleve " +
                "FROM " + schema + Competences.DEVOIR_TABLE +
                " WHERE " + Competences.DEVOIR_TABLE + ".id = ? ON CONFLICT ( id_devoir, id_competence, id_eleve ) " +
                " DO UPDATE SET evaluation = ? RETURNING id ;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(competenceNote.getInteger("id_competence"))
                .add(competenceNote.getInteger("evaluation"))
                .add(competenceNote.getString("id_eleve"))
                .add(competenceNote.getLong("id_devoir"))
                .add(competenceNote.getInteger("evaluation"));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
    private void add(JsonObject competenceNote, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.create(competenceNote, user, handler);
    }

    private void update(JsonObject competenceNote, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + resourceTable + " SET evaluation = ? " +
                "WHERE id_competence = ? AND id_devoir = ? AND id_eleve = ?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(competenceNote.getLong("evaluation"))
                .add(competenceNote.getLong("id_competence"))
                .add(competenceNote.getLong("id_devoir"))
                .add(competenceNote.getString("id_eleve"));

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }


    @Override
    public void updateCompetenceNote(String id, JsonObject competenceNote, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.update(id, competenceNote, user, handler);
    }

    @Override
    public void deleteCompetenceNote(String id, Handler<Either<String, JsonObject>> handler) {
        super.delete(id, handler);
    }

    @Override
    public void getCompetencesNotes(Long idDevoir, String idEleve, Boolean returnNotEvaluatedcompetences,
                                    Long idCycle, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT ").append(table).append(".*,competences.nom as nom, competences.id_type as id_type, ")
                .append(" competences.id_parent as id_parent, ")
                .append(" competence_niveau_final.niveau_final AS niveau_final  ")
                .append(" FROM ").append(resourceTable).append(", ").append(schema).append("devoirs, ").append(schema).append("competences " )

                // Jointure pour le niveau final
                .append(" LEFT JOIN ").append(schema).append("competence_niveau_final ON ")
                .append(" competence_niveau_final.id_competence = competences.id ")
                .append(" AND competence_niveau_final.id_eleve = ? ")

                .append(" WHERE ").append(table).append(".id_competence = competences.id ")
                .append(" AND ").append(table).append(".id_devoir = ? AND ").append(table).append(".id_eleve = ? ")
                .append(" AND devoirs.id = ").append(table).append(".id_devoir ")
                .append(" AND (devoirs.id_matiere = competence_niveau_final.id_matiere " )
                .append(" OR  competence_niveau_final.id_matiere IS NULL) ");

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(idEleve).add(idDevoir).add(idEleve);
        if (idCycle != null) {
            query.append(" AND competences.id_cycle = ?");
            params.add(idCycle);
        }
        if (returnNotEvaluatedcompetences) {
            query.append(" UNION SELECT null as id, competences_devoirs.id_devoir, ")
                    .append(" competences_devoirs.id_competence, null as evaluation, ")
                    .append(" null as owner, ? as id_eleve,null as created, null as modified, competences.nom as nom, ")
                    .append(" competences.id_type as id_type, competences.id_parent as id_parent,  ")
                    .append(" competence_niveau_final.niveau_final AS niveau_final  ")
                    .append(" FROM ").append(schema).append("competences_devoirs, ")
                    .append(schema).append("devoirs, ")
                    .append(schema).append("competences ")

                    // Jointure pour le niveau final
                    .append(" LEFT JOIN ").append(schema).append("competence_niveau_final ON ")
                    .append(" competence_niveau_final.id_competence = competences.id ")
                    .append(" AND competence_niveau_final.id_eleve = ? ")

                    .append(" WHERE competences_devoirs.id_competence = competences.id  ")
                    .append(" AND devoirs.id = competences_devoirs.id_devoir  ")
                    .append(" AND (devoirs.id_matiere = competence_niveau_final.id_matiere ")
                    .append("       OR  competence_niveau_final.id_matiere IS NULL) ")
                    .append(" AND competences_devoirs.id_devoir = ? AND    ")
                    .append(" NOT competences_devoirs.id_competence IN  ")
                    .append(" (SELECT id_competence FROM ").append(resourceTable )
                    .append(" WHERE ").append(table).append(".id_eleve = ? AND ").append(table).append(".id_devoir = ? )");
            params.add(idEleve).add(idEleve).add(idDevoir).add(idEleve).add(idDevoir);
            if (idCycle != null) {
                query.append(" AND competences.id_cycle = ?");
                params.add(idCycle);
            }

        }

        query.append(" ORDER BY id ASC ");
        Sql.getInstance().prepared(query.toString(), params, DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

    public void getCompetencesNotes(JsonArray idDevoirs, String idEleve, Boolean returnNotEvaluatedcompetences,
                                    Long idCycle, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        String idDevoirsForQuery = Sql.listPrepared(idDevoirs.getList());

        query.append("SELECT ").append(table).append(".*,competences.nom as nom, competences.id_type as id_type, ")
                .append(" competences.id_parent as id_parent, ")
                .append(" competence_niveau_final.niveau_final AS niveau_final  ")
                .append(" FROM ").append(resourceTable).append(", ")
                .append(schema).append("devoirs, ")
                .append(schema).append("competences ")

                // Jointure pour le niveau final
                .append(" LEFT JOIN ").append(schema).append("competence_niveau_final ON ")
                .append(" competence_niveau_final.id_competence = competences.id ")
                .append(" AND competence_niveau_final.id_eleve = ? ")

                .append(" WHERE ").append(table).append(".id_competence = competences.id ")
                .append(" AND ").append(table).append(".id_devoir IN ").append(idDevoirsForQuery)
                .append(" AND ").append(table).append(".id_eleve = ? ")
                .append(" AND devoirs.id = ").append(table).append(".id_devoir ")
                .append(" AND (devoirs.id_matiere = competence_niveau_final.id_matiere " )
                .append(" OR  competence_niveau_final.id_matiere IS NULL) ");

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(idEleve).addAll(idDevoirs).add(idEleve);
        if (idCycle != null) {
            query.append(" AND competences.id_cycle = ?");
            params.add(idCycle);
        }
        if (returnNotEvaluatedcompetences) {
            query.append(" UNION SELECT null as id, competences_devoirs.id_devoir, ")
                    .append(" competences_devoirs.id_competence, null as evaluation, ")
                    .append(" null as owner, ? as id_eleve,null as created, null as modified, competences.nom as nom, ")
                    .append(" competences.id_type as id_type, competences.id_parent as id_parent,  ")
                    .append(" competence_niveau_final.niveau_final AS niveau_final  ")
                    .append(" FROM ").append(schema).append("competences_devoirs, ")
                    .append(schema).append("devoirs, ")
                    .append(schema).append("competences ")

                    // Jointure pour le niveau final
                    .append(" LEFT JOIN ").append(schema).append("competence_niveau_final ON ")
                    .append(" competence_niveau_final.id_competence = competences.id ")
                    .append(" AND competence_niveau_final.id_eleve = ? ")

                    .append(" WHERE competences_devoirs.id_competence = competences.id  ")
                    .append(" AND devoirs.id = competences_devoirs.id_devoir  ")
                    .append(" AND (devoirs.id_matiere = competence_niveau_final.id_matiere ")
                    .append("       OR  competence_niveau_final.id_matiere IS NULL) ")
                    .append(" AND competences_devoirs.id_devoir  IN ").append(idDevoirsForQuery)
                    .append(" AND  NOT competences_devoirs.id_competence IN  ")
                    .append(" (SELECT id_competence FROM ").append(resourceTable)
                    .append("  WHERE ").append(table).append(".id_eleve = ? AND ").append(table).append(".id_devoir IN ")
                    .append(idDevoirsForQuery).append(" )");
            params.add(idEleve).add(idEleve).addAll(idDevoirs).add(idEleve).addAll(idDevoirs);
            if (idCycle != null) {
                query.append(" AND competences.id_cycle = ?");
                params.add(idCycle);
            }
        }

        query.append(" ORDER BY id ASC ");
        Sql.getInstance().prepared(query.toString(), params, DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getCompetencesNotesDevoir(Long idDevoir, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder()
                .append("SELECT C.nom, CN.id, CN.id_devoir, codification, CN.id_eleve, CN.id_competence, CN.evaluation ")
                .append("FROM ").append(resourceTable).append(" CN ")
                .append("INNER JOIN ").append(schema).append("competences C ON CN.id_competence = C.id ")
                .append("INNER JOIN ").append(schema).append("rel_competences_domaines RCD ON RCD.id_competence = C.id ")
                .append("INNER JOIN ").append(schema).append("domaines D ON RCD.id_domaine = D.id ")
                .append("WHERE CN.id_devoir = ? ");

        Sql.getInstance().prepared(query.toString(),
                new JsonArray().add(idDevoir), DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

    @Override
    public void updateCompetencesNotesDevoir(JsonArray _datas, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < _datas.size(); i++) {
            JsonObject o = _datas.getJsonObject(i);
            query.append("UPDATE ").append(resourceTable).append(" SET evaluation = ?, modified = now() WHERE id = ?;");
            values.add(o.getLong("evaluation")).add(o.getInteger("id"));
        }
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void createCompetencesNotesDevoir(JsonArray _datas, String idUser, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < _datas.size(); i++) {
            JsonObject o = _datas.getJsonObject(i);
            query.append("INSERT INTO ").append(resourceTable)
                    .append(" (id_devoir, id_competence, evaluation, owner, id_eleve, created) ")
                    .append(" VALUES (?, ?, ?, ?, ?, now()) ")
                    .append(" ON CONFLICT (id_devoir, id_competence, id_eleve) DO UPDATE SET evaluation = ? ;");
            values.add(o.getInteger("id_devoir")).add(o.getInteger("id_competence"))
                    .add(o.getInteger("evaluation"))
                    .add(idUser).add(o.getString("id_eleve"))
                    .add(o.getInteger("evaluation"));
        }
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void dropCompetencesNotesDevoir(JsonArray oIdsJsonArray, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

            query.append("DELETE FROM ").append(resourceTable)
                    .append(" WHERE id IN ").append(Sql.listPrepared(oIdsJsonArray.getList())).append(";");
        Sql.getInstance().prepared(query.toString(), oIdsJsonArray, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getMaxOrAverageCompetencesNotesClasse(List<String> idEleves, Long idPeriode, Boolean isSkillAverage,
                                                      Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT ").append(table).append(".id_eleve AS id_eleve, competences.id as id_competence, ");
        if(Boolean.TRUE.equals(isSkillAverage))
            query.append("ROUND(AVG(").append(table).append(".evaluation),2) as evaluation, ");
        else
            query.append("MAX(" ).append(table).append( ".evaluation) as evaluation, ");
        query.append("competence_niveau_final.niveau_final ,")
                .append("competence_niveau_final_annuel.niveau_final AS niveau_final_annuel, ")
                .append("rel_competences_domaines.id_domaine, devoirs.id_matiere, ").append(table).append(".owner ")
                .append("FROM ").append(schema).append("competences ")
                .append("INNER JOIN ").append(schema).append("rel_competences_domaines")
                .append(" ON (competences.id = rel_competences_domaines.id_competence) ")
                .append("INNER JOIN ").append(resourceTable)
                .append(" ON (" ).append(table).append(".id_competence = competences.id AND ").append(table).append(".id_eleve IN (");

        for (int i=0; i<idEleves.size()-1 ; i++){
            query.append("?,");
            values.add(idEleves.get(i));
        }
        query.append("?)) ");
        values.add(idEleves.get(idEleves.size()-1));

        query.append("INNER JOIN ").append(schema).append("devoirs ON (")
                .append(table).append(".id_devoir = devoirs.id) AND devoirs.eval_lib_historise = false ");

        if (idPeriode != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(idPeriode);
        }

        query.append("INNER JOIN ").append(schema).append("type ON (type.id = devoirs.id_type) ");
        query.append("LEFT JOIN ").append(schema).append("competence_niveau_final ")
                .append("ON (competence_niveau_final.id_eleve = ").append(table).append(".id_eleve ")
                .append("AND competence_niveau_final.id_periode = devoirs.id_periode ")
                .append("AND competence_niveau_final.id_competence = competences.id ")
                .append("AND competence_niveau_final.id_matiere= devoirs.id_matiere) ")
                .append("LEFT JOIN ").append(schema).append("competence_niveau_final_annuel ")
                .append("ON (competence_niveau_final_annuel.id_eleve = ").append(table).append(".id_eleve ")
                .append("AND competence_niveau_final_annuel.id_competence = competences.id ")
                .append("AND competence_niveau_final_annuel.id_matiere= devoirs.id_matiere) ")
                .append("WHERE type.formative = false ")
                .append("GROUP BY competences.id, competences.id_cycle, rel_competences_domaines.id_domaine, ")
                .append(table).append(".id_eleve, ").append(table).append(".owner, competence_niveau_final.niveau_final, ")
                .append("competence_niveau_final_annuel.niveau_final, devoirs.id_matiere ");

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getConversionNoteCompetence(String idEtablissement, String idClasse, Handler<Either<String,JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT valmin, valmax, coalesce(perso.libelle, niv.libelle) as libelle, ordre, niv.couleur, bareme_brevet ")
                .append("FROM notes.niveau_competences AS niv ")
                .append("INNER JOIN  ").append(schema).append("echelle_conversion_niv_note AS echelle ON niv.id = echelle.id_niveau ")
                .append("INNER JOIN  ").append(schema).append("rel_groupe_cycle CC ON cc.id_cycle = niv.id_cycle ")
                .append("AND cc.id_groupe = ? ")
                .append("AND echelle.id_structure = ? ")
                .append("LEFT JOIN (SELECT * FROM ").append(schema).append("perso_niveau_competences ")
                .append("WHERE id_etablissement = ?) AS perso ON (perso.id_niveau = niv.id) ")
                .append("ORDER BY  ordre DESC");
        values.add(idClasse);
        values.add(idEtablissement);
        values.add(idEtablissement);
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    public Future<JsonArray> getConversionNoteCompetence(String structureId, String classId) {

        Promise<JsonArray> conversionTable = Promise.promise();
        getConversionNoteCompetence(structureId, classId,
                FutureHelper.handlerJsonArray(conversionTable, String.format (
                        "[Comptences@%s :: getConversionNoteCompetence]  error during resquest : ",
                        this.getClass().getSimpleName())));

        return conversionTable.future();
    }


    @Override
    public void getConversionTableByClass(String idEtablissement, List<String> idsClasses, Boolean hasClassList, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT id_groupe, json_agg(json_build_object(\'valmin\',valmin,\'valmax\', valmax, \'libelle\', libelle,")
                .append("\'ordre\', ordre,\'couleur\', couleur,\'bareme_brevet\', bareme_brevet )) as table_conversion ")
                .append("FROM notes.niveau_competences AS niv ")
                .append("INNER JOIN  ").append(schema).append("echelle_conversion_niv_note AS echelle ON niv.id = echelle.id_niveau ")
                .append("INNER JOIN  ").append(schema).append("rel_groupe_cycle CC ON cc.id_cycle = niv.id_cycle ")
                .append("AND cc.id_groupe IN ").append(Sql.listPrepared(idsClasses))
                .append(" AND echelle.id_structure = ? ")
                .append("GROUP BY  id_groupe");
        for(String idClass : idsClasses){
            values.add(idClass);
        }

        values.add(idEtablissement);
        Sql.getInstance().prepared(query.toString(), values, DELIVERY_OPTIONS,
                SqlResult.validResultHandler(handler));
    }

    /**
     * Récupère la note maximale pour chaque compétence de chaque élève dont l'id est passé en paramètre.
     * pour une période ou pour l'année
     * @param idEleves  id des élèves
     * @param idPeriode id de la période dont on souhaite récupérer les notes, peut être null pour sélectionner l'année
     * @param isYear    afin de savoir si on récupère les notes de toutes l'année ou pas,
     * @param handler   handler portant le résultat de la requête
     */
    public void getMaxOrAverageCompetenceNoteEleveByPeriod (String[] idEleves, Long idPeriode, Boolean isYear,
                                                            Boolean isSkillAverage,
                                                            Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT ").append(table).append(".id_eleve, rel_competences_domaines.id_domaine,")
                .append("competences.id as id_competence,");
        if(isSkillAverage)
            query.append("ROUND(AVG(").append(table).append(".evaluation),2) as evaluation,");
        else query.append("MAX(" ).append(table).append( ".evaluation) as evaluation, ");
        query.append( "competence_niveau_final.niveau_final AS niveau_final,devoirs.id_matiere, devoirs.owner ");

        if(idPeriode == null) {
            query.append(", competence_niveau_final_annuel.niveau_final AS niveau_final_annuel ");
        }

        query.append("FROM ").append(resourceTable)
                .append(" INNER JOIN ").append(schema).append("rel_competences_domaines ON ")
                .append(table).append(".id_competence = rel_competences_domaines.id_competence ")
                .append("INNER JOIN ").append(schema).append("competences ON ")
                .append(table).append(".id_competence = competences.id ")
                .append("INNER JOIN ").append(schema).append("devoirs ON ")
                .append(table).append(".id_devoir = devoirs.id ")
                .append("INNER JOIN ").append(schema).append("type ON (type.id = devoirs.id_type) ")
                .append("LEFT JOIN ").append(schema).append("competence_niveau_final ")
                .append("ON (competence_niveau_final.id_periode = devoirs.id_periode ")
                .append("AND competence_niveau_final.id_eleve = ").append(table).append(".id_eleve ")
                .append("AND competence_niveau_final.id_competence = competences.id ")
                .append("AND competence_niveau_final.id_matiere = devoirs.id_matiere ) ");

        if(idPeriode == null) {
            query.append("LEFT JOIN ").append(schema).append("competence_niveau_final_annuel ")
                    .append("ON (competence_niveau_final_annuel.id_competence = competences.id ")
                    .append("AND competence_niveau_final_annuel.id_eleve = ").append(table).append(".id_eleve ")
                    .append("AND competence_niveau_final_annuel.id_matiere = devoirs.id_matiere ) ");
        }

        query.append("WHERE type.formative = false ")
                .append("AND ").append(table).append(".id_eleve IN ")
                .append(Sql.listPrepared(idEleves)).append(" AND evaluation >= 0 ");

        for(String s : idEleves) {
            values.add(s);
        }

        query.append(" AND (devoirs.eval_lib_historise = false ) ");

        if(idPeriode != null) {
            query.append("AND devoirs.id_periode = ? AND devoirs.owner <> 'id-user-transition-annee'");
            values.add(idPeriode);
        }else if(isYear){
            query.append("AND devoirs.owner <> 'id-user-transition-annee'");
        }

        query.append(" GROUP BY ").append(table).append(".id_eleve, competences.id, competences.id_cycle,")
                .append("rel_competences_domaines.id_domaine, ")
                .append("devoirs.id_matiere, competence_niveau_final.niveau_final, devoirs.owner");

        if(idPeriode == null) {
            query.append(", competence_niveau_final_annuel.niveau_final");
        }

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getCompetencesNotesEleve(String idEleve, Long idPeriode, Long idCycle, boolean isCycle, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT DISTINCT competences.id as id_competence, competences.id_parent,")
                .append("competences.id_type, competences.id_cycle, ")
                .append(table).append(".id as id_competences_notes, ").append(table).append(".evaluation, ")
                .append(table ).append(".owner, ")
                .append(table).append(".created, devoirs.name as evaluation_libelle, devoirs.date as evaluation_date,")
                .append("devoirs.id_matiere AS id_matiere, rel_competences_domaines.id_domaine, devoirs.id_type AS id_typeEval, " )
                .append(table).append(".id_devoir AS id_devoir, type.formative AS formative ")
                .append(", competence_niveau_final.niveau_final AS niveau_final  ")
                .append(", competence_niveau_final_annuel.niveau_final AS niveau_final_annuel  ")
                .append(", devoirs.eval_lib_historise as eval_lib_historise, users.username as owner_name ")
                .append("FROM ").append(schema).append("competences ")

                .append("INNER JOIN ").append(schema).append("rel_competences_domaines ")
                .append("ON (competences.id = rel_competences_domaines.id_competence) ")
                .append("INNER JOIN ").append(resourceTable).append(" ON (").append(table).append(".id_competence = competences.id) ")
                .append("INNER JOIN ").append(schema).append("devoirs ON (").append(table).append(".id_devoir = devoirs.id) ")
                .append("INNER JOIN ").append(schema).append("type ON (type.id = devoirs.id_type) ")
                .append("INNER JOIN ").append(schema).append("users ON users.id = ").append(table).append(".owner ")

                .append("LEFT JOIN ").append(schema).append("competence_niveau_final ")
                .append("ON (competence_niveau_final.id_competence = competences.id ")
                .append("AND competence_niveau_final.id_periode = devoirs.id_periode ")
                .append("AND competence_niveau_final.id_eleve = ").append(table).append(".id_eleve ")
                .append("AND competence_niveau_final.id_matiere = devoirs.id_matiere )")

                .append("LEFT JOIN ").append(schema).append("competence_niveau_final_annuel ")
                .append("ON (competence_niveau_final_annuel.id_competence = competences.id ")
                .append("AND competence_niveau_final_annuel.id_eleve = ").append(table).append(".id_eleve ")
                .append("AND competence_niveau_final_annuel.id_matiere = devoirs.id_matiere )")

                .append("WHERE ").append(table).append(".id_eleve = ? AND evaluation >= 0 ");
        values.add(idEleve);
        if (idPeriode != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(idPeriode);
        }
        if (idCycle != null) {
            query.append("AND competences.id_cycle = ? ");
            values.add(idCycle);
        }
        if(!isCycle) {
            query.append("AND devoirs.eval_lib_historise = false ");
        }
        query.append("ORDER BY ").append(table).append(".created ");

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getMaxOrAverageCompetenceNoteEleveByCycle (String[] idEleves, Long idCycle, Boolean isSkillAverage,
                                                  Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT ").append(table).append(".id_eleve, competences.id as id_competence, ");
        if(isSkillAverage) {
            query.append("ROUND(AVG(").append(table).append(".evaluation),2) as evaluation,")
                    .append("ROUND(AVG(competence_niveau_final.niveau_final),2) AS niveau_final, ");
        } else {
            query.append("MAX(" ).append(table).append(".evaluation) as evaluation,")
                    .append("MAX(competence_niveau_final.niveau_final) AS niveau_final, ");
        }
        query.append("devoirs.id_matiere, devoirs.owner ,competence_niveau_final_annuel.niveau_final AS niveau_final_annuel ")
                .append("FROM ").append(resourceTable)
                .append(" INNER JOIN ").append(schema).append("competences ON ")
                .append(table).append(".id_competence = competences.id ")
                .append("INNER JOIN ").append(schema).append("devoirs ON ")
                .append(table).append(".id_devoir = devoirs.id ")
                .append("INNER JOIN ").append(schema).append("type ON (type.id = devoirs.id_type) ")
                .append("LEFT JOIN ").append(schema).append("competence_niveau_final ")
                .append("ON (competence_niveau_final.id_periode = devoirs.id_periode ")
                .append("AND competence_niveau_final.id_eleve = ").append(table).append(".id_eleve ")
                .append("AND competence_niveau_final.id_competence = competences.id AND competence_niveau_final.id_matiere = devoirs.id_matiere ) ")
                .append("LEFT JOIN ").append(schema).append("competence_niveau_final_annuel ")
                .append("ON (competence_niveau_final_annuel.id_competence = competences.id ")
                .append("AND competence_niveau_final_annuel.id_eleve = ").append(table).append(".id_eleve ")
                .append("AND competence_niveau_final_annuel.id_matiere = devoirs.id_matiere ) ")
                .append("WHERE  type.formative = FALSE AND ").append(table).append(".id_eleve IN ")
                .append(Sql.listPrepared(idEleves)).append(" AND evaluation >= 0 ")
                .append("AND competences.id_cycle = ? AND devoirs.owner <> 'id-user-transition-annee'");

        for(String s : idEleves) {
            values.add(s);
        }
        values.add(idCycle);

        query.append(" GROUP BY ").append(table).append(".id_eleve, competences.id, competences.id_cycle, ")
                .append("devoirs.id_matiere, devoirs.owner, competence_niveau_final_annuel.niveau_final")
                .append(" UNION ") // les dernières evaluations archivees
                .append("SELECT ").append(table).append(".id_eleve,competences.id AS id_competence, ")
                .append(table).append(".evaluation,")
                .append(" NULL AS niveau_final, devoirs.id_matiere, devoirs.owner, NULL AS niveau_final_annual ")
                .append("FROM ").append(resourceTable)
                .append(" INNER JOIN ").append(schema).append("competences ")
                .append("ON ").append(table).append(".id_competence = competences.id ")
                .append("INNER JOIN ").append(schema).append("devoirs ON ").append(table).append(".id_devoir = devoirs.id ")
                .append("INNER JOIN ")
                .append("(SELECT MAX (devoirs.created) AS created,").append(table).append(".id_eleve,competences.id AS id_competence ")
                .append("FROM ").append(resourceTable)
                .append(" INNER JOIN ").append(schema).append("competences ON ").append(table).append(".id_competence = competences.id ")
                .append("INNER JOIN ").append(schema).append("devoirs ON ").append(table).append(".id_devoir = devoirs.id ")
                .append("WHERE ").append(table).append(".id_eleve IN ").append(Sql.listPrepared(idEleves))
                .append(" AND competences.id_cycle = ? AND devoirs.owner = 'id-user-transition-annee' ")
                .append("GROUP BY ").append(table).append(".id_eleve,competences.id ) AS maxDate ")
                .append("ON maxDate.id_competence= ").append(table).append(".id_competence ")
                .append("AND maxDate.created = devoirs.created AND maxDate.id_eleve = ").append(table).append(".id_eleve ");
        for(String s : idEleves) {
            values.add(s);
        }
        values.add(idCycle);

        query.append("WHERE ").append(table).append(".id_eleve IN ").append(Sql.listPrepared(idEleves))
                .append(" AND evaluation >= 0 AND competences.id_cycle = ? AND devoirs.owner = 'id-user-transition-annee' ")
                .append("ORDER BY id_eleve, id_competence");
        for(String s : idEleves) {
            values.add(s);
        }
        values.add(idCycle);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getMaxBaremeMapOrderBaremeBrevet(String idEtablissement, String idClasse,
                                                 final Handler<Either<String,Map<Integer, Map<Integer, Integer>>>> handler) {
        getConversionNoteCompetence(idEtablissement, idClasse, repNivCompetence ->{
           if(repNivCompetence.isRight()){
               JsonArray niveauxCompetences = repNivCompetence.right().getValue();
               Map<Integer,Integer> mapOrdreBareme = new HashMap<>();
               Integer maxBaremeBrevet = 0;
               for(int i=0; i< niveauxCompetences.size(); i++ ){
                    JsonObject nivCompetence = niveauxCompetences.getJsonObject(i);
                    mapOrdreBareme.put(nivCompetence.getInteger("ordre"),nivCompetence.getInteger("bareme_brevet"));
                    if(maxBaremeBrevet < nivCompetence.getInteger("bareme_brevet"))
                        maxBaremeBrevet = nivCompetence.getInteger("bareme_brevet");
               }
               Map<Integer,Map<Integer,Integer>> mapMaxBaremeMapOrdreBareme = new HashMap<>();
               mapMaxBaremeMapOrdreBareme.put(maxBaremeBrevet,mapOrdreBareme);
               handler.handle(new Either.Right<>(mapMaxBaremeMapOrdreBareme));
            }else{
                handler.handle(new Either.Left<>(
                        "erreur lors de la récupération des niveaux de compétence"));
                log.error("getMapOrderBaremeBrevet: getConversionNoteCompetence : " + repNivCompetence.left().getValue());
            }
        });
    }
    public void getCyclesEleve(String idEleve, Handler<Either<String, JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT cycle.id AS id_cycle, cycle.libelle ")
                .append("FROM ").append(schema).append("cycle ")
                .append("INNER JOIN ").append(schema).append("competences ")
                .append("ON competences.id_cycle = cycle.id ")
                .append("INNER JOIN ").append(resourceTable)
                .append(" ON competences.id = ").append(table).append(".id_competence ")
                .append("WHERE ").append(table).append(".id_eleve = ? ")
                .append("GROUP BY cycle.id; ");
        values.add(idEleve);
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public Future<AchievementsProgress> getStudentSubjectsSkillsValidatedPercentage(String structureId, String studentId,
                                                                                    Long periodId, String groupId) {
        Promise<AchievementsProgress> promise = Promise.promise();
        this.getStudentsSubjectsSkillsValidatedPercentage(structureId, Collections.singletonList(studentId), periodId, groupId)
                .onFailure(promise::fail)
                .onSuccess(achievements -> {
                    if (achievements.isEmpty()) {
                        promise.complete(new AchievementsProgress(structureId, studentId));
                        return;
                    }
                    // when we filter on one studentId, we can only have one Achievements
                    promise.complete(achievements.get(0));
                });
        return promise.future();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Future<List<AchievementsProgress>> getStudentsSubjectsSkillsValidatedPercentage(String structureId, List<String> studentIds,
                                                                                           Long periodId, String groupId) {
        Promise<List<AchievementsProgress>> promise = Promise.promise();
        structureOptionsService.isAverageSkills(structureId)
                .compose(isAverageSkills ->
                        getSubjectSkillsValidatedPercentageRequest(structureId, studentIds, periodId, groupId,
                                isAverageSkills))
                .onFailure(promise::fail)
                .onSuccess(subjectsSkillsValidatedPercentage ->
                        promise.complete(formatSubjectSkillsValidatedPercentage(structureId,
                                subjectsSkillsValidatedPercentage.getList())));
        return promise.future();
    }

    private List<AchievementsProgress> formatSubjectSkillsValidatedPercentage(String structureId,
                                                                              List<JsonObject> subjectsSkillsValidatedPercentage) {
        return new ArrayList<>(subjectsSkillsValidatedPercentage.stream()
                .collect(Collectors.toMap(
                        // We regroup each achievements per student_id
                        subjectSkillsValidatedPercentage -> subjectSkillsValidatedPercentage.getString(Field.STUDENT_ID),
                        subjectSkillsValidatedPercentage -> initAchievements(structureId, subjectSkillsValidatedPercentage),
                        this::mergeSubjectsAchievements
                )).values());
    }

    private AchievementsProgress initAchievements(String structureId, JsonObject subjectSkillsValidatedPercentage) {
        return new AchievementsProgress(subjectSkillsValidatedPercentage)
                .structureId(structureId)
                .addAchievementsSubject(subjectSkillsValidatedPercentage);
    }

    private AchievementsProgress mergeSubjectsAchievements(AchievementsProgress subjectSkillsValidatedPercentage1,
                                                           AchievementsProgress subjectSkillsValidatedPercentage2) {
        return subjectSkillsValidatedPercentage1.addAchievementsSubjects(subjectSkillsValidatedPercentage2.achievementsSubjects());
    }


    private Future<JsonArray> getSubjectSkillsValidatedPercentageRequest(String structureId, List<String> studentIds,
                                                                         Long periodId, String groupId,
                                                                         Boolean isAverageSkills) {
        Promise<JsonArray> promise = Promise.promise();
        JsonArray params = new JsonArray();

        String request = " SELECT id_eleve as student_id, id_matiere as subject_id, " +
                " (SUM(is_validated::int) * 100) / COUNT(id_competence) as skills_validated_percentage " +
                " FROM ( " +
                getSubjectSkillsIsValidatedQuery(structureId, studentIds, periodId, groupId, isAverageSkills, params) +
                " )  as is_validated_skills_by_subjects " +
                " GROUP BY id_eleve, id_matiere";

        sql.prepared(request, params,
                SqlResult.validResultHandler(FutureHelper.handler(promise,
                        String.format("[Competences@%s::getSubjectSkillsValidatedPercentageRequest] " +
                                "Fail to retrieve SubjectSkillsValidatedPercentage types assigners", this.getClass().getSimpleName()))));

        return promise.future();
    }

    private String getSubjectSkillsIsValidatedQuery(String structureId, List<String> studentIds,
                                                    Long periodId, String groupId, Boolean isAverageSkills,
                                                    JsonArray params) {
        return " SELECT cn.id_eleve, d.id_matiere, cn.id_competence, " +
                String.format(" %s(COALESCE(cnf.niveau_final, cn.evaluation)) > 1 as is_validated ",
                        Boolean.TRUE.equals(isAverageSkills) ? "AVG" : "MAX") +
                String.format(" FROM %s.%s cn ", Field.NOTES_TABLE, Field.COMPETENCES_NOTES_TABLE) +
                String.format(" INNER JOIN %s.%s d on d.id = cn.id_devoir ", Field.NOTES_TABLE, Field.DEVOIRS_TABLE) +
                String.format(" INNER JOIN %s.%s rdg on d.id = rdg.id_devoir ", Field.NOTES_TABLE, Field.REL_DEVOIRS_GROUPES_TABLE) +
                String.format(" INNER JOIN %s.%s t on d.id_type = t.id ", Field.NOTES_TABLE, Field.TYPE_TABLE) +
                String.format(" LEFT JOIN %s.%s cnf ", Field.NOTES_TABLE, Field.COMPETENCE_NIVEAU_FINAL) +
                " on d.id_matiere = cnf.id_matiere AND cn.id_eleve = cnf.id_eleve AND cn.id_competence = cnf.id_competence " +
                getSubjectSkillsIsValidatedQueryFilters(structureId, studentIds, periodId, groupId, params) +
                " GROUP BY cn.id_eleve, d.id_matiere, cn.id_competence";
    }

    private String getSubjectSkillsIsValidatedQueryFilters(String structureId, List<String> studentIds,
                                                           Long periodId, String groupId, JsonArray params) {
        String queryFilter = String.format(" WHERE d.id_etablissement = ? AND cn.id_eleve IN %s ", Sql.listPrepared(studentIds)) +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL " +
                " AND t.formative IS FALSE ";

        params.add(structureId)
                .addAll(new JsonArray(studentIds));

        if (periodId != null) {
            queryFilter += " AND d.id_periode = ? ";
            params.add(periodId);
        }

        if (groupId != null) {
            queryFilter += " AND rdg.type_groupe = 1 AND rdg.id_groupe = ? ";
            params.add(groupId);
        }

        return queryFilter;
    }
}
