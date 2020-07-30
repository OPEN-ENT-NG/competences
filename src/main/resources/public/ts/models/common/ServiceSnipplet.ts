import {Utils} from "../teacher";
import http from "axios";
import {notify, idiom as lang, angular, _,toasts} from 'entcore';
import {Mix} from "entcore-toolkit";
import {MultiTeaching} from "./MultiTeaching";

export class Service {
    id_etablissement: string;
    id_enseignant: string;
    id_groupe: string;
    id_matiere: string;
    nom_enseignant: object;
    nom_groupe: object;
    topicName: object;
    modalite: string;
    previous_modalite: string;
    evaluable: boolean;
    previous_evaluable: boolean;
    isManual: boolean;
    coefficient: number | string;
    filter:string;
    groups_name:string;
    coTeachers_name : string;
    substituteTeachers_name : string;
    groups:any;
    subTopics: any;
    competencesParams:any;
    id_groupes: any;
    id_groups:any;
    coefficientPlaceHolder: string;
    coTeachers: MultiTeaching[];
    substituteTeachers: MultiTeaching[];
    is_visible: boolean;

    constructor(service) {
        _.extend(this, service);
        this.previous_modalite = this.modalite;
        this.previous_evaluable = this.evaluable;
        this.coefficient = (Utils.isNull(this.coefficient)? 1 : this.coefficient);
        this.groups = service.groups;
        this.groups_name = service.groups_name;
        this.subTopics = service.subTopics;
        this.substituteTeachers = service.substituteTeachers;
        this.coTeachers = service.coTeachers;
        this.is_visible = service.is_visible;
        this.coTeachers_name = service.coTeachers_name;
        this.substituteTeachers_name = service.substituteTeachers_name;

        if(service.competencesParams && service.competencesParams.length != 0){
            let services = [];
            service.competencesParams.forEach(param =>{
                let subService = {};
                subService["id_enseignant"] = this.id_enseignant;
                subService["id_groupe"] = param.id_groupe;
                subService["id_matiere"] = this.id_matiere;
                subService["id_etablissement"] = this.id_etablissement;
                subService["modalite"] = param.modalite;
                subService["evaluable"] = param.evaluable;
                subService["coefficient"] = param.coefficient;
                subService["nom_groupe"] = param.nom_groupe;
                services.push(Mix.castAs(Service,subService));
                if(this.modalite!= param.modalite){
                    this.modalite = lang.translate('multiples');
                }
                if(this.coefficient != param.coefficient){
                    this.coefficient = undefined;
                    this.coefficientPlaceHolder = "Multiples"
                }
            });
            this.competencesParams = services;
        }
    }

    hasAllServicesEvaluable(){
        if(this.hasCompetencesParams() ){
            let hasAllServiceEvaluable = true ;
            this.competencesParams.forEach(param => {
                hasAllServiceEvaluable = hasAllServiceEvaluable &&  param.evaluable;
            });
            return hasAllServiceEvaluable;
        }else
            return this.evaluable;
    }
    hasAllServicesNotEvaluable(){
        if(this.hasCompetencesParams() ){
            let hasAllServiceEvaluable = false ;
            this.competencesParams.forEach(param => {
                hasAllServiceEvaluable = hasAllServiceEvaluable ||  param.evaluable;
            });
            return !hasAllServiceEvaluable;
        }else
            return !this.evaluable;
    }

    hasCompetencesParams() {
        return this.competencesParams && this.competencesParams.length != 0;
    }

    hasVariousEvaluable(){
        return !this.hasAllServicesEvaluable() && !this.hasAllServicesNotEvaluable();
    }
    hasNullProperty(){
        return !this.nom_enseignant || !this.nom_groupe || !this.topicName;
    }

    createService(){
        try {
            http.post('/viescolaire/service', this.toJson());
        } catch (e) {
            toasts.warning('evaluation.service.error.create');
        }
    }

