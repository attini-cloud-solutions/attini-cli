package se.attini.deployment;

public class DeploymentAbortedException extends RuntimeException {
    public DeploymentAbortedException(String message) {
        super(message);
    }
}
