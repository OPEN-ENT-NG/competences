package fr.openent.competences.security.utils;

import fr.openent.competences.Competences;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Created by anabah on 02/03/2017.
 */
public class FilterAppreciationUtils {
        public void validateAppreciationOwner (Long idAppreciation, String owner, final Handler<Boolean> handler) {
            StringBuilder query = new StringBuilder()
                    .append("SELECT count(devoirs.*) " +
                            "FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs " +
                            "INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".appreciations ON (appreciations.id_devoir = devoirs.id) " +
                            "WHERE appreciations.id = ? " +
                            "AND devoirs.owner = ?;");

            JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(idAppreciation).add(owner);

            Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> sqlResult) {
                    Long count = SqlResult.countResult(sqlResult);
                    handler.handle(count != null && count > 0);
                }
            });
        }



        public void validateAccessAppreciation (Long idNote, UserInfos user, final Handler<Boolean> handler) {
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

            StringBuilder query = new StringBuilder()
                    .append("SELECT count(*) FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs ")
                    .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".appreciations ON " +
                            "(appreciations.id_devoir = devoirs.id) ")
                    .append("WHERE appreciations.id = ? ")
                    .append("AND (devoirs.owner = ? OR ")
                    .append("devoirs.owner IN (SELECT DISTINCT id_titulaire ")
                    .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_professeurs_remplacants ")
                    .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".devoirs ON devoirs.id_etablissement = rel_professeurs_remplacants.id_etablissement  ")
                    .append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".appreciations ON (appreciations.id_devoir = devoirs.id) ")
                    .append("WHERE appreciations.id = ? ")
                    .append("AND id_remplacant = ? ")
                    .append(") OR ")

                    .append("? IN (SELECT member_id ")
                    .append("FROM " + Competences.COMPETENCES_SCHEMA + ".devoirs_shares ")
                    .append("WHERE resource_id = devoirs.id ")
                    .append("AND action = '" + Competences.DEVOIR_ACTION_UPDATE+"')")

                    .append(")");

            // Ajout des params pour la partie de la requête où on vérifie si on est le propriétaire
            params.add(idNote);
            params.add(user.getUserId());

            // Ajout des params pour la partie de la requête où on vérifie si on a des titulaires propriétaire
            params.add(idNote);
            params.add(user.getUserId());

            // Ajout des params pour la partie de la requête où on vérifie si on a des droits de partage provenant d'un remplaçant
            params.add(user.getUserId());


            Sql.getInstance().prepared(query.toString(), params, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> sqlResult) {
                    Long count = SqlResult.countResult(sqlResult);
                    handler.handle(count != null && count > 0);
                }
            });
        }

}
