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

package fr.openent.competences.service;

import  fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface SyntheseBilanPeriodiqueService {

    /**
     * Créer ou mettre à jour une synthèse d'un élève pour une période donnée
     * @param idEleve id eleve
     * @param idTypePeriode id_type periode
     * @param synthese synthese saisie par le professeur
     * @param idStructure id de l'établissement où la synthèse a été saisie
     * @param handler handler portant le résultat de la requête
     */
    void createOrUpdateSyntheseBilanPeriodique(Long idTypePeriode, String idEleve, String idStructure,
                                                      String synthese, Handler<Either<String, JsonObject>> handler);

    /**
     * Récupérer une synthèse d'un élève pour une période donnée
     * @param idEleve student identifier
     * @param idTypePeriode period type identifier
     * @param idStructure structure identifier
     * @param handler handler portant le résultat de la requête
     */
    void getSyntheseBilanPeriodique(Long idTypePeriode, String idEleve, String idStructure,
                                           Handler<Either<String, JsonArray>> handler);

    /**
     * Récupérer une synthèse d'un élève pour une période donnée
     * @param studentIds student identifiers
     * @param periodTypeId period type identifier
     * @param structureId structure identifier
     * @return
     */
    Future<JsonArray> getPeriodicReportSummaries(Long periodTypeId, List<String> studentIds, String structureId);


}
