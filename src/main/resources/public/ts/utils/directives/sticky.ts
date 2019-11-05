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

import { ng, angular } from 'entcore';
import * as utils from '../../utils/teacher';

export let sticky = ng.directive('sticky', ['$window', '$timeout', function($window, $timeout) {
        return {
            restrict: 'A', // this directive can only be used as an attribute.
            scope: false,
            link: function linkFn($scope, $elem, $attrs) {

                // Initial scope
                let scrollableNodeTagName = 'sticky-scroll';
                let initialPosition = $elem.css('position');
                let initialStyle = $elem.attr('style') || '';
                let stickyBottomLine = 0;
                let isSticking = false;
                let onStickyHeighUnbind;
                let originalInitialCSS;
                let originalOffset;
                let placeholder;
                let stickyLine;
                let initialCSS;

                // Optional Classes
                let stickyClass = $attrs.stickyClass || '';
                let unstickyClass = $attrs.unstickyClass || '';
                let bodyClass = $attrs.bodyClass || '';
                let bottomClass = $attrs.bottomClass || '';

                // Find scrollbar
                let scrollbar = deriveScrollingViewport ($elem);

                // Define elements
                let windowElement = angular.element($window);
                let scrollbarElement = angular.element(scrollbar);
                let $body = angular.element(document.body);

                // Resize callback
                let $onResize = function () {
                    if ($scope.$root && !$scope.$root.$$phase) {
                        utils.safeApply($scope);
                    } else {
                        onResize();
                    }
                };

                angular.element($elem).ready(() => {
                    utils.safeApply($scope);
                });

                // Define options
                let usePlaceholder = ($attrs.usePlaceholder !== 'false');
                let anchor = $attrs.anchor === 'bottom' ? 'bottom' : 'top';
                let confine = ($attrs.confine === 'true');
                $scope.disabled = ($attrs.disabled === 'true');

                // flag: can react to recalculating the initial CSS dimensions later
                // as link executes prematurely. defaults to immediate checking
                let isStickyLayoutDeferred = $attrs.isStickyLayoutDeferred !== undefined
                    ? ($attrs.isStickyLayoutDeferred === 'true')
                    : false;

                // flag: is sticky content constantly observed for changes.
                // Should be true if content uses ngBind to show text
                // that may lety in size over time
                let isStickyLayoutWatched = $attrs.isStickyLayoutWatched !== undefined
                    ? ($attrs.isStickyLayoutWatched === 'true')
                    : true;


                let offset = $attrs.offset
                    ? parseInt ($attrs.offset.replace(/px;?/, ''))
                    : 0;

                /**
                 * Trigger to initialize the sticky
                 * Because of the `timeout()` method for the call of
                 * @type {Boolean}
                 */
                let shouldInitialize = true;

                /**
                 * Initialize Sticky
                 */
                function initSticky() {

                        // Listeners
                        scrollbarElement.on('scroll', checkIfShouldStick);
                        windowElement.on('resize', $onResize);

                        memorizeDimensions(); // remember sticky's layout dimensions
                        setTimeout(() => {
                           memorizeDimensions();
                        }, 1000);

                        checkIfShouldStick();

                        // Setup watcher on digest and change
                        $scope.$watch(onDigest, onChange);

                        // Clean up
                        $scope.$on('$destroy', onDestroy);
                        shouldInitialize = false;
                };

                /**
                 * need to recall sticky's DOM attributes (make sure layout has occured)
                 */
                function memorizeDimensions() {
                    // immediate assignment, but there is the potential for wrong values if content not ready
                    initialCSS = $scope.getInitialDimensions();

                    // option to calculate the dimensions when layout is 'ready'
                    if (isStickyLayoutDeferred) {

                        // logic: when this directive link() runs before the content has had a chance to layout on browser, height could be 0
                        if (!$elem[0].getBoundingClientRect().height) {

                            onStickyHeighUnbind = $scope.$watch(
                                function() {
                                    return $elem.height();
                                },

                                // state change: sticky content's height set
                                function onStickyContentLayoutInitialHeightSet(newValue, oldValue) {
                                    if (newValue > 0) {
                                        // now can memorize
                                        initialCSS = $scope.getInitialDimensions();

                                        if (!isStickyLayoutWatched) {
                                            // preference was to do just a one-time async watch on the sticky's content; now stop watching
                                            onStickyHeighUnbind();
                                        }
                                    }
                                }
                            );
                        }
                    }
                }

                /**
                 * Determine if the element should be sticking or not.
                 */
                let checkIfShouldStick = function() {
                    if ($scope.disabled === true || mediaQueryMatches()) {
                        if (isSticking) unStickElement();
                        return false;
                    }

                    // What's the document client top for?
                    let scrollbarPosition = scrollbarYPos();
                    let shouldStick;

                    if (anchor === 'top') {
                        if (confine === true) {
                            shouldStick = scrollbarPosition > stickyLine && scrollbarPosition <= stickyBottomLine;
                        } else {
                            shouldStick = scrollbarPosition > stickyLine;
                        }
                    } else {
                        shouldStick = scrollbarPosition <= stickyLine;
                    }

                    // Switch the sticky mode if the element crosses the sticky line
                    // $attrs.stickLimit - when it's equal to true it enables the user
                    // to turn off the sticky function when the elem height is
                    // bigger then the viewport
                    let closestLine = getClosest (scrollbarPosition, stickyLine, stickyBottomLine);

                    if (shouldStick && !shouldStickWithLimit ($attrs.stickLimit) && !isSticking) {
                        stickElement (closestLine);
                    } else if (!shouldStick && isSticking) {
                        unStickElement(closestLine, scrollbarPosition);
                    } else if (confine && !shouldStick) {
                        // If we are confined to the parent, refresh, and past the stickyBottomLine
                        // We should 'remember' the original offset and unstick the element which places it at the stickyBottomLine
                        originalOffset = elementsOffsetFromTop ($elem[0]);
                        unStickElement (closestLine, scrollbarPosition);
                    }
                };

                /**
                 * determine the respective node that handles scrolling, defaulting to browser window
                 */
                function deriveScrollingViewport(stickyNode) {
                    // derive relevant scrolling by ascending the DOM tree
                    let match =findAncestorTag (scrollableNodeTagName, stickyNode);
                    return (match.length === 1) ? match[0] : $window;
                }

                /**
                 * since jqLite lacks closest(), this is a pseudo emulator (by tag name)
                 */
                function findAncestorTag(tag, context) {
                    let m = []; // nodelist container
                    let n = context.parent(); // starting point
                    let p;

                    do {
                        let node = n[0]; // break out of jqLite
                        // limit DOM territory
                        if (node.nodeType !== 1) {
                            break;
                        }

                        // success
                        if (node.tagName.toUpperCase() === tag.toUpperCase()) {
                            return n;
                        }

                        p = n.parent();
                        n = p; // set to parent
                    } while (p.length !== 0);

                    return m; // empty set
                }

                /**
                 * Seems to be undocumented functionality
                 */
                function shouldStickWithLimit(shouldApplyWithLimit) {
                    return shouldApplyWithLimit === 'true'
                        ? ($window.innerHeight - ($elem[0].offsetHeight + parseInt(offset.toString())) < 0)
                        : false;
                }

                /**
                 * Finds the closest value from a set of numbers in an array.
                 */
                function getClosest(scrollTop, stickyLine, stickyBottomLine) {
                    let closest = 'top';
                    let topDistance = Math.abs(scrollTop - stickyLine);
                    let bottomDistance = Math.abs(scrollTop - stickyBottomLine);

                    if (topDistance > bottomDistance) {
                        closest = 'bottom';
                    }

                    return closest;
                }

                /**
                 * Unsticks the element
                 */
                function unStickElement(fromDirection?, scrollbarPosition?) {
                    if (initialStyle) {
                        $elem.attr('style', initialStyle);
                    }
                    isSticking = false;

                    initialCSS.width = $scope.getInitialDimensions().width;

                    $body.removeClass(bodyClass);
                    $elem.removeClass(stickyClass);
                    $elem.addClass(unstickyClass);

                    if (fromDirection === 'top') {
                        $elem.removeClass(bottomClass);

                        $elem
                            .css('z-index', 10)
                            .css('width', initialCSS.width)
                            .css('top', initialCSS.top)
                            .css('position', initialCSS.position)
                            .css('left', initialCSS.cssLeft)
                            .css('margin-top', initialCSS.marginTop)
                            .css('height', initialCSS.height);
                    } else if (fromDirection === 'bottom' && confine === true) {
                        $elem.addClass(bottomClass);

                        // It's possible to page down page and skip the 'stickElement'.
                        // In that case we should create a placeholder so the offsets don't get off.
                                                createPlaceholder();

                        $elem
                            .css('z-index', 10)
                            .css('width', initialCSS.width)
                            .css('top', '')
                            .css('bottom', 0)
                            .css('position', 'absolute')
                            .css('left', initialCSS.cssLeft)
                            .css('margin-top', initialCSS.marginTop)
                            .css('margin-bottom', initialCSS.marginBottom)
                            .css('height', initialCSS.height);
                    }

                    if (placeholder && fromDirection === anchor) {
                        placeholder.remove();
                    }
                }

                /**
                 * Sticks the element
                 */
                function stickElement(closestLine) {
                    // Set sticky state
                    isSticking = true;
                    $timeout(function() {
                        initialCSS.offsetWidth = $elem[0].offsetWidth;
                    }, 0);
                    $body.addClass(bodyClass);
                    $elem.removeClass(unstickyClass);
                    $elem.removeClass(bottomClass);
                    $elem.addClass(stickyClass);

                    createPlaceholder();

                    $elem
                        .css('z-index', '10')
                        .css('width', $elem[0].offsetWidth + 'px')
                        .css('position', 'fixed')
                        .css('left', $elem.css('left').replace('px', '') + 'px')
                        .css(anchor, (offset + elementsOffsetFromTop (scrollbar)) + 'px')
                        .css('margin-top', 0);

                    if (anchor === 'bottom') {
                        $elem.css('margin-bottom', 0);
                    }
                }

                /**
                 * Clean up directive
                 */
                let onDestroy = function() {
                    scrollbarElement.off('scroll', checkIfShouldStick);
                    windowElement.off('resize', $onResize);

                    $onResize = null;

                    $body.removeClass(bodyClass);

                    if (placeholder) {
                        placeholder.remove();
                    }
                };

                /**
                 * Updates on resize.
                 */
                function onResize() {
                    unStickElement (anchor);
                    checkIfShouldStick();
                }

                /**
                 * Triggered on load / digest cycle
                 * return `0` if the DOM element is hidden
                 */
                let onDigest = function() {
                    if ($scope.disabled === true) {
                        return unStickElement();
                    }
                    let offsetFromTop = elementsOffsetFromTop ($elem[0]);
                    if (offsetFromTop === 0) {
                        return offsetFromTop;
                    }
                    if (anchor === 'top') {
                        return (originalOffset || offsetFromTop) - elementsOffsetFromTop (scrollbar) + scrollbarYPos();
                    } else {
                        return offsetFromTop - scrollbarHeight() + $elem[0].offsetHeight + scrollbarYPos();
                    }
                };

                /**
                 * Triggered on change
                 */
                let onChange = function (newVal, oldVal) {

                    /**
                     * Indicate if the DOM element is showed, or not
                     * @type {boolean}
                     */
                    let elemIsShowed = !!newVal;

                    /**
                     * Indicate if the DOM element was showed, or not
                     * @type {boolean}
                     */
                    let elemWasHidden = !oldVal;
                    let valChange = (newVal !== oldVal || typeof stickyLine === 'undefined');
                    let notSticking = (!isSticking && !isBottomedOut());

                    if (valChange && notSticking && newVal > 0 && elemIsShowed) {
                        stickyLine = newVal - offset;
                        //Update dimensions of sticky element when is showed
                        if (elemIsShowed && elemWasHidden) {
                            $scope.updateStickyContentUpdateDimensions($elem[0].offsetWidth, $elem[0].offsetHeight);
                        }
                        // IF the sticky is confined, we want to make sure the parent is relatively positioned,
                        // otherwise it won't bottom out properly
                        if (confine) {
                            $elem.parent().css({
                                'position': 'relative'
                            });
                        }

                        // Get Parent height, so we know when to bottom out for confined stickies
                        let parent = $elem.parent()[0];

                        // Offset parent height by the elements height, if we're not using a placeholder
                        let parentHeight = parseInt (parent.offsetHeight) - (usePlaceholder ? 0 : $elem[0].offsetHeight);

                        // and now lets ensure we adhere to the bottom margins
                        // TODO: make this an attribute? Maybe like ignore-margin?
                        let marginBottom = parseInt ($elem.css('margin-bottom').replace(/px;?/, '')) || 0;

                        // specify the bottom out line for the sticky to unstick
                        let elementsDistanceFromTop = elementsOffsetFromTop ($elem[0]);
                        let parentsDistanceFromTop = elementsOffsetFromTop (parent)
                        let scrollbarDistanceFromTop = elementsOffsetFromTop (scrollbar);

                        let elementsDistanceFromScrollbarStart = elementsDistanceFromTop - scrollbarDistanceFromTop;
                        let elementsDistanceFromBottom = parentsDistanceFromTop + parentHeight - elementsDistanceFromTop;

                        stickyBottomLine = elementsDistanceFromScrollbarStart
                            + elementsDistanceFromBottom
                            - $elem[0].offsetHeight
                            - marginBottom
                            - offset
                            + +scrollbarYPos();

                        checkIfShouldStick();
                    }
                };

                /**
                 * Helper Functions
                 */

                /**
                 * Create a placeholder
                 */
                function createPlaceholder() {
                    if (usePlaceholder) {
                        // Remove the previous placeholder
                        if (placeholder) {
                            placeholder.remove();
                        }

                        placeholder = angular.element('<div>');
                        let elementsHeight = $elem[0].offsetHeight;
                        let computedStyle = $elem[0].currentStyle || window.getComputedStyle($elem[0]);
                        elementsHeight += parseInt(computedStyle.marginTop, 10);
                        elementsHeight += parseInt(computedStyle.marginBottom, 10);
                        elementsHeight += parseInt(computedStyle.borderTopWidth, 10);
                        elementsHeight += parseInt(computedStyle.borderBottomWidth, 10);
                        placeholder.css('height', $elem[0].offsetHeight + 'px');

                        $elem.after(placeholder);
                    }
                }

                /**
                 * Are we bottomed out of the parent element?
                 */
                function isBottomedOut() {
                    if (confine && scrollbarYPos() > stickyBottomLine) {
                        return true;
                    }

                    return false;
                }

                /**
                 * Fetch top offset of element
                 */
                function elementsOffsetFromTop(element) {
                    let offset = 0;

                    if (element.getBoundingClientRect) {
                        offset = element.getBoundingClientRect().top;
                    }

                    return offset;
                }

                /**
                 * Retrieves top scroll distance
                 */
                function scrollbarYPos() {
                    let position;

                    if (typeof scrollbar.scrollTop !== 'undefined') {
                        position = scrollbar.scrollTop;
                    } else if (typeof scrollbar.pageYOffset !== 'undefined') {
                        position = scrollbar.pageYOffset;
                    } else {
                        position = document.documentElement.scrollTop;
                    }

                    return position;
                }

                /**
                 * Determine scrollbar's height
                 */
                function scrollbarHeight() {
                    let height;

                    if (scrollbarElement[0] instanceof HTMLElement) {
                        // isn't bounding client rect cleaner than insane regex mess?
                        height = $window.getComputedStyle(scrollbarElement[0], null)
                            .getPropertyValue('height')
                            .replace(/px;?/, '');
                    } else {
                        height = $window.innerHeight;
                    }

                    return parseInt (height) || 0;
                }

                /**
                 * Checks if the media matches
                 */
                function mediaQueryMatches() {
                    let mediaQuery = $attrs.mediaQuery || false;
                    let matchMedia = $window.matchMedia;

                    return mediaQuery && !(matchMedia ('(' + mediaQuery + ')').matches || matchMedia (mediaQuery).matches);
                }

                /**
                 * Get more accurate CSS values
                 */
                function getCSS($el, prop){
                    let el = $el[0],
                        computed = window.getComputedStyle(el),
                        prevDisplay = computed.display,
                        val;

                    // hide the element so that we can get original css
                    // values instead of computed values
                    el.style.display = "none";

                    // NOTE - computed style declaration object is a reference
                    // to the element's CSSStyleDeclaration, so it will always
                    // reflect the current style of the element
                    val = computed[prop];

                    // restore previous display value
                    el.style.display = prevDisplay;

                    return val;
                }

                // public accessors for the controller to hitch into. Helps with external API access
                $scope.getElement = function() { return $elem; };
                $scope.getScrollbar = function() { return scrollbar; };
                $scope.getInitialCSS = function() { return initialCSS; };
                $scope.getAnchor = function() { return anchor; };
                $scope.isSticking = function() { return isSticking; };
                $scope.getOriginalInitialCSS = function() { return originalInitialCSS; };
                // pass through aliases
                $scope.processUnStickElement = function(anchor) { unStickElement(anchor)};
                $scope.processCheckIfShouldStick =function() { checkIfShouldStick(); };

                /**
                 * set the dimensions for the defaults of the content block occupied by the sticky element
                 */
                $scope.getInitialDimensions = function() {
                    return {
                        zIndex: $elem.css('z-index'),
                        top: $elem.css('top'),
                        position: initialPosition, // revert to true initial state
                        marginTop: $elem.css('margin-top'),
                        marginBottom: $elem.css('margin-bottom'),
                        cssLeft: getCSS($elem, 'left'),
                        width: $elem.css('width'),
                        height: $elem.css('height')
                    };
                };

                /**
                 * only change content box dimensions
                 */
                $scope.updateStickyContentUpdateDimensions = function(width, height) {
                    if (width && height) {
                        initSticky();
                        initialCSS.width = width + 'px';
                        initialCSS.height = height + 'px';
                    }
                };

                // ----------- configuration -----------

                $timeout(function() {
                    originalInitialCSS = $scope.getInitialDimensions(); // preserve a copy
                    // Init the directive
                    initSticky();
                },0);

            },

            /**
             * +++++++++ public APIs+++++++++++++
             */
            controller: ['$scope', '$window', function($scope, $window) {

                /**
                 * integration method allows for an outside client to reset the pinned state back to unpinned.
                 * Useful for when refreshing the scrollable DIV content completely
                 * if newWidth and newHeight integer values are not supplied then function will make a best guess
                 */
                this.resetLayout = function(newWidth, newHeight) {

                    let scrollbar = $scope.getScrollbar(),
                        initialCSS = $scope.getInitialCSS(),
                        anchor = $scope.getAnchor();

                    function _resetScrollPosition() {

                        // reset means content is scrolled to anchor position
                        if (anchor === 'top') {
                            // window based scroller
                            if (scrollbar === $window) {
                                $window.scrollTo(0, 0);
                                // DIV based sticky scroller
                            } else {
                                if (scrollbar.scrollTop > 0) {
                                    scrollbar.scrollTop = 0;
                                }
                            }
                        }
                        // todo: need bottom use case
                    }

                    // only if pinned, force unpinning, otherwise height is inadvertently reset to 0
                    if ($scope.isSticking()) {
                        $scope.processUnStickElement (anchor);
                        $scope.processCheckIfShouldStick();
                    }
                    // remove layout-affecting attribures that were modified by this sticky
                    $scope.getElement().css({ 'width': '', 'height': '', 'position': '', 'top': '', zIndex: '' });
                    // model resets
                    initialCSS.position = $scope.getOriginalInitialCSS().position; // revert to original state
                    delete initialCSS.offsetWidth; // stickElement affected

                    // use this directive element's as default, if no measurements passed in
                    if (newWidth === undefined && newHeight === undefined) {
                        let e_bcr = $scope.getElement()[0].getBoundingClientRect();
                        newWidth = e_bcr.width;
                        newHeight = e_bcr.height;
                    }

                    // update model with new dimensions (if supplied from client's own measurement)
                    $scope.updateStickyContentUpdateDimensions(newWidth, newHeight); // update layout dimensions only

                    _resetScrollPosition();
                };

                /**
                 * return a reference to the scrolling element (window or DIV with overflow)
                 */
                this.getScrollbar = function() {
                    return $scope.getScrollbar();
                };
            }]
        };
    }]
);