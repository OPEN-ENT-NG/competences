package fr.openent.competences.helpers;


import fr.openent.competences.constants.Field;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

public class FutureHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FutureHelper.class);

    private FutureHelper() {
    }

    @Deprecated
    public static Handler<Either<String, JsonArray>> handlerJsonArray(Promise<Object> promise) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
            } else {
                LOGGER.error(event.left().getValue());
                promise.fail(event.left().getValue());
            }
        };
    }

    @Deprecated
    public static Handler<Either<String, JsonArray>> handlerJsonArray(Future<JsonArray> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                LOGGER.error(event.left().getValue());
                future.fail(event.left().getValue());
            }
        };
    }

    @Deprecated
    public static Handler<Either<String, JsonObject>> handlerJsonObject(Promise<JsonObject> promise) {
        return handlerJsonObject(promise, null);
    }

    @Deprecated
    public static Handler<Either<String, JsonObject>> handlerJsonObject(Promise<JsonObject> promise, String logsInfo) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
            } else {
                LOGGER.error(String.format("%s %s", logsInfo != null ? logsInfo : "", event.left().getValue()));
                promise.fail(event.left().getValue());
            }
        };
    }

    public static Handler<Either<String, JsonArray>> handlerJsonArray(Promise<JsonArray> promise, String logs) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
            } else {
                LOGGER.error(String.format("%s %s",
                        (logs != null ? logs : ""), event.left().getValue()));
                promise.fail(event.left().getValue());
            }
        };
    }

    public static <R> Handler<Either<String, R>> handler(Promise<R> promise) {
        return handler(promise, null);
    }
    public static <R> Handler<Message<JsonObject>> handlerToAsyncHandler(Promise<R> promise, String errorMessage) {
        return message -> {
            JsonObject body = message.body();
            if (Field.OK.equals(body.getString(Field.STATUS))) {
                promise.complete((R) body.getValue(Field.RESULTS));
                return;
            }
            LOGGER.error(String.format("%s %s", (errorMessage != null ? errorMessage : ""), body.getString(Field.MESSAGE)));
            promise.fail(errorMessage != null ? errorMessage : body.getString(Field.MESSAGE));
        };
    }

    public static <R> Handler<Either<String, R>> handler(Promise<R> promise, String errorMessage) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
                return;
            }
            LOGGER.error(String.format("%s %s", (errorMessage != null ? errorMessage : ""), event.left().getValue()));
            promise.fail(errorMessage != null ? errorMessage : event.left().getValue());
        };
    }
    public static <T> CompositeFuture all(List<Future<T>> futures) {
        return CompositeFutureImpl.all(futures.toArray(new Future[futures.size()]));
    }

    public static <T> CompositeFuture join(List<Future<T>> futures) {
        return CompositeFutureImpl.join(futures.toArray(new Future[futures.size()]));
    }

    public static <T> CompositeFuture any(List<Future<T>> futures) {
        return CompositeFutureImpl.any(futures.toArray(new Future[0]));
    }
}