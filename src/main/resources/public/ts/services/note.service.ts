import http, {AxiosResponse} from "axios";
import {ng} from "entcore";
import {typeImport} from "../constants";

export interface INoteService {
    importNote(classId : string, devoirId: number): Promise<AxiosResponse>;
}


export const NoteService: INoteService = {

    async importNote (classId : string, devoirId: number) : Promise<AxiosResponse> {
        return http.post(`competences/notes/${typeImport.CSV}/csv/exercizer/import/${classId}/${devoirId}`);
    }

}
export const noteService = ng.service('NoteService', NoteService);