package fr.openent.competences.service.impl;

import fr.openent.competences.service.SubTopicService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;

import java.util.List;
import java.util.stream.Collectors;

import static org.entcore.common.sql.SqlResult.validResultHandler;

public class DefaultSubTopicService extends SqlCrudService implements SubTopicService {

    public DefaultSubTopicService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void upsertCoefficient(JsonObject data, Handler<Either<String, JsonArray>> handler) {
         List<String> groups = data.getJsonArray("groups")
                .stream().map(group -> ((JsonObject)group).getString("id")).collect(Collectors.toList());
        JsonArray statements = new JsonArray();
        for(String idGroup: groups){
            statements.add(setStatementCoefficient(data,idGroup));
        }
        Sql.getInstance().transaction(statements,validResultHandler(handler));
    }

    private JsonObject setStatementCoefficient(JsonObject data, String idGroup) {
        String statement = "INSERT INTO " + this.resourceTable +
                "  (coefficient ,id_subtopic, id_teacher, id_topic, id_structure, id_group) " +
                "VALUES (? , ? , ?, ?, ?, ?)" +
                " ON CONFLICT (id_teacher, id_topic, id_group,id_subtopic) DO UPDATE SET coefficient = ?";

        JsonArray params = new JsonArray().add(Double.parseDouble(data.getValue("coefficient").toString()))
                .add(data.getInteger("id_subtopic"))
                .add(data.getString("id_teacher"))
                .add(data.getString("id_topic"))
                .add(data.getString("id_structure"))
                .add(idGroup)
                .add(Double.parseDouble(data.getValue("coefficient").toString()));

        return new JsonObject().put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    @Override
    public void getSubtopicServices(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT  id_subtopic, id_teacher, id_topic, id_group, coefficient::numeric, id_structure " +
                " From " + this.resourceTable + " WHERE id_structure = ? ";
        JsonArray params = new JsonArray().add(idStructure);
        Sql.getInstance().prepared(query,params,validResultHandler(handler));
    }
}
