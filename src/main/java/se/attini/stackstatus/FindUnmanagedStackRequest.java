package se.attini.stackstatus;

import java.util.Optional;

import se.attini.ClientWithEnvironmentRequest;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;

public class FindUnmanagedStackRequest implements ClientWithEnvironmentRequest {

    private final EnvironmentName environment;
    private final DistributionName distributionName;

    private FindUnmanagedStackRequest(Builder builder) {
        this.environment = builder.environment;
        this.distributionName = builder.distributionName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<EnvironmentName> getEnvironment() {
        return Optional.ofNullable(environment);
    }

    public Optional<DistributionName> getDistributionName() {
        return Optional.ofNullable(distributionName);
    }


    public static class Builder {
        private EnvironmentName environment;
        private DistributionName distributionName;

        private Builder() {
        }

        public Builder setEnvironment(EnvironmentName environment) {
            this.environment = environment;
            return this;
        }

        public Builder setDistributionName(DistributionName distributionName) {
            this.distributionName = distributionName;
            return this;
        }

        public FindUnmanagedStackRequest build() {
            return new FindUnmanagedStackRequest(this);
        }
    }
}
