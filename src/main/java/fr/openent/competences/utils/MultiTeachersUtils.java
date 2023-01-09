package fr.openent.competences.utils;

import fr.openent.competences.constants.Field;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MultiTeachersUtils {
    protected static final Logger log = LoggerFactory.getLogger(MultiTeachersUtils.class);

    public static JsonArray filterSubtitute(List<Object> periodes, JsonArray multiTeachers) {
        for (Object periodeO : periodes) {
            JsonObject periode = (JsonObject) periodeO;
            multiTeachers = new JsonArray(multiTeachers.stream().filter(obj -> {
                JsonObject multi = (JsonObject) obj;
                if (!multi.getBoolean(Field.IS_COTEACHING)) {
                    String classOrGroupId =
                            (periode.containsKey(Field.ID_CLASSE))
                                    ? periode.getString(Field.ID_CLASSE) : periode.getString(Field.ID_GROUPE);
                    return filterSubtituteByDatePeriode(periode, multi, classOrGroupId);
                } else {
                    return true;
                }
            }).collect(Collectors.toList()));
        }
        return multiTeachers;
    }

    private static boolean filterSubtituteByDatePeriode(JsonObject periode, JsonObject multi, String classOrGroupId) {
        if (classOrGroupId.equals(multi.getString(Field.CLASS_OR_GROUP_ID))) {
            SimpleDateFormat formatter1 = new SimpleDateFormat(Field.dateFormateYYYYMMDDTHHMMSS);
            Date multiStartDate;
            Date multiEndDate;
            Date periodeStartDate;
            Date periodeEndDate;
            try {
                multiStartDate = formatter1.parse(multi.getString(Field.START_DATE));
                multiEndDate = formatter1.parse(multi.getString(Field.END_DATE));
                periodeStartDate = formatter1.parse(periode.getString(Field.TIMESTAMP_DT));
                periodeEndDate = formatter1.parse(periode.getString(Field.TIMESTAMP_FN));
            } catch (ParseException e) {
                log.error("[Competences@MultiTeachersUtils:filterSubtituteByDatePeriode] cannot parse dates");
                return true;
            }
            if (multiStartDate != null && multiEndDate != null && periodeEndDate != null && periodeStartDate != null)
                return (multiStartDate.after(periodeStartDate) && multiStartDate.before(periodeEndDate))
                        || (multiEndDate.after(periodeStartDate) && multiEndDate.before(periodeEndDate));
            else {
                return true;
            }
        } else {
            return true;
        }
    }
}
