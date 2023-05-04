package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.service.SyntheseBilanPeriodiqueService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.Collections;
import java.util.List;


public class DefaultSyntheseBilanPeriodiqueService implements SyntheseBilanPeriodiqueService {

    @Override
    public void createOrUpdateSyntheseBilanPeriodique(Long idTypePeriode, String idEleve, String idStructure,
                                                      String synthese, Handler<Either<String, JsonObject>> handler) {
        if (synthese.length() == 0) {
            deleteSynthese(idTypePeriode, idEleve, idStructure, handler);
        } else {
            String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".synthese_bilan_periodique" +
                    "(id_typePeriode, id_eleve, synthese, id_etablissement) VALUES (?, ?, ?, ?)" +
                    "ON CONFLICT (id_typePeriode, id_eleve, id_etablissement) DO UPDATE SET synthese = ?";
            JsonArray values = new fr.wseduc.webutils.collections.JsonArray().add(idTypePeriode).add(idEleve)
                    .add(synthese).add(idStructure).add(synthese);
            Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
        }
    }

    private void deleteSynthese(Long idTypePeriode, String idEleve, String idStructure,
                                Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".synthese_bilan_periodique " +
                " WHERE id_eleve = ? AND id_typePeriode = ? AND id_etablissement = ?";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(idEleve)
                .add(idTypePeriode).add(idStructure);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getSyntheseBilanPeriodique(Long idTypePeriode, String idEleve, String idStructure,
                                           Handler<Either<String, JsonArray>> handler) {
        getPeriodicReportSummaries(idTypePeriode, Collections.singletonList(idEleve), idStructure, handler);
    }

    @Override
    public Future<JsonArray> getPeriodicReportSummaries(Long periodTypeId, List<String> studentIds, String structureId) {
        Promise<JsonArray> promise = Promise.promise();
        getPeriodicReportSummaries(periodTypeId, studentIds, structureId, FutureHelper.handler(promise));
        return promise.future();
    }

    private void getPeriodicReportSummaries(Long periodTypeId, List<String> studentIds, String structureId,
                                            Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + String.format("%s.%s", Field.NOTES_TABLE, Field.SYNTHESE_BILAN_PERIODIQUE_TABLE) +
                String.format(" WHERE id_eleve IN %s AND id_etablissement = ? ", Sql.listPrepared(studentIds));

        JsonArray params = new JsonArray()
                .addAll(new JsonArray(studentIds))
                .add(structureId);

        if (periodTypeId != null) {
            query += "AND id_typePeriode = ?";
            params.add(periodTypeId);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

}
