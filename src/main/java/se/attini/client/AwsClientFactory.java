package se.attini.client;

import static java.util.Objects.requireNonNull;

import java.time.Duration;

import se.attini.cli.global.GlobalConfig;
import se.attini.domain.Region;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sts.StsClient;


public class AwsClientFactory {

    private final GlobalConfig globalConfig;

    public AwsClientFactory(GlobalConfig globalConfig) {
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    private SnsClient snsClient(Region region) {
        return SnsClient.builder()
                        .overrideConfiguration(getClientOverride())
                        .credentialsProvider(getCredentials())
                        .region(software.amazon.awssdk.regions.Region.of(region.getName()))
                        .build();
    }

    public SnsClient snsClient() {
        return globalConfig.getRegion()
                           .map(this::snsClient)
                           .orElseGet(() -> SnsClient.builder()
                                                     .credentialsProvider(getCredentials())
                                                     .build());
    }

    public S3Client s3Client(Region region) {
        return S3Client.builder()
                       .overrideConfiguration(getS3ClientOverride())
                       .credentialsProvider(getCredentials())
                       .region(software.amazon.awssdk.regions.Region.of(region.getName()))
                       .build();
    }

    public S3Client s3Client() {
        return globalConfig.getRegion()
                           .map(this::s3Client)
                           .orElseGet(() -> S3Client.builder()
                                                    .overrideConfiguration(getS3ClientOverride())
                                                    .credentialsProvider(getCredentials())
                                                    .build());

    }


    private StsClient stsClient(Region region) {
        return StsClient.builder()
                        .overrideConfiguration(getClientOverride())
                        .credentialsProvider(getCredentials())
                        .region(software.amazon.awssdk.regions.Region.of(region.getName()))
                        .build();
    }

    public StsClient stsClient() {
        return globalConfig.getRegion()
                           .map(this::stsClient)
                           .orElseGet(() -> StsClient.builder()
                                                     .credentialsProvider(getCredentials())
                                                     .overrideConfiguration(getClientOverride())
                                                     .build());

    }

    private CloudWatchLogsClient cloudWatchClient(Region region) {
        return CloudWatchLogsClient.builder()
                                   .credentialsProvider(getCredentials())
                                   .overrideConfiguration(getClientOverride())
                                   .region(software.amazon.awssdk.regions.Region.of(region.getName()))
                                   .build();
    }

    public CloudWatchLogsClient cloudWatchClient() {
        return globalConfig.getRegion()
                           .map(this::cloudWatchClient)
                           .orElseGet(() -> CloudWatchLogsClient.builder()
                                                                .credentialsProvider(getCredentials())
                                                                .overrideConfiguration(getClientOverride())
                                                                .build());
    }


    public DynamoDbClient dynamoClient(Region region) {
        return DynamoDbClient.builder()
                             .credentialsProvider(getCredentials())
                             .overrideConfiguration(getClientOverride())
                             .region(software.amazon.awssdk.regions.Region.of(region.getName()))
                             .build();

    }

    public DynamoDbClient dynamoClient() {
        return globalConfig.getRegion()
                           .map(this::dynamoClient)
                           .orElseGet(() -> DynamoDbClient.builder()
                                                          .credentialsProvider(getCredentials())
                                                          .overrideConfiguration(getClientOverride())
                                                          .build());

    }


    public CloudFormationClient cfnClient(Region region) {
        return CloudFormationClient.builder()
                                   .overrideConfiguration(getClientOverride())
                                   .credentialsProvider(getCredentials())
                                   .region(software.amazon.awssdk.regions.Region.of(region.getName()))
                                   .build();
    }

    public CloudFormationClient cfnClient() {
        return globalConfig.getRegion()
                           .map(this::cfnClient)
                           .orElseGet(() -> CloudFormationClient.builder()
                                                                .credentialsProvider(getCredentials())
                                                                .overrideConfiguration(getClientOverride())
                                                                .build());

    }


    public SfnClient sfnClient(Region region) {
        return SfnClient.builder()
                        .credentialsProvider(getCredentials())
                        .overrideConfiguration(getClientOverride())
                        .region(software.amazon.awssdk.regions.Region.of(region.getName()))
                        .build();
    }

    public SfnClient sfnClient() {
        return globalConfig.getRegion()
                           .map(this::sfnClient)
                           .orElseGet(() -> SfnClient.builder()
                                                     .overrideConfiguration(getClientOverride())
                                                     .credentialsProvider(getCredentials())
                                                     .build());
    }

    private static ClientOverrideConfiguration getClientOverride() {
        return ClientOverrideConfiguration.builder()
                                          .apiCallTimeout(Duration.ofSeconds(240))
                                          .apiCallAttemptTimeout(Duration.ofSeconds(15))
                                          .retryPolicy(RetryPolicy.builder()
                                                                  .numRetries(10)
                                                                  .build())
                                          .build();
    }

    private static ClientOverrideConfiguration getS3ClientOverride() {
        return ClientOverrideConfiguration.builder()
                                          .apiCallTimeout(Duration.ofMinutes(15))
                                          .apiCallAttemptTimeout(Duration.ofMinutes(5))
                                          .retryPolicy(RetryPolicy.builder()
                                                                  .numRetries(10)
                                                                  .build())
                                          .build();
    }


    private AttiniCredentialProvider getCredentials() {
        return AttiniCredentialProvider.create();
    }

}
