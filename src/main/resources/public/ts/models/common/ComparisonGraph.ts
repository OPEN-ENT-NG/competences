import {Model, angular, idiom as lang} from "entcore";
import {Graph} from "./Graph";
import {Classe, Defaultcolors, ElementBilanPeriodique, Utils} from "../teacher";
declare  let  _, Color: any;
export class ComparisonGraph extends Model {

    static scales (): object {
        return {
            xAxes: [{
                stacked: true,
                gridLines: {
                    offsetGridLines: true,
                    display:false
                }
            }],
            yAxes: [{
                stacked: false,
                gridLines: {
                    offsetGridLines: true
                }
            }]
        };
    }

    static tooltipsFunction(tooltipModel, forDomaine) {
        if (tooltipModel.body !== undefined) {
            let graphToSet = forDomaine? 'comparisonGraphDom' : 'comparisonGraph';
            let comparisonChart = angular.element('#comparisonGraphCompetences' + forDomaine).scope();
            if(comparisonChart)
                Utils.helperTooltipsForGraph(tooltipModel, forDomaine, comparisonChart, graphToSet, 80);
            let comparisonNotesChart = angular.element('#comparisonGraphNotes' + forDomaine).scope();
            if(comparisonNotesChart)
                Utils.helperTooltipsForGraph(tooltipModel, forDomaine, comparisonNotesChart, graphToSet, 80);
        }
    }

