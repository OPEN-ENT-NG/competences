package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.MatiereService;
import fr.openent.competences.utils.FormateFutureEvent;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import io.vertx.core.Handler;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.*;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.getLibelle;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;


public class DefaultMatiereService extends SqlCrudService implements MatiereService {

    private static final Logger log = LoggerFactory.getLogger(DefaultMatiereService.class);
    private static String subjectLibelleTable = VSCO_SCHEMA + "." + VSCO_MATIERE_LIBELLE_TABLE;
    private static String modelSubjectLibelleTable = VSCO_SCHEMA + "." + VSCO_MODEL_MATIERE_LIBELLE_TABLE;
    private  EventBus eb;
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
    public void getLibellesCourtsMatieres(Handler<Either<String, Map<String,String>>> handler) {

        String query = "SELECT code, libelle_court FROM "+ this.resourceTable;
        Map<String,String> mapCodeLibelleCourt = new HashMap<>();

        Sql.getInstance().prepared(query ,new JsonArray(), Competences.DELIVERY_OPTIONS,
                SqlResult.validResultHandler( event -> {

                    if(event.isRight()){
                        JsonArray codesLibellesCourts = event.right().getValue();

                        for(int i = 0; i < codesLibellesCourts.size(); i++ ){
                            if(!mapCodeLibelleCourt.containsKey(codesLibellesCourts.getJsonObject(i).getString(CODE))) {
                                mapCodeLibelleCourt.put(codesLibellesCourts.getJsonObject(i).getString(CODE),
                                        codesLibellesCourts.getJsonObject(i).getString(LIBELLE_COURT));
                            }
                        }
                        handler.handle(new Either.Right<>(mapCodeLibelleCourt));
                    }else{
                        handler.handle(new Either.Right<>(mapCodeLibelleCourt));
                        log.error("getLibellesCourtsMatieres : " + event.left().getValue());
                    }

                }));

    }

