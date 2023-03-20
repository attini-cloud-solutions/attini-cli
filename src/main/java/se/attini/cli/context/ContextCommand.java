package se.attini.cli.context;


import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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
import se.attini.context.Context;
import se.attini.context.ContextService;
import se.attini.context.GetContextRequest;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;

@CommandLine.Command(name = "context", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Print info about current account, environments etc.", usageHelpAutoWidth = true)
public class ContextCommand implements Runnable {

    private final ContextService contextService;
    private final ConsolePrinter consolePrinter;
    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @CommandLine.Option(names = {"--distribution-name", "-n"}, description = "Specify a name of a distribution.")
    private DistributionName distributionName;

    @CommandLine.Option(names = {"-e", "--environment"}, description = "Only include specified environment in result.")
    private EnvironmentName environment;
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    @Inject
    public ContextCommand(ContextService contextService,
                          ConsolePrinter consolePrinter) {
        this.contextService = requireNonNull(contextService, "contextService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }


    @Override
    public void run() {
        try {
            Context context = contextService.getContext(new GetContextRequest(environment,
                                                                              distributionName));

            String json = getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(context);
            consolePrinter.print(PrintItem.message(json));
        } catch (Exception e) {
            CliError error = ErrorResolver.resolve(e);
            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        }

    }

    private ObjectMapper getObjectMapper() {
        if (jsonOption.printAsJson()) {
            return new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        return new ObjectMapper(new YAMLFactory()).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
