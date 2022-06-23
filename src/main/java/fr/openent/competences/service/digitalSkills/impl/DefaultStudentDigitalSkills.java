package fr.openent.competences.service.digitalSkills.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.service.digitalSkills.StudentDigitalSkillsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultStudentDigitalSkills extends SqlCrudService implements StudentDigitalSkillsService {
    private static final Logger log = LoggerFactory.getLogger(DefaultStudentDigitalSkills.class);

    public DefaultStudentDigitalSkills (String schema, String table){
        super(schema, table);
    }

    @Override
    public void createOrUpdateLevel(JsonObject digitalSkill, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ").append(this.schema).append(this.table)
                .append("(id_digital_skill, student_id, structure_id, level)")
                .append("VALUES (?, ?, ?, ?)")
                .append("ON CONFLICT (id_digital_skill, student_id, structure_id) DO UPDATE SET level = ? ")
                .append("RETURNING id");

        JsonArray values = setValue(digitalSkill);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

    private JsonArray setValue(JsonObject digitalSkill){
        return new JsonArray().add(digitalSkill.getLong("id_digital_skill"))
                .add(digitalSkill.getString("id_student"))
                .add(digitalSkill.getString("id_structure"))
                .add(digitalSkill.getLong("level"))
                .add(digitalSkill.getLong("level"));
    }

    @Override
    public void deleteDigitalSkillLevel(Long idDigSkill, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ").append(this.schema).append(this.table)
                .append(" WHERE id = ?");

        Sql.getInstance().prepared(query.toString(), new JsonArray().add(idDigSkill),
                SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getEvaluatedDigitalSkills(String idStudent, String idStructure,
                                          Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ").append(this.schema).append(this.table)
                .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Field.DIGITAL_SKILLS_TABLE)
                .append(" ds ON ds.id = id_digital_skill")
                .append(" WHERE student_id = ? AND structure_id = ?");

        JsonArray values = new JsonArray().add(idStudent).add(idStructure);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }
}
