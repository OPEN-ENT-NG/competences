/**
 * Tri les éléments de la collection alphabétiquement par le
 * champ lastName en ignorant les accents
 * @param collection La collection à trier
 */
export function sortByLastnameWithAccentIgnored (collection) {
    collection.sort(function(a, b) {
        var textA = removeAccent(a.lastName.toUpperCase());
        var textB = removeAccent(b.lastName.toUpperCase());
        return (textA < textB) ? -1 : (textA > textB) ? 1 : 0;
    });
}

/**
 * Retire les accents du string passé en paramètre
 * @param str
 * @returns {any}
 */
let removeAccent = function(str){
    var accent = [
        /[\300-\306]/g, /[\340-\346]/g, // A, a
        /[\310-\313]/g, /[\350-\353]/g, // E, e
        /[\314-\317]/g, /[\354-\357]/g, // I, i
        /[\322-\330]/g, /[\362-\370]/g, // O, o
        /[\331-\334]/g, /[\371-\374]/g, // U, u
        /[\321]/g, /[\361]/g, // N, n
        /[\307]/g, /[\347]/g, // C, c
    ];
    var noaccent = ['A','a','E','e','I','i','O','o','U','u','N','n','C','c'];

    for(var i = 0; i < accent.length; i++){
        str = str.replace(accent[i], noaccent[i]);
    }

    return str;
}