import {Model, notify,_} from "entcore";
import {ClassAppreciation} from "./ClassAppreciationDigitalSkills";
import {StudentAppreciation} from "./StudentAppreciationDigitalSkills";

import {Classe} from "../Classe";
import {Eleve} from "../Eleve";
import http from "axios";
import {Structure} from "../Structure";
import {Mix} from "entcore-toolkit";
import {EvaluatedDigitalSkills} from "./EvaluatedDigitalSkills";
import {DomaineDigitalSkills} from "./DomaineDigitalSkills";

export class DigitalSkills extends Model {
    id_structure: string;
    class: Classe;
    student: Eleve;
    classAppreciation: ClassAppreciation;
    studentAppreciation: StudentAppreciation;
    digitalSkillsByDomaine: Array<DomaineDigitalSkills>;

    constructor(o : any, structure : Structure) {
        super();
        this.class = o.classe;
        this.student = o.eleve;
        this.id_structure = structure.id;
        this.studentAppreciation = new StudentAppreciation(o.eleve.id, structure.id);
        this.classAppreciation = new ClassAppreciation(o.classe);
        this.digitalSkillsByDomaine = [];
    }

    async sync() {
        try{
            let url = `competences/digitalSkills/appreciations/evaluation?idStructure=${this.id_structure}`;
            url += `&idStudent=${this.student.id}&idClass=${this.class.id}`;
            let data = await http.get(url);
            if(data.status === 200) {
                if(DigitalSkills.hasData(data.data.studentAppreciation)){
                    this.studentAppreciation = Mix.castAs(StudentAppreciation, data.data.studentAppreciation);
                }

                if(DigitalSkills.hasData(data.data.classAppreciation)){
                    this.classAppreciation = Mix.castAs(ClassAppreciation, data.data.classAppreciation);
                }

                await this.syncAllDigitalSkills();
                if(DigitalSkills.hasData(data.data.evaluatedDigitalSkills)){
                    _.forEach(data.data.evaluatedDigitalSkills, (line) => {
                        _.forEach(this.digitalSkillsByDomaine, (domaine) => {
                            let digitalSkill = _.find(domaine.digitalSkills, {digital_skill_id: line.id_digital_skill});
                            if(digitalSkill != undefined) {
                                digitalSkill.level = line.level;
                                digitalSkill.id = line.id;
                            }
                        });
                    });
                }
            }
        }catch (e) {
            notify.error("evaluation.digital.skills.sync.error");
        }
    }

    async syncAllDigitalSkills(){
        try{
            let data = await http.get(`competences/digitalSkills?idStructure=${this.id_structure}`);
            if(data.status === 200) {
                _.forEach(data.data, (line) => {
                    let domaineDigitalSkill = _.find(this.digitalSkillsByDomaine, {id: line.id_domaine});

                    if(domaineDigitalSkill === undefined){
                        domaineDigitalSkill = new DomaineDigitalSkills(line.id_domaine, line.libelle_domaine);
                        this.digitalSkillsByDomaine.push(domaineDigitalSkill);
                    }

                    let digitalSkill = new EvaluatedDigitalSkills(line.id_digital_skill, this.student.id,
                        this.id_structure, 0, line.libelle);
                    domaineDigitalSkill.digitalSkills.push(digitalSkill);
                });
            }
        }catch (e) {
            notify.error("evaluation.digital.skills.sync.error");
        }
    }

    private static hasData (data : object) : boolean {
        return !_.isEmpty(data);
    }
}