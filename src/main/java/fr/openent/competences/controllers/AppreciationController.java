package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.security.*;
import fr.openent.competences.service.AppreciationService;
import fr.openent.competences.service.impl.DefaultAppreciationService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

/**
 * Created by anabah on 01/03/2017.
 */
public class AppreciationController extends ControllerHelper {




    /**
     * Déclaration des services
     */
    private final AppreciationService appreciationService;

    public AppreciationController() {
        appreciationService = new DefaultAppreciationService(Competences.COMPETENCES_SCHEMA,
                Competences.APPRECIATIONS_TABLE);
    }




    /**
     * Créer une appreciation avec les données passées en POST
     * @param request
     */
    @Post("/appreciation")
    @ApiDoc("Créer une appreciation")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(CreateEvaluationWorkflow.class)
    public void create(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    String validator = pathPrefix + Competences.SCHEMA_APPRECIATIONS_CREATE;
                    RequestUtils.bodyToJson(request, validator,
                            new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            appreciationService.createAppreciation(resource, user, notEmptyResponseHandler(request));
                        }
                    });
                }else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    /**
     * Modifie une appreciation avec les données passées en PUT
     * @param request
     */
    @Put("/appreciation")
    @ApiDoc("Modifie une appreciation")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AccessAppreciationFilter.class)
    public void update(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    String validator = pathPrefix + Competences.SCHEMA_APPRECIATIONS_UPDATE;
                    RequestUtils.bodyToJson(request,validator , new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resource) {
                            appreciationService.updateAppreciation(resource, user, defaultResponseHandler(request));
                        }
                    });
                }else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }
    /**
     * Supprime l'appreciation passée en paramètre
     * @param request
     */
    @Delete("/appreciation")
    @ApiDoc("Supprimer une appréciation donnée")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AccessAppreciationFilter.class)
    public void deleteAppreciationDevoir(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){

                    Long idAppreciation;
                    try {
                        idAppreciation = Long.parseLong(request.params().get("idAppreciation"));
                    } catch(NumberFormatException e) {
                        log.error("Error : idAppreciation must be a long object", e);
                        badRequest(request, e.getMessage());
                        return;
                    }

                    appreciationService.deleteAppreciation(idAppreciation, user, defaultResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }



}

