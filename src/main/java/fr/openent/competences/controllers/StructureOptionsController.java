package fr.openent.competences.controllers;

import fr.openent.competences.constants.Field;
import fr.openent.competences.security.AdministratorRight;
import fr.openent.competences.service.StructureOptionsService;
import fr.openent.competences.service.impl.DefaultStructureOptions;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class StructureOptionsController extends ControllerHelper {

    protected static final Logger log = LoggerFactory.getLogger(StructureOptionsController.class);

    private final StructureOptionsService structureOptionService;

    public StructureOptionsController () {
        this.structureOptionService = new DefaultStructureOptions(eb);
    }

    @Get("/structure/:structureId/options/isSkillAverage")
    @ApiDoc(" create and update structure_ options isAverableSkills")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getStructureOptionIsAverage (HttpServerRequest request) {
        String structureId = request.getParam(Field.STRUCTUREID);
        structureOptionService.getIsAverageSkills(structureId, defaultResponseHandler(request));
    }

    @Post("/structure/options/isSkillAverage")
    @ApiDoc(" create and update structure_options isSkillAverage")
    @ResourceFilter(AdministratorRight.class)
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    public void createOrUpdateIsAverageSkills (final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, user -> {
            if( user != null) {
                RequestUtils.bodyToJson(request,pathPrefix + Field.SCHEMA_EVAL_CREATEORUPDATESTRUCTUREOPTIONISAVERAGESKILLS,
                        body -> {

                    if (isStructureOptionBodyInvalid(body)) {
                        badRequest(request, "params request does not valid");
                        return;
                    }
                    structureOptionService.createOrUpdateIsAverageSkills(body,
                            defaultResponseHandler(request));
                });
            } else {
                unauthorized(request, "user is not valid");
            }
        });

    }

    /**
     * Active ou désactive la récupération des absences/retards de presences sur compétences pour une structure donnée
     * @param request
     */
    @Post("/sync/presences")
    @ApiDoc("Active la récupération des absences/retards de presences sur compétences pour une structure donnée")
    @SecuredAction(value="", type = ActionType.AUTHENTICATED)
    public void activateStructureRecuperationAbsencesRetardsFromPresences(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject body) {
                        if(user != null && body.containsKey("structureId") && body.containsKey("state")){
                            final String structureId = body.getString("structureId");
                            final Boolean state = body.getBoolean("state");
                            Handler<Either<String, JsonObject>> handler = defaultResponseHandler(request);
                            structureOptionService.activeDeactiveSyncStatePresences(structureId, state, handler);
                        }else{
                            badRequest(request);
                        }
                    }
                });
            }
        });
    }


    /**
     * Retourne l'activation de la structure du module présences ainsi que l'activation de la récupération des retards/absences du module présence
     * @param request
     */
    @Get("/init/sync/presences")
    @ApiDoc("Retourne la liste des identifiants des structures de l'utilisateur la récupération des absences/retards du module presences est activée")
    @SecuredAction(value="", type = ActionType.AUTHENTICATED)
    public void initRecuperationAbsencesRetardsFromPresences(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null && request.params().contains("structureId")){
                    final String structureId = request.params().get("structureId");
                    // Récupération de l'état d'activation du module présences de l'établissement
                    Future<JsonObject> activationFuture = Future.future();
                    structureOptionService.getActiveStatePresences(structureId,event -> formate(activationFuture,event));

                    // Récupération de l'état de la récupération des données du modules présences
                    Future<JsonObject> syncFuture = Future.future();
                    structureOptionService.getSyncStatePresences(structureId,event -> formate(syncFuture,event));

                    CompositeFuture.all(syncFuture, activationFuture).setHandler(
                            event -> {
                                if(event.failed()){
                                    String error = event.cause().getMessage();
                                    log.error("[initRecuperationAbsencesRetardsFromPresences] : " + error);
                                    badRequest(request, "[initRecuperationAbsencesRetardsFromPresences] : " + error);
                                } else{
                                    JsonObject activationState = activationFuture.result();
                                    JsonObject syncState = syncFuture.result();
                                    JsonObject result = activationState.mergeIn(syncState);
                                    Renders.renderJson(request, result);
                                }
                            });
                }else{
                    badRequest(request);
                }
            }
        });
    }

    private boolean isStructureOptionBodyInvalid (JsonObject body) {
        return !body.containsKey(Field.STRUCTUREID) && !body.containsKey(Field.ISSKILLAVERAGE);
    }



}
