import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';

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
            const {data}: AxiosResponse = await http.get(`/competences/exports/logs`);
            return data;
        } catch (err) {
            throw err;
        }
    },

}
export const ParameterService = ng.service('ParameterService', (): ParameterService => parameterService);