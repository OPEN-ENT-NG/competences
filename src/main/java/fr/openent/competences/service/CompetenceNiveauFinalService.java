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
import org.entcore.common.service.CrudService;

import java.util.List;

public interface CompetenceNiveauFinalService extends CrudService {

    /**
     * create and update a competence_niveau_final
     * @param niveauFinal niveauFinal to create or to update (with id_periode, id_eleve, id_competence, id_matiere and
     *                    id_classe)
     * @param handler Function returning data
     */
    void setNiveauFinal(JsonObject niveauFinal, Handler<Either<String,JsonObject>> handler);

    /**
     * create and update a competence_niveau_final set for the year
     * @param niveauFinal niveauFinal to create or to update (with id_periode, id_eleve, id_competence, id_matiere and
     *                    id_classe)
     * @param handler Function returning data
     */
    void setNiveauFinalAnnuel(JsonObject niveauFinal, Handler<Either<String,JsonObject>> handler);

    /**
     * delete niveaufinal
     * @param niveauFinal niveauFinal to delete (with id_periode, id_eleve, id_competence, id_matiere and id_classe)
     * @param handler Function returning data
     */
    void deleteNiveauFinal(JsonObject niveauFinal, Handler<Either<String,JsonObject>> handler);

    /**
     * get the niveau final by  a periode, a student, a competence, a subject and a class
     * @param id_periode
     * @param id_eleve
     * @param ids_matieres
     * @param id_classe
     * @param handler
     */
    void getNiveauFinalByEleve(Long id_periode, String id_eleve, List<String> ids_matieres, String id_classe, Handler<Either<String, JsonArray>> handler);

}
