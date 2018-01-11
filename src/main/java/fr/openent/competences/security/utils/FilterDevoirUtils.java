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
 * Created by ledunoiss on 20/10/2016.
 */
public class FilterDevoirUtils {

    public void validateOwnerDevoir(Integer idDevoir, String owner, final Handler<Boolean> handler) {
        StringBuilder query = new StringBuilder()
                .append("SELECT count(devoirs.*) " +
                        "FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs " +
                        "WHERE devoirs.id = ? " +
                        "AND devoirs.owner = ?;");

        JsonArray params = new JsonArray().addNumber(idDevoir).addString(owner);

        Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                Long count = SqlResult.countResult(message);
                handler.handle(count != null && count > 0);
            }
        });
    }

    public void validateDevoirFinSaisie(Long idDevoir, UserInfos user, final Handler<Boolean> handler) {
        StringBuilder query = new StringBuilder()
                .append("SELECT count(devoir.id) " +
                        "FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs, " + Competences.VSCO_SCHEMA + ".periode "+
                        "WHERE devoirs.id = ? " +
                        "AND devoirs.owner = ?  " +
                        "AND now() < periode.date_fin_saisie;" );

        JsonArray params = new JsonArray().addNumber(idDevoir).addString(user.getUserId());

        Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                Long count = SqlResult.countResult(message);
                handler.handle(count != null && count > 0);
            }
        });
    }
    public void validateAccessDevoir(Long idDevoir, UserInfos user, boolean modification, final Handler<Boolean> handler) {

        JsonArray params = new JsonArray();

        StringBuilder query = new StringBuilder()
                .append("SELECT count(*) FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs ");

        if (modification) {
            query.append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_devoirs_groupes on (devoirs.id = rel_devoirs_groupes.id_devoir)");
            query.append("INNER JOIN " + Competences.VSCO_SCHEMA + ".periode on (rel_devoirs_groupes.id_groupe = periode.id_classe)");
        }

        query.append("WHERE devoirs.id = ? ")
                .append("AND (devoirs.owner = ? OR ")
                .append("devoirs.owner IN (SELECT DISTINCT id_titulaire ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_professeurs_remplacants ")
                .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".devoirs ON devoirs.id_etablissement = rel_professeurs_remplacants.id_etablissement  ")
                .append("WHERE devoirs.id = ? ")
                .append("AND id_remplacant = ? ")
                .append(") OR ")

                .append("? IN (SELECT member_id ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs_shares ")
                .append("WHERE resource_id = ? ")
                .append("AND action = '" + Competences.DEVOIR_ACTION_UPDATE+"')")

                .append(")");

        if (modification) {
            query.append(" AND now() < periode.date_fin_saisie");
        }

        // Ajout des params pour la partie de la requête où on vérifie si on est le propriétaire
        params.addNumber(idDevoir);
        params.addString(user.getUserId());

        // Ajout des params pour la partie de la requête où on vérifie si on a des titulaires propriétaire
        params.addNumber(idDevoir);
        params.addString(user.getUserId());

        // Ajout des params pour la partie de la requête où on vérifie si on a des droits de partage provenant d'un remplaçant
        params.addString(user.getUserId());
        params.addNumber(idDevoir);


        Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                Long count = SqlResult.countResult(message);
                handler.handle(count != null && count > 0);
            }
        });
    }

}
