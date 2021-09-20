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

import {_, http, notify, template, idiom as lang} from 'entcore';
import {Domaine} from '../models/teacher';
import * as utils from '../utils/teacher';


export const itemsCompetences = {
    title: 'Ajout/Modification items de compétences',
    description: "Permet d'ajouter ou de modifier les items de compétences pour un établissement",
    that: undefined,
    controller: {
        init: function () {
            template.open('lightboxContainerCreateItem',
                '../../../competences/public/template/personnels/param_items/display_creation_item');
            itemsCompetences.that = this;
            this.idStructure = this.source.idStructure;
            this.cycles = this.source.cycles;
            this.search = {
                keyword: undefined
            };
            this.synchronized = {
                domaines: false,
                enseingments: false
            };
            this.opened.errorDeletePersoItem = false;
            this.newItem = {};
            this.opened = this.$parent.opened;
            this.lastSelectedCycle = this.$parent.lastSelectedCycle;
            this.showCompetencesDomaine = {};
            this.displayFilterDomaine = false;
            this.enableWatchers();
            this.getCompetences();
        },
        initSource: function () {
        },
        getCompetences: function () {
            http().getJson(`/competences/domaines?idStructure=${this.idStructure}`)
                .done((resDomaines) => {
                    if (resDomaines) {
                        let _res = [];
                        for (let i = 0; i < resDomaines.length; i++) {

                            let domaine = new Domaine(resDomaines[i]);
                            _res.push(domaine);
                        }
                        itemsCompetences.that.domaines = {
                            all: _res
                        };
                        itemsCompetences.that.synchronized.domaines = true;
                    }
                })
                .error(function () {
                    console.error('domaine not founded');
                    itemsCompetences.that.domaines = {
                        all: []
                    };
                }).bind(this);
            http().getJson(`/competences/enseignements?idStructure=${this.idStructure}`).done(function (res) {
                _.forEach(res, function (enseignement) {
                    enseignement.competences = {
                        all: enseignement['competences_1']
                    };
                    _.map(enseignement.competences.all, function (competence) {
                        return competence.composer = enseignement;
                    });
                    _.forEach(enseignement.competences.all, function (competence) {
                        if (competence['competences_2'].length > 0) {
                            competence.competences = {
                                all: competence['competences_2']
                            };
                            _.map(competence.competences.all, function (sousCompetence) {
                                return sousCompetence.composer = competence;
                            });
                        }
                        delete competence['competences_2'];
                    });
                    delete enseignement['competences_1'];
                });
                itemsCompetences.that.enseignements = {
                    all: res
                };
                itemsCompetences.that.synchronized.enseignements = true;
                itemsCompetences.that.initCycles();
            }.bind(this));
        },
        enableWatchers: function () {
            this.$watch(() => this.$parent.lastSelectedCycle, () => {
                this.lastSelectedCycle = this.$parent.lastSelectedCycle;
                utils.safeApply(this);
            });
            this.$watch(() => this.search.keyword, (newValue, oldValue) => {
                this.search.haschange = (newValue !== oldValue);
                utils.safeApply(this);
            });
        },
        initCycles: function () {
            if (this.synchronized.enseignements && this.synchronized.domaines) {
                let domaines = _.groupBy(this.domaines.all, 'id_cycle');
                for (let index in domaines) {
                    let cycle = _.findWhere(this.source.cycles, {id_cycle: parseInt(index)});
                    if (cycle !== undefined) {
                        cycle.domaines = {
                            all: domaines[index]
                        };
                        cycle.enseignements = {
                            all: []
                        };
                        _.forEach(this.enseignements.all, function (enseignement) {
                            let _enseignement = {
                                id: enseignement.id,
                                ids_domaine_int: enseignement.ids_domaine_int,
                                nom: enseignement.nom,
                                competences: {all: []}
                            };
                            _.forEach(enseignement.competences.all, function (connaissance) {
                                if (connaissance.id_cycle === cycle.id_cycle) {
                                    _enseignement.competences.all.push(connaissance);
                                }
                            });
                            if (_enseignement.competences.all.length > 0) {
                                cycle.enseignements.all.push(_enseignement);
                            }
                        });
                    }
                }
                this.searchBilan = {
                    parDomaine: 'false'
                };
                this.initFilter(true);
                this.$apply();
                console.log(' Sniplets ItemsCompetences Loaded');
            }
        },
        initFilter: function (pbInitSelected) {
            itemsCompetences.that.enseignementsFilter = {};
            itemsCompetences.that.competencesFilter = {};
            itemsCompetences.that._domaines = [];
            itemsCompetences.that.showCompetencesDomaine = {};
            itemsCompetences.that.displayFilterDomaine = false;
            for (let i = 0; i < itemsCompetences.that.enseignements.all.length; i++) {
                let currEnseignement = itemsCompetences.that.enseignements.all[i];
                itemsCompetences.that.enseignementsFilter[currEnseignement.id] = {
                    isSelected: pbInitSelected,
                    nomHtml: currEnseignement.nom
                };
                // on initialise aussi les compétences
                this.initFilterRec(currEnseignement.competences, pbInitSelected);
            }
            itemsCompetences.that._domaines = _.sortBy(this._domaines, 'code_domaine');
        },

        /**
         * Initialise le nom html des compétences (pour gérer le surlignement lors des recherches)
         *
         * @param poCompetences la liste des compétences
         * @param pbInitSelected boolean d'initialisation
         */

        initFilterRec: function (poCompetences, pbInitSelected) {
            if (poCompetences !== undefined) {
                let _b = false;
                let comp: any = null;
                for (let i = 0; i < poCompetences.all.length; i++) {
                    let currCompetence = poCompetences.all[i];
                    if ((currCompetence.ids_domaine_int !== undefined && currCompetence.ids_domaine_int[0].lengh === 1
                            && itemsCompetences.that.showCompetencesDomaine[currCompetence.ids_domaine_int[0]] === true)
                        || itemsCompetences.that.showCompetencesDomaine.length === undefined) {
                        comp = _.findWhere(poCompetences.all, {id: poCompetences.all[i].id}) !== undefined;
                        if (comp !== undefined) _b = false;
                        itemsCompetences.that.competencesFilter[
                        currCompetence.id + '_' + currCompetence.id_enseignement] = {
                            isSelected: _b,
                            nomHtml: currCompetence.nom,
                            data: currCompetence
                        };

                        this.initFilterRec(currCompetence.competences, pbInitSelected);
                    }
                }
            }
        },

        deletePersoItem: function () {
            http().delete(`/competences/items/${this.idStructure}`)
                .done(() => {
                    this.opened.lightboxDeletePersoItem = false;
                    this.opened.errorDeletePersoItem = false;
                    this.opened.lightboxConfirmDeletePersoItem = false;
                    this.getCompetences();
                    notify.info('item.success.delete.perso');
                    utils.safeApply(this);
                })
                .error(() => {
                    this.opened.errorDeletePersoItem = true;
                    console.error('delete not work');
                    utils.safeApply(this);
                }).bind(this);
        },

        // suppression de la personnalisation des items de compétences
        openDeletePersoItem: function () {
            this.opened.lightboxDeletePersoItem = true;
        },
        openCreateItem: function (connaissance, domaines) {
            this.itemsCompetences.that.opened.lightboxCreateItem = true;
            this.itemsCompetences.that.newItem = {
                ismanuelle: true,
                nom: undefined,
                id_parent: connaissance.id,
                id_cycle: connaissance.id_cycle,
                id_enseignement: connaissance.id_enseignement,
                id_type: 2,
                ids_domaine: connaissance.ids_domaine_int,
                id_etablissement: this.itemsCompetences.that.structure.id
            };
            this.itemsCompetences.that.printDomaines = _.clone(domaines);
          //  this.itemsCompetences.that.selectedDomaines = this.itemsCompetences.that.newItem.ids_domaine;
            this.itemsCompetences.that.initSelectDomaine(this.itemsCompetences.that.printDomaines);
            utils.safeApply(this.itemsCompetences.that);
        }.bind(this),

        initSelectDomaine: function (domaines) {
            _.forEach(domaines.all, (domaine) => {
                domaine.selected = _.contains(this.itemsCompetences.that.newItem.ids_domaine, domaine.id);
                this.itemsCompetences.that.initSelectDomaine(domaine.domaines);
            });
        }.bind(this),

        isStringUndefinedOrEmpty: function(name) {
            return (name === undefined || name.trim().length === 0)
        }.bind(this),

        selectDomaine: function (domaine) {
            if (domaine.selected && !_.contains(this.itemsCompetences.that.newItem.ids_domaine, domaine.id)) {
                this.itemsCompetences.that.newItem.ids_domaine.push(domaine.id);
            }
            else if (!domaine.selected) {
                this.itemsCompetences.that.newItem.ids_domaine =
                    _.without(this.itemsCompetences.that.newItem.ids_domaine, domaine.id);
            }
            if (this.itemsCompetences.that.newItem.hasOwnProperty('id')) {
                this.itemsCompetences.that.updatedDomaineId = domaine.id;
                this.itemsCompetences.that.saveItem(this.itemsCompetences.that.newItem, 'updateDomaine');
            }
        }.bind(this),

        // Affichage des Domaines d'une compétence
        openItemDomaine: function (competence) {
            this.itemsCompetences.that.newItem = competence;
            this.itemsCompetences.that.newItem.ids_domaine = competence.ids_domaine_int;
            this.itemsCompetences.that.printDomaines = _.clone(this.itemsCompetences.that.lastSelectedCycle.domaines);
            if (template.isEmpty('patchwork' + competence.id)) {
                template.open('patchwork' + competence.id,
                    '../../../competences/public/template/personnels/param_items/showDomaine');
            }
            else {
                template.close('patchwork' + competence.id);
            }
            if (this.itemsCompetences.that.lastCompetence !== undefined
                && this.itemsCompetences.that.lastCompetence.id !== competence.id
                && !template.isEmpty('patchwork' + this.itemsCompetences.that.lastCompetence.id)) {
                template.close('patchwork' + this.itemsCompetences.that.lastCompetence.id);
                utils.safeApply(this.itemsCompetences.that);
            }
            this.itemsCompetences.that.lastCompetence = competence;
            utils.safeApply(this.itemsCompetences.that);
        }.bind(this),

        jsonCreateItem: function (item) {
            return {
                nom: item.nom,
                id_etablissement: itemsCompetences.that.structure.id,
                id_parent: item.id_parent,
                id_type: item.id_type,
                id_cycle: item.id_cycle,
                id_enseignement: item.id_enseignement,
                ids_domaine: item.ids_domaine
            };
        },
        jsonUpdateMaskItem: function (item) {
            return {
                id: item.id,
                id_etablissement: itemsCompetences.that.structure.id,
                masque: !item.masque
            };
        },
        jsonUpdateNameItem: function (item) {
            return {
                id: item.id,
                id_etablissement: itemsCompetences.that.structure.id,
                nom: item.nom
            };
        },
        jsonUpdateDomaineItem: function (item) {
            return {
                id: item.id,
                id_etablissement: itemsCompetences.that.structure.id,
                id_domaine: itemsCompetences.that.updatedDomaineId
            };
        },
        jsonUpdateOrderItem: function (item) {
            return {
                id: item.id,
                id_etablissement: itemsCompetences.that.structure.id,
                id_enseignement: item.id_enseignement,
                index: item.index
            };
        },
        saveItem: function (item, action) {
            switch (action) {
                case 'create': {
                    http().postJson(`competences/competence`, this.jsonCreateItem(item))
                        .done(() => {
                            this.opened.lightboxCreateItem = false;
                            this.getCompetences();
                            notify.info('item.success.create');
                            utils.safeApply(this);
                        })
                        .error((res) => {
                            console.error(res);
                            this.opened.lightboxCreateItem = false;
                            this.opened.error = true;
                            if (res.status === 401) {
                                notify.error('item.error.unautorize.create');
                                utils.safeApply(this);
                            }
                            else {
                                notify.error('item.error.create');
                                utils.safeApply(this);
                            }
                        }).bind(this);
                    break;
                }
                case 'mask': {
                    http().putJson(`competences/competence`, this.jsonUpdateMaskItem(item))
                        .done((res) => {
                            item.masque = !item.masque;
                            if(item.masque && res.masquecompetence === 'use') {
                                notify.info('item.message.isUse');
                            }
                            else {
                                notify.info('item.success.updateMask');
                            }
                            utils.safeApply(this);
                        })
                        .error(function () {
                            notify.error('item.error.updateMask');
                            utils.safeApply(this);
                        }).bind(this);
                    break;
                }
                case 'rename': {
                    if (item.name !== item.nom) {
                        item.nom = item.name;
                        http().putJson(`competences/competence`, this.jsonUpdateNameItem(item))
                            .done(() => {
                                this.getCompetences();
                                notify.info('item.success.updateName');
                                utils.safeApply(this);
                            })
                            .error(function () {
                                notify.error('item.error.updateName');
                                utils.safeApply(this);
                            }).bind(this);
                    }
                    break;

                }
                case 'updateDomaine': {
                    http().putJson(`competences/competence`, this.jsonUpdateDomaineItem(item))
                        .done(() => {
                            this.getCompetences();
                            template.close('patchwork' + item.id);
                            utils.safeApply(this);
                            template.open('patchwork' + item.id,
                                '../../../competences/public/template/personnels/param_items/showDomaine');
                            utils.safeApply(this);
                        })
                        .error(function () {
                            notify.error('item.error.updateDomaine');
                            utils.safeApply(this);
                        }).bind(this);
                    break;
                }
                case 'reinitItem': {
                    this.trash(item, true);
                    break;
                }
                default:
                    break;
            }
        },
        trash: function (item, reinit) {
            console.dir('trash' + item.nom);
            http().delete(`competences/competence?id=${item.id}&id_etablissement=${itemsCompetences.that.structure.id}`)
                .done((res) => {
                    this.getCompetences();
                    if(reinit) {
                        notify.info('item.success.reinit');
                        utils.safeApply(this);
                    }else {
                        if (res.deletecompetence === 'MASQUAGE') {
                            notify.info('item.success.delete.mask');
                        }
                        else {
                            notify.info('item.success.delete');
                        }
                        utils.safeApply(this);
                    }
                })
                .error(function () {
                    notify.error('item.error.delete');
                    utils.safeApply(this);
                }).bind(this);
        },
        updateOrder: function (competence) {
            if (competence.oldIndex !== competence.index) {
                competence.oldIndex = competence.index;
                this.competence.oldIndex = competence.oldIndex;
                let res = [];
                for (let i = 0 ; i < this.$parent.connaissance.competences.all.length; i++) {
                    let _c = _.findWhere(this.$parent.connaissance.competences.all, {index: i});
                    if (_c !== undefined) {
                        res.push(this.jsonUpdateOrderItem(_c));
                    }
                }
                if (res.length === this.$parent.connaissance.competences.all.length) {
                    console.dir(competence.index + ' update competence ' + competence.id);

                    http().putJson(`competences/competence`, {index: res})
                        .done(() => {
                            // notify.success('item.success.updateOrder');
                            utils.safeApply(this);
                        })
                        .error(function () {
                            notify.error('item.error.updateOrder');
                            utils.safeApply(this);
                        }).bind(this);
                }
            }
        },
        initEnseignementItem: function(competence, index) {
            competence.action = false;
            competence.rename = false;
            if ( competence.index === undefined) {
                competence.index = index;
                competence.oldIndex = index;
            }
        },
        initEnseignementConnaissance: function (connaissance) {
            connaissance.OpenSousDom = false;
            connaissance.action = false;
        },
        isEmptyShowDomaine: function(id) {
            return template.isEmpty('patchwork' + id);
        }
    }
};