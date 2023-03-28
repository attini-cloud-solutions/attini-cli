package se.attini.stackstatus;

import java.util.Collections;
import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;

@Introspected
@ReflectiveAccess
@SuppressWarnings("unused")
public class UnmanagedDistributionStacks {

    private final String distributionName;
    private final List<UnmanagedStack> unmanagedStacks;
    private final String message;

    public UnmanagedDistributionStacks(String distributionName, String message) {
        this.distributionName = distributionName;
        this.unmanagedStacks = Collections.emptyList();
        this.message = message;
    }

    public UnmanagedDistributionStacks(String distributionName, List<UnmanagedStack> unmanagedStacks) {
        this.distributionName = distributionName;
        this.unmanagedStacks = unmanagedStacks;
        this.message = null;
    }

    public String getDistributionName() {
        return distributionName;
    }

    public List<UnmanagedStack> getUnmanagedStacks() {
        return unmanagedStacks;
    }

    public String getMessage() {
        return message;
    }
}
