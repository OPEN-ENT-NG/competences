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
import fr.openent.competences.service.DevoirRemplacementService;
import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static org.entcore.common.sql.SqlResult.validResultHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

@Deprecated
public class DefaultDevoirRemplacementService extends SqlCrudService implements DevoirRemplacementService {

    public DefaultDevoirRemplacementService(String schema, String table) {
        super(schema, table);
    }

    @Deprecated
    @Override
    public void listRemplacements(List<String> poListIdEtab, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        Object[] oListIdEtabArray = poListIdEtab.toArray();

        query.append("SELECT rel_professeurs_remplacants.*, users_titulaires.username AS libelle_titulaire, users_remplacants.username AS libelle_remplacant ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".rel_professeurs_remplacants ")
                .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".users users_titulaires ON users_titulaires.id = rel_professeurs_remplacants.id_titulaire  ")
                .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".users users_remplacants ON users_remplacants.id = rel_professeurs_remplacants.id_remplacant  ")
                .append("WHERE id_etablissement IN " + Sql.listPrepared(oListIdEtabArray) + " ")
        /*.append("AND current_date <= date_fin")*/; // TODO a décomenter

        for (Object idEab: oListIdEtabArray) {
            values.add(idEab.toString());
        }

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }
    @Deprecated
    @Override
    public void createRemplacement(JsonObject poRemplacement, Handler<Either<String, JsonObject>> handler) {
        SqlStatementsBuilder s = new SqlStatementsBuilder();

        // Ajout du titulaire dans la table users s'il n'existe pas
        String userQueryTitulaire = "SELECT "+ Competences.COMPETENCES_SCHEMA+ ".merge_users(?,?)";
        s.prepared(userQueryTitulaire, new fr.wseduc.webutils.collections.JsonArray().add(poRemplacement.getString("id_titulaire")).add(poRemplacement.getString("libelle_titulaire")));

        // Ajout du remplaçant dans la table users s'il n'existe pas
        String userQueryRemplacant = "SELECT "+ Competences.COMPETENCES_SCHEMA+ ".merge_users(?,?)";
        s.prepared(userQueryRemplacant, new fr.wseduc.webutils.collections.JsonArray().add(poRemplacement.getString("id_remplacant")).add(poRemplacement.getString("libelle_remplacant")));


        // Ajout du remplacement
        String remplacementQuery = "INSERT INTO "+ Competences.COMPETENCES_SCHEMA+ ".rel_professeurs_remplacants (id_titulaire, id_remplacant, date_debut, date_fin, id_etablissement) VALUES (?, ?, to_timestamp(?,'YYYY-MM-DD'), to_timestamp(?,'YYYY-MM-DD'), ?);";
        s.prepared(remplacementQuery, new fr.wseduc.webutils.collections.JsonArray().add(poRemplacement.getString("id_titulaire"))
                .add(poRemplacement.getString("id_remplacant"))
                .add(poRemplacement.getString("date_debut"))
                .add(poRemplacement.getString("date_fin"))
                .add(poRemplacement.getString("id_etablissement"))
        );


        Sql.getInstance().transaction(s.build(), validUniqueResultHandler(1, handler));
    }

    @Deprecated
    @Override
    public void deleteRemplacement(String id_titulaire, String id_remplacant, String date_debut, String date_fin, String id_etablissement, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".rel_professeurs_remplacants ")
                .append("WHERE id_titulaire = ? ")
                .append("AND id_remplacant = ? ")
                .append("AND date_debut = to_date(?,'YYYY-MM-DD') ")
                .append("AND date_fin = to_date(?,'YYYY-MM-DD') ")
                .append("AND id_etablissement = ? ");

        values.add(id_titulaire);
        values.add(id_remplacant);
        values.add(date_debut);
        values.add(date_fin);
        values.add(id_etablissement);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getClassesIdsDevoir(String userId, String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT distinct(" + Field.REL_DEVOIRS_GROUPES_TABLE + ".id_groupe) " +
                "FROM notes.devoirs " +
                "inner join notes." + Field.REL_DEVOIRS_GROUPES_TABLE + " ON (" + Field.REL_DEVOIRS_GROUPES_TABLE + ".id_devoir = devoirs.id) " +
                "AND (devoirs.id_etablissement = ? ) " +
                "AND (devoirs.owner = ? " +
                "OR devoirs.owner IN (SELECT DISTINCT id_titulaire " +
                "FROM notes.rel_professeurs_remplacants " +
                "INNER JOIN notes.devoirs ON devoirs.id_etablissement = rel_professeurs_remplacants.id_etablissement " +
                "WHERE id_remplacant = ? " +
                "AND rel_professeurs_remplacants.id_etablissement = ?) " +
                "OR ? IN (SELECT member_id " +
                "FROM notes.devoirs_shares " +
                "WHERE resource_id = devoirs.id " +
                "AND action = '"+ Competences.DEVOIR_ACTION_UPDATE +"'))";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(structureId)
                .add(userId)
                .add(userId)
                .add(structureId)
                .add(userId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private void getNeoInfo(JsonArray classes, String userId, String idStructure, Handler<Either<String, JsonArray>> handler){
        JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < classes.size(); i++) {
            JsonObject o = classes.getJsonObject(i);
            if (o.containsKey("id_groupe")) ids.add(o.getString("id_groupe"));
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
                .put("ids", ids)
                .put("userId", userId);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }
    @Override
    public void getRemplacementClasse(String userId, String idStructure, Handler<Either<String, JsonArray>> handler) {
        getClassesIdsDevoir(userId, idStructure, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {

                    final JsonArray classeIds = event.right().getValue();
                    getNeoInfo(classeIds, userId, idStructure, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                JsonArray values = event.right().getValue();
                                handler.handle(new Either.Right<>(values));
                            } else {
                                handler.handle(new Either.Left<>("Error when getting remplacments classes from neo"));
                            }
                        }
                    });
                } else {
                    handler.handle(new Either.Left<>("Error when getting homeworks classes from sql"));
                }
            }
        });
    }
}
