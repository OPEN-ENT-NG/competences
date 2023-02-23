package fr.openent.competences.security;

import fr.openent.competences.Competences;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AccessViscoParamServiceStructure implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        String structureId = WorkflowActionUtils.getParamStructure(request);
        handler.handle(structureId != null && userInfos.getStructures().contains(structureId)
                && WorkflowActionUtils.hasRight(userInfos, WorkflowActions.PARAM_SERVICES_RIGHT.toString()));
    }
}
