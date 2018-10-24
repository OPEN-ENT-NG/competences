import {notify, idiom as lang, ng, template, model} from 'entcore';
import * as utils from '../utils/teacher';
import {ElementBilanPeriodique} from "../models/teacher/ElementBilanPeriodique";
import {BilanPeriodique} from "../models/teacher/BilanPeriodique";
import {Eleve, Utils} from "../models/teacher";
import {SyntheseBilanPeriodique} from "../models/teacher/SyntheseBilanPeriodique";
import {AppreciationCPE} from "../models/teacher/AppreciationCPE";

declare let _:any;

export let evalBilanPeriodiqueCtl = ng.controller('EvalBilanPeriodiqueCtl', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$route','$timeout',
    async function ($scope, route, $rootScope, $location, $filter) {
        template.close('suivi-acquis');
        template.close('projet');
        template.close('vie-scolaire');
        template.close('graphique');
        template.close('graphMatiere');
        template.close('graphDomaine');
        template.close('synthese');
        utils.safeApply($scope);
        $scope.critereIsEmpty = true;
        $scope.showHistoric = false;
        $scope.showAvis = false;

        $scope.selected = {suiviAcquis: true, projet: false, vieScolaire: false, graphique: false};

        $scope.displayBilanPeriodique = () => {
            if(model.me.type === 'PERSRELELEVE'){
                $scope.critereIsEmpty = !($scope.search.periode !== '*' && $scope.search.periode !== null && $scope.search.periode !== undefined);
            }else{
                $scope.critereIsEmpty = !(($scope.search.classe !== '*' && $scope.search.classe !== null && $scope.search.classe !== undefined)
                    && ($scope.search.eleve !== '*' && $scope.search.eleve !== null && $scope.search.eleve !== undefined)
                    && ($scope.search.periode !== '*' && $scope.search.periode !== null && $scope.search.periode !== undefined));
            }
            $scope.updateColorAndLetterForSkills();
        };

        if(model.me.type === 'PERSRELELEVE'){
            template.open('left-menu','enseignants/bilan_periodique/left-side-parent-bilanperiodique');
            $scope.displayBilanPeriodique();
            await $scope.openSuiviAcquis();
        }else{
            template.open('left-menu', 'enseignants/bilan_periodique/left-side-bilanperiodique');
            $scope.displayBilanPeriodique();
        }

        $scope.fiterTrimestres = () => {
            return ( item ) => {
                return item.id_type > -1;
            }
        };

        $scope.MAX_CHAR_APPRECIATION_ELEMENT_LENGTH = 600;
        $scope.MAX_CHAR_APPRECIATION_LENGTH = 300;


        //////            Onglets du bilan périodique            //////

        $scope.openSuiviAcquis = async () => {
            $scope.selected = {suiviAcquis: true, projet: false, vieScolaire: false, graphique: false};
            template.close('projet');
            template.close('vie-scolaire');
            template.close('graphique');
            template.close('graphMatiere');
            template.close('graphDomaine');
            template.close('synthese');
            if(model.me.type === 'PERSRELELEVE'){
                $scope.isChefEtabOrIsHeadTeacher = false;
            }else{
                $scope.isChefEtabOrIsHeadTeacher = await Utils.isChefEtab($scope.search.classe);
            }
            $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.search.eleve,
                $scope.search.periode.id_type,$scope.structure, $scope.filteredPeriode);
            await $scope.elementBilanPeriodique.suivisAcquis.getSuivisDesAcquis();
            await $scope.elementBilanPeriodique.syncSyntheseBilanPeriodique();
            utils.safeApply($scope);
            template.open('suivi-acquis', 'enseignants/bilan_periodique/display_suivi_acquis');
            template.open('synthese', 'enseignants/bilan_periodique/display_synthese');
            utils.safeApply($scope);
        };

        $scope.openProjet = async () => {
            $scope.selected = {projet: true};
            template.close('suivi-acquis');
            template.close('vie-scolaire');
            template.close('graphique');
            template.close('graphMatiere');
            template.close('graphDomaine');
            template.close('synthese');
            $scope.canUpdateAppreciations = await Utils.canUpdateAppreciations($scope.search.classe);
            utils.safeApply($scope);
            template.open('projet', 'enseignants/bilan_periodique/display_projets');
            $scope.getElementsBilanBilanPeriodique("isBilanPeriodique");
            $scope.isBilanPeriodique = "isBilanPeriodique";
            utils.safeApply($scope);
        };

        $scope.openVieScolaire = async function () {
            $scope.selected = {vieScolaire: true};
            template.close('suivi-acquis');
            template.close('projet');
            template.close('graphique');
            template.close('graphMatiere');
            template.close('graphDomaine');
            template.close('synthese');
            let getEvenements = _.isEmpty($scope.search.eleve.evenements);
            if (!getEvenements) {
                $scope.search.eleve.evenement = _.findWhere($scope.search.eleve.evenements,
                    {id_periode: $scope.search.periode.id_type});
            }
            else {
                await $scope.search.eleve.getEvenements();
                let year = {
                    retard: 0,
                    abs_just: 0,
                    abs_non_just: 0,
                    abs_totale_heure: 0,
                    ordre: 0,
                    periode: $scope.getI18nPeriode({id: null})
                };
                _.forEach($scope.filteredPeriode, (periode) => {
                    let evenement = _.findWhere($scope.search.eleve.evenements, {id_periode: periode.id_type});
                    let pushIt = false;
                    if (evenement === undefined) {
                        evenement = {id_periode : periode.id_type};
                        pushIt = true;
                    }
                    evenement.periode = $scope.getI18nPeriode(periode);
                    evenement.ordre = periode.ordre;

                    // Remplissage de la ligne pour l'année
                    year.retard += ((evenement.retard !== undefined) ? evenement.retard : 0);
                    year.abs_just += ((evenement.abs_just !== undefined) ? evenement.abs_just : 0);
                    year.abs_non_just += ((evenement.abs_non_just !== undefined) ? evenement.abs_non_just : 0);
                    year.abs_totale_heure += ((evenement.abs_totale_heure !== undefined) ? evenement.abs_totale_heure : 0);
                    year.ordre += ((evenement.ordre !== undefined) ? evenement.ordre : 0);


                    if (periode.id_type === $scope.search.periode.id_type) {
                        $scope.search.eleve.evenement = evenement;
                    }
                    if (pushIt) {
                        $scope.search.eleve.evenements.push(evenement);
                    }
                });
                $scope.search.eleve.evenements.push(year);
                }
            $scope.elementBilanPeriodique.appreciationCPE = new AppreciationCPE($scope.informations.eleve.id, $scope.search.periode.id_type);
            await $scope.elementBilanPeriodique.appreciationCPE.sync();
            utils.safeApply($scope);
            template.open('vie-scolaire', 'enseignants/bilan_periodique/display_vie_scolaire');
            utils.safeApply($scope);
        };


        $scope.openGraphique = async function () {
            $scope.selected = { graphique: true };
            template.close('suivi-acquis');
            template.close('projet');
            template.close('vie-scolaire');
            $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.search.eleve, $scope.search.periode.id_type,$scope.structure, $scope.filteredPeriode);
            $scope.elementBilanPeriodique.syntheseBilanPeriodique = new SyntheseBilanPeriodique($scope.informations.eleve.id, $scope.search.periode.id_type);
            await $scope.elementBilanPeriodique.syntheseBilanPeriodique.sync();
            utils.safeApply($scope);
            template.open('graphique', 'enseignants/bilan_periodique/display_graphiques');
            template.open('synthese', 'enseignants/bilan_periodique/display_synthese');
            utils.safeApply($scope);
        };


        //////            Graph de l'onglet graphique            //////

        $scope.openMatiere = async function () {
            template.close('graphMatiere');
            utils.safeApply($scope);
            await $scope.elementBilanPeriodique.getDataForGraph($scope.informations.eleve, false);
            template.open('graphMatiere', 'enseignants/bilan_periodique/graph/graph_subject');
            utils.safeApply($scope);
        };

        $scope.openDomaine = async function () {
            template.close('graphDomaine');
            utils.safeApply($scope);
            await $scope.elementBilanPeriodique.getDataForGraph($scope.informations.eleve, true);
            template.open('graphDomaine', 'enseignants/bilan_periodique/graph/graph_domaine');
            utils.safeApply($scope);
        };


        //////            Changement d'élèves à l'aide des flèches            //////

        $scope.incrementEleve = async function (num) {
            let index = _.findIndex($scope.search.classe.eleves.all, {id: $scope.search.eleve.id});
            if (index !== -1 && index + parseInt(num) >= 0
                && index + parseInt(num) < $scope.search.classe.eleves.all.length) {
                $scope.search.eleve = $scope.search.classe.eleves.all[index + parseInt(num)];
                $scope.changeContent();
                delete $scope.informations.competencesNotes;
                $scope.informations.competencesNotes = $scope.informations.eleve.competencesNotes;
            }
            if (template.contains('graphMatiere', 'enseignants/bilan_periodique/graph/graph_subject')) {
                $scope.openMatiere();
            }
            if (template.contains('graphDomaine', 'enseignants/bilan_periodique/graph/graph_domaine')) {
                $scope.openDomaine();
            }
            if (template.contains('synthese', 'enseignants/bilan_periodique/display_synthese')) {
                $scope.openDomaine();
            }
        };

        $scope.changeContent = async function () {
            $scope.informations.eleve = $scope.search.eleve;
            if (!$scope.critereIsEmpty) {
                if (template.contains('graphMatiere', 'enseignants/bilan_periodique/graph/graph_subject')) {
                    $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.informations.eleve, $scope.search.periode.id_type,$scope.structure, $scope.filteredPeriode);
                    $scope.openMatiere();
                }
                if (template.contains('graphDomaine', 'enseignants/bilan_periodique/graph/graph_domaine')) {
                    $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.informations.eleve, $scope.search.periode.id_type, $scope.structure, $scope.filteredPeriode);
                    $scope.openDomaine();
                }
                if (template.contains('synthese', 'enseignants/bilan_periodique/display_synthese')) {
                    await $scope.openGraphique();
                }
                if ($scope.selected.suiviAcquis) {
                    await $scope.openSuiviAcquis();
                }
                if ($scope.selected.vieScolaire) {
                    await $scope.openVieScolaire();
                }
            }
            if ($scope.selected.projet) {
                await $scope.openProjet();
            }
        };

        $scope.deleteStudent = function () {
            $scope.informations.eleve = '';
            $scope.critereIsEmpty = true;
        };


        //////            Lightbox historique            //////

        $scope.openHistoric = function () {
            $scope.opened.historic = true;
        };


        /**
         * Saisir projet   -   Bilan Periodique
         */

        $scope.getElementsBilanBilanPeriodique = async function (param?) {
            if ($scope.bilanPeriodique !== undefined) {
                delete $scope.bilanPeriodique;
            }
            if ($scope.search.periode !== undefined && $scope.search.periode !== null && $scope.search.periode !== "*"
                && $scope.search.classe !== undefined && $scope.search.classe !== null && $scope.search.classe !== "*") {

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
            if (param === "isClassChanging") {
                if ($scope.search.classe.periodes.length() === 0) {
                    await $scope.search.classe.periodes.sync();
                }
                $scope.filteredPeriode = $filter('customClassPeriodeFilters')($scope.structure.typePeriodes.all, $scope.search);
            }
            if (param === "isBilanPeriodique") {
                _.forEach($scope.bilanPeriodique.appreciations, (appreciation) => {
                    if ($scope.elementsDisplay === undefined) {
                        $scope.elementsDisplay = [_.findWhere($scope.bilanPeriodique.elements, {id: appreciation.id_elt_bilan_periodique})];
                    }
                    else if (!_.findWhere($scope.elementsDisplay, {id: appreciation.id_elt_bilan_periodique})) {
                        $scope.elementsDisplay.push(_.findWhere($scope.bilanPeriodique.elements, {id: appreciation.id_elt_bilan_periodique}));
                    }
                })

                $scope.informations.eleve = _.findWhere($scope.bilanPeriodique.classe.eleves.all, {id: $scope.search.eleve.id});
            }
            utils.safeApply($scope);
        };


        /**
         * Séquence d'enregistrement d'une appréciation
         * @param element sur lequel est faite l'appréciation
         * @param eleve élève propriétaire de l'appréciation
         */

        $scope.saveAppElement = function (element, eleve?) {
            if (eleve) {
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
        };


        $scope.filterElements = (type) => {
            return (item) => {
                if ($scope.selected.AP) {
                    return !item.theme;
                }
                else if ($scope.selected.parcours) {
                    return !item.libelle;
                }
                else if ($scope.selected.EPI) {
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
        };
    }


]);
