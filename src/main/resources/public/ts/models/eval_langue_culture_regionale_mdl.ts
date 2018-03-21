import { model, IModel, Model, notify, _ }from 'entcore';
import http from 'axios';
import{Mix} from "entcore-toolkit";
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

