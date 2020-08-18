import {Enseignant, Classe, Utils} from "../../models/teacher";
import {_, moment} from "entcore";
import {Periode} from "../../models/common/Periode";

/**
 * @param {string} schoolClassId
 * @param  {Array<Classe>} schoolClasses
 * @param teachers {Array<Enseignant>}
 * @return { {idSubject:Enseignant}[] }
 */

export const getTeacherBySubject:Function = (schoolClasses:Array<any>,
                                             schoolClassId:string,
                                             teachers:Array<Enseignant>,
                                             periode : Periode):{idSubject:Enseignant}[] => {
    const currentClass = _.chain(schoolClasses)
        .findWhere({id: schoolClassId})
        .value();
    const teacherBySubject:{} = {};
    if(currentClass && currentClass.services){
        currentClass.services.forEach(item => {
            if(item && item.id_matiere && item.id_enseignant){
                teacherBySubject[item.id_matiere] = _.findWhere(teachers, {id : item.id_enseignant});
                if(teacherBySubject[item.id_matiere] != undefined){
                    teacherBySubject[item.id_matiere].coTeachers = [];
                    teacherBySubject[item.id_matiere].substituteTeachers = [];
                    teacherBySubject[item.id_matiere].ensIsVisible = item.is_visible;
                    item.coTeachers.forEach(coTeacher => {
                        if(coTeacher.is_visible){
                            let coTeacherLastName, coTeacherFirstName, coTeacherName;
                            [coTeacherLastName, coTeacherFirstName] = _.findWhere(teachers,
                                {id : coTeacher.second_teacher_id}).displayName.split(" ");
                            coTeacherName = Utils.makeShortName(coTeacherLastName, coTeacherFirstName);
                            if(!_.contains(teacherBySubject[item.id_matiere].coTeachers, coTeacherName))
                                teacherBySubject[item.id_matiere].coTeachers.push(coTeacherName);
                        }
                    });
                    item.substituteTeachers.forEach(substituteTeacher => {
                        if(substituteTeacher.is_visible){
                            let substituteTeacherLastName, substituteTeacherFirstName, substituteTeacherName;
                            [substituteTeacherLastName, substituteTeacherFirstName] = _.findWhere(teachers,
                                {id : substituteTeacher.second_teacher_id}).displayName.split(" ");
                            substituteTeacherName = Utils.makeShortName(substituteTeacherLastName, substituteTeacherFirstName);
                            let conditionForDate = periode.id != null ?
                                moment(substituteTeacher.start_date).isBetween(moment(periode.timestamp_dt), moment(periode.timestamp_fn), 'days', '[]')
                                || moment(substituteTeacher.end_date).isBetween(moment(periode.timestamp_dt), moment(periode.timestamp_fn), 'days', '[]') : true;
                            if(!_.contains(teacherBySubject[item.id_matiere].substituteTeachers, substituteTeacherName) && conditionForDate){
                                teacherBySubject[item.id_matiere].substituteTeachers.push(substituteTeacherName);
                            }
                        }
                    });
                }
            }
        });
    }
    return <{idSubject:Enseignant}[]> teacherBySubject;
};