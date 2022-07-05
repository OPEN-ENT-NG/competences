
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
import fr.wseduc.webutils.Either;
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

import static fr.openent.competences.Competences.DELIVERY_OPTIONS;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultCompetenceNoteService extends SqlCrudService implements fr.openent.competences.service.CompetenceNoteService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultCompetenceNoteService.class);

    public DefaultCompetenceNoteService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void createCompetenceNote(final JsonObject competenceNote, final String idUser, final Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " +  Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_NOTES_TABLE +
                "( id_devoir, id_competence, evaluation, owner, id_eleve ) " +
                "SELECT id AS id_devoir, ? AS id_competence, ? AS evaluation, owner, ? AS id_eleve " +
                "FROM "+ Competences.COMPETENCES_SCHEMA + "." + Field.DEVOIR_TABLE +
                " WHERE " + Field.DEVOIR_TABLE + ".id = ? ON CONFLICT ( id_devoir, id_competence, id_eleve ) " +
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
        String query = "UPDATE notes." + Field.COMPETENCES_NOTES_TABLE + " SET evaluation = ? " +
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

        query.append("SELECT " + Field.COMPETENCES_NOTES_TABLE + ".*," + Field.COMPETENCES_TABLE + ".nom as nom, " + Field.COMPETENCES_TABLE + ".id_type as id_type, ")
                .append(Field.COMPETENCES_TABLE + ".id_parent as id_parent, ")
                .append(Field.COMPETENCE_NIVEAU_FINAL + ".niveau_final AS niveau_final  ")
                .append(" FROM "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_NOTES_TABLE + ", ")
                .append( Competences.COMPETENCES_SCHEMA +"." + Field.DEVOIR_TABLE + ", ")
                .append( Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_TABLE)

                // Jointure pour le niveau final
                .append(" LEFT JOIN notes." + Field.COMPETENCE_NIVEAU_FINAL + " ON ")
                .append(Field.COMPETENCE_NIVEAU_FINAL + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                .append(" AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_eleve = ? ")

                .append(" WHERE " + Field.COMPETENCES_NOTES_TABLE + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                .append(" AND " + Field.COMPETENCES_NOTES_TABLE + ".id_devoir = ? AND " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve = ? ")
                .append(" AND " + Field.DEVOIR_TABLE + ".id = " + Field.COMPETENCES_NOTES_TABLE + ".id_devoir ")
                .append(" AND (" + Field.DEVOIR_TABLE + ".id_matiere = " + Field.COMPETENCE_NIVEAU_FINAL + ".id_matiere " )
                .append(" OR " + Field.COMPETENCE_NIVEAU_FINAL + ".id_matiere IS NULL) ");

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(idEleve).add(idDevoir).add(idEleve);
        if (idCycle != null) {
            query.append(" AND " + Field.COMPETENCES_TABLE + ".id_cycle = ?");
            params.add(idCycle);
        }
        if (returnNotEvaluatedcompetences) {
            query.append(" UNION SELECT null as id, " + Field.COMPETENCES_DEVOIRS + ".id_devoir, ")
                    .append(Field.COMPETENCES_DEVOIRS + ".id_competence, null as evaluation, ")
                    .append(" null as owner, ? as id_eleve,null as created, null as modified, " + Field.COMPETENCES_TABLE + ".nom as nom, ")
                    .append(Field.COMPETENCES_TABLE + ".id_type as id_type, " + Field.COMPETENCES_TABLE + ".id_parent as id_parent,  ")
                    .append(Field.COMPETENCE_NIVEAU_FINAL + ".niveau_final AS niveau_final  ")
                    .append(" FROM "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_DEVOIRS + ", ")
                    .append( Competences.COMPETENCES_SCHEMA +"." + Field.DEVOIR_TABLE + ", ")
                    .append( Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_TABLE)

                    // Jointure pour le niveau final
                    .append(" LEFT JOIN notes." + Field.COMPETENCE_NIVEAU_FINAL + " ON ")
                    .append(Field.COMPETENCE_NIVEAU_FINAL + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                    .append(" AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_eleve = ? ")

                    .append(" WHERE " + Field.COMPETENCES_DEVOIRS + ".id_competence = " + Field.COMPETENCES_TABLE + ".id  ")
                    .append(" AND " + Field.DEVOIR_TABLE + ".id = " + Field.COMPETENCES_DEVOIRS + ".id_devoir  ")
                    .append(" AND (" + Field.DEVOIR_TABLE + ".id_matiere = " + Field.COMPETENCE_NIVEAU_FINAL + ".id_matiere ")
                    .append("       OR " + Field.COMPETENCE_NIVEAU_FINAL + ".id_matiere IS NULL) ")
                    .append(" AND " + Field.COMPETENCES_DEVOIRS + ".id_devoir = ? AND    ")
                    .append(" NOT " + Field.COMPETENCES_DEVOIRS + ".id_competence IN  ")
                    .append(" (SELECT id_competence FROM notes." + Field.COMPETENCES_NOTES_TABLE)
                    .append("  WHERE " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve = ? AND " + Field.COMPETENCES_NOTES_TABLE + ".id_devoir = ? )");
            params.add(idEleve).add(idEleve).add(idDevoir).add(idEleve).add(idDevoir);
            if (idCycle != null) {
                query.append(" AND " + Field.COMPETENCES_TABLE + ".id_cycle = ?");
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

        query.append("SELECT " + Field.COMPETENCES_NOTES_TABLE + ".*, " + Field.COMPETENCES_TABLE + ".nom as nom, " + Field.COMPETENCES_TABLE + ".id_type as id_type, ")
                .append(Field.COMPETENCES_TABLE + ".id_parent as id_parent, ")
                .append(Field.COMPETENCE_NIVEAU_FINAL + ".niveau_final AS niveau_final  ")
                .append(" FROM "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_NOTES_TABLE + ", ")
                .append( Competences.COMPETENCES_SCHEMA +"." + Field.DEVOIR_TABLE + ", ")
                .append( Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_TABLE)

                // Jointure pour le niveau final
                .append(" LEFT JOIN notes." + Field.COMPETENCE_NIVEAU_FINAL + " ON ")
                .append(Field.COMPETENCE_NIVEAU_FINAL + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                .append(" AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_eleve = ? ")

                .append(" WHERE " + Field.COMPETENCES_NOTES_TABLE + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                .append(" AND " + Field.COMPETENCES_NOTES_TABLE + ".id_devoir IN " + idDevoirsForQuery)
                .append(" AND " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve = ? ")
                .append(" AND " + Field.DEVOIR_TABLE + ".id = " + Field.COMPETENCES_NOTES_TABLE + ".id_devoir ")
                .append(" AND (" + Field.DEVOIR_TABLE + ".id_matiere = " + Field.COMPETENCE_NIVEAU_FINAL + ".id_matiere " )
                .append(" OR  " + Field.COMPETENCE_NIVEAU_FINAL + ".id_matiere IS NULL) ");

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(idEleve).addAll(idDevoirs).add(idEleve);
        if (idCycle != null) {
            query.append(" AND " + Field.COMPETENCES_TABLE + ".id_cycle = ?");
            params.add(idCycle);
        }
        if (returnNotEvaluatedcompetences) {
            query.append(" UNION SELECT null as id, " + Field.COMPETENCES_DEVOIRS + ".id_devoir, ")
                    .append(Field.COMPETENCES_DEVOIRS + ".id_competence, null as evaluation, ")
                    .append(" null as owner, ? as id_eleve,null as created, null as modified, " + Field.COMPETENCES_TABLE + ".nom as nom, ")
                    .append(Field.COMPETENCES_TABLE + ".id_type as id_type, " + Field.COMPETENCES_TABLE + ".id_parent as id_parent,  ")
                    .append(Field.COMPETENCE_NIVEAU_FINAL + ".niveau_final AS niveau_final  ")
                    .append(" FROM "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_DEVOIRS + ", ")
                    .append( Competences.COMPETENCES_SCHEMA +"." + Field.DEVOIR_TABLE + ", ")
                    .append( Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_TABLE + " ")

                    // Jointure pour le niveau final
                    .append(" LEFT JOIN notes." + Field.COMPETENCE_NIVEAU_FINAL + " ON ")
                    .append(Field.COMPETENCE_NIVEAU_FINAL + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                    .append(" AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_eleve = ? ")

                    .append(" WHERE " + Field.COMPETENCES_DEVOIRS + ".id_competence = " + Field.COMPETENCES_TABLE + ".id  ")
                    .append(" AND " + Field.DEVOIR_TABLE + ".id = " + Field.COMPETENCES_DEVOIRS + ".id_devoir  ")
                    .append(" AND (" + Field.DEVOIR_TABLE + ".id_matiere = " + Field.COMPETENCE_NIVEAU_FINAL + ".id_matiere ")
                    .append("       OR  " + Field.COMPETENCE_NIVEAU_FINAL + ".id_matiere IS NULL) ")
                    .append(" AND " + Field.COMPETENCES_DEVOIRS + ".id_devoir  IN " + idDevoirsForQuery)
                    .append(" AND  NOT " + Field.COMPETENCES_DEVOIRS + ".id_competence IN  ")
                    .append(" (SELECT id_competence FROM notes." + Field.COMPETENCES_NOTES_TABLE)
                    .append("  WHERE " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve = ? AND " + Field.COMPETENCES_NOTES_TABLE + ".id_devoir IN ")
                    .append(idDevoirsForQuery + " )");
            params.add(idEleve).add(idEleve).addAll(idDevoirs).add(idEleve).addAll(idDevoirs);
            if (idCycle != null) {
                query.append(" AND " + Field.COMPETENCES_TABLE + ".id_cycle = ?");
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
                .append("FROM ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCES_NOTES_TABLE + " CN ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCES_TABLE + " C ON CN.id_competence = C.id ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".rel_competences_domaines RCD ON RCD.id_competence = C.id ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".domaines D ON RCD.id_domaine = D.id ")
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
            query.append("UPDATE "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_NOTES_TABLE + " SET evaluation = ?, modified = now() WHERE id = ?;");
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
            query.append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_NOTES_TABLE)
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

            query.append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_NOTES_TABLE + " WHERE id IN " + Sql.listPrepared(oIdsJsonArray.getList()) + ";");
        Sql.getInstance().prepared(query.toString(), oIdsJsonArray, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCompetencesNotesClasse(List<String> idEleves, Long idPeriode, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve AS id_eleve, " + Field.COMPETENCES_TABLE + ".id as id_competence, " +
                        "max(" + Field.COMPETENCES_NOTES_TABLE + ".evaluation) as evaluation , " + Field.COMPETENCE_NIVEAU_FINAL + ".niveau_final ," +
                        Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".niveau_final AS niveau_final_annuel, " +
                        "rel_competences_domaines.id_domaine, " + Field.DEVOIR_TABLE + ".id_matiere, " + Field.COMPETENCES_NOTES_TABLE + ".owner ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_TABLE)
                .append(" INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_competences_domaines ON (" + Field.COMPETENCES_TABLE + ".id = rel_competences_domaines.id_competence)")
                .append(" INNER JOIN "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_NOTES_TABLE +
                        " ON (" + Field.COMPETENCES_NOTES_TABLE + ".id_competence = " + Field.COMPETENCES_TABLE + ".id" +
                        " AND " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve IN (");

        for (int i=0; i<idEleves.size()-1 ; i++){
            query.append("?,");
            values.add(idEleves.get(i));
        }
        query.append("?)) ");
        values.add(idEleves.get(idEleves.size()-1));

        query.append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +"." + Field.DEVOIR_TABLE +
                " ON (" + Field.COMPETENCES_NOTES_TABLE + ".id_devoir = " + Field.DEVOIR_TABLE + ".id)" +
                " AND " + Field.DEVOIR_TABLE + ".eval_lib_historise = false ");

        if (idPeriode != null) {
            query.append("AND " + Field.DEVOIR_TABLE + ".id_periode = ? ");
            values.add(idPeriode);
        }

        query.append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".type ON (type.id = " + Field.DEVOIR_TABLE + ".id_type) ");
        query.append("LEFT JOIN "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCE_NIVEAU_FINAL +
                " ON (" + Field.COMPETENCE_NIVEAU_FINAL + ".id_eleve = " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve " +
                "AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_periode = " + Field.DEVOIR_TABLE + ".id_periode " +
                "AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_competence = " + Field.COMPETENCES_TABLE + ".id " +
                "AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_matiere= " + Field.DEVOIR_TABLE + ".id_matiere) ");
        query.append("LEFT JOIN "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL +
                "ON (" + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_eleve = " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve " +
                "AND " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_competence = " + Field.COMPETENCES_TABLE + ".id " +
                "AND " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_matiere= " + Field.DEVOIR_TABLE + ".id_matiere) ");
        query.append("WHERE type.formative = false ");
        query.append("GROUP BY " + Field.COMPETENCES_TABLE + ".id, " + Field.COMPETENCES_TABLE + ".id_cycle, rel_competences_domaines.id_domaine, " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve, " +
                Field.COMPETENCES_NOTES_TABLE + ".owner, " + Field.COMPETENCE_NIVEAU_FINAL + ".niveau_final, " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".niveau_final, " + Field.DEVOIR_TABLE + ".id_matiere ");

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCompetencesNotesDomaineClasse(List<String> idEleves, Long idPeriode, List<String> idDomaines, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve AS id_eleve," +
                        Field.COMPETENCES_TABLE + ".id as id_competence," +
                        " max(" + Field.COMPETENCES_NOTES_TABLE + ".evaluation) as evaluation," +
                        " rel_competences_domaines.id_domaine, " + Field.COMPETENCES_NOTES_TABLE + ".owner ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_TABLE)
                .append(" INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_competences_domaines ON (" + Field.COMPETENCES_TABLE + ".id = rel_competences_domaines.id_competence" +
                        " AND rel_competences_domaines.id_domaine IN ( " );

        for (int i=0; i<idDomaines.size()-1 ; i++){
            query.append("?,");
            values.add(Integer.valueOf(idDomaines.get(i)));
        }
        query.append("?)) ");
        values.add(Integer.valueOf(idDomaines.get(idDomaines.size()-1)));

        query.append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +"." + Field.COMPETENCES_NOTES_TABLE +
                " ON (" + Field.COMPETENCES_NOTES_TABLE + ".id_competence = " + Field.COMPETENCES_TABLE + ".id" +
                " AND " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve IN (");

        for (int i=0; i<idEleves.size()-1 ; i++){
            query.append("?,");
            values.add(idEleves.get(i));
        }
        query.append("?)) ");
        values.add(idEleves.get(idEleves.size()-1));

        query.append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +"." + Field.DEVOIR_TABLE +
                " ON (" + Field.COMPETENCES_NOTES_TABLE + ".id_devoir = " + Field.DEVOIR_TABLE + ".id) ");

        if (idPeriode != null) {
            query.append("AND " + Field.DEVOIR_TABLE + ".id_periode = ? ");
            values.add(idPeriode);
        }
        query.append("GROUP BY " + Field.COMPETENCES_TABLE + ".id, " + Field.COMPETENCES_TABLE + ".id_cycle,rel_competences_domaines.id_domaine, "
                + Field.COMPETENCES_NOTES_TABLE + ".id_eleve, " + Field.COMPETENCES_NOTES_TABLE + ".owner ");

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getConversionNoteCompetence(String idEtablissement, String idClasse, Handler<Either<String,JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT valmin, valmax, coalesce(perso.libelle, niv.libelle) as libelle, ordre, niv.couleur, bareme_brevet ")
                .append("FROM notes.niveau_competences AS niv ")
                .append("INNER JOIN  " + Competences.COMPETENCES_SCHEMA + ".echelle_conversion_niv_note AS echelle ON niv.id = echelle.id_niveau ")
                .append("INNER JOIN  " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle CC ON cc.id_cycle = niv.id_cycle ")
                .append("AND cc.id_groupe = ? ")
                .append("AND echelle.id_structure = ? ")
                .append("LEFT JOIN (SELECT * FROM " + Competences.COMPETENCES_SCHEMA + ".perso_niveau_competences ")
                .append("WHERE id_etablissement = ?) AS perso ON (perso.id_niveau = niv.id) ")
                .append("ORDER BY  ordre DESC");
        values.add(idClasse);
        values.add(idEtablissement);
        values.add(idEtablissement);
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getConversionTableByClass(String idEtablissement, List<String> idsClasses, Boolean hasClassList, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT id_groupe, json_agg(json_build_object(\'valmin\',valmin,\'valmax\', valmax, \'libelle\', libelle," +
                        "\'ordre\', ordre,\'couleur\', couleur,\'bareme_brevet\', bareme_brevet )) as table_conversion ")
                .append("FROM notes.niveau_competences AS niv ")
                .append("INNER JOIN  " + Competences.COMPETENCES_SCHEMA + ".echelle_conversion_niv_note AS echelle ON niv.id = echelle.id_niveau ")
                .append("INNER JOIN  " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle CC ON cc.id_cycle = niv.id_cycle ")
                .append("AND cc.id_groupe IN " + Sql.listPrepared(idsClasses))
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
    @Override
    public void getMaxCompetenceNoteEleveByPeriod (String[] idEleves, Long idPeriode, Boolean isYear, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve, rel_competences_domaines.id_domaine, " + Field.COMPETENCES_TABLE + ".id as id_competence, max(" + Field.COMPETENCES_NOTES_TABLE + ".evaluation) as evaluation, ")
                .append(Field.COMPETENCE_NIVEAU_FINAL + ".niveau_final AS niveau_final, " + Field.DEVOIR_TABLE + ".id_matiere, " + Field.DEVOIR_TABLE + ".owner ");

        if(idPeriode == null) {
            query.append(", " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".niveau_final AS niveau_final_annuel ");
        }

        query.append("FROM ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCES_NOTES_TABLE)
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".rel_competences_domaines ON " + Field.COMPETENCES_NOTES_TABLE + ".id_competence = rel_competences_domaines.id_competence")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCES_TABLE + " ON " + Field.COMPETENCES_NOTES_TABLE + ".id_competence = " + Field.COMPETENCES_TABLE + ".id")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.DEVOIR_TABLE + " ON " + Field.COMPETENCES_NOTES_TABLE + ".id_devoir = " + Field.DEVOIR_TABLE + ".id")
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".type ON (type.id = " + Field.DEVOIR_TABLE + ".id_type)")
                .append( "LEFT JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCE_NIVEAU_FINAL)
                .append(" ON (" + Field.COMPETENCE_NIVEAU_FINAL + ".id_periode = " + Field.DEVOIR_TABLE + ".id_periode AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_eleve = " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve")
                .append(" AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_competence = " + Field.COMPETENCES_TABLE + ".id AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_matiere = " + Field.DEVOIR_TABLE + ".id_matiere ) ");

        if(idPeriode == null) {
            query.append("LEFT JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCE_NIVEAU_FINAL)
                    .append("ON (" + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                    .append("AND " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_eleve = competences_notes.id_eleve ")
                    .append("AND " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_matiere = " + Field.DEVOIR_TABLE + ".id_matiere ) ");
        }

        query.append("WHERE type.formative = false ")
                .append("AND " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve IN ").append(Sql.listPrepared(idEleves)).append(" AND evaluation >= 0 ");

        for(String s : idEleves) {
            values.add(s);
        }

        query.append(" AND (" + Field.DEVOIR_TABLE + ".eval_lib_historise = false ) ");

        if(idPeriode != null) {
            query.append("AND " + Field.DEVOIR_TABLE + ".id_periode = ? AND " + Field.DEVOIR_TABLE + ".owner <> 'id-user-transition-annee'");
            values.add(idPeriode);
        }else if(isYear){
            query.append("AND " + Field.DEVOIR_TABLE + ".owner <> 'id-user-transition-annee'");
        }

        query.append(" GROUP BY " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve, " + Field.COMPETENCES_TABLE + ".id, " + Field.COMPETENCES_TABLE + ".id_cycle,rel_competences_domaines.id_domaine, ")
                .append(Field.DEVOIR_TABLE + ".id_matiere, " + Field.COMPETENCE_NIVEAU_FINAL + ".niveau_final, " + Field.DEVOIR_TABLE + ".owner");

        if(idPeriode == null) {
            query.append(", " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".niveau_final");
        }

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getCompetencesNotesEleve(String idEleve, Long idPeriode, Long idCycle, boolean isCycle, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT DISTINCT " + Field.COMPETENCES_TABLE + ".id as id_competence, " + Field.COMPETENCES_TABLE + ".id_parent, " + Field.COMPETENCES_TABLE + ".id_type, " + Field.COMPETENCES_TABLE + ".id_cycle, ")
                .append(Field.COMPETENCES_NOTES_TABLE + ".id as id_competences_notes, " + Field.COMPETENCES_NOTES_TABLE + ".evaluation, " + Field.COMPETENCES_NOTES_TABLE + ".owner, ")
                .append(Field.COMPETENCES_NOTES_TABLE + ".created, " + Field.DEVOIR_TABLE + ".name as evaluation_libelle, " + Field.DEVOIR_TABLE + ".date as evaluation_date, ")
                .append(Field.DEVOIR_TABLE + ".id_matiere AS id_matiere, rel_competences_domaines.id_domaine, " + Field.DEVOIR_TABLE + ".id_type AS id_typeEval, " + Field.COMPETENCES_NOTES_TABLE + ".id_devoir AS id_devoir,")
                .append(" type.formative AS formative ")
                .append(", " + Field.COMPETENCE_NIVEAU_FINAL + ".niveau_final AS niveau_final  ")
                .append(", " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".niveau_final AS niveau_final_annuel  ")
                .append(", " + Field.DEVOIR_TABLE + ".eval_lib_historise as eval_lib_historise, users.username as owner_name ")
                .append("FROM notes." +Field.COMPETENCES_TABLE)

                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".rel_competences_domaines ON (" + Field.COMPETENCES_TABLE + ".id = rel_competences_domaines.id_competence) ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCES_NOTES_TABLE + " ON (" + Field.COMPETENCES_NOTES_TABLE + ".id_competence = " + Field.COMPETENCES_TABLE + ".id) ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.DEVOIR_TABLE + " ON (" + Field.COMPETENCES_NOTES_TABLE + ".id_devoir = " + Field.DEVOIR_TABLE + ".id) ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".type ON (type.id = " + Field.DEVOIR_TABLE + ".id_type) ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".users ON users.id = " + Field.COMPETENCES_NOTES_TABLE + ".owner ")

                .append("LEFT JOIN notes." + Field.COMPETENCE_NIVEAU_FINAL + " ON (" + Field.COMPETENCE_NIVEAU_FINAL + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                .append("AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_periode = " + Field.DEVOIR_TABLE + ".id_periode ")
                .append("AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_eleve = " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve ")
                .append("AND " + Field.COMPETENCE_NIVEAU_FINAL + ".id_matiere = " + Field.DEVOIR_TABLE + ".id_matiere )")

                .append("LEFT JOIN notes." + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + " ON (" + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                .append("AND " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_eleve = " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve ")
                .append("AND " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_matiere = " + Field.DEVOIR_TABLE + ".id_matiere )")

                .append("WHERE " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve = ? AND evaluation >= 0 ");
        values.add(idEleve);
        if (idPeriode != null) {
            query.append("AND " + Field.DEVOIR_TABLE + ".id_periode = ? ");
            values.add(idPeriode);
        }
        if (idCycle != null) {
            query.append("AND " + Field.COMPETENCES_TABLE + ".id_cycle = ? ");
            values.add(idCycle);
        }
        if(!isCycle) {
            query.append("AND " + Field.DEVOIR_TABLE + ".eval_lib_historise = false ");
        }
        query.append("ORDER BY " + Field.COMPETENCES_NOTES_TABLE + ".created ");

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getMaxCompetenceNoteEleveByCycle (String[] idEleves, Long idCycle, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve, " + Field.COMPETENCES_TABLE + ".id as id_competence, ")
                .append("MAX(" + Field.COMPETENCES_NOTES_TABLE + ".evaluation) as evaluation, MAX(" + Field.COMPETENCE_NIVEAU_FINAL + ".niveau_final) AS niveau_final, ")
                .append(Field.DEVOIR_TABLE + ".id_matiere, " + Field.DEVOIR_TABLE + ".owner ," + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".niveau_final AS niveau_final_annuel ")
                .append("FROM ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCES_NOTES_TABLE)
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCES_TABLE + " ON " + Field.COMPETENCES_NOTES_TABLE + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.DEVOIR_TABLE + " ON " + Field.COMPETENCES_NOTES_TABLE + ".id_devoir = " + Field.DEVOIR_TABLE + ".id ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".type ON (type.id = " + Field.DEVOIR_TABLE + ".id_type) ")
                .append("LEFT JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCE_NIVEAU_FINAL)
                .append("ON (" + Field.COMPETENCE_NIVEAU_FINAL + ".id_periode = " + Field.DEVOIR_TABLE + ".id_periode AND competence_niveau_final.id_eleve = " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve ")
                .append("AND competence_niveau_final.id_competence = " + Field.COMPETENCES_TABLE + ".id AND competence_niveau_final.id_matiere = " + Field.DEVOIR_TABLE + ".id_matiere ) ")
                .append("LEFT JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL)
                .append("ON (" + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                .append("AND " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_eleve = " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve ")
                .append("AND " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".id_matiere = " + Field.DEVOIR_TABLE + ".id_matiere ) ")
                .append("WHERE  type.formative = FALSE AND " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve IN ").append(Sql.listPrepared(idEleves))
                .append(" AND evaluation >= 0 ")
                .append("AND " + Field.COMPETENCES_TABLE + ".id_cycle = ? AND " + Field.DEVOIR_TABLE + ".owner <> 'id-user-transition-annee'");

        for(String s : idEleves) {
            values.add(s);
        }
        values.add(idCycle);

        query.append(" GROUP BY " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve, " + Field.COMPETENCES_TABLE + ".id, " + Field.COMPETENCES_TABLE + ".id_cycle, ")
                .append(Field.COMPETENCES_DEVOIRS + ".id_matiere, " + Field.DEVOIR_TABLE + ".owner, " + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL + ".niveau_final")
                .append(" UNION ") // les dernières evaluations archivees
                .append("SELECT " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve," + Field.COMPETENCES_TABLE + ".id AS id_competence, " + Field.COMPETENCES_NOTES_TABLE + ".evaluation,")
                .append(" NULL AS niveau_final, " + Field.DEVOIR_TABLE + ".id_matiere, " + Field.DEVOIR_TABLE + ".owner, NULL AS niveau_final_annual ")
                .append("FROM ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCES_NOTES_TABLE)
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCES_TABLE + " ON " + Field.COMPETENCES_NOTES_TABLE + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.DEVOIR_TABLE + " ON " + Field.COMPETENCES_NOTES_TABLE + ".id_devoir = " + Field.DEVOIR_TABLE + ".id ")
                .append("INNER JOIN ")
                .append("(SELECT MAX (" + Field.DEVOIR_TABLE + ".created) AS created, " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve," + Field.COMPETENCES_TABLE + ".id AS id_competence ")
                .append("FROM ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCES_NOTES_TABLE)
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.COMPETENCES_TABLE + " ON " + Field.COMPETENCES_NOTES_TABLE + ".id_competence = " + Field.COMPETENCES_TABLE + ".id ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append("." + Field.DEVOIR_TABLE + " ON " + Field.COMPETENCES_NOTES_TABLE + ".id_devoir = " + Field.DEVOIR_TABLE + ".id ")
                .append("WHERE " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve IN ").append(Sql.listPrepared(idEleves))
                .append(" AND " + Field.COMPETENCES_TABLE + ".id_cycle = ? AND " + Field.DEVOIR_TABLE + ".owner = 'id-user-transition-annee' ")
                .append("GROUP BY " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve," + Field.COMPETENCES_TABLE + ".id ) AS maxDate ON maxDate.id_competence= " + Field.COMPETENCES_NOTES_TABLE + ".id_competence ")
                .append("AND maxDate.created = " + Field.DEVOIR_TABLE + ".created AND maxDate.id_eleve = " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve ");
        for(String s : idEleves) {
            values.add(s);
        }
        values.add(idCycle);

        query.append("WHERE " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve IN ").append(Sql.listPrepared(idEleves))
                .append(" AND evaluation >= 0 AND " + Field.COMPETENCES_TABLE + ".id_cycle = ? AND " + Field.DEVOIR_TABLE + ".owner = 'id-user-transition-annee' ")
                .append("ORDER BY id_eleve, id_competence");
        for(String s : idEleves) {
            values.add(s);
        }
        values.add(idCycle);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getMaxBaremeMapOrderBaremeBrevet(String idEtablissement, String idClasse,final Handler<Either<String,Map<Integer, Map<Integer, Integer>>>> handler) {
        getConversionNoteCompetence(idEtablissement, idClasse, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> repNivCompetence) {
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
                    handler.handle(new Either.Right<String,Map<Integer, Map<Integer, Integer>>>(mapMaxBaremeMapOrdreBareme));
                }else{
                    handler.handle(new Either.Left<String,Map<Integer, Map<Integer, Integer>>>("erreur lors de la récupération des niveaux de compétence"));
                    log.error("getMapOrderBaremeBrevet: getConversionNoteCompetence : " + repNivCompetence.left().getValue());
                }
            }
        });
    }
    public void getCyclesEleve(String idEleve, Handler<Either<String, JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT " + Field.CYCLE_TABLE + ".id AS id_cycle, " + Field.CYCLE_TABLE + ".libelle ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + "." + Field.CYCLE_TABLE)
                .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + "." + Field.COMPETENCES_TABLE)
                .append(" ON " + Field.COMPETENCES_TABLE + ".id_cycle = " + Field.CYCLE_TABLE + ".id")
                .append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + "." + Field.COMPETENCES_NOTES_TABLE)
                .append(" ON " + Field.COMPETENCES_TABLE + ".id = " + Field.COMPETENCES_NOTES_TABLE + ".id_competence")
                .append(" WHERE " + Field.COMPETENCES_NOTES_TABLE + ".id_eleve = ?")
                .append(" GROUP BY " + Field.CYCLE_TABLE + ".id; ");
        values.add(idEleve);
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }
}
