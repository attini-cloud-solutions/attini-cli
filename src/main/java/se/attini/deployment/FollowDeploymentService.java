package se.attini.deployment;

import static java.util.Objects.requireNonNull;
import static se.attini.cli.PrintUtil.Color.GREEN;
import static se.attini.cli.PrintUtil.Color.RED;
import static se.attini.cli.PrintUtil.Color.YELLOW;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.ROLLBACK_FAILED;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.UPDATE_COMPLETE;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_COMPLETE;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_FAILED;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import se.attini.cli.ConsolePrinter;
import se.attini.cli.PrintUtil;
import se.attini.cli.deployment.DataEmitter;
import se.attini.cli.global.GlobalConfig;
import se.attini.client.AwsClientFactory;
import se.attini.deployment.history.DeploymentHistoryFacade;
import se.attini.domain.Deployment;
import se.attini.domain.DeploymentError;
import se.attini.domain.DeploymentPlanStatus;
import se.attini.domain.DeploymentPlanStepStatus;
import se.attini.domain.DeploymentType;
import se.attini.domain.ExecutionArn;
import se.attini.domain.InitStackError;
import se.attini.domain.Region;
import se.attini.domain.StackName;
import se.attini.domain.StepStatus;
import se.attini.profile.ProfileFacade;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.sfn.model.ExecutionStatus;

public class FollowDeploymentService {


    private final AwsClientFactory awsClientFactory;
    private final DeploymentPlanStatusFacade deploymentPlanStatusFacade;
    private final DeploymentHistoryFacade deploymentHistoryFacade;
    private final DeploymentPlanStatusPrinter statusPrinter;
    private final ProfileFacade profileFacade;
    private final DataEmitter dataEmitter;
    private final GlobalConfig globalConfig;
    private final ObjectMapper objectMapper;
    private final ConsolePrinter consolePrinter;

    public FollowDeploymentService(AwsClientFactory awsClientFactory,
                                   DeploymentPlanStatusFacade deploymentPlanStatusFacade,
                                   DeploymentHistoryFacade deploymentHistoryFacade,
                                   DeploymentPlanStatusPrinter statusPrinter,
                                   ProfileFacade profileFacade,
                                   DataEmitter dataEmitter,
                                   GlobalConfig globalConfig,
                                   ObjectMapper objectMapper, ConsolePrinter consolePrinter) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.deploymentPlanStatusFacade = requireNonNull(deploymentPlanStatusFacade, "deploymentPlanStatusFacade");
        this.deploymentHistoryFacade = requireNonNull(deploymentHistoryFacade, "deploymentHistoryFacade");
        this.statusPrinter = requireNonNull(statusPrinter, "statusPrinter");
        this.profileFacade = requireNonNull(profileFacade, "profileFacade");
        this.dataEmitter = requireNonNull(dataEmitter, "dataEmitter");
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    public void followDeployment(FollowDeploymentRequest request) {

        dataEmitter.emitString("Publishing distribution");
        GetDeploymentRequest getDeploymentRequest = toGetDeploymentRequest(request);
        waitForDeployData(getDeploymentRequest);
        dataEmitter.emitString("Published distribution");
        Deployment deployment = deploymentHistoryFacade.getDeployment(getDeploymentRequest);
        dataEmitter.emitKeyValue("Environment", deployment.getEnvironment().getName(), GREEN);
        dataEmitter.emitKeyValue("DistributionName", deployment.getDistribution()
                                                               .getDistributionName()
                                                               .getName(), GREEN);

        dataEmitter.emitKeyValue("DistributionId", deployment.getDistribution().getDistributionId().getId(), GREEN);

        deployment.getVersion().ifPresent(s -> dataEmitter.emitKeyValue("Version",s, GREEN));

        if (deployment.getDeploymentError().isPresent() && deployment.getExecutionArn().isEmpty()) {
            DeploymentError deploymentError = deployment.getDeploymentError().get();
            if (deploymentError.getErrorCode().equals("AccessDenied") &&
                deploymentError.getErrorMessage()
                               .contains("cloudformation:UpdateStack")) {
                throw new IllegalStateException(
                        "Attini is not authorized to update the deployment stack. " +
                        "If you are using the Attini init deploy default role this is most likely because " +
                        "the stack already exists and is not tagged with AttiniResourceType=init-deploy. If you are " +
                        "using your own role then make sure that the role is allowed to perform updates on Cloudformation stacks. " + System.lineSeparator() + "Original error: " + deploymentError.getErrorMessage());
            }
            throw new IllegalStateException(deploymentError.getErrorMessage());
        }

        if (deployment.getStackName().isPresent()) {
            if (deployment.getDeploymentType() == DeploymentType.PLATFORM && !deployment.isInitStackUnchanged()) {
                dataEmitter.emitString("Deploying init stack");
                pollDeploymentPlanCloudFormation(deployment.getStackName().get(),
                                                 dataEmitter,
                                                 getDeploymentRequest);
            } else if(deployment.getDeploymentType() == DeploymentType.PLATFORM) {
                dataEmitter.emitString("No changes to init stack");
            }

            waitForStepGuardUpdates(getDeploymentRequest);
            deployment = deploymentHistoryFacade.getDeployment(getDeploymentRequest);
            if (deployment.getDeploymentPlanCount()
                          .map(deploymentPlanCount -> deploymentPlanCount.getValue() <= 0)
                          .orElse(false)) {
                dataEmitter.emitString("No deployment plan found");
                return;
            }
            dataEmitter.emitString("Running deployment plan");
            pollDeploymentPlanStatus(getDeploymentRequest,
                                     dataEmitter);
        } else {
            dataEmitter.emitString("No stack present in distribution");
        }
    }

