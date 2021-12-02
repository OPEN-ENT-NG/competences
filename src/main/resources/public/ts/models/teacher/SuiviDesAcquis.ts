import  http  from "axios";
import {Enseignant, ElementProgramme, TableConversion, Utils, TypePeriode, AppreciationMatiere} from "./index";
import * as utils from '../../utils/teacher';
import { Mix } from "entcore-toolkit";
import {notify, _, Collection, Model, http as httpEntcore} from "entcore";
import {Historique} from "../common/Historique";
import {getNN} from "../../utils/functions/utilsNN";

export class SuiviDesAcquis  {
    id_matiere : string;
    libelleMatiere : string;
    teachers : Enseignant[];
    elementsProgramme : string;
    elementsProgrammeByClasse: ElementProgramme[];
    appreciationByClasse : AppreciationMatiere;
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

    _positionnements_auto : any[];
    _moyenne : any[];
    idEleve: string;
    idClasse: string;
    idEtablissement: string;
    idPeriode : number;
    previousAppreciationMatiere : string;

    constructor(o?:any ) {
    }

    get api(){
        return{
            POST_DATA_RELEVE_PERIODIQUE: `/competences/releve/periodique`,
            POST_DATA_ELEMENT_PROGRAMME: `/competences/releve/element/programme`,
        }
    }

    toJson() {
        return {
            idEleve: this.idEleve,
            idMatiere: this.id_matiere,
            idEtablissement: this.idEtablissement,
            idPeriode: this.idPeriode
        };
    }

    setPreviousAppreciationMatiere () {
        this.previousAppreciationMatiere = this.appreciationByClasse.appreciation;
    };

    goBackAppreciation () {
        this.appreciationByClasse.appreciation = this.previousAppreciationMatiere;
    }

    async saveElementsProgramme(idClasse?, texte?) {
        let _data = _.extend(this.toJson(), {
            idClasse: idClasse != undefined ? idClasse : this.elementsProgrammeByClasse[0].id_classe,
            texte: texte != undefined ? texte : this.elementsProgramme
        });

        try{
            return await http.post(this.api.POST_DATA_ELEMENT_PROGRAMME, _data);
        }catch (e){
            notify.error('evaluations.releve.elementProgramme.classe.save.error');
            console.log(e);
        }
    }

    setElementsProgramme() {
        let elementsProgramme = ""

        this.elementsProgrammeByClasse.forEach(element => {
            if(elementsProgramme.length === 0) {
                elementsProgramme = element.texte;
            } else {
                elementsProgramme += " " + element.texte;
            }
        });

        this.elementsProgramme = elementsProgramme;
    }

    async saveElementProgrammeByClasse(elementProgrammeByClasse){
        this.saveElementsProgramme(elementProgrammeByClasse.id_classe, elementProgrammeByClasse.texte);
    }

    async savePositionnementEleve(positionnement) {
        let _data = _.extend(this.toJson(), {
            idClasse: this.idClasse,
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
            console.error(e);
        }
    }

    getPositionnementDefinitif(): any{
        if(this.positionnement_final !== this.positionnement_auto){
            return this.positionnement_final;
        }else{
            return this.positionnement_auto;
        }
    }

    initPositionnement() : any {
        this.positionnement_final = this.getPositionnementDefinitif();
    }
}

export class SuivisDesAcquis extends Model{
    all: SuiviDesAcquis[];
    tableConversions: Collection<TableConversion>;
    moyenneGeneraleClasse: string;
    idEleve: string;
    idClasse: string;
    idEtablissement: string;
    idPeriode : number;
    historiques: Historique[];
    hasCoefficientConflict : boolean;

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
        this.hasCoefficientConflict = false;
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

    checkCoefficientConflict = () => {
        this.hasCoefficientConflict = false;
        _.forEach(this.all, (subject) => {
            subject.coefficients = [];
            if(Utils.isNotNull(subject) && _.keys(subject.coefficient).length > 1){
                this.hasCoefficientConflict = true;
                subject.hasConflict = true;
                this.hasCoefficientConflict = true;
                _.mapObject(subject.coefficient, (val, key) => {
                    subject.coefficients.push(_.extend(val, {coefficient : key}));
                });
            } else {
                subject.hasConflict = false;
            }
        })
    }

