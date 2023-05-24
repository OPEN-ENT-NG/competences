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

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface AverageService {

    /**
     * Recover averages students by subjects data to then export it
     *
     * @param structureId structure identifier to filter on
     * @param periodId period identifier to filter on
     * @param classId classe identifier to filter on
     * @param typeGroup group type
     * @param className class name
     * @param isWithSummaries if export require summaries
     * @param withMoyGeneraleByEleve if export require general Average
     * @param language language option to translate
     * @param host host option to translate
     * @return JsonObject containing averages students by subjects
     */
    Future<JsonObject> getStudentsAverageForExportPdf(String structureId, Integer periodId, String classId, Integer typeGroup,
                                                             String className, boolean isWithSummaries, Boolean withMoyGeneraleByEleve,
                                                             String language, String host);
}
