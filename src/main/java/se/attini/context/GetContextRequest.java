package se.attini.context;

import java.util.Optional;

import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;

public class GetContextRequest {
    private final EnvironmentName environment;
    private final DistributionName distributionName;

    public GetContextRequest(EnvironmentName environment, DistributionName distributionName) {
        this.environment = environment;
        this.distributionName = distributionName;
    }
    public Optional<EnvironmentName> getEnvironment() {
        return Optional.ofNullable(environment);
    }

    public Optional<DistributionName> getDistributionName() {
        return Optional.ofNullable(distributionName);
    }
}
