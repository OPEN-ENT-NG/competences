package fr.openent.competences.utils;

import org.vertx.java.core.json.JsonArray;

/**
 * Created by lugana on 26/04/2018.
 */
public class UtilsConvert {

    public static String[] jsonArrayToStringArr (JsonArray jsonArray) {
        if(jsonArray == null) {
            jsonArray = new JsonArray();
        }
        String[] stringArr = new String[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            stringArr[i] = jsonArray.get(i);
        }
        return stringArr;
    }
}
