package fr.openent.competences.service.impl;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.vertx.java.busmods.BusModBase;

import static fr.openent.competences.Utils.isNotNull;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.*;

public class ArchiveBulletinWorker extends BusModBase {
    private Storage storage;
    public static final String ARCHIVE_BULLETIN = "archiveBulletin";


    @Override
    public void start(){
        super.start();
        vertx.eventBus().localConsumer(ArchiveBulletinWorker.class.getSimpleName(), archiveHandler());
        this.storage = new StorageFactory(vertx).getStorage();

    }


    private Handler<Message<JsonObject>> archiveHandler() {
        return message -> {
            JsonObject body = message.body();
            final String action = body.getString(ACTION, "");
            final String host = body.getString(HOST);
            final String acceptLanguage = body.getString(ACCEPT_LANGUAGE);
            final Boolean forwardedFor = body.getBoolean(X_FORWARDED_FOR);
            final String path = body.getString(PATH);
             if(action.equals(ARCHIVE_BULLETIN) && isNotNull(host) && isNotNull(acceptLanguage)
                     && isNotNull(forwardedFor)){
                    new DefaultExportBulletinService(eb, storage, vertx).archiveBulletin(vertx, config, path, host,
                            acceptLanguage, forwardedFor);
            }
            else{

             }
        };
    }
}
