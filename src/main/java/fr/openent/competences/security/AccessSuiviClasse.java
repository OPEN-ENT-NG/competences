package fr.openent.competences.security;

import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;


public class AccessSuiviClasse implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        final String idStructure = WorkflowActionUtils.getParamStructure(request);
        FilterUserUtils filter = new FilterUserUtils(user, null);
        if (idStructure == null){
            handler.handle(false);
        }else{
            handler.handle(filter.validateStructure(idStructure) && WorkflowActionUtils.hasRight(user, WorkflowActions.ACCESS_SUIVI_CLASSE.toString()));
        }
    }
}
