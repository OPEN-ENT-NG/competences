package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.AvisOrientationService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultAvisOrientationService implements AvisOrientationService {

    public void createOrUpdateAvisOrientation (String idEleve, Long idPeriode,  Long id_avis_conseil_bilan, Handler<Either<String, JsonObject>> handler){
            String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".avis_conseil_orientation " +
                    "(id_eleve, id_periode, id_avis_conseil_bilan) VALUES (?, ?, ?) " +
                    "ON CONFLICT (id_eleve, id_periode) DO UPDATE SET id_avis_conseil_bilan = ? ";
            JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
            values.add(idEleve);
            values.add(idPeriode);
            values.add(id_avis_conseil_bilan);
            values.add(id_avis_conseil_bilan);
            Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
        }

    public void deleteAvisOrientation (Long idTypePeriode, String idEleve, Handler<Either<String, JsonObject>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "";

        query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".avis_conseil_orientation " +
                " WHERE id_eleve = ?" +
                " AND id_periode = ? ";
        params.add(idEleve)
                .add(idTypePeriode);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void getAvisOrientation (String idEleve, Long idPeriode, Handler<Either<String, JsonObject>> handler) {

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "";

        query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA + ".avis_conseil_orientation " +
                "INNER JOIN "+ Competences.COMPETENCES_SCHEMA + ".avis_conseil_bilan_periodique " +
                "ON(avis_conseil_bilan_periodique.id = avis_conseil_orientation.id_avis_conseil_bilan)  " +
                "WHERE "+ Competences.COMPETENCES_SCHEMA + ".avis_conseil_orientation.id_eleve = ? " +
                "AND "+ Competences.COMPETENCES_SCHEMA + ".avis_conseil_orientation.id_periode = ? ";

        params.add(idEleve);
        params.add(idPeriode);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }


}
