package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import static fr.openent.competences.Competences.TRANSITION_CONFIG;


public class DefaultSyntheseBilanPeriodiqueService {

    public void createOrUpdateSyntheseBilanPeriodique (Long idTypePeriode, String idEleve,  String synthese, Handler<Either<String, JsonObject>> handler){
        if(synthese.length() == 0){
            deleteSynthese(idTypePeriode, idEleve, handler);
        }
        else {
          String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".synthese_bilan_periodique" +
                    "(id_typePeriode, id_eleve, synthese) VALUES (?, ?, ?)" +
                    "ON CONFLICT (id_typePeriode, id_eleve) DO UPDATE SET synthese = ?";
            JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
            values.add(idTypePeriode);
            values.add(idEleve);
            values.add(synthese);
            values.add(synthese);
            Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
        }

    }

    private void deleteSynthese (Long idTypePeriode, String idEleve, Handler<Either<String, JsonObject>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "";

            query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".synthese_bilan_periodique " +
                    " WHERE id_eleve = ?" +
                    " AND id_typePeriode = ? ";
        params.add(idEleve)
                .add(idTypePeriode);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void getSyntheseBilanPeriodique (Long idTypePeriode, String idEleve, Handler<Either<String, JsonObject>> handler) {

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "";

        query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA +".synthese_bilan_periodique " +
                "WHERE "+ Competences.COMPETENCES_SCHEMA +".synthese_bilan_periodique.id_eleve = ? " +
                "AND "+ Competences.COMPETENCES_SCHEMA +".synthese_bilan_periodique.id_typePeriode = ? ";

        params.add(idEleve);
        params.add(idTypePeriode);

        Sql.getInstance().prepared(query, params,
                new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG
                        .getInteger("timeout-transaction") * 1000L),
                SqlResult.validUniqueResultHandler(handler));
    }

}
