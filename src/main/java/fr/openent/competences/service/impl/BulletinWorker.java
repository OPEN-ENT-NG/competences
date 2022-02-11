package fr.openent.competences.service.impl;


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

import java.util.ArrayList;
import java.util.List;

import static fr.openent.competences.service.impl.DefaultExportBulletinService.*;

public class BulletinWorker extends BusModBase implements Handler<Message<JsonObject>>{
    private Storage storage;
    private MongoExportService  exportService = new DefaultMongoService();
    private boolean isWorking = false;
    List<JsonObject> bulletinsEleves = new ArrayList();
    List<JsonObject> bfcs = new ArrayList();
    private ExportBulletinService exportBulletinService;
    public static final String SAVE_BULLETIN = "saveBulletin";
    public static final String SAVE_BFC = "saveBFC";
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

    //TODO le worker arrete de faire des threads et utilise le mongo comme stack
    @Override
    public void handle(Message<JsonObject> message) {
        message.reply(new JsonObject().put("status", "ok"));
        if (isSleeping) {
            logger.info("Calling BulletinWorker");
            isSleeping = false;
            processExport();
        }
//
//        switch (action) {
//            case SAVE_BULLETIN :
//                if(!isWorking){
//                    isWorking = true;
//                    //RENMPLACER par appel mongo
//                        processBulletin(params, event -> {
//                            log.info("end bulletin");
//                        });
//                    }
//                break;
//            //TODO NE PAS OUBLIER LE BFC
//            case SAVE_BFC:
//                JsonObject datas = params.getJsonObject("resultFinal");
//                JsonArray eleves = datas.getJsonArray("classes").getJsonObject(0).getJsonArray("eleves");
//                stackBfc(eleves);
//                if(!isWorking){
//                    isWorking = true;
//                    new Thread(() -> {
//                        processBFC(params, event -> {
//                            log.info("end bfc");
//                        });
//                    }).start();
//                }
//                break;
//            default:
//                log.error(" BULLETIN WORKER NO ACTION ");
//        }
    }
    private void processExport() {
        Handler<Either<String, Boolean>> exportHandler = event -> {
            logger.info("exportHandler");
            if (event.isRight()) {
                isWorking = false;
                logger.info("export to Waiting");
                processExport();
            } else {
                log.error(event.left().getValue());
            }
        };
        exportService.getWaitingExport(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isRight()){
                    log.info("getWaitingExport");
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
            }
        });
    }

    private void chooseExport(JsonObject body, Handler<Either<String, Boolean>> exportHandler) {
        final String action = body.getString("action", "");
        JsonObject params = body.getJsonObject("params");
        log.info("[Competnces BulletinWorker ]Export Type : " + action);
        switch (action) {
            case SAVE_BULLETIN :
                if(!isWorking){
                    isWorking = true;
                    params.put("_id",body.getString("_id"));
                    processBulletin(params, exportHandler);
                }
                break;
            //TODO NE PAS OUBLIER LE BFC
            case SAVE_BFC:
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
    //vérifier si ca marche
    public static void catchError(MongoExportService exportService, String idFile, String errorCatchTextOutput) {
        exportService.updateWhenError(idFile, makeError -> {
            if (makeError.isLeft()) {
                log.error("Error for create file export excel " + makeError.left().getValue() + errorCatchTextOutput);
            }
        });
        log.error("Error for create file export excel " + errorCatchTextOutput);
    }


    private JsonObject HandleStackJsonObject(List<JsonObject> list){
        if (list.size() != 0) {
            JsonObject bulletin = list.get(0);
            list.remove(0);
            return bulletin;
        } else {
            log.info("end Work bulletinWorker");
            isWorking = false;
            return null;
        }
    }
    private JsonObject HandleStackJsonObjectBFC() {
        return HandleStackJsonObject(bfcs);
    }

    private void processBFC(JsonObject paramBfc ,Handler<Either<String, Boolean>> bfcHandler) {

        JsonObject params = paramBfc.copy();
        JsonObject bfcToHandle = paramBfc.getJsonObject("eleve").copy();
        bfcToHandle.put("typeExport", TypePDF.BFC.toString());
        log.info("Process BFC");
        exportBulletinService.runSavePdf(bfcToHandle, params, vertx, config, event -> {
            if(event.isRight()){
                log.info("process DONE " +event.right().getValue());
                exportService.updateWhenSuccess( event.right().getValue(),paramBfc.getString("_id"),bfcHandler);
            }
            if (event.isLeft()) {
                catchError(exportService, paramBfc.getString("_id"), "Invalid action in worker : " +  event.left().getValue());
                log.error("ERROR [BulletinWorker | processBulletin] : " + event.left().getValue());
                bfcHandler.handle(new Either.Left<>(event.left().getValue()));
            }
        });
    }

    private void processBulletin(JsonObject paramBulletin, Handler<Either<String, Boolean>> bulletinHandlerWork) {
        JsonObject bulletinToHandle = paramBulletin.getJsonObject("eleve").copy();
        paramBulletin.remove("eleve");
        log.info("Process BULLETIN");
        bulletinToHandle.put("typeExport", TypePDF.BULLETINS.toString());
        exportBulletinService.runSavePdf(bulletinToHandle, paramBulletin, vertx, config, event -> {
            isSleeping = true;
            isWorking = false;
            if(event.isRight()){
                log.info("process DONE");
                exportService.updateWhenSuccess( event.right().getValue(),paramBulletin.getString("_id"),bulletinHandlerWork);
            }
            if(event.isLeft()){
                catchError(exportService, paramBulletin.getString("_id"), "Invalid action in worker : " +  event.left().getValue());
                log.error("ERROR [BulletinWorker | processBulletin] : " + event.left().getValue());
                bulletinHandlerWork.handle(new Either.Left<>(event.left().getValue()));
            }
        });
    }
}
