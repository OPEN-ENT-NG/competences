import { idiom as lang } from 'entcore';

/**
 * Retourne la valeur de la clé i18n
 * @param key clé i18n
 * @returns {SVGMatrix|MSCSSMatrix|any|WebKitCSSMatrix|void} Valeur de la clé i18n
 */
export function translate(key){
    return lang.translate(key)
};