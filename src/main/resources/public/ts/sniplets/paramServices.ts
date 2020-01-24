import {notify, idiom as lang, angular, _} from 'entcore';
import http from "axios";
import * as utils from '../utils/teacher';
import {Classe, TypeSousMatieres, Utils} from "../models/teacher";

export const paramServices = {
    title: 'Configuration des services',
    description: 'Permet le parametrage des services',
    that: undefined,
    controller: {
        Service: class Service {
            id_etablissement: string;
            id_enseignant: string;
            id_groupe: string;
            id_matiere: string;
            nom_enseignant: object;
            nom_groupe: object;
            nom_matiere: object;
            modalite: string;
            previous_modalite: string;
            evaluable: boolean;
            previous_evaluable: boolean;
            isManual: boolean;
            coefficient: number;

            constructor(service) {
                _.extend(this, service);
                this.previous_modalite = this.modalite;
                this.previous_evaluable = this.evaluable;
                this.coefficient = (Utils.isNull(this.coefficient)? 1 : this.coefficient);
            }

            hasNullProperty(){
                return !this.nom_enseignant || !this.nom_groupe || !this.nom_matiere;
            }

            createService(){
                try {
                    return http.post('/competences/service', this.toJson());
                } catch (e) {
                    notify.error('evaluation.service.error.create');
                }
            }
            updateServiceModalite(){

                let request = () => {
                    try {
                        return http.put('/competences/service', this.toJson());
                    } catch (e) {
                        notify.error('evaluation.service.error.update');
                    }
                };

                paramServices.that.classesSelected = [this.id_groupe];

                if(this.modalite == this.previous_modalite) {
                    return;
                } else {
                    request().then(() => {
                        this.previous_modalite = this.modalite;
                    }, () => {
                        this.modalite = this.previous_modalite;
                    })
                }
            }
            updateServiceCoefficient () {
                try {
                    return http.put('/competences/service', this.toJson());
                } catch (e) {
                    notify.error('evaluation.service.error.update');
                }
            }
            updateServiceEvaluable() {

                let request = () => {
                    try {
                        return http.put('/competences/service', this.toJson());
                    } catch (e) {
                        notify.error('evaluation.service.error.update');
                    }
                };

                if (this.evaluable == this.previous_evaluable) {
                    return;
                } else {
                    request().then(() => {
                        this.previous_evaluable = this.evaluable;
                    }, () => {
                        this.evaluable = this.previous_evaluable;
                    })
                }
            }

            deleteService(){
                try {
                    return http.delete("/competences/service"+
                        `?id_matiere=${this.id_matiere}`+
                        `&id_groupe=${this.id_groupe}`+
                        `&id_enseignant=${this.id_enseignant}`);
                } catch (e) {
                    notify.error('evaluation.service.error.delete');
                }
            }

            getDevoirsService(){
                try {
                    return http.get("/competences/devoirs/service"+
                        `?id_matiere=${this.id_matiere}`+
                        `&id_groupe=${this.id_groupe}`+
                        `&id_enseignant=${this.id_enseignant}`);
                } catch (e) {
                    notify.error("evaluations.service.devoir.error");
                }
            }

            updateDevoirsService(devoirs, matiere) {
                try {
                    return http.put("/competences/devoirs/service", {
                        id_devoirs: devoirs,
                        id_matiere: matiere
                    });
                } catch (e) {
                    notify.error('evaluations.service.devoir.update.error');
                }
            }

            deleteDevoirsService(devoirs) {
                try {
                    return http.put("/competences/devoirs/delete", {
                        id_devoirs: devoirs
                    });
                } catch (e) {
                    notify.error('evaluations.service.devoir.delete.error');
                }
            }

            toJson() {
                if(paramServices.that.classesSelected.length == 0 || !paramServices.that.lightboxes.create)
                    return {
                        id_etablissement: this.id_etablissement,
                        id_enseignant: this.id_enseignant,
                        id_matiere: this.id_matiere,
                        id_groupes: [this.id_groupe],
                        modalite: this.modalite,
                        evaluable: this.evaluable,
                        coefficient: this.coefficient
                    };
                else
                    return {
                        id_etablissement: this.id_etablissement,
                        id_enseignant: this.id_enseignant,
                        id_matiere: this.id_matiere,
                        id_groupes: _.map(paramServices.that.classesSelected,(classe) => {return classe.id;}),
                        modalite: this.modalite,
                        evaluable: this.evaluable,
                        coefficient: this.coefficient
                    };
            }
        },

        initServices: async function () {
            await this.runMessageLoader();

            paramServices.that.getServices().then(async ({data}) => {
                paramServices.that.services = _.reject(_.map(data, service => {
                    let enseignant = _.findWhere(paramServices.that.columns.enseignant.data, {id: service.id_enseignant});
                    let groupe = _.findWhere(paramServices.that.columns.classe.data, {id: service.id_groupe});
                    let matiere = _.findWhere(paramServices.that.columns.matiere.data, {id: service.id_matiere});
                    let missingParams = {
                        id_etablissement: paramServices.that.idStructure,
                        nom_enseignant: enseignant ? enseignant.displayName : null,
                        nom_matiere: matiere ? matiere.name + " (" + matiere.externalId + ")" : null,
                        nom_groupe: groupe ? groupe.name : null};
                    return new paramServices.that.Service(_.defaults(service, missingParams ));
                }), service => service.hasNullProperty());

                await  paramServices.that.subTopics.get();
                await this.stopMessageLoader();
            })
                .catch( async (error) => {
                    console.error(error);
                    await this.stopMessageLoader();
                });
            paramServices.that.classesSelected = [];
        },
        init: async function () {
            console.log(" ParamServices");
            this.idStructure = this.source.idStructure;
            this.services = [];
            this.search = "";
            this.matiereSelected = "";

            this.headers = {
                all: {name:"all", value: null, isSelected: true},
                evaluable: {name:"evaluation.service.headers.evaluable", value: true, isSelected: false},
                notEvaluable: {name:"evaluation.service.headers.notEvaluable", value: false, isSelected: false}
            };

            this.typeGroupes= {
                classes: {isSelected: true, name: "evaluation.service.filter.classes", type: 0},
                groupes: {isSelected: true, name: "evaluation.service.filter.groupes", type: 1},
                manualGroupes : {isSelected: true, name:"evaluation.service.filter.manualGroupes", type:2}
            };

            this.subTopics = new TypeSousMatieres([]);
            this.columns = {
                delete: {size : "one", name: "evaluation.service.columns.delete", filtered: false},
                matiere: {size: "three", data: [], name: "evaluation.service.columns.matiere", filtered: false},
                enseignant: {size: "one", data: [], name: "evaluation.service.columns.teacher", filtered: false},
                classe: {size: "two", data: [], name: "evaluation.service.columns.classGroup", filtered: false},
                modalite: {size: "one", data: [], name: "evaluation.service.columns.modalite", filtered: false},
                coefficient: {size: "one", name: "viescolaire.utils.coefficient", filtered: false},
                evaluable: {size: "one", name: "evaluation.service.columns.evaluable", filtered: false},
            };

            this.lightboxes= {
                switchEval: false,
                confirm: false,
                create: false,
                subEducationCreate:false,
                subTopicCreationForm:false
            };

            paramServices.that = this;
            await this.runMessageLoader();

            Promise.all([this.getClasses(), this.getMatieres(), this.getTeachers(), this.getModalite(), this.getRemplacements()])
                .then(async function([aClasses, aMatieres, aTeachers, aModalite, aRemplacement]) {
                    paramServices.that.columns.classe.data = _.map(aClasses.data,
                        (classe) => {
                            classe.type_groupe_libelle = Classe.get_type_groupe_libelle(classe);
                            classe = new Classe(classe);
                            return classe;
                        });
                    paramServices.that.columns.enseignant.data = _.map(aTeachers.data, teacher => teacher.u.data);
                    paramServices.that.columns.matiere.data = aMatieres.data;
                    paramServices.that.columns.modalite.data = _.pluck(aModalite.data, "id");
                    paramServices.that.remplacements = aRemplacement.data;

                    await paramServices.that.initServices();
                });
            this.classesSelected = [];
        },

        translate: function(key) {
            return lang.translate(key);
        },
        runMessageLoader: async function () {
            paramServices.that.displayMessageLoader = true;
            await utils.safeApply(paramServices.that);
        },
        stopMessageLoader: async function ( ) {
            paramServices.that.displayMessageLoader = false;
            await utils.safeApply(paramServices.that);
        },
        updateFilterEvaluable: function (selectedHeader) {
            _.each(paramServices.that.headers, header => header.isSelected = false);
            selectedHeader.isSelected = true;
        },

        filterGroupe: function (groupes, typeGroupes) {
            return (service) => {
                return _.findWhere(typeGroupes, {type: _.findWhere(groupes, {id: service.id_groupe}).type_groupe}).isSelected;
            }
        },

        filterEvaluable: function (headers) {
            return (service) => {
                let selectedHeader = _.findWhere(headers, {isSelected: true});
                if (selectedHeader.value == null) {
                    return true;
                } else {
                    return service.evaluable == selectedHeader.value
                }
            }
        },

        filterSearch: function (search) {
            return (service) => {
                return service.nom_groupe.toUpperCase().includes(search.toUpperCase())
                    || service.nom_enseignant.toUpperCase().includes(search.toUpperCase())
                    || service.nom_matiere.toUpperCase().includes(search.toUpperCase());
            }
        },

        checkIfExistsAndValid: function (service) {
            let exist = false;
            if(paramServices.that.classesSelected.length>0 && service.id_matiere != "" && service.id_enseignant != "") {
                paramServices.that.classesSelected.forEach(classe => {
                    if (_.findWhere(paramServices.that.services, {
                        id_matiere: service.id_matiere, id_enseignant: service.id_enseignant,
                        id_groupe: classe
                    }))
                        exist = true;
                });
                return exist;
            }else{
                return true;
            }
        },

        checkDevoirsService: async function (service, callback) {
            service.getDevoirsService().then(async function ({data}) {
                if (data.length == 0) {
                    await callback();
                } else {
                    paramServices.that.service = service;
                    paramServices.that.devoirs = data;
                    paramServices.that.callback = callback;
                    paramServices.that.error = paramServices.that.translate("evaluations.service.devoir.error").replace("[nbDevoir]", paramServices.that.devoirs.length);
                    paramServices.that.lightboxes.switchEval = true;
                    await utils.safeApply(paramServices.that);
                }
            })
        },
        initMatieresForSelect :function() {
            let isAlreadyIn = false;
            paramServices.that.matieresForSelect = [];
            paramServices.that.columns.matiere.data.map(matiere =>{
                paramServices.that.matieres.forEach(mm =>{
                    if(mm.id === matiere.id)
                        isAlreadyIn = true;
                });
                    if(!isAlreadyIn)
                        paramServices.that.matieresForSelect.push(matiere);
                isAlreadyIn = false;
                }
            )

        },
        openSubEducationLightBoxCreation: function (selectedServices){
            paramServices.that.matieres =[];
            console.log(  paramServices.that.columns.matiere.data)
            selectedServices.map(service => {
                paramServices.that.columns.matiere.data.map(matiere => {
                    if (matiere.id === service.id_matiere) {
                        matiere.selected = true;
                        paramServices.that.matieres.push(matiere);
                        matiere.sous_matieres.forEach(sm => {
                            paramServices.that.subTopics.all.map(sb =>{
                                if(sm.id_type_sousmatiere == sb.id){
                                    sb.selected = true;
                                }
                            })
                        })
                    }
                })
            });
            paramServices.that.initMatieresForSelect();
            paramServices.that.servicesSelected = selectedServices;
            paramServices.that.lightboxes.subEducationCreate = true;

        },
        openCreationSubTopicCreationLightBox: function(){
            paramServices.that.lightboxes.subEducationCreate = false;
            paramServices.that.lightboxes.subTopicCreationForm = true;


        },
        addMatiereToCreateSubTopic: async function(matiereToAdd){
            matiereToAdd.selected = true;
            paramServices.that.matieres.push(matiereToAdd);
            paramServices.that.matieresForSelect.map((matiere,index) =>{
                if (matiere.id === matiereToAdd.id){
                    paramServices.that.matieresForSelect.splice(index,1)
                }
            });
            await utils.safeApply(paramServices.that);
        },
        getSelectedDisciplines:function(){
            let selectedDisciplines = []
            paramServices.that.services.forEach(service =>{
                if(service.selected)
                    selectedDisciplines.push(service);
            });
            return selectedDisciplines;
        },
        oneDisicplineSelected:function(){
            let service = paramServices.that.services.find(service => service.selected)
            return service !== undefined;
        },
        switchEvaluableService: async function(service) {
            if(service.evaluable) {
                await service.updateServiceEvaluable();
            } else {
                await paramServices.that.checkDevoirsService(service, () => service.updateServiceEvaluable());
            }
        },

        deleteService: async function(service) {
            await  paramServices.that.checkDevoirsService(service, () => service.deleteService());
            notify.success('evaluation.service.delete');
            await paramServices.that.initServices();
        },

        doUpdateOrDelete: function (updateOrDelete, devoirs, service) {
            let id_devoirs = _.pluck(devoirs, "id");
            switch (updateOrDelete){
                case "update": {
                    if(paramServices.that.matiereSelected) {
                        let matiere = paramServices.that.matiereSelected;
                        if(matiere === service.id_matiere){
                            notify.info('evaluation.service.choose.another.subject');
                            break;
                        }
                        service.updateDevoirsService(id_devoirs, matiere).then(async () => {
                            let nom_matiere = _.findWhere(paramServices.that.columns.matiere.data, {id : matiere}).name;
                            let newService = new paramServices.that.Service({...service.toJson(),
                                id_matiere: matiere, nom_matiere: nom_matiere, evaluable: true});
                            await newService.createService();
                            paramServices.that.services.push(newService);
                            await paramServices.that.callback();
                            paramServices.that.lightboxes.switchEval = false;
                            notify.success('evaluation.service.update');
                            await paramServices.that.initServices();
                        });
                    }
                    else {
                        notify.info('evaluation.service.choose.a.subject');
                    }
                }
                    break;
                case "delete" : {
                    service.deleteDevoirsService(id_devoirs).then(async () => {
                        try {
                            await paramServices.that.callback();
                            paramServices.that.lightboxes.switchEval = false;
                            notify.success('evaluation.service.delete');
                            await paramServices.that.initServices();
                        }
                        catch (e) {
                            console.error(e);
                            notify.error('evaluation.service.delete.error');
                        }

                    });
                }
            }
        },

        setMatiere: function(matiere) {
            paramServices.that.matiereSelected = matiere;
        },

        deleteClasse: function (classe) {
            for (let i = 0; i < paramServices.that.classesSelected.length; i++) {
                if (paramServices.that.classesSelected[i] == classe) {
                    paramServices.that.classesSelected = _.without(paramServices.that.classesSelected,
                        paramServices.that.classesSelected[i]);
                }
            }
        },

        pushData:function(classe,listClasses){
            utils.pushData(classe,listClasses);
        },

        openCreateLightbox: function () {
            paramServices.that.service = new paramServices.that.Service(
                {
                    id_etablissement: paramServices.that.idStructure,
                    id_matiere: "",
                    id_enseignant: "",
                    id_groupe: "",
                    nom_matiere: "",
                    nom_enseignant: "",
                    nom_groupe: "",
                    isManual: true,
                    modalite: 'S',
                    evaluable: true
                });
            paramServices.that.lightboxes.create = true;
        },

        createService: async function(service) {
            try {
                await service.createService();
                if(paramServices.that.classesSelected.length >1)
                    notify.success('evaluation.services.create');
                else
                    notify.success('evaluation.service.create');
            } finally {
                paramServices.that.lightboxes.create = false;
                await paramServices.that.initServices();
            }
        },

        getServices: function () {
            try {
                return http.get(`/competences/services?idEtablissement=${paramServices.that.idStructure}`);
            } catch (e) {
                notify.error('evaluation.service.error.get');
            }
        },

        getClasses: function () {
            try {
                return http.get(`/viescolaire/classes?idEtablissement=${
                    paramServices.that.idStructure}&forAdmin=true`)
            } catch (e) {
                notify.error('evaluations.service.error.classe');
            }
        },

        getMatieres: function () {
            try {
                return http.get(`/viescolaire/matieres?idEtablissement=${paramServices.that.idStructure}`)
            } catch (e) {
                notify.error('evaluations.service.error.matiere');
            }
        },

        getModalite: function() {
            try {
                return http.get("/competences/modalites")
            } catch (e) {
                notify.error('evaluations.service.error.classe');
            }
        },

        getTeachers: function() {
            try {
                return http.get(`/viescolaire/teachers?idEtablissement=${paramServices.that.idStructure}`)
            } catch (e) {
                notify.error('evaluations.service.error.teacher');
            }
        },

        getRemplacements: function() {
            try {
                return http.get(`/competences/remplacements/list`)
            } catch (e) {
                notify.error('evaluations.service.error.remplacement');
            }
        }
    }
}