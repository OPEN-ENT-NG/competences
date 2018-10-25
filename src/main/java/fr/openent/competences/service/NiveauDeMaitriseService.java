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

/**
 * Created by anabah on 30/08/2017.
 */
public interface NiveauDeMaitriseService extends CrudService {

    /**
     * Recupère l'ensemble des couleurs des niveaux de maitrise pour un établissement.
     * @param idEtablissement identifiant de l'établissement
     * @param idCycle           identifiant du cycle (opt)
     * @param handler handler portant le resultat de la requête
     */
    public void getNiveauDeMaitrise(String idEtablissement, Long idCycle, Handler<Either<String, JsonArray>> handler);

    /**
     * Recupère l'ensemble des couleurs des niveaux de maitrise pour  un cycle
     * @param Cycle
     * @param handler
     */
    public void getNiveauDeMaitriseofCycle(Long Cycle, Handler<Either<String, JsonArray>> handler);
    /**
     * Créer un niveau de maitrise pour un établissement
     * @param maitrise objet contenant les informations relative à la note
     * @param user utilisateur
     * @param handler handler portant le résultat de la requête
     */
    public void createMaitrise(final JsonObject maitrise, final UserInfos user, final Handler<Either<String, JsonArray>> handler);

    /**
     * Mise à jour d'un niveau de maitrise
     * @param data niveau de maitrise à mettre à jour
     * @param user user
     * @param handler handler portant le resultat de la requête
     */
    public void update(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Suppression d'un niveau de maitrise
     * @param idEtablissement identifiant du niveau de maitrise
     * @param user user
     * @param handler handler portant le résultat de la requête
     */
    public void delete(String idEtablissement, UserInfos user, Handler<Either<String, JsonObject>> handler);


}
