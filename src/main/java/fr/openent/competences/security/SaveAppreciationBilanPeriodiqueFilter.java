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

public class SaveAppreciationBilanPeriodiqueFilter implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
        MultiMap params = resourceRequest.params();
        if(!params.contains("idClasse")){
            resourceRequest.resume();
            handler.handle(false);
        } else {
            JsonArray idClasses = new JsonArray().add(params.get("idClasse"));
            WorkflowActionUtils.hasHeadTeacherRight(user, idClasses,null,null, null,null,
                    null, event -> {
                        Boolean isHeadTeacher = event.isRight() ? event.right().getValue() : false;
                        handler.handle(isHeadTeacher ||
                                WorkflowActionUtils.hasRight(user, WorkflowActions.SAVE_APPRECIATION_BILAN_PERIODIQUE.toString()));
                    });
        }
    }
}
