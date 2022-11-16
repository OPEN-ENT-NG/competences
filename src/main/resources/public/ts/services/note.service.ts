import http, {AxiosResponse} from "axios";
import {ng} from "entcore";
import {typeImport} from "../constants";

export interface INoteService {
    importNote(classId: string, devoirId: number, classType: number, periodeId: number, formData: FormData): Promise<AxiosResponse>;
}


export const NoteService: INoteService = {

    async importNote (classId : string, devoirId: number, classType: number, periodeId: number, formData: FormData) : Promise<AxiosResponse> {
        return http.post(`competences/notes/${typeImport.CSV}/csv/exercizer/import/${classId}/${devoirId}/${classType}/${periodeId}`,
            formData, {'headers' : { 'Content-Type': 'multipart/form-data' }});
    }

}
export const noteService = ng.service('NoteService', NoteService);