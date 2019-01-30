package fr.openent.competences.utils;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;

public class FormateFutureEvent {


    public static <T> void formate (Future<T> future, Either<String, T> event) {
        if(event.isLeft()) {
            String error = event.left().getValue();
            future.fail(error);
        }
        else {
            future.complete(event.right().getValue());
        }
    }

}


