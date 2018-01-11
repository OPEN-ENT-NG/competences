package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

/**
 * Created by agnes.lapeyronnie on 28/11/2017.
 */
public interface EnseignementComplementService extends CrudService {

    /**
     * récupérer  tous les enseignements de compléments
     * @param handler
     */
   public void getEnseignementsComplement(Handler<Either<String, JsonArray>> handler);

}
