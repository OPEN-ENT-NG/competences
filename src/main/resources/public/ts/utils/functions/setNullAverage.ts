import { getEA, getNN } from "./utilsNN";

export const setNullAverage = (eleves) => {
    eleves.forEach(eleve => {
        console.log("niko : ", eleve);
        if (!eleve.moyenne || eleve.moyenne == getNN()) {
            if (eleve.isUserInThirdClassLevel) {
                console.log("niko1");
                eleve.moyenne = getEA();
            }
            else {
                console.log("niko2");
                eleve.moyenne = getNN();
            }
        }
    });
}