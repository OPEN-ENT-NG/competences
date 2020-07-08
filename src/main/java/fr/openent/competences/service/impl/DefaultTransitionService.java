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

package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.TransitionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static org.entcore.common.sql.SqlResult.validResultHandler;

public class DefaultTransitionService extends SqlCrudService implements TransitionService{
    protected static final Logger log = LoggerFactory.getLogger(DefaultTransitionService.class);
    private final Neo4j neo4j = Neo4j.getInstance();
    public DefaultTransitionService() {
        super(Competences.COMPETENCES_SCHEMA, Competences.TRANSITION_TABLE);
    }

    @Override
    public void transitionAnneeStructure(final JsonObject structure,
                                         final Handler<Either<String, JsonArray>> finalHandler) {
        String idStructureATraiter =  structure.getString("id");
        log.info("DEBUT : transition année : isStructure : " + idStructureATraiter);

        checkIfEtabActif(idStructureATraiter, handlerBusGetStrucuresActives(structure,finalHandler, idStructureATraiter));

    }

    private void checkIfEtabActif(String idStructureATraiter, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT EXISTS(SELECT id_etablissement FROM "+ Competences.COMPETENCES_SCHEMA + ".etablissements_actifs " +
                "WHERE actif = TRUE AND id_etablissement = ? ) as etab_actif";

        values.add(idStructureATraiter);

        Sql.getInstance().prepared(query, values, new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.
                getInteger("timeout-transaction") * 1000L), SqlResult.validUniqueResultHandler(handler));
    }

    private Handler<Either<String, JsonObject>> handlerBusGetStrucuresActives(JsonObject structure, Handler<Either<String, JsonArray>> finalHandler,
                                                                              String idStructureATraiter) {
        return event -> {
            if (event.isRight()) {
                Boolean etab_actif = event.right().getValue().getBoolean("etab_actif");
                if(etab_actif){
                    conditionsToDoTransition(idStructureATraiter, handlerCheckTransitionCondition(finalHandler,
                            idStructureATraiter, structure));
                } else {
                    log.warn("transition année : établissement inactif : id Etablissement : " + idStructureATraiter);
                    log.info("FIN : transition année ");
                    finalHandler.handle(new Either.Left<>("transition année : établissement inactif : id Etablissement : " + idStructureATraiter));
                }
            } else {
                log.error("transition année : problème dans la requête chechant si l'établissement est actif");
                log.info("FIN : transition année ");
                finalHandler.handle(new Either.Left<>("transition année : problème dans la requête chechant si l'établissement est actif"));
            }
        };
    }

