package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
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

    /**
     *
     * @param structureId structure id
     * @return response
     */
    Future<Boolean> isAverageSkills(String structureId);

    /**
     * change sync state of data from presenecs
     * @param idStructure id of the structure
     * @param state state
     * @param handler response
     */
    void activeDeactiveSyncStatePresences(String idStructure, Boolean state, Handler<Either<String, JsonObject>> handler);

    /**
     * get sync state of data from presenecs
     * @param idStructure id of the structure
     * @param handler response
     */
    void getSyncStatePresences(String idStructure, Handler<Either<String, JsonObject>> handler);

    /**
     * get config viescolaire and activation state of presences modules
     * @param idStructure id of the structure
     * @param handler response
     */
    void getActiveStatePresences (final String idStructure, Handler<Either<String,JsonObject>> handler);
}
