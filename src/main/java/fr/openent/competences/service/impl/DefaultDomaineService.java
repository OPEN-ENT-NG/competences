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
import fr.openent.competences.service.DomainesService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public class DefaultDomaineService extends SqlCrudService implements DomainesService {
    public DefaultDomaineService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void getArbreDomaines(String idClasse, String idEleve, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new JsonArray();


        query.append("WITH RECURSIVE search_graph(niveau, id, id_parent, libelle, codification, ")
                .append(" evaluated, pathinfo, id_cycle, dispensable) AS ")
                .append(" ( ")
                .append(" SELECT 1 as niveau, id, id_parent, libelle, codification, evaluated, array[id] as pathinfo ")
                .append(" , id_cycle, dispensable")
                .append(" FROM "+ Competences.COMPETENCES_SCHEMA +".domaines ")
                .append(" WHERE id_parent = 0 ");

        if(null != idClasse) {
            query.append(" AND id_cycle = (SELECT id_cycle FROM " + Competences.COMPETENCES_SCHEMA)
                    .append(".rel_groupe_cycle WHERE id_groupe = ?) ");
        }
        query.append("UNION ")
                .append(" SELECT sg.niveau + 1  as niveau , dom.id, dom.id_parent, dom.libelle, ")
                .append(" dom.codification, dom.evaluated, sg.pathinfo||dom.id, dom.id_cycle, dom.dispensable ")
                .append(" FROM "+ Competences.COMPETENCES_SCHEMA +".domaines dom , search_graph sg ")
                .append(" WHERE dom.id_parent = sg.id ")
                .append(") ")
                .append(" SELECT niveau, id, id_parent, libelle, codification, evaluated, id_cycle, dispensable, " )
                .append(" libelle as nom, libelle as nomHtml " );
        if(idEleve != null){
            query.append(", dispense_domaine_eleve.dispense as dispense_eleve ");
        }
        query .append("FROM search_graph");
        if(idEleve !=null){
            query.append(" LEFT JOIN notes.dispense_domaine_eleve  ON search_graph.id = dispense_domaine_eleve.id_domaines ")
                    .append("AND dispense_domaine_eleve.id_eleve = ?");
        }
        query.append(" GROUP BY niveau, id, id_parent, libelle, codification, evaluated, id_cycle, dispensable, nom, nomHtml, pathinfo");
        if(idEleve !=null){
            query.append(", dispense_eleve");
        }
        query.append(" ORDER BY pathinfo, id_cycle");

        if(null != idClasse) {
            params.addString(idClasse);
        }
        if(null != idEleve){
            params.addString(idEleve);
        }
        Sql.getInstance().prepared(query.toString(), params , SqlResult.validResultHandler(handler));
    }

    @Override
    public void getDomainesLibCod(int[] idDomaines, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new JsonArray();


        query.append("SELECT id, id_parent, libelle, codification ");
        query.append("FROM notes.domaines ");
        query.append("WHERE id IN " + listIntPrepared(idDomaines));

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
    public void getDomainesRacines(String idClasse, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray params = new JsonArray();

        //On extrait d'abord les domaines evalués et dont le cycle correspond à celui de la classe
        query.append("WITH evaluated_domaines AS ")
                .append("(SELECT id, id_parent, libelle, codification ")
                .append("FROM notes.domaines ")
                .append("LEFT JOIN notes.rel_groupe_cycle ON notes.domaines.id_cycle = notes.rel_groupe_cycle.id_cycle")
                .append(" WHERE domaines.evaluated = TRUE AND rel_groupe_cycle.id_groupe = ?) ");
        params.addString(idClasse);

        //Puis on sélectionne les domaines dont le parent n'existe pas dans la requête précente,
        // c'est-à-dire un domaine racine (id_parent = 0) ou dont le domaine parent n'est pas évalué
        query.append("SELECT id, libelle, codification ")
                .append("FROM evaluated_domaines ")
                .append("WHERE id NOT IN ")
                .append("(SELECT t1.id ")
                .append("FROM evaluated_domaines AS t1, evaluated_domaines AS t2 ")
                .append("WHERE t1.id_parent = t2.id) ORDER BY codification;");

        Sql.getInstance().prepared(query.toString(), params , SqlResult.validResultHandler(handler));
    }
}
