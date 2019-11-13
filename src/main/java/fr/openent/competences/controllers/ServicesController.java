/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.security.utils.ParamServicesRight;
import fr.openent.competences.service.ServicesService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultServicesService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.Map;

import static fr.openent.competences.service.impl.DefaultExportService.COEFFICIENT;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ServicesController extends ControllerHelper {

    /**
     * Déclaration des services
     */
    private ServicesService servicesConfigurationService;
    private UtilsService utilsService;

    public ServicesController() {
        this.utilsService = new DefaultUtilsService();
        this.servicesConfigurationService = new DefaultServicesService();
    }

    @Get("/services")
    @ApiDoc("Récupère les services")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getDefaultServices(final HttpServerRequest request) {

        if (!request.params().contains("idEtablissement")) {
            log.error("Error : idEtablissement should be provided.");
            badRequest(request, "idEtablissement is null");
            return;
        } else {
            JsonObject action = new JsonObject()
                    .put("action", "matiere.getAllMatieresEnseignants")
                    .put("idStructure", request.getParam("idEtablissement"));

            log.debug("DEBUT Appel bus");
            eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                    handlerToAsyncHandler(message -> {
                        JsonObject body = message.body();
                        log.debug("FIN Appel bus");
                        if ("ok".equals(body.getString("status"))) {
                            JsonArray matiere = body.getJsonArray("results");

                            Handler<Either<String, JsonArray>> handlerOverwrite = getserviceHandler(utilsService.flatten(matiere, "idClasses"), arrayResponseHandler(request));

                            log.debug("DEBUT Appel SQL");
                            servicesConfigurationService.getServices(request.getParam("idEtablissement"), getParams(request), handlerOverwrite);
                        }
                    }));
        }
    }

    @Post("/service")
    @ApiDoc("Crée un nouveau service")
    @SecuredAction(value = Competences.PARAM_SERVICES_RIGHT, type = ActionType.WORKFLOW)
    public void createService(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_SERVICE, oService -> servicesConfigurationService.createService(oService, defaultResponseHandler(request)));
    }

    @Put("/service")
    @ApiDoc("Met à jour un service")
    @ResourceFilter(ParamServicesRight.class)
    public void updateService(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + Competences.SCHEMA_SERVICE, oService ->
                servicesConfigurationService.createService(oService, defaultResponseHandler(request)));
    }

    @Delete("/service")
    @ApiDoc("Supprime un service")
    @ResourceFilter(ParamServicesRight.class)
    public void deleteService(final HttpServerRequest request) {

        if (!request.params().contains("id_groupe") || !request.params().contains("id_enseignant") || !request.params().contains("id_matiere")) {
            log.error("Error : id_groupe, id_enseignant and id_matiere should be given");
            badRequest(request, "id_groupe or id_enseignant or id_matiere is null");
            return;
        } else {
            servicesConfigurationService.deleteService(getParams(request), defaultResponseHandler(request));
        }
    }

    private JsonObject getParams (HttpServerRequest request) {
        JsonObject oService = new JsonObject();

        if (request.params().contains("id_groupe")) {
            oService.put("id_groupe", request.getParam("id_groupe"));
        }
        if (request.params().contains("id_matiere")) {
            oService.put("id_matiere", request.getParam("id_matiere"));
        }
        if (request.params().contains("id_enseignant")) {
            oService.put("id_enseignant", request.getParam("id_enseignant"));
        }
        if (request.params().contains("modalite")) {
            oService.put("modalite", request.getParam("modalite").charAt(0));
        }
        if (request.params().contains("evaluable")) {
            oService.put("evaluable", Boolean.parseBoolean(request.getParam("evaluable")));
        }
        if (request.params().contains("order")) {
            oService.put("order", Integer.parseInt(request.getParam("order")));
        }

        return oService;
    }


    private Handler<Either<String, JsonArray>> getserviceHandler(JsonArray aDBService, Handler<Either<String, JsonArray>> requestHandler) {

        return stringJsonArrayEither -> {
            log.debug("FIN Appel SQL");
            log.debug("DEBUT getserviceHandler");
            if(stringJsonArrayEither.isRight()) {
                JsonArray aParamService = stringJsonArrayEither.right().getValue();
                JsonArray result = new JsonArray();

                for (Object o : aDBService) {
                    JsonObject oDBService = normalizeMatiere((JsonObject) o);

                    JsonObject criteria = new JsonObject();

                    criteria.put("id_matiere", oDBService.getString("id_matiere"));
                    criteria.put("id_enseignant", oDBService.getString("id_enseignant"));
                    criteria.put("id_groupe", oDBService.getString("id_groupe"));


                    JsonObject overwrittenService = utilsService.findWhere(aParamService, criteria);

                    if (overwrittenService != null) {
                        aParamService.remove(overwrittenService);
                        oDBService.put("modalite", overwrittenService.getString("modalite"));
                        oDBService.put("evaluable", overwrittenService.getBoolean("evaluable"));
                        oDBService.put(COEFFICIENT, overwrittenService.getLong(COEFFICIENT));
                        oDBService.put("isManual", false);
                        result.add(oDBService);
                    } else {
                        oDBService.put("modalite", "S");
                        oDBService.put("evaluable", true);
                        oDBService.put("isManual", false);
                        oDBService.put(COEFFICIENT, 1);
                        result.add(oDBService);
                    }
                }

                for (Object o : aParamService) {
                    JsonObject oParamService = (JsonObject) o;
                    oParamService.put("isManual", true);
                    result.add(oParamService);
                }

                requestHandler.handle(new Either.Right<>(result));
            } else {
                requestHandler.handle(stringJsonArrayEither.left());
            }
            log.debug("FIN getserviceHandler");
        };
    }

    private JsonObject normalizeMatiere (JsonObject matiere) {
        JsonObject finalMatiere = new JsonObject();
        for(Map.Entry<String, Object> value : matiere.getMap().entrySet()) {
            switch (value.getKey()) {
                case "id":
                    finalMatiere.put("id_matiere", value.getValue());
                    break;
                case "idClasses":
                    finalMatiere.put("id_groupe", value.getValue());
                    break;
                case "idEnseignant":
                    finalMatiere.put("id_enseignant", value.getValue());
                    break;
                case "idEtablissement":
                    finalMatiere.put("id_etablissement", value.getValue());
                    break;
            }
        }
        return finalMatiere;
    }
}
