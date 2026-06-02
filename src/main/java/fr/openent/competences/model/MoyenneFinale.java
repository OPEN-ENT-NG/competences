package fr.openent.competences.model;

import fr.openent.competences.helper.ModelHelper;
import io.vertx.core.json.JsonObject;

import static fr.openent.competences.constants.Field.*;

public class MoyenneFinale implements IModel<MoyenneFinale> {
    
    private Integer idPeriode;
    private Integer moyenne;
    private String idClasse;
    private String idMatiere;
    private String statut;

    public MoyenneFinale() {
    }

    public MoyenneFinale(JsonObject json) {
        this.setIdPeriode(json.getValue(ID_PERIODE) != null ? Integer.valueOf(json.getValue(ID_PERIODE).toString()) : null)
            .setMoyenne(json.getValue(MOYENNE) != null ? Integer.valueOf(json.getValue(MOYENNE).toString()) : null)
            .setIdClasse(json.getString(ID_CLASSE))
            .setIdMatiere(json.getString(ID_MATIERE))
            .setStatut(json.getString(STATUT));
    }


    // Getters

    public Integer getIdPeriode() {
        return idPeriode;
    }

    public Integer getMoyenne() {
        return moyenne;
    }

    public String getIdClasse() {
        return idClasse;
    }

    public String getIdMatiere() {
        return idMatiere;
    }

    public String getStatut() {
        return statut;
    }

    // Setters (style fluide)

    public MoyenneFinale setIdPeriode(Integer idPeriode) {
        this.idPeriode = idPeriode;
        return this;
    }

    public MoyenneFinale setMoyenne(Integer moyenne) {
        this.moyenne = moyenne;
        return this;
    }

    public MoyenneFinale setIdClasse(String idClasse) {
        this.idClasse = idClasse;
        return this;
    }

    public MoyenneFinale setIdMatiere(String idMatiere) {
        this.idMatiere = idMatiere;
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
