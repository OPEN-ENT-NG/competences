package fr.openent.competences.security;

import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AccessIfMyStructure implements ResourcesProvider {

    @Override
    public void authorize (HttpServerRequest httpServerRequest, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {

            String structureId = WorkflowActionUtils.getParamStructure(httpServerRequest);
            if (structureId == null) {
                handler.handle(false);
            } else {
                handler.handle(new FilterUserUtils(userInfos,null).validateStructure(structureId));
            }
    }
}
