import {DefaultEleve} from "../common/DefaultEleve";
import {Classe} from "./Classe";
import http from "axios";
import {notify} from "entcore";

export class Eleve extends DefaultEleve {
    classe: Classe;
    idStructure: string;
    id_cycle: string;
    cycles : any;

    constructor(o?: any) {
        super();
        if (o) this.updateData(o);
    }

    get api() {
        return {
            GET_CYCLES : `/competences/cycles/eleve/`
        }
    }

    async getCycles () {
        try {
            let {data} = await http.get(this.api.GET_CYCLES + this.id);
            if (!data.error) {
                this.cycles = data;
            }
        } catch (e) {
            notify.error('evaluations.eleve.cycle.get.error');
        }

    }
}