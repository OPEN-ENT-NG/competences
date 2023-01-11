package fr.openent.competences.security;

import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import static fr.openent.competences.Competences.ID_STRUCTURE_KEY;

public class AccessSuiviClasse implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        final String idStructure = request.params().get(ID_STRUCTURE_KEY);
        handler.handle(user.getStructures().contains(idStructure) && WorkflowActionUtils.hasRight(user, WorkflowActions.ACCESS_SUIVI_CLASSE.toString()));
    }
}