    async getSuivisDesAcquis(){
        try{
            await this.getConversionTable();
            let {data} = await http.get(`/competences/bilan/periodique/eleve/${this.idEleve}` +
                `?idEtablissement=${this.idEtablissement}&idClasse=${this.idClasse}&idPeriode=${this.idPeriode}`);

            if(data.length > 0) {
                this.all = Mix.castArrayAs(SuiviDesAcquis, data);

                let suiviDesAcquisToRemove = [];

                // pour chaque suiviDesAcquis setter l'appréciation de toutes les classes et groupes
                _.each(this.all, (suiviDesAcquis) => {
                    suiviDesAcquis.idEleve = this.idEleve;
                    suiviDesAcquis.idEtablissement = this.idEtablissement;
                    suiviDesAcquis.idPeriode = this.idPeriode;

                    if(suiviDesAcquis.appreciations !== null && suiviDesAcquis.appreciations !== undefined){
                        let appreciationsPeriode = _.find(suiviDesAcquis.appreciations, {id_periode : suiviDesAcquis.idPeriode});
                        if(appreciationsPeriode !== undefined) {
                            suiviDesAcquis.appreciationByClasse = appreciationsPeriode.appreciationByClasse[0];
                        }
                    }
                    if(suiviDesAcquis.appreciationByClasse === undefined) {
                        suiviDesAcquis.appreciationByClasse = new AppreciationMatiere(suiviDesAcquis.idClasse);
                    }

                    // la moyenneEleve pour chaque période et chaque matiere
                    let finalAverage = (suiviDesAcquis.moyennesFinales !== null && suiviDesAcquis.moyennesFinales !== undefined) ?
                        _.find(suiviDesAcquis.moyennesFinales, {id_periode: suiviDesAcquis.idPeriode}) : undefined;
                    if(finalAverage !== undefined){
                        suiviDesAcquis.moyenneEleve = _.find(suiviDesAcquis.moyennesFinales,{id_periode : suiviDesAcquis.idPeriode}).moyenneFinale;
                    } else if (suiviDesAcquis.moyennes !== null && suiviDesAcquis.moyennes !== undefined &&
                        _.find(suiviDesAcquis.moyennes, {id: suiviDesAcquis.idPeriode}) !== undefined) {
                        suiviDesAcquis.moyenneEleve = _.find(suiviDesAcquis.moyennes, {id : suiviDesAcquis.idPeriode}).moyenne;
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
                    suiviDesAcquis.positionnement_auto = 0;
                    if(suiviDesAcquis.positionnements_auto !== null && suiviDesAcquis.positionnements_auto !== undefined
                        && _.find(suiviDesAcquis.positionnements_auto, {id_periode: suiviDesAcquis.idPeriode}) !== undefined) {
                        let positionnementCalcule = _.find(suiviDesAcquis.positionnements_auto, {id_periode: suiviDesAcquis.idPeriode}).moyenne;
                        let positionnementConverti = utils.getMoyenneForBFC(positionnementCalcule, this.tableConversions.all);
                        suiviDesAcquis.positionnement_auto = (positionnementConverti !== -1) ? positionnementConverti : 0;
                    }

                    if (suiviDesAcquis.positionnementsFinaux !== null && suiviDesAcquis.positionnementsFinaux !== undefined && suiviDesAcquis.positionnementsFinaux.length > 0) {
                        if(_.find( suiviDesAcquis.positionnementsFinaux, {id_periode : suiviDesAcquis.idPeriode}) !== undefined){
                            suiviDesAcquis.positionnement_final = _.find(suiviDesAcquis.positionnementsFinaux, {id_periode : suiviDesAcquis.idPeriode}).positionnementFinal;
                        } else {
                            suiviDesAcquis.positionnement_final = suiviDesAcquis.positionnement_auto;
                        }
                    } else {
                        suiviDesAcquis.positionnement_final = suiviDesAcquis.positionnement_auto;
                    }

                    // Positionnement et moyenne des sousMatieres
                    _.forEach(suiviDesAcquis.sousMatieres, (sousMat) => {
                        sousMat.posi = this.getPositionnement(suiviDesAcquis, sousMat);
                        sousMat.moy = this.getMoyenne(suiviDesAcquis, sousMat);
                        sousMat.moyClasse = this.getMoyenneClasse(suiviDesAcquis, sousMat);
                    });

                    // ajout des moyennes par matiere sur les periodes
                    _.each(this.historiques, (histo) => {
                        //ajout de la moyennefinale si elle existe sinon ajout de la moyenne de l'eleve si elle existe pour la periode en cours
                        if(_.find(suiviDesAcquis.moyennesFinales, {id : histo.id_type}) !== undefined){
                            histo.moyEleveAllMatieres.push(_.find(suiviDesAcquis.moyennesFinales, {id : histo.id_type}).moyenne);
                        } else if (_.find(suiviDesAcquis.moyennes, {id: histo.id_type})!== undefined){
                            histo.moyEleveAllMatieres.push(_.find(suiviDesAcquis.moyennes, {id : histo.id_type}).moyenne);
                        }
                        if(_.find(suiviDesAcquis.moyennesClasse,{id: histo.id_type}) !== undefined){
                            histo.moyClasseAllMatieres.push(_.find(suiviDesAcquis.moyennesClasse, {id: histo.id_type}).moyenne);
                        }
                    });

                    if (suiviDesAcquis.appreciationByClasse.appreciation === "" && suiviDesAcquis.moyenneEleve === "NN"
                        && finalAverage === undefined && suiviDesAcquis.positionnement_auto === 0
                        && suiviDesAcquis.positionnement_final === 0) {
                        suiviDesAcquisToRemove.push(suiviDesAcquis)
                    }

                    suiviDesAcquis.elementsProgrammeByClasse = Mix.castArrayAs(ElementProgramme, suiviDesAcquis.elementsProgrammeByClasse);
                });

                //supprime le suiviDesAcquis si pas de donnée pour l'élève pour la période sélectionnée
                _.each(suiviDesAcquisToRemove, (suiviDesAcquisToRemove) => {
                    this.all = _.without(this.all, suiviDesAcquisToRemove);
                });

                this.checkCoefficientConflict();

                //calcul moyenne pour chaque periode
                _.each(this.historiques, (histo) => {
                    histo.moyGeneraleEleve = (histo.moyEleveAllMatieres.length === 0) ? utils.getNN() : utils.average(histo.moyEleveAllMatieres).toFixed(2);
                    histo.moyGeneraleClasse = (histo.moyClasseAllMatieres.length === 0) ? utils.getNN() : utils.average(histo.moyClasseAllMatieres).toFixed(2);
                });
            }
        }catch(e){
            notify.error('bilan.periodique.suivis.des.acquis.error.get');
            console.error(e)
        }
    }

    getHistoriqueByPeriode (id_periode): Historique {
        return _.find( this.historiques, {id_type: id_periode});
    }

    getPositionnement(suivi, sousMat) {
        let res = 0;
        let moy = suivi._positionnements_auto;
        if(moy !== undefined) {
            moy = suivi._positionnements_auto[this.idPeriode];
            if(moy !== undefined) {
                moy = moy[sousMat.id_type_sousmatiere];
            }
        }

        if(Utils.isNotNull(moy) && moy.hasNote > 0) {
            let positionnementConverti = utils.getMoyenneForBFC(moy.moyenne, this.tableConversions.all);
            res = (positionnementConverti !== -1) ? positionnementConverti : 0;
        }

        return res;
    }

    getMoyenne(suivi, sousMat) {
        let moy = suivi._moyenne;
        if(moy !== undefined) {
            moy = suivi._moyenne[this.idPeriode];
            if(moy !== undefined) {
                moy = moy [sousMat.id_type_sousmatiere];
            }
        }
        return (moy === undefined) ? getNN() : moy.moyenne;
    }

    getMoyenneClasse(suivi, sousMat) {
        let moy = suivi._moyennesClasse;
        if(moy !== undefined) {
            moy = suivi._moyennesClasse[this.idPeriode];
            if(moy !== undefined) {
                moy = moy [sousMat.id_type_sousmatiere];
            }
        }
        return (moy === undefined)? getNN() : moy;
    }
}

