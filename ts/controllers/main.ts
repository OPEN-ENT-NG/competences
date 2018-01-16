import { ng } from 'entcore';

export const mainController = ng.controller('MainController', ['$scope', 'route',($scope, route) => {

	route({
		main: function(){}
	});

}]);
