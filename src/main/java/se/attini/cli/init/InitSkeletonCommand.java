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

@CommandLine.Command(name = "skeleton", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Create a minimum configuration project.")
public class InitSkeletonCommand implements Runnable {

    @CommandLine.Mixin
    private DebugOption debugOption;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;
    private final ConsolePrinter consolePrinter;

    @Inject
    public InitSkeletonCommand(ConsolePrinter consolePrinter) {
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {
        try {
            InitProjectService.initSkeleton();
            consolePrinter.print(PrintItem.message(SUCCESS, "Successfully initialized skeleton project"));
        } catch (Exception e) {
            CliError cliError = ErrorResolver.resolve(e);
            consolePrinter.printError(cliError);
            System.exit(cliError.getErrorCode().getExitCode());
        }
    }
}
