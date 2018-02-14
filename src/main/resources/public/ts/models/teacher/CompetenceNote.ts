import { Model, IModel, http } from 'entcore';

export class CompetenceNote extends Model implements IModel {
    id: number;
    id_devoir: number;
    id_competence: number;
    evaluation: number;
    id_eleve: string;

    get api() {
        return {
            create: '/competences/competence/note',
            update: '/competences/competence/note?id=' + this.id,
            delete: '/competences/competence/note?id=' + this.id
        }
    }

    constructor(o? : any) {
        super();
        if (o !== undefined) this.updateData(o, false);
    }

    toJSON() {
        return {
            id: this.id,
            id_devoir: this.id_devoir,
            id_competence: this.id_competence,
            evaluation: this.evaluation,
            id_eleve: this.id_eleve
        }
    }

    create(): Promise<number> {
        return new Promise((resolve, reject) => {
            http().postJson(this.api.create, this.toJSON()).done((data) => {
                this.id = data.id;
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data.id);
                }
            });
        });
    }

    update(): Promise<any> {
        return new Promise((resolve, reject) => {
            http().putJson(this.api.update, this.toJSON()).done(function (data) {
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve();
                }
            });
        });
    }

    delete(): Promise<any> {
        return new Promise((resolve, reject) => {
            let that = this;
            http().delete(this.api.delete).done(function (data) {
                delete that.id;
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve();
                }
            });
        });
    }

    save(): Promise<any> {
        return new Promise((resolve, reject) => {
            if (!this.id) {
                this.create().then((data) => {
                    if (resolve && (typeof (resolve) === 'function')) {
                        resolve(data);
                    }
                });
            } else if (this.evaluation === -1) {
                this.delete().then((data) => {
                    if (resolve && (typeof (resolve) === 'function')) {
                        resolve();
                    }
                });
            } else {
                this.update();
            }
        });
    }
}