    static buildOptions(notes, tooltipGroup?, forDomaine?): object {

        return{
            maintainAspectRatio: false,
            //scales: this.scales(),
            title: {
                display: true,
                text: ' '
            },
            legend: {
                display: true,
                position: 'bottom',
                pointStyle: 'circle'
            },
            tooltips:{
                xPadding: 12,
                mode: (tooltipGroup === true)? 'label': 'nearest',
                custom: (tooltipModel) => {
                    this.tooltipsFunction(tooltipModel, forDomaine);
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
                            labelString: (notes)?lang.translate('averages'):lang.translate('level.items')
                        },
                        ticks: {
                            beginAtZero:true,
                            max: (notes)?20:4,
                        }
                    },
                    {
                        stacked: false,
                        position: "right",
                        id: "y-axis-1",
                        scaleLabel: {
                            display: false,
                            labelString: lang.translate('averages')
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

    static buildOptionsRadar(tooltipGroup?, forDomaine?): object {
        let i18nTitleView = (forDomaine !== true) ? 'evaluation.by.subject' : 'evaluation.by.domaine';
        let commonOption = {
            legend: {
                display: true,
                position: 'bottom',
                pointStyle: 'circle'
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
        return {average : averageOption, level : levelOption};
    }

    static setLabelDataForPeriode(newPeriodeDataset, labels, periodeDatasets, labelsIndex, newPercentage,
                                  periodesLabelIndex?) {
        let initPeriodeDatasetIfUndefined = (index) => {
            if (newPeriodeDataset[index] === undefined) {
                newPeriodeDataset[index] = new Array(labels.length);
                newPercentage[index] = new Array(labels.length);
            }
        };
        for (let datasetIndex = 0; datasetIndex < periodeDatasets.length; datasetIndex++) {
            let value = 0;
            let percent = ' ';
            initPeriodeDatasetIfUndefined(datasetIndex);
            if (periodesLabelIndex !== undefined) {
                let dataset = periodeDatasets[datasetIndex];
                value = dataset.data[periodesLabelIndex];
                percent = dataset.tooltipsPercentage[periodesLabelIndex];
            }
            newPeriodeDataset[datasetIndex][labelsIndex] = value;
            newPercentage[datasetIndex][labelsIndex] = percent;
        }
    }

    static buildPeriodeDataSet(periodeDatasets, periode, stackNumber, labelsPeriode, labels, withDarkness?) {
        let newPercentage = [];
        let newPeriodeDataset = [];
        let datas = [];
        let percentages = [];

        // config datasets add the name of Periode
        _.forEach(periodeDatasets, (dataset) => {
            dataset.label += ` ${periode.name}`;
            dataset.stack = stackNumber.toString();
            let backgroundColor = dataset.backgroundColor;
            if(backgroundColor !== undefined && withDarkness === true ) {
                dataset.backgroundColor =
                    `rgb(${Color(backgroundColor).darken((stackNumber) * 0.25).values.rgb.toString()})`;
                dataset.borderColor =
                    `rgb(${Color(dataset.backgroundColor).darken((stackNumber) * 0.25).values.rgb.toString()})`;
            }

            if(dataset.type === 'line') {
                dataset.pointHoverBackgroungColor =
                    `rgb(${Color(dataset.pointHoverBorderColor).darken((stackNumber) * 0.15).values.rgb.toString()})`;
                dataset.pointBackgroundColor =
                    `rgb(${Color(dataset.pointBackgroundColor).darken((stackNumber) * 0.15).values.rgb.toString()})`;
                dataset.backgroundColor =
                    `rgb(${Color(backgroundColor).darken((stackNumber) * 0.15).values.rgb.toString()})`;
                dataset.borderColor =
                    `rgb(${Color(dataset.backgroundColor).darken((stackNumber) * 0.15).values.rgb.toString()})`;
            }
            dataset.borderWidth = (stackNumber+1)*0.5 + 1;
            newPeriodeDataset.push(new Array(labels.length));
            newPercentage.push(new Array(labels.length));
        });


        // build new data for each dataSet for periode
        let periodesLabelIndex = 0;
        for (let labelsIndex = 0; labelsIndex < labels.length; labelsIndex++) {
            if (periodesLabelIndex < labelsPeriode.length
                && labels[labelsIndex] === labelsPeriode[periodesLabelIndex]) {
                this.setLabelDataForPeriode(newPeriodeDataset, labels, periodeDatasets, labelsIndex,
                    newPercentage, periodesLabelIndex);
                periodesLabelIndex++;
            }
            else {
                // put 0 for labels without data
                this.setLabelDataForPeriode(newPeriodeDataset, labels, periodeDatasets, labelsIndex, newPercentage);
            }
        }


        // replace data of periodeDatasets
        for (let datasetIndex = 0; datasetIndex < periodeDatasets.length; datasetIndex++) {
            let dataset = periodeDatasets[datasetIndex];
            dataset.data = newPeriodeDataset[datasetIndex];
            dataset.tooltipsPercentage = newPercentage[datasetIndex];
            datas.push(dataset.data);
            percentages.push(dataset.tooltipsPercentage);
        }
        return {datas: datas, percentages : percentages};
    }

    static builData(response, drawedPeriodes, notes, withDarkness?){
        let data = {
            labels: [],
            datasetOverride: [],
            data: [],
            type: 'bar',
            forDomaine:response[0].forDomaine
        };
        try {
            // build labels
            _.forEach(response, res => {
                data.labels = _.union(data.labels, res.configMixedChart.labels);
            });
            for (let i = 0; i < response.length; i++) {
                let datasets;
                if(notes)
                    datasets = response[i].configMixedChart.datasetsNotesOveride;
                else
                    datasets = response[i].datasets;
                let periode = _.findWhere(drawedPeriodes, {id_type: response[i].idPeriode});
                data.data = _.union(data.data,
                    this.buildPeriodeDataSet(datasets, periode, i, response[i].configMixedChart.labels,
                        data.labels, withDarkness).datas);
                data.datasetOverride = _.union(data.datasetOverride, datasets);

            }
            //remonter les notes qui seront affichés en ligne afin que toutes les courbes passsent devant les barres
            if(notes){
                data.data.splice(2,0,_.clone(data.data[4]),_.clone(data.data[5]));
                data.data.splice(6,2);
                data.datasetOverride.splice(2,0,_.clone(data.datasetOverride[4]),_.clone(data.datasetOverride[5]));
                data.datasetOverride.splice(6,2);
                if(data.data.length == 12){
                    data.data.splice(4,0,_.clone(data.data[8]),_.clone(data.data[9]));
                    data.data.splice(10,2);
                    data.datasetOverride.splice(4,0,_.clone(data.datasetOverride[8]),_.clone(data.datasetOverride[9]));
                    data.datasetOverride.splice(10,2);
                }
            }else{
                data.data.splice(2,0,_.clone(data.data[6]),_.clone(data.data[7]));
                data.data.splice(8,2);
                data.datasetOverride.splice(2,0,_.clone(data.datasetOverride[6]),_.clone(data.datasetOverride[7]));
                data.datasetOverride.splice(8,2);
                if(data.data.length == 18){
                    data.data.splice(4,0,_.clone(data.data[12]),_.clone(data.data[13]));
                    data.data.splice(14,2);
                    data.datasetOverride.splice(4,0,_.clone(data.datasetOverride[12]),_.clone(data.datasetOverride[13]));
                    data.datasetOverride.splice(14,2);
                }
            }
            return data;
        }
        catch (e) {
            console.error(e);
            return data;
        }

    }

    static builDataRadar(response, drawedPeriodes, withDarkness?){
        let data = {
            labels: [],
            average: [],
            data: [],
            legend:[]
        };
        try {
            // build labels
            _.forEach(response, res => {
                data.labels = _.union(data.labels, res.configMixedChart.labels);
            });
            for (let i = 0; i < response.length; i++) {
                let datasets = response[i].datasets;
                datasets.length = 2;
                let dataToClean = _.map(_.pluck(datasets,'data'), array =>{
                    return _.map(array, number =>{
                        if(number != "Nan")
                            return parseFloat(number.toString());
                        else
                            return 0
                    })
                });
                data.data = _.union(data.data,dataToClean);
                data.legend = _.union(data.legend,_.pluck(datasets,'label'));
            }
            return data;
        }
        catch (e) {
            console.error(e);
            return data;
        }

    }


    static async buildComparisonGraph(eleve, periodes, structure, forDomaine, niveauCompetences,
                                      withDarkness?, tooltipGroup?) {
        let allPromise = [];
        let drawedPeriodeTab = [];
        let fieldToCheck = (forDomaine===true)?'comparisonDom' : 'comparison';
        _.forEach(periodes, (periode) => {
            if (periode[fieldToCheck]) {
                let object = new ElementBilanPeriodique(new Classe({
                    id: eleve.idClasse,
                    type_groupe: Classe.type.CLASSE
                }), eleve, periode.id_type, structure, periodes);
                drawedPeriodeTab[drawedPeriodeTab.length] = periode;
                allPromise.push(Graph.getDataForGraph(object, eleve, forDomaine, niveauCompetences, true));
            }
        });
        if (!_.isEmpty(allPromise)) {
            let response = await Promise.all(allPromise);
            //graphiques batons
            let data = this.builData(_.clone(response), drawedPeriodeTab, false, withDarkness);
            let dataNotes = this.builData(_.clone(response), drawedPeriodeTab, true, withDarkness);
            let options = this.buildOptions(false, tooltipGroup, data.forDomaine);
            let optionsNotes = this.buildOptions(true, tooltipGroup, data.forDomaine);
            let graphObject = {
                type: 'bar',
                data: data,
                dataNotes: dataNotes,
                options: options,
                optionsNotes: optionsNotes
            };
            let fieldGraph = forDomaine? 'comparisonGraphDom' : 'comparisonGraph';
            eleve[fieldGraph] = graphObject;
            //graphiques radar
            let dataRadar = this.builDataRadar(_.clone(response), drawedPeriodeTab, withDarkness);
            let optionsRadar = this.buildOptionsRadar(tooltipGroup, data.forDomaine);
            let dataRadarNotes = _.clone(graphObject.dataNotes.datasetOverride);
            if(dataRadarNotes.length == 8)
                dataRadarNotes.length = 4;
            else
                dataRadarNotes.length = 6;
            let graphObjectRadar = {
                datasets: dataRadar,
                series: dataRadar.legend,
                averageSeries : _.pluck(dataRadarNotes,'label'),
                datasetsNotes: _.map(_.pluck(dataRadarNotes,'data'), array =>{
                    return _.map(array, number =>{
                        if(number != "Nan")
                            return parseFloat(number.toString());
                        else
                            return 0
                    })
                }),
                options: optionsRadar,
                colors: [{
                    backgroundColor: "#000080",
                    borderColor: "#000080",
                    fill: false,
                    radius: 3,
                    pointRadius: 3,
                    pointBorderWidth: 3,
                    pointBackgroundColor: "#000080",
                    pointBorderColor: "#000080",
                    pointHoverRadius: 6},
                    {
                backgroundColor: "#ffdf00",
                    borderColor: "#ffdf00",
                    fill: false,
                    radius: 3,
                    pointRadius: 3,
                    pointBorderWidth: 3,
                    pointBackgroundColor: "#ffdf00",
                    pointBorderColor: "#ffdf00",
                    pointHoverRadius: 6,
            },
            {
                backgroundColor: "#007cba",
                    borderColor: "#007cba",
                fill: false,
                radius: 3,
                pointRadius: 3,
                pointBorderWidth: 3,
                pointBackgroundColor: "#007cba",
                pointBorderColor: "#007cba",
                pointHoverRadius: 6
            },
                    {
                backgroundColor: "#fd9236",
                    borderColor: "#fd9236",
                    fill: false,
                    radius: 3,
                    pointRadius: 3,
                    pointBorderWidth: 3,
                    pointBackgroundColor: "#fd9236",
                    pointBorderColor: "#fd9236",
                    pointHoverRadius: 6
            },
            {
                backgroundColor: "#00ADF9",
                    borderColor: "#00ADF9",
                fill: false,
                radius: 3,
                pointRadius: 3,
                pointBorderWidth: 3,
                pointBackgroundColor: "#00ADF9",
                pointBorderColor: "#00ADF9",
                pointHoverRadius: 6,
            },
            {
                backgroundColor: "#a30000",
                    borderColor: "#a30000",
                fill: false,
                radius: 3,
                pointRadius: 3,
                pointBorderWidth: 3,
                pointBackgroundColor: "#a30000",
                pointBorderColor: "#a30000",
                pointHoverRadius: 6
            }],
                niveau: Defaultcolors
            };
            let fieldGraphRadar = forDomaine? 'comparisonGraphDomRadar' : 'comparisonGraphRadar';
            eleve[fieldGraphRadar] = graphObjectRadar;

        }
    }
}