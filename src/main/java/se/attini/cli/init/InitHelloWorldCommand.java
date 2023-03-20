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

@CommandLine.Command(name = "hello-world", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Create a hello world project.")
public class InitHelloWorldCommand implements Runnable {

    @CommandLine.Mixin
    private DebugOption debugOption;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;
    private ConsolePrinter consolePrinter;

    @SuppressWarnings("unused")
    public InitHelloWorldCommand() {
    }

    @Inject
    public InitHelloWorldCommand(ConsolePrinter consolePrinter) {
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }



    @Override
    public void run() {
        try {
            InitProjectService.initHelloWorld();
            consolePrinter.print(PrintItem.message(SUCCESS, "Successfully initialized hello world project"));
        } catch (Exception e) {
            CliError resolve = ErrorResolver.resolve(e);
            consolePrinter.printError(resolve);
            System.exit(resolve.getErrorCode().getExitCode());
        }
    }
}
