import http, {AxiosResponse} from "axios";
import {SubtopicserviceService} from "../models/sniplets";
import {ng} from "entcore";

export class SubTopicsServiceService{
    async set(subTopicsService: SubtopicserviceService): Promise<AxiosResponse>{
       return http.post(`competences/subtopics/services/update`, subTopicsService.toJson());

    }
    async get(idStructure): Promise<AxiosResponse>{
      return  http.get(`/competences/subtopics/services/${idStructure}`);
    }
}

export const subTopicService = ng.service('SubTopicService', SubTopicsServiceService);