    updateService(){
        try {
            return http.put('/viescolaire/service', this.toJson());
        } catch (e) {
            notify.error('evaluation.service.error.update');
        }
    }

    async updateServices(isModalite?,isCoefficient?){
        try {
            let {status} = await http.put('/viescolaire/services',{"services" :this.competencesParams.map(service =>
                    {
                        if(isModalite)
                            service.modalite = this.modalite;
                        if(isCoefficient)
                            service.coefficient = this.coefficient;
                        return service.toJson()
                    }
                )});
            return status
        } catch (e) {
            toasts.warning('evaluation.service.error.update');
        }
    }
    updateServiceModalite(){
        let request = () => this.updateService();

        if(this.modalite == this.previous_modalite) {
            return;
        } else {
            request().then(() => {
                this.previous_modalite = this.modalite;
            }).catch(() => {
                this.modalite = this.previous_modalite;
            })
        }
    }


    async updateServiceEvaluable() {
        if(this.hasCompetencesParams()){
            this.competencesParams.forEach(service =>{
                service.evaluable = this.evaluable
            });
            await this.updateServices();
        }else {
            let request = () => this.updateService();
            if (this.evaluable == this.previous_evaluable) {
                return;
            } else {
                request().then(() => {
                    this.previous_evaluable = this.evaluable;
                }).catch(() => {
                    this.evaluable = this.previous_evaluable;
                })
            }
        }
    }

    getDifferentEvaluableSubServices(service){
        let diffSubServices = [];
        if(service.hasCompetencesParams() && this.hasCompetencesParams()){
            let  hasSameEvaluables = true;
            this.competencesParams.forEach((s,index)=>{
                if(s.evaluable !== service.competencesParams[index].evaluable)
                    diffSubServices.push(s);
            });
        }
        return diffSubServices;

    }
    hasSameEvaluableSubServices(service){
        if(service.hasCompetencesParams() && this.hasCompetencesParams()){
            let  hasSameEvaluables = true;
            this.competencesParams.forEach((s,index)=>{
                if(s.evaluable !== service.competencesParams[index].evaluable)
                    hasSameEvaluables = false;
            });
            return hasSameEvaluables;
        }
        return  false;
    }
    getDevoirsService(){
        try {
            let url;
            if(this.hasCompetencesParams()) {
                console.log(this.id_groups)
                url = "/competences/devoirs/service" +
                    `?id_matiere=${this.id_matiere}`+
                    `&id_groupe=${this.id_groups.join(",")}`;

                url += `&id_enseignant=${this.id_enseignant}`;
                console.log(url)
            }else{
                url = "/competences/devoirs/service"+
                    `?id_matiere=${this.id_matiere}`+
                    `&id_groupe=${this.id_groupe}`+
                    `&id_enseignant=${this.id_enseignant}`;
            }
            return http.get(url);
        } catch (e) {
            toasts.warning("evaluations.service.devoir.error");
            return  e;
        }
    }

    updateDevoirsService(devoirs, matiere) {
        try {
            return http.put("/competences/devoirs/service", {
                id_devoirs: devoirs,
                id_matiere: matiere
            });
        } catch (e) {
            toasts.warning('evaluations.service.devoir.update.error');
        }
    }

    deleteDevoirsService(devoirs) {
        try {
            return http.put("/competences/devoirs/delete", {
                id_devoirs: devoirs
            });
        } catch (e) {
            toasts.warning('evaluations.service.devoir.delete.error');
        }
    }

    toJson() {
        return {
            id_etablissement: this.id_etablissement,
            id_enseignant: this.id_enseignant,
            id_matiere: this.id_matiere,
            id_groupes: (this.id_groups) ? this.id_groups : [this.id_groupe],
            modalite: this.modalite,
            evaluable: this.evaluable,
            coefficient: this.coefficient,
            is_visible: this.is_visible
        };
    }
}