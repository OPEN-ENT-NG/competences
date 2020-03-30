package fr.openent.competences.enums;

public enum Common {
    ERROR("Error"),
    INFO("Info");

    private String word ;

    Common(String word) {
        this.word = word ;
    }

    public String getString() {
        return  this.word ;
    }
}
