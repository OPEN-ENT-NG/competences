package fr.openent.competences.model.importservice;

import com.opencsv.bean.CsvBindByName;

public class ExercizerStudent {

    @CsvBindByName(column = "Nom de l'élève", required = true)
    private String studentName;

    @CsvBindByName(column = "Score final", required = true)
    private Double note;

    @CsvBindByName(column = "Commentaire")
    private String annotation;

    private String id;

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Double getNote() {
        return note;
    }

    public void setNote(Double note) {
        this.note = note;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public void setId(String id) {
        this.id = id;
    }

    // id to check if its verified
    public String id() {
       return this.id;
    }
}

