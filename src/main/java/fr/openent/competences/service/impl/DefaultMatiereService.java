package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.model.Subject;
import fr.openent.competences.service.MatiereService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import io.vertx.core.Handler;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.*;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.*;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.sql.Sql.listPrepared;


public class DefaultMatiereService extends SqlCrudService implements MatiereService {

    private static final Logger log = LoggerFactory.getLogger(DefaultMatiereService.class);
    private static final String subjectLibelleTable = VSCO_SCHEMA + "." + VSCO_MATIERE_LIBELLE_TABLE;
    private static final String modelSubjectLibelleTable = VSCO_SCHEMA + "." + VSCO_MODEL_MATIERE_LIBELLE_TABLE;
    private static final String underSubjectTable = VSCO_SCHEMA + "." + VSCO_SOUS_MATIERE_TABLE;
    private static final String typeUnderSubjectTable = VSCO_SCHEMA + "." + VSCO_TYPE_SOUS_MATIERE_TABLE;

    private final EventBus eb;
    private static final String LIBELLE_COURT = "libelle_court";

    public DefaultMatiereService(EventBus eb) {
        super(Competences.COMPETENCES_SCHEMA, Competences.MATIERE_TABLE);
        this.eb = eb;
    }

    public DefaultMatiereService() {
        super(Competences.COMPETENCES_SCHEMA, Competences.MATIERE_TABLE);
        this.eb = null;
    }

    @Override
    public void getLibellesCourtsMatieres(Boolean wantMapCodeLibelle, Handler<Either<String, Map<String, String>>> handler) {
        String query = "SELECT bcn, code, libelle_court FROM " + this.resourceTable + " ;";
        Map<String, String> responseMap = new HashMap<>();

        Sql.getInstance().prepared(query, new JsonArray(), Competences.DELIVERY_OPTIONS,
                SqlResult.validResultHandler(event -> {
                    if (event.isRight()) {
                        JsonArray codesLibellesCourts = event.right().getValue();

                        for (int i = 0; i < codesLibellesCourts.size(); i++) {
                            if (!responseMap.containsKey(codesLibellesCourts.getJsonObject(i).getString(CODE))) {
                                //if you want map<codeMatiere,libelleCourt> get only libelle_court of bcn
                                if (wantMapCodeLibelle && codesLibellesCourts.getJsonObject(i).getBoolean("bcn")) {
                                    responseMap.put(codesLibellesCourts.getJsonObject(i).getString(CODE),
                                            codesLibellesCourts.getJsonObject(i).getString(LIBELLE_COURT));
                                } else {//if you want map<libelleCourt,codeMatiere get all libelle_court
                                    responseMap.put(codesLibellesCourts.getJsonObject(i).getString(LIBELLE_COURT),
                                            codesLibellesCourts.getJsonObject(i).getString(CODE));
                                }
                            }
                        }
                        handler.handle(new Either.Right<>(responseMap));
                    } else {
                        handler.handle(new Either.Right<>(responseMap));
                        log.error("getLibellesCourtsMatieres : " + event.left().getValue());
                    }
                }));
    }

