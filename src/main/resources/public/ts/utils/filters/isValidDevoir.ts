import {_, model, moment} from "entcore";
import {Utils} from "../../models/teacher";

export function isValidDevoir (idClasse, id_matiere, classes) {
    if (classes) {
        let classe = _.findWhere(classes, {id: idClasse});
        //sinon on regarde s'il enseigne sur cette classe ou s'il est coTeacher ou encore remplaçant sur la bonne période
        if (classe && classe.services){
            if(Utils.isChefEtab(classe)){
                return true;
            } else {
                let evaluables = _.filter(classe.services, service => {
                    let substituteTeacher = _.findWhere(service.substituteTeachers, {second_teacher_id : model.me.userId});
                    let correctDateSubstituteTeacher = substituteTeacher &&
                        moment(new Date()).isBetween(moment(substituteTeacher.start_date),
                            moment(substituteTeacher.entered_end_date), 'days', '[]');
                    let coTeachers = _.findWhere(service.coTeachers, {second_teacher_id: model.me.userId});
                    let mainTeacher = service.id_enseignant == model.me.userId;
                    if(id_matiere){
                        correctDateSubstituteTeacher = correctDateSubstituteTeacher &&
                            substituteTeacher.subject_id == id_matiere;
                        coTeachers = _.findWhere(service.coTeachers,
                            {second_teacher_id: model.me.userId, subject_id : id_matiere});
                        mainTeacher = mainTeacher && service.id_matiere == id_matiere;
                    }
                    return coTeachers || correctDateSubstituteTeacher || mainTeacher;
                });
                return evaluables.length > 0;
            }
        }
    }
    return false;
}