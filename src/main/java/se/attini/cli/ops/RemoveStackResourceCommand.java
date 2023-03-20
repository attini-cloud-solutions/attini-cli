package se.attini.cli.ops;


import static java.util.Objects.requireNonNull;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.RegionCompletionCandidates;
import se.attini.cli.RemoveStackService;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.domain.Region;
import se.attini.domain.StackName;
import se.attini.removestack.RemoveStackResourceRequest;

@CommandLine.Command(name = "delete-stack-resources", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Delete all resource Attini has created to track a CloudFormation stack. The stack itself will not be deleted unless specified with the --delete-stack option.")
public class RemoveStackResourceCommand implements Runnable {


    private RemoveStackService removeStackService;
    private ConsolePrinter consolePrinter;

    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @CommandLine.Option(names = {"--stack-region"}, description = "Specify an aws region that the stack was deployed to. If absent the profile specified via the --profile option be used. If both --stack-profile and --profile is absent then the default region will be used.", completionCandidates = RegionCompletionCandidates.class)
    private Region stackRegion;

    @CommandLine.Option(names = {"--stack-name"}, description = "Specify the name of the stack which resources should be deleted. Required.", required = true, completionCandidates = RegionCompletionCandidates.class)
    private StackName stackName;

    @CommandLine.Option(names = {"--account-id"}, description = "Specify what account the stack was deployed to, only required if the stack was deployed to a different account then the current.")
    private String account;

    @CommandLine.Option(names = {"--delete-stack"}, description = "Also delete the CloudFormation stack. Not supported cross account.")
    private boolean deleteStack;


    @SuppressWarnings("unused")
    public RemoveStackResourceCommand() {
    }


    @Inject
    public RemoveStackResourceCommand(RemoveStackService removeStackService, ConsolePrinter consolePrinter) {
        this.removeStackService = requireNonNull(removeStackService, "removeStackService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {
        try {
            removeStackService.removeStackResources(RemoveStackResourceRequest.builder()
                                                                              .setStackName(stackName)
                                                                              .setStackRegion(stackRegion)
                                                                              .setAccountId(account)
                                                                              .setDeleteStack(deleteStack)
                                                                              .build());


        } catch (Exception e) {
            CliError cliError = ErrorResolver.resolve(e);
            consolePrinter.printError(cliError);
            System.exit(cliError.getErrorCode().getExitCode());
        }
    }
}
