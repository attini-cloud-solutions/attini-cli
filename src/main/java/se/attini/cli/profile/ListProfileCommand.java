package se.attini.cli.profile;


import static java.util.Objects.requireNonNull;

import java.util.stream.Collectors;

import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.domain.Profile;
import se.attini.profile.ProfileFacade;
import se.attini.util.ObjectMapperFactory;

@Command(name = "list-profiles", versionProvider = AttiniCliCommand.VersionProvider.class, description = "List all configured AWS profiles.")
public class ListProfileCommand implements Runnable {

    private final ProfileFacade profileLoader;
    private final ObjectMapperFactory objectMapperFactory;
    private final ConsolePrinter consolePrinter;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    @Inject
    public ListProfileCommand(ProfileFacade profileLoader, ObjectMapperFactory objectMapperFactory, ConsolePrinter consolePrinter) {
        this.profileLoader = requireNonNull(profileLoader, "profileLoader");
        this.objectMapperFactory = requireNonNull(objectMapperFactory, "objectMapperFactory");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {
        try {
            System.out.println(objectMapperFactory.getObjectMapper()
                                                  .writerWithDefaultPrettyPrinter()
                                                  .writeValueAsString(profileLoader.loadProfiles()
                                                                                   .stream()
                                                                                   .map(Profile::getProfileName)
                                                                                   .collect(Collectors.toList())));
        } catch (Exception e) {
            CliError error = ErrorResolver.resolve(e);
            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        }

    }

}


