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
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;

import static org.entcore.common.sql.SqlResult.validResultHandler;

/**
 * Created by anabah on 01/03/2017.
 */
public class DefaultAppreciationService extends SqlCrudService implements fr.openent.competences.service.AppreciationService {
    public DefaultAppreciationService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void createAppreciation(JsonObject appreciation, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.create(appreciation, user, handler);
    }

    @Override
    public void updateAppreciation(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.update(data.getValue("id").toString(), data, user, handler);
    }

    @Override
    public void deleteAppreciation(Long idNote, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.delete(idNote.toString(), user, handler);
    }

    @Override
    public void createOrUpdateAppreciationClasse(String appreciation, String id_classe, Integer id_periode, String id_matiere, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder().append("INSERT INTO ")
                .append( Competences.COMPETENCES_SCHEMA + ".appreciation_classe (appreciation, id_classe, id_periode, id_matiere) ")
                .append(" VALUES " )
                .append(" ( ?, ?, ?, ?)" )
                .append(" ON CONFLICT (id_classe, id_periode, id_matiere) DO UPDATE SET appreciation = ?");
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(appreciation);
        values.add(id_classe);
        values.add(id_periode);
        values.add(id_matiere);
        values.add(appreciation);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getAppreciationClasse(String[] id_classes, Integer id_periode, String[] id_matieres, Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT * FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_classe WHERE id_periode = ? ";

        JsonArray values = new JsonArray();
        values.add(id_periode);

        if(id_classes != null && id_classes.length > 0) {
            query += " AND id_classe IN " + Sql.listPrepared(id_classes);
            Arrays.stream(id_classes).forEach(values::add);
        }

        if(id_matieres != null && id_matieres.length > 0) {
            query += " AND id_matiere IN " + Sql.listPrepared(id_matieres);
            Arrays.stream(id_matieres).forEach(values::add);
        }

        Sql.getInstance().prepared(query, values, validResultHandler(handler));
    }

}
