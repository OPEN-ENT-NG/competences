import {ng} from 'entcore'
import { http, HttpResponse } from 'entcore-toolkit';

export interface ExportError {
    uai: string;
    name: string;
    logs: string;
    id: string;
}

export interface ParameterService {
    getExports():Promise<Array<ExportError>>;
}



export const parameterService: ParameterService =  {
    getExports: async (): Promise<Array<ExportError>> => {
        try {
            const {data}: HttpResponse = await http.get(`/competences/admin/exports/logs`);
            return data;
        } catch (err) {
            throw err;
        }
    },

}
export const ParameterService = ng.service('ParameterService', (): ParameterService => parameterService);