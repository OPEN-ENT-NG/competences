package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class AppreciationSubjectPeriodModel extends Model {
    String idClassSchool;
    String idStudent;
    Long idPeriod;
    String idSubject;
    String appreciation;

    public AppreciationSubjectPeriodModel(JsonObject appreciationSubjectPeriodDirty) {
        setIdClassSchool(appreciationSubjectPeriodDirty.getString("idClasse"));
        setIdStudent(appreciationSubjectPeriodDirty.getString("idEleve"));
        setIdPeriod(appreciationSubjectPeriodDirty.getLong("idPeriode"));
        setIdSubject(appreciationSubjectPeriodDirty.getString("idMatiere"));
        setAppreciation(appreciationSubjectPeriodDirty.getString("appreciation_matiere_periode"));
    }

    public Long getIdPeriod() {
        return idPeriod;
    }

    public void setIdPeriod(Long idPeriod) {
        this.idPeriod = idPeriod;
    }

    public String getAppreciation() {
        return appreciation;
    }

    public void setAppreciation(String appreciation) {
        this.appreciation = appreciation;
    }

    public String getIdClassSchool() {
        return idClassSchool;
    }

    public void setIdClassSchool(String idClassSchool) {
        this.idClassSchool = idClassSchool;
    }

    public String getIdStudent() {
        return idStudent;
    }

    public void setIdStudent(String idStudent) {
        this.idStudent = idStudent;
    }

    public String getIdSubject() {
        return idSubject;
    }

    public void setIdSubject(String idSubject) {
        this.idSubject = idSubject;
    }

    @Override
    public JsonObject toJsonObject() {
        return new JsonObject()
                .put("idClasse", idClassSchool)
                .put("idEleve", idStudent)
                .put("idPeriode", idPeriod)
                .put("idMatiere", idSubject)
                .put("appreciation_matiere_periode", appreciation);
    }

}
