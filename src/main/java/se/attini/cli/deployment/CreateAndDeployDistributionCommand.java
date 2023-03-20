package se.attini.cli.deployment;

import static java.util.Objects.requireNonNull;
import static se.attini.cli.ConsolePrinter.ErrorPrintType.TEXT;

import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import se.attini.CheckVersionService;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorCode;
import se.attini.cli.PrintItem;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.GlobalConfig;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.deployment.CreateAndDeployDistributionRequest;
import se.attini.deployment.DeployDistributionResponse;
import se.attini.deployment.DeployDistributionService;
import se.attini.deployment.DeploymentAbortedException;
import se.attini.deployment.DistributionValidationException;
import se.attini.deployment.FollowDeploymentRequest;
import se.attini.deployment.FollowDeploymentService;
import se.attini.deployment.file.config.AttiniConfigFiles;
import se.attini.domain.DistributionId;
import se.attini.domain.EnvironmentName;
import se.attini.domain.FilePath;
import se.attini.pack.PackageDistributionService;

@Command(name = "run", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Package and deploy a new distributions.", usageHelpAutoWidth = true)
public class CreateAndDeployDistributionCommand implements Runnable {


    private final DeployDistributionService deployDistributionService;
    private final FollowDeploymentService followDeploymentService;
    private final PackageDistributionService packageDistributionService;
    private final AttiniConfigFiles attiniConfigFiles;
    private final CheckVersionService checkVersionService;
    private final GlobalConfig globalConfig;
    private final ConsolePrinter consolePrinter;
    @Parameters(description = "Specify a path to a directory you wish do deploy. Required.")
    private FilePath path;

    @Option(names = {"-e", "--environment"}, description = "Specify an environment. Required if there is more then one environment configured in the account")
    private EnvironmentName environment;

    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @Option(names = {"--dont-follow", "--blind", "-b"}, description = "Don't follow the deployment and print info to the console.")
    private boolean dontFollow;

    @Option(names = {"--environment-config-script", "-ec"}, description = "Optional path to a script file that will be executed before each package command execution phase." +
                                                                          " The file needs to be located inside the distribution and the path should be specified relative to the distribution root." +
                                                                          " Will only apply if not deploying an already packaged distribution.")
    private Path envConfigPath;

    @Option(names = {"--container-repository-login", "-crl"}, description = "Run login commands before getting container image.")
    private boolean containerRepoLogin;

    @Option(names = {"--container-build", "-cb"}, description = "Run the package phase in a container using docker.")
    private boolean containerBuild;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    @Option(names = {"--force", "-f"}, description = "Force the deployment. This will skipp the confirmation question for production environments.")
    private boolean forceDeployment;

    @CommandLine.Option(names = {"--distribution-id", "-i"}, description = "Specify a distribution id to set for the distribution during the package phase. Will be set before any prepackage commands are run. Will override any existing distributionId. Will be ignored if the path specifies an already packaged distribution.")
    private DistributionId distributionId;

    @CommandLine.Option(names = {"--skip-commands"}, description = "Don't run the pre or post package commands defined in the attini-config file during the package phase. Will be ignored if the path specifies an already packaged distribution.")
    private boolean skipCommands;

    @CommandLine.Option(names = {"--distribution-version", "-v"}, description = "Specify a semantic version for the distribution. Will be executed during the package phase and will be ignored if the path specifies an already packaged distribution.")
    private String version;

    @Inject
    public CreateAndDeployDistributionCommand(DeployDistributionService deployDistributionService,
                                              FollowDeploymentService followDeploymentService,
                                              PackageDistributionService packageDistributionService,
                                              AttiniConfigFiles attiniConfigFiles,
                                              CheckVersionService checkVersionService,
                                              GlobalConfig globalConfig,
                                              ConsolePrinter consolePrinter) {
        this.deployDistributionService = requireNonNull(deployDistributionService, "deployDistributionService");
        this.followDeploymentService = requireNonNull(followDeploymentService, "followDeploymentService");
        this.packageDistributionService = requireNonNull(packageDistributionService, "packageDistributionService");
        this.attiniConfigFiles = requireNonNull(attiniConfigFiles, "attiniConfigFiles");
        this.checkVersionService = requireNonNull(checkVersionService, "checkVersionService");
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {
        try {

            if (envConfigPath != null && envConfigPath.isAbsolute()) {
                consolePrinter.print(PrintItem.errorMessage( "Invalid environment config script path. Absolute paths are not supported. The path should be specified relative to the distribution root."));
                System.exit(1);
            }
            checkVersionService.checkVersion();

            if (path.getSourceType().equals(FilePath.SourceType.FILE_SYSTEM_DIRECTORY)) {
                Path destinationPath =
                        Path.of(path.getPath() + "/attini_dist/" + attiniConfigFiles.getDistributionName(Paths.get(
                                path.getPath())).getName() + ".zip");

                packageDistributionService.packageDistribution(Paths.get(path.getPath()),
                                                               destinationPath,
                                                               envConfigPath,
                                                               containerBuild,
                                                               containerRepoLogin,
                                                               distributionId,
                                                               skipCommands,
                                                               version);
                path = FilePath.create(destinationPath.toString());
            }

            CreateAndDeployDistributionRequest request = CreateAndDeployDistributionRequest.builder()
                                                                                           .setEnvironment(environment)
                                                                                           .setPath(path)
                                                                                           .setForceDeployment(
                                                                                                   forceDeployment)
                                                                                           .setJson(jsonOption.printAsJson())
                                                                                           .build();


            DeployDistributionResponse response = deployDistributionService.deployDistribution(request);
            if (!dontFollow) {
                followDeploymentService.followDeployment(FollowDeploymentRequest.builder()
                                                                                .setDistributionName(response.getDistributionName())
                                                                                .setObjectIdentifier(response.getObjectIdentifier())
                                                                                .setEnvironment(response.getEnvironment())
                                                                                .build());

            } else {
                consolePrinter.print(PrintItem.message(
                        "Deployment started, to see its status use the following command: \n\n\t" + createFollowCommand(
                                response) + "\n"));
            }
        } catch (DeploymentAbortedException e) {
            if (jsonOption.printAsJson()) {
                consolePrinter.printError(CliError.create(ErrorCode.ExecutionError, e.getMessage()));
            } else {
                consolePrinter.print(PrintItem.message(e.getMessage()));
            }
            System.exit(ErrorCode.ExecutionError.getExitCode());
        } catch (DistributionValidationException e) {
            CliError cliError = CliError.create(ErrorCode.ExecutionError, e.getMessage());
            if (jsonOption.printAsJson()) {
                consolePrinter.printError(cliError);
            } else {
                consolePrinter.printError(cliError, TEXT);
            }
            System.exit(cliError.getErrorCode().getExitCode());
        } catch (Exception e) {
            CliError cliError = CliError.create(ErrorCode.ExecutionError, e.getMessage(), e);
            if (jsonOption.printAsJson()) {
                consolePrinter.printError(cliError);
            } else {
                consolePrinter.printError(cliError, TEXT);
            }
            System.exit(cliError.getErrorCode().getExitCode());
        }


    }

    private String createFollowCommand(DeployDistributionResponse response) {
        StringBuilder stringBuilder = new StringBuilder().append("attini deploy describe -n ")
                                                         .append(response.getDistributionName().getName());

        globalConfig.getProfile().ifPresent(profile -> stringBuilder.append(" -p ").append(profile.getProfileName()));
        globalConfig.getRegion().ifPresent(region -> stringBuilder.append(" -r ").append(region.getName()));

        return stringBuilder.toString();
    }


}
