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

import { http } from 'entcore';

export const visibilitymoyBFC = {
    title: 'Moyenne BFC',
    description: 'Active la visibilité de la moyenne calculée sur le BFC ',
    controller: {
        init: function () {
            this.idStructure = this.source.idStructure;
            //id_visibility = 1 si moyBFC
           this.idVisibility = 1;

            http().get(`/competences/bfc/visibility/structures/${this.idStructure}/${this.idVisibility}`)
                .done(function (res) {
                    this.visible = res[0].visible;
                    console.log('load sniplet averageBFC');
                    this.$apply('visible');
                }.bind(this));
        },
        initSource: function () {
        },
        save: function (visibility:number) {
            // visibility values
            // 0 : caché pour tout le monde
            // 1 : caché pour les enseignants
            // 2 : visible pour tous

            http().putJson(`/competences/bfc/visibility/structures/${this.idStructure}/${this.idVisibility}/${visibility}`)
                .done(function () {
                    this.visible = visibility;
                    this.$apply('visible');
                    console.log('visibility set');
                }.bind(this))
                .error(function () {
                }.bind(this));
        }
    }
}