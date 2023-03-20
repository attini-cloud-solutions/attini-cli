package se.attini.cli.ops;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorCode;
import se.attini.cli.ErrorResolver;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.logs.ExportLogsRequest;
import se.attini.logs.ExportLogsService;

@CommandLine.Command(name = "export-logs", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Export all attini logs in a given time span.")
public class ExportLogsCommand implements Runnable {

    private final ExportLogsService exportLogsService;
    private final ConsolePrinter consolePrinter;
    @CommandLine.Mixin
    private RegionAndProfileOption regionAndProfileOptions;

    @CommandLine.Mixin
    private DebugOption debugOption;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;


    static class FromExclusive {
        @CommandLine.Option(names = {"--from"}, description = "The start of the time range (local time) for what logs should be exported. Default format is ISO-8601 extended local time format, ex: 2021-10-29T09:10:00", required = true)
        String from;
        @CommandLine.Option(names = {"--from-epoch-milli"}, description = "The start of the time range for what logs should be exported in epoch milliseconds. Can not be used together with the --from option", required = true)
        Long fromEpoch;
    }

    @CommandLine.ArgGroup(multiplicity = "1")
    FromExclusive fromExclusive;


    static class ToExclusive {
        @CommandLine.Option(names = {"--to"}, description = "The end of the time range (local time) for what logs should be exported. Default format is ISO-8601 extended local time format, ex: 2021-10-29T09:10:00", required = true)
        String to;

        @CommandLine.Option(names = {"--to-epoch-milli"}, description = "The end of the time range for what logs should be exported in epoch milliseconds. Can not be used together with the --to option", required = true)
        Long toEpoch;
    }

    @CommandLine.ArgGroup(multiplicity = "1")
    ToExclusive toExclusive;


    @Inject
    public ExportLogsCommand(ExportLogsService exportLogsService,
                             ConsolePrinter consolePrinter) {
        this.exportLogsService = requireNonNull(exportLogsService, "exportLogsService");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }


    @Override
    public void run() {

        long from = fromExclusive.from != null ? parseDate(fromExclusive.from) : fromExclusive.fromEpoch;
        long to = toExclusive.to != null ? parseDate(toExclusive.to) : toExclusive.toEpoch;

        try {
            exportLogsService.exportLogs(ExportLogsRequest.builder()
                                                          .setStartTime(from)
                                                          .setEndTime(to)
                                                          .build());
        } catch (Exception e) {
            CliError cliError = ErrorResolver.resolve(e);
            consolePrinter.printError(cliError);
            System.exit(cliError.getErrorCode().getExitCode());
        }
    }

    private long parseDate(String time) {
        try {
            return LocalDateTime.parse(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            CliError cliError = CliError.create(ErrorCode.IllegalArgument,
                                                "Could not parse date: " + time + ", only ISO-8601 extended local time format is supported, ex: 2021-10-29T09:10:00");
            consolePrinter.printError(cliError);
            System.exit(cliError.getErrorCode().getExitCode());
        }
        return 0;
    }
}
