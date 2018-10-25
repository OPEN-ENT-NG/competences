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
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * Created by ledunoiss on 21/10/2016.
 */
public class FilterCompetenceNoteUtils {

    public void validateCompetenceNoteOwner(Long idNote, String owner,
                                            final Handler<Boolean> handler) {
        StringBuilder query = new StringBuilder()
                .append("SELECT count(devoirs.*) " +
                        "FROM "+ Competences.COMPETENCES_SCHEMA +".devoirs INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".competences_notes " +
                        "ON (competences_notes.id_devoir = devoirs.id) " +
                        "WHERE competences_notes.id = ? " +
                        "AND devoirs.owner = ?;");

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(idNote).add(owner);

        Sql.getInstance().prepared(query.toString(), params,
                new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> sqlResult) {
                Long count = SqlResult.countResult(sqlResult);
                handler.handle(count != null && count > 0);
            }
        });

    }

    public void validateCompetencesNotesOwner(List<Long> idNotes, String owner, final Handler<Boolean> handler) {
        StringBuilder query = new StringBuilder().append("SELECT count(DISTINCT devoirs.*) " +
                "FROM "+ Competences.COMPETENCES_SCHEMA +".devoirs INNER JOIN "+ Competences.COMPETENCES_SCHEMA +".competences_notes " +
                "ON (competences_notes.id_devoir = devoirs.id) " +
                "WHERE competences_notes.id IN " + Sql.listPrepared(idNotes.toArray()) + " " +
                "AND devoirs.owner = ?;");

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < idNotes.size(); i++) {
            params.add(idNotes.get(i));
        }

        params.add(owner);

        Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> sqlResult) {
                Long count = SqlResult.countResult(sqlResult);
                handler.handle(count != null && count > 0);
            }
        });

    }

    public void validateAccessCompetencesNotes(List<Long> idNotes, UserInfos user, final Handler<Boolean> handler) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder()
                .append("SELECT count(*) FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs ")
                .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".competences_notes ON (competences_notes.id_devoir = devoirs.id) ")
                .append("WHERE competences_notes.id IN " + Sql.listPrepared(idNotes.toArray()) + " ")
                .append("AND (devoirs.owner = ? OR ")
                        .append("devoirs.owner IN (SELECT DISTINCT id_titulaire ")
                                    .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_professeurs_remplacants ")
                                    .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".devoirs ON devoirs.id_etablissement = rel_professeurs_remplacants.id_etablissement  ")
                                    .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".competences_notes ON (competences_notes.id_devoir = devoirs.id) ")
                                    .append("WHERE competences_notes.id IN " + Sql.listPrepared(idNotes.toArray()) + " ")
                                    .append("AND id_remplacant = ? ")
                                    .append(") OR ")

                        .append("? IN (SELECT member_id ")
                                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs_shares ")
                                .append("WHERE resource_id = devoirs.id ")
                                .append("AND action = '" + Competences.DEVOIR_ACTION_UPDATE+"')")

                    .append(")");

        // Ajout des params pour la partie de la requête où on vérifie si on est le propriétaire
        for (int i = 0; i < idNotes.size(); i++) {
            params.add(idNotes.get(i));
        }
        params.add(user.getUserId());

        // Ajout des params pour la partie de la requête où on vérifie si on a des titulaires propriétaire
        for (int i = 0; i < idNotes.size(); i++) {
            params.add(idNotes.get(i));
        }
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
