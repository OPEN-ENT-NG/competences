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

package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.enums.EventStoresCompetences;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

import static fr.openent.competences.Competences.COMPETENCES_SCHEMA;


public class CompetencesController extends ControllerHelper {

    private EventStore eventStore;
    private JsonObject config;
    public CompetencesController() {}
    public CompetencesController(EventStore eventStore, JsonObject config) {
        this.eventStore = eventStore;
        this.config = config;
    }

	/**
	 * Displays the home view.
	 * @param request Client request
	 */
	@Get("")
	@SecuredAction("competences.access")
	public void view(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                Utils.setLocale(I18n.acceptLanguage(request));
                Utils.setDomain(getHost(request));

                // test request rendering competences v2
                if (Boolean.TRUE.equals(config.getBoolean("enableCompetencesv2"))) {
                    JsonObject action = new JsonObject()
                            .put("action", "user.getActivesStructure")
                            .put("module", COMPETENCES_SCHEMA)
                            .put("structures", new JsonArray(user.getStructures()));

                    eb.request(Competences.VIESCO_BUS_ADDRESS, action, event -> {
                        JsonObject body = (JsonObject) event.result().body();
                        if (event.failed() || "error".equals(body.getString("status"))) {
                            log.error("[Competences@CompetencesController] Failed to retrieve actives structures");
                            renderError(request);
                        } else {
                            renderView(request, new JsonObject().put("structures", body.getJsonArray("results", new JsonArray())),
                                    "competences2.html", null);
                        }
                    });
                    return;
                }

                if(user.getType().equals("Teacher") || user.getType().equals("Personnel")) {
                    renderView(request, null, "eval_teacher.html", null);
                }else if(user.getType().equals("Student") || user.getType().equals("Relative")){
                    renderView(request, null,  "eval_parents.html", null);
                }
                eventStore.createAndStoreEvent(EventStoresCompetences.ACCESS.toString(), request);
            }
        });
	}
}
