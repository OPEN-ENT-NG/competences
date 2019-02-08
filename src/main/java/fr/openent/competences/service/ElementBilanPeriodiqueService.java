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

import java.util.List;

public interface ElementBilanPeriodiqueService {

    /**
     * Enregistrement d'une nouvelle thématique EPI ou parcours.
     * @param thematique nouvelle thématique à enregistrer
     * @param handler Handler de retour
     */
   void insertThematiqueBilanPeriodique (JsonObject thematique,
                                                 Handler<Either<String, JsonObject>> handler);

    /**
     * Enregistremet d'un nouvel élément EPI, AP ou parcours.
     * @param element nouvel élément à enregistrer
     * @param handler Handler de retour
     */
   void insertElementBilanPeriodique (JsonObject element, Handler<Either<String, JsonObject>> handler);

    /**
     * Retourne l'ensemble des thématiques d'EPI ou de parcours.
     * @param typeElement type de l'élément
     * @param handler Handler de retour
     */
   void getThematiqueBilanPeriodique (Long typeElement, String idEtablissement, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les éléments correspondants à la thématique passée en paramètre..
     * @param idThematique
     * @param handler Handler de retour
     */
   void getElementsOnThematique (String idThematique, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les élèments du bilan périodique sur la classe et/ou l'enseignant et/ou l'établissement.
     * @param idEnseignant enseignant connecté
     * @param idClasse id classe
     * @param idEtablissement id établissement
     * @param handler Handler de retour
     */
   void getElementBilanPeriodique (String idEnseignant, List<String> idClasse, String idEtablissement,
                                           Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les élèments du bilan périodique sur la classe et/ou l'enseignant et/ou l'établissement.
     * @param idEnseignant enseignant connecté
     * @param idClasse id classe
     * @param idEtablissement id établissement
     * @param handler Handler de retour
     */
    void getElementsBilanPeriodique (String idEnseignant, List<String> idClasse, String idEtablissement,
                                    Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les élèments du bilan périodique sur la classe et/ou l'enseignant et/ou l'établissement.
     * @param idElements id des éléments
     * @param handler Handler de retour
     */
   void getEnseignantsElementsBilanPeriodique (List<String> idElements, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les groupes de l'élèment du bilan périodique dont l'id est passé en paramètre.
     * @param idElement id élèment bilan périodique
     * @param handler Handler de retour
     */
   void getGroupesElementBilanPeriodique (String idElement, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les groupes de l'élèment du bilan périodique dont l'id est passé en paramètre.
     * @param idElement id élèment bilan périodique
     * @param handler Handler de retour
     */
   void getIntervenantMatiereElementBilanPeriodique (String idElement,
                                                             Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les appéciations de classe dont les id sont passés en paramètre.
     * @param idsClasses
     * @param idElements id des élèments du bilan périodique
     * @param handler Handler de retour
     */
   void getApprecBilanPerClasse (List<String> idsClasses, String idPeriode, List<String> idElements, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les appéciations d'élèves les id sont passés en paramètre.
     * @param idsClasses
     * @param idElements id des élèments du bilan périodique
     * @param handler Handler de retour
     */
   void getApprecBilanPerEleve (List<String> idsClasses, String idPeriode, List<String> idElements, String idEleve, Handler<Either<String, JsonArray>> handler);

    /**
     * Enregistrement d'une appreciation pour un élève.
     * @param idEleve id élève
     * @param idEltBilanPeriodique id élément bilan périodique
     * @param idPeriode id période
     * @param commentaire appréciation laissée par le professeur
     * @param handler Handler de retour
     */
   void insertOrUpdateAppreciationElement (String idEleve, String idClasse, String externalidClasse, Long idPeriode, Long idEltBilanPeriodique,
                                           String commentaire, JsonArray groupes, Handler<Either<String, JsonObject>> handler);

    /**
     * Mise à jour d'un élément EPI, AP ou parcours.
     * @param idElement id de l'élément à mettre à jour
     * @param element données à mettre à jour
     * @param apprecClasseOnDeletedClasses
     * @param apprecEleveOnDeletedClasses
     * @param handler Handler de retour
     */
   void updateElementBilanPeriodique (Long idElement, JsonObject element,
                                              JsonArray apprecClasseOnDeletedClasses,
                                              JsonObject apprecEleveOnDeletedClasses,
                                              List<String> deletedClasses, Handler<Either<String, JsonObject>> handler);

    /**
     * Suppression d'un élément EPI, AP ou parcours.
     * @param idEltBilanPeriodique id des éléments à supprimer
     * @param handler Handler de retour
     */
   void deleteElementBilanPeriodique (List<String> idEltBilanPeriodique, Handler<Either<String, JsonArray>> handler);
    /**
     * Suppression d'une thématique.
     * @param idThematique id des éléments à supprimer
     * @param handler Handler de retour
     */
   void deleteThematique (String idThematique, Handler<Either<String, JsonArray>> handler);

    /**
     * Mise à jour d'une appreciation pour un élève ou une classe.
     * @param idThematique id de la thématique à mettre à jour
     * @param thematique nouvelles données de la thématique
     * @param handler Handler de retour
     */
   void updateThematique (String idThematique, JsonObject thematique,
                                                   Handler<Either<String, JsonObject>> handler);

    /**
     * Suppression des appréciations d'élève d'un élément EPI, AP ou parcours.
     * @param idEltBilanPeriodique id de l'élément
     * @param handler Handler de retour
     */
   void deleteAppreciationElement (String idEleve, Long idPeriode,
                                           Long idEltBilanPeriodique,
                                           String idClasse, List<String> groupes,
                                           Handler<Either<String, JsonObject>> handler);

    /**
     * Mise à jour d'une appreciation pour un élève ou une classe.
     * @param idAppreciation id de l'appréciation à mettre à jour
     * @param commentaire nouvelle appréciation laissée par le professeur
     * @param type eleve ou classe
     * @param handler Handler de retour
     */
   void updateAppreciationBilanPeriodique (Long idAppreciation, String commentaire, String type,
                                                   Handler<Either<String, JsonObject>> handler);


    /**
     * Récupère les externalIds de classe des parcours, AP et EPI de l'enseignant en paramètre
     * @param idEtablissement
     * @param idEnseignant
     * @param handler
     */
   void getClassesElementsBilanPeriodique (String idEtablissement, String idEnseignant,
                                                   Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les appreciations liées au élèments du bilan périodiques passés en paramètre
     * @param idsClasses
     * @param idPeriode
     * @param idElements
     * @param idEleve
     * @param handler
     */
   void getAppreciations (List<String> idsClasses, String idPeriode, List<String> idElements, String idEleve,
                          Handler<Either<String, JsonArray>> handler);
}
