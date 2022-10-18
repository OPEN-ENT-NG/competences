package fr.openent.competences.importservice;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public interface Import {

    Future<Buffer> processImportFile();

    Future<JsonObject> run();
}
