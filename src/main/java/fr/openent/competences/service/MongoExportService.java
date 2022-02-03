package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface MongoExportService {


    void getExportName(String fileId, Handler<Either<String, JsonArray>> handler);


    void deleteExportMongo(JsonArray idsExports, Handler<Either<String, JsonObject>> handler);

    void createWhenStart(String extension , JsonObject infoFile,  String action,
                         Promise<String> handler);

    void updateWhenError(String idExport, Handler<Either<String, Boolean>> handler);

    void updateWhenSuccess(String fileId, String idExport, Handler<Either<String, Boolean>> handler);


    void getWaitingExport(Handler<Either<String, JsonObject>> handler);

    void updateWhenErrorTimeout(String idFile,  Handler<Either<String, Boolean>> handler);
}
