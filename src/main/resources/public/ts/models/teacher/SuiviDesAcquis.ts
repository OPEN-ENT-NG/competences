import  http  from "axios";
import {Enseignant, ElementProgramme, TableConversion, Utils, TypePeriode} from "./index";
import * as utils from '../../utils/teacher';
import { Mix } from "entcore-toolkit";
import {notify, _, Collection, Model, http as httpEntcore} from "entcore";
import {Historique} from "../common/Historique";


export class SuiviDesAcquis  {

    id_matiere : string;
    libelleMatiere : string;
    teachers : Enseignant[];
    elementsProgramme : string;
    appreciation : string;
    moyenne_finale : number ;
    positionnement_final : any ;
    moyenneEleve : any;
    moyenneClasse : any;
    positionnement_auto : number;
    //tous positionnements de l'élève pour chaque période pour une matière
    //le calculé
    positionnements_auto : any[];
    //moyennes de l'élève pour une matière pour les 3 périodes de l'année en cours
    moyennesEleve : any[];
    //moyennes de la classe pour une matière pour les 3 périodes de l'année en cours
    moyennesClasse : any[];

    idEleve: string;
    idClasse: string;
    idEtablissement: string;
    idPeriode : number;

    constructor(o?:any ) {

    }



    get api(){
        return{
            POST_DATA_RELEVE_PERIODIQUE: `/competences/bilan/periodique`,
        }
    }

    toJson() {
        return {
            idEleve: this.idEleve,
            idMatiere: this.id_matiere,
            idClasse: this.idClasse,
            idEtablissement: this.idEtablissement,
            idPeriode: this.idPeriode
        };
    }

    async saveAppreciationMatierePeriodeEleve() {
            if(this.appreciation !== undefined){
                let _data = _.extend(this.toJson(),{
                    appreciation_matiere_periode: this.appreciation,
                    colonne: 'appreciation_matiere_periode',
                    delete: this.appreciation === "",
                    isBilanPeriodique: true
                });

                try{
                     return await http.post(this.api.POST_DATA_RELEVE_PERIODIQUE, _data);
                }catch (e){
                    notify.error('evaluations.releve.appreciation.classe.save.error');
                    console.log(e);
                }
            }else{
                notify.error('evaluations.releve.appreciation.classe.max.length');

            }
    }
   async savePositionnementEleve(positionnement) {

       let _data = _.extend(this.toJson(), {
           colonne: 'positionnement',
           positionnement: positionnement,
           delete: this.positionnement_final === "",
           isBilanPeriodique: true
       });
       try {
           if (_data.idPeriode !== null) {
               await http.post(this.api.POST_DATA_RELEVE_PERIODIQUE, _data);
               this.positionnement_final = positionnement;
           }
       }catch(e){
           notify.error('bilan.periodique.suivis.des.acquis.error.save.positionnement');
           console.log(e);
       }
   }

    getPositionnementDefinitif(): any{
       if(this.positionnement_final === this.positionnement_final){
           return this.positionnement_auto;
       }else{
           return this.positionnement_final;
       }
    }
}
export class SuivisDesAcquis extends Model{

    all: SuiviDesAcquis[];
    tableConversions: Collection<TableConversion>;
    moyenneGeneraleElve : string;
    moyenneGeneraleClasse: string;
    idEleve: string;
    idClasse: string;
    idEtablissement: string;
    idPeriode : number;
    historiques: Historique[];

    constructor (idEleve: string, idClasse: string, idEtablissement: string, idPeriode : number, typesPeriode : TypePeriode[]) {
        super();
        this.all = [];
        this.idEleve = idEleve;
        this.idClasse =  idClasse;
        this.idEtablissement = idEtablissement;
        this.idPeriode = idPeriode;
        this.historiques = [];
        _.each(typesPeriode, (typeP) => {
            this.historiques.push(new Historique(typeP.id_type));
        });
        this.historiques.push(new Historique(null));

    }

