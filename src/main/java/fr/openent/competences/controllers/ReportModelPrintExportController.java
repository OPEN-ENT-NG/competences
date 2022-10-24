package fr.openent.competences.controllers;

import fr.openent.competences.constants.Field;
import fr.openent.competences.enums.Common;
import fr.openent.competences.enums.report_model_print_export.ReportModelPrintExportMongo;
import fr.openent.competences.helper.ManageError;
import fr.openent.competences.model.ReportModelPrintExport;
import fr.openent.competences.security.modelbulletinrights.PostModelExportBulletin;
import fr.openent.competences.security.modelbulletinrights.UserIdModelExportBulletin;
import fr.openent.competences.security.modelbulletinrights.GetModelExportBulletin;
import fr.openent.competences.service.ReportModelPrintExportService;
import fr.openent.competences.service.impl.DefaultReportModelPrintExportService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ReportModelPrintExportController extends ControllerHelper {

    private final ReportModelPrintExportService reportModelService;

    public ReportModelPrintExportController() {
        reportModelService = new DefaultReportModelPrintExportService(ReportModelPrintExportMongo.COLLECTION_REPORT_MODEL.getString());
    }

    @Post("/report-model-print-export")
    @ApiDoc("Post report model")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PostModelExportBulletin.class)
    public void postReportModel(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "reportModelPrintExport", bodyRequest -> {
            UserUtils.getUserInfos(eb, request, user -> {
                if (!ManageError.haveUser(request, user)) {
                    return;
                }
                try {
                    ReportModelPrintExport newReportModel = createReportModelWithBodyRequest(bodyRequest);
                    newReportModel.setUserId(user.getUserId());
                    reportModelService.addReportModel(newReportModel, defaultResponseHandler(request, 201));
                } catch (Exception errorInPost) {
                    ManageError.requestFailError(request,
                            Common.ERROR.getString(), "Error during the POST report model. ",
                            errorInPost.toString());
                }
            });
        });
    }

    @Get("/reports-models-print-export/structure/:structureId")
    @ApiDoc("Get all report model by idStructure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(GetModelExportBulletin.class)
    public void getReportModel(final HttpServerRequest request) {
        String idStructure = request.getParam(Field.STRUCTUREID);
        UserUtils.getUserInfos(eb, request, user -> {
            ReportModelPrintExport newReportModel = new ReportModelPrintExport();
            try {
                newReportModel.setStructureId(idStructure);
                reportModelService.getReportModel(newReportModel, defaultResponseHandler(request));
            } catch (Exception errorUpdate) {
                ManageError.requestFailError(request, Common.ERROR.getString(),
                        "Error during the GET all report model. ", errorUpdate.toString());
            }
        });
    }

    @Put("/report-model-print-export/:idReportModel")
    @ApiDoc("Update report model")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(UserIdModelExportBulletin.class)
    public void putReportModel(final HttpServerRequest request) {
        String idReportModel = request.getParam("idReportModel");
        RequestUtils.bodyToJson(request, bodyRequest -> {
            if (idReportModel == null) {
                ManageError.requestFail(request, Common.INFO.getString(), "Id Report model is null.");
                return;
            }
            try {
                ReportModelPrintExport newReportModel = createReportModelWithBodyRequest(bodyRequest);
                newReportModel.setId(idReportModel);
                reportModelService.putReportModel(newReportModel, defaultResponseHandler(request));
            } catch (Exception errorUpdate) {
                ManageError.requestFailError(request,
                        Common.ERROR.getString(), "Error during the PUT report model. ",
                        errorUpdate.toString());
            }
        });
    }

    @Get("/reports-models-print-export/selected")
    @ApiDoc("Get report model by user selected")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getReportModelsSelected(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (!ManageError.haveUser(request, user)) {
                return;
            }
            try {
                ReportModelPrintExport newReportModel = new ReportModelPrintExport();
                newReportModel.setUserId(user.getUserId());
                reportModelService.getReportModelSelected(newReportModel, defaultResponseHandler(request));
            } catch (Exception errorUpdate) {
                ManageError.requestFailError(request, Common.ERROR.getString(),
                        "Error during the GET report model. ", errorUpdate.toString());
            }
        });
    }

    @Delete("/report-model-print-export/:idReportModel")
    @ApiDoc("Delete report model")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(UserIdModelExportBulletin.class)
    public void deleteReportModel(final HttpServerRequest request) {
        String idReportModel = request.getParam("idReportModel");
        if (idReportModel.isEmpty()) {
            ManageError.requestFail(request, Common.INFO.getString(), "Id report model is empty.");
            return;
        }
        try {
            ReportModelPrintExport newReportModel = new ReportModelPrintExport();
            newReportModel.setId(idReportModel);
            reportModelService.deleteReportModel(newReportModel, defaultResponseHandler(request));
        } catch (Exception errorUpdate) {
            ManageError.requestFailError(request,
                    Common.ERROR.getString(), "Error during the DELETE report model. ",
                    errorUpdate.toString());
        }
    }

    private ReportModelPrintExport createReportModelWithBodyRequest(JsonObject bodyRequest) {
        String structureId = bodyRequest.getString("structureId");
        String title = bodyRequest.getString("title");
        Boolean selected = bodyRequest.getBoolean("selected");
        JsonObject preferencesCheckbox = bodyRequest.getJsonObject("preferencesCheckbox");
        JsonObject preferencesText = bodyRequest.getJsonObject("preferencesText");

        return new ReportModelPrintExport(
                structureId,
                title,
                selected,
                preferencesCheckbox,
                preferencesText);
    }
}

