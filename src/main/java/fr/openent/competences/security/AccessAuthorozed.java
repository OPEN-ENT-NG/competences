package fr.openent.competences.security;

import fr.openent.competences.security.utils.FilterUserUtils;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * Created by rahnir on 20/03/2017.
 */
public class AccessAuthorozed  implements ResourcesProvider {

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, final UserInfos user, final Handler<Boolean> handler) {
        switch (user.getType()) {
            case "Personnel" : {
                resourceRequest.pause();
                FilterUserUtils userUtils = new FilterUserUtils(user);
                    new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean isValid) {
                            resourceRequest.resume();
                            handler.handle(isValid);
                        }
                    };
            }
            break;
            default : {
                handler.handle(false);
            }
        }
    }
}
