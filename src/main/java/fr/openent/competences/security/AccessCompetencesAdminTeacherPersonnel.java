package fr.openent.competences.security;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import static fr.openent.competences.Competences.ID_ELEVE_KEY;
import static fr.openent.competences.Competences.ID_ETABLISSEMENT_KEY;

public class AccessCompetencesAdminTeacherPersonnel implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String etablissementId = request.params().get(Field.ID_ETABLISSEMENT);
        Boolean isTeacher = Field.TEACHER.equals(user.getType());
        Boolean isPersonnel = Field.PERSONNEL.equals(user.getType());
        Boolean isAdmin = WorkflowActionUtils.hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());
        Boolean haveAccess = WorkflowActionUtils.hasRight(user, WorkflowActions.COMPETENCES_ACCESS.toString());

        if(isTeacher || isPersonnel || isAdmin) {
            handler.handle(haveAccess && user.getStructures().contains(etablissementId));
        } else {
            handler.handle(false);
        }
    }
}
