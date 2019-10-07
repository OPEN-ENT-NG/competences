import {Defaultcolors} from "../eval_niveau_comp";
import {_, angular, http, idiom as lang, Model} from "entcore";
import {Utils} from "../teacher";


declare  let Chart, Color: any;

export class Graph extends Model{

    static _metas : any;

    static getDataForGraph(that, eleve, forDomaine?, niveauCompetences?, forComparison?): any {
        return new Promise((resolve, reject) => {
            let uri = (forDomaine === true) ? that.api.GET_DATA_FOR_GRAPH_DOMAINE : that.api.GET_DATA_FOR_GRAPH;
            uri += '&idEleve=' + eleve.id;
            uri += (that.idPeriode !== null) ? ('&idPeriode=' + that.idPeriode) : '';
            http().getJson(uri)
                .done(async (res) => {
                    resolve(_.extend(this.configCharts(eleve, res, forDomaine, niveauCompetences, forComparison),
                        {idPeriode: that.idPeriode}));
                })
                .error((err) => {
                    reject(err);
                });
        });
    }

    static moyenneNiveau(competencesNotes): number {
        let res = 0;
        let summ = 0;
        _.forEach(competencesNotes, (c) => {
            let val = (c.niveau_final !== null) ? c.niveau_final : c.evaluation;
            res += (val + 1);
            summ++;
        });

        return (summ === 0) ? 0 : parseFloat((res / summ).toFixed(2));
    }

    static buildDatasets(configMixedChart, niveauCompetences) : Array<object> {
        return [
            {
                label: _.clone(configMixedChart.averageStudent.label),
                type: 'line',
                data: _.clone(configMixedChart.averageStudent.data),
                tooltipsPercentage : _.clone(configMixedChart.averageStudent.data),
                fill: false,
            },
            {
                label: _.clone(configMixedChart.averageClass.label),
                type: 'line',
                data: _.clone(configMixedChart.averageClass.data),
                tooltipsPercentage: _.clone(configMixedChart.averageClass.data),
                fill: false,
                borderWidth : 1 + Chart.defaults.global.elements.line.borderWidth,
            },
            {
                label: _.clone(niveauCompetences[3].libelle),
                backgroundColor: _.clone(niveauCompetences[3].couleur),
                borderColor :`rgb(${Color(niveauCompetences[3].couleur).darken(0.25).values.rgb.toString()})`,
                data: _.clone(configMixedChart.datasets.data_set1),
                tooltipsPercentage: _.clone(configMixedChart.datasets.percentage_set1),
            }, {
                label: _.clone(niveauCompetences[2].libelle),
                backgroundColor: _.clone(niveauCompetences[2].couleur),
                borderColor :`rgb(${Color(niveauCompetences[2].couleur).darken(0.25).values.rgb.toString()})`,
                data: _.clone(configMixedChart.datasets.data_set2),
                tooltipsPercentage: _.clone(configMixedChart.datasets.percentage_set2)
            }, {
                label: _.clone(niveauCompetences[1].libelle),
                backgroundColor: _.clone(niveauCompetences[1].couleur),
                borderColor :`rgb(${Color(niveauCompetences[1].couleur).darken(0.25).values.rgb.toString()})`,
                data: _.clone(configMixedChart.datasets.data_set3),
                tooltipsPercentage: _.clone(configMixedChart.datasets.percentage_set3)
            }, {
                label: _.clone(niveauCompetences[0].libelle),
                backgroundColor: _.clone(niveauCompetences[0].couleur),
                borderColor :`rgb(${Color(niveauCompetences[0].couleur).darken(0.25).values.rgb.toString()})`,
                data: _.clone(configMixedChart.datasets.data_set4),
                tooltipsPercentage: _.clone(configMixedChart.datasets.percentage_set4)
            }
        ];
    }

    static moyenneNote(notes): number {
        let res = 0;
        let sum = 0;
        let sumCoef = 0;

        _.forEach(notes, (n) => {
            if (n.valeur !== null) {
                // si on a une moyenne finale, on la prend
                let real_note = (parseFloat(n.valeur) * parseFloat(n.coefficient) * 20 / parseFloat(n.diviseur));
                let val_to_add = (n.moyenne !== null) ? (n.moyenne * parseFloat(n.coefficient)) : real_note;
                sum += val_to_add;
                sumCoef += parseFloat(n.coefficient);
            }
        });
        if (sumCoef > 0) {
            res = sum / sumCoef;
        }
        return parseFloat(res.toFixed(2));
    }

    static tooltipsFunction(tooltipModel, forDomaine, eleve) : any{
        if (tooltipModel.body !== undefined) {
            let graphToSet = forDomaine? 'configMixedChartDomaine' : 'configMixedChart';
            let currentChart = angular.element('#mixedChart' + forDomaine).scope();
            if(currentChart === undefined){
                currentChart = {
                    informations : {eleve : eleve}
                };
            }
            Utils.helperTooltipsForGraph(tooltipModel, forDomaine, currentChart, graphToSet, 60);
        }
    }

