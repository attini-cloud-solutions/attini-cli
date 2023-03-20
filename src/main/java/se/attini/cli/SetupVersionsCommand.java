package se.attini.cli;

import static java.util.Objects.requireNonNull;

import java.util.List;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.setup.SetupVersionsService;
import se.attini.util.ObjectMapperFactory;


@CommandLine.Command(name = "list", aliases = {"ls"}, description = "List available versions.")
public class SetupVersionsCommand implements Runnable {


    @CommandLine.Mixin
    private DebugOption debugOption;
    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    JsonOption jsonOption;
    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    boolean help;

    private SetupVersionsService setupVersionsService;
    private ObjectMapperFactory objectMapperFactory;
    private ConsolePrinter consolePrinter;

    @SuppressWarnings("unused")
    public SetupVersionsCommand() {
    }

    @Inject
    public SetupVersionsCommand(SetupVersionsService setupVersionsService, ObjectMapperFactory objectMapperFactory,
                                ConsolePrinter consolePrinter) {
        this.setupVersionsService = requireNonNull(setupVersionsService, "setupVersionsService");
        this.objectMapperFactory = requireNonNull(objectMapperFactory, "objectMapperFactory");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {
        try {
            List<String> setupVersions = setupVersionsService.getSetupVersions();
            System.out.println(objectMapperFactory.getObjectMapper()
                                                  .writerWithDefaultPrettyPrinter()
                                                  .writeValueAsString(setupVersions));
        } catch (Exception e) {
            CliError cliError = ErrorResolver.resolve(e);
            consolePrinter.printError(cliError);
            System.exit(cliError.getErrorCode().getExitCode());
        }

    }


}
