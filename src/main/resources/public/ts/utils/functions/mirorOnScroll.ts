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

import { angular, $ } from 'entcore';

declare let document: any;
declare let setTimeout: any;
declare let window: any;
declare let requestAnimationFrame: any;

export function mirorOnScroll (element, mirorElement) {
    angular.element(document).ready(function () {

        let animation = function() {
            element.addClass('scrolling');
            if (mirorElement != undefined && mirorElement.offset() != undefined) {
                element.offset({
                    top: mirorElement.offset().top
                });
            }
            else {
                //$(window).scrollTop(0);
            }
            requestAnimationFrame(animation);
        };

        let scrolls = false;
        $(window).scroll(function() {
            if (!scrolls) {
                animation();
            }
            scrolls = true;
        })

    });
}


