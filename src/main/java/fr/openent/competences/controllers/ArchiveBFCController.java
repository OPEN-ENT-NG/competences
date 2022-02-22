package fr.openent.competences.controllers;

import fr.openent.competences.service.ArchiveService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultArchiveBFCService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class ArchiveBFCController extends ControllerHelper {
    private UtilsService utilsService;
    private final ArchiveService archiveService;

    public ArchiveBFCController(EventBus eb) {
        utilsService = new DefaultUtilsService(eb);
        archiveService = new DefaultArchiveBFCService();
    }

    @Get("/archiveBFC")
    @ApiDoc("Retourne les archives BFC d'un établissement donné.")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getArchivesBFC(final HttpServerRequest request) {
        if (request.params().contains("idEtablissement")) {
            String idEtablissement = request.params().get("idEtablissement");
            archiveService.getArchives(idEtablissement, arrayResponseHandler(request));

        } else {
            Renders.badRequest(request, "Invalid parameters");
        }
    }
}
