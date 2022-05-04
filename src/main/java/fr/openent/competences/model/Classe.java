package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class Classe extends Model{
    private String name;
    private Cycle cycle;
    private Periode periode;
    private String displayName;

    public Classe() {
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

    public String getDisplayName() {
       return displayName != null ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }

}

