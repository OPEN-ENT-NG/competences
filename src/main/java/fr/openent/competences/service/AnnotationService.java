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

public interface AnnotationService extends CrudService {

    /** Récupère les annotations d'un établissement
     * @param idEtab identifiant d'établissement
     * @param handler handler portant le resultat de la requête
     */
    public void listAnnotations(String idEtab, Handler<Either<String, JsonArray>> handler);

    /**
     * Création d'une appréciation
     * @param appreciation
     * @param user
     * @param handler
     */
    public void createAppreciation(JsonObject appreciation, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * MAJ d'une appréciation
     * @param data
     * @param user
     * @param handler
     */
    public void updateAppreciation(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) ;

    /**
     * Suppression d'une appréciation
     * @param idAppreciation
     * @param user
     * @param handler
     */
    public void deleteAppreciation(Long idAppreciation, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Créee une anntotation à partir d'un élève et un devoir donné
     * @param idDevoir
     * @param idAnnotation
     * @param idEleve
     * @param handler
     */
    public void createAnnotationDevoir(Long idDevoir, Long idAnnotation, String idEleve, Handler<Either<String, JsonObject>> handler);

    /**
     * Modifie une anntotation à partir d'un élève et un devoir donné
     * @param idDevoir
     * @param idAnnotation
     * @param idEleve
     * @param handler
     */
    public void updateAnnotationDevoir(Long idDevoir, Long idAnnotation, String idEleve, Handler<Either<String, JsonObject>> handler);

    /**
     * Supprime une anntotation à partir d'un élève et un devoir donné
     * @param idDevoir
     * @param idEleve
     * @param handler
     */
    public void deleteAnnotation(Long idDevoir, String idEleve, Handler<Either<String, JsonObject>> handler);

    public void getAnnotationByEleveByDevoir(Long[] ids_devoir, String[] ids_eleve, Handler<Either<String, JsonArray>> handler);
}
