package fr.openent.competences.constants;

public class DateFormat {
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String DATE_TIME_FORMAT_2 = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String TIME_FORMAT_2 = "HH:mm:ss";
    public static final String DB_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private DateFormat() {
        throw new IllegalStateException("Utility class");
    }

}