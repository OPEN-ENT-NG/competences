package fr.openent.competences.security.modelbulletinrights;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import fr.openent.competences.security.utils.WorkflowActions;

import static fr.openent.competences.security.utils.WorkflowActionUtils.hasRight;

public class ModelExportBulletin implements ResourcesProvider {

    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        // Verify user right export.bulletins.periodique
        handler.handle(hasRight(user, WorkflowActions.ACCESS_MODEL_BULLETIN.toString()));
    }
}
