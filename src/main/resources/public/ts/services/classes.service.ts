import http, {AxiosResponse} from 'axios';
import {ng} from "entcore";

export interface IClassesService {
    /**
     * Retrieve event summary graph based on given data
     *
     * @param structure structure identifier
     * @param student student identifier
     */
    getClassesAndGroup(structure: string): Promise<Array<Object>>;
}

export const ClassesService: IClassesService = {
    async getClassesAndGroup(structure: string): Promise<Array<Object>> {
        return http.get(`/competences/classe/groupes?idStructure=${structure}`)
            .then((res: AxiosResponse) => res.data as Array<Object>)
    }
}

export const classesService = ng.service('ClassesService', (): IClassesService => ClassesService);