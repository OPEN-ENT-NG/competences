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
import fr.openent.competences.service.LanguesCultureRegionaleService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public class DefaultLanguesCultureRegionaleService extends SqlCrudService implements LanguesCultureRegionaleService {


    public DefaultLanguesCultureRegionaleService(String schema, String table) {
        super(schema, table);
    }


    public void getLanguesCultureRegionaleService(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, libelle, code FROM "+ Competences.COMPETENCES_SCHEMA + ".langues_culture_regionale";
        JsonArray values = new JsonArray();
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }



}
