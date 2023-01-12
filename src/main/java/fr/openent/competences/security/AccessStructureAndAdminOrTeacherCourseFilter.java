package fr.openent.competences.security;

import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AccessStructureAndAdminOrTeacherCourseFilter implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        boolean isAdmin = WorkflowActionUtils.hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());
        boolean isInStructure = false;
        boolean isTeacherWhomClassBelong = false;

        //Check if the user is in the structure
        if(request.params().contains(Field.IDETABLISSEMENT)){
            isInStructure = user.getStructures().contains(request.getParam(Field.IDETABLISSEMENT));
        }

        //Check if the user is a teacher, if yes check if the class belongs to this teacher
        if(request.params().contains(Field.IDCLASSE) && request.params().contains(Field.IDENSEIGNANT)){
            if(user.getType().equals(Field.TEACHER)){
                isTeacherWhomClassBelong = user.getClasses().contains(request.getParam(Field.IDCLASSE));
            }
        }

        handler.handle(isInStructure &&
                (isAdmin
                || isTeacherWhomClassBelong));
    }
}
