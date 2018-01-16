/**
 * Created by ledunoiss on 12/01/2017.
 */
/**
 * Scroll la fenêtre vers une position donnée
 * @param pos position vers laquelle scroller. La position peut être de type string ('top') ou
 * de type array ([0,0], [0,500])
 */
export function scrollTo (pos : string | number[]) : void {
    let posToScoll : number[] = (typeof pos === 'string' && pos === 'top') ? [0,0] : pos as number[];
    window.scrollTo(posToScoll[0], posToScoll[1]);
}