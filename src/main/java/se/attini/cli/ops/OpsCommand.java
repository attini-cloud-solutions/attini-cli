package se.attini.cli.ops;

import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;

@CommandLine.Command(name = "ops",
                     versionProvider = AttiniCliCommand.VersionProvider.class,
                     description = "Provides subcommands for various operations related tasks.",
                     subcommands = {RougeStacksCommand.class, ExportLogsCommand.class, RemoveStackResourceCommand.class})
public class OpsCommand {

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;
}
