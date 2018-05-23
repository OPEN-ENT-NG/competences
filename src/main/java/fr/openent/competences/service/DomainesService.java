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

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface DomainesService extends CrudService {
    /**
     * Récupération de tous les enseignements
     * @param idClasse L'identifiant de la classe.
     * @param handler handler portant le résultat de la requête.
     */
    public void getArbreDomaines(String idClasse,String idEleve, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère les domaines évalués pour le BFC
     * @param idClasse L'identifiant de la classe.
     * @param handler handler portant le résultat de la requête.
     */
    public void getDomainesRacines(String idClasse, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère les libellés et codifications des domaines passés en paramètre
     * @param idDomaines L'identifiant de la classe.
     * @param handler handler portant le résultat de la requête.
     */
    public void getDomainesLibCod(int[] idDomaines, Handler<Either<String, JsonArray>> handler);
}
