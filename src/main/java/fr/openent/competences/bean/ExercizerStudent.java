package fr.openent.competences.bean;

import com.opencsv.bean.CsvBindByName;

public class ExercizerStudent {

    @CsvBindByName(column = "Nom de l'élève", required = true)
    private String studentName;

    @CsvBindByName(column = "Score final", required = true)
    private int note;

    @CsvBindByName(column = "Commentaire")
    private String annotation;

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }
}

