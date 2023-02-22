package fr.openent.competences.security;

import fr.openent.competences.Competences;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AccessParamLinkGroupCycleStructure implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String structureId = WorkflowActionUtils.getParamStructure(request);
        if(structureId == null) {
            handler.handle(false);
        } else {
            handler.handle(user.getStructures().contains(structureId) && WorkflowActionUtils.hasRight(user, Competences.PARAM_LINK_GROUP_CYCLE_RIGHT.toString()));
        }
    }
}
