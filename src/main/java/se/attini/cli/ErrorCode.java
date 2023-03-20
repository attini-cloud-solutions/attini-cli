package se.attini.cli;

public enum ErrorCode {

    IllegalArgument(151),
    NotConfigured(152),
    ExecutionError(1),
    AttiniFrameworkNotInstalled(153),
    InvalidCredentials(154),
    
    AttiniConfigError(155);

    private final int exitCode;

    ErrorCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
