package fr.openent.competences.service.impl;


import fr.openent.competences.constants.Field;
import fr.openent.competences.enums.TypePDF;
import fr.openent.competences.service.ExportBulletinService;
import fr.openent.competences.service.MongoExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.vertx.java.busmods.BusModBase;



public class BulletinWorker extends BusModBase implements Handler<Message<JsonObject>>{
    private Storage storage;
    private final MongoExportService  exportService = new DefaultMongoExportService();
    private boolean isWorking = false;
    private ExportBulletinService exportBulletinService;
    public static final String SAVE_BULLETIN = "saveBulletin";
    private static final Logger log = LoggerFactory.getLogger(BulletinWorker.class);
    private boolean isSleeping = true;

    @Override
    public void start(){
        super.start();
        String neo4jConfig = (String) vertx.sharedData().getLocalMap("server").get("neo4jConfig");
        Neo4j.getInstance().init(vertx, new JsonObject(neo4jConfig));
        this.storage = new StorageFactory(vertx).getStorage();
        this.exportBulletinService = new DefaultExportBulletinService(eb, storage, vertx);
        vertx.eventBus().localConsumer(BulletinWorker.class.getSimpleName(), this);
        processExport();
    }

    @Override
    public void handle(Message<JsonObject> message) {
        message.reply(new JsonObject().put("status", "ok"));
        if (isSleeping) {
            logger.info(String.format("[Competences@%s] BulletinWorker called " , this.getClass().getSimpleName()) );
            isSleeping = false;
            processExport();
        }
    }
    private void processExport() {
        Handler<Either<String, Boolean>> exportHandler = event -> {
            logger.info(String.format("[Competences@%s] exportHandler" , this.getClass().getSimpleName()));
            if (event.isRight()) {
                isWorking = false;
                logger.info("[Competences@BulletinWorker ] export to Waiting");
                processExport();
            } else {
                log.error(event.left().getValue());
            }
        };
        exportService.getWaitingExport(event -> {
            if(event.isRight() &&  !event.right().getValue().isEmpty() ){
                log.info("[Competences@BulletinWorker::processExport ] getWaitingExport");
                JsonObject waitingOrder = event.right().getValue();
                chooseExport( waitingOrder,exportHandler);
            }else{
                isSleeping = true;
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                processExport();
                            }
                        },
                        3600*1000
                );
            }
        });
    }

    private void chooseExport(JsonObject body, Handler<Either<String, Boolean>> exportHandler) {
        final String action = body.getString("action", "");
        JsonObject params = body.getJsonObject("params");
        log.info("[Competences@BulletinWorker::chooseExport ] "+ this.getClass().toString()+ "  Export Type : " + action);
        switch (action) {
            case SAVE_BULLETIN :
                if(!isWorking){
                    isWorking = true;
                    params.put("_id",body.getString("_id"));
                    processBulletin(params, exportHandler);
                }
                break;
            case Field.SAVE_BFC:
                if(!isWorking){
                    isWorking = true;
                    params.put("_id",body.getString("_id"));
                    processBFC(params, exportHandler);
                }
                break;
            default:
                catchError(exportService, body.getString("_id"), "Invalid action in worker : " + action);
                isSleeping = true;
                isWorking = false;
                break;
        }
    }
    public static void catchError(MongoExportService exportService, String idFile, String errorCatchTextOutput) {
        exportService.updateWhenError(idFile,errorCatchTextOutput, makeError -> {
            if (makeError.isLeft()) {
                log.error("[Competences@BulletinWorker::catchError ]Error for create file export excel " + makeError.left().getValue() + errorCatchTextOutput);
            }
        });
        log.error("[Competences@BulletinWorker::catchError ] Error for create file export excel " + errorCatchTextOutput);
    }



    private void processBFC(JsonObject paramBfc ,Handler<Either<String, Boolean>> bfcHandler) {
        try {
            JsonObject params = paramBfc.copy();

            JsonObject bfcToHandle = paramBfc.getJsonObject("eleve").copy();
            params.getJsonArray("classes").getJsonObject(0).put("eleves", new JsonArray().add(bfcToHandle));
            paramBfc.remove("eleve");
            bfcToHandle.put("typeExport", TypePDF.BFC.toString());
            bfcToHandle.put("idCycle", paramBfc.getInteger("idCycle"));
            log.info(String.format("[Competences@%s::processBFC : Process BFC", this.getClass().getSimpleName()));
            exportBulletinService.runSavePdf(bfcToHandle, params, vertx, config, event -> {
                if (event.isRight()) {
                    log.info("[Competences@BulletinWorker::processBFC  ] process DONE " + event.right().getValue());
                    exportService.updateWhenSuccess(event.right().getValue(), paramBfc.getString("_id"), bfcHandler);
                }
                if (event.isLeft()) {
                    catchError(exportService, paramBfc.getString("_id"), "Error while Doing BFC : " + event.left().getValue());
                    log.error("ERROR [Competences@BulletinWorker::processBFC] : " + event.left().getValue());
                    bfcHandler.handle(new Either.Left<>(event.left().getValue()));
                }
            });
        }catch (Exception e){
            catchError(exportService, paramBfc.getString("_id"), "Error while processing  BFC: " + e.getMessage());
            log.error("ERROR [Competences@BulletinWorker | processBFC] : " +  e.getMessage());

        }
    }

    private void processBulletin(JsonObject paramBulletin, Handler<Either<String, Boolean>> bulletinHandlerWork) {
        try {
            JsonObject bulletinToHandle = paramBulletin.getJsonObject("eleve").copy();
            paramBulletin.put("eleves", new JsonArray().add(bulletinToHandle)  );
            paramBulletin.remove("eleve");
            log.info("[Competences@BulletinWorker::processBulletin ] Process BULLETIN");
            bulletinToHandle.put("typeExport", TypePDF.BULLETINS.toString());
            exportBulletinService.runSavePdf(bulletinToHandle, paramBulletin, vertx, config, event -> {
                isSleeping = true;
                isWorking = false;
                if (event.isRight()) {
                    log.info("[Competences@BulletinWorker::processBulletin ] process DONE");
                    exportService.updateWhenSuccess(event.right().getValue(), paramBulletin.getString("_id"), bulletinHandlerWork);
                }
                if (event.isLeft()) {
                    catchError(exportService, paramBulletin.getString("_id"), "Error while processing Bulletin : " + event.left().getValue());
                    log.error("ERROR [Competences@BulletinWorker::processBulletin] : " + event.left().getValue());
                    bulletinHandlerWork.handle(new Either.Left<>(event.left().getValue()));
                }
            });
        } catch (Exception e) {
            catchError(exportService, paramBulletin.getString("_id"), "Error while processing Bulletin : "  + e.getClass().toString() );
            log.error("ERROR [Competences@BulletinWorker::processBulletin] : " + e.getMessage() );
        }
    }
}
