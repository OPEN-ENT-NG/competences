package fr.openent.competences.utils;

import fr.openent.competences.Competences;
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

    public static void addValueForRequest(JsonArray values, UserInfos user, Boolean isChefEtab) {
        if (!isChefEtab) {
            // Ajout des params pour les devoirs dont on est le propriétaire
            values.add(user.getUserId());

            // Ajout des params pour la récupération des devoirs de mes tiulaires
            values.add(user.getUserId());
            for (int i = 0; i < user.getStructures().size(); i++) {
                values.add(user.getStructures().get(i));
            }

            // Ajout des params pour les devoirs que l'on m'a partagés (lorsqu'un remplaçant a créé un devoir
            // pour un titulaire par exemple)
            values.add(user.getUserId());
        }

    }

    public static void getNbNotesDevoirs(UserInfos user, Long idDevoir, Handler<Either<String, JsonArray>> handler,
                                         Boolean isChefEtab) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT count(notes.id) as nb_notes , devoirs.id, rel_devoirs_groupes.id_groupe")
                .append(" FROM ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.NOTES_TABLE)
                .append(", ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.DEVOIR_TABLE)
                .append(", ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.REL_DEVOIRS_GROUPES)
                .append(" WHERE notes.id_devoir = devoirs.id")
                .append(" AND rel_devoirs_groupes.id_devoir = devoirs.id")
                .append(" AND devoirs.id = ?");

        values.add(idDevoir);

        if (!isChefEtab) {
            query.append(" AND (devoirs.owner = ? OR") // devoirs dont on est le propriétaire
                    .append(" devoirs.owner IN (SELECT DISTINCT id_titulaire") // ou dont l'un de mes tiulaires le sont (on regarde sur tous mes établissments)
                    .append(" FROM ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.REL_PROFESSEURS_REMPLACANTS_TABLE)
                    .append(" INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.DEVOIR_TABLE)
                    .append(" ON devoirs.id_etablissement = rel_professeurs_remplacants.id_etablissement")
                    .append(" WHERE id_remplacant = ?")
                    .append(" AND rel_professeurs_remplacants.id_etablissement IN ").append(Sql.listPrepared(user.getStructures().toArray()))
                    .append(" ) OR")
                    .append(" ? IN (SELECT member_id") // ou devoirs que l'on m'a partagés (lorsqu'un remplaçant a créé un devoir pour un titulaire par exemple)
                    .append(" FROM ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.DEVOIR_SHARE_TABLE)
                    .append(" WHERE resource_id = devoirs.id")
                    .append(" AND action = '").append(Competences.DEVOIR_ACTION_UPDATE).append("')")
                    .append(" )");
        }
        query.append(" GROUP by devoirs.id, rel_devoirs_groupes.id_groupe");

        HomeworkUtils.addValueForRequest(values, user, isChefEtab);

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
        o.remove("id");
        // le pourcentage d'avancement n'est pas conservé lors de la duplication d'un devoir
        o.put("percent", 0);
        try {
            o.put("coefficient", safeGetDouble(devoir, "coefficient"));
        } catch (ClassCastException e) {
            log.error("An error occured when casting devoir object to duplication format.");
            log.error(e);
        }
        if (o.getString("libelle") == null) {
            o.remove("libelle");
        }
        if (o.getLong("id_sousmatiere") == null) {
            o.remove("id_sousmatiere");
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
