package fr.openent.competences.security;

import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by vogelmt on 31/03/2017.
 */
public class AccessBFCFilter implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(AccessBFCFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user,
                          final Handler<Boolean> handler) {
        if(new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString())) {
            handler.handle(true);
        }
        else {
            handler.handle(false);
        }
    }
}
