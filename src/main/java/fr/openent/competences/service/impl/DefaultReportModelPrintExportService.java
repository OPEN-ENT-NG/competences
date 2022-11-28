package fr.openent.competences.service.impl;

import com.mongodb.QueryBuilder;
import fr.openent.competences.enums.report_model_print_export.ReportModelPrintExportMongo;
import fr.openent.competences.model.ReportModelPrintExport;
import fr.openent.competences.service.ReportModelPrintExportService;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.MongoDbCrudService;

import static org.entcore.common.mongodb.MongoDbResult.validActionResultHandler;

public class DefaultReportModelPrintExportService extends MongoDbCrudService implements ReportModelPrintExportService {
    public DefaultReportModelPrintExportService(String collection) {
        super(collection);
    }

    public void addReportModel(ReportModelPrintExport reportModelPrintExport, Handler<Either<String, JsonObject>> handler) {
        JsonObject jsonReportModel = reportModelPrintExport.toJsonObject();
        mongo.insert(this.collection, jsonReportModel, validActionResultHandler(handler));
    }

    public void getReportModel(ReportModelPrintExport reportModelPrintExport, Handler<Either<String, JsonObject>> handler) {
        JsonObject matcher = new JsonObject();
        if(reportModelPrintExport.getId() != null){
            matcher.put(ReportModelPrintExportMongo.KEY_ID.getString(), reportModelPrintExport.getId());
        }
        if(reportModelPrintExport.getUserId() != null){
            matcher.put(ReportModelPrintExportMongo.KEY_USER_ID.getString(), reportModelPrintExport.getUserId());
        }
        if(reportModelPrintExport.getStructureId() != null){
            matcher.put(ReportModelPrintExportMongo.KEY_STRUCTUREID.getString(), reportModelPrintExport.getStructureId());
        }
        mongo.find(collection, matcher, validActionResultHandler(handler));
    }

    public void putReportModel(ReportModelPrintExport reportModelPrintExport, Handler<Either<String, JsonObject>> handler){
        JsonObject jsonReportModel = reportModelPrintExport.toJsonObject();
        QueryBuilder matcher = QueryBuilder.start(ReportModelPrintExportMongo.KEY_ID.getString())
                .is(reportModelPrintExport.getId());

        MongoUpdateBuilder update = new MongoUpdateBuilder();
        jsonReportModel.remove(ReportModelPrintExportMongo.KEY_ID.getString());
        jsonReportModel.remove(ReportModelPrintExportMongo.KEY_USER_ID.getString());

        for (String attr : jsonReportModel.fieldNames()) {
            update.set(attr, jsonReportModel.getValue(attr));
        }
        mongo.update(this.collection, MongoQueryBuilder.build(matcher), update.build(),  validActionResultHandler(handler));
    }

    public void deleteReportModel(ReportModelPrintExport reportModelPrintExport, Handler<Either<String, JsonObject>> handler) {
        JsonObject matcher = new JsonObject()
                .put(ReportModelPrintExportMongo.KEY_ID.getString(), reportModelPrintExport.getId());
        mongo.delete(collection, matcher, validActionResultHandler(handler));
    }
}