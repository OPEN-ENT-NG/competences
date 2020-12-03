package fr.openent.competences.service.impl;

import com.sun.org.apache.xpath.internal.operations.Bool;
import fr.openent.competences.Competences;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.service.AppreciationSubjectPeriodService;
import fr.openent.competences.service.UtilsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;

import static fr.openent.competences.Utils.isNull;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.sql.SqlResult.validUniqueResultHandler;

/**
 *
 */
public class DefaultAppreciationSubjectPeriod extends SqlCrudService implements AppreciationSubjectPeriodService {
    final String tableRelation;
    final String PERSONNEL = "Personnel";

    protected static final Logger log = LoggerFactory.getLogger(DefaultAppreciationSubjectPeriod.class);
    private UtilsService utilsService;


    public DefaultAppreciationSubjectPeriod(String schema, String table, String tableRelation, EventBus eb) {
        super(schema, table);
        this.tableRelation = tableRelation;
        utilsService = new DefaultUtilsService(eb);
    }

    public void updateOrInsertAppreciationSubjectPeriod(JsonArray valuesGetIdAppreciationSubjectPeriod,
                                                        UserInfos userInfos,
                                                        String appreciation,
                                                        String idStructure,
                                                        Handler<Either<String, JsonObject>> handler) {
        String queryGetIdAppreciationSubjectPeriod = "" +
                "SELECT id FROM " + this.resourceTable + " " +
                "WHERE id_periode = ? " +
                "AND id_eleve = ? " +
                "AND id_classe = ? " +
                "AND id_matiere = ? ;";
        Sql.getInstance().prepared(queryGetIdAppreciationSubjectPeriod,
                valuesGetIdAppreciationSubjectPeriod,
                validUniqueResultHandler(useIdForChooseMethod(valuesGetIdAppreciationSubjectPeriod, userInfos, appreciation, idStructure, handler)));
    }

    /**
     * @param valuesGetIdAppreciationSubjectPeriod [appreciation, idPeriod, idStudent, idClassSchool, idSubject , idUser]
     * @param userInfos is value of user
     * @param appreciation is value appreciation
     * @param handler
     * @return Handler
     */
    private Handler<Either<String, JsonObject>> useIdForChooseMethod(JsonArray valuesGetIdAppreciationSubjectPeriod,
                                                                     UserInfos userInfos,
                                                                     String appreciation,
                                                                     String idStructure,
                                                                     Handler<Either<String, JsonObject>> handler) {
        return responseId -> {
            if (responseId.isLeft()) {
                handler.handle(new Either.Left<>("Error in getResponseId: " + responseId.left().getValue()));
            } else {
                isVisibleForAppreciations(userInfos, valuesGetIdAppreciationSubjectPeriod.getString(2),
                        valuesGetIdAppreciationSubjectPeriod.getString(3), idStructure, isVisible -> {
                    JsonArray values = new JsonArray()
                            .add(appreciation)
                            .addAll(valuesGetIdAppreciationSubjectPeriod);

                    if(isVisible) values.add(userInfos.getUserId());

                    if (isNull(responseId.right().getValue().getInteger("id"))) {
                        insertAppreciationSubjectPeriod(values, isVisible, handler);
                    } else {
                        updateAppreciationSubjectPeriod(values, isVisible, handler);
                    }
                });

            }
        };
    }

