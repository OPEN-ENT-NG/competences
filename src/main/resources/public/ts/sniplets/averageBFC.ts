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
        save: function () {
            this.visible = ! this.visible;
            http().putJson(`/competences/bfc/moyennes/visible/structures/${this.idStructure}/${this.visible}`)
                .done(function () {
                    this.$apply('visible');
                    console.log('visibility set');
                }.bind(this))
                .error(function () {
                    this.visible = ! this.visible;
                    this.$apply('visible');
                }.bind(this));
        }
    }
};