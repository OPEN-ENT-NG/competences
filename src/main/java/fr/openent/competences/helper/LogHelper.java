package fr.openent.competences.helper;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.openent.competences.constants.Field.COMPETENCES;

public class LogHelper {

    private LogHelper() {}

    public static String getBaseLog(Class<?> myClass, String methodName) {
        return String.format("[%s@%s::%s]", COMPETENCES, myClass.getSimpleName(), methodName);
    }

    public static Logger getLogger(Class<?> myClass) {
        return LoggerFactory.getLogger(myClass);
    }

    public static void logError(Object classObject, String methodName, String message) {
        Class<?> myClass = classObject.getClass();
        getLogger(myClass).error(String.format("%s %s", getBaseLog(myClass, methodName), message));
    }

    public static void logError(Object classObject, String methodName, String message, String err) {
        Class<?> myClass = classObject.getClass();
        getLogger(myClass).error(String.format("%s %s : %s", getBaseLog(myClass, methodName), message, err));
    }

    public static void logInfo(Object classObject, String methodName, Object object) {
        logInfo(classObject, methodName, object.toString());
    }

    public static void logInfo(Object classObject, String methodName, String message) {
        Class<?> myClass = classObject.getClass();
        getLogger(myClass).info(String.format("%s %s", getBaseLog(myClass, methodName), message));
    }
}