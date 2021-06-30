package fr.openent.competences.controllers;

import fr.openent.competences.enums.Common;
import fr.openent.competences.helper.ManageError;
import fr.openent.competences.service.TransitionService;
import fr.openent.competences.service.impl.DefaultTransitionService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

import java.util.ArrayList;
import java.util.List;

import static fr.openent.competences.Utils.isNull;

public class YearTransitionController extends ControllerHelper {
    protected static final Logger log = LoggerFactory.getLogger(YearTransitionController.class);
    private final TransitionService transitionService;

    public YearTransitionController() {
        transitionService = new DefaultTransitionService();
    }

    @Post("/transition/before")
    @ApiDoc("processing before transition")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void beforeTransition(final HttpServerRequest request) {
        log.info("START transition before ...");
        String year = request.params().get("year");
        if (isNull(year) || year.isEmpty()) year = "backup";
        final String currentYear = year;
        transitionService.cloneSchemas(currentYear, getHandlerCloneSchema(request, currentYear));
    }

    @Post("/transition/after")
    @ApiDoc("processing after transition and alimentation")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void afterTransition(final HttpServerRequest request){
        log.info("Start transition after ...");
        List<Future> futures = new ArrayList<>();
        Future<JsonArray> future1 = Future.future();
        log.info("START clearTablePostTransition ...");
        transitionService.clearTablePostTransition(event ->{
            if(event.isRight()){
                future1.complete(event.right().getValue());
                log.info("SUCCESS clearTablePostTransition ...");
            }else{
                log.error("Problem in afterTransition in purge tables");
                log.error(event.left());
                future1.fail("Problem in afterTransition in purge tables");
            }
        });
        futures.add(future1);

        Future<JsonArray> future2 = Future.future();
        updateClassId(future2);
        futures.add(future2);

        Future<JsonArray> future3 = Future.future();
        supprimerSousMatieresNonManuelles(future3);
        futures.add(future3);

        CompositeFuture.all(futures).setHandler(
                eventFutur -> {
                    if (eventFutur.succeeded()) {
                        Renders.ok(request);
                    } else {
                        badRequest(request, eventFutur.cause().getMessage() + " -> Problem in afterTransition in purge tables");
                    }
                });
    }

    private void updateClassId(Future<JsonArray> future) {
        log.info("START updateClassId ...");
        transitionService.getOldIdClassTransition(oldIdClassEvent -> {
            if(oldIdClassEvent.isRight()){
                JsonArray externalIdsClasses = oldIdClassEvent.right().getValue();
                transitionService.matchExternalId(externalIdsClasses, matchExternalIdEvent -> {
                    if(matchExternalIdEvent.isRight()) {
                        JsonArray classesFromNeo = matchExternalIdEvent.right().getValue();
                        transitionService.updateTablesTransition(classesFromNeo, updateTableTransitionEvent -> {
                            if(updateTableTransitionEvent.isRight()){
                                future.complete(updateTableTransitionEvent.right().getValue());
                                log.info("SUCCESS updateClassId ...");
                            } else {
                                future.fail("Problem in afterTransition in updateClassId function where " +
                                        "updating the id of the class");
                                log.error("Problem afterTransition");
                            }
                        });
                    } else {
                        future.fail("Problem in afterTransition in updateClassId function where getting " +
                                "classes informations in NEO");
                        log.error("Problem afterTransition");
                    }
                });
            } else {
                future.fail("Problem in afterTransition in updateClassId function where getting oldIdClassTransition");
                log.error("Problem afterTransition");
            }
        });
    }

    private void supprimerSousMatieresNonManuelles(Future<JsonArray> future) {
        log.info("START supprimerSousMatieresNonManuelles ...");
        transitionService.getSubjectsNeo(event -> {
            if(event.isRight()){
                JsonArray matieres = event.right().getValue();
                transitionService.supprimerSousMatiereNonRattaches(matieres, eventDelete -> {
                    if(eventDelete.isRight()) {
                        future.complete(eventDelete.right().getValue());
                        log.info("SUCCESS supprimerSousMatieresNonManuelles ...");
                    } else {
                        future.fail("Problem in afterTransition supprimerSousMatieresNonManuelles");
                        log.error("Problem afterTransition");
                    }
                });
            } else {
                future.fail("Problem in afterTransition in supprimerSousMatieresNonManuelles");
                log.error("Problem afterTransition");
            }
        });
    }

    private Handler<Either<String, JsonObject>> getHandlerCloneSchema(HttpServerRequest request, String currentYear) {
        log.info("START clone Schemas ...");
        return eventClone -> {
            if (eventClone.isLeft()) {
                ManageError.requestFailError(request, Common.ERROR.getString(), "Error in cloneSchemas function ", eventClone.left().getValue());
                return;
            }
            log.info("END clone Schemas, new schemas in sql: notes_" + currentYear + " and viesco_" + currentYear);
            transitionService.cleanTableSql(getHandlerCleanTableSql(request));
        };
    }

    private Handler<Either<String, JsonArray>> getHandlerCleanTableSql(HttpServerRequest request) {
        log.info("START cleanTableSql ...");
        return eventBeforeTransition -> {
            if (eventBeforeTransition.isLeft()) {
                ManageError.requestFailError(request, Common.ERROR.getString(), "Error in beforeTransition function", eventBeforeTransition.left().getValue());
                return;
            }
            log.info("END cleanTableSql");
            transitionService.updateSqlMatchClassIdTransition(getHandlerUpdateSqlMatchClassIdTransition(request));
        };
    }

    private Handler<Either<String, JsonArray>> getHandlerUpdateSqlMatchClassIdTransition(HttpServerRequest request) {
        log.info("START update sql match_class_id_transition ...");
        return eventIdClassAndExternal -> {
            if (eventIdClassAndExternal.isLeft()) {
                ManageError.requestFailError(request, Common.ERROR.getString(), "Error in insertSqlMatchClassIdTransition function", eventIdClassAndExternal.left().getValue());
                return;
            }
            log.info("END update sql match_class_id_transition");
            // END
            request.response()
                    .setStatusCode(200)
                    .end("Before transition is OK!!!");
            log.info("END transition before");
        };
    }
}
