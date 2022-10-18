package fr.openent.competences.importservice;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public class ExercizerImportNote extends ImportFile {


    @Override
    public Future<JsonObject> process() {
        super.process()
                .compose(// info de ton file et après tu passes à la suitep our traiter l'info (du coup usage CSV)) parseCsv()
        Promise<JsonObject> promise = Promise.promise();
        return promise.future();
        // colonne spécifique donc à lui de traiter l'information
        // envoyer un json dans son contexte pour la 3eme partie qui sera côté SQL (donc dans le noteController/service)
        // mon fonctionnement METIER
    }
}
