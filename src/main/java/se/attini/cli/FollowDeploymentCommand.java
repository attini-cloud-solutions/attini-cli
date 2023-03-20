package se.attini.cli;


import static java.util.Objects.requireNonNull;
import static se.attini.cli.ConsolePrinter.ErrorPrintType.TEXT;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import picocli.CommandLine;
import se.attini.cli.global.DebugOption;
import se.attini.cli.global.JsonOption;
import se.attini.cli.global.RegionAndProfileOption;
import se.attini.client.AwsClientFactory;
import se.attini.deployment.DeploymentOrigin;
import se.attini.deployment.FollowDeploymentRequest;
import se.attini.deployment.FollowDeploymentService;
import se.attini.deployment.history.DeploymentHistoryFacade;
import se.attini.domain.BucketName;
import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;
import se.attini.domain.Environment;
import se.attini.domain.EnvironmentName;
import se.attini.domain.ObjectIdentifier;
import se.attini.environment.EnvironmentUserInput;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.Tag;

@CommandLine.Command(name = "describe", versionProvider = AttiniCliCommand.VersionProvider.class, description = "Describe/follow a deployment. Will print info about a running deployment or a previously run deployment.")
public class FollowDeploymentCommand implements Runnable {


    private FollowDeploymentService followDeploymentService;
    private AwsClientFactory awsClientFactory;
    private EnvironmentUserInput environmentUserInput;
    private DeploymentHistoryFacade deploymentHistoryFacade;
    private DeploymentOrigin deploymentOrigin;
    private ConsolePrinter consolePrinter;
    @CommandLine.Mixin
    RegionAndProfileOption regionAndProfileOption;

    @CommandLine.Mixin
    JsonOption jsonOption;

    @CommandLine.Mixin
    DebugOption debug;

    @CommandLine.Option(names = {"--distribution-name", "-n"}, description = "Specify a name of a distribution. Required.", required = true)
    DistributionName distributionName;

    @CommandLine.Option(names = {"--distribution-id", "-i"}, description = "Specify an id of a distribution. Default is the latest deployed.")
    DistributionId distributionId;

    @CommandLine.Option(names = {"-e", "--environment"}, description = "Specify an environment. Required if there is more then one environment in your account.")
    EnvironmentName environment;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"--help", "-h"}, description = "Show information about this command.", usageHelp = true)
    boolean help;

    @SuppressWarnings("unused")
    public FollowDeploymentCommand() {
    }

    @Inject
    public FollowDeploymentCommand(FollowDeploymentService followDeploymentService,
                                   AwsClientFactory awsClientFactory,
                                   DeploymentOrigin deploymentOrigin,
                                   EnvironmentUserInput environmentUserInput,
                                   DeploymentHistoryFacade deploymentHistoryFacade,
                                   ConsolePrinter consolePrinter) {
        this.followDeploymentService = requireNonNull(followDeploymentService, "followDeploymentService");
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.environmentUserInput = requireNonNull(environmentUserInput, "environmentUserInput");
        this.deploymentHistoryFacade = requireNonNull(deploymentHistoryFacade, "deploymentHistoryFacade");
        this.deploymentOrigin = requireNonNull(deploymentOrigin, "deploymentOrigin");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    @Override
    public void run() {

        try {
            followDeploymentService.followDeployment(createRequest());
        } catch (Exception e) {
            CliError cliError = ErrorResolver.resolve(e);
            if (jsonOption.printAsJson()){
                consolePrinter.printError(cliError);
            }else {
                consolePrinter.printError(cliError, TEXT);
            }
            System.exit(cliError.getErrorCode().getExitCode());
        }
    }

    private FollowDeploymentRequest createRequest() {

        Environment givenEnvironment = environmentUserInput.getEnvironment(() -> Optional.ofNullable(environment));


        DistributionId effectiveDistId = distributionId == null ? deploymentHistoryFacade.getLatestDeployment(
                distributionName,
                givenEnvironment).getDistribution().getDistributionId() : distributionId;


        String objectIdentifier = getObjectIdentifier(distributionName,
                                                      effectiveDistId,
                                                      givenEnvironment);

        return FollowDeploymentRequest.builder()
                                      .setDistributionName(distributionName)
                                      .setEnvironment(givenEnvironment)
                                      .setObjectIdentifier(ObjectIdentifier.create(
                                              objectIdentifier))
                                      .build();
    }

    private String getObjectIdentifier(DistributionName distributionName,
                                       DistributionId distributionId,
                                       Environment environment) {

        try( S3Client s3Client = awsClientFactory.s3Client()){
            BucketName deploymentOriginBucketName = deploymentOrigin.getDeploymentOriginBucketName();
            List<ObjectVersion> versions = s3Client.listObjectVersions(ListObjectVersionsRequest.builder()
                                                                                                .bucket(deploymentOriginBucketName.getName())
                                                                                                .prefix(environment.getName()
                                                                                                                   .getName() + "/" + distributionName.getName())
                                                                                                .build())
                                                   .versions();

            for (ObjectVersion objectVersion : versions) {
                GetObjectTaggingResponse objectTagging = s3Client.getObjectTagging(GetObjectTaggingRequest.builder()
                                                                                                          .bucket(deploymentOriginBucketName.getName())
                                                                                                          .key(objectVersion.key())
                                                                                                          .versionId(
                                                                                                                  objectVersion.versionId())
                                                                                                          .build());

                if (objectTagging.tagSet().contains(Tag.builder()
                                                       .key("distributionId")
                                                       .value(distributionId.getId())
                                                       .build())) {
                    return objectVersion.key() + "#" + objectTagging.versionId();
                }
            }
        }
        throw new RuntimeException("Could not find object identifier");
    }
}
