package se.attini.context;

import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;

@Introspected
@ReflectiveAccess
@SuppressWarnings("unused")
public class EnvironmentContext {
    private final String environmentName;
    private final List<String> warnings;
    private final List<DistributionContext> distributions;


    public EnvironmentContext(String environmentName, List<DistributionContext> distributions, List<String> warnings) {
        this.environmentName = environmentName;
        this.distributions = distributions;
        this.warnings = warnings;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public List<DistributionContext> getDistributions() {
        return distributions;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    @Override
    public String toString() {
        return "EnvironmentContext{" +
               "environmentName='" + environmentName + '\'' +
               ", warnings=" + warnings +
               ", distributions=" + distributions +
               '}';
    }
}
