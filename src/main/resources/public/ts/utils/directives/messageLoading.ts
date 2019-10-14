import { ng, $ as jquery } from 'entcore';
declare let require : any;
let anime = require ('animejs');
export let messageLoaderLink = ($scope) => {
    // Wrap every letter in a span
    jquery('#message-loader .ml2 > span').each( function(){
        jquery(this).html(jquery(this).text().replace(/([^\x00-\x80]|\w|\.)/g, "<span class='letter'>$&</span>"));
    });

    let animation = anime.timeline({loop: true})
        .add({
            targets: '#message-loader .ml2 .letter',
            scale: [4,1],
            opacity: [0,1],
            translateZ: 0,
            easing: "easeOutExpo",
            duration: 950,
            delay: function(el, i) {
                return 70*i;
            }
        }).add({
            targets: '#message-loader .ml2',
            opacity: 0,
            duration: 1000,
            easing: "easeOutExpo",
            delay: 1000
        });
    $scope.$watch('display', function (newValue, oldValue) {
        if (newValue !== oldValue) {
            if(newValue === true){
                jquery('#message-loader').css('z-index', 100);
                animation.restart();
            }
            else {
                animation.pause();
            }
        }
    });
};
export let messageLoader = ng.directive('messageLoader', function() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            display: '='
        },
        link: messageLoaderLink,
        template: '<div id="message-loader" class="overlay" ng-show="display">' +
        '<div>' +
        '<h4 class="content-loader"> ' +
        '<i18n class="ml2">evaluations.loading</i18n>' +
        '</h4>' +
        '</div>' +
        '</div>',
        replace: true
    };
});


