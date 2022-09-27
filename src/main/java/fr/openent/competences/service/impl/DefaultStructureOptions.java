package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.service.StructureOptionsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

public class DefaultStructureOptions extends SqlCrudService implements StructureOptionsService {

    public DefaultStructureOptions () {
        super(Competences.EVAL_SCHEMA, Field.STRUTUCTURE_OPTIONS);
    }

    @Override
    public void createOrUpdateIsAverageSkills (JsonObject body, Handler<Either<String, JsonObject>> handler) {
        final String structureId = body.getString(Field.STRUCTUREID);
        final boolean isAverageSkills = body.getBoolean(Field.ISSKILLAVERAGE);
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ").append(this.resourceTable)
                .append("(id_structure, is_average_skills)")
                .append("VALUES (?, ?)")
                .append("ON CONFLICT (id_structure) DO UPDATE SET is_average_skills = ? ");

        JsonArray values = new JsonArray().add(structureId).add(isAverageSkills).add(isAverageSkills);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getIsAverageSkills (String structureId, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT EXISTS (SELECT is_average_skills FROM ").append(this.resourceTable)
                .append(" WHERE id_structure = ? AND is_average_skills = TRUE) AS is_average_skills ");
        JsonArray params = new JsonArray().add(structureId);

        Sql.getInstance().prepared(query.toString(), params, Competences.DELIVERY_OPTIONS,
                validUniqueResultHandler(handler));
    }

    /**
     * @param structureId structure id
     * @return response
     */
    @Override
    public Future<Boolean> isAverageSkills (String structureId) {

        Promise<Boolean> promise = Promise.promise();
        getIsAverageSkills(structureId, event -> {
            if(event.isRight())
                promise.complete(event.right().getValue().getBoolean(Field.IS_AVERAGE_SKILLS)); // pour "is_average_skills"
            else
                promise.fail(event.left().getValue());
        });
        return promise.future();
    }

}
