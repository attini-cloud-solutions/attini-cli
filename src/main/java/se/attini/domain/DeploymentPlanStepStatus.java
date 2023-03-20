package se.attini.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class DeploymentPlanStepStatus {
    private final String name;
    private final StepStatus stepStatus;
    private final String message;

    private final Instant timestamp;

    public DeploymentPlanStepStatus(String name, StepStatus stepStatus, Instant timestamp) {
        this.name = name;
        this.stepStatus = stepStatus;
        this.message = null;
        this.timestamp = timestamp;
    }
    public DeploymentPlanStepStatus(String name, StepStatus stepStatus, String message, Instant timestamp) {
        this.name = name;
        this.stepStatus = stepStatus;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public StepStatus getStepStatus() {
        return stepStatus;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeploymentPlanStepStatus that = (DeploymentPlanStepStatus) o;
        return Objects.equals(name, that.name) && stepStatus == that.stepStatus && Objects.equals(
                message,
                that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, stepStatus, message);
    }
}
