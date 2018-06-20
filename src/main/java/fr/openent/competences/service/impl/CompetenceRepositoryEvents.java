/*
 * Copyright (c) Région Hauts-de-France, Département 77, CGI, 2017.
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

package fr.openent.competences.service.impl;


import fr.openent.competences.service.TransitionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.RepositoryEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ledunoiss on 29/03/2017.
 */
public class CompetenceRepositoryEvents implements RepositoryEvents {

    private static final Logger log = LoggerFactory.getLogger(CompetenceRepositoryEvents.class);

    private EventBus eb;
    private final TransitionService transitionService;
    public CompetenceRepositoryEvents(EventBus eb) {
        this.eb = eb;
        this.transitionService = new DefaultTransitionService();
    }

    @Override
    public void exportResources(String s, String s1, JsonArray jsonArray, String s2, String s3, String s4, Handler<Boolean> handler) {
        log.info("[CompetenceRepositoryEvents] : export resources event is not implemented");
    }

    @Override
    public void deleteGroups(JsonArray jsonArray) {
        log.info("[CompetenceRepositoryEvents] : delete groups event is not implemented");
    }

    @Override
    public void deleteUsers(JsonArray jsonArray) {
        log.info("[CompetenceRepositoryEvents] : delete groups event is not implemented");
    }

    @Override
    public void transition(JsonObject structure) {
        String idStructure = structure.getString("id");
        if(null == idStructure
                || !structure.containsKey("id")){
            log.error("[CompetenceRepositoryEvents] : An error occured when managing transition annee, cannot find id structure");
        } else {
            List<String> idsStructures = new ArrayList<>();
            idsStructures.add(idStructure);
            transitionService.transitionAnnee(this.eb,idsStructures, new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isLeft()) {
                        log.error("[CompetenceRepositoryEvents] : An error occured when managing transition annee id structure idStructure");
                    }
                }
            });
        }
    }
}
