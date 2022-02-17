package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.BulletinsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;


public class DefaultBulletinsService implements BulletinsService {

    @Override
    public void getBulletinsCount(String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT * FROM "+ Competences.EVAL_SCHEMA + "." + Competences.BULLETIN_ARCHIVE_TABLE +
                " WHERE id_etablissement = ? ";

        params.add(idStructure);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}