package fr.openent.competences.service.share.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.service.DevoirService;
import fr.openent.competences.service.impl.DefaultDevoirService;
import fr.openent.competences.service.share.ShareCompetencesService;
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

public class DefaultShareCompetencesService implements ShareCompetencesService {
    private DevoirService devoirService;

    public DefaultShareCompetencesService(EventBus eb, Map<String, SecuredAction> securedActions) {
        this.devoirService = new DefaultDevoirService(eb);
    }

    protected static final Logger log = LoggerFactory.getLogger(ShareCompetencesService.class);

    @Override
    public void shareHomeworks(JsonArray idsArray, Handler<Either<String, JsonArray>> jsonArrayBusResultHandler, ShareService shareService) {
        for (int i = 0; i < idsArray.size(); i++) {
            JsonArray ids = idsArray.getJsonArray(i);
            String userIdSecondTeacher = ids.getString(0);
            String userIdMainTeacher = ids.getString(1);
            String subjectId = ids.getString(2);
            String groupId = ids.getString(3);
            List<String> actions = new ArrayList<String>();
            actions.add(Competences.DEVOIR_ACTION_UPDATE);
            List<Future> futures = new ArrayList<>();
            Future<JsonArray> getSecondTeacherHomewokFuture = Future.future();
            Future<JsonArray> getMainTeacherHomewokFuture = Future.future();

            futures.add(getMainTeacherHomewokFuture);
            futures.add(getSecondTeacherHomewokFuture);
//
            devoirService.getHomeworksFromSubjectAndTeacher(subjectId, userIdMainTeacher, groupId,
                    getShareHandler(getHandlerJsonArray(getSecondTeacherHomewokFuture), userIdSecondTeacher, actions));
            devoirService.getHomeworksFromSubjectAndTeacher(subjectId, userIdSecondTeacher, groupId,
                    getShareHandler(getHandlerJsonArray(getMainTeacherHomewokFuture), userIdMainTeacher, actions));

            CompositeFuture.all(futures).setHandler(eventFuture -> {
                jsonArrayBusResultHandler.handle(new Either.Right(new JsonArray(eventFuture.result().list())));
            });
        }
    }

    private Handler<Either<String, JsonArray>> getShareHandler(Handler<Either<String, JsonArray>> jsonArrayBusResultHandler, String userIdSecondTeacher,
                                                               List<String> actions) {
        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray results = event.right().getValue();
                    JsonArray statements = new JsonArray();
                    for (int i = 0; i < results.size(); i++) {
                        Long homeworkId = results.getJsonObject(i).getLong("id");
                        statements.add(devoirService.getNewShareStatements(userIdSecondTeacher, homeworkId.toString(), actions));
                    }
                    if (statements.size() > 0)
                        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(jsonArrayBusResultHandler));
                    else {
                        jsonArrayBusResultHandler.handle(new Either.Right<>(new JsonArray()));
                    }
                } else {
                    log.error("Error when getting devoirs from POSTGRES");
                }
            }
        };
    }

    /*private JsonObject getNewShareStatements(String userIdSecondTeacher, String devoirID, List<String> actions) {
        String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".devoirs_shares (member_id ,resource_id,action)" +
                "VALUES (?,?,?) ON CONFLICT DO NOTHING";
        JsonArray paramsDevoirShare = new fr.wseduc.webutils.collections.JsonArray();
        paramsDevoirShare.add(userIdSecondTeacher).add(devoirID).add(actions.get(0));
        return new JsonObject()
                .put("statement", query)
                .put("values",paramsDevoirShare)
                .put("action", "prepared");
    }*/

    @Override
    public void removeShareHomeworks(JsonArray idsArray, Handler<Either<String, JsonArray>> jsonArrayBusResultHandler, ShareService shareService) {
        for (int i = 0; i < idsArray.size(); i++) {
            JsonArray ids = idsArray.getJsonArray(i);
            String userIdSecondTeacher = ids.getString(0);
            String userIdMainTeacher = ids.getString(1);
            String subjectId = ids.getString(2);
            String groupId = ids.getString(3);
            List<String> actions = new ArrayList<String>();
//            actions.add(Competences.DEVOIR_ACTION_UPDATE);
            devoirService.getHomeworksFromSubjectAndTeacher(subjectId, userIdMainTeacher, groupId, getRemoveShareHandler(jsonArrayBusResultHandler,
                    userIdSecondTeacher
            ));
        }
    }

    private Handler<Either<String, JsonArray>> getRemoveShareHandler(Handler<Either<String, JsonArray>> jsonArrayBusResultHandler,
                                                                     String userIdSecondTeacher) {
        return event -> {
            if (event.isRight()) {
                JsonArray results = event.right().getValue();
                JsonArray statements = new JsonArray();
                for (int i = 0; i < results.size(); i++) {
                    Long homeworkId = results.getJsonObject(i).getLong("id");
                    statements.add(removeDevoirUserShareStatement(userIdSecondTeacher,
                            homeworkId.toString()
                    ));
                }
                if (statements.size() > 0) {
                    Sql.getInstance().transaction(statements, SqlResult.validResultHandler(response -> {
                        if (response.isRight()) {
                            jsonArrayBusResultHandler.handle(new Either.Right<>(new JsonArray().add(results.size())));
                        } else {
                            jsonArrayBusResultHandler.handle(new Either.Left<>("Error when gettings subjects and classes"));
                        }
                    }));
                } else {
                    jsonArrayBusResultHandler.handle(new Either.Right<>(new JsonArray()));
                }
            } else {
                log.error("Error when getting devoirs from POSTGRES");
            }
        };
    }

    private JsonObject removeDevoirUserShareStatement(String idUser, String homeworkId) {
        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + "." + Field.DEVOIR_SHARE_TABLE +
                " WHERE member_id = ? AND resource_id = ? ";
        JsonArray params = new JsonArray().add(idUser).add(homeworkId);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
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
