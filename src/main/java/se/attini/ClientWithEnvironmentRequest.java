package se.attini;

import java.util.Optional;

import se.attini.domain.EnvironmentName;
public interface ClientWithEnvironmentRequest {

    Optional<EnvironmentName> getEnvironment();

}
