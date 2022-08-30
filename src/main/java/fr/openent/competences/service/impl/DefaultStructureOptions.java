package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.Utils;
import fr.openent.competences.service.StructureOptionsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

public class DefaultStructureOptions extends SqlCrudService implements StructureOptionsService {

    private EventBus eb;
    protected static final Logger log = LoggerFactory.getLogger(Utils.class);
    public DefaultStructureOptions () {
        super(Competences.EVAL_SCHEMA, Field.STRUTUCTURE_OPTIONS);
    }

    public DefaultStructureOptions (EventBus eb) {
        super(Competences.EVAL_SCHEMA, Field.STRUTUCTURE_OPTIONS);
        this.eb = eb;
    }

    @Override
    public void createOrUpdateIsAverageSkills (JsonObject body, Handler<Either<String, JsonObject>> handler) {
        final String structureId = body.getString(Field.STRUCTUREID);
        final boolean isAverageSkills = body.getBoolean(Field.ISSKILLAVERAGE);
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ").append(this.resourceTable)
                .append("(id_structure, is_average_skills)")
                .append("VALUES (?, ?)")
                .append("ON CONFLICT (id_structure) DO UPDATE SET is_average_skills = ? ");

        JsonArray values = new JsonArray().add(structureId).add(isAverageSkills).add(isAverageSkills);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getIsAverageSkills (String structureId, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT EXISTS (SELECT is_average_skills FROM ").append(this.resourceTable)
                .append(" WHERE id_structure = ? AND is_average_skills = TRUE) AS is_average_skills ");
        JsonArray params = new JsonArray().add(structureId);

        Sql.getInstance().prepared(query.toString(), params, Competences.DELIVERY_OPTIONS,
                validUniqueResultHandler(handler));
    }

    @Override
    public void activeDeactiveSyncStatePresences(String idStructure, Boolean state, Handler<Either<String, JsonObject>> eitherHandler){
        StringBuilder query = new StringBuilder().append("INSERT INTO ")
                .append(Competences.COMPETENCES_SCHEMA + ".structure_options (id_structure, presences_sync) ")
                .append(" VALUES ")
                .append(" ( ?, ? )")
                .append(" ON CONFLICT (id_structure) DO UPDATE SET presences_sync = ?");
        JsonArray params = new JsonArray().add(idStructure).add(state).add(state);
        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(eitherHandler));
    }


    @Override
    public void getSyncStatePresences(String idStructure, Handler<Either<String, JsonObject>> eitherHandler){
        JsonArray params = new JsonArray().add(idStructure);

        String query = "SELECT presences_sync " +
                " FROM " + Competences.COMPETENCES_SCHEMA + ".structure_options " +
                " WHERE id_structure = ? ";
        Sql.getInstance().prepared(query, params, Competences.DELIVERY_OPTIONS, validUniqueResultHandler(eitherHandler));

    }

    public void getActiveStatePresences ( final String idStructure, Handler<Either<String,JsonObject>> handler){
        // Récupération de la config vie scolaire
        Promise<JsonObject> configFuture = Promise.promise();
        JsonObject action = new JsonObject()
                .put("action", "config.generale");
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if (OK.equals(body.getString(STATUS))) {
                    JsonObject queryResult = body.getJsonObject(RESULT);
                    configFuture.complete(queryResult);
                } else {
                    log.error("getRetardsAndAbsences-getconfigVieScolaire failed : " + body.getString(Field.MESSAGE));
                    configFuture.fail(body.getString(Field.MESSAGE));

                }
            }
        }));

        // Récupération de l'activation du module présences de l'établissement
//        isStructureActivatePresences(idStructure,event -> formate(activationFuture,event));

        configFuture.future()
                .onSuccess(configEvent -> {
                    JsonObject result = new JsonObject();
                    boolean configInstalled = Boolean.TRUE.equals(configEvent.getBoolean(Field.PRESENCES));
                    result.put(Field.INSTALLED, configInstalled);
                    if (configInstalled){
                        Future<JsonObject> activationFuture = Future.future();
                        isStructureActivatePresences(idStructure,event -> formate(activationFuture,event));
                        activationFuture.onSuccess(event -> {
                            result.put(Field.ACTIVATE,!event.isEmpty() && event.getBoolean(Field.ACTIF));
                            handler.handle(new Either.Right<>(result));
                        }).onFailure(event -> handler.handle(new Either.Left<>("[getRetardsAndAbsences-config] "+event.getMessage())));
                    }else{
                        result.put(Field.ACTIVATE, false);
                        handler.handle(new Either.Right<>(result));
                    }
                })
                .onFailure(event -> handler.handle(new Either.Left<>("[getRetardsAndAbsences-config] "+event.getMessage())));;
    }

    private void isStructureActivatePresences(String idStructure, Handler<Either<String, JsonObject>> eitherHandler){
        JsonArray params = new JsonArray().add(idStructure);

        String query = " SELECT * " +
                " FROM presences.etablissements_actifs " +
                " WHERE id_etablissement = ? ";
        Sql.getInstance().prepared(query, params, Competences.DELIVERY_OPTIONS, validUniqueResultHandler(eitherHandler));

    }
}
