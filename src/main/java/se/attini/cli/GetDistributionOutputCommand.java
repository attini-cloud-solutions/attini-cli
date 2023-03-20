package se.attini.cli;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.GetDistributionOutputRequest;
import se.attini.GetDistributionOutputService;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;
import se.attini.util.ObjectMapperFactory;

@CommandLine.Command(name = "output", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Get the output from the latest deployment of the specified distribution.")
public class GetDistributionOutputCommand implements Runnable {



    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;
    @CommandLine.Option(names = {"-e", "--environment"}, description = "Specify an environment. Required if there is more then one environment in your account.")
    private EnvironmentName environment;

    @CommandLine.Option(names = {"--distribution-name", "-n"}, description = "Specify a name of a distribution. Required.", required = true)
    private DistributionName distributionName;

    @CommandLine.Option(names = {"--distribution-id", "-i"}, description = "Specify an id of a distribution.")
    private DistributionId distributionId;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    private GetDistributionOutputService getDistributionOutputService;
    private ObjectMapperFactory objectMapperFactory;
    private ConsolePrinter consolePrinter;

    @SuppressWarnings("unused")
    public GetDistributionOutputCommand() {
    }


    @Inject
    public GetDistributionOutputCommand(GetDistributionOutputService getDistributionOutputService,
                                        ObjectMapperFactory objectMapperFactory,
                                        ConsolePrinter consolePrinter) {
        this.getDistributionOutputService = requireNonNull(getDistributionOutputService,
                                                           "getDistributionOutputService");
        this.objectMapperFactory = requireNonNull(objectMapperFactory, "objectMapperFactory");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }


    @Override
    public void run() {

        try {
            String distributionOutput = getDistributionOutputService.getDistributionOutput(new GetDistributionOutputRequest(
                    distributionName,
                    environment,
                    distributionId));

            ObjectMapper objectMapper = objectMapperFactory.getObjectMapper();
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
                                           .writeValueAsString(objectMapper.readTree(distributionOutput)));
        } catch (Exception e) {
            CliError error = ErrorResolver.resolve(e);
            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        }


    }
}
