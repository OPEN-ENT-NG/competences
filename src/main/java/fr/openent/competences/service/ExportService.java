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
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ExportService {

    void getExportEval(final Boolean text, Boolean only_evaluation, JsonObject devoir, String idGroupe,
                       String idEtablissement, HttpServerRequest request,
                       Handler<Either<String, JsonObject>> handler);

    void getExportReleveComp(final Boolean text, final Boolean pByEnseignement, final String idEleve,
                             String[] idGroupes,String[] idFunctionalGroupes, final String idEtablissement,
                             final List<String> idMatieres, Long idPeriodeType, final Boolean isCycle, final JsonArray enseignementsOrdered, final JsonArray domainesRacines,
                             final Handler<Either<String, JsonObject>> handler);

    void getExportRecapEval(final Boolean text, final Long idCycle, final String idEtablissement,
                            final Handler<Either<String, JsonArray>> handler);

    void genererPdf(final HttpServerRequest request, final JsonObject templateProps, final String templateName,
                    final String prefixPdfName, Vertx vertx, JsonObject config);
}
