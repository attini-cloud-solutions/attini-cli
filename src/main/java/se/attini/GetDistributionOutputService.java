package se.attini;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import se.attini.client.AwsClientFactory;
import se.attini.domain.DistributionId;
import se.attini.domain.Environment;
import se.attini.domain.Region;
import se.attini.environment.EnvironmentUserInput;
import se.attini.profile.ProfileFacade;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.sts.StsClient;


public class GetDistributionOutputService {

    private final static String KEY = "outputs/%s/%s/%s/output.json";
    private final static String BUCKET = "attini-artifact-store-%s-%s";

    private final AwsClientFactory awsClientFactory;
    private final ProfileFacade profileFacade;
    private final EnvironmentUserInput environmentUserInput;

    public GetDistributionOutputService(AwsClientFactory awsClientFactory,
                                        ProfileFacade profileFacade, EnvironmentUserInput environmentUserInput) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.profileFacade = requireNonNull(profileFacade, "profileFacade");
        this.environmentUserInput = requireNonNull(environmentUserInput, "environmentUserInput");
    }

    public String getDistributionOutput(GetDistributionOutputRequest request) {

        Region givenRegion = profileFacade.getRegion();
        try (StsClient stsClient = awsClientFactory.stsClient()) {
            String account = stsClient.getCallerIdentity().account();
            Environment environment = environmentUserInput.getEnvironment(request);
            String distId = request.getDistributionId()
                                   .map(DistributionId::getId)
                                   .orElseGet(() -> getDistId(request, environment));
            String key = String.format(KEY,
                                       environment.getName().getName(),
                                       request.getDistributionName().getName(),
                                       distId);
            String bucket = String.format(BUCKET, givenRegion.getName(), account);

            try (S3Client s3Client = awsClientFactory.s3Client()) {
                return new String(s3Client.getObject(GetObjectRequest.builder()
                                                                     .key(key)
                                                                     .bucket(bucket)
                                                                     .build(),
                                                     ResponseTransformer.toBytes())
                                          .asByteArray());
            } catch (NoSuchKeyException e) {
                throw new IllegalArgumentException(
                        "No output found for distributionName: %s, distributionId: %s. This could be because the distribution is not deployed in the current environment, because the deployment failed, or because the output has been cleaned by the artifact store life cycle policy".formatted(
                                request.getDistributionName().getName(), distId));
            }

        }

    }

    private String getDistId(GetDistributionOutputRequest request, Environment environment) {
        try (DynamoDbClient dynamoDbClient = awsClientFactory.dynamoClient()) {
            AttributeValue attributeValue = dynamoDbClient
                    .getItem(GetItemRequest.builder()
                                           .tableName("AttiniResourceStatesV1")
                                           .key(Map.of("resourceType",
                                                       AttributeValue.builder()
                                                                     .s("Distribution")
                                                                     .build(),
                                                       "name",
                                                       AttributeValue.builder()
                                                                     .s(environment.getName()
                                                                                   .getName() + "-" + request.getDistributionName()
                                                                                                             .getName())
                                                                     .build()))
                                           .build())
                    .item()
                    .get("distributionId");

            if (attributeValue == null) {
                throw new IllegalArgumentException("Distribution output for distribution " + request.getDistributionName()
                                                                                                    .getName() + " not found");
            }

            return attributeValue.s();
        }

    }
}
