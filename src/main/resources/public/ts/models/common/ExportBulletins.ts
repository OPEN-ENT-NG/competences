/**
 * Created by anabah on 10/10/2018.
 */
import {_ , notify, idiom as lang} from 'entcore';
import http from "axios";
declare let ArrayBufferLike :any;

export class ExportBulletins {

    static toJSON (options) {
        let o = {
            idStudents: options.idStudents,
            getResponsable: (options.getResponsable === true)? options.getResponsable:false ,
            getProgramElements: (options.getProgramElements === true)? options.getProgramElements:false ,
            moyenneClasse: (options.moyenneClasse === true)? options.moyenneClasse:false ,
            moyenneEleve: (options.moyenneEleve === true)? options.moyenneEleve:false ,
            positionnement: (options.positionnement === true)? options.positionnement:false ,
            showBilanPerDomaines: (options.showBilanPerDomaines === true)? options.showBilanPerDomaines:false ,
            showFamily: (options.showFamily === true)? options.showFamily:false ,
            showProjects: (options.showProjects === true)? options.showProjects:false ,
            threeLevel: (options.threeLevel === true)? options.threeLevel:false ,
            threeMoyenneClasse: (options.threeMoyenneClasse === true)? options.threeMoyenneClasse:false ,
            threeMoyenneEleve: (options.threeMoyenneEleve === true)? options.threeMoyenneEleve:false ,
            threePage: (options.threePage === true)? options.threePage:false ,
            classeName: options.classeName
        };
        if (options.idPeriode !== null || options.idPeriode!== undefined){
            _.extend(o, {idPeriode: options.idPeriode});
        }
        return o
    }


    public static async  generateBulletins (options) {
        try {
            let data = await http.post(`/competences/export/bulletins`, this.toJSON(options),
                {responseType: 'arraybuffer'});

             let blob = new Blob([data.data]);
             let link = document.createElement('a');
             link.href = window.URL.createObjectURL(blob);
             link.download =  data.headers['content-disposition'].split('filename=')[1];
             document.body.appendChild(link);
             link.click();
             setTimeout(function() {
                document.body.removeChild(link);
                window.URL.revokeObjectURL(link.href);
            }, 100);
            notify.success(options.classeName + ' : ' + lang.translate('evaluations.export.bulletin.success'));
        } catch (e) {
            notify.error(options.classeName + ' : ' + lang.translate('evaluations.export.bulletin.error'));
        }

    }


}