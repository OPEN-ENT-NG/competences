package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Created by agnes.lapeyronnie on 28/11/2017.
 */
public interface EleveEnseignementComplementService extends CrudService {


    public void createEnsCplByELeve(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);


   public void updateEnsCpl(Integer id, JsonObject data, Handler<Either<String, JsonObject>> handler);


    public void getNiveauEnsCplByEleve(String idsEleve, Long idCycle, Handler<Either<String, JsonObject>> handler);

    public void listNiveauCplByEleves(String[] idsEleve, Handler<Either<String, JsonArray>> handler);
}
