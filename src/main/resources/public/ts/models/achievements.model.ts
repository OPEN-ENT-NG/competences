import {SkillModel} from "./model";

export interface IAchievementsPayload {
    periodId?: number,
    groupId?: string,
}

export interface IAchievementsResponse {
    structureId: string;
    studentId: string;

    subjectsAchievements: ISubjectAchievementsResponse[]
}

export interface ISubjectAchievementsResponse {
    subjectId: string;
    skillsValidatedPercentage: number;
}

export class Achievements extends SkillModel<Achievements> {
    structureId: string;
    studentId: string;
    subjectsAchievements: SubjectAchievements[];

    constructor(data?: IAchievementsResponse) {
        super();
        if (data) this.build(data);
    }

    build(data: IAchievementsResponse): Achievements {
        this.structureId = data.structureId;
        this.studentId = data.studentId;
        this.subjectsAchievements = new SubjectAchievements().toList(data.subjectsAchievements);
        return this;
    }

    toModel(model: any): Achievements {
        return new Achievements(model)
    }
}

export class SubjectAchievements extends SkillModel<SubjectAchievements> {
    subjectId: string;
    skillsValidatedPercentage: number;

    constructor(data?: ISubjectAchievementsResponse) {
        super();
        if (data) this.build(data);
    }

    build(data: ISubjectAchievementsResponse): SubjectAchievements {
        this.subjectId = data.subjectId;
        this.skillsValidatedPercentage = data.skillsValidatedPercentage;
        return this;
    }

    toModel(model: any): SubjectAchievements {
        return new SubjectAchievements(model)
    }
}