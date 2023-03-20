package se.attini.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Deployment {

    private final Distribution distribution;
    private final Region region;
    private final EnvironmentName environment;
    private final Instant deployTime;
    private final DeploymentError deploymentError;
    private final StackName stackName;
    private final ExecutionArn executionArn;
    private final DeploymentPlanCount deploymentPlanCount;
    private final List<InitStackError> initStackErrors;
    private final List<DeploymentPlanStepError> deploymentPlanStepErrors;
    private final boolean initStackUnchanged;

    private final Map<String, String> distributionTags;

    private final String deploymentPlanStatus;

    private final Map<String, String> attiniSteps;

    private final String version;


    private Deployment(Builder builder) {
        this.distribution = builder.distribution;
        this.region = builder.region;
        this.environment = builder.environment;
        this.deployTime = builder.deployTime;
        this.deploymentError = builder.deploymentError;
        this.stackName = builder.stackName;
        this.executionArn = builder.executionArn;
        this.deploymentPlanCount = builder.deploymentPlanCount;
        this.initStackErrors = builder.initStackErrors;
        this.deploymentPlanStepErrors = builder.deploymentPlanStepErrors;
        this.initStackUnchanged = builder.initStackUnchanged;
        this.distributionTags =builder.distributionTags;
        this.deploymentPlanStatus = builder.deploymentPlanStatus;
        this.attiniSteps = builder.attiniSteps;
        this.version =builder.version;
    }

    public static Builder builder() {
        return new Builder();
    }


    public Distribution getDistribution() {
        return distribution;
    }

    public Optional<Region> getRegion() {
        return Optional.ofNullable(region);
    }

    public Optional<DeploymentError> getDeploymentError() {
        return Optional.ofNullable(deploymentError);
    }

    public EnvironmentName getEnvironment() {
        return environment;
    }

    public Instant getDeployTime() {
        return deployTime;
    }

    public Optional<StackName> getStackName() {
        return Optional.ofNullable(stackName);
    }

    public Optional<ExecutionArn> getExecutionArn() {
       return Optional.ofNullable(executionArn);
    }

    public List<InitStackError> getInitStackErrors() {
        return initStackErrors != null ? initStackErrors : Collections.emptyList();
    }

    public List<DeploymentPlanStepError> getDeploymentPlanStepErrors() {
        return deploymentPlanStepErrors != null ? deploymentPlanStepErrors : Collections.emptyList();
    }

    public Map<String, String> getDistributionTags() {
        return distributionTags == null ? Collections.emptyMap() : distributionTags;
    }

    public Optional<DeploymentPlanCount> getDeploymentPlanCount() {
        return Optional.ofNullable(deploymentPlanCount);
    }

    public boolean isInitStackUnchanged() {
        return initStackUnchanged;
    }

    public Optional<String> getDeploymentPlanStatus() {
        return Optional.ofNullable(deploymentPlanStatus);
    }

    public Map<String, String> getAttiniSteps() {
        return attiniSteps == null ? Collections.emptyMap() : attiniSteps;
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Deployment that = (Deployment) o;
        return initStackUnchanged == that.initStackUnchanged && Objects.equals(distribution,
                                                                               that.distribution) && Objects.equals(
                region,
                that.region) && Objects.equals(environment, that.environment) && Objects.equals(
                deployTime,
                that.deployTime) && Objects.equals(deploymentError,
                                                   that.deploymentError) && Objects.equals(stackName,
                                                                                           that.stackName) && Objects.equals(
                executionArn,
                that.executionArn) && Objects.equals(deploymentPlanCount,
                                                     that.deploymentPlanCount) && Objects.equals(
                initStackErrors,
                that.initStackErrors) && Objects.equals(deploymentPlanStepErrors,
                                                        that.deploymentPlanStepErrors) && Objects.equals(
                distributionTags,
                that.distributionTags) && Objects.equals(deploymentPlanStatus,
                                                         that.deploymentPlanStatus) && Objects.equals(
                attiniSteps,
                that.attiniSteps) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distribution,
                            region,
                            environment,
                            deployTime,
                            deploymentError,
                            stackName,
                            executionArn,
                            deploymentPlanCount,
                            initStackErrors,
                            deploymentPlanStepErrors,
                            initStackUnchanged,
                            distributionTags,
                            deploymentPlanStatus,
                            attiniSteps,
                            version);
    }

    @Override
    public String toString() {
        return "Deployment{" +
               "distribution=" + distribution +
               ", region=" + region +
               ", environment=" + environment +
               ", deployTime=" + deployTime +
               ", deploymentError=" + deploymentError +
               ", stackName=" + stackName +
               ", executionArn=" + executionArn +
               ", deploymentPlanCount=" + deploymentPlanCount +
               ", initStackErrors=" + initStackErrors +
               ", deploymentPlanStepErrors=" + deploymentPlanStepErrors +
               ", initStackUnchanged=" + initStackUnchanged +
               ", distributionTags=" + distributionTags +
               ", deploymentPlanStatus='" + deploymentPlanStatus + '\'' +
               ", attiniSteps=" + attiniSteps +
               ", version='" + version + '\'' +
               '}';
    }

    public static class Builder {
        private Distribution distribution;
        private Region region;
        private EnvironmentName environment;
        private Instant deployTime;
        private DeploymentError deploymentError;
        private StackName stackName;
        private ExecutionArn executionArn;
        private DeploymentPlanCount deploymentPlanCount;
        private List<InitStackError> initStackErrors;
        private List<DeploymentPlanStepError> deploymentPlanStepErrors;
        private boolean initStackUnchanged;

        private Map<String, String> distributionTags;

        private String deploymentPlanStatus;

        private Map<String, String> attiniSteps;

        private String version;



        private Builder() {
        }

        public Builder setExecutionArn(ExecutionArn executionArn) {
            this.executionArn = executionArn;
            return this;
        }

        public Builder setStackName(StackName stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder setDeploymentError(DeploymentError deploymentError) {
            this.deploymentError = deploymentError;
            return this;
        }

        public Builder setDistribution(Distribution distribution) {
            this.distribution = distribution;
            return this;
        }

        public Builder setRegion(Region region) {
            this.region = region;
            return this;
        }

        public Builder setEnvironment(EnvironmentName environment) {
            this.environment = environment;
            return this;
        }

        public Builder setDeployTime(Instant deployTime) {
            this.deployTime = deployTime;
            return this;
        }

        public Builder setDeploymentPlanCount(DeploymentPlanCount deploymentPlanCount) {
            this.deploymentPlanCount = deploymentPlanCount;
            return this;
        }

        public Builder setInitStackErrors(List<InitStackError> initStackErrors) {
            this.initStackErrors = initStackErrors;
            return this;
        }

        public Builder setDeploymentPlanStepErrors(List<DeploymentPlanStepError> deploymentPlanStepErrors) {
            this.deploymentPlanStepErrors = deploymentPlanStepErrors;
            return this;
        }

        public Builder setInitStackUnchanged(boolean initStackUnchanged) {
            this.initStackUnchanged = initStackUnchanged;
            return this;
        }

        public Builder setDistributionTags(Map<String, String> distributionTags) {
            this.distributionTags = distributionTags;
            return this;
        }

        public Builder setDeploymentPlanStatus(String deploymentPlanStatus) {
            this.deploymentPlanStatus = deploymentPlanStatus;
            return this;
        }


        public Builder setAttiniSteps(Map<String,String> attiniSteps) {
            this.attiniSteps = attiniSteps;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }



        public Deployment build() {
            return new Deployment(this);
        }
    }
}
