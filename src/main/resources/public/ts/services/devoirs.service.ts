import http, {AxiosResponse} from 'axios';
import {Evaluation, Matiere} from "../models/teacher";
import {ng} from "entcore";
import {Period} from "../models/common/Periode";

export interface IDevoirsService {
    /**
     * Retrieve event summary graph based on given data
     *
     * @param structure structure identifier
     * @param student student identifier
     */
    getLatestNotes(structure: string, student: string): Promise<Array<Evaluation>>;

    /**
     * Retrieve event summary based on given data
     *
     * @param structure structure identifier
     * @param student student identifier
     * @param period
     */
    getDetailsStudentSubject(structure: string, student: string, period: Period): Promise<Array<Matiere>>;
}

export const DevoirsService: IDevoirsService = {
    async getLatestNotes(structure: string, student: string): Promise<Array<Evaluation>> {
        return http.get(`/competences/devoirs/eleve?idEtablissement=${structure}&idEleve=${student}`)
            .then((res: AxiosResponse) => res.data.devoirs as Array<Evaluation>)
    },

    async getDetailsStudentSubject(structure: string, student: string, period: Period): Promise<Array<Matiere>> {
        let periodFilter = "";
        if (period.id_type) {
            periodFilter = `&idPeriode=${period.id_type}`
        }
        return http.get(`/competences/devoirs/notes?idEtablissement=${structure}&idEleve=${student}${periodFilter}`)
            .then((res: AxiosResponse) =>
                Object.keys(res.data).map(key => {
                    const matiere: Matiere = res.data[key] as Matiere;
                    matiere.id = key;
                    return matiere
                }) as Array<Matiere>
            )
    }
}

export const devoirsService = ng.service('DevoirsService', (): IDevoirsService => DevoirsService);