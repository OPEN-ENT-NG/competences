import { notify,Model }from 'entcore';
import http from 'axios';
import {Mix} from 'entcore-toolkit';

export class BaremeBrevetEleve {


    id_eleve: string;
    controlesContinus_brevet: number;
    totalMaxBaremeBrevet: number;

    constructor() {

    }

}

    export class BaremeBrevetEleves {
    all: BaremeBrevetEleve[];

    constructor()   {
        this.all = [];
    }

    async sync(id_classe: string,idTypePeriode: number){

        try{
            let { data } = await http.get(`/competences/bfc/bareme/brevet/eleves?idClasse=${id_classe}&idTypePeriode=${idTypePeriode}`);
            this.all = Mix.castArrayAs(BaremeBrevetEleve,data);
        }catch (e){
            notify.error('evaluation.bfc.controle.continu.eleves.err');
        }
    }

}