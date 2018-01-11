package fr.openent.competences.security;

import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.utils.CompetencesWorkflowActions;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * Created by ledunoiss on 01/06/2017.
 */
public class CreateEvaluationWorkflow  implements ResourcesProvider {
	@Override
	public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
		handler.handle(new WorkflowActionUtils().hasRight(user, CompetencesWorkflowActions.CREATE_EVALUATION.toString()));
	}
}
