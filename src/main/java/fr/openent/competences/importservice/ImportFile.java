package fr.openent.competences.importservice;

import fr.openent.competences.helpers.FileHelper;
import fr.openent.competences.model.importservice.ExercizerStudent;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.storage.Storage;

import java.util.List;

public abstract class ImportFile implements Import {

    protected final Storage storage;
    protected final HttpServerRequest request;

    protected ImportFile(HttpServerRequest request, Storage storage) {
        this.request = request;
        this.storage = storage;
    }

    public Future<Buffer> processImportFile() {
        Promise<Buffer> promise = Promise.promise();
        FileHelper.addFile(storage, request)
                .compose(res -> {
                    String fileId = res.getString("_id");
                    return readImportedFile(fileId);
                })
                .onSuccess(res -> {
                    promise.complete(res);
                })
                .onFailure(err -> {
                    promise.fail(err.getMessage());
                });
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
