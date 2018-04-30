package fr.openent.competences.security;

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

import java.util.List;

public class AccessControleContinuFilter implements ResourcesProvider{

    protected static final Logger log = LoggerFactory.getLogger(AccessControleContinuFilter.class);

    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
        boolean isAdmin = new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString());

        if(isAdmin){
            resourceRequest.resume();
            handler.handle(true);
            return;
        }

        if(user.getType().equals("Teacher")){
            resourceRequest.pause();
            MultiMap params = resourceRequest.params();
            if(!(params.contains("idClasse") && params.contains("idPeriode"))){
                resourceRequest.resume();
                handler.handle(false);
                return;
            }

            if(!new FilterUserUtils(user, null).validateClasse(params.get("idClasse"))) {
                resourceRequest.resume();
                handler.handle(false);
                return;
            }
            Long idPeriode;
            if(params.get("idPeriode")!= null){
                try{
                    idPeriode = Long.valueOf(params.get("idPeriode"));
                }catch(NumberFormatException e){
                    log.error("Error : idPeriode must be a Long ",e);
                    resourceRequest.resume();
                    handler.handle(false);
                    return;
                }
            }

            resourceRequest.resume();
            handler.handle(true);
        }else{
            handler.handle(false);
        }

    }
}
