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
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.openent.competences.Competences.COMPETENCES_SCHEMA;

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
            String groupId = ids.getString(3);
            List<String> actions = new ArrayList<String>();
            actions.add(Competences.DEVOIR_ACTION_UPDATE);
            List<Future> futures = new ArrayList<>();
            Future<JsonArray>  getSecondTeacherHomewokFuture = Future.future();
            Future<JsonArray>  getMainTeacherHomewokFuture = Future.future();

            futures.add(getMainTeacherHomewokFuture);
            futures.add(getSecondTeacherHomewokFuture);
//
            devoirService.getHomeworksFromSubjectAndTeacher(subjectId, userIdMainTeacher,groupId, getShareHandler(getHandlerJsonArray(getSecondTeacherHomewokFuture), userIdSecondTeacher, actions));
            devoirService.getHomeworksFromSubjectAndTeacher(subjectId,userIdSecondTeacher,groupId,  getShareHandler(getHandlerJsonArray(getMainTeacherHomewokFuture), userIdMainTeacher, actions));

            CompositeFuture.all(futures).setHandler( eventFuture -> {
                jsonArrayBusResultHandler.handle(new Either.Right(new JsonArray(eventFuture.result().list())));
            });
        }
    }

    private Handler<Either<String, JsonArray>> getShareHandler(Handler<Either<String, JsonArray>> jsonArrayBusResultHandler, String userIdSecondTeacher,
                                                               List<String> actions) {
        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if(event.isRight()){
                    JsonArray results = event.right().getValue();
                    JsonArray statements = new JsonArray();
                    for(int i = 0 ; i < results.size(); i++) {
                        Long homeworkId = results.getJsonObject(i).getLong("id");
                        statements.add(getNewShareStatements(userIdSecondTeacher,homeworkId.toString(),actions));
                    }
                    if(statements.size() > 0)
                        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(jsonArrayBusResultHandler));
                    else{
                        jsonArrayBusResultHandler.handle(new Either.Right<>(new JsonArray()));
                    }
                }else{
                    log.error("Error when getting devoirs from POSTGRES");
                }
            }
        };
    }

    private JsonObject getNewShareStatements(String userIdSecondTeacher, String devoirID, List<String> actions) {
        String query = "INSERT INTO " + COMPETENCES_SCHEMA + ".devoirs_shares (member_id ,resource_id,action)" +
                "VALUES (?,?,?) ON CONFLICT DO NOTHING";
        JsonArray paramsDeleteAnnotation = new fr.wseduc.webutils.collections.JsonArray();
        paramsDeleteAnnotation.add(userIdSecondTeacher).add(devoirID).add(actions.get(0));
        return new JsonObject()
                .put("statement", query)
                .put("values",paramsDeleteAnnotation)
                .put("action", "prepared");
    }

    @Override
    public void removeShareHomeworks(JsonArray idsArray, Handler<Either<String, JsonArray>> jsonArrayBusResultHandler, ShareService shareService) {
        for(int i = 0 ; i< idsArray.size();i++){
            JsonArray ids = idsArray.getJsonArray(i);
            String userIdSecondTeacher = ids.getString(0);
            String userIdMainTeacher = ids.getString(1);
            String subjectId = ids.getString(2);
            String groupId = ids.getString(3);
            List<String> actions = new ArrayList<String>();
//            actions.add(Competences.DEVOIR_ACTION_UPDATE);
            devoirService.getHomeworksFromSubjectAndTeacher(subjectId, userIdMainTeacher, groupId, getRemoveShareHandler(jsonArrayBusResultHandler, shareService, userIdSecondTeacher,
                    userIdMainTeacher, actions));
        }
    }

    private Handler<Either<String, JsonArray>> getRemoveShareHandler(Handler<Either<String, JsonArray>> jsonArrayBusResultHandler, ShareService shareService,
                                                                     String userIdSecondTeacher, String userIdMainTeacher, List<String> actions) {
        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if(event.isRight()){
                    JsonArray results = event.right().getValue();
                    List resultList = results.getList();
                    List<Future> futures = new ArrayList<>();
                    for(int i = 0 ; i < results.size(); i++) {
                        Future<Boolean>  removeShareHomeworkFuture = Future.future();
                        futures.add(removeShareHomeworkFuture);
                        Long homeworkId = results.getJsonObject(i).getLong("id");
                        //ON GARDE l appel service car il est rapide
                        shareService.removeUserShare(userIdSecondTeacher,
                                homeworkId.toString(),
                                actions,getHandlerJsonObject(removeShareHomeworkFuture));

                    }
                    CompositeFuture.all(futures).setHandler(result -> {
                        log.info("getRemoveShareHandler end futures");
                        if (result.succeeded()) {
                            jsonArrayBusResultHandler.handle(new Either.Right<>(new JsonArray().add(results.size())));
                        }else{
                            jsonArrayBusResultHandler.handle(new Either.Left<>("Error when gettings subjects and classes"));
                        }
                    });
                }else{
                    log.error("Error when getting devoirs from POSTGRES");
                }
            }
        };
    }
    private Handler<Either<String, JsonArray>> getHandlerJsonArray(Future<JsonArray> serviceFuture) {
        return event -> {
            if (event.isRight()) {
                serviceFuture.complete(event.right().getValue());
            } else {
                serviceFuture.fail(event.left().getValue());
            }
        };
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
