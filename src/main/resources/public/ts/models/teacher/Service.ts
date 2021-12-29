import {Model, moment, notify, http} from 'entcore';
//import http from 'axios';
import * as utils from '../../utils/teacher';

export class Service extends Model{
    id: number;
    id_etablissement: string;
    id_enseignant: string;
    id_matiere: string;
    id_groupe: string;
    modalite: string;
    evaluable: boolean;
    ordre: string;
    coefficient: number;

    matieres: any[];
    services: any[];

    constructor(o?: any) {
        super();
        if (o) this.updateData(o, false);
    }

    get api() {
        return {
            GET_MATIERES: '/viescolaire/matieres?idEtablissement=',
            GET_SERVICES: '/viescolaire/services?idEtablissement=',
        };
    }

    async syncMatieres(){
        try{
            this.matieres = await http().getJson(this.api.GET_MATIERES)
        }catch (e){
            console.log(e);
        }
    }

    async syncServices(){
        try{
            this.services = await http().getJson(this.api.GET_SERVICES);
        }catch (e){
            console.log(e);
        }
    }

}



