package fr.openent.competences.controllers;

import fr.openent.competences.security.AccessExportBulletinFilter;
import fr.openent.competences.service.*;
import fr.openent.competences.service.impl.*;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ExportBulletinController extends ControllerHelper {
    private UtilsService utilsService;

    public ExportBulletinController(EventBus eb) {
        utilsService = new DefaultUtilsService(eb);
    }



    @Post("/image/bulletins/structure")
    @ApiDoc("Met à jour l'image de la structure pour l'export des bulletins ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessExportBulletinFilter.class)
    public void setStructureImage(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, ressource -> {

            String idStructure = ressource.getString("idStructure");
            String path = ressource.getString("path");
            if(idStructure != null) {
                utilsService.setStructureImage(idStructure, path,defaultResponseHandler(request));
            }
            else {
                badRequest(request);
            }
        });

    }


    @Post("/informations/bulletins/ce")
    @ApiDoc("Retourne tous les types de devoir par etablissement")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessExportBulletinFilter.class)
    public void setInformationCE(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, ressource -> {

            String idStructure = ressource.getString("idStructure");
            String path = ressource.getString("path");
            String name = ressource.getString("name");
            if(idStructure != null) {
                utilsService.setInformationCE(idStructure, path, name, defaultResponseHandler(request));
            }
            else {
                badRequest(request);
            }
        });

    }

    @Get("/images/and/infos/bulletins/structure/:idStructure")
    @ApiDoc("Retourne tous les types de devoir par etablissement")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessExportBulletinFilter.class)
    public void getImagesAndInfosStructure(final HttpServerRequest request) {
        String idStructure = request.params().get("idStructure");
        if(idStructure != null) {
            utilsService.getParametersForExport(idStructure, defaultResponseHandler(request));
        }
        else {
            badRequest(request);
        }
    }
}
