package se.attini.cli;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;

@Introspected
@ReflectiveAccess
public class CliError {

    private final ErrorCode errorCode;
    private final String message;
    @JsonIgnore
    private final Throwable originalException;

    private CliError(ErrorCode errorCode, String message, Throwable originalException) {
        this.errorCode = errorCode;
        this.message = message;
        this.originalException = originalException;
    }

    public static CliError create(ErrorCode errorCode, String message){
        return new CliError(errorCode, message, null);
    }

    public static CliError create(ErrorCode errorCode, String message, Throwable originalException){
        return new CliError(errorCode, message, originalException);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public Optional<Throwable> getOriginalException() {
        return Optional.of(originalException);
    }
}
