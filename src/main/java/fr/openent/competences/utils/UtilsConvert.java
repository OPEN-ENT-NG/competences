/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.openent.competences.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;


/**
 * Created by lugana on 26/04/2018.
 */
public class UtilsConvert {

    protected static final Logger log = LoggerFactory.getLogger(UtilsConvert.class);

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

    public static Date convertStringToDate(String dateString,String format)  {
        Date date = null;
        if(dateString != null && format != null){
            try{
                SimpleDateFormat formatter = new SimpleDateFormat(format);
                date = formatter.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
                log.info(e.getMessage());
            }
        }
        return date;
    }
}
