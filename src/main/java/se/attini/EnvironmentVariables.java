package se.attini;

public class EnvironmentVariables {

    public String getAwsRegion() {
        return System.getenv("AWS_REGION");
    }

    public String getShell() {
        return System.getenv("SHELL") != null ? System.getenv("SHELL") : "/bin/bash";
    }

    public boolean isDisableCliVersionCheck() {
        return "true".equalsIgnoreCase(System.getenv("ATTINI_DISABLE_CLI_VERSION_CHECK"));
    }

    public boolean isDisableAnsiColor() {
        return "true".equalsIgnoreCase(System.getenv("ATTINI_DISABLE_ANSI_COLOR"));
    }
}
