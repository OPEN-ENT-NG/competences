import { Periode } from "./Periode";
import { DefaultClasse } from "../common/DefaultClasse";
import { Collection, http } from 'entcore';
import { translate } from "../../utils/functions/translate";
import {TypePeriode} from "../common/TypePeriode";

export class Classe extends DefaultClasse {
    id: string;
    periodes: Collection<Periode>;
    typePeriodes: Collection<TypePeriode>;
    id_cycle: number;


    get api() {
        return {
            getCycle: '/viescolaire/cycle/eleve/' + this.id,
            syncGroupe: '/viescolaire/groupe/enseignement/users/' + this.id + '?type=Student',
            syncPeriode: '/viescolaire/periodes?idGroupe=' + this.id,

            TYPEPERIODES: {
                synchronisation: '/viescolaire/periodes/types'
            }
        };
    }

    constructor(o?: any) {
        super();
        if (o !== undefined) this.updateData(o);
    }

    async sync(): Promise<any> {
        return new Promise(async (resolve, reject) => {
            this.collection(Periode, {
                sync: async (): Promise<any> => {
                    return new Promise((resolve, reject) => {
                        http().getJson(this.api.syncPeriode).done((res) => {
                            res.push({libelle: translate('viescolaire.utils.annee'), id: null});
                            res.push({libelle: "cycle", id: null});
                            this.periodes.load(res);
                            http().getJson(this.api.getCycle).done( async (res) => {
                                this.id_cycle = res[0].id_cycle;
                                resolve();
                            }).bind(this);
                        }).error(function () {
                            if (reject && typeof reject === 'function') {
                                reject();
                            }
                        });
                    });
                }
            });
            this.collection(TypePeriode, {
                sync: async (): Promise<any> => {
                    return await http().getJson(this.api.TYPEPERIODES.synchronisation).done((res) => {
                        this.typePeriodes.load(res);
                    })
                    .error(function () {
                        if (reject && typeof reject === 'function') {
                            reject();
                        }
                    });
                }
            });
            await this.periodes.sync();
            await this.typePeriodes.sync();
            resolve();
        });
    }
}