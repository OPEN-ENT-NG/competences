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
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface RemplacementService extends CrudService {

    /**
     * Récupère les remplacments en cours
     * @param poListIdEtab identifiants des établissements
     * @param handler handler portant le resultat de la requête
     */
    public void listRemplacements(List<String> poListIdEtab, Handler<Either<String, JsonArray>> handler);

    /**
     * Ajout d'un remplacement
     * @param poRemplacement remplacement à créer
     * @param handler handler portant le résultat de la requête
     */
    public void createRemplacement(final JsonObject poRemplacement, final Handler<Either<String, JsonObject>> handler);

    /**
     * Suppression d'un remplacement
     * @param id_titulaire
     * @param id_remplacant
     * @param date_debut
     * @param date_fin
     * @param id_etablissement
     * @param handler
     */
    public void deleteRemplacement(String id_titulaire, String id_remplacant, String date_debut, String date_fin, String id_etablissement, Handler<Either<String, JsonObject>> handler);

    /**
     * Retourne la liste des classes dont l'enseignant à fait l'objet de remplacement et dont il possède un devoir.
     * @param classes Liste des classes sur lequel l'utilisateur à créer un devoir
     * @param user Utilisateur courant
     * @param idStructure Structure courante
     * @param handler handler portant le résultat de la requête
     */
    public void getRemplacementClasse(JsonArray classes, UserInfos user, String idStructure, Handler<Either<String, JsonArray>> handler);
}
