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
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.service.ElementProgramme;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.competences.Competences.TRANSITION_CONFIG;
import static org.entcore.common.sql.SqlResult.validResultHandler;

public class DefaultElementProgramme implements ElementProgramme {
    private final Sql sql = Sql.getInstance();

    @Override
    public void setElementProgramme(String userId, Long idPeriode, String idMatiere, String idClasse,String texte,
                                    Handler<Either<String, JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder()
                .append("INSERT INTO ").append(Competences.COMPETENCES_SCHEMA).append(".element_programme ")
                .append("(id_periode, id_matiere, id_classe, id_user_create, id_user_update, texte) VALUES ")
                .append("(?, ?, ?, ?, ?, ?) ")
                .append("ON CONFLICT (id_periode, id_matiere , id_classe) ")
                .append("DO UPDATE SET id_user_update = ?, texte = ? ");

        values.add(idPeriode).add(idMatiere).add(idClasse).add(userId).add(userId).add(texte).add(userId).add(texte);

        sql.prepared(query.toString(), values, validResultHandler(handler));
    }


    @Override
    public void getElementProgramme(Long idPeriode, String idMatiere, String idClasse,
                                    Handler<Either<String, JsonObject>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT * ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".element_programme ")
                .append("WHERE "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_classe = ? ")
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_periode = ? ")
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_matiere = ? ");

        values.add(idClasse);
        values.add(idPeriode);
        values.add(idMatiere);

        sql.prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

    public Future<JsonObject> getElementProgramme(Long idPeriode, String idMatiere, String idClasse){
        Promise<JsonObject> promiseElementProgramme = Promise.promise();
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT * ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".element_programme ")
                .append("WHERE "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_classe = ? ")
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_periode = ? ")
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_matiere = ? ");

        values.add(idClasse);
        values.add(idPeriode);
        values.add(idMatiere);
        sql.prepared(query.toString(), values,
                SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(promiseElementProgramme, "[DefaultElementProgramme] : getElementProgramme :")));

        return promiseElementProgramme.future();
    }

    @Override
    public void getElementProgrammeClasses(Long idPeriode, String idMatiere, JsonArray idsClasse,
                                           Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT texte, id_classe ")
                .append("FROM ").append(Competences.COMPETENCES_SCHEMA).append(".element_programme ")
                .append("WHERE ").append(Competences.COMPETENCES_SCHEMA).append(".element_programme.id_classe IN ").append(Sql.listPrepared(idsClasse.getList()))
                .append("AND ").append(Competences.COMPETENCES_SCHEMA).append(".element_programme.id_periode = ? ")
                .append("AND ").append(Competences.COMPETENCES_SCHEMA).append(".element_programme.id_matiere = ? ");

        for(Object idClasse : idsClasse){
            values.add(idClasse);
        }

        values.add(idPeriode);
        values.add(idMatiere);

        sql.prepared(query.toString(), values, Competences.DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getDomainesEnseignement(String idCycle, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT * FROM " + Competences.COMPETENCES_SCHEMA + ".domaine_enseignement ");
        if(idCycle != null){
            query.append("WHERE id_cycle = ? ");
            values.add(idCycle);
        }
        query.append("ORDER BY libelle");

        sql.prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void getSousDomainesEnseignement(String idDomaine, Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT * FROM " + Competences.COMPETENCES_SCHEMA +".sous_domaine_enseignement ");
        if(idDomaine != null){
            query.append("WHERE id_domaine = ? ");
            values.add(idDomaine);
        }
        query.append("ORDER BY libelle");

        sql.prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void getPropositions(String idStructure, Long idSousDomaine, Handler<Either<String, JsonArray>> handler){
        String query = "SELECT * FROM " + Competences.COMPETENCES_SCHEMA + ".proposition" +
                " WHERE (id_etablissement = ? OR id_etablissement IS NULL) AND id_sous_domaine = ?" +
                " ORDER BY libelle";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(idStructure);
        values.add(idSousDomaine);
        sql.prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void createProposition(String idStructure, String libelle, Long idSousDomaine,
                                  Handler<Either<String, JsonArray>> handler){
        String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".proposition " +
                "(libelle, id_sous_domaine, id_etablissement) SELECT ?, ?, ? " +
                "WHERE NOT EXISTS (SELECT id FROM " + Competences.COMPETENCES_SCHEMA + ".proposition " +
                "WHERE libelle = ? AND id_sous_domaine = ? AND id_etablissement = ?)";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(libelle);
        values.add(idSousDomaine);
        values.add(idStructure);
        values.add(libelle);
        values.add(idSousDomaine);
        values.add(idStructure);
        sql.prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void editProposition(Long idProposition, String libelle, Handler<Either<String, JsonArray>> handler){
        String query = "UPDATE " + Competences.COMPETENCES_SCHEMA + ".proposition " +
                "SET libelle = ? WHERE id = ?";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(libelle);
        values.add(idProposition);
        sql.prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void deleteProposition(Long idProposition, Handler<Either<String, JsonArray>> handler){
        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".proposition " +
                "WHERE id = ?";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(idProposition);
        sql.prepared(query.toString(), values, validResultHandler(handler));
    }
}
