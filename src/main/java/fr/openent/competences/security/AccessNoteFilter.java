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

import fr.openent.competences.security.utils.FilterNoteUtils;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

/**
 * Created by ledunoiss on 20/10/2016.
 */
public class AccessNoteFilter implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(AccessNoteFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user, final Handler<Boolean> handler) {
        switch (user.getType()) {
            case "Teacher" : {
                resourceRequest.pause();

                Long idNote;
                try {
                    idNote = Long.parseLong(resourceRequest.params().get("idNote"));
                } catch(NumberFormatException e) {
                    log.error("Error : idNote must be a long object", e);
                    handler.handle(false);
                    return;
                }

                new FilterNoteUtils().validateAccessNote(idNote, user, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean isValid) {
                                resourceRequest.resume();
                                handler.handle(isValid);
                            }
                        });
            }
            break;
            case "Personnel" : {
                resourceRequest.pause();
                if(user.getFunctions().containsKey("DIR")){
                    resourceRequest.resume();
                    handler.handle(true);
                }else{
                    handler.handle(false);
                }
            }
            break;
            default: {
                handler.handle(false);
            }
        }
    }

}
