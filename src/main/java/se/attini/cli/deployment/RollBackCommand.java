package se.attini.cli.deployment;


import static java.util.Objects.requireNonNull;

import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.PrintItem;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.deployment.DeployDistributionRequest;
import se.attini.deployment.DeployDistributionResponse;
import se.attini.deployment.DeploymentAbortedException;
import se.attini.deployment.FollowDeploymentRequest;
import se.attini.deployment.FollowDeploymentService;
import se.attini.deployment.RedeployDistributionService;
import se.attini.domain.Distribution;
import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;

@CommandLine.Command(name = "rollback", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Perform a rollback to a previously deployed distribution.")
public class RollBackCommand implements Runnable {

    @Option(names = {"-e", "--environment"}, description = "Specify an environment. Required if there is more then one environment in your account.")
    private EnvironmentName environment;

    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @Option(names = {"--distribution-name", "-n"}, description = "Specify a name of a distribution. Required.", required = true)
    private DistributionName distributionName;

    @Option(names = {"--distribution-id", "-i"}, description = "Specify an id of a distribution. Required.", required = true)
    private DistributionId distributionId;

    @SuppressWarnings("unused")
    @Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    @Option(names = {"--force", "-f"}, description = "Force the deployment. This will skipp the confirmation question for production environments.")
    private boolean forceDeployment;

    private RedeployDistributionService redeployDistributionService;
    private FollowDeploymentService followDeploymentService;
    private ConsolePrinter consolePrinter;

    @SuppressWarnings("unused")
    public RollBackCommand() {
    }

    @Inject
    public RollBackCommand(RedeployDistributionService redeployDistributionService,
                           FollowDeploymentService followDeploymentService,
                           ConsolePrinter consolePrinter) {
        this.redeployDistributionService = requireNonNull(redeployDistributionService, "redeployDistributionService");
        this.followDeploymentService = requireNonNull(followDeploymentService, "followDeploymentService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {

        try {
            Distribution distribution = Distribution.builder()
                                                    .setDistributionId(distributionId)
                                                    .setDistributionName(distributionName)
                                                    .build();
            DeployDistributionResponse deployDistributionResponse = redeployDistributionService
                    .redeployDistribution(DeployDistributionRequest.builder()
                                                                   .setDistribution(distribution)
                                                                   .setEnvironment(environment)
                                                                   .setForceDeployment(forceDeployment)
                                                                   .build());



            followDeploymentService
                    .followDeployment(FollowDeploymentRequest.builder()
                                                             .setObjectIdentifier(deployDistributionResponse.getObjectIdentifier())
                                                             .setDistributionName(deployDistributionResponse.getDistributionName())
                                                             .setEnvironment(deployDistributionResponse.getEnvironment())
                                                             .build());


            consolePrinter.print(PrintItem.successMessage("Distribution successfully redeployed"));
        } catch (DeploymentAbortedException e) {
            consolePrinter.print(PrintItem.message(e.getMessage()));
        } catch (Exception e) {
            CliError resolve = ErrorResolver.resolve(e);
            consolePrinter.printError(resolve);
            System.exit(resolve.getErrorCode().getExitCode());
        }

    }
}
