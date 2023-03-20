package se.attini.cli.deployment;


import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.FollowDeploymentCommand;
import se.attini.cli.GetDistributionOutputCommand;

@CommandLine.Command(name = "deploy", versionProvider = AttiniCliCommand.VersionProvider.class, aliases = {"d"}, subcommands = {CreateAndDeployDistributionCommand.class,
                                                                                                                                DeploymentHistoryCommand.class,
                                                                                                                                RollBackCommand.class,
                                                                                                                                FollowDeploymentCommand.class,
                                                                                                                                GetDistributionOutputCommand.class,
                                                                                                                                ContinueDeploymentCommand.class}, description = "Provides various subcommands for handling deployments.")
public class DeploymentCommand {

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help","-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;
}
