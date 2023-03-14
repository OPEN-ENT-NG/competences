/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.openent.competences.security.utils;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.Arrays;
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

	public static void hasHeadTeacherRight(UserInfos user, JsonArray idsClasse, JsonArray idsRessource, String table,
										   JsonArray idsEleve, final EventBus eb, String idStructure,
										   final Handler<Either<String, Boolean>> handler) {
		// Si les classes sont renseignées, on check directement si l'utilisateur est profprincipal sur les classes
		// passées en paramètre
		if (idsClasse != null) {
			FilterUserUtils.validateHeadTeacherWithClasses(user, idsClasse, handler);
		}
		else if (idsEleve != null) {
			FilterUserUtils.validateHeadTeacherWithEleves(user, idsEleve, eb, idStructure, handler);
		}
		else if (idsRessource != null) {
			FilterUserUtils.validateHeadTeacherWithRessources(user, idsRessource, table, handler);
		}
	}

	public static String getParamStructure(HttpServerRequest request){
		List<String> structureIdFields = Arrays.asList(Field.ID_STRUCTURE, Field.IDETABLISSEMENT, Field.ID_ETABLISSEMENT, Field.IDSTRUCTURE, Field.STRUCTUREID);
		return structureIdFields.stream()
				.map(structureIdField -> request.params().get(structureIdField))
				.filter(structureIdField ->  structureIdField != null && !structureIdField.isEmpty())
				.findFirst()
				.orElse(null);
	}
}
