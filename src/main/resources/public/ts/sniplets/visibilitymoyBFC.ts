import { http } from 'entcore';

export const visibilitymoyBFC = {
    title: 'Moyenne BFC',
    description: 'Active la visibilité de la moyenne calculée sur le BFC ',
    controller: {
        init: function () {
            this.idStructure = this.source.idStructure;
            //id_visibility = 1 si moyBFC
           this.idVisibility = 1;

            http().get(`/competences/bfc/visibility/structures/${this.idStructure}/${this.idVisibility}`)
                .done(function (res) {
                    this.visible = res[0].visible;
                    console.log('load sniplet averageBFC');
                    this.$apply('visible');
                }.bind(this));
        },
        initSource: function () {
        },
        save: function (visibility:number) {
            // visibility values
            // 0 : caché pour tout le monde
            // 1 : caché pour les enseignants
            // 2 : visible pour tous

            http().putJson(`/competences/bfc/visibility/structures/${this.idStructure}/${this.idVisibility}/${visibility}`)
                .done(function () {
                    this.visible = visibility;
                    this.$apply('visible');
                    console.log('visibility set');
                }.bind(this))
                .error(function () {
                }.bind(this));
        }
    }
}