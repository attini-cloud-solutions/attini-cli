package se.attini.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.attini.EnvironmentVariables;
import se.attini.client.AwsClientFactory;
import se.attini.deployment.file.config.AttiniConfigFile;
import se.attini.deployment.file.config.AttiniConfigFiles;
import se.attini.domain.BucketName;
import se.attini.domain.DistributionName;
import se.attini.domain.Environment;
import se.attini.domain.EnvironmentName;
import se.attini.domain.EnvironmentType;
import se.attini.domain.FilePath;
import se.attini.environment.EnvironmentUserInput;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;


@ExtendWith(MockitoExtension.class)
class DeployDistributionServiceTest {

    public static final Path PATH = Paths.get("src", "test", "resources", "test-dist", "infra.zip");
    public static final DistributionName DISTRIBUTION_NAME = DistributionName.create("infra");
    public static final String VERSION_ID = "12323232";

    @Mock
    AwsClientFactory awsClientFactory;
    @Mock
    AttiniConfigFiles attiniConfigFiles;
    @Mock
    S3Client s3Client;
    @Mock
    AttiniConfigFile attiniConfigFile;

    @Mock
    EnvironmentUserInput environmentUserInput;

    @Mock
    ConfirmDeploymentUserInput confirmDeploymentUserInput;

    @Mock
    EnvironmentVariables environmentVariables;

    @Mock
    DeploymentOrigin deploymentOrigin;


    DeployDistributionService deployDistributionService;

    @BeforeEach
    void setUp() {
        deployDistributionService = new DeployDistributionService(awsClientFactory,
                                                                  attiniConfigFiles,
                                                                  environmentUserInput,
                                                                  confirmDeploymentUserInput,
                                                                  environmentVariables,
                                                                  deploymentOrigin);

        when(awsClientFactory.s3Client()).thenReturn(s3Client);
        when(attiniConfigFiles.getAttiniConfigFile(any(byte[].class))).thenReturn(attiniConfigFile);
        when(attiniConfigFile.getDistributionName()).thenReturn(DISTRIBUTION_NAME);
        when(deploymentOrigin.getDeploymentOriginBucketName()).thenReturn(BucketName.create("a-bucket"));
        when(s3Client.putObject(any(PutObjectRequest.class),
                                any(RequestBody.class))).thenReturn(PutObjectResponse.builder()
                                                                                     .versionId(VERSION_ID)
                                                                                     .build());
        when(environmentUserInput.getEnvironment(any())).thenReturn(Environment.create(EnvironmentName.create("dev"),
                                                                                       EnvironmentType.TEST));
    }

    @Test
    void deployArtifact() {

        CreateAndDeployDistributionRequest request = CreateAndDeployDistributionRequest.builder()
                                                                                       .setEnvironment(EnvironmentName.create(
                                                                                               "dev"))
                                                                                       .setPath(FilePath.create(PATH.toString()))
                                                                                       .build();

        String key = String.format("%s/%s/%s.zip", "dev", DISTRIBUTION_NAME.getName(), DISTRIBUTION_NAME.getName());

        DeployDistributionResponse response = deployDistributionService.deployDistribution(request);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals(key + "#" + VERSION_ID, response.getObjectIdentifier().getValue());

    }
}
