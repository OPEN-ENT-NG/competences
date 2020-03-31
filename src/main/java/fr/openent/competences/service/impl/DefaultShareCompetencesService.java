package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.DevoirService;
import fr.openent.competences.service.ShareCompetencesService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.share.ShareService;
import org.entcore.common.share.impl.SqlShareService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultShareCompetencesService implements ShareCompetencesService {
    private DevoirService devoirService ;

    public DefaultShareCompetencesService(EventBus eb , Map<String, SecuredAction> securedActions){
        this.devoirService = new DefaultDevoirService(eb);
    }
    protected static final Logger log = LoggerFactory.getLogger(ShareCompetencesService.class);
    @Override
    public void shareHomeworks(JsonArray idsArray, Handler<Either<String, JsonArray>> jsonArrayBusResultHandler, ShareService shareService) {
        for(int i = 0 ; i< idsArray.size();i++){
            JsonArray ids = idsArray.getJsonArray(i);
            String userIdSecondTeacher = ids.getString(0);
            String userIdMainTeacher = ids.getString(1);
            String subjectId = ids.getString(2);
            List<String> actions = new ArrayList<String>();
            actions.add(Competences.DEVOIR_ACTION_UPDATE);

            devoirService.getHomeworksFromSubjectAndTeacher(subjectId, userIdMainTeacher, new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if(event.isRight()){
                        JsonArray results = event.right().getValue();
                        List<Future> futures = new ArrayList<>();

                        for(int i = 0 ; i < results.size(); i++) {
                            Future<Boolean>  shareHomeworkFuture = Future.future();
                            futures.add(shareHomeworkFuture);
                            Long homeworkId = results.getJsonObject(i).getLong("id");
                            shareService.userShare(userIdSecondTeacher,
                                    userIdMainTeacher,
                                    homeworkId.toString(),
                                    actions, getHandlerJsonObject(shareHomeworkFuture));

                        }
                        CompositeFuture.all(futures).setHandler(result -> {
                            if (result.succeeded()) {
                                jsonArrayBusResultHandler.handle(new Either.Right<>(new JsonArray().add(results.size())));
                            }else{
                                jsonArrayBusResultHandler.handle(new Either.Left<>("Error when gettings subjects and classes"));
                            }
                        });
//
//                        }
                    }else{
                        log.error("Error when getting devoirs from POSTGRES");
                    }
                }
            });
            log.info(subjectId + " " + userIdMainTeacher + " "  + userIdSecondTeacher);
        }
//        String userIdTitulaire = ((JsonObject)values.getJsonObject(0))
//                .getString("id_titulaire");

//        shareService.userShare(user.getUserId(),
//                userIdTitulaire,
//                devoirWithId.getLong("id").toString(),
//                actions, new Handler<Either<String, JsonObject>>() {
//                    @Override
//                    public void handle(Either<String, JsonObject> event) {
//                        if (event.isRight()) {
//                            renderJson(request, devoirWithId);
//                        } else {
//                            leftToResponse(request, event.left());
//                        }
//
//                    }
//                });

    }

    private Handler<Either<String, JsonObject>> getHandlerJsonObject(Future<Boolean> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.isRight());
            } else {
                future.fail(event.left().getValue());
            }
        };
    }
}
