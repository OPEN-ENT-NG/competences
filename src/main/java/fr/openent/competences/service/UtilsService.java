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
import fr.openent.competences.model.SubTopic;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.*;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public interface UtilsService {
    // TODO REDECOUPER LA STRUCTURE UNE FOIS L'ARCHITECTURE DEFINIE


    /**
     * Récupère la liste des professeurs remplaçants du titulaire sur un établissement donné
     * (si lien titulaire/remplaçant toujours actif à l'instant T)
     * @param psIdTitulaire identifiant neo4j du titulaire
     * @param psIdEtablissement identifiant de l'établissement
     * @param handler handler portant le resultat de la requête : la liste des identifiants neo4j des rempacants
     */
    void getRemplacants(String psIdTitulaire, String psIdEtablissement, Handler<Either<String, JsonArray>> handler);


    /**
     * Récupère la liste des professeurs titulaires d'un remplaçant sur un établissement donné
     * (si lien titulaire/remplaçant toujours actif à l'instant T)
     * @param psIdRemplacant identifiant neo4j du remplaçant
     * @param psIdEtablissement identifiant de l'établissement
     * @param handler handler portant le resultat de la requête : la liste des identifiants neo4j des titulaires
     */
    void getTitulaires(String psIdRemplacant, String psIdEtablissement, Handler<Either<String, JsonArray>> handler);

    void getMultiTeachersByClass( final String idEtablissement, final String idClasse, final Integer idPeriode,
                                   Handler<Either<String, JsonArray>> handler);
    void getMultiTeachers(final String structureId, final JsonArray groupIds, final Integer PeriodeId,
                          Handler<Either<String, JsonArray>> handler);

    void getServices(final String idEtablissement, final JsonArray idClasse, Handler<Either<String, JsonArray>> handler);

    void getDefaultServices(final String structureId, final JsonArray groupIds, Handler<Either<String,JsonArray>> handler);

    void getDefaultServices(final String structureId, final JsonArray groupIds, final JsonObject filters,
                           Handler<Either<String,JsonArray>> handler);

    void hasService(String idEtablissement, JsonArray idClasses, String idMatiere, Long idPeriode,
                    UserInfos user, Handler<Boolean> handler);


    /**
     * Liste les types de devoirs pour un etablissement donné
     *
     * @param idEtablissement identifiant de l'établissement
     * @param handler         handler portant le resultat de la requête
     */
    void listTypesDevoirsParEtablissement(String idEtablissement, Handler<Either<String, JsonArray>> handler);

    /**
     * Recupère les informations de l'élève
     *
     * @param id     identifiant de l'élève
     * @param result handler portant le résultat de la requête
     */
    void getInfoEleve(String id, Handler<Either<String, JsonObject>> result);

    /**
     * Recupère les informations de l'élève en backup
     *
     * @param id     identifiant de l'élève
     * @param result handler portant le résultat de la requête
     */
    void getInfoEleveBackup(String id, Handler<Either<String, JsonObject>> result);

    /**
     * Récupère les enfants d'une parent donné
     *
     * @param id      identifiant du parent
     * @param handler handler portant la résultat de la requête
     */
    void getEnfants(String id, Handler<Either<String, JsonArray>> handler);
    /**
     * Fonction de calcul générique de la moyenne
     * La formule suivante est utilisée :(SUM ( ni *m *ci /di)  + SUM ( nj *cj)  ) / (S ( ci)  + SUM ( cj  *dj /m)  )
     * @param listeNoteDevoirs : contient une liste de NoteDevoir.
     *                         Dans le cas ou les objets seraient des moyennes, toutes les propriétés ramener sur devront
     *                         être à false.
     * @param diviseurM        : diviseur de la moyenne. Par défaut, cette valeur est égale à 20 (optionnel).
     * @param annual        : permet de savoir si on doit faire l'arrondi à la fin.
     * @return Double : moyenne calculée
     **/
    public JsonObject calculMoyenne(List<NoteDevoir> listeNoteDevoirs, Boolean statistiques, Integer diviseurM, Boolean annual);

    /**
     * Fonction de calcul générique de la moyenne
     * La formule suivante est utilisée : SUM(notes)/ nombre/Notes
     * @param listeNoteDevoirs : contient une liste de NoteDevoir.

     * @return Double : moyenne calculée
     **/
    public JsonObject calculMoyenneParDiviseur(List<NoteDevoir> listeNoteDevoirs, Boolean statistiques);
    /**
     * Recupere un établissemnt sous sa representation en BDD
     *
     * @param id      identifiant de l'etablissement
     * @param handler handler comportant le resultat
     */
    void getStructure(String id, Handler<Either<String, JsonObject>> handler);


    /**
     * Récupère les cycles des classes dans la relation classe_cycle
     * @param idClasse liste des identifiants des classes.
     * @param handler Handler portant le résultat de la requête.
     */
    void getCycle(List<String> idClasse, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère le cycle de la classe dans la relation classe_cycle
     * @param idClasse Identifiant de la classe.
     * @param handler Handler portant le résultat de la requête.
     */
    void getCycle(String idClasse, Handler<Either<String, JsonObject>> handler);

    /**
     * Récupère la liste des utilisateurs selon les paramètres précisés
     *
     *
     * @param structureId
     * @param classId
     * @param groupId
     * @param types
     * @param filterActive
     * @param nameFilter
     * @param user
     * @param eitherHandler
     */
    void list(String structureId, String classId, String groupId, JsonArray types, String filterActive, String nameFilter, UserInfos user, Handler<Either<String, JsonArray>> eitherHandler);

    /**
     * Réalise une union de deux JsonArray de String
     * @param recipient Tableau d'accueil
     * @param list Tableau à transférer
     * @return Un JsonArray contenant les deux tableau
     */
    JsonArray saUnion(JsonArray recipient, JsonArray list);

    /**
     * Ajoute la NoteDevoir passé en paramètre à la collection associée à la clé passée. Si la collection n'existe pas, la crée.
     * @param key Clé à laquelle ajouter la valueToAdd
     * @param map La map dans laquelle faire l'ajout
     * @param valueToAdd La valeur à ajouter.
     * @param <K> Le type de la clé
     */
    <K,V> void addToMap(K key, HashMap<K, ArrayList<V>> map, V valueToAdd);
    void addToMap(String key, Long sousmatiere, HashMap<String,HashMap<Long,ArrayList<NoteDevoir>>> map,
                  NoteDevoir valueToAdd);
    <K,V> void addToMapWithJsonArray(K id, HashMap<K, JsonArray> map, V valueToAdd);

    /**
     * Récupère le nom de l'entité à qui appartient l'identifiant passé en paramètre.
     * @param name  l'identifiant de l'entité
     */
    void getNameEntity(String[] name, String field, Handler<Either<String, JsonArray>> handler);

    /**
     *
     * @param idClasses liste des classes à lier
     * @param id_cycle cycle vers lequel on lie les classes
     * @param handler
     */
    void linkGroupesCycles(final String[] idClasses, final Number id_cycle, final Number[] typeGroupes,
                           Handler<Either<String, JsonArray>> handler);

    /**
     *
     * @param idClasses
     * @param handler
     */
    void checkDataOnClasses(String[] idClasses, final Handler<Either<String, JsonArray>> handler);

    void studentIdAvailableForPeriode (final String idClasse, final Long idPeriode, Integer typeClasse,
                                       Handler<Either<String, JsonArray>> handler);

    /**
     *
     * @param idClasse id Class
     * @param idEtablissement id Eteblissement
     * @param handler periodes of the class
     */
    void getPeriodes(List<String> idClasse, String idEtablissement, Handler<Either<String,JsonArray>> handler);

    /**
     * Future getPeriodes
     * @param idClasse id Class
     * @param idEtablissement id Eteblissement
     */
    Future<JsonArray> getPeriodes(List<String> idClasse, String idEtablissement);

    /**
     *
     * @param idClasse
     * @param idEtablissement
     * @param handler
     */
    void getPeriodes(JsonArray idClasse, String idEtablissement, Handler<Either<String,JsonArray>> handler);
    /**
     * Ajouter un élement concernant les absences ou retard d'un élève sur une  période donnée
     * @param idEleve
     * @param colonne
     * @param idPeriode
     * @param value
     * @param handler
     */
    void insertEvenement (String idEleve, String colonne, Long idPeriode, Long value,
                          Handler<Either<String, JsonArray>> handler);

    /**
     * Mettre à jour l'image d'un établissement pour l'export du bulletin
     * @param idStructure
     * @param path
     * @param handler
     */
    void setStructureImage (String idStructure, String path, Handler<Either<String, JsonObject>> handler);

    /**
     * Met à jour le nom du chef etab ou de son adjoint et l'image de sa signature pour l'export du bulletin
     * @param idStructure
     * @param path
     * @param name
     * @param handler
     */
    void setInformationCE (String idStructure, String path, String name,
                           Handler<Either<String, JsonObject>> handler);
    /**
     * Récupère les informations de paramétrage pour l'export d'un établissement
     * @param idStructure
     * @param handler
     */
    void getParametersForExport (String idStructure, Handler<Either<String, JsonObject>> handler);

    /**
     *  ORDER jsonArr by sorted field
     * @param jsonArr
     * @param sortedField
     * @return
     */
    JsonArray sortArray(JsonArray jsonArr, String[] sortedField);

    void getLibelleMatAndTeacher(SortedMap<String, Set<String>> mapIdMatiereIdsTeacher,
                                 Handler<Either<String, SortedMap<String, JsonObject>>> handler);

    JsonObject findWhere(JsonArray collection, JsonObject oCriteria);

    JsonArray where(JsonArray collection, JsonObject oCriteria);

    JsonArray pluck(JsonArray collection, String key);

    JsonObject getObjectForPeriode(JsonArray array, Long idPeriode, String key);

    /**
     * A partir d'un positionnement calculé pos, retourne  le positionnement réel avec l'échelle de conversion
     * @param moyenne moyenne calculée du positionnement
     * @param tableauDeconversion tableau de conversion des niveaux du cycle de la classe
     * @return la valeur convertie grâce à l'échelle
     */
     int getPositionnementValue(Float moyenne, JsonArray tableauDeconversion);

    /**
     * Renvoit la valeur correspondant au positionnement à partir de la moyenne calculée
     * @param moyenne
     * @param tableauDeconversion
     * @param translation
     * @return
     */
    String convertPositionnement(Float moyenne, JsonArray tableauDeconversion, Boolean translation);

    /**
     * get class info with eventBus
     * @param idClass id class
     * @param handler response
     */
    void getClassInfo(final String idClass, Handler<Either<String, JsonObject>> handler);

    void lauchTransition(List<String> structureIds);

    void getYearsAndPeriodes(String idStructure, boolean onlyYear, Handler<Either<String,JsonObject>> handler);

    /**
     * get unregularized reasons id from presences
     * @param idStructure
     * @param handler
     */
    void getPresencesReasonsId(String idStructure, Handler<Either<String, JsonArray>> handler);

    /**
     * getPeriode one periode By classes
     * @param idEtablissement
     * @param idClasses
     * @param idPeriode
     * @param handler
     */
    void getPeriodesClasses (String idEtablissement, JsonArray idClasses,
                             Long idPeriode, final Handler<Either<String, JsonArray>> handler);

    void getYearsArchive(String idStructure, String type,  Handler<Either<String, JsonArray>> defaultResponseHandler);

    void getSubTopicCoeff(String idEtablissement, String idClasse, Promise<List<SubTopic>> promise);

    void getSubTopicCoeff(String idEtablissement,  Promise<List<SubTopic>> promise);
}