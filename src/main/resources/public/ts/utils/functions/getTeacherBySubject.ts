import {Enseignant, Classe} from "../../models/teacher";
import {_} from "entcore";

/**
 * @param {string} schoolClassId
 * @param  {Array<Classe>} schoolClasses
 * @param teachers {Array<Enseignant>}
 * @return { {idSubject:Enseignant}[] }
 */

export const getTeacherBySubject:Function = (schoolClasses:Array<any>,
                                             schoolClassId:string,
                                             teachers:Array<Enseignant>):{idSubject:Enseignant}[] => {
    const currentClass = _.chain(schoolClasses)
        .findWhere({id: schoolClassId})
        .value();
    const teacherBySubject:{} = {};
    if(currentClass && currentClass.services){
        currentClass.services.forEach(item => {
            if(item && item.id_matiere && item.id_enseignant){
                teacherBySubject[item.id_matiere] = _.findWhere(teachers, {id:item.id_enseignant});
            }
        });
    }
    return <{idSubject:Enseignant}[]> teacherBySubject;
};