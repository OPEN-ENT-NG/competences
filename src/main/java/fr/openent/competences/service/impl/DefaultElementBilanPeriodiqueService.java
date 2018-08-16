package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.ElementBilanPeriodiqueService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import java.util.List;
import java.util.Map;

import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;


public class DefaultElementBilanPeriodiqueService extends SqlCrudService implements ElementBilanPeriodiqueService {

    public DefaultElementBilanPeriodiqueService() {
        super(Competences.COMPETENCES_SCHEMA, null);
    }

    @Override
    public void insertThematiqueBilanPeriodique (JsonObject thematique, Handler<Either<String, JsonObject>> handler){

        SqlStatementsBuilder statements = new SqlStatementsBuilder();
        String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique(libelle, code, type_elt_bilan_periodique) VALUES (?, ?, ?);";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(thematique.getString("libelle"))
                .add(thematique.getString("code"))
                .add(thematique.getInteger("type"));

        statements.prepared(query, params);
                Sql.getInstance().prepared(query.toString(), params, validResultHandler(handler));

    }

    @Override
    public void insertElementBilanPeriodique (JsonObject element, Handler<Either<String, JsonObject>> handler){

        SqlStatementsBuilder statements = new SqlStatementsBuilder();

        final String queryElement =
                "SELECT nextval('" + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique_id_seq') as id";

        sql.raw(queryElement, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    Long idElement = event.right().getValue().getLong("id");

                    String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique(" +
                            "id, type_elt_bilan_periodique, id_etablissement ";

                    if(element.getInteger("type") == 1 || element.getInteger("type") == 2){
                        query += ", intitule, description";
                    }
                    if(element.getInteger("type") == 1 || element.getInteger("type") == 3){
                        query += ", id_thematique";
                    }

                    query += ") VALUES (?, ?, ?";
                    JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                            .add(idElement)
                            .add(element.getInteger("type"))
                            .add(element.getString("idEtablissement"));

                    if(element.getInteger("type") == 1 || element.getInteger("type") == 2){
                        query += ", ?, ?";
                        params.add(element.getString("libelle"))
                                .add(element.getString("description"));
                    }
                    if(element.getInteger("type") == 1 || element.getInteger("type") == 3){
                        query += ", ?";
                        params.add(element.getInteger("theme"));
                    }

                    query += ") RETURNING *;";

                    statements.prepared(query, params);

                    if(element.getInteger("type") == 1 || element.getInteger("type") == 2){
                        insertRelEltIntervenantMatiere(element.getJsonArray("ens_mat"), idElement, statements);
                    }
                    insertRelEltgroupe(element.getJsonArray("classes"), idElement, statements);
                }
                Sql.getInstance().transaction(statements.build(), SqlResult.validRowsResultHandler(handler));
            }
        }));
    }

    /**
     * Enregistremet de la relation élément-intervenants-matieres du nouvel élément
     * (EPI, AP ou parcours) en cours d'insertion.
     * @param intervenantsMatieres association intervenant - matière de élément en cours d'insertion
     * @param elementId id de l'élément
     * @param statements Sql statement builder
     */
    private void insertRelEltIntervenantMatiere(JsonArray intervenantsMatieres, Long elementId, SqlStatementsBuilder statements){

        for (Object o : intervenantsMatieres) {

            JsonObject intervenantMatiere = (JsonObject) o;
            String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA +
                    ".rel_elt_bilan_periodique_intervenant_matiere(id_elt_bilan_periodique, id_intervenant, id_matiere) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT ON CONSTRAINT elt_bilan_period_interv_mat_unique DO NOTHING;";
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                    .add(elementId)
                    .add(intervenantMatiere.getJsonObject("ens").getString("id"))
                    .add(intervenantMatiere.getJsonObject("mat").getString("id"));
            statements.prepared(query, params);
        }
    }

    /**
     * Enregistremet de la relation élément-intervenants-matieres du nouvel élément
     * (EPI, AP ou parcours) en cours d'insertion.
     * @param groupes association intervenant - matière de élément en cours d'insertion
     * @param elementId id de l'élément
     * @param statements Sql statement builder
     */
    private void insertRelEltgroupe(JsonArray groupes, Long elementId, SqlStatementsBuilder statements){

        for (Object o : groupes) {
            JsonObject group = (JsonObject) o;
            String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA +
                    ".rel_elt_bilan_periodique_groupe(id_elt_bilan_periodique, id_groupe, externalid_groupe) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT ON CONSTRAINT elt_bilan_period_groupe_unique DO NOTHING;";
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                    .add(elementId)
                    .add(group.getString("id"))
                    .add(group.getString("externalId"));
            statements.prepared(query, params);
        }
    }

    @Override
    public void getThematiqueBilanPeriodique (Long typeElement, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id, libelle, code, personnalise ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique ")
                .append("WHERE type_elt_bilan_periodique = ? ");

        params.add(typeElement);
        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getElementBilanPeriodique (String idEnseignant, String idClasse, String idEtablissement, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("(SELECT elt_bilan_periodique.*, thematique_bilan_periodique.libelle, string_agg(DISTINCT id_groupe, ',') AS groupes, ")
                .append(" array_agg(distinct CONCAT(id_intervenant, ',', id_matiere)) AS intervenants_matieres")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique ")
                .append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_groupe ")
                .append(" ON rel_elt_bilan_periodique_groupe.id_elt_bilan_periodique = elt_bilan_periodique.id ");
        if(idClasse != null){
            query.append(" AND rel_elt_bilan_periodique_groupe.id_groupe = ? ");
            params.add(idClasse);
        }

        query.append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_intervenant_matiere ")
                .append(" ON rel_elt_bilan_periodique_intervenant_matiere.id_elt_bilan_periodique = elt_bilan_periodique.id ");
        if(idEnseignant != null){
            query.append(" AND rel_elt_bilan_periodique_intervenant_matiere.id_intervenant = ? ");
            params.add(idEnseignant);
        }
        query.append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique ")
                .append(" ON elt_bilan_periodique.id_thematique = thematique_bilan_periodique.id ")
                .append(" WHERE id_etablissement = ? ")
                .append(" GROUP BY elt_bilan_periodique.id, thematique_bilan_periodique.libelle) ");
        params.add(idEtablissement);

        query.append(" UNION ")
                .append(" (SELECT elt_bilan_periodique.*, thematique_bilan_periodique.libelle, string_agg(DISTINCT id_groupe, ',') AS groupes, null ")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique ")
                .append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_groupe ")
                .append(" ON rel_elt_bilan_periodique_groupe.id_elt_bilan_periodique = elt_bilan_periodique.id ");
        if(idClasse != null){
            query.append(" AND rel_elt_bilan_periodique_groupe.id_groupe = ? ");
            params.add(idClasse);
        }
        query.append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique ")
                .append(" ON elt_bilan_periodique.id_thematique = thematique_bilan_periodique.id ")
                .append(" WHERE id_etablissement = ? ")
                .append(" AND intitule is null ")
                .append(" GROUP BY elt_bilan_periodique.id, thematique_bilan_periodique.libelle) ");
        params.add(idEtablissement);

        query.append(" UNION ")
                .append(" (SELECT elt_bilan_periodique.*, null, string_agg(DISTINCT id_groupe, ',') AS groupes, ")
                .append(" array_agg(distinct CONCAT(id_intervenant, ',', id_matiere)) AS intervenants_matieres")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique ")
                .append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_groupe ")
                .append(" ON rel_elt_bilan_periodique_groupe.id_elt_bilan_periodique = elt_bilan_periodique.id ");
        if(idClasse != null){
            query.append(" AND rel_elt_bilan_periodique_groupe.id_groupe = ? ");
            params.add(idClasse);
        }
        query.append(" INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_intervenant_matiere ")
                .append(" ON rel_elt_bilan_periodique_intervenant_matiere.id_elt_bilan_periodique = elt_bilan_periodique.id ");
        if(idEnseignant != null){
            query.append(" AND rel_elt_bilan_periodique_intervenant_matiere.id_intervenant = ? ");
            params.add(idEnseignant);
        }
        query.append(" WHERE id_etablissement = ? ")
                .append(" AND id_thematique is null ")
                .append(" GROUP BY elt_bilan_periodique.id) ");
        params.add(idEtablissement);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getGroupesElementBilanPeriodique (String idElement, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id_groupe ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_groupe ")
                .append("WHERE id_elt_bilan_periodique = ? ");
        params.add(idElement);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getIntervenantMatiereElementBilanPeriodique (String idElement, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id_intervenant, id_matiere ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_intervenant_matiere ")
                .append("WHERE id_elt_bilan_periodique = ? ");
        params.add(idElement);

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getApprecBilanPerClasse (String idClasse, List<String> idElements, Handler<Either<String, JsonArray>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT * " +
                "FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe ";

        if(idClasse != null){
            query += " INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_appreciation_elt_classe " +
                    " ON rel_groupe_appreciation_elt_classe.id_appreciation = appreciation_elt_bilan_periodique_classe.id " +
                    " AND rel_groupe_appreciation_elt_classe.id_groupe = ? ";
            params.add(idClasse);
        }

        query += "WHERE id_elt_bilan_periodique IN " + Sql.listPrepared(idElements);

        for (int i = 0; i < idElements.size(); i++) {
            params.add(idElements.get(i));
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getApprecBilanPerEleve (String idClasse, List<String> idElements, Handler<Either<String, JsonArray>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT * " +
                "FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve ";

        if(idClasse != null){
            query += " INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_appreciation_elt_eleve " +
                    " ON rel_groupe_appreciation_elt_classe.id_appreciation = appreciation_elt_bilan_periodique_eleve.id " +
                    " AND rel_groupe_appreciation_elt_eleve.id_groupe = ? ";
            params.add(idClasse);
        }

        query += " WHERE id_elt_bilan_periodique IN " + Sql.listPrepared(idElements);

        for (int i = 0; i < idElements.size(); i++) {
            params.add(idElements.get(i));
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void insertAppreciationEleve (String idEleve, Long idEltBilanPeriodique, Long idPeriode,
                                         String commentaire, Handler<Either<String, JsonObject>> handler){

        SqlStatementsBuilder statements = new SqlStatementsBuilder();

        final String queryAppreciation =
                "SELECT nextval('" + Competences.VSCO_SCHEMA + ".appreciation_elt_bilan_periodique_eleve') as id";

        sql.raw(queryAppreciation, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    Long idAppreciation = event.right().getValue().getLong("id");

                    String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve(" +
                            "id, id_eleve, id_elt_bilan_periodique, id_periode, commentaire) " +
                            "VALUES (?, ?, ?, ?, ?);";

                    JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                            .add(idAppreciation)
                            .add(idEleve)
                            .add(idEltBilanPeriodique)
                            .add(idPeriode)
                            .add(commentaire);

                    statements.prepared(query, params);
                }
                Sql.getInstance().transaction(statements.build(), SqlResult.validRowsResultHandler(handler));
            }
        }));
    }

    @Override
    public void insertAppreciationClasse (Long idEltBilanPeriodique, Long idPeriode,
                                          String commentaire, Handler<Either<String, JsonObject>> handler){

        SqlStatementsBuilder statements = new SqlStatementsBuilder();

        final String queryAppreciation =
                "SELECT nextval('" + Competences.VSCO_SCHEMA + ".appreciation_elt_bilan_periodique_classe') as id";

        sql.raw(queryAppreciation, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    Long idAppreciation = event.right().getValue().getLong("id");

                    String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe(" +
                            "id, id_elt_bilan_periodique, id_periode, commentaire) " +
                            "VALUES (?, ?, ?, ?);";

                    JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                            .add(idAppreciation)
                            .add(idEltBilanPeriodique)
                            .add(idPeriode)
                            .add(commentaire);

                    statements.prepared(query, params);
                }
                Sql.getInstance().transaction(statements.build(), SqlResult.validRowsResultHandler(handler));
            }
        }));
    }

    @Override
    public void updateElementBilanPeriodique (Long idElement, JsonObject element, Handler<Either<String, JsonObject>> handler){

        SqlStatementsBuilder statements = new SqlStatementsBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "UPDATE " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique SET ";

        if(element.getInteger("type") == 1 || element.getInteger("type") == 2){
            query += "intitule = ?, description = ?, ";
        }
        if(element.getInteger("type") == 1 || element.getInteger("type") == 3){
            query += "id_thematique = ?, ";
        }

        query.substring(0, query.length() - 3);

        query += " WHERE id = ?;";

        if(element.getInteger("type") == 1 || element.getInteger("type") == 2){
            params.add(element.getString("libelle"))
                .add(element.getString("description"));
        }
        if(element.getInteger("type") == 1 || element.getInteger("type") == 3){
            params.add(element.getInteger("theme"));
        }

        params.add(idElement);

        statements.prepared(query, params);

        if(element.getInteger("type") == 1 || element.getInteger("type") == 2){
            insertRelEltIntervenantMatiere(element.getJsonArray("ens_mat"), idElement, statements);
        }
        insertRelEltgroupe(element.getJsonArray("classes"), idElement, statements);

        Sql.getInstance().transaction(statements.build(), SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void deleteElementBilanPeriodique (List<String> idsEltBilanPeriodique, Handler<Either<String, JsonArray>> handler){

        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (String id : idsEltBilanPeriodique) {
            params.add(id);
        }

        String queryDelAppEleve = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve WHERE id_elt_bilan_periodique IN "
                + Sql.listPrepared(idsEltBilanPeriodique);

        String queryDelAppClasse = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe WHERE id_elt_bilan_periodique IN "
                + Sql.listPrepared(idsEltBilanPeriodique);

        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique WHERE id IN " + Sql.listPrepared(idsEltBilanPeriodique);

        statements.add(new JsonObject()
                .put("statement", queryDelAppEleve.toString())
                .put("values", params)
                .put("action", "prepared"));
        statements.add(new JsonObject()
                .put("statement", queryDelAppClasse.toString())
                .put("values", params)
                .put("action", "prepared"));
        statements.add(new JsonObject()
                .put("statement", query.toString())
                .put("values", params)
                .put("action", "prepared"));
        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteThematique (String idThematique, Handler<Either<String, JsonArray>> handler){
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique " +
                "WHERE id_elt_bilan_periodique = ? ";
        params.add(idThematique);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void updateThematique (String idThematique, JsonObject thematique,
                                  Handler<Either<String, JsonObject>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query ="UPDATE notes.thematique_bilan_periodique " +
                "SET libelle = ?, code = ? WHERE id = ?";
        params.add(thematique.getString("libelle"))
                .add(thematique.getString("code"))
                .add(idThematique);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getElementsOnThematique (String idThematique, Handler<Either<String, JsonArray>> handler){
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT * FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique " +
                "WHERE id_thematique = ? ";
        params.add(idThematique);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteApprecClasseElement (List<String> idsEltBilanPeriodique, Handler<Either<String, JsonArray>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (String id : idsEltBilanPeriodique) {
            params.add(id);
        }

        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe WHERE id_elt_bilan_periodique IN "
                + Sql.listPrepared(idsEltBilanPeriodique);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteApprecEleveElement (List<String> idsEltBilanPeriodique, Handler<Either<String, JsonArray>> handler){

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (String id : idsEltBilanPeriodique) {
            params.add(id);
        }

        String query = "DELETE FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve WHERE id_elt_bilan_periodique IN "
                + Sql.listPrepared(idsEltBilanPeriodique);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void updateAppreciationBilanPeriodique (Long idAppreciation, String commentaire, String type,
                                                   Handler<Either<String, JsonObject>> handler){

    }

    @Override
    public void deleteAppreciationBilanPeriodique (Long idAppreciation, String type, Handler<Either<String, JsonObject>> handler){

    }
}

