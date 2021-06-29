import {Model, notify} from "entcore";
import http from "axios";

export class StudentAppreciation extends Model {
    id: number;
    student_id: string;
    structure_id: string;
    period_type_id: number;
    appreciation: string;

    constructor (id_student, id_structure, id_type_period) {
        super();
        this.student_id = id_student;
        this.structure_id = id_structure;
        this.period_type_id = id_type_period;
    }

    toJSON(){
        return {
            id_student: this.student_id,
            id_structure: this.structure_id,
            id_type_period: this.period_type_id,
            appreciation : this.appreciation
        }
    }

    async save () {
        try {
            if (this.appreciation != undefined && this.appreciation.length !== 0){
               let data = await http.post(`/competences/studentAppreciationDigitalSkills`, this.toJSON())
                this.id = data.data.id;
            } else if (this.appreciation != undefined && this.appreciation.length === 0 && this != undefined){
                let response = await http.delete(`/competences/studentAppreciationDigitalSkills?idStudentApp=${this.id}`);
                if (response.status === 200) {
                    this.id = undefined;
                }
            }
        }catch(e){
            notify.error('evaluation.student.appreciation.digital.skills.save.error')
            console.log (e);
        }
    }
}