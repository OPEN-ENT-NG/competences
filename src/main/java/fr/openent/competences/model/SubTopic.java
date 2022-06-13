package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class SubTopic extends Model{
    private String libelle;
    private String id;
    private Service service;
    private int coefficient = 1;

    public SubTopic() {
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    @Override
    public JsonObject toJsonObject() {
        return new JsonObject()
                .put("id",this.id)
                .put("id_matiere",this.service.getMatiere().getId())
                .put("coefficient",coefficient);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public int getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(int coefficient) {
        this.coefficient = coefficient;
    }
}
