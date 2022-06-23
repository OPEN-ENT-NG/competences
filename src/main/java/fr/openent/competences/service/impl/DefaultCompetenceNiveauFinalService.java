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
import fr.openent.competences.service.CompetenceNiveauFinalService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.user.UserInfos;

import java.util.List;

import static org.entcore.common.sql.SqlResult.*;

public class DefaultCompetenceNiveauFinalService extends SqlCrudService implements CompetenceNiveauFinalService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultCompetenceNiveauFinalService.class);

    public DefaultCompetenceNiveauFinalService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void setNiveauFinal(JsonObject niveauFinal,  Handler<Either<String, JsonObject>> handler) {

        SqlStatementsBuilder sqlBuilder =new SqlStatementsBuilder();
        JsonArray idsMatieres = niveauFinal.getJsonArray("ids_matieres");

        String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + "." + Field.COMPETENCE_NIVEAU_FINAL
                + " (id_periode, id_eleve, niveau_final, id_competence, id_matiere) VALUES(?, ?, ?, ?, ?)"
                + " ON CONFLICT (id_periode, id_eleve, id_competence, id_matiere)"
                + " DO UPDATE SET niveau_final = ?";

        for ( int i = 0; i < idsMatieres.size(); i++ ) {
            JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
            values.add(niveauFinal.getInteger("id_periode"))
                    .add(niveauFinal.getString("id_eleve"))
                    .add(niveauFinal.getInteger("niveau_final"))
                    .add(niveauFinal.getInteger("id_competence"))
                    .add(idsMatieres.getString(i))
                    .add(niveauFinal.getInteger("niveau_final"));
            sqlBuilder.prepared(query,values);

        }
        Sql.getInstance().transaction(sqlBuilder.build(), SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void setNiveauFinalAnnuel(JsonObject niveauFinal,  Handler<Either<String, JsonObject>> handler) {

        SqlStatementsBuilder sqlBuilder =new SqlStatementsBuilder();
        JsonArray idsMatieres = niveauFinal.getJsonArray("ids_matieres");

        String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + "." + Field.COMPETENCE_NIVEAU_FINAL_ANNUEL
                + " (id_eleve, niveau_final, id_competence, id_matiere) VALUES( ?, ?, ?, ?)"
                + " ON CONFLICT (id_eleve, id_competence, id_matiere)"
                + " DO UPDATE SET niveau_final = ?";

        for ( int i = 0; i < idsMatieres.size(); i++ ) {
            JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
              values.add(niveauFinal.getString("id_eleve"))
                    .add(niveauFinal.getInteger("niveau_final"))
                    .add(niveauFinal.getInteger("id_competence"))
                    .add(idsMatieres.getString(i))
                    .add(niveauFinal.getInteger("niveau_final"));
            sqlBuilder.prepared(query,values);

        }
        Sql.getInstance().transaction(sqlBuilder.build(), SqlResult.validRowsResultHandler(handler));
    }



    @Override
    public void deleteNiveauFinal(JsonObject niveauFinal, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        String query = "DELETE FROM "+ Competences.COMPETENCES_SCHEMA + "." +Field.COMPETENCE_NIVEAU_FINAL
                + " WHERE id_periode = ? AND id_eleve = ? AND id_competence = ? AND id_matiere = ? AND id_classe = ? ";

        values.add(niveauFinal.getInteger("id_periode")).add(niveauFinal.getString("id_eleve"))
                .add(niveauFinal.getInteger("niveau_final")).add(niveauFinal.getInteger("id_competence"))
                .add(niveauFinal.getString("id_matiere")).add(niveauFinal.getString("id_classe"));


        Sql.getInstance().prepared(query, values, validRowsResultHandler(handler));
    }

    @Override
    public void getNiveauFinalByEleve(Long id_periode, String id_eleve, List<String> ids_matieres, String id_classe, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA + "." + Field.COMPETENCE_NIVEAU_FINAL
                + " WHERE id_periode = ? AND id_eleve = ? AND id_matiere IN "+ Sql.listPrepared(ids_matieres) +" AND id_classe = ? ";

        values.add(id_periode).add(id_eleve);
        for(String id_matiere: ids_matieres){
            values.add(id_matiere);
        }
        values.add(id_classe);

        Sql.getInstance().prepared(query, values, validResultHandler(handler));
    }
}
