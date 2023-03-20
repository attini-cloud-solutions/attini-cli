package se.attini.domain;

public class DeploymentError {
    private final String errorCode;
    private final String errorMessage;

    private DeploymentError(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static DeploymentError create(String errorCode, String errorMessage){
        return new DeploymentError(errorCode, errorMessage);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
