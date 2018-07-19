package fr.openent.competences.security.utils;

import fr.openent.competences.Competences;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import org.entcore.common.user.UserInfos;

import java.util.List;

/**
 * Created by ledunoiss on 01/06/2017.
 */
public class WorkflowActionUtils {
	public static boolean hasRight (UserInfos user, String action) {
		List<UserInfos.Action> actions = user.getAuthorizedActions();
		for (UserInfos.Action userAction : actions) {
			if (action.equals(userAction.getDisplayName())) {
				return true;
			}
		}
		return false;
	}

	public static void hasHeadTeacherRight (UserInfos user, JsonArray idsClasse,
                                            JsonArray idsRessource, String table,
											JsonArray idsEleve, final EventBus eb,
										   final Handler<Either<String, Boolean>> handler) {

		if (!hasRight(user, Competences.CAN_UPDATE_BFC_SYNTHESE_RIGHT)) {
			handler.handle(new Either.Right<>(false));
		}
		else {
			// Si les classes sont renseignées, on check directement si l'utilisateur est profprincipal sur les classes
			// passées en paramètre
			if (idsClasse != null) {
				FilterUserUtils.validateHeadTeacherWithClasses(user, idsClasse, handler);
			}
			else if (idsEleve != null) {
                FilterUserUtils.validateHeadTeacherWithEleves(user,idsEleve,eb,handler);
			}
            else if (idsRessource != null) {
                FilterUserUtils.validateHeadTeacherWithRessources(user, idsRessource, table, handler);
            }
		}

	}
}
