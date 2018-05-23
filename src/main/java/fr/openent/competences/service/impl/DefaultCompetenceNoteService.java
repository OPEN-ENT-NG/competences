
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
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultCompetenceNoteService extends SqlCrudService implements fr.openent.competences.service.CompetenceNoteService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultCompetenceNoteService.class);

    public DefaultCompetenceNoteService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void createCompetenceNote(final JsonObject competenceNote, final UserInfos user, final Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT id FROM " +  Competences.COMPETENCES_SCHEMA +".competences_notes " +
                "WHERE id_competence = ? AND id_devoir = ? AND id_eleve = ?;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(competenceNote.getInteger("id_competence"))
                .add(competenceNote.getLong("id_devoir"))
                .add(competenceNote.getString("id_eleve"));
        Sql.getInstance().prepared(query, params, new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> result) {
                JsonArray values = result.body().getJsonArray("results");
                if (values.size() == 0) {
                    add(competenceNote, user, handler);
                } else {
                    update(competenceNote, handler);
                }
            }
        });
    }

    private void add(JsonObject competenceNote, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.create(competenceNote, user, handler);
    }

    private void update(JsonObject competenceNote, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE notes.competences_notes SET evaluation = ? " +
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
    public void getCompetencesNotes(Long idDevoir, String idEleve, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT competences_notes.*,competences.nom as nom, competences.id_type as id_type, competences.id_parent as id_parent ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences_notes, "+ Competences.COMPETENCES_SCHEMA +".competences ")
                .append("WHERE competences_notes.id_competence = competences.id ")
                .append("AND competences_notes.id_devoir = ? AND competences_notes.id_eleve = ? ")
                .append("ORDER BY competences_notes.id ASC;");

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(idDevoir);
        params.add(idEleve);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCompetencesNotesDevoir(Long idDevoir, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT competences.nom, competences_notes.id, competences_notes.id_devoir, competences_notes.id_eleve, competences_notes.id_competence, competences_notes.evaluation " +
                "FROM "+ Competences.COMPETENCES_SCHEMA +".competences_notes , "+ Competences.COMPETENCES_SCHEMA +".competences " +
                "WHERE competences_notes.id_devoir = ? " +
                "AND competences.id = competences_notes.id_competence");

        Sql.getInstance().prepared(query.toString(), new fr.wseduc.webutils.collections.JsonArray().add(idDevoir), SqlResult.validResultHandler(handler));
    }

    @Override
    public void updateCompetencesNotesDevoir(JsonArray _datas, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < _datas.size(); i++) {
            JsonObject o = _datas.getJsonObject(i);
            query.append("UPDATE "+ Competences.COMPETENCES_SCHEMA +".competences_notes SET evaluation = ?, modified = now() WHERE id = ?;");
            values.add(o.getLong("evaluation")).add(o.getInteger("id"));
        }
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void createCompetencesNotesDevoir(JsonArray _datas, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < _datas.size(); i++) {
            JsonObject o = _datas.getJsonObject(i);
            query.append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".competences_notes (id_devoir, id_competence, evaluation, owner, id_eleve, created) VALUES (?, ?, ?, ?, ?, now());");
            values.add(o.getInteger("id_devoir")).add(o.getInteger("id_competence")).add(o.getInteger("evaluation"))
                    .add(user.getUserId()).add(o.getString("id_eleve"));
        }
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void dropCompetencesNotesDevoir(JsonArray oIdsJsonArray, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        query.append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".competences_notes WHERE id IN " + Sql.listPrepared(Arrays.asList(oIdsJsonArray)) + ";");
        Sql.getInstance().prepared(query.toString(), oIdsJsonArray, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCompetencesNotesEleve(String idEleve, Long idPeriode, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray().add(idEleve);
        StringBuilder query = new StringBuilder()
                .append("SELECT DISTINCT competences.id as id_competence, competences.id_parent, competences.id_type, competences.id_cycle, ")
                .append("competences_notes.id as id_competences_notes, competences_notes.evaluation, competences_notes.owner, competences_notes.created, devoirs.name as evaluation_libelle, devoirs.date as evaluation_date,")
                .append("rel_competences_domaines.id_domaine, ")
                .append("users.username as owner_name, type.formative AS formative ")
                .append("FROM notes.competences ")
                .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_competences_domaines ON (competences.id = rel_competences_domaines.id_competence) ")
                .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".competences_notes ON (competences_notes.id_competence = competences.id) ")
                .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".devoirs ON (competences_notes.id_devoir = devoirs.id) ")
                .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".type ON (type.id = devoirs.id_type) ")
                .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".users ON (users.id = devoirs.owner) ")
                .append("WHERE competences_notes.id_eleve = ? AND evaluation >= 0 ");
        if (idPeriode != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(idPeriode);
        }
        query.append("ORDER BY competences_notes.created ");



        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getCompetencesNotesClasse(List<String> idEleves, Long idPeriode, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
            .append("SELECT competences_notes.id_eleve AS id_eleve, competences.id as id_competence, max(competences_notes.evaluation) as evaluation,rel_competences_domaines.id_domaine, competences_notes.owner ")
            .append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences ")
            .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_competences_domaines ON (competences.id = rel_competences_domaines.id_competence) ")
            .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".competences_notes ON (competences_notes.id_competence = competences.id AND competences_notes.id_eleve IN (");

        for (int i=0; i<idEleves.size()-1 ; i++){
            query.append("?,");
            values.add(idEleves.get(i));
        }
        query.append("?)) ");
        values.add(idEleves.get(idEleves.size()-1));

        query.append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".devoirs ON (competences_notes.id_devoir = devoirs.id) ");

        if (idPeriode != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(idPeriode);
        }

        query.append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".type ON (type.id = devoirs.id_type) ");
        query.append("WHERE type.formative = false ");
        query.append("GROUP BY competences.id, competences.id_cycle,rel_competences_domaines.id_domaine, competences_notes.id_eleve, competences_notes.owner ");

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCompetencesNotesDomaineClasse(List<String> idEleves, Long idPeriode, List<String> idDomaines, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT competences_notes.id_eleve AS id_eleve, competences.id as id_competence, max(competences_notes.evaluation) as evaluation,rel_competences_domaines.id_domaine, competences_notes.owner ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences ")
                .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_competences_domaines ON (competences.id = rel_competences_domaines.id_competence " +
                        "AND rel_competences_domaines.id_domaine IN ( " );

        for (int i=0; i<idDomaines.size()-1 ; i++){
            query.append("?,");
            values.add(Integer.valueOf(idDomaines.get(i)));
        }
        query.append("?)) ");
        values.add(Integer.valueOf(idDomaines.get(idDomaines.size()-1)));

         query.append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".competences_notes ON (competences_notes.id_competence = competences.id AND competences_notes.id_eleve IN (");

        for (int i=0; i<idEleves.size()-1 ; i++){
            query.append("?,");
            values.add(idEleves.get(i));
        }
        query.append("?)) ");
        values.add(idEleves.get(idEleves.size()-1));

        query.append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".devoirs ON (competences_notes.id_devoir = devoirs.id) ");

        if (idPeriode != null) {
            query.append("AND devoirs.id_periode = ? ");
            values.add(idPeriode);
        }
        query.append("GROUP BY competences.id, competences.id_cycle,rel_competences_domaines.id_domaine, competences_notes.id_eleve, competences_notes.owner ");

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getConversionNoteCompetence(String idEtablissement, String idClasse, Handler<Either<String,JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT valmin, valmax, libelle, ordre, couleur, bareme_brevet ")
                .append("FROM notes.niveau_competences AS niv ")
                .append("INNER JOIN  " + Competences.COMPETENCES_SCHEMA + ".echelle_conversion_niv_note AS echelle ON niv.id = echelle.id_niveau ")
                .append("INNER JOIN  " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle CC ON cc.id_cycle = niv.id_cycle ")
                .append("AND cc.id_groupe = ? ")
                .append("AND echelle.id_structure = ? ")
                .append("ORDER BY ordre DESC");
        values.add(idClasse);
        values.add(idEtablissement);
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getMaxCompetenceNoteEleve(String[] id_eleve, Long idPeriode,Long idCycle, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT competences_notes.id_eleve, rel_competences_domaines.id_domaine, competences.id as id_competence, max(competences_notes.evaluation) as evaluation ")
                .append("FROM ").append(Competences.COMPETENCES_SCHEMA).append(".competences_notes ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".rel_competences_domaines ON competences_notes.id_competence = rel_competences_domaines.id_competence ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".competences ON competences_notes.id_competence = competences.id ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".devoirs ON competences_notes.id_devoir = devoirs.id ")
                .append("INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".type ON (type.id = devoirs.id_type) ")
                .append("WHERE type.formative = false ")
                .append("AND competences_notes.id_eleve IN ").append(Sql.listPrepared(id_eleve)).append(" AND evaluation >= 0 ");

        for(String s : id_eleve) {
            values.add(s);
        }


        if(idCycle != null) {
            query.append("AND competences.id_cycle = ? ");
            values.add(idCycle);
        }

        if(idPeriode != null) {
            query.append("AND devoirs.id_periode = ?");
            values.add(idPeriode);
        }

        query.append(" GROUP BY competences_notes.id_eleve, competences.id, competences.id_cycle,rel_competences_domaines.id_domaine");

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
/*
    @Override
    public void getMaxBaremeBrevet(String idEtablissement, String idClasse, Handler<Either<String, JsonObject>> handler) {

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
       String query = "SELECT MAX (bareme_brevet) FROM(SELECT * FROM notes.niveau_competences " +
               "INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".echelle_conversion_niv_note AS echelle ON niv.id = echelle.id_niveau " +
               "INNER JOIN  " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle CC ON cc.id_cycle = niv.id_cycle " +
                "AND cc.id_groupe = ? " +
                "AND echelle.id_structure = ? ) as maxbareme";
        params.add(idClasse);
        params.add(idEtablissement);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));

    }*/
}
