package se.attini;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.attini.cli.global.GlobalConfig;
import se.attini.client.AwsClientFactory;
import se.attini.domain.Profile;
import software.amazon.awssdk.services.sts.StsClient;

public class AwsAccountFacade {

    private final Map<Profile, String> accountCache = new ConcurrentHashMap<>();


    private final AwsClientFactory awsClientFactory;
    private final GlobalConfig globalConfig;

    public AwsAccountFacade(AwsClientFactory awsClientFactory,
                            GlobalConfig globalConfig) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    public String getAccount() {
        return accountCache.computeIfAbsent(globalConfig.getProfile().orElse(Profile.create("default")),
                                            profile ->{
                                                try(StsClient stsClient = awsClientFactory.stsClient()) {
                                                    return stsClient.getCallerIdentity()
                                                            .account();
                                                }
                                            });
    }
}
