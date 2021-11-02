package fr.openent.competences.security;

import fr.openent.competences.AccessEventBus;
import fr.openent.competences.Competences;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class SetAvisConseilFilter implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
        boolean hasRight = WorkflowActionUtils.hasRight(user, WorkflowActions.SET_AVIS_CONSEIL.toString());
        EventBus eventBus = AccessEventBus.getInstance().getEventBus();

        if ("POST".equals(resourceRequest.method().toString())) {
            RequestUtils.bodyToJson(resourceRequest, params -> {
                JsonArray idEleves = new JsonArray().add(params.getString("id_eleve"));
                String idStructure = params.getString("id_structure");
                WorkflowActionUtils.hasHeadTeacherRight(user, null,null,null, idEleves,
                        eventBus, idStructure, event -> {
                            boolean isHeadTeacher = event.isRight() ? event.right().getValue() : false;
                            handler.handle(isHeadTeacher || hasRight);
                        });
            });
        } else if ("DELETE".equals(resourceRequest.method().toString())) {
            JsonArray idEleves = new JsonArray().add(resourceRequest.params().get("id_eleve"));
            String idStructure = resourceRequest.params().get("id_structure");
            WorkflowActionUtils.hasHeadTeacherRight(user, null,null,null, idEleves,
                    eventBus, idStructure, event -> {
                        boolean isHeadTeacher = event.isRight() ? event.right().getValue() : false;
                        handler.handle(isHeadTeacher || hasRight);
                    });
        } else {
            resourceRequest.resume();
            handler.handle(false);
        }
    }
}
