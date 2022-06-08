import {notify, idiom as lang, _, toasts, $, moment} from 'entcore';
import http from "axios";
import * as utils from '../utils/teacher';
import {Classe, TypeSousMatieres} from "../models/teacher";
import {Service} from "../models/common/ServiceSnipplet";
import {safeApply} from "../utils/teacher";
import {MultiTeaching} from "../models/common/MultiTeaching";
import {SubTopicsService, SubTopicsServices} from "../models/sniplets";
import {SubTopicsServiceService} from "../services/SubTopicServiceService";

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
            this.searchForClasse = [];
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

            this.subTopicsServices = new SubTopicsServices([])
            this.subTopicsServiceService = new SubTopicsServiceService();
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
                    await paramServices.that.setServicesWithGroups(aService.data);

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
        stopMessageLoader: async function () {
            paramServices.that.showServicesLoader = false;
            await utils.safeApply(paramServices.that);
        },

        saveSearch: async (event) => {
            if (event && (event.which === 13 || event.keyCode === 13) && event.target.value.length > 0) {
                let value = event.target.value.trim().toUpperCase();
                let searchAdded = false;
                paramServices.that.services.forEach(service => {
                    if(!searchAdded) {
                        if(service.groups_name != null && service.groups_name.toUpperCase().includes(value) ||
                            service.nom_groupe != null && service.nom_groupe.toUpperCase().includes(value)){
                            if(!_.contains(paramServices.that.searchForClasse, value)){
                                paramServices.that.searchForClasse.push(value);
                                searchAdded = true;
                            }
                        }
                    }
                });
                if (!searchAdded && !_.contains(paramServices.that.searchToFilter, value)){
                    paramServices.that.searchToFilter.push(value);
                }
                await paramServices.that.initServices();
                event.target.value = '';
            }
        },

        dropSearchFilter: async (search) => {
            paramServices.that.searchToFilter = _.without(paramServices.that.searchToFilter, search);
            await paramServices.that.initServices();
        },

        dropSearchClass: async (search) => {
            paramServices.that.searchForClasse = _.without(paramServices.that.searchForClasse, search);
            await paramServices.that.initServices();
        },

        filterSearch: () => {
            return (service) => {
                let isInClassSearched = false;
                let isInSearched = true;
                if(paramServices.that.searchToFilter.length > 0) {
                    for(let search of paramServices.that.searchToFilter) {
                        isInSearched = (service.nom_enseignant.toUpperCase().includes(search.toUpperCase())
                            || service.topicName.toUpperCase().includes(search.toUpperCase())
                            || service.coTeachers_name.toUpperCase().includes(search.toUpperCase())
                            || service.substituteTeachers_name.toUpperCase().includes(search.toUpperCase()));
                        if(!isInSearched)
                            break;
                    }
                }
                if(paramServices.that.searchForClasse.length > 0) {
                    let classesSearched = [];
                    for(let search of paramServices.that.searchForClasse) {
                        service.groups.forEach(group => {
                            if(group.name.toUpperCase().includes(search.toUpperCase())
                                && !_.contains(classesSearched, group)){
                                classesSearched.push(group);
                                isInClassSearched = true;
                            }
                        });
                    }
                    if(classesSearched.length > 0) {
                        service.id_groups = classesSearched.map(classe => classe.id);
                        service.competencesParams = _.filter(service.competencesParams, param => {
                            return _.contains(service.id_groups, param.id_groupe);
                        });
                        service.groups = [];
                        service.groups_name = paramServices.that.getGroupsName(service, service.groups);
                    }
                }
                else {
                    isInClassSearched = true;
                }
                return isInClassSearched && isInSearched;
            }
        },

        getGroupsName: function (service, groups) {
            if(service.competencesParams && service.competencesParams.length > 0){
                service.competencesParams.forEach(param => {
                    let group =  _.findWhere(paramServices.that.columns.classe.data, {id: param.id_groupe});
                    if(group && !groups.includes(group)){
                        groups.push(group);
                        param.nom_groupe = group.name;
                    }
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
                return groups.join(", ");
            }
            else {
                let group = _.findWhere(paramServices.that.columns.classe.data, {id: service.id_groupe});
                if(group && !groups.includes(group)){
                    groups.push(group);
                    return group.name;
                }
            }
            return null;
        },
        updateCoeffSubTopics: async function(subtopic){
            await paramServices.that.subTopicsServiceService.set(subtopic);
        }
        ,
        setServicesWithGroups: async function (data) {
            await paramServices.that.subTopicsServices.get(paramServices.that.idStructure)
            await paramServices.that.subTopics.get(paramServices.that.idStructure);
            paramServices.that.services = _.reject(_.map(data, service => {
                let enseignant = _.findWhere(paramServices.that.columns.enseignant.data, {id: service.id_enseignant});
                let groupe = _.findWhere(paramServices.that.columns.classe.data, {id: service.id_groupe});
                let groups = [];
                let subTopics = [];
                let coTeachers = service.coTeachers;
                let substituteTeachers = service.substituteTeachers;
                let matiere = _.findWhere(paramServices.that.columns.matiere.data, {id: service.id_matiere});
                if (matiere && matiere.sous_matieres && matiere.sous_matieres.length > 0)
                    matiere.sous_matieres.forEach(sm => {
                        paramServices.that.subTopics.all.map(sb => {
                            let sbt =  paramServices.that.subTopicsServices.all.find(subTopicsService =>{
                                return subTopicsService.id_teacher === service.id_enseignant
                                    && subTopicsService.id_topic === service.id_matiere
                                    && subTopicsService.id_group === service.id_groupe
                                    && subTopicsService.id_subtopic === sm.id_type_sousmatiere
                            });
                            if (sm.id_type_sousmatiere == sb.id) {
                                if(sbt !== undefined){
                                    sbt.libelle =   sb.libelle ;
                                 }else{
                                    sbt = new SubTopicsService()
                                    sbt.libelle =   sb.libelle ;
                                    sbt.id_teacher = service.id_enseignant ;
                                    sbt.id_group = service.id_groupe ;
                                    sbt.groups = service.groups ;
                                    sbt.id_topic = service.id_matiere ;
                                    sbt.id_subtopic = sm.id_type_sousmatiere
                                    sbt.id_structure = paramServices.that.idStructure;
                                    sbt.coefficient = 1 ;
                                }
                                subTopics.push(sbt)
                            }
                        });
                    });
                let groups_name = paramServices.that.getGroupsName(service, groups);

                let coTeachers_name = "";
                let substituteTeachers_name = "";

                if(coTeachers){
                    _.each(coTeachers , (coTeacher) => {
                        let coT = _.findWhere(paramServices.that.columns.enseignant.data,
                            {id: coTeacher.second_teacher_id});
                        if(coT != undefined) {
                            coTeacher.displayName = coT.displayName;
                            coTeachers_name += coTeacher.displayName + " ";
                        } else {
                            coTeacher.displayName = "";
                        }
                    });
                }
                if(substituteTeachers){
                    _.each(substituteTeachers , (substituteTeacher) => {
                        let subT = _.findWhere(paramServices.that.columns.enseignant.data,
                            {id: substituteTeacher.second_teacher_id});
                        if(subT != undefined) {
                            substituteTeacher.displayName = subT.displayName;
                            substituteTeachers_name += substituteTeacher.displayName + " ";
                        } else {
                            substituteTeacher.displayName = "";
                        }
                    });
                }

                let missingParams = {
                    id_etablissement: paramServices.that.idStructure,
                    nom_enseignant: enseignant ? enseignant.displayName : null,
                    topicName: matiere ? matiere.name + " (" + matiere.externalId + " - " + matiere.source + ")" : null,
                    groups: groups ? groups : null,
                    groups_name: groups_name ? groups_name : "",
                    coTeachers_name : coTeachers_name,
                    substituteTeachers_name : substituteTeachers_name,
                    nom_groupe: groupe ? groupe.name : null,
                    subTopics: subTopics ? subTopics : [],
                    coTeachers: (_.isEmpty(coTeachers)) ? [] : _.map(coTeachers, (coTeacher) => { return new MultiTeaching(coTeacher)}),
                    substituteTeachers:  (_.isEmpty(substituteTeachers)) ? [] : _.map(substituteTeachers, (substituteTeacher) => { return new MultiTeaching(substituteTeacher)})
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
                paramServices.that.filter = "";
                filtersArray.forEach(filter =>{
                    paramServices.that.filter += filter.filterName + "=" + filter.isSelected + "&";
                })
            }
            await paramServices.that.setServices();
        },

        checkIfExistsAndValid: function (service) {
            let exist = false;
            if(paramServices.that.classesSelected && paramServices.that.classesSelected.length > 0 && service.id_matiere != "" && service.id_enseignant != "") {
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
            await service.getDevoirsService()
                .then(async ({data}) => {
                    if (data.length === 0) {
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
            safeApply(paramServices.that);
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
        switchEvaluableService: async function(service) {
            if(!service.evaluable || service.hasAllServicesNotEvaluable() || service.hasVariousEvaluable()) {
                service.evaluable = true;
                await service.updateServiceEvaluable();
            } else {
                console.log(service);
                await paramServices.that.checkDevoirsService(service, async () => {
                    service.evaluable = false;
                    await service.updateServiceEvaluable();
                });
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
        },
        handleUpdateDevoirs: function (service, id_devoirs, matiere) {
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
        },

        doUpdateOrDelete: function (updateOrDelete, devoirs, service) {
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
        validForm: async function (serviceToUpdate) {
            paramServices.that.lightboxes.update = false;
            await serviceToUpdate.updateServices();
            await paramServices.that.setServices();
        },
        updateServices: async function(){
            let oldService = paramServices.that.oldService;
            let serviceToUpdate = paramServices.that.serviceToUpdate;
            if(!serviceToUpdate.hasSameEvaluableSubServices(oldService)){
                let servicesToCheck = serviceToUpdate.getDifferentEvaluableSubServices(oldService)
                let service = new Service(serviceToUpdate);
                service.competencesParams = servicesToCheck;
                service.id_groups = _.pluck(servicesToCheck, 'id_groupe');
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

        filterValidDateSubstituteTeacher: function(substituteTeacher) {
            return moment(new Date()).isBetween(moment(substituteTeacher.start_date),
                moment(substituteTeacher.entered_end_date), 'days', '[]');
        },

        canSwitchServiceVisibility: function(service) {
            if(!service.is_visible){
                return false;
            }
            else {
                let multiTeachersVisible = _.where(service.coTeachers, {is_visible : true})
                    .concat(_.filter(service.substituteTeachers, (substituteTeacher) => {
                        return substituteTeacher.is_visible === true
                            && this.filterValidDateSubstituteTeacher(substituteTeacher);
                    }));
                return multiTeachersVisible.length === 0;
            }
        },

        canSwitchMultiTeacherVisibility: function(service, multiTeacher) {
            if(!multiTeacher.is_visible){
                return false;
            }
            else {
                let multiTeachersVisible = _.where(service.coTeachers, {is_visible : true})
                    .concat(_.filter(service.substituteTeachers, (substituteTeacher) => {
                        return substituteTeacher.is_visible === true
                            && this.filterValidDateSubstituteTeacher(substituteTeacher);
                    }));
                multiTeachersVisible = _.filter(multiTeachersVisible, (mulT) =>
                    mulT.second_teacher_id !== multiTeacher.second_teacher_id);
                return !(service.is_visible || multiTeachersVisible.length > 0);
            }
        },

        switchServiceVisibility: function(service) {
            if(!this.canSwitchServiceVisibility(service)){
                service.is_visible = !service.is_visible;
                service.updateService();
            }
        },

        switchMultiTeacherVisibility: function(service, multiTeacher) {
            if(!this.canSwitchMultiTeacherVisibility(service, multiTeacher)){
                multiTeacher.is_visible = !multiTeacher.is_visible;
                new MultiTeaching(multiTeacher).updateMultiTeaching();
            }
        },
    }
}