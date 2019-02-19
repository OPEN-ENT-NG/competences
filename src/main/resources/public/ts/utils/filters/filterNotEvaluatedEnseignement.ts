import {_} from "entcore";

export let  FilterNotEvaluated = function (maCompetence, display?) {
    let _t = maCompetence.competencesEvaluations;
    let max = _.max(_t, function (evaluation) {
        return evaluation.evaluation;
    });
    return ((typeof max === 'object') || display === true);
};

let FilterNotEvaluatedConnaissance = function (maConnaissance, display?) {
    for (let i = 0; i < maConnaissance.competences.all.length; i++) {
        let maCompetence = maConnaissance.competences.all[i];
        if(FilterNotEvaluated(maCompetence, display)){
            return true;
        }
    }
    return false;
};

export let FilterNotEvaluatedEnseignement = function (monEnseignement, display? ) {
    for (let i = 0; i < monEnseignement.competences.all.length; i++) {
        let maConnaissance = monEnseignement.competences.all[i];
        if(FilterNotEvaluatedConnaissance(maConnaissance, display)){
            return true;
        }
    }
    return false;
};
