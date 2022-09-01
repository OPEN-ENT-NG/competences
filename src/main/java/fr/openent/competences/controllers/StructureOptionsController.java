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
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class StructureOptionsController extends ControllerHelper {

    protected static final Logger log = LoggerFactory.getLogger(StructureOptionsController.class);

    private final StructureOptionsService structureOptionService;

    public StructureOptionsController () {
        this.structureOptionService = new DefaultStructureOptions();
    }

    @Get("/structure/options/isSkillAverage")
    @ApiDoc(" create and update structure_ options isAverableSkills")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getStructureOptionIsAverage (HttpServerRequest request) {
        if( !request.params().contains(Field.STRUCTUREID)) {
            badRequest(request, "no structureId");
        }
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

    private boolean isStructureOptionBodyInvalid (JsonObject body) {
        return !body.containsKey(Field.STRUCTUREID) && !body.containsKey(Field.ISSKILLAVERAGE);
    }



}
