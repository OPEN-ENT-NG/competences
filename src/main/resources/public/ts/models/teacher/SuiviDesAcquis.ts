import  http  from "axios";
import {Enseignant, ElementProgramme,  TableConversion,Utils} from "./index";
import * as utils from '../../utils/teacher';
import { Mix } from "entcore-toolkit";
import {notify, _, Collection, Model, http as httpEntcore} from "entcore";




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
            POST_DATA_RELEVE_PERIODIQUE: `/competences/releve/periodique`,
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
           delete: this.positionnement_final === ""
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

    constructor (idEleve: string, idClasse: string, idEtablissement: string, idPeriode : number ) {
        super();
        this.all = [];
        this.idEleve = idEleve;
        this.idClasse =  idClasse;
        this.idEtablissement = idEtablissement;
        this.idPeriode = idPeriode;

    }

    async getConversionTable(): Promise<any> {
        this.collection(TableConversion, {
            sync: async (): Promise<any> => {

                   let{data} = await http.get( `/competences/competence/notes/bilan/conversion?idEtab=${
                        this.idEtablissement}&idClasse=${this.idClasse}`);
                        this.tableConversions.load(data);
            }
        });
        return this.tableConversions.sync();
    }
    async getSuivisDesAcquis ( ){

        try{

            let moyennesEleveAllMatieres: number[] = [];
            let moyennesClasseAllMatieres: number[] = [];

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

                    // la moyenneEleve
                    if (suiviDesAcquis.moyenne_finale !== null && suiviDesAcquis.moyenne_finale !== undefined) {
                        suiviDesAcquis.moyenneEleve = suiviDesAcquis.moyenne_finale;
                        moyennesEleveAllMatieres.push(suiviDesAcquis.moyenneEleve);
                    } else if (suiviDesAcquis.moyennes !== undefined && suiviDesAcquis.moyennes.length > 0) {
                        suiviDesAcquis.moyenneEleve = (_.find(suiviDesAcquis.moyennes, {id: this.idPeriode}) !== undefined) ?
                            _.find(suiviDesAcquis.moyennes, {id: this.idPeriode}).moyenne : utils.getNN();

                        if (suiviDesAcquis.moyenneEleve !== utils.getNN()) {
                            moyennesEleveAllMatieres.push(suiviDesAcquis.moyenneEleve);
                        }
                    } else {
                        suiviDesAcquis.moyenneEleve = utils.getNN();
                    }
                    //la moyenneClasse pour la période sélectionnée
                    if (suiviDesAcquis.moyennesClasse !== undefined && suiviDesAcquis.moyennesClasse.length > 0) {
                        suiviDesAcquis.moyenneClasse = (_.find(suiviDesAcquis.moyennesClasse, {id: this.idPeriode}) !== undefined) ?
                            _.find(suiviDesAcquis.moyennesClasse, {id: this.idPeriode}).moyenne : utils.getNN();

                        if (suiviDesAcquis.moyenneClasse !== utils.getNN()) {
                            moyennesClasseAllMatieres.push(suiviDesAcquis.moyenneClasse);
                        }
                    } else {
                        suiviDesAcquis.moyenneClasse = utils.getNN();
                    }
                    //le positionnement auto
                    if(suiviDesAcquis.positionnements_auto !== undefined && suiviDesAcquis.positionnements_auto.length > 0) {
                        let positionnementCalcule = (_.find(suiviDesAcquis.positionnements_auto, {id_periode: this.idPeriode}) !== undefined) ?
                            _.find(suiviDesAcquis.positionnements_auto, {id_periode: this.idPeriode}).moyenne : 0;
                        let positionnementConverti = utils.getMoyenneForBFC(positionnementCalcule + 1, this.tableConversions.all);
                        if (suiviDesAcquis.positionnement_final === undefined) {
                            suiviDesAcquis.positionnement_final = (positionnementConverti !== -1) ? positionnementConverti : 0;
                        }
                        suiviDesAcquis.positionnement_auto = (positionnementConverti !== -1) ? positionnementConverti : 0;
                    }else{
                        suiviDesAcquis.positionnement_auto = 0;
                    }
                });
                this.moyenneGeneraleElve = (moyennesEleveAllMatieres.length === 0) ? utils.getNN() : utils.average(moyennesEleveAllMatieres).toFixed(2);
                this.moyenneGeneraleClasse = (moyennesClasseAllMatieres.length === 0) ? utils.getNN() : utils.average(moyennesClasseAllMatieres).toFixed(2);
            }
        }catch(e){
            notify.error('bilan.periodique.suivis.des.acquis.error.get');
            console.log(e)
        }
    }

}

