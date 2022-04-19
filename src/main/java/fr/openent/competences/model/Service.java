package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class Service {
    private Structure structure;
    private Group group;
    private Matiere matiere;
    private String modalite;
    private String order;
    private boolean evaluable;
    private boolean visible;
    private Teacher teacher;
    private Long coefficient;

    public Service() {
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Matiere getMatiere() {
        return matiere;
    }

    public void setMatiere(Matiere matiere) {
        this.matiere = matiere;
    }

    public String getModalite() {
        return modalite;
    }

    public void setModalite(String modalite) {
        this.modalite = modalite;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public boolean isEvaluable() {
        return evaluable;
    }

    public void setEvaluable(boolean evaluable) {
        this.evaluable = evaluable;
    }

    public boolean isVisible() {
        return visible;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Long getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(Long coefficient) {
        this.coefficient = coefficient;
    }

    public JsonObject toJson(){
        return new JsonObject()
                .put("id_etablissement",structure.getId())
                .put("id_enseignant",teacher.getId())
                .put("id_matiere",matiere.getId())
                .put("id_groupe",group.getId())
                .put("ordre",order)
                .put("is_visible",visible)
                .put("evaluable",evaluable)
                .put("modalite",modalite)
                .put("coefficient",coefficient);
    }
}
