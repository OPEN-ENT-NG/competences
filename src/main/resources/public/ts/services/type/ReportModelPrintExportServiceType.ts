import {ReportModelPrintExport} from "../../models/teacher/ReportModelPrintExport";

export interface ReportModelPrintExportServiceType {
    getAll: () => Promise<Array<ReportModelPrintExport>>;
    getAllSelected: () => Promise<Array<ReportModelPrintExport>>;
    getFirstSelected: () => Promise<ReportModelPrintExport>;
}