package fr.openent.competences.security;

import fr.openent.competences.constants.Field;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import static fr.openent.competences.Competences.ID_ELEVE_KEY;

public class AccessStructureIsParent implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String idEtablissement = request.getParam(Field.IDETABLISSEMENT);
        final boolean isInStructure = user.getStructures().contains(idEtablissement);
        handler.handle(isInStructure && user.getChildrenIds().contains(request.params().get(ID_ELEVE_KEY)));
    }
}
