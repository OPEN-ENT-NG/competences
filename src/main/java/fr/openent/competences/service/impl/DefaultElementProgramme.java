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
import fr.openent.competences.service.ElementProgramme;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.sql.SqlResult.validResultHandler;

public class DefaultElementProgramme implements ElementProgramme {

    @Override
    public void setElementProgramme(String userId, Long idPeriode, String idMatiere, String idClasse,String texte, Handler<Either<String, JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder()
                .append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".element_programme ")
                .append(" (id_periode, id_matiere , id_classe, id_user_create, id_user_update, texte) VALUES ")
                .append(" (?, ?, ?, ?, ?, ?) ")
                .append(" ON CONFLICT (id_periode, id_matiere , id_classe) ")
                .append(" DO UPDATE SET id_user_update = ? , texte = ? ");

        values.add(idPeriode).add(idMatiere).add(idClasse).add(userId).add(userId).add(texte);
        values.add(userId).add(texte);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }


    @Override
    public void getElementProgramme(Long idPeriode, String idMatiere, String idClasse, Handler<Either<String, JsonObject>> handler){
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

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getElementProgrammeClasses(Long idPeriode, String idMatiere, JsonArray idsClasse, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT element_programme.texte ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".element_programme ")
                .append("WHERE "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_classe IN " + Sql.listPrepared(idsClasse.getList()))
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_periode = ? ")
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_matiere = ? ");

        for(Object idClasse : idsClasse){
            values.add(idClasse);
        }

        values.add(idPeriode);
        values.add(idMatiere);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getDomainesEnseignement(Handler<Either<String, JsonArray>> handler){
        String query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA +".domaine_enseignement ORDER BY libelle";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void getSousDomainesEnseignement(Handler<Either<String, JsonArray>> handler){
        String query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA +".sous_domaine_enseignement ORDER BY libelle";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void getPropositions(Handler<Either<String, JsonArray>> handler){
        String query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA +".proposition ORDER BY libelle";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

}
