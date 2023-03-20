package se.attini.context;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;

@Introspected
@ReflectiveAccess
public class DeploymentPlanContext {
    private final String name;
    private final String lastStatus;
    private final String lastExecutionStart;
    private final String error;

    public DeploymentPlanContext(String name, String error) {
        this.name = name;
        this.lastStatus = null;
        this.lastExecutionStart = null;
        this.error = error;
    }

    public DeploymentPlanContext(String name, String lastStatus, String lastExecutionStart) {
        this.name = name;
        this.lastStatus = lastStatus;
        this.lastExecutionStart = lastExecutionStart;
        this.error = null;
    }

    public String getName() {
        return name;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public String getLastExecutionStart() {
        return lastExecutionStart;
    }

    public String getError() {
        return error;
    }
}
