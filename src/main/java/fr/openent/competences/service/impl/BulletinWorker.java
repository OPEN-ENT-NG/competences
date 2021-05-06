package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.enums.TypePDF;
import fr.openent.competences.service.ExportBulletinService;
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
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class BulletinWorker extends BusModBase implements Handler<Message<JsonObject>>{
    private Storage storage;
    private boolean isWorking = false;
    List<JsonObject> bulletinsEleves = new ArrayList();
    List<JsonObject> bfcs = new ArrayList();
    private ExportBulletinService exportBulletinService;
    public static final String SAVE_BULLETIN = "saveBulletin";
    public static final String SAVE_BFC = "saveBFC";
    private static final Logger log = LoggerFactory.getLogger(BulletinWorker.class);

    @Override
    public void start(){
        super.start();
        String neo4jConfig = (String) vertx.sharedData().getLocalMap("server").get("neo4jConfig");
        Neo4j.getInstance().init(vertx, new JsonObject(neo4jConfig));
        this.storage = new StorageFactory(vertx).getStorage();
        this.exportBulletinService = new DefaultExportBulletinService(eb, storage, vertx);
        vertx.eventBus().localConsumer(BulletinWorker.class.getSimpleName(), this);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject params = message.body();
        final String action = params.getString(ACTION, "");
        JsonArray bulletins;
        switch (action) {
            case SAVE_BULLETIN :
                bulletins = params.getJsonObject("resultFinal").getJsonArray("eleves");
                params.getJsonObject("resultFinal").remove("eleves");
                stackBulletin(bulletins);
                if(!isWorking){
                    isWorking = true;
                    new Thread(() -> {
                        processBulletin(params, event -> {
                            log.info("end bulletin");
                        });
                    }).start();
                }
                break;
            case  SAVE_BFC:
                JsonObject datas = params.getJsonObject("resultFinal");
                JsonArray eleves = datas.getJsonArray("classes").getJsonObject(0).getJsonArray("eleves");
                stackBfc(eleves);
                if(!isWorking){
                    isWorking = true;
                    new Thread(() -> {
                        processBFC(params, event -> {
                            log.info("end bfc");
                        });
                    }).start();
                }
                break;
            default:
                log.error(" BULLETIN WORKER NO ACTION ");
        }
    }

    private void stackBulletin(JsonArray bulletins) {
        if( bulletinsEleves != null ){
            for(int i =0 ; i < bulletins.size(); i++){
                bulletinsEleves.add(bulletins.getJsonObject(i));
            }
        }
    }

    private void stackBfc(JsonArray eleves) {
        if( bfcs != null ){
            for(int i =0 ; i < eleves.size(); i++){
                bfcs.add(eleves.getJsonObject(i).copy());
            }
        }
    }

    private  JsonObject HandleStackJsonObject(List<JsonObject> list){
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
    private JsonObject HandleStackJsonObjectBulletins() {
        return HandleStackJsonObject(bulletinsEleves);
    }
    private JsonObject HandleStackJsonObjectBFC() {
        return HandleStackJsonObject(bfcs);
    }

    private void processBFC(JsonObject paramBulletin ,Handler<Either<String, Boolean>> bulletinHandlerWork) {
        JsonObject bulletinToHandle = HandleStackJsonObjectBFC();
        if (bulletinToHandle == null) return;
        JsonObject params = paramBulletin.copy();
        bulletinToHandle.put("typeExport", TypePDF.BFC);
        params.getJsonObject("resultFinal").getJsonArray("classes").getJsonObject(0).getJsonArray("eleves").add(bulletinToHandle);
        if(!params.getJsonObject("resultFinal").containsKey("idClasse")){
            JsonObject actionClasse = new JsonObject().put("action","classe.getClasseIdByEleve")
                    .put("idEleve",bulletinToHandle.getString("idEleve"));
            eb.send(Competences.VIESCO_BUS_ADDRESS, actionClasse, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    bulletinToHandle.put("idClasse",event.body().getJsonObject("result").
                            getJsonObject("c").getJsonObject("data").getValue("id"));
                    bulletinToHandle.put("externalId",event.body().getJsonObject("result").
                            getJsonObject("c").getJsonObject("data").getValue("externalId"));
                    launchProcess(bulletinToHandle, params, paramBulletin, bulletinHandlerWork);
                }
            }));
        }else {
            bulletinToHandle.put("idClasse",params.getJsonObject("resultFinal").getValue("idClasse"));
            launchProcess(bulletinToHandle, params, paramBulletin, bulletinHandlerWork);
        }
    }

    private void launchProcess(JsonObject bulletinToHandle, JsonObject params, JsonObject paramBulletin, Handler<Either<String, Boolean>> bulletinHandlerWork) {
        bulletinToHandle.put("idCycle", params.getJsonObject("resultFinal").getValue("idCycle"));
        exportBulletinService.runSavePdf(bulletinToHandle, params, vertx, config, event -> {
            processBFC(paramBulletin, bulletinHandlerWork);
            if (event.isLeft()) {
                log.error("[BulletinWorker] : " + event.left().getValue());
            }
        });
    }

    private void processBulletin(JsonObject paramBulletin ,Handler<Either<String, Boolean>> bulletinHandlerWork) {
        JsonObject bulletinToHandle = HandleStackJsonObjectBulletins();
        if (bulletinToHandle == null) return;
        log.info("start Work processBulletin in Bulletins worker");
        paramBulletin.put("typeExport", TypePDF.BULLETIN);
        exportBulletinService.runSavePdf(bulletinToHandle, paramBulletin, vertx, config, event -> {
            processBulletin(paramBulletin, bulletinHandlerWork);
            if( event.isLeft()){
                log.error("[BulletinWorker] : " + event.left().getValue());
            }
        });
    }
}
