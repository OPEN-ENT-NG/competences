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

import {_,http as HTTP, idiom as lang,notify,$} from 'entcore';
import {TypePeriode} from "../models/common/TypePeriode";
import {Attachment} from "../models/common/Attachement";
import * as utils from "../utils/teacher";
import http from 'axios';

export const paramImportCSV = {
    title: 'Paramètres d\'importation des retards absences' ,
    description: 'Permet d\'importer un fichier répertoriant tous les absences retards pour une période pour un établissement donné',
    that: undefined,
    controller: {
        init: async function () {
            console.log("paramImportCSV");
            this.structure = this.source;
            this.import = {periode : undefined };
            this.opened.displayMessageLoader = false;
            this.hasHomonymes = false;
            this.loadingAttachments = [];
            this.attachments = [];
            this.absencesRetardsFromPresences = false;
            this.checkBox = {
                disabled : false,
                hidden : false
            };
            paramImportCSV.that = this;
            await this.initRecuperationAbsencesRetardsFromPresences();
            utils.safeApply(paramImportCSV.that);
        },
        /**
         * Récupère les structures de l'utilisateur qui ont activées la récupération des absences/retards du module presences.
         * @returns {Promise<T>} Callback de retour.
         */
        initRecuperationAbsencesRetardsFromPresences(): Promise<any[]> {
            return new Promise((resolve, reject) => {
                HTTP().getJson('/competences/init/sync/presences?structureId='+paramImportCSV.that.structure.id)
                    .done((state) => {
                        if(state.presences_sync && state.installed && state.activate)
                            paramImportCSV.that.absencesRetardsFromPresences = true;
                        paramImportCSV.that.checkBox.hidden = !state.installed || !state.activate;
                        resolve(state);
                    })
                    .error(() => {
                        reject();
                    });
            });
        },

        /**
         * Active ou désactive la récupération des absences/retards du module presences d'une structure de l'utilisateur.
         * @param checked
         * @param id_structure
         * @returns {Promise<T>} Callback de retour.
         */
        changeAbsencesRetardsFromPresences(checked:boolean,id_structure: string): Promise<any[]> {
            return new Promise((resolve, reject) => {
                HTTP().postJson('/competences/sync/presences', {state: checked, structureId: id_structure})
                    .done((res) => {
                        resolve(res);
                    })
                    .error(() => {
                        reject();
                    });
            });
        },

        displayImportPeriode : function (periode) {
            return paramImportCSV.that.getI18n("viescolaire.periode." +
                paramImportCSV.that.getImportOrdreTypePeriode(periode).type) + " " +
                paramImportCSV.that.getImportOrdreTypePeriode(periode).ordre;
        },
        getI18n : (libelle: string) => {
            return lang.translate(libelle);
        },
        getImportOrdreTypePeriode : (periode: TypePeriode) => {
            if (periode !== undefined) {
                return _.findWhere(paramImportCSV.that.structure.typePeriodes.all, {id: periode.id});
            }
        },
        postAttachments : (fichiers) => {
            paramImportCSV.that.loadingAttachments = [];
            paramImportCSV.that.newAttachments = fichiers;
            for (let i = 0; i < fichiers.length; i++) {
                const targetAttachment = fichiers[i];
                const attachmentObj = new Attachment(targetAttachment);
                paramImportCSV.that.loadingAttachments.push(attachmentObj);
                paramImportCSV.that.attachments.push(attachmentObj);
            }
        },
        importAttachments :  async function (fichiers) {
            paramImportCSV.that.hasHomonymes = false;
            paramImportCSV.that.opened.displayMessageLoader = true;
            utils.safeApply(paramImportCSV.that);
            await paramImportCSV.that.promisesOfImportAttachments(fichiers);
            paramImportCSV.that.opened.displayMessageLoader = false;
            utils.safeApply(paramImportCSV.that);
        },
        promisesOfImportAttachments : (fichiers) => {
            paramImportCSV.that.newAttachments = (fichiers !== undefined)? fichiers : [];
            const promises: Promise<any>[] = [];

            if(paramImportCSV.that.import.periode != undefined) {
                paramImportCSV.that.homonymes = [];
                utils.safeApply(paramImportCSV.that);
                for (let i = 0; i < paramImportCSV.that.newAttachments.length; i++) {
                    const attachmentObj = paramImportCSV.that.newAttachments[i];

                    const formData = new FormData();
                    formData.append('file', attachmentObj.file);

                    let idPeriode = paramImportCSV.that.import.periode.id;
                    let idEtablissement = paramImportCSV.that.structure.id;
                    let url = `/viescolaire/import/evenements?idPeriode=${idPeriode}&idEtablissement=${idEtablissement}`;
                    const promise = http.post(url, formData, {
                        onUploadProgress: (e: ProgressEvent) => {
                            if (e.lengthComputable) {
                                let percentage = Math.round((e.loaded * 100) / e.total);
                                console.log(percentage);
                                attachmentObj.progress.completion = percentage;
                                utils.safeApply(paramImportCSV.that);
                            }
                        }})
                        .then(response => {
                            paramImportCSV.that.loadingAttachments.splice(paramImportCSV.that.loadingAttachments.indexOf(attachmentObj), 1);
                            let fileName = response.data.filename;
                            if (response.data.homonymes !== undefined && response.data.homonymes.length > 0){
                                paramImportCSV.that.hasHomonymes = true;
                                paramImportCSV.that.homonymes.push({filename : fileName, eleves : response.data.homonymes});
                                utils.safeApply(paramImportCSV.that);
                            }
                            // On reset le file input
                            $('#input-attachment-declaration').val('');
                            if (response.data.error) {
                                notify.error(fileName + ' : ' + lang.translate('import.csv.error'));
                            }
                            else {
                                if(!response.data.isValid) {
                                    notify.error(fileName + ' : ' + lang.translate('import.csv.error.invalid'));
                                }
                                else{
                                    let nbInsert = response.data.nbInsert;
                                    let nbNotInsert = response.data.nbLines - nbInsert - 2;
                                    notify.success(fileName + ' : ' +  lang.translate('import.csv.success')
                                        +  nbInsert
                                        + lang.translate('import.students')
                                        + ((nbNotInsert > 0)?(nbNotInsert + lang.translate('import.user.not.insert')): '')
                                    );
                                }
                            }
                            utils.safeApply(paramImportCSV.that);
                        })
                        .catch(e => {
                            paramImportCSV.that.loadingAttachments.splice(paramImportCSV.that.loadingAttachments.indexOf(attachmentObj), 1);
                            notify.error(attachmentObj.name + ' : ' + lang.translate('import.csv.error'));

                            utils.safeApply(paramImportCSV.that);
                        });

                    promises.push(promise);
                }
            }
            else {
                notify.info('please.set.periode');
            }
            return Promise.all(promises);
        },
        deleteAttachment : (filename) => {
            paramImportCSV.that.attachments = paramImportCSV.that.attachments.filter(obj => obj.filename !== filename);
        },
        clearHomonymes : function () {
            paramImportCSV.that.hasHomonymes = false;
            utils.safeApply(paramImportCSV.that);
        }
    }
};