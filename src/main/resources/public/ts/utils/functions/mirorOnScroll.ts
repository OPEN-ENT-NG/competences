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


