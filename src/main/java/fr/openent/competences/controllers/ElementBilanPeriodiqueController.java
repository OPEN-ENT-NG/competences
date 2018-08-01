package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.security.CreateElementBilanPeriodique;
import fr.openent.competences.service.impl.*;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

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
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
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

    /**
     * Retourne les thématiques correspondantes au type passé en paramètre
     * @param request
     */
    @Get("/thematique")
    @ApiDoc("Retourne les thématiques correspondantes au type passé en paramètre")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void getThematiques(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.getThematiqueBilanPeriodique(
                            Long.parseLong(request.params().get("type")),
                            arrayResponseHandler(request));
                }else{
                    unauthorized(request);
                }
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
