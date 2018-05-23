package fr.openent.competences.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;


public class CompetencesController extends ControllerHelper {

    public CompetencesController() {}

	/**
	 * Displays the home view.
	 * @param request Client request
	 */
	@Get("")
	@SecuredAction("competences.access")
	public void view(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

            @Override
            public void handle(UserInfos user) {
                if(user.getType().equals("Teacher") || user.getType().equals("Personnel")) {
                    renderView(request, null, "eval_teacher.html", null);
                }else if(user.getType().equals("Student") || user.getType().equals("Relative")){
                    renderView(request, null,  "eval_parents.html", null);
                }
            }
        });
	}
}
