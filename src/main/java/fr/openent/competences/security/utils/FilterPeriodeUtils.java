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
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.Renders.getHost;

/**
 * Created by ledunoiss on 20/10/2016.
 */
public class FilterPeriodeUtils {

    protected static final Logger log = LoggerFactory.getLogger(FilterPeriodeUtils.class);
    private UserInfos user;
    public FilterPeriodeUtils() {
    }

    public FilterPeriodeUtils(EventBus eventBus) {
        this.eb = eventBus;
    }
    private EventBus eb;
    public FilterPeriodeUtils(EventBus eventBus, UserInfos user) {
        this.eb = eventBus;
        this.user = user;
    }


    public void validateEndSaisie(final HttpServerRequest request, final String idClasse,
                                  final Integer idTypePeriode, final Handler<Boolean> handler)  {
        if(user == null) {
            handler.handle(false);
            return;
        }
        else if(new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString())) {
            handler.handle(true);
            return;
        }
        else {
            WorkflowActionUtils.hasHeadTeacherRight(user, new JsonArray().add(idClasse), null,
                    null, null, null, new Handler<Either<String, Boolean>>() {
                        @Override
                        public void handle(Either<String, Boolean> event) {
                            Boolean isHeadTeacher;
                            if (event.isLeft()) {
                                isHeadTeacher = false;
                            }
                            else {
                                isHeadTeacher = true;
                            }


                            if (isHeadTeacher) {
                                handler.handle(true);
                                return;
                            }
                            else {
                                validateEndSaisieUtils(request, idClasse, idTypePeriode, handler);
                            }
                        }
                    });

        }
    }

    private  void validateEndSaisieUtils(final HttpServerRequest request, final String idClasse,
                                           final Integer idTypePeriode, final Handler<Boolean> handler)  {
        JsonObject jsonRequest = new JsonObject()
                .put("headers", new JsonObject()
                        .put("Accept-Language",
                                request.headers().get("Accept-Language")))
                .put("Host", getHost(request));
        JsonObject action = new JsonObject()
                .put("action", "periode.getPeriodes")
                //.put("idEtablissement", idEtablissement)
                .put("idGroupes", new fr.wseduc.webutils.collections.JsonArray().add(idClasse))
                .put("request", jsonRequest);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                JsonArray periodes = body.getJsonArray("result");
                boolean isUpdatable = true;

                if ("ok".equals(body.getString("status"))) {
                    // On vérifie que la date de fin de saisie n'est pas dépassée
                    JsonObject periode = null;
                    for (int i = 0; i < periodes.size(); i++) {
                        if (idTypePeriode.intValue()
                                == ((JsonObject) periodes.getJsonObject(i)).getInteger("id_type").intValue()) {
                            periode = (JsonObject) periodes.getJsonObject(i);
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
        }));
    }
    public void validateStructure (final String idEtablissement, Long idPeriode, final Handler<Boolean> handler) {

        StringBuilder query = new StringBuilder()
                .append("SELECT count(periode.*) " +
                        "FROM " + Competences.VSCO_SCHEMA + ".periode " +
                        "WHERE periode.id_etablissement = ?");
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(idEtablissement);

        if(idPeriode != null) {
            query.append("AND  periode.id_type = ? ");
            params.add(idPeriode);
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
