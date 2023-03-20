package se.attini.cli.configure;


import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;

import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.PrintItem;
import se.attini.deployment.file.config.AttiniConfigFileException;
import se.attini.pack.EditConfigService;
import se.attini.pack.PropertyExistsException;

@CommandLine.Command(name = "set-tag", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Set a tag in a specified configuration file.")
public class SetTagsCommand implements Runnable {

    @Parameters(description = "Specify a configuration file to update.")
    private Path path;

    @Option(names = {"--override"}, description = "Override existing tag if it exists.")
    private boolean override;

    @Option(names = {"--key"}, required = true, description = "Specify a key for the tag.")
    private String key;

    @Option(names = {"--value"}, required = true, description = "Specify a value for the tag.")
    private String value;

    @SuppressWarnings("unused")
    @Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    private final EditConfigService editConfigService;
    private final ConsolePrinter consolePrinter;

    @Inject
    public SetTagsCommand(EditConfigService editConfigService, ConsolePrinter consolePrinter) {
        this.editConfigService = requireNonNull(editConfigService, "editConfigService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {

        try {

            editConfigService.setProperty(path.toFile(),
                                          List.of("tags"),
                                          key,
                                          value,
                                          override);

            consolePrinter.print(PrintItem.message("Set tag " + key +"=" + value + " in " + path));

        } catch (AttiniConfigFileException e) {
            consolePrinter.print(PrintItem.errorMessage(e.getMessage()));
            System.exit(ErrorResolver.resolve(e).getErrorCode().getExitCode());
        } catch (PropertyExistsException e) {
            consolePrinter.print(PrintItem.errorMessage("tag with key=" + key + " already exists, use --override to overwrite existing value"));
            System.exit(1);
        }

    }
}
