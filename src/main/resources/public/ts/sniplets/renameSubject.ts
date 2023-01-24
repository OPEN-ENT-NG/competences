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

import {_, notify, idiom as lang} from 'entcore';
import http from 'axios';
import {safeApply} from "../utils/functions/safeApply";

export const renameSubject = {
    title: lang.translate('evaluations.rename.subject.title'),
    description: lang.translate('evaluations.rename.subject.description'),
    that: {
        newModel : undefined,
        opened: undefined,
        models: undefined,
        id: undefined
    },
    controller: {
        init: async function () {
            this.lang = lang;
            this.id = this.source.id;
            this.search = {
                name : ""
            };
            this.opened = {
                lightboxCreateModel: false,
                displayMessageLoader: true
            };
            _.extend(this.opened, this.source.opened);
            renameSubject.that = this;
            await this.getSubjects();
        },

        initSource: function () {
        },

        getSubjects: async function () {
            try {
                renameSubject.that.opened.displayMessageLoader = true;
                await safeApply(renameSubject.that);
                let response = await http.get(`/competences/matieres/models/${this.id}`);
                renameSubject.that.models = {
                    all: response.data
                };
                renameSubject.that.opened.displayMessageLoader = false;
                await safeApply(renameSubject.that);
            } catch (e) {
                notify.error('evaluations.rename.subject.error.get.subjects');
                renameSubject.that.opened.displayMessageLoader = false;
                await safeApply(renameSubject.that);
            }
        },

        toJson: function() {
             _.extend(renameSubject.that.newModel, {
                 idStructure : renameSubject.that.id
            });
            return renameSubject.that.newModel;
        },

        saveModel: async function () {
            try {
                renameSubject.that.opened.lightboxCreateModel = false;
                await safeApply(renameSubject.that);
                await http.post('/competences/matieres/libelle/model/save', this.toJson());
                notify.success('evaluations.rename.subject.success.save.model');
                await this.getSubjects();
            } catch (e) {
                console.error(e);
                notify.error('evaluations.rename.subject.error.save.model');
            }
        },

        deleteModel: async function(model) {
          try{
              await http.delete(`/competences/matieres/model/${model.id}?idEtablissement=${renameSubject.that.id}`);
              notify.success('evaluations.rename.subject.success.delete.model');
              await this.getSubjects();
          } catch (e) {
              console.error(e);
              notify.error('evaluations.rename.subject.error.delete.model');
          }
        },

        openCreateModel: async function (model?) {
            renameSubject.that.opened.lightboxCreateModel = true;
            if (model === undefined) {
                renameSubject.that.newModel = {
                    subjects: [],
                    title: " "
                };
                _.forEach(renameSubject.that.models.all[0].subjects, (subject) => {
                    renameSubject.that.newModel.subjects.push(_.clone(subject));
                });
            } else {
                renameSubject.that.newModel = model;
            }
            await safeApply(renameSubject.that);
        },

        filterSearch: function(searchParams){
            return (model) => {
                let result = true;

                if (result && searchParams.name && searchParams.name !== '*' && searchParams.name !== '') {
                    let regexp = new RegExp('^' + searchParams.name.toUpperCase());
                    result = regexp.test(model.title.toUpperCase());
                }
                return result;
            };
        }
    }
};