package fr.openent.competences.model;

import fr.openent.competences.helper.ModelHelper;
import io.vertx.core.json.JsonObject;

import static fr.openent.competences.constants.Field.*;

public class MoyenneFinale implements IModel<MoyenneFinale> {

    private String id;
    private Integer id_periode;
    private Integer moyenne;
    private String id_classe;
    private String id_matiere;
    private String statut;

    public MoyenneFinale() {
    }

    public MoyenneFinale(JsonObject json) {
        this.setId(json.getString(ID))
                .setIdPeriode(json.getInteger(ID_PERIODE))
                .setMoyenne(json.getInteger(MOYENNE))
                .setIdClasse(json.getString(ID_CLASSE))
                .setIdMatiere(json.getString(ID_MATIERE))
                .setStatut(json.getString(STATUT));
    }

    // Getters

    public String getId() {
        return id;
    }

    public Integer getIdPeriode() {
        return id_periode;
    }

    public Integer getMoyenne() {
        return moyenne;
    }

    public String getIdClasse() {
        return id_classe;
    }

    public String getIdMatiere() {
        return id_matiere;
    }

    public String getStatut() {
        return statut;
    }

    // Setters (style fluide)

    public MoyenneFinale setId(String id) {
        this.id = id;
        return this;
    }

    public MoyenneFinale setIdPeriode(Integer id_periode) {
        this.id_periode = id_periode;
        return this;
    }

    public MoyenneFinale setMoyenne(Integer moyenne) {
        this.moyenne = moyenne;
        return this;
    }

    public MoyenneFinale setIdClasse(String id_classe) {
        this.id_classe = id_classe;
        return this;
    }

    public MoyenneFinale setIdMatiere(String id_matiere) {
        this.id_matiere = id_matiere;
        return this;
    }

    public MoyenneFinale setStatut(String statut) {
        this.statut = statut;
        return this;
    }

    // Méthode de conversion en JSON
    public JsonObject toJson() {
        return ModelHelper.toJson(this, false, false);
    }
}
