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

import fr.openent.competences.model.achievements.AchievementsProgress;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public interface CompetenceNoteService extends CrudService {

    /**
     * Créer une compétenceNote
     * @param competenceNote objet contenant les informations relatives à la compétenceNote
     * @param idUser utilisateur courant
     * @param handler handler portant le résultat de la requête
     */
    void createCompetenceNote(JsonObject competenceNote, String idUser, Handler<Either<String, JsonObject>> handler);
    /**
     * Met à jour une compétence note
     * @param id identifiant de la compétence note à mettre à jour
     * @param competenceNote objet contenant les informations relatives à la compétenceNote
     * @param user utilisateur courant
     * @param handler handler portant le résultat de la requête
     */
    void updateCompetenceNote(String id, JsonObject competenceNote, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Supprimer une compétence Note
     * @param id identifiant de la compétence note à supprimer
     * @param handler handler portant le résultat de la requête
     */
    void deleteCompetenceNote(String id, Handler<Either<String, JsonObject>> handler);

    /**
     * Recupère toutes les notes des compétences pour un devoir donné et un élève donné
     * @param idDevoir identifiant du devoir
     * @param idEleve identifiant de l'élève
     * @param returnNotEvaluatedcompetences  si on retourne les compétences non évaluées
     * @param idCycle identifiant du cycle sélectionné
     * @param handler handler portant le résultat de la requête
     */
    void getCompetencesNotes(Long idDevoir, String idEleve, Boolean returnNotEvaluatedcompetences,
                             Long idCycle, Handler<Either<String, JsonArray>> handler);

    /**
     * Recupère toutes les notes des compétences pour un devoir donné et un élève donné
     * @param idDevoirs identifiant du devoir
     * @param idEleve identifiant de l'élève
     * @param returnNotEvaluatedcompetences  si on retourne les compétences non évaluées
     * @param idCycle identifiant du cycle sélectionné
     * @param handler handler portant le résultat de la requête
     */
    void getCompetencesNotes(JsonArray idDevoirs, String idEleve, Boolean returnNotEvaluatedcompetences,
                             Long idCycle, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne toutes les notes des compétences pour un devoir donné
     * @param idDevoir identifiant du devoir
     * @param handler handler portant le résultat de la requête
     */
    void getCompetencesNotesDevoir(Long idDevoir, Handler<Either<String, JsonArray>> handler);

    /**
     * Met à jour une liste de compétences notes pour un devoir donné
     * @param _datas liste des compétences notes à mettre à jour
     * @param handler handler portant le résultat de la requête
     */
    void updateCompetencesNotesDevoir(JsonArray _datas, Handler<Either<String, JsonArray>> handler);

    /**
     * Créer une liste de compétences notes pour un devoir donné
     * @param _datas liste des compétences notes à créer
     * @param handler handler portant le résultat de la requête
     */
    void createCompetencesNotesDevoir(JsonArray _datas, String idUser, Handler<Either<String, JsonArray>> handler);
    /**
     * Supprimer une liste de compétences notes
     * @param oIdsJsonArray liste d'identifiant à supprimer
     * @param handler handler portant le résultat de la requête
     */
    void dropCompetencesNotesDevoir(JsonArray oIdsJsonArray, Handler<Either<String, JsonArray>> handler);

    /**
     * Get max or average competenceNote
     * @param idEleves id Eleves
     * @param idPeriode id Periode
     * @param isYear isYear
     * @param isSkillAverage isSkillAverage
     * @param  handler handler
     */
    void getMaxOrAverageCompetenceNoteEleveByPeriod (String[] idEleves, Long idPeriode, Boolean isYear, Boolean isSkillAverage,
                                            Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère toutes les compétences notes d'un élève
     * @param idEleve identifiant de l'élève
     * @param idPeriode identifiant de la période
     * @param idCycle identifiant du cycle
     * @param isCycle indique si on demande les competences sur un cycles ou pas
     * @param handler handler portant le résultat de la requête
     */
    void getCompetencesNotesEleve(String idEleve, Long idPeriode, Long idCycle, boolean isCycle, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère toutes les compétences notes d'une classe soit le max de la note chaque item soit la moyenne des notes de chaque item
     * pour un periode donnée ou l'année
     * @param idEleves identifiant des élèves de la classe
     * @param idPeriode identifiant de la période
     * @param isSkillAverage chois du calcul
     * @param handler handler portant le résultat de la requête
     */
    void getMaxOrAverageCompetencesNotesClasse(List<String> idEleves, Long idPeriode, Boolean isSkillAverage,
                                               Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère la table de correspendance entre (Moyenne Note - Evaluation competence) d'un cycle et etablissment donné
     * @param idEtablissement identifiant de l'établissement
     * @param idclasse identifiant de la classe
     * @param handler handler
     **/
    void getConversionNoteCompetence(String idEtablissement, String idclasse, Handler<Either<String, JsonArray>> handler);

    /**
     *  Récupère la table de correspendance entre (Moyenne Note - Evaluation competence) d'un cycle et etablissment donné
     * @param structureId  structure Id
     * @param classId class Id
     * @return Future
     */
    Future<JsonArray> getConversionNoteCompetence(String structureId, String classId);

    /**
     *  Récupère la table de correspendance entre (Moyenne Note - Evaluation competence) d'un cycle et etablissment donné et d'une liste de classe
     * @param idEtablissement identifiant de l'établissement
     * @param idsClasses list of classes
     * @param isClassList boolean for the params of request
     * @param handler response
     */
    void getConversionTableByClass(String idEtablissement, List<String> idsClasses, Boolean isClassList,
                                   Handler<Either<String, JsonArray>> handler);
    /**
     * Récupère les cycles des groupes sur lequels un élève a des devoirs avec compétences notées
     * @param idEleve id de l'élève
     * @param handler handler portant le résultat de la requête
     */
    void getCyclesEleve(String idEleve, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère toutes les compétences notes d'une classe soit le max de la note chaque item soit la moyenne des notes de chaque item
     * pour un cycle
     * @param id_eleve identifiant des élèves de la classe
     * @param idCycle id du cycle
     * @param isSkillAverage choix du calcul
     * @param handler handler portant le résultat de la requête
     */
    void getMaxOrAverageCompetenceNoteEleveByCycle (String[] id_eleve, Long idCycle, Boolean isSkillAverage,
                                                    Handler<Either<String, JsonArray>> handler);

    /**
     * utilise la méthode getConversionNoteCompetence pour associer ordre du niveau de compétence au barème du brevet
     * @param idEtablissement id de la structure
     * @param idClasse id de la classe
     * @param handler retourne la réponse
     */
    void getMaxBaremeMapOrderBaremeBrevet(String idEtablissement, String idClasse,
                                          Handler<Either<String,Map<Integer, Map<Integer,Integer>>>> handler);

    /**
     * Get skills validated percentage by subject for each achievement's student (one Achievements for one student_id)
     *
     * @param structureId structure identifier filter on
     * @param studentIds  students identifier to filter on
     * @param periodId    period identifier to filter on
     * @param groupId     group identifier to filter on
     * @return return Future containing students (Achievements) SubjectsSkillsValidatedPercentage
     */
    Future<List<AchievementsProgress>> getStudentsSubjectsSkillsValidatedPercentage(String structureId, List<String> studentIds,
                                                                                    Long periodId, String groupId);

    /**
     * same that getStudentsSubjectsSkillsValidatedPercentage, but only for one student
     *
     * @param structureId structure identifier filter on
     * @param studentId   student identifier to filter on
     * @param periodId    period identifier to filter on
     * @param groupId     group identifier to filter on
     * @return same that getStudentsSubjectsSkillsValidatedPercentage, but only for one student
     */
    Future<AchievementsProgress> getStudentSubjectsSkillsValidatedPercentage(String structureId, String studentId,
                                                                             Long periodId, String groupId);
}
