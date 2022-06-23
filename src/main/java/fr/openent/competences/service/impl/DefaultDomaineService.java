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
import fr.openent.competences.constants.Field;
import fr.openent.competences.service.DomainesService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import static fr.openent.competences.Competences.DELIVERY_OPTIONS;

public class DefaultDomaineService extends SqlCrudService implements DomainesService {
    public DefaultDomaineService(String schema, String table) {
        super(schema, table);
    }
    public DefaultDomaineService() {
        super(Competences.COMPETENCES_SCHEMA, Field.DOMAINES_TABLE);
    }

    @Override
    public void getArbreDomaines(String idClasse, String idEleve, Long idCycle, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();


        query.append("WITH RECURSIVE search_graph(niveau, id, id_parent, libelle, codification, ")
                .append(" evaluated, pathinfo, id_cycle, dispensable) AS ")
                .append(" ( ")
                .append(" SELECT 1 as niveau, id, id_parent, libelle, codification, evaluated, array[id] as pathinfo ")
                .append(" , id_cycle, dispensable")
                .append(" FROM "+ Competences.COMPETENCES_SCHEMA +"." + Field.DOMAINES_TABLE)
                .append(" WHERE id_parent = 0 ");

        if(null != idCycle) {
            query.append(" AND id_cycle = ? ");
        } else if(null != idClasse) {
            query.append(" AND id_cycle = (SELECT id_cycle FROM " + Competences.COMPETENCES_SCHEMA)
                    .append(".rel_groupe_cycle WHERE id_groupe = ?) ");
        }

        query.append(" UNION ")
                .append(" SELECT sg.niveau + 1  as niveau , dom.id, dom.id_parent, dom.libelle, ")
                .append(" dom.codification, dom.evaluated, sg.pathinfo||dom.id, dom.id_cycle, dom.dispensable ")
                .append(" FROM "+ Competences.COMPETENCES_SCHEMA +"." + Field.DOMAINES_TABLE + " dom , search_graph sg ")
                .append(" WHERE dom.id_parent = sg.id ")
                .append(") ")
                .append(" SELECT niveau, id, id_parent, libelle, codification, evaluated, id_cycle, dispensable, " )
                .append(" libelle as nom, libelle as nomHtml " );
        if(idEleve != null){
            query.append(", " + Field.DISPENSE_DOMAINE_ELEVE + ".dispense as dispense_eleve ");
        }
        query .append("FROM search_graph");
        if(idEleve !=null){
            query.append(" LEFT JOIN notes." + Field.DISPENSE_DOMAINE_ELEVE + "  ON search_graph.id = " + Field.DISPENSE_DOMAINE_ELEVE + ".id_domaines ")
                    .append("AND " + Field.DISPENSE_DOMAINE_ELEVE + ".id_eleve = ?");
        }
        query.append(" GROUP BY niveau, id, id_parent, libelle, codification, evaluated, id_cycle, dispensable, nom, nomHtml, pathinfo");
        if(idEleve !=null){
            query.append(", dispense_eleve");
        }
        query.append(" ORDER BY pathinfo, id_cycle");

        if(null != idCycle) {
            params.add(idCycle);
        } else if(null != idClasse) {
            params.add(idClasse);
        }
        if(null != idEleve){
            params.add(idEleve);
        }
        Sql.getInstance().prepared(query.toString(), params , SqlResult.validResultHandler(handler));
    }

    @Override
    public void getDomainesLibCod(int[] idDomaines, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();


        query.append("SELECT id, id_parent, libelle, codification");
        query.append(" FROM notes." + Field.DOMAINES_TABLE);
        query.append(" WHERE id IN " + listIntPrepared(idDomaines));

        for(int s : idDomaines) {
            params.add(s);
        }
        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    private static String listIntPrepared(int[] array) {
        StringBuilder sb = new StringBuilder("(");
        if (array != null && array.length > 0) {
            for (int i = 0; i< array.length; i++) {
                sb.append("?,");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.append(")").toString();
    }

    @Override
    public void getDomainesRacines(String idClasse, Long idCycle, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        //On extrait d'abord les domaines evalués et dont le cycle correspond à celui de la classe
        query.append("WITH evaluated_domaines AS ")
                .append("(SELECT id, id_parent, libelle, codification ")
                .append("FROM notes." + Field.DOMAINES_TABLE);
        if(idCycle == null) {
            query.append(" LEFT JOIN notes.rel_groupe_cycle ON notes." + Field.DOMAINES_TABLE + ".id_cycle = notes.rel_groupe_cycle.id_cycle")
                    .append(" WHERE " + Field.DOMAINES_TABLE + ".evaluated = TRUE AND rel_groupe_cycle.id_groupe = ?) ");
            params.add(idClasse);
        }else {
            query.append(" WHERE " + Field.DOMAINES_TABLE + ".evaluated = TRUE AND " + Field.DOMAINES_TABLE + ".id_cycle = ?) ");
            params.add(idCycle);
        }

        //Puis on sélectionne les domaines dont le parent n'existe pas dans la requête précente,
        // c'est-à-dire un domaine racine (id_parent = 0) ou dont le domaine parent n'est pas évalué
        query.append("SELECT id, libelle, codification ")
                .append("FROM evaluated_domaines ")
                .append("WHERE id NOT IN ")
                .append("(SELECT t1.id ")
                .append("FROM evaluated_domaines AS t1, evaluated_domaines AS t2 ")
                .append("WHERE t1.id_parent = t2.id) ORDER BY codification;");

        Sql.getInstance().prepared(query.toString(), params, DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getDomaines(String idClasse, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();


        params.add(idClasse);
        String query = " SELECT id, " + Field.DOMAINES_TABLE + ".id_cycle, codification, libelle, type, evaluated, code_domaine " +
                " FROM " + Competences.COMPETENCES_SCHEMA + "." + Field.DOMAINES_TABLE +
                " INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle " +
                " ON " + Field.DOMAINES_TABLE + ".id_cycle = rel_groupe_cycle.id_cycle " +
                " WHERE  id_groupe = ? " +
                " AND evaluated = true " +
                " ORDER BY codification ";
        Sql.getInstance().prepared(query, params, DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }
}