    private static GetDeploymentRequest toGetDeploymentRequest(FollowDeploymentRequest request) {
        return GetDeploymentRequest.builder()
                                   .setDistributionName(request.getDistributionName())
                                   .setEnvironment(request.getEnvironment())
                                   .setObjectIdentifier(request.getObjectIdentifier())
                                   .build();
    }

    private void pollDeploymentPlanCloudFormation(StackName stackName,
                                                  DataEmitter emitter,
                                                  GetDeploymentRequest getDeploymentRequest) {

        try(DynamoDbClient dynamoDbClient = awsClientFactory.dynamoClient();
            CloudFormationClient cloudFormationClient = awsClientFactory.cfnClient()) {
            EnumSet<StackStatus> completedStatuses = EnumSet.of(ROLLBACK_COMPLETE,
                                                                CREATE_COMPLETE,
                                                                UPDATE_ROLLBACK_COMPLETE,
                                                                UPDATE_COMPLETE,
                                                                UPDATE_ROLLBACK_FAILED,
                                                                ROLLBACK_FAILED);

            waitFor(3000);
            //TODO above wait is a temp workaround that is not very good.
            //Things like throttling could cause the template to take considerably longer.
            //In order to handle this the deploy origin lambda will need to report the status
            //of the template deployment so that the CLI can react to it.
            StackStatus stackStatus = getStackStatusWithRetry(stackName, cloudFormationClient);
            boolean sameLine = false;

            Set<String> errors = new HashSet<>();
            while (!completedStatuses.contains(stackStatus)) {
                waitFor(2000);
                stackStatus = getStackStatus(stackName, cloudFormationClient, emitter);
                emitter.emitKeyValueSameLine("StackStatus", stackStatus.name(), getColorForStatus(stackStatus));
                sameLine = true;
                printInitErrors(errors, getDeploymentRequest, emitter);
            }
            if (sameLine) {
                emitter.emitNewLine();
            }
            switch (stackStatus) {
                case CREATE_COMPLETE -> emitter.emitString("Init stack created successfully");
                case UPDATE_COMPLETE -> emitter.emitString("Init stack updated successfully");
                default -> {
                    waitFor(1000); //wait for 1 second to make sure step guard has time to save error
                    if (errors.isEmpty()) {
                        Deployment deployment = deploymentHistoryFacade.getDeployment(getDeploymentRequest);
                        //Using an old version of the framework
                        throw new IllegalStateException("Stack " + stackName.getName() + " failed, ended with status: " + stackStatus
                                .name() + ", error = " + deployment.getDeploymentError()
                                                                   .map(DeploymentError::getErrorMessage)
                                                                   .orElseGet(() -> getInitStackError(dynamoDbClient,
                                                                                                      stackName)));
                    } else {
                        throw new IllegalStateException("Deployment failed");
                    }
                }
            }
        }
    }

    private PrintUtil.Color getColorForStatus(StackStatus stackStatus) {
        return switch (stackStatus) {
            case CREATE_COMPLETE, UPDATE_COMPLETE -> GREEN;
            case ROLLBACK_FAILED, DELETE_FAILED, CREATE_FAILED, UPDATE_ROLLBACK_FAILED, UPDATE_FAILED, IMPORT_ROLLBACK_FAILED,
                    UPDATE_ROLLBACK_COMPLETE, UPDATE_ROLLBACK_IN_PROGRESS -> RED;
            default -> YELLOW;
        };
    }


