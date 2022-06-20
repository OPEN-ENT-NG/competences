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

package fr.openent.competences.security.utils;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Created by anabah on 02/03/2017.
 */
public class FilterAppreciationUtils {
        public void validateAppreciationOwner (Long idAppreciation, String owner, final Handler<Boolean> handler) {
            StringBuilder query = new StringBuilder()
                    .append("SELECT count(devoirs.*) " +
                            "FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs " +
                            "INNER JOIN " + Competences.COMPETENCES_SCHEMA + "." + Field.APPRECIATIONS_TABLE + " ON (appreciations.id_devoir = devoirs.id) " +
                            "WHERE " + Field.APPRECIATIONS_TABLE + ".id = ? " +
                            "AND devoirs.owner = ?;");

            JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(idAppreciation).add(owner);

            Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> sqlResult) {
                    Long count = SqlResult.countResult(sqlResult);
                    handler.handle(count != null && count > 0);
                }
            });
        }



        public void validateAccessAppreciation (Long idNote, UserInfos user, final Handler<Boolean> handler) {
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

            StringBuilder query = new StringBuilder()
                    .append("SELECT count(*) FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs ")
                    .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + "." + Field.APPRECIATIONS_TABLE + " ON " +
                            "(" + Field.APPRECIATIONS_TABLE + ".id_devoir = devoirs.id) ")
                    .append("WHERE " + Field.APPRECIATIONS_TABLE + ".id = ? ")
                    .append("AND (devoirs.owner = ? OR ")
                    .append("devoirs.owner IN (SELECT DISTINCT id_titulaire ")
                    .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_professeurs_remplacants ")
                    .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".devoirs ON devoirs.id_etablissement = rel_professeurs_remplacants.id_etablissement  ")
                    .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + "." + Field.APPRECIATIONS_TABLE + " ON (" + Field.APPRECIATIONS_TABLE + ".id_devoir = devoirs.id) ")
                    .append("WHERE " + Field.APPRECIATIONS_TABLE + ".id = ? ")
                    .append("AND id_remplacant = ? ")
                    .append(") OR ")

                    .append("? IN (SELECT member_id ")
                    .append("FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs_shares ")
                    .append("WHERE resource_id = devoirs.id ")
                    .append("AND action = '" + Competences.DEVOIR_ACTION_UPDATE+"')")

                    .append(")");

            // Ajout des params pour la partie de la requête où on vérifie si on est le propriétaire
            params.add(idNote);
            params.add(user.getUserId());

            // Ajout des params pour la partie de la requête où on vérifie si on a des titulaires propriétaire
            params.add(idNote);
            params.add(user.getUserId());

            // Ajout des params pour la partie de la requête où on vérifie si on a des droits de partage provenant d'un remplaçant
            params.add(user.getUserId());


            Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> sqlResult) {
                    Long count = SqlResult.countResult(sqlResult);
                    handler.handle(count != null && count > 0);
                }
            });
        }

}
