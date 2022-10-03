package fr.openent.competences.security.modelbulletinrights;

import com.mongodb.QueryBuilder;
import fr.openent.competences.constants.Field;
import fr.openent.competences.enums.report_model_print_export.ReportModelPrintExportMongo;
import fr.openent.competences.model.ReportModelPrintExport;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.ReportModelPrintExportService;
import fr.openent.competences.service.impl.DefaultReportModelPrintExportService;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.MongoAppFilter;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.user.UserInfos;


import static fr.openent.competences.security.utils.WorkflowActionUtils.hasRight;

    public class UserIdModelExportBulletin implements ResourcesProvider {
    private ReportModelPrintExportService reportModelService;
    private MongoDbConf conf = MongoDbConf.getInstance();

    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        // Verify user right export.bulletins.periodique
        if(!hasRight(user, WorkflowActions.ACCESS_MODEL_BULLETIN.toString())){
            handler.handle(false);
            return;
        }
        // Verify if userId equals id user mongo
        String idReportModel = request.getParam(Field.IDREPORTMODEL);
        QueryBuilder query = QueryBuilder.start("_id").is(idReportModel).and("userId").is(user.getUserId());
        MongoAppFilter.executeCountQuery(request, ReportModelPrintExportMongo.COLLECTION_REPORT_MODEL.getString(), MongoQueryBuilder.build(query), 1, event -> {
            if (Boolean.TRUE.equals(event)) {
                handler.handle(true);
            } else {
                handler.handle(false);
            }
        });
    }
}
