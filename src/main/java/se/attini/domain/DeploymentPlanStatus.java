package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class DeploymentPlanStatus {

    private final String deploymentPlanName;
    private final List<DeploymentPlanStepStatus> completedSteps;
    private final List<DeploymentPlanStepStatus> startedSteps;
    private final String deploymentPlanStatus;
    private final Instant startTime;
    private final Instant endTime;

    private DeploymentPlanStatus(String deploymentPlanName,
                                 List<DeploymentPlanStepStatus> completedSteps,
                                 List<DeploymentPlanStepStatus> startedSteps,
                                 String deploymentPlanStatus,
                                 Instant startTime,
                                 Instant endTime) {
        this.deploymentPlanName = requireNonNull(deploymentPlanName, "deploymentPlanName");
        this.completedSteps = requireNonNull(completedSteps, "completedSteps");
        this.startedSteps = requireNonNull(startedSteps, "runningSteps");
        this.deploymentPlanStatus = requireNonNull(deploymentPlanStatus, "deploymentPlanStatus");
        this.startTime = requireNonNull(startTime, "startTime");
        this.endTime = endTime;
    }

    public String getDeploymentPlanName() {
        return deploymentPlanName;
    }

    public List<DeploymentPlanStepStatus> getCompletedSteps() {
        return completedSteps;
    }

    public List<DeploymentPlanStepStatus> getStartedSteps() {
        return startedSteps;
    }

    public String getDeploymentPlanStatus() {
        return deploymentPlanStatus;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Optional<Instant> getEndTime() {
        return Optional.ofNullable(endTime);
    }

    public static DeploymentPlanStatus create(List<DeploymentPlanStepStatus> completedSteps,
                                              List<DeploymentPlanStepStatus> runningSteps,
                                              String deploymentPlanStatus,
                                              String deploymentPlanName, Instant startTime) {
        return new DeploymentPlanStatus(deploymentPlanName, completedSteps, runningSteps, deploymentPlanStatus, startTime, null);
    }

    public static DeploymentPlanStatus create(List<DeploymentPlanStepStatus> completedSteps,
                                              List<DeploymentPlanStepStatus> runningSteps,
                                              String deploymentPlanStatus,
                                              String deploymentPlanName,
                                              Instant startTime,
                                              Instant endTime) {
        return new DeploymentPlanStatus(deploymentPlanName, completedSteps, runningSteps, deploymentPlanStatus, startTime, endTime);
    }
}
