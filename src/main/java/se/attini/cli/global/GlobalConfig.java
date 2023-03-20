package se.attini.cli.global;

import java.util.Optional;

import se.attini.domain.Profile;
import se.attini.domain.Region;

public class GlobalConfig {

    private Region region;

    private Profile profile;

    private boolean printAsJson;

    private boolean debug;
    public Optional<Region> getRegion() {
        return Optional.ofNullable(region);
    }

    void setRegion(Region region) {
        this.region = region;
    }

    public Optional<Profile> getProfile() {
        return Optional.ofNullable(profile);
    }

    void setProfile(Profile profile) {
        this.profile = profile;
        System.setProperty("aws.profile", profile.getProfileName());
    }

    public boolean printAsJson() {
        return printAsJson;
    }

    void setPrintAsJson(boolean printAsJson) {
        this.printAsJson = printAsJson;
    }

    public boolean isDebug() {
        return debug;
    }

    void setDebug(boolean debug) {
        this.debug = debug;
    }
}
