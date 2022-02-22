package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface MongoExportService {




    void deleteExportMongo(JsonArray idsExports, Handler<Either<String, JsonObject>> handler);

    void createWhenStart(String extension , JsonObject infoFile,  String action,
                         Promise<String> handler);

    List<Future<String>> insertDataInMongo(JsonArray students, JsonObject params, JsonObject request, String title, String template, String typeExport);

    void updateWhenError(String idExport,String errorMessage, Handler<Either<String, Boolean>> handler);

    void updateWhenSuccess(String fileId, String idExport, Handler<Either<String, Boolean>> handler);


    void getWaitingExport(Handler<Either<String, JsonObject>> handler);

    void getErrorExport(Handler<Either<String, JsonArray>> handler);

    void updateWhenErrorTimeout(String idFile,  Handler<Either<String, Boolean>> handler);
}
