package fr.openent.competences.security.modelbulletinrights;

import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import static fr.openent.competences.security.utils.WorkflowActionUtils.hasRight;

public class AccessExportModelBulletinStructureId implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String structureId = WorkflowActionUtils.getParamStructure(request);
        if(structureId == null){
            handler.handle(false);
        } else {
            handler.handle(user.getStructures().contains(structureId) && hasRight(user, WorkflowActions.ACCESS_EXPORT_BULLETIN.toString()));
        }

    }
}

