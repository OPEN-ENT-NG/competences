import { getEA, getNN } from "./utilsNN";

export const setNullAverage = (eleves) => {
    eleves.forEach(eleve => {
        if (!eleve.moyenne || eleve.moyenne == getNN()) {
            setNullAverageForStudent(eleve);
        }
    });
}

export const setNullAverageForStudent = (eleve) => {
    !!eleve.isUserInThirdClassLevel ?
        eleve.moyenne = getEA() :
        eleve.moyenne = getNN();
}