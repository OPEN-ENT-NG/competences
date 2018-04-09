package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by anabah on 01/03/2017.
 */
public class DefaultAppreciationService extends SqlCrudService implements fr.openent.competences.service.AppreciationService {
    public DefaultAppreciationService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void createAppreciation(JsonObject appreciation, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.create(appreciation, user, handler);
    }

    @Override
    public void updateAppreciation(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.update(data.getValue("id").toString(), data, user, handler);
    }

    @Override
    public void deleteAppreciation(Long idNote, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.delete(idNote.toString(), user, handler);
    }

    @Override
    public void createOrUpdateAppreciationClasse(String appreciation, String id_classe, Integer id_periode, String id_matiere, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder().append("INSERT INTO ")
                .append( Competences.COMPETENCES_SCHEMA + ".appreciation_classe (appreciation, id_classe, id_periode, id_matiere) ")
                .append(" VALUES " )
                .append(" ( ?, ?, ?, ?)" )
                .append(" ON CONFLICT (id_classe, id_periode, id_matiere) DO UPDATE SET appreciation = ?");
        JsonArray values = new JsonArray();
        values.addString(appreciation);
        values.addString(id_classe);
        values.addNumber(id_periode);
        values.addString(id_matiere);
        values.addString(appreciation);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getAppreciationClasse(String id_classe, int id_periode, String id_matiere, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonArray values = new JsonArray();

        query.append("SELECT * ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".appreciation_classe ")
                .append("WHERE "+ Competences.COMPETENCES_SCHEMA +".appreciation_classe.id_classe = ? ")
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".appreciation_classe.id_periode = ? ")
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".appreciation_classe.id_matiere = ? ");

        values.addString(id_classe);
        values.addNumber(id_periode);
        values.addString(id_matiere);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

}
