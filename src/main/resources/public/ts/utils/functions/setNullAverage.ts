import { getEA, getNN } from "./utilsNN";

export const setNullAverage = (eleves) => {
    eleves.forEach(eleve => {
        if (!eleve.moyenne || eleve.moyenne == getNN()) {
            if (eleve.isUserInThirdClassLevel) {
                eleve.moyenne = getEA();
            }
            else {
                eleve.moyenne = getNN();
            }
        }
    });
}