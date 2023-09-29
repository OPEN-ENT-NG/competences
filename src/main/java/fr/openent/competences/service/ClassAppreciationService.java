package fr.openent.competences.service;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;
import org.entcore.common.storage.Storage;

public interface ClassAppreciationService extends CrudService {


   Future<JsonObject> getTeacherAppreciationPDF (String idClass, Integer finalIdPeriod, String language ,
                                                 String host, String idStructure, HttpServerRequest request);
}
