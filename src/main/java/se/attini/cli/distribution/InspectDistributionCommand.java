package se.attini.cli.distribution;


import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.deployment.file.config.AttiniConfigFile;
import se.attini.deployment.file.config.AttiniConfigFiles;
import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;
import se.attini.domain.FilePath;
import se.attini.util.ObjectMapperFactory;

@CommandLine.Command(name = "inspect", description = "Inspect a local distribution.")
public class InspectDistributionCommand implements Runnable {


    @CommandLine.Parameters(description = "Specify a path to a packaged distribution. Required.")
    private FilePath target;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;


    @CommandLine.Mixin
    private JsonOption jsonOption;

    @CommandLine.Mixin
    private DebugOption debugOption;

    private final AttiniConfigFiles attiniConfigFiles;
    private final ObjectMapperFactory objectMapperFactory;
    private final ConsolePrinter consolePrinter;

    @Inject
    public InspectDistributionCommand(AttiniConfigFiles attiniConfigFiles,
                                      ObjectMapperFactory objectMapperFactory,
                                      ConsolePrinter consolePrinter) {
        this.attiniConfigFiles = requireNonNull(attiniConfigFiles, "attiniConfigFiles");
        this.objectMapperFactory = requireNonNull(objectMapperFactory, "objectMapperFactory");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }


    @Override
    public void run() {
        try {
            if (!target.getSourceType().equals(FilePath.SourceType.FILE_SYSTEM_ZIP)) {
                throw new IllegalArgumentException(
                        "Illegal path. Only local, already packaged distributions are supported.");
            }
            AttiniConfigFile attiniConfigFile = attiniConfigFiles.getAttiniConfigFile(getTarget());
            DistributionName distributionName = attiniConfigFile.getDistributionName();

            ObjectWriter objectWriter = objectMapperFactory.getObjectMapper().writerWithDefaultPrettyPrinter();

            System.out.println(objectWriter.writeValueAsString(new Distribution(attiniConfigFile.getDistributionId()
                                                                                                .map(DistributionId::getId)
                                                                                                .orElse(null),
                                                                                distributionName.getName(),
                                                                                attiniConfigFile.getDistributionTags(),
                                                                                attiniConfigFile.getVersion()
                                                                                                .orElse(null))));


        } catch (Exception e) {
            CliError error = ErrorResolver.resolve(e);
            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        }
    }

    private byte[] getTarget() {
        try {
            return Files.readAllBytes(Path.of(target.getPath()));
        } catch (NoSuchFileException e) {
            throw new IllegalArgumentException("Target does not exist");

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read specified target");
        }
    }

    @Introspected
    @ReflectiveAccess
    public record Distribution(String distributionId, String distributionName, Map<String, String> distributionTags,
                               @JsonInclude(JsonInclude.Include.NON_NULL) String version) {

    }
}
