package fr.openent.competences.service;

import fr.wseduc.webutils.Either;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

import io.vertx.core.Handler;

import java.util.Map;

public interface MatiereService extends CrudService {
     void getLibellesCourtsMatieres(Handler<Either<String, Map<String,String>>> handler);
}
