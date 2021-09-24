package fr.openent.competences.service.digitalSkills;

import fr.openent.competences.Competences;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public interface DigitalSkillsService {

    void getDigitalSkillsByStudentByClass(final String id_student, final String id_class, final String id_structure,
                             Handler<Either<String, JsonObject>> handler);

    void getDigitalSkillsByStudent(final String id_student, final String id_structure,
                                    Handler<Either<String, JsonObject>> handler);

    void getAllDigitalSkillsByDomaine(Handler<Either<String, JsonArray>> handler);
}
