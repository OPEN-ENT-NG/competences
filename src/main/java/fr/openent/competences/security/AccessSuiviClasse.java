package fr.openent.competences.security;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import static fr.openent.competences.Competences.ID_CLASSE_KEY;
import static fr.openent.competences.Competences.ID_STRUCTURE_KEY;

public class AccessSuiviClasse implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        final String idStructure = WorkflowActionUtils.getParamStructure(request);
        String classId = request.params().get(ID_CLASSE_KEY);
        FilterUserUtils filter = new FilterUserUtils(user, null);
        if (idStructure == null | classId == null){
            handler.handle(false);
        }else{
            handler.handle(filter.validateStructure(idStructure) && filter.validateClasse(classId) && WorkflowActionUtils.hasRight(user, WorkflowActions.ACCESS_SUIVI_CLASSE.toString()));
        }
    }
}
