package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class Periode extends Model{
    private String name;
    private Long type;
    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getType() {
        return type;
    }

    public void setType(Long type) {
        this.type = type;
    }
}
