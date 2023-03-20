package se.attini;

public class InvalidCredentialsException extends RuntimeException{

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
