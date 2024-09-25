package fr.openent.competences.helpers;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FormateFutureEvent {

    private static final Logger log = LoggerFactory.getLogger(FormateFutureEvent.class);
    public static <T> void formate(String errorMessage, Promise<T> promise, Either<String, T> event) {
        if(event.isLeft()) {
            String error = event.left().getValue();
            log.info(errorMessage + error);
            promise.fail(error);
        }
        else {
            promise.complete(event.right().getValue());
        }
    }
    public static <T> void formate(Promise<T> promise, Either<String, T> event) {
        if(event.isLeft()) {
            String error = event.left().getValue();
            promise.fail(error);
        }
        else {
            promise.complete(event.right().getValue());
        }
    }

}


