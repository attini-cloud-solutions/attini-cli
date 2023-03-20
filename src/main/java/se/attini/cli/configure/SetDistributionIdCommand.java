package se.attini.cli.configure;


import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.UUID;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.PrintItem;
import se.attini.cli.PrintUtil;
import se.attini.deployment.file.config.AttiniConfigFileException;
import se.attini.pack.EditAttiniConfigService;
import se.attini.pack.PropertyExistsException;

@CommandLine.Command(name = "set-dist-id", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Set a distribution id in the attini-config file. It is recommended to execute this command as a prepackage command when packaging a distribution. See https://docs.attini.io/api-reference/attini-configuration.html for more information.")
public class SetDistributionIdCommand implements Runnable {


    private final EditAttiniConfigService editAttiniConfigService;
    private final ConsolePrinter consolePrinter;

    @CommandLine.Option(names = {"--path"}, description = "Specify a path to your distribution root. The default is the current working directory.")
    private Path path;

    static class IdExclusive {
        @CommandLine.Option(names = {"--id"}, description = "Specify the distribution id you want to set.", required = true)
        String id;
        @CommandLine.Option(names = {"--random"}, description = "Set a random distribution id.", required = true)
        boolean random;
    }

    @CommandLine.Option(names = {"--override"}, description = "Override existing id.")
    private boolean override;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    @CommandLine.ArgGroup(multiplicity = "1")
    private IdExclusive idExclusive;
    @Inject
    public SetDistributionIdCommand(EditAttiniConfigService editAttiniConfigService,
                                    ConsolePrinter consolePrinter) {

        this.editAttiniConfigService = requireNonNull(editAttiniConfigService, "editAttiniConfigService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {
        Path workingPath = path != null ? path : Path.of(".");
        try {
            String id = idExclusive.random ? UUID.randomUUID().toString() : idExclusive.id;
            editAttiniConfigService.setProperty(workingPath, emptyList(), "distributionId", id, override);
            consolePrinter.print(PrintItem.message("DistributionId set to: " + PrintUtil.toBlue(id)));
        } catch (AttiniConfigFileException e) {
            CliError cliError = ErrorResolver.resolve(e);
            consolePrinter.print(PrintItem.errorMessage(cliError.getMessage()));
            System.exit(cliError.getErrorCode().getExitCode());
        } catch (PropertyExistsException e) {
            consolePrinter.print(PrintItem.errorMessage(
                    "distributionId already exists, use --override to overwrite existing value"));
            System.exit(1);
        }


    }
}
