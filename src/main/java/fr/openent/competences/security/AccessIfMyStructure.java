package fr.openent.competences.security;

import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AccessIfMyStructure implements ResourcesProvider {

    @Override
    public void authorize (HttpServerRequest httpServerRequest, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {

            String structureId = WorkflowActionUtils.getParamStructure(httpServerRequest);
            handler.handle(structureId != null && userInfos.getStructures().contains(structureId));
    }
}
