package fr.openent.competences.helpers;

import fr.openent.competences.constants.Field;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NoteControllerHelper {

    protected static final Logger log = LoggerFactory.getLogger(DevoirControllerHelper.class);

    public static JsonObject setResponseExportReleve(JsonObject responseAnnual, JsonObject responsePeriodic) {

        JsonArray annualStudents = responseAnnual.getJsonArray(Field.ELEVES);
        JsonArray periodicStudents = responsePeriodic.getJsonArray(Field.ELEVES);

        for(int i = 0; i < periodicStudents.size(); i++){
            JsonObject studentPeriodicJO = periodicStudents.getJsonObject(i);
            for (int j = 0; j < annualStudents.size(); j++) {
                JsonObject studentAnnualJO = annualStudents.getJsonObject(j);
                if (studentPeriodicJO.getString(Field.ID).equals(studentAnnualJO.getString(Field.ID))) {
                    studentPeriodicJO.put(Field.AVERAGES, studentAnnualJO.getJsonArray(Field.MOYENNES, new JsonArray()));
                    studentPeriodicJO.put(Field.FINALAVERAGES, studentAnnualJO.getJsonArray(Field.MOYENNESFINALES, new JsonArray()));
                }
            }
        }
        return responsePeriodic;
    }


}
