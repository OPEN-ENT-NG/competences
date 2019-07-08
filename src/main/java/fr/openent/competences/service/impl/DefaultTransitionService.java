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
import fr.wseduc.webutils.I18n;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.*;

import static fr.openent.competences.Competences.TRANSITION_CONFIG;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultTransitionService extends SqlCrudService implements TransitionService{
    protected static final Logger log = LoggerFactory.getLogger(DefaultTransitionService.class);

    public DefaultTransitionService() {
        super(Competences.COMPETENCES_SCHEMA, Competences.TRANSITION_TABLE);
    }

    private int compteurStructureTraitee ;
    public void initiateCounter() {
        this.compteurStructureTraitee = 0;
    }
    public void incrementCounter() {
        this.compteurStructureTraitee ++;
    }
    public int getCounter() {
        return this.compteurStructureTraitee;
    }

    @Override
    public void transitionAnneeStructure(EventBus eb, final JsonObject structure, final Handler<Either<String, JsonArray>> finalHandler) {
        String idStructureATraiter =  structure.getString("id");
        List<String> idStructures= new ArrayList<String>();
        idStructures.add (idStructureATraiter);
        int nbStructureATraiter = 1;
        log.info("DEBUT : transition année : isStructure : " + idStructureATraiter);
        JsonObject action = new JsonObject()
                .put("action", "structure.getStructuresActives")
                .put("module","notes");
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status"))) {
                    List<String> vListIdEtabActifs = new ArrayList<>();
                    final JsonArray listIdsEtablisement = body.getJsonArray("results");
                    if (listIdsEtablisement.size() > 0) {
                        for (int i = 0; i < listIdsEtablisement.size(); i++) {
                            vListIdEtabActifs.add(listIdsEtablisement.getJsonObject(i).getString("id_etablissement"));
                        }
                    }
                    if (vListIdEtabActifs.size() > 0 && null != idStructures && idStructures.size() > 0) {
                        // Si l'établissement à traiter est actif on comme la transition d'année pour cet établissement
                        if(vListIdEtabActifs.contains(idStructureATraiter)){
                            conditionsToDoTransition(idStructureATraiter, new Handler<Either<String, JsonObject>>() {
                                @Override
                                public void handle(Either<String, JsonObject> event) {
                                    if(event.isLeft()){
                                        log.error(event.left().getValue());
                                        endTransition(nbStructureATraiter, finalHandler, idStructures);
                                    }else {
                                        log.info(event.right().getValue());
                                        Boolean hasDevoir = event.right().getValue().getBoolean("has_devoir");
                                        Boolean hasPeriode = event.right().getValue().getBoolean("has_periode");
                                        Boolean hasTransition = event.right().getValue().getBoolean("has_transition");

                                        if (hasTransition) {
                                            log.warn("transition année : établissement déjà effectuée : " +
                                                    "id Etablissement : " + idStructureATraiter);
                                            endTransition(nbStructureATraiter, finalHandler, idStructures);
                                        } else {
                                            if (!hasDevoir || !hasPeriode) {
                                                if (!hasDevoir)
                                                    log.warn("transition année : établissement n'a pas de devoir :" +
                                                            " id Etablissement : " + idStructureATraiter);
                                                if (!hasPeriode)
                                                    log.warn("transition année : établissement n'a pas de periode " +
                                                            "paramétrée : id Etablissement : " + idStructureATraiter);
                                                endTransition(nbStructureATraiter, finalHandler, idStructures);
                                            } else {
                                                Map<String, List<String>> classeIdsEleves = new HashMap<String, List<String>>();
                                                List<String> vListIdsGroupesATraiter = new ArrayList<>();
                                                Map<String, String> vMapGroupesATraiter = new HashMap<String, String>();
                                                if (structure.containsKey("classes")) {
                                                    JsonArray vJsonArrayClass = structure.getJsonArray("classes");
                                                    // On récupère la liste des classes à traiter
                                                    for (int i = 0; i < vJsonArrayClass.size(); i++) {
                                                        JsonObject vJsonObjectClasse = vJsonArrayClass.getJsonObject(i);
                                                        if (vJsonObjectClasse.containsKey("classId")) {
                                                            String classId = vJsonObjectClasse.getString("classId");
                                                            vListIdsGroupesATraiter.add(classId);
                                                            vMapGroupesATraiter.put(classId, vJsonObjectClasse.getString("className"));
                                                            // On récupère la liste des élèves de chaque classe
                                                            if (vJsonObjectClasse.containsKey("users")) {
                                                                JsonArray vJsonArrayIdUsersClasse = vJsonObjectClasse.getJsonArray("users");
                                                                List<String> vListIdUsersClasse = new ArrayList<String>();
                                                                if (vJsonArrayIdUsersClasse.size() > 0) {
                                                                    for (int j = 0; j < vJsonArrayIdUsersClasse.size(); j++) {
                                                                        vListIdUsersClasse.add(vJsonArrayIdUsersClasse.getString(j));
                                                                    }
                                                                }
                                                                classeIdsEleves.put(classId, vListIdUsersClasse);
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    log.warn("transition année :  erreur lors de la récupération des groupes : id Etablissement : " + idStructureATraiter);
                                                    endTransition(nbStructureATraiter, finalHandler, idStructures);
                                                }
                                                executeTransitionForStructure(classeIdsEleves, vListIdsGroupesATraiter, vMapGroupesATraiter, idStructureATraiter, 1, finalHandler, idStructures);

                                            }
                                        }
                                    }
                                }
                            });
                        } else {
                            log.warn("transition année : établissement inactif : id Etablissement : " + idStructureATraiter);
                        }
                    } else {
                        log.warn("transition année : Aucun établissement actif ou à traiter");
                        log.info("FIN : transition année ");
                        finalHandler.handle(new Either.Left<String,JsonArray>("Transition d'année arrêtée : Aucun établissement actif"));
                    }
                } else {
                    log.error("transition année : Impossible de récupérer les établissements actifs");
                    log.info("FIN : transition année ");
                    finalHandler.handle(new Either.Left<String,JsonArray>(body.getString("message")));
                }
            }}));

    }

    @Override
    public void conditionsToDoTransition(String idStructureATraiter, Handler<Either<String, JsonObject>> handler) {
        JsonArray valuesCount = new fr.wseduc.webutils.collections.JsonArray();

        String queryNbDevoir = "(SELECT count(*) as nb_devoir FROM notes.devoirs d " +
                "WHERE id_etablissement= ? AND d.owner !='id-user-transition-annee' )";

        String queryNbPeriode = "SELECT count(*) as nb_periode FROM viesco.periode p " +
                "WHERE id_etablissement = ? ";

        String queryNbTransition = "SELECT count(*) as nb_transition FROM notes.transition WHERE id_etablissement = ? ";

        String query = " SELECT CASE nb_devoir WHEN 0 THEN FALSE ELSE TRUE END as has_devoir, " +
                "CASE nb_periode WHEN 0 THEN FALSE ELSE TRUE END as has_periode, " +
                "CASE nb_transition WHEN 0 THEN FALSE ELSE TRUE END as has_transition FROM ( " + queryNbDevoir +" ) as t_nbdevoir , "+
                "( " + queryNbPeriode + ") as t_nbperiode, ( " + queryNbTransition + ") as t_nbtransition " +
                "WHERE nb_devoir = 0 OR nb_periode = 0 OR nb_transition = 0 OR nb_devoir > 0";


        valuesCount.add(idStructureATraiter).add(idStructureATraiter).add(idStructureATraiter);

        Sql.getInstance().prepared(query, valuesCount, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void transitionAnnee(EventBus eb, final List<String> idStructures, final Handler<Either<String, JsonArray>> finalHandler) {
        log.info("DEBUT : transition année ");
        JsonObject action = new JsonObject()
                .put("action", "structure.getStructuresActives")
                .put("module","notes");
        // On récupère tout d'abord la liste des établissements actifs
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status"))) {

                    List <String> vListIdEtabActifs = new ArrayList<>();
                    final JsonArray listIdsEtablisement = body.getJsonArray("results");
                    if(listIdsEtablisement.size() > 0){
                        for (int i = 0; i < listIdsEtablisement.size(); i++) {
                            vListIdEtabActifs.add(listIdsEtablisement.getJsonObject(i).getString("id_etablissement"));
                        }
                    }
                    if (vListIdEtabActifs.size() > 0 &&  null != idStructures && idStructures.size() > 0){
                        initiateCounter();
                        final int nbStructureATraiter = idStructures.size();
                        for (int i = 0; i < idStructures.size(); i++) {
                            String idStructureATraiter = idStructures.get(i);
                            // Si l'établissement à traiter est actif on comme la transition d'année pour cet établissement
                            if(vListIdEtabActifs.contains(idStructureATraiter)){
                                JsonArray valuesCount = new fr.wseduc.webutils.collections.JsonArray();
                                valuesCount.add(idStructureATraiter);
                                String isTransitionCount = "SELECT COUNT(*) FROM notes.transition WHERE id_etablissement = ? ";
                                Sql.getInstance().prepared(isTransitionCount, valuesCount, new Handler<Message<JsonObject>>() {
                                    @Override
                                    public void handle(Message<JsonObject> sqlResultCount) {
                                        Long nbTransition = SqlResult.countResult(sqlResultCount);
                                        if(nbTransition > 0){
                                            log.warn("transition année : établissement déjà effectuée : id Etablissement : " + idStructureATraiter);
                                            endTransition(nbStructureATraiter, finalHandler, idStructures);
                                        } else {
                                            JsonObject action = new JsonObject()
                                                    .put("action", "classe.listClasses")
                                                    .put("idStructure", idStructureATraiter);
                                            // On récupère la liste des classes à traiter
                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> message) {
                                                    log.info("DEBUT : transition année id Etablissement : " + idStructureATraiter);
                                                    JsonObject body = message.body();
                                                    if ("ok".equals(body.getString("status"))) {
                                                        JsonArray listGroupes = body.getJsonArray("results");
                                                        if(listGroupes.size() > 0){
                                                            JsonArray jsonArryIdGroupe = new fr.wseduc.webutils.collections.JsonArray();
                                                            for (int i = 0; i < listGroupes.size(); i++) {
                                                                JsonObject vGroupe = listGroupes.getJsonObject(i).getJsonObject("m").getJsonObject("data");
                                                                jsonArryIdGroupe.add(vGroupe.getString("id"));
                                                            }
                                                            JsonObject action = new JsonObject()
                                                                    .put("action", "classe.getElevesClasses")
                                                                    .put("idClasses", jsonArryIdGroupe);
                                                            // On récupère la liste des élèves de chaque classe
                                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                                @Override
                                                                public void handle(Message<JsonObject> message) {
                                                                    JsonObject body = message.body();

                                                                    if ("ok".equals(body.getString("status"))) {


                                                                        Map<String,List<String>> classeIdsEleves = new HashMap<String,List<String>> ();
                                                                        JsonArray queryResult = body.getJsonArray("results");
                                                                        for (int i = 0; i < queryResult.size(); i++) {
                                                                            JsonObject tempJsonObjectResult = queryResult.getJsonObject(i);
                                                                            String idClasse = tempJsonObjectResult.getString("idClasse");
                                                                            String idEleve = tempJsonObjectResult.getString("idEleve");
                                                                            List<String> vListIdEleves ;
                                                                            if(classeIdsEleves.containsKey(idClasse)){
                                                                                vListIdEleves = classeIdsEleves.get(idClasse);
                                                                            } else {
                                                                                vListIdEleves = new ArrayList<String>();
                                                                            }
                                                                            vListIdEleves.add(idEleve);
                                                                            classeIdsEleves.put(idClasse,vListIdEleves);
                                                                        }
                                                                        List<String> vListIdsGroupesATraiter = new ArrayList<>();
                                                                        Map<String,String> vMapGroupesATraiter = new HashMap<String,String>();

                                                                        for (int i = 0; i < listGroupes.size(); i++) {
                                                                            JsonObject vGroupe = listGroupes.getJsonObject(i).getJsonObject("m").getJsonObject("data");
                                                                            vListIdsGroupesATraiter.add(vGroupe.getString("id"));
                                                                            vMapGroupesATraiter.put(vGroupe.getString("id"),vGroupe.getString("name"));
                                                                        }
                                                                        executeTransitionForStructure(classeIdsEleves, vListIdsGroupesATraiter,vMapGroupesATraiter, idStructureATraiter, nbStructureATraiter, finalHandler, idStructures);
                                                                    }
                                                                }
                                                            }));

                                                        } else {
                                                            log.warn("transition année :  aucune classe : id Etablissement : " + idStructureATraiter);
                                                            endTransition(nbStructureATraiter, finalHandler, idStructures);
                                                        }
                                                    } else {
                                                        log.warn("transition année :  erreur lors de la récupération des groupes : id Etablissement : " + idStructureATraiter);
                                                        endTransition(nbStructureATraiter, finalHandler, idStructures);
                                                    }

                                                }
                                            }));
                                        }
                                    };
                                });
                            } else {
                                log.warn("transition année : établissement inactif : id Etablissement : " + idStructureATraiter);
                            }
                        }
                    } else {
                        log.warn("transition année : Aucun établissement actif ou à traiter");
                        log.info("FIN : transition année ");
                        finalHandler.handle(new Either.Left<String,JsonArray>("Transition d'année arrêtée : Aucun établissement actif"));
                    }

                } else {
                    log.error("transition année : Impossible de récupérer les établissements actifs");
                    log.info("FIN : transition année ");
                    finalHandler.handle(new Either.Left<String,JsonArray>(body.getString("message")));
                }
            }
        }));
    }

    private void executeTransitionForStructure(Map<String, List<String>> classeIdsEleves, List<String> pListIdsGroupesATraiter, Map<String,String> vMapGroupesATraiter, String idStructureATraiter, int nbStructureATraiter, Handler<Either<String, JsonArray>> finalHandler, List<String> idStructures) {
        // On récupère la liste des identifiants de devoir nécessaires à la crétaion de devoir

        String queryNextVal = createQueryNextValDevoirs(pListIdsGroupesATraiter);
        if(queryNextVal != null && !queryNextVal.isEmpty()){
            sql.raw(queryNextVal.toString(), SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> result) {
                    if (result.isRight()) {
                        // On ajoute l'id du cours aux cours à créer.
                        List<Long> listIdDevoirsToCreate = new ArrayList<>();
                        for (Object value : result.right().getValue().getMap().values()) {
                            listIdDevoirsToCreate.add((Long) value);
                        }
                        Map<String,Long> vMapGroupesIdsDevoirATraiter = new HashMap<String,Long>();
                        for (int i = 0; i < pListIdsGroupesATraiter.size(); i++) {
                            vMapGroupesIdsDevoirATraiter.put(pListIdsGroupesATraiter.get(i), listIdDevoirsToCreate.get(i));
                        }
                        transitionAnneeStructure(classeIdsEleves,pListIdsGroupesATraiter,vMapGroupesATraiter,vMapGroupesIdsDevoirATraiter, idStructureATraiter,new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle(Either<String, JsonArray> event) {
                                if (event.isRight()) {
                                    log.info("FIN : transition année id Etablissement : " + idStructureATraiter);
                                    endTransition(nbStructureATraiter, finalHandler, idStructures);
                                } else if (event.isLeft()){
                                    log.error("FIN : transition année id Etablissement ERREUR : " + idStructureATraiter + " Erreur  : " + event.left().getValue());
                                    endTransition(nbStructureATraiter, finalHandler, idStructures);
                                }
                            }});
                    }
                }
            }));
        } else {
            log.warn("transition année :  queryNextVal vide : id Etablissement : " + idStructureATraiter);
            endTransition(nbStructureATraiter, finalHandler, idStructures);
        }
    }

    /**
     * Détermine si le traitement est fini
     * @param nbStructureATraiter
     * @param finalHandler
     * @param listIdsEtablisement
     */
    private void endTransition(int nbStructureATraiter, Handler<Either<String, JsonArray>> finalHandler, List<String> listIdsEtablisement) {
        incrementCounter();
        if(getCounter() == nbStructureATraiter ) {
            JsonArray vJsonArrayEtabTraites = new JsonArray();
            for (int i = 0; i < listIdsEtablisement.size(); i++) {
                vJsonArrayEtabTraites.add(listIdsEtablisement.get(i));
            }

            if (listIdsEtablisement.size() > 0){
                log.info("FIN : transition année listIdsEtablisement : " + listIdsEtablisement.toString());
            }
            finalHandler.handle(new Either.Right<String,JsonArray>(vJsonArrayEtabTraites));
        }
    }

    /**
     * Retourne la requête qui récupère la liste des ids de devoirs à créer
     * @param pListIdsGroupesATraiter
     * @return
     */
    private String createQueryNextValDevoirs(List pListIdsGroupesATraiter) {
        int nbrDevoirsToCreate = pListIdsGroupesATraiter.size();
        StringBuilder queryNextVal = new StringBuilder();
        if (nbrDevoirsToCreate > 0) {
            queryNextVal.append("WITH ");
            for (int i = 0; i < nbrDevoirsToCreate; i++) {
                queryNextVal.append("r" + i + " AS ( SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".devoirs_id_seq') as id" + i + ") ");
                if (i != nbrDevoirsToCreate - 1) {
                    queryNextVal.append(",");
                }
            }
            queryNextVal.append("SELECT * FROM ");
            for (int i = 0; i < nbrDevoirsToCreate; i++) {
                queryNextVal.append("r" + i);
                if (i != nbrDevoirsToCreate - 1) {
                    queryNextVal.append(",");
                }
            }
        }
        return queryNextVal.toString();
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
    private void transitionAnneeStructure(Map<String,List<String>> classeIdsEleves,  List<String> vListIdsGroupesATraiter,Map<String,String> vMapGroupesATraiter, Map<String,Long> vMapGroupesIdsDevoirATraiter ,String idStructureATraiter, Handler<Either<String, JsonArray>> handler) {

        log.info("DEBUT : transactions pour la transition année id Etablissement : " + idStructureATraiter);
        if (vListIdsGroupesATraiter != null && vListIdsGroupesATraiter.size()>0) {
            log.info("INFO : transactions pour la transition année vListIdsGroupesATraiter  : " + vListIdsGroupesATraiter.toString());
        } else {
            log.warn("WARN : transactions pour la transition année vListIdsGroupesATraiter : Aucun groupe ");
        }
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();


        // Sauvegarde BDD : Si le schéma n'existe pas : sera fait à part
        //        String queryCloneNotes ="SELECT notes.clone_schema('notes','notes_2017_2018')";
        //        statements.add(new JsonObject().put("statement", queryCloneNotes).put("values", values).put("action", "prepared"));
        //        String queryCloneVieSCo ="SELECT notes.clone_schema('viesco','viesco_2017_2018')";
        //        statements.add(new JsonObject().put("statement", queryCloneVieSCo).put("values", values).put("action", "prepared"));

        JsonArray valuesForDeletion = new fr.wseduc.webutils.collections.JsonArray();
        for (String idGroupe:vListIdsGroupesATraiter) {
            valuesForDeletion.add(idGroupe);
        }
        valuesForDeletion.add(idStructureATraiter);

        // Suppresssion : appreciation_matiere_periode
        supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA, Competences.ID_CLASSE,
                vListIdsGroupesATraiter, statements, valuesForDeletion ,
                Competences.APPRECIATION_MATIERE_PERIODE_TABLE, Competences.ID_PERIODE);

        // Suppresssion : element_programme
        supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA, Competences.ID_CLASSE,
                vListIdsGroupesATraiter, statements, valuesForDeletion ,
                Competences.ELEMENT_PROGRAMME_TABLE,Competences.ID_PERIODE);

        // Suppresssion : moyenne_finale
                supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA,Competences.ID_CLASSE,
                vListIdsGroupesATraiter, statements, valuesForDeletion ,
                Competences.MOYENNE_FINALE_TABLE, Competences.ID_PERIODE);

        // Suppresssion : appreciation_classe
        supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA,Competences.ID_CLASSE,
                vListIdsGroupesATraiter, statements, valuesForDeletion ,
                Competences.APPRECIATION_CLASSE_TABLE,Competences.ID_PERIODE);

        // Suppresssion : Conservation des  compétences max par l'élève, suppresion des devoirs
        manageDevoirsAndCompetences(idStructureATraiter, vListIdsGroupesATraiter, vMapGroupesATraiter, vMapGroupesIdsDevoirATraiter, classeIdsEleves, statements);

        //suppression des elts thematiques créés par l'etab
        JsonArray valuesIdEtab = new fr.wseduc.webutils.collections.JsonArray();
        valuesIdEtab.add(idStructureATraiter);
        suppressionTransitionParamIdStructure(statements,valuesIdEtab,
                Competences.THEMATIQUE_BILAN_PERIODIQUE_TABLE, Competences.COMPETENCES_SCHEMA);
        // Suppresion des remplacants, notes.users, notes.members, notes.groups, rel_group_cycle, périodes


        //Suppresion des tables avec id_eleve
        for(Map.Entry<String,List<String>> idClassIdsEleve : classeIdsEleves.entrySet()){
            List<String> idsEleve = idClassIdsEleve.getValue();
            if(!idsEleve.isEmpty()){
                JsonArray valuesForSuppressionIdEleve = new fr.wseduc.webutils.collections.JsonArray();
                for(String idEleve: idsEleve){
                    valuesForSuppressionIdEleve.add(idEleve);
                }
                valuesForSuppressionIdEleve.add(idStructureATraiter);

                supressionTransitionCheckPeriode(Competences.VSCO_SCHEMA, Competences.ID_ELEVE,
                        idsEleve,statements,valuesForSuppressionIdEleve,
                        Competences.VSCO_ABSENCES_ET_RETARDS, Competences.ID_PERIODE);

                supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA, Competences.ID_ELEVE,
                        idsEleve,statements, valuesForSuppressionIdEleve,
                        Competences.APPRECIATION_CPE_BILAN_PERIODIQUE, Competences.ID_PERIODE);

                supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA, Competences.ID_ELEVE,
                        idsEleve,statements,valuesForSuppressionIdEleve,
                        Competences.APPRECIATION_ELT_BILAN_PERIODIQUE_ELEVE_TABLE, Competences.ID_PERIODE);

                supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA,Competences.ID_ELEVE,
                        idsEleve,statements,valuesForSuppressionIdEleve,
                        Competences.AVIS_CONSEIL_DE_CLASSE_TABLE, Competences.ID_PERIODE);

                supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA, Competences.ID_ELEVE,
                        idsEleve,statements,valuesForSuppressionIdEleve,
                        Competences.AVIS_CONSEIL_ORIENTATION_TABLE, Competences.ID_PERIODE);

                supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA, Competences.ID_ELEVE,
                        idsEleve,statements,valuesForSuppressionIdEleve,
                        Competences.COMPETENCE_NIVEAU_FINAL, Competences.ID_PERIODE);

                supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA, Competences.ID_ELEVE,
                        idsEleve, statements, valuesForSuppressionIdEleve,
                        Competences.POSITIONNEMENT, Competences.ID_PERIODE);

                supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA, Competences.ID_ELEVE,
                        idsEleve,statements,valuesForSuppressionIdEleve,
                        Competences.SYNTHESE_BILAN_PERIODIQUE_TABLE,"id_typeperiode");

                //Suppression rel_groupe_appreciation_elt_eleve
                supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA, Competences.ID_ELEVE ,
                        idsEleve, statements, valuesForSuppressionIdEleve ,
                        Competences.ELEVES_IGNORES_LSU_TABLE, Competences.ID_PERIODE);
                //Suppression élèves ignorés LSU
                supressionTransitionCheckPeriode(Competences.COMPETENCES_SCHEMA, Competences.ID_ELEVE,
                        idsEleve,statements,valuesForSuppressionIdEleve,
                        Competences.REL_GROUPE_APPRECIATION_ELT_ELEVE_TABLE, Competences.ID_PERIODE);

            }
        }



        deleteUsersGroups(idStructureATraiter, statements);

        // Transition pour l'établissement effectué
        JsonArray valuesTransition = new fr.wseduc.webutils.collections.JsonArray();
        valuesTransition.add(idStructureATraiter);
        String queryInsertTransition ="INSERT INTO notes.transition(id_etablissement) VALUES (?)";
        statements.add(new JsonObject().put("statement", queryInsertTransition).put("values", valuesTransition).put("action", "prepared"));

        Sql.getInstance().transaction(statements,new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L), SqlResult.validResultHandler(handler));

        log.info("FIN : transactions pour la transition année id Etablissement : " + idStructureATraiter);
    }

    /**
     * Suppressions : remplacants, members, groups et relations groupes d'enseignement - cycle, périodes
     * @param idStructureATraiter
     * @param statements
     */
    private void deleteUsersGroups(String idStructureATraiter, JsonArray statements) {
        JsonArray values;

        // Suppresion des remplacants
        values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(idStructureATraiter);
        suppressionTransitionParamIdStructure(statements,values, "rel_professeurs_remplacants",
                Competences.COMPETENCES_SCHEMA);

        // Suppresion des members, groups et relations groupes d'enseignement - cycle
        values = new fr.wseduc.webutils.collections.JsonArray();
        String queryMembers = "DELETE FROM notes.members";
        statements.add(new JsonObject().put("statement", queryMembers).put("values", values).put("action", "prepared"));

        String queryGroups = "DELETE FROM notes.groups";
        statements.add(new JsonObject().put("statement", queryGroups).put("values", values).put("action", "prepared"));

        String queryRelGroupeType= "DELETE FROM notes.rel_groupe_cycle WHERE type_groupe > 0";
        statements.add(new JsonObject().put("statement", queryRelGroupeType).put("values", values).put("action", "prepared"));

        // Suppresion des members, groups et relations groupes d'enseignement - cycle
        values = new fr.wseduc.webutils.collections.JsonArray();
        String queryUsers = "" +
                "DELETE FROM notes.users " +
                "WHERE" +
                " NOT EXISTS ( " +
                "    SELECT 1 " +
                "    FROM notes.devoirs " +
                "    WHERE " +
                "     devoirs.owner = users.id " +
                " )";
        statements.add(new JsonObject().put("statement", queryUsers).put("values", values).put("action", "prepared"));

        // Suppresion des remplacants
        values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(idStructureATraiter);
        suppressionTransitionParamIdStructure(statements, values,"periode", Competences.VSCO_SCHEMA);
    }

    /**
     * Conservation des  compétences max par l'élève, suppresion des devoirs, dispenses domaines
     * @param idStructureATraiter
     * @param vListIdsGroupesATraiter
     * @param vMapGroupesATraiter
     * @param vMapGroupesIdsDevoirATraiter
     * @param classeIdsEleves
     * @param statements
     */
    private void manageDevoirsAndCompetences( String idStructureATraiter, List<String> vListIdsGroupesATraiter, Map<String, String> vMapGroupesATraiter, Map<String, Long> vMapGroupesIdsDevoirATraiter, Map<String, List<String>> classeIdsEleves, JsonArray statements) {
        JsonArray values;// Ajout de l'utilisateur pour la transition année
        String username = I18n.getInstance().translate(key_username_user_transition_annee.toString(),I18n.DEFAULT_DOMAIN, Locale.FRANCE);
        String classname = I18n.getInstance().translate(key_libelle_classe_transition_annee.toString(),I18n.DEFAULT_DOMAIN, Locale.FRANCE);
        values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(_id_user_transition_annee).add(username).add(username);
        String query = "INSERT INTO notes.users(id, username) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET username = ?";
        statements.add(new JsonObject().put("statement", query).put("values", values).put("action", "prepared"));

//        values = new fr.wseduc.webutils.collections.JsonArray();
//        values.add(idStructureATraiter).add(_id_periode_transition_annee);
//        query = "INSERT INTO viesco.periode(id_etablissement, timestamp_dt, timestamp_fn, date_fin_saisie, id_classe,id_type) " +
//                " VALUES (?, now(), now(), now(), ?, 1)";
//        statements.add(new JsonObject().put("statement", query).put("values", values).put("action", "prepared"));

        for (Map.Entry<String, String> entry : vMapGroupesATraiter.entrySet()){
            String idClasse = entry.getKey();
            // Création des évaluations libre par classe de l'établissement
            values = new fr.wseduc.webutils.collections.JsonArray();
            values.add(true).add(idStructureATraiter).add(idStructureATraiter).add(idClasse);
            String queryInsertDevoir = "INSERT INTO " +
                    "  notes.devoirs(id,owner, name, id_type, id_etablissement, diviseur, ramener_sur, date_publication," +
                    " is_evaluated, id_etat, percent, apprec_visible, eval_lib_historise,id_periode, date) " +
                    "  (" +
                    "   SELECT " + vMapGroupesIdsDevoirATraiter.get(idClasse) + ",'" + _id_user_transition_annee + "','" +
                    classname + entry.getValue() + "', type.id,periode.id_etablissement, 20, false, current_date, false, 1, 0, false, true , MAX(periode.id_type),MAX(periode.timestamp_fn) " +
                    "   FROM notes.type , viesco.periode " +
                    "    WHERE " +
                    "     type.default_type = ? " +
                    "     AND type.id_etablissement = ? " +
                    "     AND periode.id_etablissement = ? " +
                    "     AND periode.id_classe = ? " +
                    "    GROUP BY periode.id_etablissement,type.id" +
                    "  )";

            statements.add(new JsonObject()
                    .put("statement", queryInsertDevoir)
                    .put("values", values)
                    .put("action", "prepared"));


            List<String> vListEleves = classeIdsEleves.get(idClasse);
            if(null != vListEleves && vListEleves.size() > 0) {
                JsonArray valuesMaxCompetence = new fr.wseduc.webutils.collections.JsonArray();
                for (String idEleve : vListEleves) {
                    valuesMaxCompetence.add(idEleve);
                }
                for (String idEleve : vListEleves) {
                    valuesMaxCompetence.add(idEleve);
                }

                String queryMaxCompNoteByPeriode = "(SELECT competences_notes.id_competence, " +
                        "MAX(competences_notes.evaluation) AS max_comp, competences_notes.id_eleve, devoirs.id_periode "+
                        "FROM notes.competences_notes " +
                        "INNER JOIN notes.devoirs ON devoirs.id = competences_notes.id_devoir " +
                        "LEFT JOIN notes.competence_niveau_final " +
                        "ON devoirs.id_periode = competence_niveau_final.id_periode " +
                        "AND competences_notes.id_competence = competence_niveau_final.id_competence " +
                        "AND competences_notes.id_eleve = competence_niveau_final.id_eleve " +
                        "WHERE competences_notes.owner != '" + _id_user_transition_annee +
                        "' AND competences_notes.id_eleve IN " + Sql.listPrepared(vListEleves.toArray()) +
                        " AND competence_niveau_final.id_eleve IS NULL " +
                        "GROUP BY competences_notes.id_competence, competences_notes.id_eleve, devoirs.id_periode)";

                String queryMaxNiveauFinalByPeriode = "(SELECT competence_niveau_final.id_competence,  " +
                        "MAX(competence_niveau_final.niveau_final) AS max_comp ,competence_niveau_final.id_eleve, " +
                        "competence_niveau_final.id_periode FROM notes.competences_notes " +
                        "INNER JOIN notes.devoirs ON devoirs.id = competences_notes.id_devoir " +
                        "INNER JOIN notes.competence_niveau_final " +
                        "ON devoirs.id_periode = competence_niveau_final.id_periode " +
                        "AND competences_notes.id_competence = competence_niveau_final.id_competence "+
                        "AND competences_notes.id_eleve = competence_niveau_final.id_eleve " +
                        "WHERE competences_notes.owner != '" + _id_user_transition_annee +
                        "' AND competences_notes.id_eleve IN " + Sql.listPrepared(vListEleves.toArray()) +
                        " GROUP BY competence_niveau_final.id_competence, competence_niveau_final.id_eleve, competence_niveau_final.id_periode)";

                // Ajout du max des compétences ou du niveau final pour chaque élève
                String queryInsertMaxCompetenceNoteG = "" +
                        "INSERT INTO notes.competences_notes(id, id_devoir, id_competence, evaluation, owner, id_eleve) " +
                        "(" +
                       "SELECT nextval('notes.competences_notes_id_seq'), " + vMapGroupesIdsDevoirATraiter.get(idClasse) +
                        ", id_competence, MAX(max_comp), '" + _id_user_transition_annee + "',id_eleve " +
                        "FROM (" + queryMaxCompNoteByPeriode +
                        " UNION ALL" + queryMaxNiveauFinalByPeriode +
                        ") AS tmax GROUP BY id_competence, id_eleve )";

                statements.add(new JsonObject()
                        .put("statement", queryInsertMaxCompetenceNoteG)
                        .put("values", valuesMaxCompetence)
                        .put("action", "prepared"));

                // Suppression Dispenses domaine
                String querySuppressionDispenseDomaine = "" +

                        "  DELETE " +
                        "  FROM notes.dispense_domaine_eleve" +
                        "  WHERE " +
                        "   id_eleve IN " + Sql.listPrepared(vListEleves.toArray());
               JsonArray valuesDeleteDispenseEleve = new fr.wseduc.webutils.collections.JsonArray();
                for (String idEleve : vListEleves) {
                    valuesDeleteDispenseEleve.add(idEleve);
                }
                statements.add(new JsonObject()
                        .put("statement", querySuppressionDispenseDomaine)
                        .put("values", valuesDeleteDispenseEleve)
                        .put("action", "prepared"));
            }
        }

        // Création des compétences par devoir (historisé)
        values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(idStructureATraiter);
        String queryInsertCompetenceDevoir = "" +
                "INSERT INTO notes.competences_devoirs (id, id_devoir, id_competence, index) " +
                "( " +
                "    SELECT  nextval('notes.competences_devoirs_id_seq'),competences_notes.id_devoir,competences_notes.id_competence,0" +
                "    FROM notes.competences_notes " +
                "           INNER JOIN notes.devoirs ON competences_notes.id_devoir = devoirs.id" +
                "    WHERE " +
                "           devoirs.eval_lib_historise = true" +
                "           AND id_etablissement = ? " +
                "    GROUP BY id_devoir, id_competence  " +
                ")";

        statements.add(new JsonObject()
                .put("statement", queryInsertCompetenceDevoir)
                .put("values", values)
                .put("action", "prepared"));

        // Suppression devoir non historisé
        String queryDeleteDevoirNonHistorise = "" +
                "DELETE FROM notes.devoirs  " +
                "WHERE " +
                " eval_lib_historise = false " +
                " AND id_etablissement = ? ";

        statements.add(new JsonObject()
                .put("statement", queryDeleteDevoirNonHistorise)
                .put("values", values)
                .put("action", "prepared"));
    }

    /**
     * Suppression pour la transition année : appreciation_matiere_periode, element_programme, moyenne_finale, positionnement, appreciation_classe
     * Pour les groupes an paramètres (établissement en cours de traitement)
     * * @param vListIdsGroupesATraiter
     * @param statements
     * @param values
     * @param table
     */
   /* private void supressionTransition(List<String> vListIdsGroupesATraiter, JsonArray statements, JsonArray values,String table) {
        String query =
                " DELETE  " +
                        " FROM notes." + table +
                        " WHERE  " +
                        "  id_classe IN " + Sql.listPrepared(vListIdsGroupesATraiter.toArray()) +
                        "  AND EXISTS " +
                        "   ( " +
                        "     SELECT 1 " +
                        "     FROM viesco.periode " +
                        "     WHERE  " +
                        "      periode.id_type = " + table +".id_periode " +
                        "      AND periode.id_etablissement = ? " +
                        "   )";

        statements.add(new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared"));
    }*/
    private void supressionTransitionCheckPeriode(String schema, String idGroupOrIdClassOrIdEleve, List<String> idGroupOrIdELeve, JsonArray statements,
                                                  JsonArray values, String table, String idPeriode) {
        String query =
                " DELETE  " +
                        " FROM "+ schema +"." + table +
                        " WHERE  " +
                        idGroupOrIdClassOrIdEleve + " IN " + Sql.listPrepared(idGroupOrIdELeve.toArray()) +
                        "  AND EXISTS " +
                        "   ( " +
                        "     SELECT 1 " +
                        "     FROM viesco.periode " +
                        "     WHERE  " +
                        "      periode.id_type = " + table +"." + idPeriode +
                        "      AND periode.id_etablissement = ? " +
                        "   )";

        statements.add(new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared"));
    }

    private void suppressionTransitionParamIdStructure( JsonArray statements, JsonArray values, String table, String schema){
        String query = "DELETE FROM " + schema + "." + table +
                " WHERE id_etablissement = ?";
        statements.add(new JsonObject()
                .put("statement", query).put("values",values)
                .put("action", "prepared"));

    }
}