    private void printInitErrors(Set<String> errorSet,
                                 GetDeploymentRequest getDeploymentRequest,
                                 DataEmitter emitter) {
        Deployment deployment = deploymentHistoryFacade.getDeployment(getDeploymentRequest);

        for (InitStackError error : deployment.getInitStackErrors()) {
            boolean newAddition = errorSet.add(error.resourceName());
            if (newAddition) {
                emitter.emitNewLine();
                emitter.emitString("\tResource " + error.resourceName() + " failed with Status " + error.resourceStatus());
                deployment.getStackName()
                          .ifPresent(stackName -> emitter.emitString("\tStack: " + stackName.getName()));
                emitter.emitString("\tReason: " + error.reason());
            }
        }
    }


    private String getInitStackError(DynamoDbClient dynamoDbClient, StackName stackName) {
        GetItemResponse attiniDeployStatesTable = dynamoDbClient
                .getItem(GetItemRequest.builder()
                                       .tableName(
                                               "AttiniResourceStatesV1")
                                       .key(initStackStateKey(stackName))
                                       .build());
        AttributeValue stackError = attiniDeployStatesTable.item().get("stackError");

        if (stackError != null) {
            Map<String, AttributeValue> error = stackError.m();
            return error.get("Message").s() + ", resource = " + error.get("Resource").s();
        }

        return "Could not resolve init stack error, please check cloudformation logs";

    }


    private StackStatus getStackStatus(StackName stackName,
                                       CloudFormationClient cloudFormationClient,
                                       DataEmitter dataEmitter) {
        try {
            return cloudFormationClient
                    .describeStacks(DescribeStacksRequest.builder()
                                                         .stackName(stackName.getName())
                                                         .build())
                    .stacks()
                    .get(0)
                    .stackStatus();
        } catch (AwsServiceException e) {
            dataEmitter.emitNewLine();
            throw new IllegalStateException(
                    "Can no longer get stack info, possible reason for this could be that the stack was deleted during deployment",
                    e);
        } catch (SdkClientException e) {
            dataEmitter.emitNewLine();
            throw e;
        }
    }


    private StackStatus getStackStatus(StackName stackName, CloudFormationClient cloudFormationClient) {

        return cloudFormationClient
                .describeStacks(DescribeStacksRequest.builder()
                                                     .stackName(stackName.getName())
                                                     .build())
                .stacks()
                .get(0)
                .stackStatus();
    }

    private StackStatus getStackStatusWithRetry(StackName stackName, CloudFormationClient cloudFormationClient) {

        for (int i = 0; i < 3; i++) {
            try {
                return getStackStatus(stackName, cloudFormationClient);
            } catch (CloudFormationException e) {
                if (e.awsErrorDetails().errorMessage().contains("does not exist")) {
                    waitFor(2000);
                }
            }
        }

        throw new IllegalStateException(
                "Could not find the init stack, please repeat the deployment. If the problem persist please contact Attini support.");
    }

