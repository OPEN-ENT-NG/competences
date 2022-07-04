package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.ArchiveService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import static fr.openent.competences.Competences.COMPETENCES_SCHEMA;


public class DefaultArchiveBulletinService implements ArchiveService {

    @Override
    public void getArchives(String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT * FROM "+ COMPETENCES_SCHEMA + "." + Competences.BULLETIN_ARCHIVE_TABLE +
                " WHERE id_etablissement = ? ";

        params.add(idStructure);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}