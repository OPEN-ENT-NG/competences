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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ElementProgramme {

    /**
     * Créée ou met à jour un élément du programme (si élément du  programme déjà existant
     * @param userId
     * @param idPeriode
     * @param idMatiere
     * @param idClasse
     * @param texte
     * @param handler
     */
    void setElementProgramme(String userId, Long idPeriode, String idMatiere, String idClasse,String texte, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère un élément du programme
     * @param idPeriode
     * @param idMatiere
     * @param idClasse
     * @param handler
     */
    void getElementProgramme(Long idPeriode, String idMatiere, String idClasse, Handler<Either<String, JsonObject>> handler);

    /**
     * Get element Programme for many class
     * @param idPeriode
     * @param idMatiere
     * @param idsClasse
     * @param handler
     */
    void getElementProgrammeClasses(Long idPeriode, String idMatiere, JsonArray idsClasse, Handler<Either<String, JsonArray>> handler);

    /**
     * @param handler
     */
    void getDomainesEnseignement(String idCycle, Handler<Either<String, JsonArray>> handler);

    /**
     * @param handler
     */
    void getSousDomainesEnseignement(String idDomaine, Handler<Either<String, JsonArray>> handler);

    /**
     * @param handler
     */
    void getPropositions(String idStructure, Long idSousDomaine, Handler<Either<String, JsonArray>> handler);

    /**
     * @param handler
     */
    void createProposition(String idStructure, String libelle, Long idSousDomaine, Handler<Either<String, JsonArray>> handler);

    /**
     * @param handler
     */
    void editProposition(Long idProposition, String libelle, Handler<Either<String, JsonArray>> handler);

    /**
     * @param handler
     */
    void deleteProposition(Long idProposition, Handler<Either<String, JsonArray>> handler);

}
