package se.attini.cli.global;

import static java.util.Objects.requireNonNull;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.ProfileCompletionCandidates;
import se.attini.cli.RegionCompletionCandidates;
import se.attini.domain.Profile;
import se.attini.domain.Region;

public class RegionAndProfileOption {
    private final GlobalConfig globalConfig;

    @Inject
    public RegionAndProfileOption(GlobalConfig globalConfig) {
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    @CommandLine.Option(names = {"--region", "-r"}, description = "Specify an aws region (ex eu-west-1), if absent the default region will be used.", completionCandidates = RegionCompletionCandidates.class)
    private void region(Region region){
        globalConfig.setRegion(region);
    }

    @CommandLine.Option(names = {"--profile", "-p"}, description = "Specify a configured profile, if absent the default profile will be used.", completionCandidates = ProfileCompletionCandidates.class)
    private void profile(Profile profile){
        globalConfig.setProfile(profile);
    }
}
