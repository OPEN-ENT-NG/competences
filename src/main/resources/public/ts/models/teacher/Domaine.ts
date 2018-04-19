import {Model, Collection, _, notify} from 'entcore';
import {Mix} from 'entcore-toolkit';
import { Competence, BilanFinDeCycle, Utils,DispenseDomaine } from './index';
import http from "axios";


export class Domaine extends Model {
    domaines : Collection<Domaine>;
    competences : Collection<Competence>;
    id : number;
    niveau : number;
    id_parent : number;
    moyenne : number;
    bfc : BilanFinDeCycle;
    libelle : string;
    codification : string;
    composer : any;
    evaluated : boolean;
    visible : boolean;
    id_eleve : string;
    id_chef_etablissement : string;
    id_etablissement : string;
    dispensable: boolean;
    //dispense_eleve : DispenseDomaine;
    dispense_eleve : boolean;


    /**
     * Méthode activant l'affichage des sous domaines d'un domaine
     *
     * @pbMesEvaluations booleen indiquant d'afficher ou non un domaine
     *
     */
    setVisibleSousDomaines (pbVisible) {
        Utils.setVisibleSousDomainesRec(this.domaines, pbVisible);
    }


    async saveDispenseEleve() {
        let dispenseDomaineEleve = new DispenseDomaine(this.id,this.id_eleve,this.dispense_eleve);
        try{
        await dispenseDomaineEleve.save();
        }catch(e){
            this.dispense_eleve = dispenseDomaineEleve.dispense;
        }
    }

    constructor (poDomaine?,id_eleve?: string) {
        super();
        if(id_eleve) poDomaine.id_eleve = id_eleve;
        if(poDomaine.dispense_eleve === null){
            poDomaine.dispense_eleve = false;
        }

        this.collection(Competence);
        this.collection(Domaine);

        if(poDomaine !== undefined) {

            let sousDomaines = poDomaine.domaines;
            let sousCompetences = poDomaine.competences;
            if(sousDomaines !== undefined){
                for (let i=0; i< sousDomaines.length; i++){
                    sousDomaines[i].id_eleve = poDomaine.id_eleve;
                    if(sousDomaines[i].dispense_eleve === null){
                        sousDomaines[i].dispense_eleve = false;
                    }
                }
            }
            this.updateData(poDomaine, false);

            if(sousDomaines !== undefined) {
                this.domaines.load(sousDomaines);
            }

            if(sousCompetences !== undefined) {
                this.competences.load(sousCompetences);
            }
        }
    }


}