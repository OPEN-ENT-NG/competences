/**
 * Created by ledunoiss on 21/09/2016.
 */
import { ng, appPrefix, _ } from 'entcore';

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
            functionFilter : '=',
            functionSearch : '=',
            functionFilterCompetencesByDomaines : '=',
            functionFilterHidden : '=',
            enseignementsFilter : '=',
            competencesFilter: '=',
            search: '='
        },
        templateUrl : "/"+appPrefix+"/public/template/directives/cSkillsList.html",
        controller : ['$scope', '$sce','$timeout', function($scope, $sce, $timeout){

            $scope.initCheckBox = function(item, parentItem){

                //item.nomHtml = item.nom;

                // on regarde sur l'objet competencesLastDevoirList pour detecter quand il est charge
                // et pouvoir deplier l'arbre des competences selectionnees lors du dernier devoir
                $scope.$watch('devoir.competencesLastDevoirList', function (newValue, oldValue) {
                    if (newValue !== oldValue) {
                        $scope.initCheckBox(item, parentItem);
                    }
                }, true);
                var bLastCompetence = (_.findWhere($scope.devoir.competencesLastDevoirList, {id_competence : item.id}) !== undefined);

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
                    $scope.competencesFilter[e.id+"_"+e.id_enseignement].isSelected
                        = $scope.competencesFilter[parentItem.id+"_"+parentItem.id_enseignement].isSelected
                            && !($scope.competencesFilter[e.id + "_" + e.id_enseignement].data.masque
                                && _.findWhere($scope.devoir.competences.all,{id: e.id}) === undefined);
                }));
            });

            // item pas utilise ici mais utilise dans la creation d'un devoir
            $scope.$on('checkParent', function(event, parentItem, item){
                return ($scope.competencesFilter[parentItem.id+"_"+parentItem.id_enseignement].isSelected = parentItem.competences.every(function(e){
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

        }]
    };
});