/**
 * Created by anabah on 10/10/2018.
 */
import {_, notify, idiom as lang} from 'entcore';
import http from "axios";
import {Classe, ElementBilanPeriodique} from "../teacher";

declare let $ : any;
declare let Chart: any;

export class ExportBulletins {

     toJSON (options) {
        let o = {
            idStudents: options.idStudents,
            getResponsable: (options.getResponsable === true)? options.getResponsable:false ,
            getProgramElements: (options.getProgramElements === true)? options.getProgramElements:false ,
            moyenneClasse: (options.moyenneClasse === true)? options.moyenneClasse:false ,
            moyenneEleve: (options.moyenneEleve === true)? options.moyenneEleve:false ,
            positionnement: (options.positionnement === true)? options.positionnement:false ,
            showBilanPerDomaines: (options.showBilanPerDomaines === true)? options.showBilanPerDomaines:false ,
            showFamily: (options.showFamily === true)? options.showFamily:false ,
            showProjects: (options.showProjects === true)? options.showProjects:false ,
            threeLevel: (options.threeLevel === true)? options.threeLevel:false ,
            threeMoyenneClasse: (options.threeMoyenneClasse === true)? options.threeMoyenneClasse:false ,
            threeMoyenneEleve: (options.threeMoyenneEleve === true)? options.threeMoyenneEleve:false ,
            threePage: (options.threePage === true)? options.threePage:false ,
            classeName: options.classeName,
            idClasse: options.idClasse,
            idStructure: options.idStructure,
            pdfBlobs: options.pdfBlobs,
            images: options.images,
            nameCE: (options.nameCE !== undefined)? options.nameCE : "",
            imgStructure: (options.imgStructure !== undefined)? options.imgStructure : "",
            hasImgStructure: (options.imgStructure !== undefined)? true: false,
            imgSignature: (options.imgSignature !== undefined)? options.imgSignature : "",
            hasImgSignature: (options.imgSignature !== undefined)? true: false,
        };
        if (options.idPeriode !== null || options.idPeriode!== undefined){
            _.extend(o, {idPeriode: options.idPeriode});
        }
        return o;
    }


    public static async  generateBulletins (options, $scope) {

        return new Promise( async (resolve, reject) => {
            try {

                options.images = {};
                if (options.showBilanPerDomaines === true) {
                    await this.createCanvas(options, $scope);
                }
                // await this.drawGraph(options);
                let data = await http.post(`/competences/export/bulletins`, new ExportBulletins().toJSON(options),
                    {responseType: 'arraybuffer'});

                let blob = new Blob([data.data]);
                let link = document.createElement('a');
                link.href = window.URL.createObjectURL(blob);
                link.download =  data.headers['content-disposition'].split('filename=')[1];
                document.body.appendChild(link);
                link.click();
                setTimeout(function() {
                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(link.href);
                }, 100);
                resolve();
                $('.chart-container').empty();
                notify.success(options.classeName + ' : ' + lang.translate('evaluations.export.bulletin.success'));
            } catch (e) {
                console.dir(e);
                $('.chart-container').empty();
                reject();
                notify.error(options.classeName + ' : ' + lang.translate('evaluations.export.bulletin.error'));
            }
        });
    }

    private static async drawGraphPerDomaine($scope, student, options) {
        return new Promise(async (resolve, reject) => {
            try {
                let object = new ElementBilanPeriodique(new Classe({
                        id: student.idClasse,
                        type_groupe: Classe.type.CLASSE
                    }),
                    student, null, $scope.structure, null);

                await object.getDataForGraph(student, true);
                let canvas = <HTMLCanvasElement> document.getElementById("myChart"+student.id);
                let ctx = canvas.getContext('2d');
                let myChart = new Chart(ctx, {
                    type: 'bar',
                    data: {
                        labels: student.configMixedChartDomaine.labels,
                        datasets: [
                            student.configMixedChartDomaine.averageStudent,
                            student.configMixedChartDomaine.averageClass,
                            {
                                label: $scope.niveauCompetences[3].libelle,
                                backgroundColor: $scope.niveauCompetences[3].couleur,
                                stack: 'Stack 0',
                                data: student.configMixedChartDomaine.datasets.data_set1,
                            }, {
                                label: $scope.niveauCompetences[2].libelle,
                                backgroundColor: $scope.niveauCompetences[2].couleur,
                                stack: 'Stack 0',
                                data: student.configMixedChartDomaine.datasets.data_set2,
                            }, {
                                label: $scope.niveauCompetences[1].libelle,
                                backgroundColor: $scope.niveauCompetences[1].couleur,
                                stack: 'Stack 0',
                                data: student.configMixedChartDomaine.datasets.data_set3,
                            }, {
                                label: $scope.niveauCompetences[0].libelle,
                                backgroundColor: $scope.niveauCompetences[0].couleur,
                                stack: 'Stack 0',
                                data: student.configMixedChartDomaine.datasets.data_set4,
                            }
                        ]

                    },
                    options: {
                        animation: { duration: 0 },
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
                        layout: {
                            padding: {
                                left: 100,
                                right: 0,
                                top: 100,
                                bottom: 50
                            }
                        },
                        responsive: false,
                        scales: {
                            xAxes: [{
                                stacked: true,
                                gridLines: {
                                    display: false
                                }
                            }],
                            yAxes: [{
                                stacked: false,
                                position: "left",
                                scaleLabel: {
                                    display: true,
                                    labelString: student.configMixedChartDomaine.labelyAxes[0]
                                }
                            }]
                        }
                    }

                });
                let image = myChart.toBase64Image();
                options.images[student.id] = image;
                resolve();
            }
            catch (e) {
                reject (e);
            }
        });
    }
    private static async createCanvas(options, $scope){
        return new Promise(async (resolve, reject) => {
            let students = options.students;

            try {

                // on lance la creation des images des graphes et on attend
                let allPromise = [];
                _.forEach( students, (student) => {

                    $('.chart-container')
                        .append('' +
                            '<canvas ' +
                            '   id="myChart' + student.id + '"' +
                            '   width="700" height="218" ' +
                            '   class="chart-bar">' +
                            '</canvas>');

                    allPromise.push( Promise.all( [this.drawGraphPerDomaine($scope, student, options)]));
                });

                await  Promise.all(allPromise);
                resolve();
            }
            catch (e) {
                console.log(e);
                reject(e);
            }
        });
    }

    public static async setImageStructure (idStructure, path) {
        try {
            let data = {idStructure: idStructure, path: path};
            await http.post(`/competences/image/bulletins/structure`, data);
        }

        catch (e) {
            console.log(e);
        }
    }

    public static async setInformationsCE (idStructure, path, name) {
        try {
            let data = {idStructure: idStructure,
                path: (path !== undefined)? path : "/img/illustrations/image-default.svg",
                name: (name !== undefined)? name : "" };
            await http.post(`/competences/informations/bulletins/ce`, data);
        }

        catch (e) {
            console.log(e);
        }
    }

    public static async getInfosStructure (idStructure) {
        try {
            let dataStructure = await http.get(`/competences/images/and/infos/bulletins/structure/${idStructure}`);
            return dataStructure;
        }

        catch (e) {
            console.log(e);
        }
    }

}