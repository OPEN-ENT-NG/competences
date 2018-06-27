package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

import java.util.List;

public interface TransitionService  extends CrudService {
    /**
     * Effectue la transition d'année pour une liste de structure
     * @param idsStructures
     * @param handler
     */
    public void transitionAnnee(EventBus eb, final List<String> idsStructures, final Handler<Either<String, JsonArray>> handler);

    /**
     * Effectue la transition d'année pour une structure
     * @param eb
     * @param structure
     * @param finalHandler
     */
    public void transitionAnneeStructure(EventBus eb, final JsonObject structure, final Handler<Either<String, JsonArray>> finalHandler);

}
