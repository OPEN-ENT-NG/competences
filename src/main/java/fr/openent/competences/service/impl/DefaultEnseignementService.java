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
import fr.openent.competences.service.EnseignementService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import static fr.openent.competences.Competences.COMPETENCES_SCHEMA;
import static fr.openent.competences.Competences.DELIVERY_OPTIONS;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultEnseignementService extends SqlCrudService implements EnseignementService {

    public DefaultEnseignementService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void getEnseignements(Handler<Either<String, JsonArray>> handler) {
        super.list(handler);
    }

    @Override
    public void getEnseignementsOrdered(Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT * ")
                .append("FROM "+ COMPETENCES_SCHEMA +".enseignements ")
                .append("ORDER BY nom ASC");

        Sql.getInstance().prepared(query.toString(), values, DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }
}
