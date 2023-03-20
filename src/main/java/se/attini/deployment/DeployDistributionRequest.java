package se.attini.deployment;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import se.attini.ClientWithEnvironmentRequest;
import se.attini.domain.Distribution;
import se.attini.domain.EnvironmentName;

public class DeployDistributionRequest implements ClientWithEnvironmentRequest {

    private final Distribution distribution;
    private final EnvironmentName environment;
    private final boolean forceDeployment;

    private DeployDistributionRequest(Builder builder) {
        this.distribution = requireNonNull(builder.distribution, "distribution");
        this.environment = builder.environment;
        this.forceDeployment = builder.forceDeployment;
    }

    public static Builder builder() {
        return new Builder();
    }


    public Distribution getDistribution() {
        return distribution;
    }

    public Optional<EnvironmentName> getEnvironment() {
        return Optional.ofNullable(environment);
    }

    public boolean isForceDeployment() {
        return forceDeployment;
    }

    public static class Builder {
        private Distribution distribution;
        private EnvironmentName environment;
        private boolean forceDeployment;

        private Builder() {
        }

        public Builder setDistribution(Distribution distribution) {
            this.distribution = distribution;
            return this;
        }

        public Builder setEnvironment(EnvironmentName environment) {
            this.environment = environment;
            return this;
        }

        public Builder setForceDeployment(boolean forceDeployment) {
            this.forceDeployment = forceDeployment;
            return this;
        }

        public DeployDistributionRequest build() {
            return new DeployDistributionRequest(this);
        }
    }
}
