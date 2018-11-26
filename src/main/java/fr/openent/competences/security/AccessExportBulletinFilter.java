package fr.openent.competences.security;

import fr.openent.competences.Competences;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AccessExportBulletinFilter  implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(ParamCompetenceRight.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user,
                          final Handler<Boolean> handler) {
        handler.handle(WorkflowActionUtils.hasRight(user, Competences.CAN_ACCESS_EXPORT_BULLETIN));
    }
}
