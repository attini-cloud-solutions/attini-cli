package se.attini.stackstatus;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import se.attini.AttiniNotInstalledException;
import se.attini.cli.global.GlobalConfig;
import se.attini.client.AwsClientFactory;
import se.attini.domain.Environment;
import se.attini.environment.EnvironmentUserInput;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.ExecutionListItem;
import software.amazon.awssdk.services.sfn.model.ExecutionStatus;
import software.amazon.awssdk.services.sfn.model.ListExecutionsRequest;

public class FindUnmanagedStackService {

    private final EnvironmentUserInput environmentUserInput;
    private final AwsClientFactory awsClientFactory;
    private final GlobalConfig globalConfig;

    public FindUnmanagedStackService(EnvironmentUserInput environmentUserInput,
                                     AwsClientFactory awsClientFactory,
                                     GlobalConfig globalConfig) {
        this.environmentUserInput = requireNonNull(environmentUserInput, "environmentUserInput");
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    public List<UnmanagedDistributionStacks> findUnmanagedStacks(FindUnmanagedStackRequest request) {
        try (SfnClient sfnClient = awsClientFactory.sfnClient();
             DynamoDbClient dynamoDbClient = awsClientFactory.dynamoClient()
        ) {
            return getInitDeployStacks(request, dynamoDbClient)
                    .stream()
                    .map(map -> findUnmanagedStacksFor(dynamoDbClient,
                                                       sfnClient,
                                                       map))
                    .collect(Collectors.toList());
        } catch (ResourceNotFoundException e) {
            throw new AttiniNotInstalledException();
        }


    }

    private List<Map<String, AttributeValue>> getInitDeployStacks(FindUnmanagedStackRequest request,
                                                                  DynamoDbClient dynamoDbClient) {

        Environment environment = environmentUserInput.getEnvironment(request);


        return dynamoDbClient.query(QueryRequest.builder().tableName(
                                                        "AttiniResourceStatesV1")
                                                .keyConditionExpression(
                                                        "resourceType=:v_resourceType")
                                                .expressionAttributeValues(
                                                        Map.of(":v_resourceType",
                                                               AttributeValue.builder()
                                                                             .s("InitDeployCloudformationStack")
                                                                             .build()))
                                                .build())
                             .items()
                             .stream()
                             .filter(map -> map.get("environment").s().equals(environment.getName().getName()))
                             .filter(map -> {
                                 if (request.getDistributionName().isPresent()) {
                                     return request.getDistributionName()
                                                   .get()
                                                   .getName()
                                                   .equals(map.get("distributionName").s());
                                 }
                                 return true;
                             }).collect(Collectors.toList());

    }

    private UnmanagedDistributionStacks findUnmanagedStacksFor(DynamoDbClient dynamoDbClient,
                                                               SfnClient client,
                                                               Map<String, AttributeValue> initStackData) {
        String distributionId = initStackData.get("distributionId").s();
        String distributionName = initStackData.get("distributionName").s();
        String environment = initStackData.get("environment").s();


        if (!initStackData.containsKey("sfnArns")) {
            return new UnmanagedDistributionStacks(distributionName,
                                                   "No StepFunction arn found connected to the init deploy stack for distribution.");
        }

        List<String> sfnArns = initStackData.get("sfnArns")
                                            .ss();

        boolean hasFailedStepFunction = sfnArns
                .stream()
                .map(s -> client.listExecutions(ListExecutionsRequest.builder()
                                                                     .stateMachineArn(s)
                                                                     .build())
                                .executions()
                                .stream()
                                .max(Comparator.comparing(ExecutionListItem::startDate)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(executionListItem -> !executionListItem.status()
                                                                 .equals(ExecutionStatus.SUCCEEDED));

        if (hasFailedStepFunction) {
            return new UnmanagedDistributionStacks(distributionName,
                                                   "Could not accurately detect unmanaged stacks for distribution, the last deployment plan was not successful or is ongoing.");

        }


        List<Map<String, AttributeValue>> collect = dynamoDbClient.query(QueryRequest.builder().tableName(
                                                                                             "AttiniResourceStatesV1")
                                                                                     .keyConditionExpression(
                                                                                             "resourceType=:v_resourceType")
                                                                                     .expressionAttributeValues(
                                                                                             Map.of(
                                                                                                     ":v_resourceType",
                                                                                                     AttributeValue.builder()
                                                                                                                   .s("CloudformationStack")
                                                                                                                   .build()))
                                                                                     .build())
                                                                  .items()
                                                                  .stream()
                                                                  .filter(map -> map.get("distributionName")
                                                                                    .s()
                                                                                    .equals(distributionName))
                                                                  .filter(map -> !map.get("distributionId")
                                                                                     .s()
                                                                                     .equals(distributionId))
                                                                  .filter(map -> map.get("environment")
                                                                                    .s()
                                                                                    .equals(environment)).toList();

        List<UnmanagedStack> unmanagedStacks = collect.stream()
                                                      .map(map -> map.get("name").s())
                                                      .map(s -> {
                                                          String[] split = s.split("-");

                                                          String account = split[split.length - 1];
                                                          String region = String.join("-",
                                                                                      split[split.length - 4],
                                                                                      split[split.length - 3],
                                                                                      split[split.length - 2]);
                                                          String stackName = s.substring(0,
                                                                                         s.length() - account.length() - region.length() - 2);

                                                          return createUnmanagedStack(stackName,
                                                                                      region,
                                                                                      account);
                                                      }).collect(Collectors.toList());

        return new UnmanagedDistributionStacks(distributionName, unmanagedStacks);

    }

    private UnmanagedStack createUnmanagedStack(String stackName,
                                                       String region,
                                                       String account) {

        String profileFlag = globalConfig.getProfile()
                                          .map(profile -> " --profile " + profile.getProfileName())
                                          .orElse("");
        String regionFlag = globalConfig.getRegion().map(region1 -> " --region " + region1.getName()).orElse("");

        String removeCommand = String.format(
                "attini ops delete-stack-resources --stack-name %s --stack-region %s --account-id %s%s%s",
                stackName,
                region,
                account,
                profileFlag,
                regionFlag);

        return new UnmanagedStack(stackName,
                                  region,
                                  account,
                                  removeCommand);
    }
}
