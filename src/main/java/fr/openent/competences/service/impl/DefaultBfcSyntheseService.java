package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.BfcSyntheseService;
import fr.openent.competences.service.UtilsService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

/**
 * Created by agnes.lapeyronnie on 03/11/2017.
 */
public class DefaultBfcSyntheseService extends SqlCrudService implements BfcSyntheseService {

    private UtilsService utilsService ;
    private EventBus eb;
    private static final Logger log = LoggerFactory.getLogger(DefaultBfcSyntheseService.class);

    public DefaultBfcSyntheseService(String schema, String table, EventBus eb) {
        super(schema, table);
        this.eb = eb;
        utilsService = new DefaultUtilsService();
    }

    @Override
    public void createBfcSynthese(JsonObject synthese, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        super.create(synthese,user,handler);
    }

    @Override
    public void updateBfcSynthese(String id, JsonObject synthese, Handler<Either<String, JsonObject>> handler) {
        super.update(id, synthese, handler);
    }

    @Override
    public void deleteBfcSynthese(String id, Handler<Either<String, JsonObject>> handler) {
        super.delete(id, handler);
    }

    @Override
    public void getBfcSyntheseByEleve(String idEleve,Integer idCycle, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder()
                .append("SELECT * FROM "+ Competences.COMPETENCES_SCHEMA +".bfc_synthese WHERE id_eleve = ? AND id_cycle = ? ;");

        Sql.getInstance().prepared(query.toString(),new fr.wseduc.webutils.collections.JsonArray().add(idEleve).add(idCycle), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getBfcSyntheseByIdsEleve(final String[] idsEleve, final Long idCycle,final Handler<Either<String, JsonArray>> handler) {
        JsonArray valuesCount = new fr.wseduc.webutils.collections.JsonArray();
        String queryCount = "SELECT count(*) FROM "+ Competences.COMPETENCES_SCHEMA +".bfc_synthese WHERE id_eleve IN "+ Sql.listPrepared(idsEleve)+"  AND id_Cycle = ? ";

        for(String idEleve:idsEleve){
            valuesCount.add(idEleve);
        }
        valuesCount.add(idCycle);
        Sql.getInstance().prepared(queryCount, valuesCount, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> sqlResultCount) {
                Long nbSyntheseBFC = SqlResult.countResult(sqlResultCount);
                if (nbSyntheseBFC > 0) {
                    JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
                    String query = "SELECT * FROM "+ Competences.COMPETENCES_SCHEMA +".bfc_synthese WHERE id_eleve IN "+ Sql.listPrepared(idsEleve)+"  AND id_Cycle = ? ";

                    for(String s:idsEleve){
                        values.add(s);
                    }
                    values.add(idCycle);

                    Sql.getInstance().prepared(query,values, SqlResult.validResultHandler(handler));

                }else{
                    handler.handle(new Either.Right<String,JsonArray>(new fr.wseduc.webutils.collections.JsonArray()));
                }
            }
        });
    }

    @Override
    public void getBfcSyntheseByIdsEleveAndClasse(final String[] idsEleve, final String idClasse,final Handler<Either<String, JsonArray>> handler) {
        JsonArray valuesCount = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT bfc_synthese.*, rel_groupe_cycle.id_groupe " +
                "FROM " + Competences.COMPETENCES_SCHEMA + ".bfc_synthese " +
                "INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".rel_groupe_cycle ON rel_groupe_cycle.id_cycle = bfc_synthese.id_cycle " +
                "WHERE bfc_synthese.id_eleve IN "+ Sql.listPrepared(idsEleve)+"  " +
                "AND rel_groupe_cycle.id_groupe = ? ";

        for(String s:idsEleve){
            values.add(s);
        }
        values.add(idClasse);

        Sql.getInstance().prepared(query,values, SqlResult.validResultHandler(handler));
    }


    // A partir d'un idEleve retourne idCycle dans lequel il est.(avec les requêtes getClasseByEleve de ClasseService et getCycle de UtilsService)
    @Override
    public void getIdCycleWithIdEleve(String idEleve,final Handler<Either<String,Integer>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "classe.getClasseByEleve")
                .put("idEleve", idEleve);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    utilsService.getCycle(body.getJsonObject("result").getJsonObject("c").getJsonObject("data").getString("id"), new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> idCycleObject) {
                            if (idCycleObject.isRight()) {
                                Integer idCycle = idCycleObject.right().getValue().getInteger("id_cycle");
                                handler.handle(new Either.Right<String,Integer>(idCycle));
                            } else {
                                log.error("idCycle not found" + idCycleObject.left().getValue());
                                handler.handle(new Either.Left<String,Integer>("idCycle not found : " + idCycleObject.left().getValue()));
                            }
                        }
                    });
                } else {
                    log.error("Class not found" + body.getString("message"));
                    handler.handle(new Either.Left<String,Integer>("idClass not found : " + body.getString("message")));
                }
            }
        }));
    }

}
