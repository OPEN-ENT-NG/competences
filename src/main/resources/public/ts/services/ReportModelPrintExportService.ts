import http from "axios";
import {ReportModelPrintExportConstant} from "../constants/";
import {ng, notify} from "entcore";
import {ReportModelPrintExportServiceType} from "./type";
import {MongoDBUtils} from "./utils";
import {ReportModelPrintExport} from "../models/teacher/ReportModelPrintExport";

const {
    controlDataAndGetResult,
} = MongoDBUtils;

const {
    URL_API_GET_ALL,
    URL_API_GET_ONE
} = ReportModelPrintExportConstant;

export const reportModelPrintExportService: ReportModelPrintExportServiceType = {
    getAll: async (structureId: String):Promise<Array<ReportModelPrintExport>> => {
        try {
            const response = await http.get(`${URL_API_GET_ALL}${structureId}`);
            const dirtyData:Array<ReportModelPrintExport> = controlDataAndGetResult(response);
            if(!dirtyData) return [];
            return preparedReportModels(dirtyData);
        } catch (error) {
            notify.error('competences.report-model.api.error.getAll');
            return [];
        }
    },

    getModelSelected: async(id : string): Promise<ReportModelPrintExport> => {
        try {
            const { status, data } = await http.get(`${URL_API_GET_ONE}${id}`);
            const reportModel = data.results[0];
            if(status === 200){
                return new ReportModelPrintExport(reportModel);
            }
            return undefined;
        } catch (error) {
            notify.error('competences.report-model.api.error.getSelected');
        }

    }
};

function preparedReportModels(reportModels:Array<ReportModelPrintExport>):Array<ReportModelPrintExport>{
    const cleanData:Array<ReportModelPrintExport> = [];
    reportModels.forEach((reportModel:ReportModelPrintExport) => {
        cleanData.push(new ReportModelPrintExport(reportModel))
    });
    return cleanData;
}

export const ReportModelPrintExportService = ng.service('ReportModelPrintExportService', (): ReportModelPrintExportServiceType => reportModelPrintExportService);