    async getConversionTable(): Promise<any> {
        this.collection(TableConversion, {
            sync: async (): Promise<any> => {
                   let{data} = await http.get( `/competences/competence/notes/bilan/conversion?idEtab=${this.idEtablissement}&idClasse=${this.idClasse}`);
                        this.tableConversions.load(data);
            }
        });
        return this.tableConversions.sync();
    }
    async getSuivisDesAcquis ( ){

        try{

            //let moyennesEleveAllMatieres: number[] = [];
           // let moyennesClasseAllMatieres: number[] = [];

            await this.getConversionTable();
            let { data } = await http.get(`/competences/bilan/periodique/eleve/${this.idEleve}?idEtablissement=${this.idEtablissement}&idClasse=${this.idClasse}&idPeriode=${this.idPeriode}` );
            if(data.length > 0) {
                this.all = Mix.castArrayAs(SuiviDesAcquis, data);

                // pour chaque suiviDesAcquis setter
                // l'appréciation de toutes les classes et groupes
                _.each(this.all, (suiviDesAcquis) => {
                    suiviDesAcquis.idEleve = this.idEleve;
                    suiviDesAcquis.idClasse = this.idClasse;
                    suiviDesAcquis.idEtablissement = this.idEtablissement;
                    suiviDesAcquis.idPeriode = this.idPeriode;
                    if(suiviDesAcquis.appreciations !== null && suiviDesAcquis.appreciations !== undefined && _.find(suiviDesAcquis.appreciations,{id_periode: suiviDesAcquis.idPeriode}) !== undefined){
                        suiviDesAcquis.appreciation =  _.find(suiviDesAcquis.appreciations,{id_periode: suiviDesAcquis.idPeriode}).appreciation ;
                    }else{
                        suiviDesAcquis.appreciation = "";
                    }

                    // la moyenneEleve pour chaque période et chaque matiere
                    if (suiviDesAcquis.moyennesFinales !== null && suiviDesAcquis.moyennesFinales !== undefined && _.find(suiviDesAcquis.moyennesFinales,{id_periode: suiviDesAcquis.idPeriode}) !== undefined) {
                          suiviDesAcquis.moyenneEleve = _.find(suiviDesAcquis.moyennesFinales,{id_periode: suiviDesAcquis.idPeriode}).moyenneFinale;
                        }
                    else if (suiviDesAcquis.moyennes !== null && suiviDesAcquis.moyennes !== undefined &&
                        _.find(suiviDesAcquis.moyennes, {id: suiviDesAcquis.idPeriode}) !== undefined) {
                            suiviDesAcquis.moyenneEleve = _.find(suiviDesAcquis.moyennes, {id: suiviDesAcquis.idPeriode}).moyenne;
                        } else {
                            suiviDesAcquis.moyenneEleve = utils.getNN();
                        }
                    //la moyenneClasse pour la période sélectionnée et une matiere
                    if (suiviDesAcquis.moyennesClasse !== null && suiviDesAcquis.moyennesClasse !== undefined &&
                        _.find(suiviDesAcquis.moyennesClasse, {id: suiviDesAcquis.idPeriode}) !== undefined) {
                        suiviDesAcquis.moyenneClasse = _.find(suiviDesAcquis.moyennesClasse, {id: suiviDesAcquis.idPeriode}).moyenne

                    } else {
                        suiviDesAcquis.moyenneClasse = utils.getNN();
                    }
                    //le positionnement auto
                    if( suiviDesAcquis.positionnements_auto !== null && suiviDesAcquis.positionnements_auto !== undefined
                        && _.find(suiviDesAcquis.positionnements_auto, {id_periode: suiviDesAcquis.idPeriode}) !== undefined) {
                        let positionnementCalcule = _.find(suiviDesAcquis.positionnements_auto, {id_periode: suiviDesAcquis.idPeriode}).moyenne;
                        let positionnementConverti = utils.getMoyenneForBFC(positionnementCalcule + 1, this.tableConversions.all);
                        if ( suiviDesAcquis.positionnementsFinaux !== null && suiviDesAcquis.positionnementsFinaux !== undefined && suiviDesAcquis.positionnementsFinaux.length > 0) {
                            if(_.find( suiviDesAcquis.positionnementsFinaux, {id_periode: suiviDesAcquis.idPeriode}) !== undefined){
                                suiviDesAcquis.positionnement_final = _.find( suiviDesAcquis.positionnementsFinaux, {id_periode: suiviDesAcquis.idPeriode}).positionnementFinal;
                            }else{
                                suiviDesAcquis.positionnement_final = (positionnementConverti !== -1) ? positionnementConverti : 0;
                            }
                        }else{
                            suiviDesAcquis.positionnement_final = (positionnementConverti !== -1) ? positionnementConverti : 0;
                        }
                        suiviDesAcquis.positionnement_auto = (positionnementConverti !== -1) ? positionnementConverti : 0;
                    }else{
                        suiviDesAcquis.positionnement_auto = 0;
                    }
                     // ajout des moyennes par matiere sur les periodes
                    _.each(this.historiques, (histo) => {
                        //ajout de la moyennefinale si elle existe sinon ajout de la moyenne de l'eleve si elle existe pour la periode en cours
                        if(_.find(suiviDesAcquis.moyennesFinales,{id: histo.id_type}) !== undefined){
                            histo.moyEleveAllMatieres.push(_.find(suiviDesAcquis.moyennesFinales,{id: histo.id_type}).moyenne);
                        }else if(_.find(suiviDesAcquis.moyennes,{id: histo.id_type})!== undefined ){
                            histo.moyEleveAllMatieres.push(_.find(suiviDesAcquis.moyennes,{id: histo.id_type}).moyenne);
                        }
                       if(_.find(suiviDesAcquis.moyennesClasse,{id: histo.id_type}) !== undefined){
                           histo.moyClasseAllMatieres.push(_.find(suiviDesAcquis.moyennesClasse,{id: histo.id_type}).moyenne);
                       }
                    });
                });

                //calcul moyenne pour chaque periode
                _.each(this.historiques, (histo) => {
                    histo.moyGeneraleEleve = (histo.moyEleveAllMatieres.length === 0)? utils.getNN() : utils.average(histo.moyEleveAllMatieres).toFixed(2);
                    histo.moyGeneraleClasse = (histo.moyClasseAllMatieres.length === 0)? utils.getNN() : utils.average(histo.moyClasseAllMatieres).toFixed(2);
                });

            }

        }catch(e){
            notify.error('bilan.periodique.suivis.des.acquis.error.get');
            console.log(e)
        }

    }

    getHistoriqueByPeriode (id_periode): Historique {
        return _.find( this.historiques, {id_type: id_periode});
    }

}

