
import {
    structureOptionsService,
    IStructureOptionsService,
    StructureOptionsService,
    StructureOptions
} from "../services";

import {toasts} from "entcore";
import {safeApply} from "../utils/functions/safeApply";

export class SnipletStructureOptions {
   option : StructureOptions;

    constructor( private $scope: any, private structureOptionsService: IStructureOptionsService) {
        this.initOptionsIsAverageSkills($scope.source.idStructure);
    }

    async initOptionsIsAverageSkills(structureId: string): Promise<void> {
        try {
            this.option = await this.structureOptionsService.getStructureOptionsIsAverageSkills(structureId);
            safeApply(this.$scope);
        } catch (err) {
            console.error(err);
            toasts.warning('competences.structure.options.error.get')
        }
    }

    async saveOptionIsAverageSkills () : Promise<void> {
        try {
            await this.structureOptionsService.saveStrustureOptinsIsAverageSkills(this.option);
        } catch (err ) {
            console.error(err);
            this.option.isSkillAverage = !this.option.isSkillAverage;
            safeApply(this.$scope);
            toasts.warning('competences.structure.options.error.save')
        }

    }
}
export const structureOptionIsAverageSkills = {
    title: 'choose calculate skill items',
    description: 'choose the calculation of the average of the skill items',
    controller: {
        init: function(): void {
            console.log('load snipplet isAveargeSkills');
            this.vm = new SnipletStructureOptions(this, structureOptionsService);
        }
    }
};
