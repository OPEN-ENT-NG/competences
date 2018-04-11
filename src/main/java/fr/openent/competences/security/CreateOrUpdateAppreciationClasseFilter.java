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
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

/**
 * Created by ledunoiss on 20/10/2016.
 */
public class CreateOrUpdateAppreciationClasseFilter implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(CreateOrUpdateAppreciationClasseFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, final UserInfos user, final Handler<Boolean> handler) {
       final FilterUserUtils userUtils = new FilterUserUtils(user, null);

            RequestUtils.bodyToJson(resourceRequest, "/competences" + Competences.SCHEMA_APPRECIATIONS_CLASSE,
                    new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject appreciation) {
                            Integer idPeriode = appreciation.getInteger("id_periode");
                            String idMatiere = appreciation.getString("id_matiere");
                            String idClasse = appreciation.getString("id_classe");
                            String appreciationStr = appreciation.getString("appreciation");

                            if(appreciationStr != null && appreciationStr.length() > 300) {
                                log.error("appreciation must be < 300 carac");
                                handler.handle(false);
                                return;
                            }


                            boolean isAdmin = new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());
                            if(isAdmin) {
                                handler.handle(true);
                                return;
                            }

                            // controles supplémentaires dans les cas des enseignants
                            if (user.getType().equals("Teacher")) {
                                //On check que la classe passé en paramètre soit bien ceux de l'utilisateur
                                if (!userUtils.validateClasse(idClasse)) {
                                    handler.handle(false);
                                    return;
                                }


                                // check de la date de fin de saisie et l'accès à la matière côté controleur

                                handler.handle(true);
                            } else {
                                log.error("unauthorized for this profile");
                                handler.handle(false);
                                return;
                            }

                        }
                    });
    }
}
