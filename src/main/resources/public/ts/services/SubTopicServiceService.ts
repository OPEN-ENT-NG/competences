import http from "axios";
import {SubTopicsService} from "../models/sniplets";
import {Mix} from "entcore-toolkit";

export class SubTopicsServiceService{
    async set(subTopicsService: SubTopicsService){
       return http.post(`competences/subtopics/services/update`, subTopicsService.toJson());

    }
    async get(idStructure){
      return  http.get(`/competences/subtopics/services/${idStructure}`);
    }
}