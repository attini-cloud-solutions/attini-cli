package se.attini.stackstatus;


import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;

@Introspected
@ReflectiveAccess
@SuppressWarnings("unused")
public class UnmanagedStack {
    private final String stackName;
    private final String region;
    private final String account;
    private final String removeCommand;

    public UnmanagedStack(String stackName, String region, String account, String removeCommand) {
        this.stackName = stackName;
        this.region = region;
        this.account = account;
        this.removeCommand = removeCommand;
    }

    public String getStackName() {
        return stackName;
    }

    public String getRegion() {
        return region;
    }

    public String getAccount() {
        return account;
    }

    public String getRemoveCommand() {
        return removeCommand;
    }
}
