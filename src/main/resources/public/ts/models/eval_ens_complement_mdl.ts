/**
 * Created by agnes.lapeyronnie on 28/11/2017.
 */
import {IModel, Model, notify, _} from "entcore";
import http from "axios";
import {Mix} from "entcore-toolkit";
export class EnsCpl extends Model  {
    id : number;
    libelle : string;
    code : string;

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


export class NiveauEnseignementCpl extends Model  {
    libelle : string;
    niveau : number;

    constructor(niveau : number){
        super();
        this.niveau = niveau
    }
}

export class NiveauEnseignementCpls extends Model {
    all : NiveauEnseignementCpl[];

    constructor(){
        super();
        this.all=[];
        this.all[0] = new NiveauEnseignementCpl(1);
        this.all[0].libelle = "Objectif atteint";
        this.all[1] = new NiveauEnseignementCpl(2);
        this.all[1].libelle ="Objectif dépassé";
    }
}

export class EleveEnseignementCpl extends Model implements IModel{
    id : number;
    id_eleve : string;
    id_enscpl : number;
    id_langue : number;
    niveau : number;
    libelle : string;
    niveau_lcr : number;
    libelle_lcr : string;

    constructor(id_eleve : string){
        super();
        this.id_eleve = id_eleve;
        this.niveau = 0;
    }
     setAttributsEleveEnsCpl (id_enscpl : number,niveau : number,niveau_lcr : number, id_langue : number)  {
            this.id_enscpl = id_enscpl;
            this.niveau = niveau;
            this.id_langue = id_langue;
            this.niveau_lcr = niveau_lcr;
            return this;
    }

    get api(){
        return {
            create :`/competences/CreateNiveauEnsCpl`,
            update : `/competences/UpdateNiveauEnsCpl?id=${this.id}`,
            get :`/competences/GetNiveauEnsCpl?idEleve=${this.id_eleve}`
        }
    }
    toJSON(){
        return{
            id_eleve : this.id_eleve,
            id_enscpl : this.id_enscpl,
            id_langue : this.id_langue,
            niveau : this.niveau,
            niveau_lcr : this.niveau_lcr
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
        if(data !== undefined && data.length === 1) {
            let enseignementComplementEleve = data[0] ;
            if (enseignementComplementEleve.hasOwnProperty('id')) {
                let nivEnsCpls = new NiveauEnseignementCpls();
                if (enseignementComplementEleve.niveau != 0) {
                    enseignementComplementEleve.libelle = _.findWhere(nivEnsCpls.all, {niveau: enseignementComplementEleve.niveau}).libelle;
                }
                this.updateData(enseignementComplementEleve);
            }
        }
    }
   /* save () {
        this.create().then((id) => {

        }).catch((e) => {

        });
    }*/


}