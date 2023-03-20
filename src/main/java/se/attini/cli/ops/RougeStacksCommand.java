package se.attini.cli.ops;


import static java.util.Objects.requireNonNull;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.PrintItem;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;
import se.attini.stackstatus.FindUnmanagedStackRequest;
import se.attini.stackstatus.FindUnmanagedStackService;
import se.attini.stackstatus.UnmanagedDistributionStacks;

@CommandLine.Command(name = "rogue-stacks", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Find stacks in an environment that has been created by Attini but is no longer managed by a distribution.")
public class RougeStacksCommand implements Runnable {

    private final FindUnmanagedStackService findUnmanagedStackService;
    private final ConsolePrinter consolePrinter;

    @CommandLine.Option(names = {"-e", "--environment"}, description = "Specify an environment. Required if there is more then one environment in your account.")
    private EnvironmentName environment;

    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @CommandLine.Option(names = {"--distribution-name", "-n"}, description = "Run the command for a specific distribution. Optional.")
    private DistributionName distributionName;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    @Inject
    public RougeStacksCommand(FindUnmanagedStackService findUnmanagedStackService, ConsolePrinter consolePrinter) {
        this.findUnmanagedStackService = requireNonNull(findUnmanagedStackService, "findUnmanagedStackService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }


    @Override
    public void run() {

        try {
            List<UnmanagedDistributionStacks> unmanagedStacks = findUnmanagedStackService.findUnmanagedStacks(
                    FindUnmanagedStackRequest.builder()
                                             .setDistributionName(distributionName)
                                             .setEnvironment(environment)
                                             .build());

            String json = getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(unmanagedStacks);


            consolePrinter.print(PrintItem.message(json));
        } catch (Exception e) {
            CliError cliError = ErrorResolver.resolve(e);
            consolePrinter.printError(cliError);
            System.exit(cliError.getErrorCode().getExitCode());
        }


    }

    private ObjectMapper getObjectMapper(){
        if(jsonOption.printAsJson()){
            return new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        return new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.SPLIT_LINES)).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
