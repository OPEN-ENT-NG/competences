package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.share.ShareService;

public interface ShareCompetencesService{
    public void shareHomeworks(JsonArray idsArray, Handler<Either<String, JsonArray>> jsonArrayBusResultHandler, ShareService shareService);

    public void removeShareHomeworks(JsonArray idsArray, Handler<Either<String, JsonArray>> jsonArrayBusResultHandler, ShareService shareService);
}
