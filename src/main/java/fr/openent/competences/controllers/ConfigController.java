package fr.openent.competences.controllers;

import fr.openent.competences.constants.Field;
import fr.openent.competences.helper.StringHelper;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

public class ConfigController extends ControllerHelper {

    @Get("/config")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void getConfig(final HttpServerRequest request) {
        JsonObject safeConfig = config.copy();
        JsonObject nodePdfGenerator = safeConfig.getJsonObject(Field.NODEPDFGENERATOR, new JsonObject());
        nodePdfGenerator.put(Field.AUTHORIZATION, StringHelper.repeat("*",
                nodePdfGenerator.getString(Field.AUTHORIZATION, "").length()));
        safeConfig.put(Field.NODEPDFGENERATOR, nodePdfGenerator);
        renderJson(request, safeConfig);
    }
}
