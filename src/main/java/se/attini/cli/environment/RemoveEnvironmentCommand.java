package se.attini.cli.environment;


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
import se.attini.domain.EnvironmentName;
import se.attini.environment.EnvironmentService;
import se.attini.environment.RemoveEnvironmentRequest;

@CommandLine.Command(name = "remove", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Remove an environment from the account. Note that this will only remove the environment configuration, " +
                                                                                                              "not any resources that exists in the environment.", usageHelpAutoWidth = true)
public class RemoveEnvironmentCommand implements Runnable{

    @CommandLine.Parameters(description = "Specify a name for the new environment.")
    private EnvironmentName environment;


    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help","-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;


    private EnvironmentService environmentService;
    private ConsolePrinter consolePrinter;

    @SuppressWarnings("unused")
    public RemoveEnvironmentCommand() {
    }

    @Inject
    public RemoveEnvironmentCommand(EnvironmentService environmentService,
                                    ConsolePrinter consolePrinter) {
        this.environmentService = requireNonNull(environmentService, "environmentService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }



    @Override
    public void run() {
        try {
            environmentService.removeEnvironment(new RemoveEnvironmentRequest(environment));
        } catch (Exception e) {
            CliError error = ErrorResolver.resolve(e);
            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        }

    }
}
