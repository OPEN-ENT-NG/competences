package fr.openent.competences.service.digitalSkills.impl;

import fr.openent.competences.service.digitalSkills.ClassAppreciationDigitalSkillsService;
import fr.openent.competences.service.impl.DefaultAppreciationSubjectPeriod;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

import static fr.openent.competences.Competences.DELIVERY_OPTIONS;
import static org.entcore.common.sql.SqlResult.validResultHandler;

public class DefaultClassAppreciationDigitalSkills extends SqlCrudService implements ClassAppreciationDigitalSkillsService {
    private static final Logger log = LoggerFactory.getLogger(DefaultClassAppreciationDigitalSkills.class);

    public DefaultClassAppreciationDigitalSkills(String schema,String table){
        super(schema,table);
    }

    @Override
    public void createOrUpdateClassAppreciation (final JsonObject classApp, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = setValue(classApp);
        query.append("INSERT INTO ").append(this.schema).append(this.table)
                .append(" (class_or_group_id, type_structure, appreciation) VALUES (?, ?, ?) ")
                .append("ON CONFLICT (class_or_group_id) DO UPDATE SET appreciation = ? ")
                .append("RETURNING id");

        Sql.getInstance().prepared(query.toString(),values, SqlResult.validUniqueResultHandler(handler));
    }

    private JsonArray setValue(JsonObject classApp){
        return new JsonArray().add(classApp.getString("id_class"))
                .add(classApp.getString("id_type_structure"))
                .add(classApp.getString("appreciation"))
                .add(classApp.getString("appreciation"));
    }

    @Override
    public void deleteClassAppreciation(final Long idClassAppreciation, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ").append(this.schema).append(this.table)
                .append(" WHERE id = ?");

        Sql.getInstance().prepared(query.toString(), new JsonArray().add(idClassAppreciation),
                SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getClassAppreciation(final String id_class, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ").append(this.schema).append(this.table)
                .append(" WHERE class_or_group_id = ?");

        JsonArray values = new JsonArray().add(id_class);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getAppreciationsClasses(List<String> listIdsClass, Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ").append(this.schema).append(this.table)
                .append(" WHERE class_or_group_id IN ").append(Sql.listPrepared(listIdsClass));

        JsonArray values = new JsonArray();
        for(String idClass: listIdsClass){
            values.add(idClass);
        }

        Sql.getInstance().prepared(query.toString(), values, DELIVERY_OPTIONS, SqlResult.validResultHandler(handler));
    }

}
