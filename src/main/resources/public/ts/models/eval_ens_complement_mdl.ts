/**
 * Created by agnes.lapeyronnie on 28/11/2017.
 */
import { model, IModel, Model, notify, _ }from 'entcore';
import http from 'axios';
import{Mix} from "entcore-toolkit";
export class EnsCpl extends Model  {
    id : number;
    libelle : string;

    constructor(){
        super();
    }

}


export class EnsCpls extends Model implements IModel{
    all : EnsCpl[];

     constructor(){
         super();
         this.all=[];
     }
    get api() {
        return {
            get: `/competences/ListEnseignementComplement`
        }
    }

    async sync() {
        let { data } = await http.get(this.api.get);
        this.all = Mix.castArrayAs(EnsCpl, data);
    }
}


export class NiveauEnseignementCpls extends Model {
    all : EleveEnseignementCpl[];

    constructor(id_eleve : string,){
        super();
        this.all=[];
        this.all[0] = new EleveEnseignementCpl(id_eleve);
        this.all[0].libelle = "Objectif atteint";
        this.all[0].niveau = 1;
        this.all[1] = new EleveEnseignementCpl(id_eleve);
        this.all[1].libelle ="Objectif dépassé";
        this.all[1].niveau = 2;
    }
}

export class EleveEnseignementCpl extends Model implements IModel{
    id : number;
    id_eleve : string;
    id_enscpl : number;
    niveau : number;
    libelle : string;

    //TODO Delete when infra-front will be fixed
    collection:  (type, mixin?, name?) => void;
    updateData: (o) => void;

    constructor(id_eleve : string){
        super();
        this.id_eleve = id_eleve;
        this.niveau = 0;
    }
     setAttributsEleveEnsCpl (id_enscpl : number,niveau : number)  {
            this.id_enscpl = id_enscpl;
            this.niveau = niveau;
            return this;
    }

    get api(){
        return {
            create :`/competencess/CreateNiveauEnsCpl`,
            update : `/competences/UpdateNiveauEnsCpl?id=${this.id}`,
            get :`/competences/GetNiveauEnsCpl?idEleve=${this.id_eleve}`
        }
    }
    toJSON(){
        return{
            id_eleve : this.id_eleve,
            id_enscpl : this.id_enscpl,
            niveau : this.niveau
        }
    }

    async create (): Promise<number> {
        try {
            let res = await http.post(this.api.create, this.toJSON());
            this.id = res.data.id ;
            return this.id;

        } catch (e) {
            //TODO NOTIFIER
            notify.error('Problème lors de la création');
            console.error('Problème lors de la création');
            throw e;
        }
    }
    async update (): Promise<void> {
        await http.put(this.api.update,this.toJSON());
    }

    async save() {
        if(this.id){
           await this.update();
        }else{
            await this.create();
         }
    }

    async sync(){
        let { data } = await  http.get(this.api.get);
        if(data.hasOwnProperty('id')) {
            let nivEnsCpls = new NiveauEnseignementCpls(data.id_eleve);
            if (data.niveau != 0) {
                data.libelle = _.findWhere(nivEnsCpls.all, {niveau: data.niveau}).libelle;
            }
            this.updateData(data);
        }
    }
   /* save () {
        this.create().then((id) => {

        }).catch((e) => {

        });
    }*/


}