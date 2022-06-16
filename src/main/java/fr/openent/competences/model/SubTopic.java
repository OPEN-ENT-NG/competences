package fr.openent.competences.model;

import fr.openent.competences.constants.Field;
import io.vertx.core.json.JsonObject;

public class SubTopic {
    private String libelle;
    private Long id;
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
                .put(Field.COEFFICIENT,coefficient);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
