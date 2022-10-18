package fr.openent.competences.importservice;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;

public abstract class ImportFile {

    Storage storage;
    protected Future<JsonObject> run(HttpServerRequest request) {
        Promise<JsonObject> promise = Promise.promise();
        storage.writeUploadFile(request, entries -> {
            if (!"ok".equals(entries.getString("status"))) {
                return;
            }
            String fileId = entries.getString("_id");
            String filename = entries.getJsonObject("metadata").getString("filename");
            parseCsv(request, fileId, filename);
        });

        return promise.future();


        // storage fetch file via HTTP
        // Ajout du fichier (storage, read le fichier et voir ce qu'on retourne et le supprimer) + envoyer un buffer/objet exploitable
        //
        //2) Développer une classe (un outil) qui interpretera les données que la 1ere partie va envoyer
        //
        //Faire ensuite des classes/outils/models (exercizer pour commencer) pour récupérer les 3 champs et vérifier leur contenu et gérer les réponses pour les controllers/autres
    }
    protected abstract Future<JsonObject> process();

    public void process();
}
