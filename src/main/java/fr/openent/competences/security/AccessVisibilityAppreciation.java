package fr.openent.competences.security;

import fr.openent.competences.security.utils.FilterDevoirUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AccessVisibilityAppreciation implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(AccessAppreciationFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user, final Handler<Boolean> handler) {
        if (new WorkflowActionUtils().hasRight(user,
                WorkflowActions.ADMIN_RIGHT.toString())) {
            handler.handle(true);
        } else {
            switch (user.getType()) {
                case "Teacher": {
                    resourceRequest.pause();

                    Long idDevoir;
                    try {
                        idDevoir = Long.parseLong(resourceRequest.params().get("idDevoir"));
                    } catch (NumberFormatException e) {
                        log.error("Error : idDevoir must be a long object", e);
                        handler.handle(false);
                        return;
                    }
                    // On cheque si l'utilisateur est professeur principal sur la classe du devoir
                    new FilterDevoirUtils().validateAccesDevoirWithHeadTeacher(idDevoir,user,handler,resourceRequest);
                }
                break;
                default: {
                    handler.handle(false);
                }
            }
        }
    }
}