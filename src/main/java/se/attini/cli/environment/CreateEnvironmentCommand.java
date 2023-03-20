package se.attini.cli.environment;


import static java.util.Objects.requireNonNull;
import static se.attini.domain.EnvironmentType.PRODUCTION;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorCode;
import se.attini.cli.ErrorResolver;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.domain.EnvironmentName;
import se.attini.domain.EnvironmentType;
import se.attini.environment.CreateEnvironmentRequest;
import se.attini.environment.EnvironmentService;

@CommandLine.Command(name = "create", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Create a new environment for the current account.", usageHelpAutoWidth = true)

public class CreateEnvironmentCommand implements Runnable {

    @CommandLine.Parameters(description = "Specify a name for the new environment")
    private EnvironmentName environment;

    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    @CommandLine.Option(names = {"--env-type"}, description = "Set type of environment, will default to production.", completionCandidates = EnvironmentTypeCompletionCandidates.class)
    private String environmentType;



    private final EnvironmentService environmentService;
    private final ConsolePrinter consolePrinter;

    @Inject
    public CreateEnvironmentCommand(EnvironmentService environmentService,
                                    ConsolePrinter consolePrinter) {
        this.environmentService = requireNonNull(environmentService, "environmentService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }


    @Override
    public void run() {

        try {

            EnvironmentType finalEnvironmentType = environmentType == null ? PRODUCTION : EnvironmentType.fromString(environmentType);

            environmentService.createEnvironment(new CreateEnvironmentRequest(environment,
                                                                              finalEnvironmentType));
        } catch (IllegalArgumentException e) {
            CliError error = CliError.create(ErrorCode.IllegalArgument,
                                             "Illegal value for --env-type. Allowed values are: " +
                                             Arrays.stream(EnvironmentType.values())
                                                   .map(EnvironmentType::getValue)
                                                   .collect(Collectors.joining(", ")));

            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        } catch (Exception e) {
            CliError error = ErrorResolver.resolve(e);
            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        }
    }

    static public class EnvironmentTypeCompletionCandidates implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            try {
                return Arrays.stream(EnvironmentType.values()).map(EnvironmentType::getValue).iterator();
            } catch (Exception e) {
                return Collections.emptyIterator();
            }
        }
    }
}
