package se.attini.cli.distribution;


import static java.util.Objects.requireNonNull;

import java.util.Map;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.PrintItem;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.distribution.DownloadDistributionRequest;
import se.attini.distribution.DownloadDistributionService;
import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;
import se.attini.util.ObjectMapperFactory;

@CommandLine.Command(name = "download", description = "Download a distribution from the artifact store.", usageHelpAutoWidth = true)
public class DownloadDistributionCommand implements Runnable {

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;
    @CommandLine.Option(names = {"--distribution-name", "-n"}, description = "Specify a name of a distribution. Required.", required = true)
    private DistributionName distributionName;

    @CommandLine.Option(names = {"--distribution-id", "-i"}, description = "Specify an id of a distribution. Default is the latest deployed.")
    private DistributionId distributionId;

    @CommandLine.Option(names = {"-e", "--environment"}, description = "Specify an environment. Required if there is more then one environment in your account.")
    private EnvironmentName environment;

    private DownloadDistributionService downloadDistributionService;
    private ObjectMapperFactory objectMapperFactory;
    private ConsolePrinter consolePrinter;

    @SuppressWarnings("unused")
    public DownloadDistributionCommand() {
    }

    @Inject
    public DownloadDistributionCommand(DownloadDistributionService downloadDistributionService,
                                       ObjectMapperFactory objectMapperFactory,
                                       ConsolePrinter consolePrinter) {
        this.downloadDistributionService = requireNonNull(downloadDistributionService, "downloadDistributionService");
        this.objectMapperFactory = requireNonNull(objectMapperFactory, "objectMapperFactory");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {

        try {
            String distribution = downloadDistributionService.downloadDistribution(new DownloadDistributionRequest(
                    distributionName,
                    environment,
                    distributionId));

           consolePrinter.print(PrintItem.message(objectMapperFactory.getObjectMapper()
                                                                      .writerWithDefaultPrettyPrinter()
                                                                      .writeValueAsString(Map.of("fileName", distribution))));
        } catch (Exception e) {
            consolePrinter.printError(ErrorResolver.resolve(e));
        }


    }
}
