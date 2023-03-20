package se.attini.setup;

import java.util.List;

import software.amazon.awssdk.services.cloudformation.model.Parameter;

public class TemplateParameters {

    static final String ENVIRONMENT_PARAMETER_NAME = "EnvironmentParameterName";
    static final String EMAIL = "Email";
    static final String RETAIN_DISTRIBUTION_DAYS = "RetainDistributionDays";
    static final String RETAIN_DISTRIBUTION_VERSIONS = "RetainDistributionVersions";
    static final String GIVE_ADMIN_ACCESS = "GiveAdminAccess";
    static final String INIT_DEPLOY_ARN = "InitDeployRoleArn";
    static final String CREATE_DEPLOYMENT_PLAN_DEFAULT_ROLE = "CreateDeploymentPlanDefaultRole";
    static final String CREATE_INIT_DEPLOY_DEFAULT_ROLE = "CreateInitDeployDefaultRole";
    static final String LOG_LVL = "LogLevel";
    static final String VPC_ID = "VpcId";
    static final String SUBNET_IDS = "SubnetsIds";
    static final String AUTO_UPDATE = "AutoUpdate";
    static final String LICENCE_TOKEN = "LicenseToken";

    static final String ACCEPT_LICENSE_AGREEMENT = "AcceptLicenseAgreement";
    static final String RESOURCE_ALLOCATION = "ResourceAllocation";

    static final String ATTACH_LEAST_PRIVILEGE_INIT_DEPLOY_POLICY = "AttachLeastPrivilegePolicyToInitDeployRole";

    static Parameter toParameter(String key, String value) {
        return Parameter.builder()
                        .parameterKey(key)
                        .parameterValue(value)
                        .build();
    }

    static Parameter toParameter(String key, Boolean value) {
        return Parameter.builder()
                        .parameterKey(key)
                        .parameterValue(value ? "true" : "false")
                        .build();
    }

    static Parameter toParameter(String key, Integer value) {
        return Parameter.builder().parameterKey(key)
                        .parameterValue(String.valueOf(value))
                        .build();
    }

    static Parameter toParameter(String key, List<String> value) {
        return Parameter.builder().parameterKey(key)
                        .parameterValue(String.join(",", value))
                        .build();
    }
}
