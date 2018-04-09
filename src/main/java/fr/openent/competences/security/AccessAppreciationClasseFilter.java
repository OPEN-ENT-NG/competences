/*
 * Copyright (c) Région Hauts-de-France, Département 77, CGI, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package fr.openent.competences.security;

import fr.openent.competences.Competences;
import fr.openent.competences.security.utils.FilterPeriodeUtils;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

/**
 * Created by ledunoiss on 20/10/2016.
 */
public class AccessAppreciationClasseFilter implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(AccessAppreciationClasseFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user, final Handler<Boolean> handler) {
        FilterUserUtils userUtils = new FilterUserUtils(user);

        boolean isAdmin = new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());
        if(isAdmin) {
            handler.handle(true);
        }

        if (user.getType().equals("Teacher")) {
            resourceRequest.pause();
            MultiMap params = resourceRequest.params();

            //On check si tous les paramètres sont bien présents
            if (!(resourceRequest.params().contains("id_classe") &&
                    resourceRequest.params().contains("id_periode") &&
                    resourceRequest.params().contains("id_matiere") &&
                    resourceRequest.params().contains("appreciation")) ) {
                handler.handle(false);
            }

            //On check que la classe passé en paramètre soit bien ceux de l'utilisateur
            if (!userUtils.validateClasse(params.get("idClasse"))) {
                handler.handle(false);
            }


            Long idPeriode;
            if (params.get("idPeriode") != null) {
                try {
                    idPeriode = Long.parseLong(params.get("idPeriode"));
                } catch (NumberFormatException e) {
                    log.error("Error : idPeriode must be a long object", e);
                    handler.handle(false);
                    return;
                }
            }

            //TODO checker la matiere
            handler.handle(true);
        }
        else {
            handler.handle(false);
        }
    }
}
