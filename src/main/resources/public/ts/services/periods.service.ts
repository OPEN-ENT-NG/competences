import http from 'axios';
import {ng} from "entcore";
import {Period} from "../models/common/Periode";

export interface IPeriodService {
    getPeriods(structure: string, group: Array<string>): Promise<Array<Period>>;
}

export const PeriodsService: IPeriodService = {
    async getPeriods(structure: string, groups: Array<string>): Promise<Array<Period>> {
        try {
            let idGroups: string = '';
            if (groups) {
                groups.forEach((g: string) => {
                    idGroups += `&idGroupe=${g}`;
                });
            }
            const {data} = await http.get(`/viescolaire/periodes?idEtablissement=${structure}${idGroups}`);
            return data as Array<Period>;
        } catch (e) {
            throw e;
        }
    }
};

export const periodeService = ng.service('PeriodService', (): IPeriodService => PeriodsService);