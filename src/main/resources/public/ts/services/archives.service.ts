import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {ArchiveBFC} from "../models/common/ArchiveBFC";
import {ArchiveBulletin} from "../models/common/ArchiveBulletin";
import {Period} from "../models/common/Periode";


export interface IArchivesService {
    getArchivesBFC(idStructure: String): Promise<Array<ArchiveBFC>>;
    getArchivesBulletins(idStructure: String): Promise<Array<ArchiveBulletin>>;
}

export const archivesService: IArchivesService = {
    getArchivesBFC: async (idStructure: String): Promise<Array<ArchiveBFC>> => {
        const {data} = await http.get(`/competences/archive-bfc?idEtablissement=${idStructure}`);
        return data as Array<ArchiveBFC>;
    },

    getArchivesBulletins: async (idStructure: String): Promise<Array<ArchiveBulletin>> => {
        const {data} = await http.get(`/competences/archive-bulletin?idEtablissement=${idStructure}`);
        return data as Array<ArchiveBulletin>;
    }
};

export const ArchivesService = ng.service('ArchivesService', (): IArchivesService => archivesService);