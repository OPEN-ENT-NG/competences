package fr.openent.competences.service.digitalSkills;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

import java.util.List;

public interface ClassAppreciationDigitalSkillsService extends CrudService {

    void createOrUpdateClassAppreciation (final JsonObject classApp, Handler<Either<String, JsonObject>> handler);

    void deleteClassAppreciation (final Long idClassAppreciation, Handler<Either<String, JsonObject>> handler);

    void getClassAppreciation (final String id_class, final Long id_type_periode, Handler<Either<String, JsonObject>> handler);

    void getAppreciationsClasses (final List<String> listIdsClass, final List<Integer> listIdsPeriod, Handler<Either<String, JsonArray>> handler);
}