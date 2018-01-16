import { Model, http } from 'entcore';
export class Devoir extends Model {
    id : number;
    appreciation: string;

    //TODO Delete when infra-front will be fixed
    updateData: (o) => void;

    get api() {
        return {
            GET_APPRECIATION: '/viescolaire/appreciation/devoir/' + this.id +  '/eleve/'
         };
    }

    constructor(o?: any) {
        super();
        if (o !== undefined) this.updateData(o);
    }

    async getAppreciation(idEleve): Promise<any> {
        return new Promise(async (resolve, reject) => {
            let uri = this.api.GET_APPRECIATION + idEleve;
            http().getJson(uri).done((res) => {
                if(res.length > 0) {
                    this.appreciation = res[0].appreciation;
                }
                resolve();
            }).error(() => {
                reject();
            });
        });
    };
}
