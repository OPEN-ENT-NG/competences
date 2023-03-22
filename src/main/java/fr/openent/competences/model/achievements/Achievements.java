package fr.openent.competences.model.achievements;

import fr.openent.competences.constants.Field;
import fr.openent.competences.model.SkillModel;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;


public class Achievements implements SkillModel<Achievements> {
    private String structureId;
    private String studentId;
    private List<SubjectAchievements> subjectsAchievements;
    public Achievements() {
    }
    public Achievements(JsonObject badge) {
        this.set(badge);
    }
    public Achievements(String structureId, String studentId) {
        this.structureId = structureId;
        this.studentId = studentId;
    }

    @Override
    public Achievements set(JsonObject achievements) {
        this.structureId =  achievements.getString(Field.STRUCTURE_ID);
        this.studentId = achievements.getString(Field.STUDENT_ID);
        return this;
    }

    public void setSubjectsAchievements(JsonArray subjectsAchievements) {
        this.subjectsAchievements =  new SubjectAchievements().toList(subjectsAchievements);
    }


    @Override
    public JsonObject toJson() {
        JsonObject result = new JsonObject()
                .put(Field.STRUCTUREID, this.structureId)
                .put(Field.STUDENTID, this.studentId);

        if (subjectsAchievements != null)
            result.put(Field.SUBJECTSACHIEVEMENTS, new SubjectAchievements().toArray(subjectsAchievements));

        return result;
    }

    @Override
    public Achievements model(JsonObject achievements) {
        return new Achievements(achievements);
    }
}

