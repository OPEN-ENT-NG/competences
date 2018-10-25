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
 * Created by agnes.lapeyronnie on 03/11/2017.
 */
public interface BfcSyntheseService extends CrudService {
    /**
     * crée la synthese d'un eleve
     * @param synthese
     * @param user
     * @param handler
     */
    public void createBfcSynthese(JsonObject synthese, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * met à jour la synthese d'un eleve
     *
     * @param id
     * @param synthese
     * @param handler
     */

   public void updateBfcSynthese(String id, JsonObject synthese, Handler<Either<String, JsonObject>> handler);

    /**
     * supprime la synthese d'un eleve
     * @param id
     * @param handler
     */

   void deleteBfcSynthese(String id, Handler<Either<String, JsonObject>> handler);

    /**
     * recupere la synthese d'un eleve
     * @param idEleve
     * @param handler
     */
    public void getBfcSyntheseByEleve(String idEleve, Integer idCycle, Handler<Either<String, JsonObject>> handler);

    /**
     *
     * @param idsEleve tableau des idsEleve pour un cycle donné
     * @param idCycle cycle des élèves sélectionnés
     * @param handler
     */
    public void getBfcSyntheseByIdsEleve(String[] idsEleve, Long idCycle, Handler<Either<String, JsonArray>> handler);

    /**
     *
     * @param idsEleve tableau des idsEleve pour un cycle donné
     * @param idClasse classe des élèves sélectionnés
     * @param handler
     */
    public void getBfcSyntheseByIdsEleveAndClasse(String[] idsEleve, String idClasse, Handler<Either<String, JsonArray>> handler);

    /**
     * return idCycle
     * @param IdEleve
     * @param handler
     */
    public void getIdCycleWithIdEleve(String IdEleve, Handler<Either<String, Integer>> handler);

}
