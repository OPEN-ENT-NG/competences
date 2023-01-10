package fr.openent.competences.security;

import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class CreateAvisConseilBilanPeriodiqueEtablissementId implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        final String idStructure = request.params().get(Field.ID_ETABLISSEMENT);
        final boolean isInStructure = user.getStructures().contains(idStructure);
        handler.handle(isInStructure && WorkflowActionUtils.hasRight(user, WorkflowActions.CREATE_AVIS_CONSEIL_BILAN_PERIODIQUE.toString()));
    }
}
