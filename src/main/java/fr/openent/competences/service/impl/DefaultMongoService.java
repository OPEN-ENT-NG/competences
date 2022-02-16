package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.helpers.MongoHelper;
import fr.openent.competences.service.MongoExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DefaultMongoService implements MongoExportService {
    private Logger log = LoggerFactory.getLogger(DefaultMongoService.class);
    MongoHelper mongo;

    public DefaultMongoService() {
        mongo = new MongoHelper(Competences.MONGO_COLLECTION);
    }




    public void deleteExportMongo(JsonArray idsExports, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        for (int i = 0; i < idsExports.size(); i++) {
            values.add(idsExports.getValue(i));
        }
        mongo.deleteExports(values, handler);
    }

    public void createWhenStart(String extension, JsonObject infoFile, String action,
                                Promise<String> promise) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        JsonObject params = new JsonObject()
                .put("status", "WAITING")
                .put("created", dtf.format(now))
                .put("action", action)
                .put("extension", extension)
                .put("params",infoFile);

        mongo.addExport(params, event -> {
            if (event.equals("mongoinsertfailed"))
                promise.fail("Error when inserting mongo");
            else {
                promise.complete(event);
            }
        });
    }


    @Override
    public  List<Future<String>> insertDataInMongo(JsonArray students , JsonObject params,JsonObject request , String title, String template,String typeExport){
        JsonObject common  = params.copy();
        try {
            common.remove("eleves");
            common.put("template", template);
            common.put("title", title);
            common.put("request", request);
        }catch (Exception e){
            log.info(String.format("[Competences@%s::setFuturesToInsertMongo] an error has occurred during insert data in mongo: %s.", this.getClass().getSimpleName(), e.getMessage()));
        }
        List<Future<String>> futureArray= new ArrayList<>();
        for(Object studentJO : students){
            JsonObject student = (JsonObject) studentJO;
            student.remove("u.deleteDate");
            common.put("eleve",student);
            Promise<String> promise = Promise.promise();
            this.createWhenStart("pdf", common,
                    typeExport,promise);
            futureArray.add(promise.future());
        }
        return futureArray;
    }


    public void updateWhenError (String idExport, Handler<Either<String, Boolean>> handler){
        try{
            mongo.updateExport(idExport,"ERROR", "", event -> {
                if(event.equals("mongoinsertfailed"))
                    handler.handle(new Either.Left<>("Error when inserting mongo"));
                else{
                    handler.handle(new Either.Right<>(true));
                }

            });
        } catch (Exception error){
            log.error("error when update ERROR in export" + error);
        }
    }

    public void updateWhenErrorTimeout (String idExport, Handler<Either<String, Boolean>> handler){
        try{
            mongo.updateExport(idExport,"ERROR", "", event -> {
                if(event.equals("mongoinsertfailed"))
                    handler.handle(new Either.Left<>("Error when inserting mongo"));
                else{
                    log.info("EXPORT TIMED OUT ");
                    handler.handle(new Either.Right<>(true));
                }

            });
        } catch (Exception error){
            log.error("error when update ERROR in export" + error);
        }
    }
    public void updateWhenSuccess (String fileId, String idExport, Handler<Either<String, Boolean>> handler) {
        try {
            log.info("[Competences] updating status to SUCCESS in mongo fileId: " + fileId );
            mongo.updateExport(idExport,"SUCCESS",fileId, event -> {
                if (event.equals("mongoinsertfailed"))
                    handler.handle(new Either.Left<>("Error when inserting mongo"));
                else {
                    handler.handle(new Either.Right<>(true));
                }
            });
        } catch (Exception error) {
            log.error("error when update ERROR in export" + error);

        }
    }

    @Override
    public void getWaitingExport(Handler<Either<String,JsonObject>> handler) {
        mongo.getWaitingExports(handler);
    }
}
