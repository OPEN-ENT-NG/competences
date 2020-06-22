package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class PdfFile extends Model{
    String id_class;
    String id_structure;
    String filename ;


    String id_parent;
    String id_student;
    String id_file;

    public String getId_class() {
        return id_class;
    }

    public void setId_class(String id_class) {
        this.id_class = id_class;
    }
    public String getId_parent() {
        return id_parent;
    }

    public void setId_parent(String id_parent) {
        this.id_parent = id_parent;
    }
    public String getId_structure() {
        return id_structure;
    }

    public void setId_structure(String id_structure) {
        this.id_structure = id_structure;
    }

    public String getId_student() {
        return id_student;
    }

    public void setId_student(String id_student) {
        this.id_student = id_student;
    }

    public String getId_file() {
        return id_file;
    }

    public void setId_file(String id_file) {
        this.id_file = id_file;
    }


    public PdfFile(String id_class, String id_structure, String id_student, String id_file, String name){
        this.id_class=id_class;
        this.id_structure=id_structure;
        this.id_student=id_student;
        this.id_file=id_file;
        this.filename = name;
    }

    @Override
    public JsonObject toJsonObject() {
        return new JsonObject()
                .put("id_class",id_class)
                .put("parent",id_parent)
                .put("id_structure",id_structure)
                .put("id_student",id_student)
                .put("id",id_file)
                .put("file",id_file)
                .put("name",filename)
                .put("type","file");
    }
}
