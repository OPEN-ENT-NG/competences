package fr.openent.competences.enums.subjects;

public enum SubjectKey {
    ID("id"),
    RANK("rank"),
    LABEL("label"),
    SOURCE("source"),
    EXTERNAL_ID("externalId"),
    LAST_UPDATE("lastUpdated"),
    CODE("code"),
    ID_STRUCTURE("idStructure"),
    NAME("name"),
    EXTERNAL_ID_SUBJECT("external_id_subject");


    private String subjectElement;

    SubjectKey(String element) {
        this.subjectElement = element;
    }

    public String getString() {
        return this.subjectElement;
    }
}
