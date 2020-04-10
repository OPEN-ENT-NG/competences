import {notify, idiom as lang, angular, _,toasts} from 'entcore';
import http from "axios";
import * as utils from '../utils/teacher';
import {Classe, TypeSousMatiere, TypeSousMatieres, Utils} from "../models/teacher";
import {Service} from "../models/common/ServiceSnipplet";
import {safeApply} from "../utils/teacher";

export const paramServices = {
    title: 'Configuration des services',
    description: 'Permet le parametrage des services',
    that: undefined,
    controller: {

        initServices: async function () {
            await paramServices.that.setServices();
            paramServices.that.classesSelected = [];
        },

        init: async function () {
            console.log(" ParamServices");
            this.idStructure = this.source.idStructure;
            this.services = [];
            this.searchToFilter = [];
            this.matiereSelected = "";
            this.sortBy = "topicName";
            this.sortByAsc = true;
            this.headers = {
                evaluable: {name:"evaluation.service.headers.evaluable", filterName:"evaluable", value: true, isSelected: true},//a voir si ca bug
                notEvaluable: {name:"evaluation.service.headers.notEvaluable", filterName:"notEvaluable", value: false, isSelected: false}
            };

            this.typeGroupes= {
                classes: {isSelected: true, filterName:"classes", name: "evaluation.service.filter.classes", type: 0},
                groupes: {isSelected: true, filterName:"groups", name: "evaluation.service.filter.groupes", type: 1},
                manualGroupes : {isSelected: true, filterName:"manualGroups", name:"evaluation.service.filter.manualGroupes", type:2}
            };

            this.subTopics = new TypeSousMatieres([]);
            this.columns = {
                matiere: {size: "two", data: [], name: "evaluation.service.columns.matiere", filtered: true},
                classe: {size:"two", data: [], name: "evaluation.service.columns.classGroup", filtered: false},
                enseignant: {size: "two", data: [], name: "evaluation.service.columns.teacher", filtered: false},
                remplacement: {size: "two", data: [], name: "evaluation.service.columns.remplacement", filtered: false},
                modalite: {size: "one", data: [], name: "evaluation.service.columns.modalite", filtered: false},
                coefficient: {size: "one", name: "viescolaire.utils.coefficient", filtered: false},
                evaluable: {size: "one", name: "evaluation.service.columns.evaluable", filtered: false}
            };

            this.lightboxes = {
                switchEval: false,
                confirm: false,
                update: false,
                subEducationCreate:false,
                switchEvaluation:false
            };
            paramServices.that = this;
            await this.runMessageLoader();
            Promise.all([this.getClasses(), this.getMatieres(), this.getTeachers(), this.getModalite(),  this.getServices()])
                .then(async function([aClasses, aMatieres, aTeachers, aModalite, aService]) {
                    paramServices.that.columns.classe.data = _.map(aClasses.data,
                        (classe) => {
                            classe.type_groupe_libelle = Classe.get_type_groupe_libelle(classe);
                            classe = new Classe(classe);
                            return classe;
                        });
                    paramServices.that.columns.enseignant.data = _.map(aTeachers.data, teacher => teacher.u.data);
                    paramServices.that.columns.matiere.data = aMatieres.data;
                    paramServices.that.columns.modalite.data = _.pluck(aModalite.data, "id");
                    paramServices.that.columns.modalite.data.unshift(lang.translate('multiples'));
                    // paramServices.that.remplacements = aRemplacement.data;
                    await  paramServices.that.setServicesWithGroups(aService.data);

                    // await paramServices.that.initServices();
                });
            this.classesSelected = [];
        },

        translate: function(key) {
            return lang.translate(key);
        },
        runMessageLoader: async function () {
            paramServices.that.showServicesLoader = true;
            await utils.safeApply(paramServices.that);
        },
        stopMessageLoader: async function ( ) {
            paramServices.that.showServicesLoader = false;
            await utils.safeApply(paramServices.that);
        },
        saveSearch :function async (event) {
            if (event && (event.which === 13 || event.keyCode === 13 )) {
                if (!_.contains(paramServices.that.searchToFilter, event.target.value)
                    && event.target.value.length > 0
                    && event.target.value.trim()){
                    paramServices.that.searchToFilter.push(event.target.value);
                }
                event.target.value = '';
            }
        },
        dropSearchFilter: function(search) {
            paramServices.that.searchToFilter = _.without(paramServices.that.searchToFilter, search);
        },
        filterSearch: function () {
            return (service) => {
                let isInSearched = true;
                if(paramServices.that.searchToFilter.length !=0){
                    paramServices.that.searchToFilter.forEach(search =>{
                        if( !(service.nom_groupe.toUpperCase().includes(search.toUpperCase())
                            || service.nom_enseignant.toUpperCase().includes(search.toUpperCase())
                            || service.topicName.toUpperCase().includes(search.toUpperCase()))){
                            isInSearched = false;
                        }
                    });
                }else{
                    isInSearched = true;

                }
                return isInSearched;
            }
        },

        setServicesWithGroups: async function (data) {
            function getGroupsName(service, groups, groups_name: string) {
                if(service.competencesParams && service.competencesParams.length !== 0)
                    service.competencesParams.forEach(param => {
                        let group =  _.findWhere(paramServices.that.columns.classe.data, {id: param.id_groupe});
                        groups.push(group);
                        param.nom_groupe = group.name;
                    });
                groups.sort((group1, group2) => {
                    if (group1.name > group2.name) {
                        return 1;
                    }
                    if (group1.name < group2.name) {
                        return -1;
                    }
                    return 0;
                });
                groups_name = groups.join(",")
                return groups_name;
            }

            await paramServices.that.subTopics.get();
            paramServices.that.services = _.reject(_.map(data, service => {
                let enseignant = _.findWhere(paramServices.that.columns.enseignant.data, {id: service.id_enseignant});
                let groupe = _.findWhere(paramServices.that.columns.classe.data, {id: service.id_groupe});
                let groups = [];
                let subTopics = [];
                let matiere = _.findWhere(paramServices.that.columns.matiere.data, {id: service.id_matiere});
                if (matiere && matiere.sous_matieres && matiere.sous_matieres.length > 0)
                    matiere.sous_matieres.forEach(sm => {
                        paramServices.that.subTopics.all.map(sb => {
                            if (sm.id_type_sousmatiere == sb.id) {
                                subTopics.push(sb)
                            }
                        });
                    });
                let groups_name = "";

                groups_name = getGroupsName(service, groups, groups_name);
                let missingParams = {
                    id_etablissement: paramServices.that.idStructure,
                    nom_enseignant: enseignant ? enseignant.displayName : null,
                    topicName: matiere ? matiere.name + " (" + matiere.externalId + ")" : null,
                    groups: groups ? groups : null,
                    groups_name: groups_name ? groups_name : null,
                    nom_groupe: groupe ? groupe.name : null,
                    subTopics: subTopics ? subTopics : []
                };

                return new Service(_.defaults(service, missingParams));
            }), service => service.hasNullProperty());
            await paramServices.that.stopMessageLoader();
        },

        setServices : async () => {
            if( !paramServices.that.showServicesLoader) {
                await paramServices.that.runMessageLoader();
            }


            paramServices.that.getServices().then(async ({data}) => {
                await paramServices.that.setServicesWithGroups(data);
            })
            /* .catch( async (error) => {
                 console.error(error);
                 await paramServices.that.stopMessageLoader();
             });*/
        },

        updateFilter: async (selectedHeader) => {

            selectedHeader.isSelected = !selectedHeader.isSelected;
            let filtersArray = _.values(paramServices.that.typeGroupes).concat(_.values(paramServices.that.headers));
            if(filtersArray.length != 0 ){
                paramServices.that.filter= "";
                filtersArray.forEach(filter =>{
                    paramServices.that.filter += filter.filterName + "=" + filter.isSelected + "&";
                })
            }
            await paramServices.that.setServices();
        },


        checkIfExistsAndValid: function (service) {
            let exist = false;
            if(paramServices.that.classesSelected && paramServices.that.classesSelected.length>0 && service.id_matiere != "" && service.id_enseignant != "") {
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
        deploySubtopics:function (service) {
            if(service.subTopics.length != 0)
                service.deploy = !service.deploy;
        },
        checkDevoirsService: async function (service, callback) {

            service.getDevoirsService().then(async function ({data}) {
                if (data.length == 0) {
                    await callback();
                } else {
                    paramServices.that.service = service;
                    paramServices.that.devoirs = data;
                    console.log("true form  ");
                    console.log(paramServices.that.devoirs);

                    paramServices.that.callback = callback;
                    paramServices.that.error = paramServices.that.translate("evaluations.service.devoir.error").replace("[nbDevoir]", paramServices.that.devoirs.length);
                    paramServices.that.lightboxes.switchEval = true;
                    await utils.safeApply(paramServices.that);
                }
            })

        },
        openSwitchEvaluation:function() {
            paramServices.that.lightboxes.subEducationCreate = false;
            paramServices.that.matieresWithoutSubTopic =[];
            paramServices.that.matieres.forEach(matiere =>{
                if(matiere.selected && matiere.sous_matieres.length === 0){
                    paramServices.that.matieresWithoutSubTopic.push(matiere)
                }
            });
            paramServices.that.defaultSubTopic = paramServices.that.subTopics.all.find(subtopic => subtopic.selected)

            paramServices.that.lightboxes.switchEvaluation = true;
            safeApply(paramServices.that);
        },
        switchEvaluableService: async function(service,isVarious?) {
            if(isVarious){
                service.evaluable = true;
            }else{
                service.evaluable = !service.evaluable;
            }
            if(service.hasAllServicesNotEvaluable() || service.hasVariousEvaluable()) {
                await service.updateServiceEvaluable();
            } else {
                await paramServices.that.checkDevoirsService(service, () => service.updateServiceEvaluable());
            }
        },
        changeSort:function (nameSort)  {
            if(paramServices.that.sortBy === nameSort)
                paramServices.that.sortByAsc = !paramServices.that.sortByAsc;
            else
                paramServices.that.sortByAsc = true;
            paramServices.that.sortBy = nameSort;

        },
        addNewService: async function (service, matiere, topicName) {
            let newService = new Service({
                ...service.toJson(),
                id_matiere: matiere, topicName: topicName, evaluable: true
            });
            await newService.createService();
            paramServices.that.services.push(newService);
        }, handleUpdateDevoirs: function (service, id_devoirs, matiere) {
            if(paramServices.that.matiereSelected) {
                let matiere = paramServices.that.matiereSelected;
                if(matiere === service.id_matiere){
                    notify.info('evaluation.service.choose.another.subject');
                }
                service.updateDevoirsService(id_devoirs, matiere).then(async () => {
                    let topicName = _.findWhere(paramServices.that.columns.matiere.data, {id : matiere}).name;
                    toasts.info("service.homework.update.topic");
                    if(service.hasCompetencesParams()) {
                        service.id_groupes = [];
                        service.id_groupe = undefined;
                        service.competencesParams.forEach(s => {
                            service.id_groupes.push(s.id_groupe);
                        });
                    }
                    await this.addNewService(service, matiere, topicName);
                    await paramServices.that.callback();
                    paramServices.that.lightboxes.switchEval = false;
                    await paramServices.that.initServices();
                });
            }
            else {
                notify.info('evaluation.service.choose.a.subject');
            }
        },
        handleDeleteDevoirs: function (service, id_devoirs) {
            {
                service.deleteDevoirsService(id_devoirs).then(async () => {
                    try {
                        await paramServices.that.callback();
                        paramServices.that.lightboxes.switchEval = false;
                        toasts.confirm('evaluation.service.delete');
                        await paramServices.that.initServices();
                    } catch (e) {
                        console.error(e);
                        toasts.warning('evaluation.service.delete.error');
                    }
                });
            }
        }, doUpdateOrDelete: function (updateOrDelete, devoirs, service) {
            let id_devoirs = _.pluck(devoirs, "id");
            switch (updateOrDelete){
                case "update":
                    this.handleUpdateDevoirs(service, id_devoirs);
                    break;
                case "delete" :
                    this.handleDeleteDevoirs(service, id_devoirs);
                    break;
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

        openUpdateLightbox: function (service) {
            paramServices.that.oldService = service;
            paramServices.that.serviceToUpdate = new Service(service);
            paramServices.that.lightboxes.update = true;

        },
        validForm: function (serviceToUpdate) {
            paramServices.that.lightboxes.update = false;
            serviceToUpdate.updateServices()
            paramServices.that.setServices();
        },
        updateServices: async function(){
            let oldService = paramServices.that.oldService;
            let serviceToUpdate = paramServices.that.serviceToUpdate;

            if(!serviceToUpdate.hasSameEvaluableSubServices(oldService)){
                let servicesToCheck = serviceToUpdate.getDifferentEvaluableSubServices(oldService)
                console.log(servicesToCheck);
                let service = new Service(serviceToUpdate);
                service.competencesParams = servicesToCheck;
                await paramServices.that.checkDevoirsService(service, () => {
                    this.validForm(serviceToUpdate);
                })
            }else{
                this.validForm(serviceToUpdate);
            }
        },
        getServices: function () {
            try {
                if(!paramServices.that.filter)
                    paramServices.that.filter = "classes=true&groups=true&manualGroups=true&evaluable=true&notEvaluable=false";
                return http.get(`/viescolaire/services?idEtablissement=${paramServices.that.idStructure}&${paramServices.that.filter}`);

            } catch (e) {
                toasts.warning('evaluation.service.error.get');
            }
        },

        getClasses: function () {
            try {
                return http.get(`/viescolaire/classes?idEtablissement=${
                    paramServices.that.idStructure}&forAdmin=true`)
            } catch (e) {
                toasts.warning('evaluations.service.error.classe');
            }
        },

        getMatieres: function () {
            try {
                return http.get(`/viescolaire/matieres?idEtablissement=${paramServices.that.idStructure}`)
            } catch (e) {
                toasts.warning('evaluations.service.error.matiere');
            }
        },

        getModalite: function() {
            try {
                return http.get("/competences/modalites")
            } catch (e) {
                toasts.warning('evaluations.service.error.classe');
            }
        },

        getTeachers: function() {
            try {
                return http.get(`/viescolaire/teachers?idEtablissement=${paramServices.that.idStructure}`)
            } catch (e) {
                toasts.warning('evaluations.service.error.teacher');
            }
        },

        getRemplacements: function() {
            try {
                return http.get(`/competences/remplacements/list`)
            } catch (e) {
                toasts.warning('evaluations.service.error.remplacement');
            }
        }
    }
}