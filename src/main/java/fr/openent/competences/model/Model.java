package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public abstract class Model {
    protected String id;

    public Model() {}

    public Model(String id) {
        this.id = id;
    }

    public abstract JsonObject toJsonObject();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }




}
