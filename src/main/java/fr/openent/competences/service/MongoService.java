package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface MongoService {
    void addExport(JsonObject export, Handler<String> handler);

    void updateExport(String idExport, String status, String fileId, Handler<String> handler);

    void getExports(Handler<Either<String, JsonArray>> handler, String userId);

    void getWaitingExports(Handler<Either<String, JsonObject>> handler);

    void getExport(JsonObject params, Handler<Either<String, JsonArray>> handler);

    void deleteExports(JsonArray values, Handler<Either<String, JsonObject>> handler);
}
