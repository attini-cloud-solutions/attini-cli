package se.attini.deployment;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import se.attini.ClientWithEnvironmentRequest;
import se.attini.domain.EnvironmentName;
import se.attini.domain.FilePath;

public class CreateAndDeployDistributionRequest implements ClientWithEnvironmentRequest {

    private final FilePath path;
    private final EnvironmentName environment;
    private final boolean forceDeployment;

    private final boolean json;

    private CreateAndDeployDistributionRequest(Builder builder) {
        this.path = requireNonNull(builder.path, "path");
        this.environment =builder.environment;
        this.forceDeployment =builder.forceDeployment;
        this.json =builder.json;
    }

    public static Builder builder() {
        return new Builder();
    }


    public FilePath getPath() {
        return path;
    }

    public Optional<EnvironmentName> getEnvironment() {
        return Optional.ofNullable(environment);
    }

    public boolean forceDeployment() {
        return forceDeployment;
    }

    public boolean isJson() {
        return json;
    }

    public static class Builder {
        private FilePath path;
        private EnvironmentName environment;
        private boolean forceDeployment;

        private boolean json;


        private Builder() {
        }

        public Builder setPath(FilePath path) {
            this.path = path;
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

        public Builder setJson(boolean json) {
            this.json = json;
            return this;
        }

        public CreateAndDeployDistributionRequest build() {
            return new CreateAndDeployDistributionRequest(this);
        }
    }
}
