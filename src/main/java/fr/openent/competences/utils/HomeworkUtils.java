package fr.openent.competences.utils;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.service.impl.DefaultDevoirService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomeworkUtils {

    protected static final Logger log = LoggerFactory.getLogger(HomeworkUtils.class);

    public static void getNbNotesDevoirs(UserInfos user, Long idDevoir, Handler<Either<String, JsonArray>> handler,
                                         Boolean isChefEtab) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT count(" + Field.NOTES_TABLE + ".id) as nb_notes , " + Field.DEVOIR_TABLE + "." + Field.ID + ", " + Field.REL_DEVOIRS_GROUPES_TABLE + "." + Field.ID_GROUPE)
                .append(" FROM ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.NOTES_TABLE)
                .append(", ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.DEVOIR_TABLE)
                .append(", ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.REL_DEVOIRS_GROUPES)
                .append(" WHERE " + Field.NOTES_TABLE + "." + Field.ID_DEVOIR + " = " + Field.DEVOIR_TABLE + "." + Field.ID)
                .append(" AND " + Field.REL_DEVOIRS_GROUPES_TABLE + "." + Field.ID_DEVOIR + " = " + Field.DEVOIR_TABLE + "." + Field.ID)
                .append(" AND " + Field.DEVOIR_TABLE + "." + Field.ID + " = ?");

        values.add(idDevoir);

        if (!isChefEtab) {
            query.append(" AND (" + Field.DEVOIR_TABLE + "." + Field.OWNER + " = ? OR") // devoirs dont on est le propriétaire
                    .append(" ? IN (SELECT member_id") // ou devoirs que l'on m'a partagés (lorsqu'un remplaçant a créé un devoir pour un titulaire par exemple)
                    .append(" FROM ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.DEVOIR_SHARE_TABLE)
                    .append(" WHERE resource_id = " + Field.DEVOIR_TABLE + "." + Field.ID)
                    .append(" AND action = '").append(Competences.DEVOIR_ACTION_UPDATE).append("')")
                    .append(" )");
            values.add(user.getUserId()).add(user.getUserId());
        }
        query.append(" GROUP by " + Field.DEVOIR_TABLE + "." + Field.ID + ", " + Field.REL_DEVOIRS_GROUPES_TABLE + "." + Field.ID_GROUPE);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    public static StringBuilder formatDate(String date) {
        Pattern p = Pattern.compile("[0-9]*-[0-9]*-[0-9]*.*");
        Matcher m = p.matcher(date);
        if (!m.matches()) {
            StringBuilder dateFormated = new StringBuilder();
            String[] splitedDate = date.split("/");
            if (splitedDate.length < 3) {
                log.error("Date " + date + " cannot be formated");
                return new StringBuilder(date);
            }

            dateFormated.append(date.split("/")[2]).append('-');
            dateFormated.append(date.split("/")[1]).append('-');
            dateFormated.append(date.split("/")[0]);
            return dateFormated;
        } else {
            return new StringBuilder(date);
        }

    }

    public static JsonObject formatDevoirForDuplication (JsonObject devoir) {
        JsonObject o = new JsonObject(devoir.getMap());
        o.remove("created");
        o.remove("modified");
        o.remove(Field.ID);
        // le pourcentage d'avancement n'est pas conservé lors de la duplication d'un devoir
        o.put("percent", 0);
        try {
            o.put(Field.COEFFICIENT, safeGetDouble(devoir, Field.COEFFICIENT));
        } catch (ClassCastException e) {
            log.error("An error occured when casting devoir object to duplication format.");
            log.error(e);
        }
        if (o.getString("libelle") == null) {
            o.remove("libelle");
        }
        if (o.getLong(Field.ID_SOUSMATIERE) == null) {
            o.remove(Field.ID_SOUSMATIERE);
        }
        return o;
    }
    public static Double safeGetDouble(JsonObject jo, String key) {
        Double result;
        try {
            result = jo.getDouble(key);
        } catch (Exception e) {
            result = Double.parseDouble(jo.getString(key).replaceAll(",", "."));
        }
        return result;
    }

}
