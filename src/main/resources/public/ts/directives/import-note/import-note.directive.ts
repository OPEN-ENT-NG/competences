import {ng, angular, idiom, _} from "entcore";
import {IScope, ILocationService, IWindowService} from "angular";
import {Devoir, Periode, Utils} from "../../models/teacher";
import {RootsConst} from "../../constants/roots.const";
import {NoteService} from "../../services/note.service";
import {AxiosError, AxiosResponse} from "axios";
import * as utils from "../../utils/teacher";


interface IViewModel {

    openLightboxImportNote: boolean;
    isErrorStudent: boolean;
    files: File[];
    multiple: boolean;
    errorMessage: string;
    lang: typeof idiom;

    toggleLightbox(): void;
    cancelLightboxImportNote(): void;
    isValid(): boolean;
    validImport(): Promise<void>;
    importNote(): Promise<any>;
    disabledButton() : boolean;
}

interface IImportNoteProps {
    devoir: Devoir;
    onImport;
}

interface IImportScope extends IScope   {
    vm: IImportNoteProps;
}


class Controller implements ng.IController, IViewModel {

    openLightboxImportNote: boolean;
    isErrorStudent: boolean;
    files: File[];
    devoir: Devoir;
    multiple: boolean;
    errorMessage: string;
    lang: typeof idiom;

    constructor(private $scope: IImportScope,
                private $location:ILocationService,
                private $window: IWindowService,
                private $parse: any
               ) {
        this.lang = idiom;
        this.files = [];
    }

    $onInit() {
    }

    $onDestroy() {
    }

    toggleLightbox() : void {
        this.openLightboxImportNote = true;
        this.isErrorStudent = false;
    }

    cancelLightboxImportNote() : void {
        this.openLightboxImportNote = false;
        delete this.errorMessage;
    }

    async validImport(): Promise<void> {
        if(this.isValid()) {
            if(!this.isErrorStudent) {
                await this.importNote();
                await utils.safeApply(this.$scope);
            } else {
                this.cancelLightboxImportNote();
                this.$parse(this.$scope.vm.onImport())();
            }
        } else {
            this.errorMessage = "competences.error.import.type.csv";
        }
    }

    disabledButton(): boolean {
        return this.files.length === 0 || (this.errorMessage && this.errorMessage.length > 0);
    }

    async importNote(): Promise<any> {
            delete this.errorMessage;
            let formData = new FormData();
            formData.append('file', this.files[0], this.files[0].name);

            await NoteService.importNote(this.devoir.id_groupe, this.devoir.id, this.devoir.type_groupe,
                parseInt(<string>this.devoir.id_periode), formData)
                .then((response: AxiosResponse) => {
                    if (response.data.status) {
                        if (_.isEmpty(response.data.missing)){
                            this.cancelLightboxImportNote();
                            this.$parse(this.$scope.vm.onImport())();
                        } else {
                            this.isErrorStudent = true;
                        }

                    } else {
                        this.errorMessage = "competences.error.import.csv";
                    }

                })
                .catch((e: AxiosError) => {
                    if (e.response.data.status == "formate"){
                        this.errorMessage = "competences.error.import.csv.formate";
                    }
                    else
                        this.errorMessage = "competences.error.import.csv";
                });
    }

    isValid(): boolean {
        return  this.files.length > 0 ?
            this.files[0].name.endsWith('.csv') && this.files[0].name.trim() !== '' : false ;
    }

}

function directive($parse) {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}import-note/import-note.html`,
        scope: {
            devoir: "=",
            onImport: "&"
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope','$location','$window', '$parse', Controller],
        link: function (scope: ng.IScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: ng.IController) {

            let elt = angular.element('.file-picker-list').scope();
            scope.$watch(() => elt.filesArray, () => {
                if( vm.files && elt.filesArray && !elt.filesArray.length) {
                    vm.files = elt.filesArray;
                    delete vm.errorMessage;
                }
            });
        }
    }
}

export const importNote = ng.directive('importNote', directive);