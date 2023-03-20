package se.attini.cli;

import static java.util.Objects.requireNonNull;
import static se.attini.cli.ConsolePrinter.ErrorPrintType.TEXT;

import java.util.List;

import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.domain.Email;
import se.attini.domain.IamRoleArn;
import se.attini.domain.LogLevel;
import se.attini.setup.SetupAttiniRequest;
import se.attini.setup.SetupAttiniService;

@Command(name = "setup", versionProvider = AttiniCliCommand.VersionProvider.class, subcommands = SetupVersionsCommand.class, description = "Setup the Attini framework.")
public class OnBoardingCommand implements Runnable {

    private SetupAttiniService setupAttiniService;
    private ConsolePrinter consolePrinter;
    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @Option(names = {"--dont-follow", "-blind", "-b"}, description = "Don't follow the deployment and print info to the console.")
    boolean dontFollow;

    @Option(names = {"--version", "-v"}, description = "Specify a version of the Attini framework, if left empty the latest version will be used.")
    String version;

    @Option(names = {"-k", "--environment-parameter-key"}, description = "If you have a Cloudformation parameter AttiniEnvironmentName in any of your templates, Attini will automatically configure it. If you want this parameter to be called something else in your templates, you can override it.")
    String environmentVariable;

    @Option(names = {"-m", "--contact-email"}, description = "Specify a contact email address that Attini can use to contact you or your organization.")
    Email email;

    @Option(names = {"--retain-distribution-days"}, description = "Configure how long Attini should save old distributions. Set to 0 to disable.")
    Integer retainDistributionDays;

    @Option(names = {"--retain-distribution-versions"}, description = "Configure how many distributions Attini should save, will take precedence over --retain-distribution-days. Set to 0 to disable.")
    Integer retainDistributionVersions;

    @Option(names = {"--init-deploy-role-arn"}, description = "Arn for the InitDeploy Cloudformation role, the AttiniDefaultRole role has Admin access and will therefore always work, however it does not follow the minimum access principle.")
    IamRoleArn initDeployArn;

    @Option(names = {"--give-admin-access"}, description = "Gives the Attini framework admin access. This will create two admin roles, one for the Attini runner and" +
                                                           "one for the Attini action lambda function, which runs Attini steps like AttiniCfn or AttiniSam. These roles will be used" +
                                                           "unless another role is specified.")
    Boolean giveAdminAccess;

    @Option(names = {"--create-deployment-plan-default-role"}, description = "Should the Attini setup create a default role for the deployment plan? The default role have quite broad permissions so" +
                                                                             "in a high security environment you can remove this role and provide your own role arn for the underlying step function." +
                                                                             "NOTE, you will have to provide the role to every individual deployment plan. That Role will need permission to read from AttiniDeployData DynamoDb table.")
    Boolean createDeploymentPlanDefaultRole;

    @Option(names = {"--vpc-id"}, description = "If you require the Attini Lambda functions to be executed in any specific VPC, please fill it here. This also requires SubnetsIds to be configured.")
    String vpcId;

    @Option(names = {"--subnets-ids"}, description = "If you require the Attini Lambda functions to be executed in any specific subnets, please fill it here. This also required VpcId to be configured.")
    List<String> subnetIds;

    @Option(names = {"--log-level"}, description = "The log level that all Attini services should use.")
    LogLevel logLevel;

    @Option(names = {"--auto-update"}, description = "Should Attini framework auto update? If yes, enter a cron or rate expressions for when it should be done. If you don't want in to auto update, leave this field empty. More info about cron or rate expressions https://docs.aws.amazon.com/AmazonCloudWatch/latest/events/ScheduledEvents.html")
    String autoUpdate;

    @Option(names = {"--license-token"}, description = "Token to manage billing data. Mandatory for usage exceeding free tier. Tokens get be created from the Attini customer admin webpage.")
    String token;

    @Option(names = {"--accept-license-agreement"}, description = "Accept Attinis licence agreement, read more at https://docs.attini.io/pricing-and-license/product-offering.html")
    Boolean acceptLicenceAgreement;

    @Option(names = {"--keep-version"}, description = "Run the command without updating the version of Attini in your account.")
    Boolean keepSameVersion;

    @Option(names = {"--create-init-deploy-default-role"}, description = "Should the Attini setup create a default role for the deployment plan? The default role have quite broad permissions so" +
                                                                         "in a high security environment you can remove this role and provide your own role arn for the underlying step function." +
                                                                         "NOTE, you will have to provide the role to every individual deployment plan. That Role will need permission to read from AttiniDeployData DynamoDb table.")
    Boolean createInitDeployDefaultRole;

    @Option(names = {"--resource-allocation"}, description = "Configure ReservedConcurrentExecutions for the Attini Lambda functions. Find more details here https://docs.attini.io/getting-started/installations/deploy-and-update-the-attini-framework.html", completionCandidates = ResourceAllocationCompletionCandidates.class)
    String resourceAllocation;

    @SuppressWarnings("unused")
    @Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    boolean help;
    @CommandLine.Option(names = {"--guided"}, description = "Perform in guided mode.")
    boolean guided;

    @CommandLine.Option(names = {"--disable-least-privilege-init-deploy-policy"}, description = "Use json as output format.")
    boolean disableLeastPrivilegeInitDeployPolicy;

    @SuppressWarnings("unused")
    public OnBoardingCommand() {
    }

    @Inject
    public OnBoardingCommand(SetupAttiniService setupAttiniService,
                             ConsolePrinter consolePrinter) {
        this.setupAttiniService = requireNonNull(setupAttiniService, "setupAttiniService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {

        if (email != null && !email.isValid()) {
            CliError error = CliError.create(ErrorCode.IllegalArgument,
                                             "Invalid value for option '--contact-email, value=" + email.getEmail());
            consolePrinter.printError(error, TEXT);
            System.exit(error.getErrorCode().getExitCode());
        }

        SetupAttiniRequest.Builder builder = SetupAttiniRequest.builder()
                                                               .setVersion(version)
                                                               .setEnvironmentVariable(environmentVariable)
                                                               .setRetainDistributionDays(retainDistributionDays)
                                                               .setRetainDistributionVersions(retainDistributionVersions)
                                                               .setCreateDeploymentPlanDefaultRole(
                                                                       createDeploymentPlanDefaultRole)
                                                               .setGiveAdminAccess(giveAdminAccess)
                                                               .setInitDeployArn(initDeployArn)
                                                               .setLogLevel(logLevel)
                                                               .setSubnetIds(subnetIds)
                                                               .setVpcId(vpcId)
                                                               .setEmail(email)
                                                               .setAutoUpdate(autoUpdate)
                                                               .setToken(token)
                                                               .setAcceptLicenseAgreement(acceptLicenceAgreement)
                                                               .setUseExistingVersion(keepSameVersion)
                                                               .setCreateInitDeployDefaultRole(
                                                                       createInitDeployDefaultRole)
                                                               .setResourceAllocation(resourceAllocation)
                                                               .setDisableLeastPrivilegeInitDeployPolicy(
                                                                       disableLeastPrivilegeInitDeployPolicy)
                                                               .setGuided(guided);


        try {
            setupAttiniService.setup(builder.build(), !dontFollow);
        } catch (Exception e) {
            CliError error = ErrorResolver.resolve(e);
            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        }


    }
}
