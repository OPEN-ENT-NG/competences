package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.security.AccessIfMyStructure;
import fr.openent.competences.service.SubTopicService;
import fr.openent.competences.service.impl.DefaultSubTopicService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import static fr.openent.competences.constants.Field.IDSTRUCTURE;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class SubTopicController extends ControllerHelper {

    SubTopicService subTopicService;
    public SubTopicController() {
        this.subTopicService = new DefaultSubTopicService(Competences.COMPETENCES_SCHEMA, Field.SERVICE_SUBTOPIC);
    }

    @Post("/subtopics/services/update")
    @ApiDoc("set SubtopicsServices")
    @SecuredAction(value = "competences.paramSubtopics", type = ActionType.WORKFLOW)
    public void updateSubtopicsServices(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request,pathPrefix +
                Competences.SCHEMA_SUBTOPIC_COEFF_UPDATE, body -> subTopicService.upsertCoefficent(body,arrayResponseHandler(request)));
    }

    @Get("/subtopics/services/:idStructure")
    @ApiDoc("get SubtopicsServices")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    public void getDefaultSubtopicsServices(final HttpServerRequest request) {
        String idStructure = request.params().get(IDSTRUCTURE);
        subTopicService.getSubtopicServices(idStructure)
                .onSuccess(res -> {
                    renderJson(request, res);
                })
                .onFailure(err -> {
                    renderError(request);
                });
    }
}
