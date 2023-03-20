package se.attini.deployment.file.config;

public class AttiniConfigFileException extends RuntimeException {


    public AttiniConfigFileException(String message) {
        super(createErrorMessage(message));
    }

    private static String createErrorMessage(String message) {
        return "Attini configuration file error: %s".formatted(message);
    }

    public AttiniConfigFileException(String message, Throwable cause) {
        super(createErrorMessage(message), cause);
    }
}

