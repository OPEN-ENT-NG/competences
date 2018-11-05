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

package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.service.ModaliteService;
import fr.openent.competences.service.impl.DefaultModaliteService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class ModaliteController extends ControllerHelper {

    ModaliteService modaliteService;

    public ModaliteController() {
        this.modaliteService = new DefaultModaliteService(Competences.COMPETENCES_SCHEMA, Competences.MODALITES_TABLE);
    }

    @Get("/modalites")
    @ApiDoc("Récupère les modalités")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getDefaultServices(final HttpServerRequest request) {
        this.modaliteService.getModalites(arrayResponseHandler(request));
    }
}
