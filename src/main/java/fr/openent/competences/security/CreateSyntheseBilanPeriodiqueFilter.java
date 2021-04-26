package fr.openent.competences.security;

import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class CreateSyntheseBilanPeriodiqueFilter implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
        MultiMap params = resourceRequest.params();
        if(!params.contains("id_classe")){
            resourceRequest.resume();
            handler.handle(false);
        } else {
            JsonArray idClasses = new JsonArray().add(params.get("id_classe"));
            WorkflowActionUtils.hasHeadTeacherRight(user, idClasses,null,null, null,null,
                    null, event -> {
                Boolean isHeadTeacher = event.isRight() ? event.right().getValue() : false;
                handler.handle(isHeadTeacher ||
                        WorkflowActionUtils.hasRight(user, WorkflowActions.CREATE_SYNTHESE_BILAN_PERIODIQUE.toString()));
            });
        }
    }
}
