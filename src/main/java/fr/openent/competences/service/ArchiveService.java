package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface ArchiveService {
    /**
     * Récupère le nombre de bulletins dans un
     * @param idStructure
     * @param handler
     */
    void getArchives(String idStructure, Handler<Either<String, JsonArray>> handler);
}

