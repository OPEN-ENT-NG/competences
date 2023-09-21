package fr.openent.competences.enums;

public enum DateFormat {
    SQL_FORMAT("yyyy-MM-dd'T'HH:mm:ss.SSS"),
    MONGO_FORMAT("yyyy-MM-dd HH:mm:ss.SSS");
    private String format;
    DateFormat(String format) {
        this.format = format;
    }

    public String format() {
        return format;
    }
}
