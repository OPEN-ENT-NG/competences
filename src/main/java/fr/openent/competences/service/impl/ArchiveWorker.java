package fr.openent.competences.service.impl;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.vertx.java.busmods.BusModBase;

import static fr.openent.competences.Competences.ID_STRUCTURES_KEY;
import static fr.openent.competences.Utils.isNotNull;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.*;

public class ArchiveWorker extends BusModBase {
    private Storage storage;
    public static final String ARCHIVE_BULLETIN = "archiveBulletin";
    public static final String ARCHIVE_BFC = "archiveBfc";
    private static final Logger log = LoggerFactory.getLogger(ArchiveWorker.class);

    @Override
    public void start(){
        super.start();
        vertx.eventBus().localConsumer(ArchiveWorker.class.getSimpleName(), archiveHandler());
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
             if(isNotNull(host) && isNotNull(acceptLanguage)
                     && isNotNull(forwardedFor)) {
                 switch (action) {

                     case ARCHIVE_BULLETIN:
                         final JsonArray idStructures = body.getJsonArray(ID_STRUCTURES_KEY);
                         new DefaultExportBulletinService(eb, storage, vertx).archiveBulletin(idStructures, vertx,
                                 config, path, host, acceptLanguage, forwardedFor);
                         break;

                     case ARCHIVE_BFC:
                         String scheme = body.getString(SCHEME);
                         new DefaultBFCService(eb,storage).archiveBFC(vertx, config, path, host, acceptLanguage,
                                 forwardedFor, scheme);
                         break;
                     default:
                         log.error(" ARCHIVE WORKER NO ACTION ");
                 }
             }
            else{

             }
        };
    }
}
