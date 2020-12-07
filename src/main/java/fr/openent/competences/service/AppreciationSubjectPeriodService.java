package fr.openent.competences.service;

import fr.openent.competences.model.AppreciationSubjectPeriodModel;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;

public interface AppreciationSubjectPeriodService extends CrudService {
    /**
     * @param userInfos
     * @param handler
     */
    void updateOrInsertAppreciationSubjectPeriod(AppreciationSubjectPeriodModel appreciationSubjectPeriod,
                                                 UserInfos userInfos, String idStructure,
                                                 Handler<Either<String, JsonObject>> handler);
}
