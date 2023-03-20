package se.attini.setup;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import se.attini.domain.Email;
import se.attini.domain.IamRoleArn;
import se.attini.domain.LogLevel;

public class SetupAttiniRequest {

    private final String environmentVariable;
    private final String version;
    private final Email email;
    private final Integer retainDistributionDays;
    private final Integer retainDistributionVersions;
    private final IamRoleArn initDeployArn;
    private final Boolean giveAdminAccess;
    private final Boolean createDeploymentPlanDefaultRole;
    private final String vpcId;
    private final List<String> subnetIds;
    private final LogLevel logLevel;
    private final String autoUpdate;
    private final String token;
    private final Boolean acceptLicenceAgreement;
    private final Boolean useExistingVersion;
    private final Boolean createInitDeployDefaultRole;
    private final String resourceAllocation;

    private final boolean guided;

    private final Boolean disableLeastPrivilegeInitDeployPolicy;

    private SetupAttiniRequest(Builder builder) {
        this.environmentVariable = builder.environmentVariable;
        this.version = builder.version;
        this.email = builder.email;
        this.retainDistributionDays = builder.retainDistributionDays;
        this.retainDistributionVersions = builder.retainDistributionVersions;
        this.initDeployArn = builder.initDeployArn;
        this.giveAdminAccess = builder.giveAdminAccess;
        this.createDeploymentPlanDefaultRole = builder.createDeploymentPlanDefaultRole;
        this.vpcId = builder.vpcId;
        this.subnetIds = builder.subnetIds;
        this.logLevel = builder.logLevel;
        this.autoUpdate = builder.autoUpdate;
        this.token = builder.token;
        this.acceptLicenceAgreement = builder.acceptLicenceAgreement;
        this.useExistingVersion = builder.useExistingVersion;
        this.createInitDeployDefaultRole = builder.createInitDeployDefaultRole;
        this.resourceAllocation = builder.resourceAllocation;
        this.guided = builder.guided;
        this.disableLeastPrivilegeInitDeployPolicy = builder.disableLeastPrivilegeInitDeployPolicy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getEnvironmentVariable() {
        return Optional.ofNullable(environmentVariable);
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    public Optional<Email> getEmail() {
        return Optional.ofNullable(email);
    }

    public Optional<Integer> getRetainDistributionDays() {
        return Optional.ofNullable(retainDistributionDays);
    }


    public Optional<Integer> getRetainDistributionVersions() {
        return Optional.ofNullable(retainDistributionVersions);
    }

    public Optional<IamRoleArn> getInitDeployArn() {
        return Optional.ofNullable(initDeployArn);
    }

    public Optional<Boolean> getGiveAdminAccess() {
        return Optional.ofNullable(giveAdminAccess);
    }

    public Optional<Boolean> getCreateDeploymentPlanDefaultRole() {
        return Optional.ofNullable(createDeploymentPlanDefaultRole);
    }

    public Optional<String> getVpcId() {
        return Optional.ofNullable(vpcId);
    }

    public List<String> getSubnetIds() {
        return subnetIds == null ? Collections.emptyList() : subnetIds;
    }

    public Optional<LogLevel> getLogLevel() {
        return Optional.ofNullable(logLevel);
    }

    public Optional<String> getAutoUpdate() {
        return Optional.ofNullable(autoUpdate);
    }

    public Optional<String> getToken() {
        return Optional.ofNullable(token);
    }

    public Optional<Boolean> getAcceptLicenceAgreement() {
        return Optional.ofNullable(acceptLicenceAgreement);
    }

    public Optional<Boolean> getUseExistingVersion() {
        return Optional.ofNullable(useExistingVersion);
    }

    public Optional<Boolean> getCreateInitDeployDefaultRole() {
        return Optional.ofNullable(createInitDeployDefaultRole);
    }

    public Optional<String> getResourceAllocation() {
        return Optional.ofNullable(resourceAllocation);
    }

    public boolean isGuided() {
        return guided;
    }

    public Optional<Boolean> isDisableLeastPrivilegeInitDeployPolicy() {
        return Optional.ofNullable(disableLeastPrivilegeInitDeployPolicy);
    }

    public static class Builder {
        private String environmentVariable;
        private String version;
        private Email email;
        private Integer retainDistributionDays;
        private Integer retainDistributionVersions;
        private IamRoleArn initDeployArn;
        private Boolean giveAdminAccess;
        private Boolean createDeploymentPlanDefaultRole;
        private String vpcId;
        private List<String> subnetIds;
        private LogLevel logLevel;
        private String autoUpdate;
        private String token;
        private Boolean acceptLicenceAgreement;
        private Boolean useExistingVersion;
        private Boolean createInitDeployDefaultRole;
        private String resourceAllocation;

        private boolean guided;

        private Boolean disableLeastPrivilegeInitDeployPolicy;




        private Builder() {
        }


        public Builder setEnvironmentVariable(String environmentVariable) {
            this.environmentVariable = environmentVariable;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setEmail(Email email) {
            this.email = email;
            return this;
        }

        public Builder setRetainDistributionDays(Integer retainDistributionDays) {
            this.retainDistributionDays = retainDistributionDays;
            return this;
        }

        public Builder setRetainDistributionVersions(Integer retainDistributionVersions) {
            this.retainDistributionVersions = retainDistributionVersions;
            return this;
        }

        public Builder setInitDeployArn(IamRoleArn initDeployArn) {
            this.initDeployArn = initDeployArn;
            return this;
        }

        public Builder setGiveAdminAccess(Boolean giveAdminAccess) {
            this.giveAdminAccess = giveAdminAccess;
            return this;
        }

        public Builder setCreateDeploymentPlanDefaultRole(Boolean createDeploymentPlanDefaultRole) {
            this.createDeploymentPlanDefaultRole = createDeploymentPlanDefaultRole;
            return this;
        }

        public Builder setVpcId(String vpcId) {
            this.vpcId = vpcId;
            return this;
        }

        public Builder setSubnetIds(List<String> subnetIds) {
            this.subnetIds = subnetIds;
            return this;
        }

        public Builder setLogLevel(LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder setAutoUpdate(String autoUpdate) {
            this.autoUpdate = autoUpdate;
            return this;
        }

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public Builder setAcceptLicenseAgreement(Boolean acceptLicenceAgreement) {
            this.acceptLicenceAgreement = acceptLicenceAgreement;
            return this;
        }

        public Builder setUseExistingVersion(Boolean useExistingVersion) {
            this.useExistingVersion = useExistingVersion;
            return this;
        }

        public Builder setCreateInitDeployDefaultRole(Boolean createInitDeployDefaultRole) {
            this.createInitDeployDefaultRole = createInitDeployDefaultRole;
            return this;
        }

        public Builder setResourceAllocation(String resourceAllocation) {
            this.resourceAllocation = resourceAllocation;
            return this;
        }
        public Builder setGuided(boolean guided) {
            this.guided = guided;
            return this;
        }

        public Builder setDisableLeastPrivilegeInitDeployPolicy(Boolean disableLeastPrivilegeInitDeployPolicy){
            this.disableLeastPrivilegeInitDeployPolicy = disableLeastPrivilegeInitDeployPolicy;
            return this;
        }
        public SetupAttiniRequest build() {
            return new SetupAttiniRequest(this);
        }
    }
}
