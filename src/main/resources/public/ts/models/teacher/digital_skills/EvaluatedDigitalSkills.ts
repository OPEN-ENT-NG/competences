import {Model, notify} from "entcore";
import http from "axios";

export class EvaluatedDigitalSkills extends Model {
    id: number;
    digital_skill_id: number;
    student_id: string;
    structure_id: string;
    level: number;
    libelle: string;

    constructor (id_digital_skill, id_student, id_structure, level, libelle) {
        super();
        this.digital_skill_id = id_digital_skill;
        this.student_id = id_student;
        this.structure_id = id_structure;
        this.level = level;
        this.libelle = libelle;
    }

    toJSON(){
        return {
            id_digital_skill: this.digital_skill_id,
            id_student: this.student_id,
            id_structure: this.structure_id,
            level: this.level
        }
    }

    async save () {
        try {
            if (this.level !== 0){
                let data = await http.post(`/competences/digitalSkills`, this.toJSON())
                this.id = data.data.id;
            } else {
                let response = await http.delete(`/competences/digitalSkills?idDigSkill=${this.id}`);
                if (response.status === 200) {
                    this.id = undefined;
                }
            }
        }catch(e){
            notify.error('evaluation.digital.skills.save.error')
            console.log (e);
        }
    }

    onLevelChange () {
        this.level = parseInt(String(this.level));
        this.save();
    }
}