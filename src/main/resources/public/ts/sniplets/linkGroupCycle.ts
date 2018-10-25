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

import {_, http, notify} from 'entcore';
import {Classe} from "../models/teacher";
import * as utils from "../utils/teacher";

export const linkGroupCycle = {
    title: 'Lien Group Cycle',
    description: 'Permet de rattacher des classes ou des groupes à un cycle',
    that: undefined,
    controller: {
        init: function () {
            this.idStructure = this.source.idStructure;
            this.groupLibelle = [];
            this.search = {
                name : "",
                type :{ all : true},
                id_cycle : undefined,
                hasChange : false,
                classeAll : false
            };
            this.libelle = Classe.libelle;
            _.forEach(_.values(this.libelle),  (lib) => {
                this.search.type[this.groupLibelle.length] = true;
                this.groupLibelle.push({name: lib, type : this.groupLibelle.length, isSelected: true});
            });
            this.cycles = this.source.cycles;
            this.$parent.filterLinkSearch = this.filterLinkSearch;
            this.selectedClasses = [];
            this.opened.lightboxLinkGroupCycle = false;
            this.selectedRadio = {id_cycle: undefined};
            this.dataOnClasses = [];
            linkGroupCycle.that = this;
            this.getClasses();
        },
        initSource: function () {
        },
        getClasses: function () {
            http().get(`/viescolaire/classes?idEtablissement=${this.idStructure}`)
                .done(function (res) {
                    this.classesGroupes = {
                        all: this.castClasses(res)
                    };
                    console.log('load sniplet link group cycle');
                    this.$apply('classesGroupes');
                }.bind(this));
        },
        checkDataOnClasses: function(classes){
            let jsonToSave = {
                idClasses: _.pluck(_.filter(classes, function (classe) {
                    return classe.id_cycle !== parseInt(linkGroupCycle.that.selectedRadio.id_cycle);
                }), 'id'),
                id_cycle: parseInt(linkGroupCycle.that.selectedRadio.id_cycle),
            };
            if (jsonToSave.idClasses.length > 0 && !isNaN(jsonToSave.id_cycle)) {
                http().postJson(`competences/link/check/data/classes`, jsonToSave)
                    .done((res) => {
                        if (res.length > 0) {
                            _.forEach(res, function (devoir) {
                                devoir.classe = _.findWhere(linkGroupCycle.that.classesGroupes.all,
                                    {id: devoir.id_groupe}).name;
                            });
                            linkGroupCycle.that.dataOnClasses = res;
                            linkGroupCycle.that.opened.lightboxDataOnDevoirs = true;
                        }
                        else {
                            linkGroupCycle.that.dataOnClasses = [];
                            linkGroupCycle.that.opened.lightboxDataOnDevoirs = false;
                            linkGroupCycle.that.save(classes);
                        }
                        linkGroupCycle.that.opened.lightboxLinkGroupCycle = false;
                        utils.safeApply(this);
                    })
                    .error(function (res) {
                        if (res.status === 401) {
                            notify.error('evaluation.error.unautorize');
                            utils.safeApply(this);
                        } else {
                            notify.error('evaluation.link.error.request');
                        }
                        utils.safeApply(this);
                    }).bind(this);
            }
            else {

                if (isNaN(jsonToSave.id_cycle)) {
                    notify.error("evaluation.link.no.cycle");
                }
                else {
                    // Si aucune classe ne change de cycle, on les sélectionne juste et
                    // on ferme la lightbox
                    _.forEach(classes, (classe) => {
                        classe.selected = false;
                    });
                    linkGroupCycle.that.opened.lightboxLinkGroupCycle = false;
                    linkGroupCycle.that.opened.lightboxDataOnDevoirs = false;
                }
                utils.safeApply(this);
            }
        },
        save: function (classes) {
            let classesToUpdate = _.filter(classes, function (classe) {
                return classe.id_cycle !== parseInt(linkGroupCycle.that.selectedRadio.id_cycle);
            });
            let jsonToSave = {
                idClasses: _.pluck(classesToUpdate, 'id'),
                id_cycle: parseInt(linkGroupCycle.that.selectedRadio.id_cycle),
                typesGroupes: _.pluck(classesToUpdate, 'type_groupe')
            };
            http().putJson(`competences/link/groupes/cycles`, jsonToSave)
                .done(() => {
                    notify.info('evaluation.link.success');
                    _.forEach(classes, (classe) => {
                        classe.selected = false;
                        classe.id_cycle = jsonToSave.id_cycle;
                    });
                    linkGroupCycle.that.opened.lightboxLinkGroupCycle = false;
                    linkGroupCycle.that.opened.lightboxDataOnDevoirs = false;
                    utils.safeApply(this);
                })
                .error(function (res) {
                    if (res.status === 401) {
                        notify.error('evaluation.error.unautorize');
                        utils.safeApply(this);
                    }else {
                        notify.error('evaluation.link.error.request');
                    }
                    utils.safeApply(this);
                }).bind(this);
        },
       castClasses: (classes) => {
            return _.map(classes, (classe) => {
                classe.type_groupe_libelle = Classe.get_type_groupe_libelle(classe);
                classe = new Classe(classe);
                return classe;
            });
        },
        getClasseCycle: (id_cycle) => {
            let _cycle = _.findWhere(linkGroupCycle.that.cycles, {id_cycle: parseInt(id_cycle)});
            return (_cycle !== undefined) ?  _cycle.libelle : '';
        },
        selectedClasse: (classe) => {
            classe.selected = ! classe.selected;

        },
        selectType: (type,search) => {
            type.isSelected = !type.isSelected;
            search.type[type.type] = !search.type[type.type];
        },
        selectAllType: () => {
            _.forEach(_.keys(linkGroupCycle.that.search.type), function (key) {
                linkGroupCycle.that.search.type[key]= !linkGroupCycle.that.search.type.all;
            });
            _.forEach(linkGroupCycle.that.groupLibelle, function (type) {
                type.isSelected = linkGroupCycle.that.search.type.all;
            });
        },
        selectAllClasses: (classes) => {
            linkGroupCycle.that.search.classeAll = !linkGroupCycle.that.search.classeAll;
            _.forEach(classes, (classe) => {
                classe.selected = linkGroupCycle.that.search.classeAll;
            });
            utils.safeApply(linkGroupCycle.that);
        },
        filterLinkSearch: function(searchParams){
            return (classe) => {
                let result = true;
                if (classe !== undefined) {
                    result = searchParams.type[classe.type_groupe];
                }
                if (result && searchParams.id_cycle !== undefined && classe !== undefined) {
                    result = (classe.id_cycle === searchParams.id_cycle);
                }

                if (result && searchParams.name && searchParams.name !== '*' && classe !== undefined
                    && searchParams.name !== '') {
                    let regexp = new RegExp('^' + searchParams.name.toUpperCase());
                    result =  regexp.test(classe.name.toUpperCase());
                }
                return result;
            };
        },
        checkSelectedClasses: function (classes) {
            linkGroupCycle.that.selectedClasses = _.filter(classes, function (classe){
                return classe.selected === true;
            });
            if (linkGroupCycle.that.selectedClasses.length === 0) {
                this.opened.lightboxLinkGroupCycle = false;
            }

            return linkGroupCycle.that.selectedClasses;
        },
        initRadio: function (classes) {
            linkGroupCycle.that.selectedClasses = _.filter(classes, function (classe){
                return classe.selected === true;
            });
            if (linkGroupCycle.that.selectedClasses.length !== 0
                && linkGroupCycle.that.selectedClasses[0].id_cycle !== null) {
                linkGroupCycle.that.selectedRadio = {
                    id_cycle: linkGroupCycle.that.selectedClasses[0].id_cycle.toString()
                };
            }
        }
    }
};