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
    public MultiTeaching() {
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

    //    "id":23549,
//            "structure_id":"92feb6f1-2016-4215-b53f-5337fbcba244",
//            "main_teacher_id":"3cbf7852-8e60-4934-9a63-f534a0c1232c",
//            "second_teacher_id":"2f5ef34b-dc45-4cfc-82ae-baad55a60706",
//            "subject_id":"1790293-1566493593334",
//            "class_or_group_id":"36e6eeb8-3a90-497b-8b6a-18ac9d0f3cb0",
//            "start_date":null,
//            "end_date":null,
//            "entered_end_date":null,
//            "is_coteaching":true,
//            "is_visible":true,
//            "id_etablissement":"92feb6f1-2016-4215-b53f-5337fbcba244",
//            "libelle":null,
//            "timestamp_dt":"2021-09-02T00:00:00.000",
//            "timestamp_fn":"2021-12-08T00:00:00.000",
//            "date_fin_saisie":"2021-12-08T00:00:00.000",
//            "id_classe":"36e6eeb8-3a90-497b-8b6a-18ac9d0f3cb0",
//            "id_type":3,
//            "date_conseil_classe":"2021-12-08T00:00:00.000",
//            "publication_bulletin":false
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
                .put("id_type",type)
                .put("date_conseil_classe",dateConseilClass)
                .put("publication_bulletin",publicationBulletin)
                ;
    }


    public void setIdInteger(Integer id) {
        this.idInt = id;
    }
}
