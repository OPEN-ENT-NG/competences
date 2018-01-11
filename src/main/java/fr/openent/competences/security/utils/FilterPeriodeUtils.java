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
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by ledunoiss on 20/10/2016.
 */
public class FilterPeriodeUtils {

    public void validateStructure (final String idEtablissement, Long idPeriode, final Handler<Boolean> handler) {

        StringBuilder query = new StringBuilder()
                .append("SELECT count(periode.*) " +
                        "FROM " + Competences.VSCO_SCHEMA + ".periode " +
                        "WHERE periode.id_etablissement = ?");
        JsonArray params = new JsonArray().addString(idEtablissement);

        if(idPeriode != null) {
            query.append("AND  periode.id_type = ? ");
            params.addNumber(idPeriode);
        }


        Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                Long count = SqlResult.countResult(message);
                handler.handle(count != null && count > 0);
            }
        });

    }

}
