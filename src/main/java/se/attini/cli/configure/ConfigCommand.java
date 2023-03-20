package se.attini.cli.configure;


import picocli.CommandLine;
import picocli.CommandLine.Option;
import se.attini.cli.AttiniCliCommand;

@CommandLine.Command(name = "configure", versionProvider = AttiniCliCommand.VersionProvider.class, subcommands = {SetDistributionIdCommand.class,
                                                                                                                  SetInitParameterCommand.class,
                                                                                                                  SetInitTagsCommand.class,
                                                                                                                  SetTagsCommand.class,
                                                                                                                  SetParameterCommand.class}, description = "Provides various subcommands for configuring a distribution. ")
public class ConfigCommand {

    @SuppressWarnings("unused")
    @Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;
}
