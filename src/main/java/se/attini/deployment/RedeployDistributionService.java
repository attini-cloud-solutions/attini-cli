package se.attini.deployment;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.function.Predicate;

import se.attini.client.AwsClientFactory;
import se.attini.deployment.file.config.AttiniConfigFile;
import se.attini.deployment.file.config.AttiniConfigFiles;
import se.attini.domain.BucketName;
import se.attini.domain.Distribution;
import se.attini.domain.DistributionId;
import se.attini.domain.Environment;
import se.attini.domain.ObjectIdentifier;
import se.attini.environment.EnvironmentUserInput;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.Tag;

public class RedeployDistributionService {

    private static final String ATTINI_DISTRIBUTION_ID_TAG = "distributionId";
    private final AwsClientFactory awsClientFactory;
    private final DeploymentOrigin deploymentOrigin;
    private final EnvironmentUserInput environmentUserInput;
    private final ConfirmDeploymentUserInput deploymentUserInput;
    private final AttiniConfigFiles attiniConfigFiles;

    public RedeployDistributionService(AwsClientFactory awsClientFactory,
                                       DeploymentOrigin deploymentOrigin,
                                       EnvironmentUserInput environmentUserInput,
                                       ConfirmDeploymentUserInput deploymentUserInput,
                                       AttiniConfigFiles attiniConfigFiles) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.deploymentOrigin = requireNonNull(deploymentOrigin, "deploymentOrigin");
        this.environmentUserInput = requireNonNull(environmentUserInput, "environmentUserInput");
        this.deploymentUserInput = requireNonNull(deploymentUserInput, "deploymentUserInput");
        this.attiniConfigFiles = requireNonNull(attiniConfigFiles, "attiniConfigFiles");
    }

    public DeployDistributionResponse redeployDistribution(DeployDistributionRequest request) {
        try(S3Client s3Client = awsClientFactory.s3Client()) {
            Environment environment = environmentUserInput.getEnvironment(request);

            BucketName bucketName = deploymentOrigin.getDeploymentOriginBucketName();
            ObjectVersion objectVersion = getDistributionObjectVersion(request, s3Client, bucketName, environment);

            byte[] distributionZip = getObject(s3Client, bucketName, objectVersion);

            AttiniConfigFile attiniConfigFile = attiniConfigFiles.getAttiniConfigFile(distributionZip);

            if (!request.isForceDeployment()){
                deploymentUserInput.confirmDeployment(environment, request.getDistribution().getDistributionName() ,attiniConfigFile);
            }

            ObjectIdentifier objectIdentifier = deployObject(s3Client,
                                                             bucketName,
                                                             distributionZip,
                                                             objectVersion.key());
            return DeployDistributionResponse.builder().setDistributionName(request.getDistribution().getDistributionName()).setEnvironment(environment).setObjectIdentifier(objectIdentifier).build();

        }

    }

    private ObjectVersion getDistributionObjectVersion(DeployDistributionRequest request,
                                                       S3Client s3Client,
                                                       BucketName bucketName,
                                                       Environment environment) {
        return getObjectVersions(bucketName,
                                 s3Client,
                                 environment,
                                 request.getDistribution())
                .stream()
                .filter(isCorrectVersion(request.getDistribution().getDistributionId(), s3Client, bucketName))
                .findAny()
                .orElseThrow(() -> new DeployDistributionServiceException(
                        "no distribution was found with distribution id = " + request.getDistribution().getDistributionId().getId()));
    }

    private static List<ObjectVersion> getObjectVersions(BucketName bucketName,
                                                         S3Client s3Client,
                                                         Environment environment,
                                                         Distribution distribution) {
        return s3Client.listObjectVersions(
                ListObjectVersionsRequest.builder()
                                         .bucket(bucketName.getName())
                                         .prefix(createKeyPrefix(environment, distribution))
                                         .build())
                       .versions()
                       .stream()
                       .filter(objectVersion -> objectVersion.key().contains(".zip"))
                       .collect(toList());
    }

    private ObjectIdentifier deployObject(S3Client s3Client, BucketName bucketName, byte[] object, String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .bucket(bucketName.getName())
                                                            .key(key)
                                                            .build();

        PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromBytes(object));

       return ObjectIdentifier.create(key + "#" + response.versionId());
    }

    private static byte[] getObject(S3Client s3Client, BucketName bucketName, ObjectVersion version) {
        return s3Client.getObject(GetObjectRequest.builder()
                                                  .key(version.key())
                                                  .bucket(bucketName.getName())
                                                  .versionId(version.versionId())
                                                  .build(),
                                  ResponseTransformer.toBytes()).asByteArray();
    }

    private static Predicate<ObjectVersion> isCorrectVersion(DistributionId distributionId,
                                                             S3Client s3Client,
                                                             BucketName bucketName) {
        return version -> s3Client.getObjectTagging(GetObjectTaggingRequest.builder()
                                                                           .bucket(bucketName.getName())
                                                                           .versionId(version.versionId())
                                                                           .key(version.key())
                                                                           .build())
                                  .tagSet()
                                  .stream()
                                  .anyMatch(isCorrectDistribution(distributionId));
    }

    private static Predicate<Tag> isCorrectDistribution(DistributionId distributionId) {
        return tag -> tag.key()
                         .equals(ATTINI_DISTRIBUTION_ID_TAG) && tag.value()
                                                                   .equals(distributionId
                                                                                   .getId());
    }

    private static String createKeyPrefix(Environment environment, Distribution distribution) {
        return environment
                       .getName().getName() + "/" + distribution
                       .getDistributionName()
                       .getName();
    }

}
