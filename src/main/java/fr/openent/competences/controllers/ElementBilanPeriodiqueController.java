package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.security.CreateElementBilanPeriodique;
import fr.openent.competences.service.impl.*;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class ElementBilanPeriodiqueController extends ControllerHelper {

    private final DefaultElementBilanPeriodiqueService defaultElementBilanPeriodiqueService;

    public ElementBilanPeriodiqueController() {
        defaultElementBilanPeriodiqueService = new DefaultElementBilanPeriodiqueService();
    }

    /**
     * Créer une thématique avec les données passées en POST
     * @param request
     */
    @Post("/thematique")
    @ApiDoc("Créer une thématique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void createThematique(final HttpServerRequest request){

        RequestUtils.bodyToJson(request, pathPrefix +
                Competences.SCHEMA_THEMATIQUE_BILAN_PERIODIQUE, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject resource) {
                defaultElementBilanPeriodiqueService.insertThematiqueBilanPeriodique(resource, defaultResponseHandler(request));
            }
        });

    }

    @Post("/elementBilanPeriodique")
    @ApiDoc("Créer une élément bilan périodique")
    @SecuredAction(value= "create.element.bilan.periodique",type= ActionType.WORKFLOW)
    public void createElementBilanPeriodique(final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_DISPENSEDOMAINE_ELEVE_CREATE, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject dispenseDomaineEleve) {
//                dispenseDomaineEleveService.createDispenseDomaineEleve(dispenseDomaineEleve,defaultResponseHandler(request));

            }

        });
    }

}
