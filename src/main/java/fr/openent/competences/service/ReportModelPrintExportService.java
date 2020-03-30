package fr.openent.competences.service;

import fr.openent.competences.model.ReportModelPrintExport;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/*
    Link into confluance doc: https://entconf.gdapublic.fr/display/MN/Report+model+print+export
 */

public interface ReportModelPrintExportService {

    /**
     * Created report model in mongoDB
     *
     * @param reportModelPrintExport { ReportModelPrintExport } data of report model
     * @param handler { Handler<Either<String, JsonObject>> } Response of mongoDB =>
     * {
     *     "number": 0,
     *     "_id": "8c49c690-ffbf-42d0-80ca-c5d168ff2cc2",
     * }
     */
    void addReportModel(ReportModelPrintExport reportModelPrintExport, Handler<Either<String, JsonObject>> handler);

    /**
     * Get reports models in mongoDB
     *
     * @param reportModelPrintExport { ReportModelPrintExport } user id
     * @param handler { Handler<Either<String, JsonArray>> } Response of mongoDB =>
     * {
     *      "results":
     *         [
     *                {
     *                   "_id": "b61898c3-d5c9-4e82-87e1-5b1781fc0418",
     *                   "userId": "43512bf6-8f0d-4a78-ae3d-a32f8e48a1d8",
     *                   "title": "no seleted",
     *                   "selected": false,
     *                   "preferences": {
     *                   "moyenneClasse": false
     *                   }
     *               }, ...
     *           ],
     *      "number": 4
     * }
     */
    void getReportModel(ReportModelPrintExport reportModelPrintExport, Handler<Either<String, JsonObject>> handler);

    /**
     * Update report model in mongoDB
     *
     * @param reportModelPrintExport { ReportModelPrintExport } data of report model
     * @param handler { Handler<Either<String, JsonObject>> } Response of mongoDB =>
     * {
     *     "number": 1,
     * }
     */
    void putReportModel(ReportModelPrintExport reportModelPrintExport, Handler<Either<String, JsonObject>> handler);


    /**
     * Get reports models selected in mongoDB
     *
     * @param reportModelPrintExport { ReportModelPrintExport } user id
     * @param handler { Handler<Either<String, JsonObject>> } Response of mongoDB =>
     * {
     *      "results":
     *         [
     *                {
     *                   "_id": "b61898c3-d5c9-4e82-87e1-5b1781fc0418",
     *                   "userId": "43512bf6-8f0d-4a78-ae3d-a32f8e48a1d8",
     *                   "title": "no seleted",
     *                   "selected": true,
     *                   "preferences": {
     *                   "moyenneClasse": false
     *                   }
     *               }, ...
     *           ],
     *      "number": 4
     * }
     */
    void getReportModelSelected(ReportModelPrintExport reportModelPrintExport, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete report model in mongoDB
     *
     * @param reportModelPrintExport { ReportModelPrintExport } data of report model
     * @param handler { Handler<Either<String, JsonObject>> } Response of mongoDB =>
     * {
     *     "number": 1,
     * }
     */
    void deleteReportModel(ReportModelPrintExport reportModelPrintExport, Handler<Either<String, JsonObject>> handler);
}
