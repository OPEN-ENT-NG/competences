package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;


public class Matiere {
    private String backgroundColor;
    private String id;
    private String libelle;
    private static final String BACKGROUND_COLOR = "backgroundColor";
    private static final String LIBELLE_MATIERE = "libelleMatiere";

    public Matiere() {
    }

    public Matiere(String id) {
        this.id = id;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public JsonObject toJsonObject() {
        return new JsonObject().put(BACKGROUND_COLOR,backgroundColor).put(LIBELLE_MATIERE,libelle);
    }
}
