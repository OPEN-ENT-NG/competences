package fr.openent.competences.service;

import fr.openent.competences.model.Subject;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

import java.util.List;
import java.util.Map;
import io.vertx.core.Handler;

public interface BulletinsService{
    /**
     * Récupère le nombre de bulletins dans un
     * @param idStructure
     * @param handler
     */
    void getBulletinsCount(String idStructure, Handler<Either<String, JsonArray>> handler);
}

