import {ReportModelPrintExport} from "../../models/teacher/ReportModelPrintExport";

export interface ReportModelPrintExportServiceType {
    getAll: (structureId: String) => Promise<Array<ReportModelPrintExport>>;
    getAllSelected: () => Promise<Array<ReportModelPrintExport>>;
    getFirstSelected: () => Promise<ReportModelPrintExport>;
}