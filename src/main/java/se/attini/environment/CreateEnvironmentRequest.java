package se.attini.environment;

import static java.util.Objects.requireNonNull;

import se.attini.domain.EnvironmentName;
import se.attini.domain.EnvironmentType;

public record CreateEnvironmentRequest(EnvironmentName environment, EnvironmentType type) {

    public CreateEnvironmentRequest(EnvironmentName environment,
                                    EnvironmentType type) {
        this.environment = requireNonNull(environment, "environment");
        this.type = requireNonNull(type, "type");
    }

}
