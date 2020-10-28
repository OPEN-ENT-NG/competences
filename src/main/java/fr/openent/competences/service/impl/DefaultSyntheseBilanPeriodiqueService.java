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

    public void createOrUpdateSyntheseBilanPeriodique (Long idTypePeriode, String idEleve, String idStructure,
                                                       String synthese, Handler<Either<String, JsonObject>> handler){
        if(synthese.length() == 0){
            deleteSynthese(idTypePeriode, idEleve, idStructure, handler);
        }
        else {
            String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".synthese_bilan_periodique" +
                    "(id_typePeriode, id_eleve, synthese, id_etablissement) VALUES (?, ?, ?, ?)" +
                    "ON CONFLICT (id_typePeriode, id_eleve, id_etablissement) DO UPDATE SET synthese = ?";
            JsonArray values = new fr.wseduc.webutils.collections.JsonArray().add(idTypePeriode).add(idEleve)
                    .add(synthese).add(idStructure).add(synthese);
            Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
        }
    }

    private void deleteSynthese (Long idTypePeriode, String idEleve, String idStructure,
                                 Handler<Either<String, JsonObject>> handler){
        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".synthese_bilan_periodique " +
                " WHERE id_eleve = ? AND id_typePeriode = ? AND id_etablissement = ?";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(idEleve)
                .add(idTypePeriode).add(idStructure);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void getSyntheseBilanPeriodique(Long idTypePeriode, String idEleve, String idStructure,
                                           Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Competences.COMPETENCES_SCHEMA + ".synthese_bilan_periodique " +
                "WHERE id_eleve = ? AND id_etablissement = ? ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(idEleve).add(idStructure);

        if(idTypePeriode != null){
            query += "AND id_typePeriode = ?";
            params.add(idTypePeriode);
        }

        Sql.getInstance().prepared(query, params, new DeliveryOptions().setSendTimeout(TRANSITION_CONFIG
                .getInteger("timeout-transaction") * 1000L), SqlResult.validResultHandler(handler));
    }
}
