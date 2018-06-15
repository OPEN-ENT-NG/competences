/**
 * Created by ledunoiss on 27/10/2016.
 */
import {ng, template, model, moment} from "entcore";
import {
    SuiviCompetence, Devoir, CompetenceNote, evaluations, Structure, Classe, Eleve,
    Domaine,
} from "../models/teacher";
import * as utils from "../utils/teacher";

import {Defaultcolors} from "../models/eval_niveau_comp";
import {NiveauLangueCultReg, NiveauLangueCultRegs,BaremeBrevetEleve} from "../models/teacher/index";
import {Utils} from "../models/teacher/Utils";
import {Mix} from "entcore-toolkit";



declare let _: any;
declare let Chart: any;
declare let location: any;

export let evalSuiviCompetenceEleveCtl = ng.controller('EvalSuiviCompetenceEleveCtl', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$route', '$timeout',
    function ($scope, route, $rootScope, $location, $filter, $route, $timeout) {

        // $scope.initPeriodesList = (Index?: number,annee?:boolean) => {
        //     $scope.periodesList = {
        //         "type": "select",
        //         "name": "Service",
        //         "value":  $scope.periodeParDefault(),
        //         "values": []
        //     };
        //     if(Index || Index==0){
        //         _.map($scope.classes.all[Index].periode, function (per) {
        //             $scope.periodesList.values.push(per);
        //         });
        //     }
        //     if(annee !== false){
        //         $scope.periodesList.values.push({libelle: $scope.translate('viescolaire.utils.annee'), id: undefined});
        //     }
        //
        // };
        // $scope.initPeriodesList();
        //rajout de la periode Annee

        template.open('container', 'layouts/2_10_layout');
        template.open('left-side', 'enseignants/suivi_competences_eleve/left_side');
        template.open('content', 'enseignants/suivi_competences_eleve/content');
        template.open('suivi-competence-content', 'enseignants/suivi_competences_eleve/content_vue_suivi_eleve');


        // $scope.displayPeriode = false;
        // $scope.periodeDisplay = (classe,annee) => {
        //     if(typeof(classe) == 'object'&& classe !== null){
        //         if(classe.type_groupe == 0){
        //             let indexClasse = _.indexOf($scope.classes.all,classe);
        //             if(!('periode' in classe && classe.periode !== null && classe.periode !== undefined)){
        //                 $scope.classes.all[indexClasse].periode = _.where($scope.evaluations.structure.periodes.all, {id_classe: $scope.classes.all[indexClasse].id});
        //             }
        //             $scope.initPeriodesList(indexClasse,annee);
        //             $scope.displayPeriode = true ;
        //             utils.safeApply($scope);
        //         }else{
        //             let indexClasse = _.indexOf($scope.classes.all,classe);
        //             if('periode' in classe && classe.periode !== null && classe.periode !== undefined){
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
        $scope.route = $route;

        $scope.opened.lightboxEvalLibre = false;



        /**
         * show label too long
         */
            // create the timer variable
        var timer;

        // mouseenter event
        $scope.showIt = (item) => {
            timer = $timeout(function () {
                item.hovering = true;
            }, 350);
        };

        // mouseleave event
        $scope.hideIt =  (item) => {
            $timeout.cancel(timer);
            item.hovering = false;
        };

        /**
         * Initialise d'une évaluation libre.
         */
        $scope.initEvaluationLibre = () => {
            let today = new Date();
            let evaluationLibre = new Devoir({
                date_publication: today,
                date: today,
                diviseur: 20,
                coefficient: 1,
                id_etablissement: $scope.evaluations.structure.id,
                ramener_sur: false,
                id_etat: 1,
                owner: model.me.userId,
                is_evaluated: false,
                id_classe: null,
                id_periode: $scope.search.periode.id_type,
                id_type: 1, // TODO modifier en optional foreign key
                id_matiere: "",
                id_sousmatiere: null,
                competences: [],
                controlledDate: true,
                matieres: [_.findWhere(evaluations.matieres.all, {idEtablissement: $scope.evaluations.structure.id})],
                sousmatiere: [],
            });

            let competenceEvaluee = new CompetenceNote({
                evaluation: -1,
                id_competence: $scope.detailCompetence.id,
                id_eleve: $scope.informations.eleve.id,
                owner: model.me.userId
            });
            evaluationLibre.competences.all.push($scope.detailCompetence.id);
            evaluationLibre.competenceEvaluee = competenceEvaluee;

            return evaluationLibre;
        };

        $scope.$watch(function () {
            if ($scope.evaluationLibre != undefined)
                return $scope.evaluationLibre.id_matiere;
        }, function (newValue) {
            if (newValue !== "" && newValue !== undefined && newValue !== null) {
                let mamatiere = _.findWhere($scope.evaluationLibre.matieres, {id: $scope.evaluationLibre.id_matiere});
                if (mamatiere != undefined)
                    $scope.evaluationLibre.sousmatiere = mamatiere.sousMatieres.all;
            } else if (newValue === null) {
                $scope.evaluationLibre.sousmatiere = []
            }
        });

        /**
         * Ouvre la fenêtre de création d'une évaluation libre
         */
        $scope.createEvaluationLibre = async () => {
            $scope.messages.successEvalLibre = false;
            $scope.evaluationLibre = $scope.initEvaluationLibre();

            if ($scope.search.classe && $scope.search.classe.periodes && $scope.search.classe.periodes.length() == 0) {
                await $scope.search.classe.periodes.sync();
            }
            $scope.evaluationLibre.periodes = $scope.search.classe.periodes.all;

            $scope.controleDate();
            $scope.opened.lightboxEvalLibre = true;
            template.open('lightboxContainerEvalLibre', 'enseignants/creation_devoir/display_creation_eval_libre');
        };

        /**
         * Evaluation de la compétence sur laquelle on est lors d'une évaluation libre
         */
        $scope.switchColor = function () {
            // recupération de la compétence (il n'y en a qu'une)
            var competenceEvaluee = $scope.evaluationLibre.competenceEvaluee;
            let niveauCompetenceMax = -1;
            for (let o in $scope.mapCouleurs) {
                niveauCompetenceMax++;
            }

            if (competenceEvaluee.evaluation === -1) {
                competenceEvaluee.evaluation = niveauCompetenceMax - 1;
            } else {
                competenceEvaluee.evaluation = competenceEvaluee.evaluation - 1;
            }
        };

        /**
         *  Sauvegarde d'une évaluation libre
         */
        $scope.saveNewEvaluationLibre = function () {
            $scope.evaluationLibre.date = $scope.getDateFormated($scope.evaluationLibre.dateDevoir);
            $scope.evaluationLibre.date_publication = $scope.getDateFormated($scope.evaluationLibre.datePublication);

            // fermeture popup
            $scope.opened.lightboxEvalLibre = false;

            // message de succes
            $scope.messages.successEvalLibre = true;
            $scope.evaluationLibre.create().then(function (res) {

                // refresh du suivi élève
                $scope.suiviCompetence = new SuiviCompetence($scope.search.eleve, $scope.search.periode, $scope.search.classe, $scope.evaluations.structure);
                $scope.suiviCompetence.sync().then(() => {
                    $scope.suiviCompetence.domaines.sync().then(() => {
                        $scope.suiviCompetence.setMoyenneCompetences($scope.suiviFilter.mine);
                        $scope.detailCompetence = $scope.suiviCompetence.findCompetence($scope.detailCompetence.id);
                        $scope.initChartsEval();

                        utils.safeApply($scope);
                    });
                });
                $scope.initSliderBFC();
                utils.safeApply($scope);

            });
        };

        // /**
        //  * Controle que la date de publication du devoir n'est pas inférieur à la date du devoir.
        //  * Et que la date de creation est comprise dans la période
        //  */
        $scope.controleDate = async () => {
            let idClasse = _.findWhere($scope.structure.eleves.all, {id: $scope.evaluationLibre.competenceEvaluee.id_eleve}).idClasse;
            let classe = _.findWhere($scope.structure.classes.all, {id: idClasse});
            if (classe.periodes.empty()) {
                await classe.periodes.sync();
            }
            let current_periode = _.findWhere(classe.periodes.all, {id_type: $scope.evaluationLibre.id_periode});

            let start_datePeriode = current_periode.timestamp_dt;
            let end_datePeriode = current_periode.timestamp_fn;
            let date_saisie = current_periode.date_fin_saisie;

            $scope.errDatePubliEvalFree = (moment($scope.evaluationLibre.datePublication).diff(moment($scope.evaluationLibre.dateDevoir), "days") < 0);
            $scope.errDateEvalFree = !(moment($scope.evaluationLibre.dateDevoir).isBetween(moment(start_datePeriode), moment(end_datePeriode), 'days', '[]'));
            $scope.endSaisieFree = moment($scope.evaluationLibre.dateDevoir).isAfter(moment(date_saisie), 'days', '[') || moment(new Date()).isAfter(moment(date_saisie), 'days', '[');

            $scope.evaluationLibre.controlledDate = !$scope.errDatePubliEvalFree && !$scope.errDateEvalFree && !$scope.endSaisieFree;
            utils.safeApply($scope);
        };

        /**
         * Controle la validité du formulaire de création d'une évaluation libre.
         * @returns {boolean} Validité du formulaire
         */
        $scope.controleNewEvaluationLibreForm = function () {
            return $scope.evaluationLibre == undefined || !(
                $scope.evaluationLibre.controlledDate
                && $scope.evaluationLibre.name !== undefined
                && $scope.evaluationLibre.id_periode !== null && $scope.evaluationLibre.id_periode !== undefined
                && $scope.evaluationLibre.competenceEvaluee.evaluation !== -1
            );
        };

        $scope.initFilterMine = () => {
            $scope.suiviFilter = {
                mine: (!Utils.isChefEtab()).toString()
            };
        };

        $scope.initFilterMine();

        $scope.opened.detailCompetenceSuivi = false;
        $scope.refreshSlider = function () {
            $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
            });
        };

        /**
         * Supprime un BFC créé par un chef d'établissement
         */
        $scope.deleteBFC = function () {
            this.domaine.bfc.deleteBilanFinDeCycle().then((res) => {
                if (res.rows === 1) {
                    this.domaine.bfc = undefined;
                    // Récupération de la moyenne convertie
                    let maConvertion = utils.getMoyenneForBFC(this.domaine.moyenne, $scope.suiviCompetence.tableConversions.all);
                    this.domaine.slider.value = maConvertion;
                }
                utils.safeApply($scope);
            });
        };

        $scope.switchEtablissementSuivi = () => {
            delete $scope.suiviCompetence;
            $scope.changeEtablissement();
        };
        $scope.updateColorAndLetterForSkills = function () {
            let niveauCompetence = _.findWhere(evaluations.structure.cycles, {
                id_cycle: $scope.search.classe.id_cycle
            });
            if (niveauCompetence !== undefined) {
                niveauCompetence = niveauCompetence.niveauCompetencesArray;
            }
            else {
                niveauCompetence = evaluations.structure.cycles[0].niveauCompetencesArray;
            }
            $scope.mapCouleurs = {"-1": Defaultcolors.unevaluated};
            $scope.mapLettres = {"-1": " "};
            _.forEach(niveauCompetence, function (niv) {
                $scope.mapCouleurs[niv.ordre - 1] = niv.couleur;
                $scope.mapLettres[niv.ordre - 1] = niv.lettre;
            });
            $scope.initChartsEval();
            utils.safeApply($scope);
        };

        $scope.updateNiveau = function (usePerso) {
            if (usePerso === 'true') {
                evaluations.structure.niveauCompetences.sync(false).then(() => {
                    evaluations.structure.niveauCompetences.first().markUser().then(() => {
                        $scope.structure.usePerso = 'true';
                        $scope.updateColorAndLetterForSkills();
                        utils.safeApply($scope);
                    });
                });

            }
            else if (usePerso === 'false') {
                evaluations.structure.niveauCompetences.sync(true).then(() => {
                    evaluations.structure.niveauCompetences.first().unMarkUser().then(() => {
                        $scope.structure.usePerso = 'false';
                        $scope.updateColorAndLetterForSkills();
                        utils.safeApply($scope);
                    });
                });
            }

        };

        /**
         * Créer un suivi de compétence
         */
        $scope.selectSuivi = async function (classeHasChange) {
            if(classeHasChange === true){
                await $scope.syncPeriode($scope.search.classe.id);
            }
            $scope.selected.grey = true;
            if ($scope.search.classe.id_cycle === null) {
                return;
            }
            if ($scope.search.classe.eleves.empty) {
                await $scope.search.classe.eleves.sync();
            }
            if ($scope.search.eleve !== undefined &&
                $scope.search.classe.eleves.findWhere({id: $scope.search.eleve.id}) === undefined) {
                $scope.search.eleve = "";
                delete $scope.suiviCompetence;
                return;
            }


            $scope.informations.eleve = $scope.search.eleve;
            if ($scope.informations.eleve !== null && $scope.search.eleve !== ""
                && $scope.informations.eleve !== undefined) {

                // Récupérer le suivi de l'élève
                let eleveIsEvaluable = $scope.search.eleve.isEvaluable($scope.search.periode);
                if (eleveIsEvaluable) {
                    $scope.suiviCompetence = new SuiviCompetence($scope.search.eleve,
                        $scope.search.periode, $scope.search.classe, $scope.evaluations.structure);
                    let niveauCompetence = _.findWhere(evaluations.structure.cycles, {
                        id_cycle: $scope.search.classe.id_cycle
                    });
                    if (niveauCompetence !== undefined) {
                        niveauCompetence = niveauCompetence.niveauCompetencesArray;
                    }
                    else {
                        niveauCompetence = evaluations.structure.cycles[0].niveauCompetencesArray;
                    }
                    $scope.mapCouleurs = {"-1": Defaultcolors.unevaluated};
                    $scope.mapLettres = {"-1": " "};
                    _.forEach(niveauCompetence, function (niv) {
                        $scope.mapCouleurs[niv.ordre - 1] = niv.couleur;
                        $scope.mapLettres[niv.ordre - 1] = niv.lettre;
                    });

                    //Enseignement de complement cycle 4
                    $scope.suiviCompetence.niveauLangueCultRegs = new NiveauLangueCultRegs();
                    $scope.suiviCompetence.ensCpls.sync().then(() => {
                        $scope.suiviCompetence.niveauEnsCpls.sync().then(() => {
                            $scope.suiviCompetence.eleveEnsCpl.sync().then(() => {
                                $scope.suiviCompetence.langues.sync().then(() => {
                                    $scope.showButtonSave = true;
                                    if ($scope.suiviCompetence.eleveEnsCpl.id) {
                                        $scope.suiviCompetence.ensCplSelected = _.findWhere($scope.suiviCompetence.ensCpls.all, {id: $scope.suiviCompetence.eleveEnsCpl.id_enscpl});
                                        $scope.suiviCompetence.niveauEnsCplSelected = _.findWhere($scope.suiviCompetence.niveauEnsCpls.all, {id: $scope.suiviCompetence.eleveEnsCpl.id_niveau});
                                        // si il y a une langue régionale de precisée, on la sélectionne
                                        if ($scope.suiviCompetence.eleveEnsCpl.id_langue !== undefined) {
                                            $scope.suiviCompetence.langueSelected = _.findWhere($scope.suiviCompetence.langues.all, {id: $scope.suiviCompetence.eleveEnsCpl.id_langue});
                                            // sélection du niveau si renseigné
                                            if ($scope.suiviCompetence.eleveEnsCpl.niveau_lcr !== undefined) {
                                                $scope.suiviCompetence.niveauLangueCultRegSelected = _.findWhere($scope.suiviCompetence.niveauLangueCultRegs.all, {niveau: $scope.suiviCompetence.eleveEnsCpl.niveau_lcr});
                                            }
                                        }
                                        utils.safeApply($scope);
                                    } else {
                                        $scope.suiviCompetence.niveauEnsCplSelected = $scope.suiviCompetence.eleveEnsCpl;
                                        utils.safeApply($scope);
                                    }
                                });
                            });
                        });
                    });

                    $scope.onChangeEns = () => {
                        // réinit des listes déroulantes concernant les langues régionales
                        $scope.suiviCompetence.langueSelected = undefined;
                        $scope.suiviCompetence.niveauLangueCultRegSelected = undefined;
                        $scope.onChangeObjectif();
                        //si id=1 on est sur ensCpl Aucun
                        if ($scope.suiviCompetence.ensCplSelected.id === 1) {
                            // on met à jour le niveau à 0
                            $scope.suiviCompetence.niveauEnsCplSelected = _.findWhere($scope.suiviCompetence.niveauEnsCpls.all,
                                {niveau: $scope.suiviCompetence.eleveEnsCpl.niveau});
                        } else {
                            //sinon on positionne sur le 1er niveau par défaut
                            $scope.suiviCompetence.niveauEnsCplSelected = _.findWhere($scope.suiviCompetence.niveauEnsCpls.all,
                                {niveau: $scope.suiviCompetence.niveauEnsCpls.niveau = 1});

                            // si l'enseignement sélectionné est avec le code LCR, alors, on affiche la liste déroulante
                            // de choix de la langue de culture régionale et on positionne sur la 1ère langue par défaut
                            if ($scope.suiviCompetence.ensCplSelected.code === 'LCR') {
                                $scope.suiviCompetence.langueSelected = $scope.suiviCompetence.langues.all[0];
                            }

                        }
                    };
                    $scope.onChangeObjectif = () => {
                        ($scope.showButtonSave) ? $scope.showButtonSave = !$scope.showButtonSave :
                            $scope.showButtonSave = $scope.showButtonSave;
                    }

                    $scope.showSaveButton = () => {
                        let id_langue;
                        if ($scope.suiviCompetence.langueSelected !== undefined) {
                            id_langue = $scope.suiviCompetence.langueSelected.id;
                        }

                        let visible = $scope.suiviCompetence.ensCplSelected !== undefined &&
                            $scope.suiviCompetence.ensCplSelected.id !== undefined && // ense complement
                            $scope.suiviCompetence.niveauEnsCplSelected.niveau !== undefined && // avec un niveau
                            ($scope.suiviCompetence.langueSelected == undefined || // et pas de langue regionale
                                ($scope.suiviCompetence.langueSelected !== undefined && // ou un langue avec le code AUC mais sans niveau
                                    $scope.suiviCompetence.langueSelected.code === 'AUC') ||
                                ($scope.suiviCompetence.langueSelected !== undefined && // ou une langue avec un niveau
                                    $scope.suiviCompetence.niveauLangueCultRegSelected.niveau !== undefined)
                            );

                        return visible;
                    };


                    $scope.oncChangeLangue = () => {
                        $scope.onChangeObjectif();
                        if ($scope.suiviCompetence.langueSelected.code === 'AUC') {
                            // suppression du niveau
                            $scope.suiviCompetence.niveauLangueCultRegSelected = new NiveauLangueCultReg(0);
                        } else {
                            // sélection du 1er niveau par defaut
                            $scope.suiviCompetence.niveauLangueCultRegSelected = $scope.suiviCompetence.niveauLangueCultRegs.all[0];
                        }

                    };

                    $scope.suiviCompetence.sync().then(() => {
                        // On récupère d'abord les bilans de fin de cycle enregistrés par le chef d'établissement
                        //on récupère la période en cours en fonction du type car quand il n'y a pas de période sélectionnée on a un type de période
                        let idTypePeriode = ($scope.suiviCompetence.periode.id !== null) ? $scope.suiviCompetence.periode.id_type : null;
                        $scope.suiviCompetence.baremeBrevetEleves.sync($scope.suiviCompetence.classe.id, idTypePeriode).then(() => {
                            $scope.suiviCompetence.bilanFinDeCycles.all = [];
                            $scope.suiviCompetence.bilanFinDeCycles.sync().then(() => {
                                $scope.suiviCompetence.domaines.all = [];
                                $scope.suiviCompetence.domaines.sync().then(() => {
                                    $scope.suiviCompetence.baremeBrevetEleve = new BaremeBrevetEleve();
                                    $scope.suiviCompetence.baremeBrevetEleve = Mix.castAs(BaremeBrevetEleve, _.findWhere($scope.suiviCompetence.baremeBrevetEleves.all, {id_eleve: $scope.search.eleve.id}));
                                    $scope.suiviCompetence.setMoyenneCompetences($scope.suiviFilter.mine);
                                    if ($scope.opened.detailCompetenceSuivi) {
                                        if ($scope.detailCompetence !== undefined) {
                                            $scope.detailCompetence = $scope.suiviCompetence.findCompetence($scope.detailCompetence.id);
                                            if ($scope.detailCompetence) {
                                                let detail = $scope.template.containers['suivi-competence-detail'];
                                                if (detail !== undefined) {
                                                    detail = detail.split('.html?hash=')[0].split('template/')[1];
                                                }
                                                $scope.openDetailCompetence($scope.detailCompetence, detail);
                                            } else {
                                                $scope.backToSuivi();
                                            }
                                        } else $scope.backToSuivi();
                                    }
                                });
                            });

                        });
                        $scope.initSliderBFC();
                        $scope.informations.eleve.suiviCompetences.push($scope.suiviCompetence);
                        $scope.template.close('suivi-competence-content');
                        utils.safeApply($scope);
                        $scope.template.open('suivi-competence-content', 'enseignants/suivi_competences_eleve/content_vue_suivi_eleve');
                        if ($scope.displayFromClass) delete $scope.displayFromClass;
                        utils.safeApply($scope);
                    });


                }
                else {
                    delete $scope.suiviCompetence;
                    utils.safeApply($scope);
                    return;
                }
            }
        };
        $scope.initSliderBFC = function () {
            $scope.suiviCompetence.getConversionTable($scope.evaluations.structure.id, $scope.search.classe.id, $scope.mapCouleurs).then(
                function (data) {
                    return $scope.suiviCompetence.tableConversions;
                }
            );
        };
        $scope.updateSuiviEleve = (Eleve) => {
            $scope.selected.grey = true;
            $scope.search.classe = _.findWhere(evaluations.classes.all, {'id': Eleve.idClasse});
            $scope.syncPeriode($scope.search.classe.id);
            $scope.search.periode = '*';
            $scope.search.classe.eleves.sync().then(() => {
                $scope.search.eleve = _.findWhere($scope.search.classe.eleves.all, {'id': Eleve.id});
                $scope.selectSuivi($scope.route.current.$$route.originalPath);
                utils.safeApply($scope);
            });
        };
        $scope.initSuivi = () => {
            if ($scope.displayFromClass !== true) {
                $scope.search.eleve = "";
                delete $scope.informations.eleve;
                delete $scope.suiviCompetence;
            } else {
                $scope.selectSuivi($scope.route.current.$$route.originalPath);
                $scope.displayFromEleve = true;
                utils.safeApply($scope);
            }
        };

        $scope.initSuivi();
        $scope.$watch($scope.displayFromClass, function (newValue, oldValue) {
            if (newValue !== oldValue) {
                $scope.initSuivi();
            }
        });

        $scope.pOFilterEval = {
            limitTo: 2
        };

        $scope.exportBFC = (object, periode?) => {
            let url = "/competences/BFC/pdf?";
            if (object instanceof Structure) {
                url += "idStructure=" + object.id;
            } else if (object instanceof Classe) {
                url += "idClasse=" + object.id;
            } else if (object instanceof Eleve) {
                url += "idEleve=" + object.id;
            }
            if (periode && periode !== "*" && periode.id_type) {
                url += "&idPeriode=" + periode.id_type;
            }
            location.replace(url);
        };

        $scope.saveNiveauEnsCpl = () => {
            let id_langue;
            if ($scope.suiviCompetence.langueSelected !== undefined) {
                id_langue = $scope.suiviCompetence.langueSelected.id;

                // si la langue culturelle choisie est aucun, on remet le niveau à 0 par pécaution
                if ($scope.suiviCompetence.langueSelected.code == 'AUC') {
                    $scope.suiviCompetence.niveauLangueCultRegSelected = new NiveauLangueCultReg(0);
                }
            } else {
                // si pas de langue culturelle choisie on remet le niveau à 0 par pécaution
                $scope.suiviCompetence.niveauLangueCultRegSelected = new NiveauLangueCultReg(0);
            }

            $scope.suiviCompetence.eleveEnsCpl.setAttributsEleveEnsCpl($scope.suiviCompetence.ensCplSelected.id,
                $scope.suiviCompetence.niveauEnsCplSelected.id,
                $scope.suiviCompetence.niveauLangueCultRegSelected.niveau,
                id_langue).save();
            $scope.showButtonSave = !$scope.showButtonSave;

            $scope.successUpdateEnseignement = true;
            utils.safeApply($scope);
            $timeout(()=> {
                $scope.successUpdateEnseignement = false;
                utils.safeApply($scope);
            },3000);
        };

        $scope.successCreateSynthese = false;
        $scope.successUpdateSynthese = false;
        $scope.successUpdateEnseignement = false;

        $scope.saveSynthese = () => {
            $scope.suiviCompetence.bfcSynthese.saveBfcSynthese().then((res) => {
                if (res.rows === 1) {
                    $scope.successUpdateSynthese = true;
                    utils.safeApply($scope);
                    $timeout(() => {
                        $scope.successUpdateSynthese = false;
                        utils.safeApply($scope);
                    }, 3000);
                } else {
                    $scope.successCreateSynthese = true;
                    utils.safeApply($scope);
                    $timeout(() => {
                        $scope.successCreateSynthese = false;
                        utils.safeApply($scope);
                    }, 3000);
                }
            });
        };

        /**
         * Filtre permettant de retourner l'évaluation maximum en fonction du paramètre de recherche "Mes Evaluations"
         * @param listeEvaluations Tableau d'évaluations de compétences
         * @returns {(evaluation:any)=>(boolean|boolean)} Retourne true si la compétence courante est la plus haute du tableau listeEvaluations
         */
        $scope.isMaxEvaluation = function (listeEvaluations) {
            return function (evaluation) {
                var _evalFiltered = listeEvaluations;
                if ($scope.suiviFilter.mine === 'true' || $scope.suiviFilter.mine === true) {
                    _evalFiltered = _.filter(listeEvaluations, function (competence) {
                        return competence.owner !== undefined && competence.owner === $scope.me.userId;
                    });
                }

                // filtre sur les competences prises dans le calcul
                _evalFiltered = _.filter(_evalFiltered, function (competence) {
                    return !competence.formative; // la competence doit être reliée à un devoir ayant un type non "formative"
                });

                // calcul du max parmis les competences
                var max = _.max(_evalFiltered, function (competence) {
                    return competence.evaluation;
                });
                if (typeof max === 'object') {
                    return evaluation.id_competences_notes === max.id_competences_notes;
                } else {
                    return false;
                }
            };
        };

        /**
         * Retourne si l'utilisateur n'est pas le propriétaire de compétences
         * @param listeEvaluations Tableau d'évaluations de compétences
         * @returns {boolean} Retourne true si l'utilisateur n'est pas le propriétaire
         */
        $scope.notEvalutationOwner = function (listeEvaluations) {
            if ($scope.suiviFilter === undefined) $scope.initFilterMine();
            if ($scope.suiviFilter.mine === 'false' || $scope.suiviFilter.mine === false) {
                return false;
            }
            var _t = _.filter(listeEvaluations, function (competence) {
                return competence.owner === undefined || competence.owner === $scope.me.userId;
            });
            return _t.length === 0;
        };


        /*
         Listener sur le template suivi-competence-detail permettant la transition entre la vue détail
         et la vue globale
         */
        template.watch("suivi-competence-detail", function () {
            if (!$scope.opened.detailCompetenceSuivi) {
                $scope.opened.detailCompetenceSuivi = true;
            }
        });

        /**
         * Lance la séquence d'ouverture du détail d'une compétence permettant d'accéder à la vue liste ou graph
         * @param competence Compétence à ouvrir
         */
        $scope.openDetailCompetence = function (competence, detail) {
            $scope.detailCompetence = competence;
            $scope.initChartsEval();
            if (detail !== undefined) {
                template.open("suivi-competence-detail", detail);
            }
            else {
                template.open("suivi-competence-detail",
                    "enseignants/suivi_competences_eleve/detail_vue_graph");
            }
            utils.scrollTo('top');
        };

        /**
         * Lance la séquence de retour à la vue globale du suivi de compétence
         */
        $scope.backToSuivi = function () {
            template.close("suivi-competence-detail");
            $scope.opened.detailCompetenceSuivi = false;
            $scope.detailCompetence = null;
            $scope.messages.successEvalLibre = false;
        };

        /**
         * Retourne si l'utilisateur est le propriétaire de l'évaluation
         * @param evaluation Evaluation à afficher
         * @returns {boolean} Retourne true si l'utilisateur est le propriétaire de l'évaluation
         */
        $scope.filterOwnerSuivi = function (evaluation) {
            if ($scope.suiviFilter === undefined) $scope.initFilterMine();
            if ($scope.suiviFilter.mine === 'false' || $scope.suiviFilter.mine === false) {
                return true;
            }
            return evaluation.owner === $scope.me.userId;
        };

        $scope.EvaluationExiste = function (list) {
            let ListOfOwner = _.map(list, function (item) {
                if (item.owner === $scope.me.userId)
                    return item;
            });
            if (ListOfOwner.length === 0) {
                return true;
            } else {
                return false;
            }
        };

        /**
         * Remplace l'élève recherché par le nouveau suite à l'incrémentation de l'index
         * @param num pas d'incrémentation. Peut être positif ou négatif
         */
        $scope.incrementEleve = async function (num) {
            $scope.selected.grey = true;
            let index = _.findIndex($scope.search.classe.eleves.all, {id: $scope.search.eleve.id});
            if (index !== -1 && index + parseInt(num) >= 0
                && index + parseInt(num) < $scope.search.classe.eleves.all.length) {
                $scope.search.eleve = $scope.search.classe.eleves.all[index + parseInt(num)];
                $scope.changeContent();
            }
        };

        $scope.changeContent = async function () {
            let content = $scope.template.containers['suivi-competence-content'].split('.html?hash=')[0].split('template/')[1];
            await $scope.selectSuivi($scope.route.current.$$route.originalPath);
            $scope.template.open('suivi-competence-content', content);
            utils.safeApply($scope);
        };
        $scope.textPeriode = "Hors periode scolaire";

        // /**
        //  * Return la periode scolaire courante
        //  * @returns {any}
        //  */
        // $scope.periodeParDefault = function () {
        //     let PeriodeParD = new Date().toISOString();
        //     let PeriodeSet = false;
        //     //let  PeriodeParD = new Date().getFullYear() +"-"+ new Date().getMonth() +1 +"-" +new Date().getDate();
        //
        //     for (let i = 0; i < $scope.periodes.all.length; i++) {
        //         if (PeriodeParD >= $scope.periodes.all[i].timestamp_dt && PeriodeParD <= $scope.periodes.all[i].timestamp_fn) {
        //             PeriodeSet = true;
        //             return $scope.periodes.all[i];
        //         }
        //     }
        //     if (PeriodeSet === false) {
        //         return $scope.textPeriode;
        //     }
        // };

        $scope.chartOptionsEval = {
            series: ['Evaluation'],
            tooltipLabels: [],
            options: {
                tooltips: {
                    callbacks: {
                        label: function (tooltipItems, data) {
                            return $scope.chartOptionsEval.tooltipLabels[tooltipItems.index];
                        }
                    }
                },
                elements: {
                    point: {
                        radius: 10,

                    },
                    line: {
                        fill: false,
                        borderDash: [0, 15]
                    }
                },
                maintainAspectRatio: false,
                scales: {
                    responsive: true,
                    yAxes: [{
                        gridLines: {
                            display: false,
                            color: '#000000'
                        },
                        pointRadius: 10,
                        type: 'linear',
                        display: true,
                        ticks: {
                            max: 6,
                            min: 0,
                            fontColor: 'black',
                            stepSize: 1,
                            padding: 20,
                            callback: function (value, index, values) {
                                if (value === 1) {
                                    return "Compétence non évaluée";
                                }
                                else if (value === 2) {
                                    return "Maîtrise insuffisante";
                                }
                                else if (value === 3) {
                                    return "Maîtrise fragile";
                                }
                                else if (value === 4) {
                                    return "Maîtrise satisfaisante";
                                }
                                else if (value === 5) {
                                    return "Très bonne maîtrise";
                                }
                                else {
                                    return " ";
                                }
                                // return parseFloat(value).toFixed(2) + '%';
                            }
                        },
                    }],
                    xAxes: [{
                        type: 'category',
                        display: true,
                        responsive: false,
                        gridLines: {
                            display: false,
                            offsetGridLines: false,
                            color: '#000000'
                        },
                        ticks: {
                            labelOffset: 30,
                            minRotation: 20, // rotation des labels
                            autoSkip: true,
                            maxTicksLimit: 20,
                            fontColor: 'black'
                        }
                    }]
                }
            },
            //les données des axes X: et Y:
            datasets: {
                labels: [],
                data: []
            },
            //les couleurs des points
            colors: []
        };
        /**
         * MISE A JOUR POUR LA PRISE EN COMPTE DE LA PERSONNALISATION DES COULEURS DE COMPETENCES DANS LE GRAPHE
         */
        Chart.plugins.register({
            afterDatasetsDraw: function (chart, easing) {
                // To only draw at the end of animation, check for easing === 1
                let ctx = chart.chart.ctx;

                chart.data.datasets.forEach(function (dataset, i) {
                    let meta = chart.getDatasetMeta(i);
                    if (!meta.hidden) {
                        meta.data.forEach(function (element, index) {
                            // Draw the text invert color of buble, with the specified font
                            let rgba = dataset.backgroundColor[index];
                            rgba = rgba.split('(')[1].split(')')[0].split(',');
                            let r = 255 - parseInt(rgba[0]);
                            let g = 255 - parseInt(rgba[1]);
                            let b = 255 - parseInt(rgba[2]);
                            let a = rgba[3];

                            ctx.fillStyle = "rgba(" + r.toString() + "," + g.toString() + "," + b.toString() + "," + a + ")";
                            let fontSize = 10.5;
                            let fontStyle = 'normal';
                            let fontFamily = 'Helvetica Neue';
                            ctx.font = Chart.helpers.fontString(fontSize, fontStyle, fontFamily);
                            // Just naively convert to string for now
                            let dataString = dataset.data[index].label;
                            // Make sure alignment settings are correct
                            ctx.textAlign = 'center';
                            ctx.textBaseline = 'middle';
                            //var padding = 5;
                            let position = element.tooltipPosition();
                            if (dataString === undefined) {
                                dataString = " ";
                            }
                            ctx.fillText(dataString, position.x, position.y);

                        });
                    }
                });
            }
        });
        /**
         *
         */
        $scope.initChartsEval = function () {
            if ($scope.detailCompetence !== undefined && $scope.detailCompetence !== null) {
                let ListEval = _.filter($scope.detailCompetence.competencesEvaluations, function (evalu) {
                    return $scope.filterOwnerSuivi(evalu);
                });
                //initialisation et rajout de la 1er colomn vide
                $scope.chartOptionsEval.tooltipLabels = [];
                $scope.chartOptionsEval.tooltipLabels.push(' ');
                $scope.chartOptionsEval.datasets.data = [];
                $scope.chartOptionsEval.datasets.data.push(-10);
                $scope.chartOptionsEval.datasets.labels = [];
                $scope.chartOptionsEval.datasets.labels.push(" ");
                $scope.chartOptionsEval.colors = [];
                $scope.chartOptionsEval.colors.push('#FFFFFF');
                ListEval = _.sortBy(ListEval, function (evalu) {
                    return evalu.evaluation_date;
                });

                for (let i = 0; i < ListEval.length; i++) {

                    let fontText = $scope.mapLettres[ListEval[i].evaluation];
                    if (!fontText) {
                        fontText = " ";
                    }
                    $scope.chartOptionsEval.datasets.data.push({
                        y: ListEval[i].evaluation + 2,
                        x: $scope.getDateFormated(ListEval[i].evaluation_date),
                        r: 10,
                        label: fontText
                    });
                    $scope.chartOptionsEval.datasets.labels.push($scope.getDateFormated(ListEval[i].evaluation_date));
                    let colorValue;
                    if (ListEval[i].evaluation !== -1) {
                        colorValue = $scope.mapCouleurs[ListEval[i].evaluation];
                    }
                    else {
                        colorValue = Defaultcolors.unevaluated;
                    }
                    $scope.chartOptionsEval.colors.push(colorValue);

                    let libelle = ListEval[i].evaluation_libelle;
                    if (ListEval[i].formative) {
                        libelle += " (F)"
                    }
                    $scope.chartOptionsEval.tooltipLabels.push(libelle + ' : ' + ListEval[i].owner_name);

                }

                //rajout de la dernière colomn vide
                $scope.chartOptionsEval.datasets.data.push(-10);
                $scope.chartOptionsEval.datasets.labels.push(" ");
                $scope.chartOptionsEval.colors.push('#FFFFFF');
                $scope.chartOptionsEval.tooltipLabels.push(' ');
            }
            utils.safeApply($scope);
        };
        $scope.$watch($scope.detailCompetence, function () {
            $scope.initChartsEval();
        });

        $scope.selected.grey = true;

        $scope.FilterNotEvaluated = function (MaCompetence) {
            if ($scope.selected.grey === true || ($scope.selected.grey === false && MaCompetence.masque)) {
                let _t = MaCompetence.competencesEvaluations;
                if ($scope.suiviFilter.mine === 'true' || $scope.suiviFilter.mine === true) {
                    _t = _.filter(MaCompetence.competencesEvaluations, function (evaluation) {
                        if (evaluation.owner !== undefined && evaluation.owner === $scope.me.userId)
                            return evaluation;
                    });
                }


                let max = _.max(_t, function (evaluation) {
                    return evaluation.evaluation;
                });
                if (typeof max === 'object') {
                    return (!(max.evaluation == -1));
                } else {
                    return false;
                }

            } else {
                return true;
            }

        };


        $scope.hasMaxNotFormative = function (MaCompetence) {
            let _evalFiltered = MaCompetence.competencesEvaluations;
            if ($scope.suiviFilter.mine === 'true' || $scope.suiviFilter.mine === true) {
                _evalFiltered = _.filter(MaCompetence.competencesEvaluations, function (evaluation) {
                    if (evaluation.owner !== undefined && evaluation.owner === $scope.me.userId)
                        return evaluation;
                });
            }

            // filtre sur les competences prises dans le calcul
            _evalFiltered = _.filter(_evalFiltered, function (competence) {
                return !competence.formative; // la competence doit être reliée à un devoir ayant un type non "formative"
            });

            let max = _.max(_evalFiltered, function (evaluation) {
                return evaluation.evaluation;
            });
            if (typeof max === 'object') {
                return (!(max.evaluation == -1));
            } else {
                return false;
            }
        }

        $scope.saveDispenseEleve = async (domaine) => {
            //$scope.domaine = new Domaine(domaine);
            domaine.dispense_eleve = !domaine.dispense_eleve;
            await domaine.saveDispenseEleve();
            domaine.slider.options.disabled = !domaine.slider.options.disabled;
            domaine.slider.options.readOnly = !domaine.slider.options.readOnly;

            //on récupère la période en cours en fonction du type car quand il n'y a pas de période sélectionnée on a un type de période
            let idTypePeriode = ($scope.suiviCompetence.periode.id !== null)?  $scope.suiviCompetence.periode.id_type : null;
            await $scope.suiviCompetence.baremeBrevetEleves.sync($scope.suiviCompetence.classe.id, idTypePeriode);
            $scope.suiviCompetence.baremeBrevetEleve = _.findWhere($scope.suiviCompetence.baremeBrevetEleves.all, {id_eleve : $scope.search.eleve.id});
            utils.safeApply($scope);
        }
    }
]);