    private JsonObject createModel (String title, Long idModel, String idStructure) {
        String query =
                " INSERT INTO " + modelSubjectLibelleTable +
                        " (id, title, id_etablissement ) " +
                        " VALUES " +
                        " (?, ?, ?) ";

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        values.add(idModel).add(title).add(idStructure);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    private JsonObject updateModel (String title, Long idModel, String idStructure) {
        String query =
                " UPDATE " + modelSubjectLibelleTable +
                        " SET title = ?, id_etablissement =? " +
                        " WHERE id = ? ";

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        values.add(title).add(idStructure).add(idModel);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    private JsonObject saveLibelleSubjectModel ( String libelle, Long idModel, String idSubject) {
        String query =
                " INSERT INTO " + subjectLibelleTable +
                        " (id_model, libelle, external_id_subject ) " +
                        " VALUES " +
                        " (?, ?, ?) " +
                        " ON CONFLICT (external_id_subject, id_model) DO UPDATE SET libelle = ? ";

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        values.add(idModel).add(libelle).add(idSubject).add(libelle);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    private void buildStatement (String idStructure, String title, Long idModel,
                                 JsonArray libelleMatiere, boolean create,
                                 Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();

        if (create) {
            statements.add(createModel(title, idModel, idStructure));
        }
        else {
            statements.add(updateModel(title, idModel, idStructure));
        }

        for (int i =0; i < libelleMatiere.size(); i++) {
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
            final String queryNewCours =
                    "SELECT nextval('" + VSCO_SCHEMA + ".model_subject_libelle_id_seq') as id";

            sql.raw(queryNewCours, SqlResult.validUniqueResultHandler(event -> {
                if (event.isRight()) {
                    Long id = event.right().getValue().getLong(ID_KEY);
                    buildStatement(idStructure, title, id, libelleMatiere, true, handler);
                }
                else {
                    handler.handle(new Either.Left<>(event.left().getValue()));
                }
            }));
        }
        else {
            buildStatement(idStructure, title, idModel, libelleMatiere, false, handler);
        }
    }

    private void getLibelleMatierePostgres(String idStructure, Long idModel,
                                           Handler<Either<String, JsonArray>> handler) {

        String query =" SELECT title, id_etablissement, id_model, libelle, external_id_subject " +
                " FROM " + modelSubjectLibelleTable +
                " INNER JOIN " + subjectLibelleTable +
                " ON id_model = id  AND  id_etablissement = ? " +
                ((idModel != null)? " AND id_model = ? " : "") +
                " ORDER BY  libelle;";
        JsonArray params = new JsonArray();
        params.add(idStructure);
        if(idModel != null) {
            params.add(idModel);
        }
        sql.prepared(query, params, SqlResult.validResultHandler(handler));

    }

    private void getDefaultLibele(Handler<Either<String, JsonArray>> handler) {
        String query = " SELECT * FROM " + EVAL_SCHEMA + "." + VSCO_MATIERE_TABLE;
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private String defaultLibelle(String libelle) {
        return libelle.substring(0, Math.min(3, libelle.length() -1));
    }

    private String getDefaultlibelle(String code, JsonArray defaultSubject){
        String libelle = null;
        for (int i = 0; i< defaultSubject.size(); i++){
            JsonObject subject = defaultSubject.getJsonObject(i);
            String codePostgre = subject.getString(CODE);

            if(code.equals(codePostgre)) {
                libelle = subject.getString(LIBELLE_COURT);
                break;
            }
        }
        return libelle;
    }
    private void setLibelleSubject(JsonObject subject, JsonArray defaultSubject){
        String name = subject.getString(NAME);
        String code = subject.getString(EXTERNAL_ID_KEY);
        String source = subject.getString("source");

        String libelle;
        if(defaultSubject != null) {
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
    private void getNameSubject(JsonObject subject, JsonArray defaultSubject){
        if(defaultSubject != null) {
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
        for(int i=0; i < subjects.size(); i++){
            JsonObject subject = subjects.getJsonObject(i);
            setLibelleSubject(subject, defaultSubjects);
        }
    }
    public void getModels(String idStructure, Long idModelToget, Handler<Either<String, JsonArray>> handler){

        // Récupération des matières de l'établissement dans l'annuaire
        Future<JsonArray> subjectNeo = Future.future();
        listMatieresEtab(idStructure, subjectsEvent ->
                FormateFutureEvent.formate(subjectNeo, subjectsEvent));

        // Récupération des libelles courts dans la table notes.matiere
        Future<JsonArray> libelleCourt = Future.future();
        getDefaultLibele( event ->
                FormateFutureEvent.formate(libelleCourt, event));

        // Récupération des libelles et des models de l'établissement
        Future<JsonArray> modelsFuture = Future.future();
        getLibelleMatierePostgres(idStructure, idModelToget, modelsEvent ->
                FormateFutureEvent.formate(modelsFuture, modelsEvent));

        CompositeFuture.all(subjectNeo, modelsFuture, libelleCourt).setHandler(
                event -> {
                    if (event.failed()) {
                        handler.handle(new Either.Left<>(event.cause().getMessage()));
                    }
                    else {
                        JsonArray subjects = subjectNeo.result();
                        JsonArray defaultSubject = libelleCourt.result();
                        JsonArray models = new JsonArray().add(new JsonObject()
                                .put(TITLE, getLibelle("evaluations.default.model.libelle"))
                                .put(SUBJECTS, subjects));

                        // Construction des libelles par défault
                        buildSubjectForDefaultModel(subjects, defaultSubject);

                        JsonArray modelsMatiere = modelsFuture.result();

                        // - Groupement des libelles enregistrés par model
                        // - Et mapping (par l'externalId) de chaque libelle enregistré dans postgres
                        //   avec le nom complet (stocké dans Neo).
                        Map<Long, JsonObject> modelsMap = new HashMap<>();
                        for (int i= 0; i < modelsMatiere.size(); i++ ){
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

                        for (Map.Entry<Long, JsonObject> model: modelsMap.entrySet()) {
                            JsonArray modelSubject = model.getValue().getJsonArray(SUBJECTS);
                            Long id = model.getKey();
                            if (idModelToget != null && idModelToget.equals(id)) {
                                models = new JsonArray().add(modelToJson(id, modelSubject));
                                break;
                            }
                            models.add(modelToJson(id, modelSubject));
                        }
                        handler.handle( new Either.Right<>(models));
                    }
                }
        );
    }

    private JsonObject modelToJson ( Long id, JsonArray modelSubject) {
        String title = modelSubject.getJsonObject(0).getString(TITLE);
        return new JsonObject().put(TITLE, title).put("id", id)
                .put(SUBJECTS, modelSubject);
    }

    private void listMatieresEtab(String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "matiere.listMatieresEtab")
                .put(ID_STRUCTURE_KEY, idStructure)
                .put("onlyId", false);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler( message -> {
                    JsonObject body = message.body();
                    if (OK.equals(body.getString(STATUS))) {
                        handler.handle(new Either.Right<>(body.getJsonArray(RESULTS)));
                    }
                    else {
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
}
