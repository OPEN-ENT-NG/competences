import {ReportModelPrintExport} from "../../models/teacher/ReportModelPrintExport";

export interface ReportModelPrintExportServiceType {
    getAll: (structureId: String) => Promise<Array<ReportModelPrintExport>>;
    getModelSelected: (id : string) => Promise<ReportModelPrintExport>;
}