package fr.openent.competences.security;

import fr.openent.competences.security.utils.FilterAppreciationUtils;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by anabah on 02/03/2017.
 */
public class AccessAppreciationFilter implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(AccessAppreciationFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user, final Handler<Boolean> handler) {
        switch (user.getType()) {
            case "Teacher": {
                resourceRequest.pause();

                Long idAppreciation;
                try {
                    idAppreciation = Long.parseLong(resourceRequest.params().get("idAppreciation"));
                } catch (NumberFormatException e) {
                    log.error("Error : idAppreciation must be a long object", e);
                    handler.handle(false);
                    return;
                }

                new FilterAppreciationUtils().validateAccessAppreciation(idAppreciation, user, new Handler<Boolean>() {
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
