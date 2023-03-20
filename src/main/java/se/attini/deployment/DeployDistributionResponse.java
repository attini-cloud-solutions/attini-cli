package se.attini.deployment;

import se.attini.domain.DistributionName;
import se.attini.domain.Environment;
import se.attini.domain.ObjectIdentifier;

public class DeployDistributionResponse {
    private final ObjectIdentifier objectIdentifier;
    private final Environment environment;
    private final DistributionName distributionName;

    private DeployDistributionResponse(Builder builder) {
        this.objectIdentifier = builder.objectIdentifier;
        this.environment = builder.environment;
        this.distributionName = builder.distributionName;
    }

    public static Builder builder() {
        return new Builder();
    }


    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public static class Builder {
        private ObjectIdentifier objectIdentifier;
        private Environment environment;
        private DistributionName distributionName;

        private Builder() {
        }

        public Builder setObjectIdentifier(ObjectIdentifier objectIdentifier) {
            this.objectIdentifier = objectIdentifier;
            return this;
        }

        public Builder setEnvironment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder setDistributionName(DistributionName distributionName) {
            this.distributionName = distributionName;
            return this;
        }

        public DeployDistributionResponse build() {
            return new DeployDistributionResponse(this);
        }
    }
}
