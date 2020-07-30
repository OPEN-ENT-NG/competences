import {moment, notify, _} from "entcore";
import http from "axios";


export class MultiTeaching {

    structure_id: string;
    main_teacher_id: string;
    second_teacher_id: string;
    subject_id: string;
    idsAndIdsGroups: any[];
    displayName: string;
    start_date: Date;
    end_date: Date;
    entered_end_date: Date;
    isCoteaching: boolean;
    is_visible: boolean;

    constructor(o){
        this.structure_id = o.structure_id;
        this.main_teacher_id = o.main_teacher_id;
        this.subject_id = o.subject_id;
        this.isCoteaching = o.isCoteaching;
        if(o.idsAndIdsGroups &&  o.second_teacher_id && o.displayName) {
            this.idsAndIdsGroups = o.idsAndIdsGroups;
            this.second_teacher_id = o.second_teacher_id;
            this.displayName = o.displayName;
        }
        if(!o.is_coteaching){
            if(o.start_date !=null && o.start_date != undefined){
                this.start_date = new Date (o.start_date);
                this.end_date = new Date (o.end_date);
                this.entered_end_date = new Date(o.entered_end_date);
            }else{
                this.start_date = new Date();
                this.end_date = moment(new Date()).add(1, 'day');
                this.entered_end_date = this.end_date;
            }
        }
        this.is_visible = o.is_visible;
    }

    updateMultiTeaching(){
        try {
            return http.put('/viescolaire/multiteaching/update_visibility', this.toJson());
        } catch (e) {
            notify.error('evaluation.service.error.update');
        }
    }

    toJson(){
        return{
            structure_id: this.structure_id,
            main_teacher_id: this.main_teacher_id,
            second_teacher_ids: this.second_teacher_id,
            subject_id: this.subject_id,
            class_or_group_ids: _.pluck(this.idsAndIdsGroups, "idGroup"),
            is_visible: this.is_visible
        }
    }
}