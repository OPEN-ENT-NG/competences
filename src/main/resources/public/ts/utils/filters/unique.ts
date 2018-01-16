/**
 * Created by ledunoiss on 16/09/2016.
 */
import { ng, angular } from 'entcore';

export let uniqueFilter = ng.filter('unique',function() {
    return function(collection, keyname) {
        var output = [],
            keys = [];

        angular.forEach(collection, function(item) {
            var key = item[keyname];
            if(keys.indexOf(key) === -1) {
                keys.push(key);
                output.push(item);
            }
        });
        return output;
    };
});