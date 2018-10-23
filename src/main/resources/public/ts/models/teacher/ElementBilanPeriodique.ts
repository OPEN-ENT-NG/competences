import {_, http, idiom as lang, Model} from 'entcore';
import {Classe, Eleve, evaluations, Structure, SuivisDesAcquis} from "./index";
import {Defaultcolors} from "../eval_niveau_comp";
import {SyntheseBilanPeriodique} from "./SyntheseBilanPeriodique";
import {AppreciationCPE} from "./AppreciationCPE";


declare  let Chart: any;

export class ElementBilanPeriodique extends Model {
    suivisAcquis : SuivisDesAcquis;
    projet : object;
    vieScolaire : object;
    graphique : object;
    synchronized: any;
    elementProgramme: any;
    idTypePeriode : number;

    classe: Classe;
    eleve: Eleve;
    structure: Structure;
    syntheseBilanPeriodique : SyntheseBilanPeriodique;
    appreciationCPE : AppreciationCPE;

    get api() {
        return {
            GET_DATA_FOR_GRAPH: `/competences/bilan/periodique/datas/graph?idEtablissement=${this.structure.id}&idClasse=${
                this.classe.id}&typeClasse=${this.classe.type_groupe}`,
            GET_DATA_FOR_GRAPH_DOMAINE: `/competences/bilan/periodique/datas/graph/domaine?idEtablissement=${
                this.structure.id}&idClasse=${this.classe.id}&typeClasse=${this.classe.type_groupe}`
        }
    }


    constructor(pClasse, pEleve, pIdTypePeriode) {
        super();
        this.structure = evaluations.structure;
        this.classe = pClasse;
        this.eleve = pEleve;
        this.idTypePeriode = pIdTypePeriode;
        this.suivisAcquis = new SuivisDesAcquis(this.eleve.id, this.classe.id, this.structure.id, this.idTypePeriode );
    }


    async syncSyntheseBilanPeriodique() {
        return new Promise((resolve, reject) => {
            if (this.idTypePeriode != null) {
                this.syntheseBilanPeriodique = new SyntheseBilanPeriodique(this.eleve.id, this.idTypePeriode);
                this.syntheseBilanPeriodique.sync();
            }
            resolve();
        });
    }


    moyenneNiveau (competencesNotes) : number {
        let res  = 0;
        let summ = 0;
        _.forEach(competencesNotes, (c) => {
            let val = (c.niveau_final !== null)? c.niveau_final : c.evaluation;
            res += (val + 1);
            summ ++;
        });

        return (summ === 0)? 0 : parseFloat(Math.round(res/summ).toFixed(2));
    }

    moyenneNote (notes) : number {
        let res = 0;
        let sum = 0;
        let sumCoef = 0;

        _.forEach(notes, (n) => {
            if(n.valeur !== null) {
                // si on a une moyenne finale, on la prend
                let real_note = ( parseFloat(n.valeur) * parseFloat(n.coefficient) * 20 / parseFloat(n.diviseur));
                let val_to_add = (n.moyenne !== null)? (n.moyenne * parseFloat(n.coefficient)) : real_note;
                sum += val_to_add;
                sumCoef += parseFloat(n.coefficient);
            }
        });
        if (sumCoef > 0) {
            res = sum / sumCoef;
        }
        return parseFloat(res.toFixed(2));
    }

    getDataForGraph(eleve, forDomaine?): any {
        return new Promise((resolve, reject) => {
            let uri = (forDomaine === true) ? this.api.GET_DATA_FOR_GRAPH_DOMAINE : this.api.GET_DATA_FOR_GRAPH;
            uri += '&idEleve=' + eleve.id;
            uri += (this.idTypePeriode !== null) ? ('&idTypePeriode=' + this.idTypePeriode) : '';
            http().get(uri)
                .done(async (res) => {
                    this.configCharts(eleve, res, forDomaine);
                    resolve();
                })
                .error((err) => {
                    reject(err);
                });
        });
    }

