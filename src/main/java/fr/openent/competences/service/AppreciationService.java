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
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public interface AppreciationService extends CrudService {

    /**
     * Créer une note pour un élève
     * @param appreciation objet contenant les informations relative à la note
     * @param user utilisateur
     * @param handler handler portant le résultat de la requête
     */
    void createAppreciation(final JsonObject appreciation, final UserInfos user, final Handler<Either<String, JsonObject>> handler);

    /**
     * Mise à jour d'une appreciation
     * @param data appreciation à mettre à jour
     * @param user user
     * @param handler handler portant le resultat de la requête
     */
    void updateAppreciation(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Suppression d'une appreciation en bdd
     * @param idAppreciation identifiant de la note
     * @param user user
     * @param handler handler portant le résultat de la requête
     */
    void deleteAppreciation(Long idAppreciation, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Créer ou mettre à jour une appreciation d'une classe pour une période et matière donnée
     * @param appreciation appréciation saisie
     * @param id_classe id classe neo
     * @param id_periode id periode
     * @param id_matiere id matiere neo
     * @param handler handler portant le résultat de la requête
     */
    void createOrUpdateAppreciationClasse(String appreciation, String id_classe, Integer id_periode, String id_matiere, Handler<Either<String, JsonObject>> handler);

    /**
     * Récupérer une appreciation d'une classe pour une période et matière donnée
     * @param id_classes id classe neo
     * @param id_periode id periode
     * @param id_matieres id matiere neo
     * @param handler handler portant le résultat de la requête
     */
    void getAppreciationClasse(String[] id_classes, Integer id_periode, String[] id_matieres, Handler<Either<String, JsonArray>> handler);

    /**
     *
     * @param classIds class
     * @param periodId
     * @param subjectsIds
     * @return results on Future
     */
    Future<JsonArray> getAppreciationClass(String[] classIds, Integer periodId, String[] subjectsIds);
}
