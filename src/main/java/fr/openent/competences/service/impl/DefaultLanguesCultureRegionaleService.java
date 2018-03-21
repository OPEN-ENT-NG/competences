package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.LanguesCultureRegionaleService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public class DefaultLanguesCultureRegionaleService extends SqlCrudService implements LanguesCultureRegionaleService {


    public DefaultLanguesCultureRegionaleService(String schema, String table) {
        super(schema, table);
    }


    public void getLanguesCultureRegionaleService(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, libelle, code FROM "+ Competences.COMPETENCES_SCHEMA + ".langues_culture_regionale";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }



}
