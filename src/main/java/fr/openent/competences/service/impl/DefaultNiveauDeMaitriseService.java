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
import fr.openent.competences.service.NiveauDeMaitriseService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.competences.Competences.DELIVERY_OPTIONS;
import static org.entcore.common.sql.SqlResult.validResultHandler;
import static org.entcore.common.sql.SqlResult.validRowsResultHandler;

/**
 * Created by anabah on 30/08/2017.
 */
public class DefaultNiveauDeMaitriseService extends SqlCrudService implements NiveauDeMaitriseService {


    public DefaultNiveauDeMaitriseService() {
        super(Competences.COMPETENCES_SCHEMA, Field.PERSO_NIVEAU_COMPETENCES_TABLE);
    }

    /**
     * Recupère l'ensemble des couleurs des niveaux de maitrise pour un établissement.
     * @param idEtablissement identifiant de l'établissement
     * @param handler handler portant le resultat de la requête 
     */
    public void getNiveauDeMaitrise(String idEtablissement, Long idCycle, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT niv.libelle, t1.libelle as default_lib, t1.ordre, t1.couleur AS default,")
                .append(" t1.id_cycle, t1.id AS id_niveau,")
                .append(" niv.id_etablissement, niv.couleur, niv.lettre, niv.id AS id, t2.libelle AS cycle")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + "." + Field.NIVEAU_COMPETENCES_TABLE + " AS t1")
                .append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + "." + Competences.CYCLE_TABLE + " AS t2")
                .append(" ON t1.id_cycle = t2.id ")
                .append(" LEFT JOIN ")
                .append(" (SELECT * FROM "+ Competences.COMPETENCES_SCHEMA + "." + Field.PERSO_NIVEAU_COMPETENCES_TABLE)
                .append(" WHERE id_etablissement = ? ) AS niv")
                .append(" ON (niv.id_niveau = t1.id) ");

        values.add(idEtablissement);

        if(idCycle != null) {
            query.append(" WHERE t1.id_cycle = ?");
            values.add(idCycle);
        }

        query.append(" ORDER BY ordre");

        Sql.getInstance().prepared(query.toString(), values, DELIVERY_OPTIONS, validResultHandler(handler));
    }

    public void getNiveauDeMaitriseofCycle(Long Cycle, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT niv1.libelle, niv1.ordre, niv1.couleur couleurDefault, niv1.id_cycle ")
                .append("FROM notes." + Field.NIVEAU_COMPETENCES_TABLE + " niv1 ")
                .append("WHERE id_cycle = ? ")
                .append("ORDER BY (ordre);");

        values.add(Cycle);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    public void getNiveauDeMaitriseofClasse(String idClasse, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT " + Field.NIVEAU_COMPETENCES_TABLE + ".libelle, " + Field.NIVEAU_COMPETENCES_TABLE + ".ordre, ")
                .append(Field.NIVEAU_COMPETENCES_TABLE + ".couleur couleurDefault, " + Field.NIVEAU_COMPETENCES_TABLE + ".id_cycle  ")
                .append(" FROM  notes." + Field.NIVEAU_COMPETENCES_TABLE)
                .append(" INNER JOIN " +   Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle ")
                .append(" ON id_groupe = ? AND rel_groupe_cycle.id_cycle = " + Field.NIVEAU_COMPETENCES_TABLE + ".id_cycle ")
                .append(" order By (ordre);" );

        values.add(idClasse);

        Sql.getInstance().prepared(query.toString(), values, DELIVERY_OPTIONS, validResultHandler(handler));
    }

    public void getPersoNiveauMaitrise(String idUser,Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT * FROM " + Competences.COMPETENCES_SCHEMA + "." + Competences.USE_PERSO_NIVEAU_COMPETENCES_TABLE)
                .append(" WHERE id_user = ? ");

        values.add(idUser);
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    public void markUsePerso(final JsonObject idUser, final Handler<Either<String, JsonArray>> handler) {
        final String queryNewUserId =
                "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + "."+ Competences.USE_PERSO_NIVEAU_COMPETENCES_TABLE
                        +"_id_seq') as id";

        sql.raw(queryNewUserId, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    final Long userId = event.right().getValue().getLong("id");
                    final String table = Competences.COMPETENCES_SCHEMA + "."+
                            Competences.USE_PERSO_NIVEAU_COMPETENCES_TABLE;
                    doCreate(handler, userId, idUser, table);
                }
                else {
                    handler.handle(new Either.Left<String, JsonArray>(event.left().getValue()));
                }
            }
        }));
    }
    public void createMaitrise(final JsonObject maitrise, UserInfos user, final Handler<Either<String, JsonArray>> handler) {

        final String queryNewNivCompetenceId =
                "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".perso_niveau_competences_id_seq') as id";

        sql.raw(queryNewNivCompetenceId, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {

                if (event.isRight()) {
                    final Long niveauCompetenceId = event.right().getValue().getLong("id");
                    maitrise.put("id", niveauCompetenceId);
                    doCreate(handler,niveauCompetenceId,maitrise,resourceTable);
                }
                else {
                    handler.handle(new Either.Left<String, JsonArray>(event.left().getValue()));
                }
            }
        }));
    }

    public void doCreate ( final Handler<Either<String, JsonArray>> handler,
                           final Long returning, final JsonObject o, String table) {
        SqlStatementsBuilder s = new SqlStatementsBuilder();
        s.insert(table, o, "id");
        Sql.getInstance().transaction(s.build(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body();
                if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
                    handler.handle(new Either.Right<String, JsonArray>(new fr.wseduc.webutils.collections.JsonArray()
                            .add(new JsonObject().put("id", returning))));
                } else {
                    handler.handle(new Either.Left<String, JsonArray>(result.getString("status")));
                }
            }
        });
    }

    @Override
    public void update(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.update(data.getValue("id").toString(), data, user, handler);
    }

    @Override
    public void delete(String idEtablissement, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + resourceTable + " WHERE id_etablissement = ?";
        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(idEtablissement), validRowsResultHandler(handler));
    }

    public void deleteUserFromPerso(String idUser,Handler<Either<String, JsonObject>> handler ) {
        final String table = Competences.COMPETENCES_SCHEMA + "."+
                Competences.USE_PERSO_NIVEAU_COMPETENCES_TABLE;

        String query = "DELETE FROM " + table + " WHERE id_user = ?";
        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(idUser), validRowsResultHandler(handler));
    }
}