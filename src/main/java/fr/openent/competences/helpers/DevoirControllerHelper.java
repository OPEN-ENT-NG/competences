package fr.openent.competences.helpers;

import fr.openent.competences.Competences;
import fr.openent.competences.model.Devoir;
import fr.openent.competences.service.DevoirService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

public class DevoirControllerHelper {
    protected static final Logger log = LoggerFactory.getLogger(DevoirControllerHelper.class);

    public static void creationDevoir(HttpServerRequest request, UserInfos user, JsonObject resource, String pathPrefix,
                                      DevoirService devoirsService, ShareService shareService, EventBus eb) {
        resource.remove("competences");
        resource.remove("competencesAdd");
        resource.remove("competencesRem");
        resource.remove("competenceEvaluee");
        resource.remove("competencesUpdate");
        RequestUtils.bodyToJson(request, pathPrefix +
                Competences.SCHEMA_DEVOIRS_CREATE, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject devoirJO) {
                Devoir devoir = new Devoir(devoirJO);
                devoirsService.createDevoir(devoir.getOldModel(), user,
                        getCreationDevoirHandler(devoir,  user, shareService, request, eb));
            }
        });
    }

    private static Handler<Either<String, JsonObject>> getCreationDevoirHandler(Devoir devoir, UserInfos user, ShareService shareService, HttpServerRequest request, EventBus eb) {
        return new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    final JsonObject devoirWithId = event.right().getValue();
                    // recuperation des professeurs que l'utilisateur connecté remplacent

                    JsonObject action = new JsonObject()
                            .put("action", "multiTeaching.getIdMultiTeachers")
                            .put("subjectId", devoir.getSubjectId())
                            .put("structureId", devoir.getStructureId())
                            .put("groupId", devoir.getGroupId())
                            .put("userId", user.getUserId());
                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, getReplyHandler(devoirWithId, shareService, user, request));
                } else {
                    Renders.badRequest(request);
                }
            }
        };
    }

    public static Handler<Either<String, JsonArray>> getDuplicationDevoirHandler(UserInfos user, ShareService shareService, HttpServerRequest request, EventBus eb) {
        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    final JsonArray devoirs = event.right().getValue();
                    for(Object devoirO : devoirs) {
                        // recuperation des professeurs que l'utilisateur connecté remplacent
                        JsonObject devoirJO = (JsonObject)devoirO;
                        Devoir devoir = new Devoir(devoirJO.getJsonObject("devoir"));
                        JsonObject action = new JsonObject()
                                .put("action", "multiTeaching.getIdMultiTeachers")
                                .put("subjectId", devoir.getSubjectId())
                                .put("structureId", devoir.getStructureId())
                                .put("groupId", devoir.getGroupId())
                                .put("userId", user.getUserId());
                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, getReplyHandler(devoirJO, shareService, user, request));
                    }
                } else {
                    Renders.badRequest(request);
                }
            }
        };
    }

    private static Handler<AsyncResult<Message<JsonObject>>> getReplyHandler(JsonObject devoirWithId, ShareService shareService, UserInfos user, HttpServerRequest request) {
        return new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> event) {
                JsonArray results = event.result().body().getJsonArray("result");
                List<Future> futures = new ArrayList<>();
                List<String> actions = new ArrayList<String>();
                actions.add(Competences.DEVOIR_ACTION_UPDATE);
                for(int i = 0; i < results.size() ; i++){
                    Future<JsonObject> shareServiceFuture = Future.future();
                    futures.add(shareServiceFuture);

                    String id = results.getJsonObject(i).getString("teacher_id");
                    shareService.userShare(user.getUserId(), id, devoirWithId.getLong("id").toString(),
                        actions, getFutureHandler(shareServiceFuture));
                }
                CompositeFuture.all(futures).setHandler(futuresEvent -> {
                    if (event.succeeded()) {
                        Renders.renderJson(request, devoirWithId);
                    }else{
                        leftToResponse(request, new Either.Left<>("Error during futures in DevoirControllerHelper"));
                    }
                });
            }
        };
    }

    private static Handler<Either<String, JsonObject>> getFutureHandler(Future<JsonObject> shareServiceFuture) {
        return event -> {
            if (event.isRight()) {
                shareServiceFuture.complete(event.right().getValue());
            } else {
                shareServiceFuture.fail(event.left().getValue());
            }
        };
    }
}
