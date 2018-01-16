import { _ } from 'entcore';
/**
 * @param arr liste de nombres
 * @returns la moyenne si la liste n'est pas vide
 */
export function average (arr) {
    return _.reduce(arr, function(memo, num) {
            return memo + num;
        }, 0) / (arr.length === 0 ? 1 : arr.length);
}