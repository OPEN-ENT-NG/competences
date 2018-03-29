import {IModel, Model} from "entcore";
import http from "axios";
import {Mix} from "entcore-toolkit";
export class LangueCultReg extends Model  {
    id : number;
    libelle : string;
    code : string;

    constructor(){
        super();
    }
}

export class LanguesCultRegs extends Model implements IModel{
    all : LangueCultReg[];

    constructor(){
        super();
        this.all=[];
    }
    get api() {
        return {
            get: `/competences/langues/culture/regionale/list`
        }
    }

    async sync() {
        let { data } = await http.get(this.api.get);
        this.all = Mix.castArrayAs(LangueCultReg, data);
    }
}

export class NiveauLangueCultReg extends Model  {
    libelle : string;
    niveau : number;

    constructor(niveau : number){
        super();
        this.niveau = niveau
    }
}

export class NiveauLangueCultRegs extends Model {
    all : NiveauLangueCultReg[];

    constructor(){
        super();
        this.all=[];
        this.all[0] = new NiveauLangueCultReg(1);
        this.all[0].libelle = "Objectif atteint";
        this.all[1] = new NiveauLangueCultReg(2);
        this.all[1].libelle ="Objectif dépassé";
    }
}

