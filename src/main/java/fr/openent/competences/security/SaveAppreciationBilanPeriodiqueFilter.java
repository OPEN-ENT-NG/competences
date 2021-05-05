package fr.openent.competences.security;

import fr.openent.competences.AccessEventBus;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class SaveAppreciationBilanPeriodiqueFilter implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
        RequestUtils.bodyToJson(resourceRequest, resource -> {
            if(!resource.containsKey("idClasse") && !resource.containsKey("idEtablissement")
                    && !resource.containsKey("idMatiere") && !resource.containsKey("idPeriode")){
                resourceRequest.resume();
                handler.handle(false);
            } else {
                JsonArray idClasses = new JsonArray().add(resource.getString("idClasse"));
                String idEtablissement = resource.getString("idEtablissement");
                String idMatiere = resource.getString("idMatiere");
                Long idPeriode = resource.getLong("idPeriode");
                WorkflowActionUtils.hasHeadTeacherRight(user, idClasses,null,null, null,null,
                        null, event -> {
                            boolean isHeadTeacher = event.isRight() ? event.right().getValue() : false;

                            if(isHeadTeacher ||
                                    WorkflowActionUtils.hasRight(user, WorkflowActions.SAVE_APPRECIATION_BILAN_PERIODIQUE.toString())){
                                handler.handle(true);
                            } else {
                                DefaultUtilsService utilsService = new DefaultUtilsService(AccessEventBus.getInstance().getEventBus());
                                utilsService.hasService(idEtablissement, idClasses, idMatiere, idPeriode, user, isValid -> {
                                    resourceRequest.resume();
                                    handler.handle(isValid);
                                });
                            }
                        });
            }
        });
    }
}



