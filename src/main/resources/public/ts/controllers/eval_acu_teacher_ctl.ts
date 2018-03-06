import { ng, moment, _ } from 'entcore';
import { evaluations } from '../models/teacher';
import * as utils from '../utils/teacher';

export let evalAcuTeacherController = ng.controller('EvalAcuTeacherController', [
    '$scope', 'route', 'model', '$rootScope',
    function ($scope, route, model, $rootScope) {
        // // Méthode d'initialisation ou de réinitialisation du Controler : notamment lors du changement d'établissement
        // $scope.initPeriodesList = (Index?: number,annee?:boolean) => {
        //     $scope.periodesList = {
        //         "type": "select",
        //         "name": "Service",
        //         "value":  $scope.periodeParDefault(),
        //         "values": []
        //     };
        //     if(Index || Index==0) {
        //         _.map($scope.classes.all[Index].periode, function (per) {
        //             $scope.periodesList.values.push(per);
        //         });
        //     }
        //     if(annee !== false) {
        //         $scope.periodesList.values.push({libelle: $scope.translate('viescolaire.utils.annee'), id: undefined});
        //     }
        //
        // };
        $scope.initControler = function () {
            // $scope.initPeriodesList();
            $scope.evaluations = evaluations;
            // $scope.search = {
            //     matiere: '*',
            //     periode: '*',
            //     enseignant: '*',
            //     classe: '*',
            //     sousmatiere: '*',
            //     type: '*',
            //     idEleve: '*',
            //     name: ''
            // };
            // $scope.displayPeriode = false;
            $scope.chartOptions = {
                classes: {},
                options: {
                    tooltips: {
                        callbacks: {
                            label: function (tooltipItems, data) {
                                return tooltipItems.yLabel + "%";
                            }
                        }
                    },
                    scales: {
                        yAxes: [{
                            ticks: {
                                size: 0,
                                max: 100,
                                min: 0,
                                stepSize: 20,
                            },
                        }],
                        xAxes: [{
                            display: false,
                        }]
                    }
                },
                colors: ['#4bafd5', '#46bfaf', '#ecbe30', '#FF8500', '#e13a3a', '#b930a2', '#763294', '#1a22a2']
            };
            $scope.showAutocomplete = false;
            $scope.devoirsNotDone = [];
            $scope.devoirsClasses = [];

            // $scope.periodes = evaluations.periodes;

            // Récupération des structures
            $scope.structures = evaluations.structures;
            $scope.usePerso = evaluations.structure.usePerso;

            // $scope.getDefaultPeriode = function () {
            //     return utils.getDefaultPeriode($scope.periodes.all);
            // };

            $scope.getDevoirsNotDone = function (idDevoirs?) {
                return new Promise((resolve, reject) => {
                    let calcPercent = () => {
                        if (!idDevoirs) {
                            idDevoirs = _.pluck(_.filter($scope.devoirs.all, (devoir) => {
                                return _.contains(_.pluck($scope.classes.all, 'id'),
                                        devoir.id_groupe);
                            }), 'id');
                        }
                        resolve($scope.devoirs.filter((devoir) => {
                            return (devoir.percent < 100 && _.contains(idDevoirs, devoir.id));
                         }));
                    };
                    if (!evaluations.structure.synchronized.devoirs) {
                        evaluations.structure.devoirs.one('sync', function () {
                            calcPercent();
                        });
                    } else {
                        calcPercent();
                    }

                });
            };

            $scope.getCurrentDevoirsNotDone = function () {
                $scope.getDevoirsNotDone().then(async (devoirs) => {
                    $scope.currentDevoirsNotDone = [];
                    for(var d = 0; d < devoirs.length; d++){
                        let classe = _.findWhere($scope.structure.classes.all, {id: devoirs[d].id_groupe});
                        let current_periode = await $scope.getCurrentPeriode(classe);
                        if(current_periode === -1 || current_periode.id_type === devoirs[d].id_periode){
                            $scope.currentDevoirsNotDone.push($scope.devoirsNotDone[d]);
                        }
                    }
                    utils.safeApply($scope);
                });
            }

            $scope.initChartListNotDone = function () {
                $scope.getDevoirsNotDone().then((devoirs) => {
                    $scope.devoirsNotDone = devoirs;
                    $scope.devoirsClasses = _.filter(evaluations.structure.classes.all, (classe) => {
                        return _.contains(_.uniq(_.pluck($scope.devoirsNotDone, 'id_groupe')), classe.id) && classe.remplacement !== true;
                    });
                    if ($scope.devoirsClasses.length > 0 ) {
                        $scope.chartOptions.selectedClasse = _.first(_.sortBy($scope.devoirsClasses, 'name')).id;
                        $scope.loadChart($scope.chartOptions.selectedClasse);
                    }
                    utils.safeApply($scope);
                });
            };
        };

        // Initialisation du Controler
        if (evaluations.structure !== undefined) {
            $scope.initControler(false);
        }else {
            console.log("Aucun établissement actif pour l'utilisateur");
        }
        // $scope.FilterPeriode = (Maperiode) => {
        //     if($scope.search.classe !== '' && $scope.search.classe !== '*' && typeof($scope.search.classe) == "object") {
        //         if(Maperiode.id_classe == $scope.search.classe.id ) {
        //             return Maperiode ;
        //         }
        //     }else {
        //         return ;
        //     }
        // };
        // $scope.displayPeriode = false;
        // $scope.periodeDisplay = (classe,annee) => {
        //     if(typeof(classe) == 'object'&& classe !== null) {
        //         if(classe.type_groupe == 0) {
        //             let indexClasse = _.indexOf($scope.classes.all,classe);
        //             if(!('periode' in classe && classe.periode !== null && classe.periode !== undefined)) {
        //                 $scope.classes.all[indexClasse].periode = _.where($scope.evaluations.structure.periodes.all, {id_classe: $scope.classes.all[indexClasse].id});
        //             }
        //             $scope.initPeriodesList(indexClasse,annee);
        //             $scope.displayPeriode = true ;
        //             utils.safeApply($scope);
        //         }else{
        //             let indexClasse = _.indexOf($scope.classes.all,classe);
        //             if('periode' in classe && classe.periode !== null && classe.periode !== undefined) {
        //                 $scope.initPeriodesList(indexClasse,annee);
        //                 $scope.displayPeriode = true ;
        //                 utils.safeApply($scope);
        //             }else{
        //                 $scope.classes.all[indexClasse].getGroupePeriode().then((res)=>{
        //                     if (! (res == undefined)) {
        //                         $scope.initPeriodesList(indexClasse,annee);
        //                         $scope.displayPeriode = true ;
        //                         utils.safeApply($scope);
        //                     }else{
        //                         $scope.initPeriodesList();
        //                         $scope.displayPeriode = false ;
        //                         utils.safeApply($scope);
        //                     }
        //                 })
        //             }
        //         }
        //     }else {
        //         $scope.displayPeriode = false ;
        //         utils.safeApply($scope);
        //     }
        //
        // };

        $scope.loadChart = function (idClasse) {
            let idDevoirs = _.pluck(_.where($scope.devoirsNotDone, {id_groupe: idClasse}), 'id');
            $scope.getDevoirsNotDone(idDevoirs).then((devoirs) => {
                if (devoirs) {
                    $scope.chartOptions.classes[idClasse] = {
                        names: _.pluck(devoirs, 'name'),
                        percents: _.pluck(devoirs, 'percent'),
                        id: _.pluck(devoirs, 'id')
                    };
                } else {
                    $scope.chartOptions.classes[idClasse] = {
                        names: [],
                        percents: [],
                        id: []
                    };
                }
                utils.safeApply($scope);
            });
        };

        /**
         * ouvrir le suivi d'un eleve (utilisé dans la barre de recherche)
         * @param Eleve
         */
        $scope.openSuiviEleve = (Eleve) => {
            let path = '/competences/eleve';
            let idOfpath = {idEleve : Eleve.id, idClasse: Eleve.idClasse};
            $scope.goTo(path, idOfpath);
        };

        $scope.changeEtablissementAccueil =  () => {
            let switchEtab = async () => {
                await $scope.initControler();
                await $scope.$parent.initReferences();
                $scope.search = $scope.initSearch();
                $scope.devoirs = evaluations.structure.devoirs;
                $scope.usePerso = evaluations.structure.usePerso;
                $scope.classes = evaluations.structure.classes;
                $scope.initChartListNotDone();
                $scope.getCurrentDevoirsNotDone();
                utils.safeApply($scope);
            };
            if (!evaluations.structure.isSynchronized) {
                $scope.$parent.opened.displayStructureLoader = true;
                evaluations.structure.sync().then(() => {
                    switchEtab();
                    $scope.$parent.opened.displayStructureLoader = false;
                });
            } else {
                switchEtab();
            }
        };

        /**
         * ouvrir la page de création devoir
         */
        $scope.openCreateEval = () => {
            let path = '/devoir/create';
            $scope.goTo(path);
        };
        $scope.FilterGroupEmpty = (item) => {
            let nameofclasse = $scope.getClasseData(item.id_groupe, 'name');
            if ( item.id_groupe !== '' && nameofclasse !== undefined && nameofclasse !== '') {
                return item;
            }
        };

        evaluations.devoirs.on('sync', function () {
            $scope.initChartListNotDone();
            $scope.getCurrentDevoirsNotDone();
        });

        if (evaluations.structure.isSynchronized) {
            $scope.initChartListNotDone();
            $scope.getCurrentDevoirsNotDone();
            $scope.initSearch();
        }

        // permet de basculer sur l' écran de saisie de note en cliquant sur le diagramme
        $scope.SaisieNote = (points, evt) => {
            if ( points.length > 0 && points !== undefined ) {
                let path = '/devoir/' +
                    $scope.chartOptions.classes[$scope.chartOptions.selectedClasse].id[points[0]._index];
                $scope.goTo(path);
            }

        };

    }
]);