    /**
     * @param userInfos use for get right
     * @return boolean use for the visibility on appreciation view
     */
    private void isVisibleForAppreciations(UserInfos userInfos, String idClasse, String idMatiere, String idEtablissement, Handler<Boolean> handler) {
        FilterUserUtils.validateHeadTeacherWithClasses(userInfos, new JsonArray().add(idClasse), response -> {
            if(response.isRight()){
                Boolean isHeadTeacher = response.right().getValue();
                if(isHeadTeacher) {
                    utilsService.getServices(idEtablissement, new JsonArray().add(idClasse), event -> {
                        if(event.isRight()){
                            Boolean isHisSubject = false;
                            JsonArray services = event.right().getValue();
                            JsonObject service = null;
                            for(Object s : services) {
                                JsonObject serviceJson = (JsonObject) s;
                                if(serviceJson.getString("id_matiere").equals(idMatiere)){
                                    service = serviceJson;
                                    break;
                                }
                            }
                            if(service != null) {
                                isHisSubject = (service.getString("id_enseignant").equals(userInfos.getUserId()));
                            }
                            handler.handle(isHisSubject && !(userInfos.getType().contains(PERSONNEL)));
                        }
                        else{
                            handler.handle(false);
                            log.error("Error : isVisibleForAppreciations");
                        }
                    });
                }
                handler.handle(!isHeadTeacher && !(userInfos.getType().contains(PERSONNEL)));
            } else {
                handler.handle(false);
                log.error("Error : isVisibleForAppreciations");
            }
        });
    }

    /**
     * @param values    =[appreciation, idPeriod, idStudent, idClassSchool, idSubject , idUser]
     * @param isVisible for add id'user into BDD
     * @param handler
     */
    private void insertAppreciationSubjectPeriod(JsonArray values, Boolean isVisible, Handler<Either<String, JsonObject>> handler) {
        String addRelationHeaderQuery = "WITH insert_appreciation_matiere_periode AS ( ";
        String queryForInsert = "" +
                "INSERT INTO " + this.resourceTable + " " +
                "   (appreciation_matiere_periode, id_periode, id_eleve, id_classe, id_matiere) " +
                "VALUES ( ? , ? , ? , ? , ? ) " +
                "RETURNING id ";
        String addRelationFooterQuery = "" +
                ") " +
                "INSERT INTO " + this.schema + this.tableRelation + " " +
                "   (appreciation_matiere_periode_id, user_id_neo, creation_date) " +
                "VALUES ((SELECT id FROM insert_appreciation_matiere_periode LIMIT 1) , ? , NOW()) " +
                "RETURNING appreciation_matiere_periode_id, id;";
        String finalQuery = isVisible
                ? (addRelationHeaderQuery + queryForInsert + addRelationFooterQuery)
                : queryForInsert;

        Sql.getInstance().prepared(finalQuery, values, validUniqueResultHandler(handler));
    }

    /**
     * @param values [appreciation, idPeriod, idStudent, idClassSchool, idSubject , idUser]
     * @param isVisible for add id'user into BDD
     * @param handler
     */
    private void updateAppreciationSubjectPeriod(JsonArray values, Boolean isVisible, Handler<Either<String, JsonObject>> handler) {
        String addRelationHeaderQuery = "WITH update_appreciation_matiere_periode AS ( ";
        String queryForUpdate = "" +
                "UPDATE " + this.resourceTable + " AS amp " +
                "SET appreciation_matiere_periode = ? " +
                "WHERE id_periode = ? " +
                "   AND id_eleve = ? " +
                "   AND id_classe = ? " +
                "   AND id_matiere = ? " +
                "RETURNING id ";
        String addRelationFooterQuery = "" +
                ") " +
                "INSERT INTO " + this.schema + this.tableRelation + " " +
                "   (appreciation_matiere_periode_id, user_id_neo, update_date) " +
                "VALUES ( (SELECT id FROM update_appreciation_matiere_periode LIMIT 1), ? , NOW()) " +
                "ON CONFLICT (user_id_neo, appreciation_matiere_periode_id) " +
                "DO UPDATE SET update_date = NOW() " +
                "RETURNING appreciation_matiere_periode_id, id;";

        String finalQuery = isVisible
                ? (addRelationHeaderQuery + queryForUpdate + addRelationFooterQuery)
                : queryForUpdate;

        Sql.getInstance().prepared(finalQuery, values, validUniqueResultHandler(handler));
    }
}
