package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class Group extends  Model {
    private String name;
    private Cycle cycle;
    private Periode periode;

    public Group() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Cycle getCycle() {
        return cycle;
    }

    public void setCycle(Cycle cycle) {
        this.cycle = cycle;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
