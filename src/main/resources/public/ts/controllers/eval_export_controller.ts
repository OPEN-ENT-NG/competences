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
import{ ng, _ } from "entcore";
import {Classe, LSU} from '../models/teacher';
export let exportControleur = ng.controller('ExportController',['$scope',
    function($scope) {

      $scope.lsu = new LSU($scope.structure.id, $scope.evaluations.classes.where({type_groupe : Classe.type.CLASSE}),
          $scope.structure.responsables);

      //Vue export du fichier xml

      //initialisation
      $scope.bSelectAllClasses = false;
      $scope.bSelectAllResponsables = false;

      $scope.pOFilterCtrl={
          bOFClasse : false,
          bOFResponsable : false
      };

      //function toutes les classes selectionnées ont leur attribut selected mis à la valeur  bSelectAllClasses
      $scope.switchAllClasses = function(oListe) {
            oListe.forEach(function(o) {
                o.selected = $scope.bSelectAllClasses;
            });
      };

        //selectionner seulement les classes sans les groupes
        /*$scope.criteriaMatch = () => {
            return function(classe) {
                return classe.type_groupe === 0;
            };
        };*/

        // Créer une fonction dans le $scope qui lance la récupération des responsables
        $scope.getResponsables = function () {
            $scope.structure.responsables.sync().then(() => {
                // On a fini la synchronisation
               $scope.lsu.responsable = $scope.structure.responsables.all[0].displayName
            });
        };

        $scope.getResponsables();

        $scope.switchAllResponsables = function(oListe) {
            oListe.each(function(o) {
                o.selected = $scope.bSelectAllResponsables;
            });
        };

        /**
         * Controle la validité des selections avant l'exportLSU
         */
        $scope.controleExportLSU = function(){
            return !(
                $scope.lsu.classes.filter(classe => classe.selected === true).length > 0 &&
                _.where($scope.lsu.responsables.all, {selected: true}).length > 0
            );
        };
        $scope.exportLSU = ()=> {
          $scope.lsu.export();


        };

    }
]);




// Si 1 structure =>  Initialiser lsu.structureId à l'id de la structure
// if($scope.evaluations.structures.all.length == 1){
//$scope.lsu.idStructure = $scope.structure.id;
// }