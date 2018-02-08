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
import fr.openent.competences.service.CompetencesService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultCompetencesService extends SqlCrudService implements CompetencesService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultCompetencesService.class);

    private static final String COMPETENCES_PERSO_TABLE = Competences.COMPETENCES_SCHEMA
            + "." + Competences.PERSO_COMPETENCES_TABLE;

    public DefaultCompetencesService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void getCompetencesItem(final String idEtablissement, final String idClasse,
                                   final Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT rel_competences_domaines.id_domaine, competences.* , competences.nom as nomHtml, ")
                .append(" perso.nom as name, perso.masque ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences ")
                .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_competences_domaines ")
                .append(" ON (competences.id = rel_competences_domaines.id_competence) ")
                .append(" LEFT OUTER JOIN " + COMPETENCES_PERSO_TABLE  )
                .append("  AS perso ")
                .append(" ON ( competences.id = perso.id_competence AND  perso.id_etablissement = ? )" )
                .append(" WHERE  (competences.id_etablissement IS null OR competences.id_etablissement = ? ) AND ");

        if (null != idClasse) {
            query.append(" competences.id_cycle = (SELECT id_cycle FROM " + Competences.COMPETENCES_SCHEMA +
                    ".rel_groupe_cycle WHERE id_groupe = ? ) AND");
        }

        query.append(" NOT EXISTS ( ")
                .append("SELECT 1 ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences AS competencesChildren ")
                .append("WHERE competencesChildren.id_parent = competences.id ");

        if (null != idClasse) {
            query.append("AND competences.id_cycle = (SELECT id_cycle FROM " + Competences.COMPETENCES_SCHEMA +
                    ".rel_groupe_cycle WHERE id_groupe = ?) ");
        }

        query.append(") ")
                .append("ORDER BY id_cycle, competences.nom ASC,  perso.nom ASC ");

        JsonArray params = new JsonArray().addString(idEtablissement).addString(idEtablissement);
        if (null != idClasse) {
            params.addString(idClasse);
            params.addString(idClasse);
        }

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getCompetences(Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT competences.id, competences.nom, competences.description, competences.id_type, competences.id_parent, type_competences.nom as type, competences.id_cycle ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences ")
                .append("WHERE competences.id_type = type_competences.id ")
                .append("ORDER BY competences.id ASC");
        Sql.getInstance().prepared(query.toString(), new JsonArray(), SqlResult.validResultHandler(handler));
    }

    @Override
    public void setDevoirCompetences(Long devoirId, JsonArray values, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray data = new JsonArray();
        query.append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".competences_devoirs (id_devoir, id_competence) VALUES ");
        for(int i = 0; i < values.size(); i++){
            query.append("(?, ?)");
            data.addNumber(devoirId);
            data.addNumber((Number) values.get(i));
            if(i != values.size()-1){
                query.append(",");
            }else{
                query.append(";");
            }
        }

        Sql.getInstance().prepared(query.toString(), data, SqlResult.validRowsResultHandler(handler));
    }
    @Override
    public void remDevoirCompetences(Long devoirId, JsonArray values, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray data = new JsonArray();
        query.append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".competences_devoirs WHERE ");
        for(int i = 0; i < values.size(); i++){
            query.append("(id_devoir = ? AND  id_competence = ?)");
            data.addNumber(devoirId);
            data.addNumber((Number) values.get(i));
            if(i != values.size()-1){
                query.append(" OR ");
            }else{
                query.append(";");
            }
        }

        Sql.getInstance().prepared(query.toString(), data, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getDevoirCompetences(Long devoirId, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT string_agg(domaines.codification, ', ') as code_domaine, ")
                .append("string_agg( cast (domaines.id as text), ',') as ids_domaine, competences.id as id_competence,")
                .append(" competences_devoirs.*, competences.nom as nom, competences.id_type as id_type, ")
                .append(" competences.id_parent as id_parent, competences_devoirs.index as index ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences ")
                .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".competences_devoirs ON (competences.id = competences_devoirs.id_competence ) ")
                .append("LEFT OUTER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_competences_domaines ON (competences.id = rel_competences_domaines.id_competence) ")
                .append("LEFT OUTER JOIN "+ Competences.COMPETENCES_SCHEMA +".domaines ON (domaines.id = rel_competences_domaines.id_domaine) ")
                .append("WHERE competences_devoirs.id_devoir = ? ")
                .append("GROUP BY competences_devoirs.id, competences.nom, competences.id_type, competences.id_parent, competences.id ")
                .append("ORDER BY (competences_devoirs.index ,competences_devoirs.id);");

        Sql.getInstance().prepared(query.toString(), new JsonArray().addNumber(devoirId), SqlResult.validResultHandler(handler));
    }


    @Override
    public void getLastCompetencesDevoir(String userId, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT competences_devoirs.*, competences.nom as nom ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences_devoirs, "+ Competences.COMPETENCES_SCHEMA +".competences ")
                .append("WHERE competences_devoirs.id_competence = competences.id ")
                .append("AND id_devoir IN ")
                .append("(SELECT id FROM "+ Competences.COMPETENCES_SCHEMA +".devoirs WHERE devoirs.owner = ? ORDER BY devoirs.created DESC LIMIT 1);");

        Sql.getInstance().prepared(query.toString(), new JsonArray().addString(userId), SqlResult.validResultHandler(handler));
    }

    @Override
    public void getSousCompetences(Long skillId, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT * ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences ")
                .append("WHERE competences.id_parent = ?;");

        Sql.getInstance().prepared(query.toString(), new JsonArray().addNumber(skillId), SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCompetencesEnseignement(Long teachingId, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT * ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences ")
                .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_competences_enseignements ON (competences.id = rel_competences_enseignements.id_competence) ")
                .append("WHERE rel_competences_enseignements.id_enseignement = ? ")
                .append("AND competences.id_parent = 0 ;");

        Sql.getInstance().prepared(query.toString(), new JsonArray().addNumber(teachingId), SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCompetencesByLevel(final String idEtablissement, final String filter, final String idClasse,
                                      final Handler<Either<String, JsonArray>> handler) {

        StringBuilder query = new StringBuilder();
        JsonArray params = new JsonArray().addString(idEtablissement).addString(idEtablissement);

        query.append("SELECT DISTINCT string_agg(domaines.codification, ', ') as code_domaine, ")
                .append(" string_agg( cast (domaines.id as text), ',') as ids_domaine, competences.id, ")
                .append(" competences.nom, competences.id_parent, competences.id_type, ")
                .append(" rel_competences_enseignements.id_enseignement, competences.id_cycle, perso.nom as name, perso.masque ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".competences ")
                .append("INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_competences_enseignements ON (competences.id = rel_competences_enseignements.id_competence) ");

        if (idClasse != null) {
            query.append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle ON (rel_groupe_cycle.id_cycle = competences.id_cycle) ");
        }

        query.append("LEFT OUTER JOIN "+ Competences.COMPETENCES_SCHEMA +".rel_competences_domaines ON (competences.id = rel_competences_domaines.id_competence) ")
                .append("LEFT OUTER JOIN "+ Competences.COMPETENCES_SCHEMA +".domaines ON (domaines.id = rel_competences_domaines.id_domaine) ")
                .append(" LEFT OUTER JOIN "+ COMPETENCES_PERSO_TABLE + " AS perso")
                .append(" ON ( competences.id = perso.id_competence AND perso.id_etablissement = ? ) ")
                .append(" WHERE  (competences.id_etablissement is null OR competences.id_etablissement = ? ) AND ")
                .append(" competences."+ filter);

        if (idClasse != null) {
            query.append(" AND rel_groupe_cycle.id_groupe = ? ");
            params.addString(idClasse);
        }

        query.append(" GROUP BY perso.nom, perso.masque, competences.id, competences.nom, competences.id_parent, competences.id_type, rel_competences_enseignements.id_enseignement, competences.id_cycle ")
                .append("ORDER BY competences.nom ASC, perso.nom ASC;");

        Sql.getInstance().prepared(query.toString(), params , SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCompetencesDomaines(Long[] idDomaines, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new JsonArray();

        query.append("SELECT * FROM " + Competences.COMPETENCES_SCHEMA + ".rel_competences_domaines WHERE id_domaine IN " + Sql.listPrepared(idDomaines));

        for(Long l : idDomaines) {
            params.addNumber(l);
        }

        Sql.getInstance().prepared(query.toString(), params , SqlResult.validResultHandler(handler));
    }

    @Override
    public void delete(String idEtablissement, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();

        // SUPPRESSION DE COMPETENCES MANUELLES
        StringBuilder query = new StringBuilder().append(" DELETE FROM "+ Competences.COMPETENCES_SCHEMA )
                .append(".competences WHERE id_etablissement = ? ");
        JsonArray params = new JsonArray();
        params.addString(idEtablissement);
        statements.add(new JsonObject()
                .putString("statement", query.toString())
                .putArray("values", params)
                .putString("action", "prepared"));

        // SSUPPRESSION D'INFO PERSO
        StringBuilder query_perso = new StringBuilder().append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA )
                .append(".perso_competences WHERE id_etablissement = ? ");
        JsonArray params_perso = new JsonArray();

        params_perso.addString(idEtablissement);
        statements.add(new JsonObject()
                .putString("statement", query_perso.toString())
                .putArray("values", params_perso)
                .putString("action", "prepared"));
        Sql.getInstance().transaction(statements, SqlResult.validRowsResultHandler(handler));

    }
}
