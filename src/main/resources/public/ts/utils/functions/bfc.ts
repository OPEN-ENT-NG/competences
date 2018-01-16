/**
 * Created by vogelmt on 13/04/2017.
 */
/**
 * détermine la valeur à envoyer au BFC en fonction de la moyenne et la table de conversions
 * @param moyenne
 * @param table de conversions
 * @returns {any|numeric} moyenne pour bfc
 */
export function getMoyenneForBFC(moyenne, tableConversions){
    let maConvertion = undefined;
    for(let i= 0 ; i < tableConversions.length ; i++){
        if((tableConversions[i].valmin <= moyenne && tableConversions[i].valmax > moyenne) && tableConversions[i].ordre !== tableConversions.length ){
            maConvertion = tableConversions[i];
        }else if((tableConversions[i].valmin <= moyenne && tableConversions[i].valmax >= moyenne) && tableConversions[i].ordre === tableConversions.length ){
            maConvertion = tableConversions[i];
        }
    }
    if(maConvertion !== undefined){
        return parseInt(maConvertion.ordre);
    }else{
        return -1;
    }
};