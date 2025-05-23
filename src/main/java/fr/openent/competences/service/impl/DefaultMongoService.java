package fr.openent.competences.service.impl;

import fr.openent.competences.service.MongoService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.service.impl.MongoDbCrudService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DefaultMongoService extends MongoDbCrudService implements MongoService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMongoService.class);

    private static final String  STATUS = "status";
    public DefaultMongoService(String collection) {
        super(collection);
    }

    @Override
    public void addExport(JsonObject export, Handler<String> handler) {
        try {
            mongo.insert(this.collection, export, jsonObjectMessage -> handler.handle(jsonObjectMessage.body().getString("_id")));
        } catch (Exception e) {
            handler.handle("mongoinsertfailed");
        }
    }
    @Override
    public void updateExport(String idExport, String status, String fileId,String log, Handler<String> handler){
        try {
            final JsonObject matches = new JsonObject().put("_id", idExport);
            mongo.findOne(this.collection, matches , MongoDbResult.validResultHandler(either -> {
                if (either.isRight()) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                    LocalDateTime now = LocalDateTime.now();
                    JsonObject exportProperties = either.right().getValue();
                    sanitizeExportProperties(exportProperties);
                    if(exportProperties != null && exportProperties.containsKey(STATUS) && !exportProperties.getString(STATUS).equals("SUCCESS")) {
                        exportProperties.put("updated", dtf.format(now));
                        exportProperties.put(STATUS, status);
                        if (!fileId.isEmpty())
                            exportProperties.put("fileId", fileId);
                        exportProperties.put("log",log);
                        mongo.save(collection, exportProperties, event -> {
                            if (!event.body().getString("status").equals("ok")) {
                                handler.handle("mongoinsertfailed");
                            } else {
                                handler.handle("ok");
                            }
                        });
                    }else{
                        handler.handle("ok");
                    }
                }
            }));
        } catch (Exception e) {
            handler.handle("mongoinsertfailed");
        }
    }

    private void sanitizeExportProperties(JsonObject exportProperties){
        JsonArray niveauCompetences = exportProperties
                .getJsonObject("params", new JsonObject())
                .getJsonObject("eleve", new JsonObject())
                .getJsonArray("niveauCompetences");

        if (niveauCompetences != null) {
            niveauCompetences.forEach(niveau -> ((JsonObject) niveau).remove("$$hashKey"));
        }
    }

    @Override
    public void getExports(Handler<Either<String, JsonArray>> handler, String userId) {
        mongo.find(collection, new JsonObject().put("userId",userId), MongoDbResult.validResultsHandler(handler));
    }
    @Override
    public void getWaitingExports(Handler<Either<String, JsonObject>> handler){
        mongo.findOne(collection, new JsonObject().put("status","WAITING"),  MongoDbResult.validResultHandler(handler));
    }
    @Override
    public void getExports(JsonObject params, Handler<Either<String, JsonArray>> handler) {
        mongo.find(collection, params, MongoDbResult.validResultsHandler(handler));
    }

    @Override
    public void deleteExports(JsonArray values, Handler<Either<String, JsonObject>> handler) {
        mongo.delete(collection, new JsonObject().put("_id", new JsonObject().put("$in", values)), MongoDbResult.validResultHandler(handler));
    }
}
