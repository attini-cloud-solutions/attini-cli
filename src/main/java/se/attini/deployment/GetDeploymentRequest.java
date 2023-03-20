package se.attini.deployment;

import se.attini.domain.DistributionName;
import se.attini.domain.Environment;
import se.attini.domain.ObjectIdentifier;

public class GetDeploymentRequest {

    private final ObjectIdentifier objectIdentifier;
    private final Environment environment;
    private final DistributionName distributionName;

    private GetDeploymentRequest(Builder builder) {
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

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public Environment getEnvironment() {
        return environment;
    }


    public static class Builder {
        private ObjectIdentifier objectIdentifier;
        private Environment environment;
        private DistributionName distributionName;

        private Builder() {
        }

        public Builder setDistributionName(DistributionName distributionName) {
            this.distributionName = distributionName;
            return this;
        }

        public Builder setObjectIdentifier(ObjectIdentifier objectIdentifier) {
            this.objectIdentifier = objectIdentifier;
            return this;
        }

        public Builder setEnvironment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public GetDeploymentRequest build() {
            return new GetDeploymentRequest(this);
        }
    }

    @Override
    public String toString() {
        return "GetDeploymentRequest{" +
               "objectIdentifier=" + objectIdentifier +
               ", environment=" + environment +
               ", distributionName=" + distributionName +
               '}';
    }
}
