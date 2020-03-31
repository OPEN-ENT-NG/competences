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
    URL_API_GET_SELECTED,
} = ReportModelPrintExportConstant;

export const reportModelPrintExportService: ReportModelPrintExportServiceType = {
    getAll: async ():Promise<Array<ReportModelPrintExport>> => {
        try {
            const response = await http.get(URL_API_GET_ALL);
            const dirtyData:Array<ReportModelPrintExport> = controlDataAndGetResult(response);
            if(!dirtyData) return [];
            return preparedReportModels(dirtyData);
        } catch (error) {
            notify.error('competences.report-model.api.error.getAll');
            return [];
        }
    },
    getAllSelected: async ():Promise<Array<ReportModelPrintExport>> => {
        try {
            const response = await http.get(URL_API_GET_SELECTED);
            const dirtyData:Array<ReportModelPrintExport> = controlDataAndGetResult(response);
            if(!dirtyData) return [];
            return preparedReportModels(dirtyData);
        } catch (error) {
            notify.error('competences.report-model.api.error.getSelected');
        }
    },
     getFirstSelected: async():Promise<ReportModelPrintExport> => {
         try {
        const reportsModels =  await reportModelPrintExportService.getAllSelected();
        if(reportsModels.length > 0){
            return reportsModels[0];
        }
        return new ReportModelPrintExport(undefined);
         } catch (error) {
             notify.error('competences.report-model.api.error.getFirstSelected');
             return new ReportModelPrintExport(undefined);
         }
    },
};

function preparedReportModels(reportModels:Array<ReportModelPrintExport>):Array<ReportModelPrintExport>{
    const cleanData:Array<ReportModelPrintExport> = [];
    reportModels.forEach((reportModel:ReportModelPrintExport) => {
        cleanData.push(new ReportModelPrintExport(reportModel))
    });
    return cleanData;
}

export const ReportModelPrintExportService = ng.service('ReportModelPrintExportService', (): ReportModelPrintExportServiceType => reportModelPrintExportService);
