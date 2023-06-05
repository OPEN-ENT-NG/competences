package fr.openent.competences.model;

import fr.openent.competences.constants.Field;
import fr.openent.competences.constants.SqlVersion;
import io.vertx.core.json.JsonObject;


public class Config implements SkillModel<Config> {
    private String transitionSqlVersion;

    public Config() {
    }

    public Config(JsonObject config) {
        this.set(config);
    }

    @Override
    public Config set(JsonObject config) {
        if (config != null) {
            this.transitionSqlVersion = config.getJsonObject(Field.TRANSITION, new JsonObject()).getString(Field.SQL_VERSION);
        }
        return this;
    }

    public String transitionSqlVersion() {
        return this.transitionSqlVersion != null ? this.transitionSqlVersion : SqlVersion.V2;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(Field.SQL_VERSION, transitionSqlVersion);
    }

    @Override
    public Config model(JsonObject model) {
        return new Config(model);
    }
}