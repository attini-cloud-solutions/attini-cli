package se.attini.deployment;

import java.util.Optional;

import se.attini.ClientWithEnvironmentRequest;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;

public class ContinueDeploymentRequest implements ClientWithEnvironmentRequest {

    private final EnvironmentName environmentName;
    private final DistributionName distributionName;
    private final String stepName;
    private final String message;
    private final boolean abort;

    private ContinueDeploymentRequest(Builder builder) {
        environmentName = builder.environmentName;
        distributionName = builder.distributionName;
        stepName = builder.stepName;
        message = builder.message;
        abort = builder.abort;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<EnvironmentName> getEnvironment() {
        return Optional.ofNullable(environmentName);
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public String getStepName() {
        return stepName;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }


    public boolean isAbort() {
        return abort;
    }

    public static final class Builder {
        private EnvironmentName environmentName;
        private DistributionName distributionName;
        private String stepName;
        private String message;
        private boolean abort;

        private Builder() {
        }

        public Builder environmentName(EnvironmentName val) {
            environmentName = val;
            return this;
        }

        public Builder distributionName(DistributionName val) {
            distributionName = val;
            return this;
        }

        public Builder stepName(String val) {
            stepName = val;
            return this;
        }

        public Builder message(String val) {
            message = val;
            return this;
        }

        public Builder abort(boolean val) {
            abort = val;
            return this;
        }


        public ContinueDeploymentRequest build() {
            return new ContinueDeploymentRequest(this);
        }
    }
}
