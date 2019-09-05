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
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public interface CompetencesService extends CrudService {

    /**
     * Récupération des compétences évaluables (feuille de l'arbre) du cycle donné.
     *
     * @param idClasse l'identifiant de la classe.
     * @param handler  handler portant le résultat de la requête
     */
    void getCompetencesItem(String idEtablissement, String idClasse, Long idCycle,
                            Handler<Either<String, JsonArray>> handler);

    void getCompetencesItem(String idEtablissement, Number idCycle, Handler<Either<String, JsonArray>> handler);

    /**
     * Setter des compétences pour un devoir donné
     * @param devoirId Id du devoir (Integer)
     * @param values Objet contenant les compétences (JsonObject)
     * @param handler handler portant le résultat de la requête
     */
    public void setDevoirCompetences(Long devoirId, JsonArray values, Handler<Either<String, JsonObject>> handler);

    /**
     * Enlever des compétences pour un devoir donné
     * @param devoirId Id du devoir (Integer)
     * @param values Objet contenant les compétences (JsonObject)
     * @param handler handler portant le résultat de la requête
     */
    void remDevoirCompetences(Long devoirId, JsonArray values, Handler<Either<String, JsonObject>> handler);

    /**
     * Getter : Récupération des compétences pour un devoir donné
     * @param devoirId id du Devoir (Integer)
     * @param handler handler portant le résultat de la requête
     */
    void getDevoirCompetences(Long devoirId,  Handler<Either<String, JsonArray>> handler);


    /**
     * Getter : Récupération des compétences pour un devoir donné
     * @param devoirIds ids des Devoirs (Integer)
     * @param handler handler portant le résultat de la requête
     */
    void getDevoirCompetences(JsonArray devoirIds,
                              String idEtablissement, Handler<Either<String, JsonArray>> handler);

    /**
     * Getter : Récupération des compétences par enseignement pour un devoir donné
     * @param devoirId
     * @param handler
     */
    void getDevoirCompetencesByEnseignement(Long devoirId, final Handler<Either<String, JsonArray>> handler);

    /**
     * Getter : Récupération des compétences par enseignement pour un devoir donné
     * @param devoirIds
     * @param handler
     */
    void getDevoirCompetencesByEnseignement(JsonArray devoirIds, final Handler<Either<String, JsonArray>> handler);
    /**
     * Getter : Récupération des compétences sélectionné sur le dernier devoir créé par
     * l'utilisateur
     * @param userId identifiant de l'utilisateur connecté
     * @param handler handler portant le résultat de la requête
     */
    void getLastCompetencesDevoir(String idEtablissement, String userId, Handler<Either<String, JsonArray>> handler);

    /**
     * Getter : Récupération des compétences suivant le niveau spécifié en paramètre
     * @param filter filtre
     * @param  idClasse : identifiant de la classe
     * @param  idCycle : identifiant du cycle. A utiliser lorsqu'on veut récupérer des
     *                 compétences d'un cycle différent du cycle de la classe
     * @param handler handler portant le résultat de la requête
     */
    void getCompetencesByLevel(String filter, String idClasse, String idCycle, Handler<Either<String, JsonArray>> handler);

    void getCompetencesByLevel(String idEtablissement, String filter, String idClasse,
                               String idCycle, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère les compétences des domaines dont l'id est passé en paramètre
     * @param idDomaines id des domaines dont on souhaite récupérer les compétences
     * @param handler handler portant le résultat de la requête
     */
    void getCompetencesDomaines(String idClasse, Long[] idDomaines, Handler<Either<String, JsonArray>> handler);

    void create(JsonObject competence, Handler<Either<String, JsonObject>> handler);

    void update(Number idComp, String idEtab, String fieldToUpdate, Object valueToUpdate,
                Handler<Either<String, JsonObject>> handler, Number idEns);

    void delete(Number id, String idEtablissement, Handler<Either<String, JsonObject>> handler);

    /**
     * Supprime les compétences customs
     * @param idEtablissement
     * @param handler handler portant le résultat de la requête
     */
    void deleteCustom(String idEtablissement,  Handler<Either<String, JsonObject>> handler);
}
