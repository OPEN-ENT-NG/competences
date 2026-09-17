import { http, HttpResponse } from 'entcore-toolkit';
import {SubtopicserviceService} from "../models/sniplets";
import {ng} from "entcore";

export class SubTopicsServiceService{
    async set(subTopicsService: SubtopicserviceService): Promise<HttpResponse>{
       return http.post(`competences/subtopics/services/update`, subTopicsService.toJson());

    }
    async get(idStructure): Promise<HttpResponse>{
      return  http.get(`/competences/subtopics/services/${idStructure}`);
    }
}

export const subTopicService = ng.service('SubTopicService', SubTopicsServiceService);