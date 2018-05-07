import { http } from 'entcore';

export const visibilityDNB = {
    title: 'Barème du DNB',
    description: 'Active la visibilité du Barème du DNB sur le BFC ',
    controller: {
        init: function () {
            this.idStructure = this.source.idStructure;
            //id_visibility = 2 si barèmeDNB
           this.idVisibility = 2;

            http().get(`/competences/bfc/visibility/structures/${this.idStructure}/${this.idVisibility}`)
                .done(function (res) {
                    this.visible = res[0].visible;
                    console.log('load sniplet visibilityDNB');
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
                    console.log('visibilityDNB set');
                }.bind(this))
                .error(function () {
                }.bind(this));
        }
    }
}