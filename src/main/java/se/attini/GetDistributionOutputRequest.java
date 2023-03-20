package se.attini;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;

public class GetDistributionOutputRequest implements ClientWithEnvironmentRequest {

    private final EnvironmentName environment;
    private final DistributionName distributionName;

    private final DistributionId distributionId;


    public GetDistributionOutputRequest(DistributionName distributionName,
                                        EnvironmentName environment,
                                        DistributionId distributionId) {
        this.distributionName = requireNonNull(distributionName, "distributionName");
        this.environment  = environment;
        this.distributionId = distributionId;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    @Override
    public Optional<EnvironmentName> getEnvironment() {
        return Optional.ofNullable(environment);
    }

    public Optional<DistributionId> getDistributionId() {
        return Optional.ofNullable(distributionId);
    }
}
