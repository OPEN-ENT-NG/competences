package fr.openent.competences.security;

import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.List;

public class CreateDispenseDomaineEleveFilter implements ResourcesProvider{
    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {

       handler.handle(new WorkflowActionUtils().hasRight(user, WorkflowActions.CREATE_DISPENSE_DOMAINE_ELEVE.toString()));
    }
}
