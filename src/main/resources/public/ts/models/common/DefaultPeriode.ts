import { Model } from 'entcore';
/**
 * Created by anabah on 28/11/2017.
 */
export class DefaultPeriode extends Model {
    id: number;
    id_etablissement : string;
    id_classe: string;
    timestamp_dt: Date;
    timestamp_fn: Date;
    date_fin_saisie: Date;
    id_type: number;
}