package se.attini.cli.init;


import static java.util.Objects.requireNonNull;
import static se.attini.cli.PrintItem.PrintType.SUCCESS;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.PrintItem;
import se.attini.cli.global.DebugOption;
import se.attini.init.InitProjectService;

@CommandLine.Command(name = "simple-website", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Create an example simple website project.")
public class InitSimpleWebsiteCommand implements Runnable {

    @CommandLine.Mixin
    DebugOption debugOption;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;
    private final ConsolePrinter consolePrinter;


    @Inject
    public InitSimpleWebsiteCommand(ConsolePrinter consolePrinter) {
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {
        try {
            InitProjectService.initSimpleWebsite();
            consolePrinter.print(PrintItem.message(SUCCESS, "Successfully initialized simple website project"));
        } catch (Exception e) {
            CliError error = ErrorResolver.resolve(e);
            consolePrinter.printError(error);
            System.exit(error.getErrorCode().getExitCode());
        }
    }
}
