package fr.openent.competences.enums;

public enum TypePDF {
    BULLETIN("Bulletin"),
    BFC("BFC");

    private String word ;

    TypePDF(String word) {
        this.word = word ;
    }

    public String getString() {
        return  this.word ;
    }
}