    static buildOption(configMixedChart, forDomaine, eleve){
        return  {
            maintainAspectRatio: false,
            title: {
                display: true,
                text: ' '
            },
            legend: {
                display: true,
                position: 'bottom',
                pointStyle: 'circle'
            },
            tooltips: {
                mode: 'label',
                custom: (tooltipModel) => {
                    this.tooltipsFunction(tooltipModel, forDomaine, eleve);
                }
            },
            responsive: true,
            scales: {
                xAxes: [{
                    stacked: true,
                    gridLines: {
                        display: false
                    },
                    ticks: {
                        autoSkip: false
                    }
                }],
                yAxes: [
                    {
                        stacked: false,
                        position: "left",
                        id: "y-axis-0",
                        scaleLabel: {
                            display: true,
                            labelString: configMixedChart.labelyAxes[0]
                        },
                        ticks: {
                            beginAtZero:true,
                            max: 4,
                        }
                    },
                    {
                        stacked: false,
                        position: "right",
                        id: "y-axis-1",
                        scaleLabel: {
                            display: false,
                            labelString: configMixedChart.labelyAxes[1]
                        }
                        ,
                        gridLines: {
                            display:false
                        },
                        ticks: {
                            callback: function(value, index, values) {
                                return ' ';
                            }
                        }
                    }
                ]
            }
        };
    }

