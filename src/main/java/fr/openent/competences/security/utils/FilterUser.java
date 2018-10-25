package fr.openent.competences.security.utils;

import fr.wseduc.webutils.Either;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.user.UserInfos;

public class FilterUser {

    public static void isChefEtabAndHeadTeacher(UserInfos user, final JsonArray idsClasse, Handler<Boolean> handler ){

        if( WorkflowActionUtils.hasRight(user, WorkflowActions.ADMIN_RIGHT.toString())){
            handler.handle( true);
            return;
        } else {
            FilterUserUtils.validateHeadTeacherWithClasses(user, idsClasse, new Handler<Either<String, Boolean>>() {
                @Override
                public void handle(Either<String, Boolean> response) {
                    if(response.isRight()){
                        handler.handle((response.right().getValue())? true : false);
                    }else{
                        handler.handle(false);
                    }
                }
            });
        }
    }
}
