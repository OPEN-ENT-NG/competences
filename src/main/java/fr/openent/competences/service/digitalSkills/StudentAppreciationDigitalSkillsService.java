package fr.openent.competences.service.digitalSkills;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

public interface StudentAppreciationDigitalSkillsService extends CrudService {

    void createOrUpdateStudentAppreciation (JsonObject studentApp, Handler<Either<String, JsonObject>> handler);

    void deleteStudentAppreciation(Long idStudentApp, Handler<Either<String, JsonObject>> handler);

    void getStudentAppreciation(String id_student, String id_structure, Long id_type_periode,
                                Handler<Either<String, JsonObject>> handler);
}
