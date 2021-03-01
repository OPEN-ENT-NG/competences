package fr.openent.competences.security;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import static fr.openent.competences.Competences.ID_ELEVE_KEY;

public class AccessStudentParentFilter implements ResourcesProvider {
    @Override
    public void authorize(final HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        handler.handle((user.getUserId().equals(request.params().get(ID_ELEVE_KEY))
                || user.getChildrenIds().contains(request.params().get(ID_ELEVE_KEY)))
        );
    }
}
