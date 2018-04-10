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
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.wseduc.webutils.Server.getEventBus;
import static fr.wseduc.webutils.http.Renders.getHost;

/**
 * Created by ledunoiss on 20/10/2016.
 */
public class FilterPeriodeUtils extends BusModBase {

    protected static final Logger log = LoggerFactory.getLogger(FilterPeriodeUtils.class);

    public FilterPeriodeUtils() {
    }

    public FilterPeriodeUtils(EventBus eventBus) {
        this.eb = eventBus;
    }


    public void validateEndSaisie(final HttpServerRequest request, final String idClasse, final Integer idTypePeriode, final Handler<Boolean> handler)  {
        JsonObject jsonRequest = new JsonObject()
                .putObject("headers", new JsonObject()
                        .putString("Accept-Language",
                                request.headers().get("Accept-Language")))
                .putString("Host", getHost(request));
        JsonObject action = new JsonObject()
                .putString("action", "periode.getPeriodes")
                //.putString("idEtablissement", idEtablissement)
                .putArray("idGroupes", new JsonArray().addString(idClasse))
                .putObject("request", jsonRequest);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                JsonArray periodes = body.getArray("result");
                boolean isUpdatable = true;

                if ("ok".equals(body.getString("status"))) {
                    // On vérifie que la date de fin de saisie n'est pas dépassée
                    JsonObject periode = null;
                    for (int i = 0; i < periodes.size(); i++) {
                        if (idTypePeriode.intValue()
                                == ((JsonObject) periodes.get(i)).getNumber("id_type").intValue()) {
                            periode = (JsonObject) periodes.get(i);
                            break;
                        }
                    }
                    if (periode != null) {
                        String dateFinSaisieStr = periode.getString("date_fin_saisie")
                                .split("T")[0];
                        DateFormat formatter = new SimpleDateFormat("yy-MM-dd");
                        try {
                            Date dateFinSaisie = formatter.parse(dateFinSaisieStr);
                            Date dateActuelle = new Date();
                            dateActuelle.setTime(0);
                            if (dateActuelle.after(dateFinSaisie)) {
                                isUpdatable = false;
                            }
                        } catch (ParseException e) {
                            log.error("Erreur lors du calcul de fin de saisie de la periode", e);
                        }
                    } else {
                        isUpdatable = false;
                    }
                }

                handler.handle(isUpdatable);
            }
        });
    }

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
