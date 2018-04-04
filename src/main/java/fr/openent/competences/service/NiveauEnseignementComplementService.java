package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by agnes.lapeyronnie on 28/11/2017.
 */
public interface NiveauEnseignementComplementService extends CrudService {


    public void createEnsCplByELeve(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);


   public void updateEnsCpl(Integer id, JsonObject data, Handler<Either<String, JsonObject>> handler);


    public void getNiveauEnsCplByEleve(String[] idsEleve, Handler<Either<String, JsonArray>> handler);

    public void listNiveauCplByEleves(String[] idsEleve, Handler<Either<String, JsonArray>> handler);
}
