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
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface ExportService {
    /**
     * get jsonObject with param to build the template for pdf cartouche
     * @param params front params
     * @param handler response to build the template for pdf
     */
    void getExportCartouche (final MultiMap params, Handler<Either<String, JsonObject>> handler);

    void getExportEval(final Boolean text, final Boolean usePerso, Boolean only_evaluation, JsonObject devoir, String idGroupe,
                       String idEtablissement, HttpServerRequest request,
                       Handler<Either<String, JsonObject>> handler);

    void getExportReleveComp(final Boolean text, final Boolean usePerso, final Boolean pByEnseignement, final String idEleve,
                             final int eleveLevel, String[] idGroupes,String[] idFunctionalGroupes, final String idEtablissement,
                             final List<String> idMatieres, Long idPeriodeType, final Boolean isCycle, final Long idCycle,
                             final Handler<Either<String, JsonObject>> handler);

    void getLegendeRecapEval(final Boolean text, final Boolean usePerso, final Long idCycle,
                                   final String idEtablissement, final Handler<Either<String, JsonArray>> handler);

    void genererPdf(final HttpServerRequest request, final JsonObject templateProps, final String templateName,
                    final String prefixPdfName, Vertx vertx, JsonObject config);

    /**
     * Generate PDF by proceeding template html first before generating its buffer with PDF sequentially
     *
     * @param request       HttpServerRequest to send response  {@link HttpServerRequest}
     * @param templateProps Props object to send for proceeding template {@link JsonObject}
     * @param templateName  Name of the template used for proceeding template {@link String} (e.g bulletin.pdf.xhtml...)
     * @param title         Title of the pdf {@link String}
     * @param vertx         Vertx instance {@link Vertx}
     * @param config        config module entcore instance {@link JsonObject}
     */
    void generateSchoolReportPdf(HttpServerRequest request, JsonObject templateProps, String templateName, String title,
                                 Vertx vertx, JsonObject config);

    void getMatiereExportReleveComp(final JsonArray idMatieres, Handler<Either<String, String>> handler);

    void getLibellePeriodeExportReleveComp(final HttpServerRequest request, final Long finalIdPeriode,
                                           Boolean isCycle, Long idCycle, Handler<Either<String, String>> handler);

    void getElevesExportReleveComp(final String finalIdClasse, String idStructure, String finalIdEleve,
                                   final Long finalIdPeriode, final Map<String, String> elevesMap,
                                   Handler<Either<String, Object>> handler);

    void getDataForExportReleveEleve(String idUser, String idEtablissement, Long idPeriode,
                                     final MultiMap params, Handler<Either<String, JsonObject>> handler);

    void getDataForExportReleveClasse(String idClasse, String idEtablissement, Long idPeriode, final Long idTypePeriode,
                                      final Long ordre, Boolean scores, Boolean skills, Handler<Either<String, JsonObject>> handler);
}
