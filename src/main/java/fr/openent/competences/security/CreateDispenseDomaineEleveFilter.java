package fr.openent.competences.security;

import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

import java.util.List;

public class CreateDispenseDomaineEleveFilter implements ResourcesProvider{
    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler)
    {
        // Si On est autorisé d'accès si on a le droit
        // Pour les profs le filtre se fera côté controlleur
       handler.handle(new WorkflowActionUtils()
               .hasRight(user, WorkflowActions.CREATE_DISPENSE_DOMAINE_ELEVE.toString())
       || "Teacher".equals(user.getType()));
    }
}
