package fr.openent.competences.security;

import fr.openent.competences.Competences;
import fr.openent.competences.security.utils.FilterDevoirUtils;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by anabah on 02/03/2017.
 */
public class AccessAnnotationFilter implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(AccessAnnotationFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding,
                          UserInfos user, final Handler<Boolean> handler) {

        if (new WorkflowActionUtils().hasRight(user,
                WorkflowActions.ADMIN_RIGHT.toString())) {
            handler.handle(true);
        } else {
            switch (user.getType()) {
                case "Teacher": {
                    resourceRequest.pause();

                    Long idDevoir;
                    try {
                        idDevoir = Long.valueOf(resourceRequest.params().get("idDevoir"));
                    } catch (NumberFormatException e) {
                        log.error("Error : idDevoir must be a long object", e);
                        handler.handle(false);
                        return;
                    }
                    // On check si l'utilisateurest professeur principal de la classe
                    new FilterDevoirUtils().validateAccesDevoirWithHeadTeacher(idDevoir, user,handler,resourceRequest);

                }
                break;
                default: {
                    handler.handle(false);
                }
            }
        }
    }
}

