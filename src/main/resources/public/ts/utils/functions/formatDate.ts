import { moment } from 'entcore';

/**
 * Formatte la date en fonction du format passé en paramètre
 * @param date Date à formatter
 * @param format Format de retour de la date
 * @returns {any|string} Date formattée
 */
export function getFormatedDate(date, format){
    return moment(date).format(format);
};