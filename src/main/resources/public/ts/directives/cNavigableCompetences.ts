/**
 * Created by ledunoiss on 26/10/2016.
 */

/**
 * Created by ledunoiss on 21/09/2016.
 */
import { ng, $ } from 'entcore';

export let navigableCompetences = ng.directive('cNavigableCompetences', function(){
    return {
        restrict : 'A',
        link: function(scope, element, attrs) {

            scope.getNext = function(obj){
                var row = obj.closest('.navigable-inputs-row');

            };
            scope.findAncestor = function(el, cls) {
                while ((el = el.parentElement) && !el.classList.contains(cls));
                return el;
            };

            scope.findChildren = function(el, cls){
                return $(el).find('.'+cls);
            };

            scope.hasClass = function(el, cls){
                return el.className.indexOf(cls) > -1;
            };

            scope.findIndex = function(el, row){
                var i;
                for(i = 0; i < row.length; i++){
                    if(row[i] === el){
                        return i;
                    }
                }
                return -1;
            };

            scope.findInput = function(row) {
                for (let i = 0; i < row.children.length; i++) {
                    if (row.children[i].tagName === 'INPUT-TEXT-LIST') {
                        let inputTextList = row.children[i];
                        for (let j = 0 ; j < inputTextList.children.length; j++) {
                            if (inputTextList.children[j].tagName === 'INPUT') {
                                return inputTextList.children[j];
                            }
                        }
                    }
                }
                return null;
            };

            scope.findInputs = function(row) {
                let inputs = [];
                for (let i = 0 ; i < row.children.length; i++) {
                    if (row.children[i].tagName === 'INPUT-TEXT-LIST') {
                        let inputTextList = row.children[i];
                        for (let j = 0 ; j < inputTextList.children.length; j++) {
                            if (inputTextList.children[j].tagName === 'INPUT') {
                                inputs.push(inputTextList.children[j]);
                            }
                        }
                    }
                }
                return inputs;
            };

            scope.isCompetenceCell = function (cell) {
                return scope.hasClass(cell, 'competences-cell');
            };

            scope.isCompetence = function (input) {
                return scope.hasClass(input, 'competence-eval');
            };

            scope.findNavigableRow = function(row){
                return (scope.findChildren(row, 'navigable-inputs-row'))[0];
            };

            scope.findCompetence = function (cell) {
                return $(cell).find('.competence-eval')[0];
            };

            /**
             * Détermine si l'élément peut être sélectionner
             * @param moveToRow
             * @param index
             * @returns {any}
             */
            scope.canMove = function (expandChildren, index) {
                while (index < expandChildren.length) {
                    index ++;
                    let moveToRow = expandChildren[index];
                    let navigableCells = scope.findChildren(scope.findNavigableRow(moveToRow), 'navigable-cell');
                    if (moveToRow !== undefined && navigableCells.length > 0) {
                        return navigableCells[0];
                    }
                }
            }

            element.bind('keydown', function(event){
                var keys = {
                    enter : 13,
                    arrow : {left: 37, up: 38, right: 39, down: 40},
                    numbers : {zero : 96, one : 97, two : 98, three : 99, four : 100},
                    shiftNumbers : {zero : 48, one : 49, two : 50, three : 51, four : 52}
                };
                var key = event.which | event.keyCode;

                if ($.inArray(key, [keys.arrow.left, keys.arrow.up, keys.arrow.right, keys.arrow.down, keys.enter,
                        keys.numbers.zero, keys.numbers.one, keys.numbers.two, keys.numbers.three, keys.numbers.four,
                        keys.shiftNumbers.zero, keys.shiftNumbers.one, keys.shiftNumbers.two, keys.shiftNumbers.three, keys.shiftNumbers.four]) < 0) { return; }
                var input = event.target;
                var td = scope.findAncestor(event.target, 'navigable-cell');
                var row = scope.findAncestor(td, 'navigable-inputs-row');
                var children = scope.findChildren(row, 'navigable-cell');
                var index = scope.findIndex(td, children);
                var moveTo = null;
                switch(key){
                    case keys.arrow.left:{
                        var moveToRow = null;

                        // si champs de type input
                        if (!scope.isCompetence(input)) {

                            // si on est en debut de champs au passe au champs précédent
                            if (input.selectionStart === 0) {
                                // si on est pas en debut de ligne, on passe à la cellule précédente
                                if (index > 0) {
                                    moveTo = children[index - 1];
                                } else {
                                    // sinon on est en debut de ligne, on doit remonter au dernier element de la ligne précédente
                                    var tr = scope.findAncestor(td, 'navigable-row');
                                    var pos = scope.findIndex(td, children);
                                    var expandChildren = scope.findChildren(scope.findAncestor(tr, 'expandable-liste'), 'navigable-row');
                                    var trIndex = scope.findIndex(tr, expandChildren);
                                    var moveToRow = expandChildren[trIndex-1];
                                    if (moveToRow !== null) {
                                        var navigableCells = scope.findChildren(scope.findNavigableRow(moveToRow), 'navigable-cell');
                                        moveTo = navigableCells[navigableCells.length-1];
                                    }
                                }
                            }
                        } else {
                            // si on est pas en debut de ligne, on passe à la cellule précédente
                            if (index > 0) {
                                moveTo = children[index - 1];

                            // sinon on est en debut de ligne, on doit remonter au dernier element de la ligne précédente
                            } else {
                                // si on est en debut de ligne, on doit remonter au dernier element de la ligne précédente
                                var tr = scope.findAncestor(td, 'navigable-row');
                                var pos = scope.findIndex(td, children);
                                var expandChildren = scope.findChildren(scope.findAncestor(tr, 'expandable-liste'), 'navigable-row');
                                var trIndex = scope.findIndex(tr, expandChildren);
                                var moveToRow = expandChildren[trIndex-1];
                                if (moveToRow !== null) {
                                    var navigableCells = scope.findChildren(scope.findNavigableRow(moveToRow), 'navigable-cell');
                                    moveTo = navigableCells[navigableCells.length-1];
                                }
                            }
                        }
                    }
                        break;
                    case keys.numbers.zero:
                    case keys.numbers.one:
                    case keys.numbers.two:
                    case keys.numbers.three:
                    case keys.numbers.four:
                    case keys.shiftNumbers.zero:
                    case keys.shiftNumbers.one:
                    case keys.shiftNumbers.two:
                    case keys.shiftNumbers.three:
                    case keys.shiftNumbers.four: {
                        if (scope.isCompetence(input)) {
                            // si pas en fin de ligne on passe à la cellule suivante
                            if((index +1) < children.length) {
                                moveTo = children[index + 1];

                                // sinon on passe à la ligne suivante
                            } else {
                                var tr = scope.findAncestor(td, 'navigable-row');
                                var pos = scope.findIndex(td, children);
                                var expandChildren = scope.findChildren(scope.findAncestor(tr, 'expandable-liste'), 'navigable-row');
                                var trIndex = scope.findIndex(tr, expandChildren);
                                moveTo = scope.canMove(expandChildren, trIndex);
                            }
                        }
                    }
                        break;
                    case keys.arrow.right:{
                        // si champs de type input
                        if (!scope.isCompetence(input)) {
                                // seulement si on est a la fin de l 'input text on passe au champ suivant
                                if (input.selectionStart === input.value.length) {

                                    // si pas en fin de ligne on passe à la cellule suivante
                                    if((index +1) < children.length) {
                                        moveTo = children[index + 1];

                                        // sinon on passe à la ligne suivante
                                    } else {
                                        var tr = scope.findAncestor(td, 'navigable-row');
                                        var pos = scope.findIndex(td, children);
                                        var expandChildren = scope.findChildren(scope.findAncestor(tr, 'expandable-liste'), 'navigable-row');
                                        var trIndex = scope.findIndex(tr, expandChildren);
                                        var moveToRow = expandChildren[trIndex + 1];
                                        if (moveToRow !== null) {
                                            var navigableCells = scope.findChildren(scope.findNavigableRow(moveToRow), 'navigable-cell');
                                            moveTo = navigableCells[0];
                                        }
                                    }
                                }
                        } else {
                            // si pas en fin de ligne on passe à la cellule suivante
                            if((index +1) < children.length) {
                                moveTo = children[index + 1];

                            // sinon on passe à la ligne suivante
                            } else {
                                    var tr = scope.findAncestor(td, 'navigable-row');
                                    var pos = scope.findIndex(td, children);
                                    var expandChildren = scope.findChildren(scope.findAncestor(tr, 'expandable-liste'), 'navigable-row');
                                    var trIndex = scope.findIndex(tr, expandChildren);
                                    var moveToRow = expandChildren[trIndex + 1];
                                    if (moveToRow !== null) {
                                        var navigableCells = scope.findChildren(scope.findNavigableRow(moveToRow), 'navigable-cell');
                                        moveTo = navigableCells[0];
                                    }
                                }
                        }
                    }
                        break;
                    case keys.arrow.up:
                    case keys.enter:
                    case keys.arrow.down: {
                        var tr = scope.findAncestor(td, 'navigable-row');
                        var pos = scope.findIndex(td, children);
                        var expandChildren = scope.findChildren(scope.findAncestor(tr, 'expandable-liste'),'navigable-row');
                        var trIndex = scope.findIndex(tr, expandChildren);
                        var moveToRow = null;
                        if (key === keys.arrow.down || key === keys.enter) {
                            if (trIndex < expandChildren.length - 1) {
                                let index = trIndex + 1;
                                moveToRow = expandChildren[index];
                                let findNavigableCell = scope.findChildren(scope.findNavigableRow(moveToRow), 'navigable-cell').length;
                                while (moveToRow !== null && findNavigableCell === 0 && index < expandChildren.length - 1) {
                                    index ++;
                                    moveToRow = expandChildren[index];
                                    findNavigableCell = scope.findChildren(scope.findNavigableRow(moveToRow), 'navigable-cell').length;
                                }
                            }
                        }
                        else if (key === keys.arrow.up) {
                            if (trIndex > 0) {
                                let index = trIndex - 1;
                                moveToRow = expandChildren[index];
                                let findNavigableCell = scope.findChildren(scope.findNavigableRow(moveToRow), 'navigable-cell').length;
                                while (moveToRow !== null && findNavigableCell === 0 && index !== 0 ) {
                                    index --;
                                    moveToRow = expandChildren[index];
                                    findNavigableCell = scope.findChildren(scope.findNavigableRow(moveToRow), 'navigable-cell').length;
                                }
                            }
                        }
                        if (moveToRow !== null) {
                            if (moveToRow.children.length) {
                                var targets = [];
                                var navigableCells = scope.findChildren(scope.findNavigableRow(moveToRow), 'navigable-cell');
                                for(var i = 0 ; i < navigableCells.length; i++){
                                    targets.push(scope.findInput(navigableCells[i]));
                                }

                                // s'il l'on essaie de remonter/descendre sur une ligne avec plus ou moins de cellules
                                // alors on effectue un traitement particulier
                                if(children.length  !== navigableCells.length) {
                                    // si la ligne n'a qu'un cellule on se positionne sur celle ci
                                    // if(navigableCells.length === 1) {
                                    //     moveTo = navigableCells[0];
                                    // } else {
                                    //     // sinon on se positionne sur la dernière si l'on remonte
                                    //     if (key == keys.arrow.up) {
                                    //         moveTo = navigableCells[navigableCells.length - 1];
                                    //     } else {
                                    //         // la première si l'on descends
                                    //         moveTo = navigableCells[0];
                                    //     }
                                    // }
                                    moveTo = navigableCells[0];
                                } else {
                                    moveTo = navigableCells[pos];
                                }

                            }
                        }
                    }
                        break;
                }
                if (moveTo) {
                    event.preventDefault();
                    if (scope.isCompetenceCell(moveTo)) {
                        input = scope.findCompetence(moveTo);
                    } else {
                        input = scope.findInput(moveTo);
                    }

                    input.classList.remove("display-none");
                    input.focus();
                    if (!scope.isCompetenceCell(moveTo)) {
                        input.select();
                    }
                }

            });
        }
    };
});