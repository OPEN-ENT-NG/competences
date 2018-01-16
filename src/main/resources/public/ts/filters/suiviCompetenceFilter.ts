/**
 * Created by ledunoiss on 09/11/2016.
 */
/**
 * Created by ledunoiss on 20/09/2016.
 */
import { ng, moment, _ } from 'entcore';
import {CompetenceNote} from "../models/teacher/eval_teacher_mdl";

export let fitlerCompetenceSuivi = ng.filter('fitlerCompetenceSuivi', function(){
    return function(competences, mine, user){
        var _t = competences;
        if (mine === 'true' || mine === true) {
            _t = _.filter(competences, function (competence) {
                return competence.owner === undefined || competence.owner === user.userId;
            });
        }
        var output = [];
        var max = null;
        if (_t.length > 0) {
            max = _.max(_t, function (competence) {
                return competence.evaluation;
            });
        }
        max === -Infinity || max === null ? output.push(new CompetenceNote({evaluation : -1})) : output.push(max);
        return output;
    };
});
