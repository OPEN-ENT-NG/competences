package fr.openent.competences.security.modelbulletinrights;

import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import static fr.openent.competences.security.utils.WorkflowActionUtils.hasRight;

public class GetModelExportBulletin implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        // Verify if idStructure front equals idStructure user && user right export.bulletins.periodique
        String structureId = request.getParam(Field.STRUCTUREID);
        if (structureId == null) {
            handler.handle(false);
            return;
        }

        handler.handle(new FilterUserUtils(user,null).validateStructure(structureId) && hasRight(user, WorkflowActions.ACCESS_MODEL_BULLETIN.toString()));
    }
}
