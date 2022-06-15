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

import fr.openent.competences.bean.NoteDevoir;
import fr.wseduc.webutils.Either;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

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
     * create a note or update note
     *
     * @param jo jsonObject id_devoir, id_eleve, note
     * @param handler response
     */
    void createNoteDevoir(JsonObject jo, String id_user, final Handler<Either<String, JsonObject>> handler );
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

    void getNotesParElevesParDevoirs(String[] idEleves, Long[] idDevoirs, Integer idPeriode, Handler<Either<String, JsonArray>> handler);

    void getNotesParElevesParDevoirs(String[] idEleves, String[] idGroupes, Long[] idDevoirs, Integer idPeriode, Handler<Either<String, JsonArray>> handler);

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
    void getNoteElevePeriode(String userId, String etablissementId, JsonArray classeId, String matiereId, Long periodeId,
                             Handler<Either<String, JsonArray>> handler);

    /**
     * Récupération des toutes les notes de tous les élèves pour un relevé de notes
     * @param etablissementId identifiant de l'établissement
     * @param matiereId identifiant de la matière
     * @param periodeId identifiant de la période
     * @param typeClasse le type de la classe
     * @param withMoyenneFinale retourner la moyenne finale ou pas
     * @param handler handler portant le résultat de la requête
     */
    void getNotesReleve(String etablissementId, String classeId, String matiereId, Long periodeId, Integer typeClasse,
                        Boolean withMoyenneFinale,JsonArray idsGroup,Handler<Either<String, JsonArray>> handler);


    /**
     * Récupération des toutes les Competences-notes de tous les élèves pour un relevé de notes
     * @param etablissementId identifiant de l'établissement
     * @param matiereId identifiant de la matière
     * @param periodeId identifiant de la période
     * @param eleveId identifiant de l'élève
     * @param typeClasse le type de la classe
     * @param withDomaineInfo renvoit les competences-notes par domaines
     * @param isYear récupère les niveaux finaux annuels ou non
     * @param handler handler portant le résultat de la requête
     */
    void getCompetencesNotesReleve(String etablissementId, String classeId, JsonArray groupIds, String matiereId, Long periodeId,
                                   String eleveId, Integer typeClasse, Boolean withDomaineInfo, Boolean isYear,
                                   Handler<Either<String, JsonArray>> handler);

    /**
     * Supprime la colonne d'un élève pour une période, une matiere et une classe
     * @param idEleve identifiant de l'eleve
     * @param idPeriode identifiant de la période
     * @param idMatiere identifiant de la matière
     * @param idClasse idClasse
     * @param handler handler
     */
    void deleteColonneReleve(String idEleve, Long idPeriode, String idMatiere, String idClasse,
                             String colonne,   Handler<Either<String, JsonArray>> handler);

    void  getColonneReleve(JsonArray idEleves, Long idPeriode, String idMatiere, JsonArray idsClasse,
                           String colonne, Handler<Either<String, JsonArray>> handler);
    /**
     * Met à jour la moyennes finale d'un élève pour une période, une matiere et une classe
     * @param idEleve idEleve
     * @param idPeriode identifiant de la période
     * @param idMatiere identifiant de la matière
     * @param idClasse idClasse
     * @param field (moyenne, positionnement)
     * @param userId userId
     * @param handler handler
     */
    void setColonneReleve(String idEleve,
                          Long idPeriode,
                          String idMatiere,
                          String idClasse,
                          JsonObject field,
                          String colonne,
                          String userId,
                          Handler<Either<String, JsonArray>> handler);

    /**
     * Regroupe les notes des matières par coefficient puis effectue le calcul par matière
     * @param moyFinalesEleves moyFinalesEleves
     * @param listNotes notes de tous les élèves
     * @param result JsonObject of result
     * @param idEleve idEleve
     * @param idEleves id des Eleves ayant une note dans la lisNotes
     */
    void getMoyennesMatieresByCoefficient(JsonArray moyFinalesEleves, JsonArray listNotes, final JsonObject result,
                                          String idEleve, JsonArray idEleves);
    /**
     *Calcul la moyenne d'un eleve a
     * @param listNotes response of request
     * @param result JsonObject of result
     * @param idEleve idEleve
     * @param idEleves id des Eleves ayant une note dans la lisNotes
     * @param idsClassWithNoteAppCompNoteStudent idsClassWithNoteAppCompNoteStudent
     * @param idPriodeAsked idPriodeAsked
     * @param services
     * @return retourne une map avec
     */
    HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> calculMoyennesEleveByPeriode(JsonArray listNotes,
                                                                                     final JsonObject result,
                                                                                     String idEleve,
                                                                                     JsonArray idEleves,
                                                                                     List<String> idsClassWithNoteAppCompNoteStudent,
                                                                                     Long idPriodeAsked, JsonArray services);

    /**
     * Récupère toutes les appreciations, les moyennes finales et les positionnement pour un eleve, une matiere, une periode
     * @param idEleve idEleve
     * @param idMatiere idMatiere
     * @param idPeriode idPeriode
     * @param handler response
     */
    void getAppreciationMoyFinalePositionnement(String idEleve, String idMatiere, Long idPeriode, JsonArray idGroups,
                                                Handler<Either<String,JsonArray>> handler);


    /**
     * calcul la moyenne de la classe pour toute les périodes où il y a eu une note
     * @param moyFinalesEleves moyenne finale de l'élève pour une classe donnée
     * @param notesByDevoirByPeriodeClasse map<periode map<periode,liste de devoirs>>
     * @param result JsonObject sur lequel est ajouté les moyennes de la classe
     */
    void calculAndSetMoyenneClasseByPeriode(final JsonArray moyFinalesEleves,
                                            final HashMap<Long, HashMap<Long,
                                                    ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse,
                                            final JsonObject result);

    /**
     * @param idPeriod idPeriod
     * @param idEleve idEleve
     * @param notesByDevoirByPeriodeClasse notesByDevoirByPeriodeClasse
     * @param moyFinalesEleves moyFinalesEleves
     * @param result result
     */
    void setRankAndMinMaxInClasseByPeriode(final Long idPeriod,
                                           final String idEleve,
                                           final HashMap<Long, HashMap<Long,
                                                   ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse,
                                           final JsonArray moyFinalesEleves,
                                           final JsonObject result);

    void calculPositionnementAutoByEleveByMatiere(JsonArray listNotes, JsonObject result, Boolean annual,
                                                  JsonArray tableauConversion,
                                                  List<String> idsClassWithNoteAppCompNoteStudent, Long idPeriodAsked);

    void getMoyennesFinal(String[] idEleves, Integer idPeriode, String[] idMatieres, String[] idClasses,
                          Handler<Either<String, JsonArray>> handler);

    /**
     * get all notes of a student by  matiere ,idGroup
     * @param idsEleve idsEleve list
     * @param idPeriode idPeriode
     * @param handler response
     */
    void getNotesAndMoyFinaleByClasseAndPeriode(List<String> idsEleve, JsonArray idsGroups, Integer idPeriode,
                                                String idEtablissement, Handler<Either<String,JsonArray>> handler);

    /**
     * get eleve moy By matiere By class
     * @param idClasse idClasse
     * @param idPeriode idPeriode
     * @param mapAllidMatAndidTeachers mapAllidMatAndidTeachers
     * @param mapIdMatListMoyByEleve mapIdMatListMoyByEleve
     * @param handler response
     */
    void getMoysEleveByMatByPeriode(String idClasse, Integer idPeriode, String idEtablissement,
                           SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
                           Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve,
                           Handler<Either<String,JsonObject>> handler);

    /**
     * get eleve moy By matiere and By year
     * @param periodes periodes
     * @param mapAllidMatAndidTeachers mapAllidMatAndidTeachers
     * @param handler JsonObject
     */
    void getMoysEleveByMatByYear(String idEtablissement, JsonArray periodes,
                                 SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
                                 Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve,
                                 Handler<Either<String,JsonObject>> handler);

    /**
     *
     * @param mapAllidMatAndidTeachers  mapAllidMatAndidTeachers
     * @param mapIdMatListMoyByEleve mapIdMatListMoyByEleve
     * @param handler response JsonObject
     */
    void getMatEvaluatedAndStat(SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
                                Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve,
                                Handler<Either<String,JsonObject>> handler);

    /**
     * Réalise l'export d'un relevé
     * @param param objet contenant les informations relative au releve
     * @param handler handler portant le résultat de la requête
     */
    void getDatasReleve(final JsonObject param, final Handler<Either<String, JsonObject>> handler);

    /**
     * Réalise l'export totale (dans toutes les matières passé en paramètres) d'un relevé
     * @param param objet contenant les informations relative au releve
     * @param idPeriode periode sur laquelle réaliser l'export
     * @param annual booléen pour savoir si on se situe dans le cas annuel
     * @param handler handler portant le résultat de la requête
     */
    void getTotaleDatasReleve(final JsonObject param, final Long idPeriode, final boolean annual, final Handler<Either<String, JsonObject>> handler);

    /**
     * Renseigne les libéllés et paramètre ne nécessaire à ll'xport
     * @param resultFinal resultFinal
     * @param params params
     */
    void putLibelleAndParamsForExportReleve(JsonObject resultFinal, JsonObject params);

    /**
     * Permet de réaliser l'export PDF d'un relevé périodique
     * @param param param
     * @param request request
     * @param vertx vertx
     * @param config config
     */
    void exportPDFRelevePeriodique(JsonObject param, final HttpServerRequest request, Vertx vertx,
                                   JsonObject config );

    /**
     * Récupère les données pour un graph par domaine
     * @param idEleve idEleve
     * @param groupIds groupIds
     * @param idEtablissement idEtablissement
     * @param idClasse idClasse
     * @param typeClasse typeClasse
     * @param idPeriodeString idPeriodeString
     * @param isYear isYear
     * @param handler handler
     */
    void getDataGraphDomaine(final String idEleve, JsonArray groupIds, final String idEtablissement ,
                             final String idClasse, final Integer typeClasse, final String idPeriodeString,
                             final Boolean isYear, final Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère les donnée pour un graph par matière
     * @param idEleve idEleve
     * @param groupIds groupIds
     * @param idEtablissement idEtablissement
     * @param idClasse idClasse
     * @param typeClasse typeClasse
     * @param idPeriodeString idPeriodeString
     * @param isYear isYear
     * @param handler handler
     */
    void getDataGraph(final String idEleve, JsonArray groupIds, final String idEtablissement ,
                             final String idClasse,final Integer typeClasse, final String idPeriodeString,
                             final Boolean isYear, final Handler<Either<String, JsonArray>> handler);

    /**
     *
     * @param idEleve idEleve
     * @param idClasse idClasse
     * @param idMatiere idMatiere
     * @param idEtablissement idEtablissement
     * @param request request
     */
    void getDetailsReleve(final String idEleve, final String idClasse, final String idMatiere,
                          final String idEtablissement, final HttpServerRequest request);
}
