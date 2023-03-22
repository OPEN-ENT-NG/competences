import {SkillModel} from "./model";

export interface IAchievementsProgressPayload {
    periodId?: number,
    groupId?: string,
}

export interface IAchievementsProgressResponse {
    structureId: string;
    studentId: string;

    achievementsSubjects: IAchievementsSubjectResponse[]
}

export interface IAchievementsSubjectResponse {
    subjectId: string;
    skillsValidatedPercentage: number;
}

export class AchievementsProgress extends SkillModel<AchievementsProgress> {
    structureId: string;
    studentId: string;
    achievementsSubjects: AchievementsSubject[];

    constructor(data?: IAchievementsProgressResponse) {
        super();
        if (data) this.build(data);
    }

    build(data: IAchievementsProgressResponse): AchievementsProgress {
        this.structureId = data.structureId;
        this.studentId = data.studentId;
        this.achievementsSubjects = new AchievementsSubject().toList(data.achievementsSubjects);
        return this;
    }

    toModel(model: any): AchievementsProgress {
        return new AchievementsProgress(model)
    }
}

export class AchievementsSubject extends SkillModel<AchievementsSubject> {
    subjectId: string;
    skillsValidatedPercentage: number;

    constructor(data?: IAchievementsSubjectResponse) {
        super();
        if (data) this.build(data);
    }

    build(data: IAchievementsSubjectResponse): AchievementsSubject {
        this.subjectId = data.subjectId;
        this.skillsValidatedPercentage = data.skillsValidatedPercentage;
        return this;
    }

    toModel(model: any): AchievementsSubject {
        return new AchievementsSubject(model)
    }
}