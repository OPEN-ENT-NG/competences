export function pushData(teacher, dataElem, subject?) {
    if (subject) {

        let foundEM = dataElem.find(
            EM => EM.intervenant.id === teacher.id && EM.matiere.id === subject.id);

        if (!foundEM && subject.id !== undefined) {
            dataElem.push({intervenant: teacher, matiere: subject});
        }
    } else {
        let foundClasse = dataElem.find(classe => classe.id === teacher.id);
        if (!foundClasse) {
            dataElem.push(teacher);
        }
    }
}