package fr.openent.competences.service.digitalSkills.impl;

import fr.openent.competences.service.digitalSkills.StudentAppreciationDigitalSkillsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultStudentAppreciationDigitalSkills extends SqlCrudService implements StudentAppreciationDigitalSkillsService {
    private static final Logger log = LoggerFactory.getLogger(DefaultStudentAppreciationDigitalSkills.class);

    public DefaultStudentAppreciationDigitalSkills(String schema, String table){ super(schema, table);}

    @Override
    public void createOrUpdateStudentAppreciation (JsonObject studentApp, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = setValue(studentApp);
        query.append("INSERT INTO " ).append(this.schema).append(this.table)
                .append(" (student_id, structure_id, appreciation) VALUES (?, ?, ?) ")
                .append("ON CONFLICT (student_id, structure_id) DO UPDATE SET appreciation = ? ")
                .append("RETURNING id");

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

    private JsonArray setValue(JsonObject studentApp){
        return new JsonArray().add(studentApp.getString("id_student"))
                .add(studentApp.getString("id_structure"))
                .add(studentApp.getString("appreciation"))
                .add(studentApp.getString("appreciation"));
    }

    @Override
    public void deleteStudentAppreciation(Long idStudentApp, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ").append(this.schema).append(this.table)
                .append(" WHERE id = ?");
        Sql.getInstance().prepared(query.toString(), new JsonArray().add(idStudentApp),
                SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getStudentAppreciation(String idStudent, String idStructure,
                                       Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ").append(this.schema).append(this.table)
                .append(" WHERE student_id = ? AND structure_id = ?");

        JsonArray values = new JsonArray().add(idStudent).add(idStructure);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }
}
