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

package fr.openent.competences.service;

import fr.openent.competences.bean.lsun.Donnees;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public interface LSUService {

    /**
     * Permet de sélectionner parmis les disciplines sélectionnées de l'établissement, celles qui sont évaluées
     * (reférencées dans au moins une balise suiviAcquis d'un élève).
     *
     * @param idsEvaluatedDiscipline
     * @param donnees
     * @param errorsExport
     */
    void validateDisciplines(JsonArray idsEvaluatedDiscipline, Donnees donnees, JsonObject errorsExport);

    JsonArray getIdsEvaluatedDiscipline() ;

    void addIdsEvaluatedDiscipline( Object idDiscipline);

    void initIdsEvaluatedDiscipline();

    void serviceResponseOK (AtomicBoolean answer, int count, String thread, String method);

    void getIdClassIdCycleValue(List<String> classIds, final Handler<Either<String, List<Map>>> handler);

    void getMapIdClassCodeDomaineById(List<String> idClass, final Handler<Either<String, Map<String,Map<Long, String>>>> handler);

    /**
     * get deleted student by classe and deleted date > min(resqueted periodes)
     * @param periodesByClass resqueted periodes of the class
     * @param handler response
     */
    void getDeletedStudentsPostgres(Map<String,JsonArray> periodesByClass, Map<String, JsonObject> mapDeleteStudent,
                                    Handler<Either<String, Map<String, JsonObject>>> handler);

    /**
     * get all students of the icClass list
     * @param idStrure idStructure
     * @param idsClass requested idsClass
     * @param idsDeletedStudent deleted students of Postgres
     * @param handler response
     */
    void getAllStudentWithRelatives(String idStrure, List<String> idsClass, List<String> idsDeletedStudent,Handler<Either<String,JsonArray>> handler);
}
