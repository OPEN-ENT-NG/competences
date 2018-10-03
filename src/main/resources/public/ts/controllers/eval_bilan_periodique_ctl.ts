import {notify, idiom as lang, ng, template} from 'entcore';
import * as utils from '../utils/teacher';
import {ElementBilanPeriodique} from "../models/teacher/ElementBilanPeriodique";
import {BilanPeriodique} from "../models/teacher/BilanPeriodique";


declare let _:any;

export let evalBilanPeriodiqueCtl = ng.controller('EvalBilanPeriodiqueCtl', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$route','$timeout',
    async function ($scope, route, $rootScope, $location, $filter) {
        template.close('suivi-acquis');
        template.close('projet');
        template.close('vie-scolaire');
        template.close('graphique');
        utils.safeApply($scope);
        $scope.critereIsEmpty = true;


        $scope.MAX_CHAR_APPRECIATION_ELEMENT_LENGTH = 600;

        $scope.displayBilanPeriodique = function () {
            if (($scope.search.classe !== '*' && $scope.search.classe !== null && $scope.search.classe !== undefined)
                && ($scope.informations.eleve !== null && $scope.informations.eleve !== null && $scope.informations.eleve !== undefined)
                && ($scope.search.periode !== '*') && $scope.search.periode !== null && $scope.search.periode !== undefined) {
                $scope.critereIsEmpty = false;
            }
        }

        $scope.selected = { suiviAcquis: true, projet: false, vieScolaire: false, graphique: false };

        $scope.openSuiviAcquis = function () {
            $scope.selected = { suiviAcquis: true, projet: false, vieScolaire: false, graphique: false };
            template.close('projet');
            template.close('vie-scolaire');
            template.close('graphique');
            utils.safeApply($scope);
            template.open('suivi-acquis', 'enseignants/bilan_periodique/display_suivi_acquis');
            utils.safeApply($scope);
        }

        $scope.openProjet = function () {
            $scope.selected = { projet: true };
            template.close('suivi-acquis');
            template.close('vie-scolaire');
            template.close('graphique');
            utils.safeApply($scope);
            template.open('projet', 'enseignants/bilan_periodique/display_projets');
            $scope.getElementsBilanBilanPeriodique("isBilanPeriodique");
            $scope.isBilanPeriodique = "isBilanPeriodique";
            utils.safeApply($scope);
        }

        $scope.openVieScolaire = function () {
            $scope.selected = { vieScolaire: true };
            template.close('suivi-acquis');
            template.close('projet');
            template.close('graphique');
            utils.safeApply($scope);
            template.open('vie-scolaire', 'enseignants/bilan_periodique/display_vie_scolaire');
            utils.safeApply($scope);
        }

        $scope.openGraphique = function () {
            $scope.selected = { graphique: true };
            template.close('suivi-acquis');
            template.close('projet');
            template.close('vie-scolaire');
            utils.safeApply($scope);
            $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.informations.eleve, $scope.search.periode.id);
            template.open('graphique', 'enseignants/bilan_periodique/display_graphiques');
            // $scope.critereIsEmpty = true;
            utils.safeApply($scope);
        }

        // $scope.displayGraphique = async function () {
        //     $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.informations.eleve, $scope.search.periode.id);
        //     if ($scope.selected = { graphique: true }) {
        //         template.close('suivi-acquis');
        //         template.open('graphique', 'enseignants/bilan_periodique/display_graphiques');
        //         $scope.critereIsEmpty = false;
        //     }
        // }

        $scope.openMatiere = async function () {
            template.close('graphMatiere');
            utils.safeApply($scope);
            await $scope.elementBilanPeriodique.getDataForGraph($scope.informations.eleve, false);
            template.open('graphMatiere', 'enseignants/bilan_periodique/graph/graph_subject');
            utils.safeApply($scope);
        }

        $scope.openDomaine = async function () {
            template.close('graphDomaine');
            utils.safeApply($scope);
            await $scope.elementBilanPeriodique.getDataForGraph($scope.informations.eleve, true);
            template.open('graphDomaine', 'enseignants/bilan_periodique/graph/graph_domaine');
            utils.safeApply($scope);
        }

        $scope.changeContentBilanPeriod = function () {
            $scope.informations.eleve = $scope.search.eleve;
        }

        /**
         * Saisir projet   -   Bilan Periodique
         */

        $scope.getElementsBilanBilanPeriodique = async function (param?){
            if($scope.bilanPeriodique !== undefined ) {
                delete $scope.bilanPeriodique;
            }
            if($scope.search.periode !== undefined && $scope.search.periode !== null && $scope.search.periode !== "*"
                && $scope.search.classe !== undefined && $scope.search.classe !== null && $scope.search.classe !== "*"){

                if ($scope.search.classe.periodes.length() === 0) {
                    await $scope.search.classe.periodes.sync();
                }

                let _p = _.findWhere($scope.search.classe.periodes.all, {id_type: $scope.search.periode.id_type});
                if (_p) {
                    if (!$scope.bilanPeriodique || param === "isClassChanging") {
                        $scope.bilanPeriodique = new BilanPeriodique($scope.search.periode, $scope.search.classe);
                    }

                    if (param === "isClassChanging" || $scope.bilanPeriodique.elements === undefined) {
                        if ($scope.search.classe.eleves.length() === 0) {
                            await $scope.search.classe.eleves.sync();
                        }
                        await $scope.bilanPeriodique.syncElements(param);
                    }

                    if ($scope.bilanPeriodique.elements !== undefined && $scope.bilanPeriodique.elements.length > 0) {
                        await $scope.bilanPeriodique.syncAppreciations($scope.bilanPeriodique.elements, $scope.search.periode, $scope.search.classe);
                        utils.safeApply($scope);
                    } else {
                        delete $scope.bilanPeriodique;
                    }
                } else {
                    $scope.search.periode = "*";
                    delete $scope.bilanPeriodique;
                }
            } else {
                delete $scope.bilanPeriodique;
            }
            if(param === "isClassChanging"){
                if ($scope.search.classe.periodes.length() === 0) {
                    await $scope.search.classe.periodes.sync();
                }
                $scope.filteredPeriode = $filter('customClassPeriodeFilters')($scope.structure.typePeriodes.all, $scope.search);
            }
            if(param === "isBilanPeriodique"){
                _.forEach($scope.bilanPeriodique.appreciations, (appreciation) => {

                    if($scope.elementsDisplay === undefined) {
                        $scope.elementsDisplay = [_.findWhere($scope.bilanPeriodique.elements, {id: appreciation.id_elt_bilan_periodique})];
                    } else {
                        $scope.elementsDisplay.push(_.findWhere($scope.bilanPeriodique.elements, {id: appreciation.id_elt_bilan_periodique}));
                    }

                })

                $scope.informations.eleve = _.findWhere($scope.bilanPeriodique.classe.eleves.all, {id: $scope.search.eleve.id});
            }

            utils.safeApply($scope);
        }

        /**
         * Séquence d'enregistrement d'une appréciation
         * @param element sur lequel est faite l'appréciation
         * @param eleve élève propriétaire de l'appréciation
         */

        $scope.incrementEleve = async function (num) {
            let index = _.findIndex($scope.search.classe.eleves.all, {id: $scope.search.eleve.id});
            if (index !== -1 && index + parseInt(num) >= 0
                && index + parseInt(num) < $scope.search.classe.eleves.all.length) {
                $scope.search.eleve = $scope.search.classe.eleves.all[index + parseInt(num)];
                $scope.changeContentBilanPeriod();
                // delete $scope.informations.competencesNotes;
                // $scope.informations.competencesNotes = $scope.informations.eleve.competencesNotes;
            }
            if(template.contains('graphMatiere', 'enseignants/bilan_periodique/graph/graph_subject')) {
                template.close('graphMatiere');
                utils.safeApply($scope);
                await $scope.elementBilanPeriodique.getDataForGraph($scope.informations.eleve, false);
                template.open('graphMatiere', 'enseignants/bilan_periodique/graph/graph_subject');
                utils.safeApply($scope);
            }
            if(template.contains('graphDomaine', 'enseignants/bilan_periodique/graph/graph_domaine')) {
                template.close('graphDomaine');
                utils.safeApply($scope);
                await $scope.elementBilanPeriodique.getDataForGraph($scope.informations.eleve, true);
                template.open('graphDomaine', 'enseignants/bilan_periodique/graph/graph_domaine');
                utils.safeApply($scope);
            }
        };


        $scope.saveAppElement = function (element, eleve?) {
            if(eleve){
                if (eleve.appreciations !== undefined) {
                    if (eleve.appreciations[$scope.search.periode.id][element.id] !== undefined) {
                        if (eleve.appreciations[$scope.search.periode.id][element.id].length <= $scope.MAX_CHAR_APPRECIATION_ELEMENT_LENGTH) {
                            $scope.bilanPeriodique.saveAppreciation($scope.search.periode, element, eleve, $scope.search.classe);
                        }
                        else {
                            notify.error(lang.translate("error.char.outbound") +
                                $scope.MAX_CHAR_APPRECIATION_ELEMENT_LENGTH);
                        }
                    }
                }
            } else {
                if (element.appreciationClasse[$scope.search.periode.id][$scope.search.classe.id] !== undefined) {
                    if (element.appreciationClasse[$scope.search.periode.id][$scope.search.classe.id].length <= $scope.MAX_CHAR_APPRECIATION_ELEMENT_LENGTH) {
                        $scope.bilanPeriodique.saveAppreciation($scope.search.periode, element, null, $scope.search.classe, $scope.bilanPeriodique);
                    }
                    else {
                        notify.error(lang.translate("error.char.outbound") +
                            $scope.MAX_CHAR_APPRECIATION_ELEMENT_LENGTH);
                    }
                }
            }
            utils.safeApply($scope);
        }

        $scope.filterElements = (type) => {
            return (item) => {
                if($scope.selected.AP){
                    return !item.theme;
                }
                else if($scope.selected.parcours){
                    return !item.libelle;
                }
                else if($scope.selected.EPI){
                    return item.theme && item.libelle;
                }
                else {
                    return item.type === type;
                }
            };
        };

        $scope.syncPeriodesBilanPeriodique = async function () {
            if ($scope.search.classe.periodes.length() === 0) {
                await $scope.search.classe.periodes.sync();
            }
            $scope.filteredPeriode = $filter('customClassPeriodeFilters')($scope.structure.typePeriodes.all, $scope.search);
            utils.safeApply($scope);
        }
    }


]);
