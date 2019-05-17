/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
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
 */

package fr.openent.competences.service;

import fr.openent.competences.bean.lsun.Donnees;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public interface LSUService {

    /**
     * Permet de sélectionner parmis les disciplines sélectionnées de l'établissement, celles qui sont évaluées
     * (reférencées dans au moins une balise suiviAcquis d'un élève).
     *
     * @param idsEvaluatedDiscipline
     * @param donnees
     * @param errorsExport
     */
    void validateDisciplines(JsonArray idsEvaluatedDiscipline, Donnees donnees, JsonObject errorsExport);

    /**
     * @return retourne le tableau des disciplines évaluées au court d'un export
     */
    JsonArray getIdsEvaluatedDiscipline() ;

    /**
     * Ajoute un id de discipline évaluée à la liste pendant un export
     * @param idDiscipline
     */
    void addIdsEvaluatedDiscipline( Object idDiscipline);

    /**
     * Crée une liste vide dans laquelle on va stocker les ids des disciplines évaluées dans un export
     */
    void initIdsEvaluatedDiscipline();

    /**
     * Permet de remplir les logs lorsqu'un service a terminé son traitement
     * @param answer si le traitement est terminé ou pas
     * @param count le nombre de fois que le service c'est executé pour un thread donné (élève)
     * @param thread le thread
     * @param method le nom du service appelé
     */
    void serviceResponseOK (AtomicBoolean answer, int count, String thread, String method);

    void getIdClassIdCycleValue(List<String> classIds, final Handler<Either<String, List<Map>>> handler);

    void getMapIdClassCodeDomaineById(List<String> idClass, final Handler<Either<String, Map<String,Map<Long, String>>>> handler);

    /**
     * Rajoute un/des élève(s) dans la table des élèves à ignorer pour une classe et une période donnée
     * @param idsStudents liste des ids des élèves à ignorer pour une classe
     * @param idPeriode identifiants de la période
     * @param idClasse identifiant de la classe
     * @param handler contient le résultat de la réponse
     */
    void addUnheededStudents(JsonArray idsStudents, Long idPeriode, String idClasse,
                               final Handler<Either<String, JsonArray>> handler);

    /**
     * Rajoute un/des élève(s) dans la table des élèves à ignorer pour une classe et une période donnée
     * @param idsStudents liste des ids des élèves à ignorer pour une classe
     * @param idPeriode identifiants de la période
     * @param idClasse identifiant de la classe
     * @param handler contient le résultat de la réponse
     */
    void remUnheededStudents(JsonArray idsStudents, Long idPeriode, String idClasse,
                               final Handler<Either<String, JsonArray>> handler);
    /**
     * get deleted student by classe and deleted date > min(resqueted periodes)
     * @param periodesByClass resqueted periodes of the class
     * @param handler response
     */
    void getDeletedStudentsPostgres(Map<String,JsonArray> periodesByClass, Map<String, JsonObject> mapDeleteStudent,
                                    Handler<Either<String, Map<String, JsonObject>>> handler);

    /**
     * get all students of the icClass list
     * @param idStrure idStructure
     * @param idsClass requested idsClass
     * @param idsDeletedStudent deleted students of Postgres
     * @param handler response
     */
    void getAllStudentWithRelatives(String idStrure, List<String> idsClass, List<String> idsDeletedStudent,Handler<Either<String,JsonArray>> handler);

    /**
     * Récupère la liste un/des élève(s) dans la table des élèves à ignorer pour une classe et une période donnée
     * @param idPeriodes array de périodes
     * @param idClasses array de classes
     * @param idStructure identifiant de l'établissement
     * @param handler contient le résultat de la réponse
     */
    void getUnheededStudents(JsonArray idPeriodes, JsonArray idClasses, String idStructure,
                             final Handler<Either<String, JsonArray>> handler);


    /**
     * Récupère la liste un/des élève(s) dans la table des élèves à ignorer pour une classe et une période donnée
     * @param idPeriodes array de périodes
     * @param idClasses array de classes
     * @param handler contient le résultat de la réponse
     */
    void getUnheededStudents(JsonArray idPeriodes, JsonArray idClasses,
                             final Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère les élèves pour le LSU
     * @param classids
     * @param studentsFuture
     * @param count
     * @param answer
     * @param thread
     * @param method
     */
    void getStudents(final List<String> classids, Future<Message<JsonObject>> studentsFuture,
                     AtomicInteger count, AtomicBoolean answer, final String thread, final String method);

    /**
     * Calcule le nombre de périodes sur lesquels un élève est ignoré
     * @param idEleve identifiant de l'élève
     * @param idClasse identifiant de la classe
     * @param periodesByClass Map de période par classe
     * @param periodeUnheededStudents Map des élèves ignorés <idPeriode, {"idClasse":["idEleve"]}></idPeriode,>
     * @return
     */
    int nbIgnoredTimes(String idEleve, String idClasse, Map<String,JsonArray> periodesByClass,
                       Map<Long, JsonObject> periodeUnheededStudents);

    /**
     *
     * @param idEleve
     * @param idClasse
     * @param idPeriode
     * @param periodeUnheededStudents
     * @return
     */
    Boolean isIgnorated(String idEleve, String idClasse, Long idPeriode,Map<Long, JsonObject> periodeUnheededStudents);

    /**
     * Met à jour la map des élèves ignorés sur des périodes et des classes
     * @param ignoredStudentFuture Future contenant les élèves ignorés
     * @param periodeUnheededStudents Map à remplir <idPeriode, {"idClasse":["idEleve"]}></idPeriode,>
     */
    void setLsuUnheededStudents(Future<JsonArray> ignoredStudentFuture, Map<Long, JsonObject> periodeUnheededStudents);

    /**
     *
     * @param students
     * @param unheededStudents
     * @return
     */
    JsonArray filterUnheededStrudentsForBfc(JsonArray students, JsonArray unheededStudents );
}
