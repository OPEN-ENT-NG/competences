import {model, notify, idiom as lang, ng, template, moment, _, angular, http} from 'entcore';
import {
    evaluations,
    Classe,
    Structure,
} from '../models/teacher';
import * as utils from '../utils/teacher';
import {Utils} from "../models/teacher/Utils";

declare let $: any;
declare let document: any;
declare let window: any;
declare let console: any;
declare let location: any;

export let evalBilanPeriodiqueCtl = ng.controller('BilanPeriodiqueController', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$sce', '$compile', '$timeout', '$route',
    async function ($scope, route, $rootScope, $location, $filter, $sce, $compile, $timeout, $route) {



    }

    ]);

