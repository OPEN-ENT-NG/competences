package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultAppreciationCPEService {
    public void createOrUpdateAppreciationCPE (Long idPeriode, String idEleve,  String appreciation, Handler<Either<String, JsonObject>> handler){
        if(appreciation.length() == 0){
            deleteAppreciationCPE(idPeriode, idEleve, handler);
        }
        else {
            String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".appreciation_CPE_bilan_periodique " +
                    "(id_periode, id_eleve, appreciation) VALUES (?, ?, ?) " +
                    "ON CONFLICT (id_periode, id_eleve) DO UPDATE SET appreciation = ? ";
            JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
            values.add(idPeriode);
            values.add(idEleve);
            values.add(appreciation);
            values.add(appreciation);
            Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
        }
    }

    private void deleteAppreciationCPE (Long idPeriode, String idEleve, Handler<Either<String, JsonObject>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "";

        query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_CPE_bilan_periodique " +
                "WHERE id_eleve = ? " +
                "AND id_periode = ? ";
        params.add(idEleve)
                .add(idPeriode);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void getAppreciationCPE (Long idPeriode, String idEleve, Handler<Either<String, JsonObject>> handler) {

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "";

        query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA + ".appreciation_CPE_bilan_periodique " +
                "WHERE "+ Competences.COMPETENCES_SCHEMA + ".appreciation_CPE_bilan_periodique.id_eleve = ? " +
                "AND "+ Competences.COMPETENCES_SCHEMA + ".appreciation_CPE_bilan_periodique.id_periode = ? ";

        params.add(idEleve);
        params.add(idPeriode);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
