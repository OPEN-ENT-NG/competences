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

/**
 * Created by ledunoiss on 21/09/2016.
 */
import {ng, appPrefix, _, template, http, notify} from 'entcore';
import * as utils from "../utils/teacher";
import {Cycle, Domaine} from "../models/teacher";

/**
 * function-filter : méthode qui va checker si l'enseignement parcouru doit être affiché ou non
 * function-search : méthode qui va déplier les enseignements/compétences qui matche le mot clef recherché. Cette méthode surligne
 * également les mots recherchés
 * data : les enseignements parcourus
 * enseignements-filter : objet où l'on stocke si un enseignement est sélectionné ou non
 * competences-filter : objet où l'on stocke les noms au format html des compétences (pour le surlignement lors d'une recherche)
 * search : objet concernant la recherche
 */
export let cSkillsList = ng.directive("cSkillsList", function(){
    return {
        restrict : 'E',
        scope : {
            data : '=',
            devoir : '=',
            functionFilterCompetencesByDomaines : '=',
            functionFilterHidden : '=',
            enseignementsFilter : '=',
            competencesFilter: '=',
            search: '='
        },
        templateUrl : "/" + appPrefix + "/public/template/directives/cSkillsList.html",
        controller : ['$scope', '$sce','$timeout','$location', function($scope, $sce, $timeout,$location){
            $scope.initCheckBox = function(item, parentItem){
                //item.nomHtml = item.nom;

                // on regarde sur l'objet competencesLastDevoirList pour detecter quand il est charge
                // et pouvoir deplier l'arbre des competences selectionnees lors du dernier devoir
                $scope.$watch('devoir.competencesLastDevoirList', function (newValue, oldValue) {
                    if (newValue !== oldValue) {
                        $scope.initCheckBox(item, parentItem);
                    }
                }, true);
                var bLastCompetence = (_.findWhere($scope.devoir.competencesLastDevoirList, {id : item.id}) !== undefined);

                if(bLastCompetence) {
                    item.open = true;

                    var parent = item.composer;
                    while(parent !== undefined) {
                        parent.open = true;
                        parent = parent.composer;
                    }
                    $scope.safeApply();
                }
                $scope.opened = {
                    lightboxCreationCompetence : false
                };
                return (item.selected = parentItem.enseignement && parentItem.enseignement.selected || item.selected || false);
            };

            $scope.doNotCheckIfCreation = function(item){
                if ($location.path() === "/devoir/create") {
                    item.isSelected = false;
                }
            };

            $scope.openLightboxCreationCompetence = function(enseignement, competence, $event) {
                $event.stopPropagation();
                $scope.opened.lightboxCreationCompetence = true;
                $scope.openedEnseignementsIds = [];
                $scope.openedElementSignifiantsIds = [];
                $scope.newItem = $scope.initNewCompetence();
                $scope.newItem.cycle.id_cycle = competence.id_cycle;
                $scope.newItem.cycle.libelle = competence.id_cycle === 1 ? "Cycle 4" : "Cycle 3";
                $scope.newItem.enseignement = enseignement;
                $scope.newItem.elementSignifiant = competence;
                $scope.newItem.id_parent = competence.id;
                $scope.newItem.id_enseignement = enseignement.id;
                $scope.idEtablissement = $scope.devoir.id_etablissement;
                $scope.getDomaines();
                template.open('lightboxCreationCompetence', 'enseignants/creation_competence/lightbox_creation_competence');
                utils.safeApply($scope);
            };

            $scope.initNewCompetence = function() {
                return {
                    cycle: new Cycle(),
                    enseignement: null,
                    elementSignifiant: null,
                    domaines: null,
                    libelle: "",
                    ids_domaine: [],
                    id_type: 2,
                    ismanuelle: true
                }
            };

            $scope.getDomaines = async function () {
                $scope.idEtablissement = $scope.devoir.id_etablissement;
                await http().getJson(`/competences/domaines?idStructure=${$scope.idEtablissement}&idCycle=${$scope.newItem.cycle.id_cycle}`)
                    .done((resDomaines) => {
                        if (resDomaines) {
                            let _res = [];
                            for (let i = 0; i < resDomaines.length; i++) {

                                let domaine = new Domaine(resDomaines[i]);
                                _res.push(domaine);
                            }
                            $scope.newItem.domaines = _res;
                            $scope.initializeDomainesSelected($scope.newItem);
                        }
                    })
                    .error(function () {
                        console.error('domaine not founded');
                        $scope.newItem.domaines = [];
                    })
            };

            $scope.isStringUndefinedOrEmpty = function(name) {
                return (name === undefined || name.trim().length === 0)
            };

            $scope.initializeDomainesSelected = function (item) {
               item.domaines.forEach(domaine => {
                    if(_.contains(item.elementSignifiant.ids_domaine_int, domaine.id)){
                        domaine.selected = true;
                        $scope.selectDomaine(domaine);
                    }
                    domaine.domaines.all.forEach(sousDomaine => {
                        if(_.contains(item.elementSignifiant.ids_domaine_int, sousDomaine.id)){
                            sousDomaine.selected = true;
                            $scope.selectDomaine(sousDomaine);
                        }
                    });
                });
            }

            $scope.selectDomaine = function (domaine) {
                if (domaine.selected && !_.contains($scope.newItem.ids_domaine, domaine.id)) {
                    $scope.newItem.ids_domaine.push(domaine.id);
                }
                else if (!domaine.selected) {
                    $scope.newItem.ids_domaine =
                        _.without($scope.newItem.ids_domaine, domaine.id);
                }
                if ($scope.newItem.hasOwnProperty('id')) {
                    $scope.updatedDomaineId = domaine.id;
                    $scope.saveItem($scope.newItem, 'updateDomaine');
                }
            };

            $scope.saveItem = function (item) {
                $scope.registerOpened();
                http().postJson(`competences/competence`, $scope.jsonCreateItem(item))
                    .done(async (res) => {
                        $scope.opened.lightboxCreationCompetence = false;
                        $scope.id = res.id;
                        $scope.selectedTab = _.filter($scope.competencesFilter, function(item){
                            return item.isSelected;
                        });
                        await $scope.$emit('loadEnseignementsByClasse');
                        $scope.getDomaines();
                        notify.info('competence.createCompetence.success');
                        utils.safeApply(this);
                    })
                    .error((res) => {
                        console.error(res);
                        $scope.opened.lightboxCreationCompetence = false;
                        if (res.status === 401) {
                            notify.error('competence.createCompetence.error.unautorize');
                            utils.safeApply(this);
                        }
                        else {
                            notify.error('competence.createCompetence.error');
                            utils.safeApply(this);
                        }
                    })
            };

            $scope.$on('checkboxNewCompetence', function () {
                $scope.checkboxNewCompetence($scope.id);
            })

            $scope.registerOpened = function () {
                _.forEach($scope.data, ens => {
                    if(ens.open){
                        $scope.openedEnseignementsIds.push(ens.id);
                        _.forEach(ens.competences.all, elemSign => {
                            if(elemSign.open) {
                                $scope.openedElementSignifiantsIds.push(elemSign.id);
                            }
                        });
                    }
                });
            };

            $scope.reassignOpened = function() {
                _.forEach($scope.data, ens => {
                    if (_.contains($scope.openedEnseignementsIds, ens.id)){
                        ens.open = true;
                        _.forEach(ens.competences.all, elemSign => {
                            if(_.contains($scope.openedElementSignifiantsIds, elemSign.id)) {
                                elemSign.open = true;
                            }
                        });
                    }
                });
            };

            $scope.jsonCreateItem = function (item) {
                return {
                    nom: item.libelle,
                    id_etablissement: $scope.idEtablissement,
                    id_parent: item.id_parent,
                    id_type: item.id_type,
                    id_enseignement: item.id_enseignement,
                    ids_domaine: item.ids_domaine,
                    id_cycle: item.cycle.id_cycle
                };
            };

            $scope.checkboxNewCompetence = function (idItem) {
                let enseignement = _.findWhere($scope.data, {id: $scope.newItem.elementSignifiant.id_enseignement});
                let elemSign = _.findWhere(enseignement.competences.all, {id: $scope.newItem.elementSignifiant.id});
                let item = _.findWhere(elemSign.competences.all, {id: idItem});
                $scope.toggleCheckbox(item, $scope.newItem.elementSignifiant, true);
                enseignement.open = true;
                elemSign.open = true;
                $scope.reassignOpened();
                $scope.doNotApplySearchFilter();
                utils.safeApply(this);
            }

            $scope.initHeader = function(item){
                if(item.open === undefined) {
                    return (item.open = false);
                }
            };

            $scope.initAction = function(){
                $scope.mouseHovering = false;
            }

            $scope.safeApply = function(fn) {
                var phase = this.$root.$$phase;
                if(phase == '$apply' || phase == '$digest') {
                    if(fn && (typeof(fn) === 'function')) {
                        fn();
                    }
                } else {
                    this.$apply(fn);
                }
            };

            $scope.doNotApplySearchFilter = function(){
                $scope.search.haschange=false;
            };

            $scope.toggleCheckbox = function(item, parentItem, created?){
                if (created){
                    $scope.competencesFilter[item.id + '_' + item.id_enseignement].isSelected = true;
                    $scope.selectedTab.forEach(comp => {
                        $scope.competencesFilter[comp.data.id + '_' + comp.data.id_enseignement].isSelected = true;
                    });
                }

                $scope.emitToggleCheckbox(item, parentItem);
                var items = document.getElementsByClassName("competence_"+item.id);

                if(items != null && items !== undefined) {
                    for (var i = 0; i < items.length; i++) {
                        var sIdCompetence = items[i].getAttribute("id");

                        // on coche également les compétences similaires à item qui sont dans d'autres enseignements
                        if($scope.competencesFilter[item.id+"_"+item.id_enseignement].isSelected !== $scope.competencesFilter[sIdCompetence].isSelected) {

                            var competenceSimilaire = $scope.competencesFilter[sIdCompetence];

                            // simulation click
                            competenceSimilaire.isSelected = $scope.competencesFilter[item.id+"_"+item.id_enseignement].isSelected;

                            // appel de lamethode toggleCheckBox pour cocher les éventuelles sous competences et le parent
                            //$scope.toggleCheckbox(competenceSimilaire.data, competenceSimilaire.data.composer)

                            // si la competence a des sous competences
                            if(item.competences != undefined && item.competences.all.length > 0){
                                if(competenceSimilaire.data.competences != undefined && competenceSimilaire.data.competences.all.length) {
                                    // on coche toutes ces sous competences dans l'arbre de la competence similaire
                                    competenceSimilaire.data.competences.each(function (sousCompSimilaire) {

                                        // on ne coche QUE ces sous competences par les autres s'il y en a
                                        // car l'arbre de la competence similaire peut potentiellement contenir d'autres sous compétences
                                        var bSousCompetenceSimilaireInSousCompetenceOriginal =
                                            _.findWhere(item.competences.all, {id: sousCompSimilaire.id}) !== undefined;

                                        if (bSousCompetenceSimilaireInSousCompetenceOriginal) {
                                            $scope.competencesFilter[sousCompSimilaire.id + "_" + sousCompSimilaire.id_enseignement].isSelected = competenceSimilaire.isSelected;
                                        }
                                    });
                                }
                            } else {
                                $scope.$emit('checkParent', competenceSimilaire.data.composer, competenceSimilaire.data);
                            }
                        }
                    }
                }
            };

            $scope.emitToggleCheckbox = function(item, parentItem){
                if(item.competences !== undefined && item.competences.all.length > 0){
                    $scope.$emit('checkConnaissances', item);
                }else{
                    $scope.$emit('checkParent', parentItem, item);
                }
            };

            $scope.$on('checkConnaissances', function(event, parentItem){
                return (parentItem.competences.each(function(e){
                    $scope.competencesFilter[e.id + "_" + e.id_enseignement].isSelected
                        = $scope.competencesFilter[parentItem.id + "_" + parentItem.id_enseignement].isSelected
                        && !($scope.competencesFilter[e.id + "_" + e.id_enseignement].data.masque
                            && _.findWhere($scope.devoir.competences.all, {id: e.id}) === undefined);
                }));
            });

            // item pas utilise ici mais utilise dans la creation d'un devoir
            $scope.$on('checkParent', function(event, parentItem, item){
                return ($scope.competencesFilter[parentItem.id + "_" + parentItem.id_enseignement].isSelected = parentItem.competences.every(function(e){
                    let comp = $scope.competencesFilter[e.id + "_" + e.id_enseignement];
                    return comp.isSelected === true || comp.data.masque;
                }));
            });

            // start with the div hidden
            $scope.hovering = false;

            // create the timer variable
            var timer;

            // mouseenter event
            $scope.showIt = function (item) {
                timer = $timeout(function () {
                    item.hovering = true;
                }, 350);
            };

            // mouseleave event
            $scope.hideIt = function (item) {
                $timeout.cancel(timer);
                item.hovering = false;
            };

            $scope.isVisibleAndHasVisibleChildren = function (competence) {
                if(competence.masque) {
                    return false;
                }

                let hasVisibleChildren = false;
                for(var i = 0; i < competence.competences.all.length; i++) {
                    let item = competence.competences.all[i];
                    if(!item.masque) {
                        hasVisibleChildren = true;
                        break;
                    }
                }
                return hasVisibleChildren;
            };
        }]
    };
});