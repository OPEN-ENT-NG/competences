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
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.constants.Field;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.message.MessageResponseHandler;
import fr.openent.competences.service.UtilsService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.math.RoundingMode;
import java.text.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.isNotNull;
import static fr.openent.competences.Utils.isNull;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.TIME;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.openent.competences.helpers.NodePdfGeneratorClientHelper.CONNECTION_WAS_CLOSED;
import static org.entcore.common.sql.SqlResult.validResultHandler;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;


/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultUtilsService implements UtilsService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultUtilsService.class);
    protected EventBus eb;
    private final Neo4j neo4j = Neo4j.getInstance();

    public DefaultUtilsService(EventBus eb) {
        this.eb = eb;
    }

    public DefaultUtilsService() {
    }

    @Override
    /**
     * Récupère la liste des professeurs remplaçants du titulaire
     * (si lien titulaire/remplaçant toujours actif à l'instant T)
     *
     * @param psIdTitulaire identifiant neo4j du titulaire
     * @param psIdEtablissement identifiant de l'établissement
     * @param handler handler portant le resultat de la requête : la liste des identifiants neo4j des rempacants
     */
    public void getRemplacants(String psIdTitulaire, String psIdEtablissement, Handler<Either<String, JsonArray>> handler) {

        //TODO Methode à tester (pas utilisée pour le moment)

        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT DISTINCT id_remplacant ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_professeurs_remplacants ")
                .append("WHERE id_titulaire = ? ")
                .append("AND id_etablissement = ? ")
                .append("AND date_debut <= current_date ")
                .append("AND current_date <= date_fin ");

        values.add(psIdTitulaire);
        values.add(psIdEtablissement);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    /**
     * Récupère la liste des professeurs titulaires d'un remplaçant sur un établissement donné
     * (si lien titulaire/remplaçant toujours actif à l'instant T)
     *
     * @param psIdRemplacant identifiant neo4j du remplaçant
     * @param psIdEtablissement identifiant de l'établissement
     * @param handler handler portant le resultat de la requête : la liste des identifiants neo4j des titulaires
     */
    public void getTitulaires(String psIdRemplacant, String psIdEtablissement, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT DISTINCT main_teacher_id ")
                .append("FROM " + Competences.VSCO_SCHEMA + ".multi_teaching ")
                .append("WHERE second_teacher_id = ? ")
                .append("AND structure_id = ? ")
                .append("AND start_date <= current_date ")
                .append("AND current_date <= end_date ");

        values.add(psIdRemplacant);
        values.add(psIdEtablissement);
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    public void getMultiTeachersByClass(final String idEtablissement, final String idClasse, final Integer idPeriode,
                                        Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "multiTeaching.getMultiTeachersByClass")
                .put("structureId", idEtablissement)
                .put("groupId", idClasse)
                .put("periodId", idPeriode != null ? idPeriode.toString() : null);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                JsonArray result = body.getJsonArray(RESULTS);
                handler.handle(new Either.Right<>(result));
            } else {
                handler.handle(new Either.Left<>(body.getString("message")));
                log.error("[Competences] DefaultUtilsService at getMultiTeachersByClass : " + body.getString("message"));
            }
        }));
    }

    @Override
    public void getMultiTeachers(final String structureId, final JsonArray groupIds, final Integer PeriodeId,
                                 Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "multiTeaching.getMultiteachers")
                .put("structureId", structureId)
                .put("groupIds", groupIds)
                .put("periodId", PeriodeId != null ? PeriodeId.toString() : null);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                JsonArray result = body.getJsonArray(RESULTS);
                handler.handle(new Either.Right<>(result));
            } else {
                handler.handle(new Either.Left<>(body.getString("message")));
                log.error("[Competences] DefaultUtilsService at getMultiteachers : " + body.getString("message"));
            }
        }));
    }

    /**
     * get only evaluable sql services
     * @param structureId
     * @param idsClass groups or/and classes ids
     * @param handler request response
     */

    public void getServices(final String structureId, final JsonArray idsClass,
                            Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "service.getServices")
                .put("idStructure", structureId)
                .put("aIdGroupe", idsClass)
                .put("evaluable", true);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                JsonArray results = body.getJsonArray(RESULTS);
                handler.handle(new Either.Right<String, JsonArray>(results));
            } else {
                handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                log.error("getServices : " + body.getString("message"));
            }
        }));
    }

    @Override
    public void getDefaultServices (final String structureId, final JsonArray groupIds, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "service.getDefaultServices")
                .put("idEtablissement", structureId)
                .put("idsGroupe", groupIds);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                JsonArray results = body.getJsonArray(RESULTS);
                handler.handle(new Either.Right<String, JsonArray>(results));
            } else {
                handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                log.error("getDefaultServices : " + body.getString("message"));
            }
        }));
    }

    /**
     * get All Services evaluable or no evaluable, neo and sql with multiTeachers on each service
     * @param structureId strusure id
     * @param groupIds all groups or/and classes ids
     * @param filters JsonObject with boolean parameters :
     *                evaluable,noEvaluable,classes,groups,manualGroups are true by default,
     *                compressed is false by default
     * @param handler return services with fields of serviceModel (class java viesco) each service contains multiTeachers
     */
    @Override
    public void getDefaultServices (final String structureId, final JsonArray groupIds, final JsonObject filters,
                                    Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "service.getDefaultServices")
                .put("idEtablissement", structureId)
                .put("idsGroupe", groupIds)
                .put("filters",filters);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                JsonArray results = body.getJsonArray(RESULTS);
                handler.handle(new Either.Right<String, JsonArray>(results));
            } else {
                handler.handle(new Either.Left<String, JsonArray>(body.getString("message")));
                log.error("[Competences] getDefaultServices : " + body.getString("message"));
            }
        }));
    }

    @Override
    public void hasService(String idEtablissement, JsonArray idClasses, String idMatiere, Long idPeriode,
                           UserInfos user, Handler<Boolean> handler) {
        Future<JsonArray> servicesFuture = Future.future();
        getServices(idEtablissement, idClasses, event -> formate(servicesFuture, event));

        Future<JsonArray> multiTeachersFuture = Future.future();
        getMultiTeachers(idEtablissement, idClasses, idPeriode != null ? idPeriode.intValue() : null,
                event -> formate(multiTeachersFuture, event));

        CompositeFuture.all(servicesFuture, multiTeachersFuture).setHandler(event -> {
            if(event.failed()) {
                log.error("[Competences] hasService : " + event.cause().getMessage());
                handler.handle(false);
            } else {
                JsonArray services = servicesFuture.result();
                JsonArray multiTeachers = multiTeachersFuture.result();

                List<Object> userServices = services.stream()
                        .filter(el -> !user.getUserId().equals(((JsonObject) el).getString("id_enseignant")) ||
                                idMatiere == null || idMatiere.equals(((JsonObject) el).getString("id_matiere")))
                        .collect(Collectors.toList());
                List<Object> userMultiTeaching = multiTeachers.stream()
                        .filter(el -> !user.getUserId().equals(((JsonObject) el).getString("second_teacher_id")) ||
                                idMatiere == null || idMatiere.equals(((JsonObject) el).getString("subject_id")))
                        .collect(Collectors.toList());

                handler.handle(!userServices.isEmpty() || !userMultiTeaching.isEmpty());
            }
        });
    }

    @Override
    public void listTypesDevoirsParEtablissement(String idEtablissement, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT type.* ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".type ")
                .append("WHERE type.id_etablissement = ? ");
        values.add(idEtablissement);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void getInfoEleve(String id, Handler<Either<String, JsonObject>> result) {
        StringBuilder query = new StringBuilder();

        query.append("MATCH (u:`User` {id: {id}}) ")
                .append("OPTIONAL MATCH ")
                .append("(n:`UserBook` {userid : {id}}) ")
                .append("OPTIONAL MATCH (c:`Class`) WHERE c.externalId in u.classes ")
                .append("RETURN u, n, c");
        neo4j.execute(query.toString(), new JsonObject().put("id", id), Neo4jResult.validUniqueResultHandler(result));
    }

    @Override
    public void getInfoEleveBackup(String id, Handler<Either<String, JsonObject>> result) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT DISTINCT \"idEleve\" AS id, \"idEleve\", \"idGroupes\", ")
                .append(" display_name AS \"displayName\", ")
                .append(" delete_date AS \"deleteDate\", ")
                .append(" first_name AS \"firstName\", ")
                .append(" last_name AS \"lastName\", ")
                .append(" id_structure AS \"idEtablissement\", ")
                .append(" id_groupe AS \"idClasse\", ")
                .append(" birth_date AS \"birthDate\" ")
                .append(" FROM ")
                .append(" ( SELECT * FROM ")

                // Selection
                .append(" (SELECT personnes_supp.id_user AS \"idEleve\", MAX(delete_date) AS \"deleteDate\", ")
                .append(" string_agg(DISTINCT rel_groupes_personne_supp.id_groupe, ',') AS \"idGroupes\" ")
                .append(" FROM " + Competences.VSCO_SCHEMA + ".personnes_supp, viesco.rel_groupes_personne_supp ")
                .append(" WHERE personnes_supp.id = rel_groupes_personne_supp.id ")
                .append(" AND id_user = ? ")
                .append(" AND user_type = 'Student' ")
                .append(" GROUP BY personnes_supp.id_user) AS res ")


                .append(" INNER JOIN " + Competences.VSCO_SCHEMA + ".personnes_supp ")
                .append(" ON \"deleteDate\" = personnes_supp.delete_date ")
                .append(" AND \"idEleve\" = personnes_supp.id_user)  AS res1 ")

                .append(" LEFT JOIN viesco.rel_groupes_personne_supp ON res1.id = rel_groupes_personne_supp.id ")
                .append(" AND type_groupe = 0 ")

                .append(" INNER JOIN viesco.rel_structures_personne_supp ON res1.id = rel_structures_personne_supp.id");

        params.add(id);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(result));
    }

    @Override
    public void getEnfants(String id, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        //query.append("MATCH (m:`User` {id: {id}})-[:COMMUNIQUE_DIRECT]->(n:`User`) RETURN n");

        query.append("MATCH (m:`User`{id: {id}})<-[:RELATED]-(n:`User`)-[:ADMINISTRATIVE_ATTACHMENT]->(s:`Structure`) ")
                .append("WITH n.id as id, n.displayName as displayName, n.classes as externalIdClasse, s.id as idStructure,  ")
                .append("n.firstName as firstName, n.lastName as lastName MATCH(c:Class)WHERE c.externalId IN externalIdClasse")
                .append(" RETURN id, displayName, firstName, lastName, c.id as idClasse, idStructure");
        neo4j.execute(query.toString(), new JsonObject().put("id", id), Neo4jResult.validResultHandler(handler));
    }

    /**
     * Fonction de calcul générique de la moyenne
     *
     * @param listeNoteDevoirs : contient une liste de NoteDevoir.
     *                         La formule suivante est utilisée :(SUM ( ni *m *ci /di)  + SUM ( nj *cj)  ) / (S ( ci)  + SUM ( cj  *dj /m)  )
     * @param diviseurM        : diviseur de la moyenne. Par défaut, cette valeur est égale à 20 (optionnel).
     * @param annual        : permet de savoir si on doit faire l'arrondi à la fin.
     **/
    @Override
    public JsonObject calculMoyenne(List<NoteDevoir> listeNoteDevoirs, Boolean statistiques, Integer diviseurM, Boolean annual) {
        if (diviseurM == null) {
            diviseurM = 20;
        }
        double noteMax = 0;
        double noteMin = diviseurM;

        // (SUM ( ni *m *ci /di)  + SUM ( nj *cj)  ) / (S ( ci)  + SUM ( cj  *dj /m)  )
        // avec d : diviseurs, n : note, c : coefficient, m = 20 : si ramené sur
        // avec i les notes ramenées sur m, et j les notes non ramenées sur m

        double sumCI = 0;
        double sumCJDJParM = 0;
        double sumCJDJ = 0;
        double sumNIMCIParD = 0;
        double sumCoefficient = 0;

        for (NoteDevoir noteDevoir : listeNoteDevoirs) {
            Double currNote = noteDevoir.getNote();
            Double currCoefficient = noteDevoir.getCoefficient();
            Double currDiviseur = noteDevoir.getDiviseur();

            if (!noteDevoir.getRamenerSur()) {
                sumCJDJParM += (currCoefficient * currDiviseur / diviseurM);
                sumCJDJ += (currNote * currCoefficient);
            } else if (currCoefficient == 0) {
                continue;
            } else {
                sumNIMCIParD += ((currNote * diviseurM * currCoefficient) / currDiviseur);
                sumCI += currCoefficient;
            }

            sumCoefficient += currCoefficient;

            // Calcul de la note min et max
            if (statistiques) {
                if (currNote > noteMax) {
                    noteMax = currNote;
                }
                if (currNote < noteMin) {
                    noteMin = currNote;
                }
            }
        }

        Double moyenne = ((sumNIMCIParD + sumCJDJ) / (sumCI + sumCJDJParM));

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("fr", "FR"));
        symbols.setDecimalSeparator('.');

        DecimalFormat df = new DecimalFormat("#.0", symbols);
        df.setRoundingMode(RoundingMode.HALF_UP);//with this mode 2.125 -> 2.13 without 2.125 -> 2.12

        try {
            if (moyenne.isNaN()) {
                moyenne = null;
            } else {
                if(!annual)
                    moyenne = Double.valueOf(df.format(moyenne));
            }

        } catch (NumberFormatException e) {
            log.error("Moyenne : " + moyenne, e);
        }
        if (null == moyenne) moyenne = 0.0;

        JsonObject r = new JsonObject().put("moyenne", moyenne)
                .put("hasNote", listeNoteDevoirs.size() > 0);

        if(sumCoefficient == 0)
            r.put("moyenne","NN");

        if( statistiques){
            r.put("noteMax", noteMax).put("noteMin", noteMin);
        }

        return r;
    }

    /**
     * Fonction de calcul générique de la moyenne
     * La formule suivante est utilisée : SUM(notes)/ nombre/Notes
     *
     * @param listeNoteDevoirs : contient une liste de NoteDevoir.
     **/
    @Override
    public JsonObject calculMoyenneParDiviseur(List<NoteDevoir> listeNoteDevoirs, Boolean statistiques) {
        Double noteMax = (double) 0;
        Double noteMin = null;
        Double notes = (double) 0;
        //Double diviseur = new Double(0);

        for (NoteDevoir noteDevoir : listeNoteDevoirs) {
            Double currNote = noteDevoir.getNote();
            notes += currNote;
            // Calcul de la note min et max
            if (statistiques) {
                if (null == noteMin) {
                    noteMin = noteDevoir.getDiviseur();
                }
                if (currNote > noteMax) {
                    noteMax = currNote;
                }
                if (currNote < noteMin) {
                    noteMin = currNote;
                }
            }
        }

        Double moyenne = ((notes) / (listeNoteDevoirs.size()));

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("fr", "FR"));
        symbols.setDecimalSeparator('.');

        DecimalFormat df = new DecimalFormat("#.0", symbols);
        df.setRoundingMode(RoundingMode.HALF_UP);//with this mode 2.125 -> 2.13 without 2.125 -> 2.12
        try {
            moyenne = Double.valueOf(df.format(moyenne));
        } catch (NumberFormatException e) {
            log.error("Moyenne : " + moyenne, e);
        }
        JsonObject r = new JsonObject().put("moyenne", moyenne);
        if (statistiques) {
            r.put("noteMax", noteMax).put("noteMin", noteMin);
        }
        return r;
    }

    /**
     * Recupere un établissemnt sous sa representation en BDD
     *
     * @param id      identifiant de l'etablissement
     * @param handler handler comportant le resultat
     */
    @Override
    public void getStructure(String id, Handler<Either<String, JsonObject>> handler) {
        String query = "match (s:`Structure`) where s.id = {id} return s";
        neo4j.execute(query, new JsonObject().put("id", id), Neo4jResult.validUniqueResultHandler(handler));
    }


    @Override
    public void list(String structureId, String classId, String groupId,
                     JsonArray expectedProfiles, String filterActivated, String nameFilter,
                     UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
        JsonObject params = new JsonObject();
        String filter = "";
        String filterProfile = "WHERE 1=1 ";
        String optionalMatch =
                "OPTIONAL MATCH u-[:IN]->(:ProfileGroup)-[:DEPENDS]->(class:Class)-[:BELONGS]->(s) " +
                        "OPTIONAL MATCH u-[:RELATED]->(parent: User) " +
                        "OPTIONAL MATCH (child: User)-[:RELATED]->u " +
                        "OPTIONAL MATCH u-[rf:HAS_FUNCTION]->fg-[:CONTAINS_FUNCTION*0..1]->(f:Function) ";
        if (expectedProfiles != null && expectedProfiles.size() > 0) {
            filterProfile += "AND p.name IN {expectedProfiles} ";
            params.put("expectedProfiles", expectedProfiles);
        }
        if (classId != null && !classId.trim().isEmpty()) {
            filter = "(n:Class {id : {classId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-";
            params.put("classId", classId);
        } else if (structureId != null && !structureId.trim().isEmpty()) {
            filter = "(n:Structure {id : {structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-";
            params.put("structureId", structureId);
        } else if (groupId != null && !groupId.trim().isEmpty()) {
            filter = "(n:Group {id : {groupId}})<-[:IN]-";
            params.put("groupId", groupId);
        }
        String condition = "";
        String functionMatch = "WITH u MATCH (s:Structure)<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), u-[:IN]->pg ";

        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            condition += "AND u.displayName =~ {regex}  ";
            params.put("regex", "(?i)^.*?" + Pattern.quote(nameFilter.trim()) + ".*?$");
        }
        if (filterActivated != null) {
            if ("inactive".equals(filterActivated)) {
                condition += "AND NOT(u.activationCode IS NULL)  ";
            } else if ("active".equals(filterActivated)) {
                condition += "AND u.activationCode IS NULL ";
            }
        }

        String query =
                "MATCH " + filter + "(u:User) " +
                        functionMatch + filterProfile + condition + optionalMatch +
                        "RETURN DISTINCT u.id as id, p.name as type, u.externalId as externalId, " +
                        "u.activationCode as code, u.login as login, u.firstName as firstName, " +
                        "u.lastName as lastName, u.displayName as displayName, u.source as source, u.attachmentId as attachmentId, " +
                        "u.birthDate as birthDate, " +
                        "u.title as civility, " +
                        "extract(function IN u.functions | last(split(function, \"$\"))) as aafFunctions, " +
                        "collect(distinct {id: s.id, name: s.name}) as structures, " +
                        "collect(distinct {id: class.id, name: class.name}) as allClasses, " +
                        "collect(distinct [f.externalId, rf.scope]) as functions, " +
                        "CASE WHEN parent IS NULL THEN [] ELSE collect(distinct {id: parent.id, firstName: parent.firstName, lastName: parent.lastName}) END as parents, " +
                        "CASE WHEN child IS NULL THEN [] ELSE collect(distinct {id: child.id, firstName: child.firstName, lastName: child.lastName, attachmentId : child.attachmentId }) END as children, " +
                        "HEAD(COLLECT(distinct parent.externalId)) as parent1ExternalId, " + // Hack for GEPI export
                        "HEAD(TAIL(COLLECT(distinct parent.externalId))) as parent2ExternalId " + // Hack for GEPI export
                        "ORDER BY type DESC, displayName ASC ";
        neo4j.execute(query, params, Neo4jResult.validResultHandler(results));
    }

    @Override
    public JsonArray saUnion(JsonArray recipient, JsonArray list) {
        for (int i = 0; i < list.size(); i++) {

            if (list.getValue(i) instanceof JsonObject) {
                recipient.add(list.getJsonObject(i));
            } else {
                recipient.add(list.getValue(i));
            }
        }

        return recipient;
    }

    @Override
    public <K, V> void addToMap(K id, HashMap<K, ArrayList<V>> map, V valueToAdd) {
        if (!map.containsKey(id) ) {
            map.put(id, new ArrayList<>());
        }
        if(isNull(map.get(id))){
            map.put(id, new ArrayList<>());
        }
        map.get(id).add(valueToAdd);
    }


    public  void addToMap(String id, Long sousMatiereId,
                          HashMap<String, HashMap<Long, ArrayList<NoteDevoir>>> map,
                          NoteDevoir valueToAdd) {
        if (!map.containsKey(id)) {
            map.put(id, new HashMap<>());
        }
        if(!map.get(id).containsKey(sousMatiereId)) {
            map.get(id).put(sousMatiereId, new ArrayList<>());
        }

        map.get(id).get(sousMatiereId).add(valueToAdd);
    }

    public <K,V> void addToMapWithJsonArray(K id, HashMap<K, JsonArray> map, V valueToAdd){
        if (map.containsKey(id)) {

            map.get(id).add(valueToAdd);

        } else {

            JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
            values.add(valueToAdd);
            map.put(id, values);
        }
    }
    /**
     * Récupère les cycles des classes dans la relation classe_cycle
     *
     * @param idClasse liste des identifiants des classes.
     * @param handler  Handler portant le résultat de la requête.
     */
    @Override
    public void getCycle(List<String> idClasse, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id_groupe, id_cycle, libelle, value_cycle ")
                .append("FROM ").append(Competences.COMPETENCES_SCHEMA).append(".rel_groupe_cycle, ")
                .append(Competences.COMPETENCES_SCHEMA).append(".cycle ")
                .append("WHERE id_groupe IN ").append(Sql.listPrepared(idClasse.toArray()))
                .append(" AND id_cycle = cycle.id");

        for (String id : idClasse) {
            params.add(id);
        }
        Sql.getInstance().prepared(query.toString(), params, DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

    /**
     * Récupère le cycle de la classe dans la relation classe_cycle
     *
     * @param idClasse Identifiant de la classe.
     * @param handler  Handler portant le résultat de la requête.
     */
    @Override
    public void getCycle(String idClasse, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id_cycle ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle,  ")
                .append(Competences.COMPETENCES_SCHEMA + ".cycle ")
                .append("WHERE id_groupe = ? ")
                .append(" AND id_cycle = cycle.id");

        params.add(idClasse);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }


    private Handler<Message<JsonObject>> nameHandler(String[] name, String field,
                                                     Handler<Either<String, JsonArray>> handler){
        return event -> {
            if (!OK.equals(( event.body()).getString(STATUS))) {
                String error = event.body().getString(MESSAGE);
                log.error("nameHandler : " + error);
                if(error.contains(CONNECTION_WAS_CLOSED) || error.contains(TIME)){
                    log.info("RESTART "+ name);
                    getNameEntity(name, field, handler);
                    return ;
                }
                handler.handle(new Either.Left<>("Error While get User in Neo4J "));
            } else {

                JsonArray rNeo =  event.body().getJsonArray(RESULT, new fr.wseduc.webutils.collections.JsonArray());
                if (rNeo.size() > 0) {
                    handler.handle(new Either.Right<>(rNeo));
                } else {
                    // Si l'id n'est pas retrouvé dans l'annuaire, on le récupère dans Postgres
                    String queryPostgres = " SELECT  personnes_supp.last_name as name " +
                            " FROM " + Competences.VSCO_SCHEMA + ".personnes_supp" +
                            " WHERE id_user IN " + Sql.listPrepared(Arrays.asList(name));

                    JsonArray paramsPostgres = new JsonArray();
                    for (int i = 0; i < name.length; i++) {
                        paramsPostgres.add(name[i]);
                    }

                    Sql.getInstance().prepared(queryPostgres, paramsPostgres,SqlResult.validResultHandler(handler));
                }
            }
        };
    }
    @Override
    public void getNameEntity(String[] name, String field,  Handler<Either<String, JsonArray>> handler) {

        String returning = "WHERE s.id IN {id} RETURN CASE WHEN s.name IS NULL THEN s.lastName ELSE s.name END AS name";
        String query = "";
        if(field.equals(ID_STRUCTURE_KEY)){
            query= "MATCH (s:Structure) " + returning;
        } else if(field.equals(ID_CLASSE_KEY)){
            query= " MATCH (s:Class) " + returning + " UNION MATCH (s:FunctionalGroup) " +  returning +
                    " UNION MATCH (s:ManualGroup) " + returning;
        } else if(field.equals(ID_ELEVE_KEY)){
            query= " MATCH (s:User {profiles: ['Student']}) " + returning;
        }
        JsonObject params = new JsonObject();
        params.put(ID_KEY, new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(name)));
        try {
            neo4j.execute(query, params, nameHandler(name, field, handler));
        }
        catch( VertxException e) {
            log.error("getNameEntity " + e.getMessage() + " stack : " + e.getStackTrace());
            getNameEntity(name, field, handler);
        }
    }

    @Override
    public void linkGroupesCycles(final String[] idClasses, final Number id_cycle, final Number[] typeGroupes,
                                  final Handler<Either<String, JsonArray>> handler) {
        if (idClasses.length > 0) {
            checkDataOnClasses(idClasses, new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isRight()) {
                        final JsonArray listDevoir = event.right().getValue();

                        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();

                        // SUPPRESSION DES DEVOIRS AVEC COMPETENCES AVANT LE CHANGEMENT
                        if (listDevoir.size() > 0) {
                            StringBuilder queryDeleteDevoir = new StringBuilder();
                            JsonArray idDevoirs = new fr.wseduc.webutils.collections.JsonArray();
                            queryDeleteDevoir.append("DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs")
                                    .append(" WHERE id IN " + Sql.listPrepared(listDevoir.getList().toArray()));
                            for (int i = 0; i < listDevoir.size(); i++) {
                                idDevoirs.add(((JsonObject) listDevoir.getJsonObject(i)).getLong("id"));
                            }

                            statements.add(new JsonObject()
                                    .put("statement", queryDeleteDevoir.toString())
                                    .put("values", idDevoirs)
                                    .put("action", "prepared"));
                        }

                        // CREATION DU LIEN VERS LE NOUVEAU CYCLE
                        StringBuilder queryLink = new StringBuilder();
                        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
                        if(id_cycle.intValue() != 0) {
                            queryLink.append("INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle ")
                                    .append(" (id_cycle, id_groupe, type_groupe) VALUES ");
                            for (int i = 0; i < idClasses.length; i++) {
                                queryLink.append(" (?, ?, ?) ");
                                values.add(id_cycle).add(idClasses[i]).add(typeGroupes[i]);
                                if (i != (idClasses.length - 1)) {
                                    queryLink.append(",");
                                } else {
                                    queryLink.append(" ON CONFLICT (id_groupe)")
                                            .append(" DO UPDATE SET id_cycle = ? ");
                                    values.add(id_cycle);
                                }
                            }
                        }
                        else {
                            queryLink.append("DELETE FROM ").append(COMPETENCES_SCHEMA).append(".rel_groupe_cycle")
                                    .append(" WHERE id_groupe IN ").append(Sql.listPrepared(idClasses));
                            for (String idClass : idClasses) {
                                values.add(idClass);
                            }
                        }

                        statements.add(new JsonObject()
                                .put("statement", queryLink.toString())
                                .put("values", values)
                                .put("action", "prepared"));

                        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(handler));
                    } else {
                        handler.handle(event.left());
                    }
                }
            });
        } else {
            handler.handle(new Either.Left<String, JsonArray>("IdClasses is Empty "));
        }
    }

    @Override
    public void checkDataOnClasses(String[] idClasses, final Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        query.append(" SELECT devoirs.id, devoirs.name, id_groupe, COUNT(" + Field.COMPETENCES_DEVOIRS + ".id) as nbcompetences ")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs ")
                .append(" LEFT JOIN notes.rel_devoirs_groupes ")
                .append(" ON rel_devoirs_groupes.id_devoir = devoirs.id ")
                .append(" LEFT OUTER JOIN " + Competences.COMPETENCES_SCHEMA + "." + Field.COMPETENCES_DEVOIRS)
                .append(" ON devoirs.id = " + Field.COMPETENCES_DEVOIRS + ".id_devoir ")
                .append(" WHERE rel_devoirs_groupes.id_groupe IN " + Sql.listPrepared(idClasses))
                .append(" GROUP BY devoirs.id, devoirs.name, id_groupe ")
                .append(" HAVING COUNT(" + Field.COMPETENCES_DEVOIRS + ".id) > 0 ")
                .append(" ORDER BY id_groupe ");

        for (String id : idClasses) {
            values.add(id);
        }

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void studentIdAvailableForPeriode(final String idClasse, final Long idPeriode, final Integer typeClasse,
                                             Handler<Either<String, JsonArray>> handler) {
        // Si on a pas de filtre sur la période, on ne récupère pas les période
        // On va renvoyer tous les id des élèves de la classe ou du groupe d'enseignement
        if (idPeriode == null) {
            calculAvailableId(idClasse, typeClasse, null, null, null, handler);
            return;
        } else {
            JsonObject action = new JsonObject();
            action.put("action", "periode.getPeriodes")
                    .put("idGroupes", new fr.wseduc.webutils.collections.JsonArray().add(idClasse));

            eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                    handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            JsonObject body = message.body();
                            JsonArray periodes = body.getJsonArray("result");
                            JsonArray idAvailableEleve = new JsonArray();

                            if ("ok".equals(body.getString("status"))) {
                                // On récupére la période de la classe
                                JsonObject periode = null;
                                for (int i = 0; i < periodes.size(); i++) {

                                    if (idPeriode.intValue()
                                            == periodes.getJsonObject(i).getInteger("id_type").intValue()) {
                                        periode = periodes.getJsonObject(i);
                                        break;
                                    }
                                }
                                if (periode != null) {
                                    String debutPeriode = periode.getString("timestamp_dt")
                                            .split("T")[0];
                                    String finPeriode = periode.getString("timestamp_fn")
                                            .split("T")[0];

                                    DateFormat formatter = new SimpleDateFormat("yy-MM-dd");
                                    try {
                                        final Date dateDebutPeriode = formatter.parse(debutPeriode);
                                        final Date dateFinPeriode = formatter.parse(finPeriode);

                                        calculAvailableId(idClasse, typeClasse, idPeriode,
                                                dateDebutPeriode, dateFinPeriode, handler);

                                    } catch (ParseException e) {
                                        String messageLog = "Error :can not calcul students of groupe : " + idClasse;
                                        log.error(messageLog);
                                        handler.handle(new Either.Left<>(messageLog));
                                    }
                                } else {
                                    handler.handle(new Either.Right<>(idAvailableEleve));
                                }
                            }

                        }
                    }));
        }
    }

    public void getPeriodes(JsonArray idClasse, String idEtablissement, Handler<Either<String,JsonArray>> handler){
        getPeriodes((List<String>)idClasse.getList(), idEtablissement, handler);
    }
    public void getPeriodes(List<String> idClasses, String idEtablissement, Handler<Either<String,JsonArray>> handler){
        JsonObject action = new JsonObject()
                .put("action", "periode.getPeriodes")
                .put("idGroupes", idClasses)
                .put("idEtablissement", idEtablissement);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            JsonArray periodes = body.getJsonArray("result");
            if ("ok".equals(body.getString("status"))) {
                handler.handle(new Either.Right<>(periodes));
            } else {
                handler.handle(new Either.Left<>("no periode for this class : " + body.getString("message")) );
            }
        }));
    }

    public Future<JsonArray> getPeriodes(List<String> idClasses, String idEtablissement) {
        Promise<JsonArray> promise = Promise.promise();
        getPeriodes(idClasses, idEtablissement, FutureHelper.handlerJsonArray(promise.future()));
        return promise.future();
    }

    protected void calculAvailableId(String idClasse, Integer typeClasse, Long idPeriode,
                                     Date dateDebutPeriode, Date dateFinPeriode,
                                     Handler<Either<String, JsonArray>> handler) {
        if (!(typeClasse == 0 || typeClasse == 1 || typeClasse == 2)) {
            handler.handle(new Either.Left<>("bad Request"));
            return;
        }
        studentAvailableForPeriode(idClasse, null, typeClasse, message -> {
            JsonObject body = message.body();
            JsonArray students = body.getJsonArray("results");
            JsonArray idAvailableEleve = new JsonArray();

            if ("ok".equals(body.getString("status"))) {
                // Si aucune période n'est sélectionnée, on rajoute tous les élèves
                for (int i = 0; i < students.size(); i++) {
                    JsonObject student = (JsonObject) students.getValue(i);
                    // Sinon Si l'élève n'est pas Supprimé on l'ajoute
                    if (idPeriode == null ||
                            student.getValue("deleteDate") == null) {
                        idAvailableEleve.add(student.getString("id"));
                    }
                    // Sinon S'il sa date sa suppression survient avant la fin de
                    // la période, on l'ajoute aussi
                    else {
                        Date deleteDate = new Date();

                        if (student.getValue("deleteDate")
                                instanceof Number) {
                            deleteDate = new Date(student.getLong("deleteDate"));
                        } else {
                            try {

                                deleteDate = new SimpleDateFormat("yy-MM-dd")
                                        .parse(student.getString("deleteDate").split("T")[0]);

                            } catch (ParseException e) {
                                String messageLog = "PB While read date of deleted Student : "
                                        + student.getString("id");
                                log.error(messageLog);
                            }

                        }
                        if ((deleteDate.after(dateFinPeriode) || deleteDate.equals(dateFinPeriode))
                                ||
                                ((deleteDate.after(dateDebutPeriode)
                                        || deleteDate.equals(dateDebutPeriode))
                                        && (deleteDate.before(dateFinPeriode)
                                        || deleteDate.equals(dateFinPeriode)))) {
                            idAvailableEleve.add(student.getString("id"));
                        }
                    }
                }
            }
            handler.handle(new Either.Right<>(idAvailableEleve));
        });

    }

    public void insertEvenement(String idEleve, String colonne, Long idPeriode, Long value,
                                Handler<Either<String, JsonArray>> handler) {

        JsonArray valideColonne = new JsonArray().add("abs_totale").add("abs_totale_heure").add("abs_non_just")
                .add("abs_non_just_heure").add("abs_just").add("abs_just_heure").add("retard");
        if (!valideColonne.contains(colonne)) {
            String message = I18n.getInstance().translate("viescolaire.no.column.is.named",
                    I18n.DEFAULT_DOMAIN, Locale.FRANCE);
            log.error(" insertEvernement | " + message);
            handler.handle(new Either.Left<>(message));
        } else {
            if (idEleve == null) {
                String message = " insertEvenement | idEleve is null ";
                log.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                StringBuilder query = new StringBuilder()
                        .append(" INSERT INTO viesco.absences_et_retards( id_periode, id_eleve, ")
                        .append(colonne + " )")
                        .append(" VALUES ")
                        .append(" (?, ?, ?) ")
                        .append(" ON CONFLICT (id_periode, id_eleve) ")
                        .append(" DO UPDATE SET " + colonne + "= ? ");
                JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

                params.add(idPeriode).add(idEleve).add(value).add(value);
                Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultsHandler(handler));
            }
        }
    }

    private void getStructureImage(String idStructure, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder()
                .append(" SELECT * FROM " + Competences.VSCO_SCHEMA + ".logo_etablissement ")
                .append(" WHERE id_etablissement = ? ");
        JsonArray params = new JsonArray().add(idStructure);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }

    public void getInformationCE(String idStructure, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder()
                .append(" SELECT * FROM " + Competences.VSCO_SCHEMA + ".nom_et_signature_CE ")
                .append(" WHERE id_etablissement = ? ");
        JsonArray params = new JsonArray().add(idStructure);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }

    public void setStructureImage(String idStructure, String path, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder()
                .append(" INSERT INTO " + Competences.VSCO_SCHEMA + ".logo_etablissement ")
                .append(" (id_etablissement, path) VALUES ")
                .append(" ( ?, ?) ")
                .append(" ON CONFLICT (id_etablissement) ")
                .append(" DO UPDATE SET path = ? ");

        JsonArray params = new JsonArray().add(idStructure).add(path).add(path);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }

    public void setInformationCE(String idStructure, String path, String name,
                                 Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder()
                .append(" INSERT INTO " + Competences.VSCO_SCHEMA + ".nom_et_signature_CE ")
                .append(" (id_etablissement, path, name) VALUES ( ?, ?, ?) ")
                .append(" ON CONFLICT (id_etablissement) DO UPDATE SET path = ?, name = ? ");

        JsonArray params = new JsonArray().add(idStructure).add(path).add(name).add(path).add(name);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getParametersForExport(String idStructure, Handler<Either<String, JsonObject>> handler) {
        Future<JsonObject> imageStructureFuture = Future.future();
        getStructureImage(idStructure, event -> {
            if (event.isRight()) {
                imageStructureFuture.complete(event.right().getValue());
            } else {
                log.error(event.left());
                imageStructureFuture.complete(new JsonObject());
            }
        });

        Future<JsonObject> infosCEFuture = Future.future();
        getInformationCE(idStructure, event -> {
            if (event.isRight()) {
                infosCEFuture.complete(event.right().getValue());
            } else {
                log.error(event.left());
                infosCEFuture.complete(new JsonObject());
            }
        });

        CompositeFuture.all(infosCEFuture, imageStructureFuture).setHandler(event -> {
            if (event.succeeded()) {
                JsonObject result = new JsonObject();
                result.put("imgStructure", imageStructureFuture.result());
                result.put("nameAndBrad", infosCEFuture.result());
                handler.handle(new Either.Right<>(result));
            } else {
                handler.handle(new Either.Left<>(event.cause().getMessage()));
            }
        });
    }

    @Override
    public void getLibelleMatAndTeacher(SortedMap<String, Set<String>> mapIdMatiereIdsTeacher,
                                        Handler<Either<String, SortedMap<String, JsonObject>>> handler) {
        JsonArray idsTeacher = new JsonArray();
        JsonArray idsMatiere = new JsonArray();

        mapIdMatiereIdsTeacher.forEach((currIdMatOrMatGroupe, currIdTeachers) -> {
            currIdTeachers.forEach(idTeacher -> {
                if (!idsTeacher.contains(idTeacher)) {
                    idsTeacher.add(idTeacher);
                }
            });

            // mapIdMatiereIdsTeacher peut contenir soit des id matiere soit des idMatiere;idGroupe
            String[] split = currIdMatOrMatGroupe.split(";");
            if (split.length == 1 || split.length == 2) {
                String idMat = split[0];
                if (!idsMatiere.contains(idMat)) {
                    idsMatiere.add(idMat);
                }
            } else {
                String erreur = "getLibelleMatAndTeacher : id format for mapIdMatiereIdsTeacher is not supported";
                log.error(erreur);
                handler.handle(new Either.Left<>(erreur));
            }
        });

        Utils.getLibelleMatiere(eb, idsMatiere, new Handler<Either<String, Map<String, JsonObject>>>() {
            @Override
            public void handle(Either<String, Map<String, JsonObject>> respMat) {
                if (respMat.isLeft()) {
                    log.error("getLibelleMatAndTeacher : getLibelleMat " + respMat.left().getValue());
                    handler.handle(new Either.Left<>(respMat.left().getValue()));
                } else {
                    Utils.getLastNameFirstNameUser(eb, idsTeacher, new Handler<Either<String, Map<String, JsonObject>>>() {
                        @Override
                        public void handle(Either<String, Map<String, JsonObject>> respTeacher) {
                            if (respTeacher.isLeft()) {
                                log.error("getLibelleMatAndTeacher : getLastNameFirstNameUser " + respMat.left().getValue());
                                handler.handle(new Either.Left<>(respMat.left().getValue()));
                            } else {
                                Map<String, JsonObject> mapIdMatLibelle = respMat.right().getValue();
                                Map<String, JsonObject> mapIdTeacher = respTeacher.right().getValue();

                                SortedMap<String, JsonObject> mapIdMatLibelleMapEtProf = new TreeMap<>();

                                for (Map.Entry<String, Set<String>> setEntry : mapIdMatiereIdsTeacher.entrySet()) {
                                    // setEntry.getKey() peut contenir soit des id matiere soit des idMatiere;idGroupe
                                    String idMat = setEntry.getKey().split(";")[0];
                                    JsonArray teachers = new fr.wseduc.webutils.collections.JsonArray();
                                    JsonObject matiere = new JsonObject();

                                    if (mapIdMatLibelle != null && !mapIdMatLibelle.isEmpty() && mapIdMatLibelle.containsKey(idMat)) {
                                        //matiere = response with libelle,code,libelle_court...
                                        matiere = mapIdMatLibelle.get(idMat);
                                        matiere.remove("data");
                                    } else {
                                        matiere.put("id", idMat).put("name", "no libelle").put("libelle_court", "no libelle");
                                        log.error("matiere non retrouvee sans libelle idMatiere : " + idMat);
                                    }

                                    for (String idTeacher : setEntry.getValue()) {
                                        if (mapIdTeacher != null && !mapIdTeacher.isEmpty() && mapIdTeacher.containsKey(idTeacher)) {
                                            String displayName = mapIdTeacher.get(idTeacher).getString("firstName").substring(0, 1) + ".";
                                            String lastName = mapIdTeacher.get(idTeacher).getString("name").replace("-", "\n");
                                            displayName = lastName + " " + displayName;
                                            teachers.add(new JsonObject()
                                                    .put("id_teacher", idTeacher)
                                                    .put("displayName", (displayName.length() <= 10) ? displayName : lastName));
                                        } else {
                                            teachers.add(new JsonObject()
                                                    .put("id_teacher", idTeacher)
                                                    .put("displayName", "no Name"));
                                            log.error("enseignant non retrouve idTeacher : " + idTeacher);
                                        }
                                    }
                                    matiere.put("teachers", teachers);
                                    mapIdMatLibelleMapEtProf.put(setEntry.getKey(), matiere);
                                }
                                handler.handle(new Either.Right<>(mapIdMatLibelleMapEtProf));
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public JsonArray sortArray(JsonArray jsonArr, String[] sortedField) {
        JsonArray sortedJsonArray = new JsonArray();

        List<JsonObject> jsonValues = new ArrayList<JsonObject>();
        if (jsonArr.size() > 0 && !(jsonArr.getValue(0) instanceof JsonObject)) {
            return jsonArr;
        } else {
            for (int i = 0; i < jsonArr.size(); i++) {
                jsonValues.add(jsonArr.getJsonObject(i));
            }
            Collections.sort(jsonValues, new Comparator<JsonObject>() {

                @Override
                public int compare(JsonObject a, JsonObject b) {
                    String valA = new String();
                    String valB = new String();

                    try {
                        for (int i = 0; i < sortedField.length; i++) {
                            valA += ((String) a.getValue(sortedField[i])).toLowerCase();
                            valB += ((String) b.getValue(sortedField[i])).toLowerCase();
                        }
                    } catch (Exception e) {
                        //do something
                        log.error("Pb While Sorting Two Array", e);
                    }

                    return valA.compareTo(valB);
                    //if you want to change the sort order, simply use the following:
                    //return -valA.compareTo(valB);
                }
            });

            for (int i = 0; i < jsonArr.size(); i++) {
                sortedJsonArray.add(jsonValues.get(i));
            }
            return sortedJsonArray;
        }
    }

    public JsonObject findWhere(JsonArray collection, JsonObject oCriteria) {

        Integer matchNeeded = oCriteria.size();
        Integer matchFound = 0;

        for (Object o : collection) {
            JsonObject object = (JsonObject) o;
            for (Map.Entry<String, Object> criteriaValue : oCriteria.getMap().entrySet()) {
                if (object.containsKey(criteriaValue.getKey()) && object.getValue(criteriaValue.getKey()).equals(criteriaValue.getValue())) {
                    matchFound++;
                    if (matchFound.equals(matchNeeded)) {
                        return object;
                    }
                }
            }
            matchFound = 0;
        }
        return null;
    }

    public JsonArray where(JsonArray collection, JsonObject oCriteria) {
        JsonArray loopCollection = new JsonArray(new ArrayList(collection.getList()));
        JsonArray result = new JsonArray();
        JsonObject found;

        do {
            found = findWhere(loopCollection, oCriteria);
            if (found != null) {
                loopCollection.remove(found);
                result.add(found);
            }
        }
        while (found != null);

        return result;
    }

    public JsonArray pluck(JsonArray collection, String key) {
        JsonArray result = new JsonArray();

        for (Object o : collection) {
            JsonObject castedO = (JsonObject) o;
            if (castedO.containsKey(key)) {
                result.add(castedO.getValue(key));
            }
        }

        return result;
    }

    public JsonObject getObjectForPeriode(JsonArray array, Long idPeriode, String key) {
        JsonObject res = null;
        if (isNotNull(array)) {
            for (int i = 0; i < array.size(); i++) {
                JsonObject o = array.getJsonObject(i);
                if (isNotNull(o)){
                    if((isNotNull(o.getValue(key)) && o.getLong(key).equals(idPeriode) && isNotNull(idPeriode))
                            || (isNull(idPeriode) && isNull(o.getValue(key))) ) {
                        res = o;
                    }
                }
            }
        }
        return res;
    }

    public int getPositionnementValue(Float moyenne, JsonArray tableauDeconversion) {
        int value = -1;

        for (int i = 0; i < tableauDeconversion.size(); i++) {
            JsonObject ligne = tableauDeconversion.getJsonObject(i);
            Float valmin = ligne.getFloat("valmin");
            Float valmax = ligne.getFloat("valmax");
            int ordre = ligne.getInteger("ordre");

            if ((valmin <= moyenne && valmax > moyenne) && ordre != tableauDeconversion.size()) {
                value = ordre;
            } else if ((valmin <= moyenne && valmax >= moyenne) && ordre == tableauDeconversion.size()) {
                value = ordre;
            }
        }
        return value;
    }

    public String convertPositionnement(Float moyenne, JsonArray tableauDeconversion, Boolean translation) {
        String val = "";
        float moyenneToSend;
        if (moyenne != null && moyenne != -1 && tableauDeconversion != null) {
            moyenneToSend = (translation) ? moyenne + 1 : moyenne;

            int posConverti = getPositionnementValue(moyenneToSend, tableauDeconversion);
            if (posConverti != -1) {
                val = String.valueOf(posConverti);
            }
        }
        return val;
    }

    public void studentAvailableForPeriode(final String idClasse, final Long idPeriode, final Integer typeClasse,
                                           Handler<Message<JsonObject>> handler) {
        JsonObject action = new JsonObject();
        if (typeClasse == 0) {
            action.put("action", "classe.getEleveClasse")
                    .put("idClasse", idClasse);
        } else if (typeClasse == 1 || typeClasse == 2) {
            action.put("action", "groupe.listUsersByGroupeEnseignementId")
                    .put("groupEnseignementId", idClasse)
                    .put("profile", "Student");
        }
        if (idPeriode != null) {
            action.put(ID_PERIODE_KEY, idPeriode);
        }
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS, handlerToAsyncHandler(handler));
    }

    public void getClassInfo ( final String idClass, Handler<Either<String,JsonObject>> handler){
        JsonObject action = new JsonObject()
                .put("action", "classe.getClasseInfo")
                .put("idClasse", idClass);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status"))) {
                    JsonObject classInfo = body.getJsonObject("result").getJsonObject("c").getJsonObject("data");
                    handler.handle(new Either.Right<>(classInfo));
                } else {
                    log.error("GetClassInfo : can not get class info : "+ idClass);
                    handler.handle(new Either.Left<>(body.getString(MESSAGE)));
                }
            }
        }));
    }

    public void getActiveStatePresences ( final String idStructure, Handler<Either<String,JsonObject>> handler){
        // Récupération de la config vie scolaire
        Promise<JsonObject> configFuture = Promise.promise();
        JsonObject action = new JsonObject()
                .put("action", "config.generale");
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if (OK.equals(body.getString(STATUS))) {
                    JsonObject queryResult = body.getJsonObject(RESULT);
                    configFuture.complete(queryResult);
                } else {
                    log.error("getRetardsAndAbsences-getconfigVieScolaire failed : " + body.getString("message"));
                    configFuture.fail(body.getString("message"));

                }
            }
        }));

        // Récupération de l'activation du module présences de l'établissement
//        isStructureActivatePresences(idStructure,event -> formate(activationFuture,event));

        configFuture.future()
                .onSuccess(configEvent -> {
                    JsonObject result = new JsonObject();
                    boolean configInstalled = Boolean.TRUE.equals(configEvent.getBoolean("presences"));
                    result.put("installed",configInstalled);
                    if (configInstalled){
                        Future<JsonObject> activationFuture = Future.future();
                        isStructureActivatePresences(idStructure,event -> formate(activationFuture,event));
                        activationFuture.onSuccess(event -> {
                            result.put("activate",!event.isEmpty() && event.getBoolean("actif"));
                            handler.handle(new Either.Right<>(result));
                        }).onFailure(event -> handler.handle(new Either.Left<>("[getRetardsAndAbsences-config] "+event.getMessage())));
                    }else{
                        result.put("activate",false);
                        handler.handle(new Either.Right<>(result));
                    }
                })
                .onFailure(event -> handler.handle(new Either.Left<>("[getRetardsAndAbsences-config] "+event.getMessage())));;
    }

    public void getSyncStatePresences(String idStructure, Handler<Either<String, JsonObject>> eitherHandler){
        JsonArray params = new JsonArray().add(idStructure);

        String query = "SELECT presences_sync " +
                " FROM " + Competences.COMPETENCES_SCHEMA + ".structure_options " +
                " WHERE id_structure = ? ";
        Sql.getInstance().prepared(query, params, Competences.DELIVERY_OPTIONS, validUniqueResultHandler(eitherHandler));

    }

    public void activeDeactiveSyncStatePresences(String idStructure, Boolean state, Handler<Either<String, JsonObject>> eitherHandler){
        StringBuilder query = new StringBuilder().append("INSERT INTO ")
                .append(Competences.COMPETENCES_SCHEMA + ".structure_options (id_structure, presences_sync) ")
                .append(" VALUES ")
                .append(" ( ?, ? )")
                .append(" ON CONFLICT (id_structure) DO UPDATE SET presences_sync = ?");
        JsonArray params = new JsonArray().add(idStructure).add(state).add(state);
        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(eitherHandler));
    }

    @Override
    public void lauchTransition(List<String> structureIds) {
        for(int i = 0;  i<structureIds.size();i++){
            getStructure(structureIds.get(i), new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> event) {
                    JsonObject structure = event.right().getValue().getJsonObject("s").getJsonObject("data");
                    Competences.launchTransitionWorker(eb,new JsonObject().put("structure",structure).put("isHTTP",false),false);

                }
            });
        }

    }

    private void isStructureActivatePresences(String idStructure, Handler<Either<String, JsonObject>> eitherHandler){
        JsonArray params = new JsonArray().add(idStructure);

        String query = " SELECT * " +
                " FROM presences.etablissements_actifs " +
                " WHERE id_etablissement = ? ";
        Sql.getInstance().prepared(query, params, Competences.DELIVERY_OPTIONS, validUniqueResultHandler(eitherHandler));

    }

    @Override
    public void getYearsAndPeriodes(String idStructure, boolean onlyYear, Handler<Either<String,JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT start_date, end_date");

        if(!onlyYear){
            query.append(", JSON_AGG(DISTINCT(id_type)) AS periodes");
        }

        query.append(" FROM viesco.setting_period");

        if(!onlyYear) {
            query.append(" JOIN viesco.periode ON id_etablissement = id_structure");
        }

        query.append(" WHERE id_structure = ?")
                .append(" AND code = 'YEAR'")
                .append(" GROUP BY start_date, end_date;");
        JsonArray params = new JsonArray().add(idStructure);
        Sql.getInstance().prepared(query.toString(), params, Competences.DELIVERY_OPTIONS,
                validUniqueResultHandler(handler));
    }

    @Override
    public void getPresencesReasonsId(String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "get-reasons")
                .put("reasonType", 0)
                .put("structure", idStructure);

        eb.send("fr.openent.presences", action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    /**
     * getPeriode one periode By classes
     *
     * @param idEtablissement
     * @param idClasses
     * @param idPeriode
     * @param handler
     */
    @Override
    public void getPeriodesClasses (String idEtablissement, JsonArray idClasses, Long idPeriode, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "periode.getPeriodesClasses")
                .put("idEtablissement", idEtablissement)
                .put("idsClasse", idClasses)
                .put("idPeriode", idPeriode);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        JsonArray periodes = body.getJsonArray("results");
                        if ("ok".equals(body.getString("status"))) {
                            handler.handle(new Either.Right<String,JsonArray>(periodes) );
                        }else{
                            handler.handle(new Either.Left<String,JsonArray>("no periode for this class : " + body.getString("message")) );
                        }

                    }
                })

        );
    }

    @Override
    public void getYearsArchive(String idStructure, String type, Handler<Either<String, JsonArray>> handler) {
        String tableName = "archive_" + type;

        StringBuilder query = new StringBuilder()
                .append("SELECT DISTINCT id_annee")
                .append(" FROM notes.").append(tableName)
                .append(" WHERE id_etablissement = ?;");

        JsonArray params = new JsonArray().add(idStructure);
        Sql.getInstance().prepared(query.toString(), params, Competences.DELIVERY_OPTIONS,
                validResultHandler(handler));
    }
}