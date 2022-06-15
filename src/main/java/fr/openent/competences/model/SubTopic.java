package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class SubTopic {
    private String libelle;
    private Integer id;
    private Service service;
    private Double coefficient = 1.d;

    public SubTopic() {
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public JsonObject toJsonObject() {
        return new JsonObject()
                .put("id",this.id)
                .put("id_matiere",this.service.getMatiere().getId())
                .put("coefficient",coefficient);
    }

    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }
}
