package fr.openent.competences.security;

import fr.openent.competences.AccessEventBus;
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
        RequestUtils.bodyToJson(resourceRequest, params -> {
            if(!params.containsKey("id_eleve") && !params.containsKey("id_structure")){
                resourceRequest.resume();
                handler.handle(false);
            } else {
                JsonArray idEleves = new JsonArray().add(params.getString("id_eleve"));
                String idStructure = params.getString("id_structure");
                EventBus eventBus = AccessEventBus.getInstance().getEventBus();
                WorkflowActionUtils.hasHeadTeacherRight(user, null,null,null, idEleves,
                        eventBus, idStructure, event -> {
                            Boolean isHeadTeacher = event.isRight() ? event.right().getValue() : false;
                            handler.handle(isHeadTeacher ||
                                    WorkflowActionUtils.hasRight(user, WorkflowActions.SET_AVIS_CONSEIL.toString()));
                        });
            }
        });
    }
}
