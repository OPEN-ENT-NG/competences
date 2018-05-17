import {model, notify, idiom as lang, ng, template, moment, _, angular, http} from 'entcore';
import {
    Devoir,
    Evaluation,
    evaluations,
    ReleveNote,
    GestionRemplacement,
    Classe,
    Structure,
    Annotation
} from '../models/teacher';
import * as utils from '../utils/teacher';
import {Defaultcolors} from "../models/eval_niveau_comp";
import {Utils} from "../models/teacher/Utils";

declare let $: any;
declare let document: any;
declare let window: any;
declare let console: any;
declare let location: any;

export let evaluationsController = ng.controller('EvaluationsController', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$sce', '$compile', '$timeout', '$route',
    function ($scope, route, $rootScope, $location, $filter, $sce, $compile, $timeout, $route) {

        model.me.workflow.load(['viescolaire']);

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
        //     if($scope.periodesList.values.length <= 1) {
        //         $scope.displayPeriode = false   ;
        //         utils.safeApply($scope);
        //     }else {
        //         $scope.displayPeriode =  true  ;
        //         utils.safeApply($scope);
        //     }
        //
        // };

        $scope.selectCycleForView = function (location) {
            let idCycle;
            if (location === 'saisieNote') {
                idCycle = $scope.classes.findWhere({id: $scope.currentDevoir.id_groupe}).id_cycle;
            }

            if (!idCycle && $scope.search.classe !== undefined && $scope.search.classe !== null
                && $scope.search.classe !== '*') {
                idCycle = $scope.search.classe.id_cycle;
            }
            evaluations.structure.cycle = _.findWhere(evaluations.structure.cycles, {
                id_cycle: idCycle
            });
            if (!evaluations.structure.cycle) {
                evaluations.structure.cycle = evaluations.structure.cycles[0];
            }
            $scope.structure.cycle = evaluations.structure.cycle;
            return $scope.structure.cycle.niveauCompetencesArray;
        };


        $scope.printOption = {
            display: false,
            fileType: "formSaisie",
            cartoucheNmb: 1,
            byEleve: false,
            inColor: false,
        };

        $scope.getI18nPeriode = (periode: any) => {
            let result;
            if (periode.id === null) {
                result = lang.translate("viescolaire.utils.annee");
            }
            else if (!(periode.hasOwnProperty('id_classe') || periode.hasOwnProperty('id_groupe'))) {
                result = periode ?
                    lang.translate("viescolaire.periode." + periode.type) + " " + periode.ordre
                    : lang.translate("viescolaire.utils.periodeError");
            }
            else {
                let type_periode = _.findWhere($scope.structure.typePeriodes.all, {id: periode.id_type});

                result = type_periode ?
                    lang.translate("viescolaire.periode." + type_periode.type) + " " + type_periode.ordre
                    : lang.translate("viescolaire.utils.periodeError");
            }
            return result;
        };

        $scope.initSearch = () => {
            return {
                matiere: '*',
                periode: '*',
                classe: '*',
                sousmatiere: '*',
                type: '*',
                idEleve: '*',
                name: '',
                enseignant: '*',
                duplication: ''
            }
        };
        $scope.togglePanel = function ($event) {
            $scope.showPanel = !$scope.showPanel;
            $event.stopPropagation();
        };
        $rootScope.$on('close-panel', function (e) {
            $scope.showPanel = false;
        });

        let routesActions = {

            accueil: function (params) {
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    $scope.search = $scope.initSearch();
                }
                $scope.opened.lightbox = false;
                template.open('main', 'enseignants/eval_acu_teacher');
                utils.safeApply($scope);
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
                    $scope.devoir.id_etat = parseInt(devoirTmp.id_etat);
                    $scope.devoir.date_publication = new Date(devoirTmp.date_publication);
                    $scope.devoir.id_etablissement = devoirTmp.id_etablissement;
                    $scope.devoir.diviseur = devoirTmp.diviseur;
                    $scope.devoir.coefficient = parseInt(devoirTmp.coefficient);
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
                    $scope.devoir.competences.sync().then(async () => {
                        await $scope.createDevoir();
                        template.open('main', 'enseignants/creation_devoir/display_creation_devoir');
                        $scope.displayCreationDevoir = true;
                        utils.safeApply($scope);
                    });
                }
            },

            listDevoirs: async function (params) {
                $scope.opened.lightbox = false;
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    _.map($scope.devoirs.all,(devoir)=> {
                        devoir.nameClass = $scope.getClasseData(devoir.id_groupe,'name');
                    });

                    let openTemplates = async () => {
                        $scope.search.enseignant = "*";
                        //rajout de la periode Annee
                        //await $scope.periodes.sync();
                        //$scope.initPeriodesList();
                        template.open('main', 'enseignants/liste_devoirs/display_devoirs_structure');
                        template.open('evaluations', 'enseignants/liste_devoirs/list_view');
                        utils.safeApply($scope);
                    };

                    if (Utils.isChefEtab()) {
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
                    $scope.currentDevoir = _.findWhere(evaluations.structure.devoirs.all, {id: parseInt(params.devoirId)});
                    $scope.usePerso = evaluations.structure.usePerso;
                    $scope.updateColorAndLetterForSkills('saisieNote');
                    $scope.updateNiveau($scope.usePerso);
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

                    if ($scope.currentDevoir.groupe.periodes.empty()) {
                        await $scope.currentDevoir.groupe.periodes.sync();
                    }
                    if ($scope.structure.typePeriodes.empty()) {
                        await $scope.structure.typePeriodes.sync();
                    }
                    $scope.currentDevoir.periode = _.findWhere($scope.currentDevoir.groupe.periodes.all,
                        {id_type: $scope.currentDevoir.id_periode});

                    $scope.currentDevoir.endSaisie = await $scope.checkEndSaisieSeul($scope.currentDevoir);

                    let syncStudents = () => {
                        $scope.openedDetails = true;
                        $scope.openedStatistiques = true;
                        $scope.openedStudentInfo = true;
                        if ($scope.currentDevoir !== undefined) {
                            $scope.currentDevoir.competences.sync().then(() => {
                                utils.safeApply($scope);
                            });
                            $scope.currentDevoir.eleves.sync().then(() => {
                                //$scope.$broadcast('initHeaderColumn');
                                var _evals = [];
                                for (var i = 0; i < $scope.currentDevoir.eleves.all.length; i++) {
                                    if ($scope.currentDevoir.eleves.all[i].evaluation.valeur !== null
                                        && $scope.currentDevoir.eleves.all[i].evaluation.valeur !== undefined
                                        && $scope.currentDevoir.eleves.all[i].evaluation.valeur !== "") {
                                        _evals.push($scope.currentDevoir.eleves.all[i].evaluation);
                                    }
                                }
                                utils.safeApply($scope);
                                $scope.currentDevoir.calculStats(_evals).then(() => {
                                    utils.safeApply($scope);
                                });
                            });
                        }

                        template.open('main', 'enseignants/liste_notes_devoir/display_notes_devoir');
                        utils.safeApply($scope);

                        angular.element(document).bind('mousewheel', function () {
                            // On Calque la position de la partie centrale sur le menu de gauche
                            let element = $('#left-side-notes');
                            let mirorElement = $('#liste-notes-devoir-header');
                            utils.mirorOnScroll(element, mirorElement);
                        });

                    };

                    let _classe = evaluations.structure.classes.findWhere({id: $scope.currentDevoir.id_groupe});
                    if (_classe !== undefined) {
                        if (_classe.eleves.all.length === 0) {
                            _classe.eleves.sync().then(() => {
                                syncStudents();
                            })
                        } else {
                            syncStudents();
                        }
                    }
                }
            },
            displayReleveNotes: function (params) {
                $scope.opened.lightbox = false;
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    // $scope.initPeriodesList();
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
            displaySuiviCompetencesEleve: function (params) {
                $scope.opened.lightbox = false;
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    let display = () => {
                        $scope.selected.matieres = [];
                        $scope.exportByEnseignement = "false";
                        $scope.allUnselect = true;
                        $scope.releveComp = {
                            textMod: true
                        };
                        $scope.showRechercheBar = true;
                        if (!Utils.isChefEtab()) {
                            http().getJson('/viescolaire/matieres?idEtablissement=' + evaluations.structure.id,).done(function (res) {
                                $scope.allMatieresSorted = _.sortBy(res, 'name');
                                utils.safeApply($scope);
                            });
                        } else {
                            $scope.allMatieresSorted = _.sortBy($scope.matieres.all, 'name');
                        }

                        template.open('main', 'enseignants/suivi_competences_eleve/container');
                        if ($scope.informations.eleve === undefined) {
                            $scope.informations.eleve = null;
                        }
                        $scope.sortType = 'title'; // set the default sort type
                        $scope.sortReverse = false;  // set the default sort order
                        $scope.usePerso = evaluations.structure.usePerso;
                        $scope.updateColorAndLetterForSkills();
                        utils.safeApply($scope);
                    };
                    if (params.idEleve != undefined && params.idClasse != undefined) {
                        $scope.search.classe = _.findWhere(evaluations.classes.all, {'id': params.idClasse});
                        $scope.syncPeriode($scope.search.classe.id);
                        $scope.search.periode = '*';
                        $scope.search.classe.eleves.sync().then(() => {
                            $scope.search.eleve = _.findWhere($scope.search.classe.eleves.all, {'id': params.idEleve});
                            if ($scope.displayFromClass) $scope.displayFromClass = false;
                            $scope.displayFromClass = true;
                            display();
                        });
                    } else {
                        display();
                    }

                }
            },
            displaySuiviCompetencesClasse: function (params) {
                if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
                    $scope.cleanRoot();
                    let display = () => {
                        $scope.selected.matieres = [];
                        $scope.allUnselect = true;
                        $scope.allRefreshed = false;
                        $scope.opened.recapEval = false;
                        $scope.exportRecapEvalObj = {
                            errExport: false
                        };
                        $scope.printSuiviClasse = "printRecapEval";
                        $scope.suiviClasse = {
                            textMod: true,
                            exportByEnseignement: 'false'
                        };
                        if (_.findIndex($scope.allMatieresSorted, {select: true}) === -1) {
                            $scope.disabledExportSuiviClasse = true;
                        } else {
                            $scope.disabledExportSuiviClasse = false;
                        }
                        $scope.sortType = 'title'; // set the default sort type
                        $scope.sortReverse = false;  // set the default sort order
                        $scope.usePerso = evaluations.structure.usePerso;
                        template.open('main', 'enseignants/suivi_competences_classe/container');
                        utils.safeApply($scope);
                    };
                    if (!Utils.isChefEtab()) {
                        http().getJson('/viescolaire/matieres?idEtablissement=' + evaluations.structure.id,).done(function (res) {
                            $scope.allMatieresSorted = _.sortBy(res, 'name');
                            utils.safeApply($scope);
                        });
                    } else {
                        $scope.allMatieresSorted = _.sortBy($scope.matieres.all, 'name');
                    }
                    if (params.idClasse != undefined) {
                        let classe: Classe = evaluations.classes.findWhere({id: params.idClasse});
                        $scope.search.classe = classe;
                        if (classe !== undefined) {
                            if (classe.eleves.empty()) classe.eleves.sync();
                            display();
                        }
                    } else {
                        display();
                    }
                }
            }, export: function () {
                template.open('main', 'export/lsun');
                utils.safeApply($scope);
            }, disabled: () => {
                template.open('main', 'disabled_structure');
                utils.safeApply($scope);
            }
        };

        route(routesActions);


        $scope.disabledExportSuiviClasseButton = function () {
            if ($scope.printSuiviClasse === "printReleveComp" && _.findIndex($scope.allMatieresSorted, {select: true}) === -1) {
                return true;
            } else {
                return false;
            }
        }

        $scope.showRechercheBarFunction = function (display) {
            $scope.showRechercheBar = display;
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
        $scope.MAX_CHAR_APPRECIATION_LENGTH = 300;
        $scope.exportRecapEvalObj = {
            errExport: false
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
            recapEval: false,
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
                suppretionMsg1: false,
                suppretionMsg2: false,
            },
            displayStructureLoader: false
        };

        $scope.isChefEtab = () => {
            return Utils.isChefEtab();
        };

        $scope.canUpdateBFCSynthese = () => {
            return Utils.canUpdateBFCSynthese();
        };

        $scope.evaluations = evaluations;
        $scope.competencesSearchKeyWord = "";
        $scope.devoirs = evaluations.devoirs;
        $scope.enseignements = evaluations.enseignements;
        $scope.bSelectAllEnseignements = false;
        $scope.bSelectAllDomaines = false;
        $scope.matieres = evaluations.matieres;
        $scope.releveNotes = evaluations.releveNotes;
        $scope.releveNote = null;
        $scope.classes = evaluations.classes;
        $scope.types = evaluations.types;
        $scope.filter = $filter;
        $scope.template = template;
        $scope.currentDevoir = {};
        $scope.search = $scope.initSearch();
        $scope.informations = {};
        $scope.messages = {
            successEvalLibre: false
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

        };

        $scope.aideSaisie = {
            cycle: null,
            domaineEnseignement: null,
            sousDomainesEnseignement: [],
        }

        $scope.syncPeriode = (idClasse) => {
            let classe = _.findWhere($scope.structure.classes.all, {id: idClasse});
            if (classe && classe.periodes && classe.periodes.length() === 0) {
                classe.periodes.sync().then(() => {
                    $scope.getCurrentPeriode(classe).then(function (res) {
                        $scope.search.periode = res;
                        if ($location.path() === '/devoir/create' ||
                            ($scope.devoir !== undefined
                                && ($location.path() === "/devoir/" + $scope.devoir.id + "/edit"))) {
                            $scope.devoir.id_periode = res.id_type;
                            $scope.controleDate($scope.devoir);
                            utils.safeApply($scope);
                        }
                        utils.safeApply($scope);
                    });
                });

            } else if (classe && classe.periodes) {
                $scope.getCurrentPeriode(classe).then(function (res) {
                    $scope.search.periode = _.findWhere($scope.structure.typePeriodes.all, {id: res.id_type});
                    if (($location.path() === '/devoir/create') ||
                        ($scope.devoir !== undefined
                            && ($location.path() === "/devoir/" + $scope.devoir.id + "/edit"))) {
                        $scope.devoir.id_periode = res.id_type;
                        $scope.controleDate($scope.devoir);
                        utils.safeApply($scope);
                    }
                    utils.safeApply($scope);
                });
            }
            utils.safeApply($scope);
        };

        $scope.getPeriodes = (idClasse) => {
            let classe = _.findWhere($scope.structure.classes.all, {id: idClasse});
            if (classe && classe.periodes && classe.periodes.length() === 0) {
                classe.periodes.sync().then(() => {
                    return classe.periodes.all;
                });
            }
            return (classe !== undefined) ? classe.periodes.all : [];
        };

        $scope.synchronizeStudents = (idClasse): boolean => {
            if (idClasse) {
                let _classe = evaluations.structure.classes.findWhere({id: idClasse});
                evaluations.structure.cycle = _.findWhere(evaluations.structure.cycles, {id_cycle: _classe.id_cycle});
                $scope.structure.cycle = evaluations.structure.cycle;
                utils.safeApply($scope);
                if (_classe !== undefined && !_classe.remplacement && _classe.eleves.empty()) {
                    _classe.eleves.sync().then(() => {
                        utils.safeApply($scope);
                        return true;
                    });
                }
                return false;
            }
        };

        $scope.confirmerDuplication = () => {
            if ($scope.selected.devoirs.list.length === 1) {
                let devoir: Devoir = $scope.selected.devoirs.list[0];
                devoir.duplicate($scope.selected.classes)
                    .then(() => {
                        $scope.devoirs.sync().then(() => {
                            $scope.resetSelected();
                            $scope.opened.lightboxs.duplication = false;
                            utils.safeApply($scope);
                        });
                    })
                    .catch(() => {
                        notify.error(lang.translate('evaluation.duplicate.devoir.error'));
                    });
            }
        };

        $scope.switchVisibilityApprec = async (devoir: Devoir) => {
            try {
                await devoir.switchVisibilityApprec();
            } catch (e) {
                console.log(e);
            } finally {
                utils.safeApply($scope);
            }
        };
        /**
         * Changement établissemnt : réinitial
         */

        $scope.changeEtablissement = async () => {
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
                    $scope.devoir.id_matiere = $scope.searchOrFirst("matiere", evaluations.structure.matieres.all).id;
                    $scope.devoir.id_type = $scope.searchOrFirst("type", evaluations.structure.types.all).id;
                    let currentPeriode = await $scope.getCurrentPeriode(
                        _.findWhere($scope.structure.classes.all, {id: $scope.devoir.id_groupe}));
                    $scope.devoir.id_periode = currentPeriode !== -1 ? currentPeriode.id_type : null;
                    if ($scope.devoir.id_periode == null
                        && $scope.search.periode && $scope.search.periode !== "*") {
                        $scope.devoir.id_periode = $scope.search.periode.id_type;
                        utils.safeApply($scope);
                    }
                    evaluations.structure.enseignements = $scope.devoir.enseignements;
                    $scope.enseignements = $scope.devoir.enseignements;
                    await $scope.loadEnseignementsByClasse();
                    await $scope.controleDate($scope.devoir);
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
                    await  init();
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

        $scope.getClassesByIdCycle = (type_groupe?: number) => {
            let currentIdGroup = $scope.selected.devoirs.list[0].id_groupe;
            let targetIdCycle = _.find($scope.classes.all, {id: currentIdGroup}).id_cycle;
            return _.filter($scope.classes.all, function (classe) {
                return type_groupe !== undefined ?
                    (classe.id_cycle === targetIdCycle && classe.id !== currentIdGroup && type_groupe === classe.type_groupe) :
                    (classe.id_cycle === targetIdCycle && classe.id !== currentIdGroup);
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
            if (classe !== undefined) {
                $scope.selected.classes.push({
                    id: selectedClasseId,
                    type_groupe: classe.type_groupe
                });
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

        $scope.confirmSuppretion = function () {
            if ($scope.selected.devoirs.list.length > 0) {
                $scope.devoirsUncancelable = [];
                if (!Utils.isChefEtab()) {
                    _.map($scope.selected.devoirs.list, async function (devoir) {
                        let isEndSaisieDevoir = await $scope.checkEndSaisieSeul(devoir);
                        if (isEndSaisieDevoir) {
                            $scope.selected.devoirs.list = _.without($scope.selected.devoirs.list, devoir);
                            devoir.selected = false;
                            $scope.devoirsUncancelable.push(devoir);
                            utils.safeApply($scope);
                        }
                    });
                }
                $scope.opened.evaluation.suppretionMsg1 = true;

                utils.safeApply($scope);
            }
        };
        $scope.textSuppretionMsg2 = {
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
                    $scope.opened.evaluation.suppretionMsg1 = false;
                    if ($scope.selected.devoirs.listwithEvaluatedSkills.length > 0
                        || $scope.selected.devoirs.listwithEvaluatedMarks.length > 0) {
                        $scope.opened.evaluation.suppretionMsg2 = true;
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
        $scope.annulerSuppretion = function () {
            $scope.opened.evaluation.suppretionMsg2 = false;
            $scope.opened.evaluation.suppretionMsg1 = false;
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
                owner: model.me.userId,
                matieresByClasse: [],
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

        $scope.lightboxChampsApparition = function () {
            if ($scope.controleNewDevoirForm() == true) {
                $scope.lightboxChampsObligatoire = true;
            } else {
                $scope.beforSaveDevoir();
            }
        };

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
                && $scope.devoir.coefficient >= 0
                && $scope.devoir.diviseur !== undefined
                && $scope.devoir.diviseur > 0
                && $scope.devoir.id_type !== undefined
                && $scope.devoir.ramener_sur !== undefined
                && $scope.devoir.id_etat !== undefined
                && ($scope.devoir.is_evaluated
                    || $scope.evaluations.competencesDevoir.length > 0)
                && $scope.evaluations.competencesDevoir.length <= $scope.MAX_NBR_COMPETENCE
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
        $scope.initFilter = function (pbInitSelected) {
            $scope.enseignementsFilter = {};
            $scope.competencesFilter = {};
            $scope.domaines = [];
            $scope.showCompetencesDomaine = {};
            $scope.displayFilterDomaine = false;
            for (let i = 0; i < $scope.enseignements.all.length; i++) {
                let currEnseignement = $scope.enseignements.all[i];
                $scope.enseignementsFilter[currEnseignement.id] = {
                    isSelected: pbInitSelected,
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
                let _b = false;
                let comp: any = null;
                for (let i = 0; i < poCompetences.all.length; i++) {
                    let currCompetence = poCompetences.all[i];
                    if ((currCompetence.ids_domaine_int !== undefined && currCompetence.ids_domaine_int[0].lengh === 1 && $scope.showCompetencesDomaine[currCompetence.ids_domaine_int[0]] === true) || $scope.showCompetencesDomaine.length == undefined) {
                        comp = _.findWhere(poCompetences.all, {id: poCompetences.all[i].id}) !== undefined
                        if (comp !== undefined) _b = false;
                        $scope.competencesFilter[currCompetence.id + "_" + currCompetence.id_enseignement] = {
                            isSelected: _b,
                            nomHtml: $scope.buildCompetenceNom(currCompetence),
                            data: currCompetence
                        };

                        $scope.initFilterRec(currCompetence.competences, pbInitSelected);
                    }
                }
            }
        };


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
         * Lance la séquence d'ouverture de l'ajout d'une appréciation pour un élève
         * @param eleve élève
         */
        $scope.addAppreciation = function (eleve) {
            template.open('lightboxContainer', 'enseignants/liste_notes_devoir/add_appreciation');
            $scope.selected.eleve = eleve;
            $scope.opened.lightbox = true;
            utils.safeApply($scope);
        };

        /**
         * Methode qui determine si un enseignement doit être affiché ou non
         * (pour chaque enseignement on rentre dans cette fonction et on check le booleen stocké
         * dans le tableau  $scope.enseignementsFilter[])
         *
         * @param enseignement l'enseignement à tester
         * @returns {true si l'enseignement est sélectionné, false sinon.}
         */
        $scope.enseignementsFilterFunction = function (enseignement) {
            // si valeur est rensiegnée on la retourne sinon on considère qu'elle est sélectionné (gestion du CTRL-F5)
            if ($scope.enseignementsFilter !== undefined) {
                if ($scope.enseignementsFilter[enseignement.id] && $scope.enseignementsFilter[enseignement.id].isSelected) {
                    if (enseignement.ids_domaine_int !== undefined && enseignement.ids_domaine_int.length > 0) {
                        for (let i = 0; i < enseignement.ids_domaine_int.length; i++) {
                            if ($scope.showCompetencesDomaine[enseignement.ids_domaine_int[i]]) {
                                return true;
                            }
                        }
                        return false;
                    } else {
                        // Si un enseignement n'a pas de domaines (pas de conncompétence lié on ne l'affiche pas
                        return false;
                    }
                } else {
                    return false;
                }
            }
        };
        $scope.enseignementsWithCompetences = function (enseignement) {
            return enseignement.competences.all.length > 0;
        };
        $scope.checkDomainesSelected = function (idDomaine) {
            $scope.showCompetencesDomaine[idDomaine] = !$scope.showCompetencesDomaine[idDomaine];
            let isAllDomainesSelected = true;
            let isAllDomainesUnSelected = true;
            for (let i = 0; i < $scope.domaines.length; i++) {
                if ($scope.showCompetencesDomaine[$scope.domaines[i].id]) {
                    isAllDomainesUnSelected = false;
                } else {
                    isAllDomainesSelected = false;
                }
            }

            if (isAllDomainesSelected) {
                $scope.bSelectAllDomaines = false;
            }

            if (isAllDomainesUnSelected) {
                $scope.bSelectAllDomaines = true;
            }
        }


        /**
         * Methode qui determine si une compétences doit être affichée ou non
         * (pour chaque compétence on rentre dans cette fonction et on check le booleen stocké
         * dans le tableau  $scope.enseignementsFilter[])
         *
         * @param compétence à tester
         * @returns {true si compétence est sélectionnée, false sinon.}
         */
        $scope.competencesByDomainesFilterFunction = function (competence) {
            // si valeur est rensiegnée on la retourne sinon on considère qu'elle est sélectionné (gestion du CTRL-F5)
            if ($scope.showCompetencesDomaine !== undefined) {
                if (competence.ids_domaine_int !== undefined) {
                    if (competence.ids_domaine_int.length === 1) {
                        return $scope.showCompetencesDomaine[competence.ids_domaine_int[0]];
                    } else {
                        for (let i = 0; i < competence.ids_domaine_int.length; i++) {
                            if ($scope.showCompetencesDomaine[competence.ids_domaine_int[i]]) {
                                return true;
                            }
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
                return _.some(competence.competences.all, {masque: false});
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
        };

        /**
         * affiche/masque toutes les compétences lors de la création d'un devoir.
         *
         * @param pbIsSelected booleen pour afficher ou masquer les compétences.
         */
        $scope.showDomaine = function (pbIsSelected) {
            for (let i = 0; i < $scope.domaines.length; i++) {
                let currdomaine = $scope.domaines[i];
                $scope.showCompetencesDomaine[currdomaine.id] = pbIsSelected;
            }
        };

        /**
         * affiche/masque tous les domaines lors de la création d'un devoir.
         *
         */
        $scope.showHideDomaines = function () {
            $scope.showDomaine($scope.bSelectAllDomaines);
            $scope.bSelectAllDomaines = !$scope.bSelectAllDomaines;
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
        $scope.enseignementsSearchFunction = function (psKeyword) {

            return function (enseignement) {

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
            }
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
                            if ($scope.competencesFilter[sousCompetence.id + "_" + sousCompetence.id_enseignement]
                                !== undefined) {
                                $scope.competencesFilter[sousCompetence.id + "_" + sousCompetence.id_enseignement].nomHtml
                                    = DisplayNomSousCompetence;
                            }
                        }

                    } else {
                        if ($scope.competencesFilter[sousCompetence.id + "_" + sousCompetence.id_enseignement]
                            !== undefined) {
                            $scope.competencesFilter[sousCompetence.id + "_" + sousCompetence.id_enseignement].nomHtml
                                = $scope.buildCompetenceNom(sousCompetence);
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
        $scope.loadEnseignementsByClasse = function () {
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
            if (currentIdCycle !== newIdCycle) {
                evaluations.enseignements.sync(classe_Id).then(function () {
                    // suppression des compétences qui n'appartiennent pas au cycle
                    $scope.initFilter(true);
                    evaluations.competencesDevoir = [];
                    utils.safeApply($scope);
                });
            }
            else if ($scope.cleanItems && $scope.oldCompetencesDevoir !== undefined) {
                evaluations.competencesDevoir = $scope.oldCompetencesDevoir;
                $scope.cleanItems = false;
            }
        };

        /**
         * Séquence de création d'un devoir
         */
//TODO Déplacer cette séquence dans la séquence du router
        $scope.createDevoir = async () => {
            if ($location.path() === "/devoir/create") {
                $scope.devoir = $scope.initDevoir();
                $scope.devoir.id_groupe = $scope.searchOrFirst("classe", $scope.structure.classes.all).id;
                $scope.devoir.id_matiere = $scope.searchOrFirst("matiere", $scope.structure.matieres.all).id;
                $scope.devoir.id_type = $scope.searchOrFirst("type", $scope.structure.types.all).id;

                let currentPeriode = await $scope.getCurrentPeriode(_.findWhere($scope.structure.classes.all, {id: $scope.devoir.id_groupe}));
                $scope.devoir.id_periode = currentPeriode !== -1 ? currentPeriode.id_type : null;
                if ($scope.devoir.id_periode == null
                    && $scope.search.periode && $scope.search.periode !== "*") {
                    $scope.devoir.id_periode = $scope.search.periode.id_type;
                    utils.safeApply($scope);
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
                    $scope.setClasseMatieres();
                    $scope.selectedMatiere();
                } else {
                    // selection de la premiere classe par defaut
                    $scope.devoir.id_groupe = $scope.classes.all[0].id;
                    // selection de la premiere matière associée à la classe
                    $scope.setClasseMatieres();
                }
            }
            let selectedClasse = _.findWhere($scope.classes.all, {id: $scope.devoir.id_groupe});
            if (selectedClasse !== undefined && selectedClasse.id_cycle !== null) {
                $scope.structure.enseignements.sync($scope.devoir.id_groupe).then(() => {
                    _.extend($scope.devoir.enseignements, $scope.enseignements);
                    $scope.initFilter(true);
                    $scope.evaluations.competencesDevoir = [];
                    for (let i = 0; i < $scope.devoir.competences.all.length; i++) {
                        $scope.evaluations.competencesDevoir.push($scope.devoir.competences.all[i]);
                    }
                    if ($scope.devoir.hasOwnProperty('id')) {
                        $scope.updateFilter();
                    }
                });
            }
            if ($scope.devoir.dateDevoir === undefined
                && $scope.devoir.date !== undefined) {
                $scope.devoir.dateDevoir = new Date($scope.devoir.date);
            }
            // Chargement des enseignements et compétences en fonction de la classe
            // evaluations.enseignements.sync($scope.devoir.id_groupe);

            if ($location.path() === "/devoirs/list") {
                $scope.devoir.id_type = $scope.search.type.id;
                $scope.devoir.id_sousmatiere = $scope.search.sousmatiere.id;
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
        let getClassesMatieres = function (idClasse) {
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
        };

        $scope.searchOrFirst = (key, collection) => {
            if ($scope.search[key] && $scope.search[key] !== "*") {
                return $scope.search[key];
            } else {
                return _.first(collection);
            }
        };
        /**
         * Set les matière en fonction de l'identifiant de la classe
         */
        $scope.setClasseMatieres = function () {
            getClassesMatieres($scope.devoir.id_groupe).then((matieres) => {
                $scope.devoir.matieresByClasse = matieres;
                if ($scope.devoir.matieresByClasse.length === 1) $scope.devoir.id_matiere = $scope.devoir.matieresByClasse[0].id;
                $scope.selectedMatiere();
            });
        };

        $scope.deleteDevoir = function () {
            if ($scope.selected.devoirs.list.length > 0) {
                $scope.selected.devoirs.list.forEach(function (d) {
                        d.remove().then((res) => {
                            evaluations.devoirs.sync();
                            evaluations.devoirs.on('sync', function () {
                                $scope.opened.lightbox = false;
                                var index = $scope.selected.devoirs.list.indexOf(d);
                                if (index > -1) {
                                    $scope.selected.devoirs.list = _.without($scope.selected.devoirs.list, d);
                                }
                                $scope.goTo('/devoirs/list');
                                utils.safeApply($scope);
                            });
                        })
                            .catch(() => {
                                notify.error("evaluation.delete.devoir.error");
                            });
                    }
                );
            }
            $scope.opened.evaluation.suppretionMsg2 = false;
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
            $scope.devoir.date_publication = $scope.getDateFormated($scope.devoir.datePublication);

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
                $scope.devoir.coefficient = parseInt($scope.devoir.coefficient);
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
            $scope.devoir.save($scope.devoir.competencesAdd,
                $scope.devoir.competencesRem, $scope.devoir.competencesUpdate).then((res) => {
                evaluations.structure.devoirs.sync().then(() => {
                    if ($location.path() === "/devoir/create") {
                        if (res !== undefined) {
                            let _devoir = evaluations.structure.devoirs.findWhere({id: res.id});
                            evaluations.structure.devoirs.getPercentDone(_devoir).then(() => {
                                utils.safeApply($scope);
                                $location.path("/devoir/" + res.id);
                            });
                        }

                    } else if ($location.path() === "/releve") {
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
        $scope.getReleve = function () {
            if ($scope.releveNote !== undefined) {
                delete $scope.releveNote;
            }
            if ($scope.elementProgrammeDisplay !== undefined) {
                delete $scope.elementProgrammeDisplay;
            }
            if ($scope.selected.devoirs.list !== undefined) {
                for (let i = 0; i < $scope.selected.devoirs.list.length; i++) {
                    $scope.selected.devoirs.list[i].selected = false;
                }
                $scope.selected.devoirs.list = [];
            }

            if ($scope.search.classe && $scope.search.classe !== '*' && $scope.search.classe.id !== undefined
                && $scope.search.matiere && $scope.search.matiere !== '*' && $scope.search.matiere.id !== undefined
                && _.findWhere($scope.evaluations.devoirs.all, {id_groupe: $scope.search.classe.id})
                && $scope.search.periode !== '*') {

                let p = {
                    idEtablissement: evaluations.structure.id,
                    idClasse: $scope.search.classe.id,
                    idMatiere: $scope.search.matiere.id,
                    idPeriode: null
                };

                if ($scope.search.periode) {
                    p.idPeriode = $scope.search.periode.id_type;
                }

                let releve = new ReleveNote(p);
                releve.sync().then(() => {
                    if (releve.devoirs.all.length !== 0) {
                        releve.synchronized.releve = true;
                        evaluations.releveNotes.push(releve);
                        $scope.releveNote = releve;
                        if ($scope.releveNote.elementProgramme !== undefined) {
                            $scope.elementProgrammeDisplay = $scope.releveNote.elementProgramme.texte;
                        }
                    }
                    if (releve.isNN) {
                        $scope.toogleDevoirNote();
                    }
                    utils.safeApply($scope);
                });

                $scope.openedStudentInfo = false;
                utils.safeApply($scope);
            }
        };

        /**
         * Position l'objet matière sur le devoir en cours de création
         */
        $scope.selectedMatiere = function () {
            var matiere = evaluations.matieres.findWhere({id: $scope.devoir.id_matiere});
            if (matiere !== undefined) $scope.devoir.matiere = matiere;
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
            if ($scope.classes === undefined || idClasse === null || idClasse === ''
                || ($scope.classes === undefined || $scope.evaluations.classes === undefined)
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
         * @param idSousMatiere identifiant de la sous matière
         * @returns {any} libelle de la sous matière
         */
        $scope.getLibelleSousMatiere = function (idSousMatiere) {
            if (idSousMatiere === null || idSousMatiere === "" || idSousMatiere === undefined) return "";
            $scope.selectedMatiere();
            return _.findWhere($scope.devoir.matiere.sousMatieres.all, {id: parseInt(idSousMatiere)}).libelle;
        };


        /*  $scope.getLibelleDevoir = function (id) {
         let devoir = $scope.devoirs.findWhere({id : id});
         if (devoir !== undefined) return devoir.name;
         };
         */
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
        $scope.getMoyenne = function (idPeriode,moyennes, moyennesFinales) {
            let _moyenneFinale = _.findWhere(moyennesFinales, {id_periode: idPeriode});
            if (_moyenneFinale !== undefined && _moyenneFinale !== null && _moyenneFinale.moyenne !== undefined) {
                return _moyenneFinale.moyenne;
            }
            let _moyenne = _.findWhere(moyennes, {id_periode: idPeriode});
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

        $scope.getLibellePositionnement = function (positionnementCalcule) {
            return lang.translate("evaluations.positionnement.calculee") + " : " + positionnementCalcule;
        };
            /**
         * Séquence d'enregistrement d'une annotation
         * @param evaluation évaluation à enregistrer
         * @param $event evenement déclenchant
         * @param eleve élève propriétaire de l'évaluation
         */
        $scope.saveAnnotationDevoirEleve = function (evaluation, $event, eleve) {
            if (evaluation.id_annotation !== undefined
                && evaluation.id_annotation > 0) {
                if (evaluation.oldId_annotation !== evaluation.id_annotation && evaluation.oldValeur !== evaluation.valeur) {
                    evaluation.saveAnnotation().then((res) => {
                        let annotation = _.findWhere($scope.evaluations.annotations.all, {id: evaluation.id_annotation});
                        evaluation.oldValeur = annotation.libelle_court;
                        evaluation.valeur = annotation.libelle_court;
                        delete evaluation.id;
                        delete evaluation.data.id;
                        for (let i = 0; i < evaluation.competenceNotes.all.length; i++) {
                            evaluation.competenceNotes.all[i].evaluation = -1;
                        }
                        evaluation.oldId_annotation = evaluation.id_annotation;
                        $scope.calculStatsDevoir();
                        evaluation.valid = true;
                        utils.safeApply($scope);
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
        $scope.deleteAnnotationDevoir = function (evaluation, resetValeur) {
            if (evaluation.id_annotation !== undefined
                && evaluation.id_annotation !== null
                && evaluation.id_annotation > 0) {
                evaluation.deleteAnnotationDevoir().then((res) => {
                    delete evaluation.oldId_annotation;
                    delete evaluation.id_annotation;
                    if (resetValeur) {
                        evaluation.oldValeur = "";
                        evaluation.valeur = "";
                        for (let i = 0; i < evaluation.competenceNotes.all.length; i++) {
                            evaluation.competenceNotes.all[i].evaluation = -1;
                            delete evaluation.competenceNotes.all[i].id;
                        }
                    }
                    $scope.calculStatsDevoir();
                    if (!evaluation.valid) {
                        evaluation.valid = true;
                    }
                    utils.safeApply($scope);
                });
            } else {
                evaluation.id_annotation = evaluation.oldId_annotation;
            }
        };

        /**
         * Séquence d'enregistrement d'une évaluation
         * @param evaluation évaluation à enregistrer
         * @param $event evenement déclenchant
         * @param eleve élève propriétaire de l'évaluation
         */
        $scope.saveNoteDevoirEleve = function (evaluation, $event, eleve) {
            if (evaluation !== undefined && ((evaluation.valeur !== evaluation.oldValeur) || (evaluation.oldAppreciation !== evaluation.appreciation))) {
                let reg = /^[0-9]+(\.[0-9]{1,2})?$/;
                if (evaluation.data !== undefined && evaluation.data.id_appreciation !== undefined && evaluation.id_appreciation === undefined) {
                    evaluation.id_appreciation = evaluation.data.id_appreciation;
                }
                // On est dans le cas d'une sauvegarde ou création d'appréciation
                if (evaluation.oldAppreciation !== undefined
                    && evaluation.oldAppreciation !== evaluation.appreciation
                    && evaluation.appreciation !== undefined && evaluation.appreciation !== '') {
                    evaluation.saveAppreciation().then((res) => {
                        evaluation.oldAppreciation = evaluation.appreciation;
                        if (res.id !== undefined) {
                            evaluation.id_appreciation = res.id;
                            evaluation.data.id_appreciation = res.id;
                        }
                        utils.safeApply($scope);
                    });
                }
                else {

                    // On est dans le cas d'une suppression d'appréciation
                    if (evaluation.id_appreciation !== undefined && evaluation.appreciation === "") {
                        evaluation.deleteAppreciation().then((res) => {
                            evaluation.oldAppreciation = evaluation.appreciation;
                            if (res.rows === 1) {
                                evaluation.id_appreciation = undefined;
                                evaluation.data.id_appreciation = undefined;
                            }
                            utils.safeApply($scope);
                        });
                    } else {
                        // On est dans le cas d'une sauvegarde d'une note ou d'annotation
                        if (evaluation.data.id !== undefined && evaluation.id === undefined) {
                            evaluation.id = evaluation.data.id;
                        }
                        let annotation = _.findWhere($scope.evaluations.annotations.all, {libelle_court: evaluation.valeur});
                        let oldAnnotation = _.findWhere($scope.evaluations.annotations.all, {id: evaluation.oldId_annotation});
                        if (!reg.test(evaluation.valeur) && ((annotation !== undefined && annotation !== null && annotation.id !== evaluation.oldId_annotation) ||
                                (oldAnnotation !== undefined && annotation === undefined))) {
                            if (oldAnnotation !== undefined && annotation === undefined) {
                                $scope.deleteAnnotationDevoir(evaluation, true);
                            } else {
                                evaluation.id_annotation = annotation.id;
                                $scope.saveAnnotationDevoirEleve(evaluation, $event, eleve);
                            }
                        } else {
                            if ((evaluation.oldValeur !== undefined && evaluation.oldValeur !== evaluation.valeur)
                                || evaluation.oldAppreciation !== undefined && evaluation.oldAppreciation !== evaluation.appreciation) {
                                if (evaluation.valeur !== "" && evaluation.valeur && reg.test(evaluation.valeur) && evaluation.valeur !== null) {
                                    let devoir = evaluations.devoirs.findWhere({id: evaluation.id_devoir});
                                    if (devoir !== undefined) {
                                        if (parseFloat(evaluation.valeur) <= devoir.diviseur && parseFloat(evaluation.valeur) >= 0) {
                                            evaluation.save().then((res) => {
                                                evaluation.valid = true;
                                                evaluation.oldValeur = evaluation.valeur;
                                                if (res.id !== undefined) {
                                                    evaluation.id = res.id;
                                                    evaluation.data.id = res.id;
                                                }

                                                if ($location.$$path === '/releve') {
                                                    $scope.calculerMoyenneEleve(eleve, $scope.releveNote.devoirs.all);
                                                    $scope.calculStatsDevoirReleve(_.findWhere($scope.releveNote.devoirs.all, {id: evaluation.id_devoir}));
                                                } else {
                                                    $scope.calculStatsDevoir();
                                                }
                                                $scope.opened.lightbox = false;
                                                delete $scope.selected.eleve;
                                                utils.safeApply($scope);
                                            });
                                        } else {
                                            notify.error(lang.translate("error.note.outbound") + devoir.diviseur);
                                            evaluation.valeur = evaluation.oldValeur;
                                            evaluation.valid = false;
                                            utils.safeApply($scope);
                                            if ($event !== undefined && $event.target !== undefined) {
                                                $event.target.focus();
                                            }
                                            return;
                                        }
                                    }
                                } else {
                                    if (evaluation.id !== undefined && evaluation.valeur === "" && (evaluation.id_annotation === undefined || evaluation.id_annotation < 0)) {
                                        evaluation.delete().then((res) => {
                                            evaluation.valid = true;
                                            evaluation.oldValeur = evaluation.valeur;
                                            if ($location.$$path === '/releve') {
                                                $scope.calculerMoyenneEleve(eleve, $scope.releveNote.devoirs.all);
                                                $scope.calculStatsDevoirReleve(_.findWhere($scope.releveNote.devoirs.all, {id: evaluation.id_devoir}));
                                                if (res.rows === 1) {
                                                    evaluation.id = undefined;
                                                    evaluation.data.id = undefined;
                                                }
                                            } else {
                                                if (res.rows === 1) {
                                                    evaluation.id = undefined;
                                                    evaluation.data.id = undefined;
                                                }
                                                $scope.calculStatsDevoir();

                                            }
                                            utils.safeApply($scope);
                                        });
                                    } else if (evaluation.id !== undefined && evaluation.valeur === "" && (evaluation.id_annotation !== undefined && evaluation.id_annotation > 0)) {
                                        $scope.deleteAnnotationDevoir(evaluation, true);
                                    } else {
                                        if (evaluation.valeur !== "" && !_.findWhere($scope.evaluations.annotations.all, {libelle_court: evaluation.valeur})) {
                                            // notify.error(lang.translate("error.note.invalid"));
                                            // evaluation.valid = false;
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
        $scope.calculerMoyenneEleve = function (eleve, devoirs) {
            eleve.getMoyenne(devoirs).then(() => {
                utils.safeApply($scope);
            });
        };

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
            $event.target.select();
        };

        /**
         * Affiche les informations d'un devoir en fonction de l'objet passé en paramètre
         * @param obj objet de type Evaluation ou de type Devoir
         */
        $scope.getDevoirInfo = function (obj) {
            if (template.isEmpty('leftSide-devoirInfo')) template.open('leftSide-devoirInfo', 'enseignants/informations/display_devoir');
            if (obj instanceof Devoir) $scope.informations.devoir = obj;
            else if (obj instanceof Evaluation) {
                var devoir = $scope.releveNote.devoirs.findWhere({id: obj.id_devoir});
                if (devoir !== undefined) $scope.informations.devoir = devoir;
            }

            if ($location.$$path === '/releve') {
                $scope.openLeftMenu("openedDevoirInfo", false);
                if ($scope.informations.devoir !== undefined &&
                    $scope.informations.devoir.statistiques === undefined) {
                    $scope.informations.devoir.statistiques = {
                        percentDone: $scope.informations.devoir.percent
                    };

                }
                utils.safeApply($scope);
            }
        };


        /**
         * Retourne les informations relatives à un élève
         * @param eleve élève
         */
        $scope.getEleveInfo = function (eleve) {
            template.close('leftSide-userInfo');
            utils.safeApply($scope);
            template.open('leftSide-userInfo', 'enseignants/informations/display_eleve');
            $scope.informations.eleve = eleve;
            delete $scope.informations.competencesNotes;
            $scope.informations.competencesNotes = $scope.informations.eleve.competencesNotes;
            utils.safeApply($scope);
        };

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

        $scope.isValidClasse = (idClasse) => {
            if ($scope.classes !== undefined) {
                return $scope.classes.findWhere({id: idClasse, remplacement: false}) !== undefined;
            }
        };

        $scope.filterValidClasse = () => {
            return (item) => {
                return $scope.isValidClasse(item.id_groupe || item.id);
            };
        };

        $rootScope.notYear = () => {
            return (periode) => {
                return periode.id !== null;
            };
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
        $scope.displaySuiviEleve = function (eleve) {
            $scope.informations.eleve = eleve;
            $scope.search.eleve = eleve;
            $scope.selected.eleve = eleve;
            $scope.displayFromClass = true;
            $scope.displayFromEleve = true;
            utils.safeApply($scope);
            $scope.goTo("/competences/eleve");
        };

        $scope.pOFilterEval = {
            limitTo: 22
        };

        /**
         *  Initialiser le filtre de recherche pour faire disparaitre la liste
         *  des élèves
         *
         */
        $scope.cleanRoot = function () {
            let elem = angular.element(".autocomplete");

            for (let i = 0; i < elem.length; i++) {
                elem[i].style.height = "0px";
            }
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
            if (classe.periodes.empty()) {
                await classe.periodes.sync();
            }
            let current_periode = _.findWhere(classe.periodes.all, {id_type: parseInt(devoir.id_periode)});
            if (current_periode !== undefined) {
                let start_datePeriode = current_periode.timestamp_dt;
                let end_datePeriode = current_periode.timestamp_fn;
                let date_saisie = current_periode.date_fin_saisie;

                $scope.errDatePubli = (moment($scope.devoir.datePublication).diff(
                    moment($scope.devoir.dateDevoir), "days") < 0);
                $scope.errDateDevoir = !(moment($scope.devoir.dateDevoir).isBetween(
                    moment(start_datePeriode), moment(end_datePeriode), 'days', '[]'));
                $scope.endSaisie = moment($scope.devoir.dateDevoir).isAfter(
                    moment(date_saisie), 'days', '[') || moment(new Date()).isAfter(
                    moment(date_saisie), 'days', '[');

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
            if (classe.periodes.empty()) {
                await classe.periodes.sync();
                utils.safeApply($scope);
            }
            let date_fin_saisie = _.findWhere(classe.periodes.all, {id_type: devoir.id_periode}).date_fin_saisie;

            return !(moment(date_fin_saisie).isAfter(moment(), "days") || Utils.isChefEtab());
        };

        /**
         * Cherche si la période de fin de saisie est dépassée pour un devoir donné
         * On ne vérifie pas si l'utilisateur est chef d'établissement
         * @param devoir
         */
        $scope.checkEndSaisieSeul = async (devoir) => {
            let classe = _.findWhere($scope.structure.classes.all, {id: devoir.id_groupe});
            if (classe.periodes.empty()) {
                await classe.periodes.sync();
                utils.safeApply($scope);
            }
            let date_fin_saisie = _.findWhere(classe.periodes.all, {id_type: devoir.id_periode}).date_fin_saisie;

            return moment().isAfter(date_fin_saisie, "days");
        };

        $scope.getPeriodeAnnee = () => {
            return {libelle: $scope.translate('viescolaire.utils.annee'), id: undefined}
        };

        $scope.getCurrentPeriode = async (classe) => {

            if (classe.periodes.empty()) {
                await classe.periodes.sync();
            }

            let currentPeriode = _.find(classe.periodes.all, (periode) => {
                return moment().isBetween(moment(periode.timestamp_dt), moment(periode.timestamp_fn), 'days', '[]');
            });
            return currentPeriode != null ? currentPeriode : -1;
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
                return false;
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
            $scope.devoirs = evaluations.structure.devoirs;
            $scope.enseignements = evaluations.structure.enseignements;
            $scope.matieres = evaluations.structure.matieres;
            $scope.releveNotes = evaluations.structure.releveNotes;
            $scope.classes = evaluations.structure.classes;
            $scope.types = evaluations.structure.types;
            $scope.eleves = evaluations.structure.eleves;
            $scope.enseignants = evaluations.structure.enseignants;
            $scope.usePerso = evaluations.structure.usePerso;
            $scope.useDefaut = !$scope.usePerso;
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

        evaluations.sync()
            .then(() => {
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
            })
            .catch(() => {
                $location.path() === '/disabled' ?
                    executeAction() :
                    $location.path('/disabled');
                $location.replace();
            });

        $scope.saveTheme = function () {
            $rootScope.chooseTheme();
        };

        $scope.updateColorAndLetterForSkills = function (location) {

            $scope.niveauCompetences = $scope.selectCycleForView(location);
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
            if (usePerso === 'true') {
                evaluations.structure.niveauCompetences.sync(false).then(() => {
                    evaluations.structure.niveauCompetences.first().markUser().then(() => {
                        $scope.structure.usePerso = 'true';
                        $scope.updateColorAndLetterForSkills();
                        //utils.safeApply($scope);
                    });
                });

            }
            else if (usePerso === 'false') {
                evaluations.structure.niveauCompetences.sync(true).then(() => {
                    evaluations.structure.niveauCompetences.first().unMarkUser().then(() => {
                        $scope.structure.usePerso = 'false';
                        $scope.updateColorAndLetterForSkills();
                        //utils.safeApply($scope);
                    });
                });
            }
        };

        $scope.togglePanel = function ($event) {
            $scope.showPanel = !$scope.showPanel;
            $event.stopPropagation();
        };


        $rootScope.$on('close-panel', function (e) {
            $scope.showPanel = false;
        });


        $scope.printCartouche = (unType?: String) => {

            let url;
            if (unType) {
                url = '/competences/devoirs/print/' + $scope.currentDevoir.id + '/formsaisie';
            } else {
                switch ($scope.printOption.fileType) {
                    case 'cartouche' : {
                        url = "/competences/devoirs/print/" + $scope.currentDevoir.id + "/cartouche?eleve=" + $scope.printOption.byEleve
                            + '&color=' + $scope.printOption.inColor + "&nbr=" + $scope.printOption.cartoucheNmb + "&image=" + $scope.printOption.image;
                        break;
                    }
                    case 'formSaisie' : {
                        url = '/competences/devoirs/print/' + $scope.currentDevoir.id + '/formsaisie';
                        break;
                    }
                }
            }
            $scope.exportDevoirObj.errExport = false;
            $scope.printOption.display = false;
            utils.safeApply($scope);
            location.replace(url);
        };

        $scope.exportDevoirObj = {};
        $scope.exportRelCompObj = {};
        $scope.exportDevoir = async (idDevoir, textMod = false) => {
            let url = "/competences/devoirs/print/" + idDevoir + "/export?text=" + textMod;
            http().getJson(url + "&json=true").error(() => {
                $scope.exportDevoirObj.errExport = true;
                utils.safeApply($scope);
            }).done((result) => {
                $scope.opened.evaluation.exportDevoirLB = false;
                $scope.textModExport = false;
                $scope.exportDevoirObj.errExport = false;
                $scope.printOption.display = false;
                utils.safeApply($scope);
                location.replace(url);
            });
            utils.safeApply($scope);
        };

        $scope.exportReleveComp = async (idEleve: String, idPeriode: Number, textMod: Boolean = false, exportByEnseignement: Boolean) => {
            let url = "/competences/releveComp/print/export?text=" + textMod;
            url += "&idEleve=" + idEleve;
            for (var m = 0; m < $scope.selected.matieres.length; m++) {
                url += "&idMatiere=" + $scope.selected.matieres[m];
            }
            if (idPeriode) {
                url += "&idPeriode=" + idPeriode;
            }
            if ($scope.forClasse) {
                url += "&idClasse=" + $scope.search.classe.id;
            }
            url += "&byEnseignement=" + exportByEnseignement;
            await http().getJson(url + "&json=true")
                .error((result) => {
                    $scope.errorResult(result);
                    utils.safeApply($scope);
                })
                .done(() => {
                    delete $scope.releveComp;
                    $scope.releveComp = {
                        textMod: true
                    };
                    $scope.opened.releveComp = false;
                    location.replace(url);
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
                default :
                    $scope.exportRecapEvalObj.errExport = true;
            }
        }

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
        }

        $scope.disabledExport = function () {
            return (_.findIndex($scope.allMatieresSorted, {select: true}) === -1) || typeof($scope.releveComp.periode) === 'undefined'
        }


        $scope.closeWarningMessages = function () {
            $scope.evalNotFound = false;
            $scope.periodeNotFound = false;
            $scope.classeNotFound = false;
            $scope.etabNotFound = false;
            $scope.bfcNotFound = false;
            $scope.elevesNotFound = false;
            $scope.cycleNotFound = false;
            $scope.exportRelCompObj.errExport = false;
            $scope.exportRecapEvalObj.errExport = false;
        }

        $scope.openedLigthbox = function (classe) {
            $scope.opened.releveComp = true;
            $scope.releveComp.periode = $scope.search.periode;
            $scope.releveComp.textMod = true;
            $scope.closeWarningMessages();
            $scope.selectUnselectMatieres(false);
            classe ? $scope.forClasse = true : $scope.forClasse = false;
        }

        $scope.getFormatedDate = function (date) {
            return moment(date).format("DD/MM/YYYY");
        };

        $scope.openedRecapEval = function () {
            $scope.opened.recapEval = true;
            $scope.suiviClasse.periode = $scope.search.periode;
            $scope.disabledExportSuiviClasse = typeof($scope.suiviClasse.periode) === 'undefined';
            $scope.closeWarningMessages();
            utils.safeApply($scope);
        };

        $scope.changePrintSuiviClasse = function (option) {
            $scope.printSuiviClasse = option;
            if (option === "printReleveComp")
                $scope.disabledExportSuiviClasse = $scope.allUnselect || typeof($scope.suiviClasse.periode) === 'undefined';
            if (option === "printRecapEval")
                $scope.disabledExportSuiviClasse = typeof($scope.suiviClasse.periode) === 'undefined';
        };

        $scope.initDefaultMatiere = function () {
            if ($scope.matieres.all.length === 1) {
                $scope.search.matiere = $scope.matieres.all[0];
            }
        };

        $scope.initMoyenneFinale = function (eleve) {
            if (eleve.moyenneFinale === undefined || eleve.moyenneFinale === null) {
                eleve.moyenneFinale = eleve.moyenne;
            }

        };

        $scope.disabledSaisieMoyenne = function () {
            if ($scope.search.periode.id === null || $scope.search.periode === "*"
                || ($scope.releveNote !== undefined && $scope.releveNote.isNN)) {
                return true;
            }
            else {
                if ($scope.isChefEtab()) {
                    return false;
                }
                else {
                    if ($scope.releveNote === undefined || $scope.releveNote.classe === undefined) {
                        return true;
                    } else {
                        let selectedPeriode = _.findWhere($scope.releveNote.classe.periodes.all,
                            {id_type: $scope.search.periode.id});
                        if (selectedPeriode !== undefined) {
                            return moment().isAfter(moment(selectedPeriode.date_fin_saisie), "days");
                        }
                        else {
                            return true;
                        }
                    }
                }
            }
        };

        $scope.saveMoyenneFinaleEleve = function (eleve) {
            if (eleve.moyenneFinale !== undefined && eleve.moyenneFinale !== null) {

                let reg = /^[0-9]+(\.[0-9]{1,2})?$/;
                if ((reg.test(eleve.moyenneFinale) && parseInt(eleve.moyenneFinale) < 20)
                    || eleve.moyenneFinale === "") {
                    eleve.oldMoyenneFinale = eleve.moyenneFinale;
                    $scope.releveNote.saveMoyenneFinaleEleve(eleve).then(() => {
                        $scope.updateHistorique(eleve, 'moyenneFinale');
                    });
                }
                else {
                    notify.error(lang.translate("error.average.outbound"));
                    eleve.moyenneFinale = eleve.oldMoyenneFinale;
                    utils.safeApply($scope);
                }
            }
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

        $scope.saveAppreciationMatierePeriodeEleve = function (eleve) {
            if (eleve.appreciation_matiere_periode !== undefined) {
                if (eleve.appreciation_matiere_periode.length <= $scope.MAX_CHAR_APPRECIATION_LENGTH) {
                    $scope.releveNote.saveAppreciationMatierePeriodeEleve(eleve);
                }
                else {
                    notify.error(lang.translate("error.char.outbound") +
                        $scope.MAX_CHAR_APPRECIATION_LENGTH);
                }
            }
            utils.safeApply($scope);
        };

        $scope.syncSousDomaines = function (id) {
            $scope.sousDomaines = _.where(evaluations.sousDomainesEnseignements, {
                id_domaine: id
            });
        }

        $scope.openElementProgramme = function openElementProgramme() {
            $scope.opened.elementProgramme = !$scope.opened.elementProgramme;
        }

        $scope.saveElementProgramme = function (texte) {
            if (texte !== undefined) {
                if (texte.length <= $scope.MAX_CHAR_APPRECIATION_LENGTH) {
                    $scope.releveNote.saveElementProgramme(texte).then(() => {
                        $scope.getReleve();
                    });
                    $scope.opened.lightbox = false;
                }
                else {
                    notify.error(lang.translate("error.char.outbound") +
                        $scope.MAX_CHAR_APPRECIATION_LENGTH);
                }
            }
            utils.safeApply($scope);
        };

        $scope.openEditElementProgramme = function () {


            $scope.releveNote.syncDomainesEnseignement().then(() => {
                $scope.releveNote.syncSousDomainesEnseignement().then(() => {
                    $scope.releveNote.elementProgramme.texte = $scope.elementProgrammeDisplay;
                    $scope.aideSaisie.cycle = null;
                    $scope.aideSaisie.domaineEnseignement = null;
                    template.open('lightboxContainer', 'enseignants/releve_notes/elements_programme');
                    $scope.opened.lightbox = true;
                    utils.safeApply($scope);
                });
            });


        }

        $scope.addProposition = function (libelleProposition) {
            if($scope.releveNote.elementProgramme.texte === undefined){
                $scope.releveNote.elementProgramme.texte = "";
            }
            if ($scope.releveNote.elementProgramme.texte !== "")
                $scope.releveNote.elementProgramme.texte += " ";
            $scope.releveNote.elementProgramme.texte += libelleProposition;
        }


        $scope.toogleDevoirNote = function () {
            if ($scope.releveNote !== undefined && $scope.releveNote.idPeriode !== null) {
                $scope.releveNote.toogle = !$scope.releveNote.toogle;
                utils.safeApply($scope);
                $('html, body')
                // on arrête toutes les animations en cours
                    .stop()
                $(".colDevoir").animate({
                    width: "toggle"
                }, 'slow');
            } else {
                $scope.releveNote.toogle = false;
            }
        };


        $scope.initDataLightBoxEleve = async function () {
            if ($scope.informations.eleve.evaluations.extended !== true) {
                _.forEach($scope.informations.eleve.evaluations.all, (evaluation) => {
                    _.extend(evaluation, $scope.releveNote.devoirs.findWhere({id: evaluation.id_devoir}));
                    _.extend(evaluation, {
                        competencesNotes:
                            _.where($scope.informations.eleve.competencesNotes, {id_devoir: evaluation.id_devoir})
                    });
                });
                $scope.informations.eleve.historiques = [];
                try {
                    await $scope.informations.eleve.getDetails($scope.releveNote.idEtablissement,
                        $scope.releveNote.idClasse,
                        $scope.releveNote.idMatiere);
                } catch (e) {
                    console.log(e);
                }

                let moyennneAnnee = 0;
                let nbMoyenneAnnee = 0;
                // Pour vérifier que si la moyenne finale de l'année a été modifiée
                let isMoyenneFinaleAnnee = false;

                let positionnementAnnee = 0;
                let nbPositionnementAnnee = 0;
                // Pour vérifier que si le Positionnement final de l'année a été modifié
                let isPositionnementFinaleAnnee = false;

                let historiqueAnnee;
                $scope.informations.eleve.historiques = [];
                _.forEach($scope.filteredPeriode, function (periode) {

                    // get moyenne auto eleve
                    let details_moyennes = _.findWhere($scope.informations.eleve.details.moyennes, {
                        id:
                            (periode.id_type !== null) ? parseInt(periode.id_type) : null
                    });
                    let moyenne = (details_moyennes !== undefined) ? details_moyennes.moyenne : "";

                    // get moyenne classe
                    let details_moyennes_classe = _.findWhere($scope.informations.eleve.details.moyennesClasse, {
                        id:
                            (periode.id_type !== null) ? parseInt(periode.id_type) : null
                    });
                    let moyenneClasse = (details_moyennes_classe !== undefined) ? details_moyennes_classe.moyenne : "";
                    if ($scope.releveNote.idPeriode === periode.id_type) {
                        $scope.informations.eleve.moyenneClasse = moyenneClasse;
                    }

                    // get appreciation
                    let details_appreciations = _.findWhere($scope.informations.eleve.details.appreciations,
                        {id_periode: (periode.id_type !== null) ? parseInt(periode.id_type) : null});
                    let appreciation = (details_appreciations !== undefined) ?
                        details_appreciations.appreciation_matiere_periode : "";

                    // get moyenne finale
                    let details_moyennes_finales = _.findWhere($scope.informations.eleve.details.moyennes_finales,
                        {id_periode: (periode.id_type !== null) ? parseInt(periode.id_type) : null});
                    let moyenneFinale = (details_moyennes_finales !== undefined) ? details_moyennes_finales.moyenne : "";

                    // get positionnement Auto
                    let details_pos_auto = _.findWhere(
                        $scope.informations.eleve.details.positionnements_auto,
                        {id_periode: (periode.id_type !== null) ? parseInt(periode.id_type) : null});

                    // Déduction du positionnement par défaut en fonction de l'échelle de convertion
                    // Ajout de 1 à la moyenne pour rentrer dans l'échelle de conversion
                    // (Logique prise au calcul du niveau dans le BFC).
                    let moyenne_convertie = (details_pos_auto !== undefined) ? (utils.getMoyenneForBFC(
                        details_pos_auto.moyenne + 1,
                        $scope.releveNote.tableConversions.all)) : 0;
                    let positionnement = (moyenne_convertie !== -1) ? moyenne_convertie : 0;
                    $scope.informations.eleve.positionnementCalcule = positionnement;
                    // get positionnement final
                    let details_pos = _.findWhere(
                        $scope.informations.eleve.details.positionnements,
                        {id_periode: (periode.id_type !== null) ? parseInt(periode.id_type) : null});
                    let positionnementFinal = (details_pos !== undefined) ? details_pos.positionnement : "";
                    // initialisation du positionnement pour le détail élève
                    if ($scope.releveNote.idPeriode === periode.id_type) {
                        $scope.informations.eleve.positionnement =
                            (positionnementFinal !== "") ? positionnementFinal : (positionnement);
                    }

                    // On stocke la moyenne du trimestre pour le calcul de la moyenne à l'année
                    if (periode.id_type !== null &&
                        (details_moyennes_finales !== undefined || details_moyennes !== undefined)) {
                        nbMoyenneAnnee++;
                        if (details_moyennes_finales !== undefined) {
                            isMoyenneFinaleAnnee = true;
                            moyennneAnnee += parseInt(moyenneFinale);
                        } else {
                            moyennneAnnee += moyenne;
                        }
                    }
                    // On stocke le positionnement du trimestre pour le calcul du positionnement à l'année
                    if (periode.id_type !== null &&
                        ((positionnement !== undefined && positionnement > 0) || (details_pos !== undefined && details_pos > 0))) {
                        nbPositionnementAnnee++;
                        if (details_pos !== undefined) {
                            isPositionnementFinaleAnnee = true;
                            positionnementAnnee += details_pos.positionnement;
                        } else {
                            positionnementAnnee += positionnement;
                        }
                    }

                    if (periode.id_type !== null) {
                        $scope.informations.eleve.historiques.push({
                            periode: $scope.getI18nPeriode(periode),
                            moyenneClasse: moyenneClasse,
                            moyenne: moyenne,
                            moyenneFinale: moyenneFinale,
                            positionnement: positionnement,
                            positionnementFinal: positionnementFinal,
                            appreciation: appreciation,
                            idPeriode: periode.id_type
                        });
                    } else {
                        historiqueAnnee = {
                            periode: $scope.getI18nPeriode(periode),
                            moyenneClasse: moyenneClasse,
                            appreciation: appreciation,
                            idPeriode: periode.id_type
                        }
                    }

                });

                // On calcule la moyenne à l'année
                let moyenneFinaleAnnee;
                if (nbMoyenneAnnee !== 0) {
                    moyenneFinaleAnnee = (moyennneAnnee / nbMoyenneAnnee).toFixed(2);
                } else {
                    moyenneFinaleAnnee = "";
                }

                // On calcule le positionnement à l'année
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

                if (isPositionnementFinaleAnnee) {
                    historiqueAnnee.positionnementFinal = positionnementFinaleAnnee;
                } else {
                    historiqueAnnee.positionnement = positionnementFinaleAnnee;
                    historiqueAnnee.positionnementFinal = "";
                }

                $scope.informations.eleve.historiques.push(historiqueAnnee);

                $scope.informations.eleve.evaluations.extended = true;
                utils.safeApply($scope);
            }
        };

        $scope.openedLigthboxEleve = function (eleve, filteredPeriode) {
            $scope.getEleveInfo(eleve);
            $scope.filteredPeriode = filteredPeriode;
            $scope.opened.lightbox = true;
            $scope.initDataLightBoxEleve();
            template.open('lightboxContainer', 'enseignants/releve_notes/details_releve_periodique_eleve');
        };

        $scope.incrementReleveEleve = async function (num) {
            let index = _.findIndex($scope.releveNote.classe.eleves.all, {id: $scope.informations.eleve.id});
            if (index !== -1 && index + parseInt(num) >= 0
                && index + parseInt(num) < $scope.releveNote.classe.eleves.all.length) {
                $scope.informations.eleve = $scope.releveNote.classe.eleves.all[index + parseInt(num)];
                $scope.initDataLightBoxEleve();
            }
        };
        $scope.hasCompetences = function (devoir) {
            return devoir.nbcompetences > 0;
        };

        $scope.updateHistorique = function (eleve, colonne: string) {
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
                        historique.positionnementFinal = eleve.positionnement;
                        break;
                    default:
                        break;
                }

            }
            $scope.informations.eleve.evaluations.extended = false;
            $scope.initDataLightBoxEleve();
            utils.safeApply($scope);
        };
        $scope.hasCompetencesNotes = function (evaluations) {
            if (evaluations.all === undefined) {
                return false;
            } else {
                for (let i = 0; i < evaluations.all.length; i++) {
                    let evaluation = evaluations.all[i];
                    if (evaluation.nbcompetences > 0 && evaluation.competencesNotes.length > 0) {
                        return true;
                    }
                }
                return false;
            }
        };
        $scope.hasDevoirsEvalues = function (evaluations) {
            let hasDevoirsEvalues = _.where(evaluations.all, {is_evaluated: true});
            if (hasDevoirsEvalues.length > 0) {
                for (let i = 0; i < hasDevoirsEvalues.length; i++) {
                    if (hasDevoirsEvalues[i].valeur !== '') {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        };

        $scope.isNotFirstEleve = function (evaluations) {
            if ($scope.releveNote === undefined) {
                return false;
            }
            let index = _.findIndex($scope.releveNote.classe.eleves.all, {id: $scope.informations.eleve.id});
            if (index === 0) {
                return false;
            } else {
                return true;
            }
        }
        $scope.isNotLastEleve = function (evaluations) {
            if ($scope.releveNote === undefined) {
                return false;
            }
            let index = _.findIndex($scope.releveNote.classe.eleves.all, {id: $scope.informations.eleve.id});
            if (index === $scope.releveNote.classe.eleves.all.length - 1) {
                return false;
            } else {
                return true;
            }
        }
    }
]);
