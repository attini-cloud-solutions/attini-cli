package se.attini.cli.distribution;


import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;

@CommandLine.Command(name = "distribution", versionProvider = AttiniCliCommand.VersionProvider.class, aliases = {"dist"}, subcommands = {PackageDistributionCommand.class,
                                                                                                                                         InspectDistributionCommand.class,
                                                                                                                                         DownloadDistributionCommand.class}, description = "Provides various subcommands for handling distributions.")
public class DistributionCommand {

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;
}
