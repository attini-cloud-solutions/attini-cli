package se.attini.cli.deployment;


import static java.util.Objects.requireNonNull;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.deployment.ContinueDeploymentRequest;
import se.attini.deployment.ContinueDeploymentService;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;

@CommandLine.Command(name = "continue", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Continue a manual approval step", usageHelpAutoWidth = true)
public class ContinueDeploymentCommand implements Runnable {

    @CommandLine.Option(names = {"-e", "--environment"}, description = "Specify an environment. Required if there is more then one environment configured in the account")
    private EnvironmentName environment;

    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @CommandLine.Option(names = {"--step-name"}, required = true, description = "Specify the name of the manual approval step. Required.")
    private String stepName;

    @CommandLine.Option(names = {"--message", "-m"}, description = "Specify a message that will be included in the steps output")
    private String message;
    @CommandLine.Option(names = {"--distribution-name", "-n"}, required = true, description = "Specify a name of the distribution. Required.", hideParamSyntax = true)
    private DistributionName distributionName;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    boolean help;

    @CommandLine.Option(names = {"--abort"}, description = "Abort the deployment plan.")
    boolean abort;

    private ContinueDeploymentService continueDeploymentService;
    private ConsolePrinter consolePrinter;

    @SuppressWarnings("unused")
    public ContinueDeploymentCommand() {
    }


    @Inject
    public ContinueDeploymentCommand(ContinueDeploymentService continueDeploymentService,
                                     ConsolePrinter consolePrinter) {
        this.continueDeploymentService = requireNonNull(continueDeploymentService, "continueDeploymentService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }


    @Override
    public void run() {
        try {
            continueDeploymentService.continueDeployment(ContinueDeploymentRequest.builder()
                                                                                  .environmentName(environment)
                                                                                  .distributionName(distributionName)
                                                                                  .message(message)
                                                                                  .stepName(stepName)
                                                                                  .abort(abort)
                                                                                  .build());

        } catch (Exception e) {
            CliError error = ErrorResolver.resolve(e);
            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        }

    }
}
