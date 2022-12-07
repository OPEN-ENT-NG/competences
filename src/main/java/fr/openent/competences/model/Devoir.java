package fr.openent.competences.model;

import fr.openent.competences.constants.Field;
import fr.openent.competences.utils.HomeworkUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Devoir extends Model implements Cloneable {
    private String structureId;
    private String name;
    private String owner;
    private String ownerName;
    private String libelle;
    private String subjectId;
    private String groupId;
    private String publishDate;
    private boolean ramenerSur;
    private boolean isEvalued;
    private double coefficient;
    private Double diviseur;
    private String date;
    private int percent;
    private boolean apprecVisible;
    private boolean evalLibHistorise;

    //TODO remplacer par les futurs model
    private int SubtopicId;
    private int periodId;
    private int typeId;
    private JsonArray idsCompetences;

    //TODO supprimer une fois le model totalement integré
    private JsonObject oldModel;

    public  Devoir(JsonObject devoirJO) {
        super();
        this.structureId = devoirJO.getString("id_etablissement");
        this.name = devoirJO.getString("name");
        this.owner = devoirJO.getString("owner");
        this.ownerName = devoirJO.getString("owner_name");
        this.subjectId = devoirJO.getString("id_matiere");
        this.groupId = devoirJO.getString("id_groupe");
        this.publishDate = devoirJO.getString("date_publication");
        this.ramenerSur = devoirJO.getBoolean("ramener_sur");
        this.isEvalued = devoirJO.getBoolean("is_evaluated");
        try{
            this.coefficient = devoirJO.getInteger(Field.COEFFICIENT);
        } catch (ClassCastException c) {
            this.coefficient = HomeworkUtils.safeGetDouble(devoirJO, Field.COEFFICIENT);
        }
        this.diviseur = HomeworkUtils.safeGetDouble(devoirJO, Field.DIVISEUR);
        this.periodId = devoirJO.getInteger("id_periode");
        this.typeId = devoirJO.getInteger("id_type");
        this.idsCompetences = devoirJO.getJsonArray("competences");

        this.oldModel = devoirJO;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public int getSubtopicId() {
        return SubtopicId;
    }

    public void setSubtopicId(int subtopicId) {
        SubtopicId = subtopicId;
    }

    public int getPeriodId() {
        return periodId;
    }

    public void setPeriodId(int periodId) {
        this.periodId = periodId;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public JsonArray getIdsCompetences() {
        return idsCompetences;
    }

    public void setIdsCompetences(JsonArray idsCompetences) {
        this.idsCompetences = idsCompetences;
    }


    public String getStructureId() {
        return structureId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }

    public Double getDiviseur() {
        return diviseur;
    }

    public void setDiviseur(Double diviseur) {
        this.diviseur = diviseur;
    }

    public boolean isRamenerSur() {
        return ramenerSur;
    }

    public void setRamenerSur(boolean ramenerSur) {
        this.ramenerSur = ramenerSur;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isEvalued() {
        return isEvalued;
    }

    public void setEvalued(boolean evalued) {
        isEvalued = evalued;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public boolean isApprecVisible() {
        return apprecVisible;
    }

    public void setApprecVisible(boolean apprecVisible) {
        this.apprecVisible = apprecVisible;
    }

    public boolean isEvalLibHistorise() {
        return evalLibHistorise;
    }

    public void setEvalLibHistorise(boolean evalLibHistorise) {
        this.evalLibHistorise = evalLibHistorise;
    }

    @Override
    public Devoir clone(){
        try {
            return (Devoir) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }
    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    public JsonObject getOldModel() {
        return oldModel;
    }

    public void setOldModel(JsonObject oldModel) {
        this.oldModel = oldModel;
    }
}
