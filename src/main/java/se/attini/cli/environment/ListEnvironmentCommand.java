package se.attini.cli.environment;


import static java.util.Objects.requireNonNull;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorCode;
import se.attini.cli.ErrorResolver;
import se.attini.cli.PrintItem;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.domain.Environment;
import se.attini.environment.EnvironmentService;
import se.attini.util.ObjectMapperFactory;

@CommandLine.Command(name = "list", versionProvider = AttiniCliCommand.VersionProvider.class, description = "List all environments in the current account.", usageHelpAutoWidth = true)

public class ListEnvironmentCommand implements Runnable {


    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help","-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    private final EnvironmentService environmentService;
    private final ObjectMapperFactory objectMapperFactory;
    private final ConsolePrinter consolePrinter;

    @Inject
    public ListEnvironmentCommand(EnvironmentService environmentService,
                                  ObjectMapperFactory objectMapperFactory,
                                  ConsolePrinter consolePrinter) {
        this.environmentService = requireNonNull(environmentService, "environmentService");
        this.objectMapperFactory = requireNonNull(objectMapperFactory, "objectMapperFactory");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }


    @Override
    public void run() {
        try {
            List<Environment> environments = environmentService.getEnvironments();
            ObjectWriter objectWriter = objectMapperFactory.getObjectMapper()
                                                           .writerWithDefaultPrettyPrinter();
            if (!environments.isEmpty()) {
                consolePrinter.print(PrintItem.message(objectWriter.writeValueAsString(environments)));
            } else {
                CliError error = CliError.create(ErrorCode.NotConfigured, "No environments configured in account");
                consolePrinter.printError(error);
                System.exit(error.getErrorCode().getExitCode());
            }
        } catch (Exception e) {
            CliError error = ErrorResolver.resolve(e);
            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        }

    }
}
