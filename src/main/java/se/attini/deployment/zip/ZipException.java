package se.attini.deployment.zip;

public class ZipException extends RuntimeException {
    public ZipException(String message) {
        super(message);
    }

    public ZipException(Exception e) {
        super(e);
    }
}
