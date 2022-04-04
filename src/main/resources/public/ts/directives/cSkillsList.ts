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
import {Domaine} from "../models/teacher";

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
                return (item.selected = parentItem.enseignement && parentItem.enseignement.selected || item.selected || false);
            };

            $scope.doNotCheckIfCreation = function(item){
                if ($location.path() === "/devoir/create") {
                    item.isSelected = false;
                }
            };

            $scope.openLightboxCreationCompetence = function(enseignement, competence) {
                $scope.creatingCompetence = true;
                $scope.newItem = $scope.initNewCompetence();
                $scope.id_cycle = competence.id_cycle;
                $scope.newItem.cycle = competence.id_cycle == 1 ? "Cycle 4" : "Cycle 3"; //TODO : Faire une récupération du libellé de cycle proprement
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
                    cycle: null,
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
                await http().getJson(`/competences/domaines?idStructure=${$scope.idEtablissement}&idCycle=${$scope.id_cycle}`)
                    .done((resDomaines) => {
                        if (resDomaines) {
                            let _res = [];
                            for (let i = 0; i < resDomaines.length; i++) {

                                let domaine = new Domaine(resDomaines[i]);
                                _res.push(domaine);
                            }
                            $scope.newItem.domaines = _res;
                            $scope.printDomaines = {
                                all: _res
                            };
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
                http().postJson(`competences/competence`, $scope.jsonCreateItem(item))
                    .done(() => {
                        $scope.creatingCompetence = false;
                        $scope.getDomaines();
                        notify.info('item.success.create');
                        utils.safeApply(this);
                    })
                    .error((res) => {
                        console.error(res);
                        $scope.creatingCompetence = false;
                        if (res.status === 401) {
                            notify.error('item.error.unautorize.create');
                            utils.safeApply(this);
                        }
                        else {
                            notify.error('item.error.create');
                            utils.safeApply(this);
                        }
                    })
            };

            $scope.jsonCreateItem = function (item) {
                return {
                    nom: item.libelle,
                    id_etablissement: $scope.idEtablissement,
                    id_parent: item.id_parent,
                    id_type: item.id_type,
                    id_enseignement: item.id_enseignement,
                    ids_domaine: item.ids_domaine,
                    id_cycle: $scope.id_cycle
                };
            };

            $scope.initHeader = function(item){
                if(item.open === undefined) {
                    return (item.open = false);
                }
            };

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

            $scope.toggleCheckbox = function(item, parentItem){
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