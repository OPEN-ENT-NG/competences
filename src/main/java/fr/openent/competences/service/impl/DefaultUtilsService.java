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

package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.service.UtilsService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;
import static org.entcore.common.sql.SqlResult.validResultHandler;


/**
 * Created by ledunoiss on 05/08/2016.
 */
public class DefaultUtilsService  implements UtilsService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultUtilsService.class);

    private final Neo4j neo4j = Neo4j.getInstance();

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
        JsonArray values = new JsonArray();

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
        JsonArray values = new JsonArray();

        query.append("SELECT DISTINCT id_titulaire ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_professeurs_remplacants ")
                .append("WHERE id_remplacant = ? ")
                .append("AND id_etablissement = ? ")
                .append("AND date_debut <= current_date ")
                .append("AND current_date <= date_fin ");

        values.add(psIdRemplacant);
        values.add(psIdEtablissement);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }


    @Override
    public void listTypesDevoirsParEtablissement(String idEtablissement, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new JsonArray();

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
                .append("RETURN u,n,c");
        neo4j.execute(query.toString(), new JsonObject().putString("id", id), Neo4jResult.validUniqueResultHandler(result));
    }

    @Override
    public void getEnfants(String id, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        //query.append("MATCH (m:`User` {id: {id}})-[:COMMUNIQUE_DIRECT]->(n:`User`) RETURN n");

        query.append("MATCH (m:`User`{id: {id}})<-[:RELATED]-(n:`User`)-[:ADMINISTRATIVE_ATTACHMENT]->(s:`Structure`) ")
                .append("WITH n.id as id, n.displayName as displayName, n.classes as externalIdClasse, s.id as idStructure,  ")
                .append("n.firstName as firstName, n.lastName as lastName MATCH(c:Class)WHERE c.externalId IN externalIdClasse")
                .append(" RETURN id, displayName, firstName, lastName, c.id as idClasse, idStructure");
        neo4j.execute(query.toString(), new JsonObject().putString("id", id), Neo4jResult.validResultHandler(handler));
    }

    /**
     * Fonction de calcul générique de la moyenne
     *
     * @param listeNoteDevoirs : contient une liste de NoteDevoir.
     *                         La formule suivante est utilisée :(SUM ( ni *m *ci /di)  + SUM ( nj *cj)  ) / (S ( ci)  + SUM ( cj  *dj /m)  )
     * @param diviseurM        : diviseur de la moyenne. Par défaut, cette valeur est égale à 20 (optionnel).
     **/
    @Override
    public JsonObject calculMoyenne(List<NoteDevoir> listeNoteDevoirs, Boolean statistiques, Integer diviseurM) {
        if (diviseurM == null) {
            diviseurM = 20;
        }
        Double noteMax = new Double(0);
        Double noteMin = new Double(diviseurM);
        Double notes = new Double(0);
        Double diviseur = new Double(0);

        // (SUM ( ni *m *ci /di)  + SUM ( nj *cj)  ) / (S ( ci)  + SUM ( cj  *dj /m)  )
        // avec d : diviseurs, n : note, c : coefficient, m = 20 : si ramené sur
        // avec i les notes ramenées sur m, et j les notes non ramenées sur m

        Double sumCI = new Double(0);
        Double sumCJDJParM = new Double(0);
        Double sumCJDJ = new Double(0);
        Double sumNIMCIParD = new Double(0);

        for (NoteDevoir noteDevoir : listeNoteDevoirs) {
            Double currNote = noteDevoir.getNote();
            Double currCoefficient = noteDevoir.getCoefficient();
            Double currDiviseur = noteDevoir.getDiviseur();

            if (!noteDevoir.getRamenerSur()) {
                sumCJDJParM += (currCoefficient * currDiviseur / diviseurM);
                sumCJDJ += (currNote * currCoefficient);
            } else {
                sumNIMCIParD += ((currNote * diviseurM * currCoefficient) / currDiviseur);
                sumCI += currCoefficient;
            }

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

        DecimalFormat df = new DecimalFormat("##.##", symbols);
        try {
            moyenne = Double.valueOf(df.format(moyenne));
        } catch (NumberFormatException e) {
            log.error("Moyenne : " + String.valueOf(moyenne), e);
        }
        JsonObject r = new JsonObject().putNumber("moyenne", moyenne);
        if (statistiques) {
            r.putNumber("noteMax", noteMax).putNumber("noteMin", noteMin);
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

        Double noteMax = new Double(0);
        Double noteMin = null;
        Double notes = new Double(0);
        //Double diviseur = new Double(0);

        for (NoteDevoir noteDevoir : listeNoteDevoirs) {
            Double currNote = noteDevoir.getNote();
            notes += currNote;
            // Calcul de la note min et max
            if (statistiques) {
                if (null == noteMin) {
                    noteMin = new Double(noteDevoir.getDiviseur());
                    ;
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

        DecimalFormat df = new DecimalFormat("##.##", symbols);
        try {
            moyenne = Double.valueOf(df.format(moyenne));
        } catch (NumberFormatException e) {
            log.error("Moyenne : " + String.valueOf(moyenne), e);
        }
        JsonObject r = new JsonObject().putNumber("moyenne", moyenne);
        if (statistiques) {
            r.putNumber("noteMax", noteMax).putNumber("noteMin", noteMin);
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
        neo4j.execute(query, new JsonObject().putString("id", id), Neo4jResult.validUniqueResultHandler(handler));
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
            params.putArray("expectedProfiles", expectedProfiles);
        }
        if (classId != null && !classId.trim().isEmpty()) {
            filter = "(n:Class {id : {classId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-";
            params.putString("classId", classId);
        } else if (structureId != null && !structureId.trim().isEmpty()) {
            filter = "(n:Structure {id : {structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-";
            params.putString("structureId", structureId);
        } else if (groupId != null && !groupId.trim().isEmpty()) {
            filter = "(n:Group {id : {groupId}})<-[:IN]-";
            params.putString("groupId", groupId);
        }
        String condition = "";
        String functionMatch = "WITH u MATCH (s:Structure)<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), u-[:IN]->pg ";

        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            condition += "AND u.displayName =~ {regex}  ";
            params.putString("regex", "(?i)^.*?" + Pattern.quote(nameFilter.trim()) + ".*?$");
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
            recipient.add(list.get(i));
        }
        return recipient;
    }

    @Override
    public <K> void addToMap(K id, HashMap<K, ArrayList<NoteDevoir>> map, NoteDevoir valueToAdd) {
        if (map.containsKey(id)) {

            map.get(id).add(valueToAdd);

        } else {

            ArrayList<NoteDevoir> notes = new ArrayList<>();
            notes.add(valueToAdd);
            map.put(id, notes);
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
        JsonArray params = new JsonArray();

        query.append("SELECT id_groupe, id_cycle, libelle, value_cycle ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle,  ")
                .append(Competences.COMPETENCES_SCHEMA + ".cycle ")
                .append("WHERE id_groupe IN " + Sql.listPrepared(idClasse.toArray()))
                .append(" AND id_cycle = cycle.id");

        for (String id : idClasse) {
            params.addString(id);
        }
        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
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
        JsonArray params = new JsonArray();

        query.append("SELECT id_cycle ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle,  ")
                .append(Competences.COMPETENCES_SCHEMA + ".cycle ")
                .append("WHERE id_groupe = ? ")
                .append(" AND id_cycle = cycle.id");

        params.addString(idClasse);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }


    @Override
    public void getNameEntity(String[] name, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonObject params = new JsonObject();

        query.append("MATCH (s) WHERE s.id IN {id} RETURN CASE WHEN s.name IS NULL THEN s.lastName ELSE s.name END AS name");
        params.putArray("id", new JsonArray(name));

        neo4j.execute(query.toString(), params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void linkGroupesCycles(final String[] idClasses, final Number id_cycle, final Number[] typeGroupes,
                                 final Handler<Either<String, JsonArray>> handler) {
        if (idClasses.length > 0 ) {
            checkDataOnClasses(idClasses, new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isRight()) {
                        final JsonArray listDevoir = event.right().getValue();

                        JsonArray statements = new JsonArray();

                        // SUPPRESSION DES DEVOIRS AVEC COMPETENCES AVANT LE CHANGEMENT
                        if (listDevoir.size() > 0 ) {
                            StringBuilder queryDeleteDevoir = new StringBuilder();
                            JsonArray idDevoirs = new JsonArray();
                            queryDeleteDevoir.append("DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs")
                                    .append(" WHERE id IN " + Sql.listPrepared(listDevoir.toArray()));
                            for(int i =0; i < listDevoir.size(); i++) {
                                idDevoirs.addNumber(((JsonObject)listDevoir.get(i)).getNumber("id"));
                            }

                            statements.add(new JsonObject()
                                    .putString("statement", queryDeleteDevoir.toString())
                                    .putArray("values", idDevoirs)
                                    .putString("action", "prepared"));
                        }

                        // CREATION DU LIEN VERS LE NOUVEAU CYCLE
                        StringBuilder queryLink = new StringBuilder()
                                .append("INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle ")
                                .append(" (id_cycle, id_groupe, type_groupe) VALUES ");
                        JsonArray values = new JsonArray();
                        for (int i = 0; i < idClasses.length; i++) {
                            queryLink.append(" (?, ?, ?) ");
                            values.addNumber(id_cycle)
                                    .addString(idClasses[i]).addNumber(typeGroupes[i]);
                            if (i != (idClasses.length - 1)) {
                                queryLink.append(",");
                            } else {
                                queryLink.append(" ON CONFLICT (id_cycle, id_groupe) DO UPDATE SET id_cycle = ? ");
                                values.addNumber(id_cycle);
                            }
                        }
                        statements.add(new JsonObject()
                                .putString("statement", queryLink.toString())
                                .putArray("values", values)
                                .putString("action", "prepared"));


                        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(handler));
                    } else {
                        handler.handle(event.left());
                    }
                }
            });
        }
        else {
            handler.handle(new Either.Left<String, JsonArray>("IdClasses is Empty "));
        }
    }
    @Override
    public void checkDataOnClasses(String[] idClasses, final Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new JsonArray();
        query.append(" SELECT devoirs.id, devoirs.name, id_groupe, COUNT(competences_devoirs.id) as nbcompetences ")
                .append(" FROM "+ Competences.COMPETENCES_SCHEMA +".devoirs ")
                .append(" LEFT JOIN notes.rel_devoirs_groupes ")
                .append(" ON rel_devoirs_groupes.id_devoir = devoirs.id ")
                .append(" LEFT OUTER JOIN "+ Competences.COMPETENCES_SCHEMA +".competences_devoirs ")
                .append(" ON devoirs.id = competences_devoirs.id_devoir ")
                .append(" WHERE rel_devoirs_groupes.id_groupe IN " + Sql.listPrepared(idClasses))
                .append(" GROUP BY devoirs.id, devoirs.name, id_groupe ")
                .append(" HAVING COUNT(competences_devoirs.id) > 0 ")
                .append(" ORDER BY id_groupe ");


        for (String id : idClasses) {
            values.addString(id);
        }

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

}
