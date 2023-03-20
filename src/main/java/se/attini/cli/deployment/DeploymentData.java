package se.attini.cli.deployment;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;
import se.attini.domain.Deployment;
import se.attini.domain.DeploymentError;

@Introspected
@ReflectiveAccess
public class DeploymentData {

    private final String distributionName;
    private final String distributionId;
    private final String environment;
    private final String deploymentTime;
    private final String errorCode;
    private final String deploymentPlanStatus;
    private final Map<String, String> distributionTags;
    private final String version;

    public DeploymentData(Deployment deployment) {
        this.distributionId = deployment.getDistribution().getDistributionId().getId();
        this.distributionName = deployment.getDistribution().getDistributionName().getName();
        this.environment = deployment.getEnvironment().getName();
        this.deploymentTime = deployment.getDeployTime()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDateTime()
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        this.errorCode = deployment.getDeploymentError().map(DeploymentError::getErrorCode).orElse(null);
        this.distributionTags = deployment.getDistributionTags();
        this.deploymentPlanStatus = deployment.getDeploymentPlanStatus().orElse(null);
        this.version = deployment.getVersion().orElse(null);
    }

    public String getDistributionId() {
        return distributionId;
    }

    public String getDistributionName() {
        return distributionName;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getDeploymentTime() {
        return deploymentTime;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, String> getDistributionTags() {
        return distributionTags;
    }

    public String getDeploymentPlanStatus() {
        return deploymentPlanStatus;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "DeploymentData{" +
               "distributionId='" + distributionId + '\'' +
               ", distributionName='" + distributionName + '\'' +
               ", environment='" + environment + '\'' +
               ", deploymentTime='" + deploymentTime + '\'' +
               ", errorCode='" + errorCode + '\'' +
               ", deploymentPlanStatus='" + deploymentPlanStatus + '\'' +
               ", distributionTags=" + distributionTags +
               ", version='" + version + '\'' +
               '}';
    }
}
