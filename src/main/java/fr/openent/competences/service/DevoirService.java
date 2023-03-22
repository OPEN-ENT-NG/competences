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
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;
import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserInfos;

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
    void createDevoir(final JsonObject devoir, UserInfos user, final Handler<Either<String, JsonObject>> handler);

    /**
     * récupère les information d'un devoir. ne récupère pas les notes.
     * @param idDevoir
     * @param handler handler portant le résultat de la requête
     */
    void getDevoirInfo(final Long idDevoir, final Handler<Either<String, JsonObject>> handler);

    /**
     * Get only devoir
     * @param idDevoir id devoir
     * @param handler response
     */
    void getDevoir(Long idDevoir, Handler<Either<String, JsonObject>> handler);

    void getDevoirsInfosCompetencesCondition(Long[] idDevoirs, Handler<Either<String, JsonArray>> handler);

    /**
     * récupère les information de plusieurs devoirs
     * @param idDevoirs
     * @param handler handler portant le résultat de la requête
     */
    void getDevoirsInfos(Long[] idDevoirs, Handler<Either<String, JsonArray>> handler);

    /**
     * Créer le statement SQL de création d'un devoir.
     * @param idDevoir Identifiant du devoir
     * @param devoir devoir
     * @param user utilisateur courant
     * @return Statements SQL
     */
    JsonArray createStatement(Long idDevoir, JsonObject devoir, UserInfos user);

    /**
     * Duplique le devoir passé en paramètre sur la liste de classes passée en paramètre
     * @param devoir devoir à dupliquer
     * @param teacherId
     * @param classes liste des classes
     * @param user utilisateur courant
     * @param shareService
     * @param promise
     * @param eb
     */
    void duplicateDevoir(JsonObject devoir, String teacherId, JsonArray classes, UserInfos user, ShareService shareService,
                         Promise<Void> promise, EventBus eb);

    /**
     * Met à jour un devoir
     * @param id Identifian du devoir
     * @param devoir Devoir à mettre à jour
     * @param handler
     */
    void updateDevoir(String id, JsonObject devoir, Handler<Either<String, JsonArray>> handler);

    /**
     * Liste des devoirs de l'utilisateur
     * @param user utilisateur l'utilisateur connecté
     * @param handler handler portant le résultat de la requête
     */
    void listDevoirs(UserInfos user, String idEtablissement, Integer limit, Handler<Either<String, JsonArray>> handler);

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
    void listDevoirs(String idEleve, String idEtablissement, String idClasse, String idMatiere, Long idPeriode,
                     boolean historise, Handler<Either<String, JsonArray>> handler);

    void listDevoirs(String idEleve, String[] idGroupes, Long[] idDevoirs, Long[] idPeriodes,
                     String[] idEtablissement, String[] idMatieres, Boolean hasCompetences,
                     Boolean historise, Handler<Either<String, JsonArray>> handler);

    Future<JsonArray> listDevoirs(String studentId, String[] groupIds, Long[] homeworkIds, Long[] periodIds,
                                  String[] structureIds, String[] subjectIds, Boolean hasSkills, Boolean historized);

    void listDevoirsWithAnnotations(String idEleve, Long idPeriode, String idMatiere,
                                    Handler<Either<String, JsonArray>> handler);

    void listDevoirsWithCompetences(String idEleve, Long idPeriode, String idMatiere, JsonArray groups,
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
    void listDevoirs(String idEtablissement, Long idPeriode, String idUser,
                     Handler<Either<String, JsonArray>> handler);
    /**
     * Récupère le nombre de notes en fonction du devoir pour un utilisateur donné
     * @param user l'utilisateur connecté
     * @param idDevoir id du devoir concerné
     * @param handler handler portant le résultat de la requête
     */
    void getNbNotesDevoirs(UserInfos user, Long idDevoir, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère le nombre d'annotations en fonction du devoir pour un utilisateur donné
     * @param user l'utilisateur connecté
     * @param idEleves identifiants des élèves de la classe à l'instant T
     * @param idDevoir id du devoir concerné
     * @param handler handler portant le résultat de la requête
     */
    void getNbAnnotationsDevoirs(Long idDevoir, Handler<Either<String, JsonArray>> handler);

    /**
     * verifie si le devoir est evalué ou pas
     * @param idDevoir
     * @param handler
     */
    void getevaluatedDevoir(Long idDevoir, Handler<Either<String, JsonArray>> handler);

    /**
     * verifie si la liste des devoir est evalué ou pas
     * @param idDevoir
     * @param handler
     */
    void getevaluatedDevoirs(Long[] idDevoir, Handler<Either<String, JsonArray>> handler);

    /**
     * Liste des devoirs pour un établissement.
     *
     * @param user
     * @param limit
     * @param handler
     */
    void listDevoirsChefEtab(UserInfos user, String idEtablissement, Integer limit, Handler<Either<String, JsonArray>> handler);


    /**
     * Récupère les notes du devoirs dans la base et en calcule la moyenne
     * @param idDevoir Devoir dont on souhaite avoir la moyenne
     * @param stats Booléen permettant de demander le calcul des statistique en plus
     * @param handler handler portant le résultat de la requête.
     */
    void getMoyenne(Long idDevoir, String[] idEleves, final Handler<Either<String, JsonObject>> handler);

    /**
     * Récupère le nombre de compétences en fonction du devoir pour un utilisateur donné
     * @param idGroupes La liste des devoirs désirés
     * @param handler handler portant le résultat de la requête
     */
    void getNbCompetencesDevoirs(Long[] idGroupes, Handler<Either<String, JsonArray>> handler);

    void getNbCompetencesDevoirsByEleve(Long idDevoir, Handler<Either<String, JsonArray>> handler);

    void updatePercent(Long IdDevoir, Integer percent, Handler<Either<String, JsonArray>> handler);

    void switchVisibilityApprec(Long idDevoir, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère les ids matières des devoirs sur lesquels un élève a eu une note ou une compétence ou annotation ou une appréciation,
     * les enseignants de chaque matière et les groupes de l'élève pour chaque matière pour une période donnée
     * @param id_eleve
     * @param idEtablissement
     * @param id_classe
     * @param handler
     */
    void getMatiereTeacherForOneEleve(String id_eleve, String idEtablissement, JsonArray id_classe, Handler<Either<String,JsonArray>> handler);

    void listDevoirsService(String idEnseignant, String idMatiere, List<String> idGroups, Handler<Either<String, JsonArray>> handler);

    void updateDevoirsService(JsonArray ids, String idMatiere, Handler<Either<String, JsonArray>> handler);

    void delete(JsonArray ids, Handler<Either<String, JsonObject>> handler);

    /**
     * Met à jour les tables SQL (possible suppression des moyennes finales, appreciations des classes, appreciations des
     * élèves sur une matière et période donnée et des élèments de programme) lorsqu'il n'y a plus aucun devoir sur une matière, à une période donné et à une classe ou un élève donné
     * @param handler handler portant le résultat de la requête NEO4j
     */
    void autoCleanSQLTable(Handler<Either<String, JsonObject>> handler);

    /**
     * Récupère l'id des groupes auquel chaque élève fait partie
     * @param id_classe l'id de la classe
     * @param result handler portant le résultat de la requête NEO4j
     */
    void getEleveGroups(String id_classe, Handler<Either<String, JsonArray>> result);

    /**
     * Met à jour la table SQL de la competence niveau final lorsqu'il n'y a plus aucun devoirs sur une matière, à une période donné et à une classe ou un élève donné
     * @param listEleves liste d'ids des élèves
     * @param listGroups listes d'ids des groupes et la classe auquel l'élève fait partie
     * @param idMatiere la matière du devoir supprimé
     * @param idPeriode la période du devoir supprimé
     * @param result handler portant le résultat de la requête NEO4j
     */
    void updateCompetenceNiveauFinalTableAfterDelete(List<String> listEleves, List<String> listGroups, String idMatiere, Long idPeriode, Handler<Either<String, JsonArray>> result);

    /**
     * Met à jour la table SQL des positionnements des élèves sur une matière
     * lorsqu'il n'y a plus aucun devoir sur une matière, à une période donné et à une classe ou un élève donné
     * @param listEleves liste d'ids des élèves
     * @param listGroups listes d'ids des groupes et la classe auquel l'élève fait partie
     * @param idMatiere la matière du devoir supprimé
     * @param idPeriode la période du devoir supprimé
     * @param result handler portant le résultat de la requête NEO4j
     */
    void updatePositionnementTableAfterDelete(List<String> listEleves, List<String> listGroups, String idMatiere, Long idPeriode, Handler<Either<String, JsonArray>> result);

    /**
     * Récupère les informations nécessaires pour générer un formulaire de saisie
     * @param idDevoir
     * @param acceptLanguage
     * @param host
     * @param handler
     */
    void getFormSaisieDevoir(Long idDevoir, String acceptLanguage, String host,
                             Handler<Either<String, JsonObject>> handler);

    void getHomeworksFromSubjectAndTeacher(String idSubject, String idTeacher,
                                           String groupId, Handler<Either<String, JsonArray>> handler);

    void getDevoirsEleve(String idEtablissement, String idEleve, String idMatiere, Long idPeriode,
                         Handler<Either<String, JsonObject>> handler);

    void getDevoirsNotes(String idEtablissement, String idEleve, Long idPeriode,
                         Handler<Either<String, JsonObject>> handler);

    JsonObject getNewShareStatements(String userIdSecondTeacher, String devoirID, List<String> actions);

    void duplicateDevoirs(HttpServerRequest request, UserInfos user, JsonObject body,
                          CompetencesService competencesService, ShareService shareService);
}
