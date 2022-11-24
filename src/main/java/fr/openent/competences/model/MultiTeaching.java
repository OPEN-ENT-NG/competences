package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class MultiTeaching extends Model {

    Integer idInt;
    Structure structure;
    Teacher mainTeacher;
    Teacher secondTeacher;
    Subject subject;
    String groupOrClassId;
    String startDate;
    String endDate;
    String enteredEndDate;
    boolean isCoTeaching;
    boolean visible;
    String libelle;
    String timestampDt;
    String timestampFn;
    String endDateSaisie;

    Classe classe;
    int type;
    String dateConseilClass;
    boolean publicationBulletin;

    private boolean isDeleted ;

    public MultiTeaching() {
        isDeleted = false;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    public Teacher getMainTeacher() {
        return mainTeacher;
    }

    public void setMainTeacher(Teacher mainTeacher) {
        this.mainTeacher = mainTeacher;
    }

    public Teacher getSecondTeacher() {
        return secondTeacher;
    }

    public void setSecondTeacher(Teacher secondTeacher) {
        this.secondTeacher = secondTeacher;
    }

    public String getGroupOrClassId() {
        return groupOrClassId;
    }

    public void setGroupOrClassId(String groupOrClassId) {
        this.groupOrClassId = groupOrClassId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getEnteredEndDate() {
        return enteredEndDate;
    }

    public void setEnteredEndDate(String enteredEndDate) {
        this.enteredEndDate = enteredEndDate;
    }

    public boolean isCoTeaching() {
        return isCoTeaching;
    }

    public void setCoTeaching(boolean coTeaching) {
        isCoTeaching = coTeaching;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getTimestampDt() {
        return timestampDt;
    }

    public void setTimestampDt(String timestampDt) {
        this.timestampDt = timestampDt;
    }

    public String getTimestampFn() {
        return timestampFn;
    }

    public void setTimestampFn(String timestampFn) {
        this.timestampFn = timestampFn;
    }

    public String getEndDateSaisie() {
        return endDateSaisie;
    }

    public void setEndDateSaisie(String endDateSaisie) {
        this.endDateSaisie = endDateSaisie;
    }

    public Classe getClasse() {
        return classe;
    }

    public void setClasse(Classe classe) {
        this.classe = classe;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDateConseilClass() {
        return dateConseilClass;
    }

    public void setDateConseilClass(String dateConseilClass) {
        this.dateConseilClass = dateConseilClass;
    }

    public boolean isPublicationBulletin() {
        return publicationBulletin;
    }

    public void setPublicationBulletin(boolean publicationBulletin) {
        this.publicationBulletin = publicationBulletin;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }


    @Override
    public JsonObject toJsonObject() {

        return new JsonObject().put("id",idInt)
                .put("structure_id",structure.getId())
                .put("main_teacher_id",mainTeacher.getId())
                .put("second_teacher_id",secondTeacher.getId())
                .put("subject_id",subject.getId())
                .put("class_or_group_id",groupOrClassId)
                .put("start_date",startDate)
                .put("end_date",endDate)
                .put("entered_end_date",enteredEndDate)
                .put("is_coteaching",isCoTeaching)
                .put("is_visible",visible)
                .put("id_etablissement",structure.getId())
                .put("libelle",libelle)
                .put("timestamp_dt",timestampDt)
                .put("timestamp_fn",timestampFn)
                .put("date_fin_saisie",endDateSaisie)
                .put("id_classe",classe.getId())
//                .put("id_type",type)
                .put("date_conseil_classe",dateConseilClass)
//                .put("publication_bulletin",publicationBulletin)
                ;
    }


    public void setIdInteger(Integer id) {
        this.idInt = id;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public boolean isDeleted() {
        return isDeleted;
    }
}
