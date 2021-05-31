/**
 * Created by anabah on 10/10/2018.
 */
import {_, notify, idiom as lang} from 'entcore';
import http from "axios";
import {Classe, ElementBilanPeriodique, Utils} from "../teacher";
import {Stopwatch} from "./StopWatch";

declare let $ : any;
declare let Chart: any;
declare let TextDecoder : any;

export class ExportBulletins {

    toJSON (options) {
        let o = {
            idStudents: options.idStudents,
            getResponsable: (options.getResponsable === true),
            getProgramElements: (options.getProgramElements === true) ,
            moyenneClasse: (options.moyenneClasse === true),
            moyenneEleve: (options.moyenneEleve === true),
            studentRank: (options.studentRank === true) ,
            classAverageMinMax: (options.classAverageMinMax === true),
            positionnement: (options.positionnement === true) ,
            moyenneClasseSousMat: (options.moyenneClasseSousMat === true),
            moyenneEleveSousMat: (options.moyenneEleveSousMat === true),
            moyenneGenerale: (options.moyenneGenerale === true),
            moyenneAnnuelle: (options.moyenneAnnuelle === true),
            coefficient: (options.coefficient === true),
            positionnementSousMat: (options.positionnementSousMat === true),
            showBilanPerDomaines: (options.showBilanPerDomaines === true),
            showFamily: (options.showFamily === true),
            showProjects: (options.showProjects === true),
            threeLevel: (options.threeLevel === true) ,
            threeMoyenneClasse: (options.threeMoyenneClasse === true) ,
            threeMoyenneEleve: (options.threeMoyenneEleve === true) ,
            threePage: (options.threePage === true) ,
            classeName: options.classeName,
            idClasse: options.idClasse,
            idStructure: options.idStructure,
            pdfBlobs: options.pdfBlobs,
            images: options.images,
            nameCE: (options.nameCE !== undefined)? options.nameCE : "",
            imgStructure: (options.imgStructure !== undefined)? options.imgStructure : "",
            hasImgStructure: (options.imgStructure !== undefined),
            imgSignature: (options.imgSignature !== undefined)? options.imgSignature : "",
            hasImgSignature: (options.imgSignature !== undefined),
            useModel : (options.useModel === true),
            simple : (options.simple === true),
            neutre: (options.neutre === true),
            niveauCompetences: options.niveauCompetences,
            withLevelsStudent : options.withLevelsStudent,
            withLevelsClass : options.withLevelsClass,
            mentionOpinion: options.mentionOpinion,
            orientationOpinion: options.orientationOpinion,
            agricultureLogo: options.agricultureLogo,
            hideHeadTeacher: options.hideHeadTeacher,
            addOtherTeacher: options.addOtherTeacher,
            functionOtherTeacher: options.functionOtherTeacher,
            otherTeacherName: options.otherTeacherName
        };
        if (Utils.isNotNull(options.idPeriode)){
            _.extend(o, {idPeriode: options.idPeriode});
        }
        if (Utils.isNotNull(options.idStructure)){
            _.extend(o, {idStructure: options.idStructure});
        }
        if (Utils.isNotNull(options.type)){
            _.extend(o, {typePeriode: options.type});
        }else{
            _.extend(o, {typePeriode: options.typePeriode});
        }
        if(o.showBilanPerDomaines) {
            _.extend(o, {idImagesFiles : options.idImagesFiles});
        }
        if(o.useModel === true) {
            _.extend(o, {idModel : options.idModel});
        }
        _.extend(o, {printSousMatieres :
                (o.moyenneClasseSousMat || o.moyenneEleveSousMat || o.positionnementSousMat)});
        if(!o.moyenneClasse && !o.moyenneEleve && !o.positionnement){
            o['printSousMatieres'] = false;
        }
        return o;
    }

    private static startDebug ($scope, options, method) {
        let stopwatch = undefined;
        if ($scope.debug === true) {
            stopwatch = new Stopwatch(
                document.querySelector('.stopwatch'),
                document.querySelector('.results'));

            stopwatch.start();
            console.log(" DEBUT " + method + " ====== " + options.classeName);
        }
        return stopwatch;
    }

    private static  stopDebug (stopwatch, $scope, options, method) {
        if ($scope.debug === true) {
            if (stopwatch !== undefined) {
                stopwatch.stop();
                console.log(" FIN " + method + " ====== " + options.classeName + " " +
                    stopwatch.format(stopwatch.times));
            }
        }
    }

