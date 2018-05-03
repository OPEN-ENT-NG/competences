package fr.openent.competences.security;

import fr.openent.competences.Competences;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class CanUpdateBFCSyntheseRight implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(CanUpdateBFCSyntheseRight.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user, final Handler<Boolean> handler) {
        handler.handle(WorkflowActionUtils.hasRight(user, Competences.CAN_UPDATE_BFC_SYNTHESE_RIGHT));
    }
}
