package fr.openent.competences.helpers;

import fr.openent.competences.constants.Field;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;

import java.util.List;


public class FileHelper {
    private static final Logger log = LoggerFactory.getLogger(FileHelper.class);

    private FileHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * remove files by ids
     * @param storage           Storage instance
     * @param fileIds           list of file identifiers to send
     * @return                  {@link Future<JsonObject>}
     */
    public static Future<JsonObject> removeFiles(Storage storage, List<String> fileIds) {
        Promise<JsonObject> promise = Promise.promise();
        if (fileIds.isEmpty()) {
            promise.complete(new JsonObject().put("remove file status", "ok"));
        } else {
            storage.removeFiles(new JsonArray(fileIds), result -> {
                if (!"ok".equals(result.getString("status"))) {
                    String message = "[Competences@FileHelper::removeFiles] Failed to remove files.";
                    log.error(message, result.getString("message"));
                    promise.fail(message);
                    return;
                }
               promise.complete(result);
            });
        }
        return promise.future();
    }

    public static Future<JsonObject> addFile(Storage storage, HttpServerRequest request) {
        Promise<JsonObject> promise = Promise.promise();
        storage.writeUploadFile(request, message -> {
            if (!Field.OK.equals(message.getString(Field.STATUS))) {
                String messageErr = String.format("[%s - addFile] Failed to upload file from http request",
                        FileHelper.class.getSimpleName());
                log.error(messageErr);
                promise.fail("Failed to upload file from http request");
            } else {
                message.remove(Field.STATUS);
                promise.complete(message);
            }
        });
        return promise.future();
    }

    public static Future<Buffer> readFile(Storage storage, String fileId) {
        Promise<Buffer> promise = Promise.promise();
        storage.readFile(fileId, result -> {
            if (result == null) {
                promise.complete();
                return;
            }
            promise.complete(result);
        });
        return promise.future();
    }
}
