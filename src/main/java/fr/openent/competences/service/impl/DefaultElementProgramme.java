package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.ElementProgramme;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.sql.SqlResult.validResultHandler;

public class DefaultElementProgramme implements ElementProgramme {

    @Override
    public void setElementProgramme(String userId, Long idPeriode, String idMatiere, String idClasse,String texte, Handler<Either<String, JsonArray>> handler){
        JsonArray values = new JsonArray();

        StringBuilder query = new StringBuilder()
                .append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".element_programme ")
                .append(" (id_periode, id_matiere , id_classe, id_user_create, id_user_update, texte) VALUES ")
                .append(" (?, ?, ?, ?, ?, ?) ")
                .append(" ON CONFLICT (id_periode, id_matiere , id_classe) ")
                .append(" DO UPDATE SET id_user_update = ? , texte = ? ");

        values.addNumber(idPeriode).addString(idMatiere).addString(idClasse).addString(userId).addString(userId).addString(texte);
        values.addString(userId).addString(texte);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }


    @Override
    public void getElementProgramme(Long idPeriode, String idMatiere, String idClasse, Handler<Either<String, JsonObject>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new JsonArray();

        query.append("SELECT * ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".element_programme ")
                .append("WHERE "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_classe = ? ")
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_periode = ? ")
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_matiere = ? ");

        values.addString(idClasse);
        values.addNumber(idPeriode);
        values.addString(idMatiere);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

}
