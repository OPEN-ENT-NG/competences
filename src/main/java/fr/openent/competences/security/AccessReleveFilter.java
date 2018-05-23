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

import fr.openent.competences.security.utils.FilterPeriodeUtils;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by ledunoiss on 20/10/2016.
 */
public class AccessReleveFilter implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(AccessReleveFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, final UserInfos user,
                          final Handler<Boolean> handler) {

        if ("GET".equals(resourceRequest.method().toString())) {
            String idClasse = resourceRequest.params().get("idClasse");
            String idEtablissement = resourceRequest.params().get("idEtablissement");
            String idMatiere = resourceRequest.params().get("idMatiere");
            String idPeriodeString = resourceRequest.params().get("idPeriode");
            Long idPeriode = null;
            if (idPeriodeString != null) {
                try {
                    idPeriode = Long.parseLong(idPeriodeString);
                } catch (NumberFormatException e) {
                    log.error(" Error :idPeriode must be a long object ", e);
                    resourceRequest.resume();
                    handler.handle(false);
                    return;
                }
            }
            authorizeAccess(resourceRequest,idEtablissement,idClasse,idMatiere,idPeriode,user,handler);
        }
        else {
            RequestUtils.bodyToJson(resourceRequest, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject resource) {
                   String idClasse = resource.getString("idClasse");
                    String idEtablissement = resource.getString("idEtablissement");
                    String idMatiere = resource.getString("idMatiere");
                    Long idPeriode = resource.getLong("idPeriode");
                    authorizeAccess(resourceRequest,idEtablissement,idClasse,idMatiere, idPeriode,user,handler);
                }
            });
        }
    }

    private  void authorizeAccess(final HttpServerRequest resourceRequest,
                                  String idEtablissement, String idClasse, String idMatiere, Long idPeriode,
                                  UserInfos user,final Handler<Boolean> handler) {
        FilterUserUtils userUtils = new FilterUserUtils(user,null);

        boolean isAdmin = new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());

        if(isAdmin) {
            resourceRequest.resume();
            handler.handle(true);
            return;
        }

        else if (user.getType().equals("Teacher")) {
            resourceRequest.pause();

            //On check si tous les paramètres sont bien présents
            if (null == idClasse || null == idMatiere || null == idEtablissement) {
                resourceRequest.resume();
                handler.handle(false);
            }
            //On check que la classe et l'établissement passé en paramètre soit bien ceux de l'utilisateur
           else if (!userUtils.validateClasse(idClasse) &&
                    !userUtils.validateStructure(idEtablissement)) {
                resourceRequest.resume();
                handler.handle(false);
            }

            else {
                new FilterPeriodeUtils().validateStructure(idEtablissement,
                        idPeriode, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean isValid) {
                                resourceRequest.resume();
                                handler.handle(isValid);
                            }
                        });
            }
        }
        else {
            resourceRequest.resume();
            handler.handle(false);
        }
    }

}
