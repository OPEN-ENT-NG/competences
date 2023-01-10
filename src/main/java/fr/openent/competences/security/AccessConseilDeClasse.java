package fr.openent.competences.security;

import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AccessConseilDeClasse implements ResourcesProvider {

    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        final String idStructure = request.params().get(Field.IDSTRUCTURE);
        handler.handle(user.getStructures().contains(idStructure) &&
            WorkflowActionUtils.hasRight(user, WorkflowActions.ACCESS_CONSEIL_DE_CLASSE.toString()));
    }
}
