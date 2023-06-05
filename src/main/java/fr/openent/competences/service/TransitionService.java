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
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

public interface TransitionService  extends CrudService {

    /**
     * Effectue la transition d'année pour une structure
     * @param structure
     * @param finalHandler
     */
    void transitionAnneeStructure(final JsonObject structure,
                                  final Handler<Either<String, JsonArray>> finalHandler);
    /**
     * - Delete tables SQL of  viesco.rel_structures_personne_supp, viesco.rel_groupes_personne_supp, notes.match_class_id_transition and viesco.personnes_supp notes.transition
     *  @param handler response success
     */
    void cleanTableSql( Handler<Either<String, JsonArray>> handler);

    /**
     * Clonage des schémas viesco et notes
     * @param currentYear année utilisé pour renommer le schéma avant le clonage
     * @param handler
     */
    void cloneSchemas(final String currentYear, final String sqlVersion, final Handler<Either<String, JsonObject>> handler);

    /**
     * Effectue la purge des tables durant l'étape de post-transition
     * @param finalHandler
     */

    void clearTablePostTransition(final Handler<Either<String, JsonArray>> finalHandler);
    /**
     * get externalId class with idClass in rel_group_cycle where type=0 and put on table match_class_id_transition
     * @param handler response success
     */
    void updateSqlMatchClassIdTransition(Handler<Either<String, JsonArray>> handler);

    /**
     * Récupération des external id depuis la table match_transition
     * @param handler
     */
    void getOldIdClassTransition(final Handler<Either<String, JsonArray>> handler);

    /**
     * Récupération des nouvelles id depuis le Néo
     * @param handler
     */
    void matchExternalId(JsonArray externalIdsClasses, final Handler<Either<String, JsonArray>> handler);

    /**
     * Récupération des ids des matières depuis le Néo
     * @param handler
     */
    void getSubjectsNeo(final Handler<Either<String, JsonArray>> handler);

    /**
     * Supprimer les sous-matières plus rattacher à une matière
     * @param handler
     */
    void supprimerSousMatiereNonRattaches(JsonArray matieres, final Handler<Either<String,JsonArray>> handler);

    /**
     *Mets à jour les nouvelles id de la table match_transition et mets à jour la table rel_groupe_cycle
     * @param classesFromNeo
     * @param handler
     */
    void updateTablesTransition(JsonArray classesFromNeo, final Handler<Either<String, JsonArray>> handler);
}
