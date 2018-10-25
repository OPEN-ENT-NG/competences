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

import fr.openent.competences.security.AccessElementProgrammeFilter;
import fr.openent.competences.service.ElementProgramme;
import fr.openent.competences.service.impl.DefaultElementProgramme;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import io.vertx.core.http.HttpServerRequest;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class ElementProgrammeController extends ControllerHelper {

    /**
     * Déclaration des services
     */
    private final ElementProgramme elementProgramme;


    public ElementProgrammeController() {
        elementProgramme = new DefaultElementProgramme();
    }


    @Get("/element/programme/domaines")
    @ApiDoc("Récupère les domaines d'enseignement")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessElementProgrammeFilter.class)
    public void getDomainesEnseignement(final HttpServerRequest request) {
        elementProgramme.getDomainesEnseignement(arrayResponseHandler(request));
    }

    @Get("/element/programme/sous/domaines")
    @ApiDoc("Récupère les domaines d'enseignement")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessElementProgrammeFilter.class)
    public void getSousDomainesEnseignement(final HttpServerRequest request) {
        elementProgramme.getSousDomainesEnseignement(arrayResponseHandler(request));
    }

    @Get("/element/programme/propositions")
    @ApiDoc("Récupère les domaines d'enseignement")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessElementProgrammeFilter.class)
    public void getPropositions(final HttpServerRequest request) {
        elementProgramme.getPropositions(arrayResponseHandler(request));
    }
}
