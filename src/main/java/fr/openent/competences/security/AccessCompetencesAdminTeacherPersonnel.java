package fr.openent.competences.security;

import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AccessCompetencesAdminTeacherPersonnel implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String etablissementId = request.params().get(Field.ID_ETABLISSEMENT);
        Boolean isTeacher = Field.TEACHER_PROFIL.equals(user.getType());
        Boolean isPersonnel = Field.PERSONNEL.equals(user.getType());
        Boolean isAdmin = WorkflowActionUtils.hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());
        Boolean haveAccess = WorkflowActionUtils.hasRight(user, WorkflowActions.COMPETENCES_ACCESS.toString());

        handler.handle(
                (isTeacher || isPersonnel || isAdmin) && haveAccess && user.getStructures().contains(etablissementId)
        );
    }
}
