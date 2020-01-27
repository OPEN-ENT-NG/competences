/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (veang-model="batonRadar" phMatierersion 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

import {ng, $ as jquery, idiom as lang} from 'entcore';

declare let require: any;
let anime = require ('animejs');

export let structureLoader = ng.directive('structureLoader', function() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            structureName: '=',
            display: '='
        },
        link: ($scope) => {
            $scope.initStructureLoader = false;
            jquery('#structure-loader .ml2 > span').each( function(){
                jquery(this).html(jquery(this).text().replace(/([^\x00-\x80]|\w|\.)/g,
                    "<span class='letter'>$&</span>"));
            });
            let animation = anime.timeline({loop: true})
                .add({
                    targets: '#structure-loader .ml2 .letter',
                    scale: [4,1],
                    opacity: [0,1],
                    translateZ: 0,
                    easing: "easeOutExpo",
                    duration: 950,
                    delay: function(el, i) {
                        return 70*i;
                    }
                }).add({
                targets: '#structure-loader .ml2',
                opacity: 0,
                duration: 1000,
                easing: "easeOutExpo",
                delay: 1000
            });
            $scope.$watch('display', function (newValue, oldValue) {
                if (newValue !== oldValue) {
                    if(newValue === true){
                        animation.restart();
                    }
                    else {
                        animation.pause();
                    }
                }
            });
        },
        template: '<div id="structure-loader" class="overlay" ng-show="display">' +
        '<div>' +
        '<h4 class="content-loader" >' +
            '<i18n>viescolaire.structure.load</i18n>' +
            '<span ng-bind="structureName"></span>' +
            '<i18n>viescolaire.structure.load.end</i18n>' +
            '<i18n class="ml2">viescolaire.tree.dot</i18n>' +
        '</h4>' +
        '</div>' +
        '</div>',
        replace: true
    };
});