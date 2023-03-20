package se.attini.cli.distribution;


import static java.util.Objects.requireNonNull;
import static se.attini.cli.ConsolePrinter.ErrorPrintType.TEXT;

import java.nio.file.Path;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.CheckVersionService;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.PrintItem;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.deployment.file.config.AttiniConfigFiles;
import se.attini.domain.DistributionId;
import se.attini.pack.PackageDistributionService;

@CommandLine.Command(name = "package", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Package a distribution.")
public class PackageDistributionCommand implements Runnable {

    private final PackageDistributionService packageDistributionService;
    private final AttiniConfigFiles attiniConfigFiles;
    private final CheckVersionService checkVersionService;
    private final ConsolePrinter consolePrinter;


    @CommandLine.Parameters(description = "Specify a path to your distribution root. Required.")
    private Path path;

    @CommandLine.Option(names = {"--name"}, description = "Set a custom name for the package")
    private String name;

    @CommandLine.Option(names = {"--environment-config-script", "-ec"}, description = "Optional path to a script file that will be executed before each package command execution phase."+
                                                                                      " The file needs to be located inside the distribution and the path should be specified relative to the distribution root.")
    private Path envConfigPath;

    @CommandLine.Option(names = {"--container-build", "-cb"}, description = "Package in a container using docker.")
    private boolean containerBuild;

    @CommandLine.Option(names = {"--container-repository-login", "-crl"}, description = "Run login commands before getting container image.")
    private boolean containerRepoLogin;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    @CommandLine.Option(names = {"--distribution-id", "-i"}, description = "Specify a distribution id to set for the distribution. Will be set before any prepackage commands are run. Will override any existing distributionId. ")
    private DistributionId distributionId;

    @CommandLine.Option(names = {"--skip-commands"}, description = "Dont run the pre or post package commands defined in the attini-config file.")
    private boolean skipCommands;


    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @CommandLine.Option(names =  {"--distribution-version", "-v"}, description = "Specify a semantic version for the distribution.")
    private String version;

    @Inject
    public PackageDistributionCommand(PackageDistributionService packageDistributionService,
                                      AttiniConfigFiles attiniConfigFiles,
                                      CheckVersionService checkVersionService,
                                      ConsolePrinter consolePrinter) {
        this.packageDistributionService = requireNonNull(packageDistributionService, "packageDistributionService");
        this.attiniConfigFiles = requireNonNull(attiniConfigFiles, "attiniConfigFiles");
        this.checkVersionService = requireNonNull(checkVersionService, "checkVersionService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {

        checkVersionService.checkVersion();

        try {

            if (envConfigPath != null && envConfigPath.isAbsolute()){
                consolePrinter.print(PrintItem.errorMessage("Invalid environment config script path. Absolute paths are not supported. The path should be specified relative to the distribution root."));
                System.exit(1);
            }

            String fileName = name != null ? name : attiniConfigFiles.getDistributionName(path).getName();

            String fileExtension = fileName.endsWith(".zip") ? "" : ".zip";
            Path destinationPath = Path.of(path + "/attini_dist/" + fileName + fileExtension);
            packageDistributionService.packageDistribution(path,
                                                           destinationPath,
                                                           envConfigPath,
                                                           containerBuild,
                                                           containerRepoLogin,
                                                           distributionId,
                                                           skipCommands,
                                                           version);
        } catch (Exception e) {
            CliError resolve = ErrorResolver.resolve(e);
            if (jsonOption.printAsJson()) {
                consolePrinter.printError(resolve);
            } else {
                consolePrinter.printError(resolve, TEXT);
            }
            System.exit(resolve.getErrorCode().getExitCode());
        }


    }
}
