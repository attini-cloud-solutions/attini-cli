package se.attini.domain;

import static java.util.Objects.requireNonNull;

public record DeploymentPlanStepError(String stepName,
                                      StackName stackName,
                                      String resourceName,
                                      String resourceStatus,
                                      String error,
                                      Region region) {
    public DeploymentPlanStepError(String stepName,
                                   StackName stackName,
                                   String resourceName,
                                   String resourceStatus, String error,
                                   Region region) {
        this.stepName = requireNonNull(stepName, "stepName");
        this.stackName = requireNonNull(stackName, "stackName");
        this.resourceName = requireNonNull(resourceName, "resourceName");
        this.resourceStatus = requireNonNull(resourceStatus, "resourceStatus");
        this.error = requireNonNull(error, "error");
        this.region = requireNonNull(region, "region");
    }
}
