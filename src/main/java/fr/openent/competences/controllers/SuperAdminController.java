package fr.openent.competences.controllers;

import fr.openent.competences.service.MongoExportService;
import fr.openent.competences.service.MongoService;
import fr.openent.competences.service.impl.DefaultMongoExportService;
import fr.openent.competences.service.impl.DefaultMongoService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.response.DefaultResponseHandler;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class SuperAdminController extends ControllerHelper {
    private final MongoExportService mongoExportService;
    public SuperAdminController() {
        super();
        this.mongoExportService = new DefaultMongoExportService();
    }

    @Get("/admin/archives/report")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Render SuperAdmin view")
    public void setting(HttpServerRequest request) {
        renderView(request, null, "admin-parameter.html", null);
    }

    @Get("/admin/exports/logs")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("get exports logs")
    public void getExports(HttpServerRequest request){
        mongoExportService.getErrorExport(arrayResponseHandler(request));
    }
}