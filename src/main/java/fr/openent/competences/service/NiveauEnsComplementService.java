package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface NiveauEnsComplementService extends CrudService {
    /**
     * récupère tous les niveaux d'enseignements de complément
     * @param handler portant le résultat de la requête
     */
   public void getNiveauEnsComplement(Handler<Either<String, JsonArray>> handler);

}
