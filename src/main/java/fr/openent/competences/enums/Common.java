package fr.openent.competences.enums;

public enum Common {
    ERROR("Error"),
    INFO("Info"),
    INVALID_PARAMETERS ("Invalid parameters"),
    ZERO("0");

    private String word ;

    Common(String word) {
        this.word = word ;
    }

    public String getString() {
        return  this.word ;
    }
}
