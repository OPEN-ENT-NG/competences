package fr.openent.competences.controllers;

import fr.openent.competences.Utils;
import fr.openent.competences.security.AccessExportBulletinFilter;
import fr.openent.competences.service.*;
import fr.openent.competences.service.impl.*;
import fr.openent.competences.utils.ArchiveUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.storage.Storage;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.utils.ArchiveUtils.ARCHIVE_BULLETIN_TABLE;
import static fr.openent.competences.utils.ArchiveUtils.generateArchiveBulletin;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ExportBulletinController extends ControllerHelper {
    private UtilsService utilsService;
    private final Storage storage;

    public ExportBulletinController(EventBus eb, Storage storage) {
        utilsService = new DefaultUtilsService(eb);
        this.storage = storage;
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


    /**
     * Genere le releve
     */
    @Get("/generate/archive/bulletin")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @ResourceFilter(SuperAdminFilter.class)
    public void archiveBulletin(final HttpServerRequest request){
        Utils.setLocale(I18n.acceptLanguage(request));
        Utils.setDomain(getHost(request));
        ArchiveUtils.generateArchiveBulletin(eb,request);
//        JsonObject action = new JsonObject()
//                .put(ACTION, ArchiveWorker.ARCHIVE_BULLETIN)
//                .put(HOST, getHost(request))
//                .put(ACCEPT_LANGUAGE, I18n.acceptLanguage(request))
//                .put(X_FORWARDED_FOR, request.headers().get(X_FORWARDED_FOR) == null)
//                .put(PATH, request.path());
//        eb.send(ArchiveWorker.class.getSimpleName(), action, Competences.DELIVERY_OPTIONS);
//        Renders.ok(request);
    }

    @Get("/archive/bulletin/:idEleve/:idPeriode/:idClasse")
    @SecuredAction(value ="", type = ActionType.AUTHENTICATED)
    @ResourceFilter(SuperAdminFilter.class)
    public void getArchive(final HttpServerRequest request){
        String idEleve = request.params().get(ID_ELEVE_KEY);
        String idClasse = request.params().get(ID_CLASSE_KEY);
        Long idPeriode = Long.valueOf(request.params().get(ID_PERIODE_KEY));
        Boolean isCycle = false;
        ArchiveUtils.getArchiveBulletin(idEleve, idClasse, idPeriode, storage, ARCHIVE_BULLETIN_TABLE, isCycle, request);
    }

    @Get("/delete/archive/bulletin")
    @SecuredAction(value ="", type = ActionType.AUTHENTICATED)
    @ResourceFilter(SuperAdminFilter.class)
    public void deleteArchive(final HttpServerRequest request){
        ArchiveUtils.deleteAll(ARCHIVE_BULLETIN_TABLE, storage,  response -> Renders.renderJson(request, response));
    }

    @Post("/generate/archive/bulletin")
    @ApiDoc("Retourne tous les types de devoir par etablissement")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessExportBulletinFilter.class)
    public void ArvhiveBulletinPost(final HttpServerRequest request) {
        Utils.setLocale(I18n.acceptLanguage(request));
        Utils.setDomain(getHost(request));
        generateArchiveBulletin(eb,request);
//        RequestUtils.bodyToJson(request, body -> {
//            JsonArray idStructures = body.getJsonArray(ID_STRUCTURES_KEY);
//            JsonObject action = new JsonObject()
//                    .put(ACTION, ArchiveWorker.ARCHIVE_BULLETIN)
//                    .put(HOST, getHost(request))
//                    .put(ACCEPT_LANGUAGE, I18n.acceptLanguage(request))
//                    .put(X_FORWARDED_FOR, request.headers().get(X_FORWARDED_FOR) == null)
//                    .put(PATH, request.path())
//                    .put(ID_STRUCTURES_KEY, idStructures);
//            eb.send(ArchiveWorker.class.getSimpleName(), action, Competences.DELIVERY_OPTIONS);
//            Renders.ok(request);
//        });
    }

    @Get("/archive/bulletin/:idStructure")
    @ApiDoc("télécharge l archive d'un étab")
    @SecuredAction(value = "",type = ActionType.AUTHENTICATED)
    @ResourceFilter(SuperAdminFilter.class)
    public  void getArchiveBulletin(final  HttpServerRequest request){
        String idStructure = request.params().get("idStructure");
        ArchiveUtils.getArchiveBulletinZip(idStructure,request,eb,storage, vertx);
//        request.response().setStatusCode(201).end();

    }

}
