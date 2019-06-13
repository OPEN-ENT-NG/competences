import {notify, idiom as lang, ng, template, model, Behaviours, angular} from 'entcore';
import * as utils from '../utils/teacher';
import {ElementBilanPeriodique} from "../models/teacher/ElementBilanPeriodique";
import {BilanPeriodique} from "../models/teacher/BilanPeriodique";
import {evaluations, Utils} from "../models/teacher";
import {SyntheseBilanPeriodique} from "../models/teacher/SyntheseBilanPeriodique";
import {AppreciationCPE} from "../models/teacher/AppreciationCPE";
import {AvisConseil} from "../models/teacher/AvisConseil";
import {AvisOrientation} from "../models/teacher/AvisOrientation";
import {updateColorAndLetterForSkills, updateNiveau} from "../models/common/Personnalisation";
import {ComparisonGraph} from "../models/common/ComparisonGraph";

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

        let finSaisieBilan : boolean;
        $scope.critereIsEmpty = true;
        $scope.showHistoric = false;
        $scope.showAvisOrientation = false;
        $scope.showMoyGeneral = false;
        $scope.opened.criteres = true;
        $scope.opened.avis = true;
        $scope.opened.ensCplt = true;
        $scope.selected = {suiviAcquis: true, projet: false, vieScolaire: false, graphique: false, bfc: false};
        $scope.graphDom = {opened: true, comparison: false, darkness:true, infoGrouped: false};
        $scope.graphMat = {opened: true, comparison: false, darkness:true, infoGrouped: false};
        $scope.opened.bfcPeriode = undefined;
        $scope.displayBilanPeriodique = async() => {
            if(model.me.type === 'PERSRELELEVE'){
                $scope.critereIsEmpty = !($scope.search.periode !== '*' && $scope.search.periode !== null && $scope.search.periode !== undefined);
            }else{
                $scope.critereIsEmpty = !(($scope.search.classe !== '*' && $scope.search.classe !== null && $scope.search.classe !== undefined)
                    && ($scope.search.eleve !== '*' && $scope.search.eleve !== null && $scope.search.eleve !== undefined)
                    && ($scope.search.periode !== '*' && $scope.search.periode !== null && $scope.search.periode !== undefined));
            }
        };

        if(model.me.type === 'PERSRELELEVE'){
            template.open('left-menu','enseignants/bilan_periodique/left-side-parent-bilanperiodique');
            $scope.displayBilanPeriodique();
        }else{
            template.open('left-menu', 'enseignants/bilan_periodique/left-side-bilanperiodique');
            $scope.displayBilanPeriodique();
        }

        $scope.fiterTrimestres = () => {
            return ( item ) => {
                return item.id_type > -1 || $scope.selected.bfc ===true;
            }
        };

        $scope.MAX_CHAR_APPRECIATION_ELEMENT_LENGTH = 600;
        $scope.MAX_CHAR_APPRECIATION_LENGTH = 300;

        let closeTemplateButNot = (notThis) => {
            let allTemplate = ['suivi-acquis','vie-scolaire', 'graphique', 'graphMatiere', 'graphDomaine',
                'synthese','projet','bfc'];

            _.forEach(allTemplate, (_template) => {
                if(notThis instanceof Array){
                    if(!_.contains(notThis,template) || _.isEmpty(notThis)){
                        template.close(_template);
                    }
                }
                else {
                    if(notThis !== _template){
                        template.close(_template);
                    }
                }
            });
            if(!$scope.critereIsEmpty && $scope.elementBilanPeriodique === undefined){
                $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.search.eleve,
                    $scope.search.periode.id_type,$scope.structure, $scope.filteredPeriode);
            }
        };

        //////            Onglets du bilan périodique            //////

        $scope.openSuiviAcquis = async () => {
            $scope.selected = {suiviAcquis: true, projet: false, vieScolaire: false, graphique: false, bfc :false};
            closeTemplateButNot('suivi-acquis');
            if($scope.search.periode === undefined ||  $scope.search.periode.id_type < 0
                || $scope.search.periode.id_type === undefined){
                await utils.safeApply($scope);
                return;
            }
            utils.safeApply($scope);

            if(model.me.type === 'PERSRELELEVE'){
                $scope.canSaveAppMatierePosiBilanPeriodique = false;
                $scope.canSaisiSyntheseBilanPeriodique = false;
            }else{
                $scope.canSaveAppMatierePosiBilanPeriodique = await Utils.rightsChefEtabHeadTeacherOnBilanPeriodique($scope.search.classe,
                    "canSaveAppMatierePosiBilanPeriodique") && finSaisieBilan;
                $scope.canSaisiSyntheseBilanPeriodique = await Utils.rightsChefEtabHeadTeacherOnBilanPeriodique($scope.search.classe,
                    "canSaisiSyntheseBilanPeriodique" ) && finSaisieBilan;
            }
            $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.search.eleve, $scope.search.periode.id_type,$scope.structure, $scope.filteredPeriode);
            await $scope.elementBilanPeriodique.suivisAcquis.getSuivisDesAcquis();
            $scope.elementBilanPeriodique.syntheseBilanPeriodique = new SyntheseBilanPeriodique($scope.informations.eleve.id, $scope.search.periode.id_type);
            await $scope.elementBilanPeriodique.syntheseBilanPeriodique.syncSynthese();
            utils.safeApply($scope);
            template.open('suivi-acquis', 'enseignants/bilan_periodique/display_suivi_acquis');
            template.open('synthese', 'enseignants/bilan_periodique/display_synthese');
            utils.safeApply($scope);
        };

        $scope.openProjet = async () => {
            $scope.selected = {projet: true};
            closeTemplateButNot('projet');
            if($scope.search.periode === undefined ||  $scope.search.periode.id_type < 0
                || $scope.search.periode.id_type === undefined){
                await utils.safeApply($scope);
                return;
            }
            $scope.canUpdateAppreciations = await Utils.rightsChefEtabHeadTeacherOnBilanPeriodique($scope.search.classe,
                "canUpdateAppreciations") && finSaisieBilan;
            utils.safeApply($scope);
            template.open('projet', 'enseignants/bilan_periodique/display_projets');
            $scope.getElementsBilanBilanPeriodique("isBilanPeriodique");
            utils.safeApply($scope);
        };

        $scope.openVieScolaire = async function () {
            $scope.selected = {vieScolaire: true};
            closeTemplateButNot('vie-scolaire');
            if($scope.search.periode === undefined ||  $scope.search.periode.id_type < 0
                || $scope.search.periode.id_type === undefined){
                await utils.safeApply($scope);
                return;
            }
            utils.safeApply($scope);
            $scope.canUpdateRetardAndAbscence = model.me.hasWorkflow(
                Behaviours.applicationsBehaviours.competences.rights.workflow.canUpdateRetardAndAbscence) && finSaisieBilan;

            $scope.canSaisiAppreciationCPE = Utils.canSaisiAppreciationCPE() && finSaisieBilan;
            let getEvenements = _.isEmpty($scope.search.eleve.evenements);
            if (!getEvenements) {
                $scope.search.eleve.evenement = _.findWhere($scope.search.eleve.evenements,
                    {id_periode: $scope.search.periode.id_type});
            }
            else {
                await $scope.search.eleve.getEvenements();
                $scope.setHistoriqueEvenement();
            }
            $scope.elementBilanPeriodique.appreciationCPE = new AppreciationCPE($scope.informations.eleve.id,
                $scope.search.periode.id_type);
            await $scope.elementBilanPeriodique.appreciationCPE.syncAppreciationCPE();
            utils.safeApply($scope);
            template.open('vie-scolaire', 'enseignants/bilan_periodique/display_vie_scolaire');
            utils.safeApply($scope);
        };

        $scope.setHistoriqueEvenement = async function () {
            let year = {
                retard: 0,
                abs_just: 0,
                abs_non_just: 0,
                abs_totale_heure: 0,
                ordre: 0,
                periode: $scope.getI18nPeriode({id: null})
            };
            if(!_.isEmpty($scope.search.eleve.evenements)
                && ($scope.search.eleve.evenements.length > $scope.filteredPeriode.length)) {
                // On enlève la ligne correspondant à l'année pour la recalculer si on doit la mettre à jour
                $scope.search.eleve.evenements.pop();

            }
            _.forEach(_.filter($scope.filteredPeriode, (p) => {return p.id_type > -1}), (periode) => {
                let evenement = _.findWhere($scope.search.eleve.evenements, {id_periode: periode.id_type});
                let pushIt = false;
                if (evenement === undefined) {
                    evenement = {id_periode : periode.id_type};
                    pushIt = true;
                }
                evenement.periode = $scope.getI18nPeriode(periode);
                evenement.ordre = periode.ordre;

                // initialisation des retards et absences
                evenement.retard = (evenement.retard !== undefined) ? evenement.retard : 0;
                evenement.abs_just = (evenement.abs_just !== undefined) ? evenement.abs_just : 0;
                evenement.abs_non_just = (evenement.abs_non_just !== undefined) ? evenement.abs_non_just : 0;
                evenement.abs_totale_heure = (evenement.abs_totale_heure !== undefined) ?
                    evenement.abs_totale_heure : 0;
                evenement.ordre = (evenement.ordre !== undefined) ? evenement.ordre : 0;

                // Remplissage de la ligne pour l'année
                year.retard += evenement.retard;
                year.abs_just += evenement.abs_just;
                year.abs_non_just += evenement.abs_non_just;
                year.abs_totale_heure += evenement.abs_totale_heure;
                year.ordre += evenement.ordre;


                if (periode.id_type === $scope.search.periode.id_type) {
                    $scope.search.eleve.evenement = evenement;
                }
                if (pushIt) {
                    $scope.search.eleve.evenements.push(evenement);
                }
            });
            $scope.search.eleve.evenements.push(year);

            $scope.elementBilanPeriodique.appreciationCPE = new AppreciationCPE($scope.informations.eleve.id, $scope.search.periode.id_type);
            await $scope.elementBilanPeriodique.appreciationCPE.syncAppreciationCPE();
            utils.safeApply($scope);
            template.open('vie-scolaire', 'enseignants/bilan_periodique/display_vie_scolaire');
            utils.safeApply($scope);
        };


        $scope.openGraphique = async function () {
            try {

                $scope.selected = {graphique: true};
                template.close('suivi-acquis');
                template.close('projet');
                template.close('vie-scolaire');
                if($scope.search.periode === undefined ||  $scope.search.periode.id_type < 0
                    || $scope.search.periode.id_type === undefined){
                    await utils.safeApply($scope);
                    return;
                }
                await Utils.runMessageLoader($scope);
                $scope.canSaisiSyntheseBilanPeriodique = await Utils.rightsChefEtabHeadTeacherOnBilanPeriodique(
                    $scope.search.classe,"canSaisiSyntheseBilanPeriodique") && finSaisieBilan;
                $scope.elementBilanPeriodique.syntheseBilanPeriodique = new SyntheseBilanPeriodique(
                    $scope.informations.eleve.id, $scope.search.periode.id_type);
                await $scope.elementBilanPeriodique.syntheseBilanPeriodique.syncSynthese();
                await utils.safeApply($scope);
                template.open('graphique', 'enseignants/bilan_periodique/display_graphiques');
                template.open('synthese', 'enseignants/bilan_periodique/display_synthese');
                await Utils.stopMessageLoader($scope);
            }
            catch (e) {
                await Utils.stopMessageLoader($scope);
            }
        };
        $scope.buildCycle =  function(){
            return {libelle: lang.translate('viescolaire.utils.cycle.tolower'), id: null, isCycle: true, id_type: -2};
        };
        $scope.buildYear = function(){
            return {libelle: lang.translate('viescolaire.utils.annee'), id: null, isCycle: false, id_type: -1};
        };

        $scope.openBFC = async function (bfcPeriode?) {
            await Utils.runMessageLoader($scope);
            $scope.selected = { bfc: false };

            try {
                if (bfcPeriode === undefined) {
                    await $scope.search.eleve.getCycles();
                    let cycle = _.findWhere($scope.filteredPeriode, {isCycle: true});
                    let year = _.findWhere($scope.filteredPeriode, {isCycle: false});
                    if (year === undefined) {
                        $scope.filteredPeriode.push($scope.buildYear());
                    }
                    if (cycle === undefined) {
                        cycle = $scope.buildCycle();
                        $scope.filteredPeriode.push(cycle);
                    }
                    bfcPeriode = cycle;
                    $scope.search.periode = cycle;
                }

                $scope.opened.bfcPeriode = bfcPeriode;
                closeTemplateButNot([]);
                await utils.safeApply($scope);
                template.open('bfc', 'enseignants/suivi_competences_eleve/content');
                $scope.selected = {bfc: true};
                await utils.safeApply($scope);
            }
            catch (e){
                console.error(e);
                await Utils.stopMessageLoader($scope);
            }
        };

        //////            Graph de l'onglet graphique            //////
        $scope.switchOpenMatiere = async function (isOpened){
            if(isOpened !== true) {
                await $scope.openMatiere();
            }
        };

        $scope.openMatiere = async function () {
            template.close('graphMatiere');
            await utils.safeApply($scope);
            let promiseOpenMatiere = [];
            promiseOpenMatiere.push($scope.elementBilanPeriodique.getDataForGraph($scope.informations.eleve, false,
                $scope.niveauCompetences, $scope.search.periode.id_type));


            if($scope.graphMat !== undefined && $scope.graphMat.comparison === true){
                promiseOpenMatiere.push($scope.drawComparison($scope.filteredPeriode,false,$scope.graphMat.darkness,
                    $scope.graphMat.infoGrouped, true));
            }
            await Promise.all(promiseOpenMatiere);
            template.open('graphMatiere', 'enseignants/bilan_periodique/graph/graph_subject');
            await utils.safeApply($scope);
        };

        $scope.drawComparison = async function (periodes, forDomaine, withdarkness, tooltipGroup,
                                                withoutNotification?) {

            let conditionTocheck = {};
            let fieldToCheck = (forDomaine===true)?'comparisonDom' : 'comparison';
            conditionTocheck[fieldToCheck] = true;
            let selectedPeriodes = _.where(periodes, conditionTocheck);

            if(_.isEmpty(selectedPeriodes) || selectedPeriodes.length < 2){
                if(withoutNotification !== true) {
                    notify.info('evaluations.choose.at.least.two.periode');
                }
                return;
            }

            if($scope.informations.eleve === undefined){
                if(withoutNotification !== true) {
                    notify.info('evaluations.choose.student.for.periode');
                }
                return;
            }

            let templateToSet = (forDomaine===true)? 'comparisonGraphDom' : 'comparisonGraphMatiere';

            template.close(templateToSet);
            await Utils.runMessageLoader($scope);
            try {
                await ComparisonGraph.buildComparisonGraph($scope.informations.eleve, selectedPeriodes,
                    evaluations.structure, forDomaine, $scope.niveauCompetences, withdarkness, tooltipGroup);
                let templateToOpen = forDomaine? 'domaine' : 'subject';
                template.open(templateToSet, `enseignants/bilan_periodique/graph/comparison_graph_${templateToOpen}`);
                if(withoutNotification !== true) {
                    await Utils.stopMessageLoader($scope);
                }
            } catch (e) {
                await Utils.stopMessageLoader($scope);
            }
        };
        $scope.switchOpenDomaine = async function (isOpened){
            if(isOpened !== true) {
                await $scope.openDomaine();
            }
        };
        $scope.openDomaine = async function () {
            template.close('graphDomaine');
            await utils.safeApply($scope);
            let promiseDomaine = [];
            if(template.contains('comparisonGraphDom', 'enseignants/bilan_periodique/graph/comparison_graph_domaine' )){
                promiseDomaine.push($scope.drawComparison($scope.filteredPeriode,true,$scope.graphDom.darkness,
                    $scope.graphDom.infoGrouped, true));
            }
            promiseDomaine.push($scope.elementBilanPeriodique.getDataForGraph($scope.informations.eleve, true,
                $scope.niveauCompetences, $scope.search.periode.id_type));
            await Promise.all(promiseDomaine);
            template.open('graphDomaine', 'enseignants/bilan_periodique/graph/graph_domaine');
            await utils.safeApply($scope);
        };


        //////            Changement d'élèves à l'aide des flèches            //////

        $scope.incrementEleve = async function (num) {
            let index = _.findIndex($scope.search.classe.eleves.all, {id: $scope.search.eleve.id});
            let allPromise = [];
            await Utils.runMessageLoader($scope);
            if (index !== -1 && index + parseInt(num) >= 0
                && index + parseInt(num) < $scope.search.classe.eleves.all.length) {
                $scope.search.eleve = $scope.search.classe.eleves.all[index + parseInt(num)];
                await $scope.changeContent();
                delete $scope.informations.competencesNotes;
                $scope.informations.competencesNotes = $scope.informations.eleve.competencesNotes;
            }
        };

        $scope.changeContent = async function () {
            $scope.informations.eleve = $scope.search.eleve;
            let allPromise = [];
            if (!$scope.critereIsEmpty) {
                $scope.updateColorAndLetterForSkills();
                if($scope.selected.bfc !== true) {
                    finSaisieBilan = !_.find($scope.search.classe.periodes.all,
                        {id_type: $scope.search.periode.id_type}).publication_bulletin;
                }

                if (template.contains('graphMatiere', 'enseignants/bilan_periodique/graph/graph_subject')) {
                    $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.informations.eleve,
                        $scope.search.periode.id_type,$scope.structure, $scope.filteredPeriode);
                    allPromise.push($scope.openMatiere());
                }
                if (template.contains('graphDomaine', 'enseignants/bilan_periodique/graph/graph_domaine')) {
                    $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.informations.eleve,
                        $scope.search.periode.id_type, $scope.structure, $scope.filteredPeriode);

                    allPromise.push($scope.openDomaine());
                }
                if ((template.contains('synthese', 'enseignants/bilan_periodique/display_synthese')) && ($scope.selected.graphique)) {
                    $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.search.eleve,
                        $scope.search.periode.id_type,$scope.structure, $scope.filteredPeriode);
                    allPromise.push($scope.openGraphique());
                }

                if ($scope.selected.suiviAcquis) {
                    $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.search.eleve,
                        $scope.search.periode.id_type,$scope.structure, $scope.filteredPeriode);
                    await $scope.openSuiviAcquis();
                }
                if($scope.elementBilanPeriodique === undefined){
                    $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.search.eleve,
                        $scope.search.periode.id_type,$scope.structure, $scope.filteredPeriode);
                }
                if ($scope.selected.vieScolaire) {
                    await $scope.openVieScolaire();
                }

                if ($scope.selected.projet) {
                    await $scope.openProjet();
                }
                if ($scope.selected.bfc){
                    await $scope.openBFC($scope.search.periode);
                }
                await Utils.awaitAndDisplay(allPromise, $scope, undefined, $scope.selected.bfc);

                $scope.elementBilanPeriodique.avisConseil = new AvisConseil($scope.informations.eleve.id, $scope.search.periode.id_type);
                $scope.elementBilanPeriodique.avisOrientation = new AvisOrientation($scope.informations.eleve.id, $scope.search.periode.id_type);
                await $scope.elementBilanPeriodique.avisConseil.getLibelleAvis();
                await $scope.elementBilanPeriodique.avisConseil.syncAvisConseil();
                await $scope.elementBilanPeriodique.avisOrientation.syncAvisOrientation();
                $scope.search.idAvisClasse = $scope.elementBilanPeriodique.avisConseil.id_avis_conseil_bilan;
                $scope.search.idAvisOrientation = $scope.elementBilanPeriodique.avisOrientation.id_avis_conseil_bilan;
                utils.safeApply($scope);
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


        $scope.filterAvis = function (param) {
            return (avis) => {
                if (avis.type_avis === param) {
                    return avis;
                }
            }
        };



        /**
         * Saisir projet   -   Bilan Periodique
         */

        $scope.getElementsBilanBilanPeriodique = async function (param?) {
            if($scope.bilanPeriodique !== undefined ) {
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
                        await $scope.bilanPeriodique.syncAppreciations($scope.bilanPeriodique.elements,
                            $scope.search.periode, $scope.search.classe);
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
                $scope.filteredPeriode = $filter('customClassPeriodeFilters')
                ($scope.structure.typePeriodes.all, $scope.search);
                if($scope.selected.bfc === true){
                    let cycle = _.findWhere($scope.filteredPeriode, {isCycle: true});
                    let year = _.findWhere($scope.filteredPeriode, {isCycle: false});
                    if(year === undefined){
                        $scope.filteredPeriode.push($scope.buildYear());
                    }
                    if(cycle === undefined){
                        $scope.filteredPeriode.push($scope.buildCycle());
                    }

                    if($scope.search.periode === undefined){
                        $scope.search.periode = _.findWhere($scope.filteredPeriode, {isCycle: true});
                    }
                }

            }
            if (param === "isBilanPeriodique" && $scope.bilanPeriodique !== undefined) {
                _.forEach($scope.bilanPeriodique.appreciations, (appreciation) => {
                    if ($scope.elementsDisplay === undefined) {
                        $scope.elementsDisplay = [_.findWhere($scope.bilanPeriodique.elements, {id: appreciation.id_elt_bilan_periodique})];
                    }
                    else if (!_.findWhere($scope.elementsDisplay, {id: appreciation.id_elt_bilan_periodique})) {
                        $scope.elementsDisplay.push(_.findWhere($scope.bilanPeriodique.elements, {id: appreciation.id_elt_bilan_periodique}));
                    }
                });

                $scope.informations.eleve = _.findWhere($scope.bilanPeriodique.classe.eleves.all, {id: $scope.search.eleve.id});
            }
            utils.safeApply($scope);
        };


        /**
         * Séquence d'enregistrement d'une appréciation
         * @param element sur lequel est faite l'appréciation
         * @param eleve élève propriétaire de l'appréciation
         */

        $scope.saveAppElement = function (element, isBilanPeriodique, eleve? ) {
            if (eleve) {
                if (eleve.appreciations !== undefined) {
                    if (eleve.appreciations[$scope.search.periode.id][element.id] !== undefined) {
                        if (eleve.appreciations[$scope.search.periode.id][element.id].length <= $scope.MAX_CHAR_APPRECIATION_ELEMENT_LENGTH) {
                            $scope.bilanPeriodique.saveAppreciation($scope.search.periode, element, eleve, $scope.search.classe, isBilanPeriodique);
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
                        $scope.bilanPeriodique.saveAppreciation($scope.search.periode, element, null,
                            $scope.search.classe, $scope.bilanPeriodique, isBilanPeriodique);
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


        $scope.syncPeriodesBilanPeriodique = async function (classe) {
            if ($scope.search.classe.periodes.all.length === 0) {
                await $scope.search.classe.periodes.sync();
            }
            $scope.filteredPeriode = $filter('customClassPeriodeFilters')
            ($scope.structure.typePeriodes.all, $scope.search);
            $scope.filteredPeriode = $filter('customClassPeriodeFilters')
            ($scope.structure.typePeriodes.all, $scope.search);
            if($scope.selected.bfc === true){
                let cycle = _.findWhere($scope.filteredPeriode, {isCycle: true});
                let year = _.findWhere($scope.filteredPeriode, {isCycle: false});
                if(year === undefined){
                    $scope.filteredPeriode.push($scope.buildYear());
                }
                if(cycle === undefined){
                    $scope.filteredPeriode.push($scope.buildCycle());
                }
                if($scope.search.periode === undefined){
                    $scope.search.periode = _.findWhere($scope.filteredPeriode, {isCycle: true});
                }
            }
            $scope.getCurrentPeriodeBP = function (classe, res) {
                if($location.path() === '/conseil/de/classe'){
                    let selectedPeriode = undefined;
                    if ($scope.search.periode !== undefined) {
                        selectedPeriode = _.findWhere(classe.periodes.all,
                            {id_type: $scope.search.periode.id_type});
                    }
                    if ($scope.search !== undefined && $scope.search.eleve !== undefined) {
                        // On choisit la période présélectionnée
                        $scope.search.periode = selectedPeriode;
                    }else  if ($scope.displayFromClass === true || $scope.displayFromEleve === true){
                        $scope.search.periode = selectedPeriode;
                    }
                    else {
                        $scope.search.periode = res;
                    }
                }
            }
            utils.safeApply($scope);
        };

        $scope.savePositionnementEleve = async (suiviDesAcquis,positionnement) => {
            await suiviDesAcquis.savePositionnementEleve(positionnement);
            utils.safeApply($scope);
        };

        $scope.setColonne = async function (colonne) {
            await $scope.search.eleve.setColonne(colonne, $scope.search.periode.id_type);
            $scope.setHistoriqueEvenement();
            utils.safeApply($scope);
        };

        $scope.updateNiveau = function (usePerso) {
            updateNiveau(usePerso, $scope);
        };

        $scope.updateColorAndLetterForSkills = async function () {
            updateColorAndLetterForSkills($scope, $location);
            let graphiqueClosed = false;
            let bfcClosed = false;
            if($scope.selected !== undefined) {
                if($scope.selected.graphique === true) {
                    await Utils.runMessageLoader($scope);
                    $scope.selected.graphique = false;
                    template.close('graphique');
                    graphiqueClosed = true;
                }
                if($scope.selected.bfc === true) {
                    await Utils.runMessageLoader($scope);
                    $scope.selected.bfc = false;
                    template.close('bfc');
                    bfcClosed = true;
                }
            }
            await utils.safeApply($scope);
            if(graphiqueClosed){
                $scope.selected.graphique = true;
                template.open('graphique', 'enseignants/bilan_periodique/display_graphiques');
                if (template.contains('graphMatiere', 'enseignants/bilan_periodique/graph/graph_subject')) {
                    $scope.openMatiere();
                }
                if (template.contains('graphDomaine', 'enseignants/bilan_periodique/graph/graph_domaine')) {
                    $scope.openDomaine();
                }
                await Utils.stopMessageLoader($scope);
            }
            if(bfcClosed){
                $scope.selected.bfc = true;
                template.open('bfc', 'enseignants/suivi_competences_eleve/content');
            }

            await utils.safeApply($scope);
        };
        $scope.$watch('selected', function(newValue, oldValue) {
            if($scope.selected !== newValue) {
                angular.copy(newValue, $scope.selected);
            }
        });

    }

]);
