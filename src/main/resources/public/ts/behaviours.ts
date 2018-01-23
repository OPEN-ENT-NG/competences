import { Behaviours } from 'entcore';

Behaviours.register('competences', {
	rights: {
		workflow: {
            'export-lsun': 'fr.openent.evaluations.controller.LSUController|getXML'
        },
		resource: {}
	},
	loadResources: async function(callback){}
});
