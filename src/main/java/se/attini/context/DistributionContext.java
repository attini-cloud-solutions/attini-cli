package se.attini.context;

import java.util.List;
import java.util.Map;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;


@Introspected
@ReflectiveAccess
@SuppressWarnings("unused")
public class DistributionContext {
    private final String name;
    private final String distributionId;
    private final List<DeploymentPlanContext> deploymentPlans;
    private final Map<String, String> distributionTags;

    private final String version;

    public DistributionContext(String name,
                               String distributionId,
                               List<DeploymentPlanContext> deploymentPlans,
                               Map<String, String> distributionTags, String version ) {
        this.name = name;
        this.distributionId = distributionId;
        this.deploymentPlans = deploymentPlans;
        this.distributionTags = distributionTags;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getDistributionId() {
        return distributionId;
    }

    public List<DeploymentPlanContext> getDeploymentPlans() {
        return deploymentPlans;
    }

    public Map<String, String> getDistributionTags() {
        return distributionTags;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "DistributionContext{" +
               "name='" + name + '\'' +
               ", distributionId='" + distributionId + '\'' +
               ", deploymentPlans=" + deploymentPlans +
               ", distributionTags=" + distributionTags +
               ", version='" + version + '\'' +
               '}';
    }
}
