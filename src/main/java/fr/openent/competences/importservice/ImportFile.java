package fr.openent.competences.importservice;

import fr.openent.competences.helpers.FileHelper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;

public abstract class ImportFile implements Import {

    Storage storage;
    HttpServerRequest request;

    protected ImportFile(HttpServerRequest request) {
        this.request = request;
    }

    public Future<JsonObject> process() {
        Promise<JsonObject> promise = Promise.promise();
        FileHelper.addFile(storage, request)
                .compose(res -> {
                    String fileId = res.getString("_id");
                    String filename = res.getJsonObject("metadata").getString("filename");
                    return Future.succeededFuture();
                })
                .onSuccess(res -> {

                })
                .onFailure(err -> {

                });
        return promise.future();
    }
}
