package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.AvisConseilService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultAvisConseilService implements AvisConseilService {


    public void getLibelleAvis (Long typeAvis, String idStructure, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id, libelle, type_avis, id_etablissement, active ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".avis_conseil_bilan_periodique ");
        if (typeAvis != null) {
            query.append("WHERE type_avis = ? ");
            params.add(typeAvis);
        }
        if (idStructure != null) {
            if(typeAvis != null)
                query.append("AND ");
            else
                query.append("WHERE ");
            query.append("id_etablissement = ? OR id_etablissement IS NULL");
            params.add(idStructure);
        }
        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    public void createOpinion (Long typeAvis, String libelle, String idStructure, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".avis_conseil_bilan_periodique " +
                "(type_avis, libelle, id_etablissement, active) VALUES (?, ?, ?, true) " +
                "RETURNING id";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(typeAvis);
        values.add(libelle);
        values.add(idStructure);
        Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }

    public void updateOpinion (Long idAvis, boolean active, String libelle, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Competences.COMPETENCES_SCHEMA + ".avis_conseil_bilan_periodique " +
                "SET active = ?, libelle = ? " +
                "WHERE id = ?";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(active);
        values.add(libelle);
        values.add(idAvis);
        Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }

    public void deleteOpinion (Long idAvis, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".avis_conseil_bilan_periodique " +
                "WHERE id = ?";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(idAvis);
        Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }

    public void createOrUpdateAvisConseil (String idEleve, Long idPeriode,  Long id_avis_conseil_bilan, String idStructure,
                                           Handler<Either<String, JsonObject>> handler){
        String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".avis_conseil_de_classe " +
                "(id_eleve, id_periode, id_avis_conseil_bilan, id_etablissement) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (id_eleve, id_periode, id_etablissement) DO UPDATE SET id_avis_conseil_bilan = ? ";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(idEleve);
        values.add(idPeriode);
        values.add(id_avis_conseil_bilan);
        values.add(idStructure);
        values.add(id_avis_conseil_bilan);
        Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }

    public void deleteAvisConseil (Long idTypePeriode, String idEleve, String idStructure, Handler<Either<String, JsonObject>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "";

        query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".avis_conseil_de_classe " +
                " WHERE id_eleve = ?" +
                " AND id_periode = ? " +
                "AND id_etablissement = ?";
        params.add(idEleve)
                .add(idTypePeriode)
                .add(idStructure);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void getAvisConseil (String idEleve, Long idPeriode, String idStructure, Handler<Either<String, JsonArray>> handler) {

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "";

        query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA + ".avis_conseil_de_classe " +
                "INNER JOIN "+ Competences.COMPETENCES_SCHEMA + ".avis_conseil_bilan_periodique " +
                "ON(avis_conseil_bilan_periodique.id = avis_conseil_de_classe.id_avis_conseil_bilan)  " +
                "WHERE "+ Competences.COMPETENCES_SCHEMA + ".avis_conseil_de_classe.id_eleve = ? " +
                "AND "+ Competences.COMPETENCES_SCHEMA + ".avis_conseil_de_classe.id_etablissement = ? ";

        params.add(idEleve);
        params.add(idStructure);

        if(idPeriode != null){
            query += "AND "+ Competences.COMPETENCES_SCHEMA + ".avis_conseil_de_classe.id_periode = ? ";
            params.add(idPeriode);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }


}
