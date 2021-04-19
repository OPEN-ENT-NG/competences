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

import {notify, _, $,http as HTTP} from 'entcore';
import http from "axios";
import {Classe, evaluations} from '../models/teacher';
import * as utils from '../utils/teacher';


export const bilanPeriodique = {


    title: 'Bilan périodique',
    description: 'Permet de paramétrer les éléments nécessaires à la construction des bilans périodiques',
    that: undefined,
    controller: {
        init: async function () {
            console.log("bilanPeriodique");
            bilanPeriodique.that = this;
            bilanPeriodique.that.displayMessageLoader = true;
            bilanPeriodique.that.elementAll = {selected: false};
            bilanPeriodique.that.idStructure = bilanPeriodique.that.source.idStructure;
            bilanPeriodique.that.selected = {EPI: true, AP: false, parcours: false};
            await evaluations.sync();
            await evaluations.structure.sync();
            bilanPeriodique.that.selectedElements = [];
            bilanPeriodique.that.search.enseignant = null;
            bilanPeriodique.that.search.matiere = null;
            bilanPeriodique.that.search.classe = null;
            bilanPeriodique.that.dataELem = {
                idEtablissement: evaluations.structure.id,
                classes: [],
                ens_mat: []
            };
            bilanPeriodique.that.options = {};
            bilanPeriodique.that.thematique = {
                libelle: ""
            };
            bilanPeriodique.that.theme = {
                libelle: ""
            };
            bilanPeriodique.that.propertyName = 'theme.libelle';
            bilanPeriodique.that.reverse = true;
            bilanPeriodique.that.supprTheme = false;
            await bilanPeriodique.that.getElements();
            bilanPeriodique.that = this;
            bilanPeriodique.that.enableWatchers();
            bilanPeriodique.that.displayMessageLoader = false;
            bilanPeriodique.that.themeListe = {
                open:false
            };
            bilanPeriodique.that.epiAp = {
                classesSelected:[]
            };
            await utils.safeApply(this);
        },

        openElementLigthbox: async function (param?) {
            await bilanPeriodique.that.getThematique(bilanPeriodique.that.getTypeElement());
            bilanPeriodique.that.classes = evaluations.structure.classes;
            bilanPeriodique.that.enseignants = evaluations.structure.enseignants;
            bilanPeriodique.that.modifElem = param;
            bilanPeriodique.that.openedLightbox = true;
            bilanPeriodique.that.epiAp.classesSelected = [];
            if (param) {
                bilanPeriodique.that.dataELem = {
                    id: bilanPeriodique.that.selectedElements[0].id,
                    idEtablissement: evaluations.structure.id,
                    theme: bilanPeriodique.that.selectedElements[0].theme,
                    type: bilanPeriodique.that.selectedElements[0].type,
                    classes: bilanPeriodique.that.selectedElements[0].groupes,
                    ens_mat: bilanPeriodique.that.selectedElements[0].intervenantsMatieres,
                    libelle: bilanPeriodique.that.selectedElements[0].libelle,
                    description: bilanPeriodique.that.selectedElements[0].description
                };
                bilanPeriodique.that.search.classe = null;
                bilanPeriodique.that.search.enseignant = null;
                bilanPeriodique.that.search.matiere = null;
            } else {
                bilanPeriodique.that.emptyCheckbox(bilanPeriodique.that.elements);
                await bilanPeriodique.that.emptyLightbox();
            }
            bilanPeriodique.that.opened.lightboxCreatePE = true;
            bilanPeriodique.that.showAddtheme = false;
            await utils.safeApply(bilanPeriodique.that);
        },


        /////        Décoche les checkboxs      /////

        emptyCheckbox: function (elements) {
            _.forEach(elements, (element) => {
                element.selected = false;
            });
            bilanPeriodique.that.elementAll.selected = false;
        },


        /////       Création et modification des thèmes personnalisés      /////

        showTheme: async function (param) {
            bilanPeriodique.that.showAddtheme = param;
            bilanPeriodique.that.changeThematique = false;
            if (param) {
                bilanPeriodique.that.thematique.code = null;
                bilanPeriodique.that.thematique.libelle = null;
            }

            await utils.safeApply(bilanPeriodique.that);
            bilanPeriodique.that.getFocus();
        },

        openAddtheme: async function (theme) {
            bilanPeriodique.that.changeThematique = true;
            bilanPeriodique.that.thematique = {
                id: theme.id,
                code: theme.code,
                libelle: theme.libelle
            };
            bilanPeriodique.that.showAddtheme = true;

            await utils.safeApply(bilanPeriodique.that);
            bilanPeriodique.that.getFocus();
        },


        /////       Création d'un thème personnalisé      /////

        createThematique: async function (thematique) {
            try {
                let notThemeUnique = bilanPeriodique.that.themes.find(themes => themes.libelle === thematique.libelle);
                // Si le thème est unique on récupère le thème créé
                if (!notThemeUnique) {
                    await http.post('/competences/thematique',
                        { libelle: thematique.libelle, type: bilanPeriodique.that.getTypeElement(), idEtablissement: evaluations.structure.id});
                    bilanPeriodique.that.getThematique(bilanPeriodique.that.getTypeElement());
                    bilanPeriodique.that.showAddtheme = false;
                }
                else {
                    // Si le thème n'est pas unique : affichage de la lightbox avec message d'erreur
                    bilanPeriodique.that.createTheme = true;
                    bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                }

            } catch (e) {
                notify.error('Problème lors de la création de la thématique');
                console.error('Problème lors de la création de la thématique', e);
                throw e;
            }
            utils.safeApply(this);
        },


        /////       Modification d'un thème personnalisé      /////

        updateThematique: async function (thematique) {
            try {
                await http.put(`/competences/thematique?idThematique=${thematique.id}`,
                    {code: thematique.code, libelle: thematique.libelle, type: bilanPeriodique.that.getTypeElement()});
                bilanPeriodique.that.getThematique(bilanPeriodique.that.getTypeElement());
                bilanPeriodique.that.getElements();
                bilanPeriodique.that.showAddtheme = false;
            } catch (e) {
                notify.error('evaluations.thematique.update.error');
            }
            utils.safeApply(bilanPeriodique.that);
        },


        /////       Suppression d'un thème personnalisé      /////

        tryDeleteTheme: async function (theme) {
            let elements = await bilanPeriodique.that.getElementsOnThematique(theme.id);
            if (elements.length > 0) {
                bilanPeriodique.that.supprTheme = true;
                bilanPeriodique.that.opened.lightboxConfirmDelete = true;
            } else {
                await bilanPeriodique.that.deleteThematique(theme);
                bilanPeriodique.that.supprTheme = true;
            }
            await utils.safeApply(bilanPeriodique.that);
        },

        deleteThematique: async function (thematique) {
            try {
                if (bilanPeriodique.that.dataELem.theme === thematique) {
                    delete bilanPeriodique.that.dataELem.theme;
                }
                await http.delete(`/competences/thematique?idThematique=${thematique.id}`);
                await bilanPeriodique.that.getThematique(bilanPeriodique.that.getTypeElement());
                bilanPeriodique.that.showAddtheme = false;
            }
            catch (e) {
                notify.error('evaluations.thematique.delete.error');
            }
            await utils.safeApply(bilanPeriodique.that);
        },

        addTheme: function (theme) {
            bilanPeriodique.that.dataELem.theme = theme;
            bilanPeriodique.that.themeListe.open = false;
        },

        /////       Création d'un EPI/AP/Parcours      /////

        createElementBilanPeriodique: async function (dataELem) {
            try {
                if (bilanPeriodique.that.dataELem !== undefined && bilanPeriodique.that.dataELem !== null) {
                    bilanPeriodique.that.dataELem.type = bilanPeriodique.that.getTypeElement();
                    bilanPeriodique.that.dataELem.classes = bilanPeriodique.that.epiAp.classesSelected;
                    if (bilanPeriodique.that.dataELem.theme !== undefined
                        && bilanPeriodique.that.dataELem.theme.id !== undefined) {
                        bilanPeriodique.that.dataELem.id_theme = bilanPeriodique.that.dataELem.theme.id;

                        let libelleExistOnThematique = bilanPeriodique.that.elements.find(theme =>
                            (theme.libelle === dataELem.libelle) && (theme.theme.libelle === dataELem.theme.libelle));
                        // Si le libellé est unique sur le thème choisi, on récupère le thème créé
                        if (!libelleExistOnThematique) {
                            let url = `/competences/elementBilanPeriodique?type=${bilanPeriodique.that.dataELem.type}`;
                            const {data} = await http.post(url, bilanPeriodique.that.dataELem);
                            bilanPeriodique.that.elements.push(data);
                            bilanPeriodique.that.showAddtheme = false;
                            bilanPeriodique.that.opened.lightboxCreatePE = false;
                            bilanPeriodique.that.openedLightbox = false;
                            bilanPeriodique.that.emptyLightbox();
                            bilanPeriodique.that.getElements();
                        }
                        else {
                            // Si le libellé n'est pas unique sur le thème : affichage de la lightbox avec message d'erreur
                            bilanPeriodique.that.createElementBP = true;
                            bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                        }
                    }
                    else {
                        let libelleExist = bilanPeriodique.that.elements.find(theme =>
                            theme.libelle === dataELem.libelle);
                        // Si le libellé est unique on récupère le thème créé
                        if (!libelleExist) {
                            let url = `/competences/elementBilanPeriodique?type=${bilanPeriodique.that.dataELem.type}`;
                            const {data} = await http.post(url, bilanPeriodique.that.dataELem);
                            bilanPeriodique.that.elements.push(data);
                            bilanPeriodique.that.showAddtheme = false;
                            bilanPeriodique.that.opened.lightboxCreatePE = false;
                            bilanPeriodique.that.openedLightbox = false;
                            await bilanPeriodique.that.emptyLightbox();
                            await bilanPeriodique.that.getElements();
                        }
                        else {
                            // Si le libellé n'est pas unique : affichage de la lightbox avec message d'erreur
                            bilanPeriodique.that.createElementBP = true;
                            bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                        }
                        utils.safeApply(bilanPeriodique.that);
                    }
                }
            } catch (e) {
                notify.error('Problème lors de la création de l\'élément du bilan périodique');
                console.error('Problème lors de la création de l\'élément du bilan périodique');
                throw e;
            }
            utils.safeApply(this);
        },

        getThematique: async function (type) {
            try {
                let url = `/competences/thematique?type=${type}&idEtablissement=${evaluations.structure.id}`;
                let data = await http.get(url);
                bilanPeriodique.that.themes = data.data;
            } catch (e) {
                notify.error('evaluations.theme.get.error');
            }
            utils.safeApply(this);
        },

        /////       Filtre les thèmes pour savoir si ce sont des EPI / AP / Parcours      /////

        filterTheme: function (param) {
            return (item) => {
                if (item.personnalise === param) {
                    return item;
                }
            }
        },


        getElementsOnThematique: async function (idThematique) {
            try {
                let data = await http.get(`/competences/elements/thematique?idThematique=${idThematique}`);
                return data.data;
            } catch (e) {
                notify.error('evaluations.theme.get.error');
            }
        },


        getAppreciationsOnClasse: async function (idClasse, idElement) {
            try {
                let url = `/competences/elementsAppreciations?idClasse=${idClasse}&idElement=${idElement}`;
                let data = await http.get(url);
                return data.data;
            } catch (e) {
                notify.error('evaluations.appreciations.get.error');
            }
        },


        translate: function (key) {
            return utils.translate(key);
        },


        /////       Filtre les matières en fonction de l'enseignant sélectionné      /////

        syncMatieresEnseignant: async function (enseignant) {
            try {
                let data = await http.get(`/viescolaire/matieres?idEtablissement=${evaluations.structure.id}`);
                bilanPeriodique.that.options.matieres = data.data;
                await utils.safeApply(this);
            } catch (e) {
                console.error(e);
                notify.error('evaluations.matiere.get.error');
            }
        },

        notYetSelected : function () {
            return (subject) => {
                if (subject === undefined){
                    return false;
                }
                return !_.contains(bilanPeriodique.that.distinctSubjects, subject.id);
            };
        },

        pushData: function (teacher, dataElem, subject?) {
            if (subject) {
                utils.pushData(teacher,dataElem,subject);
            }
        },


        getElements: async function () {
            try {
                let url = `/competences/elementsBilanPeriodique?idEtablissement=${evaluations.structure.id}`;
                let {data} = await http.get(url);
                bilanPeriodique.that.elements = data;
                _.map(bilanPeriodique.that.elements, (element) => {
                    element.old_groupes = _.clone(element.groupes);
                    element.old_intervenantsMatieres = _.clone(element.intervenantsMatieres);
                });
                bilanPeriodique.that.$apply();
            } catch (e) {
                notify.error('evaluations.elements.get.error');
            }
        },


        closeLightbox: function () {
            bilanPeriodique.that.opened.lightboxConfirmDelete = false;
            bilanPeriodique.that.openedLightbox = true;
        },


        enableWatchers: function () {
            bilanPeriodique.that.$watch('opened.lightboxConfirmDelete', function (newVal, oldVal) {
                if (!newVal && bilanPeriodique.that.openedLightbox) {
                    bilanPeriodique.that.opened.lightboxCreatePE = true;
                } else {
                    bilanPeriodique.that.opened.lightboxCreatePE = false;
                    bilanPeriodique.that.openedLightbox = false;
                }
                if (oldVal === true && newVal === false) {
                    bilanPeriodique.that.createTheme = false;
                    bilanPeriodique.that.createElementBP = false;
                    bilanPeriodique.that.supprElem = false;
                    bilanPeriodique.that.supprElemAppr = false;
                    bilanPeriodique.that.supprEnseignant = false;
                    bilanPeriodique.that.supprClasse = false;
                    bilanPeriodique.that.supprTheme = false;
                    bilanPeriodique.that.modifElemSupprClasse = false;
                    utils.safeApply(bilanPeriodique.that);
                }
            });
        },


        getTypeElement: function () {
            let type = null;
            if (bilanPeriodique.that.selected.EPI) type = 1;
            if (bilanPeriodique.that.selected.AP) type = 2;
            if (bilanPeriodique.that.selected.parcours) type = 3;
            return type;
        },


        /////       Suppression d'un EPI/AP/Parcours      /////

        tryDeleteElements: async function (elements) {
            await bilanPeriodique.that.getAppreciations(elements);
            if (bilanPeriodique.that.appreciations !== undefined) {
                if (bilanPeriodique.that.appreciations.length > 0) {
                    bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                    bilanPeriodique.that.supprElemAppr = true;
                }
                else {
                    bilanPeriodique.that.supprElem = true;
                    bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                }
            }
            await utils.safeApply(this);
        },


        deleteElements: async function (elements) {
            try {
                let url = "/competences/elementsBilanPeriodique?idEtablissement=" + evaluations.structure.id;
                for (let i = 0; i < elements.length; i++) {
                    url += "&idElement=" + elements[i].id;
                }
                await http.delete(url);
                bilanPeriodique.that.emptyCheckbox(elements);
                bilanPeriodique.that.getElements();
            } catch (e) {
                notify.error('evaluations.elements.delete.error');
            }
            utils.safeApply(bilanPeriodique.that);
        },


        getAppreciations: async function (elements) {
            try {
                let url = "/competences/elementsAppreciations?idEtablissement=" + evaluations.structure.id;
                for (let i = 0; i < elements.length; i++) {
                    url += "&idElement=" + elements[i].id;
                }
                let data = await http.get(url);
                this.appreciations = data.data;
                utils.safeApply(this);
            } catch (e) {
                notify.error('evaluations.appreciations.get.error');
            }
        },


        /////       Modification d'un EPI/AP/Parcours      /////

        updateElement: async function (dataELem) {
            try {
                if (bilanPeriodique.that.dataELem
                    && bilanPeriodique.that.dataELem.theme !== undefined
                    && bilanPeriodique.that.dataELem.theme.id !== undefined) {
                    bilanPeriodique.that.dataELem.id_theme = bilanPeriodique.that.dataELem.theme.id;
                }

                if (bilanPeriodique.that.dataELem && bilanPeriodique.that.dataELem.description === null
                    || bilanPeriodique.that.dataELem.description === '') {
                    delete(bilanPeriodique.that.dataELem.description);
                }

                if (!_.isEmpty(bilanPeriodique.that.epiAp.classesSelected)) {
                    _.map(bilanPeriodique.that.epiAp.classesSelected, (classe) => {
                       if(!bilanPeriodique.that.dataELem.classes.includes(classe))
                           bilanPeriodique.that.dataELem.classes.push(classe);
                    });
                }

                if (bilanPeriodique.that.dataELem.theme !== undefined
                    && bilanPeriodique.that.dataELem.theme.id !== undefined) {
                    bilanPeriodique.that.dataELem.id_theme = bilanPeriodique.that.dataELem.theme.id;
                    let libelleExistOnThematique = bilanPeriodique.that.elements.find(theme =>
                        (theme.libelle === dataELem.libelle)
                        && (theme.theme.libelle === dataELem.theme.libelle) && theme.id !== dataELem.id);
                    // Si le libellé est unique sur le thème choisi, on récupère le thème créé
                    if (!libelleExistOnThematique) {
                        let url = `/competences/elementBilanPeriodique?idElement=${
                            bilanPeriodique.that.dataELem.id}&type=${bilanPeriodique.that.dataELem.type}`;
                        await http.put(url, bilanPeriodique.that.dataELem);
                        bilanPeriodique.that.showAddtheme = false;
                        bilanPeriodique.that.opened.lightboxCreatePE = false;
                        bilanPeriodique.that.openedLightbox = false;
                        bilanPeriodique.that.emptyLightbox();
                        bilanPeriodique.that.getElements();
                    }
                    else {
                        // Si le libellé n'est pas unique sur le thème : affichage de la lightbox avec message d'erreur
                        bilanPeriodique.that.createElementBP = true;
                        bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                    }
                }

                else {
                    let libelleExist = bilanPeriodique.that.elements.find(theme =>
                        theme.libelle === dataELem.libelle  && theme.id !== dataELem.id);
                    // Si le libellé est unique on récupère le thème créé
                    if (!libelleExist) {
                        let url = `/competences/elementBilanPeriodique?idElement=${
                            bilanPeriodique.that.dataELem.id}&type=${bilanPeriodique.that.dataELem.type}`;
                        await http.put(url, bilanPeriodique.that.dataELem);
                        bilanPeriodique.that.showAddtheme = false;
                        bilanPeriodique.that.opened.lightboxCreatePE = false;
                        bilanPeriodique.that.openedLightbox = false;
                        await bilanPeriodique.that.emptyLightbox();
                        await bilanPeriodique.that.getElements();
                    }
                    else {
                        // Si le libellé n'est pas unique : affichage de la lightbox avec message d'erreur
                        bilanPeriodique.that.createElementBP = true;
                        bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                    }
                    await utils.safeApply(bilanPeriodique.that);
                }
            }catch (e) {
                notify.error('evaluations.elements.update.error');
            }
            await utils.safeApply(bilanPeriodique.that);
        },

        distinctSubject: function () {
            let distinctSubjects = _.groupBy(_.pluck(bilanPeriodique.that.dataELem.ens_mat, 'matiere'), 'id');
            bilanPeriodique.that.distinctSubjects = _.keys(distinctSubjects);
            let size = _.size(bilanPeriodique.that.distinctSubjects);
            bilanPeriodique.that.hasTwoTimesSubject = _.some(_.values(distinctSubjects), function(subjectGrouped){
                return _.size(subjectGrouped) > 1;});
            return {size : size, hasTwoTimesSubject: bilanPeriodique.that.hasTwoTimesSubject};
        },


        /////       Champs obligatoire      /////

        requiredElement: function () {
            if (bilanPeriodique.that.dataELem) {
                if (bilanPeriodique.that.getTypeElement() === 1) {
                    let distinctSubject = bilanPeriodique.that.distinctSubject();
                    return (bilanPeriodique.that.dataELem.theme === undefined
                        || bilanPeriodique.that.dataELem.libelle === null
                        || bilanPeriodique.that.dataELem.libelle === undefined
                        || bilanPeriodique.that.dataELem.libelle === ""
                        || bilanPeriodique.that.dataELem.ens_mat.length < 2
                        || (bilanPeriodique.that.dataELem.classes.length === 0 && bilanPeriodique.that.epiAp.classesSelected.length === 0)
                        || distinctSubject.size < 2 || distinctSubject.hasTwoTimesSubject);
                }
                else if (bilanPeriodique.that.getTypeElement() === 2) {
                    let distinctSubject = bilanPeriodique.that.distinctSubject();
                    return (bilanPeriodique.that.dataELem.libelle === null
                        || bilanPeriodique.that.dataELem.libelle === undefined
                        || bilanPeriodique.that.dataELem.libelle === ""
                        || bilanPeriodique.that.dataELem.ens_mat.length < 1
                        || (bilanPeriodique.that.dataELem.classes.length === 0 && bilanPeriodique.that.epiAp.classesSelected.length === 0)
                        || distinctSubject.hasTwoTimesSubject);
                }
                else {
                    return (bilanPeriodique.that.dataELem.theme === null
                        || (bilanPeriodique.that.dataELem.classes.length === 0 && bilanPeriodique.that.epiAp.classesSelected.length === 0)
                    );
                }
            }
        },


        requiredTheme: function () {
            if (bilanPeriodique.that) {
                if (!bilanPeriodique.that.thematique) {
                    return false;
                }
                return bilanPeriodique.that.thematique.libelle === undefined
                    || bilanPeriodique.that.thematique.libelle === null
                    || bilanPeriodique.that.thematique.libelle === '';
            }
        },


        /////       Selection checkbox      /////

        selectUnselectElement: function (element) {
            if (!_.contains(bilanPeriodique.that.selectedElements, element)) {
                bilanPeriodique.that.selectedElements.push(element);
            } else {
                bilanPeriodique.that.selectedElements = _.without(bilanPeriodique.that.selectedElements, element);
            }
        },


        checkSelectedElements: function (elements) {
            bilanPeriodique.that.selectedElements = _.filter(elements, function (element) {
                return element.selected === true;
            });
            return bilanPeriodique.that.selectedElements;
        },


        selectAllElements: function (elements) {
            _.forEach(elements, (element) => {
                if (bilanPeriodique.that.itemFiltered(element)) {
                    element.selected = bilanPeriodique.that.elementAll.selected;
                }
            });
        },


        filterItem: function () {
            return (item) => {
                return bilanPeriodique.that.itemFiltered(item);
            }
        },


        itemFiltered: function (item) {
            if (bilanPeriodique.that.selected.EPI) {
                if (item.theme && item.libelle)
                    return item;
            }
            else if (bilanPeriodique.that.selected.AP) {
                if (!item.theme && item.libelle)
                    return item;
            }
            else if (bilanPeriodique.that.selected.parcours) {
                if (item.theme && !item.libelle)
                    return item;
            }
        },


        /////       Suppression des chips enseignants / matières et classe   /////

        delete: async function () {
            if (bilanPeriodique.that.supprElemAppr || bilanPeriodique.that.supprElemAppr
                || bilanPeriodique.that.supprElem || bilanPeriodique.that.supprElem) {
                bilanPeriodique.that.deleteElements(bilanPeriodique.that.selectedElements);
                bilanPeriodique.that.opened.lightboxConfirmDelete = false;
            }
            else if (bilanPeriodique.that.supprEnseignant || bilanPeriodique.that.supprEnseignant) {
                bilanPeriodique.that.openedLightbox = true;
                bilanPeriodique.that.deleteEnseignantMatiere();
            }
            else if (bilanPeriodique.that.supprClasse
                || bilanPeriodique.that.modifElemSupprClasse
                || bilanPeriodique.that.supprClasse || bilanPeriodique.that.modifElemSupprClasse) {
                bilanPeriodique.that.openedLightbox = true;
                await bilanPeriodique.that.deleteClasse();
            }
            else {
                bilanPeriodique.that.openedLightbox = true;
                bilanPeriodique.that.opened.lightboxConfirmDelete = false;
            }
        },


        tryDeleteEnseignantMatiere: function (item) {
            item.selected = true;
            bilanPeriodique.that.supprEnseignant = true;
            bilanPeriodique.that.opened.lightboxConfirmDelete = true;
        },


        deleteEnseignantMatiere: function () {
            bilanPeriodique.that.ensei_matieres = bilanPeriodique.that.dataELem.ens_mat;
            for (let i = 0; i < bilanPeriodique.that.ensei_matieres.length; i++) {
                if (bilanPeriodique.that.ensei_matieres[i].selected) {
                    bilanPeriodique.that.dataELem.ens_mat = _.without(bilanPeriodique.that.dataELem.ens_mat,
                        bilanPeriodique.that.dataELem.ens_mat[i]);
                    let matiere = _.pluck(bilanPeriodique.that.dataELem.ens_mat, 'matiere');
                    bilanPeriodique.that.options.matieres = _.union(bilanPeriodique.that.options.matieres, [matiere]);
                }
            }
            bilanPeriodique.that.search.enseignant = null;
            bilanPeriodique.that.search.matiere = null;
            bilanPeriodique.that.opened.lightboxConfirmDelete = false;
        },


        tryDeleteClasse: async function (classe) {
            let appreciations = await bilanPeriodique.that.getAppreciationsOnClasse(classe.id,
                bilanPeriodique.that.dataELem.id);
            if (appreciations) {
                if (appreciations.length > 0) {
                    bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                    classe.selected = true;
                    bilanPeriodique.that.modifElemSupprClasse = true;
                } else {
                    classe.selected = true;
                    bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                    bilanPeriodique.that.opened.lightboxCreatePE = false;
                    bilanPeriodique.that.supprClasse = true;
                }
            }
            await utils.safeApply(bilanPeriodique.that);
        },


        deleteClasse: async function () {
            for (let i = 0; i < bilanPeriodique.that.dataELem.classes.length; i++) {
                if (bilanPeriodique.that.dataELem.classes[i].selected) {
                    bilanPeriodique.that.dataELem.classes = _.without(bilanPeriodique.that.dataELem.classes,
                        bilanPeriodique.that.dataELem.classes[i]);
                }
            }
            bilanPeriodique.that.search.classe = null;
            bilanPeriodique.that.opened.lightboxConfirmDelete = false;
            await utils.safeApply(bilanPeriodique.that);
        },

        dropComboModel: async function (el: any, table: any) {
            table.splice(table.indexOf(el), 1);
            bilanPeriodique.that.epiAp.classesSelected = table;
            await utils.safeApply(bilanPeriodique.that);
        },

        setClassesSelected: async function (classes) {
            bilanPeriodique.that.epiAp.classesSelected = classes;
            await utils.safeApply(bilanPeriodique.that);
        },

        /////       Réinitialise la lightbox       /////

        emptyLightbox: async function () {
            bilanPeriodique.that.search.enseignant = null;
            bilanPeriodique.that.search.matiere = null;
            bilanPeriodique.that.search.classe = null;
            bilanPeriodique.that.search.enseignant = null;
            bilanPeriodique.that.search.matiere = null;
            bilanPeriodique.that.search.classe = null;
            bilanPeriodique.that.dataELem = {
                idEtablissement: evaluations.structure.id,
                classes: [],
                ens_mat: []
            };
            bilanPeriodique.that.createTheme = false;
            bilanPeriodique.that.createElementBP = false;
            bilanPeriodique.that.supprElem = false;
            bilanPeriodique.that.supprElemAppr = false;
            bilanPeriodique.that.supprEnseignant = false;
            bilanPeriodique.that.supprClasse = false;
            bilanPeriodique.that.supprTheme = false;
            bilanPeriodique.that.modifElemSupprClasse = false;
            await utils.safeApply(bilanPeriodique.that);
        },


        /////       Scroll jusqu'à l'input de création/modification dans la liste des thèmes       /////

        getFocus: function () {
            let el = $("#scrollto");
            el.blur();
            el.focus();
        },


        sortBy: async function (propertyName) {
            let isPropertyName = (bilanPeriodique.that.propertyName === propertyName);
            bilanPeriodique.that.reverse = (isPropertyName) ? !bilanPeriodique.that.reverse : false;
            bilanPeriodique.that.propertyName = propertyName;
            await utils.safeApply(bilanPeriodique.that);
        }

    }

};