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
import fr.openent.competences.service.EleveEnseignementComplementService;
import fr.wseduc.webutils.Either;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.competences.Competences.DELIVERY_OPTIONS;
import static fr.openent.competences.Competences.MESSAGE;
import static fr.openent.competences.Utils.isNotNull;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.TIME;

/**
 * Created by agnes.lapeyronnie on 28/11/2017.
 */
public class DefaultEleveEnseignementComplementService extends SqlCrudService implements EleveEnseignementComplementService {

    public DefaultEleveEnseignementComplementService(String schema, String table){
        super(schema,table);

    }

    public void createEnsCplByELeve(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler){
        super.create(data, user, handler);
    }

    @Override
    public void updateEnsCpl(Integer id, JsonObject data, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        String query = "UPDATE " + Competences.COMPETENCES_SCHEMA + ".eleve_enseignement_complement " +
                "SET id_enscpl = ?, id_niveau = ?, id_langue = ?, niveau_lcr = ? " +
                "WHERE id = ?";

        values.add(data.getLong("id_enscpl"));
        values.add(data.getLong("id_niveau"));
        values.add(data.getLong("id_langue"));
        values.add(data.getLong("niveau_lcr"));
        values.add(id);

        Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getNiveauEnsCplByEleve(String idEleve, Long idCycle, Handler<Either<String, JsonObject>> handler) {
        JsonArray values =  new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT eleve_enseignement_complement.*, enseignement_complement.libelle " +
                "FROM " + Competences.COMPETENCES_SCHEMA + ".eleve_enseignement_complement " +
                "INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".enseignement_complement ON enseignement_complement.id = eleve_enseignement_complement.id_enscpl " +
                "WHERE id_eleve = ?" ;

        values.add(idEleve);
        if(idCycle != null) {
            query = query + " AND id_cycle = ?";
            values.add(idCycle);
        }

        Sql.getInstance().prepared(query,values, Competences.DELIVERY_OPTIONS,
                SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void listNiveauCplByEleves( final String[] idsEleve, final  Handler<Either<String, JsonArray>> handler) {
        JsonArray valuesCount = new fr.wseduc.webutils.collections.JsonArray();
        String queryCount = "SELECT count(*) FROM " +
                Competences.COMPETENCES_SCHEMA + ".eleve_enseignement_complement INNER JOIN " +
                Competences.COMPETENCES_SCHEMA +".enseignement_complement " +
                " ON notes.eleve_enseignement_complement.id_enscpl = notes.enseignement_complement.id " +
                " WHERE id_eleve IN "+ Sql.listPrepared(idsEleve);
        for(String idEleve : idsEleve){
            valuesCount.add(idEleve);
        }

        Sql.getInstance().prepared(queryCount, valuesCount, DELIVERY_OPTIONS,  sqlResultCount ->  {
            Long nbEnsCpl = SqlResult.countResult(sqlResultCount);
            if(isNotNull(nbEnsCpl) && nbEnsCpl > 0){
                String query = "SELECT langues_culture_regionale.code as code_lcr, " +
                        " langues_culture_regionale.libelle as libelle_lcr ,eleve_enseignement_complement.*, " +
                        " enseignement_complement.libelle,enseignement_complement.code, niveau_ens_complement.niveau " +
                        " FROM " + Competences.COMPETENCES_SCHEMA + ".eleve_enseignement_complement" +
                        " INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".enseignement_complement " +
                        "    ON notes.eleve_enseignement_complement.id_enscpl = notes.enseignement_complement.id " +
                        " INNER JOIN "+ Competences.COMPETENCES_SCHEMA + ".niveau_ens_complement " +
                        "    ON niveau_ens_complement.id = eleve_enseignement_complement.id_niveau "+

                        "LEFT JOIN "+ Competences.COMPETENCES_SCHEMA + ".langues_culture_regionale " +
                        "    ON eleve_enseignement_complement.id_langue = langues_culture_regionale.id "+

                        " WHERE id_eleve IN " + Sql.listPrepared(idsEleve);
                JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
                for (String idEleve : idsEleve) {
                    values.add(idEleve);
                }

                Sql.getInstance().prepared(query, values, DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
            }else{
                String error =  sqlResultCount.body().getString(MESSAGE);
                // log.error("listNiveauCplByEleves " + error);
                if(error.contains(TIME)){
                    listNiveauCplByEleves(idsEleve, handler);
                    return;
                }
                handler.handle(new Either.Right<>(new fr.wseduc.webutils.collections.JsonArray()) );
            }
        });
    }
}
