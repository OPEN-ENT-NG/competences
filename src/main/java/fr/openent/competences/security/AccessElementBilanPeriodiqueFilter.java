package fr.openent.competences.security;

import fr.openent.competences.security.utils.FilterBilanPeriodique;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AccessElementBilanPeriodiqueFilter implements ResourcesProvider {
    protected static final Logger log = LoggerFactory.getLogger(AccessElementProgrammeFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, final UserInfos user,
                          final Handler<Boolean> handler) {

        if(new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString())) {
            resourceRequest.resume();
            handler.handle(true);
        } else {
            switch (user.getType()) {
                case "Teacher": {
                    new FilterBilanPeriodique().validateAccessBilanPeriodique(user, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean isValid) {
                            resourceRequest.resume();
                            handler.handle(isValid);
                        }
                    });
                }
                break;
                default: {
                    resourceRequest.resume();
                    handler.handle(false);
                }
            }
        }
    }
}
