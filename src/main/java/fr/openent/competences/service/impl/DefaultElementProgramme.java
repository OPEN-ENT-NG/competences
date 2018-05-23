package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.ElementProgramme;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.sql.SqlResult.validResultHandler;

public class DefaultElementProgramme implements ElementProgramme {

    @Override
    public void setElementProgramme(String userId, Long idPeriode, String idMatiere, String idClasse,String texte, Handler<Either<String, JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder()
                .append("INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".element_programme ")
                .append(" (id_periode, id_matiere , id_classe, id_user_create, id_user_update, texte) VALUES ")
                .append(" (?, ?, ?, ?, ?, ?) ")
                .append(" ON CONFLICT (id_periode, id_matiere , id_classe) ")
                .append(" DO UPDATE SET id_user_update = ? , texte = ? ");

        values.add(idPeriode).add(idMatiere).add(idClasse).add(userId).add(userId).add(texte);
        values.add(userId).add(texte);

        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }


    @Override
    public void getElementProgramme(Long idPeriode, String idMatiere, String idClasse, Handler<Either<String, JsonObject>> handler){
        StringBuilder query = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT * ")
                .append("FROM "+ Competences.COMPETENCES_SCHEMA +".element_programme ")
                .append("WHERE "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_classe = ? ")
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_periode = ? ")
                .append("AND "+ Competences.COMPETENCES_SCHEMA +".element_programme.id_matiere = ? ");

        values.add(idClasse);
        values.add(idPeriode);
        values.add(idMatiere);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }


    @Override
    public void getDomainesEnseignement(Handler<Either<String, JsonArray>> handler){
        String query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA +".domaine_enseignement ORDER BY libelle";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void getSousDomainesEnseignement(Handler<Either<String, JsonArray>> handler){
        String query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA +".sous_domaine_enseignement ORDER BY libelle";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

    @Override
    public void getPropositions(Handler<Either<String, JsonArray>> handler){
        String query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA +".proposition ORDER BY libelle";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        Sql.getInstance().prepared(query.toString(), values, validResultHandler(handler));
    }

}
