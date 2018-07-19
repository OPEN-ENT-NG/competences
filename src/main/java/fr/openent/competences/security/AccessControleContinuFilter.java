package fr.openent.competences.security;

import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

public class AccessControleContinuFilter implements ResourcesProvider{

    protected static final Logger log = LoggerFactory.getLogger(AccessControleContinuFilter.class);

    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
        boolean isAdmin = new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());

        if(isAdmin){
            resourceRequest.resume();
            handler.handle(true);
            return;
        }

        if(user.getType().equals("Teacher")){
            resourceRequest.pause();
            MultiMap params = resourceRequest.params();
            if(!(params.contains("idClasse") )){
                resourceRequest.resume();
                handler.handle(false);
                return;
            }

            else {
                WorkflowActionUtils.hasHeadTeacherRight(user, new JsonArray().add(params.get("idClasse")),
                        null, null, null, null, new Handler<Either<String, Boolean>>() {
                            @Override
                            public void handle(Either<String, Boolean> event) {
                                Boolean isHeadTeacher;
                                if(event.isLeft()) {
                                    isHeadTeacher = false;
                                }
                                else {
                                    isHeadTeacher = event.right().getValue();
                                }
                                if (!new FilterUserUtils(user, null).validateClasse(params.get("idClasse"))) {
                                    resourceRequest.resume();
                                    handler.handle(false || isHeadTeacher);
                                    return;
                                }
                                resourceRequest.resume();
                                handler.handle(true || isHeadTeacher);
                            }
                        });

            }
        }else{
            handler.handle(false);
        }

    }
}