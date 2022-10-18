package fr.openent.competences.importservice;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class ExercizerImportNote extends ImportFile {

    public ExercizerImportNote(HttpServerRequest request) {
        super(request);
    }

    @Override
    public Future<JsonObject> run() {
        Promise<JsonObject> promise = Promise.promise();
        super.process()
                .compose(resFile -> {
                    // read en CSV avec tes colonnes spécifiques (p-e à externaliser dans ImportFile)
                    return Future.succeededFuture();
                })
                .onSuccess(res -> {

                })
                .onFailure(err -> {

                });
        return promise.future();
    }
}
