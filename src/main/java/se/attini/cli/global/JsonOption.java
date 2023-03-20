package se.attini.cli.global;

import static java.util.Objects.requireNonNull;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.PrintUtil;

public class JsonOption {

    private final GlobalConfig globalConfig;

    private boolean json;

    @Inject
    public JsonOption(GlobalConfig globalConfig) {
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    @CommandLine.Option(names = {"--json"}, description = "Use json as output format instead of yaml.")
     private void json(boolean json){
        this.json = json;
        PrintUtil.clearColors();
        globalConfig.setPrintAsJson(json);
    }

    public boolean printAsJson() {
        return json;
    }
}
