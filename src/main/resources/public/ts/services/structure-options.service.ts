import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {http as HTTP} from "entcore";

export interface StructureOptions {
    isSkillAverage ?: boolean;
    structureId : string;
}
export interface IStructureOptionsService {

    getStructureOptionsIsAverageSkills(structureId: String): Promise<StructureOptions>;

    saveStrustureOptionsIsAverageSkills(options: StructureOptions): Promise<AxiosResponse>;

    initRecuperationAbsencesRetardsFromPresences(paramImportCSV: any): Promise<any[]>;

    changeAbsencesRetardsFromPresences(checked:boolean,id_structure: string): Promise<any[]>;
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

    saveStrustureOptionsIsAverageSkills: async(options: StructureOptions): Promise<AxiosResponse> =>{
        return http.post(`competences/structure/options/isSkillAverage`, options);
    },

    /**
     * Récupère les structures de l'utilisateur qui ont activées la récupération des absences/retards du module presences.
     * @returns {Promise<T>} Callback de retour.
     */
    initRecuperationAbsencesRetardsFromPresences(paramImportCSV: any): Promise<any[]> {
        return new Promise((resolve, reject) => {
            HTTP().getJson('/competences/init/sync/presences?structureId='+paramImportCSV.that.structure.id)
                .done((state) => {
                    if(state.presences_sync && state.installed && state.activate)
                        paramImportCSV.that.absencesRetardsFromPresences = true;
                    paramImportCSV.that.checkBox.hidden = !state.installed || !state.activate;
                    resolve(state);
                })
                .error(() => {
                    reject();
                });
        });
    },

    /**
     * Active ou désactive la récupération des absences/retards du module presences d'une structure de l'utilisateur.
     * @param checked
     * @param id_structure
     * @returns {Promise<T>} Callback de retour.
     */
    changeAbsencesRetardsFromPresences(checked:boolean, id_structure: string): Promise<any[]> {
        return new Promise((resolve, reject) => {
            HTTP().postJson('/competences/sync/presences', {state: checked, structureId: id_structure})
                .done((res) => {
                    resolve(res);
                })
                .error(() => {
                    reject();
                });
        });
    }
};

export const StructureOptionsService = ng.service('structureOptionsService', (): IStructureOptionsService => structureOptionsService)