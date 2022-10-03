package fr.openent.competences.security;

import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class UserIdStructure implements ResourcesProvider {


    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        // Verify if idStructure front equals idStructure user
        RequestUtils.bodyToJson(request, body -> {
            String idStructure = body.getString(Field.ID_STRUCTURE);
            handler.handle(new FilterUserUtils(user,null).validateStructure(idStructure));
        });
    }
}
