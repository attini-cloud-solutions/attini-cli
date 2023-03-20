package se.attini.pack;

public class AttiniFileSystemException extends RuntimeException {

    public AttiniFileSystemException(String message) {
        super(message);
    }
    public AttiniFileSystemException(String message, Exception e) {
        super(message,e);
    }
}