    private void conditionsToDoTransition(String idStructureATraiter, Handler<Either<String, JsonObject>> handler) {
        JsonArray valuesCount = new fr.wseduc.webutils.collections.JsonArray();

        String queryDevoir = "SELECT id FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs WHERE id_etablissement= ? AND owner !='id-user-transition-annee' ";

        String queryPeriode = "SELECT id FROM " + Competences.VSCO_SCHEMA + ".periode WHERE id_etablissement = ? ";

        String queryTransition = "SELECT id_etablissement FROM " + Competences.COMPETENCES_SCHEMA + ".transition WHERE id_etablissement = ? ";

        String query = " SELECT EXISTS(" + queryDevoir + ") as has_devoir, "+
                "EXISTS(" + queryPeriode + ") as has_periode, " +
                "EXISTS(" + queryTransition + ") as has_transition";

        valuesCount.add(idStructureATraiter).add(idStructureATraiter).add(idStructureATraiter);

        Sql.getInstance().prepared(query, valuesCount, new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.
                getInteger("timeout-transaction") * 1000L), SqlResult.validUniqueResultHandler(handler));
    }

    private Handler<Either<String, JsonObject>> handlerCheckTransitionCondition(Handler<Either<String, JsonArray>> finalHandler,
                                                                                String idStructureATraiter, JsonObject structure) {
        return event -> {
            if(event.isLeft()){
                log.error(event.left().getValue());
                finalHandler.handle(new Either.Left<>(
                        "transition année : l'établissement a une erreur dans la récupération des valeurs des conditions : " + idStructureATraiter));
            }else {
                log.info(event.right().getValue());
                Boolean hasDevoir = event.right().getValue().getBoolean("has_devoir");
                Boolean hasPeriode = event.right().getValue().getBoolean("has_periode");
                Boolean hasTransition = event.right().getValue().getBoolean("has_transition");

                if (hasTransition) {
                    log.warn("transition année : établissement déjà effectuée : " +
                            "id Etablissement : " + idStructureATraiter);
                    finalHandler.handle(new Either.Left<>(
                            "transition année : l'établissement a déjà effectué sa transition d'année : " + idStructureATraiter));
                } else {
                    if (!hasDevoir || !hasPeriode) {
                        if (!hasDevoir)
                            log.warn("transition année : établissement n'a pas de devoir :" +
                                    " id Etablissement : " + idStructureATraiter);
                        if (!hasPeriode)
                            log.warn("transition année : établissement n'a pas de periode " +
                                    "paramétrée : id Etablissement : " + idStructureATraiter);
                        finalHandler.handle(new Either.Left<>(
                                "transition année : établissement n'a pas de devoir ou de periodes : id Etablissement : " + idStructureATraiter));
                    } else {
                        Map<String, List<String>> classeIdsEleves = new HashMap<>();
                        List<String> vListIdsGroupesATraiter = new ArrayList<>();
                        Map<String, String> vMapGroupesATraiter = new TreeMap<>();
                        if (structure.containsKey("classes")) {
                            List<String> listIdClassWithPeriode = new ArrayList<>();
                            classesWithPeriode(idStructureATraiter, handlerGetInfosClasses(classeIdsEleves, vListIdsGroupesATraiter,
                                    vMapGroupesATraiter, listIdClassWithPeriode, structure, idStructureATraiter, finalHandler)
                            );
                        } else {
                            log.warn("transition année :  erreur lors de la récupération des groupes : id Etablissement : " + idStructureATraiter);
                            finalHandler.handle(new Either.Left<>(
                                    "transition année :  erreur lors de la récupération des groupes : id Etablissement : " + idStructureATraiter));
                        }


                    }
                }
            }
        };
    }

    private void classesWithPeriode(String id_etablissement, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT id_classe FROM "+ Competences.VSCO_SCHEMA +".periode WHERE id_etablissement = ?";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray().add(id_etablissement);
        Sql.getInstance().prepared(query, values,new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.
                getInteger("timeout-transaction") * 1000L), SqlResult.validResultHandler(handler));
    }

    private Handler<Either<String, JsonArray>> handlerGetInfosClasses(Map<String, List<String>> classeIdsEleves, List<String> vListIdsGroupesATraiter,
                                                                      Map<String, String> vMapGroupesATraiter, List<String> listIdClassWithPeriode,
                                                                      JsonObject structure, String idStructureATraiter,
                                                                      Handler<Either<String, JsonArray>> finalHandler) {
        return event -> {
            if(event.isRight()) {
                JsonArray idClassWithPeriodeja = event.right().getValue();
                for (int i = 0; i < idClassWithPeriodeja.size(); i++) {
                    listIdClassWithPeriode.add(idClassWithPeriodeja
                            .getJsonObject(i).getString("id_classe"));
                }
                JsonArray vJsonArrayClass = structure.getJsonArray("classes");
                // On récupère la liste des classes à traiter si elles ont une période d'initialisée
                // INFO : Les periodes ne sont plus utiles cette année à prioris, => handle le cas où pas de période au cas où
                for (int i = 0; i < vJsonArrayClass.size(); i++) {
                    JsonObject vJsonObjectClasse = vJsonArrayClass.getJsonObject(i);
                    if (vJsonObjectClasse.containsKey("classId")) {
                        String classId = vJsonObjectClasse.getString("classId");
                        if (listIdClassWithPeriode.contains(classId)) {
                            vListIdsGroupesATraiter.add(classId);
                            vMapGroupesATraiter.put(classId, vJsonObjectClasse.getString("className"));
                            // On récupère la liste des élèves de chaque classe
                            if (vJsonObjectClasse.containsKey("users")) {
                                JsonArray vJsonArrayIdUsersClasse = vJsonObjectClasse.getJsonArray("users");
                                List<String> vListIdUsersClasse = new ArrayList<>();
                                if (vJsonArrayIdUsersClasse.size() > 0) {
                                    for (int j = 0; j < vJsonArrayIdUsersClasse.size(); j++) {
                                        vListIdUsersClasse.add(vJsonArrayIdUsersClasse.getString(j));
                                    }
                                }
                                classeIdsEleves.put(classId, vListIdUsersClasse);
                            }
                        }
                    }
                }
                vListIdsGroupesATraiter.sort(Comparator.naturalOrder());
                executeTransitionForStructure(classeIdsEleves, vListIdsGroupesATraiter, vMapGroupesATraiter,
                        idStructureATraiter, finalHandler);
            }else{
                log.warn("transition année :  erreur lors de la récupération des classes dans la table " + Competences.VSCO_SCHEMA + ".periode :" +
                        " id Etablissement : " + idStructureATraiter);
                finalHandler.handle(new Either.Left<>(
                        "transition année :  erreur lors de la récupération des classes dans la table " + Competences.VSCO_SCHEMA + ".periode :" +
                                " id Etablissement : " + idStructureATraiter));
            }
        };
    }

    private void executeTransitionForStructure(Map<String, List<String>> classeIdsEleves, List<String> pListIdsGroupesATraiter,
                                               Map<String, String> vMapGroupesATraiter, String idStructureATraiter,
                                               Handler<Either<String, JsonArray>> finalHandler) {
        // On récupère les ids des prochains devoirs pour créer la liste des identifiants de devoir nécessaires à la création de devoir
        int nbrDevoirsToCreate = pListIdsGroupesATraiter.size();
        String queryNextVal = "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".devoirs_id_seq') AS id FROM generate_series(1," + nbrDevoirsToCreate + ")";
        sql.raw(queryNextVal, SqlResult.validResultHandler(result -> {
            if (result.isRight()) {
                // On ajoute l'id du cours aux cours à créer.
                Map<String,Long> vMapGroupesIdsDevoirATraiter = new HashMap<>();
                JsonArray listIds = result.right().getValue();
                for (int i = 0; i < nbrDevoirsToCreate; i++) {
                    vMapGroupesIdsDevoirATraiter.put(pListIdsGroupesATraiter.get(i), listIds.getJsonObject(i).getLong("id"));
                }
                transitionAnneeStructure(classeIdsEleves,pListIdsGroupesATraiter,vMapGroupesATraiter,vMapGroupesIdsDevoirATraiter,
                        idStructureATraiter, event -> {
                            if (event.isRight()) {
                                log.info("FIN : transition année id Etablissement : " + idStructureATraiter);
                                finalHandler.handle(new Either.Right<>(new JsonArray().add(idStructureATraiter)));
                            } else if (event.isLeft()){
                                log.error("FIN : transition année id Etablissement ERREUR : " + idStructureATraiter +
                                        " Erreur  : " + event.left().getValue());
                                finalHandler.handle(new Either.Left<>(
                                        "FIN : transition année id Etablissement ERREUR : " + idStructureATraiter));
                            }
                        });
            }
        }));
    }

    private static final String _id_user_transition_annee = "id-user-transition-annee";
    private static final String key_username_user_transition_annee ="transition.bilan.annee";
    private static final String key_libelle_classe_transition_annee = "transition.bilan.annee.classe";

    /**
     * * Effectue la transistion d'année de l'établissement actif passé en paramètre
     * @param classeIdsEleves : Map <idClasse,List<IdsEleves>>
     * @param vListIdsGroupesATraiter : List idsClasses
     * @param vMapGroupesATraiter : Map <idClasse,Nom Classe>
     * @param vMapGroupesIdsDevoirATraiter : Map <idClasse, id Devoir>
     * @param idStructureATraiter : id Structure en cours de traitement
     * @param handler
     */
    private void transitionAnneeStructure(Map<String,List<String>> classeIdsEleves,  List<String> vListIdsGroupesATraiter,
                                          Map<String,String> vMapGroupesATraiter, Map<String,Long> vMapGroupesIdsDevoirATraiter,
                                          String idStructureATraiter, Handler<Either<String, JsonArray>> handler) {

        log.info("DEBUT : transactions pour la transition année id Etablissement : " + idStructureATraiter);
        if (vListIdsGroupesATraiter != null && vListIdsGroupesATraiter.size()>0) {
            //BCP de logs, illisible
            //log.info("INFO : transactions pour la transition année vListIdsGroupesATraiter  : " + vListIdsGroupesATraiter.toString());
        } else {
            log.warn("WARN : transactions pour la transition année vListIdsGroupesATraiter : Aucun groupe ");
        }
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();

        // Conservation des  compétences max par l'élève
        manageDevoirsAndCompetences(idStructureATraiter, vMapGroupesATraiter, vMapGroupesIdsDevoirATraiter,
                classeIdsEleves, statements);

        // Transition pour l'établissement effectué
        JsonArray valuesTransition = new fr.wseduc.webutils.collections.JsonArray();
        valuesTransition.add(idStructureATraiter);
        String queryInsertTransition ="INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".transition(id_etablissement) VALUES (?)";
        statements.add(new JsonObject().put("statement", queryInsertTransition).put("values", valuesTransition).put("action", "prepared"));

        Sql.getInstance().transaction(statements,new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L),
                SqlResult.validResultHandler(handler));

        log.info("FIN : transactions pour la transition année id Etablissement : " + idStructureATraiter);
    }

    /**
     * Conservation des  compétences max par l'élève
     * @param idStructureATraiter
     * @param vMapGroupesATraiter
     * @param vMapGroupesIdsDevoirATraiter
     * @param classeIdsEleves
     * @param statements
     */
    private void manageDevoirsAndCompetences( String idStructureATraiter, Map<String,String> vMapGroupesATraiter,
                                              Map<String, Long> vMapGroupesIdsDevoirATraiter, Map<String,List<String>> classeIdsEleves,
                                              JsonArray statements) {
        JsonArray values;// Ajout de l'utilisateur pour la transition année
        String username = "NC";
        String classname = "Bilan Année classe : ";
        values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(_id_user_transition_annee).add(username).add(username);
        String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".users(id, username) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET username = ?";
        statements.add(new JsonObject().put("statement", query).put("values", values).put("action", "prepared"));

        if(vMapGroupesATraiter.size() > 0) {
            // Création des évaluations libre par classe de l'établissement
            values = new fr.wseduc.webutils.collections.JsonArray();
            values.add(true).add(idStructureATraiter).add(idStructureATraiter);
            String queryInsertDevoir = "WITH temp_periode AS ( " +
                    "SELECT type.id as id_type, MAX(periode.id_type) as max_periode_id_type, MAX(periode.date_fin_saisie) as max_periode_date_fin_saisie," +
                    "periode.id_classe as periode_id_classe " +
                    "FROM " + Competences.COMPETENCES_SCHEMA + ".type , " + Competences.VSCO_SCHEMA + ".periode WHERE type.default_type = ? AND type.id_etablissement = ? AND periode.id_etablissement = ? " +
                    "GROUP BY periode.id_etablissement,type.id, periode.id_classe ) " +
                    "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".devoirs(id,owner, name, id_type, id_etablissement, diviseur, ramener_sur, date_publication," +
                    " is_evaluated, id_etat, percent, apprec_visible, eval_lib_historise,id_periode, date) ";

            for (Map.Entry<String, String> entry : vMapGroupesATraiter.entrySet()) {
                String idClasse = entry.getKey();
                values.add(idClasse);
                queryInsertDevoir += "  (" +
                        "   SELECT " + vMapGroupesIdsDevoirATraiter.get(idClasse) + ",'" + _id_user_transition_annee + "','" +
                        classname + entry.getValue() + "', temp_periode.id_type,'" + idStructureATraiter + "', 20, false, current_date, " +
                        "false, 1, 0, false, true , temp_periode.max_periode_id_type,temp_periode.max_periode_date_fin_saisie FROM temp_periode " +
                        "WHERE temp_periode.periode_id_classe = ? ) UNION ALL";
            }

            queryInsertDevoir = queryInsertDevoir.substring(0,queryInsertDevoir.length()-10);

            statements.add(new JsonObject()
                    .put("statement", queryInsertDevoir)
                    .put("values", values)
                    .put("action", "prepared"));
        }

        for (Map.Entry<String, String> entry : vMapGroupesATraiter.entrySet()) {
            String idClasse = entry.getKey();
            List<String> vListEleves = classeIdsEleves.get(idClasse);
            if(null != vListEleves && vListEleves.size() > 0) {
                JsonArray valuesMaxCompetence = new fr.wseduc.webutils.collections.JsonArray();
                for (String idEleve : vListEleves) {
                    valuesMaxCompetence.add(idEleve);
                }

                String queryMaxCompNoteNiveauFinalByPeriode = "(SELECT competences_notes.id_competence, " +
                        "competences_notes.id_eleve, devoirs.id_periode, CASE " +

                        "WHEN competence_niveau_final.id_eleve IS NULL AND competence_niveau_final_annuel.id_eleve IS NULL" +
                        "   THEN MAX(competences_notes.evaluation) " +

                        "WHEN competence_niveau_final.id_eleve IS NOT NULL AND competence_niveau_final_annuel.id_eleve IS NULL" +
                        "   THEN MAX(competence_niveau_final.niveau_final) " +

                        "ELSE MAX(competence_niveau_final_annuel.niveau_final) " +

                        "END AS max_comp "+
                        "FROM " + Competences.COMPETENCES_SCHEMA + ".competences_notes " +
                        "INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".devoirs ON devoirs.id = competences_notes.id_devoir " +

                        "LEFT JOIN " + Competences.COMPETENCES_SCHEMA + ".competence_niveau_final " +
                        "ON devoirs.id_periode = competence_niveau_final.id_periode " +
                        "AND competences_notes.id_competence = competence_niveau_final.id_competence " +
                        "AND competences_notes.id_eleve = competence_niveau_final.id_eleve " +

                        "LEFT JOIN " + Competences.COMPETENCES_SCHEMA + ".competence_niveau_final_annuel " +
                        "ON competences_notes.id_competence = competence_niveau_final_annuel.id_competence " +
                        "AND competences_notes.id_eleve = competence_niveau_final_annuel.id_eleve " +

                        "WHERE competences_notes.owner != '" + _id_user_transition_annee +
                        "' AND competences_notes.id_eleve IN " + Sql.listPrepared(vListEleves.toArray()) +
                        "GROUP BY competences_notes.id_competence, competences_notes.id_eleve, competence_niveau_final.id_eleve," +
                        "competence_niveau_final_annuel.id_eleve, devoirs.id_periode)";

                // Ajout du max des compétences ou du niveau final pour chaque élève
                //Cette requête fait peur
                String queryInsertMaxCompetenceNoteG = "" +
                        "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".competences_notes(id_devoir, id_competence, evaluation, owner, id_eleve) " +
                        "( SELECT " + vMapGroupesIdsDevoirATraiter.get(idClasse) +
                        ", id_competence, MAX(max_comp), '" + _id_user_transition_annee + "', id_eleve " +
                        "FROM "+queryMaxCompNoteNiveauFinalByPeriode+" AS tmax GROUP BY id_competence, id_eleve )";

                statements.add(new JsonObject()
                        .put("statement", queryInsertMaxCompetenceNoteG)
                        .put("values", valuesMaxCompetence)
                        .put("action", "prepared"));
            }
        }

        // Création des compétences par devoir (historisé)
        values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(idStructureATraiter);
        String queryInsertCompetenceDevoir = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".competences_devoirs (id_devoir, id_competence, index) " +
                "( " +
                "    SELECT competences_notes.id_devoir,competences_notes.id_competence,0" +
                "    FROM " + Competences.COMPETENCES_SCHEMA + ".competences_notes " +
                "           INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".devoirs ON competences_notes.id_devoir = devoirs.id" +
                "    WHERE " +
                "           devoirs.eval_lib_historise = true" +
                "           AND id_etablissement = ? " +
                "    GROUP BY id_devoir, id_competence  " +
                ")";

        statements.add(new JsonObject()
                .put("statement", queryInsertCompetenceDevoir)
                .put("values", values)
                .put("action", "prepared"));
    }

    @Override
    public void clearTablePostTransition(Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        SqlStatementsBuilder statements = new SqlStatementsBuilder();

        // Suppression devoir non historisé
        String queryDeleteDevoirNonHistorise = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs  " +
                "WHERE eval_lib_historise = false ";

        statements.prepared(queryDeleteDevoirNonHistorise, params);

        String queryUsers = "" +
                "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".users " +
                "WHERE" +
                " NOT EXISTS ( " +
                "    SELECT 1 " +
                "    FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs " +
                "    WHERE " +
                "     devoirs.owner = users.id " +
                " )";
        statements.prepared(queryUsers, params);

        String queryTruncate = "TRUNCATE TABLE " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.APPRECIATIONS_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.APPRECIATION_CLASSE_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.APPRECIATION_CPE_BILAN_PERIODIQUE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.APPRECIATION_ELT_BILAN_PERIODIQUE_ELEVE_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.APPRECIATION_ELT_BILAN_PERIODIQUE_CLASSE_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.APPRECIATION_MATIERE_PERIODE_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.AVIS_CONSEIL_DE_CLASSE_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.AVIS_CONSEIL_ORIENTATION_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.BULLETIN_PARAMETERS_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.COMPETENCE_NIVEAU_FINAL + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.COMPETENCE_NIVEAU_FINAL_ANNUEL + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.DISPENSE_DOMAINE_ELEVE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.ELEMENT_PROGRAMME_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.ELEVES_IGNORES_LSU_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.ELT_BILAN_PERIODIQUE_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.MOYENNE_FINALE_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.POSITIONNEMENT + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.REL_GROUPE_APPRECIATION_ELT_ELEVE_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.REL_ELT_BILAN_PERIODIQUE_GROUPE_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.REL_ELT_BILAN_PERIODIQUE_INTERVENANT_MATIERE_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.SYNTHESE_BILAN_PERIODIQUE_TABLE + ", " +
                Competences.COMPETENCES_SCHEMA + "." + Competences.THEMATIQUE_BILAN_PERIODIQUE_TABLE + ", " +
                Competences.VSCO_SCHEMA + "." + Competences.VSCO_ABSENCES_ET_RETARDS + ", " +
                Competences.VSCO_SCHEMA + "." + Competences.VSCO_PERIODE + ", " +
                Competences.VSCO_SCHEMA + "." + Competences.VSCO_SERVICES_TABLE;

        statements.prepared(queryTruncate, params);

        // Suppresion des relations groupes d'enseignement - cycle
        String queryRelGroupeType= "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle WHERE type_groupe > 0";
        statements.prepared(queryRelGroupeType, params);

        Sql.getInstance().transaction(statements.build() ,new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.
                        getInteger("timeout-transaction") * 1000L), SqlResult.validResultHandler(handler)
        );
    }

    @Override
    public void cloneSchemas(final String currentYear, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = createStatements(currentYear);

        Sql.getInstance().transaction(statements, new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.
                getInteger("timeout-transaction") * 1000L), event -> {
            JsonObject result = event.body();
            if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
                handler.handle(new Either.Right<>(result));
            } else {
                handler.handle(new Either.Left<>(result.getString("message")));
            }
        });
    }

    public void cleanTableSql(Handler<Either<String, JsonArray>> handler) {
        JsonArray emptyParams = new JsonArray();
        SqlStatementsBuilder statements = new SqlStatementsBuilder();
        String truncateQuery = "TRUNCATE " + VSCO_SCHEMA + ".rel_structures_personne_supp," +
                VSCO_SCHEMA + ".rel_groupes_personne_supp," +
                VSCO_SCHEMA + ".personnes_supp," +
                COMPETENCES_SCHEMA + ".transition," +
                COMPETENCES_SCHEMA + ".match_class_id_transition CASCADE";

        statements.prepared(truncateQuery, emptyParams);

        Sql.getInstance().transaction(statements.build(),
                SqlResult.validResultHandler(handler)
        );
    }

    @Override
    public JsonArray createStatements(final String currentYear){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();

        statements.add(new JsonObject()
                .put("statement", "ALTER SCHEMA " + Competences.COMPETENCES_SCHEMA + " RENAME TO " + Competences.COMPETENCES_SCHEMA + "_" + currentYear)
                .put("values", new fr.wseduc.webutils.collections.JsonArray())
                .put("action", "prepared"));


        StringBuilder queryForClone = new StringBuilder()
                .append("SELECT clone_schema_with_sequences(?::text, ?::text, TRUE)");

        JsonArray valuesForCloneNotes = new fr.wseduc.webutils.collections.JsonArray()
                .add(Competences.COMPETENCES_SCHEMA + "_" + currentYear).add(Competences.COMPETENCES_SCHEMA);

        statements.add(new JsonObject()
                .put("statement", queryForClone.toString())
                .put("values", valuesForCloneNotes)
                .put("action", "prepared"));

        statements.add(new JsonObject()
                .put("statement", "ALTER SCHEMA " + Competences.VSCO_SCHEMA + " RENAME TO " + Competences.VSCO_SCHEMA + "_" + currentYear)
                .put("values", new fr.wseduc.webutils.collections.JsonArray())
                .put("action", "prepared"));


        JsonArray valuesForCloneVieSco = new fr.wseduc.webutils.collections.JsonArray()
                .add(Competences.VSCO_SCHEMA + "_" + currentYear).add(Competences.VSCO_SCHEMA);

        statements.add(new JsonObject()
                .put("statement", queryForClone.toString())
                .put("values", valuesForCloneVieSco)
                .put("action", "prepared"));

        return statements;
    }

    public void updateSqlMatchClassIdTransition( Handler<Either<String, JsonArray>> handler){
        getIdGroupInSql(eventIdGroups -> {
            if (eventIdGroups.isLeft()) {
                handler.handle(new Either.Left<>("Error in getIdGroupInSql function: " + eventIdGroups.left().getValue()));
                return;
            }
            List<String> idClasses = eventIdGroups.right().getValue();
            getExternalIdInNeo4j(idClasses, eventIdClassesAndExternals -> {
                if (eventIdClassesAndExternals.isLeft()) {
                    handler.handle(new Either.Left<>("Error in getExternalIdInNeo4j function: " + eventIdGroups.left().getValue()));
                    return;
                }
                JsonArray idClassesAndExternals = eventIdClassesAndExternals.right().getValue();
                insertSqlMatchClassIdTransition(idClassesAndExternals, eventIdClassAndExternal -> {
                    if (eventIdClassAndExternal.isLeft()) {
                        handler.handle(new Either.Left<>("Error in insertSqlMatchClassIdTransition function: " + eventIdGroups.left().getValue()));
                        return;
                    }
                    handler.handle(new Either.Right<>(eventIdClassAndExternal.right().getValue()));
                });
            });
        });
    }

    private void getIdGroupInSql(Handler<Either<String, List<String>>> handler) {
        StringBuilder query = new StringBuilder()
                .append("SELECT ")
                .append("rgc.id_groupe AS id_group ")
                .append("FROM ")
                .append(COMPETENCES_SCHEMA).append(".rel_groupe_cycle AS rgc ")
                .append("WHERE ")
                .append("rgc.type_groupe = 0;");

        Sql.getInstance()
                .prepared(query.toString(), new JsonArray(), validResultHandler(eventIdGroups -> {
                    if (eventIdGroups.isLeft()) {
                        handler.handle(new Either.Left<>("Error in getIdGroupInSql function " + eventIdGroups.left().getValue()));
                    }
                    try {
                        JsonArray idGroupsDirty = eventIdGroups.right().getValue();
                        List<String> idGroups = idGroupsDirty.stream()
                                .map(t -> ((JsonObject) t).getString("id_group"))
                                .collect(Collectors.toList());
                        handler.handle(new Either.Right<>(idGroups));
                    } catch (Exception errorMapping) {
                        handler.handle(new Either.Left<>("Error in getIdGroupInSql function, errorMapping: " + errorMapping.toString()));
                    }
                }));
    }

    private void getExternalIdInNeo4j(List<String> idClasses, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder()
                .append("MATCH (class:Class) ")
                .append("WHERE class.id IN {idClasses} ")
                .append("RETURN class.id AS idClass, class.externalId AS externalId");

        neo4j.execute(
                query.toString(),
                new JsonObject().put("idClasses",
                        idClasses),
                Neo4jResult.validResultHandler(handler));
    }

    private void insertSqlMatchClassIdTransition(
            JsonArray idClassesAndExternals,
            Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        StringBuilder preparedQueryInsert = new StringBuilder();

        for (int i = 0; i < idClassesAndExternals.size(); i++) {
            JsonObject idClassAndExternal = idClassesAndExternals.getJsonObject(i);

            preparedQueryInsert.append("(?, ?)");
            if (i < idClassesAndExternals.size() - 1) preparedQueryInsert.append(", ");

            params.add(idClassAndExternal.getString("idClass")) // First id class
                    .add(idClassAndExternal.getString("externalId")); // Second external id
        }

        StringBuilder query = new StringBuilder()
                .append("INSERT INTO ")
                .append(COMPETENCES_SCHEMA).append(".match_class_id_transition ")
                .append("(old_class_id, external_id) ") // First id class and second external id
                .append("VALUES ")
                .append(preparedQueryInsert)
                .append(";");

        Sql.getInstance()
                .prepared(query.toString(), params, validResultHandler(handler));
    }

    public void getOldIdClassTransition(final Handler<Either<String,JsonArray>> handler) {
        String query = "SELECT external_id FROM " + Competences.COMPETENCES_SCHEMA + ".match_class_id_transition";

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        sql.prepared(query, values, validResultHandler(handler));
    }

    public void matchExternalId(JsonArray externalIdsClasses, final Handler<Either<String,JsonArray>> handler) {
        String query = "MATCH (c:Class) WHERE c.externalId IN {idsClasses} return c.id as id, c.externalId as externalId";

        JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < externalIdsClasses.size(); i++) {
            JsonObject o = externalIdsClasses.getJsonObject(i);
            if (o.containsKey("external_id")) ids.add(o.getString("external_id"));
        }

        neo4j.execute(query, new JsonObject().put("idsClasses", ids), event -> {
            JsonObject body = event.body();

            if(body.getString("status").equals("ok")) {
                JsonArray classesGetFromNeo = body.getJsonArray("result");
                handler.handle(new Either.Right<String, JsonArray>(classesGetFromNeo));
            } else {
                String message = body.getString("message") + " -> PB while getting classes from Neo";
                log.error(message);
                handler.handle(new Either.Left<String, JsonArray>(message));
            }
        });
    }

    public void getSubjectsNeo(final Handler<Either<String,JsonArray>> handler) {
        String query = "MATCH (s:Subject) RETURN s.id as id";

        neo4j.execute(query, new JsonObject(), event -> {
            JsonObject body = event.body();
            if(body.getString("status").equals("ok")) {
                JsonArray subjectsGetFromNeo = body.getJsonArray("result");
                handler.handle(new Either.Right<String, JsonArray>(subjectsGetFromNeo));
            } else {
                String message = body.getString("message") + " -> PB while getting subjects from Neo";
                log.error(message);
                handler.handle(new Either.Left<String, JsonArray>(message));
            }
        });
    }

    public void supprimerSousMatiereNonRattaches(JsonArray matieres, final Handler<Either<String,JsonArray>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder();

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < matieres.size(); i++) {
            String id = matieres.getJsonObject(i).getString("id");
            if(id != null)
                values.add(id);
        }

        query.append("DELETE FROM ").append(Competences.VSCO_SCHEMA).append(".").append(Competences.VSCO_SOUS_MATIERE_TABLE)
                .append(" WHERE id_matiere NOT IN ").append(Sql.listPrepared(matieres.getList()));

        statements.add(new JsonObject()
                .put("statement", query.toString())
                .put("values", values)
                .put("action", "prepared"));

        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(handler));
    }


    private void updateNewIdClassTransition(JsonArray statements, JsonArray classesFromNeo) {
        StringBuilder query = new StringBuilder();

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < classesFromNeo.size(); i++) {
            JsonObject o = classesFromNeo.getJsonObject(i);
            String id = o.getString("id");
            String externalId = o.getString("externalId");

            if(id != null && externalId != null) {
                query.append("UPDATE " + Competences.COMPETENCES_SCHEMA + ".match_class_id_transition " +
                        "SET new_class_id = ? WHERE external_id = ?;");
                values.add(id).add(externalId);
            }
        }
        statements.add(new JsonObject()
                .put("statement", query.toString())
                .put("values", values)
                .put("action", "prepared"));
    }

    private void updateRelationGroupeCycle(JsonArray statements) {
        String query = "UPDATE " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle r " +
                "SET id_groupe = m.new_class_id " +
                "FROM " + Competences.COMPETENCES_SCHEMA + ".match_class_id_transition m " +
                "WHERE m.old_class_id = r.id_groupe AND new_class_id != null;";

        statements.add(new JsonObject()
                .put("statement", query)
                .put("values", new fr.wseduc.webutils.collections.JsonArray())
                .put("action", "prepared"));
    }

    public void updateTablesTransition(JsonArray classesFromNeo, final Handler<Either<String,JsonArray>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();

        updateNewIdClassTransition(statements, classesFromNeo);
        updateRelationGroupeCycle(statements);

        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(handler));
    }
}