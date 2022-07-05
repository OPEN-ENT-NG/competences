package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.service.MongoExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultMongoExportService implements MongoExportService {
    private Logger log = LoggerFactory.getLogger(DefaultMongoExportService.class);
    DefaultMongoService mongo;

    public DefaultMongoExportService() {
        mongo = new DefaultMongoService(Competences.MONGO_COLLECTION);
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
            if (Field.SAVE_BFC.equals(typeExport)){
                JsonObject studentsDelete = common.getJsonArray("classes").getJsonObject(0);
                studentsDelete.remove ("eleves");
            } else { // cas des bulletins
                common.remove("eleves");
            }

            common.put("template", template);
            common.put("title", title);
            common.put("request", request);
        }catch (Exception e){
            log.info(String.format("[Competences@%s::setFuturesToInsertMongo] an error has occurred during insert data in mongo: %s.", this.getClass().getSimpleName(), e.getMessage()));
        }
        List<Future<String>> futureArray = new ArrayList<>();
        for(Object studentJO : students){
            JsonObject student = (JsonObject) studentJO;

            //besoin 2 loops sinon erreur de json
            List<String> keysToDelete = new ArrayList<>();
            for (Map.Entry<String, Object> map : student) {
                String key = map.getKey();
                if(key.startsWith("$") || key.contains("."))
                    keysToDelete.add(key);
            }
            for(String key : keysToDelete){
                student.remove(key);
            }

            Promise<String> promise = Promise.promise();
            this.createWhenStart("pdf", common.copy().put("eleve",student),
                    typeExport,promise);
            futureArray.add(promise.future());
        }
        return futureArray;
    }


    public void updateWhenError (String idExport,String messageError, Handler<Either<String, Boolean>> handler){
        try{
            mongo.updateExport(idExport,"ERROR", "",messageError, event -> {
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
            mongo.updateExport(idExport,"ERROR", "","TimeOut", event -> {
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
            log.info("[Competences@s::updateWhenSuccess] updating status to SUCCESS in mongo fileId: " + fileId,this.getClass().getSimpleName());
            mongo.updateExport(idExport,"SUCCESS",fileId,"success", event -> {
                if (event.equals("mongoinsertfailed"))
                    handler.handle(new Either.Left<>("Error when inserting mongo"));
                else {
                    handler.handle(new Either.Right<>(true));
                }
            });
        } catch (Exception error) {
            log.error("[Competences@s::updateWhenSuccess]error when update ERROR in export" + error , this.getClass().getSimpleName());

        }
    }

    @Override
    public void getWaitingExport(Handler<Either<String,JsonObject>> handler) {
        mongo.getWaitingExports(handler);
    }

    @Override
    public void getErrorExport(Handler<Either<String, JsonArray>> handler) {
        mongo.getExports(new JsonObject().put("status","ERROR"),handler);
    }
}
