package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

public interface StructureOptionsService extends CrudService {

    /**
     *
     * @param body contain structureId String and isSkillAverage boolean
     * @param handler response empty JsonObject
     */
    void createOrUpdateIsAverageSkills (JsonObject body, Handler<Either<String, JsonObject>> handler);

    /**
     *
     * @param structureId structureId
     * @param handler response contains is_average_skill boolean
     */
    void getIsAverageSkills (String structureId, Handler<Either<String, JsonObject>> handler);

}
