package fr.openent.competences.model.achievements;

import fr.openent.competences.constants.Field;
import fr.openent.competences.model.SkillModel;
import io.vertx.core.json.JsonObject;

public class AchievementsSubject implements SkillModel<AchievementsSubject> {
    private String subjectId;
    private Integer skillsValidatedPercentage;

    public AchievementsSubject() {
    }

    public AchievementsSubject(JsonObject achievementsSubject) {
        this.set(achievementsSubject);
    }

    @Override
    public AchievementsSubject set(JsonObject achievementsSubject) {
        this.subjectId = achievementsSubject.getString(Field.SUBJECT_ID);
        this.skillsValidatedPercentage = achievementsSubject.getInteger(Field.SKILLS_VALIDATED_PERCENTAGE);
        return this;
    }

    public String subjectId() {
        return subjectId;
    }

    public Integer skillsValidatedPercentage() {
        return skillsValidatedPercentage;
    }


    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(Field.SUBJECTID, this.subjectId)
                .put(Field.SKILLSVALIDATEDPERCENTAGE, this.skillsValidatedPercentage);
    }

    @Override
    public AchievementsSubject model(JsonObject achievementsSubject) {
        return new AchievementsSubject(achievementsSubject);
    }
}
