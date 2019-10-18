import {Model, angular} from "entcore";
import {Graph} from "./Graph";
import {Classe, ElementBilanPeriodique, Utils} from "../teacher";
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
            let comparisonChart = angular.element('#comparisonGraph' + forDomaine).scope();
            Utils.helperTooltipsForGraph(tooltipModel, forDomaine, comparisonChart, graphToSet, 80);
        }
    }

    static buildOptions(tooltipGroup?, forDomaine?): object {

        return{
            maintainAspectRatio: false,
            scales: this.scales(),
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
            }
        };
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

    static builData(response, drawedPeriodes, withDarkness?){
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
                let datasets = response[i].datasets;
                let periode = _.findWhere(drawedPeriodes, {id_type: response[i].idPeriode});
                data.data = _.union(data.data,
                    this.buildPeriodeDataSet(datasets, periode, i, response[i].configMixedChart.labels,
                        data.labels, withDarkness).datas);
                data.datasetOverride = _.union(data.datasetOverride, datasets);

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
            let data = this.builData(response, drawedPeriodeTab, withDarkness);
            let options = this.buildOptions(tooltipGroup, data.forDomaine);
            let graphObject = {
                type: 'bar',
                data: data,
                options: options
            };
            let fieldGraph = forDomaine? 'comparisonGraphDom' : 'comparisonGraph';
            eleve[fieldGraph] = graphObject;
        }
    }
}