package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface LanguesCultureRegionaleService extends CrudService {

    /**
     * récupérer  tous les langues de culture regionaleService
     * @param handler
     */
   public void getLanguesCultureRegionaleService(Handler<Either<String, JsonArray>> handler);

}
