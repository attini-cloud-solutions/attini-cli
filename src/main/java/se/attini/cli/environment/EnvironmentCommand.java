package se.attini.cli.environment;

import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.context.ContextCommand;

@CommandLine.Command(name = "environment", versionProvider = AttiniCliCommand.VersionProvider.class, subcommands = {CreateEnvironmentCommand.class,
                                                                                                                    RemoveEnvironmentCommand.class,
                                                                                                                    ListEnvironmentCommand.class, ContextCommand.class}, description = "Provides various subcommands for handling environments.")
public class EnvironmentCommand {

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help","-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

}
