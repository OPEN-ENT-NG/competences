package fr.openent.competences.utils;

import io.vertx.core.json.JsonArray;

/**
 * Created by lugana on 26/04/2018.
 */
public class UtilsConvert {

    public static String[] jsonArrayToStringArr (JsonArray jsonArray) {
        if(jsonArray == null) {
            jsonArray = new fr.wseduc.webutils.collections.JsonArray();
        }
        String[] stringArr = new String[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            stringArr[i] = jsonArray.getString(i);
        }
        return stringArr;
    }


    public static JsonArray strIdGroupesToJsonArray (Object idGroupes) {
        JsonArray result = new JsonArray();
        if (idGroupes instanceof JsonArray) {
            result = (JsonArray) idGroupes;
        }
        else if (idGroupes != null) {
            String [] idGps = ((String)idGroupes).split(",");
            for(int i=0;i< idGps.length; i++) {
                result.add(idGps[i]);
            }
        }
        return result;
    }
}
