package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class Periode extends Model{
    private Long idPeriode;
    private String name;
    private Long type;
    private String startDate;
    private String endDate;
    @Override
    public JsonObject toJsonObject() {
        return new JsonObject().put("name",name)
                .put("year", startDate + " - " + endDate);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
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
