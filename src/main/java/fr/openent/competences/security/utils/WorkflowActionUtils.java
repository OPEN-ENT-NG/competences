package fr.openent.competences.security.utils;

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
}
