package fr.openent.competences.service.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultLSUService implements LSUService {

    private static final Logger log = LoggerFactory.getLogger(DefaultExportBulletinService.class);

    public void serviceResponseOK (AtomicBoolean answer, int count, String thread, String method) {
        if (count > 1 ) {
            log.info("[ "+ method + " ] : " + thread + " TIME OUT " + count);
        }
        if(!answer.get()) {
            answer.set(true);
            log.info(" -------[" + method + "]: " + thread + " FIN " );
        }
    }
}
