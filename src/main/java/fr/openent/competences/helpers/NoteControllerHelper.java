package fr.openent.competences.helpers;

import fr.openent.competences.constants.Field;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class NoteControllerHelper {

    protected static final Logger log = LoggerFactory.getLogger(NoteControllerHelper.class);

    private NoteControllerHelper() {
        throw new IllegalStateException("NoteControllerHelper class");
    }

    @SuppressWarnings("unchecked")
    public static void setResponseExportReleve(JsonObject responseAnnual, JsonObject responsePeriodic) {

        List<JsonObject> annualStudents = responseAnnual.getJsonArray(Field.ELEVES).getList();
        List<JsonObject> periodicStudents = responsePeriodic.getJsonArray(Field.ELEVES).getList();
        responsePeriodic .put(Field.ELEVES, periodicStudents.stream()
                .map( periocStudent -> {
                            JsonObject annualStudentFind = annualStudents
                                    .stream()
                                    .filter( annualStudent ->
                                            periocStudent.getString(Field.ID).equals(annualStudent.getString(Field.ID))).
                                    findFirst().orElse(new JsonObject());

                            periocStudent.put(Field.AVERAGES,
                                    annualStudentFind.getJsonArray(Field.MOYENNES, new JsonArray()));
                            periocStudent.put(Field.FINALAVERAGES, annualStudentFind.getJsonArray(Field.MOYENNESFINALES, new JsonArray()));
                            return periocStudent;
                        }

                ).collect(Collectors.toList())
        );
    }

}
