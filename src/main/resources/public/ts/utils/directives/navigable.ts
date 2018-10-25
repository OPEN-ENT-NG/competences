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
import { ng, $ } from 'entcore';

export let navigable = ng.directive('cNavigable', function(){
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
                var children = [];
                for(var i = 0 ; i < el.children.length; i++){
                    if(scope.hasClass(el.children[i], cls)){
                        children.push(el.children[i]);
                    }
                }
                return children;
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

            scope.findInput = function(row){
                for(var i = 0; i < row.children.length; i++){
                    if(row.children[i].tagName === 'INPUT'){
                        return row.children[i];
                    }
                }
                return null;
            };

            scope.findInputs = function(row){
                var inputs = [];
                for(var i = 0 ; i < row.children.length; i++){
                    if(row.children[i].tagName === 'INPUT'){
                        inputs.push(row.children[i]);
                    }
                }
                return inputs;
            };

            scope.findNavigableRow = function(row){
                return (scope.findChildren(row, 'navigable-inputs-row'))[0];
            };

            element.bind('keydown', function(event){
                var keys = {
                    enter : 13,
                    arrow : {left: 37, up: 38, right: 39, down: 40}
                };
                var key = event.which;

                if ($.inArray(key, [keys.arrow.left, keys.arrow.up, keys.arrow.right, keys.arrow.down, keys.enter]) < 0) { return; }
                var input = event.target;
                var td = scope.findAncestor(event.target, 'navigable-cell');
                var row = scope.findAncestor(td, 'navigable-inputs-row');
                var children = scope.findChildren(row, 'navigable-cell');
                var index = scope.findIndex(td, children);
                var moveTo = null;
                switch(key){
                    case keys.arrow.left:{
                        if (input.selectionStart === 0) {
                            if(index > 0){
                                moveTo = children[index-1];
                            }
                        }
                        break;
                    }
                    case keys.arrow.right:{
                        if (input.selectionEnd == input.value.length) {
                            if(index < row.children.length){
                                moveTo = children[index+1];
                            }
                        }
                        break;
                    }
                    case keys.arrow.up:
                    case keys.enter:
                    case keys.arrow.down:{
                        var tr = scope.findAncestor(td, 'navigable-row');
                        var pos = scope.findIndex(td, children);
                        var expandChildren = scope.findChildren(scope.findAncestor(tr, 'expandable-liste'),'navigable-row');
                        var trIndex = scope.findIndex(tr, expandChildren);
                        var moveToRow = null;
                        if (key == keys.arrow.down || key == keys.enter) {
                            if(trIndex < expandChildren.length -1){
                                moveToRow = expandChildren[trIndex+1];
                            }
                        }
                        else if (key == keys.arrow.up) {
                            if(trIndex > 0){
                                moveToRow = expandChildren[trIndex-1];
                            }
                        }
                        if(moveToRow !== null){
                            if (moveToRow.children.length) {
                                var targets = [];
                                var navigableCells = scope.findChildren(scope.findNavigableRow(moveToRow), 'navigable-cell');
                                for(var i = 0 ; i < navigableCells.length; i++){
                                    targets.push(scope.findInput(navigableCells[i]));
                                }
                                moveTo = navigableCells[pos];
                            }
                        }
                        break;
                    }
                    /*case keys.enter:{
                     var focusedElement = $(input);
                     var nextElement = focusedElement.parent().next();
                     if (nextElement.find('input').length>0){
                     nextElement.find('input').focus();
                     }else{
                     nextElement = nextElement.parent().next().find('input').first();
                     nextElement.focus();
                     }
                     break;
                     }*/
                }
                if (moveTo) {
                    event.preventDefault();
                    input = scope.findInput(moveTo);
                    input.focus();
                    input.select();
                }

            });
        }
    };
});