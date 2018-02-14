import { Model, Collection } from 'entcore';
import { Competence, BilanFinDeCycle, Utils } from './index';

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


    /**
     * Méthode activant l'affichage des sous domaines d'un domaine
     *
     * @pbMesEvaluations booleen indiquant d'afficher ou non un domaine
     *
     */
    setVisibleSousDomaines (pbVisible) {
        Utils.setVisibleSousDomainesRec(this.domaines, pbVisible);
    }

    constructor (poDomaine?) {
        super();
        this.collection(Competence);
        this.collection(Domaine);

        if(poDomaine !== undefined) {

            let sousDomaines = poDomaine.domaines;
            let sousCompetences = poDomaine.competences;

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