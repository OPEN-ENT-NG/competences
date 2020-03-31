package fr.openent.competences.helper;

import fr.openent.competences.enums.Common;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserInfos;

public class ManageError {
    protected static final Logger log = LoggerFactory.getLogger(ManageError.class);

    static public  Boolean haveUser (HttpServerRequest request, UserInfos user){
        if (user == null) {
            requestFail(request, Common.INFO.getString(), "User not found.");
            return false;
        }
        String userId =  user.getUserId();
        if(userId.isEmpty()){
            requestFail(request, Common.INFO.getString(), "Id user is empty.");
            return false;
        }
        return true;
    }

    static public  void requestFailError(HttpServerRequest request, String type, String message, String errorMessage){
        log.error(message);
        if(type.equals(Common.ERROR.getString())) log.error(errorMessage);
        requestFail(request, type, message);
    }

    static public void requestFail(HttpServerRequest request, String type, String message){
        JsonObject messageSend = new JsonObject()
                .put("Message", message)
                .put("Type: ", type);
        Renders.badRequest(request, messageSend.toString());
    }
}
