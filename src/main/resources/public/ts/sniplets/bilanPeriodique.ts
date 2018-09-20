import {notify, template, _, $} from 'entcore';
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
            this.propertyName = 'theme.libelle';
            this.reverse = true;
            this.supprTheme = false;
            await this.getElements();
            bilanPeriodique.that = this;
            this.enableWatchers();
        },

        openElementLigthbox: async function (param?) {
            await this.getThematique(this.getTypeElement());
            bilanPeriodique.that.classes = evaluations.structure.classes;
            bilanPeriodique.that.enseignants = evaluations.structure.enseignants;
            bilanPeriodique.that.modifElem = param;
            bilanPeriodique.that.openedLightbox = true;
            if (param) {
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
                bilanPeriodique.that.search.classe = null;
                bilanPeriodique.that.search.enseignant = null;
                bilanPeriodique.that.search.matiere = null;
            } else {
                bilanPeriodique.that.emptyCheckbox(this.elements);
                bilanPeriodique.that.emptyLightbox();
            }
            bilanPeriodique.that.opened.lightboxCreatePE = true;
            bilanPeriodique.that.showAddtheme = false;
            utils.safeApply(bilanPeriodique.that);
        },


        /////        Décoche les checkboxs      /////

        emptyCheckbox: function (elements) {
            _.forEach(elements, (element) => {
                element.selected = false;
            });
            bilanPeriodique.that.elementAll.selected = false;
        },


        /////       Création et modification des thèmes personnalisés      /////

        showTheme: function (param) {
            bilanPeriodique.that.showAddtheme = param;
            bilanPeriodique.that.changeThematique = false;
            if (param) {
                bilanPeriodique.that.thematique.code = null;
                bilanPeriodique.that.thematique.libelle = null;
            }
            bilanPeriodique.that.getFocus();
            utils.safeApply(bilanPeriodique.that);
        },

        openAddtheme: function (theme) {
            bilanPeriodique.that.changeThematique = true;
            bilanPeriodique.that.thematique = {
                id: theme.id,
                code: theme.code,
                libelle: theme.libelle
            };
            bilanPeriodique.that.showAddtheme = true;
            bilanPeriodique.that.getFocus();
            utils.safeApply(bilanPeriodique.that);
        },


        /////       Création d'un thème personnalisé      /////

        createThematique: async function (thematique) {
            try {
                /*let notcodeUnique = bilanPeriodique.that.themes.find(themes => themes.code === thematique.code);
                // Si le code est Unique  on récupère le thème créé
                if (!notcodeUnique) {
                    await http.post('/competences/thematique',
                        {code: thematique.code, libelle: thematique.libelle, type: this.getTypeElement(), idEtablissement: evaluations.structure.id});
                     bilanPeriodique.that.getThematique(this.getTypeElement());
                     bilanPeriodique.that.showAddtheme = false;
                }
                else {
                    // Si le code n'est pas unique : affichage de la lightbox avec message d'erreur
                    bilanPeriodique.that.createTheme = true;
                    bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                }*/

                await http.post('/competences/thematique',
                    {code: null, libelle: thematique.libelle, type: this.getTypeElement(), idEtablissement: evaluations.structure.id});
                bilanPeriodique.that.getThematique(this.getTypeElement());
                bilanPeriodique.that.showAddtheme = false;

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
                    {code: thematique.code, libelle: thematique.libelle, type: this.getTypeElement()});
                bilanPeriodique.that.getThematique(this.getTypeElement());
                bilanPeriodique.that.getElements();
                bilanPeriodique.that.showAddtheme = false;
            } catch (e) {
                notify.error('evaluations.thematique.update.error');
            }
            utils.safeApply(bilanPeriodique.that);
        },


        /////       Suppression d'un thème personnalisé      /////

        tryDeleteTheme: async function (theme) {
            let elements = await this.getElementsOnThematique(theme.id);
            if (elements.length > 0) {
                bilanPeriodique.that.supprTheme = true;

                bilanPeriodique.that.opened.lightboxConfirmDelete = true;
            } else {
                await this.deleteThematique(theme);
                bilanPeriodique.that.supprTheme = true;
            }
            utils.safeApply(bilanPeriodique.that);
        },

        deleteThematique: async function (thematique) {
            try {
                if (this.dataELem.theme === thematique) {
                    delete this.dataELem.theme;
                }
                await http.delete(`/competences/thematique?idThematique=${thematique.id}`);
                bilanPeriodique.that.getThematique(this.getTypeElement());
                bilanPeriodique.that.showAddtheme = false;
            }
            catch (e) {
                notify.error('evaluations.thematique.delete.error');
            }
            utils.safeApply(this);
        },


        addTheme: function (theme) {
            this.dataELem.theme = theme;
            this.themeListe.open = false;
        },


        /////       Création d'un EPI/AP/Parcours      /////

        createElementBilanPeriodique: async function () {
            try {
                if (this.dataELem !== undefined && this.dataELem !== null) {
                    this.dataELem.type = this.getTypeElement();
                    if (this.dataELem.theme !== undefined && this.dataELem.theme.id !== undefined) {
                        this.dataELem.id_theme = this.dataELem.theme.id;
                    }
                    const {data} = await http.post(`/competences/elementBilanPeriodique?type=${this.dataELem.type}`, this.dataELem);
                    this.elements.push(data);
                }
                bilanPeriodique.that.emptyLightbox();
                bilanPeriodique.that.opened.lightboxCreatePE = false;
                bilanPeriodique.that.openedLightbox = false;
                bilanPeriodique.that.getElements();
            } catch (e) {
                notify.error('Problème lors de la création de l\'élément du bilan périodique');
                console.error('Problème lors de la création de l\'élément du bilan périodique');
                throw e;
            }
            utils.safeApply(this);
        },

        getThematique: async function (type) {
            try {
                let data = await http.get(`/competences/thematique?type=${type}&idEtablissement=${evaluations.structure.id}`);
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


        /////       Filtre les matières en fonction de l'enseignant sélectionné      /////

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

                let foundEM = this.dataELem.ens_mat.find(EM => EM.intervenant.id === param1.id && EM.matiere.id === param2.id);
                console.log(foundEM);
                if (!foundEM) {
                    this.dataELem.ens_mat.push({intervenant: param1, matiere: param2});
                }
            } else {
                let foundClasse = this.dataELem.classes.find(classe => classe.id === param1.id);
                if (!foundClasse) {
                    this.dataELem.classes.push(param1);
                }
            }
        },


        getElements: async function () {
            try {
                let {data} = await http.get(`/competences/elementsBilanPeriodique?idEtablissement=${evaluations.structure.id}`);
                this.elements = data;
                _.map(this.elements, (element) => {
                    element.old_groupes = _.clone(element.groupes);
                    element.old_intervenantsMatieres = _.clone(element.intervenantsMatieres);
                });
                this.$apply();
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
            if (this.selected.EPI) type = 1;
            if (this.selected.AP) type = 2;
            if (this.selected.parcours) type = 3;
            return type;
        },


        /////       Suppression d'un EPI/AP/Parcours      /////

        tryDeleteElements: async function (elements) {
            await this.getAppreciations(elements);
            if (this.appreciations !== undefined) {
                if (this.appreciations.length > 0) {
                    this.opened.lightboxConfirmDelete = true;
                    bilanPeriodique.that.supprElemAppr = true;
                }
                else {
                    bilanPeriodique.that.supprElem = true;
                    bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                    // await this.deleteElements(elements);
                }
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

        updateElement: async function () {
            try {
                if (this.dataELem !== undefined && this.dataELem !== null
                    && this.dataELem.theme !== undefined && this.dataELem.theme.id !== undefined) {
                    this.dataELem.id_theme = this.dataELem.theme.id;
                }
                if (this.dataELem.description === null
                    || this.dataELem.description === '') {
                    delete(this.dataELem.description);
                }
                await http.put(`/competences/elementBilanPeriodique?idElement=${bilanPeriodique.that.dataELem.id}&type=${bilanPeriodique.that.dataELem.type}`, this.dataELem);
                bilanPeriodique.that.opened.lightboxCreatePE = false;
                bilanPeriodique.that.openedLightbox = false;
                bilanPeriodique.that.getElements();

            } catch (e) {
                notify.error('evaluations.elements.update.error');
            }
            utils.safeApply(bilanPeriodique.that);
        },


        /////       Champs obligatoire      /////

        requiredElement: function () {
            if (this.dataELem) {
                if (this.getTypeElement() === 1) {
                    return this.dataELem.theme === undefined || this.dataELem.libelle === null
                        || this.dataELem.libelle === undefined || this.dataELem.libelle === ""
                        || this.dataELem.ens_mat.length < 2 || this.dataELem.classes.length === 0
                }
                else if (this.getTypeElement() === 2) {
                    return this.dataELem.libelle === null || this.dataELem.libelle === undefined
                        || this.dataELem.libelle === "" || this.dataELem.ens_mat.length < 1
                        || this.dataELem.classes.length === 0
                }
                else {
                    return this.dataELem.theme === null || this.dataELem.classes.length === 0
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
            return this.selectedElements;
        },


        selectAllElements: function (elements) {
            _.forEach(elements, (element) => {
                if (this.itemFiltered(element)) {
                    element.selected = bilanPeriodique.that.elementAll.selected;
                }
            });
        },


        filterItem: function () {
            return (item) => {
                return this.itemFiltered(item);
            }
        },


        itemFiltered: function (item) {
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


        /////       Suppression des chips enseignants / matières et classe   /////

        delete: function () {
            if (this.supprElemAppr || bilanPeriodique.that.supprElemAppr || this.supprElem || bilanPeriodique.that.supprElem) {
                bilanPeriodique.that.deleteElements(this.selectedElements);
                bilanPeriodique.that.opened.lightboxConfirmDelete = false;
            }
            else if (bilanPeriodique.that.supprEnseignant || this.supprEnseignant) {
                bilanPeriodique.that.openedLightbox = true;
                bilanPeriodique.that.deleteEnseignantMatiere();
            }
            else if (bilanPeriodique.that.supprClasse
                || bilanPeriodique.that.modifElemSupprClasse
                || this.supprClasse || this.modifElemSupprClasse) {
                bilanPeriodique.that.openedLightbox = true;
                bilanPeriodique.that.deleteClasse();
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
            this.ensei_matieres = this.dataELem.ens_mat;
            for (let i = 0; i < this.ensei_matieres.length; i++) {
                if (this.ensei_matieres[i].selected) {
                    this.dataELem.ens_mat = _.without(this.dataELem.ens_mat, this.dataELem.ens_mat[i]);
                }
            }
            bilanPeriodique.that.search.enseignant = null;
            bilanPeriodique.that.search.matiere = null;
            bilanPeriodique.that.opened.lightboxConfirmDelete = false;
        },


        tryDeleteClasse: async function (classe) {
            if (bilanPeriodique.that.modifElem) {
                let appreciations = await this.getAppreciationsOnClasse(classe.id, this.dataELem.id);
                if (appreciations) {
                    if (appreciations.length > 0) {
                        bilanPeriodique.that.opened.lightboxConfirmDelete = true;
                        classe.selected = true;
                        bilanPeriodique.that.modifElemSupprClasse = true;
                    } else {
                        classe.selected = true;
                        this.opened.lightboxConfirmDelete = true;
                        bilanPeriodique.that.opened.lightboxCreatePE = false;
                        bilanPeriodique.that.supprClasse = true;
                    }
                }
                utils.safeApply(this);
            } else {
                classe.selected = true;
                this.opened.lightboxConfirmDelete = true;
                bilanPeriodique.that.opened.lightboxCreatePE = false;
                bilanPeriodique.that.supprClasse = true;
            }
        },


        deleteClasse: function () {
            for (let i = 0; i < this.dataELem.classes.length; i++) {
                if (this.dataELem.classes[i].selected) {
                    this.dataELem.classes = _.without(this.dataELem.classes, this.dataELem.classes[i]);
                }
            }
            bilanPeriodique.that.search.classe = null;
            bilanPeriodique.that.opened.lightboxConfirmDelete = false;
        },


        /////       Réinitialise la lightbox       /////

        emptyLightbox: function () {
            this.search.enseignant = null;
            this.search.matiere = null;
            this.search.classe = null;
            bilanPeriodique.that.search.enseignant = null;
            bilanPeriodique.that.search.matiere = null;
            bilanPeriodique.that.search.classe = null;
            bilanPeriodique.that.dataELem = {
                idEtablissement: evaluations.structure.id,
                classes: [],
                ens_mat: []
            };
            this.dataELem = bilanPeriodique.that.dataELem;
            bilanPeriodique.that.createTheme = false;
            bilanPeriodique.that.supprElem = false;
            bilanPeriodique.that.supprElemAppr = false;
            bilanPeriodique.that.supprEnseignant = false;
            bilanPeriodique.that.supprClasse = false;
            bilanPeriodique.that.supprTheme = false;
            bilanPeriodique.that.modifElemSupprClasse = false;
            utils.safeApply(bilanPeriodique.that);
        },


        /////       Scroll jusqu'à l'input de création/modification dans la liste des thèmes       /////

        getFocus: function () {
            $("#scrollto").focus();
        },


        /////       O,v       /////

        sortBy: function (propertyName) {
            bilanPeriodique.that.reverse = (this.propertyName === propertyName) ? !this.reverse : false;
            bilanPeriodique.that.propertyName = propertyName;
            utils.safeApply(bilanPeriodique.that);
        }

    }

}