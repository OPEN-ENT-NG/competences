package fr.openent.competences.helper;

public class NumberHelper {

    private NumberHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static double roundUp(double number, double precision) {
        return Math.round(number * precision) / precision;
    }
    public static double roundUpTenth(double number) {
        return roundUp(number, 1e1);
    }
}
