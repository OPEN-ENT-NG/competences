import {notify, template, _} from 'entcore';
import http from "axios";
import {evaluations} from '../models/teacher';
import * as utils from '../utils/teacher';

console.log("here");

export const bilanPeriodique = {
    title: 'Bilan périodique',
    description: 'Permet de paramétrer les éléments nécessaires à la construction des bilans périodiques',
    that: undefined,
    controller: {
        init: async function () {
            this.elementAll = {selected: false};
            this.idStructure = this.source.idStructure;
            this.selected = {EPI: true, AP: false, parcours: false};
            await evaluations.sync();
            await evaluations.structure.sync();
            this.selectedElements = [];
            this.search.enseignant = null;
            this.search.matiere = null;
            this.search.classe = null;
            this.dataELem = {
                idEtablissement: evaluations.structure.id,
                classes: [],
                ens_mat: []
            };
            this.thematique = {
                code: "",
                libelle: ""
            };
            await this.getElements();
            this.enableWatchers();
            bilanPeriodique.that = this;
        },

        openElementLigthbox: async function (param?) {
            bilanPeriodique.that.emptyCheckbox(this.elements);
            await this.getThematique(this.getTypeElement());
            bilanPeriodique.that.classes = evaluations.structure.classes;
            bilanPeriodique.that.enseignants = evaluations.structure.enseignants;
            bilanPeriodique.that.modifElem = param;
            bilanPeriodique.that.openedLightbox = true;

            if(param) {
                bilanPeriodique.that.dataELem = {
                    id: this.selectedElements[0].id,
                    idEtablissement: evaluations.structure.id,
                    theme: this.selectedElements[0].theme,
                    type: this.selectedElements[0].type,
                    classes: this.selectedElements[0].groupes,
                    ens_mat: this.selectedElements[0].intervenantsMatieres,
                    libelle: this.selectedElements[0].libelle,
                    description: this.selectedElements[0].description
                };
            } else {
                bilanPeriodique.that.dataELem = {
                    idEtablissement : evaluations.structure.id,
                    classes: [],
                    ens_mat: []
                };
            }
            bilanPeriodique.that.themeBase = {
                open: false
            };
            bilanPeriodique.that.themePerso = {
                open: false
            };
            bilanPeriodique.that.opened.lightboxCreatePE = true;
            bilanPeriodique.that.showAddtheme = false;
            utils.safeApply(bilanPeriodique.that);
        },

        emptyCheckbox : function(elements) {
            _.forEach(elements, (element) => {
                element.selected = false;
            });
            bilanPeriodique.that.elementAll.selected = false;
        },


        showTheme: function(param) {
            bilanPeriodique.that.showAddtheme = param;
            bilanPeriodique.that.changeThematique = false;
            if(param){
                bilanPeriodique.that.thematique.code = null;
                bilanPeriodique.that.thematique.libelle = null;
            }
        },

        openAddtheme: function(theme) {
            bilanPeriodique.that.changeThematique = true;

            bilanPeriodique.that.thematique = {
                id: theme.id,
                code: theme.code,
                libelle: theme.libelle
            };
            bilanPeriodique.that.showAddtheme = true;
        },

        /////       Création d'un thème personnalisé      /////

        createThematique: async function (thematique) {
            try {
                await http.post('/competences/thematique',
                    {code: thematique.code, libelle: thematique.libelle, type: this.getTypeElement()});
                bilanPeriodique.that.getThematique(this.getTypeElement());
                // bilanPeriodique.that.emptyCheckbox();
                bilanPeriodique.that.showAddtheme = false;
            } catch (e) {
                notify.error('Problème lors de la création de la thématique');
                console.error('Problème lors de la création de la thématique');
                throw e;
            }

            utils.safeApply(this);
        },


        /////       Modification d'un thème personnalisé      /////

        updateThematique: async function (thematique) {
            try {
                await http.put(`/competences/thematique?idThematique=${thematique.id}`,
                    {code: thematique.code, libelle: thematique.libelle, type: this.getTypeElement()});
                bilanPeriodique.that.getThematique(this.getTypeElement());
                bilanPeriodique.that.getElements();
                bilanPeriodique.that.showAddtheme = false;
            } catch (e) {
                notify.error('evaluations.thematique.update.error');
            }
            utils.safeApply(this);
        },


        /////       Suppression d'un thème personnalisé      /////

         tryDeleteTheme: async function (theme) {
            let elements = await this.getElementsOnThematique(theme.id);
            if (elements.length > 0) {
                this.opened.lightboxConfirmDeleteThemes = true;
            } else {
                await this.deleteThematique(theme);
            }
            utils.safeApply(this);
        },

        deleteThematique: async function (thematique) {
            try {
                if(this.dataELem.theme === thematique){
                    delete this.dataELem.theme;
                }
                await http.delete(`/competences/thematique?idThematique=${thematique.id}`);
                bilanPeriodique.that.getThematique(this.getTypeElement());
                bilanPeriodique.that.showAddtheme = false;
                // bilanPeriodique.that.opened.lightboxCreatePE = true;
            }
            catch (e) {
                notify.error('evaluations.thematique.delete.error');
            }
            utils.safeApply(this);
        },

        addTheme: function(theme) {
            this.dataELem.theme = theme;
            this.themeBase.open = false;
            this.themePerso.open = false;
        },


        /////       Création d'un EPI/AP/Parcours      /////

        createElementBilanPeriodique: async function () {
            try {
                if (this.dataELem !== undefined && this.dataELem !== null) {
                    this.dataELem.type = this.getTypeElement();
                    if (this.dataELem.theme !== undefined && this.dataELem.theme.id !== undefined) {
                        this.dataELem.id_theme =  this.dataELem.theme.id;
                    }
                    const {data} = await http.post(`/competences/elementBilanPeriodique?type=${this.dataELem.type}`, this.dataELem);
                    this.elements.push(data);
                }
                bilanPeriodique.that.emptyLightbox();
                bilanPeriodique.that.opened.lightboxCreatePE = false;
                bilanPeriodique.that.getElements();
            } catch (e) {
                notify.error('Problème lors de la création de l\'élément du bilan périodique');
                console.error('Problème lors de la création de l\'élément du bilan périodique');
                throw e;
            }
        },

        getThematique: async function (type) {
            try {
                let data = await http.get(`/competences/thematique?type=${type}`);
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
                let data = await http.get(`/competences/elementsAppreciations?idClasse=${idClasse}&idElement=${idElement}`);
                return data.data;
            } catch (e) {
                notify.error('evaluations.appreciations.get.error');
            }
        },

        translate: function (key) {
            return utils.translate(key);
        },

        /////       Filtre les matières en fonctions de l'enseignant sélectionné      /////

        syncMatieresEnseignant: async function (enseignant) {
            try {
                    let data = await http.get(`/viescolaire/matieres?idEnseignant=${enseignant.id}&idEtablissement=${evaluations.structure.id}&isEnseignant=${true}`);
                    this.matieres = data.data;
                    utils.safeApply(this);
            } catch (e) {
                notify.error('evaluations.matiere.get.error');
            }
        },

        pushData: function (param1, param2?) {
            if (param2) {
                if (!_.findWhere(this.dataELem.ens_mat, {intervenant: param1, matiere: param2})) {
                    this.dataELem.ens_mat.push({intervenant: param1, matiere: param2});
                }
            } else {
                if (!_.contains(this.dataELem.classes, param1)) {
                    this.dataELem.classes.push(param1)
                }
            }
        },

        getElements: async function () {
            try {
                let {data} = await http.get(`/competences/elementsBilanPeriodique?idEtablissement=${evaluations.structure.id}`);
                this.elements = data;
                this.$apply();
            } catch (e) {
                notify.error('evaluations.elements.get.error');
            }
        },

        enableWatchers: function () {
            this.$watch('opened.lightboxConfirmDeleteChips', function(newVal) {
                if (!newVal && bilanPeriodique.that.openedLightbox) {
                    bilanPeriodique.that.opened.lightboxCreatePE = true;
                } else {
                    bilanPeriodique.that.opened.lightboxCreatePE = false;
                }
            });

            this.$watch('opened.lightboxConfirmDeleteClasse', function(newVal) {
                if (!newVal && bilanPeriodique.that.openedLightbox) {
                    bilanPeriodique.that.opened.lightboxCreatePE = true;
                } else {
                    bilanPeriodique.that.opened.lightboxCreatePE = false;
                }
            });

            this.$watch('opened.lightboxConfirmDeleteThemes', function(newVal) {
                if (!newVal && bilanPeriodique.that.openedLightbox) {
                    bilanPeriodique.that.opened.lightboxCreatePE = true;
                } else {
                    bilanPeriodique.that.opened.lightboxCreatePE = false;
                }
            });
        },

        getTypeElement: function () {
            let type = null;
            if (this.selected.EPI) type = 1;
            if (this.selected.AP) type = 2;
            if (this.selected.parcours) type = 3;
            return type;
        },


        /////       Suppression d'un EPI/AP/Parcours      /////

        tryDeleteElements: async function (elements) {
            await this.getAppreciations(elements);
            if (this.appreciations !== undefined) {
                (this.appreciations.length > 0) ? this.opened.lightboxConfirmDeleteElements = true
                    : await this.deleteElements(elements);
            }
            utils.safeApply(this);
        },

        deleteElements: async function (elements) {
            try {
                let url = "/competences/elementsBilanPeriodique?idEtablissement=" + evaluations.structure.id;
                for (var i = 0; i < elements.length; i++) {
                    url += "&idElement=" + elements[i].id;
                }
                await http.delete(url);
                this.emptyCheckbox();
                this.getElements();
            } catch (e) {
                notify.error('evaluations.elements.delete.error');
            }
            utils.safeApply(this);
        },

        getAppreciations: async function (elements) {
            try {
                let url = "/competences/elementsAppreciations?idEtablissement=" + evaluations.structure.id;
                for (var i = 0; i < elements.length; i++) {
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

        updateElement: async function () {
            try {
                if (this.dataELem !== undefined && this.dataELem !== null
                && this.dataELem.theme !== undefined && this.dataELem.theme.id !== undefined ) {
                    this.dataELem.id_theme =  this.dataELem.theme.id;
                }
                if (this.dataELem.description === null
                    ||  this.dataELem.description === '') {
                    delete(this.dataELem.description);
                }
                await http.put(`/competences/elementBilanPeriodique?idElement=${bilanPeriodique.that.dataELem.id}&type=${bilanPeriodique.that.dataELem.type}`, this.dataELem);
                bilanPeriodique.that.opened.lightboxCreatePE = false;
                bilanPeriodique.that.getElements();

            } catch (e) {
                notify.error('evaluations.elements.update.error');
            }
            utils.safeApply(this);
        },


        /////       Champs obligatoire      /////

        requiredElement: function () {
            if(this.dataELem) {
                if (this.getTypeElement() === 1) {
                    return this.dataELem.theme === undefined || this.dataELem.libelle === null
                        || this.dataELem.libelle === undefined || this.dataELem.libelle === ""
                        || this.dataELem.ens_mat.length < 2 || this.dataELem.classes.length === 0
                }
                else if (this.getTypeElement() === 2) {
                    return this.dataELem.libelle === null || this.dataELem.libelle === undefined
                        || this.dataELem.libelle === "" || this.dataELem.ens_mat.length < 2
                        || this.dataELem.classes.length === 0
                }
                else {
                    return this.dataELem.theme === null || this.dataELem.classes.length === 0
                }
            }
        },

        requiredTheme: function() {
            if(bilanPeriodique.that){
                if(!bilanPeriodique.that.thematique){
                    return false;
                }
                return bilanPeriodique.that.thematique.code === undefined
                    || bilanPeriodique.that.thematique.libelle === undefined
                    || bilanPeriodique.that.thematique.code === null
                    || bilanPeriodique.that.thematique.libelle === null
                    || bilanPeriodique.that.thematique.code === ''
                    || bilanPeriodique.that.thematique.libelle === ''
            }
        },

        /////       Selection checkbox      /////

        selectUnselectElement: function (element) {
            if (!_.contains(this.selectedElements, element)) {
                this.selectedElements.push(element);
            } else {
                this.selectedElements = _.without(this.selectedElements, element);
            }
        },

        checkSelectedElements: function (elements) {
            this.selectedElements = _.filter(elements, function (element) {
                return element.selected === true;
            });
            if (this.selectedElements.length === 0) {
                this.opened.lightboxConfirmDeleteElements = false;
            }
            return this.selectedElements;
        },

        selectAllElements: function (elements) {
             _.forEach(elements, (element) => {
                if(this.itemFiltered(element)){
                    element.selected = bilanPeriodique.that.elementAll.selected;
                }
            });
        },

        filterItem: function () {
        return (item) => {
            return this.itemFiltered(item);
            }
        },

        itemFiltered: function (item){
            if (this.selected.EPI) {
                if (item.theme && item.libelle)
                    return item;
            }
            else if (this.selected.AP) {
                if (!item.theme && item.libelle)
                    return item;
            }
            else if (this.selected.parcours) {
                if (item.theme && !item.libelle)
                    return item;
            }
        },


        /////       Suppression des chips enseigantns / matières et classe   /////

        tryDeleteEnseignantMatiere: function (item) {
            item.selected = true;
            this.opened.lightboxConfirmDeleteChips = true;
        },

        deleteEnseignantMatiere: function () {
            this.ensei_matieres = this.dataELem.ens_mat;
            for (let i = 0; i < this.ensei_matieres.length; i++) {
                if (this.ensei_matieres[i].selected) {
                    this.dataELem.ens_mat = _.without(this.dataELem.ens_mat, this.dataELem.ens_mat[i]);
                }
            }
            bilanPeriodique.that.search.enseignant = null;
            bilanPeriodique.that.search.matiere = null;
            bilanPeriodique.that.opened.lightboxConfirmDeleteChips = false;
            // bilanPeriodique.that.opened.lightboxCreatePE = true;
        },

        tryDeleteClasse: async function (classe) {
            if(bilanPeriodique.that.modifElem){
                let appreciations = await this.getAppreciationsOnClasse(classe.id, this.dataELem.id);
                if(appreciations){
                    if (appreciations.length > 0) {
                        this.opened.lightboxConfirmDeleteClasse = true;
                        // bilanPeriodique.that.opened.lightboxCreatePE = false;
                        classe.selected = true;
                    } else {
                        classe.selected = true;
                        await this.deleteClasse();
                    }
                }
                utils.safeApply(this);
            } else {
                classe.selected = true;
                await this.deleteClasse();
            }
        },

        deleteClasse: function () {
            for(let i = 0; i < this.dataELem.classes.length; i++){
                if(this.dataELem.classes[i].selected){
                    this.dataELem.classes = _.without(this.dataELem.classes, this.dataELem.classes[i]);
                }
            }
            bilanPeriodique.that.search.classe = null;
            bilanPeriodique.that.opened.lightboxConfirmDeleteClasse = false;
            // bilanPeriodique.that.opened.lightboxCreatePE = true;
        },

        emptyLightbox: function () {
            bilanPeriodique.that.search.enseignant = null;
            bilanPeriodique.that.search.matiere = null;
            bilanPeriodique.that.search.classe = null;
            bilanPeriodique.that.dataELem = {
                idEtablissement: evaluations.structure.id,
                classes: [],
                ens_mat: []
            };
            this.dataELem = bilanPeriodique.that.dataELem;
        }
    }
}