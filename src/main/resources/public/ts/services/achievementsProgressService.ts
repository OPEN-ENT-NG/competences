import {ng} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {AchievementsProgress, IAchievementsProgressPayload, IAchievementsProgressResponse} from "../models/achievements.model";

export interface IAchievementsProgressService {
    getSubjectsSkillsValidatedPercentage(structureId: string, studentId: string, payload: IAchievementsProgressPayload): Promise<AchievementsProgress>;
}

export const achievementsProgressService: IAchievementsProgressService = {
    /**
     * get subjectSkillsAssignments for a given student.
     *
     * @param structureId
     * @param studentId
     * @param payload
     */
    getSubjectsSkillsValidatedPercentage: async (structureId: string, studentId: string, payload: IAchievementsProgressPayload): Promise<AchievementsProgress> => {
        let uriParams = new URLSearchParams();

        if (!!payload.periodId) uriParams.append("periodId", payload.periodId.toString())
        if (!!payload.groupId) uriParams.append("groupId", payload.groupId)

        return http.get(`competences/structures/${structureId}/student/${studentId}/subjectsSkillsValidatedPercentage?${uriParams}`)
            .then((res: AxiosResponse) => new AchievementsProgress(<IAchievementsProgressResponse>res.data));
    }

};

export const AchievementsService = ng.service('AchievementsService', (): IAchievementsProgressService => achievementsProgressService);