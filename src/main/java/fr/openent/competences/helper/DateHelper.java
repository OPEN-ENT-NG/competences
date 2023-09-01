package fr.openent.competences.helper;

import fr.openent.competences.enums.DateFormat;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class DateHelper {
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
}
