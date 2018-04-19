
import {notify} from 'entcore';
import http from 'axios';


export class DispenseDomaine {
    id_domaine : number;
    id_eleve : string;
    dispense : boolean;

    constructor(id_domaine : number,id_eleve: string,dispense_eleve:boolean){
        this. id_domaine = id_domaine;
        this.id_eleve = id_eleve;
        this.dispense = dispense_eleve;
    }

    toJson(){
        return {
            id_eleve: this.id_eleve,
            id_domaine: this.id_domaine,
            dispense: this.dispense
        };
    }

    async save(){
        if(this.dispense){
            await this.create();
        }else{
            await this.delete();
        }
    }
   async create(){
        try{
            await http.post(`/competences/domaine/dispense/eleve`, this.toJson());
        }catch(e){
            notify.error('evaluations.dispense.domaine.eleve.create.err');
            this.dispense = false;
        }
   }
    async delete(){
        try{
            await http.delete(`/competences/domaine/dispense/eleve/${this.id_domaine}/${this.id_eleve}`);
        }catch(e){
            notify.error('evaluations.dispense.domaine.eleve.delete.err');
            this.dispense = true;
        }
    }

}
