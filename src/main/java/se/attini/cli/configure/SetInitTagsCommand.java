package se.attini.cli.configure;


import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.PrintItem;
import se.attini.deployment.file.config.AttiniConfigFileException;
import se.attini.domain.EnvironmentName;
import se.attini.pack.EditAttiniConfigService;
import se.attini.pack.PropertyExistsException;

@CommandLine.Command(name = "set-init-deploy-tag", versionProvider = AttiniCliCommand.VersionProvider.class,
                     description = "Set a tag in the initDeployConfig part of the attini-config file.")
public class SetInitTagsCommand implements Runnable {

    @CommandLine.Option(names = {"--path"}, description = "Specify a path to your distribution root. The default is the current working directory.")
    private Path path;

    @Option(names = {"--override"}, description = "Override existing tag if it exists.")
    private boolean override;

    @Option(names = {"-e", "--environment"}, description = "Specify an environment. If not specified default will be used.")
    private EnvironmentName environment;

    @Option(names = {"--key"}, required = true, description = "Specify a key for the tag.")
    private String key;

    @Option(names = {"--value"}, required = true, description = "Specify a value for the tag.")
    private String value;

    @SuppressWarnings("unused")
    @Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    private final EditAttiniConfigService editAttiniConfigService;
    private final ConsolePrinter consolePrinter;

    @Inject
    public SetInitTagsCommand(EditAttiniConfigService editAttiniConfigService, ConsolePrinter consolePrinter) {
        this.editAttiniConfigService = requireNonNull(editAttiniConfigService, "editAttiniConfigService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {


        try {
            Path workingPath = path != null ? path : Path.of(".");


            String environment = Optional.ofNullable(this.environment).map(
                    EnvironmentName::getName).orElse("default");
            editAttiniConfigService.setProperty(workingPath,
                                                List.of("initDeployConfig",
                                                        "tags",
                                                        environment),
                                                key,
                                                value,
                                                override);

            System.out.println("Set tag " + key + "=" + value + " for environment = " + environment + " in " + workingPath.getFileName());

        } catch (AttiniConfigFileException e) {
            consolePrinter.print(PrintItem.errorMessage(e.getMessage()));
            System.exit(ErrorResolver.resolve(e).getErrorCode().getExitCode());
        } catch (PropertyExistsException e) {
            consolePrinter.print(PrintItem.errorMessage("tag with key=" + key + " already exists in environment=" + environment + ", use --override to overwrite existing value"));
            System.exit(1);
        }

    }
}
