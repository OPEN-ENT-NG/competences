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
                            "id, intitule, id_thematique, description, type_elt_bilan_periodique, id_etablissement) " +
                            "VALUES (?, ?, ?, ?, ?, ?);";

                    JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                            .add(idElement)
                            .add(element.getString("libelle"))
                            .add(element.getInteger("theme"))
                            .add(element.getString("description"))
                            .add(element.getInteger("type"))
                            .add(element.getString("idEtablissement"));
                    statements.prepared(query, params);

                    insertRelEltIntervenantMatiere(element.getJsonArray("ens_mat"), idElement, statements);
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
                    "VALUES (?, ?, ?);";
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                    .add(elementId)
                    .add(intervenantMatiere.getString("ens"))
                    .add(intervenantMatiere.getString("mat"));
            statements.prepared(query, params);
        }
    }

    /**
     * Enregistremet de la relation élément-intervenants-matieres du nouvel élément
     * (EPI, AP ou parcours) en cours d'insertion.
     * @param IdGroupes association intervenant - matière de élément en cours d'insertion
     * @param elementId id de l'élément
     * @param statements Sql statement builder
     */
    private void insertRelEltgroupe(JsonArray IdGroupes, Long elementId, SqlStatementsBuilder statements){

        for (Object o : IdGroupes) {
            String idGroup = (String) o;
            String query = "INSERT INTO " + Competences.COMPETENCES_SCHEMA +
                    ".rel_elt_bilan_periodique_groupe(id_elt_bilan_periodique, id_groupe) " +
                    "VALUES (?, ?);";
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                    .add(elementId)
                    .add(idGroup);
            statements.prepared(query, params);
        }
    }

    @Override
    public void getThematiqueBilanPeriodique (Long typeElement, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id, libelle, code ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".thematique_bilan_periodique ")
                .append("WHERE type_elt_bilan_periodique = ? ");

        params.add(typeElement);
        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getElementBilanPeriodique (String idEnseignant, String idClasse, String idEtablissement, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT intitule, description, thematique.libelle as theme ")
                .append("FROM " + Competences.COMPETENCES_SCHEMA + ".elt_bilan_periodique ");

        if(idClasse != null){
            query.append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_groupe ")
                    .append("ON rel_elt_bilan_periodique_groupe.id_elt_bilan_periodique = elt_bilan_periodique.id ")
                    .append("AND rel_elt_bilan_periodique_groupe.id_groupe = ? ");
            params.add(idClasse);
        }

        if(idEnseignant != null){
            query.append("INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_elt_bilan_periodique_intervenant_matiere ")
                    .append("ON rel_elt_bilan_periodique_intervenant_matiere.id_elt_bilan_periodique = elt_bilan_periodique.id ")
                    .append("AND rel_elt_bilan_periodique_intervenant_matiere.id_intervenant = ? ");
            params.add(idEnseignant);
        }

        query.append("WHERE id_etablissement = ? ");
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
    public void getAppreciationsBilanPeriodique (String[] idElements, String type, Handler<Either<String, JsonArray>> handler){

        StringBuilder query = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        query.append("SELECT id_elt_bilan_periodique, id_periode, commentaire ");

        if("eleve".equals(type)){
            query.append(", id_eleve ");
        }

        if("eleve".equals(type)){
            query.append("FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_eleve ");
        } else {
            query.append("FROM " + Competences.COMPETENCES_SCHEMA + ".appreciation_elt_bilan_periodique_classe ");
        }

        query.append("WHERE id_elt_bilan_periodique IN " + Sql.listPrepared(idElements));

        for (int i = 0; i < idElements.length; i++) {
            params.add(idElements[i]);
        }

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validResultHandler(handler));
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
    public void updateElementBilanPeriodique (JsonObject element, Handler<Either<String, JsonObject>> handler){

        //UPDATE notes.type_elt_bilan_periodique SET code='Parcours' WHERE nom='Parcours';
    }

    @Override
    public void deleteElementBilanPeriodique (Long idEltBilanPeriodique, Handler<Either<String, JsonObject>> handler){
        String query = "DELETE FROM table WHERE id_etablissement = ?";
        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(idEltBilanPeriodique), SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void updateAppreciationBilanPeriodique (Long idAppreciation, String commentaire, String type,
                                                   Handler<Either<String, JsonObject>> handler){

    }

    @Override
    public void deleteAppreciationBilanPeriodique (Long idAppreciation, String type, Handler<Either<String, JsonObject>> handler){

    }
}

