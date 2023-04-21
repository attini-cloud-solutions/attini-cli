package se.attini.profile;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import se.attini.EnvironmentVariables;
import se.attini.cli.global.GlobalConfig;
import se.attini.domain.Profile;
import se.attini.domain.Region;
import software.amazon.awssdk.profiles.ProfileFile;


public class ProfileFacade {

    private final Map<Profile, Region> regionCache = new ConcurrentHashMap<>();
    private final EnvironmentVariables environmentVariables;
    private final GlobalConfig globalConfig;

    public ProfileFacade(EnvironmentVariables environmentVariables,
                         GlobalConfig globalConfig) {
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    public List<Profile> loadProfiles() {
        return ProfileFile.defaultProfileFile()
                          .profiles()
                          .keySet()
                          .stream()
                          .map(Profile::create)
                          .collect(Collectors.toList());
    }

    private Region getProfileRegion(Profile profile) {
        String awsRegion = environmentVariables.getAwsRegion();
        if (awsRegion != null) {
            return Region.create(awsRegion);
        }
        try {
            Process process = new ProcessBuilder()
                    .command(List.of(environmentVariables.getShell(),
                                     "-c",
                                     "export AWS_CLI_AUTO_PROMPT=off && aws configure get region --profile " + profile.getProfileName()))
                    .start();
            String region = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
            if (region == null) {
                throw new IllegalArgumentException("No region is configured for profile =" + profile.getProfileName());
            }
            return Region.create(region);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Region getProfileRegion() {
        String awsRegion = environmentVariables.getAwsRegion();
        if (awsRegion != null) {
            return Region.create(awsRegion);
        }
        try {
            Process process = new ProcessBuilder()
                    .command(List.of(environmentVariables.getShell(),
                                     "-c",
                                     "export AWS_CLI_AUTO_PROMPT=off && aws configure get region"))
                    .start();
            String region = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
            if (region == null) {
                throw new IllegalArgumentException("No default region is configured");
            }
            return Region.create(region);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Region getRegion() {

        return regionCache.computeIfAbsent(globalConfig.getProfile().orElse(Profile.create("Default")), profile -> {
            if (globalConfig.getRegion().isPresent()) {
                return globalConfig.getRegion().get();
            }
            if (globalConfig.getProfile().isPresent()) {
                return getProfileRegion(globalConfig.getProfile().get());
            }

            return getProfileRegion();
        });


    }

}
