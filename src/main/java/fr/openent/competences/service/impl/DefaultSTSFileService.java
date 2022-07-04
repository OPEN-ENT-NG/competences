package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.STSFileService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.JsonArray;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import static fr.openent.competences.Competences.COMPETENCES_SCHEMA;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

public class DefaultSTSFileService extends SqlCrudService implements STSFileService {


    public DefaultSTSFileService (String table) {
        super(COMPETENCES_SCHEMA, Competences.STSFILE_TABLE);
    }

    @Override
    public void create (JsonObject oSTSFile, Handler<Either<String, JsonObject>> handler) {

        String colums = " id_etablissement, name_file, content ";

        JsonArray values = (JsonArray) new JsonArray()
                .add( oSTSFile.getString("id_structure"))
                .add( oSTSFile.getString("name_file"))
                .add( oSTSFile.getString("content"));

        String query = "INSERT INTO " + this.resourceTable + "(" + colums
                + ") VALUES ( ?, ?, ? ) RETURNING id, creation_date;";

        Sql.getInstance().prepared(query,values, validUniqueResultHandler(handler));
    }

    @Override
    public void getSTSFile (String id_etablissement, Handler<Either<String, io.vertx.core.json.JsonArray>> handler) {

        String query = "SELECT * FROM " + this.resourceTable +
                " WHERE id_etablissement = ? ORDER BY creation_date DESC LIMIT 10";

        JsonArray values = (JsonArray) new JsonArray()
                .add(id_etablissement);

        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));

    }


}
