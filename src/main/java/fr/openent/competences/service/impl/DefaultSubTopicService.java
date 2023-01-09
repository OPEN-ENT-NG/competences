package fr.openent.competences.service.impl;

import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.service.SubTopicService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.competences.constants.Field.*;
import static org.entcore.common.sql.SqlResult.validResultHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

public class DefaultSubTopicService extends SqlCrudService implements SubTopicService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultSubTopicService.class);

    public DefaultSubTopicService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void upsertCoefficent(JsonObject data, Handler<Either<String, JsonArray>> handler) {
        List<String> groups = data.getJsonArray(GROUPS)
                .stream().map(group -> ((JsonObject)group).getString(ID)).collect(Collectors.toList());
        JsonArray statements = new JsonArray();
        groups.forEach(idGroup -> statements.add(setStatementCoefficient(data,idGroup)));
        Sql.getInstance().transaction(statements,validResultHandler(handler));
    }

    private JsonObject setStatementCoefficient(JsonObject data, String idGroup) {
        String statement = "INSERT INTO " + this.resourceTable +
                "  (coefficient ,id_subtopic, id_teacher, id_topic, id_structure, id_group) " +
                "VALUES (? , ? , ?, ?, ?, ?)" +
                " ON CONFLICT (id_teacher, id_topic, id_group,id_subtopic) DO UPDATE SET coefficient = ?";

        JsonArray params = new JsonArray().add(Double.parseDouble(data.getValue("coefficient").toString()))
                .add(data.getInteger(ID_SUBTOPIC))
                .add(data.getString(ID_TEACHER))
                .add(data.getString(ID_TOPIC))
                .add(data.getString(ID_STRUCTURE))
                .add(idGroup)
                .add(Double.parseDouble(data.getValue(COEFFICIENT).toString()));

        return new JsonObject().put(STATEMENT, statement)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    @Override
    public Future<JsonArray> getSubtopicServices(String idStructure) {
        Promise<JsonArray> promise = Promise.promise();
        String query = "SELECT  id_subtopic, id_teacher, id_topic, id_group, coefficient::numeric, id_structure " +
                " From " + this.resourceTable + " WHERE id_structure = ? ";
        JsonArray params = new JsonArray().add(idStructure);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise,
                String.format("[Competences@%s::getSubtopicServices(String)] Fail to get subtopics.",
                        this.getClass().getSimpleName()))));
        return promise.future();
    }

    @Override
    public Future<JsonArray> getSubtopicServices(String idStructure, String idClasse) {
        Promise<JsonArray> promise = Promise.promise();
        String query = "SELECT  id_subtopic, id_teacher, id_topic, id_group, coefficient::numeric, id_structure " +
                " From " + this.resourceTable + " WHERE id_structure = ? AND id_group = ? ";
        JsonArray params = new JsonArray().add(idStructure).add(idClasse);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise,
                String.format("[Competences@%s::getSubtopicServices(String, String)] Fail to get subtopics.",
                        this.getClass().getSimpleName()))));
        return promise.future();
    }

    @Override
    public Future<JsonArray> getSubtopicServices(String idStructure, JsonArray idsClasse) {
        Promise<JsonArray> promise = Promise.promise();
        String query = "SELECT  id_subtopic, id_teacher, id_topic, id_group, coefficient::numeric, id_structure " +
                " From " + this.resourceTable + " WHERE id_structure = ? AND id_group IN " + Sql.listPrepared(idsClasse);
        JsonArray params = new JsonArray().add(idStructure).addAll(idsClasse);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise,
                String.format("[Competences@%s::getSubtopicServices(String, JsonArray)] Fail to get subtopics.",
                        this.getClass().getSimpleName()))));
        return promise.future();
    }

    @Override
    public Future<JsonObject> getSubtopicServices(String idStructure, String idClasse, String idTeacher, String idMatiere) {
        Promise<JsonObject> promise = Promise.promise();
        String query = "SELECT  id_subtopic, id_teacher, id_topic, id_group, coefficient::numeric, id_structure " +
                " From " + this.resourceTable + " WHERE id_structure = ? AND id_group = ? AND id_teacher = ? AND id_topic = ?";
        JsonArray params = new JsonArray().add(idStructure).add(idClasse).add(idTeacher).add(idMatiere);
        Sql.getInstance().prepared(query,params, validUniqueResultHandler(event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
            } else {
                log.error(String.format("[DefaultUtilsService@%s::getSubtopicServices] Error during request : %s.",
                        this.getClass().getSimpleName(), event.left().getValue()));
                promise.fail(event.left().getValue());
            }
        }));
        return promise.future();

    }

    @Override
    public void deleteSubtopicServices(String idMatiere, String idEnseignant, JsonArray idGroups, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE FROM " + this.resourceTable +
                " WHERE id_topic = ? " +
                " AND id_teacher = ? " +
                " AND id_group = ? ";
        JsonArray params = new JsonArray().add(idMatiere).add(idEnseignant).addAll(idGroups);
        Sql.getInstance().prepared(query, params, validResultHandler(handler));

    }
}
