package fr.openent.competences.enums.subjects;

public enum SubjectOther {
    DRAG_DOWN("dragDown"),
    DRAG_UP("dragUp"),
    INDEX_END("indexEnd"),
    INDEX_START("indexStart")
    ;

    private String subjectElement;

    SubjectOther(String element) {
        this.subjectElement = element;
    }

    public String getString() {
        return this.subjectElement;
    }
}

