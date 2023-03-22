import {ng} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Achievements, IAchievementsPayload, IAchievementsResponse} from "../models/achievements.model";

export interface IAchievementsService {
    getSubjectsSkillsValidatedPercentage(structureId: string, studentId: string, payload: IAchievementsPayload): Promise<Achievements>;
}

export const achievementsService: IAchievementsService = {
    /**
     * Create badge if not exists and create an assignment to it.
     *
     * @param structureId
     * @param studentId
     * @param payload
     */
    getSubjectsSkillsValidatedPercentage: async (structureId: string, studentId: string, payload: IAchievementsPayload): Promise<Achievements> => {
        let uriParams = new URLSearchParams();

        if (!!payload.periodId) uriParams.append("periodId", payload.periodId.toString())
        if (!!payload.groupId) uriParams.append("groupId", payload.groupId)

        return http.get(`competences/structures/${structureId}/student/${studentId}/subjectsSkillsValidatedPercentage?${uriParams}`)
            .then((res: AxiosResponse) => new Achievements(<IAchievementsResponse>res.data));
    }

};

export const AchievementsService = ng.service('AchievementsService', (): IAchievementsService => achievementsService);