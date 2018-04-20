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
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public interface NoteService extends CrudService {

    /**
     * Créer une note pour un élève
     * @param note objet contenant les informations relative à la note
     * @param user utilisateur
     * @param handler handler portant le résultat de la requête
     */
    void createNote(final JsonObject note, final UserInfos user, final Handler<Either<String, JsonObject>> handler);

    /**
     * Recupère la liste des Notes en fonction des identifiants de devoir donnés.
     * @param devoirId identifiants des devoirs
     * @param handler handler portant le resultat de la requête
     */
    void listNotesParDevoir(Long devoirId, Handler<Either<String, JsonArray>> handler);

    /**
     * Recupere les notes d'un élève pour les devoirs passés en paramètre.
     *
     * @param idEleves l'identifiant de l'élève
     * @param idDevoirs l'identifiant du devoir
     * @param handler handler portant le résultat de la requête
     */
    void getNotesParElevesParDevoirs(String[] idEleves, Long[] idDevoirs, Handler<Either<String, JsonArray>> handler);

    /**
     * Mise à jour d'une note
     * @param data Note à mettre à jour
     * @param user user
     * @param handler handler portant le resultat de la requête
     */
    void updateNote(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Suppression d'un note en bdd
     * @param idNote identifiant de la note
     * @param user user
     * @param handler handler portant le résultat de la requête
     */
    void deleteNote(Long idNote, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Récupération des Notes pour le widget
     * @param userId identifiant de l'utilisateur
     * @param handler handler portant le résultat de la requête
     */
    void getWidgetNotes(String userId, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupération de toutes les notes par devoir des élèves
     * @param userId identifiant de l'utilisateur
     * @param etablissementId identifiant de l'établissement
     * @param classeId identifiant de la classe
     * @param matiereId identifiant de la matière
     * @param periodeId identifiant de la periode
     * @param handler handler portant le résultat de la requête
     */
    void getNoteElevePeriode(String userId, String etablissementId, String classeId, String matiereId, Long periodeId,
                             Handler<Either<String, JsonArray>> handler);

    /**
     * Récupération des toutes les notes de tous les élèves pour un relevé de notes
     * @param etablissementId identifiant de l'établissement
     * @param matiereId identifiant de la matière
     * @param periodeId identifiant de la période
     * @param handler handler portant le résultat de la requête
     */
    void getNotesReleve(String etablissementId, String classeId, String matiereId, Long periodeId,
                        Handler<Either<String, JsonArray>> handler);


    /**
     * Récupération des toutes les Competences-notes de tous les élèves pour un relevé de notes
     * @param etablissementId identifiant de l'établissement
     * @param matiereId identifiant de la matière
     * @param periodeId identifiant de la période
     * @param eleveId identifiant de l'élève
     * @param handler handler portant le résultat de la requête
     */
    void getCompetencesNotesReleve(String etablissementId, String classeId, String matiereId, Long periodeId,
                                   String eleveId,
                                   Handler<Either<String, JsonArray>> handler);

    /**
     * Supprime la colonne d'un élève pour une période, une matiere et une classe
     * @param idEleve
     * @param idPeriode
     * @param idMatiere
     * @param idClasse
     * @param handler
     */
    void deleteColonneReleve(String idEleve, Long idPeriode, String idMatiere, String idClasse,
                             String colonne,   Handler<Either<String, JsonArray>> handler);

    void getColonneReleve(JsonArray idEleves, Long idPeriode, String idMatiere, String idClasse,
                          String colonne, Handler<Either<String, JsonArray>> handler);
    /**
     * Met à jour la moyennes finale d'un élève pour une période, une matiere et une classe
     * @param idEleve
     * @param idPeriode
     * @param idMatiere
     * @param idClasse
     * @param field (moyenne, positionnement)
     * @param handler
     */
    void setColonneReleve(String idEleve, Long idPeriode, String idMatiere, String idClasse, JsonObject field,
                          String colonne,Handler<Either<String, JsonArray>> handler);

}
