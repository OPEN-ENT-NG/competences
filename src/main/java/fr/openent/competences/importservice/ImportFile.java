package fr.openent.competences.importservice;

import fr.openent.competences.constants.Field;
import fr.openent.competences.helpers.FileHelper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.storage.Storage;

import java.util.Collections;

public abstract class ImportFile <T> implements Import<T> {

    protected final Storage storage;
    protected final HttpServerRequest request;

    protected ImportFile(HttpServerRequest request, Storage storage) {
        this.request = request;
        this.storage = storage;
    }

    public Future<Buffer> processImportFile() {
        Promise<Buffer> promise = Promise.promise();
        FileHelper.addFile(storage, request)
                .onSuccess(res -> {
                    String fileId = res.getString(Field._ID);
                    readImportedFile(fileId)
                            .onComplete(event -> {
                                if (event.failed()) {
                                    promise.fail(event.cause().getMessage());
                                } else {
                                    promise.complete(event.result());
                                }
                                FileHelper.removeFiles(storage, Collections.singletonList(fileId));
                            });
                })
                .onFailure(err -> promise.fail(err.getMessage()));
        return promise.future();
    }

    private Future<Buffer> readImportedFile(String fileId) {
        Promise<Buffer> promise = Promise.promise();
        FileHelper.readFile(this.storage, fileId)
                .onSuccess(promise::complete)
                .onFailure(err -> {
                    // gestion log etc
                    promise.fail(err.getMessage());
                });
        return promise.future();
    }
}
