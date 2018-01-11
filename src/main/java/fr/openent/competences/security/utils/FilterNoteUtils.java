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

package fr.openent.competences.security.utils;

import fr.openent.competences.Competences;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by ledunoiss on 21/10/2016.
 */
public class FilterNoteUtils {

    public void validateNoteOwner (Long idNote, String owner, final Handler<Boolean> handler) {
        StringBuilder query = new StringBuilder()
                .append("SELECT count(devoirs.*) " +
                        "FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs INNER JOIN" +
                        Competences.COMPETENCES_SCHEMA + ".notes ON (notes.id_devoir = devoirs.id) " +
                        "WHERE notes.id = ? " +
                        "AND devoirs.owner = ?;");

        JsonArray params = new JsonArray().addNumber(idNote).addString(owner);

        Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> sqlResult) {
                Long count = SqlResult.countResult(sqlResult);
                handler.handle(count != null && count > 0);
            }
        });
    }



    public void validateAccessNote (Long idNote, UserInfos user, final Handler<Boolean> handler) {
        JsonArray params = new JsonArray();

        StringBuilder query = new StringBuilder()
                .append("SELECT count(*) FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs ")
                .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".notes ON (notes.id_devoir = devoirs.id) ")
                .append("WHERE notes.id = ? ")
                .append("AND (devoirs.owner = ? OR ")
                        .append("devoirs.owner IN (SELECT DISTINCT id_titulaire ")
                                    .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_professeurs_remplacants ")
                                    .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".devoirs ON devoirs.id_etablissement = rel_professeurs_remplacants.id_etablissement  ")
                                    .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".notes ON (notes.id_devoir = devoirs.id) ")
                                    .append("WHERE notes.id = ? ")
                                    .append("AND id_remplacant = ? ")
                                    .append(") OR ")

                        .append("? IN (SELECT member_id ")
                            .append("FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs_shares ")
                            .append("WHERE resource_id = devoirs.id ")
                            .append("AND action = '" + Competences.DEVOIR_ACTION_UPDATE+"')")

                    .append(")");

        // Ajout des params pour la partie de la requête où on vérifie si on est le propriétaire
        params.addNumber(idNote);
        params.addString(user.getUserId());

        // Ajout des params pour la partie de la requête où on vérifie si on a des titulaires propriétaire
        params.addNumber(idNote);
        params.addString(user.getUserId());

        // Ajout des params pour la partie de la requête où on vérifie si on a des droits de partage provenant d'un remplaçant
        params.addString(user.getUserId());


        Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> sqlResult) {
                Long count = SqlResult.countResult(sqlResult);
                handler.handle(count != null && count > 0);
            }
        });
    }

}
