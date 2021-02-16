import {Enseignant, Classe, Utils} from "../../models/teacher";
import {_, moment} from "entcore";
import {Periode} from "../../models/common/Periode";

/**
 * @param {string} schoolClassId
 * @param  {Array<Classe>} schoolClasses
 * @param teachers {Array<Enseignant>}
 * @return { {idSubject:Enseignant}[] }
 */

export const getTeacherBySubject:Function = (schoolClasses:Array<any>, schoolClassId:string, teachers:Array<Enseignant>,
                                             periode : Periode) : {idSubject : Enseignant}[] => {
    const currentClass = _.chain(schoolClasses).findWhere({id: schoolClassId}).value();
    const teacherBySubject:{} = {};

    if(currentClass && currentClass.services){
        _.where(currentClass.services, {evaluable : true}).forEach(item => {
            if(item && item.id_matiere && item.id_enseignant){
                let teacher = _.findWhere(teachers, {id : item.id_enseignant})

                if(teacher != undefined){
                    if(!teacherBySubject[item.id_matiere]) {
                        teacherBySubject[item.id_matiere] = teacher;
                    }

                    teacherBySubject[item.id_matiere].coTeachers = [];
                    teacherBySubject[item.id_matiere].substituteTeachers = [];
                    item.coTeachers.forEach(coTeacher => {
                        if(coTeacher.is_visible){
                            let coTeacherLastName, coTeacherFirstName, coTeacherName;
                            let coT = _.findWhere(teachers, {id : coTeacher.second_teacher_id});
                            if(coT != undefined){
                                [coTeacherLastName, coTeacherFirstName] = coT.displayName.split(" ");
                                coTeacherName = Utils.makeShortName(coTeacherLastName, coTeacherFirstName);
                                if(!_.contains(teacherBySubject[item.id_matiere].coTeachers, coTeacherName))
                                    teacherBySubject[item.id_matiere].coTeachers.push(coTeacherName);
                            }
                        }
                    });
                    item.substituteTeachers.forEach(substituteTeacher => {
                        if(substituteTeacher.is_visible){
                            let substituteTeacherLastName, substituteTeacherFirstName, substituteTeacherName;
                            let subT = _.findWhere(teachers, {id : substituteTeacher.second_teacher_id});
                            if(subT != undefined){
                                [substituteTeacherLastName, substituteTeacherFirstName] = subT.displayName.split(" ");
                                substituteTeacherName = Utils.makeShortName(substituteTeacherLastName, substituteTeacherFirstName);
                                let conditionForDate = periode.id != null ?
                                    moment(substituteTeacher.start_date).isBetween(moment(periode.timestamp_dt), moment(periode.timestamp_fn), 'days', '[]')
                                    || moment(substituteTeacher.end_date).isBetween(moment(periode.timestamp_dt), moment(periode.timestamp_fn), 'days', '[]')
                                    || moment(periode.timestamp_dt).isBetween(moment(substituteTeacher.start_date), moment(substituteTeacher.end_date), 'days', '[]')
                                    || moment(periode.timestamp_fn).isBetween(moment(substituteTeacher.start_date), moment(substituteTeacher.end_date), 'days', '[]')
                                    : true;
                                if(!_.contains(teacherBySubject[item.id_matiere].substituteTeachers, substituteTeacherName) && conditionForDate){
                                    teacherBySubject[item.id_matiere].substituteTeachers.push(substituteTeacherName);
                                }
                            }
                        }
                    });
                }
            }
        });
    }
    return <{idSubject : Enseignant}[]> teacherBySubject;
};