package fr.openent.competences.helpers;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FormateFutureEvent {

    private static final Logger log = LoggerFactory.getLogger(FormateFutureEvent.class);
    public static <T> void formate(Future<T> future, Either<String, T> event) {
        if(event.isLeft()) {
            String error = event.left().getValue();
            future.fail(error);
        }
        else {
            future.complete(event.right().getValue());
        }
    }

}


