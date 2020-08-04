package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;

public interface AppreciationSubjectPeriodService extends CrudService {
    /**
     * @param valuesGetIdAppreciationSubjectPeriod [idPeriod, idStudent, idClassSchool, idSubject ]
     * @param userInfos
     * @param appreciation
     * @param handler
     */
    void updateOrInsertAppreciationSubjectPeriod(JsonArray valuesGetIdAppreciationSubjectPeriod,
                                                 UserInfos userInfos,
                                                 String appreciation,
                                                 Handler<Either<String, JsonObject>> handler);
}
