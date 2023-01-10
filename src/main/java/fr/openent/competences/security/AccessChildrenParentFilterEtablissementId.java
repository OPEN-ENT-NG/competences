package fr.openent.competences.security;

import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import static fr.openent.competences.Competences.ID_ELEVE_KEY;

public class AccessChildrenParentFilterEtablissementId implements ResourcesProvider {
    @Override
    public void authorize(final HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        final String idStructure = request.params().get(Field.IDETABLISSEMENT);
        final boolean isInStructure = user.getStructures().contains(idStructure);

        boolean isAdminTeacherPersonnel = WorkflowActionUtils.hasRight(user, WorkflowActions.ADMIN_RIGHT.toString())
                || "Personnel".equals(user.getType()) || "Teacher".equals(user.getType());

        if ("GET".equals(request.method().toString())) {
            handler.handle(isAdminTeacherPersonnel
                    || isInStructure && user.getUserId().equals(request.params().get(ID_ELEVE_KEY))
                    || isInStructure && user.getChildrenIds().contains(request.params().get(ID_ELEVE_KEY))
            );
        } else {
            RequestUtils.bodyToJson(request, params -> {
                handler.handle(isAdminTeacherPersonnel
                        || isInStructure && user.getUserId().equals(params.getString(ID_ELEVE_KEY))
                        || isInStructure && user.getChildrenIds().contains(params.getString(ID_ELEVE_KEY))
                );
            });
        }
    }
}