    static configCharts(eleve, _datas, forDomaine?, niveauCompetences?, forComparison?): object {
        // CompetenceNotes
        let data = [];
        let dataStudent = [];  // Moyenne CompetenceNotes par matiere de l'élève
        let dataClass = []; // Moyenne CompetenceNotes par matière de la classe

        // notes
        let average = [];
        let averageStudent = []; // Moyenne notes par matiere de l'élève
        let averageClass = []; // Moyenne notes par matiere de la classe


        let labels = []; //Nom des matières
        let data_set1 = [], percentage_set1 = [];
        let data_set2 = [], percentage_set2 = [];
        let data_set3 = [], percentage_set3 = [];
        let data_set4 = [], percentage_set4 = [];
        let colors = ['#00ADF9',
            '#fd9236',
            '#dc151c',
            '#46BFBD',
            '#949fb1',
            '#5f626c',
            '#40424b'];


        let series = [lang.translate('level.student'), lang.translate('level.class')];
        let i18nTitleView = (forDomaine !== true) ? 'evaluation.by.subject' : 'evaluation.by.domaine';
        let commonOption = {
            legend: {
                display: true
            },
            title: {
                display: false,
                text: lang.translate(i18nTitleView)
            }
        };
        let tooltips = {tooltips: {
                mode: 'label',
                custom: function (tooltipModel) {
                    if (tooltipModel.body !== undefined) {
                        tooltipModel.width += 30;
                        for (let i = 0; i < tooltipModel.body.length; i++) {
                            let yLabel = tooltipModel.dataPoints[i].yLabel;
                            let body = tooltipModel.body[i];
                            if(Utils.isNull(body)){
                                continue;
                            }
                            let line = tooltipModel.body[i].lines[0];
                            if(Utils.isNotNull(line) && !line.endsWith(yLabel)){
                                tooltipModel.body[i].lines[0] += `${yLabel}`;
                            }
                        }
                    }
                }
            }};
        let averageOption = {
            scale: {scaleOverride : true,
                ticks: {
                    min: 0,
                    max: 20,
                    stepSize: 2
                }
            }
        };
        let levelOption = {
            scale: {scaleOverride : true,
                ticks: {
                    min: 0,
                    max: 4,
                    stepSize: 1
                }
            }
        };
        _.extend(commonOption, tooltips);
        _.extend(averageOption,commonOption);
        _.extend(levelOption, commonOption);

        let options = {average : averageOption, level : levelOption};
        let configRadarChart = {
            datasets: {
                data: data,
                labels: labels,
                average: average
            },
            series: series,
            averageSeries: [lang.translate('average.student'), lang.translate('average.class')],
            options: options,
            colors: colors,
            niveau: Defaultcolors
        };

        let configMixedChart = {
            labels: labels,
            colors: colors,
            niveau: Defaultcolors,
            labelyAxes: [lang.translate('level.items'), lang.translate('averages')],
            datasetsOveride : undefined,
            options: undefined,
            _datas: [],
            datasets: {
                test: undefined,
                average: data,
                data_set1: data_set1,
                data_set2: data_set2,
                data_set3: data_set3,
                data_set4: data_set4,
                percentage_set1: percentage_set1,
                percentage_set2: percentage_set2,
                percentage_set3: percentage_set3,
                percentage_set4: percentage_set4
            },
            averageStudent: {
                label: lang.translate('level.student'),
                type: 'line',
                data: dataStudent,
                borderColor: '#00ADF9',
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
                borderWidth:
                1 + Chart.defaults.global.elements.line.borderWidth,
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
            }

        };
        _.forEach(_datas, (matiereOrDomaine) => {
            let diviseur: number = 4;
            if (matiereOrDomaine.id !== undefined) {
                labels.push(
                    (matiereOrDomaine.name !== undefined) ? matiereOrDomaine.name : matiereOrDomaine.codification);
                dataStudent.push(this.moyenneNiveau(matiereOrDomaine.competencesNotesEleve));
                dataClass.push(this.moyenneNiveau(matiereOrDomaine.competencesNotes));
                if(matiereOrDomaine.studentAverage !== null){
                    averageStudent.push(parseFloat(matiereOrDomaine.studentAverage));
                }else{
                    averageStudent.push(0);
                }
                if(matiereOrDomaine.classAverage !== null){
                    averageClass.push(parseFloat(matiereOrDomaine.classAverage));
                }else{
                    averageClass.push(0);
                }

                let nbrCompNotesUnevaluated = _.where(matiereOrDomaine.competencesNotesEleve, {evaluation: -1});
                nbrCompNotesUnevaluated = (!nbrCompNotesUnevaluated) ? nbrCompNotesUnevaluated.length : 0;
                let nbrCompNotes = (!matiereOrDomaine.competencesNotesEleve) ? 0 :
                    (matiereOrDomaine.competencesNotesEleve.length - nbrCompNotesUnevaluated);

                let nbrCompNotes_set1 = _.union(
                    _.where(matiereOrDomaine.competencesNotesEleve, {evaluation: 0, niveau_final: null}),
                    _.where(matiereOrDomaine.competencesNotesEleve, {niveau_final: 0}));
                nbrCompNotes_set1 = !(nbrCompNotes_set1) ? 0 : nbrCompNotes_set1.length;
                let set1_val = Math.min(diviseur, diviseur * (nbrCompNotes_set1 / (nbrCompNotes)));
                let set1_percent = `${(nbrCompNotes_set1 * 100 / (nbrCompNotes)).toFixed(2)} %`;

                let nbrCompNotes_set2 = _.union(
                    _.where(matiereOrDomaine.competencesNotesEleve, {evaluation: 1, niveau_final: null}),
                    _.where(matiereOrDomaine.competencesNotesEleve, {niveau_final: 1}));
                nbrCompNotes_set2 = !(nbrCompNotes_set2) ? 0 : nbrCompNotes_set2.length;
                let set2_val = Math.min(diviseur, diviseur * (nbrCompNotes_set2 / (nbrCompNotes)) + set1_val);
                let set2_percent = `${(nbrCompNotes_set2 * 100 / (nbrCompNotes)).toFixed(2)} %`;

                let nbrCompNotes_set3 = _.union(
                    _.where(matiereOrDomaine.competencesNotesEleve, {evaluation: 2, niveau_final: null}),
                    _.where(matiereOrDomaine.competencesNotesEleve, {niveau_final: 2}));
                nbrCompNotes_set3 = !(nbrCompNotes_set3) ? 0 : nbrCompNotes_set3.length;
                let set3_val = Math.min(diviseur, diviseur * (nbrCompNotes_set3 / (nbrCompNotes)) + set2_val);
                let set3_percent = `${(nbrCompNotes_set3 * 100 / (nbrCompNotes)).toFixed(2)} %`;

                let nbrCompNotes_set4 = _.union(
                    _.where(matiereOrDomaine.competencesNotesEleve, {evaluation: 3, niveau_final: null}),
                    _.where(matiereOrDomaine.competencesNotesEleve, {niveau_final: 3}));
                nbrCompNotes_set4 = !(nbrCompNotes_set4) ? 0 : nbrCompNotes_set4.length;
                let set4_val = Math.min(diviseur, diviseur * (nbrCompNotes_set4 / (nbrCompNotes)) + set3_val);
                let set4_percent = `${(nbrCompNotes_set4 * 100 / (nbrCompNotes)).toFixed(2)} %`;

                // données des niveaux de maitrise
                data_set1.push(set1_val.toFixed(2));
                data_set2.push(set2_val.toFixed(2));
                data_set3.push(set3_val.toFixed(2));
                data_set4.push(set4_val.toFixed(2));

                // Pourcentage des niveaux de maitrise
                percentage_set1.push(set1_percent);
                percentage_set2.push(set2_percent);
                percentage_set3.push(set3_percent);
                percentage_set4.push(set4_percent);
            }
        });
        data.push(_.clone(dataStudent));
        data.push(_.clone(dataClass));

        average.push(_.clone(averageStudent));
        average.push(_.clone(averageClass));

        if( forComparison !== true) {
            if (forDomaine === true) {
                eleve.configRadarChartDomaine = configRadarChart;
                eleve.configMixedChartDomaine = configMixedChart;
            }
            else {
                eleve.configRadarChart = configRadarChart;
                eleve.configMixedChart = configMixedChart;
            }
            if(niveauCompetences !== undefined) {
                configMixedChart.datasetsOveride = this.buildDatasets(configMixedChart, niveauCompetences);
                configMixedChart._datas = [averageStudent, averageClass, data_set1, data_set2, data_set3, data_set4];
                configMixedChart.options = this.buildOption(configMixedChart, forDomaine, eleve);
            }
        }

        if(niveauCompetences !== undefined && forComparison == true){

            return {
                configMixedChart: configMixedChart,
                datasets: this.buildDatasets(configMixedChart, niveauCompetences),
                forDomaine : forDomaine
            };
        }
        else {
            return {};
        }

    }
}