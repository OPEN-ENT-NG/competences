/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
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
 */

package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.ServicesService;
import fr.openent.competences.service.UtilsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;

import java.util.Map;

import static fr.openent.competences.service.impl.DefaultExportService.COEFFICIENT;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.sql.SqlResult.validResultHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

public class DefaultServicesService extends SqlCrudService implements ServicesService {

    private UtilsService utilsService;

    public DefaultServicesService() {
        super(Competences.COMPETENCES_SCHEMA, Competences.SERVICES_TABLE);
        this.utilsService = new DefaultUtilsService();
    }

    public void createService(JsonObject oService, Handler<Either<String, JsonObject>> handler){

        String query="";
        String columns = "id_matiere, id_groupe, id_enseignant, coefficient";
        String params = "?,?,?,?";
        JsonArray values = new JsonArray();

        for(Object id_groupe : oService.getJsonArray("id_groupes")) {

            values.add(oService.getString("id_matiere"));
            values.add(id_groupe);
            values.add(oService.getString("id_enseignant"));
            values.add(oService.getValue("coefficient"));

            columns = "id_matiere, id_groupe, id_enseignant, coefficient";
            params = "?,?,?,?";

            if (oService.containsKey("id_etablissement")) {
                columns += ", id_etablissement";
                params += ",?";
                values.add(oService.getString("id_etablissement"));
            }

            if (oService.containsKey("modalite")) {
                columns += ", modalite";
                params += ",?";
                values.add(oService.getString("modalite"));
            }

            if (oService.containsKey("evaluable")) {
                columns += ", evaluable";
                params += ",?";
                values.add(oService.getBoolean("evaluable"));
            }

            query += "INSERT INTO " + this.resourceTable + " (" + columns + ") "
                    + "VALUES (" + params + ") ON CONFLICT ON CONSTRAINT pk_services DO UPDATE SET";

            if (oService.containsKey("modalite")) {
                query += " modalite=?";
                values.add(oService.getValue("modalite"));
            }
            if (oService.containsKey("evaluable")) {
                query += oService.containsKey("modalite") ? ", evaluable=?" : " evaluable=?";
                values.add(oService.getBoolean("evaluable"));
            }
            if (oService.containsKey(COEFFICIENT)) {
                query += oService.containsKey("modalite") || oService.containsKey("evaluable") ? ", coefficient=?" : " coefficient=?";
                values.add(oService.getLong(COEFFICIENT));
            }

            query += "; ";
        }

        Sql.getInstance().prepared(query, values, validUniqueResultHandler(handler));
    }


    public void getServices(String idEtablissement, JsonObject oService, Handler<Either<String, JsonArray>> handler) {

        String sqlQuery = "SELECT * FROM " + this.resourceTable + " WHERE id_etablissement = ?";
        JsonArray sqlValues = new JsonArray();
        sqlValues.add(idEtablissement);

        if (!oService.isEmpty()) {

            for (Map.Entry<String, Object> entry : oService.getMap().entrySet()) {

                if (entry.getValue() instanceof JsonArray) {
                    sqlQuery += " AND " + entry.getKey() + " IN " + Sql.listPrepared(((JsonArray) entry.getValue()).getList());
                    for (Object o : ((JsonArray) entry.getValue()).getList()) {
                        sqlValues.add(o);
                    }
                } else {
                    sqlQuery += " AND " + entry.getKey() + " = ?";
                    sqlValues.add(entry.getValue());
                }
            }
        }

        Sql.getInstance().prepared(sqlQuery, sqlValues, validResultHandler(handler));
    }

    public void deleteService(JsonObject oService, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + this.resourceTable + " WHERE id_matiere=? AND id_groupe=? AND id_enseignant=?";
        JsonArray values = new JsonArray();
        values.add(oService.getString("id_matiere"))
                .add(oService.getString("id_groupe"))
                .add(oService.getString("id_enseignant"));

        Sql.getInstance().prepared(query, values, validUniqueResultHandler(handler));
    }
}
