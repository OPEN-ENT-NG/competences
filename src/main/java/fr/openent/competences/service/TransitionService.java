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

package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

import java.util.List;

public interface TransitionService  extends CrudService {
    /**
     * Effectue la transition d'année pour une liste de structure
     * @param idsStructures
     * @param handler
     */
    public void transitionAnnee(EventBus eb, final List<String> idsStructures,
                                final Handler<Either<String, JsonArray>> handler);

    /**
     * Effectue la transition d'année pour une structure
     * @param eb
     * @param structure
     * @param finalHandler
     */
    public void transitionAnneeStructure(EventBus eb, final JsonObject structure,
                                         final Handler<Either<String, JsonArray>> finalHandler);

    /**
     * get conditions to do transition : get nb devoir > 0 has_devoir = true else false,
     * nb periode > 0 has_periode = true else false
     * and nb transition > 0 has_transition = true else false
     * @param idStructureATraiter id structure
     * @param handler response
     */
    public void conditionsToDoTransition( String idStructureATraiter, final Handler<Either<String, JsonObject>> handler);

    /**
     * get ids_classe with periode
     * @param id_etablissement ids_etablissement
     * @param handler response
     */
    public void classesWithPeriode(String id_etablissement, final Handler<Either<String,JsonArray>> handler);

}
