/**
 * Created by ledunoiss on 16/09/2016.
 */
import { ng, moment, angular, _ } from 'entcore';

export let uniqueMatiereFilter = ng.filter('uniqueMatiere', function() {
    return function(input, collection, keyname, devoirs) {
        var output = [],
            keys = [];

        angular.forEach(collection, function(item) {
            var key = item[keyname];
            if(keys.indexOf(key) === -1) {
                keys.push(key);
                output.push(item);
            }
        });
        var t = _.filter(output, function(item){
            return devoirs.findWhere({ idmatiere : item.id }) !== undefined;
        });
        return t;
    };
});
