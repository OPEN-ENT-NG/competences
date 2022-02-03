package fr.openent.competences.helpers;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.MongoDbCrudService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MongoHelper extends MongoDbCrudService {
    private static final Logger logger = LoggerFactory.getLogger(MongoHelper.class);

    private static final String  STATUS = "status";
    public MongoHelper(String collection) {
        super(collection);
    }


    public void addExport(JsonObject export, Handler<String> handler) {
        try {
            mongo.insert(this.collection, export, jsonObjectMessage -> handler.handle(jsonObjectMessage.body().getString("_id")));
        } catch (Exception e) {
            handler.handle("mongoinsertfailed");
        }
    }

    public void updateExport(String idExport, String status, String fileId, Handler<String> handler){
        try {
            final JsonObject matches = new JsonObject().put("_id", idExport);
            mongo.findOne(this.collection, matches , result -> {
                if ("ok".equals(result.body().getString(STATUS))) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                    LocalDateTime now = LocalDateTime.now();
                    JsonObject exportProperties = result.body().getJsonObject("result");
                    if(exportProperties != null && exportProperties.containsKey(STATUS) && !exportProperties.getString(STATUS).equals("SUCCESS")) {
                        exportProperties.put("updated", dtf.format(now));
                        exportProperties.put(STATUS, status);
                        exportProperties.put("NbIterationsLeft", exportProperties.getInteger("NbIterationsLeft") - 1);
                        if (!fileId.isEmpty())
                            exportProperties.put("fileId", fileId);
                        mongo.save(collection, exportProperties, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> event) {
                                if (!event.body().getString("status").equals("ok")) {
                                    handler.handle("mongoinsertfailed");
                                } else {
                                    handler.handle("ok");
                                }
                            }
                        });
                    }else{
                        handler.handle("ok");
                    }
                }
            });
        } catch (Exception e) {
            handler.handle("mongoinsertfailed");
        }
    }

    public void getExports(Handler<Either<String, JsonArray>> handler, String userId) {
        mongo.find(collection, new JsonObject().put("userId",userId), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(new Either.Right(event.body().getJsonArray("results")));
            }
        });
    }

    public void getWaitingExports(Handler<Either<String,JsonObject>> handler){
        mongo.findOne(collection, new JsonObject().put("status","WAITING"),new Handler<Message<JsonObject>>(){
            @Override
            public void handle(Message<JsonObject> event){
                JsonObject result = event.body();
                if(result.containsKey("result"))
                    handler.handle(new Either.Right(result.getJsonObject("result")));
                else
                    handler.handle(new Either.Left<>("No waiting orders"));
            }
        });
    }
    public void getExport( JsonObject params, Handler<Either<String, JsonArray>> handler) {
        mongo.find(collection, params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(new Either.Right(event.body().getJsonArray("results")));
            }
        });
    }

    public void deleteExports(JsonArray values, Handler<Either<String, JsonObject>> handler) {
        mongo.delete(collection, new JsonObject().put("_id", new JsonObject().put("$in", values)), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if(!event.body().getString("status").equals("ok")) {
                    handler.handle(new Either.Left<>("mongoinsertfailed"));
                }else {
                    handler.handle(new Either.Right<>(new JsonObject()));
                }
            }
        });
    }
}
