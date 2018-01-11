package fr.openent.competences.security;

import fr.openent.competences.security.utils.FilterDevoirUtils;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class AccessVisibilityAppreciation implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(AccessAppreciationFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user, final Handler<Boolean> handler) {
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

                new FilterDevoirUtils().validateAccessDevoir(idDevoir, user, false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean isValid) {
                        resourceRequest.resume();
                        handler.handle(isValid);
                    }
                });
            }
            break;
            case "Personnel": {
                resourceRequest.pause();
                if (user.getFunctions().containsKey("DIR")) {
                    resourceRequest.resume();
                    handler.handle(true);
                } else {
                    handler.handle(false);
                }
            }
            break;
            default: {
                handler.handle(false);
            }
        }
    }
}