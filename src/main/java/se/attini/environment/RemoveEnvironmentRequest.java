package se.attini.environment;

import static java.util.Objects.requireNonNull;

import se.attini.domain.EnvironmentName;

public record RemoveEnvironmentRequest(EnvironmentName environment) {

    public RemoveEnvironmentRequest(EnvironmentName environment) {
        this.environment = requireNonNull(environment, "environment");
    }
}
