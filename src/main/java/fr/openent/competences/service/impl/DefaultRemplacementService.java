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
import fr.openent.competences.service.RemplacementService;
import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

import static org.entcore.common.sql.SqlResult.validResultHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

public class DefaultRemplacementService extends SqlCrudService implements RemplacementService {

    public DefaultRemplacementService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void listRemplacements(List<String> poListIdEtab, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new JsonArray();
        Object[] oListIdEtabArray = poListIdEtab.toArray();

        query.append("SELECT rel_professeurs_remplacants.*, users_titulaires.username AS libelle_titulaire, users_remplacants.username AS libelle_remplacant ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".rel_professeurs_remplacants ")
                .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".users users_titulaires ON users_titulaires.id = rel_professeurs_remplacants.id_titulaire  ")
                .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".users users_remplacants ON users_remplacants.id = rel_professeurs_remplacants.id_remplacant  ")
                .append("WHERE id_etablissement IN " + Sql.listPrepared(oListIdEtabArray) + " ")
                /*.append("AND current_date <= date_fin")*/; // TODO a décomenter

        for (Object idEab: oListIdEtabArray) {
            values.addString(idEab.toString());
        }

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void createRemplacement(JsonObject poRemplacement, Handler<Either<String, JsonObject>> handler) {
        SqlStatementsBuilder s = new SqlStatementsBuilder();

        // Ajout du titulaire dans la table users s'il n'existe pas
        String userQueryTitulaire = "SELECT "+ Competences.COMPETENCES_SCHEMA+ ".merge_users(?,?)";
        s.prepared(userQueryTitulaire, new JsonArray().add(poRemplacement.getString("id_titulaire")).add(poRemplacement.getString("libelle_titulaire")));

        // Ajout du remplaçant dans la table users s'il n'existe pas
        String userQueryRemplacant = "SELECT "+ Competences.COMPETENCES_SCHEMA+ ".merge_users(?,?)";
        s.prepared(userQueryRemplacant, new JsonArray().add(poRemplacement.getString("id_remplacant")).add(poRemplacement.getString("libelle_remplacant")));


        // Ajout du remplacement
        String remplacementQuery = "INSERT INTO "+ Competences.COMPETENCES_SCHEMA+ ".rel_professeurs_remplacants (id_titulaire, id_remplacant, date_debut, date_fin, id_etablissement) VALUES (?, ?, to_timestamp(?,'YYYY-MM-DD'), to_timestamp(?,'YYYY-MM-DD'), ?);";
        s.prepared(remplacementQuery, new JsonArray().add(poRemplacement.getString("id_titulaire"))
                                                        .add(poRemplacement.getString("id_remplacant"))
                                                        .add(poRemplacement.getString("date_debut"))
                                                        .add(poRemplacement.getString("date_fin"))
                                                        .add(poRemplacement.getString("id_etablissement"))
        );


        Sql.getInstance().transaction(s.build(), validUniqueResultHandler(1, handler));
    }


    @Override
    public void deleteRemplacement(String id_titulaire, String id_remplacant, String date_debut, String date_fin, String id_etablissement, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new JsonArray();

        query.append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".rel_professeurs_remplacants ")
                .append("WHERE id_titulaire = ? ")
                .append("AND id_remplacant = ? ")
                .append("AND date_debut = to_date(?,'YYYY-MM-DD') ")
                .append("AND date_fin = to_date(?,'YYYY-MM-DD') ")
                .append("AND id_etablissement = ? ");

        values.addString(id_titulaire);
        values.addString(id_remplacant);
        values.addString(date_debut);
        values.addString(date_fin);
        values.addString(id_etablissement);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getRemplacementClasse(JsonArray classes, UserInfos user, String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonArray ids = new JsonArray();
        for (int i = 0; i < classes.size(); i++) {
            JsonObject o = classes.get(i);
            if (o.containsField("id_groupe")) ids.addString(o.getString("id_groupe"));
        }
        String query = "MATCH (c:Class) " +
                "WHERE NOT (:User {id: {userId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
                "AND c.id IN {ids} " +
                "RETURN  c.id as id, c.name as name, true as remplacement, 0 as type_groupe";

        query += " UNION ALL ";

        query += "MATCH (c:FunctionalGroup) " +
                "WHERE NOT (:User {id:{userId}})-[:IN]->(c:FunctionalGroup) " +
                "AND c.id IN {ids} " +
                "RETURN  c.id as id, c.name as name, true as remplacement, 1 as type_groupe";

        JsonObject params = new JsonObject()
                .putArray("ids", ids)
                .putString("userId", user.getUserId());

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }
}
