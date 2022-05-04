package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class Cycle extends Model{

    private int value;
    private String name;

    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
