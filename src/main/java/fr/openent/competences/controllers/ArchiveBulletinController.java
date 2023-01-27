package fr.openent.competences.controllers;

import fr.openent.competences.security.HasExportLSURight;
import fr.openent.competences.service.ArchiveService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultArchiveBulletinService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class ArchiveBulletinController extends ControllerHelper {
    private UtilsService utilsService;
    private final ArchiveService archiveService;

    public ArchiveBulletinController(EventBus eb) {
        utilsService = new DefaultUtilsService(eb);
        archiveService = new DefaultArchiveBulletinService();
    }

    @Get("/archive-bulletin")
    @ApiDoc("Retourne les archives de bulletins d'un établissement donné.")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(HasExportLSURight.class)
    public void getArchivesBulletins(final HttpServerRequest request) {
        if (request.params().contains("idEtablissement")) {
            String idEtablissement = request.params().get("idEtablissement");
            archiveService.getArchives(idEtablissement, arrayResponseHandler(request));

        } else {
            Renders.badRequest(request, "Invalid parameters");
        }
    }
}
