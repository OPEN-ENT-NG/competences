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
 * Created by agnes.lapeyronnie on 15/09/2017.
 */
import {ng, _, Collection, model} from "entcore";
import {Classe, LSU, Periode} from '../models/teacher';
import {Responsable} from '../models/teacher/Responsable';
export let exportControleur = ng.controller('ExportController',['$scope',
    function($scope) {


      $scope.lsu = new LSU($scope.structure.id, $scope.evaluations.classes.where({type_groupe : Classe.type.CLASSE}),
          $scope.structure.responsables.all);
        $scope.allClasses = $scope.evaluations.classes.where({type_groupe: Classe.type.CLASSE});


        function initparams(type) {
            $scope.inProgress = false;
            $scope.params = {
                type: type,
                idStructure: $scope.structure.id,
                classes: [],
                responsables: [],
                periodes_type: [],
                stsFile: null
            };
        }
        initparams("1");

        $scope.changeType = (type: String): void => {
            initparams(type);
        };

        $scope.dropComboModel = (el: any, table: any): void => {
            table = _.without(table, el);
        };

        $scope.toggleperiode = function toggleperiode(periode_type) {
            let idx = $scope.params.periodes_type.indexOf(periode_type);
            if (idx > -1) {
                $scope.params.periodes_type.splice(idx, 1);
            }
            else {
                $scope.params.periodes_type.push(periode_type);
            }
        };


        // Créer une fonction dans le $scope qui lance la récupération des responsables
        $scope.getResponsables = function () {
            $scope.structure.responsables.sync().then(() => {
               $scope.lsu.responsable = $scope.structure.responsables.all[0].displayName
            });
        };
        $scope.getResponsables();


        /**
         * Controle la validité des selections avant l'exportLSU
         */
        $scope.controleExportLSU = function(){
            return !(
                $scope.params.classes.length > 0 &&
                $scope.params.responsables.length > 0
            );
        };
        $scope.exportLSU = ()=> {
            $scope.inProgress = true;
            $scope.lsu.export($scope.params).then((res) => {
                console.log("$scope.lsu.export res");
                console.log(res);

                let blob = new Blob([res.data]);
                let link = document.createElement('a');
                link.href = window.URL.createObjectURL(blob);
                link.download = res.headers['content-disposition'].split('filename=')[1];
                document.body.appendChild(link);
                link.click();

                initparams(1);

            }).catch((error) => {
                console.log("$scope.lsu.export error");
                $scope.errorResponse = error.response.statusText;
                initparams(1);
            });

        };

    }
]);




// Si 1 structure =>  Initialiser lsu.structureId à l'id de la structure
// if($scope.evaluations.structures.all.length == 1){
//$scope.lsu.idStructure = $scope.structure.id;
// }