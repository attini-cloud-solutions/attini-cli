package se.attini.util;

public class StringUtils {

    public static String pad(String value, int length) {
        return " ".repeat(length - Math.min(value.length(), length)) + value;
    }

    public static String padStrict(String value, int length) {
        return " ".repeat(length) + value;
    }


    public static String cut(String value, int maxLength) {
        int effectiveMaxLength = Math.max(maxLength, 5);
        if (value.length() > effectiveMaxLength) {
            return value.substring(0, effectiveMaxLength - 3) + "...";
        }

        return value;
    }

}
