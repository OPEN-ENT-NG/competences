package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

public interface StructureOptionsService extends CrudService {

    void createOrUpdateIsAverageSkills (JsonObject body, Handler<Either<String, JsonObject>> handler);

    void getIsAverageSkills (String structureId, Handler<Either<String, JsonObject>> handler);

}
