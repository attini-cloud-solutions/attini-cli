package se.attini.deployment;

import static java.util.Objects.requireNonNull;

import se.attini.domain.ExecutionArn;

public record GetDeploymentPlanExecutionRequest(ExecutionArn executionArn) {

    public GetDeploymentPlanExecutionRequest(ExecutionArn executionArn) {
        this.executionArn = requireNonNull(executionArn, "executionArn");
    }
}
