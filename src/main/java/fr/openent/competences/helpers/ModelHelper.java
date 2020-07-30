package fr.openent.competences.helpers;

import io.vertx.core.json.JsonArray;
import fr.openent.competences.model.Model;
import java.util.List;

public class ModelHelper {
    public static JsonArray convetToJsonArray (List<? extends Model> modelInterfaceList) {
        JsonArray jsonArrayModel = new JsonArray();
        if (!modelInterfaceList.isEmpty()) {
            for (Model modelInstance : modelInterfaceList) {
                jsonArrayModel.add(modelInstance.toJsonObject());
            }
        }
        return jsonArrayModel;
    }
}

