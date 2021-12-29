/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
import {Mix} from "entcore-toolkit";
import {model, notify, idiom as lang, ng, template, moment, _, angular, http, skin, Behaviours} from 'entcore';
import {
    Devoir,
    Evaluation,
    evaluations,
    ReleveNote,
    ReleveNoteTotale,
    GestionRemplacement,
    Classe, Annotation,
    Service,
} from '../models/teacher';
import * as utils from '../utils/teacher';
import {Defaultcolors} from "../models/eval_niveau_comp";
import {Utils} from "../models/teacher";
import {selectCycleForView, updateNiveau} from "../models/common/Personnalisation";
import httpAxios from "axios";
import {AppreciationCPE} from "../models/teacher/AppreciationCPE";
import {
    evaluationCreationCompetences,
    evaluationCreationCompetencesDevoir,
    evaluationCreationEnseignements,
    PreferencesUtils
} from "../utils/preferences";
import {ShortTermAnnotation} from "../constants/ShortTermAnnotation";
import { LengthLimit} from "../constants/ConstantCommonLength"
import {isValidClasse} from "../utils/functions/isValidClasse";
import {isValidDevoir} from "../utils/filters/isValidDevoir";
import {AppreciationSubjectPeriodStudent} from "../models/teacher/AppreciationSubjectPeriodStudent";

declare let $: any;
declare let document: any;
declare let window: any;
declare let console: any;
declare let Chart: any;

const {
    SHORT_TERM_ABSENT,
    SHORT_TERM_NOT_RETURNED,
    SHORT_TERM_DISPENSE,
    SHORT_TERM_NOT_NOTED,
} = ShortTermAnnotation;