    private JsonObject createModel(String title, Long idModel, String idStructure) {
        String query = "INSERT INTO " + modelSubjectLibelleTable + " (id, title, id_etablissement) " +
                " VALUES (?, ?, ?) ";

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        values.add(idModel).add(title).add(idStructure);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    private JsonObject updateModel(String title, Long idModel, String idStructure) {
        String query = "UPDATE " + modelSubjectLibelleTable +
                " SET title = ?, id_etablissement = ? " +
                " WHERE id = ? ";

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        values.add(title).add(idStructure).add(idModel);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    private JsonObject saveLibelleSubjectModel(String libelle, Long idModel, String idSubject) {
        String query = " INSERT INTO " + subjectLibelleTable + " (id_model, libelle, external_id_subject) " +
                " VALUES (?, ?, ?) " +
                " ON CONFLICT (external_id_subject, id_model) DO UPDATE SET libelle = ? ";

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        values.add(idModel).add(libelle).add(idSubject).add(libelle);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    private void buildStatement(String idStructure, String title, Long idModel, JsonArray libelleMatiere,
                                boolean create, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();

        if (create) {
            statements.add(createModel(title, idModel, idStructure));
        } else {
            statements.add(updateModel(title, idModel, idStructure));
        }

        for (int i = 0; i < libelleMatiere.size(); i++) {
            JsonObject subject = libelleMatiere.getJsonObject(i);
            String libelle = subject.getString(LIBELLE);
            String idSubject = subject.getString(EXTERNAL_ID_SUBJECT);
            statements.add(saveLibelleSubjectModel(libelle, idModel, idSubject));
        }

        sql.transaction(statements, SqlResult.validRowsResultHandler(handler));
    }

    public void saveModel(String idStructure, String title, final Long idModel, JsonArray libelleMatiere,
                          Handler<Either<String, JsonObject>> handler) {
        if (idModel == null) {
            final String queryNewCours = "SELECT nextval('" + VSCO_SCHEMA + ".model_subject_libelle_id_seq') as id";

            sql.raw(queryNewCours, SqlResult.validUniqueResultHandler(event -> {
                if (event.isRight()) {
                    Long id = event.right().getValue().getLong(ID_KEY);
                    buildStatement(idStructure, title, id, libelleMatiere, true, handler);
                } else {
                    handler.handle(new Either.Left<>(event.left().getValue()));
                }
            }));
        } else {
            buildStatement(idStructure, title, idModel, libelleMatiere, false, handler);
        }
    }

    private void getLibelleMatierePostgres(String idStructure, Long idModel,
                                           Handler<Either<String, JsonArray>> handler) {

        String query = " SELECT title, id_etablissement, id_model, libelle, external_id_subject " +
                " FROM " + modelSubjectLibelleTable +
                " INNER JOIN " + subjectLibelleTable +
                " ON id_model = id  AND  id_etablissement = ? " +
                ((idModel != null) ? " AND id_model = ? " : "") +
                " ORDER BY  libelle;";
        JsonArray params = new JsonArray();
        params.add(idStructure);
        if (idModel != null) {
            params.add(idModel);
        }
        sql.prepared(query, params, SqlResult.validResultHandler(handler));

    }

    private void getDefaultLibele(Handler<Either<String, JsonArray>> handler) {
        String query = " SELECT * FROM " + EVAL_SCHEMA + "." + VSCO_MATIERE_TABLE;
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private String defaultLibelle(String libelle) {
        return libelle.substring(0, Math.min(3, libelle.length() - 1));
    }

    private String getDefaultlibelle(String code, JsonArray defaultSubject) {
        String libelle = null;
        for (int i = 0; i < defaultSubject.size(); i++) {
            JsonObject subject = defaultSubject.getJsonObject(i);
            String codePostgre = subject.getString(CODE);

            if (code.equals(codePostgre)) {
                libelle = subject.getString(LIBELLE_COURT);
                break;
            }
        }
        return libelle;
    }

    private void setLibelleSubject(JsonObject subject, JsonArray defaultSubject) {
        String name = subject.getString(NAME);
        String code = subject.getString(EXTERNAL_ID_KEY);
        String source = subject.getString("source");

        String libelle;
        if (defaultSubject != null) {
            if ("AAF".equals(source)) {
                libelle = getDefaultlibelle(code, defaultSubject);
            } else {
                libelle = code;
            }

            if (libelle == null) {
                libelle = defaultLibelle(name);
            }

            subject.put(LIBELLE, libelle);
        }
    }

    private void getNameSubject(JsonObject subject, JsonArray defaultSubject) {
        if (defaultSubject != null) {
            String externalId = subject.getString(EXTERNAL_ID_SUBJECT);

            for (int i = 0; i < defaultSubject.size(); i++) {
                JsonObject defaultSubjectJsonObject = defaultSubject.getJsonObject(i);
                String externalIdSubject = defaultSubjectJsonObject.getString(EXTERNAL_ID_SUBJECT);
                if (externalId.equals(externalIdSubject)) {
                    subject.put(NAME, defaultSubjectJsonObject.getValue(NAME));
                    subject.put(ID_KEY, defaultSubjectJsonObject.getValue(ID_KEY));
                    break;
                }
            }
        }
    }

    private void buildSubjectForDefaultModel(JsonArray subjects, JsonArray defaultSubjects) {
        for (int i = 0; i < subjects.size(); i++) {
            JsonObject subject = subjects.getJsonObject(i);
            setLibelleSubject(subject, defaultSubjects);
        }
    }

    public void getModels(String idStructure, Long idModelToget, Handler<Either<String, JsonArray>> handler) {

        // Récupération des matières de l'établissement dans l'annuaire
        Promise<JsonArray> subjectNeoPromise = Promise.promise();

        listMatieresEtab(idStructure, subjectsEvent ->
                formate(subjectNeoPromise, subjectsEvent));

        // Récupération des libelles courts dans la table notes.matiere
        Promise<JsonArray> libelleCourtPromise = Promise.promise();
        getDefaultLibele(event ->
                formate(libelleCourtPromise, event));

        // Récupération des libelles et des models de l'établissement
        Promise<JsonArray> modelsPromise = Promise.promise();
        getLibelleMatierePostgres(idStructure, idModelToget, modelsEvent ->
                formate(modelsPromise, modelsEvent));

        Future.all(subjectNeoPromise.future(), modelsPromise.future(), libelleCourtPromise.future()).onComplete(
                event -> {
                    if (event.failed()) {
                        handler.handle(new Either.Left<>(event.cause().getMessage()));
                    } else {
                        JsonArray subjects = subjectNeoPromise.future().result();
                        JsonArray defaultSubject = libelleCourtPromise.future().result();
                        JsonArray models = new JsonArray().add(new JsonObject()
                                .put(TITLE, getLibelle("evaluations.default.model.libelle"))
                                .put(SUBJECTS, subjects));

                        // Construction des libelles par défault
                        buildSubjectForDefaultModel(subjects, defaultSubject);

                        JsonArray modelsMatiere = modelsPromise.future().result();

                        // - Groupement des libelles enregistrés par model
                        // - Et mapping (par l'externalId) de chaque libelle enregistré dans postgres
                        //   avec le nom complet (stocké dans Neo).
                        Map<Long, JsonObject> modelsMap = new HashMap<>();
                        for (int i = 0; i < modelsMatiere.size(); i++) {
                            JsonObject subject = modelsMatiere.getJsonObject(i);
                            Long idModel = subject.getLong("id_model");
                            String title = subject.getString(TITLE);
                            if (!modelsMap.containsKey(idModel)) {
                                modelsMap.put(idModel, new JsonObject().put(TITLE, title)
                                        .put(SUBJECTS, new JsonArray()));
                            }
                            getNameSubject(subject, subjects);
                            modelsMap.get(idModel).getJsonArray(SUBJECTS).add(subject);
                        }

                        for (Map.Entry<Long, JsonObject> model : modelsMap.entrySet()) {
                            JsonArray modelSubject = model.getValue().getJsonArray(SUBJECTS);
                            Long id = model.getKey();
                            if (idModelToget != null && idModelToget.equals(id)) {
                                models = new JsonArray().add(modelToJson(id, modelSubject));
                                break;
                            }
                            models.add(modelToJson(id, modelSubject));
                        }
                        handler.handle(new Either.Right<>(models));
                    }
                }
        );
    }

    private JsonObject modelToJson(Long id, JsonArray modelSubject) {
        String title = modelSubject.getJsonObject(0).getString(TITLE);
        return new JsonObject().put(TITLE, title).put("id", id)
                .put(SUBJECTS, modelSubject);
    }

    private void listMatieresEtab(String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "matiere.listMatieresEtab")
                .put(ID_STRUCTURE_KEY, idStructure)
                .put("onlyId", false);
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(message -> {
                    JsonObject body = message.body();
                    if (OK.equals(body.getString(STATUS))) {
                        handler.handle(new Either.Right<>(body.getJsonArray(RESULTS)));
                    } else {
                        handler.handle(new Either.Left<>(body.getString(MESSAGE)));
                    }

                }));

    }

    public void deleteModeleLibelle(String idModel, Handler<Either<String, JsonArray>> handler) {


        String query = " DELETE FROM " + modelSubjectLibelleTable + " WHERE id = ? ";
        JsonArray params = new JsonArray();
        params.add(Long.valueOf(idModel));
        sql.prepared(query, params, SqlResult.validResultHandler(handler));

    }

    public void getSousMatieres(String idMatiere, String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = " SELECT id_type_sousmatiere as id_sousmatiere, id_matiere , libelle " +
                " FROM " + underSubjectTable + "  INNER JOIN " + typeUnderSubjectTable +
                " ON id_type_sousmatiere = type_sousmatiere.id " +
                " WHERE id_matiere = ? AND id_structure = ?";

        JsonArray params = new JsonArray().add(idMatiere).add(idStructure);
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    public Future<JsonArray> getUnderSubjects(String subjectId, String strutureId) {
        Promise<JsonArray> underSubjectPromise = Promise.promise();
        getSousMatieres(subjectId, strutureId, FutureHelper.handlerJsonArray(underSubjectPromise,
                String.format("[Competences@%s::getUnderSubjects] Error during sql request: ",
                        this.getClass().getSimpleName())));
        return underSubjectPromise.future();
    }

    @Override
    public Future<JsonArray> getMatieresEtab(String idStructure) {
        Promise<JsonArray> promise = Promise.promise();
        getMatieresEtab(idStructure, FutureHelper.handler(promise,
                String.format("[Competences@%s::getMatieresEtab] Error during sql request: ",
                this.getClass().getSimpleName())));
        return promise.future();
    }

    /**
     * @deprecated Use @link {#getMatieresEtab(String idStructure)}
     * @param idEtablissement
     * @param handler
     */
    @Override
    @Deprecated
    public void getMatieresEtab(String idEtablissement, Handler<Either<String, JsonArray>> handler) {
        //vu qu on use pas le user peut être appeller un autre fonction de viesco?
        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatieresForUser")
                .put("userType", "Personnel")
                .put("idUser", "null")
                .put("idStructure", idEtablissement)
                .put("onlyId", false);
        eb.request(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                handler.handle(new Either.Right<>(body.getJsonArray(RESULTS)));
            } else {
                handler.handle(new Either.Left<>(body.getString(MESSAGE)));
            }
        }));
    }

