package fr.openent.competences.model.achievements;

import fr.openent.competences.constants.Field;
import fr.openent.competences.model.SkillModel;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;


public class AchievementsProgress implements SkillModel<AchievementsProgress> {
    private String structureId;
    private String studentId;
    private List<AchievementsSubject> achievementsSubjects;

    public AchievementsProgress() {
        this.achievementsSubjects = new ArrayList<>();
    }

    public AchievementsProgress(JsonObject achievementsSubject) {
        this.set(achievementsSubject);
    }

    public AchievementsProgress(String structureId, String studentId) {
        this.structureId = structureId;
        this.studentId = studentId;
        this.achievementsSubjects = new ArrayList<>();
    }

    @Override
    public AchievementsProgress set(JsonObject achievements) {
        this.structureId =  achievements.getString(Field.STRUCTURE_ID);
        this.studentId = achievements.getString(Field.STUDENT_ID);
        this.achievementsSubjects = new ArrayList<>();
        return this;
    }

    public AchievementsProgress structureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String studentId() {
        return this.studentId;
    }

    public AchievementsProgress setAchievementsSubjects(JsonArray achievementsSubjects) {
        this.achievementsSubjects =  new AchievementsSubject().toList(achievementsSubjects);
        return this;
    }

    public AchievementsProgress addAchievementsSubjects(List<AchievementsSubject> achievementsSubjects) {
        this.achievementsSubjects.addAll(achievementsSubjects);
        return this;
    }

    public AchievementsProgress addAchievementsSubject(JsonObject subjectAchievements) {
        this.achievementsSubjects.add(new AchievementsSubject(subjectAchievements));
        return this;
    }

    public List<AchievementsSubject> achievementsSubjects() {
        return achievementsSubjects;
    }


    @Override
    public JsonObject toJson() {
        JsonObject result = new JsonObject()
                .put(Field.STRUCTUREID, this.structureId)
                .put(Field.STUDENTID, this.studentId);

        if (achievementsSubjects != null)
            result.put(Field.ACHIEVEMENTSSUBJECTS, new AchievementsSubject().toArray(achievementsSubjects));

        return result;
    }

    @Override
    public AchievementsProgress model(JsonObject achievements) {
        return new AchievementsProgress(achievements);
    }
}

