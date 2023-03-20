package se.attini.deployment;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import javax.net.ssl.HttpsURLConnection;

import se.attini.EnvironmentVariables;
import se.attini.cli.LoadingIndicator;
import se.attini.client.AwsClientFactory;
import se.attini.deployment.file.config.AttiniConfigFile;
import se.attini.deployment.file.config.AttiniConfigFiles;
import se.attini.domain.BucketName;
import se.attini.domain.DistributionName;
import se.attini.domain.Environment;
import se.attini.domain.FilePath;
import se.attini.domain.ObjectIdentifier;
import se.attini.domain.Region;
import se.attini.environment.EnvironmentUserInput;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class DeployDistributionService {

    private final AwsClientFactory awsClientFactory;
    private final AttiniConfigFiles attiniConfigFiles;
    private final EnvironmentUserInput environmentUserInput;
    private final ConfirmDeploymentUserInput confirmDeploymentUserInput;
    private final EnvironmentVariables environmentVariables;
    private final DeploymentOrigin deploymentOrigin;

    public DeployDistributionService(AwsClientFactory awsClientFactory,
                                     AttiniConfigFiles attiniConfigFiles,
                                     EnvironmentUserInput environmentUserInput,
                                     ConfirmDeploymentUserInput confirmDeploymentUserInput,
                                     EnvironmentVariables environmentVariables,
                                     DeploymentOrigin deploymentOrigin) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "s3Client");
        this.attiniConfigFiles = requireNonNull(attiniConfigFiles, "attiniConfigFiles");
        this.environmentUserInput = requireNonNull(environmentUserInput, "environmentUserInput");
        this.confirmDeploymentUserInput = requireNonNull(confirmDeploymentUserInput, "confirmDeploymentUserInput");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.deploymentOrigin = requireNonNull(deploymentOrigin, "deploymentOrigin");
    }


    public DeployDistributionResponse deployDistribution(CreateAndDeployDistributionRequest request) {


        S3Client s3Client = awsClientFactory.s3Client();
        BucketName bucketName = deploymentOrigin.getDeploymentOriginBucketName();

        return deployDistribution(request.getPath(),
                                  environmentUserInput.getEnvironment(request),
                                  s3Client,
                                  bucketName,
                                  request);
    }


    private DeployDistributionResponse deployDistribution(FilePath path,
                                                          Environment environment,
                                                          S3Client s3Client,
                                                          BucketName bucket,
                                                          CreateAndDeployDistributionRequest deployDistributionRequest) {

        byte[] bytes = getFileToUpload(path, s3Client);
        AttiniConfigFile attiniConfigFile = attiniConfigFiles.getAttiniConfigFile(bytes);
        DistributionName distributionName  = attiniConfigFile.getDistributionName();

        LoadingIndicator loadingIndicator = new LoadingIndicator("Uploading distribution",
                                                                 environmentVariables.isDisableAnsiColor());

        if (!deployDistributionRequest.forceDeployment()) {
            confirmDeploymentUserInput.confirmDeployment(environment,
                                                         distributionName,
                                                         attiniConfigFile);
        }

        try {
            if (deployDistributionRequest.isJson()) {
                System.out.println("{\"timestamp\":" + Instant.now().toEpochMilli() +
                                   ",\"type\":\"string\",\"data\":\"Uploading distribution\"}");
            } else {
                loadingIndicator.startSpinner();
            }

            String key = String.format("%s/%s/%s.zip",
                                       environment.getName()
                                                  .getName(),
                                       distributionName.getName(),
                                       distributionName.getName());
            PutObjectRequest request = PutObjectRequest.builder()
                                                       .bucket(bucket.getName())
                                                       .key(key)
                                                       .build();
            PutObjectResponse response = s3Client.putObject(request,
                                          RequestBody.fromBytes(bytes));

            if (deployDistributionRequest.isJson()) {
                System.out.println("{\"timestamp\":" + Instant.now().toEpochMilli() +
                                   ",\"type\":\"string\",\"data\":\"Uploaded distribution\"}");
            } else {
                loadingIndicator.stopSpinner();
                System.out.println("Uploaded distribution");
            }

            return DeployDistributionResponse.builder()
                                             .setDistributionName(distributionName)
                                             .setEnvironment(environment)
                                             .setObjectIdentifier(ObjectIdentifier.create(key + "#" + response.versionId()))
                                             .build();
        } catch (Exception e) {
            loadingIndicator.stopSpinner();
            throw e;
        }


    }

    private byte[] getFileToUpload(FilePath filePath, S3Client s3Client) {
        switch (filePath.getSourceType()) {
            case HTTPS -> {
                try {
                    HttpsURLConnection connection = (HttpsURLConnection) new URL(filePath.getPath()).openConnection();
                    connection.setRequestMethod("GET");
                    InputStream in = connection.getInputStream();
                    return in.readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException("Could not get file from url " + filePath.getPath(), e);
                }
            }
            case S3 -> {
                URI uri = URI.create(filePath.getPath());
                GetBucketLocationResponse bucketLocation = s3Client.getBucketLocation(GetBucketLocationRequest.builder()
                                                                                                              .bucket(uri.getHost())
                                                                                                              .build());
                try (S3Client newClient = awsClientFactory.s3Client(Region.create(bucketLocation.locationConstraintAsString()))) {
                    return newClient.getObject(GetObjectRequest.builder()
                                                               .bucket(uri.getHost())
                                                               .key(uri.getPath().substring(1))
                                                               .build(),
                                               ResponseTransformer.toBytes()).asByteArray();

                }
            }
            default -> {
                try {
                    return Files.readAllBytes(Paths.get(filePath.getPath()));
                } catch (IOException e) {
                    throw new UncheckedIOException("Could not get zip file from path " + filePath.getPath(), e);
                }
            }
        }
    }


}
