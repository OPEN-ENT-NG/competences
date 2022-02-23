import {Model} from 'entcore';

export interface IArchiveBulletin {

    id_classe?: string;
    id_eleve?: string;
    id_etablissement?: string;
    external_id_classe?: string;
    id_cycle?: string;
    id_file?: string;
    created?: Date;
    file_name?: string;
    id_annee?: string;
    modified?: Date;
}

export class ArchiveBulletin extends Model {
    id_classe: string;
    id_eleve: string;
    id_etablissement: string;
    external_id_classe: string;
    id_cycle: string;
    id_file: string;
    created: Date;
    file_name: string;
    id_annee: string;
    modified: Date;

    toString = function () {
        return this.file_name;
    }
}