    private void pollDeploymentPlanStatus(GetDeploymentRequest request,
                                          DataEmitter emitter) {

        ExecutionArn executionArn = waitForExecutionArns(request);

        Region region = profileFacade.getRegion();

        String sfnUrl = "https://%s.console.aws.amazon.com/states/home?region=%s#/v2/executions/details/%s".formatted(
                region.getName(),
                region.getName(),
                executionArn.getValue());


        emitter.emitKeyValue("StepFunctionUrl", sfnUrl);

        DeploymentPlanStatus deploymentPlanStatus = getDeploymentPlansStatus(executionArn);

        StackErrorPrinter stackErrorPrinter = new StackErrorPrinter(globalConfig.printAsJson(), objectMapper, consolePrinter);

        statusPrinter.configureStepLength(deploymentPlanStatusFacade.getLongestStepNameLength(executionArn));

        statusPrinter.printHeader();
        Deployment deployment = deploymentHistoryFacade.getDeployment(request);
        while (ExecutionStatus.RUNNING.name().equals(deploymentPlanStatus.getDeploymentPlanStatus())) {
            waitFor(1000);
            deploymentPlanStatus = getDeploymentPlansStatus(executionArn);
            deployment = deploymentHistoryFacade.getDeployment(request);
            stackErrorPrinter.printStackError(deployment);
            statusPrinter.print(deploymentPlanStatus,
                                executionArn,
                                deployment);

        }

        waitFor(1000);
        deploymentPlanStatus = getDeploymentPlansStatus(executionArn);
        statusPrinter.print(deploymentPlanStatus,
                            executionArn,
                            deployment);

        if (deploymentPlanStatus.getEndTime().isPresent()) {
            emitter.emitString(deploymentPlanStatus.getDeploymentPlanName() +
                               " ended at " + deploymentPlanStatus.getEndTime().get().atZone(
                                                                          ZoneId.systemDefault())
                                                                  .toLocalDateTime()
                                                                  .format(DateTimeFormatter.ofPattern(
                                                                          "yyyy-MM-dd HH:mm:ss")));

            long executionTimeInSec = deploymentPlanStatus.getEndTime()
                                                          .get()
                                                          .getEpochSecond() - deploymentPlanStatus.getStartTime()
                                                                                                  .getEpochSecond();
            emitter.emitString(deploymentPlanStatus.getDeploymentPlanName() + " took " + executionTimeInSec + " seconds to execute");
        }

        emitter.emitString("Deployment plan finished with status: " + (hasFailedStatus(deploymentPlanStatus) ? PrintUtil.toRed(
                deploymentPlanStatus.getDeploymentPlanStatus()) : PrintUtil.toGreen(deploymentPlanStatus.getDeploymentPlanStatus())));
        if (hasFailedStatus(deploymentPlanStatus) && stackErrorPrinter.noErrorsPrinted()) {
            throw new IllegalStateException("A deployment plan in the distribution failed: "
                                            + getFailedReason(deploymentPlanStatus));
        } else if (hasFailedStatus(deploymentPlanStatus)) {
            throw new IllegalStateException("A deployment plan in the distribution failed");
        }
    }


    private boolean hasFailedStatus(DeploymentPlanStatus deploymentPlanStatuses) {
        String status = deploymentPlanStatuses.getDeploymentPlanStatus();
        return status.equals(ExecutionStatus.FAILED.name())
               || status.equals(ExecutionStatus.ABORTED.name())
               || status.equals(ExecutionStatus.TIMED_OUT.name());
    }

    private String getFailedReason(DeploymentPlanStatus deploymentPlanStatus) {

        return deploymentPlanStatus.getCompletedSteps()
                                   .stream()
                                   .filter(deploymentPlanStepStatus -> !deploymentPlanStepStatus.getStepStatus()
                                                                                                .equals(StepStatus.SUCCESS))
                                   .filter(deploymentPlanStepStatus -> deploymentPlanStepStatus.getMessage()
                                                                                               .isPresent())
                                   .findAny()
                                   .flatMap(DeploymentPlanStepStatus::getMessage)
                                   .orElse("No cause found, check step function logs");

    }


    private DeploymentPlanStatus getDeploymentPlansStatus(ExecutionArn executionArn) {

        return deploymentPlanStatusFacade.getDeploymentPlanStatus(new GetDeploymentPlanExecutionRequest(executionArn));


    }

    private Optional<Deployment> getDeployment(GetDeploymentRequest getDeploymentRequest) {
        try {
            return Optional.of(deploymentHistoryFacade.getDeployment(getDeploymentRequest));
        } catch (NoDistributionFoundException e) {
            return Optional.empty();
        }
    }


    private void waitForDeployData(GetDeploymentRequest getDeploymentRequest) {
        while (getDeployment(getDeploymentRequest).isEmpty()) {
            waitFor(1000);
        }
    }

    private void waitForStepGuardUpdates(GetDeploymentRequest getDeploymentRequest) {
        while (getDeployment(getDeploymentRequest).flatMap(Deployment::getDeploymentPlanCount).isEmpty()) {
            waitFor(1000);
        }
    }

    private ExecutionArn waitForExecutionArns(GetDeploymentRequest getDeploymentRequest) {
        Deployment deployment = deploymentHistoryFacade.getDeployment(getDeploymentRequest);
        while (deployment.getExecutionArn().isEmpty()) {
            waitFor(1000);
            deployment = deploymentHistoryFacade.getDeployment(getDeploymentRequest);
        }
        return deployment.getExecutionArn().get();
    }

    private static Map<String, AttributeValue> initStackStateKey(StackName stackName) {
        return Map.of("name",
                      AttributeValue.builder()
                                    .s(stackName.getName())
                                    .build(),
                      "resourceType",
                      AttributeValue.builder()
                                    .s("InitDeployCloudformationStack")
                                    .build());
    }

    private static void waitFor(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
