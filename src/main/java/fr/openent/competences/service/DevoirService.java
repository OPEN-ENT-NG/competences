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
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public interface DevoirService extends CrudService {

    /**
     * Créer un devoir
     * @param devoir devoir à créer
     * @param user utilisateur
     * @param handler handler portant le résultat de la requête
     */
    public void createDevoir(final JsonObject devoir, UserInfos user, final Handler<Either<String, JsonObject>> handler);

    /**
     * récupère les information d'un devoir. ne récupère pas les notes.
     * @param idDevoir
     * @param handler handler portant le résultat de la requête
     */
    public void getDevoirInfo(final Long idDevoir, final Handler<Either<String, JsonObject>> handler);

    /**
     * Créer le statement SQL de création d'un devoir.
     * @param idDevoir Identifiant du devoir
     * @param devoir devoir
     * @param user utilisateur courant
     * @return Statements SQL
     */
    public JsonArray createStatement(Long idDevoir, JsonObject devoir, UserInfos user);

    /**
     * Duplique le devoir passé en paramètre sur la liste de classes passée en paramètre
     * @param idDevoir identifiant du devoir à dupliquer
     * @param devoir devoir à dupliquer
     * @param classes liste des classes
     * @param user utilisateur courant
     * @param handler handler portant le résultat de la requête
     */
    public void duplicateDevoir(Long idDevoir, JsonObject devoir, JsonArray classes, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * Met à jour un devoir
     * @param id Identifian du devoir
     * @param devoir Devoir à mettre à jour
     * @param handler
     */
    public void updateDevoir(String id, JsonObject devoir, Handler<Either<String, JsonArray>> handler);

    /**
     * Liste des devoirs de l'utilisateur
     * @param user utilisateur l'utilisateur connecté
     * @param handler handler portant le résultat de la requête
     */
    public void listDevoirs(UserInfos user, String idEtablissement, Handler<Either<String, JsonArray>> handler);

    /**
     * Liste des devoirs (avec ou sans note) pour un établissement, une classe, une matière et une période donnée.
     * La liste est ordonnée selon la date du devoir (du plus ancien au plus récent).
     * @param idEleve identifiant de l'elève lorsqu'on veut récupérer les notes
     * @param idEtablissement identifiant de l'établissement
     * @param idClasse identifiant de la classe
     * @param idMatiere identifiant de la matière
     * @param idPeriode identifiant de la période
     * @param historise evaluation historise
     * @param handler handler portant le résultat de la requête
     */
    public void listDevoirs(String idEleve, String idEtablissement, String idClasse, String idMatiere, Long
            idPeriode,boolean historise, Handler<Either<String, JsonArray>> handler);

    public void listDevoirs(String idEleve, String[] idGroupes, Long[] idDevoirs, Long[] idPeriodes,
                            String[] idEtablissement, String[] idMatieres, Boolean hasCompetences,
                            Handler<Either<String, JsonArray>> handler);

    /**
     * Liste des devoirs publiés pour un établissement et une période donnée.
     * La liste est ordonnée selon la date du devoir (du plus ancien au plus récent).
     *
     * @param idEtablissement identifiant de l'établissement
     * @param idPeriode identifiant de la période
     * @param idUser identifant de l'utilisateur
     * @param handler handler portant le résultat de la requête
     */
    public void listDevoirs(String idEtablissement, Long idPeriode, String idUser,
                            Handler<Either<String, JsonArray>> handler);
    /**
     * Récupère le nombre de notes en fonction du devoir pour un utilisateur donné
     * @param user l'utilisateur connecté
     * @param idEleves identifiants des élèves de la classe à l'instant T
     * @param idDevoir id du devoir concerné
     * @param handler handler portant le résultat de la requête
     */
    public void getNbNotesDevoirs(UserInfos user, List<String> idEleves, Long idDevoir, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère le nombre d'annotations en fonction du devoir pour un utilisateur donné
     * @param user l'utilisateur connecté
     * @param idEleves identifiants des élèves de la classe à l'instant T
     * @param idDevoir id du devoir concerné
     * @param handler handler portant le résultat de la requête
     */
    public void getNbAnnotationsDevoirs(UserInfos user, List<String> idEleves, Long idDevoir, Handler<Either<String, JsonArray>> handler);

    /**
     * verifie si le devoir est evalué ou pas
     * @param idDevoir
     * @param handler
     */
    public void getevaluatedDevoir(Long idDevoir, Handler<Either<String, JsonArray>> handler);

    /**
     * verifie si la liste des devoir est evalué ou pas
     * @param idDevoir
     * @param handler
     */
    public void getevaluatedDevoirs(Long[] idDevoir, Handler<Either<String, JsonArray>> handler);

    /**
     * Liste des devoirs pour un établissement.
     *
     * @param user
     * @param handler
     */
    public void listDevoirsEtab(UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne la liste des toutes les classes qui font ou ont fait l'objet d'un devoir par
     * l'utilisateur.
     * @param user Utilisateur en cours
     * @param structureId Identifiant de la structure
     * @param handler handler portant le résultat de la requête.
     */
    public void getClassesIdsDevoir(UserInfos user, String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère les notes du devoirs dans la base et en calcule la moyenne
     * @param idDevoir Devoir dont on souhaite avoir la moyenne
     * @param stats Booléen permettant de demander le calcul des statistique en plus
     * @param handler handler portant le résultat de la requête.
     */
    public void getMoyenne(Long idDevoir, final boolean stats, final Handler<Either<String, JsonObject>> handler);

    /**
     * Récupère le nombre de compétences en fonction du devoir pour un utilisateur donné
     * @param idGroupes La liste des devoirs désirés
     * @param handler handler portant le résultat de la requête
     */
    public void getNbCompetencesDevoirs(Long[] idGroupes, Handler<Either<String, JsonArray>> handler);

    public void getNbCompetencesDevoirsByEleve(List<String> idEleves, Long idDevoir, Handler<Either<String, JsonArray>> handler);

    public void updatePercent(Long IdDevoir, Integer percent, Handler<Either<String, JsonArray>> handler);

    public void switchVisibilityApprec(Long idDevoir, Handler<Either<String, JsonArray>> handler);
}
