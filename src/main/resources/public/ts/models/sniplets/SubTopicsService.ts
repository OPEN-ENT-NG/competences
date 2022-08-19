import {Mix, Selectable, Selection} from "entcore-toolkit";
import http from "axios";
import {Model} from "entcore";
import {Classe} from "../teacher";
import {SubTopicsServiceService} from "../../services/SubTopicServiceService";

let subTopicsServiceService = new SubTopicsServiceService();


export class SubTopicsService implements Selectable{
    libelle: string;
    selected: boolean;
    id_structure: string;
    id_teacher: string ;
    id_group : string ;
    id_topic: string ;
    id_subtopic:string;
    coefficient : number ;
    groups : Classe[];

    toJson () {
        return {
            coefficient : this.coefficient,
            id_teacher : this.id_teacher,
            id_group : this.id_group,
            id_topic : this.id_topic,
            id_subtopic: this.id_subtopic,
            id_structure: this.id_structure,
            groups: this.groups
        }
    }
    build(data) {
        this.libelle = data.libelle;
        this.selected = false;
        this.id_structure = data.id_structure
        this.id_teacher = data.id_teacher;
        this.id_group = data.id_group;
        this.id_topic = data.id_topic;
        this.id_subtopic = data.id_subtopic;
        this.coefficient = (parseFloat(data.coefficient)) ? parseFloat(data.coefficient) : 1;
        return this;
    }
}

export class SubTopicsServices extends Selection<SubTopicsService> {
    async get(idStructure) {
        let {data} = await subTopicsServiceService.get(idStructure);
        data.forEach((item) => {
            let sts =  new SubTopicsService();
            this.all.push(sts.build(item))
        })
    }
}