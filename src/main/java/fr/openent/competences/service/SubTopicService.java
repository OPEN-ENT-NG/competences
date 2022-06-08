package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface SubTopicService {
    public void upsertCoefficent(JsonObject data, Handler<Either<String, JsonArray>> handler) ;

    void getSubtopicServices(String idStructure, Handler<Either<String, JsonArray>> defaultResponseHandler);
}
