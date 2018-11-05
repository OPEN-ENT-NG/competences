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

package fr.openent.competences.security;

import fr.openent.competences.security.utils.FilterDevoirUtils;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.http.BaseServer;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;


/**
 * Created by ledunoiss on 19/10/2016.
 */
public class AccessEvaluationFilter implements ResourcesProvider   {

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding,
                          final UserInfos user, final Handler<Boolean> handler) {


        switch (user.getType()) {
            case "Teacher" : {
                resourceRequest.pause();

                if (!resourceRequest.params().contains("idDevoir")) {
                    handler.handle(false);
                }
                try {
                    final Long idDevoir = Long.parseLong(resourceRequest.params().get("idDevoir"));

                    new FilterDevoirUtils().validateAccessDevoir(idDevoir, user, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean isValid) {
                            resourceRequest.resume();
                            handler.handle(isValid);
                        }
                    });
                } catch (NumberFormatException e) {
                    resourceRequest.resume();
                    Renders.badRequest(resourceRequest, "Error : idNote must be a long object");
                }
            }
            break;
            case "Personnel" : {
                handler.handle(true);
            }
            break;
            default : {
                handler.handle(false);
            }
        }
    }
}
