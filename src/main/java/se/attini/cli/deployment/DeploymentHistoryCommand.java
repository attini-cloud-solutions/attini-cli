package se.attini.cli.deployment;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.PrintItem;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.deployment.GetDeploymentHistoryRequest;
import se.attini.deployment.history.DeploymentHistoryFacade;
import se.attini.domain.Deployment;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;
import se.attini.util.ObjectMapperFactory;

@Command(name = "history", versionProvider = AttiniCliCommand.VersionProvider.class, description = "List previous deployments.", usageHelpAutoWidth = true)
public class DeploymentHistoryCommand implements Runnable {


    private final DeploymentHistoryFacade deploymentHistoryFacade;
    private final ObjectMapperFactory objectMapperFactory;
    private final ConsolePrinter consolePrinter;

    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @Option(names = {"--distribution-name", "-n"}, required = true, description = "Specify a name of a distribution. Required.", hideParamSyntax = true)
    private DistributionName distributionName;

    @Option(names = {"--environment", "-e"}, description = "Specify an environment. Required if there is more then one environment in your account.")
    private EnvironmentName environment;

    @Option(names = {"--output-limit", "-o"}, description = "How many deployments to list, default = 10.")
    private int outputLimit = 10;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    @Inject
    public DeploymentHistoryCommand(DeploymentHistoryFacade deploymentHistoryFacade,
                                    ObjectMapperFactory objectMapperFactory,
                                    ConsolePrinter consolePrinter) {
        this.deploymentHistoryFacade = requireNonNull(deploymentHistoryFacade, "deploymentHistoryFacade");
        this.objectMapperFactory = requireNonNull(objectMapperFactory, "objectMapperFactory");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {
        try {
            List<Deployment> deploymentHistory =
                    deploymentHistoryFacade.getDeploymentHistory(GetDeploymentHistoryRequest.builder()
                                                                                            .setDistributionName(
                                                                                                    distributionName)
                                                                                            .setEnvironment(environment)
                                                                                            .build());

            if (deploymentHistory.isEmpty()) {
                System.out.println("No distribution with name " + distributionName.getName() + " found in region");
                return;
            }

            String result = objectMapperFactory.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
                                               .writerWithDefaultPrettyPrinter()
                                               .writeValueAsString(deploymentHistory.stream()
                                                                                  .skip(getPrintLimit(deploymentHistory.size()))
                                                                                  .map(DeploymentData::new)
                                                                                  .collect(
                                                                                          Collectors.toList()));
            consolePrinter.print(PrintItem.message(result));
        } catch (Exception e) {
            CliError error = ErrorResolver.resolve(e);
            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        }

    }


    private int getPrintLimit(int listSize) {
        if (listSize <= outputLimit) {
            return 0;
        }
        return listSize - outputLimit;
    }
}
