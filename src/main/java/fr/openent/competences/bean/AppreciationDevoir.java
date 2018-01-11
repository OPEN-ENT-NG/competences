package fr.openent.competences.bean;

/**
 * Created by anabah on 01/03/2017.
 */
public class AppreciationDevoir {
    /**
     * Valeur de l'appreciation
     */
    private String appreciation;

    public AppreciationDevoir(String appreciation) {
        this.appreciation = appreciation;
    }

    public String getAppreciation() {
        return this.appreciation;
    }

    public void setAppreciation(String appreciation) {
        this.appreciation = appreciation;
    }

}
