package fr.openent.competences.importservice;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface Import {

    Future<JsonObject> process();

    Future<JsonObject> run();
}