    private void getDevoirsToUpdate(JsonArray idsMatieres, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT devoirs.*\n" +
                " FROM " + COMPETENCES_SCHEMA + ".devoirs\n" +
                " INNER JOIN " +
                "      " + VSCO_SCHEMA + ".sousmatiere " +
                "       ON devoirs.id_matiere = sousmatiere.id_matiere  AND " +
                (isNull(idsMatieres) ? "true" : listPrepared(idsMatieres.getList())) +
                "       AND devoirs.id_sousmatiere IS NULL;";

        JsonArray params = new JsonArray();
        if (isNotNull(idsMatieres)) {
            params.addAll(idsMatieres);
        }
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private void getSubJectInfos(JsonArray idsMatieres, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id_type_sousmatiere as id_sousmatiere, id_matiere, libelle, id_structure " +
                " FROM " + VSCO_SCHEMA + ".sousmatiere " +
                " INNER JOIN " + VSCO_SCHEMA + ".type_sousmatiere " +
                " ON sousmatiere.id_type_sousmatiere = type_sousmatiere.id AND " +
                (isNull(idsMatieres) ? "true" : listPrepared(idsMatieres.getList()));

        JsonArray params = new JsonArray();
        if (isNotNull(idsMatieres)) {
            params.addAll(idsMatieres);
        }
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private JsonObject buildUpdateDevoir(JsonObject devoir, JsonObject sousMatiere) {
        String query = "UPDATE " + COMPETENCES_SCHEMA + ".devoirs " +
                " SET id_sousmatiere = ? " +
                " WHERE id = ? ";

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
                .add(sousMatiere.getLong("id_sousmatiere"))
                .add(devoir.getLong(ID_KEY));

        return new JsonObject().put("statement", query).put("values", values).put("action", "prepared");
    }

    public void updateDevoirs(JsonArray idsMatieres, Handler<Either<String, JsonArray>> handler) {
        //Récupération des matières avec sousMatières
        Promise<JsonArray> subjectPromise = Promise.promise();
        getSubJectInfos(idsMatieres, event -> formate(subjectPromise, event));

        //Récupération des devoirs rattachés auxMatières avec sousMAtières
        Promise<JsonArray> devoirsPromise = Promise.promise();
        getDevoirsToUpdate(idsMatieres, event -> formate(devoirsPromise, event));

        Future.all(devoirsPromise.future(), subjectPromise.future()).onComplete(event -> {
            if (event.failed()) {
                returnFailure("updateDevoirs", event, handler);
                return;
            }
            JsonArray subjects = subjectPromise.future().result();
            Map<String, JsonObject> defaultSousMatiere = new HashMap<>();
            for (int i = 0; i < subjects.size(); i++) {
                JsonObject subject = subjects.getJsonObject(i);
                String idMatiere = subject.getString(ID_MATIERE);
                if (defaultSousMatiere.containsKey(idMatiere)) {
                    continue;
                }
                defaultSousMatiere.put(idMatiere, subject);
            }

            JsonArray statements = new JsonArray();
            JsonArray devoirs = devoirsPromise.future().result();
            for (int i = 0; i < devoirs.size(); i++) {
                JsonObject devoir = devoirs.getJsonObject(i);
                String idMatiereDevoir = devoir.getString(ID_MATIERE);
                JsonObject sousMatiere = defaultSousMatiere.get(idMatiereDevoir);
                statements.add(buildUpdateDevoir(devoir, sousMatiere));
                devoir.put("sousMatiere", sousMatiere.getString(LIBELLE));
            }
            sql.transaction(statements, SqlResult.validResultsHandler(statementEvent -> {
                if (statementEvent.isLeft()) {
                    handler.handle(statementEvent.left());
                } else {
                    handler.handle(new Either.Right<>(devoirs));
                }
            }));


        });
    }

    public void removeRankOnSubject(Subject subject, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        JsonObject params = new JsonObject();

        query.append("MATCH (subject:Subject)-[sj:SUBJECT]->(s:Structure {id: {idStructure} }) ")
                .append("WHERE subject.rank IS NOT NULL ")
                .append("WITH subject.id AS idSubjects ")
                .append("MATCH (sub:Subject) ")
                .append("WHERE sub.id IN [idSubjects] ")
                .append("REMOVE sub.rank ")
                .append("RETURN COLLECT(sub.id) AS ids ;");

        params.put("idStructure", subject.getIdStructure());

        Neo4j.getInstance().execute(query.toString(), params, Neo4jResult.validUniqueResultHandler(handler));
    }

    public void updateListRank(List<Subject> subjects, Handler<Either<String, JsonObject>> handler) {
        StringBuilder queryMatch = new StringBuilder();
        StringBuilder queryFinal = new StringBuilder();
        StringBuilder querySet = new StringBuilder();
        StringBuilder queryReturn = new StringBuilder();
        JsonObject params = new JsonObject();

        for (int subjectNumber = 0; subjectNumber < subjects.size(); subjectNumber++) {
            Subject subject = subjects.get(subjectNumber);
            String comma = subjectNumber < subjects.size() - 1 ? ", " : " ";

            String idSubjectNeo4j = "id" + subjectNumber;
            String rankSubjectNeo4j = "rank" + subjectNumber;
            String queryId = "{" + idSubjectNeo4j + "} ";

            // Match all subject
            params.put(idSubjectNeo4j, subject.getId());
            queryMatch.append("(" + idSubjectNeo4j + ":Subject{id: " + queryId + " })" + comma);

            // Set rank on subject match
            params.put(rankSubjectNeo4j, subject.getRank());
            querySet.append(idSubjectNeo4j + ".rank = {" + rankSubjectNeo4j + "}" + comma);

            // Return all id subject
            queryReturn.append(idSubjectNeo4j + ".id" + comma);
        }

        queryFinal.append("MATCH ")
                .append(queryMatch.toString())
                .append("SET ")
                .append((querySet.toString()))
                .append("WITH ")
                .append("([" + queryReturn.toString() + "]) AS ids ")
                .append("RETURN ids ;");

        Neo4j.getInstance().execute(queryFinal.toString(), params, Neo4jResult.validUniqueResultHandler(handler));
    }
}