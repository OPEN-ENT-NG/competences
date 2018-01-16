import {Model} from 'entcore';
/**
 * Created by rahnir on 10/08/2017.
 */
export class Periode extends Model{
    id: number;
    id_etablissement : string;
    id_classe: string;
    timestamp_dt: Date;
    timestamp_fn: Date;
    date_fin_saisie: Date;
    id_type: number;

}