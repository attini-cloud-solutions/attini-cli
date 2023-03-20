package se.attini.deployment;

import java.util.Objects;

import se.attini.domain.DistributionName;
import se.attini.domain.ObjectIdentifier;
import se.attini.domain.Environment;

public class FollowDeploymentRequest {
    private final Environment environment;
    private final ObjectIdentifier objectIdentifier;
    private final DistributionName distributionName;

    private FollowDeploymentRequest(Builder builder) {
        this.environment = builder.environment;
        this.objectIdentifier = builder.objectIdentifier;
        this.distributionName = builder.distributionName;
    }

    public static Builder builder() {
        return new Builder();
    }


    public Environment getEnvironment() {
        return environment;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public static class Builder {
        private Environment environment;
        private ObjectIdentifier objectIdentifier;
        private DistributionName distributionName;

        private Builder() {
        }

        public Builder setEnvironment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder setObjectIdentifier(ObjectIdentifier objectIdentifier) {
            this.objectIdentifier = objectIdentifier;
            return this;
        }

        public Builder setDistributionName(DistributionName distributionName) {
            this.distributionName = distributionName;
            return this;
        }

        public Builder of(FollowDeploymentRequest followDeploymentRequest) {
            this.environment = followDeploymentRequest.environment;
            this.objectIdentifier = followDeploymentRequest.objectIdentifier;
            this.distributionName = followDeploymentRequest.distributionName;
            return this;
        }

        public FollowDeploymentRequest build() {
            return new FollowDeploymentRequest(this);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FollowDeploymentRequest that = (FollowDeploymentRequest) o;
        return Objects.equals(environment, that.environment) && Objects.equals(objectIdentifier,
                                                                               that.objectIdentifier) && Objects.equals(
                distributionName,
                that.distributionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environment, objectIdentifier, distributionName);
    }

    @Override
    public String toString() {
        return "FollowDeploymentRequest{" +
               "environment=" + environment +
               ", objectIdentifier=" + objectIdentifier +
               ", distributionName=" + distributionName +
               '}';
    }
}
