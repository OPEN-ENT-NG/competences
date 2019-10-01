package fr.openent.competences.controllers;

import fr.openent.competences.security.AdministratorRight;
import fr.openent.competences.service.MatiereService;
import fr.openent.competences.service.impl.DefaultMatiereService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import static fr.openent.competences.Competences.*;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class MatiereController extends ControllerHelper {

    private final MatiereService matiereService;

    public MatiereController(EventBus eb) {
        matiereService = new DefaultMatiereService(eb);
    }


    @Post("/matieres/libelle/model/save")
    @ApiDoc("sauvegarde un model de libelle de matiere pour un établissement")
    @ResourceFilter(AdministratorRight.class)
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void setModel(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, ressource -> {

            String idStructure = ressource.getString(ID_STRUCTURE_KEY);
            String title = ressource.getString(TITLE);
            Long idModel = ressource.getLong(ID_KEY);
            JsonArray libelleMatiere = ressource.getJsonArray(SUBJECTS);
            if(idStructure != null) {
                matiereService.saveModel(idStructure, title, idModel, libelleMatiere, event -> {
                    if(event.isLeft()){
                        JsonObject error = (new JsonObject()).put("error", event.left().getValue());
                        Renders.renderJson(request, error, 400);
                    }
                    else {
                        Renders.renderJson(request, event.right().getValue());
                    }
                });
            }
            else {
                badRequest(request);
            }
        });

    }

    @Get("/matieres/models/:idStructure")
    @ApiDoc("Retourne les models de libellé d'un établissement")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void getModels(final HttpServerRequest request) {
        String idStructure = request.params().get(ID_STRUCTURE_KEY);
        if(idStructure != null) {
            try {

                matiereService.getModels(idStructure, null, arrayResponseHandler(request));
            }
            catch (Exception e) {
                Renders.renderError(request, new JsonObject().put("error", e.getCause()));
            }
        }
        else {
            badRequest(request);
        }
    }

    @Delete("/matieres/model/:id")
    @ResourceFilter(AdministratorRight.class)
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void deleteModel(final HttpServerRequest request) {
        matiereService.deleteModeleLibelle(request.params().get("id"), arrayResponseHandler(request));
    }

    @Get("/matieres/devoirs/update")
    @ApiDoc("ont met par défaut une sousMatiere à chaque devoir")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void updateDevoirs(final HttpServerRequest request) {
        matiereService.updateDevoirs(null, arrayResponseHandler(request));
    }
}
