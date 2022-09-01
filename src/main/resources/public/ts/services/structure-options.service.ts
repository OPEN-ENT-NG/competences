import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';

export interface StructureOptions {
    isSkillAverage ?: boolean;
    structureId : string;
}
export interface IStructureOptionsService {

    getStructureOptionsIsAverageSkills(structureId: String): Promise<StructureOptions>;

    saveStrustureOptinsIsAverageSkills(options: StructureOptions): Promise<AxiosResponse>;
}


export const structureOptionsService: IStructureOptionsService = {


    getStructureOptionsIsAverageSkills: async(structureId: string): Promise<StructureOptions> => {
        try {
            const response : AxiosResponse = await http.get(`competences/structure/${structureId}/options/isSkillAverage`);
            return {structureId:  structureId, isSkillAverage: response.data.is_average_skills} ;
        } catch (err) {
            throw err;
        }

    },

    saveStrustureOptinsIsAverageSkills: async(options: StructureOptions): Promise<AxiosResponse> =>{
        return http.post(`competences/structure/options/isSkillAverage`, options);
    }

};

export const StructureOptionsService = ng.service('structureOptionsService', (): IStructureOptionsService => structureOptionsService)