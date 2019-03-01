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
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;


/**
 * Created by vogelmt on 29/03/2017.
 */
public interface BFCService extends CrudService {
    /**
     * Créer un BFC pour un élève
     * @param bfc objet contenant les informations relative au BFC
     * @param user utilisateur
     * @param handler handler portant le résultat de la requête
     */
    void createBFC(final JsonObject bfc, final UserInfos user, final Handler<Either<String, JsonObject>> handler);

    /**
     * Mise à jour d'un BFC pour un élève
     * @param data appreciation à mettre à jour
     * @param user utilisateur
     * @param handler handler portant le resultat de la requête
     */
    void updateBFC(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Suppression d'un BFC pour un élève
     * @param idBFC identifiant de la note
     * @param idEleve identifiant de l'élève
     * @param user user
     * @param handler handler portant le résultat de la requête
     */
    void deleteBFC(long idBFC, String idEleve, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Récupère les BFCs d'un élève pour chaque domaine
     * @param idEleves
     * @param idEtablissement
     * @param idCycle
     * @param handler
     */
    void getBFCsByEleve(String[] idEleves, String idEtablissement, Long idCycle, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les moyennes par domaines des élève dont l'id est passé en paramètre.
     * La map retournee a pour clé l'id de l'élève, et contient une autre map qui contient, pour chaque id de domaine racine, la moyenne simplifiée (
     * @param idEleves id des élèves dont on souhaite obtenir les moyennes pour le BFC
     * @param recapEval indique si on est dans un export de récapitulation d'évaluation
     * @param idClasse l'id de la classe à laquelle appartient l'élève
     * @param idStructure l'id de l'établissement auquel appartient la classe
     * @param handler handler portant le résultat du calcul de moyenne
     */
    void buildBFC(boolean recapEval, String[] idEleves, String idClasse, String idStructure, Long idPeriode, Long idCycle, Handler<Either<String, JsonObject>> handler);

    /**
     * retourne la date de creation du BFC, si null la date de modification sinon la date du jour
     * pour un idEleve
     * @param idEleve id de l'élève
     * @param handler handler portant le résultat
     */
    // public void getDateCreatedBFC(String idEleve, Handler<Either<String,JsonArray>> handler);

    /**
     * Récupère les valeurs de la table calc_millesime
     * @param handler handler portant le résultat
     */
    void getCalcMillesimeValues(Handler<Either<String, JsonArray>> handler);

    /**
     * Active la visibilité des moyennes sur l'écran de BFC
     * @param structureId id établissement neo
     * @param idVisibility   id de la visibilité 1--> des moyBFC ou 2-> du baremeDNB
     * @param user utilisateur connecté
     * @param visible 0 : caché pour tout le monde, 1 : caché pour les enseignants, 2 : visible pour tous
     * @param handler handler portant le résultat
     */
    void setVisibility(String structureId, Integer idVisibility,UserInfos user, Integer visible,
                              Handler<Either<String, JsonArray>> handler);


    /**
     *  Récupère la valeur de l'état de la visibilité des moyennes sur l'écran de BFC.
     *  0 : caché pour tout le monde, 1 : caché pour les enseignants, 2 : visible pour tous
     * @param structureId id établissement neo
     * @param idVisibility   id de la visibilité 1--> des moyBFC ou 2-> du baremeDNB
     * @param user utilisateur connecté
     * @param handler handler portant le résultat
     */
    void getVisibility(String structureId, Integer idVisibility, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * donne un JsonArray avec
     * la moyenne du contrôle continu qui correspond à la somme des maîtrises obtenue pour chaque domaine Racine
     * en tenant compte de la dispense d'un domaine ou non.
     * idEleve
     * et le totalMaxBaremeBrevet = nb de domaines non dispensé x MaxBaremeBrevet
     * @param eb eventBus
     * @param idsClasses des id des  classe
     * @param idPeriode identifiant de la période sélectionnée
     * @param isCycle si la périodePassée est un cycle
     * @param idCycle id du cycle
     * @param handler  handler portant le résultat
     */

    void getMoyenneControlesContinusBrevet(EventBus eb, List<String> idsClasses,Long idPeriode,
                                           Boolean isCycle, Long idCycle,
                                           final Handler<Either<String, JsonArray>> handler);


    void checkHeadTeacherForBFC(UserInfos user, String id_eleve, String id_etablissement,
                                final Handler<Boolean> handler);
}
