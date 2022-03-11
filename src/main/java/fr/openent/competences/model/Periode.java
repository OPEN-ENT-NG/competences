package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class Periode extends Model{
    private Long idPeriode;
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

    public Long getIdPeriode() {
        return idPeriode;
    }

    public void setIdPeriode(Long idPeriode) {
        this.idPeriode = idPeriode;
    }

    public void setType(Long type) {
        this.type = type;
    }
}
