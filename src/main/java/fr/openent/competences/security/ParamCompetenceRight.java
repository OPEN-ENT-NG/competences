package fr.openent.competences.security;

import fr.openent.competences.Competences;
import fr.openent.competences.security.utils.WorkflowActionUtils;
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
public class ParamCompetenceRight implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(ParamCompetenceRight.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user, final Handler<Boolean> handler) {
        handler.handle(WorkflowActionUtils.hasRight(user, Competences.PARAM_COMPETENCE_RIGHT));
    }
}
