package fr.openent.competences.service.digitalSkills;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

public interface StudentDigitalSkillsService extends CrudService {
    void createOrUpdateLevel(JsonObject digitalSkill, Handler<Either<String, JsonObject>> handler);

    void deleteDigitalSkillLevel(Long idDigSkill, Handler<Either<String, JsonObject>> handler);

    void getEvaluatedDigitalSkills(String idStudent, String idStructure, Handler<Either<String, JsonArray>> handler);
}
