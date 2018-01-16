/**
 * Recherche true si une chaine de caractère contient un mot clef.
 * La casse est ignorée et la recherche d'un mot clef vide ou contenant que des espaces
 * retourne false
 * @param psString la chaine de caractère
 * @param psKeyword le mot clef recherché
 * @returns {boolean} true si ça match false sinon
 */
export function containsIgnoreCase(psString, psKeyword){
    if (psKeyword !== undefined) {
        return psString.toLowerCase().indexOf(psKeyword.toLowerCase())>=0 && psKeyword.trim() !== "";
    } else {
        return false;
    }
};