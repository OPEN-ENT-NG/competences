package fr.openent.competences.helper;

import fr.openent.competences.enums.DateFormat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import static fr.openent.competences.constants.DateFormat.*;

public final class DateHelper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_FORMAT);
    private static final DateTimeFormatter TIME_FORMATTER_2 = DateTimeFormatter.ofPattern(TIME_FORMAT_2);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DB_DATE_TIME_FORMAT);

    private DateHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static LocalDateTime parse(String dateString, String format) {
        return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(format));
    }

    public static LocalDateTime parse(String dateString) {
        return parse(dateString, dateString.contains("T") ? DateFormat.SQL_FORMAT.format(): DateFormat.MONGO_FORMAT.format());
    }

    public static boolean isPeriodContainedWithinAnother(String basePeriodStartString, String basePeriodEndString,
                                   String testPeriodStartString, String testPeriodEndString) {
        LocalDateTime basePeriodStart = parse(basePeriodStartString);
        LocalDateTime basePeriodEnd = parse(basePeriodEndString);
        LocalDateTime testPeriodStart = parse(testPeriodStartString);
        LocalDateTime testPeriodEnd = parse(testPeriodEndString);

        return testPeriodStart.isBefore(basePeriodEnd) && basePeriodStart.isBefore(testPeriodEnd);
    }

    // Date

    public static LocalDate parseDate(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }

    // Time

    public static LocalTime parseTime(String time) {
        if (time == null || time.isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(time, TIME_FORMATTER);
        } catch (DateTimeParseException e1) {
            try {
                return LocalTime.parse(time, TIME_FORMATTER_2);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    public static String formatTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(TIME_FORMATTER);
    }

    // DateTime

    public static LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(dateTime, DB_DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    // Duration

    public static Duration parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) return null;

        try {
            // Case parse from front (ex : '01:00')
            Pattern DURATION_PATTERN = Pattern.compile("^\\d+:\\d{2}$");
            if (!DURATION_PATTERN.matcher(duration).matches()) throw new NumberFormatException();
            String[] parts = duration.split(":");
            long hours = Long.parseLong(parts[0]);
            long minutes = Long.parseLong(parts[1]);
            return Duration.ofHours(hours).plusMinutes(minutes);
        } catch (NumberFormatException e1) {
            try {
                // Case parse from database (ex : '0 years 0 mons 0 days 1 hours 0 mins 0.0 secs')
                String[] parts = duration.split(" ");
                long hours = Long.parseLong(parts[6]); // Extract hours
                long minutes = Long.parseLong(parts[8]); // Extract minutes
                return Duration.ofHours(hours).plusMinutes(minutes);
            } catch (NumberFormatException e2) {
                return null;
            }
        }
    }

    public static String formatDuration(Duration duration) {
        if (duration == null || duration == Duration.ZERO) {
            return null;
        }
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        return String.format("%02d:%02d", hours, minutes);
    }
}
