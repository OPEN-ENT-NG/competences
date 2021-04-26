package fr.openent.competences.security;

import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import static fr.openent.competences.Competences.ID_ELEVE_KEY;

public class AccessBulletinChildrenParentCEFilter implements ResourcesProvider {
    @Override
    public void authorize(final HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        RequestUtils.bodyToJson(request, params -> {
            handler.handle(new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString())
                    || "Personnel".equals(user.getType())
                    || "Teacher".equals(user.getType())
                    || (user.getUserId().equals(params.getString(ID_ELEVE_KEY))
                    || user.getChildrenIds().contains(params.getString(ID_ELEVE_KEY)))
            );
        });
    }
}
