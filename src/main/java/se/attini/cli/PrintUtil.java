package se.attini.cli;

public class PrintUtil {
    private static String ansiRed = "\u001B[31;1m";
    private static String ansiGreen = "\u001B[32;1m";
    private static String ansiYellow = "\u001B[33;1m";
    private static String ansiBlue = "\u001B[34;1m";
    private static String ansiReset = "\u001B[0m";

    public enum Color {
        RED, GREEN, YELLOW,BLUE, DEFAULT
    }

    public static String toColor(String value, Color color) {
        return switch (color) {
            case RED -> toRed(value);
            case GREEN -> toGreen(value);
            case YELLOW -> toYellow(value);
            case BLUE ->  toBlue(value);
            default -> value;
        };
    }

    public static String toBlue(String string){
        return ansiBlue + string + ansiReset;
    }

    public static String toYellow(String string) {
        return ansiYellow + string + ansiReset;
    }

    public static String toGreen(String string) {
        return ansiGreen + string + ansiReset;
    }

    public static String toRed(String string) {
        return ansiRed + string + ansiReset;
    }

    public static void clearColors() {
        ansiRed = "";
        ansiGreen = "";
        ansiYellow = "";
        ansiBlue = "";
        ansiReset = "";

    }
}
