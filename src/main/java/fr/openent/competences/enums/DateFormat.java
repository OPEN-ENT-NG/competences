package fr.openent.competences.enums;

public enum DateFormat {
    SQL_FORMAT("yyyy-MM-dd'T'HH:mm:ss"),
    MONGO_FORMAT("yyyy-MM-dd HH:mm:ss");
    private String format;
    DateFormat(String format) {
        this.format = format;
    }

    public String format() {
        return format;
    }
}
