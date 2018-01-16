/**
 * Created by anabah on 28/11/2017.
 */
import { Model } from 'entcore';


export abstract class DefaultClasse extends Model {
    id: string;
    name: string;
    type_groupe: number;
    type_groupe_libelle: string;
}