    private static manageError(data, $scope){
        if(data instanceof ArrayBuffer && data.byteLength !== 0) {
            let obj: string;
            let decodedString: any;
            if ('TextDecoder' in window) {
                let dataView = new DataView(data);
                decodedString = new TextDecoder('utf8');
                obj = JSON.parse(decodedString.decode(dataView));
            } else {
                decodedString = String.fromCharCode.apply(null, new Uint8Array(data));
                obj = JSON.parse(decodedString);
            }
            $scope.opened.coefficientConflict = true;
            if(Utils.isNull($scope.error.eleves)){
                $scope.error.eleves = obj['eleves'];
            }
            else{
                $scope.error.eleves = _.union($scope.error.eleves, obj['eleves']);
            }
        }
    }
    public static async generateBulletins (options, $scope) {
        let method = "generateBulletins";
        let stopwatch = this.startDebug( $scope, options, method);
        $('.chart-container').empty();
        try {
            options.images = {}; // contiendra les id des images par élève
            options.idImagesFiles = []; // contiendra tous les ids d'images à supprimer après l'export

            if (options.showBilanPerDomaines === true && options.simple !== true) {
                // Si on choisit de déssiner les graphes
                await this.createCanvas(options, $scope);
            }
            options.niveauCompetences = $scope.niveauCompetences;

            // lancement de l'export et récupération du fichier généré
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
            notify.success(options.classeName + ' : ' + lang.translate('evaluations.export.bulletin.success'));
            this.stopDebug(stopwatch, $scope, options, method);
            if(data.status == 200)
                await http.post(`/competences/save/bulletin/parameters`, new ExportBulletins().toJSON(options));
        } catch (data) {
            console.error(data);
            if(data.response != undefined && data.response.status === 500){
                this.manageError(data.response.data, $scope);
            }
            notify.error(options.classeName + ' : ' + lang.translate('evaluations.export.bulletin.error'));
            this.stopDebug(stopwatch, $scope, options, method);
        }
    }

    // - Récupère les informations nécessaires à la construction du graphe d'un élève
    // - construit le graph en Front
    // - upload l'image obtenue dans le FileSystem
    // - stocke l'id de l'image dans options.images: Map<idStudent, idFile> et options.idImagesFiles: array[idFiles].

    private static async drawGraphPerDomaine($scope, student, options) {
        return new Promise(async (resolve, reject) => {
            try {
                let object = new ElementBilanPeriodique(new Classe({
                        id: student.idClasse,
                        type_groupe: Classe.type.CLASSE
                    }),
                    student, options.idPeriode, $scope.structure, options.idPeriode);
                await object.getDataForGraph(student, true);
                let datasetsArray = [];
                if(options.withLevelsStudent){
                    datasetsArray.push(student.configMixedChartDomaine.averageStudent,);
                }
                if(options.withLevelsClass){
                    datasetsArray.push(student.configMixedChartDomaine.averageClass,);
                }
                datasetsArray.push({
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
                });


                let canvas = <HTMLCanvasElement> document.getElementById("myChart"+student.id);
                let ctx = canvas.getContext('2d');
                let myChart = new Chart(ctx, {
                    type: 'bar',
                    data: {
                        labels: student.configMixedChartDomaine.labels,
                        datasets: datasetsArray
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
                                left: 0,
                                right: 0,
                                top: 0,
                                bottom: 20
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
                            },
                                {
                                    stacked: false,
                                    position: "right",
                                    id: "y-axis-1",
                                    scaleLabel: {
                                        display: false,
                                        labelString: student.configMixedChartDomaine.labelyAxes[0]
                                    }
                                    ,
                                    gridLines: {
                                        display:false
                                    },
                                    ticks: {
                                        callback: function() {
                                            return ' ';
                                        }
                                    }
                                }
                            ]
                        }
                    }

                });
                let image = myChart.toBase64Image();
                let blob = new Blob([image], {type: 'image/png'});

                let formData = new FormData();
                formData.append('file', blob);


                let response = await http.post('/competences/graph/img', formData);

                options.images[student.id] = response.data._id;
                options.idImagesFiles.push(response.data._id);

                resolve();
            }
            catch (e) {
                reject (e);
            }
        });
    }


    public static async createCanvas(options, $scope){
        return new Promise(async (resolve, reject) => {
            let students = options.students;
            let method = "DESSIN GRAPH PAR DOMAINE";
            let stopwatch = ExportBulletins.startDebug($scope, options, method);
            try {

                // on lance la creation des images des graphes et on attend

                let allPromise = [];
                _.forEach( students, (student) => {

                    $('.chart-container')
                        .append('' +
                            '<canvas ' +
                            '   id="myChart' + student.id + '"' +
                            '   width="1267" height="400" ' +
                            '   class="chart-bar">' +
                            '</canvas>');

                    allPromise.push(this.drawGraphPerDomaine($scope, student, options));
                });

                await  Promise.all(allPromise);
                this.stopDebug(stopwatch, $scope, options, method);
                resolve();
            }
            catch (e) {
                console.error(e);
                this.stopDebug(stopwatch, $scope, options, method);
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
            console.error(e);
        }
    }

    public static async setInformationsCE(idStructure, path, name) {
        try {
            let data = {idStructure: idStructure,
                path: (path !== undefined) ? path : "/img/illustrations/image-default.svg",
                name: (name !== undefined) ? name : "" };
            await http.post(`/competences/informations/bulletins/ce`, data);
        }

        catch (e) {
            console.error(e);
        }
    }

    public static async getInfosStructure (idStructure) {
        try {
            return await http.get(`/competences/images/and/infos/bulletins/structure/${idStructure}`);
        }

        catch (e) {
            console.error(e);
        }
    }

}