export let evaluationsController = ng.controller('EvaluationsController', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$sce', '$compile', '$timeout', '$route',
    async function ($scope, route, $rootScope, $location, $filter, $sce, $compile, $timeout, $route) {

        await model.me.workflow.load(['viescolaire']);
        await PreferencesUtils.initPreference();

        $scope.buildLoadingMessageStructure = function (libelle) {
            return  `${  libelle  }`;
        };

        $scope.selectCycleForView = function (id_cycle?) {
            return selectCycleForView($scope, $location, id_cycle);
        };

        $scope.printOption = {
            display: false,
            fileType: "formSaisie",
            cartoucheNmb: 1,
            byEleve: false,
            inColor: false,
        };

        $scope.getI18nPeriode = (periode : any): string => {
            let result = lang.translate("viescolaire.utils.annee");
            if (periode) {
                if (periode.libelle !== null && periode.libelle !== undefined) {
                    if (periode.libelle === ("cycle")) {
                        result = lang.translate("viescolaire.utils.cycle");
                    }
                } else if (periode.id === null) {
                    result = lang.translate("viescolaire.utils.annee");
                } else if (!(periode.hasOwnProperty('id_classe') || periode.hasOwnProperty('id_groupe'))) {
                    result = periode ?
                        lang.translate("viescolaire.periode." + periode.type) + " " + periode.ordre
                        : lang.translate("viescolaire.utils.periodeError");
                } else {
                    let type_periode = _.findWhere($scope.structure.typePeriodes.all, {id: periode.id_type});
                    result = type_periode ?
                        lang.translate("viescolaire.periode." + type_periode.type) + " " + type_periode.ordre
                        : lang.translate("viescolaire.utils.periodeError");
                }
            }
            return result;
        };

        $scope.initSearch = () => {
            return {
                matiere: null,
                periode: null,
                classe: null,
                sousmatiere: null,
                type: null,
                idEleve: null,
                name: '',
                enseignant: null,
                duplication: '',
                matieres: [],
                services: [],

            }
        };
        $scope.togglePanel = function ($event) {
            $scope.showPanel = !$scope.showPanel;
            $event.stopPropagation();
        };

        $rootScope.$on('close-panel', function (e) {
            $scope.showPanel = false;
        });

        function endAccueil() {
            $scope.opened.lightbox = false;
            template.open('main', 'enseignants/eval_acu_teacher');
            utils.safeApply($scope);
        }

        let routesActions = {
            accueil: function () {
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    $scope.search = $scope.initSearch();
                    $scope.displayCycles($scope.search.periode);
                    if(evaluations.structure.devoirs.length() == 0) {
                        evaluations.structure.syncDevoirs(25);
                    }
                }
                endAccueil();
            },

            listRemplacements: function () {
                $scope.opened.lightbox = false;
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    $scope.listRemplacements();
                    utils.safeApply($scope);
                }
            },

            createDevoir: function () {
                $scope.opened.lightbox = false;
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    $scope.initReferences();
                    $scope.createDevoir();
                    $scope.devoir.dateDevoir = new Date($scope.devoir.date);
                    utils.safeApply($scope);
                }
            },

            editDevoir: async function (params) {
                $scope.opened.lightbox = false;
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    let devoirTmp = $scope.devoirs.findWhere({id: parseInt(params.idDevoir)});
                    $scope.devoir = $scope.initDevoir();
                    $scope.devoir.apprec_visible = false;
                    $scope.devoir.id_groupe = devoirTmp.id_groupe;
                    $scope.devoir.old_id_groupe = devoirTmp.id_groupe;
                    $scope.devoir.id = devoirTmp.id;
                    $scope.devoir.name = devoirTmp.name;
                    $scope.devoir.owner = devoirTmp.owner;
                    $scope.devoir.libelle = devoirTmp.libelle;
                    $scope.devoir.id_sousmatiere = devoirTmp.id_sousmatiere;
                    $scope.devoir.id_type = parseInt(devoirTmp.id_type);
                    $scope.devoir.id_matiere = devoirTmp.id_matiere;
                    $scope.devoir.matiere = _.findWhere(evaluations.structure.matieres.all, {id: $scope.devoir.id_matiere});
                    $scope.devoir.id_etat = parseInt(devoirTmp.id_etat);
                    $scope.devoir.date_publication = new Date(devoirTmp.date_publication);
                    $scope.devoir.id_etablissement = devoirTmp.id_etablissement;
                    $scope.devoir.diviseur = devoirTmp.diviseur;
                    $scope.devoir.coefficient = parseFloat(devoirTmp.coefficient);
                    $scope.devoir.date = new Date(devoirTmp.date);
                    $scope.devoir.ramener_sur = devoirTmp.ramener_sur;
                    $scope.devoir.is_evaluated = devoirTmp.is_evaluated;
                    $scope.oldIs_Evaluated = devoirTmp.is_evaluated;
                    $scope.devoir.apprec_visible = devoirTmp.apprec_visible;
                    $scope.devoir.dateDevoir = new Date($scope.devoir.date);
                    $scope.devoir.datePublication = new Date($scope.devoir.date_publication);
                    $scope.devoir.id_periode = devoirTmp.id_periode;
                    $scope.devoir.controlledDate = true;
                    $scope.firstConfirmSuppSkill = false;
                    $scope.secondConfirmSuppSkill = false;
                    $scope.evaluatedDisabel = false;
                    $scope.allCompetences = devoirTmp.competences;
                    $scope.evaluatedCompetence = $scope.evaluationOfSkilles($scope.allCompetences, devoirTmp);
                    $scope.setClasseEnseignants($scope.search);
                    $scope.notYearPeriodes = _.filter($scope.filteredPeriode, (periode) => {
                        return $scope.notYear(periode);
                    });
                    $scope.devoir.competences.sync().then(async () => {
                        await $scope.createDevoir();
                        template.open('main', 'enseignants/creation_devoir/display_creation_devoir');
                        $scope.displayCreationDevoir = true;
                        utils.safeApply($scope);
                    });
                }
            },

            listDevoirs: async function (params) {
                // Somehow $scope.selected.devoirs can be undefined so we encounter :
                // TypeError: Cannot set property 'list' of undefined then we handle this error by setting empty object that will be set
                if (!$scope.selected.devoirs) {
                    $scope.selected.devoirs = {};
                }
                $scope.selected.devoirs.list = [];
                $scope.opened.lightbox = false;
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();

                    // récupération de tous les devoirs sans limite
                    await evaluations.structure.syncDevoirs();

                    $scope.filteredDevoirs = _.filter($scope.devoirs.all, devoir => {
                        devoir.nameClass = $scope.getClasseData(devoir.id_groupe, 'name');
                        return $scope.filterValidDevoir(devoir);
                    });
                    let openTemplates = (): void => {
                        $scope.search.enseignant = null;
                        //rajout de la periode Annee
                        //await $scope.periodes.sync();
                        //$scope.initPeriodesList();
                        template.open('main', 'enseignants/liste_devoirs/display_devoirs_structure');
                        template.open('evaluations', 'enseignants/liste_devoirs/list_view');
                        utils.safeApply($scope);
                    };

                    if (Utils.isChefEtabOrHeadTeacher()) {
                        $scope.modificationDevoir = false;
                        if (!$scope.structure.synchronized.classes) {
                            $scope.structure.classes.sync();
                            evaluations.structure.devoirs.getPercentDone(_.pluck(evaluations.devoirs.all, 'id')).then(() => {
                                utils.safeApply($scope);
                            });
                        }
                    }
                    /* TODO PERCENT DONE
                     evaluations.devoirs.getPercentDone(_.pluck(evaluations.devoirs.all,'id'));
                     */
                    openTemplates();
                }
            },

            viewNotesDevoir: async function (params) {
                try {
                    await Utils.runMessageLoader($scope);
                    $scope.opened.lightbox = false;
                    if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                        $scope.cleanRoot();
                        window.scrollTo(0, 0);
                        $scope.resetSelected();
                        if (!template.isEmpty('leftSide-userInfo')) template.close('leftSide-userInfo');
                        if (!template.isEmpty('leftSide-devoirInfo')) template.close('leftSide-devoirInfo');
                        if ($scope.structure.devoirs.empty()) {
                            await $scope.structure.devoirs.sync();
                        }
                        let getDevoir = () => {
                            $scope.currentDevoir = _.findWhere(evaluations.structure.devoirs.all,
                                {id: parseInt(params.devoirId)});
                        };
                        getDevoir();
                        if ($scope.currentDevoir === undefined) {
                            await evaluations.structure.syncDevoirs();
                            getDevoir();
                            if ($scope.currentDevoir === undefined) {
                                notify.error('error.homework.not.found');
                                await Utils.stopMessageLoader($scope);
                                $scope.goTo('/');
                                return;
                            }
                        }
                        $scope.usePerso = evaluations.structure.usePerso;
                        $scope.updateColorAndLetterForSkills();
                        if ($scope.printOption === undefined) {
                            $scope.printOption = {
                                display: false,
                                fileType: "formSaisie",
                                cartoucheNmb: 1,
                                byEleve: false,
                                inColor: false,
                            };
                        }
                        if (evaluations.structure.classes.empty()) {
                            await evaluations.structure.classes.sync();
                        }
                        $scope.structure.classes = evaluations.structure.classes;
                        $scope.currentDevoir.groupe = _.findWhere($scope.structure.classes.all,
                            {id: $scope.currentDevoir.id_groupe});

                        let allPromise = [$scope.currentDevoir.calculStats(), $scope.currentDevoir.competences.sync()];
                        if ($scope.currentDevoir.groupe.periodes.empty()) {
                            allPromise.push($scope.currentDevoir.groupe.periodes.sync(),
                                $scope.currentDevoir.groupe.eleves.sync());
                        }
                        if ($scope.structure.typePeriodes.empty()) {
                            allPromise.push($scope.structure.typePeriodes.sync());
                        }

                        await Promise.all(allPromise);

                        $scope.currentDevoir.periode = _.findWhere($scope.currentDevoir.groupe.periodes.all,
                            {id_type: $scope.currentDevoir.id_periode});

                        $scope.currentDevoir.endSaisie = await $scope.checkEndSaisieSeul($scope.currentDevoir);

                        template.open('main', 'enseignants/liste_notes_devoir/display_notes_devoir');
                        let syncStudents = async () => {
                            $scope.openedDetails = true;
                            $scope.openedStatistiques = true;
                            $scope.openedStudentInfo = true;
                            if ($scope.currentDevoir !== undefined) {
                                await $scope.currentDevoir.eleves.sync($scope.currentDevoir.periode);
                            }

                            await Utils.stopMessageLoader($scope);
                        };

                        let _classe = evaluations.structure.classes.findWhere({id: $scope.currentDevoir.id_groupe});
                        if (_classe !== undefined) {
                            await syncStudents();
                        } else {
                            await Utils.stopMessageLoader($scope);
                        }
                    }
                } catch (e){
                    await Utils.stopMessageLoader($scope);
                }
            },

            displayReleveNotes: function (params) {
                $scope.myCharts = {};
                $scope.opened.lightbox = false;
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();

                    // récupération de tous les devoirs sans limite
                    evaluations.structure.syncDevoirs();

                    // Affichage des criteres par défaut quand on arrive sur le releve
                    $scope.openLeftMenu("opened.criteres", false);
                    if (!template.isEmpty('leftSide-userInfo')) template.close('leftSide-userInfo');
                    if (!template.isEmpty('leftSide-devoirInfo')) template.close('leftSide-devoirInfo');
                    if ($scope.releveNote !== undefined &&
                        (($scope.search.matiere === undefined
                                || $scope.search.matiere === null)
                            || $scope.search.matiere.id !== $scope.releveNote.idMatiere
                            || $scope.search.classe.id !== $scope.releveNote.idClasse
                            || $scope.search.periode.id_type !== $scope.releveNote.idPeriode)) {
                        $scope.releveNote = undefined;
                    }

                    if ($scope.search.classe !== '*' &&
                        ($scope.search.matiere !== null && $scope.search.matiere.id !== '*')
                        && $scope.search.periode !== '*') {
                        $scope.getReleve();
                    }
                    $scope.usePerso = evaluations.structure.usePerso;
                    $scope.updateColorAndLetterForSkills();
                    utils.safeApply($scope);
                    template.open('main', 'enseignants/releve_notes/display_releve');
                }
            },

            displayEpiApParcours: async function () {
                $scope.opened.lightbox = false;
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();

                    // Affichage des criteres par défaut quand on arrive sur les EPI AP Parcours
                    if ($scope.bilanPeriodique !== undefined) {
                        $scope.bilanPeriodique = undefined;
                    }

                    $scope.openLeftMenu("opened.criteres", false);
                    if (!template.isEmpty('leftSide-userInfo')) template.close('leftSide-userInfo');

                    $scope.selected = {EPI: true, AP: false, parcours: false};

                    $scope.filteredPeriode = $filter('customClassPeriodeFilters')($scope.structure.typePeriodes.all, $scope.search);
                    $scope.notYearPeriodes = _.filter($scope.filteredPeriode, (periode) => {
                        return $scope.notYear(periode);
                    });

                    utils.safeApply($scope);
                    template.open('main', 'enseignants/epi_ap_parcours/display_epi_ap_parcours');
                }
            },

            displayBilanPeriodique: function () {
                $scope.myCharts = {};
                $scope.opened.lightbox = false;
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    $scope.filteredPeriode = $filter('customClassPeriodeFilters')($scope.structure.typePeriodes.all, $scope.search);

                    delete $scope.informations.eleve;
                    utils.safeApply($scope);
                    template.open('main', 'enseignants/bilan_periodique/display_bilan_periodique');
                }
            },

            displaySuiviEleve: async function (params) {
                let Service = new Service({
                    id_etablissement : evaluations.structure.id
                });
                $scope.opened.lightbox = false;
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    let display = async function (){
                        $scope.selected.matieres = [];
                        $scope.exportByEnseignement = "false";
                        $scope.allUnselect = true;
                        $scope.releveComp = {
                            textMod: true
                        };
                        $scope.showRechercheBar = false;
                        if (!Utils.isChefEtabOrHeadTeacher()) {
                            Service.syncMatieres()
                                .then(function () {
                                    $scope.search.matieres = Service.matieres;
                                });
                            $scope.allMatieresSorted = _.sortBy($scope.search.matieres, 'rank');
                            utils.safeApply($scope);
                        } else {
                            $scope.allMatieresSorted = _.sortBy($scope.matieres.all, 'rank');
                            $scope.search.matieres = $scope.allMatieresSorted;
                        }

                        Service.syncServices()
                            .then(function () {
                                $scope.search.services = Service.services;
                            });

                        if ($scope.informations.eleve === undefined) {
                            $scope.informations.eleve = null;
                        }
                        $scope.sortType = 'rank'; // set the default sort type
                        $scope.sortReverse = false;  // set the default sort order
                        $scope.usePerso = evaluations.structure.usePerso;
                        $scope.updateColorAndLetterForSkills();
                        await utils.safeApply($scope);
                    };
                    if (params.idEleve != undefined && params.idClasse != undefined) {
                        $scope.search.classe = _.findWhere(evaluations.classes.all, {'id': params.idClasse});
                        $scope.search.eleve = _.findWhere($scope.structure.eleves.all, {'id': params.idEleve});
                        $scope.syncPeriode($scope.search.classe.id);
                        $scope.search.periode = null;
                        await Utils.runMessageLoader($scope);
                        await $scope.search.classe.eleves.sync();
                        $scope.search.eleve = _.findWhere($scope.search.classe.eleves.all, {'id': params.idEleve});
                        if ($scope.displayFromClass) $scope.displayFromClass = false;
                        $scope.displayFromClass = true;
                        await display();
                    } else {
                        $scope.syncPeriode($scope.search.classe ? $scope.search.classe.id : undefined);
                        await display();
                    }
                    template.open('main', 'enseignants/suivi_eleve/tabs_follow_eleve/follow_items/container');
                    await  utils.safeApply($scope);
                }
            },

            displaySuiviCompetencesClasse: async function (params) {
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    template.close('suivi-competence-content');
                    let display = async function(){
                        $scope.selected.matieres = [];
                        $scope.allUnselect = true;
                        $scope.allRefreshed = false;
                        $scope.opened.recapEval = false;
                        $scope.exportRecapEvalObj = {
                            errExport: false
                        };
                        $scope.suiviClasse = {
                            textMod: true,
                            exportByEnseignement: 'false',
                            withMoyGeneraleByEleve: true,
                            withMoyMinMaxByMat: true,
                            withAppreciations: true,
                            withAvisConseil: true,
                            withAvisOrientation: true,
                            print: 'printRecapEval'
                        };
                        $scope.disabledExportSuiviClasse = _.findIndex($scope.allMatieresSorted, {select: true}) === -1;
                        $scope.sortType = 'title'; // set the default sort type
                        $scope.sortReverse = false;  // set the default sort order
                        $scope.usePerso = evaluations.structure.usePerso;
                        template.open('main', 'enseignants/suivi_competences_classe/container');
                        await utils.safeApply($scope);
                    };
                    if (!Utils.isChefEtabOrHeadTeacher()) {
                        http().getJson('/viescolaire/matieres?idEtablissement=' + evaluations.structure.id)
                            .done(async function (matieres) {
                                $scope.search.matieres = matieres;
                            });
                        $scope.allMatieresSorted = _.sortBy($scope.search.matieres, 'rank');
                        utils.safeApply($scope);
                    } else {
                        $scope.allMatieresSorted = _.sortBy($scope.matieres.all, 'rank');
                        $scope.search.matieres = $scope.allMatieresSorted;
                    }
                    http().getJson('/viescolaire/services?idEtablissement=' + evaluations.structure.id)
                        .done(function (services) {
                            $scope.search.services = services;
                        });
                    if (params.idClasse != undefined) {
                        let classe: Classe = evaluations.classes.findWhere({id: params.idClasse});
                        $scope.search.classe = classe;
                        if (classe !== undefined) {
                            if (classe.eleves.empty()) classe.eleves.sync();
                            $scope.syncPeriode(params.idClasse);
                            await display();
                        }
                    } else {
                        await display();
                    }
                }
            },

            export: async function () {
                template.open('main', 'export/exports');
                await utils.safeApply($scope);
            },
            disabled: async () => {
                template.open('main', 'disabled_structure');
                await utils.safeApply($scope);
            },

            bulletin: async () => {
                template.open('main', 'enseignants//bulletin/print_bulletin');
                $scope.usePerso = evaluations.structure.usePerso;
                $scope.updateColorAndLetterForSkills();
                await utils.safeApply($scope);
            }
        };

        route(routesActions);


        $scope.disabledExportSuiviClasseButton = function () {
            if ($scope.suiviClasse.print === "printReleveComp" && _.findIndex($scope.allMatieresSorted, {select: true}) === -1) {
                return true;
            } else {
                return false;
            }
        }

        $scope.showRechercheBarFunction = function (display) {
            $scope.showRechercheBar = display;
        }


        $scope.displayCycles = (periode) => {
            if (periode !== null && periode !== undefined) {
                if (periode.libelle === "cycle") {
                    $scope.displayCycle = true;
                } else {
                    $scope.displayCycle = false;
                }
            } else {
                $scope.displayCycle = false;
            }
            if ($scope.search.periode && $scope.search.periode.id !== null) {
                $scope.isEndSaisieNivFinal = moment($scope.search.periode.date_fin_saisie).isBefore(moment(), "days");
            } else {
                $scope.isEndSaisieNivFinal = false;
            }
        }

        $scope.updateOrder = function () {
            let res = [];
            for (let i = 0; i < $scope.evaluations.competencesDevoir.length; i++) {
                let _c = _.findWhere($scope.evaluations.competencesDevoir, {index: i});
                if (_c !== undefined) {
                    res.push(_c);
                }
            }
            if (res.length === evaluations.competencesDevoir.length) {
                evaluations.competencesDevoir = res;
            }
        };
        $scope.lightboxChampsObligatoire = false;
        $scope.MAX_NBR_COMPETENCE = 12;
        $scope.MAX_LENGTH_300 = LengthLimit.MAX_300;
        $scope.exportRecapEvalObj = {
            errExport: false
        };
        $scope.bindElem = {
            lefSide: false
        };
        $scope.opened = {
            devoir: -1,
            note: -1,
            criteres: true,
            elementProgramme: true,
            editElementProgramme: false,
            details: true,
            statistiques: true,
            studentInfo: true,
            devoirInfo: true,
            lightbox: false,
            lightboxEvalLibre: false,
            lightboxReleve: false,
            lightboxConfirmCleanComment: false,
            recapEval: false,
            coefficientConflict: false,
            lightboxs: {
                updateDevoir: {
                    firstConfirmSupp: false,
                    secondConfirmSupp: false,
                    evaluatedSkillDisabel: false
                },
                createDevoir: {
                    firstConfirmSupp: false,
                    secondConfirmSupp: false
                }
            },
            accOp: 0,
            evaluation: {
                suppressionMsg1: false,
                suppressionMsg2: false,
            },
            displayStructureLoader: false,
            displayMessageLoader: false,
            releveNoteTotaleChoice : "moyPos"
        };

        $scope.isChefEtabOrHeadTeacher = (classe?) => {
            return Utils.isChefEtabOrHeadTeacher(classe);
        };

        $scope.isPersEducNat = () => {
            return Utils.isPersEducNat();
        }

        $scope.canAccessReleve = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.accessReleve);
        }

        $scope.canAccessSuiviEleve = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.accessSuiviEleve);
        }

        $scope.canAccessSuiviClasse = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.accessSuiviClasse);
        }

        $scope.canAccessProjets = () => {
            return Utils.canSaisieProjet();
        }

        $scope.canAccessConseil = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.accessConseil);
        }

        $scope.canAccessExport = () => {
            return Utils.canExportLSU();
        }

        $scope.canAccessBulletin = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.exportBulletins);
        }

        $scope.canCreateEval = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.canCreateEval);
        }

        $scope.cancreateDispenseDomaineEleve = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.createDispenseDomaineEleve);
        }

        $scope.evaluations = evaluations;
        $scope.competencesSearchKeyWord = "";
        $scope.devoirs = evaluations.devoirs;
        $scope.filteredDevoirs = _.filter($scope.devoirs.all, devoir => {
            devoir.nameClass = $scope.getClasseData(devoir.id_groupe, 'name');
            return $scope.filterValidDevoir(devoir);
        });
        $scope.enseignements = evaluations.enseignements;
        $scope.bSelectAllEnseignements = false;
        $scope.selectAllDomaines = true;
        $scope.matieres = evaluations.matieres;
        $scope.releveNotes = evaluations.releveNotes;
        $scope.releveNote = null;
        $scope.releveNoteTotale = null;
        $scope.classes = evaluations.classes;
        $scope.filteredClasses = _.filter($scope.classes.all, classe => {
            return $scope.filterValidClasse(classe);
        });
        $scope.search = $scope.initSearch();
        $scope.matieresFiltered = $filter('getMatiereClasse')($scope.matieres.all, $scope.search.classe ? $scope.search.classe.id : undefined, $scope.classes, model.me.userId);
        $scope.types = evaluations.types;
        $scope.filter = $filter;
        $scope.template = template;
        $scope.currentDevoir = {};
        $scope.informations = {};
        $scope.messages = {
            successEvalLibre: false,
            deleteEvalLibre: false
        };
        $scope.me = model.me;
        $scope.text = "";
        $scope.selected = {
            devoirs: {
                list: [],
                listwithEvaluatedSkills: [],
                listwithEvaluatedMarks: [],
                all: false
            },
            eleve: null,
            eleves: {
                list: [],
                all: false
            },
            competences: {
                list: [],
                all: false
            },
            chartClasse: false,
            classes: [],
            matieres: [],
            grey : true
        };

        $scope.annotationNN = utils.getNN();
        $scope.aideSaisie = {
            cycle: null,
            domaineEnseignement: null,
            sousDomainesEnseignement: [],
        };

        let setSearchPeriode = function(classe, res){
            if($location.path() === '/competences/eleve' || $location.path() === '/competences/classe' ){
                let year = _.findWhere(classe.periodes.all, {id: null});
                let selectedPeriode = undefined;
                if ($scope.search.periode !== undefined) {
                    selectedPeriode = _.findWhere(classe.periodes.all,
                        {id_type: $scope.search.periode ? $scope.search.periode.id_type : undefined});
                }
                if ($scope.search.eleve !== undefined && $scope.search.eleve.deleteDate !== undefined) {
                    // On choisit la periode annee ou la période présélectionnée
                    $scope.search.periode = (selectedPeriode !== undefined)? selectedPeriode : year;
                }else  if ($scope.displayFromClass === true || $scope.displayFromEleve === true){
                    $scope.search.periode = (selectedPeriode !== undefined)? selectedPeriode : year;
                }
                else {
                    $scope.search.periode = res;
                }
            } else if ($location.path() === '/devoirs/list' || $location.path() === '/'){
                $scope.search.periode = (res.id !== null)?
                    _.findWhere($scope.structure.typePeriodes.all, {id: res.id_type}) : _.findWhere($scope.structure.typePeriodes.all, {id: res.id}) ;
            } else {
                $scope.search.periode = res;
            }
        };

        $scope.syncPeriode = async (idClasse) => {
            if(idClasse){
                let classe = _.findWhere($scope.structure.classes.all, {id: idClasse});
                let currentPeriode = await $scope.getCurrentPeriode(classe);
                if ($location.path() === '/competences/eleve') {
                    if(!_.findWhere(classe.periodes.all, {libelle: "cycle"})) {
                        classe.periodes.all.push({libelle: "cycle", id: null});
                    }
                } else {
                    let cycle = _.findWhere(classe.periodes.all, {libelle: "cycle"});
                    classe.periodes.all = _.without(classe.periodes.all, cycle);
                }
                setSearchPeriode(classe, currentPeriode);
                if ($location.path() === '/devoir/create' ||
                    ($scope.devoir !== undefined && ($location.path() === "/devoir/" + $scope.devoir.id + "/edit"))) {
                    $scope.devoir.id_periode = currentPeriode !== null ? currentPeriode.id_type : null;
                    $scope.controleDate($scope.devoir);
                }
                if($location.path() === '/releve') {
                    $scope.filteredPeriode = $filter('customPeriodeTypeFilter')($scope.structure.typePeriodes.all, $scope.search);
                    $scope.setMatieresFiltered();
                    $scope.getReleve()
                }
                utils.safeApply($scope);
            }
        };

        $scope.synchronizeStudents = (idClasse): boolean => {
            if (idClasse) {
                if($scope.search.classe == undefined)
                    $scope.search.classe = evaluations.structure.classes.findWhere({id: idClasse});
                evaluations.structure.cycle = _.findWhere(evaluations.structure.cycles,
                    {id_cycle: $scope.search.classe.id_cycle});
                $scope.structure.cycle = evaluations.structure.cycle;
                utils.safeApply($scope);
                if (!$scope.search.classe.remplacement && $scope.search.classe.eleves
                    && $scope.search.classe.eleves.length() === 0) {
                    $scope.search.classe.eleves.sync().then(() => {
                        utils.safeApply($scope);
                        $scope.search.classe.trigger('synchronize-students');
                        return true;
                    });
                }
                return false;
            }
        };

        $scope.confirmerDuplication = () => {
            if ($scope.selected.devoirs.list.length === 1) {
                let devoir: Devoir = $scope.selected.devoirs.list[0];
                devoir.duplicate($scope.selected.classes).then(() => {
                    $scope.devoirs.sync().then(() => {
                        $scope.filteredDevoirs = _.filter($scope.devoirs.all, devoir => {
                            devoir.nameClass = $scope.getClasseData(devoir.id_groupe, 'name');
                            return $scope.filterValidDevoir(devoir);
                        });
                        $scope.resetSelected();
                        $scope.opened.lightboxs.duplication = false;
                    });
                }).catch(() => {
                    notify.error(lang.translate('evaluation.duplicate.devoir.error'));
                });
            }
        };

        $scope.switchVisibilityApprec = async (devoir: Devoir) => {
            try {
                await devoir.switchVisibilityApprec();
            } catch (e) {
                console.error(e);
            } finally {
                utils.safeApply($scope);
            }
        };

        $scope.changeEtablissement = async () => {
            // Angular 1.7.9 <select> now change the reference of our $scope evaluations.structures
            // We reassign the $scope with the ng-option element structures.all selected in order to keep the same reference
            evaluations.structure = $scope.evaluations.structures.all.find(s => s.id ===  evaluations.structure.id);

            let init = () => {
                $scope.initReferences();
                $scope.search = $scope.initSearch();

                utils.safeApply($scope);
            };

            let initFieldOfDevoir = async () => {
                if (evaluations.structure.classes.empty()) {
                    await evaluations.structure.classes.sync();
                }

                if ($location.path() === '/devoir/create') {
                    $scope.devoir.id_groupe = $scope.searchOrFirst("classe", evaluations.structure.classes.all).id;

                    $scope.devoir.matiere = undefined;
                    $scope.setEnseignantMatieres();

                    if($scope.devoir.matiere.sousMatieres !== undefined && $scope.devoir.matiere.sousMatieres.all.length > 0) {
                        $scope.devoir.id_sousmatiere = $scope.devoir.matiere.sousMatieres.all[0].id_type;
                    }

                    $scope.devoir.id_type = $scope.searchOrFirst("type", evaluations.structure.types.all).id;
                    let currentPeriode = await $scope.getCurrentPeriode(
                        _.findWhere($scope.structure.classes.all, {id: $scope.devoir.id_groupe}));
                    $scope.devoir.id_periode = currentPeriode !== null ? currentPeriode.id_type : null;
                    if ($scope.devoir.id_periode == null
                        && $scope.search.periode && $scope.search.periode !== "*") {
                        $scope.devoir.id_periode = $scope.search.periode.id_type;
                        utils.safeApply($scope);
                    }
                    await $scope.loadEnseignementsByClasse(true);
                    await $scope.controleDate($scope.devoir);
                    $scope.devoir.enseignements = evaluations.structure.enseignements;
                    $scope.updateFilter();

                    utils.safeApply($scope);
                }

                $scope.structure.classes = evaluations.structure.classes;
                $scope.opened.displayStructureLoader = false;
                utils.safeApply($scope);
            };

            $scope.structure = evaluations.structure;

            if (!evaluations.structure.isSynchronized) {
                $scope.opened.displayStructureLoader = true;
                evaluations.structure.sync().then(async () => {
                    await init();
                    await initFieldOfDevoir();
                });
            } else {
                await init();
                await initFieldOfDevoir();
            }
        };

        $scope.updateFilter = function () {
            // tableau des connaissances à cocher éventuellement
            let parentToCheck = [];

            for (let i = 0; i < $scope.evaluations.competencesDevoir.length; i++) {
                for (let j = 0; j < $scope.evaluations.enseignements.all.length; j++) {
                    if ($scope.competencesFilter[$scope.evaluations.competencesDevoir[i].id_competence + '_'
                    + $scope.evaluations.enseignements.all[j].id] !== undefined) {
                        // selection des competences du devoir
                        $scope.competencesFilter[$scope.evaluations.competencesDevoir[i].id_competence
                        + '_' + $scope.evaluations.enseignements.all[j].id].isSelected = true;

                        $scope.evaluations.competencesDevoir[i].id
                            = $scope.evaluations.competencesDevoir[i].id_competence;
                        $scope.devoir.competences.all[i].id = $scope.devoir.competences.all[i].id_competence;

                        // remplissage des connaissances parent  à cocher éventuellement
                        let parentCo = $scope.competencesFilter[$scope.evaluations.competencesDevoir[i].id_parent
                        + '_' + $scope.evaluations.enseignements.all[j].id];
                        if (parentToCheck.indexOf(parentCo) === -1 && parentCo !== undefined) {
                            parentToCheck.push(parentCo);
                        }

                        utils.safeApply($scope);
                    }
                }
            }

            // On coche la connaissance si elle n'a aucun fils sélectionné
            for (let i = 0; i < parentToCheck.length; i++) {
                let checkIt = true;
                for (let j in  $scope.competencesFilter) {
                    if ($scope.competencesFilter.hasOwnProperty(j)) {
                        let currComp = $scope.competencesFilter[j];
                        if (currComp !== undefined &&
                            currComp.data.id_parent === parentToCheck[i].data.id) {
                            checkIt = currComp.isSelected || currComp.data.masque;
                        }
                        // si on rencontre un fils non selectionné on arrête de chercher
                        if (!checkIt) {
                            break;
                        }
                    }
                }
                if (checkIt) {
                    parentToCheck[i].isSelected = true;
                    parentToCheck[i].id = parentToCheck[i].id_competence;
                }
                else {
                    parentToCheck[i].isSelected = false;
                    parentToCheck[i].id = parentToCheck[i].id_competence;
                }
                // depliement de l'enseignement pour les compétences sélectionnées
                // du devoir à modifier
                let enseignementToOpen = $scope.devoir.enseignements.all.find(
                    function (elem) {
                        return elem.id === parentToCheck[i].data.id_enseignement
                    });
                enseignementToOpen.open = true;

                // depliement des connaissances parent des compétences du devoir à modifier
                parentToCheck[i].data.open = true;
                parentToCheck[i].open = true;
                utils.safeApply($scope);
            }

        };
        $scope.clearScope = () => {
            delete $scope.releveNote;
        };

        $scope.switchStructureCreation = () => {
            let structure = evaluations.structures.findWhere({id: $scope.devoir.id_etablissement});
            if (structure !== undefined) {
                evaluations.structure = structure;
                $scope.changeEtablissement();
            }
        };

        $scope.annulerDuplication = () => {
            $scope.selected.classes = [];
            $scope.opened.lightboxs.duplication = false;
        };

        $scope.getClassesByIdCycle = (type_groupe) => {
            let currentIdGroup = $scope.selected.devoirs.list[0].id_groupe;
            let targetIdCycle = _.find($scope.classes.all, {id: currentIdGroup}).id_cycle;

            return _.filter($scope.classes.all, function (classe) {
                return classe.id_cycle === targetIdCycle && classe.id !== currentIdGroup
                    && type_groupe === classe.type_groupe;
            });
        };

        $scope.filterSearchDuplication = () => {
            return function (classe) {
                if ($scope.search.duplication === '') return true;
                else return classe.name.indexOf($scope.search.duplication) !== -1;
            };
        };

        /**
         * Ajoute la classe qui vient
         * @param selectedClasseId Identifiant de la classe sélectionnée
         */
        $scope.selectClasse = function (selectedClasseId: string) {
            let classe = $scope.classes.findWhere({id: selectedClasseId});
            if(!_.contains(_.pluck($scope.selected.classes, 'id'), selectedClasseId)) {
                if (classe !== undefined) {
                    $scope.selected.classes.push({
                        id: selectedClasseId,
                        type_groupe: classe.type_groupe
                    });
                }
            } else {
                $scope.selected.classes = _.reject($scope.selected.classes, (classe) => {
                    return classe.id === selectedClasseId;
                });
            }
        };

        $scope.isSelected = function (id) {
            return _.indexOf($scope.selected.classes, id) !== -1;
        };

        // for (let i = 0; i < evaluations.classes.all.length; i++) {
        //     let elevesOfclass = _.map(evaluations.classes.all[i].eleves.all, function(eleve) {
        //         if((_.findWhere($scope.eleves, {id: eleve.id})) === undefined) {
        //             return _.extend(eleve, {
        //                     classEleve : evaluations.classes.all[i]
        //                 }
        //             );
        //         }
        //     });
        //     $scope.eleves = _.union($scope.eleves,  _.without(elevesOfclass, undefined));
        // }

        /**
         * cette function permet d'extraire les competences evalué du devoir
         * @param Skills : les competences du devoir
         * @param Devoir : le devoir à examiner
         * @returns {Array} of skills
         */
        $scope.evaluationOfSkilles = function (Skills, Devoir) {
            let Myarray = [];

            if (Skills.all.length > 0) {
                for (let i = 0; i < Skills.all.length; i++) {
                    let isEvaluated = false;
                    _.map(Devoir.eleves.all, function (eleve) {
                        if (eleve.evaluation.competenceNotes.findWhere({id_competence: Skills.all[i].id_competence}).evaluation !== -1) {
                            isEvaluated = true;
                        }
                    });
                    if (isEvaluated)
                        Myarray.push(Skills.all[i]);
                }
                return Myarray;
            }
        };

        $scope.afficherRecap = function () {
            if ($scope.opened.accOp === 1) {
                $scope.opened.accOp = 0;
            } else {
                $scope.opened.accOp = 1;
            }
        };

        $scope.confirmSuppression = function () {
            if ($scope.selected.devoirs.list.length > 0) {
                $scope.devoirsUncancelable = [];
                if (!Utils.isChefEtabOrHeadTeacher()) {
                    _.map($scope.selected.devoirs.list, async function (devoir) {
                        let isEndSaisieDevoir = await $scope.checkEndSaisieSeul(devoir);
                        let isHeadTeacher = Utils.isHeadTeacher(
                            _.findWhere(evaluations.structure.classes.all, {id: devoir.id_groupe }));
                        if (isEndSaisieDevoir && !isHeadTeacher) {
                            $scope.selected.devoirs.list = _.without($scope.selected.devoirs.list, devoir);
                            devoir.selected = false;
                            $scope.devoirsUncancelable.push(devoir);
                            utils.safeApply($scope);
                        }
                    });
                }
                $scope.opened.evaluation.suppressionMsg1 = true;

                utils.safeApply($scope);
            }
        };
        $scope.textSuppressionMsg2 = {
            Text1: lang.translate('evaluations.devoir.recaputilatif.suppression.text1'),
            Text2: lang.translate('evaluations.devoir.recaputilatif.suppression.text2'),
            Text3: lang.translate('evaluations.devoir.recaputilatif.suppression.text3'),
            Text4: lang.translate('evaluations.devoir.recaputilatif.suppression.text4'),
            Text5: lang.translate('evaluations.devoir.recaputilatif.suppression.text5'),
            Text6: lang.translate('evaluations.devoir.recaputilatif.suppression.text6'),
            TexTUncancelable: lang.translate('evaluations.devoir.recaputilatif.suppression.Uncacelable'),
            TextFin: lang.translate('evaluations.devoir.recaputilatif.suppression.confirmation')
        };


        $scope.firstConfirmationSuppDevoir = function () {
            if ($scope.selected.devoirs.list.length > 0) {

                let idDevoir = [];
                _.map($scope.selected.devoirs.list, function (devoir) {
                    if ($scope.checkEndSaisie(devoir)) {
                        idDevoir.push(devoir.id);
                    }
                });

                //verification si le/les devoirs ne contiennent pas une compétence evaluée
                $scope.devoirs.areEvaluatedDevoirs(idDevoir).then((res) => {

                    $scope.selected.devoirs.listwithEvaluatedSkills = [];
                    $scope.selected.devoirs.listwithEvaluatedMarks = [];
                    for (let i = 0; i < res.length; i++) {
                        if (res[i].nbevalskill > 0 && res[i].nbevalskill != null) {
                            $scope.selected.devoirs.listwithEvaluatedSkills.push(
                                {
                                    id: res[i].id,
                                    nbevalskill: res[i].nbevalskill,
                                    name: _.findWhere($scope.devoirs.all, {id: res[i].id}).name
                                });

                        }

                        if (res[i].nbevalnum > 0 && res[i].nbevalnum != null) {
                            $scope.selected.devoirs.listwithEvaluatedMarks.push({
                                id: res[i].id,
                                nbevalnum: res[i].nbevalnum,
                                name: _.findWhere($scope.devoirs.all, {id: res[i].id}).name
                            });

                        }
                    }
                    $scope.opened.evaluation.suppressionMsg1 = false;
                    if ($scope.selected.devoirs.listwithEvaluatedSkills.length > 0
                        || $scope.selected.devoirs.listwithEvaluatedMarks.length > 0) {
                        $scope.opened.evaluation.suppressionMsg2 = true;
                    } else {
                        $scope.deleteDevoir();
                    }

                    utils.safeApply($scope);
                });
            }
        };

        $scope.conditionAffichageText = function (NumText) {
            if (NumText === 1) {
                if (($scope.selected.devoirs.listwithEvaluatedSkills.length + $scope.selected.devoirs.listwithEvaluatedMarks.length) > 16 && $scope.selected.devoirs.listwithEvaluatedSkills.length !== 0 && $scope.selected.devoirs.listwithEvaluatedMarks.length !== 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (NumText === 2) {
                if ($scope.selected.devoirs.listwithEvaluatedMarks.length > 16 && $scope.selected.devoirs.listwithEvaluatedSkills.length === 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (NumText === 3) {
                if ($scope.selected.devoirs.listwithEvaluatedSkills.length > 16 && $scope.selected.devoirs.listwithEvaluatedMarks.length === 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (NumText === 4) {
                if ($scope.selected.devoirs.listwithEvaluatedSkills.length < 16 && $scope.selected.devoirs.listwithEvaluatedMarks.length === 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (NumText === 5) {
                if ($scope.selected.devoirs.listwithEvaluatedMarks.length < 16 && $scope.selected.devoirs.listwithEvaluatedSkills.length === 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (NumText === 6) {
                if (($scope.selected.devoirs.listwithEvaluatedSkills.length + $scope.selected.devoirs.listwithEvaluatedMarks.length) < 16 && $scope.selected.devoirs.listwithEvaluatedSkills.length !== 0 && $scope.selected.devoirs.listwithEvaluatedMarks.length !== 0) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }

        };
        $scope.annulerSuppression = function () {
            $scope.opened.evaluation.suppressionMsg2 = false;
            $scope.opened.evaluation.suppressionMsg1 = false;
        };

        $scope.releveNote = undefined;
        if ($scope.devoirs !== undefined) {
            evaluations.devoirs.on('sync', function () {
                $scope.mapIdLibelleDevoir = _.object(_.map($scope.devoirs.all, function (item) {
                    return [item.id, item.name];
                }));
            });
        } else {
            console.log("Devoirs indéfinies, l'établissement ne doit pas être actif.");
        }

        if ($scope.classes !== undefined) {
            evaluations.classes.on('classes-sync', function () {
                utils.safeApply($scope);
            });
        } else {
            console.log("Classes indéfinies, l'établissement ne doit pas être actif.");
        }

        $scope.goTo = function (path, id) {
            $location.path(path);
            if (id != undefined)
                $location.search(id);
            $location.replace();
            utils.safeApply($scope);
        };

        $scope.resetSelected = function () {
            $scope.selected = {
                devoirs: {
                    list: [],
                    listwithEvaluatedSkills: [{}],
                    listwithEvaluatedMarks: [],
                    all: false
                },
                eleve: null,
                eleves: {
                    list: [],
                    all: false
                },
                competences: {
                    list: [],
                    all: false
                },
                classes: []
            };
        };

        /**
         * Initialise un nouveau devoir.
         */
        $scope.initDevoir = function () {
            return new Devoir({
                name: undefined,
                old_id_devoir: undefined,
                date_publication: new Date(),
                date: new Date(),
                diviseur: 20,
                coefficient: 1,
                id_etablissement: $scope.evaluations.structure.id,
                ramener_sur: false,
                id_etat: 1,
                owner: $scope.isChefEtabOrHeadTeacher() ? undefined :model.me.userId,
                matieresByClassByTeacher: [],
                teachersByClass : [],
                controlledDate: true,
                is_evaluated: false
            });
        };

        $scope.selectDevoir = function (devoir) {
            var index = _.indexOf($scope.selected.devoirs.list, devoir);
            if (index === -1) {
                $scope.selected.devoirs.list.push(devoir);
            } else {
                $scope.selected.devoirs.list = _.without($scope.selected.devoirs.list, devoir);
            }
        };

        function getCompetencesDataForPreferences(enseignement, competencesFilterArray) {
            let idCycle = $scope.getClasseData($scope.devoir.id_groupe, 'id_cycle');
            let evaluationCompetencesFilterPreferences = [];
            if(PreferencesUtils.isNotEmpty(evaluationCreationCompetences)){
                evaluationCompetencesFilterPreferences = PreferencesUtils.getPreferences(evaluationCreationCompetences);
                for(var keyOtherCycle in evaluationCompetencesFilterPreferences) {
                    if(!$scope.competencesFilter[keyOtherCycle] || ($scope.competencesFilter[keyOtherCycle] && $scope.competencesFilter[keyOtherCycle].data.id_cycle != idCycle))
                        competencesFilterArray[keyOtherCycle]  = {isSelected : evaluationCompetencesFilterPreferences[keyOtherCycle].isSelected};
                }
            }
            for(var keyCurrentCycle in $scope.competencesFilter) {
                if($scope.competencesFilter[keyCurrentCycle].data.id_cycle == idCycle)
                    competencesFilterArray[keyCurrentCycle]  = {isSelected : $scope.competencesFilter[keyCurrentCycle].isSelected};
            }
        }

        function getcompetencesDevoir(competencesDevoirArray: any[]) {
            $scope.evaluations.competencesDevoir.forEach(cp => {
                let dataToInsert  = {
                    data: cp.data,
                    code_domaine:  cp.code_domaine,
                    ids_domaine:  cp.ids_domaine,
                    id:  cp.id,
                    nom:  cp.nom,
                    id_parent: cp.id_parent,
                    id_type: cp.id_type,
                    id_enseignement:  cp.id_enseignement,
                    id_cycle: cp.id_cycle,
                    index: cp.index,
                    ismanuelle: cp.ismanuelle,
                    hasnameperso: cp.hasnameperso,
                    masque:  cp.masque,
                    ids_domaine_int: cp.ids_domaine_int,
                    callbacks: cp.callbacks,
                    selected:  cp.selected,
                };
                competencesDevoirArray.push(dataToInsert)
            })
        }

        function savePreferences() {
            let enseignementsFilterArray = [];
            let competencesFilterArray = {};
            let competencesDevoirArray = [];
            $scope.enseignements.all.forEach(enseignement => {
                let data = $scope.enseignementsFilter[enseignement.id];
                let enseignementToInsert = {
                    id: enseignement.id,
                    isSelected: data.isSelected
                };
                getCompetencesDataForPreferences(enseignement, competencesFilterArray);
                enseignementsFilterArray.push(enseignementToInsert);
            });

            let idCycle = $scope.getClasseData($scope.devoir.id_groupe, 'id_cycle');
            let evaluationCompetencesDevoirPreferences = [];
            if(PreferencesUtils.isNotEmpty(evaluationCreationCompetencesDevoir)){
                evaluationCompetencesDevoirPreferences = PreferencesUtils.getPreferences(evaluationCreationCompetencesDevoir);
                evaluationCompetencesDevoirPreferences.forEach(ecdp => {
                    if(ecdp.id_cycle != idCycle)
                        competencesDevoirArray.push(ecdp)
                })
            }

            getcompetencesDevoir(competencesDevoirArray);
            let arrayKeys = [], datasArray = [];
            datasArray.push(enseignementsFilterArray);
            datasArray.push(competencesFilterArray);
            datasArray.push(competencesDevoirArray);
            arrayKeys.push(evaluationCreationEnseignements);
            arrayKeys.push(evaluationCreationCompetences);
            arrayKeys.push(evaluationCreationCompetencesDevoir);

            PreferencesUtils.savePreferences(arrayKeys, datasArray);
        }

        $scope.lightboxChampsApparition = function () {
            if ($scope.controleNewDevoirForm() == true) {
                $scope.lightboxChampsObligatoire = true;
            } else {
                $scope.beforSaveDevoir();
            }
        };

        $scope.toMuchCompetences = function (): boolean {
            // checking existence competencesDevoir (case its undefined we set 0 as default value)
            return ($scope.evaluations.competencesDevoir ? $scope.evaluations.competencesDevoir.length : 0) > $scope.MAX_NBR_COMPETENCE;
        }

        /**
         * Controle la validité du formulaire de création d'un devoir
         * @returns {boolean} Validité du formulaire
         */
        $scope.controleNewDevoirForm = function () {
            let name = $scope.devoir.name || '';
            return !(
                $scope.devoir.controlledDate
                && $scope.devoir.id_etablissement !== undefined
                && $scope.devoir.id_groupe !== undefined
                && $scope.devoir.id_matiere !== undefined
                && name.trim() !== ''
                && $scope.devoir.id_periode !== undefined
                && $scope.devoir.coefficient !== undefined
                && (!$scope.devoir.is_evaluated || $scope.devoir.coefficient !== null)
                && $scope.devoir.coefficient >= 0
                && $scope.devoir.diviseur !== undefined
                && $scope.devoir.diviseur > 0
                && $scope.devoir.id_type !== undefined
                && $scope.devoir.ramener_sur !== undefined
                && $scope.devoir.id_etat !== undefined
                && ($scope.devoir.is_evaluated
                    || $scope.evaluations.competencesDevoir.length > 0)
                && $scope.evaluations.competencesDevoir.length <= $scope.MAX_NBR_COMPETENCE
                && (($scope.devoir.owner && $scope.isChefEtabOrHeadTeacher()) || !$scope.isChefEtabOrHeadTeacher())
            );
        };

        $scope.initCoef = function () {
            if ($scope.devoir.is_evaluated) {
                let evalFormative = _.findWhere(evaluations.types.all, {nom: "Formative"});
                $scope.devoir.id_type === evalFormative.id.toString() ? $scope.devoir.coefficient = 0 : $scope.devoir.coefficient = 1;
            }
        };

        /**
         * Retourne la valeur de la clé i18n
         * @param key Clé i18n
         * @returns {any} Valeur i18n
         */
        $scope.translate = function (key) {
            return utils.translate(key);
        };

        /**
         * Permet de faire la jointure entre les directive de compétences cSkilllsColorColumn et cSkillsNoteDevoir
         */
        $scope.$on('majHeaderColumn', function (event, competence) {
            // $scope.$broadcast('changeHeaderColumn', competence);
        });

        /**
         * Retourne le nom de la structure en fonction de l'id de la structure
         * @param etabId Identifiant de la structure
         * @returns {any} Nom de la structure
         */
        $scope.getEtablissementName = function (etabId) {
            return model.me.structureNames[model.me.structures.indexOf(etabId)];
        };

        /**
         * Sélectionne/Déselectionne chaque objet de la liste
         * @param list liste d'objet
         * @param bool booleen
         */
        $scope.selectElement = function (list, bool) {
            for (var i = 0; i < list.length; i++) {
                if (bool !== undefined && list[i].selected === bool) continue;
                list[i].selected = !list[i].selected;
            }
        };


        /**
         * Sélectionne/Déselectionne tous les devoirs de l'utilisateur
         */
        $scope.selectAllDevoirs = function () {
            if ($scope.selected.devoirs.all !== true) {
                $scope.selectElement($scope.selected.devoirs.list, false);
                $scope.selected.devoirs.list = [];
                return;
            }
            $scope.selected.devoirs.list = $filter('customSearchFilters')($scope.devoirs.all, $scope.search);
            $scope.selectElement($scope.selected.devoirs.list, true);
        };

        /**
         * Récupère toutes les sous matière de la matière recherchée
         */
        $scope.getSousMatieres = function () {
            let matiere = evaluations.matieres.findWhere({id: $scope.search.matiere.id});
            if (matiere) $scope.selected.matiere = matiere;
        };


        /**
         *
         * Initialise tous les enseignements dans l'écran de filtre des compétences
         * lors de la création d'un devoir.
         *
         * @param pbInitSelected booleen indiuant si l'enseignement doit être sélectionnée ou non.
         */
        $scope.domaines = [];
        $scope.showCompetencesDomaine = {};
        $scope.displayFilterDomaine = false;

        function initCompetencesDevoir() {
            $scope.evaluations.competencesDevoir = [];
            $scope.devoir.competencesLastDevoirList = [];
            let idCycle = $scope.getClasseData($scope.devoir.id_groupe, 'id_cycle');
            let evaluationCompetencesDevoirPreferences = [];
            if(PreferencesUtils.isNotEmpty(evaluationCreationCompetencesDevoir)){
                evaluationCompetencesDevoirPreferences = PreferencesUtils.getPreferences(evaluationCreationCompetencesDevoir);
                evaluationCompetencesDevoirPreferences.forEach(ecdp => {
                    if(ecdp.id_cycle == idCycle)
                        $scope.devoir.competencesLastDevoirList.push(ecdp)
                })
            }
        }

        $scope.initFilter = function (pbInitSelected) {
            let evaluationCreationEnseignementsPreferences = [];
            if(PreferencesUtils.isNotEmpty(evaluationCreationEnseignements)  ){
                evaluationCreationEnseignementsPreferences = PreferencesUtils.getPreferences(evaluationCreationEnseignements)
            }
            $scope.enseignementsFilter = {};
            $scope.competencesFilter = {};
            $scope.domaines = [];
            $scope.showCompetencesDomaine = {};
            $scope.displayFilterDomaine = false;
            $scope.bSelectAllEnseignements = false;
            for (let i = 0; i < $scope.enseignements.all.length; i++) {
                let currEnseignement = $scope.enseignements.all[i];
                let isSelected = (evaluationCreationEnseignementsPreferences && evaluationCreationEnseignementsPreferences.length > 0
                    && evaluationCreationEnseignementsPreferences[i]) ? evaluationCreationEnseignementsPreferences[i].isSelected : true;
                if(isSelected === false){
                    $scope.bSelectAllEnseignements = true;
                }
                $scope.enseignementsFilter[currEnseignement.id] = {
                    isSelected: isSelected,
                    nomHtml: currEnseignement.nom
                };
                // on initialise aussi les compétences
                $scope.initFilterRec(currEnseignement.competences, pbInitSelected);
            }
            $scope.domaines = _.sortBy($scope.domaines, "code_domaine");
        };

        /**
         * Initialise le nom html des compétences (pour gérer le surlignement lors des recherches)
         *
         * @param poCompetences la liste des compétences
         * @param pbInitSelected boolean d'initialisation
         */

        $scope.initFilterRec = function (poCompetences, pbInitSelected) {
            if (poCompetences !== undefined) {
                let evaluationCreationCompetencesPreferences = [];
                if(PreferencesUtils.isNotEmpty(evaluationCreationCompetences)  ){
                    evaluationCreationCompetencesPreferences = PreferencesUtils.getPreferences( evaluationCreationCompetences);
                }
                let idCycle = $scope.getClasseData($scope.devoir.id_groupe, 'id_cycle');
                let _b = false;
                let comp: any = null;
                for (let i = 0; i < poCompetences.all.length; i++) {
                    let currCompetence = poCompetences.all[i];
                    if ((currCompetence.ids_domaine_int !== undefined && currCompetence.ids_domaine_int[0].lengh === 1 &&
                        $scope.showCompetencesDomaine[currCompetence.ids_domaine_int[0]] === true) || $scope.showCompetencesDomaine.length == undefined) {
                        comp = _.findWhere(poCompetences.all, {id: poCompetences.all[i].id}) !== undefined;
                        if (comp !== undefined) _b = false;
                        let key = currCompetence.id + "_" + currCompetence.id_enseignement;
                        if(evaluationCreationCompetencesPreferences && evaluationCreationCompetencesPreferences[key] &&
                            currCompetence.id_cycle == idCycle) {
                            _b = evaluationCreationCompetencesPreferences[key].isSelected;
                        }

                        $scope.competencesFilter[key] = {
                            isSelected:_b,
                            nomHtml: $scope.buildCompetenceNom(currCompetence),
                            data: currCompetence
                        };
                        $scope.initFilterRec(currCompetence.competences, pbInitSelected);
                    }
                }
            }
        };
        $scope.showCompetences = (eleve) => {
            return (eleve.evaluation.id_annotation === undefined
                    || eleve.evaluation.id_annotation === -1
                    || (eleve.evaluation.valeur === $scope.annotationNN && $scope.currentDevoir.is_evaluated))
                || (eleve.evaluation.valeur != "ABS" && eleve.evaluation.valeur != "DISP" && eleve.evaluation.valeur != "NR");
        }

        /**
         * Construis le nom d'une compétence préfixée de la codification du domaine dont elle est rattachée.
         * Si pas de domaine rattaché, seul le nom est retourné
         * @param poCompetence la compétence
         * @returns {le nom construis sous forme d'une chaine de caractères}
         */
        $scope.buildCompetenceNom = function (poCompetence) {
            if (poCompetence.code_domaine !== null && poCompetence.code_domaine !== undefined) {
                if (poCompetence.ids_domaine_int !== null && poCompetence.ids_domaine_int !== undefined
                    && poCompetence.ids_domaine_int.length === 1) {
                    let id_domaine = poCompetence.ids_domaine_int[0];
                    if (_.findIndex($scope.domaines, function (domaine) {
                        return domaine.id === id_domaine;
                    }) === -1) {
                        $scope.domaines.push({"code_domaine": poCompetence.code_domaine, "id": id_domaine});
                        $scope.showCompetencesDomaine[id_domaine] = true;
                    }
                }
                return poCompetence.code_domaine + " - " + poCompetence.nom;
            } else {
                return poCompetence.nom;
            }
        };

        /**
         * Methode qui determine si un enseignement doit être affiché ou non
         * (pour chaque enseignement on rentre dans cette fonction et on check le booleen stocké
         * dans le tableau  $scope.enseignementsFilter[])
         *
         * @param enseignement l'enseignement à tester
         * @returns {true si l'enseignement est sélectionné, false sinon.}
         */
        $scope.enseignementsFilterFunction = (enseignement) => {
            // si valeur est rensiegnée on la retourne sinon on considère qu'elle est sélectionné (gestion du CTRL-F5)
            if ($scope.enseignementsFilter !== undefined && $scope.enseignementsFilter[enseignement.id]
                && $scope.enseignementsFilter[enseignement.id].isSelected && enseignement.ids_domaine_int !== undefined
                && enseignement.ids_domaine_int.length > 0) {
                for (let i = 0; i < enseignement.ids_domaine_int.length; i++) {
                    if ($scope.showCompetencesDomaine[enseignement.ids_domaine_int[i]]) {
                        return true;
                    }
                }
            }
            return false;
        };

        $scope.enseignementsWithCompetences = (enseignement) => {
            return enseignement.competences.all.length > 0;
        };

        $scope.checkDomainesSelected = function (idDomaine) {
            $scope.showCompetencesDomaine[idDomaine] = !$scope.showCompetencesDomaine[idDomaine];
            let isAllDomainesSelected = true;
            for (let i = 0; i < $scope.domaines.length; i++) {
                if(!$scope.showCompetencesDomaine[$scope.domaines[i].id]) {
                    isAllDomainesSelected = false;
                }
            }
            $scope.selectAllDomaines = isAllDomainesSelected;
            $scope.setCSkillsList(false, false);
        }

        /**
         * Methode qui determine si une compétences doit être affichée ou non
         * (pour chaque compétence on rentre dans cette fonction et on check le booleen stocké
         * dans le tableau  $scope.enseignementsFilter[])
         *
         * @param compétence à tester
         * @returns {true si compétence est sélectionnée, false sinon.}
         */
        $scope.competencesByDomainesFilterFunction = (competence) => {
            // si valeur est rensiegnée on la retourne sinon on considère qu'elle est sélectionné (gestion du CTRL-F5)
            if ($scope.showCompetencesDomaine !== undefined) {
                if (competence.ids_domaine_int !== undefined) {
                    for (let i = 0; i < competence.ids_domaine_int.length; i++) {
                        if ($scope.showCompetencesDomaine[competence.ids_domaine_int[i]]) {
                            return true;
                        }
                    }
                } else {
                    // Par défaut on affiche la compétence
                    return true;
                }
            }
            return false;
        };

        $scope.hideHiddenCompetence = (competence) => {
            if (!_.isEmpty(competence.competences.all)) {
                return _.some(competence.competences.all, {masque : false});
            } else if (_.findWhere($scope.devoir.competences.all, {id_competence: competence.id})) {
                return true;
            } else {
                return !competence.masque;
            }
        };


        /**
         * Sélectionne/désélectionne tous les enseignements dans l'écran de filtre des compétences
         * lors de la création d'un devoir.
         *
         * @param pbIsSelected booleen pour sélectionner ou désélectionner les enseignements.
         */
        $scope.selectEnseignements = function (pbIsSelected) {
            for (let i = 0; i < $scope.enseignements.all.length; i++) {
                let currEnseignement = $scope.enseignements.all[i];
                $scope.enseignementsFilter[currEnseignement.id].isSelected = pbIsSelected;
            }
        };

        /**
         * Sélectionne/Désélectionne tous les enseignements dans l'écran de filtre des compétences
         * lors de la création d'un devoir.
         *
         */
        $scope.selectUnselectEnseignements = function () {
            $scope.selectEnseignements($scope.bSelectAllEnseignements);
            $scope.bSelectAllEnseignements = !$scope.bSelectAllEnseignements;
            $scope.setCSkillsList(false, false);
        };

        /**
         * affiche/masque toutes les compétences lors de la création d'un devoir.
         *
         * @param pbIsSelected booleen pour afficher ou masquer les compétences.
         */
        $scope.showHideDomaines = function () {
            $scope.selectAllDomaines = !$scope.selectAllDomaines;
            for (let i = 0; i < $scope.domaines.length; i++) {
                let currdomaine = $scope.domaines[i];
                $scope.showCompetencesDomaine[currdomaine.id] = $scope.selectAllDomaines;
            }
            $scope.setCSkillsList(false, false);
        };

        /**
         *
         * Methode qui determine si un enseignement doit être affiché ou non (selon le mot clef saisi)
         *
         * En realité on retourne toujours l'enseignement, il s'agit ici de savoir si on doit le déplier
         * en cas de match de mot clef ou si on le replie.
         *
         * @param psKeyword le mot clef recherché
         * @returns {function(enseignement): (retourne true systématiquement)}
         */
        $scope.enseignementsSearchFunction = (enseignement, psKeyword) => {
            if (!$scope.search.haschange) {
                return true;
            }

            // on check ici si l'enseignement  match le mot clef recherché pour éviter de rechecker
            // systématiquement dans la méthode récursive
            enseignement.open = utils.containsIgnoreCase(enseignement.nom, psKeyword);
            if (enseignement.open) {
                let nomHtml = $scope.highlight(enseignement.nom, psKeyword);
                // mise à jour que si la réelle valeur de la chaine html est différente ($sce.trustAsHtml renvoie systématiquement une valeur différente)
                if ($sce.getTrustedHtml($scope.enseignementsFilter[enseignement.id].nomHtml) !== $sce.getTrustedHtml(nomHtml)) {
                    $scope.enseignementsFilter[enseignement.id].nomHtml = nomHtml;
                }
            } else {
                $scope.enseignementsFilter[enseignement.id].nomHtml = enseignement.nom;
            }

            // Appel de la méthode récursive pour chercher dans les enseignements et compétences / sous compétences /
            // sous sous compétences / ...
            $scope.enseignementsSearchFunctionRec(enseignement, psKeyword);

            // dans tous les cas, à la fin, on retourne l'enseignement "racine"
            return true;
        };


        /**
         * Methode récursive qui determine si un enseignement / une compétence / une sous compétence / une sous sous compétence ...
         * match le mot clef recherché et doit être dépliée dans les résultats de recherche
         *
         * @param item un enseignement / une compétence / une sous compétence / une sous sous compétence / ...
         * @psKeyword le mot clef recherché
         */
        $scope.enseignementsSearchFunctionRec = function (item, psKeyword) {
            // Condition d'arret de l'appel récursif : pas de sous compétences (on est sur une feuille de l'arbre)
            if (item.competences != undefined) {
                // Parcours de chaque compétences / sous compétences
                for (let i = 0; i < item.competences.all.length; i++) {
                    let sousCompetence = item.competences.all[i];
                    let matchDomaine = false;

                    // check si la compétence / sous compétence match le mot clef
                    // on la déplie / replie en conséquence
                    sousCompetence.open = utils.containsIgnoreCase(sousCompetence.nom, psKeyword);

                    if (sousCompetence.code_domaine != null) {
                        if (matchDomaine = utils.containsIgnoreCase(sousCompetence.code_domaine, psKeyword))
                            sousCompetence.open = true;
                    }

                    if (sousCompetence.open) {
                        let nomHtml = $scope.highlight(sousCompetence.nom, psKeyword);
                        let DisplayNomSousCompetence = nomHtml;

                        if (sousCompetence.code_domaine != null) {
                            let nomDomaine;
                            if (matchDomaine) {
                                nomDomaine = $scope.highlight(sousCompetence.code_domaine, psKeyword);
                            } else {
                                nomDomaine = sousCompetence.code_domaine;
                            }
                            DisplayNomSousCompetence = nomDomaine + " - " + nomHtml;
                        }
                        // mise à jour que si la réelle valeur de la chaine html est différente ($sce.trustAsHtml renvoie systématiquement une valeur différente)
                        if ($sce.getTrustedHtml($scope.competencesFilter[sousCompetence.id + "_" + sousCompetence.id_enseignement].nomHtml) !== $sce.getTrustedHtml(nomHtml)) {
                            if ($scope.competencesFilter[sousCompetence.id + "_" + sousCompetence.id_enseignement] !== undefined) {
                                $scope.competencesFilter[sousCompetence.id + "_" + sousCompetence.id_enseignement].nomHtml = DisplayNomSousCompetence;
                            }
                        }
                    } else {
                        if ($scope.competencesFilter[sousCompetence.id + "_" + sousCompetence.id_enseignement] !== undefined) {
                            $scope.competencesFilter[sousCompetence.id + "_" + sousCompetence.id_enseignement].nomHtml = $scope.buildCompetenceNom(sousCompetence);
                        }
                    }

                    // si elle match le mot clef on déplie également les parents
                    if (sousCompetence.open) {
                        item.open = true;
                        var parent = item.composer;

                        while (parent !== undefined) {
                            parent.open = true;
                            parent = parent.composer;
                        }
                    }

                    // et on check sur les compétences de l'item en cours de parcours
                    $scope.enseignementsSearchFunctionRec(sousCompetence, psKeyword)
                }
            }
        };

        /**
         * Retourne une chaine avec toutes les occurences du mot clef trouvées surlignées (encadrement via des balises html)
         *
         * @param psText le texte où rechercher
         * @param psKeyword le mot clef à rechercher
         * @returns le texte avec les occurences trouvées surlignées
         */
        $scope.highlight = function (psText, psKeyword) {
            var psTextLocal = psText;

            if (!psKeyword) {
                return $sce.trustAsHtml(psText);
            }
            return $sce.trustAsHtml(psTextLocal.replace(new RegExp(psKeyword, 'gi'), '<span class="highlightedText">$&</span>'));
        };


        /**
         * Charge les enseignements et les compétences en fonction de la classe.
         */
        $scope.loadEnseignementsByClasse = function (changeEtab?) {
            let classe_Id = $scope.devoir.id_groupe;
            let newIdCycle = $scope.getClasseData(classe_Id, 'id_cycle');
            if (newIdCycle === null) {
                $scope.oldCompetencesDevoir = _.extend(evaluations.competencesDevoir);
                evaluations.competencesDevoir = [];
                $scope.cleanItems = true;
                utils.safeApply($scope);
                return;
            }
            let currentIdCycle = null;
            for (let i = 0; i < $scope.enseignements.all.length && currentIdCycle === null; i++) {
                if ($scope.enseignements.all[i].data.competences_1 !== undefined &&
                    $scope.enseignements.all[i].data.competences_1 !== null) {
                    for (let j = 0; j < $scope.enseignements.all[i].data.competences_1.length
                    && currentIdCycle === null; j++) {
                        currentIdCycle = $scope.enseignements.all[i].data.competences_1[j].id_cycle;
                    }
                }
            }
            if (currentIdCycle !== newIdCycle || changeEtab === true) {
                evaluations.enseignements.sync(classe_Id).then(function () {
                    // suppression des compétences qui n'appartiennent pas au cycle
                    if($scope.devoir.enseignements.all.length === 0) {
                        _.extend($scope.devoir.enseignements, $scope.enseignements);
                    }
                    $scope.initFilter(true);
                    $scope.setCSkillsList(false, true);
                    if ($location.path() === "/devoir/create") {
                        initCompetencesDevoir();
                    }
                    //$scope.$broadcast("test");
                    utils.safeApply($scope);
                });
            }
            else if ($scope.cleanItems && $scope.oldCompetencesDevoir !== undefined) {
                evaluations.competencesDevoir = $scope.oldCompetencesDevoir;
                $scope.cleanItems = false;
            }
        };

        $scope.setCSkillsList = (searchChanged, enseignementsChanged) => {
            if(enseignementsChanged){
                $scope.filteredEnseignements = _.filter($scope.devoir.enseignements.all, enseignement => {
                    return $scope.enseignementsWithCompetences(enseignement);
                });
            }

            let enseignements = angular.copy($scope.devoir.enseignements.all); // Variable intermédiaire pour ne pas réécraser les compétences avec les filtres ci-dessous
            $scope.cSkillsListData = _.filter(enseignements, (enseignement) => {
                enseignement.competences.all = _.filter(enseignement.competences.all, (competence) => {
                    competence.competences.all = _.filter(competence.competences.all, (comp) => {
                        return $scope.hideHiddenCompetence(comp)
                            && $scope.competencesByDomainesFilterFunction(comp);
                    });
                    return $scope.hideHiddenCompetence(competence)
                        && $scope.competencesByDomainesFilterFunction(competence);
                });

                if(searchChanged) {
                    return $scope.enseignementsSearchFunction(enseignement, $scope.search.keyword)
                        && $scope.enseignementsFilterFunction(enseignement);
                } else {
                    return $scope.enseignementsFilterFunction(enseignement);
                }
            });

            utils.safeApply($scope);
        }

        /**
         * Séquence de création d'un devoir
         */
        $scope.createDevoir = async () => {
            if ($location.path() === "/devoir/create") {
                $scope.devoir = $scope.initDevoir();
                let classes = _.filter($scope.structure.classes.all, (classe) => {
                    return $scope.isValidClasseMatiere(classe.id)
                });
                if(!_.isEmpty(classes)) {
                    $scope.devoir.id_groupe = $scope.searchOrFirst("classe", classes).id;
                    $scope.devoir.id_type = _.findWhere($scope.structure.types.all, {default_type: true}).id;
                    $scope.setClasseEnseignants($scope.search);

                    let currentPeriode = await $scope.getCurrentPeriode(_.findWhere(classes,
                        {id: $scope.devoir.id_groupe}));
                    $scope.devoir.id_periode = currentPeriode !== null ? currentPeriode.id_type : null;
                    if ($scope.devoir.id_periode == null && $scope.search.periode && $scope.search.periode !== "*") {
                        $scope.devoir.id_periode = $scope.search.periode.id_type;
                        await utils.safeApply($scope);
                    }
                    $scope.hideCreation = false;
                }
                else {
                    $scope.hideCreation = true;
                    template.open('main', 'enseignants/creation_devoir/display_creation_devoir');
                    await utils.safeApply($scope);
                    return;
                }
            }

            //$scope.opened.lightbox = true;
            $scope.controlledDate = (moment($scope.devoir.date_publication).diff(moment($scope.devoir.date), "days") <= 0);
            // resynchronisation de la liste pour eviter les problemes de references et de checkbox precedements cochees
            $scope.search.keyword = "";
            // si le mot clef de recherche n'a pas changé c'est qu'on rentre dans le filtre lors d'un autre
            // evenement (depliement/repliement d'un compétence par exemple)
            // on ne réaplique pas le filtre dans ce cas car on veut déplier l'élément sur lequel on a cliqué
            $scope.$watch('search.keyword', function (newValue, oldValue) {
                $scope.search.haschange = (newValue !== oldValue);
            }, true);

            evaluations.competencesDevoir = [];

            if ($location.path() === "/devoir/create") {
                $scope.devoir.getLastSelectedCompetence().then(function (res) {
                    $scope.devoir.competencesLastDevoirList = res;
                });
            }

            if ($scope.devoir.id_type === undefined) {
                $scope.devoir.id_type = _.findWhere($scope.structure.types, {default_type: true});
            }

            if ($scope.devoir.id_groupe === undefined) {
                if ($scope.search.classe !== null && $scope.search.classe !== undefined
                    && $scope.search.classe.id !== '*' && $scope.search.matiere !== '*') {
                    $scope.devoir.id_groupe = $scope.search.classe.id;
                    $scope.devoir.id_matiere = $scope.search.matiere.id;

                } else {
                    // selection de la premiere classe par defaut
                    $scope.devoir.id_groupe = $scope.classes.all[0].id;
                }
                //$scope.selectedMatiere($scope.devoir);
            }

            let selectedClasse = _.findWhere($scope.classes.all, {id: $scope.devoir.id_groupe});
            if (selectedClasse !== undefined && selectedClasse.id_cycle !== null) {
                $scope.structure.enseignements.sync($scope.devoir.id_groupe).then(() => {
                    _.extend($scope.devoir.enseignements, $scope.enseignements);
                    $scope.initFilter(true);
                    $scope.setCSkillsList(false, true);
                    if ($location.path() === "/devoir/create") {
                        initCompetencesDevoir();
                    }
                    for (let i = 0; i < $scope.devoir.competences.all.length; i++) {
                        $scope.evaluations.competencesDevoir.push($scope.devoir.competences.all[i]);
                    }
                    if ($scope.devoir.hasOwnProperty('id')) {
                        $scope.updateFilter();
                    }
                });
            }

            if ($scope.devoir.dateDevoir === undefined && $scope.devoir.date !== undefined) {
                $scope.devoir.dateDevoir = new Date($scope.devoir.date);
            }

            if ($location.path() === "/devoirs/list") {
                $scope.devoir.id_type = $scope.search.type.id;
                $scope.devoir.id_sousmatiere = $scope.search.sousmatiere.id_type_sousmatiere;
            }

            if ($location.path() !== "/devoir/" + $scope.devoir.id + "/edit") {
                template.open('main', 'enseignants/creation_devoir/display_creation_devoir');
                utils.safeApply($scope);
            }
        };

        // on ecoute sur l'evenement checkConnaissances
        // ie on doit ajouter/supprimer toutes les sous competences dans le recap
        $scope.$on('checkConnaissances', function (event, parentItem) {
            parentItem.competences.each(function (e) {
                if (e.masque && _.findWhere($scope.devoir.competences.all, {id: e.id}) === undefined) {
                    return;
                } else if ($scope.competencesFilter[parentItem.id + "_" + parentItem.id_enseignement].isSelected === true) {
                    // check si on a pas deja ajoute pour eviter les doublons
                    var competence = _.findWhere(evaluations.competencesDevoir, {id: e.id});

                    // on ajoute que si la compétence n'existe pas (cela peut arriver si on a la même compétence sous un ensignement différent par exemple)
                    if (competence === undefined) {
                        //if(!_.contains(evaluations.competencesDevoir, e)) {
                        let _competencesDevoir = e;
                        _competencesDevoir.index = evaluations.competencesDevoir.length;
                        evaluations.competencesDevoir.push(_competencesDevoir);
                    }
                } else {
                    evaluations.competencesDevoir = _.reject(evaluations.competencesDevoir, function (comp) {
                        return comp.id === e.id;
                    });
                }
            });
        });

        // on ecoute sur l'evenement checkParent
        // ie on doit ajouter la sous competence selectionnee dans le recap
        $scope.$on('checkParent', function (event, parentItem, item) {
            if ($scope.competencesFilter[item.id + "_" + item.id_enseignement].isSelected === true) {
                // check si on a pas deja ajoute pour eviter les doublons
                var competence = _.findWhere(evaluations.competencesDevoir, {id: item.id});

                // on ajoute que si la compétence n'existe pas (cela peut arriver si on a la même compétence sous un ensignement différent par exemple)
                if (competence === undefined) {
                    // if(!_.contains(evaluations.competencesDevoir, item)) {
                    evaluations.competencesDevoir.push(item);
                }
            } else {
                evaluations.competencesDevoir = _.reject(evaluations.competencesDevoir, function (comp) {
                    return comp.id === item.id;
                });
            }
        });

        // create the timer variable
        var timer;

        // mouseenter event
        $scope.showIt = function (item) {
            timer = $timeout(function () {
                item.hoveringRecap = true;
            }, 350);
        };

        // mouseleave event
        $scope.hideIt = function (item) {
            $timeout.cancel(timer);
            item.hoveringRecap = false;
        };

        /**
         * Récupère les matières enseignées sur la classe donnée
         * @param idClasse Identifiant de la classe
         * @returns {Promise<T>} Promesse de retour
         */
        /* let getClassesMatieres = function (idClasse) {
              return new Promise((resolve, reject) => {
                  let classe = $scope.classes.findWhere({id: idClasse});
                  if (classe !== undefined) {
                      if (resolve && typeof resolve === 'function') {
                          resolve($scope.matieres.filter((matiere) => {
                              return (matiere.libelleClasses.indexOf(classe.externalId) !== -1)
                          }));
                      }
                  } else {
                      reject();
                  }
              });
          };*/

        $scope.searchOrFirst = (key, collection) => {
            if ($scope.search[key] && $scope.search[key] !== "*") {
                return $scope.search[key];
            } else {
                return _.first(collection);
            }
        };

        /**
         * Set les enseignants en fonction de l'identifiant de la classe
         */
        $scope.setClasseEnseignants = function (search ?) {
            //si c'est un chef étab on va chercher les enseignants
            if(Utils.isChefEtabOrHeadTeacher()) {
                $scope.devoir.teachersByClass = $filter('getEnseignantClasse')($scope.structure.enseignants.all,
                    $scope.devoir.id_groupe, $scope.classes);
                if ($scope.devoir.owner === undefined && search !== undefined &&
                    search.matiere != null && search.classe != null) {
                    if (search.enseignant != null) {
                        $scope.devoir.owner = search.enseignant.id;
                    } else {
                        let teacher = _.findWhere(search.classe.services,
                            {id_groupe: search.classe.id, id_matiere: search.matiere.id});
                        $scope.devoir.owner = (teacher != undefined) ? teacher.id_enseignant
                            : $scope.devoir.teachersByClass[0].id;
                    }
                } else if (($scope.devoir.teachersByClass.length > 0 && $scope.devoir.owner === undefined) ||
                    _.findWhere($scope.devoir.teachersByClass, {id: $scope.devoir.owner}) === undefined) {
                    if ($scope.devoir.teachersByClass.length > 0) {
                        $scope.devoir.owner = $scope.devoir.teachersByClass[0].id;
                    }
                }
            } else { //Sinon c'est le professeur connecté qui est le créateur du devoir
                $scope.devoir.owner = model.me.userId;
            }
            $scope.setEnseignantMatieres();
        };

        /**
         * Set les matières en fonction de l'identifiant de l'enseignant choisi
         */
        $scope.setEnseignantMatieres = function () {
            $scope.devoir.matieresByClassByTeacher = $filter('getMatiereClasse')($scope.structure.matieres.all,
                $scope.devoir.id_groupe, $scope.classes, $scope.devoir.owner);

            if(!$scope.devoir.matiere) {
                $scope.devoir.matiere = $scope.devoir.matieresByClassByTeacher[0];
            } else if(_.findWhere($scope.devoir.matieresByClassByTeacher, {id : $scope.devoir.matiere.id}) === undefined) {
                $scope.devoir.matiere = $scope.devoir.matieresByClassByTeacher[0];
            }

            if($scope.devoir.matiere) {
                $scope.devoir.id_matiere = $scope.devoir.matiere.id;
                if ($scope.devoir.matiere.sousMatieres && $scope.devoir.matiere.sousMatieres.all.length > 0) {
                    // attention sur le devoir on stocke l'id_type et non l'id de la sous matiere
                    $scope.devoir.id_sousmatiere = $scope.devoir.matiere.sousMatieres.all[0].id_type_sousmatiere;
                }
            }
            if($scope.devoir.owner && $scope.devoir.teachersByClass.length > 0) {
                $scope.devoir.owner_name = _.findWhere($scope.devoir.teachersByClass,
                    {id : $scope.devoir.owner}).displayName;
            }
        };

        $scope.deleteDevoir = function () {
            if ($scope.selected.devoirs.list.length > 0) {
                $scope.selected.devoirs.list.forEach(function (d) {
                        d.remove().then(() => {
                            evaluations.devoirs.sync().then(() => {
                                $scope.opened.lightbox = false;
                                var index = $scope.selected.devoirs.list.indexOf(d);
                                if (index > -1) {
                                    $scope.selected.devoirs.list = _.without($scope.selected.devoirs.list, d);
                                }
                                $scope.filteredDevoirs = _.filter($scope.devoirs.all, devoir => {
                                    devoir.nameClass = $scope.getClasseData(devoir.id_groupe, 'name');
                                    return $scope.filterValidDevoir(devoir);
                                });
                                utils.safeApply($scope);
                            });
                        }).catch(() => {
                            notify.error("evaluation.delete.devoir.error");
                        });
                    }
                );
            }
            $scope.opened.evaluation.suppressionMsg2 = false;
        };

        $scope.cancelUpdateDevoir = function () {
            $scope.firstConfirmSuppSkill = false;
            $scope.secondConfirmSuppSkill = false;
            $scope.evaluatedDisabel = false;
            $scope.opened.lightboxs.updateDevoir.firstConfirmSupp = false;
            $scope.opened.lightboxs.updateDevoir.secondConfirmSupp = false;
            $scope.opened.lightboxs.updateDevoir.evaluatedSkillDisabel = false;

        };
        $scope.ConfirmeUpdateDevoir = function () {
            if ($scope.opened.lightboxs.updateDevoir.firstConfirmSupp === true) {
                $scope.firstConfirmSuppSkill = true;
                if ($scope.evaluatedCompetencesSupp.length > 0) {
                    $scope.opened.lightboxs.updateDevoir.secondConfirmSupp = true;
                }
                $scope.opened.lightboxs.updateDevoir.firstConfirmSupp = false;
            } else if ($scope.opened.lightboxs.updateDevoir.secondConfirmSupp === true) {
                $scope.secondConfirmSuppSkill = true;
                $scope.opened.lightboxs.updateDevoir.secondConfirmSupp = false;
            } else if ($scope.opened.lightboxs.updateDevoir.evaluatedSkillDisabel) {
                $scope.evaluatedDisabel = true;
                $scope.opened.lightboxs.updateDevoir.evaluatedSkillDisabel = false;
            }
        };
        /**
         *
         */
        $scope.beforSaveDevoir = function () {
            $scope.competencesSupp = [];
            $scope.evaluatedCompetencesSupp = [];
            //
            if ($location.path() === "/devoir/" + $scope.devoir.id + "/edit") {
                //les compétences à supprimer
                for (let i = 0; i < $scope.allCompetences.all.length; i++) {
                    let maCompetence = _.findWhere(evaluations.competencesDevoir,
                        {id_competence: $scope.allCompetences.all[i].id_competence});

                    if (maCompetence === undefined) {
                        $scope.competencesSupp.push($scope.allCompetences.all[i]);
                    }
                }
                $scope.devoir.isEvaluatedDevoir($scope.devoir.id).then((res) => {
                    $scope.devoir.evaluationDevoirs;
                    //si il y a des competences à supprimer

                    if ($scope.competencesSupp.length > 0) {

                        //est ce que les competences sont evalué
                        let competence;
                        for (let i = 0; i < $scope.competencesSupp.length; i++) {
                            competence = _.findWhere($scope.devoir.evaluationDevoirs.all, {
                                id: String($scope.competencesSupp[i].id_competence),
                                typeeval: 'TypeEvalSkill'
                            });
                            if (competence !== undefined
                                && _.findWhere($scope.evaluations.competencesDevoir,
                                    {id: $scope.competencesSupp[i].id_competence}) === undefined) {
                                $scope.evaluatedCompetencesSupp.push($scope.competencesSupp[i]);
                            }
                        }
                        if ($scope.evaluatedCompetencesSupp.length > 0 || ($scope.devoir.is_evaluated === true && $scope.devoir.old_id_groupe !== undefined && $scope.devoir.old_id_groupe !== $scope.devoir.id_groupe)) {
                            $scope.opened.lightboxs.updateDevoir.firstConfirmSupp = true;
                            if ($scope.evaluatedCompetencesSupp.length == 0) {
                                $scope.secondConfirmSuppSkill = true;
                                $scope.evaluatedDisabel = true;
                            }
                        } else {
                            $scope.firstConfirmSuppSkill = true;
                            $scope.secondConfirmSuppSkill = true;
                        }
                    } else if ($scope.devoir.old_id_groupe !== undefined && $scope.devoir.old_id_groupe !== $scope.devoir.id_groupe) {
                        $scope.opened.lightboxs.updateDevoir.firstConfirmSupp = true;
                        $scope.secondConfirmSuppSkill = true;
                        $scope.evaluatedDisabel = true;
                    } else {
                        $scope.firstConfirmSuppSkill = true;
                        $scope.secondConfirmSuppSkill = true;
                    }
                });

                utils.safeApply($scope);

            } else {
                $scope.firstConfirmSuppSkill = true;
                $scope.secondConfirmSuppSkill = true;
                $scope.evaluatedDisabel = true;
            }

        };

        $scope.listnerSaveNewDevoir = function () {
            if ($scope.firstConfirmSuppSkill === true && $scope.secondConfirmSuppSkill === true && $scope.evaluatedDisabel === true) {
                $scope.saveNewDevoir();
                $scope.firstConfirmSuppSkill = false;
                $scope.secondConfirmSuppSkill = false;
                $scope.evaluatedDisabel = false;
            }
        };
        $scope.$watch(function () {
            return $scope.firstConfirmSuppSkill;
        }, function (newValue, oldValue) {
            if (newValue) {

                $scope.listnerSaveNewDevoir();
            }

        });
        $scope.$watch(function () {
            return $scope.secondConfirmSuppSkill;
        }, function (newValue, oldValue) {
            if (newValue) {
                if ($scope.firstConfirmSuppSkill === true && $scope.secondConfirmSuppSkill === true && $scope.evaluatedDisabel === false) {
                    if ($scope.oldIs_Evaluated === true && $scope.devoir.is_evaluated === false && (_.findWhere($scope.devoir.evaluationDevoirs.all, {typeeval: 'TypeEvalNum'}) !== undefined)) {
                        $scope.opened.lightboxs.updateDevoir.evaluatedSkillDisabel = true;
                    } else
                        $scope.evaluatedDisabel = true;
                }
                $scope.listnerSaveNewDevoir();
            }
        });
        $scope.$watch(function () {
            return $scope.evaluatedDisabel;
        }, function (newValue, oldValue) {
            if (newValue)
                $scope.listnerSaveNewDevoir();
        });
        /**
         *  Sauvegarde du devoir à la suite du formulaire de création
         */
        $scope.saveNewDevoir = function () {
            $scope.devoir.date = $scope.getDateFormated($scope.devoir.dateDevoir);
            $scope.devoir.date_publication = $scope.getDateFormated($scope.devoir.date_publication);

            // Pour la création on ne recupere que les id des competences
            if ($location.path() !== "/devoir/" + $scope.devoir.id + "/edit") {
                if (evaluations.competencesDevoir !== undefined) {
                    $scope.devoir.competences = [];

                    for (let i = 0; i < evaluations.competencesDevoir.length; i++) {

                        $scope.devoir.competences.push(evaluations.competencesDevoir[i].id);

                    }
                }
            }
            else {
                $scope.devoir.coefficient = parseFloat($scope.devoir.coefficient);
                if (evaluations.competencesDevoir !== undefined) {
                    $scope.devoir.competencesAdd = [];
                    $scope.devoir.competencesRem = [];
                    $scope.devoir.competencesUpdate = [];

                    //recherche des competences a ajouter
                    for (let i = 0; i < evaluations.competencesDevoir.length; i++) {
                        let toAdd = true;
                        for (let j = 0; j < $scope.devoir.competences.all.length; j++) {
                            if ($scope.devoir.competences.all[j].id
                                === evaluations.competencesDevoir[i].id) {
                                // si l'index de la compétence a changé, on doit
                                // mettre à jour la compétence.
                                $scope.devoir.competencesUpdate.push({
                                    id: evaluations.competencesDevoir[i].id,
                                    id_competence: evaluations.competencesDevoir[i].id,
                                    index: evaluations.competencesDevoir[i].index
                                });
                                toAdd = false;
                                break;
                            }
                        }
                        if (toAdd) {
                            $scope.devoir.competencesAdd.push({
                                id: evaluations.competencesDevoir[i].id,
                                id_competence: evaluations.competencesDevoir[i].id,
                                index: evaluations.competencesDevoir[i].index
                            });
                        }
                    }
                    //Remplissage des competences a supprimer

                    for (let j = 0; j < $scope.competencesSupp.length; j++) {
                        if (_.findWhere($scope.devoir.competencesUpdate,
                            {id: $scope.competencesSupp[j].id_competence}) === undefined) {
                            $scope.devoir.competencesRem.push($scope.competencesSupp[j].id_competence);
                        }
                    }
                }
                utils.safeApply($scope);
            }

            $scope.opened.displayMessageLoader = true;
            $scope.devoir.save($scope.devoir.competencesAdd,
                $scope.devoir.competencesRem, $scope.devoir.competencesUpdate).then((res) => {
                evaluations.structure.devoirs.sync().then(() => {
                    if ($location.path() === "/devoir/create") {
                        if (res !== undefined) {
                            $scope.filteredDevoirs = _.filter($scope.devoirs.all, devoir => {
                                devoir.nameClass = $scope.getClasseData(devoir.id_groupe, 'name');
                                return $scope.filterValidDevoir(devoir);
                            });
                            savePreferences();
                            let _devoir = evaluations.structure.devoirs.findWhere({id: res.id});
                            evaluations.structure.devoirs.getPercentDone(_devoir).then(async () => {
                                $location.path("/devoir/" + res.id);
                                await utils.safeApply($scope);
                            });
                        }
                    } else if ($location.path() === "/releve") {
                        $scope.opened.displayMessageLoader = false;
                        if ($scope.releveNote === undefined || !$scope.releveNote) {
                            $scope.search.classe.id = $scope.devoir.id_groupe;
                            $scope.search.matiere.id = $scope.devoir.id_matiere;
                            $scope.search.periode.id_type = $scope.devoir.id_periode;
                            $scope.getReleve();
                        } else {
                            $scope.releveNote.devoirs.sync();
                        }
                    }
                    else if ($location.path() === "/devoir/" + $scope.devoir.id + "/edit") {
                        $location.path("/devoir/" + $scope.devoir.id);
                    }
                    $scope.opened.lightbox = false;
                    utils.safeApply($scope);
                });
            });
        };

        /**
         * Déclenche un safeApply lors de l'event apply du model
         */
        model.on('apply', function () {
            utils.safeApply($scope);
        });

        $scope.openLeftMenu = function openLeftMenu(psMenu, pbAfficherMenu) {

            pbAfficherMenu = !pbAfficherMenu;

            if (psMenu === "openedDevoirInfo") {
                $scope.openedDevoirInfo = pbAfficherMenu;
            } else if (psMenu === "openedStudentInfo") {
                $scope.openedStudentInfo = pbAfficherMenu;
            } else if (psMenu === "opened.criteres") {
                $scope.opened.criteres = pbAfficherMenu;
            } else if (psMenu === "opened.legend") {
                $scope.opened.legend = pbAfficherMenu;
            } else if (psMenu === "opened.avis") {
                $scope.opened.avis = pbAfficherMenu;
            } else {
                console.error("Parametre psMenu inconnu : psMenu=" + psMenu);
            }


            // Dans le cas du relevé de notes, on replie les 2 autres menus dans
            // un problème d'espace vertical
            if ($location.$$path === '/releve') {

                if (pbAfficherMenu) {
                    if (psMenu === "openedDevoirInfo") {
                        $scope.openedStudentInfo = false;
                        $scope.opened.criteres = false;
                    }

                    if (psMenu === "openedStudentInfo") {
                        $scope.openedDevoirInfo = false;
                        $scope.opened.criteres = false;
                    }

                    if (psMenu === "opened.criteres") {
                        $scope.openedDevoirInfo = false;
                        $scope.openedStudentInfo = false;
                    }
                }
            }
        };
        /**
         *Afficher une lightbox 'page en cours de construction'
         */
        $scope.displayInConstruction = () => {
            $scope.modificationDevoir = true;
            utils.safeApply($scope);
        };
        /**
         *Fermer une lightbox 'page en cours de construction'
         */
        $scope.closeInConstruction = () => {
            $scope.modificationDevoir = false;
            utils.safeApply($scope);
        };
        /**
         * Séquence de récupération d'un relevé de note
         */
        /*  let oldExternalIdClassSearch:String = undefined;
          function initSubjectWhenSearchAnotherThing():any{
              if(oldExternalIdClassSearch === $scope.search.matiere.externalId) $scope.search.matiere = "*";
              oldExternalIdClassSearch = $scope.search.matiere.externalId;
          }*/

        $scope.getReleve = async function () {
            if (Utils.isNotNull($scope.releveNote)) {
                delete $scope.releveNote;
            }

            if ($scope.elementProgrammeDisplay !== undefined) {
                delete $scope.elementProgrammeDisplay;
            }

            if (Utils.isNotNull($scope.selected) && Utils.isNotNull($scope.selected.devoirs)
                && Utils.isNotNull($scope.selected.devoirs.list)) {
                for (let i = 0; i < $scope.selected.devoirs.list.length; i++) {
                    $scope.selected.devoirs.list[i].selected = false;
                }
                $scope.selected.devoirs.list = [];
            }

            if (Utils.isNotDefault($scope.search.classe) && $scope.search.classe.id !== undefined
                && Utils.isNotDefault($scope.search.matiere) && $scope.search.matiere.id !== undefined
                && $scope.search.periode !== '*' && $scope.search.matiere !== "*") {

                let p = {
                    idEtablissement: evaluations.structure.id,
                    idClasse: $scope.search.classe.id,
                    idMatiere: $scope.search.matiere.id,
                    idPeriode: null
                };

                if ($scope.search.periode) {
                    p.idPeriode = ($scope.search.periode.id_type) ? $scope.search.periode.id_type : $scope.search.periode.id;
                }

                $scope.opened.displayMessageLoader = true;
                let releve = new ReleveNote(p);
                releve.sync().then(async () => {
                    releve.synchronized.releve = true;
                    evaluations.releveNotes.push(releve);
                    $scope.releveNote = releve;
                    if ($scope.releveNote.elementProgramme !== undefined) {
                        $scope.elementProgrammeDisplay = $scope.releveNote.elementProgramme.texte;
                    }

                    // s'il n'ya que des devoirs sans note, on les masque par défaut
                    if (releve.isNN) {
                        // safeApply avant pour charger le DOM
                        await utils.safeApply($scope);
                        $scope.toogleDevoirNote();
                    }
                    $scope.opened.displayMessageLoader = false;

                    if(Utils.isNotNull($scope.informations) && Utils.isNotNull($scope.informations.eleve)) {
                        $scope.informations.eleve.appreciationCPE = null;
                    } else {
                        $scope.informations.eleve = $scope.releveNote.classe.eleves.all[0];
                    }
                    await $scope.getEleveInfo($scope.informations.eleve);

                    await utils.safeApply($scope);
                }).catch( async () => {
                    await Utils.stopMessageLoader($scope);
                });

                $scope.openedStudentInfo = false;
            }

            await utils.safeApply($scope);
        };

        $scope.setMatieresFiltered = () => {
            let idMatiereSelected = $scope.search.matiere != undefined ? $scope.search.matiere.id : undefined;
            delete $scope.search.matiere;
            if($scope.search.periode != '*' && $scope.search.periode != -1){
                $scope.matieresFiltered = _.unique($filter('getMatiereClasse')($scope.matieres.all,
                        $scope.search.classe ? $scope.search.classe.id : undefined, $scope.classes, model.me.userId), (mat) => {
                        return mat.id;
                    }
                );

                let matiere = _.findWhere($scope.matieresFiltered, {id : idMatiereSelected});
                if(matiere !== undefined){
                    $scope.search.matiere = matiere;
                } else if($scope.matieresFiltered.length === 1) {
                    $scope.search.matiere = $scope.matieresFiltered[0];
                }
            } else {
                $scope.matieresFiltered = [];
            }
            utils.safeApply($scope);
        }

        /**
         * Position l'objet matière sur le devoir en cours de création
         */
        $scope.selectedMatiere = function (devoir) {
            let matieres = $filter('getMatiereClasse')($scope.structure.matieres.all,
                $scope.devoir.id_groupe, $scope.classes, model.me.userId);
            if (matieres.length === 1){
                $scope.devoir.id_matiere = matieres[0].id;
            }
            let matiere = _.findWhere(matieres, {id: devoir.id_matiere});

            if (matiere !== undefined) {
                devoir.matiere = matiere;
                devoir.id_matiere = matiere.id;
            }

            // sélection de la 1ère sous matière par défaut
            if(matiere.sousMatieres !== undefined && matiere.sousMatieres.all.length > 0) {
                // attention sur le devoir on stocke l'id_type et non l'id de la sous matiere
                devoir.id_sousmatiere = matiere.sousMatieres.all[0].id_type_sousmatiere;
            }

            if( matiere.sousMatieres !== undefined && matiere.sousMatieres.all.length === 0
                && devoir.id_sousmatiere !== null){
                devoir.id_sousmatiere = null;
            }
        };

        /**
         * Controle le fonctionnement des filtres dates sur la vue liste des devoirs
         */
        $scope.controleDatePicker = function () {
            if (moment($scope.search.dateCreation.debut).diff(moment($scope.search.dateCreation.fin)) > 0) {
                $scope.search.dateCreation.fin = moment();
            }
            if (moment($scope.search.datePublication.debut).diff(moment($scope.search.datePublication.fin)) > 0) {
                $scope.search.datePublication.fin = moment();
            }
            utils.safeApply($scope);
        };

        /**
         * Format la date passée en paramètre
         * @param date Date à formatter
         * @returns {any|string} date formattée
         */
        $scope.getDateFormated = function (date) {
            return utils.getFormatedDate(date, "DD/MM/YYYY");
        };


        /**
         * Retourne la données de la classe passé en paramètre
         * @param idClasse identifiant de la classe
         * @param key clé à renvoyer
         * @returns {any} la valeur de la clé passée en paramètre
         */
        $scope.getClasseData = (idClasse, key) => {
            if ($scope.classes === undefined || idClasse === null || idClasse === '' || ($scope.evaluations.classes === undefined)
                || ($scope.classes.all.length === 0 && $scope.evaluations.classes.all.length === 0)) {
                return '';
            }
            let classe = $scope.classes.findWhere({id: idClasse});
            if (classe === undefined) {
                classe = $scope.evaluations.classes.findWhere({id: idClasse});
            }
            if (classe !== undefined && classe.hasOwnProperty(key)) {
                return classe[key];
            } else {
                return '';
            }
        };

        /**
         * Retourne le libelle de la période correspondant à l'identifiant passé en paramètre
         * @param idPeriode identifiant de la période
         * @returns {any} libelle de la période
         */
        $scope.getLibellePeriode = function (idPeriode) {
            if (idPeriode == null || idPeriode === "") return "";
            return _.findWhere($scope.periodes.all, {id: parseInt(idPeriode)}).libelle;
        };

        /**
         * Retourne le libelle du type de devoir correspondant à l'identifiant passé en paramètre
         * @param idType identifiant du type de devoir
         * @returns {any} libelle du type de devoir
         */
        $scope.getLibelleType = function (idType) {
            if (idType == null || idType === "") return "";
            let type = _.findWhere($scope.types.all, {id: parseInt(idType)});
            if (type !== undefined && type.hasOwnProperty('nom')) return type.nom;
            else return '';
        };

        /**
         * Retourne le libelle de la matière correspondant à l'identifiant passé en paramètre
         * @param idMatiere identifiant de la matière
         * @returns {any} libelle de la matière
         */
        $scope.getLibelleMatiere = function (idMatiere) {
            if (idMatiere == undefined || idMatiere == null || idMatiere === "") return "";
            let matiere = _.findWhere($scope.matieres.all, {id: idMatiere});
            if (matiere !== undefined && matiere.hasOwnProperty('name')) {
                return matiere.name;
            } else {
                return '';
            }
        };

        /**
         * Retourne le libelle de la sous matière correspondant à l'identifiant passé en paramètre
         * @param currentDevoir le devoir en cours de visualisation
         * @returns {any} libelle de la sous matière
         */

        $scope.getLibelleSousMatiere = function (currentDevoir) {
            let id_type_sousmatiere = currentDevoir.id_sousmatiere;
            if (id_type_sousmatiere === null || id_type_sousmatiere === "" || id_type_sousmatiere === undefined) return "";
            return _.findWhere($scope.structure.typeSousMatieres, {id: parseInt(id_type_sousmatiere)}).libelle;
        };


        $scope.getLibelleDevoir = function (id) {
            if ($scope.mapIdLibelleDevoir !== undefined) return $scope.mapIdLibelleDevoir[parseInt(id)];
        };

        /**
         * Récupère la moyenne finale d'un élève ou la moyenne calculée pour une période donnée
         * @param idPeriode
         * @param moyennes
         * @param moyennesFinales
         * @returns {string}
         */
        $scope.getMoyenne = function (idPeriode,eleve) {
            if(idPeriode == null){
                let periodes = 0;
                let sum = 0;
                for(let i=1;i<6;i++){
                    let _moyenneFinale = _.findWhere(eleve.moyennesFinales, {id_periode: i});
                    if (_moyenneFinale !== undefined && _moyenneFinale !== null && _moyenneFinale.moyenne !== undefined && _moyenneFinale.moyenne !== null) {
                        sum += Number(_moyenneFinale.moyenne);
                        periodes ++;
                    }else {
                        let _moyenne = _.findWhere(eleve.moyennes, {id_periode: i});
                        if (_moyenne !== undefined && _moyenne !== null && _moyenne.moyenne !== undefined && _moyenne.moyenne !== "NN") {
                            sum += Number(_moyenne.moyenne);
                            periodes++;
                        }
                    }
                }
                if(periodes != 0){
                    return (Number(sum/periodes).toFixed(2));
                }
            }
            let _moyenneFinale = _.findWhere(eleve.moyennesFinales, {id_periode: idPeriode});
            if (_moyenneFinale !== undefined && _moyenneFinale !== null && _moyenneFinale.moyenne !== undefined && _moyenneFinale.moyenne !== null) {
                return _moyenneFinale.moyenne;
            }
            let _moyenne = _.findWhere(eleve.moyennes, {id_periode: idPeriode});
            if (_moyenne !== undefined && _moyenne !== null && _moyenne.moyenne !== undefined) {
                return _moyenne.moyenne;
            }
            return "";
        };

        /**
         * Détermine si la moyenne finale d'un élève pour une période donnée a été définie
         * @param idPeriode
         * @param moyennesFinales
         * @returns {string}
         */
        $scope.hasMoyenneFinale = function (idPeriode, moyennesFinales) {
            let _moyenneFinale = _.findWhere(moyennesFinales, {id_periode: idPeriode});
            if (_moyenneFinale !== undefined && _moyenneFinale !== null && _moyenneFinale.moyenne !== undefined && _moyenneFinale.moyenne > -1) {
                return true;
            }
            return false;
        };

        $scope.getLibellePositionnement = function (informationEleve,number) {
            let positionnementCalcule;
            if(informationEleve.positionnements_auto) {
                let positionnementFind = _.find(informationEleve.positionnements_auto,
                    function (positionnement) {
                        return positionnement.id_periode == $scope.search.periode.id_type
                    });
                if(positionnementFind)
                    positionnementCalcule = (positionnementFind.moyenne || positionnementFind.moyenne == 0) ? positionnementFind.moyenne : utils.getNN();
                else
                    positionnementCalcule = utils.getNN();
            }else{
                positionnementCalcule = utils.getNN();
            }
            if(number) {
                if (positionnementCalcule === utils.getNN()) {
                    return "";
                } else {
                    return +(positionnementCalcule).toFixed(2);
                }
            }else {
                if (positionnementCalcule === utils.getNN()) {
                    return lang.translate('evaluations.no.positionnement.calculee');
                } else {
                    return lang.translate("evaluations.positionnement.calculee")
                        + " : " + +(positionnementCalcule).toFixed(2);
                }
            }
        };
        /**
         * Séquence d'enregistrement d'une annotation
         * @param evaluation évaluation à enregistrer
         * @param $event evenement déclenchant
         * @param eleve élève propriétaire de l'évaluation
         * @param isAnnotaion sauvegarde depuis un champ de type annotation (evaluation devoir actuellement)
         */
        $scope.saveAnnotationDevoirEleve = function (evaluation, $event, eleve, isAnnotaion) {
            if (evaluation.id_annotation !== undefined && evaluation.id_annotation > 0) {
                if (evaluation.oldId_annotation !== evaluation.id_annotation && evaluation.oldValeur !== evaluation.valeur) {
                    evaluation.saveAnnotation().then(() => {
                        let annotation = _.findWhere($scope.evaluations.annotations.all, {id: evaluation.id_annotation});
                        evaluation.oldValeur = annotation.libelle_court;
                        evaluation.valeur = annotation.libelle_court;
                        delete evaluation.id;
                        delete evaluation.data.id;
                        if($scope.currentDevoir.is_evaluated === false ||
                            (evaluation.valeur !== utils.getNN() && $scope.currentDevoir.is_evaluated === true)) {
                            for (let i = 0; i < evaluation.competenceNotes.all.length; i++) {
                                evaluation.competenceNotes.all[i].evaluation = -1;
                            }
                        }
                        evaluation.oldId_annotation = evaluation.id_annotation;
                        if (evaluation.valeur === utils.getNN() && !isAnnotaion) {
                            $scope.calculerMoyenneEleve(eleve);
                            $scope.calculStatsDevoirReleve(_.findWhere($scope.releveNote.devoirs.all, {id: evaluation.id_devoir}));
                        }
                        else {
                            $scope.calculStatsDevoir();
                            evaluation.valid = true;
                            utils.safeApply($scope);
                        }
                    });
                }
            }
        };

        /**
         * get liste des annotations
         * @param evaluation évaluation à ouvrir

         */
        $scope.getListeAnnotations = function (evaluations, evaluation) {
            if (evaluation !== undefined &&
                evaluation.id_annotation !== undefined && evaluation.id_annotation > 0) {
                return evaluations.annotationsfull;
            } else {
                return evaluations.annotations.all;
            }
        };

        /**
         * Supppression d'une annotation
         * @param evaluation évaluation à enregistrer
         */
        $scope.deleteAnnotationDevoir = async function (evaluation, resetValeur) {
            if (evaluation.id_annotation !== undefined
                && evaluation.id_annotation !== null
                && evaluation.id_annotation > 0) {
                await evaluation.deleteAnnotationDevoir();
                delete evaluation.oldId_annotation;
                delete evaluation.id_annotation;
                if (resetValeur) {
                    if($scope.currentDevoir.is_evaluated === false ||
                        (evaluation.valeur !== utils.getNN() && $scope.currentDevoir.is_evaluated === true)) {
                        for (let i = 0; i < evaluation.competenceNotes.all.length; i++) {
                            evaluation.competenceNotes.all[i].evaluation = -1;
                            delete evaluation.competenceNotes.all[i].id;
                        }
                    }
                    evaluation.oldValeur = "";
                    evaluation.valeur = "";
                }
                $scope.calculStatsDevoir();
                if (!evaluation.valid) {
                    evaluation.valid = true;
                }
            } else {
                evaluation.id_annotation = evaluation.oldId_annotation;
            }
            await  utils.safeApply($scope);
        };

        function getHTMLiD($event):String{
            if ($event.relatedTarget) {
                return $($event.relatedTarget).parent().attr('id');
            }
        }

        function shortTermIsCorrect(ShortTermAnnotation:String):Boolean{
            return (ShortTermAnnotation === SHORT_TERM_ABSENT
                || ShortTermAnnotation === SHORT_TERM_NOT_NOTED
                || ShortTermAnnotation === SHORT_TERM_NOT_RETURNED
                || ShortTermAnnotation === SHORT_TERM_DISPENSE);
        }

        function cleanShortTermCaseValue(evaluation):void{
            if(evaluation.annotation_libelle_court){
                const shortTermUpperCase = evaluation.annotation_libelle_court.toString().toUpperCase();
                if(shortTermIsCorrect(shortTermUpperCase)) {
                    evaluation.annotation_libelle_court = shortTermUpperCase;
                    evaluation.valeur = shortTermUpperCase;
                }
            }
            if(evaluation.valeur){
                const shortTermUpperCase = evaluation.valeur.toString().toUpperCase();
                if(shortTermIsCorrect(shortTermUpperCase)) {
                    evaluation.valeur = shortTermUpperCase;
                }
            }
        }

        function giveShortTermToValue(evaluation):void{
            if(evaluation && evaluation.annotation_libelle_court && !isNaN(parseFloat(evaluation.annotation_libelle_court))){
                evaluation.valeur = evaluation.annotation_libelle_court;
            }
        }

        function cleanComma(evaluation):void{
            if (evaluation.valeur !== undefined && Utils.isNotNull(evaluation.valeur)) {
                evaluation.valeur = evaluation.valeur.replace(",",".");
            }
        }

        function updateValueToNN(evaluation, isAnnotaion):void{
            if (evaluation.valeur === "" && isAnnotaion !== undefined && !isAnnotaion) {
                evaluation.valeur = SHORT_TERM_NOT_NOTED;
            }
        }
        function cleanIdAppreciation(evaluation):void{
            if (evaluation.data !== undefined && evaluation.data.id_appreciation !== undefined && evaluation.id_appreciation === undefined) {
                evaluation.id_appreciation = evaluation.data.id_appreciation;
            }
        }

        /**
         * Séquence d'enregistrement d'une évaluation
         * @param evaluation évaluation à enregistrer
         * @param $event evenement déclenchant
         * @param eleve élève propriétaire de l'évaluation
         * @param isAnnotaion sauvegarde depuis un champ de type annotation (evaluation devoir actuellement)
         */
        let isWorkingProgress:Boolean = false;
        let idHTMLofInput:String;
        const reg = /^[0-9]+(\.[0-9]{1,2})?$/;

        function isSaveEvaluationAppreciation(evaluation: Evaluation, isAppreciationChanged: Boolean) {
            return evaluation.oldAppreciation !== undefined && evaluation.appreciation !== undefined
                && isAppreciationChanged && evaluation.appreciation !== '';
        }

        function isDeleteEvaluationAppreciation(evaluation: Evaluation) {
            return evaluation.id_appreciation !== undefined && evaluation.id_appreciation !== null
                && evaluation.appreciation === "";
        }

        function isSaveAnnotationDevoir(evaluation: Evaluation, annotation) {
            return !reg.test(evaluation.valeur) && (annotation !== undefined && annotation !== null
                && annotation.id !== evaluation.oldId_annotation) && evaluation.oldValeur != evaluation.valeur;
        }

        function handleErrorWrongValue(devoir: Devoir, evaluation: Evaluation, $event) {
            notify.error(lang.translate("error.note.outbound") + devoir.diviseur);
            evaluation.valeur = evaluation.oldValeur;
            evaluation.valid = false;
            utils.safeApply($scope);
            if ($event !== undefined && $event.target !== undefined) {
                $event.target.focus();
            }
        }

        function isEmptyValue(evaluation: Evaluation) {
            return evaluation.valeur === "" &&
                (evaluation.id_annotation === undefined || evaluation.id_annotation < 0);
        }

        function deleteEvaluationAppreciation(evaluation) {
            evaluation.deleteAppreciation().then((res) => {
                evaluation.oldAppreciation = evaluation.appreciation;
                if (res.rows === 1) {
                    evaluation.id_appreciation = undefined;
                    evaluation.data.id_appreciation = undefined;
                }
                utils.safeApply($scope);
            });
        }

        async function deleteAnnotation(evaluation, isInReleve: boolean, eleve) {
            isWorkingProgress = true;
            let res = await evaluation.delete();
            evaluation.valid = true;
            evaluation.oldValeur = evaluation.valeur;
            if (isInReleve) {
                if (res.rows === 1) {
                    evaluation.id = undefined;
                    evaluation.data.id = undefined;
                }
                $scope.calculerMoyenneEleve(eleve);
                $scope.calculStatsDevoirReleve(_.findWhere($scope.releveNote.devoirs.all, {id: evaluation.id_devoir}));
            } else {
                if (res.rows === 1) {
                    evaluation.id = undefined;
                    evaluation.data.id = undefined;
                }
                $scope.calculStatsDevoir();
            }
        }

        async function saveEvaluationValeur(evaluation, devoir: Devoir, isInReleve: boolean, eleve) {
            isWorkingProgress = true;
            let res = await evaluation.save();
            evaluation.valid = true;
            evaluation.oldValeur = evaluation.valeur;
            if (devoir.coefficient === null) {
                notify.info('evaluation.devoir.coef.is.null');
            }
            if (res.id !== undefined) {
                evaluation.id = res.id;
                evaluation.data.id = res.id;
            }

            if (isInReleve) {
                $scope.calculerMoyenneEleve(eleve);
                $scope.calculStatsDevoirReleve(_.findWhere($scope.releveNote.devoirs.all, {id: evaluation.id_devoir}));
            } else {
                $scope.calculStatsDevoir();
            }
            $scope.opened.lightbox = false;
            delete $scope.selected.eleve;
            await utils.safeApply($scope);
            isWorkingProgress = false;
        }

        $scope.saveNoteDevoirEleve = async function (evaluation, $event, eleve, isAnnotation?) {
            // todo refacto make this function more readable
            let isInReleve = $location.$$path === '/releve';
            if (isInReleve)
                idHTMLofInput = getHTMLiD($event);

            let isValueChanged:Boolean = (evaluation.valeur !== evaluation.oldValeur);
            let isAppreciationChanged:Boolean = (evaluation.oldAppreciation !== evaluation.appreciation);

            cleanShortTermCaseValue(evaluation);
            if (isValueChanged || isAppreciationChanged) {
                giveShortTermToValue(evaluation);
                cleanComma(evaluation);
                updateValueToNN(evaluation, isAnnotation);
                cleanIdAppreciation(evaluation);
                // On est dans le cas d'une sauvegarde ou création d'appréciation
                if (isSaveEvaluationAppreciation(evaluation, isAppreciationChanged)) {
                    evaluation.saveAppreciation().then((res) => {
                        evaluation.oldAppreciation = evaluation.appreciation;
                        if (res.id !== undefined) {
                            evaluation.id_appreciation = res.id;
                            evaluation.data.id_appreciation = res.id;
                        }
                        utils.safeApply($scope);
                    });
                } else {
                    // On est dans le cas d'une suppression d'appréciation
                    if (isDeleteEvaluationAppreciation(evaluation)) {
                        deleteEvaluationAppreciation(evaluation);
                    } else {
                        // On est dans le cas d'une sauvegarde d'une note ou d'annotation
                        if (evaluation.data !== undefined && evaluation.data.id !== undefined && evaluation.id === undefined) {
                            evaluation.id = evaluation.data.id;
                        }
                        let annotation = _.findWhere($scope.evaluations.annotations.all, {libelle_court: evaluation.valeur});
                        if (isSaveAnnotationDevoir(evaluation, annotation)) {
                            evaluation.id_annotation = annotation.id;
                            $scope.saveAnnotationDevoirEleve(evaluation, $event, eleve, isAnnotation);
                        } else {
                            if ((evaluation.oldValeur !== undefined && isValueChanged) || evaluation.oldAppreciation !== undefined && isAppreciationChanged) {
                                if (evaluation.valeur !== undefined && evaluation.valeur !== "" && reg.test(evaluation.valeur)) {
                                    let devoir = evaluations.devoirs.findWhere({id: evaluation.id_devoir});
                                    if (devoir !== undefined) {
                                        if (parseFloat(evaluation.valeur) <= devoir.diviseur && parseFloat(evaluation.valeur) >= 0) {
                                            await saveEvaluationValeur(evaluation, devoir, isInReleve, eleve);
                                            if (isInReleve)
                                                $(`#${idHTMLofInput} > input`).select();
                                        } else {
                                            handleErrorWrongValue(devoir,evaluation, $event)
                                        }
                                    }
                                } else {
                                    if (isEmptyValue(evaluation)) {
                                        await deleteAnnotation(evaluation, isInReleve, eleve);
                                        await utils.safeApply($scope);
                                        isWorkingProgress = false;
                                        if (isInReleve)
                                            $(`#${idHTMLofInput} > input`).select();
                                    } else if (evaluation.valeur === "" && (evaluation.id_annotation !== undefined && evaluation.id_annotation > 0)) {
                                        await $scope.deleteAnnotationDevoir(evaluation, true);
                                    } else {
                                        if (evaluation.valeur !== "" && !annotation) {
                                            if ($event !== undefined && $event.target !== undefined) {
                                                $event.target.focus();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                $scope.opened.lightbox = false;
                await utils.safeApply($scope);
            }
        };

        /**
         * Ouvre le détail du devoir correspondant à l'index passé en paramètre
         * @param index index du devoir
         * @param bool état du détail
         */
        $scope.expand = function (index, bool) {
            if ($scope.openedDevoir !== index) {
                $scope.openedDevoir = index;
            } else {
                if (bool === true) {
                    $scope.openedDevoir = -1;
                }
            }
        };

        /**
         * Ouvre le détail de l'élève correspondant à l'index passé en paramètre et affiche l'appréciation
         * @param index index du devoir
         * @param bool état du détail
         */
        $scope.getLibelleCourtAnnotation = function (annotations, id_annotation) {
            let libelle_court = $scope.translate('search.annotation');
            if (id_annotation !== undefined && id_annotation !== null && id_annotation !== '') {
                let list_annotations = _.where(annotations, {id: id_annotation});
                if (list_annotations.length === 1) {
                    libelle_court = list_annotations.get(0).libelle_court;
                }
            }
            return libelle_court;
        };


        /**
         * Ouvre le détail de l'élève correspondant à l'index passé en paramètre et affiche l'appréciation
         * @param index index du devoir
         * @param bool état du détail
         */
        $scope.expandAppreciation = function (index, bool) {
            if ($scope.openedEleve !== index) {
                $scope.openedEleve = index;
            } else {
                if (bool === true) {
                    $scope.openedEleve = -1;
                }
            }
        };

        /**
         * Calcul la moyenne pour un élève
         * @param eleve élève
         */
        $scope.calculerMoyenneEleve = function (eleve) {
            eleve.getMoyenne($scope.releveNote.devoirs.all).then(() => {
                if(!eleve.moyenneFinaleIsSet){
                    eleve.moyenneFinale = eleve.moyenne;
                    eleve.oldMoyenneFinale = eleve.moyenne;
                }
                $scope.calculerMoyenneClasse();
                utils.safeApply($scope);
            });
        };

        $scope.calculerMoyenneClasse = function() {
            let moyenne = 0, max = 0, nbEleve = 0, moyenneFinal = 0, maxFinal = 0, nbEleveFinal = 0;
            let min = 20, minFinal = 20;
            $scope.releveNote.classe.eleves.all.forEach(eleve => {
                if(eleve.moyenne !== utils.getNN()){
                    moyenne += eleve.moyenne;
                    nbEleve++;
                    if(eleve.moyenne > max)
                        max = eleve.moyenne;
                    if(min > eleve.moyenne)
                        min = eleve.moyenne;
                }
                if(eleve.moyenneFinale !== utils.getNN()){
                    let moyF = eleve.moyenneFinale;
                    if(eleve.moyenneFinaleIsSet)
                        moyF = parseFloat(moyF);
                    moyenneFinal += moyF;
                    nbEleveFinal++;
                    if(moyF > maxFinal)
                        maxFinal = moyF;
                    if(minFinal > moyF)
                        minFinal = moyF;
                }
            });
            moyenne = moyenne / nbEleve;
            moyenneFinal = moyenneFinal / nbEleveFinal;
            if ($scope.releveNote._tmp._moyenne_classe.null.moyenne != "NN") {
                $scope.releveNote._tmp._moyenne_classe.null.moyenne = moyenne.toFixed(2);
                $scope.releveNote._tmp._moyenne_classe.null.min = min;
                $scope.releveNote._tmp._moyenne_classe.null.max = max;
            }

            if (!isNaN(moyenneFinal)) {
                $scope.releveNote._tmp._moyenne_classe.nullFinal.moyenne = moyenneFinal.toFixed(2);
                $scope.releveNote._tmp._moyenne_classe.nullFinal.min = minFinal;
                $scope.releveNote._tmp._moyenne_classe.nullFinal.max = maxFinal;
            } else {
                $scope.releveNote._tmp._moyenne_classe.nullFinal.moyenne = "NN";
                $scope.releveNote._tmp._moyenne_classe.nullFinal.min = "NN";
                $scope.releveNote._tmp._moyenne_classe.nullFinal.max = "NN"
            }


            utils.safeApply($scope);
        }

        /**
         * Ouvre la fenêtre détail des compétences sur un devoir
         */
        $scope.getInfoCompetencesDevoir = function () {
            $scope.opened.lightbox = true;
            template.open('lightboxContainer', 'enseignants/informations/display_competences');
        };

        /**
         * Calcul les statistiques du devoir courant
         */
        $scope.calculStatsDevoir = function () {
            for (var i = 0; i < $scope.currentDevoir.eleves.all.length; i++) {
                if ($scope.currentDevoir.eleves.all[i].evaluation !== undefined &&
                    $scope.currentDevoir.eleves.all[i].evaluation.valeur) {
                    $scope.currentDevoir.eleves.all[i].evaluation.ramener_sur = $scope.currentDevoir.ramener_sur;
                }
            }

            $scope.currentDevoir.calculStats().then(() => {
                utils.safeApply($scope);
            });
        };

        /**
         * Calcul les statistiques du devoir dont l'identifiant est passé en paramètre
         * @param devoirId identifiant du devoir
         */
        $scope.calculStatsDevoirReleve = function (devoir) {
            if (devoir !== undefined) {
                devoir.calculStats().then(() => {
                    utils.safeApply($scope);
                });
            }
        };

        /**
         * Lance le focus sur la cible de l'évènement
         * @param $event évènement
         */
        $scope.focusMe = function ($event) {
            if(isWorkingProgress) return;
            $event.target.select();
        };

        /**
         * Masque l'encart du détail de l'évaluation
         */
        $scope.hideInfosEval = function (){
            $scope.showInfosEval = false;
        };
        /**
         * Affiche les informations d'un devoir en fonction de l'objet passé en paramètre
         * @param obj objet de type Evaluation ou de type Devoir
         */
        $scope.getDevoirInfo = async function (obj) {
            if (template.isEmpty('leftSide-devoirInfo')) template.open('leftSide-devoirInfo', 'enseignants/informations/display_devoir');
            if (obj instanceof Devoir) $scope.informations.devoir = obj;
            else if (obj instanceof Evaluation) {
                let devoir = $scope.releveNote.devoirs.findWhere({id: obj.id_devoir});
                if (devoir !== undefined){
                    $scope.informations.devoir = devoir;
                    $scope.currentDevoir = devoir;
                }
            }

            if ($location.$$path === '/releve') {
                $scope.showInfosEval = true;
                $scope.openLeftMenu("openedDevoirInfo", false);
                if ($scope.informations.devoir !== undefined &&
                    $scope.informations.devoir.statistiques === undefined) {
                    $scope.informations.devoir.statistiques = {
                        percentDone: $scope.informations.devoir.percent
                    };

                }
                await utils.safeApply($scope);
            }
        };

        /**
         * Retourne les informations relatives à un élève
         * @param eleve élève
         */
        $scope.studentTemp = undefined;
        $scope.getEleveInfo = async function (eleve) {
            if(Utils.isNull(eleve)){
                return;
            }

            let idPeriode = null;
            if(Utils.isNotNull($scope.search.periode)) {
                idPeriode = $scope.search.periode.id_type
            } else if(Utils.isNotNull($scope.currentDevoir)) {
                idPeriode = $scope.currentDevoir.id_periode;
            }

            if(!$scope.opened.lightboxConfirmCleanAppreciation) {
                $scope.appreciationBackUp = eleve.appreciation_matiere_periode;
                $scope.studentTemp = eleve;
            }

            template.close('leftSide-userInfo');
            await utils.safeApply($scope);

            if(eleve != "" && !eleve.idClasse){
                eleve.idClasse = $scope.search.classe.id;
            }

            if(eleve.img == null) {
                eleve.getAvatar($scope.structure.id);
            }

            let allPromise = [];

            if(eleve.evenements == null) {
                allPromise.push(eleve.getEvenements($scope.structure.id));
            } else {
                if(Utils.isNotNull(idPeriode)) {
                    eleve.evenement = _.findWhere(eleve.evenements, {id_periode: idPeriode});
                }
            }

            if(eleve.appreciationCPE == null) {
                if(Utils.isNotNull(idPeriode)) {
                    eleve.appreciationCPE = new AppreciationCPE(eleve.id, idPeriode);
                    allPromise.push(eleve.appreciationCPE.syncAppreciationCPE());
                }
            }

            await Promise.all(allPromise);

            let syncPeriodeClasse = async () => {
                if(Utils.isNotNull($scope.search.classe) && Utils.isNotNull($scope.search.classe.periodes) &&
                    _.isEmpty($scope.search.classe.periodes.all)){
                    await $scope.search.classe.periodes.sync();
                }
            };

            if($location.path() === `/devoir/${$scope.currentDevoir.id}`){
                $scope.search.classe = _.findWhere($scope.structure.classes.all, {id : $scope.currentDevoir.id_groupe});
                await syncPeriodeClasse();
                if(Utils.isNotNull($scope.search.classe) && Utils.isNotNull($scope.search.classe.periodes)) {
                    $scope.filteredPeriode = $scope.search.classe.periodes.all;
                    $scope.search.periode = _.findWhere($scope.filteredPeriode, {id_type: idPeriode});
                }
            } else {
                await syncPeriodeClasse();
                $scope.filteredPeriode = $scope.search.classe.periodes.all;
            }

            utils.setHistoriqueEvenement($scope, eleve, $scope.filteredPeriode);

            $scope.informations.eleve = eleve;
            template.open('leftSide-userInfo', 'enseignants/informations/display_eleve');
            await utils.safeApply($scope);
        };

        $scope.deleteStudentInformations = () => {
            delete $scope.informations.eleve;
        }

        $scope.deleteDevoirInformations = () => {
            delete $scope.informations.devoir;
        }

        /**
         * Highlight la compétence survolée
         * @param id identifiant de la compétence
         */
        $scope.highlightCompetence = function (id, bool) {
            $scope.currentDevoir.competences.forEach((competence) => {
                if (competence && competence !== undefined && competence.id_competence === id) {
                    competence.hovered = bool;
                }
                else if (competence && competence !== undefined && competence.id_competence !== id) {
                    competence.hovered = false;
                }
            });
            return;
        };

        $scope.isValidClasseMatiere = (idClasse, idMatiere?) => {
            if ($scope.classes !== undefined) {
                let matiereClasse = $filter('getMatiereClasse')($scope.structure.matieres.all, idClasse, $scope.classes, model.me.userId);
                if(idMatiere)
                    return $scope.classes.findWhere({id: idClasse, remplacement: false}) !== undefined
                        && !_.isEmpty(matiereClasse) && _.findWhere(matiereClasse, {id : idMatiere});
                else
                    return $scope.classes.findWhere({id: idClasse, remplacement: false}) !== undefined
                        && !_.isEmpty(matiereClasse);
            }
        };

        $scope.filterValidClasseMatiere = () => {
            return (item) => {
                if($scope.selected && $scope.selected.devoirs && $scope.selected.devoirs.list && $scope.selected.devoirs.list[0])
                    return $scope.isValidClasseMatiere(item.id_groupe || item.id, $scope.selected.devoirs.list[0].id_matiere);
                else
                    return $scope.isValidClasseMatiere(item.id_groupe || item.id);
            }
        };

        $scope.filterValidClasse = (item) => {
            return isValidClasse(item.id, item.id_matiere, $scope.classes.all);
        };

        $scope.filterValidClasseGroups = (item) => {
            let valid = isValidClasse(item.id, item.id_matiere, $scope.allClasses.all);

            if(!valid && item.type_groupe === Classe.type.CLASSE && item.idGroups) {
                for(let i = 0; i < item.idGroups.length; i++) {
                    let group = item.idGroups[i];
                    if(isValidClasse(group, item.id_matiere, $scope.allClasses.all)){
                        valid = true;
                        break;
                    }
                }
            }

            return valid;
        }

        $scope.filterValidDevoir = (item) => {
            return isValidDevoir(item.id_groupe, item.id_matiere, $scope.classes.all);
        };

        $scope.filterHeadTeacherOrPersEducNat = () => {
            return (item) => {
                return Utils.isChefEtabOrHeadTeacher(item) || Utils.isPersEducNat();
            }
        };

        $rootScope.notYear = (periode) => {
            return periode.id !== null;
        };

        $scope.isEvaluableOnPeriode = (periode) => {
            return periode.id !== null && $scope.search.eleve.isEvaluable(periode);
        };

        /**
         * Sélectionne un élève et l'ajoute dans la liste d'élèves sélectionnés.
         * @param eleve élève
         */
        $scope.selectEleveListe = function (eleve) {
            $scope.selectObject($scope.selected.eleves.list, eleve);
            $scope.selected.eleves.all = $scope.currentDevoir.eleves.every(function (eleve) {
                return eleve.selected
            });
        };

        $scope.linkGroupsToClasses = async function () {
            return new Promise(async (resolve, reject) => {
                let url = '/competences/classe/groupes?idStructure=' + $scope.structure.id;
                http().getJson(url).done((mapGroups) => {
                    for (let i = 0; i < mapGroups.length; i++) {
                        let classe = _.findWhere($scope.allClasses.all, {id: mapGroups[i].id_classe});
                        if (classe != null) {
                            classe.idGroups = mapGroups[i].id_groupes;
                        }
                    }
                    resolve();
                }).error((e) => {
                    console.error(e);
                    reject();
                });
            });
        }


        /**
         * Sélectionne tous les élèves de la liste passée en paramètre
         */
        $scope.selectAllEleveListe = function () {
            if ($scope.selected.eleves.all !== true) {
                for (var i = 0; i < $scope.currentDevoir.eleves.all.length; i++) {
                    $scope.selected.eleves.list.push($scope.currentDevoir.eleves.all[i]);
                }
                $scope.selected.eleves.all = !$scope.selected.eleves.all;
                $scope.selectElement($scope.currentDevoir.eleves.all, $scope.selected.eleves.all);
                return;
            }
            $scope.selected.eleves.list = [];
            $scope.selected.eleves.all = !$scope.selected.eleves.all;
            $scope.selectElement($scope.currentDevoir.eleves.all, $scope.selected.eleves.all);
        };

        /**
         * Ajout ou supprimer l'objet dans la liste
         * @param list liste d'objet
         * @param object objet
         */
        $scope.selectObject = function (list, object) {
            if (list.indexOf(object) === -1) {
                list.push(object);
            }
            else {
                list.splice(list.indexOf(object), 1);
            }
        };

        /**
         * Afficher le suivi d'un élève depuis le suivi de classe
         * @param eleve
         */
        $scope.displaySuiviEleve = async function (eleve) {
            $scope.informations.eleve = eleve;
            $scope.search.eleve = eleve;
            $scope.selected.eleve = eleve;
            $scope.displayFromClass = true;
            $scope.displayFromEleve = true;
            let display = async function (){
                $scope.selected.matieres = [];
                $scope.exportByEnseignement = "false";
                $scope.allUnselect = true;
                $scope.releveComp = {
                    textMod: true
                };
                $scope.showRechercheBar = false;
                if (!Utils.isChefEtabOrHeadTeacher()) {
                    http().getJson('/viescolaire/matieres?idEtablissement=' + evaluations.structure.id,).done(function (res) {
                        $scope.allMatieresSorted = _.sortBy(res, 'rank');
                        utils.safeApply($scope);
                    });
                } else {
                    $scope.allMatieresSorted = _.sortBy($scope.matieres.all, 'rank');
                }

                if ($scope.informations.eleve === undefined) {
                    $scope.informations.eleve = null;
                }
                $scope.sortType = 'title'; // set the default sort type
                $scope.sortReverse = false;  // set the default sort order
                $scope.usePerso = evaluations.structure.usePerso;
                $scope.updateColorAndLetterForSkills();
                await utils.safeApply($scope);
            };
            await $scope.search.classe.eleves.sync();
            await display();
            template.open('main', 'enseignants/suivi_eleve/tabs_follow_eleve/follow_items/container');
            await  utils.safeApply($scope);
            $scope.goTo("/competences/eleve");
        };

        $scope.pOFilterEval = {
            limitTo: 22
        };

        /**
         *  Initialiser le filtre de recherche pour faire disparaitre la liste
         *  des élèves et ferme la popup contenant des filtres
         *
         */
        $scope.cleanRoot = function () {
            let elem = angular.element(".autocomplete");
            for (let i = 0; i < elem.length; i++) {
                elem[i].style.height = "0px";
            }
            template.close('lightboxEleveDetails');
        };

        /**
         * Affiche la liste des remplacements en cours et initialise le
         * formulaire de creation d'un remplacement
         */
        $scope.listRemplacements = function () {
            $scope.gestionRemplacement = new GestionRemplacement();
            // TODO gérer les établissements ?
            $scope.gestionRemplacement.remplacements.sync();
            $scope.gestionRemplacement.enseignants.sync();

            $scope.gestionRemplacement.sortType = 'date_debut'; // set the default sort type
            $scope.gestionRemplacement.sortReverse = false;  // set the default sort order

            template.open('main', 'personnels/remplacements/eval_remp_chef_etab');
        };

        /**
         * Sélectionne/Déselectionne tous les remplacemnts
         */
        $scope.selectAllRemplacements = function () {
            if ($scope.gestionRemplacement.selectAll === false) {
                // maj de la vue
                $scope.selectElement($scope.gestionRemplacement.remplacements.all, false);

                // vidage de la sélection
                $scope.gestionRemplacement.selectedRemplacements = [];
            } else {

                // maj de la vue
                $scope.selectElement($scope.gestionRemplacement.remplacements.all, true);

                // ajout à la liste de sélection
                $scope.gestionRemplacement.selectedRemplacements = _.where($scope.gestionRemplacement.remplacements.all, {selected: true});
            }


        };


        /**
         * Supprime les remplacments sélectionnés
         */
        $scope.deleteSelectedRemplacement = function () {
            var iNbSupp = $scope.gestionRemplacement.selectedRemplacements.length;
            var iCpteur = 0;

            for (var i = 0; i < iNbSupp; ++i) {
                var oRemplacement = $scope.gestionRemplacement.selectedRemplacements[i];

                // suppression des remplacments en BDD
                oRemplacement.remove().then(function (poRemplacementSupp) {

                    $scope.gestionRemplacement.remplacements.remove(poRemplacementSupp);
                    $scope.gestionRemplacement.selectedRemplacements

                    iCpteur++;

                    // si toutes les suppressions ont été faites on refresh la vue
                    if (iNbSupp === iCpteur) {

                        // fermeture popup
                        $scope.gestionRemplacement.confirmation = false;

                        // désélection de tous les remplacements
                        $scope.gestionRemplacement.selectAll = false;

                        // vidage de la liste des remplacements sélectionnés
                        $scope.gestionRemplacement.selectedRemplacements = [];

                        utils.safeApply($scope);
                    }
                });

            }
        };


        /**
         * Sélectionne/Déselectionne un remplacment
         * @param poRemplacement le remplacement
         */
        $scope.selectRemplacement = function (poRemplacement) {
            var index = _.indexOf($scope.gestionRemplacement.selectedRemplacements, poRemplacement);

            // ajout dans la liste des remplacements sélectionnés s'il n'y est pas présent
            if (index === -1) {
                $scope.gestionRemplacement.selectedRemplacements.push(poRemplacement);
                poRemplacement.selected = true;
            } else {
                // retrait sinon
                $scope.gestionRemplacement.selectedRemplacements = _.without($scope.gestionRemplacement.selectedRemplacements, poRemplacement);
                poRemplacement.selected = false;
            }

            // coche de la checkbox de sélection de tous les remplacements s'ils on tous été sélectionnés (un à un)
            $scope.gestionRemplacement.selectAll = $scope.gestionRemplacement.selectedRemplacements.length > 0 &&
                ($scope.gestionRemplacement.selectedRemplacements.length === $scope.gestionRemplacement.remplacements.all.length);

        };

        /**
         * Vérification de la cohérence de l'ajout du remplacement (verif remplacement déjà existant par exemple)
         *
         * @return true si aucune erreur, false sinon
         */
        $scope.controlerNewRemplacement = function () {
            // var oRemplacements = [];

            // _.each($scope.gestionRemplacement.remplacements.all, function (remp) {
            //     if (oRemplacement.titulaire.id == $scope.gestionRemplacement.remplacement.titulaire.id) {
            //         oRemplacements.push(remp);
            //     }
            // });

            $scope.gestionRemplacement.showError = false;

            for (var i = 0; i < $scope.gestionRemplacement.remplacements.all.length; i++) {
                var oRemplacement = $scope.gestionRemplacement.remplacements.all[i];
                if (oRemplacement.titulaire.id == $scope.gestionRemplacement.remplacement.titulaire.id) {

                    // la date de fin du nouveau  remplacement doit etre avant la date de debut d'un remplacement existant
                    var isRemplacementApresExistant = moment($scope.gestionRemplacement.remplacement.date_fin).diff(moment(oRemplacement.date_debut), "days") < 0;

                    // la date de fin d'un remplacement existant doit être avant la date de début d'un nouveau remplacement
                    var isFinApresFinRemplacementExistant = moment(oRemplacement.date_fin).diff(moment($scope.gestionRemplacement.remplacement.date_debut), "days") < 0;

                    // si l'une des 2 conditions n'est pas remplie le remplacement chevauche un remplacent existant
                    if (!(isRemplacementApresExistant || isFinApresFinRemplacementExistant)) {
                        $scope.gestionRemplacement.showError = true;
                        return false;
                    }
                }
            }

            return true;
        };


        /**
         * Enregistre un remplacemnt en base de données
         */
        $scope.saveNewRemplacement = function () {

            // Vérification de la cohérence de l'ajout du remplacement (verif remplacement déjà existant par exemple)
            var hasError = !$scope.controlerNewRemplacement();

            if (hasError) {
                return;
            }

            // TODO Recupere le bon établissement
            $scope.gestionRemplacement.remplacement.id_etablissement = $scope.evaluations.structure.id;

            // Conversion des dates en string
            /*$scope.gestionRemplacement.remplacement.date_debut = $scope.getDateFormated($scope.gestionRemplacement.remplacement.date_debut);
             $scope.gestionRemplacement.remplacement.date_fin = $scope.getDateFormated($scope.gestionRemplacement.remplacement.date_fin);*/

            // enregistrement du remplacement et refressh de la liste
            $scope.gestionRemplacement.remplacement.create().then(function () {

                // Mise à jour de la liste des remplacements
                $scope.gestionRemplacement.remplacements.sync().then(function () {
                    // Réinitialisation du formulaire d'ajout de remplacement
                    $scope.gestionRemplacement.remplacement.date_debut = new Date();

                    var today = new Date();
                    today.setFullYear(today.getFullYear() + 1);
                    $scope.gestionRemplacement.remplacement.date_fin = today;
                    $scope.gestionRemplacement.remplacement.titulaire = undefined;
                    $scope.gestionRemplacement.remplacement.remplacant = undefined;

                    $scope.gestionRemplacement.selectAll = false;
                    $scope.gestionRemplacement.selectedRemplacements = [];

                    utils.safeApply($scope);
                });


            });
        };
        $scope.disabledDevoir = [];
        $rootScope.$on("$locationChangeSuccess", function ($event, $nextRoute, $oldRoute) {
            if ($oldRoute === $nextRoute && ($route.current.originalPath === '/devoir/:idDevoir/edit' || $route.current.originalPath === '/devoir/:idDevoir/edit/')) {
                $scope.goTo('/');
                console.log('redirect');
                utils.safeApply($scope);
            }
            utils.safeApply($scope);
        });

        /**
         * Controle que la date de publication du devoir n'est pas inférieur à la date du devoir.
         *          et que la date du devoir est comprise dans la période
         */
        $scope.controleDate = async (devoir) => {
            $scope.endSaisie = null;
            $scope.errDateDevoir = null;
            let classe = _.findWhere($scope.structure.classes.all, {id: devoir.id_groupe});
            if (classe && classe.periodes.length() === 0) {
                await classe.periodes.sync();
            }
            let current_periode = _.findWhere(classe ? classe.periodes.all : undefined, {id_type: parseInt(devoir.id_periode)});
            if (current_periode !== undefined) {
                let start_datePeriode = current_periode.timestamp_dt;
                let end_datePeriode = current_periode.timestamp_fn;
                let date_saisie = current_periode.date_fin_saisie;

                $scope.errDatePubli = (moment($scope.devoir.date_publication).diff(
                    moment($scope.devoir.dateDevoir), "days") < 0);
                $scope.errDateDevoir = !(moment($scope.devoir.dateDevoir).isBetween(
                    moment(start_datePeriode), moment(end_datePeriode), 'days', '[]'));
                $scope.endSaisie = (moment($scope.devoir.dateDevoir).isAfter(
                    moment(date_saisie), 'days', '[') || moment(new Date()).isAfter(
                    moment(date_saisie), 'days', '[')) && !$scope.isChefEtabOrHeadTeacher();

                $scope.devoir.controlledDate = !$scope.errDatePubli && !$scope.errDateDevoir && !$scope.endSaisie;
            }
            else {
                $scope.devoir.controlledDate = false;
            }

            utils.safeApply($scope);
        };

        /**
         * Cherche si la période de fin de saisie est dépassée pour un devoir donné
         * @param devoir
         */
        $scope.checkEndSaisie = async (devoir) => {
            let classe = _.findWhere($scope.structure.classes.all, {id: devoir.id_groupe});
            if (classe.periodes.length() === 0) {
                await classe.periodes.sync();
                utils.safeApply($scope);
            }
            let date_fin_saisie = _.findWhere(classe.periodes.all, {id_type: devoir.id_periode}).date_fin_saisie;

            return !(moment(date_fin_saisie).isAfter(moment(), "days")
                || Utils.isChefEtabOrHeadTeacher(_.findWhere(evaluations.structure.classes.all, {id: devoir.id_groupe})));
        };

        /**
         * Cherche si la période de fin de saisie est dépassée pour un devoir donné
         * On ne vérifie pas si l'utilisateur est chef d'établissement
         * @param devoir
         */
        $scope.checkEndSaisieSeul = async (devoir) => {
            let classe = _.findWhere($scope.structure.classes.all, {id: devoir.id_groupe});
            if (classe.periodes.length() === 0) {
                await classe.periodes.sync();
                utils.safeApply($scope);
            }
            let date_fin_saisie = _.findWhere(classe.periodes.all, {id_type: devoir.id_periode}).date_fin_saisie;
            let isHeadTeacherOfClass = await Utils.isHeadTeacher(classe);
            return moment().isAfter(date_fin_saisie, "days") && !isHeadTeacherOfClass;
        };

        $scope.getPeriodeAnnee = () => {
            return {libelle: $scope.translate('viescolaire.utils.annee'), id: undefined}
        };

        $scope.getCurrentPeriode = async (classe) => {
            if (classe && classe.periodes && classe.periodes.length() === 0) {
                await classe.periodes.sync();
            }
            let currentPeriode = null;
            if(classe) {
                $scope.periodes = classe.periodes;
                $scope.notYearPeriodes = _.filter($scope.periodes.all, (periode) => {
                    return $scope.notYear(periode);
                });

                currentPeriode = _.find(classe.periodes.all, (periode) => {
                    return periode.timestamp_dt != undefined && periode.timestamp_fn != undefined ?
                        moment().isBetween(moment(periode.timestamp_dt), moment(periode.timestamp_fn), 'days', '[]')
                        : false;
                });
            }
            return currentPeriode != undefined ? currentPeriode : null;
        };

        //TODO MIX BOTH
        // /**
        //  * Return la periode scolaire courante
        //  * @returns {any}
        //  */
        // $scope.periodeParDefault = function () {
        //     return utils.getDefaultPeriode($scope.periodes.all);
        // };

        $scope.filterDuplicationAction = () => {
            try {
                if ($scope.selected.devoirs.list.length > 1) {
                    return false;
                }
                let classe = evaluations.structure.classes.findWhere({id: $scope.selected.devoirs.list[0].id_groupe});
                return !classe.remplacement;
            } catch (e) {

            }
        };

        /**
         * Controle la validité du formulaire de création d'un remplacement
         * @returns {boolean} Validité du formulaire
         */
        $scope.controleNewRemplacementForm = function () {
            return !(
                $scope.gestionRemplacement.remplacement !== undefined
                && $scope.gestionRemplacement.remplacement.titulaire !== undefined
                && $scope.gestionRemplacement.remplacement.remplacant !== undefined
                && $scope.gestionRemplacement.remplacement.titulaire.id !== $scope.gestionRemplacement.remplacement.remplacant.id
                && $scope.gestionRemplacement.remplacement.date_debut !== undefined
                && $scope.gestionRemplacement.remplacement.date_fin !== undefined
                && (moment($scope.gestionRemplacement.remplacement.date_fin).diff(moment($scope.gestionRemplacement.remplacement.date_debut), "days") >= 0)
            );
        };

        $scope.initReferences = () => {
            evaluations.enseignements = evaluations.structure.enseignements;
            evaluations.releveNotes = evaluations.structure.releveNotes;
            evaluations.matieres = evaluations.structure.matieres;
            evaluations.eleves = evaluations.structure.eleves;
            evaluations.classes = evaluations.structure.classes;
            evaluations.devoirs = evaluations.structure.devoirs;
            evaluations.types = evaluations.structure.types;
            $scope.enseignements = evaluations.structure.enseignements;
            $scope.filteredEnseignements = _.filter($scope.enseignements.all, enseignement => {
                return $scope.enseignementsWithCompetences(enseignement);
            });
            $scope.matieres = evaluations.structure.matieres;
            $scope.allMatieresSorted = _.sortBy($scope.matieres.all, 'rank');
            $scope.releveNotes = evaluations.structure.releveNotes;

            $scope.classes = evaluations.structure.classes;
            $scope.allClasses = evaluations.structure.allClasses;
            $scope.filteredClasses = _.filter($scope.classes.all, classe => {
                return $scope.filterValidClasse(classe);
            });
            $scope.linkGroupsToClasses().then(() => {
                $scope.filteredClassesGroups = _.filter($scope.allClasses.all, classe => {
                    return $scope.filterValidClasseGroups(classe);
                });
            });

            $scope.devoirs = evaluations.structure.devoirs;
            $scope.filteredDevoirs = _.filter($scope.devoirs.all, devoir => {
                devoir.nameClass = $scope.getClasseData(devoir.id_groupe, 'name');
                return $scope.filterValidDevoir(devoir);
            });
            $scope.types = evaluations.structure.types;
            $scope.eleves = evaluations.structure.eleves;
            $scope.enseignants = evaluations.structure.enseignants;
            $scope.usePerso = evaluations.structure.usePerso;
            $scope.useDefaut = !$scope.usePerso;
            $scope.structure = evaluations.structure;
            // $scope.initPeriodesList();
            utils.safeApply($scope);
        };

        let getCurrentAction = function (): string {
            return $route.current.$$route.action;
        };

        let executeAction = function (): void {
            routesActions[getCurrentAction()]($route.current.params);
            utils.safeApply($scope);
        };

        evaluations.sync().then(() => {
            $scope.structure = evaluations.structure;
            $scope.opened.displayStructureLoader = true;
            template.close('main');
            evaluations.structure.sync().then(() => {
                $scope.initReferences();
                if ($location.path() === '/disabled') {
                    $location.path('/');
                    $location.replace();
                } else {
                    executeAction();
                    utils.safeApply($scope);
                }
                $scope.opened.displayStructureLoader = false;
            });
            utils.safeApply($scope);
        }).catch(() => {
            $location.path() === '/disabled' ?
                executeAction() :
                $location.path('/disabled');
            $location.replace();
        });

        $scope.saveTheme = function () {
            $rootScope.chooseTheme();
        };

        $scope.updateColorAndLetterForSkills = function () {
            $scope.niveauCompetences = $scope.selectCycleForView();
            $scope.currentDevoir.niveauCompetences = $scope.niveauCompetences;

            // chargement dynamique des couleurs du niveau de compétences
            // et de la valeur max (maxOrdre)
            $scope.currentDevoir.maxOrdre = $scope.niveauCompetences[0].ordre - 1;
            $scope.mapCouleurs = {"-1": Defaultcolors.unevaluated};
            $scope.mapLettres = {"-1": " "};
            _.forEach($scope.currentDevoir.niveauCompetences, function (niv) {
                $scope.mapCouleurs[niv.ordre - 1] = niv.couleur;
                $scope.mapLettres[niv.ordre - 1] = niv.lettre;
            });
            utils.safeApply($scope);
        };

        $scope.updateNiveau = function (usePerso) {
            updateNiveau(usePerso, $scope);
        };

        $scope.togglePanel = function ($event) {
            $scope.showPanel = !$scope.showPanel;
            $event.stopPropagation();
        };

        $rootScope.$on('close-panel', function (e) {
            $scope.showPanel = false;
        });


        $scope.printCartouche = async (unType?: String) => {
            let url;
            if (unType) {
                url = '/competences/devoirs/print/' + $scope.currentDevoir.id + '/formsaisie';
            } else {
                switch ($scope.printOption.fileType) {
                    case 'cartouche' : {
                        url = "/competences/devoirs/print/" + $scope.currentDevoir.id + "/cartouche?eleve=" + $scope.printOption.byEleve
                            + '&color=' + $scope.printOption.inColor + "&usePerso=" + $scope.structure.usePerso + "&nbr=" + $scope.printOption.cartoucheNmb + "&image=" + $scope.printOption.image +
                            "&withResult=" + $scope.printOption.withResult + "&withAppreciations=" + $scope.printOption.withAppreciations;
                        $scope.printOption.display = false;
                        await Utils.runMessageLoader($scope);
                        http().getJson(url + "&json=true").error(async (result) => {
                            $scope.errorResultExportDevoir(result);
                            $scope.printOption.display = true;
                            await Utils.stopMessageLoader($scope);
                        }).done(async () => {
                            $scope.printOption.display = false;
                            let res = await httpAxios.get(url, {responseType: 'arraybuffer'});
                            Utils.downloadFile(res, document);
                            await Utils.stopMessageLoader($scope);
                        });
                        break;
                    }
                    case 'formSaisie' : {
                        url = '/competences/devoirs/print/' + $scope.currentDevoir.id + '/formsaisie';
                        $scope.exportDevoirObj.empty = false;
                        $scope.exportDevoirObj.errExport = false;
                        $scope.printOption.display = false;
                        await Utils.runMessageLoader($scope);
                        let res = await httpAxios.get(url, {responseType: 'arraybuffer'});
                        Utils.downloadFile(res, document);
                        await Utils.stopMessageLoader($scope);
                        break;
                    }
                }
            }
        };

        $scope.exportDevoirObj = {};
        $scope.exportRelCompObj = {};
        $scope.exportDevoir = async (idDevoir, textMod = false) => {
            $scope.printOption.display = false;
            await Utils.runMessageLoader($scope);
            let url = "/competences/devoirs/print/" + idDevoir + "/export?text=" + textMod
                + "&usePerso=" + $scope.structure.usePerso;
            http().getJson(url + "&json=true").error(async (result) => {
                $scope.errorResultExportDevoir(result);
                $scope.printOption.display = true;
                await Utils.stopMessageLoader($scope);
            }).done(async () => {
                $scope.opened.evaluation.exportDevoirLB = false;
                $scope.textModExport = false;
                $scope.printOption.display = false;
                let res = await httpAxios.get(url, {responseType: 'arraybuffer'});
                Utils.downloadFile(res, document);
                await Utils.stopMessageLoader($scope);
            });
        };

        $scope.errorResultExportDevoir = (result) => {
            switch (result.responseText) {
                case "{\"error\":\"exportDevoir : empty result.\"}" :
                    $scope.exportDevoirObj.emptyResult = true;
                    break;
                case "{\"error\":\"exportCartouche : empty result.\"}":
                    $scope.exportDevoirObj.emptyResult = true;
                    break;
                case "{\"error\":\"exportDevoir : no level.\"}" :
                    $scope.exportDevoirObj.emptyLevel = true ;
                    break;
                case  "{\"error\":\"exportCartouche : no level.\"}" :
                    $scope.exportDevoirObj.emptyLevel = true ;
                    break;
                case "{\"error\":\"errorCartouche : can not get students\"}":
                    $scope.exportDevoirObj.emptyStudent = true;
                    break;
                default :
                    $scope.exportDevoirObj.errExport = true;
            }
        };

        $scope.exportReleveComp = async (idEleve: String, idPeriode: Number, textMod: Boolean = false,
                                         exportByEnseignement: Boolean) => {
            let url = "/competences/releveComp/print/export?text=" + textMod;
            url += "&usePerso=" + $scope.structure.usePerso;
            url += "&idEleve=" + idEleve;
            url += "&idEtablissement=" + $scope.structure.id;

            for (var m = 0; m < $scope.selected.matieres.length; m++) {
                url += "&idMatiere=" + $scope.selected.matieres[m];
            }
            if (idPeriode) {
                url += "&idPeriode=" + idPeriode;
            }
            if ($scope.forClasse) {
                url += "&idClasse=" + $scope.search.classe.id;
                $scope.opened.releveComp = false;
                await Utils.runMessageLoader($scope);
            }

            ($scope.search.periode.libelle  === "cycle") ? url += "&isCycle=" + true : "&isCycle=" + false;

            url += "&byEnseignement=" + exportByEnseignement;

            await http().getJson(url + "&json=true")
                .error( async(result)=>{
                    if(url.includes('&idClasse=')){
                        await Utils.stopMessageLoader($scope);
                        $scope.opened.releveComp = true;
                    }
                    $scope.errorResult(result);
                    console.error(result);
                    utils.safeApply($scope);
                })
                .done( async() => {
                    delete $scope.releveComp;
                    $scope.releveComp = {
                        textMod: true
                    };
                    if($scope.opened.releveComp) $scope.opened.releveComp = false;

                    await httpAxios.get(url, {responseType: 'arraybuffer' }).then(async(data) => {
                        let blob;
                        let link = document.createElement('a');
                        let response = data.data;
                        blob = new Blob([response]);
                        link = document.createElement('a');
                        link.href = window.URL.createObjectURL(blob);
                        link.download = data.headers['content-disposition'].split('filename=')[1];
                        document.body.appendChild(link);
                        link.click();
                        if(url.includes('&idClasse=')){
                            await Utils.stopMessageLoader($scope);
                            notify.success('evaluations.export.bulletin.success');
                        }
                    });
                });
            utils.safeApply($scope);
        };

        $scope.errorResult = function (result) {
            switch (result.responseText) {
                case "{\"error\":\"getExportReleveComp : No exams on given period and/or material.\"}" :
                    $scope.evalNotFound = true;
                    break;
                case "{\"error\":\"devoirs not found\"}" :
                    $scope.periodeNotFound = true;
                    break;
                case "{\"error\":\"matieres not found\"}" :
                    $scope.classeNotFound = true;
                    break;
                case "{\"error\":\"domaines not found\"}" :
                    $scope.etabNotFound = true;
                    break;
                case "{\"error\":\"bfc not found\"}" :
                    $scope.bfcNotFound = true;
                    break;
                case "{\"error\":\"eleves not found\"}" :
                    $scope.elevesNotFound = true;
                    break;
                case "{\"error\":\"getExportReleveComp : Given groups belong to different cycle.\"}" :
                    $scope.cycleNotFound = true;
                    break;
                case "{\"error\":\"eval not found\"}" :
                    $scope.evalNotFound = true;
                    break;
                case "{\"error\":\"periode not found\"}" :
                    $scope.periodeNotFound = true;
                    break;
                case "{\"error\":\"classe not found\"}" :
                    $scope.classeNotFound = true;
                    break;
                case "{\"error\":\"etab not found\"}" :
                    $scope.etabNotFound = true;
                    break;
                case "{\"error\":\"bfc not found\"}" :
                    $scope.bfcNotFound = true;
                    break;
                case "{\"error\":\"eleves not found\"}" :
                    $scope.elevesNotFound = true;
                    break;
                case "{\"error\":\"different cycle\"}" :
                    $scope.cycleNotFound = true;
                    break;
                case "{\"error\":\"one or more students are in several classes\"}" :
                    $scope.studentInSeveralClasses = true;
                    break;
                default :
                    $scope.exportRecapEvalObj.errExport = true;
            }
        };

        $scope.selectMatiere = function (id) {
            $scope.closeWarningMessages();
            if (!$scope.selected.matieres.includes(id)) {
                $scope.selected.matieres.push(id);
            } else {
                $scope.selected.matieres.splice(_.indexOf($scope.selected.matieres, id), 1);
            }
            if ($scope.selected.matieres.length == 0) {
                $scope.allUnselect = true;
                $scope.disabledExportSuiviClasse = true;
            } else {
                $scope.allUnselect = false;
                $scope.disabledExportSuiviClasse = false;
            }
        };

        $scope.selectUnselectMatieres = function (selectAllMatieres) {
            $scope.closeWarningMessages();
            if (selectAllMatieres) {
                for (var m = 0; m < $scope.allMatieresSorted.length; m++) {
                    if (!$scope.selected.matieres.includes($scope.allMatieresSorted[m].id))
                        $scope.selected.matieres.push($scope.allMatieresSorted[m].id);
                    $scope.allMatieresSorted[m].select = true;
                }
                $scope.allUnselect = false;
                $scope.disabledExportSuiviClasse = false;
            } else {
                $scope.selected.matieres = [];
                for (var m = 0; m < $scope.allMatieresSorted.length; m++) {
                    $scope.allMatieresSorted[m].select = false;
                }
                $scope.allUnselect = true;
                $scope.disabledExportSuiviClasse = true;
            }
        };

        $scope.closeLightboxChampsObligatoire = function () {
            $scope.lightboxChampsObligatoire = false;
        };

        $scope.disabledExport = function () {
            return (_.findIndex($scope.allMatieresSorted, {select: true}) === -1) || typeof($scope.releveComp.periode) === 'undefined'
        };


        $scope.closeWarningMessages = function () {
            $scope.evalNotFound = false;
            $scope.periodeNotFound = false;
            $scope.classeNotFound = false;
            $scope.etabNotFound = false;
            $scope.bfcNotFound = false;
            $scope.elevesNotFound = false;
            $scope.cycleNotFound = false;
            $scope.studentInSeveralClasses = false;
            $scope.exportRelCompObj.errExport = false;
            $scope.exportRecapEvalObj.errExport = false;
            $scope.exportDevoirObj.errExport = false;
            $scope.exportDevoirObj.emptyLevel = false;
            $scope.exportDevoirObj.emptyResult = false;
            $scope.exportDevoirObj.emptyStudent = false;
        };

        $scope.openedLigthbox = function (classe) {
            $scope.opened.releveComp = true;
            $scope.releveComp.periode = $scope.search.periode;
            $scope.releveComp.textMod = true;
            $scope.closeWarningMessages();
            $scope.selectUnselectMatieres(false);
            $scope.updateMatieres();

            classe ? $scope.forClasse = true : $scope.forClasse = false;
        };

        $scope.getFormatedDate = function (date) {
            return moment(date).format("DD/MM/YYYY");
        };

        $scope.updateMatieres = function () {
            let filteredServices = _.filter($scope.search.services, service => {
                return service.id_groupe === $scope.search.classe.id;
            })
            $scope.allMatieresFiltered = _.filter($scope.search.matieres, matiere => {
                for (let service of filteredServices){
                    if (service.id_matiere === matiere.id) return true;
                }
                return false;
            });
            $scope.allMatieresSorted = _.sortBy($scope.allMatieresFiltered, 'rank');
        }

        $scope.openedRecapEval = () => {
            if ($scope.releveNoteTotale !== undefined) {
                delete $scope.releveNoteTotale;
            }

            $scope.opened.recapEval = true;
            $scope.suiviClasse.periode = $scope.search.periode;
            $scope.disableAppreciation();
            $scope.disabledExportSuiviClasse = typeof($scope.suiviClasse.periode) === 'undefined';
            $scope.closeWarningMessages();
            $scope.updateMatieres();
            utils.safeApply($scope);
        };

        $scope.changePrintSuiviClasse = function (option) {
            $scope.suiviClasse.print = option;
            if (option === "printReleveComp")
                $scope.disabledExportSuiviClasse = $scope.allUnselect || typeof($scope.suiviClasse.periode) === 'undefined';
            if (option === "printRecapEval")
                $scope.disabledExportSuiviClasse = typeof($scope.suiviClasse.periode) === 'undefined';
            if(option === "printTabMoys")
                $scope.disabledExportSuiviClasse = typeof($scope.suiviClasse.periode) === 'undefined';
        };

        $scope.initDefaultMatiere = function () {
            if ($scope.matieres.all.length === 1) {
                $scope.search.matiere = $scope.matieres.all[0];
            }
        };

        $scope.initMoyenneFinale = function (eleve) {
            if (eleve.moyenneFinale === undefined) {
                eleve.moyenneFinale = eleve.moyenne;
                eleve.oldMoyenneFinale = eleve.moyenneFinale;
                eleve.moyenneFinaleIsSet = false;
            }else{
                if(eleve.moyenneFinale === null)
                    eleve.moyenneFinale = "NN";
                eleve.moyenneFinaleIsSet = (eleve.moyenneFinale === "NN" && eleve.moyenne === eleve.moyenneFinale ) ? false :true;
                eleve.oldMoyenneFinale = eleve.moyenneFinale;
            }
        };

        $scope.isEndSaisie = function() {
            let classe = ($scope.releveNote !== undefined)? $scope.releveNote.classe : undefined;
            if (Utils.isChefEtabOrHeadTeacher(classe)) {
                return false;
            }
            else {
                if ($scope.releveNote === undefined || $scope.releveNote.classe === undefined) {
                    return true;
                } else {
                    let selectedPeriode = $scope.search.periode;
                    if (selectedPeriode !== undefined) {
                        return moment().isAfter(moment(selectedPeriode.date_fin_saisie), "days");
                    }
                    else {
                        return true;
                    }
                }
            }
        };

        $scope.disabledSaisieMoyenne = function () {
            if ($scope.search.periode.id === null || $scope.search.periode === "*") {
                return true;
            }
            else {
                return $scope.isEndSaisie();
            }
        };

        $scope.disabledSaisieNNoutPeriode = (): boolean => {
            if (!$scope.search.periode || $scope.search.periode.id === null || $scope.search.periode === "*") {
                return true;
            }
            else {
                return $scope.isEndSaisie();
            }
        };

        $scope.saveMoyenneFinaleEleve = function (eleve,updateHistorique) {
            if (eleve.moyenneFinale !== undefined && eleve.moyenneFinale !== null) {
                if(eleve.moyenneFinale !== undefined && typeof eleve.moyenneFinale.replace === 'function') {
                    eleve.moyenneFinale = eleve.moyenneFinale.replace(",",".");
                }
                let reg = /^[0-9]+(\.[0-9]{1,2})?$/;
                if(eleve.moyenneFinale.toUpperCase() === "NN") eleve.moyenneFinale = eleve.moyenneFinale.toUpperCase();
                if (reg.test(eleve.moyenneFinale) && parseFloat(eleve.moyenneFinale) <= 20 ||
                    eleve.moyenneFinale === "" || eleve.moyenneFinale === "NN"){
                    if(eleve.oldMoyenneFinale !== parseFloat(eleve.moyenneFinale) ||
                        eleve.oldMoyenneFinale !== eleve.moyenneFinale || eleve.moyenneFinale !== "") {

                        if( eleve.oldMoyenneFinale !== eleve.moyenneFinale ) {
                            $scope.releveNote.saveMoyenneFinaleEleve(eleve).then(async () => {
                                eleve.moyenneFinaleIsSet = true;
                                eleve.oldMoyenneFinale = eleve.moyenneFinale ;
                                if (updateHistorique) {
                                    $scope.updateHistorique(eleve, 'moyenneFinale');
                                }
                                if (eleve.moyenneFinale === "" && eleve.moyenne !== undefined ||
                                    eleve.moyenne === eleve.moyenneFinale || eleve.moyenne === parseFloat(eleve.moyenneFinale)) {
                                    eleve.moyenneFinaleIsSet = false;
                                    eleve.moyenneFinale = eleve.moyenne;
                                    eleve.oldMoyenneFinale = eleve.moyenneFinale;
                                }
                                $scope.calculerMoyenneClasse();
                                utils.safeApply($scope);
                            });
                            utils.safeApply($scope);
                        }
                    }
                    else {
                        eleve.moyenneFinale = eleve.oldMoyenneFinale;
                    }
                }
                else{
                    if ((eleve.oldMoyenneFinale !== "NN") || (eleve.moyenne !== "NN") || parseFloat(eleve.moyenneFinale) > 20) {
                        notify.error(lang.translate("error.average.outbound"));
                        eleve.moyenneFinale = eleve.oldMoyenneFinale;
                    }
                }
            }else{
                eleve.moyenneFinale = eleve.oldMoyenneFinale;
            }
            utils.safeApply($scope);
        };

        $scope.savePositionnementEleve = function (eleve, positionnement) {
            if ($scope.search.periode.id_type !== null) {
                eleve.positionnement = parseInt(positionnement);
                if (parseInt(eleve.positionnement) <= $scope.structure.cycle.niveauCompetencesArray.length) {
                    eleve.oldPositionnement = eleve.positionnement;
                    $scope.releveNote.savePositionnementEleve(eleve).then(() => {
                        $scope.updateHistorique(eleve, 'positionnement');
                    });
                }
                else {
                    notify.error(lang.translate("error.positionnement.outbound") +
                        $scope.structure.cycle.niveauCompetencesArray.length);
                    eleve.positionnement = eleve.oldPositionnement;
                    utils.safeApply($scope);
                }

            }
        };

        $scope.previousAppreciationMatiere = "";

        $scope.setPreviousAppreciationMatiere = (eleve) => {
            $scope.previousAppreciationMatiere = eleve.appreciation_matiere_periode;
            utils.safeApply($scope);
        };

        function preparedDataForAppreciation(student):AppreciationSubjectPeriodStudent{
            const appreciationSubjectPeriodStudentPrepared = {
                idStudent: student.id,
                appreciation: student.appreciation_matiere_periode,
                idStructure: $scope.releveNote.idEtablissement,
                idSubject: $scope.releveNote.idMatiere,
                idPeriod: $scope.releveNote.idPeriode,
                idClass: $scope.releveNote.idClasse,
            }
            return new AppreciationSubjectPeriodStudent(appreciationSubjectPeriodStudentPrepared);
        }

        function initAppreciation(): void {
            $scope.studentTemp = undefined;
            $scope.appreciationBackUp = undefined;
        }
        initAppreciation();

        $scope.saveAppreciationMatierePeriodeEleve = async (student, updateHistoric:Boolean):Promise<void> => {
            if (student.appreciation_matiere_periode === undefined
                || $scope.opened.lightboxConfirmCleanAppreciation)
                return;
            $scope.appreciationSubjectPeriod = preparedDataForAppreciation(student);
            if (student.appreciation_matiere_periode.length <= $scope.MAX_LENGTH_300) {
                if (student.appreciation_matiere_periode.length > 0) {
                    if($scope.previousAppreciationMatiere) {
                        await $scope.appreciationSubjectPeriod.put();
                    } else {
                        await $scope.appreciationSubjectPeriod.post();
                    }
                    if (updateHistoric && !$scope.opened.lightboxConfirmCleanAppreciation) {
                        $scope.updateHistorique(student, 'appreciation');
                    }
                } else if (student.appreciation_matiere_periode.length === 0
                    && $scope.previousAppreciationMatiere !== student.appreciation_matiere_periode
                    && $scope.previousAppreciationMatiere) {
                    if(!$scope.appreciationBackUp) $scope.appreciationBackUp = $scope.previousAppreciationMatiere;
                    if(!$scope.studentTemp) $scope.studentTemp = student;
                    template.open('lightboxConfirmCleanAppreciation', '/enseignants/informations/lightbox_confirm_clean_appreciation');
                    $scope.opened.lightboxConfirmCleanAppreciation = true;
                }
            } else {
                notify.error(lang.translate("error.char.outbound") + $scope.MAX_LENGTH_300);
            }
            utils.safeApply($scope);
        };

        $scope.deleteAppreciationSubjectStudent = async function (appreciationSubjectPeriod:AppreciationSubjectPeriodStudent):Promise<void> {
            let isDeleted:Boolean = false;
            try{
                await appreciationSubjectPeriod.delete();
                $scope.updateHistorique($scope.studentTemp, 'appreciation');
                isDeleted = true;
            } catch(error) {
                console.error(error);
                isDeleted = false;
            } finally {
                $scope.closeLightboxConfirmCleanAppreciation(isDeleted);
            }
        };

        $scope.closeLightboxConfirmCleanAppreciation = function (isDeleted:Boolean):void {
            if(!isDeleted) goBackAppreciation($scope.appreciationSubjectPeriod.idStudent, $scope.appreciationBackUp);
            $scope.opened.lightboxConfirmCleanAppreciation = false;
            template.close('lightboxConfirmCleanAppreciation');
            initAppreciation();
        };

        function goBackAppreciation(idStudent:string, appreciation:string):void{
            let studentReturning:any = {};
            if(!$scope.releveNote.classe.eleves.all
                || $scope.releveNote.classe.eleves.all.length === 0
                || !idStudent
                || idStudent.length === 0
                || typeof idStudent !== "string"
                || !appreciation
                || appreciation.trim().length === 0) return studentReturning;
            for(let student of $scope.releveNote.classe.eleves.all){
                if(student.id === idStudent) {
                    student.appreciation_matiere_periode = appreciation;
                    break;
                }
            }
        }

        $scope.openElementProgramme = function openElementProgramme() {
            $scope.opened.elementProgramme = !$scope.opened.elementProgramme;
        };

        $scope.openAppreciation = function () {
            $scope.opened.appreciation = !$scope.opened.appreciation;
        };

        $scope.saveElementProgramme = function (texte) {
            if (texte !== undefined) {
                if (texte.length <= $scope.MAX_LENGTH_300) {
                    $scope.releveNote.saveElementProgramme(texte).then(() => {
                        $scope.elementProgrammeDisplay = $scope.releveNote.elementProgramme.texte;
                        template.close('lightboxContainer');
                        utils.safeApply($scope);
                        template.open('lightboxContainer', 'enseignants/releve_notes/elements_programme');
                    });
                    $scope.opened.lightbox = false;
                }
                else {
                    notify.error(lang.translate("error.char.outbound") +
                        $scope.MAX_LENGTH_300);
                }
            }
            utils.safeApply($scope);
        };

        $scope.openEditElementProgramme = function () {
            $scope.disabledSaisieNNoutPeriode = () => {
                if (!$scope.search.periode || $scope.search.periode.id === null || $scope.search.periode === "*") {
                    return true;
                }
                else {
                    return $scope.isEndSaisie();
                }
            };
            $scope.releveNote.elementProgramme.texte = $scope.elementProgrammeDisplay;
            $scope.aideSaisie.cycle = null;
            $scope.aideSaisie.domaineEnseignement = null;
            template.open('lightboxContainer', 'enseignants/releve_notes/elements_programme');
            $scope.opened.lightbox = true;
            utils.safeApply($scope);
        };

        $scope.syncDomainesEnseignement = async function (cycle) {
            await $scope.releveNote.syncDomainesEnseignement(cycle);
            utils.safeApply($scope);
        };

        $scope.syncSousDomainesEnseignement = async function (domaine) {
            await $scope.releveNote.syncSousDomainesEnseignement(domaine);
            utils.safeApply($scope);
        };

        $scope.addProposition = function (libelleProposition) {
            if ($scope.releveNote.elementProgramme.texte === undefined){
                $scope.releveNote.elementProgramme.texte = "";
            }
            if ($scope.releveNote.elementProgramme.texte !== "")
                $scope.releveNote.elementProgramme.texte += " ";
            $scope.releveNote.elementProgramme.texte += libelleProposition;
        }

        $scope.toogleDevoirNote = function () {
            if ($scope.releveNote !== undefined && $scope.releveNote.idPeriode !== null) {
                $scope.releveNote.toogle = !$scope.releveNote.toogle;

                //masque ou affiche les devoirs
                $(".colDevoir").toggle();

                utils.safeApply($scope);
            } else {
                if($scope.releveNote !== undefined) {
                    $scope.releveNote.toogle = false;
                }
            }
        };

        function copyDevoirs(evaluation, eleve) {
            let devoirTmp = $scope.releveNote.devoirs.findWhere({id: evaluation.id_devoir});
            let devoir = utils.clone(devoirTmp);

            let competencesNotes = [];
            _.forEach(_.where(eleve.competencesNotes,
                {id_devoir: evaluation.id_devoir}), (competencesNote) => {
                competencesNotes.push(utils.clone(competencesNote))
            });

            _.extend(devoir, {competencesNotes: competencesNotes});
        }

        async function getDetailEleve(eleve) {
            try {
                await eleve.getDetails($scope.releveNote.idEtablissement,
                    $scope.releveNote.idClasse, $scope.releveNote.idMatiere);
            } catch (e) {
                console.error(e);
            }
        }

        function getPositionementData(nbPositionnementAnnee: number, positionnementAnnee: number, isMoyenneFinaleAnnee: boolean, historiqueAnnee: any, moyenneFinaleAnnee, isPositionnementFinaleAnnee: boolean, moyenneSousMatiereAnnee: {}, posSousMatiereAnnee: {}, eleve, moyenneClasseFinaleAnnee: number) {
            let positionnementFinaleAnnee = 0;
            if (nbPositionnementAnnee !== 0) {
                positionnementFinaleAnnee = Math.round(positionnementAnnee / nbPositionnementAnnee);
            }

            if (isMoyenneFinaleAnnee) {
                historiqueAnnee.moyenneFinale = moyenneFinaleAnnee;
            } else {
                historiqueAnnee.moyenne = moyenneFinaleAnnee;
                historiqueAnnee.moyenneFinale = "";
            }

            historiqueAnnee.moyenneClasse = moyenneClasseFinaleAnnee;

            let posAnneeForHistorique = (positionnementFinaleAnnee > 0) ? positionnementFinaleAnnee : utils.getNN();
            if (isPositionnementFinaleAnnee) {
                historiqueAnnee.positionnementFinal = posAnneeForHistorique;
            } else {
                historiqueAnnee.positionnement = posAnneeForHistorique;
                historiqueAnnee.positionnementFinal = "";
            }

            _.forEach($scope.releveNote.matiere.sousMatieres.all, (sousMatiere) => {
                let idSousMatiere = sousMatiere.id_type_sousmatiere;
                let tabMoy = moyenneSousMatiereAnnee[idSousMatiere];
                let tabPos = posSousMatiereAnnee[idSousMatiere];

                if (tabMoy !== '' && !_.isEmpty(tabMoy)) {
                    moyenneSousMatiereAnnee[idSousMatiere] = Utils.basicMoy(moyenneSousMatiereAnnee[idSousMatiere]);
                } else {
                    moyenneSousMatiereAnnee[idSousMatiere] = '';
                }

                if (tabPos !== '' && !_.isEmpty(tabPos)) {
                    posSousMatiereAnnee[idSousMatiere] =
                        Math.round(Utils.basicMoy(posSousMatiereAnnee[idSousMatiere]));
                } else {
                    posSousMatiereAnnee[idSousMatiere] = utils.getNN();
                }

            });
            eleve.historiques.push(historiqueAnnee);
        }

        function getMoyenneData(nbMoyenneAnnee: number, moyenneAnnee: number) {
            let moyenneFinaleAnnee;
            if (nbMoyenneAnnee !== 0) {
                moyenneFinaleAnnee = (moyenneAnnee / nbMoyenneAnnee).toFixed(2);
            } else {
                moyenneFinaleAnnee = "NN";
            }
            return moyenneFinaleAnnee;
        }

        function initDataWhenPeriodeIsNotNull(eleve, periode, moyenneClasse, moyenne, moyenneFinale, positionnement: number, positionnementFinal, appreciation, moyenne_sous_matieres: {}, pos_sous_matieres: {}, idPeriode) {
            eleve.historiques.push({
                periode: $scope.getI18nPeriode(periode),
                moyenneClasse: moyenneClasse,
                moyenne: moyenne,
                moyenneFinale: (moyenneFinale === null) ? utils.getNN() : moyenneFinale,
                positionnement: (positionnement > 0) ? positionnement : utils.getNN(),
                positionnementFinal: ((positionnementFinal === 0) ? utils.getNN() : positionnementFinal),
                appreciation: appreciation,
                moyenneSousMatieres: moyenne_sous_matieres,
                posSousMatieres: pos_sous_matieres,
                idPeriode: idPeriode
            });
            if ($scope.search.periode.id_type === idPeriode) {
                eleve._positionnement = pos_sous_matieres;
            }
        }

        $scope.initDataLightBoxEleve = async function () {
            let eleve = $scope.informations.eleve;
            if (eleve.evaluations.extended !== true) {
                _.forEach(eleve.evaluations.all, (evaluation) => {
                    // On Clone (copie par valeur) les devoirs  et les competencesNotes ici, pour ne pas dénaturer
                    // les objects lors de l'utilisation de la fonction extend
                    copyDevoirs(evaluation, eleve);
                    //if decomment check releve
                    // _.extend(evaluation, devoir);
                });
                await getDetailEleve(eleve);

                let moyenneAnnee = 0;
                let nbMoyenneAnnee = 0;
                let moyenneClasseAnnee = 0;
                let nbMoyenneClasseAnnee = 0;
                let moyenneSousMatiereAnnee = {};
                let posSousMatiereAnnee = {};
                _.forEach($scope.releveNote.matiere.sousMatieres.all, (sousMatiere) => {
                    moyenneSousMatiereAnnee[sousMatiere.id_type_sousmatiere] = '';
                    posSousMatiereAnnee[sousMatiere.id_type_sousmatiere] = '';
                });
                // Pour vérifier que si la moyenne finale de l'année a été modifiée
                let isMoyenneFinaleAnnee = false;

                let positionnementAnnee = 0;
                let nbPositionnementAnnee = 0;
                // Pour vérifier que si le Positionnement final de l'année a été modifié
                let isPositionnementFinaleAnnee = false;

                let historiqueAnnee:any = {};
                eleve.historiques = [];
                _.forEach($scope.filteredPeriode, function (periode) {
                    let idPeriode = periode.id_type;
                    // get moyenne auto eleve
                    let details_moyennes = _.findWhere(eleve.details.moyennes, {
                        id: (idPeriode !== null) ? parseInt(idPeriode) : null
                    });
                    let moyenne = (details_moyennes !== undefined) ? details_moyennes.moyenne : "NN";

                    // get moyenne classe
                    let details_moyennes_classe = _.findWhere(eleve.details.moyennesClasse, {
                        id:
                            (idPeriode !== null) ? parseInt(idPeriode) : null
                    });
                    let moyenneClasse = (details_moyennes_classe !== undefined) ? details_moyennes_classe.moyenne : "NN";
                    if ($scope.releveNote.idPeriode === idPeriode) {
                        eleve.moyenneClasse = moyenneClasse;
                    }

                    // get appreciation
                    let details_appreciations = _.findWhere(eleve.details.appreciations,
                        {id_periode: (idPeriode !== null) ? parseInt(idPeriode) : null});
                    let appreciation = (details_appreciations !== undefined) ?
                        details_appreciations.appreciation_matiere_periode : "";

                    // get moyenne finale
                    let details_moyennes_finales = _.findWhere(eleve.details.moyennes_finales,
                        {id_periode: (idPeriode !== null) ? parseInt(idPeriode) : null});
                    let moyenneFinale = (details_moyennes_finales !== undefined) ? details_moyennes_finales.moyenne : "";

                    let moyenne_sous_matieres = {};
                    let pos_sous_matieres = {};
                    _.forEach($scope.releveNote.matiere.sousMatieres.all, (sousMatiere) => {
                        let idSousMatiere = sousMatiere.id_type_sousmatiere;
                        let moy = eleve.getAverageSousMatiere(idPeriode, idSousMatiere, true);
                        moyenne_sous_matieres[idSousMatiere] = moy;

                        if(moyenneSousMatiereAnnee[idSousMatiere] === '') {
                            moyenneSousMatiereAnnee[idSousMatiere] = [];
                        }

                        if(moy !== '') {
                            moyenneSousMatiereAnnee[idSousMatiere].push(moy);
                        }

                        if(Utils.isNotNull(eleve.details._positionnements_auto)) {
                            let pos_sous_mat = eleve.details._positionnements_auto[idPeriode];
                            pos_sous_mat = (pos_sous_mat !== undefined) ? pos_sous_mat[idSousMatiere] : undefined;
                            let pos_converti = (pos_sous_mat !== undefined) ? (utils.getMoyenneForBFC(
                                pos_sous_mat.moyenne, $scope.releveNote.tableConversions.all)) : -1;
                            pos_sous_matieres[idSousMatiere] = (pos_converti !== -1) ? pos_converti : utils.getNN();
                            let isNN = (pos_sous_matieres[idSousMatiere] === utils.getNN());
                            if(posSousMatiereAnnee[idSousMatiere] === ''){
                                posSousMatiereAnnee[idSousMatiere] = [];
                            }
                            if (!isNN && Utils.isNotNull(pos_sous_matieres[idSousMatiere]) &&
                                pos_sous_matieres[idSousMatiere] > 0 ) {
                                posSousMatiereAnnee[idSousMatiere].push(pos_sous_matieres[idSousMatiere]);
                            }
                        }
                        else{
                            pos_sous_matieres[idSousMatiere] = utils.getNN();
                        }
                    });

                    // get positionnement Auto
                    let details_pos_auto = _.findWhere(eleve.details.positionnements_auto,
                        {id_periode : (idPeriode !== null) ? parseInt(idPeriode) : null});

                    // Déduction du positionnement par défaut en fonction de l'échelle de convertion
                    // Ajout de 1 à la moyenne pour rentrer dans l'échelle de conversion
                    // (Logique prise au calcul du niveau dans le BFC).
                    let positionnement = -1;
                    if(Utils.isNotNull(details_pos_auto) && details_pos_auto.hasNote) {
                        let moyenne_convertie = (details_pos_auto !== undefined) ? (utils.getMoyenneForBFC(
                            details_pos_auto.moyenne, $scope.releveNote.tableConversions.all)) : -1;
                        positionnement = (moyenne_convertie !== -1) ? moyenne_convertie : -1;
                    }

                    // get positionnement final
                    let details_pos = _.findWhere( eleve.details.positionnements,
                        {id_periode: (idPeriode !== null) ? parseInt(idPeriode) : null});
                    let positionnementFinal = (details_pos !== undefined) ? details_pos.positionnement : "";
                    // initialisation du positionnement pour le détail élève
                    if ($scope.releveNote.idPeriode === idPeriode) {
                        let pos = Utils.isNull(positionnement)? -1 : positionnement;
                        eleve.positionnementCalcule = ((pos <= 0)? utils.getNN() : pos);
                        eleve.positionnement =
                            (positionnementFinal !== "") ? positionnementFinal : (eleve.positionnementCalcule);

                    }

                    // On stocke la moyenne du trimestre pour le calcul de la moyenne à l'année
                    if (idPeriode !== null && (details_moyennes_finales !== undefined|| details_moyennes !== undefined) && moyenneFinale !== null ) {
                        nbMoyenneAnnee++;
                        if (details_moyennes_finales !== undefined ) {
                            isMoyenneFinaleAnnee = true;
                            moyenneAnnee += parseFloat(moyenneFinale);
                        } else {
                            moyenneAnnee += moyenne;
                        }
                    }

                    // On stocke la moyenne du trimestre pour le calcul de la moyenne à l'année
                    if (idPeriode != null && moyenneClasse != undefined) {
                        nbMoyenneClasseAnnee++;
                        moyenneClasseAnnee += moyenneClasse;
                    }

                    // On stocke le positionnement du trimestre pour le calcul du positionnement à l'année
                    if (idPeriode !== null) {
                        if (Utils.isNotNull(details_pos)) {
                            if (details_pos.positionnement > 0) {
                                isPositionnementFinaleAnnee = true;
                                positionnementAnnee += details_pos.positionnement;
                                nbPositionnementAnnee++;
                            }
                        } else if (Utils.isNotNull(positionnement) && positionnement > 0) {
                            positionnementAnnee += positionnement;
                            nbPositionnementAnnee++;
                        }
                    }

                    if (idPeriode !== null && idPeriode !== undefined) {
                        initDataWhenPeriodeIsNotNull(eleve, periode, moyenneClasse, moyenne, moyenneFinale, positionnement,
                            positionnementFinal, appreciation, moyenne_sous_matieres, pos_sous_matieres, idPeriode);
                    } else {
                        historiqueAnnee = {
                            periode: $scope.getI18nPeriode(periode),
                            moyenneClasse: moyenneClasse,
                            appreciation: appreciation,
                            moyenneSousMatieres: moyenneSousMatiereAnnee,
                            posSousMatieres: posSousMatiereAnnee,
                            idPeriode: idPeriode
                        }
                    }
                });

// On calcule la moyenne à l'année
                let moyenneFinaleAnnee = getMoyenneData(nbMoyenneAnnee, moyenneAnnee);

                let moyenneClasseFinaleAnnee = getMoyenneData(nbMoyenneClasseAnnee, moyenneClasseAnnee);

// On calcule le positionnement à l'année
                getPositionementData(nbPositionnementAnnee, positionnementAnnee, isMoyenneFinaleAnnee, historiqueAnnee,
                    moyenneFinaleAnnee, isPositionnementFinaleAnnee, moyenneSousMatiereAnnee, posSousMatiereAnnee, eleve,
                    moyenneClasseFinaleAnnee);

                eleve.evaluations.extended = true;
                utils.safeApply($scope);
            }
            // END
        };

        $scope.openedLightboxEleve = async (eleve, filteredPeriode) => {
            await $scope.getEleveInfo(eleve);
            $scope.filteredPeriode = filteredPeriode;
            $scope.opened.lightboxReleve = true;
            eleve.showCompetencesDetails = false;
            await $scope.updateHistorique(eleve,'');
            template.close('lightboxEleveDetails');
            template.open('lightboxContainerReleve', 'enseignants/releve_notes/details_releve_periodique_eleve');
            await utils.safeApply($scope);
            if(template.contains('contentDetails', 'enseignants/releve_notes/details_graph_view')) {
                template.close('contentDetails');
                await utils.safeApply($scope);
                await $scope.releveNote.getDataForGraph($scope.informations.eleve, $scope.displayDomaine,
                    $scope.niveauCompetences);
                template.open('contentDetails', 'enseignants/releve_notes/details_graph_view');
                await utils.safeApply($scope);
            }
        };

        $scope.openedLigthboxDetailsEleve = async (eleve) => {
            eleve.showCompetencesDetails = true;
            delete $scope.informations.domaines;
            await $scope.releveNote.getArbreDomaine(eleve);
            $scope.informations.domaines = eleve.domaines;
            $scope.suiviFilter = {
                mine: false
            };
            $scope.selected = {
                grey: true
            };
            template.open('lightboxEleveDetails', 'enseignants/releve_notes/details_competences_eleve');
            utils.safeApply($scope);
        };

        $scope.incrementReleveEleve = async function (num, details?) {
            let index = _.findIndex($scope.releveNote.classe.eleves.all, {id: $scope.informations.eleve.id});
            if (index !== -1 && index + parseInt(num) >= 0
                && index + parseInt(num) < $scope.releveNote.classe.eleves.all.length) {
                $scope.informations.eleve = $scope.releveNote.classe.eleves.all[index + parseInt(num)];
                $scope.initDataLightBoxEleve();
            }
            if(details === true) {
                await $scope.openedLigthboxDetailsEleve($scope.informations.eleve);
            }
            if(template.contains('contentDetails', 'enseignants/releve_notes/details_graph_view')) {
                template.close('contentDetails');
                await utils.safeApply($scope);
                await $scope.releveNote.getDataForGraph($scope.informations.eleve, $scope.displayDomaine,
                    $scope.niveauCompetences);
                template.open('contentDetails', 'enseignants/releve_notes/details_graph_view');
            }
            await utils.safeApply($scope);
        };

        $scope.hasCompetences = function (evaluation) {
            return evaluation.competenceNotes.length() > 0;
        };

        $scope.updateHistorique = async function (eleve, colonne: string) {
            let historique = _.findWhere(eleve.historiques, {idPeriode: $scope.releveNote.idPeriode});
            if (historique !== undefined) {
                switch (colonne) {
                    case 'appreciation' :
                        historique.appreciation = eleve.appreciation_matiere_periode;
                        break;
                    case 'moyenneFinale':
                        historique.moyenneFinale = eleve.moyenneFinale;
                        break;
                    case 'positionnement':
                        historique.positionnementFinal = (eleve.positionnement>0)? eleve.positionnement:utils.getNN();
                        break;
                    default:
                        break;
                }

            }
            $scope.informations.eleve.evaluations.extended = false;
            await $scope.initDataLightBoxEleve();
            await utils.safeApply($scope);
        };

        $scope.hasCompetenceNotes = function (evaluations) {
            if (evaluations !== undefined && evaluations.all !== undefined) {
                for (let i = 0; i < evaluations.all.length; i++) {
                    let evaluation = evaluations.all[i];
                    if (evaluation.competenceNotes.length() > 0) {
                        return true;
                    }
                }
            }
            return false;
        };

        $scope.hasDevoirsEvalues = function (evaluations) {
            let hasDevoirsEvalues = (evaluations) ? _.where(evaluations.all, {is_evaluated: true}) : false;
            for (let i = 0; i < hasDevoirsEvalues.length; i++) {
                if (hasDevoirsEvalues[i].valeur !== '') {
                    return true;
                }
            }
            return false;
        };

        $scope.isNotFirstEleve = function () {
            if ($scope.releveNote === undefined || $scope.informations.eleve == undefined) {
                return false;
            }
            let index = _.findIndex($scope.releveNote.classe.eleves.all, {id: $scope.informations.eleve.id});
            if (index === 0) {
                return false;
            } else {
                return true;
            }
        };
        $scope.isNotLastEleve = function () {
            if ($scope.releveNote === undefined || $scope.informations.eleve == undefined) {
                return false;
            }
            let index = _.findIndex($scope.releveNote.classe.eleves.all, {id: $scope.informations.eleve.id});
            if (index === $scope.releveNote.classe.eleves.all.length - 1) {
                return false;
            } else {
                return true;
            }
        };
        /**
         * Filtre permettant de retourner l'évaluation maximum en fonction du paramètre de recherche "Mes Evaluations"
         * @param listeEvaluations Tableau d'évaluations de compétences
         * @returns {(evaluation:any)=>(boolean|boolean)} Retourne true si la compétence courante est la plus haute du
         * tableau listeEvaluations
         */
        $scope.isMaxEvaluation = function (listeEvaluations) {
            return Utils.isMaxEvaluation(listeEvaluations,$scope);
        };


        $scope.hasMaxNotFormative = function (MaCompetence) {
            return Utils.hasMaxNotFormative(MaCompetence, $scope);
        };


        /**
         * Retourne si l'utilisateur n'est pas le propriétaire de compétences
         * @param listeEvaluations Tableau d'évaluations de compétences
         * @returns {boolean} Retourne true si l'utilisateur n'est pas le propriétaire
         */
        $scope.notEvalutationOwner = function (listeEvaluations) {
            return Utils.hasMaxNotFormative(listeEvaluations, $scope);
        };

        $scope.FilterNotEvaluated = function (MaCompetence) {
            return Utils.FilterNotEvaluated(MaCompetence, $scope);
        };


// Permet de faire afficher la lightBox  au dessus du bandeau du thème
        $scope.displayBoxAboveTheHeadBand = function () {
            $('body').addClass('lightbox-opened');
        };
        $scope.closeLightBoxDetails = function () {
            $scope.displayBoxAboveTheHeadBand();
            $scope.informations.eleve.showCompetencesDetails = false;
        };

        $scope.openGraphView = async function (displayDomaine) {
            $scope.displayDomaine = displayDomaine;
            template.close('contentDetails');
            await utils.safeApply($scope);
            await $scope.releveNote.getDataForGraph($scope.informations.eleve, displayDomaine,$scope.niveauCompetences);
            template.open('contentDetails', 'enseignants/releve_notes/details_graph_view');
            await utils.safeApply($scope);
            $scope.displayBoxAboveTheHeadBand();
            await utils.safeApply($scope);
        };

        $scope.exportReleve = async function () {
            $scope.opened.displayMessageLoader = true;
            $scope.releveNote.exportOptions.show = false;
            await utils.safeApply($scope);
            let stopLoading = async function (){
                $scope.opened.displayMessageLoader = false;
                await utils.safeApply($scope);
            };

            try{
                await $scope.releveNote.export();
                await stopLoading();
                notify.success('evaluations.export.bulletin.success');
            }
            catch (e) {
                console.error(e);
                await stopLoading();
                notify.error(e);
            }

        };

        $scope.exportMoyennesMatieres = async function () {
            $scope.opened.displayMessageLoader = true;
            $scope.opened.recapEval = false;
            await utils.safeApply($scope);
            let stopLoading = async function (){
                $scope.opened.displayMessageLoader = false;
                await utils.safeApply($scope);
            };

            try{
                let p = {
                    idEtablissement: evaluations.structure.id,
                    idClasse: $scope.search.classe.id,
                    idPeriode: null,
                    periodeName: null,
                    periodes:[],
                    exportOptions: {
                        appreciation:$scope.suiviClasse.withAppreciations,
                        averageFinal: $scope.suiviClasse.withMoyGeneraleByEleve,
                        statistiques: $scope.suiviClasse.withMoyMinMaxByMat,
                        positionnementFinal: $scope.opened.releveNoteTotaleChoice == "pos" || $scope.opened.releveNoteTotaleChoice == "moyPos",
                        moyenneMat: $scope.opened.releveNoteTotaleChoice == "moy" || $scope.opened.releveNoteTotaleChoice == "moyPos",
                        avisConseil: $scope.suiviClasse.withAvisConseil,
                        avisOrientation: $scope.suiviClasse.withAvisOrientation
                    },
                    allMatieres: $scope.allMatieresSorted
                };
                if ($scope.suiviClasse.periode) {
                    p.idPeriode = $scope.suiviClasse.periode.id_type;
                    p.periodeName = $scope.getI18nPeriode($scope.suiviClasse.periode);
                }
                if ($scope.suiviClasse.periode.id_type == undefined) {
                    _.forEach($scope.search.classe.periodes.all, periode => {
                        if(periode.id_type != undefined){
                            p.periodes.push({
                                idPeriode: periode.id_type,
                                periodeName: $scope.getI18nPeriode(periode)
                            })
                        }
                    });
                }
                $scope.releveNoteTotale = new ReleveNoteTotale(p);
                const teacherBySubject = utils.getTeacherBySubject($scope.classes.all, $scope.search.classe.id,
                    $scope.structure.enseignants.all, $scope.search.periode);
                await $scope.releveNoteTotale.export(teacherBySubject);
                await stopLoading();
                notify.success('evaluations.export.bulletin.success');
            }
            catch (e) {
                console.error(e);
                await stopLoading();
                notify.error(e);
            }

        };

        $scope.disableAppreciation = function (){
            if($scope.suiviClasse.periode.id_type == undefined){
                $scope.suiviClasse.withAppreciations = false;
                utils.safeApply($scope);
            }
        };

        /*
        Permet de positionner une évaluation à 100% terminée même si des compétences ou des notes n'ont pas toutes été saisies
         */
        $scope.finishCurrentDevoir = async function () {
            $scope.currentDevoir.percent = 100;
            $scope.currentDevoir.statistiques.percentDone = 100;
            await $scope.currentDevoir.finishDevoir();
            await utils.safeApply($scope);
        };

        angular.merge = function (s1,s2) {
            return $.extend(true,s1,s2);
        };

        Chart.plugins.register({
            afterDatasetUpdate: function ( chart, easing) {
                if ($location.path() === '/conseil/de/classe' || $location.path() === '/releve') {
                    let currentChart = $scope.myCharts[chart.chart.canvas.id];

                    for (let i = 0; i < chart.data.datasets.length&& currentChart!==undefined; i++) {
                        let currentLabel =chart.data.datasets[i].label;
                        let currentDatasets = _.findWhere(currentChart.datasets, {label : currentLabel});
                        let hidden = chart.getDatasetMeta(i).hidden;
                        if (currentDatasets !== undefined){
                            currentDatasets.hidden = hidden;
                        }
                        else{
                            currentChart.datasets.push({label: currentLabel, hidden : hidden});
                        }
                    }
                }
            },
            afterInit:  function (chart, easing){
                if ($location.path() !== '/bulletin' && $location.path() !== '/competences/eleve' ) {
                    if($scope.myCharts === undefined){
                        $scope.myCharts = {};
                    }
                    let haveToUpdate = false;
                    let oldChart = $scope.myCharts[chart.chart.canvas.id];
                    let newChart = {datasets: []};

                    for (let i = 0; i < chart.data.datasets.length; i++) {

                        let currentlabel = chart.data.datasets[i].label;
                        let hidden = chart.getDatasetMeta(i).hidden;
                        if (oldChart !== undefined) {
                            let datasets = _.findWhere(oldChart.datasets, {label: currentlabel});
                            if (datasets !== undefined) {
                                hidden = datasets.hidden;
                                chart.getDatasetMeta(i).hidden = hidden;
                                haveToUpdate = true;
                            }
                        }

                        newChart.datasets.push({label: currentlabel, hidden: hidden});
                    }
                    $scope.myCharts[chart.chart.canvas.id] = newChart;
                }
            }
        });
    }
]);
