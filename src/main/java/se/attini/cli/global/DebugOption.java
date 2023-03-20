package se.attini.cli.global;

import static java.util.Objects.requireNonNull;

import jakarta.inject.Inject;
import picocli.CommandLine;

public class DebugOption {

    private final GlobalConfig globalConfig;


    @Inject
    public DebugOption(GlobalConfig globalConfig) {
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    @CommandLine.Option(names = {"--debug"}, hidden = true, description = "Print additional error info, only useful for debugging the cli application.")
    private void debug(boolean debug){
        globalConfig.setDebug(debug);
    }

}
