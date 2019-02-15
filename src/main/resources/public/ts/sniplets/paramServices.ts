import {notify, idiom as lang, angular, _} from 'entcore';
import http from "axios";
import * as utils from '../utils/teacher';

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

            constructor(service) {
                _.extend(this, service);
                this.previous_modalite = this.modalite;
                this.previous_evaluable = this.evaluable;
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
                }

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

            updateServiceEvaluable() {

                let request = () => {
                    try {
                        return http.put('/competences/service', this.toJson());
                    } catch (e) {
                        notify.error('evaluation.service.error.update');
                    }
                }

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
                return {
                    id_etablissement: this.id_etablissement,
                    id_enseignant: this.id_enseignant,
                    id_matiere: this.id_matiere,
                    id_groupe: this.id_groupe,
                    modalite: this.modalite,
                    evaluable: this.evaluable
                }
            }
        },

        init: function () {
            console.log("tic");
            this.displayMessageLoader = true;
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

            this.columns = {
                delete: {size : "one", name: "evaluation.service.columns.delete", filtered: false},
                matiere: {size: "three", data: [], name: "evaluation.service.columns.matiere", filtered: false},
                enseignant: {size: "three", data: [], name: "evaluation.service.columns.teacher", filtered: false},
                classe: {size: "two", data: [], name: "evaluation.service.columns.classGroup", filtered: false},
                modalite: {size: "one", data: [], name: "evaluation.service.columns.modalite", filtered: false},
                evaluable: {size: "one", name: "evaluation.service.columns.evaluable", filtered: false},
            };

            this.lightboxes= {
                switchEval: false,
                confirm: false,
                create: false
            };

            paramServices.that = this;

            Promise.all([this.getClasses(), this.getMatieres(), this.getTeachers(), this.getModalite(), this.getRemplacements()])
                .then(function([aClasses, aMatieres, aTeachers, aModalite, aRemplacement]) {
                    paramServices.that.columns.classe.data = aClasses.data;
                    paramServices.that.columns.enseignant.data = _.map(aTeachers.data, teacher => teacher.u.data);
                    paramServices.that.columns.matiere.data = aMatieres.data;
                    paramServices.that.columns.modalite.data = _.pluck(aModalite.data, "id");
                    paramServices.that.remplacements = aRemplacement.data;

                    paramServices.that.getServices().then(function ({data}) {
                        paramServices.that.services = _.reject(_.map(data, service => {
                            let enseignant = _.findWhere(paramServices.that.columns.enseignant.data, {id: service.id_enseignant});
                            let groupe = _.findWhere(paramServices.that.columns.classe.data, {id: service.id_groupe});
                            let matiere = _.findWhere(paramServices.that.columns.matiere.data, {id: service.id_matiere});
                            let missingParams = {
                                id_etablissement: paramServices.that.idStructure,
                                nom_enseignant: enseignant ? enseignant.displayName : null,
                                nom_matiere: matiere ? matiere.name : null,
                                nom_groupe: groupe ? groupe.name : null};
                            return new paramServices.that.Service(_.defaults(service, missingParams ));
                        }), service => service.hasNullProperty());
                        paramServices.that.displayMessageLoader = false;
                        utils.safeApply(paramServices.that)
                    });
                });
        },

        translate: function(key) {
            return lang.translate(key);
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

        checkIfExists: function (service) {
            return !!_.findWhere(paramServices.that.services, {id_matiere: service.id_matiere, id_enseignant: service.id_enseignant,
                id_groupe: service.id_groupe})  ;
        },

        checkDevoirsService: function (service, callback) {
            service.getDevoirsService().then(function ({data}) {
                if (data.length == 0) {
                    callback();
                } else {
                    paramServices.that.service = service;
                    paramServices.that.devoirs = data;
                    paramServices.that.callback = callback;
                    paramServices.that.error = paramServices.that.translate("evaluations.service.devoir.error").replace("[nbDevoir]", paramServices.that.devoirs.length);
                    paramServices.that.lightboxes.switchEval = true;
                    utils.safeApply(paramServices.that)
                }
            })
        },

        switchEvaluableService: function(service) {
            if(service.evaluable) {
                service.updateServiceEvaluable();
            } else {
                paramServices.that.checkDevoirsService(service, () => service.updateServiceEvaluable())
            }
        },

        deleteService: function(service) {
            paramServices.that.checkDevoirsService(service, () => service.deleteService());
        },

        doUpdateOrDelete: function (updateOrDelete, devoirs, service) {
            let id_devoirs = _.pluck(devoirs, "id");
            switch (updateOrDelete){
                case "update": {
                    if(paramServices.that.matiereSelected) {
                        let matiere = paramServices.that.matiereSelected;
                        service.updateDevoirsService(id_devoirs, matiere).then(() => {
                            let nom_matiere = _.findWhere(paramServices.that.columns.matiere.data, {id : matiere}).name;
                            let newService = new paramServices.that.Service({...service.toJson(), id_matiere: matiere,
                                nom_matiere: nom_matiere, evaluable: true});
                            newService.createService();
                            paramServices.that.services.push(newService);
                            paramServices.that.callback();
                            paramServices.that.lightboxes.switchEval = false;
                            utils.safeApply(paramServices.that);
                        });
                    }
                }
                break;
                case "delete" : {
                    service.deleteDevoirsService(id_devoirs).then(() => {
                        paramServices.that.callback();
                        paramServices.that.lightboxes.switchEval = false;
                        utils.safeApply(paramServices.that);
                    });
                }
            }
        },

        setMatiere: function(matiere) {
            paramServices.that.matiereSelected = matiere;
        },

        openCreateLightbox: function () {
            let matiere = _.first(paramServices.that.columns.matiere.data);
            let enseignant = _.first(paramServices.that.columns.enseignant.data);
            let groupe = _.first(paramServices.that.columns.classe.data);
            paramServices.that.service = new paramServices.that.Service(
                {
                    id_etablissement: paramServices.that.idStructure,
                    id_matiere: matiere.id,
                    id_enseignant: enseignant.id,
                    id_groupe: groupe.id,
                    nom_matiere: matiere.name,
                    nom_enseignant: enseignant.displayName,
                    nom_groupe: groupe.name,
                    isManual: true,
                    modalite: 'S',
                    evaluable: true
                });
            paramServices.that.lightboxes.create = true
        },

        createService: function(service) {
            try {
                service.createService();
            } finally {
                paramServices.that.lightboxes.create = false;
                utils.safeApply(paramServices.that)
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