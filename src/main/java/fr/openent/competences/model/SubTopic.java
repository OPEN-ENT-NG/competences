package fr.openent.competences.model;

import fr.openent.competences.constants.Field;
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
                .put(Field.ID,this.id)
                .put(Field.ID_MATIERE,this.service.getMatiere().getId())
                .put(Field.COEFFICIENT,coefficient);
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
