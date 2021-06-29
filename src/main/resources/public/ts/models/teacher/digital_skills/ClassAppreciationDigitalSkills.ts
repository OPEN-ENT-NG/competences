import {Model, notify} from "entcore";
import http from 'axios';

export class ClassAppreciation extends Model {
    id: number;
    class_or_group_id: string;
    type_structure: string;
    period_type_id: number;
    appreciation: string;

    constructor (o_class, id_type_period ) {
        super();
        this.class_or_group_id = o_class.id ;
        this.type_structure = (o_class.type_groupe === 0)? "D" : "G";
        this.period_type_id = id_type_period;
    }

    toJSON(){
        return {
            id_class: this.class_or_group_id,
            id_type_structure: this.type_structure,
            id_type_period: this.period_type_id,
            appreciation : this.appreciation
        }
    }

    async save () {
        try {
            if (this.appreciation != undefined && this.appreciation.length !== 0){
                let res = await http.post(`/competences/classAppreciationDigitalSkills`,this.toJSON());
                this.id = res.data.id;
            } else if (this.appreciation != undefined && this.appreciation.length === 0 && this.id != undefined){
                let response = await http.delete(`/competences/classAppreciationDigitalSkills?idClassApp=${this.id}`);
                if (response.status === 200) {
                    this.id = undefined;
                }
            }
        }catch(e){
            notify.error('evaluation.class.appreciation.digital.skills.save.error')
            console.log (e);
        }
    }
}