package se.attini.cli.init;


import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;

@CommandLine.Command(name = "init-project",
                     versionProvider = AttiniCliCommand.VersionProvider.class,
                     subcommands = {InitHelloWorldCommand.class,
                                    InitSkeletonCommand.class,
                                    InitSimpleWebsiteCommand.class,
                                    InitSamProjectCommand.class},
                     description = "Create different template projects.")
public class InitProjectCommand {

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;
}