    configCharts (eleve, _datas, forDomaine?) : any {
        // CompetenceNotes
        let data = [];
        let dataStudent = [];  // Moyenne CompetenceNotes par matiere de l'élève
        let dataClass = []; // Moyenne CompetenceNotes par matière de la classe

        // notes
        let average = [];
        let averageStudent = []; // Moyenne notes par matiere de l'élève
        let averageClass = []; // Moyenne notes par matiere de la classe


        let labels = [] //Nom des matières
        let data_set1 = [];
        let data_set2 = [];
        let data_set3 = [];
        let data_set4 = [];
        let colors = ['#00ADF9',
            '#fd9236',
            '#dc151c',
            '#46BFBD',
            '#949fb1',
            '#5f626c',
            '#40424b'];


        let series = [lang.translate('level.student'),lang.translate('level.class') ];
        let i18nTitleView =(forDomaine !== true)?'evaluation.by.subject' :'evaluation.by.domaine';
        let options = {legend : {
                display: true
            },
            title: {
                display: false,
                text: lang.translate(i18nTitleView)
            },
        };
        let configRadarChart = {
            datasets : {
                data: data,
                labels: labels,
                average : average
            },
            series : series,
            averageSeries : [lang.translate('average.student'),lang.translate('average.class')],
            options : options,
            colors : colors,
            niveau : Defaultcolors
        };

        let configMixedChart = {
            labels: labels,
            colors : colors,
            niveau : Defaultcolors,
            labelyAxes : [lang.translate('level.items'),lang.translate('averages')],
            datasets : {
                test : undefined,
                average : average,
                data_set1 : data_set1,
                data_set2 : data_set2,
                data_set3 : data_set3,
                data_set4 : data_set4
            },
            averageStudent :  {
                label: lang.translate('level.student'),
                type: 'line',
                data: dataStudent,
                borderColor:'#00ADF9',
                backgroundColor: '#009eea',
                fill: false,
                id: undefined,
                options: {
                    scales: {
                        xAxes: [{
                            stacked: false,
                        }],
                        yAxes: [{
                            stacked: false
                        }]
                    }
                }

            },
            averageClass: {
                label: lang.translate('level.class'),
                type: 'line',
                data: dataClass,
                borderColor: '#5f626c',
                backgroundColor: '#5f626c',
                borderWidth :
                1 + Chart.defaults.global.elements.line.borderWidth,
                fill: false,
                id:  undefined,
                options: {
                    scales: {
                        xAxes: [{
                            stacked: false,
                        }],
                        yAxes: [{
                            stacked: false
                        }]
                    }
                }
            }

        };
        _.forEach(_datas, (matiereOrDomaine) => {
            let diviseur : number = 4;
            if(matiereOrDomaine.id !== undefined) {
                labels.push(
                    (matiereOrDomaine.name !== undefined)? matiereOrDomaine.name : matiereOrDomaine.codification);
                dataStudent.push(this.moyenneNiveau(matiereOrDomaine.competencesNotesEleve));
                dataClass.push(this.moyenneNiveau(matiereOrDomaine.competencesNotes));

                averageStudent.push(this.moyenneNote(matiereOrDomaine.notesEleve));
                averageClass.push(this.moyenneNote(matiereOrDomaine.notes));

                let nbrCompNotesUnevaluated =  _.where(matiereOrDomaine.competencesNotesEleve , {evaluation: -1});
                nbrCompNotesUnevaluated = (!nbrCompNotesUnevaluated)? nbrCompNotesUnevaluated.length: 0;
                let nbrCompNotes = (!matiereOrDomaine.competencesNotesEleve)? 0 :
                    (matiereOrDomaine.competencesNotesEleve.length - nbrCompNotesUnevaluated);

                let nbrCompNotes_set1 = _.union(
                    _.where(matiereOrDomaine.competencesNotesEleve , {evaluation: 0, niveau_final: null}),
                    _.where(matiereOrDomaine.competencesNotesEleve , {niveau_final: 0}));
                nbrCompNotes_set1 =  !(nbrCompNotes_set1)? 0 : nbrCompNotes_set1.length;
                let set1_val = Math.min(diviseur, diviseur * (nbrCompNotes_set1 / (nbrCompNotes)));

                let nbrCompNotes_set2 = _.union(
                    _.where(matiereOrDomaine.competencesNotesEleve , {evaluation: 1, niveau_final: null}),
                    _.where(matiereOrDomaine.competencesNotesEleve , {niveau_final: 1}));
                nbrCompNotes_set2 =  !(nbrCompNotes_set2)? 0 : nbrCompNotes_set2.length;
                let set2_val = Math.min(diviseur, diviseur * (nbrCompNotes_set2 / (nbrCompNotes)) + set1_val);

                let nbrCompNotes_set3 = _.union(
                    _.where(matiereOrDomaine.competencesNotesEleve , {evaluation: 2, niveau_final: null}),
                    _.where(matiereOrDomaine.competencesNotesEleve , {niveau_final: 2}));
                nbrCompNotes_set3 =  !(nbrCompNotes_set3)? 0 : nbrCompNotes_set3.length;
                let set3_val = Math.min(diviseur, diviseur * (nbrCompNotes_set3 / (nbrCompNotes)) + set2_val);

                let nbrCompNotes_set4 = _.union(
                    _.where(matiereOrDomaine.competencesNotesEleve , {evaluation: 3, niveau_final: null}),
                    _.where(matiereOrDomaine.competencesNotesEleve , {niveau_final: 3}));
                nbrCompNotes_set4 =  !(nbrCompNotes_set4)? 0 : nbrCompNotes_set4.length;
                let set4_val = Math.min(diviseur, diviseur * (nbrCompNotes_set4 / (nbrCompNotes)) + set3_val);

                data_set1.push(set1_val.toFixed(2));
                data_set2.push(set2_val.toFixed(2));
                data_set3.push(set3_val.toFixed(2));
                data_set4.push(set4_val.toFixed(2));

            }
        });
        data.push(dataStudent);
        data.push(dataClass);

        average.push(averageStudent);
        average.push(averageClass);

        if (forDomaine === true) {
            eleve.configRadarChartDomaine = configRadarChart;
            eleve.configMixedChartDomaine = configMixedChart;
        }
        else {
            eleve.configRadarChart = configRadarChart;
            eleve.configMixedChart = configMixedChart;
        }
    }
}