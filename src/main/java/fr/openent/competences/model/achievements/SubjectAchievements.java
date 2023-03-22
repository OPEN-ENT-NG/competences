package fr.openent.competences.model.achievements;

import fr.openent.competences.constants.Field;
import fr.openent.competences.model.SkillModel;
import io.vertx.core.json.JsonObject;

public class SubjectAchievements implements SkillModel<SubjectAchievements> {
    private String subjectId;
    private Integer skillsValidatedPercentage;

    public SubjectAchievements() {
    }

    public SubjectAchievements(JsonObject badge) {
        this.set(badge);
    }

    @Override
    public SubjectAchievements set(JsonObject subjectAchievements) {
        this.subjectId = subjectAchievements.getString(Field.SUBJECT_ID);
        this.skillsValidatedPercentage = subjectAchievements.getInteger(Field.SKILLS_VALIDATED_PERCENTAGE);
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(Field.SUBJECTID, this.subjectId)
                .put(Field.SKILLSVALIDATEDPERCENTAGE, this.skillsValidatedPercentage);
    }

    @Override
    public SubjectAchievements model(JsonObject subjectAchievements) {
        return new SubjectAchievements(subjectAchievements);
    }
}
