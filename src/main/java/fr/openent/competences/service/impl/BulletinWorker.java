package fr.openent.competences.service.impl;

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

public class BulletinWorker extends BusModBase implements Handler<Message<JsonObject>>{
    private Storage storage;
    private boolean isWorking = false;
    List<JsonObject> bulletinsEleves = new ArrayList();
    private ExportBulletinService exportBulletinService;
    public static final String SAVE_BULLETIN = "saveBulletin";
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
        JsonObject paramBulletin = message.body();
        JsonArray bulletins = paramBulletin.getJsonObject("resultFinal").getJsonArray("eleves");
        paramBulletin.getJsonObject("resultFinal").remove("eleves");
        final String action = paramBulletin.getString(ACTION, "");
        switch (action) {
            case SAVE_BULLETIN :
                stackBulletin(bulletins);
                if(!isWorking){
                    isWorking = true;
                    new Thread(() -> {
                        processBulletin(paramBulletin, event -> {
                            log.info("end bulletin");
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


    private JsonObject HandleStackJsonObject() {
        if (bulletinsEleves.size() != 0) {
            JsonObject bulletin = bulletinsEleves.get(0);
            bulletinsEleves.remove(0);
            return bulletin;
        } else {
            log.info("end Work");
            isWorking = false;
            return null;
        }
    }

    private void processBulletin(JsonObject paramBulletin ,Handler<Either<String, Boolean>> bulletinHandlerWork) {
        JsonObject bulletinToHandle = HandleStackJsonObject();
        if (bulletinToHandle == null) return;
        log.info("start Work processBulletin");
        exportBulletinService.runSavePdf(bulletinToHandle, paramBulletin, vertx, config, event -> {
            processBulletin(paramBulletin, bulletinHandlerWork);
            if( event.isLeft()){
                log.error("[BulletinWorker] : " + event.left().getValue());
            }
        });
    }
}
