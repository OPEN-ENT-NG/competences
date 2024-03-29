package fr.openent.competences.security.modelbulletinrights;

import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import static fr.openent.competences.security.utils.WorkflowActionUtils.hasRight;

public class AccessExportModelBulletin implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        handler.handle(hasRight(user, WorkflowActions.ACCESS_EXPORT_BULLETIN.toString()));
    }
}

