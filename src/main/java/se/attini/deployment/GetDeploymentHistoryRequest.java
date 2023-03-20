package se.attini.deployment;

import java.util.Optional;

import se.attini.ClientWithEnvironmentRequest;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;

public class GetDeploymentHistoryRequest implements ClientWithEnvironmentRequest {

    private final DistributionName distributionName;
    private final EnvironmentName environment;

    private GetDeploymentHistoryRequest(Builder builder) {
        this.distributionName = builder.distributionName;
        this.environment = builder.environment;
    }

    public static Builder builder() {
        return new Builder();
    }


    public DistributionName getDistributionName() {
        return distributionName;
    }

    public Optional<EnvironmentName> getEnvironment() {
        return Optional.ofNullable(environment);
    }

    public static class Builder {
        private DistributionName distributionName;
        private EnvironmentName environment;

        private Builder() {
        }

        public Builder setDistributionName(DistributionName distributionName) {
            this.distributionName = distributionName;
            return this;
        }

        public Builder setEnvironment(EnvironmentName environment) {
            this.environment = environment;
            return this;
        }

        public GetDeploymentHistoryRequest build() {
            return new GetDeploymentHistoryRequest(this);
        }
    }
}
