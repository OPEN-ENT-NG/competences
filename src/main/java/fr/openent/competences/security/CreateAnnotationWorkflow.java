package fr.openent.competences.security;

import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

/**
 * Created by ledunoiss on 01/06/2017.
 */
public class CreateAnnotationWorkflow implements ResourcesProvider {
	@Override
	public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
		handler.handle(new WorkflowActionUtils().hasRight(user,WorkflowActions.CREATE_EVALUATION.toString()));
	}
}
