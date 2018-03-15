import { http } from 'entcore';

export const averageBFC = {
    title: 'Moyenne BFC',
    description: 'Active la visibilité de la moyenne calculée sur le BFC',
    controller: {
        init: function () {
            this.idStructure = this.source.idStructure;
            http().get(`/competences/bfc/moyennes/visible/structures/${this.idStructure}`)
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

            http().putJson(`/competences/bfc/moyennes/visible/structures/${this.idStructure}/${visibility}`)
                .done(function () {
                    this.visible = visibility;
                    this.$apply('visible');
                    console.log('visibility set');
                }.bind(this))
                .error(function () {
                    this.$apply('error during seting visiblility');
                }.bind(this));
        }
    }
}