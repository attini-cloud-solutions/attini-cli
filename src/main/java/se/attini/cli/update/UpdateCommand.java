package se.attini.cli.update;


import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.EnvironmentVariables;
import se.attini.cli.AttiniCliCommand;
import se.attini.cli.CliError;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.ErrorResolver;
import se.attini.cli.global.DebugOption;

@CommandLine.Command(name = "update-cli", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Update the Attini CLI.", usageHelpAutoWidth = true)
public class UpdateCommand implements Runnable {

    @CommandLine.Mixin
    private DebugOption debugOption;

    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    private boolean help;

    @CommandLine.Option(names = {"--version", "-v"}, description = "Specify what version should be installed. Defaults to latest.")
    private String version;
    private final EnvironmentVariables environmentVariables;
    private final ConsolePrinter consolePrinter;


    @Inject
    public UpdateCommand(EnvironmentVariables environmentVariables, ConsolePrinter consolePrinter) {
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {

        try {

            String ver = version == null ? "" : "-v " + version;

            String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

            String command = "curl -fsS -o attini_c75e7621.sh https://docs.attini.io/blob/attini-cli/install-cli.sh && chmod +x attini_c75e7621.sh && ./attini_c75e7621.sh "
                             + ver + " -n -t "
                             + path.substring(
                    0, path.lastIndexOf(File.separator)) + " && rm -f attini_c75e7621.sh";
            int i = new ProcessBuilder()
                    .inheritIO()
                    .command(List.of(environmentVariables.getShell(),
                                     "-c",
                                     command
                    ))
                    .start().waitFor();

            if (i != 0) {
                System.exit(i);
            }

        } catch (IOException | InterruptedException e) {
            CliError cliError = ErrorResolver.resolve(e);
            consolePrinter.printError(cliError);
            System.exit(cliError.getErrorCode().getExitCode());
        }
    }

}
